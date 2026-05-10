package com.firestorm.newview

import java.util.UUID

private const val ME_TYPING_TIMEOUT_SECS: Float = 4.0f
private const val OTHER_TYPING_TIMEOUT_SECS: Float = 9.0f
private const val AGENT_TYPING_TIMEOUT_SECS: Float = 10.0f

// ─── FloaterIMSession ─────────────────────────────────────────────────────────
//
// Represents an individual IM session window (P2P, group, or ad-hoc conference).
// Corresponds to llfloaterimsession.h / llfloaterimsession.cpp, stripped of
// Vivox-specific voice stubs and RLVa filter logic (both remain as TODO stubs).

open class FloaterIMSession(sessionId: UUID) : FloaterIMSessionTab(sessionId) {

    companion object {
        val imFloaterShowedListeners: MutableList<(UUID) -> Unit> = mutableListOf()

        fun findInstance(sessionId: UUID): FloaterIMSession? =
            TODO("APR: use JVM equivalent - FloaterReg.findTypedInstance(\"impanel\", $sessionId)")

        fun getInstance(sessionId: UUID): FloaterIMSession =
            TODO("APR: use JVM equivalent - FloaterReg.getTypedInstance(\"impanel\", $sessionId)")

        fun show(sessionId: UUID): FloaterIMSession? =
            TODO("APR: use JVM equivalent - make IM session floater visible and bring to front for $sessionId")

        fun toggle(sessionId: UUID): Boolean =
            TODO("APR: use JVM equivalent - toggle visibility of session floater $sessionId; return new visibility")

        fun newIMCallback(data: Map<String, Any>) {
            val numUnread = (data["num_unread"] as? Int) ?: 0
            val fromIdNull = (data["from_id"] as? UUID) == null
            if (numUnread > 0 || fromIdNull) {
                val sessionId = data["session_id"] as? UUID ?: return
                val floater = findInstance(sessionId) ?: return
                if (floater.isInVisibleChain()) floater.updateMessages()
            }
        }

        fun removeTypingIndicator(data: Map<String, Any>) {
            val sessionId = data["session_id"] as? UUID ?: return
            val floater = findInstance(sessionId) ?: return
            floater.removeTypingIndicator(data["from_id"] as? UUID)
        }

        fun onIMChicletCreated(sessionId: UUID) {
            TODO("APR: use JVM equivalent - notify chiclet bar that IM session $sessionId was created")
        }

        fun setIMFloaterShowedCallback(callback: (UUID) -> Unit) {
            imFloaterShowedListeners += callback
        }
    }

    var lastMessageIndex: Int = -1
        private set

    private var dialog: InstantMessageType = InstantMessageType.IM_NOTHING_SPECIAL
    var otherParticipantUUID: UUID = UUID(0, 0)
        private set

    private var meTyping: Boolean = false
    private var otherTyping: Boolean = false
    private var shouldSendTypingState: Boolean = false
    private var sessionNameUpdatedForTyping: Boolean = false
    private var positioned: Boolean = false
    private var sessionInitialized: Boolean = false
    private val queuedMsgsForInit: MutableList<String> = mutableListOf()
    private val invitedParticipants: MutableList<UUID> = mutableListOf()

    private var meTypingElapsed: Float = 0f
    private var typingTimeoutElapsed: Float = 0f
    private var otherTypingElapsed: Float = 0f

    init {
        isNearbyChat = false
        initIMSession(sessionId)
        TODO("APR: use JVM equivalent - register as voice client status observer; set docked=true")
    }

    fun initIMSession(sessionId: UUID) {
        this.sessionID = sessionId
        val session = IMModel.findIMSession(sessionId)
        if (session != null) {
            isP2PChat = session.isP2PSessionType()
            sessionInitialized = session.sessionInitialized
            dialog = session.type
        }
    }

    fun initIMFloater() {
        val otherParty = IMModel.getOtherParticipantID(sessionID)
        if (otherParty != UUID(0, 0)) otherParticipantUUID = otherParty
        TODO("APR: use JVM equivalent - bindVoiceChannel(); set typingStart label; show/hide participant list panel")

        val session = IMModel.findIMSession(sessionID)
        if (session != null && !session.textIMPossible) {
            TODO("APR: use JVM equivalent - disable input editor; set unavailable label")
        }
        if (!isP2PChat) {
            val sessionName = IMModel.getName(sessionID)
            updateSessionName(sessionName)
        }
    }

    override fun postBuild(): Boolean {
        val result = super.postBuild()
        TODO("APR: use JVM equivalent - configure input editor: maxLen=1023, autoreplace, focus/keystroke/commit callbacks; add_btn enabled/click; register voice observer; setDocked(true)")
        initIMFloater()
        return result
    }

    override fun refresh() {
        if (meTyping) {
            meTypingElapsed += frameDelta()
            if (meTypingElapsed > ME_TYPING_TIMEOUT_SECS && !shouldSendTypingState) {
                IMModel.sendTypingState(sessionID, otherParticipantUUID, true)
                meTypingElapsed = 0f
            }
            typingTimeoutElapsed += frameDelta()
            if (typingTimeoutElapsed > AGENT_TYPING_TIMEOUT_SECS) {
                setTyping(false)
            }
        }
        if (otherTyping) {
            otherTypingElapsed += frameDelta()
            if (otherTypingElapsed > OTHER_TYPING_TIMEOUT_SECS) {
                removeTypingIndicator(otherParticipantUUID)
                otherTyping = false
            }
        }
    }

    override fun draw() {
        TODO("GPU: draw IM session panel including chat history, typing indicator, and input editor")
    }

    override fun onClose(appQuitting: Boolean) {
        if (appQuitting) {
            super.onClose(appQuitting)
            return
        }
        val session = IMModel.findIMSession(sessionID)
        if (session != null) {
            val isCallWithChat = session.isGroupSessionType() || session.isAdHocSessionType() || session.isP2PSessionType()
            val voiceChannel = IMModel.getVoiceChannel(sessionID)
            val voiceActive = voiceChannel != null && (TODO("APR: use JVM equivalent - voiceChannel.isActive()") as Boolean)
            if (isCallWithChat && voiceActive) {
                TODO("APR: use JVM equivalent - show ConfirmLeaveCall notification with session_id payload")
                return
            }
        }
        super.onClose(appQuitting)
    }

    override fun onTearOffClicked() {
        super.onTearOffClicked()
    }

    override fun setMinimized(value: Boolean) {
        TODO("APR: use JVM equivalent - super.setMinimized($value)")
    }

    override fun setFocus(focus: Boolean) {
        TODO("APR: use JVM equivalent - super.setFocus($focus)")
    }

    fun setPositioned(value: Boolean) { positioned = value }

    fun sessionInitReplyReceived(imSessionId: UUID) {
        sessionInitialized = true
        sessionID = imSessionId
        for (msg in queuedMsgsForInit) {
            IMModel.sendMessage(msg, sessionID, otherParticipantUUID, dialog)
        }
        queuedMsgsForInit.clear()
        updateMessages()
    }

    fun updateMessages() {
        TODO("APR: use JVM equivalent - fetch new messages from IMModel for sessionID and append to chat history widget")
    }

    fun reloadMessages(cleanMessages: Boolean = false) {
        TODO("APR: use JVM equivalent - clear and re-populate chat history from IMModel message store")
    }

    fun sendMsgFromInputEditor() {
        val isGodlike = TODO("APR: use JVM equivalent - agent.isGodlike()") as Boolean
        if (!isGodlike && dialog == InstantMessageType.IM_NOTHING_SPECIAL && otherParticipantUUID == UUID(0, 0)) return
        val text = getInputEditorText().trim()
        if (text.isEmpty()) return
        sendMsg(text)
        clearInputEditor()
    }

    fun sendMsg(msg: String) {
        val truncated = msg.take(MAX_MSG_BUF_SIZE - 1)
        val filtered = applyRlvFilter(truncated)
        if (sessionInitialized) {
            IMModel.sendMessage(filtered, sessionID, otherParticipantUUID, dialog)
        } else {
            queuedMsgsForInit += filtered
        }
        updateMessages()
    }

    fun processIMTyping(fromId: UUID, typing: Boolean) {
        if (typing) addTypingIndicator(fromId) else removeTypingIndicator(fromId)
    }

    fun processAgentListUpdates(body: Map<String, Any>) {
        TODO("APR: use JVM equivalent - update participant list with agent additions/removals from body")
    }

    fun processSessionUpdate(sessionUpdate: Map<String, Any>) {
        TODO("APR: use JVM equivalent - update session metadata (name, moderation mode) from sessionUpdate")
    }

    fun enableGearMenuItem(userdata: Any): Boolean {
        val command = userdata.toString()
        return TODO("APR: use JVM equivalent - IMContainer.enableContextMenuItem($command, [otherParticipantUUID])")
    }

    fun gearDoToSelected(userdata: Any) {
        val command = userdata.toString()
        TODO("APR: use JVM equivalent - IMContainer.doToParticipants($command, [otherParticipantUUID])")
    }

    fun checkGearMenuItem(userdata: Any): Boolean {
        val command = userdata.toString()
        return TODO("APR: use JVM equivalent - IMContainer.checkContextMenuItem($command, [otherParticipantUUID])")
    }

    fun onVisibilityChanged(newVisibility: Boolean) {
        val voiceChannel = IMModel.getVoiceChannel(sessionID)
        if (newVisibility && voiceChannel != null) {
            val connected = TODO("APR: use JVM equivalent - voiceChannel.state == STATE_CONNECTED") as Boolean
            if (connected) TODO("APR: use JVM equivalent - FloaterReg.showInstance(voice_call, $sessionID)")
        } else {
            TODO("APR: use JVM equivalent - FloaterReg.hideInstance(voice_call, $sessionID)")
        }
    }

    fun onVoiceChannelStateChanged(oldState: VoiceChannelState, newState: VoiceChannelState) {
        TODO("APR: use JVM equivalent - update call/end-call buttons and session title based on state transition")
    }

    fun onChange(status: VoiceStatusType, channelInfo: Any, proximal: Boolean) {
        TODO("APR: use JVM equivalent - enable/disable call button based on voice availability")
    }

    fun needsTitleOverwrite(): Boolean = sessionNameUpdatedForTyping && otherTyping
    fun getLastChatMessageIndex(): Int = lastMessageIndex

    override fun updateSessionName(name: String) {
        TODO("APR: use JVM equivalent - update floater title and input placeholder to $name")
    }

    private fun setTyping(typing: Boolean) {
        meTyping = typing
        shouldSendTypingState = true
        if (!typing) {
            typingTimeoutElapsed = 0f
            meTypingElapsed = 0f
        }
        TODO("APR: use JVM equivalent - send typing state via IMModel.sendTypingState if shouldSend")
    }

    private fun onAddButtonClicked() {
        TODO("APR: use JVM equivalent - show avatar picker floater; on selection call addSessionParticipants")
    }

    private fun addSessionParticipants(uuids: List<UUID>) {
        if (isP2PChat) {
            TODO("APR: use JVM equivalent - show ConfirmAddingChatParticipants notification; on accept call addP2PSessionParticipants")
        } else {
            invitedParticipants += uuids
            inviteToSession(uuids)
        }
    }

    private fun addP2PSessionParticipants(response: Int, uuids: List<UUID>) {
        if (response != 0) return
        val voiceActive = TODO("APR: use JVM equivalent - IMModel.getVoiceChannel($sessionID)?.isActive()") as Boolean
        val allIds = buildList { add(otherParticipantUUID); addAll(uuids) }
        if (voiceActive) {
            TODO("APR: use JVM equivalent - start conference voice call with allIds")
        } else {
            TODO("APR: use JVM equivalent - IMMgr.addSession as conference with allIds then close this floater")
        }
    }

    private fun inviteToSession(uuids: List<UUID>): Boolean =
        TODO("APR: use JVM equivalent - HTTP POST to chatter-box invite URL for session $sessionID with uuids")

    private fun canAddSelectedToChat(uuids: List<UUID>): Boolean {
        if (dialog == InstantMessageType.IM_SESSION_GROUP_START) return false
        val session = IMModel.findIMSession(sessionID) ?: return false
        return if (isP2PChat) {
            uuids.none { it == otherParticipantUUID }
        } else {
            val speakers = TODO("APR: use JVM equivalent - IMModel.getSpeakerManager($sessionID)?.getSpeakerList(active=true)") as List<UUID>
            uuids.none { it in speakers }
        }
    }

    private fun addTypingIndicator(fromId: UUID) {
        if (!otherTyping) {
            otherTyping = true
            sessionNameUpdatedForTyping = true
            otherTypingElapsed = 0f
            TODO("APR: use JVM equivalent - show typing indicator in chat history / title for from_id=$fromId")
        }
        otherTypingElapsed = 0f
    }

    private fun removeTypingIndicator(fromId: UUID?) {
        if (otherTyping) {
            otherTyping = false
            sessionNameUpdatedForTyping = false
            TODO("APR: use JVM equivalent - remove typing indicator from chat history for from_id=$fromId")
        }
    }

    private fun handleDragAndDrop(x: Int, y: Int, mask: Int, drop: Boolean, cargoType: Any, cargoData: Any): Boolean =
        TODO("APR: use JVM equivalent - accept item/category drops and invite dropped users to session")

    private fun applyRlvFilter(text: String): String {
        TODO("APR: use JVM equivalent - apply RLVa sendim/sendimto restrictions; return filtered text or redirection marker")
    }

    private fun getInputEditorText(): String =
        TODO("APR: use JVM equivalent - return inputEditor.getConvertedText() trimmed")

    private fun clearInputEditor() {
        TODO("APR: use JVM equivalent - inputEditor.setText(\"\")")
    }

    private fun frameDelta(): Float =
        TODO("APR: use JVM equivalent - return seconds elapsed since last frame (LLFrameTimer equivalent)")

    private fun isInVisibleChain(): Boolean =
        TODO("APR: use JVM equivalent - check if floater is in visible UI hierarchy")

    companion object {
        private const val MAX_MSG_BUF_SIZE = 4096
    }
}

// ─── Supporting enums / stubs ─────────────────────────────────────────────────

enum class InstantMessageType {
    IM_NOTHING_SPECIAL,
    IM_SESSION_P2P_INVITE,
    IM_SESSION_GROUP_START,
    IM_SESSION_CONFERENCE_START,
    IM_SESSION_INVITE,
    IM_COUNT,
}

enum class VoiceChannelState {
    STATE_CALL_STARTED, STATE_CONNECTED, STATE_HUNG_UP, STATE_RINGING, STATE_NO_CHANNEL
}

enum class VoiceChannelDirection { INCOMING_CALL, OUTGOING_CALL }

enum class VoiceStatusType { STATUS_JOINING, STATUS_LEFT_CHANNEL, STATUS_VOICE_ENABLED, STATUS_VOICE_DISABLED }

abstract class FloaterIMSessionTab(var sessionID: UUID) {
    var isNearbyChat: Boolean = false
    var isP2PChat: Boolean = false

    open fun postBuild(): Boolean = true
    open fun refresh() {}
    open fun draw() {}
    open fun onClose(appQuitting: Boolean) {}
    open fun onTearOffClicked() {}
    open fun setMinimized(value: Boolean) {}
    open fun setFocus(focus: Boolean) {}
    open fun updateSessionName(name: String) {}
}
