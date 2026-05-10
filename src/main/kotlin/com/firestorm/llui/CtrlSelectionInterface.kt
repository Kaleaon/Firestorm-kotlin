package com.firestorm.llui

enum class AddPosition { ADD_TOP, ADD_BOTTOM, ADD_DEFAULT }

interface CtrlSelectionInterface {

    enum class Operation { DELETE, SELECT, DESELECT }

    fun getCanSelect(): Boolean
    fun getItemCount(): Int

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

    fun selectByValue(value: Any?): Boolean = setSelectedByValue(value, true)
    fun deselectByValue(value: Any?): Boolean = setSelectedByValue(value, false)
}

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

data class ScrollListItem(
    val value: Any?,
    val label: String,
    var selected: Boolean = false,
    val userdata: Any? = null
)
