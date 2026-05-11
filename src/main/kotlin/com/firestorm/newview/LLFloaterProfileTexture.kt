package com.firestorm.newview

import com.firestorm.ui.LLFloater
import com.firestorm.ui.LLButton
import com.firestorm.ui.LLIconCtrl
import com.firestorm.ui.LLView
import com.firestorm.ui.LLUICtrl
import com.firestorm.llsd.LLSD
import com.firestorm.texture.LLViewerFetchedTexture
import java.util.UUID

class LLProfileImageCtrl(p: Params) : LLIconCtrl(p) {

    class Params : LLIconCtrl.Params()

    private var mImage: LLViewerFetchedTexture? = null
    private var mImageID: UUID = UUID.randomUUID().let { UUID(0, 0) }
    private var mImageOldBoostLevel: Int = LLGLTexture.BOOST_NONE
    private var mWasNoDelete: Boolean = false

    private val mImageLoadedSignal: MutableList<(Boolean, LLViewerFetchedTexture?) -> Unit> = mutableListOf()

    fun destroy() {
        TODO("APR: use JVM equivalent — clean up callback list mCallbackTextureList")
        releaseTexture()
    }

    private fun releaseTexture() {
        mImage?.let { img ->
            img.setBoostLevel(mImageOldBoostLevel)
            if (!mWasNoDelete) {
                // setBoostLevel marks images as NO_DELETE in most cases; undo that here.
                img.forceActive()
            }
            mImage = null
        }
    }

    override fun setValue(value: LLSD) {
        val id = value.asUUID()
        setImageAssetId(id)
        if (id == null || id == UUID(0, 0)) {
            super.setValue(LLSD("Generic_Person_Large"), LLGLTexture.BOOST_UI)
        } else {
            // Called second so the old boost level is captured in mImageOldBoostLevel first.
            super.setValue(value, LLGLTexture.BOOST_PREVIEW)
        }
    }

    open fun draw() {
        mImage?.let { img ->
            TODO("GPU: img.addTextureStats(MAX_IMAGE_AREA); img.setKnownDrawSize(MAX_IMAGE_SIZE_DEFAULT, MAX_IMAGE_SIZE_DEFAULT)")
        }
        super.draw()
    }

    fun setImageLoadedCallback(cb: (Boolean, LLViewerFetchedTexture?) -> Unit) {
        mImageLoadedSignal.add(cb)
    }

    fun getImageAssetId(): UUID = mImageID

    fun getImage(): LLViewerFetchedTexture? = mImage

    private fun setImageAssetId(assetId: UUID?) {
        if (mImageID == assetId) return

        releaseTexture()
        mImageID = assetId ?: UUID(0, 0)

        if (assetId != null && assetId != UUID(0, 0)) {
            TODO("GPU: fetch texture via LLViewerTextureManager.getFetchedTexture; set boost level, draw size, forceToSaveRawImage; register loaded callback")
        }
    }

    private fun onImageLoaded(success: Boolean, img: LLViewerFetchedTexture?) {
        mImageLoadedSignal.forEach { it(success, img) }
    }

    companion object {
        fun onImageLoadedStatic(
            success: Boolean,
            srcVi: LLViewerFetchedTexture?,
            discardLevel: Int,
            final: Boolean,
            userdata: Any?
        ) {
            val handle = userdata as? LLHandle<LLUICtrl> ?: return
            if (!handle.isDead()) {
                val caller = handle.get() as? LLProfileImageCtrl
                caller?.mImageLoadedSignal?.forEach { it(success, srcVi) }
            }
            if (final || !success) {
                // handle goes out of scope and is GC'd — no manual delete needed in JVM
            }
        }
    }
}

class LLFloaterProfileTexture(owner: LLView) : LLFloater(LLSD()) {

    private var mContextConeOpacity: Float = 0f
    private var mLastHeight: Int = 0
    private var mLastWidth: Int = 0

    private val mOwnerHandle: LLHandle<LLView> = owner.getHandle()
    private var mProfileIcon: LLProfileImageCtrl? = null
    private var mCloseButton: LLButton? = null

    init {
        buildFromFile("floater_profile_texture.xml")
    }

    override fun postBuild(): Boolean {
        mProfileIcon = getChild("profile_pic")
        mProfileIcon!!.setImageLoadedCallback { success, imagep -> onImageLoaded(success, imagep) }

        mCloseButton = getChild("close_btn")
        mCloseButton!!.setCommitCallback { closeFloater() }

        getChild<LLButton>("btn_refresh").setCommitCallback { refreshTexture() }

        return true
    }

    open fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        super.reshape(width, height, calledFromParent)
    }

    open fun draw() {
        val owner = mOwnerHandle.get()
        val maxOpacity = gSavedSettings.getFloat("PickerContextOpacity", 0.4f)
        drawConeToOwner(mContextConeOpacity, maxOpacity, owner)
        super.draw()
    }

    open fun onOpen(key: LLSD) {
        mCloseButton?.setFocus(true)
    }

    fun resetAsset() {
        mProfileIcon?.setValue(LLSD(UUID(0, 0)))
    }

    fun loadAsset(imageId: UUID) {
        mProfileIcon?.setValue(LLSD(imageId))
        updateDimensions()
    }

    fun refreshTexture() {
        val icon = mProfileIcon ?: return
        if (icon.getImageAssetId() != UUID(0, 0) && icon.getImage() != null) {
            TODO("GPU: destroy_texture(icon.getImageAssetId()); icon.getImage()!!.forceToRefetchTexture()")
        }
    }

    fun onImageLoaded(success: Boolean, imagep: LLViewerFetchedTexture?) {
        if (success) {
            updateDimensions()
        }
    }

    fun getHandle(): LLHandle<LLFloater> = super.getHandle()

    private fun updateDimensions() {
        val image = mProfileIcon?.getImage() ?: return
        TODO("GPU: read image.getFullWidth() / getFullHeight(); scale to MAX_DIMENTIONS=512; call reshape(); gFloaterView.adjustToFitScreen()")
    }
}
