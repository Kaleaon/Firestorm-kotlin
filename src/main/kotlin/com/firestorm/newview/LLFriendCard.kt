package com.firestorm.newview

import java.util.UUID

private const val INVENTORY_STRING_FRIENDS_SUBFOLDER = "Friends"
private const val INVENTORY_STRING_FRIENDS_ALL_SUBFOLDER = "All"

private fun getFriendFolderName(): String = INVENTORY_STRING_FRIENDS_SUBFOLDER
private fun getFriendAllSubfolderName(): String = INVENTORY_STRING_FRIENDS_ALL_SUBFOLDER

// Stub interfaces representing the inventory/avatar-tracker layer that must be
// provided by a JVM-compatible implementation.
interface LLInventoryItem {
    fun getType(): Int
    fun getCreatorUUID(): UUID
    fun getUUID(): UUID
    fun getName(): String
}

interface LLInventoryCategory {
    fun getType(): Int
    fun getUUID(): UUID
    fun getName(): String
}

interface LLInventoryObject {
    fun getType(): Int
    fun getName(): String
}

interface LLViewerInventoryItem : LLInventoryItem
interface LLViewerInventoryCategory : LLInventoryCategory

// Observer pattern: inventory observers are notified of friendship changes.
interface LLFriendObserver {
    fun changed(mask: UInt)
}

object LLFriendCardsManager : LLFriendObserver {

    enum class EManagerState {
        INIT,
        LOADING_FRIENDS_FOLDER,
        LOADING_ALL_FOLDER,
        MANAGER_READY
    }

    // folderid -> list of buddy UUIDs
    data class FolderBuddiesMap(val map: MutableMap<UUID, MutableList<UUID>> = mutableMapOf())

    private val mBuddyIDSet: MutableSet<UUID> = mutableSetOf()
    private var mState: EManagerState = EManagerState.INIT

    override fun changed(mask: UInt) {
        onFriendListUpdate(mask)
    }

    fun isManagerReady(): Boolean = mState == EManagerState.MANAGER_READY

    fun getManagerState(): EManagerState = mState

    fun isItemInAnyFriendsList(item: LLViewerInventoryItem): Boolean {
        TODO("APR: use JVM equivalent — query inventory model for calling-card items matching item.getCreatorUUID()")
    }

    fun isObjDirectDescendentOfCategory(obj: LLInventoryObject?, cat: LLViewerInventoryCategory?): Boolean {
        if (obj == null || cat == null) return false
        if (!isCategoryInFriendFolder(cat)) return false
        TODO("APR: use JVM equivalent — lock and scan direct descendents of cat for obj match by type+name or creator UUID")
    }

    fun isCategoryInFriendFolder(cat: LLViewerInventoryCategory?): Boolean {
        if (cat == null) return false
        TODO("APR: use JVM equivalent — check gInventory.isObjectDescendentOf(cat.getUUID(), findFriendFolderUUIDImpl())")
    }

    fun isAnyFriendCategory(catID: UUID): Boolean {
        TODO("APR: use JVM equivalent — compare catID to friend folder UUID or check descendant relationship")
    }

    fun isAvatarDataStored(avatarID: UUID): Boolean =
        mBuddyIDSet.contains(avatarID)

    fun syncFriendCardsFolders() {
        TODO("APR: use JVM equivalent — fetch calling-cards folder descendants then call ensureFriendsFolderExists()")
    }

    fun createAgentCallingCard() {
        TODO("APR: use JVM equivalent — fetch calling-cards folder, check for agent card, create if missing")
    }

    // ---- private helpers ----

    private fun putAvatarData(avatarID: UUID) {
        val added = mBuddyIDSet.add(avatarID)
        if (!added) {
            println("WARN LLFriendCardsManager: Trying to add avatar UUID for already-stored avatar: $avatarID")
        }
    }

    private fun extractAvatarID(avatarID: UUID): UUID {
        if (!mBuddyIDSet.remove(avatarID)) {
            println("WARN LLFriendCardsManager: extractAvatarID called for non-existent avatar: $avatarID")
            return UUID(0L, 0L)
        }
        return avatarID
    }

    private fun findChildFolderUUID(parentFolderUUID: UUID, nonLocalizedName: String): UUID {
        TODO("APR: use JVM equivalent — collectDescendentsIf with name-match functor")
    }

    private fun findFirstCallingCardSubfolder(parentId: UUID): UUID {
        TODO("APR: use JVM equivalent — getDirectDescendentsOf and find first FT_CALLINGCARD sub-category")
    }

    private fun findFriendFolderUUIDImpl(): UUID {
        TODO("APR: use JVM equivalent — findCategoryUUIDForType(FT_CALLINGCARD) then findFirstCallingCardSubfolder")
    }

    private fun findFriendAllSubfolderUUIDImpl(): UUID {
        TODO("APR: use JVM equivalent — findFriendFolderUUIDImpl then findFirstCallingCardSubfolder")
    }

    private fun findFriendCardInventoryUUIDImpl(avatarID: UUID): UUID {
        TODO("APR: use JVM equivalent — collectDescendents of Friends/All and match by creator UUID")
    }

    private fun findMatchedFriendCards(avatarID: UUID): List<LLViewerInventoryItem> {
        TODO("APR: use JVM equivalent — walk friend folder tree collecting items whose creator == avatarID")
    }

    private fun fetchAndCheckFolderDescendents(folderId: UUID, cb: () -> Unit) {
        TODO("APR: use JVM equivalent — async inventory fetch with callback on completion")
    }

    // Ensures Friends folder exists under Calling Cards; creates it and All sub-folder if absent,
    // then synchronises contents with the buddy list.
    private fun ensureFriendsFolderExists() {
        TODO("APR: use JVM equivalent — findFriendFolderUUIDImpl; create FT_CALLINGCARD category if missing")
    }

    private fun ensureFriendsAllFolderExists() {
        TODO("APR: use JVM equivalent — findFriendAllSubfolderUUIDImpl; create FT_CALLINGCARD sub-category if missing")
    }

    private fun syncFriendsFolder() {
        TODO("APR: use JVM equivalent — copyBuddyList, check for agent calling card, create missing friend cards")
    }

    private fun addFriendCardToInventory(avatarID: UUID) {
        TODO("APR: use JVM equivalent — check isManagerReady, findFriendCardInventoryUUIDImpl, isAvatarDataStored, then create_inventory_callingcard")
    }

    private fun removeFriendCardFromInventory(avatarID: UUID) {
        TODO("APR: use JVM equivalent — findMatchedFriendCards then gInventory.removeItem for each")
    }

    private fun onFriendListUpdate(changedMask: UInt) {
        TODO("APR: use JVM equivalent — dispatch ADD→addFriendCardToInventory, REMOVE→removeFriendCardFromInventory for each changed ID")
    }
}
