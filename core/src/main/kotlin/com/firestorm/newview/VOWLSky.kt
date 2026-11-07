package com.firestorm.newview

import kotlin.math.*

private const val MIN_SKY_DETAIL: UInt = 8u
private const val MAX_SKY_DETAIL: UInt = 180u
private const val STARS_NUM_VERTS: Int = 1000

open class VOWLSky(
    id: String,
    pCode: UByte,
    region: ViewerRegion?,
) : StaticViewerObject(id, pCode, region) {

    private var fsSkyVerts: Any? = null
    private val stripsVerts: MutableList<Any?> = mutableListOf()
    private var starsVerts: Any? = null

    private val starVertices: MutableList<FloatArray> = mutableListOf()
    private val starColors: MutableList<FloatArray> = mutableListOf()
    private val starIntensities: MutableList<Float> = mutableListOf()

    init {
        initStars()
    }

    open fun isActive(): Boolean = false

    open fun idleUpdate(time: Double) {}

    open fun createDrawable(pipeline: Any?): Any? {
        // no-op
        return null
    }

    open fun updateGeometry(drawable: Any?): Boolean {
        buildFullscreenSkyQuad()
        buildDomeStrips()
        updateStarColors()
        updateStarGeometry(drawable)
        return false
    }

    fun drawStars() {
        if (starsVerts == null) return
        // no-op
    }

    fun drawDome() {
        if (stripsVerts.isEmpty()) updateGeometry(null)
        // no-op
    }

    fun drawFsSky() {
        if (fsSkyVerts == null) updateGeometry(null)
        // no-op
    }

    fun resetVertexBuffers() {
        stripsVerts.clear()
        starsVerts = null
        fsSkyVerts = null
        // no-op
    }

    fun cleanupGL() {
        stripsVerts.clear()
        starsVerts = null
        fsSkyVerts = null
        // no-op
    }

    fun restoreGL() {
        // no-op
    }

    private fun buildFullscreenSkyQuad() {
        // no-op
    }

    private fun buildDomeStrips() {
        val totalStacks = numStacks()
        val slices = numSlices()
        // no-op
    }

    private fun buildStripsBuffer(
        beginStack: UInt,
        endStack: UInt,
        domeRadius: Float,
        numSlices: UInt,
        numStacks: UInt,
    ) {
        val reciprocalNumStacks = 1f / numStacks.toFloat()
        // Vertices: for each stack row and each slice column, place a point on the sphere.
        // UVs: planar mapping with x/z transposed so the sky animates east-northward.
        // Indices: degenerate-triangle-strip connecting consecutive rows.
        // no-op
    }

    private fun initStars() {
        repeat(STARS_NUM_VERTS) {
            val x = Math.random().toFloat() - 0.5f
            val y = Math.random().toFloat() - 0.5f
            val z = Math.random().toFloat() / 2f   // upper hemisphere only
            val len = sqrt(x * x + y * y + z * z).coerceAtLeast(1e-6f)
            // no-op
            starVertices.add(floatArrayOf(x / len, y / len, z / len))
            val intensity = (Math.random().toFloat().pow(2f) + 0.1f).coerceAtMost(1f)
            starIntensities.add(intensity)
            starColors.add(
                floatArrayOf(
                    0.75f + Math.random().toFloat() * 0.25f,
                    1f,
                    0.75f + Math.random().toFloat() * 0.25f,
                    1f,
                )
            )
        }
    }

    private var starColorSwapCounter: Int = 0

    private fun updateStarColors() {
        starColorSwapCounter++
        if (starColorSwapCounter % 2 != 1) return
        val variance = 0.15f
        val minAlpha = 0.5f
        for (i in starColors.indices) {
            val intensity = starIntensities[i]
            var alpha = starColors[i][3] + (Math.random().toFloat() - 0.5f) * variance * intensity
            alpha = alpha.coerceAtLeast(minAlpha * intensity).coerceAtMost(intensity).coerceIn(0f, 1f)
            starColors[i][3] = alpha
        }
    }

    private fun updateStarGeometry(drawable: Any?) {
        // Each star expands into a screen-aligned quad (6 verts = 2 triangles).
        // The billboard axes are computed from the star direction crossed with up/left.
        // no-op
    }

    companion object {
        fun numStacks(skyDetail: UInt = 32u): UInt =
            skyDetail.coerceIn(MIN_SKY_DETAIL, MAX_SKY_DETAIL)

        fun numSlices(skyDetail: UInt = 32u): UInt =
            2u * numStacks(skyDetail)

        fun stripsNumVerts(skyDetail: UInt = 32u): UInt =
            (numStacks(skyDetail) - 1u) * numSlices(skyDetail)

        fun stripsNumIndices(skyDetail: UInt = 32u): UInt {
            val stacks = numStacks(skyDetail)
            val slices = numSlices(skyDetail)
            return 2u * ((stacks - 2u) * (slices + 1u)) + 1u
        }

        // phi(i) = (PI/8) * (1 − (1 − t^4)^2), t = i/numStacks
        // Biases tessellation density toward the dome apex.
        fun calcPhi(stackIndex: UInt, numStacks: UInt): Float {
            var t = stackIndex.toFloat() / numStacks.toFloat()
            t = t * t * t * t
            t = 1f - t
            t = t * t
            t = 1f - t
            return (PI / 8.0).toFloat() * t
        }
    }
}
