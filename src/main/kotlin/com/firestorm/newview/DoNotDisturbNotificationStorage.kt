package com.firestorm.newview

import java.util.UUID

private const val DND_TIMER_SECONDS = 3.0f

class DoNotDisturbNotificationStorageTimer {
    private var intervalSeconds: Float = DND_TIMER_SECONDS
    private var lastTickMs: Long = System.currentTimeMillis()

    fun tick(): Boolean {
        val now = System.currentTimeMillis()
        if ((now - lastTickMs) / 1000f >= intervalSeconds) {
            lastTickMs = now
            val storage = DoNotDisturbNotificationStorage
            if (storage.getDirty()) {
                storage.saveNotifications()
            }
        }
        return false
    }
}

object DoNotDisturbNotificationStorage : NotificationStorage("") {
    const val toastName = "IMToast"
    const val offerName = "UserGiveItem"

    private var dirty = false
    private val timer = DoNotDisturbNotificationStorageTimer()

    private val nameToPayloadParameterMap: MutableMap<String, String> = mutableMapOf(
        toastName to "SESSION_ID",
        offerName to "object_id"
    )

    init {
        initialize()
    }

    fun reset() {
        setFileName(dirUtils.expandedFilename(PathType.PER_SL_ACCOUNT, "dnd_notifications.xml"))
    }

    private fun initialize() {
        reset()
        getCommunicationChannel().connectFailedFilter(::onChannelChanged)
    }

    fun getDirty(): Boolean = dirty
    fun resetDirty() { dirty = false }

    fun saveNotifications() {
        val channelPtr = getCommunicationChannel()
        val commChannel = channelPtr as? CommunicationChannel
            ?: error("DoNotDisturbNotificationStorage: expected CommunicationChannel")

        val data = mutableListOf<Map<String, Any>>()
        for (notification in commChannel.history()) {
            if (!notification.isRespondedTo() && !notification.isCancelled()
                && !notification.isExpired() && !notification.isPersistent()
            ) {
                data.add(notification.asLLSD(true))
            }
        }

        writeNotifications(mapOf("data" to data))
        resetDirty()
    }

    fun loadNotifications() {
        System.out.println("DoNotDisturbNotificationStorage: start loading notifications")

        val input = mutableMapOf<String, Any>()
        if (!readNotifications(input) || input.isEmpty()) return

        @Suppress("UNCHECKED_CAST")
        val data = input["data"] as? List<Map<String, Any>> ?: return

        var imToastExists = false
        var groupAdHocToastExists = false
        var offerExists = false

        for (notificationParams in data) {
            val notificationId = notificationParams["id"] as? UUID ?: continue
            val notificationName = notificationParams["name"] as? String ?: ""
            var notification = Notifications.find(notificationId)

            if (notificationName == toastName) {
                val toastSessionType = (notificationParams["payload"] as? Map<*, *>)
                    ?.get("SESSION_TYPE") as? Int ?: -1
                when (toastSessionType) {
                    IMSessionType.P2P_SESSION -> imToastExists = true
                    IMSessionType.GROUP_SESSION, IMSessionType.ADHOC_SESSION -> {
                        groupAdHocToastExists = true
                        continue
                    }
                }
            } else if (notificationName == offerName) {
                offerExists = true
            }

            if (notification != null) {
                notification.setDND(true)
                Notifications.update(Notifications.find(notificationId)!!)
            } else {
                val paramsWithDnd = notificationParams + mapOf("is_dnd" to true)
                val id = notificationId
                val name = notificationName
                notification = Notification(id = id, name = name, payload = paramsWithDnd)

                @Suppress("UNCHECKED_CAST")
                val responderSd = (notificationParams["responder_sd"] as? Map<String, Any>) ?: emptyMap()
                val responderType = responderSd["responder_type"] as? String ?: ""
                val responder = createResponder(responderType, responderSd)
                if (responder == null) {
                    System.err.println("DoNotDisturbNotificationStorage: cannot create responder for notification of type '${notification.name}'")
                } else {
                    notification.setResponseFunctor(responder)
                }

                Notifications.add(notification)
            }
        }

        val isConversationLoggingAllowed = savedPerAccountSettings.getInt("KeepConversationLogTranscripts") > 0
        if (groupAdHocToastExists && isConversationLoggingAllowed) {
            FloaterReg.showInstance("conversation")
        }

        if (imToastExists || groupAdHocToastExists || offerExists) {
            makeUiSoundDeferred("UISndNewIncomingIMSession", false)
        }

        saveNotifications()
        System.out.println("DoNotDisturbNotificationStorage: finished loading notifications")
    }

    fun updateNotifications() {
        val channelPtr = getCommunicationChannel()
        val commChannel = channelPtr as? CommunicationChannel
            ?: error("DoNotDisturbNotificationStorage: expected CommunicationChannel")

        var imToastExists = false
        var offerExists = false

        for (notification in commChannel.history()) {
            val notificationName = notification.name
            when (notificationName) {
                toastName -> imToastExists = true
                offerName -> offerExists = true
            }
            notification.setDND(true)
            Notifications.update(notification)
        }

        if (imToastExists || offerExists) {
            makeUiSound("UISndNewIncomingIMSession")
        }

        if (commChannel.historySize() > 0) {
            commChannel.clearHistory()
            saveNotifications()
        }
    }

    fun removeNotification(name: String, id: UUID) {
        val channelPtr = getCommunicationChannel()
        val commChannel = channelPtr as? CommunicationChannel
            ?: error("DoNotDisturbNotificationStorage: expected CommunicationChannel")

        val payloadVariable = nameToPayloadParameterMap[name] ?: return
        val itemsToRemove = mutableListOf<Notification>()

        for (notification in commChannel.history()) {
            val notificationObjectId = notification.payload[payloadVariable] as? UUID
            if (notification.name == name && id == notificationObjectId) {
                itemsToRemove.add(notification)
            }
        }

        if (itemsToRemove.isNotEmpty()) {
            for (notification in itemsToRemove) {
                commChannel.removeItemFromHistory(notification)
                Notifications.cancel(notification)
            }
            saveNotifications()
        }
    }

    private fun getCommunicationChannel(): NotificationChannel {
        return Notifications.getChannel("Communication")
    }

    private fun onChannelChanged(payload: Map<String, Any>): Boolean {
        if ((payload["sigtype"] as? String) != "load") {
            dirty = true
        }
        return false
    }
}

// ---------------------------------------------------------------------------
// Stub declarations for viewer subsystems referenced above.
// ---------------------------------------------------------------------------

object IMSessionType {
    const val P2P_SESSION = 0
    const val GROUP_SESSION = 1
    const val ADHOC_SESSION = 2
}

abstract class CommunicationChannel : NotificationChannel("Communication", "", "communication") {
    abstract fun history(): List<Notification>
    abstract fun historySize(): Int
    abstract fun clearHistory()
    abstract fun removeItemFromHistory(notification: Notification)
}

object savedPerAccountSettings {
    fun getInt(key: String): Int { System.err.println("savedPerAccountSettings: getInt not yet implemented"); return 0 }
}

object FloaterReg {
    fun showInstance(name: String) { // no-op
    }
}

fun makeUiSoundDeferred(sound: String, immediate: Boolean) { System.err.println("makeUiSoundDeferred: not yet implemented") }
fun makeUiSound(sound: String) { System.err.println("makeUiSound: not yet implemented") }
