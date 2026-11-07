package com.firestorm.newview

import java.util.UUID
import java.util.LinkedList

object DialogStack {

    private val notificationIds: LinkedList<UUID> = LinkedList()

    private fun update() {
        SavedSettings.instance.setBool("DialogStackIconVisible", notificationIds.size > 1)
    }

    fun push(uuid: UUID) {
        notificationIds.addLast(uuid)
        update()
    }

    fun pop(uuid: UUID) {
        if (notificationIds.isEmpty()) {
            System.err.println("Dialog Stack count was 0 when pop() was called.")
        } else {
            notificationIds.remove(uuid)
            update()
        }
    }

    fun flip(uuid: UUID): UUID {
        val index = notificationIds.indexOf(uuid)
        if (index < 0) return UUID(0L, 0L)
        return if (index == 0) notificationIds.last else notificationIds[index - 1]
    }
}
