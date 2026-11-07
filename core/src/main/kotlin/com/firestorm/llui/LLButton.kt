package com.firestorm.llui

import com.firestorm.llmath.Rect

class Button(
    name: String,
    rect: Rect = Rect()
) : View(name, rect) {

    var label: String = ""
    var isToggle: Boolean = false
    var isToggled: Boolean = false
    var clickCallback: (() -> Unit)? = null
    var heldDownCallback: (() -> Unit)? = null

    private var mouseDown: Boolean = false

    override fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        if (!enabled) return false
        mouseDown = true
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        if (!enabled) return false
        if (mouseDown) {
            mouseDown = false
            if (isToggle) isToggled = !isToggled
            clickCallback?.invoke()
        }
        return true
    }

    fun setClickedCallback(fn: () -> Unit) {
        clickCallback = fn
    }

    fun setLabel(text: String) {
        label = text
    }

    override fun draw() {
        if (!visible) return
        // stub: draw button background and label
    }
}
