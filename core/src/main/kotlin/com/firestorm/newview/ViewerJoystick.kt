/**
 * ViewerJoystick.kt
 * Converted from llviewerjoystick.h / llviewerjoystick.cpp
 *
 * Viewer joystick / NDOF (SpaceNavigator) device support.
 * Manages device initialisation, axis polling, and translating joystick
 * deltas into avatar / flycam movement on each frame tick.
 *
 * Original: Copyright (C) 2010, Linden Research, Inc. (LGPL v2.1)
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// Constants (mirrors the C++ #defines)
// ---------------------------------------------------------------------------

/** Maximum number of axes reported by a joystick device. */
const val MAX_JOYSTICK_AXES: Int    = 8

/** Maximum number of buttons reported by a joystick device. */
const val MAX_JOYSTICK_BUTTONS: Int = 16

/** Maximum absolute value from a SpaceNavigator axis (DirectInput quirk). */
const val MAX_JOYSTICK_INPUT_VALUE: Float = 3000.0f

// ---------------------------------------------------------------------------
// Axis index constants — mirror the C++ X_I / Y_I / Z_I / RX_I / RY_I / RZ_I
// ---------------------------------------------------------------------------

private const val AXIS_X  = 1
private const val AXIS_Y  = 2
private const val AXIS_Z  = 0
private const val AXIS_RX = 4
private const val AXIS_RY = 5
private const val AXIS_RZ = 3

// ---------------------------------------------------------------------------
// JoystickAxis enum
// Named axes exposed to callers; values match the C++ index constants above.
// ---------------------------------------------------------------------------

enum class JoystickAxis(val index: Int) {
    /** Left/right strafe */
    TRANSLATE_X(AXIS_X),
    /** Forward/backward */
    TRANSLATE_Y(AXIS_Y),
    /** Up/down */
    TRANSLATE_Z(AXIS_Z),
    /** Pitch */
    ROTATE_X(AXIS_RX),
    /** Roll */
    ROTATE_Y(AXIS_RY),
    /** Yaw */
    ROTATE_Z(AXIS_RZ);

    companion object {
        fun fromIndex(idx: Int): JoystickAxis? = values().firstOrNull { it.index == idx }
    }
}

// ---------------------------------------------------------------------------
// DriverState — mirrors EJoystickDriverState
// ---------------------------------------------------------------------------

enum class DriverState {
    UNINITIALIZED,
    INITIALIZING,
    INITIALIZED
}

// ---------------------------------------------------------------------------
// ViewerJoystick — singleton, mirrors LLViewerJoystick (LLSingleton)
// ---------------------------------------------------------------------------

object ViewerJoystick {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /**
     * Current NDOF driver state.
     * Mirrors LLViewerJoystick::mDriverState.
     */
    var driverState: DriverState = DriverState.UNINITIALIZED
        private set

    /**
     * True when the NDOF driver has been successfully initialised.
     * Mirrors LLViewerJoystick::isJoystickInitialized().
     */
    val isJoystickInitialized: Boolean
        get() = driverState == DriverState.INITIALIZED

    /** Raw axis values from the most recent device poll. */
    private val axes:    FloatArray = FloatArray(MAX_JOYSTICK_AXES)

    /** Raw button states (non-zero = pressed) from the most recent poll. */
    private val buttons: IntArray   = IntArray(MAX_JOYSTICK_BUTTONS)

    /**
     * Per-frame axis deltas used by moveAvatar / moveFlycam.
     * Index 0-6 maps to the six translational + rotational axes plus
     * a seventh "zoom" channel (mirrors static sDelta[7]).
     */
    private val delta:     FloatArray = FloatArray(7)
    private val lastDelta: FloatArray = FloatArray(7)

    /** Performance scale factor applied to raw axis values. */
    private var perfScale: Float = 1.0f / MAX_JOYSTICK_INPUT_VALUE

    /**
     * When true the current move cycle should be reset to zero.
     * Mirrors LLViewerJoystick::mResetFlag.
     */
    private var resetFlag: Boolean = false

    /** When true the camera-needs-update flag is set. */
    private var cameraUpdated: Boolean = false

    /** When true the flycam override is active. */
    private var overrideCamera: Boolean = false

    /** Joystick "run" state (0 = walk, 1 = run). */
    private var joystickRun: UInt = 0u

    /**
     * OS-specific device UUID stored as an opaque LLSD blob.
     * Windows: _GUID bytes; macOS: long bytes; other: integer 1.
     */
    private var lastDeviceUUID: Any? = null

    // -----------------------------------------------------------------------
    // Initialisation / shutdown
    // -----------------------------------------------------------------------

    /**
     * Initialise the NDOF library and enumerate attached devices.
     * Mirrors LLViewerJoystick::init(autoenable).
     *
     * @param autoEnable if true, enable the device immediately on detection.
     * @return true if a usable device was found and initialised.
     */
    fun init(autoEnable: Boolean = true): Boolean {
        driverState = DriverState.INITIALIZING
        System.err.println("ViewerJoystick: init not yet implemented")
        return false
    }

    /**
     * Initialise a specific device identified by its platform GUID/UUID.
     * Mirrors LLViewerJoystick::initDevice(LLSD&).
     */
    fun initDevice(guid: Any?): Boolean {
        System.err.println("ViewerJoystick: initDevice not yet implemented")
        return false
    }

    /**
     * Shut down the NDOF library and release the device.
     * Mirrors LLViewerJoystick::terminate().
     */
    fun terminate() {
        driverState = DriverState.UNINITIALIZED
        System.err.println("ViewerJoystick: terminate not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Per-frame update
    // -----------------------------------------------------------------------

    /**
     * Poll the device for new axis and button values.
     * Mirrors LLViewerJoystick::updateStatus().
     */
    fun updateStatus() {
        if (!isJoystickInitialized) return
        System.err.println("ViewerJoystick: updateStatus not yet implemented")
    }

    /**
     * Scan attached joystick devices (hot-plug support).
     * Mirrors LLViewerJoystick::scanJoystick().
     */
    fun scanJoystick() {
        System.err.println("ViewerJoystick: scanJoystick not yet implemented")
    }

    /**
     * Translate current axis deltas into avatar movement commands.
     * Should be called once per render frame.
     * Mirrors LLViewerJoystick::moveAvatar(reset).
     *
     * @param dt  frame delta-time in seconds
     */
    fun moveAvatar(dt: Float) {
        if (!isJoystickInitialized) return
        System.err.println("ViewerJoystick: moveAvatar not yet implemented")
    }

    /**
     * Translate current axis deltas into flycam movement commands.
     * Should be called once per render frame when flycam is active.
     * Mirrors LLViewerJoystick::moveFlycam(reset).
     *
     * @param dt  frame delta-time in seconds
     */
    fun moveFlycam(dt: Float) {
        if (!isJoystickInitialized) return
        System.err.println("ViewerJoystick: moveFlycam not yet implemented")
    }

    /**
     * Apply movement to all selected objects.
     * Mirrors LLViewerJoystick::moveObjects(reset).
     */
    fun moveObjects(reset: Boolean = false) {
        if (!isJoystickInitialized) return
        System.err.println("ViewerJoystick: moveObjects not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Axis / button accessors
    // -----------------------------------------------------------------------

    /**
     * Return the raw value of an axis (normalised to [-1.0, 1.0]).
     * Mirrors LLViewerJoystick::getJoystickAxis().
     *
     * @param axis  axis index in [0, MAX_JOYSTICK_AXES)
     */
    fun getJoystickAxis(axis: UInt): Float {
        val i = axis.toInt()
        if (i !in axes.indices) return 0f
        return axes[i]
    }

    /**
     * Return the state of a button (0 = released, non-zero = pressed).
     * Mirrors LLViewerJoystick::getJoystickButton().
     *
     * @param button  button index in [0, MAX_JOYSTICK_BUTTONS)
     */
    fun getJoystickButton(button: UInt): UInt {
        val i = button.toInt()
        if (i !in buttons.indices) return 0u
        return buttons[i].toUInt()
    }

    /** Number of physical axes the current device exposes. */
    fun getNumOfJoystickAxes(): UInt {
        System.err.println("ViewerJoystick: getNumOfJoystickAxes not yet implemented")
        return 0u
    }

    /** Number of physical buttons the current device exposes. */
    fun getNumOfJoystickButtons(): UInt {
        System.err.println("ViewerJoystick: getNumOfJoystickButtons not yet implemented")
        return 0u
    }

    // -----------------------------------------------------------------------
    // SpaceNavigator helpers
    // -----------------------------------------------------------------------

    /**
     * True if the connected device looks like a 3Dconnexion SpaceNavigator.
     * Mirrors LLViewerJoystick::isLikeSpaceNavigator().
     */
    fun isLikeSpaceNavigator(): Boolean {
        System.err.println("ViewerJoystick: isLikeSpaceNavigator not yet implemented")
        return false
    }

    /**
     * Apply the recommended SpaceNavigator default axis/sensitivity settings.
     * Mirrors LLViewerJoystick::setSNDefaults().
     */
    fun setSNDefaults() {
        System.err.println("ViewerJoystick: setSNDefaults not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Camera / override
    // -----------------------------------------------------------------------

    fun getCameraNeedsUpdate(): Boolean = cameraUpdated
    fun setCameraNeedsUpdate(b: Boolean) { cameraUpdated = b }

    fun getOverrideCamera(): Boolean = overrideCamera
    fun setOverrideCamera(value: Boolean) {
        overrideCamera = value
        System.err.println("ViewerJoystick: setOverrideCamera not yet implemented")
    }

    /**
     * Toggle the flycam override on/off.
     * Mirrors LLViewerJoystick::toggleFlycam().
     */
    fun toggleFlycam(): Boolean {
        setOverrideCamera(!overrideCamera)
        return overrideCamera
    }

    fun setNeedsReset(reset: Boolean = true) { resetFlag = reset }

    // -----------------------------------------------------------------------
    // Device UUID persistence
    // -----------------------------------------------------------------------

    fun isDeviceUUIDSet(): Boolean = lastDeviceUUID != null

    fun getDeviceUUID(): Any? = lastDeviceUUID

    fun getDeviceUUIDString(): String {
        System.err.println("ViewerJoystick: getDeviceUUIDString not yet implemented")
        return ""
    }

    fun getDescription(): String {
        System.err.println("ViewerJoystick: getDescription not yet implemented")
        return ""
    }

    fun saveDeviceIdToSettings() {
        System.err.println("ViewerJoystick: saveDeviceIdToSettings not yet implemented")
    }
}
