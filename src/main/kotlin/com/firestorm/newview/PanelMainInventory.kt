// Converted from indra/newview/llpanelmaininventory.h + llpanelmaininventory.cpp
// Original: Copyright (C) 2010, Linden Research, Inc. (LGPL 2.1)
package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ── View-mode enum ────────────────────────────────────────────────────────────

/**
 * Mirrors [LLPanelMainInventory::EViewModeType].
 */
enum class ViewMode {
    LIST,
    GALLERY,
    COMBINATION
}

// ── Tab identifiers ───────────────────────────────────────────────────────────

private const val TAB_ALL_ITEMS    = "All Items"
private const val TAB_RECENT_ITEMS = "Recent Items"
private const val TAB_WORN_ITEMS   = "Worn Items"
private const val TAB_FAVORITES    = "Favorites"

// ── PanelMainInventory ────────────────────────────────────────────────────────

/**
 * Main inventory panel combining an "All Items" panel, a "Recent Items" panel
 * and optional gallery and combination views.  Manages filter state, tabs,
 * sort order and navigation buttons.
 *
 * Mirrors [LLPanelMainInventory] from llpanelmaininventory.h/.cpp.
 *
 * UI widget construction (tab container, filter editor, menu buttons) and
 * complex rendering are stubbed with [TODO].
 */
class PanelMainInventory {

    // ── Whether per-session filter saves are allowed ──────────────────────────

    // ── Sub-panels ────────────────────────────────────────────────────────────

    /** "All Items" tab panel.  Always non-null after [postBuild]. */
    private var allItemsPanel: InventoryPanel? = null

    /** "Recent Items" tab panel.  Always non-null after [postBuild]. */
    private var recentPanel: InventoryPanel? = null

    /** "Worn Items" tab panel. */
    private var wornItemsPanel: InventoryPanel? = null

    /** The panel currently displayed in the active tab. */
    private var activePanel: InventoryPanel? = null

    // ── Single-folder / gallery panels ────────────────────────────────────────

    private var combinationInventoryPanel: InventorySingleFolderPanel? = null

    // ── View mode ─────────────────────────────────────────────────────────────

    private var viewMode: ViewMode = ViewMode.COMBINATION
    private var singleFolderMode: Boolean = false

    // ── Filter state ──────────────────────────────────────────────────────────

    private var filterText: String = ""
    private var filterSubString: String = ""
    private var itemCount: Int = 0
    private var categoryCount: Int = 0
    private var savedFolderState: SaveFolderState = SaveFolderState()

    // ── Navigation button state ───────────────────────────────────────────────

    private var backAvailable: Boolean = false
    private var forwardAvailable: Boolean = false

    // ── FS: save-filters flag (mirrors LLPanelMainInventory::sSaveFilters) ─────

    companion object {
        /** Global flag — set false during settings restore to skip filter saves. */
        var saveFilters: Boolean = true
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Initialises sub-panels and wires up observer callbacks.
     * Mirrors [LLPanelMainInventory::postBuild].
     *
     * @return true on success (mirrors the C++ bool return).
     */
    fun postBuild(): Boolean {
        allItemsPanel = InventoryPanel(
            name            = TAB_ALL_ITEMS,
            allowMultiSelect = true,
            allowDrag        = true,
            sortOrderSetting = InventoryPanelSortOrder.DEFAULT
        ).also {
            it.initFolderRoot()
            it.setSelectCallback { items, userAction ->
                onSelectionChange(it, items, userAction)
            }
        }

        recentPanel = InventoryPanel(
            name             = TAB_RECENT_ITEMS,
            allowMultiSelect = true,
            allowDrag        = true,
            sortOrderSetting = InventoryPanelSortOrder.RECENT_ITEMS
        ).also {
            it.initFolderRoot()
            it.setSelectCallback { items, userAction ->
                onSelectionChange(it, items, userAction)
            }
        }

        wornItemsPanel = InventoryPanel(
            name             = TAB_WORN_ITEMS,
            allowMultiSelect = true,
            allowDrag        = false,
            sortOrderSetting = InventoryPanelSortOrder.DEFAULT
        ).also {
            it.initFolderRoot()
        }

        activePanel = allItemsPanel

        combinationInventoryPanel = InventorySingleFolderPanel(
            name = "combination_inventory_panel"
        )

        // Register as InventoryModel observer
        InventoryModel.addObserver(object : InventoryObserver {
            override fun changed(mask: UInt) { this@PanelMainInventory.onInventoryChanged(mask) }
        })

        return true
    }

    // ── Panel accessors ───────────────────────────────────────────────────────

    /** Returns the currently-active [InventoryPanel]. */
    fun getActivePanel(): InventoryPanel =
        activePanel ?: error("PanelMainInventory not yet built — call postBuild() first")

    fun getPanel(): InventoryPanel = getActivePanel()

    fun getAllItemsPanel(): InventoryPanel =
        allItemsPanel ?: error("PanelMainInventory not yet built")

    // ── Tab switching ─────────────────────────────────────────────────────────

    /**
     * Activates the named tab.  Mirrors the tab-container callbacks wired up
     * in postBuild.
     */
    fun setActivePanel() {
        TODO("Read the selected tab index and update activePanel accordingly")
    }

    fun selectAllItemsPanel() {
        activePanel = allItemsPanel
        showInventoryPanel()
        TODO("Switch the tab container to the All Items tab")
    }

    /** Mirrors [LLPanelMainInventory::showAllItemsPanel]. */
    fun showAllItemsPanel() {
        selectAllItemsPanel()
    }

    fun isRecentItemsPanelSelected(): Boolean = activePanel === recentPanel

    // ── Visibility ────────────────────────────────────────────────────────────

    /** Makes the inventory panel visible. */
    fun showInventoryPanel() {
        TODO("Set panel visibility to true in the UI toolkit")
    }

    /** Hides the inventory panel. */
    fun hideInventoryPanel() {
        TODO("Set panel visibility to false in the UI toolkit")
    }

    fun onVisibilityChange(visible: Boolean) {
        activePanel?.onVisibilityChange(visible)
    }

    // ── Filter editor ─────────────────────────────────────────────────────────

    /**
     * Called when the user edits the search box.
     * Mirrors [LLPanelMainInventory::onFilterEdit].
     */
    fun onFilterEdit(searchString: String) {
        filterSubString = searchString
        setFilterSubString(searchString)
        updateItemcountText()
    }

    fun setFocusOnFilterEditor() {
        TODO("Move keyboard focus to the filter text box")
    }

    // ── Filter state helpers ──────────────────────────────────────────────────

    fun getFilterText(): String = filterText

    fun getCurrentFilter() = getActivePanel().getFilterSubString()

    fun setFilterSubString(string: String) {
        getActivePanel().setFilterSubString(string)
        allItemsPanel?.setFilterSubString(string)
        recentPanel?.setFilterSubString(string)
    }

    fun getFilterSubString(): String = getActivePanel().getFilterSubString()

    fun resetFilters() {
        resetAllItemsFilters()
        recentPanel?.setFilterSubString("")
        filterSubString = ""
        filterText = ""
        updateItemcountText()
    }

    fun resetAllItemsFilters() {
        allItemsPanel?.setFilterSubString("")
    }

    fun toggleFindOptions() {
        TODO("Show or hide the LLFloaterInventoryFinder dialog")
    }

    // ── Filter-type dropdown (Firestorm extension) ────────────────────────────

    fun onFilterTypeSelected(filterTypeName: String) {
        TODO("Look up filterTypeName in the filter map and apply the bitmask to the active panel")
    }

    fun updateFilterDropdown(/*filter: InventoryFilter*/) {
        TODO("Sync combo-box selection to reflect the current filter state")
    }

    // ── Sort ──────────────────────────────────────────────────────────────────

    fun setSortBy(userdata: String) {
        TODO("Parse sort key from userdata and call activePanel.setSortOrder()")
    }

    fun isSortByChecked(userdata: String): Boolean {
        TODO("Return true if the sort key matches activePanel.getSortOrder()")
    }

    // ── Selection callbacks ───────────────────────────────────────────────────

    fun setSelectCallback(cb: (List<FolderViewItem>, Boolean) -> Unit) {
        allItemsPanel?.setSelectCallback(cb)
        recentPanel?.setSelectCallback(cb)
    }

    private fun onSelectionChange(
        panel: InventoryPanel,
        items: List<FolderViewItem>,
        userAction: Boolean
    ) {
        // Bring the panel that generated the selection event to the front.
        if (activePanel !== panel) activePanel = panel
    }

    // ── View mode ─────────────────────────────────────────────────────────────

    fun setViewMode(mode: ViewMode) {
        viewMode = mode
        updatePanelVisibility()
    }

    fun isListViewMode():        Boolean = viewMode == ViewMode.LIST
    fun isGalleryViewMode():     Boolean = viewMode == ViewMode.GALLERY
    fun isCombinationViewMode(): Boolean = viewMode == ViewMode.COMBINATION
    fun isSingleFolderMode():    Boolean = singleFolderMode

    fun onViewModeClick() {
        viewMode = when (viewMode) {
            ViewMode.LIST        -> ViewMode.GALLERY
            ViewMode.GALLERY     -> ViewMode.COMBINATION
            ViewMode.COMBINATION -> ViewMode.LIST
        }
        updatePanelVisibility()
    }

    fun toggleViewMode() = onViewModeClick()

    // ── Single-folder navigation ──────────────────────────────────────────────

    fun initSingleFolderRoot(startFolderId: LLUUID = LLUUID()) {
        combinationInventoryPanel?.initFolderRoot(startFolderId)
    }

    fun setSingleFolderViewRoot(folderId: LLUUID, clearNavHistory: Boolean = true) {
        if (clearNavHistory) combinationInventoryPanel?.clearNavigationHistory()
        combinationInventoryPanel?.changeFolderRoot(folderId)
        updateNavButtons()
    }

    fun getSingleFolderViewRoot(): LLUUID =
        combinationInventoryPanel?.folderID ?: LLUUID()

    fun getCurrentSFVRoot(): LLUUID = getSingleFolderViewRoot()

    fun getLocalizedRootName(): String {
        TODO("Look up the root folder name in InventoryModel and translate if needed")
    }

    fun onUpFolderClicked() {
        val currentId = getSingleFolderViewRoot()
        val cat = InventoryModel.categories[currentId] ?: return
        setSingleFolderViewRoot(cat.parentUUID, clearNavHistory = false)
    }

    fun onBackFolderClicked() {
        combinationInventoryPanel?.onBackwardFolder()
        updateNavButtons()
    }

    fun onForwardFolderClicked() {
        combinationInventoryPanel?.onForwardFolder()
        updateNavButtons()
    }

    private fun updateNavButtons() {
        backAvailable    = combinationInventoryPanel?.isBackwardAvailable() ?: false
        forwardAvailable = combinationInventoryPanel?.isForwardAvailable()  ?: false
    }

    // ── Gallery selection helpers ─────────────────────────────────────────────

    fun setGallerySelection(itemId: LLUUID, newWindow: Boolean = false) {
        TODO("Select item in the gallery view, optionally in a new window")
    }

    fun scrollToGallerySelection() {
        TODO("Scroll the gallery panel to reveal the selected item")
    }

    fun scrollToInvPanelSelection() {
        getActivePanel().folderRoot?.scrollToShowSelection()
    }

    // ── Links ─────────────────────────────────────────────────────────────────

    fun findLinks(itemId: LLUUID, itemName: String) {
        TODO("Apply a link filter to show all items that link to itemId named itemName")
    }

    // ── Item count text ───────────────────────────────────────────────────────

    private fun updateItemcountText() {
        itemCount = InventoryModel.items.size
        categoryCount = InventoryModel.categories.size
        // Actual formatted string building would use localisation — stubbed.
    }

    // ── Panel visibility ──────────────────────────────────────────────────────

    private fun updatePanelVisibility() {
        TODO("Show/hide list, gallery and combination sub-panels according to viewMode")
    }

    // ── Inventory observer callback ───────────────────────────────────────────

    private fun onInventoryChanged(mask: UInt) {
        activePanel?.modelChanged(mask)
        updateItemcountText()
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    fun draw() {
        TODO("Delegate drawing to the active sub-panel and UI chrome")
    }

    // ── New window helpers ────────────────────────────────────────────────────

    fun newWindow() {
        TODO("Open a new floating inventory window via FloaterRegistry")
    }

    fun newFolderWindow(folderId: LLUUID = LLUUID(), itemToSelect: LLUUID = LLUUID()) {
        TODO("Open a new floating window rooted at folderId, selecting itemToSelect")
    }
}
