package com.firestorm.llui

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

abstract class TextCmd(val position: Int, val groupWithNext: Boolean) {
    abstract fun execute(editor: TextEditor, delta: IntArray): Boolean
    abstract fun undo(editor: TextEditor): Int
    abstract fun redo(editor: TextEditor): Int
    open fun canExtend(pos: Int): Boolean = false
    open fun blockExtensions() {}
    open fun extendAndExecute(editor: TextEditor, pos: Int, wc: Int, delta: IntArray): Boolean = false

    protected fun insertText(editor: TextEditor, pos: Int, str: String): Int {
        return editor.rawInsert(pos, str)
    }

    protected fun removeText(editor: TextEditor, pos: Int, len: Int): Int {
        return editor.rawRemove(pos, len)
    }

    protected fun overwriteText(editor: TextEditor, pos: Int, wc: Int) {
        editor.rawOverwrite(pos, wc)
    }
}

private class TextCmdInsert(
    pos: Int, groupWithNext: Boolean,
    private var wstr: String
) : TextCmd(pos, groupWithNext) {
    override fun execute(editor: TextEditor, delta: IntArray): Boolean {
        delta[0] = insertText(editor, position, wstr)
        if (delta[0] < wstr.length) wstr = wstr.substring(0, delta[0])
        return delta[0] != 0
    }
    override fun undo(editor: TextEditor): Int {
        removeText(editor, position, wstr.length)
        return position
    }
    override fun redo(editor: TextEditor): Int {
        insertText(editor, position, wstr)
        return position + wstr.length
    }
}

private class TextCmdAddChar(
    pos: Int, groupWithNext: Boolean,
    private val wc: Int
) : TextCmd(pos, groupWithNext) {
    private var wstr: String = String(intArrayOf(wc), 0, 1)
    private var blockExtensionsFlag = false

    override fun blockExtensions() { blockExtensionsFlag = true }

    override fun canExtend(pos: Int): Boolean =
        !blockExtensionsFlag && pos == position + wstr.length

    override fun execute(editor: TextEditor, delta: IntArray): Boolean {
        delta[0] = insertText(editor, position, wstr)
        if (delta[0] < wstr.length) wstr = wstr.substring(0, delta[0])
        return delta[0] != 0
    }

    override fun extendAndExecute(editor: TextEditor, pos: Int, wc: Int, delta: IntArray): Boolean {
        val ws = String(intArrayOf(wc), 0, 1)
        delta[0] = insertText(editor, pos, ws)
        if (delta[0] > 0) wstr += ws
        return delta[0] != 0
    }

    override fun undo(editor: TextEditor): Int {
        removeText(editor, position, wstr.length)
        return position
    }
    override fun redo(editor: TextEditor): Int {
        insertText(editor, position, wstr)
        return position + wstr.length
    }
}

private class TextCmdOverwriteChar(
    pos: Int, groupWithNext: Boolean,
    private val wc: Int
) : TextCmd(pos, groupWithNext) {
    private var oldChar: Int = 0

    override fun execute(editor: TextEditor, delta: IntArray): Boolean {
        oldChar = editor.getText().codePointAt(position)
        overwriteText(editor, position, wc)
        delta[0] = 0
        return true
    }
    override fun undo(editor: TextEditor): Int {
        overwriteText(editor, position, oldChar)
        return position
    }
    override fun redo(editor: TextEditor): Int {
        overwriteText(editor, position, wc)
        return position + 1
    }
}

private class TextCmdRemove(
    pos: Int, groupWithNext: Boolean,
    private val len: Int
) : TextCmd(pos, groupWithNext) {
    private var removedStr: String = ""

    override fun execute(editor: TextEditor, delta: IntArray): Boolean {
        return try {
            removedStr = editor.getText().substring(position, position + len)
            delta[0] = removeText(editor, position, len)
            delta[0] != 0
        } catch (e: IndexOutOfBoundsException) { false }
    }
    override fun undo(editor: TextEditor): Int {
        insertText(editor, position, removedStr)
        return position + removedStr.length
    }
    override fun redo(editor: TextEditor): Int {
        removeText(editor, position, len)
        return position
    }
}

open class TextEditor(
    defaultText: String = "",
    val allowEmbeddedItems: Boolean = false,
    val autoIndent: Boolean = true,
    ignoreTab: Boolean = true,
    val commitOnFocusLost: Boolean = false,
    var showContextMenu: Boolean = true,
    var showEmojiHelper: Boolean = false,
    val enableTooltipPaste: Boolean = false,
    val enableTabRemove: Boolean = true,
    defaultColor: Color4 = Color4(1f, 1f, 1f)
) {

    companion object {
        const val FIRST_EMBEDDED_CHAR = 0x100000
        const val LAST_EMBEDDED_CHAR  = 0x10ffff
        const val MAX_EMBEDDED_ITEMS  = LAST_EMBEDDED_CHAR - FIRST_EMBEDDED_CHAR + 1
        const val SPACES_PER_TAB      = 4
        const val SPELLCHECK_DELAY    = 0.5f

        const val MASK_SHIFT   = 0x01
        const val MASK_CONTROL = 0x02
        const val MASK_NONE    = 0x00

        const val KEY_LEFT      = 0x25
        const val KEY_RIGHT     = 0x27
        const val KEY_UP        = 0x26
        const val KEY_DOWN      = 0x28
        const val KEY_HOME      = 0x24
        const val KEY_END       = 0x23
        const val KEY_PAGE_UP   = 0x21
        const val KEY_PAGE_DOWN = 0x22
        const val KEY_BACKSPACE = 0x08
        const val KEY_DELETE    = 0x2E
        const val KEY_INSERT    = 0x2D
        const val KEY_RETURN    = 0x0D
        const val KEY_TAB       = 0x09
    }

    protected var text: StringBuilder = StringBuilder(defaultText)
    protected var cursorPos: Int = 0
    protected var selectionStart: Int = 0
    protected var selectionEnd: Int = 0
    protected var isSelecting: Boolean = false
    protected var readOnly: Boolean = false
    protected var enabled: Boolean = true
    protected var parseOnTheFly: Boolean = true
    protected var showChatMentionPicker: Boolean = false
    protected var passDelete: Boolean = false
    protected var keepSelectionOnReturn: Boolean = false
    protected var selectAllOnFocusReceived: Boolean = false
    protected var selectedOnFocusReceived: Boolean = false

    protected var mouseDownX: Int = 0
    protected var mouseDownY: Int = 0
    protected var border: ViewBorder? = null

    protected var defaultColor: Color4 = defaultColor
    protected var tabsToNextField: Boolean = ignoreTab
    protected var parseHighlights: Boolean = false
    protected var autoReplaceCallback: ((IntArray, IntArray, StringBuilder, IntArray, String) -> Unit)? = null

    private var baseDocIsPristine: Boolean = true
    private var pristineCmd: TextCmd? = null
    private var lastCmd: TextCmd? = null
    private val undoStack: ArrayDeque<TextCmd> = ArrayDeque()
    private var keystrokeListeners: MutableList<(TextEditor) -> Unit> = mutableListOf()

    private var maxTextByteLength: Int = Int.MAX_VALUE

    fun getText(): String = text.toString()

    open fun setText(utf8str: String) {
        blockUndo()
        deselect()
        parseOnTheFly = false
        text.clear()
        text.append(utf8str)
        parseOnTheFly = true
        resetDirty()
    }

    fun reparseText(utf8str: String) {
        parseOnTheFly = false
        text.clear()
        text.append(utf8str)
        parseOnTheFly = true
    }

    fun getSelectionString(): String {
        val (start, len) = selectionRange()
        return if (len > 0) text.substring(start, start + len) else ""
    }

    fun selectNext(searchText: String, caseInsensitive: Boolean, wrap: Boolean = true, searchUp: Boolean = false) {
        if (searchText.isEmpty()) return

        val haystack = if (caseInsensitive) text.toString().lowercase() else text.toString()
        val needle   = if (caseInsensitive) searchText.lowercase() else searchText

        if (isSelecting) {
            val sel = text.substring(
                min(selectionStart, selectionEnd),
                max(selectionStart, selectionEnd)
            ).let { if (caseInsensitive) it.lowercase() else it }
            if (sel == needle) {
                cursorPos = if (searchUp) max(0, cursorPos - 1)
                            else cursorPos + needle.length
            }
        }

        var loc = if (searchUp) haystack.lastIndexOf(needle, max(0, cursorPos - needle.length))
                  else haystack.indexOf(needle, cursorPos)

        if (wrap && loc == -1) {
            loc = if (searchUp) haystack.lastIndexOf(needle) else haystack.indexOf(needle)
        }

        if (loc == -1) {
            isSelecting = false
            selectionEnd = 0
            selectionStart = 0
            return
        }

        cursorPos = loc
        isSelecting = true
        selectedOnFocusReceived = false
        selectionEnd = cursorPos
        selectionStart = min(text.length, cursorPos + needle.length)
    }

    fun replaceText(
        searchText: String, replaceText: String,
        caseInsensitive: Boolean, wrap: Boolean = true, searchUp: Boolean = false
    ): Boolean {
        if (searchText.isEmpty()) return false
        var replaced = false

        if (isSelecting) {
            val sel = text.substring(
                min(selectionStart, selectionEnd),
                max(selectionStart, selectionEnd)
            )
            val cmpSel    = if (caseInsensitive) sel.lowercase()        else sel
            val cmpSearch = if (caseInsensitive) searchText.lowercase() else searchText
            if (cmpSel == cmpSearch) {
                insertText(replaceText)
                replaced = true
            }
        }

        selectNext(searchText, caseInsensitive, wrap, searchUp)
        return replaced
    }

    fun replaceTextAll(searchText: String, replaceText: String, caseInsensitive: Boolean) {
        startOfDoc()
        selectNext(searchText, caseInsensitive, false)
        var replaced = true
        while (replaced) {
            replaced = replaceText(searchText, replaceText, caseInsensitive, false)
        }
    }

    fun prevWordPos(cursorPos: Int): Int {
        var pos = cursorPos
        while (pos > 0 && text[pos - 1] == ' ') pos--
        while (pos > 0 && text[pos - 1].isLetterOrDigit()) pos--
        return pos
    }

    fun nextWordPos(cursorPos: Int): Int {
        var pos = cursorPos
        while (pos < text.length && text[pos].isLetterOrDigit()) pos++
        while (pos < text.length && text[pos] == ' ') pos++
        return pos
    }

    fun hasSelection(): Boolean = selectionStart != selectionEnd

    open fun canSelectAll(): Boolean = true
    open fun canCut(): Boolean = !readOnly && hasSelection()
    open fun canCopy(): Boolean = hasSelection()
    open fun canPaste(): Boolean {
        return false
    }
    open fun canPastePrimary(): Boolean {
        return false
    }
    open fun canDoDelete(): Boolean = !readOnly && (hasSelection() || cursorPos < text.length)

    fun setKeystrokeCallback(cb: (TextEditor) -> Unit) {
        keystrokeListeners.add(cb)
    }

    fun setParseHighlights(parsing: Boolean) { parseHighlights = parsing }

    fun insertEmoji(emoji: Int) {
        System.err.println("TextEditor: insertEmoji not yet implemented")
    }

    fun handleEmojiCommit(emoji: Int) {
        System.err.println("TextEditor: handleEmojiCommit not yet implemented")
    }

    fun handleMentionCommit(nameUrl: String) {
        System.err.println("TextEditor: handleMentionCommit not yet implemented")
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (!(mask and MASK_SHIFT != 0)) deselect()

        if (mask and MASK_SHIFT != 0) {
            val oldCursor = cursorPos
            setCursorAtLocalPos(x, y, true)
            if (hasSelection()) selectionEnd = cursorPos
            else { selectionStart = oldCursor; selectionEnd = cursorPos }
            isSelecting = true
        } else {
            setCursorAtLocalPos(x, y, true)
            startSelection()
        }
        resetCursorBlink()
        selectedOnFocusReceived = false
        return true
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (isSelecting) {
            setCursorAtLocalPos(x, y, true)
            endSelection()
        }
        updatePrimary()
        resetCursorBlink()
        return true
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        setCursorAtLocalPos(x, y, false)
        if (showContextMenu) showContextMenu(x, y)
        return true
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (isSelecting) {
            setCursorAtLocalPos(x, y, true)
            selectionEnd = cursorPos
        }
        resetCursorBlink()
        // no-op
        return true
    }

    open fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        setCursorAtLocalPos(x, y, false)
        deselect()

        if (cursorPos < text.length && text[cursorPos].isLetterOrDigit()) {
            while (cursorPos > 0 && text[cursorPos - 1].isLetterOrDigit()) {
                if (!setCursorPos(cursorPos - 1)) break
            }
            startSelection()
            while (cursorPos < text.length && text[cursorPos].isLetterOrDigit()) {
                if (!setCursorPos(cursorPos + 1)) break
            }
            selectionEnd = cursorPos
        } else if (cursorPos < text.length && !text[cursorPos].isWhitespace()) {
            startSelection()
            setCursorPos(cursorPos + 1)
            selectionEnd = cursorPos
        }

        isSelecting = false
        resetCursorBlink()
        return true
    }

    open fun handleMiddleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (canPastePrimary()) {
            setCursorAtLocalPos(x, y, true)
            pastePrimary()
        }
        return true
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        var handled = false
        if (!readOnly) {
            handled = handleSpecialKey(key, mask)
        }
        if (!handled) handled = handleNavigationKey(key, mask)
        if (!handled) handled = handleSelectionKey(key, mask)
        if (!handled) handled = handleControlKey(key, mask)
        return handled
    }

    open fun handleUnicodeCharHere(uniChar: Int): Boolean {
        if (!enabled || readOnly) return false
        addChar(uniChar)
        return true
    }

    protected open fun handleSpecialKey(key: Int, mask: Int): Boolean {
        var handled = true
        when (key) {
            KEY_RETURN -> {
                if (mask == MASK_NONE) {
                    if (hasSelection() && !keepSelectionOnReturn) deleteSelection(false)
                    addLineBreakChar()
                } else handled = false
            }
            KEY_TAB -> {
                if (tabsToNextField) handled = false
                else {
                    if (mask and MASK_CONTROL != 0) indentSelectedLines(-SPACES_PER_TAB)
                    else indentSelectedLines(SPACES_PER_TAB)
                }
            }
            KEY_BACKSPACE -> {
                if (!canDoDelete()) handled = false
                else { if (hasSelection()) deleteSelection(false) else removeCharOrTab() }
            }
            KEY_DELETE -> {
                if (!canDoDelete()) handled = false
                else { if (hasSelection()) deleteSelection(false) else doDelete() }
            }
            else -> handled = false
        }
        return handled
    }

    protected fun handleSelectionKey(key: Int, mask: Int): Boolean {
        if (mask and MASK_SHIFT == 0) return false
        when (key) {
            KEY_LEFT  -> { startSelection(); setCursorPos(if (mask and MASK_CONTROL != 0) prevWordPos(cursorPos) else max(0, cursorPos - 1)); selectionEnd = cursorPos }
            KEY_RIGHT -> { startSelection(); setCursorPos(if (mask and MASK_CONTROL != 0) nextWordPos(cursorPos) else min(text.length, cursorPos + 1)); selectionEnd = cursorPos }
            KEY_UP    -> { startSelection(); changeLine(-1); selectionEnd = cursorPos }
            KEY_DOWN  -> { startSelection(); changeLine(1); selectionEnd = cursorPos }
            KEY_HOME  -> { startSelection(); if (mask and MASK_CONTROL != 0) setCursorPos(0) else startOfLine(); selectionEnd = cursorPos }
            KEY_END   -> { startSelection(); if (mask and MASK_CONTROL != 0) setCursorPos(text.length) else endOfLine(); selectionEnd = cursorPos }
            KEY_PAGE_UP   -> { startSelection(); changePage(-1); selectionEnd = cursorPos }
            KEY_PAGE_DOWN -> { startSelection(); changePage(1); selectionEnd = cursorPos }
            else -> return false
        }
        return true
    }

    protected fun handleNavigationKey(key: Int, mask: Int): Boolean {
        if (mask != MASK_NONE) return false
        when (key) {
            KEY_UP    -> changeLine(-1)
            KEY_DOWN  -> { changeLine(1); deselect() }
            KEY_HOME  -> startOfLine()
            KEY_END   -> endOfLine()
            KEY_PAGE_UP   -> changePage(-1)
            KEY_PAGE_DOWN -> changePage(1)
            KEY_LEFT  -> {
                if (hasSelection()) setCursorPos(min(selectionStart, selectionEnd))
                else if (cursorPos > 0) setCursorPos(cursorPos - 1)
                else System.err.println("TextEditor: reportBadKeystroke not yet implemented")
            }
            KEY_RIGHT -> {
                if (hasSelection()) setCursorPos(max(selectionStart, selectionEnd))
                else if (cursorPos < text.length) setCursorPos(cursorPos + 1)
                else System.err.println("TextEditor: reportBadKeystroke not yet implemented")
            }
            else -> return false
        }
        deselect()
        return true
    }

    protected fun handleControlKey(key: Int, mask: Int): Boolean {
        if (mask and MASK_CONTROL == 0) return false
        when (key) {
            KEY_HOME -> {
                if (mask and MASK_SHIFT != 0) { startSelection(); setCursorPos(0); selectionEnd = cursorPos }
                else { deselect(); startOfDoc() }
            }
            KEY_END -> {
                if (mask and MASK_SHIFT != 0) { startSelection(); setCursorPos(text.length); selectionEnd = cursorPos }
                else { deselect(); endOfDoc() }
            }
            else -> return false
        }
        return true
    }

    open fun onMouseCaptureLost() { isSelecting = false }

    open fun draw() {
        // no-op
    }

    open fun onFocusReceived() {
        System.err.println("TextEditor: onFocusReceived not yet implemented")
    }

    open fun onFocusLost() {
        if (commitOnFocusLost) onCommit()
        System.err.println("TextEditor: onFocusLost emoji/mention hide not yet implemented")
    }

    open fun onCommit() {
        System.err.println("TextEditor: onCommit not yet implemented")
    }

    open fun setEnabled(enabled: Boolean) { this.enabled = enabled }

    open fun clear() {
        setText("")
    }

    open fun setFocus(b: Boolean) {
        System.err.println("TextEditor: setFocus not yet implemented")
    }

    open fun isDirty(): Boolean = !baseDocIsPristine || pristineCmd != lastCmd

    open fun undo() {
        if (!canUndo()) return
        deselect()
        val cmd = undoStack.firstOrNull { it == lastCmd } ?: return
        val newPos = cmd.undo(this)
        setCursorPos(newPos)

        val idx = undoStack.indexOf(cmd)
        lastCmd = if (idx + 1 < undoStack.size) undoStack[idx + 1] else null
    }

    open fun canUndo(): Boolean = lastCmd != null

    open fun redo() {
        if (!canRedo()) return
        deselect()
        val idx = undoStack.indexOf(lastCmd)
        val target = if (idx > 0) undoStack[idx - 1] else undoStack.firstOrNull() ?: return
        val newPos = target.redo(this)
        setCursorPos(newPos)
        lastCmd = target
    }

    open fun canRedo(): Boolean = undoStack.isNotEmpty() && lastCmd != undoStack.first()

    open fun cut() {
        if (!canCut()) return
        val (leftPos, length) = selectionRange()
        copyToClipboard(text.substring(leftPos, leftPos + length))
        deleteSelection(false)
        onKeyStroke()
    }

    open fun copy() {
        if (!canCopy()) return
        val (leftPos, length) = selectionRange()
        copyToClipboard(text.substring(leftPos, leftPos + length))
    }

    open fun paste() = pasteHelper(false)
    open fun pastePrimary() = pasteHelper(true)

    open fun updatePrimary() { if (canCopy()) copyPrimary() }

    open fun copyPrimary() {
        if (!canCopy()) return
        val (leftPos, length) = selectionRange()
        copyToPrimaryClipboard(text.substring(leftPos, leftPos + length))
    }

    open fun doDelete() {
        if (!canDoDelete()) return
        if (hasSelection()) deleteSelection(false)
        else removeChar(cursorPos)
        onKeyStroke()
    }

    open fun selectAll() {
        selectionStart = text.length
        selectionEnd = 0
        setCursorPos(selectionEnd)
    }

    open fun deselect() {
        selectionStart = 0
        selectionEnd = 0
        isSelecting = false
        selectedOnFocusReceived = false
    }

    fun selectByCursorPosition(prevCursorPos: Int, nextCursorPos: Int) {
        setCursorPos(prevCursorPos)
        startSelection()
        setCursorPos(nextCursorPos)
        endSelection()
    }

    fun setSelectAllOnFocusReceived(b: Boolean) { selectAllOnFocusReceived = b }

    open fun canLoadOrSaveToFile(): Boolean = false

    fun insertText(str: String) {
        if (hasSelection()) deleteSelection(true)
        val delta = insert(cursorPos, str, false)
        setCursorPos(cursorPos + delta)
        onKeyStroke()
    }

    fun insertLinefeed() = addLineBreakChar()

    fun removeTextFromEnd(numChars: Int) {
        if (numChars <= 0) return
        val start = max(0, text.length - numChars)
        rawRemove(start, text.length - start)
    }

    fun blockUndo() {
        lastCmd?.blockExtensions()
    }

    open fun makePristine() {
        pristineCmd = lastCmd
        baseDocIsPristine = true
    }

    fun isPristine(): Boolean = pristineCmd == lastCmd

    fun tryToRevertToPristineState(): Boolean {
        if (isPristine()) return true
        while (canUndo() && !isPristine()) undo()
        if (!isPristine()) while (canRedo() && !isPristine()) redo()
        return isPristine()
    }

    fun setCursorAndScrollToEnd() {
        setCursorPos(text.length)
        System.err.println("TextEditor: setCursorAndScrollToEnd scroll not yet implemented")
    }

    fun getCurrentLineAndColumn(includeWordwrap: Boolean): Pair<Int, Int> {
        return Pair(0, 0)
    }

    fun setCommitOnFocusLost(b: Boolean) {
        System.err.println("TextEditor: setCommitOnFocusLost not yet implemented")
    }

    open fun importBuffer(buffer: String): Boolean {
        setText(buffer)
        return true
    }

    open fun exportBuffer(): String = getText()

    fun getSourceId(): Any = ""

    fun setPassDelete(b: Boolean) { passDelete = b }

    fun showEmojiHelper() {
        if (readOnly || !showEmojiHelper) return
        System.err.println("TextEditor: showEmojiHelper not yet implemented")
    }

    fun hideEmojiHelper() {
        if (showEmojiHelper) System.err.println("TextEditor: hideEmojiHelper not yet implemented")
    }

    fun setShowEmojiHelper(show: Boolean) {
        if (!show) System.err.println("TextEditor: setShowEmojiHelper hide not yet implemented")
        this.showEmojiHelper = show
    }

    fun setShowChatMentionPicker(show: Boolean) { showChatMentionPicker = show }
    fun getShowChatMentionPicker(): Boolean = showChatMentionPicker
    fun getShowEmojiHelper(): Boolean = showEmojiHelper
    fun getShowContextMenu(): Boolean = showContextMenu

    fun getConvertedText(): String {
        return ""
    }

    open fun onSpellCheckPerformed() {
        System.err.println("TextEditor: onSpellCheckPerformed not yet implemented")
    }

    protected open fun showContextMenu(x: Int, y: Int, setCursorPosition: Boolean = true) {
        System.err.println("TextEditor: showContextMenu not yet implemented")
    }

    protected fun drawPreeditMarker() {
        // no-op
    }

    protected fun removeCharOrTab() {
        if (!enabled) return
        if (cursorPos > 0) {
            var charsToRemove = 1
            if (enableTabRemove && text[cursorPos - 1] == ' ') {
                val offset = getLineOffsetFromDocIndex(cursorPos)
                if (offset > 0) {
                    charsToRemove = offset % SPACES_PER_TAB
                    if (charsToRemove == 0) charsToRemove = SPACES_PER_TAB
                    for (i in 0 until charsToRemove) {
                        if (text[cursorPos - i - 1] != ' ') { charsToRemove = 1; break }
                    }
                }
            }
            repeat(charsToRemove) {
                setCursorPos(cursorPos - 1)
                remove(cursorPos, 1, false)
            }
            tryToShowEmojiHelper()
            tryToShowMentionHelper()
        } else System.err.println("TextEditor: reportBadKeystroke not yet implemented")
    }

    protected fun indentSelectedLines(spaces: Int) {
        if (!hasSelection()) return
        val left = min(selectionStart, selectionEnd)
        val right = max(selectionStart, selectionEnd)
        val cursorOnRight = selectionEnd > selectionStart

        var lineStart = left
        while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--

        var lineEnd = right
        while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++

        parseOnTheFly = false
        var cur = lineStart
        var adjustedRight = lineEnd

        while (cur <= adjustedRight) {
            if (cur > 0 && text[cur - 1] == '\n') {}
            val delta = indentLine(cur, spaces)
            adjustedRight += delta
            while (cur < adjustedRight && text[cur] != '\n') cur++
            cur++
        }
        parseOnTheFly = true

        if (cursorOnRight) { selectionStart = lineStart; selectionEnd = adjustedRight }
        else { selectionStart = adjustedRight; selectionEnd = lineStart }
        setCursorPos(selectionEnd)
    }

    protected fun indentLine(pos: Int, spaces: Int): Int {
        var delta = 0
        if (spaces >= 0) {
            repeat(spaces) { delta += addChar(pos + delta, ' '.code) }
        } else {
            repeat(-spaces) {
                if (pos < text.length && text[pos] == ' ') delta -= rawRemove(pos, 1)
            }
        }
        return delta
    }

    protected fun unindentLineBeforeCloseBrace() {
        System.err.println("TextEditor: unindentLineBeforeCloseBrace not yet implemented")
    }

    protected fun selectionContainsLineBreaks(): Boolean {
        if (!hasSelection()) return false
        val left = min(selectionStart, selectionEnd)
        val right = max(selectionStart, selectionEnd)
        return text.substring(left, right).contains('\n')
    }

    protected fun deleteSelection(groupWithNextOp: Boolean) {
        if (!enabled || !hasSelection()) return
        val pos = min(selectionStart, selectionEnd)
        val length = abs(selectionStart - selectionEnd)
        remove(pos, length, groupWithNextOp)
        deselect()
        setCursorPos(pos)
    }

    protected fun execute(cmd: TextCmd): Int {
        val delta = intArrayOf(0)
        if (cmd.execute(this, delta)) {
            val endIter = undoStack.indexOf(lastCmd)
            if (endIter > 0) {
                repeat(endIter) { undoStack.removeFirst() }
            }
            undoStack.addFirst(cmd)
            lastCmd = cmd
            if (delta[0] == 0) {
                undo()
                undoStack.removeFirst()
            }
        }
        return delta[0]
    }

    fun insert(pos: Int, str: String, groupWithNextOp: Boolean): Int {
        return execute(TextCmdInsert(pos, groupWithNextOp, str))
    }

    fun remove(pos: Int, length: Int, groupWithNextOp: Boolean): Int {
        return execute(TextCmdRemove(pos, groupWithNextOp, length))
    }

    fun overwriteChar(pos: Int, wc: Int): Int {
        return if (pos == text.length) addChar(pos, wc)
               else execute(TextCmdOverwriteChar(pos, false, wc))
    }

    fun removeChar(pos: Int): Int = remove(pos, 1, false)

    fun removeChar() {
        if (!enabled) return
        if (cursorPos > 0) {
            setCursorPos(cursorPos - 1)
            removeChar(cursorPos)
            tryToShowEmojiHelper()
            tryToShowMentionHelper()
        } else System.err.println("TextEditor: reportBadKeystroke not yet implemented")
    }

    fun removeWord(prev: Boolean) {
        val pos = cursorPos
        val limit = if (prev) pos > 0 else pos < text.length
        if (limit) {
            var newPos = if (prev) prevWordPos(pos) else nextWordPos(pos)
            if (newPos == pos) newPos = if (prev) prevWordPos(newPos - 1) else nextWordPos(newPos + 1)
            val diff = abs(pos - newPos)
            if (prev) { remove(newPos, diff, false); setCursorPos(newPos) }
            else remove(pos, diff, false)
        } else System.err.println("TextEditor: reportBadKeystroke not yet implemented")
    }

    fun addChar(pos: Int, wc: Int): Int {
        if (lastCmd?.canExtend(pos) == true) {
            val delta = intArrayOf(0)
            lastCmd!!.extendAndExecute(this, pos, wc, delta)
            return delta[0]
        }
        return execute(TextCmdAddChar(pos, false, wc))
    }

    fun addChar(wc: Int) {
        if (!enabled) return
        if (hasSelection()) deleteSelection(true)
        setCursorPos(cursorPos + addChar(cursorPos, wc))
        tryToShowEmojiHelper()
        tryToShowMentionHelper()
    }

    fun addLineBreakChar(groupTogether: Boolean = false) {
        if (!enabled) return
        if (hasSelection()) deleteSelection(true)
        val pos = execute(TextCmdAddChar(cursorPos, groupTogether, '\n'.code))
        setCursorPos(cursorPos + pos)
    }

    private fun pasteHelper(isPrimary: Boolean) {
        val canDo = if (isPrimary) canPastePrimary() else canPaste()
        if (!canDo) return

        val paste: String = if (isPrimary) pasteFromPrimaryClipboard() else pasteFromClipboard()
        if (paste.isEmpty()) return

        if (!isPrimary && hasSelection()) deleteSelection(true)

        val cleanString = cleanStringForPaste(paste)

        parseOnTheFly = false
        pasteTextWithLinebreaks(cleanString)
        parseOnTheFly = true

        deselect()
        onKeyStroke()
    }

    private fun cleanStringForPaste(raw: String): String {
        var s = raw.replace('\r', '\n')
        s = s.replace("\t", " ".repeat(SPACES_PER_TAB))
        return s
    }

    private fun pasteTextWithLinebreaks(str: String) {
        var start = 0
        var pos = str.indexOf('\n', start)

        while (pos != -1 && pos != str.length - 1) {
            if (pos != start) {
                val fragment = str.substring(start, pos)
                setCursorPos(cursorPos + insert(cursorPos, fragment, true))
            }
            addLineBreakChar(true)
            start = pos + 1
            pos = str.indexOf('\n', start)
        }

        when {
            pos != start && pos == str.length - 1 -> {
                val fragment = str.substring(start, str.length - 1)
                setCursorPos(cursorPos + insert(cursorPos, fragment, true))
                addLineBreakChar(false)
            }
            pos != start -> {
                val fragment = str.substring(start)
                setCursorPos(cursorPos + insert(cursorPos, fragment, false))
            }
            else -> addLineBreakChar(false)
        }
    }

    private fun selectionRange(): Pair<Int, Int> {
        val left = min(selectionStart, selectionEnd)
        val length = abs(selectionStart - selectionEnd)
        return Pair(left, length)
    }

    internal fun rawInsert(pos: Int, str: String): Int {
        val clamped = pos.coerceIn(0, text.length)
        text.insert(clamped, str)
        return str.length
    }

    internal fun rawRemove(pos: Int, len: Int): Int {
        val end = (pos + len).coerceAtMost(text.length)
        val actual = end - pos
        if (actual > 0) text.delete(pos, end)
        return actual
    }

    internal fun rawOverwrite(pos: Int, wc: Int) {
        if (pos < text.length) {
            text.deleteCharAt(pos)
            text.insert(pos, String(intArrayOf(wc), 0, 1))
        }
    }

    private fun onKeyStroke() {
        keystrokeListeners.forEach { it(this) }
    }

    protected fun startSelection() {
        if (!isSelecting) {
            selectionStart = cursorPos
            selectionEnd = cursorPos
            isSelecting = true
        }
    }

    protected fun endSelection() {
        isSelecting = false
    }

    protected fun resetDirty() {
        baseDocIsPristine = true
        pristineCmd = lastCmd
    }

    protected fun setCursorPos(pos: Int): Boolean {
        val newPos = pos.coerceIn(0, text.length)
        cursorPos = newPos
        return cursorPos == pos
    }

    protected fun resetCursorBlink() {
        // no-op
    }

    protected fun setCursorAtLocalPos(x: Int, y: Int, round: Boolean) {
        // no-op
    }

    protected open fun pasteEmbeddedItem(extChar: Int): Int = extChar

    protected fun tryToShowEmojiHelper() {
        if (readOnly || !showEmojiHelper) return
        System.err.println("TextEditor: tryToShowEmojiHelper not yet implemented")
    }

    protected fun tryToShowMentionHelper() {
        if (readOnly || !showChatMentionPicker) return
        System.err.println("TextEditor: tryToShowMentionHelper not yet implemented")
    }

    protected fun updateAllowingLanguageInput() {
        System.err.println("TextEditor: updateAllowingLanguageInput not yet implemented")
    }

    protected fun updateLinkSegments() {
        System.err.println("TextEditor: updateLinkSegments not yet implemented")
    }

    protected fun changeLine(delta: Int) {
        // no-op
    }

    protected fun changePage(delta: Int) {
        // no-op
    }

    protected fun startOfLine() {
        // no-op
    }

    protected fun endOfLine() {
        // no-op
    }

    protected fun startOfDoc() { setCursorPos(0) }
    protected fun endOfDoc()   { setCursorPos(text.length) }

    protected fun updateScrollFromCursor() {
        // no-op
    }

    protected fun getLineOffsetFromDocIndex(docIndex: Int): Int {
        return 0
    }

    protected fun copyToClipboard(str: String) {
        System.err.println("TextEditor: copyToClipboard not yet implemented")
    }

    protected fun copyToPrimaryClipboard(str: String) {
        System.err.println("TextEditor: copyToPrimaryClipboard not yet implemented")
    }

    protected fun pasteFromClipboard(): String {
        return ""
    }

    protected fun pasteFromPrimaryClipboard(): String {
        return ""
    }

    protected fun hasPreeditString(): Boolean {
        return false
    }

    protected fun resetPreedit() {
        // no-op
    }

    protected fun updatePreedit(preeditString: String, segmentLengths: List<Int>, standouts: List<Boolean>, caretPosition: Int) {
        // no-op
    }

    protected fun markAsPreedit(position: Int, length: Int) {
        // no-op
    }

    protected fun getPreeditRange(): Pair<Int, Int> {
        return Pair(0, 0)
    }

    protected fun getSelectionRange(): Pair<Int, Int> {
        val left = min(selectionStart, selectionEnd)
        return Pair(left, abs(selectionStart - selectionEnd))
    }

    protected fun getPreeditLocation(queryOffset: Int): Any {
        return ""
    }

    protected fun getPreeditFontSize(): Int {
        return 0
    }

    protected open fun useFontBuffers(): Boolean = readOnly
}
