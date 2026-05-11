package com.firestorm.newview

import kotlin.math.abs

// Exported globals – reflect which camera-mode button is active in the toolbar.
var gCameraBtnZoom: Boolean = true
var gCameraBtnOrbit: Boolean = false
var gCameraBtnPan: Boolean = false

private const val SLOP_RANGE = 4

// True while the user holds the right mouse button for forward mouse-steering.
private var rightHoldMouseWalk: Boolean = false

private const val CAMERA_MODE_CUSTOMIZE_AVATAR = 4

object ToolCamera : Tool("Camera") {

    private var accumX: Int = 0
    private var accumY: Int = 0
    private var mouseDownX: Int = 0
    private var mouseDownY: Int = 0
    private var outsideSlopX: Boolean = false
    private var outsideSlopY: Boolean = false
    private var validClickPoint: Boolean = false
    private var clickPickPending: Boolean = false
    private var validSelection: Boolean = false
    private var mouseSteering: Boolean = false
    private var mouseUpX: Int = 0
    private var mouseUpY: Int = 0
    private var mouseUpMask: Int = 0

    override fun getOverrideTool(mask: Int): Tool? = null

    override fun handleSelect() {
        TODO("APR: gFloaterTools?.setStatusText(\"camera\"); validSelection = gFloaterTools?.getVisible() ?: false")
    }

    override fun handleDeselect() {
        val overrideMask: Int = TODO("APR: gKeyboard?.currentMask(true) ?: 0") as Int
        // Only clear selection on deselect when there is a keyboard override or tools floater is visible.
        if (!validSelection && (overrideMask != MASK_NONE ||
                    TODO("APR: gFloaterTools?.getVisible() ?: false") as Boolean)
        ) {
            TODO("APR: LLMenuGL.sMenuContainer.hideMenus(); SelectMgr.getInstance().validateSelection()")
        }
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        setMouseCapture(true)
        super.handleMouseDown(x, y, mask)

        accumX = 0
        accumY = 0
        outsideSlopX = false
        outsideSlopY = false
        validClickPoint = false
        clickPickPending = true

        // Cache mouse-down position so releaseMouse() can replay the up-event
        // if capture is lost before the async pick arrives.
        mouseUpX = x
        mouseUpY = y
        mouseUpMask = mask

        TODO("APR: gViewerWindow.hideCursor()")
        TODO("APR: gViewerWindow.pickAsync(x, y, mask, ToolCamera::pickCallback, pickTransparent=false, pickRigged=false, pickUnselectable=true)")
        return true
    }

    fun setClickPickPending() {
        clickPickPending = true
    }

    fun pickCallback(pickInfo: PickInfo) {
        if (!clickPickPending) return
        clickPickPending = false

        mouseDownX = pickInfo.mouseX
        mouseDownY = pickInfo.mouseY

        TODO("APR: gViewerWindow.moveCursorToCenter()")

        val hitObj: ViewerObjectStub? = pickInfo.getObject()

        if (hitObj == null && pickInfo.isPosGlobalZero()) {
            validClickPoint = false
            return
        }

        if (hitObj?.isHUDAttachment() == true) {
            val selection = TODO("APR: SelectMgr.getInstance().getSelection()")
            if (TODO("APR: selection.getObjectCount() == 0 || selection.getSelectType() != SELECT_TYPE_HUD") as Boolean) {
                validClickPoint = false
                return
            }
        }

        val cameraMode: Int = TODO("APR: gAgentCamera.getCameraMode()") as Int
        if (cameraMode == CAMERA_MODE_CUSTOMIZE_AVATAR) {
            val goodHit = hitObj != null &&
                    (hitObj.isAgentAvatar() || (hitObj.isAttachment() && hitObj.permYouOwner()))
            if (!goodHit) { validClickPoint = false; return }
            TODO("APR: gMorphView?.setCameraDrivenByKeys(false)")
        } else if (pickInfo.keyMask and MASK_A != 0 ||
            ToolMgr.getCurrentTool()?.getName() == "Camera"
        ) {
            if (hitObj != null && !hitObj.isHUDAttachment()) {
                TODO("APR: gAgentCamera.setFocusOnAvatar(false, ANIMATE); gAgentCamera.setFocusGlobal(pickInfo)")
            } else if (!pickInfo.isPosGlobalZero()) {
                TODO("APR: gAgentCamera.setFocusOnAvatar(false, ANIMATE); gAgentCamera.setFocusGlobal(pickInfo)")
            }

            val zoomTool = gCameraBtnZoom && ToolMgr.getBaseTool() === ToolCamera
            val freezeTime: Boolean = TODO("APR: gSavedSettings.getBOOL(\"FreezeTime\")") as Boolean
            if (pickInfo.keyMask and MASK_A == 0 &&
                !freezeTime &&
                !zoomTool &&
                TODO("APR: !FloaterCamera.inFreeCameraMode()") as Boolean &&
                TODO("APR: gAgentCamera.cameraThirdPerson()") as Boolean &&
                TODO("APR: gViewerWindow.getLeftMouseDown()") as Boolean &&
                (hitObj?.isAgentAvatar() == true ||
                        (hitObj?.isAttachment() == true && hitObj.isSelf()))
            ) {
                mouseSteering = true
            }
        }

        validClickPoint = true

        if (cameraMode == CAMERA_MODE_CUSTOMIZE_AVATAR) {
            TODO("APR: gAgentCamera.setFocusOnAvatar(false, false); gAgentCamera.setCameraPosAndFocusGlobal(camPos, pickInfo.mPosGlobal, pickInfo.mObjectID)")
        }
    }

    private fun releaseMouse() {
        super.handleMouseUp(mouseUpX, mouseUpY, mouseUpMask)
        TODO("APR: gViewerWindow.showCursor()")
        if (TODO("APR: !FloaterCamera.inFreeCameraMode()") as Boolean) {
            ToolMgr.clearTransientTool()
        }
        mouseSteering = false
        validClickPoint = false
        outsideSlopX = false
        outsideSlopY = false
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        mouseUpX = x
        mouseUpY = y
        mouseUpMask = mask

        if (hasMouseCapture()) {
            if (!clickPickPending) {
                if (validClickPoint) {
                    val cameraMode: Int = TODO("APR: gAgentCamera.getCameraMode()") as Int
                    when {
                        cameraMode == CAMERA_MODE_CUSTOMIZE_AVATAR ->
                            TODO("APR: project focus global pos to screen, warp cursor there")
                        mouseSteering ->
                            TODO("APR: LLUI.getInstance().setMousePositionScreen(mouseDownX, mouseDownY)")
                        else ->
                            TODO("APR: gViewerWindow.moveCursorToCenter()")
                    }
                } else {
                    TODO("APR: LLUI.getInstance().setMousePositionScreen(mouseDownX, mouseDownY)")
                }
            }
            setMouseCapture(false)
        } else {
            releaseMouse()
        }
        return true
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (rightHoldMouseWalk) {
            TODO("APR: agent_push_forward(KEYSTATE_LEVEL)")
        }

        val dx: Int = TODO("APR: gViewerWindow.getCurrentMouseDX()") as Int
        val dy: Int = TODO("APR: gViewerWindow.getCurrentMouseDY()") as Int

        if (hasMouseCapture() && validClickPoint) {
            accumX += abs(dx)
            accumY += abs(dy)
            if (accumX >= SLOP_RANGE) outsideSlopX = true
            if (accumY >= SLOP_RANGE) outsideSlopY = true
        }

        if (outsideSlopX || outsideSlopY) {
            if (!validClickPoint) {
                TODO("APR: gViewerWindow.setCursor(UI_CURSOR_NO); gViewerWindow.showCursor()")
                return true
            }

            val isOrbit = gCameraBtnOrbit ||
                    mask == MASK_ORBIT_KEY ||
                    mask == (MASK_A or MASK_ORBIT_KEY)
            val isPan = gCameraBtnPan ||
                    mask == MASK_PAN_KEY ||
                    mask == (MASK_PAN_KEY or MASK_A)

            when {
                isOrbit && hasMouseCapture() -> {
                    val radiansPerPixel: Float =
                        TODO("APR: 360f * DEG_TO_RAD / gViewerWindow.getWorldViewWidthScaled()") as Float
                    if (dx != 0) TODO("APR: gAgentCamera.cameraOrbitAround(-dx * radiansPerPixel)")
                    if (dy != 0) TODO("APR: gAgentCamera.cameraOrbitOver(-dy * radiansPerPixel)")
                    TODO("APR: gViewerWindow.moveCursorToCenter()")
                }
                isPan && hasMouseCapture() -> {
                    val dist: Float = TODO("APR: normVec of camera-to-focus vector") as Float
                    val metersPerPixel = 3f * dist / (TODO("APR: gViewerWindow.getWorldViewWidthScaled()") as Float)
                    if (dx != 0) TODO("APR: gAgentCamera.cameraPanLeft(dx * metersPerPixel)")
                    if (dy != 0) TODO("APR: gAgentCamera.cameraPanUp(-dy * metersPerPixel)")
                    TODO("APR: gViewerWindow.moveCursorToCenter()")
                }
                gCameraBtnZoom && hasMouseCapture() -> {
                    val radiansPerPixel: Float =
                        TODO("APR: 360f * DEG_TO_RAD / gViewerWindow.getWorldViewWidthScaled()") as Float
                    if (dx != 0) TODO("APR: gAgentCamera.cameraOrbitAround(-dx * radiansPerPixel)")
                    val inFactor = 0.99f
                    if (dy != 0 && outsideSlopY) {
                        if (mouseSteering) {
                            TODO("APR: gAgentCamera.cameraOrbitOver(-dy * radiansPerPixel)")
                        } else {
                            TODO("APR: gAgentCamera.cameraZoomIn(inFactor.pow(dy.toFloat()))")
                        }
                    }
                    TODO("APR: gViewerWindow.moveCursorToCenter()")
                }
            }
        }

        val isOrbit = gCameraBtnOrbit || mask == MASK_ORBIT_KEY || mask == (MASK_A or MASK_ORBIT_KEY)
        val isPan = gCameraBtnPan || mask == MASK_PAN_KEY || mask == (MASK_PAN_KEY or MASK_A)
        when {
            isOrbit -> TODO("APR: gViewerWindow.setCursor(UI_CURSOR_TOOLCAMERA)")
            isPan -> TODO("APR: gViewerWindow.setCursor(UI_CURSOR_TOOLPAN)")
            else -> TODO("APR: gViewerWindow.setCursor(UI_CURSOR_TOOLZOOMIN)")
        }
        return true
    }

    // Firestorm: right-click while mouse-steering starts forward movement.
    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (mouseSteering) {
            TODO("APR: agent_push_forward(KEYSTATE_DOWN)")
            rightHoldMouseWalk = true
            return true
        }
        return false
    }

    override fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (mouseSteering || rightHoldMouseWalk) {
            TODO("APR: agent_push_forward(KEYSTATE_UP)")
            rightHoldMouseWalk = false
            return true
        }
        return false
    }

    override fun onMouseCaptureLost() {
        releaseMouse()
        handleRightMouseUp(0, 0, 0)
    }

    fun mouseSteerMode(): Boolean = mouseSteering
}
