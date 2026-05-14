/**
 * FSFloaterPerformance.kt
 * Kotlin conversion of fsfloaterperformance.h / fsfloaterperformance.cpp
 *
 * Viewer performance statistics floater — displays FPS, per-category frame-time
 * percentages, avatar render costs, HUD attachments, and nearby-avatar GPU times.
 * Forked from llfloaterperformance.h/cpp (Linden Research, Inc., 2021).
 *
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// Data class
// ---------------------------------------------------------------------------

/**
 * Snapshot of the key performance counters shown in the floater.
 *
 * @param fps             Median frames-per-second over the last 50 periods.
 * @param renderMs        Total frame time in milliseconds.
 * @param memUsageMB      Current resident memory usage in megabytes.
 * @param avatarComplexity Aggregate visual complexity of nearby avatars (×1 000).
 */
data class PerformanceData(
    val fps: Float,
    val renderMs: Float,
    val memUsageMB: Int,
    val avatarComplexity: Int,
)

// ---------------------------------------------------------------------------
// Avatar render-override action (mirrors LLVOAvatar::VisualMuteSettings)
// ---------------------------------------------------------------------------

/** The rendering treatment to apply to a specific nearby avatar. */
enum class AvatarRenderSetting {
    /** Render normally according to complexity rules. */
    RENDER_NORMALLY,
    /** Always render at full quality. */
    ALWAYS_RENDER,
    /** Never render (jelly-doll or fully hidden). */
    DO_NOT_RENDER,
}

// ---------------------------------------------------------------------------
// Main floater class
// ---------------------------------------------------------------------------

/**
 * Firestorm performance statistics floater.
 *
 * Shows a main panel with sub-panels for:
 * - **Nearby** avatars and their GPU render times.
 * - **Complexity** — worn attachment object costs.
 * - **HUDs** — worn HUD attachment render times.
 * - **Settings** — graphics-quality slider and preset management.
 * - **AutoTune** — target-FPS tuning controls.
 *
 * UI is driven by a 1-second refresh timer (mirrors [REFRESH_INTERVAL = 1.0f]).
 * Complex UI / pipeline calls are stubbed with [TODO].
 *
 * Mirrors [FSFloaterPerformance] from `fsfloaterperformance.h`.
 */
class FSFloaterPerformance {

    // ------------------------------------------------------------------
    // Public state
    // ------------------------------------------------------------------

    /** Most-recently collected performance snapshot, updated by [refresh]. */
    var performanceData: PerformanceData = PerformanceData(
        fps = 0f,
        renderMs = 0f,
        memUsageMB = 0,
        avatarComplexity = 0,
    )
        private set

    // ------------------------------------------------------------------
    // Private state
    // ------------------------------------------------------------------

    /** Maximum GPU time (ms) among nearby avatars; -1 until first profile. */
    private var nearbyMaxGpuTime: Float = -1f

    /** Whether the nearby-avatar list is currently visible. */
    private var nearbyPanelVisible: Boolean = false

    /** Whether the HUD-attachments panel is currently visible. */
    private var hudsPanelVisible: Boolean = false

    /** Whether the worn-object complexity panel is currently visible. */
    private var complexityPanelVisible: Boolean = false

    /** Whether statistics are currently being collected. */
    private var updateTimerExpired: Boolean = false

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Called after the floater's XUI children have been built.
     *
     * Wires up child panels, back buttons, slider callbacks, context menus,
     * and signal connections for complexity/ART setting changes.
     *
     * C++ equivalent: `FSFloaterPerformance::postBuild()`
     */
    fun postBuild(): Boolean {
        System.err.println("FSFloaterPerformance: postBuild not yet implemented")
        return false
    }

    /**
     * Called every frame to update the display.
     *
     * Reads frame-time stats from [LLPerfStats::StatsRecorder], computes
     * per-category percentages, updates text boxes, and conditionally
     * re-populates whichever sub-list is currently visible.
     *
     * Refresh is rate-limited to once per second via an internal timer
     * (mirrors [REFRESH_INTERVAL = 1.0f]).
     *
     * C++ equivalent: `FSFloaterPerformance::draw()`
     */
    fun draw() {
        System.err.println("FSFloaterPerformance: draw not yet implemented")
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Collect a fresh [PerformanceData] snapshot and update [performanceData].
     *
     * Called from [draw] after the refresh timer expires, and can also be
     * triggered externally (e.g. after changing the graphics level).
     */
    fun refresh() {
        System.err.println("FSFloaterPerformance: refresh not yet implemented")
    }

    // ------------------------------------------------------------------
    // Panel visibility
    // ------------------------------------------------------------------

    /**
     * Reveal [selectedPanel] and hide all other sub-panels.
     * If the revealed panel is the HUD, nearby, or complexity panel it also
     * triggers a list population.
     *
     * C++ equivalent: `FSFloaterPerformance::showSelectedPanel(LLPanel*)`
     */
    fun showSelectedPanel(selectedPanel: String) {
        hidePanels()
        System.err.println("FSFloaterPerformance: showSelectedPanel not yet implemented")
    }

    /** Show the main panel and hide all sub-panels. */
    fun showMainPanel() {
        hidePanels()
        System.err.println("FSFloaterPerformance: showMainPanel not yet implemented")
    }

    /** Hide all sub-panels (nearby, complexity, HUDs, settings, auto-tune). */
    fun hidePanels() {
        nearbyPanelVisible = false
        hudsPanelVisible = false
        complexityPanelVisible = false
        System.err.println("FSFloaterPerformance: hidePanels not yet implemented")
    }

    // ------------------------------------------------------------------
    // Avatar render-override actions (context-menu callbacks)
    // ------------------------------------------------------------------

    /**
     * Apply a render-override action to the avatar identified by [avId].
     *
     * If the avatar is not in the scene, persists the setting via
     * FSAvatarRenderPersistence.
     *
     * C++ equivalent: `FSFloaterPerformance::onCustomAction(userdata, av_id)`
     */
    fun onCustomAction(command: String, avId: LLUUID) {
        val newSetting = when (command) {
            "never"  -> AvatarRenderSetting.DO_NOT_RENDER
            "always" -> AvatarRenderSetting.ALWAYS_RENDER
            else     -> AvatarRenderSetting.RENDER_NORMALLY
        }
        System.err.println("FSFloaterPerformance: onCustomAction not yet implemented")
    }

    /**
     * Returns whether [command] matches the current render setting for [avId].
     *
     * C++ equivalent: `FSFloaterPerformance::isActionChecked(userdata, av_id)`
     */
    fun isActionChecked(command: String, avId: LLUUID): Boolean {
        System.err.println("FSFloaterPerformance: isActionChecked not yet implemented")
        return false
    }

    /**
     * Execute an extended context-menu action (inspect attachments or zoom).
     *
     * C++ equivalent: `FSFloaterPerformance::onExtendedAction(userdata, av_id)`
     */
    fun onExtendedAction(command: String, avId: LLUUID) {
        System.err.println("FSFloaterPerformance: onExtendedAction not yet implemented")
    }

    // ------------------------------------------------------------------
    // HUD / object list actions
    // ------------------------------------------------------------------

    /**
     * Detach the inventory item identified by [itemId] from the avatar.
     *
     * C++ equivalent: `FSFloaterPerformance::detachItem(item_id)`
     */
    fun detachItem(itemId: LLUUID) {
        System.err.println("FSFloaterPerformance: detachItem not yet implemented")
    }

    // ------------------------------------------------------------------
    // Preference management
    // ------------------------------------------------------------------

    /**
     * Reset the maximum-ART slider and recompute render-cost limits.
     *
     * C++ equivalent: `FSFloaterPerformance::resetMaxArtSlider()`
     */
    fun resetMaxArtSlider() {
        System.err.println("FSFloaterPerformance: resetMaxArtSlider not yet implemented")
    }

    /** Open the save-preset dialog. */
    fun savePreset() {
        System.err.println("FSFloaterPerformance: savePreset not yet implemented")
    }

    /** Open the load-preset dialog and reset the ART slider. */
    fun loadPreset() {
        System.err.println("FSFloaterPerformance: loadPreset not yet implemented")
    }

    /** Apply recommended hardware defaults and reset the ART slider. */
    fun setHardwareDefaults() {
        System.err.println("FSFloaterPerformance: setHardwareDefaults not yet implemented")
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private fun initBackBtn(panelName: String) {
        System.err.println("FSFloaterPerformance: initBackBtn not yet implemented")
    }

    private fun populateHudList() {
        System.err.println("FSFloaterPerformance: populateHudList not yet implemented")
    }

    private fun populateObjectList() {
        System.err.println("FSFloaterPerformance: populateObjectList not yet implemented")
    }

    private fun populateNearbyList() {
        System.err.println("FSFloaterPerformance: populateNearbyList not yet implemented")
    }

    private fun onChangeQuality(level: UInt) {
        System.err.println("FSFloaterPerformance: onChangeQuality not yet implemented")
    }

    private fun onClickHideAvatars() {
        System.err.println("FSFloaterPerformance: onClickHideAvatars not yet implemented")
    }

    private fun onClickExceptions() {
        System.err.println("FSFloaterPerformance: onClickExceptions not yet implemented")
    }

    private fun onAvatarListRightClick(x: Int, y: Int) {
        System.err.println("FSFloaterPerformance: onAvatarListRightClick not yet implemented")
    }

    private fun updateMaxComplexity() {
        System.err.println("FSFloaterPerformance: updateMaxComplexity not yet implemented")
    }

    private fun updateMaxRenderTime() {
        System.err.println("FSFloaterPerformance: updateMaxRenderTime not yet implemented")
    }

    private fun updateMaxRenderTimeText() {
        System.err.println("FSFloaterPerformance: updateMaxRenderTimeText not yet implemented")
    }

    private fun updateComplexityText() {
        System.err.println("FSFloaterPerformance: updateComplexityText not yet implemented")
    }

    // ------------------------------------------------------------------
    // Companion object — singleton access and factory
    // ------------------------------------------------------------------

    companion object {

        @Volatile
        private var instance: FSFloaterPerformance? = null

        /**
         * Return the singleton instance, creating it if necessary.
         * Mirrors [LLFloaterReg::getInstance("fs_performance")] in C++.
         */
        fun getInstance(): FSFloaterPerformance =
            instance ?: synchronized(this) {
                instance ?: FSFloaterPerformance().also { instance = it }
            }

        /**
         * Make the performance floater visible.
         * Mirrors [LLFloaterReg::showInstance("fs_performance")] in C++.
         */
        fun show() {
            getInstance()
            System.err.println("FSFloaterPerformance: show not yet implemented")
        }
    }
}
