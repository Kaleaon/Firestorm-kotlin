package com.firestorm.llui

import kotlin.math.ceil
import kotlin.math.max

// --- Callback type aliases ---
typealias ToolStartDragCallback   = (x: Int, y: Int, button: ToolbarButton) -> Unit
typealias ToolHandleDragCallback  = (x: Int, y: Int, uuid: String, type: AssetType) -> Boolean
typealias ToolHandleDropCallback  = (data: Any?, cargoType: DragAndDropType, x: Int, y: Int, toolbar: Toolbar) -> Boolean

// --- Enums ---

enum class ButtonType {
    ICONS_WITH_TEXT,
    ICONS_ONLY,
    TEXT_ONLY
}

enum class SideType {
    BOTTOM, LEFT, RIGHT, TOP
}

enum class ToolbarLocation {
    NONE, LEFT, RIGHT, BOTTOM;

    companion object {
        val FIRST = LEFT
        val LAST  = BOTTOM
    }
}

enum class Alignment {
    START, END, CENTER
}

enum class LayoutStyle {
    NONE, EQUALIZE, FILL
}

fun getOrientation(sideType: SideType): Orientation =
    if (sideType == SideType.LEFT || sideType == SideType.RIGHT) Orientation.VERTICAL
    else Orientation.HORIZONTAL

// --- ToolbarButton ---

open class ToolbarButton(params: Params = Params()) : Button(params) {

    open class Params : Button.Params() {
        var buttonWidthMin: Int = 0
        var buttonWidthMax: Int = Int.MAX_VALUE
        var desiredHeight: Int = 20
    }

    var commandId: CommandId = CommandId.NULL
    var mouseDownX: Int = 0
    var mouseDownY: Int = 0
    var widthMin: Int = (params as? Params)?.buttonWidthMin ?: 0
    var widthMax: Int = (params as? Params)?.buttonWidthMax ?: Int.MAX_VALUE
    var desiredHeight: Int = (params as? Params)?.desiredHeight ?: 20
    var isDragged: Boolean = false
    var initialWidth: Int = 0
        private set

    var startDragCallback: ToolStartDragCallback? = null
    var handleDragCallback: ToolHandleDragCallback? = null

    val isEnabledSignal:   MutableList<UICtrl.EnableCallback> = mutableListOf()
    val isRunningSignal:   MutableList<UICtrl.EnableCallback> = mutableListOf()
    val isStartingSignal:  MutableList<UICtrl.EnableCallback> = mutableListOf()

    var originalImageSelected: UIImage? = null
    var originalImageUnselected: UIImage? = null
    var originalImagePressed: UIImage? = null
    var originalImagePressedSelected: UIImage? = null
    var originalLabelColor: UIColor = UIColor.WHITE
    var originalLabelColorSelected: UIColor = UIColor.WHITE
    var originalImageOverlayColor: UIColor = UIColor.WHITE
    var originalImageOverlaySelectedColor: UIColor = UIColor.WHITE

    fun clampWidth(w: Int): Int = w.coerceIn(widthMin, widthMax)
    fun getInitialWidth(): Int = initialWidth

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        mouseDownX = x
        mouseDownY = y
        return super.handleMouseDown(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        val dx = x - mouseDownX
        val dy = y - mouseDownY
        val distSq = dx * dx + dy * dy
        val threshold = DRAG_N_DROP_DISTANCE_THRESHOLD
        if (distSq > threshold * threshold && hasMouseCapture() &&
            startDragCallback != null && handleDragCallback != null) {
            return if (!isDragged) {
                startDragCallback!!(x, y, this)
                isDragged = true
                true
            } else {
                handleDragCallback!!(x, y, commandId.uuid, AssetType.AT_WIDGET)
            }
        }
        return super.handleHover(x, y, mask)
    }

    override fun onMouseEnter(x: Int, y: Int, mask: Int) {
        super.onMouseEnter(x, y, mask)
        needsHighlight = true
        val parentToolbar = getParentByType<Toolbar>()
        parentToolbar?.buttonEnterSignal?.forEach { it(this) }
    }

    override fun onMouseLeave(x: Int, y: Int, mask: Int) {
        super.onMouseLeave(x, y, mask)
        val parentToolbar = getParentByType<Toolbar>()
        parentToolbar?.buttonLeaveSignal?.forEach { it(this) }
    }

    fun onMouseCaptureLost() { isDragged = false }

    override fun onCommit() {
        val command = CommandManager.getCommand(commandId) ?: return
        val enabled = isEnabledSignal.isEmpty() ||
            isEnabledSignal.all { it(this, command.isEnabledParameters()) }
        if (enabled) super.onCommit()
    }

    override fun reshape(width: Int, height: Int, calledFromParent: Boolean) {
        super.reshape(clampWidth(width), height, calledFromParent)
        if (initialWidth == 0) initialWidth = width
    }

    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            imageSelected           = originalImageSelected
            imageUnselected         = originalImageUnselected
            imagePressed            = originalImagePressed
            imagePressedSelected    = originalImagePressedSelected
            unselectedLabelColor    = originalLabelColor
            selectedLabelColor      = originalLabelColorSelected
            imageOverlayColor       = originalImageOverlayColor
            imageOverlaySelectedColor = originalImageOverlaySelectedColor
        } else {
            imageSelected           = imageDisabledSelected
            imageUnselected         = imageDisabled
            imagePressed            = imageDisabled
            imagePressedSelected    = imageDisabledSelected
            unselectedLabelColor    = disabledLabelColor
            selectedLabelColor      = disabledSelectedLabelColor
            imageOverlayColor       = imageOverlayDisabledColor
            imageOverlaySelectedColor = imageOverlayDisabledColor
        }
    }

    override fun getToolTip(): String {
        val parentToolbar = getParentByType<Toolbar>()
        val base = if (labelIsTruncated() || currentLabel.isEmpty()) {
            val cmd = CommandManager.getCommand(commandId)
            val labelKey = cmd?.labelRef ?: ""
            Trans.getString(labelKey) + " -- " + super.getToolTip()
        } else {
            super.getToolTip()
        }
        val suffix = parentToolbar?.buttonTooltipSuffix ?: ""
        return if (suffix.isNotEmpty()) "$base\n($suffix)" else base
    }

    fun callIfEnabled(commit: UICtrl.CommitCallback, ctrl: UICtrl, param: LLSD) {
        val command = CommandManager.getCommand(commandId) ?: return
        val enabled = isEnabledSignal.isEmpty() ||
            isEnabledSignal.all { it(this, command.isEnabledParameters()) }
        if (enabled) commit(ctrl, param)
    }

    companion object {
        const val DRAG_N_DROP_DISTANCE_THRESHOLD = 3
    }
}

// --- Toolbar ---

open class Toolbar(params: Params = Params()) : UICtrl(params) {

    // --- nested layout panel ---

    inner class CenterLayoutPanel(params: LayoutPanel.Params = LayoutPanel.Params()) : LayoutPanel(params) {
        var locationId: ToolbarLocation = ToolbarLocation.NONE
        var reshapeCallback: ((ToolbarLocation, Rect) -> Unit)? = null
        var buttonPanel: Panel? = null

        override fun handleReshape(rect: Rect, byUser: Boolean) {
            super.handleReshape(rect, byUser)
            reshapeCallback?.let { cb ->
                val bp = buttonPanel ?: return
                val r = localRectToOtherView(bp.rect)
                cb(locationId, r)
            }
        }
    }

    open class Params : UICtrl.Params() {
        var buttonDisplayMode: ButtonType = ButtonType.ICONS_WITH_TEXT
        var side: SideType = SideType.TOP
        var buttonIconParams: ToolbarButton.Params = ToolbarButton.Params()
        var buttonIconAndTextParams: ToolbarButton.Params = ToolbarButton.Params()
        var buttonTextOnlyParams: ToolbarButton.Params = ToolbarButton.Params()
        var readOnly: Boolean = false
        var wrap: Boolean = true
        var padLeft: Int = 0
        var padTop: Int = 0
        var padRight: Int = 0
        var padBottom: Int = 0
        var padBetween: Int = 0
        var minGirth: Int = 0
        var maxRows: Int = 0
        var commands: MutableList<CommandId> = mutableListOf()
        var layoutStyle: LayoutStyle = LayoutStyle.NONE
        var alignment: Alignment = Alignment.CENTER
    }

    companion object {
        const val RANK_NONE = -1
    }

    // --- static layout state ---
    val readOnly: Boolean        = (params as? Params)?.readOnly ?: false
    val sideType: SideType       = (params as? Params)?.side ?: SideType.TOP
    private val wrap: Boolean    = (params as? Params)?.wrap ?: true
    private val padLeft: Int     = (params as? Params)?.padLeft ?: 0
    private val padRight: Int    = (params as? Params)?.padRight ?: 0
    private val padTop: Int      = (params as? Params)?.padTop ?: 0
    private val padBottom: Int   = (params as? Params)?.padBottom ?: 0
    private val padBetween: Int  = (params as? Params)?.padBetween ?: 0
    private val minGirth: Int    = (params as? Params)?.minGirth ?: 0
    private val maxRows: Int     = (params as? Params)?.maxRows ?: 0

    private val buttonParams: Map<ButtonType, ToolbarButton.Params> = mapOf(
        ButtonType.ICONS_WITH_TEXT to ((params as? Params)?.buttonIconAndTextParams ?: ToolbarButton.Params()),
        ButtonType.ICONS_ONLY      to ((params as? Params)?.buttonIconParams        ?: ToolbarButton.Params()),
        ButtonType.TEXT_ONLY       to ((params as? Params)?.buttonTextOnlyParams    ?: ToolbarButton.Params())
    )

    // --- layout state ---
    var layoutStyle: LayoutStyle = (params as? Params)?.layoutStyle ?: LayoutStyle.NONE
    var alignment: Alignment     = (params as? Params)?.alignment   ?: Alignment.CENTER
    private var needsLayout: Boolean = false
    private var modified: Boolean = false

    // --- drag-and-drop state ---
    var startDragItemCallback: ToolStartDragCallback?  = null
    var handleDragItemCallback: ToolHandleDragCallback? = null
    var handleDropCallback: ToolHandleDropCallback?    = null
    private var dragAndDropTarget: Boolean = false
    private var dragRank: Int = 0
    private var dragX: Int = 0
    private var dragY: Int = 0
    private var dragGirth: Int = 0

    // --- buttons ---
    private val buttons: MutableList<ToolbarButton> = mutableListOf()
    private val buttonCommands: MutableList<CommandId> = mutableListOf()
    private val buttonMap: MutableMap<String, ToolbarButton> = mutableMapOf()
    var buttonType: ButtonType = (params as? Params)?.buttonDisplayMode ?: ButtonType.ICONS_WITH_TEXT
        private set

    // --- related widgets ---
    private var centeringStack: LayoutStack? = null
    private var centerPanel: CenterLayoutPanel? = null
    private var buttonPanel: Panel? = null
    var startCenteringPanel: Panel? = null
    var endCenteringPanel: Panel? = null
    private var rightMouseTargetButton: ToolbarButton? = null
    private var caretIcon: IconCtrl? = null

    // --- signals ---
    val buttonAddSignal:    MutableList<(View) -> Unit> = mutableListOf()
    val buttonEnterSignal:  MutableList<(View) -> Unit> = mutableListOf()
    val buttonLeaveSignal:  MutableList<(View) -> Unit> = mutableListOf()
    val buttonRemoveSignal: MutableList<(View) -> Unit> = mutableListOf()

    var buttonTooltipSuffix: String = ""

    init {
        initFromParams(params as? Params ?: Params())
    }

    override fun initFromParams(p: UICtrl.Params) {
        super.initFromParams(p)
        val tp = p as? Params ?: return
        val orientation = getOrientation(tp.side)

        val centeringStackParams = LayoutStack.Params().also {
            it.name = "centering_stack"
            it.orientation = orientation
            it.mouseOpaque = false
        }
        centeringStack = LayoutStack(centeringStackParams)
        addChild(centeringStack!!)

        val borderPanelParams = LayoutPanel.Params().also {
            it.name = "border_panel"
            it.autoResize = true
            it.userResize = false
            it.mouseOpaque = false
        }

        startCenteringPanel = UICtrlFactory.createFromParams<LayoutPanel>(borderPanelParams)
        centeringStack!!.addChild(startCenteringPanel!!)

        val centerPanelParams = LayoutPanel.Params().also {
            it.name = "center_panel"
            it.autoResize = false
            it.userResize = false
            it.mouseOpaque = false
        }
        centerPanel = CenterLayoutPanel(centerPanelParams)
        centeringStack!!.addChild(centerPanel!!)

        val buttonPanelParams = Panel.Params(tp.buttonPanel ?: Panel.Params()).also {
            it.followsFlags = FollowsFlags.BOTTOM or FollowsFlags.LEFT
        }
        buttonPanel = Panel(buttonPanelParams)
        centerPanel!!.buttonPanel = buttonPanel
        centerPanel!!.addChild(buttonPanel!!)

        endCenteringPanel = UICtrlFactory.createFromParams<LayoutPanel>(borderPanelParams)
        centeringStack!!.addChild(endCenteringPanel!!)

        for (id in tp.commands) addCommand(id)

        needsLayout = true
    }

    fun getCenterLayoutPanel(): CenterLayoutPanel? = centerPanel

    // --- command management ---

    fun addCommand(commandId: CommandId, rank: Int = RANK_NONE): Boolean {
        val command = CommandManager.getCommand(commandId) ?: return false
        val button = createButton(commandId)
        buttonPanel?.addChild(button)
        buttonMap[commandId.uuid] = button

        if (rank >= buttonCommands.size || rank == RANK_NONE) {
            buttonCommands.add(command.id)
            buttons.add(button)
        } else {
            buttonCommands.add(rank, command.id)
            buttons.add(rank, button)
        }

        needsLayout = true
        updateLayoutAsNeeded()
        buttonAddSignal.forEach { it(button) }
        return true
    }

    fun removeCommand(commandId: CommandId): Int {
        if (!hasCommand(commandId)) return RANK_NONE
        buttonMap.remove(commandId.uuid)

        val rank = buttons.indexOfFirst { it.commandId == commandId }
        if (rank < 0) return RANK_NONE

        val button = buttons[rank]
        buttonRemoveSignal.forEach { it(button) }
        buttonPanel?.removeChild(button)
        buttons.removeAt(rank)
        buttonCommands.removeAt(rank)

        needsLayout = true
        return rank
    }

    fun clearCommandsList() {
        buttonCommands.clear()
        createButtons()
    }

    fun hasCommand(commandId: CommandId): Boolean =
        commandId != CommandId.NULL && buttonMap.containsKey(commandId.uuid)

    fun enableCommand(commandId: CommandId, enabled: Boolean): Boolean {
        if (commandId == CommandId.NULL) return false
        val btn = buttonMap[commandId.uuid] ?: return false
        btn.setEnabled(enabled)
        return true
    }

    fun stopCommandInProgress(commandId: CommandId): Boolean {
        if (commandId == CommandId.NULL) return false
        val command = CommandManager.getCommand(commandId) ?: return false
        if (command.executeStopFunctionName.isEmpty()) return false
        val button = buttonMap[commandId.uuid] ?: return false
        val running = button.isRunningSignal.all { it(button, command.isRunningParameters()) }
        if (running) button.onCommit()
        return true
    }

    fun flashCommand(commandId: CommandId, flash: Boolean, forceFlashing: Boolean = false): Boolean {
        if (commandId == CommandId.NULL) return false
        val btn = buttonMap[commandId.uuid] ?: return false
        btn.setFlashing(flash, forceFlashing)
        return true
    }

    fun getCommandsList(): MutableList<CommandId> = buttonCommands

    // --- layout ---

    fun setButtonType(buttonType: ButtonType) {
        val regen = this.buttonType != buttonType
        this.buttonType = buttonType
        if (regen) createButtons()
    }

    private fun resizeButtonsInRow(buttonsInRow: List<ToolbarButton>, maxRowGirth: Int) {
        for (button in buttonsInRow) {
            if (getOrientation(sideType) == Orientation.HORIZONTAL) {
                button.reshape(button.clampWidth(button.rect.width), maxRowGirth)
            } else {
                button.reshape(maxRowGirth, button.rect.height)
            }
        }
    }

    private fun updateLayoutAsNeeded() {
        if (!needsLayout) return
        val orientation = getOrientation(sideType)

        val maxLength: Int
        var curRow: Int
        val rowPadStart: Int
        val rowPadEnd: Int
        val girthPadEnd: Int

        if (orientation == Orientation.HORIZONTAL) {
            maxLength    = rect.width - padLeft - padRight
            rowPadStart  = padLeft
            rowPadEnd    = padRight
            curRow       = padTop
            girthPadEnd  = padBottom
        } else {
            maxLength    = rect.height - padTop - padBottom
            rowPadStart  = padTop
            rowPadEnd    = padBottom
            curRow       = padLeft
            girthPadEnd  = padRight
        }

        var rowRunningLength = rowPadStart
        var curStart         = rowPadStart
        var maxRowGirth      = 0
        var maxRowLength     = 0

        val fullScreenWidth  = maxLength
        var equalizedWidth   = 0

        when (layoutStyle) {
            LayoutStyle.FILL -> {
                if (buttons.isNotEmpty()) {
                    equalizedWidth = (fullScreenWidth - padBetween * (buttons.size + 1)) / buttons.size
                }
            }
            LayoutStyle.EQUALIZE -> {
                for (button in buttons) {
                    val w = button.getInitialWidth()
                    if (w > equalizedWidth) equalizedWidth = w
                }
                val totalW = buttons.size * equalizedWidth + (buttons.size + 1) * padBetween
                if (maxRows > 0 && orientation == Orientation.HORIZONTAL && totalW > fullScreenWidth) {
                    val buttonsPerRow = ceil(buttons.size.toDouble() / maxRows).toInt()
                    equalizedWidth = (fullScreenWidth - padBetween * (buttonsPerRow + 1)) / buttonsPerRow
                }
            }
            else -> {}
        }

        val panelRect = buttonPanel?.localRect ?: Rect()
        val buttonsInRow: MutableList<ToolbarButton> = mutableListOf()

        for (button in buttons) {
            if (equalizedWidth > 0) {
                val effMin = if (button.widthMin > equalizedWidth) equalizedWidth else button.widthMin
                val effMax = equalizedWidth
                button.widthMin = effMin
                button.widthMax = effMax
                button.reshape(equalizedWidth, button.desiredHeight)
            } else {
                button.reshape(button.widthMin, button.desiredHeight)
            }
            button.autoResize()

            val buttonClampedWidth = if (equalizedWidth > 0) equalizedWidth
                                     else button.clampWidth(button.rect.width)
            val buttonLength = if (orientation == Orientation.HORIZONTAL) buttonClampedWidth else button.rect.height
            val buttonGirth  = if (orientation == Orientation.HORIZONTAL) button.rect.height else buttonClampedWidth

            if (wrap &&
                layoutStyle != LayoutStyle.FILL &&
                rowRunningLength + buttonLength > maxLength &&
                curStart != rowPadStart) {

                if (orientation == Orientation.VERTICAL) {
                    val clamped = button.clampWidth(maxRowGirth)
                    if (clamped != maxRowGirth) resizeButtonsInRow(buttonsInRow, clamped)
                }
                resizeButtonsInRow(buttonsInRow, maxRowGirth)
                buttonsInRow.clear()
                maxRowLength     = max(maxRowLength, rowRunningLength)
                rowRunningLength = rowPadStart
                curStart         = rowPadStart
                curRow          += maxRowGirth + padBetween
                maxRowGirth      = 0
            }

            val buttonRect = if (orientation == Orientation.HORIZONTAL) {
                Rect.fromLeftTopSize(curStart, panelRect.top - curRow, buttonClampedWidth, button.rect.height)
            } else {
                Rect.fromLeftTopSize(curRow, panelRect.top - curStart, buttonClampedWidth, button.rect.height)
            }
            button.setShape(buttonRect)
            buttonsInRow.add(button)

            rowRunningLength += buttonLength + padBetween
            curStart          = rowRunningLength
            maxRowGirth       = max(buttonGirth, maxRowGirth)
        }

        val totalGirth = max(curRow + maxRowGirth + girthPadEnd, minGirth)
        maxRowLength   = max(maxRowLength, rowRunningLength - padBetween + rowPadEnd)

        if (equalizedWidth == 0) resizeButtonsInRow(buttonsInRow, maxRowGirth)

        if (orientation == Orientation.HORIZONTAL) {
            if (sideType == SideType.TOP) translate(0, rect.height - totalGirth)
            reshape(rect.width, totalGirth)
            buttonPanel?.reshape(maxRowLength, totalGirth)
        } else {
            if (sideType == SideType.RIGHT) translate(rect.width - totalGirth, 0)
            reshape(totalGirth, rect.height)
            buttonPanel?.reshape(totalGirth, maxRowLength)
        }

        buttonPanel?.parent?.setShape(buttonPanel!!.localRect)
        centeringStack?.updateLayout()

        when (alignment) {
            Alignment.CENTER -> {
                startCenteringPanel?.setVisible(true)
                endCenteringPanel?.setVisible(true)
            }
            Alignment.START -> {
                startCenteringPanel?.setVisible(false)
                endCenteringPanel?.setVisible(true)
            }
            Alignment.END -> {
                startCenteringPanel?.setVisible(true)
                endCenteringPanel?.setVisible(false)
            }
        }

        buttonPanel?.setVisible(buttons.isNotEmpty())
        buttonPanel?.setMouseOpaque(buttons.isNotEmpty())
        needsLayout = false
    }

    override fun draw() {
        buttonPanel?.setVisible(buttons.isNotEmpty())
        buttonPanel?.setMouseOpaque(buttons.isNotEmpty())

        if (!readOnly) {
            for (btn in buttons) {
                val command = CommandManager.getCommand(btn.commandId) ?: continue
                if (btn.isEnabledSignal.isNotEmpty()) {
                    btn.setEnabled(btn.isEnabledSignal.all { it(btn, command.isEnabledParameters()) })
                }
                if (btn.isRunningSignal.isNotEmpty()) {
                    btn.setToggleState(btn.isRunningSignal.all { it(btn, command.isRunningParameters()) })
                }
            }
        }

        updateLayoutAsNeeded()
        TODO("GPU: translate and draw toolbar (LLUI::popMatrix/pushMatrix/translate equivalent)")
        super.draw()
    }

    override fun reshape(width: Int, height: Int, calledFromParent: Boolean) {
        super.reshape(width, height, calledFromParent)
        needsLayout = true
    }

    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val bpRect = buttonPanel?.let { localRectToOtherView(it.localRect, this) } ?: return false
        val handleHere = !readOnly && bpRect.containsPoint(x, y)
        if (handleHere) {
            rightMouseTargetButton = null
            for (button in buttons) {
                val br = localRectToOtherView(button.localRect, this)
                if (br.containsPoint(x, y)) {
                    rightMouseTargetButton = button
                    break
                }
            }
            createContextMenu(x, y)
        }
        return handleHere
    }

    open fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: AcceptanceRef, tooltipMsg: StringBuilder
    ): Boolean {
        var handled = handleDropCallback != null
        if (handled && drop) {
            handled = handleDropCallback!!(cargoData, cargoType, x, y, this)
        }
        accept.value = if (handled) Acceptance.YES_SINGLE else Acceptance.NO
        dragAndDropTarget = false
        if (!readOnly && handled && !drop) {
            if (cargoType == DragAndDropType.DAD_WIDGET) {
                val item = cargoData as? InventoryItem
                if (item != null) {
                    val dragged = CommandId(item.uuid)
                    val origRank = getRankFromPosition(dragged)
                    dragRank = getRankFromPosition(x, y)
                    dragAndDropTarget = (origRank == RANK_NONE) ||
                        ((dragRank != origRank) && ((dragRank - 1) != origRank))
                } else {
                    handled = false
                }
            } else {
                handled = false
            }
        }
        return handled
    }

    fun getRankFromPosition(x: Int, y: Int): Int {
        if (buttons.isEmpty()) return RANK_NONE
        val orientation = getOrientation(sideType)
        var rank = 0
        for (button in buttons) {
            val br = button.rect
            val pointX = br.right + padRight
            val pointY = br.bottom - padBottom
            if (x < pointX && y > pointY) break
            rank++
        }
        if (rank < buttons.size) {
            val br = buttons[rank].rect
            if (orientation == Orientation.HORIZONTAL) {
                val mid = (br.right + br.left) / 2
                if (x < mid) {
                    dragX = br.left - padLeft; dragY = br.top + padTop
                } else {
                    rank++; dragX = br.right + padRight - 1; dragY = br.top + padTop
                }
            } else {
                val mid = (br.top + br.bottom) / 2
                if (y > mid) {
                    dragX = br.left - padLeft; dragY = br.top + padTop
                } else {
                    rank++; dragX = br.left - padLeft; dragY = br.bottom - padBottom + 1
                }
            }
            dragGirth = if (orientation == Orientation.HORIZONTAL)
                br.height + padBottom + padTop
            else
                br.width + padLeft + padRight
        } else {
            val last = buttons.last().rect
            if (orientation == Orientation.HORIZONTAL) {
                dragX = last.right + padRight; dragY = last.top + padTop
            } else {
                dragX = last.left - padLeft; dragY = last.bottom - padBottom
            }
            dragGirth = if (orientation == Orientation.HORIZONTAL)
                last.height + padBottom + padTop
            else
                last.width + padLeft + padRight
        }
        return rank
    }

    fun getRankFromPosition(id: CommandId): Int {
        if (!hasCommand(id)) return RANK_NONE
        return buttons.indexOfFirst { it.commandId == id }
    }

    fun isModified(): Boolean = modified

    // --- button construction ---

    fun createButton(id: CommandId): ToolbarButton {
        val commandp = CommandManager.getCommand(id)
            ?: error("No command found for id: $id")

        val buttonP = ToolbarButton.Params().also {
            it.name  = commandp.name
            it.label = Trans.getString(commandp.labelRef)
            it.toolTip = Trans.getString(commandp.tooltipRef)
            if (!readOnly) {
                if (commandp.controlVariableName.isNotEmpty()) {
                    it.controlName = commandp.controlVariableName
                    it.isToggle = true
                }
                if (commandp.checkboxControlVariableName.isNotEmpty()) {
                    it.checkboxControl = commandp.checkboxControlVariableName
                }
            }
            if (buttonType != ButtonType.TEXT_ONLY) {
                it.imageOverlay = UIImage.get(commandp.icon)
            }
            it.buttonFlashEnable = commandp.isFlashingAllowed
        }
        buttonParams[buttonType]?.let { buttonP.overwriteFrom(it) }

        val button = ToolbarButton(buttonP)

        if (!readOnly) {
            val isEnabledFn = commandp.isEnabledFunctionName
            if (isEnabledFn.isNotEmpty()) {
                val isEnabledCB = initEnableCallback(UICtrl.EnableCallbackParam().also {
                    it.base.functionName = isEnabledFn
                    it.base.parameter    = commandp.isEnabledParameters()
                })
                button.isEnabledSignal.add(isEnabledCB)
            }

            val execFn   = commandp.executeFunctionName
            val execStopFn = commandp.executeStopFunctionName
            val execCB   = initCommitCallback(UICtrl.CommitCallbackParam().also {
                it.base.functionName = execFn
                it.base.parameter    = commandp.executeParameters()
            })

            if (execStopFn.isNotEmpty()) {
                val stopCB = initCommitCallback(UICtrl.CommitCallbackParam().also {
                    it.base.functionName = execStopFn
                    it.base.parameter    = commandp.executeStopParameters()
                })
                button.setFunctionName(execFn)
                button.addMouseDownCallback { ctrl, _ -> button.callIfEnabled(execCB, ctrl, LLSD()) }
                button.addMouseUpCallback   { ctrl, _ -> button.callIfEnabled(stopCB, ctrl, LLSD()) }
            } else {
                button.setFunctionName(execFn)
                button.addCommitCallback { ctrl, param -> button.callIfEnabled(execCB, ctrl, param) }
            }

            val isRunningFn = commandp.isRunningFunctionName
            if (isRunningFn.isNotEmpty()) {
                val isRunningCB = initEnableCallback(UICtrl.EnableCallbackParam().also {
                    it.base.functionName = isRunningFn
                    it.base.parameter    = commandp.isRunningParameters()
                })
                button.isRunningSignal.add(isRunningCB)
            }
        }

        button.startDragCallback  = startDragItemCallback
        button.handleDragCallback = handleDragItemCallback
        button.commandId          = id
        return button
    }

    private fun createButtons() {
        val flashingIds = buttons.filter { btn ->
            btn.flashTimer?.isFlashingInProgress == true
        }.map { it.commandId.uuid }.toSet()

        for (button in buttons) {
            buttonRemoveSignal.forEach { it(button) }
            buttonPanel?.removeChild(button)
        }
        buttons.clear()
        buttonMap.clear()
        rightMouseTargetButton = null

        for (id in buttonCommands) {
            val button = createButton(id)
            buttons.add(button)
            buttonPanel?.addChild(button)
            buttonMap[id.uuid] = button
            buttonAddSignal.forEach { it(button) }
            if (id.uuid in flashingIds) button.setFlashing(true)
        }
        needsLayout = true
    }

    private fun createContextMenu(x: Int, y: Int) {
        TODO("GPU: create and show context menu for toolbar at ($x,$y)")
    }

    private fun isSettingChecked(setting: String): Boolean = when (setting) {
        "icons_with_text" -> buttonType == ButtonType.ICONS_WITH_TEXT
        "icons_only"      -> buttonType == ButtonType.ICONS_ONLY
        "text_only"       -> buttonType == ButtonType.TEXT_ONLY
        else              -> false
    }

    private fun onSettingEnable(setting: String) {
        check(!readOnly)
        when (setting) {
            "icons_with_text" -> setButtonType(ButtonType.ICONS_WITH_TEXT)
            "icons_only"      -> setButtonType(ButtonType.ICONS_ONLY)
            "text_only"       -> setButtonType(ButtonType.TEXT_ONLY)
        }
    }

    private fun onRemoveSelectedCommand() {
        check(!readOnly)
        rightMouseTargetButton?.let {
            removeCommand(it.commandId)
            rightMouseTargetButton = null
        }
    }

    fun isAlignment(value: String): Boolean = when (value) {
        "center"           -> alignment == Alignment.CENTER
        "left",  "top"     -> alignment == Alignment.START
        "right", "bottom"  -> alignment == Alignment.END
        else               -> false
    }

    fun onAlignmentChanged(value: String) {
        when (value) {
            "center"           -> setAlignment(Alignment.CENTER)
            "left",  "top"     -> setAlignment(Alignment.START)
            "right", "bottom"  -> setAlignment(Alignment.END)
        }
    }

    fun setAlignment(a: Alignment) { alignment = a; needsLayout = true }
    fun getAlignment(): Alignment = alignment

    fun isLayoutStyle(value: String): Boolean = when (value) {
        "none"     -> layoutStyle == LayoutStyle.NONE
        "equalize" -> layoutStyle == LayoutStyle.EQUALIZE
        "fill"     -> layoutStyle == LayoutStyle.FILL
        else       -> false
    }

    fun onLayoutStyleChanged(value: String) {
        when (value) {
            "none"     -> setLayoutStyle(LayoutStyle.NONE)
            "equalize" -> setLayoutStyle(LayoutStyle.EQUALIZE)
            "fill"     -> setLayoutStyle(LayoutStyle.FILL)
        }
    }

    fun setLayoutStyle(ls: LayoutStyle) { layoutStyle = ls; needsLayout = true }
    fun getLayoutStyle(): LayoutStyle = layoutStyle
}

// --- Vertical toolbar subclass (separate widget tag) ---

open class ToolbarVertical(params: Toolbar.Params = Toolbar.Params()) : Toolbar(params)
