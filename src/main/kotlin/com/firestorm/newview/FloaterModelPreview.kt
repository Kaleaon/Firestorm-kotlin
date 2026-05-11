package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.Button
import com.firestorm.llui.ComboBox
import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.LineEditor
import com.firestorm.llui.TextBox
import com.firestorm.llui.UICtrl
import com.firestorm.llui.TabContainer
import com.firestorm.llui.Panel
import com.firestorm.llui.SpinCtrl
import com.firestorm.llui.SliderCtrl
import java.util.UUID

private const val RETAIN_COEFFICIENT      = 100.0
private const val SMOOTH_VALUES_NUMBER    = 10
private const val PREVIEW_CAMERA_DISTANCE = 16f
private const val NUM_LOD                 = 4

enum class LodMode {
    LOD_FROM_FILE,
    MESH_OPTIMIZER_AUTO,
    MESH_OPTIMIZER_SLOPPY,
    MESH_OPTIMIZER_PRECISE,
    GENERATE,
    USE_LOD_ABOVE,
}

data class JointOverrideData(
    val posOverrides: MutableMap<String, FloatArray> = mutableMapOf(),
    val modelsNoOverrides: MutableSet<String>        = mutableSetOf(),
    var hasConflicts: Boolean                        = false
)

typealias JointOverrideDataMap = MutableMap<String, JointOverrideData>

class DecompRequest(val stage: String, val model: Any?) {
    var shouldContinue: Int = 1
    private var statusMessage: String = ""

    fun statusCallback(status: String, p1: Int, p2: Int): Int {
        if (shouldContinue != 0) {
            statusMessage = "$status: $p1/$p2"
            FloaterModelPreview.instance?.setStatusMessage(statusMessage)
        }
        return shouldContinue
    }

    fun completed() {
        if (shouldContinue != 0) {
            TODO("GPU: model->setConvexHullDecomposition(mHull, mHullMesh)")
            FloaterModelPreview.instance?.also { inst ->
                inst.modelPreview?.also { mp ->
                    mp.dirty = true
                    TODO("GPU: mp.refresh()")
                }
            }
        }
        FloaterModelPreview.instance?.curRequest?.remove(this)
    }
}

class MeshFilePicker(private val modelPreview: Any?, private val lod: Int) {
    fun getFile() {
        TODO("APR: use JVM equivalent - open file picker for model files (FFLOAD_MODEL) then notify")
    }

    fun notify(filenames: List<String>) {
        if (filenames.isNotEmpty()) {
            TODO("APR: use JVM equivalent - modelPreview.loadModel(filenames[0], lod)")
        } else {
            TODO("APR: use JVM equivalent - modelPreview.loadModel(\"\", lod) to signal cancel")
        }
    }
}

open class FloaterModelUploadBase(key: Any) : Floater(key) {
    protected var hasUploadPerm: Boolean  = false
    protected var uploadModelUrl: String  = ""
    protected fun requestAgentUploadPermissions() { TODO("APR: use JVM equivalent - HTTP cap request for MeshUploadFlag") }
    open fun onPermissionsReceived(result: Any)                           {}
    open fun setPermissonsErrorStatus(status: Int, reason: String)        {}
    open fun onModelPhysicsFeeReceived(result: Any, uploadUrl: String)    {}
    open fun setModelPhysicsFeeErrorStatus(status: Int, reason: String, result: Any) {}
    open fun onModelUploadSuccess()  {}
    open fun onModelUploadFailure()  {}
    protected fun getWholeModelFeeObserverHandle(): Any    = TODO("APR: use JVM equivalent")
    protected fun getWholeModelUploadObserverHandle(): Any = TODO("APR: use JVM equivalent")
}

class FloaterModelPreview(key: Any) : FloaterModelUploadBase(key) {

    var modelPreview: Any?                              = null
    private var decompParams: MutableMap<String, Any>  = mutableMapOf()
    private var defaultDecompParams: MutableMap<String, Any> = mutableMapOf()

    private var lastMouseX: Int = 0
    private var lastMouseY: Int = 0
    private var previewRect: Any? = null

    val curRequest: MutableSet<DecompRequest> = mutableSetOf()
    private var statusMessage: String         = ""
    private val statusLock: Any               = Any()

    private val viewOptionDisabled: MutableMap<String, Boolean> = mutableMapOf()

    // Index 0 = LOD_IMPOSTOR .. 3 = LOD_HIGH
    private val lodMode: IntArray = IntArray(NUM_LOD)

    private var modelPhysicsFee: MutableMap<String, Any> = mutableMapOf()
    private var destinationFolderId: UUID                = UUID(0, 0)

    private var uploadBtn: Button?          = null
    private var calculateBtn: Button?       = null
    private var uploadLogText: Any?         = null
    private var tabContainer: TabContainer? = null
    private var avatarTabIndex: Int         = 0
    private var selectedJointName: String   = ""

    private val jointOverrides: Array<JointOverrideDataMap> = Array(NUM_LOD) { mutableMapOf() }

    companion object {
        var instance: FloaterModelPreview? = null
        var uploadAmount: Int = 10

        fun showModelPreview(destFolder: UUID = UUID(0, 0)) {
            val fmp: FloaterModelPreview = TODO("APR: use JVM equivalent - FloaterReg::getInstance(upload_model)")
            if (!fmp.isModelLoading()) {
                fmp.setUploadDestination(destFolder)
                fmp.loadHighLodModel()
            }
        }

        fun addStringToLog(message: String, args: Map<String, String>, flash: Boolean, lod: Int = -1) {
            val inst = instance ?: return
            val prefix = when (lod) {
                0 -> "LOD0 "
                1 -> "LOD1 "
                2 -> "LOD2 "
                4 -> "PHYS "
                3 -> "LOD3 "
                else -> ""
            }
            val str = prefix + inst.getString(message, args)
            inst.addStringToLogTab(str, flash)
        }

        fun addStringToLog(str: String, flash: Boolean) {
            instance?.addStringToLogTab(str, flash)
        }

        fun onMouseCaptureLostModelPreview(handler: Any?) {
            TODO("APR: use JVM equivalent - gViewerWindow->showCursor()")
        }

        private fun getBoundingBoxCubePath(): String {
            TODO("APR: use JVM equivalent - gDirUtilp->getAppRODataDir() + /cube.dae")
        }

        private fun getSourceFileFormat(filename: String): String {
            val ext = filename.substringAfterLast('.').lowercase()
            return when (ext) {
                "gltf", "glb" -> "gltf"
                "dae"         -> "dae"
                "slm"         -> "slm"
                else          -> "unknown file"
            }
        }
    }

    init {
        instance = this
        lodMode[3] = LodMode.LOD_FROM_FILE.ordinal
        val defaultToGlod: Boolean = TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(FSMeshUploadUseGLODAsDefault)")
        for (i in 0 until 3) {
            lodMode[i] = if (defaultToGlod) LodMode.GENERATE.ordinal else LodMode.MESH_OPTIMIZER_AUTO.ordinal
        }
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false

        childSetCommitCallback("cancel_btn")       { onCancel() }
        childSetCommitCallback("crease_angle")     { onGenerateNormalsCommit() }
        getChild<CheckBoxCtrl>("gen_normals").setCommitCallback { toggleGenerateNormals() }
        childSetCommitCallback("lod_generate")     { onAutoFillCommit() }

        val lodNames = listOf("lowest", "low", "medium", "high")
        for (lod in 0..3) {
            getChild<ComboBox>("lod_source_${lodNames[lod]}").apply {
                setCommitCallback { onLodSourceCommit(lod) }
                setCurrentByIndex(lodMode[lod])
            }
            getChild<Button>("lod_browse_${lodNames[lod]}").setCommitCallback { onBrowseLod(lod) }
            getChild<ComboBox>("lod_mode_${lodNames[lod]}").setCommitCallback { onLodParamCommit(lod, false) }
            getChild<SpinCtrl>("lod_error_threshold_${lodNames[lod]}").setCommitCallback { onLodParamCommit(lod, false) }
            getChild<SpinCtrl>("lod_triangle_limit_${lodNames[lod]}").setCommitCallback { onLodParamCommit(lod, true) }
        }

        childSetCommitCallback("upload_skin")                    { onUploadOptionChecked(it) }
        childSetCommitCallback("upload_joints")                  { onUploadOptionChecked(it) }
        childSetCommitCallback("lock_scale_if_joint_position")   { onUploadOptionChecked(it) }
        childSetCommitCallback("upload_textures")                { onUploadOptionChecked(it) }

        childSetTextArg("status", "[STATUS]", getString("status_idle"))
        childSetAction("ok_btn")    { onUpload() }
        childDisable("ok_btn")
        childSetAction("reset_btn") { onReset() }

        childSetCommitCallback("preview_lod_combo")  { onPreviewLodCommit() }
        childSetCommitCallback("import_scale")       { onImportScaleCommit() }
        childSetCommitCallback("pelvis_offset")      { onPelvisOffsetCommit() }

        getChild<LineEditor>("description_form").setKeystrokeCallback { onDescriptionKeystroke(it) }

        getChild<CheckBoxCtrl>("show_edges").setCommitCallback          { onViewOptionChecked(it) }
        getChild<CheckBoxCtrl>("show_physics").setCommitCallback        { onViewOptionChecked(it) }
        getChild<CheckBoxCtrl>("show_textures").setCommitCallback       { onViewOptionChecked(it) }
        getChild<CheckBoxCtrl>("show_skin_weight").setCommitCallback    { onShowSkinWeightChecked(it) }
        getChild<CheckBoxCtrl>("show_joint_overrides").setCommitCallback { onViewOptionChecked(it) }
        getChild<CheckBoxCtrl>("show_joint_positions").setCommitCallback { onViewOptionChecked(it) }
        getChild<CheckBoxCtrl>("show_uv_guide").setCommitCallback       { onViewOptionChecked(it) }

        val previewRefreshCb: (UICtrl) -> Unit = { modelPreviewRefresh() }
        getChild<UICtrl>("mesh_preview_canvas_color").setCommitCallback          { previewRefreshCb(it) }
        getChild<UICtrl>("mesh_preview_edge_color").setCommitCallback            { previewRefreshCb(it) }
        getChild<UICtrl>("mesh_preview_physics_edge_color").setCommitCallback    { previewRefreshCb(it) }
        getChild<UICtrl>("mesh_preview_physics_fill_color").setCommitCallback    { previewRefreshCb(it) }
        getChild<UICtrl>("mesh_preview_degenerate_edge_color").setCommitCallback { previewRefreshCb(it) }
        getChild<UICtrl>("mesh_preview_degenerate_fill_color").setCommitCallback { previewRefreshCb(it) }

        getChild<ComboBox>("lod_suffix_combo").setCommitCallback { onSuffixStandardSelected() }
        getChild<Button>("set_user_def_phys").setCommitCallback  { onSelectUdPhysics() }

        childDisable("upload_skin")
        childDisable("upload_joints")
        childDisable("lock_scale_if_joint_position")
        childSetVisible("skin_too_many_joints", false)
        childSetVisible("skin_unknown_joint",   false)
        childSetVisible("warning_title",        false)
        childSetVisible("warning_message",      false)

        initDecompControls()

        previewRect = getChild<UICtrl>("preview_panel").getRect()
        initModelPreview()

        for (i in 0..3) {
            listOf(
                "lod_label_$i", "lod_triangles_$i", "lod_vertices_$i", "lod_status_$i"
            ).forEach { childName ->
                findChild<TextBox>(childName)?.setMouseDownCallback { setPreviewLod(i) }
            }
        }

        val validateUrl: String = TODO("APR: use JVM equivalent - determine mesh validate URL from grid manager")
        getChild<TextBox>("warning_message").setTextArg("[VURL]", validateUrl)

        uploadBtn        = getChild("ok_btn")
        calculateBtn     = getChild("calculate_btn")
        uploadLogText    = getChild("log_text")
        tabContainer     = getChild("import_tab")
        val riggingPanel = tabContainer!!.getPanelByName("rigging_panel")
        avatarTabIndex   = tabContainer!!.getIndexForPanel(riggingPanel)
        riggingPanel.getChild<UICtrl>("joints_list").setCommitCallback { onJointListSelection() }

        if (convexDecompositionAvailable()) {
            calculateBtn!!.setClickedCallback { onClickCalculateBtn() }
            toggleCalculateButton(visible = true)
        } else {
            calculateBtn!!.setEnabled(false)
        }

        return true
    }

    open fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        super.reshape(width, height, calledFromParent)
        val previewPanel = findChild<UICtrl>("preview_panel") ?: return
        val rect = previewPanel.getRect()
        if (rect != previewRect) {
            modelPreviewRefresh()
            previewRect = rect
        }
    }

    override fun onDestroy() {
        instance = null
        TODO("APR: use JVM equivalent - delete mModelPreview; delete mStatusLock")
    }

    fun initModelPreview() {
        val maxDim: Int = TODO("APR: use JVM equivalent - min(gSavedSettings.getS32(PreviewRenderSize), gPipeline.mRT.width/height)")
        var texWidth  = 512
        var texHeight = 512
        while (texWidth  * 2 <= maxDim) texWidth  *= 2
        while (texHeight * 2 <= maxDim) texHeight *= 2

        modelPreview = TODO("GPU: create LLModelPreview(texWidth, texHeight, this)")
        TODO("GPU: modelPreview.setPreviewTarget(PREVIEW_CAMERA_DISTANCE)")
        TODO("APR: use JVM equivalent - modelPreview.setDetailsCallback { x, y, z -> setDetails(x, y, z) }")
        TODO("APR: use JVM equivalent - modelPreview.setModelUpdatedCallback { visible -> modelUpdated(visible) }")
    }

    fun setUploadDestination(destFolder: UUID) {
        destinationFolderId = destFolder
    }

    private fun onUploadOptionChecked(ctrl: UICtrl) {
        val mp = modelPreview ?: return
        val name  = ctrl.getName()
        val value = ctrl.getValue() as? Boolean ?: false
        when (name) {
            "upload_skin" -> {
                childSetValue("show_skin_weight", value)
                setViewOption(mp, "show_skin_weight", value)
                if (!value) {
                    setViewOption(mp, "show_joint_overrides", false)
                    setViewOption(mp, "show_joint_positions", false)
                    childSetValue("show_joint_overrides", false)
                    childSetValue("show_joint_positions", false)
                }
            }
            "upload_joints" -> {
                if (getViewOption(mp, "show_skin_weight")) {
                    childSetValue("show_joint_overrides", value)
                    setViewOption(mp, "show_joint_overrides", value)
                }
            }
            "upload_textures" -> {
                childSetValue("show_textures", value)
                setViewOption(mp, "show_textures", value)
            }
            "lock_scale_if_joint_position" -> {
                setViewOption(mp, "lock_scale_if_joint_position", value)
            }
        }
        modelPreviewRefresh()
        TODO("APR: use JVM equivalent - modelPreview.resetPreviewTarget(); modelPreview.clearBuffers(); modelPreview.mDirty = true")
        toggleCalculateButton(visible = true)
    }

    private fun onShowSkinWeightChecked(ctrl: UICtrl) {
        if (modelPreview != null) {
            TODO("APR: use JVM equivalent - modelPreview.mCameraOffset.clearVec()")
            onViewOptionChecked(ctrl)
        }
    }

    private fun onViewOptionChecked(ctrl: UICtrl) {
        if (modelPreview != null) {
            val name = ctrl.getName()
            val current = getViewOption(modelPreview!!, name)
            setViewOption(modelPreview!!, name, !current)
            modelPreviewRefresh()
        }
    }

    fun isViewOptionChecked(userdata: Any): Boolean =
        modelPreview?.let { getViewOption(it, userdata.toString()) } ?: false

    fun isViewOptionEnabled(userdata: Any): Boolean =
        getChildView(userdata.toString()).getEnabled()

    fun setViewOptionEnabled(option: String, enabled: Boolean) {
        childSetEnabled(option, enabled)
    }

    fun enableViewOption(option: String)  = setViewOptionEnabled(option, true)
    fun disableViewOption(option: String) = setViewOptionEnabled(option, false)

    fun isModelLoading(): Boolean {
        return TODO("APR: use JVM equivalent - modelPreview?.mLoading ?: false")
    }

    fun loadHighLodModel() {
        TODO("APR: use JVM equivalent - modelPreview.mLookUpLodFiles = true")
        loadModel(3)
    }

    private fun prepareToLoadModel(lod: Int) {
        TODO("APR: use JVM equivalent - check modelPreview.mLoading; set modelPreview.mLoading = true; configure physics search LOD if lod == LOD_PHYSICS")
    }

    fun loadModel(lod: Int) {
        prepareToLoadModel(lod)
        MeshFilePicker(modelPreview, lod).getFile()
    }

    fun loadModel(lod: Int, fileName: String, forceDisableSlm: Boolean = false) {
        prepareToLoadModel(lod)
        TODO("APR: use JVM equivalent - modelPreview.loadModel(fileName, lod, forceDisableSlm)")
    }

    private fun onClickCalculateBtn() {
        clearLogTab()
        addStringToLog("Calculating model data.", false)
        TODO("APR: use JVM equivalent - modelPreview.rebuildUploadData()")

        val uploadSkinweights     = childGetValue("upload_skin") as Boolean
        val uploadJointPositions  = childGetValue("upload_joints") as Boolean
        val lockScaleIfJointPos   = childGetValue("lock_scale_if_joint_position") as Boolean

        uploadModelUrl = ""
        modelPhysicsFee.clear()

        val lodSources = fillLodSourceStatistics()

        TODO("APR: use JVM equivalent - gMeshRepo.uploadModel(...) to request fee; getWholeModelFeeObserverHandle()")

        toggleCalculateButton(visible = false)
        uploadBtn?.setEnabled(false)

        TODO("APR: use JVM equivalent - disable all children of physics simplification panel")
    }

    fun clearAvatarTab() {
        val panel = tabContainer!!.getPanelByName("rigging_panel")
        panel.getChild<UICtrl>("joints_list").deleteAllItems()
        panel.getChild<UICtrl>("pos_overrides_list").deleteAllItems()
        selectedJointName = ""
        for (i in 0 until NUM_LOD) jointOverrides[i].clear()
        panel.getChild<TextBox>("conflicts_description").apply {
            setTextArg("[CONFLICTS]", "0")
            setTextArg("[JOINTS_COUNT]", "0")
        }
        panel.getChild<TextBox>("pos_overrides_descr").setTextArg("[JOINT]", "mPelvis")
    }

    fun updateAvatarTab(highlightOverrides: Boolean) {
        val displayLod: Int = TODO("APR: use JVM equivalent - modelPreview.mPreviewLOD")
        if (modelIsEmpty(displayLod)) {
            selectedJointName = ""
            return
        }

        if (jointOverrides[displayLod].isEmpty()) {
            TODO("APR: use JVM equivalent - populate mJointOverrides[displayLod] from mScene[displayLod] skin info")
        }

        val panel = tabContainer!!.getPanelByName("rigging_panel")
        val jointsList = panel.getChild<UICtrl>("joints_list")

        if (jointsListIsEmpty(jointsList)) {
            TODO("APR: use JVM equivalent - populate joints_list from mJointOverrides[displayLod], counting conflicts; update conflicts_description text args")
        }
    }

    fun setDetails(x: Float, y: Float, z: Float) {
        childSetTextArg("import_dimensions", "[X]", "%.3f".format(x))
        childSetTextArg("import_dimensions", "[Y]", "%.3f".format(y))
        childSetTextArg("import_dimensions", "[Z]", "%.3f".format(z))
    }

    fun setPreviewLod(lod: Int) {
        TODO("APR: use JVM equivalent - modelPreview?.setPreviewLOD(lod)")
    }

    fun onBrowseLod(lod: Int) {
        loadModel(lod)
    }

    private fun onReset() {
        childDisable("reset_btn")
        clearLogTab()
        clearAvatarTab()
        val filename: String = TODO("APR: use JVM equivalent - modelPreview.mLODFile[LOD_HIGH]")
        resetDisplayOptions()
        resetUploadOptions()
        initModelPreview()
        TODO("APR: use JVM equivalent - modelPreview.loadModel(filename, LOD_HIGH, true)")
    }

    private fun onUpload() {
        clearLogTab()
        uploadBtn?.setEnabled(false)
        TODO("APR: use JVM equivalent - modelPreview.rebuildUploadData()")

        val uploadSkinweights    = childGetValue("upload_skin") as Boolean
        val uploadJointPositions = childGetValue("upload_joints") as Boolean
        val lockScaleIfJoint     = childGetValue("lock_scale_if_joint_position") as Boolean

        if (TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(MeshImportUseSLM)")) {
            TODO("APR: use JVM equivalent - modelPreview.saveUploadData(uploadSkinweights, uploadJointPositions, lockScaleIfJoint)")
        }

        val lodSources = fillLodSourceStatistics()
        TODO("APR: use JVM equivalent - gMeshRepo.uploadModel(..., getWholeModelUploadObserverHandle())")
    }

    fun refresh() {
        instance?.toggleCalculateButton(visible = true)
        TODO("APR: use JVM equivalent - modelPreview.mDirty = true")
    }

    private fun onJointListSelection() {
        val displayLod: Int = TODO("APR: use JVM equivalent - modelPreview.mPreviewLOD")
        val panel = tabContainer!!.getPanelByName("rigging_panel")
        val jointsList  = panel.getChild<UICtrl>("joints_list")
        val jointsPos   = panel.getChild<UICtrl>("pos_overrides_list")
        val jointDescr  = panel.getChild<TextBox>("pos_overrides_descr")

        jointsPos.deleteAllItems()

        val selected = getFirstSelectedItem(jointsList)
        if (selected != null) {
            val label = selected.getValue().toString()
            val data  = jointOverrides[displayLod][label] ?: JointOverrideData()
            val uploadJointPositions = childGetValue("upload_joints") as Boolean
            populateListWithOverrides(jointsPos, data, uploadJointPositions)
            jointDescr.setTextArg("[JOINT]", label)
            selectedJointName = label
        } else {
            jointDescr.setTextArg("[JOINT]", "mPelvis")
            selectedJointName = ""
        }
    }

    private fun onDescriptionKeystroke(ctrl: UICtrl) {
        val input = ctrl as? LineEditor ?: return
        if (input.isDirty()) {
            toggleCalculateButton(visible = true)
        }
    }

    private fun onImportScaleCommit() {
        TODO("APR: use JVM equivalent - modelPreview.mDirty = true; toggleCalculateButton(true); modelPreview.refresh()")
    }

    private fun onPelvisOffsetCommit() {
        TODO("APR: use JVM equivalent - modelPreview.mDirty = true; toggleCalculateButton(true); modelPreview.refresh()")
    }

    private fun onPreviewLodCommit() {
        val combo = getChild<ComboBox>("preview_lod_combo")
        val whichMode = (NUM_LOD - 1) - combo.getFirstSelectedIndex()
        TODO("APR: use JVM equivalent - modelPreview.setPreviewLOD(whichMode)")
    }

    private fun onGenerateNormalsCommit() {
        TODO("APR: use JVM equivalent - modelPreview.generateNormals()")
    }

    private fun toggleGenerateNormals() {
        val enabled = childGetValue("gen_normals") as Boolean
        setViewOption(modelPreview!!, "gen_normals", enabled)
        childSetEnabled("crease_angle", enabled)
        if (enabled) {
            TODO("APR: use JVM equivalent - modelPreview.generateNormals()")
        } else {
            TODO("APR: use JVM equivalent - modelPreview.restoreNormals()")
        }
    }

    private fun onAutoFillCommit() {
        TODO("APR: use JVM equivalent - modelPreview.queryLODs()")
    }

    private fun onLodParamCommit(lod: Int, enforceTriLimit: Boolean) {
        val lodNames = listOf("lowest", "low", "medium", "high")
        val combo = getChild<ComboBox>("lod_source_${lodNames[lod]}")
        val mode  = combo.getCurrentIndex()
        when (mode) {
            LodMode.MESH_OPTIMIZER_AUTO.ordinal,
            LodMode.MESH_OPTIMIZER_SLOPPY.ordinal,
            LodMode.MESH_OPTIMIZER_PRECISE.ordinal ->
                TODO("APR: use JVM equivalent - modelPreview.onLODMeshOptimizerParamCommit(lod, enforceTriLimit, mode)")
            LodMode.GENERATE.ordinal ->
                TODO("APR: use JVM equivalent - modelPreview.onLODGLODParamCommit(lod, enforceTriLimit)")
            else -> error("onLodParamCommit called with non-generative mode $mode")
        }
        for (i in lod - 1 downTo 0) {
            val lowerCombo = getChild<ComboBox>("lod_source_${lodNames[i]}")
            if (lowerCombo.getCurrentIndex() == LodMode.USE_LOD_ABOVE.ordinal) {
                onLodSourceCommit(i)
            } else {
                break
            }
        }
    }

    private fun draw3dPreview() {
        TODO("GPU: render model preview texture into mPreviewRect using two triangles")
    }

    open fun draw() {
        super.draw()
        val mp = modelPreview ?: return
        TODO("APR: use JVM equivalent - mp.update()")
        TODO("APR: use JVM equivalent - update status text from mp.getLoadState()")
        if (!isMinimized() && lodsReady()) draw3dPreview()
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (pointInPreviewRect(x, y)) {
            bringToFront(x, y)
            TODO("APR: use JVM equivalent - gFocusMgr.setMouseCapture(this)")
            TODO("APR: use JVM equivalent - gViewerWindow.hideCursor()")
            lastMouseX = x
            lastMouseY = y
            return true
        }
        return super.handleMouseDown(x, y, mask)
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        TODO("APR: use JVM equivalent - gFocusMgr.setMouseCapture(null); gViewerWindow.showCursor()")
        return super.handleMouseUp(x, y, mask)
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        val localMask = mask and MASK_ALT.inv()
        if (modelPreview != null && hasMouseCapture()) {
            when (localMask) {
                MASK_PAN   -> TODO("APR: use JVM equivalent - modelPreview.pan(dx * -0.005f, dy * -0.005f)")
                MASK_ORBIT -> {
                    val yawRadians   = (x - lastMouseX) * -0.01f
                    val pitchRadians = (y - lastMouseY) *  0.02f
                    TODO("APR: use JVM equivalent - modelPreview.rotate(yawRadians, pitchRadians)")
                }
                else -> {
                    val yawRadians = (x - lastMouseX) * -0.01f
                    val zoomAmt    = (y - lastMouseY) *  0.02f
                    TODO("APR: use JVM equivalent - modelPreview.rotate(yawRadians, 0f); modelPreview.zoom(zoomAmt)")
                }
            }
            modelPreviewRefresh()
            TODO("APR: use JVM equivalent - LLUI.setMousePositionLocal(this, lastMouseX, lastMouseY)")
        }

        if (!pointInPreviewRect(x, y) || modelPreview == null) {
            return super.handleHover(x, y, mask)
        }
        when (localMask) {
            MASK_ORBIT -> TODO("APR: use JVM equivalent - gViewerWindow.setCursor(UI_CURSOR_TOOLCAMERA)")
            MASK_PAN   -> TODO("APR: use JVM equivalent - gViewerWindow.setCursor(UI_CURSOR_TOOLPAN)")
            else       -> TODO("APR: use JVM equivalent - gViewerWindow.setCursor(UI_CURSOR_TOOLZOOMIN)")
        }
        return true
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (pointInPreviewRect(x, y) && modelPreview != null) {
            TODO("APR: use JVM equivalent - modelPreview.zoom(clicks * -0.2f); modelPreview.refresh()")
        } else {
            super.handleScrollWheel(x, y, clicks)
        }
        return true
    }

    open fun onOpen(key: Any) {
        TODO("APR: use JVM equivalent - LLModelPreview.sIgnoreLoadedCallback = false")
        requestAgentUploadPermissions()
    }

    override fun onClose(appQuitting: Boolean) {
        TODO("APR: use JVM equivalent - LLModelPreview.sIgnoreLoadedCallback = true")
    }

    private fun onPhysicsParamCommit(ctrl: UICtrl, paramName: String) {
        var value: Any = ctrl.getValue()
        if (paramName == "Retain%") {
            value = (ctrl.getValue() as? Double ?: 0.0) / RETAIN_COEFFICIENT
        }
        decompParams[paramName] = value
        if (paramName == "Simplify Method") {
            val showRetain = ctrl.getValue() as? Int == 0
            childSetVisible("Retain%",       showRetain)
            childSetVisible("Retain%_label", showRetain)
            childSetVisible("Detail Scale",       !showRetain)
            childSetVisible("Detail Scale label", !showRetain)
        }
    }

    private fun onPhysicsStageExecute(stageName: String) {
        if (curRequest.isNotEmpty()) return
        TODO("APR: use JVM equivalent - iterate modelPreview.mModel[LOD_PHYSICS], create DecompRequest per model, submit to gMeshRepo.mDecompThread")
        when (stageName) {
            "Analyze"   -> { setStatusMessage(getString("decomposing")); childSetVisible("Analyze", false); childSetVisible("analyze_cancel", true) }
            "Decompose" -> { setStatusMessage(getString("decomposing")); childSetVisible("Decompose", false); childSetVisible("decompose_cancel", true); childDisable("Simplify") }
            "Simplify"  -> { setStatusMessage(getString("simplifying")); childSetVisible("Simplify", false); childSetVisible("simplify_cancel", true); childDisable("Decompose") }
        }
    }

    private fun onPhysicsBrowse() {
        loadModel(TODO("APR: use JVM equivalent - LLModel.LOD_PHYSICS"))
    }

    private fun onPhysicsUseLod() {
        val numLods  = 4
        val iface    = childGetSelectionInterface("physics_lod_combo") ?: return
        val whichMode = iface.getFirstSelectedIndex()
        val fileMode  = iface.getItemCount() - 1
        val cubeMode  = fileMode - 1
        if (whichMode < cubeMode) {
            if (whichMode > numLods) {
                TODO("APR: use JVM equivalent - modelPreview.setPhysicsFromPreset(whichMode - numLods)")
            } else {
                val whichLod = numLods - whichMode
                TODO("APR: use JVM equivalent - modelPreview.setPhysicsFromLOD(whichLod)")
            }
        } else if (whichMode == cubeMode) {
            loadModel(TODO("APR: use JVM equivalent - LLModel.LOD_PHYSICS"), getBoundingBoxCubePath())
        }
        TODO("APR: use JVM equivalent - modelPreview.refresh(); modelPreview.updateStatusMessages()")
    }

    private fun onSuffixStandardSelected() {
        val iface = childGetSelectionInterface("lod_suffix_combo") ?: return
        val which = iface.getFirstSelectedIndex()
        val slSuffixes   = listOf("LOD0", "LOD1", "LOD2", "",     "PHYS")
        val stdSuffixes  = listOf("LOD3", "LOD2", "LOD1", "LOD0", "PHYS")
        val descSuffixes = listOf("LOWEST", "LOW", "MED", "HIGH", "PHYS")
        val suffixes = when (which) {
            1    -> slSuffixes
            2    -> stdSuffixes
            3    -> descSuffixes
            else -> return
        }
        TODO("APR: use JVM equivalent - store suffixes into gSavedSettings via LLModelPreview.sSuffixVarNames")
    }

    private fun onSelectUdPhysics() {
        TODO("APR: use JVM equivalent - open file picker for Collada file; store result in FSPhysicsPresetUser1 setting")
    }

    private fun onCancel() {
        closeFloater(false)
    }

    private fun onPhysicsStageCancel() {
        for (req in curRequest) req.shouldContinue = 0
        curRequest.clear()
        TODO("APR: use JVM equivalent - modelPreview?.updateStatusMessages()")
    }

    private fun initDecompControls() {
        childSetCommitCallback("simplify_cancel")  { onPhysicsStageCancel() }
        childSetCommitCallback("decompose_cancel") { onPhysicsStageCancel() }
        childSetCommitCallback("analyze_cancel")   { onPhysicsStageCancel() }
        childSetCommitCallback("physics_lod_combo") { onPhysicsUseLod() }
        childSetCommitCallback("physics_browse")    { onPhysicsBrowse() }

        TODO("APR: use JVM equivalent - iterate LLConvexDecomposition stages and params; bind UI controls; build smooth combo; store defaults")

        defaultDecompParams = decompParams.toMutableMap()
        childSetCommitCallback("physics_explode") { modelPreviewRefresh() }
    }

    private fun createSmoothComboBox(comboBox: ComboBox, min: Float, max: Float) {
        val delta = (max - min) / SMOOTH_VALUES_NUMBER
        comboBox.add("0 (none)")
        var ilabel = 0
        var value = min + delta
        while (value < max) {
            ilabel++
            val label = if (ilabel == SMOOTH_VALUES_NUMBER) "10 (max)" else ilabel.toString()
            comboBox.add(label, value)
            value += delta
        }
    }

    fun setStatusMessage(msg: String) {
        synchronized(statusLock) { statusMessage = msg }
    }

    private fun addStringToLogTab(str: String, flash: Boolean) {
        if (str.isEmpty()) return
        TODO("APR: use JVM equivalent - append str to mUploadLogText, trimming oldest lines if at capacity; flash logs_panel tab if needed")
    }

    fun setCtrlLoadFromFile(lod: Int) {
        val lodPhysics: Int = TODO("APR: use JVM equivalent - LLModel.LOD_PHYSICS constant")
        if (lod == lodPhysics) {
            findChild<ComboBox>("physics_lod_combo")?.apply {
                setCurrentByIndex(getItemCount() - 1)
            }
        } else {
            val lodNames = listOf("lowest", "low", "medium", "high")
            findChild<ComboBox>("lod_source_${lodNames[lod]}")?.setCurrentByIndex(0)
        }
    }

    fun toggleCalculateButton(visible: Boolean = true) {
        calculateBtn?.setVisible(visible)

        val uploadingSkin           = childGetValue("upload_skin")   as? Boolean ?: false
        val uploadingJointPositions = childGetValue("upload_joints") as? Boolean ?: false
        if (uploadingSkin && uploadingJointPositions) {
            val rigValid: Boolean = TODO("APR: use JVM equivalent - modelPreview.isRigValidForJointPositionUpload()")
            if (!rigValid) calculateBtn?.setVisible(false)
        }

        uploadBtn?.setVisible(!visible)
        uploadBtn?.setEnabled(isModelUploadAllowed())

        if (visible) {
            val tbd   = getString("tbd")
            val dashes = if (hasString("--")) getString("--") else "--"
            childSetTextArg("prim_weight",     "[EQ]",  tbd)
            childSetTextArg("download_weight", "[ST]",  tbd)
            childSetTextArg("server_weight",   "[SIM]", tbd)
            childSetTextArg("physics_weight",  "[PH]",  tbd)
            if (modelPhysicsFee.isEmpty()) childSetTextArg("upload_fee", "[FEE]", tbd)
            listOf("[STREAMING]", "[PHYSICS]", "[INSTANCES]", "[TEXTURES]", "[MODEL]").forEach {
                childSetTextArg("price_breakdown", it, dashes)
            }
            listOf("[PCH]", "[PM]", "[PHU]").forEach { childSetTextArg("physics_breakdown", it, dashes) }
        }
    }

    private fun onLodSourceCommit(lod: Int) {
        TODO("APR: use JVM equivalent - modelPreview.updateLodControls(lod)")
        val lodNames = listOf("lowest", "low", "medium", "high")
        val combo = getChild<ComboBox>("lod_source_${lodNames[lod]}")
        val lodFile: String = TODO("APR: use JVM equivalent - modelPreview.mLODFile[lod]")
        if (combo.getCurrentIndex() == LodMode.LOD_FROM_FILE.ordinal && lodFile.isEmpty()) return

        refresh()

        val index = combo.getCurrentIndex()
        if (index in listOf(LodMode.MESH_OPTIMIZER_AUTO.ordinal, LodMode.GENERATE.ordinal,
                LodMode.MESH_OPTIMIZER_SLOPPY.ordinal, LodMode.MESH_OPTIMIZER_PRECISE.ordinal)) {
            onLodParamCommit(lod, true)
        }
        if (index == LodMode.USE_LOD_ABOVE.ordinal) {
            TODO("APR: use JVM equivalent - modelPreview.mDirty = true")
        }
    }

    private fun resetDisplayOptions() {
        TODO("APR: use JVM equivalent - iterate modelPreview.mViewOption and set each UI control to false")
    }

    private fun resetUploadOptions() {
        childSetValue("import_scale",    1)
        childSetValue("pelvis_offset",   0)
        childSetValue("physics_explode", 0)
        childSetValue("physics_file",    "")
        childSetVisible("Retain%",            false)
        childSetVisible("Retain%_label",      false)
        childSetVisible("Detail Scale",       true)
        childSetVisible("Detail Scale label", true)
        val lodNames = listOf("lowest", "low", "medium", "high")
        getChild<ComboBox>("lod_source_${lodNames[NUM_LOD - 1]}").setCurrentByIndex(LodMode.LOD_FROM_FILE.ordinal)
        for (lod in 0 until NUM_LOD - 1) {
            getChild<ComboBox>("lod_source_${lodNames[lod]}").setCurrentByIndex(LodMode.MESH_OPTIMIZER_AUTO.ordinal)
            childSetValue("lod_file_${lodNames[lod]}", "")
        }
        for ((ctrlName, value) in defaultDecompParams) {
            findChild<UICtrl>(ctrlName)?.setValue(value)
        }
        getChild<ComboBox>("physics_lod_combo").setCurrentByIndex(0)
        getChild<ComboBox>("Cosine%").setCurrentByIndex(0)
    }

    private fun clearLogTab() {
        TODO("APR: use JVM equivalent - mUploadLogText.clear(); stop tab flashing")
    }

    private fun modelUpdated(calculateVisible: Boolean) {
        modelPhysicsFee.clear()
        toggleCalculateButton(calculateVisible)
    }

    override fun onModelPhysicsFeeReceived(result: Any, uploadUrl: String) {
        TODO("APR: use JVM equivalent - store result and url in modelPhysicsFee; schedule handleModelPhysicsFeeReceived on idle")
    }

    private fun handleModelPhysicsFeeReceived() {
        TODO("APR: use JVM equivalent - unpack modelPhysicsFee and update all weight / fee / breakdown UI labels; show upload_fee and price_breakdown; enable upload button")
    }

    override fun setModelPhysicsFeeErrorStatus(status: Int, reason: String, result: Any) {
        addStringToLog("LLFloaterModelPreview::setModelPhysicsFeeErrorStatus($status : $reason)", false)
        TODO("APR: use JVM equivalent - schedule toggleCalculateButton(true) on idle; update upload_fee if result has upload_price")
    }

    override fun onModelUploadSuccess() {
        closeFloater(false)
    }

    override fun onModelUploadFailure() {
        toggleCalculateButton(visible = true)
        uploadBtn?.setEnabled(true)
    }

    fun isModelUploadAllowed(): Boolean {
        val modelNoErrors: Boolean = TODO("APR: use JVM equivalent - modelPreview?.mModelNoErrors ?: false")
        return hasUploadPerm && uploadModelUrl.isNotEmpty() && modelNoErrors
    }

    override fun onPermissionsReceived(result: Any) {
        val uploadStatus: String = TODO("APR: use JVM equivalent - result[mesh_upload_status].asString()")
        hasUploadPerm = uploadStatus.isEmpty() || uploadStatus == "valid"
        uploadBtn?.setEnabled(isModelUploadAllowed())
        getChild<TextBox>("warning_title").setVisible(!hasUploadPerm)
        getChild<TextBox>("warning_message").setVisible(!hasUploadPerm)
    }

    override fun setPermissonsErrorStatus(status: Int, reason: String) {
        TODO("APR: use JVM equivalent - show MeshUploadPermError notification")
    }

    private fun fillLodSourceStatistics(): Map<String, String> {
        val lodSources = mutableMapOf<String, String>()
        val lodNames   = listOf("lowest", "low", "medium", "high")
        for (lod in 0..3) {
            lodSources[lodNames[lod]] = when (lodMode[lod]) {
                LodMode.USE_LOD_ABOVE.ordinal -> "lod above"
                LodMode.MESH_OPTIMIZER_AUTO.ordinal,
                LodMode.MESH_OPTIMIZER_PRECISE.ordinal,
                LodMode.MESH_OPTIMIZER_SLOPPY.ordinal -> "generated"
                LodMode.LOD_FROM_FILE.ordinal -> {
                    val file: String = TODO("APR: use JVM equivalent - modelPreview.mLODFile[lod]")
                    getSourceFileFormat(file)
                }
                else -> "unknown source"
            }
        }
        val physFile: String = TODO("APR: use JVM equivalent - modelPreview.mLODFile[LOD_PHYSICS]")
        if (physFile.isEmpty()) {
            val physSearch: Int = TODO("APR: use JVM equivalent - modelPreview.mPhysicsSearchLOD")
            lodSources["physics"] = if (physSearch in 0..3) lodNames[physSearch] else "none"
        } else {
            lodSources["physics"] = if (physFile == getBoundingBoxCubePath()) "bounding box"
                                    else getSourceFileFormat(physFile)
        }
        return lodSources
    }

    // ---------------------------------------------------------------------------
    // Stubs for C++ subsystems without direct JVM equivalents
    // ---------------------------------------------------------------------------

    private fun populateListWithOverrides(list: UICtrl, data: JointOverrideData, includeOverrides: Boolean) =
        TODO("APR: use JVM equivalent - add rows to scroll list from data.posOverrides and data.modelsNoOverrides")

    private fun convexDecompositionAvailable(): Boolean =
        TODO("APR: use JVM equivalent - LLConvexDecomposition::getInstance() != null")

    private fun lodsReady(): Boolean       = TODO("APR: use JVM equivalent - modelPreview.lodsReady()")
    private fun modelIsEmpty(lod: Int): Boolean = TODO("APR: use JVM equivalent - modelPreview.mModel[lod].empty()")
    private fun jointsListIsEmpty(list: UICtrl): Boolean = TODO("APR: use JVM equivalent - list.isEmpty()")
    private fun getFirstSelectedItem(list: UICtrl): Any? = TODO("APR: use JVM equivalent - list.getFirstSelected()")
    private fun pointInPreviewRect(x: Int, y: Int): Boolean = TODO("APR: use JVM equivalent - mPreviewRect.pointInRect(x, y)")
    private fun modelPreviewRefresh() = TODO("APR: use JVM equivalent - modelPreview?.refresh()")
    private fun setViewOption(mp: Any, name: String, value: Boolean) =
        TODO("APR: use JVM equivalent - (mp as LLModelPreview).mViewOption[name] = value")
    private fun getViewOption(mp: Any, name: String): Boolean =
        TODO("APR: use JVM equivalent - (mp as LLModelPreview).mViewOption[name] ?: false")
    private fun childGetSelectionInterface(name: String): Any? =
        TODO("APR: use JVM equivalent - childGetSelectionInterface for named combo box")

    private companion object {
        const val MASK_ALT   = 0x01
        const val MASK_PAN   = 0x02
        const val MASK_ORBIT = 0x04
    }
}
