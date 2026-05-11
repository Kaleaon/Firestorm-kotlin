package com.firestorm.newview

import com.firestorm.ui.UICtrl
import com.firestorm.ui.UIColor
import com.firestorm.ui.TextBox
import com.firestorm.ui.ViewBorder
import com.firestorm.ui.Rect
import com.firestorm.ui.UIImage
import com.firestorm.types.Color4
import com.firestorm.floater.FloaterColorPicker
import com.firestorm.focus.FocusMgr

// ────────────────────────────────────────────────────────────────────────────
// ColorSwatchCtrl — clickable color preview widget that opens a color picker
// ────────────────────────────────────────────────────────────────────────────

open class ColorSwatchCtrl(
    color: Color4 = Color4.white,
    canApplyImmediately: Boolean = false,
    alphaBackgroundImage: UIImage? = null,
    borderColor: UIColor = UIColor.default_,
    labelWidth: Int = -1,
    labelHeight: Int = -1,
    label: String = "",
    cancelCallback: ((UICtrl, Any?) -> Unit)? = null,
    selectCallback: ((UICtrl, Any?) -> Unit)? = null,
    previewCallback: ((UICtrl, Any?) -> Unit)? = null,
    textEnabledColor: UIColor = UIColor.default_,
    textDisabledColor: UIColor = UIColor.default_
) : UICtrl() {

    enum class ColorPickOp { COLOR_CHANGE, COLOR_SELECT, COLOR_CANCEL }

    private var valid: Boolean = true
    private var mColor: Color4 = color
    private val borderColor: UIColor = borderColor
    private var canApplyImmediately: Boolean = canApplyImmediately
    private var onCancelCallback: ((UICtrl, Any?) -> Unit)? = cancelCallback
    private var onSelectCallback: ((UICtrl, Any?) -> Unit)? = selectCallback
    var previewCallback: ((UICtrl, Any?) -> Unit)? = previewCallback
    private var labelWidth: Int = labelWidth
    private var labelHeight: Int = labelHeight
    private var alphaGradientImage: UIImage? = alphaBackgroundImage
    private var fallbackImage: UIImage? = null
    private var textEnabledColor: UIColor = textEnabledColor
    private var textDisabledColor: UIColor = textDisabledColor

    private lateinit var caption: TextBox
    private var border: ViewBorder? = null
    private var picker: FloaterColorPicker? = null

    init {
        val resolvedLabelHeight = if (labelHeight != -1) labelHeight else BTN_HEIGHT_SMALL
        this.labelHeight = resolvedLabelHeight

        val captionRect = if (labelWidth != -1)
            Rect(left = 0, top = resolvedLabelHeight, right = labelWidth, bottom = 0)
        else
            Rect(left = 0, top = resolvedLabelHeight, right = rectWidth, bottom = 0)

        caption = TextBox(rect = captionRect, text = label)
        addChild(caption)

        val borderRect = localRect.copy().also {
            it.top -= 1
            it.right -= 1
            it.bottom += resolvedLabelHeight
        }
        border = ViewBorder(rect = borderRect)
        addChild(border!!)

        updateLabelColor()
    }

    // ── Color accessors ───────────────────────────────────────────────────────

    fun get(): Color4 = mColor

    fun getValue(): Any = mColor.getValue()

    fun setValue(value: Any) {
        set(Color4(value), updatePicker = true, fromEvent = true)
    }

    fun set(color: Color4, updatePicker: Boolean = false, fromEvent: Boolean = false) {
        mColor = color
        if (updatePicker) picker?.setCurRgb(mColor.r, mColor.g, mColor.b)
        if (!fromEvent) setControlValue(mColor.getValue())
    }

    fun setOriginal(color: Color4) {
        mColor = color
        picker?.setOrigRgb(mColor.r, mColor.g, mColor.b)
    }

    fun setValid(valid: Boolean) {
        this.valid = valid
        picker?.setActive(valid)
    }

    fun setLabel(label: String) {
        caption.setText(label)
    }

    fun setLabelWidth(lw: Int) { labelWidth = lw }

    fun setCanApplyImmediately(apply: Boolean) { canApplyImmediately = apply }

    fun setOnCancelCallback(cb: (UICtrl, Any?) -> Unit) { onCancelCallback = cb }
    fun setOnSelectCallback(cb: (UICtrl, Any?) -> Unit) { onSelectCallback = cb }
    fun setFallbackImage(image: UIImage) { fallbackImage = image }

    // ── Label color helpers (Firestorm extension) ─────────────────────────────

    fun setLabelColor(c: Color4) { textEnabledColor = UIColor(c); updateLabelColor() }
    fun setDisabledLabelColor(c: Color4) { textDisabledColor = UIColor(c); updateLabelColor() }

    protected fun updateLabelColor() {
        caption.setColor(textEnabledColor.get())
        caption.setReadOnlyColor(textDisabledColor.get())
    }

    // ── Input handling ────────────────────────────────────────────────────────

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        FocusMgr.setMouseCapture(this)
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture()) {
            FocusMgr.setMouseCapture(null)
            if (pointInView(x, y)) {
                setFocus(true)
                showPicker(false)
            }
        }
        return true
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean = handleMouseDown(x, y, mask)

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        TODO("APR: use JVM equivalent — set cursor to HAND")
        return true
    }

    override fun handleUnicodeCharHere(uniChar: Int): Boolean {
        if (uniChar == ' '.code) showPicker(true)
        return super.handleUnicodeCharHere(uniChar)
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun draw() {
        // Do not apply floater alpha to the swatch itself so active color reads correctly (STORM-676)
        val alpha = if (transparencyType == TT_ACTIVE) 1f else currentTransparency

        border?.setKeyboardFocusHighlight(hasFocus())

        val borderRect = Rect(left = 0, top = rect.height, right = rect.width, bottom = labelHeight)
        TODO("GPU: gl_rect_2d(borderRect, borderColor, filled=false)")

        val interior = borderRect.copy().also { it.stretch(-1) }

        if (valid) {
            if (!mColor.isOpaque) {
                TODO("GPU: gl_rect_2d_checkerboard(interior, alpha)")
            }
            TODO("GPU: gl_rect_2d(interior, mColor * alpha, filled=true)")
            if (!mColor.isOpaque) {
                val opaqueColor = mColor.copy(a = alpha)
                TODO("GPU: gGL.color4fv(opaqueColor); mAlphaGradientImage?.draw(interior, mColor * alpha)")
            }
        } else {
            if (fallbackImage != null) {
                TODO("GPU: fallbackImage.draw(interior, Color4.white * alpha)")
            } else {
                TODO("GPU: gl_rect_2d(interior, Color4.grey * alpha, filled=true); gl_draw_x(interior, Color4.black * alpha)")
            }
        }

        caption.setEnabled(isEnabled && isInEnabledChain())
        super.draw()
    }

    override fun setEnabled(enabled: Boolean) {
        caption.setEnabled(enabled && isInEnabledChain())
        super.setEnabled(enabled)
        if (!enabled) {
            picker?.cancelSelection()
            picker?.closeFloater()
            picker = null
        }
    }

    // ── Picker management ─────────────────────────────────────────────────────

    fun showPicker(takeFocus: Boolean) {
        if (picker == null) {
            picker = FloaterColorPicker(this, canApplyImmediately)
            val parent = getParentFloater()
            parent?.addDependentFloater(picker!!)
        }
        picker!!.initUI(mColor.r, mColor.g, mColor.b)
        picker!!.showUI()
        if (takeFocus) picker!!.setFocus(true)
    }

    fun closeFloaterColorPicker() {
        picker?.setSwatch(null)
        picker?.closeFloater()
        picker = null
    }

    // ── Static callback ───────────────────────────────────────────────────────

    companion object {
        private const val BTN_HEIGHT_SMALL = 16
        private const val TT_ACTIVE = 1

        fun onColorChanged(swatch: ColorSwatchCtrl, pickOp: ColorPickOp = ColorPickOp.COLOR_CHANGE) {
            val p = swatch.picker ?: return
            val updated = Color4(r = p.curR, g = p.curG, b = p.curB, a = swatch.mColor.a)
            val colorChanged = swatch.mColor != updated
            if (colorChanged) {
                swatch.mColor = updated
                swatch.setControlValue(updated.getValue())
            }

            when (pickOp) {
                ColorPickOp.COLOR_CANCEL -> swatch.onCancelCallback?.invoke(swatch, null)
                ColorPickOp.COLOR_SELECT -> swatch.onSelectCallback?.invoke(swatch, null)
                else -> swatch.onCommit()
            }

            if (pickOp == ColorPickOp.COLOR_CANCEL || pickOp == ColorPickOp.COLOR_SELECT) {
                swatch.setFocus(true)
            }
        }
    }
}
