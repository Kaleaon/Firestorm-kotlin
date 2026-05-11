package com.firestorm.llui

class UIString {
    private var orig: String = ""
    private var args: MutableMap<String, String>? = null

    private var result: String = ""
    private var needsResult: Boolean = true

    constructor()

    constructor(instring: String) {
        assign(instring)
    }

    constructor(instring: String, initialArgs: Map<String, String>) {
        orig = instring
        args = initialArgs.toMutableMap()
        dirty()
    }

    fun assign(s: String) {
        orig = s
        dirty()
    }

    operator fun invoke(): String = getString()

    fun setArgList(newArgs: Map<String, String>) {
        ensureArgs().putAll(newArgs)
        dirty()
    }

    fun setArgs(newArgs: Map<String, String>) = setArgList(newArgs)

    fun setArgs(sd: Map<String, Any?>) {
        for ((k, v) in sd) {
            setArg(k, v?.toString() ?: "")
        }
        dirty()
    }

    fun setArg(key: String, replacement: String) {
        ensureArgs()[key] = replacement
        dirty()
    }

    fun getString(): String = getUpdatedResult()

    fun empty(): Boolean = getUpdatedResult().isEmpty()

    fun length(): Int = getUpdatedResult().length

    fun clear() {
        orig = ""
        result = ""
        needsResult = true
    }

    fun clearArgs() {
        args?.clear()
    }

    fun updateCurrencySymbols() {
        dirty()
    }

    fun truncate(maxChars: Int) {
        val r = getUpdatedResult()
        if (r.length > maxChars) {
            result = r.substring(0, maxChars)
            needsResult = false
        }
    }

    fun erase(charIdx: Int, len: Int) {
        val r = getUpdatedResult().toStringBuilder()
        r.delete(charIdx, charIdx + len)
        result = r.toString()
        needsResult = false
    }

    fun insert(charIdx: Int, chars: String) {
        val r = getUpdatedResult().toStringBuilder()
        r.insert(charIdx, chars)
        result = r.toString()
        needsResult = false
    }

    fun replace(charIdx: Int, ch: Char) {
        val r = getUpdatedResult().toStringBuilder()
        r.setCharAt(charIdx, ch)
        result = r.toString()
        needsResult = false
    }

    private fun dirty() {
        needsResult = true
    }

    private fun getUpdatedResult(): String {
        if (needsResult) updateResult()
        return result
    }

    private fun updateResult() {
        needsResult = false
        if (orig.isEmpty()) {
            result = ""
            return
        }
        var r = orig
        val localArgs = args
        if (!localArgs.isNullOrEmpty()) {
            for ((k, v) in localArgs) {
                r = r.replace(k, v)
            }
        }
        result = r
    }

    private fun ensureArgs(): MutableMap<String, String> {
        if (args == null) args = mutableMapOf()
        return args!!
    }

    override fun toString(): String = getString()
}

private fun String.toStringBuilder(): StringBuilder = StringBuilder(this)
