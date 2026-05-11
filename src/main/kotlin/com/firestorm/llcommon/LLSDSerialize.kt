package com.firestorm.llcommon

import java.io.StringReader
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

object LLSDSerialize {

    // ── XML ──────────────────────────────────────────────────────────────────

    fun toXML(sd: LLSD): String = toXML(sd, canonical = false)

    fun toCanonicalXML(sd: LLSD): String = toXML(sd, canonical = true)

    private fun toXML(sd: LLSD, canonical: Boolean): String {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val root = doc.createElement("llsd")
        doc.appendChild(root)
        root.appendChild(xmlElement(doc, sd, canonical))

        val tf = TransformerFactory.newInstance().newTransformer()
        tf.setOutputProperty(OutputKeys.INDENT, "no")
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        val sw = java.io.StringWriter()
        tf.transform(DOMSource(doc), StreamResult(sw))
        return sw.toString()
    }

    private fun xmlElement(doc: Document, sd: LLSD, canonical: Boolean): Element = when (sd) {
        is LLSD.Undefined -> doc.createElement("undef")
        is LLSD.LLSDBoolean -> doc.createElement("boolean").also {
            it.textContent = if (sd.value) "true" else "false"
        }
        is LLSD.LLSDInteger -> doc.createElement("integer").also { it.textContent = sd.value.toString() }
        is LLSD.LLSDReal -> doc.createElement("real").also { it.textContent = sd.value.toString() }
        is LLSD.LLSDString -> if (sd.value.isEmpty()) doc.createElement("string")
        else doc.createElement("string").also { it.textContent = sd.value }
        is LLSD.LLSDUUID -> if (sd.value.isNull()) doc.createElement("uuid")
        else doc.createElement("uuid").also { it.textContent = sd.value.toString() }
        is LLSD.LLSDDate -> doc.createElement("date").also { it.textContent = sd.value.toISOString() }
        is LLSD.LLSDURI -> doc.createElement("uri").also { it.textContent = sd.value.asString() }
        is LLSD.LLSDBinary -> if (sd.value.isEmpty()) doc.createElement("binary")
        else doc.createElement("binary").also {
            it.textContent = Base64.getEncoder().encodeToString(sd.value)
        }
        is LLSD.LLSDMap -> doc.createElement("map").also { el ->
            mapEntries(sd.value, canonical).forEach { (k, v) ->
                el.appendChild(doc.createElement("key").also { it.textContent = k })
                el.appendChild(xmlElement(doc, v, canonical))
            }
        }
        is LLSD.LLSDArray -> doc.createElement("array").also { el ->
            sd.value.forEach { el.appendChild(xmlElement(doc, it, canonical)) }
        }
    }

    fun fromXML(xml: String): LLSD {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))
        val root = doc.documentElement
        val child = firstElementChild(root) ?: return LLSD.Undefined
        return parseXmlNode(child)
    }

    private fun parseXmlNode(el: Element): LLSD = when (el.tagName) {
        "undef" -> LLSD.Undefined
        "boolean" -> LLSD.LLSDBoolean(el.textContent.trim().let { it == "true" || it == "1" })
        "integer" -> LLSD.LLSDInteger(el.textContent.trim().toIntOrNull() ?: 0)
        "real" -> LLSD.LLSDReal(el.textContent.trim().toDoubleOrNull() ?: 0.0)
        "string" -> LLSD.LLSDString(el.textContent)
        "uuid" -> LLSD.LLSDUUID(LLUUID.fromString(el.textContent.trim()) ?: LLUUID.NULL)
        "date" -> LLSD.LLSDDate(LLDate.fromISOString(el.textContent.trim()) ?: LLDate.NULL)
        "uri" -> LLSD.LLSDURI(LLURI.fromString(el.textContent.trim()))
        "binary" -> LLSD.LLSDBinary(
            if (el.textContent.isBlank()) ByteArray(0)
            else Base64.getDecoder().decode(el.textContent.trim())
        )
        "map" -> {
            val map = mutableMapOf<String, LLSD>()
            val nodes = el.childNodes
            var i = 0
            while (i < nodes.length) {
                val node = nodes.item(i)
                if (node is Element && node.tagName == "key") {
                    val key = node.textContent
                    var j = i + 1
                    while (j < nodes.length && nodes.item(j).nodeType != Node.ELEMENT_NODE) j++
                    val valNode = nodes.item(j)
                    if (valNode is Element) {
                        map[key] = parseXmlNode(valNode)
                        i = j + 1
                        continue
                    }
                }
                i++
            }
            LLSD.LLSDMap(map)
        }
        "array" -> {
            val list = mutableListOf<LLSD>()
            val nodes = el.childNodes
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                if (node is Element) list.add(parseXmlNode(node))
            }
            LLSD.LLSDArray(list)
        }
        else -> LLSD.Undefined
    }

    private fun firstElementChild(node: Node): Element? {
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element) return child
        }
        return null
    }

    // ── Notation ─────────────────────────────────────────────────────────────

    fun toNotation(sd: LLSD): String = buildString { appendNotation(sd, canonical = false) }

    fun toCanonicalNotation(sd: LLSD): String = buildString { appendNotation(sd, canonical = true) }

    private fun StringBuilder.appendNotation(sd: LLSD, canonical: Boolean) {
        when (sd) {
            is LLSD.Undefined -> append('!')
            is LLSD.LLSDBoolean -> append(if (sd.value) "true" else "false")
            is LLSD.LLSDInteger -> append('i').append(sd.value)
            is LLSD.LLSDReal -> append('r').append(sd.value)
            is LLSD.LLSDString -> {
                append('\'')
                append(sd.value.replace("\\", "\\\\").replace("'", "\\'"))
                append('\'')
            }
            is LLSD.LLSDUUID -> append("u\"").append(sd.value).append('"')
            is LLSD.LLSDDate -> append("d\"").append(sd.value.toISOString()).append('"')
            is LLSD.LLSDURI -> append("l\"").append(sd.value.asString()).append('"')
            is LLSD.LLSDBinary -> {
                append("b64\"")
                append(Base64.getEncoder().encodeToString(sd.value))
                append('"')
            }
            is LLSD.LLSDMap -> {
                append('{')
                mapEntries(sd.value, canonical).entries.forEachIndexed { idx, (k, v) ->
                    if (idx > 0) append(',')
                    append('\'').append(k.replace("'", "\\'")).append("':")
                    appendNotation(v, canonical)
                }
                append('}')
            }
            is LLSD.LLSDArray -> {
                append('[')
                sd.value.forEachIndexed { idx, v ->
                    if (idx > 0) append(',')
                    appendNotation(v, canonical)
                }
                append(']')
            }
        }
    }

    fun fromNotation(text: String): LLSD = NotationParser(text).parse()

    private class NotationParser(private val text: String) {
        private var pos = 0

        fun parse(): LLSD {
            skipWs()
            return parseValue()
        }

        private fun skipWs() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        private fun peek(): Char = if (pos < text.length) text[pos] else ' '
        private fun consume(): Char = text[pos++]

        private fun parseValue(): LLSD {
            skipWs()
            return when {
                peek() == '!' -> { consume(); LLSD.Undefined }
                text.startsWith("true", pos) -> { pos += 4; LLSD.LLSDBoolean(true) }
                text.startsWith("false", pos) -> { pos += 5; LLSD.LLSDBoolean(false) }
                peek() == 'i' -> { consume(); LLSD.LLSDInteger(parseWord().toIntOrNull() ?: 0) }
                peek() == 'r' -> { consume(); LLSD.LLSDReal(parseWord().toDoubleOrNull() ?: 0.0) }
                peek() == 'u' -> { consume(); LLSD.LLSDUUID(LLUUID.fromString(parseQuoted()) ?: LLUUID.NULL) }
                peek() == 'd' -> { consume(); LLSD.LLSDDate(LLDate.fromISOString(parseQuoted()) ?: LLDate.NULL) }
                peek() == 'l' -> { consume(); LLSD.LLSDURI(LLURI.fromString(parseQuoted())) }
                peek() == 'b' -> parseBinaryNotation()
                peek() == '\'' -> LLSD.LLSDString(parseSingleQuoted())
                peek() == '"' -> LLSD.LLSDString(parseQuoted())
                peek() == '{' -> parseMap()
                peek() == '[' -> parseArray()
                else -> { consume(); LLSD.Undefined }
            }
        }

        private fun parseWord(): String {
            val start = pos
            while (pos < text.length && !text[pos].isWhitespace() && text[pos] !in ",}]") pos++
            return text.substring(start, pos)
        }

        private fun parseQuoted(): String {
            val delim = consume()
            val sb = StringBuilder()
            while (pos < text.length && text[pos] != delim) {
                if (text[pos] == '\\' && pos + 1 < text.length) { pos++; sb.append(text[pos]) }
                else sb.append(text[pos])
                pos++
            }
            if (pos < text.length) pos++
            return sb.toString()
        }

        private fun parseSingleQuoted(): String {
            consume()
            val sb = StringBuilder()
            while (pos < text.length && text[pos] != '\'') {
                if (text[pos] == '\\' && pos + 1 < text.length) { pos++; sb.append(text[pos]) }
                else sb.append(text[pos])
                pos++
            }
            if (pos < text.length) pos++
            return sb.toString()
        }

        private fun parseBinaryNotation(): LLSD {
            consume()
            val encodingStart = pos
            val quotePos = text.indexOf('"', pos)
            pos = if (quotePos >= 0) quotePos else pos
            val data = parseQuoted()
            return LLSD.LLSDBinary(
                if (data.isEmpty()) ByteArray(0)
                else Base64.getDecoder().decode(data)
            )
        }

        private fun parseMap(): LLSD {
            consume()
            val map = mutableMapOf<String, LLSD>()
            skipWs()
            while (pos < text.length && peek() != '}') {
                skipWs()
                val key = if (peek() == '\'') parseSingleQuoted() else parseQuoted()
                skipWs()
                if (peek() == ':') consume()
                skipWs()
                map[key] = parseValue()
                skipWs()
                if (peek() == ',') consume()
                skipWs()
            }
            if (pos < text.length) consume()
            return LLSD.LLSDMap(map)
        }

        private fun parseArray(): LLSD {
            consume()
            val list = mutableListOf<LLSD>()
            skipWs()
            while (pos < text.length && peek() != ']') {
                list.add(parseValue())
                skipWs()
                if (peek() == ',') consume()
                skipWs()
            }
            if (pos < text.length) consume()
            return LLSD.LLSDArray(list)
        }
    }

    // ── Binary ───────────────────────────────────────────────────────────────

    fun toBinary(sd: LLSD): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val dos = java.io.DataOutputStream(out)
        writeBinary(dos, sd)
        dos.flush()
        return out.toByteArray()
    }

    private fun uuidToBytes(uuid: LLUUID): ByteArray {
        val msb = uuid.uuid.mostSignificantBits
        val lsb = uuid.uuid.leastSignificantBits
        return ByteArray(16) { i ->
            if (i < 8) ((msb ushr ((7 - i) * 8)) and 0xffL).toByte()
            else ((lsb ushr ((15 - i) * 8)) and 0xffL).toByte()
        }
    }

    private fun uuidFromBytes(bytes: ByteArray): LLUUID {
        var msb = 0L
        var lsb = 0L
        for (i in 0..7) msb = (msb shl 8) or (bytes[i].toLong() and 0xffL)
        for (i in 8..15) lsb = (lsb shl 8) or (bytes[i].toLong() and 0xffL)
        return LLUUID(java.util.UUID(msb, lsb))
    }

    private fun writeBinary(dos: java.io.DataOutputStream, sd: LLSD) {
        when (sd) {
            is LLSD.Undefined -> dos.writeByte('!'.code)
            is LLSD.LLSDBoolean -> {
                dos.writeByte('1'.code)
                dos.writeByte(if (sd.value) 1 else 0)
            }
            is LLSD.LLSDInteger -> {
                dos.writeByte('i'.code)
                dos.writeInt(sd.value)
            }
            is LLSD.LLSDReal -> {
                dos.writeByte('r'.code)
                dos.writeDouble(sd.value)
            }
            is LLSD.LLSDString -> {
                dos.writeByte('s'.code)
                val bytes = sd.value.toByteArray(Charsets.UTF_8)
                dos.writeInt(bytes.size)
                dos.write(bytes)
            }
            is LLSD.LLSDUUID -> {
                dos.writeByte('u'.code)
                dos.write(uuidToBytes(sd.value))
            }
            is LLSD.LLSDDate -> {
                dos.writeByte('d'.code)
                dos.writeDouble(sd.value.secondsSinceEpoch)
            }
            is LLSD.LLSDURI -> {
                dos.writeByte('l'.code)
                val bytes = sd.value.asString().toByteArray(Charsets.UTF_8)
                dos.writeInt(bytes.size)
                dos.write(bytes)
            }
            is LLSD.LLSDBinary -> {
                dos.writeByte('b'.code)
                dos.writeInt(sd.value.size)
                dos.write(sd.value)
            }
            is LLSD.LLSDMap -> {
                dos.writeByte('{'.code)
                dos.writeInt(sd.value.size)
                mapEntries(sd.value, true).forEach { (k, v) ->
                    val kb = k.toByteArray(Charsets.UTF_8)
                    dos.writeByte('k'.code)
                    dos.writeInt(kb.size)
                    dos.write(kb)
                    writeBinary(dos, v)
                }
                dos.writeByte('}'.code)
            }
            is LLSD.LLSDArray -> {
                dos.writeByte('['.code)
                dos.writeInt(sd.value.size)
                sd.value.forEach { writeBinary(dos, it) }
                dos.writeByte(']'.code)
            }
        }
    }

    fun fromBinary(data: ByteArray): LLSD {
        val dis = java.io.DataInputStream(java.io.ByteArrayInputStream(data))
        return readBinary(dis)
    }

    private fun readBinary(dis: java.io.DataInputStream): LLSD {
        val tag = dis.readByte().toInt().toChar()
        return when (tag) {
            '!' -> LLSD.Undefined
            '1' -> LLSD.LLSDBoolean(dis.readByte().toInt() != 0)
            'i' -> LLSD.LLSDInteger(dis.readInt())
            'r' -> LLSD.LLSDReal(dis.readDouble())
            's' -> {
                val len = dis.readInt()
                val bytes = ByteArray(len).also { dis.readFully(it) }
                LLSD.LLSDString(String(bytes, Charsets.UTF_8))
            }
            'u' -> {
                val bytes = ByteArray(16).also { dis.readFully(it) }
                LLSD.LLSDUUID(uuidFromBytes(bytes))
            }
            'd' -> LLSD.LLSDDate(LLDate(dis.readDouble()))
            'l' -> {
                val len = dis.readInt()
                val bytes = ByteArray(len).also { dis.readFully(it) }
                LLSD.LLSDURI(LLURI.fromString(String(bytes, Charsets.UTF_8)))
            }
            'b' -> {
                val len = dis.readInt()
                val bytes = ByteArray(len).also { dis.readFully(it) }
                LLSD.LLSDBinary(bytes)
            }
            '{' -> {
                val count = dis.readInt()
                val map = mutableMapOf<String, LLSD>()
                repeat(count) {
                    dis.readByte()
                    val klen = dis.readInt()
                    val kbytes = ByteArray(klen).also { dis.readFully(it) }
                    val key = String(kbytes, Charsets.UTF_8)
                    map[key] = readBinary(dis)
                }
                dis.readByte()
                LLSD.LLSDMap(map)
            }
            '[' -> {
                val count = dis.readInt()
                val list = (1..count).map { readBinary(dis) }
                dis.readByte()
                LLSD.LLSDArray(list)
            }
            else -> LLSD.Undefined
        }
    }

    // ── JSON ─────────────────────────────────────────────────────────────────

    fun toJSON(sd: LLSD): String = buildString { appendJSON(sd, canonical = false) }

    fun toCanonicalJSON(sd: LLSD): String = buildString { appendJSON(sd, canonical = true) }

    private fun StringBuilder.appendJSON(sd: LLSD, canonical: Boolean) {
        when (sd) {
            is LLSD.Undefined -> append("null")
            is LLSD.LLSDBoolean -> append(sd.value)
            is LLSD.LLSDInteger -> append(sd.value)
            is LLSD.LLSDReal -> append(sd.value)
            is LLSD.LLSDString -> appendJSONString(sd.value)
            is LLSD.LLSDUUID -> { append('"'); append(sd.value); append('"') }
            is LLSD.LLSDDate -> { append('"'); append(sd.value.toISOString()); append('"') }
            is LLSD.LLSDURI -> { append('"'); appendJSONEscaped(sd.value.asString()); append('"') }
            is LLSD.LLSDBinary -> {
                append('"'); append(Base64.getEncoder().encodeToString(sd.value)); append('"')
            }
            is LLSD.LLSDMap -> {
                append('{')
                mapEntries(sd.value, canonical).entries.forEachIndexed { idx, (k, v) ->
                    if (idx > 0) append(',')
                    appendJSONString(k); append(':'); appendJSON(v, canonical)
                }
                append('}')
            }
            is LLSD.LLSDArray -> {
                append('[')
                sd.value.forEachIndexed { idx, v ->
                    if (idx > 0) append(',')
                    appendJSON(v, canonical)
                }
                append(']')
            }
        }
    }

    private fun StringBuilder.appendJSONString(s: String) {
        append('"'); appendJSONEscaped(s); append('"')
    }

    private fun StringBuilder.appendJSONEscaped(s: String) {
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }

    

    private fun mapEntries(value: Map<String, LLSD>, canonical: Boolean): Map<String, LLSD> =
        if (canonical) value.toSortedMap() else value

    fun fromJSON(json: String): LLSD = JSONParser(json.trim()).parse()

    private class JSONParser(private val text: String) {
        private var pos = 0

        fun parse(): LLSD { skipWs(); return parseValue() }

        private fun skipWs() { while (pos < text.length && text[pos].isWhitespace()) pos++ }
        private fun peek(): Char = if (pos < text.length) text[pos] else ' '
        private fun consume(): Char = text[pos++]

        private fun parseValue(): LLSD {
            skipWs()
            return when (peek()) {
                'n' -> { pos += 4; LLSD.Undefined }
                't' -> { pos += 4; LLSD.LLSDBoolean(true) }
                'f' -> { pos += 5; LLSD.LLSDBoolean(false) }
                '"' -> LLSD.LLSDString(parseString())
                '{' -> parseObject()
                '[' -> parseJSONArray()
                else -> parseNumber()
            }
        }

        private fun parseString(): String {
            consume()
            val sb = StringBuilder()
            while (pos < text.length && text[pos] != '"') {
                if (text[pos] == '\\' && pos + 1 < text.length) {
                    pos++
                    when (text[pos]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        else -> sb.append(text[pos])
                    }
                } else {
                    sb.append(text[pos])
                }
                pos++
            }
            if (pos < text.length) pos++
            return sb.toString()
        }

        private fun parseNumber(): LLSD {
            val start = pos
            if (peek() == '-') pos++
            while (pos < text.length && (text[pos].isDigit() || text[pos] in ".eE+-")) pos++
            val token = text.substring(start, pos)
            return if ('.' in token || 'e' in token || 'E' in token)
                LLSD.LLSDReal(token.toDoubleOrNull() ?: 0.0)
            else
                LLSD.LLSDInteger(token.toIntOrNull() ?: 0)
        }

        private fun parseObject(): LLSD {
            consume()
            val map = mutableMapOf<String, LLSD>()
            skipWs()
            while (pos < text.length && peek() != '}') {
                skipWs()
                val key = parseString()
                skipWs(); if (peek() == ':') consume(); skipWs()
                map[key] = parseValue()
                skipWs(); if (peek() == ',') consume(); skipWs()
            }
            if (pos < text.length) consume()
            return LLSD.LLSDMap(map)
        }

        private fun parseJSONArray(): LLSD {
            consume()
            val list = mutableListOf<LLSD>()
            skipWs()
            while (pos < text.length && peek() != ']') {
                list.add(parseValue())
                skipWs()
                if (peek() == ',') consume()
                skipWs()
            }
            if (pos < text.length) consume()
            return LLSD.LLSDArray(list)
        }
    }
}
