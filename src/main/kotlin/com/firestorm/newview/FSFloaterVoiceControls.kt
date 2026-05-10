package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// =============================================================================
// Supporting enumerations
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

// =============================================================================
// FSFloaterVoiceControls
// =============================================================================

/**
 * Voice Control Panel — shown when the user is in a voice session.
 *
 * Provides per-participant volume sliders, a mute button, and a "Leave Call"
 * button for non-local channels.  Participant rows are kept in sync with
 * LLVoiceClient via [VoiceParticipantObserver].
 *
 * Mirrors `FSFloaterVoiceControls` from `fsfloatervoicecontrols.h/.cpp`.
 *
 * @param key LLSD construction key from the floater registry.
 */
class FSFloaterVoiceControls(val key: Any) : VoiceParticipantObserver {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private val speakerStateMap: MutableMap<LLUUID, SpeakerState> = mutableMapOf()

    private var speakerManager: SpeakerMgr? = null
    private var participantList: ParticipantList? = null
    private var avatarList: AvatarList? = null
    private var voiceType: VoiceControlType = VoiceControlType.LOCAL_CHAT
    private var agentPanel: Panel? = null
    private var speakingIndicator: OutputMonitorCtrl? = null
    private var isModeratorMutedVoice: Boolean = false
    private var isRlvShowNearbyRestricted: Boolean = false

    private var selectedParticipant: LLUUID = LLUUID.nullId()
    private var volumeSlider: SliderCtrl? = null
    private var muteButton: Button? = null
    private var rlvRestrictedText: TextBox? = null

    private var initParticipantsVoiceState: Boolean = false

    private var speakerDelayRemover: SpeakersDelayActionsStorage? = null

    companion object {
        var currentVoiceChannel: VoiceChannel? = null
            private set

        /**
         * Static callback registered with the voice channel system.
         *
         * Reconnects the floater whenever the active channel changes.
         */
        fun onCurrentChannelChanged(sessionId: LLUUID) {
            val channel = VoiceChannel.currentChannel
            if (channel == currentVoiceChannel) return
            val floater = FloaterReg.findInstance<FSFloaterVoiceControls>("fs_voice_controls")
            floater?.connectToChannel(channel)
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    init {
        val leftRemoveDelay = SavedSettings.getInt("VoiceParticipantLeftRemoveDelay").toFloat()
            .coerceAtLeast(10f)
        speakerDelayRemover = SpeakersDelayActionsStorage(
            action = { id -> removeVoiceLeftParticipant(id) },
            delaySeconds = leftRemoveDelay,
        )
        VoiceClient.addObserver(this)
        TODO("Platform: LLTransientFloaterMgr.addControlView(this); " +
            "LLAvatarNameCache.addUseDisplayNamesCallback { updateAgentModeratorState() }; " +
            "LLViewerDisplayName.addNameChangedCallback { updateAgentModeratorState() }")
    }

    fun destroy() {
        resetVoiceRemoveTimers()
        participantList = null
        TODO("Platform: mAvatarListRefreshConnection.disconnect(); " +
            "mVoiceChannelStateChangeConnection.disconnect(); " +
            "LLVoiceClient.removeObserver(this); " +
            "LLTransientFloaterMgr.removeControlView(this)")
    }

    fun postBuild(): Boolean {
        avatarList = getChild("speakers_list")
        TODO("Platform: mAvatarListRefreshConnection = avatarList.setRefreshCompleteCallback { onAvatarListRefreshed() }")

        setChildAction("leave_call_btn") { leaveCall() }

        volumeSlider = findChild("volume_slider")
        muteButton = findChild("mute_btn")
        rlvRestrictedText = getChild("rlv_restricted")
        rlvRestrictedText?.setText(RlvStrings.getString("blocked_nearby"))

        if (volumeSlider != null && muteButton != null) {
            avatarList?.onCommit = { onParticipantSelected() }
            volumeSlider?.onCommit = { onVolumeChanged() }
            muteButton?.onCommit = { onMuteChanged() }
        }

        initAgentData()
        connectToChannel(VoiceChannel.currentChannel)
        TODO("Platform: updateTransparency(TT_ACTIVE)")
        updateSession()
        return true
    }

    fun onOpen(key: Any) {
        TODO("Platform: LLFirstUse::speak(false)")
    }

    fun draw() {
        val isModeratorMuted = VoiceClient.getIsModeratorMuted(Agent.id)
        if (isModeratorMutedVoice != isModeratorMuted) {
            setModeratorMutedVoice(isModeratorMuted)
        }
        participantList?.update()
        TODO("Platform: LLFloater::draw()")
    }

    fun setFocus(focused: Boolean) {
        TODO("Platform: LLFloater::setFocus(focused); updateTransparency(TT_ACTIVE)")
    }

    // -------------------------------------------------------------------------
    // VoiceParticipantObserver
    // -------------------------------------------------------------------------

    override fun onParticipantsChanged() {
        if (participantList == null) return
        updateParticipantsVoiceState()

        val speakerUuids = getVoiceParticipantUuids()
        for (id in speakerUuids) {
            participantList?.addAvatarExceptAgent(id)
        }
    }

    // -------------------------------------------------------------------------
    // Channel / session management
    // -------------------------------------------------------------------------

    private fun leaveCall() {
        val channel = VoiceChannel.currentChannel ?: return
        TODO("Platform: gIMMgr.endCall(channel.sessionId)")
    }

    private fun updateSession() {
        val voiceChannel = VoiceChannel.currentChannel

        if (voiceChannel != null) {
            val currentSessionId = speakerManager?.sessionId
            if (currentSessionId != null && voiceChannel.sessionId == currentSessionId) return
            speakerManager = null
        }

        val sessionId = voiceChannel?.sessionId ?: LLUUID.nullId()
        val imSession = ImModel.findSession(sessionId)

        if (imSession != null) {
            speakerManager = ImModel.getSpeakerManager(sessionId)
            voiceType = when (imSession.type) {
                ImSessionType.NOTHING_SPECIAL,
                ImSessionType.P2P_INVITE -> VoiceControlType.PEER_TO_PEER
                ImSessionType.CONFERENCE_START,
                ImSessionType.GROUP_START,
                ImSessionType.INVITE ->
                    if (Agent.isInGroup(sessionId)) VoiceControlType.GROUP_CHAT
                    else VoiceControlType.AD_HOC_CHAT
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
            voiceChannel.state == VoiceChannelState.CONNECTED
        ) {
            val imFloater = FloaterReg.findInstance<Any>("fs_im_${sessionId}")
            val showMe = imFloater == null || !isFloaterVisible(imFloater)
            if (showMe) setVisible(true)
        }

        avatarList?.checkRlvShowNames = isLocalChat
    }

    private fun refreshParticipantList() {
        updateListVisibility()
        check(participantList == null) { "Possible memory leak: participantList was not cleared" }

        val mgr = speakerManager ?: return
        participantList = ParticipantList(
            speakerMgr = mgr,
            avatarList = avatarList!!,
            sortByRecentSpeaker = true,
            excludeGroupAndAdHoc = voiceType != VoiceControlType.GROUP_CHAT &&
                voiceType != VoiceControlType.AD_HOC_CHAT,
            showTextChatters = false,
        ).also { pl ->
            pl.validateSpeakerCallback = { id -> validateSpeaker(id) }
            val sortOrder = SavedSettings.getInt("SpeakerParticipantDefaultOrder")
            pl.sortOrder = sortOrder
        }

        if (speakerManager == LocalSpeakerMgr) {
            avatarList?.noItemsText = getString("no_one_near")
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
        val list = avatarList ?: return
        val participants = list.selectedUuids()

        volumeSlider?.isEnabled = false
        muteButton?.isEnabled = false
        selectedParticipant = LLUUID.nullId()

        if (participants.size != 1) return
        selectedParticipant = participants[0]
        if (selectedParticipant.isNull()) return
        if (!VoiceClient.isParticipant(selectedParticipant)) return

        volumeSlider?.isEnabled = true
        muteButton?.isEnabled = true
        muteButton?.toggleState = MuteList.isMuted(selectedParticipant)
        volumeSlider?.value = VoiceClient.getUserVolume(selectedParticipant)
    }

    private fun onVolumeChanged() {
        if (selectedParticipant.isNull()) return
        VoiceClient.setUserVolume(selectedParticipant, volumeSlider?.value ?: return)
    }

    private fun onMuteChanged() {
        if (selectedParticipant.isNull()) return
        val item = avatarList?.itemByValue(selectedParticipant) as? AvatarListItem ?: return
        val mute = Mute(id = selectedParticipant, name = item.avatarName, type = MuteType.AGENT)
        if (muteButton?.toggleState == true) {
            MuteList.add(mute, MuteList.FLAG_VOICE_CHAT)
        } else {
            MuteList.remove(mute, MuteList.FLAG_VOICE_CHAT)
        }
    }

    // -------------------------------------------------------------------------
    // Title
    // -------------------------------------------------------------------------

    private fun updateTitle() {
        val voiceChannel = VoiceChannel.currentChannel
        if (voiceType == VoiceControlType.PEER_TO_PEER) {
            val sessionId = voiceChannel?.sessionId ?: LLUUID.nullId()
            val imSession = ImModel.findSession(sessionId)
            if (imSession != null) {
                TODO("Platform: LLAvatarNameCache.get(imSession.otherParticipantId) { av_name -> " +
                    "setTitle(getString(\"title_peer_2_peer\", mapOf(\"NAME\" to av_name.completeName))) }")
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

    private fun onAvatarNameCache(agentId: LLUUID, avName: AvatarName) {
        setTitle(getString("title_peer_2_peer").replace("[NAME]", avName.completeName))
    }

    // -------------------------------------------------------------------------
    // Agent panel
    // -------------------------------------------------------------------------

    private fun initAgentData() {
        agentPanel = getChild("my_panel")
        agentPanel?.let { panel ->
            panel.setChildValue("user_icon", Agent.id)
            val avName = AvatarNameCache.get(Agent.id)
            panel.setChildValue("user_text", avName?.displayName ?: "")
            speakingIndicator = panel.getChild("speaking_indicator")
            speakingIndicator?.speakerId = Agent.id
        }
    }

    private fun setModeratorMutedVoice(moderatorMuted: Boolean) {
        isModeratorMutedVoice = moderatorMuted
        if (moderatorMuted) {
            TODO("Platform: LLNotificationsUtil.add(\"VoiceIsMutedByModerator\")")
        }
        speakingIndicator?.isModeratorMuted = moderatorMuted
    }

    private fun onModeratorNameCache(avName: AvatarName) {
        var name = avName.displayName
        val mgr = speakerManager
        if (mgr != null && Agent.isInGroup(mgr.sessionId)) {
            val speaker = mgr.findSpeaker(Agent.id)
            if (speaker != null && speaker.isModerator) {
                name += " " + getString("IM_moderator_label")
            }
        }
        agentPanel?.setChildValue("user_text", name)
    }

    private fun updateAgentModeratorState() {
        TODO("Platform: LLAvatarNameCache.get(Agent.id) { _, avName -> onModeratorNameCache(avName) }")
    }

    // -------------------------------------------------------------------------
    // Voice participant state tracking
    // -------------------------------------------------------------------------

    private fun initParticipantsVoiceState() {
        val items = avatarList?.items() ?: return
        val speakerUuids = getVoiceParticipantUuids()

        for (item in items) {
            val speakerId = item.avatarId
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
        val items = avatarList?.items() ?: return

        for (item in items) {
            val participantId = item.avatarId
            val speakerIdx = speakerUuids.indexOf(participantId)
            if (speakerIdx != -1) {
                setState(item, SpeakerState.JOINED)
                val speaker = speakerManager?.findSpeaker(participantId)
                if (speaker != null) speaker.hasLeftCurrentCall = false
                speakerUuids.removeAt(speakerIdx)
            } else {
                updateNotInVoiceParticipantState(item)
            }
        }
    }

    private fun updateNotInVoiceParticipantState(item: AvatarListItem) {
        when (getState(item.avatarId)) {
            SpeakerState.JOINED -> {
                setState(item, SpeakerState.LEFT)
                speakerManager?.findSpeaker(item.avatarId)?.hasLeftCurrentCall = true
            }
            SpeakerState.LEFT -> { /* nothing — stable terminal state */ }
            SpeakerState.INVITED, SpeakerState.UNKNOWN -> setState(item, SpeakerState.INVITED)
        }
    }

    private fun setState(item: AvatarListItem, state: SpeakerState) {
        // The agent's own row must never be shown as "Has Left".
        if (state == SpeakerState.LEFT && item.avatarId == Agent.id) return

        setState(item.avatarId, state)

        when (state) {
            SpeakerState.INVITED -> item.voiceState = AvatarListItemVoiceState.INVITED
            SpeakerState.JOINED  -> {
                removeVoiceRemoveTimer(item.avatarId)
                item.voiceState = AvatarListItemVoiceState.JOINED
            }
            SpeakerState.LEFT    -> {
                setVoiceRemoveTimer(item.avatarId)
                item.voiceState = AvatarListItemVoiceState.LEFT
            }
            SpeakerState.UNKNOWN -> { /* no visual change */ }
        }
    }

    private fun setState(speakerId: LLUUID, state: SpeakerState) {
        speakerStateMap[speakerId] = state
    }

    private fun getState(speakerId: LLUUID): SpeakerState =
        speakerStateMap[speakerId] ?: SpeakerState.UNKNOWN

    // -------------------------------------------------------------------------
    // Delayed-removal timers
    // -------------------------------------------------------------------------

    private fun setVoiceRemoveTimer(speakerId: LLUUID) {
        speakerDelayRemover?.setActionTimer(speakerId)
    }

    private fun removeVoiceLeftParticipant(speakerId: LLUUID): Boolean {
        avatarList?.removeId(speakerId)
        return false
    }

    private fun resetVoiceRemoveTimers() {
        speakerDelayRemover?.removeAllTimers()
    }

    private fun removeVoiceRemoveTimer(speakerId: LLUUID) {
        speakerDelayRemover?.unsetActionTimer(speakerId)
    }

    // -------------------------------------------------------------------------
    // Speaker validation
    // -------------------------------------------------------------------------

    private fun validateSpeaker(speakerId: LLUUID): Boolean = when (voiceType) {
        VoiceControlType.LOCAL_CHAT ->
            speakerId in getVoiceParticipantUuids()
        VoiceControlType.GROUP_CHAT ->
            getState(speakerId) != SpeakerState.LEFT
        else -> true
    }

    // -------------------------------------------------------------------------
    // Channel state machine
    // -------------------------------------------------------------------------

    fun connectToChannel(channel: VoiceChannel?) {
        TODO("Platform: mVoiceChannelStateChangeConnection.disconnect()")
        currentVoiceChannel = channel
        if (channel != null) {
            TODO("Platform: mVoiceChannelStateChangeConnection = channel.setStateChangedCallback { old, new -> " +
                "onVoiceChannelStateChanged(old, new) }")
            updateState(channel.state)
        }
    }

    private fun onVoiceChannelStateChanged(
        oldState: VoiceChannelState,
        newState: VoiceChannelState,
    ) {
        if (VoiceClient.isVoiceWorking()) {
            updateState(newState)
        } else {
            TODO("Platform: closeFloater()")
        }
    }

    private fun updateState(newState: VoiceChannelState) {
        if (newState == VoiceChannelState.CONNECTED) {
            updateSession()
        } else {
            reset(newState)
        }
    }

    private fun reset(newState: VoiceChannelState) {
        resetVoiceRemoveTimers()
        speakerStateMap.clear()
        participantList = null
        avatarList?.clear()

        when {
            !ParcelMgr.agentVoiceAllowed && newState == VoiceChannelState.HUNG_UP -> {
                setChildVisible("leave_call_btn_panel", false)
                setTitle(getString("title_nearby"))
                avatarList?.noItemsText = getString("no_one_near")
            }
            newState == VoiceChannelState.RINGING -> {
                avatarList?.noItemsText = getString("LoadingData")
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
        avatarList?.isVisible = !hideList
        rlvRestrictedText?.isVisible = hideList
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    private fun getString(key: String): String =
        TODO("Platform: getString(\"$key\") / LLTrans::getString(\"$key\")")

    private fun setTitle(title: String) =
        TODO("Platform: LLFloater::setTitle(\"$title\")")

    private fun setVisible(visible: Boolean) =
        TODO("Platform: setVisible($visible)")

    private fun setChildVisible(name: String, visible: Boolean) =
        TODO("Platform: getChildView(\"$name\").setVisible($visible)")

    private fun setChildAction(name: String, action: () -> Unit) =
        TODO("Platform: childSetAction(\"$name\", action)")

    private fun <T> getChild(name: String): T? =
        TODO("Platform: getChild<T>(\"$name\")")

    private fun <T> findChild(name: String): T? =
        TODO("Platform: findChild<T>(\"$name\")")

    private fun isFloaterVisible(floater: Any): Boolean =
        TODO("Platform: floater.getVisible()")

    private fun getVoiceParticipantUuids(): List<LLUUID> {
        val result = mutableListOf<LLUUID>()
        TODO("Platform: LLVoiceClient.getInstance().getParticipantList(participants); convert to list")
        return result
    }
}

// =============================================================================
// Stub types specific to this floater
// =============================================================================

/** Stub: voice channel state machine. */
enum class VoiceChannelState { NO_CHANNEL_INFO, DISCONNECTED, RINGING, CONNECTED, HUNG_UP }

/** Stub: voice channel handle. */
class VoiceChannel {
    val sessionId: LLUUID get() = TODO("Platform: voice_channel->getSessionID()")
    val sessionName: String get() = TODO("Platform: voice_channel->getSessionName()")
    val state: VoiceChannelState get() = TODO("Platform: voice_channel->getState()")

    companion object {
        val currentChannel: VoiceChannel? get() = TODO("Platform: LLVoiceChannel::getCurrentVoiceChannel()")
    }
}

/** Stub: IM session type codes. */
enum class ImSessionType {
    NOTHING_SPECIAL, P2P_INVITE, CONFERENCE_START, GROUP_START, INVITE, UNKNOWN
}

/** Stub: lightweight IM session descriptor. */
class ImSession {
    val type: ImSessionType get() = TODO("Platform: im_session->mType")
    val otherParticipantId: LLUUID get() = TODO("Platform: im_session->mOtherParticipantID")
}

/** Stub: IM model singleton. */
object ImModel {
    fun findSession(sessionId: LLUUID): ImSession? =
        TODO("Platform: LLIMModel::getInstance()->findIMSession(sessionId)")
    fun getSpeakerManager(sessionId: LLUUID): SpeakerMgr? =
        TODO("Platform: LLIMModel::getInstance()->getSpeakerManager(sessionId)")
}

/** Stub: speaker manager interface. */
interface SpeakerMgr {
    val sessionId: LLUUID
    fun findSpeaker(id: LLUUID): Speaker?
}

/** Stub: speaker record. */
class Speaker {
    val isModerator: Boolean get() = TODO("Platform: speakerp->mIsModerator")
    var hasLeftCurrentCall: Boolean = false
}

/** Stub: local (nearby) speaker manager singleton. */
object LocalSpeakerMgr : SpeakerMgr {
    override val sessionId: LLUUID get() = LLUUID.nullId()
    override fun findSpeaker(id: LLUUID): Speaker? = TODO("Platform: LLLocalSpeakerMgr::findSpeaker(id)")
}

/** Stub: participant list widget. */
class ParticipantList(
    speakerMgr: SpeakerMgr,
    avatarList: AvatarList,
    sortByRecentSpeaker: Boolean,
    excludeGroupAndAdHoc: Boolean,
    showTextChatters: Boolean,
) {
    var validateSpeakerCallback: ((LLUUID) -> Boolean)? = null
    var sortOrder: Int = 0

    fun addAvatarExceptAgent(id: LLUUID) = TODO("Platform: mParticipants->addAvatarIDExceptAgent(id)")
    fun update() = TODO("Platform: mParticipants->update()")
}

/** Stub: avatar list widget. */
class AvatarList {
    var noItemsText: String = ""
    var isVisible: Boolean = true
    var checkRlvShowNames: Boolean = false
    var onCommit: (() -> Unit)? = null

    fun selectedUuids(): List<LLUUID> = TODO("Platform: mAvatarList->getSelectedUUIDs(participants)")
    fun items(): List<AvatarListItem> = TODO("Platform: mAvatarList->getItems(items)")
    fun itemByValue(id: LLUUID): Any? = TODO("Platform: mAvatarList->getItemByValue(id)")
    fun removeId(id: LLUUID) = TODO("Platform: speaker_uuids.erase(pos); mAvatarList->setDirty()")
    fun clear() = TODO("Platform: mAvatarList->clear()")
}

/** Stub: avatar list item. */
class AvatarListItem {
    val avatarId: LLUUID get() = TODO("Platform: item->getAvatarId()")
    val avatarName: String get() = TODO("Platform: item->getAvatarName()")
    var voiceState: AvatarListItemVoiceState = AvatarListItemVoiceState.INVITED
}

enum class AvatarListItemVoiceState { INVITED, JOINED, LEFT }

/** Stub: output (speaking) monitor widget. */
class OutputMonitorCtrl {
    var speakerId: LLUUID = LLUUID.nullId()
    var isModeratorMuted: Boolean = false
}

/** Stub: slider widget. */
class SliderCtrl {
    var isEnabled: Boolean = false
    var value: Float = 0f
    var onCommit: (() -> Unit)? = null
}

/** Stub: button widget. */
class Button {
    var isEnabled: Boolean = false
    var toggleState: Boolean = false
    var onCommit: (() -> Unit)? = null
}

/** Stub: text label widget. */
class TextBox {
    var isVisible: Boolean = true
    fun setText(text: String) = TODO("Platform: mRlvRestrictedText->setText(text)")
}

/** Stub: Panel widget with named child lookup. */
open class Panel {
    fun <T> getChild(name: String): T? = TODO("Platform: panel.getChild<T>(\"$name\")")
    fun setChildValue(name: String, value: Any) = TODO("Platform: panel.getChild(\"$name\").setValue(value)")
}

/** Stub: avatar name data. */
class AvatarName {
    val displayName: String get() = TODO("Platform: av_name.getDisplayName()")
    val completeName: String get() = TODO("Platform: av_name.getCompleteName()")
}

/** Stub: avatar name cache. */
object AvatarNameCache {
    fun get(id: LLUUID): AvatarName? = TODO("Platform: LLAvatarNameCache::get(id, &av_name)")
}

/** Stub: mute entry and mute list. */
data class Mute(val id: LLUUID, val name: String, val type: MuteType)
enum class MuteType { AGENT, OBJECT, GROUP, BY_NAME }

object MuteList {
    const val FLAG_VOICE_CHAT: Int = 1 shl 5
    fun add(mute: Mute, flag: Int) = TODO("Platform: LLMuteList::instance().add(mute, flag)")
    fun remove(mute: Mute, flag: Int) = TODO("Platform: LLMuteList::instance().remove(mute, flag)")
    fun isMuted(id: LLUUID): Boolean = TODO("Platform: LLVoiceClient::instance().getOnMuteList(id)")
}

/** Stub: RLVa string lookup. */
object RlvStrings {
    fun getString(key: String): String = TODO("Platform: RlvStrings::getString(\"$key\")")
}

/** Stub: parcel access policy. */
object ParcelMgr {
    val agentVoiceAllowed: Boolean get() = TODO("Platform: LLViewerParcelMgr::getInstance()->allowAgentVoice()")
}

/** Stub: delayed action storage (fires callback after a per-ID timer expires). */
class SpeakersDelayActionsStorage(
    private val action: (LLUUID) -> Boolean,
    private val delaySeconds: Float,
) {
    fun setActionTimer(id: LLUUID) = TODO("Platform: mSpeakerDelayRemover->setActionTimer(id)")
    fun unsetActionTimer(id: LLUUID) = TODO("Platform: mSpeakerDelayRemover->unsetActionTimer(id)")
    fun removeAllTimers() = TODO("Platform: mSpeakerDelayRemover->removeAllTimers()")
}

/** Stub: agent group membership check. */
fun Agent.isInGroup(groupId: LLUUID): Boolean =
    TODO("Platform: gAgent.isInGroup(groupId)")
