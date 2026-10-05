package com.firestorm.newview

import java.util.UUID

interface LLSearchableControl {
    fun getHighlighted(): Boolean
    fun setHighlighted(highlighted: Boolean)
}

interface LLViewSearchable {
    fun getVisible(): Boolean
}

interface LLPanelSearchable : LLViewSearchable
interface LLTabContainerSearchable : LLViewSearchable {
    fun setTabVisibility(panel: LLPanelSearchable?, visible: Boolean)
}

interface LLMenuItemGLSearchable : LLViewSearchable {
    fun setVisible(visible: Boolean)
}

object LLPrefsSearch {

    open class SearchableItem {
        var label: String = ""
        var view: LLViewSearchable? = null
        var ctrl: LLSearchableControl? = null
        val children: MutableList<SearchableItem> = mutableListOf()

        fun setNotHighlighted() {
            ctrl?.setHighlighted(false)
        }

        open fun highlightAndHide(filter: String): Boolean {
            val c = ctrl ?: return false
            if (c.getHighlighted()) return true

            val v = view
            if (v != null && !v.getVisible()) return false

            if (filter.isEmpty()) {
                c.setHighlighted(false)
                return true
            }

            if (label.contains(filter)) {
                c.setHighlighted(true)
                return true
            }

            return false
        }
    }

    open class PanelData {
        var panel: LLPanelSearchable? = null
        var label: String = ""
        val children: MutableList<SearchableItem> = mutableListOf()
        val childPanels: MutableList<PanelData> = mutableListOf()

        fun setNotHighlighted() {
            children.forEach { it.setNotHighlighted() }
            childPanels.forEach { it.setNotHighlighted() }
        }

        open fun highlightAndHide(filter: String): Boolean {
            children.forEach { it.setNotHighlighted() }
            childPanels.forEach { it.setNotHighlighted() }

            var visible = false
            children.forEach { visible = visible or it.highlightAndHide(filter) }
            childPanels.forEach { visible = visible or it.highlightAndHide(filter) }

            return visible
        }
    }

    class TabContainerData : PanelData() {
        var tabContainer: LLTabContainerSearchable? = null

        override fun highlightAndHide(filter: String): Boolean {
            children.forEach { it.setNotHighlighted() }

            var visible = false
            children.forEach { visible = visible or it.highlightAndHide(filter) }
            childPanels.forEach { child ->
                val panelVisible = child.highlightAndHide(filter)
                tabContainer?.setTabVisibility(child.panel, panelVisible)
                visible = visible or panelVisible
            }

            return visible
        }
    }

    class SearchData {
        var rootTab: TabContainerData? = null
        var lastFilter: String = ""
    }
}

object LLStatusbarSearch {

    class SearchableItem {
        var label: String = ""
        var menu: LLMenuItemGLSearchable? = null
        val children: MutableList<SearchableItem> = mutableListOf()
        var ctrl: LLSearchableControl? = null
        var wasHiddenBySearch: Boolean = false

        fun setNotHighlighted() {
            children.forEach { it.setNotHighlighted() }

            val c = ctrl ?: return
            c.setHighlighted(false)
            if (wasHiddenBySearch) {
                menu?.setVisible(true)
                wasHiddenBySearch = false
            }
        }

        fun highlightAndHide(filter: String, hide: Boolean = true): Boolean {
            val m = menu
            // Tear-off menu items and items already hidden by user (not search) are never shown.
            if (m != null && !m.getVisible() && !wasHiddenBySearch) return false

            setNotHighlighted()

            if (filter.isEmpty()) {
                ctrl?.setHighlighted(false)
                return true
            }

            var highlighted = !hide
            if (label.contains(filter)) {
                ctrl?.setHighlighted(true)
                highlighted = true
            }

            var childVisible = false
            children.forEach { childVisible = childVisible or it.highlightAndHide(filter, !highlighted) }

            if (ctrl != null && !childVisible && !highlighted) {
                wasHiddenBySearch = true
                menu?.setVisible(false)
            }

            return childVisible || highlighted
        }
    }

    class SearchData {
        var rootMenu: SearchableItem? = null
        var lastFilter: String = ""
    }
}
