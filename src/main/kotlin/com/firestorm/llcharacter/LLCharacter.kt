// Converted from llcharacter.h / llcharacter.cpp — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// Visual parameter stub (mirrors LLVisualParam)
// ---------------------------------------------------------------------------

/**
 * Minimal representation of a morph/visual parameter.
 * The full implementation lives in the llappearance package.
 */
data class VisualParam(
    val id: Int,
    val name: String,
    var weight: Float = 0f,
    val defaultWeight: Float = 0f
)

// ---------------------------------------------------------------------------
// abstract class LLCharacter
//
// Root of the character hierarchy.  Mirrors C++ LLCharacter.
// Subclasses must supply the skeleton root and spatial query methods.
// Motion management is delegated to an embedded [MotionController].
// ---------------------------------------------------------------------------

abstract class LLCharacter {

    // ---- embedded controller -----------------------------------------------

    val motionController: MotionController = MotionController()

    // ---- per-instance state ------------------------------------------------

    protected var preferredPelvisHeight: Float = 0f
    var sex: Sex = Sex.FEMALE
    var appearanceSerialNum: UInt = 0u
    var skeletonSerialNum: UInt   = 0u
    var hoverOffset: Vector3 = Vector3()

    private val animationData: MutableMap<String, Any?> = mutableMapOf()

    // keyed by param-id; name map maintained in parallel
    private val visualParamById:   MutableMap<Int, VisualParam>    = mutableMapOf()
    private val visualParamByName: MutableMap<String, VisualParam> = mutableMapOf()

    init {
        @Suppress("LeakingThis")
        motionController.setCharacter(this)
        instances.add(this)
    }

    // ---- abstract interface (must be implemented by concrete subclasses) ---

    abstract fun getAnimationPrefix(): String
    abstract fun getRootJoint(): Joint
    abstract fun getCharacterPosition(): Vector3
    abstract fun getCharacterRotation(): Quaternion
    abstract fun getCharacterVelocity(): Vector3
    abstract fun getCharacterAngularVelocity(): Vector3
    abstract fun getGround(inPos: Vector3, outPos: Vector3, outNorm: Vector3)
    abstract fun getCharacterJoint(i: Int): Joint?
    abstract fun getTimeDilation(): Float
    abstract fun getPixelArea(): Float
    abstract fun getPosGlobalFromAgent(position: Vector3): DoubleArray  // maps to LLVector3d
    abstract fun getPosAgentFromGlobal(position: DoubleArray): Vector3
    abstract fun addDebugText(text: String)
    abstract fun getID(): LLUUID

    // ---- skeleton queries --------------------------------------------------

    /** Find a joint by name; default uses a recursive tree search. */
    open fun getJoint(name: String): Joint? {
        val joint = getRootJoint().findJoint(name)
        if (joint == null) System.err.println("LLCharacter: failed to find joint '$name'")
        return joint
    }

    open fun getDebugName(): String = getID().toString()

    // ---- motion management -------------------------------------------------

    fun registerMotion(id: LLUUID, factory: (LLUUID) -> LLMotion): Boolean =
        motionController.registerMotion(id, factory)

    fun removeMotion(id: LLUUID) = motionController.removeMotion(id)

    fun createMotion(id: LLUUID): LLMotion? = motionController.createMotion(id)

    fun findMotion(id: LLUUID): LLMotion? = motionController.findMotion(id)

    open fun startMotion(id: LLUUID, startOffset: Float = 0f): Boolean =
        motionController.startMotion(id, startOffset)

    open fun stopMotion(id: LLUUID, stopImmediate: Boolean = false): Boolean =
        motionController.stopMotion(id, stopImmediate)

    fun isMotionActive(id: LLUUID): Boolean {
        val motion = motionController.findMotion(id) ?: return false
        return motionController.isMotionActive(motion)
    }

    /** Called when a motion fully stops. Override to react (default: no-op). */
    open fun requestStopMotion(motion: LLMotion) {}

    enum class UpdateType { NORMAL, HIDDEN, FORCE }

    fun updateMotions(updateType: UpdateType) {
        when (updateType) {
            UpdateType.NORMAL -> motionController.updateMotions(0.033f)
            UpdateType.HIDDEN -> motionController.updateMotionsMinimal()
            UpdateType.FORCE  -> motionController.updateMotions(0.033f, forceUpdate = true)
        }
    }

    fun areAnimationsPaused(): Boolean = motionController.isPaused

    fun setAnimTimeFactor(factor: Float) { motionController.timeFactor = factor }

    fun setTimeStep(step: Float) { motionController.timeStep = step }

    open fun flushAllMotions()       = motionController.flushAllMotions()
    open fun deactivateAllMotions()  = motionController.deactivateAllMotions()

    // ---- animation-data key-value store ------------------------------------

    fun setAnimationData(name: String, data: Any?) { animationData[name] = data }
    fun getAnimationData(name: String): Any?        = animationData[name]
    fun removeAnimationData(name: String)           { animationData.remove(name) }

    // ---- visual parameters -------------------------------------------------

    fun addVisualParam(param: VisualParam) {
        visualParamById[param.id]     = param
        visualParamByName[param.name] = param
    }

    fun getVisualParam(id: Int): VisualParam?     = visualParamById[id]
    fun getVisualParam(name: String): VisualParam? = visualParamByName[name]
    fun getVisualParamCount(): Int                 = visualParamById.size

    /** Returns all visual params in id order. */
    fun visualParams(): List<VisualParam> =
        visualParamById.entries.sortedBy { it.key }.map { it.value }

    open fun setVisualParamWeight(param: VisualParam, weight: Float): Boolean {
        val stored = visualParamById[param.id] ?: return false
        stored.weight = weight
        return true
    }

    open fun setVisualParamWeight(name: String, weight: Float): Boolean {
        val stored = visualParamByName[name] ?: return false
        stored.weight = weight
        return true
    }

    open fun setVisualParamWeight(id: Int, weight: Float): Boolean {
        val stored = visualParamById[id] ?: return false
        stored.weight = weight
        return true
    }

    fun getVisualParamWeight(param: VisualParam): Float = visualParamById[param.id]?.weight ?: 0f
    fun getVisualParamWeight(name: String): Float       = visualParamByName[name]?.weight ?: 0f
    fun getVisualParamWeight(id: Int): Float            = visualParamById[id]?.weight ?: 0f

    fun clearVisualParamWeights() {
        visualParamById.values.forEach { it.weight = it.defaultWeight }
    }

    open fun updateVisualParams() {
        // Default: no-op; subclasses apply morph targets.
    }

    // ---- misc accessors ----------------------------------------------------

    open fun getPreferredPelvisHeight(): Float = preferredPelvisHeight

    open fun getVolumePos(jointIndex: Int, volumeOffset: Vector3): Vector3 = Vector3()
    open fun findCollisionVolume(volumeId: Int): Joint? = null
    open fun getCollisionVolumeID(name: String): Int    = -1

    open fun setHoverOffset(offset: Vector3) { hoverOffset = offset }

    // ---- companion ---------------------------------------------------------

    companion object {
        val instances: MutableList<LLCharacter> = mutableListOf()
    }
}
