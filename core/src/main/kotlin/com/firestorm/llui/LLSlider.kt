package com.firestorm.llui

import com.firestorm.llmath.Rect
import com.firestorm.llcommon.*
import kotlin.math.abs

open class Slider(
    name: String,
    rect: Rect = Rect()
) : View(name, rect) {

    enum class Orientation { HORIZONTAL, VERTICAL }

    var orientation: Orientation = Orientation.HORIZONTAL

    var minValue: Float = 0f
        set(v) {
            field = v
            updateThumbRect()
        }

    var maxValue: Float = 1f
        set(v) {
            field = v
            updateThumbRect()
        }

    var increment: Float = 0.1f

    var value: Float = 0f
        private set

    var onMouseDown: ((Slider) -> Unit)? = null
    var onMouseUp: ((Slider) -> Unit)? = null
    var onChange: ((Slider) -> Unit)? = null

    private var thumbLeft: Int = 0
    private var thumbRight: Int = 0
    private var thumbTop: Int = 0
    private var thumbBottom: Int = 0
    private var mouseOffset: Int = 0
    private var dragging: Boolean = false

    companion object {
        private const val DEFAULT_THUMB_SIZE = 16
    }

    fun setValue(v: Float, fromEvent: Boolean = false) {
        val clamped = v.coerceIn(minValue, maxValue)
        val snapped = snapToIncrement(clamped)
        val old = value
        value = snapped
        updateThumbRect()
        if (!fromEvent && old != value) {
            onChange?.invoke(this)
        }
    }

    fun getValueF32(): Float = value

    private fun snapToIncrement(v: Float): Float {
        if (increment <= 0f) return v
        var relative = v - minValue
        relative += increment / 2.0001f
        relative -= relative % increment
        return (relative + minValue).coerceIn(minValue, maxValue)
    }

    private fun updateThumbRect() {
        val range = maxValue - minValue
        val t = if (range > 0f) (value - minValue) / range else 0f
        val halfThumb = DEFAULT_THUMB_SIZE / 2
        val width = rect.right - rect.left
        val height = rect.top - rect.bottom
        if (orientation == Orientation.HORIZONTAL) {
            val leftEdge = halfThumb
            val rightEdge = width - halfThumb
            val x = leftEdge + (t * (rightEdge - leftEdge)).toInt()
            thumbLeft = x - halfThumb
            thumbRight = x + halfThumb
            thumbTop = height / 2 + halfThumb
            thumbBottom = height / 2 - halfThumb
        } else {
            val topEdge = halfThumb
            val bottomEdge = height - halfThumb
            val y = topEdge + (t * (bottomEdge - topEdge)).toInt()
            thumbLeft = width / 2 - halfThumb
            thumbRight = width / 2 + halfThumb
            thumbBottom = y - halfThumb
            thumbTop = y + halfThumb
        }
    }

    override fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        dragging = true
        mouseOffset = 0
        onMouseDown?.invoke(this)
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        dragging = false
        onMouseUp?.invoke(this)
        return true
    }

    override fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        if (!dragging) return false
        val width = rect.right - rect.left
        val height = rect.top - rect.bottom
        val halfThumb = DEFAULT_THUMB_SIZE / 2
        if (orientation == Orientation.HORIZONTAL) {
            val leftEdge = halfThumb
            val rightEdge = width - halfThumb
            val cx = (x + mouseOffset).coerceIn(leftEdge, rightEdge)
            val t = (cx - leftEdge).toFloat() / (rightEdge - leftEdge)
            setValue(t * (maxValue - minValue) + minValue)
        } else {
            val topEdge = halfThumb
            val bottomEdge = height - halfThumb
            val cy = (y + mouseOffset).coerceIn(topEdge, bottomEdge)
            val t = (cy - topEdge).toFloat() / (bottomEdge - topEdge)
            setValue(t * (maxValue - minValue) + minValue)
        }
        return true
    }

    override fun draw() {
        if (!visible) return
        // no-op
    }
}
