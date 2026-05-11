package com.firestorm.llui

var MENU_BAR_HEIGHT: Int = 18
var MENU_BAR_WIDTH: Int = 410

private const val LABEL_BOTTOM_PAD_PIXELS: Int = 2
private const val LEFT_PAD_PIXELS: UInt = 3u
private const val LEFT_WIDTH_PIXELS: UInt = 15u
private const val LEFT_PLAIN_PIXELS: UInt = LEFT_PAD_PIXELS + LEFT_WIDTH_PIXELS
private const val RIGHT_PAD_PIXELS: UInt = 7u
private const val RIGHT_WIDTH_PIXELS: UInt = 15u
private const val RIGHT_PLAIN_PIXELS: UInt = RIGHT_PAD_PIXELS + RIGHT_WIDTH_PIXELS
private const val PLAIN_PAD_PIXELS: UInt = LEFT_PAD_PIXELS + LEFT_WIDTH_PIXELS + RIGHT_PAD_PIXELS + RIGHT_WIDTH_PIXELS
private const val BRIEF_PAD_PIXELS: UInt = 2u
private const val SEPARATOR_HEIGHT_PIXELS: UInt = 8u
private const val TEAROFF_SEPARATOR_HEIGHT_PIXELS: Int = 10
private const val MENU_ITEM_PADDING: Int = 4

data class MenuKeyboardBinding(val key: Int, val mask: UInt)

open class MenuItemGL(val name: String) {
    var label: String = name
    var enabled: Boolean = true
    var visible: Boolean = true
    var highlight: Boolean = false

    var acceleratorKey: Int = KEY_NONE
    var acceleratorMask: UInt = MASK_NONE
    var jumpKey: Int = KEY_NONE
    var allowKeyRepeat: Boolean = false

    var drawBoolLabel: String = ""
    var drawAccelLabel: String = ""
    var drawBranchLabel: String = ""

    var enabledColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var disabledColor: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 1f)
    var highlightBackground: FloatArray = floatArrayOf(0f, 0f, 1f, 1f)
    var highlightForeground: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)

    var briefItem: Boolean = false
    var drawTextDisabled: Boolean = false
    var gotHover: Boolean = false

    private var parentMenu: MenuGL? = null

    val commitCallbacks: MutableList<(MenuItemGL) -> Unit> = mutableListOf()
    val visibilityCallbacks: MutableList<(MenuItemGL) -> Boolean> = mutableListOf()

    fun getMenu(): MenuGL? = parentMenu
    fun setParentMenu(menu: MenuGL) { parentMenu = menu }

    open fun setValue(value: Any?) { label = value?.toString() ?: "" }
    open fun getValue(): Any? = label

    fun setLabel(lbl: String) { label = lbl }
    fun getLabel(): String = label

    fun setLabelArg(key: String, text: String): Boolean {
        label = label.replace("[$key]", text)
        return true
    }

    fun setJumpKey(key: Int) { jumpKey = key.uppercaseChar().code }
    fun getJumpKey(): Int = jumpKey

    open fun getNominalHeight(): UInt = (12u + MENU_ITEM_PADDING.toUInt())

    open fun setBriefItem(brief: Boolean) { briefItem = brief }
    open fun isBriefItem(): Boolean = briefItem

    open fun addToAcceleratorList(list: MutableList<MenuKeyboardBinding>): Boolean {
        if (acceleratorKey == KEY_NONE) return true
        val normalMask = acceleratorMask and MASK_NORMALKEYS
        if (list.any { it.key == acceleratorKey && it.mask == normalMask }) return false
        list.add(MenuKeyboardBinding(acceleratorKey, normalMask))
        return true
    }

    fun setAllowKeyRepeat(allow: Boolean) { allowKeyRepeat = allow }
    fun getAllowKeyRepeat(): Boolean = allowKeyRepeat

    open fun hasAccelerator(key: Int, mask: UInt): Boolean =
        acceleratorKey == key && acceleratorMask == mask

    open fun handleAcceleratorKey(key: Int, mask: UInt): Boolean {
        if (enabled && (allowKeyRepeat || !keyIsRepeated(key)) &&
            key == acceleratorKey && mask == (acceleratorMask and MASK_NORMALKEYS)) {
            onCommit()
            return true
        }
        return false
    }

    open fun buildDrawLabel() {
        drawAccelLabel = appendAcceleratorString()
    }

    open fun updateBranchParent(parent: Any?) {}

    open fun onCommit() {
        val menu = getMenu()
        if (menu != null && !menu.getTornOff() && menu.isOpen()) {
            MenuGL.sMenuContainer?.hideMenus()
        }
        commitCallbacks.forEach { it(this) }
    }

    open fun setHighlight(hl: Boolean) {
        if (hl) getMenu()?.clearHoverItem()
        if (highlight != hl) dirtyRect()
        highlight = hl
    }

    open fun getHighlight(): Boolean = highlight

    open fun isActive(): Boolean = false
    open fun isOpen(): Boolean = false

    open fun setEnabledSubMenus(enable: Boolean) {}

    open fun handleKeyHere(key: Int, mask: UInt): Boolean {
        if (getHighlight() && getMenu()?.isOpen() == true) {
            when {
                key == KEY_UP -> {
                    MenuGL.keyboardMode = true
                    getMenu()?.highlightPrevItem(this)
                    return true
                }
                key == KEY_DOWN -> {
                    MenuGL.keyboardMode = true
                    getMenu()?.highlightNextItem(this)
                    return true
                }
                key == KEY_RETURN && mask == MASK_NONE -> {
                    MenuGL.keyboardMode = true
                    onCommit()
                    return true
                }
            }
        }
        return false
    }

    open fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        MenuGL.keyboardMode = false
        setHighlight(true)
        return true
    }

    open fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        MenuGL.keyboardMode = false
        onCommit()
        return true
    }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean =
        !(getMenu()?.isScrollable() ?: false)

    open fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        TODO("GPU: set cursor to arrow")
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: UInt): Boolean = false

    open fun handleRightMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        if (ContextMenuHolder.contextMenuSpawnX != Int.MAX_VALUE ||
            ContextMenuHolder.contextMenuSpawnY != Int.MAX_VALUE) {
            return handleMouseUp(x, y, mask)
        }
        return false
    }

    open fun onMouseEnter(x: Int, y: Int, mask: UInt) { gotHover = true }
    open fun onMouseLeave(x: Int, y: Int, mask: UInt) { gotHover = false }

    open fun onVisibilityChange(newVisibility: Boolean) {
        getMenu()?.needsArrange()
    }

    open fun draw() {
        TODO("GPU: render menu item '${label}'")
    }

    fun getHover(): Boolean = gotHover
    fun setDrawTextDisabled(disabled: Boolean) { drawTextDisabled = disabled }
    fun getDrawTextDisabled(): Boolean = drawTextDisabled

    fun getSearchText(): String = label

    protected fun setHover(hover: Boolean) { gotHover = hover }

    protected fun appendAcceleratorString(): String {
        TODO("APR: use JVM key name lookup for accelerator string")
    }

    protected open fun dirtyRect() {}

    private fun keyIsRepeated(key: Int): Boolean = false

    private fun Char.uppercaseChar(): Char = this.uppercaseChar()

    private fun Int.uppercaseChar(): Char = this.toChar().uppercaseChar()

    fun getNominalWidth(): UInt {
        var width = if (briefItem) BRIEF_PAD_PIXELS else PLAIN_PAD_PIXELS
        if (acceleratorKey != KEY_NONE) {
            width += (getMenu()?.shortcutPad ?: 0).toUInt()
            TODO("GPU: measure accelerator string width")
        }
        TODO("GPU: measure label string width")
    }

    companion object {
        const val KEY_NONE: Int = 0
        const val KEY_UP: Int = 0x26
        const val KEY_DOWN: Int = 0x28
        const val KEY_RETURN: Int = 0x0D
        const val MASK_NONE: UInt = 0u
        const val MASK_CONTROL: UInt = 0x01u
        const val MASK_ALT: UInt = 0x02u
        const val MASK_SHIFT: UInt = 0x04u
        const val MASK_NORMALKEYS: UInt = MASK_CONTROL or MASK_ALT or MASK_SHIFT
    }
}

open class MenuItemSeparatorGL(name: String = "separator") : MenuItemGL(name) {
    val visibilityCallbacksForSelf: MutableList<(MenuItemSeparatorGL) -> Boolean> = mutableListOf()

    override fun getNominalHeight(): UInt = SEPARATOR_HEIGHT_PIXELS

    override fun draw() {
        TODO("GPU: render separator line")
    }

    override fun buildDrawLabel() {
        if (visibilityCallbacksForSelf.isNotEmpty()) {
            visible = visibilityCallbacksForSelf.all { it(this) }
        }
    }

    override fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        val menu = getMenu() ?: return false
        val midY = (getNominalHeight() / 2u).toInt()
        return if (y > midY) {
            val prev = menu.items.firstOrNull { it != this && it.visible && it.enabled }
            prev?.handleMouseDown(x, prev.getNominalHeight().toInt(), mask) ?: false
        } else {
            val next = menu.items.lastOrNull { it != this && it.visible && it.enabled }
            next?.handleMouseDown(x, 0, mask) ?: false
        }
    }

    override fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean = false

    override fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        val menu = getMenu() ?: return false
        if (y > (getNominalHeight() / 2u).toInt()) {
            menu.highlightPrevItem(this, false)
        } else {
            menu.highlightNextItem(this, false)
        }
        return false
    }
}

open class MenuItemCallGL(name: String) : MenuItemGL(name) {
    val enableCallbacks: MutableList<(MenuItemCallGL) -> Boolean> = mutableListOf()
    val visibilityCallbacksGL: MutableList<(MenuItemCallGL) -> Boolean> = mutableListOf()

    fun addClickCallback(cb: (MenuItemCallGL) -> Unit) { commitCallbacks.add { cb(this) } }
    fun addEnableCallback(cb: (MenuItemCallGL) -> Boolean) { enableCallbacks.add(cb) }

    protected fun updateEnabled() {
        if (enableCallbacks.isNotEmpty()) {
            enabled = enableCallbacks.all { it(this) }
        }
    }

    protected fun updateVisible() {
        if (visibilityCallbacksGL.isNotEmpty()) {
            visible = visibilityCallbacksGL.all { it(this) }
        }
    }

    override fun buildDrawLabel() {
        updateEnabled()
        updateVisible()
        super.buildDrawLabel()
    }

    override fun onCommit() {
        getMenu()?.setItemLastSelected(this)
        super.onCommit()
    }

    override fun handleAcceleratorKey(key: Int, mask: UInt): Boolean {
        if ((allowKeyRepeat || !false) &&
            key == acceleratorKey && mask == (acceleratorMask and MASK_NORMALKEYS)) {
            updateEnabled()
            if (enabled) {
                onCommit()
                return true
            }
        }
        return false
    }
}

open class MenuItemCheckGL(name: String) : MenuItemCallGL(name) {
    val checkCallbacks: MutableList<(MenuItemCheckGL) -> Boolean> = mutableListOf()
    var checked: Boolean = false

    fun addCheckCallback(cb: (MenuItemCheckGL) -> Boolean) { checkCallbacks.add(cb) }

    override fun setValue(value: Any?) {
        checked = value as? Boolean ?: value?.toString()?.toBoolean() ?: false
        drawBoolLabel = if (checked) MenuGL.BOOLEAN_TRUE_PREFIX else ""
    }

    override fun getValue(): Any? = checked

    override fun buildDrawLabel() {
        if (checkCallbacks.isNotEmpty()) {
            checked = checkCallbacks.all { it(this) }
        }
        drawBoolLabel = if (checked) MenuGL.BOOLEAN_TRUE_PREFIX else ""
        super.buildDrawLabel()
    }

    override fun onCommit() {
        super.onCommit()
    }
}

open class MenuGL(val name: String) {
    val items: MutableList<MenuItemGL> = mutableListOf()
    var firstVisibleItem: MenuItemGL? = null
    var arrowUpItem: MenuItemGL? = null
    var arrowDownItem: MenuItemGL? = null

    val jumpKeys: MutableMap<Int, MenuItemGL> = mutableMapOf()

    var lastMouseX: Int = 0
    var lastMouseY: Int = 0
    var mouseVelX: Int = 0
    var mouseVelY: Int = 0
    var maxScrollableItems: UInt = UInt.MAX_VALUE
    var preferredWidth: UInt = UInt.MAX_VALUE

    var horizontalLayout: Boolean = false
    var scrollable: Boolean = false
    var keepFixedSize: Boolean = false
    var needsArrangeFlag: Boolean = false

    var label: String = name
    var backgroundColor: FloatArray = floatArrayOf(0.1f, 0.1f, 0.1f, 0.9f)
    var bgVisible: Boolean = true
    var dropShadowed: Boolean = true
    var hasSelection: Boolean = false
    var tornOff: Boolean = false
    var jumpKey: Int = MenuItemGL.KEY_NONE
    var createJumpKeys: Boolean = false
    var shortcutPad: Int = 0
    var resetScrollPositionOnShow: Boolean = true
    var alwaysShowMenu: Boolean = false

    var visible: Boolean = false
    var enabled: Boolean = true

    var tearOffItem: MenuItemTearOffGL? = null
    var spilloverBranch: MenuItemBranchGL? = null
    var spilloverMenu: MenuGL? = null
    var parentMenuItem: MenuItemGL? = null

    open fun addChild(view: MenuItemGL, tabGroup: Int = 0): Boolean = append(view)
    open fun deleteAllChildren() { items.clear() }
    open fun removeChild(ctrl: MenuItemGL) { items.remove(ctrl) }

    open fun postBuild(): Boolean = true

    open fun hasAccelerator(key: Int, mask: UInt): Boolean =
        items.any { it.hasAccelerator(key, mask) }

    open fun handleAcceleratorKey(key: Int, mask: UInt): Boolean =
        items.any { it.handleAcceleratorKey(key, mask) }

    fun findChildMenuByName(name: String, recurse: Boolean): MenuGL? {
        for (item in items) {
            if (item is MenuItemBranchGL && item.name == name) return item.getBranch()
            if (recurse && item is MenuItemBranchGL) {
                item.getBranch()?.findChildMenuByName(name, recurse)?.let { return it }
            }
        }
        return null
    }

    fun clearHoverItem(): Boolean {
        val highlighted = items.firstOrNull { it.getHighlight() }
        highlighted?.setHighlight(false)
        return highlighted != null
    }

    fun getLabel(): String = label
    fun setLabel(lbl: String) { label = lbl }

    fun setBackgroundColor(color: FloatArray) { backgroundColor = color }
    fun getBackgroundColor(): FloatArray = backgroundColor
    fun setBackgroundVisible(b: Boolean) { bgVisible = b }

    fun setCanTearOff(tearOff: Boolean) {
        if (tearOff && tearOffItem == null) {
            tearOffItem = MenuItemTearOffGL("tear_off").also {
                it.setParentMenu(this)
                items.add(0, it)
            }
        } else if (!tearOff && tearOffItem != null) {
            items.remove(tearOffItem)
            tearOffItem = null
        }
    }

    open fun addSeparator(): Boolean {
        val sep = MenuItemSeparatorGL()
        sep.setParentMenu(this)
        items.add(sep)
        needsArrange()
        return true
    }

    open fun updateParent(parent: Any?) {}

    fun setItemEnabled(name: String, enable: Boolean) {
        items.firstOrNull { it.name == name }?.enabled = enable
    }

    fun setEnabledSubMenus(enable: Boolean) {
        items.forEach { it.setEnabledSubMenus(enable) }
    }

    fun setItemVisible(name: String, vis: Boolean) {
        items.firstOrNull { it.name == name }?.visible = vis
        needsArrange()
    }

    fun setItemLabel(name: String, lbl: String) {
        items.firstOrNull { it.name == name }?.setLabel(lbl)
    }

    fun setLeftAndBottom(left: Int, bottom: Int) {
        TODO("GPU: set menu position to ($left, $bottom)")
    }

    open fun handleJumpKey(key: Int): Boolean {
        jumpKeys[key]?.let {
            it.setHighlight(true)
            it.onCommit()
            return true
        }
        return false
    }

    open fun jumpKeysActive(): Boolean = visible

    open fun isOpen(): Boolean = visible

    open fun needsArrange() { needsArrangeFlag = true }

    open fun arrange() { TODO("GPU: arrange menu items") }
    open fun arrangeAndClear() { needsArrangeFlag = false; arrange() }

    fun empty() { items.clear() }

    fun erase(begin: Int, end: Int, doArrange: Boolean = true) {
        if (begin >= 0 && end <= items.size && begin < end) {
            items.subList(begin, end).clear()
            if (doArrange) needsArrange()
        }
    }

    fun insert(begin: Int, ctrl: MenuItemGL, doArrange: Boolean = true) {
        ctrl.setParentMenu(this)
        items.add(begin.coerceIn(0, items.size), ctrl)
        if (doArrange) needsArrange()
    }

    fun setItemLastSelected(item: MenuItemGL) {
        item.buildDrawLabel()
    }

    fun getItemCount(): UInt = items.size.toUInt()

    fun getItem(number: Int): MenuItemGL? = items.getOrNull(number)
    fun getItem(itemName: String): MenuItemGL? = items.firstOrNull { it.name == itemName }

    fun getHighlightedItem(): MenuItemGL? = items.firstOrNull { it.getHighlight() }

    fun highlightNextItem(curItem: MenuItemGL?, skipDisabled: Boolean = true): MenuItemGL? {
        if (items.isEmpty()) return null
        val startIdx = if (curItem == null) -1 else items.indexOf(curItem)
        var idx = startIdx
        repeat(items.size) {
            idx = (idx + 1) % items.size
            val item = items[idx]
            if (item.visible && (!skipDisabled || item.enabled) && item !is MenuItemSeparatorGL) {
                item.setHighlight(true)
                return item
            }
        }
        return null
    }

    fun highlightPrevItem(curItem: MenuItemGL?, skipDisabled: Boolean = true): MenuItemGL? {
        if (items.isEmpty()) return null
        val startIdx = if (curItem == null) items.size else items.indexOf(curItem)
        var idx = startIdx
        repeat(items.size) {
            idx = (idx - 1 + items.size) % items.size
            val item = items[idx]
            if (item.visible && (!skipDisabled || item.enabled) && item !is MenuItemSeparatorGL) {
                item.setHighlight(true)
                return item
            }
        }
        return null
    }

    fun buildDrawLabels() { items.forEach { it.buildDrawLabel() } }

    fun createJumpKeys() {
        if (!createJumpKeys) return
        TODO("APR: use JVM string ops for jump key creation")
    }

    open fun handleUnicodeCharHere(c: Char): Boolean =
        items.any { it.visible && it.enabled && it.handleAcceleratorKey(c.code, 0u) }

    open fun handleHover(x: Int, y: Int, mask: UInt): Boolean = true

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (!scrollable) return false
        return if (clicks < 0) scrollItems(ScrollingDirection.UP) else scrollItems(ScrollingDirection.DOWN)
    }

    open fun draw() { TODO("GPU: render menu '${name}'") }
    open fun drawBackground(item: MenuItemGL, alpha: Float) { TODO("GPU: draw menu background") }

    open fun setVisible(vis: Boolean) { visible = vis }

    fun setDropShadowed(shadowed: Boolean) { dropShadowed = shadowed }
    fun setParentMenuItem(item: MenuItemGL) { parentMenuItem = item }
    fun getParentMenuItem(): MenuItemGL? = parentMenuItem
    fun setTornOff(tornOff: Boolean) { this.tornOff = tornOff }
    fun getTornOff(): Boolean = tornOff
    fun getCanTearOff(): Boolean = tearOffItem != null
    fun getJumpKey(): Int = jumpKey
    fun setJumpKey(key: Int) { jumpKey = key }
    fun getShortcutPad(): Int = shortcutPad
    fun scrollItems(direction: ScrollingDirection): Boolean { TODO("GPU: scroll menu items") }
    fun isScrollable(): Boolean = scrollable
    fun resetScrollPositionOnShow(reset: Boolean) { resetScrollPositionOnShow = reset }
    fun isScrollPositionOnShowReset(): Boolean = resetScrollPositionOnShow
    fun setAlwaysShowMenu(show: Boolean) { alwaysShowMenu = show }
    fun getAlwaysShowMenu(): Boolean = alwaysShowMenu

    fun appendContextSubMenu(menu: MenuGL): Boolean {
        val ctx = menu as? ContextMenu ?: return false
        val branch = ContextMenuBranch(menu.name, ctx)
        return addContextChild(branch)
    }

    protected open fun append(item: MenuItemGL): Boolean {
        item.setParentMenu(this)
        items.add(item)
        needsArrange()
        return true
    }

    protected open fun appendMenu(menu: MenuGL): Boolean {
        val branch = MenuItemBranchGL(menu.name, menu)
        return append(branch)
    }

    protected fun addContextChild(view: MenuItemGL): Boolean = append(view)

    enum class ScrollingDirection { UP, DOWN, BEGIN, END }

    companion object {
        val BOOLEAN_TRUE_PREFIX: String = "✔"
        val BRANCH_SUFFIX: String = "▶"
        val ARROW_UP: String = "^^^^^^^"
        val ARROW_DOWN: String = "vvvvvvv"

        var keyboardMode: Boolean = false
        var sMenuContainer: MenuHolderGL? = null

        fun setKeyboardMode(mode: Boolean) { keyboardMode = mode }
        fun getKeyboardMode(): Boolean = keyboardMode

        fun showPopup(spawningView: Any?, menu: MenuGL, x: Int, y: Int, mouseX: Int = 0, mouseY: Int = 0) {
            TODO("GPU: show menu popup at ($x, $y)")
        }
    }
}

open class MenuItemBranchGL(name: String, private var branch: MenuGL?) : MenuItemGL(name) {
    fun getBranch(): MenuGL? = branch

    override fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        MenuGL.keyboardMode = false
        onCommit()
        return true
    }

    override fun hasAccelerator(key: Int, mask: UInt): Boolean =
        getBranch()?.hasAccelerator(key, mask) ?: false

    override fun handleAcceleratorKey(key: Int, mask: UInt): Boolean =
        getBranch()?.handleAcceleratorKey(key, mask) ?: false

    override fun addToAcceleratorList(list: MutableList<MenuKeyboardBinding>): Boolean {
        val b = getBranch() ?: return false
        var result = false
        var count = b.getItemCount().toInt()
        while (count-- > 0) {
            b.getItem(count)?.let { result = it.addToAcceleratorList(list) }
        }
        return result
    }

    override fun buildDrawLabel() {
        drawAccelLabel = ""
        drawBranchLabel = MenuGL.BRANCH_SUFFIX
    }

    override fun onCommit() {
        openMenu()
        if (MenuGL.keyboardMode && getBranch() != null && getBranch()?.getHighlightedItem() == null) {
            getBranch()?.highlightNextItem(null)
        }
    }

    override fun setHighlight(hl: Boolean) {
        if (hl == getHighlight()) return
        val b = getBranch() ?: return
        val autoOpen = enabled && (!b.visible || b.getTornOff())
        super.setHighlight(hl)
        if (hl) {
            if (autoOpen) openMenu()
        } else {
            if (!b.getTornOff()) {
                b.setVisible(false)
            } else {
                b.clearHoverItem()
            }
        }
    }

    override fun draw() {
        TODO("GPU: render branch menu item '${label}'")
    }

    override fun isActive(): Boolean = isOpen() && getBranch()?.getHighlightedItem() != null
    override fun isOpen(): Boolean = getBranch()?.isOpen() ?: false

    override fun updateBranchParent(parent: Any?) {
        getBranch()?.updateParent(parent)
    }

    override fun onVisibilityChange(newVisibility: Boolean) {
        if (!newVisibility && getBranch()?.getTornOff() == false) {
            getBranch()?.setVisible(false)
        }
        super.onVisibilityChange(newVisibility)
    }

    override fun setEnabledSubMenus(enabled: Boolean) { getBranch()?.setEnabledSubMenus(enabled) }

    override fun handleKeyHere(key: Int, mask: UInt): Boolean {
        val b = getBranch() ?: return super.handleKeyHere(key, mask)
        if (getHighlight() && getMenu()?.isOpen() == true && (isActive() || MenuGL.keyboardMode)) {
            if (b.visible && key == KEY_LEFT) {
                MenuGL.keyboardMode = true
                val handled = b.clearHoverItem()
                if (handled && getMenu()?.getTornOff() == true) {
                }
                return handled
            }
            if (key == KEY_RIGHT && b.getHighlightedItem() == null) {
                MenuGL.keyboardMode = true
                if (b.highlightNextItem(null) != null) return true
            }
        }
        return super.handleKeyHere(key, mask)
    }

    open fun openMenu() {
        TODO("GPU: open branch menu for '${name}'")
    }

    companion object {
        private const val KEY_LEFT: Int = 0x25
        private const val KEY_RIGHT: Int = 0x27
    }
}

open class ContextMenu(name: String) : MenuGL(name) {
    var hoveredAnyItem: Boolean = false
    var hoverItem: MenuItemGL? = null
    var spawningView: Any? = null

    override fun setVisible(vis: Boolean) { visible = vis }

    open fun show(x: Int, y: Int, spawningView: Any? = null) {
        this.spawningView = spawningView
        visible = true
        TODO("GPU: show context menu at ($x, $y)")
    }

    open fun hide() { visible = false }

    override fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        hoveredAnyItem = items.any { it.visible && it.enabled }
        return true
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: UInt): Boolean = true
    open fun handleRightMouseUp(x: Int, y: Int, mask: UInt): Boolean = true

    override fun addChild(view: MenuItemGL, tabGroup: Int): Boolean = addContextChild(view)
    override fun deleteAllChildren() { items.clear() }
    override fun removeChild(ctrl: MenuItemGL) { items.remove(ctrl) }
}

open class ContextMenuBranch(name: String, private val branchMenu: ContextMenu) : MenuItemGL(name) {
    fun getBranch(): ContextMenu = branchMenu

    override fun buildDrawLabel() {
        drawBranchLabel = MenuGL.BRANCH_SUFFIX
        super.buildDrawLabel()
    }

    override fun onCommit() { showSubMenu() }

    override fun setHighlight(hl: Boolean) {
        super.setHighlight(hl)
        if (hl) showSubMenu() else branchMenu.hide()
    }

    private fun showSubMenu() {
        TODO("GPU: show context sub-menu '${branchMenu.name}'")
    }
}

open class MenuBarGL(name: String) : MenuGL(name) {
    private val accelerators: MutableList<MenuKeyboardBinding> = mutableListOf()
    private var altKeyTrigger: Boolean = false

    override fun handleAcceleratorKey(key: Int, mask: UInt): Boolean {
        if (key == KEY_F10 && mask == MenuItemGL.MASK_NONE) {
            keyboardMode = true
            items.firstOrNull { it.visible && it.enabled }?.setHighlight(true)
            return true
        }
        return items.any { it.handleAcceleratorKey(key, mask) }
    }

    override fun handleJumpKey(key: Int): Boolean {
        if (!jumpKeysActive()) return false
        return super.handleJumpKey(key)
    }

    override fun jumpKeysActive(): Boolean = visible && keyboardMode

    override fun addSeparator(): Boolean {
        val sep = MenuItemSeparatorGL("vseparator")
        sep.setParentMenu(this)
        items.add(sep)
        return true
    }

    override fun handleHover(x: Int, y: Int, mask: UInt): Boolean = true

    fun getRightmostMenuEdge(): Int {
        TODO("GPU: get rightmost x edge of menu bar children")
    }

    fun resetMenuTrigger() { altKeyTrigger = false }

    override fun arrange() { TODO("GPU: arrange menu bar items horizontally") }

    override fun appendMenu(menu: MenuGL): Boolean = super.appendMenu(menu)

    private fun checkMenuTrigger() {}

    companion object {
        private const val KEY_F10: Int = 0x79
    }
}

open class MenuHolderGL(name: String) {
    var canHide: Boolean = true
    var visible: Boolean = true

    open fun hideMenus(): Boolean {
        TODO("GPU: hide all menus in holder")
    }

    fun reshape(width: Int, height: Int, fromParent: Boolean = true) {}
    fun setCanHide(hide: Boolean) { canHide = hide }

    open fun draw() { TODO("GPU: draw menu holder") }

    open fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean = false
    open fun handleRightMouseDown(x: Int, y: Int, mask: UInt): Boolean = false
    open fun handleRightMouseUp(x: Int, y: Int, mask: UInt): Boolean = false
    open fun handleKey(key: Int, mask: UInt, fromParent: Boolean): Boolean = false

    fun getVisibleMenu(): MenuGL? = null
    open fun hasVisibleMenu(): Boolean = getVisibleMenu() != null

    companion object {
        var contextMenuSpawnX: Int = Int.MAX_VALUE
        var contextMenuSpawnY: Int = Int.MAX_VALUE

        fun setActivatedItem(item: MenuItemGL) {
            TODO("APR: use JVM equivalent for item activation timer")
        }
    }
}

object ContextMenuHolder {
    var contextMenuSpawnX: Int = Int.MAX_VALUE
    var contextMenuSpawnY: Int = Int.MAX_VALUE
}

class TearOffMenu private constructor(private val menu: MenuGL) {
    private var targetHeight: Int = 0
    private var quitRequested: Boolean = false

    fun draw() { TODO("GPU: draw tear-off menu floater") }
    fun onFocusReceived() {}
    fun onFocusLost() {}
    fun handleUnicodeChar(c: Char, fromParent: Boolean): Boolean = false
    fun handleKeyHere(key: Int, mask: UInt): Boolean = false
    fun translate(x: Int, y: Int) { TODO("GPU: translate tear-off menu") }
    fun updateSize() { TODO("GPU: update tear-off menu size") }

    private fun closeTearOff() {}

    companion object {
        fun create(menu: MenuGL): TearOffMenu = TearOffMenu(menu)
    }
}

open class MenuItemTearOffGL(name: String) : MenuItemGL(name) {
    override fun onCommit() {
        val menu = getMenu() ?: return
        if (menu.getTornOff()) {
            TODO("APR: close tear-off floater parent")
        } else {
            if (getHighlight()) menu.highlightNextItem(this)
            menu.needsArrange()
            TearOffMenu.create(menu)
        }
        super.onCommit()
    }

    override fun draw() { TODO("GPU: render tear-off separator lines") }
    override fun getNominalHeight(): UInt = TEAROFF_SEPARATOR_HEIGHT_PIXELS.toUInt()
}

abstract class ViewListener {
    abstract fun handleEvent(userdata: Any?): Boolean

    init { registry.add(this) }

    companion object {
        private val registry: MutableSet<ViewListener> = mutableSetOf()

        fun addEnable(listener: ViewListener, name: String) {
            TODO("APR: register enable callback '$name'")
        }

        fun addCommit(listener: ViewListener, name: String) {
            TODO("APR: register commit callback '$name'")
        }

        fun addMenu(listener: ViewListener, name: String) {
            addEnable(listener, name)
            addCommit(listener, name)
        }

        fun cleanup() { registry.clear() }
    }
}
