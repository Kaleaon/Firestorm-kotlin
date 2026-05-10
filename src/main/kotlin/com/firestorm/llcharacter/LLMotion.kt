// Converted from llmotion.h — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Blend type and init-status enums (mirror LLMotionBlendType / LLMotionInitStatus)
// ---------------------------------------------------------------------------

enum class MotionBlendType { NORMAL_BLEND, ADDITIVE_BLEND }

enum class MotionInitStatus { FAILURE, SUCCESS, HOLD }

// ---------------------------------------------------------------------------
// abstract class LLMotion
//
// Base class for all character animations. Mirrors C++ LLMotion.
// The MotionController drives the activate/update/deactivate lifecycle;
// subclasses implement the on* callbacks.
// ---------------------------------------------------------------------------

abstract class LLMotion(val id: LLUUID) {

    // ---- identity ----------------------------------------------------------

    var name: String = ""

    // ---- lifecycle state ---------------------------------------------------

    /** True while this motion is on the controller's active list. */
    var isActive: Boolean = false
        internal set

    /** True after the controller has told this motion to stop. */
    var isStopped: Boolean = false

    // ---- timing fields set by the controller ------------------------------

    var activationTimestamp: Float = 0f
        internal set
    var stopTimestamp: Float = 0f
        internal set
    /** Time at which the simulator should be told to stop this motion. */
    var sendStopTimestamp: Float = 0f
        internal set
    /** Blend weight recorded at the start of the ease-out phase. */
    var residualWeight: Float = 0f
        internal set
    /** LOD-based fade weight; 1 = fully visible, 0 = invisible. */
    var fadeWeight: Float = 1f
        internal set

    // ---- abstract animation-callback interface ----------------------------

    abstract fun getLoop(): Boolean
    abstract fun getDuration(): Float
    abstract fun getEaseInDuration(): Float
    abstract fun getEaseOutDuration(): Float
    abstract fun getPriority(): JointPriority
    abstract fun getBlendType(): MotionBlendType
    abstract fun getMinPixelArea(): Float
    open fun getNumJointMotions(): Int = 0

    /**
     * Run-time (post-constructor) initialisation; called once before the
     * motion can be activated.  Must return [MotionInitStatus.SUCCESS] for
     * the motion to become available, or [MotionInitStatus.HOLD] while an
     * async asset is loading.
     */
    abstract fun onInitialize(character: Character): MotionInitStatus

    /** Called once when the motion becomes active; return false to abort. */
    protected abstract fun onActivate(): Boolean

    /**
     * Per-frame update.  [activeTime] is wall-clock seconds since activation.
     * Return true while the motion is still running, false when complete.
     */
    abstract fun onUpdate(activeTime: Float): Boolean

    /** Called when the motion is fully deactivated. */
    abstract fun onDeactivate()

    /** Override to prevent crossfade with a new instance (default: allowed). */
    open fun canDeprecate(): Boolean = true

    // ---- lifecycle helpers called by MotionController ---------------------

    open fun setStopTime(time: Float) {
        stopTimestamp = time
        isStopped = true
    }

    /** Activate this motion at the given [time]; used by [MotionController]. */
    fun activate(time: Float) {
        activationTimestamp = time
        isStopped = false
        isActive = onActivate()
    }

    /** Deactivate; only [MotionController] should call this. */
    internal fun deactivate() {
        isActive = false
        onDeactivate()
    }

    /** True while the motion is blending in or out (fade weight between 0 and 1). */
    fun isBlending(): Boolean = fadeWeight in 0f..1f && fadeWeight != 1f

    fun fadeOut() { /* TODO: step fadeWeight toward 0 over easeOutDuration */ }
    fun fadeIn()  { /* TODO: step fadeWeight toward 1 over easeInDuration  */ }
}

// ---------------------------------------------------------------------------
// NullMotion — no-op motion that loops forever and always succeeds
// ---------------------------------------------------------------------------

/** Mirrors C++ LLNullMotion. */
class NullMotion(id: LLUUID) : LLMotion(id) {
    override fun getLoop(): Boolean = true
    override fun getDuration(): Float = 1f
    override fun getEaseInDuration(): Float = 0f
    override fun getEaseOutDuration(): Float = 0f
    override fun getPriority(): JointPriority = JointPriority.HIGH
    override fun getBlendType(): MotionBlendType = MotionBlendType.NORMAL_BLEND
    override fun getMinPixelArea(): Float = 0f
    override fun onInitialize(character: Character): MotionInitStatus = MotionInitStatus.SUCCESS
    override fun onActivate(): Boolean = true
    override fun onUpdate(activeTime: Float): Boolean = true
    override fun onDeactivate() {}
}
