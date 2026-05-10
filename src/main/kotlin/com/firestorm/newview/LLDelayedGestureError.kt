package com.firestorm.newview

import java.util.UUID

object LLDelayedGestureError {

    private const val MAX_NAME_WAIT_TIME = 5.0f

    private data class LLErrorEntry(
        val notifyName: String,
        val itemId: UUID,
        val startTimeMs: Long = System.currentTimeMillis()
    ) {
        fun elapsedSeconds(): Float = (System.currentTimeMillis() - startTimeMs) / 1000.0f
    }

    private val sQueue: ArrayDeque<LLErrorEntry> = ArrayDeque()

    fun gestureMissing(id: UUID) {
        val ent = LLErrorEntry("GestureMissing", id)
        if (!doDialog(ent)) {
            enqueue(ent)
        }
    }

    fun gestureFailedToLoad(id: UUID) {
        val ent = LLErrorEntry("UnableToLoadGesture", id)
        if (!doDialog(ent)) {
            enqueue(ent)
        }
    }

    private fun enqueue(ent: LLErrorEntry) {
        if (sQueue.isEmpty()) {
            TODO("APR: use JVM equivalent of gIdleCallbacks.addFunction(::onIdle)")
        }
        sQueue.addLast(ent)
    }

    fun onIdle() {
        if (sQueue.isNotEmpty()) {
            val ent = sQueue.removeFirst()
            if (!doDialog(ent, uuidOk = false)) {
                enqueue(ent)
            }
        } else {
            TODO("APR: use JVM equivalent of gIdleCallbacks.deleteFunction(::onIdle)")
        }
    }

    private fun doDialog(ent: LLErrorEntry, uuidOk: Boolean = false): Boolean {
        val name: String = run {
            val item = gInventory.getItem(ent.itemId)
            if (item != null) {
                item.name
            } else {
                if (uuidOk || ent.elapsedSeconds() > MAX_NAME_WAIT_TIME) {
                    ent.itemId.toString()
                } else {
                    return false
                }
            }
        }

        if (!LLApp.isExiting()) {
            LLNotificationsUtil.add(ent.notifyName, mapOf("NAME" to name))
        }

        return true
    }
}
