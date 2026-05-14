package com.firestorm.newview

import kotlin.math.*

class VOSurfacePatch {

    companion object {
        var lodFactor: Float = 1f

        fun initClass() {}
    }

    var patch: SurfacePatch? = null
        set(value) { field = value; if (value != null) dirtyPatch() }

    var isDirtied: Boolean   = false
        private set

    private var pool: Any?   = null     // LLFacePool equivalent
    private var baseComp: Int = 0

    private var dirtyTexture: Boolean  = false
    private var dirtyTerrain: Boolean  = false

    private var lastNorthStride: Int   = 0
    private var lastEastStride: Int    = 0
    private var lastStride: Int        = 0
    private var lastLength: Int        = 0

    // Position is stored in region-local coords; renderer consumes it.
    private var positionRegion: Vector3 = Vector3(0f, 0f, 0f)

    fun setPositionRegion(pos: Vector3) { positionRegion = pos }

    fun isActive(): Boolean = false

    fun setPixelAreaAndAngle() {
        // no-op
    }

    fun updateTextures() {}

    fun getPool(): Any? {
        System.err.println("VOSurfacePatch: getPool not yet implemented")
        return null
    }

    fun createDrawable() {
        // no-op
    }

    fun updateGL() {
        patch?.updateGL()
    }

    fun updateGeometry(): Boolean {
        System.err.println("VOSurfacePatch: updateGeometry not yet implemented")
        return false
    }

    fun updateFaceSize(idx: Int) {
        if (idx != 0) return
        if (lastStride == 0) {
            // no-op
            return
        }
        var numVertices = 0; var numIndices = 0
        getGeomSizesMain(lastStride, numVertices, numIndices).also { (nv, ni) -> numVertices = nv; numIndices = ni }
        getGeomSizesNorth(lastStride, lastNorthStride, numVertices, numIndices).also { (nv, ni) -> numVertices = nv; numIndices = ni }
        getGeomSizesEast(lastStride, lastEastStride, numVertices, numIndices).also { (nv, ni) -> numVertices = nv; numIndices = ni }
        // no-op
    }

    fun updateLOD(): Boolean = true

    fun getTerrainGeometry() {
        // no-op
    }

    fun dirtyPatch() {
        isDirtied    = true
        dirtyTerrain = true
        val p = patch ?: return
        val s = p.surface ?: return
        setPositionRegion(p.centerRegion)
        val scaleFactor = s.getGridsPerPatchEdge() * s.getMetersPerGrid()
        // no-op
    }

    fun dirtyGeom() {
        // no-op
    }

    fun markDead() {
        patch?.clearVObj()
        patch = null
        // no-op
    }

    fun updateSpatialExtents() {
        // no-op
    }

    fun getPartitionType(): UInt {
        System.err.println("VOSurfacePatch: getPartitionType not yet implemented")
        return 0u
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

    // Geometry sizing helpers — pure arithmetic, no GPU calls.

    fun getGeomSizesMain(stride: Int, numVertices: Int, numIndices: Int): Pair<Int, Int> {
        val patchSize = patch?.surface?.getGridsPerPatchEdge() ?: 0
        val vertSize  = patchSize / stride
        return if (vertSize >= 2) {
            Pair(numVertices + vertSize * vertSize, numIndices + 6 * (vertSize - 1) * (vertSize - 1))
        } else Pair(numVertices, numIndices)
    }

    fun getGeomSizesNorth(stride: Int, northStride: Int, numVertices: Int, numIndices: Int): Pair<Int, Int> {
        val patchSize = patch?.surface?.getGridsPerPatchEdge() ?: 0
        var length    = patchSize / stride
        return when {
            northStride == stride -> Pair(numVertices + 2 * length + 1, numIndices + length * 6 - 3)
            northStride > stride  -> Pair(numVertices + length + length / 2 + 1, numIndices + (length / 2) * 9 - 3)
            else -> {
                length = patchSize / northStride
                Pair(numVertices + length + length / 2 + 1, numIndices + 9 * (length / 2) - 3)
            }
        }
    }

    fun getGeomSizesEast(stride: Int, eastStride: Int, numVertices: Int, numIndices: Int): Pair<Int, Int> {
        val patchSize = patch?.surface?.getGridsPerPatchEdge() ?: 0
        var length    = patchSize / stride
        return when {
            eastStride == stride -> Pair(numVertices + 2 * length + 1, numIndices + length * 6 - 3)
            eastStride > stride  -> Pair(numVertices + length + length / 2 + 1, numIndices + (length / 2) * 9 - 3)
            else -> {
                length = patchSize / eastStride
                Pair(numVertices + length + length / 2 + 1, numIndices + 9 * (length / 2) - 3)
            }
        }
    }

    // updateMainGeometry / updateNorthGeometry / updateEastGeometry all operate on GPU-side
    // strider buffers; stubs with detail comments preserved from the C++ logic.
    fun updateMainGeometry(indexOffset: UInt): UInt {
        // no-op
        return 0u
    }

    fun updateNorthGeometry(indexOffset: UInt): UInt {
        // no-op
        return 0u
    }

    fun updateEastGeometry(indexOffset: UInt): UInt {
        // no-op
        return 0u
    }
}
