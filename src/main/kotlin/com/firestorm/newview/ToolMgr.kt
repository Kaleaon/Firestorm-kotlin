package com.firestorm.newview

// Key-modifier mask constants mirroring MASK_* from llkeyboard.h
const val MASK_NONE: Int = 0x0000
const val MASK_CONTROL: Int = 0x0001
const val MASK_SHIFT: Int = 0x0002
const val MASK_ALT: Int = 0x0004

const val MASK_VERTICAL: Int = MASK_CONTROL
const val MASK_SPIN: Int = MASK_CONTROL or MASK_SHIFT
const val MASK_ZOOM: Int = MASK_NONE
const val MASK_ORBIT: Int = MASK_CONTROL
const val MASK_PAN: Int = MASK_CONTROL or MASK_SHIFT
const val MASK_COPY: Int = MASK_SHIFT

// Global toolset references – populated during ToolMgr initialisation.
var gBasicToolset: Toolset? = null
var gCameraToolset: Toolset? = null
var gMouselookToolset: Toolset? = null
var gFaceEditToolset: Toolset? = null
var gPoserToolset: Toolset? = null

// Null tool used when the application is not active to suppress hover processing.
var gToolNull: Tool? = null

object ToolMgr {

    private var baseTool: Tool? = null
    private var savedTool: Tool? = null
    private var transientTool: Tool? = null
    private var overrideTool: Tool? = null
    private var selectedTool: Tool? = null
    private var currentToolset: Toolset? = null

    init {
        gToolNull = Tool("Null")
        setCurrentTool(gToolNull!!)

        gBasicToolset = Toolset()
        gCameraToolset = Toolset()
        gMouselookToolset = Toolset().apply { showFloaterTools = false }
        gFaceEditToolset = Toolset().apply { showFloaterTools = false }
        gPoserToolset = Toolset().apply { showFloaterTools = false }
    }

    fun initTools() {
        // Guard: only run once.
        if (gBasicToolset!!.toolList.isNotEmpty()) return

        gBasicToolset!!.apply {
            addTool(ToolPie)
            addTool(ToolCamera)
        }
        gCameraToolset!!.addTool(ToolCamera)
        gMouselookToolset!!.addTool(TODO("GPU: add ToolCompGun singleton") as Tool)
        gFaceEditToolset!!.addTool(ToolCamera)

        setCurrentToolset(gBasicToolset!!)
        gBasicToolset!!.selectTool(ToolPie)
    }

    fun getCurrentTool(): Tool? {
        val overrideMask: Int = TODO("APR: query keyboard current mask") as Int

        val curTool: Tool? = when {
            transientTool != null -> {
                overrideTool = null
                transientTool
            }
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
                // When returning from Camera to Pie while the inspect floater is open,
                // re-activate the inspect composite tool rather than plain Pie.
                val inspectInstance: Any? = TODO("APR: FloaterReg.getTypedInstance(\"inspect\")")
                if (ToolComp.isToolCameraActive() &&
                    prevTool === ToolCamera &&
                    curTool === ToolPie &&
                    inspectInstance != null
                ) {
                    setTransientTool(ToolComp.inspectInstance())
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
        TODO("APR: ViewerParcelMgr.allowAgentBuild() && RlvActions.canBuild()")
    }

    fun buildEnabledOrActive(): Boolean {
        TODO("APR: FloaterReg.instanceVisible(\"build\") || canEdit()")
    }

    fun inBuildMode(): Boolean {
        TODO("APR: inEdit() && !AgentCamera.cameraMouselook() && currentToolset != gFaceEditToolset")
    }

    fun toggleBuildMode(paramName: String) {
        TODO("APR: toggle build floater / enter-leave build mode")
    }

    fun enterBuildMode(verifyCanedit: Boolean = false) {
        TODO("APR: show build floater, reset camera, select create tool")
    }

    fun leaveBuildMode() {
        TODO("APR: hide build floater, reset camera")
    }

    fun canAccessMarketplace(): Boolean {
        TODO("APR: MarketplaceData.getSLMStatus() != MARKET_PLACE_NOT_MIGRATED_MERCHANT")
    }

    fun toggleMarketplace(paramName: String) {
        if (paramName != "marketplace" || !canAccessMarketplace()) return
        TODO("APR: FloaterReg.toggleInstanceOrBringToFront(\"marketplace_listings\")")
    }

    fun setTransientTool(tool: Tool?) {
        if (tool == null) {
            clearTransientTool()
            return
        }
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
