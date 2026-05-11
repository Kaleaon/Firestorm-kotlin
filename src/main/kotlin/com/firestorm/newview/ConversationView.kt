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
            val scrolledOut = TODO("APR: use JVM equivalent - check if this session widget is scrolled out of sight in container") as Boolean
            if (scrolledOut) {
                TODO("APR: use JVM equivalent - flash 'chat' toolbar command button")
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
        val isVisible = TODO("APR: use JVM equivalent - check isInVisibleChain()") as Boolean
        val containerNotMinimized = TODO("APR: use JVM equivalent - check IM container not minimized") as Boolean
        if (isVisible && flashStateOn && !flashStarted && containerNotMinimized) {
            flashStarted = true
            TODO("APR: use JVM equivalent - start flash timer animation")
        }
    }

    fun isHighlightAllowed(): Boolean = flashStateOn || isSelected

    fun isHighlightActive(): Boolean {
        return if (flashStateOn) {
            val flashing = TODO("APR: use JVM equivalent - check flash timer is in progress") as Boolean
            val currentlyHighlighted = TODO("APR: use JVM equivalent - check flash timer currently highlighted") as Boolean
            if (flashing) currentlyHighlighted else true
        } else {
            isCurSelection
        }
    }

    fun isFlashing(): Boolean = flashStateOn

    fun isCollapsed(): Boolean = collapsedMode

    fun postBuild(): Boolean {
        itemPanel = TODO("APR: use JVM equivalent - inflate panel_conversation_list_item XML layout")
        callIconLayoutPanel = TODO("APR: use JVM equivalent - find 'call_icon_panel' child")
        sessionTitle = TODO("APR: use JVM equivalent - find 'conversation_title' text view")
        speakingIndicator = TODO("APR: use JVM equivalent - find 'speaking_indicator' output monitor") as? OutputMonitorCtrlStub

        val vmi = viewModelItem ?: return true
        when (vmi.getType()) {
            ConversationItem.ConversationType.CONV_PARTICIPANT,
            ConversationItem.ConversationType.CONV_SESSION_1_ON_1 -> {
                val session = TODO("APR: use JVM equivalent - find IM session by uuid in IMModel") as? IMSessionStub
                if (session != null) {
                    TODO("APR: use JVM equivalent - show avatar_icon and set value to session.otherParticipantID")
                    TODO("APR: use JVM equivalent - set speaking indicator speaker to other participant")
                    hasArrow = false
                }
            }
            ConversationItem.ConversationType.CONV_SESSION_AD_HOC -> {
                TODO("APR: use JVM equivalent - show group_icon")
                TODO("APR: use JVM equivalent - set speaking indicator speaker to agentID / session uuid")
            }
            ConversationItem.ConversationType.CONV_SESSION_GROUP -> {
                TODO("APR: use JVM equivalent - show group_icon and set value to session uuid")
                TODO("APR: use JVM equivalent - set speaking indicator speaker to agentID / session uuid")
            }
            ConversationItem.ConversationType.CONV_SESSION_NEARBY -> {
                TODO("APR: use JVM equivalent - show nearby_chat_icon")
                TODO("APR: use JVM equivalent - set speaking indicator to agentID / null session")
                isInActiveVoiceChannel = true
                TODO("APR: use JVM equivalent - register NearbyVoiceClientStatusObserver to manage call icon visibility")
            }
            else -> {}
        }
        refresh()
        return true
    }

    open fun draw() {
        startFlashing()
        TODO("GPU: draw highlight background (flash/selection color), open-folder arrow if !collapsedMode, recurse into children if open")
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val result = TODO("APR: use JVM equivalent - delegate to folder view mouse-down, returns selected item") as Boolean
        if (result) {
            val selectedSelf = TODO("APR: use JVM equivalent - root.curSelectedItem == this") as Boolean
            if (selectedSelf) {
                val sessionId = viewModelItem?.uuid ?: UUID(0, 0)
                TODO("APR: use JVM equivalent - collapse/expand messages pane in IM container for session $sessionId")
            }
            selectConversationItem()
        }
        return result
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        val result = TODO("APR: use JVM equivalent - delegate to folder view mouse-up") as Boolean
        if (result) {
            val selectedSelf = TODO("APR: use JVM equivalent - root.curSelectedItem == this") as Boolean
            val volumeFloaterFocused = TODO("APR: use JVM equivalent - check floater_voice_volume or chat_voice has focus") as Boolean
            if (selectedSelf && !volumeFloaterFocused) {
                val sessionId = viewModelItem?.uuid ?: UUID(0, 0)
                TODO("APR: use JVM equivalent - find session floater for $sessionId and set focus")
            }
        }
        return result
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val result = TODO("APR: use JVM equivalent - show context menu via folder view right-mouse") as Boolean
        if (result) selectConversationItem()
        return result
    }

    private fun selectConversationItem() {
        val selectedSelf = TODO("APR: use JVM equivalent - root.curSelectedItem == this") as Boolean
        if (selectedSelf) {
            val sessionId = viewModelItem?.uuid ?: UUID(0, 0)
            TODO("APR: use JVM equivalent - flash($sessionId, false) and selectConversationPair($sessionId, false) in IM container")
        }
    }

    open fun arrange(width: IntArray, height: IntArray): Int {
        val arranged = TODO("APR: use JVM equivalent - run base folder arrange to compute indentation") as Int
        val hPad = if (hasArrow) getIndentation() + arrowSize() else getIndentation()
        val rect = if (collapsedMode) localRect() else rectWithLeft(hPad)
        TODO("APR: use JVM equivalent - set itemPanel shape to rect")
        return arranged
    }

    open fun toggleOpen() {
        if (!collapsedMode) {
            TODO("APR: use JVM equivalent - toggle folder open state; if now open, select self in parent folder")
            TODO("APR: use JVM equivalent - call container.reSelectConversation()")
        }
    }

    fun toggleCollapsedMode(isCollapsed: Boolean) {
        collapsedMode = isCollapsed
        TODO("APR: use JVM equivalent - show/hide 'conversation_item_stack' child view based on !collapsedMode")
        val hPad = if (hasArrow) getIndentation() + arrowSize() else getIndentation()
        TODO("APR: use JVM equivalent - translate itemPanel by ${if (isCollapsed) -hPad else hPad} in x")
    }

    fun setVisibleIfDetached(visible: Boolean) {
        val sessionFloater = getSessionFloater()
        val detachedNotMinimized = TODO("APR: use JVM equivalent - sessionFloater.isDetachedAndNotMinimized()") as Boolean
        if (sessionFloater != null && detachedNotMinimized) {
            TODO("APR: use JVM equivalent - sessionFloater.setVisible($visible)")
        }
    }

    fun findParticipant(participantId: UUID): ConversationViewParticipant? =
        items.firstOrNull { it.hasSameValue(participantId) }

    fun showVoiceIndicator(visible: Boolean) {
        val currentChannelIsNull = TODO("APR: use JVM equivalent - check LLVoiceChannel.getCurrentVoiceChannel().sessionID.isNull") as Boolean
        TODO("APR: use JVM equivalent - setVisible(callIconLayoutPanel, ${visible} && currentChannelIsNull); requestArrange()")
    }

    open fun refresh() {
        val vmi = viewModelItem ?: return
        vmi.resetRefresh()

        val isFriend = highlightFriendTitle(vmi)
        if (!isFriend) {
            val displayName = vmi.displayName
            TODO("APR: use JVM equivalent - set sessionTitle text to '$displayName' with LabelTextColor style")
        }

        TODO("APR: use JVM equivalent - LLSpeakingIndicatorManager.updateSpeakingIndicators()")
        speakingIndicator?.setIsActiveChannel(isInActiveVoiceChannel)
        speakingIndicator?.setShowParticipantsSpeaking(isInActiveVoiceChannel)

        items.forEach { it.allowSpeakingIndicator(isInActiveVoiceChannel) }
        TODO("APR: use JVM equivalent - requestArrange()")
    }

    private fun onCurrentVoiceSessionChanged(sessionId: UUID) {
        val vmi = viewModelItem ?: return
        val old = isInActiveVoiceChannel
        isInActiveVoiceChannel = vmi.uuid == sessionId
        val voiceSuspended = TODO("APR: use JVM equivalent - LLVoiceChannel.isSuspended()") as Boolean
        TODO("APR: use JVM equivalent - setVisible(callIconLayoutPanel, $isInActiveVoiceChannel && !voiceSuspended)")
        if (old != isInActiveVoiceChannel) refresh()
    }

    fun getSessionFloater(): Any? {
        val sessionUuid = viewModelItem?.uuid ?: return null
        return TODO("APR: use JVM equivalent - FloaterIMSessionTab.getConversation($sessionUuid)")
    }

    fun highlightFriendTitle(vmi: ConversationItem): Boolean {
        val type = vmi.getType()
        if (type == ConversationItem.ConversationType.CONV_PARTICIPANT ||
            type == ConversationItem.ConversationType.CONV_SESSION_1_ON_1
        ) {
            val otherParticipantId = TODO("APR: use JVM equivalent - look up IM session for ${vmi.uuid} and get otherParticipantID") as UUID?
            val isFriend = if (otherParticipantId != null) {
                TODO("APR: use JVM equivalent - AvatarActions.isFriend($otherParticipantId)") as Boolean
            } else false
            if (isFriend) {
                TODO("APR: use JVM equivalent - set sessionTitle text to '${vmi.displayName}' with ConversationFriendColor style")
                return true
            }
        }
        return false
    }

    private fun getIndentation(): Int = TODO("APR: use JVM equivalent - return folder view indentation pixels")
    private fun arrowSize(): Int = TODO("APR: use JVM equivalent - return arrow widget size in pixels")
    private fun localRect(): Any = TODO("APR: use JVM equivalent - return local bounding rect of this view")
    private fun rectWithLeft(left: Int): Any = TODO("APR: use JVM equivalent - build rect with left=$left and this item's full height")

    private fun stopFlashing() {
        TODO("APR: use JVM equivalent - mFlashTimer.stopFlashing()")
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
            TODO("APR: use JVM equivalent - measure speakingIndicator and infoBtn widths from their rects")
        }

        const val ALIC_SPEAKER_INDICATOR = 0
        const val ALIC_INFO_BUTTON = 1
    }

    enum class AvatarListItemChildIndex { SPEAKER_INDICATOR, INFO_BUTTON, COUNT }

    var viewModelItem: ConversationItemParticipant? = null

    fun hasSameValue(id: UUID): Boolean = id == participantId

    fun destroyView() {
        TODO("APR: use JVM equivalent - remove this view from parent hierarchy")
    }

    fun postBuild(): Boolean {
        avatarIcon = TODO("APR: use JVM equivalent - find 'avatar_icon' child view")
        infoBtn = TODO("APR: use JVM equivalent - find 'info_btn' child view; set clicked callback; hide it")
        speakingIndicator = TODO("APR: use JVM equivalent - find 'speaking_indicator' OutputMonitorCtrl") as? OutputMonitorCtrlStub

        if (!staticInitialized) {
            initChildrenWidths(this)
            staticInitialized = true
        }
        updateChildren()
        viewModelItem?.let { refresh() }
        return true
    }

    fun draw() {
        TODO("GPU: draw label with appropriate color (friend/muted/selected), highlight bg, speaking indicator mute state")
    }

    fun arrange(width: IntArray, height: IntArray): Int {
        val arranged = TODO("APR: use JVM equivalent - base folder item arrange to compute indentation") as Int
        TODO("APR: use JVM equivalent - reposition avatarIcon rect to indentation x offset")
        updateChildren()
        return arranged
    }

    fun refresh() {
        val model = viewModelItem ?: return
        model.resetRefresh()
        speakingIndicator?.setIsModeratorMuted(model.isModeratorMuted())
        TODO("APR: use JVM equivalent - call base FolderViewItem.refresh()")
    }

    fun addToFolder(folder: ConversationViewSession) {
        folder.items += this
        val vmi = folder.viewModelItem as? ConversationItem
        if (vmi != null) addToSession(vmi.uuid)
        allowSpeakingIndicator(folder.isInActiveVoiceChannel)
    }

    fun addToSession(sessionId: UUID) {
        TODO("APR: use JVM equivalent - set avatarIcon value to participantId=$participantId")
        TODO("APR: use JVM equivalent - set speakingIndicator speaker to participantId=$participantId / session=$sessionId")
    }

    private fun onInfoBtnClick() {
        TODO("APR: use JVM equivalent - show inspect_avatar floater for avatar_id=$participantId")
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val result = TODO("APR: use JVM equivalent - base FolderViewItem mouse-down") as Boolean
        if (result) {
            val selectedSelf = TODO("APR: use JVM equivalent - root.curSelectedItem == this") as Boolean
            if (selectedSelf) {
                TODO("APR: use JVM equivalent - select session in IM container and expand messages pane")
            }
        }
        return result
    }

    fun onMouseEnter(x: Int, y: Int, mask: Int) {
        TODO("APR: use JVM equivalent - show infoBtn; updateChildren()")
    }

    fun onMouseLeave(x: Int, y: Int, mask: Int) {
        TODO("APR: use JVM equivalent - hide infoBtn; updateChildren()")
    }

    fun getLabelXPos(): Int {
        val avatarWidth = TODO("APR: use JVM equivalent - avatarIcon.rect.width") as Int
        val iconPad = TODO("APR: use JVM equivalent - mIconPad constant") as Int
        return getIndentation() + avatarWidth + iconPad
    }

    fun allowSpeakingIndicator(value: Boolean) {
        speakingIndicator?.setIsActiveChannel(value)
    }

    private fun updateChildren() {
        labelPaddingRight = DEFAULT_LABEL_PADDING_RIGHT
        for (i in 0 until AvatarListItemChildIndex.COUNT.ordinal) {
            val control = getItemChildView(i) ?: continue
            val visible = TODO("APR: use JVM equivalent - check control.getVisible()") as Boolean
            if (!visible) continue
            val ctrlWidth = childrenWidths[i]
            labelPaddingRight += ctrlWidth
            TODO("APR: use JVM equivalent - reposition control to align from right edge of item, offset=$labelPaddingRight")
        }
    }

    private fun getItemChildView(index: Int): Any? = when (index) {
        ALIC_SPEAKER_INDICATOR -> speakingIndicator
        ALIC_INFO_BUTTON -> infoBtn
        else -> null
    }

    private fun getIndentation(): Int = TODO("APR: use JVM equivalent - return folder view indentation pixels")
}

// ─── Stubs for unresolved UI types ──────────────────────────────────────────

class OutputMonitorCtrlStub {
    fun setIsActiveChannel(active: Boolean) { TODO("GPU: update speaking-indicator active channel visual state") }
    fun setShowParticipantsSpeaking(show: Boolean) { TODO("GPU: update speaking-indicator participants speaking visual") }
    fun setIsModeratorMuted(muted: Boolean) { TODO("GPU: update speaking-indicator moderator-muted visual state") }
    fun setSpeakerId(speakerId: UUID, sessionId: UUID, showSpeaking: Boolean = false) {
        TODO("APR: use JVM equivalent - bind indicator to speaker speakerId in session $sessionId")
    }
}

class IMSessionStub(val otherParticipantId: UUID, val sessionId: UUID)
