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
        System.err.println("FSFloaterContacts: Wire up tab container, friend list, filter editors, buttons, and signals not yet implemented")
        return false
    }

    fun onOpen(key: LLSD) {
        System.err.println("FSFloaterContacts: Handle ContactsTornOff tear-off logic; call openTab(key.asString()) not yet implemented")
    }

    fun draw() {
        if (resetLastColumnDisplayModeChanged) {
            resetLastColumnDisplayModeChanged = false
            System.err.println("FSFloaterContacts: Restore column display mode setting: $lastColumnDisplayModeChanged not yet implemented")
        }
        if (dirtyNames) {
            onDisplayNameChanged()
            dirtyNames = false
            System.err.println("FSFloaterContacts: Mark friend list as needing sort not yet implemented")
        }
        System.err.println("FSFloaterContacts: Call super.draw() not yet implemented")
    }

    fun tick(): Boolean {
        onDisplayNameChanged()
        return false
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        System.err.println("FSFloaterContacts: Handle filter-editor shortcut and Ctrl+W close-host not yet implemented")
        return false
    }

    fun changed(changedMask: UInt) {
        System.err.println("FSFloaterContacts: Decode changedMask (ADD|ONLINE, ADD, REMOVE, POWERS, ONLINE); update list items accordingly not yet implemented")
    }

    fun openTab(name: String) {
        when (name) {
            "friends"      -> childShowTab("friends_and_groups", FRIENDS_TAB_NAME)
            "groups"       -> { childShowTab("friends_and_groups", GROUP_TAB_NAME); updateGroupButtons() }
            "contact_sets" -> childShowTab("friends_and_groups", "contact_sets_panel")
            else           -> return
        }
        System.err.println("FSFloaterContacts: Show/focus the host container or this floater directly not yet implemented")
    }

    fun getPanelByName(panelName: String): Any? {
        System.err.println("FSFloaterContacts: Return mTabContainer.getPanelByName(panelName) not yet implemented")
        return null
    }

    fun sortFriendList() {
        System.err.println("FSFloaterContacts: Clear sort order, set sort column based on FSFriendListSortOrder setting, re-sort by display_name or user_name then icon_online_status not yet implemented")
    }

    fun onDisplayNameChanged() {
        dirtyNames = true
        System.err.println("FSFloaterContacts: For each item in friend list: fetch avatar name from cache; update columns; request async fetch if not cached not yet implemented")
    }

    fun resetFriendFilter() {
        friendFilterSubString = ""
        friendFilterSubStringOrig = ""
        System.err.println("FSFloaterContacts: Clear filter editor text; call onFriendFilterEdit(\"\") not yet implemented")
    }

    fun onGetFilterOpacityCallback(type: Int, alpha: Float): Float {
        val ttActive = 0
        val imOpacity = 1.0f
        return if (type != ttActive) minOf(imOpacity, alpha) else alpha
    }

    private fun getActiveTabName(): String {
        System.err.println("FSFloaterContacts: Return mTabContainer.getCurrentPanel().getName() not yet implemented")
        return ""
    }

    private fun getCurrentItemID(): LLUUID {
        val curTab = getActiveTabName()
        return when (curTab) {
            FRIENDS_TAB_NAME -> {
                System.err.println("FSFloaterContacts: Return mFriendsList.getFirstSelected()?.getUUID() ?: LLUUID.null not yet implemented")
                LLUUID.NULL
            }
            GROUP_TAB_NAME   -> {
                System.err.println("FSFloaterContacts: Return mGroupList.getSelectedUUID() not yet implemented")
                LLUUID.NULL
            }
            else             -> LLUUID.NULL
        }
    }

    private fun getCurrentItemIDs(selectedUuids: MutableList<LLUUID>) {
        val curTab = getActiveTabName()
        when (curTab) {
            FRIENDS_TAB_NAME -> getCurrentFriendItemIDs(selectedUuids)
            GROUP_TAB_NAME   -> System.err.println("FSFloaterContacts: mGroupList.getSelectedUUIDs(selectedUuids) not yet implemented")
        }
    }

    private fun getCurrentFriendItemIDs(selectedUuids: MutableList<LLUUID>) {
        System.err.println("FSFloaterContacts: Populate selectedUuids from mFriendsList.getAllSelected() not yet implemented")
    }

    private fun refreshRightsChangeList() {
        val friends = mutableListOf<LLUUID>()
        getCurrentFriendItemIDs(friends)
        val numSelected = friends.size
        var canOfferTeleport = numSelected >= 1
        var selectedFriendsOnline = true
        System.err.println("FSFloaterContacts: Check each friend's online/RLV status; enable/disable Im and TP buttons not yet implemented")
    }

    private fun refreshUI() {
        System.err.println("FSFloaterContacts: Recompute single_selected / multiple_selected; enable/disable all action buttons; call refreshRightsChangeList() not yet implemented")
    }

    private fun updateFriendCount() {
        System.err.println("FSFloaterContacts: Query LLAvatarTracker buddy list size; update mFriendsCountTb label with COUNT arg not yet implemented")
    }

    private fun onSelectName() {
        refreshUI()
        applyRightsToFriends()
    }

    private fun addFriend(agentId: LLUUID) {
        System.err.println("FSFloaterContacts: Fetch LLRelationship from avatar tracker; build LLSD element with all columns; add to mFriendsList; apply contact-set color not yet implemented")
    }

    private fun updateFriendItem(agentId: LLUUID, info: Any?) {
        System.err.println("FSFloaterContacts: Update columns in the scroll-list row for agentId: online icon, names, rights checkboxes, font style, contact-set color not yet implemented")
    }

    private fun updateFriendItem(agentId: LLUUID, relationship: Any?, requestId: LLUUID) {
        disconnectAvatarNameCacheConnection(requestId)
        updateFriendItem(agentId, relationship)
    }

    private fun updateFriendItemColor(item: Any, agentId: LLUUID) {
        System.err.println("FSFloaterContacts: Fetch contact-set color from LGGContactSets; apply or clear color on user_name, display_name, full_name cells not yet implemented")
    }

    private fun applyRightsToFriends() {
        if (rightsChangeNotificationTriggered) return
        System.err.println(
            "FSFloaterContacts: Iterate selected items; compare UI checkbox state to LLRelationship rights; " +
            "build rights_updates map; call confirmModifyRights or sendRightsGrant not yet implemented"
        )
    }

    private fun confirmModifyRights(ids: Map<LLUUID, Int>, command: GrantRevoke) {
        if (ids.isEmpty()) return
        System.err.println("FSFloaterContacts: Show GrantModifyRights / RevokeModifyRights notification with modifyRightsConfirmation callback not yet implemented")
    }

    private fun modifyRightsConfirmation(notification: Any, response: Any, rights: Map<LLUUID, Int>): Boolean {
        rightsChangeNotificationTriggered = false
        System.err.println("FSFloaterContacts: If option 0 selected call sendRightsGrant(rights), else resync view from model; call refreshUI() not yet implemented")
        return false
    }

    private fun sendRightsGrant(ids: Map<LLUUID, Int>) {
        if (ids.isEmpty()) return
        numRightsChanged = ids.size
        System.err.println("FSFloaterContacts: Send GrantUserRights message via gMessageSystem for each id; call gAgent.sendReliableMessage() not yet implemented")
    }

    private fun isItemsFreeOfFriends(uuids: List<LLUUID>): Boolean {
        System.err.println("FSFloaterContacts: Return true iff none of the uuids is already a buddy in LLAvatarTracker not yet implemented")
        return false
    }

    private fun childShowTab(id: String, tabname: String) {
        System.err.println("FSFloaterContacts: Find LLTabContainer child '$id'; call selectTabByName('$tabname') not yet implemented")
    }

    private fun updateRlvRestrictions(behavior: String) {
        if (behavior == "showloc" || behavior == "showworldmap" || behavior == "pay") {
            refreshUI()
        }
    }

    private fun onColumnDisplayModeChanged(settingsName: String = "") {
        lastColumnDisplayModeChanged = settingsName
        System.err.println(
            "FSFloaterContacts: Validate at least one column visible; rebuild mFriendsList columns based on " +
            "FSFriendListColumnShow* and FSFriendListColumnShowPermissions settings; re-sort not yet implemented"
        )
    }

    private fun onFriendFilterEdit(searchString: String) {
        friendFilterSubStringOrig = searchString.trimStart()
        val searchUpper = friendFilterSubStringOrig.uppercase()
        if (friendFilterSubString == searchUpper) return
        friendFilterSubString = searchUpper
        System.err.println("FSFloaterContacts: mFriendsList.setFilterString(friendFilterSubStringOrig) not yet implemented")
    }

    private fun onGroupFilterEdit(searchString: String) {
        System.err.println("FSFloaterContacts: mGroupList.setNameFilter(searchString) not yet implemented")
    }

    private fun onContactSetsChanged(type: String) {
        if (type == "UPDATED_LISTS" || type == "UPDATED_MEMBERS") {
            onDisplayNameChanged()
        }
    }

    private fun getFullName(avName: Any): String {
        System.err.println(
            "FSFloaterContacts: If displayName is default or UseDisplayNames=false: return userName. " +
            "Otherwise format as 'displayName (userName)' or 'userName (displayName)' " +
            "per FSFriendListFullNameFormat setting not yet implemented"
        )
        return ""
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
        System.err.println("FSFloaterContacts: Handle DAD_PERSON: accept and call requestFriendshipDialog; else forward to hit item via LLToolDragAndDrop not yet implemented")
        return false
    }

    private fun onAvatarPicked(ids: List<LLUUID>, names: List<Any>) {
        System.err.println("FSFloaterContacts: If ids/names non-empty: call LLAvatarActions.requestFriendshipDialog(ids.first, names.first.completeName) not yet implemented")
    }

    private fun onAddFriendWizButtonClicked(ctrl: Any?) {
        System.err.println("FSFloaterContacts: Show LLFloaterAvatarPicker with onAvatarPicked callback; set isItemsFreeOfFriends as OK-button enable guard not yet implemented")
    }

    private fun onViewProfileButtonClicked()   { System.err.println("FSFloaterContacts: LLAvatarActions.showProfile(getCurrentItemID()) not yet implemented") }
    private fun onImButtonClicked() {
        val selected = mutableListOf<LLUUID>()
        getCurrentItemIDs(selected)
        when {
            selected.size == 1 -> System.err.println("FSFloaterContacts: LLAvatarActions.startIM(selected.first()) not yet implemented")
            selected.size > 1  -> System.err.println("FSFloaterContacts: LLAvatarActions.startConference(selected) not yet implemented")
        }
    }
    private fun onTeleportButtonClicked() {
        val selected = mutableListOf<LLUUID>()
        getCurrentItemIDs(selected)
        System.err.println("FSFloaterContacts: LLAvatarActions.offerTeleport(selected) not yet implemented")
    }
    private fun onPayButtonClicked() {
        val id = getCurrentItemID()
        System.err.println("FSFloaterContacts: If id non-null: LLAvatarActions.pay(id) not yet implemented")
    }
    private fun onDeleteFriendButtonClicked() {
        val selected = mutableListOf<LLUUID>()
        getCurrentItemIDs(selected)
        when {
            selected.size == 1 -> System.err.println("FSFloaterContacts: LLAvatarActions.removeFriendDialog(selected.first()) not yet implemented")
            selected.size > 1  -> System.err.println("FSFloaterContacts: LLAvatarActions.removeFriendsDialog(selected) not yet implemented")
        }
    }
    private fun onMapButtonClicked() {
        val id = getCurrentItemID()
        System.err.println("FSFloaterContacts: If id non-null and is_agent_mappable: LLAvatarActions.showOnMap(id) not yet implemented")
    }

    private fun onGroupChatButtonClicked() {
        val id = getCurrentItemID()
        System.err.println("FSFloaterContacts: If id non-null: LLGroupActions.startIM(id) not yet implemented")
    }
    private fun onGroupInfoButtonClicked()     { System.err.println("FSFloaterContacts: LLGroupActions.show(getCurrentItemID()) not yet implemented") }
    private fun onGroupActivateButtonClicked() { System.err.println("FSFloaterContacts: LLGroupActions.activate(mGroupList.getSelectedUUID()) not yet implemented") }
    private fun onGroupFavoriteButtonClicked() {
        val id = getCurrentItemID()
        System.err.println("FSFloaterContacts: If id non-null: FSFavoriteGroups.toggleFavorite(id); updateGroupButtons() not yet implemented")
    }
    private fun onGroupLeaveButtonClicked() {
        val id = getCurrentItemID()
        System.err.println("FSFloaterContacts: If id non-null: LLGroupActions.leave(id) not yet implemented")
    }
    private fun onGroupCreateButtonClicked()   { System.err.println("FSFloaterContacts: LLGroupActions.createGroup() not yet implemented") }
    private fun onGroupSearchButtonClicked()   { System.err.println("FSFloaterContacts: LLGroupActions.search() not yet implemented") }
    private fun onGroupTitlesButtonClicked()   { System.err.println("FSFloaterContacts: LLFloaterReg.toggleInstance(\"fs_group_titles\") not yet implemented") }
    private fun onGroupInviteButtonClicked() {
        val id = getCurrentItemID()
        System.err.println("FSFloaterContacts: If id non-null: LLFloaterGroupInvite.showForGroup(id) not yet implemented")
    }
    private fun updateGroupButtons() {
        val groupId = getCurrentItemID()
        System.err.println(
            "FSFloaterContacts: Enable/disable group buttons based on groupId nullity, agent powers, group membership, " +
            "favorite status; update group count label; toggle favorite button label not yet implemented"
        )
    }

    companion object {
        @Volatile private var instance: FSFloaterContacts? = null

        fun getInstance(): FSFloaterContacts =
            instance ?: synchronized(this) {
                instance ?: FSFloaterContacts(LLSD.Undefined).also { instance = it }
            }

        fun findInstance(): FSFloaterContacts? = instance

        fun show() { getInstance().openTab("friends") }
        fun hide() { System.err.println("FSFloaterContacts: LLFloaterReg.hideInstance(\"imcontacts\") not yet implemented") }
        fun toggle() { System.err.println("FSFloaterContacts: LLFloaterReg.toggleInstance(\"imcontacts\") not yet implemented") }
    }
}
