/**
 * FSRadar.kt
 * Converted from fsradar.h / fsradar.cpp
 * Original copyright (c) 2013 Ansariel Hiller @ Second Life
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Constants — mirror the constexpr values in fsradar.h
// ---------------------------------------------------------------------------
const val FSRADAR_MAX_AVATARS_PER_ALERT: UInt = 6u
const val FSRADAR_COARSE_OFFSET_INTERVAL: Long = 7L       // seconds
const val FSRADAR_MAX_OFFSET_REQUESTS: UInt = 60u
const val FSRADAR_CHAT_MIN_SPACING: UInt = 6u             // seconds

// ---------------------------------------------------------------------------
// Observer interface — replaces boost::signals2 radar_update_callback_t
// ---------------------------------------------------------------------------
interface RadarUpdateObserver {
    /**
     * Called after every radar sweep with the current list of entries
     * and aggregate statistics.
     */
    fun onRadarUpdate(entries: List<FSRadarEntry>, stats: Map<String, Any>)
}

// ---------------------------------------------------------------------------
// FSRadar singleton — tracks nearby avatars, fires alerts, drives the update
// loop.  Mirrors class FSRadar : public LLSingleton<FSRadar>.
// ---------------------------------------------------------------------------
object FSRadar {

    // -----------------------------------------------------------------------
    // Internal state
    // -----------------------------------------------------------------------

    /** Live map of every avatar currently on radar, keyed by LLUUID. */
    private val entryList: MutableMap<LLUUID, FSRadarEntry> = mutableMapOf()

    /** Snapshot from the previous sweep — used for enter/leave detection. */
    private data class RadarFields(
        val lastDistance: Float,
        val lastRegion: LLUUID,
        val lastIgnore: Boolean
    )
    private val lastRadarSweep: MutableMap<LLUUID, RadarFields> = mutableMapOf()

    private val radarEnterAlerts: MutableList<LLUUID> = mutableListOf()
    private val radarLeaveAlerts: MutableList<LLUUID> = mutableListOf()
    private val radarOffsetRequests: MutableList<LLUUID> = mutableListOf()

    private var radarFrameCount: Int = 0
    private var radarAlertRequest: Boolean = false
    private var radarLastRequestTime: Float = 0f
    private var radarLastBulkOffsetRequestTime: Long = 0L

    private var trackedAvatarId: LLUUID = LLUUID.NULL
    private val avatarStats: MutableMap<String, Any> = mutableMapOf()

    // Update timer interval matches FS_RADAR_LIST_UPDATE_INTERVAL = 1.0 s
    private const val UPDATE_INTERVAL_MS: Long = 1_000L

    private val observers: MutableList<RadarUpdateObserver> = mutableListOf()

    // -----------------------------------------------------------------------
    // Observer registration — mirrors setUpdateCallback / boost::signals2
    // -----------------------------------------------------------------------

    fun addObserver(observer: RadarUpdateObserver) {
        observers += observer
    }

    fun removeObserver(observer: RadarUpdateObserver) {
        observers -= observer
    }

    private fun notifyObservers() {
        val snapshot = getRadarList()
        val stats = avatarStats.toMap()
        observers.forEach { it.onRadarUpdate(snapshot, stats) }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Returns a defensive copy of the current entry map. */
    fun getRadarList(): List<FSRadarEntry> = entryList.values.toList()

    /** Returns the entry for a specific avatar, or null if not on radar. */
    fun getEntry(avatarId: LLUUID): FSRadarEntry? = entryList[avatarId]

    /**
     * Begin tracking [avatarId] on the world map.
     * Mirrors FSRadar::startTracking().
     */
    fun startTracking(avatarId: LLUUID) {
        trackedAvatarId = avatarId
        TODO("Call LLTracker::trackAvatar(avatarId)")
    }

    /**
     * Zoom the camera to the avatar with [avatarId] and display name [name].
     * Mirrors FSRadar::zoomAvatar().
     */
    fun zoomAvatar(avatarId: LLUUID, name: String) {
        TODO("Focus camera on avatar position via LLAvatarActions::zoomIn()")
    }

    /**
     * Offer a teleport to the location of [targetAv].
     * Mirrors FSRadar::teleportToAvatar().
     */
    fun teleportToAvatar(targetAv: LLUUID) {
        TODO("Use LLAvatarActions::teleportTo() or direct TP request")
    }

    /**
     * Queue a channel-based alert sync with nearby radar-aware objects.
     * Mirrors FSRadar::requestRadarChannelAlertSync().
     */
    fun requestRadarChannelAlertSync() {
        radarAlertRequest = true
    }

    /**
     * Refresh display names for all entries (triggered when name-format
     * preference changes).  Mirrors FSRadar::updateNames().
     */
    fun updateNames() {
        entryList.values.forEach { it.updateName() }
    }

    /**
     * Refresh the display name for a single avatar.
     * Mirrors FSRadar::updateName(avatar_id).
     */
    fun updateName(avatarId: LLUUID) {
        entryList[avatarId]?.updateName()
    }

    /**
     * Persist a personal note for [avatarId].
     * Mirrors FSRadar::updateNotes().
     */
    fun updateNotes(avatarId: LLUUID, notes: String) {
        entryList[avatarId]?.setNotes(notes)
    }

    // -----------------------------------------------------------------------
    // Update loop — driven externally (e.g. a coroutine or timer) at ~1 Hz.
    // Mirrors FSRadarListUpdater::tick() → FSRadar::updateRadarList().
    // -----------------------------------------------------------------------

    /** Called once per update tick; refreshes [entryList] and fires alerts. */
    fun tick() {
        updateRadarList()
        updateTracking()
        notifyObservers()
    }

    // -----------------------------------------------------------------------
    // Internal update helpers
    // -----------------------------------------------------------------------

    /**
     * Core sweep: walk LLWorld avatar list, add/remove FSRadarEntry objects,
     * compute distances, and populate enter/leave alert queues.
     * Mirrors FSRadar::updateRadarList().
     */
    private fun updateRadarList() {
        TODO(
            "Query LLWorld for nearby avatars; update entryList; " +
            "populate radarEnterAlerts / radarLeaveAlerts; " +
            "call radarAlertMsg() for each alert UUID"
        )
    }

    /**
     * Keep the world-map tracking marker in sync with [trackedAvatarId].
     * Mirrors FSRadar::updateTracking() and FSRadar::checkTracking().
     */
    private fun updateTracking() {
        if (trackedAvatarId.isNull()) return
        TODO("Update LLTracker with latest globalPos from entryList[trackedAvatarId]")
    }

    /**
     * Send a radar-channel chat alert for an avatar entering or leaving range.
     * Mirrors FSRadar::radarAlertMsg().
     */
    private fun radarAlertMsg(agentId: LLUUID, displayName: String, postMsg: String) {
        TODO("Format and post chat notification via LLNotificationsUtil")
    }

    /**
     * Re-evaluate age alerts for all entries whenever the threshold setting
     * changes.  Mirrors FSRadar::updateAgeAlertCheck().
     */
    private fun updateAgeAlertCheck() {
        TODO("Read RadarAvatarAgeAlertValue from settings; call entry.checkAge() for each entry")
    }

    /**
     * Called when the agent crosses into a new region.
     * Mirrors FSRadar::onRegionChanged().
     */
    private fun onRegionChanged() {
        TODO("Re-subscribe to region capability signals; clear stale offset requests")
    }

    // -----------------------------------------------------------------------
    // Companion — static helpers (menu callbacks in C++)
    // -----------------------------------------------------------------------
    companion object {
        /** Toggle or set the radar name-format preference. */
        fun onRadarNameFmtClicked(userdata: String) {
            TODO("Update gSavedSettings RadarNameFormat and call FSRadar.updateNames()")
        }

        /** Return true if [userdata] matches the currently active name format. */
        fun radarNameFmtCheck(userdata: String): Boolean {
            TODO("Compare userdata against gSavedSettings RadarNameFormat")
        }

        /** Handle 'Report to…' menu item. */
        fun onRadarReportToClicked(userdata: String) {
            TODO("Open IM or abuse-report dialog for the selected avatar")
        }

        /** Return true if the 'Report to…' option identified by [userdata] is applicable. */
        fun radarReportToCheck(userdata: String): Boolean {
            TODO("Return whether the report target is valid in current context")
        }
    }
}
