package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// Corresponds to: llchicletbar.h / llchicletbar.cpp
// Top-of-screen horizontal bar that hosts the ChicletPanel and IM/notification
// well buttons. Mirrors C++ LLChicletBar (LLSingleton<LLChicletBar>, LLPanel,
// LLIMSessionObserver).

// ── Chiclet-type enum (used by bar-level factory) ─────────────────────────────

/**
 * Discriminates the chiclet type when adding to [ChicletBar].
 * Corresponds to [IMChicletBase] session types plus non-IM variants.
 */
enum class ChicletType {
    /** Person-to-person IM session. */
    IM,
    /** Group chat session. */
    GROUP,
    /** Ad-hoc / conference chat session. */
    AD_HOC,
    /** Script notification chiclet. */
    SCRIPT,
    /** Inventory-offer notification chiclet. */
    INV_OFFER,
    /** Unknown / default. */
    UNKNOWN,
}

// ── Session observer interface ────────────────────────────────────────────────

/**
 * Mirrors C++ [LLIMSessionObserver].
 * Implemented by [ChicletBar] to react to session lifecycle events from IMMgr.
 */
interface IMSessionObserver {
    fun sessionAdded(sessionId: LLUUID, name: String, otherParticipantId: LLUUID, hasOfflineMsg: Boolean)
    fun sessionActivated(sessionId: LLUUID, name: String, otherParticipantId: LLUUID) {}
    fun sessionVoiceOrIMStarted(sessionId: LLUUID) {}
    fun sessionRemoved(sessionId: LLUUID)
    fun sessionIDUpdated(oldSessionId: LLUUID, newSessionId: LLUUID)
}

// ── ChicletBar singleton ──────────────────────────────────────────────────────

/**
 * Horizontal bar at the top of the viewer that contains the [ChicletPanel]
 * and well-notification buttons.
 *
 * Mirrors C++ [LLChicletBar] (LLSingleton + LLPanel + LLIMSessionObserver).
 *
 * Implemented as a Kotlin `object` (singleton) matching the C++ LLSingleton pattern.
 */
object ChicletBar : IMSessionObserver {

    // ── Child panels / controls ───────────────────────────────────────────────

    /** Hosted chiclet panel (mirrors C++ mChicletPanel). */
    val chicletPanel: ChicletPanel = ChicletPanel()

    /** Whether the chiclet bar itself is visible (can be hidden by preference). */
    var isVisible: Boolean = true
        private set

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Called after UI is built from XML (mirrors C++ postBuild).
     * Binds the internal ChicletPanel and layout stack.
     */
    fun postBuild(): Boolean {
        // TODO("GL: getChild<ChicletPanel>('chiclet_list_panel') → chicletPanel")
        // TODO("GL: getChild<LLLayoutStack>('toolbar_stack') → mToolbarStack")
        return true
    }

    // ── IMSessionObserver ─────────────────────────────────────────────────────

    /**
     * Called when a new IM session is opened.
     * Creates the appropriate chiclet and adds it to [chicletPanel].
     * Mirrors C++ LLChicletBar::sessionAdded().
     */
    override fun sessionAdded(
        sessionId: LLUUID,
        name: String,
        otherParticipantId: LLUUID,
        hasOfflineMsg: Boolean,
    ) {
        if (chicletPanel.findChiclet(sessionId) != null) return   // already present
        createIMChiclet(sessionId)
    }

    override fun sessionRemoved(sessionId: LLUUID) {
        removeChiclet(sessionId)
    }

    override fun sessionIDUpdated(oldSessionId: LLUUID, newSessionId: LLUUID) {
        if (chicletPanel.findChiclet(oldSessionId) == null) return
        removeChiclet(oldSessionId)
        val type = resolveChicletType(newSessionId)
        addChiclet(newSessionId, type)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Adds a chiclet of [type] for [sessionId] to the bar.
     * If a chiclet for [sessionId] already exists it is not duplicated.
     */
    fun addChiclet(sessionId: LLUUID, type: ChicletType): Chiclet? {
        val existing = chicletPanel.findChiclet(sessionId)
        if (existing != null) return existing

        val imType = when (type) {
            ChicletType.IM      -> IMSessionType.IM
            ChicletType.GROUP   -> IMSessionType.GROUP
            ChicletType.AD_HOC  -> IMSessionType.AD_HOC
            else                -> IMSessionType.UNKNOWN
        }
        return chicletPanel.createChiclet(sessionId, imType)
    }

    /**
     * Removes the chiclet for [sessionId] from the bar.
     */
    fun removeChiclet(sessionId: LLUUID) {
        chicletPanel.removeChiclet(sessionId)
    }

    /**
     * Returns the chiclet for [sessionId], or null if not present.
     */
    fun getChiclet(sessionId: LLUUID): Chiclet? =
        chicletPanel.findChiclet(sessionId)

    /**
     * Returns the total number of unread IM messages across all chiclets.
     * Mirrors C++ LLChicletBar::getTotalUnreadIMCount().
     */
    fun getTotalUnreadIMCount(): Int = chicletPanel.getTotalUnreadIMCount()

    /**
     * Shows or hides a named well button (IM or Notification well).
     * Mirrors C++ LLChicletBar::showWellButton().
     *
     * @param wellName  "im_well_panel" or "notification_well_panel"
     * @param visible   desired visibility.
     */
    fun showWellButton(wellName: String, visible: Boolean) {
        // TODO("GL: getChild<LLLayoutPanel>(wellName).isVisible = visible")
        // TODO("GL: mToolbarStack.updateLayout()")
    }

    /** Hides or shows IM/group chiclets based on a preference change. */
    fun updateVisibility(visible: Boolean) {
        isVisible = visible
        // TODO("GL: show/hide the chiclet panel and re-layout toolbar stack")
    }

    // ── IM chiclet factory ────────────────────────────────────────────────────

    /**
     * Creates and registers the correct chiclet subtype based on session type.
     * Mirrors C++ LLChicletBar::createIMChiclet().
     */
    fun createIMChiclet(sessionId: LLUUID): Chiclet {
        val sessionType = IMChicletBase.getIMSessionType(sessionId)
        return chicletPanel.createChiclet(sessionId, sessionType)
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    /**
     * Responds to bar width changes; shrinks the chiclet panel if needed.
     * Mirrors C++ LLChicletBar::reshape() → processWidthDecreased().
     */
    fun reshape(width: Int, height: Int, calledFromParent: Boolean = false) {
        // TODO("GL: call processWidthDecreased(delta) when width < previous width")
        fitWithTopInfoBar()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Shrinks child controls when the bar must reduce its total width.
     * Returns any remaining delta that could not be absorbed.
     * Mirrors C++ LLChicletBar::processWidthDecreased().
     */
    private fun processWidthDecreased(deltaWidth: Int): Int {
        // TODO("GL: reduce chiclet panel width by deltaWidth, clamped to minWidth")
        return 0
    }

    /**
     * Returns headroom (current panel width − minimum) for the chiclet panel.
     * Mirrors C++ LLChicletBar::getChicletPanelShrinkHeadroom().
     */
    private fun getChicletPanelShrinkHeadroom(): Int =
        chicletPanel.minWidth.coerceAtLeast(0)

    /**
     * Adjusts chiclet bar width to avoid overlapping the mini-location bar.
     * Mirrors C++ LLChicletBar::fitWithTopInfoBar().
     */
    private fun fitWithTopInfoBar() {
        // TODO("GL: query LLPanelTopInfoBar geometry and clamp our right edge")
    }

    /** Resolves [ChicletType] for [sessionId] via IMMgr. */
    private fun resolveChicletType(sessionId: LLUUID): ChicletType =
        when (IMChicletBase.getIMSessionType(sessionId)) {
            IMSessionType.IM      -> ChicletType.IM
            IMSessionType.GROUP   -> ChicletType.GROUP
            IMSessionType.AD_HOC  -> ChicletType.AD_HOC
            IMSessionType.UNKNOWN -> ChicletType.UNKNOWN
        }
}
