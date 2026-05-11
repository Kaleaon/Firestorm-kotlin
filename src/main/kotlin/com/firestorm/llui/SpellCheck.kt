package com.firestorm.llui

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object SpellCheck {
    private const val DICT_DIR = "dictionaries"
    private const val DICT_FILE_CUSTOM = "user_custom.dic"
    private const val DICT_FILE_IGNORE = "user_ignore.dic"
    private const val DICT_FILE_MAIN = "dictionaries.xml"
    private const val DICT_FILE_USER = "user_dictionaries.xml"

    private var hunspell: Any? = null  // TODO("APR: bind Hunspell via JVM equivalent (e.g. JNA or hunspell-java)")

    var dictLanguage: String = ""
        private set
    private var dictFile: String = ""
    private val dictSecondary: MutableList<String> = mutableListOf()
    private val ignoreList: MutableList<String> = mutableListOf()

    private var dictMap: MutableList<MutableMap<String, Any?>> = mutableListOf()

    private val settingsChangeListeners: MutableList<() -> Unit> = mutableListOf()

    fun init() {
        refreshDictionaryMap()
    }

    fun addSettingsChangeListener(cb: () -> Unit): () -> Unit {
        settingsChangeListeners.add(cb)
        return cb
    }

    fun removeSettingsChangeListener(cb: () -> Unit) {
        settingsChangeListeners.remove(cb)
    }

    private fun fireSettingsChange() {
        for (cb in settingsChangeListeners) cb()
    }

    fun checkSpelling(word: String): Boolean {
        if (hunspell == null || word.length < 3) return true
        // TODO("APR: invoke hunspell.spell(word) via JVM binding")
        val wordLower = word.lowercase()
        return ignoreList.contains(wordLower)
    }

    fun getSuggestions(word: String, suggestions: MutableList<String>): Int {
        suggestions.clear()
        if (hunspell == null || word.length < 3) return 0
        // TODO("APR: invoke hunspell.suggest(word) via JVM binding and populate suggestions")
        return suggestions.size
    }

    fun getPrimaryDictionary(): String = dictLanguage

    fun getSecondaryDictionaries(): List<String> = dictSecondary

    fun isActiveDictionary(language: String): Boolean =
        dictLanguage == language || dictSecondary.contains(language)

    fun setSecondaryDictionaries(newList: List<String>) {
        if (!getUseSpellCheck()) return

        val toAdd = newList.filter { it !in dictSecondary }
        val toRemove = dictSecondary.filter { it !in newList }

        if (toRemove.isNotEmpty()) {
            dictSecondary.clear()
            dictSecondary.addAll(newList)
            val lang = dictLanguage
            initHunspell(lang)
        } else if (toAdd.isNotEmpty()) {
            val appPath = getDictionaryAppPath()
            val userPath = getDictionaryUserPath()
            for (lang in toAdd) {
                val entry = getDictionaryData(lang) ?: continue
                if (entry["installed"] != true) continue
                val dicName = entry["name"]?.toString() ?: continue
                val dicFile = "$dicName.dic"
                val userDic = File(userPath + dicFile)
                val appDic = File(appPath + dicFile)
                when {
                    userDic.exists() -> {
                        // TODO("APR: invoke hunspell.add_dic(userDic.path) via JVM binding")
                    }
                    appDic.exists() -> {
                        // TODO("APR: invoke hunspell.add_dic(appDic.path) via JVM binding")
                    }
                }
            }
            dictSecondary.clear()
            dictSecondary.addAll(newList)
            fireSettingsChange()
        }
    }

    fun getDictionaryData(language: String): MutableMap<String, Any?>? =
        dictMap.find { it["language"]?.toString() == language }

    fun getDictionaryMap(): List<MutableMap<String, Any?>> = dictMap

    fun getUseSpellCheck(): Boolean = hunspell != null

    fun hasDictionary(language: String, checkInstalled: Boolean = false): Boolean {
        val entry = getDictionaryData(language) ?: return false
        return !checkInstalled || entry["installed"] == true
    }

    fun canRemoveDictionary(language: String): Boolean {
        val info = getDictionaryData(language) ?: return false
        return info["user_installed"] == true && (!getUseSpellCheck() || !isActiveDictionary(language))
    }

    fun refreshDictionaryMap() {
        val appPath = getDictionaryAppPath()
        val userPath = getDictionaryUserPath()

        val loaded = tryLoadDictMap(userPath + DICT_FILE_MAIN)
            ?: tryLoadDictMap(appPath + DICT_FILE_MAIN)
            ?: return
        dictMap = loaded.toMutableList()

        val customMap = tryLoadDictMap(userPath + DICT_FILE_USER)
        if (customMap != null) {
            for (entry in customMap) {
                entry["user_installed"] = true
                setDictionaryData(entry)
            }
        }

        for (entry in dictMap) {
            val name = entry["name"]?.toString()
            val installed = if (name != null) {
                File(userPath + "$name.dic").exists() || File(appPath + "$name.dic").exists()
            } else false
            entry["installed"] = installed
        }

        fireSettingsChange()
    }

    fun addToCustomDictionary(word: String) {
        if (hunspell != null) {
            // TODO("APR: invoke hunspell.add(word) via JVM binding")
        }
        addToDictFile(getDictionaryUserPath() + DICT_FILE_CUSTOM, word)
        fireSettingsChange()
    }

    fun addToIgnoreList(word: String) {
        val wordLower = word.lowercase()
        if (!ignoreList.contains(wordLower)) {
            ignoreList.add(wordLower)
            addToDictFile(getDictionaryUserPath() + DICT_FILE_IGNORE, wordLower)
            fireSettingsChange()
        }
    }

    fun removeDictionary(language: String) {
        if (!canRemoveDictionary(language)) return

        val userPath = getDictionaryUserPath()
        val userMap = loadUserDictionaryMap().toMutableList()
        val iter = userMap.iterator()
        while (iter.hasNext()) {
            val info = iter.next()
            if (info["language"]?.toString() == language) {
                val name = info["name"]?.toString() ?: ""
                File(userPath + "$name.dic").takeIf { it.exists() }?.delete()
                File(userPath + "$name.aff").takeIf { it.exists() }?.delete()
                iter.remove()
                break
            }
        }
        saveUserDictionaryMap(userMap)
        refreshDictionaryMap()
    }

    fun setUseSpellCheck(language: String) {
        if ((language.isEmpty() && getUseSpellCheck()) || language.isNotEmpty()) {
            if (dictLanguage != language) {
                initHunspell(language)
            }
        }
    }

    fun getDictionaryAppPath(): String {
        // TODO("APR: resolve app settings path via JVM equivalent of gDirUtilp->getExpandedFilename")
        return "app_settings/$DICT_DIR/"
    }

    fun getDictionaryUserPath(): String {
        val path = "user_settings/$DICT_DIR/"
        File(path).mkdirs()
        return path
    }

    private fun initHunspell(language: String) {
        hunspell = null
        dictLanguage = ""
        dictFile = ""
        ignoreList.clear()

        if (language.isEmpty()) {
            fireSettingsChange()
            return
        }

        val entry = getDictionaryData(language)
        if (entry == null || entry["installed"] != true || entry["is_primary"] != true) {
            fireSettingsChange()
            return
        }

        val name = entry["name"]?.toString()
        if (name != null) {
            val appPath = getDictionaryAppPath()
            val userPath = getDictionaryUserPath()
            val affFile: File?
            val dicFile: File?

            when {
                File(userPath + "$name.aff").exists() && File(userPath + "$name.dic").exists() -> {
                    affFile = File(userPath + "$name.aff")
                    dicFile = File(userPath + "$name.dic")
                }
                File(appPath + "$name.aff").exists() && File(appPath + "$name.dic").exists() -> {
                    affFile = File(appPath + "$name.aff")
                    dicFile = File(appPath + "$name.dic")
                }
                else -> {
                    fireSettingsChange()
                    return
                }
            }

            // TODO("APR: create Hunspell(affFile.path, dicFile.path) via JVM binding")
            hunspell = object {}  // placeholder — replace with actual JVM Hunspell handle

            dictLanguage = language
            dictFile = name

            val customDic = File(userPath + DICT_FILE_CUSTOM)
            if (customDic.exists()) {
                // TODO("APR: invoke hunspell.add_dic(customDic.path) via JVM binding")
            }

            val ignoreDic = File(userPath + DICT_FILE_IGNORE)
            if (ignoreDic.exists()) {
                ignoreDic.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { word ->
                        ignoreList.add(word.lowercase())
                    }
                }
            }

            for (secLang in dictSecondary) {
                val secEntry = getDictionaryData(secLang) ?: continue
                if (secEntry["installed"] != true) continue
                val secName = secEntry["name"]?.toString() ?: continue
                val secDic = "$secName.dic"
                when {
                    File(userPath + secDic).exists() -> {
                        // TODO("APR: invoke hunspell.add_dic(userPath + secDic) via JVM binding")
                    }
                    File(appPath + secDic).exists() -> {
                        // TODO("APR: invoke hunspell.add_dic(appPath + secDic) via JVM binding")
                    }
                }
            }
        }

        fireSettingsChange()
    }

    private fun addToDictFile(dictPath: String, word: String) {
        val file = File(dictPath)
        val words = mutableListOf<String>()

        if (file.exists()) {
            file.bufferedReader().useLines { lines ->
                lines.drop(1).forEach { words.add(it) }
            }
        }

        words.add(word)

        file.bufferedWriter().use { out ->
            out.write(words.size.toString())
            out.newLine()
            for (w in words) {
                out.write(w)
                out.newLine()
            }
        }
    }

    private fun setDictionaryData(info: MutableMap<String, Any?>) {
        val language = info["language"]?.toString() ?: return
        val existing = dictMap.indexOfFirst { it["language"]?.toString() == language }
        if (existing >= 0) {
            dictMap[existing] = info
        } else {
            dictMap.add(info)
        }
    }

    private fun tryLoadDictMap(path: String): List<MutableMap<String, Any?>>? {
        // TODO("APR: parse XML at path using JVM XML parser (e.g. JDOM/DOM) to build list of dicts")
        val file = File(path)
        if (!file.exists()) return null
        return emptyList()
    }

    private fun loadUserDictionaryMap(): List<MutableMap<String, Any?>> {
        return tryLoadDictMap(getDictionaryUserPath() + DICT_FILE_USER) ?: emptyList()
    }

    private fun saveUserDictionaryMap(dictList: List<MutableMap<String, Any?>>) {
        // TODO("APR: serialize dictList to XML at getDictionaryUserPath() + DICT_FILE_USER using JVM XML serializer")
    }
}
