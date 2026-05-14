package com.firestorm.newview

import java.util.UUID

// Stub types representing viewer concepts not yet ported.
class Toast
class ScreenChannelBase {
    fun removeToastByNotificationID(id: UUID) {}
}
class ScreenChannel : ScreenChannelBase() {
    fun addToast(params: ToastParams) {}
    fun removeToastByNotificationID(id: UUID) {}
}
data class ToastParams(
    val notifId: UUID,
    val notification: Notification,
    val panel: Any?,
    val onDeleteToast: ((Toast) -> Unit)?,
    val canFade: Boolean,
    val forceShow: Boolean = false
)

// Mirrors LLHandle<LLScreenChannelBase> — null when channel is dead.
class ChannelHandle(private var channel: ScreenChannelBase?) {
    fun get(): ScreenChannelBase? = channel
    fun isDead(): Boolean = channel == null
    fun clear() { channel = null }
}

abstract class EventHandler {
    protected var mChannel: ChannelHandle = ChannelHandle(null)

    protected open fun onDeleteToast(toast: Toast) {}
    protected abstract fun initChannel()
}

abstract class NotificationHandler(
    name: String,
    notificationType: String,
    parentName: String
) : EventHandler(), NotificationChannel(name, parentName, notificationType) {

    open fun onAdd(p: NotificationPtr) { processNotification(p) }
    open fun onChange(p: NotificationPtr) { processNotification(p) }
    open fun onLoad(p: NotificationPtr) { processNotification(p, shouldLog = false) }
    open fun onDelete(p: NotificationPtr) { mChannel.get()?.removeToastByNotificationID(p.id) }

    abstract fun processNotification(notify: NotificationPtr, shouldLog: Boolean = true): Boolean
}

open class SystemNotificationHandler(name: String, notificationType: String)
    : NotificationHandler(name, notificationType, "System")

open class CommunicationNotificationHandler(name: String, notificationType: String)
    : NotificationHandler(name, notificationType, "Communication")

abstract class ChatHandler : EventHandler() {
    abstract fun processChat(chatMsg: Chat, args: Map<String, Any>)
}

class IMHandler : CommunicationNotificationHandler("IM", "im") {
    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        return false
    }

    override fun initChannel() {}
}

class TipHandler : SystemNotificationHandler("Tip", "notifytip") {
    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        return false
    }

    override fun initChannel() {}
}

class ScriptHandler : SystemNotificationHandler("Notifications", "notify") {
    init {
        val channel = ChannelManager.createNotificationChannel()
        if (channel != null) {
            channel.setControlHovering(true)
            mChannel = ChannelHandle(channel)
        }
    }

    override fun initChannel() {
        val rightBound = ViewerWindow.worldViewRectScaledRight() - SavedSettings.getInt("NotificationChannelRightMargin")
        mChannel.get()?.init(rightBound - NOTIFY_BOX_WIDTH, rightBound)
    }

    fun addToastWithNotification(notification: NotificationPtr) {
        val notifyBox = ToastPanel.buildPanelFromNotification(notification)
        val params = ToastParams(
            notifId = notification.id,
            notification = notification,
            panel = notifyBox,
            onDeleteToast = ::onDeleteToast,
            canFade = notification.canFadeToast(),
            forceShow = if (Agent.isDoNotDisturb()) {
                notification.name == "SystemMessage"
                    || notification.name == "GodMessage"
                    || notification.priority >= NotificationPriority.HIGH
            } else false
        )
        (mChannel.get() as? ScreenChannel)?.addToast(params)
    }

    override fun processNotification(notification: NotificationPtr, shouldLog: Boolean): Boolean {
        if (mChannel.isDead()) return false

        if (mChannel.get()?.isVisible() != true) {
            initChannel()
        }

        if (shouldLog && notification.canLogToIM()) {
            HandlerUtil.logToIMP2P(notification)
        }

        if (notification.hasFormElements() && !notification.canShowToast()) {
            if (RlvActions.isRlvEnabled()) {
                val chatChannel = notification.payload["chat_channel"] as? Int
                if (chatChannel != null && chatChannel == 0 && RlvActions.hasBehaviour(RLV_BHVR_SENDCHAT)) {
                    RlvUtil.notifyBlocked("blocked_scriptdialog")
                    return false
                }
            }
            ScriptFloaterManager.onAddNotification(notification.id)
        } else if (notification.canShowToast()) {
            addToastWithNotification(notification)
        }

        return false
    }

    override fun onChange(notification: NotificationPtr) {
        val channel = mChannel.get() as? ScreenChannel
        if (channel != null) {
            channel.removeToastByNotificationID(notification.id)
            addToastWithNotification(notification)
        }
    }

    override fun onDelete(notification: NotificationPtr) {
        if (notification.hasFormElements() && !notification.canShowToast()) {
            ScriptFloaterManager.onRemoveNotification(notification.id)
        } else {
            mChannel.get()?.removeToastByNotificationID(notification.id)
        }
    }

    override fun onDeleteToast(toast: Toast) {
        val notification = Notifications.find(toast.notificationId) ?: return
        if (notification.hasFormElements() && !notification.canShowToast()) {
            ScriptFloaterManager.onRemoveNotification(notification.id)
        }
    }

    companion object {
        private const val NOTIFY_BOX_WIDTH = 320
    }
}

class GroupHandler : CommunicationNotificationHandler("Group", "groupnotice") {
    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        return false
    }

    override fun initChannel() {}
}

class AlertHandler(
    name: String,
    notificationType: String,
    private val isModal: Boolean
) : SystemNotificationHandler(name, notificationType) {

    override fun onChange(p: NotificationPtr) {
        processNotification(p)
    }

    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        return false
    }

    override fun initChannel() {}
}

class ViewerAlertHandler(name: String, notificationType: String)
    : SystemNotificationHandler(name, notificationType) {

    override fun onDelete(p: NotificationPtr) {}

    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        return false
    }

    override fun initChannel() {}
}

class OfferHandler : CommunicationNotificationHandler("Offer", "offer") {
    override fun onChange(p: NotificationPtr) { processNotification(p) }

    override fun onDelete(notification: NotificationPtr) {}

    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        return false
    }

    override fun initChannel() {}
}

class HintHandler : SystemNotificationHandler("Hint", "hint") {
    override fun onAdd(p: NotificationPtr) {}
    override fun onLoad(p: NotificationPtr) {}
    override fun onDelete(p: NotificationPtr) {}

    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        return false
    }

    override fun initChannel() {}
}

class BrowserNotification : SystemNotificationHandler("Browser", "browser") {
    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        return false
    }

    override fun initChannel() {}
}

object HandlerUtil {
    fun isIMFloaterOpened(notification: NotificationPtr): Boolean {
        val fromId = notification.payload["from_id"] as? UUID ?: return false
        val sessionId = IMMgr.computeSessionID(InstantMessageType.IM_NOTHING_SPECIAL, fromId)
        return FSFloaterIM.findInstance(sessionId)?.isVisible() == true
    }

    fun logToIM(
        sessionType: InstantMessageType,
        sessionName: String,
        fromName: String,
        message: String,
        sessionOwnerId: UUID,
        fromId: UUID
    ) {
        val from = fromName.ifEmpty { SYSTEM_FROM }
        val sessionId = IMMgr.computeSessionID(sessionType, sessionOwnerId)
        val session = IMModel.findIMSession(sessionId)

        if (session == null) {
            val actualFrom = if (from == INTERACTIVE_SYSTEM_FROM) SYSTEM_FROM else from
            val fileName = when {
                sessionType == InstantMessageType.IM_SESSION_GROUP_START ->
                    sessionName + LogChat.groupChatSuffix()
                SavedSettings.getBool("UseLegacyIMLogNames") ->
                    sessionName.substringBefore(" Resident")
                else ->
                    CacheName.buildUsername(sessionName)
            }
            IMModel.logToFile(fileName, actualFrom, fromId, message)
        } else {
            val activeSessionId = IMModel.activeSessionId
            IMModel.setActiveSessionID(sessionId)

            val unread = session.numUnread
            val participantUnread = session.participantUnreadMessageCount
            IMModel.addMessageSilently(sessionId, from, fromId, message)
            session.numUnread = unread
            session.participantUnreadMessageCount = participantUnread

            updateIMFloaterMessages(sessionId)

            if (activeSessionId == null) IMModel.resetActiveSessionID()
            else IMModel.setActiveSessionID(activeSessionId)
        }
    }

    fun logToIMP2P(fromId: UUID, message: String, toFileOnly: Boolean = false) {
        if (fromId == UUID(0, 0)) return
        val fromName = if (toFileOnly) "" else INTERACTIVE_SYSTEM_FROM
        AvatarNameCache.get(fromId) { avName ->
            logToIM(InstantMessageType.IM_NOTHING_SPECIAL, avName.userName, fromName, message, fromId, UUID(0, 0))
        }
    }

    fun logToIMP2P(notification: NotificationPtr, toFileOnly: Boolean = false) {
        val fromId = notification.payload["from_id"] as? UUID ?: UUID(0, 0)
        logToIMP2P(fromId, notification.getMessage(), toFileOnly)
    }

    fun logGroupNoticeToIMGroup(notification: NotificationPtr) {
        val payload = notification.payload
        val groupId = payload["group_id"] as? UUID ?: return
        val groupData = Agent.getGroupData(groupId) ?: run {
            System.err.println("HandlerUtil: Group notice for unknown group: $groupId")
            return
        }

        if (!SavedSettings.getBool("FSGroupNoticesToIMLog")) return

        val groupName = groupData.name
        val senderName = payload["sender_name"] as? String ?: ""
        var senderId = payload["sender_id"] as? UUID

        if (senderId == null || senderId == UUID(0, 0)) {
            senderId = AvatarNameCache.findIdByName(senderName)
        }

        val message = if (SavedSettings.getBool("FSBetterGroupNoticesToIMLog")) {
            "${Trans.getString("GroupNotifyGroupNotice")} ${Trans.getString("GroupNotifySentBy")}: $senderName\n" +
                "${payload["subject"]}\n${payload["message"]}"
        } else {
            payload["message"] as? String ?: ""
        }

        logToIM(InstantMessageType.IM_SESSION_GROUP_START, groupName, senderName, message, groupId, senderId ?: UUID(0, 0))
    }

    fun logToNearbyChat(notification: NotificationPtr, type: ChatSourceType) {
        val nearbyChat = FSFloaterNearbyChat.instance ?: return
        val chatMsg = Chat(notification.getMessage()).apply {
            sourceType = type
            fromName = SYSTEM_FROM
            fromId = UUID(0, 0)
        }
        nearbyChat.addMessage(chatMsg)

        if (SavedSettings.getBool("FSUseNearbyChatConsole")) {
            // GPU render stub: render chat message to console overlay
        }
    }

    fun spawnIMSession(name: String, fromId: UUID): UUID {
        val sessionId = IMMgr.computeSessionID(InstantMessageType.IM_NOTHING_SPECIAL, fromId)
        if (IMModel.findIMSession(sessionId) == null) {
            return IMMgr.addSession(name, InstantMessageType.IM_NOTHING_SPECIAL, fromId)
        }
        return sessionId
    }

    fun getSubstitutionName(notification: NotificationPtr): String {
        val subs = notification.getSubstitutions()
        var res = subs["NAME"] ?: subs["[NAME]"] ?: ""
        if (res.isEmpty()) {
            var fromId = notification.payload["FROM_ID"] as? UUID
            if (fromId == null || fromId == UUID(0, 0)) {
                fromId = notification.payload["from_id"] as? UUID
            }
            if (fromId != null && fromId != UUID(0, 0)) {
                res = AvatarNameCache.getCached(fromId)?.userName ?: ""
            }
        }
        return res
    }

    fun getSubstitutionOriginalName(notification: NotificationPtr): String {
        val name = notification.getSubstitutions()["ORIGINAL_NAME"]
        if (!name.isNullOrEmpty()) return name
        return getSubstitutionName(notification)
    }

    fun addNotifPanelToIM(notification: NotificationPtr) {
        val name = getSubstitutionName(notification)
        val fromId = notification.payload["from_id"] as? UUID ?: UUID(0, 0)
        val sessionId = spawnIMSession(name, fromId)
        val session = IMModel.findIMSession(sessionId) ?: error("Session must exist after spawnIMSession")

        val offer = mutableMapOf<String, Any>(
            "notification_id" to notification.id,
            "from" to SYSTEM_FROM,
            "time" to LogChat.timestamp2LogString(0, false),
            "index" to session.messages.size
        )
        session.messages.addFirst(offer)

        val arg = mapOf(
            "session_id" to sessionId,
            "num_unread" to ++session.numUnread,
            "participant_unread" to ++session.participantUnreadMessageCount
        )
        IMModel.emitNewMsgSignal(arg)
    }

    fun updateIMFloaterMessages(sessionId: UUID) {
        val floater = FSFloaterIM.findInstance(sessionId)
        if (floater != null && floater.isVisible()) {
            floater.updateMessages()
        }
    }

    fun decIMMessageCounter(notification: NotificationPtr) {
        val name = getSubstitutionName(notification)
        val fromId = notification.payload["from_id"] as? UUID ?: UUID(0, 0)
        val sessionId = IMMgr.computeSessionID(InstantMessageType.IM_NOTHING_SPECIAL, fromId)
        val session = IMModel.findIMSession(sessionId) ?: return

        val arg = mutableMapOf<String, Any>("session_id" to sessionId)
        session.numUnread--
        arg["num_unread"] = session.numUnread
        session.participantUnreadMessageCount--
        arg["participant_unread"] = session.participantUnreadMessageCount
        IMModel.emitNewMsgSignal(arg)
    }

    private const val SYSTEM_FROM = "Second Life"
    private const val INTERACTIVE_SYSTEM_FROM = "Interactive System"
}

// ---------------------------------------------------------------------------
// Stub declarations for viewer subsystems referenced above.
// These will be replaced when those modules are ported.
// ---------------------------------------------------------------------------

abstract class NotificationChannel(val name: String, val parentName: String, val filterType: String) {
    abstract fun connectChanged(listener: (Map<String, Any>) -> Boolean)
    abstract fun connectFailedFilter(listener: (Map<String, Any>) -> Boolean)
}

class Notification(
    val id: UUID,
    val name: String,
    val payload: Map<String, Any>,
    val priority: Int = 0
) {
    fun getMessage(): String = ""
    fun canLogToIM(): Boolean = false
    fun hasFormElements(): Boolean = false
    fun canShowToast(): Boolean = false
    fun canFadeToast(): Boolean = false
    fun getSubstitutions(): Map<String, String> = emptyMap()
    fun isRespondedTo(): Boolean = false
    fun isCancelled(): Boolean = false
    fun isExpired(): Boolean = false
    fun isPersistent(): Boolean = false
    fun setDND(dnd: Boolean) {}
    fun setResponseFunctor(responder: NotificationResponderInterface) {}
    fun asLLSD(includeResponder: Boolean): Map<String, Any> = emptyMap()
    val notificationId: UUID get() = id
}

object NotificationPriority {
    const val HIGH = 100
}

object Notifications {
    fun find(id: UUID): Notification? = null
    fun update(notification: Notification) {}
    fun add(notification: Notification) {}
    fun cancel(notification: Notification) {}
    fun load(notification: Notification) {}
    fun getChannel(name: String): NotificationChannel {
        System.err.println("Notifications: getChannel not yet implemented")
        throw UnsupportedOperationException("Notifications: getChannel not yet implemented")
    }
}

class Chat(val text: String) {
    var sourceType: ChatSourceType = ChatSourceType.CHAT_SOURCE_SYSTEM
    var fromName: String = ""
    var fromId: UUID = UUID(0, 0)
}

enum class ChatSourceType { CHAT_SOURCE_SYSTEM, CHAT_SOURCE_AGENT, CHAT_SOURCE_OBJECT }
enum class InstantMessageType {
    IM_NOTHING_SPECIAL, IM_SESSION_GROUP_START
}

object ChannelManager {
    fun createNotificationChannel(): ScreenChannel? = null
    fun findChannelByID(id: UUID): ScreenChannelBase? = null
}

object ViewerWindow {
    fun worldViewRectScaledRight(): Int = 0
}

object SavedSettings {
    fun getInt(key: String): Int = 0
    fun getBool(key: String): Boolean = false
}

object Agent {
    fun isDoNotDisturb(): Boolean = false
    fun getGroupData(groupId: UUID): GroupData? = null
}

data class GroupData(val name: String)

object ToastPanel {
    fun buildPanelFromNotification(notification: Notification): Any? = null
}

object ScriptFloaterManager {
    fun onAddNotification(id: UUID) {}
    fun onRemoveNotification(id: UUID) {}
    fun setFloaterVisible(id: UUID, visible: Boolean) {}
}

object IMMgr {
    fun computeSessionID(type: InstantMessageType, agentId: UUID): UUID = UUID(0, 0)
    fun addSession(name: String, type: InstantMessageType, agentId: UUID): UUID = UUID(0, 0)
}

object IMModel {
    var activeSessionId: UUID? = null
    fun findIMSession(sessionId: UUID): IMSession? = null
    fun logToFile(fileName: String, from: String, fromId: UUID, message: String) {}
    fun addMessageSilently(sessionId: UUID, from: String, fromId: UUID, message: String) {}
    fun setActiveSessionID(id: UUID) { activeSessionId = id }
    fun resetActiveSessionID() { activeSessionId = null }
    fun emitNewMsgSignal(arg: Map<String, Any>) {}
}

class IMSession {
    var numUnread: Int = 0
    var participantUnreadMessageCount: Int = 0
    val messages: ArrayDeque<Map<String, Any>> = ArrayDeque()
}

object AvatarNameCache {
    fun get(id: UUID, callback: (AvatarName) -> Unit) {}
    fun getCached(id: UUID): AvatarName? = null
    fun findIdByName(name: String): UUID? = null
}

data class AvatarName(val userName: String, val displayName: String)

object CacheName {
    fun buildUsername(name: String): String = ""
}

object LogChat {
    fun groupChatSuffix(): String = ""
    fun timestamp2LogString(time: Long, utc: Boolean): String = ""
}

object Trans {
    fun getString(key: String): String = ""
}

object FSFloaterIM {
    fun findInstance(sessionId: UUID): FSFloaterIMInstance? = null
}

class FSFloaterIMInstance {
    fun isVisible(): Boolean = false
    fun updateMessages() {}
}

object FSFloaterNearbyChat {
    val instance: FSFloaterNearbyChatInstance? get() = null
}

class FSFloaterNearbyChatInstance {
    fun addMessage(chat: Chat) {}
}

object RlvActions {
    fun isRlvEnabled(): Boolean = false
    fun hasBehaviour(behaviour: String): Boolean = false
}

object RlvUtil {
    fun notifyBlocked(reason: String) {}
}

const val RLV_BHVR_SENDCHAT = "sendchat"

fun ScreenChannelBase.isVisible(): Boolean = false
fun ScreenChannelBase.init(left: Int, right: Int) {}
fun ScreenChannel.setControlHovering(enable: Boolean) {}
