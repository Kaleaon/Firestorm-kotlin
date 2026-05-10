package com.firestorm.llui

import java.util.UUID
import kotlin.math.max

typealias LlsdValue = Any?

fun llsdsAreEqual(a: LlsdValue, b: LlsdValue): Boolean {
    if (a == null || b == null) return false
    if (a is UUID && b is UUID) return a == b
    if (a is Map<*, *> && b is Map<*, *>) {
        if (a.size != b.size) return false
        for ((k, v) in a) {
            if (!llsdsAreEqual(v, b[k])) return false
        }
        return true
    }
    return a.toString() == b.toString()
}

abstract class Panel {
    var visible: Boolean = true
    var parent: Panel? = null
    var tabStop: Boolean = true

    open fun notify(action: Map<String, Any?>): Int = 0
    open fun setValue(event: Map<String, Any?>) {}
    fun getVisible(): Boolean = visible
    fun setVisible(v: Boolean) { visible = v }
    abstract fun getRect(): Rect
    abstract fun reshape(width: Int, height: Int)
}

data class Rect(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0) {
    val width get() = right - left
    val height get() = top - bottom
    fun isValid() = width > 0 && height > 0
    fun stretch(delta: Int) = copy(left = left - delta, top = top + delta, right = right + delta, bottom = bottom - delta)
    companion object {
        val NULL = Rect()
    }
}

enum class AddPosition { TOP, BOTTOM, DEFAULT }

abstract class ItemComparator {
    abstract fun compare(item1: Panel, item2: Panel): Boolean
}

class ItemReverseComparator(private val comparator: ItemComparator) : ItemComparator() {
    override fun compare(item1: Panel, item2: Panel): Boolean = comparator.compare(item2, item1)
}

data class ItemPair(val panel: Panel, var value: LlsdValue)

open class FlatListView(
    allowSelect: Boolean = false,
    multiSelect: Boolean = false,
    keepOneSelected: Boolean = false,
    keepSelectionVisibleOnReshape: Boolean = false,
    val itemPad: Int = 0,
    noItemsText: String = "",
    val magicalHackyHeightPadding: Int = 0,
) {
    protected var itemComparator: ItemComparator? = null

    protected val itemPairs: MutableList<ItemPair> = mutableListOf()
    protected val selectedItemPairs: MutableList<ItemPair> = mutableListOf()

    var allowSelection: Boolean = allowSelect
    var multipleSelection: Boolean = multiSelect
    var keepOneItemSelected: Boolean = keepOneSelected
    var commitOnSelectionChange: Boolean = false
    var keepSelectionVisibleOnReshape: Boolean = keepSelectionVisibleOnReshape
    var focusOnItemClicked: Boolean = true

    private var isConsecutiveSelection: Boolean = false

    var noItemsCommentText: String = noItemsText

    private var prevNotifyParentRect: Rect = Rect.NULL

    var onReturnSignal: (() -> Unit)? = null
    var onCommitCallback: (() -> Unit)? = null

    open fun canFocusChildren(): Boolean = false

    fun getItemsRect(): Rect = TODO("APR: use JVM equivalent for rect from items panel")

    fun getItemsPad(): Int = itemPad

    open fun addItem(item: Panel, value: LlsdValue = null, pos: AddPosition = AddPosition.BOTTOM, rearrange: Boolean = true): Boolean {
        if (value == null) return false
        if (item.parent != null) return false

        val pair = ItemPair(item, value)
        when (pos) {
            AddPosition.TOP -> itemPairs.add(0, pair)
            AddPosition.BOTTOM, AddPosition.DEFAULT -> itemPairs.add(pair)
        }

        item.tabStop = false

        if (rearrange) {
            rearrangeItems()
            notifyParentItemsRectChanged()
        }
        return true
    }

    open fun insertItemAfter(afterItem: Panel, itemToAdd: Panel, value: LlsdValue = null): Boolean {
        if (value == null) return false
        if (itemPairs.isEmpty()) return false
        if (itemToAdd.parent != null) return false

        val afterPair = getItemPair(afterItem) ?: return false
        val newPair = ItemPair(itemToAdd, value)

        val idx = itemPairs.indexOf(afterPair)
        if (idx == -1) return false
        itemPairs.add(idx + 1, newPair)
        itemToAdd.tabStop = false

        rearrangeItems()
        notifyParentItemsRectChanged()
        return true
    }

    open fun removeItem(item: Panel, rearrange: Boolean = true): Boolean {
        val pair = getItemPair(item) ?: return false
        return removeItemPair(pair, rearrange)
    }

    open fun removeItemByValue(value: LlsdValue, rearrange: Boolean = true): Boolean {
        if (value == null) return false
        val pair = getItemPair(value) ?: return false
        return removeItemPair(pair, rearrange)
    }

    open fun removeItemByUUID(uuid: UUID, rearrange: Boolean = true): Boolean =
        removeItemByValue(uuid, rearrange)

    open fun getItemByValue(value: LlsdValue): Panel? {
        if (value == null) return null
        return getItemPair(value)?.panel
    }

    open fun selectItem(item: Panel, select: Boolean = true): Boolean {
        val pair = getItemPair(item) ?: return false
        return selectItemPair(pair, select)
    }

    open fun selectItemByValue(value: LlsdValue, select: Boolean = true): Boolean {
        if (value == null) return false
        val pair = getItemPair(value) ?: return false
        return selectItemPair(pair, select)
    }

    open fun selectItemByUUID(uuid: UUID, select: Boolean = true): Boolean =
        selectItemByValue(uuid, select)

    open fun getItems(): List<Panel> = itemPairs.map { it.panel }

    open fun getValues(): List<LlsdValue> = itemPairs.map { it.value }

    open fun getSelectedValue(): LlsdValue = selectedItemPairs.firstOrNull()?.value

    open fun getSelectedValues(): List<LlsdValue> = selectedItemPairs.map { it.value }

    open fun getSelectedUUID(): UUID? {
        val v = getSelectedValue()
        return v as? UUID
    }

    open fun getSelectedUUIDs(): List<UUID> =
        selectedItemPairs.mapNotNull { it.value as? UUID }

    open fun getSelectedItem(): Panel? = selectedItemPairs.firstOrNull()?.panel

    open fun getSelectedItems(): List<Panel> = selectedItemPairs.map { it.panel }

    open fun resetSelection(noCommitOnDeselection: Boolean = false) {
        if (selectedItemPairs.isEmpty()) return

        for (pair in selectedItemPairs) {
            pair.panel.setValue(mapOf("selected" to false))
        }
        selectedItemPairs.clear()

        if (commitOnSelectionChange && !noCommitOnDeselection) {
            onCommit()
        }
    }

    fun setNoItemsCommentText(text: String) {
        noItemsCommentText = text
    }

    fun numSelected(): UInt = selectedItemPairs.size.toUInt()

    fun size(onlyVisibleItems: Boolean = true): UInt {
        return if (onlyVisibleItems) {
            itemPairs.count { it.panel.getVisible() }.toUInt()
        } else {
            itemPairs.size.toUInt()
        }
    }

    open fun clear() {
        resetSelection()
        itemPairs.clear()
        notifyParentItemsRectChanged()
    }

    fun setComparator(comp: ItemComparator) {
        itemComparator = comp
    }

    fun sort() {
        val comp = itemComparator ?: return
        itemPairs.sortWith { a, b -> if (comp.compare(a.panel, b.panel)) -1 else 1 }
        rearrangeItems()
        notifyParentItemsRectChanged()
    }

    fun updateValue(oldValue: LlsdValue, newValue: LlsdValue): Boolean {
        if (oldValue == null || newValue == null) return false
        if (llsdsAreEqual(oldValue, newValue)) return false
        val pair = getItemPair(oldValue) ?: return false
        pair.value = newValue
        return true
    }

    fun scrollToShowFirstSelectedItem() {
        if (selectedItemPairs.isEmpty()) return
        val rect = selectedItemPairs.first().panel.getRect()
        if (rect.isValid()) {
            TODO("APR: use JVM equivalent for scrollToShowRect($rect)")
        }
    }

    fun selectFirstItem() {
        if (size() == 0u) return
        val first = itemPairs.firstOrNull { it.panel.getVisible() } ?: return
        selectItemPair(first, true)
        ensureSelectedVisible()
    }

    fun selectLastItem() {
        if (size() == 0u) return
        val last = itemPairs.lastOrNull { it.panel.getVisible() } ?: return
        selectItemPair(last, true)
        ensureSelectedVisible()
    }

    open fun notify(info: Map<String, Any?>): Int {
        val action = info["action"] as? String
        if (action != null) {
            return when (action) {
                "select_first" -> { selectFirstItem(); 1 }
                "select_last" -> { selectLastItem(); 1 }
                else -> 0
            }
        }
        if (info.containsKey("rearrange")) {
            rearrangeItems()
            notifyParentItemsRectChanged()
            return 1
        }
        return 0
    }

    fun detachItems(): List<Panel> {
        val detachedItems = mutableListOf<Panel>()
        val detachAction = mapOf<String, Any?>("detach" to null)

        for (pair in itemPairs.toList()) {
            if (pair.panel.notify(detachAction) == 1) {
                selectItemPair(pair, false)
                detachedItems.add(pair.panel)
            }
        }

        if (detachedItems.isNotEmpty()) {
            if (detachedItems.size == itemPairs.size) {
                itemPairs.clear()
            } else {
                detachedItems.forEach { detached ->
                    itemPairs.removeAll { it.panel == detached }
                }
                rearrangeItems()
            }
            notifyParentItemsRectChanged()
        }

        return detachedItems
    }

    protected open fun rearrangeItems() {
        TODO("GPU: reposition all visible item panels vertically using itemPad spacing")
    }

    protected fun getItemPair(item: Panel): ItemPair? =
        itemPairs.firstOrNull { it.panel === item }

    protected fun getItemPair(value: LlsdValue): ItemPair? =
        itemPairs.firstOrNull { llsdsAreEqual(it.value, value) }

    protected open fun selectItemPair(pair: ItemPair, select: Boolean): Boolean {
        if (!allowSelection && select) return false
        if (isSelected(pair) == select) return true

        if (select) {
            selectedItemPairs.add(pair)
        } else {
            selectedItemPairs.remove(pair)
        }

        pair.panel.setValue(mapOf("selected" to select))

        if (commitOnSelectionChange) onCommit()

        isConsecutiveSelection = false
        return true
    }

    protected open fun selectNextItemPair(isUpDirection: Boolean, resetSelection: Boolean): Boolean {
        if (size() == 0u) return false

        if (!isConsecutiveSelection) {
            if (selectedItemPairs.isNotEmpty() && !resetSelection) {
                val cur = selectedItemPairs.last()
                resetSelection()
                selectItemPair(cur, true)
            }
        }

        if (selectedItemPairs.isNotEmpty()) {
            val curSel = selectedItemPairs.last()
            val toPair: ItemPair?

            if (isUpDirection) {
                val idx = itemPairs.indexOf(curSel)
                toPair = itemPairs.take(idx).lastOrNull { it.panel.getVisible() }
            } else {
                val idx = itemPairs.indexOf(curSel)
                toPair = itemPairs.drop(idx + 1).firstOrNull { it.panel.getVisible() }
            }

            if (toPair != null) {
                val doSelect: Boolean
                if (resetSelection) {
                    resetSelection()
                    doSelect = true
                } else {
                    doSelect = !selectedItemPairs.contains(toPair)
                }
                selectItemPair(if (doSelect) toPair else curSel, doSelect)
                isConsecutiveSelection = true
                return true
            }
        } else {
            if (isUpDirection) selectLastItem() else selectFirstItem()
            isConsecutiveSelection = true
            return true
        }

        return false
    }

    open fun canSelectAll(): Boolean = size() != 0u && allowSelection && multipleSelection

    open fun selectAll() {
        if (!allowSelection || !multipleSelection) return

        selectedItemPairs.clear()
        for (pair in itemPairs) {
            selectedItemPairs.add(pair)
            pair.panel.setValue(mapOf("selected" to true))
        }

        if (commitOnSelectionChange) onCommit()
    }

    protected fun isSelected(pair: ItemPair): Boolean = selectedItemPairs.contains(pair)

    protected open fun removeItemPair(pair: ItemPair, rearrange: Boolean): Boolean {
        val removed = itemPairs.remove(pair)
        if (!removed) return false

        val selectionChanged = selectedItemPairs.remove(pair)

        if (rearrange) {
            rearrangeItems()
            notifyParentItemsRectChanged()
        }

        if (selectionChanged && commitOnSelectionChange) onCommit()

        return true
    }

    protected fun notifyParentItemsRectChanged() {
        TODO("APR: use JVM equivalent for notifying parent of size_changes")
    }

    protected fun getLastSelectedItemRect(): Rect =
        selectedItemPairs.lastOrNull()?.panel?.getRect() ?: Rect.NULL

    protected fun ensureSelectedVisible() {
        val rect = getLastSelectedItemRect()
        if (rect.isValid()) {
            TODO("APR: use JVM equivalent for scrollToShowRect($rect)")
        }
    }

    protected fun onFocusReceived() {}

    protected fun onFocusLost() {}

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        val KEY_RETURN = 0x0D
        val KEY_UP = 0x26
        val KEY_DOWN = 0x28
        val KEY_ESCAPE = 0x1B
        val MASK_SHIFT = 0x01
        val MASK_NONE = 0x00

        val resetSelection = mask != MASK_SHIFT
        var handled = false

        when (key) {
            KEY_RETURN -> {
                if (selectedItemPairs.isNotEmpty() && mask == MASK_NONE) {
                    onReturnSignal?.invoke()
                    handled = true
                }
            }
            KEY_UP -> {
                if (!selectNextItemPair(true, resetSelection) && resetSelection) {
                    resetSelection()
                }
            }
            KEY_DOWN -> {
                if (!selectNextItemPair(false, resetSelection) && resetSelection) {
                    resetSelection()
                }
            }
            KEY_ESCAPE -> {
                if (mask == MASK_NONE) {
                    TODO("APR: use JVM equivalent for setFocus(false)")
                }
            }
        }

        if ((key == KEY_UP || key == KEY_DOWN) && selectedItemPairs.isNotEmpty()) {
            ensureSelectedVisible()
            handled = true
        }

        return handled
    }

    private fun onCommit() {
        onCommitCallback?.invoke()
    }
}

open class FlatListViewEx(
    allowSelect: Boolean = false,
    multiSelect: Boolean = false,
    keepOneSelected: Boolean = false,
    keepSelectionVisibleOnReshape: Boolean = false,
    itemPad: Int = 0,
    noItemsText: String = "",
    magicalHackyHeightPadding: Int = 0,
    private var noItemsMsg: String = "",
    private var noFilteredItemsMsg: String = "",
) : FlatListView(
    allowSelect, multiSelect, keepOneSelected,
    keepSelectionVisibleOnReshape, itemPad, noItemsText, magicalHackyHeightPadding
) {
    private var filterSubString: String = ""
    private var forceShowingUnmatchedItems: Boolean = false
    private var hasMatchedItems: Boolean = false

    fun getNoItemsMsg(): String = noItemsMsg
    fun setNoItemsMsg(msg: String) { noItemsMsg = msg }
    fun setNoFilteredItemsMsg(msg: String) { noFilteredItemsMsg = msg }

    fun getForceShowingUnmatchedItems(): Boolean = forceShowingUnmatchedItems

    fun setForceShowingUnmatchedItems(show: Boolean, notifyParent: Boolean) {
        if (forceShowingUnmatchedItems != show) {
            forceShowingUnmatchedItems = show
            if (filterSubString.isNotEmpty()) {
                updateNoItemsMessage(filterSubString)
                filterItems(reSortItems = false, notifyParent = true)
            }
        }
    }

    fun setFilterSubString(filterStr: String, notifyParent: Boolean) {
        if (!filterStr.equals(filterSubString, ignoreCase = true)) {
            filterSubString = filterStr
            updateNoItemsMessage(filterSubString)
            filterItems(reSortItems = false, notifyParent = notifyParent)
        }
    }

    fun getFilterSubString(): String = filterSubString

    fun filterItems(reSortItems: Boolean, notifyParent: Boolean): Boolean {
        val upperFilter = filterSubString.uppercase()
        val action = mapOf("match_filter" to upperFilter)

        hasMatchedItems = false
        var visibilityChanged = false

        for (pair in itemPairs) {
            visibilityChanged = visibilityChanged or updateItemVisibility(pair.panel, action)
        }

        if (reSortItems) sort()

        if (visibilityChanged && notifyParent) {
            rearrangeItems()
            notifyParentItemsRectChanged()
            return true
        }
        return false
    }

    fun hasMatchedItems(): Boolean = hasMatchedItems

    protected fun updateNoItemsMessage(filterString: String) {
        if (filterString.isNotEmpty()) {
            val text = noFilteredItemsMsg.replace("[SEARCH_TERM]", filterString)
            setNoItemsCommentText(text)
        } else {
            setNoItemsCommentText(noItemsMsg)
        }
    }

    protected fun updateItemVisibility(item: Panel, action: Map<String, Any?>): Boolean {
        var visible = true

        if (item.notify(action) == 0) {
            hasMatchedItems = true
        } else {
            if (!forceShowingUnmatchedItems) {
                selectItem(item, false)
                visible = false
            }
        }

        if (item.getVisible() != visible) {
            item.setVisible(visible)
            return true
        }
        return false
    }
}
