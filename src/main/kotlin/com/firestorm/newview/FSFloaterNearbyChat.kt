package com.firestorm.newview

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLUUID

enum class ChatType {
    NORMAL, WHISPER, SHOUT, OOC, IM, IM_GROUP, START, STOP
}

enum class ChatSourceType {
    SYSTEM, AGENT, OBJECT, UNKNOWN
}

enum class ChatStyle { NORMAL, HISTORY, IRC }

data class LLChat(
    val fromName: String = "",
    val fromId: LLUUID = LLUUID.NULL,
    val text: String = "",
    val timeStr: String = "",
    val chatType: ChatType = ChatType.NORMAL,
    val sourceType: ChatSourceType = ChatSourceType.AGENT,
    val chatStyle: ChatStyle = ChatStyle.NORMAL,
    val muted: Boolean = false,
    val fromNameGroup: String = ""
)

class FSFloaterNearbyChat(val key: LLSD) {

    private var chatHistory: Any? = null
    private var chatHistoryMuted: Any? = null
    private var inputEditor: Any? = null

    private var emojiRecentPanelToggleBtn: Any? = null
    private var emojiPickerToggleBtn: Any? = null
    private var emojiRecentPanel: Any? = null
    private var emojiRecentEmptyText: Any? = null
    private var emojiRecentIconsCtrl: Any? = null
    private var sendChatButton: Any? = null
    private var chatTypeCombo: Any? = null

    private var chatLayoutPanel: Any? = null
    private var inputPanels: Any? = null
    private var unreadMessagesNotificationPanel: Any? = null
    private var unreadMessagesNotificationTextBox: Any? = null

    private var unreadMessages: Int = 0
    private var unreadMessagesMuted: Int = 0
    private var inputEditorPad: Int = 0
    private var chatLayoutPanelHeight: Int = 0
    private var fsUseNearbyChatConsole: Boolean = false
    private var emojiHelperLastCallbackFrame: UInt = 0u

    private val messageArchive: MutableList<LLChat> = mutableListOf()

    private val recentEmojisCallbacks: MutableList<() -> Unit> = mutableListOf()
    private val rlvBehaviorCallbacks: MutableList<(String) -> Unit> = mutableListOf()

    fun postBuild(): Boolean {
        TODO(
            "Wire up: inputEditor (autoreplace, commit, keystroke, focusLost, focusReceived, textExpanded callbacks); " +
            "RLV behavior toggle; emoji buttons; chat history + muted history with unread callbacks; " +
            "chatTypeCombo, sendChatButton; FSUseNearbyChatConsole + FSShowMutedChatHistory settings listeners"
        )
    }

    fun onOpen(key: LLSD) {
        TODO(
            "Handle ChatHistoryTornOff tear-off; add floater to IM container at START position; " +
            "show container and floater if isChatMultiTab and not yet visible"
        )
    }

    fun setVisible(visible: Boolean) {
        if (visible) removeScreenChat()
        TODO(
            "Call super.setVisible(visible); compute is_minimized; " +
            "focus inputEditor if active tab; manage gConsole session"
        )
    }

    fun setMinimized(b: Boolean) {
        handleMinimized(b)
        TODO("Call super.setMinimized(b)")
    }

    fun openFloater(key: LLSD) {
        if (isChatMultiTab()) {
            TODO("Show IM container at START position; setVisible(true); call super.openFloater(key)")
        }
    }

    fun addMessage(chat: LLChat, archive: Boolean = true, args: LLSD = LLSD()) {
        val showTimestamps = true
        val useOnePlainText = false

        val chatArgs = args.copy()
        chatArgs["use_plain_text_chat_history"] = useOnePlainText
        chatArgs["show_time"] = showTimestamps
        chatArgs["is_local"] = true

        TODO("Append to chatHistoryMuted; if !chat.muted append to chatHistory")

        if (archive) {
            messageArchive.add(chat)
            if (messageArchive.size > 200) messageArchive.removeAt(0)
        }

        if (args["do_not_log"].asBoolean() || chat.muted) return

        if (isChatMultiTab()) {
            TODO("If not in visible chain and source is agent/object: flash container tab")
        }

        TODO("If LogNearbyChat: resolve from_name; strip RLV names; handle IM prefix for log; check antispam; LLLogChat.saveHistory")
    }

    fun clearChatHistory() {
        TODO("Call clear() on chatHistory and chatHistoryMuted")
    }

    fun updateChatHistoryStyle() {
        clearChatHistory()
        val doNotLog = LLSD()
        doNotLog["do_not_log"] = true
        messageArchive.forEach { addMessage(it, false, doNotLog) }
    }

    fun loadHistory() {
        val doNotLog = LLSD()
        doNotLog["do_not_log"] = true
        TODO(
            "Load chat history via LLLogChat.loadChatHistory(\"chat\"); " +
            "parse each entry resolving IM prefix and group IM prefix; " +
            "determine sourceType (system, agent, object, unknown); call addMessage"
        )
    }

    fun reloadMessages(cleanMessages: Boolean = false) {
        if (cleanMessages) {
            messageArchive.clear()
            loadHistory()
        }
        clearChatHistory()
        val doNotLog = LLSD()
        doNotLog["do_not_log"] = true
        messageArchive.forEach { addMessage(it, false, doNotLog) }
    }

    fun removeScreenChat() {
        TODO("Find NEARBY_CHAT_CHANNEL_UUID screen channel via LLChannelManager; removeToastsFromChannel()")
    }

    fun getVisible(): Boolean {
        TODO(
            "If torn off from container: call super.getVisible(). " +
            "Otherwise: return is_active && !container.isMinimized && container.getVisible()"
        )
    }

    fun focusFirstItem(preferTextFields: Boolean = false, focusFocus: Boolean = true): Boolean {
        TODO("Set focus on inputEditor; call onTabInto(); optionally triggerFocusFlash(); return true")
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        val maskControl = 0x01
        val maskShift   = 0x02
        val maskAlt     = 0x04
        val keyReturn   = 0x0D
        if (key == keyReturn) {
            return when (mask) {
                maskControl                  -> { TODO("If FSUseCtrlShout: updateHistory(); sendChat(SHOUT); true") }
                maskShift                    -> { TODO("If FSUseShiftWhisper: updateHistory(); sendChat(WHISPER); true") }
                maskAlt                      -> { TODO("If FSUseAltOOC: updateHistory(); sendChat(OOC); true") }
                maskShift or maskControl     -> { TODO("Insert linefeed or paragraph symbol per FSUseSingleLineChatEntry; true") }
                else                         -> false
            }
        }
        return false
    }

    fun updateUnreadMessageNotification(unreadCount: Int, mutedHistory: Boolean) {
        val showMutedHistory = false
        if (mutedHistory) {
            unreadMessagesMuted = unreadCount
            if (!showMutedHistory) return
        } else {
            unreadMessages = unreadCount
            if (showMutedHistory) return
        }
        TODO("Show/hide unreadMessagesNotificationPanel; update label with [NUM]=unreadCount if FSNotifyUnreadChatMessages")
    }

    fun updateShowMutedChatHistory(data: LLSD) {
        val showMuted = data.asBoolean()
        updateUnreadMessageNotification(if (showMuted) unreadMessagesMuted else unreadMessages, showMuted)
    }

    fun handleMinimized(minimized: Boolean) {
        if (minimized) TODO("gConsole.removeSession(LLUUID.null)")
        else           TODO("gConsole.addSession(LLUUID.null)")
    }

    fun updateFsUseNearbyChatConsole(data: LLSD) {
        fsUseNearbyChatConsole = data.asBoolean()
        if (fsUseNearbyChatConsole) {
            removeScreenChat()
            TODO("gConsole.setVisible(true)")
        } else {
            TODO("gConsole.setVisible(false)")
        }
    }

    fun updateRlvRestrictions(behavior: String) {
        if (behavior != "shownames") return
        setChatMentionPickerEnabled(true)
        TODO("setChatMentionPickerEnabled(!RlvActions.isRlvEnabled || RlvActions.canShowName(SNC_DEFAULT))")
    }

    fun setChatMentionPickerEnabled(enabled: Boolean) {
        TODO("If inputEditor non-null: inputEditor.setShowChatMentionPicker(enabled)")
    }

    fun getMessageArchiveLength(): Int = messageArchive.size

    fun getSessionParticipants(): List<LLUUID> {
        TODO("If agent valid and world/LFSimFeatureHandler exist: return LLWorld.getAvatars in sayRange around agent position")
    }

    fun onGetChatBoxOpacityCallback(type: Int, alpha: Float): Float {
        val ttActive = 0
        val imOpacity = 1.0f
        return if (type != ttActive) minOf(imOpacity, alpha) else alpha
    }

    fun onHistoryButtonClicked() {
        TODO(
            "If FSUseBuiltInHistory: show preview_conversation floater. " +
            "Else: gViewerWindow.openFile(LLLogChat.makeLogFileName(\"chat\"))"
        )
    }

    fun onSearchButtonClicked() {
        TODO("Show LLFloaterSearchReplace for whichever chat history panel is currently visible")
    }

    protected fun onChatBoxKeystroke() {
        TODO("Read channel from ChatChannel spinner if FSNearbyChatbar+FSShowChatChannel; call FSNearbyChat.handleChatBarKeystroke")
    }

    protected fun onChatBoxFocusLost() {
        TODO("gAgent.stopTyping()")
    }

    protected fun onChatBoxFocusReceived() {
        TODO("inputEditor.setEnabled(!gDisconnected && FSNearbyChatbar setting)")
    }

    protected fun onChatBoxCommit() {
        TODO("If inputEditor text non-empty: determine type from chatTypeCombo; sendChat(type); gAgent.stopTyping()")
    }

    protected fun onChatTypeChanged() {
        TODO("sendChatButton.setLabel(chatTypeCombo.getSelectedItemLabel())")
    }

    protected fun reshapeChatLayoutPanel() {
        TODO("chatLayoutPanel.reshape(width, inputEditor.height + inputEditorPad, false)")
    }

    protected fun sendChat(type: ChatType) {
        TODO(
            "Get converted text from inputEditor; trim; convert paragraph symbols to newlines; " +
            "handle OOC prefix/postfix; strip channel number; trigger gesture or send via FSNearbyChat; " +
            "clear input; gAgent.stopTyping(); handle CloseChatOnReturn"
        )
    }

    protected fun sendChatFromViewer(utf8text: String, type: ChatType, animate: Boolean) {
        TODO("Convert to WString; strip channel; call FSNearbyChat.sendChatFromViewer")
    }

    private fun onChatOptionsContextMenuItemClicked(userdata: LLSD) {
        TODO("FSChatOptionsMenu.onMenuItemClick(userdata, this)")
    }
    private fun onChatOptionsCheckContextMenuItem(userdata: LLSD): Boolean =
        TODO("FSChatOptionsMenu.onMenuItemCheck(userdata, this)")
    private fun onChatOptionsVisibleContextMenuItem(userdata: LLSD): Boolean =
        TODO("FSChatOptionsMenu.onMenuItemVisible(userdata, this)")
    private fun onChatOptionsEnableContextMenuItem(userdata: LLSD): Boolean =
        TODO("FSChatOptionsMenu.onMenuItemEnable(userdata, this)")

    private fun onEmojiRecentPanelToggleBtnClicked() {
        TODO("Toggle emojiRecentPanel visibility; call initEmojiRecentPanel if showing; update button overlay image; focus inputEditor")
    }
    private fun onEmojiPickerToggleBtnClicked() {
        TODO("Toggle emoji picker toggle state; show or hide emoji helper on inputEditor")
    }
    private fun onEmojiPickerToggleBtnDown() {
        TODO("If emojiHelperLastCallbackFrame == current frame: restore toggle state to true to prevent re-open")
    }
    private fun onEmojiPickerClosed() {
        TODO("Clear toggle state; record emojiHelperLastCallbackFrame = LLFrameTimer.getFrameCount()")
    }
    private fun initEmojiRecentPanel() {
        TODO("Fetch LLFloaterEmojiPicker.getRecentlyUsed(); show empty text or icons control accordingly")
    }
    private fun onRecentEmojiPicked(value: LLSD) {
        TODO("Parse emoji character from value string; call inputEditor.insertEmoji(emoji)")
    }

    private fun onFocusLost() {
        TODO("LLFloaterChatMentionPicker.removeParticipantSource(this); super.onFocusLost()")
    }
    private fun onFocusReceived() {
        TODO("LLFloaterChatMentionPicker.updateParticipantSource(this); super.onFocusReceived()")
    }

    companion object {
        private var lastSpecialChatChannel: Int = 0

        @Volatile private var instance: FSFloaterNearbyChat? = null

        fun findInstance(): FSFloaterNearbyChat? = instance

        fun getInstance(): FSFloaterNearbyChat =
            instance ?: synchronized(this) {
                instance ?: FSFloaterNearbyChat(LLSD()).also { instance = it }
            }

        fun isChatMultiTab(): Boolean {
            TODO("Read FSChatWindow setting (cached); return true if == 1")
        }

        fun stopChat() {
            findInstance()?.let {
                TODO("inputEditor.setFocus(false); gAgent.stopTyping()")
            }
        }

        fun processChatHistoryStyleUpdate(newvalue: LLSD) {
            val nearbyChat = getInstance()
            nearbyChat.updateChatHistoryStyle()
            TODO("Reset inputEditor font and re-set current text to force style refresh")
        }

        fun isWordsName(name: String): Boolean {
            val openParen = name.indexOf(" (")
            val closeParen = name.indexOf(')')
            if (openParen != -1 && closeParen == name.length - 1) return true
            val pos = name.indexOf(' ')
            return pos != -1 && name.lastIndexOf(' ') == pos && pos != 0 && pos != name.length - 1
        }
    }
}

private fun LLSD.asBoolean(): Boolean = TODO("APR: convert LLSD to Boolean")
private fun LLSD.copy(): LLSD = TODO("APR: shallow-copy LLSD")
private operator fun LLSD.set(key: String, value: Any?) { TODO("APR: set LLSD key") }
private operator fun LLSD.get(key: String): LLSD = TODO("APR: get LLSD key")
