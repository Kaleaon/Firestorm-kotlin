package com.firestorm.newview

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Node

interface NotificationResponderInterface {
    fun fromLLSD(params: Map<String, Any>)
    fun toLLSD(): Map<String, Any>
}

typealias NotificationPtr = Notification
typealias ResponderConstructor = (params: Map<String, Any>) -> NotificationResponderInterface

object ResponderRegistry {
    private val registry: MutableMap<String, ResponderConstructor> = mutableMapOf()

    fun register(name: String, constructor: ResponderConstructor) {
        registry[name] = constructor
    }

    fun createResponder(notificationName: String, params: Map<String, Any>): NotificationResponderInterface? {
        return registry[notificationName]?.invoke(params)
    }
}

open class NotificationStorage(private var fileName: String) {
    private var oldFileName: String = ""

    protected fun setFileName(name: String) { fileName = name }
    protected fun setOldFileName(name: String) { oldFileName = name }

    protected fun writeNotifications(notificationData: Map<String, Any>): Boolean {
        return try {
            val file = File(fileName)
            val writer = FileWriter(file)
            writer.write(serializeLLSD(notificationData))
            writer.close()
            true
        } catch (e: Exception) {
            System.err.println("NotificationStorage: Failed to open file '$fileName': ${e.message}")
            false
        }
    }

    protected fun readNotifications(notificationData: MutableMap<String, Any>, isNewFilename: Boolean = true): Boolean {
        val filename = if (isNewFilename) fileName else oldFileName
        System.out.println("NotificationStorage: starting read '$filename'")

        notificationData.clear()

        return try {
            val file = File(filename)
            if (!file.exists()) {
                System.err.println("NotificationStorage: Failed to open file '$filename'")
                if (isNewFilename && oldFileName.isNotEmpty()) {
                    val ok = readNotifications(notificationData, false)
                    if (ok) {
                        writeNotifications(notificationData)
                        File(oldFileName).delete()
                    }
                    return ok
                }
                return false
            }
            val parsed = parseLLSD(FileReader(file).readText())
            notificationData.putAll(parsed)
            true
        } catch (e: Exception) {
            System.err.println("NotificationStorage: Failed to parse notifications from '$filename': ${e.message}")
            File(filename).delete()
            System.err.println("NotificationStorage: Removed invalid open notifications file '$filename'")
            if (isNewFilename && oldFileName.isNotEmpty()) {
                val ok = readNotifications(notificationData, false)
                if (ok) {
                    writeNotifications(notificationData)
                    File(oldFileName).delete()
                }
                ok
            } else {
                false
            }
        }
    }

    protected fun createResponder(notificationName: String, params: Map<String, Any>): NotificationResponderInterface? {
        return ResponderRegistry.createResponder(notificationName, params)
    }

    private fun serializeLLSD(data: Map<String, Any>): String {
        val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<llsd><map>\n")
        data.forEach { (k, v) ->
            sb.append("<key>${k.xmlEscape()}</key>\n")
            sb.append(serializeValue(v))
        }
        sb.append("</map></llsd>\n")
        return sb.toString()
    }

    private fun serializeValue(v: Any?): String = when (v) {
        is String  -> "<string>${v.xmlEscape()}</string>\n"
        is Boolean -> "<boolean>${if (v) 1 else 0}</boolean>\n"
        is Int, is Long -> "<integer>$v</integer>\n"
        is Float, is Double -> "<real>$v</real>\n"
        is Map<*, *> -> {
            val sb = StringBuilder("<map>\n")
            @Suppress("UNCHECKED_CAST")
            (v as Map<String, Any>).forEach { (k, c) ->
                sb.append("<key>${k.xmlEscape()}</key>\n")
                sb.append(serializeValue(c))
            }
            sb.append("</map>\n")
            sb.toString()
        }
        is List<*> -> {
            val sb = StringBuilder("<array>\n")
            v.forEach { sb.append(serializeValue(it)) }
            sb.append("</array>\n")
            sb.toString()
        }
        null -> "<undef/>\n"
        else -> "<string>${v.toString().xmlEscape()}</string>\n"
    }

    private fun String.xmlEscape() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun parseLLSD(xml: String): Map<String, Any> {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val doc = factory.newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.toByteArray()))
            val root = doc.documentElement
            val child = firstElementChild(root)
            @Suppress("UNCHECKED_CAST")
            parseNode(child) as? Map<String, Any> ?: emptyMap()
        } catch (e: Exception) {
            System.err.println("NotificationStorage: Failed to parse LLSD: ${e.message}")
            emptyMap()
        }
    }

    private fun parseNode(node: Node?): Any? {
        val n = node ?: return null
        return when (n.nodeName) {
            "map" -> {
                val result = mutableMapOf<String, Any>()
                var child = firstElementChild(n)
                while (child != null) {
                    if (child.nodeName == "key") {
                        val key = child.textContent
                        val valueNode = nextElementSibling(child)
                        val value = parseNode(valueNode)
                        if (value != null) result[key] = value
                        child = if (valueNode != null) nextElementSibling(valueNode) else null
                    } else {
                        child = nextElementSibling(child)
                    }
                }
                result
            }
            "array" -> {
                val result = mutableListOf<Any?>()
                var child = firstElementChild(n)
                while (child != null) {
                    result.add(parseNode(child))
                    child = nextElementSibling(child)
                }
                result
            }
            "string"  -> n.textContent
            "integer" -> n.textContent.toLongOrNull() ?: 0L
            "real"    -> n.textContent.toDoubleOrNull() ?: 0.0
            "boolean" -> n.textContent.trim() != "0" && n.textContent.trim() != "false"
            "undef"   -> null
            else      -> n.textContent
        }
    }

    private fun firstElementChild(n: Node): Node? {
        var child = n.firstChild
        while (child != null && child.nodeType != Node.ELEMENT_NODE) child = child.nextSibling
        return child
    }

    private fun nextElementSibling(n: Node): Node? {
        var sib = n.nextSibling
        while (sib != null && sib.nodeType != Node.ELEMENT_NODE) sib = sib.nextSibling
        return sib
    }
}
