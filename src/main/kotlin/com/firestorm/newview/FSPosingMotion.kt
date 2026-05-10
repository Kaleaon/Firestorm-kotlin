package com.firestorm.newview

import java.util.UUID
import kotlin.math.abs

enum class JointPriority {
    LOW_PRIORITY,
    MEDIUM_PRIORITY,
    HIGH_PRIORITY,
    ADDITIVE_PRIORITY,
}

enum class MotionBlendType {
    NORMAL_BLEND,
    ADDITIVE_BLEND,
}

enum class MotionInitStatus {
    STATUS_FAILURE,
    STATUS_SUCCESS,
}

class LLCharacter {
    fun getCharacterJoint(index: Int): LLJoint? = TODO("APR: use JVM equivalent - enumerate character joints by index")
    fun findCollisionVolume(index: Int): LLJoint? = TODO("APR: use JVM equivalent - enumerate collision volumes by index")
    fun getJoint(name: String): LLJoint? = TODO("APR: use JVM equivalent - look up joint by name")
}

class FSJointPose(joint: LLJoint, state: UInt, isCollisionVolume: Boolean = false) {
    fun getJointState(): FSJointState = TODO("APR: use JVM equivalent")
    fun getTargetRotation(): Quaternion = TODO("APR: use JVM equivalent")
    fun getTargetPosition(): Vector3 = TODO("APR: use JVM equivalent")
    fun getTargetScale(): Vector3 = TODO("APR: use JVM equivalent")
    fun revertJoint(): Unit = TODO("APR: use JVM equivalent")
    fun jointName(): String = TODO("APR: use JVM equivalent")
    fun getJointNumber(): Int = TODO("APR: use JVM equivalent")
    fun isCollisionVolume(): Boolean = TODO("APR: use JVM equivalent")
    fun isBaseRotationZero(): Boolean = TODO("APR: use JVM equivalent")
    fun userHasSetBaseRotationToZero(): Boolean = TODO("APR: use JVM equivalent")
    fun purgeUndoQueue(): Unit = TODO("APR: use JVM equivalent")
    fun setPublicRotation(base: Boolean, delta: Boolean, changeType: Int, rotation: Quaternion): Unit = TODO("APR: use JVM equivalent")
    fun setBaseRotation(rot: Quaternion, priority: JointPriority): Unit = TODO("APR: use JVM equivalent")
    fun setBasePosition(pos: Vector3, priority: JointPriority): Unit = TODO("APR: use JVM equivalent")
    fun setBaseScale(scale: Vector3, priority: JointPriority): Unit = TODO("APR: use JVM equivalent")
    fun zeroBaseRotation(lockInBvh: Boolean): Unit = TODO("APR: use JVM equivalent")
    fun setJointPriority(priority: JointPriority): Unit = TODO("APR: use JVM equivalent")
}

class FSJointState {
    fun getJoint(): LLJoint? = TODO("APR: use JVM equivalent")
    fun setPosition(pos: Vector3): Unit = TODO("APR: use JVM equivalent")
    fun setRotation(rot: Quaternion): Unit = TODO("APR: use JVM equivalent")
    fun setScale(scale: Vector3): Unit = TODO("APR: use JVM equivalent")
    fun setUsage(state: UInt): Unit = TODO("APR: use JVM equivalent")
    fun getUsage(): UInt = TODO("APR: use JVM equivalent")
}

class FSPosingMotion(private val motionId: UUID) {

    private val mName = "fs_poser_pose"
    private val mJointPoses: MutableList<FSJointPose> = mutableListOf()
    private var mCharacter: LLCharacter? = null

    private val closeEnough: Float = 1e-6f

    private companion object {
        const val MIN_REQUIRED_PIXEL_AREA_POSING = 500f

        const val POSER_CHANGE_ROTATION = 2

        // LLJointState.POS | LLJointState.ROT | LLJointState.SCALE combined flags
        const val POSER_JOINT_STATE: UInt = 7u

        fun create(id: UUID): FSPosingMotion = FSPosingMotion(id)
    }

    fun getLoop(): Boolean = true
    fun getDuration(): Float = 0f
    fun getEaseInDuration(): Float = 0f
    fun getEaseOutDuration(): Float = 0.5f
    fun getPriority(): JointPriority = JointPriority.ADDITIVE_PRIORITY
    fun getBlendType(): MotionBlendType = MotionBlendType.NORMAL_BLEND
    fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_POSING

    fun onInitialize(character: LLCharacter?): MotionInitStatus {
        if (character == null) return MotionInitStatus.STATUS_FAILURE
        mCharacter = character
        mJointPoses.clear()

        var i = 0
        while (true) {
            val targetJoint = character.getCharacterJoint(i) ?: break
            val jointPose = FSJointPose(targetJoint, POSER_JOINT_STATE)
            mJointPoses.add(jointPose)
            addJointState(jointPose.getJointState())
            i++
        }

        i = 0
        while (true) {
            val targetJoint = character.findCollisionVolume(i) ?: break
            val jointPose = FSJointPose(targetJoint, POSER_JOINT_STATE, true)
            mJointPoses.add(jointPose)
            addJointState(jointPose.getJointState())
            i++
        }

        return MotionInitStatus.STATUS_SUCCESS
    }

    fun onActivate(): Boolean = true

    fun onUpdate(time: Float, jointMask: UByteArray): Boolean {
        for (jointPose in mJointPoses) {
            val joint = jointPose.getJointState().getJoint() ?: continue

            val currentRotation = joint.getRotation()
            val currentPosition = joint.getPosition()
            val currentScale = joint.getScale()
            val targetRotation = jointPose.getTargetRotation()
            val targetPosition = jointPose.getTargetPosition()
            val targetScale = jointPose.getTargetScale()

            if (vectorsNotQuiteEqual(currentPosition, targetPosition)) {
                val lerped = lerp(currentPosition, targetPosition, mInterpolationTime)
                jointPose.getJointState().setPosition(lerped)
            }
            if (quatsNotQuiteEqual(currentRotation, targetRotation)) {
                val slerped = slerp(mInterpolationTime, currentRotation, targetRotation)
                jointPose.getJointState().setRotation(slerped)
            }
            if (vectorsNotQuiteEqual(currentScale, targetScale)) {
                val lerped = lerp(currentScale, targetScale, mInterpolationTime)
                jointPose.getJointState().setScale(lerped)
            }
        }
        return true
    }

    fun onDeactivate() {
        revertJointsAndCollisionVolumes()
    }

    fun currentlyPosingJoint(joint: FSJointPose?): Boolean {
        joint ?: return false
        val avJoint = joint.getJointState().getJoint() ?: return false
        return currentlyPosingJointInternal(avJoint)
    }

    fun addJointToState(joint: FSJointPose?) {
        joint ?: return
        val avJoint = joint.getJointState().getJoint() ?: return
        setJointState(avJoint, POSER_JOINT_STATE)
    }

    fun removeJointFromState(joint: FSJointPose?) {
        joint ?: return
        val avJoint = joint.getJointState().getJoint() ?: return
        joint.revertJoint()
        setJointState(avJoint, 0u)
    }

    fun getJointPoseByJointName(name: String): FSJointPose? {
        if (name.isEmpty() || mJointPoses.isEmpty()) return null
        return mJointPoses.firstOrNull { it.jointName().equals(name, ignoreCase = true) }
    }

    fun getJointPoseByJointNumber(number: Int): FSJointPose? {
        if (number < 0 || mJointPoses.isEmpty()) return null
        return mJointPoses.firstOrNull { it.getJointNumber() == number }
    }

    fun allStartingRotationsAreZero(): Boolean {
        return mJointPoses.none { !it.isCollisionVolume() && !it.isBaseRotationZero() }
    }

    fun setAllRotationsToZeroAndClearUndo() {
        for (pose in mJointPoses) {
            pose.purgeUndoQueue()
            pose.setPublicRotation(true, true, POSER_CHANGE_ROTATION, Quaternion())
        }
    }

    fun setJointBvhLock(joint: FSJointPose, lockInBvh: Boolean) {
        joint.zeroBaseRotation(lockInBvh)
    }

    data class JointStateAtTime(
        val hasRotation: Boolean = false,
        val rotation: Quaternion = Quaternion(),
        val hasPosition: Boolean = false,
        val position: Vector3 = Vector3(),
        val hasScale: Boolean = false,
        val scale: Vector3 = Vector3(),
    )

    fun loadOtherMotionToBaseOfThisMotion(motionToLoad: FSPosingMotion, timeToLoadAt: Float, selectedJointNumbers: List<Int>): Boolean {
        val motionIsForAllJoints = selectedJointNumbers.isEmpty()
        val priority = motionToLoad.getPriority()

        for (pose in mJointPoses) {
            val jointNumber = pose.getJointNumber()
            val jointName = pose.jointName()

            if (!motionIsForAllJoints && jointNumber !in selectedJointNumbers) continue

            val state = motionToLoad.getJointStateAtTime(jointName, timeToLoadAt)

            if (state.hasRotation && !pose.userHasSetBaseRotationToZero()) pose.setBaseRotation(state.rotation, priority)
            if (state.hasPosition) pose.setBasePosition(state.position, priority)
            if (state.hasScale) pose.setBaseScale(state.scale, priority)
        }
        return true
    }

    fun getJointStateAtTime(jointPoseName: String, timeToLoadAt: Float): JointStateAtTime {
        TODO("APR: use JVM equivalent - iterate joint motion list, match name case-insensitively, evaluate rotation/position/scale curves at timeToLoadAt")
    }

    fun otherMotionAnimatesJoints(motionToQuery: FSPosingMotion, recapturedJointNumbers: List<Int>): Boolean {
        return motionToQuery.motionAnimatesJoints(recapturedJointNumbers)
    }

    fun motionAnimatesJoints(recapturedJointNumbers: List<Int>): Boolean {
        TODO("APR: use JVM equivalent - iterate mJointMotionList, look up joint numbers, return true if any has rotation keys in the captured set")
    }

    fun resetBonePriority(boneNumbersToReset: List<Int>) {
        for (boneNumber in boneNumbersToReset) {
            mJointPoses
                .filter { it.getJointNumber() == boneNumber }
                .forEach { it.setJointPriority(JointPriority.LOW_PRIORITY) }
        }
    }

    private val mInterpolationTime: Float = 0.25f

    private fun revertJointsAndCollisionVolumes() {
        for (pose in mJointPoses) {
            pose.revertJoint()
            val joint = pose.getJointState().getJoint() ?: continue
            addJointStateInternal(joint)
        }
    }

    private fun currentlyPosingJointInternal(joint: LLJoint): Boolean {
        if (mJointPoses.isEmpty()) return false
        TODO("APR: use JVM equivalent - look up joint state in pose, check usage flags")
    }

    private fun addJointStateInternal(joint: LLJoint) {
        setJointState(joint, POSER_JOINT_STATE)
    }

    private fun setJointState(joint: LLJoint?, state: UInt) {
        if (mJointPoses.isEmpty() || joint == null) return
        TODO("APR: use JVM equivalent - find joint state in pose, remove it, update usage, re-add")
    }

    private fun addJointState(state: FSJointState) {
        TODO("APR: use JVM equivalent - add joint state to this motion's pose")
    }

    private fun vectorsNotQuiteEqual(v1: Vector3, v2: Vector3): Boolean {
        return !(vectorAxesAlmostEqual(v1.x, v2.x) &&
                vectorAxesAlmostEqual(v1.y, v2.y) &&
                vectorAxesAlmostEqual(v1.z, v2.z))
    }

    private fun quatsNotQuiteEqual(q1: Quaternion, q2: Quaternion): Boolean {
        TODO("APR: use JVM equivalent - compare q1 vs q2 and q1 vs -q2 component-wise within closeEnough tolerance")
    }

    private fun vectorAxesAlmostEqual(a: Float, b: Float): Boolean = abs(a - b) < closeEnough

    private fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 =
        Vector3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t)

    private fun slerp(t: Float, q1: Quaternion, q2: Quaternion): Quaternion {
        TODO("APR: use JVM equivalent - spherical linear interpolation between two quaternions")
    }
}

private fun LLJoint.getRotation(): Quaternion = TODO("APR: use JVM equivalent")
private fun LLJoint.getPosition(): Vector3 = TODO("APR: use JVM equivalent")
private fun LLJoint.getScale(): Vector3 = TODO("APR: use JVM equivalent")
