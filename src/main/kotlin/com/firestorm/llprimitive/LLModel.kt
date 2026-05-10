/**
 * LLModel.kt
 * Kotlin conversion of llmodel.h / llmodel.cpp (indra/llprimitive)
 *
 * Represents a mesh asset with multiple LOD levels, materials, skin weights,
 * and a convex-hull physics decomposition.  Methods that require the full
 * mesh-processing pipeline are stubbed with TODO().
 */

package com.firestorm.llprimitive

import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector2
import com.firestorm.llmath.Matrix4
import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Skin-weight influence for a single joint on a single vertex
// ---------------------------------------------------------------------------
data class JointWeight(
    val jointIndex: Int   = 0,
    val weight: Float     = 0f,
) : Comparable<JointWeight> {
    override fun compareTo(other: JointWeight): Int {
        val cmp = weight.compareTo(other.weight)
        return if (cmp != 0) cmp else jointIndex.compareTo(other.jointIndex)
    }
}

// ---------------------------------------------------------------------------
// Physics mesh (positions + normals for a single convex-hull shard)
// ---------------------------------------------------------------------------
data class PhysicsMesh(
    val positions: MutableList<Vector3> = mutableListOf(),
    val normals: MutableList<Vector3>   = mutableListOf(),
) {
    fun clear() { positions.clear(); normals.clear() }
    fun isEmpty(): Boolean = positions.isEmpty()
}

// ---------------------------------------------------------------------------
// Convex-hull decomposition of the physics shape
// ---------------------------------------------------------------------------
class Decomposition {
    var meshId: LLUUID                              = LLUUID.NULL
    val hull: MutableList<MutableList<Vector3>>     = mutableListOf()   // convex_hull_decomposition
    val baseHull: MutableList<Vector3>              = mutableListOf()   // hull
    val meshes: MutableList<PhysicsMesh>            = mutableListOf()
    var baseHullMesh: PhysicsMesh                   = PhysicsMesh()
    var physicsShapeMesh: PhysicsMesh               = PhysicsMesh()

    fun hasHullList(): Boolean = hull.isNotEmpty()

    fun merge(rhs: Decomposition) {
        hull.addAll(rhs.hull)
        baseHull.addAll(rhs.baseHull)
        meshes.addAll(rhs.meshes)
    }
}

// ---------------------------------------------------------------------------
// Material – maps a named slot to texture asset UUIDs
// ---------------------------------------------------------------------------
data class Material(
    val name: String               = "",
    val diffuseMap: LLUUID         = LLUUID.NULL,
    val normalMap: LLUUID          = LLUUID.NULL,
    val specularMap: LLUUID        = LLUUID.NULL,
)

// ---------------------------------------------------------------------------
// Vertex – a single mesh vertex
// ---------------------------------------------------------------------------
data class Vertex(
    val position: Vector3          = Vector3(0f, 0f, 0f),
    val normal: Vector3            = Vector3(0f, 0f, 0f),
    val texCoord: FloatArray       = FloatArray(2),       // [s, t]
) {
    // FloatArray breaks structural equality; provide explicit equals/hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vertex) return false
        return position == other.position &&
               normal == other.normal &&
               texCoord.contentEquals(other.texCoord)
    }
    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + normal.hashCode()
        result = 31 * result + texCoord.contentHashCode()
        return result
    }
}

// ---------------------------------------------------------------------------
// ModelFace – a single renderable sub-mesh inside LLModel
// ---------------------------------------------------------------------------
data class ModelFace(
    val vertices: MutableList<Vertex> = mutableListOf(),
    val indices: MutableList<Int>     = mutableListOf(),
    var materialName: String          = "",
)

// ---------------------------------------------------------------------------
// Skin info – joint names, bind-pose matrices, and per-vertex weights
// (mirrors LLMeshSkinInfo)
// ---------------------------------------------------------------------------
data class MeshSkinInfo(
    var meshId: LLUUID                          = LLUUID.NULL,
    val jointNames: MutableList<String>         = mutableListOf(),
    val jointNums: MutableList<Int>             = mutableListOf(),
    val invBindMatrix: MutableList<Matrix4>     = mutableListOf(),
    val altBindMatrix: MutableList<Matrix4>     = mutableListOf(),
    val bindPoseMatrix: MutableList<Matrix4>    = mutableListOf(),
    var pelvisOffset: Float                     = 0f,
    var lockScaleIfJointPosition: Boolean       = false,
    var invalidJointsScrubbed: Boolean          = false,
    var jointNumsInitialized: Boolean           = false,
    var hash: ULong                             = 0uL,
)

// ---------------------------------------------------------------------------
// LLModel – mesh-asset representation with LOD and physics decomposition
// ---------------------------------------------------------------------------
class LLModel(
    params: VolumeParams,
    detail: Float,
) : LLVolume(params, detail) {

    // ------------------------------------------------------------------
    // Companion: LOD constants and status strings
    // ------------------------------------------------------------------
    companion object {
        const val MAX_MODEL_FACES: Int = 8

        val LOD_NAMES: Array<String> = arrayOf(
            "lowest_lod", "low_lod", "medium_lod", "high_lod", "physics_mesh"
        )

        fun getStatusString(status: ModelStatus): String = when (status) {
            ModelStatus.NO_ERRORS              -> "status_no_error"
            ModelStatus.VERTEX_NUMBER_OVERFLOW -> "status_vertex_number_overflow"
            ModelStatus.BAD_ELEMENT            -> "bad_element"
            ModelStatus.INVALID_STATUS         -> ""
        }
    }

    // ------------------------------------------------------------------
    // LOD enum  (mirrors the anonymous enum in LLModel)
    // ------------------------------------------------------------------
    enum class LOD(val index: Int) {
        LOD_IMPOSTOR(0),
        LOD_LOW(1),
        LOD_MEDIUM(2),
        LOD_HIGH(3),
        LOD_PHYSICS(4);

        companion object {
            const val NUM_LODS: Int = 5
        }
    }

    // ------------------------------------------------------------------
    // Model-status enum  (mirrors EModelStatus)
    // ------------------------------------------------------------------
    enum class ModelStatus {
        NO_ERRORS,
        VERTEX_NUMBER_OVERFLOW,   // vertex count >= 65535
        BAD_ELEMENT,
        INVALID_STATUS,
    }

    // ------------------------------------------------------------------
    // Write-mode enum  (mirrors EWriteModelMode)
    // ------------------------------------------------------------------
    enum class WriteMode { WRITE_NO, WRITE_BINARY, WRITE_HUMAN }

    // ------------------------------------------------------------------
    // Instance data
    // ------------------------------------------------------------------
    val modelFaces: MutableList<ModelFace>      = mutableListOf()
    val materials: MutableList<Material>        = mutableListOf()
    val materialList: MutableList<String>       = mutableListOf()   // ordered material names

    val positions: MutableList<Vector3>         = mutableListOf()
    /** Map from vertex position to a list of joint influences */
    val skinWeights: MutableMap<Vector3, MutableList<JointWeight>> = mutableMapOf()

    var skinInfo: MeshSkinInfo                  = MeshSkinInfo()
    var physics: Decomposition                  = Decomposition()

    var requestedLabel: String                  = ""
    var label: String                           = ""

    var normalizedScale: Vector3                = Vector3(1f, 1f, 1f)
    var normalizedTranslation: Vector3          = Vector3(0f, 0f, 0f)

    var pelvisOffset: Float                     = 0f
    var decompId: Int                           = -1
    var localId: Int                            = -1
    var submodelId: Int                         = 0

    var status: ModelStatus                     = ModelStatus.NO_ERRORS

    // ------------------------------------------------------------------
    // Face management
    // ------------------------------------------------------------------

    fun clearFacesAndMaterials() {
        modelFaces.clear()
        materialList.clear()
    }

    fun getNumVolumeFaces(): Int = modelFaces.size

    fun setNumVolumeFaces(count: Int) {
        while (modelFaces.size < count) modelFaces.add(ModelFace())
        while (modelFaces.size > count) modelFaces.removeLast()
    }

    /**
     * Sort modelFaces so their order matches mMaterialList.
     */
    fun sortVolumeFacesByMaterialName() {
        TODO("Port LLModel::sortVolumeFacesByMaterialName()")
    }

    /**
     * Translate and scale faces so that all geometry fits in the unit cube,
     * recording the normalizedScale / normalizedTranslation for later retrieval.
     */
    fun normalizeVolumeFaces() {
        TODO("Port LLModel::normalizeVolumeFaces()")
    }

    /**
     * Normalise volume faces and remap skin weights to match the new positions.
     */
    fun normalizeVolumeFacesAndWeights() {
        TODO("Port LLModel::normalizeVolumeFacesAndWeights()")
    }

    /**
     * Drop any faces beyond [newCount], placing the overflow into [remainder].
     */
    fun trimVolumeFacesToSize(
        newCount: Int = LLVolume.SCULPT_MESH_MAX_FACES,
        remainder: MutableList<ModelFace>? = null,
    ) {
        TODO("Port LLModel::trimVolumeFacesToSize()")
    }

    /**
     * Re-index vertices so that duplicate positions share a single index.
     */
    fun remapVolumeFaces() {
        TODO("Port LLModel::remapVolumeFaces()")
    }

    /**
     * Run meshoptimizer over all faces.
     */
    fun optimizeVolumeFaces() {
        TODO("Port LLModel::optimizeVolumeFaces()")
    }

    /**
     * Recompute vertex normals by averaging triangle normals within [angleCutoff].
     */
    fun generateNormals(angleCutoff: Float) {
        TODO("Port LLModel::generateNormals()")
    }

    /**
     * Clamp the model so it satisfies the server's upload constraints.
     * (Mirrors the combined effect of various validation passes in C++.)
     */
    fun limitLOD() {
        TODO("Port LLModel LOD limiting / validation logic.")
    }

    // ------------------------------------------------------------------
    // Skin-weight helpers
    // ------------------------------------------------------------------

    /** Return the joint influences closest to [pos]. */
    fun getJointInfluences(pos: Vector3): MutableList<JointWeight> =
        skinWeights.getOrPut(pos) { mutableListOf() }

    // ------------------------------------------------------------------
    // Geometry override — LLModel does not self-generate; mesh data is
    // loaded from an asset stream.
    // ------------------------------------------------------------------
    override fun generate(detail: Float) {
        // LLModel inherits LLVolume but its geometry comes from loadModel(),
        // not from the sweep algorithm.  Nothing to do here.
    }

    // ------------------------------------------------------------------
    // toString
    // ------------------------------------------------------------------
    override fun toString(): String =
        "LLModel(label='$label', faces=${modelFaces.size}, status=$status)"
}

// ---------------------------------------------------------------------------
// Degenerate-triangle guard  (mirrors ll_is_degenerate)
// ---------------------------------------------------------------------------
fun isDegenerate(
    a: Vector3, b: Vector3, c: Vector3,
    tolerance: Float = 1e-7f,
): Boolean {
    val ab = b - a
    val ac = c - a
    // cross product magnitude squared
    val cx = ab.y * ac.z - ab.z * ac.y
    val cy = ab.z * ac.x - ab.x * ac.z
    val cz = ab.x * ac.y - ab.y * ac.x
    val area2 = cx * cx + cy * cy + cz * cz
    return area2 < tolerance * tolerance
}
