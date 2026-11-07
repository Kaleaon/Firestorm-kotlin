package com.firestorm.llui

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class SliderCtrl(
    initialValue: Float = 0f,
    minValue: Float = 0f,
    maxValue: Float = 1f,
    increment: Float = 0.01f,
    orientation: Orientation = Orientation.HORIZONTAL,
    var precision: Int = 3,
    val showText: Boolean = true,
    val canEditText: Boolean = false,
    val label: String = "",
    textEnabledColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    textDisabledColor: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 1f)
) {
    val slider: Slider = Slider(
        initialValue = initialValue,
        minValue = minValue,
        maxValue = maxValue,
        increment = increment,
        orientation = orientation
    )

    var textEnabledColor: FloatArray = textEnabledColor
    var textDisabledColor: FloatArray = textDisabledColor

    private var value: Float = initialValue
    private var enabled: Boolean = true

    var labelText: String = label
    var displayText: String = ""
    var editText: String = ""

    val editorCommitCallbacks: MutableList<(SliderCtrl, Float) -> Unit> = mutableListOf()

    private val validateCallbacks: MutableList<(SliderCtrl, Float) -> Boolean> = mutableListOf()

    init {
        slider.addMouseDownCallback { _, _ -> }
        slider.addMouseUpCallback { _, _ -> }
        updateText()
    }

    fun getValueF32(): Float = slider.getValueF32()

    fun setValue(v: Float, fromEvent: Boolean = false) {
        slider.setValue(v, fromEvent)
        value = slider.getValueF32()
        updateText()
    }

    fun setMinValue(minV: Float) { slider.setMinValue(minV); updateText() }
    fun setMaxValue(maxV: Float) { slider.setMaxValue(maxV); updateText() }
    fun setIncrement(inc: Float) { slider.setIncrement(inc) }
    fun getMinValue(): Float = slider.minValue
    fun getMaxValue(): Float = slider.maxValue

    fun isMouseHeldDown(): Boolean = false

    fun setPrecision(p: Int) {
        require(p in 0..10) { "Precision out of range" }
        precision = p
        updateText()
    }

    fun setEnabled(b: Boolean) {
        enabled = b
        slider.setMinValue(slider.minValue)
    }

    fun clear() {
        setValue(0f)
        displayText = ""
        editText = ""
    }

    fun setLabelText(text: String) {
        labelText = text
    }

    fun addSliderMouseDownCallback(cb: (Slider, Float) -> Unit) {
        slider.addMouseDownCallback(cb)
    }

    fun addSliderMouseUpCallback(cb: (Slider, Float) -> Unit) {
        slider.addMouseUpCallback(cb)
    }

    fun addEditorCommitCallback(cb: (SliderCtrl, Float) -> Unit) {
        editorCommitCallbacks.add(cb)
    }

    fun addValidateCallback(cb: (SliderCtrl, Float) -> Boolean) {
        validateCallbacks.add(cb)
    }

    fun onTabInto() {}

    fun setTentative(b: Boolean) {}

    fun onCommit() {
        setTentative(false)
        onControlValueChanged(getValueF32())
        commitCallbacks.forEach { it(this, getValueF32()) }
    }

    val commitCallbacks: MutableList<(SliderCtrl, Float) -> Unit> = mutableListOf()

    private fun onControlValueChanged(v: Float) {}

    private fun updateText() {
        val displayed = floor(getValueF32() * 10.0.pow(precision.toDouble()) + 0.5) / 10.0.pow(precision.toDouble())
        val formatted = "%.${precision}f".format(displayed)
        displayText = formatted
        editText = formatted
    }

    fun onEditorCommit(text: String) {
        val savedVal = value
        val parsed = text.toFloatOrNull()
        var success = false

        if (parsed != null && parsed in getMinValue()..getMaxValue()) {
            setValue(parsed)
            val validationPassed = validateCallbacks.isEmpty() || validateCallbacks.all { it(this, parsed) }
            if (validationPassed) success = true
        }

        if (success) {
            onCommit()
            editorCommitCallbacks.forEach { it(this, getValueF32()) }
        } else {
            if (getValueF32() != savedVal) setValue(savedVal)
            reportInvalidData()
        }
        updateText()
    }

    fun onSliderCommit() {
        val savedVal = value
        val newVal = slider.getValueF32()
        value = newVal

        val validationPassed = validateCallbacks.isEmpty() || validateCallbacks.all { it(this, newVal) }
        if (validationPassed) {
            onCommit()
        } else {
            if (value != savedVal) setValue(savedVal)
            reportInvalidData()
        }
        updateText()
    }

    private fun reportInvalidData() {
        System.err.println("SliderCtrl: reportInvalidData not yet implemented")
    }

    fun getSearchText(): String = "$labelText ${getToolTip()} ${getName()}"
    fun getToolTip(): String = ""
    fun getName(): String = ""
}
