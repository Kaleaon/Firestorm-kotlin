package com.firestorm.llui

enum class AddPosition { ADD_TOP, ADD_BOTTOM, ADD_DEFAULT }

data class ScrollListItem(
    val value: Any?,
    val label: String,
    var selected: Boolean = false,
    val userdata: Any? = null
)

interface CtrlListInterface : CtrlSelectionInterface {

    fun addColumn(column: Map<String, Any?>, pos: AddPosition = AddPosition.ADD_BOTTOM)
    fun clearColumns()
    fun setColumnLabel(column: String, label: String)
    fun addElement(value: Map<String, Any?>, pos: AddPosition = AddPosition.ADD_BOTTOM, userdata: Any? = null): ScrollListItem?

    fun addSimpleElement(value: String, pos: AddPosition = AddPosition.ADD_BOTTOM, id: Any? = null): ScrollListItem?

    fun clearRows()
    fun sortByColumn(name: String, ascending: Boolean)
}

interface CtrlScrollInterface {
    fun getScrollPos(): Int
    fun setScrollPos(pos: Int)
    fun scrollToShowSelected()
}
