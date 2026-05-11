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
    fun getObject(): ViewerObjectStub? = TODO("APR: gObjectList.findObject(objectId)")
    fun isPosGlobalZero(): Boolean = TODO("APR: mPosGlobal.isExactlyZero()") as Boolean
    fun isValid(): Boolean = pickType != PickType.INVALID
}

// Minimal stub so ToolPie compiles without the full object hierarchy.
open class ViewerObjectStub {
    open fun isHUDAttachment(): Boolean = TODO("APR: object flag")
    open fun isAttachment(): Boolean = TODO("APR: object flag")
    open fun isAvatar(): Boolean = TODO("APR: object flag")
    open fun isAgentAvatar(): Boolean = TODO("APR: object flag")
    open fun isSelf(): Boolean = TODO("APR: object flag")
    open fun flagHandleTouch(): Boolean = TODO("APR: object flag")
    open fun flagUsePhysics(): Boolean = TODO("APR: object flag")
    open fun flagTakesMoney(): Boolean = TODO("APR: object flag")
    open fun allowOpen(): Boolean = TODO("APR: permission check")
    open fun getClickAction(): UByte = TODO("APR: prim property") as UByte
    open fun permYouOwner(): Boolean = TODO("APR: permission check")
    open fun getRootEdit(): ViewerObjectStub? = TODO("APR: walk to link root")
    open fun getParent(): ViewerObjectStub? = TODO("APR: scene-graph parent")
    open fun getID(): String = TODO("APR: UUID as string")
    open fun getAvatar(): ViewerObjectStub? = TODO("APR: avatar link")
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

        val transparentPick: PickInfo =
            TODO("APR: gViewerWindow.pickImmediate(x, y, includeTransparent=true, rigged=false, particle=false, unselectable=true)") as PickInfo
        val visiblePick: PickInfo =
            TODO("APR: gViewerWindow.pickImmediate(x, y, includeTransparent=false, rigged=false)") as PickInfo
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
        val pickReflectionProbe: Boolean =
            TODO("APR: gSavedSettings.getBOOL(\"SelectReflectionProbes\")") as Boolean
        pick = (TODO("APR: gViewerWindow.pickImmediate(x, y, transparent=FSEnableRightclickOnTransparentObjects, rigged=true, particle=true, unselectable=true, reflectionProbe=pickReflectionProbe)") as PickInfo)
            .copy(keyMask = mask)

        val cameraMode: Int = TODO("APR: gAgentCamera.getCameraMode()") as Int
        val enableInMouselook: Boolean =
            TODO("APR: gSavedSettings.getBOOL(\"FSEnableRightclickMenuInMouselook\")") as Boolean
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
        val uvX: Float = TODO("APR: hoverPick.uvCoords.x") as Float
        val uvY: Float = TODO("APR: hoverPick.uvCoords.y") as Float
        return if (uvX >= 0f && uvY >= 0f) {
            TODO("APR: ViewerMediaFocus.getInstance().handleScrollWheel(hoverPick.uvCoords, clicksX, clicksY)") as Boolean
        } else {
            TODO("APR: ViewerMediaFocus.getInstance().handleScrollWheel(x, y, clicksX, clicksY)") as Boolean
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
            TODO("APR: show parcel info or buy pass")
            return super.handleMouseDown(x, y, mask)
        }

        val obj = pick.getObject()
        var parent: ViewerObjectStub? = null

        if (pick.pickType != PickType.LAND) {
            TODO("APR: ViewerParcelMgr.getInstance().deselectLand()")
        }
        parent = obj?.getRootEdit()

        if (handleMediaClick(pick)) return true

        if (useClickAction(mask, obj, parent)) {
            TODO("APR: RlvActions interaction checks")

            val objAction = obj?.getClickAction() ?: ClickAction.NONE
            val parentAction = parent?.getClickAction() ?: ClickAction.NONE
            clickAction = when {
                objAction != ClickAction.NONE -> objAction
                parentAction != ClickAction.NONE -> parentAction
                else -> ClickAction.NONE
            }
            TODO("APR: RlvActions per-action buy/pay checks, clear clickAction if blocked")

            when (clickAction) {
                ClickAction.TOUCH -> { /* fall through to touch handling */ }
                ClickAction.SIT -> {
                    val blockSit: Boolean =
                        TODO("APR: gSavedSettings.getBOOL(\"FSBlockClickSit\")") as Boolean
                    if (!blockSit && TODO("APR: isAgentAvatarValid() && !gAgentAvatarp.isSitting()") as Boolean) {
                        TODO("APR: handle_object_sit_or_stand(); gFocusMgr.setKeyboardFocus(null)")
                        return true
                    }
                }
                ClickAction.PAY -> {
                    if (clickActionPayEnabled &&
                        (obj?.flagTakesMoney() == true || parent?.flagTakesMoney() == true)
                    ) {
                        clickActionObject = obj
                        leftClickSelection = TODO("APR: ToolSelect.handleObjectSelection(pick, false, true)")
                        if (TODO("APR: SelectMgr.getInstance().selectGetAllValid()") as Boolean)
                            selectionPropertiesReceived()
                        return true
                    }
                }
                ClickAction.BUY -> {
                    if (clickActionBuyEnabled) {
                        clickActionObject = parent
                        leftClickSelection = TODO("APR: ToolSelect.handleObjectSelection(pick, false, true, true)")
                        if (TODO("APR: SelectMgr.getInstance().selectGetAllValid()") as Boolean)
                            selectionPropertiesReceived()
                        return true
                    }
                }
                ClickAction.OPEN -> {
                    if (parent?.allowOpen() == true) {
                        clickActionObject = parent
                        leftClickSelection = TODO("APR: ToolSelect.handleObjectSelection(pick, false, true, true)")
                        if (TODO("APR: SelectMgr.getInstance().selectGetAllValid()") as Boolean)
                            selectionPropertiesReceived()
                    }
                    return true
                }
                ClickAction.PLAY -> { TODO("APR: handle_click_action_play()"); return true }
                ClickAction.OPEN_MEDIA -> { TODO("APR: handle_click_action_open_media(obj)"); return true }
                ClickAction.ZOOM -> { TODO("APR: zoom camera to object bounding box"); return true }
                ClickAction.DISABLED -> return true
                else -> { /* nothing */ }
            }
        }

        TODO("APR: gFocusMgr.setKeyboardFocus(null) if currently focused")

        val touchable = obj != null &&
                obj.getClickAction() != ClickAction.DISABLED &&
                (obj.flagHandleTouch() || parent?.flagHandleTouch() == true)

        if (obj != null && !obj.isAvatar() &&
            (obj.flagUsePhysics() ||
                    (parent != null && !parent.isAvatar() && parent.flagUsePhysics()) ||
                    touchable)
        ) {
            TODO("APR: RlvActions.canTouch check; switch to ToolGrab and forward hit")
            mouseButtonDown = false
            return true
        }

        val lastHitHudIcon = pick.hudIcon
        if (obj == null && lastHitHudIcon != null) {
            TODO("APR: FloaterScriptDebug.show(hudIcon.getSourceObject().getID()) if script error")
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
            TODO("APR: gViewerWindow.hideCursor(); ToolCamera.setMouseCapture(true); ToolCamera.setClickPickPending(); ToolCamera.pickCallback(pick)")
            TODO("APR: gAgentCamera.setFocusOnAvatar(true, true) unless ClickOnAvatarKeepsCamera")
            return true
        }

        return super.handleMouseDown(x, y, mask)
    }

    private fun handleRightClickPick(): Boolean {
        TODO("APR: build and show pie/context menu for the right-click pick")
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
            ClickAction.SIT -> TODO("APR: UI_CURSOR_TOOLSIT if avatar not sitting and RLVa allows") as Int
            ClickAction.BUY -> if (clickActionBuyEnabled) TODO("APR: UI_CURSOR_TOOLBUY") as Int
            else TODO("APR: UI_CURSOR_ARROW") as Int
            ClickAction.OPEN -> if (parent?.allowOpen() == true) TODO("APR: UI_CURSOR_TOOLOPEN") as Int
            else TODO("APR: UI_CURSOR_ARROW") as Int
            ClickAction.PAY -> if (clickActionPayEnabled &&
                (obj?.flagTakesMoney() == true || parent?.flagTakesMoney() == true)
            ) TODO("APR: UI_CURSOR_TOOLPAY") as Int
            else TODO("APR: UI_CURSOR_ARROW") as Int
            ClickAction.ZOOM -> TODO("APR: UI_CURSOR_TOOLZOOMIN") as Int
            ClickAction.PLAY, ClickAction.OPEN_MEDIA ->
                TODO("APR: cursor_from_parcel_media(clickAction)") as Int
            else -> TODO("APR: UI_CURSOR_ARROW") as Int
        }
    }

    fun resetSelection() {
        leftClickSelection = null
        clickActionObject = null
        clickAction = ClickAction.NONE
    }

    fun walkToClickedLocation(): Boolean {
        val flying: Boolean = TODO("APR: gAgent.getFlying()") as Boolean
        val sitting: Boolean = TODO("APR: gAgentAvatarp?.isSitting() ?: false") as Boolean
        if (flying || sitting) return false

        TODO("APR: pickImmediate for the hover position, start autopilot, spawn HUD blob effect")
        return true
    }

    fun teleportToClickedLocation(): Boolean {
        TODO("APR: pickImmediate and call gAgent.teleportViaLocationLookAt")
        return false
    }

    fun stopClickToWalk() {
        TODO("APR: reset mPick.mPosGlobal to agent position, call handle_go_to(), markDead autopilot blob")
    }

    companion object {
        fun selectionPropertiesReceived() {
            if (TODO("APR: !SelectMgr.getInstance().selectGetAllValid()") as Boolean) return
            val selection = ToolPie.getLeftClickSelection() ?: return
            val selectedObj: ViewerObjectStub? = TODO("APR: selection.getPrimaryObject()") as ViewerObjectStub?
            if (selectedObj === ToolPie.getClickActionObject()) {
                when (ToolPie.getClickAction()) {
                    ClickAction.BUY -> if (ToolPie.clickActionBuyEnabled) TODO("APR: handle_buy()")
                    ClickAction.PAY -> if (ToolPie.clickActionPayEnabled) TODO("APR: handle_give_money_dialog()")
                    ClickAction.OPEN -> TODO("APR: FloaterReg.showInstance(\"openobject\")")
                    else -> { /* nothing */ }
                }
            }
            ToolPie.resetSelection()
        }

        fun showAvatarInspector(avatarId: String) {
            TODO("APR: FloaterReg.showInstance(\"inspect_avatar\", avatarId)")
        }

        fun showObjectInspector(objectId: String, objectFace: Int = -1) {
            TODO("APR: FloaterReg.showInstance(\"inspect_object\", objectId, objectFace)")
        }

        fun playCurrentMedia(info: PickInfo) {
            TODO("APR: ViewerParcelMedia.play(info)")
        }

        fun visitHomePage(info: PickInfo) {
            TODO("APR: open parcel home-page URL in browser")
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

        TODO("APR: gViewerWindow.setCursor(UI_CURSOR_ARROW)")
        if (hasMouseCapture()) setMouseCapture(false)

        ToolMgr.clearTransientTool()
        TODO("APR: gAgentCamera.setLookAt(LOOKAT_TARGET_CONVERSATION, pick.getObject())")
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        if (handleMediaDblClick(pick)) return true

        val canDoubleClickTp: Boolean =
            TODO("APR: gSavedSettings.getBOOL(\"DoubleClickTeleport\")") as Boolean
        val allowOnScripted: Boolean =
            TODO("APR: gSavedSettings.getBOOL(\"FSAllowDoubleClickOnScriptedObjects\")") as Boolean
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
                TODO("APR: it.setTextIsHighlighted(false)")
            }
        }

        hoverPick = TODO("APR: gViewerWindow.pickImmediate(x, y, transparent=false, rigged=false)") as PickInfo
        val obj = hoverPick.getObject()
        val parent = obj?.getRootEdit()

        TODO("APR: SelectMgr.getInstance().setHoverObject(obj, hoverPick.objectFace)")

        if (!handleMediaHover(hoverPick) &&
            !mouseOutsideSlop &&
            mouseButtonDown &&
            TODO("APR: gViewerInput.isMouseBindUsed(CLICK_LEFT, MASK_NONE, MODE_THIRD_PERSON)") as Boolean
        ) {
            val dx = x - mouseDownX
            val dy = y - mouseDownY
            val threshold = DRAG_N_DROP_DISTANCE_THRESHOLD
            if (dx * dx + dy * dy > threshold * threshold) {
                startCameraSteering()
                steerCameraWithMouse(x, y)
                TODO("APR: gViewerWindow.setCursor(UI_CURSOR_TOOLGRAB)")
            } else {
                TODO("APR: gViewerWindow.setCursor(UI_CURSOR_ARROW)")
            }
        } else if (inCameraSteerMode()) {
            steerCameraWithMouse(x, y)
            TODO("APR: gViewerWindow.setCursor(UI_CURSOR_TOOLGRAB)")
        } else {
            val clickActionPick: PickInfo =
                TODO("APR: gViewerWindow.pickImmediate(x, y, false, false)") as PickInfo
            val clickActionObj = clickActionPick.getObject()
            when {
                clickActionObj != null &&
                        useClickAction(mask, clickActionObj, clickActionObj.getRootEdit()) -> {
                    val cursor = cursorFromObject(clickActionObj)
                    TODO("APR: gViewerWindow.setCursor(cursor)")
                }
                obj != null && !obj.isAvatar() && obj.flagUsePhysics() ||
                        parent != null && !parent.isAvatar() && parent.flagUsePhysics() ->
                    TODO("APR: gViewerWindow.setCursor(UI_CURSOR_TOOLGRAB)")
                obj?.getClickAction() != ClickAction.DISABLED &&
                        (obj?.flagHandleTouch() == true || parent?.flagHandleTouch() == true) &&
                        obj?.isAvatar() != true ->
                    TODO("APR: gViewerWindow.setCursor(UI_CURSOR_HAND)")
                else -> TODO("APR: gViewerWindow.setCursor(UI_CURSOR_ARROW)")
            }
        }

        if (obj == null) {
            TODO("APR: ViewerMediaFocus.getInstance().clearHover()")
        } else {
            parent?.let { TODO("APR: it.setTextIsHighlighted(true)") }
        }
        return true
    }

    override fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        TODO("APR: show hover-tip for land or object under cursor via LLToolTipMgr")
    }

    override fun render() {
        TODO("GPU: render ToolPie visual indicators (autopilot destination blob, steering point)")
    }

    override fun stopEditing() {
        TODO("APR: deactivate any in-progress editing state")
    }

    override fun onMouseCaptureLost() {
        TODO("APR: clean up mouse-capture state")
    }

    override fun handleSelect() {
        TODO("APR: gFocusMgr.setKeyboardFocus(null)")
    }

    override fun handleDeselect() {
        resetSelection()
    }

    override fun getOverrideTool(mask: Int): Tool? {
        TODO("APR: return ToolGrab or ToolCamera depending on modifier mask")
    }

    // ---- Camera-steering helpers --------------------------------------------

    private fun startCameraSteering() {
        mouseOutsideSlop = true
        steerPick = hoverPick
        mouseSteerX = mouseDownX
        mouseSteerY = mouseDownY
        TODO("APR: capture mouse; create mouseSteerGrabPoint HUD effect")
    }

    private fun stopCameraSteering() {
        if (inCameraSteerMode()) {
            mouseSteerX = -1
            mouseSteerY = -1
            TODO("APR: release mouse capture for steering; destroy mouseSteerGrabPoint HUD effect")
        }
    }

    private fun inCameraSteerMode(): Boolean = mouseSteerX != -1

    private fun steerCameraWithMouse(x: Int, y: Int) {
        TODO("APR: gAgentCamera steering based on delta from (mouseSteerX, mouseSteerY)")
    }

    private fun showVisualContextMenuEffect() {
        TODO("APR: spawn HUD blob effect at pick position for click feedback")
    }

    // ---- Media helpers (require ViewerMedia / ViewerMediaFocus subsystem) ---

    private fun handleMediaClick(info: PickInfo): Boolean =
        TODO("APR: ViewerMediaFocus click handling") as Boolean

    private fun handleMediaDblClick(info: PickInfo): Boolean =
        TODO("APR: ViewerMediaFocus double-click handling") as Boolean

    private fun handleMediaHover(info: PickInfo): Boolean =
        TODO("APR: ViewerMediaFocus hover handling") as Boolean

    @Suppress("unused")
    private fun handleMediaMouseUp(): Boolean =
        TODO("APR: ViewerMediaFocus mouse-up handling") as Boolean

    // ---- Constants ----------------------------------------------------------
    private val DRAG_N_DROP_DISTANCE_THRESHOLD = 3
}
