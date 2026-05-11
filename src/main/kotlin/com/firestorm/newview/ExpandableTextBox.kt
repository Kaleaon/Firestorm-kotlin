package com.firestorm.newview

import java.util.UUID

open class ExpandableTextBox(
    maxHeight: Int = 0,
    bgVisible: Boolean = false,
    expandedBgVisible: Boolean = true,
    bgColor: FloatArray = floatArrayOf(0f, 0f, 0f, 1f),
    expandedBgColor: FloatArray = floatArrayOf(0f, 0f, 0f, 1f)
) : UICtrl() {

    inner class TextBoxEx(
        labelOverride: String? = null,
        maxTextLength: Int = Int.MAX_VALUE
    ) : TextEditor(maxTextLength = maxTextLength) {

        private val expanderLabel: String = labelOverride ?: "More"
        private var expanderVisible: Boolean = false

        open fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
            super.reshape(width, height, calledFromParent)
        }

        open fun setText(text: String, inputParams: StyleParams = StyleParams()) {
            // Obliterates the expander segment, so clear flag to regenerate it.
            expanderVisible = false
            super.setText(text, inputParams)
            hideOrShowExpandTextAsNeeded()
        }

        fun setTextBase(text: String) {
            super.setTextDirect(text)
        }

        open fun getVerticalTextDelta(): Int {
            return getTextPixelHeight() - getRect().height
        }

        fun getTextPixelHeight(): Int {
            return getTextBoundingRect().height
        }

        fun showExpandText() {
            if (!expanderVisible) {
                mScroller?.goToTop()
                val visibleLines = getVisibleLines(fullyVisible = true)
                val lastLine = visibleLines.second - 1
                insertExpanderSegment(lastLine, expanderLabel)
                expanderVisible = true
            }
        }

        fun hideExpandText() {
            if (expanderVisible) {
                restoreNormalSegment()
                expanderVisible = false
            }
        }

        fun hideOrShowExpandTextAsNeeded() {
            hideExpandText()
            if (getTextPixelHeight() > getRect().height) {
                showExpandText()
            }
        }

        private fun insertExpanderSegment(lastLine: Int, label: String) {
            TODO("GPU: insert styled expander text segment at line $lastLine with label '$label'")
        }

        private fun restoreNormalSegment() {
            TODO("GPU: restore full-text normal segment, clearing expander and re-applying styles")
        }
    }

    protected var mText: String = ""
    protected val mTextBox: TextBoxEx = TextBoxEx()
    protected var mScroll: ScrollContainer? = ScrollContainer()

    protected var mMaxHeight: Int = maxHeight
    protected var mCollapsedRect: Rect = Rect()
    protected var mExpanded: Boolean = false
    protected var mParentRect: Rect = Rect()

    protected var mBGVisible: Boolean = bgVisible
    protected var mExpandedBGVisible: Boolean = expandedBgVisible
    protected var mBGColor: FloatArray = bgColor
    protected var mExpandedBGColor: FloatArray = expandedBgColor

    init {
        mTextBox.setCommitCallback { onExpandClicked() }
        updateTextBoxRect()
    }

    open fun setText(str: String) {
        collapseTextBox()
        mText = str
        mTextBox.setText(str)
    }

    open fun getText(): String = mText

    open fun setValue(value: Any) {
        collapseTextBox()
        mText = value.toString()
        mTextBox.setValue(value)
    }

    open fun getValue(): Any = mText

    override fun onFocusLost() {
        collapseTextBox()
        super.onFocusLost()
    }

    override fun onTopLost() {
        collapseTextBox()
        super.onTopLost()
    }

    fun updateTextShape() {
        check(!mExpanded) { "updateTextShape must only be called when collapsed" }
        updateTextBoxRect()
    }

    open override fun reshape(width: Int, height: Int, calledFromParent: Boolean) {
        mExpanded = false
        super.reshape(width, height, calledFromParent)
        updateTextBoxRect()
    }

    override fun draw() {
        if (mBGVisible && !mExpanded) {
            TODO("GPU: gl_rect_2d with mBGColor")
        }
        if (mExpandedBGVisible && mExpanded) {
            TODO("GPU: gl_rect_2d with mExpandedBGColor")
        }
        collapseIfPosChanged()
        super.draw()
    }

    fun setContentTrusted(trustedContent: Boolean) {
        mTextBox.setContentTrusted(trustedContent)
    }

    protected open fun expandTextBox() {
        mTextBox.hideExpandText()
        mTextBox.setTextBase(mText)

        var textDelta = mTextBox.getVerticalTextDelta()
        textDelta += mTextBox.getVPad() * 2
        textDelta += (mScroll?.getBorderWidth() ?: 0) * 2
        if (textDelta <= 0) return

        saveCollapsedState()

        val expandedRect = getLocalRect()
        val updatedTextDelta = recalculateTextDelta(textDelta)
        expandedRect.bottom -= updatedTextDelta

        val textBoxRect = mTextBox.getRect().copy()

        if (textDelta != updatedTextDelta) {
            val scrollbarSize = UIConstants.scrollbarSize
            textBoxRect.right -= scrollbarSize
        }

        textBoxRect.bottom -= textDelta
        mTextBox.reshape(textBoxRect.width, textBoxRect.height)
        mTextBox.setRect(textBoxRect)

        val expandedScreenRect = localRectToParent(expandedRect)
        reshape(expandedScreenRect.width, expandedScreenRect.height, false)
        setRect(expandedScreenRect)

        setFocus(true)
        ViewerWindow.instance.addPopup(this)

        mExpanded = true
    }

    protected open fun collapseTextBox() {
        if (!mExpanded) return

        mExpanded = false
        reshape(mCollapsedRect.width, mCollapsedRect.height, false)
        setRect(mCollapsedRect)
        updateTextBoxRect()
        ViewerWindow.instance.removePopup(this)
    }

    protected open fun collapseIfPosChanged() {
        if (mExpanded) {
            val parent = getParent() ?: return
            val parentRect = parent.getRect().toScreenCoords(getRootView())
            if (parentRect.left != mParentRect.left || parentRect.top != mParentRect.top) {
                collapseTextBox()
            }
        }
    }

    protected open fun updateTextBoxRect() {
        val rc = getLocalRect()
        val border = mScroll?.getBorderWidth() ?: 0
        val inner = Rect(
            left = rc.left + border,
            right = rc.right - border,
            top = rc.top - border,
            bottom = rc.bottom + border
        )
        mTextBox.reshape(inner.width, inner.height)
        mTextBox.setRect(inner)
        mTextBox.setText(mText)
    }

    protected open fun onExpandClicked() {
        expandTextBox()
    }

    protected open fun saveCollapsedState() {
        mCollapsedRect = getRect().copy()
        val parent = getParent() ?: return
        mParentRect = parent.getRect().toScreenCoords(getRootView())
    }

    protected open fun recalculateTextDelta(textDelta: Int): Int {
        val expandedRect = getLocalRect()
        val rootView = getRootView()
        val windowRect = rootView.getRect()
        val expandedScreenRect = localRectToOtherView(expandedRect, rootView)

        return when {
            expandedScreenRect.bottom - textDelta < windowRect.bottom ->
                expandedScreenRect.bottom - windowRect.bottom
            mMaxHeight > 0 && expandedRect.height + textDelta > mMaxHeight ->
                mMaxHeight - expandedRect.height
            else -> textDelta
        }
    }
}
