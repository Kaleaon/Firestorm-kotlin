package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llui.LLSD
import java.io.File
import java.net.URL

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
        TODO("APR: use JVM equivalent - register region-changed callback and call handleRegionChanged()")
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
        TODO("APR: use JVM equivalent - query gAgent.getRegion() for simulator features and LSLSyntaxId UUID")
    }

    private fun handleRegionChanged() {
        if (syntaxIdChanged()) {
            buildFullFileSpec()
            fetchKeywordsFile(fullFileSpec)
            initialized = false
        }
    }

    private fun handleCapsReceived(regionUuid: LLUUID) {
        TODO("APR: use JVM equivalent - check if regionUuid matches current region, then call syntaxIdChanged()")
    }

    private fun fetchKeywordsFile(filespec: String) {
        TODO("APR: use JVM equivalent - launch coroutine fetchKeywordsFileCoro(capabilityURL, filespec)")
    }

    private fun fetchKeywordsFileCoro(url: String, fileSpec: String) {
        if (!inflightFetches.add(fileSpec)) {
            return
        }
        try {
            TODO("APR: use JVM equivalent - HTTP GET url, deserialize LLSD XML response")
        } finally {
            inflightFetches.remove(fileSpec)
        }
        // On success:
        // if (isSupportedVersion(result)) { setKeywordsXml(result); cacheFile(fileSpec, result); loadKeywordsIntoLLSD() }
    }

    private fun cacheFile(fileSpec: String, content: LLSD) {
        TODO("APR: use JVM equivalent - serialize content to XML and write to fileSpec")
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
            TODO("APR: use JVM equivalent - deserialize LLSD XML from file into keywordsXml")
        }
        fireSyntaxIDChanged()
    }

    private fun setKeywordsXml(content: LLSD) {
        keywordsXml = content
    }

    private fun fireSyntaxIDChanged() {
        syntaxIDChangedListeners.toList().forEach { it() }
    }

    private fun appSettingsDir(): String = TODO("APR: use JVM equivalent - return app settings directory path")
    private fun cacheDir(): String = TODO("APR: use JVM equivalent - return viewer cache directory path")
}
