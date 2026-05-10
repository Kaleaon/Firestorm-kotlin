package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.llClamp
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

private const val PHYSICS_MOTION_FADEIN_TIME  = 1.0f
private const val PHYSICS_MOTION_FADEOUT_TIME = 1.0f
private const val MIN_REQUIRED_PIXEL_AREA_AVATAR_PHYSICS_MOTION = 0.0f
private const val TIME_ITERATION_STEP_MAX = 0.05f

// World-to-model scale matching the C++ world_to_model_scale constant.
private const val WORLD_TO_MODEL_SCALE = 100.0f
// Smoothing constant for acceleration (matches static smoothing = 3.0 in C++).
private const val ACCELERATION_SMOOTHING = 3.0f
private const val JOINT_LOCAL_FACTOR = 30.0f
private const val MAX_VELOCITY = 100.0f

// ---------------------------------------------------------------------------
// PhysicsParam — names the physics parameters a PhysicsMotionTrack reads.
// Mirrors C++ LLPhysicsMotion::eParamName.
// ---------------------------------------------------------------------------

private enum class PhysicsParam {
    SMOOTHING, MASS, GRAVITY, SPRING, GAIN, DAMPING, DRAG, MAX_EFFECT;
}

private val PARAM_KEYS = mapOf(
    PhysicsParam.SMOOTHING  to "Smoothing",
    PhysicsParam.MASS       to "Mass",
    PhysicsParam.GRAVITY    to "Gravity",
    PhysicsParam.SPRING     to "Spring",
    PhysicsParam.GAIN       to "Gain",
    PhysicsParam.DAMPING    to "Damping",
    PhysicsParam.DRAG       to "Drag",
    PhysicsParam.MAX_EFFECT to "MaxEffect"
)

private val DEFAULT_CONTROLLER: Map<String, Float> = mapOf(
    "Mass"      to 0.2f,
    "Gravity"   to 0.0f,
    "Damping"   to 0.05f,
    "Drag"      to 0.15f,
    "MaxEffect" to 0.1f,
    "Spring"    to 0.1f,
    "Gain"      to 10.0f
)

// ---------------------------------------------------------------------------
// PhysicsMotionTrack
//
// One degree-of-freedom physics simulation for a body-part morph controller.
// Mirrors the internal C++ class LLPhysicsMotion (not the controller).
// ---------------------------------------------------------------------------

class PhysicsMotionTrack(
    private val paramDriverName: String,
    private val jointName: String,
    private val character: LLCharacter,
    private val motionDirectionVec: Vector3,
    private val controllers: Map<String, String>
) {
    val jointState: JointState = JointState()

    private var paramDriver: VisualParam? = null
    private val paramCache: MutableMap<PhysicsParam, VisualParam?> = mutableMapOf()

    private var lastTime: Float = 0f
    private var positionLocal: Float = 0f
    private var velocityLocal: Float = 0f
    private var accelerationJointLocal: Float = 0f
    private var velocityJointLocal: Float = 0f
    private var positionLastUpdateLocal: Float = 0f
    private var positionWorld: Vector3 = Vector3()

    fun initialize(): Boolean {
        val joint = character.getJoint(jointName) ?: return false
        jointState.setJoint(joint)
        jointState.usage = JointState.Usage.ROT.mask

        paramDriver = character.getVisualParam(paramDriverName)
        if (paramDriver == null) {
            System.err.println("PhysicsMotionTrack: cannot find driver param '$paramDriverName'")
            return false
        }
        return true
    }

    private fun getParamValue(param: PhysicsParam): Float {
        val key = PARAM_KEYS[param] ?: return 0f
        if (!paramCache.containsKey(param)) {
            val controllerParamName = controllers[key]
            paramCache[param] = if (controllerParamName != null)
                character.getVisualParam(controllerParamName)
            else null
        }
        return paramCache[param]?.weight ?: DEFAULT_CONTROLLER[key] ?: 0f
    }

    private fun toLocal(world: Vector3): Float {
        val joint = jointState.joint ?: return 0f
        val worldRot = joint.getWorldRotation()
        val dirWorld = worldRot * motionDirectionVec
        val dirLen = sqrt(dirWorld.x * dirWorld.x + dirWorld.y * dirWorld.y + dirWorld.z * dirWorld.z)
        val dirNorm = if (dirLen > 1e-6f) dirWorld * (1f / dirLen) else dirWorld
        return world.x * dirNorm.x + world.y * dirNorm.y + world.z * dirNorm.z
    }

    private fun calculateVelocityLocal(timeDelta: Float): Float {
        val joint = jointState.joint ?: return 0f
        val posWorld = joint.getWorldPosition()
        val change = (posWorld - positionWorld) * WORLD_TO_MODEL_SCALE
        return if (timeDelta > 0f) toLocal(change) / timeDelta else 0f
    }

    private fun calculateAccelerationLocal(velocityLocal: Float, timeDelta: Float): Float {
        val rawAccel = if (timeDelta > 0f) (velocityLocal - velocityJointLocal) / timeDelta else 0f
        return rawAccel * (1f / ACCELERATION_SMOOTHING) +
               accelerationJointLocal * ((ACCELERATION_SMOOTHING - 1f) / ACCELERATION_SMOOTHING)
    }

    /** Returns true if visual params changed and the character should be re-rendered. */
    fun onUpdate(time: Float, lodFactor: Float, isSelf: Boolean): Boolean {
        val driver = paramDriver ?: return false

        if (lastTime == 0f || lastTime >= time) {
            lastTime = time
            return false
        }

        val timeDelta = time - lastTime
        if (timeDelta > 1f) {
            lastTime = time
            return false
        }
        if (lodFactor == 0f) return true

        val mass       = getParamValue(PhysicsParam.MASS)
        val gravity    = getParamValue(PhysicsParam.GRAVITY)
        val spring     = getParamValue(PhysicsParam.SPRING)
        val gain       = getParamValue(PhysicsParam.GAIN)
        val damping    = getParamValue(PhysicsParam.DAMPING)
        val drag       = getParamValue(PhysicsParam.DRAG)
        val maxEffect  = getParamValue(PhysicsParam.MAX_EFFECT)

        val minW = driver.defaultWeight    // stand-in for getMinWeight (not stored separately)
        val maxW = driver.weight.coerceAtLeast(minW + 0.0001f)
        val positionUserLocal = ((driver.weight - minW) / (maxW - minW)).coerceIn(0f, 1f)

        val velocityJoint = calculateVelocityLocal(timeDelta * JOINT_LOCAL_FACTOR)
        val accelJoint    = calculateAccelerationLocal(velocityJoint, timeDelta * JOINT_LOCAL_FACTOR)

        var updateVisuals = false
        val steps = (timeDelta / TIME_ITERATION_STEP_MAX).toInt() + 1
        val stepTime = timeDelta / steps.toFloat()

        repeat(steps) {
            val posCurrentLocal = positionLocal.coerceIn(0f, 1f)

            if (maxEffect == 0f && posCurrentLocal == positionUserLocal) return@repeat

            val springForce  = -(posCurrentLocal - positionUserLocal) * spring
            val accelForce   = gain * accelJoint * mass
            val gravityForce = toLocal(Vector3(0f, 0f, 1f)) * gravity * mass
            val dampingForce = -damping * velocityLocal
            val dragForce    = 0.5f * drag * velocityJoint * velocityJoint * sign(velocityJoint)

            val forceNet = accelForce + gravityForce + springForce + dampingForce + dragForce
            val accelNew = forceNet / mass.coerceAtLeast(1e-6f)

            var velNew = (velocityLocal + accelNew * stepTime).coerceIn(-MAX_VELOCITY, MAX_VELOCITY)
            var posNew = posCurrentLocal + velNew * stepTime
            if (maxEffect == 0f) posNew = positionUserLocal

            // Clamp velocity at the param limits
            if ((posNew < 0f && velNew < 0f) || (posNew > 1f && velNew > 0f)) velNew = 0f

            // NaN guard
            if (positionLocal.isNaN() || velocityLocal.isNaN() || posNew.isNaN()) {
                posNew = 0f; velNew = 0f
                accelerationJointLocal = 0f; velocityJointLocal = 0f
                positionLocal = 0f; positionWorld = Vector3()
            }

            val posClamped = posNew.coerceIn(0f, 1f)
            applyDrivenParams(driver, posClamped, maxEffect)

            // Decide if visual params need refreshing (LOD-gated)
            val areaForMinSettings = 1400f
            val areaForThisSetting = areaForMinSettings * (1f - lodFactor)
            val pixelArea = sqrt(character.getPixelArea())
            if (pixelArea > areaForThisSetting || isSelf) {
                val minDelta = (1.0001f - lodFactor) * 0.4f
                if (abs(positionLastUpdateLocal - posClamped) > minDelta) {
                    updateVisuals = true
                    positionLastUpdateLocal = posNew
                }
            }

            velocityLocal = velNew
            positionLocal = posNew
        }

        lastTime = time
        positionWorld = jointState.joint?.getWorldPosition() ?: Vector3()
        velocityJointLocal = velocityJoint
        accelerationJointLocal = accelJoint

        return updateVisuals
    }

    private fun applyDrivenParams(driver: VisualParam, posNormalized: Float, maxEffect: Float) {
        val minVal = 0.5f - maxEffect / 2f
        val maxVal = 0.5f + maxEffect / 2f
        val rescaled = minVal + (maxVal - minVal) * posNormalized
        // Param stores its own [min,max] via defaultWeight / weight range.
        // Without LLDriverParam we apply a best-effort direct weight set on the driver itself.
        val minW  = driver.defaultWeight
        val maxW  = minW + 1f   // normalized range; real driver params supply full range
        val value = minW + (maxW - minW) * rescaled
        character.setVisualParamWeight(driver.id, value)
    }
}

// ---------------------------------------------------------------------------
// PhysicsMotionController
//
// An LLMotion that wraps a collection of PhysicsMotionTracks covering breast
// bounce/sway/cleavage, butt bounce/sway, and belly bounce.
// Mirrors C++ LLPhysicsMotionController (newview/llphysicsmotion.cpp).
// ---------------------------------------------------------------------------

class PhysicsMotionController(id: LLUUID) : LLMotion(id) {

    private var character: LLCharacter? = null
    private val motions: MutableList<PhysicsMotionTrack> = mutableListOf()

    companion object {
        fun create(id: LLUUID): PhysicsMotionController = PhysicsMotionController(id)

        /**
         * LOD factor in [0,1]: 1.0 = highest fidelity, 0 = physics disabled.
         * Mirrors C++ LLVOAvatar::sPhysicsLODFactor.
         */
        var physicsLodFactor: Float = 1.0f
    }

    init {
        name = "breast_motion"
    }

    // ---- LLMotion overrides -------------------------------------------------

    override fun getLoop(): Boolean = true
    override fun getDuration(): Float = 0f
    override fun getEaseInDuration(): Float  = PHYSICS_MOTION_FADEIN_TIME
    override fun getEaseOutDuration(): Float = PHYSICS_MOTION_FADEOUT_TIME
    override fun getPriority(): JointPriority = JointPriority.MEDIUM
    override fun getBlendType(): MotionBlendType = MotionBlendType.ADDITIVE_BLEND
    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_AVATAR_PHYSICS_MOTION

    override fun onInitialize(character: Character): MotionInitStatus {
        this.character = character as? LLCharacter ?: return MotionInitStatus.FAILURE
        val ch = this.character!!
        motions.clear()

        fun addTrack(
            driverName: String, jointName: String, dir: Vector3,
            controllers: Map<String, String>
        ): Boolean {
            val track = PhysicsMotionTrack(driverName, jointName, ch, dir, controllers)
            if (!track.initialize()) return false
            motions.add(track)
            return true
        }

        // Breast cleavage (in-out, X axis)
        if (!addTrack(
            "Breast_Physics_InOut_Controller", "mChest", Vector3(-1f, 0f, 0f),
            mapOf(
                "Mass"      to "Breast_Physics_Mass",
                "Gravity"   to "Breast_Physics_Gravity",
                "Drag"      to "Breast_Physics_Drag",
                "Damping"   to "Breast_Physics_InOut_Damping",
                "MaxEffect" to "Breast_Physics_InOut_Max_Effect",
                "Spring"    to "Breast_Physics_InOut_Spring",
                "Gain"      to "Breast_Physics_InOut_Gain"
            )
        )) return MotionInitStatus.FAILURE

        // Breast bounce (up-down, Z axis)
        if (!addTrack(
            "Breast_Physics_UpDown_Controller", "mChest", Vector3(0f, 0f, 1f),
            mapOf(
                "Mass"      to "Breast_Physics_Mass",
                "Gravity"   to "Breast_Physics_Gravity",
                "Drag"      to "Breast_Physics_Drag",
                "Damping"   to "Breast_Physics_UpDown_Damping",
                "MaxEffect" to "Breast_Physics_UpDown_Max_Effect",
                "Spring"    to "Breast_Physics_UpDown_Spring",
                "Gain"      to "Breast_Physics_UpDown_Gain"
            )
        )) return MotionInitStatus.FAILURE

        // Breast sway (left-right, -Y axis)
        if (!addTrack(
            "Breast_Physics_LeftRight_Controller", "mChest", Vector3(0f, -1f, 0f),
            mapOf(
                "Mass"      to "Breast_Physics_Mass",
                "Gravity"   to "Breast_Physics_Gravity",
                "Drag"      to "Breast_Physics_Drag",
                "Damping"   to "Breast_Physics_LeftRight_Damping",
                "MaxEffect" to "Breast_Physics_LeftRight_Max_Effect",
                "Spring"    to "Breast_Physics_LeftRight_Spring",
                "Gain"      to "Breast_Physics_LeftRight_Gain"
            )
        )) return MotionInitStatus.FAILURE

        // Butt bounce (down, -Z axis)
        if (!addTrack(
            "Butt_Physics_UpDown_Controller", "mPelvis", Vector3(0f, 0f, -1f),
            mapOf(
                "Mass"      to "Butt_Physics_Mass",
                "Gravity"   to "Butt_Physics_Gravity",
                "Drag"      to "Butt_Physics_Drag",
                "Damping"   to "Butt_Physics_UpDown_Damping",
                "MaxEffect" to "Butt_Physics_UpDown_Max_Effect",
                "Spring"    to "Butt_Physics_UpDown_Spring",
                "Gain"      to "Butt_Physics_UpDown_Gain"
            )
        )) return MotionInitStatus.FAILURE

        // Butt sway (left-right, -Y axis)
        if (!addTrack(
            "Butt_Physics_LeftRight_Controller", "mPelvis", Vector3(0f, -1f, 0f),
            mapOf(
                "Mass"      to "Butt_Physics_Mass",
                "Gravity"   to "Butt_Physics_Gravity",
                "Drag"      to "Butt_Physics_Drag",
                "Damping"   to "Butt_Physics_LeftRight_Damping",
                "MaxEffect" to "Butt_Physics_LeftRight_Max_Effect",
                "Spring"    to "Butt_Physics_LeftRight_Spring",
                "Gain"      to "Butt_Physics_LeftRight_Gain"
            )
        )) return MotionInitStatus.FAILURE

        // Belly bounce (down, -Z axis)
        if (!addTrack(
            "Belly_Physics_UpDown_Controller", "mPelvis", Vector3(0f, 0f, -1f),
            mapOf(
                "Mass"      to "Belly_Physics_Mass",
                "Gravity"   to "Belly_Physics_Gravity",
                "Drag"      to "Belly_Physics_Drag",
                "Damping"   to "Belly_Physics_UpDown_Damping",
                "MaxEffect" to "Belly_Physics_UpDown_Max_Effect",
                "Spring"    to "Belly_Physics_UpDown_Spring",
                "Gain"      to "Belly_Physics_UpDown_Gain"
            )
        )) return MotionInitStatus.FAILURE

        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean = true

    override fun onUpdate(activeTime: Float): Boolean {
        val ch = character ?: return true

        // Mirrors C++ gSavedSettings["AvatarPhysics"] global flag.
        // Caller can disable physics globally by setting physicsLodFactor = 0.
        if (physicsLodFactor == 0f) return true

        var updateVisuals = false
        val isSelf = ch.motionController.isSelf
        for (track in motions) {
            updateVisuals = track.onUpdate(activeTime, physicsLodFactor, isSelf) || updateVisuals
        }

        if (updateVisuals) ch.updateVisualParams()
        return true
    }

    override fun onDeactivate() {}
}
