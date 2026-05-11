package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val LL_LOCAL_TIMER_HEARTBEAT = 3.0f
private const val LL_LOCAL_USE_MIPMAPS = true
private const val LL_LOCAL_DISCARD_LEVEL = 0
private const val LL_LOCAL_SLAM_FOR_DEBUG = true
private const val LL_LOCAL_REPLACE_ON_DEL = true
private const val LL_LOCAL_UPDATE_RETRIES = 5

// ---------------------------------------------------------------------------
// LLLocalBitmap
// ---------------------------------------------------------------------------

class LLLocalBitmap(filename: String) {

    enum class EUpdateType {
        UT_FIRSTUSE,
        UT_REGUPDATE
    }

    private enum class ELinkStatus {
        LS_ON,
        LS_BROKEN
    }

    enum class EExtension {
        ET_IMG_BMP,
        ET_IMG_TGA,
        ET_IMG_JPG,
        ET_IMG_J2C,
        ET_IMG_PNG
    }

    // Wearable type enum mirroring LLWearableType::EType
    enum class WearableType {
        WT_ALPHA, WT_EYES, WT_GLOVES, WT_JACKET, WT_PANTS, WT_SHIRT,
        WT_SHOES, WT_SKIN, WT_SKIRT, WT_SOCKS, WT_TATTOO, WT_UNIVERSAL,
        WT_UNDERPANTS, WT_UNDERSHIRT
    }

    // Baked texture index mirroring LLAvatarAppearanceDefines::EBakedTextureIndex
    enum class BakedTextureIndex {
        BAKED_HEAD, BAKED_UPPER, BAKED_LOWER, BAKED_EYES, BAKED_SKIRT, BAKED_HAIR,
        BAKED_LEFT_ARM, BAKED_LEFT_LEG, BAKED_AUX1, BAKED_AUX2, BAKED_AUX3
    }

    // Texture index mirroring LLAvatarAppearanceDefines::ETextureIndex
    enum class TextureIndex {
        TEX_EYES_ALPHA, TEX_HAIR_ALPHA, TEX_HEAD_ALPHA, TEX_LOWER_ALPHA, TEX_UPPER_ALPHA,
        TEX_EYES_IRIS, TEX_UPPER_GLOVES,
        TEX_LOWER_JACKET, TEX_UPPER_JACKET,
        TEX_LOWER_PANTS, TEX_UPPER_SHIRT, TEX_LOWER_SHOES,
        TEX_HEAD_BODYPAINT, TEX_LOWER_BODYPAINT, TEX_UPPER_BODYPAINT,
        TEX_SKIRT,
        TEX_LOWER_SOCKS,
        TEX_HEAD_TATTOO, TEX_LOWER_TATTOO, TEX_UPPER_TATTOO,
        TEX_SKIRT_TATTOO, TEX_EYES_TATTOO, TEX_HAIR_TATTOO,
        TEX_LEFT_ARM_TATTOO, TEX_LEFT_LEG_TATTOO,
        TEX_AUX1_TATTOO, TEX_AUX2_TATTOO, TEX_AUX3_TATTOO,
        TEX_UPPER_UNIVERSAL_TATTOO, TEX_LOWER_UNIVERSAL_TATTOO, TEX_HEAD_UNIVERSAL_TATTOO,
        TEX_LOWER_UNDERPANTS, TEX_UPPER_UNDERSHIRT,
        TEX_NUM_INDICES
    }

    private val mFilename: String = filename
    private val mShortName: String = filename.substringAfterLast('/').substringBeforeLast('.')
    private val mTrackingID: UUID = UUID.randomUUID()
    private var mWorldID: UUID = UUID(0L, 0L)
    private var mValid: Boolean = false
    private var mLastModified: String = ""
    private val mExtension: EExtension
    private var mLinkStatus: ELinkStatus = ELinkStatus.LS_ON
    private var mUpdateRetries: Int = LL_LOCAL_UPDATE_RETRIES

    private val mChangedCallbacks: MutableList<(UUID, UUID, UUID) -> Unit> = mutableListOf()
    private val mGLTFMaterialWithLocalTextures: MutableList<Any> = mutableListOf()

    init {
        val ext = filename.substringAfterLast('.').toLowerCase()
        mExtension = when (ext) {
            "bmp"        -> EExtension.ET_IMG_BMP
            "tga"        -> EExtension.ET_IMG_TGA
            "jpg", "jpeg"-> EExtension.ET_IMG_JPG
            "j2c", "jp2" -> EExtension.ET_IMG_J2C
            "png"        -> EExtension.ET_IMG_PNG
            else         -> {
                // No valid extension: abort creation
                return@init
            }
        }
        mValid = updateSelf(EUpdateType.UT_FIRSTUSE)
    }

    fun getFilename(): String = mFilename
    fun getShortName(): String = mShortName
    fun getTrackingID(): UUID = mTrackingID
    fun getWorldID(): UUID = mWorldID
    fun getValid(): Boolean = mValid

    fun setChangedCallback(cb: (UUID, UUID, UUID) -> Unit) {
        mChangedCallbacks.add(cb)
    }

    fun addGLTFMaterial(mat: Any?) {
        if (mat == null) return
        TODO("GPU: mat.addLocalTextureTracking(getTrackingID(), getWorldID())")
    }

    fun updateSelf(optionalFirstUpdate: EUpdateType = EUpdateType.UT_REGUPDATE): Boolean {
        var updated = false

        if (mLinkStatus == ELinkStatus.LS_ON) {
            val file = java.io.File(mFilename)
            if (file.exists()) {
                val newLastModified = file.lastModified().toString()

                if (mLastModified != newLastModified) {
                    val decodeOk = decodeBitmap()
                    if (decodeOk) {
                        val oldId = if (optionalFirstUpdate != EUpdateType.UT_FIRSTUSE && mWorldID != UUID(0L, 0L)) {
                            mWorldID
                        } else {
                            UUID(0L, 0L)
                        }
                        mWorldID = UUID.randomUUID()
                        mLastModified = newLastModified

                        TODO("GPU: create GL texture for mWorldID from decoded bitmap at mFilename")

                        if (optionalFirstUpdate != EUpdateType.UT_FIRSTUSE) {
                            replaceIDs(oldId, mWorldID)
                            TODO("GPU: remove oldId from texture list")
                        }

                        mUpdateRetries = LL_LOCAL_UPDATE_RETRIES
                        updated = true
                    } else {
                        if (mUpdateRetries > 0) {
                            mUpdateRetries--
                        } else {
                            mLinkStatus = ELinkStatus.LS_BROKEN
                        }
                    }
                }
            } else {
                mLinkStatus = ELinkStatus.LS_BROKEN
            }
        }

        return updated
    }

    fun destroy() {
        if (LL_LOCAL_REPLACE_ON_DEL && mValid) {
            replaceIDs(mWorldID, UUID(0L, 0L))
            LLLocalBitmapMgr.doRebake()
        }

        for (cb in mChangedCallbacks) {
            cb(getTrackingID(), getWorldID(), UUID(0L, 0L))
        }
        mChangedCallbacks.clear()

        TODO("GPU: remove mWorldID from texture list")
    }

    private fun decodeBitmap(): Boolean {
        return when (mExtension) {
            EExtension.ET_IMG_BMP -> {
                TODO("GPU: decode BMP from mFilename and scale to power-of-two")
            }
            EExtension.ET_IMG_TGA -> {
                TODO("GPU: decode TGA from mFilename (must have 3 or 4 components)")
            }
            EExtension.ET_IMG_JPG -> {
                TODO("GPU: decode JPEG from mFilename")
            }
            EExtension.ET_IMG_J2C -> {
                TODO("GPU: decode J2C from mFilename at discard level 0")
            }
            EExtension.ET_IMG_PNG -> {
                TODO("GPU: decode PNG from mFilename")
            }
        }
    }

    private fun replaceIDs(oldId: UUID, newId: UUID) {
        if (oldId == newId) return

        for (cb in mChangedCallbacks) {
            cb(getTrackingID(), oldId, newId)
        }

        TODO("GPU: updateUserPrims/Volumes/Layers/GLTFMaterials for oldId -> newId")
    }

    private fun getTexIndex(type: WearableType, bakedTexInd: BakedTextureIndex): TextureIndex {
        return when (type) {
            WearableType.WT_ALPHA -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_EYES  -> TextureIndex.TEX_EYES_ALPHA
                BakedTextureIndex.BAKED_HAIR  -> TextureIndex.TEX_HAIR_ALPHA
                BakedTextureIndex.BAKED_HEAD  -> TextureIndex.TEX_HEAD_ALPHA
                BakedTextureIndex.BAKED_LOWER -> TextureIndex.TEX_LOWER_ALPHA
                BakedTextureIndex.BAKED_UPPER -> TextureIndex.TEX_UPPER_ALPHA
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_EYES -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_EYES -> TextureIndex.TEX_EYES_IRIS
                else                         -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_GLOVES -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_UPPER -> TextureIndex.TEX_UPPER_GLOVES
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_JACKET -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_LOWER -> TextureIndex.TEX_LOWER_JACKET
                BakedTextureIndex.BAKED_UPPER -> TextureIndex.TEX_UPPER_JACKET
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_PANTS -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_LOWER -> TextureIndex.TEX_LOWER_PANTS
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_SHIRT -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_UPPER -> TextureIndex.TEX_UPPER_SHIRT
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_SHOES -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_LOWER -> TextureIndex.TEX_LOWER_SHOES
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_SKIN -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_HEAD  -> TextureIndex.TEX_HEAD_BODYPAINT
                BakedTextureIndex.BAKED_LOWER -> TextureIndex.TEX_LOWER_BODYPAINT
                BakedTextureIndex.BAKED_UPPER -> TextureIndex.TEX_UPPER_BODYPAINT
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_SKIRT -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_SKIRT -> TextureIndex.TEX_SKIRT
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_SOCKS -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_LOWER -> TextureIndex.TEX_LOWER_SOCKS
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_TATTOO -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_HEAD  -> TextureIndex.TEX_HEAD_TATTOO
                BakedTextureIndex.BAKED_LOWER -> TextureIndex.TEX_LOWER_TATTOO
                BakedTextureIndex.BAKED_UPPER -> TextureIndex.TEX_UPPER_TATTOO
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_UNIVERSAL -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_SKIRT    -> TextureIndex.TEX_SKIRT_TATTOO
                BakedTextureIndex.BAKED_EYES     -> TextureIndex.TEX_EYES_TATTOO
                BakedTextureIndex.BAKED_HAIR     -> TextureIndex.TEX_HAIR_TATTOO
                BakedTextureIndex.BAKED_LEFT_ARM -> TextureIndex.TEX_LEFT_ARM_TATTOO
                BakedTextureIndex.BAKED_LEFT_LEG -> TextureIndex.TEX_LEFT_LEG_TATTOO
                BakedTextureIndex.BAKED_AUX1     -> TextureIndex.TEX_AUX1_TATTOO
                BakedTextureIndex.BAKED_AUX2     -> TextureIndex.TEX_AUX2_TATTOO
                BakedTextureIndex.BAKED_AUX3     -> TextureIndex.TEX_AUX3_TATTOO
                BakedTextureIndex.BAKED_UPPER    -> TextureIndex.TEX_UPPER_UNIVERSAL_TATTOO
                BakedTextureIndex.BAKED_LOWER    -> TextureIndex.TEX_LOWER_UNIVERSAL_TATTOO
                BakedTextureIndex.BAKED_HEAD     -> TextureIndex.TEX_HEAD_UNIVERSAL_TATTOO
            }
            WearableType.WT_UNDERPANTS -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_LOWER -> TextureIndex.TEX_LOWER_UNDERPANTS
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
            WearableType.WT_UNDERSHIRT -> when (bakedTexInd) {
                BakedTextureIndex.BAKED_UPPER -> TextureIndex.TEX_UPPER_UNDERSHIRT
                else                          -> TextureIndex.TEX_NUM_INDICES
            }
        }
    }
}

// ---------------------------------------------------------------------------
// LLLocalBitmapTimer
// ---------------------------------------------------------------------------

class LLLocalBitmapTimer {

    private var running: Boolean = false

    fun startTimer() {
        running = true
        TODO("APR: use JVM equivalent timer/scheduler to fire every $LL_LOCAL_TIMER_HEARTBEAT seconds")
    }

    fun stopTimer() {
        running = false
        TODO("APR: cancel the JVM timer/scheduler")
    }

    fun isRunning(): Boolean = running

    fun tick(): Boolean {
        LLLocalBitmapMgr.doUpdates()
        return false
    }
}

// ---------------------------------------------------------------------------
// LLLocalBitmapMgr  (singleton → object)
// ---------------------------------------------------------------------------

object LLLocalBitmapMgr {

    private val mBitmapList: MutableList<LLLocalBitmap> = mutableListOf()
    private val mTimer: LLLocalBitmapTimer = LLLocalBitmapTimer()
    private var mNeedsRebake: Boolean = false

    fun addUnit(filenames: MutableList<String>): Boolean {
        var addSuccessful = false
        for (name in filenames) {
            if (name.isNotEmpty() && addUnit(name) != UUID(0L, 0L)) {
                addSuccessful = true
            }
        }
        return addSuccessful
    }

    fun addUnit(filename: String): UUID {
        if (!checkTextureDimensions(filename)) {
            return UUID(0L, 0L)
        }

        val unit = LLLocalBitmap(filename)
        return if (unit.getValid()) {
            mBitmapList.add(unit)
            unit.getTrackingID()
        } else {
            UUID(0L, 0L)
        }
    }

    fun delUnit(trackingId: UUID) {
        val toDelete = mBitmapList.filter { it.getTrackingID() == trackingId }
        for (unit in toDelete) {
            mBitmapList.remove(unit)
            unit.destroy()
        }
    }

    fun checkTextureDimensions(filename: String): Boolean {
        TODO("APR: use JVM equivalent to load image dimensions and compare against max_texture_dimension settings")
    }

    fun getTrackingID(worldId: UUID): UUID {
        return mBitmapList.firstOrNull { it.getWorldID() == worldId }?.getTrackingID()
            ?: UUID(0L, 0L)
    }

    fun getWorldID(trackingId: UUID): UUID {
        return mBitmapList.firstOrNull { it.getTrackingID() == trackingId }?.getWorldID()
            ?: UUID(0L, 0L)
    }

    fun isLocal(worldId: UUID): Boolean {
        return mBitmapList.any { it.getWorldID() == worldId }
    }

    fun getFilename(trackingId: UUID): String {
        return mBitmapList.firstOrNull { it.getTrackingID() == trackingId }?.getFilename() ?: ""
    }

    fun setOnChangedCallback(trackingId: UUID, cb: (UUID, UUID, UUID) -> Unit) {
        mBitmapList.firstOrNull { it.getTrackingID() == trackingId }?.setChangedCallback(cb)
    }

    fun associateGLTFMaterial(trackingId: UUID, mat: Any?) {
        mBitmapList.firstOrNull { it.getTrackingID() == trackingId }?.addGLTFMaterial(mat)
    }

    fun feedScrollList(ctrl: Any?) {
        if (ctrl == null) return
        TODO("GPU: populate scroll list ctrl with bitmap entries (icon + short name + tracking id)")
    }

    fun doUpdates() {
        mTimer.stopTimer()
        mNeedsRebake = false

        for (bitmap in mBitmapList) {
            bitmap.updateSelf()
        }

        doRebake()
        mTimer.startTimer()
    }

    fun setNeedsRebake() {
        mNeedsRebake = true
    }

    fun doRebake() {
        if (mNeedsRebake) {
            TODO("GPU: gAgentAvatarp->forceBakeAllTextures(LL_LOCAL_SLAM_FOR_DEBUG)")
            mNeedsRebake = false
        }
    }
}
