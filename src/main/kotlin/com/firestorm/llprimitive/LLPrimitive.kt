/**
 * LLPrimitive.kt
 * Kotlin port of llprimitive.h / llprimitive.cpp
 *
 * Converted from the Firestorm / Second Life viewer source.
 * Network codec logic is stubbed with TODO().
 */

package com.firestorm.llprimitive

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Color4
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// PCode — raw on-wire primitive byte codes (mirrors LL_PCODE_* values)
// ---------------------------------------------------------------------------

typealias LLPCode = UByte

enum class PCode(val code: UByte) {
    CUBE(1u),
    PRISM(2u),
    TETRAHEDRON(3u),
    PYRAMID(4u),
    CYLINDER(5u),
    CONE(6u),
    SPHERE(7u),
    TORUS(8u),
    VOLUME(9u),
    APP(14u),
    LEGACY(15u),

    // Legacy subtypes — high nibble encodes subtype, low nibble = LEGACY (0x0F)
    LEGACY_AVATAR(0x2Fu),
    LEGACY_GRASS(0x5Fu),
    TREE_NEW(0x6Fu),
    LEGACY_PART_SYS(0x8Fu),
    LEGACY_ROCK(0x9Fu),
    LEGACY_TEXT_BUBBLE(0xEFu),
    LEGACY_TREE(0xFFu),

    // Hemi variants
    CYLINDER_HEMI(0x15u),
    CONE_HEMI(0x16u),
    SPHERE_HEMI(0x17u),
    TORUS_HEMI(0x18u),

    UNKNOWN(0xFEu);

    companion object {
        private val byCode: Map<UByte, PCode> = entries.associateBy { it.code }

        fun fromCode(code: UByte): PCode = byCode[code] ?: UNKNOWN

        /** Legacy (U8) prim types as used in older protocol messages. */
        fun fromLegacy(legacy: UByte): PCode = when (legacy.toInt()) {
            in 1..9 -> fromCode(legacy)
            else    -> UNKNOWN
        }

        fun pCodeToString(pcode: UByte): String =
            fromCode(pcode).name.lowercase().replace('_', ' ')
    }
}

// ---------------------------------------------------------------------------
// Flexible-object constants (EFlexibleObjectConst + related F32 constants)
// ---------------------------------------------------------------------------

object FlexibleObjectDefaults {
    const val MIN_SECTIONS: Int     = 0
    const val DEFAULT_NUM_SECTIONS: Int = 2
    const val MAX_SECTIONS: Int     = 3

    const val MIN_TENSION: Float    = 0f
    const val DEFAULT_TENSION: Float = 1f
    const val MAX_TENSION: Float    = 10f

    const val MIN_AIR_FRICTION: Float    = 0f
    const val DEFAULT_AIR_FRICTION: Float = 2f
    const val MAX_AIR_FRICTION: Float    = 10f

    const val MIN_GRAVITY: Float    = -10f
    const val DEFAULT_GRAVITY: Float = 0.3f
    const val MAX_GRAVITY: Float    = 10f

    const val MIN_WIND_SENSITIVITY: Float    = 0f
    const val DEFAULT_WIND_SENSITIVITY: Float = 0f
    const val MAX_WIND_SENSITIVITY: Float    = 10f

    const val MAX_INTERNAL_TENSION_FORCE: Float = 0.99f
    const val DEFAULT_LENGTH: Float = 1f
    const val DEFAULT_USING_COLLISION_SPHERE: Boolean = false
    const val DEFAULT_RENDERING_COLLISION_SPHERE: Boolean = false
}

// ---------------------------------------------------------------------------
// Light-params constants
// ---------------------------------------------------------------------------

object LightDefaults {
    const val MIN_RADIUS: Float    = 0f
    const val DEFAULT_RADIUS: Float = 5f
    const val MAX_RADIUS: Float    = 20f
    const val MIN_FALLOFF: Float   = 0f
    const val DEFAULT_FALLOFF: Float = 1f
    const val MAX_FALLOFF: Float   = 2f
    const val MIN_CUTOFF: Float    = 0f
    const val DEFAULT_CUTOFF: Float = 0f
    const val MAX_CUTOFF: Float    = 180f
}

// ---------------------------------------------------------------------------
// Reflection-probe constants
// ---------------------------------------------------------------------------

object ReflectionProbeDefaults {
    const val MIN_AMBIANCE: Float   = 0f
    const val MAX_AMBIANCE: Float   = 100f
    const val DEFAULT_AMBIANCE: Float = 0f
    const val MIN_CLIP_DISTANCE: Float   = 0f
    const val MAX_CLIP_DISTANCE: Float   = 1024f
    const val DEFAULT_CLIP_DISTANCE: Float = 0f
}

// ---------------------------------------------------------------------------
// Object shape / cut constants
// ---------------------------------------------------------------------------

object ObjectShapeLimits {
    const val CUT_MIN: Float  = 0f
    const val CUT_MAX: Float  = 1f
    const val CUT_INC: Float  = 0.05f
    const val MIN_CUT_INC: Float = 0.02f
    const val ROTATION_PRECISION: Float = 0.05f

    const val TWIST_MIN: Float  = -360f
    const val TWIST_MAX: Float  = 360f
    const val TWIST_INC: Float  = 18f

    const val TWIST_LINEAR_MIN: Float = -180f
    const val TWIST_LINEAR_MAX: Float = 180f
    const val TWIST_LINEAR_INC: Float = 9f

    const val SL_MAX_HOLLOW_SIZE: Float = 95f
    const val OS_MAX_HOLLOW_SIZE: Float = 99f
    const val SL_MIN_HOLE_SIZE: Float   = 0.05f
    const val OS_MIN_HOLE_SIZE: Float   = 0.01f

    const val MAX_HOLE_SIZE_X: Float = 1f
    const val MAX_HOLE_SIZE_Y: Float = 0.5f

    const val REV_MIN: Float = 1f
    const val REV_MAX: Float = 4f
    const val REV_INC: Float = 0.1f
}

// ---------------------------------------------------------------------------
// Extra network parameter type IDs (LLNetworkData::PARAMS_*)
// ---------------------------------------------------------------------------

object ExtraParams {
    const val FLEXIBLE: UShort         = 0x10u
    const val LIGHT: UShort            = 0x20u
    const val SCULPT: UShort           = 0x30u
    const val LIGHT_IMAGE: UShort      = 0x40u
    const val RESERVED: UShort         = 0x50u
    const val MESH: UShort             = 0x60u
    const val EXTENDED_MESH: UShort    = 0x70u
    const val RENDER_MATERIAL: UShort  = 0x80u
    const val REFLECTION_PROBE: UShort = 0x90u
}

// ---------------------------------------------------------------------------
// Primitive flags
// ---------------------------------------------------------------------------

object PrimFlags {
    const val PHANTOM: UInt            = 0x1u shl 0
    const val VOLUME_DETECT: UInt      = 0x1u shl 1
    const val DYNAMIC: UInt            = 0x1u shl 2
    const val AVATAR: UInt             = 0x1u shl 3
    const val SCULPT: UInt             = 0x1u shl 4
    const val COLLISION_CALLBACK: UInt = 0x1u shl 5
    const val CONVEX: UInt             = 0x1u shl 6
    const val DEFAULT_VOLUME: UInt     = 0x1u shl 7
    const val SITTING: UInt            = 0x1u shl 8
    const val SITTING_ON_GROUND: UInt  = 0x1u shl 9
}

// ---------------------------------------------------------------------------
// Flexible object data (LLFlexibleObjectData)
// ---------------------------------------------------------------------------

data class FlexibleObjectData(
    var simulateLOD: Int   = FlexibleObjectDefaults.DEFAULT_NUM_SECTIONS,
    var gravity: Float     = FlexibleObjectDefaults.DEFAULT_GRAVITY,
    var airFriction: Float = FlexibleObjectDefaults.DEFAULT_AIR_FRICTION,
    var windSensitivity: Float = FlexibleObjectDefaults.DEFAULT_WIND_SENSITIVITY,
    var tension: Float     = FlexibleObjectDefaults.DEFAULT_TENSION,
    var userForce: Vector3 = Vector3.ZERO,
) {
    fun setSimulateLOD(lod: Int) {
        simulateLOD = lod.coerceIn(
            FlexibleObjectDefaults.MIN_SECTIONS,
            FlexibleObjectDefaults.MAX_SECTIONS
        )
    }

    fun setGravity(g: Float) {
        gravity = g.coerceIn(
            FlexibleObjectDefaults.MIN_GRAVITY,
            FlexibleObjectDefaults.MAX_GRAVITY
        )
    }

    fun setAirFriction(f: Float) {
        airFriction = f.coerceIn(
            FlexibleObjectDefaults.MIN_AIR_FRICTION,
            FlexibleObjectDefaults.MAX_AIR_FRICTION
        )
    }

    fun setWindSensitivity(w: Float) {
        windSensitivity = w.coerceIn(
            FlexibleObjectDefaults.MIN_WIND_SENSITIVITY,
            FlexibleObjectDefaults.MAX_WIND_SENSITIVITY
        )
    }

    fun setTension(t: Float) {
        tension = t.coerceIn(
            FlexibleObjectDefaults.MIN_TENSION,
            FlexibleObjectDefaults.MAX_TENSION
        )
    }

    fun pack(): ByteArray { System.err.println("FlexibleObjectData: network pack not yet implemented"); return byteArrayOf() }
    fun unpack(data: ByteArray) { System.err.println("FlexibleObjectData: network unpack not yet implemented") }
}

// ---------------------------------------------------------------------------
// Light params (LLLightParams)
// ---------------------------------------------------------------------------

data class LightParams(
    /** Linear (not gamma-corrected) color; alpha channel = intensity. */
    var linearColor: Color4 = Color4(1f, 1f, 1f, 1f),
    var radius: Float    = LightDefaults.DEFAULT_RADIUS,
    var falloff: Float   = LightDefaults.DEFAULT_FALLOFF,
    var cutoff: Float    = LightDefaults.DEFAULT_CUTOFF,
) {
    fun setRadius(r: Float)  { radius  = r.coerceIn(LightDefaults.MIN_RADIUS,  LightDefaults.MAX_RADIUS) }
    fun setFalloff(f: Float) { falloff = f.coerceIn(LightDefaults.MIN_FALLOFF, LightDefaults.MAX_FALLOFF) }
    fun setCutoff(c: Float)  { cutoff  = c.coerceIn(LightDefaults.MIN_CUTOFF,  LightDefaults.MAX_CUTOFF) }

    fun pack(): ByteArray { System.err.println("LightParams: network pack not yet implemented"); return byteArrayOf() }
    fun unpack(data: ByteArray) { System.err.println("LightParams: network unpack not yet implemented") }
}

// ---------------------------------------------------------------------------
// Reflection probe params (LLReflectionProbeParams)
// ---------------------------------------------------------------------------

data class ReflectionProbeParams(
    var ambiance: Float      = ReflectionProbeDefaults.DEFAULT_AMBIANCE,
    var clipDistance: Float  = ReflectionProbeDefaults.DEFAULT_CLIP_DISTANCE,
    var isBox: Boolean       = false,
    var isDynamic: Boolean   = false,
    var isMirror: Boolean    = false,
) {
    fun setAmbiance(a: Float) {
        ambiance = a.coerceIn(
            ReflectionProbeDefaults.MIN_AMBIANCE,
            ReflectionProbeDefaults.MAX_AMBIANCE
        )
    }

    fun setClipDistance(d: Float) {
        clipDistance = d.coerceIn(
            ReflectionProbeDefaults.MIN_CLIP_DISTANCE,
            ReflectionProbeDefaults.MAX_CLIP_DISTANCE
        )
    }

    fun pack(): ByteArray { System.err.println("ReflectionProbeParams: network pack not yet implemented"); return byteArrayOf() }
    fun unpack(data: ByteArray) { System.err.println("ReflectionProbeParams: network unpack not yet implemented") }
}

// ---------------------------------------------------------------------------
// Sculpt params (LLSculptParams)
// ---------------------------------------------------------------------------

data class SculptParams(
    var sculptTexture: LLUUID = SCULPT_DEFAULT_TEXTURE,
    var sculptType: UByte     = 0u,
) {
    fun pack(): ByteArray { System.err.println("SculptParams: network pack not yet implemented"); return byteArrayOf() }
    fun unpack(data: ByteArray) { System.err.println("SculptParams: network unpack not yet implemented") }

    companion object {
        val SCULPT_DEFAULT_TEXTURE = LLUUID("be293869-d0d9-0a69-5989-ad27f1946fd4")
    }
}

// ---------------------------------------------------------------------------
// Light-image params (LLLightImageParams)
// ---------------------------------------------------------------------------

data class LightImageParams(
    var lightTexture: LLUUID = LLUUID.NULL,
    var params: Vector3      = Vector3.ZERO,
) {
    val isSpotlight: Boolean get() = lightTexture != LLUUID.NULL

    fun pack(): ByteArray { System.err.println("LightImageParams: network pack not yet implemented"); return byteArrayOf() }
    fun unpack(data: ByteArray) { System.err.println("LightImageParams: network unpack not yet implemented") }
}

// ---------------------------------------------------------------------------
// Extended mesh params (LLExtendedMeshParams)
// ---------------------------------------------------------------------------

data class ExtendedMeshParams(
    var flags: UInt = 0u,
) {
    val isAnimatedMeshEnabled: Boolean
        get() = (flags and ANIMATED_MESH_ENABLED_FLAG) != 0u

    fun pack(): ByteArray { System.err.println("ExtendedMeshParams: network pack not yet implemented"); return byteArrayOf() }
    fun unpack(data: ByteArray) { System.err.println("ExtendedMeshParams: network unpack not yet implemented") }

    companion object {
        const val ANIMATED_MESH_ENABLED_FLAG: UInt = 0x1u
    }
}

// ---------------------------------------------------------------------------
// Render material override entry (LLRenderMaterialParams)
// ---------------------------------------------------------------------------

data class RenderMaterialEntry(val teIndex: UByte, val materialId: LLUUID)

class RenderMaterialParams {
    private val entries: MutableList<RenderMaterialEntry> = mutableListOf()

    val isEmpty: Boolean get() = entries.isEmpty()

    fun setMaterial(teIndex: UByte, id: LLUUID) {
        val idx = entries.indexOfFirst { it.teIndex == teIndex }
        if (idx >= 0) {
            entries[idx] = RenderMaterialEntry(teIndex, id)
        } else {
            entries += RenderMaterialEntry(teIndex, id)
        }
    }

    fun getMaterial(teIndex: UByte): LLUUID =
        entries.firstOrNull { it.teIndex == teIndex }?.materialId ?: LLUUID.NULL

    fun pack(): ByteArray { System.err.println("RenderMaterialParams: network pack not yet implemented"); return byteArrayOf() }
    fun unpack(data: ByteArray) { System.err.println("RenderMaterialParams: network unpack not yet implemented") }
}

// ---------------------------------------------------------------------------
// TE contents (LLTEContents) — intermediate representation used during
// texture-entry message parsing
// ---------------------------------------------------------------------------

class TEContents {
    companion object {
        const val MAX_TES: Int       = 45
        const val MAX_TE_BUFFER: Int = 4096
    }

    val imageData:     Array<LLUUID>  = Array(MAX_TES) { LLUUID.NULL }
    val colors:        Array<Color4>  = Array(MAX_TES) { Color4(1f, 1f, 1f, 1f) }
    val scaleS:        FloatArray     = FloatArray(MAX_TES) { 1f }
    val scaleT:        FloatArray     = FloatArray(MAX_TES) { 1f }
    val offsetS:       ShortArray     = ShortArray(MAX_TES)
    val offsetT:       ShortArray     = ShortArray(MAX_TES)
    val imageRot:      ShortArray     = ShortArray(MAX_TES)
    val bump:          UByteArray     = UByteArray(MAX_TES)
    val mediaFlags:    UByteArray     = UByteArray(MAX_TES)
    val glow:          UByteArray     = UByteArray(MAX_TES)
    val packedBuffer:  ByteArray      = ByteArray(MAX_TE_BUFFER)

    var size: Int      = 0
    var faceCount: Int = 0
}

// ---------------------------------------------------------------------------
// LLPrimitive — core prim class
// ---------------------------------------------------------------------------

/**
 * Base class for all primitives in the viewer.
 *
 * Mirrors `LLPrimitive` from llprimitive.h.  Network codec methods are
 * stubbed; call sites should replace the TODO() stubs when implementing
 * the full message layer.
 */
open class LLPrimitive(
    var primitiveCode: UByte       = 0u,
    var position: Vector3          = Vector3.ZERO,
    var rotation: Quaternion       = Quaternion.IDENTITY,
    var scale: Vector3             = Vector3(1f, 1f, 1f),
    var velocity: Vector3          = Vector3.ZERO,
    var angularVelocity: Vector3   = Vector3.ZERO,
    var acceleration: Vector3      = Vector3.ZERO,
    var material: UByte            = MaterialCode.STONE,
    var miscFlags: UInt            = 0u,
) {
    companion object {
        const val MAX_TEs: Int = TEContents.MAX_TES

        /** Default number of simulated flexible-object segments (2^n sections). */
        const val FLEXIBLE_OBJECT_DEFAULT_NUM_SEGS: Int =
            FlexibleObjectDefaults.DEFAULT_NUM_SECTIONS

        const val NO_LOD: Int = -1

        fun pCodeToString(pcode: UByte): String = PCode.pCodeToString(pcode)

        fun isPrimitive(pcode: UByte): Boolean {
            val baseType = pcode and 0x0Fu
            return baseType > 0u && baseType < 14u
        }

        fun isApp(pcode: UByte): Boolean = (pcode and 0x0Fu) == 14u.toUByte()
    }

    // ---- Texture entries -----------------------------------------------

    val textureList: MutableList<TextureEntry> = mutableListOf()

    val numTEs: Int get() = textureList.size

    fun validTE(teNum: UByte): Boolean = teNum.toInt() < textureList.size

    fun getTE(teNum: UByte): TextureEntry? =
        if (validTE(teNum)) textureList[teNum.toInt()] else null

    fun setNumTEs(numTes: UByte) {
        val n = numTes.toInt()
        while (textureList.size < n) textureList.add(TextureEntry())
        while (textureList.size > n) textureList.removeAt(textureList.size - 1)
    }

    fun setAllTETextures(texId: LLUUID) = textureList.forEach { it.textureId = texId }

    fun setTE(index: UByte, te: TextureEntry) {
        if (validTE(index)) textureList[index.toInt()] = te.copy()
    }

    fun setTETexture(te: UByte, texId: LLUUID): Int {
        getTE(te)?.let { it.textureId = texId } ?: return -1
        return 0
    }

    fun setTEColor(te: UByte, color: Color4): Int {
        getTE(te)?.let { it.color = color } ?: return -1
        return 0
    }

    fun setTEScale(te: UByte, s: Float, t: Float): Int {
        getTE(te)?.let { it.scaleS = s; it.scaleT = t } ?: return -1
        return 0
    }

    fun setTEOffset(te: UByte, s: Float, t: Float): Int {
        getTE(te)?.let { it.offsetS = s; it.offsetT = t } ?: return -1
        return 0
    }

    fun setTERotation(te: UByte, r: Float): Int {
        getTE(te)?.let { it.rotation = r } ?: return -1
        return 0
    }

    fun setTEGlow(te: UByte, glow: Float): Int {
        getTE(te)?.let { it.glow = glow } ?: return -1
        return 0
    }

    fun setTEFullbright(te: UByte, fullbright: UByte): Int {
        getTE(te)?.let { it.fullbright = fullbright != 0u.toUByte() } ?: return -1
        return 0
    }

    // ---- Extra parameters (optional attachments) -----------------------

    var flexibleData: FlexibleObjectData?   = null
    var lightParams: LightParams?           = null
    var sculptParams: SculptParams?         = null
    var lightImageParams: LightImageParams? = null
    var extendedMeshParams: ExtendedMeshParams? = null
    var reflectionProbeParams: ReflectionProbeParams? = null
    var renderMaterialParams: RenderMaterialParams?   = null

    // ---- Flags ---------------------------------------------------------

    fun setFlags(flags: UInt) { miscFlags = flags }
    fun addFlags(flags: UInt) { miscFlags = miscFlags or flags }
    fun removeFlags(flags: UInt) { miscFlags = miscFlags and flags.inv() }
    fun checkFlags(flags: UInt): Boolean = (miscFlags and flags) != 0u

    // ---- Identity helpers ----------------------------------------------

    fun isAvatar(): Boolean = primitiveCode == PCode.LEGACY_AVATAR.code

    fun isSittingAvatar(): Boolean =
        isAvatar() && checkFlags(PrimFlags.SITTING or PrimFlags.SITTING_ON_GROUND)

    fun isSittingAvatarOnGround(): Boolean =
        isAvatar() && checkFlags(PrimFlags.SITTING_ON_GROUND)

    // ---- Volume --------------------------------------------------------

    var volumeParams: VolumeParams = VolumeParams()

    /**
     * Apply new volume parameters at the given LOD.
     * Full geometry rebuilding is not implemented here; override in subclasses.
     */
    open fun setVolume(params: VolumeParams, lod: Int, uniqueVolume: Boolean = false): Boolean {
        volumeParams = params.copy()
        return true
    }

    // ---- Network codec stubs ------------------------------------------

    fun packTEMessage(): ByteArray {
        System.err.println("LLPrimitive: packTEMessage network codec not yet implemented")
        return byteArrayOf()
    }

    fun unpackTEMessage(data: ByteArray): Int {
        System.err.println("LLPrimitive: unpackTEMessage network codec not yet implemented")
        return 0
    }

    fun parseTEMessage(data: ByteArray, tec: TEContents): Int {
        System.err.println("LLPrimitive: parseTEMessage network codec not yet implemented")
        return 0
    }

    fun applyParsedTEMessage(tec: TEContents): Int {
        System.err.println("LLPrimitive: applyParsedTEMessage not yet implemented")
        return 0
    }
}
