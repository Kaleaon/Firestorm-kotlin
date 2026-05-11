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
        TODO("GPU: use JVM clipboard to copy secondlife:///app/agent/$avatarId/mention")
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
        TODO("APR: use JVM equivalent for SL inspector URL launch")
    }

    fun fetchAvatarName() {
        TODO("APR: use JVM equivalent for avatar name cache lookup")
    }

    fun banGroupMember(participantId: UUID) {
        TODO("APR: use JVM equivalent for group ban request")
    }

    fun canBanInGroup(): Boolean {
        TODO("APR: use JVM equivalent for group permission check")
    }

    fun isGroupModerator(): Boolean {
        TODO("APR: use JVM equivalent for group moderator check")
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
        TODO("GPU: delegate mention insert to chat input line control")
    }

    private fun updateChatInputLine() {
        TODO("GPU: walk focus root to find LLChatEntry equivalent")
    }

    fun setFocus(b: Boolean) {
        TODO("GPU: IME / SDL2 language input positioning")
    }

    private fun getSeparator(): Any? {
        TODO("GPU: build separator widget from $messageSeparatorFilename")
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
        TODO("GPU: full rich-text message append pipeline: " +
            "UI text rendering, color lookup, font selection, widget embedding, " +
            "moderator style, IRC-me handling, and notification panel embedding")
    }

    open fun draw() {
        TODO("GPU: draw text editor contents; handle scroll-to-bottom and unread-count reset")
    }

    open fun handleUnicodeCharHere(uniChar: Int): Boolean {
        updateChatInputLine()
        TODO("GPU: if no ctrl mask, redirect keystroke to chat input line; else let base class handle")
    }

    fun setUnreadMessagesUpdateCallback(cb: (Int) -> Unit): () -> Unit {
        unreadMessagesUpdateSignal.add(cb)
        return { unreadMessagesUpdateSignal.remove(cb) }
    }
}
