package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

private const val MAX_GROUP_INVITES = 100

// ---------------------------------------------------------------------------
// PanelGroupBulkImpl  (mirrors LLPanelGroupBulkImpl)
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

    private val pendingNameLookups: MutableSet<LLUUID> = mutableSetOf()

    fun callbackClickAdd(panel: PanelGroupBulk) {
        System.err.println("PanelGroupBulkImpl: callbackClickAdd not yet implemented")
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
                if (!pendingNameLookups.contains(agentId)) {
                    pendingNameLookups.add(agentId)
                    System.err.println("PanelGroupBulkImpl: addUsers not yet implemented")
                }
            }
        }
    }

    private fun lookupCachedAvatarName(agentId: LLUUID): String? {
        return null
    }

    fun onAvatarNameCache(agentId: LLUUID, fullName: String) {
        pendingNameLookups.remove(agentId)
        addUsers(listOf(fullName), listOf(agentId))
    }

    fun addUsers(names: List<String>, agentIds: List<LLUUID>) {
        if (listFullNotificationSent) return

        if (names.size + inviteeIds.size > MAX_GROUP_INVITES) {
            listFullNotificationSent = true
            System.err.println("PanelGroupBulkImpl: addUsers not yet implemented")
        }

        for (i in names.indices) {
            val id = agentIds[i]
            if (inviteeIds.contains(id)) continue
            inviteeIds.add(id)
            System.err.println("PanelGroupBulkImpl: addUsers not yet implemented")
        }
    }

    fun setGroupName(name: String) {
        groupName = name
        System.err.println("PanelGroupBulkImpl: setGroupName not yet implemented")
    }

    private fun handleRemove() {
        System.err.println("PanelGroupBulkImpl: handleRemove not yet implemented")
    }

    private fun handleSelection() {
        System.err.println("PanelGroupBulkImpl: handleSelection not yet implemented")
    }
}

// ---------------------------------------------------------------------------
// PanelGroupBulk  (mirrors LLPanelGroupBulk)
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
        System.err.println("PanelGroupBulk: clear not yet implemented")
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
            GroupMgr.requestGroupData(mImplementation.groupId)
        }

        if (GroupMgr.getGroupRoles(mImplementation.groupId) != null) {
            pendingRoleDataUpdate = false
        } else if (!pendingRoleDataUpdate) {
            pendingRoleDataUpdate = true
            GroupMgr.requestGroupRoleData(mImplementation.groupId)
        }

        if (GroupMgr.getGroupMembers(mImplementation.groupId) != null) {
            pendingMemberDataUpdate = false
        } else if (!pendingMemberDataUpdate) {
            pendingMemberDataUpdate = true
            GroupMgr.requestGroupMembers(mImplementation.groupId)
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
                // offline buddy without a cached name — fetch asynchronously
                agentIds.removeAt(i)
                System.err.println("PanelGroupBulk: addUsers not yet implemented")
            }
        }
        mImplementation.listFullNotificationSent = false
        mImplementation.addUsers(names, agentIds)
    }

    private fun resolveAvatarName(agentId: LLUUID): String? {
        return null
    }
}
