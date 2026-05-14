package com.firestorm.newview

import com.firestorm.llui.Button
import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.Floater
import com.firestorm.llui.LineEditor
import com.firestorm.llui.ScrollListCtrl
import com.firestorm.llui.UICtrl

class FloaterAutoreplaceSettings(key: Any) : Floater(key) {

    // Local copy of global enable flag; committed to persistent settings only on Save.
    private var enabled: Boolean = false

    // Working copy of settings; discarded on Cancel, applied on Save.
    private var settings: AutoReplaceSettings = AutoReplaceSettings()

    private var selectedListName: String = ""

    private var listNames: ScrollListCtrl? = null
    private var replacementsList: ScrollListCtrl? = null
    private var keyword: LineEditor? = null
    private var previousKeyword: String = ""
    private var replacement: LineEditor? = null

    override fun postBuild(): Boolean {
        enabled = SavedSettings.getBool("AutoReplace")
        settings = AutoReplaceSettings.copyFrom(AutoReplace)

        val enabledCheckbox = getChild<UICtrl>("autoreplace_enable")
        enabledCheckbox.setCommitCallback { onAutoReplaceToggled() }
        enabledCheckbox.setValue(enabled)

        getChild<UICtrl>("autoreplace_import_list").setCommitCallback { onImportList() }
        getChild<UICtrl>("autoreplace_export_list").setCommitCallback { onExportList() }
        getChild<UICtrl>("autoreplace_new_list").setCommitCallback { onNewList() }
        getChild<UICtrl>("autoreplace_delete_list").setCommitCallback { onDeleteList() }

        listNames = getChild("autoreplace_list_name")
        listNames!!.setCommitCallback { onSelectList() }
        listNames!!.setCommitOnSelectionChange(true)

        getChild<UICtrl>("autoreplace_list_up").setCommitCallback { onListUp() }
        getChild<UICtrl>("autoreplace_list_down").setCommitCallback { onListDown() }

        getChild<UICtrl>("autoreplace_add_entry").setCommitCallback { onAddEntry() }
        getChild<UICtrl>("autoreplace_delete_entry").setCommitCallback { onDeleteEntry() }

        keyword = getChild("autoreplace_keyword")
        replacement = getChild("autoreplace_replacement")
        getChild<UICtrl>("autoreplace_save_entry").setCommitCallback { onSaveEntry() }

        getChild<UICtrl>("autoreplace_save_changes").setCommitCallback { onSaveChanges() }
        getChild<UICtrl>("autoreplace_cancel").setCommitCallback { onCancel() }

        replacementsList = getChild("autoreplace_list_replacements")
        replacementsList!!.setCommitCallback { onSelectEntry() }
        replacementsList!!.setCommitOnSelectionChange(true)

        center()
        selectedListName = ""
        updateListNames()
        updateListNamesControls()
        updateReplacementsList()
        return true
    }

    override fun onClose(appQuitting: Boolean) {
        cleanUp()
    }

    private fun updateListNames() {
        listNames!!.deleteAllItems()
        for (name in settings.listNames()) {
            listNames!!.addSimpleElement(name)
        }
        if (selectedListName.isNotEmpty()) {
            listNames!!.setSelectedByValue(selectedListName, true)
        }
    }

    private fun updateListNamesControls() {
        if (selectedListName.isEmpty()) {
            getChild<Button>("autoreplace_export_list").setEnabled(false)
            getChild<Button>("autoreplace_delete_list").setEnabled(false)
            getChild<Button>("autoreplace_list_up").setEnabled(false)
            getChild<Button>("autoreplace_list_down").setEnabled(false)
            replacementsList!!.deleteAllItems()
        } else {
            getChild<Button>("autoreplace_export_list").setEnabled(true)
            getChild<Button>("autoreplace_delete_list").setEnabled(true)
            getChild<Button>("autoreplace_list_up").setEnabled(!selectedListIsFirst())
            getChild<Button>("autoreplace_list_down").setEnabled(!selectedListIsLast())
        }
    }

    private fun onSelectList() {
        val previousSelectedListName = selectedListName
        val selected = listNames!!.getSelectedValue()
        selectedListName = selected ?: ""

        updateListNamesControls()
        if (previousSelectedListName != selectedListName) {
            updateReplacementsList()
        }
    }

    private fun onSelectEntry() {
        val selectedRow = replacementsList!!.getSelectedValue()
        if (selectedRow != null) {
            previousKeyword = selectedRow
            keyword!!.setValue(selectedRow)
            val rep = settings.replacementFor(previousKeyword, selectedListName)
            replacement!!.setValue(rep)
            enableReplacementEntry()
            replacement!!.setFocus(true)
        } else {
            disableReplacementEntry()
        }
    }

    private fun updateReplacementsList() {
        replacementsList!!.deleteAllItems()
        if (selectedListName.isEmpty()) {
            replacementsList!!.setEnabled(false)
            getChild<Button>("autoreplace_add_entry").setEnabled(false)
            disableReplacementEntry()
        } else {
            val mappings = settings.getListEntries(selectedListName) ?: emptyMap()
            for ((kw, rep) in mappings) {
                replacementsList!!.addElement(mapOf(
                    "id" to kw,
                    "keyword" to kw,
                    "replacement" to rep
                ))
            }
            replacementsList!!.deselectAllItems(false)
            replacementsList!!.setEnabled(true)
            getChild<Button>("autoreplace_add_entry").setEnabled(true)
            disableReplacementEntry()
        }
    }

    private fun enableReplacementEntry() {
        keyword!!.setEnabled(true)
        replacement!!.setEnabled(true)
        getChild<Button>("autoreplace_save_entry").setEnabled(true)
        getChild<Button>("autoreplace_delete_entry").setEnabled(true)
    }

    private fun disableReplacementEntry() {
        previousKeyword = ""
        keyword!!.clear()
        keyword!!.setEnabled(false)
        replacement!!.clear()
        replacement!!.setEnabled(false)
        getChild<Button>("autoreplace_save_entry").setEnabled(false)
        getChild<Button>("autoreplace_delete_entry").setEnabled(false)
    }

    private fun onAutoReplaceToggled() {
        enabled = getChildValue("autoreplace_enable") as? Boolean ?: false
    }

    private fun onListUp() {
        val selectedName = listNames!!.getSelectedValue() ?: return
        if (settings.increaseListPriority(selectedName)) {
            updateListNames()
            updateListNamesControls()
        }
    }

    private fun onListDown() {
        val selectedName = listNames!!.getSelectedValue() ?: return
        if (settings.decreaseListPriority(selectedName)) {
            updateListNames()
            updateListNamesControls()
        }
    }

    private fun onDeleteEntry() {
        val selectedRow = replacementsList!!.getSelectedValue() ?: return
        replacementsList!!.deleteSelectedItems()
        settings.removeEntryFromList(selectedRow, selectedListName)
        disableReplacementEntry()
    }

    private fun onImportList() {
        System.err.println("FloaterAutoreplaceSettings: onImportList not yet implemented")
    }

    private fun loadListFromFile(filenames: MutableList<String>) {
        System.err.println("FloaterAutoreplaceSettings: loadListFromFile not yet implemented")
    }

    private fun onNewList() {
        NotificationsUtil.add(
            "AddAutoReplaceList",
            args = emptyMap(),
            payload = emptyMap<String, Any>()
        ) { notification, response -> callbackNewListName(notification, response) }
    }

    private fun callbackNewListName(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        val newList = (notification["payload"] as? Map<*, *>)?.get("list")
        val option = NotificationsUtil.getSelectedOption(notification, response)
        if (option != 1) return false

        val newName = response["listname"] as? String ?: return false
        val listData = newList ?: return false
        return handleAddListResult(settings.addListNamed(newName, listData), newName, notification)
    }

    private fun callbackListNameConflict(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        val newList = (notification["payload"] as? Map<*, *>)?.get("list") ?: return false
        val listName = settings.getListName(newList) ?: ""

        when (NotificationsUtil.getSelectedOption(notification, response)) {
            0 -> {
                // Replace existing list
                if (settings.replaceList(newList) == AddListResult.OK) {
                    selectedListName = listName
                    updateListNames()
                    updateListNamesControls()
                    updateReplacementsList()
                }
            }
            1 -> callbackNewListName(notification, response)
        }
        return false
    }

    private fun callbackRemoveList(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        val listName = (notification["payload"] as? Map<*, *>)?.get("list") as? String ?: return false
        if (NotificationsUtil.getSelectedOption(notification, response) == 1) {
            if (settings.removeReplacementList(listName)) {
                replacementsList!!.deleteSelectedItems()
                selectedListName = ""
                updateListNames()
                updateListNamesControls()
                updateReplacementsList()
            }
        }
        return false
    }

    private fun handleAddListResult(result: AddListResult, newName: String, notification: Map<String, Any>): Boolean {
        return when (result) {
            AddListResult.OK -> {
                selectedListName = newName
                updateListNames()
                updateListNamesControls()
                updateReplacementsList()
                false
            }
            AddListResult.DUPLICATE_NAME -> {
                NotificationsUtil.add(
                    "RenameAutoReplaceList",
                    args = mapOf("DUPNAME" to newName),
                    payload = notification["payload"] as? Map<String, Any> ?: emptyMap()
                ) { n, r -> callbackListNameConflict(n, r) }
                false
            }
            AddListResult.INVALID_LIST -> {
                NotificationsUtil.add("InvalidAutoReplaceList")
                selectedListName = ""
                updateListNames()
                updateListNamesControls()
                updateReplacementsList()
                false
            }
        }
    }

    private fun onDeleteList() {
        val listName = listNames!!.getSelectedValue() ?: return
        if (listName.isEmpty()) return

        val mappings = settings.getListEntries(selectedListName)
        if (mappings != null && mappings.isNotEmpty()) {
            NotificationsUtil.add(
                "RemoveAutoReplaceList",
                args = mapOf("MAP_SIZE" to mappings.size.toString(), "LIST_NAME" to listName),
                payload = mapOf("list" to listName)
            ) { n, r -> callbackRemoveList(n, r) }
        } else if (settings.removeReplacementList(listName)) {
            replacementsList!!.deleteSelectedItems()
            selectedListName = ""
            updateListNames()
            updateListNamesControls()
            updateReplacementsList()
        }
    }

    private fun onExportList() {
        val listName = listNames!!.getFirstSelected()?.getColumn(0)?.getValue() as? String ?: return
        System.err.println("FloaterAutoreplaceSettings: onExportList not yet implemented")
    }

    private fun saveListToFile(filenames: MutableList<String>, listName: String) {
        System.err.println("FloaterAutoreplaceSettings: saveListToFile not yet implemented")
    }

    private fun onAddEntry() {
        previousKeyword = ""
        replacementsList!!.deselectAllItems(false)
        keyword!!.clear()
        replacement!!.clear()
        enableReplacementEntry()
        keyword!!.setFocus(true)
    }

    private fun onSaveEntry() {
        if (previousKeyword.isNotEmpty()) {
            settings.removeEntryFromList(previousKeyword, selectedListName)
        }
        val kw = keyword!!.getText()
        val rep = replacement!!.getText()
        if (settings.addEntryToList(kw, rep, selectedListName)) {
            updateReplacementsList()
        } else {
            NotificationsUtil.add("InvalidAutoReplaceEntry")
        }
    }

    private fun onCancel() {
        cleanUp()
        closeFloater(false)
    }

    private fun onSaveChanges() {
        AutoReplace.applySettings(settings)
        SavedSettings.setBool("AutoReplace", enabled)
        cleanUp()
        closeFloater(false)
    }

    private fun cleanUp() {
        // Nothing to release in the Kotlin port.
    }

    private fun selectedListIsFirst(): Boolean {
        if (selectedListName.isEmpty()) return false
        return settings.listNames().firstOrNull() == selectedListName
    }

    private fun selectedListIsLast(): Boolean {
        if (selectedListName.isEmpty()) return false
        return settings.listNames().lastOrNull() == selectedListName
    }
}

// Snapshot of AutoReplace settings operated on by the floater; isolated from
// the live singleton so edits can be committed or discarded atomically.
class AutoReplaceSettings {
    private val lists: MutableList<AutoReplaceList> = mutableListOf()

    fun listNames(): List<String> = lists.map { it.name }

    fun getListEntries(listName: String): Map<String, String>? =
        lists.find { it.name == listName }?.replacements

    fun replacementFor(keyword: String, listName: String): String =
        lists.find { it.name == listName }?.replacements?.get(keyword) ?: ""

    fun addEntryToList(keyword: String, replacement: String, listName: String): Boolean {
        if (keyword.isEmpty()) return false
        val list = lists.find { it.name == listName } ?: return false
        list.replacements[keyword] = replacement
        return true
    }

    fun removeEntryFromList(keyword: String, listName: String): Boolean {
        val list = lists.find { it.name == listName } ?: return false
        return list.replacements.remove(keyword) != null
    }

    fun increaseListPriority(name: String): Boolean {
        val idx = lists.indexOfFirst { it.name == name }
        if (idx <= 0) return idx == 0
        val tmp = lists[idx - 1]; lists[idx - 1] = lists[idx]; lists[idx] = tmp
        return true
    }

    fun decreaseListPriority(name: String): Boolean {
        val idx = lists.indexOfFirst { it.name == name }
        if (idx < 0 || idx == lists.lastIndex) return false
        val tmp = lists[idx + 1]; lists[idx + 1] = lists[idx]; lists[idx] = tmp
        return true
    }

    fun addListNamed(name: String, data: Any): AddListResult {
        if (lists.any { it.name == name }) return AddListResult.DUPLICATE_NAME
        lists += AutoReplaceList(name = name)
        return AddListResult.OK
    }

    fun replaceList(data: Any): AddListResult {
        val name = getListName(data) ?: return AddListResult.INVALID_LIST
        lists.removeIf { it.name == name }
        lists += AutoReplaceList(name = name)
        return AddListResult.OK
    }

    fun removeReplacementList(name: String): Boolean = lists.removeIf { it.name == name }

    fun getListName(data: Any): String? =
        (data as? Map<*, *>)?.get("name") as? String

    companion object {
        fun copyFrom(source: AutoReplace): AutoReplaceSettings {
            val snap = AutoReplaceSettings()
            for (list in source.lists) {
                snap.lists += AutoReplaceList(
                    name = list.name,
                    enabled = list.enabled,
                    replacements = list.replacements.toMutableMap()
                )
            }
            return snap
        }
    }
}
