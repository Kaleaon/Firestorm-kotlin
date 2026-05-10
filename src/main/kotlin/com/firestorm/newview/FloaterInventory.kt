// Synthesised from:
//   indra/newview/fsfloaterpartialinventory.h / .cpp  (Firestorm-specific)
//   indra/newview/llpanelmaininventory.h / .cpp       (newWindow / show / hide helpers)
//   indra/newview/llinventorypanel.h                  (getActiveInventoryPanel)
// No single llfloaterinventory.h/cpp exists in this codebase; the standard
// inventory floater is surfaced through LLFloaterSidePanelContainer.
// Original: Copyright (C) 2010, Linden Research, Inc. / Firestorm Project (LGPL 2.1)
package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ── FloaterInventory ──────────────────────────────────────────────────────────

/**
 * Top-level floating inventory window.  Wraps a [PanelMainInventory] and
 * exposes the canonical show / hide / toggle / isVisible lifecycle.
 *
 * In the original C++ codebase there is no single `llfloaterinventory.h`;
 * instead the inventory floater is registered as "inventory" in the
 * [LLFloaterReg] table and opened via `LLFloaterSidePanelContainer`.
 * Firestorm also adds [FSFloaterPartialInventory] for sub-folder views.
 * This class unifies both concepts for the Kotlin port.
 */
class FloaterInventory private constructor(
    val key: LLUUID = LLUUID()
) {

    // ── Owned panel ───────────────────────────────────────────────────────────

    val mainPanel: PanelMainInventory = PanelMainInventory()

    // ── Visibility state ──────────────────────────────────────────────────────

    private var visible: Boolean = false

    // ── Filter editor state ───────────────────────────────────────────────────

    private var filterText: String = ""

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Called once after the UI XML has been inflated.
     * Mirrors [FSFloaterPartialInventory::postBuild] /
     * [LLPanelMainInventory::postBuild].
     */
    fun postBuild(): Boolean {
        return mainPanel.postBuild()
    }

    /**
     * Called when the floater is made visible, optionally navigating to an
     * item or folder specified in [key].
     * Mirrors [FSFloaterPartialInventory::onOpen].
     */
    fun onOpen(key: LLUUID = LLUUID()) {
        visible = true
        mainPanel.showInventoryPanel()

        if (!key.isNull()) {
            // If the key identifies a folder, root the single-folder view there;
            // otherwise select the item in the main panel.
            val cat = InventoryModel.categories[key]
            if (cat != null) {
                mainPanel.setSingleFolderViewRoot(key)
            } else {
                InventoryPanel.openInventoryPanelAndSetSelection(
                    autoOpen          = false,
                    objId             = key,
                    useMainPanel      = true,
                    takeKeyboardFocus = true,
                    resetFilter       = false
                )
            }
        }
    }

    /** Called when the floater is closed / hidden. */
    fun onClose() {
        visible = false
        mainPanel.hideInventoryPanel()
    }

    // ── Active inventory panel passthrough ────────────────────────────────────

    fun getInventoryPanel(): InventoryPanel = mainPanel.getActivePanel()

    fun getRootFolder(): FolderViewItem? = getInventoryPanel().getRootFolder()

    // ── Filter passthrough ────────────────────────────────────────────────────

    fun setFilterSubString(str: String) {
        filterText = str
        mainPanel.onFilterEdit(str)
    }

    fun getFilterSubString(): String = filterText

    // ── Draw ──────────────────────────────────────────────────────────────────

    fun draw() {
        if (!visible) return
        mainPanel.draw()
        TODO("Draw floater chrome (title bar, resize handles) via UI toolkit")
    }

    // ── Singleton registry ────────────────────────────────────────────────────

    companion object {

        /** The single primary inventory floater instance, if open. */
        private var instance: FloaterInventory? = null

        /** Registry of sub-folder partial-inventory floaters keyed by root UUID. */
        private val partialInstances: MutableMap<LLUUID, FloaterInventory> = mutableMapOf()

        /**
         * Shows the primary inventory floater, creating it if necessary.
         * Mirrors `LLFloaterReg::showInstance("inventory")` / `newWindow()`.
         */
        fun show() {
            if (instance == null) {
                instance = FloaterInventory().also { it.postBuild() }
            }
            instance!!.onOpen()
        }

        /**
         * Hides the primary inventory floater without destroying it.
         * Mirrors the close-button handler in the C++ floater.
         */
        fun hide() {
            instance?.onClose()
        }

        /**
         * Toggles visibility of the primary inventory floater.
         * Mirrors the keyboard shortcut handler in [LLViewerWindow].
         */
        fun toggle() {
            if (isVisible()) hide() else show()
        }

        /** Returns true if the primary inventory floater is currently visible. */
        fun isVisible(): Boolean = instance?.visible == true

        /**
         * Returns the primary inventory floater instance, or null if not open.
         * Mirrors [LLInventoryPanel::getActiveInventoryPanel] callers that also
         * check the floater.
         */
        fun getInstance(): FloaterInventory? = instance

        // ── Partial / sub-folder floater API ──────────────────────────────────

        /**
         * Opens (or brings to front) a partial inventory floater rooted at
         * [rootFolderId].  Mirrors [FSFloaterPartialInventory] construction via
         * [LLFloaterReg].
         */
        fun showPartial(rootFolderId: LLUUID) {
            val floater = partialInstances.getOrPut(rootFolderId) {
                FloaterInventory(key = rootFolderId).also { it.postBuild() }
            }
            floater.onOpen(key = rootFolderId)
        }

        /** Closes the partial inventory floater for [rootFolderId] if open. */
        fun hidePartial(rootFolderId: LLUUID) {
            partialInstances[rootFolderId]?.onClose()
        }

        /**
         * Opens a new inventory window rooted at [folderId], selecting
         * [itemToSelect] if non-null.
         * Mirrors [LLPanelMainInventory::newFolderWindow].
         */
        fun newFolderWindow(folderId: LLUUID = LLUUID(), itemToSelect: LLUUID = LLUUID()) {
            showPartial(folderId)
            if (!itemToSelect.isNull()) {
                partialInstances[folderId]
                    ?.getInventoryPanel()
                    ?.setSelection(itemToSelect, takeKeyboardFocus = true)
            }
        }
    }
}
