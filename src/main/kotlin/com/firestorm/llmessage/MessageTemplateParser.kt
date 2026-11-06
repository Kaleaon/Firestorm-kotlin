package com.firestorm.llmessage

import java.io.InputStream
import java.io.InputStreamReader

object MessageTemplateParser {

    @JvmStatic
    fun parseStream(inputStream: InputStream): List<MessageTemplate> {
        val text = InputStreamReader(inputStream, Charsets.UTF_8).use { it.readText() }
        return parseText(text)
    }

    @JvmStatic
    fun parseText(text: String): List<MessageTemplate> {
        val tokens = tokenize(text)
        val templates = mutableListOf<MessageTemplate>()
        var idx = 0

        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token == "{") {
                idx++
                if (idx >= tokens.size) break
                val msgName = tokens[idx++]
                if (idx >= tokens.size) break
                val freqStr = tokens[idx++]
                if (idx >= tokens.size) break
                val numStr = tokens[idx++]
                if (idx >= tokens.size) break
                val trustStr = tokens[idx++]
                if (idx >= tokens.size) break
                val encodingStr = tokens[idx++]

                val frequency = when (freqStr.uppercase()) {
                    "HIGH" -> Frequency.HIGH
                    "MEDIUM" -> Frequency.MEDIUM
                    "LOW" -> Frequency.LOW
                    "FIXED" -> Frequency.FIXED
                    else -> Frequency.LOW
                }

                val trust = if (trustStr.equals("Trusted", ignoreCase = true)) Trust.TRUST else Trust.NOTRUST
                val encoding = if (encodingStr.equals("Zerocoded", ignoreCase = true)) Encoding.ZEROCODED else Encoding.UNENCODED

                var deprecation = Deprecation.NOT_DEPRECATED
                while (idx < tokens.size && tokens[idx] != "{" && tokens[idx] != "}") {
                    val flag = tokens[idx++]
                    if (flag.equals("UDPDeprecated", ignoreCase = true) || flag.equals("Deprecated", ignoreCase = true)) {
                        deprecation = Deprecation.DEPRECATED
                    } else if (flag.equals("UDPBlackListed", ignoreCase = true)) {
                        deprecation = Deprecation.UDP_BLACKLISTED
                    }
                }

                val parsedNum = parseNumber(numStr)
                val template = MessageTemplate(msgName, frequency, parsedNum.toUInt(), trust, encoding, deprecation)

                while (idx < tokens.size && tokens[idx] == "{") {
                    idx++ // Consume '{' for block
                    if (idx >= tokens.size) break
                    val blockName = tokens[idx++]
                    if (idx >= tokens.size) break
                    val repeatStr = tokens[idx++]

                    val blockType = when (repeatStr.uppercase()) {
                        "SINGLE" -> MessageBlock.BlockType.SINGLE
                        "MULTIPLE" -> MessageBlock.BlockType.MULTIPLE
                        "VARIABLE" -> MessageBlock.BlockType.VARIABLE
                        else -> MessageBlock.BlockType.SINGLE
                    }

                    var count = 1
                    if (blockType == MessageBlock.BlockType.MULTIPLE) {
                        if (idx < tokens.size && tokens[idx] != "{" && tokens[idx] != "}") {
                            count = tokens[idx++].toIntOrNull() ?: 1
                        }
                    }

                    val block = MessageBlock(blockName, blockType, count)

                    while (idx < tokens.size && tokens[idx] == "{") {
                        idx++ // Consume '{' for field
                        if (idx >= tokens.size) break
                        val varName = tokens[idx++]
                        if (idx >= tokens.size) break
                        val varTypeStr = tokens[idx++]

                        val varType = parseVarType(varTypeStr)
                        var varSize = getVarTypeDefaultSize(varType)

                        if (varType == VarType.VARIABLE || varType == VarType.FIXED) {
                            if (idx < tokens.size && tokens[idx] != "}") {
                                val possibleSize = tokens[idx].toIntOrNull()
                                if (possibleSize != null) {
                                    varSize = possibleSize
                                    idx++
                                }
                            }
                        }

                        while (idx < tokens.size && tokens[idx] != "}") {
                            idx++
                        }
                        if (idx < tokens.size && tokens[idx] == "}") {
                            idx++
                        }

                        block.addVariable(varName, varType, varSize)
                    }

                    if (idx < tokens.size && tokens[idx] == "}") {
                        idx++
                    }

                    template.addBlock(block)
                }

                if (idx < tokens.size && tokens[idx] == "}") {
                    idx++
                }

                templates.add(template)
            } else {
                idx++
            }
        }

        return templates
    }

    private fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        for (line in text.lines()) {
            val commentIdx = line.indexOf("//")
            val cleanLine = if (commentIdx >= 0) line.substring(0, commentIdx) else line
            var i = 0
            while (i < cleanLine.length) {
                val c = cleanLine[i]
                if (c == '{' || c == '}') {
                    tokens.add(c.toString())
                    i++
                } else if (c.isWhitespace()) {
                    i++
                } else {
                    val start = i
                    while (i < cleanLine.length && !cleanLine[i].isWhitespace() && cleanLine[i] != '{' && cleanLine[i] != '}') {
                        i++
                    }
                    tokens.add(cleanLine.substring(start, i))
                }
            }
        }
        return tokens
    }

    private fun parseNumber(str: String): Int {
        val s = str.trim()
        return if (s.startsWith("0x", ignoreCase = true)) {
            s.substring(2).toLong(16).toInt()
        } else {
            s.toLongOrNull()?.toInt() ?: s.toIntOrNull() ?: 0
        }
    }

    private fun parseVarType(typeStr: String): VarType {
        return when (typeStr.uppercase()) {
            "U8" -> VarType.U8
            "U16" -> VarType.U16
            "U32" -> VarType.U32
            "U64" -> VarType.U64
            "S8" -> VarType.S8
            "S16" -> VarType.S16
            "S32" -> VarType.S32
            "F32" -> VarType.F32
            "F64" -> VarType.F64
            "LLUUID" -> VarType.LLUUID
            "BOOL" -> VarType.BOOL
            "LLVECTOR3" -> VarType.LLVECTOR3
            "LLVECTOR3D" -> VarType.LLVECTOR3D
            "LLVECTOR4" -> VarType.LLVECTOR4
            "LLQUATERNION" -> VarType.LLQUATERNION
            "IPADDR" -> VarType.IPADDR
            "IPPORT" -> VarType.IPPORT
            "VARIABLE", "VARIABLE1", "VARIABLE2" -> VarType.VARIABLE
            "FIXED" -> VarType.FIXED
            else -> VarType.VARIABLE
        }
    }

    private fun getVarTypeDefaultSize(type: VarType): Int {
        return when (type) {
            VarType.U8, VarType.S8, VarType.BOOL -> 1
            VarType.U16, VarType.S16, VarType.IPPORT -> 2
            VarType.U32, VarType.S32, VarType.F32, VarType.IPADDR -> 4
            VarType.U64, VarType.F64 -> 8
            VarType.LLVECTOR3, VarType.LLQUATERNION -> 12
            VarType.LLVECTOR4, VarType.LLUUID -> 16
            VarType.LLVECTOR3D -> 24
            VarType.VARIABLE -> 1
            VarType.FIXED -> 1
        }
    }
}
