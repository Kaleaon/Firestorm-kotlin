package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3

class LLPathfindingObject {

    typealias NameCallback = (LLPathfindingObject) -> Unit

    val uuid: LLUUID
    val name: String
    val description: String
    val ownerUUID: LLUUID
    val isGroupOwned: Boolean
    val location: Vector3

    private var hasOwnerName: Boolean = false
    private var ownerName: AvatarName = AvatarName()
    private var avatarNameCacheConnection: NameCallback? = null
    private val ownerNameListeners: MutableList<NameCallback> = mutableListOf()

    constructor() {
        uuid = LLUUID.NULL
        name = ""
        description = ""
        ownerUUID = LLUUID.NULL
        isGroupOwned = false
        location = Vector3.ZERO
    }

    constructor(pUUID: String, pObjectData: Map<String, Any>) {
        uuid = LLUUID.fromString(pUUID) ?: LLUUID.NULL
        val parsed = parseObjectData(pObjectData)
        name = parsed.name
        description = parsed.description
        ownerUUID = parsed.ownerUUID
        isGroupOwned = parsed.isGroupOwned
        location = parsed.location
        fetchOwnerName()
    }

    constructor(pOther: LLPathfindingObject) {
        uuid = pOther.uuid
        name = pOther.name
        description = pOther.description
        ownerUUID = pOther.ownerUUID
        isGroupOwned = pOther.isGroupOwned
        location = pOther.location
        fetchOwnerName()
    }

    fun hasOwner(): Boolean = ownerUUID.notNull()

    fun hasOwnerName(): Boolean = hasOwnerName

    fun getOwnerName(): String {
        return if (hasOwner()) ownerName.getDisplayName() else ""
    }

    fun registerOwnerNameListener(pOwnerNameCallback: NameCallback): NameCallback? {
        check(hasOwner())
        return if (hasOwnerName()) {
            pOwnerNameCallback(this)
            null
        } else {
            ownerNameListeners.add(pOwnerNameCallback)
            pOwnerNameCallback
        }
    }

    private fun fetchOwnerName() {
        hasOwnerName = false
        if (hasOwner()) {
            TODO("APR: use JVM equivalent — LLAvatarNameCache.get(ownerUUID) async lookup; on result call handleAvatarNameFetch")
        }
    }

    private fun handleAvatarNameFetch(pOwnerUUID: LLUUID, pAvatarName: AvatarName) {
        check(ownerUUID == pOwnerUUID)
        ownerName = pAvatarName
        hasOwnerName = true
        avatarNameCacheConnection = null
        ownerNameListeners.forEach { it(this) }
    }

    private data class ParsedObjectData(
        val name: String,
        val description: String,
        val ownerUUID: LLUUID,
        val isGroupOwned: Boolean,
        val location: Vector3
    )

    private fun parseObjectData(pObjectData: Map<String, Any>): ParsedObjectData {
        val parsedName = pObjectData[NAME_FIELD] as? String ?: ""
        val parsedDescription = pObjectData[DESCRIPTION_FIELD] as? String ?: ""
        val ownerStr = pObjectData[OWNER_FIELD] as? String ?: ""
        val parsedOwnerUUID = LLUUID.fromString(ownerStr) ?: LLUUID.NULL
        val parsedIsGroupOwned = pObjectData[IS_GROUP_OWNED_FIELD] as? Boolean ?: false
        @Suppress("UNCHECKED_CAST")
        val posArray = pObjectData[POSITION_FIELD] as? List<Number>
        val parsedLocation = if (posArray != null && posArray.size >= 3) {
            Vector3(posArray[0].toFloat(), posArray[1].toFloat(), posArray[2].toFloat())
        } else {
            Vector3.ZERO
        }
        return ParsedObjectData(parsedName, parsedDescription, parsedOwnerUUID, parsedIsGroupOwned, parsedLocation)
    }

    companion object {
        private const val NAME_FIELD = "name"
        private const val DESCRIPTION_FIELD = "description"
        private const val OWNER_FIELD = "owner"
        private const val POSITION_FIELD = "position"
        private const val IS_GROUP_OWNED_FIELD = "owner_is_group"
    }
}
