/**
 * FSFloaterStatistics.kt
 * Kotlin conversion of fsfloaterstatistics.h / fsfloaterstatistics.cpp
 *
 * Extended statistics floater — wraps the viewer's built-in statistics bar and
 * exposes it as a Firestorm-managed floater.  Supports an optional "no focus"
 * chrome mode controlled by the saved setting `FSStatisticsNoFocus`.
 *
 * Original author: Liny Odell, 2018
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// Stat entry data class
// ---------------------------------------------------------------------------

/**
 * A single named statistic value with an optional unit suffix.
 *
 * @param name  Human-readable label shown in the floater (e.g. "FPS").
 * @param value Current floating-point measurement.
 * @param unit  Display unit string (e.g. "ms", "%", "KB/s"); empty if dimensionless.
 */
data class StatEntry(
    val name: String,
    val value: Float,
    val unit: String,
)

// ---------------------------------------------------------------------------
// Main floater class
// ---------------------------------------------------------------------------

/**
 * Firestorm extended statistics floater.
 *
 * Presents viewer statistics grouped into named categories (e.g. "Render",
 * "Network", "Sim") as a read-only display.  Data is refreshed every frame
 * via [refresh].
 *
 * The C++ implementation is intentionally thin — it merely wraps the built-in
 * statistics panel as a Firestorm-owned floater and optionally sets chrome mode
 * so the window never steals keyboard focus.
 *
 * Complex stat-sampling and UI-wiring logic is stubbed with [TODO].
 *
 * Mirrors [FSFloaterStatistics] from `fsfloaterstatistics.h`.
 */
class FSFloaterStatistics {

    // ------------------------------------------------------------------
    // Public state
    // ------------------------------------------------------------------

    /**
     * All statistics grouped by category name.
     *
     * Keys are category labels (e.g. `"Render"`, `"Network"`, `"Sim"`).
     * Values are ordered lists of [StatEntry] objects.
     */
    val categories: MutableMap<String, MutableList<StatEntry>> = mutableMapOf()

    // ------------------------------------------------------------------
    // Private state
    // ------------------------------------------------------------------

    /**
     * Whether the floater is operating in chrome (no-focus) mode.
     *
     * Driven by the `FSStatisticsNoFocus` saved setting.
     */
    private var chromeMode: Boolean = false

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Called after the floater's XUI children have been built.
     *
     * Reads the `FSStatisticsNoFocus` setting and sets chrome mode if required,
     * matching the C++ implementation exactly.
     *
     * C++ equivalent: `FSFloaterStatistics::postBuild()`
     */
    fun postBuild(): Boolean {
        return false
    }

    /**
     * Called each time the floater is shown.
     *
     * Re-applies chrome / no-focus settings in case they changed since last open.
     *
     * C++ equivalent: `FSFloaterStatistics::onOpen(key)`
     */
    fun onOpen(key: LLSD) {
        System.err.println("FSFloaterStatistics: onOpen not yet implemented")
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Collect fresh values for all [categories] and update the displayed rows.
     *
     * Called once per frame from the floater's draw override (not present in
     * the slim C++ implementation, but required by the Kotlin model to keep
     * the UI live).
     */
    fun refresh() {
        System.err.println("FSFloaterStatistics: refresh not yet implemented")
    }

    /**
     * Add or update a single statistic in [categories].
     *
     * If [category] does not yet exist it is created.  If a [StatEntry] with
     * the same [name] already exists in the category its [StatEntry.value] is
     * replaced; otherwise a new entry is appended.
     *
     * @param category Category label under which the stat is grouped.
     * @param name     Stat name unique within [category].
     * @param value    Current measurement.
     * @param unit     Display unit (pass `""` for dimensionless values).
     */
    fun addStat(category: String, name: String, value: Float, unit: String = "") {
        val list = categories.getOrPut(category) { mutableListOf() }
        val existing = list.indexOfFirst { it.name == name }
        val entry = StatEntry(name = name, value = value, unit = unit)
        if (existing >= 0) {
            list[existing] = entry
        } else {
            list.add(entry)
        }
    }

    /**
     * Remove all entries from all categories, resetting [categories] to empty.
     *
     * Useful before a full refresh to avoid stale entries.
     */
    fun clearStats() {
        categories.clear()
    }

    /**
     * Return all [StatEntry] objects across every category as a flat list,
     * preserving category order then per-category insertion order.
     */
    fun allStats(): List<StatEntry> = categories.values.flatten()

    // ------------------------------------------------------------------
    // Companion object — singleton access and factory
    // ------------------------------------------------------------------

    companion object {

        @Volatile
        private var instance: FSFloaterStatistics? = null

        /**
         * Return the singleton instance, creating it if necessary.
         * Mirrors [LLFloaterReg::getInstance("fs_stats")] in C++.
         */
        fun getInstance(): FSFloaterStatistics =
            instance ?: synchronized(this) {
                instance ?: FSFloaterStatistics().also { instance = it }
            }

        /**
         * Make the statistics floater visible.
         * Mirrors [LLFloaterReg::showInstance("fs_stats")] in C++.
         */
        fun show() {
            getInstance()
            System.err.println("FSFloaterStatistics: show not yet implemented")
        }
    }
}
