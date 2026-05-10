package com.firestorm.llui

open class MultiFloater(
    key: String,
    minWidth: Int = 0,
    minHeight: Int = 0
) : Floater(key) {

    data class FloaterData(
        val width: Int,
        val height: Int,
        val canMinimize: Boolean,
        val canResize: Boolean,
        val saveRect: Boolean
    )

    protected var tabContainer: TabContainer? = null
    protected val floaterDataMap: MutableMap<Floater, FloaterData> = mutableMapOf()
    protected var tabPos: TabContainer.TabPosition = TabContainer.TabPosition.TOP
    protected var autoResize: Boolean = true
    protected var origMinWidth: Int = minWidth
    protected var origMinHeight: Int = minHeight

    val closeSignals: MutableList<() -> Unit> = mutableListOf()

    init {
        closeSignals.add { closeAllFloaters() }
    }

    fun buildTabContainer() {
        val tc = TabContainer("Preview Tabs")
        tc.tabPosition = tabPos
        tc.onTabChanged = { _, _ -> onTabSelected() }
        addChild(tc)
        tabContainer = tc
        if (resizable) {
            TODO("GPU: tc.setRightTabBtnOffset(RESIZE_HANDLE_WIDTH)")
        }
    }

    open fun postBuild(): Boolean {
        // APR: getResizeLimits(&origMinWidth, &origMinHeight)
        closeSignals.add { closeAllFloaters() }

        if (tabContainer != null) return true

        // APR: setTabContainer(getChild<TabContainer>("Preview Tabs"))
        setCanResize(resizable)
        return true
    }

    open fun onClose(appQuitting: Boolean) {
        if (isMinimized) isMinimized = false
        super.onClose(appQuitting)
    }

    override fun draw() {
        val tc = tabContainer ?: return
        if (tc.getTabCount() == 0) {
            close()
        } else {
            super.draw()
        }
    }

    open fun setVisible(vis: Boolean) {
        visible = vis
        val tc = tabContainer ?: return
        val cur = tc.getCurrentPanel() as? Floater
        if (cur != null) {
            cur.visible = vis
        } else if (vis) {
            tc.selectLastTab()
        }
    }

    open fun handleKeyHere(key: Int, mask: UInt): Boolean {
        if (key == KEY_W && mask == MASK_CONTROL) {
            val floater = getActiveFloater()
            if (floater != null && floater.canClose() && floater.closeable) {
                floater.close()
                if (tabContainer?.getTabCount() ?: 0 > 0) {
                    tabContainer?.setFocus(true)
                }
            }
            return true
        }
        return TODO("APR: super.handleKeyHere($key, $mask)")
    }

    open fun addChild(view: View, tabGroup: Int = 0): Boolean {
        val tc = view as? TabContainer
        if (tc != null) setTabContainer(tc)
        return TODO("APR: Floater.addChild(view, tabGroup)")
    }

    open fun setCanResize(canResize: Boolean) {
        resizable = canResize
        val tc = tabContainer ?: return
        if (resizable && tc.tabPosition == TabContainer.TabPosition.BOTTOM) {
            TODO("GPU: tc.setRightTabBtnOffset(RESIZE_HANDLE_WIDTH)")
        } else {
            TODO("GPU: tc.setRightTabBtnOffset(0)")
        }
    }

    open fun growToFit(contentWidth: Int, contentHeight: Int) {
        val floaterHeaderSize: Int = TODO("APR: default_params.header_height")
        val tabcntrHeaderHeight: Int = TODO("APR: LLPANEL_BORDER_WIDTH + UITabCntrCloseBtnSize")
        val rectWidth: Int = TODO("GPU: getRect().width")
        val rectHeight: Int = TODO("GPU: getRect().height")
        val borderPad: Int = TODO("GPU: LLPANEL_BORDER_WIDTH * 2")
        val newWidth = maxOf(rectWidth, contentWidth + borderPad)
        val newHeight = maxOf(rectHeight, contentHeight + floaterHeaderSize + tabcntrHeaderHeight)

        if (isMinimized) {
            TODO("GPU: setExpandedRect to newWidth x newHeight keeping top-left corner")
        } else {
            val oldHeight: Int = TODO("GPU: getRect().height")
            TODO("GPU: reshape($newWidth, $newHeight); translate(0, $oldHeight - $newHeight)")
        }
    }

    open fun addFloater(
        floaterp: Floater,
        selectAddedFloater: Boolean,
        insertionPoint: TabContainer.InsertionPoint = TabContainer.InsertionPoint.END
    ) {
        val tc = tabContainer ?: run {
            error("Tab Container used without having been initialized.")
        }

        when {
            floaterp.host === this -> {
                floaterDataMap.remove(floaterp)
                tc.removeTabPanel(floaterp)
            }
            floaterp.host != null -> floaterp.host!!.removeFloater(floaterp)
            else -> TODO("APR: if floaterp.parent == gFloaterView then gFloaterView.removeChild(floaterp)")
        }

        val floaterData = FloaterData(
            width = TODO("GPU: floaterp.rect.width"),
            height = TODO("GPU: floaterp.rect.height"),
            canMinimize = floaterp.minimizable,
            canResize = floaterp.resizable,
            saveRect = floaterp.saveRect
        )

        floaterp.minimizable = false
        floaterp.resizable = false
        floaterp.draggable = false
        floaterp.saveRect = false
        TODO("APR: floaterp.storeRectControl()")
        TODO("GPU: floaterp.setBackgroundVisible(false)")

        if (!hostedFloaterShowTitlebar) {
            TODO("GPU: floaterp.getDragHandle().setTitleVisible(false); shrink rect by headerHeight")
        }

        if (autoResize) growToFit(floaterData.width, floaterData.height)

        tc.addTabPanel(floaterp, floaterp.shortTitle, selectAddedFloater, insertionPoint)
        floaterDataMap[floaterp] = floaterData

        updateResizeLimits()

        if (selectAddedFloater) {
            tc.selectTabPanel(floaterp)
        } else {
            tc.selectTab(tc.getCurrentPanelIndex())
        }

        floaterp.host = this
        if (isMinimized) floaterp.visible = false

        TODO("GPU: moveResizeHandlesToFront()")
    }

    open fun showFloater(
        floaterp: Floater?,
        insertionPoint: TabContainer.InsertionPoint = TabContainer.InsertionPoint.END
    ) {
        floaterp ?: return
        val tc = tabContainer ?: return
        if (floaterp !== tc.getCurrentPanel() && !tc.selectTabPanel(floaterp)) {
            addFloater(floaterp, true, insertionPoint)
        }
    }

    open fun removeFloater(floaterp: Floater?) {
        floaterp ?: return
        if (floaterp.host !== this) return
        val tc = tabContainer ?: return

        if (!hostedFloaterShowTitlebar) {
            TODO("GPU: floaterp.getDragHandle().setTitleVisible(true); grow rect by headerHeight")
        }

        val data = floaterDataMap.remove(floaterp)
        if (data != null) {
            floaterp.minimizable = data.canMinimize
            floaterp.saveRect = data.saveRect
            if (!data.canResize) {
                TODO("GPU: floaterp.reshape(data.width, data.height)")
            }
            floaterp.resizable = data.canResize
        }

        tc.removeTabPanel(floaterp)
        TODO("GPU: floaterp.setBackgroundVisible(true)")
        floaterp.draggable = true
        floaterp.host = null
        TODO("APR: floaterp.applyRectControl(); floaterp.setFollowsNone()")

        updateResizeLimits()

        val tabFloater = tc.getCurrentPanel() as? Floater
        if (tabFloater != null) tabOpen(tabFloater, false)
    }

    open fun tabOpen(openedFloater: Floater, fromClick: Boolean) {}

    open fun tabClose() {
        if (tabContainer?.getTabCount() == 0) close()
    }

    open fun selectFloater(floaterp: Floater): Boolean =
        tabContainer?.selectTabPanel(floaterp) ?: false

    open fun selectNextFloater() { tabContainer?.selectNextTab() }
    open fun selectPrevFloater() { tabContainer?.selectPrevTab() }

    open fun getActiveFloater(): Floater? = tabContainer?.getCurrentPanel() as? Floater

    open fun isFloaterFlashing(floaterp: Floater?): Boolean {
        if (floaterp == null || floaterp.host !== this) return false
        return tabContainer?.getTabPanelFlashing(floaterp) ?: false
    }

    open fun setFloaterFlashing(floaterp: Floater?, flashing: Boolean, alternateColor: Boolean = false) {
        if (floaterp != null && floaterp.host === this) {
            tabContainer?.setTabPanelFlashing(floaterp, flashing, alternateColor)
        }
    }

    open fun closeAllFloaters(): Boolean {
        val tc = tabContainer ?: return true
        var tabToClose = 0
        var lastTabCount = tc.getTabCount()
        while (tabToClose < tc.getTabCount()) {
            val first = tc.getPanelByIndex(tabToClose) as? Floater ?: break
            first.close()
            if (lastTabCount == tc.getTabCount()) {
                tabToClose++
            } else {
                lastTabCount = tc.getTabCount()
            }
        }
        return tc.getTabCount() == 0
    }

    fun setTabContainer(tc: TabContainer?) {
        if (tc != null && tabContainer == null) {
            tabContainer = tc
            tc.onTabChanged = { _, _ -> onTabSelected() }
        }
    }

    fun onTabSelected() {
        val floaterp = tabContainer?.getCurrentPanel() as? Floater ?: return
        tabOpen(floaterp, true)
        if (!hostedFloaterShowTitlebar) {
            TODO("GPU: dragHandle.setTitle(title + \" - \" + floaterp.title)")
        }
    }

    open fun updateFloaterTitle(floaterp: Floater) {
        val tc = tabContainer ?: return
        val index = tc.getIndexForPanel(floaterp)
        if (index != -1) {
            tc.setPanelTitle(index, floaterp.shortTitle)
            if (!hostedFloaterShowTitlebar && floaterp === tc.getCurrentPanel()) {
                TODO("GPU: dragHandle.setTitle(title + \" - \" + floaterp.title)")
            }
        }
    }

    open fun updateResizeLimits() {
        var newMinWidth = origMinWidth
        var newMinHeight = origMinHeight
        computeResizeLimits(newMinWidth, newMinHeight)
        TODO("GPU: setResizeLimits($newMinWidth, $newMinHeight)")

        val curHeight: Int = TODO("GPU: getRect().height")
        val rectWidth: Int = TODO("GPU: getRect().width")
        val newWidth = maxOf(rectWidth, newMinWidth)
        val newHeight = maxOf(curHeight, newMinHeight)

        if (isMinimized) {
            TODO("GPU: update expandedRect to at least newWidth x newHeight")
        } else {
            TODO("GPU: reshape($newWidth, $newHeight); translate(0, curHeight - newHeight); gFloaterView.adjustToFitScreen(this, true)")
        }
    }

    fun closeDockedFloater() {
        val floater = getActiveFloater()
        if (floater != null && floater.canClose() && floater.closeable) {
            floater.close()
            if (tabContainer?.getTabCount() ?: 0 > 0) {
                tabContainer?.setFocus(true)
            } else {
                close()
            }
            return
        }
        close()
    }

    private fun computeResizeLimits(newMinWidth: Int, newMinHeight: Int) {
        val tc = tabContainer ?: return
        val floaterHeaderSize: Int = TODO("APR: default_params.header_height")
        val tabcntrHeaderHeight: Int = TODO("APR: LLPANEL_BORDER_WIDTH + UITabCntrCloseBtnSize")
        for (i in 0 until tc.getTabCount()) {
            val fp = tc.getPanelByIndex(i) as? Floater ?: continue
            TODO("GPU: newMinWidth = max(newMinWidth, fp.minWidth + LLPANEL_BORDER_WIDTH * 2); newMinHeight = max(newMinHeight, fp.minHeight + floaterHeaderSize + tabcntrHeaderHeight)")
        }
    }

    protected open var hostedFloaterShowTitlebar: Boolean = true

    companion object {
        private const val KEY_W: Int = 'W'.code
        private const val MASK_CONTROL: UInt = 0x01u
    }
}

var Floater.host: MultiFloater?
    get() = TODO("APR: return the hosting MultiFloater for this floater, null if none")
    set(_) { TODO("APR: assign the hosting MultiFloater for this floater") }

var Floater.saveRect: Boolean
    get() = TODO("APR: get floater saveRect flag")
    set(_) { TODO("APR: set floater saveRect flag") }

var Floater.draggable: Boolean
    get() = TODO("APR: get floater draggable flag")
    set(_) { TODO("APR: set floater draggable flag") }

val Floater.shortTitle: String
    get() = TODO("APR: return abbreviated title suitable for a tab label")

fun Floater.canClose(): Boolean = TODO("APR: return whether this floater can be closed right now")

fun TabContainer.getTabCount(): Int = TODO("APR: return number of tabs")

fun TabContainer.getCurrentPanel(): Any? = TODO("APR: return the currently visible tab panel")

fun TabContainer.getCurrentPanelIndex(): Int = TODO("APR: return index of the current panel")

fun TabContainer.getPanelByIndex(index: Int): Any? = TODO("APR: return panel at given index")

fun TabContainer.getIndexForPanel(panel: Any): Int = TODO("APR: return tab index for panel, -1 if absent")

fun TabContainer.addTabPanel(
    panel: Panel,
    label: String,
    selectTab: Boolean = false,
    insertAt: TabContainer.InsertionPoint = TabContainer.InsertionPoint.END
) { TODO("APR: insert panel as a new tab") }

fun TabContainer.removeTabPanel(panel: Panel) { TODO("APR: remove this panel's tab") }

fun TabContainer.selectTabPanel(panel: Any): Boolean = TODO("APR: activate the tab for panel; return true if found")

fun TabContainer.selectTab(index: Int) { TODO("APR: activate tab at index") }

fun TabContainer.selectNextTab() { TODO("APR: advance to next tab, wrapping") }

fun TabContainer.selectPrevTab() { TODO("APR: retreat to previous tab, wrapping") }

fun TabContainer.selectLastTab() { TODO("APR: activate the last tab") }

fun TabContainer.setPanelTitle(index: Int, title: String) { TODO("APR: update the tab label at index") }

fun TabContainer.getTabPanelFlashing(panel: Any): Boolean = TODO("APR: return flashing state for panel's tab")

fun TabContainer.setTabPanelFlashing(panel: Any, flashing: Boolean, alternateColor: Boolean = false) {
    TODO("APR: set flashing state for panel's tab")
}

fun TabContainer.setFocus(focus: Boolean) { TODO("APR: direct keyboard focus to this tab container") }
