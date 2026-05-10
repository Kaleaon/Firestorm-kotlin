// Converted from llwind.h / llwind.cpp (Firestorm / Linden Research)
// LGPL-2.1-only — see project root for full license text.

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import kotlin.math.floor

/**
 * Wind velocity lattice for one viewer region.
 *
 * Mirrors [LLWind] from llwind.h.
 * The lattice is a [size]×[size] grid of (velX, velY) samples.  Velocity at
 * an arbitrary region-local position is obtained by bilinear interpolation.
 *
 * The [decompress] method accepts the raw network payload; full DCT
 * patch-decompression is stubbed with [TODO] and must be wired to the
 * patch_dct / patch_code C++ layer (or a Kotlin port of it).
 */
object Wind {

    /** Grid dimension. The C++ implementation hard-codes 16. */
    const val SIZE = 16
    const val WIND_SCALE_HACK = 2.0f   // makes wind speeds feel more realistic

    // Flat row-major arrays: index = x + y * SIZE
    private val velX = FloatArray(SIZE * SIZE) { 0.5f }
    private val velY = FloatArray(SIZE * SIZE) { 0.5f }

    /** Global origin of this region (world space). Not used by velocity lookup but kept for completeness. */
    var originGlobal: Vector3 = Vector3.ZERO

    // ---- public API ----

    /**
     * Return bilinearly-interpolated wind velocity at a region-local position.
     *
     * Clamped to [0, regionWidth) in both axes.
     * Z component is always 0.
     *
     * @param pos region-local position (metres)
     * @param regionWidth width of the region in metres (typically 256)
     */
    fun getVelocity(pos: Vector3, regionWidth: Float = 256f): Vector3 {
        val cx = pos.x.coerceIn(0f, regionWidth - Float.MIN_VALUE) % regionWidth
        val cy = pos.y.coerceIn(0f, regionWidth - Float.MIN_VALUE) % regionWidth

        val fi = cx * SIZE / regionWidth
        val fj = cy * SIZE / regionWidth
        val i = floor(fi).toInt()
        val j = floor(fj).toInt()
        val dx = fi - i
        val dy = fj - j
        val k = i + j * SIZE

        return if (i < SIZE - 1 && j < SIZE - 1) {
            // bilinear interior interpolation
            val vx = velX[k]          * (1f - dx) * (1f - dy) +
                     velX[k + 1]      * dx         * (1f - dy) +
                     velX[k + SIZE]   * dy          * (1f - dx) +
                     velX[k + SIZE + 1] * dx        * dy
            val vy = velY[k]          * (1f - dx) * (1f - dy) +
                     velY[k + 1]      * dx         * (1f - dy) +
                     velY[k + SIZE]   * dy          * (1f - dx) +
                     velY[k + SIZE + 1] * dx        * dy
            Vector3(vx * WIND_SCALE_HACK, vy * WIND_SCALE_HACK, 0f)
        } else {
            // edge / corner: nearest sample
            Vector3(velX[k] * WIND_SCALE_HACK, velY[k] * WIND_SCALE_HACK, 0f)
        }
    }

    /**
     * Fractal-sum (noisy) velocity: recursively scales position and sums
     * [getVelocity] results, normalised by [dim].
     *
     * @param pos region-local position
     * @param dim starting octave count (1, 2, 4, or 8)
     */
    fun getVelocityNoisy(pos: Vector3, dim: Float, regionWidth: Float = 256f): Vector3 {
        val norm = when (dim) {
            8f -> 1.875f
            4f -> 1.75f
            2f -> 1.5f
            else -> 1.0f
        }
        var result = Vector3.ZERO
        var d = dim
        while (d >= 1f) {
            result = result + getVelocity(pos * d, regionWidth) * (1f / d)
            d /= 2f
        }
        return result * (1f / norm) * WIND_SCALE_HACK
    }

    /**
     * Compute average wind velocity across the whole grid.
     * Used for broad environmental effects (e.g. ambient audio).
     */
    fun getAverage(): Vector3 {
        val n = SIZE * SIZE
        var sumX = 0f
        var sumY = 0f
        for (i in 0 until n) {
            sumX += velX[i]
            sumY += velY[i]
        }
        val inv = WIND_SCALE_HACK / n.toFloat()
        return Vector3(sumX * inv, sumY * inv, 0f)
    }

    /**
     * Set a wind sample directly (used by tests or debug tools).
     *
     * @param x grid column  [0, SIZE)
     * @param y grid row     [0, SIZE)
     * @param vel wind velocity (only X and Y components used)
     */
    fun setVelocity(x: Int, y: Int, vel: Vector3) {
        require(x in 0 until SIZE && y in 0 until SIZE) {
            "Wind grid index out of range: ($x, $y)"
        }
        velX[x + y * SIZE] = vel.x
        velY[x + y * SIZE] = vel.y
    }

    /**
     * Decompress a network wind packet (DCT patch format) into [velX]/[velY].
     *
     * [data] is the raw byte payload from the simulator WindObject message.
     * Full decompression requires bridging to the C++ `patch_dct` / `patch_code`
     * routines (or a Kotlin port of them).
     */
    fun decompress(data: ByteArray) {
        TODO("GPU: init_patch_decompressor; decode_patch_header+decode_patch+decompress_patch for X then Y; scale by WIND_SCALE_HACK")
    }

    /** Render debug wind vectors as line primitives (editor/debug only). */
    fun renderVectors() {
        TODO("GPU: draw velocity arrows at each lattice point")
    }

    /** Replace the region origin used for global coordinate transforms. */
    fun setOriginGlobal(origin: Vector3) {
        originGlobal = origin
    }

    /** Reset all velocities to the default initialised value (0.5, 0.5). */
    fun reset() {
        velX.fill(0.5f)
        velY.fill(0.5f)
    }
}
