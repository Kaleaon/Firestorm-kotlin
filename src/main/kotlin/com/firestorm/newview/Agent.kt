package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d
import com.firestorm.llmath.Quaternion

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
        TODO("APR: init — connect saved-settings signals, set flying from saved state")
    }

    fun cleanup() {
        mRegionp = null
        TODO("APR: cleanup — disconnect teleport slots")
    }

    fun onAppFocusGained() {}   // currently a no-op in the C++ source

    fun setFirstLogin(b: Boolean) {
        isFirstLogin = b
        if (b && getFeatureVersion() <= UI_FEATURE_VERSION) {
            setFeatureVersion(UI_FEATURE_VERSION, UI_FEATURE_FLAGS)
        }
    }

    fun setFeatureVersion(version: Int, flags: Int) {
        TODO("APR: setFeatureVersion — persist to saved settings")
    }

    fun getFeatureVersion(): Int = TODO("APR: getFeatureVersion")
    fun getFeatureVersionAndFlags(version: IntArray, flags: IntArray) { TODO("APR: getFeatureVersionAndFlags") }
    fun showLatestFeatureNotification(key: String) { TODO("APR: showLatestFeatureNotification key=$key") }

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
    private var mFrameAgent: Any? = null     // LLCoordFrame equivalent

    val positionChangedListeners: MutableList<(Vector3, Vector3d) -> Unit> = mutableListOf()

    fun getPositionGlobal(): Vector3d = mPositionGlobal
    fun getPositionAgent(): Vector3 = TODO("getPositionAgent: global - region origin")

    fun setPositionAgent(pos: Vector3) {
        TODO("APR: setPositionAgent — fire positionChanged signal if moved > threshold")
    }

    fun getPosAgentFromGlobal(posGlobal: Vector3d): Vector3 = TODO("getPosAgentFromGlobal")
    fun getPosGlobalFromAgent(posAgent: Vector3): Vector3d = TODO("getPosGlobalFromAgent")

    fun initOriginGlobal(originGlobal: Vector3d) { mAgentOriginGlobal = originGlobal }

    fun resetAxes() { TODO("APR: resetAxes") }
    fun resetAxes(lookAt: Vector3) { TODO("APR: resetAxes lookAt=$lookAt") }

    fun getAtAxis(): Vector3 = TODO("APR: getAtAxis from mFrameAgent")
    fun getUpAxis(): Vector3 = TODO("APR: getUpAxis from mFrameAgent")
    fun getLeftAxis(): Vector3 = TODO("APR: getLeftAxis from mFrameAgent")
    fun getQuat(): Quaternion = TODO("APR: getQuat from mFrameAgent")

    fun updateAgentPosition(dt: Float, yaw: Float, mouseX: Int, mouseY: Int) {
        TODO("APR: updateAgentPosition — propagate physics + camera update")
    }

    fun getVelocity(): Vector3 = TODO("APR: getVelocity from physics")
    fun getVelocityZ(): Float {
        TODO("APR: getVelocityZ")
    }

    // ---- Home ----------------------------------------------------------------

    private var haveHomePosition: Boolean = false
    private var homeRegionHandle: ULong = 0uL
    private var homePosRegion: Vector3 = Vector3.ZERO

    fun setStartPosition(locationId: UInt) { TODO("APR: setStartPosition locationId=$locationId") }
    fun setHomePosRegion(regionHandle: ULong, posRegion: Vector3) {
        homeRegionHandle = regionHandle
        homePosRegion = posRegion
        haveHomePosition = true
    }
    fun getHomePosGlobal(posGlobal: Vector3d): Boolean = TODO("getHomePosGlobal")
    fun isInHomeRegion(): Boolean = TODO("isInHomeRegion")

    // ---- Parcel / Region -----------------------------------------------------

    private var mRegionp: ViewerRegion? = null
    private var mInterestListMode: String = "default"

    val parcelChangedListeners: MutableList<() -> Unit> = mutableListOf()
    val regionChangedListeners: MutableList<() -> Unit> = mutableListOf()

    fun getRegion(): ViewerRegion? = mRegionp

    fun setRegion(regionp: ViewerRegion) {
        if (mRegionp != regionp) {
            mAgentOriginGlobal = regionp.originGlobal
            mRegionp = regionp
            regionChangedListeners.forEach { it() }
        }
        TODO("APR: setRegion — shift local coords, update sky, water objects")
    }

    fun getRegionHost(): Any? = TODO("APR: getRegionHost")
    fun inPrelude(): Boolean = TODO("APR: inPrelude — check if in introductory region")
    fun getRegionCapability(name: String): String = TODO("APR: getRegionCapability name=$name")
    fun changeInterestListMode(newMode: String) { mInterestListMode = newMode; TODO("APR: changeInterestListMode") }
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

    fun fidget() { TODO("APR: fidget — trigger random fidget animation") }

    // ---- Flying ---------------------------------------------------------------

    fun getFlying(): Boolean = (mControlFlags and AGENT_CONTROL_FLY) != 0u

    fun setFlying(fly: Boolean, failSound: Boolean = false) {
        if (fly) {
            if (!canFly() && !getFlying()) return
            setControlFlags(AGENT_CONTROL_FLY)
        } else {
            clearControlFlags(AGENT_CONTROL_FLY)
        }
        TODO("APR: setFlying — update FloaterMove flying mode indicator")
    }

    fun canFly(): Boolean {
        if (isGodlike()) return true
        if (mAlwaysFly) return true
        TODO("APR: canFly — check parcel/region fly permission")
    }

    // ---- Voice ----------------------------------------------------------------

    var voiceConnected: Boolean = false

    // ---- Chat / typing --------------------------------------------------------

    private var mLastChatterID: LLUUID = LLUUID.NULL
    private var mNearChatRadius: Float = 10.0f

    fun heardChat(id: LLUUID) { mLastChatterID = id; TODO("APR: heardChat") }
    fun getLastChatter(): LLUUID = mLastChatterID
    fun getNearChatRadius(): Float = mNearChatRadius
    fun getTypingTime(): Float = TODO("APR: getTypingTime from mTypingTimer")

    fun startTyping() {
        setRenderState(AGENT_STATE_TYPING)
        TODO("APR: startTyping — send agent update")
    }

    fun stopTyping() {
        clearRenderState(AGENT_STATE_TYPING)
        TODO("APR: stopTyping — send agent update")
    }

    // ---- AFK ------------------------------------------------------------------

    fun setAFK() {
        setControlFlags(AGENT_CONTROL_AWAY)
        TODO("APR: setAFK — update floater, send agent update")
    }

    fun clearAFK() {
        clearControlFlags(AGENT_CONTROL_AWAY)
        TODO("APR: clearAFK — send agent update")
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
    fun sendWalkRun()    { TODO("APR: sendWalkRun — send walk/run mode to sim") }
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

    fun setDoNotDisturb(dnd: Boolean) { isDoNotDisturb = dnd; TODO("APR: setDoNotDisturb — notify UI") }
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

    fun getControlFlags(): UInt = mControlFlags
    fun setControlFlags(mask: UInt) { mControlFlags = mControlFlags or mask }
    fun clearControlFlags(mask: UInt) { mControlFlags = mControlFlags and mask.inv() }
    fun controlFlagsDirty(): Boolean = TODO("controlFlagsDirty")
    fun resetControlFlags() { mControlFlags = 0u }
    fun anyControlGrabbed(): Boolean = mControlsTakenCount.any { it > 0 }
    fun isControlGrabbed(controlIndex: Int): Boolean = mControlsTakenCount.getOrElse(controlIndex) { 0 } > 0
    fun forceReleaseControls() { TODO("APR: forceReleaseControls — send message to simulator") }
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

    fun rotate(angle: Float, axis: Vector3) { TODO("APR: rotate angle=$angle axis=$axis") }
    fun rotate(angle: Float, x: Float, y: Float, z: Float) { rotate(angle, Vector3(x, y, z)) }
    fun rotate(quaternion: Quaternion) { TODO("APR: rotate quat=$quaternion") }
    fun pitch(angle: Float) { TODO("APR: pitch angle=$angle") }
    fun roll(angle: Float)  { TODO("APR: roll angle=$angle") }
    fun yaw(angle: Float)   { TODO("APR: yaw angle=$angle") }
    fun getReferenceUpVector(): Vector3 = TODO("APR: getReferenceUpVector")

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
        mAutoPilot = true
        mAutoPilotTargetGlobal = posGlobal
        mAutoPilotBehaviorName = behaviorName
        mAutoPilotStopDistance = if (stopDistance <= 0f) 1.0f else stopDistance
        mAutoPilotRotationThreshold = rotationThreshold
        mAutoPilotAllowFlying = allowFlying
        TODO("APR: startAutoPilotGlobal — begin pathfinding to target")
    }

    fun startFollowPilot(leaderId: LLUUID, allowFlying: Boolean = true, stopDistance: Float = 0.5f) {
        mLeaderID = leaderId
        mAutoPilotAllowFlying = allowFlying
        mAutoPilotStopDistance = stopDistance
        mAutoPilot = true
        TODO("APR: startFollowPilot")
    }

    fun stopAutoPilot(userCancel: Boolean = false) {
        mAutoPilot = false
        TODO("APR: stopAutoPilot — fire callback if needed")
    }

    fun setAutoPilotTargetGlobal(targetGlobal: Vector3d) { mAutoPilotTargetGlobal = targetGlobal }

    fun autoPilot(deltaYaw: FloatArray) { TODO("APR: autoPilot — walk/fly toward target") }
    fun renderAutoPilotTarget() { TODO("GPU: renderAutoPilotTarget") }

    // ---- Teleport state -------------------------------------------------------

    private var mTeleportState: TeleportState = TeleportState.NONE
    private var mTeleportMessage: String = ""

    fun getTeleportState(): TeleportState = mTeleportState
    fun setTeleportState(state: TeleportState) { mTeleportState = state }
    fun getTeleportMessage(): String = mTeleportMessage
    fun setTeleportMessage(message: String) { mTeleportMessage = message }
    fun getTeleportKeepsLookAt(): Boolean = mbTeleportKeepsLookAt

    // ---- Teleport actions -----------------------------------------------------

    fun teleportViaLandmark(landmarkId: LLUUID) {
        TODO("APR: teleportViaLandmark landmarkId=$landmarkId")
    }

    fun teleportHome() { teleportViaLandmark(LLUUID.NULL) }

    fun teleportViaLure(lureId: LLUUID, godlike: Boolean) {
        TODO("APR: teleportViaLure lureId=$lureId godlike=$godlike")
    }

    fun teleportViaLocation(posGlobal: Vector3d) {
        TODO("APR: teleportViaLocation posGlobal=$posGlobal")
    }

    fun teleportViaLocationLookAt(posGlobal: Vector3d, lookAt: Vector3 = Vector3.ZERO) {
        TODO("APR: teleportViaLocationLookAt posGlobal=$posGlobal lookAt=$lookAt")
    }

    fun teleportCancel() { TODO("APR: teleportCancel") }
    fun restoreCanceledTeleportRequest() { TODO("APR: restoreCanceledTeleportRequest") }
    fun canRestoreCanceledTeleport(): Boolean = TODO("canRestoreCanceledTeleport")
    fun hasRestartableFailedTeleportRequest(): Boolean = TODO("hasRestartableFailedTeleportRequest")
    fun restartFailedTeleportRequest() { TODO("APR: restartFailedTeleportRequest") }
    fun clearTeleportRequest() { TODO("APR: clearTeleportRequest") }
    fun setMaturityRatingChangeDuringTeleport(maturityRatingChange: UByte) { TODO("APR: setMaturityRatingChangeDuringTeleport") }
    fun sheduleTeleportIM() { TODO("APR: sheduleTeleportIM") }

    // Firestorm LSL Bridge teleport helpers
    fun teleportBridgeLocal(posLocal: Vector3): Boolean = TODO("APR: teleportBridgeLocal posLocal=$posLocal")
    fun teleportBridgeGlobal(posGlobal: Vector3d): Boolean = TODO("APR: teleportBridgeGlobal posGlobal=$posGlobal")

    // ---- Build / parcel -------------------------------------------------------

    var canEditParcel: Boolean = false
        private set

    // ---- Access / God --------------------------------------------------------

    private var godLevel: UByte = 0u
    private var adminOverride: Boolean = false

    val godLevelChangeListeners: MutableList<(UByte) -> Unit> = mutableListOf()

    fun isGodlike(): Boolean = godLevel > 0u || adminOverride
    fun isGodlikeWithoutAdminMenuFakery(): Boolean = godLevel > 0u
    fun getGodLevel(): UByte = godLevel
    fun setAdminOverride(b: Boolean) { adminOverride = b; TODO("APR: setAdminOverride — update menus") }
    fun setGodLevel(level: UByte) {
        godLevel = level
        godLevelChangeListeners.forEach { it(level) }
    }
    fun requestEnterGodMode()  { TODO("APR: requestEnterGodMode — send message") }
    fun requestLeaveGodMode()  { TODO("APR: requestLeaveGodMode — send message") }

    fun registerGodLevelChangeListener(cb: (UByte) -> Unit) { godLevelChangeListeners.add(cb) }

    // ---- Maturity ------------------------------------------------------------

    fun wantsPGOnly(): Boolean = TODO("wantsPGOnly")
    fun canAccessMature(): Boolean = TODO("canAccessMature")
    fun canAccessAdult(): Boolean = TODO("canAccessAdult")
    fun canAccessMaturityInRegion(regionHandle: ULong): Boolean = TODO("canAccessMaturityInRegion")
    fun canAccessMaturityAtGlobal(posGlobal: Vector3d): Boolean = TODO("canAccessMaturityAtGlobal")
    fun prefersPG(): Boolean = TODO("prefersPG")
    fun prefersMature(): Boolean = TODO("prefersMature")
    fun prefersAdult(): Boolean = TODO("prefersAdult")
    fun isTeen(): Boolean = TODO("isTeen")
    fun isMature(): Boolean = TODO("isMature")
    fun isAdult(): Boolean = TODO("isAdult")
    fun setMaturity(text: Char) { TODO("APR: setMaturity text=$text") }
    fun isGrantedProxy(perm: Any?): Boolean = TODO("isGrantedProxy")
    fun allowOperation(op: Long, perm: Any?, groupProxyPower: ULong = 0uL, godMinimum: UByte = 150u): Boolean =
        TODO("allowOperation")
    fun canManageEstate(): Boolean = TODO("canManageEstate")
    fun getAdminOverride(): Boolean = adminOverride
    fun getAgentAccess(): Any? = TODO("getAgentAccess")

    // ---- Rendering ------------------------------------------------------------

    var showAvatar: Boolean = true
    private var mRenderState: UByte = 0u
    private var appearanceSerialNum: UInt = 0u

    fun setRenderState(newState: UByte) { mRenderState = (mRenderState.toInt() or newState.toInt()).toUByte() }
    fun clearRenderState(clearState: UByte) { mRenderState = (mRenderState.toInt() and clearState.toInt().inv()).toUByte() }
    fun getRenderState(): UByte = mRenderState

    fun getHeadRotation(): Quaternion = TODO("getHeadRotation")
    fun needsRenderAvatar(): Boolean = TODO("needsRenderAvatar — camera mode check")
    fun needsRenderHead(): Boolean = TODO("needsRenderHead")

    val effectColor: FloatArray = floatArrayOf(0f, 1f, 1f, 1f)  // RGBA cyan default
    fun getEffectColor(): FloatArray = effectColor
    fun setEffectColor(color: FloatArray) { color.copyInto(effectColor) }

    // ---- Animations ----------------------------------------------------------

    val mouselookModeInListeners:  MutableList<() -> Unit> = mutableListOf()
    val mouselookModeOutListeners: MutableList<() -> Unit> = mutableListOf()

    var customAnim: Boolean = false

    fun stopCurrentAnimations(forceKeepScriptPerms: Boolean = false) {
        TODO("APR: stopCurrentAnimations — send stop for all playing anims")
    }

    fun requestStopMotion(motion: Any?) {
        TODO("APR: requestStopMotion — send ANIM_REQUEST_STOP")
    }

    fun onAnimStop(id: LLUUID) { TODO("APR: onAnimStop id=$id") }

    fun sendAnimationRequests(animIds: List<LLUUID>, request: AnimRequest) {
        TODO("APR: sendAnimationRequests request=$request count=${animIds.size}")
    }

    fun sendAnimationRequest(animId: LLUUID, request: AnimRequest) {
        TODO("APR: sendAnimationRequest animId=$animId request=$request")
    }

    fun sendAnimationStateReset() { TODO("APR: sendAnimationStateReset") }
    fun sendRevokePermissions(target: LLUUID, permissions: UInt) {
        TODO("APR: sendRevokePermissions target=$target")
    }

    fun endAnimationUpdateUI() { TODO("APR: endAnimationUpdateUI") }
    fun unpauseAnimation() { TODO("APR: unpauseAnimation") }

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
        TODO("APR: setGroupContribution — send to server")
    }

    fun setUserGroupFlags(groupId: LLUUID, acceptNotices: Boolean, listInProfile: Boolean): Boolean {
        TODO("APR: setUserGroupFlags")
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

    fun canJoinGroups(): Boolean = TODO("canJoinGroups — check group count limit")

    fun observeFriends() { TODO("APR: observeFriends — register friend observer") }
    fun friendsChanged() { TODO("APR: friendsChanged — refresh proxy list") }

    // ---- Messaging -----------------------------------------------------------

    fun sendMessage() { TODO("APR: sendMessage — to agent's region") }
    fun sendReliableMessage() { TODO("APR: sendReliableMessage") }
    fun sendAgentDataUpdateRequest() { TODO("APR: sendAgentDataUpdateRequest") }
    fun sendAgentUserInfoRequest() { TODO("APR: sendAgentUserInfoRequest") }
    fun sendAgentUpdateUserInfo(imToEmail: Boolean, directoryVisibility: String) {
        TODO("APR: sendAgentUpdateUserInfo")
    }

    fun sendAgentSetAppearance() { TODO("APR: sendAgentSetAppearance") }
    fun dumpSentAppearance(dumpPrefix: String) { TODO("APR: dumpSentAppearance prefix=$dumpPrefix") }

    fun requestPostCapability(capName: String, postData: Any?, cbSuccess: ((Any?) -> Unit)? = null, cbFailure: ((Any?) -> Unit)? = null): Boolean {
        TODO("APR: requestPostCapability capName=$capName")
    }

    fun requestGetCapability(capName: String, cbSuccess: ((Any?) -> Unit)? = null, cbFailure: ((Any?) -> Unit)? = null): Boolean {
        TODO("APR: requestGetCapability capName=$capName")
    }

    // ---- Name utilities ------------------------------------------------------

    fun buildFullname(name: StringBuilder) { TODO("buildFullname") }
    fun buildFullnameAndTitle(name: StringBuilder) { TODO("buildFullnameAndTitle") }

    // ---- Phantom (Firestorm) --------------------------------------------------

    private var phantom: Boolean = false
    fun togglePhantom() { phantom = !phantom; TODO("APR: togglePhantom — send to sim") }
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

        fun parseTeleportMessages(xmlFilename: String) { TODO("APR: parseTeleportMessages xmlFilename=$xmlFilename") }
        fun stopFidget() { TODO("APR: stopFidget") }
        fun toggleFlying() {
            instance().setFlying(!instance().getFlying())
            AgentCamera.resetView(resetCamera = true, changeCamera = false, movement = true)
        }
        fun enableFlying(): Boolean = TODO("enableFlying — check avatar sit/fly state")
        fun isSitting(): Boolean = TODO("isSitting — delegate to gAgentAvatarp")
        fun isActionAllowed(sdname: String): Boolean = TODO("isActionAllowed sdname=$sdname")
        fun pressMicrophone(name: String) { TODO("APR: pressMicrophone") }
        fun releaseMicrophone(name: String) { TODO("APR: releaseMicrophone") }
        fun toggleMicrophone(name: String) { TODO("APR: toggleMicrophone") }
        fun isMicrophoneOn(sdname: String): Boolean = TODO("isMicrophoneOn")
        fun dumpGroupInfo() { TODO("dumpGroupInfo") }
        fun clearVisualParams() { TODO("clearVisualParams") }
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
