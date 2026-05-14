package com.firestorm.newview

import java.util.UUID

typealias FloaterShowedSignal = MutableList<(UUID) -> Unit>

const val ME_TYPING_TIMEOUT: Float = 4.0f
const val OTHER_TYPING_TIMEOUT: Float = 9.0f

class LLFloaterIMSession(private val sessionId: UUID)
    : LLFloaterIMSessionTab(sessionId), LLVoiceClientStatusObserver {

    private var mLastMessageIndex: Int = -1
    private var mDialog: EInstantMessage = EInstantMessage.IM_NOTHING_SPECIAL
    private var mOtherParticipantUUID: UUID = UUID(0, 0)
    private var mPositioned: Boolean = false

    private var mTypingStart: LLUIString = LLUIString("")
    private var mMeTyping: Boolean = false
    private var mOtherTyping: Boolean = false
    private var mShouldSendTypingState: Boolean = false
    private var mTypingTimer: LLFrameTimer = LLFrameTimer()
    private var mTypingTimeoutTimer: LLFrameTimer = LLFrameTimer()
    private var mSessionNameUpdatedForTyping: Boolean = false
    private var mMeTypingTimer: LLFrameTimer = LLFrameTimer()
    private var mOtherTypingTimer: LLFrameTimer = LLFrameTimer()

    private var mSessionInitialized: Boolean = false
    private var mQueuedMsgsForInit: LLSD = LLSD.emptyArray()

    private val mInvitedParticipants: MutableList<UUID> = mutableListOf()
    private val mPendingParticipants: MutableList<UUID> = mutableListOf()

    private var mVoiceChannelChanged: (() -> Unit)? = null
    private var mVoiceChannelStateChangeConnection: (() -> Unit)? = null

    private var mImFromId: UUID = UUID(0, 0)

    init {
        mIsNearbyChat = false
        initIMSession(sessionId)
        setOverlapsScreenChannel(true)
        LLTransientFloaterMgr.getInstance().addControlView(LLTransientFloaterMgr.IM, this)
        mVoiceChannelChanged = LLVoiceChannel.setCurrentVoiceChannelChangedCallback { id ->
            onVoiceChannelChanged(id)
        }
        setDocked(true)
    }

    override fun postBuild(): Boolean {
        val result = super.postBuild()

        mInputEditor.setMaxTextLength(1023)
        mInputEditor.setAutoreplaceCallback { a, b, c, d, e ->
            LLAutoReplace.getInstance().autoreplaceCallback(a, b, c, d, e)
        }
        mInputEditor.setFocusReceivedCallback { onInputEditorFocusReceived() }
        mInputEditor.setFocusLostCallback { onInputEditorFocusLost() }
        mInputEditor.setKeystrokeCallback { onInputEditorKeystroke() }
        mInputEditor.setCommitCallback { _, _ -> sendMsgFromInputEditor(); setTyping(false) }

        setDocked(true)

        val addBtn = getChild<LLButton>("add_btn")
        addBtn.setEnabled(isInviteAllowed())
        addBtn.setClickedCallback { onAddButtonClicked() }

        LLVoiceClient.addObserver(this)
        initIMFloater()
        return result
    }

    override fun onDestroy() {
        mVoiceChannelStateChangeConnection?.invoke()
        LLVoiceClient.removeObserver(this)
        LLTransientFloaterMgr.getInstance().removeControlView(LLTransientFloaterMgr.IM, this)
        mVoiceChannelChanged?.invoke()
    }

    fun initIMSession(sessionId: UUID) {
        setKey(sessionId)
        mSessionID = sessionId
        mSession = LLIMModel.getInstance().findIMSession(mSessionID)
        mSession?.let {
            mIsP2PChat = it.isP2PSessionType()
            mSessionInitialized = it.mSessionInitialized
            mDialog = it.mType
        }
    }

    fun initIMFloater() {
        val otherPartyId = LLIMModel.getInstance().getOtherParticipantID(mSessionID)
        if (otherPartyId != UUID(0, 0)) {
            mOtherParticipantUUID = otherPartyId
        }
        boundVoiceChannel()
        mTypingStart = LLTrans.getString("IM_typing_start_string")

        mParticipantListPanel.setVisible(getHost() == null && gSavedSettings.getBOOL("IMShowControlPanel"))

        mSession?.let {
            if (!it.mTextIMPossible) {
                mInputEditor.setEnabled(false)
                mInputEditor.setLabel(LLTrans.getString("IM_unavailable_text_label"))
            }
        }

        if (!mIsP2PChat) {
            val sessionName = LLIMModel.instance().getName(mSessionID)
            updateSessionName(sessionName)
        }
    }

    override fun refresh() {
        if (mMeTyping) {
            if (mMeTypingTimer.getElapsedTimeF32() > ME_TYPING_TIMEOUT && !mShouldSendTypingState) {
                LLIMModel.instance().sendTypingState(mSessionID, mOtherParticipantUUID, true)
                mMeTypingTimer.reset()
            }
            if (mTypingTimeoutTimer.getElapsedTimeF32() > LLAgent.TYPING_TIMEOUT_SECS) {
                setTyping(false)
            }
        }
        if (mOtherTyping && mOtherTypingTimer.getElapsedTimeF32() > OTHER_TYPING_TIMEOUT) {
            removeTypingIndicator(mImFromId)
            mOtherTyping = false
        }
    }

    override fun onTearOffClicked() {
        super.onTearOffClicked()
    }

    override fun onClickCloseBtn(appQuitting: Boolean) {
        if (appQuitting) {
            super.onClickCloseBtn(appQuitting)
            return
        }
        val session = LLIMModel.instance().findIMSession(mSessionID)
        if (session != null) {
            val isCallWithChat = session.isGroupSessionType()
                || session.isAdHocSessionType() || session.isP2PSessionType()
            val voiceChannel = LLIMModel.getInstance().getVoiceChannel(mSessionID)
            if (isCallWithChat && voiceChannel != null && voiceChannel.isActive()) {
                val payload = LLSD()
                payload["session_id"] = mSessionID
                LLNotificationsUtil.add("ConfirmLeaveCall", LLSD(), payload) { n, r ->
                    confirmLeaveCallCallback(n, r)
                }
                return
            }
        }
        super.onClickCloseBtn(appQuitting)
    }

    override fun onClose(appQuitting: Boolean) {
        setTyping(false)
        gIMMgr.leaveSession(mSessionID)
        mSession = null
        restoreFloater()
        super.onClose(appQuitting)
    }

    override fun setDocked(docked: Boolean, popOnUndock: Boolean) {
        val channel = LLNotificationsUI.LLChannelManager.getInstance()
            .findChannelByID(LLNotificationsUI.NOTIFICATION_CHANNEL_UUID)
        if (!isChatMultiTab()) {
            super.setDocked(docked, popOnUndock)
        }
        channel?.apply {
            updateShowToastsState()
            redrawToasts()
        }
    }

    override fun setMinimized(b: Boolean) {
        val wasMinimized = isMinimized()
        super.setMinimized(b)
        if (wasMinimized && !b && gAgent.isDoNotDisturb()) {
            LLDoNotDisturbNotificationStorage.getInstance()
                .removeNotification(LLDoNotDisturbNotificationStorage.toastName, mSessionID)
        }
    }

    override fun setVisible(visible: Boolean) {
        val channel = LLNotificationsUI.LLChannelManager.getInstance()
            .findChannelByID(LLNotificationsUI.NOTIFICATION_CHANNEL_UUID)
        super.setVisible(visible)
        channel?.apply {
            updateShowToastsState()
            redrawToasts()
        }
        if (!visible) {
            val chicletPanel = LLChicletBar.getInstance().getChicletPanel()
            chicletPanel?.findChiclet<LLIMChiclet>(mSessionID)?.setToggleState(false)
        }
        if (visible && isInVisibleChain()) {
            sIMFloaterShowedSignal.forEach { it(mSessionID) }
            updateMessages()
        }
    }

    override fun getVisible(): Boolean {
        if (isChatMultiTab()) {
            val imContainer = LLFloaterIMContainer.getInstance()
            val isActive = imContainer.getActiveFloater() == this
            return if (!isActive && getHost() != imContainer) {
                super.getVisible()
            } else {
                isActive && !imContainer.isMinimized() && imContainer.getVisible()
            }
        }
        return super.getVisible()
    }

    override fun setFocus(focus: Boolean) {
        super.setFocus(focus)
        if (focus && gAgent.isDoNotDisturb()) {
            LLDoNotDisturbNotificationStorage.getInstance()
                .removeNotification(LLDoNotDisturbNotificationStorage.toastName, mSessionID)
        }
    }

    override fun draw() {
        if (mPendingParticipants.isNotEmpty()) {
            addSessionParticipants(mPendingParticipants.toList())
            mPendingParticipants.clear()
        }
        super.draw()
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: EDragAndDropType, cargoData: Any?,
        accept: EAcceptanceRef, tooltipMsg: StringBuilder
    ): Boolean {
        if (cargoType == EDragAndDropType.DAD_PERSON) {
            val accepted = dropPerson(cargoData as? UUID, drop)
            accept.value = if (accepted) EAcceptance.ACCEPT_YES_MULTI else EAcceptance.ACCEPT_NO
        } else if (mDialog == EInstantMessage.IM_NOTHING_SPECIAL) {
            LLToolDragAndDrop.handleGiveDragAndDrop(
                mOtherParticipantUUID, mSessionID, drop, cargoType, cargoData, accept
            )
        }
        return true
    }

    fun sessionInitReplyReceived(imSessionId: UUID) {
        mSessionInitialized = true
        if (mSessionID != imSessionId) {
            initIMSession(imSessionId)
            buildConversationViewParticipant()
        }
        initIMFloater()
        updateGearBtn()
        if (mQueuedMsgsForInit.size() > 0) {
            for (i in 0 until mQueuedMsgsForInit.size()) {
                LLIMModel.sendMessage(
                    mQueuedMsgsForInit[i].asString(),
                    mSessionID, mOtherParticipantUUID, mDialog
                )
            }
            mQueuedMsgsForInit.clear()
        }
    }

    override fun updateMessages() {
        val messages = mutableListOf<LLSD>()
        LLIMModel.instance().getMessages(mSessionID, messages, mLastMessageIndex + 1, hasFocus())
        if (messages.isNotEmpty()) {
            for (msg in messages.reversed()) {
                val time = msg["time"].asString()
                val fromId = msg["from_id"].asUUID()
                val from = msg["from"].asString()
                val message = msg["message"].asString()
                val isHistory = msg["is_history"].asBoolean()
                val isRegionMsg = msg["is_region_msg"].asBoolean()

                val chat = LLChat().apply {
                    mFromID = fromId
                    mSessionID = this@LLFloaterIMSession.mSessionID
                    mFromName = from
                    mTimeStr = time
                    if (isHistory) mChatStyle = CHAT_STYLE_HISTORY
                    if (isRegionMsg) mSourceType = CHAT_SOURCE_REGION
                }

                if (msg.has("notification_id")) {
                    chat.mNotifId = msg["notification_id"].asUUID()
                    if (LLNotificationsUtil.find(chat.mNotifId) != null) {
                        val channel = LLNotificationsUI.LLChannelManager.getInstance()
                            .findChannelByID(LLNotificationsUI.NOTIFICATION_CHANNEL_UUID)
                        if (getVisible()) {
                            channel?.hideToast(chat.mNotifId)
                        }
                    } else {
                        continue
                    }
                } else {
                    chat.mText = message
                }

                appendMessage(chat)
                mLastMessageIndex = msg["index"].asInteger()

                if (chat.mNotifId != UUID(0, 0) && LLNotificationsUtil.find(chat.mNotifId) != null) {
                    mLastMessageIndex++
                }
            }
        }
    }

    fun reloadMessages(cleanMessages: Boolean = false) {
        if (cleanMessages) {
            LLIMModel.instance().findIMSession(mSessionID)?.loadHistory()
        }
        mChatHistory.clear()
        mLastMessageIndex = -1
        updateMessages()
        mInputEditor.setFont(LLViewerChat.getChatFont())
    }

    fun sendMsgFromInputEditor() {
        if (gAgent.isGodlike() || mDialog != EInstantMessage.IM_NOTHING_SPECIAL
            || mOtherParticipantUUID != UUID(0, 0)
        ) {
            val text = mInputEditor.getConvertedText().trim()
                .replace('¶', '\n')
            if (text.isNotEmpty()) {
                updateUsedEmojis(text)
                sendMsg(text)
                mInputEditor.setText("")
            }
        }
    }

    fun sendMsg(msg: String) {
        val truncated = utf8StrTruncate(msg, MAX_MSG_BUF_SIZE - 1)
        if (mSessionInitialized) {
            LLIMModel.sendMessage(truncated, mSessionID, mOtherParticipantUUID, mDialog)
        } else {
            mQueuedMsgsForInit.append(truncated)
        }
        updateMessages()
    }

    fun processIMTyping(fromId: UUID, typing: Boolean) {
        if (typing) {
            addTypingIndicator(fromId)
            mOtherTypingTimer.reset()
        } else {
            removeTypingIndicator(fromId)
        }
    }

    fun processAgentListUpdates(body: LLSD) {
        val joinedUuids = mutableListOf<UUID>()
        if (body.isMap && body.has("agent_updates") && body["agent_updates"].isMap) {
            for ((key, agentData) in body["agent_updates"].asMap()) {
                val agentId = UUID.fromString(key)
                if (agentData.isMap) {
                    if (agentData.has("transition") && agentData["transition"].asString() == "ENTER") {
                        joinedUuids.add(agentId)
                    }
                    if (agentId == gAgentID && agentData.has("info") && agentData["info"].has("mutes")) {
                        val moderatorMutedText = agentData["info"]["mutes"]["text"].asBoolean()
                        mInputEditor.setEnabled(!moderatorMutedText)
                        val label = if (moderatorMutedText)
                            LLTrans.getString("IM_muted_text_label")
                        else
                            LLTrans.getString("IM_to_label") + " " + LLIMModel.instance().getName(mSessionID)
                        mInputEditor.setLabel(label)
                        if (moderatorMutedText) LLNotificationsUtil.add("TextChatIsMutedByModerator")
                    }
                }
            }
        }
        mInvitedParticipants.sort()
        joinedUuids.sort()
        val intersection = mInvitedParticipants.intersect(joinedUuids.toSet()).toList()
        if (intersection.isNotEmpty()) {
            sendParticipantsAddedNotification(intersection)
        }
        mInvitedParticipants.removeAll(joinedUuids.toSet())
    }

    fun processSessionUpdate(sessionUpdate: LLSD) {
        // Moderated mode handling retained for future implementation.
    }

    fun enableGearMenuItem(userdata: LLSD): Boolean {
        val command = userdata.asString()
        val selectedUuids = listOf(mOtherParticipantUUID)
        return LLFloaterIMContainer.getInstance().enableContextMenuItem(command, selectedUuids)
    }

    fun gearDoToSelected(userdata: LLSD) {
        val command = userdata.asString()
        val selectedUuids = listOf(mOtherParticipantUUID)
        LLFloaterIMContainer.getInstance().doToParticipants(command, selectedUuids)
    }

    fun checkGearMenuItem(userdata: LLSD): Boolean {
        val command = userdata.asString()
        val selectedUuids = listOf(mOtherParticipantUUID)
        return LLFloaterIMContainer.getInstance().checkContextMenuItem(command, selectedUuids)
    }

    override fun onChange(status: EStatusType, channelInfo: LLSD, proximal: Boolean) {
        if (status != EStatusType.STATUS_JOINING && status != EStatusType.STATUS_LEFT_CHANNEL) {
            enableDisableCallBtn()
        }
    }

    override fun onVoiceChannelStateChanged(
        oldState: LLVoiceChannel.EState,
        newState: LLVoiceChannel.EState
    ) {
        updateCallBtnState(newState >= LLVoiceChannel.EState.STATE_CALL_STARTED)
    }

    override fun getGroup(): LLTransientFloaterMgr.ETransientGroup {
        return LLTransientFloaterMgr.ETransientGroup.IM
    }

    override fun updateSessionName(name: String) {
        if (name.isNotEmpty()) {
            super.updateSessionName(name)
            mTypingStart.setArg("[NAME]", name)
            setTitle(if (mOtherTyping) mTypingStart.getString() else name)
            mSessionNameUpdatedForTyping = mOtherTyping
        }
    }

    fun getOtherParticipantUUID(): UUID = mOtherParticipantUUID

    fun needsTitleOverwrite(): Boolean = mSessionNameUpdatedForTyping && mOtherTyping

    fun getLastChatMessageIndex(): Int = mLastMessageIndex

    fun setPositioned(b: Boolean) { mPositioned = b }

    fun onVisibilityChanged(newVisibility: LLSD) {
        val visible = newVisibility.asBoolean()
        val voiceChannel = LLIMModel.getInstance().getVoiceChannel(mSessionID)
        if (visible && voiceChannel?.getState() == LLVoiceChannel.EState.STATE_CONNECTED) {
            LLFloaterReg.showInstance("voice_call", mSessionID)
        } else {
            LLFloaterReg.hideInstance("voice_call", mSessionID)
        }
    }

    private fun onAddButtonClicked() {
        val button = findChild<LLView>("toolbar_panel")?.findChild<LLButton>("add_btn")
        val rootFloater = gFloaterView.getParentFloater(this)
        val picker = LLFloaterAvatarPicker.show({ ids ->
            addSessionParticipants(ids)
        }, true, true, false, rootFloater?.getName() ?: "", button) ?: return
        picker.setOkBtnEnableCb { ids -> canAddSelectedToChat(ids) }
        rootFloater?.addDependentFloater(picker)
    }

    private fun canAddSelectedToChat(uuids: List<UUID>): Boolean {
        if (mSession == null
            || mDialog == EInstantMessage.IM_SESSION_GROUP_START
            || (mDialog == EInstantMessage.IM_SESSION_INVITE && gAgent.isInGroup(mSessionID))
        ) return false

        if (mIsP2PChat) {
            return uuids.none { it == mOtherParticipantUUID }
        } else {
            val speakerMgr = LLIMModel.getInstance().getSpeakerManager(mSessionID)
            val speakerList = mutableListOf<LLSpeaker>()
            speakerMgr?.getSpeakerList(speakerList, true)
            return uuids.none { id -> speakerList.any { it.mID == id } }
        }
    }

    private fun addSessionParticipants(uuids: List<UUID>) {
        if (mIsP2PChat) {
            LLNotificationsUtil.add("ConfirmAddingChatParticipants", LLSD(), LLSD()) { n, r ->
                addP2PSessionParticipants(n, r, uuids)
            }
        } else {
            if (findInstance(mSessionID) != null) {
                mInvitedParticipants.addAll(uuids)
            }
            inviteToSession(uuids)
        }
    }

    private fun addP2PSessionParticipants(notification: LLSD, response: LLSD, uuids: List<UUID>) {
        if (LLNotificationsUtil.getSelectedOption(notification, response) != 0) return
        val voiceChannel = LLIMModel.getInstance().getVoiceChannel(mSessionID)
        val isVoiceCall = voiceChannel?.isActive() == true
        val tempIds = mutableListOf(mOtherParticipantUUID).also { it.addAll(uuids) }
        if (findInstance(mSessionID) != null) {
            onClose(false)
            mInvitedParticipants.addAll(uuids)
        }
        mSessionInitialized = false
        if (isVoiceCall) {
            LLAvatarActions.startAdhocCall(tempIds, mSessionID)
        } else {
            LLAvatarActions.startConference(tempIds, mSessionID)
        }
    }

    private fun sendParticipantsAddedNotification(uuids: List<UUID>) {
        val namesString = LLAvatarActions.buildResidentsString(uuids)
        val key = if (uuids.size > 1) "multiple_participants_added" else "participant_added"
        sendMsg(getString(key).replace("[NAME]", namesString))
    }

    private fun onVoiceChannelChanged(sessionId: UUID) {
        if (sessionId == mSessionID) {
            boundVoiceChannel()
        }
    }

    private fun boundVoiceChannel() {
        val voiceChannel = LLIMModel.getInstance().getVoiceChannel(mSessionID) ?: return
        mVoiceChannelStateChangeConnection?.invoke()
        mVoiceChannelStateChangeConnection = voiceChannel.setStateChangedCallback { old, new ->
            onVoiceChannelStateChanged(old, new)
        }
        updateCallBtnState(voiceChannel.getState() >= LLVoiceChannel.EState.STATE_CALL_STARTED)
    }

    private fun onInputEditorFocusReceived() {
        val imSession = LLIMModel.instance().findIMSession(mSessionID)
        if (imSession != null && imSession.mTextIMPossible && !mInputEditor.getReadOnly()) {
            mInputEditor.setEnabled(!gDisconnected)
        }
    }

    private fun onInputEditorFocusLost() {
        setTyping(false)
    }

    private fun onInputEditorKeystroke() {
        LLFloaterIMContainer.findInstance()?.flashConversationItemWidget(mSessionID, false)
        val text = mInputEditor.getText()
        setTyping(text.isNotEmpty())
    }

    private fun setTyping(typing: Boolean) {
        if (typing) {
            mTypingTimeoutTimer.reset()
        }
        if (mMeTyping != typing) {
            mMeTyping = typing
            mShouldSendTypingState = true
            mTypingTimer.reset()
        }
        if (mShouldSendTypingState && mDialog == EInstantMessage.IM_NOTHING_SPECIAL) {
            if (mMeTyping) {
                if (mTypingTimer.getElapsedTimeF32() > 1.0f) {
                    LLIMModel.instance().sendTypingState(mSessionID, mOtherParticipantUUID, true)
                    mShouldSendTypingState = false
                    mMeTypingTimer.reset()
                }
            } else {
                LLIMModel.instance().sendTypingState(mSessionID, mOtherParticipantUUID, false)
                mShouldSendTypingState = false
            }
        }
        if (!mIsNearbyChat) {
            LLIMModel.getInstance().getSpeakerManager(mSessionID)
                ?.setSpeakerTyping(gAgent.getID(), false)
        }
    }

    private fun addTypingIndicator(fromId: UUID) {
        if (fromId != UUID(0, 0) && !mOtherTyping) {
            mOtherTyping = true
            mOtherTypingTimer.reset()
            mImFromId = fromId
            LLIMModel.getInstance().getSpeakerManager(mSessionID)
                ?.setSpeakerTyping(fromId, true)
        }
    }

    private fun removeTypingIndicator(fromId: UUID = UUID(0, 0)) {
        if (mOtherTyping) {
            mOtherTyping = false
            if (fromId != UUID(0, 0)) {
                LLIMModel.getInstance().getSpeakerManager(mSessionID)
                    ?.setSpeakerTyping(fromId, false)
            }
        }
    }

    private fun dropPerson(personId: UUID?, drop: Boolean): Boolean {
        if (personId == null || personId == UUID(0, 0)) return false
        val ids = listOf(personId)
        val canAdd = canAddSelectedToChat(ids)
        if (canAdd && drop) {
            mPendingParticipants.add(personId)
        }
        return canAdd
    }

    private fun isInviteAllowed(): Boolean {
        return mDialog == EInstantMessage.IM_SESSION_CONFERENCE_START
            || (mDialog == EInstantMessage.IM_SESSION_INVITE && !gAgent.isInGroup(mSessionID))
            || mIsP2PChat
    }

    private fun inviteToSession(ids: List<UUID>): Boolean {
        val region = gAgent.getRegion() ?: return false
        if (isInviteAllowed() && ids.isNotEmpty()) {
            val url = region.getCapability("ChatSessionRequest")
            val data = LLSD().apply {
                this["params"] = LLSD.emptyArray().also { arr -> ids.forEach { arr.append(it) } }
                this["method"] = "invite"
                this["session-id"] = mSessionID
            }
            System.err.println("LLFloaterIMSession: sendInviteToSession not yet implemented")
        }
        return true
    }

    companion object {
        val sIMFloaterShowedSignal: FloaterShowedSignal = mutableListOf()

        fun findInstance(sessionId: UUID): LLFloaterIMSession? {
            return LLFloaterReg.findTypedInstance<LLFloaterIMSession>("impanel", sessionId)
        }

        fun getInstance(sessionId: UUID): LLFloaterIMSession? {
            return LLFloaterReg.getTypedInstance<LLFloaterIMSession>("impanel", sessionId)
        }

        fun show(sessionId: UUID): LLFloaterIMSession? {
            closeHiddenIMToasts()
            if (!gIMMgr.hasSession(sessionId)) return null
            val exist = findInstance(sessionId) != null
            val floater = getInstance(sessionId) ?: return null
            val floaterContainer = LLFloaterIMContainer.getInstance()
            if (!exist) {
                floaterContainer?.addFloater(floater, true, LLTabContainer.END)
            }
            floater.openFloater(floater.getKey())
            floater.setVisible(true)
            return floater
        }

        fun toggle(sessionId: UUID): Boolean {
            if (!isChatMultiTab()) {
                val floater = LLFloaterReg.findTypedInstance<LLFloaterIMSession>("impanel", sessionId)
                if (floater != null && floater.getVisible() && floater.hasFocus()) {
                    floater.setVisible(false)
                    return false
                } else if (floater != null && ((!floater.isDocked() || floater.getVisible()) && !floater.hasFocus())) {
                    floater.setVisible(true)
                    floater.setFocus(true)
                    return true
                }
            }
            show(sessionId)
            return true
        }

        fun newIMCallback(data: LLSD) {
            if (data["num_unread"].asInteger() > 0 || data["from_id"].asUUID() == UUID(0, 0)) {
                val sessionId = data["session_id"].asUUID()
                val floater = LLFloaterReg.findTypedInstance<LLFloaterIMSession>("impanel", sessionId)
                if (floater != null && floater.isInVisibleChain()) {
                    floater.updateMessages()
                }
            }
        }

        fun sRemoveTypingIndicator(data: LLSD) {
            val sessionId = data["session_id"].asUUID()
            if (sessionId == UUID(0, 0)) return
            val fromId = data["from_id"].asUUID()
            if (gAgentID == fromId || UUID(0, 0) == fromId) return
            val floater = findInstance(sessionId) ?: return
            if (floater.mDialog != EInstantMessage.IM_NOTHING_SPECIAL) return
            floater.removeTypingIndicator()
        }

        fun onIMChicletCreated(sessionId: UUID) {
            LLFloaterIMSession.addToHost(sessionId)
        }

        fun setIMFloaterShowedCallback(cb: (UUID) -> Unit): () -> Unit {
            sIMFloaterShowedSignal.add(cb)
            return { sIMFloaterShowedSignal.remove(cb) }
        }

        private fun closeHiddenIMToasts() {
            val channel = LLNotificationsUI.LLChannelManager.getNotificationScreenChannel()
            channel?.closeHiddenToasts { notification ->
                notification.getType() == "notifytoast"
            }
        }

        private fun confirmLeaveCallCallback(notification: LLSD, response: LLSD) {
            val option = LLNotificationsUtil.getSelectedOption(notification, response)
            val sessionId = notification["payload"]["session_id"].asUUID()
            val imFloater = findInstance(sessionId)
            if (option == 0 && imFloater != null) {
                imFloater.closeFloater()
            }
        }
    }
}
