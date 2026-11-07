package com.firestorm.llui

import java.util.UUID

object NotificationsUtilUI {

    fun add(name: String): NotificationEntry =
        Notifications.add(name)

    fun add(name: String, substitutions: Map<String, Any>): NotificationEntry =
        Notifications.add(name, substitutions)

    fun add(name: String, substitutions: Map<String, Any>, payload: Map<String, Any>): NotificationEntry =
        Notifications.add(name, substitutions, payload)

    fun add(name: String, substitutions: Map<String, Any>, payload: Map<String, Any>, functorName: String): NotificationEntry =
        Notifications.add(name, substitutions, payload, functorName)

    fun add(
        name: String,
        substitutions: Map<String, Any>,
        payload: Map<String, Any>,
        functor: Responder
    ): NotificationEntry = Notifications.add(name, substitutions, payload, functor)

    fun getSelectedOption(notification: Map<String, Any>, response: Map<String, Any>): Int =
        NotificationEntry.getSelectedOption(notification, response)

    fun cancel(pNotif: NotificationEntry) {
        Notifications.cancel(pNotif)
    }

    fun find(uuid: UUID): NotificationEntry? =
        Notifications.find(uuid)
}
