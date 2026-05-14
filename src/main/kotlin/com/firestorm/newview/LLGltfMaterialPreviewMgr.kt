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
    System.err.println("LLGltfMaterialPreviewMgr: fetchTextureForUi not yet implemented")
    return null
}

private fun getTextureLoadLevel(texture: Any?): Int {
    if (texture == null) return FULLY_LOADED
    System.err.println("LLGltfMaterialPreviewMgr: getTextureLoadLevel not yet implemented")
    return 0
}

private fun getMaterialLoadLevels(material: LLFetchedGLTFMaterial): MaterialLoadLevels {
    val levels = MaterialLoadLevels()
    for (i in 0 until GLTF_TEXTURE_INFO_COUNT) {
        System.err.println("LLGltfMaterialPreviewMgr: getMaterialLoadLevels not yet implemented")
    }
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
            System.err.println("LLGLTFPreviewTexture: create not yet implemented")
            return LLGLTFPreviewTexture(material, 0)
        }
    }

    fun needsRender(): Boolean {
        System.err.println("LLGLTFPreviewTexture: needsRender not yet implemented")
        return false
    }

    fun preRender(clearDepth: Boolean = true) {
        if (!shouldRender) return
        // no-op
    }

    fun render(): Boolean {
        if (!shouldRender) return false
        System.err.println("LLGLTFPreviewTexture: render not yet implemented")
        return false
    }

    fun postRender(success: Boolean) {
        if (!shouldRender) return
        shouldRender = false
        // no-op
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
            System.err.println("LLGLTFMaterialPreviewMgr: getPreview base-color texture fetch not yet implemented")
            return null
        }

        if (!isMaterialLoadedEnoughForUi(material)) return null

        return LLGLTFPreviewTexture.create(material)
    }
}

// Global singleton instance mirroring C++ `gGLTFMaterialPreviewMgr`
val gGLTFMaterialPreviewMgr = LLGLTFMaterialPreviewMgr()
