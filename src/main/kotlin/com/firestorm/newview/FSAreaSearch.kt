/**
 * @file FSAreaSearch.kt
 * @brief Search and list objects visible to (or known by) the viewer.
 *
 * Ported from fsareasearch.h / fsareasearch.cpp
 * Original copyright (c) 2012 Techwolf Lupindo
 * Phoenix Firestorm Project — LGPL v2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

/** Maximum number of objects that fit in a single property-request packet. */
private const val MAX_OBJECTS_PER_PACKET = 255

/** Seconds between full list refreshes while the search is active. */
private const val REFRESH_INTERVAL = 1.0f

/** Minimum seconds between refreshes (performance guard). */
private const val MIN_REFRESH_INTERVAL = 0.25f

/** Metres the avatar must move before distance columns are recalculated. */
private const val MIN_DISTANCE_MOVED = 1.0f

/** Seconds before a pending property request is considered timed out. */
private const val REQUEST_TIMEOUT = 30.0f

// ---------------------------------------------------------------------------
// Object-properties data class
// ---------------------------------------------------------------------------

/** Request lifecycle for a single object's property fetch. */
enum class ObjectPropertiesRequest { NEED, SENT, FINISHED, FAILED }

/**
 * All cached data for a single in-world object tracked by the area search.
 *
 * Mirrors `struct FSObjectProperties` in C++.
 */
data class FSObjectProperties(
    val id: LLUUID,
    var listed: Boolean = false,
    var name: String = "",
    var description: String = "",
    var touchName: String = "",
    var sitName: String = "",
    var creatorId: LLUUID = LLUUID(),
    var ownerId: LLUUID = LLUUID(),
    var groupId: LLUUID = LLUUID(),
    var ownershipId: LLUUID = LLUUID(),
    var groupOwned: Boolean = false,
    var creationDate: Long = 0L,
    var baseMask: UInt = 0u,
    var ownerMask: UInt = 0u,
    var groupMask: UInt = 0u,
    var everyoneMask: UInt = 0u,
    var nextOwnerMask: UInt = 0u,
    var lastOwnerId: LLUUID = LLUUID(),
    var textureIds: MutableList<LLUUID> = mutableListOf(),
    var nameRequested: Boolean = false,
    var localId: UInt = 0u,
    var regionHandle: ULong = 0uL,
    var request: ObjectPropertiesRequest = ObjectPropertiesRequest.NEED,
)

// ---------------------------------------------------------------------------
// Observer / callback interface
// ---------------------------------------------------------------------------

/**
 * Implement this interface to receive notifications when new search results
 * are available.
 *
 * (Kotlin idiom replacing the C++ signals2 observer pattern.)
 */
interface FSAreaSearchObserver {
    fun onSearchResults(results: List<FSObjectProperties>)
}

// ---------------------------------------------------------------------------
// Search-filter configuration
// ---------------------------------------------------------------------------

/**
 * Encapsulates all filter and exclusion flags that narrow down the object
 * list.  Mirrors the many `mFilter*` / `mExclude*` fields on `FSAreaSearch`.
 */
data class AreaSearchFilter(
    // --- Exclusions ---
    val excludeAttachments: Boolean = false,
    val excludeTemporary: Boolean = false,
    val excludeReflectionProbes: Boolean = false,
    val excludePhysics: Boolean = false,
    val excludeChildPrims: Boolean = true,
    val excludeNeighborRegions: Boolean = true,

    // --- Type filters ---
    val filterLocked: Boolean = false,
    val filterPhysical: Boolean = true,
    val filterTemporary: Boolean = true,
    val filterPhantom: Boolean = false,
    val filterAttachment: Boolean = false,
    val filterMoaP: Boolean = false,
    val filterReflectionProbe: Boolean = false,

    // --- For-sale filter ---
    val filterForSale: Boolean = false,
    val filterForSaleMin: Int = 0,
    val filterForSaleMax: Int = 999_999,

    // --- Distance filter ---
    val filterDistance: Boolean = false,
    val filterDistanceMin: Int = 0,
    val filterDistanceMax: Int = 0,

    // --- Click-action filter ---
    val filterClickAction: Boolean = false,
    val filterClickActionType: UByte = 0u,

    // --- Permission filters ---
    val filterPermCopy: Boolean = false,
    val filterPermModify: Boolean = false,
    val filterPermTransfer: Boolean = false,

    // --- Parcel filter ---
    val filterAgentParcelOnly: Boolean = false,
)

// ---------------------------------------------------------------------------
// Search-text configuration
// ---------------------------------------------------------------------------

/**
 * Groups all text fields and the regex-vs-plain-text switch used by the
 * Find panel.
 */
data class AreaSearchQuery(
    val name: String = "",
    val description: String = "",
    val owner: String = "",
    val group: String = "",
    val creator: String = "",
    val lastOwner: String = "",
    val useRegex: Boolean = false,
)

// ---------------------------------------------------------------------------
// Main area-search engine
// ---------------------------------------------------------------------------

/**
 * Core area-search engine.  In C++ this is an `LLFloater` subclass; here the
 * search logic is decoupled from UI and exposed as a plain object.
 *
 * Mirrors `class FSAreaSearch` in C++.
 */
object FSAreaSearch {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** All objects currently tracked, keyed by their [LLUUID]. */
    val objectDetails: MutableMap<LLUUID, FSObjectProperties> = mutableMapOf()

    /** Whether the search engine is currently active. */
    var isActive: Boolean = false
        private set

    /** Current filter settings applied to search results. */
    var filter: AreaSearchFilter = AreaSearchFilter()

    /** Current text query (may use regex). */
    var query: AreaSearchQuery = AreaSearchQuery()

    /** When `true`, render visual beacons over matching objects in the 3-D world. */
    var beaconsEnabled: Boolean = false

    private var requested: Int = 0
    private var needsRefresh: Boolean = false
    private var requestQueuePaused: Boolean = false
    private var requestNeedsSent: Boolean = false
    private val regionRequests: MutableMap<ULong, Int> = mutableMapOf()

    private val observers: MutableList<FSAreaSearchObserver> = mutableListOf()

    // -----------------------------------------------------------------------
    // Observer registration
    // -----------------------------------------------------------------------

    fun addObserver(observer: FSAreaSearchObserver) {
        observers += observer
    }

    fun removeObserver(observer: FSAreaSearchObserver) {
        observers -= observer
    }

    private fun notifyObservers(results: List<FSObjectProperties>) {
        for (obs in observers) obs.onSearchResults(results)
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Activate the area search: switch the simulator interest list to 360°
     * mode and schedule idle updates.
     */
    fun open() {
        isActive = true
        // TODO: gAgent.changeInterestListMode(IL_MODE_360)
        // TODO: register idle callback
        refreshList(cacheClear = true)
    }

    /**
     * Deactivate the area search and revert the simulator interest list to
     * the default keyhole frustum.
     */
    fun close() {
        isActive = false
        // TODO: gAgent.changeInterestListMode(IL_MODE_DEFAULT)
        // TODO: unregister idle callback
    }

    // -----------------------------------------------------------------------
    // Public search API
    // -----------------------------------------------------------------------

    /**
     * Trigger a new search using [searchQuery] and optional [searchFilter].
     * Updates [query] and [filter], clears cached listings, and initiates a
     * fresh pass over the object list.
     */
    fun search(
        searchQuery: AreaSearchQuery,
        searchFilter: AreaSearchFilter = filter,
    ) {
        query = searchQuery
        filter = searchFilter
        refreshList(cacheClear = false)
    }

    /**
     * Refresh the displayed list.  When [cacheClear] is `true` all tracked
     * object data is discarded; otherwise only the `listed` flag is reset so
     * existing property data is retained.
     */
    fun refreshList(cacheClear: Boolean) {
        isActive = true
        checkRegion()
        if (cacheClear) {
            requested = 0
            objectDetails.clear()
            regionRequests.clear()
        } else {
            objectDetails.values.forEach { it.listed = false }
        }
        needsRefresh = true
        findObjects()
    }

    /**
     * Send object-property requests for the [ids] in the provided list.
     * Objects are selected and deselected server-side in batches of up to
     * [MAX_OBJECTS_PER_PACKET] local IDs per packet.
     *
     * @param ids      List of object UUIDs whose properties are needed.
     */
    fun requestObjectProperties(ids: List<LLUUID>) {
        val localIds = ids.mapNotNull { objectDetails[it]?.localId }
        localIds.chunked(MAX_OBJECTS_PER_PACKET).forEach { batch ->
            TODO("Send ObjectSelect / ObjectDeselect packets for batch $batch")
        }
    }

    // -----------------------------------------------------------------------
    // Object costs
    // -----------------------------------------------------------------------

    /**
     * Store streaming / physics cost data for [objectId] received from the
     * simulator.
     */
    fun updateObjectCosts(
        objectId: LLUUID,
        objectCost: Float,
        linkCost: Float,
        physicsCost: Float,
        linkPhysicsCost: Float,
    ) {
        TODO("Cache cost values and trigger list refresh")
    }

    // -----------------------------------------------------------------------
    // Avatar / group name resolution
    // -----------------------------------------------------------------------

    /**
     * Called by the avatar-name cache when the display name for [id] becomes
     * available.
     */
    fun avatarNameCacheCallback(id: LLUUID, avName: AvatarName) {
        TODO("Update matching objectDetails entries and refresh list rows")
    }

    /**
     * Called when the legacy (full-name) name cache resolves [id] to
     * [fullName].
     */
    fun callbackLoadFullName(id: LLUUID, fullName: String) {
        TODO("Update matching objectDetails entries and refresh list rows")
    }

    // -----------------------------------------------------------------------
    // Message handlers
    // -----------------------------------------------------------------------

    /**
     * Process an `ObjectProperties` message from the simulator, populating
     * the corresponding [FSObjectProperties] entry and triggering a list
     * update.
     */
    fun processObjectProperties(msg: Any /* LLMessageSystem stub */) {
        TODO("Parse ObjectProperties message fields and update objectDetails")
    }

    // -----------------------------------------------------------------------
    // Region / parcel change handling
    // -----------------------------------------------------------------------

    /**
     * Check whether the agent has changed region and, if so, clear stale
     * object data or handle a cross-region-border case.
     */
    fun checkRegion() {
        TODO("Compare current region to mLastRegion; clear cache on teleport")
    }

    // -----------------------------------------------------------------------
    // Filtering helpers
    // -----------------------------------------------------------------------

    /**
     * Returns `true` when [objectId] satisfies all active search terms and
     * filter flags.
     *
     * This is the Kotlin equivalent of `matchObject` + `isSearchableObject`.
     */
    fun matchesSearch(objectId: LLUUID): Boolean {
        val details = objectDetails[objectId] ?: return false
        if (query.name.isNotEmpty() && !fieldMatches(details.name, query.name)) return false
        if (query.description.isNotEmpty() && !fieldMatches(details.description, query.description)) return false
        // TODO: owner, group, creator, last-owner checks require name resolution
        return true
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Iterate the viewer object list and queue any new objects for property requests. */
    private fun findObjects() {
        if (!isActive) return
        // TODO: iterate gObjectList, apply isSearchableObject, enqueue new entries
        TODO("Walk gObjectList and add new entries to objectDetails")
    }

    /** Drain the pending property-request queue in batches. */
    private fun processRequestQueue() {
        if (requestQueuePaused) return
        TODO("Send queued ObjectSelect batches and mark entries as SENT")
    }

    /** Case-insensitive (or regex) match of [field] against [pattern]. */
    private fun fieldMatches(field: String, pattern: String): Boolean {
        return if (query.useRegex) {
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(field)
        } else {
            field.contains(pattern, ignoreCase = true)
        }
    }

    /** Update the on-screen counter (total searchable vs. matched). */
    private fun updateCounterText() {
        TODO("Notify UI panel with current matched / total counts")
    }
}

// ---------------------------------------------------------------------------
// Find-panel model
// ---------------------------------------------------------------------------

/**
 * View-model for the "Find" tab in the area-search floater.
 *
 * Holds the current text-field values; call [buildQuery] to produce an
 * [AreaSearchQuery] to pass to [FSAreaSearch.search].
 */
class AreaSearchFindPanel {
    var nameText: String = ""
    var descriptionText: String = ""
    var ownerText: String = ""
    var groupText: String = ""
    var creatorText: String = ""
    var lastOwnerText: String = ""
    var useRegex: Boolean = false

    fun buildQuery(): AreaSearchQuery = AreaSearchQuery(
        name = nameText,
        description = descriptionText,
        owner = ownerText,
        group = groupText,
        creator = creatorText,
        lastOwner = lastOwnerText,
        useRegex = useRegex,
    )

    fun clear() {
        nameText = ""; descriptionText = ""; ownerText = ""
        groupText = ""; creatorText = ""; lastOwnerText = ""
    }
}

// ---------------------------------------------------------------------------
// Advanced-panel model
// ---------------------------------------------------------------------------

/**
 * View-model for the "Advanced" tab — controls which click-action types are
 * included in the filter.
 *
 * Mirrors `FSPanelAreaSearchAdvanced` in C++.
 */
class AreaSearchAdvancedPanel {
    var filterClickTouch: Boolean = false
    var filterClickBuy: Boolean = false
    var filterClickSit: Boolean = false
}
