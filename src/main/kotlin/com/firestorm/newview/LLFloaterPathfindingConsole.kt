package com.firestorm.newview

import java.util.UUID

class LLFloaterPathfindingConsole private constructor(seed: LLSD) : LLFloater(seed) {

    enum class EConsoleState {
        UNKNOWN,
        LIBRARY_NOT_IMPLEMENTED,
        REGION_NOT_ENABLED,
        REGION_LOADING,
        CHECKING_VERSION,
        DOWNLOADING,
        HAS_NAV_MESH,
        ERROR
    }

    companion object {
        private const val XUI_RENDER_HEATMAP_NONE = 0
        private const val XUI_RENDER_HEATMAP_A = 1
        private const val XUI_RENDER_HEATMAP_B = 2
        private const val XUI_RENDER_HEATMAP_C = 3
        private const val XUI_RENDER_HEATMAP_D = 4

        private const val XUI_CHARACTER_TYPE_NONE = 0
        private const val XUI_CHARACTER_TYPE_A = 1
        private const val XUI_CHARACTER_TYPE_B = 2
        private const val XUI_CHARACTER_TYPE_C = 3
        private const val XUI_CHARACTER_TYPE_D = 4

        private const val XUI_VIEW_TAB_INDEX = 0
        private const val XUI_TEST_TAB_INDEX = 1

        private const val CONTROL_NAME_RETRIEVE_NEIGHBOR = "PathfindingRetrieveNeighboringRegion"
        private const val CONTROL_NAME_WALKABLE_OBJECTS = "PathfindingWalkable"
        private const val CONTROL_NAME_STATIC_OBSTACLE_OBJECTS = "PathfindingObstacle"
        private const val CONTROL_NAME_MATERIAL_VOLUMES = "PathfindingMaterial"
        private const val CONTROL_NAME_EXCLUSION_VOLUMES = "PathfindingExclusion"
        private const val CONTROL_NAME_INTERIOR_EDGE = "PathfindingConnectedEdge"
        private const val CONTROL_NAME_EXTERIOR_EDGE = "PathfindingBoundaryEdge"
        private const val CONTROL_NAME_HEATMAP_MIN = "PathfindingHeatColorBase"
        private const val CONTROL_NAME_HEATMAP_MAX = "PathfindingHeatColorMax"
        private const val CONTROL_NAME_NAVMESH_FACE = "PathfindingFaceColor"
        private const val CONTROL_NAME_TEST_PATH_VALID_END = "PathfindingTestPathValidEndColor"
        private const val CONTROL_NAME_TEST_PATH_INVALID_END = "PathfindingTestPathInvalidEndColor"
        private const val CONTROL_NAME_TEST_PATH = "PathfindingTestPathColor"
        private const val CONTROL_NAME_WATER = "PathfindingWaterColor"

        var sInstanceHandle: LLHandle<LLFloaterPathfindingConsole>? = null

        fun getInstanceHandle(): LLHandle<LLFloaterPathfindingConsole>? {
            if (sInstanceHandle?.isDead() != false) {
                val floaterInstance = LLFloaterReg.findTypedInstance<LLFloaterPathfindingConsole>("pathfinding_console")
                if (floaterInstance != null) {
                    sInstanceHandle = floaterInstance.selfHandle
                }
            }
            return sInstanceHandle
        }
    }

    private var selfHandle: LLRootHandle<LLFloaterPathfindingConsole> = LLRootHandle()

    private var viewTestTabContainer: LLTabContainer? = null
    private var viewTab: LLPanel? = null
    private var showLabel: LLTextBase? = null
    private var showWorldCheckBox: LLCheckBoxCtrl? = null
    private var showWorldMovablesOnlyCheckBox: LLCheckBoxCtrl? = null
    private var showNavMeshCheckBox: LLCheckBoxCtrl? = null
    private var showNavMeshWalkabilityLabel: LLTextBase? = null
    private var showNavMeshWalkabilityComboBox: LLComboBox? = null
    private var showWalkablesCheckBox: LLCheckBoxCtrl? = null
    private var showStaticObstaclesCheckBox: LLCheckBoxCtrl? = null
    private var showMaterialVolumesCheckBox: LLCheckBoxCtrl? = null
    private var showExclusionVolumesCheckBox: LLCheckBoxCtrl? = null
    private var showRenderWaterPlaneCheckBox: LLCheckBoxCtrl? = null
    private var showXRayCheckBox: LLCheckBoxCtrl? = null
    private var pathfindingViewerStatus: LLTextBase? = null
    private var pathfindingSimulatorStatus: LLTextBase? = null
    private var testTab: LLPanel? = null
    private var ctrlClickLabel: LLTextBase? = null
    private var shiftClickLabel: LLTextBase? = null
    private var characterWidthLabel: LLTextBase? = null
    private var characterWidthUnitLabel: LLTextBase? = null
    private var characterWidthSlider: LLSliderCtrl? = null
    private var characterTypeLabel: LLTextBase? = null
    private var characterTypeComboBox: LLComboBox? = null
    private var pathTestingStatus: LLTextBase? = null
    private var clearPathButton: LLButton? = null

    private var errorColor: LLColor4 = LLColor4()
    private var warningColor: LLColor4 = LLColor4()

    private var navMeshZoneSlot: (() -> Unit)? = null
    private val navMeshZone: LLPathfindingNavMeshZone = LLPathfindingNavMeshZone()
    private var isNavMeshUpdating: Boolean = false

    private var regionBoundarySlot: (() -> Unit)? = null
    private var teleportFailedSlot: (() -> Unit)? = null
    private var pathEventSlot: (() -> Unit)? = null

    private var pathfindingToolset: LLToolset? = null
    private var savedToolset: LLToolset? = null

    private var savedSettingRetrieveNeighborSlot: (() -> Unit)? = null
    private var savedSettingWalkableSlot: (() -> Unit)? = null
    private var savedSettingStaticObstacleSlot: (() -> Unit)? = null
    private var savedSettingMaterialVolumeSlot: (() -> Unit)? = null
    private var savedSettingExclusionVolumeSlot: (() -> Unit)? = null
    private var savedSettingInteriorEdgeSlot: (() -> Unit)? = null
    private var savedSettingExteriorEdgeSlot: (() -> Unit)? = null
    private var savedSettingHeatmapMinSlot: (() -> Unit)? = null
    private var savedSettingHeatmapMaxSlot: (() -> Unit)? = null
    private var savedSettingNavMeshFaceSlot: (() -> Unit)? = null
    private var savedSettingTestPathValidEndSlot: (() -> Unit)? = null
    private var savedSettingTestPathInvalidEndSlot: (() -> Unit)? = null
    private var savedSettingTestPathSlot: (() -> Unit)? = null
    private var savedSettingWaterSlot: (() -> Unit)? = null

    private var consoleState: EConsoleState = EConsoleState.UNKNOWN
    private val renderableRestoreList: MutableList<UInt> = mutableListOf()

    init {
        selfHandle.bind(this)
    }

    override fun postBuild(): Boolean {
        viewTestTabContainer = findChild("view_test_tab_container")!!
        viewTestTabContainer!!.setCommitCallback { onTabSwitch() }

        viewTab = findChild("view_panel")!!
        showLabel = findChild("show_label")!!

        showWorldCheckBox = findChild("show_world")!!
        showWorldCheckBox!!.setCommitCallback { onShowWorldSet() }

        showWorldMovablesOnlyCheckBox = findChild("show_world_movables_only")!!
        showWorldMovablesOnlyCheckBox!!.setCommitCallback { onShowWorldMovablesOnlySet() }

        showNavMeshCheckBox = findChild("show_navmesh")!!
        showNavMeshCheckBox!!.setCommitCallback { onShowNavMeshSet() }

        showNavMeshWalkabilityLabel = findChild("show_walkability_label")!!
        showNavMeshWalkabilityComboBox = findChild("show_heatmap_mode")!!
        showNavMeshWalkabilityComboBox!!.setCommitCallback { onShowWalkabilitySet() }

        showWalkablesCheckBox = findChild("show_walkables")!!
        showStaticObstaclesCheckBox = findChild("show_static_obstacles")!!
        showMaterialVolumesCheckBox = findChild("show_material_volumes")!!
        showExclusionVolumesCheckBox = findChild("show_exclusion_volumes")!!
        showRenderWaterPlaneCheckBox = findChild("show_water_plane")!!
        showXRayCheckBox = findChild("show_xray")!!
        testTab = findChild("test_panel")!!
        pathfindingViewerStatus = findChild("pathfinding_viewer_status")!!
        pathfindingSimulatorStatus = findChild("pathfinding_simulator_status")!!
        ctrlClickLabel = findChild("ctrl_click_label")!!
        shiftClickLabel = findChild("shift_click_label")!!
        characterWidthLabel = findChild("character_width_label")!!

        characterWidthSlider = findChild("character_width")!!
        characterWidthSlider!!.setCommitCallback { onCharacterWidthSet() }

        characterWidthUnitLabel = findChild("character_width_unit_label")!!
        characterTypeLabel = findChild("character_type_label")!!

        characterTypeComboBox = findChild("path_character_type")!!
        characterTypeComboBox!!.setCommitCallback { onCharacterTypeSwitch() }

        pathTestingStatus = findChild("path_test_status")!!

        clearPathButton = findChild("clear_path")!!
        clearPathButton!!.setCommitCallback { onClearPathClicked() }

        errorColor = LLUIColorTable.instance().getColor("PathfindingErrorColor")
        warningColor = LLUIColorTable.instance().getColor("PathfindingWarningColor")

        if (LLPathingLib.getInstance() != null) {
            pathfindingToolset = LLToolset()
            pathfindingToolset!!.addTool(LLPathfindingPathTool.getInstance())
            pathfindingToolset!!.addTool(LLToolCamera.getInstance())
            pathfindingToolset!!.setShowFloaterTools(false)
        }

        updateCharacterWidth()
        updateCharacterType()

        return super.postBuild()
    }

    override fun onOpen(key: LLSD) {
        super.onOpen(key)
        if (LLPathingLib.getInstance() == null) {
            setConsoleState(EConsoleState.LIBRARY_NOT_IMPLEMENTED)
            val args = LLSD()
            args["FEATURE"] = getString("no_havok")
            LLNotificationsUtil.add("NoHavok", args)
        } else {
            if (navMeshZoneSlot == null) {
                val slot = { status: LLPathfindingNavMeshZone.ENavMeshZoneRequestStatus ->
                    handleNavMeshZoneStatus(status)
                }
                navMeshZoneSlot = navMeshZone.registerNavMeshZoneListener(slot)
            }
            isNavMeshUpdating = false
            initializeNavMeshZoneForCurrentRegion()
            registerSavedSettingsListeners()
            fillInColorsForNavMeshVisualization()
        }

        if (regionBoundarySlot == null) {
            regionBoundarySlot = gAgent.addRegionChangedCallback { onRegionBoundaryCross() }
        }

        if (teleportFailedSlot == null) {
            teleportFailedSlot = LLViewerParcelMgr.getInstance().setTeleportFailedCallback { onRegionBoundaryCross() }
        }

        if (pathEventSlot == null) {
            pathEventSlot = LLPathfindingPathTool.getInstance().registerPathEventListener { onPathEvent() }
        }

        setDefaultInputs()
        updatePathTestStatus()

        if (viewTestTabContainer!!.getCurrentPanelIndex() == XUI_TEST_TAB_INDEX) {
            switchIntoTestPathMode()
        }
    }

    override fun onClose(isAppQuitting: Boolean) {
        switchOutOfTestPathMode()

        pathEventSlot = null
        teleportFailedSlot = null
        regionBoundarySlot = null
        navMeshZoneSlot = null

        if (LLPathingLib.getInstance() != null) {
            navMeshZone.disable()
        }
        deregisterSavedSettingsListeners()

        setDefaultInputs()
        setConsoleState(EConsoleState.UNKNOWN)
        cleanupRenderableRestoreItems()

        super.onClose(isAppQuitting)
    }

    fun isRenderNavMesh(): Boolean = showNavMeshCheckBox!!.get()

    fun setRenderNavMesh(isRenderNavMesh: Boolean) {
        showNavMeshCheckBox!!.set(isRenderNavMesh)
        setNavMeshRenderState()
    }

    fun isRenderWalkables(): Boolean = showWalkablesCheckBox!!.get()

    fun setRenderWalkables(isRenderWalkables: Boolean) {
        showWalkablesCheckBox!!.set(isRenderWalkables)
    }

    fun isRenderStaticObstacles(): Boolean = showStaticObstaclesCheckBox!!.get()

    fun setRenderStaticObstacles(isRenderStaticObstacles: Boolean) {
        showStaticObstaclesCheckBox!!.set(isRenderStaticObstacles)
    }

    fun isRenderMaterialVolumes(): Boolean = showMaterialVolumesCheckBox!!.get()

    fun setRenderMaterialVolumes(isRenderMaterialVolumes: Boolean) {
        showMaterialVolumesCheckBox!!.set(isRenderMaterialVolumes)
    }

    fun isRenderExclusionVolumes(): Boolean = showExclusionVolumesCheckBox!!.get()

    fun setRenderExclusionVolumes(isRenderExclusionVolumes: Boolean) {
        showExclusionVolumesCheckBox!!.set(isRenderExclusionVolumes)
    }

    fun isRenderWorld(): Boolean = showWorldCheckBox!!.get()

    fun setRenderWorld(isRenderWorld: Boolean) {
        showWorldCheckBox!!.set(isRenderWorld)
        setWorldRenderState()
    }

    fun isRenderWorldMovablesOnly(): Boolean =
        showWorldCheckBox!!.get() && showWorldMovablesOnlyCheckBox!!.get()

    fun setRenderWorldMovablesOnly(isRenderWorldMovablesOnly: Boolean) {
        showWorldMovablesOnlyCheckBox!!.set(isRenderWorldMovablesOnly)
    }

    fun isRenderWaterPlane(): Boolean = showRenderWaterPlaneCheckBox!!.get()

    fun setRenderWaterPlane(isRenderWaterPlane: Boolean) {
        showRenderWaterPlaneCheckBox!!.set(isRenderWaterPlane)
    }

    fun isRenderXRay(): Boolean = showXRayCheckBox!!.get()

    fun setRenderXRay(isRenderXRay: Boolean) {
        showXRayCheckBox!!.set(isRenderXRay)
    }

    fun isRenderAnyShapes(): Boolean =
        isRenderWalkables() || isRenderStaticObstacles() ||
                isRenderMaterialVolumes() || isRenderExclusionVolumes()

    fun getRenderShapeFlags(): UInt {
        var shapeRenderFlag = 0u
        if (isRenderWalkables()) shapeRenderFlag = shapeRenderFlag or (1u shl LLPathingLib.LLST_WalkableObjects)
        if (isRenderStaticObstacles()) shapeRenderFlag = shapeRenderFlag or (1u shl LLPathingLib.LLST_ObstacleObjects)
        if (isRenderMaterialVolumes()) shapeRenderFlag = shapeRenderFlag or (1u shl LLPathingLib.LLST_MaterialPhantoms)
        if (isRenderExclusionVolumes()) shapeRenderFlag = shapeRenderFlag or (1u shl LLPathingLib.LLST_ExclusionPhantoms)
        return shapeRenderFlag
    }

    fun getRenderHeatmapType(): LLPathingLib.LLPLCharacterType {
        return when (showNavMeshWalkabilityComboBox!!.getValue().asInteger()) {
            XUI_RENDER_HEATMAP_A -> LLPathingLib.LLPLCharacterType.LLPL_CHARACTER_TYPE_A
            XUI_RENDER_HEATMAP_B -> LLPathingLib.LLPLCharacterType.LLPL_CHARACTER_TYPE_B
            XUI_RENDER_HEATMAP_C -> LLPathingLib.LLPLCharacterType.LLPL_CHARACTER_TYPE_C
            XUI_RENDER_HEATMAP_D -> LLPathingLib.LLPLCharacterType.LLPL_CHARACTER_TYPE_D
            else -> LLPathingLib.LLPLCharacterType.LLPL_CHARACTER_TYPE_NONE
        }
    }

    fun setRenderHeatmapType(renderHeatmapType: LLPathingLib.LLPLCharacterType) {
        val comboBoxValue = when (renderHeatmapType) {
            LLPathingLib.LLPLCharacterType.LLPL_CHARACTER_TYPE_A -> XUI_RENDER_HEATMAP_A
            LLPathingLib.LLPLCharacterType.LLPL_CHARACTER_TYPE_B -> XUI_RENDER_HEATMAP_B
            LLPathingLib.LLPLCharacterType.LLPL_CHARACTER_TYPE_C -> XUI_RENDER_HEATMAP_C
            LLPathingLib.LLPLCharacterType.LLPL_CHARACTER_TYPE_D -> XUI_RENDER_HEATMAP_D
            else -> XUI_RENDER_HEATMAP_NONE
        }
        showNavMeshWalkabilityComboBox!!.setValue(LLSD(comboBoxValue))
    }

    fun onRegionBoundaryCross() {
        initializeNavMeshZoneForCurrentRegion()
        setRenderWorld(true)
        setRenderWorldMovablesOnly(false)
    }

    private fun onTabSwitch() {
        if (viewTestTabContainer!!.getCurrentPanelIndex() == XUI_TEST_TAB_INDEX) {
            switchIntoTestPathMode()
        } else {
            switchOutOfTestPathMode()
        }
    }

    private fun onShowWorldSet() {
        setWorldRenderState()
        updateRenderablesObjects()
    }

    private fun onShowWorldMovablesOnlySet() {
        updateRenderablesObjects()
    }

    private fun onShowNavMeshSet() {
        setNavMeshRenderState()
    }

    private fun onShowWalkabilitySet() {
        LLPathingLib.getInstance()?.setNavMeshMaterialType(getRenderHeatmapType())
    }

    private fun onCharacterWidthSet() {
        updateCharacterWidth()
    }

    private fun onCharacterTypeSwitch() {
        updateCharacterType()
    }

    private fun onClearPathClicked() {
        clearPath()
    }

    private fun handleNavMeshZoneStatus(status: LLPathfindingNavMeshZone.ENavMeshZoneRequestStatus) {
        when (status) {
            LLPathfindingNavMeshZone.ENavMeshZoneRequestStatus.UNKNOWN -> setConsoleState(EConsoleState.UNKNOWN)
            LLPathfindingNavMeshZone.ENavMeshZoneRequestStatus.WAITING -> setConsoleState(EConsoleState.REGION_LOADING)
            LLPathfindingNavMeshZone.ENavMeshZoneRequestStatus.CHECKING -> setConsoleState(EConsoleState.CHECKING_VERSION)
            LLPathfindingNavMeshZone.ENavMeshZoneRequestStatus.NEEDS_UPDATE -> {
                isNavMeshUpdating = true
                navMeshZone.refresh()
            }
            LLPathfindingNavMeshZone.ENavMeshZoneRequestStatus.STARTED -> setConsoleState(EConsoleState.DOWNLOADING)
            LLPathfindingNavMeshZone.ENavMeshZoneRequestStatus.COMPLETED -> {
                isNavMeshUpdating = false
                setConsoleState(EConsoleState.HAS_NAV_MESH)
            }
            LLPathfindingNavMeshZone.ENavMeshZoneRequestStatus.NOT_ENABLED -> setConsoleState(EConsoleState.REGION_NOT_ENABLED)
            LLPathfindingNavMeshZone.ENavMeshZoneRequestStatus.ERROR -> setConsoleState(EConsoleState.ERROR)
        }
    }

    private fun onPathEvent() {
        val pathTool = LLPathfindingPathTool.getInstance()
        characterWidthSlider!!.setValue(LLSD(pathTool.getCharacterWidth()))

        val characterType = when (pathTool.getCharacterType()) {
            LLPathfindingPathTool.ECharacterType.CHARACTER_TYPE_A -> XUI_CHARACTER_TYPE_A
            LLPathfindingPathTool.ECharacterType.CHARACTER_TYPE_B -> XUI_CHARACTER_TYPE_B
            LLPathfindingPathTool.ECharacterType.CHARACTER_TYPE_C -> XUI_CHARACTER_TYPE_C
            LLPathfindingPathTool.ECharacterType.CHARACTER_TYPE_D -> XUI_CHARACTER_TYPE_D
            else -> XUI_CHARACTER_TYPE_NONE
        }
        characterTypeComboBox!!.setValue(LLSD(characterType))
        updatePathTestStatus()
    }

    private fun setDefaultInputs() {
        viewTestTabContainer!!.selectTab(XUI_VIEW_TAB_INDEX)
        setRenderWorld(true)
        setRenderWorldMovablesOnly(false)
        setRenderNavMesh(false)
        setRenderWalkables(false)
        setRenderMaterialVolumes(false)
        setRenderStaticObstacles(false)
        setRenderExclusionVolumes(false)
        setRenderWaterPlane(false)
        setRenderXRay(false)
    }

    private fun setConsoleState(state: EConsoleState) {
        consoleState = state
        updateControlsOnConsoleState()
        updateViewerStatusOnConsoleState()
        updateSimulatorStatusOnConsoleState()
    }

    private fun setWorldRenderState() {
        val renderWorld = isRenderWorld()
        showWorldMovablesOnlyCheckBox!!.setEnabled(renderWorld && showWorldCheckBox!!.getEnabled())
        if (!renderWorld) {
            showWorldMovablesOnlyCheckBox!!.set(false)
        }
    }

    private fun setNavMeshRenderState() {
        val renderNavMesh = isRenderNavMesh()
        showNavMeshWalkabilityLabel!!.setEnabled(renderNavMesh)
        showNavMeshWalkabilityComboBox!!.setEnabled(renderNavMesh)
    }

    private fun updateRenderablesObjects() {
        if (isRenderWorldMovablesOnly()) {
            TODO("GPU: gPipeline.hidePermanentObjects(renderableRestoreList)")
        } else {
            cleanupRenderableRestoreItems()
        }
    }

    private fun updateControlsOnConsoleState() {
        when (consoleState) {
            EConsoleState.UNKNOWN,
            EConsoleState.REGION_NOT_ENABLED,
            EConsoleState.REGION_LOADING,
            EConsoleState.LIBRARY_NOT_IMPLEMENTED,
            EConsoleState.CHECKING_VERSION,
            EConsoleState.DOWNLOADING,
            EConsoleState.ERROR -> {
                viewTestTabContainer!!.selectTab(XUI_VIEW_TAB_INDEX)
                viewTab!!.setEnabled(false)
                showLabel!!.setEnabled(false)
                showWorldCheckBox!!.setEnabled(false)
                showWorldMovablesOnlyCheckBox!!.setEnabled(false)
                showNavMeshCheckBox!!.setEnabled(false)
                showNavMeshWalkabilityLabel!!.setEnabled(false)
                showNavMeshWalkabilityComboBox!!.setEnabled(false)
                showWalkablesCheckBox!!.setEnabled(false)
                showStaticObstaclesCheckBox!!.setEnabled(false)
                showMaterialVolumesCheckBox!!.setEnabled(false)
                showExclusionVolumesCheckBox!!.setEnabled(false)
                showRenderWaterPlaneCheckBox!!.setEnabled(false)
                showXRayCheckBox!!.setEnabled(false)
                testTab!!.setEnabled(false)
                ctrlClickLabel!!.setEnabled(false)
                shiftClickLabel!!.setEnabled(false)
                characterWidthLabel!!.setEnabled(false)
                characterWidthUnitLabel!!.setEnabled(false)
                characterWidthSlider!!.setEnabled(false)
                characterTypeLabel!!.setEnabled(false)
                characterTypeComboBox!!.setEnabled(false)
                clearPathButton!!.setEnabled(false)
                clearPath()
            }
            EConsoleState.HAS_NAV_MESH -> {
                viewTab!!.setEnabled(true)
                showLabel!!.setEnabled(true)
                showWorldCheckBox!!.setEnabled(true)
                setWorldRenderState()
                showNavMeshCheckBox!!.setEnabled(true)
                setNavMeshRenderState()
                showWalkablesCheckBox!!.setEnabled(true)
                showStaticObstaclesCheckBox!!.setEnabled(true)
                showMaterialVolumesCheckBox!!.setEnabled(true)
                showExclusionVolumesCheckBox!!.setEnabled(true)
                showRenderWaterPlaneCheckBox!!.setEnabled(true)
                showXRayCheckBox!!.setEnabled(true)
                testTab!!.setEnabled(true)
                ctrlClickLabel!!.setEnabled(true)
                shiftClickLabel!!.setEnabled(true)
                characterWidthLabel!!.setEnabled(true)
                characterWidthUnitLabel!!.setEnabled(true)
                characterWidthSlider!!.setEnabled(true)
                characterTypeLabel!!.setEnabled(true)
                characterTypeComboBox!!.setEnabled(true)
                clearPathButton!!.setEnabled(true)
            }
        }
    }

    private fun updateViewerStatusOnConsoleState() {
        val statusText: String
        val styleColor: LLColor4?
        when (consoleState) {
            EConsoleState.UNKNOWN -> { statusText = getString("navmesh_viewer_status_unknown"); styleColor = errorColor }
            EConsoleState.LIBRARY_NOT_IMPLEMENTED -> { statusText = getString("navmesh_viewer_status_library_not_implemented"); styleColor = errorColor }
            EConsoleState.REGION_NOT_ENABLED -> { statusText = getString("navmesh_viewer_status_region_not_enabled"); styleColor = errorColor }
            EConsoleState.REGION_LOADING -> { statusText = getString("navmesh_viewer_status_region_loading"); styleColor = warningColor }
            EConsoleState.CHECKING_VERSION -> { statusText = getString("navmesh_viewer_status_checking_version"); styleColor = warningColor }
            EConsoleState.DOWNLOADING -> {
                statusText = if (isNavMeshUpdating) getString("navmesh_viewer_status_updating")
                else getString("navmesh_viewer_status_downloading")
                styleColor = warningColor
            }
            EConsoleState.HAS_NAV_MESH -> { statusText = getString("navmesh_viewer_status_has_navmesh"); styleColor = null }
            EConsoleState.ERROR -> { statusText = getString("navmesh_viewer_status_error"); styleColor = errorColor }
        }
        pathfindingViewerStatus!!.setText(statusText, styleColor)
    }

    private fun updateSimulatorStatusOnConsoleState() {
        val statusText: String
        val styleColor: LLColor4?
        when (consoleState) {
            EConsoleState.UNKNOWN,
            EConsoleState.LIBRARY_NOT_IMPLEMENTED,
            EConsoleState.REGION_NOT_ENABLED,
            EConsoleState.REGION_LOADING,
            EConsoleState.CHECKING_VERSION,
            EConsoleState.ERROR -> { statusText = getString("navmesh_simulator_status_unknown"); styleColor = errorColor }
            EConsoleState.DOWNLOADING,
            EConsoleState.HAS_NAV_MESH -> {
                val zoneStatus = navMeshZone.getNavMeshZoneStatus()
                when (zoneStatus) {
                    LLPathfindingNavMeshZone.ENavMeshZoneStatus.PENDING -> { statusText = getString("navmesh_simulator_status_pending"); styleColor = warningColor }
                    LLPathfindingNavMeshZone.ENavMeshZoneStatus.BUILDING -> { statusText = getString("navmesh_simulator_status_building"); styleColor = warningColor }
                    LLPathfindingNavMeshZone.ENavMeshZoneStatus.SOME_PENDING -> { statusText = getString("navmesh_simulator_status_some_pending"); styleColor = warningColor }
                    LLPathfindingNavMeshZone.ENavMeshZoneStatus.SOME_BUILDING -> { statusText = getString("navmesh_simulator_status_some_building"); styleColor = warningColor }
                    LLPathfindingNavMeshZone.ENavMeshZoneStatus.PENDING_AND_BUILDING -> { statusText = getString("navmesh_simulator_status_pending_and_building"); styleColor = warningColor }
                    LLPathfindingNavMeshZone.ENavMeshZoneStatus.COMPLETE -> { statusText = getString("navmesh_simulator_status_complete"); styleColor = null }
                    else -> { statusText = getString("navmesh_simulator_status_unknown"); styleColor = errorColor }
                }
            }
        }
        pathfindingSimulatorStatus!!.setText(statusText, styleColor)
    }

    private fun initializeNavMeshZoneForCurrentRegion() {
        navMeshZone.disable()
        navMeshZone.initialize()
        navMeshZone.enable()
        navMeshZone.refresh()
        cleanupRenderableRestoreItems()
    }

    private fun cleanupRenderableRestoreItems() {
        if (renderableRestoreList.isNotEmpty()) {
            TODO("GPU: gPipeline.restorePermanentObjects(renderableRestoreList)")
            renderableRestoreList.clear()
        } else {
            TODO("GPU: gPipeline.skipRenderingOfTerrain(false)")
        }
    }

    private fun switchIntoTestPathMode() {
        if (LLPathingLib.getInstance() != null) {
            val toolMgr = LLToolMgr.getInstance()
            if (toolMgr.getCurrentToolset() != pathfindingToolset) {
                savedToolset = toolMgr.getCurrentToolset()
                toolMgr.setCurrentToolset(pathfindingToolset!!)
            }
        }
    }

    private fun switchOutOfTestPathMode() {
        if (LLPathingLib.getInstance() != null) {
            val toolMgr = LLToolMgr.getInstance()
            if (toolMgr.getCurrentToolset() == pathfindingToolset) {
                toolMgr.setCurrentToolset(savedToolset!!)
                savedToolset = null
            }
        }
    }

    private fun updateCharacterWidth() {
        LLPathfindingPathTool.getInstance().setCharacterWidth(characterWidthSlider!!.getValueF32())
    }

    private fun updateCharacterType() {
        val characterType = when (characterTypeComboBox!!.getValue().asInteger()) {
            XUI_CHARACTER_TYPE_A -> LLPathfindingPathTool.ECharacterType.CHARACTER_TYPE_A
            XUI_CHARACTER_TYPE_B -> LLPathfindingPathTool.ECharacterType.CHARACTER_TYPE_B
            XUI_CHARACTER_TYPE_C -> LLPathfindingPathTool.ECharacterType.CHARACTER_TYPE_C
            XUI_CHARACTER_TYPE_D -> LLPathfindingPathTool.ECharacterType.CHARACTER_TYPE_D
            else -> LLPathfindingPathTool.ECharacterType.CHARACTER_TYPE_NONE
        }
        LLPathfindingPathTool.getInstance().setCharacterType(characterType)
    }

    private fun clearPath() {
        LLPathfindingPathTool.getInstance().clearPath()
    }

    private fun updatePathTestStatus() {
        val statusText: String
        val styleColor: LLColor4?
        when (LLPathfindingPathTool.getInstance().getPathStatus()) {
            LLPathfindingPathTool.EPathStatus.UNKNOWN -> { statusText = getString("pathing_unknown"); styleColor = errorColor }
            LLPathfindingPathTool.EPathStatus.CHOOSE_START_AND_END -> { statusText = getString("pathing_choose_start_and_end_points"); styleColor = warningColor }
            LLPathfindingPathTool.EPathStatus.CHOOSE_START -> { statusText = getString("pathing_choose_start_point"); styleColor = warningColor }
            LLPathfindingPathTool.EPathStatus.CHOOSE_END -> { statusText = getString("pathing_choose_end_point"); styleColor = warningColor }
            LLPathfindingPathTool.EPathStatus.HAS_VALID_PATH -> { statusText = getString("pathing_path_valid"); styleColor = null }
            LLPathfindingPathTool.EPathStatus.HAS_INVALID_PATH -> { statusText = getString("pathing_path_invalid"); styleColor = errorColor }
            LLPathfindingPathTool.EPathStatus.NOT_ENABLED -> { statusText = getString("pathing_region_not_enabled"); styleColor = errorColor }
            LLPathfindingPathTool.EPathStatus.NOT_IMPLEMENTED -> { statusText = getString("pathing_library_not_implemented"); styleColor = errorColor }
            LLPathfindingPathTool.EPathStatus.ERROR -> { statusText = getString("pathing_error"); styleColor = errorColor }
            else -> { statusText = getString("pathing_unknown"); styleColor = errorColor }
        }
        pathTestingStatus!!.setText(statusText, styleColor)
    }

    private fun registerSavedSettingsListeners() {
        if (savedSettingRetrieveNeighborSlot == null) {
            savedSettingRetrieveNeighborSlot = gSavedSettings.getControl(CONTROL_NAME_RETRIEVE_NEIGHBOR)
                ?.getSignal()?.connect { control, newValue -> handleRetrieveNeighborChange(control, newValue) }
        }
        if (savedSettingWalkableSlot == null) {
            savedSettingWalkableSlot = gSavedSettings.getControl(CONTROL_NAME_WALKABLE_OBJECTS)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingStaticObstacleSlot == null) {
            savedSettingStaticObstacleSlot = gSavedSettings.getControl(CONTROL_NAME_STATIC_OBSTACLE_OBJECTS)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingMaterialVolumeSlot == null) {
            savedSettingMaterialVolumeSlot = gSavedSettings.getControl(CONTROL_NAME_MATERIAL_VOLUMES)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingExclusionVolumeSlot == null) {
            savedSettingExclusionVolumeSlot = gSavedSettings.getControl(CONTROL_NAME_EXCLUSION_VOLUMES)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingInteriorEdgeSlot == null) {
            savedSettingInteriorEdgeSlot = gSavedSettings.getControl(CONTROL_NAME_INTERIOR_EDGE)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingExteriorEdgeSlot == null) {
            savedSettingExteriorEdgeSlot = gSavedSettings.getControl(CONTROL_NAME_EXTERIOR_EDGE)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingHeatmapMinSlot == null) {
            savedSettingHeatmapMinSlot = gSavedSettings.getControl(CONTROL_NAME_HEATMAP_MIN)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingHeatmapMaxSlot == null) {
            savedSettingHeatmapMaxSlot = gSavedSettings.getControl(CONTROL_NAME_HEATMAP_MAX)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingNavMeshFaceSlot == null) {
            savedSettingNavMeshFaceSlot = gSavedSettings.getControl(CONTROL_NAME_NAVMESH_FACE)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingTestPathValidEndSlot == null) {
            savedSettingTestPathValidEndSlot = gSavedSettings.getControl(CONTROL_NAME_TEST_PATH_VALID_END)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingTestPathInvalidEndSlot == null) {
            savedSettingTestPathInvalidEndSlot = gSavedSettings.getControl(CONTROL_NAME_TEST_PATH_INVALID_END)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingTestPathSlot == null) {
            savedSettingTestPathSlot = gSavedSettings.getControl(CONTROL_NAME_TEST_PATH)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
        if (savedSettingWaterSlot == null) {
            savedSettingWaterSlot = gSavedSettings.getControl(CONTROL_NAME_WATER)
                ?.getSignal()?.connect { control, newValue -> handleNavMeshColorChange(control, newValue) }
        }
    }

    private fun deregisterSavedSettingsListeners() {
        savedSettingRetrieveNeighborSlot = null
        savedSettingWalkableSlot = null
        savedSettingStaticObstacleSlot = null
        savedSettingMaterialVolumeSlot = null
        savedSettingExclusionVolumeSlot = null
        savedSettingInteriorEdgeSlot = null
        savedSettingExteriorEdgeSlot = null
        savedSettingHeatmapMinSlot = null
        savedSettingHeatmapMaxSlot = null
        savedSettingNavMeshFaceSlot = null
        savedSettingTestPathValidEndSlot = null
        savedSettingTestPathInvalidEndSlot = null
        savedSettingTestPathSlot = null
        savedSettingWaterSlot = null
    }

    private fun handleRetrieveNeighborChange(control: LLControlVariable, newValue: LLSD) {
        initializeNavMeshZoneForCurrentRegion()
    }

    private fun handleNavMeshColorChange(control: LLControlVariable, newValue: LLSD) {
        fillInColorsForNavMeshVisualization()
    }

    private fun fillInColorsForNavMeshVisualization() {
        if (LLPathingLib.getInstance() != null) {
            TODO("GPU: build NavMeshColors from gSavedSettings color4 values and call LLPathingLib.getInstance().setNavMeshColors(navMeshColors)")
        }
    }
}
