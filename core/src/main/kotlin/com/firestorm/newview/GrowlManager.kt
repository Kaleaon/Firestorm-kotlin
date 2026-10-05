package com.firestorm.newview

import java.util.UUID

data class GrowlNotification(
    val growlName: String = "",
    val growlTitle: String = "",
    val growlBody: String = "",
    val useDefaultTextForTitle: Boolean = false,
    val useDefaultTextForBody: Boolean = false
)

const val GROWL_THROTTLE_TIME: Long = 1_000_000L
const val GROWL_THROTTLE_CLEANUP_PERIOD: Float = 300.0f
const val GROWL_MAX_BODY_LENGTH: Int = 255
const val GROWL_IM_MESSAGE_TYPE: String = "Instant Message received"
const val GROWL_KEYWORD_ALERT_TYPE: String = "Keyword Alert"

var gGrowlManager: GrowlManager? = null

class GrowlManager : EventTimer(GROWL_THROTTLE_CLEANUP_PERIOD) {

    private val notifier: GrowlNotifier
    private val notifications: MutableMap<String, GrowlNotification> = mutableMapOf()
    private val titleTimers: MutableMap<String, Long> = mutableMapOf()

    private var notificationConnection: (() -> Unit)? = null
    private var instantMessageConnection: (() -> Unit)? = null
    private var scriptDialogConnection: (() -> Unit)? = null
    private var chatMessageConnection: (() -> Unit)? = null

    init {
        notifier = platformNotifier()

        val usable = if (isPlatformWindows()) {
            loadConfig()
            notifier.isUsable()
        } else {
            val u = notifier.isUsable()
            if (u) loadConfig()
            u
        }

        if (usable) {
            notificationConnection = connectLLNotificationChannel("GrowlNotifications", ::filterOldNotifications) { notice ->
                onLLNotification(notice)
            }
            instantMessageConnection = connectImNewMsgCallback(::onInstantMessage)
            scriptDialogConnection = connectScriptDialogCallback(::onScriptDialog)
            chatMessageConnection = connectNearbyChatCallback(::onNearbyChatMessage)
        }
    }

    override fun close() {
        notificationConnection?.invoke()
        instantMessageConnection?.invoke()
        scriptDialogConnection?.invoke()
        chatMessageConnection?.invoke()
    }

    override fun tick(): Boolean {
        titleTimers.clear()
        return false
    }

    private fun loadConfig() {
        val configFile = expandFilename(LL_PATH_APP_SETTINGS, "growl_notifications.xml")
        if (configFile.isEmpty()) return

        val notificationTypes = mutableSetOf(GROWL_KEYWORD_ALERT_TYPE, GROWL_IM_MESSAGE_TYPE)
        val notificationLLSD = parseLLSDXml(configFile) ?: return

        for ((key, value) in notificationLLSD.asMap()) {
            val ntype = GrowlNotification(
                growlName = value["GrowlName"].asString().also { notificationTypes.add(it) },
                growlTitle = value["GrowlTitle"]?.asString() ?: "",
                growlBody = value["GrowlBody"]?.asString() ?: "",
                useDefaultTextForTitle = value["UseDefaultTextForTitle"]?.asBoolean() ?: false,
                useDefaultTextForBody = run {
                    val explicit = value["UseDefaultTextForBody"]?.asBoolean() ?: false
                    val impliedFallback = !explicit &&
                        (value["UseDefaultTextForTitle"]?.asBoolean() != true) &&
                        (value["GrowlBody"]?.asString()?.isEmpty() != false) &&
                        (value["GrowlTitle"]?.asString()?.isEmpty() != false)
                    explicit || impliedFallback
                }
            )
            notifications[key] = ntype
        }

        notifier.registerApplication("Firestorm Viewer", notificationTypes)
    }

    private fun performNotification(title: String, message: String, type: String) {
        if (isAppExiting()) return
        if (!isFSEnableGrowl()) return
        if (!shouldNotify()) return

        if (notifier.needsThrottle()) {
            val now = totalTimeMicros()
            val last = titleTimers[title]
            if (last != null && last > now - GROWL_THROTTLE_TIME) {
                titleTimers[title] = now
                return
            }
            titleTimers[title] = now
        }

        notifier.showNotification(title, message.take(GROWL_MAX_BODY_LENGTH), type)
    }

    companion object {

        fun initiateManager() {
            gGrowlManager = GrowlManager()
        }

        fun destroyManager() {
            gGrowlManager?.close()
            gGrowlManager = null
        }

        fun isUsable(): Boolean = gGrowlManager?.notifier?.isUsable() == true

        fun notify(title: String, message: String, type: String) {
            if (isUsable()) {
                gGrowlManager!!.performNotification(title, message, type)
            }
        }

        private fun onLLNotification(notice: LLSD): Boolean {
            if (notice["sigtype"].asString() != "add") return false

            val notification = llNotificationsFind(notice["id"].asUUID()) ?: return false
            val name = notification.getName()
            val substitutions = notification.getSubstitutions()
            val gn = gGrowlManager?.notifications?.get(name) ?: return false

            var title = if (gn.useDefaultTextForTitle) {
                notification.getMessage()
            } else if (gn.growlTitle.isNotEmpty()) {
                formatString(gn.growlTitle, substitutions)
            } else ""

            var body = if (gn.useDefaultTextForBody) {
                notification.getMessage()
            } else if (gn.growlBody.isNotEmpty()) {
                formatString(gn.growlBody, substitutions)
            } else ""

            val urlReplacedNames = setOf(
                "ObjectGiveItem", "OwnObjectGiveItem", "ObjectGiveItemUnknownUser",
                "UserGiveItem", "SystemMessageTip"
            )
            if (name in urlReplacedNames || name.startsWith("TeleportOffered") || name == "TeleportRequest") {
                body = replaceUrlsWithLabels(body)
            }

            gGrowlManager!!.performNotification(title, body, gn.growlName)
            return false
        }

        private fun filterOldNotifications(notification: LLNotification): Boolean {
            return notification.getDate().secondsSinceEpoch() >= nowSecondsSinceEpoch() - 10
        }

        private fun onInstantMessage(im: LLSD) {
            val session = imModelFindSession(im["session_id"].asUUID()) ?: return
            if (!session.isP2PSessionType()) return
            if (im["is_announcement"].asBoolean()) return
            if (im["keyword_alert_performed"].asBoolean() && filterGrowlKeywordDuplicateIMs()) return

            val fromId = im["from_id"].asUUID()
            if (fromId == null || fromId == agentId()) return

            var message = im["message"].asString()
            if (isIrcMePrefix(message)) message = message.substring(3)

            avatarNameCacheGetAsync(fromId) { avName ->
                onAvatarNameCache(avName, message, GROWL_IM_MESSAGE_TYPE)
            }
        }

        private fun onScriptDialog(data: LLSD) {
            val notification = llNotificationsFind(data["notification_id"].asUUID()) ?: return
            val name = notification.getName()
            val substitutions = notification.getSubstitutions()
            val gn = gGrowlManager?.notifications?.get(name) ?: return

            val title = if (gn.useDefaultTextForTitle) {
                notification.getMessage()
            } else if (gn.growlTitle.isNotEmpty()) {
                formatString(gn.growlTitle, substitutions)
            } else ""

            val body = if (gn.useDefaultTextForBody) {
                notification.getMessage()
            } else if (gn.growlBody.isNotEmpty()) {
                formatString(gn.growlBody, substitutions)
            } else ""

            gGrowlManager!!.performNotification(title, body, gn.growlName)
        }

        private fun onNearbyChatMessage(chat: LLSD) {
            if (chat["chat_type"].asInt() != CHAT_TYPE_IM) return

            var message = chat["message"].asString()
            if (isIrcMePrefix(message)) message = message.substring(3)

            if (chat["source"].asInt() == CHAT_SOURCE_AGENT) {
                avatarNameCacheGetAsync(chat["from_id"].asUUID()) { avName ->
                    onAvatarNameCache(avName, message, GROWL_IM_MESSAGE_TYPE)
                }
            } else {
                gGrowlManager!!.performNotification(chat["from"].asString(), message, GROWL_IM_MESSAGE_TYPE)
            }
        }

        private fun onAvatarNameCache(avName: AvatarName, message: String, type: String) {
            val sender = getAvatarNameByDisplaySettings(avName)
            notify(sender, message, type)
        }

        private fun shouldNotify(): Boolean {
            if (!isStartupComplete()) return false
            if (isAgentDoNotDisturb()) return false
            val activatedWhenVisible = isFSGrowlWhenActive()
            return activatedWhenVisible || !isViewerWindowVisible() || !appHasFocus()
        }

        private fun platformNotifier(): GrowlNotifier { System.err.println("GrowlManager: APR: use JVM equivalent to select platform notifier (Win/Linux/generic) not yet implemented"); return GrowlNotifier() }
        private fun isPlatformWindows(): Boolean { System.err.println("GrowlManager: APR: use JVM System.getProperty(\"os.name\") to detect Windows not yet implemented"); return false }
        private fun isAppExiting(): Boolean { System.err.println("GrowlManager: APR: use JVM equivalent for LLAppViewer::isExiting not yet implemented"); return false }
        private fun isFSEnableGrowl(): Boolean { System.err.println("GrowlManager: APR: use JVM equivalent for gSavedSettings getBOOL(\"FSEnableGrowl\") not yet implemented"); return false }
        private fun isFSGrowlWhenActive(): Boolean { System.err.println("GrowlManager: APR: use JVM equivalent for gSavedSettings getBOOL(\"FSGrowlWhenActive\") not yet implemented"); return false }
        private fun filterGrowlKeywordDuplicateIMs(): Boolean { System.err.println("GrowlManager: APR: use JVM equivalent for gSavedSettings getBOOL(\"FSFilterGrowlKeywordDuplicateIMs\") not yet implemented"); return false }
        private fun totalTimeMicros(): Long { System.err.println("GrowlManager: APR: use JVM equivalent for LLTimer::getTotalTime not yet implemented"); return 0L }
        private fun isStartupComplete(): Boolean { System.err.println("GrowlManager: APR: use JVM equivalent for LLStartUp::getStartupState >= STATE_STARTED not yet implemented"); return false }
        private fun isAgentDoNotDisturb(): Boolean { System.err.println("GrowlManager: APR: use JVM equivalent for gAgent.isDoNotDisturb() not yet implemented"); return false }
        private fun isViewerWindowVisible(): Boolean { System.err.println("GrowlManager: APR: use JVM equivalent for gViewerWindow->getWindow()->getVisible() not yet implemented"); return false }
        private fun appHasFocus(): Boolean { System.err.println("GrowlManager: APR: use JVM equivalent for gFocusMgr.getAppHasFocus() not yet implemented"); return false }
        private fun agentId(): UUID { System.err.println("GrowlManager: APR: use JVM equivalent for gAgentID not yet implemented"); return UUID(0, 0) }
        private fun expandFilename(path: Int, name: String): String { System.err.println("GrowlManager: APR: use JVM equivalent for gDirUtilp->getExpandedFilename not yet implemented"); return "" }
        private fun parseLLSDXml(path: String): LLSDMap? { System.err.println("GrowlManager: APR: use JVM equivalent for LLSDSerialize::fromXML not yet implemented"); return null }
        private fun llNotificationsFind(id: UUID): LLNotification? { System.err.println("GrowlManager: APR: use JVM equivalent for LLNotifications::instance().find not yet implemented"); return null }
        private fun formatString(template: String, substitutions: Map<String, String>): String { System.err.println("GrowlManager: APR: use JVM equivalent for LLStringUtil::format not yet implemented"); return "" }
        private fun replaceUrlsWithLabels(text: String): String { System.err.println("GrowlManager: APR: use JVM equivalent for LLUrlRegistry URL-to-label replacement not yet implemented"); return "" }
        private fun imModelFindSession(sessionId: UUID): IMSession? { System.err.println("GrowlManager: APR: use JVM equivalent for LLIMModel::instance().findIMSession not yet implemented"); return null }
        private fun isIrcMePrefix(msg: String): Boolean { System.err.println("GrowlManager: APR: use JVM equivalent for FSCommon::is_irc_me_prefix not yet implemented"); return false }
        private fun getAvatarNameByDisplaySettings(avName: AvatarName): String { System.err.println("GrowlManager: APR: use JVM equivalent for FSCommon::getAvatarNameByDisplaySettings not yet implemented"); return "" }
        private fun avatarNameCacheGetAsync(id: UUID, callback: (AvatarName) -> Unit): Unit { System.err.println("GrowlManager: APR: use JVM equivalent for LLAvatarNameCache::get with callback not yet implemented") }
        private fun connectLLNotificationChannel(name: String, filter: (LLNotification) -> Boolean, handler: (LLSD) -> Boolean): () -> Unit { System.err.println("GrowlManager: APR: use JVM equivalent for LLNotificationChannel + connectChanged not yet implemented"); return {} }
        private fun connectImNewMsgCallback(handler: (LLSD) -> Unit): () -> Unit { System.err.println("GrowlManager: APR: use JVM equivalent for LLIMModel::addNewMsgCallback not yet implemented"); return {} }
        private fun connectScriptDialogCallback(handler: (LLSD) -> Unit): () -> Unit { System.err.println("GrowlManager: APR: use JVM equivalent for LLScriptFloaterManager::addNewObjectCallback not yet implemented"); return {} }
        private fun connectNearbyChatCallback(handler: (LLSD) -> Unit): () -> Unit { System.err.println("GrowlManager: APR: use JVM equivalent for LLNotificationManager getChatHandler addNewChatCallback not yet implemented"); return {} }
        private fun nowSecondsSinceEpoch(): Double { System.err.println("GrowlManager: APR: use JVM equivalent for LLDate::now().secondsSinceEpoch() not yet implemented"); return 0.0 }
    }
}
