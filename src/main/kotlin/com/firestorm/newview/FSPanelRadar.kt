package com.firestorm.newview

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLUUID

class FSPanelRadar {

    private var radarList: Any? = null
    private var miniMap: Any? = null
    private var radarGearButton: Any? = null
    private var addFriendButton: Any? = null
    private var optionsButton: Any? = null
    private var filterEditor: Any? = null
    private var optionsMenuHandle: Any? = null
    private var buttonsUpdater: ButtonsUpdater? = null

    private var filterSubString: String = ""
    private var filterSubStringOrig: String = ""
    private var lastResizeDelta: Int = 0

    private val columnBits: MutableMap<String, UInt> = mutableMapOf(
        "name"           to 1u,
        "voice_level"    to 2u,
        "in_region"      to 4u,
        "typing_status"  to 8u,
        "sitting_status" to 16u,
        "flags"          to 32u,
        "age"            to 64u,
        "seen"           to 128u,
        "range"          to 256u,
        "has_notes"      to 512u
    )

    private val radarUpdateCallbacks: MutableList<() -> Unit> = mutableListOf()
    private val columnConfigCallbacks: MutableList<() -> Unit> = mutableListOf()
    private val changeSignal: MutableList<() -> Unit> = mutableListOf()
    private var visibleCheckFunction: (() -> Boolean)? = null

    init {
        buttonsUpdater = ButtonsUpdater { updateButtons() }
    }

    fun postBuild(): Boolean {
        TODO(
            "Bind radarList child widget; set filter column 0 and context menu; " +
            "wire doubleClick and commit callbacks; bind miniMap, addFriendButton, filterEditor; " +
            "register Radar.Option.Action, NameFmt, ReportTo, ToggleColumn menu callbacks; " +
            "create and attach options menu from menu_fs_radar_options.xml; " +
            "listen to FSRadarColumnConfig setting; subscribe to FSRadar update signal; " +
            "call onColumnDisplayModeChanged() and updateButtons()"
        )
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        TODO("If FSCommon.isFilterEditorKeyCombo(key, mask): focus filterEditor; return true. Else super")
    }

    fun requestUpdate() {
        TODO("Fetch current radar data from FSRadar.getCurrentData(entries, stats); call updateList(entries, stats)")
    }

    fun getCurrentItemID(): LLUUID {
        TODO("Return radarList.getFirstSelected()?.getUUID() ?: LLUUID.null")
    }

    fun getCurrentItemIDs(selectedUuids: MutableList<LLUUID>) {
        TODO("Populate selectedUuids from radarList.getAllSelected()")
    }

    fun setChangeCallback(cb: () -> Unit) {
        changeSignal.add(cb)
    }

    fun setVisibleCheckFunction(func: () -> Boolean) {
        visibleCheckFunction = func
    }

    private fun updateButtons() {
        val selectedUuids = mutableListOf<LLUUID>()
        getCurrentItemIDs(selectedUuids)
        val itemSelected = selectedUuids.size == 1
        var isFriend = true
        if (itemSelected) {
            val selectedId = selectedUuids.first()
            TODO("isFriend = LLAvatarTracker.instance().getBuddyInfo(selectedId) != null")
        }
        TODO(
            "addFriendButton.setEnabled(!isFriend && !gRlvHandler.hasBehaviour(RLV_BHVR_SHOWNAMES)); " +
            "radarGearButton.setEnabled(selectedUuids.isNotEmpty() && !gRlvHandler.hasBehaviour(RLV_BHVR_SHOWNAMES))"
        )
    }

    private fun updateList(entries: List<LLSD>, stats: LLSD) {
        if (visibleCheckFunction?.invoke() == false) return

        TODO(
            "Store current selection UUIDs and scroll position; " +
            "set comment text (blocked if RLV canShowNearbyAgents); " +
            "clear rows; for each entry in entries: build row_data LLSD with name/voice_level/in_region/" +
            "typing/sitting/flags/has_notes/age/seen/range/seen_sort columns; " +
            "apply range_color, name_style/color, voice_level icon, flags values, age_color from options; " +
            "restore sort, scroll, selection; update name column header with stats (total/region/chatrange); " +
            "refreshLineHeight; call updateButtons(); fire changeSignal"
        )
    }

    private fun onAddFriendButtonClicked() {
        val id = getCurrentItemID()
        TODO("If id non-null: LLAvatarActions.requestFriendshipDialog(id)")
    }

    private fun onRadarListCommitted() {
        TODO("If selected value non-null: miniMap.setSelected(setOf(selectedId))")
        updateButtons()
        changeSignal.forEach { it() }
    }

    private fun onRadarListDoubleClicked() {
        TODO("Get first selected item; if null return; call FSRadar.getInstance().zoomAvatar(clickedId, name)")
    }

    private fun onOptionsMenuItemClicked(userdata: LLSD) {
        val chosenItem = userdata.asString()
        if (chosenItem == "panel_block_list_sidetray") {
            TODO("LLPanelBlockedList.showPanelAndSelect()")
        }
    }

    private fun onFilterEdit(searchString: String) {
        filterSubStringOrig = searchString.trimStart()
        val searchUpper = filterSubStringOrig.uppercase()
        if (filterSubString == searchUpper) return
        filterSubString = searchUpper
        TODO("radarList.setFilterString(filterSubStringOrig)")
    }

    private fun onGearButtonClicked(btn: Any?) {
        val selectedUuids = mutableListOf<LLUUID>()
        getCurrentItemIDs(selectedUuids)
        TODO("FSFloaterRadarMenu.gFSRadarMenu.show(btn, selectedUuids, 0, 0)")
    }

    private fun onColumnDisplayModeChanged() {
        TODO(
            "Read FSRadarColumnConfig U32; get column init params; " +
            "walk up to parent floater; compute default vs new width delta; " +
            "rebuild columns: include those with matching bit, hide others (pixel_width=-1); " +
            "adjust parent floater min resize width; re-sort by current column (fall back to 'range'); " +
            "setFilterColumn(0); dirtyColumns()"
        )
    }

    private fun onColumnVisibilityChecked(userdata: LLSD) {
        val column = userdata.asString()
        val columnConfig = TODO("Read FSRadarColumnConfig UInt from gSavedSettings") as UInt
        val bit = columnBits[column] ?: return
        val newValue = if (columnConfig and bit != 0u) columnConfig and bit.inv() else columnConfig or bit
        TODO("gSavedSettings.setU32(\"FSRadarColumnConfig\", newValue)")
    }

    private fun onEnableColumnVisibilityChecked(userdata: LLSD): Boolean {
        val column = userdata.asString()
        val columnConfig = TODO("Read FSRadarColumnConfig UInt from gSavedSettings") as UInt
        val bit = columnBits[column] ?: return false
        return columnConfig and bit != 0u
    }

    private inner class ButtonsUpdater(private val cb: () -> Unit) {
        init {
            TODO("LLAvatarTracker.instance().addObserver(this) — react to friend changes (STORM-557)")
        }
        fun changed(mask: UInt) = cb()
        fun destroy() {
            TODO("LLAvatarTracker.instance().removeObserver(this)")
        }
    }
}

private fun LLSD.asString(): String = TODO("APR: convert LLSD to String")
