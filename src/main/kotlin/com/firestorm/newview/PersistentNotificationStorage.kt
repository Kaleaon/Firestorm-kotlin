package com.firestorm.newview

import java.util.UUID

object PersistentNotificationStorage : NotificationStorage("") {
    private var loaded = false
    private var duringBulkUpdate = false

    init {
        initialize()
    }

    fun reset() {
        val scrubbedGrid = scrubbedFileName(GridManager.grid())
        setFileName(dirUtils.expandedFilename(PathType.PER_SL_ACCOUNT, "open_notifications_$scrubbedGrid.xml"))
        setOldFileName(dirUtils.expandedFilename(PathType.PER_SL_ACCOUNT, "open_notifications.xml"))
    }

    private fun initialize() {
        reset()
        Notifications.getChannel("Persistent")
            .connectChanged(::onPersistentChannelChanged)
    }

    fun saveNotifications() {
        val historyChannel = Notifications.getChannel("Persistent") as? PersistentNotificationChannel ?: return

        val data = mutableListOf<Map<String, Any>>()
        for (notification in historyChannel.history()) {
            if (notification.isRespondedTo() || notification.isCancelled() || notification.isExpired()) continue

            data.add(notification.asLLSD(true))
            if (data.size >= savedSettings.getInt("MaxPersistentNotifications")) {
                System.err.println("PersistentNotificationStorage: Too many persistent notifications. Saved ${data.size} of ${historyChannel.size()}.")
                break
            }
        }

        writeNotifications(mapOf("data" to data))
    }

    fun loadNotifications() {
        if (loaded) {
            System.out.println("PersistentNotificationStorage: notifications already loaded, exiting")
            return
        }

        System.out.println("PersistentNotificationStorage: start loading notifications")
        loaded = true

        val input = mutableMapOf<String, Any>()
        if (!readNotifications(input) || input.isEmpty()) return

        @Suppress("UNCHECKED_CAST")
        val data = input["data"] as? List<Map<String, Any>> ?: return

        val notificationChannel = ChannelManager.findChannelByID(NOTIFICATION_CHANNEL_UUID) as? ScreenChannel

        startBulkUpdate()

        val maxNotifications = savedSettings.getInt("MaxPersistentNotifications")
        val notificationsArray = mutableListOf<Map<String, Any>>()
        var processed = 0
        for (params in data.asReversed()) {
            notificationsArray.add(params)
            if (++processed >= maxNotifications) {
                System.err.println("PersistentNotificationStorage: Too many persistent notifications. Processed $processed of ${data.size}.")
                break
            }
        }

        for (params in notificationsArray.asReversed()) {
            val id = params["id"] as? UUID ?: UUID.randomUUID()
            val name = params["name"] as? String ?: ""
            val notification = Notification(id = id, name = name, payload = params)

            @Suppress("UNCHECKED_CAST")
            val responderSd = params["responder"] as? Map<String, Any> ?: emptyMap()
            val responder = createResponder(name, responderSd)
            if (responder != null) {
                notification.setResponseFunctor(responder)
            }

            Notifications.load(notification)
            ScriptFloaterManager.setFloaterVisible(notification.id, false)
            notificationChannel?.hideToast(notification.id)
        }

        Notifications.getChannel("Persistent")
            .connectChanged(::onPersistentChannelChanged)

        endBulkUpdate()
        System.out.println("PersistentNotificationStorage: finished loading notifications")
    }

    fun startBulkUpdate() { duringBulkUpdate = true }
    fun endBulkUpdate() { duringBulkUpdate = false }

    private fun onPersistentChannelChanged(payload: Map<String, Any>): Boolean {
        if (!loaded) {
            loadNotifications()
        }
        val sigtype = payload["sigtype"] as? String ?: ""
        if (sigtype != "load" && !duringBulkUpdate) {
            saveNotifications()
        }
        return false
    }

    private val NOTIFICATION_CHANNEL_UUID: UUID = UUID.fromString("d6b45f04-6ea6-4e1f-b31d-100000000001")
}

// Stub for the persistent notification channel's history access.
abstract class PersistentNotificationChannel : NotificationChannel("Persistent", "", "persistent") {
    abstract fun history(): List<Notification>
    abstract fun size(): Int
}

fun ScreenChannel.hideToast(id: UUID) { TODO("GPU: hide specific toast in screen channel") }

// Stub objects referenced from PersistentNotificationStorage.
object GridManager {
    fun grid(): String = TODO("APR: get current grid identifier string")
}

object dirUtils {
    fun expandedFilename(path: PathType, name: String): String = TODO("APR: expand platform path for given path type and filename")
}

enum class PathType { PER_SL_ACCOUNT }

object savedSettings {
    fun getInt(key: String): Int = TODO("APR: read integer setting by key")
}

fun scrubbedFileName(name: String): String = TODO("APR: strip illegal filesystem characters from filename")
