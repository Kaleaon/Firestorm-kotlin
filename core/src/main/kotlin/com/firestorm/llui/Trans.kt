package com.firestorm.llui

data class TransTemplate(val name: String = "", val text: String = "")

object Trans {

    private val stringTemplates:        MutableMap<String, TransTemplate> = mutableMapOf()
    private val defaultStringTemplates: MutableMap<String, TransTemplate> = mutableMapOf()
    private val defaultArgs:            MutableMap<String, String>        = mutableMapOf()

    private var defaultStringsInit = false

    fun parseStrings(root: XmlNode, defaultArgNames: Set<String>): Boolean {
        if (root.name != "strings") {
            error("Invalid root node name: expected 'strings', got '${root.name}'")
        }

        stringTemplates.clear()
        defaultArgs.clear()

        for (child in root.children) {
            val name  = child.attribute("name")  ?: continue
            val value = child.attribute("value") ?: ""
            val tmpl  = TransTemplate(name, value)
            stringTemplates[name] = tmpl
            if (!defaultStringsInit) {
                defaultStringTemplates[name] = tmpl
            }
            if (name in defaultArgNames) {
                val key = if (name.startsWith("[")) name else "[$name]"
                defaultArgs[key] = value
            }
        }
        defaultStringsInit = true
        return true
    }

    fun parseLanguageStrings(root: XmlNode): Boolean {
        if (root.name != "strings") {
            error("Invalid root node name: expected 'strings', got '${root.name}'")
        }
        for (child in root.children) {
            val name  = child.attribute("name")  ?: continue
            val value = child.attribute("value") ?: ""
            stringTemplates[name] = TransTemplate(name, value)
        }
        return true
    }

    fun getString(xmlDesc: String, args: Map<String, String> = emptyMap(), defString: Boolean = false): String {
        if (defString) return getDefString(xmlDesc, args)
        val tmpl = stringTemplates[xmlDesc]
            ?: return "MissingString($xmlDesc)"
        return format(tmpl.text, defaultArgs + args)
    }

    fun getString(xmlDesc: String, args: LLSD, defString: Boolean = false): String {
        if (defString) return getDefString(xmlDesc, args)
        val tmpl = stringTemplates[xmlDesc]
            ?: return "MissingString($xmlDesc)"
        return format(tmpl.text, args)
    }

    fun getDefString(xmlDesc: String, args: Map<String, String> = emptyMap()): String {
        val tmpl = defaultStringTemplates[xmlDesc]
            ?: return "MissingString($xmlDesc)"
        return format(tmpl.text, defaultArgs + args)
    }

    fun getDefString(xmlDesc: String, args: LLSD): String {
        val tmpl = defaultStringTemplates[xmlDesc]
            ?: return "MissingString($xmlDesc)"
        return format(tmpl.text, args)
    }

    fun findString(xmlDesc: String, args: Map<String, String> = emptyMap()): String? {
        val tmpl = stringTemplates[xmlDesc] ?: return null
        return format(tmpl.text, defaultArgs + args)
    }

    fun findString(xmlDesc: String, args: LLSD): String? {
        val tmpl = stringTemplates[xmlDesc] ?: return null
        return format(tmpl.text, args)
    }

    fun getKeyboardString(keystring: String): String =
        findString(keystring) ?: keystring

    fun getDefaultArgs(): Map<String, String> = defaultArgs

    fun setDefaultArg(name: String, value: String) {
        defaultArgs[name] = value
    }

    fun getArgs(into: MutableMap<String, String>) {
        into.putAll(defaultArgs)
    }

    fun getCountString(language: String, xmlDesc: String, count: Int): String {
        val form = when (language) {
            "ru" -> when {
                count % 10 == 1 && count % 100 != 11 -> "A"
                count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "B"
                else -> "C"
            }
            "fr", "pt" -> if (count == 0 || count == 1) "A" else "B"
            else        -> if (count == 1) "A" else "B"
        }
        return getString("$xmlDesc$form", mapOf("[COUNT]" to count.toString()))
    }

    private fun format(text: String, args: Map<String, String>): String {
        var result = text
        for ((k, v) in args) result = result.replace(k, v)
        return result
    }

    private fun format(text: String, args: LLSD): String {
        var result = text
        for ((k, v) in args.asMap()) result = result.replace("[$k]", v.toString())
        return result
    }
}
