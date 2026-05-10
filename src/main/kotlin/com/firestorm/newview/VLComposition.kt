/**
 * VLComposition.kt
 * Kotlin conversion of llvlcomposition.h / llvlcomposition.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * Licensed under LGPL v2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*
import kotlin.math.abs

// ---------------------------------------------------------------------------
// TerrainPaintType constants (mirrors TERRAIN_PAINT_TYPE_* defines)
// ---------------------------------------------------------------------------

/** Terrain rendered by blending detail textures according to height + noise. */
const val TERRAIN_PAINT_TYPE_HEIGHTMAP_WITH_NOISE: UInt = 0u
/** Terrain rendered using a PBR paintmap texture. */
const val TERRAIN_PAINT_TYPE_PBR_PAINTMAP: UInt = 1u

// ---------------------------------------------------------------------------
// ModifyRegion interface (mirrors LLModifyRegion pure virtual)
// ---------------------------------------------------------------------------

/**
 * Allows external code to provide per-asset material overrides at render
 * time, without directly modifying the base terrain composition.
 *
 * Mirrors `LLModifyRegion`.
 */
interface ModifyRegion {
    /** Return the GLTF material override for [asset] index (0–3), or null. */
    fun getMaterialOverride(asset: Int): Any?
}

// ---------------------------------------------------------------------------
// TerrainMaterials  (mirrors LLTerrainMaterials)
// ---------------------------------------------------------------------------

/**
 * Holds the four detail textures / PBR materials for one region's terrain,
 * together with a paint-type selector and optional paintmap.
 *
 * This is the base that [VLComposition] extends.
 *
 * GPU/render operations are stubbed with TODO("GPU: …").
 *
 * Mirrors `LLTerrainMaterials`.
 */
open class TerrainMaterials : ModifyRegion {

    companion object {
        /** Number of terrain detail assets (textures or materials). */
        const val ASSET_COUNT: Int = 4
    }

    // -----------------------------------------------------------------------
    // Detail asset IDs  (texture UUIDs, fetched lazily)
    // -----------------------------------------------------------------------

    /** UUIDs of the four detail textures (or PBR material assets). */
    protected val detailTextureIds: Array<LLUUID> =
        Array(ASSET_COUNT) { LLUUID.NULL }

    /**
     * Return the UUID of the detail asset at [asset] index.
     * Mirrors `LLTerrainMaterials::getDetailAssetID()`.
     */
    open fun getDetailAssetID(asset: Int): LLUUID = detailTextureIds[asset]

    /**
     * Set the UUID of the detail asset at [asset] index and queue a GPU fetch.
     * Mirrors `LLTerrainMaterials::setDetailAssetID()`.
     */
    open fun setDetailAssetID(asset: Int, id: LLUUID) {
        detailTextureIds[asset] = id
        // GPU: unboost old texture, start fetch for new id
    }

    // -----------------------------------------------------------------------
    // Material overrides  (used by gLocalTerrainMaterials mechanism)
    // -----------------------------------------------------------------------

    private val materialOverrides: Array<Any?> = arrayOfNulls(ASSET_COUNT)

    override fun getMaterialOverride(asset: Int): Any? = materialOverrides[asset]

    fun setMaterialOverride(asset: Int, matOverride: Any?) {
        materialOverrides[asset] = matOverride
        // GPU: invalidate render material cache
    }

    // -----------------------------------------------------------------------
    // Paint type & paintmap
    // -----------------------------------------------------------------------

    var paintType: UInt = TERRAIN_PAINT_TYPE_HEIGHTMAP_WITH_NOISE

    /** The paintmap texture used when [paintType] is PBR_PAINTMAP. */
    var paintMap: Any? = null  // LLViewerTexture* in C++

    // -----------------------------------------------------------------------
    // Material-readiness helpers (stubs; depend on GPU texture state)
    // -----------------------------------------------------------------------

    /**
     * Apply material overrides from [other] to this composition.
     * Mirrors `LLTerrainMaterials::apply()`.
     */
    fun apply(other: ModifyRegion) {
        for (i in 0 until ASSET_COUNT) {
            setMaterialOverride(i, other.getMaterialOverride(i))
        }
    }

    /**
     * Returns true when all four textures (or materials) are sufficiently
     * loaded for rendering.
     * Mirrors `LLTerrainMaterials::generateMaterials()`.
     */
    open fun generateMaterials(): Boolean {
        TODO("GPU: generateMaterials not yet ported")
    }

    /**
     * Boost GPU priority of all detail textures / materials.
     * Mirrors `LLTerrainMaterials::boost()`.
     */
    fun boost() {
        TODO("GPU: terrain material boost not yet ported")
    }

    /**
     * Determine whether terrain should use legacy textures or PBR materials.
     * Returns [MaterialType.TEXTURE] when textures are ready, [MaterialType.PBR] otherwise.
     * Mirrors `LLTerrainMaterials::getMaterialType()`.
     */
    fun getMaterialType(): MaterialType {
        TODO("GPU: getMaterialType not yet ported")
    }

    enum class MaterialType { TEXTURE, PBR }
}

// ---------------------------------------------------------------------------
// VLComposition  (mirrors LLVLComposition)
// ---------------------------------------------------------------------------

/**
 * Viewer-side terrain composition layer for a region.
 *
 * Blends four detail textures according to terrain height, using per-corner
 * start-height / range pairs to map the continuous height field onto discrete
 * texture indices.
 *
 * Height-to-index mapping (from the C++ comment):
 *   "Heights map into textures as 0–1 = first, 1–2 = second, etc."
 *
 * This class is open so platform-specific or test subclasses can override
 * [generateHeights] / [generateComposition].
 *
 * Mirrors `LLVLComposition` (extends LLTerrainMaterials + LLViewerLayer).
 */
open class VLComposition(
    /** The surface this composition belongs to (typed Any? until LLSurface is ported). */
    var surface: Any? = null,
    /** Width of the height-field grid in samples. */
    val width: UInt = 256u,
    /** Metres per height-field sample. */
    val scale: Float = 1f
) : TerrainMaterials() {

    companion object {
        const val ASSET_COUNT: Int = TerrainMaterials.ASSET_COUNT   // 4

        /**
         * Default detail-texture UUIDs for the four terrain layers.
         * These mirror `LLVLComposition::getDefaultTextures()`.
         */
        val DEFAULT_TEXTURES: Array<LLUUID> = arrayOf(
            LLUUID("89556747-24cb-43ed-920b-47caed15465f"),  // Dirt / terrain1
            LLUUID("6c19f8f4-2cf9-4132-8f4f-a1f4c67a5f17"),  // Grass / terrain2
            LLUUID("e97cf410-8e61-7005-ec06-629eba4cd1fb"),  // Rock / terrain3
            LLUUID("1bf03d56-c8a5-1f4c-a95d-69d3b84deb52")   // Snow / terrain4
        )
    }

    // -----------------------------------------------------------------------
    // Corner-height parameters
    // -----------------------------------------------------------------------

    /**
     * ECorner indices (mirrors the C++ `ECorner` enum).
     */
    object Corner {
        const val SOUTHWEST: Int = 0
        const val SOUTHEAST: Int = 1
        const val NORTHWEST: Int = 2
        const val NORTHEAST: Int = 3
        const val COUNT: Int     = 4
    }

    /**
     * Height at which each corner begins its lowest texture band.
     * Array is indexed by [Corner] constants.
     */
    private val startHeight: FloatArray = FloatArray(Corner.COUNT) { 0f }

    /**
     * The height range of each corner's texture band.
     * Array is indexed by [Corner] constants.
     */
    private val heightRange: FloatArray = FloatArray(Corner.COUNT) { 60f }

    // Texture scale (applied when building minimap tiles)
    var texScaleX: Float = 16f
    var texScaleY: Float = 16f

    // -----------------------------------------------------------------------
    // Parameters-ready flag
    // -----------------------------------------------------------------------

    /** True once start-height / range parameters have been received. */
    var paramsReady: Boolean = false
        private set

    fun setParamsReady() { paramsReady = true }

    // -----------------------------------------------------------------------
    // Asset IDs with default initialisation
    // -----------------------------------------------------------------------

    init {
        DEFAULT_TEXTURES.forEachIndexed { i, uuid -> detailTextureIds[i] = uuid }
    }

    override fun setDetailAssetID(asset: Int, id: LLUUID) {
        super.setDetailAssetID(asset, id)
        // Additional VLComposition-specific bookkeeping (e.g. raw image cache)
        // would go here once the minimap tile pipeline is ported.
    }

    // -----------------------------------------------------------------------
    // Corner-height accessors
    // -----------------------------------------------------------------------

    fun getStartHeight(corner: Int): Float {
        require(corner in 0 until Corner.COUNT) { "Invalid corner index $corner" }
        return startHeight[corner]
    }

    fun getHeightRange(corner: Int): Float {
        require(corner in 0 until Corner.COUNT) { "Invalid corner index $corner" }
        return heightRange[corner]
    }

    fun setStartHeight(corner: Int, value: Float) {
        require(corner in 0 until Corner.COUNT) { "Invalid corner index $corner" }
        startHeight[corner] = value
    }

    fun setHeightRange(corner: Int, value: Float) {
        require(corner in 0 until Corner.COUNT) { "Invalid corner index $corner" }
        heightRange[corner] = value
    }

    // -----------------------------------------------------------------------
    // Height → texture index
    // -----------------------------------------------------------------------

    /**
     * Bilinear interpolation helper.
     * Mirrors the file-local `bilinear()` in llvlcomposition.cpp.
     */
    private fun bilinear(
        v00: Float, v01: Float, v10: Float, v11: Float,
        xFrac: Float, yFrac: Float
    ): Float {
        val invX = 1f - xFrac
        val invY = 1f - yFrac
        return invX * invY * v00 +
               xFrac * invY * v10 +
               invX * yFrac * v01 +
               xFrac * yFrac * v11
    }

    /**
     * Return the detail-texture UUID that best represents the terrain at
     * region-local position ([x], [y]) given the current height composition.
     *
     * The C++ implementation samples the height field, bilinearly interpolates
     * the four corner start/range values, then maps the result to a texture
     * index in [0, ASSET_COUNT).
     *
     * The height-field lookup is stubbed because [surface] (LLSurface) is not
     * yet ported.
     */
    fun getTexture(x: Float, y: Float): LLUUID {
        // Normalised position within the region [0, 1]
        val xNorm = (x / (width.toFloat() * scale)).coerceIn(0f, 1f)
        val yNorm = (y / (width.toFloat() * scale)).coerceIn(0f, 1f)

        // Bilinearly interpolate start height and range from the four corners.
        val startH = bilinear(
            startHeight[Corner.SOUTHWEST], startHeight[Corner.NORTHWEST],
            startHeight[Corner.SOUTHEAST], startHeight[Corner.NORTHEAST],
            xNorm, yNorm
        )
        val rangeH = bilinear(
            heightRange[Corner.SOUTHWEST], heightRange[Corner.NORTHWEST],
            heightRange[Corner.SOUTHEAST], heightRange[Corner.NORTHEAST],
            xNorm, yNorm
        )

        // Obtain the terrain height at this position.
        val terrainHeight: Float = getTerrainHeight(x, y)

        // Map height into [0, ASSET_COUNT) based on start + range.
        val heightFrac = if (abs(rangeH) > 0.001f) {
            ((terrainHeight - startH) / rangeH).coerceIn(0f, ASSET_COUNT.toFloat() - 0.001f)
        } else {
            0f
        }

        val index = heightFrac.toInt().coerceIn(0, ASSET_COUNT - 1)
        return detailTextureIds[index]
    }

    /**
     * Sample the terrain height at region-local position ([x], [y]).
     *
     * In the C++ code this delegates to `LLSurface::resolveHeightRegion()`.
     * Stubbed until the surface layer is ported.
     */
    private fun getTerrainHeight(x: Float, y: Float): Float {
        TODO("GPU/surface: getTerrainHeight not yet ported (surface=$surface, x=$x, y=$y)")
    }

    // -----------------------------------------------------------------------
    // Generation stubs
    // -----------------------------------------------------------------------

    /**
     * Generate / cache the height-field values used by [getTexture].
     * Mirrors `LLVLComposition::generateHeights()`.
     *
     * @param x       Region-local origin X of the area to generate.
     * @param y       Region-local origin Y of the area to generate.
     * @param w       Width in metres of the area.
     * @param h       Height in metres of the area.
     */
    open fun generateHeights(x: Float, y: Float, w: Float, h: Float): Boolean {
        TODO("generateHeights not yet ported")
    }

    /**
     * Build the final composition (mipmap tiles for the minimap).
     * Mirrors `LLVLComposition::generateComposition()`.
     */
    open fun generateComposition(): Boolean {
        TODO("generateComposition not yet ported")
    }

    override fun generateMaterials(): Boolean {
        TODO("GPU: VLComposition.generateMaterials not yet ported")
    }
}
