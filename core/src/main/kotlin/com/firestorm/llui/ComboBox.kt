package com.firestorm.llui

import kotlin.math.max
import kotlin.math.min

val MAX_COMBO_WIDTH: Int = 500

enum class PreferredPosition { ABOVE, BELOW }

enum class AddPosition { ADD_TOP, ADD_BOTTOM, ADD_DEFAULT }

open class ScrollListItem(val value: Any?, val label: String = "") {
    var enabled: Boolean = true
    var userdata: Any? = null

    fun getValue(): Any? = value
    fun getEnabled(): Boolean = enabled
    fun setEnabled(e: Boolean) { enabled = e }
    fun getUserdata(): Any? = userdata
    fun setUserdata(d: Any?) { userdata = d }
    fun getColumn(index: Int): ScrollListCell? = if (index == 0) ScrollListCell(label) else null
}

class ScrollListCell(val rawValue: Any?) {
    fun getValue(): Any? = rawValue
}

open class ScrollListCtrl {
    private val items: MutableList<ScrollListItem> = mutableListOf()
    var visible: Boolean = false

    fun addSimpleElement(name: String, pos: AddPosition = AddPosition.ADD_BOTTOM, id: Any? = null): ScrollListItem {
        val item = ScrollListItem(id ?: name, name)
        when (pos) {
            AddPosition.ADD_TOP -> items.add(0, item)
            else -> items.add(item)
        }
        return item
    }

    fun addElement(value: Any?, pos: AddPosition = AddPosition.ADD_BOTTOM, userdata: Any? = null): ScrollListItem {
        val item = ScrollListItem(value)
        item.userdata = userdata
        when (pos) {
            AddPosition.ADD_TOP -> items.add(0, item)
            else -> items.add(item)
        }
        return item
    }

    fun addSeparator(pos: AddPosition = AddPosition.ADD_BOTTOM): ScrollListItem {
        val sep = ScrollListItem(null, "---")
        sep.enabled = false
        when (pos) {
            AddPosition.ADD_TOP -> items.add(0, sep)
            else -> items.add(sep)
        }
        return sep
    }

    fun getFirstSelected(): ScrollListItem? = items.firstOrNull { it.enabled && it == selectedItem }
    fun getLastSelectedItem(): ScrollListItem? = selectedItem
    fun getFirstSelectedIndex(): Int = selectedItem?.let { items.indexOf(it) } ?: -1
    fun getItemCount(): Int = items.size
    fun isEmpty(): Boolean = items.isEmpty()
    fun getCanSelect(): Boolean = true

    private var selectedItem: ScrollListItem? = null

    fun selectItem(item: ScrollListItem?, column: Int = -1, select: Boolean = true) {
        selectedItem = if (select) item else null
    }

    fun selectNthItem(index: Int): Boolean {
        val item = items.getOrNull(index) ?: return false
        selectedItem = item
        return true
    }

    fun selectItemByLabel(label: String, caseSensitive: Boolean = true): Boolean {
        val found = items.firstOrNull {
            if (caseSensitive) it.label == label else it.label.equals(label, ignoreCase = true)
        }
        selectedItem = found
        return found != null
    }

    fun selectItemByPrefix(prefix: String, caseSensitive: Boolean = true): Boolean {
        val found = items.firstOrNull {
            if (caseSensitive) it.label.startsWith(prefix) else it.label.startsWith(prefix, ignoreCase = true)
        }
        selectedItem = found
        return found != null
    }

    fun selectItemBySubstring(substring: String, caseSensitive: Boolean = true): Boolean {
        val found = items.firstOrNull {
            if (caseSensitive) it.label.contains(substring) else it.label.contains(substring, ignoreCase = true)
        }
        selectedItem = found
        return found != null
    }

    fun selectByValue(value: Any?): Boolean {
        val found = items.firstOrNull { it.value?.toString() == value?.toString() }
        selectedItem = found
        return found != null
    }

    fun selectByID(id: String): Boolean = selectByValue(id)

    fun deselectAllItems() { selectedItem = null }

    fun getItemByIndex(index: Int): ScrollListItem? = items.getOrNull(index)
    fun getItem(value: Any?): ScrollListItem? = items.firstOrNull { it.value?.toString() == value?.toString() }
    fun getItemByLabel(label: String): ScrollListItem? = items.firstOrNull { it.label == label }
    fun getItemIndex(item: ScrollListItem): Int = items.indexOf(item)

    fun getSelectedItemLabel(column: Int = 0): String = selectedItem?.label ?: ""
    fun getSelectedValue(): Any? = selectedItem?.value
    fun getStringUUIDSelectedItem(): String = selectedItem?.value?.toString() ?: ""

    fun getAllData(): List<ScrollListItem> = items.toList()

    fun setSelectedByValue(value: Any?, selected: Boolean): Boolean {
        val item = getItem(value) ?: return false
        selectedItem = if (selected) item else null
        return true
    }

    fun isSelected(value: Any?): Boolean = selectedItem?.value?.toString() == value?.toString()

    fun deleteSingleItem(index: Int) {
        if (index >= 0 && index < items.size) {
            if (items[index] == selectedItem) selectedItem = null
            items.removeAt(index)
        }
    }

    fun deleteSelectedItems() {
        items.remove(selectedItem)
        selectedItem = null
    }

    fun clearRows() {
        items.clear()
        selectedItem = null
    }

    fun clearColumns() {}
    fun addColumn(column: Any?, pos: AddPosition = AddPosition.ADD_BOTTOM) {}
    fun setColumnLabel(column: String, label: String) {}
    fun sortOnce(column: Int, ascending: Boolean) {
        val sorted = if (ascending) items.sortedBy { it.label } else items.sortedByDescending { it.label }
        items.clear()
        items.addAll(sorted)
    }
    fun sortByColumn(name: String, ascending: Boolean) = sortOnce(0, ascending)

    fun selectItemRange(first: Int, last: Int): Boolean {
        val item = items.getOrNull(first) ?: return false
        selectedItem = item
        return true
    }

    fun isDirty(): Boolean = false
    fun resetDirty() {}
    fun clearSearchString() {}
    fun mouseOverHighlightNthItem(index: Int) {}
    fun calcMaxContentWidth(): Int = 0
    fun fitContents(minWidth: Int, maxHeight: Int) { System.err.println("ScrollListCtrl: fitContents not yet implemented") }
    fun handleKeyHere(key: Int, mask: UInt): Boolean = false
    fun handleUnicodeCharHere(c: Char): Boolean = false
    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean = false
    fun setFocus(b: Boolean) {}
    fun setVisible(v: Boolean) { visible = v }
    fun getVisible(): Boolean = visible
    fun setMouseUpCallback(cb: () -> Unit) {}
    fun setOrigin(x: Int, y: Int) {}
    fun reshape(w: Int, h: Int) {}
    fun translateIntoRect(rect: IntArray) {}
    fun translate(dx: Int, dy: Int) {}
    fun localPointToScreen(lx: Int, ly: Int, sx: IntArray, sy: IntArray) {}
}

open class ComboBox(name: String) {
    enum class EOperation { OP_DELETE, OP_SELECT, OP_DESELECT }

    protected val button: Button = Button(name)
    protected var textEntry: LineEditor? = null
    protected val list: ScrollListCtrl = ScrollListCtrl()
    protected var listPosition: PreferredPosition = PreferredPosition.BELOW
    protected var arrowImage: UIImage? = null
    protected var label: String = ""
    protected var hasAutocompletedText: Boolean = false

    private var allowTextEntry: Boolean = false
    private var allowNewValues: Boolean = false
    private var maxChars: Int = 20
    private var textEntryTentative: Boolean = true
    private var forceDisableFulltextSearch: Boolean = false
    private var lastSelectedIndex: Int = -1

    var prearrangeCallback: ((ComboBox, String) -> Unit)? = null
    var textEntryCallback: ((LineEditor) -> Unit)? = null
    var textChangedCallback: ((LineEditor) -> Unit)? = null
    val onReturnCallbacks: MutableList<(ComboBox, Any?) -> Unit> = mutableListOf()

    var enabled: Boolean = true
    var visible: Boolean = true

    fun setAllowTextEntry(b: Boolean) { allowTextEntry = b }
    fun getAllowTextEntry(): Boolean = allowTextEntry

    fun postBuild(): Boolean = true

    open fun onFocusLost() {
        hideList()
        if (allowTextEntry && getCurrentIndex() != -1) {
            textEntry?.selectAll()
        }
        button.setForcePressedState(false)
    }

    open fun clear() {
        textEntry?.setText("")
        button.setLabelSelected("")
        button.setLabelUnselected("")
        list.deselectAllItems()
        lastSelectedIndex = -1
    }

    open fun onCommit() {
        val item = list.getFirstSelected()
        if (item != null) {
            if (allowTextEntry) {
                textEntry?.setText(item.label)
            }
        } else if (allowTextEntry) {
            // value comes from text entry
        }
    }

    open fun acceptsTextInput(): Boolean = allowTextEntry

    open fun isDirty(): Boolean = list.isDirty()
    open fun resetDirty() = list.resetDirty()

    open fun setFocus(b: Boolean) {
        if (b) {
            list.clearSearchString()
            if (list.getVisible()) list.setFocus(true)
        }
    }

    open fun setValue(value: Any?) {
        val alreadySelected = list.getFirstSelected()
        if (alreadySelected?.value?.toString() == value?.toString()) return

        if (list.selectByValue(value)) {
            updateLabel()
            lastSelectedIndex = list.getFirstSelectedIndex()
        } else {
            lastSelectedIndex = -1
        }
    }

    open fun getValue(): Any? {
        list.getFirstSelected()?.let { return it.value }
        if (allowTextEntry) return textEntry?.getText()
        return null
    }

    fun setTextEntry(text: String) {
        textEntry?.setText(text)
        hasAutocompletedText = false
        updateSelection()
    }

    fun setKeystrokeOnEsc(enable: Boolean) {
        textEntry?.setKeystrokeOnEsc(enable)
    }

    fun add(name: String, pos: AddPosition = AddPosition.ADD_BOTTOM, enabled: Boolean = true): ScrollListItem {
        val item = list.addSimpleElement(name, pos)
        item.setEnabled(enabled)
        if (!allowTextEntry && label.isEmpty()) selectFirstItem()
        return item
    }

    fun add(name: String, id: String, pos: AddPosition = AddPosition.ADD_BOTTOM, enabled: Boolean = true): ScrollListItem {
        val item = list.addSimpleElement(name, pos, id)
        item.setEnabled(enabled)
        if (!allowTextEntry && label.isEmpty()) selectFirstItem()
        return item
    }

    fun add(name: String, value: Any?, pos: AddPosition = AddPosition.ADD_BOTTOM, enabled: Boolean = true): ScrollListItem {
        val item = list.addSimpleElement(name, pos, value)
        item.setEnabled(enabled)
        if (!allowTextEntry && label.isEmpty()) selectFirstItem()
        return item
    }

    fun addSeparator(pos: AddPosition = AddPosition.ADD_BOTTOM): ScrollListItem = list.addSeparator(pos)

    fun remove(index: Int): Boolean {
        if (index < list.getItemCount()) {
            list.deleteSingleItem(index)
            setLabel(getSelectedItemLabel())
            return true
        }
        return false
    }

    fun remove(name: String): Boolean {
        if (list.selectItemByLabel(name)) {
            val item = list.getFirstSelected()
            if (item != null) {
                list.deleteSingleItem(list.getItemIndex(item))
            }
            lastSelectedIndex = list.getFirstSelectedIndex()
            return true
        }
        return false
    }

    fun removeall() = clearRows()

    fun itemExists(name: String): Boolean = list.getItemByLabel(name) != null

    fun getItemByValue(value: Any?): ScrollListItem? = list.getItem(value)

    fun sortByName(ascending: Boolean = true) = list.sortOnce(0, ascending)

    fun setSimple(name: String): Boolean {
        val found = list.selectItemByLabel(name, false)
        if (found) {
            setLabel(name)
            lastSelectedIndex = list.getFirstSelectedIndex()
        }
        return found
    }

    fun getSimple(): String {
        val res = getSelectedItemLabel()
        if (res.isEmpty() && allowTextEntry) return textEntry?.getText() ?: ""
        return res
    }

    open fun getSelectedItemLabel(column: Int = 0): String = list.getSelectedItemLabel(column)

    fun setLabel(name: String) {
        if (textEntry != null) {
            textEntry!!.setText(name)
            if (list.selectItemByLabel(name, false)) {
                textEntry!!.setTentative(false)
                lastSelectedIndex = list.getFirstSelectedIndex()
            } else {
                textEntry!!.setTentative(textEntryTentative)
            }
        }
        if (!allowTextEntry) {
            button.setLabel(name)
        }
    }

    fun updateLabel() {
        if (textEntry != null) {
            textEntry!!.setText(getSelectedItemLabel())
            textEntry!!.setTentative(false)
        }
        if (!allowTextEntry) {
            button.setLabel(getSelectedItemLabel())
        }
    }

    fun setCurrentByIndex(index: Int): Boolean {
        val item = list.getItemByIndex(index) ?: return false
        if (!item.getEnabled()) return false
        list.selectItem(item, -1, true)
        val lbl = getSelectedItemLabel()
        textEntry?.setText(lbl)
        textEntry?.setTentative(false)
        if (!allowTextEntry) button.setLabel(lbl)
        lastSelectedIndex = index
        return true
    }

    fun getCurrentIndex(): Int = list.getFirstSelectedIndex()

    fun selectNextItem(): Boolean {
        val last = list.getItemCount() - 1
        if (last < 0) return false
        val current = getCurrentIndex()
        if (current >= last) return false
        var newIndex = max(current, -1)
        while (++newIndex <= last) {
            if (setCurrentByIndex(newIndex)) return true
        }
        return false
    }

    fun selectPrevItem(): Boolean {
        val last = list.getItemCount() - 1
        if (last < 0) return false
        val current = getCurrentIndex()
        if (current == 0) return false
        var newIndex = if (current > 0) current else last + 1
        while (--newIndex >= 0) {
            if (setCurrentByIndex(newIndex)) return true
        }
        return false
    }

    fun setEnabledByValue(value: Any?, enabled: Boolean) {
        list.getItem(value)?.setEnabled(enabled)
    }

    fun createLineEditor() {
        if (!allowTextEntry) {
            if (textEntry != null) textEntry!!.setVisible(false)
        }
    }

    fun focusEditor() { textEntry?.setFocus(true) }

    fun getItemCount(): Int = list.getItemCount()
    fun addColumn(column: Any?, pos: AddPosition = AddPosition.ADD_BOTTOM) = list.addColumn(column, pos)
    fun clearColumns() = list.clearColumns()
    fun setColumnLabel(column: String, lbl: String) = list.setColumnLabel(column, lbl)
    fun addElement(value: Any?, pos: AddPosition = AddPosition.ADD_BOTTOM, userdata: Any? = null): ScrollListItem = list.addElement(value, pos, userdata)
    fun addSimpleElement(value: String, pos: AddPosition = AddPosition.ADD_BOTTOM, id: Any? = null): ScrollListItem = list.addSimpleElement(value, pos, id)
    fun clearRows() = list.clearRows()
    fun sortByColumn(name: String, ascending: Boolean) = list.sortByColumn(name, ascending)

    fun getCanSelect(): Boolean = true
    fun selectFirstItem(): Boolean = setCurrentByIndex(0)
    fun selectNthItem(index: Int): Boolean = setCurrentByIndex(index)
    fun selectItemRange(first: Int, last: Int): Boolean = list.selectItemRange(first, last)
    fun getFirstSelectedIndex(): Int = getCurrentIndex()

    fun setCurrentByID(id: String): Boolean {
        val found = list.selectByID(id)
        if (found) {
            setLabel(getSelectedItemLabel())
            lastSelectedIndex = list.getFirstSelectedIndex()
        }
        return found
    }

    fun getCurrentID(): String = list.getStringUUIDSelectedItem()

    fun setSelectedByValue(value: Any?, selected: Boolean): Boolean {
        val found = list.setSelectedByValue(value, selected)
        if (found) setLabel(getSelectedItemLabel())
        return found
    }

    fun getSelectedValue(): Any? = list.getSelectedValue()
    fun isSelected(value: Any?): Boolean = list.isSelected(value)

    fun operateOnSelection(op: EOperation): Boolean {
        if (op == EOperation.OP_DELETE) {
            list.deleteSelectedItems()
            return true
        }
        return false
    }

    fun operateOnAll(op: EOperation): Boolean {
        if (op == EOperation.OP_DELETE) {
            clearRows()
            return true
        }
        return false
    }

    fun setLeftTextPadding(pad: Int) {
        textEntry?.let {
            val (left, right) = it.getTextPadding()
            it.setTextPadding(pad, right)
        }
    }

    fun getCurrentUserdata(): Any? = list.getFirstSelected()?.getUserdata()

    fun setPrearrangeCallback(cb: (ComboBox, String) -> Unit) { prearrangeCallback = cb }
    fun setTextEntryCallback(cb: (LineEditor) -> Unit) { textEntryCallback = cb }
    fun setTextChangedCallback(cb: (LineEditor) -> Unit) { textChangedCallback = cb }
    fun addReturnCallback(cb: (ComboBox, Any?) -> Unit) { onReturnCallbacks.add(cb) }

    fun setButtonVisible(visible: Boolean) {
        button.visible = visible
    }

    fun onButtonMouseDown() {
        if (!list.getVisible()) {
            prearrangeList()
            if (list.getItemCount() != 0) showList()
            setFocus(true)
        } else {
            hideList()
        }
    }

    fun onListMouseUp() {
        button.setForcePressedState(false)
    }

    fun onItemSelected(data: Any?) {
        lastSelectedIndex = getCurrentIndex()
        if (lastSelectedIndex != -1) {
            updateLabel()
            if (allowTextEntry) textEntry?.selectAll()
        }
        hideList()
        onCommit()
    }

    fun onTextCommit(data: Any?) {
        val text = textEntry?.getText() ?: return
        setSimple(text)
        onCommit()
        textEntry?.selectAll()
    }

    fun updateSelection() {
        val fullText = textEntry?.getText() ?: return
        val leftStr = textEntry?.getText()?.substring(0, textEntry?.getCursor() ?: 0) ?: fullText
        val userStr = if (hasAutocompletedText) leftStr else fullText

        if (fullText.length == 1) prearrangeList(fullText)

        when {
            list.selectItemByLabel(fullText, false) -> {
                textEntry?.setTentative(false)
                lastSelectedIndex = list.getFirstSelectedIndex()
                hasAutocompletedText = false
            }
            list.selectItemByPrefix(leftStr, false) -> {
                val selectedLabel = getSelectedItemLabel()
                val completion = leftStr + selectedLabel.substring(leftStr.length)
                textEntry?.setText(completion)
                textEntry?.setSelection(leftStr.length, completion.length)
                textEntry?.setTentative(false)
                hasAutocompletedText = true
                lastSelectedIndex = list.getFirstSelectedIndex()
            }
            else -> {
                list.deselectAllItems()
                textEntry?.setText(userStr)
                textEntry?.setTentative(textEntryTentative)
                hasAutocompletedText = false
                lastSelectedIndex = -1
            }
        }
    }

    open fun showList() {
        list.setFocus(true)
        button.setToggleState(true)
        list.setVisible(true)
        // no-op
    }

    open fun hideList() {
        if (list.getVisible()) {
            if (lastSelectedIndex >= 0) list.selectNthItem(lastSelectedIndex)
            button.setToggleState(false)
            list.setVisible(false)
            list.mouseOverHighlightNthItem(-1)
        }
    }

    open fun onTextEntry(lineEditor: LineEditor) {
        textEntryCallback?.invoke(lineEditor)
        System.err.println("ComboBox: onTextEntry not yet implemented")
    }

    fun prearrangeList(filter: String = "") {
        prearrangeCallback?.invoke(this, filter)
    }

    open fun handleToolTip(x: Int, y: Int, mask: UInt): Boolean = false

    open fun handleKeyHere(key: Int, mask: UInt): Boolean {
        if (list.getVisible() && key == KEY_ESCAPE && mask == 0u) {
            hideList()
            return true
        }
        val lastSelected = list.getLastSelectedItem()
        val result = list.handleKeyHere(key, mask)
        if (key == KEY_RETURN) {
            if (mask == 0u) onReturnCallbacks.forEach { it(this, getValue()) }
            return false
        }
        return result
    }

    open fun handleUnicodeCharHere(c: Char): Boolean {
        if (c != ' ') {
            val result = list.handleUnicodeCharHere(c)
            return result
        }
        return false
    }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (list.getVisible()) return list.handleScrollWheel(x, y, clicks)
        if (allowTextEntry && list.getFirstSelected() == null) return false
        val currentIndex = getCurrentIndex()
        repeat(kotlin.math.abs(clicks)) {
            if (clicks > 0) selectNextItem() else selectPrevItem()
        }
        if (getCurrentIndex() != currentIndex) {
            prearrangeList()
            onCommit()
            return true
        }
        return false
    }

    fun getSearchText(): String {
        val sb = StringBuilder()
        list.getAllData().forEach { item ->
            item.getColumn(0)?.getValue()?.toString()?.let { sb.append(it) }
        }
        return sb.toString()
    }

    companion object {
        private const val KEY_ESCAPE: Int = 0x1B
        private const val KEY_RETURN: Int = 0x0D
    }
}

fun ScrollListCtrl.getLastSelectedItem(): ScrollListItem? = getFirstSelected()

open class IconsComboBox(name: String) : ComboBox(name) {
    private var iconColumnIndex: Int = 0
    private var labelColumnIndex: Int = 1

    override fun getSelectedItemLabel(column: Int): String {
        val iconLabel = list.getSelectedItemLabel(iconColumnIndex)
        button.setImageOverlay(iconLabel, button.getImageOverlayHAlign())
        return list.getSelectedItemLabel(labelColumnIndex)
    }
}
