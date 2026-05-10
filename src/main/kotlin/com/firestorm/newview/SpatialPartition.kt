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

    fun validate() { TODO("GPU: assert vertexBuffer != null and index range is valid") }

    fun getSkinHash(): Long {
        TODO("GPU: return skinInfo?.hash ?: 0L")
    }

    fun getDebugColor(): Int {
        TODO("GPU: derive a debug RGBA colour from a hash of this DrawInfo's identity")
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
}

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

    init {
        nodeCount++
        setState(SG_INITIAL_STATE_MASK)
        TODO("GPU: gPipeline.markRebuild(this); register with reflection map manager")
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

    private fun traverseSetState(bits: UInt) {
        TODO("GPU: octree-traverse and set state bits on every descendant group")
    }
    private fun traverseSetStateDiff(bits: UInt) {
        TODO("GPU: octree-traverse, set state bits only on groups that do NOT already have them")
    }
    private fun traverseClearState(bits: UInt) {
        TODO("GPU: octree-traverse and clear state bits on every descendant group")
    }
    private fun traverseClearStateDiff(bits: UInt) {
        TODO("GPU: octree-traverse, clear state bits only on groups that currently have them")
    }

    fun dirtyGeom() { setState(SpatialState.GEOM_DIRTY) }
    fun dirtyMesh() { setState(SpatialState.MESH_DIRTY) }

    fun isDead(): Boolean = isState(SpatialState.DEAD)
    fun isEmpty(): Boolean {
        TODO("GPU: return octreeNode.getElementCount() == 0")
    }

    fun isDirty(): Boolean {
        TODO("GPU: return LLViewerOctreeGroup.isDirty() — DIRTY flag check")
    }

    fun isVisible(): Boolean {
        TODO("GPU: check octree entry visibility stamp vs current frame counter")
    }

    fun isOcclusionState(bits: UInt): Boolean {
        TODO("GPU: check LLOcclusionCullingGroup occlusion flags")
    }

    fun setOcclusionState(bits: UInt, mode: Int) {
        TODO("GPU: set occlusion state bits; mode selects single/all-cameras scope")
    }

    fun checkOcclusion() {
        TODO("GPU: issue or check an occlusion query for this group")
    }

    fun releaseOcclusionQueryObjectNames() {
        TODO("GPU: delete GL occlusion query objects for all camera slots")
    }

    // ------------------------------------------------------------------
    // Octree node accessors
    // ------------------------------------------------------------------

    fun getOctreeNode(): Any = octreeNode
    fun getParent(): SpatialGroup? {
        TODO("GPU: return (SpatialGroup?) LLViewerOctreeGroup.getParent()")
    }

    // ------------------------------------------------------------------
    // Draw-map management
    // ------------------------------------------------------------------

    fun clearDrawMap() { drawMap.clear() }

    fun validate() {
        TODO("GPU: assert-check all drawable/group invariants in paranoia mode")
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
        setOcclusionState(TODO("DISCARD_QUERY") as UInt, TODO("STATE_MODE_ALL_CAMERAS") as Int)
        TODO("GPU: gPipeline.markRebuild(this)")
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
            TODO("GPU: gPipeline.markRebuild(this)")
            if (drawable.isSpatialBridge()) {
                bridgeList.removeAll { it === drawable }
            }
            if (isEmpty()) clearDrawMap()
        }
        return true
    }

    fun updateInGroup(drawable: Drawable, immediate: Boolean = false): Boolean {
        drawable.updateSpatialExtents()
        TODO("GPU: check if drawable still fits within this octree node; return true if so and mark OBJECT_DIRTY")
    }

    fun expandExtents(addingExtents: Array<FloatArray>, currentTransform: XformMatrix) {
        TODO("GPU: rotate all 8 corners of addingExtents into currentTransform local space; expand mExtents; recompute bounds centre/size")
    }

    fun shift(offset: FloatArray) {
        TODO("GPU: translate octreeNode centre; shift bounds/extents/objectBounds by offset; conditionally mark GEOM_DIRTY")
    }

    // ------------------------------------------------------------------
    // Distance / LOD
    // ------------------------------------------------------------------

    fun updateDistance(cameraOrigin: Vector3) {
        TODO("GPU: compute radius from objectBounds; call spatialPartition.calcDistance/calcPixelArea")
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

    fun needsUpdate(): Boolean {
        TODO("GPU: return true if the group's visible stamp is stale vs current frame")
    }

    // ------------------------------------------------------------------
    // Rebuild
    // ------------------------------------------------------------------

    fun rebuildGeom() {
        if (!isDead()) {
            spatialPartition.rebuildGeom(this)
            if (hasState(SpatialState.MESH_DIRTY)) {
                TODO("GPU: gPipeline.markMeshDirty(this)")
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
            TODO("GPU: gPipeline.markRebuild(this)")
        }
        lastUpdateTime = TODO("GPU: gFrameTimeSeconds") as Float
        vertexBuffer = null
        bufferMap.clear()
        clearDrawMap()
        if (!keepOcclusion) releaseOcclusionQueryObjectNames()
        TODO("GPU: clear all face vertex buffers for every drawable in this group")
    }

    // ------------------------------------------------------------------
    // Line-segment intersection
    // ------------------------------------------------------------------

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
        TODO("GPU: iterate drawables in this group; test each face's AABB and geometry; return closest hit")
    }

    // ------------------------------------------------------------------
    // Debug drawing
    // ------------------------------------------------------------------

    fun drawObjectBox(color: FloatArray) {
        TODO("GPU: draw a wire-frame box around objectBounds with the given color")
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
        TODO("GPU: call super.handleRemoval(node, entry)")
    }

    fun handleDestruction(node: Any) {
        if (isDead()) return
        setState(SpatialState.DEAD)
        TODO("GPU: null out group reference on every entry; clearDrawMap; set vertexBuffer null; sZombieGroups++; mOctreeNode = null")
    }

    fun handleChildAddition(parent: Any, child: Any) {
        SpatialGroup(child, spatialPartition)
        unbound()
        TODO("GPU: assert_states_valid(this)")
    }

    fun rebound() {
        if (!isDirty()) return
        TODO("GPU: super.rebound(); if CONTROL_AV partition, expandExtents for control avatar drawable")
    }

    fun unbound() {
        TODO("GPU: clear DIRTY flag propagation in parent octree group chain")
    }

    fun isHUDGroup(): Boolean = spatialPartition.isHUDPartition()

    // ------------------------------------------------------------------
    // Destructor logic
    // ------------------------------------------------------------------

    fun destroy() {
        if (isDead()) TODO("GPU: sZombieGroups--")
        nodeCount--
        clearDrawMap()
        TODO("GPU: check pipeline references in debug; release occlusion queries")
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

    init {
        TODO("GPU: create root octree node; new SpatialGroup(mOctree, this)")
    }

    // ------------------------------------------------------------------
    // Insert / remove
    // ------------------------------------------------------------------

    fun put(drawable: Drawable, wasVisible: Boolean = false): SpatialGroup? {
        drawable.updateSpatialExtents()
        if (drawable.getSpatialGroup() == null) {
            TODO("GPU: mOctree.insert(drawable.getEntry())")
        }
        val group = drawable.getSpatialGroup()
        if (group != null && wasVisible &&
            group.isOcclusionState(TODO("QUERY_PENDING") as UInt)
        ) {
            group.setOcclusionState(
                TODO("DISCARD_QUERY") as UInt,
                STATE_MODE_ALL_CAMERAS
            )
        }
        return group
    }

    fun remove(drawable: Drawable, curp: SpatialGroup): Boolean {
        if (!curp.removeObject(drawable)) {
            error("Failed to remove drawable from octree")
        } else {
            drawable.setGroup(null)
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
        TODO("GPU: octree-traverse and call shift(offset) on every SpatialGroup")
    }

    // ------------------------------------------------------------------
    // Distance / pixel-area
    // ------------------------------------------------------------------

    open fun calcDistance(group: SpatialGroup, cameraOrigin: Vector3): Float {
        val eye = FloatArray(3) {
            group.objectBounds[0][it] - cameraOrigin.toArray()[it]
        }
        var dist = sqrt((eye[0]*eye[0] + eye[1]*eye[1] + eye[2]*eye[2]).toDouble()).toFloat()

        if (group.drawMap.containsKey(TODO("PASS_ALPHA") as UInt)) {
            TODO("GPU: compute depth for alpha-sort using at-axis projection; update ALPHA_DIRTY if view angle changed enough")
        }

        if (dist < 16f) {
            dist = dist / 16f
            dist *= dist
            dist *= 16f
        }
        return dist
    }

    open fun calcPixelArea(group: SpatialGroup, cameraOrigin: Vector3): Float {
        TODO("GPU: LLPipeline.calcPixelArea(group.objectBounds[0], group.objectBounds[1], camera)")
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
            TODO("GPU: allocate/reuse LLVertexBuffer for this group; call getGeometry(group)")
        } else {
            group.vertexBuffer = null
            group.bufferMap.clear()
        }
        group.lastUpdateTime = TODO("GPU: gFrameTimeSeconds") as Float
        group.clearState(SpatialState.GEOM_DIRTY)
    }

    override fun rebuildMesh(group: SpatialGroup) { /* base no-op */ }

    override fun getGeometry(group: SpatialGroup) {
        TODO("GPU: subclasses fill vertex buffers with geometry for this group")
    }

    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) {
        TODO("GPU: sum up vertex/index requirements across all drawables in this group")
    }

    // ------------------------------------------------------------------
    // Culling
    // ------------------------------------------------------------------

    fun cull(cameraOrigin: Vector3, doOcclusion: Boolean = false): Int {
        TODO("GPU: rebound root group; select correct LLOctreeCull variant; traverse octree")
    }

    fun cull(cameraOrigin: Vector3, results: MutableList<Drawable>, forSelect: Boolean): Int {
        TODO("GPU: rebound root group; use LLOctreeSelect traverser; populate results")
    }

    fun visibleObjectsInFrustum(cameraOrigin: Vector3): Boolean {
        TODO("GPU: LLOctreeCullDetectVisible traversal; return mResult")
    }

    fun isVisible(v: Vector3): Boolean {
        TODO("GPU: check if point v is inside the camera frustum")
    }

    fun isHUDPartition(): Boolean {
        TODO("GPU: return partitionType == LLViewerRegion.PARTITION_HUD")
    }

    fun isBridge(): Boolean = bridge != null
    fun asBridge(): SpatialBridge? = bridge

    fun getVisibleExtents(cameraOrigin: Vector3, visMin: Vector3, visMax: Vector3): Boolean {
        TODO("GPU: LLOctreeCullVisExtents traversal; set visMin/visMax; return mEmpty")
    }

    // ------------------------------------------------------------------
    // Debug rendering
    // ------------------------------------------------------------------

    fun renderPhysicsShapes(depthOnly: Boolean) {
        TODO("GPU: traverse octree and render physics AABB/mesh shapes for debug")
    }

    fun renderDebug() {
        TODO("GPU: draw bounding boxes, normals, visibility indicators for debug overlay")
    }

    fun renderIntersectingBBoxes(cameraOrigin: Vector3) {
        TODO("GPU: draw AABBs of groups that intersect the camera frustum")
    }

    fun restoreGL() { /* no-op — GL resources recreated on next rebuild */ }

    // ------------------------------------------------------------------
    // Cleanup
    // ------------------------------------------------------------------

    fun cleanup() {
        TODO("GPU: delete octree; free all SpatialGroup and DrawInfo allocations")
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

    init { TODO("GPU: set mBridge = this on the SpatialPartition side") }

    override fun isSpatialBridge(): Boolean = true
    override fun asPartition(): SpatialPartition = this

    override fun updateSpatialExtents() {
        TODO("GPU: transform root drawable extents into partition space; update octree root bounds")
    }

    override fun updateBinRadius() {
        TODO("GPU: setBinRadius based on extent of attached drawable tree")
    }

    override fun setVisible(cameraId: Int, results: MutableList<Drawable>?, forSelect: Boolean) {
        TODO("GPU: transform camera into bridge space; call LLSpatialPartition.cull with transformed camera")
    }

    override fun updateDistance(cameraOrigin: Vector3, forceUpdate: Boolean) {
        TODO("GPU: transform camera origin into bridge-local space; update distanceWRTCamera")
    }

    override fun makeActive() {
        TODO("GPU: call SpatialPartition.move(this, getSpatialGroup())")
    }

    override fun move(drawable: Drawable, curp: SpatialGroup?, immediate: Boolean) {
        TODO("GPU: super.move(drawable, curp, immediate)")
    }

    override fun updateMove(): Boolean {
        TODO("GPU: rebuild octree root bounds; mark partition moved")
    }

    override fun shiftPos(shiftVector: FloatArray) {
        TODO("GPU: shift all positions within this bridge's octree")
    }

    override fun cleanupReferences() {
        TODO("GPU: destroyTree(); super.cleanupReferences()")
    }

    fun transformCamera(cameraOrigin: Vector3): Vector3 {
        TODO("GPU: transform agent-space camera position/orientation into this bridge's local frame")
    }

    fun transformExtents(src: Array<FloatArray>, dst: Array<FloatArray>) {
        TODO("GPU: transform the two-element AABB src into bridge local space and write to dst")
    }

    fun destroyTree() {
        TODO("GPU: recursively destroy all SpatialGroup nodes in this bridge's octree")
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
// Each mirrors a C++ class; geometry methods stub to TODO
// ---------------------------------------------------------------------------

open class WaterPartition(region: Any?)
    : SpatialPartition(0u, false, region) {
    override fun getGeometry(group: SpatialGroup) { /* water: no geometry */ }
    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) { }
}

class VoidWaterPartition(region: Any?) : WaterPartition(region)

class TerrainPartition(region: Any?)
    : SpatialPartition(0u, false, region) {
    override fun getGeometry(group: SpatialGroup) {
        TODO("GPU: generate terrain patch geometry into group vertex buffer")
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

    override fun rebuildGeom(group: SpatialGroup) {
        TODO("GPU: sort particles by camera distance; build draw-info list for render pass")
    }
    override fun getGeometry(group: SpatialGroup) {
        TODO("GPU: stream particle vertex data into group vertex buffer")
    }
    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) {
        TODO("GPU: count vertices/indices needed for all particles in this group")
    }
    override fun calcPixelArea(group: SpatialGroup, cameraOrigin: Vector3): Float {
        TODO("GPU: particle-specific pixel-area calculation based on particle sizes")
    }
}

class HUDParticlePartition(region: Any?) : ParticlePartition(region)

open class GrassPartition(region: Any?)
    : SpatialPartition(0u, false, region) {
    protected var renderPass: UInt = 0u

    override fun getGeometry(group: SpatialGroup) {
        TODO("GPU: build grass blade geometry into group vertex buffer")
    }
    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) {
        TODO("GPU: count vertices/indices for grass blades in this group")
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

    override fun rebuildGeom(group: SpatialGroup) {
        TODO("GPU: sort faces; allocate VBOs; fill positions/normals/UVs; build DrawInfo list")
    }

    override fun rebuildMesh(group: SpatialGroup) {
        TODO("GPU: update dynamic vertex data for rigged/animating meshes in this group")
    }

    override fun getGeometry(group: SpatialGroup) {
        TODO("GPU: fill the group vertex buffer with volume face geometry")
    }

    override fun addGeometryCount(group: SpatialGroup, vertexCount: UIntArray, indexCount: UIntArray) {
        TODO("GPU: iterate volume faces; sum vertex and index counts; account for rigged batches")
    }

    fun genDrawInfo(
        group: SpatialGroup,
        mask: UInt,
        faces: Array<Any?>,
        faceCount: UInt,
        distanceSort: Boolean = false,
        batchTextures: Boolean = false,
        rigged: Boolean = false
    ): UInt {
        TODO("GPU: create DrawInfo entries for each face batch; assign textures, VBOs, shader masks")
    }

    fun registerFace(group: SpatialGroup, face: Any?, type: UInt) {
        TODO("GPU: add face to appropriate draw bucket in group.drawMap[type]")
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
    override fun calcPixelArea(group: SpatialGroup, cameraOrigin: Vector3): Float {
        TODO("GPU: HUD pixel area is 1024×1024 always — return fixed value")
    }
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

fun drawBox(center: Vector3, halfSize: Vector3) {
    TODO("GPU: emit 12 GL_TRIANGLE_STRIP triangles forming a solid box")
}

fun drawBoxOutline(pos: Vector3, size: Vector3) {
    TODO("GPU: emit 24 GL_LINES forming the edges of an axis-aligned box")
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

fun getPhysicsDetail(volumeParams: Any?, scale: Vector3): Int {
    TODO("GPU: compute physics mesh LOD based on volume params and scale")
}

fun renderMeshBaseHull(volume: Any?, dataMask: UInt, color: FloatArray) {
    TODO("GPU: render the physics base-hull mesh for the given volume with solid color")
}

fun renderMeshBaseHullWithOutline(volume: Any?, dataMask: UInt, color: FloatArray, lineColor: FloatArray) {
    TODO("GPU: render the physics base-hull mesh with a wire-frame outline")
}

fun renderHull(mesh: Any?, color: FloatArray) {
    TODO("GPU: render a physics mesh hull as triangles with the given color")
}

fun renderHullWithOutline(mesh: Any?, color: FloatArray, lineColor: FloatArray) {
    TODO("GPU: render a physics mesh hull with a wire-frame outline")
}

// ---------------------------------------------------------------------------
// Minimal Vector3 extension to allow array indexing in sphereAABBIntersect
// (bridge to however the shared Vector3 type exposes its components)
// ---------------------------------------------------------------------------

private fun Vector3.toArray(): FloatArray = floatArrayOf(x, y, z)
private operator fun Vector3.minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
private operator fun Vector3.plus(other: Vector3)  = Vector3(x + other.x, y + other.y, z + other.z)
