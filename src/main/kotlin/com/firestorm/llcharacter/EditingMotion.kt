// Converted from lleditingmotion.h / lleditingmotion.cpp — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// Constants  (mirrors the C++ #defines and file-scope constants)
// ---------------------------------------------------------------------------

const val EDITING_EASEIN_DURATION: Float  = 0.0f
const val EDITING_EASEOUT_DURATION: Float = 0.5f
const val MIN_REQUIRED_PIXEL_AREA_EDITING: Float = 500f

/** Half-life for IK target lag smoothing (seconds). */
private const val TARGET_LAG_HALF_LIFE: Float = 0.1f

// ---------------------------------------------------------------------------
// EditingMotion
//
// Drives the left arm so it reaches toward the current "edit target" point.
// Mirrors C++ LLEditingMotion.
//
// IK (LLJointSolverRP3) is stubbed with TODO() because the solver is not yet
// ported.  All joint-state bookkeeping and the motion-lifecycle contract are
// fully preserved.
// ---------------------------------------------------------------------------

class EditingMotion(id: LLUUID) : LLMotion(id) {

    // ---- internal kinematic joints (local copy of the chain) ---------------

    private val parentJoint   = Joint("_editParent")
    private val shoulderJoint = Joint("_editShoulder")
    private val elbowJoint    = Joint("_editElbow")
    private val wristJoint    = Joint("_editWrist")
    private val ikTarget      = Joint("_editTarget")

    // ---- joint states -------------------------------------------------------

    private val parentState   = JointState()
    private val shoulderState = JointState()
    private val elbowState    = JointState()
    private val wristState    = JointState()
    private val torsoState    = JointState()

    // ---- runtime state ------------------------------------------------------

    private var character: LLCharacter? = null
    private var wristOffset = Vector3(0f, 0.2f, 0f)
    private var lastSelectPt = Vector3()

    /** Companion-object analogue of the C++ static members. */
    companion object {
        var handPose: Int = HandMotion.HandPose.RELAXED_R.ordinal
        var handPosePriority: Int = 3

        fun create(id: LLUUID): EditingMotion = EditingMotion(id)
    }

    init {
        name = "editing"

        // build kinematic chain
        parentJoint.addChild(shoulderJoint)
        shoulderJoint.addChild(elbowJoint)
        elbowJoint.addChild(wristJoint)
    }

    // ---- LLMotion overrides ------------------------------------------------

    override fun getLoop(): Boolean = true
    override fun getDuration(): Float = 0f
    override fun getEaseInDuration(): Float = EDITING_EASEIN_DURATION
    override fun getEaseOutDuration(): Float = EDITING_EASEOUT_DURATION
    override fun getPriority(): JointPriority = JointPriority.HIGH
    override fun getBlendType(): MotionBlendType = MotionBlendType.NORMAL_BLEND
    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_EDITING

    override fun onInitialize(character: Character): MotionInitStatus {
        this.character = character as? LLCharacter ?: return MotionInitStatus.FAILURE

        val shoulderLeft = character.getJoint("mShoulderLeft")
        val elbowLeft    = character.getJoint("mElbowLeft")
        val wristLeft    = character.getJoint("mWristLeft")

        if (shoulderLeft == null || elbowLeft == null || wristLeft == null) {
            System.err.println("EditingMotion: invalid skeleton — missing arm joints")
            return MotionInitStatus.FAILURE
        }

        parentState.setJoint(shoulderLeft.parent)
        if (parentState.joint == null) {
            System.err.println("EditingMotion: can't get parent joint")
            return MotionInitStatus.FAILURE
        }

        shoulderState.setJoint(shoulderLeft)
        elbowState.setJoint(elbowLeft)
        wristState.setJoint(wristLeft)
        torsoState.setJoint(character.getJoint("mTorso"))

        wristOffset = Vector3(0f, 0.2f, 0f)

        // register which DOFs each state controls
        shoulderState.usage = JointState.Usage.ROT.mask
        elbowState.usage    = JointState.Usage.ROT.mask
        torsoState.usage    = JointState.Usage.ROT.mask
        wristState.usage    = JointState.Usage.ROT.mask

        // propagate initial transforms to the internal kinematic chain
        syncChainFromSkeleton()

        // TODO: configure IK solver pole vector (-1, 1, 0), b-axis (-0.682683, 0, -0.730714),
        //       and call mIKSolver.setupJoints(shoulderJoint, elbowJoint, wristJoint, ikTarget)

        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean {
        syncChainFromSkeleton()
        return true
    }

    override fun onUpdate(activeTime: Float): Boolean {
        val ch = character ?: return false

        var focusPt = (ch.getAnimationData("PointAtPoint") as? Vector3)
            ?: run {
                // no target — keep the last known point but signal completion
                ch.setAnimationData("Hand Pose", handPose)
                ch.setAnimationData("Hand Pose Priority", handPosePriority)
                return false
            }

        lastSelectPt = focusPt
        focusPt = focusPt + ch.getCharacterPosition()

        syncChainFromSkeleton()

        // compute target relative to the parent joint's world position
        var target = focusPt - parentJoint.position
        val targetDist = target.length().also { len ->
            if (len > 0f) target = target * (1f / len) else target = Vector3(1f, 1f, 1f)
        }

        // constrain the target to the edit plane (torso-space half-space check)
        val editPlaneNormal = Vector3(
            1f / Math.sqrt(2.0).toFloat(),
            1f / Math.sqrt(2.0).toFloat(),
            0f
        ).let { n ->
            // rotate by torso world rotation
            torsoState.joint?.getWorldRotation()?.rotate(n) ?: n
        }

        val dot = editPlaneNormal.dot(target)
        if (dot < 0f) {
            target = target + editPlaneNormal * (dot * 2f)
            // lift the z component when pointing behind (clamp_rescale equivalent)
            val lift = if (dot > -1f) (-dot / 1f) * 5f else 5f
            target = Vector3(target.x, target.y, target.z + lift)
            val len = target.length()
            if (len > 0f) target = target * (1f / len)
        }

        target = target * targetDist
        ikTarget.position = target + parentJoint.position

        // TODO: solve IK, then slerp shoulder/elbow toward solved rotations
        //       using LLSmoothInterpolation::getInterpolant(TARGET_LAG_HALF_LIFE)
        //       and write back via shoulderState/elbowState/wristState.rotation
        TODO("Apply IK solver: solve(), slerp blending, write shoulder/elbow/wrist rotations")
    }

    override fun onDeactivate() {
        // nothing to clean up
    }

    // ---- helpers -----------------------------------------------------------

    /** Copy current world/local transforms from the skeleton into the internal kinematic joints. */
    private fun syncChainFromSkeleton() {
        parentJoint.position   = parentState.joint?.getWorldPosition()  ?: Vector3()
        shoulderJoint.position = shoulderState.joint?.position          ?: Vector3()
        elbowJoint.position    = elbowState.joint?.position             ?: Vector3()
        wristJoint.position    = (wristState.joint?.position            ?: Vector3()) + wristOffset

        parentJoint.rotation   = parentState.joint?.getWorldRotation()  ?: Quaternion()
        shoulderJoint.rotation = shoulderState.joint?.rotation          ?: Quaternion()
        elbowJoint.rotation    = elbowState.joint?.rotation             ?: Quaternion()
    }
}
