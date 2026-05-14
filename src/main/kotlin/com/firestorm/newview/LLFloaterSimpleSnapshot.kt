package com.firestorm.newview

import com.firestorm.ui.LLFloater
import com.firestorm.ui.LLFloaterView
import com.firestorm.ui.LLUICtrl
import com.firestorm.ui.LLView
import com.firestorm.ui.LLRect
import com.firestorm.math.LLColor4
import com.firestorm.llsd.LLSD
import com.firestorm.snapshot.LLFloaterSnapshotBase
import com.firestorm.snapshot.LLSnapshotLivePreview
import com.firestorm.snapshot.LLSnapshotModel
import com.firestorm.floater.LLFloaterReg
import java.util.UUID

var gSimpleSnapshotFloaterView: LLSimpleSnapshotFloaterView? = null

private const val THUMBNAIL_UPLOAD_CAP = "InventoryThumbnailUpload"
private const val PREVIEW_OFFSET_Y = 70

class LLFloaterSimpleSnapshot(key: LLSD) : LLFloaterSnapshotBase(key) {

    var mInventoryId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    var mTaskId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    var mOwner: LLView? = null
    private var mContextConeOpacity: Float = 0.0f
    private var mUploadCompletionCallback: ((UUID) -> Unit)? = null

    init {
        impl = Impl(this)
    }

    override fun postBuild(): Boolean {
        childSetAction("new_snapshot_btn", ImplBase::onClickNewSnapshot, this)
        childSetAction("save_btn") { onSend() }
        childSetAction("cancel_btn") { onCancel() }

        mThumbnailPlaceholder = getChild("thumbnail_placeholder")

        val fullScreenRect: LLRect = getRootView().getRect()
        val previewp = LLSnapshotLivePreview(fullScreenRect)

        impl.mPreviewHandle = previewp.getHandle()
        previewp.setContainer(this)
        impl.updateControls(this)
        impl.setAdvanced(true)
        impl.setSkipReshaping(true)

        previewp.mKeepAspectRatio = false
        previewp.setThumbnailPlaceholderRect(getThumbnailPlaceholderRect())
        previewp.setAllowRenderUI(false)
        previewp.setThumbnailSubsampled(true)

        return true
    }

    fun getThumbnailPlaceholderRect(): LLRect = mThumbnailPlaceholder!!.getRect()

    fun setInventoryId(inventoryId: UUID) { mInventoryId = inventoryId }
    fun getInventoryId(): UUID = mInventoryId
    fun setTaskId(taskId: UUID) { mTaskId = taskId }
    fun setOwner(ownerView: LLView) { mOwner = ownerView }
    fun setCompletionCallback(callback: (UUID) -> Unit) { mUploadCompletionCallback = callback }

    override fun draw() {
        val owner = mOwner
        if (owner != null) {
            val maxOpacity: Float = gSavedSettings.getFloat("PickerContextOpacity", 0.4f)
            drawConeToOwner(mContextConeOpacity, maxOpacity, owner)
        }

        val previewp: LLSnapshotLivePreview? = getPreviewView()

        if (previewp != null && (previewp.isSnapshotActive() || previewp.getThumbnailLock())) {
            return
        }

        super.draw()

        if (previewp != null && !isMinimized() && mThumbnailPlaceholder?.getVisible() == true) {
            val thumbImage = previewp.getThumbnailImage()
            if (thumbImage != null) {
                val working = impl.getStatus() == ImplBase.STATUS_WORKING
                val thumbnailW = previewp.getThumbnailWidth()
                val thumbnailH = previewp.getThumbnailHeight()

                val localRect: LLRect = getLocalRect()
                val offsetX = (localRect.getWidth() - thumbnailW) / 2
                val offsetY = PREVIEW_OFFSET_Y

                // no-op
            }
        }
        impl.updateLayout(this)
    }

    fun onOpen(key: LLSD) {
        val preview: LLSnapshotLivePreview? = getPreviewView()
        preview?.updateSnapshot(true)
        focusFirstItem(false)
        gSnapshotFloaterView?.setEnabled(true)
        gSnapshotFloaterView?.setVisible(true)
        gSnapshotFloaterView?.adjustToFitScreen(this, false)

        impl.updateControls(this)
        impl.setStatus(ImplBase.STATUS_READY)

        mInventoryId = key["item_id"].asUUID()
        mTaskId = key["task_id"].asUUID()
    }

    fun postSave() {
        impl.setStatus(ImplBase.STATUS_WORKING)
    }

    fun saveTexture() {
        val previewp: LLSnapshotLivePreview = getPreviewView() ?: return
        previewp.saveTexture(true, getInventoryId().toString())
        closeFloater()
    }

    private fun onCancel() {
        closeFloater()
    }

    private fun onSend() {
        val previewp: LLSnapshotLivePreview = getPreviewView() ?: return
        val tempFile: String = gDirUtilp.getTempFilename()
        if (previewp.createUploadFile(tempFile, THUMBNAIL_SNAPSHOT_DIM_MAX, THUMBNAIL_SNAPSHOT_DIM_MIN)) {
            uploadImageUploadFile(tempFile, mInventoryId, mTaskId, mUploadCompletionCallback)
            closeFloater()
        } else {
            val reason = LLImage.getLastThreadError()
            LLNotificationsUtil.add("CannotUploadTexture", mapOf("REASON" to reason))
            mUploadCompletionCallback?.invoke(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        }
    }

    class Impl(floater: LLFloaterSnapshotBase) : LLFloaterSnapshotBase.ImplBase(floater) {

        fun getImageFormat(floater: LLFloaterSnapshotBase): LLSnapshotModel.ESnapshotFormat =
            LLSnapshotModel.ESnapshotFormat.SNAPSHOT_FORMAT_PNG

        fun getLayerType(floater: LLFloaterSnapshotBase): LLSnapshotModel.ESnapshotLayerType =
            LLSnapshotModel.ESnapshotLayerType.SNAPSHOT_TYPE_COLOR

        override fun getSnapshotPanelPrefix(): String = "panel_outfit_snapshot_"

        override fun getActivePanel(floater: LLFloaterSnapshotBase, okIfNotFound: Boolean) = null

        override fun updateControls(floater: LLFloaterSnapshotBase) {
            val previewp: LLSnapshotLivePreview? = getPreviewView()
            updateResolution(floater)
            if (previewp != null) {
                previewp.setSnapshotType(LLSnapshotModel.ESnapshotType.SNAPSHOT_TEXTURE)
                previewp.setSnapshotFormat(LLSnapshotModel.ESnapshotFormat.SNAPSHOT_FORMAT_PNG)
                previewp.setSnapshotBufferType(LLSnapshotModel.ESnapshotLayerType.SNAPSHOT_TYPE_COLOR)
            }
        }

        fun updateResolution(data: Any?) {
            val view = data as? LLFloaterSimpleSnapshot ?: return

            var width = THUMBNAIL_SNAPSHOT_DIM_MAX
            var height = THUMBNAIL_SNAPSHOT_DIM_MAX

            val previewp: LLSnapshotLivePreview? = getPreviewView()
            if (previewp != null) {
                val originalWidth = previewp.getSizeWidth()
                val originalHeight = previewp.getSizeHeight()

                if (gSavedSettings.getBOOL("RenderHUDInSnapshot")) {
                    width = minOf(width, gViewerWindow.getWindowWidthRaw())
                    height = minOf(height, gViewerWindow.getWindowHeightRaw())
                }

                previewp.setSize(width, height)

                if (originalWidth != width || originalHeight != height) {
                    checkAutoSnapshot(previewp, false)
                    previewp.updateSnapshot(true)
                }
            }
        }

        override fun setStatus(status: EStatus, ok: Boolean, msg: String) {
            when (status) {
                EStatus.STATUS_READY -> mFloater.setCtrlsEnabled(true)
                EStatus.STATUS_WORKING -> mFloater.setCtrlsEnabled(false)
                EStatus.STATUS_FINISHED -> mFloater.setCtrlsEnabled(true)
            }
            mStatus = status
        }

        override fun setFinished(finished: Boolean, ok: Boolean, msg: String) {}

        companion object {
            fun onSnapshotUploadFinished(floater: LLFloaterSnapshotBase, status: Boolean) {
                System.err.println("LLFloaterSimpleSnapshot: onSnapshotUploadFinished not yet implemented")
            }
        }
    }

    companion object {
        const val THUMBNAIL_SNAPSHOT_DIM_MAX: Int = 256
        const val THUMBNAIL_SNAPSHOT_DIM_MIN: Int = 64

        fun update() {
            val instList = LLFloaterReg.getFloaterList("simple_snapshot")
            for (inst in instList) {
                val floater = inst as? LLFloaterSimpleSnapshot ?: continue
                floater.impl.updateLivePreview()
            }
        }

        fun findInstance(key: LLSD): LLFloaterSimpleSnapshot? =
            LLFloaterReg.findTypedInstance("simple_snapshot", key)

        fun getInstance(key: LLSD): LLFloaterSimpleSnapshot? =
            LLFloaterReg.getTypedInstance("simple_snapshot", key)

        fun uploadThumbnail(
            filePath: String,
            inventoryId: UUID,
            taskId: UUID,
            callback: ((UUID) -> Unit)? = null
        ) {
            val tempFile: String = gDirUtilp.getTempFilename()
            val codec = LLImageBase.getCodecFromExtension(gDirUtilp.getExtension(filePath))
            if (!LLViewerTextureList.createUploadFile(filePath, tempFile, codec, THUMBNAIL_SNAPSHOT_DIM_MAX, THUMBNAIL_SNAPSHOT_DIM_MIN, true)) {
                val reason = LLImage.getLastThreadError()
                LLNotificationsUtil.add("CannotUploadTexture", mapOf("REASON" to reason))
                return
            }
            uploadImageUploadFile(tempFile, inventoryId, taskId, callback)
        }

        fun uploadThumbnail(
            rawImage: LLImageRaw,
            inventoryId: UUID,
            taskId: UUID,
            callback: ((UUID) -> Unit)? = null
        ) {
            val tempFile: String = gDirUtilp.getTempFilename()
            if (!LLViewerTextureList.createUploadFile(rawImage, tempFile, THUMBNAIL_SNAPSHOT_DIM_MAX, THUMBNAIL_SNAPSHOT_DIM_MIN)) {
                val reason = LLImage.getLastThreadError()
                LLNotificationsUtil.add("CannotUploadTexture", mapOf("REASON" to reason))
                return
            }
            uploadImageUploadFile(tempFile, inventoryId, taskId, callback)
        }

        private fun uploadImageUploadFile(
            tempFile: String,
            inventoryId: UUID,
            taskId: UUID,
            callback: ((UUID) -> Unit)?
        ) {
            val data = LLSD()
            val nullUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")
            when {
                taskId != nullUuid -> {
                    data["item_id"] = inventoryId.toString()
                    data["task_id"] = taskId.toString()
                }
                gInventory.getCategory(inventoryId) != null -> {
                    data["category_id"] = inventoryId.toString()
                }
                else -> {
                    data["item_id"] = inventoryId.toString()
                }
            }

            val capUrl: String = gAgent.getRegionCapability(THUMBNAIL_UPLOAD_CAP)
            if (capUrl.isEmpty()) {
                LLNotificationsUtil.add("RegionCapabilityRequestError", mapOf("CAPABILITY" to THUMBNAIL_UPLOAD_CAP))
                return
            }

            System.err.println("LLFloaterSimpleSnapshot: uploadImageUploadFile not yet implemented")
        }
    }
}

class LLSimpleSnapshotFloaterView(params: Any) : LLFloaterView(params)
