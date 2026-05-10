package com.firestorm.newview

import java.util.UUID

private const val REGION_GROUP_TITLES_FILE    = "region_group_titles.xml"
private const val MAX_REGION_NAME_LENGTH: Int = 63
private const val REGION_VALIDATION_TIMEOUT: Float = 10f
private const val LOGIN_RETRY_DELAY: Float = 5f

data class FSRegionTitleAssignment(
    val groupId: UUID,
    val roleId: UUID,
    val displayName: String
)

object FSGroupTitleRegionMgr {

    private var noneOnUnassigned: Boolean = false
    private var dataLoaded: Boolean = false
    private var lastAppliedRegion: String = ""

    private val assignments: MutableMap<String, FSRegionTitleAssignment> = mutableMapOf()

    private var hasPendingValidation: Boolean = false
    private var pendingGroupId: UUID = UUID(0L, 0L)
    private var pendingRoleId: UUID = UUID(0L, 0L)
    private var pendingRegionName: String = ""

    private var validationTimerHandle: Any? = null
    private var loginRetryTimerHandle: Any? = null

    private var regionChangedConnection: (() -> Unit)? = null
    private val assignmentsChangedListeners: MutableList<() -> Unit> = mutableListOf()

    init {
        TODO("APR: use JVM equivalent — subscribe to agent region change events and call onRegionChanged()")
    }

    fun destroy() {
        cancelPendingValidation()
        loginRetryTimerHandle = null
        regionChangedConnection?.invoke()
        regionChangedConnection = null
    }

    fun setAssignmentsChangedCallback(cb: () -> Unit): () -> Unit {
        assignmentsChangedListeners.add(cb)
        return { assignmentsChangedListeners.remove(cb) }
    }

    fun loadFromDisk() {
        loginRetryTimerHandle = null
        assignments.clear()
        noneOnUnassigned = false
        lastAppliedRegion = ""

        TODO("APR: use JVM equivalent — resolve per-account file path for REGION_GROUP_TITLES_FILE; parse LLSD XML; populate noneOnUnassigned and assignments map; set dataLoaded=true; call onRegionChanged(); schedule LOGIN_RETRY_DELAY one-shot timer that clears lastAppliedRegion and calls onRegionChanged()")
    }

    fun saveToDisk() {
        TODO("APR: use JVM equivalent — resolve per-account file path; serialize assignments map and noneOnUnassigned to LLSD XML and write with JVM file I/O")
    }

    fun setAssignment(groupId: UUID, roleId: UUID, regionName: String) {
        val normalized = normalizeRegionName(regionName)
        if (normalized.isEmpty()) return

        val existing = assignments[normalized]
        if (existing != null && existing.groupId == groupId && existing.roleId == roleId) return

        assignments.remove(normalized)
        assignments[normalized] = FSRegionTitleAssignment(groupId, roleId, sanitizeRegionName(regionName))
        saveToDisk()
        assignmentsChangedListeners.forEach { it() }
    }

    fun setAssignmentForCurrentRegion(groupId: UUID, roleId: UUID) {
        TODO("APR: use JVM equivalent — get current region name from gAgent.getRegion(); call setAssignment(groupId, roleId, regionName)")
    }

    fun clearAssignment(groupId: UUID, roleId: UUID) {
        val toRemove = assignments.entries.filter { it.value.groupId == groupId && it.value.roleId == roleId }
        if (toRemove.isEmpty()) return
        toRemove.forEach { assignments.remove(it.key) }
        saveToDisk()
        assignmentsChangedListeners.forEach { it() }
    }

    fun clearAssignmentByRegion(regionName: String) {
        val key = normalizeRegionName(regionName)
        if (assignments.remove(key) != null) {
            saveToDisk()
            assignmentsChangedListeners.forEach { it() }
        }
    }

    fun getRegionForTitle(groupId: UUID, roleId: UUID): String =
        assignments.values
            .filter { it.groupId == groupId && it.roleId == roleId }
            .joinToString(", ") { it.displayName }

    fun getRegionDisplayNamesForTitle(groupId: UUID, roleId: UUID): MutableList<String> =
        assignments.values
            .filter { it.groupId == groupId && it.roleId == roleId }
            .map { it.displayName }
            .toMutableList()

    fun getAssignmentForRegion(regionName: String): Pair<UUID, UUID>? {
        val entry = assignments[normalizeRegionName(regionName)] ?: return null
        return Pair(entry.groupId, entry.roleId)
    }

    fun setNoneOnUnassigned(enabled: Boolean) {
        noneOnUnassigned = enabled
        saveToDisk()
    }

    fun getNoneOnUnassigned(): Boolean = noneOnUnassigned

    fun showRegionInputDialog(groupId: UUID, roleId: UUID) {
        TODO("APR: use JVM equivalent — show input dialog for FSSetTitleRegion notification with group_id/role_id payload; on confirm call validateAndSetAssignment()")
    }

    fun sanitizeRegionName(input: String): String {
        val trimmed = input.trim()
        val sb = StringBuilder(trimmed.length)
        var prevSpace = false
        for (c in trimmed) {
            when {
                c == ' ' -> {
                    if (!prevSpace) sb.append(c)
                    prevSpace = true
                }
                c.code >= 32 -> {
                    sb.append(c)
                    prevSpace = false
                }
            }
        }
        return if (sb.length > MAX_REGION_NAME_LENGTH) sb.substring(0, MAX_REGION_NAME_LENGTH) else sb.toString()
    }

    private fun normalizeRegionName(name: String): String = sanitizeRegionName(name).lowercase()

    private fun validateAndSetAssignment(groupId: UUID, roleId: UUID, regionName: String) {
        val sanitized = sanitizeRegionName(regionName)
        if (sanitized.isEmpty()) return

        cancelPendingValidation()
        pendingGroupId = groupId
        pendingRoleId = roleId
        pendingRegionName = sanitized
        hasPendingValidation = true

        TODO("APR: use JVM equivalent — send named region request via LLWorldMapMessage; on result call onValidationResult(regionHandle); schedule REGION_VALIDATION_TIMEOUT one-shot timer calling onValidationTimeout()")
    }

    private fun cancelPendingValidation() {
        hasPendingValidation = false
        validationTimerHandle = null
    }

    private fun onValidationResult(regionHandle: Long) {
        if (!hasPendingValidation) return
        TODO("APR: use JVM equivalent — look up sim info from LLWorldMap by regionHandle; if not found or canonical name doesn't match pendingRegionName show FSSetTitleRegionNotFound notification; otherwise call cancelPendingValidation() then setAssignment(pendingGroupId, pendingRoleId, canonicalName)")
    }

    private fun onValidationTimeout() {
        if (!hasPendingValidation) return
        hasPendingValidation = false
        TODO("APR: use JVM equivalent — show FSSetTitleRegionNotFound notification with REGION=pendingRegionName")
    }

    private fun onRegionChanged() {
        if (!dataLoaded) return

        TODO("APR: use JVM equivalent — get current region from gAgent; skip if null or empty name; check RLVa @setgroup lock; normalize region name; skip if same as lastAppliedRegion; look up assignment; if found verify group membership, check if title already active via LLGroupMgr, call sendGroupTitleUpdate and LLGroupActions.activate(); if no assignment and noneOnUnassigned deactivate group")
    }
}
