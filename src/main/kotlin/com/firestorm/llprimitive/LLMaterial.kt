/**
 * LLMaterial.kt
 * Kotlin port of llmaterial.h / llmaterial.cpp
 *
 * Represents the legacy Blinn-Phong "Materials" system used by Second Life
 * (normal map + specular map).  PBR materials are in GLTFMaterial.kt.
 *
 * Serialize / deserialize uses LLSD key names from the Materials capability
 * (NormMap, SpecMap, …) exactly as defined in the original C++ source.
 *
 * Network codec logic is stubbed with TODO().
 */

package com.firestorm.llprimitive

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLSD

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val MATERIALS_MULTIPLIER = 10000f

// LLSD field names (Materials capability)
private const val FIELD_NORMAL_MAP           = "NormMap"
private const val FIELD_NORMAL_OFFSET_X      = "NormOffsetX"
private const val FIELD_NORMAL_OFFSET_Y      = "NormOffsetY"
private const val FIELD_NORMAL_REPEAT_X      = "NormRepeatX"
private const val FIELD_NORMAL_REPEAT_Y      = "NormRepeatY"
private const val FIELD_NORMAL_ROTATION      = "NormRotation"
private const val FIELD_SPECULAR_MAP         = "SpecMap"
private const val FIELD_SPECULAR_OFFSET_X    = "SpecOffsetX"
private const val FIELD_SPECULAR_OFFSET_Y    = "SpecOffsetY"
private const val FIELD_SPECULAR_REPEAT_X    = "SpecRepeatX"
private const val FIELD_SPECULAR_REPEAT_Y    = "SpecRepeatY"
private const val FIELD_SPECULAR_ROTATION    = "SpecRotation"
private const val FIELD_SPECULAR_COLOR       = "SpecColor"
private const val FIELD_SPECULAR_EXP         = "SpecExp"
private const val FIELD_ENV_INTENSITY        = "EnvIntensity"
private const val FIELD_ALPHA_MASK_CUTOFF    = "AlphaMaskCutoff"
private const val FIELD_DIFFUSE_ALPHA_MODE   = "DiffuseAlphaMode"

// ---------------------------------------------------------------------------
// Diffuse alpha mode
// ---------------------------------------------------------------------------

enum class DiffuseAlphaMode(val code: UByte) {
    NONE(0u),
    BLEND(1u),
    MASK(2u),
    EMISSIVE(3u),
    DEFAULT(4u);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromCode(code: UByte): DiffuseAlphaMode = byCode[code] ?: NONE
        fun fromCode(code: Int): DiffuseAlphaMode   = fromCode(code.toUByte())
    }
}

// ---------------------------------------------------------------------------
// Simple 4-component RGBA color stored as UByte components (0..255).
// Mirrors LLColor4U from the viewer code.
// ---------------------------------------------------------------------------

data class Color4U(
    val r: UByte = 255u,
    val g: UByte = 255u,
    val b: UByte = 255u,
    val a: UByte = 255u,
) {
    fun toIntArray(): IntArray = intArrayOf(r.toInt(), g.toInt(), b.toInt(), a.toInt())

    companion object {
        val WHITE = Color4U(255u, 255u, 255u, 255u)
    }
}

// ---------------------------------------------------------------------------
// LLMaterial
// ---------------------------------------------------------------------------

/**
 * Blinn-Phong material definition, matching `LLMaterial` from the C++ viewer.
 *
 * Values that are packed on the wire as fixed-point integers (offsets, repeats,
 * rotations) are stored here as plain floats; the [MATERIALS_MULTIPLIER] scale
 * factor is applied only during LLSD serialisation so the representation stays
 * human-readable.
 */
data class LLMaterial(
    // Normal map
    var normalId: LLUUID         = LLUUID.NULL,
    var normalOffsetX: Float     = 0f,
    var normalOffsetY: Float     = 0f,
    var normalRepeatX: Float     = 1f,
    var normalRepeatY: Float     = 1f,
    var normalRotation: Float    = 0f,

    // Specular map
    var specularId: LLUUID       = LLUUID.NULL,
    var specularOffsetX: Float   = 0f,
    var specularOffsetY: Float   = 0f,
    var specularRepeatX: Float   = 1f,
    var specularRepeatY: Float   = 1f,
    var specularRotation: Float  = 0f,

    // Specular lighting
    var specularLightColor: Color4U = Color4U.WHITE,
    var specularLightExponent: UByte = DEFAULT_SPECULAR_LIGHT_EXPONENT,

    // Shared
    var environmentIntensity: UByte = DEFAULT_ENV_INTENSITY,
    var diffuseAlphaMode: DiffuseAlphaMode = DiffuseAlphaMode.BLEND,
    var alphaMaskCutoff: UByte  = 0u,
) {
    companion object {
        /** (0.2 * 255) rounded — matches the C++ DEFAULT_SPECULAR_LIGHT_EXPONENT. */
        val DEFAULT_SPECULAR_LIGHT_EXPONENT: UByte = (0.2f * 255f).toInt().toUByte()
        val DEFAULT_ENV_INTENSITY: UByte = 0u

        val NULL = LLMaterial()

        /**
         * Deserialize from LLSD returned by the Materials capability.
         * Missing fields fall back to defaults rather than throwing.
         */
        fun fromLLSD(sd: LLSD): LLMaterial = LLMaterial(
            normalId          = sd.getOrNull(FIELD_NORMAL_MAP)?.asUUID()         ?: LLUUID.NULL,
            normalOffsetX     = (sd.getOrNull(FIELD_NORMAL_OFFSET_X)?.asInt()    ?: 0).toFloat() / MATERIALS_MULTIPLIER,
            normalOffsetY     = (sd.getOrNull(FIELD_NORMAL_OFFSET_Y)?.asInt()    ?: 0).toFloat() / MATERIALS_MULTIPLIER,
            normalRepeatX     = (sd.getOrNull(FIELD_NORMAL_REPEAT_X)?.asInt()    ?: 10000).toFloat() / MATERIALS_MULTIPLIER,
            normalRepeatY     = (sd.getOrNull(FIELD_NORMAL_REPEAT_Y)?.asInt()    ?: 10000).toFloat() / MATERIALS_MULTIPLIER,
            normalRotation    = (sd.getOrNull(FIELD_NORMAL_ROTATION)?.asInt()    ?: 0).toFloat() / MATERIALS_MULTIPLIER,

            specularId        = sd.getOrNull(FIELD_SPECULAR_MAP)?.asUUID()       ?: LLUUID.NULL,
            specularOffsetX   = (sd.getOrNull(FIELD_SPECULAR_OFFSET_X)?.asInt()  ?: 0).toFloat() / MATERIALS_MULTIPLIER,
            specularOffsetY   = (sd.getOrNull(FIELD_SPECULAR_OFFSET_Y)?.asInt()  ?: 0).toFloat() / MATERIALS_MULTIPLIER,
            specularRepeatX   = (sd.getOrNull(FIELD_SPECULAR_REPEAT_X)?.asInt()  ?: 10000).toFloat() / MATERIALS_MULTIPLIER,
            specularRepeatY   = (sd.getOrNull(FIELD_SPECULAR_REPEAT_Y)?.asInt()  ?: 10000).toFloat() / MATERIALS_MULTIPLIER,
            specularRotation  = (sd.getOrNull(FIELD_SPECULAR_ROTATION)?.asInt()  ?: 0).toFloat() / MATERIALS_MULTIPLIER,

            specularLightColor = sd.getOrNull(FIELD_SPECULAR_COLOR)?.let { arr ->
                Color4U(
                    arr[0].asInt().toUByte(),
                    arr[1].asInt().toUByte(),
                    arr[2].asInt().toUByte(),
                    arr[3].asInt().toUByte(),
                )
            } ?: Color4U.WHITE,

            specularLightExponent = sd.getOrNull(FIELD_SPECULAR_EXP)?.asInt()?.toUByte()
                ?: DEFAULT_SPECULAR_LIGHT_EXPONENT,

            environmentIntensity = sd.getOrNull(FIELD_ENV_INTENSITY)?.asInt()?.toUByte()
                ?: DEFAULT_ENV_INTENSITY,

            diffuseAlphaMode = DiffuseAlphaMode.fromCode(
                sd.getOrNull(FIELD_DIFFUSE_ALPHA_MODE)?.asInt() ?: DiffuseAlphaMode.BLEND.code.toInt()
            ),

            alphaMaskCutoff = sd.getOrNull(FIELD_ALPHA_MASK_CUTOFF)?.asInt()?.toUByte() ?: 0u,
        )
    }

    /** True if this material carries no effective data (no normal or specular map set). */
    val isEmpty: Boolean
        get() = normalId == LLUUID.NULL && specularId == LLUUID.NULL

    /** Serialize to LLSD for the Materials capability. */
    fun asLLSD(): LLSD = LLSD.map(
        FIELD_NORMAL_MAP        to LLSD.uuid(normalId),
        FIELD_NORMAL_OFFSET_X   to LLSD.integer((normalOffsetX   * MATERIALS_MULTIPLIER).toInt()),
        FIELD_NORMAL_OFFSET_Y   to LLSD.integer((normalOffsetY   * MATERIALS_MULTIPLIER).toInt()),
        FIELD_NORMAL_REPEAT_X   to LLSD.integer((normalRepeatX   * MATERIALS_MULTIPLIER).toInt()),
        FIELD_NORMAL_REPEAT_Y   to LLSD.integer((normalRepeatY   * MATERIALS_MULTIPLIER).toInt()),
        FIELD_NORMAL_ROTATION   to LLSD.integer((normalRotation  * MATERIALS_MULTIPLIER).toInt()),

        FIELD_SPECULAR_MAP      to LLSD.uuid(specularId),
        FIELD_SPECULAR_OFFSET_X to LLSD.integer((specularOffsetX * MATERIALS_MULTIPLIER).toInt()),
        FIELD_SPECULAR_OFFSET_Y to LLSD.integer((specularOffsetY * MATERIALS_MULTIPLIER).toInt()),
        FIELD_SPECULAR_REPEAT_X to LLSD.integer((specularRepeatX * MATERIALS_MULTIPLIER).toInt()),
        FIELD_SPECULAR_REPEAT_Y to LLSD.integer((specularRepeatY * MATERIALS_MULTIPLIER).toInt()),
        FIELD_SPECULAR_ROTATION to LLSD.integer((specularRotation* MATERIALS_MULTIPLIER).toInt()),

        FIELD_SPECULAR_COLOR    to LLSD.array(
            LLSD.integer(specularLightColor.r.toInt()),
            LLSD.integer(specularLightColor.g.toInt()),
            LLSD.integer(specularLightColor.b.toInt()),
            LLSD.integer(specularLightColor.a.toInt()),
        ),
        FIELD_SPECULAR_EXP      to LLSD.integer(specularLightExponent.toInt()),
        FIELD_ENV_INTENSITY     to LLSD.integer(environmentIntensity.toInt()),
        FIELD_DIFFUSE_ALPHA_MODE to LLSD.integer(diffuseAlphaMode.code.toInt()),
        FIELD_ALPHA_MASK_CUTOFF to LLSD.integer(alphaMaskCutoff.toInt()),
    )

    // Convenience offset/repeat getters and setters

    fun getNormalOffset(offsetX: (Float) -> Unit, offsetY: (Float) -> Unit) {
        offsetX(normalOffsetX); offsetY(normalOffsetY)
    }

    fun setNormalOffset(x: Float, y: Float) { normalOffsetX = x; normalOffsetY = y }

    fun getNormalRepeat(repeatX: (Float) -> Unit, repeatY: (Float) -> Unit) {
        repeatX(normalRepeatX); repeatY(normalRepeatY)
    }

    fun setNormalRepeat(x: Float, y: Float) { normalRepeatX = x; normalRepeatY = y }

    fun getSpecularOffset(offsetX: (Float) -> Unit, offsetY: (Float) -> Unit) {
        offsetX(specularOffsetX); offsetY(specularOffsetY)
    }

    fun setSpecularOffset(x: Float, y: Float) { specularOffsetX = x; specularOffsetY = y }

    fun getSpecularRepeat(repeatX: (Float) -> Unit, repeatY: (Float) -> Unit) {
        repeatX(specularRepeatX); repeatY(specularRepeatY)
    }

    fun setSpecularRepeat(x: Float, y: Float) { specularRepeatX = x; specularRepeatY = y }

    /**
     * Compute a shader mask index for this material.
     * Mirrors `LLMaterial::getShaderMask()` in the C++ source; actual shader
     * selection tables are not yet ported.
     */
    fun getShaderMask(alphaMode: UInt, isAlpha: Boolean): UInt {
        // Two least-significant bits hold the effective diffuse alpha mode.
        var ret = alphaMode
        if (ret == DiffuseAlphaMode.DEFAULT.code.toUInt()) {
            ret = diffuseAlphaMode.code.toUInt()
            if (ret == DiffuseAlphaMode.BLEND.code.toUInt() && !isAlpha) {
                ret = DiffuseAlphaMode.NONE.code.toUInt()
            }
        }

        // Bit 2: specular map present
        if (specularId.notNull()) ret = ret or 0x4u

        // Bit 3: normal map present
        if (normalId.notNull()) ret = ret or 0x8u

        return ret
    }

    /** Returns a UUID-based hash of this material's content. */
    fun getHash(): LLUUID {
        val md5 = java.security.MessageDigest.getInstance("MD5")
        val bb = java.nio.ByteBuffer.allocate(128)
        bb.put(normalId.toBytes())
        bb.putFloat(normalOffsetX)
        bb.putFloat(normalOffsetY)
        bb.putFloat(normalRepeatX)
        bb.putFloat(normalRepeatY)
        bb.putFloat(normalRotation)
        bb.put(specularId.toBytes())
        bb.putFloat(specularOffsetX)
        bb.putFloat(specularOffsetY)
        bb.putFloat(specularRepeatX)
        bb.putFloat(specularRepeatY)
        bb.putFloat(specularRotation)
        bb.put(specularLightColor.r.toByte())
        bb.put(specularLightColor.g.toByte())
        bb.put(specularLightColor.b.toByte())
        bb.put(specularLightColor.a.toByte())
        bb.put(specularLightExponent.toByte())
        bb.put(environmentIntensity.toByte())
        bb.put(diffuseAlphaMode.code.toByte())
        bb.put(alphaMaskCutoff.toByte())
        md5.update(bb.array(), 0, bb.position())
        return LLUUID.fromBytes(md5.digest()) ?: LLUUID.NULL
    }
}
