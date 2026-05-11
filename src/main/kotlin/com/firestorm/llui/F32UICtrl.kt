package com.firestorm.llui

import com.firestorm.llmath.Rect

open class F32UICtrl(
    name: String,
    rect: Rect = Rect(),
    initialValue: Float = 0f,
    minValue: Float = 0f,
    maxValue: Float = 1f,
    increment: Float = 0.1f
) : UICtrl(name, rect) {

    val initialValue: Float = initialValue
    var minValue: Float = minValue
    var maxValue: Float = maxValue
    var increment: Float = increment

    protected var viewModelValue: Float = initialValue

    open fun getValueF32(): Float = viewModelValue

    open fun getValue(): Float = getValueF32()

    open fun setValue(value: Float) {
        viewModelValue = value
    }

    open fun setMinValue(min: Float) { minValue = min }
    open fun setMaxValue(max: Float) { maxValue = max }
    open fun setIncrement(inc: Float) { increment = inc }

    open fun getInitialValue(): Float = initialValue
    open fun getMinValue(): Float = minValue
    open fun getMaxValue(): Float = maxValue
    open fun getIncrement(): Float = increment
}
