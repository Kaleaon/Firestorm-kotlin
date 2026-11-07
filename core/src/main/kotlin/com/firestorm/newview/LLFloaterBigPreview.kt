package com.firestorm.newview

class LLFloaterBigPreview(key: LLSD) : LLFloater(key) {

    private var mPreviewHandle: LLHandle<LLView>? = null
    private var mPreviewPlaceholder: LLUICtrl? = null
    private var mFloaterOwner: LLFloater? = null

    override fun postBuild(): Boolean {
        mPreviewPlaceholder = getChild<LLUICtrl>("big_preview_placeholder")
        return super.postBuild()
    }

    fun setPreview(previewp: LLView) {
        mPreviewHandle = previewp.getHandle()
    }

    fun setFloaterOwner(floaterp: LLFloater) {
        mFloaterOwner = floaterp
    }

    fun isFloaterOwner(floaterp: LLFloater): Boolean = mFloaterOwner == floaterp

    fun closeOnFloaterOwnerClosing(floaterp: LLFloater) {
        if (isFloaterOwner(floaterp)) {
            closeFloater()
        }
    }

    fun onCancel() {
        closeFloater()
    }

    override fun draw() {
        super.draw()

        val previewp = mPreviewHandle?.get() as? LLSnapshotLivePreview ?: return
        if (previewp.getBigThumbnailImage() == null) return

        val previewRect = mPreviewPlaceholder!!.getRect()

        var thumbnailW = previewp.getBigThumbnailWidth()
        var thumbnailH = previewp.getBigThumbnailHeight()

        // Prevent anisotropic scaling: compute uniform ratio to fit thumbnail inside placeholder rect.
        val ratio = maxOf(
            thumbnailW.toFloat() / previewRect.getWidth().toFloat(),
            thumbnailH.toFloat() / previewRect.getHeight().toFloat()
        )
        thumbnailW = (thumbnailW.toFloat() / ratio).toInt()
        thumbnailH = (thumbnailH.toFloat() / ratio).toInt()

        val localOffsetX = (previewRect.getWidth() - thumbnailW) / 2
        val localOffsetY = (previewRect.getHeight() - thumbnailH) / 2

        val offsetX = previewRect.mLeft + localOffsetX
        val offsetY = previewRect.mBottom + localOffsetY

        // no-op
    }

    override fun onDestroy() {
        mPreviewHandle?.get()?.die()
        super.onDestroy()
    }
}
