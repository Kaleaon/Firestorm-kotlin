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
}

// ---------------------------------------------------------------------------
// Concrete tab handlers (mirror the LLGroupMoney*TabEventHandler subclasses)
// ---------------------------------------------------------------------------

class GroupMoneyDetailsTabHandler : GroupMoneyTabEventHandler(
    GroupMoneyTabHandlerImpl(intervalLength = 7, maxInterval = 8)
) {
    override fun requestData() {
        System.err.println("GroupMoneyDetailsTabHandler: requestData not yet implemented")
    }

    override fun processReply() {
        System.err.println("GroupMoneyDetailsTabHandler: processReply not yet implemented")
    }
}

class GroupMoneySalesTabHandler : GroupMoneyTabEventHandler(
    GroupMoneyTabHandlerImpl(intervalLength = 7, maxInterval = 8)
) {
    override fun requestData() {
        System.err.println("GroupMoneySalesTabHandler: requestData not yet implemented")
    }

    override fun processReply() {
        System.err.println("GroupMoneySalesTabHandler: processReply not yet implemented")
    }
}

class GroupMoneyPlanningTabHandler : GroupMoneyTabEventHandler(
    GroupMoneyTabHandlerImpl(intervalLength = 7, maxInterval = 8, currentInterval = 0)
) {
    override fun requestData() {
        // Planning always uses interval 0
        System.err.println("GroupMoneyPlanningTabHandler: requestData not yet implemented")
    }

    override fun processReply() {
        System.err.println("GroupMoneyPlanningTabHandler: processReply not yet implemented")
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
            System.err.println("PanelGroupLandMoney: processGroupAccountDetailsReply not yet implemented")
        }

        fun processGroupAccountTransactionsReply(agentId: UUID, requestId: UUID) {
            System.err.println("PanelGroupLandMoney: processGroupAccountTransactionsReply not yet implemented")
        }

        fun processGroupAccountSummaryReply(agentId: UUID, requestId: UUID) {
            System.err.println("PanelGroupLandMoney: processGroupAccountSummaryReply not yet implemented")
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

        var cantViewParcelsText: String = ""
        var cantViewAccountsText: String = ""
        var emptyParcelsText: String = ""

        var moneyDetailsTabHandler: GroupMoneyDetailsTabHandler? = null
        var moneySalesTabHandler: GroupMoneySalesTabHandler? = null
        var moneyPlanningTabHandler: GroupMoneyPlanningTabHandler? = null

        fun getStoredContribution(): Int {
            return 0
        }

        fun requestGroupLandInfo() {
            transId = UUID.randomUUID()
            System.err.println("Impl: requestGroupLandInfo not yet implemented")
        }

        fun onMapButton() {
            System.err.println("Impl: onMapButton not yet implemented")
        }

        fun applyContribution(newContribution: Int): Boolean {
            return false
        }

        fun processGroupLand(blocks: List<PlacesQueryBlock>) {
            System.err.println("Impl: processGroupLand not yet implemented")
        }
    }

    val impl = Impl()

    override fun postBuild(): Boolean {
        System.err.println("PanelGroupLandMoney: postBuild not yet implemented")
        return false
    }

    override fun activate() {
        if (!impl.beenActivated) {
            impl.beenActivated = true
            System.err.println("PanelGroupLandMoney: activate not yet implemented")
        }
        update(GroupChange.GC_ALL)
    }

    override fun needsApply(mesg: StringBuilder): Boolean = impl.needsApply

    override fun apply(mesg: StringBuilder): Boolean {
        return false
    }

    override fun cancel() {
        impl.needsApply = false
        System.err.println("PanelGroupLandMoney: cancel not yet implemented")
    }

    override fun update(gc: GroupChange) {
        if (gc != GroupChange.GC_ALL) return
        System.err.println("PanelGroupLandMoney: update not yet implemented")
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
        System.err.println("PanelGroupLandMoney: onLandSelectionChanged not yet implemented")
    }

    override fun isVisibleByAgent(): Boolean {
        return false
    }
}
