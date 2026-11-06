package com.firestorm.newview

import java.util.UUID

// Dirty flags for tracking which material properties have unsaved changes.
object MaterialDirtyFlags {
    val MATERIAL_BASE_COLOR_DIRTY: UInt                    = 0x1u shl 0
    val MATERIAL_BASE_COLOR_TEX_DIRTY: UInt                = 0x1u shl 1
    val MATERIAL_NORMAL_TEX_DIRTY: UInt                    = 0x1u shl 2
    val MATERIAL_METALLIC_ROUGHTNESS_TEX_DIRTY: UInt       = 0x1u shl 3
    val MATERIAL_METALLIC_ROUGHTNESS_METALNESS_DIRTY: UInt = 0x1u shl 4
    val MATERIAL_METALLIC_ROUGHTNESS_ROUGHNESS_DIRTY: UInt = 0x1u shl 5
    val MATERIAL_EMISIVE_COLOR_DIRTY: UInt                 = 0x1u shl 6
    val MATERIAL_EMISIVE_TEX_DIRTY: UInt                   = 0x1u shl 7
    val MATERIAL_DOUBLE_SIDED_DIRTY: UInt                  = 0x1u shl 8
    val MATERIAL_ALPHA_MODE_DIRTY: UInt                    = 0x1u shl 9
    val MATERIAL_ALPHA_CUTOFF_DIRTY: UInt                  = 0x1u shl 10
}

class LLFloaterComboOptions {
    private var mCallback: ((String, Int) -> Unit)? = null
    private var mTitle: String = ""
    private var mDescription: String = ""
    private val mOptions: MutableList<String> = mutableListOf()
    private var mConfirmLabel: String = "OK"
    private var mCancelLabel: String = "Cancel"

    companion object {
        fun showUI(
            callback: (String, Int) -> Unit,
            title: String,
            description: String,
            options: List<String>
        ): LLFloaterComboOptions = LLFloaterComboOptions().also { f ->
            f.mCallback = callback
            f.mTitle = title
            f.mDescription = description
            f.mOptions.addAll(options)
            // no-op: open floater UI with combo options not yet implemented
        }

        fun showUI(
            callback: (String, Int) -> Unit,
            title: String,
            description: String,
            okText: String,
            cancelText: String,
            options: List<String>
        ): LLFloaterComboOptions = showUI(callback, title, description, options).also { f ->
            f.mConfirmLabel = okText
            f.mCancelLabel = cancelText
        }
    }

    fun onConfirm() {
        mCallback?.invoke(mOptions.lastOrNull() ?: "", mOptions.size - 1)
    }

    fun onCancel() {
        mCallback?.invoke("", -1)
    }
}

data class LLColor4(val r: Float = 1f, val g: Float = 1f, val b: Float = 1f, val a: Float = 1f)

class LLMaterialEditor(private val key: Any) {

    private var mAssetID: UUID = UUID(0, 0)
    private var mUploadFolder: UUID = UUID(0, 0)

    private var mBaseColorTextureUploadId: UUID = UUID(0, 0)
    private var mMetallicTextureUploadId: UUID = UUID(0, 0)
    private var mEmissiveTextureUploadId: UUID = UUID(0, 0)
    private var mNormalTextureUploadId: UUID = UUID(0, 0)

    private var mBaseColorName: String = ""
    private var mNormalName: String = ""
    private var mMetallicRoughnessName: String = ""
    private var mEmissiveName: String = ""

    private var mUnsavedChanges: UInt = 0u
    private var mRevertedChanges: UInt = 0u
    private var mUploadingTexturesCount: Int = 0
    private var mExpectedUploadCost: Int = 0
    private var mUploadingTexturesFailure: Boolean = false
    private var mMaterialNameShort: String = ""
    private var mMaterialName: String = ""

    private var mIsOverride: Boolean = false
    private var mHasSelection: Boolean = false

    // Backing stores for UI control values (UI layer is platform-independent here)
    private var mBaseColorTextureId: UUID = UUID(0, 0)
    private var mMetallicRoughnessTextureId: UUID = UUID(0, 0)
    private var mEmissiveTextureId: UUID = UUID(0, 0)
    private var mNormalTextureId: UUID = UUID(0, 0)
    private var mBaseColor: LLColor4 = LLColor4()
    private var mEmissiveColor: LLColor4 = LLColor4(0f, 0f, 0f, 1f)
    private var mTransparency: Float = 1f
    private var mAlphaMode: String = "OPAQUE"
    private var mAlphaCutoff: Float = 0.5f
    private var mMetalnessFactor: Float = 1f
    private var mRoughnessFactor: Float = 1f
    private var mDoubleSided: Boolean = false

    data class LocalTextureConnection(
        val trackingId: UUID,
        val connection: (() -> Unit)?
    )
    private val mTextureChangesUpdates: MutableMap<UInt, LocalTextureConnection> = mutableMapOf()

    companion object {
        private var mOverrideObjectId: UUID = UUID(0, 0)
        private var mOverrideObjectTE: Int = -1
        private var mOverrideInProgress: Boolean = false
        private var mSelectionNeedsUpdate: Boolean = true

        fun importMaterial(destFolder: UUID = UUID(0, 0)) {
            System.err.println("LLMaterialEditor: importMaterial not yet implemented")
        }

        fun updateLive() {
            System.err.println("LLMaterialEditor: updateLive not yet implemented")
        }

        fun loadLive() {
            System.err.println("LLMaterialEditor: loadLive not yet implemented")
        }

        fun canModifyObjectsMaterial(): Boolean {
            return false
        }

        fun canSaveObjectsMaterial(): Boolean {
            return false
        }

        fun canClipboardObjectsMaterial(): Boolean {
            return false
        }

        fun saveObjectsMaterialAs() {
            System.err.println("LLMaterialEditor: saveObjectsMaterialAs not yet implemented")
        }

        fun onSaveObjectsMaterialAsMsgCallback(response: Any, permissions: Any) {
            System.err.println("LLMaterialEditor: onSaveObjectsMaterialAsMsgCallback not yet implemented")
        }

        fun onLoadComplete(assetUuid: UUID, assetType: Int, status: Int) {
            System.err.println("LLMaterialEditor: onLoadComplete not yet implemented")
        }

        fun uploadMaterialFromModel(filename: String, index: Int, destFolderId: UUID = UUID(0, 0)) {
            System.err.println("LLMaterialEditor: uploadMaterialFromModel not yet implemented")
        }

        fun loadMaterialFromFile(filename: String, index: Int = -1, destFolder: UUID = UUID(0, 0)) {
            System.err.println("LLMaterialEditor: loadMaterialFromFile not yet implemented")
        }

        fun finishInventoryUpload(itemId: UUID, newAssetId: UUID, newItemId: UUID) {
            System.err.println("LLMaterialEditor: finishInventoryUpload not yet implemented")
        }

        fun finishTaskUpload(itemId: UUID, newAssetId: UUID, taskId: UUID) {
            System.err.println("LLMaterialEditor: finishTaskUpload not yet implemented")
        }

        fun finishSaveAs(oldKey: Any, newItemId: UUID, buffer: String, hasUnsavedChanges: Boolean) {
            System.err.println("LLMaterialEditor: finishSaveAs not yet implemented")
        }

        fun capabilitiesAvailable(): Boolean {
            return false
        }

        private fun updateInventoryItem(buffer: String, itemId: UUID, taskId: UUID): Boolean {
            return false
        }

        private fun createInventoryItem(
            buffer: String,
            name: String,
            desc: String,
            permissions: Any,
            uploadFolder: UUID
        ) {
            System.err.println("LLMaterialEditor: createInventoryItem not yet implemented")
        }
    }

    fun setFromGltfModel(index: Int, setTextures: Boolean = false): Boolean {
        return false
    }

    fun setFromGltfMetaData(filename: String, index: Int) {
        System.err.println("LLMaterialEditor: setFromGltfMetaData not yet implemented")
    }

    fun applyToSelection() {
        System.err.println("LLMaterialEditor: applyToSelection not yet implemented")
    }

    fun getGLTFMaterial(mat: LLGLTFMaterial) {
        System.err.println("LLMaterialEditor: getGLTFMaterial not yet implemented")
    }

    fun loadAsset() {
        System.err.println("LLMaterialEditor: loadAsset not yet implemented")
    }

    fun onSelectionChanged() {
        mSelectionNeedsUpdate = true
    }

    fun inventoryChanged(serialNum: Int) {
        System.err.println("LLMaterialEditor: inventoryChanged not yet implemented")
    }

    fun saveTexture(name: String, assetId: UUID, cb: (UUID, Any) -> Unit) {
        System.err.println("LLMaterialEditor: saveTexture not yet implemented")
    }

    fun setFailedToUploadTexture() {
        mUploadingTexturesFailure = true
    }

    fun saveTextures(): Int {
        return 0
    }

    fun clearTextures() {
        mBaseColorTextureId = UUID(0, 0)
        mMetallicRoughnessTextureId = UUID(0, 0)
        mEmissiveTextureId = UUID(0, 0)
        mNormalTextureId = UUID(0, 0)
    }

    fun onClickSave() {
        if (!capabilitiesAvailable()) return
        applyToSelection()
        saveIfNeeded()
    }

    fun getEncodedAsset(): String {
        return ""
    }

    fun decodeAsset(buffer: ByteArray): Boolean {
        return false
    }

    fun saveIfNeeded(): Boolean {
        if (mUploadingTexturesCount > 0) return true
        return false
    }

    fun refreshFromInventory(newItemId: UUID = UUID(0, 0)) {
        System.err.println("LLMaterialEditor: refreshFromInventory not yet implemented")
    }

    fun onClickSaveAs() {
        System.err.println("LLMaterialEditor: onClickSaveAs not yet implemented")
    }

    fun onSaveAsMsgCallback(response: Any) {
        System.err.println("LLMaterialEditor: onSaveAsMsgCallback not yet implemented")
    }

    fun onClickCancel() {
        System.err.println("LLMaterialEditor: onClickCancel not yet implemented")
    }

    fun onCancelMsgCallback(response: Any) {
        System.err.println("LLMaterialEditor: onCancelMsgCallback not yet implemented")
    }

    fun setObjectID(objectId: UUID) {
        System.err.println("LLMaterialEditor: setObjectID not yet implemented")
    }

    fun postBuild(): Boolean {
        mIsOverride = false
        return false
    }

    fun onClose(appQuitting: Boolean) {
        for ((_, cn) in mTextureChangesUpdates) cn.connection?.invoke()
        mTextureChangesUpdates.clear()
    }

    fun draw() {
        if (mIsOverride && mSelectionNeedsUpdate) {
            mSelectionNeedsUpdate = false
            clearTextures()
            setFromSelection()
        }
        // no-op: delegate to parent draw() not yet implemented
    }

    fun loadDefaults() {
        System.err.println("LLMaterialEditor: loadDefaults not yet implemented")
    }

    // --- Accessors for individual material properties ---

    fun getBaseColorId(): UUID = mBaseColorTextureId

    fun setBaseColorId(id: UUID) {
        mBaseColorTextureId = id
    }

    fun setBaseColorUploadId(id: UUID) {
        if (id != UUID(0, 0)) mBaseColorTextureUploadId = id
        markChangesUnsaved(MaterialDirtyFlags.MATERIAL_BASE_COLOR_TEX_DIRTY)
    }

    fun getBaseColor(): LLColor4 = mBaseColor.copy(a = mTransparency)

    fun setBaseColor(color: LLColor4) {
        mBaseColor = color.copy(a = 1f)
        setTransparency(color.a)
    }

    fun getTransparency(): Float = mTransparency

    fun setTransparency(transparency: Float) {
        mTransparency = transparency
    }

    fun getAlphaMode(): String = mAlphaMode

    fun setAlphaMode(alphaMode: String) {
        mAlphaMode = alphaMode
    }

    fun getAlphaCutoff(): Float = mAlphaCutoff

    fun setAlphaCutoff(alphaCutoff: Float) {
        mAlphaCutoff = alphaCutoff
    }

    fun setMaterialName(name: String) {
        mMaterialName = name
    }

    fun getMetallicRoughnessId(): UUID = mMetallicRoughnessTextureId

    fun setMetallicRoughnessId(id: UUID) {
        mMetallicRoughnessTextureId = id
    }

    fun setMetallicRoughnessUploadId(id: UUID) {
        if (id != UUID(0, 0)) mMetallicTextureUploadId = id
        markChangesUnsaved(MaterialDirtyFlags.MATERIAL_METALLIC_ROUGHTNESS_TEX_DIRTY)
    }

    fun getMetalnessFactor(): Float = mMetalnessFactor

    fun setMetalnessFactor(factor: Float) {
        mMetalnessFactor = factor
    }

    fun getRoughnessFactor(): Float = mRoughnessFactor

    fun setRoughnessFactor(factor: Float) {
        mRoughnessFactor = factor
    }

    fun getEmissiveId(): UUID = mEmissiveTextureId

    fun setEmissiveId(id: UUID) {
        mEmissiveTextureId = id
    }

    fun setEmissiveUploadId(id: UUID) {
        if (id != UUID(0, 0)) mEmissiveTextureUploadId = id
        markChangesUnsaved(MaterialDirtyFlags.MATERIAL_EMISIVE_TEX_DIRTY)
    }

    fun getEmissiveColor(): LLColor4 = mEmissiveColor

    fun setEmissiveColor(color: LLColor4) {
        mEmissiveColor = color
    }

    fun getNormalId(): UUID = mNormalTextureId

    fun setNormalId(id: UUID) {
        mNormalTextureId = id
    }

    fun setNormalUploadId(id: UUID) {
        if (id != UUID(0, 0)) mNormalTextureUploadId = id
        markChangesUnsaved(MaterialDirtyFlags.MATERIAL_NORMAL_TEX_DIRTY)
    }

    fun getDoubleSided(): Boolean = mDoubleSided

    fun setDoubleSided(doubleSided: Boolean) {
        mDoubleSided = doubleSided
    }

    fun setCanSaveAs(value: Boolean) {
        System.err.println("LLMaterialEditor: setCanSaveAs not yet implemented")
    }

    fun setCanSave(value: Boolean) {
        System.err.println("LLMaterialEditor: setCanSave not yet implemented")
    }

    fun setEnableEditing(canModify: Boolean) {
        System.err.println("LLMaterialEditor: setEnableEditing not yet implemented")
    }

    fun subscribeToLocalTexture(dirtyFlag: UInt, trackingId: UUID) {
        if (mTextureChangesUpdates[dirtyFlag]?.trackingId != trackingId) {
            mTextureChangesUpdates[dirtyFlag]?.connection?.invoke()
            val connection: (() -> Unit)? = null
            mTextureChangesUpdates[dirtyFlag] = LocalTextureConnection(trackingId, connection)
        }
    }

    fun replaceLocalTexture(oldId: UUID, newId: UUID) {
        if (mBaseColorTextureId == oldId) mBaseColorTextureId = newId
        if (mMetallicRoughnessTextureId == oldId) mMetallicRoughnessTextureId = newId
        if (mEmissiveTextureId == oldId) mEmissiveTextureId = newId
        if (mNormalTextureId == oldId) mNormalTextureId = newId
    }

    fun onCommitTexture(dirtyFlag: UInt, newTextureId: UUID, isImageLocal: Boolean, localTrackingId: UUID) {
        if (isImageLocal) {
            subscribeToLocalTexture(dirtyFlag, localTrackingId)
        } else {
            mTextureChangesUpdates[dirtyFlag]?.connection?.invoke()
        }
        markChangesUnsaved(dirtyFlag)
        applyToSelection()
    }

    fun onCancelCtrl(dirtyFlag: UInt) {
        mRevertedChanges = mRevertedChanges or dirtyFlag
        applyToSelection()
    }

    fun onSelectCtrl(dirtyFlag: UInt) {
        mUnsavedChanges = mUnsavedChanges or dirtyFlag
        applyToSelection()
        System.err.println("LLMaterialEditor: onSelectCtrl not yet implemented")
    }

    fun getUnsavedChangesFlags(): UInt = mUnsavedChanges
    fun getRevertedChangesFlags(): UInt = mRevertedChanges

    fun getLocalTextureTrackingIdFromFlag(flag: UInt): UUID =
        mTextureChangesUpdates[flag]?.trackingId ?: UUID(0, 0)

    fun updateMaterialLocalSubscription(mat: LLGLTFMaterial?): Boolean {
        if (mat == null) return false
        var res = false
        for ((_, cn) in mTextureChangesUpdates) {
            val worldId: UUID = UUID(0, 0)
            if (
                worldId == mat.mTextureId[LLGLTFMaterial.TextureInfo.GLTF_TEXTURE_INFO_BASE_COLOR] ||
                worldId == mat.mTextureId[LLGLTFMaterial.TextureInfo.GLTF_TEXTURE_INFO_METALLIC_ROUGHNESS] ||
                worldId == mat.mTextureId[LLGLTFMaterial.TextureInfo.GLTF_TEXTURE_INFO_EMISSIVE] ||
                worldId == mat.mTextureId[LLGLTFMaterial.TextureInfo.GLTF_TEXTURE_INFO_NORMAL]
            ) {
                // no-op: LLLocalBitmapMgr.associateGLTFMaterial not yet implemented
                res = true
            }
        }
        return res
    }

    // --- Private helpers ---

    private fun setFromGLTFMaterial(mat: LLGLTFMaterial) {
        System.err.println("LLMaterialEditor: setFromGLTFMaterial not yet implemented")
    }

    private fun setFromSelection(): Boolean {
        return false
    }

    private fun resetUnsavedChanges() {
        mUnsavedChanges = 0u
        mRevertedChanges = 0u
        mExpectedUploadCost = 0
    }

    private fun markChangesUnsaved(dirtyFlag: UInt) {
        mUnsavedChanges = mUnsavedChanges or dirtyFlag
        refreshUploadCost()
    }

    private fun refreshUploadCost() {
        mExpectedUploadCost = 0
        System.err.println("LLMaterialEditor: refreshUploadCost not yet implemented")
    }

    private fun buildMaterialDescription(): String {
        val parts = mutableListOf<String>()
        if (mBaseColorTextureId != UUID(0, 0)) parts.add(mBaseColorName)
        if (mMetallicRoughnessTextureId != UUID(0, 0)) parts.add(mMetallicRoughnessName)
        if (mEmissiveTextureId != UUID(0, 0)) parts.add(mEmissiveName)
        if (mNormalTextureId != UUID(0, 0)) parts.add(mNormalName)
        return parts.joinToString(", ")
    }

    private fun getImageNameFromUri(imageUri: String, textureType: String): String {
        return imageUri.substringAfterLast('/').substringBeforeLast('.').ifBlank { textureType }
    }
}
