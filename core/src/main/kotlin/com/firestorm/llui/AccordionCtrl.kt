package com.firestorm.llui

import kotlin.math.max
import kotlin.math.min

private const val BORDER_MARGIN = 2
private const val PARENT_BORDER_MARGIN = 5
private const val VERTICAL_MULTIPLE = 16
private const val MIN_AUTO_SCROLL_RATE = 120f
private const val MAX_AUTO_SCROLL_RATE = 500f
private const val AUTO_SCROLL_RATE_ACCEL = 120f

class AccordionCtrl(
    singleExpansion: Boolean = false,
    fitParent: Boolean = false,
    noMatchedTabsText: String = "",
    noVisibleTabsText: String = ""
) {

    abstract class TabComparator {
        abstract fun compare(tab1: AccordionCtrlTab, tab2: AccordionCtrlTab): Boolean
    }

    companion object {
        private val pendingArrange: MutableSet<AccordionCtrl> = mutableSetOf()

        fun updateClass() {
            for (inst in pendingArrange) {
                inst.arrangePending = false
                inst.arrange()
            }
            pendingArrange.clear()
        }
    }

    private val accordionTabs: MutableList<AccordionCtrlTab> = mutableListOf()

    private var innerRectHeight: Int = 0
    private var scrollbarVisible: Boolean = false
    private var scrollbarDocPos: Int = 0

    var singleExpansion: Boolean = singleExpansion
    val fitParent: Boolean = fitParent

    private var autoScrolling: Boolean = false
    private var autoScrollRate: Float = MIN_AUTO_SCROLL_RATE

    private var noVisibleTabsHelpTextVisible: Boolean = false

    var skipScrollToChild: Boolean = false
    private var arrangePending: Boolean = false

    private val noMatchedTabsOrigString: String = noMatchedTabsText
    private val noVisibleTabsOrigString: String = noVisibleTabsText

    var selectedTab: AccordionCtrlTab? = null
        private set

    private var tabComparator: TabComparator? = null

    fun setComparator(comp: TabComparator) {
        tabComparator = comp
    }

    private var rectWidth: Int = 0
    private var rectHeight: Int = 0

    fun postBuild(): Boolean {
        arrange()

        if (singleExpansion && accordionTabs.isNotEmpty()) {
            if (!accordionTabs[0].displayChildren) accordionTabs[0].displayChildren = true
            for (i in 1 until accordionTabs.size) {
                if (accordionTabs[i].displayChildren) accordionTabs[i].displayChildren = false
            }
        }

        updateNoTabsHelpTextVisibility()
        return true
    }

    fun draw(frameDeltaTime: Float) {
        if (autoScrolling) {
            autoScrollRate = min(autoScrollRate + frameDeltaTime * AUTO_SCROLL_RATE_ACCEL, MAX_AUTO_SCROLL_RATE)
        } else {
            autoScrollRate = MIN_AUTO_SCROLL_RATE
        }
        autoScrolling = false
        // no-op
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        rectWidth = width
        rectHeight = height
        scheduleArrange()
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        // no-op
        return false
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (scrollbarVisible) {
            return true
        }
        return false
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (scrollbarVisible) return true
        return false
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: Int, cargoData: Any?,
        tooltipMsg: StringBuilder
    ): Boolean {
        autoScroll(x, y)
        return true
    }

    fun addCollapsibleCtrl(tab: AccordionCtrlTab) {
        accordionTabs.add(tab)
        val tabIndex = (accordionTabs.size - 1).toShort()
        tab.onDropDownStateChanged = { onCollapseCtrlCloseOpen(tabIndex) }
        scheduleArrange()
    }

    fun removeCollapsibleCtrl(tab: AccordionCtrlTab) {
        accordionTabs.remove(tab)
        if (selectedTab === tab) selectedTab = null
    }

    fun arrange() {
        updateNoTabsHelpTextVisibility()

        if (accordionTabs.isEmpty()) return

        if (accordionTabs.size == 1) {
            val panelTop = rectHeight - BORDER_MARGIN
            val panelWidth = rectWidth - 4
            val accordion = accordionTabs[0]
            val panelHeight = if (accordion.fitPanel) accordion.rectHeight else rectHeight - BORDER_MARGIN * 2
            ctrlSetLeftTopAndSize(accordion, accordion.rectLeft, panelTop, panelWidth, panelHeight)
            showHideScrollbar(rectWidth, rectHeight)
            return
        }

        if (singleExpansion) arrangeSingle() else arrangeMultiple()
    }

    fun onScrollPosChangeCallback(pos: Int) {
        updateLayout(rectWidth, rectHeight)
    }

    fun onUpdateScrollToChild(ctrl: UICtrlStub) {
        if (scrollbarVisible && !skipScrollToChild) {
            // no-op
        }
    }

    fun onOpen(key: Any) {
        for (tab in accordionTabs) {
            tab.accordionView?.let { it as? PanelStub }?.onOpen(key)
        }
    }

    fun notifyParent(info: Map<String, Any>): Int {
        val action = info["action"] as? String
        if (action != null) {
            when (action) {
                "size_changes" -> {
                    scheduleArrange()
                    return 1
                }
                "select_next" -> {
                    for (i in accordionTabs.indices) {
                        if (accordionTabs[i].hasFocus()) {
                            var j = i + 1
                            while (j < accordionTabs.size) {
                                if (accordionTabs[j].visible) break
                                j++
                            }
                            if (j < accordionTabs.size) {
                                accordionTabs[j].notify(mapOf("action" to "select_first"))
                                return 1
                            }
                            break
                        }
                    }
                    return 0
                }
                "select_prev" -> {
                    for (i in accordionTabs.indices) {
                        if (accordionTabs[i].hasFocus() && i > 0) {
                            var j = i - 1
                            var found = false
                            while (j >= 0) {
                                if (accordionTabs[j].visible) { found = true; break }
                                j--
                            }
                            if (found) {
                                accordionTabs[j].notify(mapOf("action" to "select_last"))
                                return 1
                            }
                            break
                        }
                    }
                    return 0
                }
                "select_current" -> {
                    for (tab in accordionTabs) {
                        if (tab.hasFocus()) {
                            if (tab !== selectedTab) {
                                selectedTab?.setSelected(false)
                                selectedTab = tab
                                tab.setSelected(true)
                            }
                            return 1
                        }
                    }
                    return 0
                }
                "deselect_current" -> {
                    if (selectedTab != null) {
                        selectedTab!!.setSelected(false)
                        selectedTab = null
                        return 1
                    }
                    return 0
                }
            }
        } else if (info.containsKey("scrollToShowRect")) {
            // no-op
        } else if (info.containsKey("child_visibility_change")) {
            val newVisibility = info["child_visibility_change"] as? Boolean ?: false
            if (newVisibility) {
                noVisibleTabsHelpTextVisible = false
            } else {
                updateNoTabsHelpTextVisibility()
            }
        }
        return 0
    }

    fun reset() {
        scrollbarDocPos = 0
    }

    fun expandDefaultTab() {
        if (accordionTabs.isEmpty()) return
        val first = accordionTabs.first()
        if (!first.displayChildren) first.displayChildren = true
        for (i in 1 until accordionTabs.size) {
            if (accordionTabs[i].displayChildren) accordionTabs[i].displayChildren = false
        }
        arrange()
    }

    fun sort() {
        val comp = tabComparator ?: return
        accordionTabs.sortWith { a, b -> if (comp.compare(a, b)) -1 else 1 }
        arrange()
    }

    fun collapseAllTabs() {
        if (accordionTabs.isEmpty()) return
        for (tab in accordionTabs) {
            if (tab.displayChildren) tab.displayChildren = false
        }
        arrange()
    }

    fun setFilterSubString(filterString: String) {
        val text = if (filterString.isEmpty()) noVisibleTabsOrigString else noMatchedTabsOrigString
        System.err.println("AccordionCtrl: setFilterSubString not yet implemented")
    }

    fun getExpandedTab(): AccordionCtrlTab? = accordionTabs.firstOrNull { it.isExpanded }

    fun scheduleArrange() {
        if (!arrangePending) {
            arrangePending = true
            pendingArrange.add(this)
        }
    }

    private fun updateNoTabsHelpTextVisibility() {
        val anyVisible = accordionTabs.any { it.visible }
        noVisibleTabsHelpTextVisible = !anyVisible
    }

    private fun arrangeSingle() {
        val panelLeft = BORDER_MARGIN
        var panelTop = rectHeight - BORDER_MARGIN
        val panelWidth = rectWidth - 4

        var collapsedHeight = 0
        for (tab in accordionTabs) {
            if (!tab.visible) continue
            if (!tab.isExpanded) collapsedHeight += tab.rectHeight
        }

        val expandedHeight = rectHeight - BORDER_MARGIN - collapsedHeight

        for (tab in accordionTabs) {
            if (!tab.visible) continue
            val panelHeight = if (!tab.isExpanded) {
                tab.rectHeight
            } else {
                if (fitParent) expandedHeight
                else tab.accordionView?.let { it.rectHeight + tab.headerHeight + BORDER_MARGIN * 2 } ?: tab.rectHeight
            }
            val clampedHeight = max(panelHeight, tab.headerHeight)
            ctrlSetLeftTopAndSize(tab, panelLeft, panelTop, panelWidth, clampedHeight)
            panelTop -= tab.rectHeight
        }

        showHideScrollbar(rectWidth, rectHeight)
        updateLayout(rectWidth, rectHeight)
    }

    private fun arrangeMultiple() {
        val panelLeft = BORDER_MARGIN
        var panelTop = rectHeight - BORDER_MARGIN
        val panelWidth = rectWidth - 4

        for (i in accordionTabs.indices) {
            val tab = accordionTabs[i]
            if (!tab.visible) continue

            if (!tab.isExpanded) {
                ctrlSetLeftTopAndSize(tab, panelLeft, panelTop, panelWidth, tab.rectHeight)
                panelTop -= tab.rectHeight
            } else {
                var panelHeight = tab.rectHeight
                if (fitParent) {
                    panelHeight = calcExpandedTabHeight(i, panelTop)
                    ctrlSetLeftTopAndSize(tab, panelLeft, panelTop, panelWidth, panelHeight)
                    val optimalHeight = (tab.accordionView?.requiredRectHeight ?: 0) +
                        tab.headerHeight + 2 * BORDER_MARGIN
                    if (optimalHeight < panelHeight) panelHeight = optimalHeight
                    if (tab.headerHeight > panelHeight) panelHeight = tab.headerHeight
                }
                ctrlSetLeftTopAndSize(tab, panelLeft, panelTop, panelWidth, panelHeight)
                panelTop -= panelHeight
            }
        }

        showHideScrollbar(rectWidth, rectHeight)
        updateLayout(rectWidth, rectHeight)
    }

    private fun calcRequiredHeight(): Int {
        var reqHeight = 0
        for (tab in accordionTabs) {
            if (tab.visible) reqHeight += tab.rectHeight
        }
        innerRectHeight = reqHeight + BORDER_MARGIN * 2 + BORDER_MARGIN
        return innerRectHeight
    }

    private fun calcExpandedTabHeight(tabIndex: Int, availableHeight: Int): Int {
        if (tabIndex < 0) return availableHeight
        var collapsedTabsHeight = 0
        var numExpanded = 0
        for (tab in accordionTabs) {
            if (!tab.isExpanded) collapsedTabsHeight += tab.headerHeight
            else numExpanded++
        }
        if (numExpanded == 0) return availableHeight
        return (availableHeight - collapsedTabsHeight - BORDER_MARGIN) / numExpanded
    }

    private fun showHideScrollbar(width: Int, height: Int) {
        calcRequiredHeight()
        if (innerRectHeight > height) showScrollbar(width, height)
        else hideScrollbar(width, height)
    }

    private fun showScrollbar(width: Int, height: Int) {
        scrollbarVisible = true
    }

    private fun hideScrollbar(width: Int, height: Int) {
        if (!scrollbarVisible) return
        scrollbarVisible = false
        val panelWidth = width - 2 * BORDER_MARGIN
        for (tab in accordionTabs) {
            ctrlSetLeftTopAndSize(tab, tab.rectLeft, tab.rectTop, panelWidth, tab.rectHeight)
        }
        scrollbarDocPos = 0
        if (accordionTabs.isNotEmpty()) {
            val panelTop = height - BORDER_MARGIN
            val diff = panelTop - accordionTabs[0].rectTop
            shiftAccordionTabs(0, diff)
        }
    }

    private fun updateLayout(width: Int, height: Int) {
        var panelTop = height - BORDER_MARGIN
        if (scrollbarVisible) panelTop += scrollbarDocPos
        val panelWidth = width - BORDER_MARGIN * 2

        for (tab in accordionTabs) {
            if (!tab.visible) continue
            ctrlSetLeftTopAndSize(tab, tab.rectLeft, panelTop, panelWidth, tab.rectHeight)
            panelTop -= tab.rectHeight
        }
    }

    private fun autoScroll(x: Int, y: Int): Boolean {
        if (!scrollbarVisible) return false
        val autoScrollRegionHeight = min(rectHeight / 3, 10)
        val autoScrollSpeed = (autoScrollRate * (1f / 60f)).toInt()

        var scrolling = false
        if (y <= autoScrollRegionHeight && scrollbarDocPos < innerRectHeight - rectHeight - 1) {
            scrollbarDocPos += autoScrollSpeed
            autoScrolling = true
            scrolling = true
        }
        if (y >= rectHeight - autoScrollRegionHeight && scrollbarDocPos > 0) {
            scrollbarDocPos -= autoScrollSpeed
            autoScrolling = true
            scrolling = true
        }
        return scrolling
    }

    private fun ctrlSetLeftTopAndSize(tab: AccordionCtrlTab, left: Int, top: Int, width: Int, height: Int) {
        tab.rectLeft = left
        tab.rectTop = top
        tab.reshape(width, height)
    }

    private fun ctrlShiftVertical(tab: AccordionCtrlTab, delta: Int) {
        tab.rectTop += delta
    }

    private fun shiftAccordionTabs(fromIndex: Int, delta: Int) {
        for (i in fromIndex until accordionTabs.size) {
            ctrlShiftVertical(accordionTabs[i], delta)
        }
    }

    private fun onCollapseCtrlCloseOpen(panelNum: Short) {
        if (singleExpansion) {
            for (i in accordionTabs.indices) {
                if (i == panelNum.toInt()) continue
                if (accordionTabs[i].displayChildren) accordionTabs[i].displayChildren = false
            }
        }
        arrange()
    }
}

interface UICtrlStub
interface PanelStub {
    fun onOpen(key: Any)
}
