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
        System.err.println("ToolCamera: handleSelect not yet implemented")
    }

    override fun handleDeselect() {
        val overrideMask: Int = 0
        // Only clear selection on deselect when there is a keyboard override or tools floater is visible.
        if (!validSelection && (overrideMask != MASK_NONE ||
                    false)
        ) {
            // no-op: LLMenuGL.sMenuContainer.hideMenus(); SelectMgr.getInstance().validateSelection() not yet implemented
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

        // no-op: gViewerWindow.hideCursor() not yet implemented
        // no-op: gViewerWindow.pickAsync not yet implemented
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

        // no-op: gViewerWindow.moveCursorToCenter() not yet implemented

        val hitObj: ViewerObjectStub? = pickInfo.getObject()

        if (hitObj == null && pickInfo.isPosGlobalZero()) {
            validClickPoint = false
            return
        }

        if (hitObj?.isHUDAttachment() == true) {
            val selection = null
            if (false) {
                validClickPoint = false
                return
            }
        }

        val cameraMode: Int = 0
        if (cameraMode == CAMERA_MODE_CUSTOMIZE_AVATAR) {
            val goodHit = hitObj != null &&
                    (hitObj.isAgentAvatar() || (hitObj.isAttachment() && hitObj.permYouOwner()))
            if (!goodHit) { validClickPoint = false; return }
            // no-op: gMorphView?.setCameraDrivenByKeys(false) not yet implemented
        } else if (pickInfo.keyMask and MASK_A != 0 ||
            ToolMgr.getCurrentTool()?.getName() == "Camera"
        ) {
            if (hitObj != null && !hitObj.isHUDAttachment()) {
                // no-op: gAgentCamera.setFocusOnAvatar(false, ANIMATE); gAgentCamera.setFocusGlobal(pickInfo) not yet implemented
            } else if (!pickInfo.isPosGlobalZero()) {
                // no-op: gAgentCamera.setFocusOnAvatar(false, ANIMATE); gAgentCamera.setFocusGlobal(pickInfo) not yet implemented
            }

            val zoomTool = gCameraBtnZoom && ToolMgr.getBaseTool() === ToolCamera
            val freezeTime: Boolean = false
            if (pickInfo.keyMask and MASK_A == 0 &&
                !freezeTime &&
                !zoomTool &&
                false &&
                false &&
                false &&
                (hitObj?.isAgentAvatar() == true ||
                        (hitObj?.isAttachment() == true && hitObj.isSelf()))
            ) {
                mouseSteering = true
            }
        }

        validClickPoint = true

        if (cameraMode == CAMERA_MODE_CUSTOMIZE_AVATAR) {
            // no-op: gAgentCamera.setFocusOnAvatar(false, false); gAgentCamera.setCameraPosAndFocusGlobal not yet implemented
        }
    }

    private fun releaseMouse() {
        super.handleMouseUp(mouseUpX, mouseUpY, mouseUpMask)
        // no-op: gViewerWindow.showCursor() not yet implemented
        if (false) {
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
                    val cameraMode: Int = 0
                    when {
                        cameraMode == CAMERA_MODE_CUSTOMIZE_AVATAR ->
                            Unit // no-op: project focus global pos to screen, warp cursor there not yet implemented
                        mouseSteering ->
                            Unit // no-op: LLUI.getInstance().setMousePositionScreen not yet implemented
                        else ->
                            Unit // no-op: gViewerWindow.moveCursorToCenter() not yet implemented
                    }
                } else {
                    // no-op: LLUI.getInstance().setMousePositionScreen not yet implemented
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
            // no-op: agent_push_forward(KEYSTATE_LEVEL) not yet implemented
        }

        val dx: Int = 0
        val dy: Int = 0

        if (hasMouseCapture() && validClickPoint) {
            accumX += abs(dx)
            accumY += abs(dy)
            if (accumX >= SLOP_RANGE) outsideSlopX = true
            if (accumY >= SLOP_RANGE) outsideSlopY = true
        }

        if (outsideSlopX || outsideSlopY) {
            if (!validClickPoint) {
                // no-op: gViewerWindow.setCursor(UI_CURSOR_NO); gViewerWindow.showCursor() not yet implemented
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
                    val radiansPerPixel: Float = 0f
                    if (dx != 0) { /* no-op: gAgentCamera.cameraOrbitAround not yet implemented */ }
                    if (dy != 0) { /* no-op: gAgentCamera.cameraOrbitOver not yet implemented */ }
                    // no-op: gViewerWindow.moveCursorToCenter() not yet implemented
                }
                isPan && hasMouseCapture() -> {
                    val dist: Float = 0f
                    val metersPerPixel = 3f * dist / 1f
                    if (dx != 0) { /* no-op: gAgentCamera.cameraPanLeft not yet implemented */ }
                    if (dy != 0) { /* no-op: gAgentCamera.cameraPanUp not yet implemented */ }
                    // no-op: gViewerWindow.moveCursorToCenter() not yet implemented
                }
                gCameraBtnZoom && hasMouseCapture() -> {
                    val radiansPerPixel: Float = 0f
                    if (dx != 0) { /* no-op: gAgentCamera.cameraOrbitAround not yet implemented */ }
                    val inFactor = 0.99f
                    if (dy != 0 && outsideSlopY) {
                        if (mouseSteering) {
                            // no-op: gAgentCamera.cameraOrbitOver not yet implemented
                        } else {
                            // no-op: gAgentCamera.cameraZoomIn not yet implemented
                        }
                    }
                    // no-op: gViewerWindow.moveCursorToCenter() not yet implemented
                }
            }
        }

        val isOrbit = gCameraBtnOrbit || mask == MASK_ORBIT_KEY || mask == (MASK_A or MASK_ORBIT_KEY)
        val isPan = gCameraBtnPan || mask == MASK_PAN_KEY || mask == (MASK_PAN_KEY or MASK_A)
        when {
            isOrbit -> { /* no-op: gViewerWindow.setCursor(UI_CURSOR_TOOLCAMERA) not yet implemented */ }
            isPan -> { /* no-op: gViewerWindow.setCursor(UI_CURSOR_TOOLPAN) not yet implemented */ }
            else -> { /* no-op: gViewerWindow.setCursor(UI_CURSOR_TOOLZOOMIN) not yet implemented */ }
        }
        return true
    }

    // Firestorm: right-click while mouse-steering starts forward movement.
    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (mouseSteering) {
            // no-op: agent_push_forward(KEYSTATE_DOWN) not yet implemented
            rightHoldMouseWalk = true
            return true
        }
        return false
    }

    override fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (mouseSteering || rightHoldMouseWalk) {
            // no-op: agent_push_forward(KEYSTATE_UP) not yet implemented
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
