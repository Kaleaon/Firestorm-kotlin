package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

// ---------------------------------------------------------------------------
// Enumerations
// ---------------------------------------------------------------------------

enum class CameraMode {
    THIRD_PERSON,
    MOUSELOOK,
    CUSTOMIZE_AVATAR,
    FOLLOW
}

enum class CameraPreset {
    REAR_VIEW,
    FRONT_VIEW,
    GROUP_VIEW,
    CUSTOM,
    TPP_VIEW,           // FS: Penny Patton third-person perspective
    RLV_SETCAM_VIEW     // RLVa: @setcam_eyeoffset / @setcam_focusoffset
}

// ---------------------------------------------------------------------------
// Constants (mirrors llagentcamera.cpp globals)
// ---------------------------------------------------------------------------

private const val MIN_ZOOM_FRACTION              = 0.25f
private const val INITIAL_ZOOM_FRACTION          = 1.0f
private const val MAX_ZOOM_FRACTION              = 8.0f
private const val CAMERA_ZOOM_HALF_LIFE          = 0.07f
private const val FOV_ZOOM_HALF_LIFE             = 0.07f
private const val CAMERA_FOCUS_HALF_LIFE         = 0.0f
private const val CAMERA_LAG_HALF_LIFE           = 0.25f
private const val MIN_CAMERA_LAG                 = 0.5f
private const val MAX_CAMERA_LAG                 = 5.0f
private const val CAMERA_COLLIDE_EPSILON         = 0.1f
private const val MIN_CAMERA_DISTANCE            = 0.1f
private const val AVATAR_ZOOM_MIN_X_FACTOR       = 0.55f
private const val AVATAR_ZOOM_MIN_Y_FACTOR       = 0.7f
private const val AVATAR_ZOOM_MIN_Z_FACTOR       = 1.15f
private const val MAX_CAMERA_DISTANCE_FROM_AGENT = 50.0f
private const val MAX_CAMERA_DISTANCE_FROM_OBJECT= 496.0f
private const val CAMERA_FUDGE_FROM_OBJECT       = 16.0f
private const val MAX_CAMERA_SMOOTH_DISTANCE     = 50.0f
private const val HEAD_BUFFER_SIZE               = 0.3f
private const val CUSTOMIZE_AVATAR_CAMERA_ANIM_SLOP = 0.1f
private const val LAND_MIN_ZOOM                  = 0.15f
private const val AVATAR_MIN_ZOOM                = 0.5f
private const val OBJECT_MIN_ZOOM                = 0.02f
private const val APPEARANCE_MIN_ZOOM            = 0.39f
private const val APPEARANCE_MAX_ZOOM            = 8.0f
private const val CUSTOMIZE_AVATAR_CAMERA_DEFAULT_DIST = 3.5f
private const val GROUND_TO_AIR_CAMERA_TRANSITION_TIME  = 0.5f
private const val OBJECT_EXTENTS_PADDING         = 0.5f
private const val DEFAULT_ANIMATION_DURATION     = 0.33f
private const val DEFAULT_FAR_PLANE              = 128.0f

// ---------------------------------------------------------------------------
// AgentCamera singleton  (C++ LLAgentCamera / extern gAgentCamera)
// ---------------------------------------------------------------------------

object AgentCamera {

    // ---- Lifecycle -----------------------------------------------------------

    var isInitialized: Boolean = false
        private set

    fun init() {
        isInitialized       = true
        mCameraMode         = CameraMode.THIRD_PERSON
        mLastCameraMode     = CameraMode.THIRD_PERSON
        mCameraPreset       = CameraPreset.REAR_VIEW
        mCameraZoomFraction = INITIAL_ZOOM_FRACTION
        mCurrentCameraDistance = 2.0f
        mTargetCameraDistance  = 2.0f
        mDrawDistance       = DEFAULT_FAR_PLANE
        mFocusOnAvatar      = true
        mTrackFocusObject   = true
        clearGeneralKeys()
        clearOrbitKeys()
        clearPanKeys()
        resetPanDiff()
        resetOrbitDiff()
    }

    fun cleanup() {
        setSitCamera(LLUUID.NULL)
        isInitialized = false
    }

    fun setAvatarObject(avatar: Any?) {
        System.err.println("AgentCamera: setAvatarObject — create HUD look-at and point-at effects not yet implemented")
    }

    // ---- Mode ----------------------------------------------------------------

    private var mCameraMode:     CameraMode = CameraMode.THIRD_PERSON
    private var mLastCameraMode: CameraMode = CameraMode.THIRD_PERSON

    val cameraMode:     CameraMode get() = mCameraMode
    val lastCameraMode: CameraMode get() = mLastCameraMode

    fun cameraThirdPerson():     Boolean = mCameraMode == CameraMode.THIRD_PERSON    && mLastCameraMode == CameraMode.THIRD_PERSON
    fun cameraMouselook():       Boolean = mCameraMode == CameraMode.MOUSELOOK       && mLastCameraMode == CameraMode.MOUSELOOK
    fun cameraCustomizeAvatar(): Boolean = mCameraMode == CameraMode.CUSTOMIZE_AVATAR
    fun cameraFollow():          Boolean = mCameraMode == CameraMode.FOLLOW          && mLastCameraMode == CameraMode.FOLLOW

    fun changeCameraToDefault()                            { System.err.println("AgentCamera: changeCameraToDefault not yet implemented") }
    fun changeCameraToMouselook(animate: Boolean = true)   { System.err.println("AgentCamera: changeCameraToMouselook not yet implemented") }
    fun changeCameraToThirdPerson(animate: Boolean = true) { System.err.println("AgentCamera: changeCameraToThirdPerson not yet implemented") }
    fun changeCameraToCustomizeAvatar()                    { System.err.println("AgentCamera: changeCameraToCustomizeAvatar not yet implemented") }
    fun changeCameraToFollow(animate: Boolean = true)      { System.err.println("AgentCamera: changeCameraToFollow not yet implemented") }

    fun updateCamera()     { /* GPU: updateCamera — interpolate position/focus each frame */ }
    fun resetCamera()      { System.err.println("AgentCamera: resetCamera not yet implemented") }
    fun updateLastCamera() { mLastCameraMode = mCameraMode }

    // ---- Preset --------------------------------------------------------------

    private var mCameraPreset: CameraPreset = CameraPreset.REAR_VIEW

    val cameraPreset: CameraPreset get() = mCameraPreset

    fun switchCameraPreset(preset: CameraPreset) {
        mCameraPreset = preset
        System.err.println("AgentCamera: switchCameraPreset not yet implemented")
    }

    fun getCameraOffsetInitial(): Vector3  { System.err.println("AgentCamera: getCameraOffsetInitial not yet implemented"); return Vector3.ZERO }
    fun getCameraOffsetScale(): Float      { System.err.println("AgentCamera: getCameraOffsetScale not yet implemented"); return 0f }
    fun getFocusOffsetInitial(): Vector3d  { System.err.println("AgentCamera: getFocusOffsetInitial not yet implemented"); return Vector3d.ZERO }

    fun getCurrentCameraOffset(): Vector3     { System.err.println("AgentCamera: getCurrentCameraOffset not yet implemented"); return Vector3.ZERO }
    fun getCurrentFocusOffset(): Vector3d     { System.err.println("AgentCamera: getCurrentFocusOffset not yet implemented"); return Vector3d.ZERO }
    fun getCurrentAvatarRotation(): Quaternion{ System.err.println("AgentCamera: getCurrentAvatarRotation not yet implemented"); return Quaternion.IDENTITY }

    fun isJoystickCameraUsed(): Boolean { System.err.println("AgentCamera: isJoystickCameraUsed not yet implemented"); return false }

    var initSitRot: Quaternion = Quaternion.IDENTITY
    fun setInitSitRot(rot: Quaternion) { initSitRot = rot }
    fun rotateToInitSitRot() { System.err.println("AgentCamera: rotateToInitSitRot not yet implemented") }

    // ---- Camera position / distance ------------------------------------------

    private var mCurrentCameraDistance: Float = 2.0f
    private var mTargetCameraDistance:  Float = 2.0f
    private var mCameraFOVZoomFactor:   Float = 0.0f
    private var mCameraCurrentFOVZoomFactor: Float = 0.0f
    private var mCameraZoomFraction:    Float = INITIAL_ZOOM_FRACTION
    private var mCameraCollidePlane:    Vector4 = Vector4.ZERO
    private var mCameraLag:             Vector3 = Vector3.ZERO
    private var mCameraVirtualPositionAgent: Vector3 = Vector3.ZERO
    private var mCameraFocusOffset:         Vector3d = Vector3d.ZERO
    private var mCameraFocusOffsetTarget:   Vector3d = Vector3d.ZERO
    var cameraSmoothingStop: Boolean = false
    private var mCameraSmoothingLastPositionGlobal: Vector3d = Vector3d.ZERO
    private var mCameraSmoothingLastPositionAgent:  Vector3d = Vector3d.ZERO
    private val mCameraUpVector: Vector3 = Vector3(0f, 0f, 1f)
    private var mPanFocusDiff: Vector3d = Vector3d.ZERO

    var cameraPositionAgent: Vector3 = Vector3.ZERO
        private set

    fun getCameraPositionGlobal(): Vector3d { System.err.println("AgentCamera: getCameraPositionGlobal not yet implemented"); return Vector3d.ZERO }
    fun getCameraPositionAgent(): Vector3   = cameraPositionAgent
    fun calcCameraPositionTargetGlobal(hitLimit: BooleanArray? = null): Vector3d { System.err.println("AgentCamera: calcCameraPositionTargetGlobal not yet implemented"); return Vector3d.ZERO }
    fun getCameraMinOffGround(): Float { System.err.println("AgentCamera: getCameraMinOffGround not yet implemented"); return 0f }
    fun setCameraCollidePlane(plane: Vector4) { mCameraCollidePlane = plane }
    fun calcCameraMinDistance(objMinDistance: FloatArray): Boolean { System.err.println("AgentCamera: calcCameraMinDistance not yet implemented"); return false }
    val currentCameraBuildOffset: Float get() { System.err.println("AgentCamera: currentCameraBuildOffset not yet implemented"); return 0f }
    fun clearCameraLag() { mCameraLag = Vector3.ZERO }
    fun getCameraUpVector(): Vector3 = mCameraUpVector

    // ---- Sit camera ----------------------------------------------------------

    private var mSitCameraEnabled: Boolean = false
    private var mSitCameraPos:     Vector3 = Vector3.ZERO
    private var mSitCameraFocus:   Vector3 = Vector3.ZERO

    fun sitCameraEnabled(): Boolean = mSitCameraEnabled
    fun setupSitCamera()   { System.err.println("AgentCamera: setupSitCamera not yet implemented") }
    fun setSitCamera(objectId: LLUUID, cameraPos: Vector3 = Vector3.ZERO, cameraFocus: Vector3 = Vector3.ZERO) {
        mSitCameraPos   = cameraPos
        mSitCameraFocus = cameraFocus
        mSitCameraEnabled = (objectId != LLUUID.NULL)
    }

    // ---- Animation -----------------------------------------------------------

    private var mCameraAnimating:            Boolean = false
    private var mAnimationDuration:          Float   = DEFAULT_ANIMATION_DURATION
    private var mAnimationCameraStartGlobal: Vector3d = Vector3d.ZERO
    private var mAnimationFocusStartGlobal:  Vector3d = Vector3d.ZERO

    fun getCameraAnimating():           Boolean = mCameraAnimating
    fun setCameraAnimating(b: Boolean) { mCameraAnimating = b }
    fun setAnimationDuration(seconds: Float) { mAnimationDuration = seconds }

    fun startCameraAnimation() {
        mAnimationCameraStartGlobal = getCameraPositionGlobal()
        mAnimationFocusStartGlobal  = focusGlobal
        mCameraAnimating = true
        System.err.println("AgentCamera: startCameraAnimation not yet implemented")
    }

    fun stopCameraAnimation()  { mCameraAnimating = false }

    // ---- Focus ---------------------------------------------------------------

    var focusOnAvatar: Boolean = true
        private set
    private var mAllowChangeToFollow: Boolean = false
    var focusGlobal:       Vector3d = Vector3d.ZERO
        private set
    var focusTargetGlobal: Vector3d = Vector3d.ZERO
        private set
    private var mFocusObject:       Any? = null   // LLViewerObject pointer
    private var mFocusObjectDist:   Float = 0.0f
    private var mFocusObjectOffset: Vector3 = Vector3.ZERO
    private var mTrackFocusObject:  Boolean = true
    private var mFocusOnAvatar:     Boolean = true

    fun calcFocusPositionTargetGlobal(): Vector3d { System.err.println("AgentCamera: calcFocusPositionTargetGlobal not yet implemented"); return Vector3d.ZERO }
    fun calcFocusOffset(obj: Any?, posAgent: Vector3, x: Int, y: Int): Vector3 { System.err.println("AgentCamera: calcFocusOffset not yet implemented"); return Vector3.ZERO }
    fun getFocusOnAvatar(): Boolean = mFocusOnAvatar
    fun getFocusObject(): Any? = mFocusObject
    fun getFocusObjectDist(): Float = mFocusObjectDist
    fun getFocusGlobal(): Vector3d = focusGlobal
    fun getFocusTargetGlobal(): Vector3d = focusTargetGlobal
    fun updateFocusOffset()   { System.err.println("AgentCamera: updateFocusOffset not yet implemented") }
    fun validateFocusObject() { System.err.println("AgentCamera: validateFocusObject not yet implemented") }
    fun clearFocusObject()    { mFocusObject = null }
    fun setFocusObject(obj: Any?) { mFocusObject = obj }

    fun setFocusGlobal(focus: Vector3d, objectId: LLUUID = LLUUID.NULL) {
        focusTargetGlobal = focus
        System.err.println("AgentCamera: setFocusGlobal not yet implemented")
    }

    fun setFocusOnAvatar(focus: Boolean, animate: Boolean, resetAxes: Boolean = true) {
        mFocusOnAvatar = focus
        focusOnAvatar = focus
        System.err.println("AgentCamera: setFocusOnAvatar not yet implemented")
    }

    fun setCameraPosAndFocusGlobal(pos: Vector3d, focus: Vector3d, objectId: LLUUID) {
        System.err.println("AgentCamera: setCameraPosAndFocusGlobal not yet implemented")
    }

    fun setAllowChangeToFollow(allow: Boolean) { mAllowChangeToFollow = allow }
    fun setObjectTracking(track: Boolean)      { mTrackFocusObject = track }

    // ---- Look-at / point-at --------------------------------------------------

    fun updateLookAt(mouseX: Int, mouseY: Int) { System.err.println("AgentCamera: updateLookAt not yet implemented") }
    fun setLookAt(targetType: Int, obj: Any? = null, position: Vector3 = Vector3.ZERO): Boolean { System.err.println("AgentCamera: setLookAt not yet implemented"); return false }
    fun getLookAtType(): Int { System.err.println("AgentCamera: getLookAtType not yet implemented"); return 0 }
    fun lookAtLastChat() { System.err.println("AgentCamera: lookAtLastChat not yet implemented") }
    fun slamLookAt(lookAt: Vector3) { System.err.println("AgentCamera: slamLookAt not yet implemented") }
    fun setPointAt(targetType: Int, obj: Any? = null, position: Vector3 = Vector3.ZERO): Boolean { System.err.println("AgentCamera: setPointAt not yet implemented"); return false }
    fun getPointAtType(): Int { System.err.println("AgentCamera: getPointAtType not yet implemented"); return 0 }

    // ---- Orbit ---------------------------------------------------------------

    private var mOrbitAroundRadians: Float = 0.0f
    private var mOrbitOverAngle:     Float = 0.0f
    private var mRollAngle:          Float = 0.0f

    // When focus is on avatar in third-person, orbit delegates to gAgent.yaw/pitch.
    // In build/free-camera mode it directly rotates mCameraFocusOffsetTarget.
    fun cameraOrbitAround(radians: Float) {
        if (mFocusOnAvatar && (mCameraMode == CameraMode.THIRD_PERSON || mCameraMode == CameraMode.FOLLOW)) {
            gAgent.yaw(radians)
        } else {
            mOrbitAroundRadians += radians
            // GPU: cameraOrbitAround — rotVec mCameraFocusOffsetTarget around Z
        }
    }

    fun cameraOrbitOver(radians: Float) {
        if (mFocusOnAvatar && mCameraMode == CameraMode.THIRD_PERSON) {
            gAgent.pitch(radians)
        } else {
            mOrbitOverAngle += radians
            // GPU: cameraOrbitOver — rotVec mCameraFocusOffsetTarget around left axis
        }
    }

    fun cameraOrbitIn(meters: Float) {
        if (mFocusOnAvatar && mCameraMode == CameraMode.THIRD_PERSON) {
            val cameraOffsetDist = getCameraOffsetInitial().length() * getCameraOffsetScale()
            mCameraZoomFraction = (mTargetCameraDistance - meters) / cameraOffsetDist.coerceAtLeast(0.001f)
            mCameraZoomFraction = mCameraZoomFraction.coerceIn(MIN_ZOOM_FRACTION, MAX_ZOOM_FRACTION)
            if (mCameraZoomFraction < MIN_ZOOM_FRACTION && meters > 0f) {
                changeCameraToMouselook(false)
            }
        } else {
            // GPU: cameraOrbitIn — move mCameraFocusOffsetTarget toward focus
        }
    }

    fun cameraRollOver(radians: Float) {
        mRollAngle = (mRollAngle + radians) % (2.0f * Math.PI.toFloat())
    }

    fun resetCameraOrbit()   {
        mCameraFocusOffset = Vector3d.ZERO  // placeholder; real impl reverses accumulated orbit
        mOrbitAroundRadians = 0f
        mOrbitOverAngle     = 0f
        mRollAngle          = 0f
        cameraZoomIn(1.0f)
    }

    fun resetOrbitDiff()     { mOrbitAroundRadians = 0f; mOrbitOverAngle = 0f }
    fun resetCameraRoll()    { mRollAngle = 0f }

    // ---- Zoom ----------------------------------------------------------------

    var defaultFov: Float = Math.toRadians(60.0).toFloat()

    fun handleScrollWheel(clicks: Int) { System.err.println("AgentCamera: handleScrollWheel not yet implemented") }

    fun cameraZoomIn(factor: Float) {
        if (factor == 1.0f) return
        // GPU: cameraZoomIn — scale mCameraFocusOffsetTarget with min/max clamps
    }

    fun getCameraZoomFraction(getThirdPerson: Boolean = false): Float {
        if (getThirdPerson || (mFocusOnAvatar && cameraThirdPerson())) {
            // map [MIN_ZOOM, MAX_ZOOM] → [1, 0]
            return 1.0f - (mCameraZoomFraction - MIN_ZOOM_FRACTION) / (MAX_ZOOM_FRACTION - MIN_ZOOM_FRACTION)
        }
        if (cameraCustomizeAvatar()) {
            System.err.println("AgentCamera: getCameraZoomFraction for CUSTOMIZE_AVATAR mode not yet implemented")
            return 0f
        }
        System.err.println("AgentCamera: getCameraZoomFraction for build/free camera mode not yet implemented")
        return 0f
    }

    fun setCameraZoomFraction(fraction: Float) {
        // fraction 0 = zoomed out, 1 = zoomed in
        if (mFocusOnAvatar && cameraThirdPerson()) {
            mCameraZoomFraction = MAX_ZOOM_FRACTION - fraction * (MAX_ZOOM_FRACTION - MIN_ZOOM_FRACTION)
            mCameraZoomFraction = mCameraZoomFraction.coerceIn(MIN_ZOOM_FRACTION, MAX_ZOOM_FRACTION)
        } else if (cameraCustomizeAvatar()) {
            System.err.println("AgentCamera: setCameraZoomFraction for CUSTOMIZE_AVATAR not yet implemented")
        } else {
            System.err.println("AgentCamera: setCameraZoomFraction for build/free camera mode not yet implemented")
        }
        startCameraAnimation()
    }

    fun calcCameraFOVZoomFactor(): Float { System.err.println("AgentCamera: calcCameraFOVZoomFactor not yet implemented"); return 0f }
    fun getAgentHUDTargetZoom(): Float   { System.err.println("AgentCamera: getAgentHUDTargetZoom not yet implemented"); return 0f }
    fun resetCameraZoomFraction()        { mCameraZoomFraction = INITIAL_ZOOM_FRACTION }
    val currentCameraZoomFraction: Float get() = mCameraZoomFraction

    // ---- Pan -----------------------------------------------------------------

    fun cameraPanIn(meters: Float) {
        System.err.println("AgentCamera: cameraPanIn not yet implemented")
    }

    fun cameraPanLeft(meters: Float) {
        cameraSmoothingStop = true
        System.err.println("AgentCamera: cameraPanLeft not yet implemented")
    }

    fun cameraPanUp(meters: Float) {
        cameraSmoothingStop = true
        System.err.println("AgentCamera: cameraPanUp not yet implemented")
    }

    fun resetCameraPan() {
        System.err.println("AgentCamera: resetCameraPan not yet implemented")
    }

    fun resetPanDiff() { mPanFocusDiff = Vector3d.ZERO }

    // ---- View ----------------------------------------------------------------

    var drawDistance: Float = DEFAULT_FAR_PLANE

    fun resetView(resetCamera: Boolean = true, changeCamera: Boolean = false, movement: Boolean = false) {
        if (changeCamera) {
            changeCameraToDefault()
        }
        if (resetCamera) {
            setFocusOnAvatar(true, true)
            mCameraFOVZoomFactor = 0.0f
            resetCameraRoll()
        }
        resetPanDiff()
        resetOrbitDiff()
        hudTargetZoom = 1.0f
        System.err.println("AgentCamera: resetView not yet implemented")
    }

    fun unlockView() {
        if (mFocusOnAvatar) {
            System.err.println("AgentCamera: unlockView not yet implemented")
        }
    }

    // ---- Mouselook -----------------------------------------------------------

    var forceMouselook: Boolean = false

    // ---- HUD -----------------------------------------------------------------

    var hudTargetZoom: Float = 1.0f
    var hudCurZoom:    Float = 1.0f

    // ---- RLVa camera constraints (Firestorm) ---------------------------------

    private var rlvMaxDist: Boolean = false
    private var rlvMinDist: Boolean = false
    private var posRlvRefGlobal: Vector3d = Vector3d.ZERO

    // ---- General movement keys -----------------------------------------------

    private var mAtKey:    Int   = 0
    private var mWalkKey:  Int   = 0
    private var mLeftKey:  Int   = 0
    private var mUpKey:    Int   = 0
    private var mYawKey:   Float = 0.0f
    private var mPitchKey: Float = 0.0f

    var atKey:    Int   get() = mAtKey;    set(v) { mAtKey   = v }
    var walkKey:  Int   get() = mWalkKey;  set(v) { mWalkKey = v }
    var leftKey:  Int   get() = mLeftKey;  set(v) { mLeftKey = v }
    var upKey:    Int   get() = mUpKey;    set(v) { mUpKey   = v }
    var yawKey:   Float get() = mYawKey;   set(v) { mYawKey  = v }
    var pitchKey: Float get() = mPitchKey; set(v) { mPitchKey = v }

    fun getAtKey():    Int   = mAtKey
    fun getWalkKey():  Int   = mWalkKey
    fun getLeftKey():  Int   = mLeftKey
    fun getUpKey():    Int   = mUpKey
    fun getYawKey():   Float = mYawKey
    fun getPitchKey(): Float = mPitchKey

    fun setAtKey(mag: Int)     { mAtKey   = mag }
    fun setWalkKey(mag: Int)   { mWalkKey = mag }
    fun setLeftKey(mag: Int)   { mLeftKey = mag }
    fun setUpKey(mag: Int)     { mUpKey   = mag }
    fun setYawKey(mag: Float)  { mYawKey  = mag }
    fun setPitchKey(mag: Float){ mPitchKey = mag }

    fun clearGeneralKeys() { mAtKey = 0; mWalkKey = 0; mLeftKey = 0; mUpKey = 0; mYawKey = 0f; mPitchKey = 0f }

    // ---- Orbit keys ----------------------------------------------------------

    private var mOrbitLeftKey:  Float = 0.0f
    private var mOrbitRightKey: Float = 0.0f
    private var mOrbitUpKey:    Float = 0.0f
    private var mOrbitDownKey:  Float = 0.0f
    private var mOrbitInKey:    Float = 0.0f
    private var mOrbitOutKey:   Float = 0.0f
    private var mRollLeftKey:   Float = 0.0f
    private var mRollRightKey:  Float = 0.0f

    fun getOrbitLeftKey():  Float = mOrbitLeftKey
    fun getOrbitRightKey(): Float = mOrbitRightKey
    fun getOrbitUpKey():    Float = mOrbitUpKey
    fun getOrbitDownKey():  Float = mOrbitDownKey
    fun getOrbitInKey():    Float = mOrbitInKey
    fun getOrbitOutKey():   Float = mOrbitOutKey
    fun getRollLeftKey():   Float = mRollLeftKey
    fun getRollRightKey():  Float = mRollRightKey

    fun setOrbitLeftKey(mag: Float)  { mOrbitLeftKey  = mag }
    fun setOrbitRightKey(mag: Float) { mOrbitRightKey = mag }
    fun setOrbitUpKey(mag: Float)    { mOrbitUpKey    = mag }
    fun setOrbitDownKey(mag: Float)  { mOrbitDownKey  = mag }
    fun setOrbitInKey(mag: Float)    { mOrbitInKey    = mag }
    fun setOrbitOutKey(mag: Float)   { mOrbitOutKey   = mag }
    fun setRollLeftKey(mag: Float)   { mRollLeftKey   = mag }
    fun setRollRightKey(mag: Float)  { mRollRightKey  = mag }

    fun clearOrbitKeys() {
        mOrbitLeftKey = 0f; mOrbitRightKey = 0f
        mOrbitUpKey   = 0f; mOrbitDownKey  = 0f
        mOrbitInKey   = 0f; mOrbitOutKey   = 0f
        mRollLeftKey  = 0f; mRollRightKey  = 0f
    }

    var orbitLeftKey:  Float get() = mOrbitLeftKey;  set(v) { mOrbitLeftKey  = v }
    var orbitRightKey: Float get() = mOrbitRightKey; set(v) { mOrbitRightKey = v }
    var orbitUpKey:    Float get() = mOrbitUpKey;    set(v) { mOrbitUpKey    = v }
    var orbitDownKey:  Float get() = mOrbitDownKey;  set(v) { mOrbitDownKey  = v }
    var orbitInKey:    Float get() = mOrbitInKey;    set(v) { mOrbitInKey    = v }
    var orbitOutKey:   Float get() = mOrbitOutKey;   set(v) { mOrbitOutKey   = v }
    var rollLeftKey:   Float get() = mRollLeftKey;   set(v) { mRollLeftKey   = v }
    var rollRightKey:  Float get() = mRollRightKey;  set(v) { mRollRightKey  = v }

    // ---- Pan keys ------------------------------------------------------------

    private var mPanUpKey:    Float = 0.0f
    private var mPanDownKey:  Float = 0.0f
    private var mPanLeftKey:  Float = 0.0f
    private var mPanRightKey: Float = 0.0f
    private var mPanInKey:    Float = 0.0f
    private var mPanOutKey:   Float = 0.0f

    fun getPanLeftKey():  Float = mPanLeftKey
    fun getPanRightKey(): Float = mPanRightKey
    fun getPanUpKey():    Float = mPanUpKey
    fun getPanDownKey():  Float = mPanDownKey
    fun getPanInKey():    Float = mPanInKey
    fun getPanOutKey():   Float = mPanOutKey

    fun setPanLeftKey(mag: Float)  { mPanLeftKey  = mag }
    fun setPanRightKey(mag: Float) { mPanRightKey = mag }
    fun setPanUpKey(mag: Float)    { mPanUpKey    = mag }
    fun setPanDownKey(mag: Float)  { mPanDownKey  = mag }
    fun setPanInKey(mag: Float)    { mPanInKey    = mag }
    fun setPanOutKey(mag: Float)   { mPanOutKey   = mag }

    fun clearPanKeys() {
        mPanUpKey = 0f; mPanDownKey  = 0f
        mPanLeftKey = 0f; mPanRightKey = 0f
        mPanInKey = 0f; mPanOutKey   = 0f
    }

    var panUpKey:    Float get() = mPanUpKey;    set(v) { mPanUpKey    = v }
    var panDownKey:  Float get() = mPanDownKey;  set(v) { mPanDownKey  = v }
    var panLeftKey:  Float get() = mPanLeftKey;  set(v) { mPanLeftKey  = v }
    var panRightKey: Float get() = mPanRightKey; set(v) { mPanRightKey = v }
    var panInKey:    Float get() = mPanInKey;    set(v) { mPanInKey    = v }
    var panOutKey:   Float get() = mPanOutKey;   set(v) { mPanOutKey   = v }

    // ---- Third-person head offset --------------------------------------------

    var thirdPersonHeadOffset: Vector3 = Vector3(0f, 0f, 1f)

    fun setThirdPersonHeadOffset(offset: Vector3) { thirdPersonHeadOffset = offset }
    fun calcThirdPersonFocusOffset(): Vector3d { System.err.println("AgentCamera: calcThirdPersonFocusOffset not yet implemented"); return Vector3d.ZERO }

    // ---- Follow cam ----------------------------------------------------------

    fun isFollowCamLocked(): Boolean { System.err.println("AgentCamera: isFollowCamLocked not yet implemented"); return false }

    // ---- Save/load camera position (FS: FIRE-7758) ---------------------------

    fun storeCameraPosition() { System.err.println("AgentCamera: storeCameraPosition not yet implemented") }
    fun loadCameraPosition()  { System.err.println("AgentCamera: loadCameraPosition not yet implemented") }

    // ---- Companion object (static members) -----------------------------------

    companion object {
        fun directionToKey(direction: Int): Int = direction.coerceIn(-1, 1)
    }
}

// Global accessor matching C++ `extern LLAgentCamera gAgentCamera;`
val gAgentCamera: AgentCamera get() = AgentCamera
