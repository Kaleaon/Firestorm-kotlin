/**
 * ViewerParcelMedia.kt
 * Converted from llviewerparcelmedia.h / llviewerparcelmedia.cpp
 *
 * Handles multimedia playback on a per-parcel basis — understands land parcels,
 * network traffic, and LSL media transport commands.
 * Equivalent to LLViewerParcelMedia (LLSingleton) in the C++ viewer.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.*

/**
 * Rough equivalent of LLPluginClassMediaOwner::EMediaStatus.
 */
enum class MediaStatus {
    IDLE,
    LOADING,
    PLAYING,
    PAUSED,
    DONE,
    ERROR,
}

/**
 * Singleton that manages parcel-level media playback.
 *
 * Usage:
 * ```kotlin
 * ViewerParcelMedia.play()
 * ViewerParcelMedia.stop()
 * ```
 *
 * C++ lineage: LLViewerParcelMedia : LLViewerMediaObserver, LLSingleton
 */
object ViewerParcelMedia {

    // ── Current media state ───────────────────────────────────────────────

    /** URL of the media currently associated with this parcel. */
    var mediaUrl: String = ""
        private set

    /** MIME type reported by the server or detected from content. */
    var mediaType: String = ""
        private set

    /** True while media is actively playing (not paused, not stopped). */
    var isPlaying: Boolean = false
        private set

    private var status: MediaStatus = MediaStatus.IDLE

    // ── Parcel / region tracking ──────────────────────────────────────────

    var mediaParcelLocalId: Int = 0
    var mediaRegionId: LLUUID = LLUUID.nullUUID()

    // ── Last-action tracking (used to suppress duplicate requests) ────────

    var mediaLastUrl: String = ""
        private set
    var mediaLastActionPlay: Boolean = false
        private set

    var audioLastUrl: String = ""
        private set
    var audioLastActionPlay: Boolean = false
        private set

    // ── Queue / current state ─────────────────────────────────────────────

    var mediaQueueEmpty: Boolean = true
        private set
    var musicQueueEmpty: Boolean = true
        private set

    var mediaCommandQueue: UInt = 0u
    var mediaCommandTime: Float = 0f

    var mediaReFilter: Boolean = false
    var mediaFilterAlertActive: Boolean = false

    // ── Core playback controls ────────────────────────────────────────────

    /**
     * Begin (or resume) parcel media playback.
     * Mirrors LLViewerParcelMedia::play() and start().
     */
    fun play() {
        if (mediaUrl.isEmpty()) return
        isPlaying = true
        status = MediaStatus.PLAYING
        mediaLastUrl = mediaUrl
        mediaLastActionPlay = true
    }

    /**
     * Stop playback and reset the media stream.
     * Mirrors LLViewerParcelMedia::stop().
     */
    fun stop() {
        isPlaying = false
        status = MediaStatus.IDLE
        mediaLastActionPlay = false
    }

    /**
     * Pause playback without tearing down the media stream.
     * Mirrors LLViewerParcelMedia::pause().
     */
    fun pause() {
        if (isPlaying) {
            isPlaying = false
            status = MediaStatus.PAUSED
        }
    }

    /**
     * Transfer input focus to the parcel media surface.
     * Mirrors LLViewerParcelMedia::focus(bool).
     */
    fun focus(hasFocus: Boolean = true) {
        // Delegate to ViewerMediaFocus in a full implementation
    }

    /**
     * Seek to a timecode position (seconds).
     * Mirrors LLViewerParcelMedia::seek(F32).
     */
    fun seek(time: Float) {
        require(time >= 0f) { "Seek time must be non-negative" }
        // Forward to plugin layer
    }

    // ── Auto-play ─────────────────────────────────────────────────────────

    /**
     * Called by the auto-play subsystem to start media without explicit user
     * action.  Mirrors LLViewerParcelMediaAutoPlay integration.
     */
    fun autoPlay() {
        if (!isPlaying) play()
    }

    // ── Media update ──────────────────────────────────────────────────────

    /**
     * Called whenever the agent's parcel changes or the parcel's media URL
     * is updated.  Mirrors LLViewerParcelMedia::update(LLParcel*).
     *
     * @param url      New media URL from the parcel data.
     * @param mimeType MIME type hint; empty string means auto-detect.
     */
    fun updateMedia(url: String, mimeType: String = "") {
        val urlChanged = url != mediaUrl
        mediaUrl = url
        mediaType = mimeType
        mediaQueueEmpty = url.isEmpty()

        if (urlChanged && url.isEmpty()) {
            stop()
        }
    }

    // ── Domain filter helpers ─────────────────────────────────────────────

    /**
     * Extract the hostname from a URL for domain-filter matching.
     * Mirrors LLViewerParcelMedia::extractDomain().
     */
    fun extractDomain(url: String): String {
        return try {
            val noScheme = url.substringAfter("://")
            noScheme.substringBefore("/").substringBefore("?").substringBefore("#")
        } catch (_: Exception) {
            url
        }
    }

    /**
     * Trigger URL filtering before play begins.
     * Mirrors LLViewerParcelMedia::filterMediaUrl().
     */
    fun filterMediaUrl(url: String) {
        // Full implementation would consult mMediaFilterList (LLSD)
        play()
    }

    /**
     * Trigger audio-URL filtering before streaming starts.
     * Mirrors LLViewerParcelMedia::filterAudioUrl().
     */
    fun filterAudioUrl(url: String) {
        audioLastUrl = url
        audioLastActionPlay = true
        // Full implementation would consult domain filter list
    }

    // ── Network message handlers ──────────────────────────────────────────

    /**
     * Handle a ParcelMediaCommandMessage from the network.
     * The [command] bitmask mirrors PARCEL_MEDIA_COMMAND_* constants.
     */
    fun handleParcelMediaCommand(command: UInt, time: Float = 0f) {
        mediaCommandQueue = command
        mediaCommandTime = time
        when {
            command and 0x1u != 0u -> play()
            command and 0x2u != 0u -> stop()
            command and 0x4u != 0u -> pause()
            command and 0x8u != 0u -> seek(time)
        }
    }

    // ── Status / metadata accessors ───────────────────────────────────────

    fun getStatus(): MediaStatus = status
    fun getMimeType(): String = mediaType
    fun getUrl(): String = mediaUrl
    fun hasParcelMedia(): Boolean = mediaUrl.isNotEmpty()

    override fun toString(): String =
        "ViewerParcelMedia(url='$mediaUrl', type='$mediaType', playing=$isPlaying, status=$status)"
}
