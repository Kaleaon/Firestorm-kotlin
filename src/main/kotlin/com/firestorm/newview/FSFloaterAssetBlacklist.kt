package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import java.util.UUID

// =============================================================================
// FSFloaterAssetBlacklist
// =============================================================================

/**
 * Floater that displays and manages the asset blacklist / derender list.
 *
 * Mirrors `FSFloaterAssetBlacklist` from `fsfloaterassetblacklist.h/.cpp`.
 * Periodic tick (every 250 ms in C++) is used to detect when a previewed
 * sound finishes playing.
 *
 * @param key LLSD construction key from the floater registry.
 */
class FSFloaterAssetBlacklist(val key: Any) {

    private var resultList: ABScrollListCtrl? = null
    private var filterSubString: String = ""
    private var filterSubStringOrig: String = ""
    var audioSourceId: UUID = NULL_UUID
        private set

    private var blacklistCallbackHandle: Any? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    fun postBuild(): Boolean {
        resultList = getChild("result_list")
        resultList?.contextMenu = FSAssetBlacklistMenu
        resultList?.filterColumn = 0
        resultList?.commitOnSelectionChange = true
        resultList?.onSelectionChanged = { onSelectionChanged() }

        setChildAction("remove_btn")      { onRemoveBtn() }
        setChildAction("remove_temp_btn") { onRemoveAllTemporaryBtn() }
        setChildAction("play_btn")        { onPlayBtn() }
        setChildAction("stop_btn")        { onStopBtn() }
        setChildAction("close_btn")       { onCloseBtn() }

        setFilterEditorCallback("filter_input") { search -> onFilterEdit(search) }

        blacklistCallbackHandle = FSAssetBlacklist.setBlacklistChangedCallback { data, op ->
            onBlacklistChanged(data, op)
        }

        setChildEnabled("play_btn", false)
        setChildEnabled("stop_btn", true)
        setChildVisible("play_btn", true)
        setChildVisible("stop_btn", false)

        return true
    }

    fun onOpen(key: Any) {
        resultList?.clearRows()
        buildBlacklist()
    }

    fun closeFloater(appQuitting: Boolean = false) {
        onStopBtn()
        System.err.println("FSFloaterAssetBlacklist: closeFloater not yet implemented")
    }

    fun hasAccelerators(): Boolean = true

    // -------------------------------------------------------------------------
    // Tick — detect sound playback completion (~250 ms period)
    // -------------------------------------------------------------------------

    fun tick(): Boolean {
        if (audioSourceId == NULL_UUID) return false

        val source = ABLAudioEngine.findAudioSource(audioSourceId)
        if (source == null || source.isDone) {
            setChildVisible("play_btn", true)
            setChildVisible("stop_btn", false)
            audioSourceId = NULL_UUID
            onSelectionChanged()
        }

        return false
    }

    fun handleKeyHere(key: Char, mask: Int): Boolean {
        if (FSCommon.isFilterEditorKeyCombo(key, mask)) {
            getChild<ABFilterEditor>("filter_input")?.requestFocus()
            return true
        }
        System.err.println("FSFloaterAssetBlacklist: handleKeyHere not yet implemented")
        return false
    }

    // -------------------------------------------------------------------------
    // List population
    // -------------------------------------------------------------------------

    private fun buildBlacklist() {
        val needsSort = resultList?.isSorted ?: false
        resultList?.isSorted = false
        for ((id, data) in FSAssetBlacklist.getAll()) {
            addElementToList(id, data)
        }
        resultList?.isSorted = needsSort
        resultList?.updateSort()
    }

    /**
     * Adds one blacklist entry to the scroll list, expanding multi-flag entries
     * into separate rows so each flag bit appears on its own line.
     */
    fun addElementToList(id: LLUUID, data: BlacklistEntry) {
        val dateStr = formatDate(data.date)
        val lastFlagValue = BlacklistFlag.GESTURE

        var flagValue = 1
        while (flagValue <= lastFlagValue) {
            if ((data.flags and flagValue) != 0 || data.flags == BlacklistFlag.NONE) {
                val flag = if (data.flags == BlacklistFlag.NONE) BlacklistFlag.NONE else flagValue

                val row = ABScrollListRow(
                    id = id,
                    columns = listOf(
                        ABScrollListColumn("name",
                            data.name.ifEmpty { getString("unknown_object") }),
                        ABScrollListColumn("region",
                            data.region.ifEmpty { getString("unknown_region") }),
                        ABScrollListColumn("type",      getTypeString(data.assetType.typeCode)),
                        ABScrollListColumn("flags",     getFlagString(flag)),
                        ABScrollListColumn("date",      dateStr),
                        ABScrollListColumn("permanent",
                            if (data.permanent) getString("asset_permanent") else "",
                            halign = "center"),
                        ABScrollListColumn("date_sort", data.date.toString()),
                        ABScrollListColumn("asset_type", data.assetType.typeCode.toString()),
                    ),
                    altValue = mapOf("flag" to flag),
                )
                resultList?.addRow(row)

                if (data.flags == BlacklistFlag.NONE) break
            }
            flagValue = flagValue shl 1
        }
    }

    // -------------------------------------------------------------------------
    // Remove operations
    // -------------------------------------------------------------------------

    fun removeElements() {
        val flagsToRemoveById = mutableMapOf<LLUUID, Int>()

        resultList?.allSelected()?.forEach { item ->
            val flag = (item.altValue["flag"] as? Int) ?: 0
            if (flag == 0) {
                FSAssetBlacklist.removeItemsFromBlacklist(listOf(item.id))
            } else {
                flagsToRemoveById[item.id] = (flagsToRemoveById[item.id] ?: 0) or flag
            }
        }

        for ((id, flags) in flagsToRemoveById) {
            FSAssetBlacklist.removeFlagsFromItem(id, flags)
        }
    }

    // -------------------------------------------------------------------------
    // Blacklist change callback
    // -------------------------------------------------------------------------

    private fun onBlacklistChanged(
        data: List<BlacklistChangeEvent>,
        op: BlacklistOperation,
    ) {
        if (op == BlacklistOperation.ADD) {
            val needsSort = resultList?.isSorted ?: false
            resultList?.isSorted = false
            for (event in data) {
                resultList?.deleteRows(event.id)
                if (event.entry != null) addElementToList(event.id, event.entry)
            }
            resultList?.isSorted = needsSort
            resultList?.updateSort()
        } else {
            for (event in data) {
                resultList?.deleteRows(event.id)
            }
            resultList?.updateLayout()
        }
    }

    // -------------------------------------------------------------------------
    // Button handlers
    // -------------------------------------------------------------------------

    protected fun onRemoveBtn() { removeElements() }

    protected fun onRemoveAllTemporaryBtn() {
        System.err.println("FSFloaterAssetBlacklist: onRemoveAllTemporaryBtn not yet implemented")
    }

    protected fun onPlayBtn() {
        val item = resultList?.firstSelected() ?: return
        val assetTypeColIdx = resultList?.columnIndex("asset_type") ?: return
        val typeCode = item.columns.getOrNull(assetTypeColIdx)?.value?.toIntOrNull() ?: return

        if (item.id.uuid.toString() == NULL_UUID.toString() ||
            typeCode != AssetType.SOUND.typeCode
        ) return

        onStopBtn()
        audioSourceId = UUID.randomUUID()
        ABLAudioEngine.triggerSound(
            soundId  = item.id,
            ownerId  = LLUUID.NULL,
            gain     = 1.0f,
            audioType = ABLAudioType.UI,
            sourceId = audioSourceId,
        )
        setChildVisible("stop_btn", true)
        setChildVisible("play_btn", false)
    }

    protected fun onStopBtn() {
        if (audioSourceId == NULL_UUID) return
        val source = ABLAudioEngine.findAudioSource(audioSourceId)
        if (source != null && !source.isDone) {
            source.stop()
        }
    }

    protected fun onCloseBtn() { closeFloater() }

    protected fun onFilterEdit(searchString: String) {
        filterSubStringOrig = searchString.trimStart()
        val upper = filterSubStringOrig.uppercase()
        if (filterSubString == upper) return
        filterSubString = upper
        resultList?.setFilterString(filterSubStringOrig)
        onSelectionChanged()
    }

    protected fun onSelectionChanged() {
        val selected = resultList?.allSelected() ?: emptyList()
        val enable = selected.size == 1 && run {
            val item = resultList?.firstSelected() ?: return@run false
            val colIdx = resultList?.columnIndex("asset_type") ?: return@run false
            item.columns.getOrNull(colIdx)?.value?.toIntOrNull() == AssetType.SOUND.typeCode
        }
        setChildEnabled("play_btn", enable)
    }

    // -------------------------------------------------------------------------
    // String helpers
    // -------------------------------------------------------------------------

    protected fun getTypeString(typeCode: Int): String = when (typeCode) {
        AssetType.TEXTURE.typeCode   -> getString("asset_texture")
        AssetType.OBJECT.typeCode    -> getString("asset_object")
        AssetType.ANIMATION.typeCode -> getString("asset_animation")
        AssetType.PERSON.typeCode    -> getString("asset_resident")
        AssetType.SOUND.typeCode     -> getString("asset_sound")
        else                         -> getString("asset_unknown")
    }

    protected fun getFlagString(flag: Int): String = when (flag) {
        BlacklistFlag.NONE    -> getString("blacklist_flag_none")
        BlacklistFlag.WORN    -> getString("blacklist_flag_mute_avatar_worn_objects_sounds")
        BlacklistFlag.REZZED  -> getString("blacklist_flag_mute_avatar_rezzed_objects_sounds")
        BlacklistFlag.GESTURE -> getString("blacklist_flag_mute_avatar_gestures_sounds")
        else                  -> getString("blacklist_flag_unknown")
    }

    companion object {
        val NULL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        fun getFlagFromLLSD(data: Map<String, Any>): Int {
            val raw = data["asset_blacklist_flag"] as? Int ?: return BlacklistFlag.NONE
            return raw
        }
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    private fun getString(key: String): String {
        System.err.println("FSFloaterAssetBlacklist: getString not yet implemented")
        return ""
    }

    private fun formatDate(epochMs: Long): String {
        System.err.println("FSFloaterAssetBlacklist: formatDate not yet implemented")
        return ""
    }

    private fun <T> getChild(name: String): T? {
        System.err.println("FSFloaterAssetBlacklist: getChild not yet implemented")
        return null
    }

    private fun setChildAction(name: String, action: () -> Unit): Unit {
        System.err.println("FSFloaterAssetBlacklist: setChildAction not yet implemented")
    }

    private fun setFilterEditorCallback(name: String, cb: (String) -> Unit): Unit {
        System.err.println("FSFloaterAssetBlacklist: setFilterEditorCallback not yet implemented")
    }

    private fun setChildEnabled(name: String, enabled: Boolean): Unit {
        System.err.println("FSFloaterAssetBlacklist: setChildEnabled not yet implemented")
    }

    private fun setChildVisible(name: String, visible: Boolean): Unit {
        System.err.println("FSFloaterAssetBlacklist: setChildVisible not yet implemented")
    }
}

// =============================================================================
// FSAssetBlacklistMenu — right-click context menu
// =============================================================================

/**
 * Context menu for the blacklist scroll list.
 *
 * Mirrors `FSFloaterAssetBlacklistMenu::FSAssetBlacklistMenu`.
 */
object FSAssetBlacklistMenu {

    fun createMenu(): Any {
        System.err.println("FSAssetBlacklistMenu: createMenu not yet implemented")
        return Unit
    }

    fun onContextMenuItemClick(param: String) {
        if (param == "remove") {
            val floater = FloaterReg.findInstance("fs_asset_blacklist") as? FSFloaterAssetBlacklist
            floater?.removeElements()
        }
    }
}

// =============================================================================
// Stub types local to this file (prefixed "AB" to avoid collision)
// =============================================================================

/** Stub: scroll-list column descriptor. */
data class ABScrollListColumn(
    val column: String,
    val value: String,
    val halign: String = "left",
)

/** Stub: scroll-list row descriptor. */
data class ABScrollListRow(
    val id: LLUUID,
    val columns: List<ABScrollListColumn>,
    val altValue: Map<String, Any> = emptyMap(),
)

/** Stub: the FSScrollListCtrl widget as used by this floater. */
class ABScrollListCtrl {
    var contextMenu: Any? = null
    var filterColumn: Int = 0
    var commitOnSelectionChange: Boolean = false
    var isSorted: Boolean = false
    var onSelectionChanged: (() -> Unit)? = null

    fun clearRows(): Unit { System.err.println("ABScrollListCtrl: clearRows not yet implemented") }
    fun addRow(row: ABScrollListRow): Unit { System.err.println("ABScrollListCtrl: addRow not yet implemented") }
    fun deleteRows(id: LLUUID): Unit { System.err.println("ABScrollListCtrl: deleteRows not yet implemented") }
    fun allSelected(): List<ABScrollListRow> { System.err.println("ABScrollListCtrl: allSelected not yet implemented"); return emptyList() }
    fun firstSelected(): ABScrollListRow? { System.err.println("ABScrollListCtrl: firstSelected not yet implemented"); return null }
    fun columnIndex(name: String): Int? { System.err.println("ABScrollListCtrl: columnIndex not yet implemented"); return null }
    fun setFilterString(filter: String): Unit { System.err.println("ABScrollListCtrl: setFilterString not yet implemented") }
    fun updateSort(): Unit { System.err.println("ABScrollListCtrl: updateSort not yet implemented") }
    fun updateLayout(): Unit { System.err.println("ABScrollListCtrl: updateLayout not yet implemented") }
    fun setNeedsSort(value: Boolean): Unit { System.err.println("ABScrollListCtrl: setNeedsSort not yet implemented") }
}

/** Stub: filter editor widget. */
class ABFilterEditor {
    fun requestFocus(): Unit { System.err.println("ABFilterEditor: requestFocus not yet implemented") }
}

/** Stub: audio engine as used by this floater. */
object ABLAudioEngine {
    fun findAudioSource(id: UUID): ABLAudioSource? {
        System.err.println("ABLAudioEngine: findAudioSource not yet implemented")
        return null
    }
    fun triggerSound(
        soundId: LLUUID, ownerId: LLUUID, gain: Float,
        audioType: ABLAudioType, sourceId: UUID,
    ): Unit {
        System.err.println("ABLAudioEngine: triggerSound not yet implemented")
    }
}

/** Stub: audio source. */
class ABLAudioSource {
    val isDone: Boolean get() { System.err.println("ABLAudioSource: isDone not yet implemented"); return false }
    fun stop(): Unit { System.err.println("ABLAudioSource: stop not yet implemented") }
}

enum class ABLAudioType { UI, AMBIENT, OBJECT_MEDIA }
