package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLSD
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

private const val BOTTOM_PAD = 12       // VPAD(4) * 3
private const val IGNORE_BTN_TOP_DELTA = 12  // 3 * VPAD
private const val HPAD = 4
private const val VPAD = 4
private const val BTN_HEIGHT = 20
private const val BTN_HEIGHT_SMALL = 16
private var BUTTON_WIDTH = 90

open class ToastNotifyPanel(
    notification: Notification,
    initialRect: Rect = Rect(),
    showImages: Boolean = true,
) : CheckBoxToastPanel(notification) {

    protected var textBox: TextBox? = null
    protected var infoPanel: Panel? = null
    protected var controlPanel: Panel? = null

    protected var isTip: Boolean = false
    protected var addedDefaultBtn: Boolean = false
    protected var isScriptDialog: Boolean = false
    protected var isCaution: Boolean = false
    protected var message: String = ""
    protected var numOptions: Int = 0
    protected var numButtons: Int = 0

    private val btnCallbackData: MutableList<InstanceAndButtonName> = mutableListOf()
    private var buttonClickConnection: Any? = null

    data class InstanceAndButtonName(val self: ToastNotifyPanel, val buttonName: String)

    init {
        init(initialRect, showImages)
    }

    open fun init(rect: Rect, showImages: Boolean) {
        deleteAllChildren()

        isTip = notification.type.name.lowercase() == "notifytip"
        val notifName = notification.name
        isScriptDialog = notifName == "ScriptDialog" || notifName == "ScriptDialogGroup"
        isCaution = notification.priority >= NotificationPriority.HIGH
        message = notification.getMessage()

        val form = notification.getForm()
        numOptions = form.numElements

        if (isTip) {
            adjustPanelForTipNotice()
        } else {
            val buttons = mutableListOf<Pair<Int, Button>>()
            var buttonsWidth = 0
            for (i in 0 until numOptions) {
                val formElement = form.getElement(i)
                if (formElement["type"].asString() != "button") continue
                if (formElement["name"].asString() == TEXTBOX_MAGIC_TOKEN) continue
                val newButton = createButton(formElement, isOption = true)
                buttonsWidth += newButton.rect.width
                val index = formElement["index"].asInt()
                buttons.add(Pair(index, newButton))
            }

            if (buttons.isEmpty()) {
                addDefaultButton()
            } else {
                val buttonPanelWidth = controlPanel?.rect?.width ?: 0
                var buttonPanelHeight = controlPanel?.rect?.height ?: 0
                var hPad = (buttonPanelWidth - buttonsWidth) / buttons.size
                if (hPad < 2 * HPAD) {
                    val buttonPerRow = buttonPanelWidth / BUTTON_WIDTH
                    hPad = if (buttonPerRow > 1) {
                        (buttonPanelWidth % BUTTON_WIDTH) / (buttonPerRow - 1)
                    } else 2 * HPAD
                    if (hPad < 2 * HPAD) hPad = 2 * HPAD
                }
                buttonPanelHeight = if (isScriptDialog) {
                    val buttonRows = ceil((buttons.size - 2).toFloat() * (BUTTON_WIDTH + hPad) / (buttonPanelWidth + hPad)).toInt()
                    (buttonRows + 1) * (BTN_HEIGHT + VPAD) + IGNORE_BTN_TOP_DELTA + BOTTOM_PAD
                } else {
                    val buttonRows = ceil(((buttons.size - 1) * hPad + buttonsWidth).toFloat() / buttonPanelWidth).toInt()
                    buttonRows * (BTN_HEIGHT + VPAD) + BOTTOM_PAD
                }

                adjustPanelForScriptNotice(buttonPanelWidth, buttonPanelHeight)
                updateButtonsLayout(buttons, hPad)
            }
        }

        setCheckBoxes(HPAD * 2, 0, infoPanel)
        if (check != null) {
            val newPanelHeight = (check?.rect?.height ?: 0) + rect.height + VPAD
            reshape(rect.width, newPanelHeight)
        }
    }

    open fun deleteAllChildren() {
        textBox = null
        infoPanel = null
        controlPanel = null
        numOptions = 0
        numButtons = 0
        addedDefaultBtn = false
    }

    fun getControlPanel(): Panel? = controlPanel

    open fun updateNotification() {}

    fun isControlPanelEnabled(): Boolean {
        val cp = controlPanel ?: return false
        if (!cp.enabled) return false
        return cp.children.filterIsInstance<Button>().any { it.enabled }
    }

    protected fun createButton(formElement: LLSD, isOption: Boolean): Button {
        val userData = InstanceAndButtonName(
            self = this,
            buttonName = if (isOption) formElement["name"].asString() else "",
        )
        btnCallbackData.add(userData)

        val index = formElement["index"].asInt()
        val name = formElement["name"].asString()
        val text = formElement["text"].asString()
        val makeSmallBtn = index == -1 || index == -2
        val fontName = if (isScriptDialog) FONT_SCRIPT else FONT_DEFAULT
        val fontSize = if (makeSmallBtn) "Small" else "Medium"

        var width = BUTTON_WIDTH
        var height = BTN_HEIGHT
        var autoResize = false

        if (!isScriptDialog) {
            System.err.println("ToastNotifyPanel: createButton font measurement not yet implemented")
        }
        if (isScriptDialog && makeSmallBtn) {
            height = BTN_HEIGHT_SMALL
            autoResize = true
            width = 1
        }

        val enabled = !formElement.has("enabled") || formElement["enabled"].asBoolean()
        val isDefault = formElement["default"].asBoolean()

        numButtons++
        return Button(
            name = name,
            label = text,
            rect = Rect(0, 0, width, height),
            enabled = enabled,
            isDefault = isDefault,
            onClick = { onClickButton(userData) },
        )
    }

    protected fun addDefaultButton() {
        val formElement = LLSD.mapOf(
            "name" to LLSD.of("OK"),
            "text" to LLSD.of("OK"),
            "default" to LLSD.of(true),
        )
        val okBtn = createButton(formElement, isOption = false)
        val centeredX = abs(rect.width - BUTTON_WIDTH) / 2
        okBtn.rect = okBtn.rect.copy(x = centeredX, y = BOTTOM_PAD)
        System.err.println("ToastNotifyPanel: addDefaultButton not yet implemented")
        numButtons = 1
        addedDefaultBtn = true
    }

    protected fun adjustPanelForScriptNotice(buttonPanelWidth: Int, buttonPanelHeight: Int) {
        val infoPanelHeight = infoPanel?.rect?.height ?: 0
        reshape(rect.width, infoPanelHeight + buttonPanelHeight + VPAD)
        controlPanel?.reshape(buttonPanelWidth, buttonPanelHeight)
    }

    protected fun adjustPanelForTipNotice() {
        controlPanel?.visible = false
        reshape(rect.width, infoPanel?.rect?.height ?: rect.height)
        if (notification.payload.has("respond_on_mousedown") &&
            notification.payload["respond_on_mousedown"].asBoolean()
        ) {
            infoPanel?.onMouseDown = {
                Notifications.respond(notification, notification.getResponseTemplate())
            }
        }
    }

    protected fun updateButtonsLayout(buttons: List<Pair<Int, Button>>, hPad: Int) {
        var left = 0
        var bottomOffset = if (isScriptDialog) BTN_HEIGHT + IGNORE_BTN_TOP_DELTA + BOTTOM_PAD else BOTTOM_PAD
        val maxWidth = controlPanel?.rect?.width ?: 0

        var ignoreBtn: Button? = null
        var muteBtn: Button? = null

        for ((index, btn) in buttons) {
            when (index) {
                -2 -> { muteBtn = btn; continue }
                -1 -> { ignoreBtn = btn; continue }
            }
            if (buttons.size == 1) {
                left = (maxWidth - btn.rect.width) / 2
            } else if (left == 0 && buttons.size == 2) {
                left = (maxWidth - (btn.rect.width * 2) - hPad) / 2
            } else if (left + btn.rect.width > maxWidth) {
                left = 0
                bottomOffset += BTN_HEIGHT + VPAD
            }
            btn.rect = btn.rect.copy(x = left, y = bottomOffset)
            left = btn.rect.x + btn.rect.width + hPad
            controlPanel?.addChild(btn)
        }

        var ignoreBtnWidth = 0
        var muteBtnPad = 0
        if (isScriptDialog && ignoreBtn != null) {
            val ignoreBtnLeft = maxWidth - ignoreBtn.rect.width
            ignoreBtn.rect = ignoreBtn.rect.copy(x = ignoreBtnLeft, y = BOTTOM_PAD)
            ignoreBtnWidth = ignoreBtn.rect.width
            muteBtnPad = 4 * HPAD
            controlPanel?.addChild(ignoreBtn)
        }
        if (isScriptDialog && muteBtn != null &&
            !notification.payload.has("own_object")
        ) {
            val muteBtnLeft = maxWidth - muteBtn.rect.width - ignoreBtnWidth - muteBtnPad
            muteBtn.rect = muteBtn.rect.copy(x = muteBtnLeft, y = BOTTOM_PAD)
            controlPanel?.addChild(muteBtn)
        }
    }

    private fun onClickButton(data: InstanceAndButtonName) {
        val self = data.self
        val buttonName = data.buttonName
        val response = self.notification.getResponseTemplate().toMutableMap()
        if (!self.addedDefaultBtn && buttonName.isNotEmpty()) {
            response[buttonName] = LLSD.of(true)
        }
        self.controlPanel?.enabled = false
        Notifications.respond(self.notification, LLSD.ofMap(response))
        sButtonClickSignal.forEach { listener -> listener(self.notification.id, buttonName) }
    }

    fun onToastPanelButtonClicked(notificationId: LLUUID, btnName: String) {
        if (notification.id == notificationId) {
            controlPanel?.enabled = false
        }
    }

    companion object {
        const val FONT_DEFAULT = "Emoji"
        const val FONT_SCRIPT = "SansSerif"
        const val TEXTBOX_MAGIC_TOKEN = "%%TEXTBOX%%"

        private val sButtonClickSignal: MutableList<(LLUUID, String) -> Unit> = mutableListOf()

        fun addButtonClickListener(listener: (LLUUID, String) -> Unit): () -> Unit {
            sButtonClickSignal.add(listener)
            return { sButtonClickSignal.remove(listener) }
        }
    }
}

class IMToastNotifyPanel(
    notification: Notification,
    val sessionId: LLUUID,
    initialRect: Rect = Rect(),
    showImages: Boolean = true,
    private val parentText: IMTextBase? = null,
) : ToastNotifyPanel(notification, initialRect, showImages) {

    init {
        compactButtons()
    }

    override fun reshape(width: Int, height: Int) {
        super.reshape(width, height)
        snapToMessageHeightIM()
    }

    private fun snapToMessageHeightIM() {
        val tb = textBox ?: return
        if (tb.visible) {
            val newHeight = computeSnappedToMessageHeight(tb, ToastPanel.MAX_TEXT_LENGTH)
            if (newHeight != rect.height) {
                super.reshape(rect.width, newHeight)
            }
        }
    }

    fun compactButtons() {
        val children = controlPanel?.children ?: return
        var offset = 0
        var lastBottom = 0
        for (child in children.asReversed()) {
            if (child is Button) {
                if (lastBottom != child.rect.y) offset = 0
                child.rect = child.rect.copy(x = offset)
                child.leftPad = 2 * HPAD
                child.rightPad = 2 * HPAD
                child.rect = child.rect.copy(width = 0)
                child.autoResize()
                offset += HPAD + child.rect.width
                lastBottom = child.rect.y
            }
        }
        parentText?.needsReflow()
    }

    override fun updateNotification() {
        init(Rect(), true)
    }

    override fun init(rect: Rect, showImages: Boolean) {
        super.init(Rect(), showImages)
        compactButtons()
    }
}

interface IMTextBase : TextBase {
    fun needsReflow()
}

class Panel {
    var rect: Rect = Rect()
    var enabled: Boolean = true
    var visible: Boolean = true
    var onMouseDown: (() -> Unit)? = null
    val children: MutableList<Any> = mutableListOf()

    fun addChild(child: Any) { children.add(child) }
    fun reshape(width: Int, height: Int) { rect = rect.copy(width = width, height = height) }
}

class Button(
    val name: String,
    var label: String,
    var rect: Rect = Rect(),
    var enabled: Boolean = true,
    val isDefault: Boolean = false,
    var leftPad: Int = 0,
    var rightPad: Int = 0,
    val onClick: () -> Unit = {},
) {
    fun autoResize() {
        // no-op
    }
}

interface TextBox : TextBase {
    var maxTextLength: Int
    var visible: Boolean
    var plainText: Boolean
    var useEmoji: Boolean
    var contentTrusted: Boolean
    fun setValue(text: String)
}
