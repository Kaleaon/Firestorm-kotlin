/**
 * DockControl.kt
 * Converted from: indra/llui/lldockcontrol.h / lldockcontrol.cpp
 * Original: LLDockControl — docking anchor service for dockable floaters.
 *
 * Provides layout services for attaching ("docking") a floater to an
 * arbitrary view widget.  The dockable floater should hold an instance of
 * this class and delegate positioning logic to it each frame.
 *
 * Copyright (C) 2010, Linden Research, Inc. (LGPL 2.1)
 * Kotlin port: Firestorm-kotlin project.
 */

package com.firestorm.newview

import com.firestorm.llui.LLFloater
import com.firestorm.llui.LLRect
import com.firestorm.llui.LLUIImage
import com.firestorm.llui.LLView

// ---------------------------------------------------------------------------
// DocAt — where the floater is positioned relative to its dock widget
// ---------------------------------------------------------------------------

/**
 * Specifies on which side of the dock widget the dockable floater appears.
 * Mirrors the `LLDockControl::DocAt` enum (with NONE added for the Kotlin
 * port to express "not docked").
 */
enum class DocAt {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    NONE
}

// ---------------------------------------------------------------------------
// DockControl
// ---------------------------------------------------------------------------

/**
 * Provides docking geometry services for a floater that cannot inherit from
 * `LLDockableFloater` directly.
 *
 * **Lifecycle**: Create one instance per dockable floater, passing the widget
 * to dock against ([dockWidget]), the floater itself ([dockableFloater]), a
 * tongue image ([dockTongue]), and the preferred docking edge ([dockAt]).
 * Call [repositionDockable] each frame (or whenever layout may have changed)
 * so the floater tracks its anchor.
 *
 * @param dockWidget       The view the floater snaps to; may be `null` to
 *                         start undocked.
 * @param dockableFloater  The floater being positioned.
 * @param dockTongue       Decorative "tongue" image drawn between the floater
 *                         and its dock widget.
 * @param dockAt           The edge of [dockWidget] the floater attaches to.
 * @param getAllowedRect    Optional callback that returns the bounding rect
 *                         within which the floater may be placed.  When
 *                         `null` the non-toolbar panel rect is used.
 */
class DockControl(
    dockWidget: LLView?,
    private val dockableFloater: LLFloater,
    private val dockTongue: LLUIImage,
    private val dockAt: DocAt,
    private val getAllowedRect: ((rect: LLRect) -> Unit)? = null
) {

    // ------------------------------------------------------------------
    // Internal state
    // ------------------------------------------------------------------

    /** Weak reference to the dock widget (may become invalid if the widget is destroyed). */
    private var dockWidgetHandle: LLView? = dockWidget

    /** Cached handle to the floater's non-toolbar panel, used to derive the allowed rect. */
    private var nonToolbarPanel: LLView? = null

    private var enabled: Boolean = false
    private var recalculateDockablePosition: Boolean = false
    private var dockWidgetVisible: Boolean = false

    // Cached layout data used to detect when repositioning is needed.
    private var prevDockRect: LLRect = LLRect()
    private var rootRect: LLRect = LLRect()
    private var floaterRect: LLRect = LLRect()

    /** X coordinate (in floater-local space) of the tongue image. */
    var dockTongueX: Int = 0
        private set

    /** Y coordinate (in floater-local space) of the tongue image. */
    var dockTongueY: Int = 0
        private set

    // ------------------------------------------------------------------
    // Initialisation
    // ------------------------------------------------------------------

    init {
        // Mirror: LLDockControl constructor body.
        nonToolbarPanel = dockableFloater.rootView
            ?.getChild<LLView>("non_toolbar_panel")

        if (dockableFloater.isDocked) on() else off()

        if (dockWidget != null) {
            repositionDockable()
            dockWidgetVisible = isDockVisible()
        } else {
            dockWidgetVisible = false
        }
    }

    // ------------------------------------------------------------------
    // Enable / disable
    // ------------------------------------------------------------------

    /**
     * Enables docking (activates positioning) if the dock widget is currently
     * visible.  Mirrors `LLDockControl::on`.
     */
    fun on() {
        if (isDockVisible()) {
            enabled = true
            recalculateDockablePosition = true
        }
    }

    /**
     * Disables docking; the floater will no longer be repositioned to follow
     * the dock widget.  Mirrors `LLDockControl::off`.
     */
    fun off() {
        enabled = false
    }

    /**
     * Marks the dockable position as dirty so [repositionDockable] will
     * recalculate even if the rects have not changed.
     * Mirrors `LLDockControl::forceRecalculatePosition`.
     */
    fun forceRecalculatePosition() {
        recalculateDockablePosition = true
    }

    // ------------------------------------------------------------------
    // Dock widget access
    // ------------------------------------------------------------------

    /** Returns the current dock widget, or `null` if unset / destroyed. */
    fun getDock(): LLView? = dockWidgetHandle

    /**
     * Sets a new dock widget.  Immediately recalculates position if non-null.
     * Mirrors `LLDockControl::setDock`.
     */
    fun setDock(dockWidget: LLView?) {
        if (dockWidget != null) {
            dockWidgetHandle = dockWidget
            repositionDockable()
            dockWidgetVisible = isDockVisible()
        } else {
            dockWidgetHandle = null
            dockWidgetVisible = false
        }
    }

    /** Returns the side of the dock widget that this control docks against. */
    fun getDockAt(): DocAt = dockAt

    // ------------------------------------------------------------------
    // Visibility check
    // ------------------------------------------------------------------

    /**
     * Returns `true` when the dock widget is visible in the view hierarchy
     * and lies within its parent's horizontal extent (for TOP/BOTTOM modes).
     * Mirrors `LLDockControl::isDockVisible`.
     */
    fun isDockVisible(): Boolean {
        val dock = getDock() ?: return false
        if (!dock.isInVisibleChain()) return false

        return when (dockAt) {
            DocAt.TOP, DocAt.BOTTOM -> {
                val dockRect = dock.calcScreenRect()
                val parentRect = dock.rootView?.calcScreenRect() ?: return true
                dockRect.right > parentRect.left && dockRect.left < parentRect.right
            }
            else -> true
        }
    }

    // ------------------------------------------------------------------
    // Allowed-rect helper
    // ------------------------------------------------------------------

    /**
     * Fills [rect] with the bounding box within which the dockable floater
     * may be placed.  Defaults to the non-toolbar panel rect.
     * Mirrors `LLDockControl::getAllowedRect`.
     */
    fun getAllowedRect(rect: LLRect) {
        nonToolbarPanel?.let { rect.set(it.getRect()) }
    }

    // ------------------------------------------------------------------
    // Repositioning
    // ------------------------------------------------------------------

    /**
     * Recalculates and applies the floater's screen position so it stays
     * anchored to the dock widget.  Must be called once per layout pass.
     *
     * Undocks the floater automatically when the dock widget becomes
     * invisible.  Mirrors `LLDockControl::repositionDockable`.
     */
    fun repositionDockable() {
        val dock = getDock() ?: return
        val dockRect = dock.calcScreenRect()
        val currentFloaterRect = dockableFloater.calcScreenRect()
        val allowedRect = LLRect()
        (getAllowedRect ?: ::getAllowedRect).invoke(allowedRect)

        val changed =
            prevDockRect != dockRect ||
            dockWidgetVisible != isDockVisible() ||
            rootRect != allowedRect ||
            floaterRect != currentFloaterRect ||
            recalculateDockablePosition

        if (!changed) return

        if (!isDockVisible()) {
            dockableFloater.isDocked = false
            off()
            dockableFloater.onDockHidden()
        } else {
            if (enabled) moveDockable()
            dockableFloater.onDockShown()
        }

        prevDockRect = dockRect
        rootRect = allowedRect
        floaterRect = currentFloaterRect
        recalculateDockablePosition = false
        dockWidgetVisible = isDockVisible()
    }

    /**
     * Draws the tongue image between the floater and its dock widget.
     * Should be called from the floater's draw method when docked.
     * Mirrors `LLDockControl::drawToungue` (sic — original typo preserved).
     */
    fun drawTongue() {
        if (enabled && dockableFloater.useTongue) {
            dockTongue.draw(dockTongueX, dockTongueY)
        }
    }

    // ------------------------------------------------------------------
    // Tongue size accessors
    // ------------------------------------------------------------------

    /** Returns the width of the dock tongue image in pixels. */
    fun getTongueWidth(): Int = dockTongue.width

    /** Returns the height of the dock tongue image in pixels. */
    fun getTongueHeight(): Int = dockTongue.height

    // ------------------------------------------------------------------
    // Private layout calculation
    // ------------------------------------------------------------------

    /**
     * Calculates and applies a new position for the dockable floater so that
     * it sits adjacent to the dock widget on the configured [dockAt] side.
     *
     * Accounts for the tongue image size, root-view clamping, and floater
     * reshaping when vertical space is constrained.
     *
     * Mirrors `LLDockControl::moveDockable`.
     */
    private fun moveDockable() {
        val dock = getDock() ?: return
        val dockRect = dock.calcScreenRect()
        val allowedRect = LLRect()
        (getAllowedRect ?: ::getAllowedRect).invoke(allowedRect)

        val useTongue = dockableFloater.useTongue
        val dockableRect = dockableFloater.calcScreenRect()

        var x: Int
        var y: Int

        when (dockAt) {
            DocAt.LEFT -> {
                x = dockRect.left - dockableRect.width
                y = dockRect.centerY + dockableRect.height / 2
                if (useTongue) x -= dockTongue.width
                dockTongueX = dockableRect.right
                dockTongueY = dockableRect.centerY - dockTongue.height / 2
            }

            DocAt.RIGHT -> {
                x = dockRect.right
                y = dockRect.centerY + dockableRect.height / 2
                if (useTongue) x += dockTongue.width
                dockTongueX = dockRect.right
                dockTongueY = dockableRect.centerY - dockTongue.height / 2
            }

            DocAt.TOP -> {
                x = dockRect.centerX - dockableRect.width / 2
                y = dockRect.top + dockableRect.height
                if (useTongue) {
                    y += dockTongue.height
                    if (y > allowedRect.top) y = allowedRect.top
                }
                x = x.coerceAtLeast(allowedRect.left)
                if (x + dockableRect.width > allowedRect.right) {
                    x = allowedRect.right - dockableRect.width
                }
                val dockParentRect = dock.parent?.calcScreenRect() ?: LLRect()
                dockTongueX = when {
                    dockRect.centerX < dockParentRect.left ->
                        dockParentRect.left - dockTongue.width / 2
                    dockRect.centerX > dockParentRect.right ->
                        dockParentRect.right - dockTongue.width / 2
                    else ->
                        dockRect.centerX - dockTongue.width / 2
                }
                dockTongueY = dockRect.top
            }

            DocAt.BOTTOM -> {
                x = dockRect.centerX - dockableRect.width / 2
                y = dockRect.bottom
                if (useTongue) y -= dockTongue.height
                x = x.coerceAtLeast(allowedRect.left)
                if (x + dockableRect.width > allowedRect.right) {
                    x = allowedRect.right - dockableRect.width
                }
                val dockParentRect = dock.parent?.calcScreenRect() ?: LLRect()
                dockTongueX = when {
                    dockRect.centerX < dockParentRect.left ->
                        dockParentRect.left - dockTongue.width / 2
                    dockRect.centerX > dockParentRect.right ->
                        dockParentRect.right - dockTongue.width / 2
                    else ->
                        dockRect.centerX - dockTongue.width / 2
                }
                dockTongueY = dockRect.bottom - dockTongue.height
            }

            DocAt.NONE -> return
        }

        // Clamp floater height so it doesn't overlap the tongue.
        val maxAvailableHeight =
            allowedRect.height - (allowedRect.bottom - dockTongueY) - dockTongue.height

        val finalRect: LLRect = if (useTongue && dockableRect.height >= maxAvailableHeight) {
            dockableFloater.reshape(dockableRect.width, maxAvailableHeight)
            LLRect.fromLeftTopSize(x, y, dockableRect.width, maxAvailableHeight)
        } else {
            LLRect.fromLeftTopSize(x, y, dockableRect.width, dockableRect.height)
        }

        // Convert screen rect to floater-parent-local rect and apply.
        val localRect = dockableFloater.parent?.screenRectToLocal(finalRect) ?: finalRect
        dockableFloater.setRect(localRect)

        // Convert tongue coordinates to floater-local space.
        val (localTongueX, localTongueY) =
            dockableFloater.screenPointToLocal(dockTongueX, dockTongueY)
        dockTongueX = localTongueX
        dockTongueY = localTongueY
    }
}
