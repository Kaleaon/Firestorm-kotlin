package com.firestorm.llwindow

enum class EMouseClickType {
    CLICK_NONE,
    CLICK_LEFT,
    CLICK_MIDDLE,
    CLICK_RIGHT,
    CLICK_BUTTON4,
    CLICK_BUTTON5,
    CLICK_DOUBLELEFT,
}

abstract class LLMouseHandler {

    enum class EShowToolTip {
        SHOW_NEVER,
        SHOW_IF_NOT_BLOCKED,
        SHOW_ALWAYS,
    }

    open fun handleAnyMouseClick(x: Int, y: Int, mask: Int, clicktype: EMouseClickType, down: Boolean): Boolean {
        if (down) {
            return when (clicktype) {
                EMouseClickType.CLICK_LEFT        -> handleMouseDown(x, y, mask)
                EMouseClickType.CLICK_RIGHT       -> handleRightMouseDown(x, y, mask)
                EMouseClickType.CLICK_MIDDLE      -> handleMiddleMouseDown(x, y, mask)
                EMouseClickType.CLICK_DOUBLELEFT  -> handleDoubleClick(x, y, mask)
                EMouseClickType.CLICK_BUTTON4,
                EMouseClickType.CLICK_BUTTON5     -> false
                else                              -> false
            }
        } else {
            return when (clicktype) {
                EMouseClickType.CLICK_LEFT        -> handleMouseUp(x, y, mask)
                EMouseClickType.CLICK_RIGHT       -> handleRightMouseUp(x, y, mask)
                EMouseClickType.CLICK_MIDDLE      -> handleMiddleMouseUp(x, y, mask)
                EMouseClickType.CLICK_DOUBLELEFT  -> handleDoubleClick(x, y, mask)
                EMouseClickType.CLICK_BUTTON4,
                EMouseClickType.CLICK_BUTTON5     -> false
                else                              -> false
            }
        }
    }

    abstract fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean
    abstract fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean
    abstract fun handleMiddleMouseDown(x: Int, y: Int, mask: Int): Boolean
    abstract fun handleMiddleMouseUp(x: Int, y: Int, mask: Int): Boolean
    abstract fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean
    abstract fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean
    abstract fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean

    abstract fun handleHover(x: Int, y: Int, mask: Int): Boolean
    abstract fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean
    abstract fun handleScrollHWheel(x: Int, y: Int, clicks: Int): Boolean
    abstract fun handleToolTip(x: Int, y: Int, mask: Int): Boolean
    abstract fun getName(): String

    abstract fun onMouseCaptureLost()

    abstract fun screenPointToLocal(screenX: Int, screenY: Int, localX: IntArray, localY: IntArray)
    abstract fun localPointToScreen(localX: Int, localY: Int, screenX: IntArray, screenY: IntArray)

    abstract fun hasMouseCapture(): Boolean
}
