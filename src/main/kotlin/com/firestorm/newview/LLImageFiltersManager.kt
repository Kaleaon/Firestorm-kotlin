package com.firestorm.newview

import java.io.File

object LLImageFiltersManager {

    private val mFiltersList: MutableMap<String, String> = mutableMapOf()

    init {
        loadAllFilters()
    }

    private fun getSysDir(): String {
        TODO("APR: use JVM equivalent of gDirUtilp->getExpandedFilename(LL_PATH_APP_SETTINGS, \"filters\", \"\")")
    }

    private fun loadAllFilters() {
        loadFiltersFromDir(getSysDir())
    }

    private fun loadFiltersFromDir(dir: String) {
        mFiltersList.clear()

        val directory = File(dir)
        val xmlFiles = directory.listFiles { f -> f.extension == "xml" } ?: return

        for (file in xmlFiles) {
            val filterNameUntranslated = file.nameWithoutExtension
            val filterName = translateFilterName(filterNameUntranslated) ?: filterNameUntranslated
            mFiltersList[filterName] = file.name
        }
    }

    private fun translateFilterName(key: String): String? {
        TODO("APR: use JVM equivalent of LLTrans::findString for localization lookup")
    }

    fun getFiltersList(): List<String> = mFiltersList.keys.toList()

    fun getFilterPath(filterName: String): String {
        val fileName = mFiltersList[filterName] ?: return ""
        val dir = getSysDir()
        TODO("APR: use JVM equivalent of gDirUtilp->add(dir, fileName) to build path")
    }
}
