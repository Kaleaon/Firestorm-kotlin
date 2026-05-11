package com.firestorm.llplugin

import kotlin.math.max

private const val LLPLUGIN_MESSAGE_CLASS_MEDIA = "media"
private const val LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER = "media_browser"
private const val LLPLUGIN_MESSAGE_CLASS_MEDIA_TIME = "media_time"
private const val LLPLUGIN_MESSAGE_CLASS_INTERNAL = "internal"

private const val LOW_PRIORITY_TEXTURE_SIZE_DEFAULT = 256

private fun nextPowerOf2(value: Int): Int {
    var p = 1
    while (p < value) p = p shl 1
    return p
}

class LLPluginClassMedia(private var owner: LLPluginClassMediaOwner?) : LLPluginProcessParentOwner() {

    enum class EMouseEventType { MOUSE_EVENT_DOWN, MOUSE_EVENT_UP, MOUSE_EVENT_MOVE, MOUSE_EVENT_DOUBLE_CLICK }
    enum class EKeyEventType { KEY_EVENT_DOWN, KEY_EVENT_UP, KEY_EVENT_REPEAT }

    enum class EPriority {
        PRIORITY_UNLOADED,
        PRIORITY_STOPPED,
        PRIORITY_HIDDEN,
        PRIORITY_SLIDESHOW,
        PRIORITY_LOW,
        PRIORITY_NORMAL,
        PRIORITY_HIGH,
    }

    private var plugin: LLPluginProcessParent? = null
    private val sendQueue: ArrayDeque<LLPluginMessage> = ArrayDeque()

    private var textureParamsReceived: Boolean = false
    private var requestedTextureDepth: Int = 0
    private var requestedTextureInternalFormat: UInt = 0u
    private var requestedTextureFormat: UInt = 0u
    private var requestedTextureType: UInt = 0u
    private var requestedTextureSwapBytes: Boolean = false
    private var requestedTextureCoordsOpenGL: Boolean = false

    private var textureSharedMemoryName: String = ""
    private var textureSharedMemorySize: Long = 0L

    private var autoScaleMedia: Boolean = false
    private var defaultMediaWidth: Int = 0
    private var defaultMediaHeight: Int = 0
    private var naturalMediaWidth: Int = 0
    private var naturalMediaHeight: Int = 0
    private var setMediaWidth: Int = -1
    private var setMediaHeight: Int = -1
    private var fullMediaWidth: Int = 0
    private var fullMediaHeight: Int = 0
    private var requestedMediaWidth: Int = 0
    private var requestedMediaHeight: Int = 0
    private var requestedTextureWidth: Int = 0
    private var requestedTextureHeight: Int = 0
    private var textureWidth: Int = 0
    private var textureHeight: Int = 0
    private var mediaWidth: Int = 0
    private var mediaHeight: Int = 0

    private var zoomFactor: Double = 1.0
    private var requestedVolume: Float = 0.0f

    private var priority: EPriority = EPriority.PRIORITY_NORMAL
    private var lowPrioritySizeLimit: Int = LOW_PRIORITY_TEXTURE_SIZE_DEFAULT
    private var allowDownsample: Boolean = false
    private var padding: Int = 0

    private var dirtyLeft: Int = 0
    private var dirtyTop: Int = 0
    private var dirtyRight: Int = 0
    private var dirtyBottom: Int = 0
    private var dirtyRectEmpty: Boolean = true

    private var cursorName: String = ""
    private var lastMouseX: Int = 0
    private var lastMouseY: Int = 0

    var status: LLPluginClassMediaOwner.EMediaStatus = LLPluginClassMediaOwner.EMediaStatus.MEDIA_NONE
        private set

    private var sleepTime: Double = 1.0 / 100.0

    private var canUndo: Boolean = false
    private var canRedo: Boolean = false
    private var canCut: Boolean = false
    private var canCopy: Boolean = false
    private var canPaste: Boolean = false
    private var canDoDelete: Boolean = false
    private var canSelectAll: Boolean = false

    private var mediaName: String = ""
    private var mediaDescription: String = ""

    private var backgroundR: Double = 1.0
    private var backgroundG: Double = 1.0
    private var backgroundB: Double = 1.0
    private var backgroundA: Double = 1.0

    private var target: String = ""

    private var navigateURI: String = ""
    private var navigateResultCode: Int = -1
    private var navigateResultString: String = ""
    private var historyBackAvailable: Boolean = false
    private var historyForwardAvailable: Boolean = false
    private var statusText: String = ""
    private var progressPercent: Int = 0
    private var location: String = ""
    private var clickURL: String = ""
    private var clickNavType: String = ""
    private var clickTarget: String = ""
    private var clickUUID: String = ""
    private var clickEnforceTarget: Boolean = false
    private var overrideClickTarget: String = ""
    private var debugMessageText: String = ""
    private var debugMessageLevel: String = ""
    private var geometryX: Int = 0
    private var geometryY: Int = 0
    private var geometryWidth: Int = 0
    private var geometryHeight: Int = 0
    private var statusCode: Int = 0
    private var authURL: String = ""
    private var authRealm: String = ""
    private var hoverText: String = ""
    private var hoverLink: String = ""
    private var fileDownloadFilename: String = ""
    private var isMultipleFilePick: Boolean = false

    private var currentTime: Double = 0.0
    private var duration: Double = 0.0
    private var currentRate: Double = 0.0
    private var loadedDuration: Double = 0.0

    private var artist: String = ""
    private var title: String = ""

    private var deleteOK: Boolean = true

    init {
        reset()
    }

    fun init(launcherFilename: String, pluginDir: String, pluginFilename: String, debug: Boolean): Boolean {
        plugin = LLPluginProcessParent.create(this).also { p ->
            p.setSleepTime(sleepTime)
        }
        val message = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "init")
        message.setValue("target", target)
        message.setValueReal("factor", zoomFactor)
        sendMessage(message)
        plugin!!.init(launcherFilename, pluginDir, pluginFilename, debug)
        return true
    }

    fun reset() {
        plugin?.requestShutdown()
        plugin = null

        textureParamsReceived = false
        requestedTextureDepth = 0
        requestedTextureInternalFormat = 0u
        requestedTextureFormat = 0u
        requestedTextureType = 0u
        requestedTextureSwapBytes = false
        requestedTextureCoordsOpenGL = false
        textureSharedMemorySize = 0L
        textureSharedMemoryName = ""
        defaultMediaWidth = 0
        defaultMediaHeight = 0
        naturalMediaWidth = 0
        naturalMediaHeight = 0
        setMediaWidth = -1
        setMediaHeight = -1
        requestedMediaWidth = 0
        requestedMediaHeight = 0
        requestedTextureWidth = 0
        requestedTextureHeight = 0
        fullMediaWidth = 0
        fullMediaHeight = 0
        textureWidth = 0
        textureHeight = 0
        mediaWidth = 0
        mediaHeight = 0
        dirtyRectEmpty = true
        autoScaleMedia = false
        requestedVolume = 0.0f
        priority = EPriority.PRIORITY_NORMAL
        lowPrioritySizeLimit = LOW_PRIORITY_TEXTURE_SIZE_DEFAULT
        allowDownsample = false
        padding = 0
        lastMouseX = 0
        lastMouseY = 0
        status = LLPluginClassMediaOwner.EMediaStatus.MEDIA_NONE
        sleepTime = 1.0 / 100.0
        canUndo = false
        canRedo = false
        canCut = false
        canCopy = false
        canPaste = false
        canDoDelete = false
        canSelectAll = false
        mediaName = ""
        mediaDescription = ""
        backgroundR = 1.0; backgroundG = 1.0; backgroundB = 1.0; backgroundA = 1.0
        navigateURI = ""
        navigateResultCode = -1
        navigateResultString = ""
        historyBackAvailable = false
        historyForwardAvailable = false
        statusText = ""
        progressPercent = 0
        clickURL = ""
        clickNavType = ""
        clickTarget = ""
        clickUUID = ""
        statusCode = 0
        clickEnforceTarget = false
        currentTime = 0.0
        duration = 0.0
        currentRate = 0.0
        loadedDuration = 0.0
    }

    fun idle() {
        plugin?.idle()

        val p = plugin
        if (mediaWidth == -1 || !textureParamsReceived || p == null || p.isBlocked || owner == null) {
            // cannot process a size change right now
        } else if (requestedMediaWidth != mediaWidth || requestedMediaHeight != mediaHeight) {
            requestedTextureHeight = requestedMediaHeight
            requestedTextureWidth = if (padding < 0) {
                nextPowerOf2(requestedMediaWidth)
            } else {
                var w = requestedMediaWidth
                if (padding > 1) {
                    val rowbytes = w * requestedTextureDepth
                    val pad = rowbytes % padding
                    val paddedRowbytes = if (pad != 0) rowbytes + padding - pad else rowbytes
                    if (paddedRowbytes % requestedTextureDepth == 0)
                        paddedRowbytes / requestedTextureDepth
                    else
                        w
                } else w
            }

            val newsize = (requestedTextureWidth * requestedTextureHeight * requestedTextureDepth +
                    requestedTextureWidth * requestedTextureDepth).toLong()

            if (newsize != textureSharedMemorySize) {
                if (textureSharedMemoryName.isNotEmpty()) {
                    p.removeSharedMemory(textureSharedMemoryName)
                    textureSharedMemoryName = ""
                }
                textureSharedMemorySize = newsize
                textureSharedMemoryName = p.addSharedMemory(textureSharedMemorySize)
                if (textureSharedMemoryName.isNotEmpty()) {
                    TODO("GPU: zero-fill the new shared memory texture buffer")
                }
            }

            textureWidth = -1
            textureHeight = -1
            mediaWidth = -1
            mediaHeight = -1
            resetDirty()

            val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "size_change")
            msg.setValue("name", textureSharedMemoryName)
            msg.setValueS32("width", requestedMediaWidth)
            msg.setValueS32("height", requestedMediaHeight)
            msg.setValueS32("texture_width", requestedTextureWidth)
            msg.setValueS32("texture_height", requestedTextureHeight)
            msg.setValueReal("background_r", backgroundR)
            msg.setValueReal("background_g", backgroundG)
            msg.setValueReal("background_b", backgroundB)
            msg.setValueReal("background_a", backgroundA)
            p.sendMessage(msg)
        }

        if (p != null && p.isRunning()) {
            while (sendQueue.isNotEmpty()) {
                p.sendMessage(sendQueue.removeFirst())
            }
        }
    }

    fun getWidth(): Int = max(mediaWidth, 0)
    fun getHeight(): Int = max(mediaHeight, 0)
    fun getNaturalWidth(): Int = naturalMediaWidth
    fun getNaturalHeight(): Int = naturalMediaHeight
    fun getSetWidth(): Int = setMediaWidth
    fun getSetHeight(): Int = setMediaHeight
    fun getBitsWidth(): Int = max(textureWidth, 0)
    fun getBitsHeight(): Int = max(textureHeight, 0)
    fun getTextureWidth(): Int = nextPowerOf2(textureWidth)
    fun getTextureHeight(): Int = nextPowerOf2(textureHeight)
    fun getFullWidth(): Int = fullMediaWidth
    fun getFullHeight(): Int = fullMediaHeight
    fun getZoomFactor(): Double = zoomFactor
    fun setZoomFactor(factor: Double) { zoomFactor = factor }

    fun getBitsData(): ByteArray? {
        val p = plugin ?: return null
        if (textureSharedMemoryName.isEmpty()) return null
        return TODO("GPU: return ByteArray mapped to shared memory segment '$textureSharedMemoryName'")
    }

    fun getTextureDepth(): Int = requestedTextureDepth
    fun getTextureFormatInternal(): UInt = requestedTextureInternalFormat
    fun getTextureFormatPrimary(): UInt = requestedTextureFormat
    fun getTextureFormatType(): UInt = requestedTextureType
    fun getTextureFormatSwapBytes(): Boolean = requestedTextureSwapBytes
    fun getTextureCoordsOpenGL(): Boolean = requestedTextureCoordsOpenGL

    fun setSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            setMediaWidth = width
            setMediaHeight = height
        } else {
            setMediaWidth = -1
            setMediaHeight = -1
        }
        setSizeInternal()
    }

    fun setAutoScale(autoScale: Boolean) {
        if (autoScale != autoScaleMedia) {
            autoScaleMedia = autoScale
            setSizeInternal()
        }
    }

    fun setBackgroundColor(r: Double, g: Double, b: Double, a: Double) {
        backgroundR = r; backgroundG = g; backgroundB = b; backgroundA = a
    }

    fun setOwner(owner: LLPluginClassMediaOwner?) { this.owner = owner }

    fun textureValid(): Boolean {
        if (!textureParamsReceived || textureWidth <= 0 || textureHeight <= 0
            || mediaWidth <= 0 || mediaHeight <= 0
            || requestedMediaWidth != mediaWidth || requestedMediaHeight != mediaHeight) return false
        return getBitsData() != null
    }

    fun getDirty(outRect: IntArray? = null): Boolean {
        val result = !dirtyRectEmpty
        if (outRect != null && outRect.size >= 4) {
            outRect[0] = dirtyLeft; outRect[1] = dirtyTop
            outRect[2] = dirtyRight; outRect[3] = dirtyBottom
        }
        return result
    }

    fun resetDirty() { dirtyRectEmpty = true }

    fun mouseEvent(type: EMouseEventType, button: Int, x: Int, y: Int, modifiers: Int) {
        val p = plugin
        if (type == EMouseEventType.MOUSE_EVENT_MOVE) {
            if (p == null || !p.isRunning() || p.isBlocked) return
            if (x == lastMouseX && y == lastMouseY) return
            lastMouseX = x
            lastMouseY = y
        }

        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "mouse_event")
        msg.setValue("event", when (type) {
            EMouseEventType.MOUSE_EVENT_DOWN -> "down"
            EMouseEventType.MOUSE_EVENT_UP -> "up"
            EMouseEventType.MOUSE_EVENT_MOVE -> "move"
            EMouseEventType.MOUSE_EVENT_DOUBLE_CLICK -> "double_click"
        })
        msg.setValueS32("button", button)
        msg.setValueS32("x", x)
        val adjustedY = if (!requestedTextureCoordsOpenGL) mediaHeight - y else y
        msg.setValueS32("y", adjustedY)
        msg.setValue("modifiers", translateModifiers(modifiers))
        sendMessage(msg)
    }

    fun keyEvent(type: EKeyEventType, keyCode: Int, modifiers: Int, nativeKeyData: Any? = null): Boolean {
        val handled = when {
            keyCode < KEY_SPECIAL -> true
            keyCode in HANDLED_KEYS -> true
            else -> false
        }
        if (!handled) return false

        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "key_event")
        msg.setValue("event", when (type) {
            EKeyEventType.KEY_EVENT_DOWN -> "down"
            EKeyEventType.KEY_EVENT_UP -> "up"
            EKeyEventType.KEY_EVENT_REPEAT -> "repeat"
        })
        msg.setValueS32("key", keyCode)
        msg.setValue("modifiers", translateModifiers(modifiers))
        if (nativeKeyData != null) msg.setValueLLSD("native_key_data", nativeKeyData)
        sendMessage(msg)
        return true
    }

    fun scrollEvent(x: Int, y: Int, clicksX: Int, clicksY: Int, modifiers: Int) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "scroll_event")
        msg.setValueS32("x", x)
        msg.setValueS32("y", y)
        msg.setValueS32("clicks_x", clicksX)
        msg.setValueS32("clicks_y", clicksY)
        msg.setValue("modifiers", translateModifiers(modifiers))
        sendMessage(msg)
    }

    fun textInput(text: String, modifiers: Int, nativeKeyData: Any? = null): Boolean {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "text_event")
        msg.setValue("text", text)
        msg.setValue("modifiers", translateModifiers(modifiers))
        if (nativeKeyData != null) msg.setValueLLSD("native_key_data", nativeKeyData)
        sendMessage(msg)
        return true
    }

    fun enableMediaPluginDebugging(enable: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "enable_media_plugin_debugging")
        msg.setValueBoolean("enable", enable)
        sendMessage(msg)
    }

    fun jsEnableObject(enable: Boolean) {
        val p = plugin ?: return
        if (!p.isRunning() || p.isBlocked) return
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "js_enable_object")
        msg.setValueBoolean("enable", enable)
        sendMessage(msg)
    }

    fun jsAgentLocationEvent(x: Double, y: Double, z: Double) {
        val p = plugin ?: return
        if (!p.isRunning() || p.isBlocked) return
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "js_agent_location")
        msg.setValueReal("x", x); msg.setValueReal("y", y); msg.setValueReal("z", z)
        sendMessage(msg)
    }

    fun jsAgentGlobalLocationEvent(x: Double, y: Double, z: Double) {
        val p = plugin ?: return
        if (!p.isRunning() || p.isBlocked) return
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "js_agent_global_location")
        msg.setValueReal("x", x); msg.setValueReal("y", y); msg.setValueReal("z", z)
        sendMessage(msg)
    }

    fun jsAgentOrientationEvent(angle: Double) {
        val p = plugin ?: return
        if (!p.isRunning() || p.isBlocked) return
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "js_agent_orientation")
        msg.setValueReal("angle", angle)
        sendMessage(msg)
    }

    fun jsAgentLanguageEvent(language: String) {
        val p = plugin ?: return
        if (!p.isRunning() || p.isBlocked) return
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "js_agent_language")
        msg.setValue("language", language)
        sendMessage(msg)
    }

    fun jsAgentRegionEvent(region: String) {
        val p = plugin ?: return
        if (!p.isRunning() || p.isBlocked) return
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "js_agent_region")
        msg.setValue("region", region)
        sendMessage(msg)
    }

    fun jsAgentMaturityEvent(maturity: String) {
        val p = plugin ?: return
        if (!p.isRunning() || p.isBlocked) return
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "js_agent_maturity")
        msg.setValue("maturity", maturity)
        sendMessage(msg)
    }

    fun injectOpenIDCookie() {
        if (sOIDcookieName.isNotEmpty() && sOIDcookieValue.isNotEmpty()) {
            setCookie(sOIDcookieUrl, sOIDcookieName, sOIDcookieValue,
                sOIDcookieHost, sOIDcookiePath, sOIDcookieHttpOnly, sOIDcookieSecure)
        }
    }

    fun setCookie(uri: String, name: String, value: String, domain: String,
                  path: String, httponly: Boolean, secure: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "set_cookie")
        msg.setValue("uri", uri)
        msg.setValue("name", name)
        msg.setValue("value", value)
        msg.setValue("domain", domain)
        msg.setValue("path", path)
        msg.setValueBoolean("httponly", httponly)
        msg.setValueBoolean("secure", secure)
        sendMessage(msg)
    }

    fun loadURI(uri: String) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "load_uri")
        msg.setValue("uri", uri)
        sendMessage(msg)
    }

    fun executeJavaScript(code: String) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "execute_javascript")
        msg.setValue("code", code)
        sendMessage(msg)
    }

    fun isPluginLoading(): Boolean = plugin?.isLoading() ?: false
    fun isPluginRunning(): Boolean = plugin?.isRunning() ?: false
    fun isPluginExited(): Boolean = plugin?.isDone() ?: false
    fun getPluginVersion(): String = plugin?.getPluginVersion() ?: ""
    fun getDisableTimeout(): Boolean = plugin?.getDisableTimeout() ?: false
    fun setDisableTimeout(disable: Boolean) { plugin?.setDisableTimeout(disable) }

    fun setPriority(priority: EPriority) {
        if (this.priority != priority) {
            this.priority = priority
            sleepTime = when (priority) {
                EPriority.PRIORITY_UNLOADED,
                EPriority.PRIORITY_STOPPED,
                EPriority.PRIORITY_HIDDEN,
                EPriority.PRIORITY_SLIDESHOW -> 1.0
                EPriority.PRIORITY_LOW -> 1.0 / 25.0
                EPriority.PRIORITY_NORMAL -> 1.0 / 50.0
                EPriority.PRIORITY_HIGH -> 1.0 / 100.0
            }
            val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "set_priority")
            msg.setValue("priority", priorityToString(priority))
            sendMessage(msg)
            plugin?.setSleepTime(sleepTime)
            setSizeInternal()
        }
    }

    fun setLowPrioritySizeLimit(size: Int) {
        val power = nextPowerOf2(size)
        if (lowPrioritySizeLimit != power) {
            lowPrioritySizeLimit = power
            setSizeInternal()
        }
    }

    fun getCPUUsage(): Double = plugin?.getCPUUsage() ?: 0.0

    fun sendPickFileResponse(files: List<String>) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "pick_file_response")
        if (plugin?.isBlocked == true) msg.setValueBoolean("blocking_response", true)
        msg.setValueLLSD("file_list", files)
        sendMessage(msg)
    }

    fun sendAuthResponse(ok: Boolean, username: String, password: String) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "auth_response")
        msg.setValueBoolean("ok", ok)
        msg.setValue("username", username)
        msg.setValue("password", password)
        if (plugin?.isBlocked == true) msg.setValueBoolean("blocking_response", true)
        sendMessage(msg)
    }

    fun getCursorName(): String = cursorName
    fun getStatus(): LLPluginClassMediaOwner.EMediaStatus = status

    fun undo() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "edit_undo")) }
    fun canUndo(): Boolean = canUndo
    fun redo() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "edit_redo")) }
    fun canRedo(): Boolean = canRedo
    fun cut() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "edit_cut")) }
    fun canCut(): Boolean = canCut
    fun copy() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "edit_copy")) }
    fun canCopy(): Boolean = canCopy
    fun paste() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "edit_paste")) }
    fun canPaste(): Boolean = canPaste
    fun doDelete() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "edit_delete")) }
    fun canDoDelete(): Boolean = canDoDelete
    fun selectAll() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "edit_select_all")) }
    fun canSelectAll(): Boolean = canSelectAll
    fun showPageSource() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "edit_show_source")) }

    fun setUserDataPath(userDataPathCache: String, username: String, userDataPathCefLog: String) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "set_user_data_path")
        msg.setValue("cache_path", userDataPathCache)
        msg.setValue("username", username)
        msg.setValue("cef_log_file", userDataPathCefLog)
        sendMessage(msg)
    }

    fun setLanguageCode(languageCode: String) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA, "set_language_code")
        msg.setValue("language", languageCode)
        sendMessage(msg)
    }

    fun setPluginsEnabled(enabled: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "plugins_enabled")
        msg.setValueBoolean("enable", enabled)
        sendMessage(msg)
    }

    fun setJavascriptEnabled(enabled: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "javascript_enabled")
        msg.setValueBoolean("enable", enabled)
        sendMessage(msg)
    }

    fun setWebSecurityDisabled(disabled: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "web_security_disabled")
        msg.setValueBoolean("disabled", disabled)
        sendMessage(msg)
    }

    fun setFileAccessFromFileUrlsEnabled(enabled: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "file_access_from_file_urls")
        msg.setValueBoolean("enabled", enabled)
        sendMessage(msg)
    }

    fun setTarget(target: String) { this.target = target }

    fun pluginSupportsMediaBrowser(): Boolean =
        plugin?.getMessageClassVersion(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER)?.isNotEmpty() ?: false

    fun focus(focused: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "focus")
        msg.setValueBoolean("focused", focused)
        sendMessage(msg)
    }

    fun setPageZoomFactor(factor: Double) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "set_page_zoom_factor")
        msg.setValueReal("factor", factor)
        sendMessage(msg)
    }

    fun clearCache() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "clear_cache")) }
    fun clearCookies() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "clear_cookies")) }

    fun setCookiesEnabled(enable: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "cookies_enabled")
        msg.setValueBoolean("enable", enable)
        sendMessage(msg)
    }

    fun proxySetup(enable: Boolean, host: String = "", port: Int = 0) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "proxy_setup")
        msg.setValueBoolean("enable", enable)
        msg.setValue("host", host)
        msg.setValueS32("port", port)
        sendMessage(msg)
    }

    fun browseStop() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "browse_stop")) }

    fun browseReload(ignoreCache: Boolean = false) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "browse_reload")
        msg.setValueBoolean("ignore_cache", ignoreCache)
        sendMessage(msg)
    }

    fun browseForward() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "browse_forward")) }
    fun browseBack() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "browse_back")) }

    fun setBrowserUserAgent(userAgent: String) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "set_user_agent")
        msg.setValue("user_agent", userAgent)
        sendMessage(msg)
    }

    fun showWebInspector(show: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "show_web_inspector")
        msg.setValueBoolean("show", true)
        sendMessage(msg)
    }

    fun proxyWindowOpened(target: String, uuid: String) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "proxy_window_opened")
        msg.setValue("target", target)
        msg.setValue("uuid", uuid)
        sendMessage(msg)
    }

    fun proxyWindowClosed(uuid: String) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "proxy_window_closed")
        msg.setValue("uuid", uuid)
        sendMessage(msg)
    }

    fun ignoreSslCertErrors(ignore: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "ignore_ssl_cert_errors")
        msg.setValueBoolean("ignore", ignore)
        sendMessage(msg)
    }

    fun addCertificateFilePath(path: String) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "add_certificate_file_path")
        msg.setValue("path", path)
        sendMessage(msg)
    }

    fun getNavigateURI(): String = navigateURI
    fun getNavigateResultCode(): Int = navigateResultCode
    fun getNavigateResultString(): String = navigateResultString
    fun getHistoryBackAvailable(): Boolean = historyBackAvailable
    fun getHistoryForwardAvailable(): Boolean = historyForwardAvailable
    fun getProgressPercent(): Int = progressPercent
    fun getStatusText(): String = statusText
    fun getLocation(): String = location
    fun getClickURL(): String = clickURL
    fun getClickNavType(): String = clickNavType
    fun getClickTarget(): String = clickTarget
    fun getClickUUID(): String = clickUUID
    fun getDebugMessageText(): String = debugMessageText
    fun getDebugMessageLevel(): String = debugMessageLevel
    fun getStatusCode(): Int = statusCode
    fun getGeometryX(): Int = geometryX
    fun getGeometryY(): Int = geometryY
    fun getGeometryWidth(): Int = geometryWidth
    fun getGeometryHeight(): Int = geometryHeight
    fun getAuthURL(): String = authURL
    fun getAuthRealm(): String = authRealm
    fun getIsMultipleFilePick(): Boolean = isMultipleFilePick
    fun getHoverText(): String = hoverText
    fun getHoverLink(): String = hoverLink
    fun getFileDownloadFilename(): String = fileDownloadFilename
    fun getMediaName(): String = mediaName
    fun getMediaDescription(): String = mediaDescription

    fun setOverrideClickTarget(target: String) { clickEnforceTarget = true; overrideClickTarget = target }
    fun resetOverrideClickTarget() { clickEnforceTarget = false }
    fun isOverrideClickTarget(): Boolean = clickEnforceTarget
    fun getOverrideClickTarget(): String = overrideClickTarget

    fun getArtist(): String = artist
    fun getTitle(): String = title

    fun setFlipY(flip: Boolean) {
        TODO("GPU: pass flip-Y flag to CEF texture renderer")
    }

    fun pluginSupportsMediaTime(): Boolean =
        plugin?.getMessageClassVersion(LLPLUGIN_MESSAGE_CLASS_MEDIA_TIME)?.isNotEmpty() ?: false

    fun stop() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_TIME, "stop")) }

    fun start(rate: Float = 0.0f) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_TIME, "start")
        msg.setValueReal("rate", rate.toDouble())
        sendMessage(msg)
    }

    fun pause() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_TIME, "pause")) }

    fun seek(time: Float) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_TIME, "seek")
        msg.setValueReal("time", time.toDouble())
        currentTime = time.toDouble()
        sendMessage(msg)
    }

    fun setLoop(loop: Boolean) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_TIME, "set_loop")
        msg.setValueBoolean("loop", loop)
        sendMessage(msg)
    }

    fun setVolume(volume: Float) {
        if (volume != requestedVolume) {
            requestedVolume = volume
            val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_TIME, "set_volume")
            msg.setValueReal("volume", volume.toDouble())
            sendMessage(msg)
        }
    }

    fun getVolume(): Float = requestedVolume
    fun getCurrentTime(): Double = currentTime
    fun getDuration(): Double = duration
    fun getCurrentPlayRate(): Double = currentRate
    fun getLoadedDuration(): Double = loadedDuration

    fun initializeUrlHistory(urlHistory: Any?) {
        val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER, "init_history")
        msg.setValueLLSD("history", urlHistory)
        sendMessage(msg)
    }

    fun crashPlugin() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_INTERNAL, "crash")) }
    fun hangPlugin() { sendMessage(LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_INTERNAL, "hang")) }

    fun setDeleteOK(flag: Boolean) { deleteOK = flag }

    override fun receivePluginMessage(message: LLPluginMessage) {
        val messageClass = message.getClass()
        when (messageClass) {
            LLPLUGIN_MESSAGE_CLASS_MEDIA -> handleMediaMessage(message)
            LLPLUGIN_MESSAGE_CLASS_MEDIA_BROWSER -> handleMediaBrowserMessage(message)
            LLPLUGIN_MESSAGE_CLASS_MEDIA_TIME -> { /* no incoming messages defined yet */ }
        }
    }

    override fun pluginLaunchFailed() {
        mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_PLUGIN_FAILED_LAUNCH)
    }

    override fun pluginDied() {
        mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_PLUGIN_FAILED)
    }

    private fun handleMediaMessage(message: LLPluginMessage) {
        when (val name = message.getName()) {
            "ndMediadata_change" -> {
                title = message.getValue("title")
                artist = message.getValue("artist")
            }
            "texture_params" -> {
                requestedTextureDepth = message.getValueS32("depth")
                requestedTextureInternalFormat = message.getValueU32("internalformat")
                requestedTextureFormat = message.getValueU32("format")
                requestedTextureType = message.getValueU32("type")
                requestedTextureSwapBytes = message.getValueBoolean("swap_bytes")
                requestedTextureCoordsOpenGL = message.getValueBoolean("coords_opengl")
                defaultMediaWidth = message.getValueS32("default_width")
                defaultMediaHeight = message.getValueS32("default_height")
                allowDownsample = message.getValueBoolean("allow_downsample")
                padding = message.getValueS32("padding")
                setSizeInternal()
                textureParamsReceived = true
            }
            "updated" -> {
                if (message.hasValue("left")) {
                    var newLeft = message.getValueS32("left")
                    var newTop = message.getValueS32("top")
                    var newRight = message.getValueS32("right")
                    var newBottom = message.getValueS32("bottom")
                    if (newTop < newBottom) { val tmp = newTop; newTop = newBottom; newBottom = tmp }
                    if (dirtyRectEmpty) {
                        dirtyLeft = newLeft; dirtyTop = newTop
                        dirtyRight = newRight; dirtyBottom = newBottom
                        dirtyRectEmpty = false
                    } else {
                        dirtyLeft = minOf(dirtyLeft, newLeft)
                        dirtyTop = maxOf(dirtyTop, newTop)
                        dirtyRight = maxOf(dirtyRight, newRight)
                        dirtyBottom = minOf(dirtyBottom, newBottom)
                    }
                    mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_CONTENT_UPDATED)
                }

                var timeDurationUpdated = false
                val previousPercent = progressPercent

                if (message.hasValue("current_time")) { currentTime = message.getValueReal("current_time"); timeDurationUpdated = true }
                if (message.hasValue("duration")) { duration = message.getValueReal("duration"); timeDurationUpdated = true }
                if (message.hasValue("current_rate")) { currentRate = message.getValueReal("current_rate") }
                if (message.hasValue("loaded_duration")) {
                    loadedDuration = message.getValueReal("loaded_duration")
                    timeDurationUpdated = true
                } else {
                    loadedDuration = duration
                }

                if (duration != 0.0) progressPercent = ((loadedDuration * 100.0) / duration).toInt()

                if (timeDurationUpdated) mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_TIME_DURATION_UPDATED)
                if (previousPercent != progressPercent) mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_PROGRESS_UPDATED)
            }
            "media_status" -> {
                status = when (message.getValue("status")) {
                    "loading" -> LLPluginClassMediaOwner.EMediaStatus.MEDIA_LOADING
                    "loaded" -> LLPluginClassMediaOwner.EMediaStatus.MEDIA_LOADED
                    "error" -> LLPluginClassMediaOwner.EMediaStatus.MEDIA_ERROR
                    "playing" -> LLPluginClassMediaOwner.EMediaStatus.MEDIA_PLAYING
                    "paused" -> LLPluginClassMediaOwner.EMediaStatus.MEDIA_PAUSED
                    "done" -> LLPluginClassMediaOwner.EMediaStatus.MEDIA_DONE
                    else -> LLPluginClassMediaOwner.EMediaStatus.MEDIA_NONE
                }
            }
            "size_change_request" -> {
                naturalMediaWidth = message.getValueS32("width")
                naturalMediaHeight = message.getValueS32("height")
                setSizeInternal()
            }
            "size_change_response" -> {
                textureWidth = message.getValueS32("texture_width")
                textureHeight = message.getValueS32("texture_height")
                mediaWidth = message.getValueS32("width")
                mediaHeight = message.getValueS32("height")
                resetDirty()
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_SIZE_CHANGED)
            }
            "cursor_changed" -> {
                cursorName = message.getValue("name")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_CURSOR_CHANGED)
            }
            "edit_state" -> {
                if (message.hasValue("undo")) canUndo = message.getValueBoolean("undo")
                if (message.hasValue("redo")) canRedo = message.getValueBoolean("redo")
                if (message.hasValue("cut")) canCut = message.getValueBoolean("cut")
                if (message.hasValue("copy")) canCopy = message.getValueBoolean("copy")
                if (message.hasValue("paste")) canPaste = message.getValueBoolean("paste")
                if (message.hasValue("delete")) canDoDelete = message.getValueBoolean("delete")
                if (message.hasValue("select_all")) canSelectAll = message.getValueBoolean("select_all")
            }
            "name_text" -> {
                historyBackAvailable = message.getValueBoolean("history_back_available")
                historyForwardAvailable = message.getValueBoolean("history_forward_available")
                mediaName = message.getValue("name")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_NAME_CHANGED)
            }
            "pick_file" -> {
                isMultipleFilePick = message.getValueBoolean("multiple_files")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_PICK_FILE_REQUEST)
            }
            "auth_request" -> {
                authURL = message.getValue("url")
                authRealm = message.getValue("realm")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_AUTH_REQUEST)
            }
            "file_download" -> {
                fileDownloadFilename = message.getValue("filename")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_FILE_DOWNLOAD)
            }
            "debug_message" -> {
                debugMessageText = message.getValue("message_text")
                debugMessageLevel = message.getValue("message_level")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_DEBUG_MESSAGE)
            }
            "tooltip_text" -> {
                hoverText = message.getValue("tooltip")
            }
        }
    }

    private fun handleMediaBrowserMessage(message: LLPluginMessage) {
        when (message.getName()) {
            "navigate_begin" -> {
                navigateURI = message.getValue("uri")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_NAVIGATE_BEGIN)
            }
            "navigate_complete" -> {
                navigateURI = message.getValue("uri")
                navigateResultCode = message.getValueS32("result_code")
                navigateResultString = message.getValue("result_string")
                historyBackAvailable = message.getValueBoolean("history_back_available")
                historyForwardAvailable = message.getValueBoolean("history_forward_available")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_NAVIGATE_COMPLETE)
            }
            "progress" -> {
                progressPercent = message.getValueS32("percent")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_PROGRESS_UPDATED)
            }
            "status_text" -> {
                statusText = message.getValue("status")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_STATUS_TEXT_CHANGED)
            }
            "location_changed" -> {
                location = message.getValue("uri")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_LOCATION_CHANGED)
            }
            "click_href" -> {
                clickURL = message.getValue("uri")
                clickTarget = message.getValue("target")
                clickUUID = generateUUID()
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_CLICK_LINK_HREF)
            }
            "click_nofollow" -> {
                clickURL = message.getValue("uri")
                clickNavType = message.getValue("nav_type")
                clickTarget = ""
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_CLICK_LINK_NOFOLLOW)
            }
            "navigate_error_page" -> {
                statusCode = message.getValueS32("status_code")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_NAVIGATE_ERROR_PAGE)
            }
            "close_request" -> mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_CLOSE_REQUEST)
            "geometry_change" -> {
                clickUUID = message.getValue("uuid")
                geometryX = message.getValueS32("x")
                geometryY = message.getValueS32("y")
                geometryWidth = message.getValueS32("width")
                geometryHeight = message.getValueS32("height")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_GEOMETRY_CHANGE)
            }
            "link_hovered" -> {
                hoverLink = message.getValue("link")
                hoverText = message.getValue("title")
                mediaEvent(LLPluginClassMediaOwner.EMediaEvent.MEDIA_EVENT_LINK_HOVERED)
            }
        }
    }

    private fun mediaEvent(event: LLPluginClassMediaOwner.EMediaEvent) {
        owner?.handleMediaEvent(this, event)
    }

    private fun sendMessage(message: LLPluginMessage) {
        val p = plugin
        if (p != null && p.isRunning()) {
            p.sendMessage(message)
        } else {
            sendQueue.addLast(message)
        }
    }

    private fun setSizeInternal() {
        requestedMediaWidth = when {
            setMediaWidth > 0 && setMediaHeight > 0 -> { requestedMediaHeight = setMediaHeight; setMediaWidth }
            naturalMediaWidth > 0 && naturalMediaHeight > 0 -> { requestedMediaHeight = naturalMediaHeight; naturalMediaWidth }
            else -> { requestedMediaHeight = defaultMediaHeight; defaultMediaWidth }
        }

        fullMediaWidth = requestedMediaWidth
        fullMediaHeight = requestedMediaHeight

        if (allowDownsample) {
            when (priority) {
                EPriority.PRIORITY_SLIDESHOW, EPriority.PRIORITY_LOW -> {
                    while (requestedMediaWidth > lowPrioritySizeLimit || requestedMediaHeight > lowPrioritySizeLimit) {
                        requestedMediaWidth /= 2
                        requestedMediaHeight /= 2
                    }
                }
                else -> {}
            }
        }

        if (autoScaleMedia) {
            requestedMediaWidth = nextPowerOf2(requestedMediaWidth)
            requestedMediaHeight = nextPowerOf2(requestedMediaHeight)
        }

        if (requestedMediaWidth > 2048) requestedMediaWidth = 2048
        if (requestedMediaHeight > 2048) requestedMediaHeight = 2048
    }

    private fun translateModifiers(modifiers: Int): String {
        var result = ""
        if (modifiers and MASK_CONTROL != 0) result += "control|"
        if (modifiers and MASK_ALT != 0) result += "alt|"
        if (modifiers and MASK_SHIFT != 0) result += "shift|"
        return result
    }

    companion object {
        var sOIDcookieUrl: String = ""
        var sOIDcookieName: String = ""
        var sOIDcookieValue: String = ""
        var sOIDcookieHost: String = ""
        var sOIDcookiePath: String = ""
        var sOIDcookieHttpOnly: Boolean = false
        var sOIDcookieSecure: Boolean = false

        fun storeOpenIDCookie(url: String, name: String, value: String,
                              host: String, path: String, httponly: Boolean, secure: Boolean) {
            sOIDcookieUrl = url; sOIDcookieName = name; sOIDcookieValue = value
            sOIDcookieHost = host; sOIDcookiePath = path
            sOIDcookieHttpOnly = httponly; sOIDcookieSecure = secure
        }

        fun priorityToString(priority: EPriority): String = when (priority) {
            EPriority.PRIORITY_UNLOADED -> "unloaded"
            EPriority.PRIORITY_STOPPED -> "stopped"
            EPriority.PRIORITY_HIDDEN -> "hidden"
            EPriority.PRIORITY_SLIDESHOW -> "slideshow"
            EPriority.PRIORITY_LOW -> "low"
            EPriority.PRIORITY_NORMAL -> "normal"
            EPriority.PRIORITY_HIGH -> "high"
        }

        private fun generateUUID(): String = java.util.UUID.randomUUID().toString()

        const val MASK_CONTROL = 0x01
        const val MASK_ALT = 0x02
        const val MASK_SHIFT = 0x04

        const val KEY_SPECIAL = 0x80
        private val HANDLED_KEYS = setOf(
            0x08, 0x09, 0x0D, 0x10, 0x11, 0x12, 0x14, 0x1B,
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28,
            0x2D, 0x2E
        )
    }
}
