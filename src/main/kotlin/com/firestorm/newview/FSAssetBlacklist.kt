package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Asset-type enum  (mirrors LLAssetType::EType subset used by the blacklist)
// ---------------------------------------------------------------------------

enum class AssetType(val typeCode: Int) {
    NONE(-1),
    TEXTURE(0),
    SOUND(1),
    OBJECT(6),
    ANIMATION(20),
    PERSON(45);

    companion object {
        fun fromCode(code: Int): AssetType =
            entries.firstOrNull { it.typeCode == code } ?: NONE
    }
}

// ---------------------------------------------------------------------------
// Blacklist-flag constants  (mirrors eBlacklistFlag)
// ---------------------------------------------------------------------------

object BlacklistFlag {
    const val NONE: Int    = 0
    const val WORN: Int    = 1 shl 0
    const val REZZED: Int  = 1 shl 1
    const val GESTURE: Int = 1 shl 2
}

// ---------------------------------------------------------------------------
// Per-entry data  (mirrors FSAssetBlacklistData)
// ---------------------------------------------------------------------------

data class FSAssetBlacklistData(
    val name: String = "",
    val region: String = "",
    val type: AssetType = AssetType.NONE,
    val flags: Int = BlacklistFlag.NONE,
    val date: Long = System.currentTimeMillis(),
    val permanent: Boolean = false
) {
    fun toLLSD(): Map<String, Any> {
        val inputDate = TODO("APR: use JVM equivalent - format date as 'yyyy-MM-dd HH:mm:ss' (strip trailing Z, replace T with space)") as String
        return mapOf(
            "asset_name"           to name,
            "asset_region"         to region,
            "asset_type"           to type.typeCode,
            "asset_blacklist_flag" to flags,
            "asset_date"           to inputDate,
            "asset_permanent"      to permanent
        )
    }

    companion object {
        fun fromLLSD(data: Map<String, Any>): FSAssetBlacklistData {
            val rawDate = (data["asset_date"] as? String ?: "") + "Z"
            val isoDate = rawDate.replace(" ", "T")
            return FSAssetBlacklistData(
                name      = data["asset_name"] as? String ?: "",
                region    = data["asset_region"] as? String ?: "",
                type      = AssetType.fromCode((data["asset_type"] as? Number)?.toInt() ?: -1),
                flags     = (data["asset_blacklist_flag"] as? Number)?.toInt() ?: 0,
                date      = TODO("APR: use JVM equivalent - parse ISO-8601 string '$isoDate' to epoch millis") as Long,
                permanent = data["asset_permanent"] as? Boolean ?: false
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Change-event types  (mirrors eBlacklistOperation / changed_signal_data_t)
// ---------------------------------------------------------------------------

enum class BlacklistOperation { BLACKLIST_ADD, BLACKLIST_REMOVE }

data class BlacklistChangeEvent(val id: UUID, val data: FSAssetBlacklistData?)

typealias BlacklistChangedCallback = (events: List<BlacklistChangeEvent>, op: BlacklistOperation) -> Unit

// ---------------------------------------------------------------------------
// Singleton  (LLSingleton<FSAssetBlacklist> → Kotlin object)
// ---------------------------------------------------------------------------

object FSAssetBlacklist {

    // Primary store: UUID → FSAssetBlacklistData
    private val blacklistData: MutableMap<UUID, FSAssetBlacklistData> = mutableMapOf()

    // Secondary index: AssetType → Set<UUID>  (fast isBlacklisted check)
    private val blacklistTypeContainer: MutableMap<AssetType, MutableSet<UUID>> = mutableMapOf()

    // Registered signal listeners  (mirrors mBlacklistChangedCallback)
    private val changeCallbacks: MutableList<BlacklistChangedCallback> = mutableListOf()

    private var blacklistFileName: String = ""

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun init() {
        blacklistFileName = TODO("APR: use JVM equivalent - expand per-SL-account path to 'asset_blacklist.xml'") as String
        loadBlacklist()
    }

    // -----------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------

    fun isBlacklisted(id: UUID, type: AssetType, flag: Int = BlacklistFlag.NONE): Boolean {
        if (blacklistData.isEmpty()) return false
        val typeSet = blacklistTypeContainer[type] ?: return false
        if (id !in typeSet) return false
        val entry = blacklistData[id] ?: return false
        return (entry.flags == BlacklistFlag.NONE && flag == BlacklistFlag.NONE) ||
            (entry.flags and flag) != 0
    }

    fun getBlacklistData(): Map<UUID, FSAssetBlacklistData> = blacklistData.toMap()

    // -----------------------------------------------------------------------
    // Mutation
    // -----------------------------------------------------------------------

    fun addNewItemToBlacklist(
        id: UUID,
        name: String,
        region: String,
        type: AssetType,
        flag: Int = BlacklistFlag.NONE,
        permanent: Boolean = true,
        save: Boolean = true
    ) {
        val existing = blacklistData[id]
        val newData = if (existing != null) {
            existing.copy(name = name, region = region,
                date = System.currentTimeMillis(), permanent = permanent,
                flags = existing.flags or flag)
        } else {
            FSAssetBlacklistData(name = name, region = region,
                date = System.currentTimeMillis(), permanent = permanent,
                flags = flag, type = type)
        }
        addNewItemToBlacklistData(id, newData, save)
    }

    fun addNewItemToBlacklistData(id: UUID, data: FSAssetBlacklistData, save: Boolean = true) {
        val isNew = id !in blacklistData
        if (!isNew) {
            blacklistData[id] = data
        } else {
            addEntryToBlacklistMap(id, data.type)
            blacklistData[id] = data
        }

        if (data.type == AssetType.SOUND && data.flags == BlacklistFlag.NONE) {
            TODO("APR: use JVM equivalent - remove cached sound file for id from filesystem")
        }

        if (save) saveBlacklist()

        if (changeCallbacks.isNotEmpty()) {
            val events = listOf(BlacklistChangeEvent(id, data))
            changeCallbacks.forEach { it(events, BlacklistOperation.BLACKLIST_ADD) }
        }
    }

    fun removeItemFromBlacklist(id: UUID) {
        removeItemsFromBlacklist(listOf(id))
    }

    fun removeItemsFromBlacklist(ids: List<UUID>) {
        if (ids.isEmpty()) return
        var needSave = false
        val events = mutableListOf<BlacklistChangeEvent>()
        for (id in ids) {
            if (removeItem(id)) needSave = true
            events.add(BlacklistChangeEvent(id, null))
        }
        if (needSave) saveBlacklist()
        if (changeCallbacks.isNotEmpty()) {
            changeCallbacks.forEach { it(events, BlacklistOperation.BLACKLIST_REMOVE) }
        }
    }

    fun removeFlagsFromItem(id: UUID, combinedFlags: Int) {
        val entry = blacklistData[id] ?: return
        val newFlags = entry.flags and combinedFlags.inv()
        if (newFlags == BlacklistFlag.NONE) {
            removeItemsFromBlacklist(listOf(id))
        } else {
            addNewItemToBlacklistData(id, entry.copy(flags = newFlags), true)
        }
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    fun saveBlacklist() {
        val saveData = mutableMapOf<String, Any>()
        for ((id, data) in blacklistData) {
            if (data.permanent) {
                val shadowId = xorEncryptUUID(id)
                saveData[shadowId.toString()] = data.toLLSD()
            }
        }
        TODO("APR: use JVM equivalent - serialize saveData to pretty XML and write to blacklistFileName")
    }

    // -----------------------------------------------------------------------
    // Callback registration  (mirrors setBlacklistChangedCallback)
    // -----------------------------------------------------------------------

    fun addChangeCallback(callback: BlacklistChangedCallback): BlacklistChangedCallback {
        changeCallbacks.add(callback)
        return callback
    }

    fun removeChangeCallback(callback: BlacklistChangedCallback) {
        changeCallbacks.remove(callback)
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun loadBlacklist() {
        val fileExists = TODO("APR: use JVM equivalent - check blacklistFileName exists") as Boolean
        if (fileExists) {
            TODO("APR: use JVM equivalent - parse XML from blacklistFileName; XOR-decrypt each UUID key; call addNewItemToBlacklistData for each valid entry with save=false")
        } else {
            val oldFile = TODO("APR: use JVM equivalent - build legacy Phoenix floater_blist_settings.xml path") as String
            val oldFileExists = TODO("APR: use JVM equivalent - check oldFile exists") as Boolean
            if (oldFileExists) {
                TODO("APR: use JVM equivalent - parse oldFile XML; migrate entries prepending '[PHOENIX] ' to names; call saveBlacklist()")
            }
        }
    }

    private fun removeItem(id: UUID): Boolean {
        TODO("GPU: gObjectList.removeDerenderedItem(id)")
        val entry = blacklistData.remove(id) ?: return false
        for ((_, container) in blacklistTypeContainer) {
            container.remove(id)
        }
        return entry.permanent
    }

    private fun addEntryToBlacklistMap(id: UUID, type: AssetType): Boolean {
        if (id == UUID(0L, 0L)) return false
        blacklistTypeContainer.getOrPut(type) { mutableSetOf() }.add(id)
        return true
    }

    // XOR-cipher used to obfuscate UUIDs on disk (mirrors LLXORCipher with MAGIC_ID).
    private val MAGIC_ID: UUID = UUID.fromString("3c115e51-04f4-523c-9fa6-98aff1034730")
    private fun xorEncryptUUID(id: UUID): UUID {
        TODO("APR: use JVM equivalent - XOR id bytes with MAGIC_ID bytes (same as LLXORCipher)")
    }
}
