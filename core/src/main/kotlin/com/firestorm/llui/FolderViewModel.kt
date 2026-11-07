package com.firestorm.llui

import com.firestorm.llmath.Rect

enum class InventorySortGroup {
    SYSTEM_FOLDER,
    TRASH_FOLDER,
    NORMAL_FOLDER,
    ITEM
}

interface FolderViewFilter {

    enum class FilterModified {
        NONE,
        RESTART,
        LESS_RESTRICTIVE,
        MORE_RESTRICTIVE
    }

    fun check(item: FolderViewModelItem): Boolean
    fun checkFolder(folder: FolderViewModelItem): Boolean

    fun setEmptyLookupMessage(message: String)
    fun getEmptyLookupMessage(isEmptyFolder: Boolean = false): String

    fun showAllResults(): Boolean

    fun getStringMatchOffset(item: FolderViewModelItem): Int
    fun getFilterStringSize(): Int

    fun isActive(): Boolean
    fun isModified(): Boolean
    fun clearModified()
    fun getName(): String
    fun getFilterText(): String
    fun setModified(behavior: FilterModified = FilterModified.RESTART)

    fun resetTime(timeout: Int)
    fun isTimedOut(): Boolean

    fun isDefault(): Boolean
    fun isNotDefault(): Boolean
    fun markDefault()
    fun resetDefault()

    fun getCurrentGeneration(): Int
    fun getFirstSuccessGeneration(): Int
    fun getFirstRequiredGeneration(): Int
}

interface FolderViewModelInterface {
    fun requestSortAll()
    fun sort(folder: FolderViewFolder)
    fun filter()
    fun contentsReady(): Boolean
    fun isFolderComplete(folder: FolderViewFolder): Boolean
    fun setFolderView(folderView: FolderView)
    fun getFilter(): FolderViewFilter
    fun getStatusText(isEmptyFolder: Boolean = false): String
    fun startDrag(items: MutableList<FolderViewModelItem>): Boolean
}

abstract class FolderViewModelItem {

    open fun update() {}

    abstract fun getName(): String
    abstract fun getDisplayName(): String
    abstract fun getSearchableName(): String

    abstract fun getSearchableDescription(): String
    abstract fun getSearchableCreatorName(): String
    abstract fun getSearchableUUIDString(): String
    abstract fun getSearchableAll(): String

    abstract fun getIcon(): UIImage?
    open fun getIconOpen(): UIImage? = getIcon()
    open fun getIconOverlay(): UIImage? = null

    abstract fun getLabelStyle(): Int
    abstract fun getLabelSuffix(): String

    abstract fun openItem()
    abstract fun closeItem()
    abstract fun selectItem()

    abstract fun navigateToFolder(newWindow: Boolean = false, changeMode: Boolean = false)

    abstract fun isFavorite(): Boolean
    open fun isItemWearable(): Boolean = false

    abstract fun isItemRenameable(): Boolean
    abstract fun renameItem(newName: String): Boolean

    abstract fun isItemMovable(): Boolean
    abstract fun move(parentListener: FolderViewModelItem)

    abstract fun isItemRemovable(checkWorn: Boolean = true): Boolean
    abstract fun isItemInTrash(): Boolean
    abstract fun removeItem(): Boolean
    abstract fun removeBatch(batch: MutableList<FolderViewModelItem>)

    abstract fun isItemCopyable(canCopyAsLink: Boolean = true): Boolean
    abstract fun copyToClipboard(): Boolean
    abstract fun cutToClipboard(): Boolean
    open fun isCutToClipboard(): Boolean = false

    abstract fun isClipboardPasteable(): Boolean
    abstract fun pasteFromClipboard()
    abstract fun pasteLinkFromClipboard()

    abstract fun isAgentInventory(): Boolean
    abstract fun isAgentInventoryRoot(): Boolean

    abstract fun buildContextMenu(menu: MenuGL, flags: UInt)

    abstract fun potentiallyVisible(): Boolean

    abstract fun filter(filter: FolderViewFilter): Boolean
    abstract fun passedFilter(filterGeneration: Int = -1): Boolean
    abstract fun descendantsPassedFilter(filterGeneration: Int = -1): Boolean
    abstract fun setPassedFilter(
        passed: Boolean,
        filterGeneration: Int,
        stringOffset: Int = Int.MIN_VALUE,
        stringSize: Int = 0
    )
    abstract fun setPassedFolderFilter(passed: Boolean, filterGeneration: Int)
    abstract fun dirtyFilter()
    abstract fun dirtyDescendantsFilter()
    abstract fun hasFilterStringMatch(): Boolean
    abstract fun getFilterStringOffset(): Int
    abstract fun getFilterStringSize(): Int

    abstract fun getLastFilterGeneration(): Int
    abstract fun getMarkedDirtyGeneration(): Int

    abstract fun hasChildren(): Boolean
    abstract fun addChild(child: FolderViewModelItem)
    abstract fun removeChild(child: FolderViewModelItem)
    abstract fun clearChildren()

    abstract fun dragOrDrop(
        mask: UInt,
        drop: Boolean,
        cargoType: DragAndDropType,
        cargoData: Any?,
        tooltipMsg: StringBuilder
    ): Boolean

    abstract fun requestSort()
    abstract fun getSortVersion(): Int
    abstract fun setSortVersion(version: Int)
    abstract fun setParent(parent: FolderViewModelItem?)
    abstract fun getParent(): FolderViewModelItem?
    abstract fun hasParent(): Boolean

    open fun isLocked(): Boolean = false
    open fun isProtected(): Boolean = false

    internal abstract fun setFolderViewItem(folderViewItem: FolderViewItem?)
}

abstract class FolderViewModelItemCommon(
    protected val rootViewModel: FolderViewModelInterface
) : FolderViewModelItem() {

    protected var sortVersion: Int = -1
    protected var passedFilterFlag: Boolean = true
    protected var passedFolderFilterFlag: Boolean = true
    protected var stringMatchOffsetFilter: Int = Int.MIN_VALUE
    protected var stringFilterSize: Int = 0
    protected var folderViewItem: FolderViewItem? = null

    protected var lastFilterGeneration: Int = -1
    protected var lastFolderFilterGeneration: Int = -1
    protected var mostFilteredDescendantGeneration: Int = -1
    protected var markedDirtyGeneration: Int = -1

    protected val children: MutableList<FolderViewModelItem> = mutableListOf()
    protected var parentItem: FolderViewModelItem? = null

    override fun requestSort() { sortVersion = -1 }
    override fun getSortVersion(): Int = sortVersion
    override fun setSortVersion(version: Int) { sortVersion = version }

    override fun getLastFilterGeneration(): Int = lastFilterGeneration
    fun getLastFolderFilterGeneration(): Int = lastFolderFilterGeneration
    override fun getMarkedDirtyGeneration(): Int = markedDirtyGeneration

    override fun dirtyFilter() {
        if (markedDirtyGeneration < 0) {
            markedDirtyGeneration = lastFilterGeneration
        }
        lastFilterGeneration = -1
        lastFolderFilterGeneration = -1
        parentItem?.dirtyFilter()
    }

    override fun dirtyDescendantsFilter() {
        mostFilteredDescendantGeneration = -1
        parentItem?.dirtyDescendantsFilter()
    }

    override fun hasFilterStringMatch(): Boolean = stringMatchOffsetFilter != Int.MIN_VALUE

    override fun getFilterStringOffset(): Int = stringMatchOffsetFilter

    override fun getFilterStringSize(): Int = rootViewModel.getFilter().getFilterStringSize()

    override fun addChild(child: FolderViewModelItem) {
        children.add(child)
        child.setParent(this)
        dirtyFilter()
        requestSort()
    }

    final override fun removeChild(child: FolderViewModelItem) {
        child.setParent(null)
        children.remove(child)
        dirtyDescendantsFilter()
        dirtyFilter()
    }

    override fun clearChildren() {
        children.forEach { it.setParent(null) }
        children.clear()
        dirtyDescendantsFilter()
        dirtyFilter()
    }

    fun childrenIterator(): Iterator<FolderViewModelItem> = children.iterator()
    fun childCount(): Int = children.size

    override fun setPassedFilter(
        passed: Boolean,
        filterGeneration: Int,
        stringOffset: Int,
        stringSize: Int
    ) {
        passedFilterFlag = passed
        lastFilterGeneration = filterGeneration
        stringMatchOffsetFilter = stringOffset
        stringFilterSize = stringSize
        markedDirtyGeneration = -1
    }

    override fun setPassedFolderFilter(passed: Boolean, filterGeneration: Int) {
        passedFolderFilterFlag = passed
        lastFolderFilterGeneration = filterGeneration
    }

    override fun potentiallyVisible(): Boolean {
        return passedFilter()
            || lastFilterGeneration < rootViewModel.getFilter().getFirstSuccessGeneration()
            || descendantsPassedFilter()
    }

    override fun passedFilter(filterGeneration: Int): Boolean {
        val gen = if (filterGeneration < 0) rootViewModel.getFilter().getFirstSuccessGeneration() else filterGeneration
        val passedFolder = passedFolderFilterFlag && lastFolderFilterGeneration >= gen
        val passedItem = passedFilterFlag && lastFilterGeneration >= gen
        return passedFolder && (passedItem || descendantsPassedFilter(gen))
    }

    override fun descendantsPassedFilter(filterGeneration: Int): Boolean {
        val gen = if (filterGeneration < 0) rootViewModel.getFilter().getFirstSuccessGeneration() else filterGeneration
        return mostFilteredDescendantGeneration >= gen
    }

    final override fun setParent(parent: FolderViewModelItem?) { parentItem = parent }
    override fun getParent(): FolderViewModelItem? = parentItem
    override fun hasParent(): Boolean = parentItem != null

    override fun setFolderViewItem(folderViewItem: FolderViewItem?) {
        this.folderViewItem = folderViewItem
    }
}

abstract class FolderViewModelCommon : FolderViewModelInterface {

    protected var targetSortVersion: Int = 0
    protected var folderView: FolderView? = null

    override fun requestSortAll() {
        targetSortVersion++
    }

    override fun getStatusText(isEmptyFolder: Boolean): String {
        val view = folderView ?: return getFilter().getEmptyLookupMessage(isEmptyFolder)
        return if (!contentsReady() ||
            view.getViewModelItem().getLastFilterGeneration() < getFilter().getCurrentGeneration()
        ) {
            "Searching"
        } else {
            getFilter().getEmptyLookupMessage(isEmptyFolder)
        }
    }

    override fun filter() {
        val maxFilterTimeMs = 10
        getFilter().resetTime(maxFilterTimeMs)
        folderView?.getViewModelItem()?.filter(getFilter())
    }

    override fun setFolderView(folderView: FolderView) {
        this.folderView = folderView
    }

    protected fun needsSort(item: FolderViewModelItem): Boolean {
        return item.getSortVersion() < targetSortVersion
    }
}

open class FolderViewModel<SorterT, FilterT : FolderViewFilter>(
    private var sorter: SorterT,
    private var filterInstance: FilterT,
    private val sortComparator: Comparator<FolderViewModelItem>
) : FolderViewModelCommon() {

    fun getSorter(): SorterT = sorter
    fun setSorter(newSorter: SorterT) {
        sorter = newSorter
        requestSortAll()
    }

    override fun getFilter(): FilterT = filterInstance
    fun setFilter(newFilter: FilterT) { filterInstance = newFilter }

    override fun contentsReady(): Boolean = true
    override fun isFolderComplete(folder: FolderViewFolder): Boolean = true

    override fun sort(folder: FolderViewFolder) {
        if (needsSort(folder.getViewModelItem())) {
            folder.sortFolders(sortComparator)
            folder.sortItems(sortComparator)
            folder.getViewModelItem().setSortVersion(targetSortVersion)
            folder.requestArrange()
        }
    }

    override fun startDrag(items: MutableList<FolderViewModelItem>): Boolean = false
}

class UIImage(val name: String, val width: Int = 0, val height: Int = 0) {
    fun draw(rect: Rect, color: Any?) {
        // no-op
    }
}

// FolderView, FolderViewFolder, FolderViewItem are defined in FolderViewItem.kt
// MenuGL is defined in LLMenuGL.kt

enum class DragAndDropType {
    NONE, CATEGORY, OBJECT, SCRIPT, TEXTURE, SOUND, LANDMARK, CLOTHING,
    NOTECARD, BODYPART, ANIMATION, GESTURE, LINK, LINK_FOLDER, MESH, SETTINGS,
    MATERIAL, UNKNOWN
}
