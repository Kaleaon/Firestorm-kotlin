// Converted from llvowlsky.h / llvowlsky.cpp (Firestorm / Linden Research)
// LGPL-2.1-only — see project root for full license text.

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import kotlin.math.*

/**
 * Viewer object that renders the WindLight sky dome, fullscreen sky quad,
 * and star field.
 *
 * Mirrors LLVOWLSky / LLStaticViewerObject in llvowlsky.h.
 * All GPU/vertex-buffer operations are stubbed with [TODO].
 */
open class VOWLSky(
    id: LLUUID,
    localId: UInt,
    pCode: UInt,
) : ViewerObject(id, localId, pCode) {

    // ---- dome geometry configuration ----

    /**
     * Radius (metres) of the sky dome sphere, sourced from the active
     * WindLight environment's sky settings.
     */
    var domeRadius: Float = 15000f

    /**
     * Fractional offset of the dome's bottom edge below the horizon plane.
     * Mirrors the C++ `getDomeOffset()` value.
     */
    var domeOffset: Float = 0.96f

    // ---- GPU-side vertex buffers (opaque handles) ----

    /** Fullscreen quad vertex buffer for advanced atmosphere rendering. */
    private var fsSkyVerts: Any? = null

    /**
     * One vertex buffer per strip segment of the dome.
     * The dome is split into segments to respect per-VBO vertex-count limits.
     */
    private val stripsVerts: MutableList<Any?> = mutableListOf()

    /** Star billboard vertex buffer. */
    private var starsVerts: Any? = null

    // ---- star data (CPU side) ----

    private val starVertices: MutableList<Vector3> = mutableListOf()
    private val starColors: MutableList<Color4> = mutableListOf()
    private val starIntensities: MutableList<Float> = mutableListOf()

    // ---- viewer-object overrides ----

    /** Sky dome is passive; it never requests its own idle updates. */
    fun isActive(): Boolean = false

    /** No per-frame CPU work needed; geometry is rebuilt on environment change. */
    fun idleUpdate(time: Double) { /* intentionally empty */ }

    // ---- drawable / geometry ----

    /**
     * Allocate the sky dome's drawable and register it with the WL sky draw pool.
     */
    fun createDrawable(): Any? {
        TODO("GPU: pipeline.allocDrawable; getPool(POOL_WL_SKY); setRenderType(RENDER_TYPE_WL_SKY)")
    }

    /**
     * Rebuild all vertex buffers (fullscreen quad, dome strips, stars).
     *
     * The dome is tessellated into [getNumStacks] × [getNumSlices] quads
     * using a non-linear phi mapping that concentrates geometry near the apex.
     * Each contiguous strip segment is stored in its own VBO to stay within
     * the configured [RenderMaxVBOSize] limit.
     */
    fun updateGeometry(): Boolean {
        buildFullscreenSkyQuad()
        buildDomeStrips()
        updateStarColors()
        updateStarGeometry()
        TODO("GPU: pipeline.sCompiles++; return true")
    }

    // ---- draw calls ----

    /** Bind the star VBO and issue a draw-arrays call for all star billboards. */
    fun drawStars() {
        if (starsVerts == null) return
        TODO("GPU: starsVerts.setBuffer(); drawArrays(TRIANGLES, 0, getStarsNumVerts()*4)")
    }

    /**
     * Bind and draw each dome strip segment with depth writes disabled so the
     * sky renders behind all opaque geometry.
     */
    fun drawDome() {
        if (stripsVerts.isEmpty()) updateGeometry()
        TODO("GPU: GLDepthTest(true, false); forEach segment: setBuffer; drawRange(TRIANGLES)")
    }

    /**
     * Bind the fullscreen quad and draw it with blending disabled for the
     * advanced atmosphere sky pass.
     */
    fun drawFsSky() {
        if (fsSkyVerts == null) updateGeometry()
        TODO("GPU: GLDisable(BLEND); fsSkyVerts.setBuffer(); drawRange(TRIANGLES)")
    }

    // ---- GL lifecycle ----

    /** Release all vertex buffers before context destruction. */
    fun cleanupGL() {
        stripsVerts.clear()
        starsVerts = null
        fsSkyVerts = null
        TODO("GPU: LLDrawPoolWLSky.cleanupGL()")
    }

    /** Recreate vertex buffers and schedule a full geometry rebuild after context restore. */
    fun restoreGL() {
        TODO("GPU: LLDrawPoolWLSky.restoreGL(); pipeline.markRebuild(drawable, REBUILD_ALL)")
    }

    /** Clear all VBOs and schedule a rebuild (e.g. after sky-detail setting changes). */
    fun resetVertexBuffers() {
        stripsVerts.clear()
        starsVerts = null
        fsSkyVerts = null
        TODO("GPU: pipeline.markRebuild(drawable, REBUILD_ALL)")
    }

    // ---- private geometry helpers ----

    /**
     * Allocate and fill the 4-vertex / 6-index fullscreen quad used by the
     * advanced atmosphere (EEP) sky shader.
     * Vertices span [-1,+1] in both X and Y at Z=0.
     */
    private fun buildFullscreenSkyQuad() {
        TODO("GPU: allocate VBO(ADV_ATMO_SKY_VERTEX_DATA_MASK, 4 verts, 6 indices); fill quad")
    }

    /**
     * Tessellate the sky dome into horizontal strips and upload each strip into
     * its own VBO, splitting at the configured VBO byte-size limit.
     *
     * The phi angle for stack [i] follows a quartic bias curve:
     *   phi = (PI/8) * (1 − (1 − t^4)^2)   where t = i / numStacks
     * This concentrates polygons near the apex while thinning them at the horizon.
     */
    private fun buildDomeStrips() {
        val totalStacks = getNumStacks()
        val numSlices = getNumSlices()
        TODO("GPU: split stacks into segments; per segment buildStripsBuffer(); unmapBuffer()")
    }

    /**
     * Fill one VBO segment of the dome triangle strip.
     * Vertices are placed on a sphere of [domeRadius] at the angles implied by
     * [beginStack]..[endStack] and 0..numSlices.
     */
    private fun buildStripsBuffer(
        beginStack: UInt,
        endStack: UInt,
        radius: Float,
        numSlices: UInt,
        numStacks: UInt,
    ) {
        TODO("GPU: fill vertex/texCoord/index streams for dome strip segment")
    }

    /** Randomise star positions, intensities, and initial colours. */
    private fun initStars() {
        repeat(STARS_NUM_VERTS) {
            val phi = acos(1f - 2f * Math.random().toFloat())
            val theta = (2f * PI * Math.random()).toFloat()
            starVertices.add(
                Vector3(
                    sin(phi) * cos(theta),
                    sin(phi) * sin(theta),
                    cos(phi),
                ) * domeRadius
            )
            starColors.add(Color4(1f, 1f, 1f, 1f))
            starIntensities.add(Math.random().toFloat())
        }
    }

    /** Flicker star alpha values to simulate atmospheric scintillation. */
    private fun updateStarColors() {
        for (i in starColors.indices) {
            val alpha = (starIntensities[i] * (0.5f + 0.5f * Math.random().toFloat())).coerceIn(0f, 1f)
            starColors[i] = starColors[i].copy(a = alpha)
        }
    }

    /** Upload the current star positions and colours to the star VBO. */
    private fun updateStarGeometry() {
        TODO("GPU: allocate/update starsVerts VBO from starVertices and starColors")
    }

    // ---- tessellation parameters ----

    companion object {
        /** Absolute limits on sky detail setting. */
        const val MIN_SKY_DETAIL: UInt = 8u
        const val MAX_SKY_DETAIL: UInt = 180u

        /** Total star billboard count (matches C++ getStarsNumVerts). */
        const val STARS_NUM_VERTS: Int = 1000

        /**
         * Number of horizontal stacks in the dome, clamped to [MIN_SKY_DETAIL]..[MAX_SKY_DETAIL].
         * Reads the "WLSkyDetail" setting.
         */
        fun getNumStacks(skyDetail: UInt = 32u): UInt =
            skyDetail.coerceIn(MIN_SKY_DETAIL, MAX_SKY_DETAIL)

        /**
         * Number of vertical slices = 2 × stacks.
         */
        fun getNumSlices(skyDetail: UInt = 32u): UInt =
            2u * getNumStacks(skyDetail)

        /**
         * Total vertex count for the dome strip geometry
         * (stacks-1 rows × slices-per-row).
         */
        fun getStripsNumVerts(skyDetail: UInt = 32u): UInt {
            val stacks = getNumStacks(skyDetail)
            val slices = getNumSlices(skyDetail)
            return (stacks - 1u) * slices
        }

        /**
         * Total index count for a triangle-strip sky dome.
         * Formula: 2 × ((stacks-2)×(slices+1)) + 1  — matches the C++ inline.
         */
        fun getStripsNumIndices(skyDetail: UInt = 32u): UInt {
            val stacks = getNumStacks(skyDetail)
            val slices = getNumSlices(skyDetail)
            return 2u * ((stacks - 2u) * (slices + 1u)) + 1u
        }

        /**
         * Non-linear phi mapping used to tessellate the dome.
         * Concentrates geometry near the apex (t→0) and spreads it at the horizon (t→1).
         *
         *   phi = (PI/8) × (1 − (1 − t^4)^2)
         */
        fun calcPhi(stackIndex: UInt, numStacks: UInt): Float {
            var t = stackIndex.toFloat() / numStacks.toFloat()
            t = t * t * t * t          // ^4 biases toward apex
            t = 1f - t
            t = t * t
            t = 1f - t
            return (PI / 8.0).toFloat() * t
        }
    }
}
