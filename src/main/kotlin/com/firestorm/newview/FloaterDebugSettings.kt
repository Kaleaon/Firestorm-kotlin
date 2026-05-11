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
    private fun enableResizeCtrls(resizeWidth: Boolean, resizeHeight: Boolean) = TODO("APR: enableResizeCtrls(true, resizeWidth, resizeHeight)")
    private fun findChild(id: String): Any = TODO("APR: getChild<LLUICtrl>($id)")
    private fun setFilterEditorCallback(id: String, fn: (String) -> Unit) = TODO("APR: getChild<LLFilterEditor>(id).setCommitCallback")
    private fun setListCommitOnSelectionChange(list: Any, v: Boolean) = TODO("APR: list.setCommitOnSelectionChange(v)")
    private fun setListCommitCallback(list: Any, fn: () -> Unit) = TODO("APR: list.setCommitCallback(fn)")
    private fun connectSettingChangeSignal(key: String, fn: () -> Unit) = TODO("APR: gSavedSettings.getControl(key).getCommitSignal().connect(fn)")
    private fun getListFirstSelected(list: Any?): Any? = TODO("APR: list.getFirstSelected()")
    private fun getListSelectedCellValue(item: Any, columnIndex: Int): String = TODO("APR: item.getColumn(columnIndex).getValue().asString()")
    private fun clearList(list: Any) = TODO("APR: list.deleteAllItems()")
    private fun addListRow(list: Any, changed: String, color: String): Any = TODO("APR: list.addElement(row, ADD_BOTTOM, null)")
    private fun listItemSetSelected(item: Any, selected: Boolean) = TODO("APR: item.setSelected(selected)")
    private fun sortList(list: Any) = TODO("APR: list.updateSort()")
    private fun isListEmpty(list: Any): Boolean = TODO("APR: list.isEmpty()")
    private fun listHasSelectedItem(list: Any): Boolean = TODO("APR: list.hasSelectedItem()")
    private fun scrollListToSelected(list: Any) = TODO("APR: list.scrollToShowSelected()")
    private fun listSelectItemByPrefix(list: Any, prefix: String): Boolean = TODO("APR: list.selectItemByPrefix(prefix, false, 1)")
    private fun listSelectFirst(list: Any) = TODO("APR: list.selectFirstItem()")
    private fun getListItemCell(item: Any, columnIndex: Int): Any? = TODO("APR: item.getColumn(columnIndex)")
    private fun setListCellValue(cell: Any, value: String) = TODO("APR: cell.setValue(value)")
    private fun setVisible(view: Any, visible: Boolean) = TODO("APR: view.setVisible(visible)")
    private fun setText(view: Any, text: String) = TODO("APR: view.setText(text)")
    private fun setToolTip(view: Any, tip: String) = TODO("APR: view.setToolTip(tip)")
    private fun uiColorTableGet(name: String): Any = TODO("APR: LLUIColorTable.instance().getColor(name)")
    private fun uiColorTableSetColor(name: String, color: Any) = TODO("APR: LLUIColorTable.instance().setColor(name, color)")
    private fun uiColorTableResetToDefault(name: String) = TODO("APR: LLUIColorTable.instance().resetToDefault(name)")
    private fun uiColorTableIsDefault(name: String): Boolean = TODO("APR: LLUIColorTable.instance().isDefault(name)")
    private fun uiColorTableGetLoadedColors(): Map<String, Any> = TODO("APR: LLUIColorTable.instance().getLoadedColors()")
    private fun uiColorTableGetUserColors(): Map<String, Any> = TODO("APR: LLUIColorTable.instance().getUserColors()")
    private fun colorSwatchValue(swatch: Any): Any = TODO("APR: swatch.getValue() as LLColor3")
    private fun setColorSwatchOriginal(swatch: Any, color: Any) = TODO("APR: swatch.setOriginal(color)")
    private fun setSpinnerLabel(spinner: Any, label: String) = TODO("APR: spinner.setLabel(label)")
    private fun spinnerHasFocus(spinner: Any): Boolean = TODO("APR: spinner.hasFocus()")
    private fun setSpinnerPrecision(spinner: Any, precision: Int) = TODO("APR: spinner.setPrecision(precision)")
    private fun setSpinnerRange(spinner: Any, min: Double, max: Double) = TODO("APR: spinner.setMinValue/setMaxValue")
    private fun setSpinnerValue(spinner: Any, value: Float) = TODO("APR: spinner.setValue(value)")
    private fun spinnerValueFloat(spinner: Any): Float = TODO("APR: spinner.getValue().asReal().toFloat()")
    private fun colorAlpha(color: Any): Float = TODO("APR: color.mV[VALPHA]")
    private fun makeColor4(col3: Any, alpha: Float): Any = TODO("APR: LLColor4(col3, alpha)")
    private fun savedSettingBool(key: String): Boolean = TODO("APR: gSavedSettings cached bool $key")
    private fun super_draw() = TODO("GPU: LLFloater.draw()")
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
        TODO("APR: LLFilePickerReplyThread.startPicker for XML import -> loadListFromFile")
    }

    private fun loadListFromFile(filenames: List<String>) {
        TODO("APR: java.io.FileReader + LLSDSerialize.fromXMLDocument; then settingsAddList with conflict handling")
    }

    private fun onExportList() {
        val listName = getListFirstSelectedColumnValue(listNames!!, 0) ?: return
        TODO("APR: LLFilePickerReplyThread.startPicker for XML export -> saveListToFile(listName)")
    }

    private fun saveListToFile(filenames: List<String>, listName: String) {
        TODO("APR: java.io.FileWriter + LLSDSerialize.toPrettyXML of exported list")
    }

    private fun onNewList() {
        TODO("APR: show AddAutoReplaceList notification dialog -> callbackNewListName")
    }

    private fun onDeleteList() {
        val listName = getListSelectedValue(listNames!!) ?: return
        val size = settingsGetListSize(listName)
        if (size > 0) {
            TODO("APR: show RemoveAutoReplaceList notification with size arg -> callbackRemoveList")
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
            TODO("APR: LLNotificationsUtil.add(\"InvalidAutoReplaceEntry\")")
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
    private fun autoReplaceGetSettings(): Any = TODO("APR: LLAutoReplace.getInstance().getSettings()")
    private fun autoReplaceSetSettings(s: Any) = TODO("APR: LLAutoReplace.getInstance().setSettings(s)")
    private fun settingsGetListNames(): List<String> = TODO("APR: settings.getListNames() LLSD Array of String")
    private fun settingsGetListEntries(name: String): Map<String, String> = TODO("APR: settings.getListEntries(name)")
    private fun settingsGetListSize(name: String): Int = TODO("APR: settings.getListEntries(name).size")
    private fun settingsReplacementFor(kw: String, list: String): String = TODO("APR: settings.replacementFor(kw, list)")
    private fun settingsAddEntry(kw: String, rep: String, list: String): Boolean = TODO("APR: settings.addEntryToList(kw, rep, list)")
    private fun settingsRemoveEntry(kw: String, list: String) = TODO("APR: settings.removeEntryFromList(kw, list)")
    private fun settingsRemoveList(name: String): Boolean = TODO("APR: settings.removeReplacementList(name)")
    private fun settingsIncreaseListPriority(name: String): Boolean = TODO("APR: settings.increaseListPriority(name)")
    private fun settingsDecreaseListPriority(name: String): Boolean = TODO("APR: settings.decreaseListPriority(name)")
    private fun savedSettingBool(key: String): Boolean = TODO("APR: gSavedSettings.getBOOL($key)")
    private fun setSavedSettingBool(key: String, v: Boolean) = TODO("APR: gSavedSettings.setBOOL($key, v)")
    private fun center() = TODO("APR: LLFloater.center()")
    private fun findChild(id: String): Any = TODO("APR: getChild<LLUICtrl>($id)")
    private fun setCheckboxCallback(id: String, fn: () -> Unit) = TODO("APR: getChild<LLUICtrl>(id).setCommitCallback(fn)")
    private fun setCheckboxValue(id: String, v: Boolean) = TODO("APR: getChild<LLUICtrl>(id).setValue(v)")
    private fun bindChildCallback(id: String, fn: () -> Unit) = TODO("APR: getChild<LLUICtrl>(id).setCommitCallback(fn)")
    private fun setListCommitOnChange(list: Any, v: Boolean) = TODO("APR: list.setCommitOnSelectionChange(v)")
    private fun setListCommitCallback(list: Any, fn: () -> Unit) = TODO("APR: list.setCommitCallback(fn)")
    private fun clearList(list: Any) = TODO("APR: list.deleteAllItems()")
    private fun addListSimpleElement(list: Any, element: String) = TODO("APR: list.addSimpleElement(element)")
    private fun addReplacementRow(list: Any, kw: String, rep: String) = TODO("APR: list.addElement(row, ADD_BOTTOM)")
    private fun listSelectByValue(list: Any, value: String) = TODO("APR: list.setSelectedByValue(value, true)")
    private fun getListSelectedValue(list: Any): String? = TODO("APR: list.getSelectedValue().asString() or null")
    private fun getListFirstSelectedColumnValue(list: Any, col: Int): String? = TODO("APR: list.getFirstSelected().getColumn(col).getValue().asString()")
    private fun deselectAll(list: Any) = TODO("APR: list.deselectAllItems(false)")
    private fun deleteListSelectedItems(list: Any) = TODO("APR: list.deleteSelectedItems()")
    private fun setListEnabled(list: Any, enabled: Boolean) = TODO("APR: list.setEnabled(enabled)")
    private fun setButtonEnabled(id: String, enabled: Boolean) = TODO("APR: getChild<LLButton>(id).setEnabled(enabled)")
    private fun setControlValue(ctrl: Any, value: String) = TODO("APR: ctrl.setValue(value)")
    private fun setControlEnabled(ctrl: Any, enabled: Boolean) = TODO("APR: ctrl.setEnabled(enabled)")
    private fun clearControl(ctrl: Any) = TODO("APR: ctrl.clear()")
    private fun setFocus(ctrl: Any) = TODO("APR: ctrl.setFocus(true)")
    private fun getChildBoolValue(id: String): Boolean = TODO("APR: childGetValue(id).asBoolean()")
    private fun getWText(ctrl: Any): String = TODO("APR: wstring_to_utf8str(ctrl.getWText())")
    private fun closeFloater(appQuitting: Boolean) = TODO("APR: LLFloater.closeFloater(appQuitting)")
}
