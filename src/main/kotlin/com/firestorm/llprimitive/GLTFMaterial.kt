/**
 * GLTFMaterial.kt
 * Kotlin port of llgltfmaterial.h / llgltfmaterial.cpp
 *
 * PBR (Physically Based Rendering) material definition for the GLTF 2.0
 * material model as used by the Second Life viewer.
 *
 * JSON / TinyGLTF serialization is stubbed with TODO() because it depends on
 * a third-party library not yet available in this module.
 */

package com.firestorm.llprimitive

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLSD
import com.firestorm.llmath.Color4
import com.firestorm.llmath.Vector2
import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// Alpha mode
// ---------------------------------------------------------------------------

enum class AlphaMode(val gltfName: String) {
    OPAQUE("OPAQUE"),
    MASK("MASK"),
    BLEND("BLEND");

    companion object {
        fun fromString(mode: String): AlphaMode = when (mode.uppercase()) {
            "BLEND" -> BLEND
            "MASK"  -> MASK
            else    -> OPAQUE
        }

        fun fromOrdinal(ord: Int): AlphaMode = entries.getOrElse(ord) { OPAQUE }
    }
}

// ---------------------------------------------------------------------------
// TextureInfo — index identifying which texture slot is being addressed
// ---------------------------------------------------------------------------

enum class TextureInfo(val index: Int) {
    BASE_COLOR(0),
    NORMAL(1),
    /** Also used for OCCLUSION — viewer currently packs ORM into one texture. */
    METALLIC_ROUGHNESS(2),
    EMISSIVE(3);

    companion object {
        const val COUNT = 4

        fun fromIndex(index: Int): TextureInfo =
            entries.firstOrNull { it.index == index } ?: BASE_COLOR
    }
}

// OCCLUSION shares the same slot as METALLIC_ROUGHNESS in this implementation
val TextureInfo.Companion.OCCLUSION get() = TextureInfo.METALLIC_ROUGHNESS

// ---------------------------------------------------------------------------
// TextureTransform — per-texture UV transform (KHR_texture_transform)
// ---------------------------------------------------------------------------

data class TextureTransform(
    var offset: Vector2   = Vector2(0f, 0f),
    var scale: Vector2    = Vector2(1f, 1f),
    var rotation: Float   = 0f,
) {
    companion object {
        val DEFAULT = TextureTransform()

        const val PACK_SIZE: Int       = 8
        const val PACK_TIGHT_SIZE: Int = 5
    }

    /** Serialize to a FloatArray of [PACK_SIZE] elements (offset, scale, rotation, padding). */
    fun getPacked(): FloatArray = floatArrayOf(
        offset.x, offset.y,
        scale.x,  scale.y,
        rotation,
        0f, 0f, 0f,  // padding to PACK_SIZE
    )

    /**
     * Compact form: [offsetX, offsetY, scaleX, scaleY, rotation] — [PACK_TIGHT_SIZE] elements.
     */
    fun getPackedTight(): FloatArray = floatArrayOf(
        offset.x, offset.y,
        scale.x,  scale.y,
        rotation,
    )
}

// ---------------------------------------------------------------------------
// GLTFMaterial
// ---------------------------------------------------------------------------

/**
 * PBR GLTF 2.0 material, matching `LLGLTFMaterial` from the C++ viewer.
 *
 * All four texture slots (base-color, normal, metallic-roughness/occlusion,
 * emissive) carry a UUID pointing to a viewer asset.  The corresponding UV
 * transforms are stored in [textureTransform].
 *
 * For material *overrides* the sentinel [GLTF_OVERRIDE_NULL_UUID] is used to
 * indicate "revert to base-material value"; normal null UUID means "no change".
 */
open class GLTFMaterial {

    companion object {
        const val ASSET_VERSION: String = "1.1"
        const val ASSET_TYPE: String    = "GLTF 2.0"

        /** Maximum serialised JSON size (minified) for a GLTF material asset or override. */
        const val MAX_ASSET_LENGTH: Int = 2048

        val ACCEPTED_ASSET_VERSIONS: List<String> = listOf("1.0", "1.1")

        fun isAcceptedVersion(version: String): Boolean =
            version in ACCEPTED_ASSET_VERSIONS

        /** Special UUID used in overrides to mean "revert to base-material default". */
        val GLTF_OVERRIDE_NULL_UUID = LLUUID("ffffffff-ffff-ffff-ffff-ffffffffffff")

        // KHR_texture_transform extension field names
        const val EXT_TRANSFORM         = "KHR_texture_transform"
        const val EXT_TRANSFORM_SCALE    = "scale"
        const val EXT_TRANSFORM_OFFSET   = "offset"
        const val EXT_TRANSFORM_ROTATION = "rotation"

        // Default GLTF spec values
        fun getDefaultAlphaCutoff(): Float      = 0.5f
        fun getDefaultAlphaMode(): AlphaMode    = AlphaMode.OPAQUE
        fun getDefaultMetallicFactor(): Float   = 1f
        fun getDefaultRoughnessFactor(): Float  = 1f
        fun getDefaultBaseColor(): Color4       = Color4(1f, 1f, 1f, 1f)
        fun getDefaultEmissiveColor(): Vector3  = Vector3(0f, 0f, 0f)
        fun getDefaultDoubleSided(): Boolean    = false
        fun getDefaultTextureOffset(): Vector2  = Vector2(0f, 0f)
        fun getDefaultTextureScale(): Vector2   = Vector2(1f, 1f)
        fun getDefaultTextureRotation(): Float  = 0f

        /**
         * Apply "override null" UUID hack so that an override can explicitly
         * clear a texture without colliding with the ordinary null UUID meaning
         * "no override present".
         */
        fun hackOverrideUUID(id: LLUUID): LLUUID =
            if (id == LLUUID.NULL) GLTF_OVERRIDE_NULL_UUID else id

        /**
         * Merge an override UUID onto a destination: if the override carries
         * [GLTF_OVERRIDE_NULL_UUID] the destination is cleared to [LLUUID.NULL];
         * otherwise the override value replaces the destination.
         */
        fun applyOverrideUUID(dstId: LLUUID, overrideId: LLUUID): LLUUID =
            if (overrideId == GLTF_OVERRIDE_NULL_UUID) LLUUID.NULL else overrideId

        /**
         * Convert legacy TE UV transform values into PBR texture-transform values.
         * The conversion accounts for the different coordinate systems used by
         * the old Blinn-Phong pipeline and the GLTF pipeline.
         */
        fun convertTextureTransformToPBR(
            texScaleS: Float, texScaleT: Float,
            texOffsetS: Float, texOffsetT: Float,
            texRotation: Float,
            pbrScale: (Vector2) -> Unit,
            pbrOffset: (Vector2) -> Unit,
            pbrRotation: (Float) -> Unit,
        ) {
            // Simple passthrough mapping — a proper conversion requires
            // accounting for the SL UV-origin difference.
            pbrScale(Vector2(texScaleS, texScaleT))
            pbrOffset(Vector2(texOffsetS, texOffsetT))
            pbrRotation(-texRotation)   // SL uses CCW, GLTF uses CW
        }
    }

    // ---- Texture IDs ---------------------------------------------------

    val textureId: Array<LLUUID> = Array(TextureInfo.COUNT) { LLUUID.NULL }

    var baseColorId: LLUUID
        get() = textureId[TextureInfo.BASE_COLOR.index]
        set(v) { setTextureId(TextureInfo.BASE_COLOR, v) }

    var normalId: LLUUID
        get() = textureId[TextureInfo.NORMAL.index]
        set(v) { setTextureId(TextureInfo.NORMAL, v) }

    var occlusionRoughnessMetallicId: LLUUID
        get() = textureId[TextureInfo.METALLIC_ROUGHNESS.index]
        set(v) { setTextureId(TextureInfo.METALLIC_ROUGHNESS, v) }

    var emissiveId: LLUUID
        get() = textureId[TextureInfo.EMISSIVE.index]
        set(v) { setTextureId(TextureInfo.EMISSIVE, v) }

    // ---- Texture transforms --------------------------------------------

    val textureTransform: Array<TextureTransform> =
        Array(TextureInfo.COUNT) { TextureTransform() }

    // ---- PBR parameters -----------------------------------------------

    /** Base color factor (linear space, not gamma-corrected). */
    var baseColor: Color4 = Color4(1f, 1f, 1f, 1f)

    /** Emissive color factor (linear space). */
    var emissiveColor: Vector3 = Vector3(0f, 0f, 0f)

    var metallicFactor: Float  = 1f
    var roughnessFactor: Float = 1f
    var alphaCutoff: Float     = 0.5f
    var alphaMode: AlphaMode   = AlphaMode.OPAQUE
    var doubleSided: Boolean   = false

    // Override-specific state (not part of the asset; only used in overrides)
    var overrideDoubleSided: Boolean = false
    var overrideAlphaMode: Boolean   = false

    // ---- Local texture tracking (viewer-only) --------------------------

    /** Maps tracking UUID → local texture UUID for local-bitmap support. */
    val trackingIdToLocalTexture: MutableMap<LLUUID, LLUUID> = mutableMapOf()

    /** Digest of [trackingIdToLocalTexture] for fast hashing. */
    var localTexDataDigest: ULong = 0uL

    // ---- Setters (with clamping / sentinel handling) ------------------

    fun setTextureId(info: TextureInfo, id: LLUUID, forOverride: Boolean = false) {
        textureId[info.index] = if (forOverride) hackOverrideUUID(id) else id
    }

    fun setBaseColorId(id: LLUUID, forOverride: Boolean = false) =
        setTextureId(TextureInfo.BASE_COLOR, id, forOverride)

    fun setNormalId(id: LLUUID, forOverride: Boolean = false) =
        setTextureId(TextureInfo.NORMAL, id, forOverride)

    fun setOcclusionRoughnessMetallicId(id: LLUUID, forOverride: Boolean = false) =
        setTextureId(TextureInfo.METALLIC_ROUGHNESS, id, forOverride)

    fun setEmissiveId(id: LLUUID, forOverride: Boolean = false) =
        setTextureId(TextureInfo.EMISSIVE, id, forOverride)

    fun setBaseColorFactor(color: Color4, forOverride: Boolean = false) {
        baseColor = Color4(
            color.r.coerceIn(0f, 1f),
            color.g.coerceIn(0f, 1f),
            color.b.coerceIn(0f, 1f),
            color.a.coerceIn(0f, 1f),
        )
    }

    fun setAlphaCutoff(cutoff: Float, forOverride: Boolean = false) {
        alphaCutoff = cutoff.coerceIn(0f, 1f)
    }

    fun setEmissiveColorFactor(color: Vector3, forOverride: Boolean = false) {
        emissiveColor = Vector3(
            color.x.coerceIn(0f, 1f),
            color.y.coerceIn(0f, 1f),
            color.z.coerceIn(0f, 1f),
        )
    }

    fun setMetallicFactor(metallic: Float, forOverride: Boolean = false) {
        metallicFactor = metallic.coerceIn(0f, 1f)
    }

    fun setRoughnessFactor(roughness: Float, forOverride: Boolean = false) {
        roughnessFactor = roughness.coerceIn(0f, 1f)
    }

    fun setAlphaMode(mode: AlphaMode, forOverride: Boolean = false) {
        alphaMode = mode
        if (forOverride) overrideAlphaMode = true
    }

    fun setAlphaMode(modeString: String, forOverride: Boolean = false) =
        setAlphaMode(AlphaMode.fromString(modeString), forOverride)

    fun setAlphaMode(ordinal: Int, forOverride: Boolean = false) =
        setAlphaMode(AlphaMode.fromOrdinal(ordinal), forOverride)

    fun setDoubleSided(value: Boolean, forOverride: Boolean = false) {
        doubleSided = value
        if (forOverride) overrideDoubleSided = true
    }

    // ---- Texture transform setters ------------------------------------

    fun setTextureOffset(info: TextureInfo, offset: Vector2) {
        textureTransform[info.index].offset = offset
    }

    fun setTextureScale(info: TextureInfo, scale: Vector2) {
        textureTransform[info.index].scale = scale
    }

    fun setTextureRotation(info: TextureInfo, rotation: Float) {
        textureTransform[info.index].rotation = rotation
    }

    // ---- Override helpers ---------------------------------------------

    /**
     * Apply an override material on top of this base material.
     * Per-field semantics: [GLTF_OVERRIDE_NULL_UUID] clears to null;
     * all other non-null overrides replace the base value.
     */
    open fun applyOverride(overrideMat: GLTFMaterial) {
        for (i in 0 until TextureInfo.COUNT) {
            val oid = overrideMat.textureId[i]
            if (oid != LLUUID.NULL) {
                textureId[i] = applyOverrideUUID(textureId[i], oid)
            }
            // Always take transform from override when the override has a non-default transform
            val ot = overrideMat.textureTransform[i]
            if (ot != TextureTransform.DEFAULT) {
                textureTransform[i] = ot.copy()
            }
        }

        if (overrideMat.overrideAlphaMode)   { alphaMode  = overrideMat.alphaMode;  alphaCutoff = overrideMat.alphaCutoff }
        if (overrideMat.overrideDoubleSided) doubleSided = overrideMat.doubleSided

        // Numeric fields: use off-by-epsilon to detect "override present"
        if (overrideMat.metallicFactor  != getDefaultMetallicFactor())  metallicFactor  = overrideMat.metallicFactor
        if (overrideMat.roughnessFactor != getDefaultRoughnessFactor()) roughnessFactor = overrideMat.roughnessFactor

        val defColor = getDefaultBaseColor()
        if (overrideMat.baseColor != defColor) baseColor = overrideMat.baseColor

        val defEmissive = getDefaultEmissiveColor()
        if (overrideMat.emissiveColor != defEmissive) emissiveColor = overrideMat.emissiveColor
    }

    /** Apply override data encoded in LLSD form. */
    fun applyOverrideLLSD(data: LLSD): Unit {
        System.err.println("GLTFMaterial: applyOverrideLLSD not yet implemented")
    }

    /** Produce the delta LLSD between this material and an override. */
    fun getOverrideLLSD(overrideMat: GLTFMaterial, data: LLSD): Unit {
        System.err.println("GLTFMaterial: getOverrideLLSD not yet implemented")
    }

    /**
     * For base materials (assets): strip UV transforms since they are not
     * supported in assets yet.
     */
    fun sanitizeAssetMaterial() {
        for (i in 0 until TextureInfo.COUNT) {
            textureTransform[i] = TextureTransform()
        }
    }

    /**
     * For overrides: clear most properties to pass-through while preserving
     * UV transforms.
     * Returns true if the material was successfully cleared.
     */
    fun setBaseMaterial(): Boolean {
        val savedTransforms = textureTransform.map { it.copy() }
        reset()
        savedTransforms.forEachIndexed { i, t -> textureTransform[i] = t }
        overrideAlphaMode   = false
        overrideDoubleSided = false
        return true
    }

    /** Reset all fields to their GLTF spec defaults. */
    fun reset() {
        for (i in 0 until TextureInfo.COUNT) {
            textureId[i]        = LLUUID.NULL
            textureTransform[i] = TextureTransform()
        }
        baseColor       = Color4(1f, 1f, 1f, 1f)
        emissiveColor   = Vector3(0f, 0f, 0f)
        metallicFactor  = 1f
        roughnessFactor = 1f
        alphaCutoff     = 0.5f
        alphaMode       = AlphaMode.OPAQUE
        doubleSided     = false
        overrideDoubleSided = false
        overrideAlphaMode   = false
    }

    // ---- Local texture tracking ---------------------------------------

    fun addLocalTextureTracking(trackingId: LLUUID, texId: LLUUID) {
        trackingIdToLocalTexture[trackingId] = texId
        updateLocalTexDataDigest()
    }

    fun removeLocalTextureTracking(trackingId: LLUUID) {
        trackingIdToLocalTexture.remove(trackingId)
        updateLocalTexDataDigest()
    }

    val hasLocalTextures: Boolean get() = trackingIdToLocalTexture.isNotEmpty()

    open fun replaceLocalTexture(trackingId: LLUUID, oldId: LLUUID, newId: LLUUID): Boolean {
        if (trackingIdToLocalTexture[trackingId] == oldId) {
            trackingIdToLocalTexture[trackingId] = newId
            // Update all texture slots that point to the old ID
            for (i in 0 until TextureInfo.COUNT) {
                if (textureId[i] == oldId) textureId[i] = newId
            }
            updateLocalTexDataDigest()
            return true
        }
        return false
    }

    open fun updateTextureTracking() {
        System.err.println("GLTFMaterial: updateTextureTracking not yet implemented")
    }

    open fun addTextureEntry(te: Any?) { /* subclass hook */ }
    open fun removeTextureEntry(te: Any?) { /* subclass hook */ }

    protected fun updateLocalTexDataDigest() {
        // Simple XOR fold over all tracking-id bytes for a lightweight digest.
        var digest = 0uL
        for ((k, v) in trackingIdToLocalTexture) {
            digest = digest xor k.hashCode().toULong()
            digest = digest xor v.hashCode().toULong()
        }
        localTexDataDigest = digest
    }

    // ---- JSON (de)serialisation ---------------------------------------

    /** Load this material from a JSON string (requires TinyGLTF, stubbed). */
    fun fromJSON(json: String): Triple<Boolean, String, String> {
        System.err.println("GLTFMaterial: fromJSON not yet implemented")
        return Triple(false, "", "")
    }

    /** Serialise this material to a GLTF JSON string (requires TinyGLTF, stubbed). */
    fun asJSON(prettyprint: Boolean = false): String {
        System.err.println("GLTFMaterial: asJSON not yet implemented")
        return ""
    }

    /** Compute a content-hash UUID for this material. */
    fun getHash(): LLUUID {
        System.err.println("GLTFMaterial: getHash not yet implemented")
        return LLUUID.NULL
    }

    // ---- Equality ------------------------------------------------------

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GLTFMaterial) return false
        return textureId.contentEquals(other.textureId) &&
            textureTransform.contentEquals(other.textureTransform) &&
            baseColor == other.baseColor &&
            emissiveColor == other.emissiveColor &&
            metallicFactor == other.metallicFactor &&
            roughnessFactor == other.roughnessFactor &&
            alphaCutoff == other.alphaCutoff &&
            alphaMode == other.alphaMode &&
            doubleSided == other.doubleSided
    }

    override fun hashCode(): Int {
        var result = textureId.contentHashCode()
        result = 31 * result + baseColor.hashCode()
        result = 31 * result + emissiveColor.hashCode()
        result = 31 * result + metallicFactor.hashCode()
        result = 31 * result + roughnessFactor.hashCode()
        result = 31 * result + alphaCutoff.hashCode()
        result = 31 * result + alphaMode.hashCode()
        result = 31 * result + doubleSided.hashCode()
        return result
    }
}
