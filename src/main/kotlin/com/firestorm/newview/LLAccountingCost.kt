package com.firestorm.newview

import java.util.UUID

data class SelectionCost(
    val physicsCost: Float = 0.0f,
    val networkCost: Float = 0.0f,
    val simulationCost: Float = 0.0f
)

enum class SelectionType {
    Roots,
    Prims
}

abstract class LLAccountingCostObserver {
    var transactionID: UUID = UUID.randomUUID()
        protected set

    abstract fun onWeightsUpdate(selectionCost: SelectionCost)
    abstract fun setErrorStatus(status: Int, reason: String)
    protected abstract fun generateTransactionID()
}

object LLAccountingCostManager {
    private val objectList: MutableSet<UUID> = mutableSetOf()
    private val pendingObjectQuota: MutableSet<UUID> = mutableSetOf()

    fun addObject(objectID: UUID) {
        objectList.add(objectID)
    }

    fun removePendingObject(objectID: UUID) {
        pendingObjectQuota.remove(objectID)
    }

    fun fetchCosts(
        selectionType: SelectionType,
        url: String,
        observer: LLAccountingCostObserver
    ) {
        if (url.isEmpty()) {
            objectList.clear()
            pendingObjectQuota.clear()
            return
        }
        accountingCostCoro(url, selectionType, observer)
    }

    private fun accountingCostCoro(
        url: String,
        selectionType: SelectionType,
        observer: LLAccountingCostObserver
    ) {
        val diffSet = objectList - pendingObjectQuota
        if (diffSet.isEmpty()) return

        objectList.clear()

        val keystr = when (selectionType) {
            SelectionType.Roots -> "selected_roots"
            SelectionType.Prims -> "selected_prims"
        }

        pendingObjectQuota.addAll(diffSet)

        TODO("APR: use JVM equivalent — POST diffSet as { keystr: [uuid...] } to url, parse 'selected.physics/streaming/simulation', call observer.onWeightsUpdate or observer.setErrorStatus")

        pendingObjectQuota.clear()
    }
}
