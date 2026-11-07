package com.firestorm.llplugin

/**
 * LLPluginMessage encapsulates serialization/deserialization of plugin messages.
 *
 * The wire format is XML produced by LLSD serialization; here we model the LLSD
 * map as nested plain maps so the structure is transparent without pulling in an
 * LLSD library dependency.
 *
 * U32 values are stored as hex strings ("0x…") matching the C++ implementation.
 * Pointer values are stored as hex strings; on the JVM side they are Long handles
 * rather than raw addresses.
 */
class LLPluginMessage {

    private val params: MutableMap<String, Any> = mutableMapOf()
    private var messageClass: String = ""
    private var messageName: String = ""

    constructor()

    constructor(other: LLPluginMessage) {
        this.messageClass = other.messageClass
        this.messageName = other.messageName
        this.params.putAll(other.params)
    }

    constructor(messageClass: String, messageName: String) {
        setMessage(messageClass, messageName)
    }

    fun clear() {
        messageClass = ""
        messageName = ""
        params.clear()
    }

    fun setMessage(messageClass: String, messageName: String) {
        clear()
        this.messageClass = messageClass
        this.messageName = messageName
    }

    fun setValue(key: String, value: String) { params[key] = value }
    fun setValueLLSD(key: String, value: Any?) { params[key] = value ?: "" }
    fun setValueS32(key: String, value: Int) { params[key] = value }
    fun setValueU32(key: String, value: UInt) { params[key] = "0x${value.toString(16)}" }
    fun setValueBoolean(key: String, value: Boolean) { params[key] = value }
    fun setValueReal(key: String, value: Double) { params[key] = value }

    fun setValuePointer(key: String, value: Long) {
        params[key] = "0x${value.toString(16)}"
    }

    fun getClass(): String = messageClass
    fun getName(): String = messageName

    fun hasValue(key: String): Boolean = params.containsKey(key)

    fun getValue(key: String): String = params[key]?.toString() ?: ""

    fun getValueLLSD(key: String): Any? = params[key]

    fun getValueS32(key: String): Int = when (val v = params[key]) {
        is Int -> v
        is Long -> v.toInt()
        is String -> v.toIntOrNull() ?: 0
        else -> 0
    }

    fun getValueU32(key: String): UInt {
        val v = params[key]?.toString() ?: return 0u
        return v.removePrefix("0x").toUIntOrNull(16) ?: 0u
    }

    fun getValueBoolean(key: String): Boolean = when (val v = params[key]) {
        is Boolean -> v
        is String -> v.equals("true", ignoreCase = true) || v == "1"
        is Int -> v != 0
        else -> false
    }

    fun getValueReal(key: String): Double = when (val v = params[key]) {
        is Double -> v
        is Float -> v.toDouble()
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    fun getValuePointer(key: String): Long {
        val v = params[key]?.toString() ?: return 0L
        return v.removePrefix("0x").toLongOrNull(16) ?: 0L
    }

    fun generate(): String {
        val sb = StringBuilder()
        sb.appendLine("<llsd><map>")
        sb.appendLine("  <key>class</key><string>${escapeXml(messageClass)}</string>")
        sb.appendLine("  <key>name</key><string>${escapeXml(messageName)}</string>")
        sb.appendLine("  <key>params</key><map>")
        for ((k, v) in params) {
            sb.appendLine("    <key>${escapeXml(k)}</key>${toLlsdXml(v)}")
        }
        sb.appendLine("  </map>")
        sb.appendLine("</map></llsd>")
        return sb.toString()
    }

    fun parse(message: String): Int {
        clear()
        return try {
            val classMatch = Regex("<key>class</key><string>(.*?)</string>").find(message)
            val nameMatch = Regex("<key>name</key><string>(.*?)</string>").find(message)
            messageClass = classMatch?.groupValues?.get(1) ?: ""
            messageName = nameMatch?.groupValues?.get(1) ?: ""

            val paramsBlock = Regex("<key>params</key><map>(.*?)</map>", RegexOption.DOT_MATCHES_ALL)
                .find(message)?.groupValues?.get(1) ?: ""
            val keyValRegex = Regex("<key>(.*?)</key>(.*?)(?=<key>|$)", RegexOption.DOT_MATCHES_ALL)
            var count = 0
            keyValRegex.findAll(paramsBlock).forEach { m ->
                val key = m.groupValues[1]
                val valXml = m.groupValues[2].trim()
                params[key] = parseXmlValue(valXml)
                count++
            }
            count
        } catch (_: Exception) {
            -1
        }
    }

    private fun parseXmlValue(xml: String): Any {
        return when {
            xml.startsWith("<string>") -> xml.removeSurrounding("<string>", "</string>")
            xml.startsWith("<integer>") -> xml.removeSurrounding("<integer>", "</integer>").toIntOrNull() ?: 0
            xml.startsWith("<real>") -> xml.removeSurrounding("<real>", "</real>").toDoubleOrNull() ?: 0.0
            xml.startsWith("<boolean>") -> xml.contains("true") || xml.contains("1")
            else -> xml
        }
    }

    private fun toLlsdXml(v: Any?): String = when (v) {
        is Boolean -> "<boolean>${if (v) "true" else "false"}</boolean>"
        is Int -> "<integer>$v</integer>"
        is Double -> "<real>$v</real>"
        is Float -> "<real>$v</real>"
        is String -> "<string>${escapeXml(v)}</string>"
        else -> "<string>${escapeXml(v.toString())}</string>"
    }

    private fun escapeXml(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

abstract class LLPluginMessageListener {
    abstract fun receivePluginMessage(message: LLPluginMessage)
}

open class LLPluginMessageDispatcher {
    private val listeners: MutableSet<LLPluginMessageListener> = mutableSetOf()

    fun addPluginMessageListener(listener: LLPluginMessageListener) {
        listeners.add(listener)
    }

    fun removePluginMessageListener(listener: LLPluginMessageListener) {
        listeners.remove(listener)
    }

    protected fun dispatchPluginMessage(message: LLPluginMessage) {
        val snapshot = listeners.toList()
        for (listener in snapshot) {
            listener.receivePluginMessage(message)
        }
    }
}
