/**
 * LLVolume.kt
 * Kotlin conversion of llvolume.h / llvolume.cpp (indra/llmath)
 *
 * Core prim geometry generator.  Geometry generation is stubbed with TODO()
 * until the full sweep/extrude algorithm is ported.
 */

package com.firestorm.llprimitive

import com.firestorm.llmath.Vector2
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector4
import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Profile-curve type (low nibble of the profile byte in LLVolumeParams)
// ---------------------------------------------------------------------------
enum class ProfileType(val code: UByte) {
    CIRCLE(0x00u),
    SQUARE(0x01u),
    ISO_TRI(0x02u),
    EQUAL_TRI(0x03u),
    RIGHT_TRI(0x04u),
    CIRCLE_HALF(0x05u);

    companion object {
        fun fromCode(code: UByte): ProfileType =
            entries.firstOrNull { it.code == code } ?: SQUARE
    }
}

// ---------------------------------------------------------------------------
// Path-curve type
// ---------------------------------------------------------------------------
enum class PathType(val code: UByte) {
    LINE(0x10u),
    CIRCLE(0x20u),
    CIRCLE_33(0x21u),   // Gregory Maurer Working33 variant
    CIRCLE2(0x30u),
    TEST(0x40u),
    FLEXIBLE(0x80u);

    companion object {
        fun fromCode(code: UByte): PathType =
            entries.firstOrNull { it.code == code } ?: LINE
    }
}

// ---------------------------------------------------------------------------
// Face-ID bit-flags  (mirrors LL_FACE_* constants)
// ---------------------------------------------------------------------------
object FaceId {
    const val PATH_BEGIN: UShort    = (0x1 shl 0).toUShort()
    const val PATH_END: UShort      = (0x1 shl 1).toUShort()
    const val INNER_SIDE: UShort    = (0x1 shl 2).toUShort()
    const val PROFILE_BEGIN: UShort = (0x1 shl 3).toUShort()
    const val PROFILE_END: UShort   = (0x1 shl 4).toUShort()
    const val OUTER_SIDE_0: UShort  = (0x1 shl 5).toUShort()
    const val OUTER_SIDE_1: UShort  = (0x1 shl 6).toUShort()
    const val OUTER_SIDE_2: UShort  = (0x1 shl 7).toUShort()
    const val OUTER_SIDE_3: UShort  = (0x1 shl 8).toUShort()
}

// ---------------------------------------------------------------------------
// Sculpt-type constants  (mirrors LL_SCULPT_TYPE_*)
// ---------------------------------------------------------------------------
object SculptTypes {
    const val NONE: UByte     = 0u
    const val SPHERE: UByte   = 1u
    const val TORUS: UByte    = 2u
    const val PLANE: UByte    = 3u
    const val CYLINDER: UByte = 4u
    const val MESH: UByte     = 5u
    const val GLTF: UByte     = 6u
    const val MAX: UByte       = GLTF

    const val FLAG_INVERT: UByte = 64u
    const val FLAG_MIRROR: UByte = 128u
    const val TYPE_MASK: UByte   = (SPHERE.toInt() or TORUS.toInt() or PLANE.toInt() or
                                    CYLINDER.toInt() or MESH.toInt() or GLTF.toInt()).toUByte()
}

// ---------------------------------------------------------------------------
// Volume quantisation constants (mirrors CUT_QUANTA etc.)
// ---------------------------------------------------------------------------
object VolumeQuanta {
    const val CUT: Float    = 0.00002f
    const val SCALE: Float  = 0.01f
    const val SHEAR: Float  = 0.01f
    const val TAPER: Float  = 0.01f
    const val REV: Float    = 0.015f
    const val HOLLOW: Float = 0.00002f
}

// ---------------------------------------------------------------------------
// Face – a single renderable surface patch on a volume
// ---------------------------------------------------------------------------
data class Face(
    val id: UShort = 0u,
    val typeMask: UInt = 0u,
    val vertices: MutableList<Vector3> = mutableListOf(),
    val normals: MutableList<Vector3> = mutableListOf(),
    val texCoords: MutableList<Vector2> = mutableListOf(),
    val indices: MutableList<Int> = mutableListOf(),
    /** S-axis range (begin, count) within the volume mesh */
    var beginS: Int = 0,
    var numS: Int = 0,
    /** T-axis range */
    var beginT: Int = 0,
    var numT: Int = 0,
) {
    // Convenience type-mask flags (mirrors LLVolumeFace enum)
    object Mask {
        const val SINGLE: UInt  = 0x0001u
        const val CAP: UInt     = 0x0002u
        const val END: UInt     = 0x0004u
        const val SIDE: UInt    = 0x0008u
        const val INNER: UInt   = 0x0010u
        const val OUTER: UInt   = 0x0020u
        const val HOLLOW: UInt  = 0x0040u
        const val OPEN: UInt    = 0x0080u
        const val FLAT: UInt    = 0x0100u
        const val TOP: UInt     = 0x0200u
        const val BOTTOM: UInt  = 0x0400u
    }
}

// ---------------------------------------------------------------------------
// LLVolume – core prim geometry generator
// ---------------------------------------------------------------------------
open class LLVolume(
    val params: VolumeParams,
    val detail: Float,
    val generateSingleFace: Boolean = false,
    val isUnique: Boolean = false,
) {
    // ------------------------------------------------------------------
    // Companion: global limits
    // ------------------------------------------------------------------
    companion object {
        const val MAX_VOLUME_FACES: Int          = 8
        const val LL_MAX_VOLUME_LOD_BIAS: Int    = 2

        const val MIN_DETAIL_FACES: Int          = 6
        const val MIN_LOD: Int                   = 0
        const val MAX_LOD: Int                   = 3

        const val MIN_PROFILE_WIDTH: Float       = 0.05f
        const val MIN_PATH_WIDTH: Float          = 0.05f

        const val MAX_TRIANGLE_INDICES: Int      = 10_000
        const val SCULPT_MESH_MAX_FACES: Int     = 8

        var numMeshPoints: Int = 0  // mirrors LLVolume::sNumMeshPoints
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------
    val faces: MutableList<Face> = mutableListOf()

    var faceMask: UInt = 0u          // bit-array of which faces exist
    var lodScaleBias: Vector3 = Vector3(1f, 1f, 1f)

    protected var surfaceArea: Float = 0f
    protected var sculptLevel: Int = 0

    protected var isMeshAssetLoaded: Boolean = false
    protected var isMeshAssetUnavailable: Boolean = false

    // ------------------------------------------------------------------
    // Init: trigger geometry generation on construction
    // ------------------------------------------------------------------
    init {
        generate(detail)
    }

    // ------------------------------------------------------------------
    // Geometry generation
    // ------------------------------------------------------------------

    /**
     * Generate all volume faces for the given LOD detail level.
     *
     * In the C++ viewer this sweeps a [LLProfile] along an [LLPath] to
     * produce the face vertex/index data.  Full geometry generation is
     * deferred until the algorithm is ported.
     */
    open fun generate(detail: Float) {
        System.err.println("LLVolume: generate not yet implemented")
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    fun getNumFaces(): Int         = faces.size
    fun getNumVolumeFaces(): Int   = faces.size

    fun getProfileType(): UByte    = params.profileCurve
    fun getPathType(): UByte       = params.pathCurve

    fun getSurfaceArea(): Float    = surfaceArea
    fun getSculptLevel(): Int      = sculptLevel
    fun setSculptLevel(level: Int) { sculptLevel = level }

    fun isConvex(): Boolean {
        // A volume is convex when hollow == 0 and beginS == 0 and endS == 1
        // and beginT == 0 and endT == 1 (simplified rule from C++ isSculpt / isConvex)
        return params.hollow == 0f &&
               params.beginS == 0f && params.endS == 1f &&
               params.beginT == 0f && params.endT == 1f
    }

    fun isMeshSculpt(): Boolean =
        (params.sculptType.toInt() and SculptTypes.TYPE_MASK.toInt()) == SculptTypes.MESH.toInt()

    open fun setMeshAssetLoaded(loaded: Boolean)           { isMeshAssetLoaded = loaded }
    open fun isMeshAssetLoaded(): Boolean                  = isMeshAssetLoaded
    open fun setMeshAssetUnavailable(unavailable: Boolean) { isMeshAssetUnavailable = unavailable }
    open fun isMeshAssetUnavailable(): Boolean             = isMeshAssetUnavailable

    // ------------------------------------------------------------------
    // LOD triangle count helper  (stubbed — mirrors static getLoDTriangleCounts)
    // ------------------------------------------------------------------
    fun getNumTriangles(): Int = faces.sumOf { it.indices.size / 3 }

    // ------------------------------------------------------------------
    // Face-mask generation  (mirrors generateFaceMask / isFaceMaskValid)
    // ------------------------------------------------------------------
    fun generateFaceMask(): UShort {
        var mask: UShort = 0u
        for (face in faces) {
            mask = (mask or face.id).toUShort()
        }
        return mask
    }

    // ------------------------------------------------------------------
    // toString
    // ------------------------------------------------------------------
    override fun toString(): String =
        "LLVolume(profileType=${ProfileType.fromCode(params.profileCurve)}, " +
        "pathType=${PathType.fromCode(params.pathCurve)}, " +
        "detail=$detail, faces=${faces.size})"
}
