// Converted from llvosurfacepatch.h / llvosurfacepatch.cpp (Firestorm / Linden Research)
// LGPL-2.1-only — see project root for full license text.

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

/**
 * Viewer object representing a single terrain patch.
 *
 * Mirrors [LLVOSurfacePatch] from llvosurfacepatch.h.
 * Each region surface is divided into a grid of patches; this class owns the
 * drawable and vertex data for one cell.
 *
 * All GPU/geometry calls are stubbed with [TODO].
 */
open class VOSurfacePatch(
    id: LLUUID,
    localId: UInt,
    pCode: UInt,
) : ViewerObject(id, localId, pCode) {

    // ---- key fields ----

    /** Current level-of-detail: stride in grid samples (1 = full resolution). */
    var lodLevel: Int = 1

    /** True when geometry needs to be rebuilt. */
    var isDirty: Boolean = false

    /** True when the underlying composition texture is out of date. */
    var isDirtyTexture: Boolean = false

    /** Reference to the [LLSurfacePatch] data source (untyped to avoid circular deps). */
    var patch: Any? = null           // typed as LLSurfacePatch* in C++

    /** Dirty flag set when the surface patch itself has changed. */
    var dirtiedPatch: Boolean = false

    // ---- stride cache (avoids per-frame recalculation) ----

    private var baseComp: Int = 0
    private var lastNorthStride: Int = 0
    private var lastEastStride: Int = 0
    private var lastStride: Int = 0
    private var lastLength: Int = 0

    // ---- lifecycle ----

    /**
     * Detach from the underlying surface patch and call base markDead.
     * Mirrors [LLVOSurfacePatch::markDead].
     */
    fun markDead() {
        (patch as? AutoCloseable)?.close()   // clearVObj() equivalent
        patch = null
        TODO("GPU: super.markDead()")
    }

    /** Terrain patches are static — idle update not needed. */
    fun isActive(): Boolean = false

    // ---- geometry / LOD ----

    /**
     * Set a fixed apparent angle and pixel area; terrain uses a large constant.
     * Mirrors the trivial [LLVOSurfacePatch::setPixelAreaAndAngle].
     */
    fun setPixelAreaAndAngle() {
        // mAppAngle = 50f; mPixelArea = 500*500f
        TODO("GPU: set fixed mAppAngle=50 / mPixelArea=250000")
    }

    /** Terrain textures are managed by the region surface; no-op here. */
    fun updateTextures() = Unit

    /** Create the RENDER_TYPE_TERRAIN drawable. */
    fun createDrawable(): Boolean {
        TODO("GPU: pipeline.allocDrawable; resolve LLDrawPoolTerrain via getPool(); addFace")
    }

    /** Propagate GL texture state from the surface patch. */
    fun updateGL() {
        TODO("GPU: delegate to mPatchp->updateGL()")
    }

    /**
     * Recompute strides for main, north, and east seam faces.
     *
     * Reads [getRenderStride] / [getNeighborPatch] from the surface patch
     * to determine required vertex/index counts, then stores results in the
     * stride-cache fields.
     */
    fun updateGeometry(): Boolean {
        isDirty = false
        TODO("GPU: resolve renderStride/northStride/eastStride from mPatchp; store in lastStride fields")
    }

    /**
     * Resize face vertex/index counts from cached stride values.
     * Only face index 0 is valid for terrain.
     */
    fun updateFaceSize(idx: Int) {
        require(idx == 0) { "Terrain partition requested invalid face index: $idx" }
        TODO("GPU: call getGeomSizesMain/North/East and facep.setSize()")
    }

    /** LOD is driven by render stride; always returns true. */
    fun updateLOD(): Boolean = true

    // ---- geometry helpers ----

    /**
     * Compute vertex/index counts for the interior (main) patch strip at [stride].
     * Returns (numVertices, numIndices).
     */
    fun getGeomSizesMain(stride: Int): Pair<Int, Int> {
        TODO("compute main quad-strip geometry counts")
    }

    /**
     * Compute vertex/index counts for the north seam strip,
     * stitching [stride] to [northStride].
     */
    fun getGeomSizesNorth(stride: Int, northStride: Int): Pair<Int, Int> {
        TODO("compute north seam geometry counts")
    }

    /**
     * Compute vertex/index counts for the east seam strip,
     * stitching [stride] to [eastStride].
     */
    fun getGeomSizesEast(stride: Int, eastStride: Int): Pair<Int, Int> {
        TODO("compute east seam geometry counts")
    }

    /** Fill main patch vertices/normals/texcoords/indices into the vertex buffer. */
    fun updateMainGeometry(indexOffset: UInt) {
        TODO("GPU: write main terrain quad-strip vertices into face vertex buffer")
    }

    /** Fill north seam vertices into the vertex buffer to stitch LOD boundaries. */
    fun updateNorthGeometry(indexOffset: UInt) {
        TODO("GPU: write north seam vertices into face vertex buffer")
    }

    /** Fill east seam vertices into the vertex buffer to stitch LOD boundaries. */
    fun updateEastGeometry(indexOffset: UInt) {
        TODO("GPU: write east seam vertices into face vertex buffer")
    }

    /** Ray–terrain intersection (delegates to patch height-map in full impl). */
    fun lineSegmentIntersect(start: Vector3, end: Vector3): Boolean {
        TODO("GPU: height-field ray intersection against terrain geometry")
    }

    /** Spatial extents from the underlying surface patch bounds. */
    fun updateSpatialExtents(): Pair<Vector3, Vector3> {
        TODO("GPU: read patch min/max elevation and return world-space AABB")
    }

    /** Mark patch and geometry dirty, schedule rebuild. */
    fun dirtyPatch() {
        dirtiedPatch = true
        isDirty = true
    }

    /** Mark only geometry dirty. */
    fun dirtyGeom() {
        isDirty = true
    }

    // ---- companion (static) ----

    companion object {
        /** Global LOD scaling factor, driven by viewer performance settings. */
        var lodFactor: Float = 1f
    }
}
