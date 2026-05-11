package com.firestorm.newview

import java.util.UUID

// Stub types representing viewer concepts not yet ported.
class Toast
class ScreenChannelBase {
    fun removeToastByNotificationID(id: UUID) { TODO("GPU: remove toast by notification ID") }
}
class ScreenChannel : ScreenChannelBase() {
    fun addToast(params: ToastParams) { TODO("GPU: add toast to screen channel") }
    fun removeToastByNotificationID(id: UUID) { TODO("GPU: remove toast by notification ID") }
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
        TODO("GPU: process IM notification, show toast or log to IM session")
    }

    override fun initChannel() {
        TODO("GPU: initialize IM notification screen channel position")
    }
}

class TipHandler : SystemNotificationHandler("Tip", "notifytip") {
    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        TODO("GPU: process tip notification, show tip toast")
    }

    override fun initChannel() {
        TODO("GPU: initialize Tip notification screen channel position")
    }
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
        TODO("GPU: process group notice notification, show toast")
    }

    override fun initChannel() {
        TODO("GPU: initialize Group notification screen channel position")
    }
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
        TODO("GPU: process alert notification, show modal or non-modal alert dialog")
    }

    override fun initChannel() {
        TODO("GPU: initialize Alert notification screen channel position")
    }
}

class ViewerAlertHandler(name: String, notificationType: String)
    : SystemNotificationHandler(name, notificationType) {

    override fun onDelete(p: NotificationPtr) {}

    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        TODO("GPU: process viewer alert notification")
    }

    override fun initChannel() {}
}

class OfferHandler : CommunicationNotificationHandler("Offer", "offer") {
    override fun onChange(p: NotificationPtr) { processNotification(p) }

    override fun onDelete(notification: NotificationPtr) {
        TODO("GPU: remove offer notification toast and clean up offer state")
    }

    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        TODO("GPU: process offer notification, show offer toast")
    }

    override fun initChannel() {
        TODO("GPU: initialize Offer notification screen channel position")
    }
}

class HintHandler : SystemNotificationHandler("Hint", "hint") {
    override fun onAdd(p: NotificationPtr) { TODO("GPU: show UI hint") }
    override fun onLoad(p: NotificationPtr) {}
    override fun onDelete(p: NotificationPtr) { TODO("GPU: hide UI hint") }

    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        TODO("GPU: process hint notification")
    }

    override fun initChannel() {}
}

class BrowserNotification : SystemNotificationHandler("Browser", "browser") {
    override fun processNotification(notify: NotificationPtr, shouldLog: Boolean): Boolean {
        TODO("GPU: process browser notification, open media browser floater")
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
            TODO("GPU: render chat message to console overlay")
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
    fun getMessage(): String = TODO("GPU: get notification formatted message")
    fun canLogToIM(): Boolean = TODO("GPU: check if notification should log to IM")
    fun hasFormElements(): Boolean = TODO("GPU: check if notification has script dialog form")
    fun canShowToast(): Boolean = TODO("GPU: check if notification can show as toast")
    fun canFadeToast(): Boolean = TODO("GPU: check if notification toast can auto-fade")
    fun getSubstitutions(): Map<String, String> = TODO("GPU: get notification substitution map")
    fun isRespondedTo(): Boolean = TODO("GPU: check if notification was responded to")
    fun isCancelled(): Boolean = TODO("GPU: check if notification was cancelled")
    fun isExpired(): Boolean = TODO("GPU: check if notification has expired")
    fun isPersistent(): Boolean = TODO("GPU: check if notification is persistent")
    fun setDND(dnd: Boolean) { TODO("GPU: set DND flag on notification") }
    fun setResponseFunctor(responder: NotificationResponderInterface) { TODO("GPU: attach responder functor") }
    fun asLLSD(includeResponder: Boolean): Map<String, Any> = TODO("GPU: serialize notification to LLSD")
    val notificationId: UUID get() = id
}

object NotificationPriority {
    const val HIGH = 100
}

object Notifications {
    fun find(id: UUID): Notification? = TODO("GPU: look up notification by id")
    fun update(notification: Notification) { TODO("GPU: propagate notification update through channels") }
    fun add(notification: Notification) { TODO("GPU: add notification to system") }
    fun cancel(notification: Notification) { TODO("GPU: cancel and remove notification") }
    fun load(notification: Notification) { TODO("GPU: load persisted notification into system") }
    fun getChannel(name: String): NotificationChannel = TODO("GPU: retrieve named notification channel")
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
    fun createNotificationChannel(): ScreenChannel? = TODO("GPU: create and register a new screen notification channel")
    fun findChannelByID(id: UUID): ScreenChannelBase? = TODO("GPU: find screen channel by UUID")
}

object ViewerWindow {
    fun worldViewRectScaledRight(): Int = TODO("GPU: get right edge of scaled world view rect")
}

object SavedSettings {
    fun getInt(key: String): Int = TODO("APR: read integer from saved settings")
    fun getBool(key: String): Boolean = TODO("APR: read boolean from saved settings")
}

object Agent {
    fun isDoNotDisturb(): Boolean = TODO("APR: check agent DND status")
    fun getGroupData(groupId: UUID): GroupData? = TODO("APR: get agent group data by id")
}

data class GroupData(val name: String)

object ToastPanel {
    fun buildPanelFromNotification(notification: Notification): Any? = TODO("GPU: build toast UI panel for notification")
}

object ScriptFloaterManager {
    fun onAddNotification(id: UUID) { TODO("GPU: show script floater for notification") }
    fun onRemoveNotification(id: UUID) { TODO("GPU: remove script floater for notification") }
    fun setFloaterVisible(id: UUID, visible: Boolean) { TODO("GPU: set script floater visibility") }
}

object IMMgr {
    fun computeSessionID(type: InstantMessageType, agentId: UUID): UUID = TODO("APR: compute IM session id")
    fun addSession(name: String, type: InstantMessageType, agentId: UUID): UUID = TODO("APR: create new IM session")
}

object IMModel {
    var activeSessionId: UUID? = null
    fun findIMSession(sessionId: UUID): IMSession? = TODO("APR: find IM session by id")
    fun logToFile(fileName: String, from: String, fromId: UUID, message: String) { TODO("APR: log IM message to file") }
    fun addMessageSilently(sessionId: UUID, from: String, fromId: UUID, message: String) { TODO("APR: add IM message without notification") }
    fun setActiveSessionID(id: UUID) { activeSessionId = id }
    fun resetActiveSessionID() { activeSessionId = null }
    fun emitNewMsgSignal(arg: Map<String, Any>) { TODO("APR: emit new message signal to listeners") }
}

class IMSession {
    var numUnread: Int = 0
    var participantUnreadMessageCount: Int = 0
    val messages: ArrayDeque<Map<String, Any>> = ArrayDeque()
}

object AvatarNameCache {
    fun get(id: UUID, callback: (AvatarName) -> Unit) { TODO("APR: async avatar name lookup") }
    fun getCached(id: UUID): AvatarName? = TODO("APR: synchronous cached avatar name lookup")
    fun findIdByName(name: String): UUID? = TODO("APR: reverse-lookup avatar id by display name")
}

data class AvatarName(val userName: String, val displayName: String)

object CacheName {
    fun buildUsername(name: String): String = TODO("APR: normalize legacy name to username format")
}

object LogChat {
    fun groupChatSuffix(): String = TODO("APR: get group chat log file suffix")
    fun timestamp2LogString(time: Long, utc: Boolean): String = TODO("APR: format timestamp for log")
}

object Trans {
    fun getString(key: String): String = TODO("APR: get translated UI string")
}

object FSFloaterIM {
    fun findInstance(sessionId: UUID): FSFloaterIMInstance? = TODO("GPU: find IM floater by session id")
}

class FSFloaterIMInstance {
    fun isVisible(): Boolean = TODO("GPU: check if IM floater is visible")
    fun updateMessages() { TODO("GPU: refresh messages in IM floater") }
}

object FSFloaterNearbyChat {
    val instance: FSFloaterNearbyChatInstance? get() = TODO("GPU: get nearby chat floater instance")
}

class FSFloaterNearbyChatInstance {
    fun addMessage(chat: Chat) { TODO("GPU: add chat message to nearby chat floater") }
}

object RlvActions {
    fun isRlvEnabled(): Boolean = TODO("APR: check if RLV restrictions are active")
    fun hasBehaviour(behaviour: String): Boolean = TODO("APR: check specific RLV behaviour flag")
}

object RlvUtil {
    fun notifyBlocked(reason: String) { TODO("APR: show RLV blocked notification") }
}

const val RLV_BHVR_SENDCHAT = "sendchat"

fun ScreenChannelBase.isVisible(): Boolean = TODO("GPU: check if channel is visible on screen")
fun ScreenChannelBase.init(left: Int, right: Int) { TODO("GPU: position screen channel between left and right bounds") }
fun ScreenChannel.setControlHovering(enable: Boolean) { TODO("GPU: enable/disable hover control on channel") }
