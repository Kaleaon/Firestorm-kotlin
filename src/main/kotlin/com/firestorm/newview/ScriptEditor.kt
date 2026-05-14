package com.firestorm.newview

import com.firestorm.llui.TextEditor
import com.firestorm.llui.Keywords
import com.firestorm.llui.KeywordToken
import com.firestorm.llui.Style
import com.firestorm.llui.Color4
import com.firestorm.llui.FontGL
import com.firestorm.llui.Rect
import com.firestorm.llui.UIColor

private const val UI_TEXTEDITOR_LINE_NUMBER_MARGIN: Int = 40

open class ScriptEditor(
    showLineNumbers: Boolean = true,
    useDefaultFontSize: Boolean = false
) : TextEditor() {

    data class Params(
        val showLineNumbers: Boolean = true,
        val defaultFontSize: Boolean = false
    )

    private val mShowLineNumbers: Boolean = showLineNumbers
    private val mUseDefaultFontSize: Boolean = useDefaultFontSize
    protected val mKeywords: Keywords = Keywords()

    init {
        if (mShowLineNumbers) {
            mHPad += UI_TEXTEDITOR_LINE_NUMBER_MARGIN
            updateRects()
        }
    }

    open fun postBuild(): Boolean {
        return super.postBuild()
    }

    override fun draw() {
        val clipRect = Rect(mVisibleTextRect).apply { stretch(1) }
        // no-op
        super.draw()
        drawLineNumbers()
        drawPreeditMarker()
        mBorder.setKeyboardFocusHighlight(hasFocus())
    }

    private fun drawLineNumbers() {
        // no-op
        if (!mShowLineNumbers) return

        val scrolledViewRect = getVisibleDocumentRect()
        val contentRect = getVisibleTextRect()
        val firstLine = getFirstVisibleLine()
        val numLines = getLineCount()
        if (firstLine >= numLines) return

        val cursorLine = mLineInfoList[getLineNumFromDocIndex(mCursorPos)].mLineNum

        val top = getRect().height
        val bottom = 0

        // no-op

        var lastLineNum = -1
        for (curLine in firstLine until numLines) {
            val line = mLineInfoList[curLine]
            if ((line.mRect.top - scrolledViewRect.bottom) < mVisibleTextRect.bottom) break

            val lineBottom = line.mRect.bottom - scrolledViewRect.bottom + mVisibleTextRect.bottom

            if (line.mLineNum != lastLineNum && line.mRect.top <= scrolledViewRect.top) {
                val isCurLine = cursorLine == line.mLineNum
                val style = if (isCurLine) FontGL.BOLD else FontGL.NORMAL
                val fgColor = if (isCurLine) mCursorColor else mReadOnlyFgColor
                // no-op
                lastLineNum = line.mLineNum
            }
        }
    }

    fun initKeywords() {
        mKeywords.initialize(SyntaxIdLSL.instance.getKeywordsXML())
    }

    fun loadKeywords() {
        mKeywords.processTokens()
        val style = Style(Style.Params(font = getFont(), color = mDefaultColor.get()))
        val segmentList = mutableListOf<Segment>()
        mKeywords.findSegments(segmentList, getWText(), this, style)
        mSegments.clear()
        segmentList.forEach { mSegments.add(it) }
    }

    fun loadKeywords(
        filename: String,
        funcs: MutableList<String>,
        tooltips: MutableList<String>,
        color: Color3
    ) {
        if (mKeywords.loadFromLegacyFile(filename)) {
            val count = minOf(funcs.size, tooltips.size)
            for (i in 0 until count) {
                val name = funcs[i].trim()
                mKeywords.addToken(KeywordToken.TT_WORD, name, UIColor(color), tooltips[i])
            }
            val segmentList = mutableListOf<Segment>()
            val style = Style(Style.Params(font = getFont(), color = mDefaultColor.get()))
            mKeywords.findSegments(segmentList, getWText(), this, style)
            mSegments.clear()
            segmentList.forEach { mSegments.add(it) }
        }
    }

    open fun clearSegments() {
        if (mSegments.isNotEmpty()) {
            mSegments.clear()
        }
    }

    fun keywordsBegin() = mKeywords.begin()
    fun keywordsEnd() = mKeywords.end()

    override fun updateSegments() {
        if (mReflowIndex < Int.MAX_VALUE && mKeywords.isLoaded() && mParseOnTheFly) {
            val style = Style(Style.Params(font = getFont(), color = mDefaultColor.get()))
            val segmentList = mutableListOf<Segment>()
            mKeywords.findSegments(segmentList, getWText(), this, style)
            clearSegments()
            segmentList.forEach { insertSegment(it) }
        }
        super.updateSegments()
    }

    override fun drawSelectionBackground() {
        if (!hasSelection() || mLineInfoList.isEmpty()) return

        val selectionRects = getSelectionRects()
        val baseColor = if (mReadOnly) mReadOnlyFgColor else mFgColor
        val alpha = (if (hasFocus()) 0.7f else 0.3f) * getDrawContext().alpha
        val selectionColor = Color4(
            (1f + baseColor.r) * 0.5f,
            (1f + baseColor.g) * 0.5f,
            (1f + baseColor.b) * 0.5f,
            alpha
        )
        val contentDisplayRect = getVisibleDocumentRect()
        for (rect in selectionRects) {
            val translated = rect.translated(
                mVisibleTextRect.left - contentDisplayRect.left,
                mVisibleTextRect.bottom - contentDisplayRect.bottom
            )
            // no-op
        }
    }

    override fun startOfLine() {
        val text = getWText()
        val lineStartPos = mCursorPos - getLineOffsetFromDocIndex(mCursorPos)

        val line = getLineNumFromDocIndex(mCursorPos)
        val numLines = getLineCount()
        val lineEndPos = if (line + 1 >= numLines) getLength() else getLineStart(line + 1) - 1

        var trimmedLineStartPos = lineStartPos
        while (trimmedLineStartPos < lineEndPos && text[trimmedLineStartPos].isWhitespace()) {
            trimmedLineStartPos++
        }

        when {
            mCursorPos != trimmedLineStartPos || mCursorPos == lineStartPos -> setCursorPos(trimmedLineStartPos)
            mCursorPos == trimmedLineStartPos -> setCursorPos(lineStartPos)
        }
    }

    override fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (key == KEY_F1 && mask == MASK_NONE) {
            val parent = getParentByType<ScriptEdCore>()
            if (parent != null) {
                parent.onBtnDynamicHelp()
                return true
            }
        }
        return super.handleKeyHere(key, mask)
    }

    companion object {
        const val KEY_F1 = 0x70
        const val MASK_NONE = 0
    }
}
