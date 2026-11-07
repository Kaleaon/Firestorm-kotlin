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
        // no-op
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        // no-op
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
        System.err.println("CheckboxCtrl: setLabelArg not yet implemented")
        return false
    }

    fun clear() = setValue(false)

    fun onCommit() {
        if (!enabled) return
        tentative = false
        // no-op
    }

    fun toggle(): Boolean {
        value = !value
        dirty = true
        return value
    }

    fun setBtnFocus() {
        // no-op
    }

    fun setEnabledColor(color: Any) { enabledColor = color }
    fun setDisabledColor(color: Any) { disabledColor = color }

    fun setLabel(text: String) {
        label = text
        // no-op
    }

    fun getLabel(): String = label

    fun setControlName(controlName: String, context: Any?) {
        // no-op
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
        // no-op
    }

    fun getSearchText(): String = "$label${getToolTip()}${getName()}"

    fun getToolTip(): String = ""
    fun getName(): String = ""
}
