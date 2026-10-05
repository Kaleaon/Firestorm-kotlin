package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
) : PluginClassMediaOwner {

    val id: LLUUID = LLUUID.generateNewID()

    var homeUrl: String = startUrl
    var homeMimeType: String = initialMimeType
    var currentNavUrl: String = ""
        private set
    var errorPageUrl: String = errorPageUrl

    var trusted: Boolean = trustedContent
    var borderVisible: Boolean = borderVisible
    var frequentUpdates: Boolean = true
    var alwaysRefresh: Boolean = false
    var forceUpdate: Boolean = false
    var takeFocusOnClick: Boolean = focusOnClick
    var stretchToFill: Boolean = true
    var maintainAspectRatio: Boolean = true
    var hideLoading: Boolean = hideLoading
    var hidingInitialLoad: Boolean = false
    var decoupleTextureSize: Boolean = decoupleTextureSize
    var allowFileDownload: Boolean = false

    private var clearCacheOnCreate: Boolean = false
    private var hoverTextChanged: Boolean   = false
    private var updateScrolls: Boolean      = false

    var textureWidth: Int = textureWidth
        private set
    var textureHeight: Int = textureHeight
        private set

    val mediaTextureId: LLUUID = id

    private var target: String = ""
    private var mediaSource: MediaImpl? = null

    var panelWidth: Int  = 0
    var panelHeight: Int = 0

    private val observers: MutableList<PluginClassMediaOwner> = mutableListOf()

    init {
        if (homeUrl.isNotEmpty()) navigateHome()
    }

    fun addObserver(obs: PluginClassMediaOwner): Boolean {
        if (observers.contains(obs)) return false
        observers.add(obs)
        return true
    }

    fun remObserver(obs: PluginClassMediaOwner): Boolean = observers.remove(obs)

    override fun handleMediaEvent(plugin: PluginClassMedia, event: MediaEvent) {
        when (event) {
            MediaEvent.SIZE_CHANGED -> {
                // GPU: reshape to new plugin dimensions
            }
            MediaEvent.NAVIGATE_BEGIN -> {
                hideNotification()
            }
            MediaEvent.NAVIGATE_COMPLETE -> {
                hidingInitialLoad = false
            }
            MediaEvent.NAVIGATE_ERROR_PAGE -> {
                if (errorPageUrl.isNotEmpty()) navigateTo(errorPageUrl, "text/html")
            }
            MediaEvent.CLICK_LINK_HREF -> {
                val url  = plugin.clickUrl
                val t    = if (plugin.clickEnforceTarget) plugin.getOverrideClickTarget() else plugin.clickTarget
                val uuid = plugin.clickUuid
                // GPU: dispatch SLURL or call Web.loadUrl($url, $t, $uuid)
            }
            MediaEvent.AUTH_REQUEST -> {
                // GPU: show AuthRequest notification with host=${plugin.authUrl} realm=${plugin.authRealm}
            }
            MediaEvent.LINK_HOVERED -> {
                hoverTextChanged = true
            }
            MediaEvent.FILE_DOWNLOAD -> {
                if (allowFileDownload) {
                    // GPU: open save-file dialog for ${plugin.fileDownloadFilename}
                } else {
                    plugin.sendPickFileResponse(emptyList())
                    // GPU: show MediaFileDownloadUnsupported notification
                }
            }
            else -> Unit
        }
        observers.forEach { it.handleMediaEvent(plugin, event) }
    }

    fun navigateTo(url: String, mimeType: String = "", cleanBrowser: Boolean = false) {
        if (url.startsWith("secondlife://") || url.startsWith("hop://")) return
        val src = ensureMediaSourceInternal() ?: return
        currentNavUrl = url
        src.navigateTo(url, mimeType)
    }

    fun navigateToLocalPage(subdir: String, filename: String) {
        val src = ensureMediaSourceInternal() ?: return
        val path = "$subdir/$filename"
        currentNavUrl = path
        src.navigateTo(path, "text/html")
    }

    fun navigateHome() {
        val src = ensureMediaSourceInternal() ?: return
        if (homeUrl.isNotEmpty()) src.navigateTo(homeUrl, homeMimeType)
    }

    fun navigateBack() {
        mediaSource?.let {
            // GPU: plugin.browse_back()
        }
    }

    fun navigateForward() {
        mediaSource?.let {
            // GPU: plugin.browse_forward()
        }
    }

    fun navigateStop() {
        mediaSource?.let {
            // GPU: plugin.browse_stop()
        }
    }

    fun canNavigateBack(): Boolean    = false
    fun canNavigateForward(): Boolean = false

    fun getCurrentNavUrl(): String = currentNavUrl

    fun setHomePageUrl(url: String, mimeType: String = "") {
        homeUrl = url
        homeMimeType = mimeType
        mediaSource?.homeUrl = url
    }

    fun getHomePageUrl(): String = homeUrl

    fun setTarget(t: String) {
        target = t
        mediaSource?.let {
            // GPU: impl.setTarget(target)
        }
    }

    fun setErrorPageUrl(url: String) { errorPageUrl = url }
    fun getErrorPageUrl(): String    = errorPageUrl

    fun clearCache() {
        if (mediaSource != null) {
            // GPU: mediaSource.clearCache()
        } else {
            clearCacheOnCreate = true
        }
    }

    fun reload() {
        // GPU: plugin.browse_reload(ignoreCache=true) or navigateTo(currentNavUrl)
    }

    fun getMediaPlugin(): PluginClassMedia? {
        // GPU: return mediaSource?.getMediaPlugin()
        return null
    }

    fun ensureMediaSourceExists(): Boolean = ensureMediaSourceInternal() != null

    private fun ensureMediaSourceInternal(): MediaImpl? {
        if (mediaSource == null) {
            val impl = ViewerMedia.newMediaImpl(mediaTextureId, textureWidth, textureHeight)
            impl.homeUrl = homeUrl
            impl.isTrustedBrowser = trusted
            if (clearCacheOnCreate) {
                // GPU: impl.clearCache()
                clearCacheOnCreate = false
            }
            mediaSource = impl
        }
        return mediaSource
    }

    fun unloadMediaSource() {
        mediaSource?.let {
            // GPU: it.remObserver(this)
        }
        mediaSource = null
    }

    fun setCaretColor(red: UInt, green: UInt, blue: UInt): Boolean = false

    fun setTextureSize(width: Int, height: Int) {
        textureWidth  = width
        textureHeight = height
        if (mediaSource != null) {
            // GPU: mediaSource.setSize($width, $height)
            forceUpdate = true
        }
    }

    fun setTrustedContent(trusted: Boolean) {
        this.trusted = trusted
        mediaSource?.isTrustedBrowser = trusted
    }

    fun setAllowFileDownload(allow: Boolean) { allowFileDownload = allow }
    fun setBorderVisible(visible: Boolean)   {
        borderVisible = visible
        // GPU: mBorder?.setVisible(visible)
    }
    fun setTakeFocusOnClick(takeFocus: Boolean) { takeFocusOnClick = takeFocus }
    fun setFrequentUpdates(frequent: Boolean) { frequentUpdates = frequent }
    fun setAlwaysRefresh(refresh: Boolean)   { alwaysRefresh = refresh }
    fun setForceUpdate(force: Boolean)       { forceUpdate = force }
    fun setDecoupleTextureSize(decouple: Boolean) { decoupleTextureSize = decouple }

    fun wantsKeyUpKeyDown(): Boolean = true
    fun wantsReturnKey(): Boolean    = true
    fun acceptsTextInput(): Boolean  = true

    fun onFocusReceived() {
        mediaSource?.focus(true)
        // GPU: LLEditMenuHandler.gEditMenuHandler = mediaSource; LLPanel.onFocusReceived()
    }

    fun onFocusLost() {
        mediaSource?.focus(false)
        // GPU: clear LLEditMenuHandler.gEditMenuHandler if it was mediaSource; viewerWindow.focusClient(); LLPanel.onFocusLost()
    }

    fun setFocus(hasFocus: Boolean) {
        // APR: SDL2 IME position update if hasFocus
        if (hasFocus) onFocusReceived() else onFocusLost()
    }

    fun handleToolTip(x: Int, y: Int, modifiers: Int): Boolean {
        val hoverText = mediaSource?.let {
            // GPU: if it.hasMedia() it.getMediaPlugin()?.hoverText else null
            null
        } as? String ?: return false
        if (hoverText.isEmpty()) return false
        // GPU: show tooltip with message=$hoverText at screen coords converted from ($x,$y)
        return true
    }

    fun onVisibilityChange(visible: Boolean) {
        frequentUpdates = visible
        mediaSource?.visible = visible
    }

    fun reshape(width: Int, height: Int) {
        panelWidth  = width
        panelHeight = height
        if (!decoupleTextureSize && width > 0 && height > 0) {
            setTextureSize(width, height)
        }
        // GPU: forward reshape to UI panel super
    }

    fun handleHover(x: Int, y: Int, modifiers: Int): Boolean {
        val (mx, my) = convertInputCoords(x, y)
        mediaSource?.let {
            // GPU: mediaSource.mouseMove($mx, $my, $modifiers); viewerWindow.setCursor(mediaSource.getLastSetCursor())
        }
        if (hoverTextChanged) {
            hoverTextChanged = false
            handleToolTip(x, y, modifiers)
        }
        return true
    }

    fun handleMouseDown(x: Int, y: Int, modifiers: Int): Boolean {
        val (mx, my) = convertInputCoords(x, y)
        mediaSource?.let {
            // GPU: mediaSource.mouseDown($mx, $my, $modifiers)
        }
        // GPU: focusMgr.setMouseCapture(this)
        if (takeFocusOnClick) setFocus(true)
        return true
    }

    fun handleMouseUp(x: Int, y: Int, modifiers: Int): Boolean {
        val (mx, my) = convertInputCoords(x, y)
        mediaSource?.let {
            // GPU: mediaSource.mouseUp($mx, $my, $modifiers)
        }
        // GPU: focusMgr.setMouseCapture(null)
        return true
    }

    fun handleRightMouseDown(x: Int, y: Int, modifiers: Int): Boolean {
        val (mx, my) = convertInputCoords(x, y)
        mediaSource?.let {
            // GPU: mediaSource.mouseDown($mx, $my, $modifiers, button=1)
        }
        // GPU: focusMgr.setMouseCapture(this)
        if (takeFocusOnClick) setFocus(true)
        // GPU: build and show context menu with debug items gated by MediaPluginDebugging setting
        return true
    }

    fun handleRightMouseUp(x: Int, y: Int, modifiers: Int): Boolean {
        val (mx, my) = convertInputCoords(x, y)
        mediaSource?.let {
            // GPU: mediaSource.mouseUp($mx, $my, $modifiers, button=1)
            if (!takeFocusOnClick) {
                // GPU: mediaSource.focus(false); viewerWindow.focusClient()
            }
        }
        // GPU: focusMgr.setMouseCapture(null)
        return true
    }

    fun handleDoubleClick(x: Int, y: Int, modifiers: Int): Boolean {
        val (mx, my) = convertInputCoords(x, y)
        mediaSource?.let {
            // GPU: mediaSource.mouseDoubleClick($mx, $my, $modifiers)
        }
        // GPU: focusMgr.setMouseCapture(this)
        if (takeFocusOnClick) setFocus(true)
        return true
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        val (mx, my) = convertInputCoords(x, y)
        mediaSource?.let {
            // GPU: mediaSource.scrollWheel($mx, $my, clicksX=0, clicksY=$clicks)
        }
        return true
    }

    fun handleScrollHWheel(x: Int, y: Int, clicks: Int): Boolean {
        val (mx, my) = convertInputCoords(x, y)
        mediaSource?.let {
            // GPU: mediaSource.scrollWheel($mx, $my, clicksX=$clicks, clicksY=0)
        }
        return true
    }

    fun handleKeyHere(key: Int, modifiers: Int): Boolean {
        // GPU: mediaSource.handleKeyHere($key, $modifiers)
        return false
    }

    fun handleKeyUpHere(key: Int, modifiers: Int): Boolean {
        // GPU: mediaSource.handleKeyUpHere($key, $modifiers)
        return false
    }

    fun handleUnicodeCharHere(unicodeChar: Int): Boolean {
        // GPU: mediaSource.handleUnicodeCharHere($unicodeChar)
        return false
    }

    fun showNotification(notifyName: String, icon: String, canClose: Boolean) {
        // GPU: configure and show window-shade notification overlay
    }

    fun hideNotification() {
        // GPU: hide window-shade notification overlay
    }

    fun onOpenWebInspector() {
        // GPU: plugin.showWebInspector(true)
    }

    fun onShowSource() {
        // GPU: plugin.showPageSource()
    }

    fun draw(alpha: Float = 1f) {
        // GPU: bind media texture, compute quad offsets via calcOffsetsAndSize, emit triangle pair with correct UV orientation based on plugin.textureCoordsOpenGL
    }

    data class QuadLayout(val xOffset: Int, val yOffset: Int, val width: Int, val height: Int)

    fun calcOffsetsAndSize(): QuadLayout {
        val plugin = getMediaPlugin()
        if (stretchToFill) {
            if (maintainAspectRatio && plugin != null && plugin.mediaHeight > 0) {
                val mediaAspect = plugin.mediaWidth.toFloat() / plugin.mediaHeight.toFloat()
                val viewAspect  = panelWidth.toFloat() / panelHeight.toFloat().coerceAtLeast(1f)
                val w: Int
                val h: Int
                if (mediaAspect > viewAspect) {
                    w = panelWidth
                    h = min(max((w / mediaAspect).roundToInt(), 0), panelHeight)
                } else {
                    h = panelHeight
                    w = min(max((h * mediaAspect).roundToInt(), 0), panelWidth)
                }
                return QuadLayout((panelWidth - w) / 2, (panelHeight - h) / 2, w, h)
            }
            return QuadLayout(0, 0, panelWidth, panelHeight)
        }
        val pw = if (plugin != null) min(plugin.mediaWidth,  panelWidth)  else panelWidth
        val ph = if (plugin != null) min(plugin.mediaHeight, panelHeight) else panelHeight
        return QuadLayout((panelWidth - pw) / 2, (panelHeight - ph) / 2, pw, ph)
    }

    private fun convertInputCoords(x: Int, y: Int): Pair<Int, Int> {
        val layout = calcOffsetsAndSize()
        val ax = x - layout.xOffset
        val ay = y - layout.yOffset
        val plugin = getMediaPlugin()
        val coordsOpenGl = plugin?.textureCoordsOpenGL ?: false
        val mx = ax
        val my = if (!coordsOpenGl) ay else (panelHeight - ay)
        return mx to my
    }

    override fun toString(): String =
        "MediaCtrl(url='$currentNavUrl', trusted=$trusted, id=$id)"
}
