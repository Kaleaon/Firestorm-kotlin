package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Rect
import com.firestorm.llmessage.IMType
import com.firestorm.llui.View

// Corresponds to: llchiclet.h / llchiclet.cpp
// Small notification widgets shown in the chiclet bar for IM/group chats,
// system notifications, inventory offers, etc.

// ── Session-type discriminator ────────────────────────────────────────────────

/**
 * Mirrors C++ [LLIMChiclet::EType].
 * Discriminates the kind of IM session a chiclet represents.
 */
enum class IMSessionType {
    UNKNOWN,
    /** Person-to-person instant message. */
    IM,
    /** Group chat. */
    GROUP,
    /** Ad-hoc (conference) chat. */
    AD_HOC,
}

// ── Observer interface ────────────────────────────────────────────────────────

/** Listener for chiclet size changes (mirrors C++ chiclet_size_changed_callback_t). */
fun interface ChicletSizeChangedListener {
    fun onChicletSizeChanged(chiclet: Chiclet)
}

// ── Base class ────────────────────────────────────────────────────────────────

/**
 * Base class for all chiclets.
 *
 * Mirrors C++ [LLChiclet] (extends LLUICtrl → View here).
 *
 * @param sessionId the chat/notification session this chiclet represents.
 */
open class Chiclet(sessionId: LLUUID) {

    var sessionId: LLUUID = sessionId
        protected set

    /** Whether the unread-count badge is shown. */
    var showCounter: Boolean = true
        protected set

    /** Whether the counter badge is enabled (can be suppressed per chiclet). */
    var counterEnabled: Boolean = true
        protected set

    /** Number of unread messages / notifications. */
    open var unreadCount: Int = 0
        protected set

    /** Whether this chiclet is the currently active (focused) session. */
    var isActive: Boolean = false
        protected set

    private val sizeChangedListeners: MutableList<ChicletSizeChangedListener> = mutableListOf()
    private val clickListeners: MutableList<() -> Unit> = mutableListOf()

    // ── Counter ───────────────────────────────────────────────────────────────

    /** Sets the number of unread notifications. Subclasses override to resize. */
    open fun setCounter(n: Int) {
        unreadCount = n.coerceAtLeast(0)
        onChicletSizeChanged()
    }

    open fun getCounter(): Int = unreadCount

    open fun setShowCounter(show: Boolean) {
        showCounter = show
    }

    open fun enableCounterControl(enable: Boolean) {
        counterEnabled = enable
    }

    // ── Flash ─────────────────────────────────────────────────────────────────

    /**
     * Briefly flashes the chiclet to draw the user's attention.
     * Mirrors C++ LLFlashTimer-driven blinking logic.
     */
    open fun flash() {
        // start LLFlashTimer for this chiclet button (GL stub)
    }

    // ── Toggle / activation ───────────────────────────────────────────────────

    open fun setToggleState(toggle: Boolean) {
        isActive = toggle
        // update mChicletButton toggle state (GL stub)
    }

    // ── Click callbacks ───────────────────────────────────────────────────────

    fun addClickListener(listener: () -> Unit) {
        clickListeners.add(listener)
    }

    protected open fun onMouseDown() {
        clickListeners.forEach { it() }
    }

    // ── Size-change notifications ─────────────────────────────────────────────

    fun addSizeChangedListener(listener: ChicletSizeChangedListener) {
        sizeChangedListeners.add(listener)
    }

    protected open fun onChicletSizeChanged() {
        sizeChangedListeners.forEach { it.onChicletSizeChanged(this) }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    open fun draw() {
        // render chiclet icon, counter badge, and new-message overlay (GL stub)
    }
}

// ── IM chiclet base ───────────────────────────────────────────────────────────

/**
 * Base for all Instant-Message chiclets (P2P, Group, Ad-hoc).
 *
 * Mirrors C++ [LLIMChiclet] (extends LLChiclet).
 */
abstract class IMChicletBase(sessionId: LLUUID) : Chiclet(sessionId) {

    var otherParticipantId: LLUUID = LLUUID()
        protected set

    var sessionName: String = ""
        protected set

    /** Whether the voice/speaker output monitor is visible. */
    var showSpeaker: Boolean = false
        protected set

    /** Width without counter or speaker control. */
    protected var defaultWidth: Int = 0

    var showNewMessagesIcon: Boolean = false
        private set

    // ── Participant / session ─────────────────────────────────────────────────

    open fun setOtherParticipantId(id: LLUUID) {
        otherParticipantId = id
    }

    open fun setIMSessionName(name: String) {
        sessionName = name
        // set tooltip text to name (GL stub)
    }

    // ── Speaker control ───────────────────────────────────────────────────────

    open fun setShowSpeaker(show: Boolean) {
        showSpeaker = show
        setRequiredWidth()
    }

    open fun toggleSpeakerControl() {
        setShowSpeaker(!showSpeaker)
    }

    open fun initSpeakerControl() {
        // bind mSpeakerCtrl to otherParticipantId voice channel (GL stub)
    }

    // ── New-message overlay ───────────────────────────────────────────────────

    open fun setShowNewMessagesIcon(show: Boolean) {
        showNewMessagesIcon = show
        // show/hide mNewMessagesIcon overlay (GL stub)
    }

    open fun getShowNewMessagesIcon(): Boolean = showNewMessagesIcon

    // ── Counter (override for width reflow) ───────────────────────────────────

    override fun setCounter(n: Int) {
        super.setCounter(n)
        setRequiredWidth()
    }

    override fun setShowCounter(show: Boolean) {
        super.setShowCounter(show)
        setRequiredWidth()
    }

    open fun toggleCounterControl() {
        setShowCounter(!showCounter)
    }

    /** Recalculates and sets the chiclet width based on visible sub-controls. */
    open fun setRequiredWidth() {
        // measure counter + speaker widths and resize chiclet rect (GL stub)
    }

    // ── Popup menu ────────────────────────────────────────────────────────────

    protected abstract fun createPopupMenu()

    open fun hidePopupMenu() {
        // close mPopupMenuHandle (GL stub)
    }

    override fun draw() {
        // render IM chiclet — icon, counter, speaker, new-message overlay (GL stub)
    }

    // ── Static factory helper ─────────────────────────────────────────────────

    companion object {
        /**
         * Determines session type from [sessionId] by consulting IMMgr.
         * Mirrors C++ [LLIMChiclet::getIMSessionType].
         */
        fun getIMSessionType(sessionId: LLUUID): IMSessionType {
            val session = IMMgr.getSession(sessionId) ?: return IMSessionType.UNKNOWN
            return when (session.type) {
                IMType.SESSION_GROUP_START      -> IMSessionType.GROUP
                IMType.SESSION_CONFERENCE_START -> IMSessionType.AD_HOC
                IMType.SESSION_P2P_INVITE,
                IMType.SESSION_SEND,
                IMType.SESSION_INVITE           -> IMSessionType.IM
                else                            -> IMSessionType.IM
            }
        }
    }
}

// ── Concrete IM chiclet types ─────────────────────────────────────────────────

/**
 * Person-to-person IM chiclet.
 * Mirrors C++ [LLIMP2PChiclet].
 */
class P2PChiclet(sessionId: LLUUID) : IMChicletBase(sessionId) {

    override fun setOtherParticipantId(id: LLUUID) {
        super.setOtherParticipantId(id)
        // update mChicletIconCtrl with avatar id (GL stub)
    }

    override fun initSpeakerControl() {
        // bind mSpeakerCtrl to otherParticipantId (GL stub)
    }

    override fun createPopupMenu() {
        // build P2P context menu — View Profile, IM, Block, etc. (GL stub)
    }
}

/**
 * Group chat chiclet.
 * Mirrors C++ [LLIMGroupChiclet] (also implements LLGroupMgrObserver).
 */
class GroupChiclet(sessionId: LLUUID) : IMChicletBase(sessionId) {

    fun setGroupSessionId(id: LLUUID) {
        this.sessionId = id
        // subscribe to LLGroupMgr for group data changes (icon, name) (GL stub)
    }

    override fun initSpeakerControl() {
        // bind mSpeakerCtrl to current group speaker (GL stub)
    }

    override fun createPopupMenu() {
        // build Group context menu — Group Info, Leave Group, etc. (GL stub)
    }

    override fun draw() {
        // track current speaker and update speaker control before drawing (GL stub)
    }

    /** Called by LLGroupMgr when group data (e.g. icon) changes. */
    fun onGroupChanged() {
        // refresh group icon from LLGroupMgr data (GL stub)
    }
}

/**
 * Ad-hoc (conference) chat chiclet.
 * Mirrors C++ [LLAdHocChiclet].
 */
class AdHocChiclet(sessionId: LLUUID) : IMChicletBase(sessionId) {

    override fun initSpeakerControl() {
        // bind mSpeakerCtrl to current conference speaker (GL stub)
    }

    override fun createPopupMenu() {
        // build Ad-hoc context menu (GL stub)
    }

    override fun draw() {
        // switchToCurrentSpeaker() then render (GL stub)
    }
}

// ── Script / inventory-offer chiclets ─────────────────────────────────────────

/**
 * Chiclet for script-floater notifications.
 * Mirrors C++ [LLScriptChiclet].
 */
class ScriptChiclet(sessionId: LLUUID) : IMChicletBase(sessionId) {

    override fun setCounter(n: Int) {
        // Script chiclets don't show a count badge — intentionally no-op.
    }

    override fun getCounter(): Int = 0

    override fun createPopupMenu() {
        // build script chiclet context menu (GL stub)
    }

    override fun onMouseDown() {
        super.onMouseDown()
        // toggle script floater visibility (GL stub)
    }
}

/**
 * Chiclet for inventory-offer notifications.
 * Mirrors C++ [LLInvOfferChiclet].
 */
class InvOfferChiclet(sessionId: LLUUID) : IMChicletBase(sessionId) {

    override fun setCounter(n: Int) {
        // Inv-offer chiclets don't show a count badge — intentionally no-op.
    }

    override fun getCounter(): Int = 0

    override fun createPopupMenu() {
        // build inventory offer chiclet context menu (GL stub)
    }

    override fun onMouseDown() {
        super.onMouseDown()
        // toggle inventory offer floater (GL stub)
    }
}

// ── System-well chiclets ──────────────────────────────────────────────────────

/**
 * "Well" chiclet that aggregates counts across many sessions or notifications.
 * Mirrors C++ [LLSysWellChiclet].
 */
abstract class SysWellChiclet(sessionId: LLUUID) : Chiclet(sessionId) {

    protected var maxDisplayedCount: Int = 9
    protected var isNewMessagesState: Boolean = false

    override fun setCounter(n: Int) {
        super.setCounter(n)
        updateWidget(n == 0)
        // update button label with capped counter text (e.g. '9+') (GL stub)
    }

    override fun setToggleState(toggled: Boolean) {
        isActive = toggled
        // update mButton toggle visual (GL stub)
    }

    fun setNewMessagesState(newMessages: Boolean) {
        isNewMessagesState = newMessages
        updateWidget(!newMessages)
    }

    /** Override to update the well button appearance based on [isWindowEmpty]. */
    open fun updateWidget(isWindowEmpty: Boolean) {
        // switch between 'lit' and 'unlit' button states (GL stub)
    }

    @Suppress("UnusedParameter")
    protected fun changeLitState(blink: Boolean) {
        // toggle lit/unlit visual for flash effect (GL stub)
    }

    protected abstract fun createMenu()

    override fun draw() {
        // render well chiclet button with counter badge (GL stub)
    }
}

/**
 * Notification well chiclet — aggregates all system notifications.
 * Mirrors C++ [LLNotificationChiclet].
 */
class NotificationChiclet(sessionId: LLUUID) : SysWellChiclet(sessionId) {

    private var unreadSystemNotifications: Int = 0

    override fun setCounter(n: Int) {
        unreadSystemNotifications = n
        super.setCounter(n)
    }

    override fun createMenu() {
        // build notification well context menu — Mark All Read, etc. (GL stub)
    }
}

/**
 * IM well chiclet — shows total unread count across all IM sessions.
 * Mirrors C++ [LLIMWellChiclet].
 */
class IMWellChiclet(sessionId: LLUUID) : SysWellChiclet(sessionId) {

    override fun createMenu() {
        // build IM well context menu (GL stub)
    }

    fun messageCountChanged() {
        // sum unread participant message counts across IMMgr.sessions (GL stub)
        val total = 0
        setCounter(total)
        // updateApplicationWindowTitle() with unread badge (GL stub)
    }
}

// ── Chiclet panel ─────────────────────────────────────────────────────────────

/**
 * Scrollable panel that hosts a list of [Chiclet] widgets.
 *
 * Mirrors C++ [LLChicletPanel] (extends LLPanel).
 * Manages creation, removal, scrolling, and layout of chiclets.
 */
class ChicletPanel {

    private val chiclets: MutableList<Chiclet> = mutableListOf()

    var chicletPadding: Int  = 3
    var scrollingOffset: Int = 10
    var minWidth: Int        = 0

    private val clickListeners: MutableList<(Chiclet) -> Unit> = mutableListOf()

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Creates a [Chiclet] of [type] for [sessionId] and inserts at [index].
     * Pass [index] = -1 to append at the end (mirrors C++ createChiclet<T>).
     */
    fun createChiclet(sessionId: LLUUID, type: IMSessionType, index: Int = -1): Chiclet {
        val chiclet: Chiclet = when (type) {
            IMSessionType.IM      -> P2PChiclet(sessionId)
            IMSessionType.GROUP   -> GroupChiclet(sessionId)
            IMSessionType.AD_HOC  -> AdHocChiclet(sessionId)
            IMSessionType.UNKNOWN -> Chiclet(sessionId)
        }
        val insertAt = if (index < 0 || index > chiclets.size) chiclets.size else index
        chiclets.add(insertAt, chiclet)
        chiclet.addSizeChangedListener { arrange() }
        chiclet.addClickListener { clickListeners.forEach { cb -> cb(chiclet) } }
        arrange()
        scrollToChiclet(chiclet)
        return chiclet
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    fun findChiclet(sessionId: LLUUID): Chiclet? =
        chiclets.firstOrNull { it.sessionId == sessionId }

    fun getChiclet(index: Int): Chiclet? = chiclets.getOrNull(index)

    fun getChicletCount(): Int = chiclets.size

    fun getChicletIndex(chiclet: Chiclet): Int = chiclets.indexOf(chiclet)

    fun setChicletIndex(chiclet: Chiclet, index: Int) {
        val current = chiclets.indexOf(chiclet)
        if (current < 0) return
        chiclets.removeAt(current)
        chiclets.add(index.coerceIn(0, chiclets.size), chiclet)
        arrange()
    }

    fun getTotalUnreadIMCount(): Int = chiclets.sumOf { it.getCounter() }

    // ── Removal ───────────────────────────────────────────────────────────────

    fun removeChiclet(index: Int) {
        if (index in chiclets.indices) {
            chiclets.removeAt(index)
            arrange()
        }
    }

    fun removeChiclet(chiclet: Chiclet) {
        if (chiclets.remove(chiclet)) arrange()
    }

    fun removeChiclet(sessionId: LLUUID) {
        if (chiclets.removeAll { it.sessionId == sessionId }) arrange()
    }

    fun removeAll() {
        chiclets.clear()
        // remove all child views from scroll area (GL stub)
    }

    // ── Toggle states ─────────────────────────────────────────────────────────

    /** Activates chiclet for [sessionId] and deactivates all others. */
    fun setChicletToggleState(sessionId: LLUUID, toggle: Boolean) {
        chiclets.forEach { c ->
            c.setToggleState(c.sessionId == sessionId && toggle)
        }
    }

    // ── Layout / scroll ───────────────────────────────────────────────────────

    private fun arrange() {
        // position chiclet child views sequentially with chicletPadding (GL stub)
    }

    private fun scrollToChiclet(chiclet: Chiclet) {
        // scroll mScrollArea so that chiclet is visible (GL stub)
    }

    fun scrollLeft() {
        // shift chiclets right by scrollingOffset (GL stub)
    }

    fun scrollRight() {
        // shift chiclets left by scrollingOffset (GL stub)
    }

    fun onCurrentVoiceChannelChanged(sessionId: LLUUID) {
        chiclets.forEach { c ->
            if (c is IMChicletBase) c.setShowSpeaker(c.sessionId == sessionId)
        }
    }

    // ── Click callback ────────────────────────────────────────────────────────

    fun addChicletClickedListener(listener: (Chiclet) -> Unit) {
        clickListeners.add(listener)
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    fun draw() {
        // render scroll area, chiclet buttons, and scroll arrows (GL stub)
    }
}
