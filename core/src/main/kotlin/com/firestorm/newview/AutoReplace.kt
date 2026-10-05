/**
 * AutoReplace.kt
 * Chat auto-replace / autocorrect manager — Kotlin port of llautoreplace.h / llautoreplace.cpp
 *
 * Original: Copyright (C) 2012 Linden Research, Inc.
 * Ported to Kotlin for the Firestorm viewer project.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1.
 */

package com.firestorm.newview

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

/**
 * A named, ordered set of keyword→replacement pairs.
 *
 * Mirrors the per-list structure stored in [LLAutoReplaceSettings::mLists]
 * in the C++ code (the LLSD array element with "name" and "replacements"
 * keys).
 *
 * @property name         Display name of the list (must be unique).
 * @property enabled      Whether this list participates in lookups.
 * @property replacements Map of keyword → replacement string.  Order of
 *                        iteration determines which keyword wins when a
 *                        match is found; insertion order is preserved by
 *                        [LinkedHashMap].
 */
data class AutoReplaceList(
    val name: String,
    var enabled: Boolean = true,
    val replacements: MutableMap<String, String> = mutableMapOf()
)

/** Result codes returned by list-add operations (matches C++ [AddListResult]). */
enum class AddListResult {
    OK,
    DUPLICATE_NAME,
    INVALID_LIST
}

// ---------------------------------------------------------------------------
// AutoReplace singleton
// ---------------------------------------------------------------------------

/**
 * Provides auto-replace / autocorrect functionality for chat text entry.
 *
 * When the user finishes typing a word (followed by whitespace or
 * punctuation), [replaceWord] is consulted.  The lists are searched in
 * priority order; the first match wins.
 *
 * Persistence ([load]/[save]) is stubbed — wire to the viewer's user-settings
 * directory before use.
 *
 * This singleton collapses the C++ [LLAutoReplace] + [LLAutoReplaceSettings]
 * pair; the settings are embedded directly since the Kotlin port does not
 * need a separate, copy-able settings object for the preferences floater (that
 * UI layer will carry its own snapshot).
 */
object AutoReplace {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /**
     * Ordered list of replacement lists.  Priority runs from index 0
     * (highest) to the end (lowest), matching the C++ LLSD array ordering.
     */
    val lists: MutableList<AutoReplaceList> = mutableListOf()

    /** Master enable switch; when false [replaceWord] is a no-op. */
    var enabled: Boolean = true

    // -----------------------------------------------------------------------
    // List management
    // -----------------------------------------------------------------------

    /**
     * Appends a new, empty list with the given name.
     *
     * @return [AddListResult.DUPLICATE_NAME] if [name] is already in use,
     *         [AddListResult.OK] otherwise.
     */
    fun addList(name: String): AddListResult {
        if (lists.any { it.name == name }) return AddListResult.DUPLICATE_NAME
        lists += AutoReplaceList(name = name)
        return AddListResult.OK
    }

    /**
     * Removes the list with the given name.
     *
     * @return true if a list was removed, false if not found.
     */
    fun removeList(name: String): Boolean =
        lists.removeIf { it.name == name }

    /**
     * Moves the named list one step toward higher priority (lower index).
     *
     * @return false if the list was not found or is already first.
     */
    fun increaseListPriority(name: String): Boolean {
        val idx = lists.indexOfFirst { it.name == name }
        if (idx <= 0) return idx == 0  // 0 means already first, -1 means not found
        val tmp = lists[idx - 1]; lists[idx - 1] = lists[idx]; lists[idx] = tmp
        return true
    }

    /**
     * Moves the named list one step toward lower priority (higher index).
     *
     * @return false if the list was not found or is already last.
     */
    fun decreaseListPriority(name: String): Boolean {
        val idx = lists.indexOfFirst { it.name == name }
        if (idx < 0 || idx == lists.lastIndex) return false
        val tmp = lists[idx + 1]; lists[idx + 1] = lists[idx]; lists[idx] = tmp
        return true
    }

    // -----------------------------------------------------------------------
    // Replacement entry management
    // -----------------------------------------------------------------------

    /**
     * Adds or overwrites a keyword→replacement pair in the named list.
     *
     * @return false if the list does not exist.
     */
    fun addReplacement(listName: String, from: String, to: String): Boolean {
        val list = lists.find { it.name == listName } ?: return false
        list.replacements[from] = to
        return true
    }

    /**
     * Removes a keyword from the named list.
     *
     * @return false if the list does not exist or the keyword was not found.
     */
    fun removeReplacement(listName: String, keyword: String): Boolean {
        val list = lists.find { it.name == listName } ?: return false
        return list.replacements.remove(keyword) != null
    }

    /**
     * Returns the replacements map for the named list, or null.
     * Read-only view — callers should use [addReplacement]/[removeReplacement]
     * for mutations.
     */
    fun getListEntries(listName: String): Map<String, String>? =
        lists.find { it.name == listName }?.replacements

    // -----------------------------------------------------------------------
    // Core replacement logic
    // -----------------------------------------------------------------------

    /**
     * Looks up [word] in each enabled list in priority order.
     *
     * Mirrors [LLAutoReplaceSettings::replaceWord] from the C++ source.
     *
     * @return The replacement string if a match is found, otherwise [word].
     */
    fun replaceWord(word: String): String {
        if (!enabled || word.isEmpty()) return word
        for (list in lists) {
            if (!list.enabled) continue
            val replacement = list.replacements[word]
            if (replacement != null) return replacement
        }
        return word
    }

    /**
     * Simulates the C++ [autoreplaceCallback] logic: given the full input
     * text and the current cursor position (just past a word boundary),
     * returns the replacement triple or null if no replacement applies.
     *
     * @param inputText  Current contents of the text field (UTF-8).
     * @param cursorPos  Cursor position (codepoint index) in [inputText].
     * @return [Triple(start, length, replacement)] or null.
     */
    fun autoreplaceCallback(inputText: String, cursorPos: Int): Triple<Int, Int, String>? {
        if (!enabled || cursorPos == 0) return null
        var wordEnd = cursorPos - 1
        val atSpace = wordEnd < inputText.length && inputText[wordEnd] == ' '
        if (atSpace && wordEnd > 0) wordEnd--
        if (wordEnd < 0 || wordEnd >= inputText.length) return null
        if (!inputText[wordEnd].isLetterOrDigit() && inputText[wordEnd] != '_') return null

        var wordStart = wordEnd
        while (wordStart > 0 && (inputText[wordStart - 1].isLetterOrDigit() || inputText[wordStart - 1] == '_')) {
            wordStart--
        }
        val lastWord = inputText.substring(wordStart, wordEnd + 1)
        val replacement = replaceWord(lastWord)
        return if (replacement != lastWord && atSpace) {
            Triple(wordStart, wordEnd - wordStart + 1, replacement)
        } else null
    }

    // -----------------------------------------------------------------------
    // Persistence (stubbed)
    // -----------------------------------------------------------------------

    /**
     * Loads auto-replace lists from the user's settings file.
     * Wire to the viewer's settings/file system before use.
     */
    fun load() {
        System.err.println("AutoReplace: load not yet implemented")
    }

    /**
     * Saves the current lists to the user's settings file.
     * Wire to the viewer's settings/file system before use.
     */
    fun save() {
        System.err.println("AutoReplace: save not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /** Returns all list names in priority order. */
    fun listNames(): List<String> = lists.map { it.name }
}
