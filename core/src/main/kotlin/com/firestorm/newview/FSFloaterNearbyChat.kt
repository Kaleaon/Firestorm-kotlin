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
        System.err.println("FSFloaterNearbyChat: postBuild not yet implemented")
        return false
    }

    fun onOpen(key: LLSD) {
        System.err.println("FSFloaterNearbyChat: onOpen not yet implemented")
    }

    fun setVisible(visible: Boolean) {
        if (visible) removeScreenChat()
        System.err.println("FSFloaterNearbyChat: setVisible not yet implemented")
    }

    fun setMinimized(b: Boolean) {
        handleMinimized(b)
        System.err.println("FSFloaterNearbyChat: setMinimized not yet implemented")
    }

    fun openFloater(key: LLSD) {
        if (isChatMultiTab()) {
            System.err.println("FSFloaterNearbyChat: openFloater not yet implemented")
        }
    }

    fun addMessage(chat: LLChat, archive: Boolean = true, args: LLSD = LLSD.Undefined) {
        System.err.println("FSFloaterNearbyChat: addMessage not yet implemented")
    }

    fun clearChatHistory() {
        System.err.println("FSFloaterNearbyChat: clearChatHistory not yet implemented")
    }

    fun updateChatHistoryStyle() {
        clearChatHistory()
        val doNotLog = LLSD.LLSDMap(mapOf("do_not_log" to LLSD.LLSDBoolean(true)))
        messageArchive.forEach { addMessage(it, false, doNotLog) }
    }

    fun loadHistory() {
        val doNotLog = LLSD.LLSDMap(mapOf("do_not_log" to LLSD.LLSDBoolean(true)))
        System.err.println("FSFloaterNearbyChat: loadHistory not yet implemented")
    }

    fun reloadMessages(cleanMessages: Boolean = false) {
        if (cleanMessages) {
            messageArchive.clear()
            loadHistory()
        }
        clearChatHistory()
        val doNotLog = LLSD.LLSDMap(mapOf("do_not_log" to LLSD.LLSDBoolean(true)))
        messageArchive.forEach { addMessage(it, false, doNotLog) }
    }

    fun removeScreenChat() {
        System.err.println("FSFloaterNearbyChat: removeScreenChat not yet implemented")
    }

    fun getVisible(): Boolean {
        System.err.println("FSFloaterNearbyChat: getVisible not yet implemented")
        return false
    }

    fun focusFirstItem(preferTextFields: Boolean = false, focusFocus: Boolean = true): Boolean {
        System.err.println("FSFloaterNearbyChat: focusFirstItem not yet implemented")
        return false
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        val maskControl = 0x01
        val maskShift   = 0x02
        val maskAlt     = 0x04
        val keyReturn   = 0x0D
        if (key == keyReturn) {
            return when (mask) {
                maskControl                  -> { System.err.println("FSFloaterNearbyChat: handleKeyHere maskControl not yet implemented"); false }
                maskShift                    -> { System.err.println("FSFloaterNearbyChat: handleKeyHere maskShift not yet implemented"); false }
                maskAlt                      -> { System.err.println("FSFloaterNearbyChat: handleKeyHere maskAlt not yet implemented"); false }
                maskShift or maskControl     -> { System.err.println("FSFloaterNearbyChat: handleKeyHere maskShift+maskControl not yet implemented"); false }
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
        System.err.println("FSFloaterNearbyChat: updateUnreadMessageNotification not yet implemented")
    }

    fun updateShowMutedChatHistory(data: LLSD) {
        val showMuted = data.asBoolean()
        updateUnreadMessageNotification(if (showMuted) unreadMessagesMuted else unreadMessages, showMuted)
    }

    fun handleMinimized(minimized: Boolean) {
        if (minimized) System.err.println("FSFloaterNearbyChat: handleMinimized(true) not yet implemented")
        else           System.err.println("FSFloaterNearbyChat: handleMinimized(false) not yet implemented")
    }

    fun updateFsUseNearbyChatConsole(data: LLSD) {
        fsUseNearbyChatConsole = data.asBoolean()
        if (fsUseNearbyChatConsole) {
            removeScreenChat()
            System.err.println("FSFloaterNearbyChat: updateFsUseNearbyChatConsole(true) not yet implemented")
        } else {
            System.err.println("FSFloaterNearbyChat: updateFsUseNearbyChatConsole(false) not yet implemented")
        }
    }

    fun updateRlvRestrictions(behavior: String) {
        if (behavior != "shownames") return
        setChatMentionPickerEnabled(true)
        System.err.println("FSFloaterNearbyChat: updateRlvRestrictions not yet implemented")
    }

    fun setChatMentionPickerEnabled(enabled: Boolean) {
        System.err.println("FSFloaterNearbyChat: setChatMentionPickerEnabled not yet implemented")
    }

    fun getMessageArchiveLength(): Int = messageArchive.size

    fun getSessionParticipants(): List<LLUUID> {
        System.err.println("FSFloaterNearbyChat: getSessionParticipants not yet implemented")
        return emptyList()
    }

    fun onGetChatBoxOpacityCallback(type: Int, alpha: Float): Float {
        val ttActive = 0
        val imOpacity = 1.0f
        return if (type != ttActive) minOf(imOpacity, alpha) else alpha
    }

    fun onHistoryButtonClicked() {
        System.err.println("FSFloaterNearbyChat: onHistoryButtonClicked not yet implemented")
    }

    fun onSearchButtonClicked() {
        System.err.println("FSFloaterNearbyChat: onSearchButtonClicked not yet implemented")
    }

    protected fun onChatBoxKeystroke() {
        System.err.println("FSFloaterNearbyChat: onChatBoxKeystroke not yet implemented")
    }

    protected fun onChatBoxFocusLost() {
        System.err.println("FSFloaterNearbyChat: onChatBoxFocusLost not yet implemented")
    }

    protected fun onChatBoxFocusReceived() {
        System.err.println("FSFloaterNearbyChat: onChatBoxFocusReceived not yet implemented")
    }

    protected fun onChatBoxCommit() {
        System.err.println("FSFloaterNearbyChat: onChatBoxCommit not yet implemented")
    }

    protected fun onChatTypeChanged() {
        System.err.println("FSFloaterNearbyChat: onChatTypeChanged not yet implemented")
    }

    protected fun reshapeChatLayoutPanel() {
        System.err.println("FSFloaterNearbyChat: reshapeChatLayoutPanel not yet implemented")
    }

    protected fun sendChat(type: ChatType) {
        System.err.println("FSFloaterNearbyChat: sendChat not yet implemented")
    }

    protected fun sendChatFromViewer(utf8text: String, type: ChatType, animate: Boolean) {
        System.err.println("FSFloaterNearbyChat: sendChatFromViewer not yet implemented")
    }

    private fun onChatOptionsContextMenuItemClicked(userdata: LLSD) {
        System.err.println("FSFloaterNearbyChat: onChatOptionsContextMenuItemClicked not yet implemented")
    }
    private fun onChatOptionsCheckContextMenuItem(userdata: LLSD): Boolean {
        System.err.println("FSFloaterNearbyChat: onChatOptionsCheckContextMenuItem not yet implemented")
        return false
    }
    private fun onChatOptionsVisibleContextMenuItem(userdata: LLSD): Boolean {
        System.err.println("FSFloaterNearbyChat: onChatOptionsVisibleContextMenuItem not yet implemented")
        return false
    }
    private fun onChatOptionsEnableContextMenuItem(userdata: LLSD): Boolean {
        System.err.println("FSFloaterNearbyChat: onChatOptionsEnableContextMenuItem not yet implemented")
        return false
    }

    private fun onEmojiRecentPanelToggleBtnClicked() {
        System.err.println("FSFloaterNearbyChat: onEmojiRecentPanelToggleBtnClicked not yet implemented")
    }
    private fun onEmojiPickerToggleBtnClicked() {
        System.err.println("FSFloaterNearbyChat: onEmojiPickerToggleBtnClicked not yet implemented")
    }
    private fun onEmojiPickerToggleBtnDown() {
        System.err.println("FSFloaterNearbyChat: onEmojiPickerToggleBtnDown not yet implemented")
    }
    private fun onEmojiPickerClosed() {
        System.err.println("FSFloaterNearbyChat: onEmojiPickerClosed not yet implemented")
    }
    private fun initEmojiRecentPanel() {
        System.err.println("FSFloaterNearbyChat: initEmojiRecentPanel not yet implemented")
    }
    private fun onRecentEmojiPicked(value: LLSD) {
        System.err.println("FSFloaterNearbyChat: onRecentEmojiPicked not yet implemented")
    }

    private fun onFocusLost() {
        System.err.println("FSFloaterNearbyChat: onFocusLost not yet implemented")
    }
    private fun onFocusReceived() {
        System.err.println("FSFloaterNearbyChat: onFocusReceived not yet implemented")
    }

    companion object {
        private var lastSpecialChatChannel: Int = 0

        @Volatile private var instance: FSFloaterNearbyChat? = null

        fun findInstance(): FSFloaterNearbyChat? = instance

        fun getInstance(): FSFloaterNearbyChat =
            instance ?: synchronized(this) {
                instance ?: FSFloaterNearbyChat(LLSD.Undefined).also { instance = it }
            }

        fun isChatMultiTab(): Boolean {
            System.err.println("FSFloaterNearbyChat: isChatMultiTab not yet implemented")
            return false
        }

        fun stopChat() {
            findInstance()?.let {
                System.err.println("FSFloaterNearbyChat: stopChat not yet implemented")
            }
        }

        fun processChatHistoryStyleUpdate(newvalue: LLSD) {
            val nearbyChat = getInstance()
            nearbyChat.updateChatHistoryStyle()
            System.err.println("FSFloaterNearbyChat: processChatHistoryStyleUpdate not yet implemented")
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

