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
        // TODO("GL: start LLFlashTimer for this chiclet button")
    }

    // ── Toggle / activation ───────────────────────────────────────────────────

    open fun setToggleState(toggle: Boolean) {
        isActive = toggle
        // TODO("GL: update mChicletButton toggle state")
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
        TODO("GL: render chiclet icon, counter badge, and new-message overlay")
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
        // TODO("GL: set tooltip text to name")
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
        // TODO("GL: bind mSpeakerCtrl to otherParticipantId voice channel")
    }

    // ── New-message overlay ───────────────────────────────────────────────────

    open fun setShowNewMessagesIcon(show: Boolean) {
        showNewMessagesIcon = show
        // TODO("GL: show/hide mNewMessagesIcon overlay")
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
        // TODO("GL: measure counter + speaker widths and resize chiclet rect")
    }

    // ── Popup menu ────────────────────────────────────────────────────────────

    protected abstract fun createPopupMenu()

    open fun hidePopupMenu() {
        // TODO("GL: close mPopupMenuHandle")
    }

    override fun draw() {
        TODO("GL: render IM chiclet — icon, counter, speaker, new-message overlay")
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
        // TODO("GL: update mChicletIconCtrl with avatar id")
    }

    override fun initSpeakerControl() {
        // TODO("GL: bind mSpeakerCtrl to otherParticipantId")
    }

    override fun createPopupMenu() {
        // TODO("GL: build P2P context menu — View Profile, IM, Block, etc.")
    }
}

/**
 * Group chat chiclet.
 * Mirrors C++ [LLIMGroupChiclet] (also implements LLGroupMgrObserver).
 */
class GroupChiclet(sessionId: LLUUID) : IMChicletBase(sessionId) {

    fun setGroupSessionId(id: LLUUID) {
        this.sessionId = id
        // TODO("GL: subscribe to LLGroupMgr for group data changes (icon, name)")
    }

    override fun initSpeakerControl() {
        // TODO("GL: bind mSpeakerCtrl to current group speaker")
    }

    override fun createPopupMenu() {
        // TODO("GL: build Group context menu — Group Info, Leave Group, etc.")
    }

    override fun draw() {
        // TODO("GL: track current speaker and update speaker control before drawing")
    }

    /** Called by LLGroupMgr when group data (e.g. icon) changes. */
    fun onGroupChanged() {
        // TODO("GL: refresh group icon from LLGroupMgr data")
    }
}

/**
 * Ad-hoc (conference) chat chiclet.
 * Mirrors C++ [LLAdHocChiclet].
 */
class AdHocChiclet(sessionId: LLUUID) : IMChicletBase(sessionId) {

    override fun initSpeakerControl() {
        // TODO("GL: bind mSpeakerCtrl to current conference speaker")
    }

    override fun createPopupMenu() {
        // TODO("GL: build Ad-hoc context menu")
    }

    override fun draw() {
        // TODO("GL: switchToCurrentSpeaker() then render")
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
        // TODO("GL: build script chiclet context menu")
    }

    override fun onMouseDown() {
        super.onMouseDown()
        // TODO("GL: toggle script floater visibility")
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
        // TODO("GL: build inventory offer chiclet context menu")
    }

    override fun onMouseDown() {
        super.onMouseDown()
        // TODO("GL: toggle inventory offer floater")
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
        // TODO("GL: update button label with capped counter text (e.g. '9+')")
    }

    fun setToggleState(toggled: Boolean) {
        isActive = toggled
        // TODO("GL: update mButton toggle visual")
    }

    fun setNewMessagesState(newMessages: Boolean) {
        isNewMessagesState = newMessages
        updateWidget(!newMessages)
    }

    /** Override to update the well button appearance based on [isWindowEmpty]. */
    open fun updateWidget(isWindowEmpty: Boolean) {
        // TODO("GL: switch between 'lit' and 'unlit' button states")
    }

    @Suppress("UnusedParameter")
    protected fun changeLitState(blink: Boolean) {
        // TODO("GL: toggle lit/unlit visual for flash effect")
    }

    protected abstract fun createMenu()

    override fun draw() {
        TODO("GL: render well chiclet button with counter badge")
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
        // TODO("GL: build notification well context menu — Mark All Read, etc.")
    }
}

/**
 * IM well chiclet — shows total unread count across all IM sessions.
 * Mirrors C++ [LLIMWellChiclet].
 */
class IMWellChiclet(sessionId: LLUUID) : SysWellChiclet(sessionId) {

    override fun createMenu() {
        // TODO("GL: build IM well context menu")
    }

    fun messageCountChanged() {
        // TODO("sum unread participant message counts across IMMgr.sessions")
        val total = 0
        setCounter(total)
        // TODO("GL: updateApplicationWindowTitle() with unread badge")
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
        // TODO("GL: remove all child views from scroll area")
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
        // TODO("GL: position chiclet child views sequentially with chicletPadding")
    }

    private fun scrollToChiclet(chiclet: Chiclet) {
        // TODO("GL: scroll mScrollArea so that chiclet is visible")
    }

    fun scrollLeft() {
        // TODO("GL: shift chiclets right by scrollingOffset")
    }

    fun scrollRight() {
        // TODO("GL: shift chiclets left by scrollingOffset")
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
        TODO("GL: render scroll area, chiclet buttons, and scroll arrows")
    }
}
