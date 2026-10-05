package com.firestorm.newview

import java.util.UUID

open class LLGLTFMaterial {
    enum class AlphaMode { OPAQUE, MASK, BLEND }
    enum class TextureInfo {
        GLTF_TEXTURE_INFO_BASE_COLOR,
        GLTF_TEXTURE_INFO_NORMAL,
        GLTF_TEXTURE_INFO_METALLIC_ROUGHNESS,
        GLTF_TEXTURE_INFO_EMISSIVE;
        companion object { const val GLTF_TEXTURE_INFO_COUNT = 4 }
    }

    val mTextureId: MutableMap<TextureInfo, UUID> = mutableMapOf()
    var mAlphaMode: AlphaMode = AlphaMode.OPAQUE
    var mAlphaCutoff: Float = 0.5f
    var mRoughnessFactor: Float = 1.0f
    var mMetallicFactor: Float = 1.0f
    var mBaseColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var mEmissiveColor: FloatArray = floatArrayOf(0f, 0f, 0f)
    val mTrackingIdToLocalTexture: MutableMap<UUID, UUID> = mutableMapOf()

    open fun applyOverride(override: LLGLTFMaterial) {}
    open fun addTextureEntry(te: LLTextureEntry) {}
    open fun removeTextureEntry(te: LLTextureEntry) {}
    open fun replaceLocalTexture(trackingId: UUID, oldId: UUID, newId: UUID): Boolean = false
    open fun updateTextureTracking() {}
    fun updateLocalTexDataDigest() {}

    fun asJSON(): String = ""
    companion object {
        const val ASSET_VERSION = "1.0"
        const val ASSET_TYPE = "GLTF 2.0"
        fun isAcceptedVersion(v: String): Boolean = v == ASSET_VERSION
    }
}

class LLTextureEntry

open class LLFetchedGLTFMaterial : LLGLTFMaterial() {

    var mBaseColorTexture: LLViewerFetchedTexture? = null
    var mNormalTexture: LLViewerFetchedTexture? = null
    var mMetallicRoughnessTexture: LLViewerFetchedTexture? = null
    var mEmissiveTexture: LLViewerFetchedTexture? = null

    val mTextureEntires: MutableSet<LLTextureEntry> = mutableSetOf()

    var mExpectedFlusTime: Double = 0.0
    var mActive: Boolean = true
    var mFetching: Boolean = false
    var mFetchSuccess: Boolean = false

    private val materialCompleteCallbacks: MutableList<() -> Unit> = mutableListOf()

    fun isFetching(): Boolean = mFetching
    fun isLoaded(): Boolean = !mFetching && mFetchSuccess

    fun onMaterialComplete(materialComplete: () -> Unit) {
        if (!mFetching) {
            materialComplete()
            return
        }
        materialCompleteCallbacks.add(materialComplete)
    }

    override fun addTextureEntry(te: LLTextureEntry) {
        mTextureEntires.add(te)
    }

    override fun removeTextureEntry(te: LLTextureEntry) {
        mTextureEntires.remove(te)
    }

    override fun replaceLocalTexture(trackingId: UUID, oldId: UUID, newId: UUID): Boolean {
        var res = false
        if (mTextureId[TextureInfo.GLTF_TEXTURE_INFO_BASE_COLOR] == oldId) {
            mTextureId[TextureInfo.GLTF_TEXTURE_INFO_BASE_COLOR] = newId
            mBaseColorTexture = fetchTexture(newId)
            res = true
        }
        if (mTextureId[TextureInfo.GLTF_TEXTURE_INFO_NORMAL] == oldId) {
            mTextureId[TextureInfo.GLTF_TEXTURE_INFO_NORMAL] = newId
            mNormalTexture = fetchTexture(newId)
            res = true
        }
        if (mTextureId[TextureInfo.GLTF_TEXTURE_INFO_METALLIC_ROUGHNESS] == oldId) {
            mTextureId[TextureInfo.GLTF_TEXTURE_INFO_METALLIC_ROUGHNESS] = newId
            mMetallicRoughnessTexture = fetchTexture(newId)
            res = true
        }
        if (mTextureId[TextureInfo.GLTF_TEXTURE_INFO_EMISSIVE] == oldId) {
            mTextureId[TextureInfo.GLTF_TEXTURE_INFO_EMISSIVE] = newId
            mEmissiveTexture = fetchTexture(newId)
            res = true
        }
        for (info in TextureInfo.values()) {
            if (mTextureId[info] == newId) res = true
        }
        if (res) {
            mTrackingIdToLocalTexture[trackingId] = newId
        } else {
            mTrackingIdToLocalTexture.remove(trackingId)
        }
        updateLocalTexDataDigest()
        return res
    }

    override fun updateTextureTracking() {
        for ((trackingId, _) in mTrackingIdToLocalTexture) {
            System.err.println("LLFetchedGLTFMaterial: updateTextureTracking not yet implemented")
        }
    }

    fun clearFetchedTextures() {
        mBaseColorTexture = null
        mNormalTexture = null
        mMetallicRoughnessTexture = null
        mEmissiveTexture = null
    }

    fun bind(mediaTex: LLViewerTexture? = null) {
        // no-op
    }

    fun materialBegin() {
        check(!mFetching) { "materialBegin called while already fetching" }
        mFetching = true
    }

    fun materialComplete(success: Boolean) {
        check(mFetching) { "materialComplete called without materialBegin" }
        mFetching = false
        mFetchSuccess = success
        for (cb in materialCompleteCallbacks) cb()
        materialCompleteCallbacks.clear()
    }

    fun assign(rhs: LLFetchedGLTFMaterial) {
        mBaseColorTexture = rhs.mBaseColorTexture
        mNormalTexture = rhs.mNormalTexture
        mMetallicRoughnessTexture = rhs.mMetallicRoughnessTexture
        mEmissiveTexture = rhs.mEmissiveTexture
    }

    companion object {
        val sDefault = LLFetchedGLTFMaterial()

        private fun fetchTexture(id: UUID): LLViewerFetchedTexture? {
            if (id == UUID(0, 0)) return null
            return null
        }
    }
}

open class LLViewerTexture
class LLViewerFetchedTexture : LLViewerTexture()
