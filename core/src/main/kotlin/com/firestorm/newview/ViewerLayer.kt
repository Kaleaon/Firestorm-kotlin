/**
 * ViewerLayer.kt
 * Kotlin port of llviewerlayer.h / llviewerlayer.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * License: GNU Lesser General Public License v2.1
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

/**
 * Represents a terrain / avatar baking layer that stores a 2-D floating-point
 * height-field and can answer bilinearly-interpolated queries.
 *
 * Corresponds to C++ LLViewerLayer.
 *
 * @param width     The number of samples along each edge of the square grid.
 * @param scale     World-space distance represented by one sample step (default 1.0).
 * @param layerType The semantic role of this layer (e.g. TERRAIN, BUMP, …).
 */
open class ViewerLayer(
    val layerType: LayerType,
    private val width: Int,
    private val scale: Float = 1.0f,
) {
    // -----------------------------------------------------------------------
    // Layer type classification
    // -----------------------------------------------------------------------

    enum class LayerType {
        /** Raw terrain height data received from the sim. */
        TERRAIN,
        /** Normal / bump map data for terrain. */
        BUMP,
        /** Water / liquid height layer. */
        WATER,
        /** Avatar baked clothing / skin layer. */
        BAKE_HEAD,
        BAKE_UPPER,
        BAKE_LOWER,
        BAKE_EYES,
        BAKE_SKIRT,
        BAKE_HAIR,
        /** Fallback / unknown layer type. */
        UNKNOWN,
    }

    // -----------------------------------------------------------------------
    // Internal state — mirrors C++ mWidth, mScale, mScaleInv, mDatap[]
    // -----------------------------------------------------------------------

    private val scaleInv: Float = if (scale != 0.0f) 1.0f / scale else 1.0f

    /** Flat row-major storage: index = y * width + x */
    protected val data: FloatArray = FloatArray(width * width) { 0.0f }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Return the stored value at integer grid coordinates (x, y), clamped to
     * the valid range.  Corresponds to C++ LLViewerLayer::getValue().
     */
    protected fun getValue(x: Int, y: Int): Float {
        val cx = x.coerceIn(0, width - 1)
        val cy = y.coerceIn(0, width - 1)
        return data[cx + cy * width]
    }

    /**
     * Return a bilinearly-interpolated value for world-space coordinates
     * (x, y).  Mirrors C++ LLViewerLayer::getValueScaled().
     */
    fun getValueScaled(x: Float, y: Float): Float {
        var xFrac = x * scaleInv
        val x1 = xFrac.toInt().also { xFrac -= it }
        val x2 = x1 + 1

        var yFrac = y * scaleInv
        val y1 = yFrac.toInt().also { yFrac -= it }
        val y2 = y1 + 1

        // Clamp to valid grid range
        val cx1 = x1.coerceIn(0, width - 1)
        val cx2 = x2.coerceIn(0, width - 1)
        val cy1 = y1.coerceIn(0, width - 1)
        val cy2 = y2.coerceIn(0, width - 1)

        val row1Left  = data[cx1 + cy1 * width]
        val row1Right = data[cx2 + cy1 * width]
        val row2Left  = data[cx1 + cy2 * width]
        val row2Right = data[cx2 + cy2 * width]

        val row1Interp = row1Left - xFrac * (row1Left - row1Right)
        val row2Interp = row2Left - xFrac * (row2Left - row2Right)

        return row1Interp - yFrac * (row1Interp - row2Interp)
    }

    /**
     * Return the associated viewer texture for this layer, if any.
     * Stub — full implementation would look up the texture cache.
     */
    open fun getTexture(): ViewerTexture? = null

    /**
     * Trigger a bake / composite operation for avatar layers.
     * Stub — full implementation drives the bake pipeline.
     */
    open fun bake(): Unit {
        // no-op
    }

    /**
     * Returns true when all local texture data needed to perform a bake is
     * present and valid.
     * Stub — full implementation inspects each required texture slot.
     */
    open fun isLocalTextureDataAvailable(): Boolean = false
}
