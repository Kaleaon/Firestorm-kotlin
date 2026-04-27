package com.firestorm.llxml

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

class XmlParser {
    var depth: Int = 0
        private set
    var lastError: String = ""
        private set

    fun parse(xml: String): XmlNode? = XmlNode.parse(xml)

    fun parseFile(path: String): XmlNode? = runCatching {
        parse(File(path).readText(Charsets.UTF_8))
    }.getOrElse { e ->
        lastError = e.message ?: "Unknown error"
        null
    }

    fun parseStream(input: java.io.InputStream): XmlNode? = runCatching {
        parse(input.reader(Charsets.UTF_8).readText())
    }.getOrElse { e ->
        lastError = e.message ?: "Unknown error"
        null
    }

    fun getErrorString(): String = lastError

    fun getCurrentLineNumber(): Int = -1

    fun getCurrentColumnNumber(): Int = -1

    fun getDepth(): Int = depth

    // SAX-based incremental parse handler, mirrors LLXmlParser's virtual callback API.
    // Subclasses override the on* methods for event-driven processing.
    open class SaxHandler : DefaultHandler() {
        var depth: Int = 0
            private set
        var lastError: String = "no error"

        open fun onStartElement(name: String, attributes: Map<String, String>) {}
        open fun onEndElement(name: String) {}
        open fun onCharacterData(data: String) {}
        open fun onProcessingInstruction(target: String, data: String) {}
        open fun onComment(data: String) {}
        open fun onStartCdataSection() {}
        open fun onEndCdataSection() {}
        open fun onDefaultData(data: String) {}

        override fun startElement(uri: String, localName: String, qName: String, atts: Attributes) {
            val map = mutableMapOf<String, String>()
            for (i in 0 until atts.length) map[atts.getQName(i)] = atts.getValue(i)
            onStartElement(qName, map)
            depth++
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            depth--
            onEndElement(qName)
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            onCharacterData(String(ch, start, length))
        }

        override fun processingInstruction(target: String, data: String) {
            onProcessingInstruction(target, data)
        }

        override fun startCDATA() {
            depth++
            onStartCdataSection()
        }

        override fun endCDATA() {
            onEndCdataSection()
            depth++
        }

        fun parseString(xml: String): Boolean = runCatching {
            val factory = SAXParserFactory.newInstance()
            val parser = factory.newSAXParser()
            parser.parse(InputSource(StringReader(xml)), this)
            true
        }.getOrElse { e ->
            lastError = e.message ?: "Unknown error"
            false
        }

        fun parseFile(path: String): Boolean = runCatching {
            parseString(File(path).readText(Charsets.UTF_8))
        }.getOrElse { e ->
            lastError = e.message ?: "Unknown error"
            false
        }

        fun getErrorString(): String = lastError
        fun getCurrentLineNumber(): Int = -1
        fun getCurrentColumnNumber(): Int = -1
    }
}
