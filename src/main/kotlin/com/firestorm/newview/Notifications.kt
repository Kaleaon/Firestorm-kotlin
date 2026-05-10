package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLSD

enum class NotificationType {
    ALERT, ALERTMODAL, NOTIFY, NOTIFYTIP, GROUPNOTIFY, TOASTNOTIFY, OFFER, MEDIA
}

data class Notification(
    val id: LLUUID = LLUUID.generate(),
    val name: String,
    val type: NotificationType,
    val substitutions: LLSD = LLSD.emptyMap(),
    val payload: LLSD = LLSD.emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
)

object Notifications {

    typealias NotificationCallback = (Notification, LLSD) -> Unit

    val pending: MutableList<Notification> = mutableListOf()
    private val channels: MutableMap<String, NotificationCallback> = mutableMapOf()
    private val responses: MutableMap<LLUUID, LLSD> = mutableMapOf()

    fun add(
        name: String,
        subs: LLSD = LLSD.emptyMap(),
        payload: LLSD = LLSD.emptyMap(),
    ): Notification {
        val notification = Notification(
            name = name,
            type = resolveType(name),
            substitutions = subs,
            payload = payload,
        )
        pending.add(notification)
        return notification
    }

    fun cancel(id: LLUUID) {
        pending.removeAll { it.id == id }
    }

    fun find(id: LLUUID): Notification? = pending.firstOrNull { it.id == id }

    fun respond(notification: Notification, response: LLSD) {
        responses[notification.id] = response
        channels.values.forEach { handler -> handler(notification, response) }
        cancel(notification.id)
    }

    fun addChannel(name: String, handler: NotificationCallback) {
        channels[name] = handler
    }

    private fun resolveType(name: String): NotificationType = when {
        name.endsWith("Modal") -> NotificationType.ALERTMODAL
        name.startsWith("Group") -> NotificationType.GROUPNOTIFY
        name.startsWith("Offer") -> NotificationType.OFFER
        name.startsWith("Media") -> NotificationType.MEDIA
        name.startsWith("Toast") -> NotificationType.TOASTNOTIFY
        name.endsWith("Tip") -> NotificationType.NOTIFYTIP
        name.startsWith("Alert") -> NotificationType.ALERT
        else -> NotificationType.NOTIFY
    }
}
