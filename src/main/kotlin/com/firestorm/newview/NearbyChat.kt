package com.firestorm.newview

import com.firestorm.ui.Floater
import com.firestorm.ui.FloaterHandle
import com.firestorm.ui.ChatEntry
import com.firestorm.ui.FontGL
import com.firestorm.chat.Chat
import com.firestorm.chat.ChatType
import com.firestorm.chat.ChatStyle
import com.firestorm.chat.ChatSource
import com.firestorm.voice.VoiceClient
import com.firestorm.speaker.LocalSpeakerMgr
import com.firestorm.speaker.OutputMonitorCtrl
import com.firestorm.types.UUID
import com.firestorm.types.LLSD
import kotlin.math.min

private const val EXPANDED_HEIGHT: Int = 266
private const val COLLAPSED_HEIGHT: Int = 60
private const val EXPANDED_MIN_HEIGHT: Int = 150

data class ChatTypeTrigger(val name: String, val type: ChatType)

private val CHAT_TYPE_TRIGGERS = listOf(
    ChatTypeTrigger("/whisper", ChatType.CHAT_TYPE_WHISPER),
    ChatTypeTrigger("/shout", ChatType.CHAT_TYPE_SHOUT),
)

open class NearbyChat(key: LLSD = LLSD.fromUUID(UUID.NULL)) : Floater() {

    companion object {
        var lastSpecialChatChannel: Int = 0
            private set

        fun buildFloater(key: LLSD): NearbyChat = NearbyChat(key)

        fun startChat(line: String?) {
            TODO("APR: use JVM equivalent - focus chat input editor, optionally prefill text")
        }

        fun stopChat() {
            TODO("APR: use JVM equivalent - clear chat input and release focus")
        }

        fun sendChatFromViewer(utf8text: String, type: ChatType, animate: Boolean) {
            TODO("APR: use JVM equivalent - send chat packet on channel derived from text")
        }

        fun isWordsName(name: String): Boolean {
            return name.any { it == ' ' }
        }

        private fun matchChatTypeTrigger(inStr: String, outStr: StringBuilder): Boolean {
            val inLen = inStr.length
            for (trigger in CHAT_TYPE_TRIGGERS) {
                if (inLen <= trigger.name.length) {
                    val triggerTrunc = trigger.name.substring(0, min(inLen, trigger.name.length))
                    if (inStr.equals(triggerTrunc, ignoreCase = true)) {
                        outStr.setLength(0)
                        outStr.append(trigger.name)
                        return true
                    }
                }
            }
            return false
        }
    }

    private var outputMonitor: OutputMonitorCtrl? = null
    private var speakerMgr: LocalSpeakerMgr? = null
    private var expandedHeight: Int = COLLAPSED_HEIGHT + EXPANDED_HEIGHT
    protected var inputEditor: ChatEntry? = null

    private val messageArchive: MutableList<Chat> = mutableListOf()

    init {
        speakerMgr = LocalSpeakerMgr.getInstance()
    }

    override fun postBuild(): Boolean {
        setIsSingleInstance(true)

        inputEditor = getChild("chat_editor")
        inputEditor?.setCommitCallback { onChatBoxCommit() }
        inputEditor?.setKeystrokeCallback { onChatBoxKeystroke() }
        inputEditor?.setFocusLostCallback { onChatBoxFocusLost() }
        inputEditor?.setFocusReceivedCallback { onChatBoxFocusReceived() }
        inputEditor?.setLabel(getString("NearbyChatTitle"))

        setTitle(getString("NearbyChatTitle"))

        if (getSavedPerAccountSetting("LogShowHistory")) {
            loadHistory()
        }

        return true
    }

    override fun onOpen(key: LLSD) {
        super.onOpen(key)
        if (!isMessagePaneExpanded()) {
            restoreFloater()
        }
    }

    override fun onClose(appQuitting: Boolean) {
        restoreFloater()
        if (appQuitting) {
            forceReshape()
            storeRectControl()
        }
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (visible) removeScreenChat()
    }

    fun setVisibleAndFrontmost(takeFocus: Boolean = true, key: LLSD = LLSD.EMPTY) {
        super.setVisibleAndFrontmost(takeFocus, key)
        if (matchesKey(key)) {
            TODO("APR: use JVM equivalent - select conversation pair in IM container")
        }
    }

    fun closeHostedFloater() {
        if (getHost() == null) setVisible(false)
        TODO("APR: use JVM equivalent - manage IM container conversation closing logic")
    }

    fun loadHistory() {
        val doNotLog = LLSD.mapOf("do_not_log" to LLSD.fromBoolean(true))
        val history = loadChatHistory("chat")
        for (msg in history) {
            val from = msg["from"].asString()
            val fromId: UUID = if (msg["from_id"].isDefined()) {
                msg["from_id"].asUUID()
            } else {
                TODO("APR: use JVM equivalent - resolve legacy name to UUID via name cache")
            }

            val chat = Chat(
                fromName = from,
                fromId = fromId,
                text = msg["message"].asString(),
                timeStr = msg["time"].asString(),
                chatStyle = ChatStyle.CHAT_STYLE_HISTORY,
                sourceType = when {
                    fromId == UUID.NULL && from == SYSTEM_FROM -> ChatSource.CHAT_SOURCE_SYSTEM
                    fromId == UUID.NULL -> if (isWordsName(from)) ChatSource.CHAT_SOURCE_UNKNOWN else ChatSource.CHAT_SOURCE_OBJECT
                    else -> ChatSource.CHAT_SOURCE_AGENT
                },
            )
            addMessage(chat, archive = true, args = doNotLog)
        }
    }

    fun reloadMessages(cleanMessages: Boolean = false) {
        if (cleanMessages) {
            messageArchive.clear()
            loadHistory()
        }
        clearChatHistory()
        val doNotLog = LLSD.mapOf("do_not_log" to LLSD.fromBoolean(true))
        for (msg in messageArchive) {
            addMessage(msg, archive = false, args = doNotLog)
        }
    }

    fun removeScreenChat() {
        TODO("APR: use JVM equivalent - remove toasts from nearby chat notification channel")
    }

    fun show() {
        openFloater(key)
    }

    fun isChatVisible(): Boolean {
        TODO("APR: use JVM equivalent - check IM container visibility and minimized state")
    }

    fun addMessage(message: Chat, archive: Boolean = true, args: LLSD = LLSD.EMPTY) {
        if (archive) messageArchive.add(message)
        TODO("APR: use JVM equivalent - append message to chat history widget")
    }

    fun getChatBox(): ChatEntry? = inputEditor

    fun getCurrentChat(): String = inputEditor?.getText() ?: ""

    fun getMessageArchiveLength(): Int = messageArchive.size

    fun showHistory() {
        openFloater()
        TODO("APR: use JVM equivalent - select nearby chat conversation and expand pane")
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        return when {
            key == KEY_RETURN && mask == MASK_CONTROL -> { sendChat(ChatType.CHAT_TYPE_SHOUT); true }
            key == KEY_RETURN && mask == MASK_SHIFT -> { sendChat(ChatType.CHAT_TYPE_WHISPER); true }
            mask == MASK_ALT && isTornOff() -> {
                when (key) {
                    KEY_UP, KEY_LEFT -> { TODO("APR: use JVM equivalent - select previous conversation") }
                    KEY_DOWN, KEY_RIGHT -> { TODO("APR: use JVM equivalent - select next conversation") }
                    else -> false
                }
            }
            else -> false
        }
    }

    protected fun onChatBoxKeystroke() {
        TODO("APR: use JVM equivalent - handle typing indicator, gesture autocomplete, and channel strip")
    }

    protected fun onChatBoxFocusLost() {
        TODO("APR: use JVM equivalent - stop typing animation via agent")
    }

    protected fun onChatBoxFocusReceived() {
        inputEditor?.setEnabled(!isDisconnected())
    }

    protected fun sendChat(type: ChatType) {
        val text = inputEditor?.getConvertedText() ?: return
        if (text.isBlank()) return
        val resolvedType = processChatTypeTriggers(type, StringBuilder(text))
        val channel = stripChannelNumber(text)
        sendChatFromViewer(channel.first, resolvedType, shouldAnimate(resolvedType))
        inputEditor?.setText("")
    }

    protected fun onChatBoxCommit() {
        sendChat(ChatType.CHAT_TYPE_NORMAL)
    }

    protected fun onChatFontChange(fontp: FontGL) {
        inputEditor?.setFont(fontp)
    }

    protected fun onTearOffClicked() {
        TODO("APR: use JVM equivalent - toggle tear-off and persist state in per-account settings")
    }

    protected fun onClickCloseBtn(appQuitting: Boolean = false) {
        if (!isTornOff()) return
        closeHostedFloater()
    }

    protected fun displaySpeakingIndicator() {
        TODO("APR: use JVM equivalent - show/hide output monitor based on PTT state")
    }

    protected fun processChatTypeTriggers(type: ChatType, str: StringBuilder): ChatType {
        val s = str.toString()
        for (trigger in CHAT_TYPE_TRIGGERS) {
            if (s.length >= trigger.name.length) {
                val prefix = s.substring(0, trigger.name.length)
                if (prefix.equals(trigger.name, ignoreCase = true)) {
                    var trimLen = trigger.name.length
                    if (s.length > trimLen && s[trimLen] == ' ') trimLen++
                    str.replace(0, str.length, s.substring(trimLen))
                    return if (type == ChatType.CHAT_TYPE_NORMAL) trigger.type else type
                }
            }
        }
        return type
    }

    private fun stripChannelNumber(text: String): Pair<String, Int> {
        TODO("APR: use JVM equivalent - parse /channel prefix from text, return remainder and channel number")
    }

    private fun shouldAnimate(type: ChatType): Boolean = type == ChatType.CHAT_TYPE_NORMAL

    private fun refresh() {
        displaySpeakingIndicator()
        VoiceClient.getInstance()?.let { vc ->
            TODO("APR: use JVM equivalent - update call button state from PTT state")
        }
        TODO("APR: use JVM equivalent - set transparency type based on focus")
    }

    private fun isMessagePaneExpanded(): Boolean {
        TODO("APR: use JVM equivalent - check collapsed state from floater geometry")
    }

    private fun restoreFloater() {
        TODO("APR: use JVM equivalent - restore floater to expanded state")
    }

    private fun forceReshape() {
        TODO("APR: use JVM equivalent - snap floater to expanded rect")
    }

    private fun storeRectControl() {
        TODO("APR: use JVM equivalent - persist floater rect to settings")
    }

    private fun clearChatHistory() {
        TODO("APR: use JVM equivalent - clear chat history widget")
    }

    private fun isDisconnected(): Boolean {
        TODO("APR: use JVM equivalent - check global gDisconnected flag equivalent")
    }

    private fun getSavedPerAccountSetting(key: String): Boolean {
        TODO("APR: use JVM equivalent - read per-account saved setting")
    }

    private fun matchesKey(key: LLSD): Boolean {
        TODO("APR: use JVM equivalent - check if key matches this floater's session key")
    }

    private fun loadChatHistory(name: String): List<LLSD> {
        TODO("APR: use JVM equivalent - read chat history from log file")
    }

    private fun getString(key: String): String {
        TODO("APR: use JVM equivalent - translate UI string key")
    }

    companion object {
        private const val SYSTEM_FROM = "Second Life"
        private const val KEY_RETURN = 13
        private const val KEY_UP = 38
        private const val KEY_DOWN = 40
        private const val KEY_LEFT = 37
        private const val KEY_RIGHT = 39
        private const val MASK_CONTROL = 2
        private const val MASK_SHIFT = 1
        private const val MASK_ALT = 4
    }
}
