/**
 * FSFloaterVRAMUsage.kt
 * Kotlin conversion of fsfloatervramusage.h / fsfloatervramusage.cpp
 *
 * GPU VRAM usage floater — scans all in-range viewer objects, estimates their
 * texture and VBO memory usage, and presents a sortable list.
 *
 * Original author: Nicky Dasmijn, 2015
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// VRAM entry classification
// ---------------------------------------------------------------------------

/**
 * Broad category of a VRAM consumer shown in the floater list.
 *
 * Mirrors the implicit categories present in the C++ implementation
 * (texture memory vs. vertex-buffer memory, per-object).
 */
enum class VRAMEntryType {
    /** Memory consumed by diffuse / specular / normal textures. */
    TEXTURE,
    /** Memory consumed by vertex-buffer objects (geometry). */
    VERTEX_BUFFER,
    /** Combined texture + VBO total for a single scene object. */
    COMBINED,
}

// ---------------------------------------------------------------------------
// Data class
// ---------------------------------------------------------------------------

/**
 * A single row in the VRAM-usage scroll list.
 *
 * @param name      Human-readable object name (filled after property response).
 * @param usageMB   Estimated VRAM consumption in megabytes (integer KB / 1024).
 * @param type      What kind of GPU resource this entry accounts for.
 */
data class VRAMEntry(
    val name: String,
    val usageMB: Int,
    val type: VRAMEntryType,
)

// ---------------------------------------------------------------------------
// Internal helper mirroring the C++ ObjectStat struct
// ---------------------------------------------------------------------------

/**
 * Lightweight record used while building the pending-objects work queue.
 *
 * @param id          UUID of the viewer object.
 * @param textureSizeKB Estimated texture memory in kilobytes (before /1024 to MB).
 */
private data class ObjectStat(
    val id: LLUUID,
    val textureSizeKB: UInt,
)

// ---------------------------------------------------------------------------
// Main floater class
// ---------------------------------------------------------------------------

/**
 * Firestorm GPU VRAM usage floater.
 *
 * On [refresh] it:
 * 1. Iterates all objects within draw distance (excluding terrain, sky, water,
 *    avatars, and non-selectable objects).
 * 2. Estimates texture VRAM from texture dimensions × components × mip factor.
 * 3. Registers an idle callback to request object properties in batches of at
 *    most 250 (mirrors [PROPERTIES_MAX_REQUEST_COUNT]).
 * 4. On property response, computes VBO size and adds a row to the scroll list.
 *
 * Property-request timeout: 10 s ([PROPERTIES_REQUEST_TIMEOUT]).
 * Minimum interval between batches: 2 s ([PROPERY_REQUEST_INTERVAL]).
 *
 * Complex pipeline / selection-manager calls are stubbed with [TODO].
 *
 * Mirrors [FSFloaterVRAMUsage] from `fsfloatervramusage.h`.
 */
class FSFloaterVRAMUsage {

    // ------------------------------------------------------------------
    // Public state
    // ------------------------------------------------------------------

    /**
     * Current list of VRAM entries shown in the scroll list.
     *
     * Updated incrementally as property responses arrive.
     */
    val entries: MutableList<VRAMEntry> = mutableListOf()

    /**
     * Sum of [VRAMEntry.usageMB] across all [entries].
     * Recalculated after each [addObjectToList] call.
     */
    var totalUsageMB: Int = 0
        private set

    // ------------------------------------------------------------------
    // Private state
    // ------------------------------------------------------------------

    /** Objects whose properties still need to be fetched from the server. */
    private val pendingObjects: ArrayDeque<ObjectStat> = ArrayDeque()

    /** Number of property requests currently in-flight. */
    private var pendingRequestCount: UInt = 0u

    /** Elapsed milliseconds since the current batch of property requests was sent. */
    private var propTimerMs: Long = 0L

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Called after the floater's XUI children have been built.
     *
     * Wires the refresh button, retrieves the scroll list control, registers
     * the property listener, and disables the selection silhouette.
     *
     * C++ equivalent: `FSFloaterVRAMUsage::postBuild()`
     */
    fun postBuild(): Boolean {
        TODO(
            "Get child LLButton 'refresh_button' and bind to doRefresh(); " +
            "get child LLScrollListCtrl 'result_list'; " +
            "LLSelectMgr::registerPropertyListener(this); " +
            "LLSelectMgr::enableSilhouette(false)"
        )
    }

    /**
     * Called when the floater is opened.
     *
     * C++ implementation is empty; placeholder for future initialization.
     *
     * C++ equivalent: `FSFloaterVRAMUsage::onOpen(key)`
     */
    fun onOpen(key: LLSD) {
        // Intentionally empty — matches C++ behaviour.
    }

    /**
     * Called when the floater is destroyed.
     *
     * Removes the idle callback, the property listener, and re-enables
     * the selection silhouette.
     *
     * C++ equivalent: `FSFloaterVRAMUsage::~FSFloaterVRAMUsage()`
     */
    fun onDestroy() {
        TODO(
            "gIdleCallbacks.deleteFunction(onIdle); " +
            "LLSelectMgr::removePropertyListener(this); " +
            "LLSelectMgr::enableSilhouette(true)"
        )
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Trigger a full refresh of the VRAM-usage list.
     *
     * Clears [entries] and [totalUsageMB], then re-scans all in-range scene
     * objects, building [pendingObjects] sorted by descending estimated texture
     * size.  Object properties are then fetched incrementally via [onIdle].
     *
     * C++ equivalent: `FSFloaterVRAMUsage::doRefresh()`
     */
    fun refresh() {
        entries.clear()
        totalUsageMB = 0
        pendingObjects.clear()
        pendingRequestCount = 0u
        TODO(
            "Iterate gObjectList; skip non-selectable, terrain, sky, water, avatar objects; " +
            "skip objects beyond gAgentCamera.mDrawDistance; " +
            "compute textureSizeKB via calcTextureSize(); " +
            "sort pendingObjects by textureSizeKB descending; " +
            "LLSelectMgr::deselectAll()"
        )
    }

    /**
     * Idle-callback handler — dispatches the next batch of property requests.
     *
     * Mirrors the `::onIdle` free function and the `FSFloaterVRAMUsage::onIdle`
     * member in the C++ source.  Called every viewer frame while the floater is
     * open.
     *
     * C++ equivalent: `FSFloaterVRAMUsage::onIdle()`
     */
    fun onIdle() {
        TODO(
            "If pendingRequestCount == 0 and pendingObjects.isEmpty() → deselectAll and return. " +
            "If pendingRequestCount > 0 and timer < PROPERTIES_REQUEST_TIMEOUT → return. " +
            "If pendingRequestCount == 0 and timer.started and timer < PROPERY_REQUEST_INTERVAL → return. " +
            "Otherwise: deselectAll; reset pendingRequestCount; " +
            "pull up to PROPERTIES_MAX_REQUEST_COUNT items from pendingObjects whose objects are in a region; " +
            "call LLSelectMgr::enableBatchMode / selectObjectAndFamily / disableBatchMode"
        )
    }

    /**
     * Called by the selection manager when object properties arrive from the server.
     *
     * Looks up the object in the scene, computes its full VRAM footprint
     * (texture KB + VBO KB), and appends a new [VRAMEntry] to [entries].
     *
     * C++ equivalent: `FSFloaterVRAMUsage::onProperties(LLSelectNode const*)`
     */
    fun onProperties(nodeId: LLUUID, objectName: String) {
        TODO(
            "Find object via gObjectList.findObject(nodeId); " +
            "call addObjectToList(object, objectName)"
        )
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Compute the estimated texture VRAM for [objectId] in kilobytes.
     *
     * Iterates all texture elements (TEs), skipping missing assets and
     * de-duplicating shared textures.  Adds 33 % for mipmaps when enabled.
     *
     * C++ equivalent: `FSFloaterVRAMUsage::calcTexturSize(LLViewerObject*, ostream*)`
     */
    private fun calcTextureSize(objectId: LLUUID): UInt {
        TODO(
            "Get object from gObjectList; iterate getNumTEs(); for each TE get LLViewerTexture; " +
            "accumulate fullWidth * fullHeight * components; add mip overhead if useMipMaps; " +
            "divide by 1024; return as UInt"
        )
    }

    /**
     * Compute the per-vertex VBO entry size in bytes for the given vertex-buffer type mask.
     *
     * C++ equivalent: `FSFloaterVRAMUsage::calcVBOEntrySize(LLVertexBuffer*)`
     */
    private fun calcVboEntrySize(typeMask: UInt): Int {
        TODO(
            "Iterate LLVertexBuffer::TYPE_MAX bits; for each set bit (excluding TYPE_TEXTURE_INDEX) " +
            "add LLVertexBuffer::sTypeSize[k]"
        )
    }

    /**
     * Compute the face extents in centimetres (width × height).
     *
     * C++ equivalent: `FSFloaterVRAMUsage::calcFaceSize(LLFace*, S32&, S32&)`
     */
    private fun calcFaceSize(faceIndex: Int): Pair<Int, Int> {
        TODO(
            "Get LLFace from drawable; subtract mExtents[0] from mExtents[1]; " +
            "compute cmX/Y/Z as size * 100; return (w, h) choosing non-zero axes"
        )
    }

    /**
     * Append an object's full VRAM footprint as a new [VRAMEntry] and update [totalUsageMB].
     *
     * Computes texture KB + VBO KB across all drawable faces, builds a tooltip
     * string with per-TE and per-face details, then adds the row to the scroll list.
     *
     * C++ equivalent: `FSFloaterVRAMUsage::addObjectToList(LLViewerObject*, string const&)`
     */
    private fun addObjectToList(objectId: LLUUID, name: String) {
        TODO(
            "Compute totalTexSizeKB = calcTextureSize(objectId); " +
            "iterate drawable faces; for each face: calcFaceSize + calcVboEntrySize * numVertices + indexSize; " +
            "sum totalVboSizeKB; build VRAMEntry(name, (totalTexSizeKB+totalVboSizeKB)/1024, COMBINED); " +
            "add to entries; recalculate totalUsageMB; add row to mList scroll control"
        )
        // Maintain totalUsageMB in sync with entries after update.
        totalUsageMB = entries.sumOf { it.usageMB }
    }

    // ------------------------------------------------------------------
    // Companion object — singleton access and factory
    // ------------------------------------------------------------------

    companion object {

        @Volatile
        private var instance: FSFloaterVRAMUsage? = null

        /**
         * Return the singleton instance, creating it if necessary.
         * Mirrors [LLFloaterReg::getInstance("fs_vramUsage")] in C++.
         */
        fun getInstance(): FSFloaterVRAMUsage =
            instance ?: synchronized(this) {
                instance ?: FSFloaterVRAMUsage().also { instance = it }
            }

        /**
         * Make the VRAM-usage floater visible.
         * Mirrors [LLFloaterReg::showInstance("fs_vramUsage")] in C++.
         */
        fun show() {
            getInstance()
            TODO("LLFloaterReg::showInstance(\"fs_vramUsage\")")
        }
    }
}
