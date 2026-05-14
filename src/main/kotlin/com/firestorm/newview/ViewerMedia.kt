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

    fun hasMedia(): Boolean { System.err.println("MediaImpl: hasMedia not yet implemented"); return false }

    fun play()  { System.err.println("MediaImpl: play not yet implemented") }
    fun stop()  { System.err.println("MediaImpl: stop not yet implemented") }
    fun pause() { System.err.println("MediaImpl: pause not yet implemented") }
    fun start() { System.err.println("MediaImpl: start not yet implemented") }
    fun unload() { System.err.println("MediaImpl: unload not yet implemented") }

    fun navigateTo(url: String, mimeType: String = "", rediscoverType: Boolean = false, serverRequest: Boolean = false) {
        this.mediaUrl = url
        this.navigateServerRequest = serverRequest
        System.err.println("MediaImpl: navigateTo not yet implemented")
    }

    fun setVolume(v: Float) { volume = v; System.err.println("MediaImpl: setVolume not yet implemented") }
    fun setMute(mute: Boolean) { System.err.println("MediaImpl: setMute not yet implemented") }
    fun updateVolume() { System.err.println("MediaImpl: updateVolume not yet implemented") }

    fun updateTexture() { System.err.println("MediaImpl: updateTexture not yet implemented") }

    fun focus(hasFocus: Boolean) { System.err.println("MediaImpl: focus not yet implemented") }

    fun clearCache() { System.err.println("MediaImpl: clearCache not yet implemented") }

    fun setSize(width: Int, height: Int) {
        mediaWidth = width
        mediaHeight = height
        System.err.println("MediaImpl: setSize not yet implemented")
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
            System.err.println("ViewerMedia: updateMediaImpl (propagate autoScale/loop/size to plugin) not yet implemented")
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
        System.err.println("ViewerMedia: setMaxInstances (RAM-based reduction) not yet implemented")
        this.maxInstances = effectiveMax
    }

    fun setOnlyAudibleMediaTextureId(textureId: LLUUID) {
        onlyAudibleTextureId = textureId
    }

    fun updateMedia(idle: Boolean = false) {
        System.err.println("ViewerMedia: updateMedia not yet implemented")
    }

    fun getCurrentUserAgent(): String {
        System.err.println("ViewerMedia: getCurrentUserAgent not yet implemented")
        return ""
    }

    fun updateBrowserUserAgent() {
        val ua = getCurrentUserAgent()
        impls.forEach { impl ->
            System.err.println("ViewerMedia: updateBrowserUserAgent not yet implemented")
        }
    }

    fun clearAllCookies() { impls.forEach { System.err.println("ViewerMedia: clearAllCookies not yet implemented") } }
    fun clearAllCaches()  { impls.forEach { it.clearCache() } }
    fun setCookiesEnabled(enabled: Boolean) { System.err.println("ViewerMedia: setCookiesEnabled not yet implemented") }

    fun setProxyConfig(enable: Boolean, host: String, port: Int) {
        System.err.println("ViewerMedia: setProxyConfig not yet implemented")
    }

    fun hasInWorldMedia(): Boolean = impls.any { !it.isParcelMedia }
    fun hasParcelMedia(): Boolean  = impls.any { it.isParcelMedia }

    fun getParcelAudioURL(): String { System.err.println("ViewerMedia: getParcelAudioURL not yet implemented"); return "" }
    fun hasParcelAudio(): Boolean   { System.err.println("ViewerMedia: hasParcelAudio not yet implemented"); return false }
    fun isParcelMediaPlaying(): Boolean { System.err.println("ViewerMedia: isParcelMediaPlaying not yet implemented"); return false }
    fun isParcelAudioPlaying(): Boolean { System.err.println("ViewerMedia: isParcelAudioPlaying not yet implemented"); return false }

    fun muteListChanged() {
        impls.forEach { System.err.println("ViewerMedia: muteListChanged not yet implemented") }
    }

    fun openIdSetup(openIdUrl: String, openIdToken: String) {
        System.err.println("ViewerMedia: openIdSetup not yet implemented")
    }

    fun proxyWindowOpened(target: String, uuid: String) {
        System.err.println("ViewerMedia: proxyWindowOpened not yet implemented")
    }

    fun proxyWindowClosed(uuid: String) {
        System.err.println("ViewerMedia: proxyWindowClosed not yet implemented")
    }

    fun createSpareBrowserMediaSource() {
        System.err.println("ViewerMedia: createSpareBrowserMediaSource not yet implemented")
    }

    fun getSpareBrowserMediaSource(): Any? {
        return null
    }

    fun getHeaders(): Map<String, String> {
        return emptyMap()
    }

    fun getOpenIdCookie(mediaInstance: MediaCtrl): Boolean {
        return false
    }

    fun onTeleportFinished() {
        System.err.println("ViewerMedia: onTeleportFinished not yet implemented")
    }
}
