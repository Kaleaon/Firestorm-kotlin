package com.firestorm.llui

import com.firestorm.llmath.Rect
import kotlin.math.roundToLong

private fun clampPrecision(value: Float, decimalPrecision: Int): Float {
    var v = value.toDouble()
    repeat(decimalPrecision) { v *= 10.0 }
    v = v.roundToLong().toDouble()
    repeat(decimalPrecision) { v /= 10.0 }
    return v.toFloat()
}

open class SpinCtrl(
    name: String,
    rect: Rect = Rect(),
    var minValue: Float = 0f,
    var maxValue: Float = 1f,
    var increment: Float = 0.1f,
    var precision: Int = 3,
    allowDigitsOnly: Boolean = false,
    textEnabledColor: Any? = null,
    textDisabledColor: Any? = null,
    labelText: String = ""
) : View(name, rect) {

    private var currentValue: Float = minValue
    var initialValue: Float = minValue
        private set

    private var hasBeenSet: Boolean = false

    val labelBox: TextBox? = if (labelText.isNotEmpty()) TextBox("SpinCtrl Label") else null
    val editor: LineEditor = LineEditor("SpinCtrl Editor")
    val upBtn: Button = Button("up_btn")
    val downBtn: Button = Button("down_btn")

    private var textEnabledColor: Any? = textEnabledColor
    private var textDisabledColor: Any? = textDisabledColor

    init {
        labelBox?.let { addChild(it) }
        addChild(upBtn)
        addChild(downBtn)
        addChild(editor)

        upBtn.clickCallback = { onUpBtn() }
        downBtn.clickCallback = { onDownBtn() }
        editor.commitCallback = { onEditorCommit() }
        updateEditor()
    }

    fun get(): Float = currentValue

    fun set(value: Float) {
        setValue(value)
        initialValue = value
    }

    open fun getValue(): Any? = currentValue
    fun getValueF32(): Float = currentValue

    open fun setValue(value: Any?) {
        val v = value?.toString()?.toFloatOrNull() ?: return
        if (currentValue != v || !hasBeenSet) {
            hasBeenSet = true
            currentValue = v.coerceIn(minValue, maxValue)
            if (!editor.hasFocus()) updateEditor()
        }
    }

    fun setValue(v: Float) = setValue(v as Any?)

    open fun forceSetValue(value: Any?) {
        val v = value?.toString()?.toFloatOrNull() ?: return
        if (currentValue != v || !hasBeenSet) {
            hasBeenSet = true
            currentValue = v.coerceIn(minValue, maxValue)
            updateEditor()
            editor.resetScrollPosition()
        }
    }

    fun isMouseHeldDown(): Boolean = downBtn.isHeld() || upBtn.isHeld()

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        editor.enabled = enabled
        updateLabelColor()
    }

    open fun setFocus(b: Boolean) {
        super.setEnabled(b)
        editor.setFocus(b)
    }

    open fun clear() {
        setValue(minValue)
        editor.setText("")
        hasBeenSet = false
    }

    fun isDirty(): Boolean = currentValue != initialValue
    fun resetDirty() { initialValue = currentValue }

    open fun setPrecision(p: Int) {
        require(p in 0..10) { "SpinCtrl precision out of range: $p" }
        precision = p
        updateEditor()
    }

    fun setLabel(label: String) {
        labelBox?.setText(label) ?: run {
            TODO("GPU: warn — no label box present for setLabel on $name")
        }
        updateLabelColor()
    }

    open fun setLabelArg(key: String, text: String): Boolean {
        labelBox?.setTextArg(key, text) ?: run {
            TODO("GPU: warn — no label box present for setLabelArg on $name")
        }
        updateLabelColor()
        return true
    }

    fun setLabelColor(c: Any?) { textEnabledColor = c; updateLabelColor() }
    fun setDisabledLabelColor(c: Any?) { textDisabledColor = c; updateLabelColor() }

    fun setAllowEdit(allowEdit: Boolean) { editor.enabled = allowEdit }

    open fun onTabInto() { editor.onTabInto() }

    open fun setTentative(b: Boolean) {
        editor.setTentative(b)
    }

    open fun onCommit() {
        setTentative(false)
        setControlValue(currentValue)
    }

    fun forceEditorCommit() { onEditorCommit() }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        var c = clicks
        if (c > 0) while (c-- > 0) onDownBtn()
        else while (c++ < 0) onUpBtn()
        return true
    }

    open fun handleKeyHere(key: Int, mask: UInt): Boolean {
        val KEY_ESCAPE = 0x1B; val KEY_UP = 0x81; val KEY_DOWN = 0x82
        if (editor.hasFocus()) {
            return when (key) {
                KEY_ESCAPE -> {
                    updateEditor(); editor.resetScrollPosition(); editor.setFocus(false); true
                }
                KEY_UP -> { onUpBtn(); true }
                KEY_DOWN -> { onDownBtn(); true }
                else -> false
            }
        }
        return false
    }

    fun onUpBtn() {
        if (!enabled) return
        val curVal = editor.getText().toFloatOrNull() ?: return
        val inc = increment * modifiedIncrement()
        var v = clampPrecision(curVal + inc, precision).coerceIn(minValue, maxValue)
        val saved = currentValue
        setValue(v)
        if (!validateValue(v)) { setValue(saved); reportInvalidData(); updateEditor(); return }
        updateEditor(); onCommit()
    }

    fun onDownBtn() {
        if (!enabled) return
        val curVal = editor.getText().toFloatOrNull() ?: return
        val inc = increment * modifiedIncrement()
        var v = clampPrecision(curVal - inc, precision).coerceIn(minValue, maxValue)
        val saved = currentValue
        setValue(v)
        if (!validateValue(v)) { setValue(saved); reportInvalidData(); updateEditor(); return }
        updateEditor(); onCommit()
    }

    fun onEditorGainFocus() { onFocusReceived() }

    fun onEditorLostFocus() {
        onFocusLost()
        val v = editor.getText().toFloatOrNull() ?: return
        if (currentValue != v && !editor.isDirty()) updateEditor()
    }

    private fun onEditorCommit() {
        var success = false
        if (editor.evaluateFloat()) {
            var v = editor.getText().toFloatOrNull()?.coerceIn(minValue, maxValue) ?: currentValue
            val saved = currentValue
            setValue(v)
            if (validateValue(v)) { success = true; onCommit() } else setValue(saved)
        }
        updateEditor()
        if (success) editor.resetScrollPosition() else reportInvalidData()
    }

    private fun updateLabelColor() {
        labelBox?.setColor(if (enabled) textEnabledColor else textDisabledColor)
    }

    private fun updateEditor() {
        val displayed = clampPrecision(currentValue, precision)
        editor.setText(displayed.toBigDecimal()
            .setScale(precision, java.math.RoundingMode.HALF_UP)
            .toPlainString())
    }

    private fun reportInvalidData() {
        makeUiSound("UISndBadKeystroke")
    }

    open fun validateValue(v: Float): Boolean = true
    open fun modifiedIncrement(): Float = 1f
    open fun setControlValue(v: Float) {}
    open fun makeUiSound(sound: String) {}
    open fun onFocusReceived() {}
    open fun onFocusLost() {}
}

private fun Button.isHeld(): Boolean = false
private fun LineEditor.hasFocus(): Boolean = false
private fun LineEditor.setFocus(b: Boolean) {}
private fun LineEditor.isDirty(): Boolean = false
private fun LineEditor.evaluateFloat(): Boolean = getText().toFloatOrNull() != null
private fun LineEditor.resetScrollPosition() {}
private fun LineEditor.setTentative(b: Boolean) {}
private fun LineEditor.onTabInto() {}
private fun TextBox.setColor(c: Any?) {}
private fun TextBox.setTextArg(key: String, text: String) {}
