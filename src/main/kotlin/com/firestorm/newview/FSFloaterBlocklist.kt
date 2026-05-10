/**
 * FSFloaterBlocklist.kt
 * Kotlin conversion of fsfloaterblocklist.h / fsfloaterblocklist.cpp
 *
 * Standalone block-list floater for the Firestorm viewer.
 *
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLSD

/**
 * Standalone block-list (mute-list) floater.
 *
 * Embeds the `panel_block_list_sidetray` child panel — the same panel that
 * appears inside the People side-tray — so it can be shown as an independent,
 * resizable window.
 *
 * Mirrors [FSFloaterBlocklist] from `fsfloaterblocklist.h`.
 *
 * @param seed LLSD key passed by the floater registry on construction.
 */
class FSFloaterBlocklist(val seed: LLSD) {

    // ------------------------------------------------------------------
    // Child widget references — populated in [postBuild]
    // ------------------------------------------------------------------

    private var blockedListPanel: BlockListPanel? = null

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Called after the floater's XML children have been inflated.
     *
     * Locates the `panel_block_list_sidetray` child panel.
     *
     * @return `true` on success; `false` if the child panel is missing.
     */
    fun postBuild(): Boolean {
        blockedListPanel = getChild<BlockListPanel>("panel_block_list_sidetray")
        return blockedListPanel != null
    }

    /**
     * Called when the floater is opened (possibly with contextual data).
     *
     * Forwards [key] to the embedded panel so it can pre-select or filter
     * the block list (e.g. scroll to a specific blocked entry).
     *
     * @param key Optional LLSD context — may carry a UUID or other hint.
     */
    fun onOpen(key: LLSD) {
        blockedListPanel?.onOpen(key)
    }

    // ------------------------------------------------------------------
    // Stubs for framework calls that require the LL UI/platform layer
    // ------------------------------------------------------------------

    private fun <T> getChild(name: String): T? {
        TODO("Platform: resolve child widget '$name' from the floater's view hierarchy")
    }

    // ------------------------------------------------------------------
    // Nested stub for the embedded panel type
    // ------------------------------------------------------------------

    /**
     * Stub: mirrors LLPanelBlockedList / the `panel_block_list_sidetray` XML panel.
     *
     * The real class lives in `llpanelblockedlist.h` in the viewer source tree.
     */
    class BlockListPanel {
        /**
         * Called when the panel (and the enclosing floater) are opened.
         *
         * @param key Optional context data forwarded from [FSFloaterBlocklist.onOpen].
         */
        fun onOpen(key: LLSD) {
            TODO("Platform: refresh block list and optionally select entry identified by key")
        }
    }
}
