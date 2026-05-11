package com.firestorm.newview

// Key-modifier mask constants mirroring MASK_* from llkeyboard.h.
// Redeclared here rather than relying on ToolComp to avoid circular dependency;
// ToolComp already declares identical values but only within its own file scope.
const val MASK_NONE: Int = 0x0000
const val MASK_CTRL: Int = 0x0001
const val MASK_SH: Int = 0x0002
const val MASK_A: Int = 0x0004

const val MASK_VERTICAL: Int = MASK_CTRL
const val MASK_SPIN: Int = MASK_CTRL or MASK_SH
const val MASK_ZOOM: Int = MASK_NONE
const val MASK_ORBIT_KEY: Int = MASK_CTRL
const val MASK_PAN_KEY: Int = MASK_CTRL or MASK_SH
const val MASK_COPY: Int = MASK_SH

// Global toolset singletons – populated during ToolMgr.init().
var gBasicToolset: Toolset? = null
var gCameraToolset: Toolset? = null
var gMouselookToolset: Toolset? = null
var gFaceEditToolset: Toolset? = null
var gPoserToolset: Toolset? = null

// Null tool: absorbs all events when the application is not focused.
var gToolNull: Tool? = null

object ToolMgr {

    private var baseTool: Tool? = null
    @Suppress("unused") private var savedTool: Tool? = null
    private var transientTool: Tool? = null
    private var overrideTool: Tool? = null
    private var selectedTool: Tool? = null
    private var currentToolset: Toolset? = null

    init {
        gToolNull = Tool("Null")
        setCurrentTool(gToolNull!!)

        gBasicToolset = Toolset()
        gCameraToolset = Toolset()
        gMouselookToolset = Toolset().also { it.showFloaterTools = false }
        gFaceEditToolset = Toolset().also { it.showFloaterTools = false }
        gPoserToolset = Toolset().also { it.showFloaterTools = false }
    }

    fun initTools() {
        if (gBasicToolset!!.toolList.isNotEmpty()) return

        gBasicToolset!!.apply {
            addTool(ToolPie)
            addTool(ToolCamera)
        }
        gCameraToolset!!.addTool(ToolCamera)
        gMouselookToolset!!.addTool(ToolCompGun)
        gFaceEditToolset!!.addTool(ToolCamera)

        setCurrentToolset(gBasicToolset!!)
        gBasicToolset!!.selectTool(ToolPie)
    }

    fun getCurrentTool(): Tool? {
        val overrideMask: Int = TODO("APR: gKeyboard?.currentMask(true) ?: 0") as Int

        val curTool: Tool? = when {
            transientTool != null -> { overrideTool = null; transientTool }
            selectedTool?.hasMouseCapture() == true -> selectedTool
            else -> {
                overrideTool = baseTool?.getOverrideTool(overrideMask)
                overrideTool ?: baseTool
            }
        }

        val prevTool = selectedTool
        selectedTool = curTool

        if (prevTool != curTool) {
            prevTool?.handleDeselect()
            if (curTool != null) {
                // When returning from Camera to Pie while FloaterInspect is open,
                // restore the inspect composite rather than plain Pie.
                val inspectInstance: FloaterInspect? = TODO("APR: FloaterReg.getTypedInstance(\"inspect\")") as FloaterInspect?
                if (ToolCompInspect.isToolCameraActive() &&
                    prevTool === ToolCamera &&
                    curTool === ToolPie &&
                    inspectInstance?.isVisible() == true
                ) {
                    setTransientTool(ToolCompInspect)
                } else {
                    curTool.handleSelect()
                }
            }
        }

        return selectedTool
    }

    fun getBaseTool(): Tool? = baseTool

    fun inEdit(): Boolean = baseTool !== ToolPie && baseTool !== gToolNull

    fun canEdit(): Boolean {
        TODO("APR: ViewerParcelMgr.getInstance().allowAgentBuild() && RlvActions.canBuild()")
    }

    fun buildEnabledOrActive(): Boolean {
        TODO("APR: FloaterReg.instanceVisible(\"build\") || canEdit()")
    }

    fun inBuildMode(): Boolean {
        TODO("APR: inEdit() && !AgentCamera.cameraMouselook() && currentToolset != gFaceEditToolset")
    }

    fun toggleBuildMode(paramName: String) {
        TODO("APR: show/hide build floater; call enterBuildMode or leaveBuildMode")
    }

    fun enterBuildMode(verifyCanedit: Boolean = false) {
        TODO("APR: show build floater, pull out of mouselook/appearance, zoom in, reset view, set create tool")
    }

    fun leaveBuildMode() {
        TODO("APR: close build floater, reset camera view")
    }

    fun canAccessMarketplace(): Boolean {
        TODO("APR: MarketplaceData.instance().getSLMStatus() != MARKET_PLACE_NOT_MIGRATED_MERCHANT")
    }

    fun toggleMarketplace(paramName: String) {
        if (paramName != "marketplace" || !canAccessMarketplace()) return
        TODO("APR: FloaterReg.toggleInstanceOrBringToFront(\"marketplace_listings\")")
    }

    fun setTransientTool(tool: Tool?) {
        if (tool == null) { clearTransientTool(); return }
        transientTool = tool
        updateToolStatus()
    }

    fun clearTransientTool() {
        transientTool = null
        updateToolStatus()
    }

    fun usingTransientTool(): Boolean = transientTool != null

    fun setCurrentToolset(current: Toolset) {
        if (current === currentToolset) {
            setCurrentTool(current.getSelectedTool()!!)
            return
        }
        selectedTool?.handleDeselect()
        currentToolset = current
        currentToolset!!.selectFirstTool()
        setCurrentTool(currentToolset!!.getSelectedTool()!!)
    }

    fun getCurrentToolset(): Toolset? = currentToolset

    fun onAppFocusGained() {
        selectedTool?.handleSelect()
        updateToolStatus()
    }

    fun onAppFocusLost() {
        selectedTool?.handleDeselect()
        updateToolStatus()
    }

    fun clearSavedTool() {
        savedTool = null
    }

    internal fun setCurrentTool(tool: Tool) {
        transientTool = null
        baseTool = tool
        updateToolStatus()
        savedTool = null
    }

    private fun updateToolStatus() {
        getCurrentTool()
    }
}

// Base class for all interactive viewer tools.  Mirrors LLTool from lltool.h.
open class Tool(private val name: String) {

    private var mouseCapture: Boolean = false

    open fun getName(): String = name

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleHover(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean = false
    open fun handleScrollHWheel(x: Int, y: Int, clicks: Int): Boolean = false
    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleAnyMouseClick(x: Int, y: Int, mask: Int, clickType: Int, down: Boolean): Boolean = false

    open fun handleSelect() {}
    open fun handleDeselect() {}
    open fun onMouseCaptureLost() {}
    open fun stopEditing() {}
    open fun render() {}

    open fun getOverrideTool(mask: Int): Tool? = null

    fun hasMouseCapture(): Boolean = mouseCapture

    fun setMouseCapture(capture: Boolean) {
        if (capture == mouseCapture) return
        mouseCapture = capture
        if (!capture) onMouseCaptureLost()
    }
}

class Toolset {

    var showFloaterTools: Boolean = true
    internal val toolList: MutableList<Tool> = mutableListOf()
    private var selectedTool: Tool? = null

    fun getSelectedTool(): Tool? = selectedTool

    fun addTool(tool: Tool) {
        toolList.add(tool)
        if (selectedTool == null) selectedTool = tool
    }

    fun selectTool(tool: Tool) {
        selectedTool = tool
        ToolMgr.setCurrentTool(tool)
    }

    fun selectToolByIndex(index: Int) {
        val tool = toolList.getOrNull(index) ?: return
        selectedTool = tool
        ToolMgr.setCurrentTool(tool)
    }

    fun selectFirstTool() {
        selectedTool = toolList.firstOrNull()
        selectedTool?.let { ToolMgr.setCurrentTool(it) }
    }

    fun selectNextTool() {
        val idx = toolList.indexOf(selectedTool)
        if (idx >= 0 && idx + 1 < toolList.size) {
            selectedTool = toolList[idx + 1]
            ToolMgr.setCurrentTool(selectedTool!!)
        } else {
            selectFirstTool()
        }
    }

    fun selectPrevTool() {
        val idx = toolList.indexOf(selectedTool)
        if (idx > 0) {
            selectedTool = toolList[idx - 1]
            ToolMgr.setCurrentTool(selectedTool!!)
        } else if (toolList.isNotEmpty()) {
            selectToolByIndex(toolList.size - 1)
        }
    }

    fun isToolSelected(index: Int): Boolean = toolList.getOrNull(index) === selectedTool

    fun handleScrollWheel(clicks: Int) {
        TODO("APR: delegate scroll-wheel to selected tool")
    }
}
