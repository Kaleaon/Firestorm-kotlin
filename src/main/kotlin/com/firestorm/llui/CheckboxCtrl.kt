package com.firestorm.llui

import kotlin.math.max
import kotlin.math.min

enum class WordWrap { NONE, UP, DOWN }

class CheckboxCtrl(
    initialValue: Boolean = false,
    label: String = " ",
    wordWrap: WordWrap = WordWrap.NONE,
    onCheck: ((CheckboxCtrl) -> Boolean)? = null
) {
    var value: Boolean = initialValue
        private set

    var label: String = label.ifEmpty { " " }
    var wordWrap: WordWrap = wordWrap

    private var tentative: Boolean = false
    private var dirty: Boolean = false

    private var enabled: Boolean = true
    private var enabledColor: Any? = null
    private var disabledColor: Any? = null

    private val checkSignal: MutableList<(CheckboxCtrl) -> Boolean> = mutableListOf()

    init {
        onCheck?.let { checkSignal.add(it) }
    }

    fun setEnabled(b: Boolean) {
        enabled = b
        TODO("GPU: update label color to enabledColor or disabledColor")
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        TODO("GPU: reshape label and button rects, call updateBoundingRect()")
    }

    fun setValue(v: Boolean) {
        value = v
        dirty = true
    }

    fun getValue(): Boolean = value

    fun get(): Boolean = value

    fun set(v: Boolean) = setValue(v)

    fun setTentative(b: Boolean) {
        tentative = b
    }

    fun getTentative(): Boolean = tentative

    fun setLabelArg(key: String, text: String): Boolean {
        TODO("GPU: replace label arg and reshape")
    }

    fun clear() = setValue(false)

    fun onCommit() {
        if (!enabled) return
        tentative = false
        TODO("GPU: propagate commit to control binding and fire commit signal")
    }

    fun toggle(): Boolean {
        value = !value
        dirty = true
        return value
    }

    fun setBtnFocus() {
        TODO("GPU: set keyboard focus on button child")
    }

    fun setEnabledColor(color: Any) { enabledColor = color }
    fun setDisabledColor(color: Any) { disabledColor = color }

    fun setLabel(text: String) {
        label = text
        TODO("GPU: update label textbox and reshape")
    }

    fun getLabel(): String = label

    fun setControlName(controlName: String, context: Any?) {
        TODO("GPU: bind button to named control in context")
    }

    fun isDirty(): Boolean = dirty

    fun resetDirty() { dirty = false }

    fun setCheckCallback(cb: (CheckboxCtrl) -> Boolean) {
        checkSignal.add(cb)
    }

    fun draw() {
        if (checkSignal.isNotEmpty()) {
            val checked = checkSignal.last().invoke(this)
            if (value != checked) setValue(checked)
        }
        TODO("GPU: delegate to UICtrl::draw()")
    }

    fun getSearchText(): String = "$label${getToolTip()}${getName()}"

    fun getToolTip(): String = TODO("GPU: return tooltip string from view")
    fun getName(): String = TODO("GPU: return view name")
}
