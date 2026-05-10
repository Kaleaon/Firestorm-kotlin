package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

interface AvatarTrackerObserver {
    fun changed(changed: MutableSet<LLUUID>)
}

object BuddyRights {
    const val ONLINE: Int         = 1
    const val MAP: Int            = 2
    const val MODIFY_OBJECTS: Int = 4
}

object AvatarTracker {

    data class BuddyInfo(val rightsGranted: Int, val rightsHas: Int)

    val buddies: MutableMap<LLUUID, BuddyInfo> = mutableMapOf()

    val trackedAgentId: LLUUID = LLUUID.NULL
    var isTrackingAgent: Boolean = false

    private val observers: MutableList<AvatarTrackerObserver> = mutableListOf()

    // ── Buddy management ─────────────────────────────────────────────────────

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

    // ── Queries ──────────────────────────────────────────────────────────────

    // Returns buddies that have granted us online-visibility rights, taken
    // here as a proxy for "online" since actual presence isn't tracked locally.
    fun findOnline(): List<LLUUID> =
        buddies.entries
            .filter { (_, info) -> (info.rightsHas and BuddyRights.ONLINE) != 0 }
            .map { it.key }

    // ── Observers ────────────────────────────────────────────────────────────

    fun addObserver(obs: AvatarTrackerObserver) {
        if (!observers.contains(obs)) observers.add(obs)
    }

    fun removeObserver(obs: AvatarTrackerObserver) {
        observers.remove(obs)
    }

    fun notifyObservers(changed: MutableSet<LLUUID>) {
        observers.toList().forEach { it.changed(changed) }
    }
}
