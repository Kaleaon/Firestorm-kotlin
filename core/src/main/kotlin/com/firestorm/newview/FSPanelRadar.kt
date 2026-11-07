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
        return false
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        return false
    }

    fun requestUpdate() {
        System.err.println("FSPanelRadar: requestUpdate not yet implemented")
    }

    fun getCurrentItemID(): LLUUID {
        return LLUUID.NULL
    }

    fun getCurrentItemIDs(selectedUuids: MutableList<LLUUID>) {
        System.err.println("FSPanelRadar: getCurrentItemIDs not yet implemented")
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
            System.err.println("FSPanelRadar: isFriend lookup via LLAvatarTracker not yet implemented")
        }
        System.err.println("FSPanelRadar: updateButtons UI update not yet implemented")
    }

    private fun updateList(entries: List<LLSD>, stats: LLSD) {
        if (visibleCheckFunction?.invoke() == false) return

        System.err.println("FSPanelRadar: updateList not yet implemented")
    }

    private fun onAddFriendButtonClicked() {
        val id = getCurrentItemID()
        System.err.println("FSPanelRadar: onAddFriendButtonClicked not yet implemented")
    }

    private fun onRadarListCommitted() {
        System.err.println("FSPanelRadar: onRadarListCommitted miniMap selection not yet implemented")
        updateButtons()
        changeSignal.forEach { it() }
    }

    private fun onRadarListDoubleClicked() {
        System.err.println("FSPanelRadar: onRadarListDoubleClicked not yet implemented")
    }

    private fun onOptionsMenuItemClicked(userdata: LLSD) {
        val chosenItem = userdata.asString()
        if (chosenItem == "panel_block_list_sidetray") {
            System.err.println("FSPanelRadar: onOptionsMenuItemClicked LLPanelBlockedList.showPanelAndSelect not yet implemented")
        }
    }

    private fun onFilterEdit(searchString: String) {
        filterSubStringOrig = searchString.trimStart()
        val searchUpper = filterSubStringOrig.uppercase()
        if (filterSubString == searchUpper) return
        filterSubString = searchUpper
        System.err.println("FSPanelRadar: onFilterEdit radarList.setFilterString not yet implemented")
    }

    private fun onGearButtonClicked(btn: Any?) {
        val selectedUuids = mutableListOf<LLUUID>()
        getCurrentItemIDs(selectedUuids)
        System.err.println("FSPanelRadar: onGearButtonClicked not yet implemented")
    }

    private fun onColumnDisplayModeChanged() {
        System.err.println("FSPanelRadar: onColumnDisplayModeChanged not yet implemented")
    }

    private fun onColumnVisibilityChecked(userdata: LLSD) {
        val column = userdata.asString()
        val bit = columnBits[column] ?: return
        System.err.println("FSPanelRadar: onColumnVisibilityChecked not yet implemented")
    }

    private fun onEnableColumnVisibilityChecked(userdata: LLSD): Boolean {
        val column = userdata.asString()
        val bit = columnBits[column] ?: return false
        return false
    }

    private inner class ButtonsUpdater(private val cb: () -> Unit) {
        init {
            System.err.println("FSPanelRadar: ButtonsUpdater LLAvatarTracker observer registration not yet implemented")
        }
        fun changed(mask: UInt) = cb()
        fun destroy() {
            System.err.println("FSPanelRadar: ButtonsUpdater LLAvatarTracker observer removal not yet implemented")
        }
    }
}
