package com.firestorm.llui

import java.io.File
import java.io.FileReader
import java.io.FileWriter

enum class ConditionType(val value: Int) {
    CONTAINS(0), MATCHES(1), STARTS_WITH(2), ENDS_WITH(3)
}

enum class HighlightType(val value: Int) {
    PART(0), ALL(1)
}

enum class HighlightPosition { WHOLE, START, MIDDLE, END }

enum class DialogAction { ACTION_NONE, ACTION_CLOSE, ACTION_ADD, ACTION_COPY, ACTION_UPDATE }

data class Highlight(
    val pattern: String,
    val condition: Int,
    val highlight: Int,
    val caseSensitive: Boolean,
    val color: Color4
)

typealias ParserOutput = List<Pair<String, Color4>>

object TextParser {

    var highlights: MutableList<Highlight> = mutableListOf()
    var loaded: Boolean = false

    private fun findPattern(text: String, highlight: Highlight): Int {
        val ltext = if (highlight.caseSensitive) text else text.lowercase()
        val pattern = if (highlight.caseSensitive) highlight.pattern else highlight.pattern.lowercase()

        return when (highlight.condition) {
            ConditionType.CONTAINS.value    -> ltext.indexOf(pattern)
            ConditionType.MATCHES.value     -> if (ltext == pattern) 0 else -1
            ConditionType.STARTS_WITH.value -> if (ltext.startsWith(pattern)) 0 else -1
            ConditionType.ENDS_WITH.value   -> {
                val pos = ltext.lastIndexOf(pattern)
                if (pos >= 0 && (ltext.length - pattern.length) == pos) pos else -1
            }
            else -> -1
        }
    }

    fun parsePartialLineHighlights(
        text: String,
        color: Color4,
        part: HighlightPosition = HighlightPosition.WHOLE,
        index: Int = 0
    ): ParserOutput {
        loadKeywords()

        for (i in index until highlights.size) {
            val h = highlights[i]
            val condition = h.condition
            if (h.highlight == HighlightType.PART.value && condition != ConditionType.MATCHES.value) {
                val applies = (condition == ConditionType.STARTS_WITH.value && part == HighlightPosition.START) ||
                              (condition == ConditionType.ENDS_WITH.value   && part == HighlightPosition.END)   ||
                               condition == ConditionType.CONTAINS.value    || part == HighlightPosition.WHOLE

                if (applies) {
                    val start = findPattern(text, h)
                    if (start >= 0) {
                        val end = h.pattern.length
                        val len = text.length
                        val hColor = h.color

                        if (start == 0) {
                            val startVec = listOf(Pair(text.substring(0, end), hColor))
                            val endVec = if (end < len) {
                                val newPart = if (part == HighlightPosition.END || part == HighlightPosition.WHOLE)
                                    HighlightPosition.END else HighlightPosition.MIDDLE
                                parsePartialLineHighlights(text.substring(end), color, newPart, i)
                            } else emptyList()
                            return startVec + endVec
                        } else {
                            val newPartStart = if (part == HighlightPosition.START || part == HighlightPosition.WHOLE)
                                HighlightPosition.START else HighlightPosition.MIDDLE
                            val startVec = parsePartialLineHighlights(text.substring(0, start), color, newPartStart, i + 1)

                            val (middleVec, endVec) = if (end < len) {
                                val mid = listOf(Pair(text.substring(start, start + end), hColor))
                                val newPartEnd = if (part == HighlightPosition.END || part == HighlightPosition.WHOLE)
                                    HighlightPosition.END else HighlightPosition.MIDDLE
                                mid to parsePartialLineHighlights(text.substring(start + end), color, newPartEnd, i)
                            } else {
                                emptyList<Pair<String, Color4>>() to listOf(Pair(text.substring(start, start + end), hColor))
                            }
                            return startVec + middleVec + endVec
                        }
                    }
                }
            }
        }

        return listOf(Pair(text, color))
    }

    fun parseFullLineHighlights(text: String, color: Color4): Pair<Boolean, Color4> {
        loadKeywords()
        for (h in highlights) {
            if (h.highlight == HighlightType.ALL.value || h.condition == ConditionType.MATCHES.value) {
                if (findPattern(text, h) >= 0) {
                    return Pair(true, h.color)
                }
            }
        }
        return Pair(false, color)
    }

    private fun getFileName(): String {
        TODO("APR: use JVM equivalent of gDirUtilp->getExpandedFilename(LL_PATH_PER_SL_ACCOUNT, \"highlights.xml\")")
    }

    private fun loadKeywords() {
        if (loaded) return
        try {
            val filename = getFileName()
            val file = File(filename)
            if (file.exists()) {
                TODO("APR: deserialize highlights XML from $filename into highlights list")
            }
            loaded = true
        } catch (e: Exception) {
            loaded = true
        }
    }

    fun saveToDisk(newHighlights: MutableList<Highlight>): Boolean {
        highlights = newHighlights
        val filename = try {
            getFileName()
        } catch (e: NotImplementedError) {
            return false
        }
        if (filename.isEmpty()) return false
        TODO("APR: serialize highlights to pretty XML at $filename")
    }
}
