/**
 * ViewerOctree.kt
 * Converted from llvieweroctree.h / llvieweroctree.cpp
 *
 * Spatial partitioning structures for the Second Life viewer's octree.
 * GPU occlusion queries, aligned SIMD vectors, and raw octree traversal
 * are all stubbed with TODO — only the data model and state machine are
 * expressed here.
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

// ---------------------------------------------------------------------------
// Free-standing geometry helpers
// ---------------------------------------------------------------------------

/**
 * Returns the index into the box-fan index buffer for the given camera/center
 * pair. Actual GPU index buffer is not available in JVM — stubbed.
 */
fun getBoxFanIndices(cameraOrigin: Vector3, center: Vector3): Int {
    System.err.println("ViewerOctree: getBoxFanIndices not yet implemented")
    return 0
}

/** AABB vs sphere intersection test. Returns -1, 0, or 1 (outside/intersect/inside). */
fun aabbSphereIntersect(min: Vector3, max: Vector3, origin: Vector3, rad: Float): Int {
    // For each axis find the distance from origin to the nearest point on the AABB.
    var distSq = 0f
    for (i in 0..2) {
        val v = origin[i]
        val lo = min[i]; val hi = max[i]
        if (v < lo) distSq += (lo - v) * (lo - v)
        else if (v > hi) distSq += (v - hi) * (v - hi)
    }
    return when {
        distSq > rad * rad -> -1   // fully outside
        else               ->  1   // intersects or inside
    }
}

/** AABB vs sphere (radius squared variant). */
fun aabbSphereIntersectR2(min: Vector3, max: Vector3, origin: Vector3, radiusSq: Float): Int {
    var distSq = 0f
    for (i in 0..2) {
        val v = origin[i]
        val lo = min[i]; val hi = max[i]
        if (v < lo) distSq += (lo - v) * (lo - v)
        else if (v > hi) distSq += (v - hi) * (v - hi)
    }
    return if (distSq > radiusSq) -1 else 1
}

// ---------------------------------------------------------------------------
// Entry data-type tag (mirrors eEntryDataType_t)
// ---------------------------------------------------------------------------

enum class OctreeEntryDataType {
    DRAWABLE,
    VO_CACHE_ENTRY;
}

// ---------------------------------------------------------------------------
// OctreeEntry  (LLViewerOctreeEntry)
// ---------------------------------------------------------------------------

/**
 * One node of spatial data held inside the octree.  An entry may have up to
 * two data payloads (drawable + VOCache) attached.
 *
 * The bounding sphere (centre/radius) is the high-level approximation used
 * for coarse culling; the full AABB extents are also tracked.
 */
open class OctreeEntry {

    // Spatial geometry — centre of the group bounding box and bin radius.
    var center: Vector3 = Vector3.ZERO
        protected set
    var radius: Float = 0f
        protected set

    // AABB extents (min, max).
    val extents: Array<Vector3> = arrayOf(Vector3.ZERO, Vector3.ZERO)

    // Position group: 4-component group centre used by the octree traversal.
    var positionGroup: Vector3 = Vector3.ZERO
        internal set

    // Bin placement hint.
    var binRadius: Float = 0f
    @Volatile var binIndex: Int = -1

    // Visibility counter — compared against OctreeEntryData.curVisible.
    @Volatile internal var visible: UInt = 0u

    // Weak back-references to the owning group (set by the group on insertion).
    internal var group: ViewerOctreeGroup? = null

    // Payload slots — analogous to mData[NUM_DATA_TYPE].
    private val data: Array<OctreeEntryData?> = arrayOfNulls(OctreeEntryDataType.entries.size)

    fun getDrawable(): OctreeEntryData?      = data[OctreeEntryDataType.DRAWABLE.ordinal]
    fun hasDrawable(): Boolean               = data[OctreeEntryDataType.DRAWABLE.ordinal] != null
    fun getVOCacheEntry(): OctreeEntryData?  = data[OctreeEntryDataType.VO_CACHE_ENTRY.ordinal]
    fun hasVOCacheEntry(): Boolean           = data[OctreeEntryDataType.VO_CACHE_ENTRY.ordinal] != null

    fun getGroup(): ViewerOctreeGroup? = group

    internal fun addData(payload: OctreeEntryData) {
        data[payload.dataType.ordinal] = payload
    }

    internal fun removeData(payload: OctreeEntryData) {
        val idx = payload.dataType.ordinal
        if (data[idx] === payload) data[idx] = null
    }

    /** Called by the owning group's handleDestruction — clears the back-link. */
    internal fun nullGroup() {
        group = null
    }

    fun setGroup(g: ViewerOctreeGroup?) {
        group = g
    }
}

// ---------------------------------------------------------------------------
// OctreeEntryData  (LLViewerOctreeEntryData)
// ---------------------------------------------------------------------------

/**
 * Abstract payload attached to an [OctreeEntry].  Concrete subclasses are
 * Drawable and VOCacheEntry, implemented elsewhere in the pipeline.
 */
abstract class OctreeEntryData(val dataType: OctreeEntryDataType) {

    protected var entry: OctreeEntry? = null

    val binRadius: Float get() = entry?.binRadius ?: 0f

    fun getSpatialExtents(): Array<Vector3>? = entry?.extents
    fun getGroup(): ViewerOctreeGroup?       = entry?.group
    fun getPositionGroup(): Vector3          = entry?.positionGroup ?: Vector3.ZERO

    fun setBinRadius(rad: Float) { entry?.binRadius = rad }

    fun setSpatialExtents(min: Vector3, max: Vector3) {
        entry?.extents?.let { it[0] = min; it[1] = max }
    }

    fun setPositionGroup(pos: Vector3) { entry?.positionGroup = pos }

    open fun setOctreeEntry(e: OctreeEntry) {
        entry = e
        e.addData(this)
    }

    fun removeOctreeEntry() {
        entry?.removeData(this)
        entry = null
    }

    /** Shift this entry's spatial position by a world-space delta. */
    fun shift(delta: Vector3) {
        entry?.let {
            it.positionGroup = it.positionGroup + delta
            it.extents[0] = it.extents[0] + delta
            it.extents[1] = it.extents[1] + delta
        }
    }

    fun getVisible(): UInt = entry?.visible ?: 0u

    fun setVisible() { entry?.visible = curVisible }

    fun resetVisible() { entry?.visible = 0u }

    open fun isVisible(): Boolean = entry?.let { it.visible >= curVisible } ?: false

    open fun isRecentlyVisible(): Boolean =
        entry?.let { it.visible + 1u >= curVisible } ?: false

    open fun setGroup(group: ViewerOctreeGroup?) { entry?.setGroup(group) }

    companion object {
        /** Frame counter — incremented once per rendered frame. */
        var curVisible: UInt = 10u
            private set

        fun getCurrentFrame(): UInt = curVisible

        fun incrementVisible() { curVisible++ }
    }
}

// ---------------------------------------------------------------------------
// ViewerOctreeGroup  (LLViewerOctreeGroup)
// ---------------------------------------------------------------------------

/**
 * An octree node that aggregates multiple [OctreeEntry] objects and tracks
 * combined AABB bounds, dirty state, and per-camera visibility.
 *
 * Heavy rendering / GPU-occlusion logic is stubbed.
 */
open class ViewerOctreeGroup {

    // -- State flags ----------------------------------------------------------

    /** Bit-flag state word mirroring the C++ unnamed enum. */
    var state: UInt = STATE_CLEAN
        protected set

    fun getState(): UInt         = state
    fun isDirty(): Boolean       = (state and STATE_DIRTY) != 0u
    fun hasState(s: UInt): Boolean = (state and s) != 0u
    fun setState(s: UInt)        { state = state or s }
    fun clearState(s: UInt)      { state = state and s.inv() }

    fun isDead(): Boolean        = hasState(STATE_DEAD)

    // -- Geometry -------------------------------------------------------------

    /** Tight AABB of this group and all children: [0]=centre, [1]=half-size. */
    val bounds: Array<Vector3>       = arrayOf(Vector3.ZERO, Vector3.ZERO)
    /** Extents (min, max) of this group and all children. */
    val extents: Array<Vector3>      = arrayOf(Vector3.ZERO, Vector3.ZERO)
    /** AABB of objects directly in this node: [0]=centre, [1]=half-size. */
    val objectBounds: Array<Vector3> = arrayOf(Vector3.ZERO, Vector3.ZERO)
    /** Extents (min, max) of objects in this node. */
    val objectExtents: Array<Vector3>= arrayOf(Vector3.ZERO, Vector3.ZERO)

    // -- Visibility -----------------------------------------------------------

    /** Latest frame this group was visible to any camera. */
    var anyVisible: Int = 0
        protected set

    /** Per-camera visibility frame counters (indexed by camera-id ordinal). */
    val visiblePerCamera: IntArray = IntArray(MAX_CAMERAS)

    // -- Entries --------------------------------------------------------------

    private val entries: MutableList<OctreeEntry> = mutableListOf()

    fun isEmpty(): Boolean = entries.isEmpty()
    fun getElementCount(): Int = entries.size

    // -- Lifecycle ------------------------------------------------------------

    open fun unbound() {
        setState(STATE_DIRTY)
    }

    /** Recompute this group's AABB to tightly fit all contained entries. */
    open fun rebound() {
        if (entries.isEmpty()) return
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (e in entries) {
            val lo = e.extents[0]; val hi = e.extents[1]
            if (lo.x < minX) minX = lo.x; if (lo.y < minY) minY = lo.y; if (lo.z < minZ) minZ = lo.z
            if (hi.x > maxX) maxX = hi.x; if (hi.y > maxY) maxY = hi.y; if (hi.z > maxZ) maxZ = hi.z
        }
        objectExtents[0] = Vector3(minX, minY, minZ)
        objectExtents[1] = Vector3(maxX, maxY, maxZ)
        val cx = (minX + maxX) * 0.5f; val cy = (minY + maxY) * 0.5f; val cz = (minZ + maxZ) * 0.5f
        objectBounds[0] = Vector3(cx, cy, cz)
        objectBounds[1] = Vector3(maxX - cx, maxY - cy, maxZ - cz)
        // Propagate to group extents (simplified — ignores children).
        extents[0] = objectExtents[0]; extents[1] = objectExtents[1]
        bounds[0]  = objectBounds[0];  bounds[1]  = objectBounds[1]
        clearState(STATE_DIRTY)
    }

    // -- Visibility helpers ---------------------------------------------------

    fun setVisible() {
        val frame = OctreeEntryData.getCurrentFrame().toInt()
        for (i in visiblePerCamera.indices) visiblePerCamera[i] = frame
        anyVisible = frame
    }

    open fun isVisible(): Boolean =
        anyVisible >= OctreeEntryData.getCurrentFrame().toInt()

    open fun isRecentlyVisible(): Boolean =
        anyVisible + 1 >= OctreeEntryData.getCurrentFrame().toInt()

    // -- Listener callbacks (OctreeListener interface stubs) ------------------

    open fun handleInsertion(entry: OctreeEntry) {
        entries.add(entry)
        entry.setGroup(this)
        setState(STATE_OBJECT_DIRTY or STATE_DIRTY)
    }

    open fun handleRemoval(entry: OctreeEntry) {
        entries.remove(entry)
        entry.nullGroup()
        setState(STATE_OBJECT_DIRTY or STATE_DIRTY)
    }

    open fun handleDestruction() {
        for (e in entries) e.nullGroup()
        entries.clear()
        setState(STATE_DEAD)
    }

    open fun handleStateChange() { /* no-op in base */ }

    open fun handleChildAddition(child: ViewerOctreeGroup) { /* no-op in base */ }

    open fun handleChildRemoval(child: ViewerOctreeGroup) { /* no-op in base */ }

    // -- Element access -------------------------------------------------------

    fun hasElement(data: OctreeEntryData): Boolean =
        entries.any { it === data.entry }

    companion object {
        const val STATE_CLEAN: UInt        = 0x00000000u
        const val STATE_DIRTY: UInt        = 0x00000001u
        const val STATE_OBJECT_DIRTY: UInt = 0x00000002u
        const val STATE_SKIP_FRUSTUM: UInt = 0x00000004u
        const val STATE_DEAD: UInt         = 0x00000008u
        const val STATE_INVALID: UInt      = 0x00000010u

        /** Number of simultaneous cameras the viewer supports. */
        const val MAX_CAMERAS: Int = 5
    }
}

// ---------------------------------------------------------------------------
// OcclusionCullingGroup  (LLOcclusionCullingGroup)
// ---------------------------------------------------------------------------

/**
 * Extends [ViewerOctreeGroup] with GPU occlusion-query tracking.
 * All query issuance / readback is stubbed — the state machine is preserved.
 */
open class OcclusionCullingGroup(val partition: OctreePartitionBase) : ViewerOctreeGroup() {

    enum class OcclusionState(val bit: UInt) {
        OCCLUDED(0x00010000u),
        QUERY_PENDING(0x00020000u),
        ACTIVE_OCCLUSION(0x00040000u),
        DISCARD_QUERY(0x00080000u),
        EARLY_FAIL(0x00100000u),
    }

    enum class SetStateMode {
        SINGLE,
        BRANCH,
        DIFF,
        ALL_CAMERAS,
    }

    private val occlusionState: UIntArray = UIntArray(MAX_CAMERAS)
    private val occlusionIssued: UIntArray = UIntArray(MAX_CAMERAS)

    fun isOcclusionState(s: UInt, cameraId: Int = 0): Boolean =
        (occlusionState[cameraId] and s) != 0u

    fun getOcclusionState(cameraId: Int = 0): UInt = occlusionState[cameraId]

    fun setOcclusionState(s: UInt, mode: SetStateMode = SetStateMode.SINGLE) {
        when (mode) {
            SetStateMode.SINGLE       -> occlusionState[0] = occlusionState[0] or s
            SetStateMode.ALL_CAMERAS  -> occlusionState.indices.forEach { occlusionState[it] = occlusionState[it] or s }
            else                      -> System.err.println("OcclusionCullingGroup: setOcclusionState branch/diff not yet implemented")
        }
    }

    fun clearOcclusionState(s: UInt, mode: SetStateMode = SetStateMode.SINGLE) {
        when (mode) {
            SetStateMode.SINGLE       -> occlusionState[0] = occlusionState[0] and s.inv()
            SetStateMode.ALL_CAMERAS  -> occlusionState.indices.forEach { occlusionState[it] = occlusionState[it] and s.inv() }
            else                      -> System.err.println("OcclusionCullingGroup: clearOcclusionState branch/diff not yet implemented")
        }
    }

    /** Read back the result of the last issued GPU occlusion query. */
    fun checkOcclusion() {
        // no-op
    }

    /** Issue a new GPU occlusion query for this group's bounding volume. */
    fun doOcclusion(cameraId: Int = 0) {
        // no-op
    }

    override fun isRecentlyVisible(): Boolean =
        super.isRecentlyVisible() || isOcclusionState(OcclusionState.OCCLUDED.bit.inv())

    companion object {
        fun getNewOcclusionQueryObjectName(): UInt {
            System.err.println("OcclusionCullingGroup: getNewOcclusionQueryObjectName not yet implemented")
            return 0u
        }

        fun releaseOcclusionQueryObjectName(name: UInt) {
            // no-op
        }
    }
}

// ---------------------------------------------------------------------------
// OctreePartitionBase  (LLViewerOctreePartition)
// ---------------------------------------------------------------------------

/** Abstract root-level spatial partition. Concrete subclasses handle culling. */
abstract class OctreePartitionBase {

    var partitionType: UInt = 0u
    var drawableType: UInt  = 0u
    var occlusionEnabled: Boolean = true
    var lodSeed: UInt   = 0u
    var lodPeriod: UInt = 1u

    /** The root group (lazily constructed by concrete subclasses). */
    protected var rootGroup: ViewerOctreeGroup? = null

    /** Full frustum cull — implemented by concrete partitions (e.g. spatial partition types). */
    abstract fun cull(doOcclusion: Boolean): Int

    fun isOcclusionEnabled(): Boolean = occlusionEnabled
}

// ---------------------------------------------------------------------------
// OctreePartition  (singleton facade — OctreePartition in the spec)
// ---------------------------------------------------------------------------

/**
 * Singleton root manager for the viewer's spatial partitioning.
 * Maps entries to groups and routes moves / insertions.
 */
object OctreePartition : OctreePartitionBase() {

    private val entryToGroup: MutableMap<OctreeEntry, ViewerOctreeGroup> = mutableMapOf()

    /**
     * Return (or lazily create) the [ViewerOctreeGroup] that currently owns
     * the given entry.
     */
    fun getGroup(entry: OctreeEntry): ViewerOctreeGroup {
        return entryToGroup.getOrPut(entry) {
            ViewerOctreeGroup().also { g -> g.handleInsertion(entry) }
        }
    }

    /**
     * Notify the partition that an entry has moved and may need to be
     * reassigned to a different group.
     */
    fun move(entry: OctreeEntry) {
        // In the real viewer this triggers an octree re-insert; here we simply
        // mark the owning group dirty so rebound() will run next frame.
        entryToGroup[entry]?.setState(ViewerOctreeGroup.STATE_DIRTY)
    }

    override fun cull(doOcclusion: Boolean): Int {
        System.err.println("OctreePartition: cull not yet implemented")
        return 0
    }
}

// ---------------------------------------------------------------------------
// Cull traversal stubs  (LLViewerOctreeCull / LLViewerOctreeDebug)
// ---------------------------------------------------------------------------

/** Base class for frustum-cull traversals over the octree. */
abstract class OctreeCull {
    abstract fun frustumCheck(group: ViewerOctreeGroup): Int
    abstract fun frustumCheckObjects(group: ViewerOctreeGroup): Int

    open fun traverse(group: ViewerOctreeGroup) {
        System.err.println("OctreeCull: traverse not yet implemented")
    }
}

/** Debug traversal — prints octree node info; stubbed. */
object OctreeDebug {
    var inDebug: Boolean = false

    fun processGroup(group: ViewerOctreeGroup) {
        if (inDebug) {
            println("OctreeDebug: group state=${group.state} elementCount=${group.getElementCount()}")
        }
    }
}
