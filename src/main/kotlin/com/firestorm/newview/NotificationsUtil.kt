package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLSD

object NotificationsUtil {

    fun add(name: String): Notification =
        Notifications.add(name)

    fun add(name: String, substitutions: LLSD): Notification =
        Notifications.add(name, substitutions)

    fun add(name: String, substitutions: LLSD, payload: LLSD): Notification =
        Notifications.add(name, substitutions, payload)

    fun add(
        name: String,
        substitutions: LLSD,
        payload: LLSD,
        functorName: String,
    ): Notification = Notifications.add(name, substitutions, payload, functorName)

    fun add(
        name: String,
        substitutions: LLSD,
        payload: LLSD,
        functor: (LLSD, LLSD) -> Unit,
    ): Notification = Notifications.add(name, substitutions, payload, functor)

    fun getSelectedOption(notification: LLSD, response: LLSD): Int =
        Notifications.getSelectedOption(notification, response)

    fun cancel(notification: Notification) {
        Notifications.cancel(notification.id)
    }

    fun find(uuid: LLUUID): Notification? = Notifications.find(uuid)
}
