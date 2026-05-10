package com.firestorm.llui

data class Rect(
    var left: Int = 0,
    var top: Int = 0,
    var right: Int = 0,
    var bottom: Int = 0
) {
    val width: Int get() = right - left
    val height: Int get() = top - bottom
    fun translate(dx: Int, dy: Int) {
        left += dx; right += dx; top += dy; bottom += dy
    }
}

data class CoordGL(val x: Int = 0, val y: Int = 0)

enum class Side { LEFT, TOP, RIGHT, BOTTOM }

enum class SnapEdge { SNAP_LEFT, SNAP_TOP, SNAP_RIGHT, SNAP_BOTTOM }

enum class UiCursor { SIZE_WE, SIZE_NS }

open class View(val name: String = "view") {
    open fun getRect(): Rect = Rect()
    open fun setRect(r: Rect) {}
    open fun setShape(r: Rect, userOp: Boolean) {}
    open fun translate(dx: Int, dy: Int) {}
    open fun findSnapEdge(
        edge: Int,
        mouseDir: CoordGL,
        snapEdge: SnapEdge,
        snapMode: Int,
        margin: Int
    ): View? = null
    open fun setSnappedTo(v: View?) {}
    open fun getName(): String = name
    open fun getRootView(): View = this
    open fun getLocalRect(): Rect = getRect()
    fun localPointInRect(rect: Rect, x: Int, y: Int): Boolean =
        x in rect.left..rect.right && y in rect.bottom..rect.top
}

open class Panel(name: String = "panel") : View(name)

open class ResizeBar(
    private val resizingView: View,
    val side: Side,
    var minSize: Int = 0,
    var maxSize: Int = Int.MAX_VALUE,
    var snappingEnabled: Boolean = true,
    var allowDoubleClickSnapping: Boolean = true
) : View("resize_bar") {

    private var dragLastScreenX = 0
    private var dragLastScreenY = 0
    private var lastMouseScreenX = 0
    private var lastMouseScreenY = 0
    private var lastMouseDir = CoordGL(0, 0)
    private var resizeListener: ((Any?) -> Unit)? = null
    private var imagePanel: Panel? = null

    init {
        // follow rules are platform-layout concerns; stub follows the C++ logic
    }

    open fun isEnabled(): Boolean = true
    fun canResize(): Boolean = isEnabled() && maxSize > minSize

    open fun hasMouseCapture(): Boolean = false
    open fun captureMouseTo(target: ResizeBar?) {}
    open fun localPointToScreen(x: Int, y: Int): Pair<Int, Int> = Pair(x, y)
    open fun setCursor(cursor: UiCursor) {}
    open fun getSnapMargin(): Int = 0

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (!canResize()) return false
        captureMouseTo(this)
        val (sx, sy) = localPointToScreen(x, y)
        dragLastScreenX = sx; dragLastScreenY = sy
        lastMouseScreenX = sx; lastMouseScreenY = sy
        return true
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture()) captureMouseTo(null)
        return true
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        var handled = false

        if (hasMouseCapture()) {
            val (screenX, screenY) = localPointToScreen(x, y)
            val deltaX = screenX - dragLastScreenX
            val deltaY = screenY - dragLastScreenY

            val mouseDirX = if (screenX == lastMouseScreenX) lastMouseDir.x else screenX - lastMouseScreenX
            val mouseDirY = if (screenY == lastMouseScreenY) lastMouseDir.y else screenY - lastMouseScreenY
            lastMouseDir = CoordGL(mouseDirX, mouseDirY)
            lastMouseScreenX = screenX; lastMouseScreenY = screenY

            val validRect = getRootView().getRect()
            if (localPointInRect(validRect, screenX, screenY)) {
                val origRect = resizingView.getRect()
                var scaledRect = origRect.copy()
                var newWidth = origRect.width
                var newHeight = origRect.height
                var adjDx = deltaX
                var adjDy = deltaY

                when (side) {
                    Side.LEFT -> {
                        newWidth = (origRect.width - adjDx).coerceIn(minSize, maxSize)
                        adjDx = origRect.width - newWidth
                        scaledRect.translate(adjDx, 0)
                    }
                    Side.TOP -> {
                        newHeight = (origRect.height + adjDy).coerceIn(minSize, maxSize)
                        adjDy = newHeight - origRect.height
                    }
                    Side.RIGHT -> {
                        newWidth = (origRect.width + adjDx).coerceIn(minSize, maxSize)
                        adjDx = newWidth - origRect.width
                    }
                    Side.BOTTOM -> {
                        newHeight = (origRect.height - adjDy).coerceIn(minSize, maxSize)
                        adjDy = origRect.height - newHeight
                        scaledRect.translate(0, adjDy)
                    }
                }

                notifyParentResize(resizingView.getName(), newHeight, newWidth)

                scaledRect.top = scaledRect.bottom + newHeight
                scaledRect.right = scaledRect.left + newWidth
                resizingView.setRect(scaledRect)

                if (snappingEnabled) {
                    val snapMargin = getSnapMargin()
                    val snapView = when (side) {
                        Side.LEFT -> resizingView.findSnapEdge(scaledRect.left, lastMouseDir, SnapEdge.SNAP_LEFT, 0, snapMargin)
                        Side.TOP -> resizingView.findSnapEdge(scaledRect.top, lastMouseDir, SnapEdge.SNAP_TOP, 0, snapMargin)
                        Side.RIGHT -> resizingView.findSnapEdge(scaledRect.right, lastMouseDir, SnapEdge.SNAP_RIGHT, 0, snapMargin)
                        Side.BOTTOM -> resizingView.findSnapEdge(scaledRect.bottom, lastMouseDir, SnapEdge.SNAP_BOTTOM, 0, snapMargin)
                    }
                    resizingView.setSnappedTo(snapView)
                }

                resizingView.setRect(origRect)
                resizingView.setShape(scaledRect, true)

                val newRect = resizingView.getRect()
                when (side) {
                    Side.LEFT -> {
                        val actualDx = newRect.left - origRect.left
                        if (actualDx != adjDx) {
                            resizingView.setShape(newRect.copy(bottom = origRect.bottom, top = origRect.top, right = origRect.right), true)
                        }
                        dragLastScreenX += actualDx
                    }
                    Side.RIGHT -> {
                        val actualDx = newRect.right - origRect.right
                        if (actualDx != adjDx) {
                            resizingView.setShape(newRect.copy(bottom = origRect.bottom, top = origRect.top, left = origRect.left), true)
                        }
                        dragLastScreenX += newRect.right - origRect.right
                    }
                    Side.TOP -> {
                        val actualDy = newRect.top - origRect.top
                        if (actualDy != adjDy) {
                            resizingView.setShape(newRect.copy(bottom = origRect.bottom, left = origRect.left, right = origRect.right), true)
                        }
                        dragLastScreenY += newRect.top - origRect.top
                    }
                    Side.BOTTOM -> {
                        val actualDy = newRect.bottom - origRect.bottom
                        if (actualDy != adjDy) {
                            resizingView.setShape(newRect.copy(top = origRect.top, left = origRect.left, right = origRect.right), true)
                        }
                        dragLastScreenY += newRect.bottom - origRect.bottom
                    }
                }
            }
            handled = true
        } else {
            handled = true
        }

        if (handled && canResize()) {
            when (side) {
                Side.LEFT, Side.RIGHT -> setCursor(UiCursor.SIZE_WE)
                Side.TOP, Side.BOTTOM -> setCursor(UiCursor.SIZE_NS)
            }
        }

        resizeListener?.invoke(null)
        return handled
    }

    open fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        if (!snappingEnabled || !allowDoubleClickSnapping) return true
        val origRect = resizingView.getRect()
        val scaledRect = origRect.copy()

        when (side) {
            Side.LEFT -> {
                resizingView.findSnapEdge(scaledRect.left, CoordGL(0, 0), SnapEdge.SNAP_LEFT, 0, Int.MAX_VALUE)
                scaledRect.left = scaledRect.right - scaledRect.width.coerceIn(minSize, maxSize)
            }
            Side.TOP -> {
                resizingView.findSnapEdge(scaledRect.top, CoordGL(0, 0), SnapEdge.SNAP_TOP, 0, Int.MAX_VALUE)
                scaledRect.top = scaledRect.bottom + scaledRect.height.coerceIn(minSize, maxSize)
            }
            Side.RIGHT -> {
                resizingView.findSnapEdge(scaledRect.right, CoordGL(0, 0), SnapEdge.SNAP_RIGHT, 0, Int.MAX_VALUE)
                scaledRect.right = scaledRect.left + scaledRect.width.coerceIn(minSize, maxSize)
            }
            Side.BOTTOM -> {
                resizingView.findSnapEdge(scaledRect.bottom, CoordGL(0, 0), SnapEdge.SNAP_BOTTOM, 0, Int.MAX_VALUE)
                scaledRect.bottom = scaledRect.top - scaledRect.height.coerceIn(minSize, maxSize)
            }
        }

        resizingView.setShape(scaledRect, true)
        return true
    }

    fun setResizeLimits(min: Int, max: Int) {
        minSize = min; maxSize = max
    }

    fun setEnableSnapping(enable: Boolean) {
        snappingEnabled = enable
    }

    fun setAllowDoubleClickSnapping(allow: Boolean) {
        allowDoubleClickSnapping = allow
    }

    fun setResizeListener(listener: (Any?) -> Unit) {
        resizeListener = listener
    }

    fun setImagePanel(panel: Panel) {
        imagePanel = panel
    }

    fun getImagePanel(): Panel? = imagePanel

    open fun notifyParentResize(viewName: String, newHeight: Int, newWidth: Int) {}
}
