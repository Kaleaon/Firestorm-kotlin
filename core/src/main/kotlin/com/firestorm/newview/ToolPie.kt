package com.firestorm.newview

// Click-action byte constants from indra_constants / llclickaction.h
object ClickAction {
    const val NONE: UByte = 0u
    const val TOUCH: UByte = 0u
    const val SIT: UByte = 1u
    const val BUY: UByte = 2u
    const val PAY: UByte = 3u
    const val OPEN: UByte = 4u
    const val PLAY: UByte = 5u
    const val OPEN_MEDIA: UByte = 6u
    const val ZOOM: UByte = 7u
    const val DISABLED: UByte = 255u
}

// Pick type – mirrors LLPickInfo::EPickType
enum class PickType { LAND, OBJECT, PARCEL_WALL, INVALID }

// Lightweight pick-result model; full implementation lives in the viewer platform layer.
data class PickInfo(
    val mouseX: Int = 0,
    val mouseY: Int = 0,
    val keyMask: Int = 0,
    val pickType: PickType = PickType.INVALID,
    val objectId: String = "",
    val objectFace: Int = -1,
    val hudIcon: Any? = null,
) {
    fun getObject(): ViewerObjectStub? = null
    fun isPosGlobalZero(): Boolean = false
    fun isValid(): Boolean = pickType != PickType.INVALID
}

// Minimal stub so ToolPie compiles without the full object hierarchy.
open class ViewerObjectStub {
    open fun isHUDAttachment(): Boolean = false
    open fun isAttachment(): Boolean = false
    open fun isAvatar(): Boolean = false
    open fun isAgentAvatar(): Boolean = false
    open fun isSelf(): Boolean = false
    open fun flagHandleTouch(): Boolean = false
    open fun flagUsePhysics(): Boolean = false
    open fun flagTakesMoney(): Boolean = false
    open fun allowOpen(): Boolean = false
    open fun getClickAction(): UByte = ClickAction.NONE
    open fun permYouOwner(): Boolean = false
    open fun getRootEdit(): ViewerObjectStub? = null
    open fun getParent(): ViewerObjectStub? = null
    open fun getID(): String = ""
    open fun getAvatar(): ViewerObjectStub? = null
}

private const val CAMERA_MODE_MOUSELOOK = 3

object ToolPie : Tool("Pie") {

    // Firestorm-specific: control which kinds of media objects get first-click without the user
    // having opted in, expressed as a bitmask stored in a viewer setting.
    enum class MediaFirstClickType(val bits: Int) {
        NONE(0),
        HUD(1 shl 0),
        OWN(1 shl 1),
        FRIEND(1 shl 2),
        GROUP(1 shl 3),
        LAND(1 shl 4),
        ANY((1 shl 15) - 1),
        BYPASS_MOAP_FLAG(1 shl 15),
    }

    private var mouseButtonDown: Boolean = false
    private var mouseOutsideSlop: Boolean = false
    private var mouseDownX: Int = 0
    private var mouseDownY: Int = 0
    private var mouseSteerX: Int = -1
    private var mouseSteerY: Int = -1
    private var autoPilotDestination: Any? = null
    private var mouseSteerGrabPoint: Any? = null
    @Suppress("unused") private var clockwise: Boolean = false
    @Suppress("unused") private var mediaMouseCaptureId: String = ""
    var pick: PickInfo = PickInfo()
        private set
    var hoverPick: PickInfo = PickInfo()
        private set
    private var steerPick: PickInfo = PickInfo()
    private var clickActionObject: ViewerObjectStub? = null
    private var clickAction: UByte = ClickAction.NONE
    private var leftClickSelection: Any? = null
    private var clickActionBuyEnabled: Boolean = true
    private var clickActionPayEnabled: Boolean = true
    private var doubleClickTimerStarted: Boolean = false

    fun getClickAction(): UByte = clickAction
    fun getClickActionObject(): ViewerObjectStub? = clickActionObject
    fun getLeftClickSelection(): Any? = leftClickSelection

    override fun handleAnyMouseClick(x: Int, y: Int, mask: Int, clickType: Int, down: Boolean): Boolean {
        // ToolPie deliberately skips LLTool's keyboard-focus reset;
        // focus is handled in the pick callback instead.
        return super.handleAnyMouseClick(x, y, mask, clickType, down)
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (doubleClickTimerStarted) doubleClickTimerStarted = false

        mouseOutsideSlop = false
        mouseDownX = x
        mouseDownY = y

        val transparentPick: PickInfo = PickInfo()
        val visiblePick: PickInfo = PickInfo()
        val transpObj = transparentPick.getObject()
        val visibleObj = visiblePick.getObject()

        // Priority order: transparent attachment > transparent actionable >
        //                 visible attachment > visible actionable > default transparent.
        pick = when {
            transpObj == visibleObj || visibleObj == null || transpObj == null -> transparentPick
            transpObj.isAttachment() -> transparentPick
            transpObj.getClickAction() != ClickAction.DISABLED &&
                    (useClickAction(mask, transpObj, transpObj.getRootEdit()) ||
                            transpObj.flagHandleTouch() ||
                            transpObj.getRootEdit()?.flagHandleTouch() == true) -> transparentPick
            visibleObj.isAttachment() -> visiblePick
            visibleObj.getClickAction() != ClickAction.DISABLED &&
                    (useClickAction(mask, visibleObj, visibleObj.getRootEdit()) ||
                            visibleObj.flagHandleTouch() ||
                            visibleObj.getRootEdit()?.flagHandleTouch() == true) -> visiblePick
            else -> transparentPick
        }
        pick = pick.copy(keyMask = mask)
        mouseButtonDown = true
        return handleLeftClickPick()
    }

    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val pickReflectionProbe: Boolean = false
        pick = PickInfo().copy(keyMask = mask)

        val cameraMode: Int = 0
        val enableInMouselook: Boolean = false
        if (cameraMode != CAMERA_MODE_MOUSELOOK || enableInMouselook) {
            handleRightClickPick()
        }
        return false
    }

    override fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean {
        ToolMgr.clearTransientTool()
        return super.handleRightMouseUp(x, y, mask)
    }

    private fun handleScrollWheelAny(x: Int, y: Int, clicksX: Int, clicksY: Int): Boolean {
        val uvX: Float = -1f
        val uvY: Float = -1f
        return if (uvX >= 0f && uvY >= 0f) {
            false
        } else {
            false
        }
    }

    override fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean =
        handleScrollWheelAny(x, y, 0, clicks)

    override fun handleScrollHWheel(x: Int, y: Int, clicks: Int): Boolean =
        handleScrollWheelAny(x, y, clicks, 0)

    private fun handleLeftClickPick(): Boolean {
        val x = pick.mouseX
        val y = pick.mouseY
        val mask = pick.keyMask

        if (pick.pickType == PickType.PARCEL_WALL) {
            System.err.println("ToolPie: show parcel info or buy pass not yet implemented")
            return super.handleMouseDown(x, y, mask)
        }

        val obj = pick.getObject()
        var parent: ViewerObjectStub? = null

        if (pick.pickType != PickType.LAND) {
            System.err.println("ToolPie: ViewerParcelMgr.getInstance().deselectLand() not yet implemented")
        }
        parent = obj?.getRootEdit()

        if (handleMediaClick(pick)) return true

        if (useClickAction(mask, obj, parent)) {
            System.err.println("ToolPie: RlvActions interaction checks not yet implemented")

            val objAction = obj?.getClickAction() ?: ClickAction.NONE
            val parentAction = parent?.getClickAction() ?: ClickAction.NONE
            clickAction = when {
                objAction != ClickAction.NONE -> objAction
                parentAction != ClickAction.NONE -> parentAction
                else -> ClickAction.NONE
            }
            System.err.println("ToolPie: RlvActions per-action buy/pay checks, clear clickAction if blocked not yet implemented")

            when (clickAction) {
                ClickAction.TOUCH -> { /* fall through to touch handling */ }
                ClickAction.SIT -> {
                    val blockSit: Boolean = false
                    if (!blockSit && false) {
                        System.err.println("ToolPie: handle_object_sit_or_stand(); gFocusMgr.setKeyboardFocus(null) not yet implemented")
                        return true
                    }
                }
                ClickAction.PAY -> {
                    if (clickActionPayEnabled &&
                        (obj?.flagTakesMoney() == true || parent?.flagTakesMoney() == true)
                    ) {
                        clickActionObject = obj
                        leftClickSelection = null
                        if (false)
                            selectionPropertiesReceived()
                        return true
                    }
                }
                ClickAction.BUY -> {
                    if (clickActionBuyEnabled) {
                        clickActionObject = parent
                        leftClickSelection = null
                        if (false)
                            selectionPropertiesReceived()
                        return true
                    }
                }
                ClickAction.OPEN -> {
                    if (parent?.allowOpen() == true) {
                        clickActionObject = parent
                        leftClickSelection = null
                        if (false)
                            selectionPropertiesReceived()
                    }
                    return true
                }
                ClickAction.PLAY -> { System.err.println("ToolPie: handle_click_action_play() not yet implemented"); return true }
                ClickAction.OPEN_MEDIA -> { System.err.println("ToolPie: handle_click_action_open_media(obj) not yet implemented"); return true }
                ClickAction.ZOOM -> { System.err.println("ToolPie: zoom camera to object bounding box not yet implemented"); return true }
                ClickAction.DISABLED -> return true
                else -> { /* nothing */ }
            }
        }

        System.err.println("ToolPie: gFocusMgr.setKeyboardFocus(null) if currently focused not yet implemented")

        val touchable = obj != null &&
                obj.getClickAction() != ClickAction.DISABLED &&
                (obj.flagHandleTouch() || parent?.flagHandleTouch() == true)

        if (obj != null && !obj.isAvatar() &&
            (obj.flagUsePhysics() ||
                    (parent != null && !parent.isAvatar() && parent.flagUsePhysics()) ||
                    touchable)
        ) {
            System.err.println("ToolPie: RlvActions.canTouch check; switch to ToolGrab and forward hit not yet implemented")
            mouseButtonDown = false
            return true
        }

        val lastHitHudIcon = pick.hudIcon
        if (obj == null && lastHitHudIcon != null) {
            System.err.println("ToolPie: FloaterScriptDebug.show(hudIcon.getSourceObject().getID()) if script error not yet implemented")
        }

        if (!mouseButtonDown) return true

        // Walk up attachment chain toward the avatar root.
        var walkObj = obj
        while (walkObj != null && walkObj.isAttachment() && !walkObj.flagHandleTouch()) {
            if (walkObj.isHUDAttachment()) break
            walkObj = walkObj.getParent()
        }
        if (walkObj?.isAgentAvatar() == true) {
            mouseButtonDown = false
            ToolMgr.setTransientTool(ToolCamera)
            System.err.println("ToolPie: gViewerWindow.hideCursor(); ToolCamera.setMouseCapture(true); ToolCamera.setClickPickPending(); ToolCamera.pickCallback(pick) not yet implemented")
            System.err.println("ToolPie: gAgentCamera.setFocusOnAvatar(true, true) unless ClickOnAvatarKeepsCamera not yet implemented")
            return true
        }

        return super.handleMouseDown(x, y, mask)
    }

    private fun handleRightClickPick(): Boolean {
        System.err.println("ToolPie: build and show pie/context menu for the right-click pick not yet implemented")
        return false
    }

    private fun useClickAction(mask: Int, obj: ViewerObjectStub?, parent: ViewerObjectStub?): Boolean {
        if (mask != MASK_NONE || obj == null || obj.isAttachment()) return false
        val objAction = obj.getClickAction()
        val parentAction = parent?.getClickAction() ?: ClickAction.NONE
        return (objAction != ClickAction.NONE && objAction != ClickAction.DISABLED) ||
                (parentAction != ClickAction.NONE && parentAction != ClickAction.DISABLED)
    }

    private fun finalClickAction(obj: ViewerObjectStub?): UByte {
        if (obj == null || obj.isAttachment()) return ClickAction.NONE
        val parent = obj.getRootEdit()
        val objectAction = obj.getClickAction()
        val parentAction = parent?.getClickAction() ?: ClickAction.TOUCH
        return when {
            parentAction == ClickAction.DISABLED || objectAction != ClickAction.NONE -> objectAction
            parentAction != ClickAction.NONE -> parentAction
            else -> ClickAction.TOUCH
        }
    }

    private fun cursorFromObject(obj: ViewerObjectStub?): Int {
        val parent = obj?.getRootEdit()
        return when (finalClickAction(obj)) {
            ClickAction.SIT -> 0
            ClickAction.BUY -> if (clickActionBuyEnabled) 0
            else 0
            ClickAction.OPEN -> if (parent?.allowOpen() == true) 0
            else 0
            ClickAction.PAY -> if (clickActionPayEnabled &&
                (obj?.flagTakesMoney() == true || parent?.flagTakesMoney() == true)
            ) 0
            else 0
            ClickAction.ZOOM -> 0
            ClickAction.PLAY, ClickAction.OPEN_MEDIA -> 0
            else -> 0
        }
    }

    fun resetSelection() {
        leftClickSelection = null
        clickActionObject = null
        clickAction = ClickAction.NONE
    }

    fun walkToClickedLocation(): Boolean {
        val flying: Boolean = false
        val sitting: Boolean = false
        if (flying || sitting) return false

        System.err.println("ToolPie: pickImmediate for the hover position, start autopilot, spawn HUD blob effect not yet implemented")
        return true
    }

    fun teleportToClickedLocation(): Boolean {
        System.err.println("ToolPie: pickImmediate and call gAgent.teleportViaLocationLookAt not yet implemented")
        return false
    }

    fun stopClickToWalk() {
        System.err.println("ToolPie: reset mPick.mPosGlobal to agent position, call handle_go_to(), markDead autopilot blob not yet implemented")
    }

    companion object {
        fun selectionPropertiesReceived() {
            if (!false) return
            val selection = ToolPie.getLeftClickSelection() ?: return
            val selectedObj: ViewerObjectStub? = null
            if (selectedObj === ToolPie.getClickActionObject()) {
                when (ToolPie.getClickAction()) {
                    ClickAction.BUY -> if (ToolPie.clickActionBuyEnabled) System.err.println("ToolPie: handle_buy() not yet implemented")
                    ClickAction.PAY -> if (ToolPie.clickActionPayEnabled) System.err.println("ToolPie: handle_give_money_dialog() not yet implemented")
                    ClickAction.OPEN -> System.err.println("ToolPie: FloaterReg.showInstance(\"openobject\") not yet implemented")
                    else -> { /* nothing */ }
                }
            }
            ToolPie.resetSelection()
        }

        fun showAvatarInspector(avatarId: String) {
            System.err.println("ToolPie: FloaterReg.showInstance(\"inspect_avatar\", avatarId) not yet implemented")
        }

        fun showObjectInspector(objectId: String, objectFace: Int = -1) {
            System.err.println("ToolPie: FloaterReg.showInstance(\"inspect_object\", objectId, objectFace) not yet implemented")
        }

        fun playCurrentMedia(info: PickInfo) {
            System.err.println("ToolPie: ViewerParcelMedia.play(info) not yet implemented")
        }

        fun visitHomePage(info: PickInfo) {
            System.err.println("ToolPie: open parcel home-page URL in browser not yet implemented")
        }
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (!doubleClickTimerStarted) {
            doubleClickTimerStarted = true
        } else {
            doubleClickTimerStarted = false
        }

        stopCameraSteering()
        mouseButtonDown = false

        System.err.println("ToolPie: gViewerWindow.setCursor(UI_CURSOR_ARROW) not yet implemented")
        if (hasMouseCapture()) setMouseCapture(false)

        ToolMgr.clearTransientTool()
        System.err.println("ToolPie: gAgentCamera.setLookAt(LOOKAT_TARGET_CONVERSATION, pick.getObject()) not yet implemented")
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        if (handleMediaDblClick(pick)) return true

        val canDoubleClickTp: Boolean = false
        val allowOnScripted: Boolean = false
        if (canDoubleClickTp && allowOnScripted) {
            doubleClickTimerStarted = false
            teleportToClickedLocation()
            return true
        }
        doubleClickTimerStarted = false
        return false
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        // Unhighlight previous hover before fetching new pick.
        if (hoverPick.isValid()) {
            hoverPick.getObject()?.getRootEdit()?.let {
                System.err.println("ToolPie: it.setTextIsHighlighted(false) not yet implemented")
            }
        }

        hoverPick = PickInfo()
        val obj = hoverPick.getObject()
        val parent = obj?.getRootEdit()

        System.err.println("ToolPie: SelectMgr.getInstance().setHoverObject(obj, hoverPick.objectFace) not yet implemented")

        if (!handleMediaHover(hoverPick) &&
            !mouseOutsideSlop &&
            mouseButtonDown &&
            false
        ) {
            val dx = x - mouseDownX
            val dy = y - mouseDownY
            val threshold = DRAG_N_DROP_DISTANCE_THRESHOLD
            if (dx * dx + dy * dy > threshold * threshold) {
                startCameraSteering()
                steerCameraWithMouse(x, y)
                System.err.println("ToolPie: gViewerWindow.setCursor(UI_CURSOR_TOOLGRAB) not yet implemented")
            } else {
                System.err.println("ToolPie: gViewerWindow.setCursor(UI_CURSOR_ARROW) not yet implemented")
            }
        } else if (inCameraSteerMode()) {
            steerCameraWithMouse(x, y)
            System.err.println("ToolPie: gViewerWindow.setCursor(UI_CURSOR_TOOLGRAB) not yet implemented")
        } else {
            val clickActionPick: PickInfo = PickInfo()
            val clickActionObj = clickActionPick.getObject()
            when {
                clickActionObj != null &&
                        useClickAction(mask, clickActionObj, clickActionObj.getRootEdit()) -> {
                    val cursor = cursorFromObject(clickActionObj)
                    System.err.println("ToolPie: gViewerWindow.setCursor(cursor) not yet implemented")
                }
                obj != null && !obj.isAvatar() && obj.flagUsePhysics() ||
                        parent != null && !parent.isAvatar() && parent.flagUsePhysics() ->
                    System.err.println("ToolPie: gViewerWindow.setCursor(UI_CURSOR_TOOLGRAB) not yet implemented")
                obj?.getClickAction() != ClickAction.DISABLED &&
                        (obj?.flagHandleTouch() == true || parent?.flagHandleTouch() == true) &&
                        obj?.isAvatar() != true ->
                    System.err.println("ToolPie: gViewerWindow.setCursor(UI_CURSOR_HAND) not yet implemented")
                else -> System.err.println("ToolPie: gViewerWindow.setCursor(UI_CURSOR_ARROW) not yet implemented")
            }
        }

        if (obj == null) {
            System.err.println("ToolPie: ViewerMediaFocus.getInstance().clearHover() not yet implemented")
        } else {
            parent?.let { System.err.println("ToolPie: it.setTextIsHighlighted(true) not yet implemented") }
        }
        return true
    }

    override fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("ToolPie: show hover-tip for land or object under cursor via LLToolTipMgr not yet implemented")
        return false
    }

    override fun render() {
        // GPU: render ToolPie visual indicators (autopilot destination blob, steering point)
    }

    override fun stopEditing() {
        System.err.println("ToolPie: deactivate any in-progress editing state not yet implemented")
    }

    override fun onMouseCaptureLost() {
        System.err.println("ToolPie: clean up mouse-capture state not yet implemented")
    }

    override fun handleSelect() {
        System.err.println("ToolPie: gFocusMgr.setKeyboardFocus(null) not yet implemented")
    }

    override fun handleDeselect() {
        resetSelection()
    }

    override fun getOverrideTool(mask: Int): Tool? {
        System.err.println("ToolPie: return ToolGrab or ToolCamera depending on modifier mask not yet implemented")
        return null
    }

    // ---- Camera-steering helpers --------------------------------------------

    private fun startCameraSteering() {
        mouseOutsideSlop = true
        steerPick = hoverPick
        mouseSteerX = mouseDownX
        mouseSteerY = mouseDownY
        System.err.println("ToolPie: capture mouse; create mouseSteerGrabPoint HUD effect not yet implemented")
    }

    private fun stopCameraSteering() {
        if (inCameraSteerMode()) {
            mouseSteerX = -1
            mouseSteerY = -1
            System.err.println("ToolPie: release mouse capture for steering; destroy mouseSteerGrabPoint HUD effect not yet implemented")
        }
    }

    private fun inCameraSteerMode(): Boolean = mouseSteerX != -1

    private fun steerCameraWithMouse(x: Int, y: Int) {
        System.err.println("ToolPie: gAgentCamera steering based on delta from (mouseSteerX, mouseSteerY) not yet implemented")
    }

    private fun showVisualContextMenuEffect() {
        System.err.println("ToolPie: spawn HUD blob effect at pick position for click feedback not yet implemented")
    }

    // ---- Media helpers (require ViewerMedia / ViewerMediaFocus subsystem) ---

    private fun handleMediaClick(info: PickInfo): Boolean = false

    private fun handleMediaDblClick(info: PickInfo): Boolean = false

    private fun handleMediaHover(info: PickInfo): Boolean = false

    @Suppress("unused")
    private fun handleMediaMouseUp(): Boolean = false

    // ---- Constants ----------------------------------------------------------
    private val DRAG_N_DROP_DISTANCE_THRESHOLD = 3
}
