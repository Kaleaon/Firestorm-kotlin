// Converted from llagentcamera.h / llagentcamera.cpp
// Original: Copyright (C) 2010, Linden Research, Inc. (LGPL 2.1)
package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llinventory.*
import com.firestorm.llappearance.*

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
    TPP_VIEW,        // FS: Third Person Perspective (Penny Patton)
    RLV_SETCAM_VIEW  // RLVa: @setcam_eyeoffset / @setcam_focusoffset
}

// ---------------------------------------------------------------------------
// Zoom / lag constants (mirrors llagentcamera.cpp globals)
// ---------------------------------------------------------------------------

private const val MIN_ZOOM_FRACTION              = 0.25f
private const val INITIAL_ZOOM_FRACTION          = 1.0f
private const val MAX_ZOOM_FRACTION              = 8.0f
private const val CAMERA_ZOOM_HALF_LIFE          = 0.07f   // seconds
private const val CAMERA_LAG_HALF_LIFE           = 0.25f
private const val MIN_CAMERA_LAG                 = 0.5f
private const val MAX_CAMERA_LAG                 = 5.0f
private const val MIN_CAMERA_DISTANCE            = 0.1f
private const val MAX_CAMERA_DISTANCE_FROM_AGENT = 50.0f
private const val LAND_MIN_ZOOM                  = 0.15f
private const val AVATAR_MIN_ZOOM                = 0.5f
private const val APPEARANCE_MIN_ZOOM            = 0.39f
private const val APPEARANCE_MAX_ZOOM            = 8.0f
private const val DEFAULT_ANIMATION_DURATION     = 0.33f

// ---------------------------------------------------------------------------
// AgentCamera singleton
// ---------------------------------------------------------------------------

object AgentCamera {

    // --- Lifecycle -----------------------------------------------------------

    var isInitialized: Boolean = false
        private set

    fun init() {
        isInitialized = true
        mCameraMode     = CameraMode.THIRD_PERSON
        mLastCameraMode = CameraMode.THIRD_PERSON
        mCameraPreset   = CameraPreset.REAR_VIEW
        mCameraZoomFraction = INITIAL_ZOOM_FRACTION
        mCurrentCameraDistance = 2.0f
        mTargetCameraDistance  = 2.0f
        clearGeneralKeys()
        clearOrbitKeys()
        clearPanKeys()
    }

    fun cleanup() {
        isInitialized = false
    }

    // --- Mode ----------------------------------------------------------------

    private var mCameraMode:     CameraMode = CameraMode.THIRD_PERSON
    private var mLastCameraMode: CameraMode = CameraMode.THIRD_PERSON

    val cameraMode:     CameraMode get() = mCameraMode
    val lastCameraMode: CameraMode get() = mLastCameraMode

    fun cameraThirdPerson()    = mCameraMode == CameraMode.THIRD_PERSON   && mLastCameraMode == CameraMode.THIRD_PERSON
    fun cameraMouselook()      = mCameraMode == CameraMode.MOUSELOOK      && mLastCameraMode == CameraMode.MOUSELOOK
    fun cameraCustomizeAvatar()= mCameraMode == CameraMode.CUSTOMIZE_AVATAR
    fun cameraFollow()         = mCameraMode == CameraMode.FOLLOW         && mLastCameraMode == CameraMode.FOLLOW

    fun changeCameraToDefault()                          { TODO("changeCameraToDefault") }
    fun changeCameraToMouselook(animate: Boolean = true) { TODO("changeCameraToMouselook") }
    fun changeCameraToThirdPerson(animate: Boolean = true) { TODO("changeCameraToThirdPerson") }
    fun changeCameraToCustomizeAvatar()                  { TODO("changeCameraToCustomizeAvatar") }
    fun changeCameraToFollow(animate: Boolean = true)    { TODO("changeCameraToFollow") }

    fun updateCamera()     { TODO("updateCamera: interpolate position/focus each frame") }
    fun resetCamera()      { TODO("resetCamera: slam to default position") }
    fun updateLastCamera() { mLastCameraMode = mCameraMode }

    // --- Preset --------------------------------------------------------------

    private var mCameraPreset: CameraPreset = CameraPreset.REAR_VIEW

    val cameraPreset: CameraPreset get() = mCameraPreset

    fun switchCameraPreset(preset: CameraPreset) {
        mCameraPreset = preset
        TODO("switchCameraPreset: apply offset/focus for $preset")
    }

    fun getCameraOffsetInitial(): Vector3 = TODO("getCameraOffsetInitial")
    fun getCameraOffsetScale(): Float     = TODO("getCameraOffsetScale")
    fun getFocusOffsetInitial(): Vector3d = TODO("getFocusOffsetInitial")

    fun getCurrentCameraOffset(): Vector3     = TODO("getCurrentCameraOffset")
    fun getCurrentFocusOffset(): Vector3d     = TODO("getCurrentFocusOffset")
    fun getCurrentAvatarRotation(): Quaternion= TODO("getCurrentAvatarRotation")

    fun isJoystickCameraUsed(): Boolean = TODO("isJoystickCameraUsed")

    var initSitRot: Quaternion = Quaternion.IDENTITY
    fun rotateToInitSitRot() { TODO("rotateToInitSitRot") }

    // --- Position ------------------------------------------------------------

    private var mCurrentCameraDistance: Float = 2.0f
    private var mTargetCameraDistance:  Float = 2.0f
    private var mCameraFOVZoomFactor:   Float = 0.0f
    private var mCameraCurrentFOVZoomFactor: Float = 0.0f
    private var mCameraZoomFraction:    Float = INITIAL_ZOOM_FRACTION
    private var mCameraCollidePlane:    Vector4 = Vector4.ZERO
    private var mCameraLag:             Vector3 = Vector3.ZERO
    var cameraSmoothingStop: Boolean = false
    private var mCameraSmoothingLastPositionGlobal: Vector3d = Vector3d.ZERO
    val cameraUpVector: Vector3 = Vector3.Z_AXIS

    /** Camera position in agent-local coordinates (updated each frame). */
    var cameraPositionAgent: Vector3 = Vector3.ZERO
        private set

    fun getCameraPositionGlobal(): Vector3d = TODO("getCameraPositionGlobal")
    fun calcCameraPositionTargetGlobal(hitLimit: BooleanArray? = null): Vector3d = TODO("calcCameraPositionTargetGlobal")
    fun getCameraMinOffGround(): Float = TODO("getCameraMinOffGround")
    fun setCameraCollidePlane(plane: Vector4) { mCameraCollidePlane = plane }
    fun calcCameraMinDistance(objMinDistance: FloatArray): Boolean = TODO("calcCameraMinDistance")
    val currentCameraBuildOffset: Float get() = TODO("currentCameraBuildOffset: mCameraFocusOffset.length")
    fun clearCameraLag() { mCameraLag = Vector3.ZERO }

    // --- Sit -----------------------------------------------------------------

    private var mSitCameraEnabled: Boolean = false
    private var mSitCameraPos:   Vector3 = Vector3.ZERO
    private var mSitCameraFocus: Vector3 = Vector3.ZERO

    fun sitCameraEnabled() = mSitCameraEnabled
    fun setupSitCamera()   { TODO("setupSitCamera") }
    fun setSitCamera(objectId: LLUUID, cameraPos: Vector3 = Vector3.ZERO, cameraFocus: Vector3 = Vector3.ZERO) {
        mSitCameraPos   = cameraPos
        mSitCameraFocus = cameraFocus
        mSitCameraEnabled = (objectId != LLUUID.NULL)
    }

    // --- Animation -----------------------------------------------------------

    private var mCameraAnimating:   Boolean = false
    private var mAnimationDuration: Float   = DEFAULT_ANIMATION_DURATION

    fun getCameraAnimating()           = mCameraAnimating
    fun setCameraAnimating(b: Boolean) { mCameraAnimating = b }
    fun setAnimationDuration(seconds: Float) { mAnimationDuration = seconds }
    fun startCameraAnimation() { mCameraAnimating = true;  TODO("startCameraAnimation: record start pos/focus") }
    fun stopCameraAnimation()  { mCameraAnimating = false }

    // --- Focus ---------------------------------------------------------------

    var focusOnAvatar: Boolean = true
        private set
    var focusGlobal:       Vector3d = Vector3d.ZERO; private set
    var focusTargetGlobal: Vector3d = Vector3d.ZERO; private set

    fun calcFocusPositionTargetGlobal(): Vector3d = TODO("calcFocusPositionTargetGlobal")
    fun updateFocusOffset()   { TODO("updateFocusOffset") }
    fun validateFocusObject() { TODO("validateFocusObject") }

    fun setFocusGlobal(focus: Vector3d, objectId: LLUUID = LLUUID.NULL) {
        focusTargetGlobal = focus
        TODO("setFocusGlobal: begin lerp toward focus")
    }
    fun setFocusOnAvatar(focus: Boolean, animate: Boolean, resetAxes: Boolean = true) {
        focusOnAvatar = focus
        TODO("setFocusOnAvatar")
    }
    fun setCameraPosAndFocusGlobal(pos: Vector3d, focus: Vector3d, objectId: LLUUID) {
        TODO("setCameraPosAndFocusGlobal")
    }
    fun setAllowChangeToFollow(allow: Boolean) { TODO("setAllowChangeToFollow") }
    fun setObjectTracking(track: Boolean)      { TODO("setObjectTracking") }

    // --- Orbit ---------------------------------------------------------------

    fun cameraOrbitAround(radians: Float) { TODO("cameraOrbitAround: CCW around focus") }
    fun cameraOrbitOver(radians: Float)   { TODO("cameraOrbitOver: tilt over focus") }
    fun cameraOrbitIn(meters: Float)      { TODO("cameraOrbitIn: move toward focus") }
    fun cameraRollOver(radians: Float)    { TODO("cameraRollOver: roll camera") }
    fun resetCameraOrbit()   { TODO("resetCameraOrbit") }
    fun resetOrbitDiff()     { TODO("resetOrbitDiff") }
    fun resetCameraRoll()    { TODO("resetCameraRoll") }

    // --- Zoom ----------------------------------------------------------------

    /** Field of view of the default camera, in radians. Exposed for HUD scaling. */
    var defaultFov: Float = Math.toRadians(60.0).toFloat()

    fun handleScrollWheel(clicks: Int)          { TODO("handleScrollWheel") }
    fun cameraZoomIn(factor: Float)             { TODO("GPU: cameraZoomIn factor=$factor") }
    fun getCameraZoomFraction(getThirdPerson: Boolean = false): Float = mCameraZoomFraction
    fun setCameraZoomFraction(fraction: Float)  { mCameraZoomFraction = fraction.coerceIn(MIN_ZOOM_FRACTION, MAX_ZOOM_FRACTION) }
    fun calcCameraFOVZoomFactor(): Float        = TODO("calcCameraFOVZoomFactor")
    fun getAgentHUDTargetZoom(): Float          = TODO("getAgentHUDTargetZoom")
    fun resetCameraZoomFraction()               { mCameraZoomFraction = INITIAL_ZOOM_FRACTION }
    val currentCameraZoomFraction: Float get()  = mCameraZoomFraction

    // --- Pan -----------------------------------------------------------------

    fun cameraPanIn(meters: Float)    { TODO("cameraPanIn") }
    fun cameraPanLeft(meters: Float)  { TODO("cameraPanLeft") }
    fun cameraPanUp(meters: Float)    { TODO("cameraPanUp") }
    fun resetCameraPan()  { TODO("resetCameraPan") }
    fun resetPanDiff()    { TODO("resetPanDiff") }

    // --- View ----------------------------------------------------------------

    var drawDistance: Float = 128.0f

    fun resetView(resetCamera: Boolean = true, changeCamera: Boolean = false, movement: Boolean = false) {
        TODO("resetView")
    }
    fun unlockView() { TODO("unlockView") }

    // --- Mouselook -----------------------------------------------------------

    var forceMouselook: Boolean = false

    // --- HUD -----------------------------------------------------------------

    var hudTargetZoom: Float = 1.0f
    var hudCurZoom:    Float = 1.0f

    // --- General movement keys -----------------------------------------------

    var atKey:   Int   = 0
    var walkKey: Int   = 0
    var leftKey: Int   = 0
    var upKey:   Int   = 0
    var yawKey:  Float = 0.0f
    var pitchKey:Float = 0.0f

    fun clearGeneralKeys() { atKey = 0; walkKey = 0; leftKey = 0; upKey = 0; yawKey = 0f; pitchKey = 0f }

    companion object {
        fun directionToKey(direction: Int): Int = direction.coerceIn(-1, 1)
    }

    // --- Orbit keys ----------------------------------------------------------

    var orbitLeftKey:  Float = 0.0f
    var orbitRightKey: Float = 0.0f
    var orbitUpKey:    Float = 0.0f
    var orbitDownKey:  Float = 0.0f
    var orbitInKey:    Float = 0.0f
    var orbitOutKey:   Float = 0.0f
    var rollLeftKey:   Float = 0.0f
    var rollRightKey:  Float = 0.0f

    fun clearOrbitKeys() { orbitLeftKey = 0f; orbitRightKey = 0f; orbitUpKey = 0f; orbitDownKey = 0f; orbitInKey = 0f; orbitOutKey = 0f; rollLeftKey = 0f; rollRightKey = 0f }

    // --- Pan keys ------------------------------------------------------------

    var panUpKey:    Float = 0.0f
    var panDownKey:  Float = 0.0f
    var panLeftKey:  Float = 0.0f
    var panRightKey: Float = 0.0f
    var panInKey:    Float = 0.0f
    var panOutKey:   Float = 0.0f

    fun clearPanKeys() { panUpKey = 0f; panDownKey = 0f; panLeftKey = 0f; panRightKey = 0f; panInKey = 0f; panOutKey = 0f }

    // --- Save/load camera position (FS: FIRE-7758) ---------------------------

    fun storeCameraPosition() { TODO("storeCameraPosition") }
    fun loadCameraPosition()  { TODO("loadCameraPosition") }

    // --- Third-person head offset --------------------------------------------

    var thirdPersonHeadOffset: Vector3 = Vector3(0f, 0f, 1f)
    fun calcThirdPersonFocusOffset(): Vector3d = TODO("calcThirdPersonFocusOffset")

    // --- Follow cam ----------------------------------------------------------

    fun isFollowCamLocked(): Boolean = TODO("isFollowCamLocked")
}
