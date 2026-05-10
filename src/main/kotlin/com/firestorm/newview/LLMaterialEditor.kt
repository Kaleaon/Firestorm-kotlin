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
            TODO("APR: use JVM equivalent — open floater UI with combo options")
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
            TODO("APR: use JVM equivalent — open file dialog to select .gltf/.glb for import")
        }

        fun updateLive() {
            TODO("APR: use JVM equivalent — update live material override on selected object")
        }

        fun loadLive() {
            TODO("APR: use JVM equivalent — load current inworld selection into material editor")
        }

        fun canModifyObjectsMaterial(): Boolean {
            TODO("APR: use JVM equivalent — check selection modify permissions")
        }

        fun canSaveObjectsMaterial(): Boolean {
            TODO("APR: use JVM equivalent — check selection save permissions")
        }

        fun canClipboardObjectsMaterial(): Boolean {
            TODO("APR: use JVM equivalent — check clipboard permissions for material")
        }

        fun saveObjectsMaterialAs() {
            TODO("APR: use JVM equivalent — prompt user to save selected object material as new asset")
        }

        fun onSaveObjectsMaterialAsMsgCallback(response: Any, permissions: Any) {
            TODO("APR: use JVM equivalent — handle save-as dialog result")
        }

        fun onLoadComplete(assetUuid: UUID, assetType: Int, status: Int) {
            TODO("APR: use JVM equivalent — handle asset load completion callback")
        }

        fun uploadMaterialFromModel(filename: String, index: Int, destFolderId: UUID = UUID(0, 0)) {
            TODO("APR: use JVM equivalent — upload material from parsed GLTF model at index $index")
        }

        fun loadMaterialFromFile(filename: String, index: Int = -1, destFolder: UUID = UUID(0, 0)) {
            TODO("APR: use JVM equivalent — load material from file and open editor")
        }

        fun finishInventoryUpload(itemId: UUID, newAssetId: UUID, newItemId: UUID) {
            TODO("APR: use JVM equivalent — finalise inventory upload and refresh editor")
        }

        fun finishTaskUpload(itemId: UUID, newAssetId: UUID, taskId: UUID) {
            TODO("APR: use JVM equivalent — finalise task (in-world object) upload")
        }

        fun finishSaveAs(oldKey: Any, newItemId: UUID, buffer: String, hasUnsavedChanges: Boolean) {
            TODO("APR: use JVM equivalent — complete save-as workflow, refresh inventory")
        }

        fun capabilitiesAvailable(): Boolean {
            TODO("APR: use JVM equivalent — check if region has RenderMaterials capability")
        }

        private fun updateInventoryItem(buffer: String, itemId: UUID, taskId: UUID): Boolean {
            TODO("APR: use JVM equivalent — HTTP PUT material buffer to inventory item")
        }

        private fun createInventoryItem(
            buffer: String,
            name: String,
            desc: String,
            permissions: Any,
            uploadFolder: UUID
        ) {
            TODO("APR: use JVM equivalent — create new inventory item with material asset")
        }
    }

    fun setFromGltfModel(index: Int, setTextures: Boolean = false): Boolean {
        TODO("APR: use JVM equivalent — parse tinygltf model and populate editor fields")
    }

    fun setFromGltfMetaData(filename: String, index: Int) {
        TODO("APR: use JVM equivalent — populate editor name/description from GLTF metadata")
    }

    fun applyToSelection() {
        TODO("APR: use JVM equivalent — apply current editor state as material override to selection")
    }

    fun getGLTFMaterial(mat: LLGLTFMaterial) {
        TODO("APR: use JVM equivalent — write editor UI values into LLGLTFMaterial")
    }

    fun loadAsset() {
        TODO("APR: use JVM equivalent — request asset from asset service by mAssetID")
    }

    fun onSelectionChanged() {
        mSelectionNeedsUpdate = true
    }

    fun inventoryChanged(serialNum: Int) {
        TODO("APR: use JVM equivalent — handle inventory change notification")
    }

    fun saveTexture(name: String, assetId: UUID, cb: (UUID, Any) -> Unit) {
        TODO("APR: use JVM equivalent — encode J2C texture and upload to asset service")
    }

    fun setFailedToUploadTexture() {
        mUploadingTexturesFailure = true
    }

    fun saveTextures(): Int {
        TODO("APR: use JVM equivalent — schedule uploads for any textures pending upload, return count")
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
        TODO("APR: use JVM equivalent — serialise GLTF material to LLSD binary")
    }

    fun decodeAsset(buffer: ByteArray): Boolean {
        TODO("APR: use JVM equivalent — deserialise LLSD binary and populate editor from GLTF model")
    }

    fun saveIfNeeded(): Boolean {
        if (mUploadingTexturesCount > 0) return true
        TODO("APR: use JVM equivalent — upload textures then PUT material to inventory/task")
    }

    fun refreshFromInventory(newItemId: UUID = UUID(0, 0)) {
        TODO("APR: use JVM equivalent — re-fetch inventory item and reload asset into editor")
    }

    fun onClickSaveAs() {
        TODO("APR: use JVM equivalent — prompt for new name and create new inventory item")
    }

    fun onSaveAsMsgCallback(response: Any) {
        TODO("APR: use JVM equivalent — handle save-as dialog confirmation")
    }

    fun onClickCancel() {
        TODO("APR: use JVM equivalent — revert editor changes and close if appropriate")
    }

    fun onCancelMsgCallback(response: Any) {
        TODO("APR: use JVM equivalent — handle cancel confirmation dialog result")
    }

    fun setObjectID(objectId: UUID) {
        TODO("APR: use JVM equivalent — associate editor with in-world object and refresh asset ID")
    }

    fun postBuild(): Boolean {
        mIsOverride = TODO("APR: use JVM equivalent — detect if this is the single live-override instance")
        TODO("APR: use JVM equivalent — wire up UI controls and callbacks")
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
        TODO("APR: use JVM equivalent — delegate to parent draw()")
    }

    fun loadDefaults() {
        TODO("APR: use JVM equivalent — reset all editor fields to default GLTF material values")
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
        TODO("APR: use JVM equivalent — enable/disable save-as button in UI")
    }

    fun setCanSave(value: Boolean) {
        TODO("APR: use JVM equivalent — enable/disable save button in UI")
    }

    fun setEnableEditing(canModify: Boolean) {
        TODO("APR: use JVM equivalent — enable/disable all editor input controls")
    }

    fun subscribeToLocalTexture(dirtyFlag: UInt, trackingId: UUID) {
        if (mTextureChangesUpdates[dirtyFlag]?.trackingId != trackingId) {
            mTextureChangesUpdates[dirtyFlag]?.connection?.invoke()
            val connection: (() -> Unit)? = TODO("APR: use JVM equivalent — subscribe to LLLocalBitmapMgr change callback for trackingId")
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
        TODO("APR: use JVM equivalent — iterate selected nodes and update saved override material for dirtyFlag")
    }

    fun getUnsavedChangesFlags(): UInt = mUnsavedChanges
    fun getRevertedChangesFlags(): UInt = mRevertedChanges

    fun getLocalTextureTrackingIdFromFlag(flag: UInt): UUID =
        mTextureChangesUpdates[flag]?.trackingId ?: UUID(0, 0)

    fun updateMaterialLocalSubscription(mat: LLGLTFMaterial?): Boolean {
        if (mat == null) return false
        var res = false
        for ((_, cn) in mTextureChangesUpdates) {
            val worldId: UUID = TODO("APR: use JVM equivalent — LLLocalBitmapMgr.getWorldID(cn.trackingId)")
            if (
                worldId == mat.mTextureId[LLGLTFMaterial.TextureInfo.GLTF_TEXTURE_INFO_BASE_COLOR] ||
                worldId == mat.mTextureId[LLGLTFMaterial.TextureInfo.GLTF_TEXTURE_INFO_METALLIC_ROUGHNESS] ||
                worldId == mat.mTextureId[LLGLTFMaterial.TextureInfo.GLTF_TEXTURE_INFO_EMISSIVE] ||
                worldId == mat.mTextureId[LLGLTFMaterial.TextureInfo.GLTF_TEXTURE_INFO_NORMAL]
            ) {
                TODO("APR: use JVM equivalent — LLLocalBitmapMgr.associateGLTFMaterial(cn.trackingId, mat)")
                res = true
            }
        }
        return res
    }

    // --- Private helpers ---

    private fun setFromGLTFMaterial(mat: LLGLTFMaterial) {
        TODO("APR: use JVM equivalent — copy all LLGLTFMaterial fields into editor UI state")
    }

    private fun setFromSelection(): Boolean {
        TODO("APR: use JVM equivalent — read current selection's render material into editor")
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
        TODO("APR: use JVM equivalent — sum upload fees for each texture pending upload")
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
