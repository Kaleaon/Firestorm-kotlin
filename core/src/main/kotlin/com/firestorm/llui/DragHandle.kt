package com.firestorm.llui

import kotlin.math.abs

private const val LEADING_PAD = 5
private const val TITLE_HPAD = 8
private const val BORDER_PAD = 1
private const val LEFT_PAD = BORDER_PAD + TITLE_HPAD + LEADING_PAD

const val DRAG_HANDLE_HEIGHT = 16
const val DRAG_HANDLE_WIDTH = 16

abstract class DragHandle(
    label: String = "",
    dragHighlightColor: Any? = null,
    dragShadowColor: Any? = null,
    labelVPadding: Int = -1
) {
    companion object {
        var snapMargin: Int = 5
    }

    protected var titleBox: TitleBoxStub? = null

    private var buttonsRect: Rect = Rect(0, 0, 0, 0)
    private var dragLastScreenX: Int = 0
    private var dragLastScreenY: Int = 0
    private var lastMouseScreenX: Int = 0
    private var lastMouseScreenY: Int = 0
    private var lastMouseDirX: Int = 0
    private var lastMouseDirY: Int = 0
    private val dragHighlightColor: Any? = dragHighlightColor
    private val dragShadowColor: Any? = dragShadowColor
    private var maxTitleWidth: Int = Int.MAX_VALUE
    private var foreground: Boolean = true

    protected val labelVPadding: Int = labelVPadding

    var rectLeft: Int = 0
    var rectTop: Int = 0
    var rectWidth: Int = 0
    var rectHeight: Int = 0

    init {
        setTitle(label)
    }

    abstract fun setTitle(title: String)
    abstract fun getTitle(): String
    abstract fun draw()
    abstract fun reshape(width: Int, height: Int, calledFromParent: Boolean = true)

    fun setValue(value: Any) {
        setTitle(value.toString())
    }

    fun setForeground(b: Boolean) { foreground = b }
    fun getForeground(): Boolean = foreground

    fun setMaxTitleWidth(maxWidth: Int) {
        maxTitleWidth = minOf(maxWidth, maxTitleWidth)
    }
    fun getMaxTitleWidth(): Int = maxTitleWidth

    fun setButtonsRect(rect: Rect) { buttonsRect = rect }
    fun getButtonsRect(): Rect = buttonsRect

    fun setTitleVisible(visible: Boolean) {
        titleBox?.visible = visible
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("DragHandle: handleMouseDown not yet implemented")
        localPointToScreen(x, y).also { (sx, sy) ->
            dragLastScreenX = sx; dragLastScreenY = sy
            lastMouseScreenX = sx; lastMouseScreenY = sy
        }
        return true
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("DragHandle: handleMouseUp not yet implemented")
        return true
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (!hasMouseCapture()) {
            // no-op
            return true
        }

        val (screenX, screenY) = localPointToScreen(x, y)
        val deltaX = screenX - dragLastScreenX
        val deltaY = screenY - dragLastScreenY

        val parent = getParent()
        if (parent != null && parent.isDocked()) {
            val SLOP = 12
            if (abs(deltaY) >= SLOP) {
                parent.setDocked(false, false)
                return true
            }
            return false
        }

        val originalRect = parent?.getRect() ?: return true
        val translatedRect = originalRect.translate(deltaX, deltaY)
        parent?.setRect(translatedRect)

        val preSnapX = translatedRect.left
        val preSnapY = translatedRect.bottom
        dragLastScreenX = screenX
        dragLastScreenY = screenY

        val mouseDirX = if (screenX == lastMouseScreenX) lastMouseDirX else screenX - lastMouseScreenX
        val mouseDirY = if (screenY == lastMouseScreenY) lastMouseDirY else screenY - lastMouseScreenY
        lastMouseDirX = mouseDirX
        lastMouseDirY = mouseDirY
        lastMouseScreenX = screenX
        lastMouseScreenY = screenY

        // no-op

        return true
    }

    private fun localPointToScreen(x: Int, y: Int): Pair<Int, Int> {
        return Pair(x, y)
    }

    private fun hasMouseCapture(): Boolean {
        return false
    }

    private fun getParent(): FloaterStub? {
        return null
    }

    data class Rect(val left: Int, val top: Int, val width: Int, val height: Int) {
        val bottom: Int get() = top - height
        fun translate(dx: Int, dy: Int): Rect = copy(left = left + dx, top = top + dy)
    }

    interface FloaterStub {
        fun isDocked(): Boolean
        fun setDocked(docked: Boolean, moveFloater: Boolean)
        fun getRect(): Rect
        fun setRect(rect: Rect)
        fun setShape(rect: Rect, sendChildReshape: Boolean = false)
        fun setSnappedTo(view: Any?)
        fun findSnapRect(mouseDir: Pair<Int, Int>, snapType: Int, margin: Int): Pair<Any?, Rect>
    }

    interface TitleBoxStub {
        var visible: Boolean
        fun setText(text: String)
        fun getText(): String
        fun setEnabled(enabled: Boolean)
        fun setShape(rect: Rect)
        fun getTextPixelHeight(): Int
        fun getTextPixelWidth(): Int
        fun getRect(): Rect
        fun reshape(width: Int, height: Int)
        fun setToolTip(text: String)
    }
}

class DragHandleTop(
    label: String = "",
    dragHighlightColor: Any? = null,
    dragShadowColor: Any? = null,
    labelVPadding: Int = -1,
    private val defaultTitleVPad: Int = 0
) : DragHandle(label, dragHighlightColor, dragShadowColor, labelVPadding) {

    override fun setTitle(title: String) {
        val trimmed = title.trim()
        val box = titleBox
        if (box != null) {
            box.setText(trimmed)
        } else {
            // no-op
        }
        reshapeTitleBox()
    }

    override fun getTitle(): String = titleBox?.getText() ?: ""

    override fun draw() {
        titleBox?.setEnabled(getForeground())
        // no-op
    }

    override fun reshape(width: Int, height: Int, calledFromParent: Boolean) {
        rectWidth = width
        rectHeight = height
        // no-op
        reshapeTitleBox()
    }

    private fun reshapeTitleBox() {
        val box = titleBox ?: return
        val vpad = if (labelVPadding == -1) defaultTitleVPad else labelVPadding
        val titleWidth = rectWidth - LEFT_PAD - 2 * BORDER_PAD - getButtonsRect().width
        val titleHeight = 0
        val titleRect = DragHandle.Rect(
            LEFT_PAD,
            rectHeight - vpad,
            titleWidth,
            titleHeight
        )
        box.setShape(titleRect)
    }
}

class DragHandleLeft(
    label: String = "",
    dragHighlightColor: Any? = null,
    dragShadowColor: Any? = null,
    labelVPadding: Int = -1
) : DragHandle(label, dragHighlightColor, dragShadowColor, labelVPadding) {

    override fun setTitle(title: String) {
        titleBox?.let {
            // no-op
            titleBox = null
        }
    }

    override fun getTitle(): String = ""

    override fun draw() {
        titleBox?.setEnabled(getForeground())
        // no-op
    }

    override fun reshape(width: Int, height: Int, calledFromParent: Boolean) {
        rectWidth = width
        rectHeight = height
        // no-op
    }
}
