package com.firestorm.llui

import java.util.UUID

object NotificationsUtilUI {

    fun add(name: String): Notification =
        Notifications.add(name)

    fun add(name: String, substitutions: Map<String, Any>): Notification =
        Notifications.add(name, substitutions)

    fun add(name: String, substitutions: Map<String, Any>, payload: Map<String, Any>): Notification =
        Notifications.add(name, substitutions, payload)

    fun add(name: String, substitutions: Map<String, Any>, payload: Map<String, Any>, functorName: String): Notification =
        Notifications.add(name, substitutions, payload, functorName)

    fun add(
        name: String,
        substitutions: Map<String, Any>,
        payload: Map<String, Any>,
        functor: Responder
    ): Notification = Notifications.add(name, substitutions, payload, functor)

    fun getSelectedOption(notification: Map<String, Any>, response: Map<String, Any>): Int =
        Notification.getSelectedOption(notification, response)

    fun cancel(pNotif: Notification) {
        Notifications.cancel(pNotif)
    }

    fun find(uuid: UUID): Notification? =
        Notifications.find(uuid)
}
