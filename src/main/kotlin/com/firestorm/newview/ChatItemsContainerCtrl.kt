package com.firestorm.newview

import com.firestorm.ui.Panel
import com.firestorm.ui.UICtrl
import com.firestorm.ui.AvatarIconCtrl
import com.firestorm.ui.UIColor
import com.firestorm.ui.UIColorTable
import com.firestorm.ui.StyleParams
import com.firestorm.ui.Rect
import com.firestorm.chat.ChatSourceType
import com.firestorm.chat.ChatStyle
import com.firestorm.chat.ChatType
import com.firestorm.llsd.LLSD
import com.firestorm.types.LLUUID
import com.firestorm.floater.FloaterReg
import com.firestorm.avatar.AvatarActions

// Constants matching the C++ file-scope values
private const val MSG_LEFT_OFFSET = 10
private const val MSG_RIGHT_OFFSET = 10
private const val MSG_HEIGHT_PAD = 5

enum class ShowItemHeader {
    SHOW_ONLY_NAME,
    SHOW_ONLY_ICON,
    SHOW_BOTH
}

// ────────────────────────────────────────────────────────────────────────────
// FloaterIMNearbyChatToastPanel
// ────────────────────────────────────────────────────────────────────────────

open class FloaterIMNearbyChatToastPanel protected constructor() : Panel() {

    companion object {
        fun createInstance(): FloaterIMNearbyChatToastPanel {
            val item = FloaterIMNearbyChatToastPanel()
            item.buildFromFile("panel_chat_item.xml")
            item.setFollows(FOLLOWS_NONE)
            return item
        }
    }

    private var fromId: LLUUID = LLUUID.null_
    private var fromName: String = ""
    private var sourceType: ChatSourceType = ChatSourceType.OBJECT
    private var msgText: ChatMsgBox? = null
    private var isDirty: Boolean = false
    private var showIconTooltip: Boolean = true

    fun getFromId(): LLUUID = fromId
    fun getFromName(): String = fromName
    fun messageId(): LLUUID = fromId

    override fun postBuild(): Boolean = super.postBuild()

    override fun reshape(width: Int, height: Int, calledFromParent: Boolean) {
        super.reshape(width, height, calledFromParent)

        val msgTextCtrl = findChild<UICtrl>("msg_text") ?: return
        val icon = findChild<UICtrl>("avatar_icon") ?: return

        val avatarRect = icon.rect.copy()
        avatarRect.setLeftTopAndSize(2, height - 2, avatarRect.width, avatarRect.height)
        icon.setRect(avatarRect)

        val msgRect = Rect(
            left   = avatarRect.right + MSG_LEFT_OFFSET,
            top    = height - MSG_HEIGHT_PAD,
            right  = width - avatarRect.right - MSG_LEFT_OFFSET - MSG_RIGHT_OFFSET + avatarRect.right + MSG_LEFT_OFFSET,
            bottom = MSG_HEIGHT_PAD
        )
        val msgWidth  = width - avatarRect.right - MSG_LEFT_OFFSET - MSG_RIGHT_OFFSET
        val msgHeight = height - 2 * MSG_HEIGHT_PAD
        msgTextCtrl.reshape(msgWidth, msgHeight)
        msgTextCtrl.setRect(msgRect)
    }

    open fun init(data: LLSD) {
        val messageText = data["message"].asString()
        fromName = data["from"].asString()
        fromId = data["from_id"].asUUID()
        showIconTooltip = if (data.has("show_icon_tooltip")) data["show_icon_tooltip"].asBoolean() else true

        sourceType = ChatSourceType.fromInt(data["source"].asInt())

        val colorName = data["text_color"].asString()
        val textColor: UIColor = UIColorTable.instance.getColor(colorName)
        val textAlpha = data["color_alpha"].asFloat()
        val fontSize = data["font_size"].asInt()
        val messageFont = fontForSize(fontSize)

        msgText = getChild<ChatMsgBox>("msg_text")
        msgText!!.setContentTrusted(false)
        msgText!!.setIsFriendCallback(AvatarActions::isFriend)
        msgText!!.setText("")

        if (data["chat_style"].asInt() != ChatStyle.IRC.ordinal) {
            var senderStr = "$fromName "
            if (sourceType == ChatSourceType.AGENT || sourceType == ChatSourceType.OBJECT) {
                val nameStyle = StyleParams(
                    color = UIColorTable.instance.getColor("HTMLLinkColor"),
                    fontName = messageFont.name,
                    fontSize = messageFont.size
                )
                if (data.has("sender_slurl")) {
                    nameStyle.linkHref = data["sender_slurl"].asString()
                    nameStyle.isLink = true
                }
                msgText!!.appendText(senderStr, false, nameStyle)
            } else {
                msgText!!.appendText(senderStr, false)
            }
        }

        val charsInLine = msgText!!.rect.width / messageFont.charWidth
        val maxLines = data["available_height"].asInt() / (msgText!!.textPixelHeight + 4)

        var truncatedText = messageText
        var lines = 0
        var chars = 0
        val cutIndex = run {
            var idx = 0
            for (c in messageText) {
                if (lines >= maxLines) break
                if (c == '\n') { ++lines; chars = 0 }
                else {
                    ++chars
                    if (chars >= charsInLine) { chars = 0; ++lines }
                }
                idx++
            }
            idx
        }
        if (cutIndex < messageText.length) {
            truncatedText = messageText.substring(0, cutIndex) + " ..."
        }

        appendStyledMessage(truncatedText, data, textColor, textAlpha, messageFont, false)
        snapToMessageHeight()
        isDirty = true
    }

    open fun addMessage(data: LLSD) {
        val messageText = data["message"].asString()
        val colorName = data["text_color"].asString()
        val textColor: UIColor = UIColorTable.instance.getColor(colorName)
        val textAlpha = data["color_alpha"].asFloat()
        val fontSize = data["font_size"].asInt()
        val messageFont = fontForSize(fontSize)

        appendStyledMessage(messageText, data, textColor, textAlpha, messageFont, true)
        snapToMessageHeight()
    }

    private fun appendStyledMessage(text: String, data: LLSD, color: UIColor, alpha: Float, font: FontDescriptor, prepend: Boolean) {
        val style = StyleParams(color = color, alpha = alpha, fontName = font.name, fontSize = font.size)
        when {
            data["chat_style"].asInt() == ChatStyle.IRC.ordinal -> style.fontStyle = "ITALIC"
            data["chat_type"].asInt() == ChatType.SHOUT.ordinal   -> style.fontStyle = "BOLD"
            data["chat_type"].asInt() == ChatType.WHISPER.ordinal -> style.fontStyle = "ITALIC"
        }
        msgText!!.appendText(text, prepend, style)
    }

    fun snapToMessageHeight() {
        val newHeight = maxOf(
            (msgText?.textPixelHeight ?: 0) + 2 * (msgText?.vPad ?: 0) + 2 * MSG_HEIGHT_PAD,
            25
        )
        val panelRect = rect.copy()
        panelRect.setLeftTopAndSize(panelRect.left, panelRect.top, panelRect.width, newHeight)
        reshape(rect.width, rect.height)
        setRect(panelRect)
    }

    open fun onMouseLeave(x: Int, y: Int, mask: Int) {}

    open fun onMouseEnter(x: Int, y: Int, mask: Int) {
        if (sourceType != ChatSourceType.AGENT) return
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean = super.handleMouseDown(x, y, mask)

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        val msgRect = msgText?.rect ?: return super.handleMouseUp(x, y, mask)
        val localX = x - msgRect.left
        val localY = y - msgRect.bottom

        if (msgText!!.pointInView(localX, localY)) {
            if (msgText!!.handleMouseUp(localX, localY, mask)) return true
            showNearbyChatHistory()
            return false
        }

        showNearbyChatHistory()
        return super.handleMouseUp(x, y, mask)
    }

    private fun showNearbyChatHistory() {
        TODO("APR: use JVM equivalent — open nearby chat history floater (FSFloaterNearbyChat / FSFloaterIMContainer)")
    }

    fun setHeaderVisibility(e: ShowItemHeader) {
        val icon = findChild<UICtrl>("avatar_icon")
        icon?.setVisible(e == ShowItemHeader.SHOW_ONLY_ICON || e == ShowItemHeader.SHOW_BOTH)
    }

    fun canAddText(): Boolean {
        val msgBox = findChild<ChatMsgBox>("msg_text") ?: return false
        return msgBox.lineCount < 10
    }

    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val avatarIcon = getChild<UICtrl>("avatar_icon")
        val localX = x - avatarIcon.rect.left
        val localY = y - avatarIcon.rect.bottom
        if (avatarIcon.pointInView(localX, localY) && sourceType != ChatSourceType.AGENT) return true
        return super.handleRightMouseDown(x, y, mask)
    }

    override fun draw() {
        super.draw()
        if (isDirty) {
            val icon = findChild<AvatarIconCtrl>("avatar_icon")
            if (icon != null) {
                icon.setDrawTooltip(showIconTooltip && sourceType == ChatSourceType.AGENT)
                when (sourceType) {
                    ChatSourceType.OBJECT -> icon.setValue(LLSD("OBJECT_Icon"))
                    ChatSourceType.SYSTEM -> icon.setValue(LLSD("SL_Logo"))
                    ChatSourceType.AGENT  -> icon.setValue(fromId)
                    else -> if (!fromId.isNull) icon.setValue(fromId)
                }
            }
            isDirty = false
        }
    }

    private fun fontForSize(size: Int): FontDescriptor = when (size) {
        0    -> FontDescriptor("SansSerifSmall", "Small")
        2    -> FontDescriptor("SansSerifBig", "Big")
        3    -> FontDescriptor("SansSerifHuge", "Huge")
        else -> FontDescriptor("SansSerif", "Medium")
    }
}

// Minimal font descriptor used internally — mirrors C++ LLFontGL selection
data class FontDescriptor(val name: String, val size: String) {
    val charWidth: Int get() = TODO("GPU: measure font character width")
}
