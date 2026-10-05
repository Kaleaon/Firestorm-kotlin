package com.firestorm.llui

import kotlin.math.log10
import kotlin.math.pow

class MultiSliderCtrl(
    private val multiSlider: MultiSlider,
    private val showText: Boolean = true,
    private val canEditText: Boolean = false,
    private var precision: Int = 3,
    private val labelWidth: Int = 0,
    private val textWidth: Int = 0,
    textColor: Color4 = Color4.white,
    textDisabledColor: Color4 = Color4.white
) {
    private var curValue: Float = multiSlider.getCurSliderValue()
    private var textEnabledColor: Color4 = textColor
    private var textDisabledColor: Color4 = textDisabledColor
    private var enabled: Boolean = true

    private var labelText: String = ""
    private var displayedText: String = ""

    val sliderMouseDownListeners: MutableList<() -> Unit> = mutableListOf()
    val sliderMouseUpListeners: MutableList<() -> Unit> = mutableListOf()
    val validateListeners: MutableList<(Float) -> Boolean> = mutableListOf()
    val commitListeners: MutableList<() -> Unit> = mutableListOf()

    init {
        multiSlider.mouseDownListeners.add { sliderMouseDownListeners.forEach { it() } }
        multiSlider.mouseUpListeners.add { sliderMouseUpListeners.forEach { it() } }
        updateText()
    }

    fun getSliderValue(name: String): Float = multiSlider.getSliderValue(name)

    fun setSliderValue(name: String, value: Float, fromEvent: Boolean = false) {
        multiSlider.setSliderValue(name, value, fromEvent)
        curValue = multiSlider.getCurSliderValue()
        updateText()
    }

    fun setValue(valueMap: Map<String, Float>) {
        multiSlider.setValue(valueMap)
        curValue = multiSlider.getCurSliderValue()
        updateText()
    }

    fun getValue(): Map<String, Float> = multiSlider.thumbRects.keys.associateWith { multiSlider.getSliderValue(it) }

    fun getCurSlider(): String = multiSlider.curSlider
    fun getCurSliderValue(): Float = curValue
    fun setCurSliderValue(value: Float, fromEvent: Boolean = false) {
        setSliderValue(multiSlider.curSlider, value, fromEvent)
    }

    fun setCurSlider(name: String) {
        multiSlider.setCurSlider(name)
        curValue = multiSlider.getCurSliderValue()
    }

    fun resetCurSlider() { multiSlider.resetCurSlider() }

    fun setMinValue(value: Float) { multiSlider.minValue = value }
    fun setMaxValue(value: Float) { multiSlider.maxValue = value }
    fun setIncrement(value: Float) { multiSlider.increment = value }
    fun getMinValue(): Float = multiSlider.minValue
    fun getMaxValue(): Float = multiSlider.maxValue

    fun getNearestIncrement(value: Float): Float = multiSlider.getNearestIncrement(value)
    fun getSliderValueFromPos(x: Int, y: Int): Float = multiSlider.getSliderValueFromPos(x, y)
    fun getSliderThumbRect(name: String): Rect = multiSlider.getSliderThumbRect(name)

    fun setSliderThumbImage(name: String) { multiSlider.setSliderThumbImage(name) }
    fun clearSliderThumbImage() { multiSlider.clearSliderThumbImage() }

    fun getMaxNumSliders(): Int = multiSlider.maxNumSliders
    fun getCurNumSliders(): Int = multiSlider.curNumSliders
    fun getOverlapThreshold(): Float = multiSlider.getOverlapThreshold()
    fun canAddSliders(): Boolean = multiSlider.canAddSliders()

    fun addSlider(): String {
        val name = multiSlider.addSlider()
        if (name.isNotEmpty()) {
            curValue = multiSlider.getCurSliderValue()
            updateText()
        }
        return name
    }

    fun addSlider(value: Float): String {
        val name = multiSlider.addSlider(value)
        if (name.isNotEmpty()) {
            curValue = multiSlider.getCurSliderValue()
            updateText()
        }
        return name
    }

    fun addSlider(value: Float, name: String): Boolean {
        val result = multiSlider.addSlider(value, name)
        if (result) {
            curValue = multiSlider.getCurSliderValue()
            updateText()
        }
        return result
    }

    fun deleteSlider(name: String) {
        multiSlider.deleteSlider(name)
        curValue = multiSlider.getCurSliderValue()
        updateText()
    }

    fun deleteCurSlider() { deleteSlider(multiSlider.curSlider) }

    fun clear() {
        setCurSliderValue(0f)
        displayedText = ""
        multiSlider.clear()
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        multiSlider.let {
            // propagate enable state; label/textbox color changes happen in draw
        }
    }

    fun setLabel(label: String) { labelText = label }
    fun setLabelColor(color: Color4) { textEnabledColor = color }
    fun setDisabledLabelColor(color: Color4) { textDisabledColor = color }

    fun setPrecision(precision: Int) {
        require(precision in 0..10) { "precision out of range" }
        this.precision = precision
        updateText()
    }

    fun setTentative(tentative: Boolean) {
        // mark editor as tentative if present
    }

    fun onCommit() {
        setTentative(false)
        commitListeners.forEach { it() }
    }

    fun onTabInto() {
        // forward to editor if present
    }

    fun setControlName(controlName: String) {
        // propagate control name to multiSlider
    }

    fun onSliderCommit() {
        val savedVal = curValue
        val newVal = multiSlider.getCurSliderValue()
        curValue = newVal

        val valid = validateListeners.isEmpty() || validateListeners.all { it(newVal) }
        if (valid) {
            onCommit()
        } else {
            if (curValue != savedVal) setCurSliderValue(savedVal)
            reportInvalidData()
        }
        updateText()
    }

    fun onEditorCommit(text: String) {
        val savedVal = curValue
        val parsed = text.toFloatOrNull()
        if (parsed != null && parsed in multiSlider.minValue..multiSlider.maxValue) {
            setCurSliderValue(parsed)
            val valid = validateListeners.isEmpty() || validateListeners.all { it(parsed) }
            if (valid) {
                onCommit()
                updateText()
                return
            }
        }
        if (getCurSliderValue() != savedVal) setCurSliderValue(savedVal)
        reportInvalidData()
        updateText()
    }

    private fun updateText() {
        val scale = 10.0.pow(precision.toDouble())
        val displayed = (kotlin.math.floor(curValue * scale + 0.5) / scale).toFloat()
        displayedText = "%.${precision}f".format(displayed)
    }

    private fun reportInvalidData() {
        System.err.println("MultiSliderCtrl: reportInvalidData not yet implemented")
    }

    fun getDisplayedText(): String = displayedText
    fun getLabelText(): String = labelText
}
