package com.firestorm.newview

import java.io.File

object LLSearchHistory {

    private const val SEARCH_HISTORY_FILE_NAME = "search_history.txt"
    private const val SEARCH_QUERY_KEY = "search_query"

    private val searchHistory: ArrayDeque<LLSearchHistoryItem> = ArrayDeque()

    fun getSearchHistoryList(): MutableList<LLSearchHistoryItem> = searchHistory.toMutableList()

    fun clearHistory() {
        searchHistory.clear()
    }

    fun addEntry(searchQuery: String) {
        if (searchQuery.isEmpty()) return

        val existing = searchHistory.indexOfFirst {
            it.searchQuery.equals(searchQuery, ignoreCase = true)
        }
        if (existing >= 0) {
            searchHistory.removeAt(existing)
        }

        searchHistory.addFirst(LLSearchHistoryItem(searchQuery))
    }

    fun save(): Boolean {
        val filePath = getHistoryFilePath()
        val hasNonEmpty = searchHistory.any { it.searchQuery.isNotEmpty() }
        if (!hasNonEmpty) {
            File(filePath).delete()
            return true
        }

        return try {
            File(filePath).bufferedWriter().use { writer ->
                searchHistory.forEach { item ->
                    writer.write("{'${SEARCH_QUERY_KEY}':'${item.searchQuery}'}")
                    writer.newLine()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun load(): Boolean {
        val filePath = getHistoryFilePath()
        val file = File(filePath)
        if (!file.exists()) return false

        clearHistory()

        return try {
            file.forEachLine { line ->
                val query = parseQueryFromLine(line)
                if (query != null) {
                    searchHistory.addLast(LLSearchHistoryItem(query))
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun parseQueryFromLine(line: String): String? {
        val key = "'$SEARCH_QUERY_KEY':'"
        val start = line.indexOf(key)
        if (start < 0) return null
        val valueStart = start + key.length
        val end = line.indexOf("'", valueStart)
        if (end < 0) return null
        return line.substring(valueStart, end)
    }

    private fun getHistoryFilePath(): String {
        System.err.println("LLSearchHistory: getHistoryFilePath not yet implemented")
        return ""
    }

    class LLSearchHistoryItem(val searchQuery: String) : Comparable<LLSearchHistoryItem> {

        override fun compareTo(other: LLSearchHistoryItem): Int =
            searchQuery.compareTo(other.searchQuery, ignoreCase = true)

        override fun equals(other: Any?): Boolean = when (other) {
            is LLSearchHistoryItem -> searchQuery.equals(other.searchQuery, ignoreCase = true)
            is String -> searchQuery.equals(other, ignoreCase = true)
            else -> false
        }

        override fun hashCode(): Int = searchQuery.lowercase().hashCode()
    }
}
