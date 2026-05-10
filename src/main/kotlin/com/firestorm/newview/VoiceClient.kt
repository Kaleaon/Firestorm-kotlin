// Converted from llvoiceclient.h / llvoiceclient.cpp (Firestorm / Linden Research)
// LGPL-2.1-only — see project root for full license text.

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

// ---------------------------------------------------------------------------
// Supporting data types
// ---------------------------------------------------------------------------

/** Friendly + internal name pair for a physical audio device. */
data class VoiceDevice(
    val displayName: String,
    val fullName: String,
)

/** Information about the voice-server plugin that is currently active. */
data class VoiceVersionInfo(
    val voiceServerType: String,
    val internalVoiceServerType: String,
    val majorVersion: Int,
    val minorVersion: Int,
    val serverVersion: String,
    val buildVersion: String,
)

/** A single participant visible in the current voice channel. */
data class VoiceParticipant(
    val id: LLUUID,
    val name: String,
    val isSpeaking: Boolean,
    /** Normalised volume received from this participant, 0..1. */
    val volume: Float,
    /**
     * Amplitude-derived "power" value.
     * Mirrors LLVoiceClient::getCurrentPower().
     */
    val power: Float,
    val isAvatar: Boolean = true,
    val isModeratorMuted: Boolean = false,
)

// ---------------------------------------------------------------------------
// Observer interfaces
// ---------------------------------------------------------------------------

/**
 * Implemented by UI panels that need to react to voice channel / login
 * state changes.
 *
 * Mirrors LLVoiceClientStatusObserver.
 */
interface VoiceObserver {
    enum class StatusType {
        STATUS_LOGIN_RETRY,
        STATUS_LOGGED_IN,
        STATUS_JOINING,
        STATUS_JOINED,
        STATUS_LEFT_CHANNEL,
        STATUS_VOICE_DISABLED,
        STATUS_VOICE_ENABLED,
        BEGIN_ERROR_STATUS,
        ERROR_CHANNEL_FULL,
        ERROR_CHANNEL_LOCKED,
        ERROR_NOT_AVAILABLE,
        ERROR_UNKNOWN,
    }

    fun onChange(status: StatusType, channelInfo: Map<String, Any?>, proximal: Boolean)

    companion object {
        fun statusToString(status: StatusType): String = status.name
    }
}

/**
 * Implemented by UI panels that display the participant list.
 *
 * Mirrors LLVoiceClientParticipantObserver.
 */
interface VoiceParticipantObserver {
    fun onParticipantsChanged()
}

/**
 * Implemented by the voice module that supports P2P calls (e.g. Vivox).
 *
 * Mirrors LLVoiceP2POutgoingCallInterface.
 */
interface VoiceP2POutgoingCallInterface {
    fun callUser(agentId: LLUUID)
    fun hangup()
}

/**
 * Interface presented to the call-accept / call-decline dialog.
 *
 * Mirrors LLVoiceP2PIncomingCallInterface.
 */
interface VoiceP2PIncomingCallInterface {
    fun answerInvite(): Boolean
    fun declineInvite()
}

// ---------------------------------------------------------------------------
// Voice power levels (Firestorm extension — FS:Ansariel)
// ---------------------------------------------------------------------------

/**
 * Discrete power-level buckets used to drive the in-world speaking indicator.
 *
 * Mirrors EVoicePowerLevel.
 */
enum class VoicePowerLevel {
    MUTED,
    PTT_OFF,
    PTT_ON,
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
}

// ---------------------------------------------------------------------------
// Main singleton
// ---------------------------------------------------------------------------

/**
 * Voice-chat client abstraction layer.
 *
 * Delegates all actual voice work to a pluggable [VoiceModuleInterface]
 * (Vivox or WebRTC).  The singleton owns the active spatial and
 * non-spatial voice modules and routes all calls to the appropriate one.
 *
 * Mirrors LLVoiceClient (LLParamSingleton) in llvoiceclient.h.
 * IPC and Boost.Signals2 plumbing is stubbed with [TODO].
 */
object VoiceClient {

    // ---- constants ----

    /** Power level at which a participant is considered "overdriven". */
    const val OVERDRIVEN_POWER_LEVEL: Float = 0.7f

    // Firestorm centralized power-level thresholds
    val POWER_LEVEL_0: Float = OVERDRIVEN_POWER_LEVEL / 3f
    val POWER_LEVEL_1: Float = OVERDRIVEN_POWER_LEVEL * 2f / 3f
    val POWER_LEVEL_2: Float = OVERDRIVEN_POWER_LEVEL

    const val VOLUME_MIN: Float = 0f
    const val VOLUME_DEFAULT: Float = 0.5f
    const val VOLUME_MAX: Float = 1f

    // ---- state ----

    /**
     * True when voice is globally enabled in preferences.
     * Writing this broadcasts the change to the active voice module.
     */
    var isVoiceEnabled: Boolean = false
        set(value) {
            field = value
            TODO("IPC: spatialVoiceModule?.setVoiceEnabled(value)")
        }

    /** True when the user is currently inside a voice channel. */
    var isInVoiceChannel: Boolean = false
        private set

    /** True while the microphone is locally muted (e.g. window minimised). */
    var isMicMuted: Boolean = false
        private set

    /** True when the user is using push-to-talk mode. */
    var usePTT: Boolean = true

    /** True when PTT is a toggle rather than a hold. */
    var pttIsToggle: Boolean = false

    /** Current PTT button state. */
    var userPTTState: Boolean = false
        private set

    private val statusObservers: MutableList<VoiceObserver> = mutableListOf()
    private val participantObservers: MutableList<VoiceParticipantObserver> = mutableListOf()

    // ---- initialisation ----

    /**
     * One-time startup; initialises the underlying voice modules.
     * [pump] is the C++-side IO pump handle (opaque in Kotlin).
     */
    fun init(pump: Any?) {
        TODO("IPC: init LLVivoxVoiceClient and LLWebRTCVoiceClient with pump")
    }

    /** Clean shutdown; call before the viewer exits. */
    fun terminate() {
        isInVoiceChannel = false
        TODO("IPC: spatialVoiceModule?.terminate(); nonSpatialVoiceModule?.terminate()")
    }

    /** Re-read all voice-related settings and push them to the active module. */
    fun updateSettings() {
        TODO("IPC: spatialVoiceModule?.updateSettings()")
    }

    /** Returns true when a voice module is connected and a channel is active. */
    fun isVoiceWorking(): Boolean =
        TODO("IPC: spatialVoiceModule?.isVoiceWorking() ?: false")

    // ---- version ----

    fun getVersion(): VoiceVersionInfo =
        TODO("IPC: spatialVoiceModule?.getVersion()")

    // ---- tuning ----

    fun tuningStart() { TODO("IPC: spatialVoiceModule?.tuningStart()") }
    fun tuningStop() { TODO("IPC: spatialVoiceModule?.tuningStop()") }
    fun inTuningMode(): Boolean = TODO("IPC: spatialVoiceModule?.inTuningMode() ?: false")
    fun tuningSetMicVolume(volume: Float) { TODO("IPC: spatialVoiceModule?.tuningSetMicVolume(volume)") }
    fun tuningSetSpeakerVolume(volume: Float) { TODO("IPC: spatialVoiceModule?.tuningSetSpeakerVolume(volume)") }
    fun tuningGetEnergy(): Float = TODO("IPC: spatialVoiceModule?.tuningGetEnergy() ?: 0f")

    // ---- device management ----

    /** True when the device list is populated and the settings dialog can be shown. */
    fun deviceSettingsAvailable(): Boolean = TODO("IPC: spatialVoiceModule?.deviceSettingsAvailable() ?: false")
    fun deviceSettingsUpdated(): Boolean = TODO("IPC: spatialVoiceModule?.deviceSettingsUpdated() ?: false")

    fun refreshDeviceLists(clearCurrentList: Boolean = true) {
        TODO("IPC: spatialVoiceModule?.refreshDeviceLists(clearCurrentList)")
    }

    fun setCaptureDevice(name: String) { TODO("IPC: spatialVoiceModule?.setCaptureDevice(name)") }
    fun setRenderDevice(name: String) { TODO("IPC: spatialVoiceModule?.setRenderDevice(name)") }
    fun setHidden(hidden: Boolean) { TODO("IPC: spatialVoiceModule?.setHidden(hidden)") }

    fun getCaptureDevices(): List<VoiceDevice> = TODO("IPC: spatialVoiceModule?.getCaptureDevices()")
    fun getRenderDevices(): List<VoiceDevice> = TODO("IPC: spatialVoiceModule?.getRenderDevices()")

    // ---- channel management ----

    /** True iff the user is in a local spatial (proximal) voice channel. */
    fun inProximalChannel(): Boolean = TODO("IPC: spatialVoiceModule?.inProximalChannel() ?: false")

    /**
     * Join a non-spatial (group / estate) voice channel described by [channelInfo].
     *
     * @param channelInfo  Serialised channel credentials (equivalent to LLSD).
     * @param notifyOnFirstJoin  Fire a status notification when the join succeeds.
     * @param hangupOnLastLeave  Leave the channel automatically when all other
     *                           participants have gone.
     */
    fun joinChannel(
        uri: String,
        notifyOnFirstJoin: Boolean = true,
        hangupOnLastLeave: Boolean = true,
    ) {
        isInVoiceChannel = true
        TODO("IPC: nonSpatialVoiceModule?.setNonSpatialChannel(channelInfo, ...)")
    }

    /** Leave the current non-spatial voice channel. */
    fun leaveChannel() {
        isInVoiceChannel = false
        TODO("IPC: nonSpatialVoiceModule?.leaveNonSpatialChannel()")
    }

    fun setSpatialChannel(channelInfo: Map<String, Any?>) {
        TODO("IPC: spatialVoiceModule?.setSpatialChannel(channelInfo)")
    }

    fun activateSpatialChannel(activate: Boolean) {
        TODO("IPC: spatialVoiceModule?.processChannels(activate)")
    }

    // ---- P2P ----

    fun getOutgoingCallInterface(): VoiceP2POutgoingCallInterface? =
        TODO("IPC: spatialVoiceModule?.getOutgoingCallInterface()")

    fun getIncomingCallInterface(voiceCallInfo: Map<String, Any?>): VoiceP2PIncomingCallInterface? =
        TODO("IPC: spatialVoiceModule?.getIncomingCallInterface(voiceCallInfo)")

    // ---- volume / mic ----

    /** Set voice-output (speaker) volume. [vol] is in [VOLUME_MIN]..[VOLUME_MAX]. */
    fun setSpeakerVolume(vol: Float) {
        TODO("IPC: spatialVoiceModule?.setVoiceVolume(vol.coerceIn(VOLUME_MIN, VOLUME_MAX))")
    }

    /** Set microphone input gain. [vol] is in [VOLUME_MIN]..[VOLUME_MAX]. */
    fun setMicGain(vol: Float) {
        TODO("IPC: spatialVoiceModule?.setMicGain(vol)")
    }

    /**
     * Mute/unmute the local microphone, bypassing PTT state.
     * Used when the viewer window loses focus.
     */
    fun setMicMute(muted: Boolean) {
        isMicMuted = muted
        TODO("IPC: spatialVoiceModule?.setMuteMic(muted); updateMicMuteLogic()")
    }

    fun setUserPTTState(ptt: Boolean) {
        userPTTState = ptt
        TODO("IPC: updateMicMuteLogic()")
    }

    fun toggleUserPTTState() { setUserPTTState(!userPTTState) }

    /** Per-volume override for a specific participant. [volume] is 0..1 where 0.5 is nominal. */
    fun setUserVolume(id: LLUUID, volume: Float) {
        TODO("IPC: spatialVoiceModule?.setUserVolume(id, volume)")
    }

    // ---- participant accessors ----

    /** Return a snapshot of all current voice participants. */
    fun getParticipants(): List<VoiceParticipant> =
        TODO("IPC: build list from spatialVoiceModule?.getParticipantList()")

    fun isParticipant(speakerId: LLUUID): Boolean =
        TODO("IPC: spatialVoiceModule?.isParticipant(speakerId) ?: false")

    fun getDisplayName(id: LLUUID): String =
        TODO("IPC: spatialVoiceModule?.getDisplayName(id) ?: id.toString()")

    fun getIsSpeaking(id: LLUUID): Boolean =
        TODO("IPC: spatialVoiceModule?.getIsSpeaking(id) ?: false")

    fun getIsModeratorMuted(id: LLUUID): Boolean =
        TODO("IPC: spatialVoiceModule?.getIsModeratorMuted(id) ?: false")

    /** Returns amplitude-derived power for [id], in 0..1. */
    fun getCurrentPower(id: LLUUID): Float =
        TODO("IPC: spatialVoiceModule?.getCurrentPower(id) ?: 0f")

    fun getUserVolume(id: LLUUID): Float =
        TODO("IPC: spatialVoiceModule?.getUserVolume(id) ?: VOLUME_DEFAULT")

    /**
     * Map a raw power value to a [VoicePowerLevel] bucket.
     * Firestorm extension: FS:Ansariel — centralised voice power level.
     */
    fun getPowerLevel(id: LLUUID): VoicePowerLevel {
        val power = getCurrentPower(id)
        return when {
            power <= 0f         -> VoicePowerLevel.MUTED
            power < POWER_LEVEL_0 -> VoicePowerLevel.PTT_OFF
            power < POWER_LEVEL_1 -> VoicePowerLevel.PTT_ON
            power < POWER_LEVEL_1 -> VoicePowerLevel.LEVEL_1
            power < POWER_LEVEL_2 -> VoicePowerLevel.LEVEL_2
            else                  -> VoicePowerLevel.LEVEL_3
        }
    }

    // ---- text-IM capability queries ----

    fun isSessionTextIMPossible(id: LLUUID): Boolean =
        TODO("IPC: spatialVoiceModule?.isSessionTextIMPossible(id) ?: false")

    fun isSessionCallBackPossible(id: LLUUID): Boolean =
        TODO("IPC: spatialVoiceModule?.isSessionCallBackPossible(id) ?: false")

    // ---- observer registration ----

    fun addObserver(observer: VoiceObserver) { statusObservers.add(observer) }
    fun removeObserver(observer: VoiceObserver) { statusObservers.remove(observer) }
    fun addObserver(observer: VoiceParticipantObserver) { participantObservers.add(observer) }
    fun removeObserver(observer: VoiceParticipantObserver) { participantObservers.remove(observer) }

    // ---- SIP / channel info helpers ----

    fun sipURIFromID(id: LLUUID): String =
        TODO("IPC: spatialVoiceModule?.sipURIFromID(id)")

    fun getP2PChannelInfoTemplate(id: LLUUID): Map<String, Any?> =
        TODO("IPC: spatialVoiceModule?.getP2PChannelInfoTemplate(id)")

    // ---- region / feature callbacks ----

    /** Called when the agent moves to a new region; triggers a channel switch. */
    fun onRegionChanged() {
        TODO("IPC: query new region for VoiceServerType; call handleSimulatorFeaturesReceived")
    }

    /**
     * Apply the voice-server type advertised by the simulator feature set
     * and switch the active spatial module accordingly.
     */
    fun handleSimulatorFeaturesReceived(simulatorFeatures: Map<String, Any?>) {
        val voiceServerType = simulatorFeatures["VoiceServerType"] as? String ?: "vivox"
        TODO("IPC: setSpatialVoiceModule(voiceServerType); resume channel processing")
    }

    fun setSpatialVoiceModule(voiceServerType: String) {
        TODO("IPC: select LLVivoxVoiceClient or LLWebRTCVoiceClient from voiceServerType")
    }

    fun setNonSpatialVoiceModule(voiceServerType: String) {
        TODO("IPC: select LLVivoxVoiceClient or LLWebRTCVoiceClient from voiceServerType")
    }

    /** Called once the agent's SL credentials have been validated. */
    fun userAuthorized(userId: String, agentId: LLUUID) {
        TODO("IPC: forward to all voice modules; register region-changed callback")
    }
}

// ---------------------------------------------------------------------------
// Speaker-volume persistent storage
// ---------------------------------------------------------------------------

/**
 * Persists per-speaker volume overrides across sessions.
 *
 * Mirrors LLSpeakerVolumeStorage (LLSingleton) in llvoiceclient.h.
 */
object SpeakerVolumeStorage {

    private const val SETTINGS_FILE_NAME = "speaker_volumes.xml"

    private val speakersData: MutableMap<LLUUID, Float> = mutableMapOf()

    /**
     * Store a volume level for [speakerId].
     * Persists to disk on shutdown (see [save]).
     */
    fun storeSpeakerVolume(speakerId: LLUUID, volume: Float) {
        speakersData[speakerId] = volume.coerceIn(VoiceClient.VOLUME_MIN, VoiceClient.VOLUME_MAX)
    }

    /**
     * Retrieve the stored volume for [speakerId].
     * Returns null if no override exists.
     */
    fun getSpeakerVolume(speakerId: LLUUID): Float? = speakersData[speakerId]

    /** Remove the stored volume for [speakerId]. */
    fun removeSpeakerVolume(speakerId: LLUUID) { speakersData.remove(speakerId) }

    /** Deserialise volumes from [SETTINGS_FILE_NAME] in the user data directory. */
    fun load() { TODO("IO: parse SETTINGS_FILE_NAME into speakersData") }

    /** Serialise all volumes back to [SETTINGS_FILE_NAME]. */
    fun save() { TODO("IO: write speakersData to SETTINGS_FILE_NAME") }

    /** Called by the singleton framework before the object is destroyed. */
    fun cleanupSingleton() { save() }
}
