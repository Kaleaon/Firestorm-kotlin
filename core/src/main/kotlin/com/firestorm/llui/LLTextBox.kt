package com.firestorm.llui

import com.firestorm.llcommon.LLSD
import com.firestorm.llmath.Rect

class TextBox(
    name: String,
    rect: Rect = Rect()
) : View(name, rect) {

    enum class HAlign { LEFT, CENTER, RIGHT }

    var text: String = ""
    var wrap: Boolean = false
    var hAlign: HAlign = HAlign.LEFT

    fun setText(text: String) {
        this.text = text
    }

    fun getValue(): LLSD = LLSD.of(text)

    override fun draw() {
        if (!visible) return
        // stub: render text with hAlign and wrap settings
    }
}
