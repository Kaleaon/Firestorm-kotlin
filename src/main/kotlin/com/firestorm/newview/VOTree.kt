// Converted from llvotree.h / llvotree.cpp (Firestorm / Linden Research)
// LGPL-2.1-only — see project root for full license text.

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

/** Per-species static data loaded from trees.xml. */
data class TreeSpeciesData(
    val textureId: LLUUID,
    val branchLength: Float,
    val droop: Float,
    val twist: Float,
    val branches: Float,
    val depth: UByte,
    val scaleStep: Float,
    val trunkDepth: UByte,
    val leafScale: Float,
    val trunkLength: Float,
    val billboardScale: Float,
    val billboardRatio: Float,
    val trunkAspect: Float,
    val branchAspect: Float,
    val randomLeafRotate: Float,
    val noiseScale: Float,
    val noiseMag: Float,
    val taper: Float,
    val repeatTrunkZ: Float,
    val name: String,
)

/**
 * Viewer object representing a procedural tree.
 *
 * Mirrors [LLVOTree] from llvotree.h.
 * Recursive branch generation and all GPU calls are stubbed with [TODO].
 */
open class VOTree(
    id: LLUUID,
    localId: UInt,
    pCode: UInt,
) : ViewerObject(id, localId, pCode) {

    // ---- per-instance state ----

    var species: UByte = 0u

    // Branch geometry parameters (copied from species table on update)
    var branchLength: Float = 0f
    var trunkLength: Float = 0f
    var droop: Float = 0f
    var twist: Float = 0f
    var branches: Float = 0f
    var depth: UByte = 0u
    var scaleStep: Float = 0f
    var trunkDepth: UByte = 0u
    var trunkLod: UInt = 0u
    var leafScale: Float = 0f
    var billboardScale: Float = 0f
    var billboardRatio: Float = 0f
    var trunkAspect: Float = 0f
    var branchAspect: Float = 0f
    var randomLeafRotate: Float = 0f

    // Wind animation
    var trunkBend: Vector3 = Vector3.ZERO
    var wind: Vector3 = Vector3.ZERO

    // Cached position/rotation to detect need for full mesh rebuild
    private var lastPosition: Vector3 = Vector3.ZERO
    private var lastRotation: Vector3 = Vector3.ZERO   // quaternion, simplified to Vector3 here

    private var frameCount: UInt = 0u

    // ---- public API ----

    /** Compute apparent angle and pixel area from camera. */
    fun setPixelAreaAndAngle() {
        TODO("GPU: compute mAppAngle/mPixelArea from billboard extents and camera distance")
    }

    /** Update species-specific texture state. */
    fun updateTextures() {
        TODO("GPU: update mTreeImagep virtual size stats")
    }

    /**
     * Per-frame idle: select LOD level and request mesh rebuild when position/rotation changes.
     *
     * LOD selection follows [sLODAngles]: lower trunk-LOD index = higher quality.
     */
    fun idleUpdate(time: Double) {
        TODO("GPU: compare app_angle vs sLODAngles, call markRebuild when trunkLOD or position changes")
    }

    /** Allocate drawable with RENDER_TYPE_TREE and one initial face. */
    fun createDrawable(): Boolean {
        TODO("GPU: pipeline.allocDrawable; add face to LLDrawPoolTree")
    }

    /**
     * Rebuild vertex buffer.
     * First builds a reference buffer (leaves + LOD cylinder slices),
     * then calls [updateMesh] to generate the final per-instance mesh.
     */
    fun updateGeometry(): Boolean {
        if (trunkLod >= MAX_LOD_LEVELS.toUInt()) {
            TODO("GPU: clear face vertex buffer, tree not visible at this distance")
        }
        TODO("GPU: allocate mReferenceBuffer with leaf quads + LOD cylinder slices; call updateMesh()")
    }

    /** Generate per-instance mesh from the reference buffer via [genBranchPipeline]. */
    fun updateMesh() {
        TODO("GPU: apply position/rotation/wind-bend matrix; call genBranchPipeline recursively")
    }

    /**
     * Recursive branch generator.
     *
     * @param trunkLod   LOD level (0 = highest quality)
     * @param stopLevel  recursion stop depth
     * @param depth      current recursion depth
     * @param trunkDepth remaining trunk segments
     * @param scale      current branch scale
     */
    fun genBranchPipeline(trunkLod: Int, stopLevel: Int, depth: Int,
                          trunkDepth: Int, scale: Float, twist: Float,
                          droop: Float, branches: Float, alpha: Float) {
        TODO("GPU: appendMesh cylinder, recurse for sub-branches, append leaf quads at tips")
    }

    /** Count vertices and indices that [genBranchPipeline] will emit. */
    fun calcNumVerts(trunkLod: Int, stopLevel: Int, depth: Int,
                     trunkDepth: Int, branches: Float): Pair<UInt, UInt> {
        TODO("compute vert_count / index_count matching genBranchPipeline recursion")
    }

    /** Compute bounding-sphere radius and update drawable. */
    fun updateRadius() {
        TODO("GPU: drawable.setRadius(32f)")
    }

    /** Spatial extents for the tree based on billboard scale and rotation. */
    fun updateSpatialExtents(): Pair<Vector3, Vector3> {
        TODO("GPU: compute newMin/newMax from billboard scale * rotation; setPositionGroup")
    }

    /** Ray–tetrahedron intersection against the tree's bounding box. */
    fun lineSegmentIntersect(start: Vector3, end: Vector3): Boolean {
        TODO("GPU: linesegment_tetrahedron against drawable spatial extents")
    }

    // ---- companion (static) ----

    companion object {
        const val MAX_LOD_LEVELS = 4

        /** Level-of-detail factor controlled by viewer preferences. */
        var treeFactor: Float = 1f

        /** Minimum apparent angle (degrees) to switch LOD; index 3 disables rendering. */
        val lodAngles: FloatArray = floatArrayOf(30f, 20f, 15f, Float.MIN_VALUE)

        /** Number of cylinder slices per LOD level (highest to lowest quality). */
        val lodSlices: IntArray = intArrayOf(10, 5, 4, 3)

        val lodVertexOffset: IntArray = IntArray(MAX_LOD_LEVELS)
        val lodVertexCount:  IntArray = IntArray(MAX_LOD_LEVELS)
        val lodIndexOffset:  IntArray = IntArray(MAX_LOD_LEVELS)
        val lodIndexCount:   IntArray = IntArray(MAX_LOD_LEVELS)

        /** Loaded from trees.xml at startup. Key = species id. */
        val speciesTable: MutableMap<UInt, TreeSpeciesData> = mutableMapOf()
        var maxTreeSpecies: Int = 0

        /** Returns true when [treeFactor] is below the lowest LOD threshold. */
        fun isTreeRenderingStopped(): Boolean =
            treeFactor < lodAngles[MAX_LOD_LEVELS - 1]

        /** Parse trees.xml and populate [speciesTable]. */
        fun initClass() {
            TODO("parse trees.xml into speciesTable")
        }

        /** Release all species data. Call on viewer shutdown. */
        fun cleanupClass() {
            speciesTable.clear()
        }
    }
}
