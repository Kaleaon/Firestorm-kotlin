package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Rect
import com.firestorm.llui.View

// Corresponds to: lltoolbarview.h / lltoolbarview.cpp  (and lltoolbar.h)
// User-customisable toolbar view that manages up to four positional toolbars.

// ── Enumerations ──────────────────────────────────────────────────────────────

/**
 * Mirrors C++ [LLToolBarEnums::EToolBarLocation].
 * Indicates which physical toolbar a command lives on.
 */
enum class ToolBarLocation(val index: Int) {
    NONE(-1),
    LEFT(0),
    RIGHT(1),
    BOTTOM(2),
    TOP(3);

    companion object {
        /** Total number of real toolbar slots (matches C++ TOOLBAR_COUNT). */
        const val COUNT = 4
    }
}

/**
 * Mirrors C++ [LLToolBarEnums::ButtonType].
 * Controls how toolbar buttons display their label/icon.
 */
enum class ButtonDisplayMode {
    ICON_ONLY,
    LABEL_ONLY,
    ICON_AND_LABEL,
}

/**
 * Mirrors C++ [LLToolBarEnums::Alignment].
 */
enum class ToolBarAlignment {
    LEFT,
    CENTER,
    RIGHT,
}

/**
 * Mirrors C++ [LLToolBarEnums::LayoutStyle].
 */
enum class ToolBarLayoutStyle {
    NONE,
    WRAP,
}

// ── Data model ────────────────────────────────────────────────────────────────

/**
 * Represents a single toolbar command / button.
 *
 * @param commandId  Stable string identifier (e.g. "build", "chat").
 * @param label      Localised display label.
 * @param icon       Asset path or resource name for the button icon.
 * @param isEnabled  Whether the command is currently enabled.
 * @param isFlashing Whether the button should be flashing (attention state).
 */
data class ToolbarCommand(
    val commandId: String,
    val label: String,
    val icon: String,
    var isEnabled: Boolean  = true,
    var isFlashing: Boolean = false,
)

/**
 * Models one physical toolbar strip (left, right, or bottom).
 * Mirrors C++ [LLToolBar].
 */
data class ToolBarStrip(
    val location: ToolBarLocation,
    var buttonDisplayMode: ButtonDisplayMode = ButtonDisplayMode.ICON_AND_LABEL,
    var alignment: ToolBarAlignment          = ToolBarAlignment.LEFT,
    var layoutStyle: ToolBarLayoutStyle      = ToolBarLayoutStyle.NONE,
    val commands: MutableList<String>        = mutableListOf(),
) {
    companion object {
        /** Sentinel rank meaning "append at end". Mirrors C++ LLToolBar::RANK_NONE. */
        const val RANK_NONE = -1
    }
}

// ── ToolBarView object ────────────────────────────────────────────────────────

/**
 * Viewer toolbar view — singleton that owns and coordinates all toolbar strips.
 *
 * Mirrors C++ [LLToolBarView] (extends LLUICtrl → View here).
 * The C++ class is accessed globally via [gToolBarView]; here we use an
 * `object` singleton and expose [getInstance] for compatibility.
 */
object ToolBarView {

    // One strip per location slot (indexed by ToolBarLocation.index 0–3)
    private val toolbars: Array<ToolBarStrip> = Array(ToolBarLocation.COUNT) { i ->
        val loc = ToolBarLocation.entries.first { it.index == i }
        ToolBarStrip(location = loc)
    }

    /** Registry of all known commands (commandId → ToolbarCommand). */
    val commandRegistry: MutableMap<String, ToolbarCommand> = mutableMapOf()

    private var toolbarsLoaded: Boolean    = false
    private var showToolbars: Boolean      = true
    private var hideBottomOnEmpty: Boolean = false

    // ── Command queries ───────────────────────────────────────────────────────

    /**
     * Returns the [ToolBarLocation] index where [commandId] lives,
     * or [ToolBarLocation.NONE].index if not found. Mirrors C++ hasCommand().
     */
    fun hasCommand(commandId: String): Int {
        for (strip in toolbars) {
            if (commandId in strip.commands) return strip.location.index
        }
        return ToolBarLocation.NONE.index
    }

    /**
     * Adds [commandId] to [location] at [rank] (position).
     * Returns the [ToolBarLocation] index where it was placed.
     * Mirrors C++ LLToolBarView::addCommand().
     */
    fun addCommand(
        commandId: String,
        location: ToolBarLocation,
        rank: Int = ToolBarStrip.RANK_NONE,
    ): Int {
        if (location == ToolBarLocation.NONE) return ToolBarLocation.NONE.index
        val strip = toolbars[location.index]
        // Remove from any current strip first
        removeFromAllStrips(commandId)
        if (rank == ToolBarStrip.RANK_NONE || rank >= strip.commands.size) {
            strip.commands.add(commandId)
        } else {
            strip.commands.add(rank.coerceAtLeast(0), commandId)
        }
        // rebuild toolbar button layout for strip at ${location.name} (GL stub)
        return location.index
    }

    /**
     * Removes [commandId] from whichever toolbar currently holds it.
     * Returns a pair of (locationIndex, rank) where rank is the former position,
     * or (NONE.index, RANK_NONE) if not found.
     * Mirrors C++ LLToolBarView::removeCommand().
     */
    fun removeCommand(commandId: String): Pair<Int, Int> {
        for (strip in toolbars) {
            val idx = strip.commands.indexOf(commandId)
            if (idx >= 0) {
                strip.commands.removeAt(idx)
                // rebuild toolbar button layout for strip at ${strip.location.name} (GL stub)
                return Pair(strip.location.index, idx)
            }
        }
        return Pair(ToolBarLocation.NONE.index, ToolBarStrip.RANK_NONE)
    }

    /**
     * Enables or disables [commandId] wherever it lives.
     * Returns location index, or NONE.index if not found.
     */
    fun enableCommand(commandId: String, enabled: Boolean): Int {
        commandRegistry[commandId]?.isEnabled = enabled
        // repaint button for commandId (GL stub)
        return hasCommand(commandId)
    }

    /**
     * Stops an in-progress command (e.g. cancels a drag).
     * Mirrors C++ stopCommandInProgress().
     */
    fun stopCommandInProgress(commandId: String): Int {
        // stop any running animation/state for commandId (GL stub)
        return hasCommand(commandId)
    }

    /**
     * Starts or stops the flash animation on a toolbar button.
     * Mirrors C++ LLToolBarView::flashCommand().
     */
    fun flashCommand(commandId: String, flash: Boolean, forceFlashing: Boolean = false): Int {
        commandRegistry[commandId]?.isFlashing = flash
        // start/stop LLFlashTimer for commandId button (GL stub)
        return hasCommand(commandId)
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /**
     * Loads toolbar layout from user or default settings file.
     * Mirrors C++ LLToolBarView::loadToolbars().
     *
     * @param forceDefault if true, ignore user overrides and load the skin defaults.
     * @return true on success.
     */
    fun loadToolbars(forceDefault: Boolean = false): Boolean {
        // parse toolbars.xml from user/skin directory and populate toolbars[] (IO stub)
        toolbarsLoaded = true
        return true
    }

    /** Removes all commands from every toolbar strip. Mirrors C++ clearToolbars(). */
    fun clearToolbars(): Boolean {
        toolbars.forEach { it.commands.clear() }
        // remove all button views from toolbar panels (GL stub)
        return true
    }

    // ── Visibility ────────────────────────────────────────────────────────────

    fun setToolBarsVisible(visible: Boolean) {
        showToolbars = visible
        // show/hide mBottomToolbarPanel and side toolbars (GL stub)
    }

    fun setHideBottomOnEmpty(hide: Boolean) {
        hideBottomOnEmpty = hide
    }

    fun isModified(): Boolean {
        // compare current layout against persisted layout (IO stub)
        return toolbarsLoaded
    }

    /** Returns the [ToolBarStrip] for [location], or null for NONE. */
    fun getToolbar(location: ToolBarLocation): ToolBarStrip? {
        if (location == ToolBarLocation.NONE) return null
        return toolbars[location.index]
    }

    // ── Drag-and-drop ─────────────────────────────────────────────────────────

    fun startDragTool(x: Int, y: Int, commandId: String) {
        // begin drag of toolbar button commandId (GL stub)
    }

    fun handleDragTool(x: Int, y: Int, uuid: LLUUID): Boolean {
        // handle drag-over for toolbar button drop target (GL stub)
        return false
    }

    fun handleDropTool(commandId: String, targetLocation: ToolBarLocation, x: Int, y: Int): Boolean {
        // accept drop and reorder command in target toolbar (GL stub)
        return false
    }

    fun resetDragTool(commandId: String) {
        // cancel drag and restore button to original strip (GL stub)
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    fun draw() {
        // render all toolbar strips and their buttons (GL stub)
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    fun loadDefaultToolbars(): Boolean = loadToolbars(forceDefault = true)
    fun clearAllToolbars(): Boolean    = clearToolbars()

    /** Global accessor matching C++ [gToolBarView] pattern. */
    fun getInstance(): ToolBarView = this

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun removeFromAllStrips(commandId: String) {
        toolbars.forEach { it.commands.remove(commandId) }
    }
}
