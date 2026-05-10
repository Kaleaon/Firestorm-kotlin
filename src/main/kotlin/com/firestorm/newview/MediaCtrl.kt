/**
 * MediaCtrl.kt
 * Converted from llmediactrl.h / llmediactrl.cpp
 *
 * In-world media browser control — hosts a web/media plugin surface inside a
 * viewer UI panel.  Equivalent to LLMediaCtrl in the C++ viewer.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.*

/**
 * Represents a media event that the control can receive from the underlying
 * plugin (mirrors EMediaEvent from the C++ side).
 */
enum class MediaEvent {
    NAVIGATE_BEGIN,
    NAVIGATE_COMPLETE,
    NAVIGATE_ERROR,
    LOAD_STARTED,
    LOAD_FINISHED,
    LINK_HOVERED,
    NAVIGATE_TO_PAGE_COMPLETE,
}

/**
 * In-world browser / media control panel.
 *
 * Lifecycle: create with [MediaCtrl], set [homeUrl], call [navigateTo] or
 * [navigateHome].  Poll events via [onMediaEvent].
 *
 * C++ lineage: LLMediaCtrl : LLPanel, LLViewerMediaObserver, LLInstanceTracker
 */
class MediaCtrl(
    startUrl: String = "",
    trustedContent: Boolean = false,
    borderVisible: Boolean = true,
    textureWidth: Int = 1024,
    textureHeight: Int = 1024,
    focusOnClick: Boolean = true,
    decoupleTextureSize: Boolean = false,
    hideLoading: Boolean = false,
    initialMimeType: String = "",
    errorPageUrl: String = "",
) {

    // ── Navigation state ──────────────────────────────────────────────────

    /** URL the control navigates to on [navigateHome]. */
    var homeUrl: String = startUrl

    /** MIME type hint associated with [homeUrl]. */
    var homeMimeType: String = ""

    /** The URL currently loaded / being loaded. */
    var currentNavUrl: String = ""
        private set

    /** URL shown when a navigation error occurs. */
    var errorPageUrl: String = errorPageUrl

    // ── Display flags ─────────────────────────────────────────────────────

    var trusted: Boolean = trustedContent
    var borderVisible: Boolean = borderVisible
    var frequentUpdates: Boolean = true
    var alwaysRefresh: Boolean = false
    var forceUpdate: Boolean = false
    var takeFocusOnClick: Boolean = focusOnClick
    var stretchToFill: Boolean = true
    var maintainAspectRatio: Boolean = true
    var hideLoading: Boolean = hideLoading
    var decoupleTextureSize: Boolean = decoupleTextureSize
    var allowFileDownload: Boolean = false

    // ── Texture / plugin metadata ─────────────────────────────────────────

    var textureWidth: Int = textureWidth
        private set
    var textureHeight: Int = textureHeight
        private set

    val mediaTextureId: LLUUID = LLUUID.generateNewID()

    // ── Zoom / caret ──────────────────────────────────────────────────────

    /** Current zoom level applied to the rendered media surface. */
    var zoom: Float = 1.0f
        private set

    // ── History ───────────────────────────────────────────────────────────

    private val navHistory: ArrayDeque<String> = ArrayDeque()
    private var historyPos: Int = -1

    private var target: String = ""
    private var initialMimeType: String = initialMimeType

    // ── Event callback ────────────────────────────────────────────────────

    /** Invoked whenever the underlying media plugin fires an event. */
    var onMediaEvent: ((event: MediaEvent, url: String) -> Unit)? = null

    // ── Navigation ────────────────────────────────────────────────────────

    /**
     * Navigate to [url] with an optional [mimeType] hint.
     * If [cleanBrowser] is true the browsing history is cleared first.
     */
    fun navigateTo(url: String, mimeType: String = "", cleanBrowser: Boolean = false) {
        if (cleanBrowser) {
            navHistory.clear()
            historyPos = -1
        }
        // Trim history forward of current position on new navigation
        if (historyPos < navHistory.size - 1) {
            repeat(navHistory.size - 1 - historyPos) { navHistory.removeLast() }
        }
        navHistory.addLast(url)
        historyPos = navHistory.size - 1
        currentNavUrl = url
        onMediaEvent?.invoke(MediaEvent.NAVIGATE_BEGIN, url)
    }

    /** Navigate to a local viewer page identified by [subdir] and [filename]. */
    fun navigateToLocalPage(subdir: String, filename: String) {
        navigateTo("file://$subdir/$filename")
    }

    /** Navigate to [homeUrl]. */
    fun navigateHome() {
        if (homeUrl.isNotEmpty()) navigateTo(homeUrl, homeMimeType)
    }

    /** Step backward in history if possible. */
    fun navigateBack() {
        if (canNavigateBack()) {
            historyPos--
            currentNavUrl = navHistory[historyPos]
            onMediaEvent?.invoke(MediaEvent.NAVIGATE_BEGIN, currentNavUrl)
        }
    }

    /** Step forward in history if possible. */
    fun navigateForward() {
        if (canNavigateForward()) {
            historyPos++
            currentNavUrl = navHistory[historyPos]
            onMediaEvent?.invoke(MediaEvent.NAVIGATE_BEGIN, currentNavUrl)
        }
    }

    /** Abort the current in-flight navigation. */
    fun navigateStop() {
        onMediaEvent?.invoke(MediaEvent.NAVIGATE_ERROR, currentNavUrl)
    }

    fun canNavigateBack(): Boolean = historyPos > 0
    fun canNavigateForward(): Boolean = historyPos < navHistory.size - 1

    // ── Zoom ─────────────────────────────────────────────────────────────

    fun setZoom(f: Float) {
        zoom = f.coerceIn(0.1f, 8.0f)
    }

    // ── Navigation helpers ────────────────────────────────────────────────

    fun setHomePageUrl(url: String, mimeType: String = "") {
        homeUrl = url
        homeMimeType = mimeType
    }
    fun getHomePageUrl(): String = homeUrl

    fun setTarget(t: String) { target = t }

    fun setErrorPageUrl(url: String) { errorPageUrl = url }
    fun getErrorPageUrl(): String = errorPageUrl

    // ── Texture size ──────────────────────────────────────────────────────

    fun setTextureSize(width: Int, height: Int) {
        textureWidth = width
        textureHeight = height
    }

    // ── Trust ─────────────────────────────────────────────────────────────

    fun setTrustedContent(trusted: Boolean) { this.trusted = trusted }

    // ── Cache ─────────────────────────────────────────────────────────────

    /** Signal that the browser cache should be cleared on next load. */
    fun clearCache() { /* implementation delegates to plugin layer */ }

    // ── Focus ─────────────────────────────────────────────────────────────

    fun onFocusReceived() { /* forward key events to plugin */ }
    fun onFocusLost()     { /* stop forwarding key events  */ }

    // ── Reload / stop ─────────────────────────────────────────────────────

    /** Reload the current page. */
    fun reload() {
        if (currentNavUrl.isNotEmpty()) navigateTo(currentNavUrl)
    }

    /** Stop media playback (alias for [navigateStop]). */
    fun stop() = navigateStop()

    // ── Source management ─────────────────────────────────────────────────

    fun ensureMediaSourceExists(): Boolean = true   // stub; real impl talks to plugin
    fun unloadMediaSource() { /* release plugin handle */ }

    // ── Caret color ───────────────────────────────────────────────────────

    fun setCaretColor(red: UInt, green: UInt, blue: UInt): Boolean = true

    // ── Misc ──────────────────────────────────────────────────────────────

    fun setBorderVisible(visible: Boolean) { borderVisible = visible }
    fun setTakeFocusOnClick(takeFocus: Boolean) { takeFocusOnClick = takeFocus }
    fun setFrequentUpdates(frequent: Boolean) { frequentUpdates = frequent }
    fun setAlwaysRefresh(refresh: Boolean) { alwaysRefresh = refresh }
    fun setForceUpdate(force: Boolean) { forceUpdate = force }
    fun setDecoupleTextureSize(decouple: Boolean) { decoupleTextureSize = decouple }
    fun setAllowFileDownload(allow: Boolean) { allowFileDownload = allow }

    override fun toString(): String =
        "MediaCtrl(url='$currentNavUrl', trusted=$trusted, zoom=$zoom)"
}
