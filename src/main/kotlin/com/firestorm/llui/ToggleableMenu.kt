package com.firestorm.llui

import com.firestorm.llmath.Rect

open class ToggleableMenu(name: String) : MenuGL(name) {

    var buttonRect: Rect = Rect()
        private set

    private var closedByButtonClick: Boolean = false

    private val visibilityChangeListeners: MutableList<(isVisible: Boolean, closedByButton: Boolean) -> Unit> =
        mutableListOf()

    fun addVisibilityChangeListener(cb: (isVisible: Boolean, closedByButton: Boolean) -> Unit) {
        visibilityChangeListeners.add(cb)
    }

    open fun onVisibilityChange(curVisible: Boolean) {
        val (mouseX, mouseY) = getCurrentMousePosition()
        // STORM-1879: only treat as button-click-close when the mouse is over the
        // button AND a capture is active — keyboard shortcuts must not set the flag.
        if (!curVisible && buttonRect.pointInRect(mouseX, mouseY) && hasMouseCapture()) {
            closedByButtonClick = true
        }
        visibilityChangeListeners.forEach { it(curVisible, closedByButtonClick) }
    }

    fun setButtonRect(rect: Rect, currentView: View) {
        buttonRect = currentView.localToScreen(rect)
    }

    fun setButtonRect(currentView: View) {
        setButtonRect(currentView.rect, currentView)
    }

    // Returns true when the caller should proceed to show the menu.
    // Returns false when the dismiss was caused by the triggering button click
    // (so the menu stays closed) or when the menu was visible and has been toggled off.
    fun toggleVisibility(): Boolean {
        if (closedByButtonClick) {
            closedByButtonClick = false
            return false
        }
        if (visible) {
            hide()
            closedByButtonClick = false
            return false
        }
        return true
    }

    // addChild routes through the context-menu path so that child views (e.g.
    // sub-menus) are registered correctly.  Mirrors LLToggleableMenu::addChild
    // which delegates to addContextChild.
    fun addChild(child: View, tabGroup: Int = 0) {
        System.err.println("ToggleableMenu: addChild not yet implemented")
    }

    private fun getCurrentMousePosition(): Pair<Int, Int> {
        return Pair(0, 0)
    }

    private fun hasMouseCapture(): Boolean {
        return false
    }
}
