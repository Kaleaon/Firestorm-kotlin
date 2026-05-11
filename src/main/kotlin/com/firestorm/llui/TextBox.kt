package com.firestorm.llui

open class TextBox(params: Params = Params()) : TextBase(params) {

    class Params : TextBase.Params()

    private var clickedCallback: (() -> Unit)? = null
    private var text: String = ""
    private var showCursorHand: Boolean = true

    init {
        skipTripleClick = true
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        var handled = super.handleMouseDown(x, y, mask)
        playSoundMouseDown()
        if (!handled && clickedCallback != null) {
            handled = true
        }
        if (handled) {
            captureMouseIfAllowed()
        }
        return handled
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        val handled = super.handleMouseUp(x, y, mask)
        playSoundMouseUp()
        if (hasMouseCapture()) {
            releaseMouse()
            if (clickedCallback != null && !handled) {
                clickedCallback!!()
                return true
            }
        }
        return handled
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        val handled = super.handleHover(x, y, mask)
        if (!handled && clickedCallback != null && showCursorHand) {
            setCursorHand()
            return true
        }
        return handled
    }

    fun setEnabled(enabled: Boolean) {
        val newReadOnly = !enabled
        if (newReadOnly != readOnly) {
            setReadOnly(newReadOnly)
            updateSegments()
        }
    }

    override fun setText(utf8str: String, inputParams: Style.Params) {
        text = utf8str
        super.setText(text, inputParams)
    }

    fun setRightAlign() { hAlign = HAlign.RIGHT }
    fun setHAlign(align: HAlign) { hAlign = align }

    fun setClickedCallback(cb: (Any?) -> Unit, userdata: Any? = null) {
        clickedCallback = { cb(userdata) }
    }

    fun setClickedCallback(cb: () -> Unit) {
        clickedCallback = cb
    }

    fun reshapeToFitText(calledFromParent: Boolean = false) {
        reflow()
        val width = getTextPixelWidth()
        val height = getTextPixelHeight()
        reshape(width + 2 * hPad + 1, height + 2 * vPad, calledFromParent)
    }

    fun getTextPixelWidth(): Int = textBoundingRect.width
    fun getTextPixelHeight(): Int = textBoundingRect.height

    override fun getText(): String = viewModelValue()

    fun getValue(): String = viewModelValue()

    fun setTextArg(key: String, value: String): Boolean {
        text = text.replace("[[$key]]", value)
        setText(text)
        return true
    }

    fun setShowCursorHand(show: Boolean) { showCursorHand = show }

    fun updateCurrencySymbols() {
        setText(text)
    }

    protected fun onUrlLabelUpdated(url: String, label: String) {
        needsReflow()
    }

    override fun getSearchText(): String = super.getSearchText() + text

    private fun viewModelValue(): String = getText()

    private fun playSoundMouseDown() {
        TODO("APR: use JVM equivalent to play UISndClick sound")
    }

    private fun playSoundMouseUp() {
        TODO("APR: use JVM equivalent to play UISndClickRelease sound")
    }

    private fun hasMouseCapture(): Boolean {
        TODO("GPU: query whether this widget has the mouse captured")
    }

    private fun captureMouseIfAllowed() {
        TODO("GPU: capture mouse input for this widget")
    }

    private fun releaseMouse() {
        TODO("GPU: release mouse capture")
    }

    private fun setCursorHand() {
        TODO("GPU: set cursor to hand/pointer shape")
    }

    protected open fun reflow() {
        TODO("GPU: reflow text layout")
    }
}
