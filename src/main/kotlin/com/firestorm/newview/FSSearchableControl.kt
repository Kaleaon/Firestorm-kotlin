package com.firestorm.newview

data class Color4(val r: Float, val g: Float, val b: Float, val a: Float = 1f)

object UIColorTable {
    fun getColor(name: String, default: Color4): Color4 {
        TODO("UI: look up named colour from the active UI colour table; return default if absent")
    }
}

abstract class SearchableControl {

    private var highlighted: Boolean = false

    val highlightColor: Color4
        get() = UIColorTable.getColor("SearchableControlHighlightColor", Color4(1f, 0f, 0f))

    fun setHighlighted(value: Boolean) {
        highlighted = value
        onSetHighlight()
    }

    fun getHighlighted(): Boolean = highlighted

    fun getSearchText(): String = searchText()

    protected abstract fun searchText(): String

    protected open fun onSetHighlight() {}
}
