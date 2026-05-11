package com.firestorm.newview

import java.util.UUID

data class AvatarName(
    val displayName: String,
    val userName: String
)

object FSConsoleUtils {

    fun processChatMessage(chatMsg: LLChat, args: Map<String, Any>): Boolean {
        val useNearbyChatConsole = getSetting("FSUseNearbyChatConsole") as? Boolean ?: false
        val useChatBubbles = getSetting("UseChatBubbles") as? Boolean ?: false
        val bubblesHideConsoleAndToasts = getSetting("FSBubblesHideConsoleAndToasts") as? Boolean ?: false

        if (!useNearbyChatConsole) {
            return false
        }

        val agentDoNotDisturb = isAgentDoNotDisturb()
        if (chatMsg.sourceType == CHAT_SOURCE_AGENT && useChatBubbles && bubblesHideConsoleAndToasts || agentDoNotDisturb) {
            return true
        }

        when (chatMsg.sourceType) {
            CHAT_SOURCE_AGENT -> {
                TODO("APR: use JVM equivalent for async avatar name cache lookup; " +
                    "on result call onProcessChatAvatarNameLookup")
            }
            CHAT_SOURCE_OBJECT -> {
                val ircMe = isIrcMePrefix(chatMsg.text)
                val delimiter = resolveDelimiter(chatMsg, ircMe)
                val message = if (ircMe) chatMsg.text.substring(3) else chatMsg.text
                val consoleChat = chatMsg.fromName + delimiter + message
                addToConsole(consoleChat, chatMsg)
            }
            else -> {
                val moneyTracker = args["money_tracker"] as? Boolean ?: false
                val consoleChat = when {
                    moneyTracker && chatMsg.sourceType == CHAT_SOURCE_SYSTEM ->
                        args["console_message"] as? String ?: ""
                    chatMsg.fromName.isEmpty() -> chatMsg.text
                    else -> "${chatMsg.fromName} ${chatMsg.text}"
                }
                addToConsole(consoleChat, chatMsg)
            }
        }

        return true
    }

    protected fun onProcessChatAvatarNameLookup(agentId: UUID, avName: AvatarName, chatMsg: LLChat) {
        val ircMe = isIrcMePrefix(chatMsg.text)
        val delimiter = resolveDelimiter(chatMsg, ircMe)
        val message = if (ircMe) chatMsg.text.substring(3) else chatMsg.text

        val senderName = if (!chatMsg.rlvNamesFiltered) {
            getAvatarNameByDisplaySettings(avName)
        } else {
            chatMsg.fromName
        }

        val consoleChat = senderName + delimiter + message
        addToConsole(consoleChat, chatMsg)
    }

    fun processInstantMessage(sessionId: UUID, fromId: UUID, message: String): Boolean {
        val useNearbyChatConsole = getSetting("FSUseNearbyChatConsole") as? Boolean ?: false
        val logGroupImToChatConsole = getSetting("FSLogGroupImToChatConsole") as? Boolean ?: false
        val logImToChatConsole = getSetting("FSLogImToChatConsole") as? Boolean ?: false

        if (!useNearbyChatConsole) return false

        val session = findImSession(sessionId) ?: return false
        if (!logGroupImToChatConsole && session.isGroupSession) return false
        if (!logImToChatConsole && !session.isGroupSession) return false
        if (fromId == UUID(0, 0) || message.isEmpty()) return true

        val groupNameLength = getSetting("FSShowGroupNameLength") as? Int ?: 0
        val group = if (groupNameLength != 0 && session.isGroupSession) {
            session.name.substring(0, minOf(groupNameLength, session.name.length))
        } else {
            ""
        }

        TODO("APR: use JVM equivalent for async avatar name cache lookup; " +
            "on result call onProccessInstantMessageNameLookup with message, group, sessionId")
    }

    protected fun onProccessInstantMessageNameLookup(
        agentId: UUID,
        avName: AvatarName,
        messageStr: String,
        group: String,
        sessionId: UUID
    ) {
        val isGroup = group.isNotEmpty()
        var message = messageStr
        val delimiter = if (isIrcMePrefix(message)) {
            message = message.substring(3)
            ""
        } else {
            ": "
        }

        var senderName = getAvatarNameByDisplaySettings(avName)
        if (isGroup) {
            senderName = "[$group] $senderName"
        }

        val consoleText = "IM: $senderName$delimiter$message"
        TODO("APR: use JVM equivalent for console line output with color and session_id=$sessionId; " +
            "line='$consoleText'")
    }

    private fun resolveDelimiter(chatMsg: LLChat, ircMe: Boolean): String {
        if (ircMe || chatMsg.chatStyle == CHAT_STYLE_IRC) return ""
        val shout = getTranslatedString("shout")
        val whisper = getTranslatedString("whisper")
        return if (chatMsg.chatType == CHAT_TYPE_SHOUT ||
            chatMsg.chatType == CHAT_TYPE_WHISPER ||
            chatMsg.text.startsWith(shout) ||
            chatMsg.text.startsWith(whisper)
        ) " " else ": "
    }

    private fun addToConsole(line: String, chatMsg: LLChat) {
        TODO("APR: use JVM equivalent for gConsole->addConsoleLine with color derived from chatMsg")
    }

    private fun isIrcMePrefix(text: String): Boolean = text.startsWith("/me ")

    private fun getAvatarNameByDisplaySettings(avName: AvatarName): String {
        TODO("APR: use JVM equivalent for FSCommon::getAvatarNameByDisplaySettings")
    }

    private fun getSetting(key: String): Any? {
        TODO("APR: use JVM equivalent for gSavedSettings.get*")
    }

    private fun isAgentDoNotDisturb(): Boolean {
        TODO("APR: use JVM equivalent for gAgent.isDoNotDisturb()")
    }

    private fun findImSession(sessionId: UUID): ImSession? {
        TODO("APR: use JVM equivalent for LLIMModel::instance().findIMSession(sessionId)")
    }

    private fun getTranslatedString(key: String): String {
        TODO("APR: use JVM equivalent for LLTrans::getString(key)")
    }

    private data class ImSession(val name: String, val isGroupSession: Boolean)

    private const val CHAT_SOURCE_AGENT = 1
    private const val CHAT_SOURCE_OBJECT = 2
    private const val CHAT_SOURCE_SYSTEM = 0
    private const val CHAT_TYPE_SHOUT = 3
    private const val CHAT_TYPE_WHISPER = 2
    private const val CHAT_STYLE_IRC = 1
}
