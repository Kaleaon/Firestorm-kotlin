package com.firestorm.llui

import kotlin.math.max
import kotlin.math.min

enum class Orientation { HORIZONTAL, VERTICAL }

class ScrollBar(
    orientation: Orientation,
    docSize: Int,
    docPos: Int,
    pageSize: Int,
    stepSize: Int = 1,
    thickness: Int = 16,
    bgVisible: Boolean = false,
    private val changeCallback: ((Int, ScrollBar) -> Unit)? = null
) {
    val orientation: Orientation = orientation

    var docSize: Int = docSize
        private set
    var docPos: Int = docPos
        private set
    var pageSize: Int = pageSize
        private set
    var stepSize: Int = stepSize
    var thickness: Int = thickness
    var bgVisible: Boolean = bgVisible

    private var docChanged: Boolean = false

    var thumbRect: Rect = Rect()
        private set
    private var dragStartX: Int = 0
    private var dragStartY: Int = 0
    private var hoverGlowStrength: Float = 0.15f
    private var curGlowStrength: Float = 0f
    private var origRect: Rect = Rect()
    private var lastDelta: Int = 0

    var trackColor: Color4 = Color4.black
    var thumbColor: Color4 = Color4.white
    var bgColor: Color4 = Color4.black

    var thumbImageV: UIImage? = null
    var thumbImageH: UIImage? = null
    var trackImageV: UIImage? = null
    var trackImageH: UIImage? = null

    private var hasMouseCapture: Boolean = false
    private var enabled: Boolean = true
    private var visible: Boolean = true
    private var width: Int = 0
    private var height: Int = 0

    val docPosMax: Int get() = max(0, docSize - pageSize)

    init {
        updateThumbRect()
    }

    fun setDocParams(size: Int, pos: Int) {
        docSize = size
        setDocPos(pos)
        docChanged = true
        updateThumbRect()
    }

    fun setDocPos(pos: Int, updateThumb: Boolean = true): Boolean {
        val clamped = pos.coerceIn(0, docPosMax)
        if (clamped != docPos) {
            docPos = clamped
            docChanged = true
            changeCallback?.invoke(docPos, this)
            if (updateThumb) updateThumbRect()
            return true
        }
        return false
    }

    fun setDocSize(size: Int) {
        if (size != docSize) {
            docSize = size
            setDocPos(docPos)
            docChanged = true
            updateThumbRect()
        }
    }

    fun setPageSize(pageSize: Int) {
        if (pageSize != this.pageSize) {
            this.pageSize = pageSize
            setDocPos(docPos)
            docChanged = true
            updateThumbRect()
        }
    }

    fun isAtBeginning(): Boolean = docPos == 0
    fun isAtEnd(): Boolean = docPos == docPosMax

    fun pageUp(overlap: Int) {
        if (docSize > pageSize) changeLine(-(pageSize - overlap), true)
    }

    fun pageDown(overlap: Int) {
        if (docSize > pageSize) changeLine(pageSize - overlap, true)
    }

    fun onLineUpBtnPressed() { changeLine(-stepSize, true) }
    fun onLineDownBtnPressed() { changeLine(stepSize, true) }

    fun setValue(value: Int) { setDocPos(value) }

    fun handleKeyHere(key: Key, mask: Int): Boolean {
        if (docPosMax == 0 && !visible) return false
        return when (key) {
            Key.HOME      -> { setDocPos(0); true }
            Key.END       -> { setDocPos(docPosMax); true }
            Key.DOWN      -> { setDocPos(docPos + stepSize); true }
            Key.UP        -> { setDocPos(docPos - stepSize); true }
            Key.PAGE_DOWN -> { pageDown(1); true }
            Key.PAGE_UP   -> { pageUp(1); true }
            else          -> false
        }
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (thumbRect.pointInRect(x, y)) {
            hasMouseCapture = true
            dragStartX = x
            dragStartY = y
            origRect = thumbRect.copy()
            lastDelta = 0
        } else {
            when {
                orientation == Orientation.VERTICAL && thumbRect.top < y -> pageUp(0)
                orientation == Orientation.HORIZONTAL && x < thumbRect.left -> pageUp(0)
                orientation == Orientation.VERTICAL && y < thumbRect.bottom -> pageDown(0)
                orientation == Orientation.HORIZONTAL && thumbRect.right < x -> pageDown(0)
            }
        }
        return true
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        return if (hasMouseCapture) {
            hasMouseCapture = false
            true
        } else {
            false
        }
    }

    fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean = handleMouseDown(x, y, mask)

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture) {
            if (orientation == Orientation.VERTICAL) {
                var deltaPixels = y - dragStartY
                if (origRect.bottom + deltaPixels < thickness)
                    deltaPixels = thickness - origRect.bottom - 1
                else if (origRect.top + deltaPixels > height - thickness)
                    deltaPixels = height - thickness - origRect.top + 1

                thumbRect = thumbRect.copy(
                    top = origRect.top + deltaPixels,
                    bottom = origRect.bottom + deltaPixels
                )

                val thumbLength = thumbRect.height
                val thumbTrackLength = height - 2 * thickness
                if (deltaPixels != lastDelta || docChanged) {
                    val usableTrackLength = thumbTrackLength - thumbLength
                    if (usableTrackLength > 0) {
                        val variableLines = docPosMax
                        val pos = thumbRect.top
                        val ratio = (pos - thickness - thumbLength).toFloat() / usableTrackLength
                        val newPos = (variableLines - ratio * variableLines + 0.5f).toInt().coerceIn(0, variableLines)
                        changeLine(newPos - docPos, false)
                    }
                }
                lastDelta = deltaPixels
            } else {
                var deltaPixels = x - dragStartX
                if (origRect.left + deltaPixels < thickness)
                    deltaPixels = thickness - origRect.left - 1
                else if (origRect.right + deltaPixels > width - thickness)
                    deltaPixels = width - thickness - origRect.right + 1

                thumbRect = thumbRect.copy(
                    left = origRect.left + deltaPixels,
                    right = origRect.right + deltaPixels
                )

                val thumbLength = thumbRect.width
                val thumbTrackLength = width - 2 * thickness
                if (deltaPixels != lastDelta || docChanged) {
                    val usableTrackLength = thumbTrackLength - thumbLength
                    if (usableTrackLength > 0) {
                        val variableLines = docPosMax
                        val pos = thumbRect.left
                        val ratio = (pos - thickness).toFloat() / usableTrackLength
                        val newPos = (ratio * variableLines + 0.5f).toInt().coerceIn(0, variableLines)
                        changeLine(newPos - docPos, false)
                    }
                }
                lastDelta = deltaPixels
            }
        }
        docChanged = false
        return true
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean = changeLine(clicks * stepSize, true)

    fun handleScrollHWheel(x: Int, y: Int, clicks: Int): Boolean {
        return if (orientation == Orientation.HORIZONTAL) changeLine(clicks * stepSize, true) else false
    }

    fun handleDragAndDrop(x: Int, y: Int, mask: Int, drop: Boolean, cargoType: Int, cargoData: Any?): Boolean = false

    fun reshape(newWidth: Int, newHeight: Int) {
        width = newWidth
        height = newHeight
        updateThumbRect()
    }

    fun draw() {
        if (!isRectValid()) return

        if (bgVisible) {
            // no-op
        }

        val hovered = enabled && hasMouseCapture
        curGlowStrength = if (hovered) {
            lerp(curGlowStrength, hoverGlowStrength, smoothInterpolant(0.05f))
        } else {
            lerp(curGlowStrength, 0f, smoothInterpolant(0.05f))
        }

        val useImages = when (orientation) {
            Orientation.VERTICAL -> thumbImageV != null && thumbImageH != null
            Orientation.HORIZONTAL -> trackImageH != null && trackImageV != null
        }

        if (!useImages) {
            // no-op
            // no-op
        } else {
            if (orientation == Orientation.HORIZONTAL) {
                // no-op
                if (curGlowStrength > 0.01f) {
                    // no-op
                }
            } else {
                // no-op
                if (curGlowStrength > 0.01f) {
                    // no-op
                }
            }
        }
    }

    private fun changeLine(delta: Int, updateThumb: Boolean): Boolean = setDocPos(docPos + delta, updateThumb)

    private fun updateThumbRect() {
        val thumbMinLength = 16
        val windowLength = if (orientation == Orientation.HORIZONTAL) width else height
        val thumbBgLength = max(0, windowLength - 2 * thickness)
        val visibleLines = min(docSize, pageSize)
        val thumbLength = if (docSize != 0)
            min(max(visibleLines * thumbBgLength / docSize, thumbMinLength), thumbBgLength)
        else
            thumbBgLength
        val variableLines = docSize - visibleLines

        if (orientation == Orientation.VERTICAL) {
            val thumbStartMax = thumbBgLength + thickness
            val thumbStartMin = thickness + thumbMinLength
            val thumbStart = if (variableLines != 0)
                min(max(thumbStartMax - (docPos * (thumbBgLength - thumbLength)) / variableLines, thumbStartMin), thumbStartMax)
            else
                thumbStartMax
            thumbRect = Rect(left = 0, top = thumbStart, right = thickness, bottom = thumbStart - thumbLength)
        } else {
            val thumbStartMax = thumbBgLength + thickness - thumbLength
            val thumbStartMin = thickness
            val thumbStart = if (variableLines != 0)
                min(max(thumbStartMin + (docPos * (thumbBgLength - thumbLength)) / variableLines, thumbStartMin), thumbStartMax)
            else
                thumbStartMin
            thumbRect = Rect(left = thumbStart, top = thickness, right = thumbStart + thumbLength, bottom = 0)
        }
    }

    private fun isRectValid(): Boolean = width > 0 && height > 0

    private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)
    private fun smoothInterpolant(dt: Float): Float = dt
}

enum class Key { HOME, END, UP, DOWN, LEFT, RIGHT, PAGE_UP, PAGE_DOWN, OTHER }

data class Rect(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val width: Int get() = right - left
    val height: Int get() = top - bottom
    fun pointInRect(x: Int, y: Int): Boolean = x in left..right && y in bottom..top
}

data class Color4(val r: Float, val g: Float, val b: Float, val a: Float) {
    companion object {
        val black = Color4(0f, 0f, 0f, 1f)
        val white = Color4(1f, 1f, 1f, 1f)
    }
}

interface UIImage {
    fun draw(rect: Rect, color: Color4 = Color4.white)
    fun drawSolid(x: Int, y: Int, width: Int, height: Int, color: Color4)
    fun drawSolid(rect: Rect, color: Color4)
}
