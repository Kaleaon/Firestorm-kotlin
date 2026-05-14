package com.firestorm.newview

import java.util.UUID
import kotlin.math.*

const val PHYSICS_MOTION_FADEIN_TIME: Float = 1.0f
const val PHYSICS_MOTION_FADEOUT_TIME: Float = 1.0f

private const val MIN_REQUIRED_PIXEL_AREA_AVATAR_PHYSICS_MOTION: Float = 0f
private const val TIME_ITERATION_STEP_MAX: Float = 0.05f

private fun llsgn(a: Double): Double = if (a >= 0.0) 1.0 else -1.0

class LLPhysicsMotion(
    private val mParamDriverName: String,
    private val mJointName: String,
    private val mCharacter: LLCharacter,
    private val mMotionDirectionVec: FloatArray,
    private val mParamControllers: MutableMap<String, String>
) {
    enum class EParamName {
        SMOOTHING, MASS, GRAVITY, SPRING, GAIN, DAMPING, DRAG, MAX_EFFECT, NUM_PARAMS
    }

    private var mParamDriver: LLViewerVisualParam? = null
    private val mJointState: LLJointState = LLJointState()
    private var mLastTime: Float = 0f
    private var mPosition_local: Float = 0f
    private var mVelocityJoint_local: Float = 0f
    private var mAccelerationJoint_local: Float = 0f
    private var mVelocity_local: Float = 0f
    private var mPositionLastUpdate_local: Float = 0f
    private var mPosition_world: FloatArray = FloatArray(3)
    private val mParamCache: Array<LLVisualParam?> = arrayOfNulls(EParamName.NUM_PARAMS.ordinal)

    fun initialize(): Boolean {
        if (!mJointState.setJoint(mCharacter.getJoint(mJointName))) return false
        mJointState.setUsage(LLJointState.ROT)
        mParamDriver = mCharacter.getVisualParam(mParamDriverName) as? LLViewerVisualParam
        return mParamDriver != null
    }

    fun getJointState(): LLJointState = mJointState

    private val controllerKeyNames = arrayOf(
        "Smoothing", "Mass", "Gravity", "Spring", "Gain", "Damping", "Drag", "MaxEffect"
    )

    private fun getParamValue(param: EParamName): Float {
        val idx = param.ordinal
        if (mParamCache[idx] == null) {
            val entry = mParamControllers[controllerKeyNames[idx]]
            if (entry == null) return sDefaultController[controllerKeyNames[idx]] ?: 0f
            mParamCache[idx] = mCharacter.getVisualParam(entry)
        }
        return mParamCache[idx]?.getWeight() ?: (sDefaultController[controllerKeyNames[idx]] ?: 0f)
    }

    private fun setParamValue(param: LLViewerVisualParam, newValueNormalized: Float, behaviorMaxEffect: Float) {
        val valueMin = param.getMinWeight()
        val valueMax = param.getMaxWeight()
        val minVal = 0.5f - behaviorMaxEffect / 2f
        val maxVal = 0.5f + behaviorMaxEffect / 2f
        val newValueRescaled = minVal + (maxVal - minVal) * newValueNormalized
        val newValueLocal = valueMin + (valueMax - valueMin) * newValueRescaled
        mCharacter.setVisualParamWeight(param, newValueLocal, false)
    }

    private fun toLocal(world: FloatArray): Float {
        val joint = mJointState.getJoint() ?: return 0f
        val rotationWorld = joint.getWorldRotation()
        val dirWorld = rotateVector(mMotionDirectionVec, rotationWorld)
        val len = sqrt(dirWorld[0]*dirWorld[0] + dirWorld[1]*dirWorld[1] + dirWorld[2]*dirWorld[2]).toFloat()
        val normalized = if (len > 0f) floatArrayOf(dirWorld[0]/len, dirWorld[1]/len, dirWorld[2]/len) else dirWorld
        return world[0]*normalized[0] + world[1]*normalized[1] + world[2]*normalized[2]
    }

    private fun calculateVelocity_local(timeDelta: Float): Float {
        val worldToModelScale = 100.0f
        val joint = mJointState.getJoint() ?: return 0f
        val positionWorld = joint.getWorldPosition()
        val positionChange = floatArrayOf(
            (positionWorld[0] - mPosition_world[0]) * worldToModelScale,
            (positionWorld[1] - mPosition_world[1]) * worldToModelScale,
            (positionWorld[2] - mPosition_world[2]) * worldToModelScale
        )
        return toLocal(positionChange) / timeDelta
    }

    private fun calculateAcceleration_local(velocityLocal: Float, timeDelta: Float): Float {
        val smoothing = 3.0f
        val accelerationLocal = (velocityLocal - mVelocityJoint_local) / timeDelta
        return accelerationLocal * (1.0f / smoothing) + mAccelerationJoint_local * ((smoothing - 1.0f) / smoothing)
    }

    fun onUpdate(time: Float): Boolean {
        val driver = mParamDriver ?: return false
        if (mLastTime == 0f || mLastTime >= time) {
            mLastTime = time
            return false
        }
        val timeDelta = time - mLastTime
        if (timeDelta > 1.0f) {
            mLastTime = time
            return false
        }

        val lodFactor = LLVOAvatar.sPhysicsLODFactor
        if (lodFactor == 0f) return true

        val joint = mJointState.getJoint() ?: return false
        val behaviorMass = getParamValue(EParamName.MASS)
        val behaviorGravity = getParamValue(EParamName.GRAVITY)
        val behaviorSpring = getParamValue(EParamName.SPRING)
        val behaviorGain = getParamValue(EParamName.GAIN)
        val behaviorDamping = getParamValue(EParamName.DAMPING)
        val behaviorDrag = getParamValue(EParamName.DRAG)
        val behaviorMaxEffect = getParamValue(EParamName.MAX_EFFECT)

        val positionUserLocal =
            (driver.getWeight() - driver.getMinWeight()) / (driver.getMaxWeight() - driver.getMinWeight())

        val jointLocalFactor = 30.0f
        val velocityJointLocal = calculateVelocity_local(timeDelta * jointLocalFactor)
        val accelerationJointLocal = calculateAcceleration_local(velocityJointLocal, timeDelta * jointLocalFactor)

        var updateVisuals = false
        val steps = (timeDelta / TIME_ITERATION_STEP_MAX).toInt() + 1
        val timeIterationStep = timeDelta / steps.toFloat()

        for (step in 0 until steps) {
            val positionCurrentLocal = mPosition_local.coerceIn(0f, 1f)
            if (behaviorMaxEffect == 0f && positionCurrentLocal == positionUserLocal) return updateVisuals

            val springLength = positionCurrentLocal - positionUserLocal
            val forceSpring = -springLength * behaviorSpring
            val forceAccel = behaviorGain * (accelerationJointLocal * behaviorMass)
            val forceGravity = toLocal(floatArrayOf(0f, 0f, 1f)) * behaviorGravity * behaviorMass
            val forceDamping = -behaviorDamping * mVelocity_local
            val forceDrag = (0.5 * behaviorDrag * velocityJointLocal * velocityJointLocal * llsgn(velocityJointLocal.toDouble())).toFloat()
            val forceNet = forceAccel + forceGravity + forceSpring + forceDamping + forceDrag

            val accelerationNewLocal = forceNet / behaviorMass
            val maxVelocity = 100.0f
            var velocityNewLocal = (mVelocity_local + accelerationNewLocal * timeIterationStep)
                .coerceIn(-maxVelocity, maxVelocity)

            var positionNewLocal = positionCurrentLocal + velocityNewLocal * timeIterationStep
            if (behaviorMaxEffect == 0f) positionNewLocal = positionUserLocal

            if ((positionNewLocal < 0f && velocityNewLocal < 0f) ||
                (positionNewLocal > 1f && velocityNewLocal > 0f)
            ) {
                velocityNewLocal = 0f
            }

            if (mPosition_local.isNaN() || mVelocity_local.isNaN() || positionNewLocal.isNaN()) {
                positionNewLocal = 0f
                mVelocity_local = 0f
                mVelocityJoint_local = 0f
                mAccelerationJoint_local = 0f
                mPosition_local = 0f
                mPosition_world = FloatArray(3)
            }

            val positionNewLocalClamped = positionNewLocal.coerceIn(0f, 1f)
            val driverParam = driver as? LLDriverParam
            checkNotNull(driverParam)
            if (driverParam.getGroup() != VISUAL_PARAM_GROUP_TWEAKABLE &&
                driverParam.getGroup() != VISUAL_PARAM_GROUP_TWEAKABLE_NO_TRANSMIT
            ) {
                mCharacter.setVisualParamWeight(driverParam, 0f, false)
            }
            for (i in 0 until driverParam.getDrivenParamsCount()) {
                val drivenParam = driverParam.getDrivenParam(i) as LLViewerVisualParam
                setParamValue(drivenParam, positionNewLocalClamped, behaviorMaxEffect)
            }

            val areaForMaxSettings = 0.0f
            val areaForMinSettings = 1400.0f
            val areaForThisSetting = areaForMaxSettings + (areaForMinSettings - areaForMaxSettings) * (1.0f - lodFactor)
            val pixelArea = sqrt(mCharacter.getPixelArea().toDouble()).toFloat()
            val isSelf = mCharacter is LLVOAvatarSelf
            if (pixelArea > areaForThisSetting || isSelf) {
                val positionDiffLocal = abs(mPositionLastUpdate_local - positionNewLocalClamped)
                val minDelta = (1.0001f - lodFactor) * 0.4f
                if (abs(positionDiffLocal) > minDelta) {
                    updateVisuals = true
                    mPositionLastUpdate_local = positionNewLocal
                }
            }
            mVelocity_local = velocityNewLocal
            mAccelerationJoint_local = accelerationJointLocal
            mPosition_local = positionNewLocal
        }
        mLastTime = time
        mPosition_world = joint.getWorldPosition()
        mVelocityJoint_local = velocityJointLocal
        return updateVisuals
    }

    companion object {
        val sDefaultController: MutableMap<String, Float> = mutableMapOf(
            "Mass"      to 0.2f,
            "Gravity"   to 0.0f,
            "Damping"   to 0.05f,
            "Drag"      to 0.15f,
            "MaxEffect" to 0.1f,
            "Spring"    to 0.1f,
            "Gain"      to 10.0f
        )
    }
}

open class LLPhysicsMotionController(id: UUID) : LLMotion(id) {

    private var mCharacter: LLCharacter? = null
    private val mMotions: MutableList<LLPhysicsMotion> = mutableListOf()

    init {
        mName = "breast_motion"
    }

    open fun getLoop(): Boolean = true
    open fun getDuration(): Float = 0.0f
    open fun getEaseInDuration(): Float = PHYSICS_MOTION_FADEIN_TIME
    open fun getEaseOutDuration(): Float = PHYSICS_MOTION_FADEOUT_TIME
    open fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_AVATAR_PHYSICS_MOTION
    open fun getPriority(): LLJoint.JointPriority = LLJoint.JointPriority.MEDIUM_PRIORITY
    open fun getBlendType(): LLMotionBlendType = LLMotionBlendType.ADDITIVE_BLEND

    open fun onActivate(): Boolean = true

    open fun onDeactivate() {}

    open fun onInitialize(character: LLCharacter): LLMotionInitStatus {
        mCharacter = character
        mMotions.clear()

        data class MotionDef(
            val driver: String, val joint: String,
            val direction: FloatArray, val controllers: Map<String, String>
        )

        val defs = listOf(
            MotionDef("Breast_Physics_InOut_Controller", "mChest", floatArrayOf(-1f, 0f, 0f), mapOf(
                "Mass" to "Breast_Physics_Mass", "Gravity" to "Breast_Physics_Gravity",
                "Drag" to "Breast_Physics_Drag", "Damping" to "Breast_Physics_InOut_Damping",
                "MaxEffect" to "Breast_Physics_InOut_Max_Effect", "Spring" to "Breast_Physics_InOut_Spring",
                "Gain" to "Breast_Physics_InOut_Gain")),
            MotionDef("Breast_Physics_UpDown_Controller", "mChest", floatArrayOf(0f, 0f, 1f), mapOf(
                "Mass" to "Breast_Physics_Mass", "Gravity" to "Breast_Physics_Gravity",
                "Drag" to "Breast_Physics_Drag", "Damping" to "Breast_Physics_UpDown_Damping",
                "MaxEffect" to "Breast_Physics_UpDown_Max_Effect", "Spring" to "Breast_Physics_UpDown_Spring",
                "Gain" to "Breast_Physics_UpDown_Gain")),
            MotionDef("Breast_Physics_LeftRight_Controller", "mChest", floatArrayOf(0f, -1f, 0f), mapOf(
                "Mass" to "Breast_Physics_Mass", "Gravity" to "Breast_Physics_Gravity",
                "Drag" to "Breast_Physics_Drag", "Damping" to "Breast_Physics_LeftRight_Damping",
                "MaxEffect" to "Breast_Physics_LeftRight_Max_Effect", "Spring" to "Breast_Physics_LeftRight_Spring",
                "Gain" to "Breast_Physics_LeftRight_Gain")),
            MotionDef("Butt_Physics_UpDown_Controller", "mPelvis", floatArrayOf(0f, 0f, -1f), mapOf(
                "Mass" to "Butt_Physics_Mass", "Gravity" to "Butt_Physics_Gravity",
                "Drag" to "Butt_Physics_Drag", "Damping" to "Butt_Physics_UpDown_Damping",
                "MaxEffect" to "Butt_Physics_UpDown_Max_Effect", "Spring" to "Butt_Physics_UpDown_Spring",
                "Gain" to "Butt_Physics_UpDown_Gain")),
            MotionDef("Butt_Physics_LeftRight_Controller", "mPelvis", floatArrayOf(0f, -1f, 0f), mapOf(
                "Mass" to "Butt_Physics_Mass", "Gravity" to "Butt_Physics_Gravity",
                "Drag" to "Butt_Physics_Drag", "Damping" to "Butt_Physics_LeftRight_Damping",
                "MaxEffect" to "Butt_Physics_LeftRight_Max_Effect", "Spring" to "Butt_Physics_LeftRight_Spring",
                "Gain" to "Butt_Physics_LeftRight_Gain")),
            MotionDef("Belly_Physics_UpDown_Controller", "mPelvis", floatArrayOf(0f, 0f, -1f), mapOf(
                "Mass" to "Belly_Physics_Mass", "Gravity" to "Belly_Physics_Gravity",
                "Drag" to "Belly_Physics_Drag", "Damping" to "Belly_Physics_UpDown_Damping",
                "MaxEffect" to "Belly_Physics_UpDown_Max_Effect", "Spring" to "Belly_Physics_UpDown_Spring",
                "Gain" to "Belly_Physics_UpDown_Gain"))
        )

        for (def in defs) {
            val motion = LLPhysicsMotion(def.driver, def.joint, character, def.direction, def.controllers.toMutableMap())
            if (!motion.initialize()) return LLMotionInitStatus.STATUS_FAILURE
            addMotion(motion)
        }
        return LLMotionInitStatus.STATUS_SUCCESS
    }

    open fun onUpdate(time: Float, jointMask: ByteArray?): Boolean {
        if (!avatarPhysicsEnabled()) return true
        var updateVisuals = false
        for (motion in mMotions) {
            updateVisuals = updateVisuals or motion.onUpdate(time)
        }
        if (updateVisuals) mCharacter?.updateVisualParams()
        return true
    }

    fun getCharacter(): LLCharacter? = mCharacter

    protected fun addMotion(motion: LLPhysicsMotion) {
        addJointState(motion.getJointState())
        mMotions.add(motion)
    }

    companion object {
        fun create(id: UUID): LLMotion = LLPhysicsMotionController(id)
    }
}

private fun avatarPhysicsEnabled(): Boolean {
    System.err.println("LLPhysicsMotion: avatarPhysicsEnabled not yet implemented")
    return false
}
private fun rotateVector(v: FloatArray, q: FloatArray): FloatArray {
    // no-op
    return FloatArray(3)
}

private const val VISUAL_PARAM_GROUP_TWEAKABLE: Int = 0
private const val VISUAL_PARAM_GROUP_TWEAKABLE_NO_TRANSMIT: Int = 1
