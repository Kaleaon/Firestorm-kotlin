package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llui.LLSD
import java.io.File

private const val SYNTAX_ID_CAPABILITY_NAME = "LSLSyntax"
private const val SYNTAX_ID_SIMULATOR_FEATURE = "LSLSyntaxId"
private const val FILENAME_DEFAULT = "keywords_lsl_default.xml"
private const val LLSD_SYNTAX_LSL_VERSION_EXPECTED: Int = 2
private const val LLSD_SYNTAX_LSL_VERSION_KEY = "llsd-lsl-syntax-version"

object SyntaxIdLSL {

    @JvmStatic
    val instance: SyntaxIdLSL get() = this

    private val inflightFetches: MutableSet<String> = mutableSetOf()
    private val syntaxIDChangedListeners: MutableList<() -> Unit> = mutableListOf()

    private var capabilityURL: String = ""
    private var fullFileSpec: String = ""
    private var syntaxId: LLUUID = LLUUID.NULL
    private var keywordsXml: LLSD = LLSD()
    private var initialized: Boolean = false

    init {
        loadDefaultKeywordsIntoLLSD()
        System.err.println("SyntaxIdLSL: region-changed callback registration not yet implemented")
    }

    fun initialize() {
        if (initialized) return
        if (syntaxId.isNull()) {
            loadDefaultKeywordsIntoLLSD()
        } else if (capabilityURL.isNotEmpty()) {
            buildFullFileSpec()
            if (syntaxId.isNotNull()) {
                if (!File(fullFileSpec).exists()) {
                    fetchKeywordsFile(fullFileSpec)
                } else {
                    loadKeywordsIntoLLSD()
                }
            } else {
                loadDefaultKeywordsIntoLLSD()
            }
        } else {
            loadDefaultKeywordsIntoLLSD()
        }
        initialized = true
    }

    fun keywordFetchInProgress(): Boolean = inflightFetches.isNotEmpty()

    fun getKeywordsXML(): LLSD = keywordsXml

    fun addSyntaxIDCallback(cb: () -> Unit): Int {
        syntaxIDChangedListeners.add(cb)
        return syntaxIDChangedListeners.lastIndex
    }

    fun removeSyntaxIDCallback(index: Int) {
        if (index in syntaxIDChangedListeners.indices) {
            syntaxIDChangedListeners.removeAt(index)
        }
    }

    private fun buildFullFileSpec() {
        val isDefault = syntaxId.isNull()
        val filename = if (isDefault) FILENAME_DEFAULT else "keywords_lsl_${syntaxId.asString()}.llsd.xml"
        val dir = if (isDefault) appSettingsDir() else cacheDir()
        fullFileSpec = "$dir/$filename"
    }

    private fun syntaxIdChanged(): Boolean {
        System.err.println("SyntaxIdLSL: syntaxIdChanged not yet implemented")
        return false
    }

    private fun handleRegionChanged() {
        if (syntaxIdChanged()) {
            buildFullFileSpec()
            fetchKeywordsFile(fullFileSpec)
            initialized = false
        }
    }

    private fun handleCapsReceived(regionUuid: LLUUID) {
        System.err.println("SyntaxIdLSL: handleCapsReceived not yet implemented")
    }

    private fun fetchKeywordsFile(filespec: String) {
        System.err.println("SyntaxIdLSL: fetchKeywordsFile not yet implemented")
    }

    private fun fetchKeywordsFileCoro(url: String, fileSpec: String) {
        if (!inflightFetches.add(fileSpec)) {
            return
        }
        try {
            System.err.println("SyntaxIdLSL: fetchKeywordsFileCoro not yet implemented")
        } finally {
            inflightFetches.remove(fileSpec)
        }
        // On success:
        // if (isSupportedVersion(result)) { setKeywordsXml(result); cacheFile(fileSpec, result); loadKeywordsIntoLLSD() }
    }

    private fun cacheFile(fileSpec: String, content: LLSD) {
        System.err.println("SyntaxIdLSL: cacheFile not yet implemented")
    }

    private fun isSupportedVersion(content: LLSD): Boolean {
        if (!content.has(LLSD_SYNTAX_LSL_VERSION_KEY)) return false
        return content[LLSD_SYNTAX_LSL_VERSION_KEY].asInt() == LLSD_SYNTAX_LSL_VERSION_EXPECTED
    }

    private fun loadDefaultKeywordsIntoLLSD() {
        syntaxId = LLUUID.NULL
        buildFullFileSpec()
        loadKeywordsIntoLLSD()
    }

    private fun loadKeywordsIntoLLSD() {
        val file = File(fullFileSpec)
        if (file.exists()) {
            System.err.println("SyntaxIdLSL: loadKeywordsIntoLLSD not yet implemented")
        }
        fireSyntaxIDChanged()
    }

    private fun setKeywordsXml(content: LLSD) {
        keywordsXml = content
    }

    private fun fireSyntaxIDChanged() {
        syntaxIDChangedListeners.toList().forEach { it() }
    }

    private fun appSettingsDir(): String {
        System.err.println("SyntaxIdLSL: appSettingsDir not yet implemented")
        return ""
    }
    private fun cacheDir(): String {
        System.err.println("SyntaxIdLSL: cacheDir not yet implemented")
        return ""
    }
}
