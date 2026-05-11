// Converted from llheadrotmotion.h / llheadrotmotion.cpp — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3
import kotlin.math.atan2
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const val MIN_REQUIRED_PIXEL_AREA_HEAD_ROT: Float = 500f
const val MIN_REQUIRED_PIXEL_AREA_EYE: Float = 25000f

private const val TORSO_LAG: Float  = 0.35f   // torso rotation factor
private const val NECK_LAG: Float   = 0.5f    // neck rotation factor
private const val HEAD_LOOKAT_LAG_HALF_LIFE: Float  = 0.15f  // head look-at smoothing half-life
private const val TORSO_LOOKAT_LAG_HALF_LIFE: Float = 0.27f  // torso look-at smoothing half-life
private const val HEAD_ROTATION_CONSTRAINT: Float   = (Math.PI / 2 * 0.8).toFloat()
private const val MIN_HEAD_LOOKAT_DISTANCE: Float   = 0.3f

// Eye jitter / look-away parameters
private const val EYE_JITTER_MIN_TIME: Float    = 0.3f
private const val EYE_JITTER_MAX_TIME: Float    = 2.5f
private const val EYE_JITTER_MAX_YAW: Float     = 0.08f
private const val EYE_JITTER_MAX_PITCH: Float   = 0.015f
private const val EYE_LOOK_AWAY_MIN_TIME: Float = 5f
private const val EYE_LOOK_AWAY_MAX_TIME: Float = 15f
private const val EYE_LOOK_BACK_MIN_TIME: Float = 1f
private const val EYE_LOOK_BACK_MAX_TIME: Float = 5f
private const val EYE_LOOK_AWAY_MAX_YAW: Float  = 0.15f
private const val EYE_LOOK_AWAY_MAX_PITCH: Float = 0.12f
private const val EYE_ROT_LIMIT_ANGLE: Float    = (Math.PI / 2 * 0.3).toFloat()

// Eye blink parameters
private const val EYE_BLINK_MIN_TIME: Float     = 0.5f
private const val EYE_BLINK_MAX_TIME: Float     = 8f
private const val EYE_BLINK_CLOSE_TIME: Float   = 0.03f
private const val EYE_BLINK_SPEED: Float        = 0.015f
private const val EYE_BLINK_TIME_DELTA: Float   = 0.005f

// ---------------------------------------------------------------------------
// HeadRotMotion
//
// Smoothly rotates the neck and head to track the character's look-at target.
// Mirrors C++ LLHeadRotMotion.
//
// The nlerp/slerp quaternion operations and the HEAD_ROTATION_CONSTRAINT clamp
// are stubbed with TODO() because the math helpers are not yet ported.
// ---------------------------------------------------------------------------

class HeadRotMotion(id: LLUUID) : LLMotion(id) {

    // ---- joint references --------------------------------------------------

    private var torsoJoint:  Joint? = null
    private var headJoint:   Joint? = null
    private var rootJoint:   Joint? = null
    private var pelvisJoint: Joint? = null

    private val torsoState = JointState()
    private val neckState  = JointState()
    private val headState  = JointState()

    // ---- runtime state -----------------------------------------------------

    private var character: LLCharacter? = null
    private var lastHeadRot: Quaternion = Quaternion()   // identity

    companion object {
        fun create(id: LLUUID): HeadRotMotion = HeadRotMotion(id)
    }

    init {
        name = "head_rot"
    }

    // ---- LLMotion overrides ------------------------------------------------

    override fun getLoop(): Boolean = true
    override fun getDuration(): Float = 0f
    override fun getEaseInDuration(): Float = 1f
    override fun getEaseOutDuration(): Float = 1f
    override fun getPriority(): JointPriority = JointPriority.MEDIUM
    override fun getBlendType(): MotionBlendType = MotionBlendType.NORMAL_BLEND
    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_HEAD_ROT

    override fun onInitialize(character: Character): MotionInitStatus {
        val ch = character as? LLCharacter ?: return MotionInitStatus.FAILURE
        this.character = ch

        pelvisJoint = ch.getJoint("mPelvis") ?: run {
            System.err.println("HeadRotMotion: can't get pelvis joint"); return MotionInitStatus.FAILURE
        }
        rootJoint = ch.getJoint("mRoot") ?: run {
            System.err.println("HeadRotMotion: can't get root joint"); return MotionInitStatus.FAILURE
        }
        torsoJoint = ch.getJoint("mTorso") ?: run {
            System.err.println("HeadRotMotion: can't get torso joint"); return MotionInitStatus.FAILURE
        }
        headJoint = ch.getJoint("mHead") ?: run {
            System.err.println("HeadRotMotion: can't get head joint"); return MotionInitStatus.FAILURE
        }

        torsoState.setJoint(torsoJoint)
        neckState.setJoint(ch.getJoint("mNeck") ?: run {
            System.err.println("HeadRotMotion: can't get neck joint"); return MotionInitStatus.FAILURE
        })
        headState.setJoint(headJoint)

        torsoState.usage = JointState.Usage.ROT.mask
        neckState.usage  = JointState.Usage.ROT.mask
        headState.usage  = JointState.Usage.ROT.mask

        lastHeadRot = Quaternion()   // identity

        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean = true

    override fun onUpdate(activeTime: Float): Boolean {
        val ch = character ?: return false

        val currentRootRotWorld = rootJoint?.getWorldRotation() ?: Quaternion()

        val targetPos = ch.getAnimationData("LookAtPoint") as? Vector3

        val targetHeadRotWorld: Quaternion = if (targetPos != null) {
            val headLookAt = targetPos
            val lookatDist = headLookAt.length()

            if (lookatDist < MIN_HEAD_LOOKAT_DISTANCE) {
                pelvisJoint?.getWorldRotation() ?: currentRootRotWorld
            } else {
                // Build a rotation from the look-at vector toward the target.
                // Use a simplified approach: derive at/left/up from the look-at direction.
                val at = headLookAt * (1f / lookatDist)
                val worldUp = Vector3(0f, 0f, 1f)
                val left = (worldUp % at).let { l ->
                    val len = l.length(); if (len > 0.0001f) l * (1f / len) else Vector3(1f, 0f, 0f)
                }
                val up = at % left
                // Build quaternion from rotation matrix columns [at, left, up]
                // Using the standard matrix-to-quaternion conversion
                val m00 = left.x; val m01 = left.y; val m02 = left.z
                val m10 = up.x;   val m11 = up.y;   val m12 = up.z
                val m20 = at.x;   val m21 = at.y;   val m22 = at.z
                val trace = m00 + m11 + m22
                val q = if (trace > 0f) {
                    val s = 0.5f / kotlin.math.sqrt(trace + 1f)
                    Quaternion((m21 - m12) * s, (m02 - m20) * s, (m10 - m01) * s, 0.25f / s)
                } else {
                    Quaternion(0f, 0f, 0f, 1f)
                }
                q.normalize()
                q
            }
        } else {
            currentRootRotWorld
        }

        // TODO: implement nlerp/slerp for torso lag, neck lag, head slerp smoothing:
        //   val headRotLocal = (targetHeadRotWorld * ~currentRootRotWorld).constrain(HEAD_ROTATION_CONSTRAINT)
        //   val torsoRotLocal = nlerp(TORSO_LAG, Quaternion.IDENTITY, headRotLocal)
        //   torsoState.rotation = nlerp(torsoSlerp, torsoState.rotation, torsoRotLocal)
        //   headRotLocal = nlerp(headSlerp, lastHeadRot, headRotLocal)
        //   lastHeadRot = headRotLocal
        //   val neckParentRot = neckState.joint?.parent?.getWorldRotation() * ~currentRootRotWorld
        //   val headRotAdj = headRotLocal * ~neckParentRot
        //   neckState.rotation = nlerp(NECK_LAG, Quaternion.IDENTITY, headRotAdj)
        //   headState.rotation = nlerp(1f - NECK_LAG, Quaternion.IDENTITY, headRotAdj)
        TODO("Apply head/neck/torso rotation via nlerp smoothing (requires Quaternion.nlerp / .constrain helpers)")
    }

    override fun onDeactivate() {
        // nothing to clean up
    }
}

// ---------------------------------------------------------------------------
// EyeMotion
//
// Drives eye-ball joint rotations with look-at targeting, convergence
// (vergence), procedural jitter, look-away saccades, and blinking.
// Mirrors C++ LLEyeMotion.
//
// The trigonometric eye-rotation math is stubbed with TODO(); all state
// machine logic (jitter timing, blink timing) is fully ported.
// ---------------------------------------------------------------------------

class EyeMotion(id: LLUUID) : LLMotion(id) {

    // ---- joint states ------------------------------------------------------

    private val leftEyeState     = JointState()
    private val rightEyeState    = JointState()
    private val altLeftEyeState  = JointState()
    private val altRightEyeState = JointState()

    // ---- runtime state -----------------------------------------------------

    private var character: LLCharacter? = null
    private var headJoint: Joint? = null

    // jitter
    private var eyeJitterTime:  Float = 0f
    private var eyeJitterYaw:   Float = 0f
    private var eyeJitterPitch: Float = 0f
    private var eyeJitterElapsed: Float = 0f  // simulated FrameTimer

    // look-away
    private var eyeLookAwayTime:  Float = 0f
    private var eyeLookAwayYaw:   Float = 0f
    private var eyeLookAwayPitch: Float = 0f

    // blink
    private var eyeBlinkElapsed: Float = 0f   // simulated FrameTimer
    private var eyeBlinkTime:    Float = EYE_BLINK_MIN_TIME + Random.nextFloat() * (EYE_BLINK_MAX_TIME - EYE_BLINK_MIN_TIME)
    private var eyesClosed:      Boolean = false

    companion object {
        fun create(id: LLUUID): EyeMotion = EyeMotion(id)
    }

    init {
        name = "eye_rot"
    }

    // ---- LLMotion overrides ------------------------------------------------

    override fun getLoop(): Boolean = true
    override fun getDuration(): Float = 0f
    override fun getEaseInDuration(): Float = 0.5f
    override fun getEaseOutDuration(): Float = 0.5f
    override fun getPriority(): JointPriority = JointPriority.MEDIUM
    override fun getBlendType(): MotionBlendType = MotionBlendType.NORMAL_BLEND
    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_EYE

    override fun onInitialize(character: Character): MotionInitStatus {
        val ch = character as? LLCharacter ?: return MotionInitStatus.FAILURE
        this.character = ch

        headJoint = ch.getJoint("mHead") ?: run {
            System.err.println("EyeMotion: can't get head joint"); return MotionInitStatus.FAILURE
        }

        leftEyeState.setJoint(ch.getJoint("mEyeLeft") ?: run {
            System.err.println("EyeMotion: can't get left eye joint"); return MotionInitStatus.FAILURE
        })
        altLeftEyeState.setJoint(ch.getJoint("mFaceEyeAltLeft") ?: run {
            System.err.println("EyeMotion: can't get alt left eye joint"); return MotionInitStatus.FAILURE
        })
        rightEyeState.setJoint(ch.getJoint("mEyeRight") ?: run {
            System.err.println("EyeMotion: can't get right eye joint"); return MotionInitStatus.FAILURE
        })
        altRightEyeState.setJoint(ch.getJoint("mFaceEyeAltRight") ?: run {
            System.err.println("EyeMotion: can't get alt right eye joint"); return MotionInitStatus.FAILURE
        })

        leftEyeState.usage     = JointState.Usage.ROT.mask
        altLeftEyeState.usage  = JointState.Usage.ROT.mask
        rightEyeState.usage    = JointState.Usage.ROT.mask
        altRightEyeState.usage = JointState.Usage.ROT.mask

        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean = true

    override fun onUpdate(activeTime: Float): Boolean {
        val ch = character ?: return false

        // --- update jitter timer (simulated elapsed; caller supplies wall time) ---
        eyeJitterElapsed += activeTime - (lastActiveTime ?: activeTime)
        eyeBlinkElapsed  += activeTime - (lastActiveTime ?: activeTime)
        lastActiveTime = activeTime

        // --- compute jitter ---
        if (eyeJitterElapsed > eyeJitterTime) {
            eyeJitterTime = EYE_JITTER_MIN_TIME +
                    Random.nextFloat() * (EYE_JITTER_MAX_TIME - EYE_JITTER_MIN_TIME)
            eyeJitterYaw   = (Random.nextFloat() * 2f - 1f) * EYE_JITTER_MAX_YAW
            eyeJitterPitch = (Random.nextFloat() * 2f - 1f) * EYE_JITTER_MAX_PITCH
            eyeLookAwayTime -= eyeJitterElapsed
            eyeJitterElapsed = 0f
        } else if (eyeJitterElapsed > eyeLookAwayTime) {
            if (Random.nextFloat() > 0.1f) {
                // trigger blink on look-away
                eyeBlinkTime = eyeBlinkElapsed
            }
            if (eyeLookAwayYaw == 0f && eyeLookAwayPitch == 0f) {
                eyeLookAwayYaw   = (Random.nextFloat() * 2f - 1f) * EYE_LOOK_AWAY_MAX_YAW
                eyeLookAwayPitch = (Random.nextFloat() * 2f - 1f) * EYE_LOOK_AWAY_MAX_PITCH
                eyeLookAwayTime  = EYE_LOOK_BACK_MIN_TIME +
                        Random.nextFloat() * (EYE_LOOK_BACK_MAX_TIME - EYE_LOOK_BACK_MIN_TIME)
            } else {
                eyeLookAwayYaw   = 0f
                eyeLookAwayPitch = 0f
                eyeLookAwayTime  = EYE_LOOK_AWAY_MIN_TIME +
                        Random.nextFloat() * (EYE_LOOK_AWAY_MAX_TIME - EYE_LOOK_AWAY_MIN_TIME)
            }
        }

        // --- blink state machine ---
        if (!eyesClosed && eyeBlinkElapsed >= eyeBlinkTime) {
            val leftBlink  = ((eyeBlinkElapsed - eyeBlinkTime) / EYE_BLINK_SPEED).coerceIn(0f, 1f)
            val rightBlink = ((eyeBlinkElapsed - eyeBlinkTime - EYE_BLINK_TIME_DELTA) / EYE_BLINK_SPEED).coerceIn(0f, 1f)
            ch.setVisualParamWeight("Blink_Left",  leftBlink)
            ch.setVisualParamWeight("Blink_Right", rightBlink)
            ch.updateVisualParams()
            if (rightBlink == 1f) {
                eyesClosed   = true
                eyeBlinkTime = EYE_BLINK_CLOSE_TIME
                eyeBlinkElapsed = 0f
            }
        } else if (eyesClosed && eyeBlinkElapsed >= eyeBlinkTime) {
            val leftBlink  = 1f - ((eyeBlinkElapsed - eyeBlinkTime) / EYE_BLINK_SPEED).coerceIn(0f, 1f)
            val rightBlink = 1f - ((eyeBlinkElapsed - eyeBlinkTime - EYE_BLINK_TIME_DELTA) / EYE_BLINK_SPEED).coerceIn(0f, 1f)
            ch.setVisualParamWeight("Blink_Left",  leftBlink)
            ch.setVisualParamWeight("Blink_Right", rightBlink)
            ch.updateVisualParams()
            if (rightBlink == 0f) {
                eyesClosed   = false
                eyeBlinkTime = EYE_BLINK_MIN_TIME +
                        Random.nextFloat() * (EYE_BLINK_MAX_TIME - EYE_BLINK_MIN_TIME)
                eyeBlinkElapsed = 0f
            }
        }

        val targetPos = ch.getAnimationData("LookAtPoint") as? Vector3

        adjustEyeTarget(targetPos, leftEyeState, rightEyeState)
        adjustEyeTarget(targetPos, altLeftEyeState, altRightEyeState)

        return true
    }

    override fun onDeactivate() {
        // Reset all eye joints to identity rotation
        listOf(leftEyeState, altLeftEyeState, rightEyeState, altRightEyeState).forEach { state ->
            state.joint?.rotation = Quaternion()
        }
    }

    // ---- private helpers ---------------------------------------------------

    /** Simulated elapsed time tracking between onUpdate calls. */
    private var lastActiveTime: Float? = null

    /**
     * Compute and apply eye rotations for one pair of eye joints.
     * Mirrors C++ LLEyeMotion::adjustEyeTarget().
     *
     * TODO: Implement full vergence + jitter quaternion math when
     *       Quaternion.getEulerAngles / .setQuat / .constrain helpers are available.
     */
    private fun adjustEyeTarget(
        targetPos: Vector3?,
        leftState: JointState,
        rightState: JointState
    ) {
        // TODO: build target_eye_rot from targetPos (if non-null):
        //   1. normalise targetPos → eye_look_at, compute left/up basis
        //   2. build LLQuaternion(eye_look_at, left, up)
        //   3. convert to head-local coords: *= ~headJoint.getWorldRotation()
        //   4. zero out roll via getEulerAngles / setQuat(0, pitch, yaw)
        //   5. constrain to EYE_ROT_LIMIT_ANGLE
        //   6. compute vergence = -atan2(interocularDist/2, lookAtDist); clamp; add 4*DEG_TO_RAD
        //   7. build eye_jitter_rot from mEyeJitterPitch/Yaw + mEyeLookAwayPitch/Yaw
        //   8. build vergence_quat; apply left/right symmetrically (transQuat for right)
        //   9. write leftState.rotation and rightState.rotation
        TODO("Compute eye vergence + jitter rotations for left/right eye pair")
    }
}

// Convenience extension (same as in HandMotion.kt — each file is self-contained)
private fun LLCharacter.getVisualParamWeight(name: String): Float =
    getVisualParam(name)?.weight ?: 0f
