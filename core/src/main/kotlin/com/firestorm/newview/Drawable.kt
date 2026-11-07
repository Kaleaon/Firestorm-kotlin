package com.firestorm.newview

import com.firestorm.llmath.Vector3
import kotlin.math.min

// ---------------------------------------------------------------------------
// XformMatrix — lightweight stand-in for LLXformMatrix
// Holds a drawable's position/rotation/scale in world space.
// ---------------------------------------------------------------------------

class XformMatrix {
    var position: Vector3 = Vector3.ZERO
    var worldPosition: Vector3 = Vector3.ZERO
    var rotation: FloatArray = FloatArray(4)      // quaternion xyzw
    var worldRotation: FloatArray = FloatArray(4)
    var scale: Vector3 = Vector3(1f, 1f, 1f)

    fun getPosition(): Vector3 = position
    fun getPositionW(): Vector3 = worldPosition
    fun getRotation(): FloatArray = rotation
    fun getWorldRotation(): FloatArray = worldRotation
    fun getScale(): Vector3 = scale
    fun isRoot(): Boolean = true // simplified; real impl checks parent chain

    fun setPosition(v: Vector3) { position = v }
    fun setRotation(q: FloatArray) { rotation = q }
    fun setScale(v: Vector3) { scale = v }
    fun updateMatrix() {
        System.err.println("GPU: recompute world matrix from position/rotation/scale")
    }

    fun getMinMax(min: Vector3, max: Vector3) {
        System.err.println("GPU: compute AABB corners from this transform's extent")
    }
}

// ---------------------------------------------------------------------------
// Face — minimal stand-in for LLFace
// ---------------------------------------------------------------------------

class Face(
    val drawable: Drawable,
    var teOffset: Int = 0
) {
    var centerLocal: Vector3 = Vector3.ZERO
    var centerAgent: Vector3 = Vector3.ZERO
    var distance: Float = 0f
    val extents: Array<FloatArray> = arrayOf(FloatArray(4), FloatArray(4))

    var inAlphaPool: Boolean = false

    companion object {
        const val FULLBRIGHT: UInt = 0x1u
    }

    private var faceState: UInt = 0u

    fun setState(bits: UInt) { faceState = faceState or bits }
    fun clearState(bits: UInt) { faceState = faceState and bits.inv() }
    fun isState(bits: UInt): Boolean = (faceState and bits) != 0u

    fun isInAlphaPool(): Boolean = inAlphaPool

    fun updateCenterAgent() {
        System.err.println("GPU: recompute face centre in agent space")
    }
    fun clearVertexBuffer() {
        System.err.println("GPU: release face vertex buffer reference")
    }
    fun hasGeometry(): Boolean {
        System.err.println("GPU: return true if this face has an allocated vertex buffer")
        return false
    }
    fun renderIndexed() {
        System.err.println("GPU: issue indexed draw call for this face")
    }
    fun verify(): Boolean {
        System.err.println("GPU: sanity-check face vertex buffer state")
        return false
    }

    fun setPool(pool: Any?, texture: Any?) {
        System.err.println("GPU: attach this face to draw pool $pool with texture")
    }
    fun setTexture(texture: Any?) {
        System.err.println("GPU: assign primary texture to this face")
    }
    fun setNormalMap(normal: Any?) {
        System.err.println("GPU: assign normal-map texture to this face")
    }
    fun setSpecularMap(specular: Any?) {
        System.err.println("GPU: assign specular-map texture to this face")
    }
    fun setPoolType(type: Int) {
        System.err.println("GPU: set draw-pool type classification for this face")
    }
    fun setTEOffset(offset: Int) { teOffset = offset }
    fun setDrawable(d: Drawable) { /* ownership transfer: see mergeFaces */ }
}

// ---------------------------------------------------------------------------
// Drawable  (mirrors LLDrawable)
//
// Holds all per-object rendering state: spatial placement, LOD distance,
// face list, state flags, and the link back to the viewer object.
// ---------------------------------------------------------------------------

class Drawable(
    val viewerObject: ViewerObject?,
    newEntry: Boolean = false
) {
    // ------------------------------------------------------------------
    // Constants — EDrawableFlags
    // ------------------------------------------------------------------

    companion object {
        const val IN_REBUILD_Q:    UInt = 0x00000001u
        const val EARLY_MOVE:      UInt = 0x00000004u
        const val MOVE_UNDAMPED:   UInt = 0x00000008u
        const val ON_MOVE_LIST:    UInt = 0x00000010u
        const val UV:              UInt = 0x00000020u
        const val UNLIT:           UInt = 0x00000040u
        const val LIGHT:           UInt = 0x00000080u
        const val REBUILD_VOLUME:  UInt = 0x00000100u
        const val REBUILD_TCOORD:  UInt = 0x00000200u
        const val REBUILD_COLOR:   UInt = 0x00000400u
        const val REBUILD_POSITION:UInt = 0x00000800u
        const val REBUILD_GEOMETRY:UInt = REBUILD_POSITION or REBUILD_TCOORD or REBUILD_COLOR
        const val REBUILD_MATERIAL:UInt = REBUILD_TCOORD or REBUILD_COLOR
        const val REBUILD_ALL:     UInt = REBUILD_GEOMETRY or REBUILD_VOLUME
        const val REBUILD_RIGGED:  UInt = 0x00001000u
        const val ON_SHIFT_LIST:   UInt = 0x00002000u
        const val ACTIVE:          UInt = 0x00004000u
        const val DEAD:            UInt = 0x00008000u
        const val INVISIBLE:       UInt = 0x00010000u
        const val NEARBY_LIGHT:    UInt = 0x00020000u
        const val BUILT:           UInt = 0x00040000u
        const val FORCE_INVISIBLE: UInt = 0x00080000u
        const val HAS_ALPHA:       UInt = 0x00100000u
        const val RIGGED:          UInt = 0x00200000u
        const val RIGGED_CHILD:    UInt = 0x00400000u
        const val PARTITION_MOVE:  UInt = 0x00800000u
        const val ANIMATED_CHILD:  UInt = 0x01000000u
        const val ACTIVE_CHILD:    UInt = 0x02000000u
        const val FOR_UNLOAD:      UInt = 0x04000000u
        const val MIRROR:          UInt = 0x08000000u

        const val SILHOUETTE_HIGHLIGHT: UInt = 0u

        const val MIN_INTERPOLATE_DISTANCE_SQUARED: Float = 0.001f * 0.001f
        const val MAX_INTERPOLATE_DISTANCE_SQUARED: Float = 10f * 10f
        const val OBJECT_DAMPING_TIME_CONSTANT: Float = 0.06f
        const val FORCE_INVISIBLE_AREA: Float = 16f

        var curPixelAngle: Float = 0f          // current pixels per radian
        var numZombieDrawables: UInt = 0u
        private val deadList: MutableList<Drawable> = mutableListOf()

        fun initClass() { /* no-op; static initialisation done in companion */ }

        fun incrementVisible() {
            System.err.println("GPU: advance the current-frame visible counter; update curPixelAngle from window height / camera FOV")
        }

        fun cleanupDeadDrawables() {
            deadList.clear()
        }
    }

    // ------------------------------------------------------------------
    // Spatial / transform data
    // ------------------------------------------------------------------

    val xform: XformMatrix = XformMatrix()
    var parent: Drawable? = null
    var distanceWRTCamera: Float = 0f

    // ------------------------------------------------------------------
    // Private state
    // ------------------------------------------------------------------

    private var state: UInt = 0u
    private var renderType: Int = 0
    private var generation: Int = -1
    private var radius: Float = 0f
    private val faces: MutableList<Face> = mutableListOf()
    private var spatialBridge: Drawable? = null  // actually LLSpatialBridge
    private var currentScale: Vector3 = Vector3(1f, 1f, 1f)

    init { init(newEntry) }

    // ------------------------------------------------------------------
    // Initialisation / destruction
    // ------------------------------------------------------------------

    fun init(newEntry: Boolean) {
        parent = null
        renderType = 0
        currentScale = Vector3(1f, 1f, 1f)
        distanceWRTCamera = 0f
        state = 0u
        radius = 0f
        generation = -1
        spatialBridge = null
        System.err.println("APR: resolve octree entry from VOCache if !newEntry; call setOctreeEntry")
    }

    fun unload() {
        System.err.println("GPU: call setNoLOD and markForUpdate on the associated VOVolume")
    }

    fun destroy() {
        if (isDead()) numZombieDrawables--
        faces.clear()
        System.err.println("GPU: check pipeline references in debug; remove from octree")
    }

    // ------------------------------------------------------------------
    // State flags
    // ------------------------------------------------------------------

    fun getState(): UInt = state
    fun isState(bits: UInt): Boolean = (state and bits) != 0u
    fun setState(bits: UInt) { state = state or bits }
    fun clearState(bits: UInt) { state = state and bits.inv() }

    // ------------------------------------------------------------------
    // Lifecycle predicates
    // ------------------------------------------------------------------

    fun isDead(): Boolean = isState(DEAD)
    fun isNew(): Boolean = !isState(BUILT)
    fun isUnload(): Boolean = isState(FOR_UNLOAD)
    fun isActive(): Boolean = isState(ACTIVE)
    fun isStatic(): Boolean = !isActive()
    fun isAnimating(): Boolean {
        System.err.println("Check viewerObject angular velocity and linear velocity flags")
        return false
    }

    fun isLight(): Boolean {
        return viewerObject?.isLight() ?: false
    }

    fun isAvatar(): Boolean = viewerObject?.isAvatar() ?: false

    fun isRoot(): Boolean = parent == null || parent!!.isAvatar()
    fun isSpatialRoot(): Boolean = isRoot()
    open fun isSpatialBridge(): Boolean = false
    open fun asPartition(): SpatialPartition? = null

    // ------------------------------------------------------------------
    // Face access
    // ------------------------------------------------------------------

    fun getFace(i: Int): Face? {
        if (i < 0 || i >= faces.size) {
            if (faces.isEmpty()) return null
            // Return face 0 as a fallback to avoid NPE on callers (mirrors C++ behaviour)
            return faces[0]
        }
        return faces[i]
    }

    fun getNumFaces(): Int = faces.size
    fun getFaces(): MutableList<Face> = faces

    fun addFace(pool: Any?, texture: Any?): Face {
        val face = Face(this, faces.size)
        faces.add(face)
        if (pool != null) face.setPool(pool, texture)
        if (isState(UNLIT)) face.setState(Face.FULLBRIGHT)
        return face
    }

    fun addFace(te: Any?, texture: Any?): Face {
        val face = Face(this, faces.size)
        face.setTEOffset(faces.size)
        face.setTexture(texture)
        if (isState(UNLIT)) face.setState(Face.FULLBRIGHT)
        faces.add(face)
        System.err.println("GPU: resolve pool type from TE + texture")
        face.setPoolType(0)
        return face
    }

    fun addFace(te: Any?, texture: Any?, normalMap: Any?): Face {
        val face = addFace(te, texture)
        face.setNormalMap(normalMap)
        return face
    }

    fun addFace(te: Any?, texture: Any?, normalMap: Any?, specularMap: Any?): Face {
        val face = addFace(te, texture, normalMap)
        face.setSpecularMap(specularMap)
        return face
    }

    fun deleteFaces(offset: Int, count: Int) {
        repeat(count) { faces.removeAt(offset) }
    }

    fun setNumFaces(newFaces: Int, pool: Any?, texture: Any?) {
        if (newFaces == faces.size) return
        while (faces.size > newFaces) faces.removeAt(faces.size - 1)
        while (faces.size < newFaces) addFace(pool, texture)
    }

    fun setNumFacesFast(newFaces: Int, pool: Any?, texture: Any?) {
        if (newFaces <= faces.size && newFaces >= faces.size / 2) return
        setNumFaces(newFaces, pool, texture)
    }

    fun mergeFaces(src: Drawable) {
        src.faces.forEach { face ->
            face.setDrawable(this)
            faces.add(face)
        }
        src.faces.clear()
    }

    // ------------------------------------------------------------------
    // Update / move
    // ------------------------------------------------------------------

    fun update() {
        error("Drawable.update() should not be called directly")
    }

    fun updateMaterial() { /* no-op in base implementation */ }

    fun updateTexture() {
        if (isDead() || viewerObject == null) return
        if (getNumFaces() != viewerObject.getNumTEs()) return
        if (getVOVolume() != null) {
            System.err.println("GPU: mark drawable for REBUILD_MATERIAL in pipeline")
        }
    }

    fun updateGeometry(): Boolean {
        System.err.println("GPU: call viewerObject.updateGeometry(this) to rebuild vertex buffers")
        return false
    }

    fun updateFaceSize(idx: Int) {
        System.err.println("GPU: resize the vertex/index allocation for face $idx")
    }

    fun updateUVMinMax() { /* sun-space bounding box cache update; no-op in base */ }

    fun updateSpecialHoverCursor(enabled: Boolean) {
        // Deferred: maintain a list of objects with special cursors
    }

    open fun makeActive() {
        if (!isState(ACTIVE)) {
            setState(ACTIVE)
            if (!isRoot() && parent?.isActive() == false) {
                parent!!.makeActive()
                parent!!.setState(ACTIVE_CHILD)
            }
            if (viewerObject?.isVolume() == true) {
                System.err.println("GPU: gPipeline.markRebuild(this, REBUILD_VOLUME)")
            }
            updatePartition()
        } else if (!isRoot() && parent?.isActive() == false) {
            parent!!.makeActive()
            parent!!.setState(ACTIVE_CHILD)
        }
    }

    open fun makeStatic(warningEnabled: Boolean = true) {
        if (isState(ACTIVE) && !isState(ACTIVE_CHILD) &&
            viewerObject?.isAttachment() == false &&
            viewerObject?.isFlexible() == false &&
            viewerObject?.isAnimatedObject() == false
        ) {
            clearState(ACTIVE or ANIMATED_CHILD)
            viewerObject?.children()?.forEach { child ->
                child.drawable?.makeStatic(warningEnabled)
            }
            if (viewerObject?.isVolume() == true) {
                System.err.println("GPU: gPipeline.markRebuild(this, REBUILD_VOLUME)")
            }
            if (spatialBridge != null) {
                spatialBridge!!.markDead()
                setSpatialBridge(null)
            }
            updatePartition()
        }
    }

    fun markDead() {
        if (isDead()) return
        setState(DEAD)
        spatialBridge?.markDead()
        spatialBridge = null
        numZombieDrawables++
        cleanupReferences()
    }

    open fun updateMove(): Boolean {
        if (isDead()) return true
        if (viewerObject == null) return false
        makeActive()
        return if (isState(MOVE_UNDAMPED)) updateMoveUndamped() else updateMoveDamped()
    }

    open fun movePartition() {
        System.err.println("GPU: getSpatialPartition()?.move(this, getSpatialGroup())")
    }

    fun updateXform(undamped: Boolean): Float {
        System.err.println("GPU: interpolate or snap position/rotation/scale toward target; call updateMatrix(); return dist_squared")
        return 0f
    }

    private fun updateMoveUndamped(): Boolean {
        val distSquared = updateXform(true)
        generation++
        if (!isState(INVISIBLE)) {
            val moved = distSquared > 0.001f && distSquared < 255.99f
            moveUpdatePipeline(moved)
            viewerObject?.updateText()
        }
        viewerObject?.clearChanged()
        return true
    }

    private fun updateMoveDamped(): Boolean {
        val distSquared = updateXform(false)
        generation++
        if (!isState(INVISIBLE)) {
            val moved = distSquared > 0.001f && distSquared < 128.0f
            moveUpdatePipeline(moved)
            viewerObject?.updateText()
        }
        val doneMoved = distSquared == 0f
        if (doneMoved) viewerObject?.clearChanged()
        return doneMoved
    }

    private fun moveUpdatePipeline(moved: Boolean) {
        if (moved) makeActive()
        for (i in 0 until getNumFaces()) {
            getFace(i)?.updateCenterAgent()
        }
    }

    private fun updatePartition() {
        if (getVOVolume() == null) {
            movePartition()
        } else if (spatialBridge != null) {
            System.err.println("GPU: gPipeline.markMoved(spatialBridge, false)")
        } else {
            System.err.println("GPU: gPipeline.markRebuild(this, REBUILD_POSITION)")
        }
    }

    open fun updateDistance(cameraOrigin: Vector3, forceUpdate: Boolean) {
        System.err.println("GPU: compute distanceWRTCamera from camera origin; account for avatar bounding box; call viewerObject.updateLOD()")
    }

    open fun shiftPos(shiftVector: FloatArray) {
        if (isDead()) return
        if (parent != null) {
            xform.setPosition(viewerObject?.getPosition() ?: Vector3.ZERO)
        } else {
            xform.setPosition(viewerObject?.getPositionAgent() ?: Vector3.ZERO)
        }
        xform.updateMatrix()
        if (isStatic()) {
            System.err.println("GPU: shift face centers/extents by shiftVector; markRebuild if needed; call shift(shiftVector)")
        } else if (spatialBridge != null) {
            System.err.println("GPU: spatialBridge.shiftPos(shiftVector)")
        } else if (isAvatar()) {
            System.err.println("GPU: shift(shiftVector)")
        }
        viewerObject?.onShift(shiftVector)
    }

    open fun setVisible(cameraId: Int, results: MutableList<Drawable>? = null, forSelect: Boolean = false) {
        System.err.println("GPU: mark this drawable visible for the current frame via octree entry")
    }

    open fun updateSpatialExtents() {
        viewerObject?.let {
            System.err.println("GPU: call viewerObject.updateSpatialExtents(extents[0], extents[1]); setSpatialExtents; updateBinRadius")
        }
        updateBinRadius()
        if (spatialBridge != null) {
            System.err.println("GPU: getGroupPosition().splat(0)")
        }
    }

    open fun updateBinRadius() {
        val binRadius = if (viewerObject != null) {
            System.err.println("GPU: return min(viewerObject.getBinRadius(), 256f)")
            0f
        } else {
            min(radius * 4f, 256f)
        }
        System.err.println("GPU: setBinRadius($binRadius)")
    }

    // ------------------------------------------------------------------
    // Bounds / spatial
    // ------------------------------------------------------------------

    fun getPosition(): Vector3 = xform.getPosition()
    fun getWorldPosition(): Vector3 = xform.getPositionW()
    fun getPositionAgent(): Vector3 {
        System.err.println("GPU: convert world position to agent-local space via region offset")
        return Vector3.ZERO
    }
    fun getScale(): Vector3 = currentScale
    fun setScale(scale: Vector3) { currentScale = scale }
    fun getWorldRotation(): FloatArray = xform.getWorldRotation()
    fun getRotation(): FloatArray = xform.getRotation()
    fun getIntensity(): Float = min(xform.scale.x, 4f)
    fun getLOD(): Int = viewerObject?.getLOD() ?: 1
    fun getXform(): XformMatrix = xform

    fun getBounds(min: Vector3, max: Vector3): Vector3 {
        xform.getMinMax(min, max)
        return xform.getPositionW()
    }

    fun getRadius(): Float = radius
    fun setRadius(r: Float) { if (radius != r) radius = r }

    fun getVisibilityRadius(): Float {
        if (isDead()) return 0f
        if (isLight()) {
            val voVol = getVOVolume()
            if (voVol != null) {
                System.err.println("GPU: max(getRadius(), voVol.getLightRadius())")
                return 0f
            }
        }
        return radius
    }

    fun getGeneration(): Int = generation

    fun getLit(): Boolean = !isState(UNLIT)
    fun setLit(lit: Boolean) { if (lit) clearState(UNLIT) else setState(UNLIT) }

    fun isVisible(): Boolean {
        System.err.println("GPU: check octree entry / group visibility flag for current frame")
        return false
    }

    fun isRecentlyVisible(): Boolean {
        System.err.println("GPU: return true if visible in current or previous frame (within MIN_VIS_FRAME_RANGE)")
        return false
    }

    fun getRenderType(): Int = renderType
    fun setRenderType(type: Int) { renderType = type }
    fun isRenderType(type: Int): Boolean = renderType == type

    fun getSpatialGroup(): SpatialGroup? {
        System.err.println("GPU: return (SpatialGroup?) getGroup() from octree entry")
        return null
    }

    fun getSpatialPartition(): SpatialPartition? {
        System.err.println("GPU: return getSpatialGroup()?.getSpatialPartition() or avatar partition for avatars")
        return null
    }

    fun getRoot(): Drawable {
        var d: Drawable = this
        while (!d.isRoot()) d = d.parent!!
        return d
    }

    fun getParent(): Drawable? = parent
    fun getRegion(): Any? = viewerObject?.getRegion()

    fun setGroup(group: Any?) {
        System.err.println("GPU: null out face vertex buffers if group changes and drawable is a volume; call octree setGroup")
    }

    fun removeFromOctree() {
        System.err.println("APR: detach this drawable from its octree entry; remove active cache entry from region")
    }

    open fun cleanupReferences() {
        faces.clear()
        System.err.println("GPU: gPipeline.unlinkDrawable(this) or remove from spatial group; removeFromOctree")
    }

    fun setSpatialBridge(bridge: Drawable?) { spatialBridge = bridge }
    fun getSpatialBridge(): Drawable? = spatialBridge

    fun findReferences(drawablep: Drawable): Int {
        var count = 0
        if (parent == drawablep) count++
        return count
    }

    // ------------------------------------------------------------------
    // VObject accessors
    // ------------------------------------------------------------------

    fun getVOVolume(): Any? {
        if (!isDead() && viewerObject?.isVolume() == true) return viewerObject
        return null
    }

    fun getTextureEntry(which: UByte): Any? = viewerObject?.getTextureEntry(which)

    fun getRenderMatrix(): Any {
        System.err.println("GPU: return isRoot() ? worldMatrix : parent.worldMatrix")
        return Unit
    }
}

// ---------------------------------------------------------------------------
// ViewerObject — minimal stub so Drawable can compile independently.
// The real class is in LLViewerObject.kt.
// ---------------------------------------------------------------------------

interface ViewerObject {
    fun isLight(): Boolean
    fun isAvatar(): Boolean
    fun isVolume(): Boolean
    fun isAttachment(): Boolean
    fun isFlexible(): Boolean
    fun isAnimatedObject(): Boolean
    fun getRegion(): Any?
    fun getPosition(): Vector3
    fun getPositionAgent(): Vector3
    fun getLOD(): Int
    fun getNumTEs(): Int
    fun getTextureEntry(which: UByte): Any?
    fun updateText()
    fun updateLOD()
    fun clearChanged()
    fun onShift(shiftVector: FloatArray)
    fun children(): List<ViewerObject>
    val drawable: Drawable?
}
