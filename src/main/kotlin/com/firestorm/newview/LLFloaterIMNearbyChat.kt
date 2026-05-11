package com.firestorm.newview

import java.util.UUID

private const val EXPANDED_HEIGHT = 266
private const val COLLAPSED_HEIGHT = 60
private const val EXPANDED_MIN_HEIGHT = 150

private data class LLChatTypeTrigger(val name: String, val type: EChatType)

private val sChatTypeTriggers = listOf(
    LLChatTypeTrigger("/whisper", EChatType.CHAT_TYPE_WHISPER),
    LLChatTypeTrigger("/shout", EChatType.CHAT_TYPE_SHOUT)
)

class LLFloaterIMNearbyChat(key: Any = UUID(0, 0)) : LLFloaterIMSessionTab(key) {

    private var mSpeakerMgr: LLLocalSpeakerMgr? = null
    private var mExpandedHeight: Int = COLLAPSED_HEIGHT + EXPANDED_HEIGHT
    private val mMessageArchive: MutableList<LLChat> = mutableListOf()

    init {
        mIsP2PChat = false
        mIsNearbyChat = true
        mSpeakerMgr = LLLocalSpeakerMgr.getInstance()
        mMinFloaterHeight = EXPANDED_MIN_HEIGHT
    }

    override fun postBuild(): Boolean {
        setIsSingleInstance(true)
        val result = super.postBuild()

        mInputEditor?.setAutoreplaceCallback { s1, s2, s3, s4, s5 ->
            LLAutoReplace.getInstance().autoreplaceCallback(s1, s2, s3, s4, s5)
        }
        mInputEditor?.setCommitCallback { onChatBoxCommit() }
        mInputEditor?.setKeystrokeCallback { onChatBoxKeystroke() }
        mInputEditor?.setFocusLostCallback { onChatBoxFocusLost() }
        mInputEditor?.setFocusReceivedCallback { onChatBoxFocusReceived() }

        val nearbyChatTitle = LLTrans.getString("NearbyChatTitle")
        mInputEditor?.setLabel(nearbyChatTitle)
        setTitle(nearbyChatTitle)

        gSavedSettings.declareS32("nearbychat_showicons_and_names", 2, "NearByChat header settings", LLControlVariable.PERSIST_NONDFT)

        if (gSavedPerAccountSettings.getBOOL("LogShowHistory")) {
            loadHistory()
        }

        return result
    }

    override fun onOpen(key: Any) {
        super.onOpen(key)
        if (!isMessagePaneExpanded()) {
            restoreFloater()
            onCollapseToLine(this)
        }
    }

    override fun onClose(appQuitting: Boolean) {
        super.restoreFloater()
        if (appQuitting) {
            forceReshape()
            storeRectControl()
        }
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (visible) {
            removeScreenChat()
        }
    }

    override fun setVisibleAndFrontmost(takeFocus: Boolean, key: Any) {
        super.setVisibleAndFrontmost(takeFocus, key)
        if (matchesKey(key)) {
            LLFloaterIMContainer.getInstance().selectConversationPair(mSessionID, true, takeFocus)
        }
    }

    override fun closeHostedFloater() {
        if (getHost() == null) {
            setVisible(false)
        }
        val floaterContainer = LLFloaterIMContainer.getInstance()
        if (floaterContainer.getConversationListItemSize() == 1) {
            if (getHost() != null) {
                floaterContainer.closeFloater()
            }
        } else {
            if (getHost() == null) {
                floaterContainer.selectNextConversationByID(UUID(0, 0))
            }
        }
    }

    override fun onTearOffClicked() {
        super.onTearOffClicked()
        val inTheMultifloater = getHost() != null
        gSavedPerAccountSettings.setBOOL("NearbyChatIsNotTornOff", inTheMultifloater)
    }

    override fun onClickCloseBtn(appQuitting: Boolean) {
        if (!isTornOff()) return
        closeHostedFloater()
    }

    override fun refresh() {
        displaySpeakingIndicator()
        updateCallBtnState(LLVoiceClient.getInstance().getUserPTTState())
        if (getTransparencyType() != TT_DEFAULT) {
            setTransparencyType(if (hasFocus()) TT_ACTIVE else TT_INACTIVE)
        }
    }

    fun loadHistory() {
        val doNotLog = mutableMapOf<String, Any>("do_not_log" to true)
        val history = LLLogChat.loadChatHistory("chat")

        for (msg in history) {
            val from = msg[LL_IM_FROM] as? String ?: ""
            var fromId: UUID = UUID(0, 0)
            if (msg[LL_IM_FROM_ID] != null) {
                fromId = UUID.fromString(msg[LL_IM_FROM_ID].toString())
            } else {
                val legacyName = gCacheName.buildLegacyName(from)
                fromId = LLAvatarNameCache.getInstance().findIdByName(legacyName)
            }

            val chat = LLChat()
            chat.mFromName = from
            chat.mFromID = fromId
            chat.mText = msg[LL_IM_TEXT]?.toString() ?: ""
            chat.mTimeStr = msg[LL_IM_TIME]?.toString() ?: ""
            chat.mChatStyle = CHAT_STYLE_HISTORY

            chat.mSourceType = CHAT_SOURCE_AGENT
            if (fromId == UUID(0, 0) && SYSTEM_FROM == from) {
                chat.mSourceType = CHAT_SOURCE_SYSTEM
            } else if (fromId == UUID(0, 0)) {
                chat.mSourceType = if (isWordsName(from)) CHAT_SOURCE_UNKNOWN else CHAT_SOURCE_OBJECT
            }

            addMessage(chat, true, doNotLog)
        }
    }

    fun reloadMessages(cleanMessages: Boolean = false) {
        if (cleanMessages) {
            mMessageArchive.clear()
            loadHistory()
        }
        mChatHistory?.clear()
        val doNotLog = mutableMapOf<String, Any>("do_not_log" to true)
        for (chat in mMessageArchive) {
            addMessage(chat, false, doNotLog)
        }
    }

    fun removeScreenChat() {
        val chatChannel = LLNotificationsUI.LLChannelManager.getInstance()
            .findChannelByID(LLNotificationsUI.NEARBY_CHAT_CHANNEL_UUID)
        chatChannel?.removeToastsFromChannel()
    }

    fun addMessage(message: LLChat, archive: Boolean = true, args: Map<String, Any> = emptyMap()) {
        appendMessage(message, args)
        if (archive) {
            mMessageArchive.add(message)
            if (mMessageArchive.size > 200) {
                mMessageArchive.removeAt(0)
            }
        }
        if (args["do_not_log"]?.toString()?.toBoolean() != true &&
            gSavedPerAccountSettings.getS32("KeepConversationLogTranscripts") > 1
        ) {
            var fromName = message.mFromName
            if (message.mSourceType == CHAT_SOURCE_AGENT) {
                val avName = LLAvatarNameCache.get(message.mFromID)
                if (avName != null && !avName.isDisplayNameDefault()) {
                    fromName = avName.getCompleteName()
                }
            }
            LLLogChat.saveHistory("chat", fromName, message.mFromID, message.mText)
        }
    }

    fun show() {
        openFloater(getKey())
    }

    fun isChatVisible(): Boolean {
        val imBox = LLFloaterIMContainer.getInstance()
        return if (isChatMultiTab() && gSavedPerAccountSettings.getBOOL("NearbyChatIsNotTornOff")) {
            imBox.getVisible() && !imBox.isMinimized()
        } else {
            getVisible() && !isMinimized()
        }
    }

    fun showHistory() {
        openFloater()
        LLFloaterIMContainer.getInstance().selectConversation(UUID(0, 0))
        if (!isMessagePaneExpanded()) {
            restoreFloater()
            setFocus(true)
        } else {
            LLFloaterIMContainer.getInstance().setFocus(true)
        }
        setResizeLimits(getMinWidth(), EXPANDED_MIN_HEIGHT)
    }

    fun getCurrentChat(): String = mInputEditor?.getText() ?: ""

    fun getMessageArchiveLength(): Int = mMessageArchive.size

    fun getChatBox(): LLChatEntry? = mInputEditor

    override fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (key == KEY_RETURN && mask == MASK_CONTROL) {
            sendChat(EChatType.CHAT_TYPE_SHOUT)
            return true
        }
        if (key == KEY_RETURN && mask == MASK_SHIFT) {
            sendChat(EChatType.CHAT_TYPE_WHISPER)
            return true
        }
        if (mask == MASK_ALT && isTornOff()) {
            val floaterContainer = LLFloaterIMContainer.getInstance()
            if (key == KEY_UP || key == KEY_LEFT) {
                floaterContainer.selectNextorPreviousConversation(false)
                return true
            }
            if (key == KEY_DOWN || key == KEY_RIGHT) {
                floaterContainer.selectNextorPreviousConversation(true)
                return true
            }
        }
        return false
    }

    private fun onChatBoxKeystroke() {
        val imBox = LLFloaterIMContainer.findInstance()
        imBox?.flashConversationItemWidget(mSessionID, false)

        LLFirstUse.otherAvatarChatFirst(false)

        val rawText = mInputEditor?.getWText() ?: return
        val trimmed = rawText.trimStart()
        val length = trimmed.length

        if (length > 0 && trimmed[0] != '/') {
            gAgent.startTyping()
        } else {
            gAgent.stopTyping()
        }

        if (gSavedSettings.getBOOL("ChatAutocompleteGestures") && length > 1 && trimmed[0] == '/') {
            val utf8Trigger = trimmed
            var utf8OutStr = utf8Trigger

            if (LLGestureMgr.instance().matchPrefix(utf8Trigger, utf8OutStr)) {
                val restOfMatch = utf8OutStr.substring(utf8Trigger.length)
                if (restOfMatch.isNotEmpty()) {
                    mInputEditor!!.setText(utf8Trigger + restOfMatch)
                    mInputEditor!!.selectByCursorPosition(
                        utf8OutStr.length - restOfMatch.length,
                        utf8OutStr.length
                    )
                }
            } else {
                var matchedTrigger: String? = null
                matchChatTypeTrigger(utf8Trigger)?.let { matchedTrigger = it }
                if (matchedTrigger != null) {
                    val restOfMatch = matchedTrigger!!.substring(utf8Trigger.length)
                    mInputEditor!!.setText(utf8Trigger + restOfMatch + " ")
                    mInputEditor!!.endOfDoc()
                }
            }
        }
    }

    private fun onChatBoxFocusLost() {
        gAgent.stopTyping()
    }

    private fun onChatBoxFocusReceived() {
        mInputEditor?.setEnabled(!gDisconnected)
    }

    private fun onChatBoxCommit() {
        sendChat(EChatType.CHAT_TYPE_NORMAL)
        gAgent.stopTyping()
    }

    private fun sendChat(type: EChatType) {
        val editor = mInputEditor ?: return
        val text = editor.getConvertedText().trim().replace('¶', '\n')
        if (text.isNotEmpty()) {
            var channel = 0
            val outText = stripChannelNumber(text) { ch -> channel = ch }
            val utf8Text = outText
            var revisedText = if (channel == 0) {
                LLGestureMgr.instance().triggerAndReviseString(utf8Text) ?: utf8Text
            } else {
                utf8Text
            }
            revisedText = revisedText.trim()
            val effectiveType = processChatTypeTriggers(type, revisedText)
            if (revisedText.isNotEmpty()) {
                sendChatFromViewer(revisedText, effectiveType, gSavedSettings.getBOOL("PlayChatAnim"))
            }
        }
        editor.setText("")
        gAgent.stopTyping()
        if (gSavedSettings.getBOOL("CloseChatOnReturn")) {
            stopChat()
        }
    }

    private fun displaySpeakingIndicator() {
        val speakerList = mutableListOf<LLSpeaker>()
        mSpeakerMgr?.update(false)
        mSpeakerMgr?.getSpeakerList(speakerList, false)
        for (s in speakerList) {
            if (s.mSpeechVolume > 0 || s.mStatus == LLSpeaker.STATUS_SPEAKING) {
                break
            }
        }
    }

    companion object {
        var sLastSpecialChatChannel: Int = 0

        fun buildFloater(key: Any): LLFloaterIMNearbyChat {
            LLFloaterReg.getInstance("im_container")
            return LLFloaterIMNearbyChat(key)
        }

        fun startChat(line: String?) {
            val nearbyChat = LLFloaterReg.getTypedInstance<LLFloaterIMNearbyChat>("nearby_chat") ?: return
            if (!nearbyChat.isTornOff()) {
                LLFloaterIMContainer.getInstance().selectConversation(UUID(0, 0))
            }
            if (nearbyChat.isMinimized()) {
                nearbyChat.setMinimized(false)
            }
            nearbyChat.show()
            nearbyChat.setFocus(true)
            if (line != null) {
                nearbyChat.mInputEditor?.setText(line)
            }
            nearbyChat.mInputEditor?.endOfDoc()
        }

        fun stopChat() {
            val nearbyChat = LLFloaterReg.getTypedInstance<LLFloaterIMNearbyChat>("nearby_chat") ?: return
            nearbyChat.mInputEditor?.setFocus(false)
            gAgent.stopTyping()
        }

        fun sendChatFromViewer(utf8Text: String, type: EChatType, animate: Boolean) {
            TODO("APR: use JVM equivalent - send ChatFromViewer or ScriptDialogReply UDP message via gMessageSystem")
        }

        fun isWordsName(name: String): Boolean {
            val openParen = name.indexOf(" (")
            val closeParen = name.indexOf(')')
            return if (openParen != -1 && closeParen == name.length - 1) {
                true
            } else {
                val pos = name.indexOf(' ')
                pos != -1 && name.lastIndexOf(' ') == pos && pos != 0 && pos != name.length - 1
            }
        }

        private fun matchChatTypeTrigger(inStr: String): String? {
            for (trigger in sChatTypeTriggers) {
                if (inStr.length <= trigger.name.length) {
                    val truncated = trigger.name.substring(0, inStr.length)
                    if (inStr.toLowerCase() == truncated.toLowerCase()) {
                        return trigger.name
                    }
                }
            }
            return null
        }

        private fun processChatTypeTriggers(type: EChatType, str: String): EChatType {
            for (trigger in sChatTypeTriggers) {
                if (str.length >= trigger.name.length) {
                    val candidate = str.substring(0, trigger.name.length)
                    if (candidate.toLowerCase() == trigger.name.toLowerCase()) {
                        if (type == EChatType.CHAT_TYPE_NORMAL) return trigger.type
                        break
                    }
                }
            }
            return type
        }

        private fun stripChannelNumber(text: String, channelOut: (Int) -> Unit): String {
            if (text.length >= 2 && text[0] == '/' && text[1] == '/') {
                channelOut(sLastSpecialChatChannel)
                return text.substring(2)
            }
            if (text.isNotEmpty() && text[0] == '/' && text.length > 1 &&
                (text[1].isDigit() || (text[1] == '-' && text.length > 2 && text[2].isDigit()))
            ) {
                var pos = 1
                val channelBuilder = StringBuilder()
                while (pos < text.length && pos < 65) {
                    val c = text[pos]
                    if (c.isDigit() || (pos == 1 && c == '-')) {
                        channelBuilder.append(c)
                        pos++
                    } else break
                }
                while (pos < text.length && text[pos].isWhitespace()) pos++
                sLastSpecialChatChannel = channelBuilder.toString().toIntOrNull() ?: 0
                channelOut(sLastSpecialChatChannel)
                return text.substring(pos)
            }
            channelOut(0)
            return text
        }
    }
}
