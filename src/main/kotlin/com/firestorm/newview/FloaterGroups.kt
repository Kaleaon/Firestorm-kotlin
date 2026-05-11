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
        TODO("APR: rebuild group list filtered by powersMask=$mask")
    }

    fun postBuild(): Boolean {
        TODO("APR: populate group list via initGroupList(agent.groupId, powersMask); wire OK/Cancel buttons")
    }

    fun removeNoneOption() {
        TODO("APR: remove the null-UUID 'none' entry from the group list")
    }

    fun ok() {
        val selectedGroupId = getSelectedGroupId()
        groupSelectCallbacks.forEach { it(selectedGroupId) }
        close()
    }

    private fun getSelectedGroupId(): LLUUID {
        TODO("APR: return currently selected group ID from group list control")
    }

    private fun close() {
        TODO("APR: close this floater")
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
        TODO("APR: clear group list; repopulate via initGroupList(agent.groupId); update group count label; enableButtons()")
    }

    fun postBuild(): Boolean {
        TODO("APR: bind group list commit callback; init group list; wire Activate/Info/IM/Leave/Create/Search buttons; call reset()")
    }

    private fun enableButtons() {
        TODO("APR: enable/disable Activate/Info/IM/Leave/Create buttons based on current group selection vs active group")
    }

    fun create() {
        TODO("APR: GroupActions.createGroup()")
    }

    fun activate() {
        val groupId = getSelectedGroupId()
        TODO("APR: GroupActions.activate($groupId)")
    }

    fun info() {
        val groupId = getSelectedGroupId()
        if (groupId != LLUUID.NULL) {
            TODO("APR: GroupActions.show($groupId)")
        }
    }

    fun startIM() {
        val groupId = getSelectedGroupId()
        if (groupId != LLUUID.NULL) {
            TODO("APR: GroupActions.startIM($groupId)")
        }
    }

    fun leave() {
        val groupId = getSelectedGroupId()
        if (groupId != LLUUID.NULL) {
            TODO("APR: GroupActions.leave($groupId)")
        }
    }

    fun search() {
        TODO("APR: GroupActions.search()")
    }

    private fun getSelectedGroupId(): LLUUID {
        TODO("APR: return currently selected ID from group list scroll control")
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
private val GroupData.powers: ULong get() = TODO("APR: expose group powers mask from GroupData")
