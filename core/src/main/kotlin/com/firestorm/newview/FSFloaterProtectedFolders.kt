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
        System.err.println("FSFloaterProtectedFolders: find child ScrollListCtrl 'folder_list', set filter column 0, wire double-click to onDoubleClick() not yet implemented")
        System.err.println("FSFloaterProtectedFolders: find child Button 'remove_btn', wire commit callback to handleRemove() not yet implemented")
        System.err.println("FSFloaterProtectedFolders: find child FilterEditor 'filter_input', wire commit callback to onFilterEdit(searchString) not yet implemented")
        return true
    }

    fun onOpen(info: Map<String, Any?>) {
        if (!initialized) {
            System.err.println("FSFloaterProtectedFolders: check gInventory.isInventoryUsable(); return if not not yet implemented")
            protectedCategoriesChangedSlot = { updateList() }
            System.err.println("FSFloaterProtectedFolders: connect protectedCategoriesChangedSlot to FSProtectedFolders setting commit signal not yet implemented")
            updateList()
            initialized = true
        }
    }

    fun draw() {
        System.err.println("FSFloaterProtectedFolders: call LLFloater.draw() not yet implemented")
        System.err.println("FSFloaterProtectedFolders: set removeFolderBtn enabled = folderList.getNumSelected() > 0 not yet implemented")
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        System.err.println("FSFloaterProtectedFolders: if FSCommon.isFilterEditorKeyCombo(key, mask), set focus on filterEditor and return true not yet implemented")
        System.err.println("FSFloaterProtectedFolders: fall through to LLFloater.handleKeyHere(key, mask) not yet implemented")
        return false
    }

    fun hasAccelerators(): Boolean = true

    private fun updateList() {
        System.err.println("FSFloaterProtectedFolders: save sort state, clear rows, disable sort not yet implemented")
        System.err.println("FSFloaterProtectedFolders: read FSProtectedFolders LLSD array from gSavedPerAccountSettings not yet implemented")
        System.err.println("FSFloaterProtectedFolders: for each UUID entry, look up category in gInventory; build row with 'name' column not yet implemented")
        System.err.println("FSFloaterProtectedFolders: restore sort state and call updateSort() not yet implemented")
    }

    private fun handleRemove() {
        System.err.println("FSFloaterProtectedFolders: collect UUIDs from all selected items in folderList not yet implemented")
        System.err.println("FSFloaterProtectedFolders: read FSProtectedFolders array, filter out selected UUIDs, write back to gSavedPerAccountSettings not yet implemented")
    }

    private fun onFilterEdit(searchString: String) {
        filterSubStringOrig = searchString.trimStart()
        val searchUpper = filterSubStringOrig.uppercase()
        if (filterSubString == searchUpper) return
        filterSubString = searchUpper
        System.err.println("FSFloaterProtectedFolders: call folderList.setFilterString(filterSubStringOrig) not yet implemented")
    }

    private fun onDoubleClick() {
        System.err.println("FSFloaterProtectedFolders: get selectedItemId from folderList.getStringUUIDSelectedItem() not yet implemented")
        System.err.println("FSFloaterProtectedFolders: if selectedItemId is non-null and RLV allows showing inventory, call show_item_original(selectedItemId) not yet implemented")
    }
}
