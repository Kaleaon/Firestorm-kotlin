package com.firestorm.newview

import com.firestorm.network.HttpClient
import java.util.UUID

data class SelectionCost(
    val physicsCost: Float,
    val networkCost: Float,
    val simulationCost: Float
)

enum class SelectionType { ROOTS, PRIMS }

interface AccountingCostObserver {
    fun onWeightsUpdate(selectionCost: SelectionCost)
    fun setErrorStatus(status: Int, reason: String)
    fun generateTransactionId()
    val transactionId: UUID
}

object AccountingCostManager {

    private val objectList: MutableSet<UUID> = mutableSetOf()
    private val pendingObjectQuota: MutableSet<UUID> = mutableSetOf()

    fun addObject(objectId: UUID) {
        objectList.add(objectId)
    }

    fun removePendingObject(objectId: UUID) {
        pendingObjectQuota.remove(objectId)
    }

    fun fetchCosts(
        selectionType: SelectionType,
        url: String,
        observer: AccountingCostObserver
    ) {
        if (url.isEmpty()) {
            Logger.warn("Supplied url is empty")
            objectList.clear()
            pendingObjectQuota.clear()
            return
        }

        System.err.println("AccountingCostManager: fetchCosts not yet implemented")
    }

    private suspend fun accountingCostCoro(
        url: String,
        selectionType: SelectionType,
        observer: AccountingCostObserver
    ) {
        val diffSet = objectList - pendingObjectQuota
        if (diffSet.isEmpty()) return

        objectList.clear()

        val keyStr = when (selectionType) {
            SelectionType.ROOTS -> "selected_roots"
            SelectionType.PRIMS -> "selected_prims"
        }

        val objectListPayload = diffSet.map { it.toString() }
        pendingObjectQuota.addAll(diffSet)

        val dataToPost = mapOf(keyStr to objectListPayload)

        try {
            val results = HttpClient.postAndAwait(url, dataToPost)

            val httpStatus = results["http_result"]["status"].asInt()
            val httpSuccess = results["http_result"]["success"].asBoolean()

            if (!httpSuccess || results.containsKey("error")) {
                val message = results["http_result"]["message"].asString()
                observer.setErrorStatus(httpStatus, message)
                return
            }

            val selected = results["selected"]
            if (selected != null) {
                val physicsCost = selected["physics"].asFloat()
                val networkCost = selected["streaming"].asFloat()
                val simulationCost = selected["simulation"].asFloat()
                observer.onWeightsUpdate(SelectionCost(physicsCost, networkCost, simulationCost))
            }
        } catch (e: Exception) {
            Logger.error("Exception in accountingCostCoro for url '$url': ${e.message}")
            throw e
        } finally {
            pendingObjectQuota.clear()
        }
    }
}
