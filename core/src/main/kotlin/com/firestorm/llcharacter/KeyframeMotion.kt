// Converted from llkeyframemotion.h / llkeyframemotion.cpp — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const val MIN_REQUIRED_PIXEL_AREA_KEYFRAME = 40f
const val MAX_CHAIN_LENGTH = 4
const val KEYFRAME_MOTION_VERSION = 1
const val KEYFRAME_MOTION_SUBVERSION = 0

// ---------------------------------------------------------------------------
// Interpolation type
// ---------------------------------------------------------------------------

enum class InterpolationType { STEP, LINEAR, SPLINE }

// ---------------------------------------------------------------------------
// Key types (immutable value objects)
// ---------------------------------------------------------------------------

data class PositionKey(val time: Float = 0f, val position: Vector3 = Vector3())
data class RotationKey(val time: Float = 0f, val rotation: Quaternion = Quaternion())
data class ScaleKey(val time: Float = 0f, val scale: Vector3 = Vector3())

// ---------------------------------------------------------------------------
// Curves — time-sorted key maps with loop-point variants
// ---------------------------------------------------------------------------

class PositionCurve {
    var interpolationType: InterpolationType = InterpolationType.LINEAR
    val keys: java.util.TreeMap<Float, PositionKey> = java.util.TreeMap()
    var loopInKey: PositionKey = PositionKey()
    var loopOutKey: PositionKey = PositionKey()

    fun getValue(time: Float, duration: Float): Vector3 {
        if (keys.isEmpty()) return Vector3()
        val floorEntry = keys.floorEntry(time) ?: keys.firstEntry()
        val ceilEntry  = keys.ceilingEntry(time) ?: keys.lastEntry()
        if (floorEntry.key == ceilEntry.key) return floorEntry.value.position
        val u = (time - floorEntry.key) / (ceilEntry.key - floorEntry.key)
        return interp(u, floorEntry.value, ceilEntry.value)
    }

    private fun interp(u: Float, a: PositionKey, b: PositionKey): Vector3 = Vector3(
        a.position.x + u * (b.position.x - a.position.x),
        a.position.y + u * (b.position.y - a.position.y),
        a.position.z + u * (b.position.z - a.position.z)
    )
}

class RotationCurve {
    var interpolationType: InterpolationType = InterpolationType.LINEAR
    val keys: java.util.TreeMap<Float, RotationKey> = java.util.TreeMap()
    var loopInKey: RotationKey = RotationKey()
    var loopOutKey: RotationKey = RotationKey()

    fun getValue(time: Float, duration: Float): Quaternion {
        if (keys.isEmpty()) return Quaternion()
        val floorEntry = keys.floorEntry(time) ?: keys.firstEntry()
        val ceilEntry  = keys.ceilingEntry(time) ?: keys.lastEntry()
        if (floorEntry.key == ceilEntry.key) return floorEntry.value.rotation
        val u = (time - floorEntry.key) / (ceilEntry.key - floorEntry.key)
        return floorEntry.value.rotation.slerp(ceilEntry.value.rotation, u)
    }
}

class ScaleCurve {
    var interpolationType: InterpolationType = InterpolationType.LINEAR
    val keys: java.util.TreeMap<Float, ScaleKey> = java.util.TreeMap()
    var loopInKey: ScaleKey = ScaleKey()
    var loopOutKey: ScaleKey = ScaleKey()

    fun getValue(time: Float, duration: Float): Vector3 {
        if (keys.isEmpty()) return Vector3(1f, 1f, 1f)
        val floorEntry = keys.floorEntry(time) ?: keys.firstEntry()
        val ceilEntry  = keys.ceilingEntry(time) ?: keys.lastEntry()
        if (floorEntry.key == ceilEntry.key) return floorEntry.value.scale
        val u = (time - floorEntry.key) / (ceilEntry.key - floorEntry.key)
        val a = floorEntry.value.scale; val b = ceilEntry.value.scale
        return Vector3(a.x + u * (b.x - a.x), a.y + u * (b.y - a.y), a.z + u * (b.z - a.z))
    }
}

// ---------------------------------------------------------------------------
// JointMotion — per-joint animation track
// ---------------------------------------------------------------------------

class JointMotion {
    val positionCurve = PositionCurve()
    val rotationCurve = RotationCurve()
    val scaleCurve    = ScaleCurve()
    var jointName: String = ""
    var usage: UInt = 0u
    var priority: JointPriority = JointPriority.LOW

    /** Sample all active curves and write results into [jointState]. */
    fun update(jointState: JointState, time: Float, duration: Float) {
        if (usage and JointState.Usage.POS.mask   != 0u) jointState.position = positionCurve.getValue(time, duration)
        if (usage and JointState.Usage.ROT.mask   != 0u) jointState.rotation = rotationCurve.getValue(time, duration)
        if (usage and JointState.Usage.SCALE.mask != 0u) jointState.scale    = scaleCurve.getValue(time, duration)
    }
}

// ---------------------------------------------------------------------------
// JointMotionList — the full baked animation dataset
// ---------------------------------------------------------------------------

class JointMotionList {
    val jointMotions: MutableList<JointMotion> = mutableListOf()
    var duration: Float = 0f
    var loop: Boolean = false
    var loopInPoint: Float = 0f
    var loopOutPoint: Float = 0f
    var easeInDuration: Float = 0f
    var easeOutDuration: Float = 0f
    var basePriority: JointPriority = JointPriority.LOW
    var maxPriority: JointPriority  = JointPriority.LOW
    var emoteName: String = ""
    var emoteID: LLUUID = LLUUID.NULL

    val numJointMotions: Int get() = jointMotions.size
    fun getJointMotion(index: Int): JointMotion = jointMotions[index]
}

// ---------------------------------------------------------------------------
// KeyframeDataCache — process-wide singleton cache
// ---------------------------------------------------------------------------

object KeyframeDataCache {
    private val store: MutableMap<LLUUID, JointMotionList> = mutableMapOf()

    fun addKeyframeData(id: LLUUID, list: JointMotionList) { store[id] = list }
    fun getKeyframeData(id: LLUUID): JointMotionList? = store[id]
    fun removeKeyframeData(id: LLUUID) { store.remove(id) }
    fun clear() { store.clear() }
}

// ---------------------------------------------------------------------------
// Asset status
// ---------------------------------------------------------------------------

enum class AssetStatus { LOADED, FETCHED, NEEDS_FETCH, FETCH_FAILED, UNDEFINED }

// ---------------------------------------------------------------------------
// KeyframeMotion
// ---------------------------------------------------------------------------

/**
 * A motion driven by keyframe data loaded from an asset.  Mirrors C++ LLKeyframeMotion.
 *
 * IK constraint application is stubbed with TODO(); the keyframe sampling and
 * loop logic are fully implemented.
 */
open class KeyframeMotion(id: LLUUID) : LLMotion(id) {

    var jointMotionList: JointMotionList? = null
        internal set

    val jointStates: MutableList<JointState> = mutableListOf()
    var assetStatus: AssetStatus = AssetStatus.UNDEFINED
    var lastUpdateTime: Float = 0f
        private set
    var lastLoopedTime: Float = 0f
        private set

    // ---- LLMotion overrides ------------------------------------------------

    override fun getLoop(): Boolean = jointMotionList?.loop ?: false
    override fun getDuration(): Float = jointMotionList?.duration ?: 0f
    override fun getEaseInDuration(): Float = jointMotionList?.easeInDuration ?: 0f
    override fun getEaseOutDuration(): Float = jointMotionList?.easeOutDuration ?: 0f
    override fun getPriority(): JointPriority = jointMotionList?.basePriority ?: JointPriority.LOW
    override fun getNumJointMotions(): Int = jointMotionList?.numJointMotions ?: 0
    override fun getBlendType(): MotionBlendType = MotionBlendType.NORMAL_BLEND
    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_KEYFRAME

    override fun onInitialize(character: Character): MotionInitStatus {
        jointMotionList = KeyframeDataCache.getKeyframeData(id)
        if (jointMotionList != null) return MotionInitStatus.SUCCESS
        // Async fetch not yet wired; hold until loaded externally.
        assetStatus = AssetStatus.NEEDS_FETCH
        return MotionInitStatus.HOLD
    }

    override fun onActivate(): Boolean {
        System.err.println("KeyframeMotion: onActivate not yet implemented")
        return false
    }

    override fun onUpdate(activeTime: Float): Boolean {
        val jml = jointMotionList ?: return false
        lastUpdateTime = activeTime

        // Compute looped time
        val loopedTime: Float = if (jml.loop && jml.duration > 0f) {
            val span = (jml.loopOutPoint - jml.loopInPoint).coerceAtLeast(0.001f)
            if (activeTime > jml.loopOutPoint) {
                jml.loopInPoint + (activeTime - jml.loopOutPoint) % span
            } else {
                activeTime
            }.also { lastLoopedTime = it }
        } else {
            activeTime.coerceAtMost(jml.duration).also { lastLoopedTime = it }
        }

        applyKeyframes(loopedTime)
        // TODO: applyConstraints(loopedTime)
        return activeTime <= jml.duration || jml.loop
    }

    override fun onDeactivate() {
        // TODO: deactivate all IK constraints
    }

    override fun setStopTime(time: Float) {
        super.setStopTime(time)
        // TODO: propagate stop time into ease-out blend
    }

    // ---- Keyframe application ---------------------------------------------

    private fun applyKeyframes(time: Float) {
        val jml = jointMotionList ?: return
        for ((index, jm) in jml.jointMotions.withIndex()) {
            val js = jointStates.getOrNull(index) ?: continue
            jm.update(js, time, jml.duration)
        }
    }

    // ---- Setters for editing a keyframe animation -------------------------

    val isLoaded: Boolean get() = jointMotionList != null
    val loopIn: Float get() = jointMotionList?.loopInPoint ?: 0f
    val loopOut: Float get() = jointMotionList?.loopOutPoint ?: 0f

    fun setLoop(loop: Boolean)       { jointMotionList?.loop = loop }
    fun setLoopIn(inPoint: Float)    { jointMotionList?.loopInPoint = inPoint }
    fun setLoopOut(outPoint: Float)  { jointMotionList?.loopOutPoint = outPoint }
    fun setEaseIn(easeIn: Float)     { jointMotionList?.easeInDuration = easeIn }
    fun setEaseOut(easeOut: Float)   { jointMotionList?.easeOutDuration = easeOut }
    fun setEmote(emoteId: LLUUID)    { jointMotionList?.emoteID = emoteId }
    fun setPriority(priority: Int)   { jointMotionList?.basePriority = JointPriority.fromInt(priority) }

    companion object {
        fun create(id: LLUUID): KeyframeMotion = KeyframeMotion(id)
        fun flushKeyframeCache() = KeyframeDataCache.clear()
    }
}
