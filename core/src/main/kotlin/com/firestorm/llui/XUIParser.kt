package com.firestorm.llui

import com.firestorm.llmath.Color4
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.io.File
import java.io.StringWriter

private const val MAX_STRING_ATTRIBUTE_SIZE = 40
private const val NO_VALUE_MARKER = "no_value"

typealias NameStack = MutableList<Pair<String, Boolean>>
typealias WidgetCreatorFunc = (node: Element, parent: Any?, outputNode: Element?) -> Any?

object WidgetTypeRegistry {
    private val registry: MutableMap<String, String> = mutableMapOf()
    fun register(name: String, typeId: String) { registry[name] = typeId }
    fun getValue(name: String): String? = registry[name]
}

object ChildRegistryRegistry {
    private val registry: MutableMap<String, MutableMap<String, WidgetCreatorFunc>> = mutableMapOf()
    fun register(typeId: String, name: String, creator: WidgetCreatorFunc) {
        registry.getOrPut(typeId) { mutableMapOf() }[name] = creator
    }
    fun getValue(typeId: String): Map<String, WidgetCreatorFunc>? = registry[typeId]
}

class XSDWriter {
    private val attributesWritten: MutableMap<Element, MutableSet<String>> = mutableMapOf()
    var attributeNode: Element? = null
    var elementNode: Element? = null
    var schemaNode: Element? = null

    fun writeXSD(typeName: String, node: Element, xmlNamespace: String) {
        schemaNode = node
        System.err.println("XSDWriter: writeXSD not yet implemented")
    }

    fun writeAttribute(
        type: String,
        stack: NameStack,
        minCount: Int,
        maxCount: Int,
        possibleValues: List<String>?
    ) {
        val nonEmpty = stack.filter { it.first.isNotEmpty() }
        val attributeName = nonEmpty.joinToString(".") { it.first }
        val attributeMandatory = minCount == 1 && maxCount == 1 && nonEmpty.size == 1

        if (maxCount <= 1) {
            addAttributeToSchema(attributeNode, attributeName, type, attributeMandatory, possibleValues)
        }

        if (nonEmpty.size > 1 && !attributeMandatory) {
            val elementName = nonEmpty.dropLast(1).joinToString(".") { it.first }
            val shortAttrName = nonEmpty.last().first

            val elNode = elementNode ?: return
            var complexTypeNode: Element? = findExistingElement(elNode, elementName)
            if (complexTypeNode == null) {
                val doc = elNode.ownerDocument
                val newElement = doc.createElement("xs:element")
                newElement.setAttribute("minOccurs", minCount.toString())
                newElement.setAttribute("maxOccurs", maxCount.toString())
                newElement.setAttribute("name", elementName)
                val ctype = doc.createElement("xs:complexType")
                newElement.appendChild(ctype)
                elNode.appendChild(newElement)
                complexTypeNode = ctype
            }
            addAttributeToSchema(complexTypeNode, shortAttrName, type, false, possibleValues)
        }
    }

    private fun findExistingElement(parent: Element, name: String): Element? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element && child.getAttribute("name") == name) {
                val grandchildren = child.childNodes
                for (j in 0 until grandchildren.length) {
                    val gc = grandchildren.item(j)
                    if (gc is Element && gc.tagName == "xs:complexType") return gc
                }
            }
        }
        return null
    }

    private fun addAttributeToSchema(
        typeNode: Element?,
        attributeName: String,
        type: String,
        mandatory: Boolean,
        possibleValues: List<String>?
    ) {
        if (attributeName.isEmpty() || typeNode == null) return
        val doc = typeNode.ownerDocument ?: return
        val written = attributesWritten.getOrPut(typeNode) { mutableSetOf() }
        if (attributeName in written) {
            val existing = findAttributeDeclaration(typeNode, attributeName) ?: return
            val existingType = existing.getAttribute("type")
            if (possibleValues != null || !existing.hasAttribute("type")) {
                existing.setAttribute("type", "xs:string")
                val simpleTypes = existing.getElementsByTagName("xs:simpleType")
                while (simpleTypes.length > 0) existing.removeChild(simpleTypes.item(0))
            } else if (existingType != type) {
                existing.setAttribute("type", "string")
            }
            return
        }
        written.add(attributeName)
        val attrNode = doc.createElement("xs:attribute")
        attrNode.setAttribute("name", attributeName)
        if (possibleValues != null) {
            val simpleType = doc.createElement("xs:simpleType")
            val restriction = doc.createElement("xs:restriction")
            restriction.setAttribute("base", "xs:string")
            for (v in possibleValues) {
                val enum = doc.createElement("xs:enumeration")
                enum.setAttribute("value", v)
                restriction.appendChild(enum)
            }
            simpleType.appendChild(restriction)
            attrNode.appendChild(simpleType)
        } else {
            attrNode.setAttribute("type", type)
        }
        attrNode.setAttribute("use", if (mandatory) "required" else "optional")
        typeNode.appendChild(attrNode)
    }

    private fun findAttributeDeclaration(parent: Element, name: String): Element? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element && child.tagName == "xs:attribute" && child.getAttribute("name") == name) {
                return child
            }
        }
        return null
    }
}

class XUIXSDWriter : XSDWriter() {
    fun writeXSD(typeName: String, path: String) {
        System.err.println("XUIXSDWriter: writeXSD not yet implemented")
    }
}

class XUIParser {
    private val nameStack: NameStack = mutableListOf()
    private var curReadNode: Element? = null
    private var writeRootNode: Element? = null
    private val outNodes: MutableMap<String, Element> = mutableMapOf()
    private var curReadDepth: Int = 0
    private var curFileName: String = ""
    private var rootNodeName: String = ""
    private var parseSilently: Boolean = false

    fun getCurrentElementName(): String =
        nameStack.joinToString("") { it.first + "." }

    fun getCurrentFileName(): String = curFileName

    fun parserWarning(message: String) {
        val line = curReadNode?.let { "" } ?: ""
        System.err.println("WARNING: $message\t$curFileName$line")
    }

    fun parserError(message: String) {
        val line = curReadNode?.let { "" } ?: ""
        System.err.println("ERROR: $message\t$curFileName$line")
    }

    fun readXUI(node: Element, block: MutableMap<String, Any?>, filename: String = "", silent: Boolean = false) {
        nameStack.clear()
        rootNodeName = node.tagName
        curFileName = filename
        curReadDepth = 0
        parseSilently = silent
        readXUIImpl(node, block)
    }

    private fun readXUIImpl(node: Element, block: MutableMap<String, Any?>): Boolean {
        val silent = curReadDepth > 0
        val hasChildren = node.hasChildNodes()
        val hasAttributes = node.attributes.length > 0
        val textContent = node.textContent?.trim() ?: ""

        if (!hasChildren && !hasAttributes && textContent.isEmpty()) {
            block["__flag__${nameStack.joinToString(".") { it.first }}"] = true
            return true
        }

        var valuesParsed = readAttributes(node, block)

        if (textContent.isNotEmpty()) {
            nameStack.add(Pair("value", true))
            block[buildKeyPath()] = textContent
            nameStack.removeAt(nameStack.lastIndex)
            valuesParsed = true
        }

        curReadDepth++
        var child = node.firstChild
        while (child != null) {
            val next = child.nextSibling
            if (child !is Element) { child = next; continue }
            val childName = child.tagName
            var numTokensPushed = 0

            if (!childName.contains('.')) {
                nameStack.add(Pair(childName, true))
                numTokensPushed = 1
            } else {
                val tokens = childName.split('.')
                val tokenIt = tokens.iterator()
                if (!tokenIt.hasNext()) { child = next; continue }
                val first = tokenIt.next()
                val expectedScope = if (nameStack.isEmpty()) rootNodeName else nameStack.last().first
                if (first != expectedScope) { child = next; continue }
                while (tokenIt.hasNext()) {
                    nameStack.add(Pair(tokenIt.next(), true))
                    numTokensPushed++
                }
            }

            if (readXUIImpl(child, block)) {
                valuesParsed = true
            }

            repeat(numTokensPushed) { nameStack.removeAt(nameStack.lastIndex) }
            child = next
        }
        curReadDepth--
        return valuesParsed
    }

    private fun readAttributes(node: Element, block: MutableMap<String, Any?>): Boolean {
        val attrs = node.attributes
        var anyParsed = false
        for (i in 0 until attrs.length) {
            val attr = attrs.item(i)
            val attrName = attr.nodeName
            val attrValue = attr.nodeValue
            val tokens = attrName.split('.')
            val pushed = tokens.size
            tokens.forEach { nameStack.add(Pair(it, true)) }
            block[buildKeyPath()] = attrValue
            anyParsed = true
            repeat(pushed) { nameStack.removeAt(nameStack.lastIndex) }
        }
        return anyParsed
    }

    private fun buildKeyPath(): String =
        nameStack.filter { it.first.isNotEmpty() }.joinToString(".") { it.first }

    fun writeXUI(node: Element, block: Map<String, Any?>) {
        writeRootNode = node
        outNodes.clear()
        for ((key, value) in block) {
            val parts = key.split('.').map { Pair(it, true) }.toMutableList()
            val targetNode = getOrCreateNode(parts) ?: continue
            when (value) {
                is Boolean -> targetNode.setAttribute(parts.last().first, value.toString())
                is String  -> writeStringToNode(targetNode, parts.last().first, value, node)
                is UByte   -> targetNode.setAttribute(parts.last().first, value.toString())
                is Int     -> targetNode.setAttribute(parts.last().first, value.toString())
                is UInt    -> targetNode.setAttribute(parts.last().first, value.toString())
                is Float   -> targetNode.setAttribute(parts.last().first, value.toString())
                is Double  -> targetNode.setAttribute(parts.last().first, value.toString())
                is FloatArray -> targetNode.setAttribute(parts.last().first, value.joinToString(" "))
                else       -> if (value != null) targetNode.setAttribute(parts.last().first, value.toString())
            }
        }
    }

    private fun getOrCreateNode(stack: List<Pair<String, Boolean>>): Element? {
        if (stack.isEmpty()) return null
        val doc = writeRootNode?.ownerDocument ?: return null
        var current = writeRootNode!!
        val path = stack.dropLast(1)
        for ((name, _) in path) {
            if (name.isEmpty()) continue
            val existing = outNodes[name]
            current = if (existing != null) {
                existing
            } else {
                val newEl = doc.createElement(name)
                current.appendChild(newEl)
                outNodes[name] = newEl
                newEl
            }
        }
        return current
    }

    private fun writeStringToNode(node: Element, attrName: String, value: String, root: Element) {
        if (value.contains('\n') || value.length > MAX_STRING_ATTRIBUTE_SIZE) {
            val doc = node.ownerDocument
            val parent = node.parentNode as? Element ?: root
            parent.removeChild(node)
            val targetNode = if (attrName == "value") {
                parent
            } else {
                val child = doc.createElement(attrName)
                parent.appendChild(child)
                child
            }
            targetNode.textContent = value
        } else {
            node.setAttribute(attrName, value)
        }
    }
}

class SimpleXUIParser(
    private val elementCallback: ((parser: SimpleXUIParser, blockName: String) -> MutableMap<String, Any?>?)? = null
) {
    private val nameStack: NameStack = mutableListOf()
    private val outputStack: MutableList<Pair<MutableMap<String, Any?>, Int>> = mutableListOf()
    private val tokenSizeStack: MutableList<Int> = mutableListOf()
    private val scope: MutableList<String> = mutableListOf()
    private val emptyLeafNode: MutableList<Boolean> = mutableListOf()
    private var curFileName: String = ""
    private var curReadDepth: Int = 0
    private var parseSilently: Boolean = false
    private var textContents: String = ""
    private var curAttributeValueBegin: String = ""

    fun getCurrentElementName(): String =
        nameStack.joinToString("") { it.first + "." }

    fun getCurrentFileName(): String = curFileName

    fun parserWarning(message: String) {
        System.err.println("WARNING: $message\t$curFileName")
    }

    fun parserError(message: String) {
        System.err.println("ERROR: $message\t$curFileName")
    }

    fun readXUI(filename: String, block: MutableMap<String, Any?>, silent: Boolean = false): Boolean {
        outputStack.clear()
        outputStack.add(Pair(block, 0))
        nameStack.clear()
        curFileName = filename
        curReadDepth = 0
        parseSilently = silent

        val file = File(filename)
        if (!file.exists()) {
            System.err.println("Unable to open file $filename")
            return false
        }

        return try {
            false
        } catch (e: Exception) {
            System.err.println("Error parsing file $filename: ${e.message}")
            false
        }
    }

    fun startElement(name: String, atts: Map<String, String>) {
        processText()

        val callbackBlock = elementCallback?.invoke(this, name)
        if (callbackBlock != null) {
            outputStack.add(Pair(callbackBlock, 0))
        }

        outputStack[outputStack.lastIndex] = outputStack.last().let { Pair(it.first, it.second + 1) }
        var numTokensPushed = 0
        val childName = name

        if (outputStack.last().second == 1) {
            scope.add(childName)
        } else {
            if (!childName.contains('.')) {
                nameStack.add(Pair(childName, true))
                numTokensPushed++
                scope.add(childName)
            } else {
                val tokens = childName.split('.')
                val it = tokens.iterator()
                if (!it.hasNext()) return
                val first = it.next()
                if (scope.isNotEmpty() && first != scope.last()) return
                while (it.hasNext()) {
                    nameStack.add(Pair(it.next(), true))
                    numTokensPushed++
                }
                if (nameStack.isNotEmpty()) scope.add(nameStack.last().first)
            }
        }

        emptyLeafNode[emptyLeafNode.lastIndex] = false
        emptyLeafNode.add(atts.isEmpty())
        tokenSizeStack.add(numTokensPushed)
        readAttributes(atts)
    }

    fun endElement(name: String) {
        val hasText = processText()

        if (!hasText && emptyLeafNode.last()) {
            curAttributeValueBegin = NO_VALUE_MARKER
            val keyPath = nameStack.filter { it.first.isNotEmpty() }.joinToString(".") { it.first }
            outputStack.last().first["__flag__$keyPath"] = true
        }

        val (block, count) = outputStack.last()
        if (count - 1 == 0) {
            outputStack.removeAt(outputStack.lastIndex)
        } else {
            outputStack[outputStack.lastIndex] = Pair(block, count - 1)
        }

        var numToPop = tokenSizeStack.removeAt(tokenSizeStack.lastIndex)
        while (numToPop-- > 0) nameStack.removeAt(nameStack.lastIndex)
        scope.removeAt(scope.lastIndex)
        emptyLeafNode.removeAt(emptyLeafNode.lastIndex)
    }

    fun characterData(s: String) {
        textContents += s
    }

    private fun readAttributes(atts: Map<String, String>) {
        for ((attrName, attrValue) in atts) {
            curAttributeValueBegin = attrValue
            val tokens = attrName.split('.')
            tokens.forEach { nameStack.add(Pair(it, true)) }
            val keyPath = nameStack.filter { it.first.isNotEmpty() }.joinToString(".") { it.first }
            outputStack.last().first[keyPath] = parseTypedValue(attrValue)
            repeat(tokens.size) { nameStack.removeAt(nameStack.lastIndex) }
        }
    }

    private fun processText(): Boolean {
        if (textContents.isNotEmpty()) {
            val trimmed = textContents.trim()
            if (trimmed.isNotEmpty()) {
                nameStack.add(Pair("value", true))
                curAttributeValueBegin = trimmed
                val keyPath = nameStack.filter { it.first.isNotEmpty() }.joinToString(".") { it.first }
                outputStack.last().first[keyPath] = trimmed
                nameStack.removeAt(nameStack.lastIndex)
            }
            textContents = ""
            return true
        }
        return false
    }

    private fun parseTypedValue(raw: String): Any = when {
        raw == "true"  -> true
        raw == "false" -> false
        raw.toIntOrNull() != null    -> raw.toInt()
        raw.toLongOrNull() != null   -> raw.toLong()
        raw.toFloatOrNull() != null  -> raw.toFloat()
        raw.toDoubleOrNull() != null -> raw.toDouble()
        else -> raw
    }
}
