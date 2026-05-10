// Converted from llmotioncontroller.h / llmotioncontroller.cpp — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// MotionRegistry
//
// Maps UUIDs to factory lambdas, mirroring C++ LLMotionRegistry.
// ---------------------------------------------------------------------------

class MotionRegistry {
    private val table: MutableMap<LLUUID, (LLUUID) -> Motion> = mutableMapOf()

    /** Register a factory for [id]; returns false if [id] is already registered. */
    fun registerMotion(id: LLUUID, factory: (LLUUID) -> Motion): Boolean {
        if (table.containsKey(id)) return false
        table[id] = factory
        return true
    }

    /** Create a new instance for [id], or null if unregistered / marked bad. */
    fun createMotion(id: LLUUID): Motion? = table[id]?.invoke(id)

    /** Prevent future instantiation of a broken motion (maps id to null). */
    fun markBad(id: LLUUID) { table[id] = { _ -> throw IllegalStateException("Motion $id marked bad") } }

    fun clear() = table.clear()
}

// ---------------------------------------------------------------------------
// MotionController
//
// Manages the full lifecycle of Motion instances for one character.
// Mirrors C++ LLMotionController.
// ---------------------------------------------------------------------------

class MotionController {

    // ---- public flags ------------------------------------------------------

    var isSelf: Boolean = false

    // ---- time --------------------------------------------------------------

    var timeFactor: Float = 1f
    var timeStep: Float   = 0f
    var animTime: Float   = 0f

    private var prevTimerElapsed: Float = 0f
    private var lastTime: Float         = 0f
    private var hasRunOnce: Boolean     = false
    private var timeStepCount: Int      = 0
    private var lastInterp: Float       = 0f

    // ---- pause state -------------------------------------------------------

    private var paused: Boolean = false
    private var pausedFrame: Int = 0
    val isPaused: Boolean get() = paused
    val pausedFrameNum: Int get() = pausedFrame

    // ---- character link ----------------------------------------------------

    var character: LLCharacter? = null

    fun setCharacter(character: LLCharacter) { this.character = character }

    // ---- motion lifecycle sets (mirrors C++ comments) ----------------------
    //
    //  mAllMotions     – owns every Motion for its entire lifetime
    //  mLoadingMotions – waiting for async asset data
    //  mLoadedMotions  – asset loaded, not yet / no longer active
    //  mActiveMotions  – currently playing (ordered list for blend priority)
    //  mDeprecatedMotions – fading out so a new instance can take over

    private val allMotions:        MutableMap<LLUUID, Motion>  = mutableMapOf()
    private val loadingMotions:    MutableSet<Motion>          = mutableSetOf()
    private val loadedMotions:     MutableSet<Motion>          = mutableSetOf()
    private val activeMotions:     ArrayDeque<Motion>          = ArrayDeque()
    private val deprecatedMotions: MutableSet<Motion>          = mutableSetOf()

    private val registry: MotionRegistry = MotionRegistry()

    companion object {
        var currentTimeFactor: Float = 1f
    }

    // ---- registration / creation -------------------------------------------

    fun registerMotion(id: LLUUID, factory: (LLUUID) -> Motion): Boolean =
        registry.registerMotion(id, factory)

    fun createMotion(id: LLUUID): Motion? {
        allMotions[id]?.let { return it }
        val motion = registry.createMotion(id) ?: return null
        allMotions[id] = motion
        return motion
    }

    fun removeMotion(id: LLUUID) {
        val motion = allMotions.remove(id) ?: return
        loadingMotions.remove(motion)
        loadedMotions.remove(motion)
        activeMotions.remove(motion)
        deprecatedMotions.remove(motion)
    }

    fun findMotion(id: LLUUID): Motion? = allMotions[id]

    // ---- start / stop ------------------------------------------------------

    fun startMotion(id: LLUUID, startOffset: Float = 0f): Boolean {
        val motion = createMotion(id) ?: return false
        return activateMotionInstance(motion, animTime + startOffset)
    }

    fun stopMotion(id: LLUUID, stopImmediate: Boolean = false): Boolean {
        val motion = allMotions[id] ?: return false
        return stopMotionInstance(motion, stopImmediate)
    }

    // ---- update ------------------------------------------------------------

    /**
     * Main per-frame update.  [dt] is the real elapsed seconds (before time scaling).
     * Pass [forceUpdate] = true to step even while paused.
     */
    fun updateMotions(dt: Float, forceUpdate: Boolean = false) {
        if (paused && !forceUpdate) return
        val scaled = dt * timeFactor
        animTime += scaled
        updateLoadingMotions()
        updateActiveMotions()
        deactivateStoppedMotions()
        purgeExcessMotions()
    }

    /** Minimal update while the character is hidden — only advances loading state. */
    fun updateMotionsMinimal() = updateLoadingMotions()

    private fun updateLoadingMotions() {
        val promoted = mutableListOf<Motion>()
        val iter = loadingMotions.iterator()
        while (iter.hasNext()) {
            val m = iter.next()
            if (m.isLoaded()) { iter.remove(); promoted.add(m) }
        }
        loadedMotions.addAll(promoted)
    }

    private fun updateActiveMotions() {
        val toDeactivate = mutableListOf<Motion>()
        for (motion in activeMotions) {
            val still = motion.onUpdate(animTime - motion.activationTimestamp)
            if (!still) toDeactivate.add(motion)
        }
        toDeactivate.forEach { deactivateMotionInstance(it) }
    }

    private fun activateMotionInstance(motion: Motion, time: Float): Boolean {
        if (motion in activeMotions) return true
        motion.activate(time)
        if (!motion.isActive()) return false   // onActivate returned false
        activeMotions.add(motion)
        loadedMotions.remove(motion)
        return true
    }

    private fun stopMotionInstance(motion: Motion, stopImmediate: Boolean): Boolean {
        motion.setStopped(true)
        if (stopImmediate) deactivateMotionInstance(motion)
        return true
    }

    private fun deactivateMotionInstance(motion: Motion): Boolean {
        activeMotions.remove(motion)
        deprecatedMotions.remove(motion)
        motion.deactivate()
        loadedMotions.add(motion)
        return true
    }

    private fun deprecateMotionInstance(motion: Motion) {
        activeMotions.remove(motion)
        deprecatedMotions.add(motion)
    }

    private fun deactivateStoppedMotions() {
        val toDeactivate = activeMotions.filter { m ->
            m.isStopped && m.getEaseOutDuration() <= 0f
        }
        toDeactivate.forEach { deactivateMotionInstance(it) }
    }

    private fun purgeExcessMotions() {
        val stale = allMotions.values.filter { m ->
            !m.isActive() &&
            m !in loadingMotions &&
            m !in loadedMotions &&
            m !in activeMotions &&
            m !in deprecatedMotions
        }
        stale.forEach { allMotions.remove(it.id) }
    }

    // ---- bulk operations ---------------------------------------------------

    fun deactivateAllMotions() {
        activeMotions.toList().forEach { deactivateMotionInstance(it) }
    }

    fun flushAllMotions() {
        deactivateAllMotions()
        allMotions.clear()
        loadingMotions.clear()
        loadedMotions.clear()
        deprecatedMotions.clear()
    }

    // ---- pause / unpause ---------------------------------------------------

    fun pauseAllMotions()   { paused = true  }
    fun unpauseAllMotions() { paused = false }

    // ---- queries -----------------------------------------------------------

    fun isMotionActive(motion: Motion): Boolean  = motion in activeMotions
    fun isMotionLoading(motion: Motion): Boolean = motion in loadingMotions

    fun getActiveMotions(): List<Motion> = activeMotions.toList()

    data class MotionCounts(
        val numMotions: Int,
        val numLoading: Int,
        val numLoaded: Int,
        val numActive: Int,
        val numDeprecated: Int
    )

    fun getMotionCounts(): MotionCounts = MotionCounts(
        numMotions   = allMotions.size,
        numLoading   = loadingMotions.size,
        numLoaded    = loadedMotions.size,
        numActive    = activeMotions.size,
        numDeprecated = deprecatedMotions.size
    )
}

// ---------------------------------------------------------------------------
// Motion — abstract base class used by MotionController
//
// Kept here (rather than in LLMotion.kt) so that MotionController.kt compiles
// stand-alone while LLMotion provides the richer abstract-class hierarchy.
// ---------------------------------------------------------------------------

abstract class Motion(val id: LLUUID) {

    var name: String = ""
    protected var stopped: Boolean = false
    protected var active: Boolean  = false
    var activationTimestamp: Float = 0f
    var stopTimestamp: Float       = 0f
    var residualWeight: Float      = 0f
    var fadeWeight: Float          = 1f

    abstract fun getLoop(): Boolean
    abstract fun getDuration(): Float
    abstract fun getEaseInDuration(): Float
    abstract fun getEaseOutDuration(): Float
    abstract fun getPriority(): JointPriority
    abstract fun getMinPixelArea(): Float

    abstract fun onInitialize(character: LLCharacter): MotionInitStatus
    protected abstract fun onActivate(): Boolean
    abstract fun onUpdate(activeTime: Float): Boolean
    abstract fun onDeactivate()

    open fun canDeprecate(): Boolean = true

    /** Returns true once asset data is available (override in asset-backed motions). */
    open fun isLoaded(): Boolean = true

    fun isStopped(): Boolean = stopped
    fun isActive(): Boolean  = active

    fun setStopped(s: Boolean) { stopped = s }

    fun activate(time: Float) {
        activationTimestamp = time
        active = onActivate()
    }

    fun deactivate() {
        active = false
        onDeactivate()
    }

    open fun setStopTime(time: Float) {
        stopTimestamp = time
        stopped = true
    }
}
