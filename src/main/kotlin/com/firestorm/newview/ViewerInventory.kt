package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llinventory.AssetTypeId
import com.firestorm.llinventory.FolderType
import com.firestorm.llinventory.InventoryCategory
import com.firestorm.llinventory.InventoryItem
import com.firestorm.llinventory.Permissions
import com.firestorm.llinventory.SaleInfo

typealias InventoryFunc = (LLUUID) -> Unit
typealias NullaryFunc   = () -> Unit

fun noOpInventoryFunc(id: LLUUID) {}
fun noOp() {}

// ── ViewerInventoryItem ──────────────────────────────────────────────────────

open class ViewerInventoryItem(
    uuid: LLUUID          = LLUUID.NULL,
    parentId: LLUUID      = LLUUID.NULL,
    thumbnailId: LLUUID   = LLUUID.NULL,
    type: AssetTypeId     = -1,
    name: String          = "",
    creationDate: Long    = 0L,
    isFavorite: Boolean   = false,
    permissions: Permissions = Permissions.DEFAULT,
    assetId: LLUUID       = LLUUID.NULL,
    inventoryType: Int    = -1,
    flags: UInt           = 0u,
    saleInfo: SaleInfo    = SaleInfo.DEFAULT,
    description: String   = ""
) : InventoryItem(
    uuid, parentId, thumbnailId, type, name, creationDate, isFavorite,
    permissions, assetId, inventoryType, flags, saleInfo, description
) {
    var isComplete: Boolean = false
    var transactionId: LLUUID = LLUUID.NULL

    constructor(other: InventoryItem) : this(
        uuid = other.uuid, parentId = other.parentId, thumbnailId = other.thumbnailId,
        type = other.type, name = other.name, creationDate = other.creationDate,
        isFavorite = other.isFavorite, permissions = other.permissions,
        assetId = other.assetId, inventoryType = other.inventoryType,
        flags = other.flags, saleInfo = other.saleInfo, description = other.description
    )

    constructor(other: ViewerInventoryItem) : this(other as InventoryItem) {
        isComplete    = other.isComplete
        transactionId = other.transactionId
    }

    fun copyViewerItem(other: ViewerInventoryItem) {
        uuid = other.uuid; parentId = other.parentId; thumbnailId = other.thumbnailId
        type = other.type; name = other.name; creationDate = other.creationDate
        permissions = other.permissions; assetId = other.assetId
        inventoryType = other.inventoryType; flags = other.flags
        saleInfo = other.saleInfo; description = other.description
        isComplete    = other.isComplete
        transactionId = other.transactionId
    }

    fun cloneViewerItem(): ViewerInventoryItem {
        val clone = ViewerInventoryItem(this)
        clone.uuid = LLUUID.generate()
        return clone
    }

    open fun updateServer(isNew: Boolean) {
        if (!isComplete) return
        TODO("APR: use JVM equivalent - send UpdateInventoryItem or AIS UpdateItem to server")
    }

    open fun updateParentOnServer(restamp: Boolean) {
        TODO("APR: use JVM equivalent - send MoveInventoryItem to server")
    }

    fun fetchFromServer() {
        if (!isComplete) {
            TODO("APR: use JVM equivalent - schedule fetch via InventoryModelBackgroundFetch or HTTP cap")
        }
    }

    fun isFinished(): Boolean = isComplete
    fun setComplete(complete: Boolean) { isComplete = complete }

    fun isBrokenLink(): Boolean {
        TODO("APR: use JVM equivalent - check linked item exists in InventoryModel")
    }

    fun getLinkedItem(): ViewerInventoryItem? {
        TODO("APR: use JVM equivalent - resolve link target in InventoryModel")
    }

    fun checkPermissionsSet(mask: UInt): Boolean =
        (permissions.maskOwner and mask) == mask

    fun getSortField(): Int {
        val sortField = intArrayOf(-1)
        extractSortFieldAndDisplayName(name, sortField, null)
        return sortField[0]
    }

    companion object {
        private val SORT_FIELD_PREFIX = Regex("""^\[(\d+)\]\s*(.*)$""")

        fun extractSortFieldAndDisplayName(
            name: String,
            sortField: IntArray?,
            displayName: Array<String>?
        ): Boolean {
            val match = SORT_FIELD_PREFIX.find(name) ?: return false
            sortField?.set(0, match.groupValues[1].toIntOrNull() ?: -1)
            displayName?.set(0, match.groupValues[2].trim())
            return true
        }
    }
}

// ── ViewerInventoryCategory ──────────────────────────────────────────────────

open class ViewerInventoryCategory(
    uuid: LLUUID        = LLUUID.NULL,
    parentId: LLUUID    = LLUUID.NULL,
    preferredType: Int  = FolderType.NONE.value,
    name: String        = "",
    val ownerId: LLUUID = LLUUID.NULL
) : InventoryCategory(
    uuid = uuid, parentId = parentId, preferredType = preferredType, name = name
) {
    enum class FetchType { NONE, NORMAL, RECURSIVE, FAILED }

    var version: Int = VERSION_UNKNOWN
    var descendentCount: Int = DESCENDENT_COUNT_UNKNOWN
    private var fetching: FetchType = FetchType.NONE
    private var descendentsExpiryMs: Long = 0L

    constructor(ownerId: LLUUID) : this(
        uuid = LLUUID.NULL, parentId = LLUUID.NULL,
        preferredType = FolderType.NONE.value, name = "", ownerId = ownerId
    )

    constructor(other: ViewerInventoryCategory) : this(
        uuid = other.uuid, parentId = other.parentId, preferredType = other.preferredType,
        name = other.name, ownerId = other.ownerId
    ) {
        version = other.version
        descendentCount = other.descendentCount
        descendentsExpiryMs = other.descendentsExpiryMs
    }

    fun copyViewerCategory(other: ViewerInventoryCategory) {
        uuid = other.uuid; parentId = other.parentId; preferredType = other.preferredType
        name = other.name; version = other.version
        descendentCount = other.descendentCount
        descendentsExpiryMs = other.descendentsExpiryMs
    }

    fun fetch(expirySeconds: Int = 10): Boolean {
        if (version == VERSION_UNKNOWN && isDescendentsTimerExpired()) {
            resetDescendentsTimer(expirySeconds.toLong() * 1000L)
            TODO("APR: use JVM equivalent - schedule background fetch via InventoryModelBackgroundFetch")
            return true
        }
        return false
    }

    fun getFetching(): FetchType {
        if (isDescendentsTimerExpired()) fetching = FetchType.NONE
        return fetching
    }

    fun setFetching(type: FetchType) {
        when {
            type == FetchType.FAILED -> {
                resetDescendentsTimer(FETCH_FAILURE_EXPIRY_MS)
                fetching = type
            }
            type.ordinal > fetching.ordinal -> {
                if (isDescendentsTimerExpired() || fetching == FetchType.NONE) {
                    resetDescendentsTimer(FETCH_TIMER_EXPIRY_MS)
                }
                fetching = type
            }
            type == FetchType.NONE -> {
                descendentsExpiryMs = 0L
                fetching = type
            }
        }
    }

    fun getViewerDescendentCount(): Int {
        TODO("APR: use JVM equivalent - call InventoryModel.getDirectDescendentsOf(uuid)")
    }

    open fun updateParentOnServer(restamp: Boolean) {
        TODO("APR: use JVM equivalent - send MoveInventoryFolder to server")
    }

    open fun updateServer(isNew: Boolean) {
        if (FolderType.fromValue(preferredType) in FolderType.isProtectedTypes) return
        TODO("APR: use JVM equivalent - send UpdateInventoryFolder or AIS UpdateCategory")
    }

    fun acceptItem(item: InventoryItem): Boolean {
        if (preferredType == FolderType.MARKETPLACE_STOCK.value) {
            if (item.permissions.maskOwner and PERM_COPY != 0u) return false
            TODO("APR: use JVM equivalent - check existing items in folder via InventoryModel")
        }
        return true
    }

    fun changeType(newType: Int) {
        preferredType = newType
        TODO("APR: use JVM equivalent - send UpdateInventoryFolder or AIS UpdateCategory")
    }

    fun localizeName() {
        name = LocalizedInventoryDictionary.localize(name) ?: name
    }

    private fun isDescendentsTimerExpired(): Boolean {
        if (descendentsExpiryMs == 0L) return true
        return System.currentTimeMillis() > descendentsExpiryMs
    }

    private fun resetDescendentsTimer(durationMs: Long) {
        descendentsExpiryMs = System.currentTimeMillis() + durationMs
    }

    companion object {
        const val VERSION_UNKNOWN = -1
        const val VERSION_INITIAL = 1
        const val DESCENDENT_COUNT_UNKNOWN = -1

        private const val FETCH_FAILURE_EXPIRY_MS = 60_000L
        private const val FETCH_TIMER_EXPIRY_MS   = 30_000L
        private const val PERM_COPY: UInt          = 0x00008000u
    }
}

// ── Localized name dictionary ─────────────────────────────────────────────────

object LocalizedInventoryDictionary {
    private val dict: Map<String, String> = mapOf(
        "New Shape" to "New Shape", "New Skin" to "New Skin",
        "New Hair" to "New Hair",   "New Eyes" to "New Eyes",
        "New Shirt" to "New Shirt", "New Pants" to "New Pants",
        "New Shoes" to "New Shoes", "New Socks" to "New Socks",
        "New Jacket" to "New Jacket", "New Gloves" to "New Gloves",
        "New Undershirt" to "New Undershirt", "New Underpants" to "New Underpants",
        "New Skirt" to "New Skirt", "New Alpha" to "New Alpha",
        "New Tattoo" to "New Tattoo", "New Universal" to "New Universal",
        "New Physics" to "New Physics", "Invalid Wearable" to "Invalid Wearable",
        "New Gesture" to "New Gesture", "New Material" to "New Material",
        "New Script" to "New Script", "New Folder" to "New Folder",
        "New Note" to "New Note",   "Contents" to "Contents",
        "Gesture" to "Gesture", "Male Gestures" to "Male Gestures",
        "Female Gestures" to "Female Gestures", "Other Gestures" to "Other Gestures",
        "Speech Gestures" to "Speech Gestures", "Common Gestures" to "Common Gestures"
    )

    fun localize(name: String): String? = dict[name]
}

// ── InventoryCallback ─────────────────────────────────────────────────────────

abstract class InventoryCallback {
    abstract fun fire(itemId: LLUUID)
}

class FuncInventoryCallback(
    fireFunc: InventoryFunc,
    private val onDestroy: NullaryFunc = ::noOp
) : InventoryCallback() {
    private val fireFuncs: MutableList<InventoryFunc> = mutableListOf(fireFunc)

    fun addOnFireFunc(func: InventoryFunc) { fireFuncs.add(func) }

    override fun fire(itemId: LLUUID) { fireFuncs.forEach { it(itemId) } }
}

class AddFavoriteLandmarkCallback : InventoryCallback() {
    var targetLandmarkId: LLUUID = LLUUID.NULL
    override fun fire(itemId: LLUUID) {
        TODO("APR: use JVM equivalent - handle favorite landmark creation for item $itemId")
    }
}

// ── InventoryCallbackManager ──────────────────────────────────────────────────

object InventoryCallbackManager {
    private val callbackMap: MutableMap<UInt, InventoryCallback> = mutableMapOf()
    private var lastCallbackId: UInt = 0u

    fun registerCallback(cb: InventoryCallback): UInt {
        lastCallbackId++
        if (lastCallbackId == 0u) lastCallbackId++
        callbackMap[lastCallbackId] = cb
        return lastCallbackId
    }

    fun fire(callbackId: UInt, itemId: LLUUID) {
        if (callbackId == 0u || itemId == LLUUID.NULL) return
        callbackMap.remove(callbackId)?.fire(itemId)
    }

    fun destroy() { callbackMap.clear() }
}

// ── Top-level inventory operation stubs ───────────────────────────────────────

const val NO_INV_SUBTYPE: UByte = 0u

private const val AT_GESTURE  = 21
private const val AT_SETTINGS = 49
private const val INV_TYPE_WEARABLE  = 18
private const val INV_TYPE_SETTINGS  = 25

fun createInventoryItem(
    agentId: LLUUID, sessionId: LLUUID, parentId: LLUUID, transactionId: LLUUID,
    name: String, desc: String, assetType: Int, invType: Int, subtype: UByte,
    nextOwnerPerm: UInt, cb: InventoryCallback?
) { TODO("APR: use JVM equivalent - send CreateInventoryItem message or AIS CreateInventory") }

fun createInventoryWearable(
    agentId: LLUUID, sessionId: LLUUID, parentId: LLUUID, transactionId: LLUUID,
    name: String, desc: String, assetType: Int, wearableType: Int,
    nextOwnerPerm: UInt, cb: InventoryCallback?
) { createInventoryItem(agentId, sessionId, parentId, transactionId,
        name, desc, assetType, INV_TYPE_WEARABLE, wearableType.toUByte(), nextOwnerPerm, cb) }

fun createInventorySettings(
    agentId: LLUUID, sessionId: LLUUID, parentId: LLUUID, transactionId: LLUUID,
    name: String, desc: String, settingsType: Int, nextOwnerPerm: UInt, cb: InventoryCallback?
) { createInventoryItem(agentId, sessionId, parentId, transactionId,
        name, desc, AT_SETTINGS, INV_TYPE_SETTINGS, settingsType.toUByte(), nextOwnerPerm, cb) }

fun createInventoryCallingCard(avatarId: LLUUID, parentId: LLUUID = LLUUID.NULL, cb: InventoryCallback? = null) {
    TODO("APR: use JVM equivalent - look up avatar name then createInventoryItem(AT_CALLINGCARD)")
}

fun copyInventoryItem(
    agentId: LLUUID, currentOwner: LLUUID, itemId: LLUUID,
    parentId: LLUUID, newName: String, cb: InventoryCallback?
) { TODO("APR: use JVM equivalent - send CopyInventoryItem message") }

fun linkInventoryObject(category: LLUUID, baseObjId: LLUUID, cb: InventoryCallback?) {
    TODO("APR: use JVM equivalent - build link payload and call AIS CreateInventory or LinkInventoryItem msg")
}

fun linkInventoryArray(category: LLUUID, baseObjIds: List<LLUUID>, cb: InventoryCallback?) {
    TODO("APR: use JVM equivalent - batch link creation via AIS or individual LinkInventoryItem msgs")
}

fun moveInventoryItem(
    agentId: LLUUID, sessionId: LLUUID, itemId: LLUUID,
    parentId: LLUUID, newName: String, cb: InventoryCallback?
) { TODO("APR: use JVM equivalent - send MoveInventoryItem message") }

fun updateInventoryItem(updateItem: ViewerInventoryItem, cb: InventoryCallback?) {
    TODO("APR: use JVM equivalent - send UpdateInventoryItem or AIS UpdateItem")
}

fun updateInventoryItemById(itemId: LLUUID, updates: Map<String, Any>, cb: InventoryCallback?) {
    TODO("APR: use JVM equivalent - send AIS UpdateItem with partial field map")
}

fun updateInventoryCategory(catId: LLUUID, updates: Map<String, Any>, cb: InventoryCallback?) {
    TODO("APR: use JVM equivalent - send UpdateInventoryFolder or AIS UpdateCategory")
}

fun removeInventoryItem(itemId: LLUUID, cb: InventoryCallback?, immediateDelete: Boolean = false) {
    TODO("APR: use JVM equivalent - send RemoveInventoryItem or AIS delete")
}

fun removeInventoryCategory(catId: LLUUID, cb: InventoryCallback?) {
    TODO("APR: use JVM equivalent - send RemoveInventoryFolder or AIS delete folder")
}

fun removeInventoryObject(objectId: LLUUID, cb: InventoryCallback?) {
    if (InventoryModel.itemMap.containsKey(objectId)) removeInventoryItem(objectId, cb)
    else removeInventoryCategory(objectId, cb)
}

fun purgeDescendentsOf(catId: LLUUID, cb: InventoryCallback?) {
    TODO("APR: use JVM equivalent - purge all descendents of folder via AIS or message")
}

fun copyInventoryFromNotecard(
    destinationId: LLUUID, objectId: LLUUID, notecardInvId: LLUUID,
    src: InventoryItem, callbackId: UInt = 0u
) { TODO("APR: use JVM equivalent - request copy from notecard inventory via HTTP cap") }

fun slamInventoryFolder(folderId: LLUUID, contents: List<Map<String, Any>>, cb: InventoryCallback?) {
    TODO("APR: use JVM equivalent - send AIS/HTTP slam folder request")
}

fun removeFolderContents(folderId: LLUUID, keepOutfitLinks: Boolean, cb: InventoryCallback?) {
    TODO("APR: use JVM equivalent - remove all contents of folder, optionally preserving outfit links")
}

fun activateGestureCallback(itemId: LLUUID) {
    if (itemId == LLUUID.NULL) return
    val item = InventoryModel.getItem(itemId) ?: return
    if (item.type != AT_GESTURE) return
    TODO("APR: use JVM equivalent - activate gesture via GestureMgr")
}

fun createScriptCallback(itemId: LLUUID) {
    if (itemId == LLUUID.NULL) return
    InventoryModel.getItem(itemId) ?: return
    TODO("APR: use JVM equivalent - set default permissions for Scripts and notify observers")
}

fun createGestureCallback(itemId: LLUUID) {
    if (itemId == LLUUID.NULL) return
    InventoryModel.getItem(itemId) ?: return
    TODO("APR: use JVM equivalent - activate gesture and set default permissions for Gestures")
}

fun createNotecardCallback(itemId: LLUUID) {
    if (itemId == LLUUID.NULL) return
    InventoryModel.getItem(itemId) ?: return
    TODO("APR: use JVM equivalent - set default permissions for Notecards and notify observers")
}

fun rezAttachmentCallback(itemId: LLUUID, attachmentPoint: Int, replace: Boolean) {
    if (itemId == LLUUID.NULL) return
    InventoryModel.getItem(itemId) ?: return
    TODO("APR: use JVM equivalent - rez attachment via agent/object manager")
}
