package com.firestorm.llcharacter

import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3

// Number of blender slots per joint (matches JSB_NUM_JOINT_STATES in llpose.h)
const val JSB_NUM_JOINT_STATES = 6

/**
 * LLPose — a weighted collection of [JointState] objects keyed by joint name.
 *
 * Translated from llpose.h / llpose.cpp.
 */
class Pose {
    private val jointMap: MutableMap<String, JointState> = mutableMapOf()
    private var weight: Float = 0f

    // Iterator support (mirrors getFirst/getNextJointState C++ pattern)
    private var iterator: Iterator<JointState>? = null

    fun getFirstJointState(): JointState? {
        val it = jointMap.values.iterator()
        iterator = it
        return if (it.hasNext()) it.next() else null
    }

    fun getNextJointState(): JointState? {
        val it = iterator ?: return null
        return if (it.hasNext()) it.next() else null
    }

    /** Add [jointState] if not already present; keyed by its joint's name. */
    fun addJointState(jointState: JointState): Boolean {
        val name = jointState.joint?.name ?: return false
        if (!jointMap.containsKey(name)) {
            jointMap[name] = jointState
        }
        return true
    }

    fun removeJointState(jointState: JointState): Boolean {
        val name = jointState.joint?.name ?: return false
        return jointMap.remove(name) != null
    }

    fun removeAllJointStates(): Boolean {
        jointMap.clear()
        return true
    }

    fun findJointState(joint: Joint): JointState? = jointMap[joint.name]
    fun findJointState(name: String): JointState? = jointMap[name]

    /** Propagate [w] to every [JointState] in this pose and cache it locally. */
    fun setWeight(w: Float) {
        for (js in jointMap.values) js.weight = w
        weight = w
    }

    fun getWeight(): Float = weight
    fun getNumJointStates(): Int = jointMap.size
}

/**
 * JointStateBlender — blends up to [JSB_NUM_JOINT_STATES] [JointState] objects
 * into a single joint transform, ordered by priority.
 *
 * Translated from llpose.h / llpose.cpp (LLJointStateBlender).
 */
class JointStateBlender {
    private val jointStates = arrayOfNulls<JointState>(JSB_NUM_JOINT_STATES)
    private val priorities = IntArray(JSB_NUM_JOINT_STATES) { Int.MIN_VALUE }
    private val additiveBlends = BooleanArray(JSB_NUM_JOINT_STATES)

    // Cached joint used when blending without immediate apply
    val jointCache: Joint = Joint()

    /** Insert [jointState] at the correct priority-ordered slot. Returns false if full. */
    fun addJointState(jointState: JointState, priority: Int, additiveBlend: Boolean): Boolean {
        for (i in 0 until JSB_NUM_JOINT_STATES) {
            if (jointStates[i] == null) {
                jointStates[i] = jointState
                priorities[i] = priority
                additiveBlends[i] = additiveBlend
                return true
            } else if (priority > priorities[i]) {
                // Shift lower-priority entries down
                for (j in JSB_NUM_JOINT_STATES - 1 downTo i + 1) {
                    jointStates[j] = jointStates[j - 1]
                    priorities[j] = priorities[j - 1]
                    additiveBlends[j] = additiveBlends[j - 1]
                }
                jointStates[i] = jointState
                priorities[i] = priority
                additiveBlends[i] = additiveBlend
                return true
            }
        }
        return false
    }

    /** Blend all accumulated joint states and apply to the target joint (or cache). */
    fun blendJointStates(applyNow: Boolean = true) {
        val first = jointStates[0] ?: return
        val targetJoint = if (applyNow) first.joint ?: return else jointCache

        var blendedPos   = targetJoint.position
        var blendedRot   = targetJoint.rotation
        var blendedScale = targetJoint.scale

        var addedPos   = Vector3()
        var addedRot   = Quaternion()
        var addedScale = Vector3()

        val sumWeights = FloatArray(3)          // [POS, ROT, SCALE]
        var sumUsage: UInt = 0u

        for (i in 0 until JSB_NUM_JOINT_STATES) {
            val jsp = jointStates[i] ?: break
            val usageMask = jsp.usage
            val w = jsp.weight
            if (w == 0f) continue

            if (additiveBlends[i]) {
                if (usageMask and JointState.Usage.POS.mask != 0u) {
                    val nw = minOf(1f, w + sumWeights[0])
                    addedPos = addedPos + jsp.position * (nw - sumWeights[0])
                }
                if (usageMask and JointState.Usage.SCALE.mask != 0u) {
                    val nw = minOf(1f, w + sumWeights[2])
                    addedScale = addedScale + jsp.scale * (nw - sumWeights[2])
                }
                if (usageMask and JointState.Usage.ROT.mask != 0u) {
                    val nw = minOf(1f, w + sumWeights[1])
                    addedRot = Quaternion.nlerp(nw - sumWeights[1], addedRot, jsp.rotation) * addedRot
                }
            } else {
                if (usageMask and JointState.Usage.POS.mask != 0u) {
                    blendedPos = if (sumUsage and JointState.Usage.POS.mask != 0u) {
                        val nw = minOf(1f, w + sumWeights[0])
                        val r = Quaternion.lerp(jsp.position, blendedPos, sumWeights[0] / nw)
                        sumWeights[0] = nw; r
                    } else { sumWeights[0] = w; jsp.position }
                }
                if (usageMask and JointState.Usage.SCALE.mask != 0u) {
                    blendedScale = if (sumUsage and JointState.Usage.SCALE.mask != 0u) {
                        val nw = minOf(1f, w + sumWeights[2])
                        val r = Quaternion.lerp(jsp.scale, blendedScale, sumWeights[2] / nw)
                        sumWeights[2] = nw; r
                    } else { sumWeights[2] = w; jsp.scale }
                }
                if (usageMask and JointState.Usage.ROT.mask != 0u) {
                    blendedRot = if (sumUsage and JointState.Usage.ROT.mask != 0u) {
                        val nw = minOf(1f, w + sumWeights[1])
                        val r = Quaternion.nlerp(sumWeights[1] / nw, jsp.rotation, blendedRot)
                        sumWeights[1] = nw; r
                    } else { sumWeights[1] = w; jsp.rotation }
                }
                sumUsage = sumUsage or usageMask
            }
        }

        targetJoint.position = blendedPos + addedPos
        targetJoint.scale    = blendedScale + addedScale
        targetJoint.rotation = addedRot * blendedRot

        if (applyNow) clear()
    }

    fun interpolate(u: Float) {
        val jsp = jointStates[0] ?: return
        val target = jsp.joint ?: return
        target.position = Quaternion.lerp(target.position, jointCache.position, u)
        target.scale    = Quaternion.lerp(target.scale,    jointCache.scale,    u)
        target.rotation = Quaternion.nlerp(u, target.rotation, jointCache.rotation)
    }

    fun resetCachedJoint() {
        val source = jointStates[0]?.joint ?: return
        jointCache.position = source.position
        jointCache.scale    = source.scale
        jointCache.rotation = source.rotation
    }

    fun clear() { for (i in 0 until JSB_NUM_JOINT_STATES) jointStates[i] = null }
}

/**
 * PoseBlender — manages a pool of [JointStateBlender]s and drives the final
 * blended pose applied to the skeleton each frame.
 *
 * Translated from llpose.h / llpose.cpp (LLPoseBlender).
 */
class PoseBlender {
    private val blenderPool: MutableMap<Joint, JointStateBlender> = mutableMapOf()
    private val activeBlenders: MutableList<JointStateBlender> = mutableListOf()
    val blendedPose = Pose()

    /** Register all joint states from [motion]'s pose into the blender pool. */
    fun addMotion(motion: Any): Boolean {
        TODO("Wire to Motion type once available")
    }

    fun blendAndApply() {
        for (jsb in activeBlenders) jsb.blendJointStates(applyNow = true)
        activeBlenders.clear()
    }

    fun blendAndCache(resetCachedJoints: Boolean) {
        for (jsb in activeBlenders) {
            if (resetCachedJoints) jsb.resetCachedJoint()
            jsb.blendJointStates(applyNow = false)
        }
    }

    fun interpolate(u: Float) { for (jsb in activeBlenders) jsb.interpolate(u) }

    fun clearBlenders() {
        for (jsb in activeBlenders) jsb.clear()
        activeBlenders.clear()
    }
}
