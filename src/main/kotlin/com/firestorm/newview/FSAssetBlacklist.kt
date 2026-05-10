/**
 * @file FSAssetBlacklist.kt
 * @brief Asset blacklist and derender management.
 *
 * Ported from fsassetblacklist.h / fsassetblacklist.cpp
 * Original copyright (C) 2012 Wolfspirit Magic / 2016 Ansariel Hiller — LGPL v2.1
 *
 * The C++ implementation uses XOR-encrypted UUIDs in the on-disk XML file and
 * supports legacy Phoenix data import.  Both behaviours are stubbed here
 * pending the port of the file-system and serialisation layers.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Asset-type enum  (subset of LLAssetType::EType used by the blacklist)
// ---------------------------------------------------------------------------

/**
 * Asset-type codes used by the blacklist, mirroring the relevant members of
 * `LLAssetType::EType` from the C++ viewer.
 */
enum class AssetType(val typeCode: Int) {
    NONE(-1),
    TEXTURE(0),
    SOUND(1),
    OBJECT(6),
    ANIMATION(20),
    PERSON(45),
    ;

    companion object {
        /** Convert a raw integer code to [AssetType], returning [NONE] for unknowns. */
        fun fromCode(code: Int): AssetType =
            entries.firstOrNull { it.typeCode == code } ?: NONE
    }
}

// ---------------------------------------------------------------------------
// Blacklist flags
// ---------------------------------------------------------------------------

/**
 * Bit-field flags that qualify *why* an asset is blacklisted.
 *
 * Mirrors `FSAssetBlacklist::eBlacklistFlag` from the C++ header.
 */
object BlacklistFlag {
    const val NONE: Int = 0
    const val WORN: Int = 1 shl 0
    const val REZZED: Int = 1 shl 1
    const val GESTURE: Int = 1 shl 2
}

// ---------------------------------------------------------------------------
// Data class
// ---------------------------------------------------------------------------

/**
 * Full record stored for a single blacklisted asset.
 *
 * Mirrors `FSAssetBlacklistData` from the C++ header.
 *
 * @param assetId   UUID of the blocked asset.
 * @param assetType Category of the asset (texture, sound, object, …).
 * @param name      Human-readable label, shown in the Derender floater.
 * @param region    Name of the region where the asset was encountered.
 * @param flags     Bit-field combination of [BlacklistFlag] constants.
 * @param date      Unix epoch milliseconds when the entry was created.
 * @param permanent If `false` the entry is session-only and not persisted.
 */
data class BlacklistEntry(
    val assetId: LLUUID,
    val assetType: AssetType,
    val name: String,
    val region: String = "",
    val flags: Int = BlacklistFlag.NONE,
    val date: Long = System.currentTimeMillis(),
    val permanent: Boolean = true,
)

// ---------------------------------------------------------------------------
// Change-event types
// ---------------------------------------------------------------------------

/** Identifies whether a blacklist-changed callback fired for an add or remove. */
enum class BlacklistOperation { ADD, REMOVE }

/**
 * Payload delivered to [FSAssetBlacklist.BlacklistChangedCallback] listeners.
 *
 * @param id    Asset UUID affected.
 * @param entry The new entry data on ADD, or `null` on REMOVE.
 */
data class BlacklistChangeEvent(val id: LLUUID, val entry: BlacklistEntry?)

/** Functional interface for blacklist-change listeners. */
fun interface BlacklistChangedCallback {
    fun onChanged(events: List<BlacklistChangeEvent>, operation: BlacklistOperation)
}

// ---------------------------------------------------------------------------
// Singleton  (C++ LLSingleton<FSAssetBlacklist> → Kotlin object)
// ---------------------------------------------------------------------------

/**
 * Singleton that tracks which asset UUIDs should be blocked from rendering
 * or playback.
 *
 * ### Internal layout (mirrors C++)
 * - `blacklistData`        — primary map: UUID → [BlacklistEntry]
 * - `blacklistByType`      — secondary index: [AssetType] → Set<UUID>
 *   (for the hot-path `isBlacklisted` check, which filters by type first).
 *
 * ### Thread safety
 * All mutations are gated on `synchronized(lock)`.  Read-only queries
 * (`isBlacklisted`, `getAll`) take the same lock to ensure a consistent view.
 */
object FSAssetBlacklist {

    private val lock = Any()

    /** Primary store: asset UUID → full [BlacklistEntry]. */
    private val blacklistData: MutableMap<LLUUID, BlacklistEntry> = mutableMapOf()

    /** Secondary index for the O(1) type-filtered lookup in [isBlacklisted]. */
    private val blacklistByType: MutableMap<AssetType, MutableSet<LLUUID>> = mutableMapOf()

    /** Registered change listeners, mirroring `mBlacklistChangedCallback`. */
    private val changeCallbacks: MutableList<BlacklistChangedCallback> = mutableListOf()

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Resolve the on-disk filename and load saved entries.
     *
     * C++ equivalent: `FSAssetBlacklist::init()` → `loadBlacklist()`.
     */
    fun init() {
        TODO("Resolve per-account 'asset_blacklist.xml' path and call loadBlacklist()")
    }

    // -----------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------

    /**
     * Return `true` if [id] is blocked for [assetType] with at least the
     * given [flag] bits set (or no flag restriction when [flag] is [BlacklistFlag.NONE]).
     *
     * C++ equivalent: `FSAssetBlacklist::isBlacklisted(id, type, flag)`.
     */
    fun isBlacklisted(
        id: LLUUID,
        assetType: AssetType,
        flag: Int = BlacklistFlag.NONE,
    ): Boolean = synchronized(lock) {
        val typeSet = blacklistByType[assetType] ?: return false
        if (id !in typeSet) return false
        val entry = blacklistData[id] ?: return false
        return (entry.flags == BlacklistFlag.NONE && flag == BlacklistFlag.NONE) ||
            (entry.flags and flag) != 0
    }

    /** Return a snapshot of all current [BlacklistEntry] records. */
    fun getAll(): Map<LLUUID, BlacklistEntry> = synchronized(lock) { blacklistData.toMap() }

    // -----------------------------------------------------------------------
    // Mutation
    // -----------------------------------------------------------------------

    /**
     * Add or update the blacklist entry for [id].
     *
     * If an entry already exists its [BlacklistEntry.flags] are OR-merged with
     * the new [flags]; otherwise a fresh entry is created.
     *
     * C++ equivalent: `FSAssetBlacklist::addNewItemToBlacklist(...)`.
     *
     * @param save Persist to disk immediately when `true`.
     */
    fun addToBlacklist(
        id: LLUUID,
        assetType: AssetType,
        name: String,
        region: String = "",
        flags: Int = BlacklistFlag.NONE,
        permanent: Boolean = true,
        save: Boolean = true,
    ) {
        val entry = synchronized(lock) {
            val existing = blacklistData[id]
            val merged = if (existing != null) {
                existing.copy(name = name, region = region, flags = existing.flags or flags,
                    permanent = permanent, date = System.currentTimeMillis())
            } else {
                BlacklistEntry(assetId = id, assetType = assetType, name = name,
                    region = region, flags = flags, permanent = permanent)
            }
            blacklistData[id] = merged
            blacklistByType.getOrPut(assetType) { mutableSetOf() }.add(id)
            merged
        }
        if (save) saveBlacklist()
        fireChanged(listOf(BlacklistChangeEvent(id, entry)), BlacklistOperation.ADD)
    }

    /**
     * Convenience overload that accepts a pre-built [BlacklistEntry].
     *
     * C++ equivalent: `FSAssetBlacklist::addNewItemToBlacklistData(id, data, save)`.
     */
    fun addToBlacklist(entry: BlacklistEntry, save: Boolean = true) {
        addToBlacklist(
            id = entry.assetId,
            assetType = entry.assetType,
            name = entry.name,
            region = entry.region,
            flags = entry.flags,
            permanent = entry.permanent,
            save = save,
        )
    }

    /**
     * Remove the entry for [id] from the blacklist.
     *
     * C++ equivalent: `FSAssetBlacklist::removeItemFromBlacklist(id)`.
     */
    fun removeFromBlacklist(id: LLUUID) {
        removeFromBlacklist(listOf(id))
    }

    /**
     * Batch-remove multiple entries.
     *
     * C++ equivalent: `FSAssetBlacklist::removeItemsFromBlacklist(ids)`.
     */
    fun removeFromBlacklist(ids: List<LLUUID>) {
        if (ids.isEmpty()) return
        val events = mutableListOf<BlacklistChangeEvent>()
        var needSave = false
        synchronized(lock) {
            for (id in ids) {
                val entry = blacklistData.remove(id) ?: continue
                blacklistByType[entry.assetType]?.remove(id)
                events.add(BlacklistChangeEvent(id, null))
                if (entry.permanent) needSave = true
            }
        }
        if (needSave) saveBlacklist()
        if (events.isNotEmpty()) fireChanged(events, BlacklistOperation.REMOVE)
    }

    // -----------------------------------------------------------------------
    // Persistence (stubs)
    // -----------------------------------------------------------------------

    /**
     * Deserialise the on-disk XML file into [blacklistData] / [blacklistByType].
     *
     * The C++ implementation XOR-decrypts each UUID key with a magic UUID before
     * inserting it.  That cipher logic should be reproduced here once the
     * file-system layer is ported.
     *
     * C++ equivalent: `FSAssetBlacklist::loadBlacklist()`.
     */
    fun loadBlacklist() {
        TODO("Read per-account 'asset_blacklist.xml', XOR-decrypt UUID keys, and populate blacklistData")
    }

    /**
     * Serialise all permanent entries to the on-disk XML file.
     *
     * C++ equivalent: `FSAssetBlacklist::saveBlacklist()`.
     */
    fun saveBlacklist() {
        TODO("XOR-encrypt UUID keys and write permanent entries to 'asset_blacklist.xml'")
    }

    // -----------------------------------------------------------------------
    // Callback registration
    // -----------------------------------------------------------------------

    /** Register [callback] to be notified on every add / remove operation. */
    fun addChangeCallback(callback: BlacklistChangedCallback) {
        synchronized(changeCallbacks) { changeCallbacks.add(callback) }
    }

    /** Unregister a previously-added [callback]. */
    fun removeChangeCallback(callback: BlacklistChangedCallback) {
        synchronized(changeCallbacks) { changeCallbacks.remove(callback) }
    }

    private fun fireChanged(events: List<BlacklistChangeEvent>, op: BlacklistOperation) {
        val snapshot = synchronized(changeCallbacks) { changeCallbacks.toList() }
        for (cb in snapshot) cb.onChanged(events, op)
    }
}
