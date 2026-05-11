/**
 * ParcelOverlay.kt
 * Kotlin conversion of llviewerparceloverlay.h / llviewerparceloverlay.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * Licensed under LGPL v2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// Parcel ownership byte constants (mirrors PARCEL_* defines from llparcel.h)
// ---------------------------------------------------------------------------

/** Parcel has no owner (public land). */
const val PARCEL_PUBLIC  : UByte = 0u
/** Parcel is owned by an agent/entity other than self. */
const val PARCEL_OWNED   : UByte = 1u
/** Parcel is owned by a group. */
const val PARCEL_GROUP   : UByte = 2u
/** Parcel is owned by the current user. */
const val PARCEL_SELF    : UByte = 3u
/** Parcel is listed for sale. */
const val PARCEL_FOR_SALE: UByte = 4u
/** Parcel is at auction. */
const val PARCEL_AUCTION : UByte = 5u

/** Bit-flag: west edge of parcel boundary. */
const val PARCEL_WEST_LINE : UByte = 0x40u
/** Bit-flag: south edge of parcel boundary. */
const val PARCEL_SOUTH_LINE: UByte = 0x80u
/** Bit-flag: parcel has local sound enabled. */
const val PARCEL_SOUND_LOCAL: UByte = 0x20u

/** Pixels per parcel grid step (4 m in standard SL). */
const val PARCEL_GRID_STEP_METERS: Float = 4f

// ---------------------------------------------------------------------------
// Axis-aligned bounding-box placeholder (mirrors LLBBox)
// ---------------------------------------------------------------------------

/**
 * Minimal axis-aligned bounding box used by the encroachment tests.
 * The full LLBBox is in the render pipeline; we forward-declare it here.
 */
data class BBox(val min: Vector3, val max: Vector3)

// ---------------------------------------------------------------------------
// Color4  (lightweight RGBA carrier for parcel overlay colours)
// ---------------------------------------------------------------------------

/**
 * Simple RGBA colour carrier.
 * In the C++ codebase this is LLColor4U (or LLColor4 for floats).
 */
data class Color4(val r: Float, val g: Float, val b: Float, val a: Float) {
    companion object {
        val TRANSPARENT: Color4 = Color4(0f, 0f, 0f, 0f)
    }
}

// ---------------------------------------------------------------------------
// ParcelOverlay  (mirrors LLViewerParcelOverlay)
// ---------------------------------------------------------------------------

/**
 * Manages the parcel ownership overlay texture for a single viewer region.
 *
 * Each [ParcelOverlay] owns a byte array ([ownership]) whose entries encode
 * the parcel type and boundary flags for every parcel-grid cell. The overlay
 * texture and property-line geometry are rebuilt on demand (when [isDirty]
 * is set) from that array.
 *
 * GPU and window-system calls are stubbed with TODO("GPU: …").
 *
 * Mirrors `LLViewerParcelOverlay`.
 *
 * @param region The [LLViewerRegion]-equivalent that owns this overlay.
 *               Typed as [Any?] because the full region class is not yet ported.
 */
class ParcelOverlay(val region: Any?) {

    // -----------------------------------------------------------------------
    // Companion: static colour constants and colour initialisation
    // -----------------------------------------------------------------------

    companion object {
        /** Colour shown for publicly available (unowned) parcels. */
        val PARCEL_COLOR_PUBLIC: Color4   = Color4(1.0f, 1.0f, 1.0f, 0.0f)  // transparent

        /** Colour shown for parcels owned by others. */
        val PARCEL_COLOR_OWNED: Color4    = Color4(1.0f, 0.25f, 0.25f, 0.5f) // reddish

        /** Colour shown for parcels listed for sale. */
        val PARCEL_COLOR_FOR_SALE: Color4 = Color4(0.25f, 1.0f, 0.25f, 0.5f) // greenish

        /** Colour shown for group-owned parcels. */
        val PARCEL_COLOR_GROUP: Color4    = Color4(0.25f, 0.25f, 1.0f, 0.5f) // bluish

        /** Colour shown for self-owned parcels. */
        val PARCEL_COLOR_SELF: Color4     = Color4(1.0f, 1.0f, 0.0f, 0.5f)   // yellow

        /** Colour shown for auctioned parcels. */
        val PARCEL_COLOR_AUCTION: Color4  = Color4(1.0f, 0.0f, 1.0f, 0.5f)   // magenta

        // The actual colour values are loaded from the UI colour table at
        // runtime in the C++ constructor; these serve as reasonable defaults.

        /**
         * Number of components per pixel in the overlay texture (RGBA = 4).
         */
        const val OVERLAY_IMG_COMPONENTS: Int = 4
    }

    // -----------------------------------------------------------------------
    // Grid dimensions
    // -----------------------------------------------------------------------

    /**
     * Number of parcel-grid cells along each edge of the region.
     * Standard SL: region is 256 m wide, grid step is 4 m → 64 cells/edge.
     * Aurora Sim regions may be larger.
     */
    val parcelGridsPerEdge: Int

    /** Region width in metres (e.g. 256 for standard SL regions). */
    val regionSize: Int

    init {
        // In the C++ constructor these values come from the region width parameter.
        // We derive defaults here; the real value would come from `region`.
        regionSize         = 256
        parcelGridsPerEdge = (regionSize / PARCEL_GRID_STEP_METERS).toInt()  // 64
    }

    // -----------------------------------------------------------------------
    // Ownership data
    // -----------------------------------------------------------------------

    /**
     * Flat array of ownership bytes.  Size = [parcelGridsPerEdge]².
     * Index = row * parcelGridsPerEdge + col.
     *
     * Lower 3 bits: ownership type (PARCEL_PUBLIC … PARCEL_AUCTION).
     * Upper bits:   edge flags (PARCEL_WEST_LINE, PARCEL_SOUTH_LINE, …).
     */
    val ownership: ByteArray =
        ByteArray(parcelGridsPerEdge * parcelGridsPerEdge) { PARCEL_PUBLIC.toByte() }

    // -----------------------------------------------------------------------
    // Dirty / update state
    // -----------------------------------------------------------------------

    /** True when the overlay texture or property lines need to be rebuilt. */
    var isDirty: Boolean = false
        private set

    private var overlayTextureIdx: Int = -1

    // -----------------------------------------------------------------------
    // Public accessors
    // -----------------------------------------------------------------------

    /**
     * Return the raw ownership byte for the parcel at ([x], [y]) in
     * *local region metres*.
     */
    private fun ownershipAt(row: Int, col: Int): UByte {
        val index = row * parcelGridsPerEdge + col
        return if (index in ownership.indices) ownership[index].toUByte() else PARCEL_PUBLIC
    }

    /** Convert a local-metres position to a (row, col) grid pair. */
    private fun posToGrid(x: Float, y: Float): Pair<Int, Int> {
        val col = (x / PARCEL_GRID_STEP_METERS).toInt()
        val row = (y / PARCEL_GRID_STEP_METERS).toInt()
        return row to col
    }

    /** Return the ownership type byte (lower 3 bits) at [pos]. */
    fun ownershipType(x: Float, y: Float): UByte {
        val (row, col) = posToGrid(x, y)
        return (ownershipAt(row, col).toInt() and 0x07).toUByte()
    }

    /** True when the parcel at [pos] has any owner (not public). */
    fun owns(x: Float, y: Float): Boolean =
        ownershipType(x, y) != PARCEL_PUBLIC

    /** True when the parcel at [pos] is owned by the current user. */
    fun isOwnedSelf(x: Float, y: Float): Boolean =
        ownershipType(x, y) == PARCEL_SELF

    /** True when the parcel at [pos] is group-owned. */
    fun isOwnedGroup(x: Float, y: Float): Boolean =
        ownershipType(x, y) == PARCEL_GROUP

    /** True when the parcel at [pos] is owned by someone other than self. */
    fun isOwnedOther(x: Float, y: Float): Boolean {
        val t = ownershipType(x, y)
        return t == PARCEL_OWNED || t == PARCEL_FOR_SALE
    }

    /** True when the parcel at [pos] has the sound-local flag set. */
    fun isSoundLocal(x: Float, y: Float): Boolean {
        val (row, col) = posToGrid(x, y)
        return (ownershipAt(row, col).toInt() and PARCEL_SOUND_LOCAL.toInt()) != 0
    }

    /** True when the parcel at [pos] is listed for sale. */
    fun isForSale(x: Float, y: Float): Boolean =
        ownershipType(x, y) == PARCEL_FOR_SALE

    /** Return the combined boundary-line flags at [pos]. */
    fun parcelLineFlags(x: Float, y: Float): UByte {
        val (row, col) = posToGrid(x, y)
        val mask = (PARCEL_WEST_LINE.toInt() or PARCEL_SOUTH_LINE.toInt()).toUByte()
        return (ownershipAt(row, col).toInt() and mask.toInt()).toUByte()
    }

    /** Return the combined boundary-line flags at grid ([row], [col]). */
    fun parcelLineFlags(row: Int, col: Int): UByte {
        val mask = (PARCEL_WEST_LINE.toInt() or PARCEL_SOUTH_LINE.toInt()).toUByte()
        return (ownershipAt(row, col).toInt() and mask.toInt()).toUByte()
    }

    // -----------------------------------------------------------------------
    // Encroachment tests (mirrors the LLBBox-based C++ methods)
    // -----------------------------------------------------------------------

    /**
     * Returns true if any of [boxes] overlaps a self-owned or group-owned parcel.
     * Mirrors `LLViewerParcelOverlay::encroachesOwned()`.
     */
    fun encroachesOwned(boxes: List<BBox>): Boolean {
        for (box in boxes) {
            if (scanGridForTypes(box, setOf(PARCEL_SELF, PARCEL_GROUP))) return true
        }
        return false
    }

    /**
     * Returns true if any of [boxes] overlaps a parcel not owned by self.
     * Mirrors `LLViewerParcelOverlay::encroachesOnUnowned()`.
     */
    fun encroachesOnUnowned(boxes: List<BBox>): Boolean {
        for (box in boxes) {
            if (scanGridExcludingType(box, PARCEL_SELF)) return true
        }
        return false
    }

    /**
     * Returns true if any of [boxes] crosses a parcel boundary.
     * Mirrors `LLViewerParcelOverlay::encroachesOnNearbyParcel()`.
     */
    fun encroachesOnNearbyParcel(boxes: List<BBox>): Boolean {
        val regionWidth = regionSize.toFloat()
        for (box in boxes) {
            if (box.min.x < 0f || box.min.y < 0f ||
                box.max.x > regionWidth || box.max.y > regionWidth) {
                return true
            }
            if (scanGridForBoundaryLine(box)) return true
        }
        return false
    }

    // -----------------------------------------------------------------------
    // Fraction of region that is owned
    // -----------------------------------------------------------------------

    /**
     * Returns the ratio of owned parcel cells to total cells.
     * Mirrors `LLViewerParcelOverlay::getOwnedRatio()`.
     */
    fun getOwnedRatio(): Float {
        val total = ownership.size
        if (total == 0) return 0f
        val owned = ownership.count {
            (it.toInt() and 0x07) != PARCEL_PUBLIC.toInt()
        }
        return owned.toFloat() / total.toFloat()
    }

    // -----------------------------------------------------------------------
    // Decompression / update entry points
    // -----------------------------------------------------------------------

    /**
     * Decompress a run-length-encoded land overlay packet received from the
     * server and update the [ownership] array.
     *
     * @param data The compressed binary payload.
     *
     * Mirrors `LLViewerParcelOverlay::uncompressLandOverlay(S32 chunk, U8*)`.
     */
    fun uncompressLandOverlay(data: ByteArray) {
        TODO("uncompressLandOverlay not yet ported (${data.size} bytes)")
    }

    /**
     * Rebuild the overlay texture from the current [ownership] data.
     * Mirrors `LLViewerParcelOverlay::updateOverlayTexture()`.
     */
    fun updateOverlayTexture() {
        TODO("GPU: updateOverlayTexture not yet ported")
    }

    /**
     * Rebuild and upload property-line geometry to the GPU.
     * Mirrors `LLViewerParcelOverlay::renderPropertyLines()`.
     */
    fun renderPropertyLines() {
        TODO("GPU: renderPropertyLines not yet ported")
    }

    /**
     * Render property lines on the minimap at the given [scalePixelsPerMeter].
     * Mirrors `LLViewerParcelOverlay::renderPropertyLinesOnMinimap()`.
     */
    fun renderPropertyLinesOnMinimap(scalePixelsPerMeter: Float, parcelOutlineColor: FloatArray) {
        TODO("GPU: renderPropertyLinesOnMinimap not yet ported")
    }

    // -----------------------------------------------------------------------
    // Dirty / idle update
    // -----------------------------------------------------------------------

    /** Mark the overlay as needing a texture / geometry rebuild. */
    fun setDirty() {
        isDirty = true
    }

    /**
     * Called each frame to decide whether to rebuild the overlay.
     * Mirrors `LLViewerParcelOverlay::idleUpdate()`.
     */
    fun idleUpdate(updateNow: Boolean = false) {
        if (!isDirty) return
        if (updateNow) {
            updateOverlayTexture()
            isDirty = false
        }
        // Otherwise wait for the next GL update tick
    }

    /**
     * GPU-thread callback to upload the rebuilt texture.
     * Mirrors `LLViewerParcelOverlay::updateGL()`.
     */
    fun updateGL() {
        TODO("GPU: updateGL not yet ported")
    }

    // -----------------------------------------------------------------------
    // Internal grid scanning helpers
    // -----------------------------------------------------------------------

    private fun gridBounds(box: BBox): IntArray {
        val regionWidth = regionSize.toFloat() - 1f
        val left   = (box.min.x / PARCEL_GRID_STEP_METERS).toInt().coerceIn(0, regionWidth.toInt())
        val right  = (box.max.x / PARCEL_GRID_STEP_METERS).toInt().coerceIn(0, regionWidth.toInt())
        val bottom = (box.min.y / PARCEL_GRID_STEP_METERS).toInt().coerceIn(0, regionWidth.toInt())
        val top    = (box.max.y / PARCEL_GRID_STEP_METERS).toInt().coerceIn(0, regionWidth.toInt())
        return intArrayOf(left, right, bottom, top)
    }

    private fun scanGridForTypes(box: BBox, types: Set<UByte>): Boolean {
        val (left, right, bottom, top) = gridBounds(box)
        for (row in bottom..top) {
            for (col in left..right) {
                if (ownershipType(
                        col * PARCEL_GRID_STEP_METERS,
                        row * PARCEL_GRID_STEP_METERS
                    ) in types
                ) return true
            }
        }
        return false
    }

    private fun scanGridExcludingType(box: BBox, exclude: UByte): Boolean {
        val (left, right, bottom, top) = gridBounds(box)
        for (row in bottom..top) {
            for (col in left..right) {
                if (ownershipType(
                        col * PARCEL_GRID_STEP_METERS,
                        row * PARCEL_GRID_STEP_METERS
                    ) != exclude
                ) return true
            }
        }
        return false
    }

    private fun scanGridForBoundaryLine(box: BBox): Boolean {
        val (left, right, bottom, top) = gridBounds(box)
        val grids = parcelGridsPerEdge
        for (row in bottom..top) {
            for (col in left..right) {
                if (col < grids - 1) {
                    val eastOverlay = ownershipAt(row, col + 1)
                    if ((eastOverlay.toInt() and PARCEL_WEST_LINE.toInt()) != 0 && col < right) return true
                }
                if (row < grids - 1) {
                    val northOverlay = ownershipAt(row + 1, col)
                    if ((northOverlay.toInt() and PARCEL_SOUTH_LINE.toInt()) != 0 && row < top) return true
                }
            }
        }
        return false
    }

    // Destructure IntArray of 4 as (left, right, bottom, top)
    private operator fun IntArray.component1() = this[0]
    private operator fun IntArray.component2() = this[1]
    private operator fun IntArray.component3() = this[2]
    private operator fun IntArray.component4() = this[3]
}
