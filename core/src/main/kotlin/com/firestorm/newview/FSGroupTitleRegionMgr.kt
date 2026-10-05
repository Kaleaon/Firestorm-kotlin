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
        System.err.println("FSGroupTitleRegionMgr: init not yet implemented")
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

        System.err.println("FSGroupTitleRegionMgr: loadFromDisk not yet implemented")
    }

    fun saveToDisk() {
        System.err.println("FSGroupTitleRegionMgr: saveToDisk not yet implemented")
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
        System.err.println("FSGroupTitleRegionMgr: setAssignmentForCurrentRegion not yet implemented")
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
        System.err.println("FSGroupTitleRegionMgr: showRegionInputDialog not yet implemented")
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

        System.err.println("FSGroupTitleRegionMgr: validateAndSetAssignment not yet implemented")
    }

    private fun cancelPendingValidation() {
        hasPendingValidation = false
        validationTimerHandle = null
    }

    private fun onValidationResult(regionHandle: Long) {
        if (!hasPendingValidation) return
        System.err.println("FSGroupTitleRegionMgr: onValidationResult not yet implemented")
    }

    private fun onValidationTimeout() {
        if (!hasPendingValidation) return
        hasPendingValidation = false
        System.err.println("FSGroupTitleRegionMgr: onValidationTimeout not yet implemented")
    }

    private fun onRegionChanged() {
        if (!dataLoaded) return

        System.err.println("FSGroupTitleRegionMgr: onRegionChanged not yet implemented")
    }
}
