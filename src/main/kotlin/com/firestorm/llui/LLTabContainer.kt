package com.firestorm.llui

import com.firestorm.llmath.Rect
import com.firestorm.llcommon.*

open class TabContainer(
    name: String,
    rect: Rect = Rect()
) : Panel(name, rect) {

    enum class TabPosition { TOP, BOTTOM, LEFT }

    enum class InsertionPoint { START, END, LEFT_OF_CURRENT, RIGHT_OF_CURRENT }

    data class Tab(
        val panel: Panel,
        var label: String,
        var enabled: Boolean = true,
        var visible: Boolean = true,
        var flashing: Boolean = false
    )

    private val tabs: MutableList<Tab> = mutableListOf()

    var currentTab: Int = -1
        private set

    var tabPosition: TabPosition = TabPosition.TOP
    var minTabWidth: Int = 0
    var maxTabWidth: Int = Int.MAX_VALUE
    var tabHeight: Int = 20
    var tabsHidden: Boolean = false
    var allowRearrange: Boolean = false

    var onTabChanged: ((TabContainer, Int) -> Unit)? = null
    var onTabRearranged: ((TabContainer, Int, Panel) -> Unit)? = null

    fun addTabPanel(
        panel: Panel,
        label: String = panel.name,
        selectTab: Boolean = false,
        insertAt: InsertionPoint = InsertionPoint.END
    ) {
        val tab = Tab(panel, label)
        addChild(panel)
        panel.visible = false
        when (insertAt) {
            InsertionPoint.START -> tabs.add(0, tab)
            InsertionPoint.END -> tabs.add(tab)
            InsertionPoint.LEFT_OF_CURRENT -> {
                val pos = currentTab.coerceAtLeast(0)
                tabs.add(pos, tab)
                if (currentTab >= pos) currentTab++
            }
            InsertionPoint.RIGHT_OF_CURRENT -> {
                val pos = (currentTab + 1).coerceIn(0, tabs.size)
                tabs.add(pos, tab)
                if (currentTab >= pos) currentTab++
            }
        }
        if (selectTab || currentTab < 0) {
            selectTab(tabs.indexOf(tab))
        }
    }

    fun removeTabPanel(panel: Panel) {
        val idx = tabs.indexOfFirst { it.panel == panel }
        if (idx < 0) return
        tabs.removeAt(idx)
        removeChild(panel)
        when {
            tabs.isEmpty() -> currentTab = -1
            currentTab >= tabs.size -> selectTab(tabs.size - 1)
            currentTab == idx -> selectTab(currentTab)
        }
    }

    fun selectTab(index: Int): Boolean {
        if (index < 0 || index >= tabs.size) return false
        if (!tabs[index].enabled) return false
        if (currentTab >= 0 && currentTab < tabs.size) {
            tabs[currentTab].panel.visible = false
        }
        currentTab = index
        tabs[currentTab].panel.visible = true
        onTabChanged?.invoke(this, currentTab)
        return true
    }

    fun selectFirstTab(): Boolean = selectTab(0)

    fun selectLastTab(): Boolean = selectTab(tabs.size - 1)

    fun selectNextTab(): Boolean = selectTab(currentTab + 1)

    fun selectPrevTab(): Boolean = selectTab(currentTab - 1)

    fun selectTabPanel(panel: Panel): Boolean {
        val idx = tabs.indexOfFirst { it.panel == panel }
        return if (idx >= 0) selectTab(idx) else false
    }

    fun selectTabByName(label: String): Boolean {
        val idx = tabs.indexOfFirst { it.label == label }
        return if (idx >= 0) selectTab(idx) else false
    }

    fun getTabCount(): Int = tabs.size

    fun getCurrentPanel(): Panel? =
        if (currentTab in tabs.indices) tabs[currentTab].panel else null

    fun getPanelByIndex(index: Int): Panel? =
        if (index in tabs.indices) tabs[index].panel else null

    fun getIndexForPanel(panel: Panel): Int =
        tabs.indexOfFirst { it.panel == panel }

    fun getPanelIndexByTitle(title: String): Int =
        tabs.indexOfFirst { it.label == title }

    fun getPanelTitle(index: Int): String =
        if (index in tabs.indices) tabs[index].label else ""

    fun setPanelTitle(index: Int, title: String) {
        if (index in tabs.indices) tabs[index] = tabs[index].copy(label = title)
    }

    fun enableTabButton(index: Int, enable: Boolean) {
        if (index in tabs.indices) tabs[index] = tabs[index].copy(enabled = enable)
    }

    fun setTabVisibility(panel: Panel, visible: Boolean) {
        val idx = tabs.indexOfFirst { it.panel == panel }
        if (idx >= 0) tabs[idx] = tabs[idx].copy(visible = visible)
    }

    fun setTabPanelFlashing(panel: Panel, state: Boolean) {
        val idx = tabs.indexOfFirst { it.panel == panel }
        if (idx >= 0) tabs[idx] = tabs[idx].copy(flashing = state)
    }

    fun getTabPanelFlashing(panel: Panel): Boolean =
        tabs.firstOrNull { it.panel == panel }?.flashing ?: false

    fun deleteAllTabs() {
        for (tab in tabs) removeChild(tab.panel)
        tabs.clear()
        currentTab = -1
    }

    override fun draw() {
        if (!visible) return
        if (!tabsHidden) {
            TODO("GL: draw tab buttons for ${tabs.size} tabs at position $tabPosition")
        }
        getCurrentPanel()?.draw()
    }
}
