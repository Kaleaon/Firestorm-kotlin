// Converted from indra/newview/llinventorypanel.h + llinventorypanel.cpp
// Original: Copyright (C) 2010, Linden Research, Inc. (LGPL 2.1)
package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ── Sort-order constants (mirrors LLInventoryPanel static strings) ────────────

object InventoryPanelSortOrder {
    const val DEFAULT      = "InventorySortOrder"
    const val RECENT_ITEMS = "RecentItemsSortOrder"
    const val INHERIT      = ""
}

// ── Filter type flags ─────────────────────────────────────────────────────────

/**
 * Bitfield constants for [InventoryPanel.setFilterTypes].  Values mirror the
 * EFilterType enum used in LLInventoryFilter.
 */
object FilterType {
    const val OBJECT:   UInt = 0x01u
    const val CATEGORY: UInt = 0x02u
    const val UUID:     UInt = 0x04u
    const val DATE:     UInt = 0x08u
    const val WORN:     UInt = 0x10u
    const val ALL:      UInt = 0xFFFFFFFFu
}

// ── Folder show state ─────────────────────────────────────────────────────────

enum class FolderShow {
    SHOW_ALL_FOLDERS,
    SHOW_NON_EMPTY_FOLDERS,
    SHOW_NO_FOLDERS
}

// ── Search type ───────────────────────────────────────────────────────────────

enum class SearchType {
    BY_NAME,
    BY_CREATOR,
    BY_DESCRIPTION,
    BY_UUID
}

// ── InventoryPanel ────────────────────────────────────────────────────────────

/**
 * Inventory display panel that owns a [FolderView] and mediates between the
 * view and the [InventoryModel] singleton.
 *
 * Mirrors [LLInventoryPanel] from llinventorypanel.h / llinventorypanel.cpp.
 *
 * View-building (buildNewViews), rendering and drag-and-drop acceptance logic
 * are stubbed with [TODO] pending UI toolkit integration.
 */
class InventoryPanel(
    val name: String = "inventory_panel",
    val allowMultiSelect: Boolean = true,
    val allowDrag: Boolean = true,
    val showItemLinkOverlays: Boolean = false,
    val showEmptyMessage: Boolean = true,
    val suppressFolderMenu: Boolean = false,
    val sortOrderSetting: String = InventoryPanelSortOrder.DEFAULT,
    val allowDropOnRoot: Boolean = true
) {

    // ── Model reference ───────────────────────────────────────────────────────

    /** UUID of the root category this panel displays. */
    var modelId: LLUUID = LLUUID()
        private set

    // ── Folder view ───────────────────────────────────────────────────────────

    /** The owned folder-view widget.  Created by [initFolderRoot]. */
    var folderRoot: FolderView? = null
        private set

    // ── Filter state ──────────────────────────────────────────────────────────

    var filter: String = ""
        private set

    private var filterTypes: UInt     = FilterType.ALL
    private var filterPermMask: UInt  = 0xFFFFFFFFu
    private var filterLinks: ULong    = 0uL
    private var filterWearableTypes: ULong = 0uL
    private var filterSettingsTypes: ULong = 0uL
    private var sinceLogoff: Boolean  = false
    private var hoursAgo: UInt        = 0u
    private var dateSearchDirection: UInt = 0u
    private var showFolderState: FolderShow = FolderShow.SHOW_ALL_FOLDERS
    private var searchType: SearchType = SearchType.BY_NAME
    private var filterCoalescedObjects: Boolean = false
    private var filterPermissions: UInt = 0u

    // ── Sort order ────────────────────────────────────────────────────────────

    var sortOrder: UInt = 0u
        private set

    // ── Item map (id → view item) ─────────────────────────────────────────────

    private val itemMap: MutableMap<LLUUID, FolderViewItem> = mutableMapOf()

    // ── Selection callback ────────────────────────────────────────────────────

    private var selectionCallback: ((List<FolderViewItem>, Boolean) -> Unit)? = null

    fun setSelectCallback(cb: (List<FolderViewItem>, Boolean) -> Unit) {
        selectionCallback = cb
        folderRoot?.setSelectCallback(cb)
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Creates the [FolderView] root and links it to [InventoryModel].
     * Mirrors the body of [LLInventoryPanel::initFromParams] and
     * [LLInventoryPanel::initFolderRoot].
     */
    fun initFolderRoot(rootId: LLUUID = InventoryModel.getRootFolderID()) {
        modelId = rootId
        folderRoot = FolderView(
            title               = name,
            allowMultiSelect    = allowMultiSelect,
            allowDrag           = allowDrag,
            showEmptyMessage    = showEmptyMessage,
            suppressFolderMenu  = suppressFolderMenu,
            showItemLinkOverlays = showItemLinkOverlays
        )
        selectionCallback?.let { folderRoot?.setSelectCallback(it) }
        buildViewsForRoot(rootId)
    }

    /**
     * Bootstraps the view tree for [rootId].  Heavy recursive view building
     * (buildNewViews) is stubbed — it requires bridging to LLInventoryObject.
     */
    private fun buildViewsForRoot(rootId: LLUUID) {
        TODO("Walk InventoryModel categories/items under rootId and build FolderViewItem tree")
    }

    // ── Filter API ────────────────────────────────────────────────────────────

    /**
     * Sets the bitmask of inventory type flags that should be visible.
     * Mirrors [LLInventoryPanel::setFilterTypes].
     */
    fun setFilterTypes(types: UInt) {
        filterTypes = types
        applyCurrentFilter()
    }

    fun setFilterWorn() {
        TODO("Apply 'worn items only' filter via LLInventoryFilter::setFilterWorn")
    }

    fun getFilterObjectTypes(): UInt = filterTypes

    fun setFilterPermMask(mask: UInt) {
        filterPermMask = mask
        applyCurrentFilter()
    }

    fun getFilterPermMask(): UInt = filterPermMask

    fun setFilterWearableTypes(filter: ULong) { filterWearableTypes = filter; applyCurrentFilter() }
    fun setFilterSettingsTypes(filter: ULong) { filterSettingsTypes = filter; applyCurrentFilter() }

    fun setFilterSubString(str: String) {
        filter = str
        folderRoot?.setFilter(str)
    }

    fun getFilterSubString(): String = filter

    fun setSinceLogoff(sl: Boolean) { sinceLogoff = sl; applyCurrentFilter() }
    fun getSinceLogoff(): Boolean = sinceLogoff

    fun setHoursAgo(hours: UInt) { hoursAgo = hours; applyCurrentFilter() }

    fun setDateSearchDirection(direction: UInt) { dateSearchDirection = direction }

    fun setFilterLinks(links: ULong) { filterLinks = links; applyCurrentFilter() }
    fun getFilterLinks(): ULong = filterLinks

    fun setFilterCoalescedObjects(coalesced: Boolean) {
        filterCoalescedObjects = coalesced
        applyCurrentFilter()
    }
    fun getFilterCoalescedObjects(): Boolean = filterCoalescedObjects

    fun setFilterPermissions(perms: UInt) { filterPermissions = perms; applyCurrentFilter() }
    fun getFilterPermissions(): UInt = filterPermissions

    fun setSearchType(type: SearchType) { searchType = type }
    fun getSearchType(): SearchType = searchType

    fun setShowFolderState(show: FolderShow) { showFolderState = show; applyCurrentFilter() }
    fun getShowFolderState(): FolderShow = showFolderState

    private fun applyCurrentFilter() {
        folderRoot?.setFilter(filter)
        // Additional filter predicates (type mask, permissions, date) require
        // walking the FolderViewItem tree and evaluating each item against the
        // full LLInventoryFilter ruleset — stubbed here.
        TODO("Evaluate filterTypes, filterPermMask, sinceLogoff, etc. on each item")
    }

    // ── Sort order ────────────────────────────────────────────────────────────

    fun setSortOrder(order: UInt) {
        sortOrder = order
        folderRoot?.sort()
    }

    fun getSortOrder(): UInt = sortOrder

    // ── Folder operations ─────────────────────────────────────────────────────

    fun openAllFolders() {
        val r = folderRoot?.root ?: return
        setOpenRecursive(r, open = true)
    }

    fun closeAllFolders() {
        folderRoot?.closeAllFolders()
    }

    private fun setOpenRecursive(item: FolderViewItem, open: Boolean) {
        item.isOpen = open
        item.children.forEach { setOpenRecursive(it, open) }
    }

    // ── Selection API ─────────────────────────────────────────────────────────

    fun setSelection(objId: LLUUID, takeKeyboardFocus: Boolean) {
        folderRoot?.selectItem(objId)
        if (takeKeyboardFocus) TODO("Request keyboard focus from the UI toolkit")
    }

    fun clearSelection() {
        folderRoot?.clearSelection()
    }

    fun getSelectedItems(): Set<FolderViewItem> {
        return folderRoot?.selectedItems?.toSet() ?: emptySet()
    }

    fun isSelectionRemovable(): Boolean {
        TODO("Check each selected item against current agent permissions")
    }

    fun openSelected() {
        val cur = folderRoot?.getCurSelectedItem() ?: return
        TODO("Open/preview item '${cur.name}' via the appropriate viewer action")
    }

    fun unSelectAll() { clearSelection() }

    // ── Root folder accessor ──────────────────────────────────────────────────

    fun getRootFolder(): FolderViewItem? = folderRoot?.root

    fun getRootFolderID(): LLUUID = modelId

    // ── Item map helpers ──────────────────────────────────────────────────────

    fun addItemID(id: LLUUID, item: FolderViewItem) { itemMap[id] = item }

    open fun removeItemID(id: LLUUID) { itemMap.remove(id) }

    fun getItemByID(id: LLUUID): FolderViewItem? = itemMap[id]

    // ── Model change notification ─────────────────────────────────────────────

    /**
     * Called when the inventory model changes.  Mirrors
     * [LLInventoryPanel::modelChanged].  Rebuilds affected view items.
     */
    fun modelChanged(mask: UInt) {
        TODO("Determine which items changed via mask and rebuild/update their FolderViewItems")
    }

    // ── Visibility / focus ────────────────────────────────────────────────────

    fun onVisibilityChange(visible: Boolean) {
        TODO("Start or pause idle callbacks based on visibility")
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    companion object {
        /** Currently active (top-most visible) inventory panel, if any. */
        private var activeInstance: InventoryPanel? = null

        /**
         * Returns the active inventory panel.  Mirrors
         * [LLInventoryPanel::getActiveInventoryPanel].
         *
         * @param autoOpen  If true and no panel is visible, open one.
         * @param ignoreSecondary  Ignore secondary window panels.
         */
        fun getInstance(autoOpen: Boolean = true, ignoreSecondary: Boolean = false): InventoryPanel? {
            return activeInstance
                ?: if (autoOpen) {
                    TODO("Show the inventory floater and return its panel")
                } else {
                    null
                }
        }

        /**
         * Sets [panel] as the globally-active inventory panel.
         * Call this when a panel gains focus.
         */
        fun setActiveInstance(panel: InventoryPanel?) {
            activeInstance = panel
        }

        fun openInventoryPanelAndSetSelection(
            autoOpen: Boolean,
            objId: LLUUID,
            useMainPanel: Boolean = false,
            takeKeyboardFocus: Boolean = true,
            resetFilter: Boolean = false
        ) {
            val panel = getInstance(autoOpen) ?: return
            if (resetFilter) panel.setFilterSubString("")
            panel.setSelection(objId, takeKeyboardFocus)
        }
    }
}

// ── InventorySingleFolderPanel ────────────────────────────────────────────────

/**
 * Single-folder mode panel — navigates like a file manager with back/forward.
 * Mirrors [LLInventorySingleFolderPanel].
 */
class InventorySingleFolderPanel(
    name: String = "single_folder_panel",
    allowMultiSelect: Boolean = false
) : InventoryPanel(name = name, allowMultiSelect = allowMultiSelect) {

    var folderID: LLUUID = LLUUID()
        private set

    private val backwardFolders: ArrayDeque<LLUUID> = ArrayDeque()
    private val forwardFolders:  ArrayDeque<LLUUID> = ArrayDeque()

    private val rootChangedCallbacks: MutableList<() -> Unit> = mutableListOf()

    fun addRootChangedCallback(cb: () -> Unit) { rootChangedCallbacks.add(cb) }

    fun initFolderRoot(startFolderId: LLUUID) {
        folderID = startFolderId
        initFolderRoot(startFolderId)
        notifyRootChanged()
    }

    fun changeFolderRoot(newId: LLUUID) {
        backwardFolders.addLast(folderID)
        forwardFolders.clear()
        folderID = newId
        initFolderRoot(newId)
        notifyRootChanged()
    }

    fun onBackwardFolder() {
        if (backwardFolders.isEmpty()) return
        forwardFolders.addLast(folderID)
        folderID = backwardFolders.removeLast()
        initFolderRoot(folderID)
        notifyRootChanged()
    }

    fun onForwardFolder() {
        if (forwardFolders.isEmpty()) return
        backwardFolders.addLast(folderID)
        folderID = forwardFolders.removeLast()
        initFolderRoot(folderID)
        notifyRootChanged()
    }

    fun clearNavigationHistory() {
        backwardFolders.clear()
        forwardFolders.clear()
    }

    fun isBackwardAvailable(): Boolean = backwardFolders.isNotEmpty()
    fun isForwardAvailable():  Boolean = forwardFolders.isNotEmpty()

    fun hasVisibleItems(): Boolean {
        val root = getRootFolder() ?: return false
        return root.children.any { it.passedFilter }
    }

    override fun removeItemID(id: LLUUID) {
        super.removeItemID(id)
    }

    private fun notifyRootChanged() {
        rootChangedCallbacks.forEach { it() }
    }
}

// ── AssetFilteredInventoryPanel ───────────────────────────────────────────────

/**
 * Pre-filters on asset type for speed.  Mirrors [LLAssetFilteredInventoryPanel].
 */
class AssetFilteredInventoryPanel(
    name: String = "asset_filtered_inv_panel",
    val filterAssetTypes: String = ""
) : InventoryPanel(name = name) {

    // Asset type flags parsed from filterAssetTypes string — stubbed.
    private val allowedAssetTypes: Set<Int> = emptySet()

    fun typedViewsFilter(id: LLUUID, assetType: Int): Boolean {
        return allowedAssetTypes.isEmpty() || assetType in allowedAssetTypes
    }
}
