package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

private const val MAX_GROUP_INVITES = 100

// ---------------------------------------------------------------------------
// Shared implementation object (mirrors LLPanelGroupBulkImpl)
// ---------------------------------------------------------------------------

class PanelGroupBulkImpl(val groupId: LLUUID) {

    var groupName: String = ""
    var loadingText: String = ""
    var tooManySelected: String = ""
    var ownerWarning: String = ""
    var alreadyInGroup: String = ""
    var confirmedOwnerInvite: Boolean = false
    var listFullNotificationSent: Boolean = false

    val inviteeIds: MutableSet<LLUUID> = mutableSetOf()

    var closeCallback: (() -> Unit)? = null

    // avatarNameCacheConnections: in C++ these are boost signals connections;
    // represented here as a set of pending-lookup IDs.
    private val pendingNameLookups: MutableSet<LLUUID> = mutableSetOf()

    fun callbackClickAdd(panel: PanelGroupBulk) {
        TODO("APR: open avatar picker floater; on selection call addUsers(agentIds)")
    }

    fun callbackClickRemove() {
        handleRemove()
    }

    fun callbackClickCancel() {
        closeCallback?.invoke()
    }

    fun callbackSelect() {
        handleSelection()
    }

    fun addUsers(agentIds: List<LLUUID>) {
        for (agentId in agentIds) {
            val cachedName = lookupCachedAvatarName(agentId)
            if (cachedName != null) {
                onAvatarNameCache(agentId, cachedName)
            } else {
                pendingNameLookups.add(agentId)
                TODO("APR: LLAvatarNameCache::get($agentId) async; on result call onAvatarNameCache")
            }
        }
    }

    private fun lookupCachedAvatarName(agentId: LLUUID): String? {
        TODO("APR: query avatar-name cache for $agentId; return null if not cached")
    }

    fun onAvatarNameCache(agentId: LLUUID, fullName: String) {
        pendingNameLookups.remove(agentId)
        addUsers(listOf(fullName), listOf(agentId))
    }

    fun addUsers(names: List<String>, agentIds: List<LLUUID>) {
        if (listFullNotificationSent) return

        if (names.size + inviteeIds.size > MAX_GROUP_INVITES) {
            listFullNotificationSent = true
            TODO("APR: show GenericAlert notification: tooManySelected")
        }

        for (i in names.indices) {
            val id = agentIds[i]
            if (inviteeIds.contains(id)) continue
            inviteeIds.add(id)
            TODO("APR: add row [id=$id, name=${names[i]}] to bulk agent list UI")
        }
    }

    fun setGroupName(name: String) {
        groupName = name
        TODO("APR: update group-name text widget to '$name'")
    }

    private fun handleRemove() {
        TODO("APR: remove selected items from bulk agent list; update OK button state")
    }

    private fun handleSelection() {
        TODO("APR: enable remove button if any item is selected in bulk agent list")
    }
}

// ---------------------------------------------------------------------------
// PanelGroupBulk — base panel for bulk invite / ban (mirrors LLPanelGroupBulk)
// ---------------------------------------------------------------------------

abstract class PanelGroupBulk(groupId: LLUUID) {

    val mImplementation: PanelGroupBulkImpl = PanelGroupBulkImpl(groupId)

    var pendingGroupPropertiesUpdate: Boolean = false
    var pendingRoleDataUpdate: Boolean = false
    var pendingMemberDataUpdate: Boolean = false

    companion object {
        fun callbackClickSubmit(panel: PanelGroupBulk) {
            panel.submit()
        }
    }

    abstract fun submit()

    open fun clear() {
        mImplementation.inviteeIds.clear()
        mImplementation.listFullNotificationSent = false
        TODO("APR: clear bulk agent list UI; disable OK button")
    }

    open fun update() {
        updateGroupName()
        updateGroupData()
    }

    open fun draw() {
        update()
    }

    protected open fun updateGroupName() {
        val gdata = GroupMgr.getGroupData(mImplementation.groupId)
        if (gdata != null) {
            if (mImplementation.groupName != gdata.name) {
                mImplementation.setGroupName(gdata.name)
            }
        } else {
            mImplementation.setGroupName(mImplementation.loadingText)
        }
    }

    protected open fun updateGroupData() {
        val gdata = GroupMgr.getGroupData(mImplementation.groupId)

        if (gdata != null) {
            pendingGroupPropertiesUpdate = false
        } else if (!pendingGroupPropertiesUpdate) {
            pendingGroupPropertiesUpdate = true
            GroupMgr.sendGroupPropertiesRequest(mImplementation.groupId)
        }

        if (gdata?.roles != null) {
            pendingRoleDataUpdate = false
        } else if (!pendingRoleDataUpdate) {
            pendingRoleDataUpdate = true
            GroupMgr.sendGroupRoleDataRequest(mImplementation.groupId)
        }

        if (gdata?.members != null) {
            pendingMemberDataUpdate = false
        } else if (!pendingMemberDataUpdate) {
            pendingMemberDataUpdate = true
            GroupMgr.sendCapGroupMembersRequest(mImplementation.groupId)
        }
    }

    open fun addUserCallback(id: LLUUID, fullName: String) {
        mImplementation.addUsers(listOf(fullName), listOf(id))
    }

    open fun setCloseCallback(callback: () -> Unit) {
        mImplementation.closeCallback = callback
    }

    open fun addUsers(agentIds: MutableList<LLUUID>) {
        val names = mutableListOf<String>()
        var i = 0
        while (i < agentIds.size) {
            val agentId = agentIds[i]
            val name = resolveAvatarName(agentId)
            if (name != null) {
                names.add(name)
                i++
            } else {
                // offline buddy without cached name — fetch async
                agentIds.removeAt(i)
                TODO("APR: LLAvatarNameCache::get($agentId) async; on result call addUserCallback")
            }
        }
        mImplementation.listFullNotificationSent = false
        mImplementation.addUsers(names, agentIds)
    }

    private fun resolveAvatarName(agentId: LLUUID): String? {
        TODO("APR: look up avatar name from object list / name cache for $agentId; return null if unavailable")
    }
}

// ---------------------------------------------------------------------------
// GroupMgr stubs used by updateGroupData (companion to the real GroupMgr object)
// ---------------------------------------------------------------------------

private fun GroupMgr.getGroupData(groupId: LLUUID): GroupDataExtended? {
    TODO("APR: retrieve GroupDataExtended from GroupMgr cache for $groupId")
}

private data class GroupDataExtended(
    val name: String,
    val roles: Any?,
    val members: Any?
)

private fun Any.sendGroupRoleDataRequest(groupId: LLUUID) {
    TODO("APR: GroupMgr.sendGroupRoleDataRequest($groupId)")
}
