package com.firestorm.newview

import com.firestorm.ui.Panel
import com.firestorm.ui.AvatarList
import com.firestorm.ui.GroupList
import com.firestorm.ui.TabContainer
import com.firestorm.ui.FilterEditor
import com.firestorm.ui.Button
import com.firestorm.ui.TextBox
import com.firestorm.ui.ComboBox
import com.firestorm.ui.UICtrl
import com.firestorm.ui.AccordionCtrlTab
import com.firestorm.ui.FloaterHandle
import com.firestorm.ui.NetMap
import com.firestorm.types.UUID
import com.firestorm.types.LLSD
import com.firestorm.voice.VoiceClientStatusObserver
import com.firestorm.voice.StatusType
import com.firestorm.avatar.AvatarName
import com.firestorm.avatar.AvatarListItem
import com.firestorm.avatar.AvatarItemComparator
import com.firestorm.avatar.AvatarItemNameComparator
import com.firestorm.people.RecentPeople
import com.firestorm.people.AvatarTracker
import com.firestorm.people.FriendObserver
import com.firestorm.people.Speaker
import com.firestorm.people.ActiveSpeakerMgr
import com.firestorm.contactsets.LGGContactSets

const val FRIEND_LIST_UPDATE_TIMEOUT: Float = 0.5f
const val NEARBY_LIST_UPDATE_INTERVAL: Float = 1.0f
const val MAX_SELECTIONS: UInt = 20u

private const val NEARBY_TAB_NAME = "nearby_panel"
private const val FRIENDS_TAB_NAME = "friends_panel"
private const val GROUP_TAB_NAME = "groups_panel"
private const val RECENT_TAB_NAME = "recent_panel"
private const val BLOCKED_TAB_NAME = "blocked_panel"
private const val CONTACT_SETS_TAB_NAME = "contact_sets_panel"
private const val COLLAPSED_BY_USER = "collapsed_by_user"

private class AvatarItemRecentComparator : AvatarItemComparator() {
    override fun doCompare(item1: AvatarListItem, item2: AvatarListItem): Boolean {
        val date1 = RecentPeople.instance().getDate(item1.avatarId)
        val date2 = RecentPeople.instance().getDate(item2.avatarId)
        return date1 > date2
    }
}

private class AvatarItemStatusComparator : AvatarItemComparator() {
    override fun doCompare(item1: AvatarListItem, item2: AvatarListItem): Boolean {
        val at = AvatarTracker.instance()
        val online1 = at.isBuddyOnline(item1.avatarId)
        val online2 = at.isBuddyOnline(item2.avatarId)
        if (online1 == online2) {
            val name1 = getComparableName(item1)
            val name2 = getComparableName(item2)
            return name1 < name2
        }
        return online1 > online2
    }
}

private class AvatarItemOnlineSipStatusComparator : AvatarItemComparator() {
    override fun doCompare(item1: AvatarListItem, item2: AvatarListItem): Boolean {
        val online1 = AvatarTracker.instance().isBuddyOnline(item1.avatarId)
        val online2 = AvatarTracker.instance().isBuddyOnline(item2.avatarId)
        if (online1 == online2) {
            return getComparableName(item1) < getComparableName(item2)
        }
        return online1 > online2
    }
}

private class AvatarItemDistanceComparator : AvatarItemComparator() {
    private val avatarsPositions: MutableMap<UUID, DoubleArray> = mutableMapOf()

    fun updateAvatarsPositions(positions: List<DoubleArray>, uuids: List<UUID>) {
        avatarsPositions.clear()
        positions.zip(uuids).forEach { (pos, id) -> avatarsPositions[id] = pos }
    }

    fun getAvatarsPositions(): Map<UUID, DoubleArray> = avatarsPositions

    override fun doCompare(item1: AvatarListItem, item2: AvatarListItem): Boolean {
        TODO("APR: use JVM equivalent - compute squared distances from agent position")
    }
}

private class AvatarItemRecentSpeakerComparator : AvatarItemNameComparator() {
    override fun doCompare(item1: AvatarListItem, item2: AvatarListItem): Boolean {
        val lhs: Speaker? = ActiveSpeakerMgr.instance().findSpeaker(item1.avatarId)
        val rhs: Speaker? = ActiveSpeakerMgr.instance().findSpeaker(item2.avatarId)
        return when {
            lhs != null && rhs != null -> lhs.lastSpokeTime > rhs.lastSpokeTime
            lhs != null -> true
            rhs != null -> false
            else -> super.doCompare(item1, item2)
        }
    }
}

private class AvatarItemRecentArrivalComparator : AvatarItemNameComparator() {
    override fun doCompare(item1: AvatarListItem, item2: AvatarListItem): Boolean {
        val arrTime1 = RecentPeople.instance().getArrivalTimeByID(item1.avatarId)
        val arrTime2 = RecentPeople.instance().getArrivalTimeByID(item2.avatarId)
        if (arrTime1 == arrTime2) {
            return getComparableName(item1) < getComparableName(item2)
        }
        return arrTime1 > arrTime2
    }
}

class PanelPeople : Panel(), FriendObserver, VoiceClientStatusObserver {

    enum class SortOrder {
        E_SORT_BY_NAME,
        E_SORT_BY_STATUS,
        E_SORT_BY_MOST_RECENT,
        E_SORT_BY_DISTANCE,
        E_SORT_BY_RECENT_SPEAKERS,
        E_SORT_BY_RECENT_ARRIVAL,
        E_SORT_BY_USERNAME,
    }

    abstract class Updater(protected val callback: () -> Unit) {
        open fun setActive(active: Boolean) {}
        protected fun update() = callback()
    }

    private var tabContainer: TabContainer? = null
    private var onlineFriendList: AvatarList? = null
    private var allFriendList: AvatarList? = null
    private var nearbyList: AvatarList? = null
    private var contactSetList: AvatarList? = null
    private var radarPanel: Any? = null
    private var recentList: AvatarList? = null
    private var groupList: GroupList? = null
    private var miniMap: NetMap? = null
    private var friendsTabContainer: TabContainer? = null

    private var friendsAllTab: AccordionCtrlTab? = null
    private var friendsOnlineTab: AccordionCtrlTab? = null

    private var nearbyGearBtn: Button? = null
    private var friendsGearBtn: Button? = null
    private var recentGearBtn: Button? = null
    private var groupDelBtn: Button? = null
    private var nearbyAddFriendBtn: Button? = null
    private var recentAddFriendBtn: Button? = null
    private var friendsDelFriendBtn: UICtrl? = null
    private var groupCountText: TextBox? = null

    private val savedOriginalFilters: MutableList<String> = mutableListOf()
    private val savedFilters: MutableList<String> = mutableListOf()

    private var friendListUpdater: Updater? = null
    private var recentListUpdater: Updater? = null
    private var buttonsUpdater: Updater? = null
    private var picker: FloaterHandle = FloaterHandle.DEAD

    private var nearbyFilterCommitConnection: Any? = null
    private var friesFilterCommitConnection: Any? = null
    private var groupsFilterCommitConnection: Any? = null
    private var recentFilterCommitConnection: Any? = null
    private var contactSetsFilterCommitConnection: Any? = null

    private var contactSetCombo: ComboBox? = null
    private var contactSetChangedConnection: Any? = null

    init {
        friendListUpdater = object : Updater({ updateFriendList() }) {}
        recentListUpdater = object : Updater({ updateRecentList() }) {}
        buttonsUpdater = object : Updater({ updateButtons() }) {}

        contactSetChangedConnection = LGGContactSets.getInstance()
            .setContactSetChangeCallback { type -> updateContactSets(type) }
        AvatarTracker.instance().addObserver(this)
    }

    override fun postBuild(): Boolean {
        val groupTab = getChild<Panel>(GROUP_TAB_NAME)
        groupDelBtn = groupTab.getChild("minus_btn")
        groupCountText = groupTab.getChild("groupcount")

        tabContainer = getChild<TabContainer>("tabs").also { tabs ->
            tabs.setCommitCallback { param -> onTabSelected(param) }
            savedFilters.ensureCapacity(tabs.tabCount)
            savedOriginalFilters.ensureCapacity(tabs.tabCount)
        }

        val friendsTab = getChild<Panel>(FRIENDS_TAB_NAME).also { tab ->
            tab.setVisibleCallback { visible -> friendListUpdater?.setActive(visible) }
            tab.setVisibleCallback { _ -> removePicker() }
        }
        friendsGearBtn = friendsTab.getChild("gear_btn")
        friendsDelFriendBtn = friendsTab.getChild("friends_del_btn")
        friendsTabContainer = friendsTab.findChild("friends_accordion")
        onlineFriendList = friendsTab.getChild<AvatarList>("avatars_online").also { list ->
            list.setNoItemsCommentText(getString("no_friends_online"))
            list.setShowIcons("FriendsListShowIcons")
            list.setUseContactSetColors(true)
        }
        allFriendList = friendsTab.getChild<AvatarList>("avatars_all").also { list ->
            list.setNoItemsCommentText(getString("no_friends"))
            list.setShowIcons("FriendsListShowIcons")
            list.setUseContactSetColors(true)
        }

        val nearbyTab = getChild<Panel>(NEARBY_TAB_NAME)
        radarPanel = nearbyTab.findChild<Any>("panel_radar")
        miniMap = nearbyTab.getChild("Net Map", true)
        nearbyGearBtn = nearbyTab.getChild("gear_btn")
        nearbyAddFriendBtn = nearbyTab.getChild("add_friend_btn")

        val recentTab = getChild<Panel>(RECENT_TAB_NAME)
        recentList = recentTab.getChild<AvatarList>("avatar_list").also { list ->
            list.setNoItemsCommentText(getString("no_recent_people"))
            list.setShowIcons("RecentListShowIcons")
            list.setItemDoubleClickCallback { ctrl -> onAvatarListDoubleClicked(ctrl) }
            list.setCommitCallback { onAvatarListCommitted(list) }
            list.setReturnCallback { onImButtonClicked() }
        }
        recentGearBtn = recentTab.getChild("gear_btn")
        recentAddFriendBtn = recentTab.getChild("add_friend_btn")

        groupList = groupTab.getChild<GroupList>("group_list").also { list ->
            list.setNoItemsCommentText(getString("no_groups_msg"))
            list.setDoubleClickCallback { onChatButtonClicked() }
            list.setCommitCallback { updateButtons() }
            list.setReturnCallback { onChatButtonClicked() }
        }

        friesFilterCommitConnection = friendsTab.getChild<FilterEditor>("friends_filter_input")
            .setCommitCallback { str -> onFilterEdit(str) }
        recentFilterCommitConnection = recentTab.getChild<FilterEditor>("recent_filter_input")
            .setCommitCallback { str -> onFilterEdit(str) }
        groupsFilterCommitConnection = groupTab.getChild<FilterEditor>("groups_filter_input")
            .setCommitCallback { str -> onFilterEdit(str) }

        findChild<FilterEditor>("contact_sets_filter_input")?.let { editor ->
            contactSetsFilterCommitConnection = editor.setCommitCallback { str -> onFilterEdit(str) }
        }

        contactSetCombo = getChild<ComboBox>("combo_sets").also { combo ->
            combo.setCommitCallback { generateCurrentContactList() }
            refreshContactSets()
        }

        contactSetList = getChild<AvatarList>("contact_list").also { list ->
            list.setUseContactSetColors(true)
            list.setNoItemsCommentText(getString("empty_list"))
            list.setCommitCallback { updateButtons() }
            list.setItemDoubleClickCallback { ctrl -> onAvatarListDoubleClicked(ctrl) }
        }

        onlineFriendList?.setItemDoubleClickCallback { ctrl -> onAvatarListDoubleClicked(ctrl) }
        onlineFriendList?.setCommitCallback { onAvatarListCommitted(onlineFriendList!!) }
        onlineFriendList?.setReturnCallback { onImButtonClicked() }

        allFriendList?.setItemDoubleClickCallback { ctrl -> onAvatarListDoubleClicked(ctrl) }
        allFriendList?.setCommitCallback { onAvatarListCommitted(allFriendList!!) }
        allFriendList?.setReturnCallback { onImButtonClicked() }

        tabContainer?.selectTabByName(NEARBY_TAB_NAME)
        updateButtons()
        return true
    }

    override fun onOpen(key: LLSD) {
        TODO("APR: use JVM equivalent - handle tab pre-selection from key")
    }

    override fun notifyChildren(info: LLSD): Boolean {
        TODO("APR: use JVM equivalent - forward notification to child panels")
    }

    override fun onChange(status: StatusType, channelInfo: LLSD, proximal: Boolean) {
        if (status == StatusType.STATUS_JOINING || status == StatusType.STATUS_LEFT_CHANNEL) return
        if (contactSetList != null && shouldSortByOnlineStatusForCurrentSet()) {
            contactSetList!!.sort()
        }
        updateButtons()
    }

    override fun changed(mask: UInt) {
        if ((mask and FriendObserver.ONLINE) != 0u && contactSetList != null
            && shouldSortByOnlineStatusForCurrentSet()
        ) {
            contactSetList!!.sort()
        }
    }

    fun getNearbyList(): AvatarList? = nearbyList

    fun updateNearbyList() {
        TODO("APR: use JVM equivalent - populate nearby list from world avatars")
    }

    fun updateNearbyArrivalTime(): Boolean {
        TODO("APR: use JVM equivalent - update arrival timestamps for nearby avatars")
    }

    private fun removePicker() {
        picker.get()?.closeFloater()
    }

    private fun updateFriendListHelpText() {
        TODO("APR: use JVM equivalent - toggle empty-friends help text visibility")
    }

    private fun updateFriendList() {
        TODO("APR: use JVM equivalent - sync friend lists from AvatarTracker buddy map")
    }

    private fun updateRecentList() {
        TODO("APR: use JVM equivalent - populate recent list from RecentPeople")
    }

    private fun isItemsFreeOfFriends(uuids: List<UUID>): Boolean {
        TODO("APR: use JVM equivalent - check none of the UUIDs are in the friend list")
    }

    private fun updateButtons() {
        TODO("APR: use JVM equivalent - enable/disable action buttons based on selection")
    }

    private fun getActiveTabName(): String =
        tabContainer?.currentPanel?.name ?: ""

    private fun getCurrentItemID(): UUID {
        TODO("APR: use JVM equivalent - get selected avatar/group UUID from active tab")
    }

    private fun getCurrentItemIDs(selectedUuids: MutableList<UUID>) {
        TODO("APR: use JVM equivalent - populate list with all selected UUIDs")
    }

    private fun setSortOrder(list: AvatarList?, order: SortOrder, save: Boolean = true) {
        TODO("APR: use JVM equivalent - apply comparator to list and optionally persist setting")
    }

    private fun onFilterEdit(searchString: String) {
        TODO("APR: use JVM equivalent - apply search string to active tab list")
    }

    private fun onGroupLimitInfo() {
        TODO("APR: use JVM equivalent - show group limit info floater")
    }

    private fun onTabSelected(param: LLSD) {
        TODO("APR: use JVM equivalent - activate relevant updater and update buttons")
    }

    private fun onAddFriendButtonClicked() {
        TODO("APR: use JVM equivalent - open add-friend dialog")
    }

    private fun onAddFriendWizButtonClicked() {
        TODO("APR: use JVM equivalent - open add-friend wizard floater")
    }

    private fun onDeleteFriendButtonClicked() {
        TODO("APR: use JVM equivalent - confirm and remove selected friend")
    }

    private fun onChatButtonClicked() {
        TODO("APR: use JVM equivalent - open group chat for selected group")
    }

    private fun onGearButtonClicked(btn: UICtrl) {
        TODO("APR: use JVM equivalent - show context gear menu")
    }

    private fun onImButtonClicked() {
        TODO("APR: use JVM equivalent - start IM with selected avatar")
    }

    private fun onMoreButtonClicked() {
        TODO("APR: use JVM equivalent - show overflow actions menu")
    }

    private fun onAvatarListDoubleClicked(ctrl: UICtrl) {
        TODO("APR: use JVM equivalent - open profile or IM on double-click")
    }

    private fun onAvatarListCommitted(list: AvatarList) {
        updateButtons()
    }

    private fun onGroupPlusButtonValidate(): Boolean {
        TODO("APR: use JVM equivalent - check group join limit before enabling button")
    }

    private fun onGroupMinusButtonClicked() {
        TODO("APR: use JVM equivalent - leave selected group")
    }

    private fun onGroupPlusMenuItemClicked(userdata: LLSD) {
        TODO("APR: use JVM equivalent - create or join group based on menu item")
    }

    private fun onFriendsViewSortMenuItemClicked(userdata: LLSD) {
        TODO("APR: use JVM equivalent - change friends list sort order")
    }

    private fun onNearbyViewSortMenuItemClicked(userdata: LLSD) {
        TODO("APR: use JVM equivalent - change nearby list sort order")
    }

    private fun onGroupsViewSortMenuItemClicked(userdata: LLSD) {
        TODO("APR: use JVM equivalent - change groups list sort order")
    }

    private fun onRecentViewSortMenuItemClicked(userdata: LLSD) {
        TODO("APR: use JVM equivalent - change recent list sort order")
    }

    private fun onFriendsViewSortMenuItemCheck(userdata: LLSD): Boolean {
        TODO("APR: use JVM equivalent - return whether this sort order is active")
    }

    private fun onRecentViewSortMenuItemCheck(userdata: LLSD): Boolean {
        TODO("APR: use JVM equivalent - return whether this sort order is active")
    }

    private fun onNearbyViewSortMenuItemCheck(userdata: LLSD): Boolean {
        TODO("APR: use JVM equivalent - return whether this sort order is active")
    }

    private fun onFriendsAccordionExpandedCollapsed(ctrl: UICtrl, param: LLSD, avatarList: AvatarList) {
        val expanded = param.asBoolean()
        setAccordionCollapsedByUser(ctrl, !expanded)
        if (!expanded) avatarList.resetSelection()
    }

    private fun showAccordion(tab: AccordionCtrlTab, show: Boolean) {
        TODO("APR: use JVM equivalent - show/hide accordion tab")
    }

    private fun showFriendsAccordionsIfNeeded() {
        TODO("APR: use JVM equivalent - make friends online/all accordion tabs visible")
    }

    private fun onFriendListRefreshComplete(ctrl: UICtrl, param: LLSD) {
        updateFriendListHelpText()
        showFriendsAccordionsIfNeeded()
    }

    private fun setAccordionCollapsedByUser(accTab: UICtrl, collapsed: Boolean) {
        accTab.setValue(LLSD.fromBoolean(collapsed))
    }

    private fun setAccordionCollapsedByUser(name: String, collapsed: Boolean) {
        findChild<UICtrl>(name)?.let { setAccordionCollapsedByUser(it, collapsed) }
    }

    private fun isAccordionCollapsedByUser(accTab: UICtrl): Boolean =
        accTab.getValue().asBoolean()

    private fun isAccordionCollapsedByUser(name: String): Boolean =
        findChild<UICtrl>(name)?.let { isAccordionCollapsedByUser(it) } ?: false

    private fun onContactSetsEnable(userdata: LLSD): Boolean {
        TODO("APR: use JVM equivalent - check contact set action is applicable")
    }

    private fun onContactSetsMenuItemClicked(userdata: LLSD) {
        TODO("APR: use JVM equivalent - handle contact sets context menu action")
    }

    private fun handlePickerCallback(ids: List<UUID>, set: String) {
        TODO("APR: use JVM equivalent - add picked avatars to contact set")
    }

    private fun moveSelectedContactsToSet() {
        TODO("APR: use JVM equivalent - move selected avatars to chosen contact set")
    }

    private fun refreshContactSets() {
        TODO("APR: use JVM equivalent - repopulate contact set combo box")
    }

    private fun generateContactList(contactSet: String) {
        TODO("APR: use JVM equivalent - populate contactSetList from named contact set")
    }

    private fun generateCurrentContactList() {
        val currentSet = contactSetCombo?.getValue()?.asString() ?: return
        generateContactList(currentSet)
    }

    private fun updateContactSetListSorting() {
        TODO("APR: use JVM equivalent - apply comparator based on current set sort preference")
    }

    private fun shouldSortByOnlineStatusForCurrentSet(): Boolean {
        TODO("APR: use JVM equivalent - check saved setting for current contact set")
    }

    private fun handleAvatarDropToCurrentContactSet(avatarId: UUID, drop: Boolean): Boolean {
        TODO("APR: use JVM equivalent - validate/perform DnD avatar into contact set")
    }

    private fun updateContactSets(type: LGGContactSets.ContactSetUpdate) {
        refreshContactSets()
        generateCurrentContactList()
    }

    private fun onColumnVisibilityChecked(userdata: LLSD) {
        TODO("APR: use JVM equivalent - toggle radar column visibility")
    }

    private fun onEnableColumnVisibilityChecked(userdata: LLSD): Boolean {
        TODO("APR: use JVM equivalent - return whether radar column can be toggled")
    }

    companion object {
        fun onAvatarPicked(ids: List<UUID>, names: List<AvatarName>) {
            TODO("APR: use JVM equivalent - handle result from avatar picker floater")
        }
    }
}
