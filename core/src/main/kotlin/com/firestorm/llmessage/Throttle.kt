package com.firestorm.llmessage

import com.firestorm.llcommon.LLUUID

enum class ThrottleType {
    VIEWER_ALERT,
    AGENT_ALERT,
    NUM_THROTTLES
}

data class ThrottleEntry(val hash: Long, val timestamp: Long)

class MessageThrottle {

    private val windowMillis: Long = 10_000L

    private val lists: Map<ThrottleType, ArrayDeque<ThrottleEntry>> = ThrottleType.entries
        .filter { it != ThrottleType.NUM_THROTTLES }
        .associateWith { ArrayDeque() }

    fun addViewerAlert(to: LLUUID, message: String): Boolean {
        val hash = combineHash(to.hashCode().toLong(), message.hashCode().toLong())
        return addEntry(ThrottleType.VIEWER_ALERT, hash)
    }

    fun addAgentAlert(agent: LLUUID, task: LLUUID, message: String): Boolean {
        val hash = combineHash(combineHash(agent.hashCode().toLong(), task.hashCode().toLong()), message.hashCode().toLong())
        return addEntry(ThrottleType.AGENT_ALERT, hash)
    }

    fun checkOverride(type: ThrottleType, id: LLUUID): Boolean {
        val hash = id.hashCode().toLong()
        return addEntry(type, hash)
    }

    private fun addEntry(type: ThrottleType, hash: Long): Boolean {
        val list = lists[type] ?: return false
        val now = System.currentTimeMillis()
        pruneList(list, now)
        if (list.any { it.hash == hash }) return true
        list.addLast(ThrottleEntry(hash, now))
        return false
    }

    fun pruneEntries() {
        val now = System.currentTimeMillis()
        lists.values.forEach { pruneList(it, now) }
    }

    private fun pruneList(list: ArrayDeque<ThrottleEntry>, now: Long) {
        while (list.isNotEmpty() && now - list.first().timestamp > windowMillis) {
            list.removeFirst()
        }
    }

    private fun combineHash(a: Long, b: Long): Long = a * 31L + b
}
