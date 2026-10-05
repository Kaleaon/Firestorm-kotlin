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
            // no-op
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
        return false
    }

    open fun addChild(view: View, tabGroup: Int = 0): Boolean {
        val tc = view as? TabContainer
        if (tc != null) setTabContainer(tc)
        return false
    }

    open fun setCanResize(canResize: Boolean) {
        resizable = canResize
        val tc = tabContainer ?: return
        if (resizable && tc.tabPosition == TabContainer.TabPosition.BOTTOM) {
            // no-op
        } else {
            // no-op
        }
    }

    open fun growToFit(contentWidth: Int, contentHeight: Int) {
        val floaterHeaderSize: Int = 0
        val tabcntrHeaderHeight: Int = 0
        val rectWidth: Int = 0
        val rectHeight: Int = 0
        val borderPad: Int = 0
        val newWidth = maxOf(rectWidth, contentWidth + borderPad)
        val newHeight = maxOf(rectHeight, contentHeight + floaterHeaderSize + tabcntrHeaderHeight)

        if (isMinimized) {
            // no-op
        } else {
            val oldHeight: Int = 0
            // no-op
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
            else -> System.err.println("MultiFloater: addFloater gFloaterView.removeChild not yet implemented")
        }

        val floaterData = FloaterData(
            width = 0,
            height = 0,
            canMinimize = floaterp.minimizable,
            canResize = floaterp.resizable,
            saveRect = floaterp.saveRect
        )

        floaterp.minimizable = false
        floaterp.resizable = false
        floaterp.draggable = false
        floaterp.saveRect = false
        System.err.println("MultiFloater: floaterp.storeRectControl() not yet implemented")
        System.err.println("MultiFloater: floaterp.setBackgroundVisible(false) not yet implemented")

        if (!hostedFloaterShowTitlebar) {
            System.err.println("MultiFloater: getDragHandle().setTitleVisible(false) not yet implemented")
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

        System.err.println("MultiFloater: moveResizeHandlesToFront() not yet implemented")
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
            System.err.println("MultiFloater: getDragHandle().setTitleVisible(true) not yet implemented")
        }

        val data = floaterDataMap.remove(floaterp)
        if (data != null) {
            floaterp.minimizable = data.canMinimize
            floaterp.saveRect = data.saveRect
            if (!data.canResize) {
                System.err.println("MultiFloater: floaterp.reshape(data.width, data.height) not yet implemented")
            }
            floaterp.resizable = data.canResize
        }

        tc.removeTabPanel(floaterp)
        System.err.println("MultiFloater: floaterp.setBackgroundVisible(true) not yet implemented")
        floaterp.draggable = true
        floaterp.host = null
        System.err.println("MultiFloater: floaterp.applyRectControl() not yet implemented")

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
            System.err.println("MultiFloater: dragHandle.setTitle not yet implemented")
        }
    }

    open fun updateFloaterTitle(floaterp: Floater) {
        val tc = tabContainer ?: return
        val index = tc.getIndexForPanel(floaterp)
        if (index != -1) {
            tc.setPanelTitle(index, floaterp.shortTitle)
            if (!hostedFloaterShowTitlebar && floaterp === tc.getCurrentPanel()) {
                System.err.println("MultiFloater: dragHandle.setTitle not yet implemented")
            }
        }
    }

    open fun updateResizeLimits() {
        var newMinWidth = origMinWidth
        var newMinHeight = origMinHeight
        computeResizeLimits(newMinWidth, newMinHeight)
        System.err.println("MultiFloater: setResizeLimits not yet implemented")

        val curHeight: Int = 0
        val rectWidth: Int = 0
        val newWidth = maxOf(rectWidth, newMinWidth)
        val newHeight = maxOf(curHeight, newMinHeight)

        if (isMinimized) {
            System.err.println("MultiFloater: update expandedRect not yet implemented")
        } else {
            System.err.println("MultiFloater: reshape/translate/adjustToFitScreen not yet implemented")
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
        val floaterHeaderSize: Int = 0
        val tabcntrHeaderHeight: Int = 0
        for (i in 0 until tc.getTabCount()) {
            val fp = tc.getPanelByIndex(i) as? Floater ?: continue
            System.err.println("MultiFloater: computeResizeLimits min dimensions not yet implemented")
        }
    }

    protected open var hostedFloaterShowTitlebar: Boolean = true

    companion object {
        private const val KEY_W: Int = 'W'.code
        private const val MASK_CONTROL: UInt = 0x01u
    }
}

var Floater.host: MultiFloater?
    get() = null
    set(_) { System.err.println("Floater: host setter not yet implemented") }

var Floater.saveRect: Boolean
    get() = false
    set(_) { System.err.println("Floater: saveRect setter not yet implemented") }

var Floater.draggable: Boolean
    get() = false
    set(_) { System.err.println("Floater: draggable setter not yet implemented") }

val Floater.shortTitle: String
    get() = ""

fun Floater.canClose(): Boolean = false

fun TabContainer.getTabCount(): Int = 0

fun TabContainer.getCurrentPanel(): Any? = null

fun TabContainer.getCurrentPanelIndex(): Int = 0

fun TabContainer.getPanelByIndex(index: Int): Any? = null

fun TabContainer.getIndexForPanel(panel: Any): Int = -1

fun TabContainer.addTabPanel(
    panel: Panel,
    label: String,
    selectTab: Boolean = false,
    insertAt: TabContainer.InsertionPoint = TabContainer.InsertionPoint.END
) { System.err.println("TabContainer: addTabPanel not yet implemented") }

fun TabContainer.removeTabPanel(panel: Panel) { System.err.println("TabContainer: removeTabPanel not yet implemented") }

fun TabContainer.selectTabPanel(panel: Any): Boolean = false

fun TabContainer.selectTab(index: Int) { System.err.println("TabContainer: selectTab not yet implemented") }

fun TabContainer.selectNextTab() { System.err.println("TabContainer: selectNextTab not yet implemented") }

fun TabContainer.selectPrevTab() { System.err.println("TabContainer: selectPrevTab not yet implemented") }

fun TabContainer.selectLastTab() { System.err.println("TabContainer: selectLastTab not yet implemented") }

fun TabContainer.setPanelTitle(index: Int, title: String) { System.err.println("TabContainer: setPanelTitle not yet implemented") }

fun TabContainer.getTabPanelFlashing(panel: Any): Boolean = false

fun TabContainer.setTabPanelFlashing(panel: Any, flashing: Boolean, alternateColor: Boolean = false) {
    System.err.println("TabContainer: setTabPanelFlashing not yet implemented")
}

fun TabContainer.setFocus(focus: Boolean) { System.err.println("TabContainer: setFocus not yet implemented") }
