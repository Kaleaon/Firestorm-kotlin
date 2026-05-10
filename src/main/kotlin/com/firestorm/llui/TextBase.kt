package com.firestorm.llui

import kotlin.math.max
import kotlin.math.min

enum class IconPositioning { LEFT, RIGHT, NONE }

enum class HAlign { LEFT, CENTER, RIGHT }
enum class VAlign { TOP, CENTER, BOTTOM }

data class Rect(var left: Int = 0, var bottom: Int = 0, var right: Int = 0, var top: Int = 0) {
    val width: Int get() = right - left
    val height: Int get() = top - bottom
    fun translate(dx: Int, dy: Int) = copy(left = left + dx, right = right + dx, bottom = bottom + dy, top = top + dy)
}

data class RectF(var left: Float = 0f, var bottom: Float = 0f, var right: Float = 0f, var top: Float = 0f)

open class TextSegment(var start: Int, var end: Int) {
    var permitsEmoji: Boolean = true

    open fun clone(target: TextBase): TextSegment = TextSegment(start, end)

    open fun getDimensionsF32(firstChar: Int, numChars: Int, width: FloatArray, height: IntArray): Boolean = false

    fun getDimensions(firstChar: Int, numChars: Int, width: IntArray, height: IntArray): Boolean {
        val fw = floatArrayOf(0f)
        val result = getDimensionsF32(firstChar, numChars, fw, height)
        width[0] = fw[0].toInt()
        return result
    }

    open fun getOffset(segmentLocalX: Int, startOffset: Int, numChars: Int, round: Boolean): Int = 0

    open fun getNumChars(numPixels: Int, segmentOffset: Int, lineOffset: Int, maxChars: Int, lineInd: Int): Int = 0

    open fun updateLayout(editor: TextBase) {}

    open fun draw(start: Int, end: Int, selectionStart: Int, selectionEnd: Int, drawRect: RectF): Float {
        TODO("GPU: draw text segment")
    }

    open fun canEdit(): Boolean = true

    open fun unlinkFromDocument(editor: TextBase) {}
    open fun linkToDocument(editor: TextBase) {}

    open fun getColor(): UIColor = UIColor.BLACK
    open fun getStyle(): Style? = null
    open fun setStyle(style: Style) {}
    open fun setToken(token: Any?) {}
    open fun getToken(): Any? = null
    open fun setToolTip(tooltip: String) {}
    open fun dump() {}

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleMiddleMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleMiddleMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleHover(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean = false
    open fun handleScrollHWheel(x: Int, y: Int, clicks: Int): Boolean = false
    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean = false
    open fun getName(): String = ""
    open fun onMouseCaptureLost() {}
    open fun hasMouseCapture(): Boolean = false
}

open class NormalTextSegment(
    var style: Style,
    start: Int,
    end: Int,
    val editor: TextBase
) : TextSegment(start, end) {

    protected var fontHeight: Int = 0
    protected var token: Any? = null
    protected var tooltip: String = ""
    protected var canEdit: Boolean = true
    protected var lastGeneration: Int = -1

    constructor(color: UIColor, start: Int, end: Int, editor: TextBase, isVisible: Boolean = true)
        : this(Style(Style.Params(color = color, visible = isVisible)), start, end, editor)

    override fun clone(target: TextBase): NormalTextSegment = NormalTextSegment(style, start, end, target)

    override fun getDimensionsF32(firstChar: Int, numChars: Int, width: FloatArray, height: IntArray): Boolean {
        TODO("GPU: measure text segment dimensions")
    }

    override fun getOffset(segmentLocalX: Int, startOffset: Int, numChars: Int, round: Boolean): Int {
        TODO("GPU: calculate character offset from x coordinate")
    }

    override fun getNumChars(numPixels: Int, segmentOffset: Int, lineOffset: Int, maxChars: Int, lineInd: Int): Int {
        TODO("GPU: calculate number of characters fitting in pixel width")
    }

    override fun updateLayout(editor: TextBase) {
        TODO("GPU: update segment layout")
    }

    override fun draw(start: Int, end: Int, selectionStart: Int, selectionEnd: Int, drawRect: RectF): Float {
        TODO("GPU: draw normal text segment")
    }

    override fun canEdit(): Boolean = canEdit
    override fun getColor(): UIColor = style.getColor()
    override fun getStyle(): Style = style
    override fun setStyle(s: Style) { style = s }
    override fun setToken(token: Any?) { this.token = token }
    override fun getToken(): Any? = token
    override fun setToolTip(t: String) { tooltip = t }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean = false
    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    override fun handleToolTip(x: Int, y: Int, mask: Int): Boolean = false

    protected open fun getWText(): String = editor.getWText()
    protected open fun getLength(): Int = editor.getLength()

    fun setAllowEdit(v: Boolean) { canEdit = v }

    protected fun drawClippedSegment(segStart: Int, segEnd: Int, selStart: Int, selEnd: Int, rect: RectF): Float {
        TODO("GPU: draw clipped text segment with selection")
    }
}

class LabelTextSegment(style: Style, start: Int, end: Int, editor: TextBase)
    : NormalTextSegment(style, start, end, editor) {

    constructor(color: UIColor, start: Int, end: Int, editor: TextBase, isVisible: Boolean = true)
        : this(Style(Style.Params(color = color, visible = isVisible)), start, end, editor)

    override fun clone(target: TextBase): LabelTextSegment = LabelTextSegment(style, start, end, target)
    override fun getWText(): String = editor.getLabel()
    override fun getLength(): Int = editor.getLabel().length
}

class EmojiTextSegment(style: Style, start: Int, end: Int, editor: TextBase)
    : NormalTextSegment(style, start, end, editor) {

    constructor(color: UIColor, start: Int, end: Int, editor: TextBase, isVisible: Boolean = true)
        : this(Style(Style.Params(color = color, visible = isVisible)), start, end, editor)

    override fun clone(target: TextBase): EmojiTextSegment = EmojiTextSegment(style, start, end, target)
    override fun canEdit(): Boolean = false
    override fun handleToolTip(x: Int, y: Int, mask: Int): Boolean = false
}

class OnHoverChangeableTextSegment(
    hoverStyle: Style,
    normalStyle: Style,
    start: Int,
    end: Int,
    editor: TextBase
) : NormalTextSegment(normalStyle, start, end, editor) {

    private val hoveredStyle: Style = hoverStyle
    private val normalStyleSaved: Style = normalStyle
    private var isHovered: Boolean = false

    override fun clone(target: TextBase): OnHoverChangeableTextSegment =
        OnHoverChangeableTextSegment(hoveredStyle, normalStyleSaved, start, end, target)

    override fun draw(start: Int, end: Int, selectionStart: Int, selectionEnd: Int, drawRect: RectF): Float {
        style = if (isHovered) hoveredStyle else normalStyleSaved
        return super.draw(start, end, selectionStart, selectionEnd, drawRect)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        isHovered = true
        return false
    }
}

class IndexSegment : TextSegment(0, 0) {
    override fun clone(target: TextBase): IndexSegment = IndexSegment()
}

class InlineViewSegment(
    private val view: Any,
    private val forceNewLine: Boolean,
    private val leftPad: Int,
    private val rightPad: Int,
    private val topPad: Int,
    private val bottomPad: Int,
    start: Int,
    end: Int
) : TextSegment(start, end) {

    override fun clone(target: TextBase): InlineViewSegment =
        InlineViewSegment(view, forceNewLine, leftPad, rightPad, topPad, bottomPad, start, end)

    override fun getDimensionsF32(firstChar: Int, numChars: Int, width: FloatArray, height: IntArray): Boolean {
        TODO("GPU: measure inline view segment dimensions")
    }

    override fun getNumChars(numPixels: Int, segmentOffset: Int, lineOffset: Int, maxChars: Int, lineInd: Int): Int {
        TODO("GPU: calculate numChars for inline view segment")
    }

    override fun updateLayout(editor: TextBase) {
        TODO("GPU: update inline view layout")
    }

    override fun draw(start: Int, end: Int, selectionStart: Int, selectionEnd: Int, drawRect: RectF): Float {
        TODO("GPU: draw inline view segment")
    }

    override fun canEdit(): Boolean = false
    override fun unlinkFromDocument(editor: TextBase) {}
    override fun linkToDocument(editor: TextBase) {}
}

class LineBreakTextSegment(private val fontHeight: Int, pos: Int) : TextSegment(pos, pos) {

    constructor(style: Style, pos: Int) : this(0, pos)

    override fun clone(target: TextBase): LineBreakTextSegment = LineBreakTextSegment(fontHeight, start)

    override fun getDimensionsF32(firstChar: Int, numChars: Int, width: FloatArray, height: IntArray): Boolean {
        width[0] = 0f
        height[0] = if (numChars > 0) fontHeight else 0
        return false
    }

    override fun getNumChars(numPixels: Int, segmentOffset: Int, lineOffset: Int, maxChars: Int, lineInd: Int): Int = 0

    override fun draw(start: Int, end: Int, selectionStart: Int, selectionEnd: Int, drawRect: RectF): Float = drawRect.left
}

class ImageTextSegment(private val style: Style, pos: Int, private val editor: TextBase) : TextSegment(pos, pos) {
    private var tooltip: String = ""

    override fun clone(target: TextBase): ImageTextSegment = ImageTextSegment(style, start, target)

    override fun getDimensionsF32(firstChar: Int, numChars: Int, width: FloatArray, height: IntArray): Boolean {
        TODO("GPU: measure image segment dimensions")
    }

    override fun getNumChars(numPixels: Int, segmentOffset: Int, charOffset: Int, maxChars: Int, lineInd: Int): Int {
        TODO("GPU: calculate numChars for image segment")
    }

    override fun draw(start: Int, end: Int, selectionStart: Int, selectionEnd: Int, drawRect: RectF): Float {
        TODO("GPU: draw image segment")
    }

    override fun handleToolTip(x: Int, y: Int, mask: Int): Boolean = false
    override fun setToolTip(t: String) { tooltip = t }
}

data class LineInfo(
    val docIndexStart: Int,
    val docIndexEnd: Int,
    val rect: Rect,
    val lineNum: Int
)

abstract class TextBase(params: Params) {

    data class Params(
        var cursorColor: UIColor = UIColor.BLACK,
        var textColor: UIColor = UIColor.BLACK,
        var textReadonlyColor: UIColor = UIColor.GREY,
        var textTentativeColor: UIColor = UIColor.GREY,
        var bgReadonlyColor: UIColor = UIColor(java.awt.Color(0.9f, 0.9f, 0.9f, 1f)),
        var bgWriteableColor: UIColor = UIColor.WHITE,
        var bgFocusColor: UIColor = UIColor.WHITE,
        var bgHighlightedColor: UIColor = UIColor.GREEN,
        var textSelectedColor: UIColor = UIColor.WHITE,
        var bgSelectedColor: UIColor = UIColor(java.awt.Color(0.4f, 0.4f, 0.8f, 1f)),
        var bgVisible: Boolean = false,
        var borderVisible: Boolean = false,
        var trackEnd: Boolean = false,
        var readOnly: Boolean = false,
        var skipLinkUnderline: Boolean = false,
        var spellcheck: Boolean = false,
        var allowScroll: Boolean = true,
        var plainText: Boolean = false,
        var wrap: Boolean = false,
        var useEllipses: Boolean = false,
        var useEmoji: Boolean = true,
        var useColor: Boolean = true,
        var parseUrls: Boolean = false,
        var forceUrlsExternal: Boolean = false,
        var parseHighlights: Boolean = false,
        var clip: Boolean = true,
        var clipPartial: Boolean = true,
        var trustedContent: Boolean = true,
        var alwaysShowIcons: Boolean = false,
        var vPad: Int = 0,
        var hPad: Int = 0,
        var lineSpacingMultiple: Float = 1f,
        var lineSpacingPixels: Int = 0,
        var maxTextLength: Int = 255,
        var fontShadow: ShadowType = ShadowType.NONE,
        var textVAlign: VAlign = VAlign.CENTER,
        var hAlign: HAlign = HAlign.LEFT,
        var vAlign: VAlign = VAlign.TOP,
        var iconPositioning: IconPositioning = IconPositioning.RIGHT,
        var font: Any? = null
    )

    protected val segments: MutableList<TextSegment> = mutableListOf()
    protected val lineInfoList: MutableList<LineInfo> = mutableListOf()
    protected var visibleTextRect: Rect = Rect()
    protected var textBoundingRect: Rect = Rect()

    protected var style: Style.Params = Style.Params()
    protected var styleDirty: Boolean = true
    protected var font: Any? = params.font
    protected val fontShadow: ShadowType = params.fontShadow

    protected var cursorColor: UIColor = params.cursorColor
    protected var fgColor: UIColor = params.textColor
    protected var readOnlyFgColor: UIColor = params.textReadonlyColor
    protected var tentativeFgColor: UIColor = params.textTentativeColor
    protected var writeableBgColor: UIColor = params.bgWriteableColor
    protected var readOnlyBgColor: UIColor = params.bgReadonlyColor
    protected var focusBgColor: UIColor = params.bgFocusColor
    protected var textSelectedColor: UIColor = params.textSelectedColor
    protected var selectedBGColor: UIColor = params.bgSelectedColor
    protected var highlightedBGColor: UIColor = params.bgHighlightedColor

    protected var cursorPos: Int = 0
    protected var desiredXPixel: Int = -1

    protected var selectionStart: Int = 0
    protected var selectionEnd: Int = 0
    protected var isSelecting: Boolean = false

    protected var spellCheck: Boolean = params.spellcheck
    protected var spellCheckStart: Int = -1
    protected var spellCheckEnd: Int = -1
    protected val misspellRanges: MutableList<Pair<UInt, UInt>> = mutableListOf()
    protected val suggestionList: MutableList<String> = mutableListOf()

    protected var highlightWord: String = ""
    protected var highlightCaseInsensitive: Boolean = false
    protected val highlights: MutableList<Pair<Int, Int>> = mutableListOf()
    protected var highlightsDirty: Boolean = false

    protected var hPad: Int = params.hPad
    protected var vPad: Int = params.vPad
    protected var hAlign: HAlign = params.hAlign
    protected var vAlign: VAlign = params.vAlign
    protected var textVAlign: VAlign = params.textVAlign
    protected var lineSpacingMult: Float = params.lineSpacingMultiple
    protected var lineSpacingPixels: Int = params.lineSpacingPixels
    protected var borderVisible: Boolean = params.borderVisible
    protected var parseHTML: Boolean = params.parseUrls
    protected var forceUrlsExternal: Boolean = params.forceUrlsExternal
    protected var parseHighlights: Boolean = params.parseHighlights
    protected var wordWrap: Boolean = params.wrap
    protected var useEllipses: Boolean = params.useEllipses
    protected var useEmoji: Boolean = params.useEmoji
    protected var useColor: Boolean = params.useColor
    protected var trackEnd: Boolean = params.trackEnd
    protected var readOnly: Boolean = params.readOnly
    protected var bgVisible: Boolean = params.bgVisible
    protected var clip: Boolean = params.clip
    protected var clipPartial: Boolean = params.clipPartial && !params.allowScroll
    protected var trustedContent: Boolean = params.trustedContent
    protected var plainText: Boolean = params.plainText
    protected var autoIndent: Boolean = false
    protected var maxTextByteLength: Int = params.maxTextLength
    protected var skipTripleClick: Boolean = false
    protected var alwaysShowIcons: Boolean = params.alwaysShowIcons
    protected var skipLinkUnderline: Boolean = params.skipLinkUnderline

    protected var scroller: Any? = null
    protected var documentView: Any? = null

    protected var reflowIndex: Int = Int.MAX_VALUE
    protected var scrollNeeded: Boolean = false
    protected var scrollIndex: Int = -1

    val urlClickListeners: MutableList<(String) -> Unit> = mutableListOf()
    val isFriendListeners: MutableList<(String) -> Boolean> = mutableListOf()
    val isObjectBlockedListeners: MutableList<(String, String) -> Boolean> = mutableListOf()
    val isObjectReachableListeners: MutableList<(String) -> Boolean> = mutableListOf()

    protected var label: String = ""
    protected var iconPositioning: IconPositioning = params.iconPositioning

    private val viewModel = TextViewModel()
    private var wtext: String = ""

    inner class TextViewModel {
        private var value: String = ""
        fun getValue(): String = value
        fun setValue(v: String) { value = v }
    }

    abstract class TextCmd(val pos: Int, val groupWithNext: Boolean) {
        protected val cmdSegments: MutableList<TextSegment> = mutableListOf()

        abstract fun execute(editor: TextBase, delta: IntArray): Boolean
        abstract fun undo(editor: TextBase): Int
        abstract fun redo(editor: TextBase): Int
        open fun canExtend(pos: Int): Boolean = false
        open fun blockExtensions() {}
        open fun extendAndExecute(editor: TextBase, pos: Int, c: Char, delta: IntArray): Boolean = false
        open fun hasExtCharValue(value: Char): Boolean = false

        fun insert(editor: TextBase, pos: Int, wstr: String): Int =
            editor.insertStringNoUndo(pos, wstr, cmdSegments)

        fun remove(editor: TextBase, pos: Int, length: Int): Int =
            editor.removeStringNoUndo(pos, length)

        fun overwrite(editor: TextBase, pos: Int, wc: Char): Int =
            editor.overwriteCharNoUndo(pos, wc)
    }

    open fun acceptsTextInput(): Boolean = !readOnly
    open fun setColor(c: UIColor) { fgColor = c }
    open fun setReadOnlyColor(c: UIColor) { readOnlyFgColor = c }

    open fun setValue(value: String) {
        wtext = value
        viewModel.setValue(value)
    }

    open fun getText(): String = viewModel.getValue()
    fun getWText(): String = wtext
    fun getLength(): Int = wtext.length
    fun getLineCount(): Int = lineInfoList.size

    fun getMaxTextLength(): Int = maxTextByteLength
    fun setMaxTextLength(length: Int) { maxTextByteLength = length }

    open fun setText(utf8str: String, inputParams: Style.Params = Style.Params()) {
        wtext = utf8str
        viewModel.setValue(utf8str)
        needsReflow()
    }

    fun setWText(text: String) {
        wtext = text
        viewModel.setValue(text)
        needsReflow()
    }

    fun getTextGeneration(): Int = reflowIndex

    fun appendText(newText: String, prependNewline: Boolean, inputParams: Style.Params = Style.Params()) {
        val prefix = if (prependNewline && wtext.isNotEmpty()) "\n" else ""
        wtext += prefix + newText
        viewModel.setValue(wtext)
        needsReflow()
    }

    fun setLabel(label: String) { this.label = label }
    open fun setLabelArg(key: String, text: String): Boolean = false
    fun getLabel(): String = label

    fun setLastSegmentToolTip(tooltip: String) {
        segments.lastOrNull()?.setToolTip(tooltip)
    }

    fun resetLabel() {
        if (label.isNotEmpty()) {
            segments.removeAll { it is LabelTextSegment }
            createLabelSegment()
        }
    }

    fun setFont(font: Any?) { this.font = font; styleDirty = true }

    fun needsReflow(index: Int = 0) {
        reflowIndex = min(reflowIndex, index)
    }

    fun removeFirstLine(): Int {
        if (lineInfoList.isEmpty()) return 0
        val firstLine = lineInfoList.removeAt(0)
        val removed = firstLine.docIndexEnd - firstLine.docIndexStart
        if (removed > 0 && removed <= wtext.length) {
            wtext = wtext.substring(removed)
            viewModel.setValue(wtext)
        }
        return removed
    }

    fun getVPad(): Int = vPad
    fun getHPad(): Int = hPad
    fun getLineSpacingMult(): Float = lineSpacingMult
    fun getLineSpacingPixels(): Int = lineSpacingPixels

    fun getDocIndexFromLocalCoord(localX: Int, localY: Int, round: Boolean, hitPastEndOfLine: Boolean = true): Int {
        TODO("GPU: hit test local coordinate against text layout")
    }

    fun getLocalRectFromDocIndex(pos: Int): Rect {
        TODO("GPU: map document index to local rect")
    }

    fun getDocRectFromDocIndex(pos: Int): Rect {
        TODO("GPU: map document index to document rect")
    }

    fun setReadOnly(readOnly: Boolean) { this.readOnly = readOnly }
    fun getReadOnly(): Boolean = readOnly

    fun setSkipLinkUnderline(skip: Boolean) { skipLinkUnderline = skip }
    fun getSkipLinkUnderline(): Boolean = skipLinkUnderline

    fun setParseURLs(parseUrls: Boolean) { parseHTML = parseUrls }
    fun setParseHTML(parseHtml: Boolean) { parseHTML = parseHtml }

    fun setPlainText(value: Boolean) { plainText = value }
    fun getPlainText(): Boolean = plainText

    fun getWordWrap(): Boolean = wordWrap
    fun getUseEllipses(): Boolean = useEllipses
    fun getUseEmoji(): Boolean = useEmoji
    fun setUseEmoji(value: Boolean) { useEmoji = value }
    fun getUseColor(): Boolean = useColor
    fun setUseColor(value: Boolean) { useColor = value }

    fun isContentTrusted(): Boolean = trustedContent
    fun setContentTrusted(trusted: Boolean) { trustedContent = trusted }

    fun truncate(): Boolean {
        val bytes = viewModel.getValue().toByteArray(Charsets.UTF_8)
        if (bytes.size <= maxTextByteLength) return false
        val truncated = bytes.take(maxTextByteLength).toByteArray().toString(Charsets.UTF_8)
        val safe = truncated.substringBeforeLast('�').ifEmpty { truncated }
        wtext = safe
        viewModel.setValue(safe)
        return true
    }

    fun setCursor(row: Int, column: Int): Boolean {
        val lineIdx = min(row, lineInfoList.size - 1)
        if (lineIdx < 0) return false
        val line = lineInfoList[lineIdx]
        cursorPos = min(line.docIndexStart + column, line.docIndexEnd)
        return true
    }

    fun getCursorPos(): Int = cursorPos

    fun setCursorPos(pos: Int, keepOffset: Boolean = false): Boolean {
        cursorPos = pos.coerceIn(0, wtext.length)
        if (!keepOffset) desiredXPixel = -1
        return true
    }

    open fun startOfLine() {
        val line = getLineNumFromDocIndex(cursorPos, true)
        cursorPos = getLineStart(line)
    }

    fun endOfLine() {
        val line = getLineNumFromDocIndex(cursorPos, true)
        cursorPos = getLineEnd(line)
    }

    fun startOfDoc() { cursorPos = 0 }
    fun endOfDoc() { cursorPos = wtext.length }

    fun changePage(delta: Int) {
        TODO("GPU: change page by scrolling visible area")
    }

    fun changeLine(delta: Int) {
        val currentLine = getLineNumFromDocIndex(cursorPos, true)
        val targetLine = (currentLine + delta).coerceIn(0, lineInfoList.size - 1)
        val lineStart = getLineStart(targetLine)
        cursorPos = lineStart
    }

    fun scrolledToStart(): Boolean = lineInfoList.isEmpty() || reflowIndex == 0
    fun scrolledToEnd(): Boolean = !scrollNeeded

    fun clearHighlights() {
        highlights.clear()
        highlightWord = ""
        highlightsDirty = false
    }

    fun refreshHighlights() {
        highlightsDirty = true
    }

    fun setHighlightWord(highlight: String, caseInsensitive: Boolean) {
        highlightWord = highlight
        highlightCaseInsensitive = caseInsensitive
        highlightsDirty = true
    }

    fun getSpellCheck(): Boolean = spellCheck
    fun getSuggestion(index: UInt): String = suggestionList.getOrElse(index.toInt()) { "" }
    fun getSuggestionCount(): UInt = suggestionList.size.toUInt()

    fun replaceWithSuggestion(index: UInt) {
        val suggestion = suggestionList.getOrNull(index.toInt()) ?: return
        val misspelled = getMisspelledWord(cursorPos.toUInt())
        if (misspelled.isNotEmpty()) {
            val misspellStart = findMisspellStart(cursorPos.toUInt())
            if (misspellStart >= 0) {
                wtext = wtext.substring(0, misspellStart) + suggestion + wtext.substring(misspellStart + misspelled.length)
                viewModel.setValue(wtext)
                needsReflow(misspellStart)
            }
        }
    }

    fun getMisspelledWord(pos: UInt): String {
        val p = pos.toInt()
        val range = misspellRanges.firstOrNull { it.first.toInt() <= p && it.second.toInt() >= p }
            ?: return ""
        return wtext.substring(range.first.toInt(), range.second.toInt())
    }

    fun isMisspelledWord(pos: UInt): Boolean =
        misspellRanges.any { it.first <= pos && it.second >= pos }

    fun onSpellCheckSettingsChange() {
        misspellRanges.clear()
        spellCheckStart = -1
        spellCheckEnd = -1
    }

    open fun onSpellCheckPerformed() {}

    private fun findMisspellStart(pos: UInt): Int {
        val p = pos.toInt()
        return misspellRanges.firstOrNull { it.first.toInt() <= p && it.second.toInt() >= p }
            ?.first?.toInt() ?: -1
    }

    fun addToDictionary() {
        val word = getMisspelledWord(cursorPos.toUInt())
        TODO("APR: use JVM equivalent to add word to spell check dictionary")
    }

    fun canAddToDictionary(): Boolean = isMisspelledWord(cursorPos.toUInt())

    fun addToIgnore() {
        val word = getMisspelledWord(cursorPos.toUInt())
        TODO("APR: use JVM equivalent to ignore word in spell checker")
    }

    fun canAddToIgnore(): Boolean = isMisspelledWord(cursorPos.toUInt())

    fun canDeselect(): Boolean = selectionStart != selectionEnd

    fun deselect() {
        selectionStart = 0
        selectionEnd = 0
        isSelecting = false
    }

    open fun onFocusReceived() {}
    open fun onFocusLost() { deselect() }

    fun setWordWrap(wrap: Boolean) {
        wordWrap = wrap
        needsReflow()
    }

    open fun draw() {
        if (bgVisible) drawBackground()
        drawSelectionBackground()
        drawHighlightsBackground(highlights, highlightedBGColor)
        drawHighlightedBackground()
        drawText()
        drawCursor()
    }

    open fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        needsReflow()
    }

    protected open fun drawBackground() {
        TODO("GPU: draw text background rect")
    }

    protected open fun drawSelectionBackground() {
        TODO("GPU: draw selection highlight background")
    }

    protected fun drawHighlightsBackground(highlightList: List<Pair<Int, Int>>, color: UIColor) {
        TODO("GPU: draw highlight word backgrounds")
    }

    protected open fun drawHighlightedBackground() {
        TODO("GPU: draw highlight-bg styled segments")
    }

    protected open fun drawCursor() {
        TODO("GPU: blink and draw insertion cursor")
    }

    protected open fun drawText() {
        TODO("GPU: iterate segments and draw each one")
    }

    protected fun insertStringNoUndo(pos: Int, wstr: String, segs: MutableList<TextSegment>? = null): Int {
        val insertPos = pos.coerceIn(0, wtext.length)
        wtext = wtext.substring(0, insertPos) + wstr + wtext.substring(insertPos)
        viewModel.setValue(wtext)
        needsReflow(insertPos)
        return wstr.length
    }

    protected fun removeStringNoUndo(pos: Int, length: Int): Int {
        val removePos = pos.coerceIn(0, wtext.length)
        val removeEnd = (removePos + length).coerceAtMost(wtext.length)
        val removed = removeEnd - removePos
        wtext = wtext.substring(0, removePos) + wtext.substring(removeEnd)
        viewModel.setValue(wtext)
        needsReflow(removePos)
        return removed
    }

    protected fun overwriteCharNoUndo(pos: Int, wc: Char): Int {
        if (pos < wtext.length) {
            wtext = wtext.substring(0, pos) + wc + wtext.substring(pos + 1)
        } else {
            wtext += wc
        }
        viewModel.setValue(wtext)
        needsReflow(pos)
        return 1
    }

    protected fun appendAndHighlightText(newText: String, highlightPart: Int, stylep: Style.Params,
                                          underlineLink: UnderlineLink = UnderlineLink.UNDERLINE_ALWAYS) {
        appendText(newText, false, stylep)
    }

    protected fun clearSegments() { segments.clear() }

    protected fun createDefaultSegment() {
        segments.clear()
        segments.add(NormalTextSegment(Style(style), 0, wtext.length, this))
    }

    private fun createLabelSegment() {
        segments.add(LabelTextSegment(UIColor.GREY, 0, label.length, this))
    }

    protected open fun updateSegments() {
        createDefaultSegment()
    }

    protected fun insertSegment(segmentToInsert: TextSegment) {
        val idx = segments.indexOfFirst { it.end > segmentToInsert.start }
        if (idx >= 0) segments.add(idx, segmentToInsert) else segments.add(segmentToInsert)
    }

    protected fun getStyleParams(): Style.Params {
        if (styleDirty) {
            style = style.copy(
                color = fgColor,
                readonlyColor = readOnlyFgColor,
                selectedColor = textSelectedColor,
                font = font,
                dropShadow = fontShadow
            )
            styleDirty = false
        }
        return style
    }

    protected fun getLineStart(line: Int): Int =
        lineInfoList.getOrNull(line)?.docIndexStart ?: 0

    protected fun getLineEnd(line: Int): Int =
        lineInfoList.getOrNull(line)?.docIndexEnd ?: wtext.length

    protected fun getLineNumFromDocIndex(docIndex: Int, includeWordWrap: Boolean = true): Int {
        if (lineInfoList.isEmpty()) return 0
        return lineInfoList.indexOfLast { it.docIndexStart <= docIndex }.coerceAtLeast(0)
    }

    protected fun getLineOffsetFromDocIndex(docIndex: Int, includeWordWrap: Boolean = true): Int {
        val lineNum = getLineNumFromDocIndex(docIndex, includeWordWrap)
        return docIndex - getLineStart(lineNum)
    }

    protected fun getFirstVisibleLine(): Int = 0

    protected fun getVisibleLines(fullyVisible: Boolean = false): Pair<Int, Int> =
        Pair(0, lineInfoList.size)

    protected fun getLeftOffset(width: Int): Int = when (hAlign) {
        HAlign.LEFT   -> hPad
        HAlign.CENTER -> max(0, (visibleTextRect.width - width) / 2)
        HAlign.RIGHT  -> max(0, visibleTextRect.width - width)
    }

    protected fun reflow() {
        TODO("GPU: reflow text segments into lines based on wrap settings")
    }

    protected fun updateCursorXPos() {
        TODO("GPU: recalculate cursor x from cursor position")
    }

    protected fun setCursorAtLocalPos(localX: Int, localY: Int, round: Boolean, keepCursorOffset: Boolean = false) {
        cursorPos = getDocIndexFromLocalCoord(localX, localY, round)
        if (!keepCursorOffset) desiredXPixel = -1
    }

    protected fun getEditableIndex(index: Int, increasingDirection: Boolean): Int {
        return index.coerceIn(0, wtext.length)
    }

    protected fun updateScrollFromCursor() { scrollNeeded = true }

    protected fun hasSelection(): Boolean = selectionStart != selectionEnd

    protected fun startSelection() {
        selectionStart = cursorPos
        isSelecting = true
    }

    protected fun endSelection() {
        selectionEnd = cursorPos
        isSelecting = false
    }

    protected fun updateRects() {
        visibleTextRect = Rect(hPad, vPad, visibleTextRect.right - hPad, visibleTextRect.top - vPad)
    }

    protected fun needsScroll() { scrollNeeded = true }

    fun replaceUrl(url: String, label: String, icon: String) {
        val idx = wtext.indexOf(url)
        if (idx < 0) return
        wtext = wtext.substring(0, idx) + label + wtext.substring(idx + url.length)
        viewModel.setValue(wtext)
        needsReflow(idx)
    }

    fun createUrlContextMenu(x: Int, y: Int, url: String) {
        TODO("GPU: show popup context menu for URL")
    }

    open fun copyContents(source: TextBase) {
        wtext = source.wtext
        viewModel.setValue(wtext)
        label = source.label
        needsReflow()
    }

    open fun appendLineBreakSegment(styleParams: Style.Params) {
        val pos = wtext.length
        wtext += "\n"
        viewModel.setValue(wtext)
        segments.add(LineBreakTextSegment(styleParams, pos))
        needsReflow(pos)
    }

    open fun appendImageSegment(styleParams: Style.Params) {
        val pos = wtext.length
        wtext += " "
        viewModel.setValue(wtext)
        segments.add(ImageTextSegment(Style(styleParams), pos, this))
        needsReflow(pos)
    }

    open fun appendWidget(view: Any, text: String, allowUndo: Boolean) {
        val pos = wtext.length
        if (text.isNotEmpty()) {
            wtext += text
            viewModel.setValue(wtext)
        }
        needsReflow(pos)
    }

    fun addUrlClickedCallback(cb: (String) -> Unit) { urlClickListeners.add(cb) }
    fun addIsFriendCallback(cb: (String) -> Boolean) { isFriendListeners.add(cb) }
    fun addIsObjectBlockedCallback(cb: (String, String) -> Boolean) { isObjectBlockedListeners.add(cb) }
    fun addIsObjectReachableCallback(cb: (String) -> Boolean) { isObjectReachableListeners.add(cb) }

    fun getVisibleTextRect(): Rect = visibleTextRect
    fun getTextBoundingRect(): Rect = textBoundingRect
    fun getVisibleDocumentRect(): Rect = visibleTextRect

    fun getVTextRect(): Rect = visibleTextRect

    protected open fun beforeValueChange() {}
    protected open fun onValueChange(start: Int, end: Int) {}
    protected open fun useLabel(): Boolean = wtext.isEmpty() && label.isNotEmpty()

    protected open fun getSearchText(): String = label + getName() + getToolTip()

    open fun getName(): String = ""
    open fun getToolTip(): String = ""

    open fun onVisibilityChange(newVisibility: Boolean) {}

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleMiddleMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleMiddleMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleHover(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean = false
    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean = false

    open fun insertMentionAtCursor(str: String) {
        insertStringNoUndo(cursorPos, str)
        cursorPos += str.length
    }

    fun normalizeUri(uri: String): Int {
        TODO("APR: use JVM equivalent for URI normalization")
    }
}
