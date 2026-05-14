// Converted from llkeyframewalkmotion.h / llkeyframewalkmotion.cpp — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3
import kotlin.math.abs
import kotlin.math.atan2

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const val MIN_REQUIRED_PIXEL_AREA_WALK_ADJUST: Float = 20f
const val MIN_REQUIRED_PIXEL_AREA_FLY_ADJUST:  Float = 20f

private const val MAX_WALK_PLAYBACK_SPEED: Float  = 8f      // m/s cap for walk cycle speed
private const val MIN_WALK_SPEED: Float           = 0.1f    // below this → standing/turning
private const val TIME_EPSILON: Float             = 0.001f  // minimum frame time
private const val MAX_TIME_DELTA: Float           = 2f      // maximum frame delta
private var SPEED_ADJUST_MAX_SEC: Float           = 2f      // max speed adjustment per second
private var ANIM_SPEED_MAX: Float                 = 1.5f    // absolute animation speed cap
private const val MAX_ROLL: Float                 = 0.6f    // max fly-roll in radians
private const val SPEED_ADJUST_TIME_CONSTANT: Float = 0.1f // speed adjustment interpolation TC

// ---------------------------------------------------------------------------
// KeyframeWalkMotion
//
// Extends KeyframeMotion with speed-adaptive playback: the walk animation is
// driven at a rate proportional to the avatar's actual movement speed so that
// feet appear not to slip.
// Mirrors C++ LLKeyframeWalkMotion.
// ---------------------------------------------------------------------------

class KeyframeWalkMotion(id: LLUUID) : KeyframeMotion(id) {

    private var character:    LLCharacter? = null
    var cyclePhase:           Float = 0f
    private var realTimeLast: Float = 0f
    private var adjTimeLast:  Float = 0f
    var downFoot:             Int   = 0

    companion object {
        fun create(id: LLUUID): KeyframeWalkMotion = KeyframeWalkMotion(id)
    }

    // ---- LLMotion / KeyframeMotion overrides --------------------------------

    override fun onInitialize(character: Character): MotionInitStatus {
        this.character = character as? LLCharacter ?: return MotionInitStatus.FAILURE
        return super.onInitialize(character)
    }

    override fun onActivate(): Boolean {
        realTimeLast = 0f
        adjTimeLast  = 0f
        return super.onActivate()
    }

    override fun onDeactivate() {
        character?.removeAnimationData("Down Foot")
        super.onDeactivate()
    }

    override fun onUpdate(activeTime: Float): Boolean {
        val ch = character ?: return false

        val deltaTime = activeTime - realTimeLast

        // Read the walk-speed multiplier broadcast by WalkAdjustMotion
        val speed = (ch.getAnimationData("Walk Speed") as? Float) ?: 1f

        val adjustedTime = adjTimeLast + deltaTime * speed

        realTimeLast = activeTime
        adjTimeLast  = adjustedTime

        // Handle wrap-around for negative adjusted time
        val clampedTime = if (adjustedTime < 0f) {
            val dur = getDuration()
            if (dur > 0f) dur + (adjustedTime % dur) else 0f
        } else {
            adjustedTime
        }

        return super.onUpdate(clampedTime)
    }
}

// ---------------------------------------------------------------------------
// WalkAdjustMotion
//
// Computes the optimal animation-playback speed multiplier ("Walk Speed") so
// that the walk cycle's foot placement matches the avatar's actual velocity.
// Also compensates via a small pelvis offset.
// Mirrors C++ LLWalkAdjustMotion.
//
// Global-coordinate foot tracking (LLVector3d / region crossing) is stubbed
// with TODO() because the double-precision coordinate type is not yet ported.
// The speed-adjustment interpolation math is also stubbed.
// ---------------------------------------------------------------------------

class WalkAdjustMotion(id: LLUUID) : LLMotion(id) {

    private val pelvisState = JointState()

    private var character:        LLCharacter? = null
    private var leftAnkleJoint:   Joint? = null
    private var rightAnkleJoint:  Joint? = null
    private var pelvisJoint:      Joint? = null

    // foot tracking (global coords — DoubleArray stands in for LLVector3d)
    private var lastLeftFootGlobalPos:  DoubleArray = DoubleArray(3)
    private var lastRightFootGlobalPos: DoubleArray = DoubleArray(3)

    private var lastTime:      Float = 0f
    private var adjustedSpeed: Float = 0f
    private var animSpeed:     Float = 0f
    private var relativeDir:   Float = 0f
    private var pelvisOffset:  Vector3 = Vector3()
    private var ankleOffset:   Float = 0f

    companion object {
        fun create(id: LLUUID): WalkAdjustMotion = WalkAdjustMotion(id)
    }

    init {
        name = "walk_adjust"
    }

    // ---- LLMotion overrides ------------------------------------------------

    override fun getLoop(): Boolean = true
    override fun getDuration(): Float = 0f
    override fun getEaseInDuration(): Float = 0f
    override fun getEaseOutDuration(): Float = 0f
    override fun getPriority(): JointPriority = JointPriority.HIGH
    override fun getBlendType(): MotionBlendType = MotionBlendType.ADDITIVE_BLEND
    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_WALK_ADJUST

    override fun onInitialize(character: Character): MotionInitStatus {
        val ch = character as? LLCharacter ?: return MotionInitStatus.FAILURE
        this.character = ch

        leftAnkleJoint  = ch.getJoint("mAnkleLeft")
        rightAnkleJoint = ch.getJoint("mAnkleRight")
        pelvisJoint     = ch.getJoint("mPelvis") ?: run {
            System.err.println("WalkAdjustMotion: can't get pelvis joint")
            return MotionInitStatus.FAILURE
        }

        pelvisState.setJoint(pelvisJoint)
        pelvisState.usage = JointState.Usage.POS.mask

        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean {
        val ch = character ?: return false

        animSpeed    = 0f
        adjustedSpeed = 0f
        relativeDir  = 1f
        pelvisState.position = Vector3()

        // Store current ankle global positions (Z zeroed — ground plane)
        val leftAnkle  = leftAnkleJoint?.getWorldPosition()  ?: Vector3()
        val rightAnkle = rightAnkleJoint?.getWorldPosition() ?: Vector3()

        lastLeftFootGlobalPos  = ch.getPosGlobalFromAgent(leftAnkle).also { it[2] = 0.0 }
        lastRightFootGlobalPos = ch.getPosGlobalFromAgent(rightAnkle).also { it[2] = 0.0 }

        val leftOffset  = (leftAnkle  - ch.getCharacterPosition()).length()
        val rightOffset = (rightAnkle - ch.getCharacterPosition()).length()
        ankleOffset = maxOf(leftOffset, rightOffset)

        return true
    }

    override fun onUpdate(activeTime: Float): Boolean {
        val ch = character ?: return true

        val deltaTime = (activeTime - lastTime).coerceIn(TIME_EPSILON, MAX_TIME_DELTA)
        lastTime = activeTime

        val avatarVelocity = (ch.getCharacterVelocity() * ch.getTimeDilation())
            .let { v -> Vector3(v.x, v.y, 0f) }                    // zero Z
        val speed = avatarVelocity.length().coerceIn(0f, MAX_WALK_PLAYBACK_SPEED)

        if (speed > MIN_WALK_SPEED) {
            // TODO: implement global-coordinate foot delta tracking:
            //   val leftGlobal  = ch.getPosGlobalFromAgent(leftAnkleJoint!!.getWorldPosition()).zeroZ()
            //   val leftDelta   = leftGlobal - lastLeftFootGlobalPos
            //   lastLeftFootGlobalPos = leftGlobal
            //   (same for right)
            //
            //   Pick the foot that is sliding most against velocity direction.
            //   F32 foot_speed = speed - (footSlipVector dot avatarMovDir) / deltaTime
            //   if foot_speed < 0 → clamp to 0
            //
            //   desired_speed_multiplier = clamp(speed / foot_speed, minMult, ANIM_SPEED_MAX)
            //   new_speed_adjust = lerp(adjustedSpeed, desired, SPEED_ADJUST_TIME_CONSTANT)
            //   speedDelta = clamp(delta, -SPEED_ADJUST_MAX_SEC*dt, SPEED_ADJUST_MAX_SEC*dt)
            //   adjustedSpeed += speedDelta
            //
            //   directional_factor = (avatarMovDir rotated into avatar space).x
            //   animSpeed = adjustedSpeed * directional_factor
            System.err.println("WalkAdjustMotion: onUpdate not yet implemented")
        } else {
            // Standing/turning: damp animation speed back toward 1
            // TODO: animSpeed = lerp(animSpeed, 1f, 0.2f) via SmoothInterpolation
            animSpeed = animSpeed + (1f - animSpeed) * 0.2f   // approximate first-order damp
        }

        ch.setAnimationData("Walk Speed", animSpeed)

        // Move pelvis to compensate for any residual foot drift
        pelvisState.position = pelvisOffset

        return true
    }

    override fun onDeactivate() {
        character?.removeAnimationData("Walk Speed")
    }
}

// ---------------------------------------------------------------------------
// FlyAdjustMotion
//
// Rolls the pelvis when flying fast, banking into turns like an aircraft.
// Mirrors C++ LLFlyAdjustMotion.
//
// The critically-damped roll interpolation is stubbed with TODO().
// ---------------------------------------------------------------------------

class FlyAdjustMotion(id: LLUUID) : LLMotion(id) {

    private val pelvisState = JointState()
    private var character:  LLCharacter? = null
    private var roll: Float = 0f

    companion object {
        fun create(id: LLUUID): FlyAdjustMotion = FlyAdjustMotion(id)
    }

    init {
        name = "fly_adjust"
    }

    // ---- LLMotion overrides ------------------------------------------------

    override fun getLoop(): Boolean = true
    override fun getDuration(): Float = 0f
    override fun getEaseInDuration(): Float = 0f
    override fun getEaseOutDuration(): Float = 0f
    override fun getPriority(): JointPriority = JointPriority.HIGHER
    override fun getBlendType(): MotionBlendType = MotionBlendType.ADDITIVE_BLEND
    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_FLY_ADJUST

    override fun onInitialize(character: Character): MotionInitStatus {
        val ch = character as? LLCharacter ?: return MotionInitStatus.FAILURE
        this.character = ch

        val pelvisJoint = ch.getJoint("mPelvis") ?: run {
            System.err.println("FlyAdjustMotion: can't get pelvis joint")
            return MotionInitStatus.FAILURE
        }

        pelvisState.setJoint(pelvisJoint)
        pelvisState.usage = JointState.Usage.POS.mask or JointState.Usage.ROT.mask

        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean {
        pelvisState.position = Vector3()
        pelvisState.rotation = Quaternion()
        roll = 0f
        return true
    }

    override fun onUpdate(activeTime: Float): Boolean {
        val ch = character ?: return false

        val angVel = ch.getCharacterAngularVelocity() * ch.getTimeDilation()
        val speed  = ch.getCharacterVelocity().length()

        // roll_factor: 0 below 7 m/s, -MAX_ROLL at 15 m/s
        val rollFactor = clampRescale(speed, 7f, 15f, 0f, -MAX_ROLL)
        val targetRoll = angVel.z.coerceIn(-4f, 4f) * rollFactor

        // TODO: roll = SmoothInterpolation.lerp(roll, targetRoll, 100ms)
        //       requires the critically-damped interpolation helper
        roll += (targetRoll - roll) * 0.1f   // approximate first-order damp

        // TODO: pelvisState.rotation = Quaternion(roll, Vector3(0f, 0f, 1f))
        //       requires Quaternion.fromAxisAngle
        return false
    }

    override fun onDeactivate() {
        // nothing to clean up
    }
}

// ---------------------------------------------------------------------------
// Utility
// ---------------------------------------------------------------------------

/**
 * Linear rescale from [inLow, inHigh] to [outLow, outHigh], clamped.
 * Mirrors C++ clamp_rescale().
 */
private fun clampRescale(
    value: Float,
    inLow: Float, inHigh: Float,
    outLow: Float, outHigh: Float
): Float {
    if (inHigh <= inLow) return outLow
    val t = ((value - inLow) / (inHigh - inLow)).coerceIn(0f, 1f)
    return outLow + t * (outHigh - outLow)
}

/** Convenience: scale a Vector3 by a Float. */
private operator fun Vector3.times(f: Float): Vector3 = Vector3(x * f, y * f, z * f)
private fun Vector3.length(): Float = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
