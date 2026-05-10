package com.firestorm.newview

import com.firestorm.ui.UICtrl
import com.firestorm.ui.Panel
import com.firestorm.ui.TextBox
import com.firestorm.ui.TextEditor
import com.firestorm.ui.LayoutStack
import com.firestorm.ui.LayoutPanel
import com.firestorm.ui.AvatarIconCtrl
import com.firestorm.ui.UIColor
import com.firestorm.ui.UIColorTable
import com.firestorm.ui.StyleParams
import com.firestorm.ui.InlineViewSegment
import com.firestorm.ui.MenuGL
import com.firestorm.ui.View
import com.firestorm.ui.Rect
import com.firestorm.chat.Chat
import com.firestorm.chat.ChatSourceType
import com.firestorm.chat.ChatStyle
import com.firestorm.chat.ChatType
import com.firestorm.chat.ViewerChat
import com.firestorm.avatar.AvatarActions
import com.firestorm.avatar.AvatarNameCache
import com.firestorm.avatar.AvatarName
import com.firestorm.agent.Agent
import com.firestorm.llsd.LLSD
import com.firestorm.types.LLUUID
import com.firestorm.types.LLDate
import com.firestorm.types.Color4
import com.firestorm.floater.FloaterReg
import com.firestorm.mute.MuteList
import com.firestorm.network.IMModel
import com.firestorm.speaker.SpeakerMgr
import com.firestorm.speaker.Speaker
import com.firestorm.group.GroupMgr
import com.firestorm.notifications.NotificationsUtil
import com.firestorm.notifications.ToastNotifyPanel

// ────────────────────────────────────────────────────────────────────────────
// ChatHistoryHeader — local panel rendered per-message in expanded chat mode
// ────────────────────────────────────────────────────────────────────────────

open class ChatHistoryHeader : Panel() {

    companion object {
        const val PADDING = 20

        fun createInstance(fileName: String): ChatHistoryHeader {
            val instance = ChatHistoryHeader()
            instance.buildFromFile(fileName)
            return instance
        }

        fun onClickInfoCtrl(infoCtrl: UICtrl?) {
            infoCtrl ?: return
            val header = infoCtrl.parent as? ChatHistoryHeader ?: return
            header.showInspector()
        }
    }

    var avatarId: LLUUID = LLUUID.null_
        private set
    private var sessionId: LLUUID = LLUUID.null_
    var sourceType: ChatSourceType = ChatSourceType.UNKNOWN
        private set
    private var from: String = ""
    private var text: String = ""
    private var time: Double = 0.0
    private val creationTime: Long = System.currentTimeMillis() / 1000L
    private var showContextMenu: Boolean = true
    private var showInfoCtrl: Boolean = true
    private var objectData: LLSD = LLSD()
    private var minUserNameWidth: Int = 0
    private var needsTimeBox: Boolean = true

    private var infoCtrl: UICtrl? = null
    private var userNameTextBox: TextBox? = null
    private var timeBoxTextBox: TextBox? = null
    private var avatarNameCacheConnection: AutoCloseable? = null
    private var popupMenuHandleAvatar: MenuGL? = null
    private var popupMenuHandleObject: MenuGL? = null

    override fun postBuild(): Boolean {
        setDoubleClickCallback { showInspector() }
        setMouseEnterCallback { showInfoCtrl() }
        setMouseLeaveCallback { hideInfoCtrl() }

        userNameTextBox = getChild("user_name")
        timeBoxTextBox = getChild("time_box")

        infoCtrl = TODO("GPU: load inspector_info_ctrl.xml widget")
        infoCtrl?.setCommitCallback { onClickInfoCtrl(infoCtrl) }
        infoCtrl?.setVisible(false)

        return super.postBuild()
    }

    fun setup(chat: Chat, styleParams: StyleParams, args: LLSD) {
        avatarId = chat.fromId
        sessionId = chat.sessionId
        sourceType = chat.sourceType
        text = chat.text
        time = chat.time

        if ((chat.fromId.isNull && chat.fromName.isEmpty()) ||
            (chat.fromName == "Second Life" && chat.fromId.isNull)) {
            sourceType = ChatSourceType.SYSTEM
        }

        val userNameBox = userNameTextBox ?: getChild<TextBox>("user_name").also { userNameTextBox = it }
        timeBoxTextBox = timeBoxTextBox ?: getChild("time_box")
        userNameBox.setReadOnlyColor(styleParams.readonlyColor)
        userNameBox.setColor(styleParams.color)

        when {
            sourceType == ChatSourceType.TELEPORT && chat.chatStyle == ChatStyle.TELEPORT_SEP -> {
                from = chat.fromName
                needsTimeBox = false
                userNameBox.setValue(from)
                updateMinUserNameWidth()
                timeBoxTextBox?.setVisible(false)
            }
            chat.fromName.isEmpty() || sourceType == ChatSourceType.SYSTEM -> {
                from = "Second Life"
                if (chat.fromName.isNotEmpty() && from != chat.fromName) {
                    from += " (${chat.fromName})"
                }
                userNameBox.setValue(from)
                updateMinUserNameWidth()
            }
            sourceType == ChatSourceType.AGENT && !avatarId.isNull && chat.chatStyle != ChatStyle.HISTORY -> {
                if (!chat.rlvNamesFiltered) {
                    userNameBox.setValue(LLSD())
                    fetchAvatarName()
                } else {
                    from = chat.fromName
                    userNameBox.setValue(from)
                    userNameBox.setToolTip(from)
                    setToolTip(from)
                    updateMinUserNameWidth()
                }
            }
            chat.chatStyle == ChatStyle.HISTORY || sourceType == ChatSourceType.AGENT -> {
                val usernameStart = chat.fromName.lastIndexOf(" (")
                val usernameEnd = chat.fromName.lastIndexOf(')')
                if (usernameStart != -1 && usernameEnd == chat.fromName.length - 1) {
                    from = chat.fromName.substring(0, usernameStart)
                    userNameBox.setValue(from)
                } else {
                    from = chat.fromName
                    userNameBox.setValue(from)
                    updateMinUserNameWidth()
                }
            }
            else -> {
                from = chat.fromName
                userNameBox.setValue(from)
                updateMinUserNameWidth()
            }
        }

        setTimeField(chat)

        val icon: AvatarIconCtrl = getChild("avatar_icon")
        if (sourceType != ChatSourceType.AGENT || avatarId.isNull) icon.setDrawTooltip(false)

        if (chat.rlvNamesFiltered &&
            (sourceType == ChatSourceType.AGENT || sourceType == ChatSourceType.OBJECT)) {
            showInfoCtrl = false
            showContextMenu = false
            icon.setDrawTooltip(false)
        }

        when (sourceType) {
            ChatSourceType.AGENT   -> icon.setValue(chat.fromId)
            ChatSourceType.OBJECT  -> icon.setValue(LLSD("OBJECT_Icon"))
            ChatSourceType.SYSTEM,
            ChatSourceType.REGION  -> icon.setValue(LLSD("SL_Logo"))
            ChatSourceType.TELEPORT -> icon.setValue(LLSD("Command_Destinations_Icon"))
            else                   -> icon.setValue(LLSD("Unknown_Icon"))
        }

        if (chat.sourceType == ChatSourceType.OBJECT) {
            var slurl = args["slurl"].asString()
            if (slurl.isEmpty()) {
                slurl = TODO("APR: use JVM equivalent — resolve region SLURL from agent position")
            }
            objectData = LLSD()
            objectData["object_id"] = chat.fromId
            objectData["name"] = chat.fromName
            objectData["owner_id"] = chat.ownerId
            objectData["slurl"] = slurl
        }
    }

    override fun draw() {
        val userNameBox = userNameTextBox ?: return
        val timeBox = timeBoxTextBox ?: return

        val userNameWidth = userNameBox.rect.width
        val timeBoxWidth = timeBox.rect.width

        if (needsTimeBox && !timeBox.isVisible && userNameWidth > minUserNameWidth) {
            val newWidth = userNameWidth - timeBoxWidth
            userNameBox.reshape(newWidth, userNameBox.rect.height)
            timeBox.setVisible(true)
        }

        super.draw()
    }

    private fun updateMinUserNameWidth() {
        val box = userNameTextBox ?: return
        minUserNameWidth = box.getTextWidth() + PADDING
    }

    private fun setTimeField(chat: Chat) {
        val timeBox: TextBox = getChild("time_box")
        val widthBefore = timeBox.rect.width
        timeBox.setValue(chat.timeStr)
        timeBox.reshapeToFitText()
        val deltaX = widthBefore - timeBox.rect.width
        timeBox.translate(deltaX, 0)
        val userNameView: View = getChild("user_name")
        userNameView.reshape(userNameView.rect.width + deltaX, userNameView.rect.height)
    }

    private fun fetchAvatarName() {
        if (avatarId.isNull) return
        avatarNameCacheConnection?.close()
        avatarNameCacheConnection = AvatarNameCache.get(avatarId) { id, avName -> onAvatarNameCache(id, avName) }
    }

    private fun onAvatarNameCache(agentId: LLUUID, avName: AvatarName) {
        avatarNameCacheConnection = null
        from = avName.displayName
        val userNameBox: TextBox = getChild("user_name")
        userNameBox.setValue(LLSD(avName.displayName))
        userNameBox.setToolTip(avName.userName)
        setToolTip(avName.userName)
        updateMinUserNameWidth()
    }

    fun showInspector() {
        if (!showInfoCtrl || (avatarId.isNull &&
                sourceType != ChatSourceType.SYSTEM &&
                sourceType != ChatSourceType.REGION)) return
        when (sourceType) {
            ChatSourceType.OBJECT -> FloaterReg.showInstance("inspect_remote_object", objectData)
            ChatSourceType.AGENT  -> FloaterReg.showInstance("inspect_avatar", LLSD().with("avatar_id", avatarId))
            else -> {}
        }
    }

    private fun showInfoCtrl() {
        val isVisible = showInfoCtrl && !avatarId.isNull && from.isNotEmpty() &&
                sourceType != ChatSourceType.SYSTEM && sourceType != ChatSourceType.REGION
        infoCtrl?.setVisible(isVisible)
    }

    private fun hideInfoCtrl() {
        infoCtrl?.setVisible(false)
    }

    private fun showContextMenu(x: Int, y: Int) {
        if (!showContextMenu) return
        when {
            sourceType == ChatSourceType.AGENT  && !avatarId.isNull  -> showAvatarContextMenu(x, y)
            sourceType == ChatSourceType.OBJECT && !avatarId.isNull  -> showObjectContextMenu(x, y)
            else -> {}
        }
    }

    private fun showAvatarContextMenu(x: Int, y: Int) {
        TODO("GPU: show avatar context popup menu at ($x,$y)")
    }

    private fun showObjectContextMenu(x: Int, y: Int) {
        TODO("GPU: show object context popup menu at ($x,$y)")
    }

    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (pointInChild("avatar_icon", x, y) || pointInChild("user_name", x, y)) {
            showContextMenu(x, y)
            return true
        }
        return super.handleRightMouseDown(x, y, mask)
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean = super.handleMouseUp(x, y, mask)

    fun onObjectIconContextMenuItemClicked(userdata: LLSD) {
        when (userdata.asString()) {
            "profile"  -> FloaterReg.showInstance("inspect_remote_object", objectData)
            "block"    -> MuteList.instance.add(avatarId, from)
            "unblock"  -> MuteList.instance.remove(avatarId, from)
            "map"      -> TODO("APR: use JVM equivalent — open map to slurl")
            "teleport" -> TODO("APR: use JVM equivalent — teleport to slurl")
        }
    }

    fun onAvatarIconContextMenuItemClicked(userdata: LLSD) {
        when (userdata.asString()) {
            "profile"          -> AvatarActions.showProfile(avatarId)
            "im"               -> AvatarActions.startIM(avatarId)
            "teleport"         -> AvatarActions.offerTeleport(avatarId)
            "request_teleport" -> AvatarActions.teleportRequest(avatarId)
            "voice_call"       -> AvatarActions.startCall(avatarId)
            "chat_history"     -> AvatarActions.viewChatHistory(avatarId)
            "add"              -> AvatarActions.requestFriendshipDialog(avatarId, from)
            "remove"           -> AvatarActions.removeFriendDialog(avatarId)
            "invite_to_group"  -> AvatarActions.inviteToGroup(avatarId)
            "zoom_in"          -> TODO("APR: use JVM equivalent — zoom camera to object")
            "map"              -> AvatarActions.showOnMap(avatarId)
            "share"            -> AvatarActions.share(avatarId)
            "pay"              -> AvatarActions.pay(avatarId)
            "block_unblock"    -> AvatarActions.toggleMute(avatarId, MuteList.FLAG_VOICE_CHAT)
            "mute_unmute"      -> AvatarActions.toggleMute(avatarId, MuteList.FLAG_TEXT_CHAT)
            "ban_member"       -> banGroupMember(avatarId)
        }
    }

    private fun isGroupModerator(): Boolean {
        val speakerMgr = IMModel.instance.getSpeakerManager(sessionId) ?: return false
        if (!Agent.instance.isInGroup(sessionId)) return false
        val speaker = speakerMgr.findSpeaker(Agent.instance.id) ?: return false
        return speaker.isModerator
    }

    private fun banGroupMember(participantId: LLUUID) {
        val groupData = GroupMgr.instance.getGroupData(sessionId) ?: return
        groupData.banMemberById(participantId)
    }

    private fun canBanInGroup(): Boolean {
        val groupData = GroupMgr.instance.getGroupData(sessionId) ?: return false
        return Agent.instance.hasPowerInGroup(sessionId, GroupMgr.GP_ROLE_REMOVE_MEMBER) &&
                Agent.instance.hasPowerInGroup(sessionId, GroupMgr.GP_GROUP_BAN_ACCESS)
    }
}

// ────────────────────────────────────────────────────────────────────────────
// ChatHistory — scrolling chat log widget
// ────────────────────────────────────────────────────────────────────────────

class ChatHistory(
    messageHeaderFilename: String = "",
    messageSeparatorFilename: String = "",
    val leftTextPad: Int = 0,
    val rightTextPad: Int = 0,
    val leftWidgetPad: Int = 0,
    val rightWidgetPad: Int = 0,
    val topSeparatorPad: Int = 0,
    val bottomSeparatorPad: Int = 0,
    val topHeaderPad: Int = 0,
    val bottomHeaderPad: Int = 0,
    val notifyAboutUnreadMsg: Boolean = true
) : UICtrl() {

    private var mMessageHeaderFilename: String = messageHeaderFilename
    private var mMessageSeparatorFilename: String = messageSeparatorFilename

    private var lastFromName: String = ""
    private var lastFromId: LLUUID = LLUUID.null_
    private var lastMessageTime: LLDate = LLDate.null_
    private var isLastMessageFromLog: Boolean = false

    private lateinit var editor: TextEditor
    private var moreChatPanel: LayoutPanel? = null
    private var moreChatText: TextBox? = null
    private val unreadChatSources: MutableSet<String> = mutableSetOf()

    fun getValue(): LLSD = LLSD(editor.getText())

    fun initFromParams() {
        val scrollbarSize = 0  // UIScrollbarSize
        val stackRect = getLocalRect().also { it.right -= scrollbarSize }

        val stack = LayoutStack(orientation = LayoutStack.VERTICAL, rect = stackRect)

        stack.addPanel(LayoutPanel(name = "spacer", autoResize = true, minDim = 30))

        val newTextNoticeHeight = 20
        val morePanelRect = getLocalRect().also { it.top = it.bottom + newTextNoticeHeight }
        moreChatPanel = LayoutPanel(name = "new_text_notice_holder", rect = morePanelRect,
            backgroundOpaque = true, visible = false, autoResize = false, minDim = 0)

        moreChatText = TextBox(rect = moreChatPanel!!.getLocalRect(), name = "more_chat_text")
        moreChatText!!.setClickedCallback { onClickMoreText() }
        moreChatPanel!!.addChild(moreChatText!!)

        stack.addPanel(moreChatPanel!!)
        addChild(stack)
    }

    private fun getSeparator(): View? {
        return TODO("GPU: build separator view from $mMessageSeparatorFilename")
    }

    private fun getHeader(chat: Chat, styleParams: StyleParams, args: LLSD): View? {
        val header = ChatHistoryHeader.createInstance(mMessageHeaderFilename)
        header.setup(chat, styleParams, args)
        return header
    }

    private fun onClickMoreText() {
        editor.endOfDoc()
    }

    fun clear() {
        lastFromName = ""
        lastFromId = LLUUID.null_
        editor.clear()
    }

    override fun draw() {
        if (editor.scrolledToEnd()) {
            unreadChatSources.clear()
            moreChatPanel?.setVisible(false)
        }
        super.draw()
    }

    fun appendMessage(chat: Chat, args: LLSD = LLSD(), inputAppendParams: StyleParams = StyleParams()) {
        val usePlainText = args["use_plain_text_chat_history"].asBoolean()
        val fromMe = chat.fromId == Agent.instance.id

        editor.setPlainText(usePlainText)

        if (notifyAboutUnreadMsg && !editor.scrolledToEnd() && !fromMe && chat.fromName.isNotEmpty()) {
            unreadChatSources.add(chat.fromName)
            moreChatPanel?.setVisible(true)
            val chatters = unreadChatSources.joinToString(", ")
            val key = if (unreadChatSources.size == 1) "unread_chat_single" else "unread_chat_multiple"
            moreChatText?.setValue(LLSD(key.replace("SOURCES", chatters)))
        }

        val txtColor: UIColor = UIColorTable.instance.getColor("White")
        var alpha = 1f
        ViewerChat.getChatColor(chat, txtColor, alpha)

        val fontName = ViewerChat.getChatFontName()
        val fontSize = ViewerChat.getChatFontSize()

        val bodyParams = StyleParams(
            color = txtColor, readonlyColor = txtColor, alpha = alpha,
            fontName = fontName, fontSize = fontSize, fontStyle = inputAppendParams.fontStyle
        )

        val nameParams = bodyParams.copy(color = txtColor, readonlyColor = txtColor)

        val prefix = if (chat.text.length >= 4) chat.text.substring(0, 4) else ""
        val ircMe = prefix == "/me " || prefix == "/me'"

        var delimiter = ": "
        if (chat.chatType == ChatType.SHOUT || chat.chatType == ChatType.WHISPER) delimiter = " "
        if (ircMe || chat.chatStyle == ChatStyle.IRC) {
            delimiter = ""
            bodyParams.fontStyle = "ITALIC"
        }
        if (chat.chatType == ChatType.WHISPER) bodyParams.fontStyle = "ITALIC"
        else if (chat.chatType == ChatType.SHOUT) bodyParams.fontStyle = "BOLD"

        val messageFromLog = chat.chatStyle == ChatStyle.HISTORY
        val teleportSeparator = chat.sourceType == ChatSourceType.TELEPORT

        if (messageFromLog) {
            bodyParams.color = UIColor.grey
            bodyParams.readonlyColor = UIColor.grey
            nameParams.color = UIColor.grey
            nameParams.readonlyColor = UIColor.grey
        }

        var prependNewLine = editor.getText().isNotEmpty()

        if (usePlainText) {
            val squareBrackets = chat.sourceType == ChatSourceType.SYSTEM

            if (args["show_time"].asBoolean() && !teleportSeparator) {
                val tsColor = UIColorTable.instance.getColor("ChatTimestampColor")
                val tsParams = if (!messageFromLog) bodyParams.copy(color = tsColor, readonlyColor = tsColor) else bodyParams
                editor.appendText("[${chat.timeStr}] ", prependNewLine, tsParams)
                prependNewLine = false
            }

            if (squareBrackets) {
                editor.appendText("[", prependNewLine, bodyParams)
                prependNewLine = false
            }

            if (args["show_names_for_p2p_conv"].asBoolean() && chat.fromName.trim().isNotEmpty()) {
                when {
                    chat.sourceType == ChatSourceType.OBJECT && !chat.fromId.isNull -> {
                        val url = chat.url.ifEmpty { ViewerChat.getSenderSLURL(chat, args) }
                        val linkParams = bodyParams.copy(isLink = true, linkHref = url,
                            color = UIColorTable.instance.getColor("HTMLLinkColor"))
                        editor.appendText("${chat.fromName}$delimiter", prependNewLine, linkParams)
                        prependNewLine = false
                    }
                    chat.fromName != "Second Life" && !chat.fromId.isNull && !messageFromLog &&
                            chat.sourceType != ChatSourceType.REGION && !chat.rlvNamesFiltered -> {
                        val linkParams = bodyParams.copy()
                        editor.appendText("${linkParams.linkHref}$delimiter", prependNewLine, linkParams)
                        prependNewLine = false
                    }
                    teleportSeparator -> {
                        editor.appendText("Teleport to <nolink>${chat.fromName}</nolink>", prependNewLine, bodyParams)
                        prependNewLine = false
                    }
                    else -> {
                        editor.appendText("<nolink>${chat.fromName}</nolink>$delimiter", prependNewLine, bodyParams)
                        prependNewLine = false
                    }
                }
            }
        } else {
            prependNewLine = false
            val segParams = InlineViewSegment.Params(forceNewline = true, leftPad = leftWidgetPad, rightPad = rightWidgetPad)
            val newMessageTime = LLDate.now()

            val view: View?
            if (!teleportSeparator &&
                lastFromName == chat.fromName &&
                lastFromId == chat.fromId &&
                !lastMessageTime.isNull &&
                (newMessageTime.secondsSinceEpoch - lastMessageTime.secondsSinceEpoch) < 60.0 &&
                isLastMessageFromLog == messageFromLog) {
                view = getSeparator()
                if (view == null) return
                segParams.topPad = topSeparatorPad
                segParams.bottomPad = bottomSeparatorPad
            } else {
                view = getHeader(chat, nameParams, args)
                if (view == null) return
                segParams.topPad = if (editor.length > 0) topHeaderPad else 0
                segParams.bottomPad = if (teleportSeparator) bottomSeparatorPad else bottomHeaderPad
            }

            segParams.view = view
            val docRect = editor.documentView.rect
            val targetLeft = docRect.left + leftWidgetPad + editor.hPad
            val targetRight = docRect.right - rightWidgetPad
            view.reshape(targetRight - targetLeft, view.rect.height)
            view.setOrigin(targetLeft, view.rect.bottom)

            var widgetText = "\n[${chat.timeStr}] "
            if (chat.fromName.trim().isNotEmpty() && chat.fromName != "Second Life")
                widgetText += "${chat.fromName}$delimiter"

            editor.appendWidget(segParams, widgetText, false)
            lastFromName = chat.fromName
            lastFromId = chat.fromId
            lastMessageTime = newMessageTime
            isLastMessageFromLog = messageFromLog
        }

        // Notification inline widget
        if (!chat.notifId.isNull) {
            val notification = NotificationsUtil.find(chat.notifId)
            if (notification != null) {
                var createToast = true
                if (notification.name == "OfferFriendship") {
                    createToast = ToastNotifyPanel.instanceSnapshot().none { panel ->
                        panel.notificationName == "OfferFriendship" && panel.isControlPanelEnabled
                    }
                }
                if (createToast) {
                    val notifyBox = ToastNotifyPanel(notification, chat.sessionId, !usePlainText, editor)
                    val docRect = editor.documentView.rect
                    val targetLeft = docRect.left + leftWidgetPad + editor.hPad
                    val targetRight = docRect.right - rightWidgetPad
                    notifyBox.reshape(targetRight - targetLeft, notifyBox.rect.height)
                    notifyBox.setOrigin(targetLeft, notifyBox.rect.bottom)
                    val params = InlineViewSegment.Params(view = notifyBox, leftPad = leftWidgetPad, rightPad = rightWidgetPad)
                    editor.appendWidget(params, "\n", false)
                }
            }
        } else if (!teleportSeparator) {
            var message = if (ircMe) chat.text.substring(3) else chat.text

            if (usePlainText && !fromMe && !chat.fromId.isNull) {
                val slurl = "secondlife:///app/agent/${chat.fromId.asString()}/about"
                if (message.length > slurl.length && message.startsWith(slurl)) {
                    message = message.substring(slurl.length, message.length - 1)
                }
            }

            if (ircMe && !usePlainText) {
                val avName = AvatarNameCache.getCached(chat.fromId)
                val fromName = if (avName != null && !avName.isDisplayNameDefault) avName.completeName else chat.fromName
                message = fromName + message
            }

            if (chat.sourceType == ChatSourceType.SYSTEM) message += "]"

            editor.appendText(message, prependNewLine, bodyParams)
        }

        editor.blockUndo()
        if (fromMe) editor.setCursorAndScrollToEnd()
    }
}
