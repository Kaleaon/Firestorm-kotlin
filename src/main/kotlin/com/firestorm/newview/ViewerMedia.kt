// ViewerMedia.kt — converted from llviewermedia.h / llviewermedia.cpp
// Copyright (C) 2007, Linden Research, Inc. LGPL 2.1
package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Observer interface
// ---------------------------------------------------------------------------

interface MediaObserver {
    /** Called whenever the focused media implementation changes. */
    fun onFocusedMediaChanged()

    /** Called whenever the hovered (mouse-over) media implementation changes. */
    fun onHoveredMediaChanged() {}
}

// ---------------------------------------------------------------------------
// MediaImpl — thin representation of a single media instance
// ---------------------------------------------------------------------------

/**
 * Represents one in-world media instance (maps to LLViewerMediaImpl).
 * Heavy plugin/GL operations are stubbed with TODO.
 */
class MediaImpl(
    val textureId: LLUUID,
    val mediaWidth: Int = 0,
    val mediaHeight: Int = 0,
    val autoScale: Boolean = false,
    val loop: Boolean = false
) {
    var mediaUrl: String = ""
    var homeUrl: String = ""
    var mimeType: String = ""
    var visible: Boolean = true
    var isParcelMedia: Boolean = false
    var isDisabled: Boolean = false
    var isTrustedBrowser: Boolean = false
    var volume: Float = 1.0f
    var interest: Double = 0.0
    var proximity: Int = Int.MAX_VALUE
    var proximityDistance: Double = Double.MAX_VALUE

    enum class NavState {
        NONE,
        BEGUN,
        FIRST_LOCATION_CHANGED,
        FIRST_LOCATION_CHANGED_SPURIOUS,
        COMPLETE_BEFORE_LOCATION_CHANGED,
        COMPLETE_BEFORE_LOCATION_CHANGED_SPURIOUS,
        SERVER_SENT,
        SERVER_BEGUN,
        SERVER_FIRST_LOCATION_CHANGED,
        SERVER_COMPLETE_BEFORE_LOCATION_CHANGED,
    }

    var navState: NavState = NavState.NONE

    fun play()  { TODO("GPU: play media plugin") }
    fun stop()  { TODO("GPU: stop media plugin") }
    fun pause() { TODO("GPU: pause media plugin") }

    fun navigateTo(url: String, mimeType: String = "", rediscoverType: Boolean = false) {
        this.mediaUrl = url
        TODO("GPU: navigate media plugin to url")
    }

    fun setVolume(v: Float) { volume = v; TODO("GPU: update plugin volume") }
    fun setMute(mute: Boolean) { TODO("GPU: mute plugin") }

    /** Update the backing GL texture from the plugin's pixel buffer. */
    fun updateTexture() { TODO("GPU: update media texture") }

    fun focus(hasFocus: Boolean) { TODO("GPU: relay focus to plugin") }
}

// ---------------------------------------------------------------------------
// ViewerMedia singleton
// ---------------------------------------------------------------------------

/**
 * Central manager for all in-world media instances.
 * Maps to the C++ LLViewerMedia singleton.
 */
object ViewerMedia {

    // ------------------------------------------------------------------
    // Settings keys (mirror C++ static const char* members)
    // ------------------------------------------------------------------
    const val AUTO_PLAY_MEDIA_SETTING          = "ParcelMediaAutoPlayEnable"
    const val SHOW_MEDIA_ON_OTHERS_SETTING     = "MediaShowOnOthers"
    const val SHOW_MEDIA_WITHIN_PARCEL_SETTING = "MediaShowWithinParcel"
    const val SHOW_MEDIA_OUTSIDE_PARCEL_SETTING= "MediaShowOutsideParcel"

    // ------------------------------------------------------------------
    // Internal state
    // ------------------------------------------------------------------
    private val impls: MutableList<MediaImpl> = mutableListOf()
    private val implsByTexture: MutableMap<LLUUID, MediaImpl> = mutableMapOf()
    private val observers: MutableList<MediaObserver> = mutableListOf()

    private var anyMediaShowing: Boolean = false
    private var anyMediaPlaying: Boolean = false
    private var globalVolume: Float = 1.0f
    private var maxInstances: Int = 8

    /** The impl that currently has keyboard / interaction focus. */
    var focusedMediaImpl: MediaImpl? = null
        private set(value) {
            field = value
            observers.forEach { it.onFocusedMediaChanged() }
        }

    /** The impl the mouse is currently hovering over. */
    var hoveredMediaImpl: MediaImpl? = null
        private set(value) {
            field = value
            observers.forEach { it.onHoveredMediaChanged() }
        }

    // ------------------------------------------------------------------
    // Observer management
    // ------------------------------------------------------------------

    fun addObserver(observer: MediaObserver): Boolean {
        if (observers.contains(observer)) return false
        observers.add(observer)
        return true
    }

    fun removeObserver(observer: MediaObserver): Boolean = observers.remove(observer)

    // ------------------------------------------------------------------
    // Impl lifecycle
    // ------------------------------------------------------------------

    fun newMediaImpl(
        textureId: LLUUID,
        mediaWidth: Int = 0,
        mediaHeight: Int = 0,
        mediaAutoScale: Boolean = false,
        mediaLoop: Boolean = false
    ): MediaImpl {
        val impl = MediaImpl(textureId, mediaWidth, mediaHeight, mediaAutoScale, mediaLoop)
        impls.add(impl)
        implsByTexture[textureId] = impl
        return impl
    }

    fun getMediaImplFromTextureID(textureId: LLUUID): MediaImpl? = implsByTexture[textureId]

    fun textureHasMedia(textureId: LLUUID): Boolean = implsByTexture.containsKey(textureId)

    /** Returns the priority-sorted list of all media impls (highest interest first). */
    fun getPriorityList(): MutableList<MediaImpl> {
        impls.sortWith(Comparator { a, b -> compareValuesBy(b, a) { it.interest } })
        return impls
    }

    // ------------------------------------------------------------------
    // Focus / hover
    // ------------------------------------------------------------------

    /**
     * Set which impl has in-world media focus.
     * Pass null to clear focus.
     */
    fun setInWorldMediaFocus(impl: MediaImpl?) {
        focusedMediaImpl?.focus(false)
        focusedMediaImpl = impl
        impl?.focus(true)
    }

    fun setHoveredMedia(impl: MediaImpl?) {
        hoveredMediaImpl = impl
    }

    // ------------------------------------------------------------------
    // Global controls
    // ------------------------------------------------------------------

    fun getVolume(): Float = globalVolume

    fun setVolume(volume: Float) {
        globalVolume = volume
        impls.forEach { it.setVolume(volume) }
    }

    fun isAnyMediaShowing(): Boolean = anyMediaShowing
    fun isAnyMediaPlaying(): Boolean = anyMediaPlaying

    fun setAllMediaEnabled(enabled: Boolean) {
        impls.forEach { it.isDisabled = !enabled }
    }

    fun setAllMediaPaused(paused: Boolean) {
        if (paused) impls.forEach { it.pause() } else impls.forEach { it.play() }
    }

    // ------------------------------------------------------------------
    // Per-frame update (called from the main idle loop)
    // ------------------------------------------------------------------

    /** Main per-frame update tick — mirrors LLViewerMedia::updateMedia(). */
    fun updateMedia(idle: Boolean = false) {
        TODO("Prioritise impl list, drive plugin updates, manage texture uploads")
    }

    // ------------------------------------------------------------------
    // Cookie / cache / proxy helpers (stubbed)
    // ------------------------------------------------------------------

    fun clearAllCookies() { TODO("Clear cookies in all loaded plugins") }
    fun clearAllCaches()  { TODO("Clear caches in all loaded plugins") }
    fun setCookiesEnabled(enabled: Boolean) { TODO("Propagate cookie flag to all plugins") }

    fun setProxyConfig(enable: Boolean, host: String, port: Int) {
        TODO("Propagate proxy settings to all plugins")
    }

    // ------------------------------------------------------------------
    // Parcel audio / media helpers
    // ------------------------------------------------------------------

    fun hasInWorldMedia(): Boolean = impls.any { !it.isParcelMedia }
    fun hasParcelMedia(): Boolean  = impls.any { it.isParcelMedia }
    fun getParcelAudioURL(): String { TODO("Return URL of current parcel audio stream") }
    fun hasParcelAudio(): Boolean   { TODO("Return whether parcel audio URL is set") }
    fun isParcelMediaPlaying(): Boolean { TODO("Check parcel media play state") }
    fun isParcelAudioPlaying(): Boolean { TODO("Check parcel audio play state") }

    fun muteListChanged() { TODO("Re-evaluate mute state for all impls") }

    fun getCurrentUserAgent(): String { TODO("Return browser user-agent string") }
}
