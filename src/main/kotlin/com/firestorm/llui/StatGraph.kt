package com.firestorm.llui

import com.firestorm.llmath.Color4
import com.firestorm.llmath.Rect
import kotlin.math.roundToInt

class StatGraph(
    name: String,
    rect: Rect = Rect(),
    private val label: String = "",
    private val units: String = "",
    private val precision: Int = 0,
    private var min: Float = 0f,
    private var max: Float = 125f,
    private val perSec: Boolean = true,
    private var value: Float = 0f,
    thresholds: List<Threshold> = defaultThresholds()
) : View(name, rect) {

    data class Threshold(val value: Float, val color: Color4) : Comparable<Threshold> {
        override fun compareTo(other: Threshold): Int = value.compareTo(other.value)
    }

    companion object {
        fun defaultThresholds(): List<Threshold> = listOf(
            Threshold(0.00f, Color4.GREEN),
            Threshold(0.33f, Color4.YELLOW),
            Threshold(0.50f, Color4.RED),
            Threshold(0.75f, Color4.RED)
        )
    }

    private val sortedThresholds: MutableList<Threshold> = thresholds.sorted().toMutableList()
    private var updateElapsed: Float = 0f

    fun setMin(v: Float) { min = v }
    fun setMax(v: Float) { max = v }

    override fun draw() {
        val range = max - min

        TODO("APR: read current stat value from PeriodicRecording (perSec=$perSec) into `value`")

        var frac = (value - min) / range
        frac = frac.coerceIn(0f, 1f)

        updateElapsed += 0f
        if (updateElapsed > 0.5f) {
            val tooltip = "$label${value}$units"
            TODO("APR: setToolTip('$tooltip') formatted with precision=$precision")
            updateElapsed = 0f
        }

        val thresholdColor = sortedThresholds
            .lastOrNull { it.value <= value / max }
            ?.color ?: Color4.WHITE

        TODO("GPU: gl_rect_2d(0, rect.height, rect.width, 0, filled=true) with MenuDefaultBgColor")
        TODO("GPU: gl_rect_2d(0, rect.height, rect.width, 0, filled=false) with Color4.BLACK outline")
        TODO("GPU: gl_rect_2d(1, round(frac*rect.height), rect.width-1, 0, filled=true) with thresholdColor=$thresholdColor")
    }
}
