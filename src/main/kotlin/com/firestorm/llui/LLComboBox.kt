package com.firestorm.llui

import com.firestorm.llmath.Rect
import com.firestorm.llcommon.*

open class ComboBox(
    name: String,
    rect: Rect = Rect()
) : View(name, rect) {

    enum class PreferredPosition { ABOVE, BELOW }

    private val items: MutableList<String> = mutableListOf()
    private var selectedIndex: Int = -1

    var allowTextEntry: Boolean = false
    var allowNewValues: Boolean = false
    var maxChars: Int = 20
    var listPosition: PreferredPosition = PreferredPosition.BELOW

    var onChange: ((ComboBox) -> Unit)? = null
    var onTextEntry: ((ComboBox) -> Unit)? = null
    var onPrearrange: ((ComboBox) -> Unit)? = null

    companion object {
        const val MAX_COMBO_WIDTH: Int = 500
    }

    fun add(label: String): Int {
        items.add(label)
        return items.size - 1
    }

    fun remove(index: Int): Boolean {
        if (index < 0 || index >= items.size) return false
        items.removeAt(index)
        when {
            items.isEmpty() -> selectedIndex = -1
            selectedIndex >= items.size -> selectedIndex = items.size - 1
        }
        return true
    }

    fun remove(label: String): Boolean {
        val idx = items.indexOf(label)
        if (idx < 0) return false
        return remove(idx)
    }

    fun clear() {
        items.clear()
        selectedIndex = -1
    }

    fun getItemCount(): Int = items.size

    fun itemExists(label: String): Boolean = items.contains(label)

    fun getSelectedValue(): String =
        if (selectedIndex in items.indices) items[selectedIndex] else ""

    fun setSelectedByValue(value: String): Boolean {
        val idx = items.indexOf(value)
        if (idx < 0) return false
        setCurrentByIndex(idx)
        return true
    }

    fun setCurrentByIndex(index: Int): Boolean {
        if (index < 0 || index >= items.size) return false
        selectedIndex = index
        onChange?.invoke(this)
        return true
    }

    fun getCurrentIndex(): Int = selectedIndex

    fun selectNextItem(): Boolean {
        if (items.isEmpty()) return false
        return setCurrentByIndex((selectedIndex + 1).coerceAtMost(items.size - 1))
    }

    fun selectPrevItem(): Boolean {
        if (items.isEmpty()) return false
        return setCurrentByIndex((selectedIndex - 1).coerceAtLeast(0))
    }

    fun getSimple(): String = getSelectedValue()

    fun setSimple(label: String): Boolean = setSelectedByValue(label)

    fun sortByName(ascending: Boolean = true) {
        val current = getSelectedValue()
        if (ascending) items.sort() else items.sortDescending()
        selectedIndex = if (current.isNotEmpty()) items.indexOf(current) else -1
    }

    override fun draw() {
        if (!visible) return
        TODO("GL: draw combo box border, selected-item label, and drop arrow")
    }
}

open class IconsComboBox(
    name: String,
    rect: Rect = Rect()
) : ComboBox(name, rect) {

    var iconColumnIndex: Int = 0
    var labelColumnIndex: Int = 1

    override fun draw() {
        if (!visible) return
        TODO("GL: draw icon combo box with icon column $iconColumnIndex and label column $labelColumnIndex")
    }
}
