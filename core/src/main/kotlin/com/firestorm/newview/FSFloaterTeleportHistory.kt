/**
 * FSFloaterTeleportHistory.kt
 * Kotlin conversion of fsfloaterteleporthistory.h / fsfloaterteleporthistory.cpp
 *
 * Standalone teleport-history floater for the Firestorm viewer.
 *
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLSD

/**
 * Standalone teleport-history floater.
 *
 * Wraps [TeleportHistoryPanel] (the same panel used inside the Places side-tray)
 * as an independent, resizable floater.  Adds a filter-editor and gear/sorting
 * menu-buttons on top of the panel's built-in controls.
 *
 * Mirrors [FSFloaterTeleportHistory] from `fsfloaterteleporthistory.h`.
 *
 * @param seed LLSD key passed by the floater registry on construction.
 */
class FSFloaterTeleportHistory(val seed: LLSD) {

    // ------------------------------------------------------------------
    // Child widget references — populated in [postBuild]
    // ------------------------------------------------------------------

    private var historyPanel: TeleportHistoryPanel? = null

    /** Filter editor that drives [TeleportHistoryPanel.onSearchEdit]. */
    private var filterEditor: FilterEditor? = null

    /** Gear button that opens the per-selection context menu. */
    private var gearMenuButton: MenuButton? = null

    /** Sorting button that opens the sort-order menu. */
    private var sortingMenuButton: MenuButton? = null

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Called after the floater's XML children have been inflated.
     *
     * Constructs a [TeleportHistoryPanel], wires its action buttons to the
     * panel's handlers, attaches the filter editor, and embeds the panel into
     * the `history_placeholder` container.
     *
     * @return `true` on success; `false` if the panel could not be created.
     */
    fun postBuild(): Boolean {
        val panel = TeleportHistoryPanel()
        historyPanel = panel

        panel.isStandAlone = true

        panel.teleportBtn    = getChild<Button>("teleport_btn")
        panel.showOnMapBtn   = getChild<Button>("map_btn")
        panel.showProfileBtn = getChild<Button>("profile_btn")

        panel.teleportBtn?.setClickedCallback    { panel.onTeleport() }
        panel.showProfileBtn?.setClickedCallback { panel.onShowProfile() }
        panel.showOnMapBtn?.setClickedCallback   { panel.onShowOnMap() }

        filterEditor = getChild<FilterEditor>("Filter")?.also { editor ->
            // Committing on focus-lost detaches list items and breaks selection,
            // so we suppress it and rely on keystroke commits only.
            editor.commitOnFocusLost = false
            editor.setCommitCallback { searchString -> onFilterEdit(searchString, forceFilter = false) }
        }

        getChild<View>("history_placeholder")?.addChild(panel)
        panel.onSearchEdit("")

        gearMenuButton = getChild<MenuButton>("options_gear_btn")?.also { btn ->
            btn.setMouseDownCallback { onGearMenuClick() }
        }

        sortingMenuButton = getChild<MenuButton>("sorting_menu_btn")?.also { btn ->
            btn.setMouseDownCallback { onSortingMenuClick() }
        }

        return historyPanel != null
    }

    /**
     * Returns `true` so the floater participates in the global accelerator chain,
     * forwarding Ctrl+F (and similar combos) to the filter editor.
     */
    fun hasAccelerators(): Boolean = true

    /**
     * Intercepts key-combos and redirects filter-editor activation keys.
     * Mirrors `FSFloaterTeleportHistory::handleKeyHere`.
     */
    fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (isFilterEditorKeyCombo(key, mask)) {
            filterEditor?.setFocus(true)
            return true
        }
        return super_handleKeyHere(key, mask)
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Clear the filter editor and reset the history list to show all entries.
     */
    fun resetFilter() {
        filterEditor?.clear()
        onFilterEdit("", forceFilter = true)
    }

    // ------------------------------------------------------------------
    // Private callbacks
    // ------------------------------------------------------------------

    /**
     * Called when the filter editor commits a new search string.
     *
     * Delegates to [TeleportHistoryPanel.onSearchEdit] only when the string
     * actually differs from the current filter, or when [forceFilter] is set.
     *
     * @param searchString New filter substring (case-sensitive — the panel
     *        handles case-folding internally for keyword matching).
     * @param forceFilter  When `true`, always forward even if unchanged.
     */
    private fun onFilterEdit(searchString: String, forceFilter: Boolean) {
        val panel = historyPanel ?: return
        if (forceFilter || panel.filterString != searchString) {
            panel.onSearchEdit(searchString)
        }
    }

    /**
     * Called when the gear menu-button is pressed.
     * Attaches the panel's selection context menu to the button.
     */
    private fun onGearMenuClick() {
        gearMenuButton?.setMenu(historyPanel?.getSelectionMenu(), MenuButton.Placement.BOTTOM_LEFT)
    }

    /**
     * Called when the sorting menu-button is pressed.
     * Attaches the panel's sort-order menu to the button.
     */
    private fun onSortingMenuClick() {
        sortingMenuButton?.setMenu(historyPanel?.getSortingMenu(), MenuButton.Placement.BOTTOM_LEFT)
    }

    // ------------------------------------------------------------------
    // Stubs for framework calls that require the LL UI/platform layer
    // ------------------------------------------------------------------

    private fun <T> getChild(name: String): T? {
        System.err.println("FSFloaterTeleportHistory: getChild not yet implemented")
        return null
    }

    private fun super_handleKeyHere(key: Int, mask: Int): Boolean {
        System.err.println("FSFloaterTeleportHistory: super_handleKeyHere not yet implemented")
        return false
    }

    private fun isFilterEditorKeyCombo(key: Int, mask: Int): Boolean {
        System.err.println("FSFloaterTeleportHistory: isFilterEditorKeyCombo not yet implemented")
        return false
    }

    // ------------------------------------------------------------------
    // Nested stubs for child types referenced above
    // ------------------------------------------------------------------

    /** Stub: LL filter-editor widget. */
    class FilterEditor {
        var commitOnFocusLost: Boolean = true
        fun setFocus(focus: Boolean) { System.err.println("FilterEditor: setFocus not yet implemented") }
        fun clear() { System.err.println("FilterEditor: clear not yet implemented") }
        fun setCommitCallback(cb: (String) -> Unit) { System.err.println("FilterEditor: setCommitCallback not yet implemented") }
    }

    /** Stub: LL menu-button widget. */
    class MenuButton {
        enum class Placement { BOTTOM_LEFT, BOTTOM_RIGHT }
        fun setMouseDownCallback(cb: () -> Unit) { System.err.println("MenuButton: setMouseDownCallback not yet implemented") }
        fun setMenu(menu: Any?, placement: Placement) { System.err.println("MenuButton: setMenu not yet implemented") }
    }

    /** Stub: generic clickable button. */
    class Button {
        fun setClickedCallback(cb: () -> Unit) { System.err.println("Button: setClickedCallback not yet implemented") }
    }

    /** Stub: generic view/container. */
    class View {
        fun addChild(child: Any) { System.err.println("View: addChild not yet implemented") }
    }

    /**
     * Stub: mirror of LLTeleportHistoryPanel.
     * The real class lives in llpanelteleporthistory.h.
     */
    class TeleportHistoryPanel {
        var isStandAlone: Boolean = false
        var teleportBtn: Button? = null
        var showOnMapBtn: Button? = null
        var showProfileBtn: Button? = null
        var filterString: String = ""

        fun onTeleport()    { System.err.println("TeleportHistoryPanel: onTeleport not yet implemented") }
        fun onShowProfile() { System.err.println("TeleportHistoryPanel: onShowProfile not yet implemented") }
        fun onShowOnMap()   { System.err.println("TeleportHistoryPanel: onShowOnMap not yet implemented") }
        fun onSearchEdit(filter: String) { System.err.println("TeleportHistoryPanel: onSearchEdit not yet implemented") }
        fun getSelectionMenu(): Any {
            System.err.println("TeleportHistoryPanel: getSelectionMenu not yet implemented")
            return Any()
        }
        fun getSortingMenu(): Any   {
            System.err.println("TeleportHistoryPanel: getSortingMenu not yet implemented")
            return Any()
        }
    }
}
