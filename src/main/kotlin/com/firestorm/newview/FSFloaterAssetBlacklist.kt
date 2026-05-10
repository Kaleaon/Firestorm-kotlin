package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// =============================================================================
// FSFloaterAssetBlacklist
// =============================================================================

/**
 * Floater that displays and manages the asset blacklist / derender list.
 *
 * Mirrors `FSFloaterAssetBlacklist` from `fsfloaterassetblacklist.h/.cpp`.
 * Periodic tick (every 250 ms) is used to detect when a previewed sound finishes.
 *
 * @param key LLSD construction key from the floater registry.
 */
class FSFloaterAssetBlacklist(val key: Any) {

    private var resultList: ScrollListCtrl? = null
    private var filterSubString: String = ""
    private var filterSubStringOrig: String = ""
    var audioSourceId: LLUUID = LLUUID.nullId()
        private set

    private var blacklistCallbackHandle: Any? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    fun postBuild(): Boolean {
        resultList = getChild("result_list")
        resultList?.contextMenu = AssetBlacklistMenu
        resultList?.filterColumn = 0
        resultList?.onSelectionChanged = { onSelectionChanged() }
        resultList?.commitOnSelectionChange = true

        setChildAction("remove_btn") { onRemoveBtn() }
        setChildAction("remove_temp_btn") { onRemoveAllTemporaryBtn() }
        setChildAction("play_btn") { onPlayBtn() }
        setChildAction("stop_btn") { onStopBtn() }
        setChildAction("close_btn") { onCloseBtn() }

        setFilterEditorCallback("filter_input") { searchString -> onFilterEdit(searchString) }

        blacklistCallbackHandle = FSAssetBlacklist.addChangeCallback { events, op ->
            onBlacklistChanged(events, op)
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
        TODO("Platform: LLFloater::closeFloater()")
    }

    // -------------------------------------------------------------------------
    // Tick — detect sound playback completion
    // -------------------------------------------------------------------------

    /**
     * Called on a ~250 ms timer.  Hides the Stop button and restores Play once
     * the audio source reports it has finished.
     *
     * @return `false` to keep the timer alive (mirrors C++ LLEventTimer semantics).
     */
    fun tick(): Boolean {
        if (audioSourceId.isNull()) return false

        val source = AudioEngine.findAudioSource(audioSourceId)
        if (source == null || source.isDone) {
            setChildVisible("play_btn", true)
            setChildVisible("stop_btn", false)
            audioSourceId = LLUUID.nullId()
            onSelectionChanged()
        }

        return false
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (FSCommon.isFilterEditorKeyCombo(key, mask)) {
            getChild<FilterEditor>("filter_input")?.requestFocus()
            return true
        }
        return false
    }

    // -------------------------------------------------------------------------
    // List population
    // -------------------------------------------------------------------------

    private fun buildBlacklist() {
        resultList?.isSorted?.also { needsSort ->
            resultList?.isSorted = false
            for ((id, data) in FSAssetBlacklist.getAll()) {
                addElementToList(id, data)
            }
            resultList?.isSorted = needsSort
            resultList?.updateSort()
        }
    }

    /**
     * Add one blacklist entry to the scroll list, expanding flag bits into
     * separate rows when the entry carries multiple flags.
     */
    fun addElementToList(id: LLUUID, data: BlacklistEntry) {
        val dateStr = formatDate(data.date)
        val lastFlagValue = BlacklistFlag.GESTURE   // highest single bit

        var flagValue = 1
        while (flagValue <= lastFlagValue) {
            if ((data.flags and flagValue) != 0 || data.flags == BlacklistFlag.NONE) {
                val flag = if (data.flags == BlacklistFlag.NONE) BlacklistFlag.NONE else flagValue

                val row = ScrollListRow(
                    id = id,
                    columns = listOf(
                        ScrollListColumn("name",      data.name.ifEmpty { getString("unknown_object") }),
                        ScrollListColumn("region",    data.region.ifEmpty { getString("unknown_region") }),
                        ScrollListColumn("type",      getTypeString(data.assetType.typeCode)),
                        ScrollListColumn("flags",     getFlagString(flag)),
                        ScrollListColumn("date",      dateStr),
                        ScrollListColumn("permanent", if (data.permanent) getString("asset_permanent") else "", halign = "center"),
                        ScrollListColumn("date_sort", data.date.toString()),
                        ScrollListColumn("asset_type", data.assetType.typeCode.toString()),
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
            flagsToRemoveById[item.id] = (flagsToRemoveById[item.id] ?: 0) or flag
        }

        for ((id, flags) in flagsToRemoveById) {
            if (flags == 0) {
                FSAssetBlacklist.removeFromBlacklist(id)
            } else {
                TODO("Platform: FSAssetBlacklist.removeFlagsFromItem(id, flags)")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Blacklist change callback
    // -------------------------------------------------------------------------

    private fun onBlacklistChanged(events: List<BlacklistChangeEvent>, op: BlacklistOperation) {
        if (op == BlacklistOperation.ADD) {
            val needsSort = resultList?.isSorted ?: false
            resultList?.isSorted = false
            for (event in events) {
                resultList?.deleteRows(event.id)
                if (event.entry != null) addElementToList(event.id, event.entry)
            }
            resultList?.isSorted = needsSort
            resultList?.updateSort()
        } else {
            for (event in events) {
                resultList?.deleteRows(event.id)
            }
            resultList?.updateLayout()
        }
    }

    // -------------------------------------------------------------------------
    // Button handlers
    // -------------------------------------------------------------------------

    private fun onRemoveBtn() { removeElements() }

    private fun onRemoveAllTemporaryBtn() {
        TODO("Platform: gObjectList.resetDerenderList(true)")
    }

    private fun onPlayBtn() {
        val item = resultList?.firstSelected() ?: return
        val assetTypeCol = resultList?.columnIndex("asset_type") ?: return
        if (item.id.isNull() || item.columns[assetTypeCol].value.toIntOrNull() != AssetType.SOUND.typeCode) return

        onStopBtn()
        audioSourceId = LLUUID.generateNewID()
        AudioEngine.triggerSound(
            soundId = item.id,
            ownerId = Agent.id,
            gain = 1.0f,
            audioType = AudioType.UI,
            sourceId = audioSourceId,
        )
        setChildVisible("stop_btn", true)
        setChildVisible("play_btn", false)
    }

    private fun onStopBtn() {
        if (audioSourceId.isNull()) return
        val source = AudioEngine.findAudioSource(audioSourceId)
        if (source != null && !source.isDone) {
            source.stop()
        }
    }

    private fun onCloseBtn() { closeFloater() }

    // -------------------------------------------------------------------------
    // Filter
    // -------------------------------------------------------------------------

    private fun onFilterEdit(searchString: String) {
        filterSubStringOrig = searchString.trimStart()
        val upper = filterSubStringOrig.uppercase()
        if (filterSubString == upper) return
        filterSubString = upper
        resultList?.setFilterString(filterSubStringOrig)
        onSelectionChanged()
    }

    // -------------------------------------------------------------------------
    // Selection state
    // -------------------------------------------------------------------------

    private fun onSelectionChanged() {
        val selected = resultList?.allSelected() ?: emptyList()
        val enable = selected.size == 1 &&
            run {
                val item = resultList?.firstSelected() ?: return@run false
                val col = resultList?.columnIndex("asset_type") ?: return@run false
                item.columns[col].value.toIntOrNull() == AssetType.SOUND.typeCode
            }
        setChildEnabled("play_btn", enable)
    }

    // -------------------------------------------------------------------------
    // String helpers
    // -------------------------------------------------------------------------

    private fun getTypeString(typeCode: Int): String = when (typeCode) {
        AssetType.TEXTURE.typeCode   -> getString("asset_texture")
        AssetType.OBJECT.typeCode    -> getString("asset_object")
        AssetType.ANIMATION.typeCode -> getString("asset_animation")
        AssetType.PERSON.typeCode    -> getString("asset_resident")
        AssetType.SOUND.typeCode     -> getString("asset_sound")
        else                         -> getString("asset_unknown")
    }

    private fun getFlagString(flag: Int): String = when (flag) {
        BlacklistFlag.NONE    -> getString("blacklist_flag_none")
        BlacklistFlag.WORN    -> getString("blacklist_flag_mute_avatar_worn_objects_sounds")
        BlacklistFlag.REZZED  -> getString("blacklist_flag_mute_avatar_rezzed_objects_sounds")
        BlacklistFlag.GESTURE -> getString("blacklist_flag_mute_avatar_gestures_sounds")
        else                  -> getString("blacklist_flag_unknown")
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    private fun getString(key: String): String =
        TODO("Platform: getString(\"$key\") from floater XUI strings")

    private fun formatDate(epochMs: Long): String =
        TODO("Platform: format epochMs using the floater's DateFormatString XUI string")

    private fun <T> getChild(name: String): T? =
        TODO("Platform: resolve child widget '$name'")

    private fun setChildAction(name: String, action: () -> Unit) =
        TODO("Platform: childSetAction(\"$name\", action)")

    private fun setFilterEditorCallback(name: String, cb: (String) -> Unit) =
        TODO("Platform: getChild<LLFilterEditor>(\"$name\").setCommitCallback(cb)")

    private fun setChildEnabled(name: String, enabled: Boolean) =
        TODO("Platform: childSetEnabled(\"$name\", $enabled)")

    private fun setChildVisible(name: String, visible: Boolean) =
        TODO("Platform: childSetVisible(\"$name\", $visible)")
}

// =============================================================================
// AssetBlacklistMenu — right-click context menu
// =============================================================================

/**
 * Context menu for the blacklist scroll list.
 *
 * Mirrors `FSFloaterAssetBlacklistMenu::FSAssetBlacklistMenu` from the C++ source.
 */
object AssetBlacklistMenu {

    fun createMenu(): Any {
        TODO("Platform: register Blacklist.Remove callback; " +
            "return createFromFile(\"menu_fs_asset_blacklist.xml\")")
    }

    fun onContextMenuItemClick(param: String) {
        if (param == "remove") {
            val floater = FloaterReg.findInstance<FSFloaterAssetBlacklist>("fs_asset_blacklist")
            floater?.removeElements()
        }
    }
}

// =============================================================================
// Stub types used only by this floater
// =============================================================================

/** Stub: scroll-list row descriptor. */
data class ScrollListColumn(
    val column: String,
    val value: String,
    val halign: String = "left",
)

data class ScrollListRow(
    val id: LLUUID,
    val columns: List<ScrollListColumn>,
    val altValue: Map<String, Any> = emptyMap(),
)

/** Stub: scroll-list widget. */
class ScrollListCtrl {
    var contextMenu: Any? = null
    var filterColumn: Int = 0
    var commitOnSelectionChange: Boolean = false
    var isSorted: Boolean = false
    var onSelectionChanged: (() -> Unit)? = null

    fun clearRows() = TODO("Platform: mResultList->clearRows()")
    fun addRow(row: ScrollListRow) = TODO("Platform: mResultList->addElement(element, ADD_BOTTOM)")
    fun deleteRows(id: LLUUID) = TODO("Platform: mResultList->deleteItems(id)")
    fun allSelected(): List<ScrollListRow> = TODO("Platform: mResultList->getAllSelected()")
    fun firstSelected(): ScrollListRow? = TODO("Platform: mResultList->getFirstSelected()")
    fun columnIndex(name: String): Int? = TODO("Platform: mResultList->getColumn(\"$name\")->mIndex")
    fun setFilterString(filter: String) = TODO("Platform: mResultList->setFilterString(filter)")
    fun updateSort() = TODO("Platform: mResultList->updateSort()")
    fun updateLayout() = TODO("Platform: mResultList->updateLayout()")
}

/** Stub: filter editor widget. */
class FilterEditor {
    fun requestFocus() = TODO("Platform: filter_input->setFocus(true)")
}

/** Stub: audio engine access. */
object AudioEngine {
    fun findAudioSource(id: LLUUID): AudioSource? = TODO("Platform: gAudiop->findAudioSource(id)")
    fun triggerSound(soundId: LLUUID, ownerId: LLUUID, gain: Float, audioType: AudioType, sourceId: LLUUID) =
        TODO("Platform: gAudiop->triggerSound(soundId, ownerId, gain, audioType, LLVector3d::zero, LLUUID::null, sourceId)")
}

/** Stub: audio source. */
class AudioSource {
    val isDone: Boolean get() = TODO("Platform: audio_source->isDone()")
    fun stop() = TODO("Platform: audio_source->play(LLUUID::null)")
}

enum class AudioType { UI, AMBIENT, OBJECT_MEDIA }

/** Stub: common Firestorm UI utilities. */
object FSCommon {
    fun isFilterEditorKeyCombo(key: Int, mask: Int): Boolean =
        TODO("Platform: FSCommon::isFilterEditorKeyCombo(key, mask)")
}

/** Stub: floater registry. */
object FloaterReg {
    inline fun <reified T> findInstance(name: String): T? =
        TODO("Platform: LLFloaterReg::findTypedInstance<T>(\"$name\")")
}
