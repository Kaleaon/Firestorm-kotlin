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
    fun profileData(id: LLUUID): Unit = TODO("show profile for $id")

    /** Show the profile floater for [avatarId]. */
    fun showProfile(avatarId: LLUUID): Unit = TODO("showProfile $avatarId")

    /** Hide the profile floater for [avatarId]. */
    fun hideProfile(avatarId: LLUUID): Unit = TODO("hideProfile $avatarId")

    /** @return true if the profile floater for [avatarId] is currently open. */
    fun profileVisible(avatarId: LLUUID): Boolean = TODO("profileVisible $avatarId")

    // -------------------------------------------------------------------------
    // Friendship
    // -------------------------------------------------------------------------

    /** Show the friendship-offer dialog for the avatar identified by [id]. */
    fun requestFriendshipDialog(id: LLUUID): Unit = TODO("requestFriendshipDialog $id")

    /** Show the friendship-offer dialog for the avatar identified by [id] and [name]. */
    fun requestFriendshipDialog(id: LLUUID, name: String): Unit =
        TODO("requestFriendshipDialog $id ($name)")

    /** Show the friend-removal confirmation dialog for [id]. */
    fun removeFriendDialog(id: LLUUID): Unit = TODO("removeFriendDialog $id")

    /** Show the friend-removal confirmation dialog for multiple [ids]. */
    fun removeFriendsDialog(ids: List<LLUUID>): Unit = TODO("removeFriendsDialog $ids")

    /** @return true if the avatar [id] is on the local agent's friend list. */
    fun isFriend(id: LLUUID): Boolean = TODO("isFriend $id")

    // -------------------------------------------------------------------------
    // Instant messaging
    // -------------------------------------------------------------------------

    /** Open (or focus) an IM session with avatar [id]. */
    fun startIM(id: LLUUID): Unit = TODO("startIM $id")

    /** End an existing IM session with avatar [id]. */
    fun endIM(id: LLUUID): Unit = TODO("endIM $id")

    // -------------------------------------------------------------------------
    // Voice
    // -------------------------------------------------------------------------

    /** Begin a P2P voice call with avatar [id]. */
    fun startCall(id: LLUUID): Unit = TODO("startCall $id")

    /** @return true if the viewer can currently initiate voice calls. */
    fun canCall(): Boolean = TODO("canCall")

    // -------------------------------------------------------------------------
    // Teleport
    // -------------------------------------------------------------------------

    /** Send a teleport offer to avatar [id]. */
    fun offerTeleport(id: LLUUID): Unit = TODO("offerTeleport $id")

    /** Send a teleport offer to multiple avatars [ids]. */
    fun offerTeleport(ids: List<LLUUID>): Unit = TODO("offerTeleport $ids")

    /**
     * Request a teleport from avatar [id].
     * Mapped from C++ `teleportRequest()`; renamed for clarity.
     */
    fun requestTeleport(id: LLUUID): Unit = TODO("requestTeleport $id")

    /**
     * @return false if [id] is a friend who is not currently visible online
     *         (offline friends cannot receive teleport offers).
     */
    fun canOfferTeleport(id: LLUUID): Boolean = TODO("canOfferTeleport $id")

    /**
     * @return true if a teleport request can be sent to avatar [id].
     */
    fun canRequestTeleport(id: LLUUID): Boolean = TODO("canRequestTeleport $id")

    // -------------------------------------------------------------------------
    // Economy
    // -------------------------------------------------------------------------

    /** Open the L$ payment dialog targeted at avatar [id]. */
    fun pay(id: LLUUID): Unit = TODO("pay $id")

    // -------------------------------------------------------------------------
    // Mute / block
    // -------------------------------------------------------------------------

    /**
     * Add avatar [id] (display name [name]) to the mute list.
     * Corresponds to the viewer's `toggleMute` with a mute-add intent.
     */
    fun mute(id: LLUUID, name: String): Unit = TODO("mute $id ($name)")

    /** Remove avatar [id] from the mute list. */
    fun unmute(id: LLUUID): Unit = TODO("unmute $id")

    /** @return true if avatar [id] is on the local mute list. */
    fun isMuted(id: LLUUID): Boolean = TODO("isMuted $id")

    /** Toggle the text/object block for avatar [id]; returns the new blocked state. */
    fun toggleBlock(id: LLUUID): Boolean = TODO("toggleBlock $id")

    /** @return true if avatar [id] is currently blocked. */
    fun isBlocked(id: LLUUID): Boolean = TODO("isBlocked $id")

    /** Toggle voice mute for avatar [id]. */
    fun toggleMuteVoice(id: LLUUID): Unit = TODO("toggleMuteVoice $id")

    /** @return true if avatar [id]'s voice is muted. */
    fun isVoiceMuted(id: LLUUID): Boolean = TODO("isVoiceMuted $id")

    // -------------------------------------------------------------------------
    // Moderation (estate/parcel)
    // -------------------------------------------------------------------------

    /** Kick avatar [id] off the grid (god / estate owner action). */
    fun kick(id: LLUUID): Unit = TODO("kick $id")

    /**
     * Freeze or unfreeze avatar [id].
     *
     * @param freeze true to freeze, false to unfreeze.
     */
    fun freeze(id: LLUUID, freeze: Boolean): Unit = TODO("freeze $id freeze=$freeze")

    /**
     * Eject avatar [id] from the parcel, optionally banning them.
     *
     * @param ban true to add to the parcel ban list after ejection.
     */
    fun eject(id: LLUUID, ban: Boolean): Unit = TODO("eject $id ban=$ban")

    /** Freeze avatar [id] at the estate level. */
    fun freezeAvatar(id: LLUUID): Unit = TODO("freezeAvatar $id")

    /** Eject avatar [id] at the estate level; optionally enable the ban action. */
    fun ejectAvatar(id: LLUUID, banEnabled: Boolean = false): Unit =
        TODO("ejectAvatar $id banEnabled=$banEnabled")

    // -------------------------------------------------------------------------
    // Group
    // -------------------------------------------------------------------------

    /** Open the group-invitation dialog to invite avatar [id]. */
    fun inviteToGroup(id: LLUUID): Unit = TODO("inviteToGroup $id")

    // -------------------------------------------------------------------------
    // Map / tracking
    // -------------------------------------------------------------------------

    /** Show avatar [id] on the world map. */
    fun showOnMap(id: LLUUID): Unit = TODO("showOnMap $id")

    /** Begin tracking avatar [id] on the minimap / world map. */
    fun track(id: LLUUID): Unit = TODO("track $id")

    /** Teleport the local agent to avatar [id]'s location. */
    fun teleportTo(id: LLUUID): Unit = TODO("teleportTo $id")

    // -------------------------------------------------------------------------
    // Chat history
    // -------------------------------------------------------------------------

    /** Open the in-viewer chat history floater for avatar [id]. */
    fun viewChatHistory(id: LLUUID): Unit = TODO("viewChatHistory $id")

    /** Open the chat history in an external viewer application. */
    fun viewChatHistoryExternally(id: LLUUID): Unit = TODO("viewChatHistoryExternally $id")

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------

    /**
     * Build a comma-separated display-name string from a list of UUIDs.
     * Returns the assembled string (C++ used an out-parameter).
     */
    fun buildResidentsString(avatarIds: List<LLUUID>): String =
        TODO("buildResidentsString $avatarIds")
}
