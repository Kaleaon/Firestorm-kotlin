package com.firestorm.llui

import kotlin.math.max

enum class Operation { DELETE, DESELECT }

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
    fun operateOnSelection(op: Operation): Boolean
    fun operateOnAll(op: Operation): Boolean
}

class RadioCtrl(
    val name: String,
    val label: String,
    var payload: Any?,
    var enabled: Boolean = true,
    var tabStop: Boolean = false,
    var checked: Boolean = false
) {
    fun setValue(v: Boolean) {
        checked = v
    }

    fun setTabStop(v: Boolean) {
        tabStop = v
    }

    fun hasTabStop(): Boolean = tabStop
    fun getEnabled(): Boolean = enabled
    fun getPayload(): Any? = payload
    fun postBuild(): Boolean {
        return true
    }

    fun handleMouseDown(x: Int, y: Int, parent: RadioGroup?): Boolean {
        if (tabStop && enabled) {
            focusFirstItem()
        } else {
            parent?.focusSelectedRadioBtn()
        }
        return true
    }

    fun focusFirstItem() {
        // TODO("APR: use JVM equivalent")
    }
}

open class RadioGroup(
    val allowDeselect: Boolean = false
) : UiCtrl(), CtrlSelectionInterface {

    private val radioButtons: MutableList<RadioCtrl> = mutableListOf()
    private var selectedIndex: Int = -1

    fun initItems(items: List<Pair<String, Any?>>) {
        for ((name, payload) in items) {
            val btn = RadioCtrl(name = name, label = name, payload = payload)
            btn.setTabStop(false)
            radioButtons.add(btn)
        }
        if (radioButtons.isNotEmpty()) {
            radioButtons[0].setTabStop(true)
        }
    }

    open fun postBuild(): Boolean {
        if (radioButtons.isNotEmpty()) {
            radioButtons[0].setTabStop(true)
        }
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
                if (child.getEnabled()) {
                    setSelectedIndex(count)
                }
                count++
            }
            if (selectedIndex < 0) {
                setSelectedIndex(0)
            }
        }
    }

    fun getSelectedIndex(): Int = selectedIndex

    fun setSelectedIndex(index: Int, fromEvent: Boolean = false): Boolean {
        if (radioButtons.size <= index) return false
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
            if (hasFocus()) {
                item.focusFirstItem()
            }
        }

        if (!fromEvent) {
            setControlValue(getValue())
        }

        return true
    }

    fun focusSelectedRadioBtn() {
        if (selectedIndex >= 0) {
            val item = radioButtons[selectedIndex]
            if (item.hasTabStop() && item.getEnabled()) {
                item.focusFirstItem()
            }
        } else if (radioButtons.getOrNull(0)?.hasTabStop() == true || hasTabStop()) {
            focusFirstItem()
        }
    }

    open fun handleKeyHere(key: Key, mask: Int): Boolean {
        if (mask != 0) return false
        return when (key) {
            Key.DOWN, Key.RIGHT -> {
                if (!setSelectedIndex(selectedIndex + 1)) makeUiSound("UISndInvalidOp")
                else onCommit()
                true
            }
            Key.UP, Key.LEFT -> {
                if (!setSelectedIndex(selectedIndex - 1)) makeUiSound("UISndInvalidOp")
                else onCommit()
                true
            }
            else -> false
        }
    }

    fun onClickButton(clicked: RadioCtrl) {
        val index = radioButtons.indexOf(clicked)
        if (index < 0) return

        if (index == selectedIndex && allowDeselect) {
            setSelectedIndex(-1)
        } else {
            setSelectedIndex(index)
        }
        onCommit()
    }

    open fun setValue(value: Any?) {
        val strVal = value?.toString() ?: ""
        var idx = 0
        for (btn in radioButtons) {
            if (btn.getPayload()?.toString() == strVal) {
                setSelectedIndex(idx)
                return
            }
            idx++
        }
        val intVal = strVal.toIntOrNull()
        if (intVal != null) {
            setSelectedIndex(intVal, fromEvent = true)
        } else {
            setSelectedIndex(-1, fromEvent = true)
        }
    }

    open fun getValue(): Any? {
        val idx = selectedIndex
        return radioButtons.getOrNull(idx)?.getPayload()
    }

    override fun getItemCount(): Int = radioButtons.size
    override fun getCanSelect(): Boolean = true
    override fun selectFirstItem(): Boolean = setSelectedIndex(0)
    override fun selectNthItem(index: Int): Boolean = setSelectedIndex(index)
    override fun selectItemRange(first: Int, last: Int): Boolean = setSelectedIndex(first)
    override fun getFirstSelectedIndex(): Int = getSelectedIndex()
    override fun setCurrentById(id: String): Boolean = false
    override fun getCurrentId(): String = ""
    override fun setSelectedByValue(value: Any?, selected: Boolean): Boolean {
        val strVal = value?.toString() ?: ""
        radioButtons.forEachIndexed { idx, btn ->
            if (btn.getPayload()?.toString() == strVal) {
                setSelectedIndex(idx)
                return true
            }
        }
        return false
    }
    override fun getSelectedValue(): Any? = getValue()
    override fun isSelected(value: Any?): Boolean {
        val strVal = value?.toString() ?: ""
        radioButtons.forEachIndexed { idx, btn ->
            if (btn.getPayload()?.toString() == strVal && idx == selectedIndex) return true
        }
        return false
    }
    override fun operateOnSelection(op: Operation): Boolean = false
    override fun operateOnAll(op: Operation): Boolean = false

    open fun hasFocus(): Boolean = false
    open fun hasTabStop(): Boolean = false
    open fun focusFirstItem() {}
    open fun setControlValue(v: Any?) {}
    open fun onCommit() {}
    open fun makeUiSound(name: String) {}
}
