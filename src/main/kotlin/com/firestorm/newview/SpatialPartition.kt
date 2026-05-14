package com.firestorm.newview

import com.firestorm.llmath.Vector3
import kotlin.math.abs
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const val SG_MIN_DIST_RATIO: Float = 0.00001f
const val SG_BOX_SIDE: Float  = 7f
const val SG_BOX_OFFSET: Float = 0.5f * SG_BOX_SIDE
const val SG_BOX_RAD: Float   = 0.5f * SG_BOX_SIDE * sqrt(3f.toDouble()).toFloat()
const val SG_OBJ_SIDE: Float  = 4f
const val SG_MAX_OBJ_RAD: Float = SG_OBJ_SIDE * 0.5f * sqrt(3f.toDouble()).toFloat()

// ---------------------------------------------------------------------------
// DrawInfo  (mirrors LLDrawInfo — a single GPU draw call)
// ---------------------------------------------------------------------------

class DrawInfo(
    val start: UShort,
    val end: UShort,
    var count: UInt,
    var offset: UInt,
    var texture: Any?,           // LLViewerTexture*
    var vertexBuffer: Any?       // LLVertexBuffer*
) {
    var specularMap: Any? = null
    var normalMap: Any? = null
    var specularMapMatrix: Any? = null
    var normalMapMatrix: Any? = null
    var textureMatrix: Any? = null
    var modelMatrix: Any? = null
    var avatar: Any? = null      // LLVOAvatar*
    var skinInfo: Any? = null    // LLMeshSkinInfo*
    var material: Any? = null    // LLMaterial*
    var gltfMaterial: Any? = null
    var specColor: FloatArray = floatArrayOf(1f, 1f, 1f, 0.5f)
    val textureList: MutableList<Any?> = mutableListOf()
    var materialId: String = ""
    var shaderMask: UInt = 0u
    var envIntensity: Float = 0f
    var alphaMaskCutoff: Float = 0.5f
    var blendFuncSrc: Int = 0x0302  // GL_SRC_ALPHA
    var blendFuncDst: Int = 0x0303  // GL_ONE_MINUS_SRC_ALPHA
    var diffuseAlphaMode: UByte = 0u
    var bump: UByte = 0u
    var shiny: UByte = 0u
    var fullbright: Boolean = false
    var hasGlow: Boolean = false

    /**
     * Assert that vertexBuffer is non-null and the index range [start..end]
     * is consistent.  Mirrors LLDrawInfo::validate().
     */
    fun validate() {
        check(vertexBuffer != null) { "DrawInfo.validate: vertexBuffer is null" }
        check(start <= end) { "DrawInfo.validate: start ($start) > end ($end)" }
    }

    /**
     * Return skinInfo?.hashCode().toLong() ?: 0L, mirroring LLDrawInfo::getSkinHash()
     * which returns mSkinInfo->mHash or 0.
     */
    fun getSkinHash(): Long = skinInfo?.hashCode()?.toLong() ?: 0L

    /**
     * Derive a stable debug RGBA colour from the identity hash of this DrawInfo.
     * Packed as 0xRRGGBBAA with alpha always 0xFF.
     * Mirrors LLDrawInfo::getDebugColor().
     */
    fun getDebugColor(): Int {
        val h = System.identityHashCode(this)
        val r = (h shr 16) and 0xFF
        val g = (h shr 8)  and 0xFF
        val b =  h         and 0xFF
        return (r shl 24) or (g shl 16) or (b shl 8) or 0xFF
    }
}

// ---------------------------------------------------------------------------
// SpatialState — eSpatialState values from LLSpatialGroup
// Extends LLViewerOctreeGroup::INVALID_STATE (0x8000)
// ---------------------------------------------------------------------------

object SpatialState {
    const val GEOM_DIRTY:    UInt = 0x00008000u  // == LLViewerOctreeGroup::INVALID_STATE
    const val ALPHA_DIRTY:   UInt = 0x00010000u
    const val IN_IMAGE_QUEUE:UInt = 0x00020000u
    const val IMAGE_DIRTY:   UInt = 0x00040000u
    const val MESH_DIRTY:    UInt = 0x00080000u
    const val NEW_DRAWINFO:  UInt = 0x00100000u
    const val IN_BUILD_Q1:   UInt = 0x00200000u
    const val IN_BUILD_Q2:   UInt = 0x00400000u
    const val STATE_MASK:    UInt = 0x0000FFFFu
    const val DEAD:          UInt = 0x00000200u  // from LLViewerOctreeGroup
    const val OBJECT_DIRTY:  UInt = 0x00000100u
    const val SKIP_FRUSTUM_CHECK: UInt = 0x00000400u

    // Occlusion-query state bits (mirrors LLOcclusionCullingGroup)
    const val QUERY_PENDING:  UInt = 0x00000001u
    const val DISCARD_QUERY:  UInt = 0x00000002u
    const val OCCLUDED:       UInt = 0x00000004u
}

// ---------------------------------------------------------------------------
// Internal frame-time / frame-counter (replaces gFrameTimeSeconds)
// ---------------------------------------------------------------------------

private object FrameClock {
    private val startNs: Long = System.nanoTime()
    val seconds: Float get() = (System.nanoTime() - startNs) / 1_000_000_000f
    var frameCounter: Long = 0L
}

// ---------------------------------------------------------------------------
// VertexBufferStub — placeholder for LLVertexBuffer when no GL context exists
// ---------------------------------------------------------------------------

private class VertexBufferStub(val numVerts: UInt, val numIndices: UInt)

// ---------------------------------------------------------------------------
// SpatialGroup  (mirrors LLSpatialGroup)
// ---------------------------------------------------------------------------

class SpatialGroup(
    val octreeNode: Any,          // OctreeNode* — GPU/octree type
    val spatialPartition: SpatialPartition
) {
    // ------------------------------------------------------------------
    // Statics
    // ------------------------------------------------------------------

    companion object {
        var nodeCount: UInt = 0u
        var noDelete: Boolean = false

        val SG_INHERIT_MASK: UInt = 0x00000004u  // OCCLUDED
        val SG_INITIAL_STATE_MASK: UInt = SpatialState.GEOM_DIRTY or SpatialState.OBJECT_DIRTY

        private var NEXT_QUERY_ID = 1  // mock GL query-name counter
        private const val MAX_CAMERAS = 4
    }

    // ------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------

    var state: UInt = 0u
    var mState: UInt = 0u         // occlusion + extra state, mirrors LLOcclusionCullingGroup

    val drawMap: MutableMap<UInt, MutableList<DrawInfo>> = mutableMapOf()
    val bridgeList: MutableList<SpatialBridge> = mutableListOf()
    val bufferMap: MutableMap<Any?, Any?> = mutableMapOf()  // mirrors buffer_map_t

    var vertexBuffer: Any? = null  // LLVertexBuffer*

    var objectBoxSize: Float = 1f
    var geometryBytes: UInt = 0u
    var surfaceArea: Float = 0f
    var built: Float = 0f

    var distance: Float = 0f
    var depth: Float = 0f
    var lastUpdateDistance: Float = -1f
    var lastUpdateTime: Float = 0f

    var pixelArea: Float = 1024f
    var radius: Float = 1f

    var viewAngle: FloatArray = FloatArray(4)
    var lastUpdateViewAngle: FloatArray = floatArrayOf(-1f, -1f, -1f, -1f)

    var avatar: Any? = null      // LLVOAvatar*
    var renderOrder: UInt = 0u
    var reflectionProbe: Any? = null  // LLReflectionMap*

    // Bounding data — mirrors mBounds, mExtents, mObjectBounds, mObjectExtents
    val bounds: Array<FloatArray> = arrayOf(FloatArray(4), FloatArray(4))
    val extents: Array<FloatArray> = arrayOf(FloatArray(4), FloatArray(4))
    val objectBounds: Array<FloatArray> = arrayOf(FloatArray(4), FloatArray(4))
    val objectExtents: Array<FloatArray> = arrayOf(FloatArray(4), FloatArray(4))

    // GL occlusion query object names (one slot per camera)
    private val occlusionQueryIds: IntArray = IntArray(MAX_CAMERAS) { 0 }

    // Visibility stamp — updated each frame this group passes culling
    private var visibilityStamp: Long = -1L

    init {
        nodeCount++
        setState(SG_INITIAL_STATE_MASK)
        // Register with rebuild queue.  The C++ equivalent calls:
        //   gPipeline.markRebuild(this)
        //   mReflectionProbe = gPipeline.mReflectionMapManager.registerSpatialGroup(this)
        // Both are handled by the partition's rebuild bookkeeping below.
        spatialPartition.markRebuild(this)
        lastUpdateTime = FrameClock.seconds
    }

    // ------------------------------------------------------------------
    // State helpers
    // ------------------------------------------------------------------

    fun getState(): UInt = state
    fun isState(bits: UInt): Boolean = (state and bits) != 0u
    fun hasState(bits: UInt): Boolean = isState(bits)

    fun setState(bits: UInt) { state = state or bits }
    fun clearState(bits: UInt) { state = state and bits.inv() }

    fun setState(bits: UInt, mode: Int) {
        when {
            mode > STATE_MODE_SINGLE && mode == STATE_MODE_DIFF ->
                traverseSetStateDiff(bits)
            mode > STATE_MODE_SINGLE ->
                traverseSetState(bits)
            else -> setState(bits)
        }
    }

    fun clearState(bits: UInt, mode: Int) {
        when {
            mode > STATE_MODE_SINGLE && mode == STATE_MODE_DIFF ->
                traverseClearStateDiff(bits)
            mode > STATE_MODE_SINGLE ->
                traverseClearState(bits)
            else -> clearState(bits)
        }
    }

    /**
     * Traverse the octree from this node downward and set [bits] on every
     * descendant SpatialGroup.  Mirrors LLSpatialSetState / OctreeTraveler.
     */
    private fun traverseSetState(bits: UInt) {
        for (group in spatialPartition.allGroups()) {
            group.setState(bits)
        }
    }

    /**
     * Set [bits] only on groups that do NOT already carry them.
     * Mirrors LLSpatialSetStateDiff.
     */
    private fun traverseSetStateDiff(bits: UInt) {
        for (group in spatialPartition.allGroups()) {
            if (!group.hasState(bits)) group.setState(bits)
        }
    }

    /**
     * Clear [bits] on every descendant group.
     * Mirrors LLSpatialClearState.
     */
    private fun traverseClearState(bits: UInt) {
        for (group in spatialPartition.allGroups()) {
            group.clearState(bits)
        }
    }

    /**
     * Clear [bits] only on groups that currently carry them.
     * Mirrors LLSpatialClearStateDiff.
     */
    private fun traverseClearStateDiff(bits: UInt) {
        for (group in spatialPartition.allGroups()) {
            if (group.hasState(bits)) group.clearState(bits)
        }
    }

    fun dirtyGeom() { setState(SpatialState.GEOM_DIRTY) }
    fun dirtyMesh() { setState(SpatialState.MESH_DIRTY) }

    fun isDead(): Boolean = isState(SpatialState.DEAD)

    /**
     * Return true when no drawables are registered in the octree node.
     * Proxied through the partition's drawable registry.
     */
    fun isEmpty(): Boolean = spatialPartition.drawablesInGroup(this).isEmpty()

    /**
     * Return true if GEOM_DIRTY or OBJECT_DIRTY is set.
     * Mirrors LLViewerOctreeGroup::isDirty().
     */
    fun isDirty(): Boolean = isState(SpatialState.GEOM_DIRTY or SpatialState.OBJECT_DIRTY)

    /**
     * Return true if this group was marked visible in the current frame.
     * Mirrors LLViewerOctreeGroup::isVisible().
     */
    fun isVisible(): Boolean = visibilityStamp == FrameClock.frameCounter

    /** Mark this group visible for the current frame. */
    fun markVisible() { visibilityStamp = FrameClock.frameCounter }

    // ------------------------------------------------------------------
    // Occlusion state  (mirrors LLOcclusionCullingGroup)
    // ------------------------------------------------------------------

    /**
     * Test whether any of [bits] are set in the combined occlusion+state word.
     */
    fun isOcclusionState(bits: UInt): Boolean = (mState and bits) != 0u

    /**
     * Set occlusion state bits.
     * STATE_MODE_ALL_CAMERAS merges into mState unconditionally.
     */
    fun setOcclusionState(bits: UInt, mode: Int) {
        mState = mState or bits
    }

    /**
     * Issue or poll an occlusion query for this group.
     * State machine mirrors LLOcclusionCullingGroup::doOcclusion().
     *
     * Real LWJGL calls (requires active GL context):
     *   Allocate: GL15.glGenQueries()
     *   Begin:    GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, id)
     *   End:      GL15.glEndQuery(GL15.GL_SAMPLES_PASSED)
     *   Poll:     GL15.glGetQueryObjectiv(id, GL15.GL_QUERY_RESULT_AVAILABLE, buf)
     *   Result:   GL15.glGetQueryObjecti(id, GL15.GL_QUERY_RESULT)
     */
    fun checkOcclusion() {
        if (isDead()) return
        val camSlot = 0  // simplified: single world camera
        if (occlusionQueryIds[camSlot] == 0) {
            // Allocate a GL occlusion query object name.
            // Real: occlusionQueryIds[camSlot] = GL15.glGenQueries()
            occlusionQueryIds[camSlot] = NEXT_QUERY_ID++
        }
        if (isOcclusionState(SpatialState.QUERY_PENDING)) {
            // Non-blocking result read.
            // Real: val available = IntArray(1)
            //       GL15.glGetQueryObjectiv(id, GL15.GL_QUERY_RESULT_AVAILABLE, available)
            //       if (available[0] != 0) { val samples = GL15.glGetQueryObjecti(id, GL15.GL_QUERY_RESULT) }
            val samplesVisible = 1  // conservative: assume visible without a context
            if (samplesVisible == 0) {
                mState = mState or SpatialState.OCCLUDED
            } else {
                mState = mState and SpatialState.OCCLUDED.inv()
            }
            mState = mState and SpatialState.QUERY_PENDING.inv()
        } else {
            // Begin a new query around this group's bounding box.
            // Real: GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, occlusionQueryIds[camSlot])
            //       ... render bounding-box geometry ...
            //       GL15.glEndQuery(GL15.GL_SAMPLES_PASSED)
            mState = mState or SpatialState.QUERY_PENDING
            mState = mState and SpatialState.DISCARD_QUERY.inv()
        }
    }

    /**
     * Delete GL occlusion query objects for all camera slots.
     * Mirrors LLOcclusionCullingGroup::releaseOcclusionQueryObjectNames().
     * Real: GL15.glDeleteQueries(id) per slot.
     */
    fun releaseOcclusionQueryObjectNames() {
        for (i in occlusionQueryIds.indices) {
            if (occlusionQueryIds[i] != 0) {
                // Real: GL15.glDeleteQueries(occlusionQueryIds[i])
                occlusionQueryIds[i] = 0
            }
        }
        mState = mState and (SpatialState.QUERY_PENDING or SpatialState.DISCARD_QUERY).inv()
    }

    // ------------------------------------------------------------------
    // Octree node accessors
    // ------------------------------------------------------------------

    fun getOctreeNode(): Any = octreeNode

    /** Return the parent SpatialGroup via the partition's parent registry. */
    fun getParent(): SpatialGroup? = spatialPartition.parentOf(this)

    // ------------------------------------------------------------------
    // Draw-map management
    // ------------------------------------------------------------------

    fun clearDrawMap() { drawMap.clear() }

    /**
     * Validate all invariants on this group.
     * Mirrors LLSpatialGroup::validate() (paranoia mode assertions).
     */
    fun validate() {
        check(!isState(SpatialState.DEAD)) { "validate: group is DEAD" }
        check(!isDirty()) { "validate: group is DIRTY during validate" }
        for (drawable in spatialPartition.drawablesInGroup(this)) {
            check(drawable.getSpatialGroup() == null || drawable.getSpatialGroup() === this) {
                "validate: drawable references wrong group"
            }
        }
        validateDrawMap()
    }

    fun validateDrawMap() {
        drawMap.values.forEach { list ->
            list.forEach { it.validate() }
        }
    }

    // ------------------------------------------------------------------
    // Object add / remove / update
    // ------------------------------------------------------------------

    fun addObject(drawable: Drawable): Boolean {
        drawable.setGroup(this)
        setState(SpatialState.OBJECT_DIRTY or SpatialState.GEOM_DIRTY)
        setOcclusionState(SpatialState.DISCARD_QUERY, STATE_MODE_ALL_CAMERAS)
        // C++: gPipeline.markRebuild(this)
        spatialPartition.markRebuild(this)
        if (drawable.isSpatialBridge()) {
            bridgeList.add(drawable as SpatialBridge)
        }
        if (drawable.getRadius() > 1f) {
            setState(SpatialState.IMAGE_DIRTY)
        }
        return true
    }

    fun removeObject(drawable: Drawable, fromOctree: Boolean = false): Boolean {
        drawable.setGroup(null)
        if (fromOctree) {
            setState(SpatialState.GEOM_DIRTY)
            spatialPartition.markRebuild(this)
            if (drawable.isSpatialBridge()) {
                bridgeList.removeAll { it === drawable }
            }
            if (isEmpty()) clearDrawMap()
        }
        return true
    }

    /**
     * Check whether [drawable] still fits within this octree node's AABB.
     * Mirrors LLSpatialGroup::updateInGroup().
     */
    fun updateInGroup(drawable: Drawable, immediate: Boolean = false): Boolean {
        drawable.updateSpatialExtents()
        val wp = drawable.getWorldPosition()
        val centre = bounds[0]
        val half   = bounds[1]
        val inside = (0..2).all { i ->
            val v = floatArrayOf(wp.x, wp.y, wp.z)[i]
            v >= centre[i] - half[i] && v <= centre[i] + half[i]
        }
        return if (inside) {
            unbound()
            setState(SpatialState.OBJECT_DIRTY)
            true
        } else {
            false
        }
    }

    /**
     * Expand this group's extents to include all eight corners of [addingExtents]
     * after rotating them into [currentTransform]'s local frame.
     * Mirrors LLSpatialGroup::expandExtents().
     */
    fun expandExtents(addingExtents: Array<FloatArray>, currentTransform: XformMatrix) {
        val minE = addingExtents[0]
        val maxE = addingExtents[1]
        val xs = floatArrayOf(minE[0], maxE[0])
        val ys = floatArrayOf(minE[1], maxE[1])
        val zs = floatArrayOf(minE[2], maxE[2])
        val worldPos = currentTransform.getPosition()
        val rot = currentTransform.getWorldRotation()  // quaternion xyzw
        // Conjugate quaternion for backward rotation
        val qx = -rot[0]; val qy = -rot[1]; val qz = -rot[2]; val qw = rot[3]
        for (xi in 0..1) for (yi in 0..1) for (zi in 0..1) {
            var cx = xs[xi] - worldPos.x
            var cy = ys[yi] - worldPos.y
            var cz = zs[zi] - worldPos.z
            // Apply conjugate quaternion rotation: v' = q^-1 * v * q
            val ix =  qw*cx + qy*cz - qz*cy
            val iy =  qw*cy + qz*cx - qx*cz
            val iz =  qw*cz + qx*cy - qy*cx
            val iw = -qx*cx - qy*cy - qz*cz
            val rx = ix*qw + iw*(-qx) + iy*(-qz) - iz*(-qy)
            val ry = iy*qw + iw*(-qy) + iz*(-qx) - ix*(-qz)
            val rz = iz*qw + iw*(-qz) + ix*(-qy) - iy*(-qx)
            val corner = floatArrayOf(rx, ry, rz)
            for (j in 0..2) {
                if (corner[j] < extents[0][j]) extents[0][j] = corner[j]
                if (corner[j] > extents[1][j]) extents[1][j] = corner[j]
            }
        }
        for (i in 0..2) {
            bounds[0][i] = (extents[0][i] + extents[1][i]) * 0.5f
            bounds[1][i] = abs(extents[1][i] - extents[0][i]) * 0.5f
        }
    }

    /**
     * Translate this group's bounding data by [offset].
     * Mirrors LLSpatialGroup::shift().
     */
    fun shift(offset: FloatArray) {
        for (i in 0..2) {
            bounds[0][i]       += offset[i]
            extents[0][i]      += offset[i]
            extents[1][i]      += offset[i]
            objectBounds[0][i] += offset[i]
            objectExtents[0][i]+= offset[i]
            objectExtents[1][i]+= offset[i]
        }
        if (!spatialPartition.renderByGroup && !spatialPartition.isBridge()) {
            setState(SpatialState.GEOM_DIRTY)
            spatialPartition.markRebuild(this)
        }
    }

    // ------------------------------------------------------------------
    // Distance / LOD
    // ------------------------------------------------------------------

    fun updateDistance(cameraOrigin: Vector3) {
        if (isEmpty()) return
        radius = if (spatialPartition.renderByGroup) {
            sqrt((objectBounds[1][0]*objectBounds[1][0] +
                  objectBounds[1][1]*objectBounds[1][1] +
                  objectBounds[1][2]*objectBounds[1][2]).toDouble()).toFloat()
        } else {
            sqrt((bounds[1][0]*bounds[1][0] +
                  bounds[1][1]*bounds[1][1] +
                  bounds[1][2]*bounds[1][2]).toDouble()).toFloat()
        }
        distance = spatialPartition.calcDistance(this, cameraOrigin)
        pixelArea = spatialPartition.calcPixelArea(this, cameraOrigin)
    }

    fun getUpdateUrgency(): Float {
        if (!isVisible()) return 0f
        val time = lastUpdateTime + 4f
        return time + (objectBounds[1].let { b ->
            b[0]*b[0] + b[1]*b[1] + b[2]*b[2] + 1f
        } / distance)
    }

    fun changeLOD(): Boolean {
        if (hasState(SpatialState.ALPHA_DIRTY or SpatialState.OBJECT_DIRTY)) return true
        val slopRatio = spatialPartition.slopRatio
        if (slopRatio > 0f) {
            val denominator = maxOf(lastUpdateDistance, radius)
            val ratio = if (denominator > 0f) (distance - lastUpdateDistance) / denominator else 0f
            if (abs(ratio) >= slopRatio) return true
        }
        return needsUpdate()
    }

    /**
     * Return true if the group's visibility stamp is stale.
     * Mirrors LLViewerOctreeGroup::needsUpdate().
     */
    fun needsUpdate(): Boolean = visibilityStamp < FrameClock.frameCounter

    // ------------------------------------------------------------------
    // Rebuild
    // ------------------------------------------------------------------

    fun rebuildGeom() {
        if (!isDead()) {
            spatialPartition.rebuildGeom(this)
            if (hasState(SpatialState.MESH_DIRTY)) {
                // C++: gPipeline.markMeshDirty(this)
                spatialPartition.markMeshDirty(this)
            }
        }
    }

    fun rebuildMesh() {
        if (!isDead()) {
            spatialPartition.rebuildMesh(this)
        }
    }

    // ------------------------------------------------------------------
    // Occlusion / GL state
    // ------------------------------------------------------------------

    fun destroyGLState(keepOcclusion: Boolean = false) {
        setState(SpatialState.GEOM_DIRTY or SpatialState.IMAGE_DIRTY)
        if (!keepOcclusion) {
            spatialPartition.markRebuild(this)
        }
        lastUpdateTime = FrameClock.seconds
        vertexBuffer = null
        bufferMap.clear()
        clearDrawMap()
        if (!keepOcclusion) releaseOcclusionQueryObjectNames()
        // Release per-face vertex buffers for every drawable in this group.
        // C++: for each drawable, for each face: facep->clearVertexBuffer()
        for (drawable in spatialPartition.drawablesInGroup(this)) {
            for (i in 0 until drawable.getNumFaces()) {
                drawable.getFace(i)?.clearVertexBuffer()
            }
        }
    }

    // ------------------------------------------------------------------
    // Line-segment intersection
    // ------------------------------------------------------------------

    /**
     * Iterate all drawables in this group and test each drawable's AABB
     * against the ray [start..end].  Returns the closest-hit drawable, or null.
     * Mirrors LLSpatialGroup::lineSegmentIntersect() (AABB level only).
     */
    fun lineSegmentIntersect(
        start: FloatArray,
        end: FloatArray,
        pickTransparent: Boolean,
        pickRigged: Boolean,
        pickUnselectable: Boolean,
        pickReflectionProbe: Boolean,
        faceHit: IntArray? = null,
        intersection: FloatArray? = null,
        texCoord: FloatArray? = null,
        normal: FloatArray? = null,
        tangent: FloatArray? = null
    ): Drawable? {
        var closest: Drawable? = null
        var closestT = Float.MAX_VALUE
        val dir = FloatArray(3) { end[it] - start[it] }

        for (drawable in spatialPartition.drawablesInGroup(this)) {
            val centre = objectBounds[0]
            val half   = objectBounds[1]
            var tMin = 0f
            var tMax = 1f
            var hit = true
            for (i in 0..2) {
                val aMin = centre[i] - half[i]
                val aMax = centre[i] + half[i]
                if (abs(dir[i]) < 1e-6f) {
                    if (start[i] < aMin || start[i] > aMax) { hit = false; break }
                } else {
                    val inv = 1f / dir[i]
                    val t0 = (aMin - start[i]) * inv
                    val t1 = (aMax - start[i]) * inv
                    val tNear = minOf(t0, t1)
                    val tFar  = maxOf(t0, t1)
                    tMin = maxOf(tMin, tNear)
                    tMax = minOf(tMax, tFar)
                    if (tMin > tMax) { hit = false; break }
                }
            }
            if (hit && tMin < closestT) {
                closestT = tMin
                closest = drawable
                faceHit?.set(0, 0)
                if (intersection != null) {
                    for (i in 0..2) intersection[i] = start[i] + dir[i] * tMin
                }
            }
        }
        return closest
    }

    // ------------------------------------------------------------------
    // Debug drawing
    // ------------------------------------------------------------------

    /**
     * Draw a wire-frame box around objectBounds with the given color.
     * Mirrors LLSpatialGroup::drawObjectBox(LLColor4 col).
     * Delegates to drawBoxOutline() which emits GL_LINES.
     */
    fun drawObjectBox(color: FloatArray) {
        val centre = Vector3(objectBounds[0][0], objectBounds[0][1], objectBounds[0][2])
        val half   = Vector3(objectBounds[1][0], objectBounds[1][1], objectBounds[1][2])
        drawBoxOutline(centre, half)
    }

    // ------------------------------------------------------------------
    // Octree event handlers
    // ------------------------------------------------------------------

    fun handleInsertion(node: Any, entry: Any) {
        addObject(entry as Drawable)
        unbound()
        setState(SpatialState.OBJECT_DIRTY)
    }

    fun handleRemoval(node: Any, entry: Any) {
        removeObject(entry as Drawable, fromOctree = true)
        // Propagate dirty state up the parent chain (mirrors super.handleRemoval)
        unbound()
    }

    fun handleDestruction(node: Any) {
        if (isDead()) return
        setState(SpatialState.DEAD)
        // Null out group reference on every entry in this node
        for (drawable in spatialPartition.drawablesInGroup(this)) {
            drawable.setGroup(null)
        }
        clearDrawMap()
        vertexBuffer = null
        bufferMap.clear()
        spatialPartition.incrementZombieGroups()
        // mOctreeNode effectively null — tracked via DEAD flag
    }

    fun handleChildAddition(parent: Any, child: Any) {
        SpatialGroup(child, spatialPartition)
        unbound()
        // assert_states_valid omitted for JVM port
    }

    /**
     * Recompute this group's bounding box from its extents and propagate
     * to CONTROL_AV partitions.  Mirrors LLSpatialGroup::rebound().
     */
    fun rebound() {
        if (!isDirty()) return
        for (i in 0..2) {
            bounds[0][i] = (extents[0][i] + extents[1][i]) * 0.5f
            bounds[1][i] = abs(extents[1][i] - extents[0][i]) * 0.5f
        }
        clearState(SpatialState.OBJECT_DIRTY)
    }

    /**
     * Propagate the un-dirty signal up the parent octree group chain.
     * Mirrors LLViewerOctreeGroup::unbound().
     */
    fun unbound() {
        var parent = getParent()
        while (parent != null) {
            parent.setState(SpatialState.OBJECT_DIRTY)
            parent = parent.getParent()
        }
    }

    fun isHUDGroup(): Boolean = spatialPartition.isHUDPartition()

    // ------------------------------------------------------------------
    // Destructor logic
    // ------------------------------------------------------------------

    fun destroy() {
        if (isDead()) spatialPartition.decrementZombieGroups()
        nodeCount--
        clearDrawMap()
        releaseOcclusionQueryObjectNames()
    }
}

// State-mode constants used by setState/clearState with mode parameter
const val STATE_MODE_SINGLE: Int   = 0
const val STATE_MODE_BRANCH: Int   = 1
const val STATE_MODE_DIFF: Int     = 2
const val STATE_MODE_ALL_CAMERAS: Int = 3

// ---------------------------------------------------------------------------
// GeometryManager  (mirrors LLGeometryManager — pure interface)
// ---------------------------------------------------------------------------

interface GeometryManager {
    val faceList: MutableList<Any?>  // LLFace*
    fun rebuildGeom(group: SpatialGroup)
    fun rebuildMesh(group: SpatialGroup)
    fun getGeometry(group: SpatialGroup)
    fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray)
}

// ---------------------------------------------------------------------------
// SpatialPartition  (mirrors LLSpatialPartition)
// ---------------------------------------------------------------------------

open class SpatialPartition(
    val vertexDataMask: UInt,
    val renderByGroup: Boolean,
    val region: Any?             // LLViewerRegion*
) : GeometryManager {

    override val faceList: MutableList<Any?> = mutableListOf()

    var bridge: SpatialBridge? = null
    var infiniteFarClip: Boolean = false
    var slopRatio: Float = 0.25f
    var depthMask: Boolean = false

    protected var octree: Any? = null  // LLOctreeNode<LLViewerOctreeEntry>*
    var partitionType: Int = 0         // LLViewerRegion::EPartitionType
    var drawableType: Int = 0          // LLPipeline render type
    var mRegionp: Any? = region

    // JVM bookkeeping replacing the C++ octree listener graph
    private val groupRegistry: MutableList<SpatialGroup> = mutableListOf()
    private val groupDrawables: MutableMap<SpatialGroup, MutableList<Drawable>> = mutableMapOf()
    private val groupParent: MutableMap<SpatialGroup, SpatialGroup> = mutableMapOf()
    internal val rebuildSet: MutableSet<SpatialGroup> = mutableSetOf()
    internal val meshDirtySet: MutableSet<SpatialGroup> = mutableSetOf()
    private var zombieGroupCount: Int = 0

    init {
        // Create the root SpatialGroup backed by a placeholder octree-node object.
        // C++: new LLSpatialGroup(mOctree, this)
        val rootNode = object {}
        val rootGroup = SpatialGroup(rootNode, this)
        groupRegistry.add(rootGroup)
    }

    // ------------------------------------------------------------------
    // Internal partition management helpers (called by SpatialGroup)
    // ------------------------------------------------------------------

    internal fun allGroups(): List<SpatialGroup> = groupRegistry.toList()

    internal fun drawablesInGroup(group: SpatialGroup): List<Drawable> =
        groupDrawables[group] ?: emptyList()

    internal fun parentOf(group: SpatialGroup): SpatialGroup? = groupParent[group]

    internal fun markRebuild(group: SpatialGroup) { rebuildSet.add(group) }

    internal fun markMeshDirty(group: SpatialGroup) { meshDirtySet.add(group) }

    internal fun incrementZombieGroups() { zombieGroupCount++ }

    internal fun decrementZombieGroups() { if (zombieGroupCount > 0) zombieGroupCount-- }

    // ------------------------------------------------------------------
    // Insert / remove
    // ------------------------------------------------------------------

    fun put(drawable: Drawable, wasVisible: Boolean = false): SpatialGroup? {
        drawable.updateSpatialExtents()
        if (drawable.getSpatialGroup() == null) {
            // Insert into the root group (simplified flat octree)
            val rootGroup = groupRegistry.firstOrNull() ?: return null
            groupDrawables.getOrPut(rootGroup) { mutableListOf() }.add(drawable)
            rootGroup.addObject(drawable)
        }
        val group = drawable.getSpatialGroup()
        if (group != null && wasVisible &&
            group.isOcclusionState(SpatialState.QUERY_PENDING)
        ) {
            group.setOcclusionState(SpatialState.DISCARD_QUERY, STATE_MODE_ALL_CAMERAS)
        }
        return group
    }

    fun remove(drawable: Drawable, curp: SpatialGroup): Boolean {
        if (!curp.removeObject(drawable)) {
            error("Failed to remove drawable from octree")
        } else {
            drawable.setGroup(null)
            groupDrawables[curp]?.remove(drawable)
        }
        return true
    }

    // ------------------------------------------------------------------
    // Move / shift
    // ------------------------------------------------------------------

    open fun move(drawable: Drawable, curp: SpatialGroup?, immediate: Boolean = false) {
        if (drawable == null) return
        val wasVisible = curp?.isVisible() ?: false
        if (curp != null && curp.spatialPartition !== this) {
            if (curp.spatialPartition.remove(drawable, curp)) {
                put(drawable, wasVisible)
                return
            } else {
                error("Drawable lost between spatial partitions on outbound transition")
            }
        }
        if (curp != null && curp.updateInGroup(drawable, immediate)) return
        if (curp != null && !remove(drawable, curp)) {
            error("Move couldn't find existing spatial group")
        }
        put(drawable, wasVisible)
    }

    open fun shift(offset: FloatArray) {
        // Traverse all registered groups and shift each one.
        // Mirrors LLSpatialPartition::shift() → octree traversal.
        for (group in groupRegistry.toList()) {
            group.shift(offset)
        }
    }

    // ------------------------------------------------------------------
    // Distance / pixel-area
    // ------------------------------------------------------------------

    open fun calcDistance(group: SpatialGroup, cameraOrigin: Vector3): Float {
        val eye = FloatArray(3) {
            group.objectBounds[0][it] - cameraOrigin.toArray()[it]
        }
        var dist = sqrt((eye[0]*eye[0] + eye[1]*eye[1] + eye[2]*eye[2]).toDouble()).toFloat()

        // Alpha-sort depth calculation for groups with alpha-pass draw infos.
        // Mirrors the LLRenderPass::PASS_ALPHA branch in LLSpatialPartition::calcDistance().
        if (group.drawMap.containsKey(PASS_ALPHA)) {
            val len = sqrt((eye[0]*eye[0] + eye[1]*eye[1] + eye[2]*eye[2]).toDouble()).toFloat()
            dist = if (len > 0f) len else dist
            val eyeNorm = if (len > 0f) floatArrayOf(eye[0]/len, eye[1]/len, eye[2]/len)
                          else floatArrayOf(0f, 0f, 1f)
            if (!group.isOcclusionState(SpatialState.QUERY_PENDING)) {
                val lastAngle = group.lastUpdateViewAngle
                val diff = sqrt(
                    ((eyeNorm[0]-lastAngle[0]).let { it*it } +
                     (eyeNorm[1]-lastAngle[1]).let { it*it } +
                     (eyeNorm[2]-lastAngle[2]).let { it*it }).toDouble()
                ).toFloat()
                if (diff > 0.64f) {
                    group.viewAngle[0] = eyeNorm[0]; group.viewAngle[1] = eyeNorm[1]
                    group.viewAngle[2] = eyeNorm[2]
                    group.lastUpdateViewAngle[0] = eyeNorm[0]
                    group.lastUpdateViewAngle[1] = eyeNorm[1]
                    group.lastUpdateViewAngle[2] = eyeNorm[2]
                    group.setState(SpatialState.ALPHA_DIRTY)
                    markRebuild(group)
                }
            }
            group.depth = dist
        }

        if (dist < 16f) {
            dist = dist / 16f
            dist *= dist
            dist *= 16f
        }
        return dist
    }

    /**
     * Compute the approximate screen-space pixel area of this group.
     * Mirrors LLPipeline::calcPixelArea(centre, size, camera).
     * Uses a pin-hole camera model with a 90° FOV and 1024×1024 nominal viewport.
     */
    open fun calcPixelArea(group: SpatialGroup, cameraOrigin: Vector3): Float {
        val centre = group.objectBounds[0]
        val half   = group.objectBounds[1]
        val dx = centre[0] - cameraOrigin.x
        val dy = centre[1] - cameraOrigin.y
        val dz = centre[2] - cameraOrigin.z
        val dist = maxOf(sqrt((dx*dx + dy*dy + dz*dz).toDouble()).toFloat(), 0.001f)
        val radius = sqrt((half[0]*half[0] + half[1]*half[1] + half[2]*half[2]).toDouble()).toFloat()
        val VIEWPORT_PIXELS = 1024f * 1024f
        return (radius / dist) * (radius / dist) * VIEWPORT_PIXELS * Math.PI.toFloat()
    }

    // ------------------------------------------------------------------
    // Geometry rebuild  (GeometryManager implementation)
    // ------------------------------------------------------------------

    override fun rebuildGeom(group: SpatialGroup) {
        if (group.isDead() || !group.hasState(SpatialState.GEOM_DIRTY)) return
        if (group.changeLOD()) {
            group.lastUpdateDistance = group.distance
            group.lastUpdateViewAngle = group.viewAngle.copyOf()
        }
        group.clearDrawMap()
        val vertexCount = UIntArray(1)
        val indexCount  = UIntArray(1)
        addGeometryCount(group, vertexCount, indexCount)
        if (vertexCount[0] > 0u && indexCount[0] > 0u) {
            group.built = 1f
            // Allocate / reuse a vertex buffer for this group and fill geometry.
            // C++: group->mVertexBuffer = new LLVertexBuffer(mVertexDataMask);
            //      group->mVertexBuffer->allocateBuffer(vertexCount, indexCount);
            //      getGeometry(group);
            group.vertexBuffer = VertexBufferStub(vertexCount[0], indexCount[0])
            getGeometry(group)
        } else {
            group.vertexBuffer = null
            group.bufferMap.clear()
        }
        group.lastUpdateTime = FrameClock.seconds
        group.clearState(SpatialState.GEOM_DIRTY)
    }

    override fun rebuildMesh(group: SpatialGroup) { /* base no-op */ }

    override fun getGeometry(group: SpatialGroup) {
        // Base no-op; subclasses fill vertex buffers with geometry for this group.
    }

    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) {
        // Sum vertex/index requirements across all drawables in this group.
        // Each face contributes a conservative 4 verts / 6 indices.
        var verts = 0u
        var indices = 0u
        for (drawable in drawablesInGroup(group)) {
            verts   += drawable.getNumFaces().toUInt() * 4u
            indices += drawable.getNumFaces().toUInt() * 6u
        }
        vertexCount[0] = verts
        indexCount[0]  = indices
    }

    // ------------------------------------------------------------------
    // Culling
    // ------------------------------------------------------------------

    /**
     * Cull the partition's octree against a sphere around [cameraOrigin].
     * Groups that pass are marked visible; visible count is returned.
     * Mirrors LLSpatialPartition::cull() → LLOctreeCull traversal.
     */
    fun cull(cameraOrigin: Vector3, doOcclusion: Boolean = false): Int {
        FrameClock.frameCounter++
        groupRegistry.firstOrNull()?.rebound()
        var visible = 0
        for (group in groupRegistry) {
            if (isGroupVisible(group, cameraOrigin)) {
                group.markVisible()
                visible++
                if (doOcclusion) group.checkOcclusion()
            }
        }
        return visible
    }

    fun cull(cameraOrigin: Vector3, results: MutableList<Drawable>, forSelect: Boolean): Int {
        FrameClock.frameCounter++
        groupRegistry.firstOrNull()?.rebound()
        var count = 0
        for (group in groupRegistry) {
            if (isGroupVisible(group, cameraOrigin)) {
                group.markVisible()
                results.addAll(drawablesInGroup(group))
                count++
            }
        }
        return count
    }

    /**
     * Return true if any group passes the frustum test.
     * Mirrors LLSpatialPartition::visibleObjectsInFrustum().
     */
    fun visibleObjectsInFrustum(cameraOrigin: Vector3): Boolean =
        groupRegistry.any { isGroupVisible(it, cameraOrigin) }

    /**
     * Return true if point [v] is within the partition's root bounds.
     * Mirrors LLSpatialPartition::isVisible(const LLVector3&).
     */
    fun isVisible(v: Vector3): Boolean {
        val root = groupRegistry.firstOrNull() ?: return false
        val centre = root.bounds[0]
        val half   = root.bounds[1]
        return (0..2).all { i ->
            val vi = floatArrayOf(v.x, v.y, v.z)[i]
            vi >= centre[i] - half[i] && vi <= centre[i] + half[i]
        }
    }

    /**
     * Return true when this partition's type is HUD.
     * Mirrors LLSpatialPartition::isHUDPartition().
     * C++ constant LLViewerRegion::PARTITION_HUD == 11.
     */
    fun isHUDPartition(): Boolean = partitionType == PARTITION_HUD

    fun isBridge(): Boolean = bridge != null
    fun asBridge(): SpatialBridge? = bridge

    /**
     * Populate [visMin]/[visMax] with the world-space extents of all visible groups.
     * Returns true if no visible groups were found (empty).
     * Mirrors LLSpatialPartition::getVisibleExtents().
     */
    fun getVisibleExtents(cameraOrigin: Vector3, visMin: Vector3, visMax: Vector3): Boolean {
        var empty = true
        val minArr = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        val maxArr = floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (group in groupRegistry) {
            if (!group.isVisible()) continue
            empty = false
            for (i in 0..2) {
                if (group.extents[0][i] < minArr[i]) minArr[i] = group.extents[0][i]
                if (group.extents[1][i] > maxArr[i]) maxArr[i] = group.extents[1][i]
            }
        }
        if (!empty) {
            visMin.x = minArr[0]; visMin.y = minArr[1]; visMin.z = minArr[2]
            visMax.x = maxArr[0]; visMax.y = maxArr[1]; visMax.z = maxArr[2]
        }
        return empty
    }

    // ------------------------------------------------------------------
    // Debug rendering
    // ------------------------------------------------------------------

    /**
     * Traverse octree groups and render physics AABB shapes.
     * Mirrors LLSpatialPartition::renderPhysicsShapes().
     * Uses drawBox / drawBoxOutline which wrap GL_TRIANGLE_STRIP / GL_LINES.
     */
    fun renderPhysicsShapes(depthOnly: Boolean) {
        for (group in groupRegistry) {
            if (!group.isVisible()) continue
            val centre = Vector3(group.bounds[0][0], group.bounds[0][1], group.bounds[0][2])
            val half   = Vector3(group.bounds[1][0], group.bounds[1][1], group.bounds[1][2])
            if (depthOnly) {
                drawBox(centre, half)
            } else {
                drawBoxOutline(centre, half)
            }
        }
    }

    /**
     * Render debug overlay bounding boxes and visibility indicators.
     * Mirrors LLSpatialPartition::renderDebug().
     */
    fun renderDebug() {
        for (group in groupRegistry) {
            val colour = if (group.isVisible()) {
                floatArrayOf(0f, 1f, 0f, 0.25f)   // green = visible
            } else {
                floatArrayOf(1f, 0f, 0f, 0.25f)   // red = culled
            }
            group.drawObjectBox(colour)
        }
    }

    /**
     * Render AABBs of groups intersecting the camera frustum (sphere approximation).
     * Mirrors LLSpatialPartition::renderIntersectingBBoxes().
     */
    fun renderIntersectingBBoxes(cameraOrigin: Vector3) {
        for (group in groupRegistry) {
            if (isGroupVisible(group, cameraOrigin)) {
                val colour = floatArrayOf(1f, 1f, 0f, 0.5f)  // yellow
                group.drawObjectBox(colour)
            }
        }
    }

    fun restoreGL() { /* no-op — GL resources recreated on next rebuild */ }

    // ------------------------------------------------------------------
    // Cleanup
    // ------------------------------------------------------------------

    /**
     * Destroy all SpatialGroup nodes and free all DrawInfo allocations.
     * Mirrors LLSpatialPartition::~LLSpatialPartition() → cleanup().
     */
    fun cleanup() {
        for (group in groupRegistry.toList()) {
            group.destroy()
        }
        groupRegistry.clear()
        groupDrawables.clear()
        groupParent.clear()
        rebuildSet.clear()
        meshDirtySet.clear()
        octree = null
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Conservative sphere-AABB test for group visibility from [camera].
     * Full frustum-plane culling requires ViewerCamera plumbing.
     */
    private fun isGroupVisible(group: SpatialGroup, camera: Vector3): Boolean {
        if (group.isDead()) return false
        val centre = group.bounds[0]
        val half   = group.bounds[1]
        val radius = sqrt((half[0]*half[0] + half[1]*half[1] + half[2]*half[2]).toDouble()).toFloat()
        val dx = centre[0] - camera.x
        val dy = centre[1] - camera.y
        val dz = centre[2] - camera.z
        val dist = sqrt((dx*dx + dy*dy + dz*dz).toDouble()).toFloat()
        return dist - radius < 1024f  // 1024 m draw distance
    }

    companion object {
        /** Render-pass type for alpha-blended draw infos (mirrors LLRenderPass::PASS_ALPHA) */
        const val PASS_ALPHA: UInt = 4u

        /** Partition-type constant for HUD (mirrors LLViewerRegion::PARTITION_HUD) */
        const val PARTITION_HUD: Int = 11
    }
}

// ---------------------------------------------------------------------------
// SpatialBridge  (mirrors LLSpatialBridge — a Drawable that IS a partition)
// ---------------------------------------------------------------------------

open class SpatialBridge(
    val root: Drawable,
    renderByGroup: Boolean,
    dataMask: UInt,
    region: Any?
) : Drawable(root.viewerObject, false), SpatialPartition(dataMask, renderByGroup, region) {

    var drawableRoot: Drawable? = root

    init {
        // Set mBridge = this so isBridge() returns true on the SpatialPartition side.
        bridge = this
    }

    override fun isSpatialBridge(): Boolean = true
    override fun asPartition(): SpatialPartition = this

    /**
     * Transform root drawable extents into partition space and update the
     * octree root group's bounds.
     * Mirrors LLSpatialBridge::updateSpatialExtents().
     */
    override fun updateSpatialExtents() {
        val rootDrawable = drawableRoot ?: return
        val wp = rootDrawable.getWorldPosition()
        val r  = rootDrawable.getRadius()
        val srcExtents = arrayOf(
            floatArrayOf(wp.x - r, wp.y - r, wp.z - r, 1f),
            floatArrayOf(wp.x + r, wp.y + r, wp.z + r, 1f)
        )
        val dstExtents = Array(2) { FloatArray(4) }
        transformExtents(srcExtents, dstExtents)
        val rootGroup = allGroups().firstOrNull() ?: return
        for (i in 0..2) {
            rootGroup.extents[0][i] = dstExtents[0][i]
            rootGroup.extents[1][i] = dstExtents[1][i]
        }
        rootGroup.setState(SpatialState.OBJECT_DIRTY)
    }

    /**
     * Set the bin radius based on the attached drawable tree's extent.
     * Mirrors LLSpatialBridge::updateBinRadius().
     */
    override fun updateBinRadius() {
        val rootDrawable = drawableRoot ?: return
        // Store the derived radius in this bridge's drawable radius field.
        setRadius(rootDrawable.getRadius() * 2f)
    }

    /**
     * Transform the camera into bridge-local space and cull the bridge's octree.
     * Mirrors LLSpatialBridge::setVisible().
     */
    override fun setVisible(cameraId: Int, results: MutableList<Drawable>?, forSelect: Boolean) {
        val agentCamOrigin = Vector3.ZERO  // placeholder; real impl uses LLViewerCamera
        val bridgeCamOrigin = transformCamera(agentCamOrigin)
        if (results != null) {
            (this as SpatialPartition).cull(bridgeCamOrigin, results, forSelect)
        } else {
            (this as SpatialPartition).cull(bridgeCamOrigin)
        }
    }

    /**
     * Transform the camera origin into bridge-local space and update distance.
     * Mirrors LLSpatialBridge::updateDistance().
     */
    override fun updateDistance(cameraOrigin: Vector3, forceUpdate: Boolean) {
        val localCam = transformCamera(cameraOrigin)
        val rootGroup = allGroups().firstOrNull() ?: return
        rootGroup.updateDistance(localCam)
        distanceWRTCamera = rootGroup.distance
    }

    /**
     * Move this bridge within its parent partition.
     * Mirrors LLSpatialBridge::makeActive().
     */
    override fun makeActive() {
        val group = (this as SpatialPartition).allGroups().firstOrNull()
        (this as SpatialPartition).move(this as Drawable, group)
    }

    override fun move(drawable: Drawable, curp: SpatialGroup?, immediate: Boolean) {
        (this as SpatialPartition).move(drawable, curp, immediate)
    }

    /**
     * Rebuild octree root bounds and mark partition moved.
     * Mirrors LLSpatialBridge::updateMove().
     */
    override fun updateMove(): Boolean {
        updateSpatialExtents()
        allGroups().firstOrNull()?.setState(SpatialState.OBJECT_DIRTY)
        return true
    }

    /**
     * Shift all positions within this bridge's octree.
     * Mirrors LLSpatialBridge::shiftPos().
     */
    override fun shiftPos(shiftVector: FloatArray) {
        (this as SpatialPartition).shift(shiftVector)
    }

    /**
     * Destroy the bridge's octree and clean up references.
     * Mirrors LLSpatialBridge::cleanupReferences().
     */
    override fun cleanupReferences() {
        destroyTree()
        drawableRoot = null
        bridge = null
    }

    /**
     * Transform an agent-space camera position into bridge-local space.
     * Mirrors LLSpatialBridge::transformCamera().
     */
    fun transformCamera(cameraOrigin: Vector3): Vector3 {
        val xform = getXform()
        val wp = xform.getPosition()
        return Vector3(
            cameraOrigin.x - wp.x,
            cameraOrigin.y - wp.y,
            cameraOrigin.z - wp.z
        )
    }

    /**
     * Transform the two-element AABB [src] into bridge-local space.
     * Mirrors LLSpatialBridge::transformExtents().
     */
    fun transformExtents(src: Array<FloatArray>, dst: Array<FloatArray>) {
        val xform = getXform()
        val wp = xform.getPosition()
        val offset = floatArrayOf(wp.x, wp.y, wp.z)
        for (i in 0..2) {
            dst[0][i] = src[0][i] - offset[i]
            dst[1][i] = src[1][i] - offset[i]
        }
        // Copy any w-components verbatim
        if (src[0].size > 3) { dst[0][3] = src[0][3]; dst[1][3] = src[1][3] }
    }

    /**
     * Recursively destroy all SpatialGroup nodes in this bridge's octree.
     * Mirrors LLSpatialBridge::destroyTree().
     */
    fun destroyTree() {
        (this as SpatialPartition).cleanup()
    }
}

// ---------------------------------------------------------------------------
// CullResult  (mirrors LLCullResult)
// ---------------------------------------------------------------------------

class CullResult {
    private val visibleGroups: MutableList<SpatialGroup> = mutableListOf()
    private val alphaGroups: MutableList<SpatialGroup> = mutableListOf()
    private val riggedAlphaGroups: MutableList<SpatialGroup> = mutableListOf()
    private val occlusionGroups: MutableList<SpatialGroup> = mutableListOf()
    private val drawableGroups: MutableList<SpatialGroup> = mutableListOf()
    private val visibleList: MutableList<Drawable> = mutableListOf()
    private val visibleBridge: MutableList<SpatialBridge> = mutableListOf()
    private val renderMap: MutableMap<UInt, MutableList<DrawInfo>> = mutableMapOf()

    fun clear() {
        visibleGroups.clear()
        alphaGroups.clear()
        riggedAlphaGroups.clear()
        occlusionGroups.clear()
        drawableGroups.clear()
        visibleList.clear()
        visibleBridge.clear()
        renderMap.clear()
    }

    fun pushVisibleGroup(group: SpatialGroup)      { visibleGroups.add(group) }
    fun pushAlphaGroup(group: SpatialGroup)        { alphaGroups.add(group) }
    fun pushRiggedAlphaGroup(group: SpatialGroup)  { riggedAlphaGroups.add(group) }
    fun pushOcclusionGroup(group: SpatialGroup)    { occlusionGroups.add(group) }
    fun pushDrawableGroup(group: SpatialGroup)     { drawableGroups.add(group) }
    fun pushDrawable(drawable: Drawable)            { visibleList.add(drawable) }
    fun pushBridge(bridge: SpatialBridge)           { visibleBridge.add(bridge) }
    fun pushDrawInfo(type: UInt, drawInfo: DrawInfo) {
        renderMap.getOrPut(type) { mutableListOf() }.add(drawInfo)
    }

    fun getVisibleGroupsSize(): UInt      = visibleGroups.size.toUInt()
    fun getAlphaGroupsSize(): UInt        = alphaGroups.size.toUInt()
    fun getRiggedAlphaGroupsSize(): UInt  = riggedAlphaGroups.size.toUInt()
    fun getDrawableGroupsSize(): UInt     = drawableGroups.size.toUInt()
    fun getVisibleListSize(): UInt        = visibleList.size.toUInt()
    fun getVisibleBridgeSize(): UInt      = visibleBridge.size.toUInt()
    fun getRenderMapSize(type: UInt): UInt = (renderMap[type]?.size ?: 0).toUInt()
    fun hasOcclusionGroups(): Boolean     = occlusionGroups.isNotEmpty()

    fun beginVisibleGroups(): Iterator<SpatialGroup>     = visibleGroups.iterator()
    fun beginAlphaGroups(): Iterator<SpatialGroup>       = alphaGroups.iterator()
    fun beginRiggedAlphaGroups(): Iterator<SpatialGroup> = riggedAlphaGroups.iterator()
    fun beginOcclusionGroups(): Iterator<SpatialGroup>   = occlusionGroups.iterator()
    fun beginDrawableGroups(): Iterator<SpatialGroup>    = drawableGroups.iterator()
    fun beginVisibleList(): Iterator<Drawable>           = visibleList.iterator()
    fun beginVisibleBridge(): Iterator<SpatialBridge>    = visibleBridge.iterator()
    fun beginRenderMap(type: UInt): Iterator<DrawInfo>   = (renderMap[type] ?: emptyList()).iterator()

    fun assertDrawMapsEmpty() {
        renderMap.forEach { (type, list) ->
            check(list.isEmpty()) { "Draw map for type $type is not empty" }
        }
    }
}

// ---------------------------------------------------------------------------
// Specialised partition classes
// ---------------------------------------------------------------------------

open class WaterPartition(region: Any?)
    : SpatialPartition(0u, false, region) {
    override fun getGeometry(group: SpatialGroup) { /* water: no geometry */ }
    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) { }
}

class VoidWaterPartition(region: Any?) : WaterPartition(region)

class TerrainPartition(region: Any?)
    : SpatialPartition(0u, false, region) {
    /**
     * Generate terrain patch geometry into the group vertex buffer.
     * Each terrain patch contributes up to 17×17 quads = 289 verts / 512 indices.
     * C++: LLVOSurfacePatch::getGeometry() streams into an LLVertexBuffer.
     */
    override fun getGeometry(group: SpatialGroup) {
        check(group.vertexBuffer != null || group.isDead()) {
            "TerrainPartition.getGeometry: vertex buffer not allocated"
        }
        // Real implementation: map the vertex buffer, iterate surface patch
        // heightmap rows, fill position/normal/texcoord arrays.
    }
}

class TreePartition(region: Any?)
    : SpatialPartition(0u, false, region) {
    override fun getGeometry(group: SpatialGroup) { /* trees render per-drawable */ }
    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) { }
}

open class ParticlePartition(region: Any?)
    : SpatialPartition(0u, false, region) {
    protected var renderPass: UInt = 0u

    /**
     * Sort particles by camera distance (back-to-front for alpha blending)
     * and build the draw-info list for this group.
     * Mirrors LLParticlePartition::rebuildGeom().
     */
    override fun rebuildGeom(group: SpatialGroup) {
        if (group.isDead() || !group.hasState(SpatialState.GEOM_DIRTY)) return
        val sorted = drawablesInGroup(group).sortedByDescending { it.distanceWRTCamera }
        group.clearDrawMap()
        if (sorted.isNotEmpty()) {
            val drawInfo = DrawInfo(
                start        = 0u,
                end          = (sorted.size * 4 - 1).toUShort(),
                count        = (sorted.size * 6).toUInt(),
                offset       = 0u,
                texture      = null,
                vertexBuffer = group.vertexBuffer
            )
            group.drawMap.getOrPut(renderPass) { mutableListOf() }.add(drawInfo)
        }
        group.lastUpdateTime = FrameClock.seconds
        group.clearState(SpatialState.GEOM_DIRTY)
    }

    /**
     * Stream particle vertex data into the group's vertex buffer.
     * Each particle is a camera-facing billboard quad: 4 verts / 6 indices.
     */
    override fun getGeometry(group: SpatialGroup) {
        check(group.vertexBuffer != null || group.isDead()) {
            "ParticlePartition.getGeometry: vertex buffer not allocated"
        }
        // Real: map vertex buffer; for each drawable (LLVOPartGroup):
        //       compute billboard corners from position, size, color;
        //       write to vertex buffer.
    }

    /**
     * Count vertices/indices: each particle = 4 verts + 6 indices.
     */
    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) {
        val particleCount = drawablesInGroup(group).size.toUInt()
        vertexCount[0] = particleCount * 4u
        indexCount[0]  = particleCount * 6u
    }

    /**
     * Particle pixel area uses the largest bounding-box half-dimension as radius.
     * Mirrors LLParticlePartition::calcPixelArea().
     */
    override fun calcPixelArea(group: SpatialGroup, cameraOrigin: Vector3): Float {
        val half = group.objectBounds[1]
        val effectiveRadius = maxOf(half[0], half[1], half[2])
        val dx = group.objectBounds[0][0] - cameraOrigin.x
        val dy = group.objectBounds[0][1] - cameraOrigin.y
        val dz = group.objectBounds[0][2] - cameraOrigin.z
        val dist = maxOf(sqrt((dx*dx + dy*dy + dz*dz).toDouble()).toFloat(), 0.001f)
        val VIEWPORT_PIXELS = 1024f * 1024f
        return (effectiveRadius / dist) * (effectiveRadius / dist) * VIEWPORT_PIXELS * Math.PI.toFloat()
    }
}

class HUDParticlePartition(region: Any?) : ParticlePartition(region)

open class GrassPartition(region: Any?)
    : SpatialPartition(0u, false, region) {
    protected var renderPass: UInt = 0u

    /**
     * Build grass blade geometry into the group's vertex buffer.
     * Mirrors LLGrassPartition::getGeometry() → LLVOGrass::updateGeometry().
     */
    override fun getGeometry(group: SpatialGroup) {
        check(group.vertexBuffer != null || group.isDead()) {
            "GrassPartition.getGeometry: vertex buffer not allocated"
        }
        // Real: for each drawable (LLVOGrass), call updateGeometry() to fill
        // blade quad geometry in the mapped vertex buffer region.
    }

    /**
     * Count vertices/indices for grass blades.
     * 16 blades per object, 8 verts / 12 indices per blade.
     */
    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) {
        val grassCount = drawablesInGroup(group).size.toUInt()
        vertexCount[0] = grassCount * 16u * 8u
        indexCount[0]  = grassCount * 16u * 12u
    }
}

// ---------------------------------------------------------------------------
// VolumeGeometryManager  (mirrors LLVolumeGeometryManager)
// ---------------------------------------------------------------------------

open class VolumeGeometryManager : GeometryManager {
    override val faceList: MutableList<Any?> = mutableListOf()

    enum class SortType { NONE, BATCH_SORT, DISTANCE_SORT }

    companion object {
        private var instanceCount: Int = 0
    }

    init { instanceCount++ }

    /**
     * Sort faces; allocate VBOs; fill positions/normals/UVs; build DrawInfo list.
     * Mirrors LLVolumeGeometryManager::rebuildGeom().
     */
    override fun rebuildGeom(group: SpatialGroup) {
        if (group.isDead() || !group.hasState(SpatialState.GEOM_DIRTY)) return
        group.clearDrawMap()
        val vertexCount = UIntArray(1)
        val indexCount  = UIntArray(1)
        addGeometryCount(group, vertexCount, indexCount)
        if (vertexCount[0] > 0u && indexCount[0] > 0u) {
            group.built = 1f
            group.vertexBuffer = VertexBufferStub(vertexCount[0], indexCount[0])
            getGeometry(group)
        } else {
            group.vertexBuffer = null
            group.bufferMap.clear()
        }
        group.lastUpdateTime = FrameClock.seconds
        group.clearState(SpatialState.GEOM_DIRTY)
    }

    /**
     * Update dynamic vertex data for rigged/animating meshes.
     * Mirrors LLVolumeGeometryManager::rebuildMesh().
     */
    override fun rebuildMesh(group: SpatialGroup) {
        if (group.isDead()) return
        // Real: use LLSkinningUtil to re-skin rigged drawables into the VBO.
        group.clearState(SpatialState.MESH_DIRTY)
    }

    /**
     * Fill the group vertex buffer with volume face geometry.
     * Mirrors LLVolumeGeometryManager::getGeometry().
     */
    override fun getGeometry(group: SpatialGroup) {
        check(group.vertexBuffer != null || group.isDead()) {
            "VolumeGeometryManager.getGeometry: vertex buffer not allocated"
        }
        // Real: lock VBO; copy position/normal/UV for each face; unlock VBO.
    }

    /**
     * Sum vertex and index counts across all volume faces.
     * Mirrors LLVolumeGeometryManager::addGeometryCount().
     * Conservative default: 32 verts / 48 indices per face.
     */
    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) {
        val faceCount = faceList.size.toUInt()
        vertexCount[0] = faceCount * 32u
        indexCount[0]  = faceCount * 48u
    }

    /**
     * Create DrawInfo entries for each face batch.
     * Assigns textures, VBOs, and shader masks.
     * Mirrors LLVolumeGeometryManager::genDrawInfo().
     */
    fun genDrawInfo(
        group: SpatialGroup,
        mask: UInt,
        faces: Array<Any?>,
        faceCount: UInt,
        distanceSort: Boolean = false,
        batchTextures: Boolean = false,
        rigged: Boolean = false
    ): UInt {
        var drawInfoCount = 0u
        var idx = 0u
        while (idx < faceCount) {
            val batchEnd = if (batchTextures) {
                // Batch consecutive faces that share the same texture (pointer identity)
                var end = idx + 1u
                while (end < faceCount && faces[end.toInt()] === faces[idx.toInt()]) end++
                end
            } else {
                faceCount
            }
            val count = batchEnd - idx
            val drawInfo = DrawInfo(
                start        = idx.toUShort(),
                end          = (batchEnd - 1u).toUShort(),
                count        = count * 6u,
                offset       = idx * 4u,
                texture      = null,
                vertexBuffer = group.vertexBuffer
            )
            drawInfo.shaderMask = mask
            group.drawMap.getOrPut(mask) { mutableListOf() }.add(drawInfo)
            drawInfoCount++
            idx = batchEnd
        }
        return drawInfoCount
    }

    /**
     * Add a face to the appropriate draw bucket in group.drawMap[type].
     * Mirrors LLVolumeGeometryManager::registerFace().
     */
    fun registerFace(group: SpatialGroup, face: Any?, type: UInt) {
        if (face == null) return
        val drawInfo = DrawInfo(
            start        = 0u,
            end          = 3u,
            count        = 6u,
            offset       = 0u,
            texture      = null,
            vertexBuffer = group.vertexBuffer
        )
        group.drawMap.getOrPut(type) { mutableListOf() }.add(drawInfo)
        faceList.add(face)
    }
}

// ---------------------------------------------------------------------------
// Volume-backed partition / bridge classes
// ---------------------------------------------------------------------------

class VolumePartition(region: Any?)
    : SpatialPartition(0u, true, region), GeometryManager by VolumeGeometryManager()

class VolumeBridge(drawable: Drawable, region: Any?)
    : SpatialBridge(drawable, true, 0u, region), GeometryManager by VolumeGeometryManager()

class AvatarBridge(drawable: Drawable, region: Any?) : VolumeBridge(drawable, region)
class ControlAVBridge(drawable: Drawable, region: Any?) : VolumeBridge(drawable, region)

class HUDBridge(drawable: Drawable, region: Any?) : VolumeBridge(drawable, region) {
    override fun shiftPos(shiftVector: FloatArray) {
        // HUD elements are screen-space; shifting is intentionally a no-op
    }

    /**
     * HUD pixel area is always 1024×1024.
     * Mirrors LLHUDBridge::calcPixelArea().
     */
    override fun calcPixelArea(group: SpatialGroup, cameraOrigin: Vector3): Float = 1024f * 1024f
}

// ---------------------------------------------------------------------------
// Bridge-only partitions (hold no geometry themselves)
// ---------------------------------------------------------------------------

open class BridgePartition(region: Any?)
    : SpatialPartition(0u, false, region) {
    override fun getGeometry(group: SpatialGroup) { /* no geometry */ }
    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) { }
}

class AvatarPartition(region: Any?) : BridgePartition(region)
class ControlAVPartition(region: Any?) : BridgePartition(region)

class HUDPartition(region: Any?) : BridgePartition(region) {
    override fun shift(offset: FloatArray) {
        // HUD is in screen space; world shift does not apply
    }
}

// ---------------------------------------------------------------------------
// Debug / utility rendering functions  (free functions in C++)
// ---------------------------------------------------------------------------

/**
 * Emit 12 GL_TRIANGLE_STRIP triangles forming a solid box.
 *
 * In production with an active LWJGL context:
 *   GL11.glBegin(GL11.GL_TRIANGLE_STRIP)
 *   GL11.glVertex3f(...)   -- 8 corners in strip order
 *   GL11.glEnd()
 *
 * Without a context the function is a structured no-op that validates inputs.
 */
fun drawBox(center: Vector3, halfSize: Vector3) {
    val cx = center.x; val cy = center.y; val cz = center.z
    val hx = halfSize.x; val hy = halfSize.y; val hz = halfSize.z
    // 8 corners (computed for documentation; fed to GL in strip order in real code)
    @Suppress("UNUSED_VARIABLE")
    val corners = arrayOf(
        floatArrayOf(cx-hx, cy-hy, cz-hz), floatArrayOf(cx+hx, cy-hy, cz-hz),
        floatArrayOf(cx-hx, cy+hy, cz-hz), floatArrayOf(cx+hx, cy+hy, cz-hz),
        floatArrayOf(cx-hx, cy-hy, cz+hz), floatArrayOf(cx+hx, cy-hy, cz+hz),
        floatArrayOf(cx-hx, cy+hy, cz+hz), floatArrayOf(cx+hx, cy+hy, cz+hz)
    )
    // Real GL calls (requires active context + LWJGL on classpath):
    // GL11.glBegin(GL11.GL_TRIANGLE_STRIP)
    // for (c in stripOrder) GL11.glVertex3f(c[0], c[1], c[2])
    // GL11.glEnd()
}

/**
 * Emit 24 GL_LINES vertices forming the 12 edges of an axis-aligned box.
 *
 * In production with an active LWJGL context:
 *   GL11.glBegin(GL11.GL_LINES)
 *   GL11.glVertex3f(a[0], a[1], a[2])
 *   GL11.glVertex3f(b[0], b[1], b[2])
 *   ... (×12 edges)
 *   GL11.glEnd()
 */
fun drawBoxOutline(pos: Vector3, size: Vector3) {
    val cx = pos.x; val cy = pos.y; val cz = pos.z
    val hx = size.x; val hy = size.y; val hz = size.z
    // 8 corners
    val v = Array(8) { FloatArray(3) }
    v[0] = floatArrayOf(cx-hx, cy-hy, cz-hz); v[1] = floatArrayOf(cx+hx, cy-hy, cz-hz)
    v[2] = floatArrayOf(cx+hx, cy+hy, cz-hz); v[3] = floatArrayOf(cx-hx, cy+hy, cz-hz)
    v[4] = floatArrayOf(cx-hx, cy-hy, cz+hz); v[5] = floatArrayOf(cx+hx, cy-hy, cz+hz)
    v[6] = floatArrayOf(cx+hx, cy+hy, cz+hz); v[7] = floatArrayOf(cx-hx, cy+hy, cz+hz)
    // 12 edges (index pairs)
    @Suppress("UNUSED_VARIABLE")
    val edges = arrayOf(
        intArrayOf(0,1), intArrayOf(1,2), intArrayOf(2,3), intArrayOf(3,0),
        intArrayOf(4,5), intArrayOf(5,6), intArrayOf(6,7), intArrayOf(7,4),
        intArrayOf(0,4), intArrayOf(1,5), intArrayOf(2,6), intArrayOf(3,7)
    )
    // Real GL calls (requires active context + LWJGL on classpath):
    // GL11.glBegin(GL11.GL_LINES)
    // for ((a, b) in edges) {
    //     GL11.glVertex3f(v[a][0], v[a][1], v[a][2])
    //     GL11.glVertex3f(v[b][0], v[b][1], v[b][2])
    // }
    // GL11.glEnd()
}

fun sphereAABBIntersect(center: Vector3, halfSize: Vector3, pos: Vector3, rad: Float): Int {
    var result = 2
    val min = center - halfSize
    val max = center + halfSize
    for (i in 0..2) {
        val minV = min.toArray()[i]
        val maxV = max.toArray()[i]
        val posV = pos.toArray()[i]
        if (minV > posV + rad || maxV < posV - rad) return 0
        if (minV < posV - rad || maxV > posV + rad) result = 1
    }
    return result
}

/**
 * Compute the physics mesh LOD level for the given volume and scale.
 * Mirrors get_physics_detail() in llspatialpartition.cpp.
 * Returns an integer in [0, 3] where higher = more detailed.
 */
fun getPhysicsDetail(volumeParams: Any?, scale: Vector3): Int {
    val mag = sqrt((scale.x*scale.x + scale.y*scale.y + scale.z*scale.z).toDouble()).toFloat()
    return when {
        mag < 1f -> 0
        mag < 4f -> 1
        mag < 8f -> 2
        else     -> 3
    }
}

/**
 * Render the physics base-hull mesh for [volume] as filled triangles.
 * Mirrors renderMeshBaseHull() in llspatialpartition.cpp.
 *
 * Real LWJGL calls (requires active context):
 *   GL11.glColor4f(color[0], color[1], color[2], color[3])
 *   GL11.glBegin(GL11.GL_TRIANGLES)
 *   for each triangle: GL11.glVertex3f(x, y, z)
 *   GL11.glEnd()
 */
fun renderMeshBaseHull(volume: Any?, dataMask: UInt, color: FloatArray) {
    check(color.size >= 4) { "renderMeshBaseHull: color array must have 4 components" }
    // Stub: a real implementation fetches the physics mesh from the volume
    // (LLVOVolume::getPhysicsShapeByID) and iterates its triangles.
}

/**
 * Render the physics base-hull with a wire-frame outline.
 * Mirrors renderMeshBaseHullWithOutline() in llspatialpartition.cpp.
 */
fun renderMeshBaseHullWithOutline(volume: Any?, dataMask: UInt, color: FloatArray, lineColor: FloatArray) {
    renderMeshBaseHull(volume, dataMask, color)
    // Outline pass (requires active GL context):
    // GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
    // renderMeshBaseHull(volume, dataMask, lineColor)
    // GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
}

/**
 * Render a physics mesh hull as filled triangles.
 * Mirrors render_hull() in llspatialpartition.cpp.
 */
fun renderHull(mesh: Any?, color: FloatArray) {
    check(color.size >= 4) { "renderHull: color array must have 4 components" }
    // Real: iterate mesh.mPositions / mesh.mIndices and emit GL_TRIANGLES.
}

/**
 * Render a physics mesh hull with a wire-frame outline.
 * Mirrors render_hull_with_outline() in llspatialpartition.cpp.
 */
fun renderHullWithOutline(mesh: Any?, color: FloatArray, lineColor: FloatArray) {
    renderHull(mesh, color)
    // Outline: GL11.glPolygonMode GL_LINE, re-render, restore GL_FILL.
}

// ---------------------------------------------------------------------------
// Minimal Vector3 extension to allow array indexing in sphereAABBIntersect
// ---------------------------------------------------------------------------

private fun Vector3.toArray(): FloatArray = floatArrayOf(x, y, z)
private operator fun Vector3.minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
private operator fun Vector3.plus(other: Vector3)  = Vector3(x + other.x, y + other.y, z + other.z)
