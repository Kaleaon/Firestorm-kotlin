package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

interface AvatarTrackerObserver {
    fun changed(changed: MutableSet<LLUUID>)
}

object BuddyRights {
    const val ONLINE: Int         = 1 shl 0
    const val MAP: Int            = 1 shl 1
    const val MODIFY_OBJECTS: Int = 1 shl 2
}

object AvatarTracker {

    data class BuddyInfo(
        val rightsGranted: Int,
        val rightsHas: Int
    ) {
        fun isOnline(): Boolean = (rightsHas and BuddyRights.ONLINE) != 0
        fun canSeeOnMap(): Boolean = (rightsHas and BuddyRights.MAP) != 0
        fun canModifyObjects(): Boolean = (rightsHas and BuddyRights.MODIFY_OBJECTS) != 0
        fun grantsOnline(): Boolean = (rightsGranted and BuddyRights.ONLINE) != 0
        fun grantsMap(): Boolean = (rightsGranted and BuddyRights.MAP) != 0
        fun grantsModifyObjects(): Boolean = (rightsGranted and BuddyRights.MODIFY_OBJECTS) != 0
    }

    val buddies: MutableMap<LLUUID, BuddyInfo> = mutableMapOf()

    var trackedAgentId: LLUUID = LLUUID.NULL
    var isTrackingAgent: Boolean = false

    private val observers: MutableList<AvatarTrackerObserver> = mutableListOf()

    fun isBuddy(id: LLUUID): Boolean = buddies.containsKey(id)

    fun getBuddyInfo(id: LLUUID): BuddyInfo? = buddies[id]

    fun addBuddy(id: LLUUID, info: BuddyInfo) {
        buddies[id] = info
        notifyObservers(mutableSetOf(id))
    }

    fun removeBuddy(id: LLUUID) {
        buddies.remove(id)
        notifyObservers(mutableSetOf(id))
    }

    fun getBuddyCount(): Int = buddies.size

    fun applyFunctionToFriends(fn: (LLUUID, BuddyInfo) -> Unit) {
        buddies.forEach { (id, info) -> fn(id, info) }
    }

    fun findOnline(): List<LLUUID> =
        buddies.entries
            .filter { (_, info) -> info.isOnline() }
            .map { it.key }

    fun isOnline(id: LLUUID): Boolean = buddies[id]?.isOnline() ?: false

    fun setBuddyRightsGranted(id: LLUUID, rights: Int) {
        val info = buddies[id] ?: return
        buddies[id] = info.copy(rightsGranted = rights)
        notifyObservers(mutableSetOf(id))
    }

    fun setBuddyRightsHas(id: LLUUID, rights: Int) {
        val info = buddies[id] ?: return
        buddies[id] = info.copy(rightsHas = rights)
        notifyObservers(mutableSetOf(id))
    }

    fun addObserver(obs: AvatarTrackerObserver) {
        if (!observers.contains(obs)) observers.add(obs)
    }

    fun removeObserver(obs: AvatarTrackerObserver) {
        observers.remove(obs)
    }

    fun notifyObservers(changed: MutableSet<LLUUID>) {
        observers.toList().forEach { it.changed(changed) }
    }

    fun clear() {
        buddies.clear()
        trackedAgentId = LLUUID.NULL
        isTrackingAgent = false
    }
}
