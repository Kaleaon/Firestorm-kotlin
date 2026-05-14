package com.firestorm.newview

import java.util.UUID

data class EventInfo(
    var name: String = "",
    var id: UInt = 0u,
    var desc: String = "",
    var categoryStr: String = "",
    var duration: UInt = 0u,
    var timeStr: String = "",
    var runById: UUID = UUID(0, 0),
    var simName: String = "",
    var posGlobal: DoubleArray = DoubleArray(3),
    var unixTime: Double = 0.0,
    var hasCover: Boolean = false,
    var cover: UInt = 0u,
    var eventFlags: UInt = 0u
) {
    fun unpack(msg: Any) { System.err.println("EventInfo: unpack not yet implemented") }
}

class EventNotification(
    private val eventId: UInt,
    private val eventDateEpoch: Double,
    private val eventDateStr: String,
    private val eventName: String
) {
    fun getEventID(): UInt = eventId
    fun getEventName(): String = eventName
    fun getEventDateEpoch(): Double = eventDateEpoch
    fun getEventDateStr(): String = eventDateStr
    fun isValid(): Boolean = eventId > 0u && eventDateEpoch != 0.0 && eventName.isNotEmpty()
}

class EventNotifier {

    private val eventNotifications: MutableMap<UInt, EventNotification> = mutableMapOf()

    private var lastNotificationCheckMs: Long = 0L

    private val eventInfoListeners: MutableList<(EventInfo) -> Boolean> = mutableListOf()

    fun addEventInfoCallback(cb: (EventInfo) -> Boolean) {
        eventInfoListeners += cb
    }

    fun update() {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastNotificationCheckMs < 30_000L) return
        lastNotificationCheckMs = nowMs

        val alertTime = System.currentTimeMillis() / 1000.0 + 5 * 60.0
        val toFire = eventNotifications.values.filter { it.getEventDateEpoch() < alertTime }
        for (notification in toFire) {
            fireNotification(notification)
            remove(notification.getEventID())
        }
    }

    private fun fireNotification(n: EventNotification) {
        System.err.println("EventNotifier: fireNotification not yet implemented")
    }

    fun handleResponse(eventId: UInt, option: Int): Boolean {
        when (option) {
            0 -> System.err.println("EventNotifier: handleResponse not yet implemented")
            else -> { }
        }
        return true
    }

    fun add(event: EventInfo): Boolean {
        if (eventInfoListeners.any { it(event) }) return false
        return add(event.id, event.unixTime, event.timeStr, event.name)
    }

    fun add(eventId: UInt, eventEpoch: Double, eventDateStr: String, eventName: String): Boolean {
        val notification = EventNotification(eventId, eventEpoch, eventDateStr, eventName)
        if (!notification.isValid()) return false
        eventNotifications[notification.getEventID()] = notification
        return true
    }

    fun add(eventId: UInt) {
        System.err.println("EventNotifier: add not yet implemented")
    }

    fun load(eventOptions: List<Map<String, Any>>) {
        for (response in eventOptions) {
            val eventId = (response["event_id"] as? Number)?.toInt()?.toUInt() ?: continue
            val eventDateUt = (response["event_date_ut"] as? Number)?.toDouble() ?: 0.0
            val eventDateStr = response["event_date"]?.toString() ?: ""
            val eventName = response["event_name"]?.toString() ?: ""
            add(eventId, eventDateUt, eventDateStr, eventName)
        }
    }

    fun remove(eventId: UInt) {
        if (!eventNotifications.containsKey(eventId)) return
        serverPushRequest(eventId, add = false)
        eventNotifications.remove(eventId)
    }

    fun hasNotification(eventId: UInt): Boolean = eventNotifications.containsKey(eventId)

    fun serverPushRequest(eventId: UInt, add: Boolean) {
        System.err.println("EventNotifier: serverPushRequest not yet implemented")
    }

    companion object {
        fun processEventInfoReply(msg: Any) {
            val info = EventInfo()
            info.unpack(msg)
            EventNotifierGlobal.add(info)
        }
    }
}

object EventNotifierGlobal : EventNotifier()
