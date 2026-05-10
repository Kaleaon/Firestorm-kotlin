package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Chat-source and chat-type enums  (mirrors EChatSourceType / EChatType)
// ---------------------------------------------------------------------------

enum class ChatSourceType {
    UNKNOWN, AGENT, OBJECT, SYSTEM, REGION, TELEPORT
}

enum class ChatType {
    NORMAL, IM, IM_GROUP, WHISPER, SHOUT, RADAR, DEBUG, OWNER, DIRECT, ESTATE,
    REGION_SAY
}

enum class ChatStyle {
    NORMAL, IRC, HISTORY, SERVER_HISTORY, TELEPORT_SEP, MODERATOR
}

// ---------------------------------------------------------------------------
// LLChat data class  (mirrors the C++ struct LLChat passed to appendMessage)
// ---------------------------------------------------------------------------

data class LLChat(
    val fromName: String = "",
    val fromId: UUID = UUID(0L, 0L),
    val ownerId: UUID = UUID(0L, 0L),
    val sessionId: UUID = UUID(0L, 0L),
    val text: String = "",
    val url: String = "",
    val timeStr: String = "",
    val time: Double = 0.0,
    val posAgent: FloatArray = FloatArray(3),
    val sourceType: ChatSourceType = ChatSourceType.SYSTEM,
    val chatType: ChatType = ChatType.NORMAL,
    val chatStyle: ChatStyle = ChatStyle.NORMAL,
    val fromNameGroup: String = "",
    val notifId: UUID = UUID(0L, 0L),
    val rlvNamesFiltered: Boolean = false
)

// ---------------------------------------------------------------------------
// Moderator-style enum  (mirrors e_moderation_options in fschathistory.cpp)
// ---------------------------------------------------------------------------

private enum class ModeratorStyle(val code: Int) {
    NORMAL(0), BOLD(1), ITALIC(2), BOLD_ITALIC(3),
    UNDERLINE(4), BOLD_UNDERLINE(5), ITALIC_UNDERLINE(6), BOLD_ITALIC_UNDERLINE(7);

    fun toStyleString(): String = when (this) {
        NORMAL            -> "NORMAL"
        BOLD              -> "BOLD"
        ITALIC            -> "ITALIC"
        BOLD_ITALIC       -> "BOLDITALIC"
        UNDERLINE         -> "UNDERLINE"
        BOLD_UNDERLINE    -> "BOLDUNDERLINE"
        ITALIC_UNDERLINE  -> "ITALICUNDERLINE"
        BOLD_ITALIC_UNDERLINE -> "BOLDITALICUNDERLINE"
    }

    companion object {
        fun fromCode(code: Int): ModeratorStyle = entries.firstOrNull { it.code == code } ?: NORMAL
    }
}

// ---------------------------------------------------------------------------
// Unread-messages signal callback type
// (boost::signals2::signal<void(S32)> → MutableList<(Int) -> Unit>)
// ---------------------------------------------------------------------------

typealias UnreadMessagesCallback = (unreadCount: Int) -> Unit

// ---------------------------------------------------------------------------
// FSChatHistoryHeader  (inner class rendered as a panel row in widget mode)
// ---------------------------------------------------------------------------

open class FSChatHistoryHeader {

    private var avatarId: UUID = UUID(0L, 0L)
    private var sessionId: UUID = UUID(0L, 0L)
    private var sourceType: ChatSourceType = ChatSourceType.UNKNOWN
    private var chatType: ChatType = ChatType.NORMAL
    private var from: String = ""
    private var text: String = ""
    private var time: Double = 0.0
    private var objectData: MutableMap<String, Any> = mutableMapOf()
    private var showContextMenu: Boolean = true
    private var showInfoCtrl: Boolean = true
    private var needsTimeBox: Boolean = true
    private var minUserNameWidth: Int = 0

    // nullable lambda slot = boost::signals2::connection
    private var avatarNameCacheConnection: (() -> Unit)? = null
    private var insertMentionCallback: ((speakerId: String) -> Unit)? = null

    fun getAvatarId(): UUID = avatarId

    fun setInsertMentionCallback(cb: (speakerId: String) -> Unit) {
        insertMentionCallback = cb
    }

    fun copyUrlToClipboard() {
        TODO("GPU: LLUrlAction.copyURLToClipboard(\"secondlife:///app/agent/$avatarId/mention\")")
    }

    fun insertMentionAtCursor() {
        insertMentionCallback?.invoke("secondlife:///app/agent/$avatarId/mention")
    }

    fun setup(chat: LLChat, styleParams: Any, args: Map<String, Any>) {
        avatarId   = chat.fromId
        sessionId  = chat.sessionId
        sourceType = chat.sourceType
        chatType   = chat.chatType
        text       = chat.text
        time       = chat.time

        if ((chat.fromId == UUID(0L, 0L) && chat.fromName.isEmpty()) ||
            (chat.fromName == "Second Life" && chat.fromId == UUID(0L, 0L))) {
            sourceType = ChatSourceType.SYSTEM
        }

        if (chat.rlvNamesFiltered &&
            (sourceType == ChatSourceType.AGENT ||
             (sourceType == ChatSourceType.SYSTEM && chatType == ChatType.RADAR) ||
             sourceType == ChatSourceType.OBJECT)) {
            showInfoCtrl = false
            showContextMenu = false
        }

        when {
            sourceType == ChatSourceType.TELEPORT && chat.chatStyle == ChatStyle.TELEPORT_SEP -> {
                from = chat.fromName
                needsTimeBox = false
                TODO("GPU: set mUserNameTextBox value to from, hide mTimeBoxTextBox, set teleport separator colour")
            }
            (chat.fromName.isEmpty() ||
             (sourceType == ChatSourceType.SYSTEM && chatType != ChatType.RADAR) ||
             chat.fromId == UUID(0L, 0L)) -> {
                from = if (sourceType == ChatSourceType.UNKNOWN) {
                    chat.fromName
                } else {
                    TODO("GPU: LLTrans.getString(\"SECOND_LIFE\") + optional (fromName) suffix") as String
                }
                TODO("GPU: set mUserNameTextBox value to from")
            }
            (sourceType == ChatSourceType.AGENT ||
             (sourceType == ChatSourceType.SYSTEM && chatType == ChatType.RADAR)) &&
             chat.fromId != UUID(0L, 0L) &&
             chat.chatStyle != ChatStyle.SERVER_HISTORY &&
             chat.chatStyle != ChatStyle.HISTORY &&
             !chat.rlvNamesFiltered -> {
                TODO("GPU: clear mUserNameTextBox and call fetchAvatarName()")
            }
            else -> {
                from = chat.fromName
                TODO("GPU: set mUserNameTextBox value to from")
            }
        }

        TODO("GPU: call setTimeField(chat); configure avatar_icon child based on sourceType/rlvNamesFiltered")

        if (chat.sourceType == ChatSourceType.OBJECT) {
            val slurl = (args["slurl"] as? String)?.takeIf { it.isNotEmpty() }
                ?: TODO("GPU: derive SLURL from LLWorld.getRegionFromPosAgent(chat.posAgent)") as String
            objectData["object_id"] = chat.fromId
            objectData["name"]      = chat.fromName
            objectData["owner_id"]  = chat.ownerId
            objectData["slurl"]     = slurl
        }
    }

    open fun postBuild(): Boolean {
        TODO("GPU: bind mUserNameTextBox, mTimeBoxTextBox, mHeaderLayoutStack, mInfoCtrl; register double-click, mouse-enter, mouse-leave callbacks")
    }

    open fun draw() {
        TODO("GPU: show mTimeBoxTextBox when user-name box is wide enough; call LLPanel.draw()")
    }

    private fun showInspector() {
        if (!showInfoCtrl) return
        when (sourceType) {
            ChatSourceType.OBJECT -> TODO("GPU: LLFloaterReg.showInstance(\"inspect_remote_object\", objectData)")
            ChatSourceType.AGENT,
            ChatSourceType.SYSTEM -> if (chatType == ChatType.RADAR) {
                TODO("GPU: LLUrlAction.executeSLURL(LLSLURL(\"agent\", avatarId, \"inspect\").getSLURLString())")
            }
            else -> Unit
        }
    }

    private fun showInfoCtrl() {
        val isVisible = showInfoCtrl && avatarId != UUID(0L, 0L) && from.isNotEmpty() &&
            (sourceType != ChatSourceType.SYSTEM || chatType == ChatType.RADAR) &&
            sourceType != ChatSourceType.REGION
        TODO("GPU: position and show/hide mInfoCtrl based on isVisible and mUserNameTextBox geometry")
    }

    private fun hideInfoCtrl() {
        TODO("GPU: mInfoCtrl.setVisible(false)")
    }

    private fun fetchAvatarName() {
        if (avatarId == UUID(0L, 0L)) return
        avatarNameCacheConnection?.invoke()
        avatarNameCacheConnection = TODO("APR: use JVM equivalent - LLAvatarNameCache.get(avatarId, ::onAvatarNameCache)") as () -> Unit
    }

    private fun onAvatarNameCache(agentId: UUID, avName: AvatarName) {
        avatarNameCacheConnection = null
        from = avName.displayName
        TODO("GPU: update mUserNameTextBox with displayName; optionally append \" - username\" in emphasis colour")
        TODO("GPU: update updateMinUserNameWidth()")
    }

    private fun showContextMenu(x: Int, y: Int) {
        if (!showContextMenu) return
        when {
            sourceType == ChatSourceType.SYSTEM && chatType == ChatType.RADAR -> showAvatarContextMenu(x, y)
            sourceType == ChatSourceType.SYSTEM -> showSystemContextMenu(x, y)
            avatarId != UUID(0L, 0L) && sourceType == ChatSourceType.AGENT -> showAvatarContextMenu(x, y)
            avatarId != UUID(0L, 0L) && sourceType == ChatSourceType.OBJECT -> showObjectContextMenu(x, y)
        }
    }

    private fun showSystemContextMenu(x: Int, y: Int) { /* no-op: system messages have no context menu */ }

    private fun showObjectContextMenu(x: Int, y: Int) {
        TODO("GPU: create or show menu_object_icon.xml popup menu at (x,y) with ObjectIcon.Action / ObjectIcon.Visible callbacks")
    }

    private fun showAvatarContextMenu(x: Int, y: Int) {
        TODO("GPU: create or show menu_avatar_icon.xml popup menu at (x,y); enable/disable items based on friend status, group, RLVa, etc.")
    }

    private fun onObjectIconContextMenuItemClicked(userdata: Any) {
        TODO("GPU: dispatch object context menu actions: profile, block, unblock, map, teleport, zoom")
    }

    private fun onAvatarIconContextMenuItemClicked(userdata: Any) {
        TODO("GPU: dispatch avatar context menu actions: profile, im, teleport, voice, add/remove friend, zoom, track, share, pay, mute, ban, etc.")
    }

    private fun onAvatarIconContextMenuItemChecked(userdata: Any): Boolean =
        TODO("GPU: return checked state for is_blocked / is_muted / is_allowed_text_chat")

    private fun onAvatarIconContextMenuItemEnabled(userdata: Any): Boolean =
        TODO("GPU: return enabled state for can_mute / can_unmute / report_abuse / can_pay / can_ban_member")

    private fun onAvatarIconContextMenuItemVisible(userdata: Any): Boolean =
        TODO("GPU: return visible state for show_mute / show_unmute")

    companion object {
        fun createInstance(fileName: String): FSChatHistoryHeader {
            val header = FSChatHistoryHeader()
            TODO("GPU: buildFromFile(fileName) to inflate the header panel UI")
            @Suppress("UNREACHABLE_CODE")
            return header
        }
    }
}

// ---------------------------------------------------------------------------
// FSChatHistory
// LLTextEditor subclass → open class; virtual methods → open
// ---------------------------------------------------------------------------

open class FSChatHistory(
    val messageHeaderFilename: String = "",
    val messageSeparatorFilename: String = "",
    val leftTextPad: Int = 0,
    val rightTextPad: Int = 0,
    val leftWidgetPad: Int = 0,
    val rightWidgetPad: Int = 0,
    val topSeparatorPad: Int = 0,
    val bottomSeparatorPad: Int = 0,
    val topHeaderPad: Int = 0,
    val bottomHeaderPad: Int = 0
) {

    private var lastFromName: String = ""
    private var lastFromId: UUID = UUID(0L, 0L)
    private var lastMessageTime: Long = 0L
    private var isLastMessageFromLog: Boolean = false
    private var scrollToBottom: Boolean = false

    private var displayName: String = ""
    private var displayNameUsername: String = ""

    private var unreadChatSources: Int = 0

    // MutableList<(Int) -> Unit> represents boost::signals2::signal<void(S32)>
    private val unreadMessagesUpdateSignal: MutableList<UnreadMessagesCallback> = mutableListOf()

    // FIRE-8602: reference to the chat-input LLChatEntry below the history widget
    var chatInputLine: Any? = null  // LLChatEntry

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    init {
        TODO("GPU: set mLineSpacingPixels from FSFontChatLineSpacingPixels setting; set mTextVAlign=VCENTER; mUseColor=true; register isObjectBlocked/isObjectReachable callbacks")
    }

    fun initFromParams() {
        TODO("GPU: call LLTextEditor.initFromParams; then setEnabled(false), setReadOnly(true), setShowContextMenu(true)")
    }

    open fun getValue(): Any {
        TODO("GPU: return LLSD(getText())")
    }

    // -----------------------------------------------------------------------
    // Chat-input-line focus integration  (FIRE-8602)
    // -----------------------------------------------------------------------

    private fun updateChatInputLine() {
        if (chatInputLine != null) return
        TODO("GPU: find root-most focus root; focusNextItem(true); cast gFocusMgr.getKeyboardFocus() to LLChatEntry and store in chatInputLine")
    }

    // SDL2 IME: ensure IME window is positioned over the chat-input line
    open fun setFocus(hasFocus: Boolean) {
        TODO("GPU: LLTextEditor.setFocus(hasFocus); if SDL2IMEEnabled and hasFocus, position IME window over chatInputLine rect")
    }

    // -----------------------------------------------------------------------
    // Mention insertion  (FIRE-8602)
    // -----------------------------------------------------------------------

    open fun insertMentionAtCursor(str: String) {
        updateChatInputLine()
        val inputLine = chatInputLine ?: return
        TODO("GPU: inputLine.insertMentionAtCursor(str); inputLine.setFocus(true)")
    }

    // -----------------------------------------------------------------------
    // LLTextEditor overrides
    // -----------------------------------------------------------------------

    open fun clear() {
        lastFromName = ""
        lastFromId = UUID(0L, 0L)
        TODO("GPU: setText(\" \\n\") then LLTextEditor.clear() to reset scrollbar state")
    }

    open fun draw() {
        TODO("GPU: call LLTextEditor.draw()")
        if (scrollToBottom) {
            TODO("GPU: mScroller.goToBottom()")
            scrollToBottom = false
        }
        TODO("GPU: if scrolledToEnd() and unreadChatSources != 0, reset unreadChatSources and fire unreadMessagesUpdateSignal(0)")
    }

    open fun handleUnicodeCharHere(uniChar: Int): Boolean {
        TODO("GPU: if CTRL key is held delegate to LLTextEditor.handleUnicodeCharHere; otherwise updateChatInputLine, forward keystroke to chatInputLine if available")
    }

    // -----------------------------------------------------------------------
    // Header / separator factories
    // -----------------------------------------------------------------------

    protected fun getSeparator(): Any? {
        TODO("GPU: LLUICtrlFactory.getInstance().createFromFile<LLPanel>(messageSeparatorFilename, null, LLPanel.child_registry_t.instance())")
    }

    protected fun getHeader(chat: LLChat, styleParams: Any, args: Map<String, Any>): Any? {
        val header = FSChatHistoryHeader.createInstance(messageHeaderFilename)
        header.setup(chat, styleParams, args)
        header.setInsertMentionCallback { str -> insertMentionAtCursor(str) }
        return header
    }

    // -----------------------------------------------------------------------
    // appendMessage  (the main entry point used by all chat sources)
    // -----------------------------------------------------------------------

    open fun appendMessage(
        chat: LLChat,
        args: Map<String, Any> = emptyMap(),
        inputAppendParams: Any? = null
    ) {
        scrollToBottom = TODO("GPU: mScroller.isAtBottom() || mScroller.getScrollbar(VERTICAL).getDocPosMax() <= 1") as Boolean

        val usePlainText = args["use_plain_text_chat_history"] as? Boolean ?: false
        val isP2p = args["is_p2p"] as? Boolean ?: false
        val isConversationLog = args["conversation_log"] as? Boolean ?: false
        val isLocal = args["is_local"] as? Boolean ?: false
        val fromMe = TODO("GPU: chat.fromId == gAgent.getID()") as Boolean

        TODO("GPU: setPlainText(usePlainText)")

        if (!TODO("GPU: scrolledToEnd()") as Boolean && !fromMe && chat.fromName.isNotEmpty()) {
            unreadChatSources++
            unreadMessagesUpdateSignal.forEach { it(unreadChatSources) }
        }

        TODO("GPU: resolve txt_color, name_color, alpha via LLViewerChat.getChatColor / getChatFont; build body_message_params and name_params")

        val ircMe = FSCommon.isIrcMePrefix(chat.text)

        var delimiter = ": "
        val shoutStr = TODO("GPU: LLTrans.getString(\"shout\")") as String
        val whisperStr = TODO("GPU: LLTrans.getString(\"whisper\")") as String
        if (chat.chatType == ChatType.SHOUT || chat.chatType == ChatType.WHISPER ||
            chat.chatType == ChatType.RADAR ||
            chat.text.startsWith(shoutStr) || chat.text.startsWith(whisperStr)) {
            delimiter = " "
        }
        if (ircMe || chat.chatStyle == ChatStyle.IRC) {
            delimiter = ""
            TODO("GPU: if EmotesUseItalic setting is true, set ITALIC body/name font style")
        }
        if (chat.chatType == ChatType.WHISPER) TODO("GPU: conditionally apply ITALIC style if FSEmphasizeShoutWhisper")
        if (chat.chatType == ChatType.SHOUT)   TODO("GPU: conditionally apply BOLD style if FSEmphasizeShoutWhisper")

        val messageFromLog  = chat.chatStyle == ChatStyle.HISTORY || chat.chatStyle == ChatStyle.SERVER_HISTORY
        val teleportSep     = chat.sourceType == ChatSourceType.TELEPORT

        if (messageFromLog && !isConversationLog) {
            TODO("GPU: set txt_color to ChatHistoryMessageFromLog or ChatHistoryMessageFromServerLog colour")
        }

        val moderatorNameStyleValue = TODO("GPU: gSavedSettings.getU32(\"FSModNameStyle\")") as Int
        val moderatorBodyStyleValue = TODO("GPU: gSavedSettings.getU32(\"FSModTextStyle\")") as Int
        val moderatorStyleActive    = chat.chatStyle == ChatStyle.MODERATOR
        if (moderatorStyleActive) {
            TODO("GPU: apply moderator font styles to name_params and body_message_params; handle EmotesUseItalic interaction")
        }

        val prependNewLine = TODO("GPU: getText().isNotEmpty()") as Boolean

        if (usePlainText) {
            // Compact mode: timestamp + name + message on one line
            val squareBrackets = chat.sourceType == ChatSourceType.SYSTEM &&
                chat.chatType != ChatType.RADAR &&
                TODO("GPU: gSavedSettings.getBOOL(\"FSIMSystemMessageBrackets\")") as Boolean

            if (args["show_time"] as? Boolean == true && !teleportSep) {
                TODO("GPU: appendText(\"[\" + chat.timeStr + \"] \", prependNewLine, timestamp_style)")
            }
            if (squareBrackets) TODO("GPU: appendText(\"[\", prependNewLine, body_message_params)")

            val trimmedName = chat.fromName.trim()
            if ((!isP2p || args["show_names_for_p2p_conv"] as? Boolean == true) && trimmedName.isNotEmpty()) {
                when {
                    chat.sourceType == ChatSourceType.OBJECT && chat.fromId != UUID(0L, 0L) ->
                        TODO("GPU: build objectim SLURL, append linked fromName + delimiter")
                    chat.fromName != "Second Life" && chat.fromId != UUID(0L, 0L) &&
                            !messageFromLog && chat.sourceType != ChatSourceType.REGION &&
                            !chat.rlvNamesFiltered ->
                        TODO("GPU: append IM-prefix if applicable, then SLURL-linked name + delimiter")
                    teleportSep ->
                        TODO("GPU: appendText teleport preamble + nolink fromName")
                    else ->
                        TODO("GPU: appendText nolink fromName + delimiter")
                }
            }
        } else {
            // Widget mode: header or separator row followed by message body
            val newMessageTime = System.currentTimeMillis()
            val sameSource = !teleportSep &&
                lastFromName == chat.fromName &&
                lastFromId == chat.fromId &&
                lastMessageTime != 0L &&
                (newMessageTime - lastMessageTime) < 60_000L &&
                isLastMessageFromLog == messageFromLog

            val view = if (sameSource) {
                getSeparator() ?: return // log warning and bail
            } else {
                getHeader(chat, TODO("GPU: name_params") as Any, args) ?: return
            }

            TODO("GPU: reshape and position view; call appendWidget with top/bottom padding from header/separator fields")

            if (!sameSource) {
                lastFromName = chat.fromName
                lastFromId   = chat.fromId
                lastMessageTime = newMessageTime
                isLastMessageFromLog = messageFromLog
            }
        }

        // Notification panels (friendship offers, etc.)
        if (chat.notifId != UUID(0L, 0L)) {
            TODO("GPU: find notification by notifId; if OfferFriendship and no existing panel, create LLIMToastNotifyPanel and appendWidget")
        } else if (!teleportSep) {
            // Regular message body
            var message = if (ircMe) chat.text.substring(3) else chat.text
            if (usePlainText && !fromMe && chat.fromId != UUID(0L, 0L)) {
                val slUrlAbout = "secondlife:///app/agent/${chat.fromId}/about"
                if (message.startsWith(slUrlAbout)) message = message.substring(slUrlAbout.length)
            }
            if (ircMe && !usePlainText && chat.sourceType == ChatSourceType.AGENT) {
                TODO("GPU: prepend agent SLURL (completename or displayname format, respecting RLVa @shownames) to message")
            } else if (ircMe && !usePlainText) {
                message = chat.fromName + message
            }
            if (chat.sourceType != ChatSourceType.OBJECT &&
                (chat.chatType == ChatType.IM || chat.chatType == ChatType.IM_GROUP)) {
                TODO("GPU: set body_message_params colour alpha to FSIMChatHistoryFade")
            }
            if (TODO("GPU: squareBrackets") as Boolean) message += "]"

            TODO("GPU: setContentTrusted(chat.fromId.isNull() && isP2p); appendText(message, prependNewLine, body_message_params); restore trusted state")
        }

        TODO("GPU: blockUndo()")
        if (fromMe) TODO("GPU: setCursorAndScrollToEnd()")
    }

    // -----------------------------------------------------------------------
    // Unread-message signal registration  (mirrors setUnreadMessagesUpdateCallback)
    // -----------------------------------------------------------------------

    fun setUnreadMessagesUpdateCallback(cb: UnreadMessagesCallback): UnreadMessagesCallback {
        unreadMessagesUpdateSignal.add(cb)
        return cb
    }

    fun removeUnreadMessagesUpdateCallback(cb: UnreadMessagesCallback) {
        unreadMessagesUpdateSignal.remove(cb)
    }
}
