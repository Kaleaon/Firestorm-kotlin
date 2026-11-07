package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID

// Key / modifier-mask typedefs (thin wrappers matching C++ KEY/MASK)
typealias Key = Byte
typealias Mask = UInt

const val KEY_NONE: Key = 0
const val MASK_NONE: Mask = 0u

/**
 * LLGesture — a single gesture entry combining a trigger key/phrase,
 * an optional sound, an animation name, and an output chat string.
 *
 * Translated from llgesture.h / llgesture.cpp.
 */
open class Gesture(
    var key: Key = KEY_NONE,
    var mask: Mask = MASK_NONE,
    var trigger: String = "",
    var soundItemId: LLUUID = LLUUID.NULL,
    var animation: String = "",
    var outputString: String = ""
) {
    /** Lowercase version of [trigger] for case-insensitive matching. */
    var triggerLower: String = trigger.lowercase()
        private set

    fun setTrigger(t: String) {
        trigger = t
        triggerLower = t.lowercase()
    }

    /** Returns true if this gesture is triggered by the given key+mask combo. */
    open fun triggerByKey(k: Key, m: Mask): Boolean = false   // override in subclass

    /** Returns true if [text] (already lowercased) matches the trigger. */
    open fun triggerByString(text: String): Boolean = false    // override in subclass

    companion object {
        const val MAX_SERIAL_SIZE = 1 + 4 + 16 + 26 + 41 + 41  // matches C++ constant
    }
}

/**
 * LLGestureList — an ordered list of [Gesture] objects.
 * Supports key-based and substring-based triggering.
 *
 * Translated from llgesture.h / llgesture.cpp.
 */
open class GestureList {
    protected val list: MutableList<Gesture> = mutableListOf()

    val count: Int get() = list.size

    open fun get(i: Int): Gesture = list[i]
    open fun put(gesture: Gesture) { list.add(gesture) }
    fun deleteAll() { list.clear() }

    /** Trigger by key+mask; returns true if a gesture matched. */
    fun trigger(key: Key, mask: Mask): Boolean {
        return list.any { it.triggerByKey(key, mask) }
    }

    /**
     * Tokenise [text] on spaces, try to trigger a gesture on the first matching
     * token, and write the (possibly revised) string to [revisedString].
     * Returns true if a gesture was found.
     */
    fun triggerAndReviseString(text: String, revisedString: StringBuilder): Boolean {
        val tokens = text.split(" ").filter { it.isNotEmpty() }
        var foundGesture = false
        val result = StringBuilder()

        for ((idx, token) in tokens.withIndex()) {
            val sep = if (idx > 0) " " else ""
            val lower = token.lowercase()

            if (!foundGesture) {
                val matched = list.firstOrNull { it.triggerByString(lower) }
                if (matched != null) {
                    foundGesture = true
                    val output = matched.outputString
                    if (output.isNotEmpty()) {
                        // Preserve original capitalisation when strings are equal ignoring case
                        result.append(sep)
                        result.append(if (lower == output.lowercase()) token else output)
                    }
                    continue
                }
            }
            result.append(sep).append(token)
        }

        revisedString.clear()
        revisedString.append(result)
        return foundGesture
    }

    companion object {
        const val SERIAL_HEADER_SIZE = 4  // S32 count field
    }
}
