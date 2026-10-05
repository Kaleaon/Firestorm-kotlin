package com.firestorm.newview

import com.firestorm.ui.LLFloater
import com.firestorm.ui.LLComboBox
import com.firestorm.ui.LLScrollListCtrl
import com.firestorm.ui.LLScrollListItem
import com.firestorm.ui.LLUICtrl
import com.firestorm.llsd.LLSD
import com.firestorm.spell.LLSpellChecker
import com.firestorm.floater.LLFloaterReg
import com.firestorm.i18n.LLTrans

class LLFloaterSpellCheckerSettings(key: LLSD) : LLFloater(key) {

    protected var mMainSelectionChanged: Boolean = false

    override fun draw() {
        super.draw()
        val selItems: List<LLScrollListItem> = getChild<LLScrollListCtrl>("spellcheck_available_list").getAllSelected()
        var enableRemove = selItems.isNotEmpty()
        for (item in selItems) {
            enableRemove = enableRemove && LLSpellChecker.getInstance().canRemoveDictionary(item.getValue().asString())
        }
        getChild<LLUICtrl>("spellcheck_remove_btn").setEnabled(enableRemove)
    }

    override fun postBuild(): Boolean {
        gSavedSettings.getControl("SpellCheck")
            ?.getSignal()
            ?.add { refreshDictionaries(false) }

        LLSpellChecker.setSettingsChangeCallback { onSpellCheckSettingsChange() }

        getChild<LLUICtrl>("spellcheck_remove_btn").setCommitCallback { onBtnRemove() }
        getChild<LLUICtrl>("spellcheck_import_btn").setCommitCallback { onBtnImport() }
        getChild<LLUICtrl>("spellcheck_main_combo").setCommitCallback { _, _ ->
            mMainSelectionChanged = true
            refreshDictionaries(false)
        }
        getChild<LLUICtrl>("spellcheck_moveleft_btn").setCommitCallback {
            onBtnMove("spellcheck_active_list", "spellcheck_available_list")
        }
        getChild<LLUICtrl>("spellcheck_moveright_btn").setCommitCallback {
            onBtnMove("spellcheck_available_list", "spellcheck_active_list")
        }
        center()
        return true
    }

    fun onOpen(key: LLSD) {
        refreshDictionaries(true)
    }

    fun onClose(appQuitting: Boolean) {
        if (appQuitting) return
        LLFloaterReg.hideInstance("prefs_spellchecker_import")
        commitChanges()
    }

    protected fun onBtnImport() {
        LLFloaterReg.showInstance("prefs_spellchecker_import")
    }

    protected fun onBtnMove(from: String, to: String) {
        val fromCtrl: LLScrollListCtrl = findChild(from) ?: return
        val toCtrl: LLScrollListCtrl = findChild(to) ?: return

        val row = LLSD()
        row["columns"][0]["column"] = "name"

        val selItems: List<LLScrollListItem> = fromCtrl.getAllSelected()
        for (item in selItems) {
            row["value"] = item.getValue()
            row["columns"][0]["value"] = item.getColumn(0)?.getValue()
            toCtrl.addElement(row)
            toCtrl.setSelectedByValue(item.getValue(), true)
        }
        fromCtrl.deleteSelectedItems()
        commitChanges()
    }

    protected fun onBtnRemove() {
        val selItems: List<LLScrollListItem> = getChild<LLScrollListCtrl>("spellcheck_available_list").getAllSelected()
        for (item in selItems) {
            LLSpellChecker.instance().removeDictionary(item.getValue().asString())
        }
    }

    protected fun onSpellCheckSettingsChange() {
        refreshDictionaries(!mMainSelectionChanged)
    }

    protected fun refreshDictionaries(fromSettings: Boolean) {
        val enabled: Boolean = gSavedSettings.getBOOL("SpellCheck")
        getChild<LLUICtrl>("spellcheck_moveleft_btn").setEnabled(enabled)
        getChild<LLUICtrl>("spellcheck_moveright_btn").setEnabled(enabled)

        val dictCombo: LLComboBox = findChild("spellcheck_main_combo") ?: return
        var dictCur: String = dictCombo.getSelectedItemLabel()
        if ((dictCur.isEmpty() || fromSettings) && LLSpellChecker.getUseSpellCheck()) {
            dictCur = LLSpellChecker.instance().getPrimaryDictionary()
        }
        dictCombo.clearRows()

        val dictMap: LLSD = LLSpellChecker.getInstance().getDictionaryMap()
        if (dictMap.size() > 0) {
            for (dict in dictMap.asArray()) {
                if (dict["installed"].asBoolean() && dict["is_primary"].asBoolean() && dict.has("language")) {
                    dictCombo.add(dict["language"].asString())
                }
            }
            if (!dictCombo.selectByValue(dictCur)) {
                dictCombo.clear()
            }
        }
        dictCombo.sortByName()
        dictCombo.setEnabled(enabled)

        val availCtrl: LLScrollListCtrl = findChild("spellcheck_available_list") ?: return
        val activeCtrl: LLScrollListCtrl = findChild("spellcheck_active_list") ?: return

        val activeList: MutableList<String> = mutableListOf()
        if ((availCtrl.getItemCount() == 0 && activeCtrl.getItemCount() == 0) || fromSettings) {
            if (LLSpellChecker.getUseSpellCheck()) {
                activeList.addAll(LLSpellChecker.instance().getSecondaryDictionaries())
            }
        } else {
            val activeItems: List<LLScrollListItem> = activeCtrl.getAllData()
            for (item in activeItems) {
                val dict = item.getValue().asString()
                if (dict != dictCur) {
                    activeList.add(dict)
                }
            }
        }

        val row = LLSD()
        row["columns"][0]["column"] = "name"

        activeCtrl.clearRows()
        activeCtrl.setEnabled(enabled)
        for (language in activeList) {
            val dict: LLSD = LLSpellChecker.getInstance().getDictionaryData(language)
            row["value"] = language
            val label = if (!dict["user_installed"].asBoolean()) {
                language
            } else {
                "$language ${LLTrans.getString("UserDictionary")}"
            }
            row["columns"][0]["value"] = label
            activeCtrl.addElement(row)
        }
        activeCtrl.sortByColumnIndex(0, true)
        activeList.add(dictCur)

        availCtrl.clearRows()
        availCtrl.setEnabled(enabled)
        for (dict in dictMap.asArray()) {
            val language = dict["language"].asString()
            if (dict["installed"].asBoolean() && !activeList.contains(language)) {
                row["value"] = language
                val label = if (!dict["user_installed"].asBoolean()) {
                    language
                } else {
                    "$language ${LLTrans.getString("UserDictionary")}"
                }
                row["columns"][0]["value"] = label
                availCtrl.addElement(row)
            }
        }
        availCtrl.sortByColumnIndex(0, true)
        commitChanges()
    }

    private fun commitChanges() {
        val listDict: MutableList<String> = mutableListOf()
        val dictCombo: LLComboBox = findChild("spellcheck_main_combo") ?: return
        val dictName: String = dictCombo.getSelectedItemLabel()
        if (dictName.isNotEmpty()) {
            listDict.add(dictName)
            val listCtrl: LLScrollListCtrl = findChild("spellcheck_active_list") ?: return
            val listItems: List<LLScrollListItem> = listCtrl.getAllData()
            for (item in listItems) {
                val language = item.getValue().asString()
                if (LLSpellChecker.getInstance().hasDictionary(language, true)) {
                    listDict.add(language)
                }
            }
        }
        gSavedSettings.setString("SpellCheckDictionary", listDict.joinToString(","))
    }
}

class LLFloaterSpellCheckerImport(key: LLSD) : LLFloater(key) {

    protected var mDictionaryDir: String = ""
    protected var mDictionaryBasename: String = ""

    override fun postBuild(): Boolean {
        getChild<LLUICtrl>("dictionary_path_browse").setCommitCallback { onBtnBrowse() }
        getChild<LLUICtrl>("ok_btn").setCommitCallback { onBtnOK() }
        getChild<LLUICtrl>("cancel_btn").setCommitCallback { onBtnCancel() }
        center()
        return true
    }

    protected fun onBtnBrowse() {
        System.err.println("LLFloaterSpellCheckerImport: onBtnBrowse not yet implemented")
    }

    protected fun importSelectedDictionary(filenames: List<String>) {
        var filepath = filenames[0]
        val extension = gDirUtilp.getExtension(filepath)
        if (extension == "xcu") {
            filepath = parseXcuFile(filepath)
            if (filepath.isEmpty()) return
        }
        getChild<LLUICtrl>("dictionary_path").setValue(filepath)
        mDictionaryDir = gDirUtilp.getDirName(filepath)
        mDictionaryBasename = gDirUtilp.getBaseFileName(filepath, true)
        getChild<LLUICtrl>("dictionary_name").setValue(mDictionaryBasename)
    }

    protected fun onBtnCancel() {
        closeFloater(false)
    }

    protected fun onBtnOK() {
        val sep = gDirUtilp.getDirDelimiter()
        val dictDic = "$mDictionaryDir$sep$mDictionaryBasename.dic"
        val dictAff = "$mDictionaryDir$sep$mDictionaryBasename.aff"
        var dictLanguage = getChild<LLUICtrl>("dictionary_language").getValue().asString().trim()

        var imported = false
        if (dictLanguage.isEmpty() || mDictionaryDir.isEmpty() || mDictionaryBasename.isEmpty() || !gDirUtilp.fileExists(dictDic)) {
            LLNotificationsUtil.add("SpellingDictImportRequired")
        } else {
            val settingsDic = LLSpellChecker.getDictionaryUserPath() + "$mDictionaryBasename.dic"
            if (LLFile.copy(dictDic, settingsDic)) {
                if (gDirUtilp.fileExists(dictAff)) {
                    val settingsAff = LLSpellChecker.getDictionaryUserPath() + "$mDictionaryBasename.aff"
                    if (LLFile.copy(dictAff, settingsAff)) {
                        imported = true
                    } else {
                        LLNotificationsUtil.add("SpellingDictImportFailed", mapOf("FROM_NAME" to dictAff, "TO_NAME" to settingsAff))
                    }
                } else {
                    LLNotificationsUtil.add("SpellingDictIsSecondary", mapOf("DIC_NAME" to dictDic))
                    imported = true
                }
            } else {
                LLNotificationsUtil.add("SpellingDictImportFailed", mapOf("FROM_NAME" to dictDic, "TO_NAME" to settingsDic))
            }
        }

        if (imported) {
            val customDictInfo = LLSD()
            customDictInfo["is_primary"] = gDirUtilp.fileExists(dictAff)
            customDictInfo["name"] = mDictionaryBasename
            customDictInfo["language"] = dictLanguage

            val customFilename = LLSpellChecker.getDictionaryUserPath() + "user_dictionaries.xml"
            val customDictMap: LLSD = loadCustomDictMap(customFilename)

            var found = false
            for (i in 0 until customDictMap.arraySize()) {
                val dictInfo = customDictMap.getArrayElement(i)
                if (dictInfo["name"].asString() == mDictionaryBasename) {
                    customDictMap.setArrayElement(i, customDictInfo)
                    found = true
                    break
                }
            }
            if (!found) {
                customDictMap.append(customDictInfo)
            }

            saveCustomDictMap(customFilename, customDictMap)
            LLSpellChecker.getInstance().refreshDictionaryMap()
        }

        closeFloater(false)
    }

    private fun loadCustomDictMap(filename: String): LLSD {
        System.err.println("LLFloaterSpellCheckerImport: loadCustomDictMap not yet implemented")
        return LLSD()
    }

    private fun saveCustomDictMap(filename: String, dictMap: LLSD) {
        System.err.println("LLFloaterSpellCheckerImport: saveCustomDictMap not yet implemented")
    }

    protected fun parseXcuFile(filePath: String): String {
        System.err.println("LLFloaterSpellCheckerImport: parseXcuFile not yet implemented")
        return ""
    }
}
