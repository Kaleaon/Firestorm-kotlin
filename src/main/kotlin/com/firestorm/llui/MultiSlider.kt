package com.firestorm.llui

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MultiSlider(
    var minValue: Float = 0f,
    var maxValue: Float = 1f,
    var increment: Float = 0.01f,
    var initialValue: Float = 0f,
    var maxNumSliders: Int = 1,
    var allowOverlap: Boolean = false,
    var loopOverlap: Boolean = false,
    var overlapThreshold: Float = 0f,
    var drawTrack: Boolean = true,
    var useTriangle: Boolean = false,
    var thumbWidth: Int = 10,
    val orientation: Orientation = Orientation.HORIZONTAL,
    var trackColor: Color4 = Color4.white,
    var thumbOutlineColor: Color4 = Color4.white,
    var thumbHighlightColor: Color4 = Color4.white,
    var thumbCenterColor: Color4 = Color4.white,
    var thumbCenterSelectedColor: Color4 = Color4.white,
    var disabledThumbColor: Color4 = Color4.white,
    var triangleColor: Color4 = Color4.white,
    var thumbImage: UIImage? = null,
    var roundedSquareImage: UIImage? = null
) {
    private val values: MutableMap<String, Float> = mutableMapOf()
    val thumbRects: MutableMap<String, Rect> = mutableMapOf()

    var curSlider: String = ""
        private set
    var hoverSlider: String = ""
        private set

    private var mouseOffset: Int = 0
    private var dragStartThumbRect: Rect = Rect()
    private var hasMouseCapture: Boolean = false
    private var hasFocus: Boolean = false
    private var enabled: Boolean = true

    private var rectWidth: Int = 0
    private var rectHeight: Int = 0

    val mouseDownListeners: MutableList<() -> Unit> = mutableListOf()
    val mouseUpListeners: MutableList<() -> Unit> = mutableListOf()

    companion object {
        private const val FLOAT_THRESHOLD = 0.00001f
        private var nameCounter: Int = 0
    }

    val curNumSliders: Int get() = values.size
    fun getOverlapThreshold(): Float = overlapThreshold
    fun canAddSliders(): Boolean = values.size < maxNumSliders
    fun getCurSliderValue(): Float = getSliderValue(curSlider)

    fun getNearestIncrement(value: Float): Float {
        val clamped = value.coerceIn(minValue, maxValue)
        val adjusted = clamped - minValue + increment / 2.0001f
        val snapped = adjusted - (adjusted % increment)
        return minValue + snapped
    }

    fun getSliderValue(name: String): Float = values[name] ?: 0f

    fun getSliderValueFromPos(xpos: Int, ypos: Int): Float {
        val t: Float
        if (orientation == Orientation.HORIZONTAL) {
            val leftEdge = thumbWidth / 2
            val rightEdge = rectWidth - thumbWidth / 2
            val clamped = (xpos + mouseOffset).coerceIn(leftEdge, rightEdge)
            t = (clamped - leftEdge).toFloat() / (rightEdge - leftEdge)
        } else {
            val bottomEdge = thumbWidth / 2
            val topEdge = rectHeight - thumbWidth / 2
            val clamped = (ypos + mouseOffset).coerceIn(bottomEdge, topEdge)
            t = (clamped - bottomEdge).toFloat() / (topEdge - bottomEdge)
        }
        return t * (maxValue - minValue) + minValue
    }

    fun getSliderThumbRect(name: String): Rect = thumbRects[name] ?: Rect()

    fun setSliderThumbImage(name: String) {
        if (name.isNotEmpty()) {
            TODO("GPU: load UIImage for name '$name'")
        } else {
            clearSliderThumbImage()
        }
    }

    fun clearSliderThumbImage() { thumbImage = null }

    fun setSliderValue(name: String, value: Float, fromEvent: Boolean = false) {
        if (!values.containsKey(name)) return

        val newValue = getNearestIncrement(value)

        if (!allowOverlap) {
            val threshold = overlapThreshold + increment / 4f
            val loopUpCheck = if (loopOverlap && value + threshold > maxValue) value + threshold - maxValue + minValue else minValue - 1f
            val loopDownCheck = if (loopOverlap && value - threshold < minValue) value - threshold - minValue + maxValue else maxValue + 1f

            for ((key, locationVal) in values) {
                if (key == name) continue
                val testVal = locationVal - newValue
                if (testVal > -threshold && testVal < threshold) return
                if (loopOverlap) {
                    if (locationVal < loopUpCheck) return
                    if (locationVal > loopDownCheck) return
                }
            }
        }

        values[name] = newValue

        if (!fromEvent && name == curSlider) {
            // notify control value changed
        }

        val t = (newValue - minValue) / (maxValue - minValue)
        val rect = thumbRects[name] ?: Rect()
        if (orientation == Orientation.HORIZONTAL) {
            val leftEdge = thumbWidth / 2
            val rightEdge = rectWidth - thumbWidth / 2
            val x = leftEdge + (t * (rightEdge - leftEdge)).toInt()
            thumbRects[name] = rect.copy(left = x - thumbWidth / 2, right = x + thumbWidth / 2)
        } else {
            val bottomEdge = thumbWidth / 2
            val topEdge = rectHeight - thumbWidth / 2
            val x = bottomEdge + (t * (topEdge - bottomEdge)).toInt()
            thumbRects[name] = rect.copy(top = x + thumbWidth / 2, bottom = x - thumbWidth / 2)
        }
    }

    fun setValue(valueMap: Map<String, Float>) {
        val iter = valueMap.entries.iterator()
        if (!iter.hasNext()) return
        val first = iter.next()
        curSlider = first.key
        setSliderValue(first.key, first.value, true)
        while (iter.hasNext()) {
            val entry = iter.next()
            setSliderValue(entry.key, entry.value, true)
        }
    }

    fun setCurSlider(name: String) {
        if (values.containsKey(name)) curSlider = name
    }

    fun setCurSliderValue(value: Float, fromEvent: Boolean = false) {
        setSliderValue(curSlider, value, fromEvent)
    }

    fun resetCurSlider() { curSlider = "" }

    fun addSlider(): String = addSliderWithValue(initialValue)

    fun addSlider(value: Float): String = addSliderWithValue(value)

    private fun addSliderWithValue(value: Float): String {
        if (values.size >= maxNumSliders) return ""
        val name = "sldr${nameCounter++}"
        val initVal = findUnusedValue(value) ?: return ""
        val initRect = if (orientation == Orientation.HORIZONTAL)
            Rect(left = 0, top = rectHeight, right = thumbWidth, bottom = 0)
        else
            Rect(left = 0, top = thumbWidth, right = rectWidth, bottom = 0)
        thumbRects[name] = initRect
        values[name] = initVal
        curSlider = name
        setSliderValue(curSlider, initVal, true)
        return curSlider
    }

    fun addSlider(value: Float, name: String): Boolean {
        if (values.size >= maxNumSliders) return false
        val initVal = findUnusedValue(value) ?: return false
        val initRect = if (orientation == Orientation.HORIZONTAL)
            Rect(left = 0, top = rectHeight, right = thumbWidth, bottom = 0)
        else
            Rect(left = 0, top = thumbWidth, right = rectWidth, bottom = 0)
        thumbRects[name] = initRect
        values[name] = initVal
        curSlider = name
        setSliderValue(curSlider, initVal, true)
        return true
    }

    private fun findUnusedValue(startVal: Float): Float? {
        var initVal = startVal
        var firstTry = true
        while (true) {
            val threshold = if (allowOverlap) FLOAT_THRESHOLD else overlapThreshold + increment / 4f
            val hit = values.values.any { abs(it - initVal) < threshold }
            if (!hit) return initVal
            initVal += increment
            if (initVal > maxValue) initVal = minValue
            if (initVal == initialValue && !firstTry) return null
            firstTry = false
        }
    }

    fun deleteSlider(name: String) {
        if (values.isEmpty()) return
        values.remove(name)
        thumbRects.remove(name)
        if (values.isNotEmpty()) {
            curSlider = thumbRects.keys.last()
        }
    }

    fun deleteCurSlider() { deleteSlider(curSlider) }

    fun clear() {
        while (thumbRects.isNotEmpty() && values.isNotEmpty()) {
            deleteCurSlider()
        }
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture) {
            setCurSliderValue(getSliderValueFromPos(x, y))
            onCommit()
        } else {
            hoverSlider = if (enabled) {
                thumbRects.entries.firstOrNull { it.value.pointInRect(x, y) }?.key ?: ""
            } else {
                ""
            }
        }
        return true
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        return if (hasMouseCapture) {
            hasMouseCapture = false
            mouseUpListeners.forEach { it() }
            true
        } else {
            true
        }
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        mouseDownListeners.forEach { it() }

        val ctrlMask = 0x0001
        if (mask and ctrlMask != 0) {
            setCurSliderValue(initialValue)
            onCommit()
        } else {
            val hit = thumbRects.entries.firstOrNull { it.value.pointInRect(x, y) }
            if (hit != null) {
                curSlider = hit.key
            }

            if (curSlider.isNotEmpty()) {
                val rect = thumbRects[curSlider]
                if (rect != null && rect.pointInRect(x, y)) {
                    mouseOffset = if (orientation == Orientation.HORIZONTAL)
                        (rect.left + thumbWidth / 2) - x
                    else
                        (rect.bottom + thumbWidth / 2) - y
                } else {
                    mouseOffset = 0
                }
                hasMouseCapture = true
                dragStartThumbRect = thumbRects[curSlider] ?: Rect()
            }
        }
        return true
    }

    fun handleKeyHere(key: Key, mask: Int): Boolean {
        return when (key) {
            Key.UP, Key.DOWN -> true
            Key.LEFT -> {
                setCurSliderValue(getCurSliderValue() - increment)
                onCommit()
                true
            }
            Key.RIGHT -> {
                setCurSliderValue(getCurSliderValue() + increment)
                onCommit()
                true
            }
            else -> false
        }
    }

    fun onMouseLeave(x: Int, y: Int, mask: Int) {
        hoverSlider = ""
    }

    fun draw() {
        val opacity = if (enabled) 1f else 0.3f

        TODO("GPU: unbind texture unit 0")

        val trackRect = computeTrackRect()
        if (drawTrack) {
            TODO("GPU: drawSolid roundedSquareImage in trackRect with trackColor * opacity")
        }

        if (useTriangle) {
            for ((_, rect) in thumbRects) {
                TODO("GPU: gl_triangle_2d for rect with triangleColor * opacity")
            }
        } else if (roundedSquareImage == null && thumbImage == null) {
            var curSldrRect: Rect? = null
            var hoverSldrRect: Rect? = null

            for ((name, rect) in thumbRects) {
                when (name) {
                    curSlider -> { curSldrRect = rect; continue }
                    hoverSlider -> if (enabled && !hasMouseCapture) { hoverSldrRect = rect; continue }
                }
                TODO("GPU: gl_rect_2d fill rect with thumbCenterColor")
            }

            curSldrRect?.let { TODO("GPU: gl_rect_2d fill curSldrRect with thumbCenterSelectedColor") }

            if (hasMouseCapture) {
                TODO("GPU: gl_rect_2d outline dragStartThumbRect with thumbCenterColor * opacity")
            } else {
                hoverSldrRect?.let { TODO("GPU: gl_rect_2d fill hoverSldrRect with thumbCenterSelectedColor") }
            }
        } else {
            if (hasMouseCapture) {
                TODO("GPU: draw ghost dragStartThumbRect with thumbCenterColor @ 0.3 opacity")
            }

            if (hasFocus && curSlider.isNotEmpty()) {
                val curRect = thumbRects[curSlider]
                if (curRect != null) {
                    TODO("GPU: drawBorder curRect with thumbHighlightColor")
                }
            }

            if (hoverSlider.isNotEmpty()) {
                val hoverRect = thumbRects[hoverSlider]
                if (hoverRect != null) {
                    TODO("GPU: drawBorder hoverRect with thumbHighlightColor")
                }
            }

            var curSldrEntry: Map.Entry<String, Rect>? = null
            var hoverSldrEntry: Map.Entry<String, Rect>? = null

            for (entry in thumbRects.entries) {
                when (entry.key) {
                    curSlider -> { curSldrEntry = entry; continue }
                    hoverSlider -> if (enabled && !hasMouseCapture) { hoverSldrEntry = entry; continue }
                }
                TODO("GPU: draw thumb image/roundedSquare for entry.value with opacity")
            }

            curSldrEntry?.let { TODO("GPU: draw current slider thumb at it.value with selected color") }
            hoverSldrEntry?.let { TODO("GPU: draw hover slider thumb at it.value with selected color") }
        }
    }

    private fun computeTrackRect(): Rect {
        TODO("GPU: compute track rect based on orientation and rect dimensions")
    }

    private fun onCommit() {
        // notify commit listeners
    }

    fun setRectDimensions(width: Int, height: Int) {
        rectWidth = width
        rectHeight = height
    }
}
