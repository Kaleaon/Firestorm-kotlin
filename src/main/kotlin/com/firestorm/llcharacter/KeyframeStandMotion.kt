// Converted from llkeyframestandmotion.h / llkeyframestandmotion.cpp — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val MIN_TRACK_SPEED: Float = 0.01f
private const val ROTATION_THRESHOLD: Float = 0.6f
private const val POSITION_THRESHOLD: Float = 0.1f    // squared distance in metres²

// ---------------------------------------------------------------------------
// KeyframeStandMotion
//
// Extends KeyframeMotion with foot-IK (LLJointSolverRP3) so that the
// avatar's feet conform to ground geometry while standing idle.
// Mirrors C++ LLKeyframeStandMotion.
//
// The IK solver (LLJointSolverRP3) and the ankle-projection math are stubbed
// with TODO() because the solver is not yet ported.  Keyframe playback and
// the threshold-based ankle tracking state machine are fully implemented.
// ---------------------------------------------------------------------------

class KeyframeStandMotion(id: LLUUID) : KeyframeMotion(id) {

    // ---- internal kinematic joints (local copies of the leg chains) --------

    private val pelvisJoint       = Joint("_standPelvis")
    private val hipLeftJoint      = Joint("_standHipL")
    private val kneeLeftJoint     = Joint("_standKneeL")
    private val ankleLeftJoint    = Joint("_standAnkleL")
    private val targetLeft        = Joint("_standTargetL")

    private val hipRightJoint     = Joint("_standHipR")
    private val kneeRightJoint    = Joint("_standKneeR")
    private val ankleRightJoint   = Joint("_standAnkleR")
    private val targetRight       = Joint("_standTargetR")

    // ---- joint states (resolved from the Pose after base-class init) -------

    private var pelvisState:      JointState? = null
    private var hipLeftState:     JointState? = null
    private var kneeLeftState:    JointState? = null
    private var ankleLeftState:   JointState? = null
    private var hipRightState:    JointState? = null
    private var kneeRightState:   JointState? = null
    private var ankleRightState:  JointState? = null

    // ---- runtime state -----------------------------------------------------

    private var character: LLCharacter? = null
    private var flipFeet: Boolean = false
    private var trackAnkles: Boolean = true
    private var frameNum: Int = 0

    private var positionLeft:  Vector3 = Vector3()
    private var positionRight: Vector3 = Vector3()
    private var normalLeft:    Vector3 = Vector3(0f, 0f, 1f)
    private var normalRight:   Vector3 = Vector3(0f, 0f, 1f)
    private var rotationLeft:  Quaternion = Quaternion()
    private var rotationRight: Quaternion = Quaternion()

    private var lastGoodPelvisRotation: Quaternion = Quaternion()
    private var lastGoodPosition: Vector3 = Vector3()

    companion object {
        fun create(id: LLUUID): KeyframeStandMotion = KeyframeStandMotion(id)
    }

    init {
        // build kinematic hierarchy
        pelvisJoint.addChild(hipLeftJoint)
        hipLeftJoint.addChild(kneeLeftJoint)
        kneeLeftJoint.addChild(ankleLeftJoint)

        pelvisJoint.addChild(hipRightJoint)
        hipRightJoint.addChild(kneeRightJoint)
        kneeRightJoint.addChild(ankleRightJoint)
    }

    // ---- LLMotion / KeyframeMotion overrides --------------------------------

    override fun onInitialize(character: Character): MotionInitStatus {
        val ch = character as? LLCharacter ?: return MotionInitStatus.FAILURE
        this.character = ch
        flipFeet = false

        // Let the base class load keyframe data and set up joint states
        val status = super.onInitialize(character)
        if (status == MotionInitStatus.FAILURE) return status

        // Resolve required joint states from the baked pose
        pelvisState     = jointStates.firstOrNull { it.getJointName() == "mPelvis" }
        hipLeftState    = jointStates.firstOrNull { it.getJointName() == "mHipLeft" }
        kneeLeftState   = jointStates.firstOrNull { it.getJointName() == "mKneeLeft" }
        ankleLeftState  = jointStates.firstOrNull { it.getJointName() == "mAnkleLeft" }
        hipRightState   = jointStates.firstOrNull { it.getJointName() == "mHipRight" }
        kneeRightState  = jointStates.firstOrNull { it.getJointName() == "mKneeRight" }
        ankleRightState = jointStates.firstOrNull { it.getJointName() == "mAnkleRight" }

        if (pelvisState == null || hipLeftState == null || kneeLeftState == null ||
            ankleLeftState == null || hipRightState == null || kneeRightState == null ||
            ankleRightState == null
        ) {
            System.err.println("KeyframeStandMotion: can't find necessary joint states")
            return MotionInitStatus.FAILURE
        }

        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean {
        // TODO: configure IK solvers:
        //   ikLeft.setPoleVector(Vector3(1f, 0f, 0f));  ikLeft.setBAxis(Vector3(0.05f, 1f, 0f))
        //   ikRight.setPoleVector(Vector3(1f, 0f, 0f)); ikRight.setBAxis(Vector3(-0.05f, 1f, 0f))

        lastGoodPelvisRotation = Quaternion()
        lastGoodPosition = Vector3()
        frameNum = 0

        return super.onActivate()
    }

    override fun onDeactivate() {
        super.onDeactivate()
    }

    override fun onUpdate(activeTime: Float): Boolean {
        // Let the base class advance the keyframe cycle
        val status = super.onUpdate(activeTime)
        if (!status) return false

        val ch = character ?: return true

        val rootWorldPos = pelvisState?.joint?.parent?.getWorldPosition()
            ?: return true

        // No valid world position yet — wait
        if (rootWorldPos == Vector3()) return true

        // --- ankle-tracking threshold checks --------------------------------

        val pelvisRot = pelvisState?.joint?.getWorldRotation() ?: Quaternion()
        if (pelvisRot.dot(lastGoodPelvisRotation) < ROTATION_THRESHOLD) {
            lastGoodPelvisRotation = pelvisRot.normalized()
            trackAnkles = true
        } else {
            val posDelta = ch.getCharacterPosition() - lastGoodPosition
            if (posDelta.lengthSquared() > POSITION_THRESHOLD) {
                lastGoodPosition = ch.getCharacterPosition()
                trackAnkles = true
            }
            // pose.getWeight() < 1f branch: approximated via ease-in period (frameNum < 3)
        }

        // --- propagate skeleton transforms into internal kinematic joints ---

        pelvisJoint.position   = rootWorldPos + (pelvisState?.position ?: Vector3())
        pelvisJoint.rotation   = pelvisRot

        copyLegChain(
            hipJoint = hipLeftJoint, kneeJoint = kneeLeftJoint, ankleJoint = ankleLeftJoint,
            hipState = hipLeftState!!, kneeState = kneeLeftState!!, ankleState = ankleLeftState!!
        )
        copyLegChain(
            hipJoint = hipRightJoint, kneeJoint = kneeRightJoint, ankleJoint = ankleRightJoint,
            hipState = hipRightState!!, kneeState = kneeRightState!!, ankleState = ankleRightState!!
        )

        // --- IK solver setup and solve ---------------------------------------

        when {
            frameNum == 2 -> {
                // TODO: ikLeft.setupJoints(hipLeftJoint, kneeLeftJoint, ankleLeftJoint, targetLeft)
                //       ikRight.setupJoints(hipRightJoint, kneeRightJoint, ankleRightJoint, targetRight)
            }
            frameNum < 2  -> { frameNum++; return true }
        }
        frameNum++

        // --- project ankles to ground and solve IK --------------------------

        if (trackAnkles) {
            // TODO: ch.getGround(ankleLeftJoint.getWorldPosition(), positionLeft, normalLeft)
            //       ch.getGround(ankleRightJoint.getWorldPosition(), positionRight, normalRight)
            //       targetLeft.position  = positionLeft
            //       targetRight.position = positionRight
        }

        // TODO: ikLeft.solve(); ikRight.solve()

        // --- conform ankle rotations to ground normals ----------------------

        if (trackAnkles) {
            // TODO: build rotationLeft from (normalLeft, ankleLeftJoint forward vector)
            //       build rotationRight from (normalRight, ankleRightJoint forward vector)
            //       if flipFeet: negate up component
        }

        // TODO: ankleLeftJoint.setWorldRotation(rotationLeft)
        //       ankleRightJoint.setWorldRotation(rotationRight)

        // --- write solved rotations back to joint states --------------------

        // TODO: uncomment once IK is implemented:
        // hipLeftState!!.rotation    = hipLeftJoint.rotation
        // kneeLeftState!!.rotation   = kneeLeftJoint.rotation
        // ankleLeftState!!.rotation  = ankleLeftJoint.rotation
        // hipRightState!!.rotation   = hipRightJoint.rotation
        // kneeRightState!!.rotation  = kneeRightJoint.rotation
        // ankleRightState!!.rotation = ankleRightJoint.rotation

        return true
    }

    // ---- helpers -----------------------------------------------------------

    /**
     * Copy position, scale and rotation from joint states into the corresponding
     * internal kinematic joints.  The rotation is taken from the keyframe pose
     * (GO_TO_KEY_POSE = true in the C++ code).
     */
    private fun copyLegChain(
        hipJoint:    Joint, kneeJoint:   Joint, ankleJoint:  Joint,
        hipState:    JointState, kneeState:   JointState, ankleState:  JointState
    ) {
        hipJoint.position    = hipState.joint?.position    ?: Vector3()
        kneeJoint.position   = kneeState.joint?.position   ?: Vector3()
        ankleJoint.position  = ankleState.joint?.position  ?: Vector3()

        hipJoint.scale       = hipState.joint?.scale       ?: Vector3(1f, 1f, 1f)
        kneeJoint.scale      = kneeState.joint?.scale      ?: Vector3(1f, 1f, 1f)
        ankleJoint.scale     = ankleState.joint?.scale     ?: Vector3(1f, 1f, 1f)

        // Use keyframe-driven rotations (GO_TO_KEY_POSE = true)
        hipJoint.rotation    = hipState.rotation
        kneeJoint.rotation   = kneeState.rotation
        ankleJoint.rotation  = ankleState.rotation
    }
}

// ---------------------------------------------------------------------------
// Private extension helpers
// ---------------------------------------------------------------------------

private fun Quaternion.dot(other: Quaternion): Float =
    // Quaternion dot product: w*w + x*x + y*y + z*z
    // Assumes Quaternion exposes w, x, y, z components.
    // If those fields are named differently, adjust accordingly.
    TODO("Implement Quaternion.dot — adjust component names to match your Quaternion class")

private fun Quaternion.normalized(): Quaternion =
    TODO("Implement Quaternion.normalized — adjust to your Quaternion class API")

private fun Vector3.lengthSquared(): Float = x * x + y * y + z * z
