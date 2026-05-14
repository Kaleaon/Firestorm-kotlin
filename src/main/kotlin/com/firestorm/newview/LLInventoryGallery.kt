package com.firestorm.newview

import java.util.UUID

const val GALLERY_ITEMS_PER_ROW_MIN: Int = 2
const val FAST_LOAD_THUMBNAIL_THRESHOLD: Int = 50

class LLInventoryGallery(params: Params = Params()) : LLPanel(), LLEditMenuHandler {

    data class Params(
        val rowPanelHeight: Int = 180,
        val rowPanelWidthFactor: Int = 166,
        val galleryWidthFactor: Int = 163,
        val verticalGap: Int = 10,
        val horizontalGap: Int = 10,
        val itemWidth: Int = 150,
        val itemHeight: Int = 175,
        val itemHorizontalGap: Int = 16,
        val itemsInRow: Int = GALLERY_ITEMS_PER_ROW_MIN
    )

    val selectionChangeSignal: MutableList<(UUID) -> Unit> = mutableListOf()
    val rootChangedSignal: MutableList<() -> Unit> = mutableListOf()

    private var categoriesObserver: LLInventoryCategoriesObserver? = LLInventoryCategoriesObserver()
    private var thumbnailsObserver: LLThumbnailsObserver? = LLThumbnailsObserver()
    private val gestureObserver: LLGalleryGestureObserver = LLGalleryGestureObserver(this)
    private val inventoryObserver: LLInventoryObserver? = null

    private val selectedItemIDs: ArrayDeque<UUID> = ArrayDeque()
    private val itemsToSelect: ArrayDeque<UUID> = ArrayDeque()
    private var lastInteractedUUID: UUID = UUID.randomUUID()
    private var isInitialized: Boolean = false
    private var rootDirty: Boolean = false

    var folderID: UUID = UUID.randomUUID()
    val backwardFolders: ArrayDeque<UUID> = ArrayDeque()
    val forwardFolders: ArrayDeque<UUID> = ArrayDeque()

    private val rowPanels: MutableList<LLPanel> = mutableListOf()
    private val itemPanels: MutableList<LLPanel> = mutableListOf()
    private val unusedRowPanels: MutableList<LLPanel> = mutableListOf()
    private val unusedItemPanels: MutableList<LLPanel> = mutableListOf()
    private val items: MutableList<LLInventoryGalleryItem> = mutableListOf()
    private val hiddenItems: MutableList<LLInventoryGalleryItem> = mutableListOf()

    private var scrollPanel: LLScrollContainer? = null
    private var galleryPanel: LLPanel? = null
    private var lastRowPanel: LLPanel? = null
    private var messageTextBox: LLTextBox? = null

    private var rowCount: Int = 0
    private var itemsAddedCount: Int = 0
    private var galleryCreated: Boolean = false
    private var loadThumbnailsImmediately: Boolean = true
    private var needsArrange: Boolean = false

    private var rowPanelHeight: Int = params.rowPanelHeight
    private var verticalGap: Int = params.verticalGap
    private var horizontalGap: Int = params.horizontalGap
    private var itemWidth: Int = params.itemWidth
    private var itemHeight: Int = params.itemHeight
    private var itemHorizontalGap: Int = params.itemHorizontalGap
    private var itemsInRow: Int = params.itemsInRow
    private var rowPanelWidth: Int = 0
    private var galleryWidth: Int = 0
    private var rowPanWidthFactor: Int = params.rowPanelWidthFactor
    private var galleryWidthFactor: Int = params.galleryWidthFactor

    private var inventoryGalleryMenu: LLInventoryGalleryContextMenu? = null
    private var rootGalleryMenu: LLInventoryGalleryContextMenu? = null
    private var filterSubString: String = ""
    private val filter: LLInventoryFilter = LLInventoryFilter()
    private var sortOrder: UInt = LLInventoryFilter.SO_DATE

    private val itemMap: MutableMap<UUID, LLInventoryGalleryItem> = mutableMapOf()
    private val cofLinkedItems: MutableList<UUID> = mutableListOf()
    private val activeGestures: MutableList<UUID> = mutableListOf()
    private val itemBuildQuery: MutableSet<UUID> = mutableSetOf()
    private val itemIndexMap: MutableMap<LLInventoryGalleryItem, Int> = mutableMapOf()
    private val indexToItemMap: MutableMap<Int, LLInventoryGalleryItem> = mutableMapOf()

    private var searchType: LLInventoryFilter.ESearchType = LLInventoryFilter.ESearchType.SEARCHTYPE_NAME
    private var username: String = ""

    init {
        updateGalleryWidth()
        System.err.println("LLInventoryGallery: register gesture observer and idle callbacks not yet implemented")
    }

    fun postBuild(): Boolean {
        scrollPanel = null
        messageTextBox = null
        inventoryGalleryMenu = LLInventoryGalleryContextMenu(this)
        rootGalleryMenu = LLInventoryGalleryContextMenu(this).also { it.isRootFolder = true }
        return true
    }

    fun initGallery() {
        if (!galleryCreated) {
            val cats = mutableListOf<UUID>()
            getCurrentCategories(cats)
            val n = cats.size
            buildGalleryPanel(n)
            scrollPanel?.addChild(galleryPanel!!)
            for (id in cats) {
                addToGallery(getItem(id) ?: return)
            }
            reArrangeRows()
            galleryCreated = true
        }
    }

    override fun draw() {
        // GPU: call super draw and invoke updateRowsIfNeeded / handleModifiedFilter
    }

    open fun onVisibilityChange(newVisibility: Boolean) {
        if (newVisibility) {
            if (rootDirty) updateRootFolder()
            else if (needsArrange) System.err.println("LLInventoryGallery: schedule idle callback not yet implemented")
        }
        // GPU: call super onVisibilityChange
    }

    open fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: EDragAndDropType, cargoData: Any?,
        accept: Array<EAcceptance>, tooltipMsg: StringBuilder
    ): Boolean {
        System.err.println("LLInventoryGallery: handleDragAndDrop not yet implemented")
        return false
    }

    fun startDrag() {
        System.err.println("LLInventoryGallery: startDrag not yet implemented")
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (selectedItemIDs.isNotEmpty()) { /* GPU: setFocus(true) */ }
        lastInteractedUUID = UUID.randomUUID()
        val res: Boolean = false
        if (lastInteractedUUID == UUID.randomUUID()) {
            clearSelection()
            if (inventoryGalleryMenu != null && folderID != UUID.randomUUID()) {
                rootGalleryMenu?.show(this, listOf(folderID), x, y)
                return true
            }
        }
        return res
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        System.err.println("LLInventoryGallery: handleKeyHere not yet implemented")
        return false
    }

    fun moveUp(mask: Int) {
        System.err.println("LLInventoryGallery: moveUp not yet implemented")
    }

    fun moveDown(mask: Int) {
        System.err.println("LLInventoryGallery: moveDown not yet implemented")
    }

    fun moveLeft(mask: Int) {
        System.err.println("LLInventoryGallery: moveLeft not yet implemented")
    }

    fun moveRight(mask: Int) {
        System.err.println("LLInventoryGallery: moveRight not yet implemented")
    }

    fun toggleSelectionRange(startIdx: Int, endIdx: Int) {
        System.err.println("LLInventoryGallery: toggleSelectionRange not yet implemented")
    }

    fun toggleSelectionRangeFromLast(target: UUID) {
        System.err.println("LLInventoryGallery: toggleSelectionRangeFromLast not yet implemented")
    }

    open fun onFocusLost() {
        // GPU: call super onFocusLost and redraw selected items
    }

    open fun onFocusReceived() {
        // GPU: call super onFocusReceived and redraw selected items
    }

    fun setFilterSubString(string: String) {
        filterSubString = string
        filter.setFilterSubString(string)
    }

    fun getFilterSubString(): String = filterSubString

    fun getFilter(): LLInventoryFilter = filter

    fun checkAgainstFilterType(objectId: UUID): Boolean {
        System.err.println("LLInventoryGallery: checkAgainstFilterType not yet implemented")
        return false
    }

    fun getCurrentCategories(vcur: MutableList<UUID>) {
        for ((id, item) in itemMap) {
            vcur.add(id)
        }
    }

    fun updateAddedItem(itemId: UUID): Boolean {
        System.err.println("LLInventoryGallery: updateAddedItem not yet implemented")
        return false
    }

    fun updateRemovedItem(itemId: UUID) {
        thumbnailsObserver?.removeItem(itemId)
        val item = itemMap.remove(itemId) ?: return
        deselectItem(itemId)
        removeFromGalleryMiddle(item)
        // GPU: call item.die() to destroy panel
        itemBuildQuery.remove(itemId)
    }

    fun updateChangedItemData(itemId: UUID, name: String, isFavorite: Boolean) {
        val item = itemMap[itemId] ?: return
        item.setItemName(name)
        item.setFavorite(isFavorite)
    }

    fun updateItemThumbnail(itemId: UUID) {
        System.err.println("LLInventoryGallery: updateItemThumbnail not yet implemented")
    }

    fun updateWornItem(itemId: UUID, isWorn: Boolean) {
        itemMap[itemId]?.setWorn(isWorn)
    }

    fun updateMessageVisibility() {
        // GPU: show or hide empty-folder message based on visible item count
    }

    fun setRootFolder(catId: UUID) {
        System.err.println("LLInventoryGallery: setRootFolder not yet implemented")
    }

    fun updateRootFolder() {
        System.err.println("LLInventoryGallery: updateRootFolder not yet implemented")
    }

    fun getRootFolder(): UUID = folderID

    fun isRootDirty(): Boolean = rootDirty

    fun setRootChangedCallback(cb: () -> Unit): (() -> Unit) {
        rootChangedSignal.add(cb)
        return cb
    }

    fun onForwardFolder() {
        val next = forwardFolders.removeFirstOrNull() ?: return
        backwardFolders.addLast(folderID)
        setRootFolder(next)
    }

    fun onBackwardFolder() {
        val prev = backwardFolders.removeLastOrNull() ?: return
        forwardFolders.addFirst(folderID)
        setRootFolder(prev)
    }

    fun clearNavigationHistory() {
        backwardFolders.clear()
        forwardFolders.clear()
    }

    fun isBackwardAvailable(): Boolean = backwardFolders.isNotEmpty()

    fun isForwardAvailable(): Boolean = forwardFolders.isNotEmpty()

    fun setNavBackwardList(backwardList: List<UUID>) {
        backwardFolders.clear()
        backwardFolders.addAll(backwardList)
    }

    fun setNavForwardList(forwardList: List<UUID>) {
        forwardFolders.clear()
        forwardFolders.addAll(forwardList)
    }

    fun getNavBackwardList(): List<UUID> = backwardFolders.toList()

    fun getNavForwardList(): List<UUID> = forwardFolders.toList()

    fun getOutfitImageID(outfitId: UUID): UUID {
        System.err.println("LLInventoryGallery: getOutfitImageID not yet implemented")
        return UUID.randomUUID()
    }

    fun refreshList(categoryId: UUID) {
        System.err.println("LLInventoryGallery: refreshList not yet implemented")
    }

    fun onCOFChanged() {
        System.err.println("LLInventoryGallery: onCOFChanged not yet implemented")
    }

    fun onGesturesChanged() {
        System.err.println("LLInventoryGallery: onGesturesChanged not yet implemented")
    }

    fun computeDifference(
        vcats: List<LLInventoryCategory>,
        vitems: List<LLInventoryItem>,
        vadded: MutableList<UUID>,
        vremoved: MutableList<UUID>
    ) {
        TODO("APR: use JVM equivalent - set-diff current itemMap keys against new cat+item UUIDs")
    }

    fun deselectItem(categoryId: UUID) {
        selectedItemIDs.remove(categoryId)
        itemMap[categoryId]?.setSelected(false)
    }

    fun clearSelection() {
        for (id in selectedItemIDs.toList()) {
            itemMap[id]?.setSelected(false)
        }
        selectedItemIDs.clear()
    }

    fun changeItemSelection(itemId: UUID, scrollToSelection: Boolean = false) {
        clearSelection()
        selectedItemIDs.addLast(itemId)
        itemMap[itemId]?.setSelected(true)
        if (scrollToSelection) scrollToShowItem(itemId)
        signalSelectionItemID(itemId)
    }

    fun addItemSelection(itemId: UUID, scrollToSelection: Boolean = false) {
        if (selectedItemIDs.contains(itemId)) return
        selectedItemIDs.addLast(itemId)
        itemMap[itemId]?.setSelected(true)
        lastInteractedUUID = itemId
        if (scrollToSelection) scrollToShowItem(itemId)
        signalSelectionItemID(itemId)
    }

    fun toggleItemSelection(itemId: UUID, scrollToSelection: Boolean = false): Boolean {
        return if (selectedItemIDs.contains(itemId)) {
            deselectItem(itemId)
            false
        } else {
            addItemSelection(itemId, scrollToSelection)
            true
        }
    }

    fun scrollToShowItem(itemId: UUID) {
        TODO("GPU: scroll container to make item panel visible")
    }

    fun signalSelectionItemID(categoryId: UUID) {
        for (cb in selectionChangeSignal) cb(categoryId)
    }

    fun setSelectionChangeCallback(cb: (UUID) -> Unit): (UUID) -> Unit {
        selectionChangeSignal.add(cb)
        return cb
    }

    fun getFirstSelectedItemID(): UUID = selectedItemIDs.firstOrNull() ?: UUID.randomUUID()

    fun setSearchType(type: LLInventoryFilter.ESearchType) {
        if (searchType != type) {
            searchType = type
            if (filterSubString.isNotEmpty()) reArrangeRows()
        }
    }

    fun getSearchType(): LLInventoryFilter.ESearchType = searchType

    fun areViewsInitialized(): Boolean = galleryCreated && isInitialized

    fun hasDescendents(catId: UUID): Boolean {
        TODO("APR: use JVM equivalent - check inventory model for descendants of category")
    }

    fun hasVisibleItems(): Boolean = items.any { !it.isHidden() }

    fun handleModifiedFilter() {
        if (filter.isModified()) reArrangeRows()
    }

    fun getScrollableContainer(): LLScrollContainer? = scrollPanel

    fun getFirstSelectedItem(): LLInventoryGalleryItem? =
        selectedItemIDs.firstOrNull()?.let { itemMap[it] }

    override fun copy() {
        TODO("APR: use JVM equivalent - copy selected item UUIDs to clipboard")
    }

    override fun canCopy(): Boolean {
        if (selectedItemIDs.isEmpty()) return false
        return selectedItemIDs.all { isItemCopyable(it) }
    }

    override fun cut() {
        TODO("APR: use JVM equivalent - mark selected items for cut in clipboard")
    }

    override fun canCut(): Boolean {
        TODO("APR: use JVM equivalent - check all selected items are removable")
    }

    override fun paste() {
        TODO("APR: use JVM equivalent - paste clipboard contents into current folder")
    }

    override fun canPaste(): Boolean {
        TODO("APR: use JVM equivalent - check clipboard has contents and destination is valid")
    }

    fun deleteSelection() {
        TODO("APR: use JVM equivalent - remove all selected items from inventory")
    }

    fun canDeleteSelection(): Boolean {
        TODO("APR: use JVM equivalent - check all selected items are removable")
    }

    fun pasteAsLink() {
        TODO("APR: use JVM equivalent - create inventory links from clipboard in current folder")
    }

    fun doCreate(dest: UUID, userdata: Any) {
        TODO("APR: use JVM equivalent - create new inventory item/folder per userdata type")
    }

    fun setSortOrder(order: UInt, update: Boolean = false) {
        sortOrder = order
        if (update) reArrangeRows()
    }

    fun getSortOrder(): UInt = sortOrder

    fun claimEditHandler() {
        TODO("GPU: set global edit menu handler to this gallery")
    }

    fun resetEditHandler() {
        TODO("GPU: clear global edit menu handler if it points to this gallery")
    }

    fun baseHandleDragAndDrop(
        destId: UUID, drop: Boolean,
        cargoType: EDragAndDropType, cargoData: Any?,
        accept: Array<EAcceptance>, tooltipMsg: StringBuilder
    ): Boolean = TODO("APR: use JVM equivalent - validate and execute item/category drag into destId")

    fun showContextMenu(ctrl: LLUICtrl, x: Int, y: Int, itemId: UUID) {
        inventoryGalleryMenu?.show(ctrl, listOf(itemId), x, y)
    }

    protected fun paste(
        dest: UUID,
        objects: List<UUID>,
        isCutMode: Boolean,
        marketplacelistingsId: UUID
    ) {
        TODO("APR: use JVM equivalent - copy or move clipboard objects to dest folder")
    }

    protected fun pasteAsLink(
        dest: UUID,
        objects: List<UUID>,
        currentOutfitId: UUID,
        marketplacelistingsId: UUID,
        myOutfitsId: UUID
    ) {
        TODO("APR: use JVM equivalent - create inventory links for objects in dest folder")
    }

    protected fun applyFilter(item: LLInventoryGalleryItem, filterSubstring: String): Boolean {
        val visible = checkAgainstFilters(item, filterSubstring)
        item.setHidden(!visible)
        return visible
    }

    protected fun checkAgainstFilters(item: LLInventoryGalleryItem, filterSubstring: String): Boolean {
        if (item.isFolder() && filter.getShowFolderState() == LLInventoryFilter.EFolderShow.SHOW_ALL_FOLDERS)
            return true

        val desc = when (searchType) {
            LLInventoryFilter.ESearchType.SEARCHTYPE_CREATOR -> item.getCreatorName()
            LLInventoryFilter.ESearchType.SEARCHTYPE_DESCRIPTION -> item.getDescription()
            LLInventoryFilter.ESearchType.SEARCHTYPE_UUID -> item.getAssetIDStr()
            LLInventoryFilter.ESearchType.SEARCHTYPE_NAME -> item.getItemName() + item.getItemNameSuffix()
        }

        if (filterSubstring.isEmpty()) return true
        return desc.toLowerCase().contains(filterSubstring.toLowerCase())
    }

    protected fun dirtyRootFolder() {
        TODO("GPU: if visible call updateRootFolder, else set rootDirty = true")
    }

    private fun addToGallery(item: LLInventoryGalleryItem) {
        if (item.isHidden()) {
            hiddenItems.add(item)
            return
        }
        itemIndexMap[item] = itemsAddedCount
        indexToItemMap[itemsAddedCount] = item
        itemsAddedCount++
        val n = itemsAddedCount
        val rowCount = if (n % itemsInRow == 0) n / itemsInRow else n / itemsInRow + 1
        val nPrev = n - 1
        val rowCountPrev = if (nPrev % itemsInRow == 0) nPrev / itemsInRow else nPrev / itemsInRow + 1

        loadThumbnailsImmediately = itemsAddedCount < FAST_LOAD_THUMBNAIL_THRESHOLD

        val addRow = rowCount != rowCountPrev
        if (addRow) {
            for (i in 0 until rowCountPrev) moveRowUp(i)
            lastRowPanel = addLastRow()
            rowPanels.add(lastRowPanel!!)
        }
        val pos = (n - 1) % itemsInRow
        items.add(item)
        addToRow(lastRowPanel!!, item, pos, horizontalGap * pos)
        reshapeGalleryPanel(rowCount)
    }

    private fun removeFromGalleryLast(item: LLInventoryGalleryItem, needsReshape: Boolean = true) {
        if (item.isHidden()) {
            hiddenItems.removeLastOrNull()
            return
        }
        val nPrev = itemsAddedCount
        val n = itemsAddedCount - 1
        val rowCount = if (n % itemsInRow == 0) n / itemsInRow else n / itemsInRow + 1
        val rowCountPrev = if (nPrev % itemsInRow == 0) nPrev / itemsInRow else nPrev / itemsInRow + 1
        itemsAddedCount--
        indexToItemMap.remove(itemsAddedCount)
        loadThumbnailsImmediately = itemsAddedCount < FAST_LOAD_THUMBNAIL_THRESHOLD

        val removeRow = rowCount != rowCountPrev
        removeFromLastRow(items[itemsAddedCount])
        items.removeAt(itemsAddedCount)
        if (removeRow) {
            for (i in 0 until rowCountPrev - 1) moveRowDown(i)
            removeLastRow()
        }
        if (needsReshape) reshapeGalleryPanel(rowCount)
    }

    private fun removeFromGalleryMiddle(item: LLInventoryGalleryItem) {
        if (item.isHidden()) {
            hiddenItems.remove(item)
            return
        }
        val n = itemIndexMap[item] ?: return
        itemIndexMap.remove(item)
        indexToItemMap.remove(n)
        val saved = mutableListOf<LLInventoryGalleryItem>()
        for (i in itemsAddedCount - 1 downTo n + 1) {
            saved.add(items[i])
            removeFromGalleryLast(items[i])
        }
        removeFromGalleryLast(items[n])
        for (savedItem in saved.reversed()) addToGallery(savedItem)
    }

    private fun addLastRow(): LLPanel {
        rowCount++
        val vgap = verticalGap * 0
        return buildRowPanel(0, 0 * rowPanelHeight + vgap)
    }

    private fun removeLastRow() {
        rowCount--
        TODO("GPU: remove lastRowPanel from galleryPanel, push to unusedRowPanels, update lastRowPanel")
    }

    private fun moveRowUp(row: Int) { moveRow(row, rowCount - 1 - row + 1) }
    private fun moveRowDown(row: Int) { moveRow(row, rowCount - 1 - row - 1) }

    private fun moveRow(row: Int, pos: Int) {
        val vgap = verticalGap * pos
        moveRowPanel(rowPanels[row], 0, pos * rowPanelHeight + vgap)
    }

    private fun addToRow(rowStack: LLPanel, item: LLInventoryGalleryItem, pos: Int, hgap: Int): LLPanel {
        val lpanel = buildItemPanel(pos * itemWidth + hgap)
        TODO("GPU: lpanel.addChild(item); rowStack.addChild(lpanel)")
        itemPanels.add(lpanel)
        return lpanel
    }

    private fun removeFromLastRow(item: LLInventoryGalleryItem) {
        TODO("GPU: remove item from last item panel, remove panel from lastRowPanel, push to unusedItemPanels")
    }

    private fun buildGalleryItem(
        name: String, itemId: UUID, type: LLAssetType.EType, thumbnailId: UUID,
        inventoryType: LLInventoryType.EType, flags: UInt,
        creationDate: Long, isLink: Boolean, isWorn: Boolean, isFavorite: Boolean
    ): LLInventoryGalleryItem {
        TODO("GPU: construct and configure LLInventoryGalleryItem with given parameters")
    }

    private fun getItem(id: UUID): LLInventoryGalleryItem? = itemMap[id]

    private fun buildGalleryPanel(rowCount: Int) {
        TODO("GPU: create LLGalleryPanel with bounding-rect disabled, call reshapeGalleryPanel")
    }

    private fun reshapeGalleryPanel(rowCount: Int) {
        val height = rowCount * (rowPanelHeight + verticalGap)
        TODO("GPU: set galleryPanel rect and reshape to (galleryWidth, height)")
    }

    private fun buildItemPanel(left: Int): LLPanel {
        return if (unusedItemPanels.isEmpty()) {
            TODO("GPU: create LLPanel with item dimensions at given left offset")
        } else {
            val panel = unusedItemPanels.removeLastOrNull()!!
            TODO("GPU: reposition panel to left offset, return it")
            panel
        }
    }

    private fun buildRowPanel(left: Int, bottom: Int): LLPanel {
        val stack: LLPanel = if (unusedRowPanels.isEmpty()) {
            TODO("GPU: create LLPanel with row dimensions")
        } else {
            unusedRowPanels.removeLastOrNull()!!
        }
        moveRowPanel(stack, left, bottom)
        return stack
    }

    private fun moveRowPanel(stack: LLPanel, left: Int, bottom: Int) {
        TODO("GPU: set stack rect to (left, bottom, left+rowPanelWidth, bottom+rowPanelHeight) and reshape")
    }

    private fun reArrangeRows(rowDiff: Int = 0) {
        val bufItems = items.toMutableList()
        for (it in bufItems.asReversed()) removeFromGalleryLast(it, false)
        bufItems.addAll(hiddenItems)
        hiddenItems.clear()

        itemsInRow += rowDiff
        updateGalleryWidth()

        val sortByDate = (sortOrder and LLInventoryFilter.SO_DATE) != 0u
        val sortFoldersByName = (sortOrder and LLInventoryFilter.SO_FOLDERS_BY_NAME) != 0u
        bufItems.sortWith { item1, item2 ->
            compareGalleryItem(item1, item2, sortByDate, sortFoldersByName)
        }

        for (it in bufItems) {
            it.setHidden(false)
            applyFilter(it, filterSubString)
            addToGallery(it)
        }
        filter.clearModified()
        updateMessageVisibility()
    }

    private fun updateRowsIfNeeded(): Boolean {
        TODO("GPU: check scroll container width vs rowPanelWidth, call reArrangeRows if needed")
    }

    private fun updateGalleryWidth() {
        rowPanelWidth = rowPanWidthFactor * itemsInRow - itemHorizontalGap
        galleryWidth = galleryWidthFactor * itemsInRow - itemHorizontalGap
    }

    companion object {
        fun onIdle(userdata: Any?) {
            val self = userdata as? LLInventoryGallery ?: return
            if (!self.isInitialized || !self.galleryCreated) {
                self.needsArrange = false
                return
            }
            TODO("APR: use JVM equivalent - process itemBuildQuery batch within time budget, then handle selection queue")
        }

        fun isItemCopyable(itemId: UUID): Boolean {
            TODO("APR: use JVM equivalent - check copy permission on item or linked item")
        }

        fun onDelete(notification: Any, response: Any, selectedIds: ArrayDeque<UUID>) {
            TODO("APR: use JVM equivalent - confirm then remove selected inventory items")
        }
    }
}

private fun compareGalleryItem(
    item1: LLInventoryGalleryItem,
    item2: LLInventoryGalleryItem,
    sortByDate: Boolean,
    sortFoldersByName: Boolean
): Int {
    val groupCmp = item1.getSortGroup().ordinal - item2.getSortGroup().ordinal
    if (groupCmp != 0) return groupCmp

    if (sortFoldersByName && item1.getSortGroup() != LLInventoryGalleryItem.EInventorySortGroup.SG_ITEM) {
        return item1.getItemName().compareToIgnoreCase(item2.getItemName())
    }

    val both = (item1.isDefaultImage() && item2.isDefaultImage()) || (!item1.isDefaultImage() && !item2.isDefaultImage())
    return if (both) {
        if (sortByDate) item2.getCreationDate().compareTo(item1.getCreationDate())
        else item1.getItemName().compareToIgnoreCase(item2.getItemName())
    } else {
        if (item2.isDefaultImage()) -1 else 1
    }
}

class LLInventoryGalleryItem(params: Params = Params()) : LLPanel() {

    class Params

    enum class EInventorySortGroup {
        SG_SYSTEM_FOLDER,
        SG_TRASH_FOLDER,
        SG_NORMAL_FOLDER,
        SG_ITEM
    }

    private var uuid: UUID = UUID.randomUUID()
    private var nameText: LLTextBox? = null
    private var textBgPanel: LLPanel? = null
    private var thumbnailCtrl: LLThumbnailCtrl? = null
    private var selected: Boolean = false
    private var worn: Boolean = false
    private var defaultImage: Boolean = true
    private var hidden: Boolean = false
    private var isFolder: Boolean = false
    private var isLink: Boolean = false
    private var cutGeneration: Int = 0
    private var selectedForCut: Boolean = false
    private var assetIDStr: String = ""
    private var desc: String = ""
    private var creatorName: String = ""
    private var creationDate: Long = 0L
    private var sortGroup: EInventorySortGroup = EInventorySortGroup.SG_ITEM
    private var type: LLAssetType.EType = LLAssetType.EType.AT_UNKNOWN
    private var itemName: String = ""
    private var wornSuffix: String = ""
    private var permSuffix: String = ""
    private var gallery: LLInventoryGallery? = null

    fun postBuild(): Boolean {
        nameText = TODO("GPU: find child text box 'item_name'")
        return true
    }

    override fun draw() {
        TODO("GPU: draw thumbnail, name text, selection highlight, worn/link overlays")
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        TODO("APR: use JVM equivalent - update gallery selection state with ctrl/shift modifiers")
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        TODO("APR: use JVM equivalent - update selection, update lastInteractedUUID, show context menu")
    }

    fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        TODO("APR: use JVM equivalent - open folder or execute item action")
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        TODO("APR: use JVM equivalent - finalize drag or click")
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        TODO("GPU: show tooltip with item name/desc, update hover state")
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: EDragAndDropType, cargoData: Any?,
        accept: Array<EAcceptance>, tooltipMsg: StringBuilder
    ): Boolean = TODO("APR: use JVM equivalent - delegate to gallery baseHandleDragAndDrop for this item's UUID")

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        TODO("APR: use JVM equivalent - delegate key handling to parent gallery")
    }

    fun onFocusLost() { TODO("GPU: redraw item without focus highlight") }
    fun onFocusReceived() { TODO("GPU: redraw item with focus highlight") }

    fun getTextFont(): LLFontGL? { TODO("GPU: return bold font if folder, plain otherwise") }

    fun setItemName(name: String) {
        itemName = name
        updateNameText()
    }

    fun isSelected(): Boolean = selected

    fun setSelected(value: Boolean) {
        selected = value
        TODO("GPU: redraw selection highlight")
    }

    fun setWorn(value: Boolean) {
        worn = value
        updateNameText()
    }

    fun setUUID(id: UUID) { uuid = id }
    fun getUUID(): UUID = uuid

    fun setAssetIDStr(assetId: String) { assetIDStr = assetId }
    fun getAssetIDStr(): String = assetIDStr
    fun setDescription(d: String) { desc = d }
    fun getDescription(): String = desc
    fun setCreatorName(name: String) { creatorName = name }
    fun getCreatorName(): String = creatorName
    fun setCreationDate(date: Long) { creationDate = date }
    fun getCreationDate(): Long = creationDate

    fun getItemName(): String = itemName
    fun getItemNameSuffix(): String = permSuffix + wornSuffix
    fun isDefaultImage(): Boolean = defaultImage

    fun isHidden(): Boolean = hidden
    fun setHidden(h: Boolean) { hidden = h }

    fun setType(type: LLAssetType.EType, inventoryType: LLInventoryType.EType, flags: UInt, isLink: Boolean) {
        this.type = type
        this.isLink = isLink
        isFolder = type == LLAssetType.EType.AT_CATEGORY
        sortGroup = when {
            !isFolder -> EInventorySortGroup.SG_ITEM
            else -> EInventorySortGroup.SG_NORMAL_FOLDER
        }
        TODO("APR: use JVM equivalent - set permSuffix from item flags")
    }

    fun setFavorite(isFavorite: Boolean) {
        TODO("GPU: update visual indicator for favorite state")
    }

    fun getAssetType(): LLAssetType.EType = type

    fun setThumbnail(id: UUID) {
        defaultImage = (id == UUID.randomUUID())
        TODO("GPU: update thumbnailCtrl with given UUID")
    }

    fun setGallery(g: LLInventoryGallery) { gallery = g }

    fun setLoadImmediately(value: Boolean) {
        TODO("GPU: configure thumbnail control load-immediately flag")
    }

    fun isFolder(): Boolean = isFolder
    fun isLink(): Boolean = isLink
    fun getSortGroup(): EInventorySortGroup = sortGroup

    fun updateNameText() {
        TODO("GPU: update nameText label with itemName + worn/perm suffix, choose font weight")
    }

    private fun isFadeItem(): Boolean {
        TODO("APR: use JVM equivalent - check whether item is in cut clipboard")
    }
}

class LLThumbnailsObserver : LLInventoryObserver() {

    private data class LLItemData(
        val itemId: UUID,
        val thumbnailId: UUID,
        val callback: () -> Unit
    )

    private val itemMap: MutableMap<UUID, LLItemData> = mutableMapOf()

    override fun changed(mask: UInt) {
        TODO("APR: use JVM equivalent - check changed UUIDs against itemMap, fire callbacks on thumbnail change")
    }

    fun addItem(objId: UUID, cb: () -> Unit): Boolean {
        TODO("APR: use JVM equivalent - fetch current thumbnail UUID, store entry, return true if added")
    }

    fun removeItem(objId: UUID) {
        itemMap.remove(objId)
    }
}

class LLGalleryGestureObserver(private val gallery: LLInventoryGallery) : LLGestureManagerObserver() {
    override fun changed() {
        gallery.onGesturesChanged()
    }
}

// ---------------------------------------------------------------------------
// Stub base/platform types
// ---------------------------------------------------------------------------

open class LLPanel {
    open fun draw() {}
    open fun addChild(child: Any) {}
    open fun removeChild(child: Any) {}
}

abstract class LLEditMenuHandler {
    abstract fun copy()
    abstract fun canCopy(): Boolean
    abstract fun cut()
    abstract fun canCut(): Boolean
    abstract fun paste()
    abstract fun canPaste(): Boolean
}

open class LLInventoryObserver {
    open fun changed(mask: UInt) {}
}

open class LLGestureManagerObserver {
    open fun changed() {}
}

class LLInventoryCategoriesObserver {
    fun addCategory(id: UUID, cb: () -> Unit) {}
    fun removeCategory(id: UUID) {}
}

class LLScrollContainer : LLPanel() {
    fun goToTop() {}
    fun goToBottom() {}
    fun pageUp(amount: Int) {}
    fun pageDown(amount: Int) {}
    fun addChild(child: LLPanel) {}
}

class LLTextBox : LLPanel()
class LLThumbnailCtrl : LLPanel()
class LLFontGL
class LLUICtrl : LLPanel()

class LLInventoryFilter {
    enum class ESearchType { SEARCHTYPE_NAME, SEARCHTYPE_CREATOR, SEARCHTYPE_DESCRIPTION, SEARCHTYPE_UUID }
    enum class EFolderShow { SHOW_ALL_FOLDERS, SHOW_NON_EMPTY_FOLDERS }

    companion object {
        val SO_DATE: UInt = 0x01u
        val SO_FOLDERS_BY_NAME: UInt = 0x02u
    }

    fun setFilterSubString(s: String) {}
    fun getShowFolderState(): EFolderShow = EFolderShow.SHOW_ALL_FOLDERS
    fun getFilterCreatorType(): Int = 0
    fun getSearchVisibilityTypes(): Int = 0
    fun checkAgainstFilterThumbnails(id: UUID): Boolean = true
    fun isModified(): Boolean = false
    fun clearModified() {}
}

enum class EDragAndDropType { DRAG_N_DROP_TYPE_NONE }
enum class EAcceptance { ACCEPT_NO, ACCEPT_YES_SINGLE, ACCEPT_YES_MULTI }
