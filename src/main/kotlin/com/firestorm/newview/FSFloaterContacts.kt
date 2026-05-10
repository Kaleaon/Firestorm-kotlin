package com.firestorm.newview

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLUUID

private const val FRIENDS_TAB_NAME   = "friends_panel"
private const val GROUP_TAB_NAME     = "groups_panel"
private const val MAX_FRIEND_SELECT  = 20u
private const val RIGHTS_CHANGE_TIMEOUT = 5f

private enum class FriendsColumnOrder {
    LIST_ONLINE_STATUS,
    LIST_FRIEND_USER_NAME,
    LIST_FRIEND_DISPLAY_NAME,
    LIST_FRIEND_NAME,
    LIST_VISIBLE_ONLINE,
    LIST_VISIBLE_MAP,
    LIST_EDIT_MINE,
    LIST_VISIBLE_MAP_THEIRS,
    LIST_EDIT_THEIRS,
    LIST_FRIEND_UPDATE_GEN
}

private enum class GrantRevoke { GRANT, REVOKE }

enum class SortOrder { BY_NAME, BY_STATUS, BY_DISPLAY_NAME }

data class ContactEntry(
    val id: LLUUID,
    val name: String,
    val isOnline: Boolean,
    val rightsGranted: UInt
)

class FSFloaterContacts(val seed: LLSD) {

    val contacts: MutableList<ContactEntry> = mutableListOf()

    private var currentSortOrder: SortOrder = SortOrder.BY_NAME
    private var friendFilterSubString: String = ""
    private var friendFilterSubStringOrig: String = ""
    private var friendListFontName: String = ""
    private var lastColumnDisplayModeChanged: String = ""
    private var resetLastColumnDisplayModeChanged: Boolean = false
    private var dirtyNames: Boolean = true
    private var allowRightsChange: Boolean = true
    private var numRightsChanged: Int = 0
    private var rightsChangeNotificationTriggered: Boolean = false

    private val rlvBehaviorCallbacks: MutableList<(String) -> Unit> = mutableListOf()
    private val contactSetChangedCallbacks: MutableList<(String) -> Unit> = mutableListOf()
    private val avatarNameCacheConnections: MutableMap<LLUUID, () -> Unit> = mutableMapOf()

    fun postBuild(): Boolean {
        TODO("Wire up tab container, friend list, filter editors, buttons, and signals")
    }

    fun onOpen(key: LLSD) {
        TODO("Handle ContactsTornOff tear-off logic; call openTab(key.asString())")
    }

    fun draw() {
        if (resetLastColumnDisplayModeChanged) {
            resetLastColumnDisplayModeChanged = false
            TODO("Restore column display mode setting: $lastColumnDisplayModeChanged")
        }
        if (dirtyNames) {
            onDisplayNameChanged()
            dirtyNames = false
            TODO("Mark friend list as needing sort")
        }
        TODO("Call super.draw()")
    }

    fun tick(): Boolean {
        onDisplayNameChanged()
        return false
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        TODO("Handle filter-editor shortcut and Ctrl+W close-host")
    }

    fun changed(changedMask: UInt) {
        TODO("Decode changedMask (ADD|ONLINE, ADD, REMOVE, POWERS, ONLINE); update list items accordingly")
    }

    fun openTab(name: String) {
        when (name) {
            "friends"      -> childShowTab("friends_and_groups", FRIENDS_TAB_NAME)
            "groups"       -> { childShowTab("friends_and_groups", GROUP_TAB_NAME); updateGroupButtons() }
            "contact_sets" -> childShowTab("friends_and_groups", "contact_sets_panel")
            else           -> return
        }
        TODO("Show/focus the host container or this floater directly")
    }

    fun getPanelByName(panelName: String): Any? {
        TODO("Return mTabContainer.getPanelByName(panelName)")
    }

    fun sortFriendList() {
        TODO("Clear sort order, set sort column based on FSFriendListSortOrder setting, re-sort by display_name or user_name then icon_online_status")
    }

    fun onDisplayNameChanged() {
        dirtyNames = true
        TODO("For each item in friend list: fetch avatar name from cache; update columns; request async fetch if not cached")
    }

    fun resetFriendFilter() {
        friendFilterSubString = ""
        friendFilterSubStringOrig = ""
        TODO("Clear filter editor text; call onFriendFilterEdit(\"\")")
    }

    fun onGetFilterOpacityCallback(type: Int, alpha: Float): Float {
        val ttActive = 0
        val imOpacity = 1.0f
        return if (type != ttActive) minOf(imOpacity, alpha) else alpha
    }

    private fun getActiveTabName(): String {
        TODO("Return mTabContainer.getCurrentPanel().getName()")
    }

    private fun getCurrentItemID(): LLUUID {
        val curTab = getActiveTabName()
        return when (curTab) {
            FRIENDS_TAB_NAME -> TODO("Return mFriendsList.getFirstSelected()?.getUUID() ?: LLUUID.null")
            GROUP_TAB_NAME   -> TODO("Return mGroupList.getSelectedUUID()")
            else             -> LLUUID.NULL
        }
    }

    private fun getCurrentItemIDs(selectedUuids: MutableList<LLUUID>) {
        val curTab = getActiveTabName()
        when (curTab) {
            FRIENDS_TAB_NAME -> getCurrentFriendItemIDs(selectedUuids)
            GROUP_TAB_NAME   -> TODO("mGroupList.getSelectedUUIDs(selectedUuids)")
        }
    }

    private fun getCurrentFriendItemIDs(selectedUuids: MutableList<LLUUID>) {
        TODO("Populate selectedUuids from mFriendsList.getAllSelected()")
    }

    private fun refreshRightsChangeList() {
        val friends = mutableListOf<LLUUID>()
        getCurrentFriendItemIDs(friends)
        val numSelected = friends.size
        var canOfferTeleport = numSelected >= 1
        var selectedFriendsOnline = true
        TODO("Check each friend's online/RLV status; enable/disable Im and TP buttons")
    }

    private fun refreshUI() {
        TODO("Recompute single_selected / multiple_selected; enable/disable all action buttons; call refreshRightsChangeList()")
    }

    private fun updateFriendCount() {
        TODO("Query LLAvatarTracker buddy list size; update mFriendsCountTb label with COUNT arg")
    }

    private fun onSelectName() {
        refreshUI()
        applyRightsToFriends()
    }

    private fun addFriend(agentId: LLUUID) {
        TODO("Fetch LLRelationship from avatar tracker; build LLSD element with all columns; add to mFriendsList; apply contact-set color")
    }

    private fun updateFriendItem(agentId: LLUUID, info: Any?) {
        TODO("Update columns in the scroll-list row for agentId: online icon, names, rights checkboxes, font style, contact-set color")
    }

    private fun updateFriendItem(agentId: LLUUID, relationship: Any?, requestId: LLUUID) {
        disconnectAvatarNameCacheConnection(requestId)
        updateFriendItem(agentId, relationship)
    }

    private fun updateFriendItemColor(item: Any, agentId: LLUUID) {
        TODO("Fetch contact-set color from LGGContactSets; apply or clear color on user_name, display_name, full_name cells")
    }

    private fun applyRightsToFriends() {
        if (rightsChangeNotificationTriggered) return
        TODO(
            "Iterate selected items; compare UI checkbox state to LLRelationship rights; " +
            "build rights_updates map; call confirmModifyRights or sendRightsGrant"
        )
    }

    private fun confirmModifyRights(ids: Map<LLUUID, Int>, command: GrantRevoke) {
        if (ids.isEmpty()) return
        TODO("Show GrantModifyRights / RevokeModifyRights notification with modifyRightsConfirmation callback")
    }

    private fun modifyRightsConfirmation(notification: Any, response: Any, rights: Map<LLUUID, Int>): Boolean {
        rightsChangeNotificationTriggered = false
        TODO("If option 0 selected call sendRightsGrant(rights), else resync view from model; call refreshUI()")
    }

    private fun sendRightsGrant(ids: Map<LLUUID, Int>) {
        if (ids.isEmpty()) return
        numRightsChanged = ids.size
        TODO("Send GrantUserRights message via gMessageSystem for each id; call gAgent.sendReliableMessage()")
    }

    private fun isItemsFreeOfFriends(uuids: List<LLUUID>): Boolean {
        TODO("Return true iff none of the uuids is already a buddy in LLAvatarTracker")
    }

    private fun childShowTab(id: String, tabname: String) {
        TODO("Find LLTabContainer child '$id'; call selectTabByName('$tabname')")
    }

    private fun updateRlvRestrictions(behavior: String) {
        if (behavior == "showloc" || behavior == "showworldmap" || behavior == "pay") {
            refreshUI()
        }
    }

    private fun onColumnDisplayModeChanged(settingsName: String = "") {
        lastColumnDisplayModeChanged = settingsName
        TODO(
            "Validate at least one column visible; rebuild mFriendsList columns based on " +
            "FSFriendListColumnShow* and FSFriendListColumnShowPermissions settings; re-sort"
        )
    }

    private fun onFriendFilterEdit(searchString: String) {
        friendFilterSubStringOrig = searchString.trimStart()
        val searchUpper = friendFilterSubStringOrig.uppercase()
        if (friendFilterSubString == searchUpper) return
        friendFilterSubString = searchUpper
        TODO("mFriendsList.setFilterString(friendFilterSubStringOrig)")
    }

    private fun onGroupFilterEdit(searchString: String) {
        TODO("mGroupList.setNameFilter(searchString)")
    }

    private fun onContactSetsChanged(type: String) {
        if (type == "UPDATED_LISTS" || type == "UPDATED_MEMBERS") {
            onDisplayNameChanged()
        }
    }

    private fun getFullName(avName: Any): String {
        TODO(
            "If displayName is default or UseDisplayNames=false: return userName. " +
            "Otherwise format as 'displayName (userName)' or 'userName (displayName)' " +
            "per FSFriendListFullNameFormat setting"
        )
    }

    private fun setDirtyNames(requestId: LLUUID) {
        disconnectAvatarNameCacheConnection(requestId)
        dirtyNames = true
    }

    private fun disconnectAvatarNameCacheConnection(requestId: LLUUID) {
        avatarNameCacheConnections.remove(requestId)
    }

    private fun handleFriendsListDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: Int, cargoData: Any?, accept: Any
    ): Boolean {
        TODO("Handle DAD_PERSON: accept and call requestFriendshipDialog; else forward to hit item via LLToolDragAndDrop")
    }

    private fun onAvatarPicked(ids: List<LLUUID>, names: List<Any>) {
        TODO("If ids/names non-empty: call LLAvatarActions.requestFriendshipDialog(ids.first, names.first.completeName)")
    }

    private fun onAddFriendWizButtonClicked(ctrl: Any?) {
        TODO("Show LLFloaterAvatarPicker with onAvatarPicked callback; set isItemsFreeOfFriends as OK-button enable guard")
    }

    private fun onViewProfileButtonClicked()   { TODO("LLAvatarActions.showProfile(getCurrentItemID())") }
    private fun onImButtonClicked() {
        val selected = mutableListOf<LLUUID>()
        getCurrentItemIDs(selected)
        when {
            selected.size == 1 -> TODO("LLAvatarActions.startIM(selected.first())")
            selected.size > 1  -> TODO("LLAvatarActions.startConference(selected)")
        }
    }
    private fun onTeleportButtonClicked() {
        val selected = mutableListOf<LLUUID>()
        getCurrentItemIDs(selected)
        TODO("LLAvatarActions.offerTeleport(selected)")
    }
    private fun onPayButtonClicked() {
        val id = getCurrentItemID()
        TODO("If id non-null: LLAvatarActions.pay(id)")
    }
    private fun onDeleteFriendButtonClicked() {
        val selected = mutableListOf<LLUUID>()
        getCurrentItemIDs(selected)
        when {
            selected.size == 1 -> TODO("LLAvatarActions.removeFriendDialog(selected.first())")
            selected.size > 1  -> TODO("LLAvatarActions.removeFriendsDialog(selected)")
        }
    }
    private fun onMapButtonClicked() {
        val id = getCurrentItemID()
        TODO("If id non-null and is_agent_mappable: LLAvatarActions.showOnMap(id)")
    }

    private fun onGroupChatButtonClicked() {
        val id = getCurrentItemID()
        TODO("If id non-null: LLGroupActions.startIM(id)")
    }
    private fun onGroupInfoButtonClicked()     { TODO("LLGroupActions.show(getCurrentItemID())") }
    private fun onGroupActivateButtonClicked() { TODO("LLGroupActions.activate(mGroupList.getSelectedUUID())") }
    private fun onGroupFavoriteButtonClicked() {
        val id = getCurrentItemID()
        TODO("If id non-null: FSFavoriteGroups.toggleFavorite(id); updateGroupButtons()")
    }
    private fun onGroupLeaveButtonClicked() {
        val id = getCurrentItemID()
        TODO("If id non-null: LLGroupActions.leave(id)")
    }
    private fun onGroupCreateButtonClicked()   { TODO("LLGroupActions.createGroup()") }
    private fun onGroupSearchButtonClicked()   { TODO("LLGroupActions.search()") }
    private fun onGroupTitlesButtonClicked()   { TODO("LLFloaterReg.toggleInstance(\"fs_group_titles\")") }
    private fun onGroupInviteButtonClicked() {
        val id = getCurrentItemID()
        TODO("If id non-null: LLFloaterGroupInvite.showForGroup(id)")
    }
    private fun updateGroupButtons() {
        val groupId = getCurrentItemID()
        TODO(
            "Enable/disable group buttons based on groupId nullity, agent powers, group membership, " +
            "favorite status; update group count label; toggle favorite button label"
        )
    }

    companion object {
        @Volatile private var instance: FSFloaterContacts? = null

        fun getInstance(): FSFloaterContacts =
            instance ?: synchronized(this) {
                instance ?: FSFloaterContacts(LLSD()).also { instance = it }
            }

        fun findInstance(): FSFloaterContacts? = instance

        fun show() { getInstance().openTab("friends") }
        fun hide() { TODO("LLFloaterReg.hideInstance(\"imcontacts\")") }
        fun toggle() { TODO("LLFloaterReg.toggleInstance(\"imcontacts\")") }
    }
}
