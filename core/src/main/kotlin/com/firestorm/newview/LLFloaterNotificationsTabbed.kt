package com.firestorm.newview

import java.util.ArrayDeque
import java.util.UUID

class LLNotificationSeparator {
    private val notificationListMap: MutableMap<String, LLNotificationListView> = mutableMapOf()
    private val notificationLists: MutableList<LLNotificationListView> = mutableListOf()
    private var unTaggedList: LLNotificationListView? = null

    fun initTaggedList(tag: String, list: LLNotificationListView) {
        notificationListMap[tag] = list
        notificationLists.add(list)
    }

    fun initTaggedList(tags: Set<String>, list: LLNotificationListView) {
        for (tag in tags) {
            initTaggedList(tag, list)
        }
    }

    fun initUnTaggedList(list: LLNotificationListView) {
        unTaggedList = list
    }

    fun addItem(tag: String, item: LLNotificationListItem): Boolean {
        val list = notificationListMap[tag]
        if (list != null) {
            return list.addNotification(item)
        } else if (unTaggedList != null) {
            return unTaggedList!!.addNotification(item)
        }
        return false
    }

    fun removeItemByID(tag: String, id: UUID): Boolean {
        val list = notificationListMap[tag]
        if (list != null) {
            return list.removeItemByValue(id)
        } else if (unTaggedList != null) {
            return unTaggedList!!.removeItemByValue(id)
        }
        return false
    }

    fun findItemByID(tag: String, id: UUID): LLPanel? {
        val list = notificationListMap[tag]
        if (list != null) {
            return list.getItemByValue(id)
        } else if (unTaggedList != null) {
            return unTaggedList!!.getItemByValue(id)
        }
        return null
    }

    fun getItems(items: MutableList<LLNotificationListItem>) {
        items.clear()
        for (list in notificationLists) {
            getItemsFromList(items, list)
        }
        unTaggedList?.let { getItemsFromList(items, it) }
    }

    fun size(): UInt {
        var total = 0u
        for (list in notificationLists) {
            total += list.size()
        }
        unTaggedList?.let { total += it.size() }
        return total
    }

    companion object {
        private fun getItemsFromList(items: MutableList<LLNotificationListItem>, list: LLNotificationListView) {
            val listItems = mutableListOf<LLPanel>()
            list.getItems(listItems)
            for (item in listItems) {
                if (item is LLNotificationListItem) {
                    items.add(item)
                }
            }
        }
    }
}

class LLFloaterNotificationsTabbed(key: LLSD) : LLTransientDockableFloater(null, true, key) {

    companion object {
        const val MAX_WINDOW_HEIGHT: Int = 200
        const val MIN_WINDOW_WIDTH: Int = 318

        fun getInstance(key: LLSD = LLSD()): LLFloaterNotificationsTabbed? {
            return LLFloaterReg.getTypedInstance("notification_well_window", key)
        }
    }

    private val notificationTabbedAnchorName: String = "notification_well_panel"
    private val imWellAnchorName: String = "im_well_panel"

    private var channel: LLNotificationsUI.LLScreenChannel? = null
    private var sysWellChiclet: LLSysWellChiclet? = null
    private var isReshapedByUser: Boolean = false

    private var groupInviteMessageList: LLNotificationListView? = null
    private var groupNoticeMessageList: LLNotificationListView? = null
    private var transactionMessageList: LLNotificationListView? = null
    private var systemMessageList: LLNotificationListView? = null
    private var notificationsSeparator: LLNotificationSeparator? = null
    private var notificationsTabContainer: LLTabContainer? = null
    private var deleteAllBtn: LLButton? = null
    private var collapseAllBtn: LLButton? = null
    private var loadedToastId: UUID = UUID.randomUUID()

    private var notificationUpdates: LLNotificationChannelPtr? = null

    private val notificationsToGo: ArrayDeque<Pair<UUID, String>> = ArrayDeque()
    private val deleteNotificationsTimer: LLTimer = LLTimer()

    init {
        setOverlapsScreenChannel(true)
        notificationUpdates = LLNotificationChannelPtr(NotificationTabbedChannel(this))
        notificationsSeparator = LLNotificationSeparator()
    }

    fun postBuild(): Boolean {
        groupInviteMessageList = getChild("group_invite_notification_list")
        groupNoticeMessageList = getChild("group_notice_notification_list")
        transactionMessageList = getChild("transaction_notification_list")
        systemMessageList = getChild("system_notification_list")

        notificationsSeparator!!.initTaggedList(LLNotificationListItem.getGroupInviteTypes(), groupInviteMessageList!!)
        notificationsSeparator!!.initTaggedList(LLNotificationListItem.getGroupNoticeTypes(), groupNoticeMessageList!!)
        notificationsSeparator!!.initTaggedList(LLNotificationListItem.getTransactionTypes(), transactionMessageList!!)
        notificationsSeparator!!.initUnTaggedList(systemMessageList!!)

        notificationsTabContainer = getChild("notifications_tab_container")
        notificationsTabContainer!!.selectTab(gSavedPerAccountSettings.getS32("FSLastNotificationsTab"))

        deleteAllBtn = getChild("delete_all_button")
        deleteAllBtn!!.setClickedCallback { onClickDeleteAllBtn() }

        collapseAllBtn = getChild("collapse_all_button")
        collapseAllBtn!!.setClickedCallback { onClickCollapseAllBtn() }

        initChannel()
        val rv = super.postBuild()
        setTitle(getString("title_notification_tabbed_window"))
        return rv
    }

    override fun setMinimized(minimize: Boolean) {
        super.setMinimized(minimize)
    }

    override fun handleReshape(rect: LLRect, byUser: Boolean) {
        isReshapedByUser = isReshapedByUser || byUser
        super.handleReshape(rect, byUser)
    }

    fun onStartUpToastClick(x: Int, y: Int, mask: Int) {
        setVisible(true)
    }

    fun setSysWellChiclet(chiclet: LLSysWellChiclet?) {
        sysWellChiclet = chiclet
        sysWellChiclet?.updateWidget(isWindowEmpty())
    }

    fun destroy() {
        gSavedPerAccountSettings.setS32("FSLastNotificationsTab", notificationsTabContainer!!.getCurrentPanelIndex())
    }

    fun removeItemByID(id: UUID, type: String) {
        if (type == "ScriptDialog" || type == "ScriptDialogGroup") {
            return
        }

        if (notificationsSeparator!!.removeItemByID(type, id)) {
            sysWellChiclet?.updateWidget(isWindowEmpty())
            reshapeWindow()
            updateNotificationCounters()
        }

        if (isWindowEmpty()) {
            setVisible(false)
        }
    }

    fun findItemByID(id: UUID, type: String): LLPanel? {
        return notificationsSeparator!!.findItemByID(type, id)
    }

    private fun initChannel() {
        val channelBase = LLNotificationsUI.LLChannelManager.getInstance()
            .findChannelByID(LLNotificationsUI.NOTIFICATION_CHANNEL_UUID)
        channel = channelBase as? LLNotificationsUI.LLScreenChannel
        if (channel != null) {
            channel!!.addOnStoreToastCallback { infoPanel, id -> onStoreToast(infoPanel, id) }
        }
    }

    override fun setVisible(visible: Boolean) {
        var shouldShow = visible
        if (shouldShow) {
            clearScreenChannels()
        }
        if (shouldShow) {
            if (getDockControl() == null && getDockTongue() != null) {
                if (gSavedSettings.getBOOL("InternalShowGroupNoticesTopRight")) {
                    setDockControl(LLDockControl(
                        LLChicletBar.getInstance().getChild<LLView>(anchorViewName), this,
                        getDockTongue()!!, LLDockControl.BOTTOM))
                } else {
                    setDockControl(LLDockControl(
                        LLChicletBar.getInstance().getChild<LLView>(anchorViewName), this,
                        getDockTongue()!!, LLDockControl.TOP))
                }
            }
        }

        if (notificationsSeparator == null || isWindowEmpty()) shouldShow = false

        super.setVisible(shouldShow)

        initChannel()
        channel?.updateShowToastsState()
        channel?.redrawToasts()
    }

    override fun setDocked(docked: Boolean, popOnUndock: Boolean) {
        super.setDocked(docked, popOnUndock)
        channel?.updateShowToastsState()
        channel?.redrawToasts()
    }

    private fun reshapeWindow() {
        if (channel != null && getVisible() && isDocked()) {
            channel!!.updateShowToastsState()
        }
    }

    fun isWindowEmpty(): Boolean {
        return notificationsSeparator!!.size() == 0u
    }

    fun updateNotificationCounter(panelIndex: Int, counterValue: Int, stringName: String) {
        val label = getString(stringName).replace("[COUNT]", counterValue.toString())
        notificationsTabContainer!!.setPanelTitle(panelIndex, label)
    }

    fun updateNotificationCounters() {
        updateNotificationCounter(0, systemMessageList!!.size().toInt(), "system_tab_title")
        updateNotificationCounter(1, transactionMessageList!!.size().toInt(), "transactions_tab_title")
        updateNotificationCounter(2, groupInviteMessageList!!.size().toInt(), "group_invitations_tab_title")
        updateNotificationCounter(3, groupNoticeMessageList!!.size().toInt(), "group_notices_tab_title")
    }

    private fun addItem(p: LLNotificationListItem.Params) {
        if (notificationsSeparator!!.findItemByID(p.notificationName, p.notificationId) != null) return

        val newItem = LLNotificationListItem.create(p) ?: return

        if (notificationsSeparator!!.addItem(newItem.getNotificationName(), newItem)) {
            sysWellChiclet?.updateWidget(isWindowEmpty())
            reshapeWindow()
            updateNotificationCounters()
            newItem.setOnItemCloseCallback { item -> onItemClose(item) }
            newItem.setOnItemClickCallback { item -> onItemClick(item) }
        }
    }

    fun closeAll() {
        clearScreenChannels()
        val items = mutableListOf<LLNotificationListItem>()
        notificationsSeparator!!.getItems(items)
        for (item in items) {
            onItemClose(item)
        }
    }

    private fun getAllItemsOnCurrentTab(items: MutableList<LLPanel>) {
        when (notificationsTabContainer!!.getCurrentPanelIndex()) {
            0 -> systemMessageList!!.getItems(items)
            1 -> transactionMessageList!!.getItems(items)
            2 -> groupInviteMessageList!!.getItems(items)
            3 -> groupNoticeMessageList!!.getItems(items)
            else -> {}
        }
    }

    private fun closeAllOnCurrentTab() {
        deleteNotificationsTimer.reset()
        clearScreenChannels()
        val items = mutableListOf<LLPanel>()
        getAllItemsOnCurrentTab(items)
        for (item in items) {
            val notifyItem = item as? LLNotificationListItem ?: continue
            val id = notifyItem.getID()
            if (id != null) {
                notificationsToGo.addLast(Pair(id, notifyItem.getNotificationName()))
            }
        }
    }

    fun idle() {
        if (notificationsToGo.isNotEmpty()) {
            val pair = notificationsToGo.pollFirst()!!
            val item = findItemByID(pair.first, pair.second) as? LLNotificationListItem
            if (item != null) {
                onItemClose(item)
            }
        }
    }

    private fun collapseAllOnCurrentTab() {
        val items = mutableListOf<LLPanel>()
        getAllItemsOnCurrentTab(items)
        for (item in items) {
            (item as? LLNotificationListItem)?.setExpanded(false)
        }
    }

    private fun clearScreenChannels() {
        if (!LLNotificationsUI.LLScreenChannel.getStartUpToastShown()) {
            LLNotificationsUI.LLChannelManager.getInstance().onStartUpToastClose()
        }
        channel?.removeAndStoreAllStorableToasts()
    }

    private fun onStoreToast(infoPanel: LLPanel, id: UUID) {
        val p = LLNotificationListItem.Params()
        p.notificationId = id
        p.title = (infoPanel as LLToastPanel).getTitle()
        val notify = channel!!.getToastByNotificationID(id)!!.getNotification()
        val payload = notify.getPayload()
        p.notificationName = notify.getName()
        p.transactionId = payload["transaction_id"]
        p.groupId = payload["group_id"]
        p.fee = payload["fee"]
        p.useOfflineCap = payload["use_offline_cap"].asInteger()
        p.subject = payload["subject"].asString()
        p.message = payload["message"].asString()
        p.paymentMessage = payload["payment_message"].asString()
        p.paymentIsGroup = payload["payment_is_group"].asBoolean()
        p.sender = payload["sender_name"].asString()
        p.timeStamp = notify.getDate()
        p.receivedTime = payload["received_time"].asDate()
        p.paidFromId = payload["from_id"]
        p.paidToId = payload["dest_id"]
        p.inventoryOffer = payload["inventory_offer"]
        p.notificationPriority = notify.getPriority()
        addItem(p)
    }

    private fun onItemClick(item: LLNotificationListItem) {
        val id = item.getID()
        if (item.showPopup()) {
            LLFloaterReg.showInstance("inspect_toast", id)
        } else {
            item.setExpanded(true)
        }
    }

    private fun onItemClose(item: LLNotificationListItem) {
        val id = item.getID()
        if (channel != null) {
            channel!!.killToastByNotificationID(id)
        } else {
            removeItemByID(id, item.getNotificationName())
        }
    }

    fun onAdd(notify: LLNotificationPtr) {
        removeItemByID(notify.getID(), notify.getName())
    }

    private fun onClickDeleteAllBtn() {
        closeAllOnCurrentTab()
    }

    private fun onClickCollapseAllBtn() {
        collapseAllOnCurrentTab()
    }

    private val anchorViewName: String
        get() = notificationTabbedAnchorName

    private inner class NotificationTabbedChannel(
        private val notificationsTabbedWindow: LLFloaterNotificationsTabbed
    ) : LLNotificationChannel(LLNotificationChannel.Params().name(notificationsTabbedWindow.getPathname())) {
        init {
            connectToChannel("Notifications")
            connectToChannel("Group Notifications")
            connectToChannel("Offer")
        }

        fun onDelete(notify: LLNotificationPtr) {
            notificationsTabbedWindow.removeItemByID(notify.getID(), notify.getName())
        }
    }
}
