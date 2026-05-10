package com.firestorm.llui

import java.awt.Color

enum class TokenType {
    UNKNOWN,
    WORD,
    LINE,
    TWO_SIDED_DELIMITER,
    ONE_SIDED_DELIMITER,
    DOUBLE_QUOTATION_MARKS,
    CONSTANT,
    CONTROL,
    EVENT,
    FUNCTION,
    LABEL,
    SECTION,
    TYPE
}

class KeywordToken(
    val type: TokenType,
    val color: UIColor,
    val token: String,
    val toolTip: String,
    val delimiter: String
) {
    fun getLengthHead(): Int = token.length
    fun getLengthTail(): Int = delimiter.length

    fun isHead(s: String, offset: Int): Boolean {
        if (offset + token.length > s.length) return false
        return s.substring(offset, offset + token.length) == token
    }

    fun isTail(s: String, offset: Int): Boolean {
        if (delimiter.isEmpty()) return false
        if (offset + delimiter.length > s.length) return false
        return s.substring(offset, offset + delimiter.length) == delimiter
    }
}

data class TextSegment(
    var start: Int,
    var end: Int,
    val color: UIColor,
    val style: Any? = null,
    var token: KeywordToken? = null
)

class Keywords {
    private var loaded: Boolean = false
    private var syntax: Map<String, Any> = emptyMap()
    private val wordTokenMap: MutableMap<String, KeywordToken> = mutableMapOf()
    private val lineTokenList: ArrayDeque<KeywordToken> = ArrayDeque()
    private val delimiterTokenList: ArrayDeque<KeywordToken> = ArrayDeque()
    private val colorGroupMap: MutableMap<String, UIColor> = mutableMapOf()
    private val attributes: MutableMap<String, String> = mutableMapOf()

    fun clearLoaded() { loaded = false }
    fun isLoaded(): Boolean = loaded

    fun getColorGroup(keyIn: String): UIColor {
        val groupName = when (keyIn) {
            "functions" -> "SyntaxLslFunction"
            "controls" -> "SyntaxLslControlFlow"
            "events" -> "SyntaxLslEvent"
            "types" -> "SyntaxLslDataType"
            "misc-flow-label" -> "SyntaxLslControlFlow"
            "deprecated" -> "SyntaxLslDeprecated"
            "god-mode" -> "SyntaxLslGodMode"
            "constants" -> "SyntaxLslConstant"
            "constants-integer" -> "SyntaxLslIntegerConstant"
            "constants-float" -> "SyntaxLslFloatConstant"
            "constants-string", "constants-key" -> "SyntaxLslStringConstant"
            "constants-rotation", "constants-vector" -> "SyntaxLslCompoundConstant"
            else -> "ScriptText"
        }
        return colorGroupMap[groupName] ?: UIColor(1f, 1f, 1f)
    }

    fun initialize(syntaxData: Map<String, Any>) {
        syntax = syntaxData
        loaded = true
    }

    fun processTokens() {
        if (!loaded) return

        val noDelim = ""
        addToken(TokenType.LABEL, "@", getColorGroup("misc-flow-label"), "Label\nTarget for jump statement", noDelim)
        addToken(TokenType.ONE_SIDED_DELIMITER, "//", UIColor(0.5f, 0.5f, 0.5f), "Comment (single-line)\nNon-functional commentary or disabled code", noDelim)
        addToken(TokenType.TWO_SIDED_DELIMITER, "/*", UIColor(0.5f, 0.5f, 0.5f), "Comment (multi-line)\nNon-functional commentary or disabled code", "*/")
        addToken(TokenType.DOUBLE_QUOTATION_MARKS, "\"", UIColor(0.8f, 0.4f, 0.4f), "String literal", "\"")

        for ((key, value) in syntax) {
            if (key == "llsd-lsl-syntax-version") continue
            @Suppress("UNCHECKED_CAST")
            val groupMap = value as? Map<String, Any> ?: continue
            processTokensGroup(groupMap, key)
        }
    }

    fun addToken(
        type: TokenType,
        key: String,
        color: UIColor,
        toolTip: String = "[no info]",
        delimiter: String = ""
    ) {
        var tip = toolTip.replace("\\n", "\n").replace("\t", " ")
        if (tip.isEmpty()) tip = "[no info]"

        val token = KeywordToken(type, color, key, tip, delimiter)
        when (type) {
            TokenType.CONSTANT,
            TokenType.CONTROL,
            TokenType.EVENT,
            TokenType.FUNCTION,
            TokenType.LABEL,
            TokenType.SECTION,
            TokenType.TYPE,
            TokenType.WORD -> wordTokenMap[key] = token

            TokenType.LINE -> lineTokenList.addFirst(token)

            TokenType.TWO_SIDED_DELIMITER,
            TokenType.DOUBLE_QUOTATION_MARKS,
            TokenType.ONE_SIDED_DELIMITER -> delimiterTokenList.addFirst(token)

            TokenType.UNKNOWN -> {}
        }
    }

    fun findSegments(
        segList: MutableList<TextSegment>,
        wtext: String,
        defaultColor: UIColor
    ) {
        segList.clear()
        if (wtext.isEmpty()) return

        val textLen = wtext.length + 1
        segList.add(TextSegment(0, textLen, defaultColor))

        var cur = 0
        val base = 0

        while (cur < wtext.length) {
            val ch = wtext[cur]
            if (ch == '\n' || cur == base) {
                if (ch == '\n') {
                    val lineBreak = TextSegment(cur, cur + 1, defaultColor)
                    lineBreak.token = null
                    insertSegment(segList, lineBreak, textLen, defaultColor)
                    cur++
                    if (cur >= wtext.length || wtext[cur] == '\n') continue
                }

                while (cur < wtext.length && wtext[cur].isWhitespace() && wtext[cur] != '\n') cur++
                if (cur >= wtext.length || wtext[cur] == '\n') continue

                var lineDone = false
                for (lineToken in lineTokenList) {
                    if (lineToken.isHead(wtext, cur)) {
                        val segStart = cur
                        while (cur < wtext.length && wtext[cur] != '\n') cur++
                        val segEnd = cur
                        insertSegments(wtext, segList, lineToken, textLen, segStart, segEnd, defaultColor)
                        lineDone = true
                        break
                    }
                }
                if (lineDone) continue
            }

            while (cur < wtext.length && wtext[cur].isWhitespace() && wtext[cur] != '\n') cur++

            while (cur < wtext.length && wtext[cur] != '\n') {
                var curDelimiter: KeywordToken? = null
                for (delim in delimiterTokenList) {
                    if (delim.isHead(wtext, cur)) {
                        curDelimiter = delim
                        break
                    }
                }

                if (curDelimiter != null) {
                    val segStart = cur
                    cur += curDelimiter.getLengthHead()
                    var betweenDelimiters = 0
                    val segEnd: Int

                    val dtype = curDelimiter.type
                    if (dtype == TokenType.TWO_SIDED_DELIMITER || dtype == TokenType.DOUBLE_QUOTATION_MARKS) {
                        while (cur < wtext.length && !curDelimiter.isTail(wtext, cur)) {
                            if (dtype == TokenType.DOUBLE_QUOTATION_MARKS && wtext[cur] == '\\') {
                                var numBackslashes = 0
                                while (cur < wtext.length && wtext[cur] == '\\') {
                                    numBackslashes++
                                    betweenDelimiters++
                                    cur++
                                }
                                if (curDelimiter.isTail(wtext, cur)) {
                                    if (numBackslashes % 2 == 1) {
                                        betweenDelimiters++
                                        cur++
                                    } else {
                                        break
                                    }
                                }
                            } else {
                                betweenDelimiters++
                                cur++
                            }
                        }
                        if (cur < wtext.length) {
                            cur += curDelimiter.getLengthHead()
                            segEnd = segStart + betweenDelimiters + curDelimiter.getLengthHead() + curDelimiter.getLengthTail()
                        } else {
                            segEnd = segStart + betweenDelimiters + curDelimiter.getLengthHead()
                        }
                    } else {
                        while (cur < wtext.length && wtext[cur] != '\n') {
                            betweenDelimiters++
                            cur++
                        }
                        segEnd = segStart + betweenDelimiters + curDelimiter.getLengthHead()
                    }

                    insertSegments(wtext, segList, curDelimiter, textLen, segStart, segEnd, defaultColor)
                    continue
                }

                val prev = if (cur > 0) wtext[cur - 1] else ' '
                if (!prev.isLetterOrDigit() && prev != '_' && prev != '#') {
                    var p = cur
                    while (p < wtext.length && (wtext[p].isLetterOrDigit() || wtext[p] == '_' || wtext[p] == '#')) p++
                    val segLen = p - cur
                    if (segLen > 0) {
                        val word = wtext.substring(cur, cur + segLen)
                        val curToken = wordTokenMap[word]
                        if (curToken != null) {
                            val segStart = cur
                            val segEnd = segStart + segLen
                            insertSegments(wtext, segList, curToken, textLen, segStart, segEnd, defaultColor)
                        }
                        cur += segLen
                        continue
                    }
                }

                if (cur < wtext.length && wtext[cur] != '\n') cur++
            }
        }
    }

    private fun insertSegments(
        wtext: String,
        segList: MutableList<TextSegment>,
        token: KeywordToken,
        textLen: Int,
        segStart: Int,
        segEnd: Int,
        defaultColor: UIColor
    ) {
        var pos = wtext.indexOf('\n', segStart)
        var start = segStart

        while (pos != -1 && pos < segEnd) {
            if (pos != start) {
                val seg = TextSegment(start, pos, token.color)
                seg.token = token
                insertSegment(segList, seg, textLen, defaultColor)
            }
            val lineBreak = TextSegment(pos, pos + 1, token.color)
            lineBreak.token = token
            insertSegment(segList, lineBreak, textLen, defaultColor)
            start = pos + 1
            pos = wtext.indexOf('\n', start)
        }

        val finalSeg = TextSegment(start, segEnd, token.color)
        finalSeg.token = token
        insertSegment(segList, finalSeg, textLen, defaultColor)
    }

    private fun insertSegment(
        segList: MutableList<TextSegment>,
        newSegment: TextSegment,
        textLen: Int,
        defaultColor: UIColor
    ) {
        val last = segList.last()
        val newSegEnd = newSegment.end

        if (newSegment.start == last.start) {
            segList.removeLast()
        } else {
            last.end = newSegment.start
        }
        segList.add(newSegment)

        if (newSegEnd < textLen) {
            segList.add(TextSegment(newSegEnd, textLen, defaultColor))
        }
    }

    private fun getArguments(arguments: Any?): String {
        if (arguments !is List<*>) return ""
        val sb = StringBuilder()
        val argsCount = arguments.size
        arguments.forEachIndexed { idx, arg ->
            @Suppress("UNCHECKED_CAST")
            val argMap = arg as? Map<String, Any> ?: return@forEachIndexed
            for ((name, details) in argMap) {
                @Suppress("UNCHECKED_CAST")
                val detailMap = details as? Map<String, Any>
                sb.append("${detailMap?.get("type") ?: ""} $name")
                if (idx < argsCount - 1) sb.append(", ")
            }
        }
        return sb.toString()
    }

    private fun getAttribute(key: String): String = attributes[key] ?: ""

    private fun processTokensGroup(tokens: Map<String, Any>, group: String) {
        val colorDeprecated = getColorGroup("deprecated")
        val colorGodMode = getColorGroup("god-mode")

        val tokenType = when (group) {
            "constants" -> TokenType.CONSTANT
            "controls" -> TokenType.CONTROL
            "events" -> TokenType.EVENT
            "functions" -> TokenType.FUNCTION
            "label" -> TokenType.LABEL
            "types" -> TokenType.TYPE
            else -> TokenType.UNKNOWN
        }

        val colorGroup = getColorGroup(group)

        for ((tokenName, tokenData) in tokens) {
            @Suppress("UNCHECKED_CAST")
            val tokenMap = tokenData as? Map<String, Any> ?: continue
            attributes.clear()
            var arguments: Any? = null

            for ((k, v) in tokenMap) {
                when {
                    k == "arguments" && v is List<*> -> arguments = v
                    v !is Map<*, *> && v !is List<*> -> attributes[k] = v.toString()
                }
            }

            var effectiveColorGroup = colorGroup
            var tooltip = ""
            when (tokenType) {
                TokenType.CONSTANT -> {
                    val attrType = getAttribute("type")
                    effectiveColorGroup = if (attrType.isNotEmpty()) {
                        getColorGroup("$group-$attrType")
                    } else {
                        getColorGroup(group)
                    }
                    tooltip = "Type: ${getAttribute("type")}, Value: ${getAttribute("value")}"
                }
                TokenType.EVENT -> {
                    tooltip = "$tokenName(${getArguments(arguments)})"
                }
                TokenType.FUNCTION -> {
                    tooltip = "${getAttribute("return")} $tokenName(${getArguments(arguments)});"
                    tooltip += "\nEnergy: ${getAttribute("energy").ifEmpty { "0.0" }}"
                    val sleep = getAttribute("sleep")
                    if (sleep.isNotEmpty()) tooltip += ", Sleep: $sleep"
                }
                else -> {}
            }

            val customTooltip = getAttribute("tooltip")
            if (customTooltip.isNotEmpty()) {
                if (tooltip.isNotEmpty()) tooltip += "\n"
                tooltip += customTooltip
            }

            var color = if (getAttribute("deprecated") == "true") colorDeprecated else effectiveColorGroup
            if (getAttribute("god-mode") == "true") color = colorGodMode

            addToken(tokenType, tokenName, color, tooltip)
        }
    }

    fun loadFromLegacyFile(filename: String): Boolean {
        TODO("APR: use JVM equivalent for LLUICtrlFactory::getLayeredXMLNode to load $filename")
    }

    val wordTokens: Map<String, KeywordToken> get() = wordTokenMap
}
