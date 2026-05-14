package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val FULLY_LOADED = 0
private const val NOT_LOADED = 99
private const val GLTF_TEXTURE_INFO_COUNT = 4   // base-color, normal, metallic-roughness, emissive

// Indices into the texture info arrays
private const val GLTF_TEXTURE_INFO_BASE_COLOR = 0
private const val GLTF_TEXTURE_INFO_NORMAL = 1
private const val GLTF_TEXTURE_INFO_METALLIC_ROUGHNESS = 2
private const val GLTF_TEXTURE_INFO_EMISSIVE = 3

// ---------------------------------------------------------------------------
// MaterialLoadLevels – per-texture discard levels (lower = better)
// ---------------------------------------------------------------------------

class MaterialLoadLevels {
    val levels: IntArray = IntArray(GLTF_TEXTURE_INFO_COUNT) { NOT_LOADED }

    fun isFullyLoaded(): Boolean = levels.all { it == FULLY_LOADED }

    operator fun get(i: Int): Int = levels[i]
    operator fun set(i: Int, v: Int) { levels[i] = v }

    // Returns true when this set of levels is strictly better (lower) than other
    // in at least one slot and never worse in any slot.
    operator fun compareTo(other: MaterialLoadLevels): Int {
        var less = false
        var greater = false
        for (i in 0 until GLTF_TEXTURE_INFO_COUNT) {
            if (levels[i] < other.levels[i]) less = true
            if (levels[i] > other.levels[i]) greater = true
        }
        return when {
            less && !greater -> -1
            greater && !less -> 1
            else -> 0
        }
    }
}

// ---------------------------------------------------------------------------
// Texture fetch helpers (GPU / platform ops stubbed out)
// ---------------------------------------------------------------------------

private fun fetchTextureForUi(currentTexture: Any?, id: UUID?): Any? {
    if (currentTexture != null || id == null || id == UUID(0, 0)) return currentTexture
    TODO("GPU: fetch viewer texture for UI preview (LLViewerTextureManager::getFetchedTexture)")
}

private fun getTextureLoadLevel(texture: Any?): Int {
    if (texture == null) return FULLY_LOADED
    TODO("GPU: return texture discard level (LLViewerFetchedTexture::getDiscardLevel)")
}

private fun getMaterialLoadLevels(material: LLFetchedGLTFMaterial): MaterialLoadLevels {
    val levels = MaterialLoadLevels()
    for (i in 0 until GLTF_TEXTURE_INFO_COUNT) {
        TODO("GPU: fetch textures and populate load levels from material texture slots")
    }
    @Suppress("UNREACHABLE_CODE")
    return levels
}

private fun isMaterialLoadedEnoughForUi(material: LLFetchedGLTFMaterial): Boolean {
    if (material.isFetching()) return false
    val levels = getMaterialLoadLevels(material)
    for (i in 0 until GLTF_TEXTURE_INFO_COUNT) {
        if (levels[i] == NOT_LOADED) return false
    }
    return true
}

// ---------------------------------------------------------------------------
// LLGLTFPreviewTexture – dynamic texture that renders a GLTF material preview
// ---------------------------------------------------------------------------

class LLGLTFPreviewTexture private constructor(
    private val gltfMaterial: LLFetchedGLTFMaterial,
    val width: Int
) {
    private var shouldRender: Boolean = true
    private val bestLoad: MaterialLoadLevels = MaterialLoadLevels()

    companion object {
        fun create(material: LLFetchedGLTFMaterial): LLGLTFPreviewTexture {
            TODO("GPU: resolve MAX_PREVIEW_WIDTH from LLPipeline and construct preview texture")
        }
    }

    fun needsRender(): Boolean {
        TODO("GPU: check material load levels against bestLoad to determine if re-render is needed")
    }

    fun preRender(clearDepth: Boolean = true) {
        if (!shouldRender) return
        TODO("GPU: bind render target (LLViewerDynamicTexture::preRender)")
    }

    fun render(): Boolean {
        if (!shouldRender) return false
        TODO("GPU: full PBR deferred render of preview sphere into auxiliary render target")
    }

    fun postRender(success: Boolean) {
        if (!shouldRender) return
        shouldRender = false
        TODO("GPU: unbind render target (LLViewerDynamicTexture::postRender)")
    }
}

// ---------------------------------------------------------------------------
// LLGLTFMaterialPreviewMgr
// ---------------------------------------------------------------------------

class LLGLTFMaterialPreviewMgr {

    // Returns null when the material has not yet loaded enough to render.
    // Callers should cache the returned texture for the lifetime of the preview.
    fun getPreview(material: LLFetchedGLTFMaterial?): Any? {
        if (material == null) return null

        // When UIPreviewMaterial is disabled just surface the base-color texture
        val uiPreviewMaterial = false  // reads gSavedSettings UIPreviewMaterial when settings are ported
        if (!uiPreviewMaterial) {
            TODO("GPU: fetch base-color texture via fetchTextureForUi and return it")
        }

        if (!isMaterialLoadedEnoughForUi(material)) return null

        return LLGLTFPreviewTexture.create(material)
    }
}

// Global singleton instance mirroring C++ `gGLTFMaterialPreviewMgr`
val gGLTFMaterialPreviewMgr = LLGLTFMaterialPreviewMgr()
