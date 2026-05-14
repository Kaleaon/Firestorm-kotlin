package com.firestorm.newview

import java.util.UUID

const val UPDATE_MEMBERS_SECONDS_PER_FRAME: Float = 0.005f

open class PanelGroupTab {
    var groupId: UUID = UUID(0, 0)
    var allowEdit: Boolean = true
    var hasModal: Boolean = false

    open fun postBuild(): Boolean = true
    open fun activate() {}
    open fun deactivate() {}
    open fun needsApply(mesg: StringBuilder): Boolean = false
    open fun apply(mesg: StringBuilder): Boolean = true
    open fun cancel() {}
    open fun update(gc: GroupChange) {}
    open fun isVisibleByAgent(): Boolean = true
    open fun setGroupId(id: UUID) { groupId = id }
    open fun setupCtrls(parent: Any?) {}
    open fun onFilterChanged() {}
    fun notifyObservers() {}
    fun getGroupId(): UUID = groupId
}

enum class GroupChange { GC_ALL, GC_PROPERTIES, GC_MEMBER_DATA, GC_ROLE_DATA, GC_ROLE_MEMBER_DATA }
enum class StatusType { STATUS_JOINING, STATUS_LEFT_CHANNEL, STATUS_OTHER }

class PanelGroup : GroupMgrObserver, VoiceClientStatusObserver {

    private var groupId: UUID = UUID(0, 0)
    private var skipRefresh: Boolean = false
    private var isUsingTabContainer: Boolean = false

    private var defaultNeedsApplyMesg: String = ""
    private var wantApplyMesg: String = ""

    private val tabs: MutableList<PanelGroupTab> = mutableListOf()

    private var groupsAccordion: Any? = null
    private var groupNameCtrl: Any? = null
    private var buttonJoin: Any? = null
    private var buttonActivate: Any? = null
    private var buttonApply: Any? = null
    private var buttonCall: Any? = null
    private var buttonChat: Any? = null
    private var buttonRefresh: Any? = null
    private var joinText: Any? = null

    private var refreshTimerExpiry: Long = 0L
    private var refreshTimerRunning: Boolean = false

    init {
        GroupMgr.addObserver(this)
    }

    fun destroy() {
        GroupMgr.removeObserver(this)
        VoiceClient.removeObserver(this)
    }

    fun postBuild(): Boolean {
        defaultNeedsApplyMesg = "Changes need to be applied"
        wantApplyMesg = "Do you want to apply changes?"
        return true
    }

    fun onOpen(key: Map<String, Any>) {
        val groupId = key["group_id"] as? UUID ?: return

        if (!key.containsKey("action")) {
            setGroupId(groupId)
            return
        }

        val action = key["action"] as? String ?: return
        when (action) {
            "refresh" -> if (this.groupId == groupId || groupId == UUID(0, 0)) refreshData()
            "close" -> onBackBtnClick()
            "refresh_notices" -> { /* delegate to notices sub-panel */ }
            "show_notices" -> setGroupId(groupId)
        }
    }

    fun setGroupId(newGroupId: UUID) {
        val isSameId = newGroupId == groupId

        GroupMgr.removeObserver(this)
        groupId = newGroupId
        GroupMgr.addObserver(this)

        for (tab in tabs) tab.setGroupId(newGroupId)

        buttonActivate?.let { System.err.println("PanelGroup: setGroupId buttonActivate visibility not yet implemented") }
        buttonJoin?.let { System.err.println("PanelGroup: setGroupId buttonJoin visibility not yet implemented") }

        if (!isSameId) {
            System.err.println("PanelGroup: setGroupId accordion reset not yet implemented")
        }

        reposButtons()
        update(GroupChange.GC_ALL)
    }

    override fun changed(gc: GroupChange) {
        for (tab in tabs) tab.update(gc)
        update(gc)
    }

    override fun onChange(status: StatusType, channelInfo: Map<String, Any>, proximal: Boolean) {
        if (status == StatusType.STATUS_JOINING || status == StatusType.STATUS_LEFT_CHANNEL) return
        System.err.println("PanelGroup: onChange call button state not yet implemented")
    }

    fun notifyObservers() {
        changed(GroupChange.GC_ALL)
    }

    open fun update(gc: GroupChange) {
        val gdata = GroupMgr.getGroupData(groupId) ?: return
        System.err.println("PanelGroup: update not yet implemented")
    }

    fun apply(): Boolean {
        return applyTab("group_general_tab_panel")
            && applyTab("group_roles_tab_panel")
            && applyTab("group_notices_tab_panel")
            && applyTab("group_land_tab_panel")
            && applyTab("group_experiences_tab_panel")
    }

    private fun applyTab(panelName: String): Boolean {
        val tab = tabs.find { it.javaClass.simpleName == panelName } ?: return true
        return applyTab(tab)
    }

    private fun applyTab(tab: PanelGroupTab): Boolean {
        val mesg = StringBuilder()
        if (!tab.needsApply(mesg)) return true

        val applyMesg = StringBuilder()
        return if (tab.apply(applyMesg)) {
            skipRefresh = true
            true
        } else {
            if (applyMesg.isNotEmpty()) {
                System.err.println("PanelGroup: applyTab notification not yet implemented")
            }
            false
        }
    }

    fun draw() {
        if (refreshTimerRunning && System.currentTimeMillis() >= refreshTimerExpiry) {
            refreshTimerRunning = false
            System.err.println("PanelGroup: draw timer re-enable not yet implemented")
        }

        val mesg = StringBuilder()
        val enable = tabs.any { it.needsApply(mesg) }
        System.err.println("PanelGroup: draw apply button state not yet implemented")
    }

    fun refreshData() {
        if (skipRefresh) {
            skipRefresh = false
            return
        }
        GroupMgr.clearGroupData(groupId)
        setGroupId(groupId)

        System.err.println("PanelGroup: refreshData disable not yet implemented")
        refreshTimerExpiry = System.currentTimeMillis() + 5_000L
        refreshTimerRunning = true
    }

    fun callGroup() {
        GroupActions.startCall(groupId)
    }

    fun chatGroup() {
        GroupActions.startIM(groupId)
    }

    fun showNotice(
        subject: String,
        message: String,
        hasInventory: Boolean,
        inventoryName: String,
        inventoryOffer: Any?
    ) {
        System.err.println("PanelGroup: showNotice not yet implemented")
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        System.err.println("PanelGroup: reshape not yet implemented")
        reposButtons()
    }

    fun hideBackBtn() {
        System.err.println("PanelGroup: hideBackBtn not yet implemented")
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        return false
    }

    fun hasAccelerators(): Boolean = true

    private fun onBackBtnClick() {
        System.err.println("PanelGroup: onBackBtnClick not yet implemented")
    }

    private fun onBtnJoin() {
        if (GroupActions.isInGroup(groupId)) {
            GroupActions.leave(groupId)
            System.err.println("PanelGroup: onBtnJoin hide activate button not yet implemented")
        } else {
            GroupActions.join(groupId)
        }
    }

    private fun onBtnActivate() {
        GroupActions.activate(groupId)
        System.err.println("PanelGroup: onBtnActivate disable button not yet implemented")
    }

    private fun reposButton(buttonName: String) {
        System.err.println("PanelGroup: reposButton not yet implemented")
    }

    private fun reposButtons() {
        reposButton("btn_apply")
        reposButton("btn_refresh")
        reposButton("btn_chat")
        reposButton("btn_call")
        reposButton("btn_activate")
    }

    companion object {
        fun showNotice(
            subject: String,
            message: String,
            groupId: UUID,
            hasInventory: Boolean,
            inventoryName: String,
            inventoryOffer: Any?
        ) {
            System.err.println("PanelGroup: companion showNotice not yet implemented")
        }
    }
}

interface GroupMgrObserver {
    fun changed(gc: GroupChange)
}

interface VoiceClientStatusObserver {
    fun onChange(status: StatusType, channelInfo: Map<String, Any>, proximal: Boolean)
}

object GroupMgr {
    fun addObserver(obs: GroupMgrObserver) {}
    fun removeObserver(obs: GroupMgrObserver) {}
    fun getGroupData(groupId: UUID): Map<String, Any>? = null
    fun clearGroupData(groupId: UUID) {}
}

object VoiceClient {
    fun addObserver(obs: VoiceClientStatusObserver) {}
    fun removeObserver(obs: VoiceClientStatusObserver) {}
    fun voiceEnabled(): Boolean = false
    fun isVoiceWorking(): Boolean = false
}

object GroupActions {
    fun isInGroup(groupId: UUID): Boolean = false
    fun leave(groupId: UUID) {}
    fun join(groupId: UUID) {}
    fun activate(groupId: UUID) {}
    fun startCall(groupId: UUID) {}
    fun startIM(groupId: UUID) {}
}
