/**
 * FSRadarEntry.kt
 * Converted from fsradarentry.h / fsradarentry.cpp
 * Original copyright (c) 2013 Ansariel Hiller @ Second Life
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.LLVector3d

// ---------------------------------------------------------------------------
// Radar status flags — mirrors avatar property flags from the C++ side.
// ---------------------------------------------------------------------------
enum class RadarStatus(val flag: Int) {
    NONE(0),
    PAYMENT_INFO_ON_FILE(1 shl 0),
    PAYMENT_INFO_USED(1 shl 1),
    IDENTIFIED(1 shl 2);

    companion object {
        fun fromFlags(flags: Int): Set<RadarStatus> =
            values().filter { it != NONE && (flags and it.flag) != 0 }.toSet()
    }
}

// ---------------------------------------------------------------------------
// Name-format preference — mirrors ERadarNameFormat in fsradar.h.
// ---------------------------------------------------------------------------
enum class RadarNameFormat {
    DISPLAYNAME,
    USERNAME,
    DISPLAYNAME_USERNAME,
    USERNAME_DISPLAYNAME
}

// ---------------------------------------------------------------------------
// FSRadarEntry — per-avatar entry tracked by FSRadar.
// ---------------------------------------------------------------------------
class FSRadarEntry(val id: LLUUID) {

    var name: String = "<waiting>"
        private set
    var userName: String = ""
        private set
    var displayName: String = ""
        private set
    var notes: String = ""
        private set

    var range: Float = 0f
    var globalPos: LLVector3d = LLVector3d.ZERO
    var region: LLUUID = LLUUID.NULL

    val firstSeen: Long = System.currentTimeMillis() / 1000L   // epoch seconds

    /** Raw avatar-property flags (PAYMENT_INFO_ON_FILE etc.) */
    var statusFlags: Int = 0
        private set

    /** Age in days; -1 = not yet fetched; -2 = hidden by avatar. */
    var age: Int = -1
        private set

    var zOffset: Float = 0f
    var lastZOffsetTime: Long = System.currentTimeMillis() / 1000L

    var isLinden: Boolean = false
        private set
    var ignore: Boolean = false

    var alertAge: Boolean = false
        private set
    var ageAlertPerformed: Boolean = false
        private set

    private var propertiesRequested: Boolean = false

    // -----------------------------------------------------------------------
    // Initialisation — mirrors constructor body in fsradarentry.cpp
    // -----------------------------------------------------------------------
    init {
        requestProperties()
        updateName()
    }

    // -----------------------------------------------------------------------
    // Name resolution
    // -----------------------------------------------------------------------

    /** Kick off an async name-cache lookup (stubbed: real impl hooks LLAvatarNameCache). */
    fun updateName() {
        System.err.println("FSRadarEntry: updateName not yet implemented")
    }

    /** Called when the avatar-name cache resolves the name for [id]. */
    fun onAvatarNameCache(avId: LLUUID, avUserName: String, avDisplayName: String, isDisplayNameDefault: Boolean) {
        // Respect RLVa shownames restriction — stub for now
        val rlvHideNames = false // RLVa handler not yet ported
        if (!rlvHideNames) {
            userName = avUserName
            displayName = avDisplayName
            name = buildRadarName(avUserName, avDisplayName, isDisplayNameDefault)
            isLinden = checkIsLinden(avId)
        } else {
            val anonymName = "[hidden]" // RlvStrings.getAnonym(avName) — when RLVa is ported
            userName = anonymName
            displayName = anonymName
            name = anonymName
            isLinden = false
        }
    }

    /** Build the display name string according to the current RadarNameFormat setting. */
    fun buildRadarName(
        avUserName: String,
        avDisplayName: String,
        isDisplayNameDefault: Boolean,
        fmt: RadarNameFormat = RadarNameFormat.DISPLAYNAME,
        useDisplayNames: Boolean = true
    ): String {
        val rlvHideNames = false // RLVa handler not yet ported
        if (rlvHideNames) return "[hidden]"

        if (!useDisplayNames) return avUserName

        return when (fmt) {
            RadarNameFormat.DISPLAYNAME -> avDisplayName
            RadarNameFormat.USERNAME -> avUserName
            RadarNameFormat.DISPLAYNAME_USERNAME ->
                if (isDisplayNameDefault) avUserName
                else "$avDisplayName ($avUserName)"
            RadarNameFormat.USERNAME_DISPLAYNAME ->
                if (isDisplayNameDefault) avUserName
                else "$avUserName ($avDisplayName)"
        }
    }

    // -----------------------------------------------------------------------
    // Property fetching
    // -----------------------------------------------------------------------

    /** Request avatar properties (age, payment info, notes) from the server. */
    fun requestProperties() {
        if (!propertiesRequested && !id.isNull()) {
            System.err.println("FSRadarEntry: requestProperties not yet implemented")
            // propertiesRequested = true
        }
    }

    /**
     * Process a response from LLAvatarPropertiesProcessor.
     * [flags] — raw avatar-property flags; [bornOnEpoch] — seconds since epoch;
     * [hideAge] — server hides account age; [notesText] — personal notes.
     */
    fun processProperties(flags: Int, bornOnEpoch: Long, hideAge: Boolean, notesText: String?) {
        statusFlags = flags
        age = if (hideAge) -2
              else ((System.currentTimeMillis() / 1000L - bornOnEpoch) / 86400L).toInt()
        checkAge()
        notesText?.let { setNotes(it) }
    }

    // -----------------------------------------------------------------------
    // Age alert
    // -----------------------------------------------------------------------

    /** Recompute [alertAge] against the configured alert threshold. */
    fun checkAge(ageAlertThreshold: Int = 0) {
        alertAge = age > -1 && age <= ageAlertThreshold
        val rlvHideNames = false // RLVa handler not yet ported
        if (!alertAge || rlvHideNames) {
            ageAlertPerformed = true
        }
    }

    // -----------------------------------------------------------------------
    // Notes
    // -----------------------------------------------------------------------

    fun setNotes(text: String) {
        notes = text.trim()
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun checkIsLinden(avId: LLUUID): Boolean {
        System.err.println("FSRadarEntry: checkIsLinden not yet implemented")
        return false
    }

    override fun toString(): String =
        "FSRadarEntry(id=$id, name='$name', range=$range, age=$age)"
}
