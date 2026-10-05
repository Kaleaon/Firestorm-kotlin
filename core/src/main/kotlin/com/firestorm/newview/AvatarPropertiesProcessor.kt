/**
 * AvatarPropertiesProcessor.kt
 * Requests and caches avatar profile data — Kotlin port of
 * llavatarpropertiesprocessor.h / llavatarpropertiesprocessor.cpp
 *
 * Original: Copyright (C) 2001-2010 Linden Research, Inc.
 * Ported to Kotlin for the Firestorm viewer project.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Avatar flags (mirrors C++ const U32 values in llavatarpropertiesprocessor.h)
// ---------------------------------------------------------------------------

/** Avatar profile/properties flags, bit-packed into [AvatarData.flags]. */
object AvatarFlags {
    /** Profile is externally visible. */
    val ALLOW_PUBLISH: UInt = 0x1u shl 0
    /** Profile is marked mature. */
    val MATURE_PUBLISH: UInt = 0x1u shl 1
    /** Avatar has provided payment info. */
    val IDENTIFIED: UInt = 0x1u shl 2
    /** Avatar has actively used payment info. */
    val TRANSACTED: UInt = 0x1u shl 3
    /** Avatar is currently online. */
    val ONLINE: UInt = 0x1u shl 4
    /** Avatar has passed age verification. */
    val AGE_VERIFIED: UInt = 0x1u shl 5
    /** Online/offline status is not yet known (FS: FIRE-32184). */
    val ONLINE_UNDEFINED: UInt = 0x1u shl 31
}

// ---------------------------------------------------------------------------
// Processor type enum (EAvatarProcessorType)
// ---------------------------------------------------------------------------

/**
 * Identifies which category of avatar data a request/reply belongs to.
 * Mirrors the C++ [EAvatarProcessorType] enum.
 */
enum class AvatarProcessorType {
    /** Properties via legacy UDP (truncates some fields). */
    PROPERTIES_LEGACY,
    /** Properties via HTTP AgentProfile cap. */
    PROPERTIES,
    NOTES,
    GROUPS,
    PICKS,
    PICK_INFO,
    TEXTURES,
    CLASSIFIEDS,
    CLASSIFIED_INFO
}

// ---------------------------------------------------------------------------
// Data structures
// ---------------------------------------------------------------------------

/**
 * Group membership record embedded in [AvatarData].
 * Maps to [LLAvatarData::LLGroupData] / [LLAvatarGroups::LLGroupData].
 */
data class AvatarGroupData(
    val groupId: LLUUID,
    val groupName: String,
    val groupTitle: String = "",
    val groupInsigniaId: LLUUID = LLUUID.NULL,
    val groupPowers: ULong = 0uL,
    val acceptNotices: Boolean = false
)

/**
 * Full avatar profile data, combining the fields from both the C++
 * [LLAvatarData] and [LLAvatarLegacyData] structs.
 *
 * @property id           Avatar (target) UUID.
 * @property partnerId    UUID of the avatar's listed partner.
 * @property imageId      Profile picture asset UUID.
 * @property flImageId    First-life profile picture asset UUID.
 * @property aboutText    Profile "about" text (SL grid).
 * @property flAboutText  First-life "about" text.
 * @property profileUrl   Web profile URL.
 * @property bornOn       Account creation date as ISO-8601 string.
 * @property flags        Bit-field; see [AvatarFlags].
 * @property customerType Account type string (e.g. "Resident").
 * @property captionIndex Caption index for the account tier display.
 * @property captionText  Caption text for the account tier display.
 * @property hideAge      Whether the avatar's age is hidden (OpenSim).
 * @property notes        Personal notes about this avatar.
 * @property groups       Group memberships.
 * @property picks         (id, name) pairs for profile picks.
 */
data class AvatarData(
    val id: LLUUID,
    val partnerId: LLUUID = LLUUID.NULL,
    val imageId: LLUUID = LLUUID.NULL,
    val flImageId: LLUUID = LLUUID.NULL,
    val aboutText: String = "",
    val flAboutText: String = "",
    val profileUrl: String = "",
    val bornOn: String = "",
    val flags: UInt = 0u,
    val customerType: String = "",
    val captionIndex: UByte = 0u,
    val captionText: String = "",
    val hideAge: Boolean = false,
    val notes: String = "",
    val groups: MutableList<AvatarGroupData> = mutableListOf(),
    val picks: MutableList<Pair<LLUUID, String>> = mutableListOf()
)

/**
 * Callback interface for components that want to receive avatar property data.
 * Mirrors the C++ [LLAvatarPropertiesObserver] pure-virtual interface.
 */
interface AvatarPropertiesObserver {
    fun processProperties(data: Any?, type: AvatarProcessorType)
}

// ---------------------------------------------------------------------------
// AvatarPropertiesProcessor singleton
// ---------------------------------------------------------------------------

/**
 * Central registry for requesting and caching avatar profile data.
 *
 * Observers register with a target avatar UUID; they are notified whenever
 * new data arrives for that avatar.  Pending requests are tracked to suppress
 * duplicate network traffic while a reply is in flight.
 *
 * Network calls are stubbed with [TODO] — wire to the AgentProfile HTTP cap
 * or the legacy UDP messaging layer before use.
 */
object AvatarPropertiesProcessor {

    // -----------------------------------------------------------------------
    // Internal state
    // -----------------------------------------------------------------------

    /** avatarId → set of registered observers. */
    private val observers: MutableMap<LLUUID, MutableList<AvatarPropertiesObserver>> =
        mutableMapOf()

    /** avatarId → cached data (populated on first reply). */
    private val avatarCache: MutableMap<LLUUID, AvatarData> = mutableMapOf()

    /**
     * Tracks pending requests as (avatarId, type) → timestamp (ms since
     * epoch).  Mirrors the C++ [mRequestTimestamps] map.
     */
    private val pendingRequests: MutableMap<Pair<LLUUID, AvatarProcessorType>, Long> =
        mutableMapOf()

    /** How long (ms) before a pending request is considered stale. */
    private const val REQUEST_TIMEOUT_MS = 30_000L

    /** Whether the connected server supports the hide-age feature (FS). */
    var isHideAgeSupportedByServer: Boolean = false

    // -----------------------------------------------------------------------
    // Observer management
    // -----------------------------------------------------------------------

    /** Registers [observer] to receive updates for [avatarId]. */
    fun addObserver(avatarId: LLUUID, observer: AvatarPropertiesObserver) {
        observers.getOrPut(avatarId) { mutableListOf() }.let { list ->
            if (observer !in list) list += observer
        }
    }

    /** Unregisters [observer] from updates for [avatarId]. */
    fun removeObserver(avatarId: LLUUID, observer: AvatarPropertiesObserver) {
        observers[avatarId]?.remove(observer)
    }

    /** Dispatches [data] of [type] to every observer registered for [avatarId]. */
    private fun notifyObservers(avatarId: LLUUID, data: Any?, type: AvatarProcessorType) {
        observers[avatarId]?.toList()?.forEach { it.processProperties(data, type) }
    }

    // -----------------------------------------------------------------------
    // Pending-request tracking
    // -----------------------------------------------------------------------

    private fun isPendingRequest(avatarId: LLUUID, type: AvatarProcessorType): Boolean {
        val ts = pendingRequests[avatarId to type] ?: return false
        return (System.currentTimeMillis() - ts) < REQUEST_TIMEOUT_MS
    }

    private fun addPendingRequest(avatarId: LLUUID, type: AvatarProcessorType) {
        pendingRequests[avatarId to type] = System.currentTimeMillis()
    }

    private fun removePendingRequest(avatarId: LLUUID, type: AvatarProcessorType) {
        pendingRequests.remove(avatarId to type)
    }

    // -----------------------------------------------------------------------
    // Request API
    // -----------------------------------------------------------------------

    /**
     * Requests avatar properties via the HTTP AgentProfile capability.
     * Duplicate in-flight requests are suppressed.
     * Stub — wire to coroutine/HTTP layer.
     */
    fun requestAvatarProperties(id: LLUUID) {
        if (isPendingRequest(id, AvatarProcessorType.PROPERTIES)) return
        addPendingRequest(id, AvatarProcessorType.PROPERTIES)
        System.err.println("AvatarPropertiesProcessor: requestAvatarProperties not yet implemented")
    }

    /**
     * Requests legacy UDP avatar properties (truncates some text fields).
     * Stub — send AvatarPropertiesRequest UDP message.
     */
    fun sendAvatarLegacyPropertiesRequest(id: LLUUID) {
        if (isPendingRequest(id, AvatarProcessorType.PROPERTIES_LEGACY)) return
        addPendingRequest(id, AvatarProcessorType.PROPERTIES_LEGACY)
        System.err.println("AvatarPropertiesProcessor: sendAvatarLegacyPropertiesRequest not yet implemented")
    }

    /**
     * Requests avatar texture UUIDs.
     * Stub — send AvatarTexturesRequest UDP/HTTP message.
     */
    fun sendAvatarTexturesRequest(id: LLUUID) {
        if (isPendingRequest(id, AvatarProcessorType.TEXTURES)) return
        addPendingRequest(id, AvatarProcessorType.TEXTURES)
        System.err.println("AvatarPropertiesProcessor: sendAvatarTexturesRequest not yet implemented")
    }

    /**
     * Requests the list of classifieds for [id].
     * Stub — send AvatarClassifiedsRequest UDP message.
     */
    fun sendAvatarClassifiedsRequest(id: LLUUID) {
        if (isPendingRequest(id, AvatarProcessorType.CLASSIFIEDS)) return
        addPendingRequest(id, AvatarProcessorType.CLASSIFIEDS)
        System.err.println("AvatarPropertiesProcessor: sendAvatarClassifiedsRequest not yet implemented")
    }

    /**
     * Requests picks list for [id] (OpenSim / FS extension).
     */
    fun sendAvatarPicksRequest(id: LLUUID) {
        if (isPendingRequest(id, AvatarProcessorType.PICKS)) return
        addPendingRequest(id, AvatarProcessorType.PICKS)
        System.err.println("AvatarPropertiesProcessor: sendAvatarPicksRequest not yet implemented")
    }

    /**
     * Requests personal notes for [id] (OpenSim / FS extension).
     */
    fun sendAvatarNotesRequest(id: LLUUID) {
        if (isPendingRequest(id, AvatarProcessorType.NOTES)) return
        addPendingRequest(id, AvatarProcessorType.NOTES)
        System.err.println("AvatarPropertiesProcessor: sendAvatarNotesRequest not yet implemented")
    }

    /**
     * Requests group membership list for [id] (OpenSim / FS extension).
     */
    fun sendAvatarGroupsRequest(id: LLUUID) {
        if (isPendingRequest(id, AvatarProcessorType.GROUPS)) return
        addPendingRequest(id, AvatarProcessorType.GROUPS)
        System.err.println("AvatarPropertiesProcessor: sendAvatarGroupsRequest not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Update / write API
    // -----------------------------------------------------------------------

    /**
     * Sends updated profile data to the server for the current user.
     * Stub — POST to AgentProfile cap or send AvatarPropertiesUpdate UDP.
     */
    fun sendAvatarPropertiesUpdate(data: AvatarData) {
        System.err.println("AvatarPropertiesProcessor: sendAvatarPropertiesUpdate not yet implemented")
    }

    /**
     * Sends a personal note update for [avatarId].
     */
    fun sendNotes(avatarId: LLUUID, notes: String) {
        System.err.println("AvatarPropertiesProcessor: sendNotes not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Cache access
    // -----------------------------------------------------------------------

    /**
     * Stores [data] in the local cache and notifies observers.
     * Called internally when a properties reply is received.
     */
    fun cacheAvatarData(data: AvatarData) {
        avatarCache[data.id] = data
        removePendingRequest(data.id, AvatarProcessorType.PROPERTIES)
        notifyObservers(data.id, data, AvatarProcessorType.PROPERTIES)
    }

    /**
     * Returns the cached [AvatarData] for [id], or null if not yet fetched.
     */
    fun getCachedAvatarData(id: LLUUID): AvatarData? = avatarCache[id]

    // -----------------------------------------------------------------------
    // Helper: human-readable account / payment strings
    // -----------------------------------------------------------------------

    companion object {

        /**
         * Returns a translated account-type label for [data], e.g. "Resident"
         * or "Linden Employee".  Mirrors the C++ static [accountType].
         */
        fun accountType(data: AvatarData): String {
            // wire to LLTrans / localisation layer when i18n is ported
            return when {
                data.flags and AvatarFlags.IDENTIFIED != 0u -> "Identified Resident"
                else -> "Resident"
            }
        }

        /**
         * Returns a human-readable payment info string for [data].
         * Mirrors the C++ static [paymentInfo].
         */
        fun paymentInfo(data: AvatarData): String {
            return when {
                hasPaymentInfoOnFile(data) && data.flags and AvatarFlags.TRANSACTED != 0u ->
                    "Payment Info Used"
                hasPaymentInfoOnFile(data) ->
                    "Payment Info on File"
                else ->
                    "No Payment Info on File"
            }
        }

        /** Returns true if the avatar has payment info on file. */
        fun hasPaymentInfoOnFile(data: AvatarData): Boolean =
            data.flags and AvatarFlags.IDENTIFIED != 0u
    }
}
