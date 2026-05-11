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
        TODO("APR: open LLFloaterAvatarPicker; on selection call addUsers(agentIds); send group members cap request")
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
                    TODO("APR: LLAvatarNameCache::get($agentId) async; on result call onAvatarNameCache($agentId, name)")
                }
            }
        }
    }

    private fun lookupCachedAvatarName(agentId: LLUUID): String? {
        TODO("APR: return cached avatar display/account name for $agentId, or null if not cached")
    }

    fun onAvatarNameCache(agentId: LLUUID, fullName: String) {
        pendingNameLookups.remove(agentId)
        addUsers(listOf(fullName), listOf(agentId))
    }

    fun addUsers(names: List<String>, agentIds: List<LLUUID>) {
        if (listFullNotificationSent) return

        if (names.size + inviteeIds.size > MAX_GROUP_INVITES) {
            listFullNotificationSent = true
            TODO("APR: LLNotificationsUtil.add(\"GenericAlert\", message=tooManySelected)")
        }

        for (i in names.indices) {
            val id = agentIds[i]
            if (inviteeIds.contains(id)) continue
            inviteeIds.add(id)
            TODO("APR: add row id=$id name=${names[i]} to bulk agent list UI; enable OK button if it was disabled")
        }
    }

    fun setGroupName(name: String) {
        groupName = name
        TODO("APR: update group-name label widget to '$name'")
    }

    private fun handleRemove() {
        TODO("APR: for each selected item remove its UUID from inviteeIds; delete selected items from bulk list; disable remove button; disable OK button if list is now empty")
    }

    private fun handleSelection() {
        TODO("APR: enable remove button iff any item is selected in bulk agent list")
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
        TODO("APR: clear bulk agent list UI widget; disable OK button")
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
                TODO("APR: LLAvatarNameCache::get($agentId) async; on result call addUserCallback($agentId, name)")
            }
        }
        mImplementation.listFullNotificationSent = false
        mImplementation.addUsers(names, agentIds)
    }

    private fun resolveAvatarName(agentId: LLUUID): String? {
        TODO("APR: look up avatar name from viewer object list or name cache for $agentId; return null if unavailable")
    }
}
