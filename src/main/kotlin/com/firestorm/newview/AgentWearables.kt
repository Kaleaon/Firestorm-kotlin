package com.firestorm.newview

import java.util.UUID

enum class WearableType {
    SHAPE, SKIN, HAIR, EYES,
    SHIRT, PANTS, SHOES, SOCKS,
    JACKET, GLOVES, UNDERSHIRT, UNDERPANTS,
    SKIRT, ALPHA, TATTOO, UNIVERSAL,
    PHYSICS,
    INVALID;

    companion object {
        const val BODY_PART_COUNT: Int = 4
    }
}

data class ViewerWearable(
    val type: WearableType,
    val itemId: UUID,
    val assetId: UUID,
    val name: String,
    val isModifiable: Boolean = true,
    val isCopyable: Boolean = true
)

typealias WearablesLoadedCallback = () -> Unit

object AgentWearables {

    const val MAX_WEARABLES_PER_TYPE: Int = 5

    private val wearables: MutableMap<WearableType, MutableList<ViewerWearable>> =
        WearableType.entries.associateWith { mutableListOf<ViewerWearable>() }.toMutableMap()

    var wearablesLoaded: Boolean = false
        private set
    var cofChangeInProgress: Boolean = false
        private set

    private var initialWearablesUpdateReceived: Boolean = false
    private var initialWearablesLoaded: Boolean = false
    private var initialAttachmentsRequested: Boolean = false

    fun areWearablesLoaded(): Boolean = wearablesLoaded
    fun areInitialWearablesLoaded(): Boolean = initialWearablesLoaded
    fun areInitialAttachmentsRequested(): Boolean = initialAttachmentsRequested
    fun isCOFChangeInProgress(): Boolean = cofChangeInProgress

    fun selfHasWearable(type: WearableType): Boolean = getWearableCount(type) > 0

    fun isWearingItem(itemId: UUID): Boolean =
        wearables.values.any { slots -> slots.any { it.itemId == itemId } }

    fun getWearableCount(type: WearableType): Int = wearables[type]?.size ?: 0

    fun getWearable(type: WearableType, index: Int): ViewerWearable? =
        wearables[type]?.getOrNull(index)

    fun getWearableFromItemId(itemId: UUID): ViewerWearable? =
        wearables.values.flatten().firstOrNull { it.itemId == itemId }

    fun getWearableFromAssetId(assetId: UUID): ViewerWearable? =
        wearables.values.flatten().firstOrNull { it.assetId == assetId }

    fun getWearableItemID(type: WearableType, index: Int): UUID =
        getWearable(type, index)?.itemId ?: NULL_UUID

    fun getWearableAssetID(type: WearableType, index: Int): UUID =
        getWearable(type, index)?.assetId ?: NULL_UUID

    fun getWearableIndexFromItem(itemId: UUID): Int {
        wearables.values.forEach { slots ->
            val idx = slots.indexOfFirst { it.itemId == itemId }
            if (idx >= 0) return idx
        }
        return -1
    }

    fun getWearableItemIDs(): List<UUID> =
        wearables.values.flatten().map { it.itemId }

    fun getWearableItemIDs(type: WearableType): List<UUID> =
        wearables[type]?.map { it.itemId } ?: emptyList()

    fun setWearable(type: WearableType, index: Int, wearable: ViewerWearable?) {
        val slots = wearables.getOrPut(type) { mutableListOf() }
        when {
            wearable == null && index < slots.size -> slots.removeAt(index)
            wearable != null && index < slots.size -> slots[index] = wearable
            wearable != null                       -> slots.add(wearable)
        }
    }

    fun addWearable(wearable: ViewerWearable): Boolean {
        val slots = wearables.getOrPut(wearable.type) { mutableListOf() }
        if (slots.size >= MAX_WEARABLES_PER_TYPE) return false
        slots.add(wearable)
        return true
    }

    fun removeWearable(type: WearableType, doRemoveAll: Boolean = false, index: Int = 0) {
        val slots = wearables[type] ?: return
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

    fun canWearableBeRemoved(wearable: ViewerWearable): Boolean {
        val isBodyPart = wearable.type.ordinal < WearableType.BODY_PART_COUNT
        return !isBodyPart || getWearableCount(wearable.type) > 1
    }

    private fun canForceRemoveType(type: WearableType): Boolean =
        type.ordinal >= WearableType.BODY_PART_COUNT || getWearableCount(type) > 1

    fun isWearableModifiable(type: WearableType, index: Int = 0): Boolean =
        getWearable(type, index)?.isModifiable ?: false

    fun isWearableModifiable(itemId: UUID): Boolean =
        getWearableFromItemId(itemId)?.isModifiable ?: false

    fun isWearableCopyable(type: WearableType, index: Int = 0): Boolean =
        getWearable(type, index)?.isCopyable ?: false

    fun canMoveWearable(itemId: UUID, closerToBody: Boolean): Boolean =
        TODO("canMoveWearable: check ordering constraints")

    fun moveWearable(itemId: UUID, closerToBody: Boolean): Boolean =
        TODO("moveWearable: reorder within type")

    fun setWearableOutfit(items: List<UUID>, wearableList: List<ViewerWearable>) {
        wearables.values.forEach { it.clear() }
        wearableList.forEach { addWearable(it) }
        TODO("setWearableOutfit: notify and trigger bake")
    }

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

    fun revertWearable(type: WearableType, index: Int) {
        TODO("revertWearable($type, $index)")
    }

    fun createStandardWearables() {
        TODO("createStandardWearables: generate defaults for bare avatar")
    }

    private var itemToEdit: UUID = NULL_UUID

    fun requestEditingWearable(itemId: UUID) {
        itemToEdit = itemId
    }

    fun editWearableIfRequested(itemId: UUID) {
        if (itemToEdit == itemId) {
            itemToEdit = NULL_UUID
            TODO("editWearableIfRequested: open wearable editor for $itemId")
        }
    }

    fun setWearableName(itemId: UUID, newName: String) {
        TODO("setWearableName($itemId, $newName)")
    }

    fun createWearable(type: WearableType, wear: Boolean = false, parentId: UUID = NULL_UUID, createdCb: ((UUID) -> Unit)? = null) {
        TODO("createWearable($type)")
    }

    fun editWearable(itemId: UUID) {
        TODO("editWearable($itemId)")
    }

    fun sendAgentWearablesUpdate() {
        TODO("sendAgentWearablesUpdate: build and send AgentWearablesUpdate message")
    }

    fun sendAgentWearablesRequest() {
        TODO("sendAgentWearablesRequest")
    }

    fun sendDummyAgentWearablesUpdate() {
        TODO("sendDummyAgentWearablesUpdate: compatibility shim for old sims")
    }

    fun queryWearableCache() {
        TODO("queryWearableCache")
    }

    fun animateAllWearableParams(delta: Float, uploadBake: Boolean) {
        TODO("animateAllWearableParams: drive morph parameters on avatar mesh")
    }

    private val loadingStartedListeners: MutableList<WearablesLoadedCallback> = mutableListOf()
    private val loadedListeners: MutableList<WearablesLoadedCallback> = mutableListOf()
    private val initialLoadedListeners: MutableList<WearablesLoadedCallback> = mutableListOf()

    fun addLoadingStartedCallback(cb: WearablesLoadedCallback) { loadingStartedListeners += cb }
    fun addLoadedCallback(cb: WearablesLoadedCallback)         { loadedListeners += cb }
    fun addInitialWearablesLoadedCallback(cb: WearablesLoadedCallback) { initialLoadedListeners += cb }

    fun notifyLoadingStarted() {
        cofChangeInProgress = true
        loadingStartedListeners.forEach { it() }
    }

    fun notifyLoadingFinished() {
        wearablesLoaded = true
        cofChangeInProgress = false
        loadedListeners.forEach { it() }
        if (!initialWearablesLoaded) {
            initialWearablesLoaded = true
            initialLoadedListeners.forEach { it() }
        }
    }

    fun updateWearablesLoaded() {
        wearablesLoaded = wearables.values.flatten().isNotEmpty()
    }

    fun changeInProgress(): Boolean = cofChangeInProgress

    fun cleanup() {
        wearables.values.forEach { it.clear() }
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

    private val NULL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
}
