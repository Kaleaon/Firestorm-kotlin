package com.firestorm.llui

import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToLong

private const val MAX_STRING_LENGTH = 255

private fun clampPrecision(value: Float, decimalPrecision: Int): Float {
    var v = value.toDouble()
    repeat(decimalPrecision) { v *= 10.0 }
    v = v.roundToLong().toDouble()
    repeat(decimalPrecision) { v /= 10.0 }
    return v.toFloat()
}

open class TextBox(var text: String = "", var color: Any? = null) {
    fun setText(t: String) { text = t }
    fun setTextArg(key: String, replacement: String) {}
    fun setColor(c: Any?) { color = c }
}

open class SpinCtrl(
    var minValue: Float = 0f,
    var maxValue: Float = 1f,
    var increment: Float = 0.1f,
    var precision: Int = 3,
    var labelWidth: Int = 0,
    allowTextEntry: Boolean = true,
    allowDigitsOnly: Boolean = false,
    dynamicButtonHeight: Boolean = false,
    textEnabledColor: Any? = null,
    textDisabledColor: Any? = null,
    labelText: String = "",
    editorFactory: () -> LineEditor = { LineEditor() },
    upButtonFactory: () -> Button = { Button("up") },
    downButtonFactory: () -> Button = { Button("down") },
    labelBoxFactory: ((String) -> TextBox)? = null
) : UiCtrl() {

    private var currentValue: Float = minValue
    var initialValue: Float = minValue
    private var hasBeenSet: Boolean = false
    private var allowEdit: Boolean = allowTextEntry

    private val labelBox: TextBox? = if (labelText.isNotEmpty()) labelBoxFactory?.invoke(labelText) ?: TextBox(labelText) else null
    private val editor: LineEditor = editorFactory()
    private val upBtn: Button = upButtonFactory()
    private val downBtn: Button = downButtonFactory()
    private var textEnabledColor: Any? = textEnabledColor
    private var textDisabledColor: Any? = textDisabledColor

    init {
        editor.commitCallback = { _, _ -> onEditorCommit() }
        editor.setSelectAllOnCommit(false)
        updateEditor()
    }

    open fun getValue(): Any? = currentValue
    open fun getValueF32(): Float = currentValue

    open fun setValue(value: Any?) {
        val v = value?.toString()?.toFloatOrNull() ?: return
        if (currentValue != v || !hasBeenSet) {
            hasBeenSet = true
            currentValue = v.coerceIn(minValue, maxValue)
            if (!editor.hasFocus()) updateEditor()
        }
    }

    open fun forceSetValue(value: Any?) {
        val v = value?.toString()?.toFloatOrNull() ?: return
        if (currentValue != v || !hasBeenSet) {
            hasBeenSet = true
            currentValue = v.coerceIn(minValue, maxValue)
            updateEditor()
            editor.resetScrollPosition()
        }
    }

    fun get(): Float = getValueF32()
    fun set(value: Float) {
        setValue(value)
        initialValue = value
    }

    fun isMouseHeldDown(): Boolean = downBtn.isHeld() || upBtn.isHeld()

    open fun setEnabled(b: Boolean) {
        editor.setEnabled(b)
        updateLabelColor()
    }

    open fun setFocus(b: Boolean) {
        editor.setFocus(b)
    }

    open fun clear() {
        setValue(minValue)
        editor.clear()
        hasBeenSet = false
    }

    open fun isDirty(): Boolean = getValueF32() != initialValue
    open fun resetDirty() { initialValue = getValueF32() }

    open fun setPrecision(p: Int) {
        require(p in 0..10) { "Precision out of range: $p" }
        precision = p
        updateEditor()
    }

    fun setLabel(label: String) {
        labelBox?.setText(label) ?: run { /* no label box, warn */ }
        updateLabelColor()
    }

    open fun setLabelArg(key: String, text: String): Boolean {
        labelBox?.setTextArg(key, text) ?: run { /* no label box, warn */ }
        updateLabelColor()
        return true
    }

    fun setLabelColor(c: Any?) { textEnabledColor = c; updateLabelColor() }
    fun setDisabledLabelColor(c: Any?) { textDisabledColor = c; updateLabelColor() }

    fun setAllowEdit(allowEdit: Boolean) {
        editor.setEnabled(allowEdit)
        this.allowEdit = allowEdit
    }

    open fun onTabInto() {
        editor.onTabInto()
    }

    open fun setTentative(b: Boolean) {
        editor.setTentative(b)
    }

    open fun onCommit() {
        setTentative(false)
        setControlValue(getValueF32())
    }

    fun forceEditorCommit() {
        onEditorCommit()
    }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        var c = clicks
        if (c > 0) {
            while (c-- > 0) onDownBtn()
        } else {
            while (c++ < 0) onUpBtn()
        }
        return true
    }

    open fun handleKeyHere(key: Key, mask: Int): Boolean {
        if (editor.hasFocus()) {
            return when (key) {
                Key.ESCAPE -> {
                    updateEditor()
                    editor.resetScrollPosition()
                    editor.setFocus(false)
                    true
                }
                Key.UP -> { onUpBtn(); true }
                Key.DOWN -> { onDownBtn(); true }
                else -> false
            }
        }
        return false
    }

    fun onUpBtn() {
        if (!isEnabled()) return
        val text = editor.getText()
        val curVal = text.toFloatOrNull() ?: return
        val inc = increment * modifiedIncrement()
        var v = clampPrecision(curVal + inc, precision)
        v = v.coerceIn(minValue, maxValue)

        val savedVal = getValueF32()
        setValue(v)
        if (!validateValue(v)) {
            setValue(savedVal)
            reportInvalidData()
            updateEditor()
            return
        }
        updateEditor()
        onCommit()
    }

    fun onDownBtn() {
        if (!isEnabled()) return
        val text = editor.getText()
        val curVal = text.toFloatOrNull() ?: return
        val inc = increment * modifiedIncrement()
        var v = clampPrecision(curVal - inc, precision)
        v = v.coerceIn(minValue, maxValue)

        val savedVal = getValueF32()
        setValue(v)
        if (!validateValue(v)) {
            setValue(savedVal)
            reportInvalidData()
            updateEditor()
            return
        }
        updateEditor()
        onCommit()
    }

    fun onEditorGainFocus() {
        onFocusReceived()
    }

    fun onEditorLostFocus() {
        onFocusLost()
        val text = editor.getText()
        val v = text.toFloatOrNull() ?: return
        val savedVal = getValueF32()
        if (savedVal != v && !editor.isDirty()) {
            updateEditor()
        }
    }

    private fun onEditorCommit() {
        var success = false
        if (editor.evaluateFloat()) {
            val text = editor.getText()
            var v = text.toFloatOrNull() ?: getValueF32()
            v = v.coerceIn(minValue, maxValue)
            val savedVal = getValueF32()
            setValue(v)
            if (validateValue(v)) {
                success = true
                onCommit()
            } else {
                setValue(savedVal)
            }
        }
        updateEditor()
        if (success) {
            editor.resetScrollPosition()
        } else {
            reportInvalidData()
        }
    }

    private fun updateLabelColor() {
        labelBox?.setColor(if (isEnabled()) textEnabledColor else textDisabledColor)
    }

    private fun updateEditor() {
        val displayed = clampPrecision(getValueF32(), precision)
        editor.setText(displayed.toBigDecimal().setScale(precision, java.math.RoundingMode.HALF_UP).toPlainString())
    }

    private fun reportInvalidData() {
        makeUiSound("UISndBadKeystroke")
    }

    open fun validateValue(v: Float): Boolean = true
    open fun modifiedIncrement(): Float = 1f
    open fun isEnabled(): Boolean = true
    open fun setControlValue(v: Float) {}
    open fun makeUiSound(name: String) {}
    open fun onFocusReceived() {}
    open fun onFocusLost() {}
    open fun setEnabled2(b: Boolean) {}
}

private fun LineEditor.setSelectAllOnCommit(b: Boolean) {}
private fun LineEditor.setEnabled(b: Boolean) {}
private fun LineEditor.setTentative(b: Boolean) {}
private fun LineEditor.onTabInto() {}
private fun Button.isHeld(): Boolean = false
