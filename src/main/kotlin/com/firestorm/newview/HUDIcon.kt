package com.firestorm.newview

import kotlin.math.cos
import kotlin.math.min

private const val ANIM_TIME: Float        = 0.4f
private const val DIST_START_FADE: Float  = 15f
private const val DIST_END_FADE: Float    = 30f
private const val MAX_VISIBLE_TIME: Float = 15f
private const val FADE_OUT_TIME: Float    = 1f

private fun calcBouncyAnimation(x: Float): Float =
    -(cos(x * Math.PI.toFloat() * 2.5f - Math.PI.toFloat() / 2f)) * (0.4f + x * -0.1f) + x * 1.3f

class HUDIcon(type: UByte) : HUDObject(type) {

    private var imagep: Any? = null         // LLViewerTexture handle
    private var animTimerStartNs: Long = System.nanoTime()
    private var lifeTimerStartNs: Long = System.nanoTime()
    private var mDistance: Float = 0f
    private var scale: Float = 0.1f
    private var hidden: Boolean = false
    private var scriptError: Boolean = false

    init {
        sIconInstances.add(this)
    }

    override fun getDistance(): Float = mDistance

    fun setImage(image: Any?) {
        imagep = image
        // no-op
    }

    fun setScale(fractionOfFov: Float) {
        scale = fractionOfFov
    }

    fun restartLifeTimer() {
        lifeTimerStartNs = System.nanoTime()
    }

    fun getHidden(): Boolean = hidden
    fun setHidden(hide: Boolean) { hidden = hide }

    fun getScriptError(): Boolean = scriptError

    fun setScriptError() {
        if (!sScriptErrorIconInstances.contains(this)) {
            scriptError = true
            sScriptErrorIconInstances.add(this)
        }
    }

    override fun markDead() {
        mSourceObject?.let { System.err.println("HUDIcon: markDead not yet implemented") }
        super.markDead()
    }

    override fun render() {
        if (hidden) return
        if (mSourceObject == null || imagep == null) { markDead(); return }

        val timeElapsed = elapsedLife()
        if (timeElapsed > MAX_VISIBLE_TIME) { markDead(); return }

        var alphaFactor = clampRescale(mDistance, DIST_START_FADE, DIST_END_FADE, 1f, 0f)
        if (timeElapsed > MAX_VISIBLE_TIME - FADE_OUT_TIME) {
            alphaFactor *= clampRescale(timeElapsed, MAX_VISIBLE_TIME - FADE_OUT_TIME, MAX_VISIBLE_TIME, 1f, 0f)
        }

        val scaleFactor: Float = if (elapsedAnim() < ANIM_TIME) {
            maxOf(0f, calcBouncyAnimation(elapsedAnim() / ANIM_TIME))
        } else 1f

        // no-op
    }

    fun lineSegmentIntersect(start: Any, end: Any, intersection: Any?): Boolean {
        if (hidden) return false
        if (mSourceObject == null || imagep == null) { markDead(); return false }

        val timeElapsed = elapsedLife()
        if (timeElapsed > MAX_VISIBLE_TIME) { markDead(); return false }

        val scaleFactor: Float = if (elapsedAnim() < ANIM_TIME) {
            maxOf(0f, calcBouncyAnimation(elapsedAnim() / ANIM_TIME))
        } else 1f

        return false
    }

    private fun elapsedAnim(): Float = ((System.nanoTime() - animTimerStartNs) / 1_000_000_000.0).toFloat()
    private fun elapsedLife(): Float = ((System.nanoTime() - lifeTimerStartNs) / 1_000_000_000.0).toFloat()

    companion object {
        private val sIconInstances: MutableList<HUDIcon> = mutableListOf()
        private val sScriptErrorIconInstances: MutableList<HUDIcon> = mutableListOf()

        fun lineSegmentIntersectAll(start: Any, end: Any, intersection: Any?): HUDIcon? {
            var result: HUDIcon? = null
            var localEnd = end
            for (icon in sIconInstances) {
                val pos = Any()
                if (icon.lineSegmentIntersect(start, localEnd, pos)) {
                    result = icon
                    localEnd = pos
                }
            }
            if (result != null && intersection != null) {
                System.err.println("HUDIcon: lineSegmentIntersectAll not yet implemented")
            }
            return result
        }

        fun updateAll() {
            cleanupDeadIcons()
        }

        fun cleanupDeadIcons() {
            val toErase = sIconInstances.filter { it.mDead }
            sIconInstances.removeAll(toErase)
            toErase.filter { it.scriptError }.forEach { sScriptErrorIconInstances.remove(it) }
        }

        fun getNumInstances(): Int = sIconInstances.size
        fun iconsNearby(): Boolean = sIconInstances.isNotEmpty()
        fun scriptIconsNearby(): Boolean = sScriptErrorIconInstances.isNotEmpty()
    }
}

private fun clampRescale(value: Float, low: Float, high: Float, outLow: Float, outHigh: Float): Float {
    if (high == low) return outLow
    val t = ((value - low) / (high - low)).coerceIn(0f, 1f)
    return outLow + t * (outHigh - outLow)
}
