package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Mute type from LLMuteList – replicated here for chiclet session typing
// ---------------------------------------------------------------------------

enum class EInstantMessage {
    IM_NOTHING_SPECIAL,
    IM_SESSION_P2P_INVITE,
    IM_SESSION_GROUP_START,
    IM_SESSION_INVITE,
    IM_SESSION_CONFERENCE_START,
    IM_COUNT
}

// ---------------------------------------------------------------------------
// LLChicletNotificationCounterCtrl
// ---------------------------------------------------------------------------

open class LLChicletNotificationCounterCtrl(
    private val maxDisplayedCount: Int = 99
) {
    private var mCounter: Int = 0
    private var mInitialWidth: Int = 0

    open fun setCounter(counter: Int) {
        mCounter = counter
        val text = if (counter != 0) {
            val capped = minOf(counter, maxDisplayedCount)
            val suffix = if (counter > maxDisplayedCount) "+" else ""
            "$capped$suffix"
        } else {
            ""
        }
        TODO("GPU: set text label to '$text'")
    }

    open fun getCounter(): Int = mCounter

    fun getRequiredRect(): IntArray {
        TODO("GPU: measure text pixel width and return rect")
    }

    open fun setValue(value: Any?) {
        if (value is Int) setCounter(value)
    }

    open fun getValue(): Int = getCounter()
}

// ---------------------------------------------------------------------------
// LLChicletAvatarIconCtrl
// ---------------------------------------------------------------------------

open class LLChicletAvatarIconCtrl {
    open fun setValue(value: Any?) {
        TODO("GPU: set avatar icon texture for value=$value")
    }
}

// ---------------------------------------------------------------------------
// LLChicletGroupIconCtrl
// ---------------------------------------------------------------------------

open class LLChicletGroupIconCtrl(
    private val defaultIcon: String = "Generic_Group"
) {
    open fun setValue(value: Any?) {
        val id = value as? UUID
        if (id == null || id == UUID(0, 0)) {
            TODO("GPU: set icon texture to '$defaultIcon'")
        } else {
            TODO("GPU: set icon texture for group id=$id")
        }
    }
}

// ---------------------------------------------------------------------------
// LLChicletInvOfferIconCtrl
// ---------------------------------------------------------------------------

open class LLChicletInvOfferIconCtrl(
    private val defaultIcon: String = "Generic_Object_Small"
) : LLChicletAvatarIconCtrl() {
    override fun setValue(value: Any?) {
        val id = value as? UUID
        if (id == null || id == UUID(0, 0)) {
            TODO("GPU: set icon texture to '$defaultIcon'")
        } else {
            super.setValue(value)
        }
    }
}

// ---------------------------------------------------------------------------
// LLChicletSpeakerCtrl
// ---------------------------------------------------------------------------

open class LLChicletSpeakerCtrl {
    fun setSpeakerId(id: UUID) {
        TODO("GPU: update output-monitor speaker indicator for id=$id")
    }

    fun setVisible(visible: Boolean) {
        TODO("GPU: show/hide speaker control, visible=$visible")
    }
}

// ---------------------------------------------------------------------------
// LLChiclet  (abstract base)
// ---------------------------------------------------------------------------

abstract class LLChiclet(
    showCounter: Boolean = true,
    enableCounter: Boolean = false
) {
    var sessionId: UUID = UUID(0, 0)
        open set(value) { field = value }

    protected var mShowCounter: Boolean = showCounter

    private val mChicletSizeChangedListeners: MutableList<(LLChiclet, Any?) -> Unit> = mutableListOf()

    open fun setShowCounter(show: Boolean) {
        mShowCounter = show
    }

    open fun getShowCounter(): Boolean = mShowCounter

    fun setLeftButtonClickCallback(cb: (Any?, Any?) -> Unit) {
        TODO("GPU: register left-click callback on chiclet button")
    }

    fun setChicletSizeChangedCallback(cb: (LLChiclet, Any?) -> Unit) {
        mChicletSizeChangedListeners.add(cb)
    }

    open fun getValue(): UUID = sessionId

    open fun setValue(value: Any?) {
        if (value is UUID) sessionId = value
    }

    abstract fun setCounter(counter: Int)
    abstract fun getCounter(): Int

    protected open fun handleMouseDown(x: Int, y: Int): Boolean {
        onChicletSizeChanged()
        TODO("GPU: forward mouse-down to child controls")
    }

    protected open fun onChicletSizeChanged() {
        mChicletSizeChangedListeners.forEach { it(this, getValue()) }
    }
}

// ---------------------------------------------------------------------------
// LLIMChiclet  (abstract base for IM chiclets)
// ---------------------------------------------------------------------------

abstract class LLIMChiclet(
    showCounter: Boolean = true,
    enableCounter: Boolean = false
) : LLChiclet(showCounter, enableCounter) {

    enum class EType {
        TYPE_UNKNOWN, TYPE_IM, TYPE_GROUP, TYPE_AD_HOC
    }

    protected var mShowSpeaker: Boolean = false
    protected var mCounterEnabled: Boolean = enableCounter
    protected var mDefaultWidth: Int = 0

    protected var mNewMessagesIcon: Any? = null
    protected var mChicletButton: Any? = null
    protected var mCounterCtrl: LLChicletNotificationCounterCtrl = LLChicletNotificationCounterCtrl()
    protected var mSpeakerCtrl: LLChicletSpeakerCtrl = LLChicletSpeakerCtrl()
    protected var mOtherParticipantId: UUID = UUID(0, 0)
    protected var mPopupMenuHandle: Any? = null

    open fun postBuild(): Boolean {
        TODO("GPU: bind chiclet_button child control and wire commit/double-click callbacks to onMouseDown()")
    }

    open fun setIMSessionName(name: String) {
        TODO("GPU: set tooltip to '$name'")
    }

    open fun setOtherParticipantId(otherParticipantId: UUID) {
        mOtherParticipantId = otherParticipantId
    }

    open fun getOtherParticipantId(): UUID = mOtherParticipantId

    open fun enableCounterControl(enable: Boolean) {
        mCounterEnabled = enable
        if (!enable) {
            super.setShowCounter(false)
        }
    }

    open fun setRequiredWidth() {
        var required = mDefaultWidth
        if (getShowCounter()) {
            TODO("GPU: add mCounterCtrl rect width to required")
        }
        if (getShowSpeaker()) {
            TODO("GPU: add mSpeakerCtrl rect width to required")
        }
        TODO("GPU: reshape chiclet to required width")
    }

    open fun setShowNewMessagesIcon(show: Boolean) {
        TODO("GPU: set mNewMessagesIcon visible=$show; call setRequiredWidth()")
    }

    open fun getShowNewMessagesIcon(): Boolean {
        TODO("GPU: return mNewMessagesIcon visibility")
    }

    open fun onMouseDown() {
        TODO("GPU: call FSFloaterIM.toggle(sessionId); setCounter(0)")
    }

    open fun setToggleState(toggle: Boolean) {
        TODO("GPU: set mChicletButton toggle state to $toggle")
    }

    open fun setShowSpeaker(show: Boolean) {
        val needsResize = getShowSpeaker() != show
        if (needsResize) mShowSpeaker = show
        toggleSpeakerControl()
    }

    open fun getShowSpeaker(): Boolean = mShowSpeaker

    open fun initSpeakerControl() {}

    override fun setShowCounter(show: Boolean) {
        if (!mCounterEnabled) return
        val needsResize = getShowCounter() != show
        if (needsResize) {
            super.setShowCounter(show)
            toggleCounterControl()
        }
    }

    open fun toggleSpeakerControl() {
        if (getShowSpeaker()) {
            TODO("GPU: reposition mSpeakerCtrl to the right of the chiclet icon (and counter if shown), then call initSpeakerControl()")
        } else {
            mSpeakerCtrl.setSpeakerId(UUID(0, 0))
        }
        setRequiredWidth()
        mSpeakerCtrl.setVisible(getShowSpeaker())
    }

    override fun setCounter(counter: Int) {
        if (mCounterCtrl.getCounter() == counter) return
        mCounterCtrl.setCounter(counter)
        setShowCounter(counter != 0)
        setShowNewMessagesIcon(counter != 0)
    }

    open fun toggleCounterControl() {
        setRequiredWidth()
        TODO("GPU: set mCounterCtrl visible=${getShowCounter()}")
    }

    open fun draw() {
        TODO("GPU: LLUICtrl.draw()")
    }

    open fun handleRightMouseDown(x: Int, y: Int): Boolean {
        if (mPopupMenuHandle == null) createPopupMenu()
        TODO("GPU: call updateMenuItems(); menu.arrangeAndClear(); LLMenuGL.showPopup()")
    }

    fun hidePopupMenu() {
        TODO("GPU: set popup menu visible=false")
    }

    protected fun canCreateMenu(): Boolean {
        if (mPopupMenuHandle != null) return false
        return sessionId != UUID(0, 0)
    }

    protected abstract fun createPopupMenu()

    protected open fun updateMenuItems() {}

    companion object {
        val sFindChicletsListeners: MutableList<(UUID) -> List<LLChiclet>> = mutableListOf()

        fun getIMSessionType(sessionId: UUID): EType {
            if (sessionId == UUID(0, 0)) return EType.TYPE_UNKNOWN
            TODO("APR: query LLIMModel for session type and map to EType")
        }
    }
}

// ---------------------------------------------------------------------------
// LLScriptChiclet
// ---------------------------------------------------------------------------

class LLScriptChiclet : LLIMChiclet() {
    private var mChicletIconCtrl: Any? = null

    override fun setSessionId(value: UUID) {
        setShowNewMessagesIcon(sessionId != value)
        super.sessionId = value
        TODO("GPU: set tooltip to LLScriptFloaterManager.getObjectName(sessionId)")
    }

    override fun onMouseDown() {
        TODO("GPU: call LLScriptFloaterManager.getInstance().toggleScriptFloater(sessionId)")
    }

    override fun setCounter(counter: Int) {
        setShowNewMessagesIcon(counter > 0)
    }

    override fun getCounter(): Int = 0

    override fun createPopupMenu() {
        if (!canCreateMenu()) return
        TODO("GPU: load menu_script_chiclet.xml and store handle in mPopupMenuHandle; register ScriptChiclet.Action → onMenuItemClicked")
    }

    private fun onMenuItemClicked(userData: String) {
        when (userData) {
            "end" -> TODO("GPU: LLScriptFloaterManager.instance.removeNotification(sessionId)")
            "close all" -> TODO("GPU: LLIMWellWindow.getInstance().closeAll()")
        }
    }
}

// ---------------------------------------------------------------------------
// LLInvOfferChiclet
// ---------------------------------------------------------------------------

class LLInvOfferChiclet : LLIMChiclet() {
    private var mChicletIconCtrl: LLChicletInvOfferIconCtrl = LLChicletInvOfferIconCtrl()

    companion object {
        private const val INVENTORY_USER_OFFER = "UserGiveItem"
    }

    override fun setSessionId(value: UUID) {
        setShowNewMessagesIcon(sessionId != value)
        TODO("GPU: set tooltip to LLScriptFloaterManager.getObjectName(value)")
        super.sessionId = value
        TODO("GPU: find notification by sessionId; if it's UserGiveItem set icon to from_id else UUID null")
    }

    override fun onMouseDown() {
        TODO("GPU: call LLScriptFloaterManager.instance.toggleScriptFloater(sessionId)")
    }

    override fun setCounter(counter: Int) {
        setShowNewMessagesIcon(counter > 0)
    }

    override fun getCounter(): Int = 0

    override fun createPopupMenu() {
        if (!canCreateMenu()) return
        TODO("GPU: load menu_inv_offer_chiclet.xml; register InvOfferChiclet.Action → onMenuItemClicked")
    }

    private fun onMenuItemClicked(userData: String) {
        when (userData) {
            "end" -> TODO("GPU: LLScriptFloaterManager.instance.removeNotification(sessionId)")
        }
    }
}

// ---------------------------------------------------------------------------
// LLIMP2PChiclet
// ---------------------------------------------------------------------------

class LLIMP2PChiclet : LLIMChiclet() {
    private var mChicletIconCtrl: LLChicletAvatarIconCtrl = LLChicletAvatarIconCtrl()

    override fun setOtherParticipantId(otherParticipantId: UUID) {
        super.setOtherParticipantId(otherParticipantId)
        mChicletIconCtrl.setValue(getOtherParticipantId())
    }

    override fun initSpeakerControl() {
        mSpeakerCtrl.setSpeakerId(getOtherParticipantId())
    }

    override fun getCounter(): Int = mCounterCtrl.getCounter()

    override fun createPopupMenu() {
        if (!canCreateMenu()) return
        TODO("GPU: load menu_fs_imchiclet_p2p.xml; register IMChicletMenu.Action → onMenuItemClicked")
    }

    override fun updateMenuItems() {
        if (mPopupMenuHandle == null || sessionId == UUID(0, 0)) return
        TODO("GPU: enable/disable 'Send IM' based on open floater visibility; enable/disable 'Add Friend' based on LLAvatarActions.isFriend()")
    }

    private fun onMenuItemClicked(userData: String) {
        when (userData) {
            "profile" -> TODO("GPU: LLAvatarActions.showProfile(otherParticipantId)")
            "im"      -> TODO("GPU: LLAvatarActions.startIM(otherParticipantId)")
            "add"     -> TODO("GPU: LLAvatarActions.requestFriendshipDialog(otherParticipantId)")
            "end"     -> TODO("GPU: LLAvatarActions.endIM(otherParticipantId)")
        }
    }
}

// ---------------------------------------------------------------------------
// LLAdHocChiclet
// ---------------------------------------------------------------------------

class LLAdHocChiclet : LLIMChiclet() {
    private var mChicletIconCtrl: LLChicletAvatarIconCtrl = LLChicletAvatarIconCtrl()

    override fun setSessionId(value: UUID) {
        super.sessionId = value
        TODO("GPU: find im session for value; set mChicletIconCtrl value to session.otherParticipantId")
    }

    override fun draw() {
        switchToCurrentSpeaker()
        super.draw()
    }

    override fun initSpeakerControl() {
        switchToCurrentSpeaker()
    }

    override fun getCounter(): Int = mCounterCtrl.getCounter()

    private fun switchToCurrentSpeaker() {
        TODO("GPU: get speaker list for session; find speaker with volume>0 or STATUS_SPEAKING; call mSpeakerCtrl.setSpeakerId()")
    }

    override fun createPopupMenu() {
        if (!canCreateMenu()) return
        TODO("GPU: load menu_fs_imchiclet_adhoc.xml; register IMChicletMenu.Action → onMenuItemClicked")
    }

    private fun onMenuItemClicked(userData: String) {
        when (userData) {
            "end" -> TODO("GPU: LLGroupActions.endIM(sessionId)")
        }
    }
}

// ---------------------------------------------------------------------------
// LLIMGroupChiclet
// ---------------------------------------------------------------------------

class LLIMGroupChiclet : LLIMChiclet() {
    private var mChicletIconCtrl: LLChicletGroupIconCtrl = LLChicletGroupIconCtrl()

    override fun setSessionId(value: UUID) {
        super.sessionId = value
        TODO("GPU: fetch group data; if insignia id is not null set icon; otherwise register as group mgr observer and request properties")
    }

    fun changed(gc: Int) {
        // gc == GC_PROPERTIES
        if (gc == 0 /* GC_PROPERTIES */) {
            TODO("GPU: re-fetch group data and update mChicletIconCtrl with insignia id")
        }
    }

    override fun draw() {
        if (getShowSpeaker()) switchToCurrentSpeaker()
        super.draw()
    }

    override fun initSpeakerControl() {
        switchToCurrentSpeaker()
    }

    override fun getCounter(): Int = mCounterCtrl.getCounter()

    private fun switchToCurrentSpeaker() {
        TODO("GPU: get speaker list for session; find active speaker; call mSpeakerCtrl.setSpeakerId()")
    }

    override fun createPopupMenu() {
        if (!canCreateMenu()) return
        TODO("GPU: load menu_fs_imchiclet_group.xml; register IMChicletMenu.Action → onMenuItemClicked")
    }

    override fun updateMenuItems() {
        if (mPopupMenuHandle == null || sessionId == UUID(0, 0)) return
        TODO("GPU: enable/disable 'Chat' based on whether open IM floater is visible")
    }

    private fun onMenuItemClicked(userData: String) {
        when (userData) {
            "group chat" -> TODO("GPU: LLGroupActions.startIM(sessionId)")
            "info"       -> TODO("GPU: LLGroupActions.show(sessionId)")
            "snooze"     -> TODO("GPU: LLGroupActions.snoozeIM(sessionId)")
            "leave"      -> TODO("GPU: LLGroupActions.leaveIM(sessionId)")
            "end"        -> TODO("GPU: LLGroupActions.endIM(sessionId)")
        }
    }
}

// ---------------------------------------------------------------------------
// LLSysWellChiclet  (abstract)
// ---------------------------------------------------------------------------

abstract class LLSysWellChiclet(
    maxDisplayedCount: Int = 99
) : LLChiclet() {
    protected var mCounter: Int = 0
    protected val mMaxDisplayedCount: Int = maxDisplayedCount
    protected var mIsNewMessagesState: Boolean = false
    protected var mContextMenuHandle: Any? = null
    protected var mButton: Any? = null

    override fun setCounter(counter: Int) {
        if (counter == mCounter) return
        val label = if (counter != 0) {
            val capped = minOf(counter, mMaxDisplayedCount)
            val suffix = if (counter > mMaxDisplayedCount) "+" else ""
            "$capped$suffix"
        } else { "" }
        TODO("GPU: set mButton label to '$label'")
        mCounter = counter
    }

    override fun getCounter(): Int = mCounter

    fun setClickCallback(cb: (Any?, Any?) -> Unit) {
        TODO("GPU: wire cb to mButton click signal")
    }

    fun setToggleState(toggled: Boolean) {
        TODO("GPU: set mButton toggle state to $toggled")
    }

    fun changeLitState(blink: Boolean) {
        setNewMessagesState(!mIsNewMessagesState)
    }

    fun setNewMessagesState(newMessages: Boolean) {
        TODO("GPU: set mButton forcePressedState=$newMessages")
        mIsNewMessagesState = newMessages
    }

    open fun updateWidget(isWindowEmpty: Boolean) {
        TODO("GPU: set mButton enabled=${!isWindowEmpty}; call LLChicletBar.showWellButton(name, !isWindowEmpty)")
    }

    open fun handleRightMouseDown(x: Int, y: Int): Boolean {
        if (mContextMenuHandle == null) createMenu()
        TODO("GPU: show context menu at x=$x y=$y")
    }

    protected abstract fun createMenu()
}

// ---------------------------------------------------------------------------
// LLNotificationChiclet
// ---------------------------------------------------------------------------

class LLNotificationChiclet(maxDisplayedCount: Int = 99) : LLSysWellChiclet(maxDisplayedCount) {
    var mUreadSystemNotifications: Int = 0

    init {
        TODO("GPU: create ChicletNotificationChannel; set sys-well chiclet on LLFloaterNotificationsTabbed or LLNotificationWellWindow depending on FSInternalLegacyNotificationWell setting")
    }

    override fun setCounter(counter: Int) {
        super.setCounter(counter)
        updateWidget(getCounter() == 0)
    }

    private fun onMenuItemClicked(userData: String) {
        when (userData) {
            "close all" -> TODO("GPU: call closeAll() on notification well window (tabbed or legacy)")
        }
    }

    private fun enableMenuItem(userData: String): Boolean {
        if (userData == "can close all") return mUreadSystemNotifications != 0
        return true
    }

    override fun createMenu() {
        if (mContextMenuHandle != null) return
        TODO("GPU: load menu_notification_well_button.xml; register NotificationWellChicletMenu.Action → onMenuItemClicked; store handle")
    }

    fun filterNotification(notificationName: String, notificationType: String, hasFormElements: Boolean, isCancelled: Boolean): Boolean {
        TODO("APR: apply notification filter logic (ScriptDialog exclusion, well-window membership check, offer type RLVa routing)")
    }
}

// ---------------------------------------------------------------------------
// LLIMWellChiclet
// ---------------------------------------------------------------------------

class LLIMWellChiclet(maxDisplayedCount: Int = 99) : LLSysWellChiclet(maxDisplayedCount) {
    init {
        TODO("APR: register messageCountChanged as new-message and no-unread-message callback on LLIMModel; add this as session observer; wire FSShowMessageCountInWindowTitle setting change to updateApplicationWindowTitle()")
    }

    fun sessionAdded(sessionId: UUID, name: String, otherParticipantId: UUID, hasOfflineMsg: Boolean) {}

    fun sessionActivated(sessionId: UUID, name: String, otherParticipantId: UUID) {}

    fun sessionVoiceOrIMStarted(sessionId: UUID) {}

    fun sessionRemoved(sessionId: UUID) {
        messageCountChanged(emptyMap<String, Any>())
    }

    fun sessionIDUpdated(oldSessionId: UUID, newSessionId: UUID) {}

    private fun onMenuItemClicked(userData: String) {
        when (userData) {
            "close all" -> TODO("GPU: LLIMWellWindow.getInstance().closeAll()")
        }
    }

    private fun enableMenuItem(userData: String): Boolean {
        if (userData == "can close all") {
            TODO("GPU: return !LLIMWellWindow.getInstance().isWindowEmpty()")
        }
        return true
    }

    override fun createMenu() {
        if (mContextMenuHandle != null) return
        TODO("GPU: load menu_fs_im_well_button.xml; register IMWellChicletMenu.Action → onMenuItemClicked; store handle")
    }

    private fun messageCountChanged(sessionData: Map<String, Any>) {
        TODO("APR: guard on LLChicletBar existence; read counter from LLChicletBar.getTotalUnreadIMCount(); update flash state; call setCounter(); updateApplicationWindowTitle()")
    }

    private fun updateApplicationWindowTitle() {
        TODO("APR: prepend unread count to window title when FSShowMessageCountInWindowTitle is true and mCounter>0; truncate to 255 chars; call gViewerWindow.setTitle()")
    }
}

// ---------------------------------------------------------------------------
// LLChicletPanel
// ---------------------------------------------------------------------------

class LLChicletPanel(
    val mChicletPadding: Int = 0,
    var mScrollingOffset: Int = 20,
    val mScrollButtonHPad: Int = 0,
    val mScrollRatio: Int = 4,
    val mMinWidth: Int = 0
) {
    private val mChicletList: MutableList<LLChiclet> = mutableListOf()
    private var mLeftScrollButton: Any? = null
    private var mRightScrollButton: Any? = null
    private var mScrollArea: Any? = null
    private var mShowControls: Boolean = true
    private var mVoiceChannelChangedConnection: (() -> Unit)? = null

    fun postBuild(): Boolean {
        TODO("GPU: bind scroll buttons; wire IM model new-message callbacks; wire script floater callbacks; connect sFindChicletsListeners; connect voice channel changed callback")
    }

    inline fun <reified T : LLChiclet> createChiclet(sessionId: UUID, index: Int): T? {
        TODO("GPU: instantiate T, call addChiclet(); scroll to it if no docked floater; set session id")
    }

    inline fun <reified T : LLChiclet> createChiclet(sessionId: UUID): T? =
        createChiclet(sessionId, mChicletList.size)

    inline fun <reified T : LLChiclet> findChiclet(imSessionId: UUID): T? {
        if (imSessionId == UUID(0, 0)) return null
        return mChicletList.firstOrNull { it.sessionId == imSessionId && it is T } as? T
    }

    inline fun <reified T : LLChiclet> getChiclet(index: Int): T? {
        if (index < 0 || index >= mChicletList.size) return null
        return mChicletList[index] as? T
    }

    fun getChiclet(index: Int): LLChiclet? = getChiclet<LLChiclet>(index)

    fun getChicletCount(): Int = mChicletList.size

    fun getChicletIndex(chiclet: LLChiclet): Int = mChicletList.indexOf(chiclet)

    fun setChicletIndex(chiclet: LLChiclet, index: Int) {
        if (index < 0 || index >= mChicletList.size) return
        val cur = getChicletIndex(chiclet)
        if (cur == -1 || cur == index) return
        mChicletList.removeAt(cur)
        mChicletList.add(index, chiclet)
        arrange()
    }

    fun removeChiclet(index: Int) {
        if (index >= 0 && index < getChicletCount()) removeChicletAt(index)
    }

    fun removeChiclet(chiclet: LLChiclet) {
        val idx = mChicletList.indexOf(chiclet)
        if (idx >= 0) removeChicletAt(idx)
    }

    fun removeChiclet(imSessionId: UUID) {
        val idx = mChicletList.indexOfFirst { it.sessionId == imSessionId }
        if (idx >= 0) removeChicletAt(idx)
    }

    private fun removeChicletAt(index: Int) {
        val chiclet = mChicletList.removeAt(index)
        TODO("GPU: remove chiclet child from mScrollArea; arrange(); LLTransientFloaterMgr.removeControlView(chiclet); chiclet.die()")
    }

    fun removeAll() {
        mChicletList.clear()
        TODO("GPU: remove all child views from mScrollArea; showScrollButtonsIfNeeded()")
    }

    fun scrollToChiclet(chiclet: LLChiclet) {
        TODO("GPU: if chiclet rect is outside scroll area, shift chiclets to bring it into view")
    }

    fun setChicletClickedCallback(cb: (Any?, Any?) -> Unit) {
        TODO("GPU: wire cb to panel commit signal")
    }

    fun onCurrentVoiceChannelChanged(sessionId: UUID) {
        TODO("GPU: show speaker on chiclets for new session; hide speaker on chiclets for previous session; open IM floater if OpenIMOnVoice setting is true")
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        TODO("GPU: reposition scroll buttons; resize mScrollArea accounting for scroll button width; update mShowControls; trimChiclets(); showScrollButtonsIfNeeded()")
    }

    fun draw() {
        TODO("GPU: iterate child list; clip-draw mScrollArea; draw other children normally")
    }

    fun notifyParent(info: Map<String, Any>): Int {
        if (info["notification"] == "size_changes") {
            arrange()
            return 1
        }
        TODO("GPU: delegate to LLPanel.notifyParent(info)")
    }

    fun setChicletToggleState(sessionId: UUID, toggle: Boolean) {
        mChicletList.filterIsInstance<LLIMChiclet>()
            .filter { it.sessionId != sessionId }
            .forEach { it.setToggleState(false) }
        (findChiclet<LLIMChiclet>(sessionId))?.setToggleState(toggle)
    }

    fun getTotalUnreadIMCount(): Int =
        mChicletList.filterIsInstance<LLIMChiclet>().sumOf { it.getCounter() }

    private fun addChiclet(chiclet: LLChiclet, index: Int): Boolean {
        TODO("GPU: add chiclet to mScrollArea; compute left_shift for right-alignment; insert into mChicletList at index; wire click/size-changed callbacks; arrange(); register with LLTransientFloaterMgr")
    }

    private fun arrange() {
        if (mChicletList.isEmpty()) return
        TODO("GPU: lay out chiclets left-to-right from first chiclet position; update mScrollArea rect; trimChiclets(); showScrollButtonsIfNeeded()")
    }

    private fun canScrollRight(): Boolean {
        if (mChicletList.isEmpty()) return false
        TODO("GPU: compare last chiclet right edge to scroll area width")
    }

    private fun needShowScroll(): Boolean {
        if (mChicletList.isEmpty()) return false
        TODO("GPU: compare total chiclet span to panel width")
    }

    private fun canScrollLeft(): Boolean {
        if (mChicletList.isEmpty()) return false
        TODO("GPU: check if first chiclet left < 0")
    }

    private fun showScrollButtonsIfNeeded() {
        TODO("GPU: enable/show left/right scroll buttons based on canScrollLeft()/canScrollRight() and mShowControls")
    }

    private fun shiftChiclets(offset: Int, startIndex: Int = 0) {
        if (startIndex < 0 || startIndex >= getChicletCount()) return
        mChicletList.drop(startIndex).forEach { chiclet ->
            TODO("GPU: translate chiclet by offset on x-axis")
        }
    }

    private fun trimChiclets() {
        TODO("GPU: if last chiclet right < scroll width or first chiclet left > 0, shift to close gap")
    }

    private fun scroll(offset: Int) { shiftChiclets(offset) }

    private fun scrollLeft() {
        if (canScrollLeft()) {
            TODO("GPU: compute offset (handle partial first chiclet); call scroll(); showScrollButtonsIfNeeded()")
        }
    }

    private fun scrollRight() {
        if (canScrollRight()) {
            TODO("GPU: compute offset clamped to align last chiclet; call scroll(); showScrollButtonsIfNeeded()")
        }
    }

    fun onLeftScrollClick()     { scrollLeft() }
    fun onRightScrollClick()    { scrollRight() }

    fun onLeftScrollHeldDown() {
        val saved = mScrollingOffset
        mScrollingOffset = mScrollingOffset / mScrollRatio
        scrollLeft()
        mScrollingOffset = saved
    }

    fun onRightScrollHeldDown() {
        val saved = mScrollingOffset
        mScrollingOffset = mScrollingOffset / mScrollRatio
        scrollRight()
        mScrollingOffset = saved
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (clicks > 0) scrollRight() else scrollLeft()
        return true
    }

    private fun onChicletClick(ctrl: Any?, param: Any?) {
        TODO("GPU: fire panel commit signal with ctrl and param")
    }

    private fun onChicletSizeChanged(ctrl: LLChiclet, param: Any?) { arrange() }

    private fun onMessageCountChanged(data: Map<String, Any>) {
        val sessionId = data["session_id"] as? UUID ?: return
        var unread = (data["participant_unread"] as? Int) ?: 0
        TODO("GPU: if FSFloaterIM for sessionId is visible and focused, set unread=0; then update all chiclets for session via sFindChicletsSignal")
    }

    private fun objectChicletCallback(data: Map<String, Any>) {
        val notificationId = data["notification_id"] as? UUID ?: return
        val newMessage = data["new_message"] as? Boolean ?: false
        TODO("GPU: find chiclets for notificationId; set counter from data[unread] if present; setShowNewMessagesIcon(newMessage)")
    }

    private fun isAnyIMFloaterDoked(): Boolean {
        TODO("GPU: iterate chiclet list; find FSFloaterIM for each; return true if any is visible, not minimised, and docked")
    }
}
