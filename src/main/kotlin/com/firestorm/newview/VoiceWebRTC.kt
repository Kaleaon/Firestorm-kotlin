package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

const val WEBRTC_VOICE_SERVER_TYPE = "webrtc"

private const val MAX_AUDIO_DIST           = 50.0f
private const val VOLUME_SCALE_WEBRTC      = 0.01f
private const val TUNING_LEVEL_SCALE       = 0.01f
private const val TUNING_LEVEL_START_POINT = 0.8f
private const val LEVEL_SCALE              = 0.005f
private const val LEVEL_START_POINT        = 0.18f
private const val SPEAKING_AUDIO_LEVEL     = 0.30f
private const val PEER_GAIN_CONVERSION_FACTOR = 220u
private const val UPDATE_THROTTLE_SECONDS  = 0.1f
private const val MAX_RETRY_WAIT_SECONDS   = 10.0f
private const val FOUR_DEGREES             = 4.0f * (kotlin.math.PI.toFloat() / 180.0f)
private val MINUSCULE_ANGLE_COS            = cos(0.5f * FOUR_DEGREES).toFloat()
private const val REPORTED_VOICE_SERVER_TYPE = "Secondlife WebRTC Gateway"

data class Vec3d(val x: Double, val y: Double, val z: Double) {
    fun distSqTo(other: Vec3d): Double {
        val dx = x - other.x; val dy = y - other.y; val dz = z - other.z
        return dx * dx + dy * dy + dz * dz
    }
    operator fun plus(other: Vec3d) = Vec3d(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3d) = Vec3d(x - other.x, y - other.y, z - other.z)
    operator fun times(s: Double) = Vec3d(x * s, y * s, z * s)
    fun magnitude() = sqrt(x * x + y * y + z * z)
}

data class Vec3f(val x: Float, val y: Float, val z: Float)

data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float) {
    fun dot(other: Quaternion) = x * other.x + y * other.y + z * other.z + w * other.w
}

class WebRTCParticipant(val agentId: LLUUID, val region: LLUUID) {
    var uri: String = ""
    var displayName: String = ""
    var level: Float = 0f
    var volume: Float = VoiceClient.VOLUME_DEFAULT
    var isSpeaking: Boolean = false
    var isModeratorMuted: Boolean = false
    var speakingTimeoutMs: Long = 0L
    var currentRegion: LLUUID = region
}

abstract class WebRTCSessionState {
    val participants: MutableMap<LLUUID, WebRTCParticipant> = mutableMapOf()
    val connections: MutableList<WebRTCConnection> = mutableListOf()

    var handle: String = ""
    var channelId: String = ""
    var name: String = ""
    var muted: Boolean = false
    var speakerVolume: Float = 0f
    var shuttingDown: Boolean = false
    var hangupOnLastLeave: Boolean = false
    var notifyOnFirstJoin: Boolean = false

    abstract val isSpatial: Boolean
    abstract val isEstate: Boolean
    abstract val isCallbackPossible: Boolean

    fun addParticipant(agentId: LLUUID, regionId: LLUUID): WebRTCParticipant {
        val p = WebRTCParticipant(agentId, regionId)
        participants[agentId] = p
        return p
    }

    fun removeParticipant(participant: WebRTCParticipant) {
        participants.remove(participant.agentId)
    }

    fun removeAllParticipants(regionId: LLUUID = LLUUID.NULL) {
        if (regionId == LLUUID.NULL) {
            participants.clear()
        } else {
            participants.entries.removeAll { it.value.currentRegion == regionId }
        }
    }

    fun findParticipantById(id: LLUUID): WebRTCParticipant? = participants[id]

    fun shutdownAllConnections() {
        connections.forEach { it.shutDown() }
    }

    fun setMuteMic(muted: Boolean) {
        connections.forEach { it.setMuteMic(muted) }
    }

    fun setSpeakerVolume(volume: Float) {
        speakerVolume = volume
        connections.forEach { it.setSpeakerVolume(volume) }
    }

    fun setUserVolume(id: LLUUID, volume: Float) {
        connections.forEach { it.setUserVolume(id, volume) }
    }

    fun setUserMute(id: LLUUID, mute: Boolean) {
        connections.forEach { it.setUserMute(id, mute) }
    }

    open fun sendData(data: String) {
        connections.forEach { it.sendData(data) }
    }

    open fun processConnectionStates(): Boolean {
        return connections.fold(false) { any, conn -> conn.connectionStateMachine() || any }
    }

    companion object {
        private val sessions: MutableMap<String, WebRTCSessionState> = mutableMapOf()

        fun addSession(channelId: String, session: WebRTCSessionState) {
            sessions[channelId] = session
        }

        fun matchSessionByChannelId(channelId: String): WebRTCSessionState? = sessions[channelId]

        fun hasSession(sessionId: String): Boolean = sessions.containsKey(sessionId)

        fun processSessionStates() {
            sessions.values.toList().forEach { it.processConnectionStates() }
        }

        fun reapEmptySessions() {
            sessions.entries.removeAll { it.value.connections.isEmpty() && it.value.shuttingDown }
        }

        fun clearSessions() { sessions.clear() }

        fun forEach(action: (WebRTCSessionState) -> Unit) {
            sessions.values.toList().forEach(action)
        }
    }
}

class EstateSessionState : WebRTCSessionState() {
    override val isSpatial: Boolean get() = true
    override val isEstate: Boolean get() = true
    override val isCallbackPossible: Boolean get() = false

    override fun processConnectionStates(): Boolean {
        TODO("IPC: iterate neighboring regions; add/remove WebRTCSpatialConnection per region; delegate to super")
    }
}

class ParcelSessionState(channelId: String, val parcelLocalId: Int) : WebRTCSessionState() {
    init { this.channelId = channelId }
    override val isSpatial: Boolean get() = true
    override val isEstate: Boolean get() = false
    override val isCallbackPossible: Boolean get() = false
}

class AdhocSessionState(
    channelId: String,
    val credentials: String,
    notifyOnFirstJoin: Boolean,
    hangupOnLastLeave: Boolean,
) : WebRTCSessionState() {
    init {
        this.channelId = channelId
        this.notifyOnFirstJoin = notifyOnFirstJoin
        this.hangupOnLastLeave = hangupOnLastLeave
    }
    override val isSpatial: Boolean get() = false
    override val isEstate: Boolean get() = false
    override val isCallbackPossible: Boolean get() = notifyOnFirstJoin && hangupOnLastLeave

    override fun sendData(data: String) { /* ad-hoc sessions don't forward spatial data */ }
}

abstract class WebRTCConnection(
    val regionId: LLUUID,
    val channelId: String,
) {
    enum class State(val value: Int) {
        ERROR(0x0),
        START_SESSION(0x1),
        WAIT_FOR_SESSION_START(0x2),
        REQUEST_CONNECTION(0x4),
        CONNECTION_WAIT(0x8),
        SESSION_ESTABLISHED(0x10),
        WAIT_FOR_DATA_CHANNEL(0x20),
        SESSION_UP(0x40),
        SESSION_RETRY(0x80),
        DISCONNECT(0x100),
        WAIT_FOR_EXIT(0x200),
        SESSION_EXIT(0x400),
        WAIT_FOR_CLOSE(0x800),
        CLOSED(0x1000),
        SESSION_STOPPING(0x1F80),
    }

    protected var connectionState: State = State.START_SESSION
    protected var currentStatus: VoiceObserver.StatusType = VoiceObserver.StatusType.STATUS_VOICE_ENABLED

    var viewerSession: LLUUID = LLUUID.NULL
    protected var channelSdp: String = ""
    protected var remoteChannelSdp: String = ""
    protected var muted: Boolean = false
    protected var speakerVolume: Float = 0f
    var shutDown: Boolean = false
    protected var outstandingRequests: Int = 0
    protected var retryWaitPeriod: Int = 0
    protected var retryWaitSecs: Float = 0f
    protected val iceCandidates: MutableList<String> = mutableListOf()
    protected var iceCompleted: Boolean = false

    fun isShuttingDown(): Boolean = shutDown

    protected fun setVoiceConnectionState(new: State) {
        val stopping = 0x1F80
        if (new.value and stopping != 0) {
            connectionState = new
            return
        }
        if (connectionState.value and stopping != 0) return
        connectionState = new
    }

    open fun setMuteMic(muted: Boolean) {
        this.muted = muted
        TODO("GPU: webRTCDeviceInterface?.setMute(muted)")
    }

    open fun setSpeakerVolume(volume: Float) {
        speakerVolume = volume
        TODO("GPU: audioInterface?.setSpeakerVolume(volume * VOLUME_SCALE_WEBRTC * PEER_GAIN_CONVERSION_FACTOR)")
    }

    fun setUserVolume(id: LLUUID, volume: Float) {
        TODO("GPU: audioInterface?.setParticipantVolume(id, volume * PEER_GAIN_CONVERSION_FACTOR)")
    }

    fun setUserMute(id: LLUUID, mute: Boolean) {
        TODO("GPU: audioInterface?.setParticipantMute(id, mute)")
    }

    fun connectionStateMachine(): Boolean {
        TODO("IPC: drive state machine; return true when still active")
    }

    fun sendJoin() {
        TODO("IPC: send join JSON via WebRTC data channel")
    }

    fun sendData(data: String) {
        TODO("IPC: forward data via WebRTC data channel")
    }

    fun processIceUpdates() {
        TODO("IPC: POST ice candidates to voice server")
    }

    fun onIceGatheringState(state: String) {
        TODO("IPC: post to main queue; if complete mark iceCompleted")
    }

    fun onIceCandidate(candidate: String) {
        TODO("IPC: post to main queue; add candidate to iceCandidates")
    }

    fun onOfferAvailable(sdp: String) {
        TODO("IPC: post to main queue; store channelSdp; advance state")
    }

    fun onRenegotiationNeeded() {
        TODO("IPC: post to main queue; restart session if up")
    }

    fun onPeerConnectionClosed() {
        TODO("IPC: post to main queue; advance to WAIT_FOR_CLOSE state")
    }

    fun onAudioEstablished() {
        TODO("IPC: post to main queue; store audio interface; advance to WAIT_FOR_DATA_CHANNEL")
    }

    fun onDataReceived(data: String, binary: Boolean) {
        TODO("IPC: post to main queue; parse JSON; update participant levels/speaking")
    }

    fun onDataChannelReady() {
        TODO("IPC: post to main queue; store data interface; advance to SESSION_UP; sendJoin()")
    }

    fun onVoiceConnectionRequestSuccess(body: Map<String, Any?>) {
        TODO("IPC: extract SDP from body; set remote SDP on peer connection; post candidates; advance state")
    }

    open val isSpatial: Boolean get() = false

    abstract fun requestVoiceConnection()
}

class WebRTCSpatialConnection(
    regionId: LLUUID,
    val parcelLocalId: Int,
    channelId: String,
) : WebRTCConnection(regionId, channelId) {

    override val isSpatial: Boolean get() = true

    override fun setMuteMic(muted: Boolean) {
        this.muted = muted
        TODO("GPU: webRTCDeviceInterface?.setMute(muted || hidden, delay)")
    }

    override fun requestVoiceConnection() {
        TODO("IPC: POST to region voice capability with parcelLocalId; call OnVoiceConnectionRequestSuccess on success")
    }
}

class WebRTCAdHocConnection(
    regionId: LLUUID,
    channelId: String,
    private val credentials: String,
) : WebRTCConnection(regionId, channelId) {

    override val isSpatial: Boolean get() = false

    override fun requestVoiceConnection() {
        TODO("IPC: POST to voice server with credentials; call OnVoiceConnectionRequestSuccess on success")
    }
}

object VoiceWebRTCStats {
    private var startTime: Double = -1.0
    var connectCycles: UInt = 0u
        private set
    private var connectTime: Double = -1.0
    var connectAttempts: UInt = 0u
        private set
    private var provisionTime: Double = -1.0
    var provisionAttempts: UInt = 0u
        private set
    private var establishTime: Double = -1.0
    var establishAttempts: UInt = 0u
        private set

    fun reset() {
        startTime = -1.0; connectCycles = 0u; connectTime = -1.0; connectAttempts = 0u
        provisionTime = -1.0; provisionAttempts = 0u; establishTime = -1.0; establishAttempts = 0u
    }

    fun connectionAttemptStart() {
        if (connectAttempts == 0u) { startTime = currentTimeSeconds(); connectCycles++ }
        connectAttempts++
    }

    fun connectionAttemptEnd(success: Boolean) {
        if (success) connectTime = currentTimeSeconds() - startTime
    }

    fun provisionAttemptStart() {
        if (provisionAttempts == 0u) startTime = currentTimeSeconds()
        provisionAttempts++
    }

    fun provisionAttemptEnd(success: Boolean) {
        if (success) provisionTime = currentTimeSeconds() - startTime
    }

    fun establishAttemptStart() {
        if (establishAttempts == 0u) startTime = currentTimeSeconds()
        establishAttempts++
    }

    fun establishAttemptEnd(success: Boolean) {
        if (success) establishTime = currentTimeSeconds() - startTime
    }

    fun read(): Map<String, Any> = mapOf(
        "connect_cycles"      to connectCycles,
        "connect_attempts"    to connectAttempts,
        "connect_time"        to connectTime,
        "provision_attempts"  to provisionAttempts,
        "provision_time"      to provisionTime,
        "establish_attempts"  to establishAttempts,
        "establish_time"      to establishTime,
    )

    private fun currentTimeSeconds(): Double = System.nanoTime() / 1_000_000_000.0
}

object WebRTCVoiceClient : VoiceModuleInterface {

    private val voiceVersion = VoiceVersionInfo(
        voiceServerType = REPORTED_VOICE_SERVER_TYPE,
        internalVoiceServerType = WEBRTC_VOICE_SERVER_TYPE,
        majorVersion = 2,
        minorVersion = 0,
        serverVersion = "",
        buildVersion = "",
    )

    private var hidden: Boolean = false
    private var tuningMicGain: Float = 0f
    private var tuningSpeakerVolume: Int = 50
    private var devicesListUpdated: Boolean = false
    private var spatialCoordsDirty: Boolean = false

    private var muteMic: Boolean = false
    private var earLocation: Int = EAR_LOC_CAMERA
    private var micGain: Float = 0f
    private var voiceEnabled: Boolean = false
    private var processChannels: Boolean = false

    private var listenerPosition: Vec3d = Vec3d(0.0, 0.0, 0.0)
    private var listenerRequestedPosition: Vec3d = Vec3d(0.0, 0.0, 0.0)
    private var listenerVelocity: Vec3f = Vec3f(0f, 0f, 0f)
    private var listenerRot: Quaternion = Quaternion(0f, 0f, 0f, 1f)
    private var avatarPosition: Vec3d = Vec3d(0.0, 0.0, 0.0)
    private var avatarVelocity: Vec3f = Vec3f(0f, 0f, 0f)
    private var avatarRot: Quaternion = Quaternion(0f, 0f, 0f, 1f)

    private var session: WebRTCSessionState? = null
    private var nextSession: WebRTCSessionState? = null
    private val neighboringRegions: MutableSet<LLUUID> = mutableSetOf()

    private val captureDevices: MutableList<VoiceDevice> = mutableListOf()
    private val renderDevices: MutableList<VoiceDevice> = mutableListOf()
    private var speakerVolume: Float = 0f

    private val participantObservers: MutableList<VoiceParticipantObserver> = mutableListOf()
    private val statusObservers: MutableList<VoiceObserver> = mutableListOf()

    private var isInTuningMode: Boolean = false
    private var isProcessingChannels: Boolean = false
    private var isCoroutineActive: Boolean = false
    private var shuttingDown: Boolean = false

    private val EAR_LOC_CAMERA = 0
    private val EAR_LOC_AVATAR = 1
    private val EAR_LOC_MIXED  = 2

    override fun init(pump: Any?) {
        shuttingDown = false
        TODO("GPU: llwebrtc::init(this); acquire device interface; setDevicesObserver; refreshDeviceLists(); launch voiceConnectionCoro")
    }

    override fun terminate() {
        if (shuttingDown) return
        voiceEnabled = false
        shuttingDown = true
        TODO("GPU: llwebrtc::terminate(); clear webRTCDeviceInterface")
    }

    override fun getVersion(): VoiceVersionInfo = voiceVersion

    override fun updateSettings() {
        TODO("IPC: read VoiceEarLocation, VoiceInputAudioDevice, VoiceOutputAudioDevice, AudioLevelMic, echo/AGC/noise settings; apply to device interface")
    }

    override fun isVoiceWorking(): Boolean =
        TODO("IPC: return isProcessingChannels || isInTuningMode")

    override fun sipURIFromID(id: LLUUID): String = ""

    override fun getP2PChannelInfoTemplate(id: LLUUID): Map<String, Any?> = emptyMap()

    override fun setHidden(hidden: Boolean) {
        this.hidden = hidden
        if (inSpatialChannel()) {
            TODO("GPU: webRTCDeviceInterface?.setMute(hidden || muteMic, if hidden 0 else SET_HIDDEN_RESTORE_DELAY_MS)")
            if (hidden) {
                WebRTCSessionState.forEach { it.setMuteMic(true) }
            } else {
                WebRTCSessionState.forEach { it.setMuteMic(muteMic) }
                updatePosition()
                sendPositionUpdate(force = true)
            }
        }
    }

    fun logMessage(level: Int, message: String) {
        // pass through to platform logger
    }

    override fun tuningStart() {
        if (!isInTuningMode) {
            TODO("GPU: webRTCDeviceInterface?.setTuningMode(true)")
            isInTuningMode = true
        }
    }

    override fun tuningStop() {
        if (isInTuningMode) {
            TODO("GPU: webRTCDeviceInterface?.setTuningMode(false)")
            isInTuningMode = false
        }
    }

    override fun inTuningMode(): Boolean = isInTuningMode

    override fun tuningSetMicVolume(volume: Float) {
        if (volume != tuningMicGain) {
            tuningMicGain = volume
            TODO("GPU: webRTCDeviceInterface?.setTuningMicGain(volume)")
        }
    }

    override fun tuningSetSpeakerVolume(volume: Float) {
        tuningSpeakerVolume = volume.toInt()
    }

    override fun tuningGetEnergy(): Float {
        TODO("GPU: val rms = webRTCDeviceInterface?.getTuningAudioLevel() ?: 0f; return TUNING_LEVEL_START_POINT - TUNING_LEVEL_SCALE * rms")
    }

    override fun deviceSettingsAvailable(): Boolean = captureDevices.isNotEmpty() && renderDevices.isNotEmpty()

    override fun deviceSettingsUpdated(): Boolean {
        val updated = devicesListUpdated
        devicesListUpdated = false
        return updated
    }

    override fun refreshDeviceLists(clearCurrentList: Boolean) {
        if (clearCurrentList) {
            captureDevices.clear()
            renderDevices.clear()
        }
        TODO("GPU: webRTCDeviceInterface?.refreshDevices()")
    }

    override fun setCaptureDevice(name: String) {
        TODO("GPU: webRTCDeviceInterface?.setCaptureDevice(name)")
    }

    override fun setRenderDevice(name: String) {
        TODO("GPU: webRTCDeviceInterface?.setRenderDevice(name)")
    }

    override fun getCaptureDevices(): MutableList<VoiceDevice> = captureDevices
    override fun getRenderDevices(): MutableList<VoiceDevice> = renderDevices

    fun onDevicesChanged(renderList: List<Pair<String, String>>, captureList: List<Pair<String, String>>) {
        TODO("IPC: post to main queue; call onDevicesChangedImpl")
    }

    fun onDevicesChangedImpl(renderList: List<Pair<String, String>>, captureList: List<Pair<String, String>>) {
        if (shuttingDown) return
        val newRender = renderList.map { VoiceDevice(it.first, it.second) }
        if (renderDevices != newRender) {
            renderDevices.clear()
            renderDevices.addAll(newRender)
            TODO("GPU: setRenderDevice(savedOutputDevice)")
        }
        val newCapture = captureList.map { VoiceDevice(it.first, it.second) }
        if (captureDevices != newCapture) {
            captureDevices.clear()
            captureDevices.addAll(newCapture)
            TODO("GPU: setCaptureDevice(savedInputDevice)")
        }
        devicesListUpdated = true
    }

    override fun getParticipantList(participants: MutableSet<LLUUID>) {
        if (processChannels) {
            session?.participants?.keys?.let { participants.addAll(it) }
        }
    }

    override fun isParticipant(speakerId: LLUUID): Boolean =
        processChannels && (session?.participants?.containsKey(speakerId) == true)

    override fun isSessionCallBackPossible(id: LLUUID): Boolean = true

    override fun isSessionTextIMPossible(id: LLUUID): Boolean = true

    override fun inProximalChannel(): Boolean = processChannels && inSpatialChannel()

    override fun setNonSpatialChannel(channelInfo: Map<String, Any?>, notifyOnFirstJoin: Boolean, hangupOnLastLeave: Boolean) {
        startAdHocSession(channelInfo, notifyOnFirstJoin, hangupOnLastLeave)
    }

    override fun setSpatialChannel(channelInfo: Map<String, Any?>): Boolean {
        TODO("IPC: parse channel_uri / channel_credentials from channelInfo; start estate or parcel session")
    }

    override fun leaveNonSpatialChannel() {
        leaveChannel(stopTalking = false)
    }

    override fun processChannels(process: Boolean) {
        processChannels = process
    }

    fun leaveChannel(stopTalking: Boolean) {
        session?.shutdownAllConnections()
        if (stopTalking) {
            TODO("IPC: VoiceClient.setUserPTTState(false)")
        }
    }

    override fun isCurrentChannel(channelInfo: Map<String, Any?>): Boolean {
        TODO("IPC: compare channelInfo to current session channelId")
    }

    override fun compareChannels(channelInfo1: Map<String, Any?>, channelInfo2: Map<String, Any?>): Boolean {
        TODO("IPC: compare channel_uri fields from both maps")
    }

    override fun getOutgoingCallInterface(): VoiceP2POutgoingCallInterface? = null

    override fun getIncomingCallInterface(voiceCallInfo: Map<String, Any?>): VoiceP2PIncomingCallInterface? = null

    override fun setVoiceVolume(volume: Float) {
        speakerVolume = volume
        WebRTCSessionState.forEach { it.setSpeakerVolume(volume) }
    }

    override fun setMicGain(volume: Float) {
        micGain = volume
        TODO("GPU: webRTCDeviceInterface?.setMicGain(volume)")
    }

    override fun setVoiceEnabled(enabled: Boolean) {
        voiceEnabled = enabled
    }

    override fun setMuteMic(muted: Boolean) {
        muteMic = muted
        WebRTCSessionState.forEach { it.setMuteMic(muted) }
    }

    override fun getDisplayName(id: LLUUID): String =
        session?.findParticipantById(id)?.displayName ?: ""

    override fun isParticipantAvatar(id: LLUUID): Boolean = true

    override fun getIsSpeaking(id: LLUUID): Boolean =
        session?.findParticipantById(id)?.isSpeaking ?: false

    override fun getIsModeratorMuted(id: LLUUID): Boolean =
        session?.findParticipantById(id)?.isModeratorMuted ?: false

    override fun getCurrentPower(id: LLUUID): Float =
        session?.findParticipantById(id)?.level ?: 0f

    override fun getUserVolume(id: LLUUID): Float =
        session?.findParticipantById(id)?.volume ?: VoiceClient.VOLUME_DEFAULT

    override fun setUserVolume(id: LLUUID, volume: Float) {
        session?.findParticipantById(id)?.volume = volume
        WebRTCSessionState.forEach { it.setUserVolume(id, volume) }
    }

    override fun isSessionTextIMPossible(id: LLUUID): Boolean = true

    override fun isSessionCallBackPossible(id: LLUUID): Boolean = true

    override fun userAuthorized(userId: String, agentId: LLUUID) {
        TODO("IPC: store agentId; launch voiceConnectionCoro if not already running")
    }

    override fun addObserver(observer: VoiceObserver) { statusObservers.add(observer) }
    override fun removeObserver(observer: VoiceObserver) { statusObservers.remove(observer) }
    override fun addObserver(observer: VoiceParticipantObserver) { participantObservers.add(observer) }
    override fun removeObserver(observer: VoiceParticipantObserver) { participantObservers.remove(observer) }

    private fun notifyParticipantObservers() {
        participantObservers.toList().forEach { it.onParticipantsChanged() }
    }

    private fun notifyStatusObservers(status: VoiceObserver.StatusType) {
        isProcessingChannels = status == VoiceObserver.StatusType.STATUS_JOINED
        val channelInfo = getAudioSessionChannelInfo()
        val inSpatial = inSpatialChannel()
        statusObservers.toList().forEach { it.onChange(status, channelInfo, inSpatial) }
        TODO("IPC: if not JOINING/LEFT/DISABLED — update agent voice connected state; trigger first-use speak hint")
    }

    fun onConnectionEstablished(channelId: String, regionId: LLUUID) {
        TODO("IPC: swap nextSession → session; addParticipant(agentId, regionId); notifyStatusObservers(LOGGED_IN / JOINED)")
    }

    fun onConnectionShutDown(channelId: String, regionId: LLUUID) {
        session?.takeIf { it.channelId == channelId }?.removeAllParticipants(regionId)
    }

    fun onConnectionFailure(channelId: String, regionId: LLUUID, statusType: VoiceObserver.StatusType = VoiceObserver.StatusType.ERROR_UNKNOWN) {
        if (nextSession?.channelId == channelId || session?.channelId == channelId) {
            notifyStatusObservers(statusType)
        }
    }

    fun setEarLocation(loc: Int) {
        if (earLocation != loc) {
            earLocation = loc
            spatialCoordsDirty = true
        }
    }

    fun updatePosition() {
        TODO("IPC: read avatar/camera positions from agent; setListenerPosition; setAvatarPosition; enforceTether; updateNeighboringRegions")
    }

    private fun setListenerPosition(position: Vec3d, velocity: Vec3f, rot: Quaternion) {
        listenerRequestedPosition = position
        if (listenerVelocity != velocity) { listenerVelocity = velocity; spatialCoordsDirty = true }
        if (listenerRot != rot) { listenerRot = rot; spatialCoordsDirty = true }
    }

    private fun setAvatarPosition(position: Vec3d, velocity: Vec3f, rot: Quaternion) {
        if (avatarPosition.distSqTo(position) > 0.01) { avatarPosition = position; spatialCoordsDirty = true }
        if (avatarVelocity != velocity) { avatarVelocity = velocity; spatialCoordsDirty = true }
        val rotCosDiff = abs(avatarRot.dot(rot))
        if (avatarRot != rot && rotCosDiff < MINUSCULE_ANGLE_COS) { avatarRot = rot; spatialCoordsDirty = true }
    }

    private fun enforceTether() {
        val cameraOffset = listenerRequestedPosition - avatarPosition
        val distance = cameraOffset.magnitude().toFloat()
        val tethered = if (distance > MAX_AUDIO_DIST) {
            avatarPosition + cameraOffset * (MAX_AUDIO_DIST / distance).toDouble()
        } else {
            listenerRequestedPosition
        }
        if (listenerPosition.distSqTo(tethered) > 0.01) {
            listenerPosition = tethered
            spatialCoordsDirty = true
        }
    }

    fun sendPositionUpdate(force: Boolean) {
        if (!spatialCoordsDirty && !force) return
        val spatial = buildString {
            append("{")
            append("\"sp\":{\"x\":${(avatarPosition.x * 100).toInt()},\"y\":${(avatarPosition.y * 100).toInt()},\"z\":${(avatarPosition.z * 100).toInt()}},")
            append("\"sh\":{\"x\":${(avatarRot.x * 100).toInt()},\"y\":${(avatarRot.y * 100).toInt()},\"z\":${(avatarRot.z * 100).toInt()},\"w\":${(avatarRot.w * 100).toInt()}},")
            append("\"lp\":{\"x\":${(listenerPosition.x * 100).toInt()},\"y\":${(listenerPosition.y * 100).toInt()},\"z\":${(listenerPosition.z * 100).toInt()}},")
            append("\"lh\":{\"x\":${(listenerRot.x * 100).toInt()},\"y\":${(listenerRot.y * 100).toInt()},\"z\":${(listenerRot.z * 100).toInt()},\"w\":${(listenerRot.w * 100).toInt()}}")
            append("}")
        }
        spatialCoordsDirty = false
        WebRTCSessionState.forEach { it.sendData(spatial) }
    }

    fun updateOwnVolume() {
        val audioLevel = if (!muteMic) {
            TODO("GPU: val rms = webRTCDeviceInterface?.getPeerConnectionAudioLevel() ?: 0f; LEVEL_START_POINT - LEVEL_SCALE * rms") as Float
        } else 0f
        WebRTCSessionState.forEach { session ->
            TODO("IPC: update own participant level in session to $audioLevel")
        }
    }

    private fun updateNeighboringRegions() {
        TODO("IPC: iterate 8 neighbor offsets at 2*MAX_AUDIO_DIST; collect region IDs into neighboringRegions")
    }

    private fun inSpatialChannel(): Boolean =
        session?.isSpatial ?: false

    private fun inEstateChannel(): Boolean =
        session?.isEstate ?: false

    private fun inOrJoiningChannel(channelId: String): Boolean =
        session?.channelId == channelId || nextSession?.channelId == channelId

    private fun getAudioSessionChannelInfo(): Map<String, Any?> =
        session?.let { mapOf("channel_id" to it.channelId, "is_spatial" to it.isSpatial) } ?: emptyMap()

    private fun startEstateSession(): Boolean {
        TODO("IPC: if not already in estate channel, create EstateSessionState; launch connection per region")
    }

    private fun startParcelSession(channelId: String, parcelId: Int): Boolean {
        TODO("IPC: create ParcelSessionState; launch WebRTCSpatialConnection")
    }

    private fun startAdHocSession(channelInfo: Map<String, Any?>, notifyOnFirstJoin: Boolean, hangupOnLastLeave: Boolean): Boolean {
        val channelId = channelInfo["channel_uri"] as? String ?: return false
        val credentials = channelInfo["channel_credentials"] as? String ?: ""
        val adhoc = AdhocSessionState(channelId, credentials, notifyOnFirstJoin, hangupOnLastLeave)
        TODO("IPC: WebRTCSessionState.addSession(channelId, adhoc); launch WebRTCAdHocConnection per region")
    }

    fun findParticipantById(channelId: String, id: LLUUID): WebRTCParticipant? =
        WebRTCSessionState.matchSessionByChannelId(channelId)?.findParticipantById(id)

    fun addParticipantById(channelId: String, id: LLUUID, region: LLUUID): WebRTCParticipant? {
        val sess = WebRTCSessionState.matchSessionByChannelId(channelId) ?: return null
        val p = sess.addParticipant(id, region)
        notifyParticipantObservers()
        return p
    }

    fun removeParticipantById(channelId: String, id: LLUUID, region: LLUUID) {
        val sess = WebRTCSessionState.matchSessionByChannelId(channelId) ?: return
        sess.findParticipantById(id)?.let { sess.removeParticipant(it) }
        notifyParticipantObservers()
    }

    fun lookupName(id: LLUUID) {
        TODO("IPC: query LLAvatarNameCache for id; call avatarNameResolved on result")
    }

    fun avatarNameResolved(id: LLUUID, name: String) {
        WebRTCSessionState.forEach { sess ->
            sess.findParticipantById(id)?.displayName = name
        }
        notifyParticipantObservers()
    }

    fun cleanupSingleton() {
        session?.shutdownAllConnections()
        nextSession?.shutdownAllConnections()
        WebRTCSessionState.clearSessions()
        statusObservers.clear()
    }

    fun cleanUp() {
        nextSession = null
        session = null
        neighboringRegions.clear()
        WebRTCSessionState.forEach { it.shutdownAllConnections() }
    }

    fun voiceConnectionCoro() {
        TODO("IPC: loop at UPDATE_THROTTLE_SECONDS; manage spatial/non-spatial sessions; update position; send position; updateOwnVolume; handle crash guard")
    }
}
