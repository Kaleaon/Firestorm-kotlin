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
        System.err.println("AvatarItemDistanceComparator: doCompare not yet implemented")
        return false
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
        System.err.println("PanelPeople: onOpen not yet implemented")
    }

    override fun notifyChildren(info: LLSD): Boolean {
        System.err.println("PanelPeople: notifyChildren not yet implemented")
        return false
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
        System.err.println("PanelPeople: updateNearbyList not yet implemented")
    }

    fun updateNearbyArrivalTime(): Boolean {
        System.err.println("PanelPeople: updateNearbyArrivalTime not yet implemented")
        return false
    }

    private fun removePicker() {
        picker.get()?.closeFloater()
    }

    private fun updateFriendListHelpText() {
        System.err.println("PanelPeople: updateFriendListHelpText not yet implemented")
    }

    private fun updateFriendList() {
        System.err.println("PanelPeople: updateFriendList not yet implemented")
    }

    private fun updateRecentList() {
        System.err.println("PanelPeople: updateRecentList not yet implemented")
    }

    private fun isItemsFreeOfFriends(uuids: List<UUID>): Boolean {
        System.err.println("PanelPeople: isItemsFreeOfFriends not yet implemented")
        return false
    }

    private fun updateButtons() {
        System.err.println("PanelPeople: updateButtons not yet implemented")
    }

    private fun getActiveTabName(): String =
        tabContainer?.currentPanel?.name ?: ""

    private fun getCurrentItemID(): UUID {
        System.err.println("PanelPeople: getCurrentItemID not yet implemented")
        return UUID.ZERO
    }

    private fun getCurrentItemIDs(selectedUuids: MutableList<UUID>) {
        System.err.println("PanelPeople: getCurrentItemIDs not yet implemented")
    }

    private fun setSortOrder(list: AvatarList?, order: SortOrder, save: Boolean = true) {
        System.err.println("PanelPeople: setSortOrder not yet implemented")
    }

    private fun onFilterEdit(searchString: String) {
        System.err.println("PanelPeople: onFilterEdit not yet implemented")
    }

    private fun onGroupLimitInfo() {
        System.err.println("PanelPeople: onGroupLimitInfo not yet implemented")
    }

    private fun onTabSelected(param: LLSD) {
        System.err.println("PanelPeople: onTabSelected not yet implemented")
    }

    private fun onAddFriendButtonClicked() {
        System.err.println("PanelPeople: onAddFriendButtonClicked not yet implemented")
    }

    private fun onAddFriendWizButtonClicked() {
        System.err.println("PanelPeople: onAddFriendWizButtonClicked not yet implemented")
    }

    private fun onDeleteFriendButtonClicked() {
        System.err.println("PanelPeople: onDeleteFriendButtonClicked not yet implemented")
    }

    private fun onChatButtonClicked() {
        System.err.println("PanelPeople: onChatButtonClicked not yet implemented")
    }

    private fun onGearButtonClicked(btn: UICtrl) {
        System.err.println("PanelPeople: onGearButtonClicked not yet implemented")
    }

    private fun onImButtonClicked() {
        System.err.println("PanelPeople: onImButtonClicked not yet implemented")
    }

    private fun onMoreButtonClicked() {
        System.err.println("PanelPeople: onMoreButtonClicked not yet implemented")
    }

    private fun onAvatarListDoubleClicked(ctrl: UICtrl) {
        System.err.println("PanelPeople: onAvatarListDoubleClicked not yet implemented")
    }

    private fun onAvatarListCommitted(list: AvatarList) {
        updateButtons()
    }

    private fun onGroupPlusButtonValidate(): Boolean {
        System.err.println("PanelPeople: onGroupPlusButtonValidate not yet implemented")
        return false
    }

    private fun onGroupMinusButtonClicked() {
        System.err.println("PanelPeople: onGroupMinusButtonClicked not yet implemented")
    }

    private fun onGroupPlusMenuItemClicked(userdata: LLSD) {
        System.err.println("PanelPeople: onGroupPlusMenuItemClicked not yet implemented")
    }

    private fun onFriendsViewSortMenuItemClicked(userdata: LLSD) {
        System.err.println("PanelPeople: onFriendsViewSortMenuItemClicked not yet implemented")
    }

    private fun onNearbyViewSortMenuItemClicked(userdata: LLSD) {
        System.err.println("PanelPeople: onNearbyViewSortMenuItemClicked not yet implemented")
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
