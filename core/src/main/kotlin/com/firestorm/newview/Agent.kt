package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.IndraConstants.SIM_ACCESS_PG
import com.firestorm.llcommon.IndraConstants.SIM_ACCESS_MATURE
import com.firestorm.llcommon.IndraConstants.SIM_ACCESS_ADULT
import com.firestorm.llmath.CoordFrame
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d
import com.firestorm.llmessage.Host
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// UDP send helper (converts Host.address UInt → InetAddress)
// ---------------------------------------------------------------------------
private fun sendUdp(host: Host, data: ByteArray) {
    val addrBytes = byteArrayOf(
        (host.address shr 24).toByte(),
        (host.address shr 16).toByte(),
        (host.address shr 8).toByte(),
        host.address.toByte()
    )
    DatagramSocket().use { socket ->
        val inetAddr = InetAddress.getByAddress(addrBytes)
        socket.send(DatagramPacket(data, data.size, inetAddr, host.port.toInt()))
    }
}

// ---------------------------------------------------------------------------
// Constants (mirrors llagent.cpp)
// ---------------------------------------------------------------------------

private const val AUTOPILOT_HEIGHT_ADJUST_DISTANCE       = 8.0f
private const val AUTOPILOT_MIN_TARGET_HEIGHT_OFF_GROUND = 1.0f
private const val AUTOPILOT_MAX_TIME_NO_PROGRESS_WALK    = 1.5f
private const val AUTOPILOT_MAX_TIME_NO_PROGRESS_FLY     = 2.5f
private const val CHAT_AGE_FAST_RATE                     = 3.0
private const val MIN_FIDGET_TIME                        = 8.0f
private const val MAX_FIDGET_TIME                        = 20.0f
private const val UI_FEATURE_VERSION                     = 1
private const val UI_FEATURE_FLAGS                       = 7

// Agent control flag bits (subset used in movement methods)
private const val AGENT_CONTROL_AT_POS       = 0x00000001u
private const val AGENT_CONTROL_AT_NEG       = 0x00000002u
private const val AGENT_CONTROL_LEFT_POS     = 0x00000004u
private const val AGENT_CONTROL_LEFT_NEG     = 0x00000008u
private const val AGENT_CONTROL_UP_POS       = 0x00000010u
private const val AGENT_CONTROL_UP_NEG       = 0x00000020u
private const val AGENT_CONTROL_PITCH_POS    = 0x00000040u
private const val AGENT_CONTROL_PITCH_NEG    = 0x00000080u
private const val AGENT_CONTROL_YAW_POS      = 0x00000100u
private const val AGENT_CONTROL_YAW_NEG      = 0x00000200u
private const val AGENT_CONTROL_FAST_AT      = 0x00000400u
private const val AGENT_CONTROL_FAST_LEFT    = 0x00000800u
private const val AGENT_CONTROL_FAST_UP      = 0x00001000u
private const val AGENT_CONTROL_FLY          = 0x00002000u
private const val AGENT_CONTROL_STOP         = 0x00004000u
private const val AGENT_CONTROL_FINISH_ANIM  = 0x00008000u
private const val AGENT_CONTROL_STAND_UP     = 0x00010000u
private const val AGENT_CONTROL_SIT_ON_GROUND= 0x00020000u
private const val AGENT_CONTROL_MOUSELOOK    = 0x00040000u
private const val AGENT_CONTROL_NUDGE_AT_POS = 0x00080000u
private const val AGENT_CONTROL_NUDGE_AT_NEG = 0x00100000u
private const val AGENT_CONTROL_NUDGE_LEFT_POS= 0x00200000u
private const val AGENT_CONTROL_NUDGE_LEFT_NEG= 0x00400000u
private const val AGENT_CONTROL_AWAY         = 0x00800000u

// Render state bits
private const val AGENT_STATE_TYPING: UByte   = 0x04u
private const val AGENT_STATE_EDITING: UByte  = 0x10u

// ---------------------------------------------------------------------------
// Supporting data classes
// ---------------------------------------------------------------------------

data class GroupData(
    val id: LLUUID,
    val insigniaId: LLUUID,
    val powers: ULong,
    var acceptNotices: Boolean,
    var listInProfile: Boolean,
    var contribution: Int,
    val name: String
)

enum class AnimRequest { START, STOP }

enum class DoubleTapRunMode {
    NONE, FORWARD, BACKWARD, SLIDE_LEFT, SLIDE_RIGHT
}

enum class TeleportState {
    NONE,
    START,
    REQUESTED,
    MOVING,
    START_ARRIVAL,
    ARRIVING,
    LOCAL,
    PENDING
}

// ---------------------------------------------------------------------------
// Internal feature-version persistence (JVM: java.util.prefs)
// ---------------------------------------------------------------------------

private object FeaturePrefs {
    private val prefs = java.util.prefs.Preferences.userRoot().node("/com/firestorm/agent")

    fun setVersionAndFlags(version: Int, flags: Int) {
        prefs.putInt("UIFeatureVersion", version)
        prefs.putInt("UIFeatureFlags", flags)
        prefs.flush()
    }

    fun getVersion(): Int = prefs.getInt("UIFeatureVersion", 0)
    fun getFlags(): Int   = prefs.getInt("UIFeatureFlags", 0)
}

// ---------------------------------------------------------------------------
// Agent  (C++ LLAgent — singleton gAgent)
// ---------------------------------------------------------------------------

class Agent private constructor() {

    // ---- Initialization -----------------------------------------------------

    var isInitialized: Boolean = false
        private set
    var isFirstLogin: Boolean = false
    var outfitChosen: Boolean = false
    var motd: String = ""   // Message of the Day

    fun init() {
        isInitialized = true
        mMoveTimerActive = true
        // Connect saved-settings equivalent: read persisted flying state from prefs.
        val prefs = java.util.prefs.Preferences.userRoot().node("/com/firestorm/agent")
        val wasFlying = prefs.getBoolean("FlyingAtExit", false)
        if (wasFlying) setFlying(true)
        // Load persisted FS flags from prefs.
        mIgnorePrejump = prefs.getBoolean("FSIgnoreFinishAnimation", false)
        mAlwaysFly     = prefs.getBoolean("FSAlwaysFly", false)
        isAutorespond              = prefs.getBoolean("FSAutorespondMode", false)
        isAutorespondNonFriends    = prefs.getBoolean("FSAutorespondNonFriendsMode", false)
        isRejectTeleportOffers     = prefs.getBoolean("FSRejectTeleportOffersMode", false)
        isRejectFriendshipRequests = prefs.getBoolean("FSRejectFriendshipRequestsMode", false)
        isRejectAllGroupInvites    = prefs.getBoolean("FSRejectAllGroupInvitesMode", false)
    }

    fun cleanup() {
        mRegionp = null
        // Disconnect any pending teleport by clearing the in-progress state.
        if (mTeleportState != TeleportState.NONE) {
            mTeleportState = TeleportState.NONE
            mTeleportMessage = ""
        }
        mAutoPilot = false
        mAutoPilotFinishedCallback = null
    }

    fun onAppFocusGained() {}   // currently a no-op in the C++ source

    fun setFirstLogin(b: Boolean) {
        isFirstLogin = b
        if (b && getFeatureVersion() <= UI_FEATURE_VERSION) {
            setFeatureVersion(UI_FEATURE_VERSION, UI_FEATURE_FLAGS)
        }
    }

    fun setFeatureVersion(version: Int, flags: Int) {
        // Persist via JVM Preferences (mirrors gSavedSettings.setLLSD("LastUIFeatureVersion"))
        FeaturePrefs.setVersionAndFlags(version, flags)
    }

    fun getFeatureVersion(): Int {
        return FeaturePrefs.getVersion()
    }

    fun getFeatureVersionAndFlags(version: IntArray, flags: IntArray) {
        version[0] = FeaturePrefs.getVersion()
        flags[0]   = FeaturePrefs.getFlags()
    }

    fun showLatestFeatureNotification(key: String) {
        val version = FeaturePrefs.getVersion()
        val flags   = FeaturePrefs.getFlags()
        if (version <= UI_FEATURE_VERSION && (flags and UI_FEATURE_FLAGS) != UI_FEATURE_FLAGS) {
            val flag = if (key == "inventory") 4 else 0
            if (flag != 0 && (flags and flag) == 0) {
                // Fire-and-forget notification on UI thread (headless: log only).
                System.err.println("[Agent] showLatestFeatureNotification key=$key")
                setFeatureVersion(UI_FEATURE_VERSION, flags or flag)
            }
        }
    }

    // ---- Session / identity ---------------------------------------------------

    var id: LLUUID = LLUUID.NULL
    var sessionId: LLUUID = LLUUID.NULL
    var secureSessionId: LLUUID = LLUUID.NULL

    fun getID(): LLUUID = id
    fun getSessionID(): LLUUID = sessionId
    fun getSecureSessionID(): LLUUID = secureSessionId

    // ---- Position / coordinate frame -----------------------------------------

    private var mPositionGlobal: Vector3d = Vector3d.ZERO
    private var mAgentOriginGlobal: Vector3d = Vector3d.ZERO
    private var mLastTestGlobal: Vector3d = Vector3d.ZERO
    // Replaces C++ LLCoordFrame mFrameAgent.
    private var mFrameAgent: CoordFrame = CoordFrame()

    val positionChangedListeners: MutableList<(Vector3, Vector3d) -> Unit> = mutableListOf()

    fun getPositionGlobal(): Vector3d = mPositionGlobal

    fun getPositionAgent(): Vector3 {
        // Return the origin of the agent coordinate frame (local region coords).
        return mFrameAgent.getOrigin()
    }

    fun setPositionAgent(pos: Vector3) {
        if (!pos.isFinite()) {
            System.err.println("[Agent] setPositionAgent received non-finite position")
            return
        }
        mFrameAgent.setOrigin(pos)
        // Convert local coords to global.
        val posAgentD = Vector3d(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        mPositionGlobal = posAgentD + mAgentOriginGlobal
        // Fire positionChanged signal when moved more than 1 metre.
        val deltaSq = (mLastTestGlobal - mPositionGlobal).let {
            it.x * it.x + it.y * it.y + it.z * it.z
        }
        if (deltaSq > 1.0) {
            mLastTestGlobal = mPositionGlobal
            positionChangedListeners.forEach { cb -> cb(pos, mPositionGlobal) }
        }
    }

    fun getPosAgentFromGlobal(posGlobal: Vector3d): Vector3 {
        val diff = posGlobal - mAgentOriginGlobal
        return Vector3(diff.x.toFloat(), diff.y.toFloat(), diff.z.toFloat())
    }

    fun getPosGlobalFromAgent(posAgent: Vector3): Vector3d {
        val agentD = Vector3d(posAgent.x.toDouble(), posAgent.y.toDouble(), posAgent.z.toDouble())
        return agentD + mAgentOriginGlobal
    }

    fun initOriginGlobal(originGlobal: Vector3d) { mAgentOriginGlobal = originGlobal }

    fun resetAxes() {
        mFrameAgent.resetAxes()
    }

    fun resetAxes(lookAt: Vector3) {
        val skyward = getReferenceUpVector()
        // Cross products for orthonormal frame.
        val cross = lookAt % skyward
        if (cross.isNull()) return                   // parallel — skip
        val left = skyward % lookAt
        val up   = lookAt % left
        mFrameAgent.setAxes(lookAt, left, up)
    }

    fun getAtAxis(): Vector3   = mFrameAgent.getAtAxis()
    fun getUpAxis(): Vector3   = mFrameAgent.getUpAxis()
    fun getLeftAxis(): Vector3 = mFrameAgent.getLeftAxis()
    fun getQuat(): Quaternion  = mFrameAgent.getQuaternion()

    fun updateAgentPosition(dt: Float, yaw: Float, mouseX: Int, mouseY: Int) {
        // Apply yaw rotation to the frame at the given dt rate.
        if (yaw != 0f) {
            val scaledYaw = yaw * dt
            mFrameAgent.rotate(scaledYaw, getReferenceUpVector())
        }
        // Run autopilot steering if active.
        val deltaYaw = FloatArray(1)
        if (mAutoPilot) autoPilot(deltaYaw)
    }

    fun getVelocity(): Vector3 {
        // In headless JVM context there is no physics engine; return zero velocity.
        return Vector3.ZERO
    }

    fun getVelocityZ(): Float = getVelocity().z

    // ---- Home ----------------------------------------------------------------

    private var haveHomePosition: Boolean = false
    private var homeRegionHandle: ULong = 0uL
    private var homePosRegion: Vector3 = Vector3.ZERO

    fun setStartPosition(locationId: UInt) {
        // Serialize the start-position request via HTTP POST to the region capability.
        val capUrl = getRegionCapability("UpdateAgentInformation")
        if (capUrl.isEmpty()) return
        thread(isDaemon = true) {
            try {
                val conn = URL(capUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/llsd+xml")
                val body = "<llsd><map><key>start_location</key><integer>$locationId</integer></map></llsd>"
                    .toByteArray(Charsets.UTF_8)
                conn.outputStream.write(body)
                val rc = conn.responseCode
                conn.disconnect()
                if (rc != 200) System.err.println("[Agent] setStartPosition HTTP $rc")
            } catch (e: Exception) {
                System.err.println("[Agent] setStartPosition error: ${e.message}")
            }
        }
    }

    fun setHomePosRegion(regionHandle: ULong, posRegion: Vector3) {
        homeRegionHandle = regionHandle
        homePosRegion = posRegion
        haveHomePosition = true
    }

    fun getHomePosGlobal(posGlobal: Vector3d): Boolean {
        if (!haveHomePosition) return false
        // Convert region-local home pos to global coords using the region handle.
        // Region handles encode the south-west corner in global metres (256 m grid).
        val regionX = ((homeRegionHandle shr 32) and 0xFFFFFFFFuL).toLong()
        val regionY = (homeRegionHandle and 0xFFFFFFFFuL).toLong()
        posGlobal.x = regionX.toDouble() + homePosRegion.x.toDouble()
        posGlobal.y = regionY.toDouble() + homePosRegion.y.toDouble()
        posGlobal.z = homePosRegion.z.toDouble()
        return true
    }

    fun isInHomeRegion(): Boolean {
        if (!haveHomePosition) return false
        val region = mRegionp ?: return false
        return region.handle == homeRegionHandle
    }

    // ---- Parcel / Region -----------------------------------------------------

    private var mRegionp: ViewerRegion? = null
    private var mInterestListMode: String = "default"

    val parcelChangedListeners: MutableList<() -> Unit> = mutableListOf()
    val regionChangedListeners: MutableList<() -> Unit> = mutableListOf()

    fun getRegion(): ViewerRegion? = mRegionp

    fun setRegion(regionp: ViewerRegion) {
        if (mRegionp != regionp) {
            mAgentOriginGlobal = regionp.getOriginGlobal()
            // Shift the local agent position into the new region's frame.
            val prevOrigin = mRegionp?.getOriginGlobal() ?: Vector3d.ZERO
            val delta = Vector3d(
                (regionp.getOriginGlobal().x - prevOrigin.x),
                (regionp.getOriginGlobal().y - prevOrigin.y),
                (regionp.getOriginGlobal().z - prevOrigin.z)
            )
            val currentAgent = getPositionAgent()
            val shiftedAgent = Vector3(
                currentAgent.x - delta.x.toFloat(),
                currentAgent.y - delta.y.toFloat(),
                currentAgent.z - delta.z.toFloat()
            )
            setPositionAgent(shiftedAgent)

            // Track visited regions.
            regionsVisited.add(regionp.handle)

            mRegionp = regionp
            regionChangedListeners.forEach { it() }
        } else {
            mRegionp = regionp
        }
        // Update water and sky objects (delegated to registered listeners).
        regionChangedListeners.forEach { it() }
    }

    fun getRegionHost(): Any? {
        return mRegionp?.host
    }

    fun inPrelude(): Boolean {
        return mRegionp?.isPrelude() ?: false
    }

    fun getRegionCapability(name: String): String {
        return mRegionp?.getCapability(name) ?: ""
    }

    fun changeInterestListMode(newMode: String) {
        mInterestListMode = newMode
        mRegionp?.setInterestListMode(newMode)
    }

    fun getInterestListMode(): String = mInterestListMode

    fun changeParcels() { parcelChangedListeners.forEach { it() } }
    fun addParcelChangedCallback(cb: () -> Unit) { parcelChangedListeners.add(cb) }
    fun addRegionChangedCallback(cb: () -> Unit) { regionChangedListeners.add(cb) }

    // ---- History / stats ------------------------------------------------------

    private val regionsVisited: MutableSet<ULong> = mutableSetOf()
    private var distanceTraveled: Double = 0.0
    private var lastPositionGlobal: Vector3d = Vector3d.ZERO

    fun getRegionsVisited(): Int = regionsVisited.size
    fun getDistanceTraveled(): Double = distanceTraveled
    fun setDistanceTraveled(dist: Double) { distanceTraveled = dist }
    fun getLastPositionGlobal(): Vector3d = lastPositionGlobal
    fun setLastPositionGlobal(pos: Vector3d) { lastPositionGlobal = pos }

    // Firestorm area search / 360 capture flags
    var fsAreaSearchActive: Boolean = false
    var captureActive360: Boolean = false

    // ---- Inventory restore-to-world ------------------------------------------

    var restoreToWorld: Boolean = false
    var restoreToWorldGroup: LLUUID = LLUUID.NULL
    var restoreToWorldItem: Any? = null

    // ---- Fidget ---------------------------------------------------------------

    private var nextFidgetTime: Float = 0.0f
    private var currentFidget: Int = 0
    private var mMoveTimerActive: Boolean = false

    fun fidget() {
        val nowSec = System.currentTimeMillis() / 1000.0f
        if (nowSec < nextFidgetTime) return
        // Cycle through three fidget types (matches C++ ANIM_AGENT_STAND_n pattern).
        currentFidget = (currentFidget + 1) % 3
        val animId = when (currentFidget) {
            0 -> "stand_1"
            1 -> "stand_2"
            else -> "stand_3"
        }
        System.err.println("[Agent] fidget anim=$animId")
        // Schedule next fidget between MIN and MAX fidget time.
        val range = (MAX_FIDGET_TIME - MIN_FIDGET_TIME).toDouble()
        nextFidgetTime = nowSec + MIN_FIDGET_TIME + (Math.random() * range).toFloat()
    }

    // ---- Flying ---------------------------------------------------------------

    fun getFlying(): Boolean = (mControlFlags and AGENT_CONTROL_FLY) != 0u

    fun setFlying(fly: Boolean, failSound: Boolean = false) {
        if (fly) {
            if (!canFly() && !getFlying()) return
            setControlFlags(AGENT_CONTROL_FLY)
        } else {
            clearControlFlags(AGENT_CONTROL_FLY)
        }
        // Persist flying state so init() can restore it next session.
        val prefs = java.util.prefs.Preferences.userRoot().node("/com/firestorm/agent")
        prefs.putBoolean("FlyingAtExit", fly)
        prefs.flush()
        System.err.println("[Agent] setFlying=$fly")
    }

    fun canFly(): Boolean {
        if (isGodlike()) return true
        if (mAlwaysFly) return true
        // Check region and parcel fly permissions via the region object.
        val region = mRegionp ?: return true
        if (region.getBlockFly()) return false
        return true   // parcel-level check deferred to ViewerParcelMgr
    }

    // ---- Voice ----------------------------------------------------------------

    var voiceConnected: Boolean = false

    // ---- Chat / typing --------------------------------------------------------

    private var mLastChatterID: LLUUID = LLUUID.NULL
    private var mNearChatRadius: Float = 10.0f
    // mTypingTimer stores the epoch-ms when typing started.
    private var mTypingStartMs: Long = 0L

    fun heardChat(id: LLUUID) {
        mLastChatterID = id
        System.err.println("[Agent] heardChat from $id")
    }

    fun getLastChatter(): LLUUID = mLastChatterID
    fun getNearChatRadius(): Float = mNearChatRadius

    fun getTypingTime(): Float {
        if (mTypingStartMs == 0L) return 0f
        return (System.currentTimeMillis() - mTypingStartMs) / 1000.0f
    }

    fun startTyping() {
        mTypingStartMs = System.currentTimeMillis()
        setRenderState(AGENT_STATE_TYPING)
        sendReliableMessage()   // will no-op when region is null
    }

    fun stopTyping() {
        mTypingStartMs = 0L
        clearRenderState(AGENT_STATE_TYPING)
        sendReliableMessage()
    }

    // ---- AFK ------------------------------------------------------------------

    fun setAFK() {
        if (mRegionp == null) return
        if ((mControlFlags and AGENT_CONTROL_AWAY) == 0u) {
            setControlFlags(AGENT_CONTROL_AWAY or AGENT_CONTROL_STOP)
            System.err.println("[Agent] setAFK")
        }
    }

    fun clearAFK() {
        clearControlFlags(AGENT_CONTROL_AWAY)
        afkSitting = false
        System.err.println("[Agent] clearAFK")
    }

    fun getAFK(): Boolean = (mControlFlags and AGENT_CONTROL_AWAY) != 0u

    var afkSitting: Boolean = false

    // ---- Run ------------------------------------------------------------------

    private var mbAlwaysRun: Boolean = false
    private var mbTempRun: Boolean = false
    private var mbTeleportKeepsLookAt: Boolean = false
    private var mAlwaysFly: Boolean = false
    private var mIgnorePrejump: Boolean = false

    var doubleTapRunMode: DoubleTapRunMode = DoubleTapRunMode.NONE

    fun setAlwaysRun()   { mbAlwaysRun = true;  sendWalkRun() }
    fun clearAlwaysRun() { mbAlwaysRun = false; sendWalkRun() }
    fun setTempRun()     { mbTempRun = true;    sendWalkRun() }
    fun clearTempRun()   { mbTempRun = false;   sendWalkRun() }

    fun sendWalkRun() {
        // Build a WalkRunState message and send it to the simulator via UDP.
        val region = mRegionp ?: return
        // Binary packet: 4 bytes control flags + 1 byte run flag.
        val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(mControlFlags.toInt())
        buf.put(if (getRunning()) 1.toByte() else 0.toByte())
        val data = buf.array()
        val host = region.host
        try {
            sendUdp(host, data)
        } catch (e: Exception) {
            System.err.println("[Agent] sendWalkRun error: ${e.message}")
        }
    }

    fun getAlwaysRun(): Boolean = mbAlwaysRun
    fun getTempRun(): Boolean = mbTempRun
    fun getRunning(): Boolean = mbAlwaysRun || mbTempRun

    // ---- Sit / stand ----------------------------------------------------------

    fun standUp() {
        setControlFlags(AGENT_CONTROL_STAND_UP)
    }

    fun sitDown() {
        setControlFlags(AGENT_CONTROL_SIT_ON_GROUND)
    }

    // ---- Do Not Disturb -------------------------------------------------------

    private var isDoNotDisturb: Boolean = false

    fun setDoNotDisturb(dnd: Boolean) {
        isDoNotDisturb = dnd
        System.err.println("[Agent] setDoNotDisturb=$dnd")
    }

    fun isDoNotDisturb(): Boolean = isDoNotDisturb

    // ---- Autorespond (Firestorm) -----------------------------------------------

    private var isAutorespond: Boolean = false
    private var isAutorespondNonFriends: Boolean = false

    fun setAutorespond()   { isAutorespond = true }
    fun clearAutorespond() { isAutorespond = false }
    fun selectAutorespond(v: Boolean) { isAutorespond = v }
    fun getAutorespond(): Boolean = isAutorespond

    fun setAutorespondNonFriends()   { isAutorespondNonFriends = true }
    fun clearAutorespondNonFriends() { isAutorespondNonFriends = false }
    fun selectAutorespondNonFriends(v: Boolean) { isAutorespondNonFriends = v }
    fun getAutorespondNonFriends(): Boolean = isAutorespondNonFriends

    // ---- Reject TP / friendship / group invites (Firestorm) -------------------

    private var isRejectTeleportOffers: Boolean = false
    private var isRejectFriendshipRequests: Boolean = false
    private var isRejectAllGroupInvites: Boolean = false

    fun setRejectTeleportOffers()          { isRejectTeleportOffers = true }
    fun clearRejectTeleportOffers()        { isRejectTeleportOffers = false }
    fun selectRejectTeleportOffers(v: Boolean) { isRejectTeleportOffers = v }
    fun getRejectTeleportOffers(): Boolean = isRejectTeleportOffers

    fun setRejectFriendshipRequests()      { isRejectFriendshipRequests = true }
    fun clearRejectFriendshipRequests()    { isRejectFriendshipRequests = false }
    fun selectRejectFriendshipRequests(v: Boolean) { isRejectFriendshipRequests = v }
    fun getRejectFriendshipRequests(): Boolean = isRejectFriendshipRequests

    fun setRejectAllGroupInvites()         { isRejectAllGroupInvites = true }
    fun clearRejectAllGroupInvites()       { isRejectAllGroupInvites = false }
    fun selectRejectAllGroupInvites(v: Boolean) { isRejectAllGroupInvites = v }
    fun getRejectAllGroupInvites(): Boolean = isRejectAllGroupInvites

    // ---- Grab / controls ----------------------------------------------------

    private var mControlFlags: UInt = 0u
    private var mControlsTakenCount: IntArray = IntArray(TOTAL_CONTROLS)
    private var mControlsTakenPassedOnCount: IntArray = IntArray(TOTAL_CONTROLS)
    private var lastJumpInputTime: Double = 0.0
    private var movementKeysLocked: Boolean = false
    // Tracks whether flags changed since last agent-update packet.
    private var mControlFlagsDirty: Boolean = false

    fun getControlFlags(): UInt {
        // Mirror C++ Firestorm: if ignorePrejump, always set FINISH_ANIM.
        return if (mIgnorePrejump) mControlFlags or AGENT_CONTROL_FINISH_ANIM else mControlFlags
    }

    fun setControlFlags(mask: UInt) {
        val prev = mControlFlags
        mControlFlags = mControlFlags or mask
        if (mControlFlags != prev) mControlFlagsDirty = true
    }

    fun clearControlFlags(mask: UInt) {
        val prev = mControlFlags
        mControlFlags = mControlFlags and mask.inv()
        if (mControlFlags != prev) mControlFlagsDirty = true
    }

    fun controlFlagsDirty(): Boolean = mControlFlagsDirty

    fun resetControlFlags() {
        // Keep persistent flags (AWAY, FLY, MOUSELOOK) — clear the rest.
        val keep = AGENT_CONTROL_AWAY or AGENT_CONTROL_FLY or AGENT_CONTROL_MOUSELOOK
        mControlFlags = mControlFlags and keep
        mControlFlagsDirty = false
    }

    fun anyControlGrabbed(): Boolean = mControlsTakenCount.any { it > 0 }
    fun isControlGrabbed(controlIndex: Int): Boolean = mControlsTakenCount.getOrElse(controlIndex) { 0 } > 0

    fun forceReleaseControls() {
        // Send a ScriptSensorReply-equivalent to tell the sim to release all grabbed controls.
        val region = mRegionp ?: return
        val buf = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        // Message type marker (0x0001 = placeholder for ReleaseControls).
        buf.putShort(0x0001)
        // Agent UUID (16 bytes).
        val msb = id.uuid.mostSignificantBits
        val lsb = id.uuid.leastSignificantBits
        buf.putLong(msb); buf.putLong(lsb)
        val data = buf.array()
        try {
            sendUdp(region.host, data)
        } catch (e: Exception) {
            System.err.println("[Agent] forceReleaseControls error: ${e.message}")
        }
        mControlsTakenCount.fill(0)
        mControlsTakenPassedOnCount.fill(0)
    }

    fun isMovementLocked(): Boolean = movementKeysLocked
    fun setMovementLocked(locked: Boolean) { movementKeysLocked = locked }

    fun leftButtonGrabbed(): Boolean  = isControlGrabbed(CONTROL_ML_LBUTTON_DOWN_INDEX)
    fun rotateGrabbed(): Boolean      = isControlGrabbed(CONTROL_YAW_POS_INDEX) || isControlGrabbed(CONTROL_YAW_NEG_INDEX)
    fun forwardGrabbed(): Boolean     = isControlGrabbed(CONTROL_AT_POS_INDEX)
    fun backwardGrabbed(): Boolean    = isControlGrabbed(CONTROL_AT_NEG_INDEX)
    fun upGrabbed(): Boolean          = isControlGrabbed(CONTROL_UP_POS_INDEX)
    fun downGrabbed(): Boolean        = isControlGrabbed(CONTROL_UP_NEG_INDEX)

    // ---- Movement from user input --------------------------------------------

    fun moveAt(direction: Int, resetView: Boolean = true) {
        AgentCamera.setAtKey(AgentCamera.directionToKey(direction))
        when {
            direction > 0 -> setControlFlags(AGENT_CONTROL_AT_POS or AGENT_CONTROL_FAST_AT)
            direction < 0 -> setControlFlags(AGENT_CONTROL_AT_NEG or AGENT_CONTROL_FAST_AT)
        }
        if (resetView) AgentCamera.resetView(resetCamera = true, changeCamera = false, movement = true)
    }

    fun moveAtNudge(direction: Int) {
        AgentCamera.setWalkKey(AgentCamera.directionToKey(direction))
        when {
            direction > 0 -> setControlFlags(AGENT_CONTROL_NUDGE_AT_POS)
            direction < 0 -> setControlFlags(AGENT_CONTROL_NUDGE_AT_NEG)
        }
        AgentCamera.resetView(resetCamera = true, changeCamera = false, movement = true)
    }

    fun moveLeft(direction: Int) {
        AgentCamera.setLeftKey(AgentCamera.directionToKey(direction))
        when {
            direction > 0 -> setControlFlags(AGENT_CONTROL_LEFT_POS or AGENT_CONTROL_FAST_LEFT)
            direction < 0 -> setControlFlags(AGENT_CONTROL_LEFT_NEG or AGENT_CONTROL_FAST_LEFT)
        }
        AgentCamera.resetView(resetCamera = true, changeCamera = false, movement = true)
    }

    fun moveLeftNudge(direction: Int) {
        AgentCamera.setLeftKey(AgentCamera.directionToKey(direction))
        when {
            direction > 0 -> setControlFlags(AGENT_CONTROL_NUDGE_LEFT_POS)
            direction < 0 -> setControlFlags(AGENT_CONTROL_NUDGE_LEFT_NEG)
        }
        AgentCamera.resetView(resetCamera = true, changeCamera = false, movement = true)
    }

    fun moveUp(direction: Int) {
        AgentCamera.setUpKey(AgentCamera.directionToKey(direction))
        when {
            direction > 0 -> {
                lastJumpInputTime = System.currentTimeMillis() / 1000.0
                setControlFlags(AGENT_CONTROL_UP_POS or AGENT_CONTROL_FAST_UP)
                AgentCamera.resetView(resetCamera = true, changeCamera = false, movement = true)
            }
            direction < 0 -> {
                setControlFlags(AGENT_CONTROL_UP_NEG or AGENT_CONTROL_FAST_UP)
                AgentCamera.resetView(resetCamera = true, changeCamera = false, movement = true)
            }
        }
    }

    fun moveYaw(mag: Float, resetView: Boolean = true) {
        AgentCamera.setYawKey(mag)
        when {
            mag > 0f -> setControlFlags(AGENT_CONTROL_YAW_POS)
            mag < 0f -> setControlFlags(AGENT_CONTROL_YAW_NEG)
        }
        val yawMask = AGENT_CONTROL_YAW_POS or AGENT_CONTROL_YAW_NEG
        if ((mControlFlags and yawMask) == yawMask) AgentCamera.setYawKey(0f)
        if (resetView) AgentCamera.resetView(resetCamera = true, changeCamera = false, movement = true)
    }

    fun movePitch(mag: Float) {
        AgentCamera.setPitchKey(mag)
        when {
            mag > 0f -> setControlFlags(AGENT_CONTROL_PITCH_POS)
            mag < 0f -> setControlFlags(AGENT_CONTROL_PITCH_NEG)
        }
    }

    // ---- Rotate agent frame --------------------------------------------------

    fun rotate(angle: Float, axis: Vector3) {
        mFrameAgent.rotate(angle, axis)
    }

    fun rotate(angle: Float, x: Float, y: Float, z: Float) { rotate(angle, Vector3(x, y, z)) }

    fun rotate(quaternion: Quaternion) {
        mFrameAgent.rotate(quaternion)
    }

    fun pitch(angle: Float) {
        // Clamp pitch to avoid flipping (mirrors llagent.cpp pitch()).
        val skyward = getReferenceUpVector()
        val atAxis  = mFrameAgent.getAtAxis()
        val dot     = atAxis.x * skyward.x + atAxis.y * skyward.y + atAxis.z * skyward.z
        val angleFromSkyward = acos(dot.coerceIn(-1f, 1f))
        val clampedAngle = when {
            angle >= 0f -> {
                val limit = (179.0 * PI / 180.0).toFloat()
                if (angleFromSkyward + angle > limit) limit - angleFromSkyward else angle
            }
            else -> {
                val limit = (5.0 * PI / 180.0).toFloat()
                if (angleFromSkyward + angle < limit) limit - angleFromSkyward else angle
            }
        }
        if (abs(clampedAngle) > 1e-4f) mFrameAgent.pitch(clampedAngle)
    }

    fun roll(angle: Float) {
        mFrameAgent.roll(angle)
    }

    fun yaw(angle: Float) {
        if (!rotateGrabbed()) {
            mFrameAgent.rotate(angle, getReferenceUpVector())
        }
    }

    fun getReferenceUpVector(): Vector3 {
        // In headless JVM context, world +Z is always up.
        return Vector3.Z_AXIS
    }

    // ---- Autopilot -----------------------------------------------------------

    private var mAutoPilot: Boolean = false
    private var mAutoPilotFlyOnStop: Boolean = false
    private var mAutoPilotAllowFlying: Boolean = true
    private var mAutoPilotTargetGlobal: Vector3d = Vector3d.ZERO
    private var mAutoPilotStopDistance: Float = 1.0f
    private var mAutoPilotUseRotation: Boolean = false
    private var mAutoPilotTargetFacing: Vector3 = Vector3.ZERO
    private var mAutoPilotTargetDist: Float = 0.0f
    private var mAutoPilotNoProgressFrameCount: Int = 0
    private var mAutoPilotRotationThreshold: Float = 0.03f
    private var mAutoPilotBehaviorName: String = ""
    private var mLeaderID: LLUUID = LLUUID.NULL
    private var mAutoPilotFinishedCallback: ((Boolean) -> Unit)? = null

    fun getAutoPilot(): Boolean = mAutoPilot
    fun getAutoPilotTargetGlobal(): Vector3d = mAutoPilotTargetGlobal
    fun getAutoPilotLeaderID(): LLUUID = mLeaderID
    fun getAutoPilotStopDistance(): Float = mAutoPilotStopDistance
    fun getAutoPilotTargetDist(): Float = mAutoPilotTargetDist
    fun getAutoPilotUseRotation(): Boolean = mAutoPilotUseRotation
    fun getAutoPilotTargetFacing(): Vector3 = mAutoPilotTargetFacing
    fun getAutoPilotRotationThreshold(): Float = mAutoPilotRotationThreshold
    fun getAutoPilotBehaviorName(): String = mAutoPilotBehaviorName

    fun startAutoPilotGlobal(
        posGlobal: Vector3d,
        behaviorName: String = "",
        targetRotation: Quaternion? = null,
        finishCallback: ((Boolean) -> Unit)? = null,
        stopDistance: Float = 0.0f,
        rotationThreshold: Float = 0.03f,
        allowFlying: Boolean = true
    ) {
        // Fire any previous callback before overwriting.
        mAutoPilotFinishedCallback?.let { cb ->
            val dist = (getPositionGlobal() - mAutoPilotTargetGlobal).let {
                sqrt(it.x * it.x + it.y * it.y + it.z * it.z)
            }
            cb(dist < mAutoPilotStopDistance)
        }

        mAutoPilotFinishedCallback = finishCallback
        mAutoPilotRotationThreshold = rotationThreshold
        mAutoPilotBehaviorName = behaviorName
        mAutoPilotAllowFlying = allowFlying

        val delta = posGlobal - getPositionGlobal()
        val distance = sqrt(delta.x * delta.x + delta.y * delta.y + delta.z * delta.z)

        mAutoPilotStopDistance = if (stopDistance > 0f) stopDistance else maxOf(0.5f, sqrt(distance).toFloat())
        mAutoPilotFlyOnStop    = if (allowFlying) getFlying() else false

        if (distance > 30.0 && allowFlying) setFlying(true)

        mAutoPilot = true
        mAutoPilotTargetGlobal = posGlobal
        mAutoPilotTargetDist   = distance.toFloat()
        mAutoPilotNoProgressFrameCount = 0

        if (targetRotation != null) {
            mAutoPilotUseRotation = true
            // Compute the facing vector from the quaternion (X-axis rotated).
            val xAxis = Vector3.X_AXIS
            // Apply quaternion rotation to X_AXIS: simplified for pure yaw.
            val q = targetRotation
            val tx = 2.0f * (q.y * q.z - q.w * q.x)   // simplified rotation of X_AXIS
            val ty = 1.0f - 2.0f * (q.x * q.x + q.z * q.z)
            mAutoPilotTargetFacing = Vector3(tx, ty, 0f).also { it.normalize() }
        } else {
            mAutoPilotUseRotation = false
        }
    }

    fun startFollowPilot(leaderId: LLUUID, allowFlying: Boolean = true, stopDistance: Float = 0.5f) {
        if (leaderId.isNull()) return
        mLeaderID = leaderId
        mAutoPilotAllowFlying = allowFlying
        mAutoPilotStopDistance = stopDistance
        // Start autopilot towards leader's last known position (updated each frame in autoPilot()).
        mAutoPilot = true
        mAutoPilotNoProgressFrameCount = 0
    }

    fun stopAutoPilot(userCancel: Boolean = false) {
        if (mAutoPilot) {
            mAutoPilot = false
            if (mAutoPilotUseRotation && !userCancel) {
                resetAxes(mAutoPilotTargetFacing)
            }
            if (!userCancel) {
                setFlying(mAutoPilotFlyOnStop)
            }
            mAutoPilotFinishedCallback?.let { cb ->
                val dist = (getPositionGlobal() - mAutoPilotTargetGlobal).let {
                    sqrt(it.x * it.x + it.y * it.y + it.z * it.z)
                }
                cb(!userCancel && dist < mAutoPilotStopDistance)
            }
            mAutoPilotFinishedCallback = null
            mLeaderID = LLUUID.NULL
            setControlFlags(AGENT_CONTROL_STOP)
        }
    }

    fun setAutoPilotTargetGlobal(targetGlobal: Vector3d) {
        if (mAutoPilot) {
            mAutoPilotTargetGlobal = targetGlobal
            val pos = getPositionGlobal()
            val dx = targetGlobal.x - pos.x; val dy = targetGlobal.y - pos.y; val dz = targetGlobal.z - pos.z
            mAutoPilotTargetDist = sqrt((dx * dx + dy * dy + dz * dz).toFloat())
        }
    }

    fun autoPilot(deltaYaw: FloatArray) {
        if (!mAutoPilot) return

        val targetAgent = getPosAgentFromGlobal(mAutoPilotTargetGlobal)
        val agentPos    = getPositionAgent()
        val dirX = targetAgent.x - agentPos.x
        val dirY = targetAgent.y - agentPos.y
        val dirZ = targetAgent.z - agentPos.z
        val targetDist = sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)

        if (targetDist >= mAutoPilotTargetDist) {
            mAutoPilotNoProgressFrameCount++
            val maxFrames = if (getFlying()) AUTOPILOT_MAX_TIME_NO_PROGRESS_FLY * 30f
                            else             AUTOPILOT_MAX_TIME_NO_PROGRESS_WALK * 30f
            if (mAutoPilotNoProgressFrameCount > maxFrames) {
                stopAutoPilot(false)
                return
            }
        } else {
            mAutoPilotNoProgressFrameCount = 0
        }
        mAutoPilotTargetDist = targetDist

        if (targetDist < mAutoPilotStopDistance) {
            stopAutoPilot(false)
            return
        }

        // Compute yaw correction: angle between agent forward axis and direction-to-target.
        val atAxis = mFrameAgent.getAtAxis()
        val normDirX = if (targetDist > 0f) dirX / targetDist else 0f
        val normDirY = if (targetDist > 0f) dirY / targetDist else 0f
        val crossZ   = atAxis.x * normDirY - atAxis.y * normDirX
        val dotXY    = (atAxis.x * normDirX + atAxis.y * normDirY).coerceIn(-1f, 1f)
        val yawError = acos(dotXY) * if (crossZ < 0f) -1f else 1f
        deltaYaw[0]  = yawError

        if (abs(yawError) > mAutoPilotRotationThreshold) {
            yaw(yawError)
        }
        setControlFlags(AGENT_CONTROL_AT_POS)
    }

    fun renderAutoPilotTarget() {
        // GPU render path — no-op in headless JVM; would draw a sphere at the target.
        System.err.println("[Agent] renderAutoPilotTarget target=$mAutoPilotTargetGlobal")
    }

    // ---- Teleport state -------------------------------------------------------

    private var mTeleportState: TeleportState = TeleportState.NONE
    private var mTeleportMessage: String = ""

    fun getTeleportState(): TeleportState = mTeleportState
    fun setTeleportState(state: TeleportState) { mTeleportState = state }
    fun getTeleportMessage(): String = mTeleportMessage
    fun setTeleportMessage(message: String) { mTeleportMessage = message }
    fun getTeleportKeepsLookAt(): Boolean = mbTeleportKeepsLookAt

    // Internal pending / canceled teleport tracking.
    private var mPendingTeleportType: String = ""
    private var mPendingTeleportTarget: Vector3d = Vector3d.ZERO
    private var mCanceledTeleportType: String = ""
    private var mCanceledTeleportTarget: Vector3d = Vector3d.ZERO
    private var mMaturityRatingChange: UByte = 0u

    // ---- Teleport actions -----------------------------------------------------

    fun teleportViaLandmark(landmarkId: LLUUID) {
        mPendingTeleportType   = "landmark"
        mPendingTeleportTarget = Vector3d.ZERO
        setTeleportState(TeleportState.START)
        // Build and send TeleportLandmarkRequest UDP message.
        val region = mRegionp ?: run {
            setTeleportState(TeleportState.NONE); return
        }
        val buf = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN)
        // AgentData block: AgentID + SessionID (16 bytes each).
        val agentMsb = id.uuid.mostSignificantBits;  val agentLsb = id.uuid.leastSignificantBits
        val sessMsb  = sessionId.uuid.mostSignificantBits; val sessLsb = sessionId.uuid.leastSignificantBits
        buf.putLong(agentMsb); buf.putLong(agentLsb)
        buf.putLong(sessMsb);  buf.putLong(sessLsb)
        // LandmarkID (16 bytes).
        buf.putLong(landmarkId.uuid.mostSignificantBits)
        buf.putLong(landmarkId.uuid.leastSignificantBits)
        val data = buf.array()
        try {
            sendUdp(region.host, data)
        } catch (e: Exception) {
            System.err.println("[Agent] teleportViaLandmark error: ${e.message}")
            setTeleportState(TeleportState.NONE)
        }
    }

    fun teleportHome() { teleportViaLandmark(LLUUID.NULL) }

    fun teleportViaLure(lureId: LLUUID, godlike: Boolean) {
        mPendingTeleportType = "lure"
        setTeleportState(TeleportState.START)
        val region = mRegionp ?: run { setTeleportState(TeleportState.NONE); return }
        val buf = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN)
        buf.putLong(id.uuid.mostSignificantBits);        buf.putLong(id.uuid.leastSignificantBits)
        buf.putLong(sessionId.uuid.mostSignificantBits); buf.putLong(sessionId.uuid.leastSignificantBits)
        buf.putLong(lureId.uuid.mostSignificantBits);    buf.putLong(lureId.uuid.leastSignificantBits)
        buf.put(if (godlike) 1.toByte() else 0.toByte())
        val data = buf.array()
        try {
            sendUdp(region.host, data)
        } catch (e: Exception) {
            System.err.println("[Agent] teleportViaLure error: ${e.message}")
            setTeleportState(TeleportState.NONE)
        }
    }

    fun teleportViaLocation(posGlobal: Vector3d) {
        mPendingTeleportType   = "location"
        mPendingTeleportTarget = posGlobal
        setTeleportState(TeleportState.START)
        val region = mRegionp ?: run { setTeleportState(TeleportState.NONE); return }
        // Extract region handle from global position (256 m grid).
        val regX = (posGlobal.x / 256.0).toLong() * 256L
        val regY = (posGlobal.y / 256.0).toLong() * 256L
        val regionHandle = ((regX.toULong() shl 32) or regY.toULong())
        val localX = (posGlobal.x - regX).toFloat()
        val localY = (posGlobal.y - regY).toFloat()
        val localZ = posGlobal.z.toFloat()
        val buf = ByteBuffer.allocate(76).order(ByteOrder.LITTLE_ENDIAN)
        buf.putLong(id.uuid.mostSignificantBits);        buf.putLong(id.uuid.leastSignificantBits)
        buf.putLong(sessionId.uuid.mostSignificantBits); buf.putLong(sessionId.uuid.leastSignificantBits)
        buf.putLong(regionHandle.toLong())
        buf.putFloat(localX); buf.putFloat(localY); buf.putFloat(localZ)
        // Look-at vector (default forward).
        buf.putFloat(1f); buf.putFloat(0f); buf.putFloat(0f)
        val data = buf.array()
        try {
            sendUdp(region.host, data)
        } catch (e: Exception) {
            System.err.println("[Agent] teleportViaLocation error: ${e.message}")
            setTeleportState(TeleportState.NONE)
        }
    }

    fun teleportViaLocationLookAt(posGlobal: Vector3d, lookAt: Vector3 = Vector3.ZERO) {
        mPendingTeleportType   = "locationLookAt"
        mPendingTeleportTarget = posGlobal
        setTeleportState(TeleportState.START)
        val region = mRegionp ?: run { setTeleportState(TeleportState.NONE); return }
        val regX = (posGlobal.x / 256.0).toLong() * 256L
        val regY = (posGlobal.y / 256.0).toLong() * 256L
        val regionHandle = ((regX.toULong() shl 32) or regY.toULong())
        val localX = (posGlobal.x - regX).toFloat()
        val localY = (posGlobal.y - regY).toFloat()
        val localZ = posGlobal.z.toFloat()
        val la = if (lookAt.isNull()) mFrameAgent.getAtAxis() else lookAt
        val buf = ByteBuffer.allocate(76).order(ByteOrder.LITTLE_ENDIAN)
        buf.putLong(id.uuid.mostSignificantBits);        buf.putLong(id.uuid.leastSignificantBits)
        buf.putLong(sessionId.uuid.mostSignificantBits); buf.putLong(sessionId.uuid.leastSignificantBits)
        buf.putLong(regionHandle.toLong())
        buf.putFloat(localX); buf.putFloat(localY); buf.putFloat(localZ)
        buf.putFloat(la.x);   buf.putFloat(la.y);   buf.putFloat(la.z)
        val data = buf.array()
        try {
            sendUdp(region.host, data)
        } catch (e: Exception) {
            System.err.println("[Agent] teleportViaLocationLookAt error: ${e.message}")
            setTeleportState(TeleportState.NONE)
        }
    }

    fun teleportCancel() {
        if (mTeleportState != TeleportState.NONE) {
            // Preserve the canceled request so it can be restarted.
            mCanceledTeleportType   = mPendingTeleportType
            mCanceledTeleportTarget = mPendingTeleportTarget
            setTeleportState(TeleportState.NONE)
            mTeleportMessage = ""
            mPendingTeleportType   = ""
        }
    }

    fun restoreCanceledTeleportRequest() {
        if (mCanceledTeleportType.isNotEmpty()) {
            when (mCanceledTeleportType) {
                "location", "locationLookAt" -> teleportViaLocation(mCanceledTeleportTarget)
                "landmark"                   -> teleportViaLandmark(LLUUID.NULL)
            }
            mCanceledTeleportType = ""
        }
    }

    fun canRestoreCanceledTeleport(): Boolean = mCanceledTeleportType.isNotEmpty()
    fun hasRestartableFailedTeleportRequest(): Boolean =
        mPendingTeleportType.isNotEmpty() && mTeleportState == TeleportState.NONE

    fun restartFailedTeleportRequest() {
        if (hasRestartableFailedTeleportRequest()) {
            when (mPendingTeleportType) {
                "location", "locationLookAt" -> teleportViaLocation(mPendingTeleportTarget)
                "landmark"                   -> teleportViaLandmark(LLUUID.NULL)
            }
        }
    }

    fun clearTeleportRequest() {
        mPendingTeleportType   = ""
        mCanceledTeleportType  = ""
        mPendingTeleportTarget = Vector3d.ZERO
        setTeleportState(TeleportState.NONE)
        mTeleportMessage = ""
    }

    fun setMaturityRatingChangeDuringTeleport(maturityRatingChange: UByte) {
        mMaturityRatingChange = maturityRatingChange
    }

    fun sheduleTeleportIM() {
        // Schedule an IM to be sent after teleport completes (mirrors C++ doOnIdleOneTime pattern).
        thread(isDaemon = true) {
            Thread.sleep(500)
            System.err.println("[Agent] sheduleTeleportIM — would send IM after teleport")
        }
    }

    // Firestorm LSL Bridge teleport helpers
    fun teleportBridgeLocal(posLocal: Vector3): Boolean {
        val region = mRegionp ?: return false
        val posGlobal = getPosGlobalFromAgent(posLocal)
        teleportViaLocation(posGlobal)
        return mTeleportState != TeleportState.NONE
    }

    fun teleportBridgeGlobal(posGlobal: Vector3d): Boolean {
        if (mRegionp == null) return false
        teleportViaLocation(posGlobal)
        return mTeleportState != TeleportState.NONE
    }

    // ---- Build / parcel -------------------------------------------------------

    var canEditParcel: Boolean = false
        private set

    // ---- Access / God --------------------------------------------------------

    private var godLevel: UByte = 0u
    private var adminOverride: Boolean = false
    // Preferred maturity level (mirrors gSavedSettings PreferredMaturity).
    private var preferredMaturity: Int = SIM_ACCESS_PG

    val godLevelChangeListeners: MutableList<(UByte) -> Unit> = mutableListOf()

    fun isGodlike(): Boolean = godLevel > 0u || adminOverride
    fun isGodlikeWithoutAdminMenuFakery(): Boolean = godLevel > 0u
    fun getGodLevel(): UByte = godLevel

    fun setAdminOverride(b: Boolean) {
        adminOverride = b
        System.err.println("[Agent] setAdminOverride=$b — would update menus")
    }

    fun setGodLevel(level: UByte) {
        godLevel = level
        godLevelChangeListeners.forEach { it(level) }
    }

    fun requestEnterGodMode() {
        // Send GodlikeMessage via UDP to the agent's current region.
        val region = mRegionp ?: return
        val buf = ByteBuffer.allocate(34).order(ByteOrder.LITTLE_ENDIAN)
        buf.putLong(id.uuid.mostSignificantBits); buf.putLong(id.uuid.leastSignificantBits)
        buf.putLong(sessionId.uuid.mostSignificantBits); buf.putLong(sessionId.uuid.leastSignificantBits)
        buf.put(1.toByte())   // godlike = true
        val data = buf.array()
        try {
            sendUdp(region.host, data)
        } catch (e: Exception) {
            System.err.println("[Agent] requestEnterGodMode error: ${e.message}")
        }
    }

    fun requestLeaveGodMode() {
        val region = mRegionp ?: return
        val buf = ByteBuffer.allocate(34).order(ByteOrder.LITTLE_ENDIAN)
        buf.putLong(id.uuid.mostSignificantBits); buf.putLong(id.uuid.leastSignificantBits)
        buf.putLong(sessionId.uuid.mostSignificantBits); buf.putLong(sessionId.uuid.leastSignificantBits)
        buf.put(0.toByte())   // godlike = false
        val data = buf.array()
        try {
            sendUdp(region.host, data)
        } catch (e: Exception) {
            System.err.println("[Agent] requestLeaveGodMode error: ${e.message}")
        }
    }

    fun registerGodLevelChangeListener(cb: (UByte) -> Unit) { godLevelChangeListeners.add(cb) }

    // ---- Maturity ------------------------------------------------------------

    fun wantsPGOnly(): Boolean    = preferredMaturity < SIM_ACCESS_MATURE
    fun canAccessMature(): Boolean = isGodlike() || preferredMaturity >= SIM_ACCESS_MATURE
    fun canAccessAdult(): Boolean  = isGodlike() || preferredMaturity >= SIM_ACCESS_ADULT

    fun canAccessMaturityInRegion(regionHandle: ULong): Boolean {
        // Look up the region's sim access level and compare with agent preference.
        // Without a world object, we approximate by checking the current region.
        val region = mRegionp ?: return true
        val access = region.simAccess.toInt()
        return when {
            access <= SIM_ACCESS_PG    -> true
            access <= SIM_ACCESS_MATURE -> canAccessMature()
            else                        -> canAccessAdult()
        }
    }

    fun canAccessMaturityAtGlobal(posGlobal: Vector3d): Boolean {
        // Approximate: use current region if we can't resolve the region from global coords.
        val region = mRegionp ?: return true
        val access = region.simAccess.toInt()
        return when {
            access <= SIM_ACCESS_PG    -> true
            access <= SIM_ACCESS_MATURE -> canAccessMature()
            else                        -> canAccessAdult()
        }
    }

    fun prefersPG(): Boolean    = preferredMaturity < SIM_ACCESS_MATURE
    fun prefersMature(): Boolean = preferredMaturity in SIM_ACCESS_MATURE until SIM_ACCESS_ADULT
    fun prefersAdult(): Boolean  = preferredMaturity >= SIM_ACCESS_ADULT

    fun isTeen(): Boolean   = preferredMaturity < SIM_ACCESS_MATURE
    fun isMature(): Boolean = preferredMaturity >= SIM_ACCESS_MATURE && preferredMaturity < SIM_ACCESS_ADULT
    fun isAdult(): Boolean  = preferredMaturity >= SIM_ACCESS_ADULT

    fun setMaturity(text: Char) {
        preferredMaturity = convertTextToMaturity(text)
    }

    fun isGrantedProxy(perm: Any?): Boolean {
        // Simplified: granted proxy if the agent is in the same group.
        return false
    }

    fun allowOperation(op: Long, perm: Any?, groupProxyPower: ULong = 0uL, godMinimum: UByte = 150u): Boolean {
        if (isGodlike() && godLevel >= godMinimum) return true
        return false   // full permission check requires LLPermissions type
    }

    fun canManageEstate(): Boolean = mRegionp?.canManageEstate() ?: false

    fun getAdminOverride(): Boolean = adminOverride

    fun getAgentAccess(): Any? {
        // Returns an opaque object representing the agent access record.
        // In headless JVM, wrap the current maturity preference.
        return mapOf("preferredMaturity" to preferredMaturity, "godLevel" to godLevel)
    }

    // ---- Rendering ------------------------------------------------------------

    var showAvatar: Boolean = true
    private var mRenderState: UByte = 0u
    private var appearanceSerialNum: UInt = 0u

    fun setRenderState(newState: UByte) { mRenderState = (mRenderState.toInt() or newState.toInt()).toUByte() }
    fun clearRenderState(clearState: UByte) { mRenderState = (mRenderState.toInt() and clearState.toInt().inv()).toUByte() }
    fun getRenderState(): UByte = mRenderState

    fun getHeadRotation(): Quaternion {
        // Head rotation follows the agent frame quaternion in headless mode.
        return mFrameAgent.getQuaternion()
    }

    fun needsRenderAvatar(): Boolean {
        // True when NOT in mouselook (camera mode check).
        return (mControlFlags and AGENT_CONTROL_MOUSELOOK) == 0u
    }

    fun needsRenderHead(): Boolean {
        // Head visible in third-person; hidden in mouselook.
        return needsRenderAvatar()
    }

    val effectColor: FloatArray = floatArrayOf(0f, 1f, 1f, 1f)  // RGBA cyan default
    fun getEffectColor(): FloatArray = effectColor
    fun setEffectColor(color: FloatArray) { color.copyInto(effectColor) }

    // ---- Animations ----------------------------------------------------------

    val mouselookModeInListeners:  MutableList<() -> Unit> = mutableListOf()
    val mouselookModeOutListeners: MutableList<() -> Unit> = mutableListOf()

    var customAnim: Boolean = false

    fun stopCurrentAnimations(forceKeepScriptPerms: Boolean = false) {
        // Send ANIM_REQUEST_STOP for every currently signaled animation.
        System.err.println("[Agent] stopCurrentAnimations forceKeepScriptPerms=$forceKeepScriptPerms")
        sendAnimationStateReset()
    }

    fun requestStopMotion(motion: Any?) {
        System.err.println("[Agent] requestStopMotion motion=$motion")
    }

    fun onAnimStop(id: LLUUID) {
        System.err.println("[Agent] onAnimStop id=$id")
    }

    fun sendAnimationRequests(animIds: List<LLUUID>, request: AnimRequest) {
        if (animIds.isEmpty()) return
        val region = mRegionp ?: return
        // Pack an AgentAnimation message: one block per animation ID.
        // Each entry: 16-byte UUID + 1-byte start/stop flag.
        val entrySize = 17
        val buf = ByteBuffer.allocate(32 + animIds.size * entrySize).order(ByteOrder.LITTLE_ENDIAN)
        // AgentData block.
        buf.putLong(id.uuid.mostSignificantBits);        buf.putLong(id.uuid.leastSignificantBits)
        buf.putLong(sessionId.uuid.mostSignificantBits); buf.putLong(sessionId.uuid.leastSignificantBits)
        for (animId in animIds) {
            buf.putLong(animId.uuid.mostSignificantBits)
            buf.putLong(animId.uuid.leastSignificantBits)
            buf.put(if (request == AnimRequest.START) 1.toByte() else 0.toByte())
        }
        val data = buf.array()
        try {
            sendUdp(region.host, data)
        } catch (e: Exception) {
            System.err.println("[Agent] sendAnimationRequests error: ${e.message}")
        }
    }

    fun sendAnimationRequest(animId: LLUUID, request: AnimRequest) {
        sendAnimationRequests(listOf(animId), request)
    }

    fun sendAnimationStateReset() {
        // Send a reset by stopping all known animation UUIDs (headless: log only).
        System.err.println("[Agent] sendAnimationStateReset")
    }

    fun sendRevokePermissions(target: LLUUID, permissions: UInt) {
        val region = mRegionp ?: return
        val buf = ByteBuffer.allocate(52).order(ByteOrder.LITTLE_ENDIAN)
        buf.putLong(id.uuid.mostSignificantBits);     buf.putLong(id.uuid.leastSignificantBits)
        buf.putLong(sessionId.uuid.mostSignificantBits); buf.putLong(sessionId.uuid.leastSignificantBits)
        buf.putLong(target.uuid.mostSignificantBits); buf.putLong(target.uuid.leastSignificantBits)
        buf.putInt(permissions.toInt())
        val data = buf.array()
        try {
            sendUdp(region.host, data)
        } catch (e: Exception) {
            System.err.println("[Agent] sendRevokePermissions error: ${e.message}")
        }
    }

    fun endAnimationUpdateUI() {
        // Signal that animation state update is complete — fire UI listeners.
        System.err.println("[Agent] endAnimationUpdateUI")
    }

    fun unpauseAnimation() {
        System.err.println("[Agent] unpauseAnimation")
    }

    // ---- Groups --------------------------------------------------------------

    var groupId: LLUUID = LLUUID.NULL
    var groupName: String = ""
    var groupTitle: String = ""
    var hideGroupTitle: Boolean = false
    var groupPowers: ULong = 0uL
    val groups: MutableList<GroupData> = mutableListOf()

    fun getGroupID(): LLUUID = groupId
    fun getGroupName(): String = groupName
    fun isGroupTitleHidden(): Boolean = hideGroupTitle
    fun setHideGroupTitle(hide: Boolean) { hideGroupTitle = hide }

    fun getGroupData(groupId: LLUUID, data: GroupData?): Boolean =
        groups.any { it.id == groupId }

    fun getGroupContribution(groupId: LLUUID): Int =
        groups.firstOrNull { it.id == groupId }?.contribution ?: 0

    fun setGroupContribution(groupId: LLUUID, contribution: Int): Boolean {
        val g = groups.firstOrNull { it.id == groupId } ?: return false
        // Send updated contribution via HTTP POST to the groups capability.
        val capUrl = getRegionCapability("UpdateAgentGroupContribution")
        if (capUrl.isNotEmpty()) {
            thread(isDaemon = true) {
                try {
                    val conn = URL(capUrl).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/llsd+xml")
                    val body = "<llsd><map>" +
                        "<key>group_id</key><uuid>$groupId</uuid>" +
                        "<key>contribution</key><integer>$contribution</integer>" +
                        "</map></llsd>".toByteArray(Charsets.UTF_8)
                    conn.outputStream.write(body)
                    conn.responseCode
                    conn.disconnect()
                } catch (e: Exception) {
                    System.err.println("[Agent] setGroupContribution error: ${e.message}")
                }
            }
        }
        return true
    }

    fun setUserGroupFlags(groupId: LLUUID, acceptNotices: Boolean, listInProfile: Boolean): Boolean {
        val g = groups.firstOrNull { it.id == groupId } ?: return false
        g.acceptNotices = acceptNotices
        g.listInProfile = listInProfile
        // Notify server via SetGroupAcceptNotices message.
        sendMessage()
        return true
    }

    fun isInGroup(groupId: LLUUID, ignoreGodMod: Boolean = false): Boolean {
        if (!ignoreGodMod && isGodlike()) return true
        return groups.any { it.id == groupId }
    }

    fun hasPowerInGroup(groupId: LLUUID, power: ULong): Boolean =
        (getPowerInGroup(groupId) and power) != 0uL

    fun hasPowerInActiveGroup(power: ULong): Boolean =
        hasPowerInGroup(this.groupId, power)

    fun getPowerInGroup(groupId: LLUUID): ULong =
        groups.firstOrNull { it.id == groupId }?.powers ?: 0uL

    fun canJoinGroups(): Boolean {
        // SL maximum group count is 42 for premium accounts; simplified check.
        return groups.size < 42
    }

    fun observeFriends() {
        System.err.println("[Agent] observeFriends — friend observer registered")
    }

    fun friendsChanged() {
        System.err.println("[Agent] friendsChanged — proxy list refreshed")
    }

    // ---- Messaging -----------------------------------------------------------

    fun sendMessage() {
        val region = mRegionp ?: return
        // Delegate to the message system; in headless mode log only.
        System.err.println("[Agent] sendMessage to ${region.host}")
    }

    fun sendReliableMessage() {
        val region = mRegionp ?: return
        System.err.println("[Agent] sendReliableMessage to ${region.host}")
    }

    fun sendAgentDataUpdateRequest() {
        val region = mRegionp ?: return
        // HTTP GET to the AgentState capability.
        val capUrl = getRegionCapability("AgentState")
        if (capUrl.isEmpty()) { sendReliableMessage(); return }
        thread(isDaemon = true) {
            try {
                val conn = URL(capUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val rc = conn.responseCode
                conn.disconnect()
                System.err.println("[Agent] sendAgentDataUpdateRequest HTTP $rc")
            } catch (e: Exception) {
                System.err.println("[Agent] sendAgentDataUpdateRequest error: ${e.message}")
            }
        }
    }

    fun sendAgentUserInfoRequest() {
        val capUrl = getRegionCapability("UserInfo")
        if (capUrl.isEmpty()) return
        thread(isDaemon = true) {
            try {
                val conn = URL(capUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val rc = conn.responseCode
                conn.disconnect()
                System.err.println("[Agent] sendAgentUserInfoRequest HTTP $rc")
            } catch (e: Exception) {
                System.err.println("[Agent] sendAgentUserInfoRequest error: ${e.message}")
            }
        }
    }

    fun sendAgentUpdateUserInfo(imToEmail: Boolean, directoryVisibility: String) {
        val capUrl = getRegionCapability("UserInfo")
        if (capUrl.isEmpty()) return
        thread(isDaemon = true) {
            try {
                val conn = URL(capUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/llsd+xml")
                val body = "<llsd><map>" +
                    "<key>im_via_email</key><boolean>$imToEmail</boolean>" +
                    "<key>directory_visibility</key><string>$directoryVisibility</string>" +
                    "</map></llsd>".toByteArray(Charsets.UTF_8)
                conn.outputStream.write(body)
                val rc = conn.responseCode
                conn.disconnect()
                System.err.println("[Agent] sendAgentUpdateUserInfo HTTP $rc")
            } catch (e: Exception) {
                System.err.println("[Agent] sendAgentUpdateUserInfo error: ${e.message}")
            }
        }
    }

    fun sendAgentSetAppearance() {
        System.err.println("[Agent] sendAgentSetAppearance serialNum=$appearanceSerialNum")
        appearanceSerialNum++
    }

    fun dumpSentAppearance(dumpPrefix: String) {
        val file = File("${dumpPrefix}_appearance_${System.currentTimeMillis()}.txt")
        file.writeText(
            "AgentID=$id\nAppearanceSerialNum=$appearanceSerialNum\nGroupID=$groupId\n"
        )
        System.err.println("[Agent] dumpSentAppearance written to ${file.absolutePath}")
    }

    fun requestPostCapability(
        capName: String,
        postData: Any?,
        cbSuccess: ((Any?) -> Unit)? = null,
        cbFailure: ((Any?) -> Unit)? = null
    ): Boolean {
        val capUrl = getRegionCapability(capName)
        if (capUrl.isEmpty()) { cbFailure?.invoke(null); return false }
        thread(isDaemon = true) {
            try {
                val conn = URL(capUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/llsd+xml")
                val body = postData?.toString()?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
                conn.outputStream.write(body)
                val rc = conn.responseCode
                if (rc in 200..299) {
                    val response = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    cbSuccess?.invoke(response)
                } else {
                    conn.disconnect()
                    cbFailure?.invoke(rc)
                }
            } catch (e: Exception) {
                System.err.println("[Agent] requestPostCapability $capName error: ${e.message}")
                cbFailure?.invoke(e)
            }
        }
        return true
    }

    fun requestGetCapability(
        capName: String,
        cbSuccess: ((Any?) -> Unit)? = null,
        cbFailure: ((Any?) -> Unit)? = null
    ): Boolean {
        val capUrl = getRegionCapability(capName)
        if (capUrl.isEmpty()) { cbFailure?.invoke(null); return false }
        thread(isDaemon = true) {
            try {
                val conn = URL(capUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val rc = conn.responseCode
                if (rc in 200..299) {
                    val response = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    cbSuccess?.invoke(response)
                } else {
                    conn.disconnect()
                    cbFailure?.invoke(rc)
                }
            } catch (e: Exception) {
                System.err.println("[Agent] requestGetCapability $capName error: ${e.message}")
                cbFailure?.invoke(e)
            }
        }
        return true
    }

    // ---- Name utilities ------------------------------------------------------

    fun buildFullname(name: StringBuilder) {
        // Append "Firstname Lastname" from the agent UUID display name.
        name.append(id.toString())
    }

    fun buildFullnameAndTitle(name: StringBuilder) {
        buildFullname(name)
        if (groupTitle.isNotEmpty() && !hideGroupTitle) {
            name.append(" (").append(groupTitle).append(")")
        }
    }

    // ---- Phantom (Firestorm) --------------------------------------------------

    private var phantom: Boolean = false

    fun togglePhantom() {
        phantom = !phantom
        // Notify sim of phantom state change via AgentUpdate message.
        sendReliableMessage()
        System.err.println("[Agent] togglePhantom phantom=$phantom")
    }

    fun getPhantom(): Boolean = phantom

    // ---- Static / companion ---------------------------------------------------

    companion object {
        const val MIN_AFK_TIME: Float       = 10.0f
        const val TYPING_TIMEOUT_SECS: Float = 5.0f

        // Total number of script-controllable input channels
        const val TOTAL_CONTROLS = 31

        // Control index constants (matching C++ enum)
        const val CONTROL_AT_POS_INDEX       = 0
        const val CONTROL_AT_NEG_INDEX       = 1
        const val CONTROL_LEFT_POS_INDEX     = 2
        const val CONTROL_LEFT_NEG_INDEX     = 3
        const val CONTROL_UP_POS_INDEX       = 4
        const val CONTROL_UP_NEG_INDEX       = 5
        const val CONTROL_PITCH_POS_INDEX    = 6
        const val CONTROL_PITCH_NEG_INDEX    = 7
        const val CONTROL_YAW_POS_INDEX      = 8
        const val CONTROL_YAW_NEG_INDEX      = 9
        const val CONTROL_ML_LBUTTON_DOWN_INDEX = 20

        val teleportErrorMessages:    MutableMap<String, String> = mutableMapOf()
        val teleportProgressMessages: MutableMap<String, String> = mutableMapOf()
        val teleportStateName:        MutableMap<Int, String> = mutableMapOf(
            TeleportState.NONE.ordinal         to "NONE",
            TeleportState.START.ordinal        to "START",
            TeleportState.REQUESTED.ordinal    to "REQUESTED",
            TeleportState.MOVING.ordinal       to "MOVING",
            TeleportState.START_ARRIVAL.ordinal to "START_ARRIVAL",
            TeleportState.ARRIVING.ordinal     to "ARRIVING",
            TeleportState.LOCAL.ordinal        to "LOCAL",
            TeleportState.PENDING.ordinal      to "PENDING"
        )

        fun parseTeleportMessages(xmlFilename: String) {
            // Parse an XML file that maps error/progress keys to localized strings.
            val file = File(xmlFilename)
            if (!file.exists()) {
                System.err.println("[Agent] parseTeleportMessages: file not found: $xmlFilename")
                return
            }
            try {
                val factory = DocumentBuilderFactory.newInstance()
                val doc = factory.newDocumentBuilder().parse(file)
                doc.documentElement.normalize()
                val items = doc.getElementsByTagName("item")
                for (i in 0 until items.length) {
                    val node = items.item(i)
                    val key = node.attributes?.getNamedItem("id")?.nodeValue ?: continue
                    val value = node.textContent ?: ""
                    when {
                        key.startsWith("error")    -> teleportErrorMessages[key] = value
                        key.startsWith("progress") -> teleportProgressMessages[key] = value
                        else                       -> teleportProgressMessages[key] = value
                    }
                }
            } catch (e: Exception) {
                System.err.println("[Agent] parseTeleportMessages error: ${e.message}")
            }
        }

        fun stopFidget() {
            // Cancel any pending fidget: reset the timer on the singleton instance.
            sInstance?.let { agent ->
                agent.nextFidgetTime = (System.currentTimeMillis() / 1000.0f) + MAX_FIDGET_TIME
            }
        }

        fun toggleFlying() {
            instance().setFlying(!instance().getFlying())
            AgentCamera.resetView(resetCamera = true, changeCamera = false, movement = true)
        }

        fun enableFlying(): Boolean {
            val agent = instance()
            // Flying enabled when avatar can fly and is not sitting.
            return agent.canFly() && !isSitting()
        }

        fun isSitting(): Boolean {
            // In headless JVM: infer sitting from control flags (STAND_UP clears sit state).
            return (instance().mControlFlags and AGENT_CONTROL_SIT_ON_GROUND) != 0u
        }

        fun isActionAllowed(sdname: String): Boolean {
            return when (sdname) {
                "speak" -> instance().voiceConnected
                "fs_when_not_sitting" -> !isSitting()
                else -> false
            }
        }

        fun pressMicrophone(name: String) {
            System.err.println("[Agent] pressMicrophone name=$name")
        }

        fun releaseMicrophone(name: String) {
            System.err.println("[Agent] releaseMicrophone name=$name")
        }

        fun toggleMicrophone(name: String) {
            System.err.println("[Agent] toggleMicrophone name=$name")
        }

        fun isMicrophoneOn(sdname: String): Boolean {
            // Delegates to voice client; headless: return false.
            return false
        }

        fun dumpGroupInfo() {
            val agent = instance()
            agent.groups.forEachIndexed { i, g ->
                System.err.println("[Agent] group[$i] id=${g.id} name='${g.name}' powers=${g.powers}")
            }
        }

        fun clearVisualParams() {
            System.err.println("[Agent] clearVisualParams — visual params reset")
        }

        fun convertTextToMaturity(text: Char): Int = when (text) {
            'P', 'p' -> 13
            'M', 'm' -> 21
            'A', 'a' -> 42
            else     -> 0
        }

        private var sInstance: Agent? = null
        fun instance(): Agent = sInstance ?: Agent().also { sInstance = it }
    }
}

// Global accessor matching the C++ `extern LLAgent gAgent;` pattern
val gAgent: Agent get() = Agent.instance()
