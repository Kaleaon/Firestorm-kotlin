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
    fun getCharacterJoint(index: Int): LLJoint? { System.err.println("LLCharacter: getCharacterJoint not yet implemented"); return null }
    fun findCollisionVolume(index: Int): LLJoint? { System.err.println("LLCharacter: findCollisionVolume not yet implemented"); return null }
    fun getJoint(name: String): LLJoint? { System.err.println("LLCharacter: getJoint not yet implemented"); return null }
}

class FSJointPose(joint: LLJoint, state: UInt, isCollisionVolume: Boolean = false) {
    fun getJointState(): FSJointState { System.err.println("FSJointPose: getJointState not yet implemented"); return FSJointState() }
    fun getTargetRotation(): Quaternion { System.err.println("FSJointPose: getTargetRotation not yet implemented"); return Quaternion() }
    fun getTargetPosition(): Vector3 { System.err.println("FSJointPose: getTargetPosition not yet implemented"); return Vector3() }
    fun getTargetScale(): Vector3 { System.err.println("FSJointPose: getTargetScale not yet implemented"); return Vector3() }
    fun revertJoint(): Unit { System.err.println("FSJointPose: revertJoint not yet implemented") }
    fun jointName(): String { System.err.println("FSJointPose: jointName not yet implemented"); return "" }
    fun getJointNumber(): Int { System.err.println("FSJointPose: getJointNumber not yet implemented"); return 0 }
    fun isCollisionVolume(): Boolean { System.err.println("FSJointPose: isCollisionVolume not yet implemented"); return false }
    fun isBaseRotationZero(): Boolean { System.err.println("FSJointPose: isBaseRotationZero not yet implemented"); return false }
    fun userHasSetBaseRotationToZero(): Boolean { System.err.println("FSJointPose: userHasSetBaseRotationToZero not yet implemented"); return false }
    fun purgeUndoQueue(): Unit { System.err.println("FSJointPose: purgeUndoQueue not yet implemented") }
    fun setPublicRotation(base: Boolean, delta: Boolean, changeType: Int, rotation: Quaternion): Unit { System.err.println("FSJointPose: setPublicRotation not yet implemented") }
    fun setBaseRotation(rot: Quaternion, priority: JointPriority): Unit { System.err.println("FSJointPose: setBaseRotation not yet implemented") }
    fun setBasePosition(pos: Vector3, priority: JointPriority): Unit { System.err.println("FSJointPose: setBasePosition not yet implemented") }
    fun setBaseScale(scale: Vector3, priority: JointPriority): Unit { System.err.println("FSJointPose: setBaseScale not yet implemented") }
    fun zeroBaseRotation(lockInBvh: Boolean): Unit { System.err.println("FSJointPose: zeroBaseRotation not yet implemented") }
    fun setJointPriority(priority: JointPriority): Unit { System.err.println("FSJointPose: setJointPriority not yet implemented") }
}

class FSJointState {
    fun getJoint(): LLJoint? { System.err.println("FSJointState: getJoint not yet implemented"); return null }
    fun setPosition(pos: Vector3): Unit { System.err.println("FSJointState: setPosition not yet implemented") }
    fun setRotation(rot: Quaternion): Unit { System.err.println("FSJointState: setRotation not yet implemented") }
    fun setScale(scale: Vector3): Unit { System.err.println("FSJointState: setScale not yet implemented") }
    fun setUsage(state: UInt): Unit { System.err.println("FSJointState: setUsage not yet implemented") }
    fun getUsage(): UInt { System.err.println("FSJointState: getUsage not yet implemented"); return 0u }
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
        System.err.println("FSPosingMotion: getJointStateAtTime not yet implemented")
        return JointStateAtTime()
    }

    fun otherMotionAnimatesJoints(motionToQuery: FSPosingMotion, recapturedJointNumbers: List<Int>): Boolean {
        return motionToQuery.motionAnimatesJoints(recapturedJointNumbers)
    }

    fun motionAnimatesJoints(recapturedJointNumbers: List<Int>): Boolean {
        System.err.println("FSPosingMotion: motionAnimatesJoints not yet implemented")
        return false
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
        System.err.println("FSPosingMotion: currentlyPosingJointInternal not yet implemented")
        return false
    }

    private fun addJointStateInternal(joint: LLJoint) {
        setJointState(joint, POSER_JOINT_STATE)
    }

    private fun setJointState(joint: LLJoint?, state: UInt) {
        if (mJointPoses.isEmpty() || joint == null) return
        System.err.println("FSPosingMotion: setJointState not yet implemented")
    }

    private fun addJointState(state: FSJointState) {
        System.err.println("FSPosingMotion: addJointState not yet implemented")
    }

    private fun vectorsNotQuiteEqual(v1: Vector3, v2: Vector3): Boolean {
        return !(vectorAxesAlmostEqual(v1.x, v2.x) &&
                vectorAxesAlmostEqual(v1.y, v2.y) &&
                vectorAxesAlmostEqual(v1.z, v2.z))
    }

    private fun quatsNotQuiteEqual(q1: Quaternion, q2: Quaternion): Boolean {
        System.err.println("FSPosingMotion: quatsNotQuiteEqual not yet implemented")
        return false
    }

    private fun vectorAxesAlmostEqual(a: Float, b: Float): Boolean = abs(a - b) < closeEnough

    private fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 =
        Vector3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t)

    private fun slerp(t: Float, q1: Quaternion, q2: Quaternion): Quaternion {
        System.err.println("FSPosingMotion: slerp not yet implemented")
        return Quaternion()
    }
}

private fun LLJoint.getRotation(): Quaternion { System.err.println("LLJoint: getRotation not yet implemented"); return Quaternion() }
private fun LLJoint.getPosition(): Vector3 { System.err.println("LLJoint: getPosition not yet implemented"); return Vector3() }
private fun LLJoint.getScale(): Vector3 { System.err.println("LLJoint: getScale not yet implemented"); return Vector3() }
