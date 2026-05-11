package com.firestorm.newview

// ---------------------------------------------------------------------------
// LLLooksHistoryPanel
//
// No .cpp was present in the source tree; this translation is derived solely
// from the header.  All UI / rendering operations are stubbed with TODO.
// ---------------------------------------------------------------------------

abstract class LLPanelAppearanceTab {
    abstract fun onSearchEdit(string: String)
    abstract fun onShowOnMap()
    abstract fun onLooks()
}

class LLLooksHistoryPanel : LLPanelAppearanceTab() {

    private enum class LooksHistoryColumnOrder {
        LIST_ICON,
        LIST_ITEM_TITLE,
        LIST_INDEX
    }

    private var mLooksHistory: Any? = null
    private var mHistoryItems: Any? = null
    private var mFilterSubString: String = ""

    fun postBuild(): Boolean {
        TODO("GPU: inflate XML layout, bind mHistoryItems scroll list, bind mLooksHistory, register double-click listener -> onDoubleClickItem")
    }

    override fun onSearchEdit(string: String) {
        mFilterSubString = string
        showLooksHistory()
    }

    override fun onShowOnMap() {
        TODO("GPU: get selected item coords from mHistoryItems and show on world map")
    }

    override fun onLooks() {
        TODO("GPU: apply or preview the selected look from mLooksHistory")
    }

    fun showLooksHistory() {
        TODO("GPU: clear mHistoryItems, iterate mLooksHistory filtered by mFilterSubString, add rows (icon, title, index)")
    }

    fun handleItemSelect(data: Map<String, Any>) {
        TODO("GPU: update UI controls based on the selected item described by data")
    }

    companion object {
        @JvmStatic
        fun onDoubleClickItem(userData: Any?) {
            TODO("GPU: cast userData to LLLooksHistoryPanel and invoke onLooks()")
        }
    }
}
