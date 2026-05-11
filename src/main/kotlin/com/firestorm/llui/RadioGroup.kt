package com.firestorm.llui

enum class SelectionOp { DELETE, DESELECT }

interface CtrlSelectionInterface {
    fun getItemCount(): Int
    fun getCanSelect(): Boolean
    fun selectFirstItem(): Boolean
    fun selectNthItem(index: Int): Boolean
    fun selectItemRange(first: Int, last: Int): Boolean
    fun getFirstSelectedIndex(): Int
    fun setCurrentById(id: String): Boolean
    fun getCurrentId(): String
    fun setSelectedByValue(value: Any?, selected: Boolean): Boolean
    fun getSelectedValue(): Any?
    fun isSelected(value: Any?): Boolean
    fun operateOnSelection(op: SelectionOp): Boolean
    fun operateOnAll(op: SelectionOp): Boolean
}

class RadioCtrl(
    val name: String,
    payload: Any? = null
) {
    var payload: Any? = payload.let { if (it == null) name else it }
    var enabled: Boolean = true
    var tabStop: Boolean = false
    var checked: Boolean = false
    var label: String = name

    fun setValue(v: Boolean) { checked = v }
    fun setTabStop(v: Boolean) { tabStop = v }
    fun hasTabStop(): Boolean = tabStop

    fun focusFirstItem() {
        TODO("APR: use JVM equivalent for focus management")
    }

    fun handleMouseDown(x: Int, y: Int, parent: RadioGroup?): Boolean {
        if (tabStop && enabled) {
            focusFirstItem()
        } else {
            parent?.focusSelectedRadioBtn()
        }
        return true
    }
}

open class RadioGroup(
    val allowDeselect: Boolean = false
) : CtrlSelectionInterface {

    private val radioButtons: MutableList<RadioCtrl> = mutableListOf()
    private var selectedIndex: Int = -1

    fun addItem(name: String, payload: Any? = null) {
        val btn = RadioCtrl(name, payload)
        btn.tabStop = radioButtons.isEmpty()
        radioButtons.add(btn)
    }

    open fun postBuild(): Boolean {
        radioButtons.firstOrNull()?.tabStop = true
        return true
    }

    fun setIndexEnabled(index: Int, enabled: Boolean) {
        val btn = radioButtons.getOrNull(index) ?: return
        btn.enabled = enabled
        if (index == selectedIndex && !enabled) {
            setSelectedIndex(-1)
        }
        if (selectedIndex < 0) {
            var count = 0
            for (child in radioButtons) {
                if (count >= index && selectedIndex >= 0) break
                if (child.enabled) setSelectedIndex(count)
                count++
            }
            if (selectedIndex < 0) setSelectedIndex(0)
        }
    }

    fun getSelectedIndex(): Int = selectedIndex

    fun setSelectedIndex(index: Int, fromEvent: Boolean = false): Boolean {
        if (index >= radioButtons.size) return false
        if (index < -1) return false
        if (index < 0 && selectedIndex >= 0 && !allowDeselect) return false

        if (selectedIndex >= 0) {
            radioButtons[selectedIndex].setTabStop(false)
            radioButtons[selectedIndex].setValue(false)
        } else {
            radioButtons.getOrNull(0)?.setTabStop(false)
        }

        selectedIndex = index

        if (selectedIndex >= 0) {
            val item = radioButtons[selectedIndex]
            item.setTabStop(true)
            item.setValue(true)
            if (hasFocus()) item.focusFirstItem()
        }

        if (!fromEvent) setControlValue(getValue())
        return true
    }

    fun focusSelectedRadioBtn() {
        if (selectedIndex >= 0) {
            val item = radioButtons[selectedIndex]
            if (item.hasTabStop() && item.enabled) item.focusFirstItem()
        } else if (radioButtons.getOrNull(0)?.hasTabStop() == true || hasTabStop()) {
            focusFirstItem()
        }
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (mask != 0) return false
        val KEY_UP = 0x81; val KEY_DOWN = 0x82; val KEY_LEFT = 0x83; val KEY_RIGHT = 0x84
        return when (key) {
            KEY_DOWN, KEY_RIGHT -> {
                if (!setSelectedIndex(selectedIndex + 1)) makeUiSound("UISndInvalidOp") else onCommit()
                true
            }
            KEY_UP, KEY_LEFT -> {
                if (!setSelectedIndex(selectedIndex - 1)) makeUiSound("UISndInvalidOp") else onCommit()
                true
            }
            else -> false
        }
    }

    fun onClickButton(clicked: RadioCtrl) {
        val index = radioButtons.indexOf(clicked)
        if (index < 0) return
        if (index == selectedIndex && allowDeselect) setSelectedIndex(-1)
        else setSelectedIndex(index)
        onCommit()
    }

    open fun setValue(value: Any?) {
        val strVal = value?.toString() ?: ""
        radioButtons.forEachIndexed { idx, btn ->
            if (btn.payload?.toString() == strVal) {
                setSelectedIndex(idx)
                return
            }
        }
        val asInt = strVal.toIntOrNull()
        if (asInt != null) setSelectedIndex(asInt, fromEvent = true)
        else setSelectedIndex(-1, fromEvent = true)
    }

    open fun getValue(): Any? = radioButtons.getOrNull(selectedIndex)?.payload

    override fun getItemCount(): Int = radioButtons.size
    override fun getCanSelect(): Boolean = true
    override fun selectFirstItem(): Boolean = setSelectedIndex(0)
    override fun selectNthItem(index: Int): Boolean = setSelectedIndex(index)
    override fun selectItemRange(first: Int, last: Int): Boolean = setSelectedIndex(first)
    override fun getFirstSelectedIndex(): Int = selectedIndex
    override fun setCurrentById(id: String): Boolean = false
    override fun getCurrentId(): String = ""

    override fun setSelectedByValue(value: Any?, selected: Boolean): Boolean {
        val strVal = value?.toString() ?: ""
        radioButtons.forEachIndexed { idx, btn ->
            if (btn.payload?.toString() == strVal) {
                setSelectedIndex(idx)
                return true
            }
        }
        return false
    }

    override fun getSelectedValue(): Any? = getValue()

    override fun isSelected(value: Any?): Boolean {
        val strVal = value?.toString() ?: ""
        return radioButtons.getOrNull(selectedIndex)?.payload?.toString() == strVal
    }

    override fun operateOnSelection(op: SelectionOp): Boolean = false
    override fun operateOnAll(op: SelectionOp): Boolean = false

    open fun hasFocus(): Boolean = false
    open fun hasTabStop(): Boolean = false
    open fun focusFirstItem() {}
    open fun setControlValue(v: Any?) {}
    open fun onCommit() {}
    open fun makeUiSound(name: String) {}
}
