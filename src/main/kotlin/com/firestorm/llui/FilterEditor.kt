package com.firestorm.llui

typealias TransparencyType = Int
typealias TransparencyOverrideCallback = (type: TransparencyType, defaultValue: Float) -> Float

open class SearchEditor {
    var commitOnFocusLost: Boolean = true
    protected var transparencyOverride: TransparencyOverrideCallback? = null
    protected var onCommitCallback: (() -> Unit)? = null
    protected var onKeystrokeCallback: (() -> Unit)? = null

    open fun setTransparencyOverrideCallback(cb: TransparencyOverrideCallback) {
        transparencyOverride = cb
    }

    open fun handleKeystroke() {
        onKeystrokeCallback?.invoke()
    }

    fun onCommit() {
        onCommitCallback?.invoke()
    }

    fun setOnCommit(cb: () -> Unit) {
        onCommitCallback = cb
    }

    fun setOnKeystroke(cb: () -> Unit) {
        onKeystrokeCallback = cb
    }
}

class FilterEditor : SearchEditor() {
    val searchEditor: SearchEditor = SearchEditor()

    init {
        commitOnFocusLost = false
    }

    override fun handleKeystroke() {
        super.handleKeystroke()
        onCommit()
    }

    override fun setTransparencyOverrideCallback(cb: TransparencyOverrideCallback) {
        searchEditor.setTransparencyOverrideCallback(cb)
    }
}
