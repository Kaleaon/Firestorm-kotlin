package com.firestorm.llui

import com.firestorm.llmath.Rect
import java.util.UUID

enum class NotificationPriority {
    UNSPECIFIED, LOW, NORMAL, HIGH, CRITICAL
}

data class Notification(
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val type: String = "notify",
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val message: String = "",
    val payload: Map<String, Any?> = emptyMap()
) {
    fun isType(t: String): Boolean = type == t
}

class ToastNotifyPanel(
    name: String,
    rect: Rect = Rect(),
    val notification: Notification,
    showImages: Boolean = true
) : Panel(name, rect) {

    companion object {
        const val FONT_DEFAULT = "Emoji"
        const val FONT_SCRIPT = "SansSerif"
        const val BUTTON_WIDTH = 90
        const val BOTTOM_PAD = 12
        const val IGNORE_BTN_TOP_DELTA = 12
        const val BTN_HEIGHT = 20
        const val BTN_HEIGHT_SMALL = 16

        private val buttonClickListeners: MutableList<(UUID, String) -> Unit> = mutableListOf()

        fun addButtonClickListener(listener: (UUID, String) -> Unit) {
            buttonClickListeners.add(listener)
        }

        private fun fireButtonClick(notificationId: UUID, buttonName: String) {
            buttonClickListeners.forEach { it(notificationId, buttonName) }
        }
    }

    val isTip: Boolean = notification.type == "notifytip"
    val isScriptDialog: Boolean = notification.name == "ScriptDialog" || notification.name == "ScriptDialogGroup"
    val isCaution: Boolean = notification.priority >= NotificationPriority.HIGH

    var message: String = notification.message
        private set

    var numOptions: Int = 0
        private set
    var numButtons: Int = 0
        private set
    private var addedDefaultButton: Boolean = false

    var textBox: View? = null
        private set
    var infoPanel: Panel? = null
        private set
    var controlPanel: Panel? = null
        private set

    private val buttonCallbackData: MutableList<ButtonCallbackData> = mutableListOf()

    private inner class ButtonCallbackData(val buttonName: String)

    init {
        buildLayout(rect, showImages)
    }

    private fun buildLayout(rect: Rect, showImages: Boolean) {
        TODO("GPU: load panel_notification layout and populate infoPanel / controlPanel children")
    }

    fun createButton(formElement: Map<String, Any?>, isOption: Boolean): Any {
        val buttonName = if (isOption) formElement["name"] as? String ?: "" else ""
        val text = formElement["text"] as? String ?: ""
        val index = (formElement["index"] as? Int) ?: 0
        val isSmall = index == -1 || index == -2
        val isDefault = formElement["default"] as? Boolean ?: false
        val enabled = (formElement["enabled"] as? Boolean) ?: true
        numButtons++

        val callbackData = ButtonCallbackData(buttonName)
        buttonCallbackData.add(callbackData)

        TODO("GPU: create Button '$buttonName' label='$text' height=${if (isSmall) BTN_HEIGHT_SMALL else BTN_HEIGHT} width=$BUTTON_WIDTH default=$isDefault enabled=$enabled")
    }

    private fun addDefaultButton() {
        val formElement = mapOf("name" to "OK", "text" to "OK", "default" to true)
        createButton(formElement, false)
        numButtons = 1
        addedDefaultButton = true
    }

    fun onClickButton(callbackData: ButtonCallbackData) {
        val response = buildResponse(callbackData.buttonName)
        controlPanel?.enabled = false
        notification.id.let { id ->
            fireButtonClick(id, callbackData.buttonName)
        }
        TODO("APR: use JVM equivalent — dispatch notification response $response")
    }

    private fun buildResponse(buttonName: String): Map<String, Any?> {
        if (addedDefaultButton || buttonName.isEmpty()) return emptyMap()
        return mapOf(buttonName to true)
    }

    fun updateButtonsLayout(buttons: List<Pair<Int, Any>>, hPad: Int) {
        val maxWidth = controlPanel?.rect?.width ?: return
        val bottomOffset = if (isScriptDialog) BTN_HEIGHT + IGNORE_BTN_TOP_DELTA + BOTTOM_PAD else BOTTOM_PAD
        TODO("GPU: lay out ${buttons.size} buttons with hPad=$hPad bottomOffset=$bottomOffset maxWidth=$maxWidth")
    }

    private fun adjustPanelForScriptNotice(buttonPanelWidth: Int, buttonPanelHeight: Int) {
        TODO("GPU: reshape panel and control panel to w=$buttonPanelWidth h=$buttonPanelHeight")
    }

    private fun adjustPanelForTipNotice() {
        controlPanel?.visible = false
        TODO("GPU: reshape panel to info-panel height only")
    }

    override fun draw() {
        if (!visible) return
        TODO("GPU: draw toast notify panel")
    }

    fun isControlPanelEnabled(): Boolean {
        val cp = controlPanel ?: return false
        if (!cp.enabled) return false
        return cp.children.any { it.enabled }
    }

    fun deleteAllChildren() {
        textBox = null
        infoPanel = null
        controlPanel = null
        numOptions = 0
        numButtons = 0
        addedDefaultButton = false
        children.clear()
    }
}

class IMToastNotifyPanel(
    name: String,
    rect: Rect = Rect(),
    notification: Notification,
    val sessionId: UUID,
    showImages: Boolean = true,
    private val parentTextBase: View? = null
) : ToastNotifyPanel(name, rect, notification, showImages) {

    init {
        compactButtons()
    }

    fun compactButtons() {
        TODO("GPU: compact and auto-resize all buttons in controlPanel from right-to-left order")
    }

    fun updateNotification() {
        deleteAllChildren()
        TODO("GPU: rebuild layout via init(LLRect(), true) equivalent")
    }

    override fun draw() {
        if (!visible) return
        TODO("GPU: draw IM toast notify panel and snap to message height")
    }
}
