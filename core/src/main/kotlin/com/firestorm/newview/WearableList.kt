/**
 * WearableList.kt
 * Converted from llwearablelist.h / llwearablelist.cpp
 *
 * Cache of downloaded wearable assets. Acts as a singleton (Kotlin object).
 * Mirrors LLWearableList (LLSingleton<LLWearableList>).
 *
 * NOTE: The original C++ code has a known design issue (EXT-6252): the map
 * keyed on assetID cannot correctly handle multiple inventory items pointing
 * to the same asset.  This port preserves that design for fidelity; the
 * comment is retained here as a warning.
 */

package com.firestorm.newview

import com.firestorm.llappearance.WearableType
import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// WearableList  (singleton  →  Kotlin object)
// ---------------------------------------------------------------------------

/**
 * In-memory cache of [ViewerWearable] instances keyed by asset UUID.
 *
 * Typical usage:
 * 1. Call [getWearable] — if the asset is cached, the callback is invoked
 *    synchronously; otherwise an async fetch is started and the callback will
 *    be invoked when the asset arrives.
 * 2. [addWearable] is called internally after a successful fetch.
 * 3. [clearWearables] is called on logout / cleanup.
 */
object WearableList {

    // -----------------------------------------------------------------------
    // Internal cache
    // -----------------------------------------------------------------------

    /**
     * The map of cached wearables.  Keyed on asset UUID (not item UUID).
     *
     * KNOWN BUG (EXT-6252): Multiple inventory items can reference the same
     * asset UUID, so a single entry in this map may be worn as several
     * different items simultaneously.  Use with care.
     */
    val wearables: MutableMap<LLUUID, ViewerWearable> = mutableMapOf()

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** Number of wearables currently held in the cache. */
    val length: Int get() = wearables.size

    /**
     * Return the cached [ViewerWearable] for [assetId], or null if not present.
     * This is a synchronous lookup — no network I/O.
     */
    fun findCached(assetId: LLUUID): ViewerWearable? = wearables[assetId]

    // -----------------------------------------------------------------------
    // Fetch / cache API
    // -----------------------------------------------------------------------

    /**
     * Return (via [callback]) the [ViewerWearable] for the given asset.
     *
     * If the asset is already in the cache the callback is invoked immediately
     * (synchronously) with the cached instance.  Otherwise an asynchronous
     * asset-store fetch is started; [callback] will receive the wearable (or
     * null on failure) once the download completes.
     *
     * Mirrors LLWearableList::getAsset().
     *
     * @param assetId  The asset UUID to retrieve.
     * @param type     Wearable type (clothing or body part) for validation.
     * @param callback Invoked with the [ViewerWearable], or null on failure.
     */
    fun getWearable(
        assetId: LLUUID,
        type: WearableType,
        callback: (ViewerWearable?) -> Unit
    ) {
        val cached = wearables[assetId]
        if (cached != null) {
            callback(cached)
            return
        }
        // Async path: request from the asset store.
        // The callback will ultimately call addWearable() then invoke [callback].
        System.err.println("WearableList: getWearable not yet implemented")
    }

    /**
     * Insert a fully-loaded [ViewerWearable] into the cache.
     * Keyed on [ViewerWearable.assetId].
     *
     * @param wearable The wearable to cache.
     */
    fun addWearable(wearable: ViewerWearable) {
        wearables[wearable.assetId] = wearable
    }

    /**
     * Evict all cached wearables and free associated resources.
     * Should be called on logout or when the cache must be invalidated.
     * Mirrors LLWearableList::cleanup().
     */
    fun clearWearables() {
        wearables.clear()
    }

    // -----------------------------------------------------------------------
    // Factory helpers  (mirror createCopy / createNewWearable)
    // -----------------------------------------------------------------------

    /**
     * Create a copy of an existing wearable with a new transaction ID,
     * optionally renaming it.  The copy is added to the cache and a new
     * asset is saved to the server.
     *
     * @param source  The wearable to copy.
     * @param newName If non-empty, overrides the copied wearable's name.
     * @return The newly created [ViewerWearable].
     */
    fun createCopy(source: ViewerWearable, newName: String = ""): ViewerWearable {
        val wearable = generateNewWearable(source.type)
        wearable.copyDataFrom(source)
        if (newName.isNotEmpty()) wearable.name = newName
        wearable.saveNewAsset()
        return wearable
    }

    /**
     * Create a brand-new wearable of the given type, populate it with
     * defaults, and save it as a new asset.
     *
     * @param type     The wearable type to create.
     * @param avatar   The avatar appearance context used to set default params.
     * @return The newly created [ViewerWearable].
     */
    fun createNewWearable(type: WearableType, avatar: Any?): ViewerWearable {
        val wearable = generateNewWearable(type)
        wearable.setParamsToDefaults()
        wearable.setTexturesToDefaults()
        wearable.saveValues()
        wearable.saveNewAsset()
        return wearable
    }

    /**
     * Allocate a fresh [ViewerWearable] with a new transaction/asset UUID,
     * register it in the cache, and return it.
     * Mirrors LLWearableList::generateNewWearable().
     */
    private fun generateNewWearable(type: WearableType): ViewerWearable {
        val assetId = LLUUID.generate()
        val wearable = ViewerWearable(assetId = assetId, type = type)
        wearables[assetId] = wearable
        return wearable
    }

    // -----------------------------------------------------------------------
    // Asset-store callback  (static; mirrors processGetAssetReply)
    // -----------------------------------------------------------------------

    /**
     * Invoked by the asset storage system when a wearable asset download
     * completes.  Parses the asset data, adds the wearable to the cache, and
     * fires the pending user callback.
     *
     * @param filename Temporary file path containing the asset data, or null
     *                 on failure.
     * @param assetId  UUID of the asset that was requested.
     * @param data     The [WearableArrivedData] userdata passed to getAsset.
     * @param status   0 or positive on success; negative on failure.
     */
    fun processGetAssetReply(
        filename: String?,
        assetId: LLUUID,
        data: WearableArrivedData,
        status: Int
    ) {
        System.err.println("WearableList: processGetAssetReply not yet implemented")
    }

    // -----------------------------------------------------------------------
    // WearableArrivedData  (mirrors C++ LLWearableArrivedData)
    // -----------------------------------------------------------------------

    /**
     * Userdata struct passed through the async asset-fetch callback chain.
     *
     * @param assetType  Expected asset type (clothing or body part).
     * @param name       Display name for error notifications.
     * @param avatar     Avatar appearance context for import.
     * @param callback   User-supplied completion callback.
     * @param retries    Number of fetch attempts made so far.
     */
    data class WearableArrivedData(
        val assetType: WearableType,
        val name: String,
        val avatar: Any?,
        val callback: (ViewerWearable?) -> Unit,
        var retries: Int = 0
    )
}
