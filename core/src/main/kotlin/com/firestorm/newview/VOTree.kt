package com.firestorm.newview

import kotlin.math.*

data class TreeSpeciesData(
    var textureId: String        = "",
    var branchLength: Float      = 0f,
    var droop: Float             = 0f,
    var twist: Float             = 0f,
    var branches: Float          = 0f,
    var depth: UByte             = 0u,
    var scaleStep: Float         = 0f,
    var trunkDepth: UByte        = 0u,
    var leafScale: Float         = 0f,
    var trunkLength: Float       = 0f,
    var billboardScale: Float    = 0f,
    var billboardRatio: Float    = 0f,
    var trunkAspect: Float       = 0f,
    var branchAspect: Float      = 0f,
    var randomLeafRotate: Float  = 0f,
    var noiseScale: Float        = 0f,
    var noiseMag: Float          = 0f,
    var taper: Float             = 0f,
    var repeatTrunkZ: Float      = 0f,
    var name: String             = ""
)

private const val MAX_SLICES  = 32
private const val LEAF_LEFT   = 0.52f
private const val LEAF_RIGHT  = 0.98f
private const val LEAF_TOP    = 1.0f
private const val LEAF_BOTTOM = 0.52f
private const val LEAF_WIDTH  = 1.0f
private const val LEAF_INDICES  = 24
private const val LEAF_VERTICES = 16

private const val SRR3 = 0.577350269f   // sqrt(1/3)
private const val SRR2 = 0.707106781f   // sqrt(1/2)

open class VOTree {

    companion object {
        const val MAX_NUM_TREE_LOD_LEVELS = 4

        val lodIndexOffset  = IntArray(MAX_NUM_TREE_LOD_LEVELS)
        val lodIndexCount   = IntArray(MAX_NUM_TREE_LOD_LEVELS)
        val lodVertexOffset = IntArray(MAX_NUM_TREE_LOD_LEVELS)
        val lodVertexCount  = IntArray(MAX_NUM_TREE_LOD_LEVELS)
        val lodSlices       = intArrayOf(10, 5, 4, 3)
        val lodAngles       = floatArrayOf(30f, 20f, 15f, Float.MIN_VALUE)

        var treeFactor: Float = 1f
        var maxTreeSpecies: Int = 0

        val speciesTable: MutableMap<UInt, TreeSpeciesData> = mutableMapOf()

        fun isTreeRenderingStopped(): Boolean =
            treeFactor < lodAngles[MAX_NUM_TREE_LOD_LEVELS - 1]

        fun initClass() {
            System.err.println("VOTree: initClass not yet implemented")
        }

        fun cleanupClass() {
            speciesTable.clear()
        }
    }

    protected var trunkBend: Vector3   = Vector3(0f, 0f, 0f)
    protected var wind: Vector3        = Vector3(0f, 0f, 0f)

    // Vertex buffer handles — GPU-side, stubbed.
    protected var referenceBuffer: Any? = null
    protected var treeImagep: Any?      = null

    protected var species: UByte        = 0u
    protected var branchLength: Float   = 0f
    protected var trunkLength: Float    = 0f
    protected var droop: Float          = 0f
    protected var twist: Float          = 0f
    protected var branches: Float       = 0f
    protected var depth: UByte          = 0u
    protected var scaleStep: Float      = 0f
    protected var trunkDepth: UByte     = 0u
    protected var trunkLod: UInt        = 0u
    protected var leafScale: Float      = 0f
    protected var billboardScale: Float = 0f
    protected var billboardRatio: Float = 0f
    protected var trunkAspect: Float    = 0f
    protected var branchAspect: Float   = 0f
    protected var randomLeafRotate: Float = 0f

    protected var lastPosition: Vector3    = Vector3(0f, 0f, 0f)
    protected var lastRotation: FloatArray = FloatArray(4)  // quaternion xyzw

    protected var frameCount: UInt = 0u

    fun processUpdateMessage(): UInt {
        System.err.println("VOTree: processUpdateMessage not yet implemented")
        return 0u
    }

    fun idleUpdate() {
        System.err.println("VOTree: idleUpdate not yet implemented")
    }

    fun render() {}

    fun setPixelAreaAndAngle() {
        // no-op
    }

    fun updateTextures() {
        // no-op
    }

    fun createDrawable() {
        // no-op
    }

    fun updateGeometry(): Boolean {
        if (trunkLod.toInt() >= MAX_NUM_TREE_LOD_LEVELS) {
            referenceBuffer = null
            // no-op
            return true
        }

        buildReferenceBufferIfNeeded()
        updateMesh()
        return true
    }

    private fun buildReferenceBufferIfNeeded() {
        if (referenceBuffer != null) return

        var maxVertices = LEAF_VERTICES
        var maxIndices  = LEAF_INDICES
        for (lod in 0 until MAX_NUM_TREE_LOD_LEVELS) {
            val slices = lodSlices[lod]
            lodVertexOffset[lod] = maxVertices
            lodVertexCount[lod]  = slices * slices
            lodIndexOffset[lod]  = maxIndices
            lodIndexCount[lod]   = (slices - 1) * (slices - 1) * 6
            maxIndices  += lodIndexCount[lod]
            maxVertices += lodVertexCount[lod]
        }

        // no-op
    }

    fun updateMesh() {
        // no-op
    }

    fun appendMesh(
        matrix: FloatArray, normMat: FloatArray,
        vertStart: Int, vertCount: Int, indexCount: Int, indexOffset: Int
    ) {
        // no-op
    }

    fun genBranchPipeline(
        matrix: FloatArray,
        trunkLod: Int, stopLevel: Int,
        depth: UShort, trunkDepth: UShort,
        scale: Float, twist: Float, droop: Float, branches: Float, alpha: Float
    ) {
        val length = if (trunkDepth > 0u || scale == 1f) this.trunkLength else this.branchLength
        val aspect = if (trunkDepth > 0u || scale == 1f) this.trunkAspect else this.branchAspect
        val constantTwist = 360f / branches

        if (stopLevel >= 0 && depth.toInt() > stopLevel) {
            val width = scale * length * aspect
            // no-op
        } else {
            // no-op
        }
    }

    fun calcNumVerts(trunkLod: Int, stopLevel: Int, depth: UShort, trunkDepth: UShort, branches: Float): Pair<UInt, UInt> {
        var verts: UInt = 0u; var indices: UInt = 0u
        if (stopLevel >= 0) {
            if (depth.toInt() > stopLevel) {
                indices += lodIndexCount[trunkLod].toUInt()
                verts   += lodVertexCount[trunkLod].toUInt()
                repeat(branches.toInt()) {
                    val (v, i) = calcNumVerts(trunkLod, stopLevel, (depth - 1u).toUShort(), 0u, branches)
                    verts += v; indices += i
                }
                if (trunkDepth > 0u) {
                    val (v, i) = calcNumVerts(trunkLod, stopLevel, depth, (trunkDepth - 1u).toUShort(), branches)
                    verts += v; indices += i
                }
            } else {
                indices += LEAF_INDICES.toUInt()
                verts   += LEAF_VERTICES.toUInt()
            }
        } else {
            indices += LEAF_INDICES.toUInt()
            verts   += LEAF_VERTICES.toUInt()
        }
        return Pair(verts, indices)
    }

    fun updateRadius() {
        // no-op
    }

    fun updateSpatialExtents() {
        // no-op
    }

    fun lineSegmentIntersect(
        start: FloatArray, end: FloatArray,
        face: Int = -1,
        pickTransparent: Boolean = false,
        pickRigged: Boolean = false,
        pickUnselectable: Boolean = true,
        faceHit: IntArray? = null,
        intersection: FloatArray? = null,
        texCoord: FloatArray? = null,
        normal: FloatArray? = null,
        tangent: FloatArray? = null
    ): Boolean {
        return false
    }

    fun getPartitionType(): UInt {
        System.err.println("VOTree: getPartitionType not yet implemented")
        return 0u
    }

    fun destroyVB() { referenceBuffer = null }
}
