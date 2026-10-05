package com.firestorm.llui

import com.firestorm.llmath.Rect

class LineEditor(
    name: String,
    rect: Rect = Rect()
) : View(name, rect) {

    var text: String = ""
    var maxLength: Int = 255
    var secret: Boolean = false
    var prevalidate: ((String) -> Boolean)? = null
    var commitCallback: ((LineEditor) -> Unit)? = null

    fun getText(): String = text

    fun setText(newText: String) {
        val clamped = if (newText.length > maxLength) newText.substring(0, maxLength) else newText
        if (prevalidate?.invoke(clamped) != false) {
            text = clamped
        }
    }

    override fun handleKey(key: Int, mask: UInt, called: Boolean): Boolean {
        if (!enabled) return false
        // KEY_RETURN = 13
        if (key == 13) {
            commitCallback?.invoke(this)
            return true
        }
        return false
    }

    override fun draw() {
        if (!visible) return
        // stub: render text (masked if secret) and cursor
    }
}
