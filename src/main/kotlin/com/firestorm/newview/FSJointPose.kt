package com.firestorm.newview

import java.util.UUID
import java.util.ArrayDeque
import java.time.Instant

enum class EPoserChangeType {
    POSER_CHANGE_DEFAULT,
    POSER_CHANGE_CHILDMOVE,
    POSER_CHANGE_ROTATION,
}

data class LLVector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun isZero() = x == 0f && y == 0f && z == 0f
    operator fun plus(other: LLVector3) = LLVector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: LLVector3) = LLVector3(x - other.x, y - other.y, z - other.z)
    companion object {
        val ZERO = LLVector3(0f, 0f, 0f)
    }
}

data class LLQuaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    fun conjugate() = LLQuaternion(-x, -y, -z, w)
    fun normalize(): LLQuaternion {
        val mag = kotlin.math.sqrt((x * x + y * y + z * z + w * w).toDouble()).toFloat()
        return if (mag > 0f) LLQuaternion(x / mag, y / mag, z / mag, w / mag) else LLQuaternion()
    }
    operator fun times(other: LLQuaternion) = LLQuaternion(
        w * other.x + x * other.w + y * other.z - z * other.y,
        w * other.y - x * other.z + y * other.w + z * other.x,
        w * other.z + x * other.y - y * other.x + z * other.w,
        w * other.w - x * other.x - y * other.y - z * other.z,
    )
    fun getEulerAngles(): Triple<Float, Float, Float> {
        TODO("GPU: convert quaternion to Euler angles")
    }
    companion object {
        val DEFAULT = LLQuaternion(0f, 0f, 0f, 1f)
    }
}

enum class JointPriority { LOW_PRIORITY, MEDIUM_PRIORITY, HIGH_PRIORITY }

interface LLJoint {
    fun getRotation(): LLQuaternion
    fun getPosition(): LLVector3
    fun getScale(): LLVector3
    fun setRotation(rot: LLQuaternion)
    fun setPosition(pos: LLVector3)
    fun setScale(scale: LLVector3)
    fun getName(): String
    fun getJointNum(): Int
    fun getWorldRotation(): LLQuaternion
    fun getWorldPosition(): LLVector3
    fun getParent(): LLJoint?
}

interface LLJointState {
    fun getJoint(): LLJoint?
    fun setJoint(joint: LLJoint)
    fun setUsage(usage: UInt)
}

private const val MAXIMUM_UNDO_QUEUE_LENGTH = 20
private const val UNDO_UPDATE_INTERVAL_MS = 800L

class FSJointPose(joint: LLJoint, usage: UInt, val isCollisionVolume: Boolean = false) {

    val jointName: String = joint.getName()
    val jointNumber: Int = joint.getJointNum()

    private val jointState: LLJointState = TODO("APR: use JVM equivalent")
    private var modifiedThisSession: Boolean = false

    private val lastSetJointStates: ArrayDeque<FSJointState> = ArrayDeque()
    private var undoneJointStatesIndex: Int = 0
    private var timeLastUpdatedCurrentState: Instant = Instant.now()

    private var currentState: FSJointState = FSJointState(joint)

    init {
        TODO("APR: use JVM equivalent for LLJointState construction")
    }

    fun getPublicPosition(): LLVector3 = currentState.position
    fun getPublicRotation(): LLQuaternion = currentState.rotation
    fun getPublicScale(): LLVector3 = currentState.scale
    fun getTargetRotation(): LLQuaternion = currentState.getTargetRotation()
    fun getTargetPosition(): LLVector3 = currentState.getTargetPosition()
    fun getTargetScale(): LLVector3 = currentState.getTargetScale()
    fun getJointModified(): Boolean = modifiedThisSession
    fun canPerformRedo(): Boolean = undoneJointStatesIndex > 0

    fun getJointState(): LLJointState = jointState

    fun canPerformUndo(): Boolean = when (lastSetJointStates.size) {
        0 -> false
        1 -> true
        else -> undoneJointStatesIndex != (lastSetJointStates.size - 1)
    }

    fun setPublicPosition(pos: LLVector3) {
        addStateToUndo(currentState)
        currentState = currentState.copy(position = pos, poserChangeType = EPoserChangeType.POSER_CHANGE_DEFAULT)
    }

    fun setPublicRotation(zeroBase: Boolean, zeroBaseAsUser: Boolean, changeType: EPoserChangeType, rot: LLQuaternion) {
        addStateToUndo(currentState)
        currentState = if (zeroBase) {
            val zeroed = currentState.withZeroedBase(lockInBvh = zeroBaseAsUser)
            zeroed.copy(rotation = rot, poserChangeType = changeType)
        } else {
            currentState.copy(
                rotation = rot,
                poserChangeType = changeType,
                userSpecifiedBaseZero = false,
            )
        }
    }

    fun setPublicScale(scale: LLVector3) {
        addStateToUndo(currentState)
        currentState = currentState.copy(scale = scale, poserChangeType = EPoserChangeType.POSER_CHANGE_DEFAULT)
    }

    fun undoLastChange(): EPoserChangeType {
        val changeType = currentState.poserChangeType
        currentState = undoLastStateChange(currentState)
        return changeType
    }

    fun redoLastChange(): EPoserChangeType {
        currentState = redoLastStateChange(currentState)
        return currentState.poserChangeType
    }

    fun resetJoint() {
        addStateToUndo(currentState)
        currentState = currentState.reset().copy(poserChangeType = EPoserChangeType.POSER_CHANGE_DEFAULT)
    }

    fun reflectRotation() {
        if (isCollisionVolume) return
        modifiedThisSession = true
        currentState = currentState.reflect()
    }

    fun reflectBaseRotation() {
        if (isCollisionVolume) return
        currentState = currentState.reflectBase()
    }

    fun zeroBaseRotation(lockInBvh: Boolean) {
        if (isCollisionVolume) return
        currentState = currentState.withZeroedBase(lockInBvh = lockInBvh)
    }

    fun isBaseRotationZero(): Boolean {
        if (isCollisionVolume) return true
        return currentState.baseRotationIsZero()
    }

    fun swapRotationWith(oppositeJoint: FSJointPose) {
        if (isCollisionVolume) return
        val temp = currentState
        currentState = currentState.cloneRotationFrom(oppositeJoint.currentState)
        oppositeJoint.currentState = oppositeJoint.currentState.cloneRotationFrom(temp)
    }

    fun swapBaseRotationWith(oppositeJoint: FSJointPose) {
        if (isCollisionVolume) return
        val temp = currentState
        currentState = currentState.cloneBaseRotationFrom(oppositeJoint.currentState)
        oppositeJoint.currentState = oppositeJoint.currentState.cloneBaseRotationFrom(temp)
    }

    fun cloneRotationFrom(fromJoint: FSJointPose) {
        addStateToUndo(currentState)
        currentState = currentState.cloneRotationFrom(fromJoint.currentState)
            .copy(poserChangeType = EPoserChangeType.POSER_CHANGE_DEFAULT)
    }

    fun mirrorRotationFrom(fromJoint: FSJointPose) {
        cloneRotationFrom(fromJoint)
        currentState = currentState.reflect()
    }

    fun recaptureJoint() {
        val joint = TODO("APR: use JVM equivalent to get joint from jointState") as LLJoint
        addStateToUndo(currentState)
        currentState = FSJointState(joint).copy(poserChangeType = EPoserChangeType.POSER_CHANGE_DEFAULT)
    }

    fun updateJointAsDelta(zeroBase: Boolean, rotation: LLQuaternion, position: LLVector3, scale: LLVector3): LLQuaternion {
        addStateToUndo(currentState)
        currentState = currentState.copy(poserChangeType = EPoserChangeType.POSER_CHANGE_DEFAULT)
        return currentState.updateFromJointProperties(zeroBase, rotation, position, scale).also { newPublicRot ->
            currentState = currentState // mutable mutation done inside FSJointState.updateFromJointProperties
        }
    }

    fun setBaseRotation(rotation: LLQuaternion, priority: JointPriority) {
        currentState = currentState.resetBaseRotation(rotation, priority)
    }

    fun setBasePosition(position: LLVector3, priority: JointPriority) {
        currentState = currentState.resetBasePosition(position, priority)
    }

    fun setBaseScale(scale: LLVector3, priority: JointPriority) {
        currentState = currentState.resetBaseScale(scale, priority)
    }

    fun setJointPriority(priority: JointPriority) {
        currentState = currentState.copy(basePriority = priority)
    }

    fun purgeUndoQueue() {
        if (isCollisionVolume) return
        undoneJointStatesIndex = 0
        lastSetJointStates.clear()
    }

    fun userHasSetBaseRotationToZero(): Boolean {
        if (isCollisionVolume) return false
        return currentState.userSpecifiedBaseZero
    }

    fun getWorldRotationLockState(): Boolean = currentState.rotationIsWorldLocked

    fun setWorldRotationLockState(newState: Boolean) {
        currentState = currentState.copy(rotationIsWorldLocked = newState)
    }

    fun getRotationMirrorState(): Boolean = currentState.jointRotationIsMirrored

    fun setRotationMirrorState(newState: Boolean) {
        currentState = currentState.copy(jointRotationIsMirrored = newState)
    }

    fun revertJoint() {
        val joint = TODO("APR: use JVM equivalent") as LLJoint
        currentState.revertJointToBase(joint)
    }

    private fun addStateToUndo(stateToAdd: FSJointState) {
        modifiedThisSession = true

        val now = Instant.now()
        val elapsed = now.toEpochMilli() - timeLastUpdatedCurrentState.toEpochMilli()
        timeLastUpdatedCurrentState = now

        if (elapsed < UNDO_UPDATE_INTERVAL_MS) return

        if (undoneJointStatesIndex > 0) {
            repeat(undoneJointStatesIndex + 1) {
                if (lastSetJointStates.isNotEmpty()) lastSetJointStates.removeFirst()
            }
            undoneJointStatesIndex = 0
        }

        lastSetJointStates.addFirst(stateToAdd)

        while (lastSetJointStates.size > MAXIMUM_UNDO_QUEUE_LENGTH) {
            lastSetJointStates.removeLast()
        }
    }

    private fun undoLastStateChange(thingToSet: FSJointState): FSJointState {
        if (lastSetJointStates.isEmpty()) return thingToSet

        if (undoneJointStatesIndex == 0) lastSetJointStates.addFirst(thingToSet)

        undoneJointStatesIndex = (undoneJointStatesIndex + 1).coerceIn(0, lastSetJointStates.size - 1)
        return lastSetJointStates.elementAt(undoneJointStatesIndex)
    }

    private fun redoLastStateChange(thingToSet: FSJointState): FSJointState {
        if (lastSetJointStates.isEmpty()) return thingToSet
        if (undoneJointStatesIndex == 0) return thingToSet

        undoneJointStatesIndex = (undoneJointStatesIndex - 1).coerceIn(0, lastSetJointStates.size - 1)
        val result = lastSetJointStates.elementAt(undoneJointStatesIndex)
        if (undoneJointStatesIndex == 0) lastSetJointStates.removeFirst()
        return result
    }

    data class FSJointState(
        val rotation: LLQuaternion = LLQuaternion.DEFAULT,
        val position: LLVector3 = LLVector3.ZERO,
        val scale: LLVector3 = LLVector3.ZERO,
        val rotationIsWorldLocked: Boolean = false,
        val poserChangeType: EPoserChangeType = EPoserChangeType.POSER_CHANGE_DEFAULT,
        val jointRotationIsMirrored: Boolean = false,
        val userSpecifiedBaseZero: Boolean = false,
        private val startingRotation: LLQuaternion = LLQuaternion.DEFAULT,
        private val baseRotation: LLQuaternion = LLQuaternion.DEFAULT,
        private val basePosition: LLVector3 = LLVector3.ZERO,
        private val baseScale: LLVector3 = LLVector3.ZERO,
        val basePriority: JointPriority = JointPriority.LOW_PRIORITY,
    ) {
        constructor(joint: LLJoint) : this(
            startingRotation = joint.getRotation(),
            baseRotation = joint.getRotation(),
            basePosition = joint.getPosition(),
            baseScale = joint.getScale(),
        )

        fun getTargetRotation(): LLQuaternion = rotation * baseRotation
        fun getTargetPosition(): LLVector3 = position + basePosition
        fun getTargetScale(): LLVector3 = scale + baseScale

        fun baseRotationIsZero(): Boolean = baseRotation == LLQuaternion.DEFAULT

        fun reflect(): FSJointState {
            val newBase = baseRotation.copy(x = -baseRotation.x, z = -baseRotation.z)
            val newRot = rotation.copy(x = -rotation.x, z = -rotation.z)
            return copy(
                baseRotation = newBase,
                rotation = newRot,
                jointRotationIsMirrored = !jointRotationIsMirrored,
            )
        }

        fun reflectBase(): FSJointState =
            copy(baseRotation = baseRotation.copy(x = -baseRotation.x, z = -baseRotation.z))

        fun cloneRotationFrom(other: FSJointState): FSJointState =
            copy(baseRotation = other.baseRotation, rotation = other.rotation, userSpecifiedBaseZero = other.userSpecifiedBaseZero)

        fun cloneBaseRotationFrom(other: FSJointState): FSJointState =
            copy(baseRotation = other.baseRotation)

        fun reset(): FSJointState = copy(
            userSpecifiedBaseZero = false,
            rotationIsWorldLocked = false,
            jointRotationIsMirrored = false,
            poserChangeType = EPoserChangeType.POSER_CHANGE_DEFAULT,
            baseRotation = startingRotation,
            rotation = LLQuaternion.DEFAULT,
            position = LLVector3.ZERO,
            scale = LLVector3.ZERO,
        )

        fun withZeroedBase(lockInBvh: Boolean): FSJointState = copy(
            basePriority = JointPriority.LOW_PRIORITY,
            baseRotation = LLQuaternion.DEFAULT,
            jointRotationIsMirrored = false,
            userSpecifiedBaseZero = lockInBvh,
        )

        fun revertJointToBase(joint: LLJoint?) {
            joint ?: return
            joint.setRotation(baseRotation)
            joint.setPosition(basePosition)
            joint.setScale(baseScale)
        }

        fun updateFromJointProperties(zeroBase: Boolean, rotation: LLQuaternion, position: LLVector3, scale: LLVector3): LLQuaternion {
            TODO("APR: use JVM equivalent for quaternion conjugate/multiply operations")
        }

        fun resetBaseRotation(rotation: LLQuaternion, priority: JointPriority): FSJointState {
            if (userSpecifiedBaseZero) return this
            if (priority < basePriority) return this
            if (rotation == LLQuaternion.DEFAULT) return this
            return copy(basePriority = priority, baseRotation = rotation)
        }

        fun resetBasePosition(position: LLVector3, priority: JointPriority): FSJointState {
            if (priority < basePriority) return this
            return copy(basePriority = priority, basePosition = position)
        }

        fun resetBaseScale(scale: LLVector3, priority: JointPriority): FSJointState {
            if (priority < basePriority) return this
            if (scale.isZero()) return this
            return copy(basePriority = priority, baseScale = scale)
        }
    }
}
