/**
 * VoiceVivox.kt
 * Converted from llvoicevivox.h / llvoicevivox.cpp
 *
 * Vivox voice-client integration for the Second Life viewer.
 * All SIP/SDP/XML protocol calls, socket I/O, coroutine state machines,
 * and audio-device management are stubbed with TODO("VIVOX: ...").
 * The session/participant data model and state flags are faithfully
 * transcribed to Kotlin idioms.
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

// ---------------------------------------------------------------------------
// Stream / session state enumerations
// ---------------------------------------------------------------------------

enum class StreamState(val code: Int) {
    UNKNOWN(0),
    IDLE(1),
    CONNECTED(2),
    RINGING(3),
    CONNECTING(6),      // matches Vivox session_media_connecting
    DISCONNECTING(7),   // matches Vivox session_media_disconnecting
}

enum class EarLocation(val code: Int) {
    CAMERA(0),
    AVATAR(1),
    SPEAKER(2),         // FS: equal voice / speaker ear
    MIXED(3),           // ear at avatar location, camera direction
}

enum class VoiceFontType(val code: Int) {
    NONE(0),
    ROOT(1),
    USER(2),
    UNKNOWN(3),
}

enum class VoiceFontStatus(val code: Int) {
    NONE(0),
    FREE(1),
    NOT_FREE(2),
    UNKNOWN(3),
}

// ---------------------------------------------------------------------------
// Participant state  (participantState inner struct)
// ---------------------------------------------------------------------------

data class ParticipantState(
    val uri: String,
    var avatarID: LLUUID = LLUUID.NULL,
    var accountName: String = "",
    var displayName: String = "",
    var lastSpokeTimestamp: Float = 0f,
    var power: Float = 0f,
    var volume: Float = 0f,
    var groupID: String = "",
    var userVolume: Int = 0,
    var isPTT: Boolean = false,
    var isSpeaking: Boolean = false,
    var isModeratorMuted: Boolean = false,
    var onMuteList: Boolean = false,
    var volumeSet: Boolean = false,
    var volumeDirty: Boolean = false,
    var avatarIDValid: Boolean = false,
    var isSelf: Boolean = false,
) {
    fun isAvatar(): Boolean = avatarIDValid
}

// ---------------------------------------------------------------------------
// Session state  (sessionState inner struct)
// ---------------------------------------------------------------------------

data class SessionState(
    var handle: String = "",
    var groupHandle: String = "",
    var sipURI: String = "",
    var alias: String = "",
    var name: String = "",
    var alternateSIPURI: String = "",
    var hash: String = "",          // channel password
    var errorStatusString: String = "",
    var imSessionID: LLUUID = LLUUID.NULL,
    var callerID: LLUUID = LLUUID.NULL,
    var errorStatusCode: Int = 0,
    var mediaStreamState: Int = StreamState.UNKNOWN.code,
    var createInProgress: Boolean = false,
    var mediaConnectInProgress: Boolean = false,
    var voiceInvitePending: Boolean = false,
    var textInvitePending: Boolean = false,
    var synthesizedCallerID: Boolean = false,
    var isChannel: Boolean = false,
    var isSpatial: Boolean = false,
    var isP2P: Boolean = false,
    var incoming: Boolean = false,
    var voiceActive: Boolean = false,
    var reconnect: Boolean = false,
    var volumeDirty: Boolean = false,
    var muteDirty: Boolean = false,
    var participantsChanged: Boolean = false,
    var voiceFontID: LLUUID = LLUUID.NULL,
) {
    private val participantsByURI: MutableMap<String, ParticipantState> = mutableMapOf()
    private val participantsByUUID: MutableMap<LLUUID, ParticipantState> = mutableMapOf()

    fun addParticipant(uri: String): ParticipantState {
        val p = ParticipantState(uri)
        participantsByURI[uri] = p
        return p
    }

    fun removeParticipant(p: ParticipantState) {
        participantsByURI.remove(p.uri)
        participantsByUUID.remove(p.avatarID)
    }

    fun removeAllParticipants() {
        participantsByURI.clear()
        participantsByUUID.clear()
    }

    fun findParticipant(uri: String): ParticipantState? = participantsByURI[uri]
    fun findParticipantByID(id: LLUUID): ParticipantState? = participantsByUUID[id]

    fun isCallBackPossible(): Boolean = !isP2P // simplified
    fun isTextIMPossible(): Boolean = !isP2P

    fun getVoiceChannelInfo(): Map<String, String> = mapOf(
        "uri"   to sipURI,
        "hash"  to hash,
        "name"  to name,
    )
}

// ---------------------------------------------------------------------------
// Voice-font entry
// ---------------------------------------------------------------------------

data class VoiceFontEntry(
    val id: LLUUID,
    var fontIndex: Int = 0,
    var fontName: String = "",
    var fontType: VoiceFontType = VoiceFontType.NONE,
    var fontStatus: VoiceFontStatus = VoiceFontStatus.NONE,
    var isNew: Boolean = false,
)

// ---------------------------------------------------------------------------
// VoiceVivox — singleton client  (LLVivoxVoiceClient)
// ---------------------------------------------------------------------------

/**
 * Singleton managing the Vivox voice-client subprocess connection.
 *
 * Lifecycle:
 *   1. [init] — launches the daemon, creates the connector.
 *   2. [connect] — joins a SIP URI channel.
 *   3. [disconnect] — leaves the current channel.
 *   4. [shutdown] — terminates the daemon cleanly.
 *
 * All socket writes, XML parsing, and coroutine state-machine transitions
 * are stubbed with TODO("VIVOX: ...").
 */
object VoiceVivox {

    // -- Connection handles ---------------------------------------------------

    /** SIP URI of the voice account server (provisioned at login). */
    var connectionUri: String = ""
        private set

    /** Connector handle returned by the Vivox connector-create response. */
    var connectorHandle: String = ""
        private set

    /** Account handle returned by the Vivox login response. */
    var accountHandle: String = ""
        private set

    // -- Account / daemon state -----------------------------------------------

    private var accountName: String = ""
    private var accountPassword: String = ""
    private var accountDisplayName: String = ""

    private var connectorEstablished: Boolean = false
    private var accountLoggedIn: Boolean = false
    private var isInitialized: Boolean = false
    private var shutdownComplete: Boolean = false
    private var isVoiceEnabled: Boolean = false
    private var processChannels: Boolean = false

    private var loginRetryCount: Int = 0
    private var numberOfAliases: Int = 0
    private var commandCookie: UInt = 0u

    private var spatialSessionURI: String = ""
    private var spatialSessionCredentials: String = ""
    private var mainSessionGroupHandle: String = ""
    private var channelName: String = ""

    // -- Audio devices --------------------------------------------------------

    private val captureDevices: MutableList<String> = mutableListOf()
    private val renderDevices:  MutableList<String> = mutableListOf()
    private var captureDevice: String = ""
    private var renderDevice:  String = ""
    private var captureDeviceDirty: Boolean = false
    private var renderDeviceDirty:  Boolean = false
    private var devicesListUpdated:  Boolean = false

    // -- Tuning ---------------------------------------------------------------

    private var tuningMode: Boolean = false
    private var tuningEnergy: Float = 0f
    private var tuningMicVolume: Int = 0
    private var tuningMicVolumeDirty: Boolean = false
    private var tuningSpeakerVolume: Int = 0
    private var tuningSpeakerVolumeDirty: Boolean = false

    // -- Volume / mute --------------------------------------------------------

    private var speakerVolume: Int = 0
    private var speakerVolumeDirty: Boolean = false
    private var speakerMuteDirty:   Boolean = false
    private var micVolume: Int = 0
    private var micVolumeDirty: Boolean = false
    private var muteMic: Boolean = false
    private var muteMicDirty: Boolean = false
    private var hidden: Boolean = false  // true during teleport

    // -- Positioning ----------------------------------------------------------

    private var spatialCoordsDirty: Boolean = false
    private var earLocation: EarLocation = EarLocation.AVATAR

    // -- Voice-font state -----------------------------------------------------

    private var voiceFontsReceived: Boolean = false
    private var voiceFontsNew: Boolean = false
    private var voiceFontListDirty: Boolean = false
    private val voiceFontMap: MutableMap<LLUUID, VoiceFontEntry> = mutableMapOf()
    private val voiceFontTemplateMap: MutableMap<LLUUID, VoiceFontEntry> = mutableMapOf()

    // -- Capture-buffer state -------------------------------------------------

    private var captureBufferMode: Boolean = false
    private var captureBufferRecording: Boolean = false
    private var captureBufferRecorded: Boolean = false
    private var captureBufferPlaying: Boolean = false

    // -- Session management ---------------------------------------------------

    private val sessionsByHandle: MutableMap<String, SessionState> = mutableMapOf()
    private var audioSession: SessionState? = null
    private var nextAudioSession: SessionState? = null

    // -- Session termination flags --------------------------------------------

    private var sessionTerminateRequested: Boolean = false
    private var relogRequested: Boolean = false
    private var spatialJoiningNum: Int = 0

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Initialise the Vivox subsystem: launch the daemon and create the
     * connector. Returns true on success.
     */
    fun init(): Boolean {
        if (isInitialized) return true
        TODO("VIVOX: launch daemon process, open socket, send connector-create XML")
    }

    /**
     * Cleanly log out and shut down the Vivox daemon.
     */
    fun shutdown() {
        TODO("VIVOX: send logout + connector-shutdown XML, close socket, kill daemon process")
    }

    // -------------------------------------------------------------------------
    // Channel management
    // -------------------------------------------------------------------------

    /**
     * Join the SIP channel at [uri].  Will create a new session if none exists
     * for that URI, or re-use an existing one.
     */
    fun connect(uri: String) {
        TODO("VIVOX: send Session.Create or Session.MediaConnect XML for URI=$uri")
    }

    /**
     * Leave the current voice channel (spatial or non-spatial).
     */
    fun disconnect() {
        TODO("VIVOX: send Session.Terminate XML for current audio session handle")
    }

    /** Leave a non-spatial (group/P2P) channel. */
    fun leaveNonSpatialChannel() {
        TODO("VIVOX: leaveNonSpatialChannel — send Session.Terminate for non-spatial session")
    }

    fun leaveChannel() {
        TODO("VIVOX: leaveChannel — determine channel type and issue appropriate terminate")
    }

    fun callUser(id: LLUUID) {
        TODO("VIVOX: callUser — build P2P SIP URI from LLUUID and connect")
    }

    fun hangup() {
        TODO("VIVOX: hangup — send Session.Terminate for current P2P session")
    }

    // -------------------------------------------------------------------------
    // Voice working state
    // -------------------------------------------------------------------------

    fun isVoiceWorking(): Boolean = connectorEstablished && accountLoggedIn && !shutdownComplete

    fun inProximalChannel(): Boolean =
        audioSession?.isSpatial ?: false

    fun inSpatialChannel(): Boolean = inProximalChannel()

    // -------------------------------------------------------------------------
    // Volume / gain
    // -------------------------------------------------------------------------

    fun setVoiceVolume(volume: Float) {
        speakerVolume = (volume * 100).toInt().coerceIn(0, 100)
        speakerVolumeDirty = true
        TODO("VIVOX: send Aux.SetSpeakerLevel XML (volume=$volume)")
    }

    fun setMicGain(volume: Float) {
        micVolume = (volume * 100).toInt().coerceIn(0, 100)
        micVolumeDirty = true
        TODO("VIVOX: send Aux.SetMicLevel XML (volume=$volume)")
    }

    fun setMuteMic(muted: Boolean) {
        muteMic = muted
        muteMicDirty = true
        TODO("VIVOX: send Connector.MuteLocalMic XML (muted=$muted)")
    }

    fun setVoiceEnabled(enabled: Boolean) {
        isVoiceEnabled = enabled
    }

    fun setHidden(hidden: Boolean) {
        this.hidden = hidden
    }

    // -------------------------------------------------------------------------
    // Tuning
    // -------------------------------------------------------------------------

    fun tuningStart() {
        tuningMode = true
        TODO("VIVOX: tuning mode start — disconnect from channels, connect to tuning fixture")
    }

    fun tuningStop() {
        tuningMode = false
        TODO("VIVOX: tuning mode stop — reconnect to prior channel")
    }

    fun inTuningMode(): Boolean = tuningMode

    fun tuningSetMicVolume(volume: Float) {
        tuningMicVolume = (volume * 100).toInt().coerceIn(0, 100)
        tuningMicVolumeDirty = true
        TODO("VIVOX: send Aux.SetMicLevel for tuning (volume=$volume)")
    }

    fun tuningSetSpeakerVolume(volume: Float) {
        tuningSpeakerVolume = (volume * 100).toInt().coerceIn(0, 100)
        tuningSpeakerVolumeDirty = true
        TODO("VIVOX: send Aux.SetSpeakerLevel for tuning (volume=$volume)")
    }

    fun tuningGetEnergy(): Float = tuningEnergy

    // -------------------------------------------------------------------------
    // Device management
    // -------------------------------------------------------------------------

    fun refreshDeviceLists(clearCurrentList: Boolean = true) {
        if (clearCurrentList) { captureDevices.clear(); renderDevices.clear() }
        devicesListUpdated = false
        TODO("VIVOX: send Aux.GetCaptureDevices + Aux.GetRenderDevices XML")
    }

    fun deviceSettingsAvailable(): Boolean = captureDevices.isNotEmpty() && renderDevices.isNotEmpty()
    fun deviceSettingsUpdated(): Boolean {
        val updated = devicesListUpdated
        devicesListUpdated = false
        return updated
    }

    fun getCaptureDevices(): List<String> = captureDevices.toList()
    fun getRenderDevices():  List<String> = renderDevices.toList()

    fun setCaptureDevice(name: String) {
        captureDevice = name; captureDeviceDirty = true
        TODO("VIVOX: send Aux.SetCaptureDevice XML (device=$name)")
    }

    fun setRenderDevice(name: String) {
        renderDevice = name; renderDeviceDirty = true
        TODO("VIVOX: send Aux.SetRenderDevice XML (device=$name)")
    }

    // -------------------------------------------------------------------------
    // Nearby speakers / participant accessors
    // -------------------------------------------------------------------------

    fun getParticipants(): Set<LLUUID> {
        TODO("VIVOX: iterate session participant map")
    }

    fun isParticipant(speakerId: LLUUID): Boolean =
        audioSession?.findParticipantByID(speakerId) != null

    fun getDisplayName(id: LLUUID): String =
        audioSession?.findParticipantByID(id)?.displayName ?: ""

    fun isParticipantAvatar(id: LLUUID): Boolean =
        audioSession?.findParticipantByID(id)?.isAvatar() ?: false

    fun getIsSpeaking(id: LLUUID): Boolean =
        audioSession?.findParticipantByID(id)?.isSpeaking ?: false

    fun getIsModeratorMuted(id: LLUUID): Boolean =
        audioSession?.findParticipantByID(id)?.isModeratorMuted ?: false

    fun getCurrentPower(id: LLUUID): Float =
        audioSession?.findParticipantByID(id)?.power ?: 0f

    fun getUserVolume(id: LLUUID): Float =
        audioSession?.findParticipantByID(id)?.volume ?: 0f

    fun setUserVolume(id: LLUUID, volume: Float) {
        audioSession?.findParticipantByID(id)?.let {
            it.volume = volume; it.volumeDirty = true
        }
        TODO("VIVOX: send Session.SetParticipantVolumeForMe XML")
    }

    // -------------------------------------------------------------------------
    // SIP URI helpers
    // -------------------------------------------------------------------------

    /** Build a sip: URI from an agent LLUUID (mirrors sipURIFromID). */
    fun sipURIFromID(id: LLUUID): String =
        "sip:${id.asString().replace("-", "")}@${voiceSIPURIHostName()}"

    private fun voiceSIPURIHostName(): String = connectionUri.substringAfterLast("//").substringBefore("/")

    // -------------------------------------------------------------------------
    // VAD (Voice Activity Detection)
    // -------------------------------------------------------------------------

    fun setupVADParams(vadAuto: UInt, vadHangover: UInt, vadNoiseFloor: UInt, vadSensitivity: UInt) {
        TODO("VIVOX: send Aux.SetVADProperties XML (auto=$vadAuto, hangover=$vadHangover, noiseFloor=$vadNoiseFloor, sensitivity=$vadSensitivity)")
    }

    // -------------------------------------------------------------------------
    // Spatial position updates
    // -------------------------------------------------------------------------

    /** Push current camera/avatar positions to the Vivox daemon. */
    fun updatePosition() {
        if (!spatialCoordsDirty) return
        TODO("VIVOX: send Session.Set3DPosition XML with camera/avatar coordinates")
    }

    fun setEarLocation(loc: EarLocation) {
        earLocation = loc
    }

    // -------------------------------------------------------------------------
    // Response / event handlers (called by the XML protocol parser)
    // -------------------------------------------------------------------------

    internal fun connectorCreateResponse(statusCode: Int, statusString: String, handle: String, versionID: String) {
        if (statusCode == 0) {
            connectorHandle = handle
            connectorEstablished = true
        }
    }

    internal fun loginResponse(statusCode: Int, statusString: String, handle: String, numberOfAliases: Int) {
        if (statusCode == 0) {
            accountHandle = handle
            accountLoggedIn = true
            this.numberOfAliases = numberOfAliases
        }
    }

    internal fun sessionCreateResponse(requestId: String, statusCode: Int, statusString: String, sessionHandle: String) {
        val session = sessionsByHandle.values.find { it.handle == requestId || it.sipURI == requestId }
        session?.let {
            it.handle = sessionHandle
            it.createInProgress = false
            if (statusCode != 0) it.errorStatusCode = statusCode
            sessionsByHandle[sessionHandle] = it
        }
    }

    internal fun sessionRemovedEvent(sessionHandle: String, sessionGroupHandle: String) {
        sessionsByHandle.remove(sessionHandle)
        if (audioSession?.handle == sessionHandle) audioSession = null
    }

    internal fun participantUpdatedEvent(
        sessionHandle: String, sessionGroupHandle: String, uri: String, alias: String,
        isModeratorMuted: Boolean, isSpeaking: Boolean, volume: Int, energy: Float
    ) {
        sessionsByHandle[sessionHandle]?.findParticipant(uri)?.let { p ->
            p.isModeratorMuted = isModeratorMuted
            p.isSpeaking = isSpeaking
            p.volume = volume / 100f
            p.power = energy
        }
    }

    internal fun auxAudioPropertiesEvent(energy: Float) {
        tuningEnergy = energy
    }

    // -------------------------------------------------------------------------
    // Session helpers
    // -------------------------------------------------------------------------

    private fun addSession(uri: String, handle: String = ""): SessionState {
        val s = SessionState(handle = handle, sipURI = uri)
        if (handle.isNotEmpty()) sessionsByHandle[handle] = s
        return s
    }

    private fun deleteSession(session: SessionState) {
        sessionsByHandle.remove(session.handle)
        if (audioSession === session) audioSession = null
    }

    private fun deleteAllSessions() {
        sessionsByHandle.clear()
        audioSession = null
        nextAudioSession = null
    }

    fun findSession(handle: String): SessionState? = sessionsByHandle[handle]

    fun findSession(participantId: LLUUID): SessionState? =
        sessionsByHandle.values.firstOrNull { it.findParticipantByID(participantId) != null }

    // -------------------------------------------------------------------------
    // Voice effects (fonts)
    // -------------------------------------------------------------------------

    fun setVoiceEffect(id: LLUUID): Boolean {
        TODO("VIVOX: send Session.SetVoiceFont XML for fontID=$id")
    }

    fun getVoiceEffect(): LLUUID {
        return audioSession?.voiceFontID ?: LLUUID.NULL
    }

    fun refreshVoiceEffectLists(clearLists: Boolean) {
        if (clearLists) { voiceFontMap.clear(); voiceFontTemplateMap.clear() }
        TODO("VIVOX: send Account.GetSessionFonts + Account.GetTemplateFonts XML")
    }

    // -------------------------------------------------------------------------
    // Capture buffer (preview)
    // -------------------------------------------------------------------------

    fun enablePreviewBuffer(enable: Boolean) {
        captureBufferMode = enable
        TODO("VIVOX: disconnect from channels for capture-buffer mode (enable=$enable)")
    }

    fun recordPreviewBuffer() {
        captureBufferRecording = true
        TODO("VIVOX: send Aux.CaptureAudioStart XML")
    }

    fun playPreviewBuffer(effectId: LLUUID = LLUUID.NULL) {
        captureBufferPlaying = true
        TODO("VIVOX: send Aux.PlayAudioBuffer XML with fontID=$effectId")
    }

    fun stopPreviewBuffer() {
        captureBufferRecording = false
        captureBufferPlaying = false
        TODO("VIVOX: send Aux.CaptureAudioStop or Aux.StopAudioBuffer XML")
    }

    fun isPreviewRecording(): Boolean = captureBufferRecording
    fun isPreviewPlaying():   Boolean = captureBufferPlaying

    // -------------------------------------------------------------------------
    // User authorization
    // -------------------------------------------------------------------------

    fun userAuthorized(userId: String, agentId: LLUUID) {
        TODO("VIVOX: provision voice account for agentId=$agentId")
    }

    // -------------------------------------------------------------------------
    // Static shutdown guard (mirrors sShuttingDown / sConnected)
    // -------------------------------------------------------------------------

    var isShuttingDown: Boolean = false
    var isConnected: Boolean = false
}

// ---------------------------------------------------------------------------
// VivoxProtocolParser stub  (LLVivoxProtocolParser)
// ---------------------------------------------------------------------------

/**
 * Minimal stub for the Vivox XML protocol parser.
 * Full Expat XML parsing is not portable to the JVM; use a SAX/DOM parser
 * if a real implementation is needed.
 */
class VivoxProtocolParser {

    private val inputBuffer: StringBuilder = StringBuilder()

    // Transient per-response fields
    private var returnCode: Int = 0
    private var statusCode: Int = 0
    private var statusString: String = ""
    private var requestId: String = ""
    private var actionString: String = ""
    private var connectorHandle: String = ""
    private var versionID: String = ""
    private var accountHandle: String = ""
    private var sessionHandle: String = ""
    private var sessionGroupHandle: String = ""
    private var isEvent: Boolean = false

    /**
     * Feed raw bytes from the Vivox daemon into the parser.
     * Full XML parsing is deferred to a native or Kotlin XML library.
     */
    fun processData(data: ByteArray) {
        TODO("VIVOX: feed data to XML parser and dispatch to VoiceVivox event handlers")
    }

    private fun reset() {
        returnCode = 0; statusCode = 0; statusString = ""
        requestId = ""; actionString = ""; connectorHandle = ""
        versionID = ""; accountHandle = ""; sessionHandle = ""
        sessionGroupHandle = ""; isEvent = false
    }
}

// ---------------------------------------------------------------------------
// VivoxSecurity stub  (LLVivoxSecurity)
// ---------------------------------------------------------------------------

/**
 * Stores the connector and account handles generated at session start.
 * In the C++ code this is a separate singleton that holds secrets;
 * here it is a simple object with mutable properties.
 */
object VivoxSecurity {
    var connectorHandle: String = ""
    var accountHandle: String = ""
}

// ---------------------------------------------------------------------------
// VoiceVivoxStats stub  (LLVoiceVivoxStats)
// ---------------------------------------------------------------------------

/** Tracks timing statistics for Vivox connection attempts. */
object VoiceVivoxStats {
    private var connectCycles: UInt = 0u
    private var connectAttempts: UInt = 0u
    private var connectTime: Double = 0.0
    private var provisionAttempts: UInt = 0u
    private var provisionTime: Double = 0.0
    private var establishAttempts: UInt = 0u
    private var establishTime: Double = 0.0

    fun reset() {
        connectCycles = 0u; connectAttempts = 0u; connectTime = 0.0
        provisionAttempts = 0u; provisionTime = 0.0
        establishAttempts = 0u; establishTime = 0.0
    }

    fun connectionAttemptStart() { connectAttempts++ }
    fun connectionAttemptEnd(success: Boolean) { if (success) connectCycles++ }
    fun provisionAttemptStart() { provisionAttempts++ }
    fun provisionAttemptEnd(success: Boolean) { /* timing stub */ }
    fun establishAttemptStart() { establishAttempts++ }
    fun establishAttemptEnd(success: Boolean) { /* timing stub */ }

    fun read(): Map<String, Any> = mapOf(
        "connect_cycles"      to connectCycles,
        "connect_attempts"    to connectAttempts,
        "provision_attempts"  to provisionAttempts,
        "establish_attempts"  to establishAttempts,
    )
}
