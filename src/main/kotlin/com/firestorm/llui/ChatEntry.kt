package com.firestorm.llui

data class ChatEntryParams(
    val hasHistory: Boolean = true,
    val isExpandable: Boolean = false,
    val expandLinesCount: Int = 1,
    val label: String = ""
)

class ChatEntry(private val params: ChatEntryParams) {
    private val lineHistory: MutableList<String> = mutableListOf()
    private var currentHistoryIndex: Int = 0

    private var hasHistory: Boolean = params.hasHistory
    private var isExpandable: Boolean = params.isExpandable
    private var expandLinesCount: Int = params.expandLinesCount
    private var isSingleLineMode: Boolean = false

    private var prevLinesCount: Int = 0
    private var prevExpandedLineCount: Int = Int.MAX_VALUE

    private var currentInput: String = ""

    var text: String = ""
        private set

    var label: String = params.label

    private val textExpandedListeners: MutableList<() -> Unit> = mutableListOf()

    fun onTextExpanded(listener: () -> Unit) {
        textExpandedListeners.add(listener)
    }

    fun removeTextExpandedListener(listener: () -> Unit) {
        textExpandedListeners.remove(listener)
    }

    fun draw(lineCount: Int, visibleLinesCount: Int) {
        if (isExpandable) {
            expandText(lineCount, visibleLinesCount)
        }
    }

    fun onCommit() {
        updateHistory()
    }

    fun onFocusReceived() {
        TODO("APR: use JVM equivalent — notify focus system and enable language input")
    }

    fun onFocusLost() {
        TODO("APR: use JVM equivalent — notify focus system")
    }

    fun enableSingleLineMode(singleLineMode: Boolean) {
        isSingleLineMode = singleLineMode
        prevLinesCount = -1
        if (singleLineMode) {
            text = text.replace('\n', '¶')
        }
    }

    fun setText(newText: String) {
        text = newText
    }

    fun getText(): String = text

    fun getLength(): Int = text.length

    fun useLabel(): Boolean = text.isEmpty() && label.isNotEmpty()

    fun beforeValueChange() {
        if (text.isEmpty() && label.isNotEmpty()) {
            clearSegments()
        }
    }

    fun onValueChange(start: Int, end: Int) {
        resetLabel()
    }

    fun updateHistory() {
        if (hasHistory && text.isNotEmpty()) {
            if (lineHistory.isEmpty() || text != lineHistory.last()) {
                lineHistory.add(text)
            }
            currentHistoryIndex = lineHistory.size
        }
    }

    fun handleKeyReturn() {
        currentInput = ""
    }

    fun handleKeyUp(ctrlHeld: Boolean): Boolean {
        if (!hasHistory || !ctrlHeld) return false
        if (lineHistory.isNotEmpty() && currentHistoryIndex > 0) {
            if (currentHistoryIndex == lineHistory.size) {
                currentInput = text
            }
            currentHistoryIndex--
            text = lineHistory[currentHistoryIndex]
        }
        return true
    }

    fun handleKeyDown(ctrlHeld: Boolean): Boolean {
        if (!hasHistory || !ctrlHeld) return false
        if (lineHistory.isNotEmpty() && currentHistoryIndex < lineHistory.size - 1) {
            currentHistoryIndex++
            text = lineHistory[currentHistoryIndex]
        } else if (lineHistory.isNotEmpty() && currentHistoryIndex == lineHistory.size - 1) {
            currentHistoryIndex++
            text = currentInput
        }
        return true
    }

    fun paste(clipboardText: String) {
        text += clipboardText
        if (isSingleLineMode) {
            text = text.replace('\n', '¶')
        }
    }

    fun insertMentionAtCursor(str: String, cursorPos: Int): Int {
        val cursorFromEnd = text.length - cursorPos
        text = text.substring(0, cursorPos) + str + text.substring(cursorPos)
        return text.length - cursorFromEnd
    }

    private fun expandText(lineCount: Int, visibleLinesCount: Int) {
        val maxLines = if (isSingleLineMode) 1 else expandLinesCount
        val canChange = lineCount <= maxLines || maxLines < prevExpandedLineCount
        prevExpandedLineCount = maxLines

        val textPasted = lineCount > maxLines && visibleLinesCount < maxLines

        if (canChange || textPasted || isSingleLineMode) {
            prevLinesCount = lineCount
            for (listener in textExpandedListeners) listener()
        }
    }

    private fun clearSegments() {
    }

    private fun resetLabel() {
    }
}
