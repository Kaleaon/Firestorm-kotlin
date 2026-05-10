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

    enum class TabPosition { TOP, BOTTOM }
    enum class InsertionPoint { BEGIN, END, LEFT_OF_CURRENT, RIGHT_OF_CURRENT }

    protected var tabContainer: TabContainer? = null
    protected val floaterDataMap: MutableMap<Floater, FloaterData> = mutableMapOf()
    protected var tabPos: TabPosition = TabPosition.TOP
    protected var autoResize: Boolean = true
    protected val origMinWidth: Int = minWidth
    protected val origMinHeight: Int = minHeight

    fun buildTabContainer() {
        val tc = TabContainer("Preview Tabs", tabPos)
        tc.commitCallback = { onTabSelected() }
        tabContainer = tc
        addChildView(tc)
        if (isResizable()) {
            tc.rightTabBtnOffset = RESIZE_HANDLE_WIDTH
        }
    }

    open fun postBuild(): Boolean {
        closeCallbacks.add { closeAllFloaters() }
        getResizeLimits()

        if (tabContainer != null) return true

        setTabContainer(findChildTabContainer("Preview Tabs"))
        setCanResize(resizable)
        return true
    }

    open fun onClose(appQuitting: Boolean) {
        if (isMinimized) setMinimized(false)
        close(appQuitting)
    }

    open fun draw() {
        if (tabContainer?.tabCount == 0) {
            close()
        } else {
            TODO("GPU: draw multifloater contents via tab container")
        }
    }

    open fun setVisible(vis: Boolean) {
        super.visible = vis
        val tc = tabContainer ?: return
        val cur = tc.currentPanel
        if (cur != null) {
            cur.visible = vis
        } else if (vis) {
            tc.selectLastTab()
        }
    }

    open fun handleKeyHere(key: Int, mask: UInt): Boolean {
        if (key == KEY_W && mask == MASK_CONTROL) {
            val floater = getActiveFloater()
            if (floater != null && floater.canClose() && floater.isCloseable()) {
                floater.close()
                if ((tabContainer?.tabCount ?: 0) > 0) {
                    tabContainer?.setFocus(true)
                }
            }
            return true
        }
        return TODO("APR: delegate to super.handleKeyHere($key, $mask)")
    }

    open fun addChild(view: Any, tabGroup: Int = 0): Boolean {
        val tc = view as? TabContainer
        if (tc != null) setTabContainer(tc)
        return addChildView(view)
    }

    open fun setCanResize(canResize: Boolean) {
        TODO("APR: set resize capability to $canResize on floater")
        val tc = tabContainer ?: return
        tc.rightTabBtnOffset = if (isResizable() && tc.tabPosition == TabPosition.BOTTOM) RESIZE_HANDLE_WIDTH else 0
    }

    open fun growToFit(contentWidth: Int, contentHeight: Int) {
        val floaterHeaderSize = defaultFloaterHeaderHeight()
        val tabcntrHeaderHeight = PANEL_BORDER_WIDTH + tabCloseBtnSize()
        val newWidth = maxOf(currentWidth(), contentWidth + PANEL_BORDER_WIDTH * 2)
        val newHeight = maxOf(currentHeight(), contentHeight + floaterHeaderSize + tabcntrHeaderHeight)

        if (isMinimized) {
            TODO("APR: set expanded rect to ($newWidth x $newHeight)")
        } else {
            val oldHeight = currentHeight()
            reshape(newWidth, newHeight)
            translate(0, oldHeight - newHeight)
        }
    }

    open fun addFloater(
        floaterp: Floater,
        selectAddedFloater: Boolean,
        insertionPoint: InsertionPoint = InsertionPoint.END
    ) {
        val tc = tabContainer ?: run {
            println("Tab Container used without having been initialized.")
            return
        }

        if (floaterp.getHost() === this) {
            floaterDataMap.remove(floaterp)
            tc.removeTabPanel(floaterp)
        } else if (floaterp.getHost() != null) {
            floaterp.getHost()?.removeFloater(floaterp)
        } else {
            TODO("APR: if floaterp is child of gFloaterView, reparent it")
        }

        val floaterData = FloaterData(
            width = floaterp.currentWidth(),
            height = floaterp.currentHeight(),
            canMinimize = floaterp.minimizable,
            canResize = floaterp.resizable,
            saveRect = floaterp.saveRect
        )

        floaterp.minimizable = false
        floaterp.resizable = false
        floaterp.draggable = false
        floaterp.saveRect = false
        floaterp.storeRectControl()
        floaterp.setBackgroundVisible(false)

        if (!hostedFloaterShowTitlebar) {
            floaterp.setDragHandleTitleVisible(false)
            floaterp.shrinkByHeaderHeight()
        }

        if (autoResize) growToFit(floaterData.width, floaterData.height)

        tc.addTabPanel(floaterp, floaterp.shortTitle, insertionPoint)
        floaterDataMap[floaterp] = floaterData

        updateResizeLimits()

        if (selectAddedFloater) {
            tc.selectTabPanel(floaterp)
        } else {
            tc.selectTab(tc.currentPanelIndex)
        }

        floaterp.setHost(this)
        if (isMinimized) floaterp.visible = false

        moveResizeHandlesToFront()
    }

    open fun showFloater(floaterp: Floater?, insertionPoint: InsertionPoint = InsertionPoint.END) {
        floaterp ?: return
        val tc = tabContainer ?: return
        if (floaterp !== tc.currentPanel && !tc.selectTabPanel(floaterp)) {
            addFloater(floaterp, true, insertionPoint)
        }
    }

    open fun removeFloater(floaterp: Floater?) {
        floaterp ?: return
        if (floaterp.getHost() !== this) return

        if (!hostedFloaterShowTitlebar) {
            floaterp.setDragHandleTitleVisible(true)
            floaterp.growByHeaderHeight()
        }

        val data = floaterDataMap.remove(floaterp)
        if (data != null) {
            floaterp.minimizable = data.canMinimize
            floaterp.saveRect = data.saveRect
            if (!data.canResize) {
                floaterp.reshape(data.width, data.height)
            }
            floaterp.resizable = data.canResize
        }

        tabContainer?.removeTabPanel(floaterp)
        floaterp.setBackgroundVisible(true)
        floaterp.draggable = true
        floaterp.setHost(null)
        floaterp.applyRectControl()
        floaterp.setFollowsNone()

        updateResizeLimits()

        val tabFloater = tabContainer?.currentPanel as? Floater
        if (tabFloater != null) tabOpen(tabFloater, false)
    }

    open fun tabOpen(openedFloater: Floater, fromClick: Boolean) {}

    open fun tabClose() {
        if ((tabContainer?.tabCount ?: 0) == 0) close()
    }

    open fun selectFloater(floaterp: Floater): Boolean =
        tabContainer?.selectTabPanel(floaterp) ?: false

    open fun selectNextFloater() { tabContainer?.selectNextTab() }
    open fun selectPrevFloater() { tabContainer?.selectPrevTab() }

    open fun getActiveFloater(): Floater? = tabContainer?.currentPanel as? Floater

    open fun isFloaterFlashing(floaterp: Floater?): Boolean {
        if (floaterp == null || floaterp.getHost() !== this) return false
        return tabContainer?.getTabPanelFlashing(floaterp) ?: false
    }

    open fun setFloaterFlashing(floaterp: Floater?, flashing: Boolean, alternateColor: Boolean = false) {
        if (floaterp != null && floaterp.getHost() === this) {
            tabContainer?.setTabPanelFlashing(floaterp, flashing, alternateColor)
        }
    }

    open fun closeAllFloaters(): Boolean {
        val tc = tabContainer ?: return true
        var tabToClose = 0
        var lastTabCount = tc.tabCount
        while (tabToClose < tc.tabCount) {
            val first = tc.getPanelByIndex(tabToClose) as? Floater
            first?.close()
            if (lastTabCount == tc.tabCount) {
                tabToClose++
            } else {
                lastTabCount = tc.tabCount
            }
        }
        return tc.tabCount == 0
    }

    fun setTabContainer(tc: TabContainer?) {
        if (tc != null && tabContainer == null) {
            tabContainer = tc
            tc.commitCallback = { onTabSelected() }
        }
    }

    fun onTabSelected() {
        val floaterp = tabContainer?.currentPanel as? Floater ?: return
        tabOpen(floaterp, true)
        if (!hostedFloaterShowTitlebar) {
            TODO("GPU: update drag handle title to '${titleString()} - ${floaterp.title}'")
        }
    }

    open fun updateResizeLimits() {
        var newMinWidth = origMinWidth
        var newMinHeight = origMinHeight
        computeResizeLimits(newMinWidth, newMinHeight)
        setResizeLimits(newMinWidth, newMinHeight)

        val curHeight = currentHeight()
        val newWidth = maxOf(currentWidth(), newMinWidth)
        val newHeight = maxOf(currentHeight(), newMinHeight)

        if (isMinimized) {
            TODO("APR: update expanded rect to fit new min constraints")
        } else {
            reshape(newWidth, newHeight)
            translate(0, curHeight - currentHeight())
            TODO("APR: gFloaterView->adjustToFitScreen(this, true)")
        }
    }

    open fun updateFloaterTitle(floaterp: Floater) {
        val tc = tabContainer ?: return
        val index = tc.getIndexForPanel(floaterp)
        if (index != -1) {
            tc.setPanelTitle(index, floaterp.shortTitle)
            if (!hostedFloaterShowTitlebar && floaterp === tc.currentPanel) {
                TODO("GPU: set drag handle title to '${titleString()} - ${floaterp.title}'")
            }
        }
    }

    fun closeDockedFloater() {
        val floater = getActiveFloater()
        if (floater != null && floater.canClose() && floater.isCloseable()) {
            floater.close()
            if ((tabContainer?.tabCount ?: 0) > 0) {
                tabContainer?.setFocus(true)
            } else {
                close()
            }
            return
        }
        close()
    }

    private fun computeResizeLimits(newMinWidth: Int, newMinHeight: Int) {
        var w = newMinWidth
        var h = newMinHeight
        val tc = tabContainer ?: return
        val floaterHeaderSize = defaultFloaterHeaderHeight()
        val tabcntrHeaderHeight = PANEL_BORDER_WIDTH + tabCloseBtnSize()
        for (i in 0 until tc.tabCount) {
            val fp = tc.getPanelByIndex(i) as? Floater ?: continue
            w = maxOf(w, fp.minWidth + PANEL_BORDER_WIDTH * 2)
            h = maxOf(h, fp.minHeight + floaterHeaderSize + tabcntrHeaderHeight)
        }
    }

    protected open var hostedFloaterShowTitlebar: Boolean = true

    companion object {
        private const val KEY_W: Int = 'W'.code
        private const val MASK_CONTROL: UInt = 0x01u
        private const val RESIZE_HANDLE_WIDTH: Int = 10
        private const val PANEL_BORDER_WIDTH: Int = 1
    }
}

abstract class TabContainer(val name: String, val tabPosition: MultiFloater.TabPosition) {
    abstract val tabCount: Int
    abstract val currentPanel: Any?
    abstract val currentPanelIndex: Int
    var rightTabBtnOffset: Int = 0
    var commitCallback: (() -> Unit)? = null

    abstract fun addTabPanel(panel: Any, label: String, insertionPoint: MultiFloater.InsertionPoint)
    abstract fun removeTabPanel(panel: Any)
    abstract fun selectTabPanel(panel: Any): Boolean
    abstract fun selectTab(index: Int): Boolean
    abstract fun selectNextTab()
    abstract fun selectPrevTab()
    abstract fun selectLastTab()
    abstract fun getIndexForPanel(panel: Any): Int
    abstract fun getPanelByIndex(index: Int): Any?
    abstract fun setPanelTitle(index: Int, title: String)
    abstract fun getTabPanelFlashing(panel: Any): Boolean
    abstract fun setTabPanelFlashing(panel: Any, flashing: Boolean, alternateColor: Boolean = false)
    abstract fun setFocus(focus: Boolean)
}

private fun Floater.getHost(): MultiFloater? { TODO("APR: get hosting MultiFloater") }
private fun Floater.setHost(host: MultiFloater?) { TODO("APR: set hosting MultiFloater") }
private fun Floater.canClose(): Boolean { TODO("APR: check if floater can be closed") }
private fun Floater.isCloseable(): Boolean = closeable
private fun Floater.currentWidth(): Int { TODO("APR: get current floater width") }
private fun Floater.currentHeight(): Int { TODO("APR: get current floater height") }
private fun Floater.reshape(w: Int, h: Int) { TODO("APR: reshape floater to ($w x $h)") }
private fun Floater.translate(dx: Int, dy: Int) { TODO("GPU: translate floater by ($dx, $dy)") }
private fun Floater.storeRectControl() { TODO("APR: store current rect into control variable") }
private fun Floater.applyRectControl() { TODO("APR: apply stored rect from control variable") }
private fun Floater.setBackgroundVisible(vis: Boolean) { TODO("GPU: set floater background visibility to $vis") }
private fun Floater.setDragHandleTitleVisible(vis: Boolean) { TODO("GPU: set drag handle title visible=$vis") }
private fun Floater.shrinkByHeaderHeight() { TODO("GPU: shrink floater rect by header height") }
private fun Floater.growByHeaderHeight() { TODO("GPU: grow floater rect by header height") }
private fun Floater.setFollowsNone() { TODO("APR: clear floater follow flags") }
private fun Floater.getResizeLimits() { TODO("APR: read resize limits from XML params") }
private fun Floater.setResizeLimits(minW: Int, minH: Int) { TODO("APR: set min resize limits to ($minW, $minH)") }
private fun Floater.moveResizeHandlesToFront() { TODO("GPU: move resize handles to front of child list") }
private fun Floater.isResizable(): Boolean = resizable
private fun Floater.setMinimized(min: Boolean) { isMinimized = min }

private val Floater.shortTitle: String get() { TODO("APR: get short title for tab label") }
private val Floater.minWidth: Int get() { TODO("APR: get minimum floater width") }
private val Floater.minHeight: Int get() { TODO("APR: get minimum floater height") }
private val Floater.saveRect: Boolean get() { TODO("APR: get saveRect flag") }
private val Floater.draggable: Boolean get() { TODO("APR: get draggable flag") }
private fun Floater.removeFloater(f: Floater) { TODO("APR: remove floater from this host") }
private fun Floater.titleString(): String = title

private fun defaultFloaterHeaderHeight(): Int { TODO("APR: get default floater header height from params") }
private fun tabCloseBtnSize(): Int { TODO("APR: get UITabCntrCloseBtnSize control value") }
private fun findChildTabContainer(name: String): TabContainer? { TODO("APR: find child TabContainer named '$name'") }
private fun addChildView(view: Any): Boolean { TODO("APR: add view as child") }

private fun Floater.MultiFloater.currentWidth(): Int { TODO("APR: get current width") }
private fun Floater.MultiFloater.currentHeight(): Int { TODO("APR: get current height") }
private fun MultiFloater.reshape(w: Int, h: Int) { TODO("GPU: reshape multifloater to ($w x $h)") }
private fun MultiFloater.translate(dx: Int, dy: Int) { TODO("GPU: translate multifloater by ($dx, $dy)") }
private fun MultiFloater.close(appQuitting: Boolean = false) { TODO("APR: close multifloater") }
private fun MultiFloater.currentWidth(): Int { TODO("APR: get multifloater current width") }
private fun MultiFloater.currentHeight(): Int { TODO("APR: get multifloater current height") }
private fun MultiFloater.moveResizeHandlesToFront() { TODO("GPU: move multifloater resize handles to front") }
private fun MultiFloater.getResizeLimits() { TODO("APR: read multifloater resize limits") }
private fun MultiFloater.setResizeLimits(minW: Int, minH: Int) { TODO("APR: set multifloater min resize limits") }
private fun MultiFloater.isResizable(): Boolean = resizable
private fun MultiFloater.addChildView(view: Any): Boolean { TODO("APR: add child view to multifloater") }
private fun MultiFloater.findChildTabContainer(name: String): TabContainer? {
    TODO("APR: find child TabContainer named '$name'")
}
private fun MultiFloater.titleString(): String = title

var Floater.saveRect: Boolean
    get() = TODO("APR: get floater saveRect")
    set(value) { TODO("APR: set floater saveRect to $value") }

var Floater.draggable: Boolean
    get() = TODO("APR: get floater draggable")
    set(value) { TODO("APR: set floater draggable to $value") }
