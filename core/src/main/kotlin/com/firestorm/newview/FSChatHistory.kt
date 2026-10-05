package com.firestorm.newview

import java.util.UUID
import java.util.Date

enum class ModerationStyle {
    NORMAL, BOLD, ITALIC, BOLD_ITALIC, UNDERLINE, BOLD_UNDERLINE, ITALIC_UNDERLINE, BOLD_ITALIC_UNDERLINE
}

fun applyModeratorStyle(style: ModerationStyle): String = when (style) {
    ModerationStyle.BOLD -> "BOLD"
    ModerationStyle.ITALIC -> "ITALIC"
    ModerationStyle.BOLD_ITALIC -> "BOLDITALIC"
    ModerationStyle.UNDERLINE -> "UNDERLINE"
    ModerationStyle.BOLD_UNDERLINE -> "BOLDUNDERLINE"
    ModerationStyle.ITALIC_UNDERLINE -> "ITALICUNDERLINE"
    ModerationStyle.BOLD_ITALIC_UNDERLINE -> "BOLDITALICUNDERLINE"
    ModerationStyle.NORMAL -> "NORMAL"
}

data class ChatHistoryParams(
    val messageHeader: String = "",
    val messageSeparator: String = "",
    val leftTextPad: Int = 0,
    val rightTextPad: Int = 0,
    val leftWidgetPad: Int = 0,
    val rightWidgetPad: Int = 0,
    val topSeparatorPad: Int = 0,
    val bottomSeparatorPad: Int = 0,
    val topHeaderPad: Int = 0,
    val bottomHeaderPad: Int = 0
)

data class LLChat(
    val fromId: UUID = UUID.randomUUID(),
    val sessionId: UUID = UUID.randomUUID(),
    val fromName: String = "",
    val text: String = "",
    val timeStr: String = "",
    val time: Double = 0.0,
    val sourceType: Int = 0,
    val chatType: Int = 0,
    val chatStyle: Int = 0,
    val ownerID: UUID = UUID.randomUUID(),
    val posAgent: FloatArray = FloatArray(3),
    val url: String = "",
    val notifId: UUID? = null,
    val fromNameGroup: String = "",
    val rlvNamesFiltered: Boolean = false
)

class FSChatHistoryHeader {
    var avatarId: UUID = UUID.randomUUID()
    var sessionId: UUID = UUID.randomUUID()
    var sourceType: Int = 0
    var chatType: Int = 0
    var from: String = ""
    var text: String = ""
    var time: Double = 0.0
    var creationTime: Long = System.currentTimeMillis()
    var showContextMenu: Boolean = true
    var showInfoCtrl: Boolean = true
    var minUserNameWidth: Int = 0
    var needsTimeBox: Boolean = true
    var objectData: Map<String, Any> = emptyMap()
    var nameStyleParams: Map<String, String> = emptyMap()

    private var insertMentionCallback: ((String) -> Unit)? = null

    fun setInsertMentionCallback(cb: (String) -> Unit) {
        insertMentionCallback = cb
    }

    fun copyUrlToClipboard() {
        // no-op
    }

    fun insertMentionAtCursor() {
        insertMentionCallback?.invoke("secondlife:///app/agent/$avatarId/mention")
    }

    fun setup(chat: LLChat, styleParams: Map<String, String>, args: Map<String, Any>) {
        avatarId = chat.fromId
        sessionId = chat.sessionId
        sourceType = chat.sourceType
        chatType = chat.chatType
        nameStyleParams = styleParams
        text = chat.text
        time = chat.time
        from = chat.fromName
    }

    fun showInspector() {
        System.err.println("FSChatHistoryHeader: showInspector not yet implemented")
    }

    fun fetchAvatarName() {
        System.err.println("FSChatHistoryHeader: fetchAvatarName not yet implemented")
    }

    fun banGroupMember(participantId: UUID) {
        System.err.println("FSChatHistoryHeader: banGroupMember not yet implemented")
    }

    fun canBanInGroup(): Boolean {
        System.err.println("FSChatHistoryHeader: canBanInGroup not yet implemented")
        return false
    }

    fun isGroupModerator(): Boolean {
        System.err.println("FSChatHistoryHeader: isGroupModerator not yet implemented")
        return false
    }
}

open class FSChatHistory(private val params: ChatHistoryParams) {

    private var lastFromName: String = ""
    private var lastFromId: UUID? = null
    private var lastMessageTime: Date? = null
    private var isLastMessageFromLog: Boolean = false
    private var scrollToBottom: Boolean = false

    private val messageHeaderFilename: String = params.messageHeader
    private val messageSeparatorFilename: String = params.messageSeparator

    private val leftTextPad: Int = params.leftTextPad
    private val rightTextPad: Int = params.rightTextPad
    private val leftWidgetPad: Int = params.leftWidgetPad
    private val rightWidgetPad: Int = params.rightWidgetPad
    private val topSeparatorPad: Int = params.topSeparatorPad
    private val bottomSeparatorPad: Int = params.bottomSeparatorPad
    private val topHeaderPad: Int = params.topHeaderPad
    private val bottomHeaderPad: Int = params.bottomHeaderPad

    private var displayName: String = ""
    private var displayNameUsername: String = ""

    private var unreadChatSources: Int = 0
    val unreadMessagesUpdateSignal: MutableList<(Int) -> Unit> = mutableListOf()

    var chatInputLine: Any? = null

    private var textContent: String = ""
    private var readOnly: Boolean = true

    fun getValue(): String = textContent

    fun initFromParams(p: ChatHistoryParams) {
        readOnly = true
    }

    open fun insertMentionAtCursor(str: String) {
        updateChatInputLine()
        // no-op
    }

    private fun updateChatInputLine() {
        // no-op
    }

    fun setFocus(b: Boolean) {
        // no-op
    }

    private fun getSeparator(): Any? {
        // no-op
        return null
    }

    private fun getHeader(chat: LLChat, styleParams: Map<String, String>, args: Map<String, Any>): FSChatHistoryHeader? {
        val header = FSChatHistoryHeader()
        header.setup(chat, styleParams, args)
        header.setInsertMentionCallback { str -> insertMentionAtCursor(str) }
        return header
    }

    open fun clear() {
        lastFromName = ""
        lastFromId = null
        // Set to single space+newline before clear so any visible scrollbar collapses
        textContent = " \n"
    }

    fun appendMessage(
        chat: LLChat,
        args: Map<String, Any> = emptyMap(),
        inputAppendParams: Map<String, String> = emptyMap()
    ) {
        // no-op
    }

    open fun draw() {
        // no-op
    }

    open fun handleUnicodeCharHere(uniChar: Int): Boolean {
        updateChatInputLine()
        // no-op
        return false
    }

    fun setUnreadMessagesUpdateCallback(cb: (Int) -> Unit): () -> Unit {
        unreadMessagesUpdateSignal.add(cb)
        return { unreadMessagesUpdateSignal.remove(cb) }
    }
}
