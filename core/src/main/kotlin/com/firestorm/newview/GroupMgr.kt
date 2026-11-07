/**
 * GroupMgr.kt
 *
 * Kotlin conversion of llgroupmgr.h / llgroupmgr.cpp
 * Original: Manager for aggregating all client knowledge for specific groups.
 * Copyright (C) 2004-2010, Linden Research, Inc. — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Change-notification enum (mirrors LLGroupChange)
// ---------------------------------------------------------------------------

/** Signals which portion of group data has changed, sent to observers. */
enum class GroupChange {
    PROPERTIES,
    MEMBER_DATA,
    ROLE_DATA,
    ROLE_MEMBER_DATA,
    TITLES,
    BAN_LIST,
    ALL
}

// ---------------------------------------------------------------------------
// Observer interfaces
// ---------------------------------------------------------------------------

/** Notified whenever any group this observer watches changes. */
interface GroupMgrObserver {
    val groupId: LLUUID
    fun changed(change: GroupChange)
}

/** Notified whenever a specific group changes. */
interface ParticularGroupObserver {
    fun changed(groupId: LLUUID, change: GroupChange)
}

// ---------------------------------------------------------------------------
// Data classes
// ---------------------------------------------------------------------------

/**
 * Immutable summary of a group's properties.
 *
 * Mirrors the public fields of `LLGroupMgrGroupData` that callers read most
 * frequently (name, charter, insignia, membership info).
 */
data class GroupData(
    val id: LLUUID,
    val name: String,
    val charter: String,
    val insigniaId: LLUUID,
    val memberCount: Int,
    val openEnrollment: Boolean,
    val membershipFee: Int
)

/**
 * Per-member record inside a group.
 *
 * Corresponds to `LLGroupMemberData`.
 *
 * @property id           Agent UUID of this member.
 * @property isOwner      True if the member holds the owner role.
 * @property contribution Land contribution (sq m) donated to the group.
 * @property agentPowers  Bitmask of role powers granted to this member.
 */
data class GroupMember(
    val id: LLUUID,
    val isOwner: Boolean,
    val contribution: Int,
    val agentPowers: ULong
)

/**
 * Role record inside a group.
 *
 * Mirrors `LLRoleData` / `LLGroupRoleData`.
 */
data class GroupRole(
    val id: LLUUID,
    val name: String,
    val title: String,
    val description: String,
    val powers: ULong,
    val memberCount: Int
)

/**
 * Ban-list entry for a group.  Mirrors `LLGroupBanData`.
 *
 * @property bannedId  UUID of the banned agent.
 * @property banDate   Unix epoch millis when the ban was issued (0 = unknown).
 */
data class GroupBanEntry(
    val bannedId: LLUUID,
    val banDate: Long = 0L
)

// ---------------------------------------------------------------------------
// GroupMgr singleton
// ---------------------------------------------------------------------------

/**
 * Client-side cache and request dispatcher for group data.
 *
 * Maps to the C++ `LLGroupMgr` singleton (`LLSingleton<LLGroupMgr>`).
 *
 * All network-bound methods are stubs (`TODO()`); the in-memory cache
 * operations (`groups`, `getGroupData`, etc.) are fully implemented so that
 * unit tests can exercise the cache logic without a live viewer.
 */
object GroupMgr {

    // -----------------------------------------------------------------------
    // Cache
    // -----------------------------------------------------------------------

    /** In-memory group-data cache, keyed by group UUID. */
    val groups: MutableMap<LLUUID, GroupData> = mutableMapOf()

    /** Per-group member lists, populated when member data is fetched. */
    val groupMembers: MutableMap<LLUUID, MutableMap<LLUUID, GroupMember>> = mutableMapOf()

    /** Per-group role lists. */
    val groupRoles: MutableMap<LLUUID, MutableMap<LLUUID, GroupRole>> = mutableMapOf()

    /** Per-group ban lists.  Capped at GB_MAX_BANNED_AGENTS (500) in the C++ code. */
    val groupBans: MutableMap<LLUUID, MutableMap<LLUUID, GroupBanEntry>> = mutableMapOf()

    // -----------------------------------------------------------------------
    // Observers
    // -----------------------------------------------------------------------

    private val observers: MutableMap<LLUUID, MutableSet<GroupMgrObserver>> = mutableMapOf()
    private val particularObservers: MutableMap<LLUUID, MutableSet<ParticularGroupObserver>> =
        mutableMapOf()

    fun addObserver(observer: GroupMgrObserver) {
        observers.getOrPut(observer.groupId) { mutableSetOf() }.add(observer)
    }

    fun removeObserver(observer: GroupMgrObserver) {
        observers[observer.groupId]?.remove(observer)
    }

    fun addObserver(groupId: LLUUID, observer: ParticularGroupObserver) {
        particularObservers.getOrPut(groupId) { mutableSetOf() }.add(observer)
    }

    fun removeObserver(groupId: LLUUID, observer: ParticularGroupObserver) {
        particularObservers[groupId]?.remove(observer)
    }

    private fun notifyObservers(groupId: LLUUID, change: GroupChange) {
        observers[groupId]?.forEach { it.changed(change) }
        particularObservers[groupId]?.forEach { it.changed(groupId, change) }
    }

    // -----------------------------------------------------------------------
    // Cache accessors
    // -----------------------------------------------------------------------

    /**
     * Return cached [GroupData] for [id], or null if not yet loaded.
     * Maps to `LLGroupMgr::getGroupData()`.
     */
    fun getGroupData(id: LLUUID): GroupData? = groups[id]

    /**
     * Return cached member list for [groupId], or null if not yet loaded.
     */
    fun getGroupMembers(groupId: LLUUID): Map<LLUUID, GroupMember>? = groupMembers[groupId]

    /**
     * Return cached role list for [groupId], or null if not yet loaded.
     */
    fun getGroupRoles(groupId: LLUUID): Map<LLUUID, GroupRole>? = groupRoles[groupId]

    /**
     * Return cached ban list for [groupId], or null if not yet loaded.
     */
    fun getGroupBans(groupId: LLUUID): Map<LLUUID, GroupBanEntry>? = groupBans[groupId]

    // -----------------------------------------------------------------------
    // Network requests (stubs)
    // -----------------------------------------------------------------------

    /**
     * Request group properties from the server for [id].
     * Maps to `LLGroupMgr::sendGroupPropertiesRequest()`.
     */
    fun requestGroupData(id: LLUUID) {
        System.err.println("GroupMgr: requestGroupData not yet implemented")
    }

    /**
     * Request full member list for [groupId].
     * Maps to `LLGroupMgr::sendGroupMembersRequest()` /
     * `sendCapGroupMembersRequest()`.
     */
    fun requestGroupMembers(groupId: LLUUID) {
        System.err.println("GroupMgr: requestGroupMembers not yet implemented")
    }

    /**
     * Request role definitions for [groupId].
     * Maps to `LLGroupMgr::sendGroupRoleDataRequest()`.
     */
    fun requestGroupRoleData(groupId: LLUUID) {
        System.err.println("GroupMgr: requestGroupRoleData not yet implemented")
    }

    /**
     * Request role-to-member mapping for [groupId].
     * Maps to `LLGroupMgr::sendGroupRoleMembersRequest()`.
     */
    fun requestGroupRoleMembers(groupId: LLUUID) {
        System.err.println("GroupMgr: requestGroupRoleMembers not yet implemented")
    }

    /**
     * Send pending role-member changes for [groupId] to the server.
     * Maps to `LLGroupMgr::sendGroupRoleMemberChanges()`.
     */
    fun sendGroupRoleMemberChanges(groupId: LLUUID) {
        System.err.println("GroupMgr: sendGroupRoleMemberChanges not yet implemented")
    }

    /**
     * Invite [invitees] to [groupId].
     * Maps to `LLGroupMgr::sendGroupMemberInvites()`.
     *
     * In the C++ version each invitee is paired with a role UUID; this stub
     * accepts a flat list and assigns the Everyone role implicitly.
     */
    fun sendGroupMemberInvites(groupId: LLUUID, invitees: List<LLUUID>) {
        System.err.println("GroupMgr: sendGroupMemberInvites not yet implemented")
    }

    /**
     * Eject [memberIds] from [groupId].
     * Maps to `LLGroupMgr::sendGroupMemberEjects()`.
     */
    fun sendGroupMemberEjects(groupId: LLUUID, memberIds: List<LLUUID>) {
        System.err.println("GroupMgr: sendGroupMemberEjects not yet implemented")
    }

    /**
     * Submit updated group info (name, charter, etc.) for [groupId].
     * Maps to `LLGroupMgr::sendUpdateGroupInfo()`.
     */
    fun sendUpdateGroupInfo(groupId: LLUUID) {
        System.err.println("GroupMgr: sendUpdateGroupInfo not yet implemented")
    }

    /**
     * Submit a ban-list request of type [requestType] for [groupId].
     * Maps to `LLGroupMgr::sendGroupBanRequest()`.
     */
    fun sendGroupBanRequest(
        groupId: LLUUID,
        requestType: BanRequestType,
        banAction: BanAction = BanAction.NO_ACTION,
        banList: List<LLUUID> = emptyList()
    ) {
        System.err.println("GroupMgr: sendGroupBanRequest not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Cache management
    // -----------------------------------------------------------------------

    /** Clear all cached data for every group. */
    fun clearGroups() {
        groups.clear()
        groupMembers.clear()
        groupRoles.clear()
        groupBans.clear()
        observers.clear()
        particularObservers.clear()
    }

    /** Remove all cached data for [groupId] only. */
    fun clearGroupData(groupId: LLUUID) {
        groups.remove(groupId)
        groupMembers.remove(groupId)
        groupRoles.remove(groupId)
        groupBans.remove(groupId)
    }

    // -----------------------------------------------------------------------
    // Ban-list helpers (mirror LLGroupMgrGroupData::createBanEntry etc.)
    // -----------------------------------------------------------------------

    fun createBanEntry(groupId: LLUUID, bannedId: LLUUID, banDate: Long = 0L) {
        groupBans.getOrPut(groupId) { mutableMapOf() }[bannedId] =
            GroupBanEntry(bannedId, banDate)
    }

    fun removeBanEntry(groupId: LLUUID, bannedId: LLUUID) {
        groupBans[groupId]?.remove(bannedId)
    }

    // -----------------------------------------------------------------------
    // Nested enums (mirrors C++ inner enums on LLGroupMgr)
    // -----------------------------------------------------------------------

    enum class BanRequestType { GET, POST, PUT, DELETE }

    enum class BanAction(val bits: Int) {
        NO_ACTION(0),
        CREATE(1),
        DELETE(2),
        UPDATE(4)
    }
}
