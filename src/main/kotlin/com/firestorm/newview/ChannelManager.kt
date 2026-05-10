package com.firestorm.newview

import java.util.UUID

enum class ChannelAlign { LEFT, RIGHT }
enum class ToastAlign { TOP, BOTTOM }

data class ScreenChannelParams(
    val id: UUID,
    val channelAlign: ChannelAlign = ChannelAlign.RIGHT,
    val toastAlign: ToastAlign = ToastAlign.BOTTOM
)

abstract class ScreenChannelBase(val params: ScreenChannelParams) {
    val channelId: UUID get() = params.id
    var showToasts: Boolean = true
    var displayToastsAlways: Boolean = false

    abstract fun getNumberOfHiddenToasts(): Int
    abstract fun setVisible(visible: Boolean)
}

open class ScreenChannel(params: ScreenChannelParams) : ScreenChannelBase(params) {

    var mouseDownCallback: ((Int, Int, Int) -> Unit)? = null
    var commitCallback: (() -> Unit)? = null

    override fun getNumberOfHiddenToasts(): Int = TODO("GPU: query hidden toast count")
    override fun setVisible(visible: Boolean): Unit = TODO("GPU: set channel visibility")

    fun init(leftBound: Int, rightBound: Int): Unit = TODO("GPU: initialize channel bounds")
    fun killMatchedToasts(matcher: ToastMatcher): Unit = TODO("GPU: kill matched toasts")
    fun createStartUpToast(awayNotifications: Int, lifetimeSecs: Float): Unit = TODO("GPU: create startup toast")
    fun closeStartUpToast(): Unit = TODO("GPU: close startup toast")

    companion object {
        fun setStartUpToastShown(): Unit = TODO("GPU: set startup toast shown flag")
    }
}

fun interface ToastMatcher {
    fun matches(toast: Any): Boolean
}

object ChannelManager {

    private val NOTIFICATION_CHANNEL_UUID: UUID = UUID.fromString("d8b6bafd-b6c4-4d5c-9a67-1d53b8b2d2b9")
    private val STARTUP_CHANNEL_UUID: UUID = UUID.fromString("fdff5b6c-cda0-4d4d-aac4-ca00a1c40e5e")
    private val NEARBY_CHAT_CHANNEL_UUID: UUID = UUID.fromString("3d3c4e6f-91e7-4e30-8940-0b89e14e8c8e")
    private const val NOTIFY_BOX_WIDTH = 300

    data class ChannelElem(val id: UUID, val channel: ScreenChannelBase)

    private val channelList: MutableList<ChannelElem> = mutableListOf()
    private var startUpChannel: ScreenChannel? = null

    fun init() {
        AppViewer.instance.setOnLoginCompletedCallback { onLoginCompleted() }
        // ViewerWindow must be initialized before ChannelManager
        check(ViewerWindow.isInitialized) { "ChannelManager.init() - viewer window is not initialized yet" }
    }

    fun cleanup() {
        channelList.forEach { it.channel.let { ch -> TODO("GPU: delete channel ${ch.channelId}") } }
        channelList.clear()
    }

    fun createNotificationChannel(): ScreenChannel? {
        val toastAlign = if (SavedSettings.instance.getBool("InternalShowGroupNoticesTopRight")) {
            ToastAlign.TOP
        } else {
            ToastAlign.BOTTOM
        }
        val p = ScreenChannelParams(
            id = NOTIFICATION_CHANNEL_UUID,
            channelAlign = ChannelAlign.RIGHT,
            toastAlign = toastAlign
        )
        return getChannel(p) as? ScreenChannel
    }

    fun onLoginCompleted() {
        var awayNotifications = 0

        for (elem in channelList) {
            val channel = elem.channel
            if (channel.channelId == NEARBY_CHAT_CHANNEL_UUID) continue
            if (!channel.displayToastsAlways) {
                awayNotifications += channel.getNumberOfHiddenToasts()
            }
        }

        awayNotifications += IMMgr.instance.numberOfUnreadIM

        if (awayNotifications == 0) {
            onStartUpToastClose()
            return
        }

        val p = ScreenChannelParams(id = STARTUP_CHANNEL_UUID, channelAlign = ChannelAlign.RIGHT)
        val channel = createChannel(p) ?: run { onStartUpToastClose(); return }
        startUpChannel = channel

        ViewerWindow.instance.rootView.addChild(channel)

        val rightBound = ViewerWindow.instance.worldViewRectRight - SavedSettings.instance.getInt("NotificationChannelRightMargin")
        channel.init(rightBound - NOTIFY_BOX_WIDTH, rightBound)

        if (!SavedSettings.instance.getBool("FSInternalLegacyNotificationWell")) {
            channel.mouseDownCallback = { x, y, mask ->
                FloaterNotificationsTabbed.instance.onStartUpToastClick(x, y, mask)
            }
        } else {
            channel.mouseDownCallback = { x, y, mask ->
                NotificationWellWindow.instance.onStartUpToastClick(x, y, mask)
            }
        }

        channel.commitCallback = { onStartUpToastClose() }
        channel.createStartUpToast(awayNotifications, SavedSettings.instance.getInt("StartUpToastLifeTime").toFloat())

        PersistentNotificationStorage.instance.loadNotifications()
        DoNotDisturbNotificationStorage.instance.loadNotifications()
    }

    fun onStartUpToastClose() {
        startUpChannel?.let { ch ->
            ch.setVisible(false)
            ch.closeStartUpToast()
            removeChannelById(STARTUP_CHANNEL_UUID)
            startUpChannel = null
        }
        ScreenChannel.setStartUpToastShown()
    }

    fun addChannel(channel: ScreenChannelBase): ScreenChannelBase {
        channelList.add(ChannelElem(channel.channelId, channel))
        return channel
    }

    private fun createChannel(params: ScreenChannelParams): ScreenChannel {
        val channel = ScreenChannel(params)
        addChannel(channel)
        return channel
    }

    fun getChannel(params: ScreenChannelParams): ScreenChannelBase {
        return findChannelById(params.id) ?: createChannel(params)
    }

    fun findChannelById(id: UUID): ScreenChannelBase? =
        channelList.firstOrNull { it.id == id }?.channel

    fun removeChannelById(id: UUID) {
        channelList.removeAll { it.id == id }
    }

    fun muteAllChannels(mute: Boolean) {
        channelList.forEach { it.channel.showToasts = !mute }
    }

    fun killToastsFromChannel(channelId: UUID, matcher: ToastMatcher) {
        (findChannelById(channelId) as? ScreenChannel)?.killMatchedToasts(matcher)
    }

    fun getNotificationScreenChannel(): ScreenChannel? {
        return findChannelById(NOTIFICATION_CHANNEL_UUID) as? ScreenChannel
            ?: run {
                System.err.println("Can't find screen channel by NotificationChannelUUID")
                null
            }
    }

    fun getChannelList(): List<ChannelElem> = channelList
}

object AppViewer {
    val instance: AppViewer get() = TODO("APR: use JVM equivalent")
    fun setOnLoginCompletedCallback(callback: () -> Unit): Unit = TODO("APR: use JVM equivalent")
}

object IMMgr {
    val instance: IMMgr get() = TODO("APR: use JVM equivalent")
    val numberOfUnreadIM: Int get() = TODO("APR: use JVM equivalent")
}

object FloaterNotificationsTabbed {
    val instance: FloaterNotificationsTabbed get() = TODO("APR: use JVM equivalent")
    fun onStartUpToastClick(x: Int, y: Int, mask: Int): Unit = TODO("GPU: handle startup toast click")
}

object NotificationWellWindow {
    val instance: NotificationWellWindow get() = TODO("APR: use JVM equivalent")
    fun onStartUpToastClick(x: Int, y: Int, mask: Int): Unit = TODO("GPU: handle startup toast click")
}

object PersistentNotificationStorage {
    val instance: PersistentNotificationStorage get() = TODO("APR: use JVM equivalent")
    fun loadNotifications(): Unit = TODO("APR: use JVM equivalent")
}

object DoNotDisturbNotificationStorage {
    val instance: DoNotDisturbNotificationStorage get() = TODO("APR: use JVM equivalent")
    fun loadNotifications(): Unit = TODO("APR: use JVM equivalent")
}

// Extensions on ViewerWindow for ChannelManager-specific needs
val ViewerWindow.rootView: RootView get() = TODO("GPU: get root view")
val ViewerWindow.worldViewRectRight: Int get() = TODO("GPU: get world view rect right edge")

class RootView {
    fun addChild(channel: ScreenChannelBase): Unit = TODO("GPU: add child to root view")
}
