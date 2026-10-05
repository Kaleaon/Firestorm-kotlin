package com.firestorm.newview

import com.firestorm.llui.MultiFloater
import com.firestorm.llui.Floater
import com.firestorm.llui.FloaterReg
import com.firestorm.llui.TextEditor
import com.firestorm.llui.LLSD
import com.firestorm.llui.UIColorTable
import com.firestorm.llui.Style
import com.firestorm.llui.Trans
import com.firestorm.llui.FontGL
import com.firestorm.newview.ViewerObjectList
import com.firestorm.newview.ViewerChat
import com.firestorm.newview.Chat
import java.util.UUID

class FloaterScriptDebug(key: LLSD) : MultiFloater(key) {

    init {
        mAutoResize = false
        setAutoFocus(false)
    }

    override fun postBuild(): Boolean {
        super.postBuild()
        mCloseSignal.disconnectAllSlots()
        return mTabContainer != null
    }

    override fun onClickCloseBtn(appQuitting: Boolean) {
        if (SavedSettings.getBool("FSScriptDebugWindowClearOnClose")) {
            FloaterReg.findTypedInstance<FloaterScriptDebugOutput>(
                "script_debug_output", LLSD(UUID.fromString("00000000-0000-0000-0000-000000000000"))
            )?.clear()
        }
        super.onClickCloseBtn(appQuitting)
    }

    companion object {
        var sInstance: FloaterScriptDebug? = null

        fun show(objectId: UUID) {
            addOutputWindow(objectId, show = true)
        }

        fun addScriptLine(chat: Chat) {
            val objectp = ViewerObjectList.findObject(chat.fromID)
            val floaterLabel: String

            if (objectp != null) {
                if (chat.chatType == Chat.CHAT_TYPE_DEBUG_MSG) {
                    if (objectp.isHUDAttachment()) {
                        if (isAgentAvatarValid()) {
                            gAgentAvatarp.setIcon(
                                ViewerTextureManager.getFetchedTextureFromFile(
                                    "script_error.j2c", FTT_LOCAL_FILE, true, GLTexture.BOOST_UI
                                )
                            )
                            gAgentAvatarp.getIcon()?.setScriptError()
                        }
                    } else {
                        objectp.setIcon(
                            ViewerTextureManager.getFetchedTextureFromFile(
                                "script_error.j2c", FTT_LOCAL_FILE, true, GLTexture.BOOST_UI
                            )
                        )
                        objectp.getIcon()?.setScriptError()
                    }
                }
                val pos = objectp.getPositionRegion()
                floaterLabel = "${chat.fromName} (%.0f, %.0f, %.0f)".format(pos.x, pos.y, pos.z)
            } else {
                floaterLabel = chat.fromName
            }

            addOutputWindow(UUID.fromString("00000000-0000-0000-0000-000000000000"))

            val ownerSayRouting = SavedSettings.getUInt("FSllOwnerSayToScriptDebugWindowRouting")

            if (chat.chatType == Chat.CHAT_TYPE_DEBUG_MSG || ownerSayRouting != 1u) {
                FloaterReg.findTypedInstance<FloaterScriptDebugOutput>(
                    "script_debug_output", LLSD(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                )?.addLine(chat, chat.fromName)
            }

            if (chat.chatType == Chat.CHAT_TYPE_DEBUG_MSG || ownerSayRouting != 2u) {
                addOutputWindow(chat.fromID)
                FloaterReg.findTypedInstance<FloaterScriptDebugOutput>(
                    "script_debug_output", LLSD(chat.fromID)
                )?.addLine(chat, floaterLabel)
            }
        }

        private fun addOutputWindow(objectId: UUID, show: Boolean = false): Floater? {
            val host = FloaterReg.showTypedInstance<MultiFloater>("script_debug", LLSD())
                ?: return null

            Floater.setFloaterHost(host)
            val floaterp = FloaterReg.showInstance("script_debug_output", LLSD(objectId), focus = false)
            Floater.setFloaterHost(null)

            if (SavedSettings.getInt("ShowScriptErrorsLocation") == 0 && !show) {
                host.closeFloater()
            }

            return floaterp
        }
    }
}

class FloaterScriptDebugOutput(objectId: LLSD) : Floater(objectId) {

    private lateinit var mHistoryEditor: TextEditor
    private val mObjectID: UUID = objectId.asUUID()
    private var mUserName: String = ""

    init {
        setAutoFocus(false)
    }

    override fun postBuild(): Boolean {
        super.postBuild()
        mHistoryEditor = getChild<TextEditor>("Chat History Editor")
        return true
    }

    fun addLine(chat: Chat, userName: String) {
        val fontp = ViewerChat.getChatFont()
        val fontName = FontGL.nameFromFont(fontp)
        val fontSize = FontGL.sizeFromFont(fontp)
        val messageParams = Style.Params().apply {
            font.name(fontName)
            font.size(fontSize)
        }

        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (mObjectID == nullId) {
            setCanTearOff(false)
            setCanClose(false)
        } else {
            if (mHistoryEditor.getText().isEmpty() || mUserName != userName) {
                mUserName = userName
                setTitle(userName)
                setShortTitle(userName)

                if (SavedPerAccountSettings.getBool("FSllOwnerSayToScriptDebugWindow") &&
                    getKey().asUUID() == nullId
                ) {
                    var url = chat.url
                    if (url.isEmpty() || !url.contains("objectim")) {
                        url = ViewerChat.getSenderSLURL(chat, LLSD())
                    }
                    val linkParams = Style.Params(messageParams).apply {
                        readonlyColor(UIColorTable.instance.getColor("ChatNameObjectColor"))
                        isLink = true
                        linkHref = url
                    }
                    mHistoryEditor.appendText(chat.fromName, !mHistoryEditor.getText().isEmpty(), linkParams)
                }
            }
        }

        System.err.println("FloaterScriptDebug: time_corrected not yet implemented")
        val utcTime = System.currentTimeMillis() / 1000L
        var timeStr = "[${Trans.getString("TimeHour")}]:[${Trans.getString("TimeMin")}]"
        val substitution = LLSD()
        substitution["datetime"] = utcTime.toInt()
        timeStr = formatString(timeStr, substitution)

        mHistoryEditor.appendText(
            "[$timeStr] ",
            !mHistoryEditor.getText().isEmpty(),
            Style.Params(messageParams).readonlyColor(UIColorTable.instance.getColor("ChatTimestampColor"))
        )

        if (mObjectID == nullId) {
            val linkParams = Style.Params(messageParams).apply {
                readonlyColor(UIColorTable.instance.getColor("ChatNameObjectColor"))
            }
            mHistoryEditor.appendText("${chat.fromName}: ", newLine = false, params = linkParams)
        }

        if (chat.chatType == Chat.CHAT_TYPE_DEBUG_MSG) {
            mHistoryEditor.appendText(
                chat.text, newLine = false,
                params = Style.Params().readonlyColor(UIColorTable.instance.getColor("ScriptErrorColor"))
            )
        } else {
            mHistoryEditor.appendText(
                chat.text, newLine = false,
                params = Style.Params(messageParams).readonlyColor(UIColorTable.instance.getColor("llOwnerSayChatColor"))
            )
        }
        mHistoryEditor.blockUndo()
    }

    fun clear() {
        mHistoryEditor.clear()
    }
}
