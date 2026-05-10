// Converted from llagentwearables.h / llagentwearables.cpp
// Original: Copyright (C) 2010, Linden Research, Inc. (LGPL 2.1)
package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llinventory.*
import com.firestorm.llappearance.*

// ---------------------------------------------------------------------------
// Wearable type enumeration
// Mirrors LLWearableType::EType; order matches the SL protocol.
// ---------------------------------------------------------------------------

enum class WearableType {
    SHAPE, SKIN, HAIR, EYES,
    SHIRT, PANTS, SHOES, SOCKS,
    JACKET, GLOVES, UNDERSHIRT, UNDERPANTS,
    SKIRT, ALPHA, TATTOO, UNIVERSAL,
    PHYSICS,
    INVALID;

    companion object {
        /** Number of body-part types (shape..eyes) that cannot be removed if count == 1. */
        const val BODY_PART_COUNT = 4
    }
}

// ---------------------------------------------------------------------------
// Lightweight wearable data holder
// Corresponds to LLViewerWearable in C++.
// ---------------------------------------------------------------------------

data class ViewerWearable(
    val type:    WearableType,
    val itemId:  LLUUID,
    val assetId: LLUUID,
    val name:    String,
    val isModifiable: Boolean = true,
    val isCopyable:   Boolean = true
)

// ---------------------------------------------------------------------------
// Callback types
// ---------------------------------------------------------------------------

typealias WearablesLoadedCallback = () -> Unit

// ---------------------------------------------------------------------------
// AgentWearables singleton
// Manages per-slot wearable arrays (up to MAX_WEARABLES_PER_TYPE per type).
// ---------------------------------------------------------------------------

object AgentWearables {

    // --- Constants -----------------------------------------------------------

    const val MAX_WEARABLES_PER_TYPE: Int = 5

    // --- Internal storage ----------------------------------------------------
    // keyed by WearableType; each value is an ordered list of worn items.

    private val mWearables: MutableMap<WearableType, MutableList<ViewerWearable>> =
        WearableType.entries.associateWith { mutableListOf<ViewerWearable>() }.toMutableMap()

    // --- State flags ---------------------------------------------------------

    var wearablesLoaded: Boolean = false
        private set

    var cofChangeInProgress: Boolean = false
        private set

    // Static across login sessions (mirrors C++ static members)
    private var initialWearablesUpdateReceived: Boolean = false
    private var initialWearablesLoaded:         Boolean = false
    private var initialAttachmentsRequested:    Boolean = false

    fun areWearablesLoaded():            Boolean = wearablesLoaded
    fun areInitialWearablesLoaded():     Boolean = initialWearablesLoaded
    fun areInitialAttachmentsRequested():Boolean = initialAttachmentsRequested
    fun isCOFChangeInProgress():         Boolean = cofChangeInProgress

    // --- Queries -------------------------------------------------------------

    /** Returns true if any worn wearable has [itemId] as its inventory item UUID. */
    fun isWearingItem(itemId: LLUUID): Boolean =
        mWearables.values.any { slots -> slots.any { it.itemId == itemId } }

    /** Number of wearables currently worn for [type]. */
    fun getWearableCount(type: WearableType): Int =
        mWearables[type]?.size ?: 0

    /** Returns the wearable at [index] for [type], or null if out of range. */
    fun getWearable(type: WearableType, index: Int): ViewerWearable? =
        mWearables[type]?.getOrNull(index)

    fun getWearableFromItemId(itemId: LLUUID): ViewerWearable? =
        mWearables.values.flatten().firstOrNull { it.itemId == itemId }

    fun getWearableFromAssetId(assetId: LLUUID): ViewerWearable? =
        mWearables.values.flatten().firstOrNull { it.assetId == assetId }

    fun getWearableItemID(type: WearableType, index: Int): LLUUID =
        getWearable(type, index)?.itemId ?: LLUUID.NULL

    fun getWearableAssetID(type: WearableType, index: Int): LLUUID =
        getWearable(type, index)?.assetId ?: LLUUID.NULL

    /** Return the slot index of [itemId] within its type, or -1 if not found. */
    fun getWearableIndexFromItem(itemId: LLUUID): Int {
        for (slots in mWearables.values) {
            val idx = slots.indexOfFirst { it.itemId == itemId }
            if (idx >= 0) return idx
        }
        return -1
    }

    /** All item UUIDs currently worn across all wearable types. */
    fun getWearableItemIDs(): List<LLUUID> =
        mWearables.values.flatten().map { it.itemId }

    /** All item UUIDs for a specific [type]. */
    fun getWearableItemIDs(type: WearableType): List<LLUUID> =
        mWearables[type]?.map { it.itemId } ?: emptyList()

    companion object {
        /** True if the agent has at least one wearable of [type] equipped. */
        fun selfHasWearable(type: WearableType): Boolean =
            AgentWearables.getWearableCount(type) > 0
    }

    // --- Setters / mutations -------------------------------------------------

    /**
     * Replace the wearable at [index] for [type].
     * Pass null to clear that slot.
     */
    fun setWearable(type: WearableType, index: Int, wearable: ViewerWearable?) {
        val slots = mWearables.getOrPut(type) { mutableListOf() }
        when {
            wearable == null && index < slots.size -> slots.removeAt(index)
            wearable != null && index < slots.size -> slots[index] = wearable
            wearable != null                       -> slots.add(wearable)
        }
    }

    /**
     * Append a wearable to the end of [type]'s slot list if not already full.
     * Returns true on success.
     */
    fun addWearable(wearable: ViewerWearable): Boolean {
        val slots = mWearables.getOrPut(wearable.type) { mutableListOf() }
        if (slots.size >= MAX_WEARABLES_PER_TYPE) return false
        slots.add(wearable)
        return true
    }

    /**
     * Remove the wearable at [index] for [type].
     * [doRemoveAll] ignores [index] and removes every wearable of [type].
     */
    fun removeWearable(type: WearableType, doRemoveAll: Boolean = false, index: Int = 0) {
        val slots = mWearables[type] ?: return
        if (doRemoveAll) {
            if (!canForceRemoveType(type)) return
            slots.clear()
        } else {
            if (index < slots.size) {
                if (!canWearableBeRemoved(slots[index])) return
                slots.removeAt(index)
            }
        }
        TODO("removeWearable: sync COF and send server update")
    }

    /** False for single-slot body parts (shape/skin/eyes/hair) when count == 1. */
    fun canWearableBeRemoved(wearable: ViewerWearable): Boolean {
        val isBodyPart = wearable.type.ordinal < WearableType.BODY_PART_COUNT
        return !isBodyPart || getWearableCount(wearable.type) > 1
    }

    private fun canForceRemoveType(type: WearableType): Boolean =
        type.ordinal >= WearableType.BODY_PART_COUNT || getWearableCount(type) > 1

    fun isWearableModifiable(type: WearableType, index: Int = 0): Boolean =
        getWearable(type, index)?.isModifiable ?: false

    fun isWearableModifiable(itemId: LLUUID): Boolean =
        getWearableFromItemId(itemId)?.isModifiable ?: false

    fun isWearableCopyable(type: WearableType, index: Int = 0): Boolean =
        getWearable(type, index)?.isCopyable ?: false

    fun canMoveWearable(itemId: LLUUID, closerToBody: Boolean): Boolean =
        TODO("canMoveWearable: check ordering constraints")

    fun moveWearable(itemId: LLUUID, closerToBody: Boolean): Boolean =
        TODO("moveWearable: reorder within type")

    // --- Outfit application --------------------------------------------------

    /** Replace all wearables atomically from a fully resolved outfit. */
    fun setWearableOutfit(items: List<LLUUID>, wearables: List<ViewerWearable>) {
        mWearables.values.forEach { it.clear() }
        wearables.forEach { addWearable(it) }
        TODO("setWearableOutfit: notify and trigger bake")
    }

    // --- Save / revert -------------------------------------------------------

    fun saveWearable(type: WearableType, index: Int, sendUpdate: Boolean = true, newName: String = "") {
        TODO("saveWearable($type, $index)")
    }

    fun saveWearableAs(type: WearableType, index: Int, newName: String, description: String, saveInLostAndFound: Boolean) {
        TODO("saveWearableAs($type, $index, $newName)")
    }

    fun saveAllWearables() {
        WearableType.entries.filter { it != WearableType.INVALID }.forEach { type ->
            for (i in 0 until getWearableCount(type)) saveWearable(type, i, sendUpdate = false)
        }
        TODO("saveAllWearables: final sendAgentWearablesUpdate")
    }

    fun revertWearable(type: WearableType, index: Int) { TODO("revertWearable($type, $index)") }

    // --- Standard wearables creation -----------------------------------------

    fun createStandardWearables() { TODO("createStandardWearables: generate defaults for bare avatar") }

    // --- Editing wearables ---------------------------------------------------

    private var itemToEdit: LLUUID = LLUUID.NULL

    fun requestEditingWearable(itemId: LLUUID) { itemToEdit = itemId }
    fun editWearableIfRequested(itemId: LLUUID) {
        if (itemToEdit == itemId) {
            itemToEdit = LLUUID.NULL
            TODO("editWearableIfRequested: open wearable editor for $itemId")
        }
    }

    fun setWearableName(itemId: LLUUID, newName: String) { TODO("setWearableName($itemId, $newName)") }

    companion object {
        fun createWearable(type: WearableType, wear: Boolean = false, parentId: LLUUID = LLUUID.NULL, createdCb: ((LLUUID) -> Unit)? = null) {
            TODO("createWearable($type)")
        }
        fun editWearable(itemId: LLUUID) { TODO("editWearable($itemId)") }
    }

    // --- Server communication (Legacy Bake) ----------------------------------

    /** Broadcast the current wearable list to the simulator. */
    fun sendAgentWearablesUpdate() { TODO("sendAgentWearablesUpdate: build and send AgentWearablesUpdate message") }

    fun sendAgentWearablesRequest() { TODO("sendAgentWearablesRequest") }
    fun sendDummyAgentWearablesUpdate() { TODO("sendDummyAgentWearablesUpdate: compatibility shim for old sims") }
    fun queryWearableCache() { TODO("queryWearableCache") }

    // --- Wearables-loaded signals --------------------------------------------

    private val loadingStartedListeners: MutableList<WearablesLoadedCallback> = mutableListOf()
    private val loadedListeners:         MutableList<WearablesLoadedCallback> = mutableListOf()

    fun addLoadingStartedCallback(cb: WearablesLoadedCallback) { loadingStartedListeners += cb }
    fun addLoadedCallback(cb: WearablesLoadedCallback)         { loadedListeners += cb }

    fun notifyLoadingStarted() {
        cofChangeInProgress = true
        loadingStartedListeners.forEach { it() }
    }

    fun notifyLoadingFinished() {
        wearablesLoaded     = true
        cofChangeInProgress = false
        loadedListeners.forEach { it() }
        if (!initialWearablesLoaded) {
            initialWearablesLoaded = true
        }
    }

    fun updateWearablesLoaded() {
        wearablesLoaded = mWearables.values.flatten().isNotEmpty()
    }

    // --- Lifecycle -----------------------------------------------------------

    fun cleanup() {
        mWearables.values.forEach { it.clear() }
        wearablesLoaded = false
    }

    fun dump() {
        WearableType.entries.forEach { type ->
            val count = getWearableCount(type)
            println("WearableType $type: $count item(s)")
            for (i in 0 until count) {
                println("  [$i] ${getWearable(type, i)}")
            }
        }
    }
}
