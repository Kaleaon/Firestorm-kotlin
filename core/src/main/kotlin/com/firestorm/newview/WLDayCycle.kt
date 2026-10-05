package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llrender.Shader

enum class WLParamScope { LOCAL, REGION }

data class WLParamKey(val name: String, val scope: WLParamScope)

data class WLDayCycle(
    val timeMap: MutableMap<Float, WLParamKey> = sortedMapOf(),
    var dayRate: Float = 120f
) {

    fun clearKeyframes() { timeMap.clear() }

    fun addKeyframe(newTime: Float, key: WLParamKey): Boolean {
        val t = newTime.coerceAtLeast(0f)
        if (timeMap.containsKey(t)) return false
        timeMap[t] = key
        return true
    }

    fun removeKeyframe(time: Float): Boolean = timeMap.remove(time) != null

    fun changeKeyframeTime(oldTime: Float, newTime: Float): Boolean {
        val key = timeMap[oldTime] ?: return false
        if (!removeKeyframe(oldTime)) return false
        return addKeyframe(newTime, key)
    }

    fun changeKeyframeParam(time: Float, key: WLParamKey): Boolean {
        if (!timeMap.containsKey(time)) return false
        timeMap[time] = key
        return true
    }

    fun getKeytime(frame: WLParamKey): Float? =
        timeMap.entries.firstOrNull { it.value == frame }?.key

    fun hasReferencesTo(frame: WLParamKey): Boolean = getKeytime(frame) != null

    fun removeReferencesTo(frame: WLParamKey) {
        while (true) {
            val t = getKeytime(frame) ?: return
            timeMap.remove(t)
        }
    }

    fun getKeyedParamName(time: Float): String? = timeMap[time]?.name

    fun getSkyMap(): Boolean = true

    fun getSettingsAtTime(fraction: Float): WLParamKey? {
        if (timeMap.isEmpty()) return null
        val t = fraction.coerceIn(0f, 1f)

        val sortedEntries = timeMap.entries.sortedBy { it.key }

        val beforeEntry = sortedEntries.lastOrNull { it.key <= t }
            ?: sortedEntries.last()
        val afterEntry  = sortedEntries.firstOrNull { it.key > t }
            ?: sortedEntries.first()

        if (beforeEntry.key == afterEntry.key) return beforeEntry.value

        val spanLength = if (afterEntry.key > beforeEntry.key)
            afterEntry.key - beforeEntry.key
        else
            1f - beforeEntry.key + afterEntry.key

        val offset = if (t >= beforeEntry.key) t - beforeEntry.key else 1f - beforeEntry.key + t
        val blend = if (spanLength > 0f) offset / spanLength else 0f

        return if (blend < 0.5f) beforeEntry.value else afterEntry.value
    }

    fun loadDayCycleFromFile(fileName: String) {
        System.err.println("WLDayCycle: loadDayCycleFromFile not yet implemented")
    }

    fun saveDayCycle(fileName: String) {
        System.err.println("WLDayCycle: saveDayCycle not yet implemented")
    }

    companion object {
        fun loadCycleDataFromFile(fileName: String): Map<Float, String> {
            return emptyMap()
        }
    }
}
