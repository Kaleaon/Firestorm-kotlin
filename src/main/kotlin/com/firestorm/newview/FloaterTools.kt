package com.firestorm.newview

import kotlin.math.log10
import kotlin.math.pow

var gFloaterTools: FloaterTools? = null

private val PANEL_NAMES = arrayOf(
    "General",
    "Object",
    "Features",
    "Texture",
    "Content"
)

private val TOOL_NAMES = arrayOf(
    "ToolCube", "ToolPrism", "ToolPyramid", "ToolTetrahedron",
    "ToolCylinder", "ToolHemiCylinder", "ToolCone", "ToolHemiCone",
    "ToolSphere", "ToolHemiSphere", "ToolTorus", "ToolTube",
    "ToolRing", "ToolTree", "ToolGrass"
)

private val TOOL_DATA = arrayOf(
    PCode.CUBE, PCode.PRISM, PCode.PYRAMID, PCode.TETRAHEDRON,
    PCode.CYLINDER, PCode.CYLINDER_HEMI, PCode.CONE, PCode.CONE_HEMI,
    PCode.SPHERE, PCode.SPHERE_HEMI, PCode.TORUS, PCode.SQUARE_TORUS,
    PCode.TRIANGLE_TORUS, PCode.LEGACY_TREE, PCode.LEGACY_GRASS
)

class FloaterTools(key: LLSD) : Floater(key) {

    enum class InfoPanel {
        GENERAL, OBJECT, FEATURES, FACE, CONTENTS;
        companion object { const val COUNT = 5 }
    }

    var btnFocus: Button? = null
    var btnMove: Button? = null
    var btnEdit: Button? = null
    var btnCreate: Button? = null
    var btnLand: Button? = null

    var textStatus: TextBox? = null

    var radioGroupFocus: RadioGroup? = null
    var radioGroupMove: RadioGroup? = null
    var radioGroupEdit: RadioGroup? = null

    var checkSelectIndividual: CheckBoxCtrl? = null
    var btnLink: Button? = null
    var btnUnlink: Button? = null

    var btnPrevPart: Button? = null
    var btnNextPart: Button? = null

    var checkSnapToGrid: CheckBoxCtrl? = null
    var btnGridOptions: Button? = null
    var comboGridMode: ComboBox? = null
    var checkStretchUniform: CheckBoxCtrl? = null
    var checkStretchTexture: CheckBoxCtrl? = null
    var checkShowHighlight: CheckBoxCtrl? = null
    var checkActualRoot: CheckBoxCtrl? = null
    var checkSelectProbes: CheckBoxCtrl? = null

    var btnRotateLeft: Button? = null
    var btnRotateReset: Button? = null
    var btnRotateRight: Button? = null

    var btnDelete: Button? = null
    var btnDuplicate: Button? = null
    var btnDuplicateInPlace: Button? = null

    var textSelectionCount: TextBox? = null
    var textSelectionEmpty: TextBox? = null
    var textSelectionFaces: TextBox? = null
    var sliderZoom: Slider? = null

    var textLinkNumObjCount: TextBox? = null
    var textMoreInfoLabel: TextBox? = null
    var btnCopyKeys: Button? = null

    var treeGrassCombo: ComboBox? = null

    var checkSticky: CheckBoxCtrl? = null
    var checkCopySelection: CheckBoxCtrl? = null
    var checkCopyCenters: CheckBoxCtrl? = null
    var checkCopyRotates: CheckBoxCtrl? = null

    var radioGroupLand: RadioGroup? = null
    var sliderDozerSize: Slider? = null
    var sliderDozerForce: Slider? = null
    var textBulldozer: TextBox? = null
    var textDozerSize: TextBox? = null
    var textDozerStrength: TextBox? = null

    var btnApplyToSelection: Button? = null

    val buttons: MutableList<Button> = mutableListOf()

    var tab: TabContainer? = null
    var panelPermissions: PanelPermissions? = null
    var panelObject: PanelObject? = null
    var panelVolume: PanelVolume? = null
    var panelContents: PanelContents? = null
    var panelLandInfo: PanelLandInfo? = null

    var costTextBorder: ViewBorder? = null
    var tabLand: TabContainer? = null

    var parcelSelection: ParcelSelectionHandle? = null
    var objectSelection: ObjectSelectionHandle? = null

    private var dirty: Boolean = true
    private var hasSelection: Boolean = true
    private var isOpen: Boolean = false
    private var collapsedHeight: Int = 0
    private var expandedHeight: Int = 0
    private val statusText: MutableMap<String, String> = mutableMapOf()

    private var panelFace: PanelFace? = null
    private var fsPanelFace: FSPanelFace? = null

    private var landImpactsObserver: LandImpactsObserver? = null

    companion object {
        var showObjectCost: Boolean = true
        var previousFocusOnAvatar: Boolean = false

        var instance: FloaterTools? = null
            private set

        fun setEditTool(tool: Tool) {
            ToolMgr.instance.getCurrentToolset().selectTool(tool)
        }

        fun setGridMode(mode: Int) {
            instance?.comboGridMode?.setCurrentByIndex(mode)
        }

        fun setObjectType(pcode: PCode) {
            ToolPlacer.setObjectType(pcode)
            SavedSettings.setBool("CreateToolCopySelection", false)
            instance?.buildTreeGrassCombo()
            FocusMgr.instance.setMouseCapture(null)
        }
    }

    init {
        instance = this
        gFloaterTools = this

        setAutoFocus(false)

        registerCommitCallback("BuildTool.setTool")           { _, userData -> setTool(userData) }
        registerCommitCallback("BuildTool.commitZoom")        { ctrl, _ -> commitSliderZoom(ctrl) }
        registerCommitCallback("BuildTool.commitRadioFocus")  { ctrl, _ -> commitRadioGroupFocus(ctrl) }
        registerCommitCallback("BuildTool.commitRadioMove")   { ctrl, _ -> commitRadioGroupMove(ctrl) }
        registerCommitCallback("BuildTool.commitRadioEdit")   { ctrl, _ -> commitRadioGroupEdit(ctrl) }
        registerCommitCallback("BuildTool.gridMode")          { ctrl, _ -> commitGridMode(ctrl) }
        registerCommitCallback("BuildTool.selectComponent")   { _, _ -> commitSelectComponent() }
        registerCommitCallback("BuildTool.gridOptions")       { _, _ -> onClickGridOptions() }
        registerCommitCallback("BuildTool.applyToSelection")  { _, _ -> ToolBrushLand.instance.modifyLandInSelectionGlobal() }
        registerCommitCallback("BuildTool.commitRadioLand")   { ctrl, _ -> commitRadioGroupLand(ctrl) }
        registerCommitCallback("BuildTool.LandBrushForce")    { ctrl, _ -> commitSliderDozerForce(ctrl) }
        registerCommitCallback("BuildTool.LinkObjects")       { _, _ -> SelectMgr.instance.linkObjects() }
        registerCommitCallback("BuildTool.UnlinkObjects")     { _, _ -> SelectMgr.instance.unlinkObjects() }
        registerCommitCallback("BuildTool.CopyKeys")          { _, _ -> onClickBtnCopyKeys() }
        registerCommitCallback("BuildTool.Expand")            { _, _ -> onClickExpand() }
        registerCommitCallback("BuildTool.TreeGrass")         { _, _ -> onSelectTreeGrassCombo() }
        registerCommitCallback("BuildTool.commitShowHighlight") { _, _ -> commitShowHighlight() }

        landImpactsObserver = LandImpactsObserver { updateLandImpacts() }
        ViewerParcelMgr.instance.addObserver(landImpactsObserver!!)
    }

    override fun postBuild(): Boolean {
        val useNewPanel = SavedSettings.getBool("FSUseNewTexturePanel")
        val facePanel: Panel = if (useNewPanel) {
            fsPanelFace = FSPanelFace()
            fsPanelFace!!
        } else {
            panelFace = PanelFace()
            panelFace!!
        }

        tab = getChild<TabContainer>("Object Info Tabs")
        tab?.addTabPanel(facePanel, insertAt = InfoPanel.FACE.ordinal)

        setVisible(false)
        setSoundFlags(View.SILENT)
        getDragHandle()?.setEnabled(!SavedSettings.getBool("ToolboxAutoMove"))

        btnFocus             = getChild("button focus")
        btnMove              = getChild("button move")
        btnEdit              = getChild("button edit")
        btnCreate            = getChild("button create")
        btnLand              = getChild("button land")
        textStatus           = getChild("text status")
        radioGroupFocus      = getChild("focus_radio_group")
        radioGroupMove       = getChild("move_radio_group")
        radioGroupEdit       = getChild("edit_radio_group")
        btnGridOptions       = getChild("Options...")
        btnLink              = getChild("link_btn")
        btnUnlink            = getChild("unlink_btn")
        btnPrevPart          = getChild("prev_part_btn")
        btnNextPart          = getChild("next_part_btn")
        checkSelectIndividual = getChild("checkbox edit linked parts")
        getChild<UICtrl>("checkbox edit linked parts")?.setValue(SavedSettings.getBool("EditLinkedParts"))
        checkSnapToGrid      = getChild("checkbox snap to grid")
        getChild<UICtrl>("checkbox snap to grid")?.setValue(SavedSettings.getBool("SnapEnabled"))
        checkStretchUniform  = getChild("checkbox uniform")
        getChild<UICtrl>("checkbox uniform")?.setValue(SavedSettings.getBool("ScaleUniform"))
        checkStretchTexture  = getChild("checkbox stretch textures")
        getChild<UICtrl>("checkbox stretch textures")?.setValue(SavedSettings.getBool("ScaleStretchTextures"))
        comboGridMode        = getChild("combobox grid mode")
        checkShowHighlight   = getChild("checkbox show highlight")
        checkShowHighlight?.setValue(SavedSettings.getBool("RenderHighlightSelections"))
        SelectMgr.instance.setFSShowHideHighlight(FS_SHOW_HIDE_HIGHLIGHT_NORMAL)
        checkActualRoot      = getChild("checkbox actual root")
        checkSelectProbes    = getChild("checkbox select probes")

        for (name in TOOL_NAMES) {
            val btn = getChild<Button>(name)
            if (btn != null) {
                val pcode = TOOL_DATA[TOOL_NAMES.indexOf(name)]
                btn.setClickedCallback { setObjectType(pcode) }
                buttons.add(btn)
            }
        }

        checkCopySelection = getChild("checkbox copy selection")
        getChild<UICtrl>("checkbox copy selection")?.setValue(SavedSettings.getBool("CreateToolCopySelection"))
        checkSticky        = getChild("checkbox sticky")
        getChild<UICtrl>("checkbox sticky")?.setValue(SavedSettings.getBool("CreateToolKeepSelected"))
        checkCopyCenters   = getChild("checkbox copy centers")
        getChild<UICtrl>("checkbox copy centers")?.setValue(SavedSettings.getBool("CreateToolCopyCenters"))
        checkCopyRotates   = getChild("checkbox copy rotates")
        getChild<UICtrl>("checkbox copy rotates")?.setValue(SavedSettings.getBool("CreateToolCopyRotates"))

        radioGroupLand      = getChild("land_radio_group")
        btnApplyToSelection = getChild("button apply to selection")
        sliderDozerSize     = getChild("slider brush size")
        getChild<UICtrl>("slider brush size")?.setValue(SavedSettings.getFloat("LandBrushSize"))
        sliderDozerForce    = getChild("slider force")
        getChild<UICtrl>("slider force")?.setValue(log10(SavedSettings.getFloat("LandBrushForce")))
        treeGrassCombo      = getChild("tree_grass_combo")
        textBulldozer       = getChild("Bulldozer:")
        textDozerSize       = getChild("Dozer Size:")
        textDozerStrength   = getChild("Strength:")
        sliderZoom          = getChild("slider zoom")
        textSelectionCount  = getChild("selection_count")
        textSelectionEmpty  = getChild("selection_empty")
        costTextBorder      = getChild("cost_text_border")

        tab = getChild<TabContainer>("Object Info Tabs")
        tab?.selectFirstTab()

        statusText["rotate"]      = getString("status_rotate")
        statusText["scale"]       = getString("status_scale")
        statusText["move"]        = getString("status_move")
        statusText["modifyland"]  = getString("status_modifyland")
        statusText["camera"]      = getString("status_camera")
        statusText["grab"]        = getString("status_grab")
        statusText["place"]       = getString("status_place")
        statusText["selectland"]  = getString("status_selectland")

        showObjectCost = SavedSettings.getBool("ShowObjectRenderingCost")

        val btnExpand = findChild<Button>("btnExpand")
        if (btnExpand != null && tab != null) {
            expandedHeight = getRect().height
            collapsedHeight = expandedHeight - tab!!.getRect().height + btnExpand.getRect().height
            if (!SavedSettings.getBool("FSToolboxExpanded")) {
                tab!!.setVisible(false)
                reshape(getRect().width, collapsedHeight)
                btnExpand.setImageOverlay("Arrow_Down", btnExpand.getImageOverlayHAlign())
            }
        } else {
            SavedSettings.setBool("FSToolboxExpanded", true)
        }

        textLinkNumObjCount = getChild("link_num_obj_count")
        btnCopyKeys         = getChild("btnCopyKeys")
        textMoreInfoLabel   = getChild("more info label")

        return true
    }

    override fun onOpen(key: LLSD) {
        parcelSelection = ViewerParcelMgr.instance.getFloatingParcelSelection()
        objectSelection = SelectMgr.instance.getEditSelection()

        if (!isOpen) {
            isOpen = true
            checkShowHighlight?.setValue(SavedSettings.getBool("RenderHighlightSelections"))
        }

        val panel = key.asString()
        if (panel.isNotEmpty()) {
            tab?.selectTabByName(panel)
        }

        val tool = ToolMgr.instance.getCurrentTool()
        if (tool == ToolCompInspect.instance || tool == ToolDragAndDrop.instance) {
            val selectCenterScreen = CoordGL()
            val mask = Keyboard.instance.currentMask(true)
            updatePopup(selectCenterScreen, mask)
        }
    }

    override fun canClose(): Boolean = !App.isExiting()

    override fun onClose(appQuitting: Boolean) {
        tab?.setVisible(false)
        ViewerJoystick.instance.moveAvatar(false)

        panelFace?.unloadMedia()
        fsPanelFace?.unloadMedia()

        AgentCamera.instance.resetView(SavedSettings.getBool("EditCameraMovement"))
        SelectMgr.instance.promoteSelectionToRoot()
        SavedSettings.setBool("EditLinkedParts", false)
        SelectMgr.instance.setFSShowHideHighlight(FS_SHOW_HIDE_HIGHLIGHT_NORMAL)

        isOpen = false

        ViewerWindow.instance.showCursor()
        resetToolState()

        parcelSelection = null
        objectSelection = null

        if (!AgentCamera.instance.cameraMouselook()) {
            ToolMgr.instance.setCurrentToolset(gBasicToolset)
            ToolMgr.instance.getCurrentToolset().selectFirstTool()
        } else {
            ToolMgr.instance.setCurrentToolset(gMouselookToolset)
            ViewerWindow.instance.hideCursor()
            ViewerWindow.instance.moveCursorToCenter()
        }

        FloaterReg.hideInstance("media_settings")
        FloaterReg.hideInstance("object_weights")
        FloaterReg.hideInstance("live_material_editor")

        panelContents?.clearContents()

        if (previousFocusOnAvatar) {
            previousFocusOnAvatar = false
            AgentCamera.instance.setAllowChangeToFollow(true)
        }
    }

    override fun draw() {
        val currentHasSelection = !SelectMgr.instance.getSelection().isEmpty()
        if (!currentHasSelection && hasSelection != currentHasSelection) {
            dirty = true
        }
        hasSelection = currentHasSelection

        if (dirty) {
            refresh()
            dirty = false
        }

        super.draw()
    }

    override fun onFocusReceived() {
        ToolMgr.instance.setCurrentToolset(gBasicToolset)
        super.onFocusReceived()
    }

    fun dirty() {
        dirty = true
        FloaterReg.findTypedInstance<FloaterOpenObject>("openobject")?.dirty()
    }

    fun showPanel(panel: InfoPanel) {
        tab?.selectTabByName(PANEL_NAMES[panel.ordinal])
    }

    fun setStatusText(text: String) {
        val resolved = statusText[text] ?: text
        textStatus?.setText(resolved)
    }

    fun setTool(userData: LLSD) {
        when (userData.asString()) {
            "Focus"  -> ToolMgr.instance.getCurrentToolset().selectTool(ToolCamera.instance)
            "Move"   -> ToolMgr.instance.getCurrentToolset().selectTool(ToolGrab)
            "Edit"   -> ToolMgr.instance.getCurrentToolset().selectTool(ToolCompTranslate.instance)
            "Create" -> ToolMgr.instance.getCurrentToolset().selectTool(ToolCompCreate.instance)
            "Land"   -> ToolMgr.instance.getCurrentToolset().selectTool(ToolSelectLand.instance)
        }
    }

    fun saveLastTool() {}

    fun updateLandImpacts() {
        val parcel = parcelSelection?.getParcel() ?: return

        val rezzedPrims = parcel.getSimWidePrimCount()
        var totalCapacity = parcel.getSimWideMaxPrimCapacity()
        val region = ViewerParcelMgr.instance.getSelectionRegion()
        if (region != null) {
            val maxTasksPerRegion = region.getMaxTasks()
            totalCapacity = minOf(totalCapacity, maxTasksPerRegion)
        }

        val remainingCapacityStr = if (MeshRepo.instance.meshRezEnabled()) {
            val capacity = totalCapacity - rezzedPrims
            getString("status_remaining_capacity").replace("[LAND_CAPACITY]", "$capacity")
        } else ""

        childSetTextArg("more info label", "[CAPACITY_STRING]", remainingCapacityStr)

        FloaterReg.findTypedInstance<FloaterObjectWeights>("object_weights")
            ?.updateLandImpacts(parcel)
    }

    fun updateToolsSizeLimits() {
        panelObject?.updateLimits(false)
    }

    fun changePrecision(decimalPrecision: Int) {
        val clamped = decimalPrecision.coerceIn(0, 7)
        panelObject?.changePrecision(clamped)
        panelFace?.changePrecision(clamped)
        fsPanelFace?.changePrecision(clamped)
    }

    fun refreshPanelFace() {
        panelFace?.refresh()
        fsPanelFace?.refresh()
    }

    fun getTextureDropChannel(): TexIndex =
        panelFace?.getTextureDropChannel()
            ?: fsPanelFace?.getTextureDropChannel()
            ?: TexIndex.NUM_TEXTURE_CHANNELS

    fun getTextureChannelToEdit(): TexIndex =
        panelFace?.getTextureChannelToEdit()
            ?: fsPanelFace?.getTextureChannelToEdit()
            ?: TexIndex.NUM_TEXTURE_CHANNELS

    fun getPBRDropChannel(): GltfTextureInfo =
        panelFace?.getPBRDropChannel()
            ?: fsPanelFace?.getPBRDropChannel()
            ?: GltfTextureInfo.GLTF_TEXTURE_INFO_COUNT

    fun createDefaultMaterial(oldMat: Material?): Material? =
        panelFace?.createDefaultMaterial(oldMat)
            ?: fsPanelFace?.createDefaultMaterial(oldMat)

    fun onClickBtnCopyKeys() {
        val separator = SavedSettings.getString("FSCopyObjKeySeparator")
        val mask = Keyboard.instance.currentMask(false)
        val keys = mutableListOf<String>()

        val functor = { obj: ViewerObject -> keys.add(obj.getId().toString()); true }

        val copied = when {
            mask == MASK_SHIFT -> SelectMgr.instance.getSelection().applyToObjects(functor)
            checkSelectIndividual?.get() == true ->
                SelectMgr.instance.getSelection().applyToObjects(functor)
            else -> SelectMgr.instance.getSelection().applyToRootObjects(functor)
        }

        if (copied) {
            ViewerWindow.instance.getWindow().copyTextToClipboard(keys.joinToString(separator))
        }
    }

    fun onClickExpand() {
        val showMore = !SavedSettings.getBool("FSToolboxExpanded")
        SavedSettings.setBool("FSToolboxExpanded", showMore)

        val btnExpand = getChild<Button>("btnExpand") ?: return
        if (showMore) {
            tab?.setVisible(true)
            reshape(getRect().width, expandedHeight)
            translate(0, collapsedHeight - expandedHeight)
            btnExpand.setImageOverlay("Arrow_Up", btnExpand.getImageOverlayHAlign())
        } else {
            tab?.setVisible(false)
            reshape(getRect().width, collapsedHeight)
            translate(0, expandedHeight - collapsedHeight)
            btnExpand.setImageOverlay("Arrow_Down", btnExpand.getImageOverlayHAlign())
        }
    }

    fun updatePopup(center: CoordGL, mask: Int) {
        val tool = ToolMgr.instance.getCurrentTool()
        if (tool == gToolNull || isMinimized()) return

        val focusVisible = tool == ToolCamera.instance
        btnFocus?.setToggleState(focusVisible)
        radioGroupFocus?.setVisible(focusVisible)
        sliderZoom?.setVisible(focusVisible)
        sliderZoom?.setEnabled(gCameraBtnZoom)

        when {
            !gCameraBtnOrbit && !gCameraBtnPan &&
            mask != MASK_ORBIT && mask != (MASK_ORBIT or MASK_ALT) &&
            mask != MASK_PAN  && mask != (MASK_PAN  or MASK_ALT) ->
                radioGroupFocus?.setValue("radio zoom")
            gCameraBtnOrbit || mask == MASK_ORBIT || mask == (MASK_ORBIT or MASK_ALT) ->
                radioGroupFocus?.setValue("radio orbit")
            gCameraBtnPan || mask == MASK_PAN || mask == (MASK_PAN or MASK_ALT) ->
                radioGroupFocus?.setValue("radio pan")
        }

        sliderZoom?.setValue(AgentCamera.instance.getCameraZoomFraction() * 0.5f)

        val moveVisible = tool == ToolGrab
        btnMove?.setToggleState(moveVisible)
        radioGroupMove?.setVisible(moveVisible)

        when {
            !grabBtnSpin && !grabBtnVertical && mask != MASK_VERTICAL && mask != MASK_SPIN ->
                radioGroupMove?.setValue("radio move")
            mask == MASK_VERTICAL || (grabBtnVertical && mask != MASK_SPIN) ->
                radioGroupMove?.setValue("radio lift")
            mask == MASK_SPIN || (grabBtnSpin && mask != MASK_VERTICAL) ->
                radioGroupMove?.setValue("radio spin")
        }

        val editVisible = tool == ToolCompTranslate.instance ||
                          tool == ToolCompRotate.instance ||
                          tool == ToolCompScale.instance ||
                          tool == ToolFace ||
                          tool == ToolIndividual.instance ||
                          tool == QToolAlign.instance ||
                          tool == ToolPipette.instance

        btnEdit?.setToggleState(editVisible)
        radioGroupEdit?.setVisible(editVisible)

        val linkedParts = SavedSettings.getBool("EditLinkedParts")

        btnLink?.setVisible(editVisible)
        btnUnlink?.setVisible(editVisible)
        btnLink?.setEnabled(SelectMgr.instance.enableLinkObjects())
        btnUnlink?.setEnabled(SelectMgr.instance.enableUnlinkObjects())

        btnPrevPart?.setVisible(editVisible)
        btnNextPart?.setVisible(editVisible)
        val selectBtnEnabled = !SelectMgr.instance.getSelection().isEmpty() &&
                (linkedParts || ToolFace == ToolMgr.instance.getCurrentTool())
        btnPrevPart?.setEnabled(selectBtnEnabled)
        btnNextPart?.setEnabled(selectBtnEnabled)

        checkSelectIndividual?.setVisible(editVisible)

        when (tool) {
            ToolCompTranslate.instance -> radioGroupEdit?.setValue("radio position")
            ToolCompRotate.instance   -> radioGroupEdit?.setValue("radio rotate")
            ToolCompScale.instance    -> radioGroupEdit?.setValue("radio stretch")
            ToolFace                  -> radioGroupEdit?.setValue("radio select face")
            QToolAlign.instance       -> radioGroupEdit?.setValue("radio align")
        }

        if (comboGridMode != null) {
            comboGridMode!!.setVisible(editVisible)
            val index = comboGridMode!!.getCurrentIndex()
            comboGridMode!!.removeAll()

            when (objectSelection?.getSelectType()) {
                SelectType.HUD -> {
                    comboGridMode!!.add(getString("grid_screen_text"))
                    comboGridMode!!.add(getString("grid_local_text"))
                }
                SelectType.WORLD -> {
                    comboGridMode!!.add(getString("grid_world_text"))
                    comboGridMode!!.add(getString("grid_local_text"))
                    comboGridMode!!.add(getString("grid_reference_text"))
                }
                SelectType.ATTACHMENT -> {
                    comboGridMode!!.add(getString("grid_attachment_text"))
                    comboGridMode!!.add(getString("grid_local_text"))
                    comboGridMode!!.add(getString("grid_reference_text"))
                }
                else -> {}
            }

            comboGridMode!!.setCurrentByIndex(index)
        }

        checkSnapToGrid?.setVisible(editVisible)
        btnGridOptions?.setVisible(editVisible)
        checkStretchUniform?.setVisible(editVisible)
        checkStretchTexture?.setVisible(editVisible)
        checkShowHighlight?.setVisible(editVisible)
        checkActualRoot?.setVisible(editVisible)
        checkSelectProbes?.setVisible(editVisible)

        val createVisible = tool == ToolCompCreate.instance
        treeGrassCombo?.setVisible(createVisible)
        if (createVisible) buildTreeGrassCombo()

        btnCreate?.setToggleState(createVisible)

        if (checkCopySelection?.get() == true) {
            for (btn in buttons) {
                btn.setToggleState(false)
                btn.setVisible(createVisible)
            }
        } else {
            for (t in buttons.indices) {
                val pcode = ToolPlacer.getObjectType()
                val buttonPcode = TOOL_DATA[t]
                buttons[t].setToggleState(pcode == buttonPcode)
                buttons[t].setVisible(createVisible)
            }
        }

        checkSticky?.setVisible(createVisible)
        checkCopySelection?.setVisible(createVisible)
        checkCopyCenters?.setVisible(createVisible)
        checkCopyRotates?.setVisible(createVisible)

        if (checkCopyCenters != null && checkCopySelection != null)
            checkCopyCenters!!.setEnabled(checkCopySelection!!.get())
        if (checkCopyRotates != null && checkCopySelection != null)
            checkCopyRotates!!.setEnabled(checkCopySelection!!.get())

        val landVisible = tool == ToolBrushLand.instance || tool == ToolSelectLand.instance
        costTextBorder?.setVisible(!landVisible)
        btnLand?.setToggleState(landVisible)
        radioGroupLand?.setVisible(landVisible)

        when (tool) {
            ToolSelectLand.instance -> radioGroupLand?.setValue("radio select land")
            ToolBrushLand.instance -> {
                val dozerMode = SavedSettings.getInt("RadioLandBrushAction")
                val radioValue = when (dozerMode) {
                    0 -> "radio flatten"
                    1 -> "radio raise"
                    2 -> "radio lower"
                    3 -> "radio smooth"
                    4 -> "radio noise"
                    5 -> "radio revert"
                    else -> null
                }
                if (radioValue != null) radioGroupLand?.setValue(radioValue)
            }
        }

        if (btnApplyToSelection != null) {
            btnApplyToSelection!!.setVisible(landVisible)
            btnApplyToSelection!!.setEnabled(
                landVisible &&
                !ViewerParcelMgr.instance.selectionEmpty() &&
                tool != ToolSelectLand.instance
            )
        }

        sliderDozerSize?.setVisible(landVisible)
        textBulldozer?.setVisible(landVisible)
        textDozerSize?.setVisible(landVisible)
        sliderDozerForce?.setVisible(landVisible)
        textDozerStrength?.setVisible(landVisible)

        val fsToolboxExpanded = SavedSettings.getBool("FSToolboxExpanded")
        tab?.setVisible(!landVisible && fsToolboxExpanded)
        panelLandInfo?.setVisible(landVisible && fsToolboxExpanded)

        val haveSelection = !SelectMgr.instance.getSelection().isEmpty()
        getChildView("more info label")?.setVisible(!landVisible && haveSelection)
        textSelectionCount?.setVisible(!landVisible && haveSelection)
        textSelectionEmpty?.setVisible(!landVisible && !haveSelection)
    }

    fun resetToolState() {
        gCameraBtnZoom = true
        gCameraBtnOrbit = false
        gCameraBtnPan = false
        grabBtnSpin = false
        grabBtnVertical = false
    }

    private fun refresh() {
        val allVolume = SelectMgr.instance.selectionAllPCode(PCode.VOLUME)

        val idxFeatures = tab?.getPanelIndexByTitle(PANEL_NAMES[InfoPanel.FEATURES.ordinal]) ?: -1
        val idxFace     = tab?.getPanelIndexByTitle(PANEL_NAMES[InfoPanel.FACE.ordinal]) ?: -1
        val idxContents = tab?.getPanelIndexByTitle(PANEL_NAMES[InfoPanel.CONTENTS.ordinal]) ?: -1
        val selectedIndex = tab?.getCurrentPanelIndex() ?: -1

        if (!allVolume && (selectedIndex == idxFeatures || selectedIndex == idxFace || selectedIndex == idxContents)) {
            tab?.selectFirstTab()
        }

        tab?.enableTabButton(idxFeatures, allVolume)
        tab?.enableTabButton(idxFace, allVolume)
        tab?.enableTabButton(idxContents, allVolume)

        val primCount = SelectMgr.instance.getSelection().getObjectCount()
        var descString = ""
        var numString = ""
        var enableLinkCount = true

        if (primCount == 1 && ToolMgr.instance.getCurrentTool() == ToolFace) {
            descString = getString("selected_faces")
            val objectp = SelectMgr.instance.getSelection().getFirstRootObject()
                ?: SelectMgr.instance.getSelection().getFirstObject()
            val nodep = SelectMgr.instance.getSelection().getFirstRootNode()
                ?: SelectMgr.instance.getSelection().getFirstNode()

            numString = when {
                objectp != null && objectp.getNumTEs() == SelectMgr.instance.getSelection().getTECount() -> "ALL_SIDES"
                objectp != null && nodep != null -> {
                    (0 until objectp.getNumTEs())
                        .filter { nodep.isTESelected(it) }
                        .joinToString(", ") { "$it" }
                }
                else -> ""
            }
        } else if (primCount == 1 && SavedSettings.getBool("EditLinkedParts")) {
            descString = getString("link_number")
            val objectp = SelectMgr.instance.getSelection().getFirstObject()
            if (objectp?.getRootEdit() != null) {
                val children = objectp.getRootEdit()!!.getChildren()
                numString = when {
                    children.isEmpty() -> "0"
                    objectp.getRootEdit()!!.isSelected() -> "1"
                    else -> {
                        var index = 1
                        var found = ""
                        for (child in children) {
                            index++
                            if (child.isSelected()) { found = "$index"; break }
                        }
                        found
                    }
                }
            }
        } else {
            enableLinkCount = false
        }

        textLinkNumObjCount?.setTextArg("[DESC]", descString)
        textLinkNumObjCount?.setTextArg("[NUM]", numString)

        val selection = SelectMgr.instance.getSelection()
        val linkCost = selection.getSelectedLinksetCost()
        val linkCount = selection.getRootObjectCount()

        val crossParcel = LLCrossParcelFunctor()
        if (!SelectMgr.instance.getSelection().applyToRootObjects(crossParcel, true)) {
            val selectedObject = objectSelection?.getFirstObject()
            if (selectedObject != null) {
                if (!selectedObject.isAttachment()) {
                    ViewerParcelMgr.instance.selectParcelAt(selectedObject.getPositionGlobal())
                } else {
                    textMoreInfoLabel?.setTextArg("[CAPACITY_STRING]", "")
                }
            }
        } else {
            textMoreInfoLabel?.setTextArg("[CAPACITY_STRING]", "")
        }

        val landImpact = linkCost.toInt()
        val selectionText = getString("status_selectcount")
            .replace("[OBJ_COUNT]", "$linkCount")
            .replace("[LAND_IMPACT]", "$landImpact")
        textSelectionCount?.setText(selectionText)

        val haveSelection = !SelectMgr.instance.getSelection().isEmpty()
        textLinkNumObjCount?.setEnabled(haveSelection && enableLinkCount)

        panelPermissions?.refresh()
        panelObject?.refresh()
        panelVolume?.refresh()
        panelFace?.refresh()
        panelFace?.refreshMedia()
        fsPanelFace?.refresh()
        fsPanelFace?.refreshMedia()
        panelContents?.refresh()
        panelLandInfo?.refresh()

        FloaterReg.findTypedInstance<FloaterObjectWeights>("object_weights")
            ?.takeIf { it.getVisible() }
            ?.refresh()

        btnCopyKeys?.setEnabled(haveSelection)
    }

    private fun buildTreeGrassCombo() {
        val combo = treeGrassCombo ?: return
        val pcode = ToolPlacer.getObjectType()
        val type: String

        when (pcode) {
            PCode.LEGACY_TREE, PCode.TREE_NEW -> {
                buildPlantCombo(VOTree.speciesTable, combo)
                combo.addSimpleElement("Random", AddPosition.TOP)
                type = "Tree"
            }
            PCode.LEGACY_GRASS -> {
                buildPlantCombo(VOGrass.speciesTable, combo)
                combo.addSimpleElement("Random", AddPosition.TOP)
                type = "Grass"
            }
            else -> {
                combo.setEnabled(false)
                return
            }
        }

        combo.setEnabled(true)
        val lastSelected = SavedSettings.getString("LastSelected$type")
        if (lastSelected.isEmpty()) {
            combo.selectByValue("Random")
        } else {
            combo.selectByValue(lastSelected)
        }
    }

    private fun <P : PlantSpecies> buildPlantCombo(list: Map<UInt, P>, combo: ComboBox) {
        if (list.isEmpty()) return
        combo.removeAll()
        for (plant in list.values) {
            combo.addSimpleElement(plant.name, AddPosition.BOTTOM)
        }
    }

    private fun onSelectTreeGrassCombo() {
        val lastSelected = treeGrassCombo?.getValue()?.asString() ?: return
        val pcode = ToolPlacer.getObjectType()
        val type = when (pcode) {
            PCode.LEGACY_GRASS -> "Grass"
            PCode.LEGACY_TREE, PCode.TREE_NEW -> "Tree"
            else -> return
        }
        SavedSettings.setString("LastSelected$type", lastSelected)
    }

    private fun onClickGridOptions() {
        val floaterp = FloaterReg.showInstance("build_options")
        floaterp?.setShape(FloaterView.instance.findNeighboringPosition(this, floaterp), true)
    }

    private fun commitSelectComponent() {
        if (FocusMgr.instance.childHasKeyboardFocus(this)) {
            FocusMgr.instance.setKeyboardFocus(null)
        }
        val selectIndividuals = checkSelectIndividual?.get() ?: false
        SavedSettings.setBool("EditLinkedParts", selectIndividuals)
        dirty()

        if (selectIndividuals) {
            SelectMgr.instance.demoteSelectionToIndividuals()
        } else {
            SelectMgr.instance.promoteSelectionToRoot()
        }
    }

    private fun commitShowHighlight() {
        val showHighlight = checkShowHighlight?.get() ?: false
        if (showHighlight) {
            SelectMgr.instance.setFSShowHideHighlight(FS_SHOW_HIDE_HIGHLIGHT_SHOW)
        } else {
            SelectMgr.instance.setFSShowHideHighlight(FS_SHOW_HIDE_HIGHLIGHT_HIDE)
        }
    }
}

private fun commitRadioGroupMove(ctrl: UICtrl) {
    val group = ctrl as? RadioGroup ?: return
    when (group.getValue().asString()) {
        "radio move" -> { grabBtnVertical = false; grabBtnSpin = false }
        "radio lift" -> { grabBtnVertical = true;  grabBtnSpin = false }
        "radio spin" -> { grabBtnVertical = false; grabBtnSpin = true  }
    }
}

private fun commitRadioGroupFocus(ctrl: UICtrl) {
    val group = ctrl as? RadioGroup ?: return
    when (group.getValue().asString()) {
        "radio zoom"  -> { gCameraBtnZoom = true;  gCameraBtnOrbit = false; gCameraBtnPan = false }
        "radio orbit" -> { gCameraBtnZoom = false; gCameraBtnOrbit = true;  gCameraBtnPan = false }
        "radio pan"   -> { gCameraBtnZoom = false; gCameraBtnOrbit = false; gCameraBtnPan = true  }
    }
}

private fun commitSliderZoom(ctrl: UICtrl) {
    val zoomLevel = ctrl.getValue().asFloat() * 2f
    AgentCamera.instance.setCameraZoomFraction(zoomLevel)
}

private fun commitSliderDozerForce(ctrl: UICtrl) {
    val dozerForce = 10f.pow(ctrl.getValue().asFloat())
    SavedSettings.setFloat("LandBrushForce", dozerForce)
}

private fun commitRadioGroupEdit(ctrl: UICtrl) {
    val showOwners = SavedSettings.getBool("ShowParcelOwners")
    val group = ctrl as? RadioGroup ?: return
    when (group.getValue().asString()) {
        "radio position"    -> FloaterTools.setEditTool(ToolCompTranslate.instance)
        "radio rotate"      -> FloaterTools.setEditTool(ToolCompRotate.instance)
        "radio stretch"     -> FloaterTools.setEditTool(ToolCompScale.instance)
        "radio select face" -> FloaterTools.setEditTool(ToolFace)
        "radio align"       -> FloaterTools.setEditTool(QToolAlign.instance)
    }
    SavedSettings.setBool("ShowParcelOwners", showOwners)
}

private fun commitRadioGroupLand(ctrl: UICtrl) {
    val group = ctrl as? RadioGroup ?: return
    val selected = group.getValue().asString()
    if (selected == "radio select land") {
        FloaterTools.setEditTool(ToolSelectLand.instance)
    } else {
        FloaterTools.setEditTool(ToolBrushLand.instance)
        val dozerMode = when (selected) {
            "radio flatten" -> 0
            "radio raise"   -> 1
            "radio lower"   -> 2
            "radio smooth"  -> 3
            "radio noise"   -> 4
            "radio revert"  -> 5
            else            -> SavedSettings.getInt("RadioLandBrushAction")
        }
        SavedSettings.setInt("RadioLandBrushAction", dozerMode)
    }
}

private fun commitGridMode(ctrl: UICtrl) {
    val combo = ctrl as? ComboBox ?: return
    SelectMgr.instance.setGridMode(combo.getCurrentIndex())
}
