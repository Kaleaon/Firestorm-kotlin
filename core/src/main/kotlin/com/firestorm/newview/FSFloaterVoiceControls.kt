package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmessage.IMType
import java.util.UUID

// =============================================================================
// Supporting enumerations (local to this file)
// =============================================================================

/**
 * Identifies what kind of voice session the panel is currently attached to.
 *
 * Mirrors `FSFloaterVoiceControls::EVoiceControls`.
 */
enum class VoiceControlType {
    LOCAL_CHAT,
    GROUP_CHAT,
    AD_HOC_CHAT,
    PEER_TO_PEER,
}

/**
 * Tracks whether a speaker is expected, already in the channel, or has left.
 *
 * Mirrors `FSFloaterVoiceControls::ESpeakerState`.
 */
enum class SpeakerState {
    UNKNOWN,
    INVITED,
    JOINED,
    LEFT,
}

/**
 * Voice channel lifecycle states.
 *
 * Mirrors `LLVoiceChannel::EState`.
 * Prefixed `VC` to avoid collision with `VCChannelState` in FloaterIM.kt.
 */
enum class VCChannelState {
    NO_CHANNEL_INFO, DISCONNECTED, RINGING, CONNECTED, HUNG_UP
}

// =============================================================================
// VoiceChannel stub  (not defined elsewhere in com.firestorm.newview)
// =============================================================================

/**
 * Lightweight stub for the C++ `LLVoiceChannel` class.
 *
 * The real implementation lives in `com.firestorm.voice`; this stub satisfies
 * references within this file until that package is ported.
 */
class VCVoiceChannel {
    val sessionId: LLUUID get() = LLUUID.NULL
    val sessionName: String get() = ""
    val state: VCChannelState get() = VCChannelState.NO_CHANNEL_INFO

    fun setStateChangedCallback(cb: (VCChannelState, VCChannelState) -> Unit): Any {
        System.err.println("Platform: channel->setStateChangedCallback(...)")
        return Unit
    }

    companion object {
        val currentChannel: VCVoiceChannel?
            get() = null
    }
}

// =============================================================================
// FSFloaterVoiceControls
// =============================================================================

/**
 * Voice Control Panel — shown when the user is in a voice session.
 *
 * Provides per-participant volume sliders, a mute button, and a "Leave Call"
 * button for non-local channels.  Participant rows are kept in sync with
 * VoiceClient via [VoiceParticipantObserver].
 *
 * Mirrors `FSFloaterVoiceControls` from `fsfloatervoicecontrols.h/.cpp`.
 *
 * @param key LLSD construction key from the floater registry.
 */
class FSFloaterVoiceControls(val key: Any) : VoiceParticipantObserver {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private val speakerStateMap: MutableMap<UUID, SpeakerState> = mutableMapOf()

    private var speakerManager: SpeakerMgr? = null
    private var participants: VCParticipantList? = null
    private var avatarListWidget: AvatarList? = null
    private var voiceType: VoiceControlType = VoiceControlType.LOCAL_CHAT
    private var isModeratorMutedVoice: Boolean = false
    private var isRlvShowNearbyRestricted: Boolean = false

    private var selectedParticipant: UUID = NULL_UUID
    private var initParticipantsVoiceState: Boolean = false

    private var speakerDelayRemover: SpeakersDelayActionsStorage? = null
    private var stateChangeConnection: Any? = null

    companion object {
        private val NULL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        var currentVoiceChannel: VCVoiceChannel? = null
            private set

        /**
         * Static callback registered with the voice channel system.
         * Reconnects the floater whenever the active channel changes.
         */
        fun onCurrentChannelChanged(sessionId: LLUUID) {
            val channel = VCVoiceChannel.currentChannel
            if (channel == currentVoiceChannel) return
            val floater = FloaterReg.findInstance("fs_voice_controls") as? FSFloaterVoiceControls
            floater?.connectToChannel(channel)
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    init {
        val leftRemoveDelaySecs = SavedSettings.getFloat("VoiceParticipantLeftRemoveDelay")
        speakerDelayRemover = SpeakersDelayActionsStorage(
            mActionCallback = { id -> removeVoiceLeftParticipant(id) },
            mActionDelay = leftRemoveDelaySecs,
        )
        VoiceClient.addObserver(this)
        System.err.println("Platform: LLTransientFloaterMgr.addControlView(this); " +
            "LLAvatarNameCache.addUseDisplayNamesCallback { updateAgentModeratorState() }; " +
            "LLViewerDisplayName.addNameChangedCallback { updateAgentModeratorState() }")
    }

    fun destroy() {
        resetVoiceRemoveTimers()
        speakerDelayRemover = null
        participants = null
        System.err.println("Platform: mAvatarListRefreshConnection.disconnect(); " +
            "stateChangeConnection disconnect; " +
            "VoiceClient.removeObserver(this); " +
            "LLTransientFloaterMgr.removeControlView(this)")
    }

    fun postBuild(): Boolean {
        avatarListWidget = getChild("speakers_list")
        System.err.println("Platform: avatarListWidget.setRefreshCompleteCallback { onAvatarListRefreshed() }")

        setChildAction("leave_call_btn") { leaveCall() }

        System.err.println("Platform: volumeSlider = findChild('volume_slider'); " +
            "muteButton = findChild('mute_btn'); " +
            "rlvRestrictedText = getChild('rlv_restricted'); " +
            "rlvRestrictedText.setText(RlvStrings.getString('blocked_nearby')); " +
            "if volumeSlider != null && muteButton != null: " +
            "  avatarListWidget.setCommitCallback { onParticipantSelected() }; " +
            "  volumeSlider.setCommitCallback { onVolumeChanged() }; " +
            "  muteButton.setCommitCallback { onMuteChanged() }")

        initAgentData()
        connectToChannel(VCVoiceChannel.currentChannel)
        System.err.println("Platform: updateTransparency(TT_ACTIVE)")
        updateSession()
        return true
    }

    fun onOpen(key: Any) {
        System.err.println("Platform: LLFirstUse::speak(false)")
    }

    fun draw() {
        val agentUuid: UUID = NULL_UUID
        val isModeratorMuted = VoiceClient.getIsModeratorMuted(LLUUID(agentUuid))
        if (isModeratorMutedVoice != isModeratorMuted) {
            setModeratorMutedVoice(isModeratorMuted)
        }
        participants?.update()
        System.err.println("Platform: LLFloater::draw()")
    }

    fun setFocus(focused: Boolean) {
        System.err.println("Platform: LLFloater::setFocus(focused); updateTransparency(TT_ACTIVE)")
    }

    // -------------------------------------------------------------------------
    // VoiceParticipantObserver
    // -------------------------------------------------------------------------

    override fun onParticipantsChanged() {
        if (participants == null) return
        updateParticipantsVoiceState()
        for (id in getVoiceParticipantUuids()) {
            participants?.addAvatarExceptAgent(id)
        }
    }

    // -------------------------------------------------------------------------
    // Channel / session management
    // -------------------------------------------------------------------------

    private fun leaveCall() {
        val channel = VCVoiceChannel.currentChannel ?: return
        System.err.println("Platform: gIMMgr.endCall(channel.sessionId)")
    }

    private fun updateSession() {
        val voiceChannel = VCVoiceChannel.currentChannel

        if (voiceChannel != null) {
            val currentMgrId = speakerManager?.getSessionID()
            if (currentMgrId != null &&
                voiceChannel.sessionId == LLUUID(currentMgrId)
            ) return
            speakerManager = null
        }

        val sessionId = voiceChannel?.sessionId ?: LLUUID.NULL
        val imSession = IMMgr.getSession(sessionId)

        if (imSession != null) {
            System.err.println("Platform: speakerManager = LLIMModel.getSpeakerManager(sessionId)")
            voiceType = when (imSession.type) {
                IMType.NOTHING_SPECIAL,
                IMType.SESSION_P2P_INVITE -> VoiceControlType.PEER_TO_PEER
                IMType.SESSION_CONFERENCE_START,
                IMType.SESSION_GROUP_START,
                IMType.SESSION_INVITE -> {
                    System.err.println("Platform: if gAgent.isInGroup(sessionId) GROUP_CHAT else AD_HOC_CHAT")
                    VoiceControlType.GROUP_CHAT
                }
                else -> VoiceControlType.GROUP_CHAT
            }
        }

        if (speakerManager == null) {
            speakerManager = LocalSpeakerMgr
            voiceType = VoiceControlType.LOCAL_CHAT
        }

        updateTitle()

        val isLocalChat = voiceType == VoiceControlType.LOCAL_CHAT
        setChildVisible("leave_call_btn_panel", !isLocalChat)

        refreshParticipantList()
        updateAgentModeratorState()

        if (!isLocalChat && voiceChannel != null &&
            voiceChannel.state == VCChannelState.CONNECTED
        ) {
            System.err.println("Platform: find FSFloaterIM for sessionId; if not visible call setVisible(true)")
        }

        avatarListWidget?.setRlvCheckShowNames(isLocalChat)
    }

    private fun refreshParticipantList() {
        updateListVisibility()

        val mgr = speakerManager ?: return
        val list = avatarListWidget ?: return

        participants = VCParticipantList(
            speakerMgr = mgr,
            avatarList = list,
            allowRemovalOfAgent = voiceType != VoiceControlType.GROUP_CHAT &&
                voiceType != VoiceControlType.AD_HOC_CHAT,
        ).also { pl ->
            pl.validateSpeakerCallback = { id -> validateSpeaker(id) }
            pl.sortOrder = SavedSettings.getFloat("SpeakerParticipantDefaultOrder").toInt()
        }

        if (speakerManager == LocalSpeakerMgr) {
            System.err.println("Platform: avatarListWidget.setNoItemsCommentText(getString('no_one_near'))")
        }

        initParticipantsVoiceState = true
    }

    private fun onAvatarListRefreshed() {
        if (initParticipantsVoiceState) {
            initParticipantsVoiceState()
            initParticipantsVoiceState = false
        } else {
            updateParticipantsVoiceState()
        }
    }

    // -------------------------------------------------------------------------
    // Per-participant controls
    // -------------------------------------------------------------------------

    private fun onParticipantSelected() {
        System.err.println("Platform: get selected UUIDs from avatarListWidget; " +
            "if exactly one: enable volumeSlider and muteButton, " +
            "set muteButton toggleState from VoiceClient.getOnMuteList(id), " +
            "set volumeSlider value from VoiceClient.getUserVolume(id); " +
            "else disable both controls")
    }

    private fun onVolumeChanged() {
        if (selectedParticipant == NULL_UUID) return
        System.err.println("Platform: VoiceClient.setUserVolume(LLUUID(selectedParticipant), volumeSlider.getValueF32())")
    }

    private fun onMuteChanged() {
        if (selectedParticipant == NULL_UUID) return
        System.err.println("Platform: get AvatarListItem for selectedParticipant; " +
            "build LLMute; add or remove from LLMuteList with flagVoiceChat")
    }

    // -------------------------------------------------------------------------
    // Title
    // -------------------------------------------------------------------------

    private fun updateTitle() {
        val voiceChannel = VCVoiceChannel.currentChannel
        if (voiceType == VoiceControlType.PEER_TO_PEER && voiceChannel != null) {
            val sessionId = voiceChannel.sessionId
            val imSession = IMMgr.getSession(sessionId)
            if (imSession != null) {
                System.err.println("Platform: LLAvatarNameCache.get(imSession.targetId) { av_name -> " +
                    "setTitle(getString('title_peer_2_peer').replace('[NAME]', av_name.completeName)) }")
                return
            }
        }

        val title = when (voiceType) {
            VoiceControlType.LOCAL_CHAT  -> getString("title_nearby")
            VoiceControlType.PEER_TO_PEER -> {
                val name = voiceChannel?.sessionName ?: ""
                getString("title_peer_2_peer").replace("[NAME]", name)
            }
            VoiceControlType.AD_HOC_CHAT -> getString("title_adhoc")
            VoiceControlType.GROUP_CHAT  -> {
                val group = voiceChannel?.sessionName ?: ""
                getString("title_group").replace("[GROUP]", group)
            }
        }
        setTitle(title)
    }

    // -------------------------------------------------------------------------
    // Agent panel
    // -------------------------------------------------------------------------

    private fun initAgentData() {
        System.err.println("Platform: mAgentPanel = getChild('my_panel'); " +
            "set user_icon value to gAgentID; " +
            "LLAvatarNameCache.get(gAgentID) { av_name -> set user_text to av_name.displayName }; " +
            "mSpeakingIndicator = mAgentPanel.getChild('speaking_indicator'); " +
            "mSpeakingIndicator.setSpeakerId(gAgentID)")
    }

    private fun setModeratorMutedVoice(moderatorMuted: Boolean) {
        isModeratorMutedVoice = moderatorMuted
        if (moderatorMuted) {
            System.err.println("Platform: LLNotificationsUtil.add('VoiceIsMutedByModerator')")
        }
        System.err.println("Platform: mSpeakingIndicator.setIsModeratorMuted(moderatorMuted)")
    }

    private fun onModeratorNameCache(displayName: String) {
        var name = displayName
        val mgr = speakerManager
        if (mgr != null) {
            val agentId: UUID = NULL_UUID
            val speaker = mgr.findSpeaker(agentId)
            if (speaker != null && speaker.isModerator) {
                name += " " + getString("IM_moderator_label")
            }
        }
        System.err.println("Platform: mAgentPanel.getChild<LLUICtrl>('user_text').setValue(name)")
    }

    private fun updateAgentModeratorState() {
        System.err.println("Platform: LLAvatarNameCache.get(gAgentID) { _, avName -> onModeratorNameCache(avName.displayName) }")
    }

    // -------------------------------------------------------------------------
    // Voice participant state tracking
    // -------------------------------------------------------------------------

    private fun initParticipantsVoiceState() {
        val speakerUuids = getVoiceParticipantUuids()
        forEachAvatarItem { item ->
            val speakerId = item.getAvatarId()
            if (speakerId in speakerUuids) {
                setState(item, SpeakerState.JOINED)
            } else {
                val speaker = speakerManager?.findSpeaker(speakerId)
                if (speaker != null && speaker.hasLeftCurrentCall) {
                    setState(item, SpeakerState.LEFT)
                } else {
                    setState(item, SpeakerState.INVITED)
                }
            }
        }
    }

    private fun updateParticipantsVoiceState() {
        val speakerUuids = getVoiceParticipantUuids().toMutableList()
        forEachAvatarItem { item ->
            val participantId = item.getAvatarId()
            val idx = speakerUuids.indexOf(participantId)
            if (idx != -1) {
                setState(item, SpeakerState.JOINED)
                speakerManager?.findSpeaker(participantId)?.hasLeftCurrentCall = false
                speakerUuids.removeAt(idx)
            } else {
                updateNotInVoiceParticipantState(item)
            }
        }
    }

    private fun updateNotInVoiceParticipantState(item: AvatarListItem) {
        when (getState(item.getAvatarId())) {
            SpeakerState.JOINED -> {
                setState(item, SpeakerState.LEFT)
                speakerManager?.findSpeaker(item.getAvatarId())?.hasLeftCurrentCall = true
            }
            SpeakerState.LEFT -> { /* stable terminal state */ }
            SpeakerState.INVITED, SpeakerState.UNKNOWN -> setState(item, SpeakerState.INVITED)
        }
    }

    private fun setState(item: AvatarListItem, state: SpeakerState) {
        val agentId: UUID = NULL_UUID
        // The agent's own row must never be shown as "Has Left".
        if (state == SpeakerState.LEFT && item.getAvatarId() == agentId) return

        setState(item.getAvatarId(), state)

        when (state) {
            SpeakerState.INVITED -> item.setState(ItemState.IS_VOICE_INVITED)
            SpeakerState.JOINED  -> {
                removeVoiceRemoveTimer(item.getAvatarId())
                item.setState(ItemState.IS_VOICE_JOINED)
            }
            SpeakerState.LEFT    -> {
                setVoiceRemoveTimer(item.getAvatarId())
                item.setState(ItemState.IS_VOICE_LEFT)
            }
            SpeakerState.UNKNOWN -> { /* no visual change */ }
        }
    }

    private fun setState(speakerId: UUID, state: SpeakerState) {
        speakerStateMap[speakerId] = state
    }

    private fun getState(speakerId: UUID): SpeakerState =
        speakerStateMap[speakerId] ?: SpeakerState.UNKNOWN

    // -------------------------------------------------------------------------
    // Delayed-removal timers
    // -------------------------------------------------------------------------

    private fun setVoiceRemoveTimer(speakerId: UUID) {
        speakerDelayRemover?.setActionTimer(speakerId)
    }

    private fun removeVoiceLeftParticipant(speakerId: UUID): Boolean {
        System.err.println("Platform: avatarListWidget.getIDs().remove(speakerId); avatarListWidget.setDirty()")
        return false
    }

    private fun resetVoiceRemoveTimers() {
        speakerDelayRemover?.removeAllTimers()
    }

    private fun removeVoiceRemoveTimer(speakerId: UUID) {
        speakerDelayRemover?.unsetActionTimer(speakerId)
    }

    // -------------------------------------------------------------------------
    // Speaker validation
    // -------------------------------------------------------------------------

    private fun validateSpeaker(speakerId: UUID): Boolean = when (voiceType) {
        VoiceControlType.LOCAL_CHAT ->
            speakerId in getVoiceParticipantUuids()
        VoiceControlType.GROUP_CHAT ->
            getState(speakerId) != SpeakerState.LEFT
        else -> true
    }

    // -------------------------------------------------------------------------
    // Channel state machine
    // -------------------------------------------------------------------------

    fun connectToChannel(channel: VCVoiceChannel?) {
        System.err.println("Platform: stateChangeConnection disconnect")
        currentVoiceChannel = channel
        if (channel != null) {
            stateChangeConnection = channel.setStateChangedCallback { old, new ->
                onVoiceChannelStateChanged(old, new)
            }
            updateState(channel.state)
        }
    }

    private fun onVoiceChannelStateChanged(oldState: VCChannelState, newState: VCChannelState) {
        if (VoiceClient.isVoiceWorking()) {
            updateState(newState)
        } else {
            System.err.println("Platform: closeFloater()")
        }
    }

    private fun updateState(newState: VCChannelState) {
        if (newState == VCChannelState.CONNECTED) {
            updateSession()
        } else {
            reset(newState)
        }
    }

    private fun reset(newState: VCChannelState) {
        resetVoiceRemoveTimers()
        speakerStateMap.clear()
        participants = null
        System.err.println("Platform: avatarListWidget.clear()")

        when {
            !ParcelMgr.allowAgentVoice() && newState == VCChannelState.HUNG_UP -> {
                setChildVisible("leave_call_btn_panel", false)
                setTitle(getString("title_nearby"))
                System.err.println("Platform: avatarListWidget.setNoItemsCommentText(getString('no_one_near'))")
            }
            newState == VCChannelState.RINGING -> {
                System.err.println("Platform: avatarListWidget.setNoItemsCommentText(getString('LoadingData'))")
            }
        }

        updateListVisibility()
        speakerManager = null
    }

    // -------------------------------------------------------------------------
    // RLVa restriction
    // -------------------------------------------------------------------------

    fun toggleRlvShowNearbyRestriction(restricted: Boolean) {
        isRlvShowNearbyRestricted = restricted
        updateListVisibility()
    }

    private fun updateListVisibility() {
        val hideList = isRlvShowNearbyRestricted && voiceType == VoiceControlType.LOCAL_CHAT
        System.err.println("Platform: avatarListWidget.setVisible(!hideList); rlvRestrictedText.setVisible(hideList)")
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun getVoiceParticipantUuids(): List<UUID> {
        System.err.println("Platform: LLVoiceClient.getInstance().getParticipantList(participants); " +
            "convert participant set to List<java.util.UUID>")
        return emptyList()
    }

    private fun forEachAvatarItem(action: (AvatarListItem) -> Unit) {
        System.err.println("Platform: avatarListWidget.getItems(panels); " +
            "panels.filterIsInstance<AvatarListItem>().forEach(action)")
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    private fun getString(key: String): String {
        System.err.println("Platform: getString(\"$key\") / LLTrans::getString(\"$key\")")
        return ""
    }

    private fun setTitle(title: String): Unit {
        System.err.println("Platform: LLFloater::setTitle(\"$title\")")
    }

    private fun setChildVisible(name: String, visible: Boolean): Unit {
        System.err.println("Platform: getChildView(\"$name\").setVisible($visible)")
    }

    private fun setChildAction(name: String, action: () -> Unit): Unit {
        System.err.println("Platform: childSetAction(\"$name\", action)")
    }

    private fun <T> getChild(name: String): T? = null
}

// =============================================================================
// VCParticipantList — participant list stub (prefixed to avoid collision)
// =============================================================================

/**
 * Manages the avatar list widget for voice participants.
 *
 * Mirrors `FSParticipantList` from `fsparticipantlist.h/.cpp`.
 */
class VCParticipantList(
    private val speakerMgr: SpeakerMgr,
    private val avatarList: AvatarList,
    private val allowRemovalOfAgent: Boolean,
) {
    var validateSpeakerCallback: ((UUID) -> Boolean)? = null
    var sortOrder: Int = 0

    fun addAvatarExceptAgent(id: UUID): Unit {
        System.err.println("Platform: mParticipants->addAvatarIDExceptAgent(id)")
    }

    fun update(): Unit {
        System.err.println("Platform: mParticipants->update()")
    }
}
