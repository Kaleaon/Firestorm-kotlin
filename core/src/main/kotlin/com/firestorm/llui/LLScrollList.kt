package com.firestorm.llui

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Rect

class ScrollList(
    name: String,
    rect: Rect = Rect()
) : View(name, rect) {

    data class Column(
        val name: String,
        val label: String,
        val width: Int,
        val dynamicWidth: Boolean = false
    )

    data class Cell(val value: LLSD, val column: String)

    class Item(
        val cells: MutableList<Cell> = mutableListOf(),
        var selected: Boolean = false,
        val uuid: LLUUID = LLUUID.NULL
    ) {
        fun getColumn(columnName: String): Cell? = cells.find { it.column == columnName }
    }

    val items: MutableList<Item> = mutableListOf()
    val columns: MutableList<Column> = mutableListOf()
    var sortCallback: ((Item, Item) -> Int)? = null

    fun addColumn(col: Column) {
        columns.add(col)
    }

    fun addItem(item: Item): Item {
        items.add(item)
        return item
    }

    fun clearRows() {
        items.clear()
    }

    fun deleteSelectedItems() {
        items.removeAll { it.selected }
    }

    fun getFirstSelected(): Item? = items.firstOrNull { it.selected }

    fun getAllSelected(): List<Item> = items.filter { it.selected }

    fun selectByValue(value: LLSD): Boolean {
        var found = false
        for (item in items) {
            val matches = item.cells.any { it.value == value }
            if (matches) {
                item.selected = true
                found = true
            }
        }
        return found
    }

    fun sortByColumn(name: String, ascending: Boolean) {
        val cb = sortCallback
        if (cb != null) {
            items.sortWith { a, b -> if (ascending) cb(a, b) else cb(b, a) }
            return
        }
        items.sortWith { a, b ->
            val cellA = a.getColumn(name)?.value
            val cellB = b.getColumn(name)?.value
            val cmp = compareValues(cellA?.toString(), cellB?.toString())
            if (ascending) cmp else -cmp
        }
    }

    override fun draw() {
        if (!visible) return
        // stub: draw column headers then rows, highlighting selected items
    }
}
