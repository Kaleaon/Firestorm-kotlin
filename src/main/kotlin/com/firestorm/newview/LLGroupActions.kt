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
        TODO("APR: use JVM equivalent – register observer and send group properties + members request")
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
        TODO("APR: use JVM equivalent – show search floater tab 'groups'")
    }

    fun startCall(groupId: UUID) {
        TODO("APR: use JVM equivalent – start group voice session via IMMgr")
    }

    fun join(groupId: UUID) {
        TODO("APR: use JVM equivalent – check group capacity, show join confirmation dialog")
    }

    fun leave(groupId: UUID) {
        if (groupId == UUID(0, 0)) return
        TODO("APR: use JVM equivalent – fetch member data then show leave confirmation")
    }

    fun activate(groupId: UUID) {
        TODO("APR: use JVM equivalent – send ActivateGroup message to simulator")
    }

    fun inspect(groupId: UUID) {
        TODO("APR: use JVM equivalent – show inspect_group floater")
    }

    fun show(groupId: UUID, expandNoticesTab: Boolean = false) {
        if (groupId == UUID(0, 0)) return
        TODO("APR: use JVM equivalent – open group info panel (standalone or side-panel)")
    }

    fun refreshNotices(groupId: UUID = UUID(0, 0)) {
        TODO("APR: use JVM equivalent – refresh group notices panel if visible")
    }

    fun refresh(groupId: UUID) {
        TODO("APR: use JVM equivalent – refresh group info panel if visible")
    }

    fun createGroup() {
        TODO("APR: use JVM equivalent – show group creation panel")
    }

    fun closeGroup(groupId: UUID) {
        TODO("APR: use JVM equivalent – close group panel or standalone floater")
    }

    fun startIM(groupId: UUID): UUID {
        if (groupId == UUID(0, 0)) return UUID(0, 0)
        TODO("APR: use JVM equivalent – add IM session and show conversation window")
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
        TODO("APR: use JVM equivalent – check agent group membership via gAgent")
    }

    fun isAvatarMemberOfGroup(groupId: UUID, avatarId: UUID): Boolean {
        if (groupId == UUID(0, 0) || avatarId == UUID(0, 0)) return false
        TODO("APR: use JVM equivalent – look up avatar in group member list")
    }

    fun canEjectFromGroup(idGroup: UUID, idAgent: UUID): Boolean {
        TODO("APR: use JVM equivalent – check GP_MEMBER_EJECT power and role membership")
    }

    fun ejectFromGroup(idGroup: UUID, idAgent: UUID) {
        TODO("APR: use JVM equivalent – show eject confirmation dialog then send eject message")
    }

    fun callbackEject(notificationOption: Int, avatarId: UUID, groupId: UUID): Boolean {
        if (notificationOption == 2) return false  // Cancel
        if (notificationOption == 0) {             // Eject
            if (!canEjectFromGroup(groupId, avatarId)) return false
            TODO("APR: use JVM equivalent – send GroupMemberEject message to simulator")
        }
        return false
    }

    // Called by LLFetchLeaveGroupData when group data is ready
    internal fun processLeaveGroupDataResponse(groupId: UUID) {
        TODO("APR: use JVM equivalent – check owner status then show leave confirmation dialog")
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private enum class CloseAction { CLOSE_DEFAULT, CLOSE_LEAVE, CLOSE_SNOOZE }

    private fun closeGroupIM(groupId: UUID, closeAction: CloseAction) {
        if (groupId == UUID(0, 0)) return
        TODO("APR: use JVM equivalent – locate IM session and leave it with the given close action")
    }

    private fun confirmGroupIMSnooze(groupId: UUID) {
        if (groupId == UUID(0, 0)) return
        TODO("APR: use JVM equivalent – prompt for snooze duration then call closeGroupIM(CLOSE_SNOOZE)")
    }

    private fun onJoinGroup(option: Int, groupId: UUID): Boolean {
        if (option == 1) return false  // user cancelled
        TODO("APR: use JVM equivalent – send GroupMemberJoin message to simulator")
    }

    private fun onLeaveGroup(option: Int, groupId: UUID): Boolean {
        if (option == 0) {
            TODO("APR: use JVM equivalent – close standalone floater if open, then send LeaveGroupRequest")
        }
        return false
    }
}
