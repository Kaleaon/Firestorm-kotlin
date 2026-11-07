package com.firestorm.newview

import java.util.UUID

enum class ContactSetType {
    CHAT, IM, TAG, RADAR, MINIMAP, FRIENDS
}

enum class ContactSetAutoresponseMode {
    BUSY, AUTORESPONSE, AUTORESPONSE_NONFRIENDS
}

object LGGContactSets {

    const val SET_ALL_SETS = "All Sets"
    const val SET_NO_SETS = "No Sets"
    const val SET_EXTRA_AVS = "extraAvs"
    const val SET_PSEUDONYM = "Pseudonyms"
    const val GLOBAL_SETTINGS = "globalSettings"
    const val PSEUDONYM = "--- ---"
    const val PSEUDONYM_QUOTED = "'--- ---'"

    private const val COLOR_DAMPENING = 0.8f
    private const val CONTACT_SETS_FILE = "settings_friends_groups.xml"

    data class Color4(val r: Float, val g: Float, val b: Float, var a: Float) {
        companion object {
            val grey = Color4(0.5f, 0.5f, 0.5f, 1.0f)
            val white = Color4(1f, 1f, 1f, 1f)
            val grey3 = Color4(0.3f, 0.3f, 0.3f, 1f)
            val red = Color4(1f, 0f, 0f, 1f)
            val blue = Color4(0f, 0f, 1f, 1f)
        }
    }

    class ContactSet(
        var name: String = "",
        val friends: MutableSet<UUID> = mutableSetOf(),
        var notify: Boolean = false,
        var sortByOnlineStatus: Boolean = true,
        var color: Color4 = Color4.grey,
        var autoresponseBusyEnabled: Boolean = false,
        var autoresponseBusy: String = "",
        var autoresponseModeEnabled: Boolean = false,
        var autoresponseMode: String = "",
        var autoresponseNonFriendsEnabled: Boolean = false,
        var autoresponseNonFriends: String = ""
    ) {
        fun hasFriend(avatarId: UUID): Boolean = friends.contains(avatarId)
    }

    enum class ContactSetUpdate { UPDATED_MEMBERS, UPDATED_LISTS }

    private val contactSets: MutableMap<String, ContactSet> = mutableMapOf()
    var defaultColor: Color4 = Color4.grey
    private val extraAvatars: MutableSet<UUID> = mutableSetOf()
    private val pseudonyms: MutableMap<UUID, String> = mutableMapOf()

    private val changedListeners: MutableList<(ContactSetUpdate) -> Unit> = mutableListOf()

    fun addChangeListener(listener: (ContactSetUpdate) -> Unit) {
        changedListeners.add(listener)
    }

    private fun notifyChanged(update: ContactSetUpdate) {
        changedListeners.forEach { it(update) }
    }

    fun loadFromDisk() {
        System.err.println("LGGContactSets: loadFromDisk not yet implemented")
    }

    private fun saveToDisk() {
        System.err.println("LGGContactSets: saveToDisk not yet implemented")
    }

    private fun toneDownColor(color: Color4): Color4 = color.copy(a = COLOR_DAMPENING)

    fun setSetColor(setName: String, color: Color4) {
        getContactSet(setName)?.let { it.color = color; saveToDisk() }
    }

    fun getSetColor(setName: String): Color4 = getContactSet(setName)?.color ?: defaultColor

    fun getFriendColor(friendId: UUID, ignoredSetName: String = ""): Color4 {
        if (ignoredSetName == SET_NO_SETS) return defaultColor

        var lowest = UInt.MAX_VALUE
        var result = defaultColor

        for (setName in getFriendSets(friendId)) {
            if (setName != ignoredSetName) {
                val setSize = getFriendsInSet(setName).size.toUInt()
                if (setSize == 0u) continue
                if (setSize < lowest) {
                    lowest = setSize
                    result = contactSets[setName]!!.color
                    if (isNonFriend(friendId)) result = toneDownColor(result)
                }
            }
        }

        if (lowest == UInt.MAX_VALUE) {
            if (isFriendInSet(friendId, ignoredSetName) && !isInternalSetName(ignoredSetName)) {
                return contactSets[ignoredSetName]!!.color
            }
        }
        return result
    }

    fun colorize(uuid: UUID, color: Color4, type: ContactSetType): Color4 {
        System.err.println("LGGContactSets: colorize not yet implemented")
        return defaultColor
    }

    fun getPseudonym(friendId: UUID): String {
        val raw = pseudonyms[friendId] ?: return ""
        return "'$raw'"
    }

    fun hasPseudonym(friendId: UUID): Boolean = getPseudonym(friendId).isNotEmpty()

    fun hasPseudonym(ids: List<UUID>): Boolean = ids.any { hasPseudonym(it) }

    fun clearPseudonym(friendId: UUID, saveChanges: Boolean = true) {
        if (pseudonyms.remove(friendId) != null) {
            System.err.println("LGGContactSets: clearPseudonym avatar name cache invalidation not yet implemented")
            if (saveChanges) saveToDisk()
            notifyChanged(ContactSetUpdate.UPDATED_MEMBERS)
        }
    }

    fun removeDisplayName(friendId: UUID) = setPseudonym(friendId, PSEUDONYM)

    fun hasDisplayNameRemoved(friendId: UUID): Boolean = getPseudonym(friendId) == PSEUDONYM_QUOTED

    fun hasDisplayNameRemoved(ids: List<UUID>): Boolean = ids.any { hasDisplayNameRemoved(it) }

    fun checkCustomName(id: UUID): Triple<Boolean, Boolean, String> {
        val dnRemoved = hasDisplayNameRemoved(id)
        val pseudonym = getPseudonym(id)
        return Triple(hasPseudonym(id), dnRemoved, pseudonym)
    }

    fun getFriendSets(friendId: UUID): List<String> =
        contactSets.values.filter { it.hasFriend(friendId) }.map { it.name }

    fun getAllContactSets(): List<String> = contactSets.values.map { it.name }

    fun addToSet(avatarIds: List<UUID>, setName: String) {
        for (avatarId in avatarIds) {
            System.err.println("LGGContactSets: addToSet isBuddy check not yet implemented")
            extraAvatars.add(avatarId)
            contactSets[setName]?.friends?.add(avatarId)
        }
        saveToDisk()
        notifyChanged(ContactSetUpdate.UPDATED_MEMBERS)
    }

    fun removeFriendFromSet(friendId: UUID, setName: String, saveChanges: Boolean = true) {
        when (setName) {
            SET_EXTRA_AVS -> return removeNonFriendFromList(friendId, saveChanges)
            SET_PSEUDONYM -> return clearPseudonym(friendId, saveChanges)
        }
        getContactSet(setName)?.let {
            it.friends.remove(friendId)
            if (saveChanges) { saveToDisk(); notifyChanged(ContactSetUpdate.UPDATED_MEMBERS) }
        }
    }

    fun removeFriendFromAllSets(friendId: UUID, saveChanges: Boolean = true) {
        for (setName in getFriendSets(friendId)) {
            removeFriendFromSet(friendId, setName, saveChanges)
        }
    }

    fun isFriendInSet(friendId: UUID, setName: String): Boolean = when (setName) {
        SET_ALL_SETS -> isFriendInAnySet(friendId)
        SET_NO_SETS -> !isFriendInAnySet(friendId)
        SET_PSEUDONYM -> hasPseudonym(friendId)
        SET_EXTRA_AVS -> isNonFriend(friendId)
        "" -> false
        else -> getContactSet(setName)?.hasFriend(friendId) ?: false
    }

    fun hasFriendColorThatShouldShow(friendId: UUID, type: ContactSetType): Boolean {
        System.err.println("LGGContactSets: hasFriendColorThatShouldShow not yet implemented")
        return false
    }

    fun hasFriendColorThatShouldShow(friendId: UUID, type: ContactSetType, colorOut: (Color4) -> Unit): Boolean {
        System.err.println("LGGContactSets: hasFriendColorThatShouldShow not yet implemented")
        return false
    }

    fun addSet(setName: String) {
        if (!isInternalSetName(setName) && !isValidSet(setName)) {
            contactSets[setName] = ContactSet(name = setName, color = defaultColor)
            saveToDisk()
            notifyChanged(ContactSetUpdate.UPDATED_LISTS)
        }
    }

    fun renameSet(setName: String, newSetName: String): Boolean {
        if (!isInternalSetName(setName) && isValidSet(setName) &&
            !isInternalSetName(newSetName) && !isValidSet(newSetName)
        ) {
            val set = contactSets.remove(setName)!!
            set.name = newSetName
            contactSets[newSetName] = set
            saveToDisk()
            notifyChanged(ContactSetUpdate.UPDATED_LISTS)
            return true
        }
        return false
    }

    fun removeSet(setName: String) {
        val set = contactSets[setName] ?: return
        val toRemove = set.friends.filter { friendId ->
            System.err.println("LGGContactSets: removeSet isBuddy check not yet implemented")
            getFriendSets(friendId).size == 1 && !hasPseudonym(friendId)
        }
        for (id in toRemove) removeNonFriendFromList(id, false)
        contactSets.remove(setName)
        saveToDisk()
        notifyChanged(ContactSetUpdate.UPDATED_LISTS)
    }

    fun removeNonFriendFromList(nonFriendId: UUID, saveChanges: Boolean = true) {
        if (extraAvatars.remove(nonFriendId)) {
            System.err.println("LGGContactSets: removeNonFriendFromList isBuddy check not yet implemented")
            if (saveChanges) { saveToDisk(); notifyChanged(ContactSetUpdate.UPDATED_MEMBERS) }
        }
    }

    fun isNonFriend(nonFriendId: UUID): Boolean {
        System.err.println("LGGContactSets: isNonFriend not yet implemented")
        return false
    }

    fun isFriendInAnySet(friendId: UUID): Boolean =
        contactSets.values.any { it.hasFriend(friendId) }

    fun getFriendsInAnySet(): List<UUID> =
        contactSets.values.flatMap { it.friends }.toSet().toList()

    fun getListOfNonFriends(): List<UUID> {
        System.err.println("LGGContactSets: getListOfNonFriends not yet implemented")
        return emptyList()
    }

    fun getListOfPseudonymAvs(): List<UUID> = pseudonyms.keys.toList()

    fun notifyForFriend(friendId: UUID): Boolean =
        getFriendSets(friendId).any { contactSets[it]?.notify == true }

    fun setNotifyForSet(setName: String, notify: Boolean) {
        getContactSet(setName)?.let { it.notify = notify; saveToDisk() }
    }

    fun getNotifyForSet(setName: String): Boolean = getContactSet(setName)?.notify ?: false

    fun setSortByOnlineStatusForSet(setName: String, sortByOnlineStatus: Boolean) {
        getContactSet(setName)?.let {
            it.sortByOnlineStatus = sortByOnlineStatus
            saveToDisk()
            notifyChanged(ContactSetUpdate.UPDATED_MEMBERS)
        }
    }

    fun getSortByOnlineStatusForSet(setName: String): Boolean =
        getContactSet(setName)?.sortByOnlineStatus ?: false

    fun setAutoresponseForSet(
        setName: String,
        mode: ContactSetAutoresponseMode,
        enabled: Boolean,
        response: String
    ) {
        val set = getContactSet(setName) ?: return
        when (mode) {
            ContactSetAutoresponseMode.BUSY -> { set.autoresponseBusyEnabled = enabled; set.autoresponseBusy = response }
            ContactSetAutoresponseMode.AUTORESPONSE -> { set.autoresponseModeEnabled = enabled; set.autoresponseMode = response }
            ContactSetAutoresponseMode.AUTORESPONSE_NONFRIENDS -> { set.autoresponseNonFriendsEnabled = enabled; set.autoresponseNonFriends = response }
        }
        saveToDisk()
    }

    fun getAutoresponseForSet(
        setName: String,
        mode: ContactSetAutoresponseMode
    ): Pair<Boolean, String> {
        val set = getContactSet(setName) ?: return Pair(false, "")
        return when (mode) {
            ContactSetAutoresponseMode.BUSY -> Pair(set.autoresponseBusyEnabled, set.autoresponseBusy)
            ContactSetAutoresponseMode.AUTORESPONSE -> Pair(set.autoresponseModeEnabled, set.autoresponseMode)
            ContactSetAutoresponseMode.AUTORESPONSE_NONFRIENDS -> Pair(set.autoresponseNonFriendsEnabled, set.autoresponseNonFriends)
        }
    }

    fun getAutoresponseForFriend(
        friendId: UUID,
        mode: ContactSetAutoresponseMode
    ): String? {
        var bestSize = UInt.MAX_VALUE
        var result: String? = null
        for (setName in getFriendSets(friendId)) {
            val set = getContactSet(setName) ?: continue
            val (enabled, message) = getAutoresponseForSet(setName, mode)
            if (!enabled || message.isEmpty()) continue
            val size = set.friends.size.toUInt()
            if (result == null || size < bestSize) {
                result = message
                bestSize = size
            }
        }
        return result
    }

    fun isValidSet(setName: String): Boolean = contactSets.containsKey(setName)

    fun isInternalSetName(setName: String): Boolean = setName.isEmpty() ||
        setName == SET_EXTRA_AVS ||
        setName == SET_PSEUDONYM ||
        setName == SET_NO_SETS ||
        setName == SET_ALL_SETS ||
        setName == GLOBAL_SETTINGS

    fun hasSets(): Boolean = contactSets.isNotEmpty()

    fun getContactSet(setName: String): ContactSet? {
        if (setName.isEmpty()) return null
        return contactSets[setName]
    }

    private fun setPseudonym(friendId: UUID, pseudonym: String) {
        pseudonyms[friendId] = pseudonym
        System.err.println("LGGContactSets: setPseudonym avatar name cache invalidation not yet implemented")
        saveToDisk()
        notifyChanged(ContactSetUpdate.UPDATED_MEMBERS)
    }

    private fun getFriendsInSet(setName: String): List<UUID> = when (setName) {
        SET_ALL_SETS -> getFriendsInAnySet()
        SET_NO_SETS -> emptyList()
        SET_PSEUDONYM -> getListOfPseudonymAvs()
        SET_EXTRA_AVS -> getListOfNonFriends()
        else -> getContactSet(setName)?.friends?.toList() ?: emptyList()
    }

    fun callbackAliasReset(notificationOption: Int, agentId: UUID): Boolean {
        if (notificationOption == 0) clearPseudonym(agentId)
        return false
    }

    fun handleAddContactSetCallback(option: Int, setName: String): Boolean {
        if (option == 0) addSet(setName)
        return false
    }

    fun handleRemoveContactSetCallback(option: Int, contactSet: String): Boolean {
        if (option == 0) removeSet(contactSet)
        return false
    }

    fun handleRemoveAvatarFromSetCallback(
        option: Int,
        setName: String,
        ids: List<UUID>
    ): Boolean {
        if (option == 0) {
            for (id in ids) {
                removeFriendFromSet(id, setName, false)
                System.err.println("LGGContactSets: handleRemoveAvatarFromSetCallback isBuddy check not yet implemented")
            }
            saveToDisk()
            notifyChanged(ContactSetUpdate.UPDATED_MEMBERS)
        }
        return false
    }

    fun handleSetAvatarPseudonymCallback(option: Int, pseudonym: String, ids: List<UUID>): Boolean {
        if (option == 0) ids.forEach { setPseudonym(it, pseudonym) }
        return false
    }
}
