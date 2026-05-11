package com.firestorm.newview

import java.util.ArrayDeque
import java.util.UUID

class NotificationSeparator {

    private val notificationListMap: MutableMap<String, NotificationListView> = mutableMapOf()
    private val notificationLists: MutableList<NotificationListView> = mutableListOf()
    private var unTaggedList: NotificationListView? = null

    fun initTaggedList(tag: String, list: NotificationListView) {
        notificationListMap[tag] = list
        notificationLists.add(list)
    }

    fun initTaggedList(tags: Set<String>, list: NotificationListView) {
        tags.forEach { initTaggedList(it, list) }
    }

    fun initUnTaggedList(list: NotificationListView) {
        unTaggedList = list
    }

    fun addItem(tag: String, item: NotificationListItem): Boolean {
        val list = notificationListMap[tag]
        return if (list != null) {
            list.addNotification(item)
        } else {
            unTaggedList?.addNotification(item) ?: false
        }
    }

    fun findItemByID(tag: String, id: UUID): Panel? {
        val list = notificationListMap[tag]
        return if (list != null) {
            list.getItemByValue(id)
        } else {
            unTaggedList?.getItemByValue(id)
        }
    }

    fun removeItemByID(tag: String, id: UUID): Boolean {
        val list = notificationListMap[tag]
        return if (list != null) {
            list.removeItemByValue(id)
        } else {
            unTaggedList?.removeItemByValue(id) ?: false
        }
    }

    fun getItems(items: MutableList<NotificationListItem>) {
        items.clear()
        for (list in notificationLists) {
            getItemsFromList(items, list)
        }
        unTaggedList?.let { getItemsFromList(items, it) }
    }

    fun size(): UInt {
        var count = 0u
        for (list in notificationLists) count += list.size()
        count += unTaggedList?.size() ?: 0u
        return count
    }

    companion object {
        private fun getItemsFromList(
            items: MutableList<NotificationListItem>,
            list: NotificationListView
        ) {
            val panelItems = mutableListOf<Panel>()
            list.getItems(panelItems)
            panelItems.filterIsInstance<NotificationListItem>().forEach { items.add(it) }
        }
    }
}

class FloaterNotificationsTabbed(key: Any?) : TransientDockableFloater(key) {

    companion object {
        const val MAX_WINDOW_HEIGHT = 200
        const val MIN_WINDOW_WIDTH  = 318

        fun getInstance(key: Any? = null): FloaterNotificationsTabbed? =
            FloaterReg.getTypedInstance("notification_well_window", key)
    }

    private val notificationTabbedAnchorName = "notification_well_panel"
    private val imWellAnchorName             = "im_well_panel"

    private var channel: ScreenChannel? = null
    private var sysWellChiclet: SysWellChiclet? = null
    private var isReshapedByUser: Boolean = false

    private var groupInviteMessageList:   NotificationListView? = null
    private var groupNoticeMessageList:   NotificationListView? = null
    private var transactionMessageList:   NotificationListView? = null
    private var systemMessageList:        NotificationListView? = null
    private var notificationsSeparator:   NotificationSeparator? = null
    private var notificationsTabContainer: TabContainer? = null
    private var deleteAllBtn:   Button? = null
    private var collapseAllBtn: Button? = null

    private var loadedToastId: UUID = UUID(0, 0)

    private val notificationsToGo: ArrayDeque<Pair<UUID, String>> = ArrayDeque()
    private var deleteNotificationsTimerStart: Long = 0L

    private val notificationUpdates: NotificationChannel = NotificationTabbedChannel(this)

    init {
        setOverlapsScreenChannel(true)
        notificationsSeparator = NotificationSeparator()
    }

    override fun postBuild(): Boolean {
        groupInviteMessageList   = getChild("group_invite_notification_list")
        groupNoticeMessageList   = getChild("group_notice_notification_list")
        transactionMessageList   = getChild("transaction_notification_list")
        systemMessageList        = getChild("system_notification_list")

        val sep = notificationsSeparator!!
        sep.initTaggedList(NotificationListItem.getGroupInviteTypes(), groupInviteMessageList!!)
        sep.initTaggedList(NotificationListItem.getGroupNoticeTypes(), groupNoticeMessageList!!)
        sep.initTaggedList(NotificationListItem.getTransactionTypes(), transactionMessageList!!)
        sep.initUnTaggedList(systemMessageList!!)

        notificationsTabContainer = getChild("notifications_tab_container")
        notificationsTabContainer!!.selectTab(SavedPerAccountSettings.getS32("FSLastNotificationsTab"))

        deleteAllBtn   = getChild("delete_all_button")
        deleteAllBtn!!.setClickedCallback { onClickDeleteAllBtn() }

        collapseAllBtn = getChild("collapse_all_button")
        collapseAllBtn!!.setClickedCallback { onClickCollapseAllBtn() }

        initChannel()
        val rv = super.postBuild()
        setTitle(getString("title_notification_tabbed_window"))
        return rv
    }

    fun setMinimized(minimize: Boolean) {
        super.setMinimized(minimize)
    }

    fun handleReshape(rect: Rect, byUser: Boolean) {
        isReshapedByUser = isReshapedByUser || byUser
        super.handleReshape(rect, byUser)
    }

    fun onStartUpToastClick(x: Int, y: Int, mask: Int) {
        setVisible(true)
    }

    fun setSysWellChiclet(chiclet: SysWellChiclet?) {
        sysWellChiclet = chiclet
        sysWellChiclet?.updateWidget(isWindowEmpty())
    }

    override fun onClose(appQuitting: Boolean) {
        SavedPerAccountSettings.setS32(
            "FSLastNotificationsTab",
            notificationsTabContainer?.getCurrentPanelIndex() ?: 0
        )
    }

    fun removeItemByID(id: UUID, type: String) {
        if (type == "ScriptDialog" || type == "ScriptDialogGroup") return

        if (notificationsSeparator?.removeItemByID(type, id) == true) {
            sysWellChiclet?.updateWidget(isWindowEmpty())
            reshapeWindow()
            updateNotificationCounters()
        }

        if (isWindowEmpty()) setVisible(false)
    }

    fun findItemByID(id: UUID, type: String): Panel? =
        notificationsSeparator?.findItemByID(type, id)

    fun updateNotificationCounters() {
        updateNotificationCounter(0, systemMessageList?.size()?.toInt()       ?: 0, "system_tab_title")
        updateNotificationCounter(1, transactionMessageList?.size()?.toInt()  ?: 0, "transactions_tab_title")
        updateNotificationCounter(2, groupInviteMessageList?.size()?.toInt()  ?: 0, "group_invitations_tab_title")
        updateNotificationCounter(3, groupNoticeMessageList?.size()?.toInt()  ?: 0, "group_notices_tab_title")
    }

    fun updateNotificationCounter(panelIndex: Int, counterValue: Int, stringName: String) {
        val label = getString(stringName).replace("[COUNT]", counterValue.toString())
        notificationsTabContainer?.setPanelTitle(panelIndex, label)
    }

    override fun setVisible(visible: Boolean) {
        var shouldShow = visible
        if (shouldShow) {
            clearScreenChannels()
            if (getDockControl() == null && getDockTongue() != null) {
                val anchorView = ChicletBar.instance.getChild<View>(notificationTabbedAnchorName)
                val dockSide =
                    if (SavedSettings.getBool("InternalShowGroupNoticesTopRight"))
                        DockControl.BOTTOM
                    else
                        DockControl.TOP
                setDockControl(DockControl(anchorView, this, getDockTongue()!!, dockSide))
            }
        }

        if (notificationsSeparator == null || isWindowEmpty()) shouldShow = false
        super.setVisible(shouldShow)

        initChannel()
        channel?.updateShowToastsState()
        channel?.redrawToasts()
    }

    fun setDocked(docked: Boolean, popOnUndock: Boolean = true) {
        super.setDocked(docked, popOnUndock)
        channel?.updateShowToastsState()
        channel?.redrawToasts()
    }

    fun closeAll() {
        clearScreenChannels()
        val items = mutableListOf<NotificationListItem>()
        notificationsSeparator?.getItems(items)
        items.forEach { onItemClose(it) }
    }

    fun idle() {
        if (notificationsToGo.isNotEmpty()) {
            val (id, type) = notificationsToGo.poll()
            val item = findItemByID(id, type) as? NotificationListItem
            if (item != null) {
                onItemClose(item)
            }
        }
    }

    fun onAdd(notify: Notification) {
        removeItemByID(notify.id, notify.name)
    }

    private fun getAnchorViewName(): String = notificationTabbedAnchorName

    private fun initChannel() {
        val ch = ChannelManager.instance.findChannelByID(NOTIFICATION_CHANNEL_UUID)
        channel = ch as? ScreenChannel
        if (channel == null) return
        channel!!.addOnStoreToastCallback { infoPanel, id -> onStoreToast(infoPanel, id) }
    }

    private fun reshapeWindow() {
        if (channel != null && getVisible() && isDocked()) {
            channel!!.updateShowToastsState()
        }
    }

    fun isWindowEmpty(): Boolean = (notificationsSeparator?.size() ?: 0u) == 0u

    private fun addItem(p: NotificationListItem.Params) {
        if (notificationsSeparator?.findItemByID(p.notificationName, p.notificationId) != null) return

        val newItem = NotificationListItem.create(p) ?: return

        if (notificationsSeparator?.addItem(newItem.notificationName, newItem) == true) {
            sysWellChiclet?.updateWidget(isWindowEmpty())
            reshapeWindow()
            updateNotificationCounters()
            newItem.setOnItemCloseCallback { onItemClose(it) }
            newItem.setOnItemClickCallback { onItemClick(it) }
        }
    }

    private fun getAllItemsOnCurrentTab(items: MutableList<Panel>) {
        when (notificationsTabContainer?.getCurrentPanelIndex()) {
            0 -> systemMessageList?.getItems(items)
            1 -> transactionMessageList?.getItems(items)
            2 -> groupInviteMessageList?.getItems(items)
            3 -> groupNoticeMessageList?.getItems(items)
        }
    }

    private fun closeAllOnCurrentTab() {
        deleteNotificationsTimerStart = System.currentTimeMillis()
        clearScreenChannels()

        val items = mutableListOf<Panel>()
        getAllItemsOnCurrentTab(items)
        for (panel in items) {
            val item = panel as? NotificationListItem ?: continue
            val id = item.id
            if (id != UUID(0, 0)) {
                notificationsToGo.offer(id to item.notificationName)
            }
        }
    }

    private fun collapseAllOnCurrentTab() {
        val items = mutableListOf<Panel>()
        getAllItemsOnCurrentTab(items)
        items.filterIsInstance<NotificationListItem>().forEach { it.setExpanded(false) }
    }

    private fun clearScreenChannels() {
        if (!ScreenChannel.getStartUpToastShown()) {
            ChannelManager.instance.onStartUpToastClose()
        }
        channel?.removeAndStoreAllStorableToasts()
    }

    private fun onStoreToast(infoPanel: Panel, id: UUID) {
        val p = NotificationListItem.Params()
        p.notificationId   = id
        p.title            = (infoPanel as? ToastPanel)?.title ?: ""
        val notify         = channel?.getToastByNotificationID(id)?.notification ?: return
        val payload        = notify.payload
        p.notificationName = notify.name
        p.transactionId    = payload["transaction_id"]
        p.groupId          = payload["group_id"]
        p.fee              = payload["fee"]
        p.useOfflineCap    = payload["use_offline_cap"].asInt()
        p.subject          = payload["subject"].asString()
        p.message          = payload["message"].asString()
        p.paymentMessage   = payload["payment_message"].asString()
        p.paymentIsGroup   = payload["payment_is_group"].asBoolean()
        p.sender           = payload["sender_name"].asString()
        p.timeStamp        = notify.date
        p.receivedTime     = payload["received_time"].asDate()
        p.paidFromId       = payload["from_id"].asUUID()
        p.paidToId         = payload["dest_id"].asUUID()
        p.inventoryOffer   = payload["inventory_offer"]
        p.notificationPriority = notify.priority
        addItem(p)
    }

    private fun onItemClick(item: NotificationListItem) {
        val id = item.id
        if (item.showPopup()) {
            FloaterReg.showInstance("inspect_toast", id)
        } else {
            item.setExpanded(true)
        }
    }

    private fun onItemClose(item: NotificationListItem) {
        val id = item.id
        if (channel != null) {
            channel!!.killToastByNotificationID(id)
        } else {
            removeItemByID(id, item.notificationName)
        }
    }

    private fun onClickDeleteAllBtn()   = closeAllOnCurrentTab()
    private fun onClickCollapseAllBtn() = collapseAllOnCurrentTab()

    private inner class NotificationTabbedChannel(
        private val tabbedWindow: FloaterNotificationsTabbed
    ) : NotificationChannel(tabbedWindow.pathname) {
        init {
            connectToChannel("Notifications")
            connectToChannel("Group Notifications")
            connectToChannel("Offer")
        }

        fun onDelete(notify: Notification) {
            tabbedWindow.removeItemByID(notify.id, notify.name)
        }
    }
}
