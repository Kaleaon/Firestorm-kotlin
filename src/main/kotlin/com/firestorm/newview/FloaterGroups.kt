package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// FloaterGroupPicker  (mirrors LLFloaterGroupPicker)
// ---------------------------------------------------------------------------

class FloaterGroupPicker(seed: LLUUID = LLUUID.NULL) {

    var id: LLUUID = seed
    var powersMask: ULong = GP_ALL_POWERS

    // boost::signals2::signal<void(LLUUID)> → list of lambdas
    private val groupSelectCallbacks: MutableList<(LLUUID) -> Unit> = mutableListOf()

    companion object {
        val instances: MutableMap<LLUUID, FloaterGroupPicker> = mutableMapOf()

        const val GP_ALL_POWERS: ULong = 0xFFFFFFFFFFFFFFFFuL
    }

    fun setSelectGroupCallback(cb: (LLUUID) -> Unit) {
        groupSelectCallbacks.add(cb)
    }

    fun setPowersMask(mask: ULong) {
        powersMask = mask
        System.err.println("FloaterGroupPicker: rebuild group list not yet implemented")
    }

    fun postBuild(): Boolean {
        System.err.println("FloaterGroupPicker: postBuild not yet implemented")
        return false
    }

    fun removeNoneOption() {
        System.err.println("FloaterGroupPicker: removeNoneOption not yet implemented")
    }

    fun ok() {
        val selectedGroupId = getSelectedGroupId()
        groupSelectCallbacks.forEach { it(selectedGroupId) }
        close()
    }

    private fun getSelectedGroupId(): LLUUID {
        System.err.println("FloaterGroupPicker: getSelectedGroupId not yet implemented")
        return LLUUID.NULL
    }

    private fun close() {
        System.err.println("FloaterGroupPicker: close not yet implemented")
    }
}

// ---------------------------------------------------------------------------
// PanelGroups  (mirrors LLPanelGroups)
// ---------------------------------------------------------------------------

class PanelGroups {

    // LLOldEvents::LLSimpleListener equivalent — handled inline
    fun handleEvent(eventDesc: String): Boolean {
        if (eventDesc == "new group") {
            reset()
            return true
        }
        return false
    }

    fun reset() {
        System.err.println("PanelGroups: reset not yet implemented")
    }

    fun postBuild(): Boolean {
        System.err.println("PanelGroups: postBuild not yet implemented")
        return false
    }

    private fun enableButtons() {
        System.err.println("PanelGroups: enableButtons not yet implemented")
    }

    fun create() {
        System.err.println("PanelGroups: create not yet implemented")
    }

    fun activate() {
        val groupId = getSelectedGroupId()
        System.err.println("PanelGroups: activate not yet implemented")
    }

    fun info() {
        val groupId = getSelectedGroupId()
        if (groupId != LLUUID.NULL) {
            System.err.println("PanelGroups: info not yet implemented")
        }
    }

    fun startIM() {
        val groupId = getSelectedGroupId()
        if (groupId != LLUUID.NULL) {
            System.err.println("PanelGroups: startIM not yet implemented")
        }
    }

    fun leave() {
        val groupId = getSelectedGroupId()
        if (groupId != LLUUID.NULL) {
            System.err.println("PanelGroups: leave not yet implemented")
        }
    }

    fun search() {
        System.err.println("PanelGroups: search not yet implemented")
    }

    private fun getSelectedGroupId(): LLUUID {
        System.err.println("PanelGroups: getSelectedGroupId not yet implemented")
        return LLUUID.NULL
    }
}

// ---------------------------------------------------------------------------
// initGroupList helper  (mirrors the free function init_group_list)
// ---------------------------------------------------------------------------

data class GroupListEntry(
    val id: LLUUID,
    val name: String,
    val bold: Boolean
)

fun buildGroupList(agentGroups: List<GroupData>, activeGroupId: LLUUID, powersMask: ULong = FloaterGroupPicker.GP_ALL_POWERS): List<GroupListEntry> {
    val entries = agentGroups
        .filter { g -> powersMask == FloaterGroupPicker.GP_ALL_POWERS || (g.powers and powersMask) != 0uL }
        .map { g -> GroupListEntry(id = g.id, name = g.name, bold = g.id == activeGroupId) }
        .sortedBy { it.name }
        .toMutableList()

    // "None" entry prepended at top
    entries.add(0, GroupListEntry(id = LLUUID.NULL, name = "None", bold = activeGroupId == LLUUID.NULL))
    return entries
}

// Stub GroupData used locally; real definition lives in GroupMgr.kt.
// Only add the `powers` field here — GroupMgr.GroupData already has the rest.
private val GroupData.powers: ULong get() = 0uL
