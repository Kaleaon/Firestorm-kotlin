/**
 * DirIterator.kt
 * Directory iteration with glob pattern matching — converted from
 * lldiriterator.h / lldiriterator.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * LGPL v2.1
 *
 * Glob wildcards supported (identical to the C++ implementation):
 *   *          zero or more characters (but not a leading dot)
 *   ?          exactly one character
 *   [abcde]    exactly one listed character
 *   [a-e]      exactly one character in range
 *   [!abcde]   any character NOT listed
 *   {abc,xyz}  exactly one of the alternatives
 */

package com.firestorm.llfilesystem

import com.firestorm.llcommon.*
import java.io.File

/**
 * Iterates over entries in [path] whose names match the glob [pattern].
 *
 * Usage:
 * ```kotlin
 * val iter = DirIterator("/some/dir", "*.txt")
 * val name = StringBuilder()
 * while (iter.next(name)) {
 *     println(name)
 * }
 * ```
 *
 * [close] resets the iterator so it can be re-used from the beginning.
 */
class DirIterator(
    private val path: String,
    private val pattern: String = "*"
) {
    private val regex: Regex? = globToRegex(pattern)
    private var entries: Iterator<File>? = buildIterator()
    private var valid: Boolean = regex != null && entries != null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Advance to the next matching entry.
     *
     * @param name Populated with the name (not full path) of the matched entry.
     * @return `true` if a match was found, `false` when the directory is exhausted.
     */
    fun next(name: StringBuilder): Boolean {
        name.clear()
        if (!valid) return false

        val iter = entries ?: return false
        while (iter.hasNext()) {
            val file = iter.next()
            val entryName = file.name
            if (regex!!.matches(entryName)) {
                name.append(entryName)
                return true
            }
        }
        return false
    }

    /** Reset the iterator back to the beginning of the directory. */
    fun close() {
        entries = buildIterator()
        valid = regex != null && entries != null
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun buildIterator(): Iterator<File>? {
        val dir = File(path)
        if (!dir.isDirectory) return null
        val files = dir.listFiles() ?: return null
        return files.iterator()
    }

    // ── Companion: glob → Regex conversion ───────────────────────────────────

    companion object {

        /**
         * Convert a glob expression to a [Regex], replicating the logic of the
         * C++ `glob_to_regex()` function in lldiriterator.cpp.
         *
         * Returns `null` if the glob expression is invalid (unmatched braces).
         */
        fun globToRegex(glob: String): Regex? {
            val sb = StringBuilder()
            var braces = 0
            var squareBraceOpen = false

            for ((idx, c) in glob.withIndex()) {
                when (c) {
                    '*'  -> {
                        // A leading '*' must not match a dot (hidden-file convention).
                        sb.append(if (idx == 0) "[^.].*" else ".*")
                    }
                    '?'  -> sb.append('.')
                    '{'  -> { braces++; sb.append('(') }
                    '}'  -> {
                        if (braces == 0) return null  // unmatched closing brace
                        sb.append(')'); braces--
                    }
                    ','  -> sb.append(if (braces > 0) '|' else ',')
                    '!'  -> sb.append(if (squareBraceOpen) '^' else '!')
                    // Characters that are special in regex but NOT in glob:
                    '.', '^', '(', ')', '+', '|', '$' -> sb.append('\\').append(c)
                    else -> sb.append(c)
                }
                squareBraceOpen = (c == '[')
            }

            if (braces != 0) return null  // unterminated brace expression

            return Regex(sb.toString())
        }
    }
}
