/**
 * FSFloaterRadar.kt
 * Kotlin conversion of fsradar.h / fsradar.cpp (fsradarentry.h / fsradarentry.cpp)
 *
 * There is no dedicated fsfloaterradar.* in the Firestorm source; the radar
 * data is owned by FSRadar (LLSingleton) and consumed by several panels.  This
 * file provides a self-contained floater that wraps FSRadar, subscribes to its
 * update signal, and exposes the UI-facing surface area needed by any radar
 * panel or HUD element.
 *
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Top-level constants — mirror constexpr values in fsradar.h
// ---------------------------------------------------------------------------

const val RADAR_MAX_AVATARS_PER_ALERT: UInt  = 6u
const val RADAR_COARSE_OFFSET_INTERVAL: Long = 7L     // seconds
const val RADAR_MAX_OFFSET_REQUESTS: UInt    = 60u
const val RADAR_CHAT_MIN_SPACING: UInt       = 6u     // seconds

// ---------------------------------------------------------------------------
// Name-format preference — mirrors ERadarNameFormat in fsradar.h
// ---------------------------------------------------------------------------

enum class RadarNameFormat {
    DISPLAYNAME,
    USERNAME,
    DISPLAYNAME_USERNAME,
    USERNAME_DISPLAYNAME
}

// ---------------------------------------------------------------------------
// Payment-info classification — mirrors ERadarPaymentInfoFlag in fsradar.h
// ---------------------------------------------------------------------------

enum class RadarPaymentInfoFlag { NONE, FILLED, USED }

// ---------------------------------------------------------------------------
// FSFloaterRadar — the UI shell around the FSRadar singleton
// ---------------------------------------------------------------------------

/**
 * Radar floater: displays nearby avatars with distance, region/parcel membership,
 * voice power level, age, notes, and enter/leave alerts.
 *
 * Consumes [FSRadar] updates via [RadarUpdateObserver] and renders them into a
 * scrollable list.  Also provides static menu-callback helpers that were `static`
 * methods of [FSRadar] in C++.
 *
 * @param seed LLSD key supplied by the floater registry on construction.
 */
class FSFloaterRadar(val seed: LLSD) : RadarUpdateObserver {

    // ------------------------------------------------------------------
    // Child widget stubs — populated in [postBuild]
    // ------------------------------------------------------------------

    private var radarList: RadarListCtrl?   = null
    private var filterEditor: FilterEditor? = null
    private var avatarCountLabel: Label?    = null

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Wire up child widgets and register this floater as a radar observer.
     *
     * @return `true` on success.
     */
    fun postBuild(): Boolean {
        radarList       = getChild<RadarListCtrl>("radar_list")
        filterEditor    = getChild<FilterEditor>("filter_input")?.also { editor ->
            editor.setCommitCallback { text -> onFilterEdit(text) }
        }
        avatarCountLabel = getChild<Label>("avatar_count")

        FSRadar.addObserver(this)
        return radarList != null
    }

    /**
     * Called when the floater is being closed/destroyed.
     * Unregisters from [FSRadar] to prevent dangling callbacks.
     */
    fun onClose(appQuitting: Boolean) {
        FSRadar.removeObserver(this)
    }

    // ------------------------------------------------------------------
    // RadarUpdateObserver
    // ------------------------------------------------------------------

    /**
     * Receive a fresh radar sweep from [FSRadar.tick].
     *
     * [entries] is a snapshot of all currently tracked avatars;
     * [stats]   carries aggregate counts (total, region, chatrange).
     */
    override fun onRadarUpdate(entries: List<FSRadarEntry>, stats: Map<String, Any>) {
        refreshList(entries)
        avatarCountLabel?.setText(
            "Total: ${stats["total"]}  Region: ${stats["region"]}  Chat: ${stats["chatrange"]}"
        )
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Request a one-shot channel-alert sync so external radar-aware LSL objects
     * are told about all currently visible avatars.
     * Mirrors [FSRadar.requestRadarChannelAlertSync].
     */
    fun requestChannelAlertSync() {
        FSRadar.requestRadarChannelAlertSync()
    }

    /**
     * Begin tracking [avatarId] on the mini-map / world-map.
     * Mirrors [FSRadar.startTracking].
     */
    fun startTracking(avatarId: LLUUID) {
        FSRadar.startTracking(avatarId)
    }

    /**
     * Focus the camera on [avatarId] (if in draw distance).
     * Mirrors [FSRadar.zoomAvatar].
     */
    fun zoomAvatar(avatarId: LLUUID, name: String) {
        FSRadar.zoomAvatar(avatarId, name)
    }

    /**
     * Teleport the local agent to [avatarId]'s last known position.
     * Mirrors [FSRadar.teleportToAvatar].
     */
    fun teleportToAvatar(avatarId: LLUUID) {
        FSRadar.teleportToAvatar(avatarId)
    }

    /**
     * Persist a personal note for [avatarId].
     * Mirrors [FSRadar.updateNotes].
     */
    fun updateNotes(avatarId: LLUUID, notes: String) {
        FSRadar.updateNotes(avatarId, notes)
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Rebuild the radar scroll-list from [entries].
     * Each entry becomes one row with columns: name, range, flags, age, seen, region.
     */
    private fun refreshList(entries: List<FSRadarEntry>) {
        radarList?.clearRows()
        val currentFilter = filterEditor?.getText().orEmpty().lowercase()
        for (entry in entries) {
            if (currentFilter.isNotEmpty() && !entry.name.lowercase().contains(currentFilter)) continue

            val paymentFlag = when {
                entry.statusFlags and AVATAR_TRANSACTED != 0 -> RadarPaymentInfoFlag.USED
                entry.statusFlags and AVATAR_IDENTIFIED  != 0 -> RadarPaymentInfoFlag.FILLED
                else                                          -> RadarPaymentInfoFlag.NONE
            }

            radarList?.addRow(
                RadarRow(
                    id          = entry.id,
                    name        = entry.name,
                    range       = entry.range,
                    paymentFlag = paymentFlag,
                    age         = entry.age,
                    ignore      = entry.ignore,
                    alertAge    = entry.alertAge
                )
            )
        }
    }

    /** Called when the filter editor commits new text. */
    private fun onFilterEdit(text: String) {
        val entries = FSRadar.getRadarList()
        refreshList(entries)
    }

    // ------------------------------------------------------------------
    // Stubs for framework / platform calls
    // ------------------------------------------------------------------

    private fun <T> getChild(name: String): T? {
        System.err.println("FSFloaterRadar: getChild not yet implemented")
        return null
    }

    // ------------------------------------------------------------------
    // Companion — static menu callbacks (were static methods of FSRadar)
    // ------------------------------------------------------------------

    companion object {

        /**
         * Handle a "Radar Name Format" menu-item click.
         * Maps the menu [userdata] string to a [RadarNameFormat] and persists it.
         * Mirrors [FSRadar::onRadarNameFmtClicked].
         */
        fun onRadarNameFmtClicked(userdata: String) {
            val fmt = when (userdata) {
                "DN"   -> RadarNameFormat.DISPLAYNAME
                "UN"   -> RadarNameFormat.USERNAME
                "DNUN" -> RadarNameFormat.DISPLAYNAME_USERNAME
                "UNDN" -> RadarNameFormat.USERNAME_DISPLAYNAME
                else   -> return
            }
            System.err.println("FSFloaterRadar: onRadarNameFmtClicked not yet implemented")
        }

        /**
         * Return `true` if the menu item identified by [userdata] matches the
         * currently active name format.
         * Mirrors [FSRadar::radarNameFmtCheck].
         */
        fun radarNameFmtCheck(userdata: String): Boolean {
            System.err.println("FSFloaterRadar: radarNameFmtCheck not yet implemented")
            return false
        }

        /**
         * Handle a "Report to…" menu-item click.
         * Persists the FSMilkshakeRadarToasts setting.
         * Mirrors [FSRadar::onRadarReportToClicked].
         */
        fun onRadarReportToClicked(userdata: String) {
            val useToasts = when (userdata) {
                "radar_toasts"      -> true
                "radar_nearby_chat" -> false
                else                -> return
            }
            System.err.println("FSFloaterRadar: onRadarReportToClicked not yet implemented")
        }

        /**
         * Return `true` if the "Report to…" menu item identified by [userdata]
         * matches the current setting.
         * Mirrors [FSRadar::radarReportToCheck].
         */
        fun radarReportToCheck(userdata: String): Boolean {
            System.err.println("FSFloaterRadar: radarReportToCheck not yet implemented")
            return false
        }

        // Avatar-property flag masks — mirrors AVATAR_TRANSACTED / AVATAR_IDENTIFIED
        private const val AVATAR_TRANSACTED: Int = 0x08
        private const val AVATAR_IDENTIFIED: Int  = 0x04
    }

    // ------------------------------------------------------------------
    // Nested stub types used only inside this floater
    // ------------------------------------------------------------------

    /** One row of data presented in the radar scroll-list. */
    data class RadarRow(
        val id: LLUUID,
        val name: String,
        val range: Float,
        val paymentFlag: RadarPaymentInfoFlag,
        val age: Int,
        val ignore: Boolean,
        val alertAge: Boolean
    )

    /** Stub: the specialised radar scroll-list control (mirrors FSRadarListCtrl). */
    class RadarListCtrl {
        fun clearRows() { System.err.println("RadarListCtrl: clearRows not yet implemented") }
        fun addRow(row: RadarRow) { System.err.println("RadarListCtrl: addRow not yet implemented") }
    }

    /** Stub: filter editor widget. */
    class FilterEditor {
        fun getText(): String {
            System.err.println("FilterEditor: getText not yet implemented")
            return ""
        }
        fun setCommitCallback(cb: (String) -> Unit) { System.err.println("FilterEditor: setCommitCallback not yet implemented") }
    }

    /** Stub: text label widget. */
    class Label {
        fun setText(text: String) { System.err.println("Label: setText not yet implemented") }
    }
}
