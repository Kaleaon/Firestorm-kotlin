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
        TODO("APR: send GroupAccountDetailsRequest for group=${impl.groupId} interval=${impl.intervalLength} current=${impl.currentInterval}")
    }

    override fun processReply() {
        TODO("APR: parse GroupAccountDetailsReply; format dates; populate details text editor")
    }
}

class GroupMoneySalesTabHandler : GroupMoneyTabEventHandler(
    GroupMoneyTabHandlerImpl(intervalLength = 7, maxInterval = 8)
) {
    override fun requestData() {
        TODO("APR: send GroupAccountTransactionsRequest for group=${impl.groupId} interval=${impl.intervalLength} current=${impl.currentInterval}")
    }

    override fun processReply() {
        TODO("APR: parse GroupAccountTransactionsReply; format transaction lines; populate sales text editor")
    }
}

class GroupMoneyPlanningTabHandler : GroupMoneyTabEventHandler(
    GroupMoneyTabHandlerImpl(intervalLength = 7, maxInterval = 8, currentInterval = 0)
) {
    override fun requestData() {
        // Planning always uses interval 0
        TODO("APR: send GroupAccountSummaryRequest for group=${impl.groupId} interval=${impl.intervalLength} current=0")
    }

    override fun processReply() {
        TODO("APR: parse GroupAccountSummaryReply; format balance/credit/debit summary; populate planning text editor")
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
            TODO("APR: dispatch GroupAccountDetailsReply to the handler identified by requestId=$requestId")
        }

        fun processGroupAccountTransactionsReply(agentId: UUID, requestId: UUID) {
            TODO("APR: dispatch GroupAccountTransactionsReply to handler for requestId=$requestId")
        }

        fun processGroupAccountSummaryReply(agentId: UUID, requestId: UUID) {
            TODO("APR: dispatch GroupAccountSummaryReply to handler for requestId=$requestId")
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
            TODO("APR: query agent's land contribution for group $groupId from agent data")
        }

        fun requestGroupLandInfo() {
            transId = UUID.randomUUID()
            TODO("APR: send DFQ_GROUP_OWNED places query for group $groupId with transId=$transId")
        }

        fun onMapButton() {
            TODO("APR: read global_x/global_y from selected parcel row; open world map floater at that location")
        }

        fun applyContribution(newContribution: Int): Boolean {
            TODO("APR: validate newContribution vs available sq-m; call agent.setGroupContribution($groupId, $newContribution)")
        }

        fun processGroupLand(blocks: List<PlacesQueryBlock>) {
            TODO("APR: populate group parcel scroll list from ${blocks.size} PlacesQueryBlock entries; update total-land/in-use/available labels")
        }
    }

    val impl = Impl()

    override fun postBuild(): Boolean {
        TODO("APR: obtain UI child widgets (contribution editor, map button, parcel list, money tabs); wire callbacks; create tab event handlers if agent is in group")
    }

    override fun activate() {
        if (!impl.beenActivated) {
            impl.beenActivated = true
            TODO("APR: select first money tab; compute max contribution = stored + statusBar.squareMetersLeft; update max-contribution label")
        }
        update(GroupChange.GC_ALL)
    }

    override fun needsApply(mesg: StringBuilder): Boolean = impl.needsApply

    override fun apply(mesg: StringBuilder): Boolean {
        TODO("APR: read contribution editor text; call impl.applyContribution(newValue)")
    }

    override fun cancel() {
        impl.needsApply = false
        TODO("APR: reset contribution editor text to impl.getStoredContribution()")
    }

    override fun update(gc: GroupChange) {
        if (gc != GroupChange.GC_ALL) return
        TODO("APR: call onClickTab() on the currently visible money tab handler; call impl.requestGroupLandInfo(); refresh contribution field")
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
        TODO("APR: enable map button iff parcel list has at least one item")
    }

    override fun isVisibleByAgent(): Boolean {
        TODO("APR: return allowEdit && agent.isInGroup($groupId)")
    }
}
