package com.firestorm.llui

import kotlin.math.abs
import kotlin.math.fmod

enum class Orientation { HORIZONTAL, VERTICAL }

data class Rect(
    var left: Int = 0,
    var right: Int = 0,
    var bottom: Int = 0,
    var top: Int = 0
) {
    fun getWidth(): Int = right - left
    fun getHeight(): Int = top - bottom
    fun getCenterX(): Int = (left + right) / 2
    fun getCenterY(): Int = (bottom + top) / 2
    fun pointInRect(x: Int, y: Int): Boolean = x in left..right && y in bottom..top
}

interface UIImage {
    fun getWidth(): Int
    fun getHeight(): Int
    fun draw(rect: Rect, color: FloatArray) { /* no-op */ }
    fun drawBorder(rect: Rect, color: FloatArray, width: Float) { /* no-op */ }
}

open class Slider(
    initialValue: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    val increment: Float = 0.01f,
    val orientation: Orientation = Orientation.HORIZONTAL,
    thumbOutlineColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    thumbCenterColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    var thumbImage: UIImage? = null,
    var thumbImagePressed: UIImage? = null,
    var thumbImageDisabled: UIImage? = null,
    var trackImageHorizontal: UIImage? = null,
    var trackImageVertical: UIImage? = null,
    var trackHighlightHorizontalImage: UIImage? = null,
    var trackHighlightVerticalImage: UIImage? = null
) {
    var thumbOutlineColor: FloatArray = thumbOutlineColor
    var thumbCenterColor: FloatArray = thumbCenterColor

    private var value: Float = initialValue
    private var mouseOffset: Int = 0
    private var dragStartThumbRect: Rect = Rect()
    private var thumbRect: Rect = Rect()
    private var rect: Rect = Rect()
    private var hasFocus: Boolean = false
    private var hasMouseCapture: Boolean = false
    private var enabled: Boolean = true
    private var isChrome: Boolean = false

    val mouseDownCallbacks: MutableList<(Slider, Float) -> Unit> = mutableListOf()
    val mouseUpCallbacks: MutableList<(Slider, Float) -> Unit> = mutableListOf()

    init {
        setValue(initialValue)
        dragStartThumbRect = thumbRect.copy()
    }

    fun getValueF32(): Float = value

    open fun setValue(newValue: Float, fromEvent: Boolean = false) {
        var v = newValue.coerceIn(minValue, maxValue)
        v -= minValue
        v += increment / 2.0001f
        v -= fmod(v.toDouble(), increment.toDouble()).toFloat()
        v += minValue

        if (!fromEvent && getValueF32() != v) {
            onControlValueChanged(v)
        }
        value = v
        updateThumbRect()
    }

    fun setMinValue(newMin: Float) { updateThumbRect() }
    fun setMaxValue(newMax: Float) { updateThumbRect() }
    fun getIncrement(): Float = increment

    fun addMouseDownCallback(cb: (Slider, Float) -> Unit) { mouseDownCallbacks.add(cb) }
    fun addMouseUpCallback(cb: (Slider, Float) -> Unit) { mouseUpCallbacks.add(cb) }

    protected open fun onControlValueChanged(v: Float) {}
    protected open fun onCommit() {}

    private fun updateThumbRect() {
        val defaultThumbSize = 16
        val t = if (maxValue != minValue) (getValueF32() - minValue) / (maxValue - minValue) else 0f
        val thumbWidth = thumbImage?.getWidth() ?: defaultThumbSize
        val thumbHeight = thumbImage?.getHeight() ?: defaultThumbSize

        if (orientation == Orientation.HORIZONTAL) {
            val leftEdge = thumbWidth / 2
            val rightEdge = rect.getWidth() - thumbWidth / 2
            val x = leftEdge + (t * (rightEdge - leftEdge)).toInt()
            thumbRect.left = x - thumbWidth / 2
            thumbRect.right = thumbRect.left + thumbWidth
            thumbRect.bottom = rect.getCenterY() - thumbHeight / 2
            thumbRect.top = thumbRect.bottom + thumbHeight
        } else {
            val topEdge = thumbHeight / 2
            val bottomEdge = rect.getHeight() - thumbHeight / 2
            val y = topEdge + (t * (bottomEdge - topEdge)).toInt()
            thumbRect.left = rect.getCenterX() - thumbWidth / 2
            thumbRect.right = thumbRect.left + thumbWidth
            thumbRect.bottom = y - thumbHeight / 2
            thumbRect.top = thumbRect.bottom + thumbHeight
        }
    }

    private fun setValueAndCommit(newValue: Float) {
        val old = getValueF32()
        setValue(newValue)
        if (getValueF32() != old) onCommit()
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture) {
            if (orientation == Orientation.HORIZONTAL) {
                val halfWidth = thumbImage?.getWidth()?.div(2) ?: 8
                val leftEdge = halfWidth
                val rightEdge = rect.getWidth() - halfWidth
                val cx = (x + mouseOffset).coerceIn(leftEdge, rightEdge)
                val t = (cx - leftEdge).toFloat() / (rightEdge - leftEdge)
                setValueAndCommit(t * (maxValue - minValue) + minValue)
            } else {
                val halfHeight = thumbImage?.getHeight()?.div(2) ?: 8
                val topEdge = halfHeight
                val bottomEdge = rect.getHeight() - halfHeight
                val cy = (y + mouseOffset).coerceIn(topEdge, bottomEdge)
                val t = (cy - topEdge).toFloat() / (bottomEdge - topEdge)
                setValueAndCommit(t * (maxValue - minValue) + minValue)
            }
        }
        return true
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture) {
            hasMouseCapture = false
            mouseUpCallbacks.forEach { it(this, getValueF32()) }
        }
        return true
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (!isChrome) hasFocus = true
        mouseDownCallbacks.forEach { it(this, getValueF32()) }

        val ctrlMask = 0x0001
        if (mask and ctrlMask != 0) {
            setValueAndCommit(value)
        } else {
            mouseOffset = if (thumbRect.pointInRect(x, y)) {
                if (orientation == Orientation.HORIZONTAL)
                    (thumbRect.left + (thumbImage?.getWidth() ?: 0) / 2) - x
                else
                    (thumbRect.bottom + (thumbImage?.getHeight() ?: 0) / 2) - y
            } else 0

            hasMouseCapture = true
            dragStartThumbRect = thumbRect.copy()
        }
        return true
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        val keyLeft = 0x101
        val keyRight = 0x102
        val keyUp = 0x103
        val keyDown = 0x104
        return when (key) {
            keyDown, keyLeft -> { setValueAndCommit(getValueF32() - getIncrement()); true }
            keyUp, keyRight -> { setValueAndCommit(getValueF32() + getIncrement()); true }
            else -> false
        }
    }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (orientation == Orientation.VERTICAL) {
            setValueAndCommit(getValueF32() - clicks * getIncrement())
            return true
        }
        return false
    }

    open fun draw(alpha: Float) {
        updateThumbRect()
        // no-op

        val trackImage = if (orientation == Orientation.HORIZONTAL) trackImageHorizontal else trackImageVertical
        val trackHighlight = if (orientation == Orientation.HORIZONTAL) trackHighlightHorizontalImage else trackHighlightVerticalImage

        val thumbWidth = thumbImage?.getWidth() ?: 16
        val thumbHeight = thumbImage?.getHeight() ?: 16

        val trackRect: Rect
        val highlightRect: Rect
        if (orientation == Orientation.HORIZONTAL) {
            val trackH = trackImage?.getHeight() ?: 4
            trackRect = Rect(
                left = thumbWidth / 2,
                top = rect.getCenterY() + trackH / 2,
                right = rect.getWidth() - thumbWidth / 2,
                bottom = rect.getCenterY() - trackH / 2
            )
            highlightRect = Rect(trackRect.left, trackRect.top, thumbRect.getCenterX(), trackRect.bottom)
        } else {
            val trackW = trackImage?.getWidth() ?: 4
            trackRect = Rect(
                left = rect.getCenterX() - trackW / 2,
                top = rect.getHeight(),
                right = rect.getCenterX() + trackW / 2,
                bottom = 0
            )
            highlightRect = Rect(trackRect.left, trackRect.top, trackRect.right, trackRect.bottom)
        }

        val enabledAlpha = if (enabled) alpha else 0.6f * alpha
        val white = floatArrayOf(1f, 1f, 1f, enabledAlpha)
        trackImage?.draw(trackRect, white)
        trackHighlight?.draw(highlightRect, white)

        if (hasFocus) {
            // no-op
        }

        if (hasMouseCapture) {
            thumbImage?.draw(dragStartThumbRect, floatArrayOf(thumbCenterColor[0], thumbCenterColor[1], thumbCenterColor[2], 0.3f * alpha))
            thumbImagePressed?.draw(thumbRect, floatArrayOf(thumbOutlineColor[0], thumbOutlineColor[1], thumbOutlineColor[2], alpha))
        } else if (!enabled) {
            thumbImageDisabled?.draw(thumbRect, floatArrayOf(thumbCenterColor[0], thumbCenterColor[1], thumbCenterColor[2], alpha))
        } else {
            thumbImage?.draw(thumbRect, floatArrayOf(thumbCenterColor[0], thumbCenterColor[1], thumbCenterColor[2], alpha))
        }
    }
}
