package com.firestorm.llui

import com.firestorm.llmath.Rect

typealias TransparencyType = Int
typealias TransparencyOverrideCallback = (type: TransparencyType, defaultValue: Float) -> Float

open class FilterEditor(
    name: String,
    rect: Rect = Rect(),
) : SearchEditor(name, rect) {

    init {
        setCommitOnFocusLost(false)
    }

    override fun handleKeystroke() {
        super.handleKeystroke()
        onCommit()
    }

    open fun setTransparencyOverrideCallback(cb: TransparencyOverrideCallback) {
        System.err.println("FilterEditor: setTransparencyOverrideCallback not yet implemented")
    }
}
