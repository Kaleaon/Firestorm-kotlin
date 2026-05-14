package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Stub interfaces / singletons referenced from the C++ translation unit
// ---------------------------------------------------------------------------

interface LLGroupMgrObserver {
    fun changed(gc: LLGroupChange)
}

enum class LLGroupChange {
    GC_PROPERTIES, GC_MEMBERS, GC_ROLES, GC_ROLE_MEMBERS, GC_TITLES, GC_ALL
}

data class LLGroupData(
    val id: UUID = UUID(0, 0),
    val name: String = "",
    val membershipFee: Int = 0,
    val listInProfile: Boolean = true,
    val acceptNotices: Boolean = true
)

// ---------------------------------------------------------------------------
// LLFetchGroupMemberData – fetches group member data before leaving
// ---------------------------------------------------------------------------

abstract class LLFetchGroupMemberData(protected val groupId: UUID) : LLGroupMgrObserver {
    protected var requestProcessed: Boolean = false

    init {
        System.err.println("LLFetchGroupMemberData: init observer registration not yet implemented")
    }

    fun getGroupId(): UUID = groupId
    abstract fun processGroupData()
}

private class LLFetchLeaveGroupData(groupId: UUID) : LLFetchGroupMemberData(groupId) {
    override fun processGroupData() {
        LLGroupActions.processLeaveGroupDataResponse(groupId)
    }

    override fun changed(gc: LLGroupChange) {
        if (gc == LLGroupChange.GC_PROPERTIES && !requestProcessed) {
            processGroupData()
            requestProcessed = true
        }
    }
}

// ---------------------------------------------------------------------------
// LLGroupActions – all static; mirrors C++ static-method class
// ---------------------------------------------------------------------------

object LLGroupActions {

    private var fetchLeaveGroupData: LLFetchLeaveGroupData? = null

    fun search() {
        System.err.println("LLGroupActions: search not yet implemented")
    }

    fun startCall(groupId: UUID) {
        System.err.println("LLGroupActions: startCall not yet implemented")
    }

    fun join(groupId: UUID) {
        System.err.println("LLGroupActions: join not yet implemented")
    }

    fun leave(groupId: UUID) {
        if (groupId == UUID(0, 0)) return
        System.err.println("LLGroupActions: leave not yet implemented")
    }

    fun activate(groupId: UUID) {
        System.err.println("LLGroupActions: activate not yet implemented")
    }

    fun inspect(groupId: UUID) {
        System.err.println("LLGroupActions: inspect not yet implemented")
    }

    fun show(groupId: UUID, expandNoticesTab: Boolean = false) {
        if (groupId == UUID(0, 0)) return
        System.err.println("LLGroupActions: show not yet implemented")
    }

    fun refreshNotices(groupId: UUID = UUID(0, 0)) {
        System.err.println("LLGroupActions: refreshNotices not yet implemented")
    }

    fun refresh(groupId: UUID) {
        System.err.println("LLGroupActions: refresh not yet implemented")
    }

    fun createGroup() {
        System.err.println("LLGroupActions: createGroup not yet implemented")
    }

    fun closeGroup(groupId: UUID) {
        System.err.println("LLGroupActions: closeGroup not yet implemented")
    }

    fun startIM(groupId: UUID): UUID {
        if (groupId == UUID(0, 0)) return UUID(0, 0)
        System.err.println("LLGroupActions: startIM not yet implemented")
        return UUID(0, 0)
    }

    fun leaveIM(groupId: UUID) {
        closeGroupIM(groupId, CloseAction.CLOSE_LEAVE)
    }

    fun snoozeIM(groupId: UUID) {
        confirmGroupIMSnooze(groupId)
    }

    fun endIM(groupId: UUID) {
        closeGroupIM(groupId, CloseAction.CLOSE_DEFAULT)
    }

    fun isInGroup(groupId: UUID): Boolean {
        System.err.println("LLGroupActions: isInGroup not yet implemented")
        return false
    }

    fun isAvatarMemberOfGroup(groupId: UUID, avatarId: UUID): Boolean {
        if (groupId == UUID(0, 0) || avatarId == UUID(0, 0)) return false
        System.err.println("LLGroupActions: isAvatarMemberOfGroup not yet implemented")
        return false
    }

    fun canEjectFromGroup(idGroup: UUID, idAgent: UUID): Boolean {
        System.err.println("LLGroupActions: canEjectFromGroup not yet implemented")
        return false
    }

    fun ejectFromGroup(idGroup: UUID, idAgent: UUID) {
        System.err.println("LLGroupActions: ejectFromGroup not yet implemented")
    }

    fun callbackEject(notificationOption: Int, avatarId: UUID, groupId: UUID): Boolean {
        if (notificationOption == 2) return false  // Cancel
        if (notificationOption == 0) {             // Eject
            if (!canEjectFromGroup(groupId, avatarId)) return false
            System.err.println("LLGroupActions: callbackEject GroupMemberEject send not yet implemented")
        }
        return false
    }

    // Called by LLFetchLeaveGroupData when group data is ready
    internal fun processLeaveGroupDataResponse(groupId: UUID) {
        System.err.println("LLGroupActions: processLeaveGroupDataResponse not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private enum class CloseAction { CLOSE_DEFAULT, CLOSE_LEAVE, CLOSE_SNOOZE }

    private fun closeGroupIM(groupId: UUID, closeAction: CloseAction) {
        if (groupId == UUID(0, 0)) return
        System.err.println("LLGroupActions: closeGroupIM not yet implemented")
    }

    private fun confirmGroupIMSnooze(groupId: UUID) {
        if (groupId == UUID(0, 0)) return
        System.err.println("LLGroupActions: confirmGroupIMSnooze not yet implemented")
    }

    private fun onJoinGroup(option: Int, groupId: UUID): Boolean {
        if (option == 1) return false  // user cancelled
        System.err.println("LLGroupActions: onJoinGroup GroupMemberJoin send not yet implemented")
        return false
    }

    private fun onLeaveGroup(option: Int, groupId: UUID): Boolean {
        if (option == 0) {
            System.err.println("LLGroupActions: onLeaveGroup LeaveGroupRequest send not yet implemented")
        }
        return false
    }
}
