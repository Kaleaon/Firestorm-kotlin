package com.firestorm.newview

import java.util.UUID

const val REFRESH_INTERVAL: Float = 1.0f
const val ICN_GROUP: String = "group_chat_icon"
const val ICN_NEARBY: String = "nearby_chat_icon"
const val ICN_AVATAR: String = "avatar_icon"

abstract class LLFloaterIMSessionTab(sessionId: Any) : LLTransientDockableFloater(null, false, sessionId), LLIMSessionObserver {

    var mIsNearbyChat: Boolean = false
    var mIsP2PChat: Boolean = false
    var mMessagePaneExpanded: Boolean = true
    var mIsParticipantListExpanded: Boolean = true
    var mMinFloaterHeight: Int = 0
    var mFloaterExtraWidth: Int = 0

    protected var mSession: LLIMModel.LLIMSession? = null
    var mSessionID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    protected var mContentsView: LLView? = null
    protected var mBodyStack: LLLayoutStack? = null
    protected var mParticipantListAndHistoryStack: LLLayoutStack? = null
    protected var mParticipantListPanel: LLLayoutPanel? = null
    protected var mRightPartPanel: LLLayoutPanel? = null
    protected var mContentPanel: LLLayoutPanel? = null
    protected var mToolbarPanel: LLLayoutPanel? = null
    protected var mInputButtonPanel: LLLayoutPanel? = null
    protected var mEmojiRecentPanel: LLLayoutPanel? = null
    protected var mEmojiRecentEmptyText: LLTextBox? = null
    protected var mEmojiRecentContainer: LLPanel? = null
    protected var mEmojiRecentIconsCtrl: LLPanelEmojiComplete? = null

    protected var mChatHistory: LLChatHistory? = null
    protected var mInputEditor: LLChatEntry? = null
    protected var mChatLayoutPanel: LLLayoutPanel? = null
    protected var mInputPanels: LLLayoutStack? = null

    protected var mExpandCollapseLineBtn: LLButton? = null
    protected var mExpandCollapseBtn: LLButton? = null
    protected var mTearOffBtn: LLButton? = null
    protected var mEmojiRecentPanelToggleBtn: LLButton? = null
    protected var mEmojiPickerShowBtn: LLButton? = null
    protected var mCloseBtn: LLButton? = null
    protected var mGearBtn: LLButton? = null
    protected var mAddBtn: LLButton? = null
    protected var mVoiceButton: LLButton? = null

    protected var mVoiceButtonHangUpMode: Boolean = false

    protected val mConversationsWidgets: MutableMap<UUID, LLConversationViewParticipant> = mutableMapOf()
    protected val mConversationViewModel: LLConversationViewModel = LLConversationViewModel()
    protected var mConversationsRoot: LLFolderView? = null
    protected var mScroller: LLScrollContainer? = null

    private var mIsHostAttached: Boolean = false
    private var mHasVisibleBeenInitialized: Boolean = false
    private var mRefreshTimer: LLTimer? = LLTimer()
    private var mInputEditorPad: Int = 0
    private var mChatLayoutPanelHeight: Int = 0
    private var mFloaterHeight: Int = 0

    private var mEmojiCloseConn: (() -> Unit)? = null
    private var mEmojiHelperLastCallbackFrame: UInt = 0u

    init {
        mSessionID = sessionId.toString().let { runCatching { UUID.fromString(it) }.getOrDefault(UUID.fromString("00000000-0000-0000-0000-000000000000")) }
        mSession = LLIMModel.getInstance().findIMSession(mSessionID)
        LLIMMgr.instance().addSessionObserver(this)
        mMinFloaterHeight = getMinHeight()
    }

    fun isHostAttached(): Boolean = mIsHostAttached
    fun setHostAttached(isAttached: Boolean) { mIsHostAttached = isAttached }
    fun isNearbyChat(): Boolean = mIsNearbyChat
    fun isMessagePaneExpanded(): Boolean = mMessagePaneExpanded
    fun setMessagePaneExpanded(expanded: Boolean) { mMessagePaneExpanded = expanded }

    open fun onOpen(key: Any) {
        if (!checkIfTornOff()) {
            val hostFloater = getHost() as? LLFloaterIMContainer
            hostFloater?.collapseMessagesPane(false)
        }
        mInputButtonPanel?.setVisible(isTornOff())
        setFocus(true)
    }

    open fun postBuild(): Boolean {
        mContentsView = getChild<LLView>("contents_view")
        mBodyStack = getChild<LLLayoutStack>("main_stack")
        mParticipantListAndHistoryStack = getChild<LLLayoutStack>("im_panels")

        mCloseBtn = getChild<LLButton>("close_btn")
        mCloseBtn?.setCommitCallback { onClickClose(this) }

        mExpandCollapseBtn = getChild<LLButton>("expand_collapse_btn")
        mExpandCollapseBtn?.setClickedCallback { onSlide(this) }

        mExpandCollapseLineBtn = getChild<LLButton>("minz_btn")
        mExpandCollapseLineBtn?.setClickedCallback { onCollapseToLine(this) }

        mTearOffBtn = getChild<LLButton>("tear_off_btn")
        mTearOffBtn?.setCommitCallback { onTearOffClicked() }

        mEmojiRecentPanelToggleBtn = getChild<LLButton>("emoji_recent_panel_toggle_btn")
        mEmojiRecentPanelToggleBtn?.setClickedCallback { onEmojiRecentPanelToggleBtnClicked() }

        mEmojiRecentPanel = getChild<LLLayoutPanel>("emoji_recent_layout_panel")
        mEmojiRecentPanel?.setVisible(false)

        mEmojiRecentEmptyText = getChild<LLTextBox>("emoji_recent_empty_text")
        mEmojiRecentEmptyText?.setToolTip(mEmojiRecentEmptyText!!.getText())
        mEmojiRecentEmptyText?.setVisible(false)

        mEmojiRecentContainer = getChild<LLPanel>("emoji_recent_container")
        mEmojiRecentContainer?.setVisible(false)

        mEmojiRecentIconsCtrl = getChild<LLPanelEmojiComplete>("emoji_recent_icons_ctrl")
        mEmojiRecentIconsCtrl?.setFocusReceivedCallback { onEmojiRecentPanelFocusReceived() }
        mEmojiRecentIconsCtrl?.setFocusLostCallback { onEmojiRecentPanelFocusLost() }
        mEmojiRecentIconsCtrl?.setCommitCallback { value -> onRecentEmojiPicked(value) }

        mEmojiPickerShowBtn = getChild<LLButton>("emoji_picker_show_btn")
        mEmojiPickerShowBtn?.setClickedCallback { onEmojiPickerShowBtnClicked() }
        mEmojiPickerShowBtn?.setMouseDownCallback { onEmojiPickerShowBtnDown() }
        mEmojiCloseConn = LLEmojiHelper.instance().setCloseCallback { onEmojiPickerClosed() }

        mGearBtn = getChild<LLButton>("gear_btn")
        mAddBtn = getChild<LLButton>("add_btn")
        mVoiceButton = getChild<LLButton>("voice_call_btn")
        mVoiceButton?.setClickedCallback { onCallButtonClicked() }

        mParticipantListPanel = getChild<LLLayoutPanel>("speakers_list_panel")
        mRightPartPanel = getChild<LLLayoutPanel>("right_part_holder")
        mToolbarPanel = getChild<LLLayoutPanel>("toolbar_panel")
        mContentPanel = getChild<LLLayoutPanel>("body_panel")
        mInputButtonPanel = getChild<LLLayoutPanel>("input_button_layout_panel")
        mInputButtonPanel?.setVisible(false)

        val scrollerViewRect = mParticipantListPanel!!.getRect()
        mScroller = LLFolderViewScrollContainer()
        mScroller!!.setFollowsAll()
        mParticipantListPanel!!.addChild(mScroller!!)

        mChatHistory = getChild<LLChatHistory>("chat_history")
        mInputEditor = getChild<LLChatEntry>("chat_editor")
        mChatLayoutPanel = getChild<LLLayoutPanel>("chat_layout_panel")
        mInputPanels = getChild<LLLayoutStack>("input_panels")

        mInputEditor?.setTextExpandedCallback { reshapeChatLayoutPanel() }
        mInputEditor?.setMouseUpCallback { onInputEditorClicked() }
        mInputEditor?.setCommitOnFocusLost(false)
        mInputEditor?.setPassDelete(true)

        mChatLayoutPanelHeight = mChatLayoutPanel!!.getRect().getHeight()
        mInputEditorPad = mChatLayoutPanelHeight - mInputEditor!!.getRect().getHeight()

        val baseItem = LLConversationItem(mSessionID, mConversationViewModel)
        mConversationsRoot = LLFolderView(baseItem, mParticipantListPanel, mConversationViewModel)
        mScroller!!.addChild(mConversationsRoot!!)
        mConversationsRoot!!.setScrollContainer(mScroller)
        mConversationsRoot!!.setFollowsAll()

        setMessagePaneExpanded(true)
        buildConversationViewParticipant()
        refreshConversation()

        mRefreshTimer!!.setTimerExpirySec(0f)
        mRefreshTimer!!.start()
        initBtns()

        mParticipantListPanel!!.getResizeBar().setResizeListener { assignResizeLimits() }
        mFloaterExtraWidth = getRect().getWidth() - mParticipantListAndHistoryStack!!.getRect().getWidth() -
                (if (mParticipantListPanel!!.isCollapsed()) 0 else LLPANEL_BORDER_WIDTH)
        assignResizeLimits()

        return if (isChatMultiTab()) LLFloater.postBuild() else LLDockableFloater.postBuild()
    }

    open fun draw() {
        if (mRefreshTimer!!.hasExpired()) {
            val item = getParticipantList()
            if (item != null) {
                item.update()
                if (item.getChildrenCount() != mConversationsWidgets.size) {
                    buildConversationViewParticipant()
                }
                refreshConversation()
            }
            mRefreshTimer!!.setTimerExpirySec(REFRESH_INTERVAL)
        }
        super.draw()
    }

    open fun setVisible(visible: Boolean) {
        if (visible && !mHasVisibleBeenInitialized) {
            mHasVisibleBeenInitialized = true
            if (!gAgentCamera.cameraMouselook()) {
                LLFloaterReg.getTypedInstance<LLFloaterIMContainer>("im_container").setVisible(true)
            }
            addToHost(mSessionID)
            val conversp = getConversation(mSessionID)
            if (conversp != null && conversp.isNearbyChat() && gSavedPerAccountSettings.getBool("NearbyChatIsNotCollapsed")) {
                onCollapseToLine(this)
            }
            mInputButtonPanel?.setVisible(isTornOff())
        }
        super.setVisible(visible)
    }

    open fun setFocus(focus: Boolean) {
        super.setFocus(focus)
        if (focus) {
            updateMessages()
            mInputEditor?.setFocus(true)
        }
    }

    open fun closeFloater(appQuitting: Boolean = false) {
        LLFloaterEmojiPicker.saveState()
        super.closeFloater(appQuitting)
    }

    open fun deleteAllChildren() {
        super.deleteAllChildren()
        mVoiceButton = null
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        var handled = false
        if (mask == MASK_ALT) {
            val floaterContainer = LLFloaterIMContainer.getInstance()
            if (key == KEY_RETURN && !isTornOff()) {
                floaterContainer.expandConversation()
                handled = true
            }
            if (key == KEY_UP || key == KEY_LEFT) {
                floaterContainer.selectNextorPreviousConversation(false)
                handled = true
            }
            if (key == KEY_DOWN || key == KEY_RIGHT) {
                floaterContainer.selectNextorPreviousConversation(true)
                handled = true
            }
        }
        return handled
    }

    override fun sessionAdded(sessionId: UUID, name: String, otherParticipantId: UUID, hasOfflineMsg: Boolean) {}
    override fun sessionActivated(sessionId: UUID, name: String, otherParticipantId: UUID) {}

    override fun sessionRemoved(sessionId: UUID) {
        if (sessionId == mSessionID) {
            mSession = null
        }
    }

    override fun sessionVoiceOrIMStarted(sessionId: UUID) {}
    override fun sessionIDUpdated(oldSessionId: UUID, newSessionId: UUID) {}

    fun addConversationViewParticipant(item: LLConversationItem, updateView: Boolean = true) {
        if (item == null) return
        val uuid = item.getUUID()
        val widget = mConversationsWidgets[uuid]
        if (widget != null) {
            if (updateView) updateConversationViewParticipant(uuid)
        } else {
            val participantView = createConversationViewParticipant(item)
            mConversationsWidgets[uuid] = participantView
            participantView.addToFolder(mConversationsRoot!!)
            participantView.addToSession(mSessionID)
            participantView.setVisible(true)
        }
    }

    fun removeConversationViewParticipant(participantId: UUID) {
        mConversationsWidgets[participantId]?.destroyView()
        mConversationsWidgets.remove(participantId)
    }

    fun updateConversationViewParticipant(participantId: UUID) {
        val widget = mConversationsWidgets[participantId]
        if (widget != null && widget.getViewModelItem() != null) {
            widget.refresh()
        }
    }

    fun refreshConversation() {
        val isAdHoc = mSession?.isAdHocSessionType() ?: false
        val participantsUuids: MutableList<UUID> = mutableListOf()

        if (mIsP2PChat && mSession != null) {
            participantsUuids.add(mSession!!.mOtherParticipantID)
        }

        for ((uuid, widget) in mConversationsWidgets) {
            if (isAdHoc && uuid != gAgentID) {
                participantsUuids.add(uuid)
            }
            if (widget.getViewModelItem() != null) {
                widget.refresh()
                widget.setVisible(true)
            }
        }

        if (isAdHoc || mIsP2PChat) {
            val sessionName = if (participantsUuids.isNotEmpty()) {
                LLAvatarActions.buildResidentsString(participantsUuids)
            } else {
                LLIMModel.instance().getName(mSessionID)
            }
            updateSessionName(sessionName)
        }

        if (mSessionID != UUID.fromString("00000000-0000-0000-0000-000000000000")) {
            val participantList = getParticipantList()
            if (participantList != null) {
                val speakerMgr = LLIMModel.getInstance().getSpeakerManager(mSessionID)
                for (participantModelItem in participantList.getChildren()) {
                    val participantModel = participantModelItem as? LLConversationItemParticipant
                    if (speakerMgr != null && participantModel != null) {
                        val participantSpeaker = speakerMgr.findSpeaker(participantModel.getUUID())
                        val agentSpeaker = speakerMgr.findSpeaker(gAgentID)
                        if (participantSpeaker != null && agentSpeaker != null) {
                            participantModel.setDisplayModeratorRole(agentSpeaker.mIsModerator && participantSpeaker.mIsModerator)
                        }
                    }
                }
            }
        }

        mConversationViewModel.requestSortAll()
        mConversationsRoot?.arrangeAll()
        mConversationsRoot?.update()
        updateHeaderAndToolbar()
        refresh()
    }

    fun buildConversationViewParticipant() {
        val widgetKeys = mConversationsWidgets.keys.toList()
        for (uuid in widgetKeys) {
            removeConversationViewParticipant(uuid)
        }

        val item = getParticipantList() ?: return
        for (participantModelItem in item.getChildren()) {
            val participantModel = participantModelItem as? LLConversationItem
            if (participantModel != null) {
                addConversationViewParticipant(participantModel)
            }
        }
    }

    fun setSortOrder(order: LLConversationSort) {
        mConversationViewModel.setSorter(order)
        mConversationsRoot?.arrangeAll()
        refreshConversation()
    }

    open fun onTearOffClicked() {
        restoreFloater()
        setFollows(if (isTornOff()) FOLLOWS_ALL else FOLLOWS_NONE)
        mSaveRect = isTornOff()
        initRectControl()
        LLFloater.onClickTearOff(this)
        val container = LLFloaterReg.findTypedInstance<LLFloaterIMContainer>("im_container")

        if (isTornOff()) {
            container?.selectAdjacentConversation(false)
            forceReshape()
        } else {
            container?.selectConversation(mSessionID)
        }

        mInputButtonPanel?.setVisible(isTornOff())
        refreshConversation()
        updateGearBtn()
    }

    fun updateGearBtn() {
        val prevVisibility = mGearBtn?.getVisible() ?: false
        mGearBtn?.setVisible(checkIfTornOff() && mIsP2PChat)

        if (prevVisibility != (mGearBtn?.getVisible() ?: false)) {
            val gearBtnRect = mGearBtn!!.getRect()
            val addBtnRect = mAddBtn!!.getRect()
            val callBtnRect = mVoiceButton!!.getRect()
            val gapWidth = callBtnRect.mLeft - addBtnRect.mRight
            val rightShift = gearBtnRect.getWidth() + gapWidth
            if (mGearBtn!!.getVisible()) {
                addBtnRect.translate(rightShift, 0)
                callBtnRect.translate(rightShift, 0)
            } else {
                addBtnRect.translate(-rightShift, 0)
                callBtnRect.translate(-rightShift, 0)
            }
            mAddBtn!!.setRect(addBtnRect)
            mVoiceButton!!.setRect(callBtnRect)
        }
    }

    fun initBtns() {
        val gearBtnRect = mGearBtn!!.getRect()
        val addBtnRect = mAddBtn!!.getRect()
        val callBtnRect = mVoiceButton!!.getRect()
        val gapWidth = callBtnRect.mLeft - addBtnRect.mRight
        val rightShift = gearBtnRect.getWidth() + gapWidth
        addBtnRect.translate(-rightShift, 0)
        callBtnRect.translate(-rightShift, 0)
        mAddBtn!!.setRect(addBtnRect)
        mVoiceButton!!.setRect(callBtnRect)
    }

    open fun updateMessages() {}

    fun getCurSelectedViewModelItem(): LLConversationItem? {
        val root = mConversationsRoot ?: return null
        val selected = root.getCurSelectedItem() ?: return null
        return selected.getViewModelItem() as? LLConversationItem
    }

    fun forceReshape() {
        val floaterRect = getRect()
        reshape(
            maxOf(floaterRect.getWidth(), getMinWidth()),
            maxOf(floaterRect.getHeight(), getMinHeight()),
            true
        )
    }

    fun restoreFloater() {
        if (!isMessagePaneExpanded()) {
            if (isMinimized()) setMinimized(false)
            mContentPanel?.setVisible(true)
            mToolbarPanel?.setVisible(true)
            val floaterRect = getRect()
            floaterRect.mTop = floaterRect.mBottom + mFloaterHeight
            setShape(floaterRect, true)
            mBodyStack?.updateLayout()
            mExpandCollapseLineBtn?.setImageOverlay(getString("expandline_icon"))
            setResizeLimits(getMinWidth(), mMinFloaterHeight)
            setMessagePaneExpanded(true)
            saveCollapsedState()
            mInputEditor?.enableSingleLineMode(false)
            enableResizeCtrls(true, true, true)
        }
    }

    fun saveCollapsedState() {
        val conversp = getConversation(mSessionID)
        if (conversp?.isNearbyChat() == true) {
            gSavedPerAccountSettings.setBool("NearbyChatIsNotCollapsed", isMessagePaneExpanded())
        }
    }

    fun updateChatIcon(id: UUID) {
        val session = mSession
        if (session != null) {
            if (session.isP2PSessionType()) {
                val icon = getChild<LLAvatarIconCtrl>(ICN_AVATAR)
                icon.setVisible(true)
                icon.setValue(id)
            }
            if (session.isAdHocSessionType()) {
                val icon = getChild<LLGroupIconCtrl>(ICN_GROUP)
                icon.setVisible(true)
            }
            if (session.isGroupSessionType()) {
                val icon = getChild<LLGroupIconCtrl>(ICN_GROUP)
                icon.setVisible(true)
                icon.setValue(id)
            }
        } else {
            if (mIsNearbyChat) {
                val icon = getChild<LLIconCtrl>(ICN_NEARBY)
                icon.setVisible(true)
            }
        }
    }

    fun getChatHistory(): LLView? = mChatHistory

    protected fun getParticipantList(): LLParticipantList? =
        LLFloaterIMContainer.getInstance().getSessionModel(mSessionID) as? LLParticipantList

    protected fun createConversationViewParticipant(item: LLConversationItem): LLConversationViewParticipant {
        val panelRect = mParticipantListPanel!!.getRect()
        return LLConversationViewParticipant(
            name = item.getDisplayName(),
            root = mConversationsRoot!!,
            listener = item,
            rect = LLRect(0, 24, panelRect.getWidth(), 0),
            participantId = item.getUUID()
        )
    }

    protected fun onIMSessionMenuItemClicked(userdata: String) {
        if (userdata == "compact_view" || userdata == "expanded_view") {
            gSavedSettings.setBool("PlainTextChatHistory", userdata == "compact_view")
        } else {
            val prevValue = gSavedSettings.getBool(userdata)
            gSavedSettings.setBool(userdata, !prevValue)
        }
        processChatHistoryStyleUpdate()
    }

    protected fun onIMCompactExpandedMenuItemCheck(userdata: String): Boolean {
        val isPlainTextMode = gSavedSettings.getBool("PlainTextChatHistory")
        return if (isPlainTextMode) userdata == "compact_view" else userdata == "expanded_view"
    }

    protected fun onIMShowModesMenuItemCheck(userdata: String): Boolean =
        gSavedSettings.getBool(userdata)

    protected fun onIMShowModesMenuItemEnable(userdata: String): Boolean {
        val plainText = gSavedSettings.getBool("PlainTextChatHistory")
        val isNotNames = userdata != "IMShowNamesForP2PConv"
        return plainText && (isNotNames || mIsP2PChat)
    }

    protected fun updateCallBtnState(callIsActive: Boolean) {
        mVoiceButton?.setImageOverlay(getString(if (callIsActive) "call_btn_stop" else "call_btn_start"))
        mVoiceButton?.setToolTip(getString(if (callIsActive) "end_call_button_tooltip" else "start_call_button_tooltip"))
        mVoiceButtonHangUpMode = callIsActive
        enableDisableCallBtn()
    }

    protected open fun enableDisableCallBtn() {
        val btn = mVoiceButton ?: return
        var enable = false
        val session = mSession
        if (mSessionID != UUID.fromString("00000000-0000-0000-0000-000000000000") &&
            session != null && session.mSessionInitialized && session.mCallBackEnabled) {
            if (mVoiceButtonHangUpMode) {
                enable = true
            } else {
                if (LLVoiceClient.instanceExists() &&
                    session.mVoiceChannel != null && !session.mVoiceChannel!!.callStarted()) {
                    val client = LLVoiceClient.getInstance()
                    if (client.voiceEnabled() && client.isVoiceWorking()) {
                        enable = true
                    }
                }
            }
        }
        btn.setEnabled(enable)
    }

    protected open fun updateSessionName(name: String) {
        mInputEditor?.setLabel(LLTrans.getString("IM_to_label") + " " + name)
    }

    protected open fun onFocusReceived() {
        setBackgroundOpaque(true)
        if (mSessionID != UUID.fromString("00000000-0000-0000-0000-000000000000") && isInVisibleChain()) {
            LLIMModel.instance().sendNoUnreadMessages(mSessionID)
        }
        LLFloaterChatMentionPicker.updateSessionID(mSessionID)
        super.onFocusReceived()
    }

    protected open fun onFocusLost() {
        setBackgroundOpaque(false)
        super.onFocusLost()
    }

    protected fun appendMessage(chat: LLChat, args: Map<String, Any> = emptyMap()) {
        if (chat.mMuted || mChatHistory == null) return

        val imBox = LLFloaterIMContainer.findInstance()
        imBox?.setTimeNow(mSessionID, chat.mFromID)

        val chatArgs = args.toMutableMap()
        if (chat.mTimeStr.isEmpty()) {
            chat.mTimeStr = appendTime()
        }
        chatArgs["use_plain_text_chat_history"] = gSavedSettings.getBool("PlainTextChatHistory")
        chatArgs["show_time"] = gSavedSettings.getBool("IMShowTime")
        chatArgs["show_names_for_p2p_conv"] = !mIsP2PChat || gSavedSettings.getBool("IMShowNamesForP2PConv")

        mChatHistory!!.appendMessage(chat, chatArgs)
    }

    protected fun appendTime(): String {
        val use24h = gSavedSettings.getBool("Use24HourClock")
        return if (use24h) {
            "[${LLTrans.getString("TimeHour")}]:[${LLTrans.getString("TimeMin")}]"
        } else {
            "[${LLTrans.getString("TimeHour12")}]:[${LLTrans.getString("TimeMin")}] [${LLTrans.getString("TimeAMPM")}]"
        }
    }

    protected fun assignResizeLimits() {
        val isParticipantsPaneCollapsed = mParticipantListPanel!!.isCollapsed()
        mRightPartPanel?.setIgnoreReshape(isParticipantsPaneCollapsed)
        val participantsPaneTargetWidth = if (isParticipantsPaneCollapsed) 0
            else mParticipantListPanel!!.getRect().getWidth() + mParticipantListAndHistoryStack!!.getPanelSpacing()
        val newMinWidth = participantsPaneTargetWidth + mRightPartPanel!!.getExpandedMinDim() + mFloaterExtraWidth
        setResizeLimits(newMinWidth, getMinHeight())
        mParticipantListAndHistoryStack?.updateLayout()
    }

    protected fun updateUsedEmojis(text: String) {
        val dictionary = LLEmojiDictionary.getInstance()
        var emojiSent = false
        for (c in text) {
            if (dictionary.isEmoji(c)) {
                LLFloaterEmojiPicker.onEmojiUsed(c)
                emojiSent = true
            }
        }
        if (!emojiSent) return
        LLFloaterEmojiPicker.saveState()
        if (mEmojiRecentPanel?.getVisible() == true) {
            initEmojiRecentPanel()
        }
    }

    protected fun hideOrShowTitle() {
        val floaterHeaderSize = LLFloater.getDefaultParams().headerHeight
        val floaterRect = getLocalRect()
        val topBorderOfContents = floaterRect.mTop - (if (isTornOff()) floaterHeaderSize else 0)
        mDragHandle?.setShape(LLRect(0, floaterRect.mTop, floaterRect.mRight, topBorderOfContents))
        mDragHandle?.setVisible(isTornOff())
        mContentsView?.setShape(LLRect(0, topBorderOfContents, floaterRect.mRight, floaterRect.mBottom))
    }

    protected fun hideAllStandardButtons() {
        for (i in 0 until BUTTON_COUNT) {
            mButtons[i]?.setVisible(false)
        }
    }

    protected fun updateHeaderAndToolbar() {
        LLFloaterIMContainer.getInstance()
        val isNotTornOff = !checkIfTornOff()
        if (isNotTornOff) hideAllStandardButtons()
        hideOrShowTitle()

        val isParticipantListVisible = !isNotTornOff && mIsParticipantListExpanded && !mIsP2PChat
        mParticipantListAndHistoryStack?.collapsePanel(mParticipantListPanel, !isParticipantListVisible)
        mParticipantListPanel?.setVisible(isParticipantListVisible)

        val isExpanded = isNotTornOff || isParticipantListVisible
        mExpandCollapseBtn?.setImageOverlay(getString(if (isExpanded) "collapse_icon" else "expand_icon"))
        mExpandCollapseBtn?.setToolTip(
            if (isNotTornOff) getString("expcol_button_not_tearoff_tooltip")
            else if (isExpanded) getString("expcol_button_tearoff_and_expanded_tooltip")
            else getString("expcol_button_tearoff_and_collapsed_tooltip")
        )
        mDragHandle?.setTitleVisible(!isNotTornOff)
        mExpandCollapseBtn?.setEnabled(isNotTornOff || !mIsP2PChat)
        mTearOffBtn?.setImageOverlay(getString(if (isNotTornOff) "tear_off_icon" else "return_icon"))
        mTearOffBtn?.setToolTip(getString(if (isNotTornOff) "tooltip_to_separate_window" else "tooltip_to_main_window"))
        mCloseBtn?.setVisible(isNotTornOff && !mIsNearbyChat)
        enableDisableCallBtn()
    }

    protected fun reshapeFloater(collapse: Boolean) {
        val floaterRect = getRect()
        if (collapse) {
            mFloaterHeight = floaterRect.getHeight()
            val height = mContentPanel!!.getRect().getHeight() + mToolbarPanel!!.getRect().getHeight() +
                    mChatLayoutPanel!!.getRect().getHeight() - mChatLayoutPanelHeight + 2
            floaterRect.mTop -= height
            setResizeLimits(getMinWidth(), floaterRect.getHeight())
        } else {
            floaterRect.mTop = floaterRect.mBottom + mFloaterHeight
            setResizeLimits(getMinWidth(), mMinFloaterHeight)
        }
        enableResizeCtrls(true, true, !collapse)
        saveCollapsedState()
        setShape(floaterRect, true)
        mBodyStack?.updateLayout()
    }

    private fun checkIfTornOff(): Boolean {
        val isTorn = getHost() == null
        if (isTorn != isTornOff()) {
            setTornOff(isTorn)
            refreshConversation()
        }
        return isTorn
    }

    private fun onCallButtonClicked() {
        if (mVoiceButtonHangUpMode) {
            gIMMgr.endCall(mSessionID)
        } else {
            val session = mSession
            if (session?.mVoiceChannel != null && !session.mVoiceChannel!!.callStarted()) {
                gIMMgr.startCall(mSessionID)
            }
        }
    }

    private fun onInputEditorClicked() {
        val imBox = LLFloaterIMContainer.findInstance()
        imBox?.flashConversationItemWidget(mSessionID, false)
        gToolBarView.flashCommand(LLCommandId("chat"), false)
    }

    private fun onEmojiRecentPanelToggleBtnClicked() {
        val show = mEmojiRecentPanel?.getVisible() != true
        if (show) initEmojiRecentPanel()
        mEmojiRecentPanel?.setVisible(show)
        mInputEditor?.setFocus(true)
    }

    private fun onEmojiPickerShowBtnClicked() {
        if (mEmojiPickerShowBtn?.getToggleState() != true) {
            mInputEditor?.hideEmojiHelper()
            mInputEditor?.setFocus(true)
            mInputEditor?.showEmojiHelper()
            mEmojiPickerShowBtn?.setToggleState(true)
        } else {
            mInputEditor?.hideEmojiHelper()
            mEmojiPickerShowBtn?.setToggleState(false)
        }
    }

    private fun onEmojiPickerShowBtnDown() {
        if (mEmojiHelperLastCallbackFrame == LLFrameTimer.getFrameCount()) {
            mEmojiPickerShowBtn?.setToggleState(true)
        }
    }

    private fun onEmojiPickerClosed() {
        if (mEmojiPickerShowBtn?.getToggleState() == true) {
            mEmojiPickerShowBtn?.setToggleState(false)
            mEmojiHelperLastCallbackFrame = LLFrameTimer.getFrameCount()
        }
    }

    private fun initEmojiRecentPanel() {
        val recentlyUsed = LLFloaterEmojiPicker.getRecentlyUsed()
        if (recentlyUsed.isEmpty()) {
            mEmojiRecentEmptyText?.setVisible(true)
            mEmojiRecentContainer?.setVisible(false)
        } else {
            val emojis = recentlyUsed.joinToString("")
            mEmojiRecentIconsCtrl?.setEmojis(emojis)
            mEmojiRecentEmptyText?.setVisible(false)
            mEmojiRecentContainer?.setVisible(true)
        }
    }

    private fun onEmojiRecentPanelFocusReceived() {
        mEmojiRecentContainer?.addBorder()
    }

    private fun onEmojiRecentPanelFocusLost() {
        mEmojiRecentContainer?.removeBorder()
    }

    private fun onRecentEmojiPicked(value: Any) {
        val str = value.toString()
        if (str.isNotEmpty()) {
            mInputEditor?.insertEmoji(str[0])
        }
    }

    private fun reshapeChatLayoutPanel() {
        mChatLayoutPanel?.reshape(
            mChatLayoutPanel!!.getRect().getWidth(),
            mInputEditor!!.getRect().getHeight() + mInputEditorPad,
            false
        )
    }

    private fun doToSelected(userdata: String) {
        val selectedUuids = mutableListOf<UUID>()
        getSelectedUUIDs(selectedUuids)
        LLFloaterIMContainer.getInstance().doToParticipants(userdata, selectedUuids)
    }

    private fun enableContextMenuItem(userdata: String): Boolean {
        val selectedUuids = mutableListOf<UUID>()
        getSelectedUUIDs(selectedUuids)
        return LLFloaterIMContainer.getInstance().enableContextMenuItem(userdata, selectedUuids)
    }

    private fun checkContextMenuItem(userdata: String): Boolean {
        val selectedUuids = mutableListOf<UUID>()
        getSelectedUUIDs(selectedUuids)
        return LLFloaterIMContainer.getInstance().checkContextMenuItem(userdata, selectedUuids)
    }

    private fun getSelectedUUIDs(selectedUuids: MutableList<UUID>) {
        val selectedItems = mConversationsRoot?.getSelectionList() ?: return
        for (item in selectedItems) {
            val conversationItem = item.getViewModelItem() as? LLConversationItem
            if (conversationItem != null) {
                selectedUuids.add(conversationItem.getUUID())
            }
        }
    }

    protected abstract fun refresh()

    companion object {
        fun processChatHistoryStyleUpdate(cleanMessages: Boolean = false) {
            val instList = LLFloaterReg.getFloaterList("impanel")
            for (floater in instList) {
                val imSession = floater as? LLFloaterIMSession
                imSession?.reloadMessages(cleanMessages)
            }
            val nearbyChat = LLFloaterReg.findTypedInstance<LLFloaterIMNearbyChat>("nearby_chat")
            nearbyChat?.reloadMessages(cleanMessages)
        }

        fun reloadEmptyFloaters() {
            val instList = LLFloaterReg.getFloaterList("impanel")
            for (floater in instList) {
                val imSession = floater as? LLFloaterIMSession
                if (imSession != null && imSession.getLastChatMessageIndex() == -1) {
                    imSession.reloadMessages(true)
                }
            }
            val nearbyChat = LLFloaterReg.findTypedInstance<LLFloaterIMNearbyChat>("nearby_chat")
            if (nearbyChat != null && nearbyChat.getMessageArchiveLength() == 0) {
                nearbyChat.reloadMessages(true)
            }
        }

        fun isChatMultiTab(): Boolean = true

        fun addToHost(sessionId: UUID) {
            if ((sessionId != UUID.fromString("00000000-0000-0000-0000-000000000000") && !gIMMgr.hasSession(sessionId)) ||
                !isChatMultiTab()) return

            val conversp = getConversation(sessionId) ?: return
            val floaterContainer = LLFloaterIMContainer.getInstance()
            if (floaterContainer != null && !conversp.isHostAttached()) {
                conversp.setHostAttached(true)
                if (!conversp.isNearbyChat() || gSavedPerAccountSettings.getBool("NearbyChatIsNotTornOff")) {
                    floaterContainer.addFloater(conversp, false, LLTabContainer.RIGHT_OF_CURRENT)
                } else {
                    conversp.setHost(floaterContainer)
                    conversp.setHost(null)
                    conversp.forceReshape()
                }
                conversp.setSortOrder(floaterContainer.getSortOrder())
            }
        }

        fun findConversation(uuid: UUID): LLFloaterIMSessionTab? =
            if (uuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                LLFloaterReg.findTypedInstance<LLFloaterIMSessionTab>("nearby_chat")
            } else {
                LLFloaterReg.findTypedInstance<LLFloaterIMSessionTab>("impanel", uuid)
            }

        fun getConversation(uuid: UUID): LLFloaterIMSessionTab? =
            if (uuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                LLFloaterReg.getTypedInstance<LLFloaterIMSessionTab>("nearby_chat")
            } else {
                LLFloaterReg.getTypedInstance<LLFloaterIMSessionTab>("impanel", uuid)?.also {
                    it.setOpenPositioning(LLFloaterEnums.POSITIONING_RELATIVE)
                }
            }

        fun onSlide(self: LLFloaterIMSessionTab) {
            val hostFloater = self.getHost() as? LLFloaterIMContainer
            var shouldBeExpanded = false
            if (hostFloater != null) {
                hostFloater.collapseMessagesPane(true)
            } else {
                if (!self.mIsP2PChat) {
                    shouldBeExpanded = self.mParticipantListPanel?.isCollapsed() ?: false
                    gSavedSettings.setBool("IMShowControlPanel", shouldBeExpanded)
                    self.mIsParticipantListExpanded = shouldBeExpanded
                    self.refreshConversation()
                }
            }
            self.assignResizeLimits()
            if (shouldBeExpanded) self.forceReshape()
        }

        fun onCollapseToLine(self: LLFloaterIMSessionTab) {
            val hostFloater = self.getHost() as? LLFloaterIMContainer
            if (hostFloater == null) {
                val expand = self.isMessagePaneExpanded()
                self.mExpandCollapseLineBtn?.setImageOverlay(self.getString(if (expand) "collapseline_icon" else "expandline_icon"))
                self.mContentPanel?.setVisible(!expand)
                self.mToolbarPanel?.setVisible(!expand)
                self.mInputEditor?.enableSingleLineMode(expand)
                self.reshapeFloater(expand)
                self.setMessagePaneExpanded(!expand)
            }
        }
    }
}
