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

        TODO("APR: LLNotificationsUtil::add(\"WarnScriptedCamera\", args[SOURCES=$followcamList])")
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
        TODO("APR: use JVM equivalent for LLTrans::getString(\"LoadingData\")")
    }

    private fun objectOutOfRangeString(): String {
        TODO("APR: use JVM equivalent for LLTrans::getString(\"ObjectOutOfRange\")")
    }

    private fun totalSeconds(): Double {
        TODO("APR: use JVM equivalent for LLTimer::getTotalSeconds()")
    }

    private fun findViewerObject(id: UUID): Any? {
        TODO("APR: use JVM equivalent for gObjectList.findObject($id)")
    }

    private fun isAgentAvatarValid(): Boolean {
        TODO("APR: use JVM equivalent for isAgentAvatarValid()")
    }

    private fun agentHasRegion(): Boolean {
        TODO("APR: use JVM equivalent for gAgentAvatarp->getRegion() != null")
    }

    private fun getAttachmentItemId(viewerObject: Any): UUID? {
        TODO("APR: use JVM equivalent for LLViewerObject::getAttachmentItemID()")
    }

    private fun sendObjectSelectDeselect(viewerObject: Any) {
        TODO("APR: use JVM equivalent for LLMessageSystem ObjectSelect/ObjectDeselect to get ObjectProperties")
    }

    private fun getAttachedPointName(attachmentId: UUID): String? {
        TODO("APR: use JVM equivalent for gAgentAvatarp->getAttachedPointName($attachmentId)")
    }

    private fun uriEscape(text: String): String =
        java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20")

    private fun inventorySlurl(attachmentId: UUID, verb: String): String {
        TODO("APR: use JVM equivalent for LLSLURL(\"inventory\", $attachmentId, \"$verb\").getSLURLString()")
    }

    private fun objectImSlurl(sourceId: UUID, objectName: String, ownerId: UUID?): String {
        TODO("APR: use JVM equivalent for LLSLURL(\"objectim\", $sourceId, \"\").getSLURLString() + query params")
    }

    private fun currentRegionSlurl(): String {
        TODO("APR: use JVM equivalent for LLSLURL(region->getName(), agentPosition).getLocationString()")
    }

    private fun transWornOnAttachmentPoint(point: String): String {
        TODO("APR: use JVM equivalent for LLTrans::getString(\"WornOnAttachmentPoint\", args[ATTACHMENT_POINT=$point])")
    }

    private fun getCachedAvatarName(avatarId: UUID): String? {
        TODO("APR: use JVM equivalent for LLAvatarNameCache::get($avatarId) synchronous variant")
    }

    private fun requestAvatarName(avatarId: UUID, callback: (UUID, String) -> Unit): (() -> Unit)? {
        TODO("APR: use JVM equivalent for LLAvatarNameCache::get($avatarId, callback); return disconnect lambda")
    }
}
