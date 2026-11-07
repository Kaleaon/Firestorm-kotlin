package com.firestorm.newview

import java.util.UUID

private const val VOICE_STATUS_UPDATE_INTERVAL: Float = 1.0f

class FSFloaterIMContainer(seed: LLSD) : LLMultiFloater(seed), LLIMSessionObserver {

    private enum class VoiceState {
        NONE, UNKNOWN, CONNECTED, NOT_CONNECTED, ERROR
    }

    private var activeVoiceFloater: LLFloater? = null
    private var activeVoiceUpdateTimer: LLTimer = LLTimer()
    private var currentVoiceState: VoiceState = VoiceState.NONE
    private var forceVoiceStateUpdate: Boolean = false
    private var isAddingNewSession: Boolean = false

    private val sessions: MutableMap<UUID, LLFloater> = mutableMapOf()
    private var newMessageConnection: (() -> Unit)? = null

    private val flashingSessions: MutableList<UUID> = mutableListOf()
    private val flashingTabStates: MutableMap<LLFloater, Boolean> = mutableMapOf()

    init {
        mAutoResize = false
        LLTransientFloaterMgr.getInstance().addControlView(LLTransientFloaterMgr.IM, this)
        LLIMMgr.getInstance().addSessionObserver(this)
    }

    fun destroy() {
        newMessageConnection = null
        LLTransientFloaterMgr.getInstance().removeControlView(LLTransientFloaterMgr.IM, this)
        if (LLIMMgr.instanceExists()) {
            LLIMMgr.getInstance().removeSessionObserver(this)
        }
    }

    override fun postBuild(): Boolean {
        newMessageConnection = LLIMModel.instance().mNewMsgSignal.connect { msg ->
            onNewMessageReceived(msg)
        }
        // Do not call super.postBuild() — that would wire the Close button to close all child floaters.

        mTabContainer.setAllowRearrange(true)
        mTabContainer.setRearrangeCallback { tabIndex, tabPanel -> onIMTabRearrange(tabIndex, tabPanel) }

        activeVoiceUpdateTimer.setTimerExpirySec(VOICE_STATUS_UPDATE_INTERVAL)
        activeVoiceUpdateTimer.start()

        gSavedSettings.getControl("FSShowConversationVoiceStateIndicator")
            ?.getSignal()
            ?.connect { _, data -> onVoiceStateIndicatorChanged(data) }

        return true
    }

    fun initTabs() {
        // Contacts and nearby-chat floaters are pinned as locked tabs if they are
        // hosted in this container. Mirror the tear-off / re-host logic from the
        // C++ original so that their saved visibility states are correct on login.
        val floaterContacts = FSFloaterContacts.getInstance()
        if (!LLFloater.isVisible(floaterContacts) && floaterContacts.getHost() != this) {
            if (gSavedSettings.getBOOL("ContactsTornOff")) {
                floaterContacts.setHost(this)
                floaterContacts.setHost(null)
                gFloaterView.addChild(floaterContacts)
            } else {
                addFloater(floaterContacts, true, EInstantMessage.IM_NOTHING_SPECIAL)
            }
        }

        val floaterChat = FSFloaterNearbyChat.getInstance()
        if (!LLFloater.isVisible(floaterChat) && floaterChat.getHost() != this) {
            if (gSavedSettings.getBOOL("ChatHistoryTornOff")) {
                floaterChat.setHost(this)
                floaterChat.setHost(null)
                gFloaterView.addChild(floaterChat)
            } else {
                addFloater(floaterChat, true, EInstantMessage.IM_NOTHING_SPECIAL)
            }
        }
    }

    override fun onOpen(key: LLSD) {
        super.onOpen(key)
        initTabs()
        val activeFloater = getActiveFloater()
        if (activeFloater != null && !activeFloater.hasFocus()) {
            mTabContainer.setFocus(true)
        }
    }

    override fun onClose(appQuitting: Boolean) {
        if (appQuitting) {
            saveOpenIMs()
            for (i in 0 until mTabContainer.getTabCount()) {
                val floater = mTabContainer.getPanelByIndex(i) as? FSFloaterIM
                floater?.onClose(appQuitting)
            }
        }
    }

    override fun addFloater(
        floaterp: LLFloater?,
        selectAddedFloater: Boolean,
        insertionPoint: LLTabContainer.InsertionPoint
    ) {
        if (floaterp == null) return
        val type = LLIMModel.getInstance().findIMSession(floaterp.getKey())?.type
            ?: EInstantMessage.IM_NOTHING_SPECIAL
        addFloater(floaterp, selectAddedFloater, type, insertionPoint)
    }

    fun addFloater(
        floaterp: LLFloater?,
        selectAddedFloater: Boolean,
        type: EInstantMessage,
        noAutoInsertionPoint: LLTabContainer.InsertionPoint = LLTabContainer.InsertionPoint.END
    ) {
        if (floaterp == null) return

        if (floaterp.getHost() == this) {
            openFloater(floaterp.getKey())
            return
        }

        forceVoiceStateUpdate = true

        val floaterName = floaterp.getName()
        if (floaterName == "imcontacts" || floaterName == "nearby_chat") {
            val numLockedTabs = mTabContainer.getNumLockedTabs()
            mTabContainer.unlockTabs()
            if (floaterName == "imcontacts") {
                super.addFloater(floaterp, selectAddedFloater, LLTabContainer.InsertionPoint.START)
                gSavedSettings.setBOOL("ContactsTornOff", false)
            } else {
                if (mTabContainer.getPanelByIndex(0) is FSFloaterContacts) {
                    mTabContainer.selectFirstTab()
                    super.addFloater(floaterp, selectAddedFloater, LLTabContainer.InsertionPoint.RIGHT_OF_CURRENT)
                } else {
                    super.addFloater(floaterp, selectAddedFloater, LLTabContainer.InsertionPoint.START)
                }
                gSavedSettings.setBOOL("ChatHistoryTornOff", false)
            }
            mTabContainer.lockTabs(numLockedTabs + 1)
            floaterp.setCanClose(false)
            return
        }

        var insertionPoint = noAutoInsertionPoint

        if (isAddingNewSession) {
            if (gSavedSettings.getBOOL("FSAutoOrderIMTabs")) {
                val orderPriorities = gSavedSettings.getString("FSAutoOrderIMTabsPriorities")
                val sortOrder = mutableMapOf(
                    EInstantMessage.IM_SESSION_GROUP_START to orderPriorities[0].code,
                    EInstantMessage.IM_SESSION_CONFERENCE_START to orderPriorities[1].code,
                    EInstantMessage.IM_NOTHING_SPECIAL to orderPriorities[2].code
                )
                val insertAtTop = gSavedSettings.getBOOL("FSAutoOrderIMTabsAtTop")

                val effectiveType = if (type == EInstantMessage.IM_SESSION_INVITE)
                    EInstantMessage.IM_SESSION_GROUP_START else type
                var typeFound = false

                var index = mTabContainer.getNumLockedTabs()
                while (index < mTabContainer.getTabCount()) {
                    val tab = mTabContainer.getPanelByIndex(index) as? FSFloaterIM
                    if (tab != null) {
                        val session = LLIMModel.getInstance().findIMSession(tab.getKey())
                        if (session != null) {
                            val sessionType = if (session.type == EInstantMessage.IM_SESSION_INVITE)
                                EInstantMessage.IM_SESSION_GROUP_START else session.type
                            if (sessionType == effectiveType) {
                                if (insertAtTop) break
                                typeFound = true
                            } else if (typeFound) {
                                break
                            } else if ((sortOrder[sessionType] ?: 0) < (sortOrder[effectiveType] ?: 0)) {
                                break
                            }
                        }
                    }
                    index++
                }
                insertionPoint = LLTabContainer.InsertionPoint.atIndex(index)
            }
        } else {
            // When re-docking a torn-off floater, restore it to its previous chiclet position.
            if (floaterp.isTornOff()) {
                val chicletPanel = LLChicletBar.instance().getChicletPanel()
                var pChiclet = chicletPanel.findChiclet<LLIMChiclet>(floaterp.getKey())
                if (pChiclet != null) {
                    var idxChiclet = chicletPanel.getChicletIndex(pChiclet)
                    if (idxChiclet in 1 until chicletPanel.getChicletCount()) {
                        while (--idxChiclet >= 0) {
                            val candidate = chicletPanel.getChiclet(idxChiclet) as? LLIMChiclet
                            if (candidate != null) {
                                pChiclet = candidate
                                val pFloater = FSFloaterIM.findInstance(candidate.getSessionId())
                                if (pFloater != null) {
                                    insertionPoint = LLTabContainer.InsertionPoint.atIndex(
                                        mTabContainer.getIndexForPanel(pFloater) + 1
                                    )
                                    break
                                }
                            }
                        }
                    } else {
                        insertionPoint = if (idxChiclet == 0)
                            LLTabContainer.InsertionPoint.START
                        else
                            LLTabContainer.InsertionPoint.END
                    }
                }
            }
        }

        val activeFloater = getActiveFloater()
        super.addFloater(floaterp, selectAddedFloater, insertionPoint)
        if (!selectAddedFloater && activeFloater != null) {
            selectFloater(activeFloater)
        }

        val sessionId = floaterp.getKey()
        sessions[sessionId] = floaterp
        floaterp.mCloseSignal.connect { onCloseFloater(sessionId) }
    }

    fun addNewSession(floaterp: LLFloater, type: EInstantMessage) {
        isAddingNewSession = true
        addFloater(floaterp, false, type)
        isAddingNewSession = false
    }

    override fun removeFloater(floaterp: LLFloater) {
        val floaterName = floaterp.getName()
        var settingName = ""
        var needsUnlock = false

        when (floaterName) {
            "nearby_chat" -> { settingName = "ChatHistoryTornOff"; needsUnlock = true }
            "imcontacts" -> { settingName = "ContactsTornOff"; needsUnlock = true }
        }

        if (needsUnlock) {
            val numLockedTabs = mTabContainer.getNumLockedTabs()
            if (numLockedTabs > 1) {
                mTabContainer.lockTabs(numLockedTabs - 1)
            } else {
                mTabContainer.unlockTabs()
            }
            gSavedSettings.setBOOL(settingName, true)
            floaterp.setCanClose(true)
        }

        flashingTabStates.remove(floaterp)
        super.removeFloater(floaterp)
    }

    fun hasFloater(floaterp: LLFloater): Boolean {
        for (i in 0 until mTabContainer.getTabCount()) {
            if (mTabContainer.getPanelByIndex(i) == floaterp) return true
        }
        return false
    }

    fun onCloseFloater(id: UUID) {
        sessions.remove(id)
        if (isShown()) {
            setFocus(true)
        } else if (isMinimized()) {
            setMinimized(true)
        }
    }

    private fun onNewMessageReceived(msg: LLSD) {
        val sessionId = msg["session_id"].asUUID()
        val floaterp = sessions[sessionId]
        val currentFloater = getActiveFloater()

        if (floaterp != null && currentFloater != null && floaterp != currentFloater
            && (gSavedSettings.getBOOL("FSIMChatFlashOnFriendStatusChange")
                    || !msg.has("from_id")
                    || msg["from_id"].asUUID() != UUID(0, 0))) {
            startFlashingTab(floaterp, msg["message"].asString())
        }
    }

    override fun getCurrentTransparency(): Float {
        val imOpacity = gSavedSettings.getFloat("FSIMOpacity", 1.0f)
        val imActiveOpacityOverride = gSavedSettings.getBoolean("FSImActiveOpacityOverride", false)

        val floaterOpacity = super.getCurrentTransparency()
        if (imActiveOpacityOverride && getTransparencyType() == TransparencyType.ACTIVE) {
            return floaterOpacity
        }
        return minOf(imOpacity, floaterOpacity)
    }

    override fun setVisible(b: Boolean) {
        super.setVisible(b)
        if (b) {
            flashingSessions.clear()
        }
    }

    override fun setMinimized(b: Boolean) {
        if (mTabContainer != null) {
            (mTabContainer.getCurrentPanel() as? FSFloaterNearbyChat)?.handleMinimized(b)
            (mTabContainer.getCurrentPanel() as? FSFloaterIM)?.handleMinimized(b)
        }
        super.setMinimized(b)
    }

    override fun sessionAdded(
        sessionId: UUID,
        name: String,
        otherParticipantId: UUID,
        hasOfflineMsg: Boolean
    ) {
        LLIMModel.getInstance().findIMSession(sessionId) ?: return
        FSFloaterIM.onNewIMReceived(sessionId)
    }

    override fun sessionActivated(sessionId: UUID, name: String, otherParticipantId: UUID) = Unit

    override fun sessionVoiceOrIMStarted(sessionId: UUID) = Unit

    override fun sessionRemoved(sessionId: UUID) {
        LLFloaterReg.findTypedInstance<FSFloaterIM>("fs_impanel", sessionId)?.closeFloater()

        val found = flashingSessions.indexOf(sessionId)
        if (found >= 0) {
            flashingSessions.removeAt(found)
            checkFlashing()
        }
    }

    override fun sessionIDUpdated(oldSessionId: UUID, newSessionId: UUID) {
        val floaterp = sessions.remove(oldSessionId)
        if (floaterp != null) {
            sessions[newSessionId] = floaterp
        }
    }

    override fun draw() {
        val fsShowVoiceStateIndicator = gSavedSettings.getBoolean("FSShowConversationVoiceStateIndicator", false)
        if (fsShowVoiceStateIndicator && (activeVoiceUpdateTimer.hasExpired() || forceVoiceStateUpdate)) {
            val currentVoiceFloater = getCurrentVoiceFloater()
            if (activeVoiceFloater != currentVoiceFloater) {
                activeVoiceFloater?.let { mTabContainer.setTabImage(it, "") }
                currentVoiceState = VoiceState.NONE
            }

            if (currentVoiceFloater != null) {
                val voiceConnectedColor = LLUIColorTable.instance().getColor("VoiceConnectedColor")
                val voiceErrorColor = LLUIColorTable.instance().getColor("VoiceErrorColor")
                val voiceNotConnectedColor = LLUIColorTable.instance().getColor("VoiceNotConnectedColor")

                var voiceState = VoiceState.UNKNOWN
                val voiceChannel = LLVoiceChannel.getCurrentVoiceChannel()
                if (voiceChannel != null) {
                    voiceState = when {
                        voiceChannel.isActive() -> VoiceState.CONNECTED
                        voiceChannel.getState() == LLVoiceChannel.STATE_ERROR -> VoiceState.ERROR
                        else -> VoiceState.NOT_CONNECTED
                    }
                }

                if (voiceState != currentVoiceState || forceVoiceStateUpdate) {
                    val iconColor = when (voiceState) {
                        VoiceState.CONNECTED -> voiceConnectedColor
                        VoiceState.ERROR -> voiceErrorColor
                        VoiceState.NOT_CONNECTED -> voiceNotConnectedColor
                        else -> LLColor4.WHITE
                    }
                    mTabContainer.setTabImage(currentVoiceFloater, "Active_Voice_Tab", LLFontGL.RIGHT, iconColor, iconColor)
                    currentVoiceState = voiceState
                }
            }

            forceVoiceStateUpdate = false
            activeVoiceFloater = currentVoiceFloater
            activeVoiceUpdateTimer.setTimerExpirySec(VOICE_STATUS_UPDATE_INTERVAL)
        }

        super.draw()
    }

    private fun getCurrentVoiceFloater(): LLFloater? {
        if (!LLVoiceClient.instance().voiceEnabled()) return null

        if (LLVoiceChannelProximal.getInstance() == LLVoiceChannel.getCurrentVoiceChannel()) {
            return FSFloaterNearbyChat.getInstance()
        }

        for (i in 0 until mTabContainer.getTabCount()) {
            val imFloater = mTabContainer.getPanelByIndex(i) as? FSFloaterIM
            if (imFloater?.getVoiceChannel() == LLVoiceChannel.getCurrentVoiceChannel()) {
                return imFloater
            }
        }
        return null
    }

    private fun onVoiceStateIndicatorChanged(data: LLSD) {
        if (!data.asBoolean()) {
            activeVoiceFloater?.let { mTabContainer.setTabImage(it, "") }
            activeVoiceFloater = null
            currentVoiceState = VoiceState.NONE
        }
    }

    fun addFlashingSession(sessionId: UUID) {
        if (!flashingSessions.contains(sessionId)) {
            flashingSessions.add(sessionId)
        }
        checkFlashing()
    }

    private fun checkFlashing() {
        gToolBarView.flashCommand(LLCommandId("chat"), flashingSessions.isNotEmpty(), isMinimized())
    }

    override fun tabOpen(openedFloater: LLFloater, fromClick: Boolean) {
        LLEmojiHelper.instance().hideHelper(null, true)
        flashingTabStates.remove(openedFloater)
    }

    fun startFlashingTab(floater: LLFloater, message: String) {
        val containsMention = LLUrlRegistry.getInstance().containsAgentMention(message)

        val isAltFlashing = flashingTabStates.getOrPut(floater) { false } || containsMention
        flashingTabStates[floater] = isAltFlashing

        if (isFloaterFlashing(floater)) {
            setFloaterFlashing(floater, false)
        }
        setFloaterFlashing(floater, true, isAltFlashing)
    }

    fun saveOpenIMs() {
        if (!gSavedSettings.getBOOL("FSRestoreOpenIMs")) {
            gSavedPerAccountSettings.setLLSD("FSLastOpenIMs", LLSD.emptyArray())
            return
        }

        val openIMs = LLSD.emptyArray()
        for (i in 0 until mTabContainer.getTabCount()) {
            val floater = mTabContainer.getPanelByIndex(i) as? FSFloaterIM ?: continue
            val sessionId = floater.getKey()
            if (sessionId == UUID(0, 0)) continue
            val session = LLIMModel.getInstance().findIMSession(sessionId) ?: continue
            if (session.sessionType == LLIMModel.LLIMSession.P2P_SESSION) {
                val sessionData = LLSD.emptyMap()
                sessionData["other_participant_id"] = session.otherParticipantId.toString()
                sessionData["session_name"] = session.name
                openIMs.append(sessionData)
            }
        }

        gSavedPerAccountSettings.setLLSD("FSLastOpenIMs", openIMs)
    }

    fun restoreOpenIMs() {
        val openIMs = gSavedPerAccountSettings.getLLSD("FSLastOpenIMs")
        if (!openIMs.isArray() || openIMs.size() == 0) return

        for (sessionData in openIMs.asArray()) {
            if (!sessionData.isMap()) continue
            val otherParticipantId = sessionData["other_participant_id"].asUUID()
            val sessionName = sessionData["session_name"].asString()
            if (otherParticipantId == UUID(0, 0)) continue

            val newSessionId = LLIMMgr.getInstance()
                .addSession(sessionName, EInstantMessage.IM_NOTHING_SPECIAL, otherParticipantId)
            if (newSessionId != UUID(0, 0)) {
                val imFloater = FSFloaterIM.show(newSessionId)
                if (imFloater != null && imFloater.getHost() != this) {
                    addFloater(imFloater, false, EInstantMessage.IM_NOTHING_SPECIAL)
                }
            }
        }
    }

    protected fun onIMTabRearrange(tabIndex: Int, tabPanel: LLPanel) {
        val imFloater = tabPanel as? LLFloater ?: return
        val sessionId = imFloater.getKey()
        if (sessionId == UUID(0, 0)) return

        val chicletPanel = LLChicletBar.instance().getChicletPanel()
        val imChiclet = chicletPanel.findChiclet<LLChiclet>(sessionId) ?: return
        chicletPanel.setChicletIndex(imChiclet, tabIndex - mTabContainer.getNumLockedTabs())
    }

    companion object {
        fun findInstance(): FSFloaterIMContainer? =
            LLFloaterReg.findTypedInstance("fs_im_container")

        fun getInstance(): FSFloaterIMContainer? =
            LLFloaterReg.getTypedInstance("fs_im_container")

        fun reloadEmptyFloaters() {
            val instList = LLFloaterReg.getFloaterList("fs_impanel")
            for (inst in instList) {
                val floater = inst as? FSFloaterIM ?: continue
                if (floater.getLastChatMessageIndex() == -1) {
                    floater.reloadMessages(true)
                }
            }

            val nearbyChat = LLFloaterReg.findTypedInstance<FSFloaterNearbyChat>("fs_nearby_chat")
            if (nearbyChat != null && nearbyChat.getMessageArchiveLength() == 0) {
                nearbyChat.reloadMessages(true)
            }
        }
    }
}
