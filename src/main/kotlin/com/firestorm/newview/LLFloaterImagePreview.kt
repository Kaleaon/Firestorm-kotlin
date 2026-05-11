package com.firestorm.newview

private const val ALPHA_EMPTY_THRESHOLD: UByte = 253u
private const val ALPHA_EMPTY_THRESHOLD_RATIO: Float = 0.999f

class LLImagePreviewSculpted(width: Int, height: Int) : LLViewerDynamicTexture(width, height, 3, ORDER_MIDDLE, false) {

    var mNeedsUpdate: Boolean = true
    var mCameraDistance: Float = 0f
    var mCameraYaw: Float = 0f
    var mCameraPitch: Float = 0f
    var mCameraZoom: Float = 1f
    var mCameraOffset: FloatArray = FloatArray(3)
    var mTextureName: UInt = 0u
    private var mVolume: LLVolume? = null
    private var mVertexBuffer: LLVertexBuffer? = null

    init {
        val volumeParams = LLVolumeParams()
        volumeParams.setType(LL_PCODE_PROFILE_CIRCLE, LL_PCODE_PATH_CIRCLE)
        volumeParams.setSculptID(UUID_NULL, LL_SCULPT_TYPE_SPHERE)
        mVolume = LLVolume(volumeParams, 4.0f)
    }

    override fun getType(): Byte = LLViewerDynamicTexture.LL_IMAGE_PREVIEW_SCULPTED

    fun setTexture(name: UInt) { mTextureName = name }

    fun setPreviewTarget(imagep: LLImageRaw?, distance: Float) {
        mCameraDistance = distance
        mCameraZoom = 1f
        mCameraPitch = 0f
        mCameraYaw = 0f
        mCameraOffset = FloatArray(3)

        if (imagep != null) {
            mVolume?.sculpt(imagep.getWidth(), imagep.getHeight(), imagep.getComponents(), imagep.getData(), 0, false)
        }

        val vf = mVolume!!.getVolumeFace(0)
        val numIndices = vf.mNumIndices
        val numVertices = vf.mNumVertices

        mVertexBuffer = LLVertexBuffer(
            LLVertexBuffer.MAP_VERTEX or LLVertexBuffer.MAP_NORMAL or LLVertexBuffer.MAP_TEXCOORD0
        )
        if (!mVertexBuffer!!.allocateBuffer(numVertices, numIndices)) {
            TODO("APR: use JVM equivalent - failed to allocate vertex buffer for sculpted preview")
        }

        TODO("GPU: fill vertex/normal/texcoord/index buffers from volume face and call unmapBuffer")
    }

    override fun render(): Boolean {
        mNeedsUpdate = false
        TODO("GPU: render sculpted preview using camera matrices, vertex buffer, and pipeline lighting")
    }

    fun refresh() { mNeedsUpdate = true }

    fun rotate(yawRadians: Float, pitchRadians: Float) {
        mCameraYaw += yawRadians
        mCameraPitch = (mCameraPitch + pitchRadians).coerceIn(-Math.PI.toFloat() / 2f * 0.8f, Math.PI.toFloat() / 2f * 0.8f)
    }

    fun zoom(zoomAmt: Float) {
        mCameraZoom = (mCameraZoom + zoomAmt).coerceIn(1f, 10f)
    }

    fun pan(right: Float, up: Float) {
        mCameraOffset[1] = (mCameraOffset[1] + right * mCameraDistance / mCameraZoom).coerceIn(-1f, 1f)
        mCameraOffset[2] = (mCameraOffset[2] + up * mCameraDistance / mCameraZoom).coerceIn(-1f, 1f)
    }

    override fun needsRender(): Boolean = mNeedsUpdate
}


class LLImagePreviewAvatar(width: Int, height: Int) : LLViewerDynamicTexture(width, height, 3, ORDER_MIDDLE, false) {

    var mNeedsUpdate: Boolean = true
    private var mTargetJoint: LLJoint? = null
    private var mTargetMesh: LLViewerJointMesh? = null
    var mCameraDistance: Float = 0f
    var mCameraYaw: Float = 0f
    var mCameraPitch: Float = 0f
    var mCameraZoom: Float = 1f
    var mCameraOffset: FloatArray = FloatArray(3)
    private var mDummyAvatar: LLVOAvatar? = null
    var mTextureName: UInt = 0u

    init {
        mDummyAvatar = gObjectList.createObjectViewer(
            LL_PCODE_LEGACY_AVATAR, gAgent.getRegion(), LLViewerObject.CO_FLAG_UI_AVATAR
        ) as LLVOAvatar
        mDummyAvatar!!.mSpecialRenderMode = 2
    }

    override fun getType(): Byte = LLViewerDynamicTexture.LL_IMAGE_PREVIEW_AVATAR

    fun setTexture(name: UInt) { mTextureName = name }

    fun setPreviewTarget(jointName: String, meshName: String, imagep: LLImageRaw?, distance: Float, male: Boolean) {
        mTargetJoint = mDummyAvatar!!.mRoot.findJoint(jointName)
        mTargetMesh?.setTestTexture(0u)

        val weight = if (male) 1f else 0f
        mDummyAvatar!!.setVisualParamWeight("male", weight)
        mDummyAvatar!!.updateVisualParams()
        mDummyAvatar!!.updateGeometry(mDummyAvatar!!.mDrawable)
        mDummyAvatar!!.mRoot.setVisible(false, true)

        mTargetMesh = mDummyAvatar!!.mRoot.findJoint(meshName) as? LLViewerJointMesh
        mTargetMesh?.setTestTexture(mTextureName)
        mTargetMesh?.setVisible(true, false)
        mCameraDistance = distance
        mCameraZoom = 1f
        mCameraPitch = 0f
        mCameraYaw = 0f
        mCameraOffset = FloatArray(3)
    }

    fun clearPreviewTexture(meshName: String) {
        val mesh = mDummyAvatar?.mRoot?.findJoint(meshName) as? LLViewerJointMesh
        mesh?.setTestTexture(0u)
    }

    override fun render(): Boolean {
        mNeedsUpdate = false
        TODO("GPU: render avatar preview using camera matrices, joint transform, and avatar draw pool")
    }

    fun refresh() { mNeedsUpdate = true }

    fun rotate(yawRadians: Float, pitchRadians: Float) {
        mCameraYaw += yawRadians
        mCameraPitch = (mCameraPitch + pitchRadians).coerceIn(-Math.PI.toFloat() / 2f * 0.8f, Math.PI.toFloat() / 2f * 0.8f)
    }

    fun zoom(zoomAmt: Float) {
        mCameraZoom = (mCameraZoom + zoomAmt).coerceIn(1f, 10f)
    }

    fun pan(right: Float, up: Float) {
        mCameraOffset[1] = (mCameraOffset[1] + right * mCameraDistance / mCameraZoom).coerceIn(-1f, 1f)
        mCameraOffset[2] = (mCameraOffset[2] + up * mCameraDistance / mCameraZoom).coerceIn(-1f, 1f)
    }

    override fun needsRender(): Boolean = mNeedsUpdate

    fun markDead() { mDummyAvatar?.markDead() }
}


class LLFloaterImagePreview(val args: Map<String, Any>) : LLFloaterNameDesc(args) {

    private var mRawImagep: LLImageRaw? = null
    private var mAvatarPreview: LLImagePreviewAvatar? = null
    private var mSculptedPreview: LLImagePreviewSculpted? = null
    private var mLastMouseX: Int = 0
    private var mLastMouseY: Int = 0
    private val mPreviewRect: LLRect = LLRect()
    private val mPreviewImageRect: LLRectf = LLRectf(0f, 1f, 1f, 0f)
    private var mImagep: LLViewerTexture? = null
    private var mImageLoadError: String = ""
    private var mDeleteTempFile: String = ""
    private var mEmptyAlphaCheck: LLCheckBoxCtrl? = null

    init {
        loadImage(mFilenameAndPath)
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false

        val iface = childGetSelectionInterface("clothing_type_combo")
        iface?.selectFirstItem()
        childSetCommitCallback("clothing_type_combo") { ctrl -> onPreviewTypeCommit(ctrl, this) }

        mPreviewRect.set(getChildView("preview_area").getRect())
        mPreviewImageRect.set(0f, 1f, 1f, 0f)

        getChildView("bad_image_text").setVisible(false)

        val tempCheck = getChild<LLCheckBoxCtrl>("temp_check")
        val losslessCheck = getChild<LLCheckBoxCtrl>("lossless_check")
        val uploadedSizeText = getChild<LLUICtrl>("uploaded_size_text")

        val rawImage = mRawImagep
        if (rawImage != null && gAgent.getRegion() != null) {
            mAvatarPreview = LLImagePreviewAvatar(256, 256).also {
                it.setPreviewTarget("mPelvis", "mUpperBodyMesh0", rawImage, 2f, false)
            }
            mSculptedPreview = LLImagePreviewSculpted(256, 256).also {
                it.setPreviewTarget(rawImage, 2.0f)
            }

            if (rawImage.getWidth() * rawImage.getHeight() <= LL_IMAGE_REZ_LOSSLESS_CUTOFF * LL_IMAGE_REZ_LOSSLESS_CUTOFF) {
                losslessCheck.setEnabled(true)
                losslessCheck.setVisible(true)
                losslessCheck.setControlVariable(gSavedSettings.getControl("LosslessJ2CUpload"))
            } else {
                losslessCheck.setEnabled(false)
                losslessCheck.setVisible(false)
            }

            val enableTempUploads = LLAgentBenefitsMgr.current().getTextureUploadCost() != 0 &&
                    gAgent.getRegion()!!.getCentralBakeVersion() == 0
            if (!enableTempUploads) {
                gSavedSettings.setBOOL("TemporaryUpload", false)
            }
            tempCheck.setVisible(enableTempUploads)

            getChild<LLUICtrl>("ok_btn").setCommitCallback { onBtnUpload() }

            uploadedSizeText.setTextArg("[X_RES]", rawImage.getWidth().toString())
            uploadedSizeText.setTextArg("[Y_RES]", rawImage.getHeight().toString())
            uploadedSizeText.setVisible(true)

            mEmptyAlphaCheck = getChild("strip_alpha_check")

            if (rawImage.getComponents() != 4) {
                getChild<LLUICtrl>("image_alpha_warning").setVisible(false)
                uploadedSizeText.setTextArg("[ALPHA]", getString("no_alpha"))
                return true
            }

            val imageBytes = rawImage.getWidth() * rawImage.getHeight() * 4
            val data = rawImage.getData()
            var emptyAlphaCount = 0
            var i = 3
            while (i < imageBytes) {
                if ((data[i].toInt() and 0xFF) > ALPHA_EMPTY_THRESHOLD.toInt()) {
                    emptyAlphaCount++
                }
                i += 4
            }

            if (emptyAlphaCount > (imageBytes / 4 * ALPHA_EMPTY_THRESHOLD_RATIO).toInt()) {
                getChild<LLUICtrl>("image_alpha_warning").setVisible(true)
                mEmptyAlphaCheck!!.setCommitCallback { emptyAlphaCheckboxCallback() }
                mEmptyAlphaCheck!!.setValue(true)
            } else {
                getChild<LLUICtrl>("image_alpha_warning").setVisible(false)
                mEmptyAlphaCheck!!.setValue(false)
            }

            val alphaKey = if (mEmptyAlphaCheck!!.getValue().asBoolean()) "no_alpha" else "with_alpha"
            uploadedSizeText.setTextArg("[ALPHA]", getString(alphaKey))
        } else {
            mAvatarPreview = null
            mSculptedPreview = null
            getChildView("bad_image_text").setVisible(true)
            getChildView("clothing_type_combo").setEnabled(false)
            getChildView("ok_btn").setEnabled(false)

            uploadedSizeText.setVisible(false)
            losslessCheck.setVisible(false)
            tempCheck.setVisible(false)

            if (mImageLoadError.isNotEmpty()) {
                getChild<LLUICtrl>("bad_image_text").setValue(mImageLoadError)
            }
        }

        return true
    }

    fun emptyAlphaCheckboxCallback() {
        if (mEmptyAlphaCheck!!.getValue().asBoolean()) {
            getChild<LLUICtrl>("uploaded_size_text").setTextArg("[ALPHA]", getString("no_alpha"))
        } else {
            LLNotificationsUtil.add("ImageEmptyAlphaLayer", emptyMap(), emptyMap()) { notification, response ->
                imageEmptyAlphaCallback(notification, response)
            }
        }
    }

    fun imageEmptyAlphaCallback(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        val option = LLNotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) {
            mEmptyAlphaCheck!!.setValue(true)
        }
        val alphaKey = if (option == 0) "no_alpha" else "with_alpha"
        getChild<LLUICtrl>("uploaded_size_text").setTextArg("[ALPHA]", getString(alphaKey))
        return true
    }

    fun onBtnUpload() {
        if (mEmptyAlphaCheck?.getValue()?.asBoolean() == true) {
            val raw = mRawImagep!!
            val strippedImage = LLImageRaw(raw.getWidth(), raw.getHeight(), 3)
            strippedImage.copyUnscaled4onto3(raw)

            val strippedPng = LLImagePNG()
            strippedPng.encode(strippedImage, 0.0f)

            mFilenameAndPath = gDirUtilp.getTempFilename() + "." + strippedPng.getExtension()
            strippedPng.save(mFilenameAndPath)
            mDeleteTempFile = mFilenameAndPath
        }
        onBtnOK()
    }

    override fun getExpectedUploadCost(): Int =
        LLAgentBenefitsMgr.current().getTextureUploadCost(mRawImagep)

    override fun destroy() {
        if (mDeleteTempFile.isNotEmpty()) {
            LLFile.remove(mDeleteTempFile)
        }
        clearAllPreviewTextures()
        mRawImagep = null
        mImagep = null
        super.destroy()
    }

    fun clearAllPreviewTextures() {
        mAvatarPreview?.let { ap ->
            ap.clearPreviewTexture("mHairMesh0")
            ap.clearPreviewTexture("mUpperBodyMesh0")
            ap.clearPreviewTexture("mLowerBodyMesh0")
            ap.clearPreviewTexture("mHeadMesh0")
            ap.clearPreviewTexture("mSkirtMesh0")
        }
    }

    fun onBtnOK() {
        getChildView("ok_btn").setEnabled(false)

        val expectedUploadCost = getExpectedUploadCost()
        if (canAffordTransaction(expectedUploadCost)) {
            val tid = LLTransactionID()
            tid.generate()
            val newAssetId = tid.makeAssetID(gAgent.getSecureSessionID())

            val formatted = LLImageJ2C()
            val raw = mRawImagep!!

            if (raw.getWidth() * raw.getHeight() <= LL_IMAGE_REZ_LOSSLESS_CUTOFF * LL_IMAGE_REZ_LOSSLESS_CUTOFF) {
                if (gSavedSettings.getBOOL("LosslessJ2CUpload")) {
                    formatted.setReversible(true)
                }
            }

            if (formatted.encode(raw, 0.0f)) {
                TODO("APR: use JVM equivalent - write encoded J2C to LLFileSystem and upload via LLResourceUploadInfo")
            } else {
                val args = mutableMapOf<String, Any>()
                args["REASON"] = LLImage.getLastThreadError()
                LLNotificationsUtil.add("ErrorEncodingImage", args)
            }
        } else {
            val args = mutableMapOf<String, Any>()
            args["COST"] = expectedUploadCost.toString()
            LLNotificationsUtil.add("ErrorCannotAffordUpload", args)
        }

        closeFloater(false)
    }

    override fun draw() {
        super.draw()

        val raw = mRawImagep ?: return

        val iface = childGetSelectionInterface("clothing_type_combo")
        var selected = 0
        if (iface != null) {
            selected = iface.getFirstSelectedIndex()
        }

        if (selected <= 0) {
            TODO("GPU: draw checkerboard background and blit raw image rect using mPreviewImageRect/mPreviewRect coords")
        } else {
            if (mAvatarPreview != null && mSculptedPreview != null) {
                TODO("GPU: bind avatar or sculpted preview texture and draw to mPreviewRect")
            }
        }
    }

    private fun loadImage(srcFilename: String): Boolean {
        return try {
            val exten = gDirUtilp.getExtension(srcFilename)
            val codec = LLImageBase.getCodecFromExtension(exten)

            val imageInfo = LLImageDimensionsInfo()
            if (!imageInfo.load(srcFilename, codec)) {
                mImageLoadError = imageInfo.getLastError()
                return false
            }

            val maxImageArea = 8096 * 8096
            if (imageInfo.getWidth() * imageInfo.getHeight() > maxImageArea) {
                mImageLoadError = LLTrans.getString(
                    "texture_load_area_error",
                    mapOf("PIXELS" to "${maxImageArea / 1_000_000}M")
                )
                return false
            }

            val image = LLImageFormatted.createFromType(codec) ?: return false
            if (!image.load(srcFilename)) return false

            val rawImage = LLImageRaw()
            if (!image.decode(rawImage, 0.0f)) return false

            val components = image.getComponents()
            if (components != 3 && components != 4) {
                image.setLastError("Image files with less than 3 or more than 4 components are not supported.")
                return false
            }

            val maxWidth = gSavedSettings.getS32("max_texture_dimension_X")
            val maxHeight = gSavedSettings.getS32("max_texture_dimension_Y")
            val origWidth = rawImage.getWidth()
            val origHeight = rawImage.getHeight()

            if (origWidth > maxWidth || origHeight > maxHeight) {
                val widthScale = maxWidth.toFloat() / origWidth.toFloat()
                val heightScale = maxHeight.toFloat() / origHeight.toFloat()
                val scale = minOf(widthScale, heightScale)

                val newWidth = LLImageRaw.contractDimToPowerOfTwo(
                    (origWidth * scale).toInt().coerceIn(4, maxWidth)
                )
                val newHeight = LLImageRaw.contractDimToPowerOfTwo(
                    (origHeight * scale).toInt().coerceIn(4, maxHeight)
                )

                if (!rawImage.scale(newWidth, newHeight)) return false

                LLNotificationsUtil.add(
                    "ImageUploadResized",
                    mapOf(
                        "[ORIGINAL_WIDTH]" to origWidth,
                        "[ORIGINAL_HEIGHT]" to origHeight,
                        "[NEW_WIDTH]" to newWidth,
                        "[NEW_HEIGHT]" to newHeight,
                        "[MAX_WIDTH]" to maxWidth,
                        "[MAX_HEIGHT]" to maxHeight
                    )
                )
            }

            rawImage.biasedScaleToPowerOfTwo(LLViewerFetchedTexture.MAX_IMAGE_SIZE_DEFAULT)
            mRawImagep = rawImage
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (mPreviewRect.pointInRect(x, y)) {
            bringToFront(x, y)
            gFocusMgr.setMouseCapture(this)
            gViewerWindow.hideCursor()
            mLastMouseX = x
            mLastMouseY = y
            return true
        }
        return super.handleMouseDown(x, y, mask)
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        gFocusMgr.setMouseCapture(null)
        gViewerWindow.showCursor()
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        val localMask = mask and MASK_ALT.inv()

        if (mAvatarPreview != null && hasMouseCapture()) {
            if (localMask == MASK_PAN) {
                val iface = childGetSelectionInterface("clothing_type_combo")
                if (iface != null && iface.getFirstSelectedIndex() <= 0) {
                    mPreviewImageRect.translate(
                        (x - mLastMouseX) * -0.005f * mPreviewImageRect.getWidth(),
                        (y - mLastMouseY) * -0.005f * mPreviewImageRect.getHeight()
                    )
                } else {
                    mAvatarPreview!!.pan((x - mLastMouseX) * -0.005f, (y - mLastMouseY) * -0.005f)
                    mSculptedPreview!!.pan((x - mLastMouseX) * -0.005f, (y - mLastMouseY) * -0.005f)
                }
            } else if (localMask == MASK_ORBIT) {
                val yawRadians = (x - mLastMouseX) * -0.01f
                val pitchRadians = (y - mLastMouseY) * 0.02f
                mAvatarPreview!!.rotate(yawRadians, pitchRadians)
                mSculptedPreview!!.rotate(yawRadians, pitchRadians)
            } else {
                val iface = childGetSelectionInterface("clothing_type_combo")
                if (iface != null && iface.getFirstSelectedIndex() <= 0) {
                    val zoomAmt = (y - mLastMouseY) * -0.002f
                    mPreviewImageRect.stretch(zoomAmt)
                } else {
                    val yawRadians = (x - mLastMouseX) * -0.01f
                    val zoomAmt = (y - mLastMouseY) * 0.02f
                    mAvatarPreview!!.rotate(yawRadians, 0f)
                    mAvatarPreview!!.zoom(zoomAmt)
                    mSculptedPreview!!.rotate(yawRadians, 0f)
                    mSculptedPreview!!.zoom(zoomAmt)
                }
            }

            val iface = childGetSelectionInterface("clothing_type_combo")
            if (iface != null && iface.getFirstSelectedIndex() <= 0) {
                if (mPreviewImageRect.getWidth() > 1f) {
                    mPreviewImageRect.stretch((1f - mPreviewImageRect.getWidth()) * 0.5f)
                } else if (mPreviewImageRect.getWidth() < 0.1f) {
                    mPreviewImageRect.stretch((0.1f - mPreviewImageRect.getWidth()) * 0.5f)
                }
                if (mPreviewImageRect.getHeight() > 1f) {
                    mPreviewImageRect.stretch((1f - mPreviewImageRect.getHeight()) * 0.5f)
                } else if (mPreviewImageRect.getHeight() < 0.1f) {
                    mPreviewImageRect.stretch((0.1f - mPreviewImageRect.getHeight()) * 0.5f)
                }
                if (mPreviewImageRect.mLeft < 0f) {
                    mPreviewImageRect.translate(-mPreviewImageRect.mLeft, 0f)
                } else if (mPreviewImageRect.mRight > 1f) {
                    mPreviewImageRect.translate(1f - mPreviewImageRect.mRight, 0f)
                }
                if (mPreviewImageRect.mBottom < 0f) {
                    mPreviewImageRect.translate(0f, -mPreviewImageRect.mBottom)
                } else if (mPreviewImageRect.mTop > 1f) {
                    mPreviewImageRect.translate(0f, 1f - mPreviewImageRect.mTop)
                }
            } else {
                mAvatarPreview!!.refresh()
                mSculptedPreview!!.refresh()
            }

            LLUIInstance.setMousePositionLocal(this, mLastMouseX, mLastMouseY)
        }

        if (!mPreviewRect.pointInRect(x, y) || mAvatarPreview == null || mSculptedPreview == null) {
            return super.handleHover(x, y, mask)
        } else if (localMask == MASK_ORBIT) {
            gViewerWindow.setCursor(UI_CURSOR_TOOLCAMERA)
        } else if (localMask == MASK_PAN) {
            gViewerWindow.setCursor(UI_CURSOR_TOOLPAN)
        } else {
            gViewerWindow.setCursor(UI_CURSOR_TOOLZOOMIN)
        }

        return true
    }

    override fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (mPreviewRect.pointInRect(x, y) && mAvatarPreview != null) {
            mAvatarPreview!!.zoom(clicks * -0.2f)
            mAvatarPreview!!.refresh()
            mSculptedPreview!!.zoom(clicks * -0.2f)
            mSculptedPreview!!.refresh()
        }
        return true
    }

    companion object {
        fun onMouseCaptureLostImagePreview(handler: LLMouseHandler?) {
            gViewerWindow.showCursor()
        }

        private fun onPreviewTypeCommit(ctrl: LLUICtrl?, userdata: Any?) {
            val fp = userdata as? LLFloaterImagePreview ?: return
            if (fp.mAvatarPreview == null || fp.mSculptedPreview == null) return

            val iface = fp.childGetSelectionInterface("clothing_type_combo") ?: return
            val whichMode = iface.getFirstSelectedIndex()

            val raw = fp.mRawImagep
            when (whichMode) {
                0 -> Unit
                1 -> fp.mAvatarPreview!!.setPreviewTarget("mSkull", "mHairMesh0", raw, 0.4f, false)
                2 -> fp.mAvatarPreview!!.setPreviewTarget("mSkull", "mHeadMesh0", raw, 0.4f, false)
                3 -> fp.mAvatarPreview!!.setPreviewTarget("mChest", "mUpperBodyMesh0", raw, 1.0f, false)
                4 -> fp.mAvatarPreview!!.setPreviewTarget("mKneeLeft", "mLowerBodyMesh0", raw, 1.2f, false)
                5 -> fp.mAvatarPreview!!.setPreviewTarget("mSkull", "mHeadMesh0", raw, 0.4f, true)
                6 -> fp.mAvatarPreview!!.setPreviewTarget("mChest", "mUpperBodyMesh0", raw, 1.2f, true)
                7 -> fp.mAvatarPreview!!.setPreviewTarget("mKneeLeft", "mLowerBodyMesh0", raw, 1.2f, true)
                8 -> fp.mAvatarPreview!!.setPreviewTarget("mKneeLeft", "mSkirtMesh0", raw, 1.3f, false)
                9 -> fp.mSculptedPreview!!.setPreviewTarget(raw, 2.0f)
            }

            fp.mAvatarPreview!!.refresh()
            fp.mSculptedPreview!!.refresh()
        }
    }
}
