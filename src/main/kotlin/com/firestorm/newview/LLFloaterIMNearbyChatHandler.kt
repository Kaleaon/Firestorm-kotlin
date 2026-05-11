package com.firestorm.newview

import java.util.UUID

class LLFloaterIMNearbyChatScreenChannel(params: Params) : LLScreenChannelBase(params) {

    private val mActiveToasts: MutableList<LLHandle<LLToast>> = mutableListOf()
    private val mToastPool: ArrayDeque<LLHandle<LLToast>> = ArrayDeque()
    private var mStopProcessing: Boolean = false
    private var mCreateToastPanelCallback: (() -> LLFloaterIMNearbyChatToastPanel?)? = null

    private var mWorldViewRectConnection: (() -> Unit)? = null

    init {
        mWorldViewRectConnection = gViewerWindow.setOnWorldViewRectUpdated { old, new -> updateSize(old, new) }

        gSavedSettings.getControl("NearbyToastLifeTime")?.getSignal()?.connect { updateToastsLifetime() }
        gSavedSettings.getControl("NearbyToastFadingTime")?.getSignal()?.connect { updateToastFadingTime() }
        gSavedSettings.getControl("FSShowOnscreenConsole")?.getSignal()?.connect { removeToastsFromChannel() }
    }

    fun setCreatePanelCallback(cb: () -> LLFloaterIMNearbyChatToastPanel?) {
        mCreateToastPanelCallback = cb
    }

    override fun removeToastsFromChannel() {
        for (handle in mActiveToasts) {
            addToToastPool(handle.get())
        }
        mActiveToasts.clear()
    }

    override fun deleteAllChildren() {
        mToastPool.clear()
        mActiveToasts.clear()
        super.deleteAllChildren()
    }

    fun redrawToasts() { arrangeToasts() }

    fun onToastDestroyed(toast: LLToast?, appQuitting: Boolean) {
        if (appQuitting) {
            mStopProcessing = true
        } else {
            deactivateToast(toast)
        }
    }

    fun onToastFade(toast: LLToast?) {
        toast ?: return
        deactivateToast(toast)
        addToToastPool(toast)
        arrangeToasts()
    }

    fun addChat(chat: MutableMap<String, Any>) {
        if (mStopProcessing) return

        if (mFloaterSnapRegion == null) {
            mFloaterSnapRegion = gViewerWindow.getFloaterSnapRegion()
        }
        val channelRect = LLRect()
        mFloaterSnapRegion!!.localRectToOtherView(mFloaterSnapRegion!!.getLocalRect(), channelRect, gFloaterView)
        val availableHeight = channelRect.getHeight() - channelRect.mBottom - gSavedSettings.getS32("ToastGap") - 110
        chat["available_height"] = availableHeight

        if (mActiveToasts.isNotEmpty()) {
            val fromId = UUID.fromString(chat["from_id"].toString())
            val from = chat["from"]?.toString() ?: ""
            val toast = mActiveToasts[0].get()
            if (toast != null) {
                val panel = toast.getPanel() as? LLFloaterIMNearbyChatToastPanel
                if (panel != null && panel.messageID() == fromId && panel.getFromName() == from && panel.canAddText()) {
                    panel.addMessage(chat)
                    reshapePanel(panel)
                    toast.reshapeToPanel()
                    toast.startTimer()
                    arrangeToasts()
                    return
                }
            }
        }

        if (mToastPool.isEmpty()) {
            if (!createPoolToast()) return
            addChat(chat)
            return
        }

        val chatType = (chat["chat_type"] as? Int) ?: 0
        if (chatType == CHAT_TYPE_DEBUG_MSG) {
            if (!gSavedSettings.getBOOL("ShowScriptErrors")) return
            if (gSavedSettings.getS32("ShowScriptErrorsLocation") == 1) return
        }

        val toast = mToastPool.removeLast().get() ?: return
        val panel = toast.getPanel() as? LLFloaterIMNearbyChatToastPanel ?: return
        panel.init(chat)
        reshapePanel(panel)
        toast.reshapeToPanel()
        toast.startTimer()
        mActiveToasts.add(toast.getHandle())
        arrangeToasts()
    }

    fun arrangeToasts() {
        if (mStopProcessing || isHovering()) return

        if (mFloaterSnapRegion == null) {
            mFloaterSnapRegion = gViewerWindow.getFloaterSnapRegion()
        }

        if (getParent() == null) {
            mFloaterSnapRegion!!.addChild(this)
            setFollows(FOLLOWS_ALL)
        }

        updateRect()
        val channelRect = LLRect()
        mFloaterSnapRegion!!.localRectToOtherView(mFloaterSnapRegion!!.getLocalRect(), channelRect, gFloaterView)
        channelRect.mLeft += 10
        channelRect.mRight = channelRect.mLeft + 300

        var bottom = channelRect.mBottom + gSavedSettings.getS32("FSNearbyChatToastsOffset")
        val margin = gSavedSettings.getS32("ToastGap")

        mActiveToasts.sortWith { a, b ->
            val ta = a.get()?.getTimeLeftToLive() ?: 0f
            val tb = b.get()?.getTimeLeftToLive() ?: 0f
            tb.compareTo(ta)
        }

        val iter = mActiveToasts.iterator()
        while (iter.hasNext()) {
            val handle = iter.next()
            val toast = handle.get() ?: continue
            val toastTop = bottom + toast.getRect().getHeight() + margin
            if (toastTop > channelRect.getHeight()) {
                addToToastPool(handle.get())
                iter.remove()
                while (iter.hasNext()) {
                    val remaining = iter.next()
                    addToToastPool(remaining.get())
                    iter.remove()
                }
                break
            }
            val toastRect = toast.getRect()
            toastRect.setLeftTopAndSize(channelRect.mLeft, bottom + toastRect.getHeight(), toastRect.getWidth(), toastRect.getHeight())
            toast.setRect(toastRect)
            bottom += toastRect.getHeight() - toast.getTopPad() + margin
        }

        for (handle in mActiveToasts.reversed()) {
            val toast = handle.get() ?: continue
            toast.setIsHidden(false)
            toast.setVisible(true)
        }
    }

    private fun reshapePanel(panel: LLFloaterIMNearbyChatToastPanel) {
        val percentage = gSavedSettings.getS32("NearbyToastWidth")
        panel.reshape(gViewerWindow.getWindowWidthScaled() * percentage / 100, panel.getRect().getHeight(), true)
    }

    private fun updateSize(oldWorldRect: LLRect, newWorldRect: LLRect) {
        for (handle in mActiveToasts) {
            val toast = handle.get() ?: continue
            val panel = toast.getPanel() as? LLFloaterIMNearbyChatToastPanel ?: continue
            reshapePanel(panel)
            toast.reshapeToPanel()
        }
        arrangeToasts()
    }

    private fun deactivateToast(toast: LLToast?) {
        toast ?: return
        val iter = mActiveToasts.iterator()
        while (iter.hasNext()) {
            if (iter.next().get() === toast) {
                iter.remove()
                break
            }
        }
    }

    private fun addToToastPool(toast: LLToast?) {
        toast ?: return
        toast.setVisible(false)
        toast.stopTimer()
        toast.setIsHidden(true)
        toast.setLifetime(gSavedSettings.getS32("NearbyToastLifeTime"))
        toast.setFadingTime(gSavedSettings.getS32("NearbyToastFadingTime"))
        mToastPool.addLast(toast.getHandle())
    }

    private fun createPoolToast(): Boolean {
        val panel = mCreateToastPanelCallback?.invoke() ?: return false
        val p = LLToast.Params().apply {
            this.panel = panel
            lifetimeSecs = gSavedSettings.getS32("NearbyToastLifeTime").toFloat()
            fadingTimeSecs = gSavedSettings.getS32("NearbyToastFadingTime").toFloat()
        }
        val toast = LLFloaterIMNearbyChatToast(p, this)
        reshapePanel(panel)
        toast.setOnFadeCallback { onToastFade(it) }
        toast.setOnToastDestroyedCallback { t -> onToastDestroyed(t, false) }
        mToastPool.addLast(toast.getHandle())
        return true
    }

    private fun updateToastsLifetime() {
        val seconds = gSavedSettings.getS32("NearbyToastLifeTime")
        for (handle in mToastPool) {
            handle.get()?.setLifetime(seconds)
        }
    }

    private fun updateToastFadingTime() {
        val seconds = gSavedSettings.getS32("NearbyToastFadingTime")
        for (handle in mToastPool) {
            handle.get()?.setFadingTime(seconds)
        }
    }

    override fun createOverflowToast(bottom: Int, timer: Float) {
        // Overflow toasts are intentionally suppressed for nearby chat.
    }
}


class LLFloaterIMNearbyChatToast(params: LLToast.Params, private val mNearbyChatScreenChannelp: LLFloaterIMNearbyChatScreenChannel) : LLToast(params) {

    override fun onClose(appQuitting: Boolean) {
        mNearbyChatScreenChannelp.onToastDestroyed(this, appQuitting)
    }
}


class LLFloaterIMNearbyChatHandler : LLChatHandler() {

    val mNewChatSignal: MutableList<(Map<String, Any>) -> Unit> = mutableListOf()

    init {
        val p = LLFloaterIMNearbyChatScreenChannel.Params().apply {
            id = NEARBY_CHAT_CHANNEL_UUID
        }
        val channel = LLFloaterIMNearbyChatScreenChannel(p)
        channel.setCreatePanelCallback { createToastPanel() }
        LLChannelManager.getInstance().addChannel(channel)
        mChannel = channel.getHandle()
    }

    fun addNewChatCallback(cb: (Map<String, Any>) -> Unit): (() -> Unit) {
        mNewChatSignal.add(cb)
        return { mNewChatSignal.remove(cb) }
    }

    override fun initChannel() {
        // Channel position is managed dynamically via the snap region.
    }

    override fun processChat(chatMsg: LLChat, args: Map<String, Any>) {
        if (chatMsg.mMuted) {
            val nearbyChat = LLFloaterReg.getTypedInstance<FSFloaterNearbyChat>("fs_nearby_chat")
            nearbyChat?.addMessage(chatMsg, true, args)
            return
        }

        if (chatMsg.mText.isEmpty()) return

        val tmpChat = chatMsg.copy()

        val nearbyChat = LLFloaterReg.getTypedInstance<FSFloaterNearbyChat>("fs_nearby_chat")

        val chat = mutableMapOf<String, Any>()
        chat["message"] = chatMsg.mText
        chat["from"] = chatMsg.mFromName
        chat["from_id"] = chatMsg.mFromID
        chat["time"] = chatMsg.mTime
        chat["source"] = chatMsg.mSourceType.ordinal
        chat["chat_type"] = chatMsg.mChatType.ordinal
        chat["chat_style"] = chatMsg.mChatStyle.ordinal
        if (chatMsg.mSourceType != CHAT_SOURCE_AGENT || !chatMsg.mRlvNamesFiltered) {
            chat["sender_slurl"] = LLViewerChat.getSenderSLURL(chatMsg, args)
        }

        if (chatMsg.mChatType == CHAT_TYPE_DIRECT &&
            chatMsg.mText.isNotEmpty() && chatMsg.mText[0] == '@'
        ) {
            sChatWatcher.post(chat)
            return
        }

        for (cb in mNewChatSignal) cb(chat)

        val fsOwnerSayToScriptDebug = gSavedPerAccountSettings.getBOOL("FSllOwnerSayToScriptDebugWindow")
        if (chatMsg.mChatType == CHAT_TYPE_DEBUG_MSG || (chatMsg.mChatType == CHAT_TYPE_OWNER && fsOwnerSayToScriptDebug)) {
            if (LLFloater.isQuitRequested()) return
            if (!gSavedSettings.getBOOL("ShowScriptErrors") && chatMsg.mChatType == CHAT_TYPE_DEBUG_MSG) return
            if (gAgentID != chatMsg.mOwnerID) return

            LLFloaterScriptDebug.addScriptLine(chatMsg)
            if (gSavedSettings.getS32("ShowScriptErrorsLocation") == 1 || chatMsg.mChatType == CHAT_TYPE_OWNER) {
                return
            }
        }

        val primfeedOauth = "#PRIMFEED_OAUTH: "
        if (chatMsg.mText.startsWith(primfeedOauth) &&
            chatMsg.mChatType == CHAT_TYPE_IM && chatMsg.mSourceType == CHAT_SOURCE_OBJECT
        ) {
            return
        }

        nearbyChat?.addMessage(chatMsg, true, args)

        if (chatMsg.mSourceType == CHAT_SOURCE_AGENT &&
            chatMsg.mFromID != UUID(0, 0) && chatMsg.mFromID != gAgentID
        ) {
            LLFirstUse.otherAvatarChatFirst()
            LLRecentPeople.instance().add(chatMsg.mFromID)
        }

        sChatWatcher.post(chat)

        if (FSConsoleUtils.processChatMessage(chatMsg, args)) return

        if (!gSavedSettings.getBOOL("FSShowOnscreenConsole")) return

        val useChatBubbles = gSavedSettings.getBOOL("UseChatBubbles")
        val fsBubblesHide = gSavedSettings.getBOOL("FSBubblesHideConsoleAndToasts")
        if ((chatMsg.mSourceType == CHAT_SOURCE_AGENT && useChatBubbles && fsBubblesHide) ||
            mChannel.isDead() || !mChannel.get()!!.getShowToasts()
        ) return

        if (!mChannel.get()!!.getVisible()) {
            initChannel()
        }

        val channel = mChannel.get() as? LLFloaterIMNearbyChatScreenChannel ?: return

        if (nearbyChat?.getVisible() == true) return

        if (gSavedSettings.getS32("NearbyToastLifeTime") > 0 || gSavedSettings.getS32("NearbyToastFadingTime") > 0) {
            val id = UUID.randomUUID()
            chat["id"] = id

            val toastMsg = if (tmpChat.mChatStyle == CHAT_STYLE_IRC) {
                (if (tmpChat.mFromName.isNotEmpty()) tmpChat.mFromName else "") + tmpChat.mText.substring(3)
            } else {
                tmpChat.mText
            }

            val rColorName = "White"
            val rColorAlpha = 1.0f
            LLViewerChat.getChatColor(chatMsg, rColorName, rColorAlpha)

            chat["text_color"] = rColorName
            chat["color_alpha"] = rColorAlpha
            chat["font_size"] = LLViewerChat.getChatFontSize()
            chat["message"] = toastMsg
            channel.addChat(chat)
        }
    }

    companion object {
        val sChatWatcher: LLEventStream = LLEventStream("LLChat")

        private fun createToastPanel(): LLFloaterIMNearbyChatToastPanel? =
            LLFloaterIMNearbyChatToastPanel.createInstance()
    }
}
