package com.firestorm.newview

import java.util.UUID

private const val RETAIN_COEFFICIENT = 100.0
private const val SMOOTH_VALUES_NUMBER = 10
private const val PREVIEW_CAMERA_DISTANCE = 16f
private const val NUM_LOD = 4

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
    val modelsNoOverrides: MutableSet<String> = mutableSetOf(),
    var hasConflicts: Boolean = false
)

typealias JointOverrideDataMap = MutableMap<String, JointOverrideData>

// Stub for physics decomp request
class DecompRequest(val stage: String, val model: Any?) {
    var shouldContinue: Int = 1

    fun statusCallback(status: String, p1: Int, p2: Int): Int {
        TODO("APR: relay decomp progress to UI status label")
    }

    fun completed() {
        TODO("APR: notify FloaterModelPreview that decomp stage finished")
    }
}

open class FloaterModelUploadBase(val key: Any?) {
    open fun postBuild(): Boolean = true
    open fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) = Unit
    open fun onPermissionsReceived(result: Any?) = Unit
    open fun setPermissionsErrorStatus(status: Int, reason: String) = Unit
    open fun onModelPhysicsFeeReceived(result: Any?, uploadUrl: String) = Unit
    open fun setModelPhysicsFeeErrorStatus(status: Int, reason: String, result: Any?) = Unit
    open fun onModelUploadSuccess() = Unit
    open fun onModelUploadFailure() = Unit
    open fun handleScrollWheel(x: Int, y: Int, clicks: Int) = Unit
    fun requestAgentUploadPermissions() = TODO("APR: request upload capability from agent region")

    var uploadModelUrl: String = ""
    val wholeModelFeeObserverHandle: Any? = null
}

class FloaterModelPreview(key: Any?) : FloaterModelUploadBase(key) {

    companion object {
        var instance: FloaterModelPreview? = null
        var uploadAmount: Int = 10

        fun showModelPreview(destFolder: UUID? = null) {
            val fmp = instance ?: return
            if (!fmp.isModelLoading()) {
                fmp.setUploadDestination(destFolder)
                fmp.loadHighLodModel()
            }
        }

        fun onMouseCaptureLostModelPreview(handler: Any?) {
            TODO("APR: release mouse capture from model preview")
        }

        fun addStringToLog(message: String, args: Any?, flash: Boolean, lod: Int = -1) {
            instance?.addStringToLogTab(message, flash)
        }

        fun addStringToLog(str: String, flash: Boolean) {
            instance?.addStringToLogTab(str, flash)
        }

        // LOD suffix standards
        private val slSuffixes = listOf("LOD0", "LOD1", "LOD2", "", "PHYS")
        private val stdSuffixes = listOf("LOD3", "LOD2", "LOD1", "LOD0", "PHYS")
        private val descSuffixes = listOf("LOWEST", "LOW", "MED", "HIGH", "PHYS")
    }

    var modelPreview: Any? = null // ModelPreview stub
    val decompParams: MutableMap<String, Any> = mutableMapOf()
    val defaultDecompParams: MutableMap<String, Any> = mutableMapOf()

    var lastMouseX: Int = 0
    var lastMouseY: Int = 0
    var previewRect: IntArray = IntArray(4) // left, bottom, right, top

    val curRequest: MutableSet<DecompRequest> = mutableSetOf()
    var statusMessage: String = ""
    val viewOptionDisabled: MutableMap<String, Boolean> = mutableMapOf()
    val lodMode: IntArray = IntArray(NUM_LOD)
    val modelPhysicsFee: MutableMap<String, Any> = mutableMapOf()

    private var destinationFolderId: UUID? = null
    private var uploadBtn: Any? = null
    private var calculateBtn: Any? = null
    private var uploadLogText: Any? = null
    private var tabContainer: Any? = null
    private var avatarTabIndex: Int = 0
    private var selectedJointName: String = ""
    private val jointOverrides: Array<JointOverrideDataMap> = Array(NUM_LOD) { mutableMapOf() }

    init {
        instance = this
        lodMode[NUM_LOD - 1] = LodMode.LOD_FROM_FILE.ordinal
        for (i in 0 until NUM_LOD - 1) {
            lodMode[i] = LodMode.MESH_OPTIMIZER_AUTO.ordinal
        }
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        initModelPreview()
        initDecompControls()
        TODO("APR: bind all UI child callbacks (cancel, crease_angle, gen_normals, lod_source combos, browse buttons, upload options, preview_lod_combo, import_scale, pelvis_offset, view checkboxes, ok_btn, reset_btn, etc.) via UI framework")
    }

    override fun reshape(width: Int, height: Int, calledFromParent: Boolean) {
        super.reshape(width, height, calledFromParent)
        TODO("APR: refresh model preview when preview_panel rect changes")
    }

    fun initModelPreview() {
        TODO("GPU: allocate LLModelPreview with power-of-2 texture dimensions capped by PreviewRenderSize setting; set camera distance and callbacks")
    }

    fun setUploadDestination(destFolder: UUID?) {
        destinationFolderId = destFolder
    }

    fun isModelLoading(): Boolean {
        TODO("APR: return modelPreview.mLoading")
    }

    fun setDetails(x: Float, y: Float, z: Float) {
        TODO("APR: update dimension fields in UI")
    }

    fun setPreviewLOD(lod: Int) {
        TODO("APR: delegate to modelPreview.setPreviewLOD(lod)")
    }

    fun onBrowseLOD(lod: Int) {
        loadModel(lod)
    }

    fun refresh() {
        TODO("APR: query modelPreview load state and update status text; draw 3-D preview if LODs ready")
    }

    fun loadModel(lod: Int) {
        prepareToLoadModel(lod)
        TODO("APR: open file picker for GLTF/DAE model; on selection call modelPreview.loadModel(filename, lod)")
    }

    fun loadModel(lod: Int, fileName: String, forceDisableSlm: Boolean = false) {
        prepareToLoadModel(lod)
        TODO("APR: call modelPreview.loadModel(fileName, lod, forceDisableSlm)")
    }

    fun loadHighLodModel() {
        TODO("APR: set modelPreview.mLookUpLodFiles = true; loadModel(LOD_HIGH)")
    }

    private fun prepareToLoadModel(lod: Int) {
        TODO("APR: guard against re-entrant loading; set mLoading = true; configure physics search LOD if lod == LOD_PHYSICS")
    }

    fun onViewOptionChecked(ctrlName: String) {
        TODO("APR: toggle modelPreview.mViewOption[ctrlName]; refresh preview")
    }

    fun onUploadOptionChecked(ctrlName: String, value: Boolean) {
        TODO("APR: update upload/view options on modelPreview; refresh, resetPreviewTarget, clearBuffers, toggle calculate button visible")
    }

    fun isViewOptionChecked(optionName: String): Boolean {
        TODO("APR: return modelPreview.mViewOption[optionName]")
    }

    fun isViewOptionEnabled(optionName: String): Boolean {
        TODO("APR: return child view enabled state")
    }

    fun setViewOptionEnabled(option: String, enabled: Boolean) {
        TODO("APR: childSetEnabled(option, enabled)")
    }

    fun enableViewOption(option: String) = setViewOptionEnabled(option, true)

    fun disableViewOption(option: String) = setViewOptionEnabled(option, false)

    fun onShowSkinWeightChecked(ctrlName: String) {
        TODO("APR: clear modelPreview camera offset; call onViewOptionChecked")
    }

    override fun onPermissionsReceived(result: Any?) {
        TODO("APR: show/hide warning title and message based on upload permissions")
    }

    override fun setPermissionsErrorStatus(status: Int, reason: String) {
        TODO("APR: show permission error notification")
    }

    override fun onModelPhysicsFeeReceived(result: Any?, uploadUrl: String) {
        uploadModelUrl = uploadUrl
        handleModelPhysicsFeeReceived()
    }

    fun handleModelPhysicsFeeReceived() {
        TODO("APR: update UI fee display; enable/disable upload button based on fee data")
    }

    override fun setModelPhysicsFeeErrorStatus(status: Int, reason: String, result: Any?) {
        TODO("APR: show fee error notification")
    }

    override fun onModelUploadSuccess() {
        TODO("APR: close floater or show success state")
    }

    override fun onModelUploadFailure() {
        TODO("APR: show failure notification")
    }

    fun isModelUploadAllowed(): Boolean {
        TODO("APR: check agent upload permissions")
    }

    fun clearAvatarTab() {
        TODO("APR: delete all items from joints_list in rigging_panel")
    }

    fun updateAvatarTab(highlightOverrides: Boolean) {
        TODO("APR: populate joints_list and pos_overrides_list from jointOverrides[previewLOD]")
    }

    private fun onDescriptionKeystroke(ctrl: Any?) {
        TODO("APR: if input is dirty, toggle calculate button visible")
    }

    private fun onLoDSourceCommit(lod: Int) {
        TODO("APR: read lod_source combo index, update lodMode[lod], refresh child views for this LOD")
    }

    private fun onLODParamCommit(lod: Int, enforceTriLimit: Boolean) {
        val mode = lodMode[lod]
        when (LodMode.values().getOrNull(mode)) {
            LodMode.MESH_OPTIMIZER_AUTO,
            LodMode.MESH_OPTIMIZER_SLOPPY,
            LodMode.MESH_OPTIMIZER_PRECISE -> TODO("APR: modelPreview.onLODMeshOptimizerParamCommit(lod, enforceTriLimit, mode)")
            LodMode.GENERATE              -> TODO("APR: modelPreview.onLODGLODParamCommit(lod, enforceTriLimit)")
            else -> error("onLODParamCommit called for non-generate mode")
        }
        for (i in lod - 1 downTo 0) {
            if (lodMode[i] == LodMode.USE_LOD_ABOVE.ordinal) {
                onLoDSourceCommit(i)
            } else break
        }
    }

    private fun draw3dPreview() {
        TODO("GPU: bind model preview texture and draw two-triangle quad over preview rect")
    }

    private fun onClickCalculateBtn() {
        clearLogTab()
        addStringToLog("Calculating model data.", false)
        TODO("APR: rebuildUploadData; collect upload options; call gMeshRepo.uploadModel(...); toggleCalculateButton(false)")
    }

    private fun onJointListSelection() {
        TODO("APR: read selected joint from joints_list, populate pos_overrides_list from jointOverrides[previewLOD][label]")
    }

    private fun toggleCalculateButton(visible: Boolean = true) {
        TODO("APR: setVisible/setEnabled on calculate_btn and ok_btn")
    }

    private fun resetDisplayOptions() {
        TODO("APR: restore view option checkboxes to their defaults")
    }

    private fun resetUploadOptions() {
        TODO("APR: uncheck upload_skin, upload_joints, lock_scale_if_joint_position, upload_textures")
    }

    private fun clearLogTab() {
        TODO("APR: delete all items from uploadLogText")
    }

    private fun addStringToLogTab(str: String, flash: Boolean) {
        TODO("APR: append str to uploadLogText; optionally flash the log tab")
    }

    private fun setStatusMessage(msg: String) {
        statusMessage = msg
    }

    private fun setCtrlLoadFromFile(lod: Int) {
        TODO("APR: update lod_source combo to LOD_FROM_FILE and apply LOD UI state")
    }

    private fun modelUpdated(calculateVisible: Boolean) {
        TODO("APR: toggleCalculateButton(calculateVisible)")
    }

    private fun fillLodSourceStatistics(lodSources: MutableMap<String, String>) {
        TODO("APR: for each LOD, record source name string into lodSources map")
    }

    private fun createSmoothComboBox(comboBox: Any?, min: Float, max: Float) {
        val delta = (max - min) / SMOOTH_VALUES_NUMBER
        var value = min
        repeat(SMOOTH_VALUES_NUMBER) {
            TODO("APR: comboBox.add(\"%.2f\".format(value), value); value += delta")
        }
    }

    fun handleMouseDown(x: Int, y: Int): Boolean {
        TODO("APR: if point in previewRect, capture mouse, hide cursor, record lastMouse coords; else delegate to super")
    }

    fun handleMouseUp(x: Int, y: Int): Boolean {
        TODO("APR: release mouse capture, show cursor, delegate to super")
    }

    fun handleHover(x: Int, y: Int): Boolean {
        TODO("APR: if mouse captured and modelPreview exists, pan/orbit/zoom based on key mask delta; update cursor icon; delegate to super otherwise")
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        TODO("APR: if point in previewRect, zoom modelPreview by clicks * -0.2; else delegate to super")
    }

    fun onOpen(key: Any?) {
        TODO("APR: set ModelPreview.sIgnoreLoadedCallback = false; requestAgentUploadPermissions()")
    }

    fun onClose(appQuitting: Boolean) {
        TODO("APR: set ModelPreview.sIgnoreLoadedCallback = true")
    }

    fun initDecompControls() {
        TODO("APR: bind cancel/execute/browse/LOD-use callbacks; iterate LLConvexDecomposition stages and params to populate sliders, spinners, combos with ranges and defaults; snapshot defaultDecompParams")
    }

    // Static-style callbacks mapped from C++ static void on*(ctrl, data) pattern
    fun onPhysicsParamCommit(name: String, value: Any) {
        val adjusted = if (name == "Retain%") {
            (value as? Double)?.div(RETAIN_COEFFICIENT) ?: value
        } else value
        decompParams[name] = adjusted
        if (name == "Simplify Method") {
            val isZero = (value as? Int) == 0
            TODO("APR: show/hide Retain% and Detail Scale children based on isZero")
        }
    }

    fun onPhysicsStageExecute(stageName: String) {
        if (curRequest.isNotEmpty()) return
        TODO("APR: for each physics LOD model, create DecompRequest(stageName, model), add to curRequest, submit to gMeshRepo.mDecompThread")
        TODO("APR: update status label and button visibility based on stageName (Analyze/Decompose/Simplify)")
    }

    fun onPhysicsStageCancel() {
        curRequest.forEach { it.shouldContinue = 0 }
        curRequest.clear()
        TODO("APR: modelPreview.updateStatusMessages()")
    }

    fun onPhysicsUseLOD(whichMode: Int) {
        val numLods = 4
        val fileMode = 5
        val cubeMode = fileMode - 1
        when {
            whichMode < cubeMode -> {
                if (whichMode > numLods) {
                    TODO("APR: modelPreview.setPhysicsFromPreset(whichMode - numLods)")
                } else {
                    val whichLod = numLods - whichMode
                    TODO("APR: modelPreview.setPhysicsFromLOD(whichLod)")
                }
            }
            whichMode == cubeMode -> {
                TODO("APR: loadModel(LOD_PHYSICS, getBoundingBoxCubePath())")
            }
        }
        TODO("APR: modelPreview.refresh(); modelPreview.updateStatusMessages()")
    }

    fun onSuffixStandardSelected(whichIndex: Int) {
        val suffixes = when (whichIndex) {
            1 -> slSuffixes
            2 -> stdSuffixes
            3 -> descSuffixes
            else -> return
        }
        for (i in suffixes.indices) {
            TODO("APR: gSavedSettings.setString(ModelPreview.sSuffixVarNames[i], suffixes[i])")
        }
    }

    fun onReset() {
        TODO("APR: reset model preview to initial state")
    }

    fun onUpload() {
        TODO("APR: trigger model upload flow")
    }

    fun onCancel() {
        TODO("APR: close this floater")
    }

    fun onSelectUDPhysics() {
        TODO("APR: open file picker for COLLADA; on selection save path to FSPhysicsPresetUser1 setting")
    }
}
