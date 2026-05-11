package com.firestorm.llui

import kotlin.math.max
import kotlin.math.min

data class Notification(
    val id: String,
    val icon: String = "",
    val message: String = "",
    val payload: Map<String, Any> = emptyMap(),
    val formElements: List<FormElement> = emptyList(),
    val ignoreType: IgnoreType = IgnoreType.NONE,
    val ignoreMessage: String = "",
    var ignored: Boolean = false,
    var active: Boolean = true
) {
    fun respond(response: Map<String, Any>) {
        TODO("APR: use JVM equivalent for notification response dispatch")
    }

    fun setIgnored(ignored: Boolean) {
        this.ignored = ignored
    }
}

enum class IgnoreType { NONE, SHOW_AGAIN, IGNORE }

sealed class FormElement(val name: String, val text: String) {
    class Button(name: String, text: String, val isDefault: Boolean = false) : FormElement(name, text)
    class TextInput(name: String, text: String, val isPassword: Boolean = false) : FormElement(name, text)
    class IgnoreCheck(name: String, text: String, val initialValue: Boolean = false) : FormElement(name, text)
}

class WindowShade(
    private val modal: Boolean = false,
    private var backgroundImage: UIImage? = null,
    private var textColor: Color4 = Color4.white,
    private val canClose: Boolean = true
) {
    companion object {
        private const val MIN_NOTIFICATION_AREA_HEIGHT = 30
        private const val MAX_NOTIFICATION_AREA_HEIGHT = 100
    }

    private val notifications: MutableList<Notification> = mutableListOf()
    private val notificationResponse: MutableMap<String, Any> = mutableMapOf()
    private var formHeight: Int = 0
    private var notificationsAreaVisible: Boolean = false
    private var notificationsAreaVisibleAmount: Float = 0f
    private var backgroundAreaBackgroundVisible: Boolean = false
    private var mouseOpaque: Boolean = false
    private var currentNotificationText: String = ""

    val closeListeners: MutableList<() -> Unit> = mutableListOf()
    val buttonClickListeners: MutableList<(String) -> Unit> = mutableListOf()
    val textInputListeners: MutableList<(String, String) -> Unit> = mutableListOf()
    val ignoreClickListeners: MutableList<(Boolean) -> Unit> = mutableListOf()

    fun show(notification: Notification) {
        notifications.add(notification)
        displayLatestNotification()
    }

    fun hide() {
        notificationsAreaVisible = false
    }

    fun isShown(): Boolean = notificationsAreaVisible

    fun setBackgroundImage(image: UIImage?) {
        backgroundImage = image
    }

    fun setTextColor(color: Color4) {
        textColor = color
    }

    fun setCanClose(canClose: Boolean) {
        // update visibility of close panel child
    }

    fun draw() {
        val messageHeight = getTextBoundingHeight()
        val notifHeight = min(
            max(messageHeight + 15, max(formHeight, MIN_NOTIFICATION_AREA_HEIGHT)),
            MAX_NOTIFICATION_AREA_HEIGHT
        )
        reshapeNotificationsArea(notifHeight)

        drawChildren()

        val iter = notifications.listIterator(notifications.size)
        while (iter.hasPrevious()) {
            val n = iter.previous()
            if (!n.active) {
                iter.remove()
                hide()
            }
        }

        if (notifications.isEmpty()) {
            hide()
        } else if (notificationsAreaVisibleAmount < 0.01f) {
            displayLatestNotification()
        }

        if (!notificationsAreaVisible && notificationsAreaVisibleAmount < 0.001f) {
            backgroundAreaBackgroundVisible = false
            mouseOpaque = false
        }
    }

    private fun onCloseNotification() {
        val current = getCurrentNotification() ?: return
        TODO("APR: use JVM equivalent for LLNotifications::cancel(notification)")
    }

    private fun onClickIgnore(checked: Boolean) {
        val notify = getCurrentNotification() ?: return
        val shouldIgnore = if (notify.ignoreType == IgnoreType.SHOW_AGAIN) !checked else checked
        notify.setIgnored(shouldIgnore)
        ignoreClickListeners.forEach { it(shouldIgnore) }
    }

    private fun onClickNotificationButton(name: String) {
        val notify = getCurrentNotification() ?: return
        notificationResponse[name] = true
        notify.respond(notificationResponse.toMap())
        buttonClickListeners.forEach { it(name) }
    }

    private fun onEnterNotificationText(value: String, name: String) {
        notificationResponse[name] = value
        textInputListeners.forEach { it(name, value) }
    }

    private fun displayLatestNotification() {
        if (notifications.isEmpty()) return

        val notification = notifications.last()
        currentNotificationText = notification.message
        notificationResponse.clear()

        val FORM_PADDING_HORIZONTAL = 10
        val FORM_PADDING_VERTICAL = 3
        val WIDGET_HEIGHT = 24
        val LINE_EDITOR_WIDTH = 120
        var curX = FORM_PADDING_HORIZONTAL
        var curY = FORM_PADDING_VERTICAL + WIDGET_HEIGHT
        var formWidth = curX

        if (notification.ignoreType != IgnoreType.NONE) {
            val checkWidth = measureCheckboxWidth(notification.ignoreMessage)
            curX = checkWidth + FORM_PADDING_HORIZONTAL
            formWidth = max(formWidth, curX)
        }

        for (element in notification.formElements) {
            when (element) {
                is FormElement.Button -> {
                    val buttonWidth = measureButtonWidth(element.text)
                    curX += buttonWidth + FORM_PADDING_HORIZONTAL
                    formWidth = max(formWidth, curX)
                }
                is FormElement.TextInput -> {
                    if (curX != FORM_PADDING_HORIZONTAL) {
                        curX = FORM_PADDING_HORIZONTAL
                        curY -= WIDGET_HEIGHT + FORM_PADDING_VERTICAL
                    }
                    curX = FORM_PADDING_HORIZONTAL + LINE_EDITOR_WIDTH + FORM_PADDING_HORIZONTAL + LINE_EDITOR_WIDTH + FORM_PADDING_HORIZONTAL
                    formWidth = max(formWidth, curX + LINE_EDITOR_WIDTH + FORM_PADDING_HORIZONTAL)
                    curX = FORM_PADDING_HORIZONTAL
                    curY -= WIDGET_HEIGHT + FORM_PADDING_VERTICAL
                }
                else -> {}
            }
        }

        formHeight = computeFormHeight(curY, WIDGET_HEIGHT, FORM_PADDING_VERTICAL)
        notificationsAreaVisible = true
        backgroundAreaBackgroundVisible = modal
        mouseOpaque = modal

        TODO("GPU: lay out and render notification area with icon, text, form elements, and close button")
    }

    private fun getCurrentNotification(): Notification? = notifications.lastOrNull()

    private fun getTextBoundingHeight(): Int = currentNotificationText.lines().size * 14

    private fun reshapeNotificationsArea(height: Int) {
        // adjust notification panel height
    }

    private fun drawChildren() {
        TODO("GPU: draw child layout panels and controls")
    }

    private fun measureCheckboxWidth(label: String): Int = label.length * 7 + 24

    private fun measureButtonWidth(label: String): Int = label.length * 7 + 16

    private fun computeFormHeight(curY: Int, widgetHeight: Int, paddingVertical: Int): Int =
        widgetHeight + paddingVertical - curY
}
