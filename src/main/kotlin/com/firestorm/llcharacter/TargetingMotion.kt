// Converted from lltargetingmotion.h / lltargetingmotion.cpp — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const val TARGETING_EASEIN_DURATION: Float  = 0.3f
const val TARGETING_EASEOUT_DURATION: Float = 0.5f
const val MIN_REQUIRED_PIXEL_AREA_TARGETING: Float = 1000f

/** Half-life for torso rotation smoothing toward the weapon aim target. */
private const val TORSO_TARGET_HALF_LIFE: Float = 0.25f

/** Maximum torso rotation constraint (80% of π/2 radians). */
private const val TORSO_ROTATION_CONSTRAINT: Float = (Math.PI / 2 * 0.8).toFloat()

// ---------------------------------------------------------------------------
// TargetingMotion
//
// Additively rotates the torso so that the right hand points toward the
// character's current "LookAtPoint" (weapon-aim target).
// Mirrors C++ LLTargetingMotion.
//
// The quaternion math (nlerp, constrain, frame-of-reference transforms) is
// stubbed with TODO() because the math helpers are not yet ported; all
// joint-state bookkeeping and the motion-lifecycle contract are fully intact.
// ---------------------------------------------------------------------------

class TargetingMotion(id: LLUUID) : LLMotion(id) {

    // ---- joint states / references -----------------------------------------

    private val torsoState = JointState()

    private var character:      LLCharacter? = null
    private var pelvisJoint:    Joint? = null
    private var torsoJoint:     Joint? = null
    private var rightHandJoint: Joint? = null

    companion object {
        fun create(id: LLUUID): TargetingMotion = TargetingMotion(id)
    }

    init {
        name = "targeting"
    }

    // ---- LLMotion overrides ------------------------------------------------

    override fun getLoop(): Boolean = true
    override fun getDuration(): Float = 0f
    override fun getEaseInDuration(): Float = TARGETING_EASEIN_DURATION
    override fun getEaseOutDuration(): Float = TARGETING_EASEOUT_DURATION
    override fun getPriority(): JointPriority = JointPriority.HIGH
    override fun getBlendType(): MotionBlendType = MotionBlendType.ADDITIVE_BLEND
    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_TARGETING

    override fun onInitialize(character: Character): MotionInitStatus {
        val ch = character as? LLCharacter ?: return MotionInitStatus.FAILURE
        this.character = ch

        pelvisJoint    = ch.getJoint("mPelvis")
        torsoJoint     = ch.getJoint("mTorso")
        rightHandJoint = ch.getJoint("mWristRight")

        if (pelvisJoint == null || torsoJoint == null || rightHandJoint == null) {
            System.err.println("TargetingMotion: invalid skeleton — missing pelvis, torso, or right wrist joint")
            return MotionInitStatus.FAILURE
        }

        torsoState.setJoint(torsoJoint)
        torsoState.usage = JointState.Usage.ROT.mask

        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean = true

    override fun onUpdate(activeTime: Float): Boolean {
        val ch = character ?: return true

        val lookAtPoint = ch.getAnimationData("LookAtPoint") as? Vector3
            ?: return true   // nothing to do; stay active

        val target = lookAtPoint.let {
            val len = it.length()
            if (len > 0f) it * (1f / len) else it
        }

        // TODO: implement the full aiming rotation:
        //   val skyward = Vector3(0f, 0f, 1f)
        //   val left  = (skyward cross target).normalized()
        //   val up    = (target cross left).normalized()
        //   val targetAimRot = Quaternion(target, left, up)          // world-space aim
        //
        //   val curTorsoRot = torsoJoint!!.getWorldRotation()
        //
        //   // right-hand "at" vector in world space
        //   val rightHandAt = Quaternion(0f, -1f, 0f) rotated by rightHandJoint!!.getWorldRotation()
        //   val rhLeft  = (skyward cross rightHandAt).normalized()
        //   val rhUp    = (rightHandAt cross rhLeft).normalized()
        //   val rightHandRot = Quaternion(rightHandAt, rhLeft, rhUp)
        //
        //   // additive rotation to align hand with target
        //   var newTorsoRot = (curTorsoRot * ~rightHandRot) * targetAimRot
        //   newTorsoRot = newTorsoRot * ~curTorsoRot          // convert to additive
        //
        //   // smooth toward ideal
        //   val slerp = smoothInterpolant(TORSO_TARGET_HALF_LIFE)
        //   newTorsoRot = nlerp(slerp, torsoState.rotation, newTorsoRot)
        //
        //   // constrain total rotation
        //   val totalRot = newTorsoRot * torsoJoint!!.rotation
        //   totalRot.constrain(TORSO_ROTATION_CONSTRAINT)
        //   newTorsoRot = totalRot * ~torsoJoint!!.rotation
        //
        //   torsoState.rotation = newTorsoRot
        System.err.println("TargetingMotion: onUpdate not yet implemented")
        return false
    }

    override fun onDeactivate() {
        // nothing to clean up
    }
}
