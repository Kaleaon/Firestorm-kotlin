package com.firestorm.newview

import kotlin.math.sqrt

var grabBtnVertical: Boolean = false
var grabBtnSpin: Boolean = false
var grabTransientTool: Tool? = null

fun sendObjectGrabMessage(obj: ViewerObject, pick: PickInfo, grabOffset: Vector3) {
    if (obj.getRegion() == null) return
    TODO("APR: use JVM equivalent — send ObjectGrab UDP message to region host")
}

fun sendObjectDeGrabMessage(obj: ViewerObject, pick: PickInfo) {
    if (obj.getRegion() == null) return
    TODO("APR: use JVM equivalent — send ObjectDeGrab UDP message to region host")
}

private const val SLOP_DIST_SQ: Int = 4
private const val GRAB_SENSITIVITY_X: Float = 0.0075f
private const val GRAB_SENSITIVITY_Y: Float = 0.0075f
const val DEFAULT_GRAB_MASK: Int = MASK_CONTROL

open class ToolGrabBase(composite: ToolComposite? = null) : Tool("Grab", composite) {

    enum class GrabMode {
        INACTIVE, ACTIVE_CENTER, NONPHYSICAL, LOCKED, NOOBJECT
    }

    private var mode: GrabMode = GrabMode.INACTIVE
    private var verticalDragging: Boolean = false
    private var hitLand: Boolean = false

    private val grabTimer: Timer = Timer()

    private var grabOffsetFromCenterInitial: Vector3 = Vector3.ZERO
    private var grabHiddenOffsetFromCamera: Vector3d = Vector3d.ZERO

    private var dragStartPointGlobal: Vector3d = Vector3d.ZERO
    private var dragStartFromCamera: Vector3d = Vector3d.ZERO

    private var grabPick: PickInfo = PickInfo()

    private var lastMouseX: Int = 0
    private var lastMouseY: Int = 0
    private var accumDeltaX: Int = 0
    private var accumDeltaY: Int = 0
    private var hasMoved: Boolean = false
    private var outsideSlop: Boolean = false
    private var deselectedThisClick: Boolean = false
    private var validSelection: Boolean = false

    private var lastFace: Int = 0
    private var lastUVCoords: Vector2 = Vector2.ZERO
    private var lastSTCoords: Vector2 = Vector2.ZERO
    private var lastIntersection: Vector3 = Vector3.ZERO
    private var lastNormal: Vector3 = Vector3.ZERO
    private var lastBinormal: Vector3 = Vector3.ZERO
    private var lastGrabPos: Vector3 = Vector3.ZERO

    private var spinGrabbing: Boolean = false
    private var spinRotation: Quaternion = Quaternion.IDENTITY

    var hideBuildHighlight: Boolean = false
        private set

    private var clickedInMouselook: Boolean = false

    override fun handleSelect() {
        FloaterTools.instance?.setStatusText("grab")
        validSelection = FloaterTools.instance?.getVisible() ?: false
        grabBtnVertical = false
        grabBtnSpin = false
    }

    override fun handleDeselect() {
        if (hasMouseCapture()) {
            setMouseCapture(false)
        }

        val overrideMask = Keyboard.instance?.currentMask(true) ?: 0
        if (!validSelection && (overrideMask != MASK_NONE || FloaterTools.instance?.getVisible() == true)) {
            MenuGL.sMenuContainer?.hideMenus()
            SelectMgr.instance.validateSelection()
        }
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean = false

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        super.handleMouseDown(x, y, mask)

        if (!Agent.instance.leftButtonGrabbed() ||
            ((mask and DEFAULT_GRAB_MASK) != 0 && !AgentCamera.instance.cameraMouselook())
        ) {
            ViewerWindow.instance.pickAsync(x, y, mask, ::pickCallback, pickTransparent = true)
        }

        clickedInMouselook = AgentCamera.instance.cameraMouselook()

        if (clickedInMouselook && ViewerInput.instance.isLMouseHandlingDefault(MODE_FIRST_PERSON)) {
            Agent.instance.setControlFlags(AGENT_CONTROL_LBUTTON_DOWN)
        }

        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        super.handleMouseUp(x, y, mask)

        if (AgentCamera.instance.cameraMouselook() && ViewerInput.instance.isLMouseHandlingDefault(MODE_FIRST_PERSON)) {
            Agent.instance.setControlFlags(AGENT_CONTROL_LBUTTON_UP)
        }

        if (hasMouseCapture()) {
            setMouseCapture(false)
        }

        mode = GrabMode.INACTIVE

        if ((clickedInMouselook && AgentCamera.instance.cameraMouselook()) ||
            (!clickedInMouselook && !AgentCamera.instance.cameraMouselook())
        ) {
            val transient = grabTransientTool
            if (transient != null) {
                BasicToolset.instance.selectTool(transient)
                grabTransientTool = null
            }
        }

        clickedInMouselook = false
        return true
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (!ViewerWindow.instance.getLeftMouseDown()) {
            ViewerWindow.instance.setCursor(UiCursor.TOOLGRAB)
            setMouseCapture(false)
            return true
        }

        if (RlvActions.isRlvEnabled() &&
            mode != GrabMode.INACTIVE &&
            mode != GrabMode.NOOBJECT &&
            hasMouseCapture() &&
            !RlvActions.canTouch(grabPick.getObject(), grabPick.objectOffset)
        ) {
            val transient = grabTransientTool
            if (transient != null) {
                BasicToolset.instance.selectTool(transient)
                grabTransientTool = null
            }
            setMouseCapture(false)
            return true
        }

        when (mode) {
            GrabMode.ACTIVE_CENTER -> handleHoverActive(x, y, mask)
            GrabMode.NONPHYSICAL   -> handleHoverNonPhysical(x, y, mask)
            GrabMode.INACTIVE      -> handleHoverInactive(x, y, mask)
            GrabMode.NOOBJECT,
            GrabMode.LOCKED        -> handleHoverFailed(x, y, mask)
        }

        lastMouseX = x
        lastMouseY = y
        return true
    }

    override fun render() {}
    override fun draw() {}

    override fun isEditing(): Boolean = grabPick.getObject() != null

    override fun getEditingObject(): ViewerObject? = grabPick.getObject()

    override fun getEditingPointGlobal(): Vector3d = getGrabPointGlobal()

    override fun stopEditing() {
        if (hasMouseCapture()) setMouseCapture(false)
    }

    override fun onMouseCaptureLost() {
        val obj = grabPick.getObject()
        if (obj == null) {
            ViewerWindow.instance.showCursor()
            return
        }

        if (!AgentCamera.instance.cameraMouselook() && mode == GrabMode.ACTIVE_CENTER) {
            if (obj.isHUDAttachment()) {
                val sx = grabPick.mousePoint.x + accumDeltaX
                val sy = grabPick.mousePoint.y + accumDeltaY
                UI.instance.setMousePositionScreen(sx, sy)
            } else if (hasMoved) {
                val grabPointAgent = obj.getRenderPosition()
                val glPoint = CoordGL()
                if (ViewerCamera.instance.projectPosAgentToScreen(grabPointAgent, glPoint)) {
                    UI.instance.setMousePositionScreen(glPoint.x, glPoint.y)
                }
            } else {
                UI.instance.setMousePositionScreen(grabPick.mousePoint.x, grabPick.mousePoint.y)
            }
            ViewerWindow.instance.showCursor()
        }

        stopGrab()
        if (spinGrabbing) stopSpin()

        mode = GrabMode.INACTIVE
        hideBuildHighlight = false
        grabPick.objectId = null

        SelectMgr.instance.updateSelectionCenter()
        AgentCamera.instance.setPointAt(POINTAT_TARGET_CLEAR)
        AgentCamera.instance.setLookAt(LOOKAT_TARGET_CLEAR)

        dialogRefreshAll()
    }

    fun handleObjectHit(info: PickInfo): Boolean {
        grabPick = info
        val obj = grabPick.getObject() ?: return false

        if (obj.isAvatar()) {
            val transient = grabTransientTool
            if (transient != null) {
                BasicToolset.instance.selectTool(transient)
                grabTransientTool = null
            }
            return true
        }

        setMouseCapture(true)

        val parent = obj.getRootEdit()
        val scriptTouch = obj.flagHandleTouch() || parent?.flagHandleTouch() == true

        hideBuildHighlight = scriptTouch || obj.flagUsePhysics()

        if (!obj.flagUsePhysics()) {
            mode = if (scriptTouch) {
                GrabMode.NONPHYSICAL
            } else if (AgentCamera.instance.cameraMouselook()) {
                ViewerWindow.instance.hideCursor()
                ViewerWindow.instance.moveCursorToCenter()
                GrabMode.LOCKED
            } else if (obj.permMove() && !obj.isPermanentEnforced()) {
                ViewerWindow.instance.hideCursor()
                ViewerWindow.instance.moveCursorToCenter()
                GrabMode.ACTIVE_CENTER
            } else {
                GrabMode.LOCKED
            }
        } else if (obj.flagCharacter() || !obj.permMove() || obj.isPermanentEnforced()) {
            mode = GrabMode.LOCKED
        } else {
            mode = GrabMode.ACTIVE_CENTER
            ViewerWindow.instance.hideCursor()
            ViewerWindow.instance.moveCursorToCenter()
        }

        lastMouseX = ViewerWindow.instance.getCurrentMouseX()
        lastMouseY = ViewerWindow.instance.getCurrentMouseY()
        accumDeltaX = 0
        accumDeltaY = 0
        hasMoved = false
        outsideSlop = false

        verticalDragging = (info.keyMask == MASK_VERTICAL) || grabBtnVertical

        startGrab()

        if ((info.keyMask == MASK_SPIN) || grabBtnSpin) {
            startSpin()
        }

        SelectMgr.instance.updateSelectionCenter()

        val editObj = info.getObject()
        if (editObj != null && info.pickType != PickInfo.PickType.FLORA) {
            val localEditPoint = Agent.instance.getPosAgentFromGlobal(info.posGlobal) -
                    editObj.getPositionAgent()
            val rotated = localEditPoint * editObj.getRenderRotation().inverse()
            AgentCamera.instance.setPointAt(POINTAT_TARGET_GRAB, editObj, rotated)
            AgentCamera.instance.setLookAt(LOOKAT_TARGET_SELECT, editObj, rotated)
        }

        if (!ViewerWindow.instance.getLeftMouseDown() &&
            grabTransientTool != null &&
            (mode == GrabMode.NONPHYSICAL || mode == GrabMode.LOCKED)
        ) {
            BasicToolset.instance.selectTool(grabTransientTool!!)
            grabTransientTool = null
        }

        return true
    }

    fun hasGrabOffset(): Boolean = true

    fun getGrabOffset(x: Int, y: Int): Vector3 {
        TODO("GPU: compute grab offset from screen coords")
    }

    fun setClickedInMouselook(value: Boolean) {
        clickedInMouselook = value
    }

    private fun getGrabPointGlobal(): Vector3d = when (mode) {
        GrabMode.ACTIVE_CENTER,
        GrabMode.NONPHYSICAL,
        GrabMode.LOCKED -> AgentCamera.instance.getCameraPositionGlobal() + grabHiddenOffsetFromCamera
        else            -> Agent.instance.getPositionGlobal()
    }

    private fun startGrab() {
        val obj = grabPick.getObject() ?: return
        val root = obj.getRoot() as ViewerObject

        val grabStartGlobal = root.getPositionGlobal()
        val grabOffsetD = root.getPositionGlobal() - obj.getPositionGlobal()
        var grabOffset = Vector3(grabOffsetD)

        val rotation = root.getRotation().conjugated()
        grabOffset = grabOffset * rotation

        dragStartPointGlobal = grabStartGlobal
        dragStartFromCamera = grabStartGlobal - AgentCamera.instance.getCameraPositionGlobal()

        sendObjectGrabMessage(obj, grabPick, grabOffset)

        grabOffsetFromCenterInitial = grabOffset
        grabHiddenOffsetFromCamera = dragStartFromCamera

        grabTimer.reset()

        lastUVCoords = grabPick.uvCoords
        lastSTCoords = grabPick.stCoords
        lastFace = grabPick.objectFace
        lastIntersection = grabPick.intersection
        lastNormal = grabPick.normal
        lastBinormal = grabPick.binormal
        lastGrabPos = Vector3(-1f, -1f, -1f)
    }

    private fun stopGrab() {
        val obj = grabPick.getObject() ?: return
        var pick = grabPick

        if (mode == GrabMode.NONPHYSICAL) {
            val x = ViewerWindow.instance.getCurrentMouseX()
            val y = ViewerWindow.instance.getCurrentMouseY()
            pick = grabPick.copy(mousePoint = CoordGL(x, y))
            pick.getSurfaceInfo()
        }

        when (mode) {
            GrabMode.ACTIVE_CENTER,
            GrabMode.NONPHYSICAL,
            GrabMode.LOCKED -> {
                sendObjectDeGrabMessage(obj, pick)
                verticalDragging = false
            }
            else -> {}
        }

        hideBuildHighlight = false
    }

    private fun startSpin() {
        val obj = grabPick.getObject() ?: return
        spinGrabbing = true
        val root = obj.getRoot() as ViewerObject
        spinRotation = root.getRotation()
        TODO("APR: use JVM equivalent — send ObjectSpinStart UDP message")
    }

    private fun stopSpin() {
        spinGrabbing = false
        val obj = grabPick.getObject() ?: return

        when (mode) {
            GrabMode.ACTIVE_CENTER,
            GrabMode.NONPHYSICAL,
            GrabMode.LOCKED -> TODO("APR: use JVM equivalent — send ObjectSpinStop UDP message")
            else -> {}
        }
    }

    private fun handleHoverActive(x: Int, y: Int, mask: Int) {
        val obj = grabPick.getObject()
        if (obj == null || !hasMouseCapture()) return
        if (obj.isDead()) { setMouseCapture(false); return }

        val verticalDrag = (mask == MASK_VERTICAL) || (grabBtnVertical && mask != MASK_SPIN)
        val spinGrab = (mask == MASK_SPIN) || (grabBtnSpin && mask != MASK_VERTICAL)

        if (spinGrabbing && !spinGrab) stopSpin()
        else if (!spinGrabbing && spinGrab) startSpin()
        spinGrabbing = spinGrab

        if (verticalDragging && !verticalDrag) {
            dragStartPointGlobal = ViewerWindow.instance.clickPointInWorldGlobal(x, y, obj)
            dragStartFromCamera = dragStartPointGlobal - AgentCamera.instance.getCameraPositionGlobal()
        } else if (!verticalDragging && verticalDrag) {
            dragStartPointGlobal = ViewerWindow.instance.clickPointInWorldGlobal(x, y, obj)
            dragStartFromCamera = dragStartPointGlobal - AgentCamera.instance.getCameraPositionGlobal()
        }
        verticalDragging = verticalDrag

        val radiansPerPixelX = 0.01f
        val radiansPerPixelY = 0.01f

        val dx = ViewerWindow.instance.getCurrentMouseDX()
        val dy = ViewerWindow.instance.getCurrentMouseDY()

        if (dx != 0 || dy != 0) {
            accumDeltaX += dx
            accumDeltaY += dy
            val distSq = accumDeltaX * accumDeltaX + accumDeltaY * accumDeltaY
            if (distSq > SLOP_DIST_SQ) outsideSlop = true
            hasMoved = true

            if (spinGrabbing) {
                val up = Vector3(0f, 0f, 1f)
                val rotAroundVertical = Quaternion.fromAxisAngle(up, dx * radiansPerPixelX)
                val agentLeft = ViewerCamera.instance.getLeftAxis()
                val rotAroundLeft = Quaternion.fromAxisAngle(agentLeft, dy * radiansPerPixelY)
                spinRotation = spinRotation * rotAroundVertical * rotAroundLeft
                TODO("APR: use JVM equivalent — send ObjectSpinUpdate UDP message with spinRotation")
            } else {
                var xPart = Vector3d(ViewerCamera.instance.getLeftAxis())
                xPart.z = 0.0
                xPart = xPart.normalized()

                val yPart: Vector3d = if (verticalDragging) {
                    Vector3d(ViewerCamera.instance.getUpAxis())
                } else {
                    var yp = xPart.cross(Vector3d.Z_AXIS)
                    yp.z = 0.0
                    yp.normalized()
                }

                grabHiddenOffsetFromCamera = grabHiddenOffsetFromCamera +
                        (xPart * (-dx * GRAB_SENSITIVITY_X)) +
                        (yPart * (dy * GRAB_SENSITIVITY_Y))

                val dt = grabTimer.getElapsedAndReset()
                val dtMillis = (1000f * dt).toUInt()

                var grabPointGlobal = AgentCamera.instance.getCameraPositionGlobal() + grabHiddenOffsetFromCamera

                val landHeight = World.instance.resolveLandHeightGlobal(grabPointGlobal)
                if (grabPointGlobal.z < landHeight) grabPointGlobal.z = landHeight.toDouble()

                val regionMaxHeight = World.instance.getRegionMaxHeight()
                if (grabPointGlobal.z > regionMaxHeight) grabPointGlobal.z = regionMaxHeight.toDouble()

                grabPointGlobal = World.instance.clipToVisibleRegions(dragStartPointGlobal, grabPointGlobal)
                grabHiddenOffsetFromCamera = grabPointGlobal - AgentCamera.instance.getCameraPositionGlobal()

                val grabPosAgent = Agent.instance.getPosAgentFromGlobal(grabPointGlobal)
                val grabCenterGl = CoordGL(
                    ViewerWindow.instance.getWorldViewWidthScaled() / 2,
                    ViewerWindow.instance.getWorldViewHeightScaled() / 2
                )
                ViewerCamera.instance.projectPosAgentToScreen(grabPosAgent, grabCenterGl)

                val rotateHMargin = ViewerWindow.instance.getWorldViewWidthScaled() / 20
                val rotateAnglePerSecond = 30f * DEG_TO_RAD
                val rotateAngle = rotateAnglePerSecond / FPS_CLAMPED

                if (grabCenterGl.x < rotateHMargin) {
                    if (AgentCamera.instance.getFocusOnAvatar()) Agent.instance.yaw(rotateAngle)
                    else AgentCamera.instance.cameraOrbitAround(rotateAngle)
                } else if (grabCenterGl.x > ViewerWindow.instance.getWorldViewWidthScaled() - rotateHMargin) {
                    if (AgentCamera.instance.getFocusOnAvatar()) Agent.instance.yaw(-rotateAngle)
                    else AgentCamera.instance.cameraOrbitAround(-rotateAngle)
                }

                if (grabCenterGl.y < ViewerWindow.instance.getWorldViewHeightScaled() - 6 &&
                    grabCenterGl.y > 24
                ) {
                    val grabPosRegion = obj.getRegion()!!.getPosRegionFromGlobal(grabPointGlobal)
                    TODO("APR: use JVM equivalent — send ObjectGrabUpdate UDP message")
                }
            }

            ViewerWindow.instance.moveCursorToCenter()
            SelectMgr.instance.updateSelectionCenter()
        }

        if (hasMoved) {
            if (!AgentCamera.instance.cameraMouselook() &&
                !obj.isHUDAttachment() &&
                obj.getRoot() == AgentAvatarSelf.instance.getRoot()
            ) {
                if (!SavedSettings.getBool("EditCameraMovement")) {
                    AgentCamera.instance.setFocusGlobal(AgentCamera.instance.calcFocusPositionTargetGlobal(), null)
                    AgentCamera.instance.setFocusOnAvatar(false, animate = true)
                }
            } else {
                AgentCamera.instance.clearFocusObject()
            }
        }

        ViewerWindow.instance.setCursor(UiCursor.ARROW)
    }

    private fun handleHoverNonPhysical(x: Int, y: Int, mask: Int) {
        val obj = grabPick.getObject()
        if (obj == null || !hasMouseCapture()) return
        if (obj.isDead()) { setMouseCapture(false); return }

        val pick = grabPick.copy(mousePoint = CoordGL(x, y))
        pick.getSurfaceInfo()

        val dt = grabTimer.getElapsedAndReset()
        val dtMillis = (1000f * dt).toUInt()

        var grabPosRegion = Vector3.ZERO

        if (!(mask == MASK_VERTICAL) && !grabBtnVertical) {
            verticalDragging = false
        } else if ((grabBtnVertical && mask != MASK_SPIN) || mask == MASK_VERTICAL) {
            verticalDragging = true
        }

        val dx = x - lastMouseX
        val dy = y - lastMouseY

        if (dx != 0 || dy != 0) {
            accumDeltaX += dx
            accumDeltaY += dy
            val distSq = accumDeltaX * accumDeltaX + accumDeltaY * accumDeltaY
            if (distSq > SLOP_DIST_SQ) outsideSlop = true
            hasMoved = true

            var xPart = Vector3d(ViewerCamera.instance.getLeftAxis())
            xPart.z = 0.0
            xPart = xPart.normalized()

            val yPart: Vector3d = if (verticalDragging) {
                Vector3d(ViewerCamera.instance.getUpAxis())
            } else {
                var yp = xPart.cross(Vector3d.Z_AXIS)
                yp.z = 0.0
                yp.normalized()
            }

            grabHiddenOffsetFromCamera = grabHiddenOffsetFromCamera +
                    (xPart * (-dx * GRAB_SENSITIVITY_X)) +
                    (yPart * (dy * GRAB_SENSITIVITY_Y))
        }

        val grabPointGlobal = AgentCamera.instance.getCameraPositionGlobal() + grabHiddenOffsetFromCamera
        grabPosRegion = obj.getRegion()!!.getPosRegionFromGlobal(grabPointGlobal)

        val changed = pick.objectFace != lastFace ||
                pick.uvCoords != lastUVCoords ||
                pick.stCoords != lastSTCoords ||
                pick.intersection != lastIntersection ||
                pick.normal != lastNormal ||
                pick.binormal != lastBinormal ||
                grabPosRegion != lastGrabPos

        if (changed) {
            TODO("APR: use JVM equivalent — send ObjectGrabUpdate UDP message")
            lastUVCoords = pick.uvCoords
            lastSTCoords = pick.stCoords
            lastFace = pick.objectFace
            lastIntersection = pick.intersection
            lastNormal = pick.normal
            lastBinormal = pick.binormal
            lastGrabPos = grabPosRegion
        }

        if (pick.objectFace != -1) {
            val localEditPoint = (pick.intersection - obj.getPositionAgent()) * obj.getRenderRotation().inverse()
            AgentCamera.instance.setPointAt(POINTAT_TARGET_GRAB, obj, localEditPoint)
            AgentCamera.instance.setLookAt(LOOKAT_TARGET_SELECT, obj, localEditPoint)
        }

        ViewerWindow.instance.setCursor(UiCursor.HAND)
    }

    private fun handleHoverInactive(x: Int, y: Int, mask: Int) {
        ViewerWindow.instance.setCursor(UiCursor.TOOLGRAB)
    }

    private fun handleHoverFailed(x: Int, y: Int, mask: Int) {
        if (mode == GrabMode.NOOBJECT) {
            ViewerWindow.instance.setCursor(UiCursor.NO)
        } else {
            val distSq = (x - grabPick.mousePoint.x).let { it * it } +
                    (y - grabPick.mousePoint.y).let { it * it }
            if (outsideSlop || distSq > SLOP_DIST_SQ) {
                outsideSlop = true
                if (mode == GrabMode.LOCKED) {
                    ViewerWindow.instance.setCursor(UiCursor.GRABLOCKED)
                }
            } else {
                ViewerWindow.instance.setCursor(UiCursor.ARROW)
            }
        }
    }

    companion object {
        fun pickCallback(pickInfo: PickInfo) {
            ToolGrab.grabPick = pickInfo
            val obj = pickInfo.getObject()

            val extendSelect = (pickInfo.keyMask and MASK_SHIFT) != 0

            if (!extendSelect && !SelectMgr.instance.getSelection().isEmpty()) {
                SelectMgr.instance.deselectAll()
                ToolGrab.deselectedThisClick = true
            } else {
                ToolGrab.deselectedThisClick = false
            }

            if (obj == null ||
                (RlvActions.isRlvEnabled() && !RlvActions.canTouch(obj, pickInfo.objectOffset))
            ) {
                ToolGrab.setMouseCapture(true)
                ToolGrab.mode = GrabMode.NOOBJECT
                ToolGrab.grabPick.objectId = null
            } else {
                ToolGrab.handleObjectHit(ToolGrab.grabPick)
            }
        }
    }
}

object ToolGrab : ToolGrabBase()
