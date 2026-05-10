package com.firestorm.llui

import com.firestorm.llmath.Rect

enum class ResizeSide { LEFT, TOP, RIGHT, BOTTOM }

open class ResizeBar(
    name: String,
    rect: Rect = Rect(),
    private val resizingView: View,
    val side: ResizeSide,
    var minSize: Int = 0,
    var maxSize: Int = Int.MAX_VALUE,
    var snappingEnabled: Boolean = true,
    var allowDoubleClickSnapping: Boolean = true
) : View(name, rect) {

    private var dragLastScreenX = 0
    private var dragLastScreenY = 0
    private var lastMouseScreenX = 0
    private var lastMouseScreenY = 0
    private var lastMouseDirX = 0
    private var lastMouseDirY = 0
    private var resizeListener: ((Any?) -> Unit)? = null
    private var imagePanel: Panel? = null

    fun canResize(): Boolean = enabled && maxSize > minSize

    override fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        if (!canResize()) return false
        TODO("APR: use JVM equivalent for gFocusMgr.setMouseCapture(this)")
        val (sx, sy) = localPointToScreen(x, y)
        dragLastScreenX = sx; dragLastScreenY = sy
        lastMouseScreenX = sx; lastMouseScreenY = sy
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        if (hasMouseCapture()) TODO("APR: use JVM equivalent for gFocusMgr.setMouseCapture(null)")
        return true
    }

    override fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        var handled = false

        if (hasMouseCapture()) {
            val (screenX, screenY) = localPointToScreen(x, y)
            val deltaX = screenX - dragLastScreenX
            val deltaY = screenY - dragLastScreenY

            val mouseDirX = if (screenX == lastMouseScreenX) lastMouseDirX else screenX - lastMouseScreenX
            val mouseDirY = if (screenY == lastMouseScreenY) lastMouseDirY else screenY - lastMouseScreenY
            lastMouseDirX = mouseDirX; lastMouseDirY = mouseDirY
            lastMouseScreenX = screenX; lastMouseScreenY = screenY

            val validRect = getRootView().rect
            if (validRect.pointInRect(screenX, screenY)) {
                val origRect = resizingView.rect.copy()
                var scaledRect = origRect.copy()
                var newWidth = origRect.width
                var newHeight = origRect.height
                var adjDx = deltaX
                var adjDy = deltaY

                when (side) {
                    ResizeSide.LEFT -> {
                        newWidth = (origRect.width - adjDx).coerceIn(minSize, maxSize)
                        adjDx = origRect.width - newWidth
                        scaledRect = scaledRect.translate(adjDx, 0)
                    }
                    ResizeSide.TOP -> {
                        newHeight = (origRect.height + adjDy).coerceIn(minSize, maxSize)
                        adjDy = newHeight - origRect.height
                    }
                    ResizeSide.RIGHT -> {
                        newWidth = (origRect.width + adjDx).coerceIn(minSize, maxSize)
                        adjDx = newWidth - origRect.width
                    }
                    ResizeSide.BOTTOM -> {
                        newHeight = (origRect.height - adjDy).coerceIn(minSize, maxSize)
                        adjDy = origRect.height - newHeight
                        scaledRect = scaledRect.translate(0, adjDy)
                    }
                }

                notifyParentResize(resizingView.name, newHeight, newWidth)

                scaledRect = scaledRect.copy(
                    top = scaledRect.bottom + newHeight,
                    right = scaledRect.left + newWidth
                )
                resizingView.rect = scaledRect

                var snapView: View? = null
                if (snappingEnabled) {
                    val margin = snapMargin()
                    snapView = when (side) {
                        ResizeSide.LEFT -> resizingView.findSnapEdge(scaledRect.left, mouseDirX, mouseDirY, margin)
                        ResizeSide.TOP -> resizingView.findSnapEdge(scaledRect.top, mouseDirX, mouseDirY, margin)
                        ResizeSide.RIGHT -> resizingView.findSnapEdge(scaledRect.right, mouseDirX, mouseDirY, margin)
                        ResizeSide.BOTTOM -> resizingView.findSnapEdge(scaledRect.bottom, mouseDirX, mouseDirY, margin)
                    }
                }
                resizingView.setSnappedTo(snapView)

                resizingView.rect = origRect
                resizingView.setShape(scaledRect, userOp = true)

                val newRect = resizingView.rect
                when (side) {
                    ResizeSide.LEFT -> {
                        val actual = newRect.left - origRect.left
                        if (actual != adjDx) resizingView.setShape(
                            newRect.copy(bottom = origRect.bottom, top = origRect.top, right = origRect.right), true)
                        dragLastScreenX += actual
                    }
                    ResizeSide.RIGHT -> {
                        val actual = newRect.right - origRect.right
                        if (actual != adjDx) resizingView.setShape(
                            newRect.copy(bottom = origRect.bottom, top = origRect.top, left = origRect.left), true)
                        dragLastScreenX += newRect.right - origRect.right
                    }
                    ResizeSide.TOP -> {
                        val actual = newRect.top - origRect.top
                        if (actual != adjDy) resizingView.setShape(
                            newRect.copy(bottom = origRect.bottom, left = origRect.left, right = origRect.right), true)
                        dragLastScreenY += newRect.top - origRect.top
                    }
                    ResizeSide.BOTTOM -> {
                        val actual = newRect.bottom - origRect.bottom
                        if (actual != adjDy) resizingView.setShape(
                            newRect.copy(top = origRect.top, left = origRect.left, right = origRect.right), true)
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
                ResizeSide.LEFT, ResizeSide.RIGHT -> TODO("GPU: set cursor to SIZE_WE")
                ResizeSide.TOP, ResizeSide.BOTTOM -> TODO("GPU: set cursor to SIZE_NS")
            }
        }

        resizeListener?.invoke(null)
        return handled
    }

    fun handleDoubleClick(x: Int, y: Int, mask: UInt): Boolean {
        if (!snappingEnabled || !allowDoubleClickSnapping) return true
        val origRect = resizingView.rect.copy()
        var scaledRect = origRect.copy()

        when (side) {
            ResizeSide.LEFT -> {
                resizingView.findSnapEdge(scaledRect.left, 0, 0, Int.MAX_VALUE)
                scaledRect = scaledRect.copy(left = scaledRect.right - scaledRect.width.coerceIn(minSize, maxSize))
            }
            ResizeSide.TOP -> {
                resizingView.findSnapEdge(scaledRect.top, 0, 0, Int.MAX_VALUE)
                scaledRect = scaledRect.copy(top = scaledRect.bottom + scaledRect.height.coerceIn(minSize, maxSize))
            }
            ResizeSide.RIGHT -> {
                resizingView.findSnapEdge(scaledRect.right, 0, 0, Int.MAX_VALUE)
                scaledRect = scaledRect.copy(right = scaledRect.left + scaledRect.width.coerceIn(minSize, maxSize))
            }
            ResizeSide.BOTTOM -> {
                resizingView.findSnapEdge(scaledRect.bottom, 0, 0, Int.MAX_VALUE)
                scaledRect = scaledRect.copy(bottom = scaledRect.top - scaledRect.height.coerceIn(minSize, maxSize))
            }
        }

        resizingView.setShape(scaledRect, userOp = true)
        return true
    }

    fun setResizeLimits(min: Int, max: Int) { minSize = min; maxSize = max }
    fun setEnableSnapping(enable: Boolean) { snappingEnabled = enable }
    fun setAllowDoubleClickSnapping(allow: Boolean) { allowDoubleClickSnapping = allow }
    fun setResizeListener(listener: (Any?) -> Unit) { resizeListener = listener }

    fun setImagePanel(panel: Panel) {
        imagePanel?.let { removeChild(it) }
        imagePanel = panel
        addChild(panel)
        sendChildToBack(panel)
    }

    fun getImagePanel(): Panel? = imagePanel

    open fun getRootView(): View = this
    open fun localPointToScreen(x: Int, y: Int): Pair<Int, Int> = Pair(x, y)
    open fun hasMouseCapture(): Boolean = false
    open fun snapMargin(): Int = 0
    open fun notifyParentResize(viewName: String, newHeight: Int, newWidth: Int) {}
    open fun sendChildToBack(child: View) {}
}

private fun View.findSnapEdge(edge: Int, dirX: Int, dirY: Int, margin: Int): View? = null
private fun View.setSnappedTo(v: View?) {}
private fun View.setShape(r: Rect, userOp: Boolean) { rect = r }
