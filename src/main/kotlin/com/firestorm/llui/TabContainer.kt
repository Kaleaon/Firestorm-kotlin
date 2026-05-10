package com.firestorm.llui

private const val SCROLL_STEP_TIME = 0.4f
private const val SCROLL_DELAY_TIME = 0.5f

enum class TabPosition { TOP, BOTTOM, LEFT }

enum class InsertionPoint(val code: Int) {
    START(-1),
    END(-2),
    LEFT_OF_CURRENT(-3),
    RIGHT_OF_CURRENT(-4)
}

class TabTuple(
    val tabContainer: TabContainer,
    val tabPanel: Panel,
    val button: Button,
    val placeholderText: TextBox? = null
) {
    var oldState: Boolean = false
    var padding: Int = 0
    var visible: Boolean = true
}

open class Panel(val name: String = "", var label: String = "") {
    var visible: Boolean = true
    fun setVisible(v: Boolean) { visible = v }
    fun getVisible(): Boolean = visible
    fun setFocus(f: Boolean) {}
    fun focusFirstItem(b: Boolean): Boolean = false
}

open class Button(val name: String = "") {
    var visible: Boolean = true
    var enabled: Boolean = true
    var flashing: Boolean = false
    private var rect: Rect = Rect()

    fun getRect(): Rect = rect
    fun setRect(r: Rect) { rect = r }
    fun setVisible(v: Boolean) { visible = v }
    fun getFlashing(): Boolean = flashing
    fun setFlashing(f: Boolean) { flashing = f }
    fun translate(dx: Int, dy: Int) { rect = rect.copy(left = rect.left + dx, right = rect.right + dx, bottom = rect.bottom + dy, top = rect.top + dy) }
    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    fun handleHover(x: Int, y: Int, mask: Int): Boolean = false
    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    fun handleToolTip(x: Int, y: Int, mask: Int): Boolean = false
    fun pointInView(x: Int, y: Int): Boolean = rect.pointInRect(x, y)
    fun onCommit() {}
    fun getEnabled(): Boolean = enabled
}

open class TextBox(val name: String = "") {
    var text: String = ""
    fun getText(): String = text
    fun setText(t: String) { text = t }
    fun getLabel(): String = text
}

open class IconCtrl

open class TabContainer(
    val tabPosition: TabPosition = TabPosition.TOP,
    tabMinWidth: Int = 0,
    tabMaxWidth: Int = 200,
    val tabHeight: Int = 25,
    val labelPadBottom: Int = 0,
    val labelPadLeft: Int = 0,
    val hideScrollArrows: Boolean = false,
    val allowRearrange: Boolean = false,
    val tabPaddingRight: Int = 0,
    val customIconCtrlUsed: Boolean = false,
    val openTabsOnDragAndDrop: Boolean = false,
    val enableTabsFlashing: Boolean = false,
    val tabsFlashingColor: FloatArray = floatArrayOf(1f, 0.5f, 0f, 1f),
    val tabIconCtrlPad: Int = 0,
    val useTabEllipses: Boolean = false,
    val useTabOffset: Boolean = false,
    val dropShadowedText: Boolean = false
) {
    private val tabList: MutableList<TabTuple> = mutableListOf()
    private var currentTabIdx: Int = -1
    private var tabsHidden: Boolean = false
    private var scrolled: Boolean = false
    private var scrollPos: Int = 0
    private var scrollPosPixels: Int = 0
    private var maxScrollPos: Int = 0
    private var topBorderHeight: Int = 1
    private var lockedTabCount: Int = 0
    var minTabWidth: Int = tabMinWidth
    var maxTabWidth: Int = tabMaxWidth
    private var rightTabBtnOffset: Int = tabPaddingRight
    private var totalTabWidth: Int = 0
    private var isVertical: Boolean = tabPosition == TabPosition.LEFT
    private var rect: Rect = Rect()
    private var dragAndDropDelayTimerStarted: Boolean = false
    private var dragAndDropElapsed: Float = 0f
    private var mouseDownElapsed: Float = 0f

    var prevArrowBtn: Button? = null
    var nextArrowBtn: Button? = null
    var jumpPrevArrowBtn: Button? = null
    var jumpNextArrowBtn: Button? = null
    var titleBox: TextBox? = null

    val rearrangeCallbacks: MutableList<(Int, Panel) -> Unit> = mutableListOf()
    val commitCallbacks: MutableList<(TabContainer, String) -> Unit> = mutableListOf()

    fun getTabCount(): Int = tabList.size
    fun getCurrentPanelIndex(): Int = currentTabIdx
    fun setCurrentPanelIndex(index: Int) { currentTabIdx = index }
    fun getTabPosition(): TabPosition = tabPosition
    fun getTopBorderHeight(): Int = topBorderHeight
    fun setTopBorderHeight(height: Int) { topBorderHeight = height }
    fun getNumLockedTabs(): Int = lockedTabCount
    fun getTotalTabWidth(): Int = totalTabWidth
    fun getScrollPos(): Int = scrollPos
    fun setScrollPos(pos: Int) { scrollPos = pos }
    fun getMaxScrollPos(): Int = maxScrollPos
    fun setMaxScrollPos(pos: Int) { maxScrollPos = pos }
    fun getScrollPosPixels(): Int = scrollPosPixels
    fun setScrollPosPixels(pixels: Int) { scrollPosPixels = pixels }
    fun getTabsHidden(): Boolean = tabsHidden
    fun setTabsHidden(hidden: Boolean) { tabsHidden = hidden }
    fun setRightTabBtnOffset(offset: Int) { rightTabBtnOffset = offset }
    fun startDragAndDropDelayTimer() { dragAndDropDelayTimerStarted = true; dragAndDropElapsed = 0f }

    private fun scrollPrev() { scrollPos = maxOf(0, scrollPos - 1) }
    private fun scrollNext() { scrollPos = minOf(scrollPos + 1, maxScrollPos) }

    fun addRearrangeCallback(cb: (Int, Panel) -> Unit) { rearrangeCallbacks.add(cb) }

    fun addTabPanel(panel: Panel) {
        addTabPanel(panel, panel.label, false, false, 0, InsertionPoint.END, null)
    }

    fun addTabPanel(
        panel: Panel,
        label: String = panel.label,
        selectTab: Boolean = false,
        isPlaceholder: Boolean = false,
        indent: Int = 0,
        insertAt: InsertionPoint = InsertionPoint.END,
        userData: Any? = null
    ) {
        if (tabList.any { it.tabPanel === panel }) return

        panel.label = label
        panel.setVisible(false)

        val btn = Button(label)
        totalTabWidth += minTabWidth

        val tuple = TabTuple(this, panel, btn)
        insertTuple(tuple, insertAt)

        if (selectTab) selectTabPanel(panel)
    }

    fun addPlaceholder(child: Panel, label: String) {
        addTabPanel(child, label, isPlaceholder = true)
    }

    fun removeTabPanel(child: Panel) {
        val tuple = getTabByPanel(child) ?: return
        totalTabWidth -= minTabWidth
        tabList.remove(tuple)
        updateMaxScrollPos()
        if (currentTabIdx >= tabList.size) currentTabIdx = tabList.size - 1
    }

    fun lockTabs(numTabs: Int = 0) {
        lockedTabCount = if (numTabs == 0) tabList.size else numTabs
    }

    fun unlockTabs() { lockedTabCount = 0 }

    fun enableTabButton(which: Int, enable: Boolean) {
        tabList.getOrNull(which)?.button?.enabled = enable
    }

    fun deleteAllTabs() {
        tabList.clear()
        totalTabWidth = 0
        currentTabIdx = -1
        updateMaxScrollPos()
    }

    fun getCurrentPanel(): Panel? = tabList.getOrNull(currentTabIdx)?.tabPanel

    fun getPanelByIndex(index: Int): Panel? = tabList.getOrNull(index)?.tabPanel

    fun getIndexForPanel(panel: Panel): Int = tabList.indexOfFirst { it.tabPanel === panel }

    fun getPanelIndexByTitle(title: String): Int = tabList.indexOfFirst { it.tabPanel.label == title }

    fun getPanelByName(name: String): Panel? = tabList.firstOrNull { it.tabPanel.name == name }?.tabPanel

    fun setCurrentTabName(name: String) { selectTabByName(name) }

    fun getPanelTitle(index: Int): String = tabList.getOrNull(index)?.tabPanel?.label ?: ""

    fun setPanelTitle(index: Int, title: String) {
        tabList.getOrNull(index)?.tabPanel?.label = title
        tabList.getOrNull(index)?.button?.let { it.name }
    }

    fun selectFirstTab() { if (tabList.isNotEmpty()) setTab(0) }
    fun selectLastTab() { if (tabList.isNotEmpty()) setTab(tabList.size - 1) }

    fun selectNextTab() {
        val start = if (currentTabIdx < 0) 0 else currentTabIdx + 1
        for (i in start until tabList.size) {
            if (tabList[i].tabPanel.getVisible()) { setTab(i); return }
        }
        for (i in 0 until start) {
            if (tabList[i].tabPanel.getVisible()) { setTab(i); return }
        }
    }

    fun selectPrevTab() {
        val start = if (currentTabIdx <= 0) tabList.size - 1 else currentTabIdx - 1
        for (i in start downTo 0) {
            if (tabList[i].tabPanel.getVisible()) { setTab(i); return }
        }
        for (i in tabList.size - 1 downTo start) {
            if (tabList[i].tabPanel.getVisible()) { setTab(i); return }
        }
    }

    fun selectTabPanel(child: Panel): Boolean {
        val idx = getIndexForPanel(child)
        if (idx < 0) return false
        return setTab(idx)
    }

    fun selectTab(which: Int): Boolean = setTab(which)

    fun selectTabByName(title: String): Boolean {
        val idx = getPanelIndexByTitle(title)
        if (idx < 0) return false
        return setTab(idx)
    }

    private fun setTab(which: Int): Boolean {
        if (which < 0 || which >= tabList.size) return false
        val prev = getCurrentPanel()
        prev?.setVisible(false)
        currentTabIdx = which
        val panel = tabList[which].tabPanel
        panel.setVisible(true)
        commitCallbacks.forEach { it(this, panel.name) }
        return true
    }

    fun getTabPanelFlashing(child: Panel): Boolean {
        return getTabByPanel(child)?.button?.getFlashing() ?: false
    }

    fun setTabPanelFlashing(child: Panel, state: Boolean, alternateColor: Boolean = false) {
        getTabByPanel(child)?.button?.setFlashing(state)
    }

    fun setTabImage(child: Panel, imgName: String, color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)) {
        TODO("GPU: set tab image by name '$imgName'")
    }

    fun setTabImage(child: Panel, icon: IconCtrl) {
        TODO("GPU: set tab image from IconCtrl")
    }

    fun setTitle(title: String) { titleBox?.setText(title) }

    fun setTabVisibility(panel: Panel, visible: Boolean) {
        getTabByPanel(panel)?.let { it.visible = visible }
        updateMaxScrollPos()
    }

    fun setValue(value: Int) { selectTab(value) }

    fun reshape(width: Int, height: Int) {
        rect = rect.copy(right = rect.left + width, top = rect.bottom + height)
        updateMaxScrollPos()
    }

    open fun draw() {
        TODO("GPU: render tab container")
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val hasScrollArrows = !hideScrollArrows && maxScrollPos > 0 && !tabsHidden
        var handled = false

        if (hasScrollArrows) {
            handled = dispatchToArrows(x, y, mask, Button::handleMouseDown) || handled
        }
        if (!handled) handled = handlePanelMouseDown(x, y, mask)

        if (tabList.isNotEmpty() && !tabsHidden) {
            mouseDownElapsed = 0f
        }
        return handled
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        val hasScrollArrows = !hideScrollArrows && maxScrollPos > 0 && !tabsHidden
        var handled = false
        if (hasScrollArrows) handled = dispatchToArrows(x, y, mask, Button::handleHover) || handled
        if (!handled) handled = handlePanelHover(x, y, mask)
        if (mouseDownElapsed > 0.25f) commitHoveredButton(x, y)
        return handled
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        val hasScrollArrows = !hideScrollArrows && maxScrollPos > 0 && !tabsHidden
        var handled = false
        if (hasScrollArrows) handled = dispatchToArrows(x, y, mask, Button::handleMouseUp) || handled
        if (!handled) handled = handlePanelMouseUp(x, y, mask)
        commitHoveredButton(x, y)
        mouseDownElapsed = 0f
        return handled
    }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (tabList.isNotEmpty() && !tabsHidden) {
            scrollPos = (scrollPos + clicks).coerceIn(0, maxScrollPos)
            return true
        }
        return false
    }

    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        for (tuple in tabList) {
            if (!tuple.button.visible) continue
            val btn = tuple.button
            val localX = x - btn.getRect().left
            val localY = y - btn.getRect().bottom
            if (btn.handleToolTip(localX, localY, mask)) return true
        }
        return false
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (allowRearrange) return false

        val keyLeft = 0x101
        val keyRight = 0x102
        val keyUp = 0x103
        val keyDown = 0x104
        val maskAlt = 0x0100

        var handled = false
        if (mask and maskAlt != 0) {
            when (key) {
                keyLeft -> { selectPrevTab(); handled = true }
                keyRight -> { selectNextTab(); handled = true }
            }
        }
        if (handled) {
            getCurrentPanel()?.setFocus(true)
            return true
        }

        if (isVertical) {
            when (key) {
                keyUp -> { selectPrevTab(); handled = true }
                keyDown -> { selectNextTab(); handled = true }
                keyLeft -> handled = true
                keyRight -> { if (tabPosition == TabPosition.LEFT) getCurrentPanel()?.setFocus(true); handled = true }
            }
        } else {
            when (key) {
                keyUp -> { if (tabPosition == TabPosition.BOTTOM) getCurrentPanel()?.setFocus(true); handled = true }
                keyDown -> { if (tabPosition == TabPosition.TOP) getCurrentPanel()?.setFocus(true); handled = true }
                keyLeft -> { selectPrevTab(); handled = true }
                keyRight -> { selectNextTab(); handled = true }
            }
        }

        if (handled) getCurrentPanel()?.setFocus(true)
        return handled
    }

    open fun handleDragAndDrop(x: Int, y: Int, mask: Int, drop: Boolean, type: Int, cargoData: Any?, accept: IntArray, tooltip: String): Boolean {
        val hasScrollArrows = !hideScrollArrows && maxScrollPos > 0
        if (openTabsOnDragAndDrop && !tabsHidden) {
            if (dragAndDropDelayTimerStarted) {
                if (dragAndDropElapsed > SCROLL_DELAY_TIME) {
                    for (tuple in tabList) {
                        tuple.button.setVisible(true)
                        val localX = x - tuple.button.getRect().left
                        val localY = y - tuple.button.getRect().bottom
                        if (tuple.button.pointInView(localX, localY) && tuple.button.getEnabled() && !tuple.tabPanel.getVisible()) {
                            tuple.button.onCommit()
                        }
                    }
                    dragAndDropDelayTimerStarted = false
                }
            } else {
                startDragAndDropDelayTimer()
            }
        }
        return false
    }

    fun onTabBtn(data: String, panel: Panel) { selectTabPanel(panel) }
    fun onNextBtn(data: String) { scrollNext() }
    fun onNextBtnHeld(data: String) { scrollNext() }
    fun onPrevBtn(data: String) { scrollPrev() }
    fun onPrevBtnHeld(data: String) { scrollPrev() }
    fun onJumpFirstBtn(data: String) { scrollPos = 0 }
    fun onJumpLastBtn(data: String) { scrollPos = maxScrollPos }

    private fun getTab(index: Int): TabTuple = tabList[index]
    private fun getTabByPanel(child: Panel): TabTuple? = tabList.firstOrNull { it.tabPanel === child }

    private fun insertTuple(tuple: TabTuple, insertionPoint: InsertionPoint) {
        when (insertionPoint) {
            InsertionPoint.START -> tabList.add(0, tuple)
            InsertionPoint.END -> tabList.add(tuple)
            InsertionPoint.LEFT_OF_CURRENT -> {
                val idx = if (currentTabIdx >= 0) currentTabIdx else tabList.size
                tabList.add(idx, tuple)
            }
            InsertionPoint.RIGHT_OF_CURRENT -> {
                val idx = if (currentTabIdx >= 0) currentTabIdx + 1 else tabList.size
                tabList.add(idx.coerceAtMost(tabList.size), tuple)
            }
        }
    }

    private fun updateMaxScrollPos() {
        maxScrollPos = maxOf(0, tabList.count { it.visible } - 1)
        scrollPos = scrollPos.coerceIn(0, maxScrollPos)
    }

    private fun commitHoveredButton(x: Int, y: Int) {
        for (tuple in tabList) {
            val btn = tuple.button
            if (!btn.visible || !btn.getEnabled()) continue
            val localX = x - btn.getRect().left
            val localY = y - btn.getRect().bottom
            if (btn.pointInView(localX, localY)) {
                selectTabPanel(tuple.tabPanel)
                break
            }
        }
    }

    private fun dispatchToArrows(x: Int, y: Int, mask: Int, action: Button.(Int, Int, Int) -> Boolean): Boolean {
        listOfNotNull(jumpPrevArrowBtn, jumpNextArrowBtn, prevArrowBtn, nextArrowBtn).forEach { btn ->
            if (btn.getRect().pointInRect(x, y)) {
                val lx = x - btn.getRect().left
                val ly = y - btn.getRect().bottom
                if (btn.action(lx, ly, mask)) return true
            }
        }
        return false
    }

    protected open fun handlePanelMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    protected open fun handlePanelHover(x: Int, y: Int, mask: Int): Boolean = false
    protected open fun handlePanelMouseUp(x: Int, y: Int, mask: Int): Boolean = false
}
