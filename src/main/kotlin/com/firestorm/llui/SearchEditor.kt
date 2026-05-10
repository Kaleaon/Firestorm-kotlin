package com.firestorm.llui

import com.firestorm.llmath.Rect

typealias CommitCallback = (source: Any?, value: Any?) -> Unit

open class SearchEditor(
    name: String,
    rect: Rect = Rect(),
    searchButtonVisible: Boolean = false,
    clearButtonVisible: Boolean = false,
    val highlightTextField: Boolean = false,
    val backgroundImage: Any? = null,
    val backgroundImageFocused: Any? = null,
    val backgroundImageHighlight: Any? = null
) : View(name, rect) {

    var keystrokeCallback: CommitCallback? = null
    var textChangedCallback: CommitCallback? = null

    protected val searchEditor: LineEditor = LineEditor("filter edit box", rect)
    protected val searchButton: Button? = if (searchButtonVisible) Button("search button") else null
    protected val clearButton: Button? = if (clearButtonVisible) Button("clear button") else null

    init {
        searchEditor.revertOnEsc = false
        searchEditor.commitCallback = { onCommit() }
        searchEditor.keystrokeCallback = { handleKeystroke() }
        addChild(searchEditor)
        searchButton?.let { searchEditor.addChild(it) }
        clearButton?.let { searchEditor.addChild(it) }
    }

    fun setCommitOnFocusLost(b: Boolean) {
        searchEditor.commitOnFocusLost = b
    }

    override fun draw() {
        clearButton?.visible = searchEditor.getText().isNotEmpty()

        if (highlightTextField) {
            if (searchEditor.getText().isNotEmpty()) {
                searchEditor.setBgImage(backgroundImageHighlight)
                searchEditor.setBgImageFocused(backgroundImageHighlight)
            } else {
                searchEditor.setBgImage(backgroundImage)
                searchEditor.setBgImageFocused(backgroundImageFocused)
            }
        }

        super.draw()
    }

    open fun setValue(value: Any?) {
        searchEditor.setText(value?.toString() ?: "")
    }

    open fun getValue(): Any? = searchEditor.getText()

    open fun setTextArg(key: String, text: String): Boolean = searchEditor.setTextArg(key, text)

    open fun setLabelArg(key: String, text: String): Boolean = searchEditor.setLabelArg(key, text)

    open fun setLabel(newLabel: String) { searchEditor.label = newLabel }

    open fun clear() { searchEditor.clear() }

    open fun setFocus(b: Boolean) { searchEditor.setFocus(b) }

    fun setText(newText: String) { searchEditor.setText(newText) }
    fun getText(): String = searchEditor.getText()

    fun setKeystrokeCallback(cb: CommitCallback) { keystrokeCallback = cb }
    fun setTextChangedCallback(cb: CommitCallback) { textChangedCallback = cb }

    protected fun onClearButtonClick() {
        setText("")
        textChangedCallback?.invoke(this, getValue())
        searchEditor.onCommit()
    }

    protected open fun handleKeystroke() {
        keystrokeCallback?.invoke(this, getValue())

        val key = currentKey()
        if (key == KEY_LEFT || key == KEY_RIGHT) return

        textChangedCallback?.invoke(this, getValue())
    }

    open fun onCommit() {}
    open fun currentKey(): Int = KEY_NONE

    companion object {
        const val KEY_NONE = 0
        const val KEY_LEFT = 0x83
        const val KEY_RIGHT = 0x84
    }
}

private var LineEditor.revertOnEsc: Boolean
    get() = false
    set(_) {}

private var LineEditor.commitOnFocusLost: Boolean
    get() = true
    set(_) {}

private var LineEditor.commitCallback: (() -> Unit)?
    get() = null
    set(v) {}

private var LineEditor.keystrokeCallback: (() -> Unit)?
    get() = null
    set(v) {}

private fun LineEditor.setFocus(b: Boolean) {}
private fun LineEditor.onCommit() {}
private fun LineEditor.setBgImage(img: Any?) {}
private fun LineEditor.setBgImageFocused(img: Any?) {}
private fun LineEditor.setTextArg(key: String, text: String): Boolean = true
private fun LineEditor.setLabelArg(key: String, text: String): Boolean = true
private fun LineEditor.clear() { setText("") }
