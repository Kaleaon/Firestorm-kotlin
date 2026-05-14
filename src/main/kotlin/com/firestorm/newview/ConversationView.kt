package com.firestorm.newview

import java.util.UUID

// ─── ConversationViewSession ─────────────────────────────────────────────────

open class ConversationViewSession(val container: Any?) {

    private var itemPanel: Any? = null
    private var callIconLayoutPanel: Any? = null
    private var sessionTitle: Any? = null
    private var speakingIndicator: OutputMonitorCtrlStub? = null
    private var flashStateOn: Boolean = false
    private var flashStarted: Boolean = false
    private var isAltFlashColor: Boolean = false
    private var collapsedMode: Boolean = false
    private var hasArrow: Boolean = true
    var isInActiveVoiceChannel: Boolean = false
        private set
    private var isSelected: Boolean = false
    private var isCurSelection: Boolean = false

    protected var viewModelItem: ConversationItem? = null

    val items: MutableList<ConversationViewParticipant> = mutableListOf()

    fun destroyView() {
        val vmi = viewModelItem as? ConversationItemSession
        if (vmi != null && vmi.getType() != ConversationItem.ConversationType.CONV_SESSION_1_ON_1) {
            val toDestroy = items.toList()
            items.clear()
            toDestroy.forEach { it.destroyView() }
            vmi.clearAndDeparentModels()
        }
    }

    fun setFlashState(flashState: Boolean, alternateColor: Boolean = false) {
        if (flashState && !flashStateOn) {
            val scrolledOut = false
            if (scrolledOut) {
                System.err.println("ConversationViewSession: flash 'chat' toolbar command button not yet implemented")
            }
        }
        flashStateOn = flashState
        flashStarted = false
        isAltFlashColor = flashStateOn && (alternateColor || isAltFlashColor)
        stopFlashing()
    }

    fun setHighlightState(highlightState: Boolean) {
        flashStateOn = highlightState
        flashStarted = true
        stopFlashing()
    }

    private fun startFlashing() {
        val isVisible = false
        val containerNotMinimized = false
        if (isVisible && flashStateOn && !flashStarted && containerNotMinimized) {
            flashStarted = true
            System.err.println("ConversationViewSession: start flash timer animation not yet implemented")
        }
    }

    fun isHighlightAllowed(): Boolean = flashStateOn || isSelected

    fun isHighlightActive(): Boolean {
        return if (flashStateOn) {
            val flashing = false
            val currentlyHighlighted = false
            if (flashing) currentlyHighlighted else true
        } else {
            isCurSelection
        }
    }

    fun isFlashing(): Boolean = flashStateOn

    fun isCollapsed(): Boolean = collapsedMode

    fun postBuild(): Boolean {
        itemPanel = null
        callIconLayoutPanel = null
        sessionTitle = null
        speakingIndicator = null

        val vmi = viewModelItem ?: return true
        when (vmi.getType()) {
            ConversationItem.ConversationType.CONV_PARTICIPANT,
            ConversationItem.ConversationType.CONV_SESSION_1_ON_1 -> {
                val session = null as? IMSessionStub
                if (session != null) {
                    System.err.println("ConversationViewSession: show avatar_icon and set value to session.otherParticipantID not yet implemented")
                    System.err.println("ConversationViewSession: set speaking indicator speaker to other participant not yet implemented")
                    hasArrow = false
                }
            }
            ConversationItem.ConversationType.CONV_SESSION_AD_HOC -> {
                System.err.println("ConversationViewSession: show group_icon not yet implemented")
                System.err.println("ConversationViewSession: set speaking indicator speaker to agentID / session uuid not yet implemented")
            }
            ConversationItem.ConversationType.CONV_SESSION_GROUP -> {
                System.err.println("ConversationViewSession: show group_icon and set value to session uuid not yet implemented")
                System.err.println("ConversationViewSession: set speaking indicator speaker to agentID / session uuid not yet implemented")
            }
            ConversationItem.ConversationType.CONV_SESSION_NEARBY -> {
                System.err.println("ConversationViewSession: show nearby_chat_icon not yet implemented")
                System.err.println("ConversationViewSession: set speaking indicator to agentID / null session not yet implemented")
                isInActiveVoiceChannel = true
                System.err.println("ConversationViewSession: register NearbyVoiceClientStatusObserver to manage call icon visibility not yet implemented")
            }
            else -> {}
        }
        refresh()
        return true
    }

    open fun draw() {
        startFlashing()
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val result = false
        if (result) {
            val selectedSelf = false
            if (selectedSelf) {
                val sessionId = viewModelItem?.uuid ?: UUID(0, 0)
                System.err.println("ConversationViewSession: collapse/expand messages pane in IM container for session $sessionId not yet implemented")
            }
            selectConversationItem()
        }
        return result
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        val result = false
        if (result) {
            val selectedSelf = false
            val volumeFloaterFocused = false
            if (selectedSelf && !volumeFloaterFocused) {
                val sessionId = viewModelItem?.uuid ?: UUID(0, 0)
                System.err.println("ConversationViewSession: find session floater for $sessionId and set focus not yet implemented")
            }
        }
        return result
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val result = false
        if (result) selectConversationItem()
        return result
    }

    private fun selectConversationItem() {
        val selectedSelf = false
        if (selectedSelf) {
            val sessionId = viewModelItem?.uuid ?: UUID(0, 0)
            System.err.println("ConversationViewSession: flash($sessionId, false) and selectConversationPair($sessionId, false) in IM container not yet implemented")
        }
    }

    open fun arrange(width: IntArray, height: IntArray): Int {
        val arranged = 0
        val hPad = if (hasArrow) getIndentation() + arrowSize() else getIndentation()
        val rect = if (collapsedMode) localRect() else rectWithLeft(hPad)
        System.err.println("ConversationViewSession: set itemPanel shape to rect not yet implemented")
        return arranged
    }

    open fun toggleOpen() {
        if (!collapsedMode) {
            System.err.println("ConversationViewSession: toggle folder open state; if now open, select self in parent folder not yet implemented")
            System.err.println("ConversationViewSession: call container.reSelectConversation() not yet implemented")
        }
    }

    fun toggleCollapsedMode(isCollapsed: Boolean) {
        collapsedMode = isCollapsed
        System.err.println("ConversationViewSession: show/hide 'conversation_item_stack' child view based on !collapsedMode not yet implemented")
        val hPad = if (hasArrow) getIndentation() + arrowSize() else getIndentation()
        System.err.println("ConversationViewSession: translate itemPanel by ${if (isCollapsed) -hPad else hPad} in x not yet implemented")
    }

    fun setVisibleIfDetached(visible: Boolean) {
        val sessionFloater = getSessionFloater()
        val detachedNotMinimized = false
        if (sessionFloater != null && detachedNotMinimized) {
            System.err.println("ConversationViewSession: sessionFloater.setVisible($visible) not yet implemented")
        }
    }

    fun findParticipant(participantId: UUID): ConversationViewParticipant? =
        items.firstOrNull { it.hasSameValue(participantId) }

    fun showVoiceIndicator(visible: Boolean) {
        val currentChannelIsNull = false
        System.err.println("ConversationViewSession: setVisible(callIconLayoutPanel, ${visible} && currentChannelIsNull); requestArrange() not yet implemented")
    }

    open fun refresh() {
        val vmi = viewModelItem ?: return
        vmi.resetRefresh()

        val isFriend = highlightFriendTitle(vmi)
        if (!isFriend) {
            val displayName = vmi.displayName
            System.err.println("ConversationViewSession: set sessionTitle text to '$displayName' with LabelTextColor style not yet implemented")
        }

        System.err.println("ConversationViewSession: LLSpeakingIndicatorManager.updateSpeakingIndicators() not yet implemented")
        speakingIndicator?.setIsActiveChannel(isInActiveVoiceChannel)
        speakingIndicator?.setShowParticipantsSpeaking(isInActiveVoiceChannel)

        items.forEach { it.allowSpeakingIndicator(isInActiveVoiceChannel) }
        System.err.println("ConversationViewSession: requestArrange() not yet implemented")
    }

    private fun onCurrentVoiceSessionChanged(sessionId: UUID) {
        val vmi = viewModelItem ?: return
        val old = isInActiveVoiceChannel
        isInActiveVoiceChannel = vmi.uuid == sessionId
        val voiceSuspended = false
        System.err.println("ConversationViewSession: setVisible(callIconLayoutPanel, $isInActiveVoiceChannel && !voiceSuspended) not yet implemented")
        if (old != isInActiveVoiceChannel) refresh()
    }

    fun getSessionFloater(): Any? {
        val sessionUuid = viewModelItem?.uuid ?: return null
        return null
    }

    fun highlightFriendTitle(vmi: ConversationItem): Boolean {
        val type = vmi.getType()
        if (type == ConversationItem.ConversationType.CONV_PARTICIPANT ||
            type == ConversationItem.ConversationType.CONV_SESSION_1_ON_1
        ) {
            val otherParticipantId = null as UUID?
            val isFriend = if (otherParticipantId != null) {
                false
            } else false
            if (isFriend) {
                System.err.println("ConversationViewSession: set sessionTitle text to '${vmi.displayName}' with ConversationFriendColor style not yet implemented")
                return true
            }
        }
        return false
    }

    private fun getIndentation(): Int = 0
    private fun arrowSize(): Int = 0
    private fun localRect(): Any = Any()
    private fun rectWithLeft(left: Int): Any = Any()

    private fun stopFlashing() {
        System.err.println("ConversationViewSession: mFlashTimer.stopFlashing() not yet implemented")
    }
}

// ─── ConversationViewParticipant ──────────────────────────────────────────────

class ConversationViewParticipant(
    private val participantId: UUID,
    private val container: Any?,
) {
    private var avatarIcon: Any? = null
    private var infoBtn: Any? = null
    private var speakingIndicator: OutputMonitorCtrlStub? = null
    private var isSelected: Boolean = false
    private var labelPaddingRight: Int = DEFAULT_LABEL_PADDING_RIGHT

    companion object {
        private const val DEFAULT_LABEL_PADDING_RIGHT = 8
        private var staticInitialized = false
        private val childrenWidths = IntArray(AvatarListItemChildIndex.COUNT.ordinal)

        private fun initChildrenWidths(self: ConversationViewParticipant) {
            System.err.println("ConversationViewParticipant: measure speakingIndicator and infoBtn widths from their rects not yet implemented")
        }

        const val ALIC_SPEAKER_INDICATOR = 0
        const val ALIC_INFO_BUTTON = 1
    }

    enum class AvatarListItemChildIndex { SPEAKER_INDICATOR, INFO_BUTTON, COUNT }

    var viewModelItem: ConversationItemParticipant? = null

    fun hasSameValue(id: UUID): Boolean = id == participantId

    fun destroyView() {
        System.err.println("ConversationViewParticipant: remove this view from parent hierarchy not yet implemented")
    }

    fun postBuild(): Boolean {
        avatarIcon = null
        infoBtn = null
        speakingIndicator = null

        if (!staticInitialized) {
            initChildrenWidths(this)
            staticInitialized = true
        }
        updateChildren()
        viewModelItem?.let { refresh() }
        return true
    }

    fun draw() {
    }

    fun arrange(width: IntArray, height: IntArray): Int {
        val arranged = 0
        System.err.println("ConversationViewParticipant: reposition avatarIcon rect to indentation x offset not yet implemented")
        updateChildren()
        return arranged
    }

    fun refresh() {
        val model = viewModelItem ?: return
        model.resetRefresh()
        speakingIndicator?.setIsModeratorMuted(model.isModeratorMuted())
        System.err.println("ConversationViewParticipant: call base FolderViewItem.refresh() not yet implemented")
    }

    fun addToFolder(folder: ConversationViewSession) {
        folder.items += this
        val vmi = folder.viewModelItem as? ConversationItem
        if (vmi != null) addToSession(vmi.uuid)
        allowSpeakingIndicator(folder.isInActiveVoiceChannel)
    }

    fun addToSession(sessionId: UUID) {
        System.err.println("ConversationViewParticipant: set avatarIcon value to participantId=$participantId not yet implemented")
        System.err.println("ConversationViewParticipant: set speakingIndicator speaker to participantId=$participantId / session=$sessionId not yet implemented")
    }

    private fun onInfoBtnClick() {
        System.err.println("ConversationViewParticipant: show inspect_avatar floater for avatar_id=$participantId not yet implemented")
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val result = false
        if (result) {
            val selectedSelf = false
            if (selectedSelf) {
                System.err.println("ConversationViewParticipant: select session in IM container and expand messages pane not yet implemented")
            }
        }
        return result
    }

    fun onMouseEnter(x: Int, y: Int, mask: Int) {
        System.err.println("ConversationViewParticipant: show infoBtn; updateChildren() not yet implemented")
    }

    fun onMouseLeave(x: Int, y: Int, mask: Int) {
        System.err.println("ConversationViewParticipant: hide infoBtn; updateChildren() not yet implemented")
    }

    fun getLabelXPos(): Int {
        val avatarWidth = 0
        val iconPad = 0
        return getIndentation() + avatarWidth + iconPad
    }

    fun allowSpeakingIndicator(value: Boolean) {
        speakingIndicator?.setIsActiveChannel(value)
    }

    private fun updateChildren() {
        labelPaddingRight = DEFAULT_LABEL_PADDING_RIGHT
        for (i in 0 until AvatarListItemChildIndex.COUNT.ordinal) {
            val control = getItemChildView(i) ?: continue
            val visible = false
            if (!visible) continue
            val ctrlWidth = childrenWidths[i]
            labelPaddingRight += ctrlWidth
            System.err.println("ConversationViewParticipant: reposition control to align from right edge of item, offset=$labelPaddingRight not yet implemented")
        }
    }

    private fun getItemChildView(index: Int): Any? = when (index) {
        ALIC_SPEAKER_INDICATOR -> speakingIndicator
        ALIC_INFO_BUTTON -> infoBtn
        else -> null
    }

    private fun getIndentation(): Int = 0
}

// ─── Stubs for unresolved UI types ──────────────────────────────────────────

class OutputMonitorCtrlStub {
    fun setIsActiveChannel(active: Boolean) { }
    fun setShowParticipantsSpeaking(show: Boolean) { }
    fun setIsModeratorMuted(muted: Boolean) { }
    fun setSpeakerId(speakerId: UUID, sessionId: UUID, showSpeaking: Boolean = false) {
        System.err.println("OutputMonitorCtrlStub: bind indicator to speaker speakerId in session $sessionId not yet implemented")
    }
}

class IMSessionStub(val otherParticipantId: UUID, val sessionId: UUID)
