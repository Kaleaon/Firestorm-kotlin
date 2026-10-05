package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Money-tab handler implementation (mirrors LLGroupMoneyTabEventHandlerImpl)
// ---------------------------------------------------------------------------

private class GroupMoneyTabHandlerImpl(
    val intervalLength: Int,
    val maxInterval: Int,
    var currentInterval: Int = 0,
    var groupId: UUID = UUID(0, 0),
    val panelId: UUID = UUID.randomUUID(),
    var loadingText: String = ""
) {
    fun canClickEarlier(): Boolean = currentInterval < maxInterval
    fun canClickLater(): Boolean = currentInterval > 0
}

// ---------------------------------------------------------------------------
// GroupMoneyTabEventHandler (mirrors LLGroupMoneyTabEventHandler)
// ---------------------------------------------------------------------------

open class GroupMoneyTabEventHandler(protected val impl: GroupMoneyTabHandlerImpl) {

    companion object {
        val instanceIds: MutableMap<UUID, GroupMoneyTabEventHandler> = mutableMapOf()
        val tabsToHandlers: MutableMap<String, GroupMoneyTabEventHandler> = mutableMapOf()

        private const val SUMMARY_INTERVAL = 7
        private const val SUMMARY_MAX = 8
    }

    fun setGroupId(id: UUID) {
        impl.groupId = id
    }

    open fun onClickTab() {
        requestData()
    }

    open fun requestData() {
        // base no-op; subclasses send the appropriate network message
    }

    open fun processReply() {
        // base no-op
    }

    fun onClickEarlier() {
        impl.currentInterval++
        requestData()
    }

    fun onClickLater() {
        impl.currentInterval--
        requestData()
    }

    fun requestId(): UUID = impl.panelId
}

// ---------------------------------------------------------------------------
// Concrete tab handlers (mirror the LLGroupMoney*TabEventHandler subclasses)
// ---------------------------------------------------------------------------

class GroupMoneyDetailsTabHandler : GroupMoneyTabEventHandler(
    GroupMoneyTabHandlerImpl(intervalLength = 7, maxInterval = 8)
) {
    override fun requestData() {
        impl.loadingText = "Loading group account details…"
    }

    override fun processReply() {
        impl.loadingText = ""
    }
}

class GroupMoneySalesTabHandler : GroupMoneyTabEventHandler(
    GroupMoneyTabHandlerImpl(intervalLength = 7, maxInterval = 8)
) {
    override fun requestData() {
        impl.loadingText = "Loading group transactions…"
    }

    override fun processReply() {
        impl.loadingText = ""
    }
}

class GroupMoneyPlanningTabHandler : GroupMoneyTabEventHandler(
    GroupMoneyTabHandlerImpl(intervalLength = 7, maxInterval = 8, currentInterval = 0)
) {
    override fun requestData() {
        // Planning always uses interval 0
        impl.loadingText = "Loading group account summary…"
    }

    override fun processReply() {
        impl.loadingText = ""
    }
}

// ---------------------------------------------------------------------------
// PanelGroupLandMoney (mirrors LLPanelGroupLandMoney)
// ---------------------------------------------------------------------------

open class PanelGroupLandMoney : PanelGroupTab() {

    companion object {
        val groupPanelMap: MutableMap<UUID, PanelGroupLandMoney> = mutableMapOf()

        fun processPlacesReply(queryId: UUID, blocks: List<PlacesQueryBlock>) {
            groupPanelMap[queryId]?.impl?.processGroupLand(blocks)
        }

        fun processGroupAccountDetailsReply(agentId: UUID, requestId: UUID) {
            GroupMoneyTabEventHandler.instanceIds[requestId]?.processReply()
        }

        fun processGroupAccountTransactionsReply(agentId: UUID, requestId: UUID) {
            GroupMoneyTabEventHandler.instanceIds[requestId]?.processReply()
        }

        fun processGroupAccountSummaryReply(agentId: UUID, requestId: UUID) {
            GroupMoneyTabEventHandler.instanceIds[requestId]?.processReply()
        }
    }

    data class PlacesQueryBlock(
        val name: String,
        val location: String,
        val actualArea: Int,
        val billableArea: Int,
        val globalX: Float,
        val globalY: Float,
        val simName: String,
        val landType: String
    )

    // Inner class mirrors LLPanelGroupLandMoney::impl
    inner class Impl {
        var transId: UUID = UUID(0, 0)
        var beenActivated: Boolean = false
        var needsSendGroupLandRequest: Boolean = true
        var needsApply: Boolean = false
        var storedContribution: Int = 0
        var pendingContribution: Int = 0

        var cantViewParcelsText: String = ""
        var cantViewAccountsText: String = ""
        var emptyParcelsText: String = ""

        var moneyDetailsTabHandler: GroupMoneyDetailsTabHandler? = null
        var moneySalesTabHandler: GroupMoneySalesTabHandler? = null
        var moneyPlanningTabHandler: GroupMoneyPlanningTabHandler? = null

        fun getStoredContribution(): Int {
            return storedContribution
        }

        fun setPendingContribution(newContribution: Int) {
            pendingContribution = newContribution
            needsApply = pendingContribution != storedContribution
        }

        fun requestGroupLandInfo() {
            transId = UUID.randomUUID()
            needsSendGroupLandRequest = false
        }

        fun onMapButton() {
            // UI map integration is not wired in this JVM placeholder.
        }

        fun applyContribution(newContribution: Int): Boolean {
            if (newContribution < 0) return false
            storedContribution = newContribution
            pendingContribution = newContribution
            needsApply = false
            return true
        }

        fun processGroupLand(blocks: List<PlacesQueryBlock>) {
            needsSendGroupLandRequest = false
        }
    }

    val impl = Impl()

    override fun postBuild(): Boolean {
        impl.moneyDetailsTabHandler = GroupMoneyDetailsTabHandler()
        impl.moneySalesTabHandler = GroupMoneySalesTabHandler()
        impl.moneyPlanningTabHandler = GroupMoneyPlanningTabHandler()

        val details = impl.moneyDetailsTabHandler!!
        val sales = impl.moneySalesTabHandler!!
        val planning = impl.moneyPlanningTabHandler!!

        details.setGroupId(groupId)
        sales.setGroupId(groupId)
        planning.setGroupId(groupId)

        GroupMoneyTabEventHandler.instanceIds[details.requestId()] = details
        GroupMoneyTabEventHandler.instanceIds[sales.requestId()] = sales
        GroupMoneyTabEventHandler.instanceIds[planning.requestId()] = planning

        GroupMoneyTabEventHandler.tabsToHandlers["details"] = details
        GroupMoneyTabEventHandler.tabsToHandlers["sales"] = sales
        GroupMoneyTabEventHandler.tabsToHandlers["planning"] = planning

        return true
    }

    override fun activate() {
        if (!impl.beenActivated) {
            impl.beenActivated = true
            impl.pendingContribution = impl.getStoredContribution()
            impl.moneyDetailsTabHandler?.onClickTab()
        }
        update(GroupChange.GC_ALL)
    }

    override fun needsApply(mesg: StringBuilder): Boolean = impl.needsApply

    override fun apply(mesg: StringBuilder): Boolean {
        if (!impl.applyContribution(impl.pendingContribution)) {
            mesg.append("Invalid group land contribution.")
            return false
        }
        return true
    }

    override fun cancel() {
        impl.needsApply = false
        impl.pendingContribution = impl.getStoredContribution()
    }

    override fun update(gc: GroupChange) {
        if (gc != GroupChange.GC_ALL) return
        impl.moneyDetailsTabHandler?.onClickTab()
        if (impl.needsSendGroupLandRequest) {
            impl.requestGroupLandInfo()
        }
    }

    override fun setGroupId(id: UUID) {
        groupPanelMap.remove(groupId)
        super.setGroupId(id)
        groupPanelMap[groupId] = this

        impl.moneyDetailsTabHandler?.setGroupId(id)
        impl.moneySalesTabHandler?.setGroupId(id)
        impl.moneyPlanningTabHandler?.setGroupId(id)

        impl.beenActivated = false
        activate()
    }

    fun onLandSelectionChanged() {
        // No UI controls are bound in this placeholder implementation.
    }

    override fun isVisibleByAgent(): Boolean {
        return allowEdit
    }
}
