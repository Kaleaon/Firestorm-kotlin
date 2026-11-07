// Converted from indra/llui/llfolderview.h + llfolderview.cpp
// Original: Copyright (C) 2010, Linden Research, Inc. (LGPL 2.1)
package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ── Data model ───────────────────────────────────────────────────────────────

/**
 * Lightweight data representation of a single item or folder in the folder
 * view tree.  Mirrors the essential fields shared by LLFolderViewItem and
 * LLFolderViewFolder in the original C++ code.
 */
data class FolderViewItem(
    val id: LLUUID,
    var name: String,
    val type: Int,                               // asset / inventory type constant
    val parent: LLUUID,
    val children: MutableList<FolderViewItem> = mutableListOf(),
    var isOpen: Boolean = false,
    var isSelected: Boolean = false,
    var passedFilter: Boolean = true             // set by filter pass; hidden when false
)

// ── Functor interface (mirrors LLFolderViewFunctor) ──────────────────────────

/**
 * Simple visitor interface applied recursively over the folder view hierarchy.
 * Mirrors [LLFolderViewFunctor] in llfolderview.h.
 */
interface FolderViewFunctor {
    fun doFolder(item: FolderViewItem)
    fun doItem(item: FolderViewItem)
}

// ── Concrete functors ─────────────────────────────────────────────────────────

/** Closes (or opens) all folders recursively.  Mirrors [LLCloseAllFoldersFunctor]. */
class CloseAllFoldersFunctor(private val close: Boolean) : FolderViewFunctor {
    override fun doFolder(item: FolderViewItem) { item.isOpen = !close }
    override fun doItem(item: FolderViewItem) { /* no-op */ }
}

/** Opens every folder whose children passed the active filter. */
class OpenFilteredFolders : FolderViewFunctor {
    override fun doFolder(item: FolderViewItem) {
        if (item.children.any { it.passedFilter }) item.isOpen = true
    }
    override fun doItem(item: FolderViewItem) { /* no-op */ }
}

/** Saves and later restores the open/closed state of folders. */
class SaveFolderState : FolderViewFunctor {
    private val openFolders: MutableSet<LLUUID> = mutableSetOf()
    private var apply: Boolean = false

    fun setApply(v: Boolean) { apply = v }
    fun clearOpenFolders() { openFolders.clear() }
    fun hasOpenFolders(): Boolean = openFolders.isNotEmpty()

    override fun doFolder(item: FolderViewItem) {
        if (apply) {
            item.isOpen = item.id in openFolders
        } else {
            if (item.isOpen) openFolders.add(item.id)
        }
    }
    override fun doItem(item: FolderViewItem) { /* no-op */ }
}

/** Selects the first item (or folder, as fallback) that passed the filter. */
class SelectFirstFilteredItem : FolderViewFunctor {
    private var itemSelected: Boolean = false
    private var folderSelected: Boolean = false

    fun wasItemSelected(): Boolean = itemSelected || folderSelected

    override fun doFolder(item: FolderViewItem) {
        if (!itemSelected && !folderSelected && item.passedFilter) {
            item.isSelected = true
            folderSelected = true
        }
    }

    override fun doItem(item: FolderViewItem) {
        if (!itemSelected && item.passedFilter) {
            item.isSelected = true
            itemSelected = true
            folderSelected = false   // item takes priority over previously selected folder
        }
    }
}

/** Reports whether ALL descendants of a folder passed the filter. */
class AllDescendentsPassedFilter : FolderViewFunctor {
    var allDescendentsPassedFilter: Boolean = true
        private set

    override fun doFolder(item: FolderViewItem) {
        allDescendentsPassedFilter = allDescendentsPassedFilter && item.passedFilter
    }
    override fun doItem(item: FolderViewItem) {
        allDescendentsPassedFilter = allDescendentsPassedFilter && item.passedFilter
    }
}

// ── Context-menu flag constants (mirrors llfolderview.h constants) ────────────

const val SUPPRESS_OPEN_ITEM: UInt    = 0x1u
const val FIRST_SELECTED_ITEM: UInt   = 0x2u
const val ITEM_IN_MULTI_SELECTION: UInt = 0x4u

// ── FolderView ────────────────────────────────────────────────────────────────

/**
 * Root-level folder-view widget.  Manages the full inventory tree hierarchy,
 * selection state, filtering, sorting and basic drag-and-drop bookkeeping.
 *
 * Mirrors [LLFolderView] from llfolderview.h / llfolderview.cpp.
 *
 * Rendering, keyboard handling, and scroll-container integration are stubbed
 * with [TODO] and must be implemented against the target UI toolkit.
 */
open class FolderView(
    val title: String = "",
    val allowMultiSelect: Boolean = true,
    val allowDrag: Boolean = true,
    val showEmptyMessage: Boolean = true,
    val useLabelSuffix: Boolean = true,
    val useEllipses: Boolean = false,
    val showItemLinkOverlays: Boolean = false,
    val suppressFolderMenu: Boolean = false
) {
    // ── Tree root ─────────────────────────────────────────────────────────────

    /** The synthetic root item that owns all top-level folders. */
    var root: FolderViewItem? = null
        protected set

    // ── Selection ─────────────────────────────────────────────────────────────

    /** Items currently selected, in selection order. */
    val selectedItems: MutableList<FolderViewItem> = mutableListOf()

    /** Flat index for O(1) membership tests. */
    private val selectedSet: MutableSet<LLUUID> = mutableSetOf()

    // ── Filter state ──────────────────────────────────────────────────────────

    var filterString: String = ""
        private set

    // ── Arrange / layout bookkeeping (mirrors mArrangeGeneration) ────────────

    var arrangeGeneration: Int = 0
        private set

    // ── Drag state ────────────────────────────────────────────────────────────

    private var dragStartX: Int = 0
    private var dragStartY: Int = 0
    var draggingOverItem: FolderViewItem? = null
    private var dragAndDropThisFrame: Boolean = false

    // ── Auto-open stack ───────────────────────────────────────────────────────

    private val autoOpenItems: ArrayDeque<FolderViewItem> = ArrayDeque()

    // ── Rename state ──────────────────────────────────────────────────────────

    var renameItem: FolderViewItem? = null
        private set

    // ── Selection callbacks ───────────────────────────────────────────────────

    private val selectCallbacks: MutableList<(List<FolderViewItem>, Boolean) -> Unit> = mutableListOf()
    private val reshapeCallbacks: MutableList<(List<FolderViewItem>, Boolean) -> Unit> = mutableListOf()

    fun setSelectCallback(cb: (List<FolderViewItem>, Boolean) -> Unit) {
        selectCallbacks.add(cb)
    }
    fun setReshapeCallback(cb: (List<FolderViewItem>, Boolean) -> Unit) {
        reshapeCallbacks.add(cb)
    }

    // ── Item management ───────────────────────────────────────────────────────

    /**
     * Adds [item] as a child of the matching parent in the tree.  If no parent
     * is found, attaches directly to [root].
     */
    open fun addItem(item: FolderViewItem) {
        val r = root ?: return
        val parentNode = findById(r, item.parent)
        val target = parentNode ?: r
        target.children.add(item)
    }

    /**
     * Removes the item with [id] from the tree, also deselects it if selected.
     */
    open fun removeItem(id: LLUUID) {
        val r = root ?: return
        removeFromTree(r, id)
        deselectById(id)
    }

    // ── Selection API ─────────────────────────────────────────────────────────

    /**
     * Selects the item identified by [id], clearing the existing selection
     * unless [addToSelection] is true.
     */
    fun selectItem(id: LLUUID, addToSelection: Boolean = false) {
        val r = root ?: return
        val target = findById(r, id) ?: return
        if (!addToSelection) clearSelection()
        if (!selectedSet.contains(id)) {
            target.isSelected = true
            selectedItems.add(target)
            selectedSet.add(id)
            notifySelectCallbacks(userAction = true)
        }
    }

    /** Deselects all currently selected items. */
    open fun clearSelection() {
        selectedItems.forEach { it.isSelected = false }
        selectedItems.clear()
        selectedSet.clear()
    }

    fun addToSelectionList(item: FolderViewItem) {
        if (!selectedSet.contains(item.id)) {
            item.isSelected = true
            selectedItems.add(item)
            selectedSet.add(item.id)
        }
    }

    fun removeFromSelectionList(item: FolderViewItem) {
        if (selectedSet.remove(item.id)) {
            item.isSelected = false
            selectedItems.remove(item)
        }
    }

    /** Returns the most recently selected item (last in [selectedItems]). */
    open fun getCurSelectedItem(): FolderViewItem? = selectedItems.lastOrNull()

    fun getSelectedCount(): Int = selectedItems.size

    // ── Filter ────────────────────────────────────────────────────────────────

    /**
     * Applies a substring filter, marking [FolderViewItem.passedFilter] for
     * each node in the tree.  Complex scoring / date filtering is stubbed.
     */
    open fun setFilter(filter: String) {
        filterString = filter
        val r = root ?: return
        applyFilter(r, filter.lowercase())
    }

    private fun applyFilter(item: FolderViewItem, lowerFilter: String) {
        item.passedFilter = lowerFilter.isEmpty() || item.name.lowercase().contains(lowerFilter)
        item.children.forEach { applyFilter(it, lowerFilter) }
        // parent folder passes if any child passes
        if (!item.passedFilter && item.children.any { it.passedFilter }) {
            item.passedFilter = true
        }
    }

    // ── Sort ─────────────────────────────────────────────────────────────────

    /** Sorts child lists alphabetically by name throughout the tree. */
    open fun sort() {
        root?.let { sortRecursive(it) }
    }

    private fun sortRecursive(item: FolderViewItem) {
        item.children.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        item.children.forEach { sortRecursive(it) }
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    /**
     * Marks the view as needing a layout pass.  Actual pixel geometry
     * computation requires the target UI toolkit and is stubbed here.
     */
    open fun arrange() {
        arrangeGeneration++
        System.err.println("FolderView: arrange not yet implemented")
    }

    fun arrangeAll() { arrangeGeneration++ }

    // ── Folder open / close ───────────────────────────────────────────────────

    fun closeAllFolders() {
        root?.let { applyFunctor(it, CloseAllFoldersFunctor(close = true)) }
    }

    fun openTopLevelFolders() {
        root?.children?.forEach { it.isOpen = true }
    }

    // ── Functor application ───────────────────────────────────────────────────

    fun applyFunctor(item: FolderViewItem, functor: FolderViewFunctor) {
        if (item.children.isNotEmpty()) {
            functor.doFolder(item)
            item.children.forEach { applyFunctor(it, functor) }
        } else {
            functor.doItem(item)
        }
    }

    // ── Drag and drop ─────────────────────────────────────────────────────────

    fun setDragStart(screenX: Int, screenY: Int) {
        dragStartX = screenX
        dragStartY = screenY
    }

    fun isOverDragThreshold(screenX: Int, screenY: Int): Boolean {
        val threshold = 4
        return Math.abs(screenX - dragStartX) > threshold ||
               Math.abs(screenY - dragStartY) > threshold
    }

    fun setDragAndDropThisFrame() { dragAndDropThisFrame = true }

    // ── Auto-open ────────────────────────────────────────────────────────────

    fun autoOpenItem(folder: FolderViewItem) {
        if (autoOpenItems.size >= AUTO_OPEN_STACK_DEPTH) autoOpenItems.removeFirst()
        autoOpenItems.addLast(folder)
        folder.isOpen = true
    }

    fun closeAutoOpenedFolders() {
        autoOpenItems.forEach { it.isOpen = false }
        autoOpenItems.clear()
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    fun startRenamingSelectedItem() {
        renameItem = getCurSelectedItem()
        System.err.println("FolderView: startRenamingSelectedItem not yet implemented")
    }

    fun finishRenamingItem() {
        renameItem = null
        System.err.println("FolderView: finishRenamingItem not yet implemented")
    }

    // ── Render / draw ─────────────────────────────────────────────────────────

    /** Renders the folder view.  Stubbed — requires UI toolkit integration. */
    open fun draw() {
        // no-op
    }

    // ── Scroll helpers ────────────────────────────────────────────────────────

    fun scrollToShowSelection() {
        System.err.println("FolderView: scrollToShowSelection not yet implemented")
    }

    // ── Status / empty message ────────────────────────────────────────────────

    var showEmptyMessageFlag: Boolean = showEmptyMessage

    // ── Context menu ─────────────────────────────────────────────────────────

    open fun updateMenu() {
        System.err.println("FolderView: updateMenu not yet implemented")
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun search(startItem: FolderViewItem, searchString: String, backward: Boolean): Boolean {
        return false
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    fun dumpSelectionInformation() {
        selectedItems.forEach { println("Selected: ${it.id} '${it.name}'") }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun findById(item: FolderViewItem, id: LLUUID): FolderViewItem? {
        if (item.id == id) return item
        for (child in item.children) {
            val found = findById(child, id)
            if (found != null) return found
        }
        return null
    }

    private fun removeFromTree(item: FolderViewItem, id: LLUUID): Boolean {
        val iter = item.children.iterator()
        while (iter.hasNext()) {
            val child = iter.next()
            if (child.id == id) { iter.remove(); return true }
            if (removeFromTree(child, id)) return true
        }
        return false
    }

    private fun deselectById(id: LLUUID) {
        val target = selectedItems.firstOrNull { it.id == id } ?: return
        target.isSelected = false
        selectedItems.remove(target)
        selectedSet.remove(id)
    }

    private fun notifySelectCallbacks(userAction: Boolean) {
        val snapshot = selectedItems.toList()
        selectCallbacks.forEach { it(snapshot, userAction) }
    }

    companion object {
        /** Auto-open stack depth limit.  Mirrors AUTO_OPEN_STACK_DEPTH in .cpp. */
        const val AUTO_OPEN_STACK_DEPTH: Int = 16

        /** Seconds before a hovered folder auto-opens.  Mirrors sAutoOpenTime. */
        var autoOpenTime: Float = 1.0f
    }
}
