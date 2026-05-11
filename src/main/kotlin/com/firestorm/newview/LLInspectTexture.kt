package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// LLInspectTextureUtil
// ---------------------------------------------------------------------------

object LLInspectTextureUtil {

    fun createInventoryToolTip(p: LLToolTip.Params): LLToolTip? {
        // RLVa: suppress texture preview when not permitted
        if (!RlvActions.canPreviewTextures()) {
            return LLToolTip(p)
        }

        val sdTooltip = p.createParams

        if (sdTooltip.has("thumbnail_id") && sdTooltip.asUUID("thumbnail_id") != null) {
            return LLTextureToolTip(p)
        }

        val eInvType: LLInventoryType.EType =
            if (sdTooltip.has("inv_type"))
                LLInventoryType.EType.fromInt(sdTooltip.asInt("inv_type"))
            else LLInventoryType.EType.IT_NONE

        return when (eInvType) {
            LLInventoryType.EType.IT_CATEGORY -> {
                if (sdTooltip.has("item_id")) {
                    val idCategory: UUID = sdTooltip.asUUID("item_id")!!
                    val cat = gInventory.getCategory(idCategory)
                    if (cat != null && cat.preferredType == LLFolderType.EType.FT_OUTFIT) {
                        val items = mutableListOf<LLViewerInventoryItem>()
                        val isTextureType = LLIsTextureType()
                        gInventory.getDirectDescendentsOf(idCategory, mutableListOf(), items, isTextureType)

                        if (items.size == 1) {
                            var item: LLViewerInventoryItem? = items.first()
                            if (item != null && item.isLinkType) {
                                item = item.linkedItem
                            }
                            if (item != null) {
                                // LLFloaterChangeItemThumbnail will persist this into the folder's
                                // thumbnail_id when opened; set it here so the preview tooltip works
                                p.createParams.setValue("thumbnail_id", item.assetUUID)
                                return LLTextureToolTip(p)
                            }
                        }
                    }
                }
                if (p.message == null || p.message!!.isEmpty()) {
                    null
                } else {
                    LLToolTip(p)
                }
            }
            else -> LLToolTip(p)
        }
    }
}

// ---------------------------------------------------------------------------
// LLTexturePreviewView
// ---------------------------------------------------------------------------

open class LLTexturePreviewView(params: LLView.Params) : LLView(params) {

    private var image: LLViewerFetchedTexture? = null
    private val loadingText: String = LLTrans.getString("texture_loading")

    open fun draw() {
        super.draw()
        val img = image ?: return
        val rctClient = localRect

        if (img.components == 4) {
            TODO("GPU: gl_rect_2d with color (0.098, 0.098, 0.098)")
        }
        TODO("GPU: gl_draw_scaled_image(rctClient, img)")

        val isLoading = !img.isFullyLoaded && img.discardLevel > 0
        if (isLoading) {
            TODO("GPU: LLFontGL render loadingText at (rctClient.left+3, rctClient.top-25)")
        }

        img.setKnownDrawSize(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
    }

    fun setImageFromAssetId(idAsset: UUID) {
        image = LLViewerTextureManager.getFetchedTexture(
            idAsset,
            FTT_DEFAULT,
            MIPMAP_TRUE,
            LLGLTexture.BOOST_THUMBNAIL
        )
        image?.let { img ->
            img.forceToSaveRawImage(0)
            img.setKnownDrawSize(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
            if (!img.isFullyLoaded && !img.hasFetcher) {
                if (img.isInFastCacheList) {
                    img.loadFromFastCache()
                }
                gTextureList.forceImmediateUpdate(img)
            }
        }
    }

    fun setImageFromItemId(idItem: UUID) {
        val pItem = gInventory.getItem(idItem)
        setImageFromAssetId(pItem?.assetUUID ?: UUID_NULL)
    }

    companion object {
        private const val MAX_IMAGE_SIZE = 256
    }
}

// ---------------------------------------------------------------------------
// LLTextureToolTip
// ---------------------------------------------------------------------------

open class LLTextureToolTip(p: LLToolTip.Params) : LLToolTip(p) {

    protected var previewView: LLTexturePreviewView? = null
    protected var previewSize: Int = 256

    init {
        maxWidth = maxOf(maxWidth, previewSize)
        setBorderVisible(true)
    }

    open fun initFromParams(p: LLToolTip.Params) {
        super.initFromParams(p)

        val pPreview = LLView.Params()
        pPreview.name = "texture_preview"
        val rctPreview = LLRect()
        rctPreview.setOriginAndSize(padding, textBox!!.rect.top, previewSize, previewSize)
        pPreview.rect = rctPreview

        val view = LLTexturePreviewView(pPreview)
        previewView = view
        addChild(view)

        val sdTextureParams = p.createParams
        when {
            sdTextureParams.has("thumbnail_id") ->
                view.setImageFromAssetId(sdTextureParams.asUUID("thumbnail_id")!!)
            sdTextureParams.has("item_id") ->
                view.setImageFromItemId(sdTextureParams.asUUID("item_id")!!)
        }

        setBackgroundVisible(true)
        setBackgroundOpaque(true)
        setTransparentImage(null)

        snapToChildren()
    }
}
