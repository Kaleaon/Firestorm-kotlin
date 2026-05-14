package com.firestorm.newview

class FloaterDebugSettings private constructor(val key: Any) {

    private var settingList: Any? = null
    private var defaultButton: Any? = null
    private var settingNameText: Any? = null
    private var alphaSpinner: Any? = null
    private var colorSwatch: Any? = null
    private var searchFilter: String = ""

    companion object {
        fun create(key: Any): FloaterDebugSettings = FloaterDebugSettings(key)
    }

    fun postBuild(): Boolean {
        enableResizeCtrls(resizeWidth = false, resizeHeight = true)

        alphaSpinner = findChild("alpha_spinner")
        colorSwatch = findChild("color_swatch")
        defaultButton = findChild("default_btn")
        settingNameText = findChild("color_name_txt")

        setFilterEditorCallback("filter_input") { filter -> setSearchFilter(filter) }

        settingList = findChild("setting_list")
        setListCommitOnSelectionChange(settingList!!, true)
        setListCommitCallback(settingList!!) { onSettingSelect() }

        updateList()

        connectSettingChangeSignal("ColorSettingsHideDefault") { updateList(skipSelection = false) }

        return true
    }

    fun draw() {
        val firstSelected = getListFirstSelected(settingList) ?: run {
            super_draw()
            return
        }
        val colorName = getListSelectedCellValue(firstSelected, columnIndex = 1)
        if (colorName.isNotEmpty()) updateControl(colorName)
        super_draw()
    }

    fun updateControl(colorName: String) {
        hideUIControls()
        if (isSettingHidden(colorName)) return

        setVisible(defaultButton!!, true)
        setVisible(settingNameText!!, true)
        setText(settingNameText!!, colorName)
        setToolTip(settingNameText!!, colorName)

        val color = uiColorTableGet(colorName)
        setVisible(colorSwatch!!, true)
        if (color != colorSwatchValue(colorSwatch!!)) {
            setColorSwatchOriginal(colorSwatch!!, color)
        }
        setVisible(alphaSpinner!!, true)
        setSpinnerLabel(alphaSpinner!!, "Alpha")
        if (!spinnerHasFocus(alphaSpinner!!)) {
            setSpinnerPrecision(alphaSpinner!!, 3)
            setSpinnerRange(alphaSpinner!!, 0.0, 1.0)
            setSpinnerValue(alphaSpinner!!, colorAlpha(color))
        }
    }

    fun onCommitSettings() {
        val firstSelected = getListFirstSelected(settingList) ?: return
        val colorName = getListSelectedCellValue(firstSelected, columnIndex = 1)
        if (colorName.isEmpty()) return

        val col3 = colorSwatchValue(colorSwatch!!)
        val alpha = spinnerValueFloat(alphaSpinner!!)
        val col4 = makeColor4(col3, alpha)
        uiColorTableSetColor(colorName, col4)
        updateDefaultColumn(colorName)
    }

    fun onClickDefault() {
        val firstSelected = getListFirstSelected(settingList) ?: return
        val colorName = getListSelectedCellValue(firstSelected, columnIndex = 1)
        if (colorName.isEmpty()) return
        uiColorTableResetToDefault(colorName)
        updateDefaultColumn(colorName)
        updateControl(colorName)
    }

    fun matchesSearchFilter(settingName: String): Boolean {
        if (searchFilter.isEmpty()) return true
        return settingName.lowercase().contains(searchFilter)
    }

    fun isSettingHidden(colorName: String): Boolean {
        val hideDefault = savedSettingBool("ColorSettingsHideDefault")
        return hideDefault && uiColorTableIsDefault(colorName)
    }

    private fun updateList(skipSelection: Boolean = false) {
        val lastSelected = getListFirstSelected(settingList)
            ?.let { getListSelectedCellValue(it, columnIndex = 1) }
            ?: ""

        clearList(settingList!!)

        val baseColors = uiColorTableGetLoadedColors()
        for ((name, _) in baseColors) {
            if (matchesSearchFilter(name) && !isSettingHidden(name)) {
                addColorRow(name, lastSelected, skipSelection, settingList!!)
            }
        }

        for ((name, _) in uiColorTableGetUserColors()) {
            if (!baseColors.containsKey(name) && matchesSearchFilter(name) && !isSettingHidden(name)) {
                addColorRow(name, lastSelected, skipSelection, settingList!!)
            }
        }

        sortList(settingList!!)

        if (!isListEmpty(settingList!!)) {
            if (listHasSelectedItem(settingList!!)) {
                scrollListToSelected(settingList!!)
            } else if (searchFilter.isNotEmpty() && !skipSelection) {
                if (!listSelectItemByPrefix(settingList!!, searchFilter)) {
                    listSelectFirst(settingList!!)
                }
                scrollListToSelected(settingList!!)
            }
        } else {
            addListRow(settingList!!, changed = "", color = "No matching colors.")
            hideUIControls()
        }
    }

    private fun addColorRow(name: String, lastSelected: String, skipSelection: Boolean, list: Any) {
        val isDefault = uiColorTableIsDefault(name)
        val changed = if (isDefault) "" else "*"
        val item = addListRow(list, changed = changed, color = name)
        if (searchFilter.isNotEmpty() && lastSelected == name && !skipSelection) {
            if (name.lowercase().startsWith(searchFilter)) {
                listItemSetSelected(item, true)
            }
        }
    }

    private fun onSettingSelect() {
        val firstSelected = getListFirstSelected(settingList) ?: return
        val colorName = getListSelectedCellValue(firstSelected, columnIndex = 1)
        if (colorName.isNotEmpty()) updateControl(colorName)
    }

    private fun setSearchFilter(filter: String) {
        val lowered = filter.lowercase()
        if (searchFilter == lowered) return
        searchFilter = lowered
        updateList()
    }

    private fun updateDefaultColumn(colorName: String) {
        if (isSettingHidden(colorName)) {
            hideUIControls()
            updateList(skipSelection = true)
            return
        }
        val item = getListFirstSelected(settingList) ?: return
        val cell = getListItemCell(item, columnIndex = 0) ?: return
        val isDefault = if (uiColorTableIsDefault(colorName)) "" else "*"
        setListCellValue(cell, isDefault)
    }

    private fun hideUIControls() {
        setVisible(colorSwatch!!, false)
        setVisible(alphaSpinner!!, false)
        setVisible(defaultButton!!, false)
        setVisible(settingNameText!!, false)
    }

    // --- stubs for platform calls ---
    private fun enableResizeCtrls(resizeWidth: Boolean, resizeHeight: Boolean) { System.err.println("FloaterDebugSettings: enableResizeCtrls not yet implemented") }
    private fun findChild(id: String): Any { System.err.println("FloaterDebugSettings: getChild<LLUICtrl>($id) not yet implemented"); return Object() }
    private fun setFilterEditorCallback(id: String, fn: (String) -> Unit) { System.err.println("FloaterDebugSettings: setFilterEditorCallback not yet implemented") }
    private fun setListCommitOnSelectionChange(list: Any, v: Boolean) { System.err.println("FloaterDebugSettings: setListCommitOnSelectionChange not yet implemented") }
    private fun setListCommitCallback(list: Any, fn: () -> Unit) { System.err.println("FloaterDebugSettings: setListCommitCallback not yet implemented") }
    private fun connectSettingChangeSignal(key: String, fn: () -> Unit) { System.err.println("FloaterDebugSettings: connectSettingChangeSignal not yet implemented") }
    private fun getListFirstSelected(list: Any?): Any? { System.err.println("FloaterDebugSettings: getListFirstSelected not yet implemented"); return null }
    private fun getListSelectedCellValue(item: Any, columnIndex: Int): String { System.err.println("FloaterDebugSettings: getListSelectedCellValue not yet implemented"); return "" }
    private fun clearList(list: Any) { System.err.println("FloaterDebugSettings: clearList not yet implemented") }
    private fun addListRow(list: Any, changed: String, color: String): Any { System.err.println("FloaterDebugSettings: addListRow not yet implemented"); return Object() }
    private fun listItemSetSelected(item: Any, selected: Boolean) { System.err.println("FloaterDebugSettings: listItemSetSelected not yet implemented") }
    private fun sortList(list: Any) { System.err.println("FloaterDebugSettings: sortList not yet implemented") }
    private fun isListEmpty(list: Any): Boolean { System.err.println("FloaterDebugSettings: isListEmpty not yet implemented"); return true }
    private fun listHasSelectedItem(list: Any): Boolean { System.err.println("FloaterDebugSettings: listHasSelectedItem not yet implemented"); return false }
    private fun scrollListToSelected(list: Any) { System.err.println("FloaterDebugSettings: scrollListToSelected not yet implemented") }
    private fun listSelectItemByPrefix(list: Any, prefix: String): Boolean { System.err.println("FloaterDebugSettings: listSelectItemByPrefix not yet implemented"); return false }
    private fun listSelectFirst(list: Any) { System.err.println("FloaterDebugSettings: listSelectFirst not yet implemented") }
    private fun getListItemCell(item: Any, columnIndex: Int): Any? { System.err.println("FloaterDebugSettings: getListItemCell not yet implemented"); return null }
    private fun setListCellValue(cell: Any, value: String) { System.err.println("FloaterDebugSettings: setListCellValue not yet implemented") }
    private fun setVisible(view: Any, visible: Boolean) { System.err.println("FloaterDebugSettings: setVisible not yet implemented") }
    private fun setText(view: Any, text: String) { System.err.println("FloaterDebugSettings: setText not yet implemented") }
    private fun setToolTip(view: Any, tip: String) { System.err.println("FloaterDebugSettings: setToolTip not yet implemented") }
    private fun uiColorTableGet(name: String): Any { System.err.println("FloaterDebugSettings: uiColorTableGet not yet implemented"); return Object() }
    private fun uiColorTableSetColor(name: String, color: Any) { System.err.println("FloaterDebugSettings: uiColorTableSetColor not yet implemented") }
    private fun uiColorTableResetToDefault(name: String) { System.err.println("FloaterDebugSettings: uiColorTableResetToDefault not yet implemented") }
    private fun uiColorTableIsDefault(name: String): Boolean { System.err.println("FloaterDebugSettings: uiColorTableIsDefault not yet implemented"); return true }
    private fun uiColorTableGetLoadedColors(): Map<String, Any> { System.err.println("FloaterDebugSettings: uiColorTableGetLoadedColors not yet implemented"); return emptyMap() }
    private fun uiColorTableGetUserColors(): Map<String, Any> { System.err.println("FloaterDebugSettings: uiColorTableGetUserColors not yet implemented"); return emptyMap() }
    private fun colorSwatchValue(swatch: Any): Any { System.err.println("FloaterDebugSettings: colorSwatchValue not yet implemented"); return Object() }
    private fun setColorSwatchOriginal(swatch: Any, color: Any) { System.err.println("FloaterDebugSettings: setColorSwatchOriginal not yet implemented") }
    private fun setSpinnerLabel(spinner: Any, label: String) { System.err.println("FloaterDebugSettings: setSpinnerLabel not yet implemented") }
    private fun spinnerHasFocus(spinner: Any): Boolean { System.err.println("FloaterDebugSettings: spinnerHasFocus not yet implemented"); return false }
    private fun setSpinnerPrecision(spinner: Any, precision: Int) { System.err.println("FloaterDebugSettings: setSpinnerPrecision not yet implemented") }
    private fun setSpinnerRange(spinner: Any, min: Double, max: Double) { System.err.println("FloaterDebugSettings: setSpinnerRange not yet implemented") }
    private fun setSpinnerValue(spinner: Any, value: Float) { System.err.println("FloaterDebugSettings: setSpinnerValue not yet implemented") }
    private fun spinnerValueFloat(spinner: Any): Float { System.err.println("FloaterDebugSettings: spinnerValueFloat not yet implemented"); return 0.0f }
    private fun colorAlpha(color: Any): Float { System.err.println("FloaterDebugSettings: colorAlpha not yet implemented"); return 1.0f }
    private fun makeColor4(col3: Any, alpha: Float): Any { System.err.println("FloaterDebugSettings: makeColor4 not yet implemented"); return Object() }
    private fun savedSettingBool(key: String): Boolean = java.util.prefs.Preferences.userRoot().getBoolean(key, false)
    private fun super_draw() { /* no-op */ }
}

class FloaterAutoReplaceSettings(val key: Any) {

    private var enabled: Boolean = false
    private var settings: Any = autoReplaceGetSettings()
    private var selectedListName: String = ""
    private var listNames: Any? = null
    private var replacementsList: Any? = null
    private var keyword: Any? = null
    private var previousKeyword: String = ""
    private var replacement: Any? = null

    fun postBuild(): Boolean {
        enabled = savedSettingBool("AutoReplace")
        settings = autoReplaceGetSettings()

        setCheckboxCallback("autoreplace_enable") { onAutoReplaceToggled() }
        setCheckboxValue("autoreplace_enable", enabled)

        bindChildCallback("autoreplace_import_list") { onImportList() }
        bindChildCallback("autoreplace_export_list") { onExportList() }
        bindChildCallback("autoreplace_new_list") { onNewList() }
        bindChildCallback("autoreplace_delete_list") { onDeleteList() }

        listNames = findChild("autoreplace_list_name")
        setListCommitOnChange(listNames!!, true)
        setListCommitCallback(listNames!!) { onSelectList() }

        bindChildCallback("autoreplace_list_up") { onListUp() }
        bindChildCallback("autoreplace_list_down") { onListDown() }

        bindChildCallback("autoreplace_add_entry") { onAddEntry() }
        bindChildCallback("autoreplace_delete_entry") { onDeleteEntry() }

        keyword = findChild("autoreplace_keyword")
        replacement = findChild("autoreplace_replacement")
        bindChildCallback("autoreplace_save_entry") { onSaveEntry() }

        bindChildCallback("autoreplace_save_changes") { onSaveChanges() }
        bindChildCallback("autoreplace_cancel") { onCancel() }

        replacementsList = findChild("autoreplace_list_replacements")
        setListCommitOnChange(replacementsList!!, true)
        setListCommitCallback(replacementsList!!) { onSelectEntry() }

        center()
        selectedListName = ""
        updateListNames()
        updateListNamesControls()
        updateReplacementsList()
        return true
    }

    fun onClose(appQuitting: Boolean) = cleanUp()

    private fun onAutoReplaceToggled() {
        enabled = getChildBoolValue("autoreplace_enable")
    }

    private fun onSelectList() {
        val prev = selectedListName
        val selected = getListSelectedValue(listNames!!)
        selectedListName = selected ?: ""
        updateListNamesControls()
        if (prev != selectedListName) updateReplacementsList()
    }

    private fun onSelectEntry() {
        val selectedRow = getListSelectedValue(replacementsList!!)
        if (selectedRow != null) {
            previousKeyword = selectedRow
            setControlValue(keyword!!, selectedRow)
            val rep = settingsReplacementFor(previousKeyword, selectedListName)
            setControlValue(replacement!!, rep)
            enableReplacementEntry()
            setFocus(replacement!!)
        } else {
            disableReplacementEntry()
        }
    }

    private fun updateListNames() {
        clearList(listNames!!)
        val names = settingsGetListNames()
        for (name in names) {
            addListSimpleElement(listNames!!, name)
        }
        if (selectedListName.isNotEmpty()) {
            listSelectByValue(listNames!!, selectedListName)
        }
    }

    private fun updateListNamesControls() {
        if (selectedListName.isEmpty()) {
            setButtonEnabled("autoreplace_export_list", false)
            setButtonEnabled("autoreplace_delete_list", false)
            setButtonEnabled("autoreplace_list_up", false)
            setButtonEnabled("autoreplace_list_down", false)
            clearList(replacementsList!!)
        } else {
            setButtonEnabled("autoreplace_export_list", true)
            setButtonEnabled("autoreplace_delete_list", true)
            setButtonEnabled("autoreplace_list_up", !selectedListIsFirst())
            setButtonEnabled("autoreplace_list_down", !selectedListIsLast())
        }
    }

    private fun updateReplacementsList() {
        clearList(replacementsList!!)
        if (selectedListName.isEmpty()) {
            setListEnabled(replacementsList!!, false)
            setButtonEnabled("autoreplace_add_entry", false)
            disableReplacementEntry()
        } else {
            val mappings = settingsGetListEntries(selectedListName)
            for ((kw, rep) in mappings) {
                addReplacementRow(replacementsList!!, kw, rep)
            }
            deselectAll(replacementsList!!)
            setListEnabled(replacementsList!!, true)
            setButtonEnabled("autoreplace_add_entry", true)
            disableReplacementEntry()
        }
    }

    private fun enableReplacementEntry() {
        setControlEnabled(keyword!!, true)
        setControlEnabled(replacement!!, true)
        setButtonEnabled("autoreplace_save_entry", true)
        setButtonEnabled("autoreplace_delete_entry", true)
    }

    private fun disableReplacementEntry() {
        previousKeyword = ""
        clearControl(keyword!!)
        setControlEnabled(keyword!!, false)
        clearControl(replacement!!)
        setControlEnabled(replacement!!, false)
        setButtonEnabled("autoreplace_save_entry", false)
        setButtonEnabled("autoreplace_delete_entry", false)
    }

    private fun onListUp() {
        val selectedName = getListSelectedValue(listNames!!) ?: return
        if (settingsIncreaseListPriority(selectedName)) {
            updateListNames()
            updateListNamesControls()
        }
    }

    private fun onListDown() {
        val selectedName = getListSelectedValue(listNames!!) ?: return
        if (settingsDecreaseListPriority(selectedName)) {
            updateListNames()
            updateListNamesControls()
        }
    }

    private fun onDeleteEntry() {
        val selectedRow = getListSelectedValue(replacementsList!!) ?: return
        deleteListSelectedItems(replacementsList!!)
        settingsRemoveEntry(selectedRow, selectedListName)
        disableReplacementEntry()
    }

    private fun onImportList() {
        System.err.println("FloaterAutoReplaceSettings: onImportList not yet implemented")
    }

    private fun loadListFromFile(filenames: List<String>) {
        System.err.println("FloaterAutoReplaceSettings: loadListFromFile not yet implemented")
    }

    private fun onExportList() {
        val listName = getListFirstSelectedColumnValue(listNames!!, 0) ?: return
        System.err.println("FloaterAutoReplaceSettings: onExportList not yet implemented")
    }

    private fun saveListToFile(filenames: List<String>, listName: String) {
        System.err.println("FloaterAutoReplaceSettings: saveListToFile not yet implemented")
    }

    private fun onNewList() {
        System.err.println("FloaterAutoReplaceSettings: onNewList not yet implemented")
    }

    private fun onDeleteList() {
        val listName = getListSelectedValue(listNames!!) ?: return
        val size = settingsGetListSize(listName)
        if (size > 0) {
            System.err.println("FloaterAutoReplaceSettings: onDeleteList (non-empty) not yet implemented")
        } else if (settingsRemoveList(listName)) {
            deleteListSelectedItems(replacementsList!!)
            selectedListName = ""
            updateListNames()
            updateListNamesControls()
            updateReplacementsList()
        }
    }

    private fun onAddEntry() {
        previousKeyword = ""
        deselectAll(replacementsList!!)
        clearControl(keyword!!)
        clearControl(replacement!!)
        enableReplacementEntry()
        setFocus(keyword!!)
    }

    private fun onSaveEntry() {
        if (previousKeyword.isNotEmpty()) {
            settingsRemoveEntry(previousKeyword, selectedListName)
        }
        val kw = getWText(keyword!!)
        val rep = getWText(replacement!!)
        if (settingsAddEntry(kw, rep, selectedListName)) {
            updateReplacementsList()
        } else {
            System.err.println("FloaterAutoReplaceSettings: InvalidAutoReplaceEntry not yet implemented")
        }
    }

    private fun onCancel() {
        cleanUp()
        closeFloater(false)
    }

    private fun onSaveChanges() {
        autoReplaceSetSettings(settings)
        setSavedSettingBool("AutoReplace", enabled)
        cleanUp()
        closeFloater(false)
    }

    private fun cleanUp() = Unit

    private fun selectedListIsFirst(): Boolean {
        if (selectedListName.isEmpty()) return false
        val names = settingsGetListNames()
        return names.firstOrNull() == selectedListName
    }

    private fun selectedListIsLast(): Boolean {
        if (selectedListName.isEmpty()) return false
        val names = settingsGetListNames()
        return names.lastOrNull() == selectedListName
    }

    // --- stubs ---
    private fun autoReplaceGetSettings(): Any { System.err.println("FloaterAutoReplaceSettings: autoReplaceGetSettings not yet implemented"); return Object() }
    private fun autoReplaceSetSettings(s: Any) { System.err.println("FloaterAutoReplaceSettings: autoReplaceSetSettings not yet implemented") }
    private fun settingsGetListNames(): List<String> { System.err.println("FloaterAutoReplaceSettings: settingsGetListNames not yet implemented"); return emptyList() }
    private fun settingsGetListEntries(name: String): Map<String, String> { System.err.println("FloaterAutoReplaceSettings: settingsGetListEntries not yet implemented"); return emptyMap() }
    private fun settingsGetListSize(name: String): Int { System.err.println("FloaterAutoReplaceSettings: settingsGetListSize not yet implemented"); return 0 }
    private fun settingsReplacementFor(kw: String, list: String): String { System.err.println("FloaterAutoReplaceSettings: settingsReplacementFor not yet implemented"); return "" }
    private fun settingsAddEntry(kw: String, rep: String, list: String): Boolean { System.err.println("FloaterAutoReplaceSettings: settingsAddEntry not yet implemented"); return false }
    private fun settingsRemoveEntry(kw: String, list: String) { System.err.println("FloaterAutoReplaceSettings: settingsRemoveEntry not yet implemented") }
    private fun settingsRemoveList(name: String): Boolean { System.err.println("FloaterAutoReplaceSettings: settingsRemoveList not yet implemented"); return false }
    private fun settingsIncreaseListPriority(name: String): Boolean { System.err.println("FloaterAutoReplaceSettings: settingsIncreaseListPriority not yet implemented"); return false }
    private fun settingsDecreaseListPriority(name: String): Boolean { System.err.println("FloaterAutoReplaceSettings: settingsDecreaseListPriority not yet implemented"); return false }
    private fun savedSettingBool(key: String): Boolean = java.util.prefs.Preferences.userRoot().getBoolean(key, false)
    private fun setSavedSettingBool(key: String, v: Boolean) { java.util.prefs.Preferences.userRoot().putBoolean(key, v) }
    private fun center() { System.err.println("FloaterAutoReplaceSettings: center not yet implemented") }
    private fun findChild(id: String): Any { System.err.println("FloaterAutoReplaceSettings: findChild($id) not yet implemented"); return Object() }
    private fun setCheckboxCallback(id: String, fn: () -> Unit) { System.err.println("FloaterAutoReplaceSettings: setCheckboxCallback not yet implemented") }
    private fun setCheckboxValue(id: String, v: Boolean) { System.err.println("FloaterAutoReplaceSettings: setCheckboxValue not yet implemented") }
    private fun bindChildCallback(id: String, fn: () -> Unit) { System.err.println("FloaterAutoReplaceSettings: bindChildCallback not yet implemented") }
    private fun setListCommitOnChange(list: Any, v: Boolean) { System.err.println("FloaterAutoReplaceSettings: setListCommitOnChange not yet implemented") }
    private fun setListCommitCallback(list: Any, fn: () -> Unit) { System.err.println("FloaterAutoReplaceSettings: setListCommitCallback not yet implemented") }
    private fun clearList(list: Any) { System.err.println("FloaterAutoReplaceSettings: clearList not yet implemented") }
    private fun addListSimpleElement(list: Any, element: String) { System.err.println("FloaterAutoReplaceSettings: addListSimpleElement not yet implemented") }
    private fun addReplacementRow(list: Any, kw: String, rep: String) { System.err.println("FloaterAutoReplaceSettings: addReplacementRow not yet implemented") }
    private fun listSelectByValue(list: Any, value: String) { System.err.println("FloaterAutoReplaceSettings: listSelectByValue not yet implemented") }
    private fun getListSelectedValue(list: Any): String? { System.err.println("FloaterAutoReplaceSettings: getListSelectedValue not yet implemented"); return null }
    private fun getListFirstSelectedColumnValue(list: Any, col: Int): String? { System.err.println("FloaterAutoReplaceSettings: getListFirstSelectedColumnValue not yet implemented"); return null }
    private fun deselectAll(list: Any) { System.err.println("FloaterAutoReplaceSettings: deselectAll not yet implemented") }
    private fun deleteListSelectedItems(list: Any) { System.err.println("FloaterAutoReplaceSettings: deleteListSelectedItems not yet implemented") }
    private fun setListEnabled(list: Any, enabled: Boolean) { System.err.println("FloaterAutoReplaceSettings: setListEnabled not yet implemented") }
    private fun setButtonEnabled(id: String, enabled: Boolean) { System.err.println("FloaterAutoReplaceSettings: setButtonEnabled not yet implemented") }
    private fun setControlValue(ctrl: Any, value: String) { System.err.println("FloaterAutoReplaceSettings: setControlValue not yet implemented") }
    private fun setControlEnabled(ctrl: Any, enabled: Boolean) { System.err.println("FloaterAutoReplaceSettings: setControlEnabled not yet implemented") }
    private fun clearControl(ctrl: Any) { System.err.println("FloaterAutoReplaceSettings: clearControl not yet implemented") }
    private fun setFocus(ctrl: Any) { System.err.println("FloaterAutoReplaceSettings: setFocus not yet implemented") }
    private fun getChildBoolValue(id: String): Boolean { System.err.println("FloaterAutoReplaceSettings: getChildBoolValue not yet implemented"); return false }
    private fun getWText(ctrl: Any): String { System.err.println("FloaterAutoReplaceSettings: getWText not yet implemented"); return "" }
    private fun closeFloater(appQuitting: Boolean) { System.err.println("FloaterAutoReplaceSettings: closeFloater not yet implemented") }
}
