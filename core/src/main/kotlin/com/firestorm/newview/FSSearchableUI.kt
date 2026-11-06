package com.firestorm.newview

// Host-side UI stubs — real implementations live in the UI layer.
interface LLView {
    fun getVisible(): Boolean
}

interface LLPanel : LLView
interface LLTabContainer : LLView {
    fun setTabVisibility(panel: LLPanel?, visible: Boolean)
}

interface LLMenuItemGL : LLView {
    fun setVisible(visible: Boolean)
}

interface FSSearchableControl {
    fun getHighlighted(): Boolean
    fun setHighlighted(highlighted: Boolean)
}

object NdPrefs {

    open class SearchableItem {
        var label: String = ""
        var view: LLView? = null
        var ctrl: FSSearchableControl? = null
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

            if (label.contains(filter, ignoreCase = false)) {
                c.setHighlighted(true)
                return true
            }

            return false
        }
    }

    open class PanelData {
        var panel: LLPanel? = null
        var label: String = ""
        val children: MutableList<SearchableItem> = mutableListOf()
        val childPanels: MutableList<PanelData> = mutableListOf()

        open fun highlightAndHide(filter: String): Boolean {
            children.forEach { it.setNotHighlighted() }

            var visible = false
            children.forEach { visible = visible or it.highlightAndHide(filter) }
            childPanels.forEach { visible = visible or it.highlightAndHide(filter) }

            return visible
        }
    }

    class TabContainerData : PanelData() {
        var tabContainer: LLTabContainer? = null

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

object NdStatusbar {

    class SearchableItem {
        var label: String = ""
        var menu: LLMenuItemGL? = null
        val children: MutableList<SearchableItem> = mutableListOf()
        var ctrl: FSSearchableControl? = null
        var wasHiddenBySearch: Boolean = false

        fun setNotHighlighted() {
            children.forEach { it.setNotHighlighted() }

            val c = ctrl ?: return
            c.setHighlighted(false)
            if (wasHiddenBySearch) {
                menu?.setVisible(true)
            }
        }

        fun highlightAndHide(filter: String): Boolean {
            val m = menu
            if (m != null && !m.getVisible() && !wasHiddenBySearch) return false

            setNotHighlighted()

            var visible = false
            children.forEach { visible = visible or it.highlightAndHide(filter) }

            if (filter.isEmpty()) {
                ctrl?.setHighlighted(false)
                return true
            }

            if (label.contains(filter, ignoreCase = false)) {
                ctrl?.setHighlighted(true)
                return true
            }

            val c = ctrl
            if (c != null && !visible) {
                wasHiddenBySearch = true
                menu?.setVisible(false)
            }

            return visible
        }
    }

    class SearchData {
        var rootMenu: SearchableItem? = null
        var lastFilter: String = ""
    }
}
