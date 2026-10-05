/**
 * SideTray.kt
 * Converted from: indra/newview/llsidetraypanelcontainer.h / .cpp
 * Original: LLSideTrayPanelContainer — tab-container subclass that acts as a
 * collapsible side-panel host with invisible tabs and navigation history.
 *
 * Note: The upstream Firestorm viewer no longer ships a full llsidetray.h/cpp
 * (the side-tray was removed in LL viewer 3.x era).  The closest surviving
 * class is LLSideTrayPanelContainer, which this file ports together with a
 * lightweight SideTray singleton that captures the panel-container API.
 *
 * Copyright (C) 2010, Linden Research, Inc. (LGPL 2.1)
 * Kotlin port: Firestorm-kotlin project.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLSD
import com.firestorm.llui.LLPanel
import com.firestorm.llui.LLTabContainer

// ---------------------------------------------------------------------------
// SideTrayPanelContainer
// ---------------------------------------------------------------------------

/**
 * A tab-container derivative that keeps its tabs hidden and provides a simple
 * panel-switch API driven by LLSD keys.  Mirrors `LLSideTrayPanelContainer`.
 *
 * Panels are switched by calling [onOpen] with an LLSD map that contains the
 * key [PARAM_SUB_PANEL_NAME] →  name-of-panel-to-activate.  The class also
 * tracks a one-level "previous panel" history so that [openPreviousPanel] can
 * return to a sensible default.
 *
 * @param defaultPanelName Name of the panel shown when [openPreviousPanel] is
 *                         called and no other history exists.  May be empty.
 */
class SideTrayPanelContainer(
    private val defaultPanelName: String = ""
) : LLTabContainer() {

    // Navigation history: panel name → index of the panel that was open before it.
    private val panelHistory: MutableMap<String, Int> = mutableMapOf()

    companion object {
        /** LLSD key used to specify which sub-panel to open. */
        const val PARAM_SUB_PANEL_NAME: String = "sub_panel_name"
    }

    // ------------------------------------------------------------------
    // Core panel-switching API
    // ------------------------------------------------------------------

    /**
     * Opens the sub-panel named by `key[PARAM_SUB_PANEL_NAME]`.
     * If the key is absent the currently-selected panel is re-opened.
     *
     * Mirrors `LLSideTrayPanelContainer::onOpen`.
     */
    fun onOpen(key: LLSD) {
        if (key.has(PARAM_SUB_PANEL_NAME)) {
            val panelName = key[PARAM_SUB_PANEL_NAME].asString()
            selectTabByName(panelName)
        }
        // Re-open current panel regardless (passes key through for panel init).
        currentPanel?.onOpen(key)
    }

    /**
     * Convenience wrapper: builds the LLSD and delegates to [onOpen].
     *
     * @param panelName Name of the panel to open.
     * @param key       Additional parameters forwarded to the panel's `onOpen`.
     */
    fun openPanel(panelName: String, key: LLSD = LLSD.emptyMap()) {
        val combined = key.toMutableMap()
        combined[PARAM_SUB_PANEL_NAME] = LLSD.fromString(panelName)
        onOpen(LLSD.fromMap(combined))
    }

    /**
     * Returns to the default panel (or tab 0 when no default is configured).
     * Mirrors `LLSideTrayPanelContainer::openPreviousPanel`.
     */
    fun openPreviousPanel() {
        if (defaultPanelName.isNotEmpty()) {
            selectTabByName(defaultPanelName)
        } else {
            selectTab(0)
        }
    }

    /**
     * Key-press handler.  Deliberately does NOT process Alt+Left/Right so that
     * the tab-switching shortcut of the parent [LLTabContainer] is suppressed.
     * Other keys are forwarded to the [LLPanel] base handler.
     */
    override fun handleKeyHere(key: Int, mask: Int): Boolean =
        super.handlePanelKeyHere(key, mask)   // delegates to LLPanel::handleKeyHere
}

// ---------------------------------------------------------------------------
// SideTray — lightweight singleton facade
// ---------------------------------------------------------------------------

/**
 * Singleton facade for the viewer's collapsible side-panel area.
 *
 * In the original LL viewer the side tray was a dedicated widget
 * (`LLSideTray`); Firestorm uses the Chiclet bar + floater approach instead.
 * This object provides the expected `showPanel` / `hidePanel` / `togglePanel`
 * surface so that call-sites ported from the old side-tray API continue to
 * compile.  All methods are stubs pending a full UI-framework integration.
 */
object SideTray {

    /** Whether the side tray is currently expanded. */
    private var expanded: Boolean = true

    // ------------------------------------------------------------------
    // Panel management
    // ------------------------------------------------------------------

    /**
     * Makes [name]'s panel visible and brings the side tray into view.
     */
    fun showPanel(name: String) {
        System.err.println("SideTray: showPanel not yet implemented")
    }

    /**
     * Hides the panel identified by [name].
     */
    fun hidePanel(name: String) {
        System.err.println("SideTray: hidePanel not yet implemented")
    }

    /**
     * Toggles the visibility of the panel identified by [name].
     */
    fun togglePanel(name: String) {
        System.err.println("SideTray: togglePanel not yet implemented")
    }

    // ------------------------------------------------------------------
    // Tray-level visibility
    // ------------------------------------------------------------------

    /** Returns `true` when the side tray is visible (not fully collapsed). */
    fun isVisible(): Boolean = expanded

    /**
     * Collapses the side tray so that no panel content is shown.
     */
    fun collapse() {
        expanded = false
        System.err.println("SideTray: collapse not yet implemented")
    }

    /**
     * Expands the side tray, restoring the last open panel.
     */
    fun expand() {
        expanded = true
        System.err.println("SideTray: expand not yet implemented")
    }
}
