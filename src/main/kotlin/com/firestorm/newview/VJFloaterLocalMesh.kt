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
        TODO("APR: use JVM equivalent — bind btn_add/reload/remove/apply/clear/rez buttons, init mTabContainer, mLogPanel, mScrollCtrl, lod_suffix_combo")
    }

    fun draw() {
        val autoReloadEnabled: Boolean = TODO("APR: read FSLocalMeshAutoReload setting")
        TODO("APR: use JVM equivalent — set auto_reload_period spinner enabled state = autoReloadEnabled")

        val currentObjectId: UUID? = TODO("APR: use JVM equivalent — LLSelectMgr.getSelection().getFirstObject()?.getID()")
        updateSelectedTarget(currentObjectId)

        TODO("APR: use JVM equivalent — call super draw()")
    }

    fun onBtnAdd() {
        TODO("APR: use JVM equivalent — open a file picker dialog for .dae/.gltf/.glb files, call onBtnAddCallback with chosen filename")
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
        val objectComboIndex: Int = TODO("APR: use JVM equivalent — get first selected index from object_apply_list combo box")
        val selectedObjectId = getCurrentSelectionIfValid() ?: return
        LLLocalMeshSystem.applyVObject(selectedObjectId, fileId, objectComboIndex, false)
    }

    fun onBtnClear() {
        val selectedObjectId = getCurrentSelectionIfValid() ?: return
        LLLocalMeshSystem.clearVObject(selectedObjectId)
    }

    fun onBtnRez() {
        mObjectCreatedCallback = TODO("APR: use JVM equivalent — register gObjectList.setNewObjectCallback -> processPrimCreated, switch tool to LLToolCompCreate")
    }

    fun onSuffixStandardSelected(which: Int) {
        val slSuffixes = arrayOf("LOD0", "LOD1", "LOD2", "", "PHYS")
        val stdSuffixes = arrayOf("LOD3", "LOD2", "LOD1", "LOD0", "PHYS")
        val descSuffixes = arrayOf("LOWEST", "LOW", "MED", "HIGH", "PHYS")

        TODO("APR: write FSMeshLodSuffixScheme = which to settings")

        val suffixes: Array<String>? = when (which) {
            1 -> slSuffixes
            2 -> stdSuffixes
            3 -> descSuffixes
            else -> null
        }

        if (suffixes != null) {
            TODO("APR: use JVM equivalent — iterate LLModel.NUM_LODS and write each suffix to LLModelPreview.sSuffixVarNames[i] setting")
        }
    }

    fun processPrimCreated(objectId: UUID): Boolean {
        TODO("APR: use JVM equivalent — select new object, set sculpt type MESH with null local_id, optionally apply selected local mesh from scroll list")
    }

    fun reloadFileList(keepSelection: Boolean) {
        val fileInfoVec = LLLocalMeshSystem.getFileInfoVector()
        val selectedNum: Int = TODO("APR: use JVM equivalent — mScrollCtrl.getFirstSelectedIndex()")

        TODO("APR: use JVM equivalent — mScrollCtrl.clearRows()")

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

            TODO(
                "APR: use JVM equivalent — add a row to mScrollCtrl with columns: " +
                "unit_status=$statusText, unit_name=${info.name}, unit_lods=$lodState, " +
                "unit_objects=$objectCount, unit_id_HIDDEN=${info.localId}"
            )
        }

        if (keepSelection && selectedNum >= 0) {
            TODO("APR: use JVM equivalent — mScrollCtrl.selectNthItem(selectedNum); reloadLowerUI()")
        } else if (fileInfoVec.isNotEmpty()) {
            TODO("APR: use JVM equivalent — mScrollCtrl.selectNthItem(last); reloadLowerUI()")
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

        TODO(
            "APR: use JVM equivalent — set btn_clear.enabled=$selectedTargetValid, " +
            "btn_remove.enabled=$selectedFileLoaded, btn_reload.enabled=$selectedFileLoaded, " +
            "btn_apply.enabled=(selectedTargetValid && selectedFileActive), " +
            "object_apply_list.enabled=$selectedFileActive, " +
            "populate object_apply_list with $selectedObjectList if active"
        )
    }

    fun toggleSelectTool(toggle: Boolean) {
        if (toggle) {
            TODO("APR: use JVM equivalent — LLSelectMgr.setForceSelection(true), set transient inspect tool, update mObjectSelection")
        } else {
            TODO("APR: use JVM equivalent — clear transient tool if current is inspect, restore basic toolset")
        }
    }

    fun getCurrentSelectionIfValid(): UUID? {
        val lastSelected = mLastSelectedObject ?: return null
        val obj: Any? = TODO("APR: use JVM equivalent — gObjectList.findObject(lastSelected)")
        val isMesh: Boolean = TODO("APR: use JVM equivalent — obj.isMesh()")
        return if (isMesh) lastSelected else null
    }

    private fun updateSelectedTarget(selectedId: UUID?) {
        if (selectedId != mLastSelectedObject) {
            mLastSelectedObject = selectedId
            onSelectionChangedCallback()
        }
    }

    private fun showLog() {
        TODO("APR: use JVM equivalent — mLogPanel.clear()")
        val fileId = getSelectedScrollItemId() ?: return
        val log = LLLocalMeshSystem.getFileLog(fileId)
        for (line in log) {
            TODO("APR: use JVM equivalent — mLogPanel.appendText(line)")
        }
    }

    private fun getSelectedScrollItemId(): UUID? {
        TODO("APR: use JVM equivalent — get first selected row from mScrollCtrl, read column $LOCAL_TRACKING_ID_COLUMN as UUID")
    }
}
