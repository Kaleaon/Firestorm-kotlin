package com.firestorm.llui

typealias CommitCallback = (source: Any?, value: Any?) -> Unit

open class LineEditor(
    val name: String = "",
    var text: String = "",
    var label: String = "",
    var textPadLeft: Int = 0,
    var textPadRight: Int = 0,
    var revertOnEsc: Boolean = true,
    var commitOnFocusLost: Boolean = true,
    var bgImage: Any? = null,
    var bgImageFocused: Any? = null,
    var passDelete: Boolean = false,
    var commitCallback: CommitCallback? = null,
    var keystrokeCallback: (() -> Unit)? = null
) {
    fun getText(): String = text
    fun getWText(): String = text
    fun setText(t: String) { text = t }
    fun setValue(v: Any?) { text = v?.toString() ?: "" }
    fun getValue(): Any? = text
    fun setTextArg(key: String, replacement: String): Boolean = true
    fun setLabelArg(key: String, replacement: String): Boolean = true
    fun setLabel(newLabel: String) { label = newLabel }
    fun clear() { text = "" }
    fun setFocus(b: Boolean) {}
    fun hasFocus(): Boolean = false
    fun isDirty(): Boolean = false
    fun setBgImage(img: Any?) { bgImage = img }
    fun setBgImageFocused(img: Any?) { bgImageFocused = img }
    fun setCommitOnFocusLost(b: Boolean) { commitOnFocusLost = b }
    fun setPassDelete(b: Boolean) { passDelete = b }
    fun onCommit() { commitCallback?.invoke(this, getValue()) }
    fun deleteAllChildren() {}
    fun addChild(child: Any) {}
    fun evaluateFloat(): Boolean = text.toFloatOrNull() != null
    fun resetScrollPosition() {}
}

open class Button(val name: String = "") {
    var visible: Boolean = true
    fun setVisible(b: Boolean) { visible = b }
}

open class SearchEditor(
    searchButtonVisible: Boolean = false,
    clearButtonVisible: Boolean = false,
    highlightTextField: Boolean = false,
    backgroundImage: Any? = null,
    backgroundImageFocused: Any? = null,
    backgroundImageHighlight: Any? = null,
    lineEditorFactory: () -> LineEditor = { LineEditor() },
    searchButtonFactory: (() -> Button)? = null,
    clearButtonFactory: (() -> Button)? = null
) : UiCtrl() {

    var keystrokeCallback: CommitCallback? = null
    var textChangedCallback: CommitCallback? = null

    protected val searchEditor: LineEditor = lineEditorFactory().also { editor ->
        editor.revertOnEsc = false
        editor.commitCallback = { _, _ -> onCommit() }
        editor.keystrokeCallback = { handleKeystroke() }
        editor.setPassDelete(true)
    }

    protected val searchButton: Button? = if (searchButtonVisible) searchButtonFactory?.invoke() else null
    protected val clearButton: Button? = if (clearButtonVisible) clearButtonFactory?.invoke() else null

    private val editorImage: Any? = backgroundImage
    private val editorImageFocused: Any? = backgroundImageFocused
    private val editorSearchImage: Any? = backgroundImageHighlight
    private val highlightTextField: Boolean = highlightTextField

    fun setCommitOnFocusLost(b: Boolean) {
        searchEditor.setCommitOnFocusLost(b)
    }

    open fun draw() {
        clearButton?.setVisible(searchEditor.getWText().isNotEmpty())

        if (highlightTextField) {
            if (searchEditor.getWText().isNotEmpty()) {
                searchEditor.setBgImage(editorSearchImage)
                searchEditor.setBgImageFocused(editorSearchImage)
            } else {
                searchEditor.setBgImage(editorImage)
                searchEditor.setBgImageFocused(editorImageFocused)
            }
        }
    }

    fun setText(newText: String) {
        searchEditor.setText(newText)
    }

    fun getText(): String = searchEditor.getText()

    open fun setValue(value: Any?) {
        searchEditor.setValue(value)
    }

    open fun getValue(): Any? = searchEditor.getValue()

    open fun setTextArg(key: String, text: String): Boolean = searchEditor.setTextArg(key, text)

    open fun setLabelArg(key: String, text: String): Boolean = searchEditor.setLabelArg(key, text)

    open fun setLabel(newLabel: String) {
        searchEditor.setLabel(newLabel)
    }

    open fun clear() {
        searchEditor.clear()
    }

    open fun setFocus(b: Boolean) {
        searchEditor.setFocus(b)
    }

    fun setKeystrokeCallback(cb: CommitCallback) {
        keystrokeCallback = cb
    }

    fun setTextChangedCallback(cb: CommitCallback) {
        textChangedCallback = cb
    }

    protected fun onClearButtonClick() {
        setText("")
        textChangedCallback?.invoke(this, getValue())
        searchEditor.onCommit()
    }

    protected open fun handleKeystroke() {
        keystrokeCallback?.invoke(this, getValue())

        val key = currentKey()
        if (key == Key.LEFT || key == Key.RIGHT) return

        textChangedCallback?.invoke(this, getValue())
    }

    open fun onCommit() {}
    open fun currentKey(): Key = Key.NONE

    override fun toString(): String = "SearchEditor(text=${searchEditor.getText()})"
}
