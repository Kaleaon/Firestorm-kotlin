package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

interface MediaObserver {
    fun onFocusedMediaChanged()
    fun onHoveredMediaChanged() {}
}

class MediaImpl(
    val textureId: LLUUID,
    var mediaWidth: Int = 0,
    var mediaHeight: Int = 0,
    var autoScale: Boolean = false,
    var loop: Boolean = false,
) {
    var mediaUrl: String = ""
    var homeUrl: String = ""
    var homeMimeType: String = ""
    var mimeType: String = ""
    var mediaEntryUrl: String = ""
    var visible: Boolean = true
    var isParcelMedia: Boolean = false
    var isDisabled: Boolean = false
    var isTrustedBrowser: Boolean = false
    var autoPlay: Boolean = false
    var navigateServerRequest: Boolean = false
    var volume: Float = 1.0f
    var interest: Double = 0.0
    var proximity: Int = Int.MAX_VALUE
    var proximityDistance: Double = Double.MAX_VALUE
    var usedInUI: Boolean = false
    var target: String = ""

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

    fun hasMedia(): Boolean = TODO("GPU: return mMediaSource != null && mMediaSource.isRunning")

    fun play()  { TODO("GPU: play media plugin") }
    fun stop()  { TODO("GPU: stop media plugin") }
    fun pause() { TODO("GPU: pause media plugin") }
    fun start() { TODO("GPU: start / unpause media plugin") }
    fun unload() { TODO("GPU: destroy media plugin; reset state") }

    fun navigateTo(url: String, mimeType: String = "", rediscoverType: Boolean = false, serverRequest: Boolean = false) {
        this.mediaUrl = url
        this.navigateServerRequest = serverRequest
        TODO("GPU: navigate media plugin to url; track nav state")
    }

    fun setVolume(v: Float) { volume = v; TODO("GPU: update plugin volume") }
    fun setMute(mute: Boolean) { TODO("GPU: mute plugin") }
    fun updateVolume() { TODO("GPU: recalculate effective volume from global + local + mute state; apply to plugin") }

    fun updateTexture() { TODO("GPU: update media texture from plugin pixel buffer") }

    fun focus(hasFocus: Boolean) { TODO("GPU: relay focus to plugin") }

    fun clearCache() { TODO("GPU: clear plugin browser cache") }

    fun setSize(width: Int, height: Int) {
        mediaWidth = width
        mediaHeight = height
        TODO("GPU: resize plugin")
    }

    fun isAutoPlayable(): Boolean = autoPlay && !isDisabled

    fun getMediaTextureId(): LLUUID = textureId
}

data class MediaEntry(
    val mediaId: LLUUID,
    val currentUrl: String = "",
    val homeUrl: String = "",
    val widthPixels: Int = 0,
    val heightPixels: Int = 0,
    val autoScale: Boolean = false,
    val autoLoop: Boolean = false,
    val autoPlay: Boolean = false,
)

object ViewerMedia {

    const val AUTO_PLAY_MEDIA_SETTING           = "ParcelMediaAutoPlayEnable"
    const val SHOW_MEDIA_ON_OTHERS_SETTING      = "MediaShowOnOthers"
    const val SHOW_MEDIA_WITHIN_PARCEL_SETTING  = "MediaShowWithinParcel"
    const val SHOW_MEDIA_OUTSIDE_PARCEL_SETTING = "MediaShowOutsideParcel"

    private const val MAX_MEDIA_INSTANCES_DEFAULT = 8
    private const val MEDIA_INSTANCES_MIN_LIMIT   = 6

    private val impls: MutableList<MediaImpl> = mutableListOf()
    private val implsByTexture: MutableMap<LLUUID, MediaImpl> = mutableMapOf()
    private val observers: MutableList<MediaObserver> = mutableListOf()

    private var anyMediaShowing: Boolean = false
    private var anyMediaPlaying: Boolean = false
    private var globalVolume: Float = 1.0f
    private var maxInstances: Int = MAX_MEDIA_INSTANCES_DEFAULT
    private var forceUpdate: Boolean = false
    private var onlyAudibleTextureId: LLUUID = LLUUID.NULL

    var focusedMediaImpl: MediaImpl? = null
        private set(value) {
            field = value
            observers.forEach { it.onFocusedMediaChanged() }
        }

    var hoveredMediaImpl: MediaImpl? = null
        private set(value) {
            field = value
            observers.forEach { it.onHoveredMediaChanged() }
        }

    fun addObserver(observer: MediaObserver): Boolean {
        if (observers.contains(observer)) return false
        observers.add(observer)
        return true
    }

    fun removeObserver(observer: MediaObserver): Boolean = observers.remove(observer)

    fun newMediaImpl(
        textureId: LLUUID,
        mediaWidth: Int = 0,
        mediaHeight: Int = 0,
        mediaAutoScale: Boolean = false,
        mediaLoop: Boolean = false,
    ): MediaImpl {
        val existing = getMediaImplFromTextureID(textureId)
        return if (existing == null || textureId == LLUUID.NULL) {
            val impl = MediaImpl(textureId, mediaWidth, mediaHeight, mediaAutoScale, mediaLoop)
            impls.add(impl)
            if (textureId != LLUUID.NULL) implsByTexture[textureId] = impl
            impl
        } else {
            existing.unload()
            existing.mediaWidth  = mediaWidth
            existing.mediaHeight = mediaHeight
            existing.autoScale   = mediaAutoScale
            existing.loop        = mediaLoop
            existing
        }
    }

    fun updateMediaImpl(mediaEntry: MediaEntry, previousUrl: String, updateFromSelf: Boolean): MediaImpl {
        val existing = getMediaImplFromTextureID(mediaEntry.mediaId)
        return if (existing != null) {
            val wasLoaded = existing.hasMedia()
            existing.homeUrl     = mediaEntry.homeUrl
            existing.autoScale   = mediaEntry.autoScale
            existing.loop        = mediaEntry.autoLoop
            existing.mediaWidth  = mediaEntry.widthPixels
            existing.mediaHeight = mediaEntry.heightPixels
            existing.autoPlay    = mediaEntry.autoPlay
            existing.mediaEntryUrl = mediaEntry.currentUrl
            TODO("GPU: propagate autoScale/loop/size to media plugin if loaded")
            val urlChanged = existing.mediaEntryUrl != previousUrl
            if (existing.mediaEntryUrl.isEmpty()) {
                if (urlChanged) existing.unload()
            } else {
                val needsNavigate = (wasLoaded || existing.isAutoPlayable()) && !updateFromSelf && urlChanged
                if (needsNavigate) {
                    existing.navigateTo(existing.mediaEntryUrl, "", rediscoverType = true, serverRequest = true)
                } else if (existing.mediaUrl.isNotEmpty() && existing.mediaUrl != existing.mediaEntryUrl) {
                    existing.mediaUrl = existing.mediaEntryUrl
                    existing.navigateServerRequest = true
                }
            }
            existing
        } else {
            val impl = newMediaImpl(
                mediaEntry.mediaId,
                mediaEntry.widthPixels,
                mediaEntry.heightPixels,
                mediaEntry.autoScale,
                mediaEntry.autoLoop,
            )
            impl.homeUrl      = mediaEntry.homeUrl
            impl.autoPlay     = mediaEntry.autoPlay
            impl.mediaEntryUrl = mediaEntry.currentUrl
            if (impl.isAutoPlayable()) {
                impl.navigateTo(impl.mediaEntryUrl, "", rediscoverType = true, serverRequest = true)
            }
            impl
        }
    }

    fun getMediaImplFromTextureID(textureId: LLUUID): MediaImpl? = implsByTexture[textureId]

    fun textureHasMedia(textureId: LLUUID): Boolean = implsByTexture.containsKey(textureId)

    fun getPriorityList(): MutableList<MediaImpl> {
        impls.sortWith(compareByDescending { it.interest })
        return impls
    }

    fun setInWorldMediaFocus(impl: MediaImpl?) {
        focusedMediaImpl?.focus(false)
        focusedMediaImpl = impl
        impl?.focus(true)
    }

    fun setHoveredMedia(impl: MediaImpl?) {
        hoveredMediaImpl = impl
    }

    fun getVolume(): Float = globalVolume

    fun setVolume(volume: Float) {
        if (volume != globalVolume || forceUpdate) {
            globalVolume = volume
            impls.forEach { it.updateVolume() }
            forceUpdate = false
        }
    }

    fun isAnyMediaShowing(): Boolean = anyMediaShowing
    fun isAnyMediaPlaying(): Boolean = anyMediaPlaying

    fun setAllMediaEnabled(enabled: Boolean) {
        impls.forEach { it.isDisabled = !enabled }
    }

    fun setAllMediaPaused(paused: Boolean) {
        if (paused) impls.forEach { it.pause() } else impls.forEach { it.play() }
    }

    fun setMaxInstances(maxInstances: Int) {
        val effectiveMax = maxInstances.coerceAtLeast(MEDIA_INSTANCES_MIN_LIMIT)
        this.maxInstances = TODO("IPC: reduce by 2 if physical RAM < 8GB, else use effectiveMax") as Int
    }

    fun setOnlyAudibleMediaTextureId(textureId: LLUUID) {
        onlyAudibleTextureId = textureId
    }

    fun updateMedia(idle: Boolean = false) {
        TODO("IPC: prioritise impl list; drive plugin updates; manage texture uploads; enforce maxInstances; update anyMediaShowing / anyMediaPlaying")
    }

    fun getCurrentUserAgent(): String {
        TODO("IPC: build 'SecondLife/<version> (<channel>; <skin> skin)' user-agent string")
    }

    fun updateBrowserUserAgent() {
        val ua = getCurrentUserAgent()
        impls.forEach { impl ->
            TODO("GPU: if impl.mediaSource?.pluginSupportsMediaBrowser() == true impl.mediaSource?.setBrowserUserAgent($ua)")
        }
    }

    fun clearAllCookies() { impls.forEach { TODO("GPU: if it.mediaSource != null it.mediaSource?.clearCookies()") } }
    fun clearAllCaches()  { impls.forEach { it.clearCache() } }
    fun setCookiesEnabled(enabled: Boolean) { TODO("GPU: propagate cookie flag to all plugins") }

    fun setProxyConfig(enable: Boolean, host: String, port: Int) {
        TODO("GPU: propagate proxy settings to all plugins")
    }

    fun hasInWorldMedia(): Boolean = impls.any { !it.isParcelMedia }
    fun hasParcelMedia(): Boolean  = impls.any { it.isParcelMedia }

    fun getParcelAudioURL(): String { TODO("IPC: return URL of current parcel audio stream") }
    fun hasParcelAudio(): Boolean   { TODO("IPC: return whether parcel audio URL is non-empty") }
    fun isParcelMediaPlaying(): Boolean { TODO("IPC: check parcel media play state") }
    fun isParcelAudioPlaying(): Boolean { TODO("IPC: check parcel audio play state") }

    fun muteListChanged() {
        impls.forEach { TODO("IPC: re-evaluate mute state for each impl") }
    }

    fun openIdSetup(openIdUrl: String, openIdToken: String) {
        TODO("IPC: launch openIDSetupCoro")
    }

    fun proxyWindowOpened(target: String, uuid: String) {
        TODO("IPC: find impl by uuid; set target")
    }

    fun proxyWindowClosed(uuid: String) {
        TODO("IPC: find impl by uuid; unload")
    }

    fun createSpareBrowserMediaSource() {
        TODO("GPU: pre-create a browser plugin for fast first-use")
    }

    fun getSpareBrowserMediaSource(): Any? {
        TODO("GPU: return and clear spare browser plugin")
    }

    fun getHeaders(): Map<String, String> {
        TODO("IPC: build standard HTTP headers map including OpenID cookie")
    }

    fun getOpenIdCookie(mediaInstance: MediaCtrl): Boolean {
        TODO("IPC: inject the openid cookie into mediaInstance")
    }

    fun onTeleportFinished() {
        TODO("IPC: resume or restart paused media after teleport completes")
    }
}
