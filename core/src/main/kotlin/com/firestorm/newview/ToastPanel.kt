package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import kotlin.math.max
import kotlin.math.min

abstract class ToastPanel(protected val notification: Notification) {

    var rect: Rect = Rect()

    open fun getTitle(): String =
        if (notification.hasLabel()) notification.getLabel() else notification.getMessage()

    open fun getNotificationName(): String = notification.name

    open fun getId(): LLUUID = notification.id

    protected fun computeSnappedToMessageHeight(message: TextBase, maxLineCount: Int): Int {
        val maxTextHeight = (message.font.lineHeight + message.lineSpacingPixels) * maxLineCount
        val oldTextHeight = message.rect.height
        val requiredTextHeight = message.textBoundingRect.height
        val newTextHeight = min(requiredTextHeight, maxTextHeight)
        val heightDelta = newTextHeight - oldTextHeight
        return max(rect.height + heightDelta, MIN_PANEL_HEIGHT)
    }

    protected fun snapToMessageHeight(message: TextBase?, maxLineCount: Int) {
        if (message == null) return
        if (message.visible) {
            val newPanelHeight = computeSnappedToMessageHeight(message, maxLineCount)
            if (newPanelHeight != rect.height) {
                reshape(rect.width, newPanelHeight)
            }
        }
    }

    open fun reshape(width: Int, height: Int) {
        rect = rect.copy(width = width, height = height)
    }

    companion object {
        const val MIN_PANEL_HEIGHT = 40
        // VPAD(4)*2 + ICON_HEIGHT(32); extra slack accounts for name buffer sizes
        const val MAX_TEXT_LENGTH = 512 + 20 + 31 + 31 + 63
    }
}

open class CheckBoxToastPanel(notification: Notification) : ToastPanel(notification) {

    protected var check: CheckBoxCtrl? = null

    fun setCheckBoxes(hPad: Int, vPad: Int, parentView: View? = null) {
        val form = notification.getForm()
        when (form.ignoreType) {
            IgnoreType.CHECKBOX_ONLY -> {
                val ignoreMessage = form.ignoreMessage.ifEmpty {
                    Notifications.getGlobalString("alwayschoose")
                }
                setCheckBox(ignoreMessage, "", hPad, vPad, parentView)
            }
            IgnoreType.WITH_DEFAULT_RESPONSE ->
                setCheckBox(Notifications.getGlobalString("skipnexttime"), "", hPad, vPad, parentView)
            IgnoreType.WITH_DEFAULT_RESPONSE_SESSION_ONLY ->
                setCheckBox(Notifications.getGlobalString("skipnexttimesessiononly"), "", hPad, vPad, parentView)
            IgnoreType.WITH_LAST_RESPONSE ->
                setCheckBox(Notifications.getGlobalString("alwayschoose"), "", hPad, vPad, parentView)
            else -> Unit
        }
    }

    fun setCheckBox(
        checkTitle: String,
        checkControl: String,
        hPad: Int,
        vPad: Int,
        parentView: View? = null,
    ): Boolean {
        val cb = CheckBoxCtrl()
        check = cb

        val lineHeight = cb.font.lineHeight
        val lines = checkTitle.split('\n')

        val maxMsgWidth = rect.width - 2 * hPad
        val checkWidth = cb.font.getWidth(lines[0]).toInt() + 16
        val dialogWidth = max(maxMsgWidth, checkWidth) + 2 * hPad
        val dialogHeight = rect.height + lineHeight * lines.size + lineHeight / 2

        reshape(dialogWidth, dialogHeight)

        val msgX = (rect.width - max(maxMsgWidth, checkWidth)) / 2
        val btnSpaceOffset = if (parentView != null) 0 else BTN_HEIGHT + lineHeight / 2
        val msgY = vPad + btnSpaceOffset
        cb.rect = Rect(msgX, msgY, max(maxMsgWidth, checkWidth), lineHeight * lines.size)
        cb.label = checkTitle
        cb.onCommit = { onCommitCheckbox(it) }

        if (parentView != null) {
            parentView.addChild(cb)
        } else {
            System.err.println("ToastPanel: addCheckbox — no parent view")
        }
        return true
    }

    protected fun onCommitCheckbox(ctrl: UICtrl) {
        var checked = ctrl.value.asBoolean()
        if (notification.getForm().ignoreType == IgnoreType.SHOW_AGAIN) {
            checked = !checked
        }
        notification.setIgnored(checked)
    }

    companion object {
        const val BTN_HEIGHT = 20
    }
}

fun buildPanelFromNotification(notification: Notification): ToastPanel? = when {
    notification.type.name.lowercase() == "notifytip" -> when (notification.name) {
        "FriendOnlineOffline" -> PanelOnlineStatus(notification)
        else -> PanelGenericTip(notification)
    }
    notification.type.name.lowercase() == "notify" -> when {
        notification.priority == NotificationPriority.CRITICAL -> ToastScriptQuestion(notification)
        else -> ToastNotifyPanel(notification)
    }
    else -> null
}

data class Rect(val x: Int = 0, val y: Int = 0, val width: Int = 0, val height: Int = 0)

interface View {
    fun addChild(child: Any)
}

interface UICtrl {
    val value: Value
    interface Value { fun asBoolean(): Boolean }
}

interface CheckBoxCtrl : UICtrl {
    val font: Font
    var rect: Rect
    var label: String
    var onCommit: ((UICtrl) -> Unit)?
}

interface TextBase {
    val font: Font
    val lineSpacingPixels: Int
    val rect: Rect
    val textBoundingRect: Rect
    val visible: Boolean
}

interface Font {
    val lineHeight: Int
    fun getWidth(text: String): Float
}

interface Value {
    fun asBoolean(): Boolean
}

class PanelOnlineStatus(notification: Notification) : ToastPanel(notification)
class PanelGenericTip(notification: Notification) : ToastPanel(notification)
class ToastScriptQuestion(notification: Notification) : ToastPanel(notification)
