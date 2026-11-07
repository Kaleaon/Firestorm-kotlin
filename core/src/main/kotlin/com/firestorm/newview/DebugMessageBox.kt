package com.firestorm.newview

import kotlin.math.abs

enum class DebugVarType {
    F32,
    S32,
    VEC3,
}

data class Vector2(val x: Float, val y: Float)
data class Vector3(val x: Float, val y: Float, val z: Float) {
    fun abs() = Vector3(abs(x), abs(y), abs(z))
}

class SliderCtrl(
    var minValue: Float = 0f,
    var maxValue: Float = 100f,
    var increment: Float = 0.1f,
    var value: Float = 0f,
    var decimalDigits: Int = 3,
    var commitCallback: ((Float) -> Unit)? = null
) {
    fun getValue(): Float = value
    fun setValue(v: Float) { value = v; commitCallback?.invoke(v) }
    fun getMaxValue(): Float = maxValue
}

class TextBox(var text: String = "")

class Button(var label: String = "") {
    var toggleState: Boolean = false
    fun setToggleState(on: Boolean) { toggleState = on }
    fun setFocus(focused: Boolean) {}
}

open class Floater(val key: Any? = null) {
    open fun draw() {}
    open fun postBuild(): Boolean = true
}

class DebugVarMessageBox private constructor(
    private val title: String,
    private val varType: DebugVarType,
    private var varData: Any?
) : Floater() {

    private val slider1: SliderCtrl? = when (varType) {
        DebugVarType.F32 -> SliderCtrl(
            minValue = -100f, maxValue = 100f, increment = 0.1f, decimalDigits = 3,
            value = (varData as? FloatRef)?.value ?: 0f
        )
        DebugVarType.S32 -> SliderCtrl(
            minValue = -255f, maxValue = 255f, increment = 1f, decimalDigits = 0,
            value = (varData as? IntRef)?.value?.toFloat() ?: 0f
        )
        DebugVarType.VEC3 -> SliderCtrl(
            minValue = -100f, maxValue = 100f, increment = 0.1f, decimalDigits = 3,
            value = (varData as? Vector3Ref)?.value?.x ?: 0f
        )
    }
    private val slider2: SliderCtrl? = if (varType == DebugVarType.VEC3)
        SliderCtrl(minValue = -100f, maxValue = 100f, increment = 0.1f,
            value = (varData as? Vector3Ref)?.value?.y ?: 0f) else null
    private val slider3: SliderCtrl? = if (varType == DebugVarType.VEC3)
        SliderCtrl(minValue = -100f, maxValue = 100f, increment = 0.1f,
            value = (varData as? Vector3Ref)?.value?.z ?: 0f) else null

    private val animateButton: Button = Button("Animate")
    private val textBox: TextBox = TextBox()
    private var animate: Boolean = false

    init {
        slider1?.commitCallback = { sliderChanged() }
        slider2?.commitCallback = { sliderChanged() }
        slider3?.commitCallback = { sliderChanged() }
    }

    private fun sliderChanged() {
        val data = varData ?: return
        when (varType) {
            DebugVarType.F32 -> (data as? FloatRef)?.value = slider1?.getValue() ?: 0f
            DebugVarType.S32 -> (data as? IntRef)?.value = slider1?.getValue()?.toInt() ?: 0
            DebugVarType.VEC3 -> (data as? Vector3Ref)?.value = Vector3(
                slider1?.getValue() ?: 0f,
                slider2?.getValue() ?: 0f,
                slider3?.getValue() ?: 0f
            )
        }
    }

    private fun onAnimateClicked() {
        animate = !animate
        animateButton.setToggleState(animate)
    }

    override fun draw() {
        val text = when (varType) {
            DebugVarType.F32 -> "%.3f".format((varData as? FloatRef)?.value ?: 0f)
            DebugVarType.S32 -> "%d".format((varData as? IntRef)?.value ?: 0)
            DebugVarType.VEC3 -> {
                val v = (varData as? Vector3Ref)?.value ?: Vector3(0f, 0f, 0f)
                "%.3f %.3f %.3f".format(v.x, v.y, v.z)
            }
        }
        textBox.text = text

        if (animate) {
            slider1?.let { s ->
                val elapsedSeconds = System.currentTimeMillis() / 1000.0
                val animatedVal = ((elapsedSeconds / 5.0) % 1.0).toFloat() * s.getMaxValue()
                s.setValue(animatedVal)
                slider2?.setValue(animatedVal)
                slider3?.setValue(animatedVal)
            }
        }

        super.draw()
    }

    companion object {
        private val instances: MutableMap<String, DebugVarMessageBox> = mutableMapOf()

        fun show(title: String, varRef: FloatRef, maxValue: Float = 100f, increment: Float = 0.1f) {
            val box = showInternal(title, DebugVarType.F32, varRef)
            val absMax = abs(maxValue)
            box.slider1?.apply {
                this.maxValue = absMax
                this.minValue = -absMax
                this.increment = increment
                this.value = varRef.value
            }
            box.slider1?.commitCallback = { box.sliderChanged() }
        }

        fun show(title: String, varRef: IntRef, maxValue: Int = 255, increment: Int = 1) {
            val box = showInternal(title, DebugVarType.S32, varRef)
            val absMax = abs(maxValue.toFloat())
            box.slider1?.apply {
                this.maxValue = absMax
                this.minValue = -absMax
                this.increment = increment.toFloat()
                this.value = varRef.value.toFloat()
            }
            box.slider1?.commitCallback = { box.sliderChanged() }
        }

        fun show(
            title: String,
            varRef: Vector3Ref,
            maxValue: Vector3 = Vector3(100f, 100f, 100f),
            increment: Vector3 = Vector3(0.1f, 0.1f, 0.1f)
        ) {
            val box = showInternal(title, DebugVarType.VEC3, varRef)
            val absMax = maxValue.abs()
            box.slider1?.apply {
                this.maxValue = absMax.x; this.minValue = -absMax.x; this.increment = increment.x
                commitCallback = { box.sliderChanged() }
            }
            box.slider2?.apply {
                this.maxValue = absMax.y; this.minValue = -absMax.y; this.increment = increment.y
                commitCallback = { box.sliderChanged() }
            }
            box.slider3?.apply {
                this.maxValue = absMax.z; this.minValue = -absMax.z; this.increment = increment.z
                commitCallback = { box.sliderChanged() }
            }
        }

        private fun showInternal(title: String, varType: DebugVarType, varData: Any): DebugVarMessageBox {
            return instances.getOrPut(title) {
                DebugVarMessageBox(title, varType, varData)
            }
        }

        fun removeInstance(title: String) {
            instances.remove(title)
        }
    }
}

class FloatRef(var value: Float)
class IntRef(var value: Int)
class Vector3Ref(var value: Vector3)
