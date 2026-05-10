package com.firestorm.llcorehttp

class HttpHeaders {
    private val entries = mutableListOf<Pair<String, String>>()

    fun append(name: String, value: String) {
        entries.add(name to value)
    }

    fun find(name: String): String? =
        entries.lastOrNull { it.first == name }?.second

    fun remove(name: String) {
        entries.removeAll { it.first == name }
    }

    fun clear() {
        entries.clear()
    }

    fun getAll(): List<Pair<String, String>> = entries.toList()

    operator fun iterator(): Iterator<Pair<String, String>> = entries.iterator()

    val size: Int get() = entries.size
}
