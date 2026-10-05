package com.firestorm.newview

import java.util.UUID

object PermissionsTracker {

    private const val PERMISSION_ENTRY_EXPIRY_SECONDS = 3600.0

    enum class PermType(val mask: UInt) {
        PERM_NONE(0u),
        PERM_FOLLOWCAM(1u)
    }

    data class PermissionsEntry(
        val ownerID: UUID? = null,
        val attachmentID: UUID? = null,
        var ownerName: String = "",
        var objectName: String = "",
        var timeSeconds: Double = 0.0,
        var type: UInt = 0u
    )

    val permissionsList: MutableMap<UUID, PermissionsEntry> = mutableMapOf()

    val requestedIDs: MutableList<UUID> = mutableListOf()

    // Nullable lambdas representing active avatar-name-cache subscriptions.
    // Key is the owner UUID; value is the disconnect handle (a no-arg lambda that cancels the subscription).
    val avatarNameCacheConnections: MutableMap<UUID, (() -> Unit)?> = mutableMapOf()

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    fun addPermissionsEntry(sourceId: UUID, permissionType: PermType) {
        if (!permissionsList.containsKey(sourceId)) {
            val entry = PermissionsEntry(objectName = loadingDataString())
            permissionsList[sourceId] = entry

            val viewerObject = findViewerObject(sourceId)
            if (viewerObject != null && isAgentAvatarValid() && agentHasRegion()) {
                permissionsList[sourceId] = entry.copy(
                    attachmentID = getAttachmentItemId(viewerObject)
                )
                requestedIDs.add(sourceId)
                sendObjectSelectDeselect(viewerObject)
            } else {
                permissionsList[sourceId] = entry.copy(objectName = objectOutOfRangeString())
            }
        }

        val existing = permissionsList[sourceId] ?: return
        permissionsList[sourceId] = existing.copy(
            type = existing.type or permissionType.mask,
            timeSeconds = totalSeconds()
        )

        purgePermissionsEntries()
    }

    fun removePermissionsEntry(sourceId: UUID, permissionType: PermType) {
        val existing = permissionsList[sourceId] ?: return

        permissionsList[sourceId] = existing.copy(
            type = existing.type and permissionType.mask.inv(),
            timeSeconds = totalSeconds()
        )

        purgePermissionsEntries()
    }

    fun purgePermissionsEntries() {
        val expiryTime = totalSeconds() - PERMISSION_ENTRY_EXPIRY_SECONDS
        val iterator = permissionsList.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.type == PermType.PERM_NONE.mask && entry.value.timeSeconds < expiryTime) {
                iterator.remove()
            }
        }
    }

    fun warnFollowcam() {
        var followcamList = ""

        for ((sourceId, entry) in permissionsList) {
            if (entry.type and PermType.PERM_FOLLOWCAM.mask == 0u) continue

            if (entry.attachmentID != null) {
                val attachmentPoint = getAttachedPointName(entry.attachmentID) ?: "???"
                val verb = "select?name=" + uriEscape(entry.objectName)
                followcamList += inventorySlurl(entry.attachmentID, verb) + " " +
                    transWornOnAttachmentPoint(attachmentPoint) + "\n"
            } else {
                val objectImSlurl = objectImSlurl(sourceId, entry.objectName, entry.ownerID)
                val regionSlurl = currentRegionSlurl()
                followcamList += objectImSlurl +
                    "&slurl=" + uriEscape(regionSlurl) + "\n"
            }
        }

        if (followcamList.isEmpty()) return

        System.err.println("PermissionsTracker: warnFollowcam LLNotificationsUtil::add not yet implemented")
    }

    fun objectPropertiesCallback(numBlocks: Int, getBlockData: (Int) -> Pair<UUID, Pair<String, UUID>>) {
        if (requestedIDs.isEmpty()) return

        for (index in 0 until numBlocks) {
            val (sourceId, nameAndOwner) = getBlockData(index)
            val (objectName, objectOwner) = nameAndOwner

            val iter = requestedIDs.indexOf(sourceId)
            if (iter < 0) continue

            requestedIDs.removeAt(iter)

            val existing = permissionsList[sourceId] ?: continue
            permissionsList[sourceId] = existing.copy(objectName = objectName, ownerID = objectOwner)

            val cachedName = getCachedAvatarName(objectOwner)
            if (cachedName != null) {
                permissionsList[sourceId] = permissionsList[sourceId]!!.copy(ownerName = cachedName)
            } else if (!avatarNameCacheConnections.containsKey(objectOwner)) {
                val disconnect = requestAvatarName(objectOwner) { avatarId, completeName ->
                    avatarNameCallback(avatarId, completeName)
                }
                avatarNameCacheConnections[objectOwner] = disconnect
            }
        }
    }

    fun avatarNameCallback(avatarId: UUID, completeName: String) {
        val conn = avatarNameCacheConnections[avatarId]
        conn?.invoke()
        avatarNameCacheConnections.remove(avatarId)

        for ((_, entry) in permissionsList) {
            if (entry.ownerID == avatarId) {
                // ownerName update requires a mutable field; PermissionsEntry.ownerName is var.
                entry.ownerName = completeName
            }
        }
    }

    // -----------------------------------------------------------------------
    // Platform/message stubs — replaced by JVM/Android equivalents
    // -----------------------------------------------------------------------

    private fun loadingDataString(): String {
        System.err.println("PermissionsTracker: loadingDataString not yet implemented")
        return ""
    }

    private fun objectOutOfRangeString(): String {
        System.err.println("PermissionsTracker: objectOutOfRangeString not yet implemented")
        return ""
    }

    private fun totalSeconds(): Double {
        System.err.println("PermissionsTracker: totalSeconds not yet implemented")
        return 0.0
    }

    private fun findViewerObject(id: UUID): Any? {
        System.err.println("PermissionsTracker: findViewerObject not yet implemented")
        return null
    }

    private fun isAgentAvatarValid(): Boolean {
        System.err.println("PermissionsTracker: isAgentAvatarValid not yet implemented")
        return false
    }

    private fun agentHasRegion(): Boolean {
        System.err.println("PermissionsTracker: agentHasRegion not yet implemented")
        return false
    }

    private fun getAttachmentItemId(viewerObject: Any): UUID? {
        System.err.println("PermissionsTracker: getAttachmentItemId not yet implemented")
        return null
    }

    private fun sendObjectSelectDeselect(viewerObject: Any) {
        System.err.println("PermissionsTracker: sendObjectSelectDeselect not yet implemented")
    }

    private fun getAttachedPointName(attachmentId: UUID): String? {
        System.err.println("PermissionsTracker: getAttachedPointName not yet implemented")
        return null
    }

    private fun uriEscape(text: String): String =
        java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20")

    private fun inventorySlurl(attachmentId: UUID, verb: String): String {
        System.err.println("PermissionsTracker: inventorySlurl not yet implemented")
        return ""
    }

    private fun objectImSlurl(sourceId: UUID, objectName: String, ownerId: UUID?): String {
        System.err.println("PermissionsTracker: objectImSlurl not yet implemented")
        return ""
    }

    private fun currentRegionSlurl(): String {
        System.err.println("PermissionsTracker: currentRegionSlurl not yet implemented")
        return ""
    }

    private fun transWornOnAttachmentPoint(point: String): String {
        System.err.println("PermissionsTracker: transWornOnAttachmentPoint not yet implemented")
        return ""
    }

    private fun getCachedAvatarName(avatarId: UUID): String? {
        System.err.println("PermissionsTracker: getCachedAvatarName not yet implemented")
        return null
    }

    private fun requestAvatarName(avatarId: UUID, callback: (UUID, String) -> Unit): (() -> Unit)? {
        System.err.println("PermissionsTracker: requestAvatarName not yet implemented")
        return null
    }
}
