package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.F_PI
import com.firestorm.llmath.cubicStep
import com.firestorm.llmath.llFloor
import com.firestorm.llmath.llMax
import kotlin.math.abs

private const val NUM_JOINT_SIGNATURE_STRIDES = CharacterConstants.MAX_ANIMATED_JOINTS.toInt() / 4
private const val MAX_MOTION_INSTANCES = 32

// ---------------------------------------------------------------------------
// MotionRegistry
//
// Maps UUIDs to factory lambdas.  Null factory means "marked bad".
// Mirrors C++ LLMotionRegistry.
// ---------------------------------------------------------------------------

class MotionRegistry {

    private val table: MutableMap<LLUUID, ((LLUUID) -> Motion)?> = mutableMapOf()

    fun registerMotion(id: LLUUID, factory: (LLUUID) -> Motion): Boolean {
        if (table.containsKey(id)) return false
        table[id] = factory
        return true
    }

    fun createMotion(id: LLUUID): Motion? {
        return when {
            !table.containsKey(id) -> KeyframeMotion.create(id)   // default: treat as keyframe asset
            else -> table[id]?.invoke(id)                          // null entry → bad, returns null
        }
    }

    fun markBad(id: LLUUID) { table[id] = null }

    fun clear() = table.clear()
}

// ---------------------------------------------------------------------------
// MotionController
//
// Manages the full lifecycle of Motion instances for one character.
// Mirrors C++ LLMotionController (llmotioncontroller.cpp).
//
// Animation lifecycle (mirrors C++ comments):
//   allMotions     – owns every Motion for its entire lifetime
//   loadingMotions – waiting for async asset data
//   loadedMotions  – asset loaded, not yet / no longer active
//   activeMotions  – currently playing (chronological order, oldest first)
//   deprecatedMotions – fading out so a new instance can take over
// ---------------------------------------------------------------------------

class MotionController {

    var isSelf: Boolean = false

    // ---- time ---------------------------------------------------------------

    /** Scales real time; 1.0 = normal speed. */
    var timeFactor: Float = sCurrentTimeFactor
        private set

    /** Optional fixed time-step quantum (0 = disabled). */
    var timeStep: Float = 0f

    /** Accumulated animation clock. */
    var animTime: Float = 0f
        private set

    private var prevTimerElapsed: Float = 0f
    private var lastTime: Float = 0f
    private var hasRunOnce: Boolean = false
    private var timeStepCount: Int = 0
    private var lastInterp: Float = 0f

    /** Multiplier for impostor avatars — allows separate speed adjustment.  Default 1. */
    var updateFactor: Float = 1f

    // ---- pause state --------------------------------------------------------

    private var paused: Boolean = false
    private var pausedFrame: Int = 0
    val isPaused: Boolean   get() = paused
    val pausedFrame_: Int   get() = pausedFrame

    // ---- character link -----------------------------------------------------

    var character: LLCharacter? = null
    fun setCharacter(c: LLCharacter) { character = c }

    // ---- collections --------------------------------------------------------

    private val allMotions:        MutableMap<LLUUID, Motion> = mutableMapOf()
    private val loadingMotions:    MutableSet<Motion>          = mutableSetOf()
    private val loadedMotions:     MutableSet<Motion>          = mutableSetOf()
    private val activeMotions:     ArrayDeque<Motion>          = ArrayDeque()   // front = newest
    private val deprecatedMotions: MutableSet<Motion>          = mutableSetOf()

    private var lastCountAfterPurge: Int = 0

    // ---- timer stub (mirrors LLFrameTimer) ----------------------------------
    // In the JVM port, we drive time externally via updateMotions(dt).
    private var timerElapsed: Float = 0f

    // ---- registration / lookup ----------------------------------------------

    fun registerMotion(id: LLUUID, factory: (LLUUID) -> Motion): Boolean =
        sRegistry.registerMotion(id, factory)

    fun findMotion(id: LLUUID): Motion? = allMotions[id]

    fun createMotion(id: LLUUID): Motion? {
        if (id == LLUUID.NULL) return null

        allMotions[id]?.let { return it }

        val motion = sRegistry.createMotion(id) ?: return null

        val status = motion.onInitialize(character ?: return null)
        when (status) {
            MotionInitStatus.FAILURE -> {
                sRegistry.markBad(id)
                return null
            }
            MotionInitStatus.HOLD    -> loadingMotions.add(motion)
            MotionInitStatus.SUCCESS -> loadedMotions.add(motion)
        }

        allMotions[id] = motion
        return motion
    }

    fun removeMotion(id: LLUUID) {
        val motion = findMotion(id)
        allMotions.remove(id)
        removeMotionInstance(motion)
    }

    private fun removeMotionInstance(motion: Motion?) {
        motion ?: return
        if (motion.isActive) motion.deactivate()
        loadingMotions.remove(motion)
        loadedMotions.remove(motion)
        activeMotions.remove(motion)
    }

    // ---- start / stop -------------------------------------------------------

    fun startMotion(id: LLUUID, startOffset: Float = 0f): Boolean {
        var motion = findMotion(id)

        // If the motion is blending/stopping and supports deprecation, replace it.
        if (motion != null
            && !paused
            && motion.canDeprecate()
            && motion.fadeWeight > 0.01f
            && (motion.isBlending() || motion.stopTimestamp != 0f)) {
            deprecateMotionInstance(motion)
            motion = null
        }

        if (motion == null) motion = createMotion(id)
        motion ?: return false

        if (motion.canDeprecate() && isMotionActive(motion)) return true

        return activateMotionInstance(motion, animTime - startOffset)
    }

    fun stopMotion(id: LLUUID, stopImmediate: Boolean = false): Boolean {
        val motion = findMotion(id)
        return stopMotionInstance(motion, stopImmediate || paused)
    }

    private fun stopMotionInstance(motion: Motion?, stopImmediate: Boolean): Boolean {
        motion ?: return false
        return if (isMotionActive(motion) && !motion.isStopped) {
            motion.setStopTime(animTime)
            if (stopImmediate) deactivateMotionInstance(motion)
            true
        } else if (isMotionLoading(motion)) {
            motion.isStopped = true
            true
        } else false
    }

    // ---- update -------------------------------------------------------------

    /**
     * Full per-frame update.  [dt] is real elapsed seconds (wall clock delta).
     * Pass [forceUpdate] = true to step even while paused.
     */
    fun updateMotions(dt: Float, forceUpdate: Boolean = false) {
        val useQuantum = timeStep != 0f

        timerElapsed += dt
        val curTime   = timerElapsed
        val deltaTime = dt
        lastTime      = animTime

        purgeExcessMotions()

        if (!paused) {
            val updateTime = animTime + deltaTime * timeFactor * updateFactor

            if (useQuantum) {
                val timeInterval  = updateTime % timeStep
                val quantumCount  = llMax(0, llFloor((updateTime - timeInterval) / timeStep)) + 1

                if (quantumCount == timeStepCount) {
                    // still in same quantum — just interpolate
                    val interp = timeInterval / timeStep
                    poseBlender.interpolate(interp - lastInterp)
                    lastInterp = interp
                    updateLoadingMotions()
                    return
                }

                poseBlender.interpolate(1f)
                clearBlenders()

                timeStepCount = quantumCount
                animTime      = quantumCount.toFloat() * timeStep
                lastInterp    = 0f
            } else {
                animTime = updateTime
            }
        }

        updateLoadingMotions()
        resetJointSignatures()

        if (paused && !forceUpdate) {
            updateIdleActiveMotions()
        } else {
            updateAdditiveMotions()
            resetJointSignatures()
            updateRegularMotions()

            if (useQuantum) poseBlender.blendAndCache(true)
            else            poseBlender.blendAndApply()
        }

        hasRunOnce = true
    }

    /** Minimal update while the avatar is hidden — deactivates stopped motions. */
    fun updateMotionsMinimal() {
        purgeExcessMotions()
        updateLoadingMotions()
        resetJointSignatures()
        deactivateStoppedMotions()
        hasRunOnce = true
    }

    fun clearBlenders() { poseBlender.clearBlenders() }

    // ---- bulk operations ----------------------------------------------------

    fun deactivateAllMotions() {
        for ((_, motion) in allMotions.toMap()) {
            deactivateMotionInstance(motion)
        }
    }

    fun flushAllMotions() {
        // Record what was active so we can restart it.
        val activeSnapshot = activeMotions.map { it.id to (animTime - it.activationTimestamp) }
        activeMotions.forEach { it.deactivate() }
        activeMotions.clear()

        deleteAllMotions()
        character?.removeAnimationData("Hand Pose")

        for ((id, dtime) in activeSnapshot) startMotion(id, dtime)
    }

    // ---- pause / unpause ----------------------------------------------------

    fun pauseAllMotions() {
        if (!paused) {
            paused       = true
            pausedFrame  = currentFrame
        }
    }

    fun unpauseAllMotions() {
        if (paused) paused = false
    }

    // ---- queries ------------------------------------------------------------

    fun isMotionActive(motion: Motion): Boolean  = motion.isActive
    fun isMotionLoading(motion: Motion): Boolean = motion in loadingMotions

    fun getActiveMotions(): List<Motion> = activeMotions.toList()

    fun incMotionCounts(
        numMotions: Int, numLoading: Int, numLoaded: Int, numActive: Int, numDeprecated: Int
    ): MotionCounts = MotionCounts(
        numMotions   + allMotions.size,
        numLoading   + loadingMotions.size,
        numLoaded    + loadedMotions.size,
        numActive    + activeMotions.size,
        numDeprecated + deprecatedMotions.size
    )

    data class MotionCounts(
        val numMotions:    Int,
        val numLoading:    Int,
        val numLoaded:     Int,
        val numActive:     Int,
        val numDeprecated: Int
    )

    fun dumpMotions() {
        for ((id, motion) in allMotions) {
            var state = ""
            if (motion in loadingMotions)  state += "l"
            if (motion in loadedMotions)   state += "L"
            if (motion in activeMotions)   state += "A"
            if (motion in deprecatedMotions) state += "D"
            println("${motion.name.ifEmpty { id.toString() }} $state")
        }
    }

    fun setTimeFactor(factor: Float) { timeFactor = factor }
    fun getTimeFactor(): Float = timeFactor
    fun getAnimTime(): Float   = animTime

    fun setTimeStep(step: Float) {
        timeStep = step
        if (step != 0f) {
            for (motion in activeMotions) {
                val at = motion.activationTimestamp
                motion.activationTimestamp = llFloor(at / step).toFloat() * step
                val wasStopped = motion.isStopped
                motion.setStopTime(llFloor(motion.stopTimestamp / step).toFloat() * step)
                motion.isStopped = wasStopped
                motion.sendStopTimestamp = llFloor(motion.sendStopTimestamp / step).toFloat() * step
            }
        }
    }

    // ---- companion ----------------------------------------------------------

    companion object {
        var sCurrentTimeFactor: Float = 1f
        private val sRegistry: MotionRegistry = MotionRegistry()

        // Frame counter stub — real viewer increments this per frame.
        private var currentFrame: Int = 0
        fun tickFrame() { currentFrame++ }
    }

    // =========================================================================
    // Internal implementation
    // =========================================================================

    // Joint signature arrays: [layer][joint] bitmask tracking which joints are
    // touched by active motions, so redundant re-evaluation can be skipped.
    private val jointSignature: Array<UByteArray> = Array(2) {
        UByteArray(CharacterConstants.MAX_ANIMATED_JOINTS.toInt())
    }

    private val poseBlender = PoseBlender()

    private fun deleteAllMotions() {
        loadingMotions.clear()
        loadedMotions.clear()
        activeMotions.clear()
        allMotions.clear()
        deprecatedMotions.clear()
    }

    private fun resetJointSignatures() {
        for (layer in jointSignature) layer.fill(0u)
    }

    private fun updateRegularMotions()  = updateMotionsByType(MotionBlendType.NORMAL_BLEND)
    private fun updateAdditiveMotions() = updateMotionsByType(MotionBlendType.ADDITIVE_BLEND)

    private fun updateIdleMotion(motion: Motion) {
        when {
            motion.isStopped && animTime > motion.stopTimestamp + motion.getEaseOutDuration() ->
                deactivateMotionInstance(motion)

            motion.isStopped && animTime > motion.stopTimestamp -> {
                if (lastTime <= motion.stopTimestamp)
                    motion.residualWeight = motion.getPose()?.weight ?: 0f
            }

            animTime > motion.sendStopTimestamp -> {
                if (lastTime <= motion.sendStopTimestamp) {
                    character?.requestStopMotion(motion)
                    stopMotionInstance(motion, false)
                }
            }

            animTime >= motion.activationTimestamp -> {
                if (lastTime < motion.activationTimestamp)
                    motion.residualWeight = motion.getPose()?.weight ?: 0f
            }
        }
    }

    private fun updateIdleActiveMotions() {
        val snapshot = activeMotions.toList()
        for (motion in snapshot) updateIdleMotion(motion)
    }

    private fun updateMotionsByType(animType: MotionBlendType) {
        val ch = character ?: return
        val lastJointSig = UByteArray(CharacterConstants.MAX_ANIMATED_JOINTS.toInt())

        val snapshot = activeMotions.toList()
        for (motion in snapshot) {
            if (!motion.isActive || motion.getBlendType() != animType) continue

            val pose = motion.getPose() ?: continue

            var updateMotion = pose.weight < 1f
            if (!updateMotion) {
                for (i in 0 until NUM_JOINT_SIGNATURE_STRIDES) {
                    val idx = i * 4
                    for (k in 0 until 4) {
                        val cur0 = jointSignature[0][idx + k]
                        val test0 = motion.jointSignature[0].getOrElse(idx + k) { 0u }
                        if ((cur0 or test0) > cur0) {
                            jointSignature[0][idx + k] = (cur0 or test0)
                            updateMotion = true
                        }
                        lastJointSig[idx + k] = jointSignature[1][idx + k]
                        val cur1 = jointSignature[1][idx + k]
                        val test1 = motion.jointSignature[1].getOrElse(idx + k) { 0u }
                        if ((cur1 or test1) > cur1) {
                            jointSignature[1][idx + k] = (cur1 or test1)
                            updateMotion = true
                        }
                    }
                }
            }

            if (!updateMotion) {
                updateIdleMotion(motion)
                continue
            }

            // LOD culling after first frame
            if (hasRunOnce && motion.getMinPixelArea() > ch.getPixelArea()) {
                motion.fadeOut()
                if (animTime > motion.sendStopTimestamp && lastTime <= motion.sendStopTimestamp) {
                    ch.requestStopMotion(motion)
                    stopMotionInstance(motion, false)
                }
                if (motion.fadeWeight < 0.01f) {
                    if (motion.isStopped && animTime > motion.stopTimestamp + motion.getEaseOutDuration()) {
                        pose.weight = 0f
                        deactivateMotionInstance(motion)
                    }
                    continue
                }
            } else {
                motion.fadeIn()
            }

            val activeTimeSinceActivation = animTime - motion.activationTimestamp

            when {
                // Motion inactive — deactivate with one final pose sample
                motion.isStopped && animTime > motion.stopTimestamp + motion.getEaseOutDuration() -> {
                    if (lastTime <= motion.stopTimestamp) {
                        pose.weight = motion.fadeWeight
                        motion.onUpdate(motion.stopTimestamp - motion.activationTimestamp)
                    } else {
                        pose.weight = 0f
                        deactivateMotionInstance(motion)
                        continue
                    }
                }

                // Ease out
                motion.isStopped && animTime > motion.stopTimestamp -> {
                    if (lastTime <= motion.stopTimestamp)
                        motion.residualWeight = pose.weight

                    pose.weight = if (motion.getEaseOutDuration() == 0f) 0f
                    else {
                        val easeRatio = 1f - (animTime - motion.stopTimestamp) / motion.getEaseOutDuration()
                        motion.fadeWeight * motion.residualWeight * cubicStep(easeRatio)
                    }
                    motion.onUpdate(activeTimeSinceActivation)
                }

                // Fully active
                animTime > motion.activationTimestamp + motion.getEaseInDuration() -> {
                    pose.weight = motion.fadeWeight

                    if (animTime > motion.sendStopTimestamp && lastTime <= motion.sendStopTimestamp) {
                        ch.requestStopMotion(motion)
                        stopMotionInstance(motion, false)
                    }
                    motion.onUpdate(activeTimeSinceActivation)
                }

                // Ease in
                animTime >= motion.activationTimestamp -> {
                    if (lastTime < motion.activationTimestamp) motion.residualWeight = pose.weight

                    pose.weight = if (motion.getEaseInDuration() == 0f) {
                        motion.fadeWeight
                    } else {
                        val easeRatio = (animTime - motion.activationTimestamp) / motion.getEaseInDuration()
                        motion.fadeWeight * motion.residualWeight + (1f - motion.residualWeight) * cubicStep(easeRatio)
                    }
                    motion.onUpdate(activeTimeSinceActivation)
                }

                else -> {
                    pose.weight = 0f
                    motion.onUpdate(0f)
                }
            }

            // If motion returned false from onUpdate, stop it
            if (!motion.isActive && !motion.isStopped || motion.stopTimestamp > animTime) {
                ch.requestStopMotion(motion)
                stopMotionInstance(motion, false)
            }

            poseBlender.addMotion(motion)
        }
    }

    private fun updateLoadingMotions() {
        val toPromote = mutableListOf<Motion>()
        val toRemove  = mutableListOf<Motion>()

        for (motion in loadingMotions.toList()) {
            val ch = character ?: continue
            when (val status = motion.onInitialize(ch)) {
                MotionInitStatus.SUCCESS -> {
                    loadingMotions.remove(motion)
                    loadedMotions.add(motion)
                    if (!motion.isStopped) activateMotionInstance(motion, animTime)
                }
                MotionInitStatus.FAILURE -> {
                    sRegistry.markBad(motion.id)
                    loadingMotions.remove(motion)
                    deprecatedMotions.remove(motion)
                    allMotions.remove(motion.id)
                }
                MotionInitStatus.HOLD -> { /* still waiting */ }
            }
        }
    }

    private fun activateMotionInstance(motion: Motion, time: Float): Boolean {
        val pose = motion.getPose() ?: return false

        if (motion in loadingMotions) {
            motion.isStopped = false
            return true
        }

        motion.residualWeight = pose.weight

        if (motion.getDuration() != 0f && !motion.getLoop()) {
            val easeOutTime = motion.getEaseOutDuration()
            val motionDur   = llMax(motion.getDuration() - easeOutTime, 0f)
            motion.sendStopTimestamp = time + motionDur
        } else {
            motion.sendStopTimestamp = Float.MAX_VALUE
        }

        if (motion.isActive) activeMotions.remove(motion)
        activeMotions.addFirst(motion)   // newest at front, mirrors C++ push_front

        motion.activate(time)
        motion.onUpdate(0f)

        if (animTime >= motion.sendStopTimestamp) {
            motion.setStopTime(motion.sendStopTimestamp)
            if (motion.residualWeight == 0f) motion.residualWeight = 1f
        }

        return true
    }

    private fun deactivateMotionInstance(motion: Motion): Boolean {
        motion.deactivate()

        if (motion in deprecatedMotions) {
            removeMotionInstance(motion)
            deprecatedMotions.remove(motion)
        } else {
            activeMotions.remove(motion)
        }
        return true
    }

    private fun deprecateMotionInstance(motion: Motion) {
        deprecatedMotions.add(motion)
        stopMotionInstance(motion, false)
        allMotions.remove(motion.id)
    }

    private fun purgeExcessMotions() {
        if (loadedMotions.size > MAX_MOTION_INSTANCES) {
            for (motion in deprecatedMotions.toList()) {
                if (!isMotionActive(motion)) {
                    removeMotionInstance(motion)
                    deprecatedMotions.remove(motion)
                }
            }
        }

        if (loadedMotions.size > MAX_MOTION_INSTANCES) {
            poseBlender.clearBlenders()
            val toKill = mutableSetOf<LLUUID>()
            for (motion in loadedMotions) {
                if (!isMotionActive(motion)) toKill.add(motion.id)
            }
            for (id in toKill) {
                val m = findMotion(id)
                if (m != null && !isMotionActive(m)) removeMotion(id)
            }
        }

        lastCountAfterPurge = loadedMotions.size
    }

    private fun deactivateStoppedMotions() {
        for (motion in activeMotions.toList()) {
            if (motion.isStopped) deactivateMotionInstance(motion)
        }
    }
}

// PoseBlender and the Motion base class are defined in Pose.kt and LLMotion.kt respectively.
// MotionController uses LLMotion throughout; the typealias below keeps older call sites
// in this file from needing renaming.
internal typealias Motion = LLMotion
