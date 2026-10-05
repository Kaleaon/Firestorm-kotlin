package com.firestorm.newview

abstract class LLComboBox {
    protected val items: MutableList<String> = mutableListOf()
    protected var textEntry: String = ""

    open fun add(text: String, atTop: Boolean = false) {
        if (atTop) items.add(0, text) else items.add(text)
    }

    open fun remove(index: Int) {
        if (index in items.indices) items.removeAt(index)
    }

    fun removeAll() {
        items.clear()
        textEntry = ""
    }

    fun getSimple(): String = textEntry

    fun setTextEntry(text: String) {
        textEntry = text
    }

    open fun showList() {}
    open fun hideList() {}
    open fun prearrangeList(filter: String = "") {}

    open fun onTextEntry(text: String) {}
    open fun onTextCommit(text: String) {}
}

class LLSearchComboBox : LLComboBox() {

    private val searchHistoryBuilder = LLSearchHistoryBuilder(this)

    var dropdownButtonVisible: Boolean = false

    init {
        textEntry = ""
    }

    fun remove(name: String): Boolean {
        val idx = items.indexOfFirst { it.equals(name, ignoreCase = true) }
        if (idx >= 0) {
            items.removeAt(idx)
            return true
        }
        return false
    }

    fun clearHistory() {
        removeAll()
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        return false
    }

    fun focusTextEntry() {
        // no-op
    }

    override fun onTextEntry(text: String) {
        if (text.isEmpty()) {
            prearrangeList()
            hideList()
        } else {
            prearrangeList(text)
            if (items.isNotEmpty()) {
                showList()
                focusTextEntry()
            } else {
                hideList()
            }
        }
        super.onTextEntry(text)
    }

    override fun hideList() {
        super.hideList()
        focusTextEntry()
    }

    fun onSelectionCommit() {
        val query = getSimple().trim()
        if (query.isNotEmpty()) {
            remove(query)
            add(query, atTop = true)
        }
        textEntry = query
    }

    fun onSearchPrearrange(filter: String) {
        rebuildSearchHistory(filter)
    }

    protected open fun rebuildSearchHistory(filter: String) {
        searchHistoryBuilder.buildSearchHistory(filter)
    }
}

private class LLSearchHistoryBuilder(private val comboBox: LLSearchComboBox) {

    fun buildSearchHistory(filter: String) {
        val allHistory = LLSearchHistory.getSearchHistoryList()

        val itemsToShow: List<LLSearchHistory.LLSearchHistoryItem> = if (filter.isEmpty()) {
            allHistory
        } else {
            allHistory
                .filter { it.searchQuery.contains(filter, ignoreCase = true) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.searchQuery })
        }

        comboBox.removeAll()
        itemsToShow.forEach { comboBox.add(it.searchQuery) }
    }
}
