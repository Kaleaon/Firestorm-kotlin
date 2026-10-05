package com.firestorm.newview

import java.util.UUID

private const val LOCAL_TRACKING_ID_COLUMN = 4

class VJFloaterLocalMesh {

    private var mObjectCreatedCallback: (() -> Unit)? = null
    private var mLastSelectedObject: UUID? = null

    fun onOpen() {
        reloadFileList(false)
        LLLocalMeshSystem.registerFloaterPointer(this)
        toggleSelectTool(true)
    }

    fun onClose(appQuitting: Boolean) {
        LLLocalMeshSystem.registerFloaterPointer(null)
        toggleSelectTool(false)
    }

    fun onSelectionChangedCallback() {
        reloadLowerUI()
        showLog()
    }

    fun postBuild(): Boolean {
        System.err.println("VJFloaterLocalMesh: bind btn_add/reload/remove/apply/clear/rez buttons, init mTabContainer, mLogPanel, mScrollCtrl, lod_suffix_combo not yet implemented")
        return false
    }

    fun draw() {
        System.err.println("VJFloaterLocalMesh: read FSLocalMeshAutoReload setting not yet implemented")
        val autoReloadEnabled: Boolean = false
        System.err.println("VJFloaterLocalMesh: set auto_reload_period spinner enabled state = autoReloadEnabled not yet implemented")

        System.err.println("VJFloaterLocalMesh: LLSelectMgr.getSelection().getFirstObject()?.getID() not yet implemented")
        val currentObjectId: UUID? = null
        updateSelectedTarget(currentObjectId)

        System.err.println("VJFloaterLocalMesh: call super draw() not yet implemented")
    }

    fun onBtnAdd() {
        System.err.println("VJFloaterLocalMesh: open a file picker dialog for .dae/.gltf/.glb files, call onBtnAddCallback with chosen filename not yet implemented")
    }

    fun onBtnAddCallback(filename: String) {
        LLLocalMeshSystem.addFile(filename, tryLods = true)
        showLog()
    }

    fun onBtnReload() {
        val selectedId = getSelectedScrollItemId() ?: return
        LLLocalMeshSystem.reloadFile(selectedId)
    }

    fun onBtnRemove() {
        val selectedId = getSelectedScrollItemId() ?: return
        LLLocalMeshSystem.deleteFile(selectedId)
        reloadLowerUI()
    }

    fun onBtnApply() {
        val fileId = getSelectedScrollItemId() ?: return
        System.err.println("VJFloaterLocalMesh: get first selected index from object_apply_list combo box not yet implemented")
        val objectComboIndex: Int = 0
        val selectedObjectId = getCurrentSelectionIfValid() ?: return
        LLLocalMeshSystem.applyVObject(selectedObjectId, fileId, objectComboIndex, false)
    }

    fun onBtnClear() {
        val selectedObjectId = getCurrentSelectionIfValid() ?: return
        LLLocalMeshSystem.clearVObject(selectedObjectId)
    }

    fun onBtnRez() {
        System.err.println("VJFloaterLocalMesh: register gObjectList.setNewObjectCallback -> processPrimCreated, switch tool to LLToolCompCreate not yet implemented")
        mObjectCreatedCallback = null
    }

    fun onSuffixStandardSelected(which: Int) {
        val slSuffixes = arrayOf("LOD0", "LOD1", "LOD2", "", "PHYS")
        val stdSuffixes = arrayOf("LOD3", "LOD2", "LOD1", "LOD0", "PHYS")
        val descSuffixes = arrayOf("LOWEST", "LOW", "MED", "HIGH", "PHYS")

        System.err.println("VJFloaterLocalMesh: write FSMeshLodSuffixScheme = which to settings not yet implemented")

        val suffixes: Array<String>? = when (which) {
            1 -> slSuffixes
            2 -> stdSuffixes
            3 -> descSuffixes
            else -> null
        }

        if (suffixes != null) {
            System.err.println("VJFloaterLocalMesh: iterate LLModel.NUM_LODS and write each suffix to LLModelPreview.sSuffixVarNames[i] setting not yet implemented")
        }
    }

    fun processPrimCreated(objectId: UUID): Boolean {
        System.err.println("VJFloaterLocalMesh: select new object, set sculpt type MESH with null local_id, optionally apply selected local mesh from scroll list not yet implemented")
        return false
    }

    fun reloadFileList(keepSelection: Boolean) {
        val fileInfoVec = LLLocalMeshSystem.getFileInfoVector()
        System.err.println("VJFloaterLocalMesh: mScrollCtrl.getFirstSelectedIndex() not yet implemented")
        val selectedNum: Int = 0

        System.err.println("VJFloaterLocalMesh: mScrollCtrl.clearRows() not yet implemented")

        for (info in fileInfoVec) {
            val statusText = when (info.status) {
                LLLocalMeshFile.LLLocalMeshFileStatus.STATUS_NONE -> "None"
                LLLocalMeshFile.LLLocalMeshFileStatus.STATUS_LOADING -> "Loading"
                LLLocalMeshFile.LLLocalMeshFileStatus.STATUS_ACTIVE -> "Active"
                LLLocalMeshFile.LLLocalMeshFileStatus.STATUS_ERROR -> "Error"
            }

            val lodState = buildString {
                for (lodReverseIter in 3 downTo 0) {
                    append("[")
                    if (info.lodAvailability[lodReverseIter]) append(lodReverseIter)
                    append("]")
                    if (lodReverseIter > 0) append(" ")
                }
            }

            val objectCount = info.objectList.size.toString()

            System.err.println(
                "VJFloaterLocalMesh: add a row to mScrollCtrl with columns: " +
                "unit_status=$statusText, unit_name=${info.name}, unit_lods=$lodState, " +
                "unit_objects=$objectCount, unit_id_HIDDEN=${info.localId} not yet implemented"
            )
        }

        if (keepSelection && selectedNum >= 0) {
            System.err.println("VJFloaterLocalMesh: mScrollCtrl.selectNthItem(selectedNum); reloadLowerUI() not yet implemented")
        } else if (fileInfoVec.isNotEmpty()) {
            System.err.println("VJFloaterLocalMesh: mScrollCtrl.selectNthItem(last); reloadLowerUI() not yet implemented")
        }
    }

    fun onFileListCommitCallback() {
        reloadLowerUI()
        showLog()
    }

    fun reloadLowerUI() {
        val selectedTargetValid = getCurrentSelectionIfValid() != null

        var selectedFileLoaded = false
        var selectedFileActive = false
        val selectedFileId: UUID? = getSelectedScrollItemId()
        var selectedObjectList = emptyList<String>()

        if (selectedFileId != null) {
            for (info in LLLocalMeshSystem.getFileInfoVector()) {
                if (info.localId == selectedFileId) {
                    when (info.status) {
                        LLLocalMeshFile.LLLocalMeshFileStatus.STATUS_ACTIVE -> {
                            selectedFileLoaded = true
                            selectedFileActive = true
                            selectedObjectList = info.objectList
                        }
                        LLLocalMeshFile.LLLocalMeshFileStatus.STATUS_ERROR -> {
                            selectedFileLoaded = true
                        }
                        else -> {}
                    }
                    break
                }
            }
        }

        System.err.println("VJFloaterLocalMesh: reloadLowerUI set button enabled states and populate object_apply_list not yet implemented")
    }

    fun toggleSelectTool(toggle: Boolean) {
        if (toggle) {
            System.err.println("VJFloaterLocalMesh: toggleSelectTool enable LLSelectMgr.setForceSelection and inspect tool not yet implemented")
        } else {
            System.err.println("VJFloaterLocalMesh: toggleSelectTool restore basic toolset not yet implemented")
        }
    }

    fun getCurrentSelectionIfValid(): UUID? {
        val lastSelected = mLastSelectedObject ?: return null
        val obj: Any? = null
        val isMesh: Boolean = false
        return if (isMesh) lastSelected else null
    }

    private fun updateSelectedTarget(selectedId: UUID?) {
        if (selectedId != mLastSelectedObject) {
            mLastSelectedObject = selectedId
            onSelectionChangedCallback()
        }
    }

    private fun showLog() {
        System.err.println("VJFloaterLocalMesh: showLog mLogPanel.clear() not yet implemented")
        val fileId = getSelectedScrollItemId() ?: return
        val log = LLLocalMeshSystem.getFileLog(fileId)
        for (line in log) {
            System.err.println("VJFloaterLocalMesh: showLog mLogPanel.appendText not yet implemented")
        }
    }

    private fun getSelectedScrollItemId(): UUID? {
        System.err.println("VJFloaterLocalMesh: getSelectedScrollItemId mScrollCtrl selected row not yet implemented")
        return null
    }
}
