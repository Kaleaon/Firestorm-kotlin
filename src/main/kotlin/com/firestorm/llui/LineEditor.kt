package com.firestorm.llui

const val CURSOR_FLASH_DELAY: Float = 1.0f
const val AUTO_SCROLL_TIME: Float = 0.05f
const val TRIPLE_CLICK_INTERVAL: Float = 0.3f
const val SPELLCHECK_DELAY: Float = 0.5f
const val PASSWORD_BULLET: String = "•"

typealias TextValidator = (String) -> Boolean
typealias AutoreplaceCallback = (startRef: IntArray, endRef: IntArray, text: StringBuilder, cursorRef: IntArray, orig: String) -> Unit

open class LineEditor(val name: String) {

    var text: String = ""
        private set

    var prevText: String = ""
    var label: String = ""
    var defaultText: String = ""

    var maxLengthBytes: Int = 4096
    var maxLengthChars: Int = 0

    var cursorPos: Int = 0
        private set

    var scrollHPos: Int = 0

    var textPadLeft: Int = 0
    var textPadRight: Int = 0
    var textLeftEdge: Int = 0
    var textRightEdge: Int = 0

    var commitOnFocusLost: Boolean = true
    var revertOnEsc: Boolean = true
    var keystrokeOnEsc: Boolean = false

    var keystrokeCallback: ((LineEditor) -> Unit)? = null

    var isSelecting: Boolean = false
    var selectionStart: Int = 0
    var selectionEnd: Int = 0
    var lastSelectionX: Int = -1
    var lastSelectionY: Int = -1
    var lastSelectionStart: Int = -1
    var lastSelectionEnd: Int = -1

    var spellCheck: Boolean = false
    var spellCheckStart: Int = -1
    var spellCheckEnd: Int = -1
    val misspellRanges: MutableList<Pair<UInt, UInt>> = mutableListOf()
    val suggestionList: MutableList<String> = mutableListOf()

    var prevalidator: TextValidator? = null
    var inputPrevalidator: TextValidator? = null

    var cursorColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var bgColor: FloatArray = floatArrayOf(0f, 0f, 0f, 1f)
    var fgColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var readOnlyFgColor: FloatArray = floatArrayOf(0.7f, 0.7f, 0.7f, 1f)
    var tentativeFgColor: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 1f)
    var highlightColor: FloatArray = floatArrayOf(0f, 0f, 1f, 0.5f)
    var preeditBgColor: FloatArray = floatArrayOf(0.8f, 0.8f, 0.8f, 1f)

    var borderThickness: Int = 0

    var ignoreArrowKeys: Boolean = false
    var ignoreTab: Boolean = true
    var drawAsterixes: Boolean = false
    var allowEmoji: Boolean = true

    var selectAllOnFocusReceived: Boolean = false
    var selectAllOnCommit: Boolean = true
    var passDelete: Boolean = false

    var readOnly: Boolean = false

    var showImageFocused: Boolean = false
    var showLabelFocused: Boolean = false
    var useBgColor: Boolean = false

    var bgImage: UIImage? = null
    var bgImageDisabled: UIImage? = null
    var bgImageFocused: UIImage? = null

    var replaceNewlinesWithSpaces: Boolean = true

    var haveHistory: Boolean = false
    val lineHistory: MutableList<String> = mutableListOf()
    private var currentHistoryIndex: Int = 0

    var showContextMenu: Boolean = true

    var tentative: Boolean = false

    var enabled: Boolean = true
    var visible: Boolean = true

    var autoreplaceCallback: AutoreplaceCallback? = null

    private var preeditString: String = ""
    private val preeditPositions: MutableList<Int> = mutableListOf()

    fun setDefaultText() { setText(defaultText) }

    fun setText(newText: String) {
        val clamped = when {
            maxLengthChars > 0 && newText.length > maxLengthChars -> newText.substring(0, maxLengthChars)
            maxLengthBytes > 0 && newText.toByteArray().size > maxLengthBytes -> {
                var end = newText.length
                while (end > 0 && newText.substring(0, end).toByteArray().size > maxLengthBytes) end--
                newText.substring(0, end)
            }
            else -> newText
        }
        text = clamped
        if (cursorPos > text.length) cursorPos = text.length
    }

    fun getText(): String = if (drawAsterixes) PASSWORD_BULLET.repeat(text.length) else text
    fun getRawText(): String = text

    fun getLength(): Int = text.length

    fun getCursor(): Int = cursorPos

    fun setCursor(pos: Int) {
        cursorPos = pos.coerceIn(0, text.length)
    }

    fun setCursorToEnd() { cursorPos = text.length }

    fun resetScrollPosition() { scrollHPos = 0 }

    fun setSelection(start: Int, end: Int) {
        isSelecting = true
        selectionStart = start.coerceIn(0, text.length)
        selectionEnd = end.coerceIn(0, text.length)
    }

    fun getSelectionRange(): Pair<Int, Int> = Pair(selectionStart, selectionEnd - selectionStart)

    fun hasSelection(): Boolean = selectionStart != selectionEnd

    fun startSelection() {
        isSelecting = true
        selectionStart = cursorPos
        selectionEnd = cursorPos
    }

    fun endSelection() { isSelecting = false }

    fun extendSelection(newCursorPos: Int) {
        selectionEnd = newCursorPos.coerceIn(0, text.length)
        cursorPos = selectionEnd
    }

    fun deleteSelection() {
        if (!hasSelection()) return
        val start = minOf(selectionStart, selectionEnd)
        val end = maxOf(selectionStart, selectionEnd)
        text = text.removeRange(start, end)
        cursorPos = start
        selectionStart = start
        selectionEnd = start
        isSelecting = false
    }

    fun selectAll() {
        selectionStart = 0
        selectionEnd = text.length
        isSelecting = true
    }

    fun canSelectAll(): Boolean = text.isNotEmpty()

    fun deselect() {
        isSelecting = false
        selectionStart = 0
        selectionEnd = 0
    }

    fun canDeselect(): Boolean = hasSelection()

    fun cut() {
        if (readOnly || !hasSelection()) return
        TODO("APR: use JVM clipboard for cut")
    }

    fun canCut(): Boolean = !readOnly && hasSelection()

    fun copy() {
        if (!hasSelection()) return
        TODO("APR: use JVM clipboard for copy")
    }

    fun canCopy(): Boolean = hasSelection()

    fun paste() {
        if (readOnly) return
        TODO("APR: use JVM clipboard for paste")
    }

    fun canPaste(): Boolean = !readOnly

    fun doDelete() {
        if (readOnly) return
        if (hasSelection()) {
            deleteSelection()
        } else if (cursorPos < text.length) {
            text = text.removeRange(cursorPos, cursorPos + 1)
        }
        onKeystroke()
    }

    fun canDoDelete(): Boolean = !readOnly && (hasSelection() || cursorPos < text.length)

    fun removeChar() {
        if (cursorPos > 0) {
            text = text.removeRange(cursorPos - 1, cursorPos)
            cursorPos--
        }
    }

    fun removeWord(prev: Boolean) {
        if (prev) {
            val newPos = prevWordPos(cursorPos)
            text = text.removeRange(newPos, cursorPos)
            cursorPos = newPos
        } else {
            val newPos = nextWordPos(cursorPos)
            text = text.removeRange(cursorPos, newPos)
        }
    }

    fun addChar(c: Char) {
        if (readOnly) return
        if (!allowEmoji && c.code > 0xFFFF) return
        if (inputPrevalidator != null && !inputPrevalidator!!.invoke(c.toString())) return
        if (hasSelection()) deleteSelection()
        if (maxLengthChars > 0 && text.length >= maxLengthChars) return
        if (maxLengthBytes > 0 && (text + c).toByteArray().size > maxLengthBytes) return
        text = text.substring(0, cursorPos) + c + text.substring(cursorPos)
        cursorPos++
        onKeystroke()
    }

    fun prevWordPos(pos: Int): Int {
        var p = pos
        while (p > 0 && text[p - 1].isWhitespace()) p--
        while (p > 0 && !text[p - 1].isWhitespace()) p--
        return p
    }

    fun nextWordPos(pos: Int): Int {
        var p = pos
        while (p < text.length && !text[p].isWhitespace()) p++
        while (p < text.length && text[p].isWhitespace()) p++
        return p
    }

    fun setSelectAllOnFocusReceived(b: Boolean) { selectAllOnFocusReceived = b }
    fun setSelectAllOnCommit(b: Boolean) { selectAllOnCommit = b }
    fun setCommitOnFocusLost(b: Boolean) { commitOnFocusLost = b }
    fun setRevertOnEsc(b: Boolean) { revertOnEsc = b }
    fun setKeystrokeOnEsc(b: Boolean) { keystrokeOnEsc = b }
    fun setIgnoreArrowKeys(b: Boolean) { ignoreArrowKeys = b }
    fun setIgnoreTab(b: Boolean) { ignoreTab = b }
    fun setPassDelete(b: Boolean) { passDelete = b }
    fun setAllowEmoji(b: Boolean) { allowEmoji = b }
    fun setDrawAsterixes(b: Boolean) { drawAsterixes = b }
    fun setReadOnly(b: Boolean) { readOnly = b }
    fun setFocus(b: Boolean) { if (b && selectAllOnFocusReceived) selectAll() }
    fun setVisible(v: Boolean) { visible = v }
    fun setEnabled(b: Boolean) { enabled = b }
    fun setTentative(t: Boolean) { tentative = t }

    fun setLabel(newLabel: String) { label = newLabel }
    fun getLabel(): String = label

    fun setMaxTextLength(max: Int) { maxLengthBytes = max }
    fun setMaxTextChars(max: Int) { maxLengthChars = max }

    fun getTextPadding(): Pair<Int, Int> = Pair(textPadLeft, textPadRight)
    fun setTextPadding(left: Int, right: Int) {
        textPadLeft = left
        textPadRight = right
    }

    fun setPrevalidate(validator: TextValidator) { prevalidator = validator }
    fun setPrevalidateInput(validator: TextValidator) { inputPrevalidator = validator }

    fun prevalidateInput(str: String): Boolean = inputPrevalidator?.invoke(str) ?: true

    fun setEnableLineHistory(enabled: Boolean) { haveHistory = enabled }
    fun updateHistory() {
        if (haveHistory) {
            lineHistory.add(text)
            currentHistoryIndex = lineHistory.size
        }
    }

    fun setReplaceNewlinesWithSpaces(replace: Boolean) { replaceNewlinesWithSpaces = replace }

    fun setBgImage(image: UIImage?) { bgImage = image }
    fun setBgImageFocused(image: UIImage?) { bgImageFocused = image }

    fun setAutoreplaceCallback(cb: AutoreplaceCallback) { autoreplaceCallback = cb }

    fun onKeystroke() {
        keystrokeCallback?.invoke(this)
    }

    fun setKeystrokeCallback(callback: (LineEditor) -> Unit) {
        keystrokeCallback = callback
    }

    fun getSpellCheck(): Boolean = spellCheck
    fun getSuggestion(index: UInt): String = suggestionList.getOrElse(index.toInt()) { "" }
    fun getSuggestionCount(): UInt = suggestionList.size.toUInt()

    fun replaceWithSuggestion(index: UInt) {
        val suggestion = suggestionList.getOrNull(index.toInt()) ?: return
        if (hasSelection()) deleteSelection()
        setText(suggestion)
    }

    fun addToDictionary() { TODO("APR: use JVM spell-check dictionary") }
    fun canAddToDictionary(): Boolean = isMisspelledWord(cursorPos.toUInt())

    fun addToIgnore() { TODO("APR: use JVM spell-check ignore list") }
    fun canAddToIgnore(): Boolean = isMisspelledWord(cursorPos.toUInt())

    fun getMisspelledWord(pos: UInt): String {
        val range = misspellRanges.firstOrNull { pos >= it.first && pos < it.second } ?: return ""
        return text.substring(range.first.toInt(), range.second.toInt())
    }

    fun isMisspelledWord(pos: UInt): Boolean =
        misspellRanges.any { pos >= it.first && pos < it.second }

    fun onSpellCheckSettingsChange() {}

    open fun onFocusReceived() {
        prevText = text
        if (selectAllOnFocusReceived) selectAll()
    }

    open fun onFocusLost() {
        if (commitOnFocusLost) onCommit()
        endSelection()
    }

    open fun onCommit() {
        if (selectAllOnCommit) selectAll()
    }

    open fun clear() { setText("") }

    open fun onTabInto() { setFocus(true) }

    fun acceptsTextInput(): Boolean = true
    fun isDirty(): Boolean = text != prevText
    fun resetDirty() { prevText = text }

    fun setValue(value: Any?) { setText(value?.toString() ?: "") }
    fun getValue(): Any? = text

    fun setTextArg(key: String, value: String): Boolean {
        label = label.replace("[${key}]", value)
        return true
    }

    fun setLabelArg(key: String, value: String): Boolean {
        label = label.replace("[${key}]", value)
        return true
    }

    open fun draw() { TODO("GPU: render line editor '${name}'") }

    open fun reshape(width: Int, height: Int, fromParent: Boolean = true) {}

    open fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        setCursorAtLocalPos(x)
        return true
    }

    open fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        endSelection()
        return true
    }

    open fun handleHover(x: Int, y: Int, mask: UInt): Boolean = true

    open fun handleDoubleClick(x: Int, y: Int, mask: UInt): Boolean {
        val wordStart = prevWordPos(cursorPos)
        val wordEnd = nextWordPos(cursorPos)
        setSelection(wordStart, wordEnd)
        return true
    }

    open fun handleMiddleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        TODO("APR: use JVM primary selection/clipboard for middle-click paste")
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        if (showContextMenu) showContextMenu(x, y)
        return true
    }

    open fun handleKeyHere(key: Int, mask: UInt): Boolean = false

    open fun handleUnicodeCharHere(c: Char): Boolean {
        addChar(c)
        return true
    }

    open fun onMouseCaptureLost() { endSelection() }

    fun updatePrimary() { TODO("APR: use JVM primary selection") }
    fun copyPrimary() { TODO("APR: use JVM primary selection for copy") }
    fun pastePrimary() { TODO("APR: use JVM primary selection for paste") }
    fun canPastePrimary(): Boolean = true

    protected fun setCursorAtLocalPos(localX: Int) {
        TODO("GPU: compute cursor position from pixel x=$localX")
    }

    fun showContextMenu(x: Int, y: Int, setCursorPos: Boolean = true) {
        TODO("GPU: show line editor context menu at ($x, $y)")
    }

    private fun hasPreeditString(): Boolean = preeditString.isNotEmpty()

    fun resetPreedit() { preeditString = "" }

    fun evaluateFloat(): Boolean {
        val d = text.toDoubleOrNull() ?: return false
        setText(d.toString())
        return true
    }

    companion object {
        fun postvalidateFloat(str: String): Boolean = str.toDoubleOrNull() != null
    }

    inner class Rollback {
        private val savedText: String = this@LineEditor.text
        private val savedCursor: Int = this@LineEditor.cursorPos
        private val savedScrollH: Int = this@LineEditor.scrollHPos
        private val savedIsSelecting: Boolean = this@LineEditor.isSelecting
        private val savedSelStart: Int = this@LineEditor.selectionStart
        private val savedSelEnd: Int = this@LineEditor.selectionEnd

        fun doRollback() {
            this@LineEditor.text = savedText
            this@LineEditor.prevText = savedText
            this@LineEditor.cursorPos = savedCursor
            this@LineEditor.scrollHPos = savedScrollH
            this@LineEditor.isSelecting = savedIsSelecting
            this@LineEditor.selectionStart = savedSelStart
            this@LineEditor.selectionEnd = savedSelEnd
        }

        fun getText(): String = savedText
    }
}
