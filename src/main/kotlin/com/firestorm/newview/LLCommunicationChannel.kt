package com.firestorm.newview

import java.time.Instant

// Analogues for SL types used here
typealias LLDate = Instant
typealias LLNotificationPtr = Any   // opaque handle; real type lives in the notification subsystem

// ---------------------------------------------------------------------------
// LLCommunicationChannel
//
// Extends LLNotificationChannel (represented abstractly here) and keeps a
// sorted history of notifications that failed the Do-Not-Disturb filter.
// ---------------------------------------------------------------------------

open class LLCommunicationChannel(
    private val name: String,
    private val parentName: String
) {
    // Sorted multi-map: date → notification.
    // TreeMap with list-of-values emulates std::multimap<LLDate, LLNotificationPtr>.
    private val mHistory: MutableMap<LLDate, MutableList<LLNotificationPtr>> =
        sortedMapOf(compareBy { it })

    init {
        TODO("APR: connect to notification channel named '$name' with parent '$parentName'; set filter to filterByDoNotDisturbStatus")
    }

    // ---- filter ------------------------------------------------------------

    companion object {
        fun filterByDoNotDisturbStatus(notification: LLNotificationPtr): Boolean {
            TODO("APR: return !gAgent.isDoNotDisturb()")
        }
    }

    // ---- history accessors -------------------------------------------------

    fun getHistorySize(): Int = mHistory.values.sumOf { it.size }

    fun beginHistory(): Sequence<Pair<LLDate, LLNotificationPtr>> =
        mHistory.entries.asSequence().flatMap { (date, list) -> list.map { date to it } }

    fun endHistory(): Sequence<Pair<LLDate, LLNotificationPtr>> = emptySequence()

    fun clearHistory() {
        mHistory.clear()
    }

    fun removeItemFromHistory(notification: LLNotificationPtr) {
        for ((date, list) in mHistory) {
            if (list.remove(notification)) {
                if (list.isEmpty()) mHistory.remove(date)
                break
            }
        }
    }

    // ---- notification channel callbacks ------------------------------------

    protected open fun onDelete(notification: LLNotificationPtr) {
        removeItemFromHistory(notification)
    }

    protected open fun onFilterFail(notification: LLNotificationPtr) {
        val type = getNotificationType(notification)
        val cancelled = isNotificationCancelled(notification)
        if ((type == "groupnotify" || type == "offer" || type == "notifytoast") && !cancelled) {
            val date = getNotificationDate(notification)
            mHistory.getOrPut(date) { mutableListOf() }.add(notification)
        }
    }

    // ---- helpers that delegate to the real notification object -------------

    private fun getNotificationType(notification: LLNotificationPtr): String {
        TODO("APR: return notification.getType()")
    }

    private fun isNotificationCancelled(notification: LLNotificationPtr): Boolean {
        TODO("APR: return notification.isCancelled()")
    }

    private fun getNotificationDate(notification: LLNotificationPtr): LLDate {
        TODO("APR: return notification.getDate()")
    }
}
