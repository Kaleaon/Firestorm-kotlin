package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

data class VoiceDevice(
    val displayName: String,
    val fullName: String,
)

data class VoiceVersionInfo(
    val voiceServerType: String,
    val internalVoiceServerType: String,
    val majorVersion: Int,
    val minorVersion: Int,
    val serverVersion: String,
    val buildVersion: String,
)

data class VoiceParticipant(
    val id: LLUUID,
    val name: String,
    val isSpeaking: Boolean,
    val volume: Float,
    val power: Float,
    val isAvatar: Boolean = true,
    val isModeratorMuted: Boolean = false,
)

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

interface VoiceParticipantObserver {
    fun onParticipantsChanged()
}

interface VoiceP2POutgoingCallInterface {
    fun callUser(agentId: LLUUID)
    fun hangup()
}

interface VoiceP2PIncomingCallInterface {
    fun answerInvite(): Boolean
    fun declineInvite()
}

enum class VoicePowerLevel {
    MUTED,
    PTT_OFF,
    PTT_ON,
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
}

interface VoiceModuleInterface {
    fun init(pump: Any?)
    fun terminate()
    fun updateSettings()
    fun isVoiceWorking(): Boolean
    fun setHidden(hidden: Boolean)
    fun getVersion(): VoiceVersionInfo

    fun tuningStart()
    fun tuningStop()
    fun inTuningMode(): Boolean
    fun tuningSetMicVolume(volume: Float)
    fun tuningSetSpeakerVolume(volume: Float)
    fun tuningGetEnergy(): Float

    fun deviceSettingsAvailable(): Boolean
    fun deviceSettingsUpdated(): Boolean
    fun refreshDeviceLists(clearCurrentList: Boolean = true)
    fun setCaptureDevice(name: String)
    fun setRenderDevice(name: String)
    fun getCaptureDevices(): MutableList<VoiceDevice>
    fun getRenderDevices(): MutableList<VoiceDevice>
    fun getParticipantList(participants: MutableSet<LLUUID>)
    fun isParticipant(speakerId: LLUUID): Boolean

    fun inProximalChannel(): Boolean
    fun setNonSpatialChannel(channelInfo: Map<String, Any?>, notifyOnFirstJoin: Boolean, hangupOnLastLeave: Boolean)
    fun setSpatialChannel(channelInfo: Map<String, Any?>): Boolean
    fun leaveNonSpatialChannel()
    fun processChannels(process: Boolean)
    fun isCurrentChannel(channelInfo: Map<String, Any?>): Boolean
    fun compareChannels(channelInfo1: Map<String, Any?>, channelInfo2: Map<String, Any?>): Boolean

    fun getOutgoingCallInterface(): VoiceP2POutgoingCallInterface?
    fun getIncomingCallInterface(voiceCallInfo: Map<String, Any?>): VoiceP2PIncomingCallInterface?

    fun setVoiceVolume(volume: Float)
    fun setMicGain(volume: Float)
    fun setVoiceEnabled(enabled: Boolean)
    fun setMuteMic(muted: Boolean)

    fun getDisplayName(id: LLUUID): String
    fun isParticipantAvatar(id: LLUUID): Boolean
    fun getIsSpeaking(id: LLUUID): Boolean
    fun getIsModeratorMuted(id: LLUUID): Boolean
    fun getCurrentPower(id: LLUUID): Float
    fun getUserVolume(id: LLUUID): Float
    fun setUserVolume(id: LLUUID, volume: Float)

    fun isSessionTextIMPossible(id: LLUUID): Boolean
    fun isSessionCallBackPossible(id: LLUUID): Boolean
    fun userAuthorized(userId: String, agentId: LLUUID)

    fun addObserver(observer: VoiceObserver)
    fun removeObserver(observer: VoiceObserver)
    fun addObserver(observer: VoiceParticipantObserver)
    fun removeObserver(observer: VoiceParticipantObserver)

    fun sipURIFromID(id: LLUUID): String
    fun getP2PChannelInfoTemplate(id: LLUUID): Map<String, Any?>
}

interface VoiceEffectObserver {
    fun onVoiceEffectChanged(effectListUpdated: Boolean)
}

interface VoiceEffectInterface {
    fun setVoiceEffect(id: LLUUID): Boolean
    fun getVoiceEffect(): LLUUID
    fun getVoiceEffectProperties(id: LLUUID): Map<String, Any?>
    fun refreshVoiceEffectLists(clearLists: Boolean)
    fun getVoiceEffectList(): Map<String, LLUUID>
    fun getVoiceEffectTemplateList(): Map<String, LLUUID>

    fun addObserver(observer: VoiceEffectObserver)
    fun removeObserver(observer: VoiceEffectObserver)

    fun enablePreviewBuffer(enable: Boolean)
    fun recordPreviewBuffer()
    fun playPreviewBuffer(effectId: LLUUID? = null)
    fun stopPreviewBuffer()
    fun isPreviewRecording(): Boolean
    fun isPreviewPlaying(): Boolean
}

object VoiceClient {

    const val OVERDRIVEN_POWER_LEVEL: Float = 0.7f
    val POWER_LEVEL_0: Float = OVERDRIVEN_POWER_LEVEL / 3f
    val POWER_LEVEL_1: Float = OVERDRIVEN_POWER_LEVEL * 2f / 3f
    val POWER_LEVEL_2: Float = OVERDRIVEN_POWER_LEVEL
    const val VOLUME_MIN: Float = 0f
    const val VOLUME_DEFAULT: Float = 0.5f
    const val VOLUME_MAX: Float = 1f

    private var spatialVoiceModule: VoiceModuleInterface? = null
    private var nonSpatialVoiceModule: VoiceModuleInterface? = null
    private var spatialCredentials: Map<String, Any?> = emptyMap()

    var isVoiceEnabled: Boolean = false
        set(value) {
            field = value
            spatialVoiceModule?.setVoiceEnabled(value)
            nonSpatialVoiceModule?.setVoiceEnabled(value)
        }

    var isInVoiceChannel: Boolean = false
        private set

    private var muteMic: Boolean = false
    private var disableMic: Boolean = false
    var usePTT: Boolean = true
        private set
    var pttIsToggle: Boolean = false
        private set
    var userPTTState: Boolean = false
        private set

    private var voiceEffectSupportNotified: Boolean = false

    val microChangedCallbacks: MutableList<() -> Unit> = mutableListOf()
    val userVolumeUpdateCallbacks: MutableList<(LLUUID) -> Unit> = mutableListOf()

    private val statusObservers: MutableList<VoiceObserver> = mutableListOf()
    private val participantObservers: MutableList<VoiceParticipantObserver> = mutableListOf()

    fun init(pump: Any?) {
        System.err.println("VoiceClient: init not yet implemented")
    }

    fun terminate() {
        isInVoiceChannel = false
        System.err.println("VoiceClient: terminate not yet implemented")
    }

    fun updateSettings() {
        System.err.println("VoiceClient: updateSettings not yet implemented")
    }

    fun isVoiceWorking(): Boolean = false

    fun voiceEnabled(noCache: Boolean = false): Boolean = false

    fun getVersion(): VoiceVersionInfo =
        spatialVoiceModule?.getVersion() ?: VoiceVersionInfo("", "", 0, 0, "", "")

    fun tuningStart() {
        System.err.println("VoiceClient: tuningStart not yet implemented")
    }

    fun tuningStop() {
        System.err.println("VoiceClient: tuningStop not yet implemented")
    }

    fun inTuningMode(): Boolean = false

    fun tuningSetMicVolume(volume: Float) {
        System.err.println("VoiceClient: tuningSetMicVolume not yet implemented")
    }

    fun tuningSetSpeakerVolume(volume: Float) {
        System.err.println("VoiceClient: tuningSetSpeakerVolume not yet implemented")
    }

    fun tuningGetEnergy(): Float = 0f

    fun deviceSettingsAvailable(): Boolean = false
    fun deviceSettingsUpdated(): Boolean = false

    fun refreshDeviceLists(clearCurrentList: Boolean = true) {
        System.err.println("VoiceClient: refreshDeviceLists not yet implemented")
    }

    fun setCaptureDevice(name: String) {
        System.err.println("VoiceClient: setCaptureDevice not yet implemented")
    }

    fun setRenderDevice(name: String) {
        System.err.println("VoiceClient: setRenderDevice not yet implemented")
    }

    fun setHidden(hidden: Boolean) {
        System.err.println("VoiceClient: setHidden not yet implemented")
    }

    fun getCaptureDevices(): List<VoiceDevice> = emptyList()
    fun getRenderDevices(): List<VoiceDevice> = emptyList()

    fun inProximalChannel(): Boolean =
        spatialVoiceModule?.inProximalChannel() ?: false

    fun setNonSpatialChannel(
        channelInfo: Map<String, Any?>,
        notifyOnFirstJoin: Boolean,
        hangupOnLastLeave: Boolean,
    ) {
        val vsType = channelInfo["voice_server_type"] as? String ?: ""
        setNonSpatialVoiceModule(vsType)
        if (spatialVoiceModule != null && spatialVoiceModule !== nonSpatialVoiceModule) {
            spatialVoiceModule?.processChannels(false)
        }
        nonSpatialVoiceModule?.processChannels(true)
        nonSpatialVoiceModule?.setNonSpatialChannel(channelInfo, notifyOnFirstJoin, hangupOnLastLeave)
        isInVoiceChannel = true
    }

    fun setSpatialChannel(channelInfo: Map<String, Any?>) {
        spatialCredentials = channelInfo
        System.err.println("VoiceClient: setSpatialChannel not yet implemented")
    }

    fun activateSpatialChannel(activate: Boolean) {
        spatialVoiceModule?.processChannels(activate)
    }

    fun leaveNonSpatialChannel() {
        nonSpatialVoiceModule?.leaveNonSpatialChannel()
        nonSpatialVoiceModule?.processChannels(false)
        nonSpatialVoiceModule = null
        isInVoiceChannel = false
    }

    fun isCurrentChannel(channelInfo: Map<String, Any?>): Boolean = false

    fun compareChannels(channelInfo1: Map<String, Any?>, channelInfo2: Map<String, Any?>): Boolean = false

    fun getOutgoingCallInterface(voiceChannelInfo: Map<String, Any?> = emptyMap()): VoiceP2POutgoingCallInterface? = null

    fun getIncomingCallInterface(voiceCallInfo: Map<String, Any?>): VoiceP2PIncomingCallInterface? {
        val vsType = voiceCallInfo["voice_server_type"] as? String ?: ""
        return null
    }

    fun setVoiceVolume(volume: Float) {
        System.err.println("VoiceClient: setVoiceVolume not yet implemented")
    }

    fun setMicGain(gain: Float) {
        System.err.println("VoiceClient: setMicGain not yet implemented")
    }

    fun setMuteMic(muted: Boolean) {
        if (muteMic != muted) {
            muteMic = muted
            updateMicMuteLogic()
            microChangedCallbacks.forEach { it() }
        }
    }

    fun setUserPTTState(ptt: Boolean) {
        userPTTState = ptt
        updateMicMuteLogic()
        microChangedCallbacks.forEach { it() }
    }

    fun getUserPTTState(): Boolean = userPTTState

    fun setUsePTT(usePTT: Boolean) {
        if (usePTT && !this.usePTT) {
            userPTTState = false
        }
        this.usePTT = usePTT
        updateMicMuteLogic()
    }

    fun setPTTIsToggle(toggle: Boolean) {
        if (!toggle && pttIsToggle) {
            userPTTState = false
        }
        pttIsToggle = toggle
        updateMicMuteLogic()
    }

    fun getPTTIsToggle(): Boolean = pttIsToggle

    fun inputUserControlState(down: Boolean) {
        if (pttIsToggle) {
            if (down) toggleUserPTTState()
        } else {
            setUserPTTState(down)
        }
    }

    fun toggleUserPTTState() {
        setUserPTTState(!userPTTState)
    }

    fun updateMicMuteLogic() {
        var newMicMute = false
        if (usePTT) {
            newMicMute = !userPTTState
        }
        if (muteMic || disableMic) {
            newMicMute = true
        }
        System.err.println("VoiceClient: updateMicMuteLogic not yet implemented")
    }

    fun getVoiceEnabled(id: LLUUID): Boolean = isParticipant(id)

    fun getDisplayName(id: LLUUID): String = ""

    fun isVoiceWorking(id: LLUUID): Boolean = false

    fun isParticipantAvatar(id: LLUUID): Boolean = true

    fun isOnlineSIP(id: LLUUID): Boolean = false

    fun getIsSpeaking(id: LLUUID): Boolean = false

    fun getIsModeratorMuted(id: LLUUID): Boolean = false

    fun getCurrentPower(id: LLUUID): Float = 0f

    fun getOnMuteList(id: LLUUID): Boolean = false

    fun getUserVolume(id: LLUUID): Float = 0f

    fun setUserVolume(id: LLUUID, volume: Float) {
        System.err.println("VoiceClient: setUserVolume not yet implemented")
        userVolumeUpdateCallbacks.forEach { it(id) }
    }

    fun getPowerLevel(id: LLUUID): VoicePowerLevel {
        val power = getCurrentPower(id)
        return when {
            getOnMuteList(id)           -> VoicePowerLevel.MUTED
            power == 0f && !getIsSpeaking(id) -> VoicePowerLevel.PTT_OFF
            power < POWER_LEVEL_0       -> VoicePowerLevel.PTT_ON
            power < POWER_LEVEL_1       -> VoicePowerLevel.LEVEL_1
            power < POWER_LEVEL_2       -> VoicePowerLevel.LEVEL_2
            else                        -> VoicePowerLevel.LEVEL_3
        }
    }

    fun getParticipantList(participants: MutableSet<LLUUID>) {
        System.err.println("VoiceClient: getParticipantList not yet implemented")
    }

    fun isParticipant(speakerId: LLUUID): Boolean = false

    fun isSessionTextIMPossible(id: LLUUID): Boolean = true

    fun isSessionCallBackPossible(id: LLUUID): Boolean = true

    fun setSpatialVoiceModule(voiceServerType: String) {
        System.err.println("VoiceClient: setSpatialVoiceModule not yet implemented")
    }

    fun setNonSpatialVoiceModule(voiceServerType: String) {
        System.err.println("VoiceClient: setNonSpatialVoiceModule not yet implemented")
    }

    fun userAuthorized(userId: String, agentId: LLUUID) {
        System.err.println("VoiceClient: userAuthorized not yet implemented")
    }

    fun onRegionChanged() {
        System.err.println("VoiceClient: onRegionChanged not yet implemented")
    }

    fun handleSimulatorFeaturesReceived(simulatorFeatures: Map<String, Any?>) {
        val voiceServerType = simulatorFeatures["VoiceServerType"] as? String ?: "vivox"
        System.err.println("VoiceClient: handleSimulatorFeaturesReceived not yet implemented")
    }

    fun addObserver(observer: VoiceObserver) {
        statusObservers.add(observer)
        System.err.println("VoiceClient: addObserver(VoiceObserver) not yet implemented")
    }

    fun removeObserver(observer: VoiceObserver) {
        statusObservers.remove(observer)
        System.err.println("VoiceClient: removeObserver(VoiceObserver) not yet implemented")
    }

    fun addObserver(observer: VoiceParticipantObserver) {
        participantObservers.add(observer)
        System.err.println("VoiceClient: addObserver(VoiceParticipantObserver) not yet implemented")
    }

    fun removeObserver(observer: VoiceParticipantObserver) {
        participantObservers.remove(observer)
        System.err.println("VoiceClient: removeObserver(VoiceParticipantObserver) not yet implemented")
    }

    fun sipURIFromID(id: LLUUID): String =
        (nonSpatialVoiceModule ?: spatialVoiceModule)?.sipURIFromID(id) ?: ""

    fun getP2PChannelInfoTemplate(id: LLUUID): Map<String, Any?> =
        (nonSpatialVoiceModule ?: spatialVoiceModule)?.getP2PChannelInfoTemplate(id) ?: emptyMap()

    fun getVoiceEffectInterface(): VoiceEffectInterface? = null
}

object SpeakerVolumeStorage {

    private const val SETTINGS_FILE_NAME = "volume_settings.xml"

    private val speakersData: MutableMap<LLUUID, Float> = mutableMapOf()

    fun storeSpeakerVolume(speakerId: LLUUID, volume: Float) {
        if (volume >= VoiceClient.VOLUME_MIN && volume <= VoiceClient.VOLUME_MAX) {
            speakersData[speakerId] = volume
        }
    }

    fun getSpeakerVolume(speakerId: LLUUID): Float? = speakersData[speakerId]

    fun removeSpeakerVolume(speakerId: LLUUID) { speakersData.remove(speakerId) }

    fun load() { System.err.println("SpeakerVolumeStorage: load not yet implemented") }

    fun save() { System.err.println("SpeakerVolumeStorage: save not yet implemented") }

    fun cleanupSingleton() { save() }

    fun transformFromLegacyVolume(volumeIn: Float): Float {
        val v = volumeIn.coerceIn(0f, 1f)
        return if (v <= 0.5f) {
            v * v * 4f * 0.56f
        } else {
            (1f - 0.56f) * (4f * v * v - 1f) / 3f + 0.56f
        }
    }

    fun transformToLegacyVolume(volumeIn: Float): Float {
        val v = volumeIn.coerceIn(0f, 1f)
        return if (v <= 0.56f) {
            kotlin.math.sqrt(v / (4f * 0.56f))
        } else {
            kotlin.math.sqrt((3f * (v - 0.56f) / (1f - 0.56f) + 1f) / 4f)
        }
    }
}
