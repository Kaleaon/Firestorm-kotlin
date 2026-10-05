package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.*
import com.firestorm.llmessage.*

// ── CallingCard data class ────────────────────────────────────────────────────

/**
 * Immutable record representing a Second Life calling card.
 *
 * A calling card is the viewer-side representation of a friendship / contact:
 * it stores the friend's agent UUID and display name.  In C++ this data is
 * embedded inside LLAvatarTracker's buddy_map_t entries; here it is a
 * first-class value type so it can be stored and passed around without
 * coupling callers to the tracker singleton.
 */
data class CallingCard(
    val id: LLUUID,
    val name: String,
)

// ── Observer interface ────────────────────────────────────────────────────────

/**
 * Observer for calling-card inventory changes.
 *
 * Mirrors the role of C++ `LLFriendObserver` but scoped to the calling-card
 * manager rather than the full avatar-tracker.  Implement this interface and
 * register with [CallingCardManager] to be notified when cards are added or
 * removed from the local cache.
 */
interface CallingCardObserver {
    /** Called after a new [CallingCard] has been inserted into the manager. */
    fun newItem(card: CallingCard)

    /** Called after a [CallingCard] has been removed from the manager. */
    fun removeItem(id: LLUUID)
}

// ── FriendObserver change-mask constants ─────────────────────────────────────

/**
 * Bitmask flags used to describe what changed in a friend-list notification.
 * Mirrors the anonymous enum inside C++ `LLFriendObserver`.
 */
object FriendChangeMask {
    const val NONE: UInt   = 0u
    const val ADD: UInt    = 1u
    const val REMOVE: UInt = 2u
    const val ONLINE: UInt = 4u
    const val POWERS: UInt = 8u
    const val PERMS: UInt  = 16u
    const val ALL: UInt    = 0xFFFFFFFFu
}

// ── CallingCardManager singleton ──────────────────────────────────────────────

/**
 * Manages the local cache of [CallingCard] objects and notifies registered
 * [CallingCardObserver]s of changes.
 *
 * In C++ the calling-card list is an implicit side-effect of
 * `LLAvatarTracker`'s buddy map; here it is an explicit singleton so the
 * UI layer can interact with it without depending on the full tracker.
 */
object CallingCardManager {

    // ── Card storage ──────────────────────────────────────────────────────────

    val cards: MutableMap<LLUUID, CallingCard> = mutableMapOf()

    // ── Observer management ───────────────────────────────────────────────────

    private val observers: MutableList<CallingCardObserver> = mutableListOf()

    fun addObserver(obs: CallingCardObserver) {
        if (!observers.contains(obs)) observers.add(obs)
    }

    fun removeObserver(obs: CallingCardObserver) {
        observers.remove(obs)
    }

    private fun notifyNewItem(card: CallingCard) {
        observers.toList().forEach { it.newItem(card) }
    }

    private fun notifyRemoveItem(id: LLUUID) {
        observers.toList().forEach { it.removeItem(id) }
    }

    // ── CRUD operations ───────────────────────────────────────────────────────

    /**
     * Insert or replace a calling card in the local cache.
     * Observers are notified via [CallingCardObserver.newItem].
     */
    fun addCard(card: CallingCard) {
        cards[card.id] = card
        notifyNewItem(card)
    }

    /**
     * Remove a calling card by agent UUID.
     * Observers are notified via [CallingCardObserver.removeItem] only if
     * the card was actually present.
     */
    fun removeCard(id: LLUUID) {
        if (cards.remove(id) != null) {
            notifyRemoveItem(id)
        }
    }

    /**
     * Look up a calling card by agent UUID.
     * @return The [CallingCard] or null if not cached locally.
     */
    fun getCard(id: LLUUID): CallingCard? = cards[id]

    /**
     * Request a calling card from the server for the given agent UUID.
     *
     * In C++ this sends an `OfferCallingCard` UDP message; the response is
     * handled by `process_accept_callingcard`, which calls [addCard].
     * Here the network send is stubbed; callers should await the observer
     * callback for the result.
     */
    fun requestCard(id: LLUUID): Unit {
        System.err.println("CallingCardManager: requestCard not yet implemented")
    }

    // ── Message handler hooks ─────────────────────────────────────────────────

    /**
     * Process an accepted calling card from the server.
     * Decodes the agent UUID and name, constructs a [CallingCard], and
     * calls [addCard].
     */
    fun onAcceptCallingCard(msg: LLMessageSystem, userData: Any?) {
        System.err.println("CallingCardManager: onAcceptCallingCard not yet implemented")
    }

    /**
     * Process a declined calling card from the server.
     * Decodes the agent UUID and calls [removeCard] if present.
     */
    fun onDeclineCallingCard(msg: LLMessageSystem, userData: Any?) {
        System.err.println("CallingCardManager: onDeclineCallingCard not yet implemented")
    }

    // ── Buddy-list bridge ─────────────────────────────────────────────────────

    /**
     * Synchronise calling cards with the avatar tracker's buddy list.
     *
     * Called after the buddy list arrives from the server so that every
     * friend has a corresponding [CallingCard] in the local cache.
     */
    fun syncWithBuddyList(buddies: Map<LLUUID, String>) {
        buddies.forEach { (id, name) ->
            if (!cards.containsKey(id)) addCard(CallingCard(id, name))
        }
    }
}
