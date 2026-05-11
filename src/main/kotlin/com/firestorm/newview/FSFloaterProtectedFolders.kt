package com.firestorm.newview

class FSFloaterProtectedFolders(key: Map<String, Any?>) {

    private var initialized: Boolean = false

    private var filterSubString: String = ""
    private var filterSubStringOrig: String = ""

    private var protectedCategoriesChangedSlot: (() -> Unit)? = null

    private var folderList: Any? = null
    private var removeFolderBtn: Any? = null
    private var filterEditor: Any? = null

    fun destroy() {
        protectedCategoriesChangedSlot = null
    }

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent: find child ScrollListCtrl 'folder_list', set filter column 0, wire double-click to onDoubleClick()")
        TODO("APR: use JVM equivalent: find child Button 'remove_btn', wire commit callback to handleRemove()")
        TODO("APR: use JVM equivalent: find child FilterEditor 'filter_input', wire commit callback to onFilterEdit(searchString)")
        return true
    }

    fun onOpen(info: Map<String, Any?>) {
        if (!initialized) {
            TODO("APR: use JVM equivalent: check gInventory.isInventoryUsable(); return if not")
            protectedCategoriesChangedSlot = { updateList() }
            TODO("APR: use JVM equivalent: connect protectedCategoriesChangedSlot to FSProtectedFolders setting commit signal")
            updateList()
            initialized = true
        }
    }

    fun draw() {
        TODO("APR: use JVM equivalent: call LLFloater.draw()")
        TODO("APR: use JVM equivalent: set removeFolderBtn enabled = folderList.getNumSelected() > 0")
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        TODO("APR: use JVM equivalent: if FSCommon.isFilterEditorKeyCombo(key, mask), set focus on filterEditor and return true")
        TODO("APR: use JVM equivalent: fall through to LLFloater.handleKeyHere(key, mask)")
    }

    fun hasAccelerators(): Boolean = true

    private fun updateList() {
        TODO("APR: use JVM equivalent: save sort state, clear rows, disable sort")
        TODO("APR: use JVM equivalent: read FSProtectedFolders LLSD array from gSavedPerAccountSettings")
        TODO("APR: use JVM equivalent: for each UUID entry, look up category in gInventory; build row with 'name' column (use UnknownFolder string if category not found); set italic font style for unknown categories")
        TODO("APR: use JVM equivalent: restore sort state and call updateSort()")
    }

    private fun handleRemove() {
        TODO("APR: use JVM equivalent: collect UUIDs from all selected items in folderList")
        TODO("APR: use JVM equivalent: read FSProtectedFolders array, filter out selected UUIDs, write back to gSavedPerAccountSettings")
    }

    private fun onFilterEdit(searchString: String) {
        filterSubStringOrig = searchString.trimStart()
        val searchUpper = filterSubStringOrig.uppercase()
        if (filterSubString == searchUpper) return
        filterSubString = searchUpper
        TODO("APR: use JVM equivalent: call folderList.setFilterString(filterSubStringOrig)")
    }

    private fun onDoubleClick() {
        TODO("APR: use JVM equivalent: get selectedItemId from folderList.getStringUUIDSelectedItem()")
        TODO("APR: use JVM equivalent: if selectedItemId is non-null and RLV allows showing inventory, call show_item_original(selectedItemId)")
    }
}
