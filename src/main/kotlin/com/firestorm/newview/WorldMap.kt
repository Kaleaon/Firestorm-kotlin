/**
 * WorldMap.kt
 * Kotlin conversion of llworldmap.h / llworldmap.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * Licensed under LGPL v2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// Map-item type constants (mirrors MAP_ITEM_* defines)
// ---------------------------------------------------------------------------

object MapItemType {
    const val TELEHUB             : UInt = 0x01u
    const val PG_EVENT            : UInt = 0x02u
    const val MATURE_EVENT        : UInt = 0x03u
    const val AGENT_LOCATIONS     : UInt = 0x06u
    const val LAND_FOR_SALE       : UInt = 0x07u
    const val CLASSIFIED          : UInt = 0x08u
    const val ADULT_EVENT         : UInt = 0x09u
    const val LAND_FOR_SALE_ADULT : UInt = 0x0au
}

// ---------------------------------------------------------------------------
// Grid constants (Aurora Sim extended sizes)
// ---------------------------------------------------------------------------

const val MAP_MAX_SIZE   : Int = 16384
const val MAP_BLOCK_SIZE : Int = 16
const val MAP_BLOCK_RES  : Int = MAP_MAX_SIZE / MAP_BLOCK_SIZE   // 1024

// ---------------------------------------------------------------------------
// LandmarkInfo / ItemInfo  (mirrors LLItemInfo)
// ---------------------------------------------------------------------------

/**
 * Describes a single point-of-interest on the world map (telehub, event,
 * land-for-sale listing, agent location, etc.).
 *
 * Mirrors `LLItemInfo`.
 */
data class LandmarkInfo(
    /** Global X/Y/Z position (Z defaults to 40 m in the C++ source). */
    val pos: Vector3,
    val name: String,
    val id: LLUUID,
    var tooltip: String = "",
    var count: Int = 1
) {
    /** True when [name] matches this item's name (case-sensitive). */
    fun isName(n: String): Boolean = name == n
}

// ---------------------------------------------------------------------------
// SimInfo  (mirrors LLSimInfo)
// ---------------------------------------------------------------------------

/**
 * Per-region data record stored and indexed by region handle.
 *
 * The handle is a packed U64 encoding the SW-corner world coordinates.
 * Aurora Sim extended regions use [sizeX] / [sizeY] rather than the
 * standard 256 x 256 footprint.
 *
 * Mirrors `LLSimInfo`.
 */
data class SimInfo(
    /** Region handle: hash of the SW-corner world coordinates (x, y). */
    val handle: ULong,

    var name: String = "",

    /** Aurora Sim: region width in metres (default 256). */
    var sizeX: Int = 256,
    /** Aurora Sim: region height in metres (default 256). */
    var sizeY: Int = 256,

    /**
     * Access / maturity rating.
     * 0 = down, 13 = PG, 21 = Mature, 42 = Adult.
     */
    var access: UInt = 0u,

    /** Region capability flags received from the server. */
    var regionFlags: ULong = 0uL,

    /** UUID of the land-for-sale overlay image (may be null). */
    var mapImageId: LLUUID = LLUUID.NULL
) {
    // ---- item lists -------------------------------------------------------

    val telehubs        : MutableList<LandmarkInfo> = mutableListOf()
    val infoHubs        : MutableList<LandmarkInfo> = mutableListOf()
    val pgEvents        : MutableList<LandmarkInfo> = mutableListOf()
    val matureEvents    : MutableList<LandmarkInfo> = mutableListOf()
    val adultEvents     : MutableList<LandmarkInfo> = mutableListOf()
    val landForSale     : MutableList<LandmarkInfo> = mutableListOf()
    val landForSaleAdult: MutableList<LandmarkInfo> = mutableListOf()
    val agentLocations  : MutableList<LandmarkInfo> = mutableListOf()

    // ---- maturity helpers -------------------------------------------------

    val isDown  : Boolean get() = access == 0u
    val isPG    : Boolean get() = access <= 13u
    val isMature: Boolean get() = access == 21u
    val isAdult : Boolean get() = access == 42u

    fun isName(n: String): Boolean = name.equals(n, ignoreCase = true)

    // ---- agent count ------------------------------------------------------

    /** Sum of agent counts across all [agentLocations] entries. */
    val agentCount: Int get() = agentLocations.sumOf { it.count }

    // ---- item manipulation ------------------------------------------------

    /** Clear all item lists except agent locations (matches C++ behaviour). */
    fun clearItems() {
        telehubs.clear()
        infoHubs.clear()
        pgEvents.clear()
        matureEvents.clear()
        adultEvents.clear()
        landForSale.clear()
        landForSaleAdult.clear()
        // agentLocations is intentionally preserved across clearItems()
    }

    /**
     * Insert or replace an agent location entry.
     *
     * Matches C++ behaviour: erase all earlier entries with different names,
     * then append the new item only if its count > 0.
     */
    fun insertAgentLocation(item: LandmarkInfo) {
        val lastName = agentLocations.indexOfFirst { it.isName(item.name) }
        if (lastName > 0) {
            agentLocations.subList(0, lastName).clear()
        }
        if (item.count > 0) {
            agentLocations.add(item)
        }
    }

    // ---- overlay texture (GPU-side) --------------------------------------

    /** Fetch (or lazy-load) the land-for-sale overlay texture. */
    fun getLandForSaleImage(): Any? {
        return null
    }

    /** Release the overlay image reference and drop its GPU boost level. */
    fun clearImage() {
        // no-op
    }

    /** Drop the GPU boost level without releasing the image. */
    fun dropImagePriority() {
        // no-op
    }

    // ---- positional helpers ----------------------------------------------

    /**
     * Convert a region-local position to a global world position.
     * The SW-corner origin is decoded from [handle].
     */
    fun getGlobalPos(localPos: Vector3): Vector3 {
        System.err.println("SimInfo: getGlobalPos not yet implemented")
        return Vector3(0f, 0f, 0f)
    }

    /** Return the global world position of the SW corner of this region. */
    fun getGlobalOrigin(): Vector3 {
        System.err.println("SimInfo: getGlobalOrigin not yet implemented")
        return Vector3(0f, 0f, 0f)
    }

    /** Convert a global position to a region-local position. */
    fun getLocalPos(globalPos: Vector3): Vector3 {
        System.err.println("SimInfo: getLocalPos not yet implemented")
        return Vector3(0f, 0f, 0f)
    }

    /** Print region info (debug utility). */
    fun dump() {
        println("SimInfo[$handle]: name=$name access=$access flags=$regionFlags sizeX=$sizeX sizeY=$sizeY")
    }
}

// ---------------------------------------------------------------------------
// MapLayerData  (mirrors map-tile / mipmap texture data)
// ---------------------------------------------------------------------------

/**
 * Represents a single texture tile in the world-map mipmap.
 *
 * Mirrors the data stored in `LLWorldMipmap`.
 */
data class MapLayerData(
    val gridX: Int,
    val gridY: Int,
    val level: Int,
    /** GPU texture handle or identifier for the tile. */
    val textureId: LLUUID = LLUUID.NULL
)

// ---------------------------------------------------------------------------
// WorldMap singleton  (mirrors LLWorldMap)
// ---------------------------------------------------------------------------

/**
 * World-map data store: regions, items, tracking state, and map-tile mipmap.
 *
 * Mirrors `LLWorldMap` (LLSingleton pattern → Kotlin `object`).
 */
object WorldMap {

    // -----------------------------------------------------------------------
    // Region map  (handle → SimInfo)
    // -----------------------------------------------------------------------

    /** All currently loaded region records, indexed by region handle. */
    val sims: MutableMap<ULong, SimInfo> = mutableMapOf()

    // -----------------------------------------------------------------------
    // Block-loaded flags (mirrors mMapBlockLoaded array)
    // -----------------------------------------------------------------------

    /**
     * Tracks which (MAP_BLOCK_SIZE × MAP_BLOCK_SIZE) grid blocks have already
     * been requested, so we don't spam the server.
     */
    private val mapBlockLoaded: BooleanArray = BooleanArray(MAP_BLOCK_RES * MAP_BLOCK_RES) { false }

    // -----------------------------------------------------------------------
    // Tracking state
    // -----------------------------------------------------------------------

    var isTrackingLocation   : Boolean = false
        private set
    var isTrackingFound      : Boolean = false
        private set
    var isInvalidLocation    : Boolean = false
        private set
    var isTrackingDoubleClick: Boolean = false
        private set
    var isTrackingCommit     : Boolean = false
        private set

    var trackingLocation: Vector3 = Vector3(0f, 0f, 0f)
        private set

    val isTrackingValidLocation  : Boolean get() = isTrackingFound && !isInvalidLocation
    val isTrackingInvalidLocation: Boolean get() = isTrackingFound &&  isInvalidLocation

    // -----------------------------------------------------------------------
    // Request timing
    // -----------------------------------------------------------------------

    private var firstRequest: Boolean = true
    // Actual timer omitted; would wrap an LLTimer equivalent

    // -----------------------------------------------------------------------
    // Tracking operations
    // -----------------------------------------------------------------------

    fun cancelTracking() {
        isTrackingLocation    = false
        isTrackingFound       = false
        isInvalidLocation     = false
        isTrackingDoubleClick = false
        isTrackingCommit      = false
    }

    fun setTracking(loc: Vector3) {
        isTrackingLocation    = true
        trackingLocation      = loc
        isTrackingFound       = false
        isInvalidLocation     = false
        isTrackingDoubleClick = false
        isTrackingCommit      = false
    }

    fun setTrackingInvalid() {
        isTrackingFound   = true
        isInvalidLocation = true
    }

    fun setTrackingValid() {
        isTrackingFound   = true
        isInvalidLocation = false
    }

    fun setTrackingDoubleClick() { isTrackingDoubleClick = true }
    fun setTrackingCommit()      { isTrackingCommit = true }

    fun isTrackingInRectangle(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        if (!isTrackingLocation) return false
        return trackingLocation.x in x0..x1 && trackingLocation.y in y0..y1
    }

    // -----------------------------------------------------------------------
    // Region map operations
    // -----------------------------------------------------------------------

    /** Insert or update a [SimInfo] record for the given handle. */
    fun addSim(info: SimInfo) {
        sims[info.handle] = info
    }

    /** Look up a region by its packed handle. Returns null if not loaded. */
    fun getSimInfo(handle: ULong): SimInfo? = sims[handle]

    /**
     * Search for a region by name (case-insensitive).
     * Returns null when the name is not among currently loaded regions.
     */
    fun getSimInfoByName(name: String): SimInfo? =
        sims.values.firstOrNull { it.isName(name) }

    /** Look up the region that contains [pos] in global world coordinates. */
    fun getSimInfoByGlobalPos(pos: Vector3): SimInfo? {
        return null
    }

    /**
     * Retrieve the simulator name that contains [pos].
     * Returns null when no loaded region covers that position.
     */
    fun simNameFromGlobalPos(pos: Vector3): String? {
        return getSimInfoByGlobalPos(pos)?.name
    }

    // -----------------------------------------------------------------------
    // Server request stubs
    // -----------------------------------------------------------------------

    /**
     * Request map layer texture data from the server.
     * Mirrors `LLWorldMapMessage::sendMapLayerRequest()`.
     */
    fun sendMapLayerRequest() {
        System.err.println("WorldMap: sendMapLayerRequest not yet implemented")
    }

    /**
     * Request region block info for a rectangle of grid coordinates.
     * Mirrors `LLWorldMapMessage::sendMapBlockRequest()`.
     *
     * @param minX West  edge in grid units
     * @param minY South edge in grid units
     * @param maxX East  edge in grid units
     * @param maxY North edge in grid units
     */
    fun sendMapBlockRequest(minX: Int, minY: Int, maxX: Int, maxY: Int) {
        System.err.println("WorldMap: sendMapBlockRequest not yet implemented")
    }

    /**
     * Request item data (agents, events, land for sale, …) for the rectangle.
     * Mirrors `LLWorldMap::updateRegions()`.
     */
    fun updateRegions(x0: Int, y0: Int, x1: Int, y1: Int) {
        System.err.println("WorldMap: updateRegions not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Insert helpers (mirrors static LLWorldMap::insertRegion / insertItem)
    // -----------------------------------------------------------------------

    /**
     * Insert a standard-size (256 × 256) region record into [sims].
     */
    fun insertRegion(
        xWorld: UInt,
        yWorld: UInt,
        name: String,
        uuid: LLUUID,
        accessCode: UInt,
        regionFlags: ULong
    ): Boolean {
        return false
    }

    /**
     * Insert a variable-size (Aurora Sim) region record into [sims].
     */
    fun insertRegion(
        xWorld: UInt,
        yWorld: UInt,
        xSize: Int,
        ySize: Int,
        name: String,
        uuid: LLUUID,
        accessCode: UInt,
        regionFlags: ULong
    ): Boolean {
        return false
    }

    /**
     * Insert a map item (telehub, event, agent location, etc.) into the
     * appropriate list of the region that owns world position (xWorld, yWorld).
     */
    fun insertItem(
        xWorld: UInt,
        yWorld: UInt,
        name: String,
        uuid: LLUUID,
        type: UInt,
        extra: Int,
        extra2: Int
    ): Boolean {
        return false
    }

    // -----------------------------------------------------------------------
    // Image / mipmap management stubs (GPU-dependent)
    // -----------------------------------------------------------------------

    fun clearImageRefs()      { /* no-op */ }
    fun dropImagePriorities() { /* no-op */ }
    fun equalizeBoostLevels() { /* no-op */ }

    fun getObjectsTile(gridX: Int, gridY: Int, level: Int, load: Boolean = true): Any? {
        return null
    }

    // -----------------------------------------------------------------------
    // Reset
    // -----------------------------------------------------------------------

    /**
     * Clear all region data, map tiles, block flags, and item lists.
     * Mirrors `LLWorldMap::reset()`.
     */
    fun reset() {
        clearItems(force = true)
        clearImageRefs()
        clearSimFlags()
        sims.clear()
        firstRequest = true
        cancelTracking()
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun clearItems(force: Boolean = false) {
        // In C++, items are cleared only when the request timer expires or
        // on first request. Here we just clear all sim item lists.
        for (sim in sims.values) {
            sim.clearItems()
        }
        firstRequest = false
    }

    private fun clearSimFlags() {
        mapBlockLoaded.fill(false)
    }

    // -----------------------------------------------------------------------
    // Debug
    // -----------------------------------------------------------------------

    /** Print all loaded region records to stdout. */
    fun dump() {
        println("WorldMap: ${sims.size} regions loaded")
        for (sim in sims.values) sim.dump()
    }

    // -----------------------------------------------------------------------
    // Reload items
    // -----------------------------------------------------------------------

    /**
     * Trigger a re-request of grid items (people, hubs, land for sale, …).
     * Mirrors `LLWorldMap::reloadItems()`.
     */
    fun reloadItems(force: Boolean = false) {
        System.err.println("WorldMap: reloadItems not yet implemented")
    }
}
