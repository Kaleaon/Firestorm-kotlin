package com.firestorm.llmessage

import com.firestorm.llcommon.LLUUID

data class CacheEntry(
    val firstName: String,
    val lastName: String,
    val isGroup: Boolean,
    val createTime: Long = System.currentTimeMillis()
) {
    val fullName: String get() = if (isGroup || lastName.isEmpty() || lastName == "Resident") {
        firstName
    } else {
        "$firstName $lastName"
    }
}

object CacheName {

    private val cache: MutableMap<LLUUID, CacheEntry> = mutableMapOf()
    private val observers: MutableList<(LLUUID, String, Boolean) -> Unit> = mutableListOf()

    fun get(id: LLUUID, isGroup: Boolean, name: StringBuilder): Boolean {
        val entry = cache[id] ?: run {
            name.setLength(0)
            name.append("waiting")
            return false
        }
        name.setLength(0)
        name.append(entry.fullName)
        return true
    }

    fun getFullName(id: LLUUID, fullName: StringBuilder): Boolean = get(id, false, fullName)

    fun getGroupName(id: LLUUID, name: StringBuilder): Boolean = get(id, true, name)

    fun getFirstLastName(id: LLUUID, first: StringBuilder, last: StringBuilder): Boolean {
        val entry = cache[id] ?: return false
        first.setLength(0); first.append(entry.firstName)
        last.setLength(0);  last.append(entry.lastName)
        return true
    }

    fun put(id: LLUUID, isGroup: Boolean, firstName: String, lastName: String = "") {
        val entry = CacheEntry(firstName, lastName, isGroup)
        cache[id] = entry
        val name = entry.fullName
        observers.forEach { it(id, name, isGroup) }
    }

    fun addObserver(observer: (LLUUID, String, Boolean) -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: (LLUUID, String, Boolean) -> Unit) {
        observers.remove(observer)
    }

    fun cleanupMemory() {
        cache.clear()
    }

    fun deleteEntriesOlderThan(secs: Int) {
        val cutoff = System.currentTimeMillis() - secs * 1000L
        cache.entries.removeIf { it.value.createTime < cutoff }
    }

    fun getUUID(firstName: String, lastName: String): LLUUID? =
        cache.entries.firstOrNull { e ->
            e.value.firstName == firstName && e.value.lastName == lastName
        }?.key

    fun getUUID(fullName: String): LLUUID? =
        cache.entries.firstOrNull { e -> e.value.fullName == fullName }?.key

    fun buildFullName(first: String, last: String): String =
        if (last.isEmpty() || last == "Resident") first else "$first $last"

    fun cleanFullName(fullName: String): String =
        if (fullName.endsWith(" Resident")) fullName.removeSuffix(" Resident") else fullName

    fun buildUsername(name: String): String {
        val cleaned = cleanFullName(name)
        return cleaned.replace(' ', '.').lowercase()
    }

    val defaultLastName: String get() = "Resident"
}
