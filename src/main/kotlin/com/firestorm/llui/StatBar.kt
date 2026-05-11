package com.firestorm.llui

import com.firestorm.llmath.Rect
import com.firestorm.llmath.Color4
import kotlin.math.*

enum class Orientation { HORIZONTAL, VERTICAL }

private const val MEAN_VALUE_UPDATE_TIME = 1f / 4f
private const val RAPID_CHANGE_THRESHOLD_S = 0.2f
private const val MAX_RAPID_CHANGES_PER_SEC = 10
private const val RAPID_CHANGE_WINDOW_S = 1.0f

private fun calcTickValue(min: Float, max: Float): Float {
    val range = max - min
    val divisors = intArrayOf(6, 8, 10, 4, 5)
    var bestDecimalDigitCount = Int.MAX_VALUE
    var bestDivisor = 10
    for (divisor in divisors) {
        val possibleTick = range / divisor
        val numWholeDigits = ceil(ln(abs(min + possibleTick)) / ln(10f)).toInt()
        for (digitCount in (-(numWholeDigits - 1))..5) {
            val testTickValue = min + (possibleTick * 10f.pow(digitCount))
            if (testTickValue.toInt().toFloat() == testTickValue) {
                if (digitCount < bestDecimalDigitCount) {
                    bestDecimalDigitCount = digitCount
                    bestDivisor = divisor
                }
                break
            }
        }
    }
    return if (range == 0f) 0f else range / bestDivisor
}

private data class AutoScaleResult(val min: Float, val max: Float, val tick: Float)

private fun calcAutoScaleRange(inMin: Float, inMax: Float): AutoScaleResult {
    var min = minOf(0f, inMin, inMax)
    var max = maxOf(0f, inMin, inMax)

    val ranges = floatArrayOf(0f, 1f, 1.5f, 2f, 3f, 5f, 10f)
    val ticks  = floatArrayOf(0f, 0.25f, 0.5f, 1f, 1f, 1f, 2f)

    val numDigitsMax = if (abs(max) == 0f) Int.MIN_VALUE + 1 else ceil(ln(abs(max)) / ln(10f)).toInt()
    val numDigitsMin = if (abs(min) == 0f) Int.MIN_VALUE + 1 else ceil(ln(abs(min)) / ln(10f)).toInt()

    val numDigits = maxOf(numDigitsMax, numDigitsMin)
    val powerOf10 = 10f.pow(numDigits - 1)
    val startingMax = powerOf10 * if (max < 0f) -1f else 1f
    val startingMin = powerOf10 * if (min < 0f) -1f else 1f

    var outMax = max
    var outMin = min
    var curTickMin = 0f
    var curTickMax = 0f

    for (i in ranges.indices) {
        val curMax = startingMax * ranges[i]
        val curMin = startingMin * ranges[i]
        if (min > 0f && curMin <= min) { outMin = curMin; curTickMin = ticks[i] }
        if (max < 0f && curMax >= max) { outMax = curMax; curTickMax = ticks[i] }
    }

    for (i in ranges.indices.reversed()) {
        val curMax = startingMax * ranges[i]
        val curMin = startingMin * ranges[i]
        if (min < 0f && curMin <= min) { outMin = curMin; curTickMin = ticks[i] }
        if (max > 0f && curMax >= max) { outMax = curMax; curTickMax = ticks[i] }
    }

    val tick = powerOf10 * maxOf(curTickMin, curTickMax)
    return AutoScaleResult(outMin, outMax, tick)
}

enum class StatType { NONE, COUNT, EVENT, SAMPLE }

class StatBar(
    name: String,
    rect: Rect = Rect(),
    private var label: String = "",
    private var unitLabel: String = "",
    barMin: Float = 0f,
    barMax: Float = 0f,
    tickSpacingInit: Float = 0f,
    private val decimalDigits: Int = 3,
    displayBar: Boolean = false,
    private val showMedian: Boolean = false,
    displayHistory: Boolean = false,
    scaleRange: Boolean = true,
    private val numHistoryFrames: Int = 200,
    private val numShortHistoryFrames: Int = 20,
    private val maxHeight: Int = 67,
    statName: String = "",
    private val orientation: Orientation = Orientation.VERTICAL,
    private val setting: String = "",
    barMinProvided: Boolean = false,
    barMaxProvided: Boolean = false
) : View(name, rect) {

    private var targetMinBar: Float = minOf(barMin, barMax)
    private var targetMaxBar: Float = maxOf(barMax, barMin)
    private var floatingTargetMinBar: Float = minOf(barMin, barMax)
    private var floatingTargetMaxBar: Float = maxOf(barMax, barMin)
    private var curMaxBar: Float = barMax
    private var curMinBar: Float = 0f
    private var tickSpacing: Float = tickSpacingInit
    private var autoScaleMax: Boolean = !barMaxProvided
    private var autoScaleMin: Boolean = !barMinProvided
    private var displayBar: Boolean = displayBar
    private var displayHistory: Boolean = displayHistory
    private var lastDisplayValue: Float = 0f
    private var lastDisplayValueElapsed: Float = 0f

    private var statType: StatType = StatType.NONE
    private var statName: String = ""

    init {
        if (tickSpacingInit == 0f && barMinProvided && barMaxProvided) {
            tickSpacing = calcTickValue(targetMinBar, targetMaxBar)
        }
        if (setting.isNotEmpty()) {
            val s = loadSetting(setting)
            when (s) {
                0 -> { this.displayBar = false; this.displayHistory = false }
                1 -> { this.displayBar = true;  this.displayHistory = true }
                2 -> { this.displayBar = true;  this.displayHistory = false }
                else -> { /* keep constructor values */ }
            }
        }
        setStat(statName)
    }

    fun destroy() {
        if (setting.isNotEmpty()) {
            val code = when {
                displayHistory -> 1
                displayBar     -> 2
                else           -> 0
            }
            saveSetting(setting, code)
        }
    }

    private fun loadSetting(key: String): Int {
        TODO("APR: use JVM equivalent to read S32 setting '$key'")
    }

    private fun saveSetting(key: String, value: Int) {
        TODO("APR: use JVM equivalent to write S32 setting '$key' = $value")
    }

    override fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        TODO("GPU: show tooltip with stat description for statType=$statType, statName=$statName")
        return true
    }

    override fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        val handled = super.handleMouseDown(x, y, mask)
        if (!handled) {
            if (displayBar) {
                if (displayHistory || orientation == Orientation.HORIZONTAL) {
                    displayBar = false
                    displayHistory = false
                } else {
                    displayHistory = true
                }
            } else {
                displayBar = true
                if (orientation == Orientation.HORIZONTAL) {
                    displayHistory = true
                }
            }
            val p = parent
            if (p != null) {
                p.reshape(p.rect.width, p.rect.height, false)
            }
        }
        return true
    }

    fun setStat(name: String) {
        statName = name
        statType = when {
            name.isEmpty() -> StatType.NONE
            else -> StatType.NONE
        }
        TODO("APR: resolve stat name '$name' against CountAccumulator/EventAccumulator/SampleAccumulator registries to set statType")
    }

    fun setRange(barMin: Float, barMax: Float) {
        targetMinBar = minOf(barMin, barMax)
        targetMaxBar = maxOf(barMin, barMax)
        floatingTargetMinBar = targetMinBar
        floatingTargetMaxBar = targetMaxBar
        tickSpacing = calcTickValue(targetMinBar, targetMaxBar)
    }

    fun getRange(): Pair<Float, Float> = Pair(targetMinBar, targetMaxBar)

    fun getRequiredRect(): Rect {
        val top = when {
            displayBar && displayHistory -> maxHeight
            displayBar -> 40
            else -> 14
        }
        return Rect(0, top, 0, 0)
    }

    override fun draw() {
        val numFrames = if (displayHistory) numHistoryFrames else numShortHistoryFrames

        var current = 0f
        var min = 0f
        var max = 0f
        var mean = 0f
        var displayValue = 0f
        var resolvedUnitLabel = unitLabel
        var decimal = decimalDigits
        var numRapidChanges = 0

        TODO("GPU: fetch PeriodicRecording, compute current/min/max/mean/displayValue based on statType=$statType, numFrames=$numFrames, showMedian=$showMedian")

        val barRect = if (orientation == Orientation.HORIZONTAL) {
            val top = maxOf(5, rect.height - 15)
            Rect(0, top, rect.width - 40, minOf(top - 5, 0))
        } else {
            val top = maxOf(5, rect.height - 15)
            Rect(0, top, rect.width, minOf(top - 5, 20))
        }

        curMaxBar = lerp(curMaxBar, targetMaxBar, 0.05f)
        curMinBar = lerp(curMinBar, targetMinBar, 0.05f)

        lastDisplayValueElapsed += 0f
        if (lastDisplayValueElapsed < MEAN_VALUE_UPDATE_TIME) {
            displayValue = lastDisplayValue
        } else {
            lastDisplayValueElapsed = 0f
        }

        drawLabelAndValue(displayValue, resolvedUnitLabel, barRect, decimal)
        lastDisplayValue = displayValue

        if (displayBar) {
            val valueScale = if (curMaxBar == curMinBar) 0f else {
                if (orientation == Orientation.HORIZONTAL)
                    barRect.height.toFloat() / (curMaxBar - curMinBar)
                else
                    barRect.width.toFloat() / (curMaxBar - curMinBar)
            }
            drawTicks(min, max, valueScale, barRect)

            TODO("GPU: draw background bar gl_rect_2d(barRect, Color4(0,0,0,0.25))")
            TODO("GPU: draw min-max range, history or current bar, and mean bar using valueScale and orientation=$orientation")
        }

        super.draw()
    }

    private fun drawLabelAndValue(value: Float, unitLbl: String, barRect: Rect, digits: Int) {
        val valueStr = if (!value.isNaN())
            "%10.*f %s".format(digits, value, unitLbl).trimEnd()
        else
            "N/A"
        TODO("GPU: render label '$label' at (0, rect.height) and valueStr '$valueStr' at (barRect.right, rect.height) in monospace font")
    }

    private fun drawTicks(min: Float, max: Float, valueScale: Float, barRect: Rect) {
        if (valueScale.isInfinite()) return
        if (!min.isNaN() && (autoScaleMax || autoScaleMin)) {
            val u = smoothInterpolant(10f)
            floatingTargetMinBar = minOf(min, lerp(floatingTargetMinBar, min, u))
            floatingTargetMaxBar = maxOf(max, lerp(floatingTargetMaxBar, max, u))
            val rangeMin = if (autoScaleMin) floatingTargetMinBar else targetMinBar
            val rangeMax = if (autoScaleMax) floatingTargetMaxBar else targetMaxBar
            val result = calcAutoScaleRange(rangeMin, rangeMax)
            if (autoScaleMin) targetMinBar = result.min
            if (autoScaleMax) targetMaxBar = result.max
            tickSpacing = if (autoScaleMin && autoScaleMax) result.tick
                          else calcTickValue(targetMinBar, targetMaxBar)
        }

        if (tickSpacing <= 0f || valueScale <= 0f) return

        val minTickSpacing = if (orientation == Orientation.HORIZONTAL) 20 else 30
        val minLabelSpacing = if (orientation == Orientation.HORIZONTAL) 30 else 60
        val tickLength = 4
        val tickWidth = 1

        val start = if (curMinBar < 0f)
            ceil(-curMinBar / tickSpacing) * -tickSpacing
        else
            0f

        var lastTick = Int.MIN_VALUE
        var lastLabel = Int.MIN_VALUE
        var tickValue = start
        while (true) {
            val tickBegin = floor(minOf((Int.MAX_VALUE / 2).toFloat(), (tickValue - curMinBar) * valueScale)).toInt()
            val tickEnd = tickBegin + tickWidth
            if (tickBegin >= lastTick + minTickSpacing) {
                lastTick = tickBegin
                val digits = if (tickValue.toInt().toFloat() == tickValue) 0 else decimalDigits
                val tickLabel = "%.*f".format(digits, tickValue)
                if (orientation == Orientation.HORIZONTAL) {
                    if (tickBegin > lastLabel + minLabelSpacing) {
                        TODO("GPU: gl_rect_2d tick line and render tickLabel '$tickLabel' at horizontal position $tickBegin")
                        lastLabel = tickBegin
                    } else {
                        TODO("GPU: gl_rect_2d short tick at horizontal $tickBegin")
                    }
                } else {
                    if (tickBegin > lastLabel + minLabelSpacing) {
                        TODO("GPU: gl_rect_2d tick line and render tickLabel '$tickLabel' at vertical position $tickBegin")
                        lastLabel = tickBegin
                    } else {
                        TODO("GPU: gl_rect_2d short tick at vertical $tickBegin")
                    }
                }
                if (tickValue > curMaxBar) break
            }
            tickValue += tickSpacing
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun smoothInterpolant(timeFactor: Float): Float {
        TODO("APR: use JVM equivalent of LLSmoothInterpolation::getInterpolant($timeFactor)")
    }
}
