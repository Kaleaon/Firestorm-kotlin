package com.firestorm.llui

enum class Corner { LEFT_TOP, LEFT_BOTTOM, RIGHT_TOP, RIGHT_BOTTOM }

class ResizeHandle(
    val corner: Corner,
    var minWidth: Int = 0,
    var minHeight: Int = 0
) {
    companion object {
        const val HEIGHT = 11
        const val WIDTH = 11
        private const val BORDER_WIDTH = 3
    }

    private var dragLastScreenX: Int = 0
    private var dragLastScreenY: Int = 0
    private var lastMouseScreenX: Int = 0
    private var lastMouseScreenY: Int = 0
    private var lastMouseDirX: Int = 0
    private var lastMouseDirY: Int = 0
    private var hasMouseCapture: Boolean = false
    private var visible: Boolean = true

    private var image: UIImage? = if (corner == Corner.RIGHT_BOTTOM) loadResizeCornerImage() else null

    private var rectLeft: Int = 0
    private var rectBottom: Int = 0
    private var rectRight: Int = 0
    private var rectTop: Int = 0

    private val rectWidth: Int get() = rectRight - rectLeft
    private val rectHeight: Int get() = rectTop - rectBottom

    fun setResizeLimits(minWidth: Int, minHeight: Int) {
        this.minWidth = minWidth
        this.minHeight = minHeight
    }

    fun setBounds(left: Int, bottom: Int, right: Int, top: Int) {
        rectLeft = left
        rectBottom = bottom
        rectRight = right
        rectTop = top
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (!pointInHandle(x, y)) return false
        hasMouseCapture = true
        val screen = localToScreen(x, y)
        dragLastScreenX = screen.first
        dragLastScreenY = screen.second
        lastMouseScreenX = dragLastScreenX
        lastMouseScreenY = dragLastScreenY
        return true
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        return if (hasMouseCapture) {
            hasMouseCapture = false
            true
        } else {
            pointInHandle(x, y)
        }
    }

    fun handleHover(x: Int, y: Int, mask: Int, parent: ResizableView?): Boolean {
        var handled = false

        if (hasMouseCapture) {
            val screenPos = localToScreen(x, y)
            val screenX = screenPos.first.coerceIn(0, Int.MAX_VALUE)
            val screenY = screenPos.second.coerceIn(0, Int.MAX_VALUE)

            if (parent != null) {
                val deltaX = screenX - dragLastScreenX
                val deltaY = screenY - dragLastScreenY

                val mouseDirX = if (screenX == lastMouseScreenX) lastMouseDirX else screenX - lastMouseScreenX
                val mouseDirY = if (screenY == lastMouseScreenY) lastMouseDirY else screenY - lastMouseScreenY
                lastMouseScreenX = screenX
                lastMouseScreenY = screenY
                lastMouseDirX = mouseDirX
                lastMouseDirY = mouseDirY

                val xMultiple = when (corner) {
                    Corner.LEFT_TOP, Corner.LEFT_BOTTOM -> -1
                    else -> 1
                }
                val yMultiple = when (corner) {
                    Corner.LEFT_TOP, Corner.RIGHT_TOP -> 1
                    else -> -1
                }

                val origRect = parent.getRect()
                var newWidth = origRect.width + xMultiple * deltaX
                var effectiveDeltaX = deltaX
                if (newWidth < minWidth) {
                    newWidth = minWidth
                    effectiveDeltaX = xMultiple * (minWidth - origRect.width)
                }

                var newHeight = origRect.height + yMultiple * deltaY
                var effectiveDeltaY = deltaY
                if (newHeight < minHeight) {
                    newHeight = minHeight
                    effectiveDeltaY = yMultiple * (minHeight - origRect.height)
                }

                var scaledLeft = origRect.left
                var scaledBottom = origRect.bottom
                when (corner) {
                    Corner.LEFT_TOP -> scaledLeft += effectiveDeltaX
                    Corner.LEFT_BOTTOM -> { scaledLeft += effectiveDeltaX; scaledBottom += effectiveDeltaY }
                    Corner.RIGHT_BOTTOM -> scaledBottom += effectiveDeltaY
                    Corner.RIGHT_TOP -> { /* no translation needed */ }
                }

                val scaledRect = ViewRect(
                    left = scaledLeft,
                    bottom = scaledBottom,
                    right = scaledLeft + newWidth,
                    top = scaledBottom + newHeight
                )

                parent.setShape(scaledRect)

                val newRect = parent.getRect()
                val actualDeltaX: Int
                val actualDeltaY: Int
                when (corner) {
                    Corner.LEFT_TOP -> {
                        actualDeltaX = newRect.left - origRect.left
                        actualDeltaY = newRect.top - origRect.top
                        if (actualDeltaX != effectiveDeltaX || actualDeltaY != effectiveDeltaY) {
                            parent.setShape(ViewRect(newRect.left, origRect.bottom, origRect.right, newRect.top))
                        }
                        dragLastScreenX += actualDeltaX
                        dragLastScreenY += actualDeltaY
                    }
                    Corner.LEFT_BOTTOM -> {
                        actualDeltaX = newRect.left - origRect.left
                        actualDeltaY = newRect.bottom - origRect.bottom
                        if (actualDeltaX != effectiveDeltaX || actualDeltaY != effectiveDeltaY) {
                            parent.setShape(ViewRect(newRect.left, newRect.bottom, origRect.right, origRect.top))
                        }
                        dragLastScreenX += actualDeltaX
                        dragLastScreenY += actualDeltaY
                    }
                    Corner.RIGHT_TOP -> {
                        actualDeltaX = newRect.right - origRect.right
                        actualDeltaY = newRect.top - origRect.top
                        if (actualDeltaX != effectiveDeltaX || actualDeltaY != effectiveDeltaY) {
                            parent.setShape(ViewRect(origRect.left, origRect.bottom, newRect.right, newRect.top))
                        }
                        dragLastScreenX += actualDeltaX
                        dragLastScreenY += actualDeltaY
                    }
                    Corner.RIGHT_BOTTOM -> {
                        actualDeltaX = newRect.right - origRect.right
                        actualDeltaY = newRect.bottom - origRect.bottom
                        if (actualDeltaX != effectiveDeltaX || actualDeltaY != effectiveDeltaY) {
                            parent.setShape(ViewRect(origRect.left, newRect.bottom, newRect.right, origRect.top))
                        }
                        dragLastScreenX += actualDeltaX
                        dragLastScreenY += actualDeltaY
                    }
                }
            }
            handled = true
        } else {
            if (pointInHandle(x, y)) handled = true
        }

        if (handled) {
            val cursorType = when (corner) {
                Corner.RIGHT_BOTTOM, Corner.LEFT_TOP -> CursorType.SIZE_NWSE
                Corner.LEFT_BOTTOM, Corner.RIGHT_TOP -> CursorType.SIZE_NESW
            }
            // no-op
        }

        return handled
    }

    fun draw() {
        if (image != null && visible && corner == Corner.RIGHT_BOTTOM) {
            // no-op
        }
    }

    private fun pointInHandle(x: Int, y: Int): Boolean {
        if (x < rectLeft || x > rectRight || y < rectBottom || y > rectTop) return false
        val topBorder = rectHeight - BORDER_WIDTH
        val rightBorder = rectWidth - BORDER_WIDTH
        return when (corner) {
            Corner.LEFT_TOP    -> (x - rectLeft) <= BORDER_WIDTH || (y - rectBottom) >= topBorder
            Corner.LEFT_BOTTOM -> (x - rectLeft) <= BORDER_WIDTH || (y - rectBottom) <= BORDER_WIDTH
            Corner.RIGHT_TOP   -> (x - rectLeft) >= rightBorder  || (y - rectBottom) >= topBorder
            Corner.RIGHT_BOTTOM -> true
        }
    }

    private fun localToScreen(x: Int, y: Int): Pair<Int, Int> {
        return Pair(x, y)
    }

    private fun loadResizeCornerImage(): UIImage? {
        return null
    }
}

data class ViewRect(val left: Int, val bottom: Int, val right: Int, val top: Int) {
    val width: Int get() = right - left
    val height: Int get() = top - bottom
}

interface ResizableView {
    fun getRect(): ViewRect
    fun setShape(rect: ViewRect)
}

enum class CursorType { SIZE_NWSE, SIZE_NESW, ARROW }
