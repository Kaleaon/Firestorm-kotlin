package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.F_SQRT2
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.clampRescale
import com.firestorm.llmath.slerp
import kotlin.math.abs

const val EDITING_EASEIN_DURATION: Float  = 0.0f
const val EDITING_EASEOUT_DURATION: Float = 0.5f
const val MIN_REQUIRED_PIXEL_AREA_EDITING: Float = 500f

private const val TARGET_LAG_HALF_LIFE: Float = 0.1f

class EditingMotion(id: LLUUID) : LLMotion(id) {

    // ---- internal kinematic joints (local copy of the chain) ----------------

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

    // ---- IK solver ----------------------------------------------------------

    private val ikSolver = JointSolverRP3()

    // ---- runtime state ------------------------------------------------------

    private var character: LLCharacter? = null
    private var wristOffset = Vector3(0f, 0.2f, 0f)
    private var lastSelectPt = Vector3()

    companion object {
        var handPose: Int = HandMotion.HandPose.RELAXED_R.ordinal
        var handPosePriority: Int = 3

        fun create(id: LLUUID): EditingMotion = EditingMotion(id)
    }

    init {
        name = "editing"

        // Build the kinematic chain (mirrors constructor addChild calls in C++)
        parentJoint.addChild(shoulderJoint)
        shoulderJoint.addChild(elbowJoint)
        elbowJoint.addChild(wristJoint)
    }

    // ---- LLMotion overrides -------------------------------------------------

    override fun getLoop(): Boolean = true
    override fun getDuration(): Float = 0f
    override fun getEaseInDuration(): Float  = EDITING_EASEIN_DURATION
    override fun getEaseOutDuration(): Float = EDITING_EASEOUT_DURATION
    override fun getPriority(): JointPriority = JointPriority.HIGH
    override fun getBlendType(): MotionBlendType = MotionBlendType.NORMAL_BLEND
    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_EDITING

    override fun onInitialize(character: Character): MotionInitStatus {
        this.character = character as? LLCharacter ?: return MotionInitStatus.FAILURE
        val ch = this.character!!

        val shoulderLeft = ch.getJoint("mShoulderLeft")
        val elbowLeft    = ch.getJoint("mElbowLeft")
        val wristLeft    = ch.getJoint("mWristLeft")

        if (shoulderLeft == null || elbowLeft == null || wristLeft == null) {
            System.err.println("EditingMotion: invalid skeleton — missing arm joints")
            return MotionInitStatus.FAILURE
        }

        parentState.setJoint(shoulderLeft.parent)
        if (parentState.joint == null) {
            System.err.println("EditingMotion: can't get parent joint of mShoulderLeft")
            return MotionInitStatus.FAILURE
        }

        shoulderState.setJoint(shoulderLeft)
        elbowState.setJoint(elbowLeft)
        wristState.setJoint(wristLeft)
        torsoState.setJoint(ch.getJoint("mTorso"))

        wristOffset = Vector3(0f, 0.2f, 0f)

        shoulderState.usage = JointState.Usage.ROT.mask
        elbowState.usage    = JointState.Usage.ROT.mask
        torsoState.usage    = JointState.Usage.ROT.mask
        wristState.usage    = JointState.Usage.ROT.mask

        syncChainFromSkeleton()

        // Configure the IK solver.
        // Pole vector (-1, 1, 0) keeps the elbow pointing to the left.
        // B-axis (-0.682683, 0, -0.730714) prevents flip at singular poses.
        ikSolver.setPoleVector(Vector3(-1f, 1f, 0f))
        ikSolver.setBAxis(Vector3(-0.682683f, 0f, -0.730714f))
        ikSolver.setupJoints(shoulderJoint, elbowJoint, wristJoint, ikTarget)

        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean {
        syncChainFromSkeleton()
        return true
    }

    override fun onUpdate(activeTime: Float): Boolean {
        val ch = character ?: return false

        val pointAtPt = ch.getAnimationData("PointAtPoint") as? Vector3
        var result = true

        var focusPt: Vector3
        if (pointAtPt == null) {
            focusPt = lastSelectPt
            result  = false
        } else {
            focusPt     = pointAtPt
            lastSelectPt = focusPt
        }

        focusPt = focusPt + ch.getCharacterPosition()

        syncChainFromSkeleton()

        // Target relative to the parent joint world position
        var target = focusPt - parentJoint.position
        val targetDist = run {
            val len = target.length()
            if (len > 1e-6f) { target = target * (1f / len); len } else { target = Vector3(1f, 1f, 1f); 1f }
        }

        // Edit-plane normal: 45° between X and Y axes, then rotated by torso world rotation.
        val ooSqrt2 = 1f / F_SQRT2
        val rawNorm = Vector3(ooSqrt2, ooSqrt2, 0f)
        val torsoWorldRot = torsoState.joint?.getWorldRotation() ?: Quaternion()
        val editPlaneNormal = torsoWorldRot * rawNorm

        val dot = editPlaneNormal.x * target.x + editPlaneNormal.y * target.y + editPlaneNormal.z * target.z
        if (dot < 0f) {
            target = target + editPlaneNormal * (dot * 2f)
            val lift = clampRescale(dot, 0f, -1f, 0f, 5f)
            target = Vector3(target.x, target.y, target.z + lift)
            val len = target.length()
            if (len > 1e-6f) target = target * (1f / len)
        }

        target = target * targetDist

        if (!target.isFinite()) {
            System.err.println("EditingMotion: non-finite target (dist=$targetDist, focus=$focusPt)")
            target = Vector3(1f, 1f, 1f)
        }

        ikTarget.position = target + parentJoint.position

        if (!ikTarget.position.isZero()) {
            val shoulderRotBefore = shoulderJoint.rotation
            val elbowRotBefore    = elbowJoint.rotation

            ikSolver.solve()

            // Smooth toward solved rotations using a critically-damped lag.
            val slerpAmt = smoothInterpolant(TARGET_LAG_HALF_LIFE)
            val shoulderBlended = slerp(slerpAmt, shoulderJoint.rotation, shoulderRotBefore)
            val elbowBlended    = slerp(slerpAmt, elbowJoint.rotation,    elbowRotBefore)

            shoulderState.rotation = shoulderBlended
            elbowState.rotation    = elbowBlended
            wristState.rotation    = Quaternion()
        }

        ch.setAnimationData("Hand Pose",          handPose)
        ch.setAnimationData("Hand Pose Priority", handPosePriority)
        return result
    }

    override fun onDeactivate() {}

    // ---- helpers ------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun Vector3.length(): Float = kotlin.math.sqrt(x * x + y * y + z * z)

private fun Vector3.isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite()

private fun Vector3.isZero(): Boolean = x == 0f && y == 0f && z == 0f

/**
 * Returns the slerp interpolant for a critically-damped lag with the given [halfLife].
 * Mirrors C++ LLSmoothInterpolation::getInterpolant(halfLife).
 * Uses the standard exponential approximation: interp = 1 - 2^(-dt/halfLife).
 */
private fun smoothInterpolant(halfLife: Float, dt: Float = 1f / 30f): Float =
    (1f - kotlin.math.exp(-dt * 0.693147f / halfLife.coerceAtLeast(1e-4f))).coerceIn(0f, 1f)
