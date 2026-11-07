/**
 * TeleportHistory.kt
 * Converted from llteleporthistory.h / llteleporthistory.cpp
 *
 * Browser-style Back/Forward teleport history for the Second Life viewer.
 * The history list grows on every successful teleport; goBack()/goForward()
 * move mCurrentItem within the list and trigger an actual teleport.
 *
 * Original: Copyright (C) 2010, Linden Research, Inc. (LGPL v2.1)
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// TeleportEntry — mirrors LLTeleportHistoryItem
//
// Stores the human-readable title (with and without coordinates), global
// position (Vector3d), the region UUID, an SLURL for OpenSim cross-grid
// teleports, and a wall-clock timestamp for display purposes.
// ---------------------------------------------------------------------------

data class TeleportEntry(
    /** Short location title (region name only, no coordinates). */
    val regionName: String,
    /** Full title including region name and local X/Y/Z coordinates. */
    val fullTitle: String,
    /**
     * Agent global position at the time of the teleport.
     * Maps to LLVector3d mGlobalPos.  [Vector3] is used here as a
     * stand-in for the 3-component double-precision type; replace with
     * Vector3d once that class is available in com.firestorm.llmath.
     */
    val globalPos: Vector3,
    /** Region UUID — used for grid-change detection on OpenSim. */
    val regionId: LLUUID = LLUUID.NULL,
    /**
     * SLURL string for this location.
     * Required for OpenSim cross-grid teleports (FIRE-35355).
     */
    val slurl: String = "",
    /** Wall-clock timestamp (ms since epoch) when the entry was recorded. */
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Return the title appropriate for the current NavBarShowCoordinates
     * setting.  Mirrors LLTeleportHistoryItem::getTitle().
     */
    fun getTitle(showCoordinates: Boolean = false): String =
        if (showCoordinates) fullTitle else regionName
}

// ---------------------------------------------------------------------------
// TeleportHistory — singleton, mirrors LLTeleportHistory (LLSingleton)
// ---------------------------------------------------------------------------

object TeleportHistory {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /**
     * The history list.  Index 0 is the oldest entry; the highest index
     * before [currentIndex] is the most recent successfully visited
     * location.  Entries after [currentIndex] are "forward" history
     * (available after goBack()).
     */
    val history: MutableList<TeleportEntry> = mutableListOf()

    /**
     * Index of the currently displayed entry within [history].
     * -1 means no entry yet (before the first location update).
     * Mirrors LLTeleportHistory::mCurrentItem.
     */
    var currentIndex: Int = -1
        private set

    /**
     * Index of the entry the user has requested to teleport to.
     * -1 means this is a fresh teleport, not a history navigation.
     * Mirrors LLTeleportHistory::mRequestedItem.
     */
    private var requestedIndex: Int = -1

    /**
     * True once we have received the first location update after login.
     * Mirrors LLTeleportHistory::mGotInitialUpdate.
     */
    private var gotInitialUpdate: Boolean = false

    /** Registered history-changed callbacks — mirrors history_signal_t. */
    private val historyChangedCallbacks: MutableList<() -> Unit> = mutableListOf()

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    /** True when there is at least one entry behind the current position. */
    fun canGoBack(): Boolean = currentIndex > 0

    /** True when there is at least one entry ahead of the current position. */
    fun canGoForward(): Boolean = currentIndex < history.size - 1

    /**
     * Navigate one step backward.
     * Mirrors LLTeleportHistory::goBack().
     */
    fun goBack() {
        goToItem(currentIndex - 1)
    }

    /**
     * Navigate one step forward.
     * Mirrors LLTeleportHistory::goForward().
     */
    fun goForward() {
        goToItem(currentIndex + 1)
    }

    /**
     * Teleport to an arbitrary history entry by its index.
     * Validates the index, guards against teleporting to the current
     * location, sets [requestedIndex], and initiates the teleport.
     * Mirrors LLTeleportHistory::goToItem().
     *
     * @param idx zero-based index into [history]
     */
    fun goToItem(idx: Int) {
        if (idx < 0 || idx >= history.size) {
            // Invalid index — log and bail.
            dump()
            return
        }
        if (idx == currentIndex) {
            // Already here — no-op.
            return
        }
        requestedIndex = idx
        val target = history[idx]
        System.err.println("TeleportHistory: goToItem not yet implemented")
    }

    /**
     * Return the entry at [currentIndex], or null if the history is empty.
     */
    fun getCurrentEntry(): TeleportEntry? =
        if (currentIndex in history.indices) history[currentIndex] else null

    // -----------------------------------------------------------------------
    // Mutation helpers
    // -----------------------------------------------------------------------

    /**
     * Record a new location.  Purges any forward entries, appends a new
     * [TeleportEntry], and advances [currentIndex].
     * Called when a teleport completes with a fresh destination (not a
     * history navigation).
     *
     * @param pos        global agent position at the new location
     * @param name       region name (short title without coordinates)
     * @param fullTitle  title including coordinates (optional)
     * @param regionId   UUID of the destination region
     * @param slurl      SLURL string for OpenSim cross-grid support
     */
    fun addEntry(
        pos: Vector3,
        name: String,
        fullTitle: String = name,
        regionId: LLUUID = LLUUID.NULL,
        slurl: String = ""
    ) {
        // Purge any forward items.
        if (history.isNotEmpty() && currentIndex < history.size - 1) {
            history.subList(currentIndex + 1, history.size).clear()
        }
        history.add(
            TeleportEntry(
                regionName = name,
                fullTitle  = fullTitle,
                globalPos  = pos,
                regionId   = regionId,
                slurl      = slurl
            )
        )
        currentIndex = history.size - 1
        onHistoryChanged()
    }

    /**
     * Called when a teleport completes.  If [requestedIndex] is set this
     * was a history navigation; otherwise it is a fresh teleport so
     * [addEntry] should have already been called.
     * Mirrors LLTeleportHistory::updateCurrentLocation().
     *
     * @param newPos  global position after the teleport
     */
    fun updateCurrentLocation(newPos: Vector3) {
        if (requestedIndex != -1) {
            currentIndex   = requestedIndex
            requestedIndex = -1
        } else {
            if (!gotInitialUpdate && history.isEmpty()) {
                // Guard: skip update if we have nothing yet.
                return
            }
            System.err.println("TeleportHistory: updateCurrentLocation not yet implemented")
        }
        if (!gotInitialUpdate) gotInitialUpdate = true
        onHistoryChanged()
    }

    /**
     * Called when a teleport fails.  Clears [requestedIndex] so the next
     * teleport is treated as a fresh navigation.
     * Mirrors LLTeleportHistory::onTeleportFailed().
     */
    fun onTeleportFailed() {
        if (requestedIndex != -1) {
            requestedIndex = -1
        }
    }

    /**
     * Called when login completes so the current location is captured as
     * the first history entry.
     * Mirrors LLTeleportHistory::handleLoginComplete().
     */
    fun handleLoginComplete() {
        if (gotInitialUpdate) return
        System.err.println("TeleportHistory: handleLoginComplete not yet implemented")
    }

    /**
     * Remove all history entries except the current one, and reset the
     * navigation state.
     * Mirrors LLTeleportHistory::purgeItems().
     */
    fun purgeItems() {
        if (history.isEmpty()) return   // guard: called before any location update
        val current = getCurrentEntry()
        history.clear()
        if (current != null) history.add(current)
        requestedIndex = -1
        currentIndex   = if (history.isNotEmpty()) 0 else -1
        onHistoryChanged()
    }

    // -----------------------------------------------------------------------
    // Callbacks / signals
    // -----------------------------------------------------------------------

    /**
     * Register a callback to be invoked whenever the history changes.
     * Returns a lambda that the caller can invoke to unregister.
     * Mirrors LLTeleportHistory::setHistoryChangedCallback().
     */
    fun setHistoryChangedCallback(cb: () -> Unit): () -> Unit {
        historyChangedCallbacks.add(cb)
        return { historyChangedCallbacks.remove(cb) }
    }

    /** Fire all registered history-changed callbacks. */
    private fun onHistoryChanged() {
        historyChangedCallbacks.forEach { it() }
    }

    // -----------------------------------------------------------------------
    // Debug
    // -----------------------------------------------------------------------

    /**
     * Print the history to the log.
     * Mirrors LLTeleportHistory::dump().
     */
    fun dump() {
        println("TeleportHistory dump (${history.size} items):")
        history.forEachIndexed { i, entry ->
            val marker = if (i == currentIndex) " * " else "   "
            println("$marker$i: ${entry.regionName} | regionId=${entry.regionId} | pos=${entry.globalPos} | slurl=${entry.slurl}")
        }
    }
}
