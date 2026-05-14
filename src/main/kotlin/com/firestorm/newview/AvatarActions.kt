/**
 * AvatarActions.kt
 *
 * Kotlin conversion of llavataractions.h / llavataractions.cpp
 * Original: Friend-related actions (add, remove, offer teleport, etc.)
 * Copyright (C) 2009-2010, Linden Research, Inc. — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

/**
 * Static action dispatchers for avatar-related operations (mute, pay,
 * teleport, friend management, moderation, etc.).  All methods are top-level
 * stubs — real implementations require the full viewer runtime.
 */
object AvatarActions {

    // -------------------------------------------------------------------------
    // Profile
    // -------------------------------------------------------------------------

    /** Show the profile floater for [id] and return its backing data record. */
    fun profileData(id: LLUUID): Unit = System.err.println("AvatarActions: profileData not yet implemented")

    /** Show the profile floater for [avatarId]. */
    fun showProfile(avatarId: LLUUID): Unit = System.err.println("AvatarActions: showProfile not yet implemented")

    /** Hide the profile floater for [avatarId]. */
    fun hideProfile(avatarId: LLUUID): Unit = System.err.println("AvatarActions: hideProfile not yet implemented")

    /** @return true if the profile floater for [avatarId] is currently open. */
    fun profileVisible(avatarId: LLUUID): Boolean {
        System.err.println("AvatarActions: profileVisible not yet implemented")
        return false
    }

    // -------------------------------------------------------------------------
    // Friendship
    // -------------------------------------------------------------------------

    /** Show the friendship-offer dialog for the avatar identified by [id]. */
    fun requestFriendshipDialog(id: LLUUID): Unit = System.err.println("AvatarActions: requestFriendshipDialog not yet implemented")

    /** Show the friendship-offer dialog for the avatar identified by [id] and [name]. */
    fun requestFriendshipDialog(id: LLUUID, name: String): Unit =
        System.err.println("AvatarActions: requestFriendshipDialog not yet implemented")

    /** Show the friend-removal confirmation dialog for [id]. */
    fun removeFriendDialog(id: LLUUID): Unit = System.err.println("AvatarActions: removeFriendDialog not yet implemented")

    /** Show the friend-removal confirmation dialog for multiple [ids]. */
    fun removeFriendsDialog(ids: List<LLUUID>): Unit = System.err.println("AvatarActions: removeFriendsDialog not yet implemented")

    /** @return true if the avatar [id] is on the local agent's friend list. */
    fun isFriend(id: LLUUID): Boolean {
        System.err.println("AvatarActions: isFriend not yet implemented")
        return false
    }

    // -------------------------------------------------------------------------
    // Instant messaging
    // -------------------------------------------------------------------------

    /** Open (or focus) an IM session with avatar [id]. */
    fun startIM(id: LLUUID): Unit = System.err.println("AvatarActions: startIM not yet implemented")

    /** End an existing IM session with avatar [id]. */
    fun endIM(id: LLUUID): Unit = System.err.println("AvatarActions: endIM not yet implemented")

    // -------------------------------------------------------------------------
    // Voice
    // -------------------------------------------------------------------------

    /** Begin a P2P voice call with avatar [id]. */
    fun startCall(id: LLUUID): Unit = System.err.println("AvatarActions: startCall not yet implemented")

    /** @return true if the viewer can currently initiate voice calls. */
    fun canCall(): Boolean {
        System.err.println("AvatarActions: canCall not yet implemented")
        return false
    }

    // -------------------------------------------------------------------------
    // Teleport
    // -------------------------------------------------------------------------

    /** Send a teleport offer to avatar [id]. */
    fun offerTeleport(id: LLUUID): Unit = System.err.println("AvatarActions: offerTeleport not yet implemented")

    /** Send a teleport offer to multiple avatars [ids]. */
    fun offerTeleport(ids: List<LLUUID>): Unit = System.err.println("AvatarActions: offerTeleport not yet implemented")

    /**
     * Request a teleport from avatar [id].
     * Mapped from C++ `teleportRequest()`; renamed for clarity.
     */
    fun requestTeleport(id: LLUUID): Unit = System.err.println("AvatarActions: requestTeleport not yet implemented")

    /**
     * @return false if [id] is a friend who is not currently visible online
     *         (offline friends cannot receive teleport offers).
     */
    fun canOfferTeleport(id: LLUUID): Boolean {
        System.err.println("AvatarActions: canOfferTeleport not yet implemented")
        return false
    }

    /**
     * @return true if a teleport request can be sent to avatar [id].
     */
    fun canRequestTeleport(id: LLUUID): Boolean {
        System.err.println("AvatarActions: canRequestTeleport not yet implemented")
        return false
    }

    // -------------------------------------------------------------------------
    // Economy
    // -------------------------------------------------------------------------

    /** Open the L$ payment dialog targeted at avatar [id]. */
    fun pay(id: LLUUID): Unit = System.err.println("AvatarActions: pay not yet implemented")

    // -------------------------------------------------------------------------
    // Mute / block
    // -------------------------------------------------------------------------

    /**
     * Add avatar [id] (display name [name]) to the mute list.
     * Corresponds to the viewer's `toggleMute` with a mute-add intent.
     */
    fun mute(id: LLUUID, name: String): Unit = System.err.println("AvatarActions: mute not yet implemented")

    /** Remove avatar [id] from the mute list. */
    fun unmute(id: LLUUID): Unit = System.err.println("AvatarActions: unmute not yet implemented")

    /** @return true if avatar [id] is on the local mute list. */
    fun isMuted(id: LLUUID): Boolean {
        System.err.println("AvatarActions: isMuted not yet implemented")
        return false
    }

    /** Toggle the text/object block for avatar [id]; returns the new blocked state. */
    fun toggleBlock(id: LLUUID): Boolean {
        System.err.println("AvatarActions: toggleBlock not yet implemented")
        return false
    }

    /** @return true if avatar [id] is currently blocked. */
    fun isBlocked(id: LLUUID): Boolean {
        System.err.println("AvatarActions: isBlocked not yet implemented")
        return false
    }

    /** Toggle voice mute for avatar [id]. */
    fun toggleMuteVoice(id: LLUUID): Unit = System.err.println("AvatarActions: toggleMuteVoice not yet implemented")

    /** @return true if avatar [id]'s voice is muted. */
    fun isVoiceMuted(id: LLUUID): Boolean {
        System.err.println("AvatarActions: isVoiceMuted not yet implemented")
        return false
    }

    // -------------------------------------------------------------------------
    // Moderation (estate/parcel)
    // -------------------------------------------------------------------------

    /** Kick avatar [id] off the grid (god / estate owner action). */
    fun kick(id: LLUUID): Unit = System.err.println("AvatarActions: kick not yet implemented")

    /**
     * Freeze or unfreeze avatar [id].
     *
     * @param freeze true to freeze, false to unfreeze.
     */
    fun freeze(id: LLUUID, freeze: Boolean): Unit = System.err.println("AvatarActions: freeze not yet implemented")

    /**
     * Eject avatar [id] from the parcel, optionally banning them.
     *
     * @param ban true to add to the parcel ban list after ejection.
     */
    fun eject(id: LLUUID, ban: Boolean): Unit = System.err.println("AvatarActions: eject not yet implemented")

    /** Freeze avatar [id] at the estate level. */
    fun freezeAvatar(id: LLUUID): Unit = System.err.println("AvatarActions: freezeAvatar not yet implemented")

    /** Eject avatar [id] at the estate level; optionally enable the ban action. */
    fun ejectAvatar(id: LLUUID, banEnabled: Boolean = false): Unit =
        System.err.println("AvatarActions: ejectAvatar not yet implemented")

    // -------------------------------------------------------------------------
    // Group
    // -------------------------------------------------------------------------

    /** Open the group-invitation dialog to invite avatar [id]. */
    fun inviteToGroup(id: LLUUID): Unit = System.err.println("AvatarActions: inviteToGroup not yet implemented")

    // -------------------------------------------------------------------------
    // Map / tracking
    // -------------------------------------------------------------------------

    /** Show avatar [id] on the world map. */
    fun showOnMap(id: LLUUID): Unit = System.err.println("AvatarActions: showOnMap not yet implemented")

    /** Begin tracking avatar [id] on the minimap / world map. */
    fun track(id: LLUUID): Unit = System.err.println("AvatarActions: track not yet implemented")

    /** Teleport the local agent to avatar [id]'s location. */
    fun teleportTo(id: LLUUID): Unit = System.err.println("AvatarActions: teleportTo not yet implemented")

    // -------------------------------------------------------------------------
    // Chat history
    // -------------------------------------------------------------------------

    /** Open the in-viewer chat history floater for avatar [id]. */
    fun viewChatHistory(id: LLUUID): Unit = System.err.println("AvatarActions: viewChatHistory not yet implemented")

    /** Open the chat history in an external viewer application. */
    fun viewChatHistoryExternally(id: LLUUID): Unit = System.err.println("AvatarActions: viewChatHistoryExternally not yet implemented")

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------

    /**
     * Build a comma-separated display-name string from a list of UUIDs.
     * Returns the assembled string (C++ used an out-parameter).
     */
    fun buildResidentsString(avatarIds: List<LLUUID>): String {
        System.err.println("AvatarActions: buildResidentsString not yet implemented")
        return ""
    }
}
