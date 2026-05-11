package com.firestorm.newview

import com.firestorm.tool.Tool
import com.firestorm.tool.ToolSelectRect
import com.firestorm.tool.ToolPlacer
import com.firestorm.tool.ToolGun
import com.firestorm.tool.ToolGrabBase
import com.firestorm.tool.ToolGrab
import com.firestorm.tool.ToolCamera
import com.firestorm.tool.ToolMgr
import com.firestorm.manip.Manip
import com.firestorm.manip.ManipTranslate
import com.firestorm.manip.ManipScale
import com.firestorm.manip.ManipRotate
import com.firestorm.ui.PickInfo
import com.firestorm.ui.ViewerWindow
import com.firestorm.ui.FloaterReg
import com.firestorm.agent.Agent
import com.firestorm.agent.AgentCamera

typealias KEY = Int
typealias MASK = Int

const val MASK_CONTROL: MASK = 0x01
const val MASK_SHIFT: MASK   = 0x02
const val MASK_ALT: MASK     = 0x04
const val KEY_ALT: KEY       = 0x12

abstract class ToolComposite(name: String) : Tool(name) {

    protected var cur: Tool = nullTool
    protected var default: Tool = nullTool
    protected var selected: Boolean = false
    protected var mouseDown: Boolean = false
    protected var manip: Manip? = null
    protected var selectRect: ToolSelectRect? = null

    companion object {
        val sNameComp: String = "Composite"
        val nullTool: Tool = Tool("null")
    }

    protected fun setCurrentTool(newTool: Tool) {
        if (cur !== newTool) {
            if (selected) {
                cur.handleDeselect()
                cur = newTool
                cur.handleSelect()
            } else {
                cur = newTool
            }
        }
    }

    protected fun setToolFromMask(mask: MASK, normal: Tool) {
        TODO("APR: use JVM equivalent")
    }

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        val handled = cur.handleMouseUp(x, y, mask)
        if (handled) setCurrentTool(default)
        return handled
    }

    override fun handleHover(x: Int, y: Int, mask: MASK): Boolean = cur.handleHover(x, y, mask)
    override fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean = cur.handleScrollWheel(x, y, clicks)
    override fun handleRightMouseDown(x: Int, y: Int, mask: MASK): Boolean = cur.handleRightMouseDown(x, y, mask)
    override fun handleRightMouseUp(x: Int, y: Int, mask: MASK): Boolean = cur.handleRightMouseUp(x, y, mask)
    override fun getEditingObject(): Any? = cur.getEditingObject()
    override fun getEditingPointGlobal(): Any = cur.getEditingPointGlobal()
    override fun isEditing(): Boolean = cur.isEditing()
    override fun stopEditing() { cur.stopEditing(); cur = default }
    override fun clipMouseWhenDown(): Boolean = cur.clipMouseWhenDown()
    override fun render() = cur.render()
    override fun draw() = cur.draw()
    override fun handleKey(key: KEY, mask: MASK): Boolean = cur.handleKey(key, mask)
    override fun screenPointToLocal(screenX: Int, screenY: Int, localX: IntArray, localY: IntArray) =
        cur.screenPointToLocal(screenX, screenY, localX, localY)
    override fun localPointToScreen(localX: Int, localY: Int, screenX: IntArray, screenY: IntArray) =
        cur.localPointToScreen(localX, localY, screenX, screenY)

    override fun handleSelect() {
        if (!Agent.savedSettings.getBool("EditLinkedParts")) {
            SelectMgr.instance.promoteSelectionToRoot()
        }
        cur = default
        cur.handleSelect()
        selected = true
    }

    override fun handleDeselect() {
        cur.handleDeselect()
        cur = default
        selected = false
    }

    override fun onMouseCaptureLost() {
        cur.onMouseCaptureLost()
        setCurrentTool(default)
    }

    fun isSelecting(): Boolean = cur === selectRect
    fun getCurrentTool(): Tool = cur
}

object ToolCompInspect : ToolComposite("Inspect") {

    private var isToolCameraActive: Boolean = false

    init {
        selectRect = ToolSelectRect(this)
        default = selectRect!!
        cur = default
    }

    override fun handleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        return if (cur === ToolCamera.instance) {
            cur.handleMouseDown(x, y, mask)
        } else {
            mouseDown = true
            ViewerWindow.instance.pickAsync(x, y, mask, ::pickCallback)
            true
        }
    }

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        val handled = super.handleMouseUp(x, y, mask)
        isToolCameraActive = getCurrentTool() === ToolCamera.instance
        return handled
    }

    fun pickCallback(pickInfo: PickInfo) {
        val hitObj = pickInfo.getObject()
        if (!mouseDown) {
            selectRect!!.handleObjectSelection(pickInfo, Agent.savedSettings.getBool("EditLinkedParts"), false)
            return
        }
        if (hitObj != null && SelectMgr.instance.getSelection().getObjectCount() > 0) {
            EditMenuHandler.gEditMenuHandler = SelectMgr.instance
        }
        setCurrentTool(selectRect!!)
        isToolCameraActive = false
        selectRect!!.handlePick(pickInfo)
    }

    companion object {
        @JvmStatic fun pickCallback(pickInfo: PickInfo) = ToolCompInspect.pickCallback(pickInfo)
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: MASK): Boolean = true

    override fun handleKey(key: KEY, mask: MASK): Boolean {
        return if (key == KEY_ALT) {
            setCurrentTool(ToolCamera.instance)
            isToolCameraActive = true
            true
        } else {
            super.handleKey(key, mask)
        }
    }

    override fun onMouseCaptureLost() {
        super.onMouseCaptureLost()
        isToolCameraActive = false
    }

    fun keyUp(key: KEY, mask: MASK) {
        if (key == KEY_ALT && cur === ToolCamera.instance) {
            setCurrentTool(default)
            isToolCameraActive = false
        }
    }

    fun isToolCameraActive(): Boolean = isToolCameraActive
}

object ToolCompTranslate : ToolComposite("Move") {

    init {
        manip = ManipTranslate(this)
        selectRect = ToolSelectRect(this)
        cur = manip!!
        default = manip!!
    }

    override fun handleHover(x: Int, y: Int, mask: MASK): Boolean {
        if (!cur.hasMouseCapture()) setCurrentTool(manip!!)
        return cur.handleHover(x, y, mask)
    }

    override fun handleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        mouseDown = true
        ViewerWindow.instance.pickAsync(
            x, y, mask, Companion::pickCallback,
            pickTransparent = false,
            pickReflectionProbes = Agent.savedSettings.getBool("SelectReflectionProbes")
        )
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        mouseDown = false
        return super.handleMouseUp(x, y, mask)
    }

    override fun getOverrideTool(mask: MASK): Tool? = when (mask) {
        MASK_CONTROL                  -> ToolCompRotate
        MASK_CONTROL or MASK_SHIFT    -> ToolCompScale
        else                          -> super.getOverrideTool(mask)
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: MASK): Boolean {
        if (manip!!.getSelection().isEmpty() && manip!!.getHighlightedPart() == Manip.NO_PART) {
            FloaterReg.showInstance("build", "Contents")
            return true
        }
        return handleMouseDown(x, y, mask)
    }

    override fun render() {
        cur.render()
        if (cur !== manip) {
            TODO("GPU: LLGLDepthTest(GL_TRUE, GL_FALSE)")
            manip!!.renderGuidelines()
        }
    }

    companion object {
        @JvmStatic fun pickCallback(pickInfo: PickInfo) {
            val hitObj = pickInfo.getObject()
            ToolCompTranslate.manip!!.highlightManipulators(pickInfo.mousePt.x, pickInfo.mousePt.y)
            if (!ToolCompTranslate.mouseDown) {
                ToolCompTranslate.selectRect!!.handleObjectSelection(
                    pickInfo, Agent.savedSettings.getBool("EditLinkedParts"), false
                )
                return
            }
            if (hitObj != null || ToolCompTranslate.manip!!.getHighlightedPart() != Manip.NO_PART) {
                if (ToolCompTranslate.manip!!.getSelection().getObjectCount() > 0) {
                    EditMenuHandler.gEditMenuHandler = SelectMgr.instance
                }
                val canMove = ToolCompTranslate.manip!!.canAffectSelection()
                if (ToolCompTranslate.manip!!.getHighlightedPart() != Manip.NO_PART && canMove) {
                    ToolCompTranslate.setCurrentTool(ToolCompTranslate.manip!!)
                    ToolCompTranslate.manip!!.handleMouseDownOnPart(pickInfo.mousePt.x, pickInfo.mousePt.y, pickInfo.keyMask)
                } else {
                    ToolCompTranslate.setCurrentTool(ToolCompTranslate.selectRect!!)
                    ToolCompTranslate.selectRect!!.handlePick(pickInfo)
                }
            } else {
                ToolCompTranslate.setCurrentTool(ToolCompTranslate.selectRect!!)
                ToolCompTranslate.selectRect!!.handlePick(pickInfo)
            }
        }
    }
}

object ToolCompScale : ToolComposite("Stretch") {

    init {
        manip = ManipScale(this)
        selectRect = ToolSelectRect(this)
        cur = manip!!
        default = manip!!
    }

    override fun handleHover(x: Int, y: Int, mask: MASK): Boolean {
        if (!cur.hasMouseCapture()) setCurrentTool(manip!!)
        return cur.handleHover(x, y, mask)
    }

    override fun handleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        mouseDown = true
        ViewerWindow.instance.pickAsync(x, y, mask, Companion::pickCallback)
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        mouseDown = false
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleMiddleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        manip!!.handleMiddleMouseDown(x, y, mask)
        return handleMouseDown(x, y, mask)
    }

    override fun handleMiddleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        manip!!.handleMiddleMouseUp(x, y, mask)
        return handleMouseUp(x, y, mask)
    }

    override fun getOverrideTool(mask: MASK): Tool? = when (mask) {
        MASK_CONTROL -> ToolCompRotate
        else         -> super.getOverrideTool(mask)
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: MASK): Boolean {
        if (!manip!!.getSelection().isEmpty() && manip!!.getHighlightedPart() == Manip.NO_PART) {
            FloaterReg.showInstance("build", "Contents")
            return true
        }
        return handleMouseDown(x, y, mask)
    }

    override fun render() {
        cur.render()
        if (cur !== manip) {
            TODO("GPU: LLGLDepthTest(GL_TRUE, GL_FALSE)")
            manip!!.renderGuidelines()
        }
    }

    companion object {
        @JvmStatic fun pickCallback(pickInfo: PickInfo) {
            val hitObj = pickInfo.getObject()
            ToolCompScale.manip!!.highlightManipulators(pickInfo.mousePt.x, pickInfo.mousePt.y)
            if (!ToolCompScale.mouseDown) {
                ToolCompScale.selectRect!!.handleObjectSelection(
                    pickInfo, Agent.savedSettings.getBool("EditLinkedParts"), false
                )
                return
            }
            if (hitObj != null || ToolCompScale.manip!!.getHighlightedPart() != Manip.NO_PART) {
                if (ToolCompScale.manip!!.getSelection().getObjectCount() > 0) {
                    EditMenuHandler.gEditMenuHandler = SelectMgr.instance
                }
                if (ToolCompScale.manip!!.getHighlightedPart() != Manip.NO_PART) {
                    ToolCompScale.setCurrentTool(ToolCompScale.manip!!)
                    ToolCompScale.manip!!.handleMouseDownOnPart(pickInfo.mousePt.x, pickInfo.mousePt.y, pickInfo.keyMask)
                } else {
                    ToolCompScale.setCurrentTool(ToolCompScale.selectRect!!)
                    ToolCompScale.selectRect!!.handlePick(pickInfo)
                }
            } else {
                ToolCompScale.setCurrentTool(ToolCompScale.selectRect!!)
                ToolCompScale.selectRect!!.handlePick(pickInfo)
            }
        }
    }
}

object ToolCompRotate : ToolComposite("Rotate") {

    init {
        manip = ManipRotate(this)
        selectRect = ToolSelectRect(this)
        cur = manip!!
        default = manip!!
    }

    override fun handleHover(x: Int, y: Int, mask: MASK): Boolean {
        if (!cur.hasMouseCapture()) setCurrentTool(manip!!)
        return cur.handleHover(x, y, mask)
    }

    override fun handleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        mouseDown = true
        ViewerWindow.instance.pickAsync(x, y, mask, Companion::pickCallback)
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        mouseDown = false
        return super.handleMouseUp(x, y, mask)
    }

    override fun getOverrideTool(mask: MASK): Tool? = when (mask) {
        MASK_CONTROL or MASK_SHIFT -> ToolCompScale
        else                       -> super.getOverrideTool(mask)
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: MASK): Boolean {
        if (!manip!!.getSelection().isEmpty() && manip!!.getHighlightedPart() == Manip.NO_PART) {
            FloaterReg.showInstance("build", "Contents")
            return true
        }
        return handleMouseDown(x, y, mask)
    }

    override fun render() {
        cur.render()
        if (cur !== manip) {
            TODO("GPU: LLGLDepthTest(GL_TRUE, GL_FALSE)")
            manip!!.renderGuidelines()
        }
    }

    companion object {
        @JvmStatic fun pickCallback(pickInfo: PickInfo) {
            val hitObj = pickInfo.getObject()
            ToolCompRotate.manip!!.highlightManipulators(pickInfo.mousePt.x, pickInfo.mousePt.y)
            if (!ToolCompRotate.mouseDown) {
                ToolCompRotate.selectRect!!.handleObjectSelection(
                    pickInfo, Agent.savedSettings.getBool("EditLinkedParts"), false
                )
                return
            }
            if (hitObj != null || ToolCompRotate.manip!!.getHighlightedPart() != Manip.NO_PART) {
                if (ToolCompRotate.manip!!.getSelection().getObjectCount() > 0) {
                    EditMenuHandler.gEditMenuHandler = SelectMgr.instance
                }
                if (ToolCompRotate.manip!!.getHighlightedPart() != Manip.NO_PART) {
                    ToolCompRotate.setCurrentTool(ToolCompRotate.manip!!)
                    ToolCompRotate.manip!!.handleMouseDownOnPart(pickInfo.mousePt.x, pickInfo.mousePt.y, pickInfo.keyMask)
                } else {
                    ToolCompRotate.setCurrentTool(ToolCompRotate.selectRect!!)
                    ToolCompRotate.selectRect!!.handlePick(pickInfo)
                }
            } else {
                ToolCompRotate.setCurrentTool(ToolCompRotate.selectRect!!)
                ToolCompRotate.selectRect!!.handlePick(pickInfo)
            }
        }
    }
}

object ToolCompCreate : ToolComposite("Create") {

    private val placer = ToolPlacer()
    private var objectPlacedOnMouseDown: Boolean = false

    init {
        selectRect = ToolSelectRect(this)
        cur = placer
        default = placer
    }

    override fun handleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        var handled = false
        mouseDown = true
        if (mask == MASK_SHIFT || mask == MASK_CONTROL) {
            ViewerWindow.instance.pickAsync(x, y, mask, Companion::pickCallback)
            handled = true
        } else {
            setCurrentTool(placer)
            handled = placer.placeObject(x, y, mask)
        }
        objectPlacedOnMouseDown = true
        return handled
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: MASK): Boolean = handleMouseDown(x, y, mask)

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        var handled = false
        if (mouseDown && !objectPlacedOnMouseDown && mask != MASK_SHIFT && mask != MASK_CONTROL) {
            setCurrentTool(placer)
            handled = placer.placeObject(x, y, mask)
        }
        objectPlacedOnMouseDown = false
        mouseDown = false
        if (!handled) handled = super.handleMouseUp(x, y, mask)
        return handled
    }

    companion object {
        @JvmStatic fun pickCallback(pickInfo: PickInfo) {
            val mask = (pickInfo.keyMask and MASK_SHIFT.inv()) and MASK_CONTROL.inv()
            ToolCompCreate.setCurrentTool(ToolCompCreate.selectRect!!)
            ToolCompCreate.selectRect!!.handlePick(pickInfo)
        }
    }
}

object ToolCompGun : ToolComposite("Mouselook") {

    private val gun: ToolGun = ToolGun(this)
    private val grab: ToolGrabBase = ToolGrabBase(this)
    private val nullToolRef: Tool = nullTool

    init {
        setCurrentTool(gun)
        default = gun
    }

    override fun handleHover(x: Int, y: Int, mask: MASK): Boolean {
        if (cur === nullToolRef && !PopupMenuView.instance.isVisible()) {
            SelectMgr.instance.deselectAll()
            setCurrentTool(grab)
        }
        cur.handleHover(x, y, mask)
        if (!ViewerWindow.instance.isLeftMouseDown()) {
            if (cur === gun && (mask and MASK_ALT) != 0) {
                setCurrentTool(grab)
            } else if (cur === grab && (mask and MASK_ALT) == 0) {
                setCurrentTool(gun)
                setMouseCapture(true)
            }
        }
        return true
    }

    override fun handleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        if (Agent.instance.leftButtonGrabbed() && ViewerInput.instance.isLMouseHandlingDefault(MODE_FIRST_PERSON)) {
            Agent.instance.setControlFlags(AGENT_CONTROL_ML_LBUTTON_DOWN)
            return false
        }
        gGrabTransientTool = this
        ToolMgr.instance.getCurrentToolset().selectTool(grab)
        return ToolGrab.instance.handleMouseDown(x, y, mask)
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: MASK): Boolean {
        if (Agent.instance.leftButtonGrabbed() && ViewerInput.instance.isLMouseHandlingDefault(MODE_FIRST_PERSON)) {
            Agent.instance.setControlFlags(AGENT_CONTROL_ML_LBUTTON_DOWN)
            return false
        }
        gGrabTransientTool = this
        ToolMgr.instance.getCurrentToolset().selectTool(grab)
        return ToolGrab.instance.handleDoubleClick(x, y, mask)
    }

    override fun handleRightMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        if ((Keyboard.instance.currentMask(true) and MASK_ALT) == 0) {
            val fovValues = Agent.savedSettings.getVector3("_NACL_MLFovValues").toMutableList()
            val cameraAngle = Agent.savedSettings.getFloat("CameraAngle")
            fovValues[0] = cameraAngle
            fovValues[2] = 1.0f
            Agent.savedSettings.setVector3("_NACL_MLFovValues", fovValues)
            Agent.savedSettings.setFloat("CameraAngle", fovValues[1])
            return true
        }
        return !Agent.savedSettings.getBool("FSEnableRightclickMenuInMouselook")
    }

    override fun handleRightMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        val fovValues = Agent.savedSettings.getVector3("_NACL_MLFovValues").toMutableList()
        val cameraAngle = Agent.savedSettings.getFloat("CameraAngle")
        if (fovValues[2] == 1.0f) {
            fovValues[1] = cameraAngle
            fovValues[2] = 0.0f
            Agent.savedSettings.setVector3("_NACL_MLFovValues", fovValues)
            Agent.savedSettings.setFloat("CameraAngle", fovValues[0])
        }
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        if (ViewerInput.instance.isLMouseHandlingDefault(MODE_FIRST_PERSON)) {
            Agent.instance.setControlFlags(AGENT_CONTROL_ML_LBUTTON_UP)
        }
        setCurrentTool(gun)
        return true
    }

    override fun onMouseCaptureLost() {
        if (composite != null) {
            composite!!.onMouseCaptureLost()
            return
        }
        cur.onMouseCaptureLost()
    }

    override fun handleSelect() {
        super.handleSelect()
        setMouseCapture(true)
    }

    override fun handleDeselect() {
        super.handleDeselect()
        setMouseCapture(false)
    }

    override fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        val fovValues = Agent.savedSettings.getVector3("_NACL_MLFovValues").toMutableList()
        fovValues[1] = Agent.savedSettings.getFloat("CameraAngle")
        if (fovValues[2] > 0.0f) {
            fovValues[1] = (fovValues[1] + clicks * 0.1f)
                .coerceIn(ViewerCamera.instance.minView, ViewerCamera.instance.maxView)
            Agent.savedSettings.setVector3("_NACL_MLFovValues", fovValues)
            Agent.savedSettings.setFloat("CameraAngle", fovValues[1])
        } else if (clicks > 0 && Agent.savedSettings.getBool("FSScrollWheelExitsMouselook")) {
            AgentCamera.instance.changeCameraToDefault()
        }
        return true
    }

    override fun getOverrideTool(mask: MASK): Tool? = null
}

class ToolCompPose : ToolComposite("Pose") {

    private val poseManip: FSManipRotateJoint = FSManipRotateJoint(this)
    private var poserFloater: FSFloaterPoser? = null

    init {
        cur = poseManip
        default = poseManip
    }

    companion object {
        private val _instance by lazy { ToolCompPose() }
        fun getInstance(): ToolCompPose = _instance

        @JvmStatic fun pickCallback(pickInfo: PickInfo) {
            val self = getInstance()
            val manip = self.poseManip
            manip.highlightManipulators(pickInfo.mousePt.x, pickInfo.mousePt.y)
            if (!self.mouseDown) return
            if (manip.getHighlightedPart() != Manip.NO_PART) {
                self.setCurrentTool(manip)
                manip.handleMouseDownOnPart(pickInfo.mousePt.x, pickInfo.mousePt.y, pickInfo.keyMask)
            }
        }
    }

    fun setAvatar(avatar: VOAvatar) = poseManip.setAvatar(avatar)
    fun setJoint(joint: Joint) = poseManip.setJoint(joint)
    fun setReferenceFrame(frame: EPoserReferenceFrame) = poseManip.setReferenceFrame(frame)
    fun setPoserFloater(poser: FSFloaterPoser) { poserFloater = poser }
    fun getPoserFloater(): FSFloaterPoser? = poserFloater

    override fun handleHover(x: Int, y: Int, mask: MASK): Boolean {
        if (!cur.hasMouseCapture()) setCurrentTool(poseManip)
        return cur.handleHover(x, y, mask)
    }

    override fun handleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        mouseDown = true
        ViewerWindow.instance.pickAsync(x, y, mask, Companion::pickCallback)
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        mouseDown = false
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: MASK): Boolean {
        if (!poseManip.getSelection().isEmpty() && poseManip.getHighlightedPart() == Manip.NO_PART) {
            poserFloater = FloaterReg.showInstance("fs_poser") as? FSFloaterPoser
            return true
        }
        return handleMouseDown(x, y, mask)
    }

    override fun render() {
        cur.render()
        if (cur !== poseManip) {
            poseManip.renderGuidelines()
            TODO("GPU: LLGLDepthTest(GL_TRUE, GL_FALSE)")
        }
    }

    override fun getOverrideTool(mask: MASK): Tool? = when (mask) {
        MASK_CONTROL -> ToolCompPoseTranslate.getInstance()
        else         -> super.getOverrideTool(mask)
    }
}

class ToolCompPoseTranslate : ToolComposite("PoseTranslate") {

    private val poseManip: FSManipTranslateJoint = FSManipTranslateJoint(this)
    private var poserFloater: FSFloaterPoser? = null

    init {
        cur = poseManip
        default = poseManip
    }

    companion object {
        private val _instance by lazy { ToolCompPoseTranslate() }
        fun getInstance(): ToolCompPoseTranslate = _instance

        @JvmStatic fun pickCallback(pickInfo: PickInfo) {
            val self = getInstance()
            val manip = self.poseManip
            manip.highlightManipulators(pickInfo.mousePt.x, pickInfo.mousePt.y)
            if (!self.mouseDown) return
            if (manip.getHighlightedPart() != Manip.NO_PART) {
                self.setCurrentTool(manip)
                manip.handleMouseDownOnPart(pickInfo.mousePt.x, pickInfo.mousePt.y, pickInfo.keyMask)
            }
        }
    }

    fun setAvatar(avatar: VOAvatar) = poseManip.setAvatar(avatar)
    fun setJoint(joint: Joint) = poseManip.setJoint(joint)
    fun setReferenceFrame(frame: EPoserReferenceFrame) = poseManip.setReferenceFrame(frame)
    fun setPoserFloater(poser: FSFloaterPoser) { poserFloater = poser }
    fun getPoserFloater(): FSFloaterPoser? = poserFloater

    override fun handleHover(x: Int, y: Int, mask: MASK): Boolean {
        if (!cur.hasMouseCapture()) setCurrentTool(poseManip)
        return cur.handleHover(x, y, mask)
    }

    override fun handleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        mouseDown = true
        ViewerWindow.instance.pickAsync(x, y, mask, Companion::pickCallback)
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        mouseDown = false
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: MASK): Boolean {
        if (!poseManip.getSelection().isEmpty() && poseManip.getHighlightedPart() == Manip.NO_PART) {
            poserFloater = FloaterReg.showInstance("fs_poser") as? FSFloaterPoser
            return true
        }
        return handleMouseDown(x, y, mask)
    }

    override fun render() {
        cur.render()
        if (cur !== poseManip) {
            poseManip.renderGuidelines()
            TODO("GPU: LLGLDepthTest(GL_TRUE, GL_FALSE)")
        }
    }

    override fun getOverrideTool(mask: MASK): Tool? = when (mask) {
        MASK_CONTROL or MASK_SHIFT -> ToolCompScale
        else                       -> super.getOverrideTool(mask)
    }
}
