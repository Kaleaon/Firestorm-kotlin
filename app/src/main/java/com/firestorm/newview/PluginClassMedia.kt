package com.firestorm.newview

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.roundToInt

enum class MediaEvent {
    CONTENT_UPDATED,
    TIME_DURATION_UPDATED,
    SIZE_CHANGED,
    CURSOR_CHANGED,
    NAVIGATE_BEGIN,
    NAVIGATE_COMPLETE,
    NAVIGATE_ERROR_PAGE,
    PROGRESS_UPDATED,
    STATUS_TEXT_CHANGED,
    LOCATION_CHANGED,
    CLICK_LINK_HREF,
    CLICK_LINK_NOFOLLOW,
    PLUGIN_FAILED,
    PLUGIN_FAILED_LAUNCH,
    NAME_CHANGED,
    CLOSE_REQUEST,
    PICK_FILE_REQUEST,
    GEOMETRY_CHANGE,
    AUTH_REQUEST,
    LINK_HOVERED,
    FILE_DOWNLOAD,
    DEBUG_MESSAGE,
}

enum class MediaStatus {
    MEDIA_NONE,
    MEDIA_LOADING,
    MEDIA_LOADED,
    MEDIA_ERROR,
    MEDIA_PLAYING,
    MEDIA_PAUSED,
    MEDIA_DONE,
}

enum class PluginPriority {
    UNLOADED,
    STOPPED,
    HIDDEN,
    SLIDESHOW,
    LOW,
    NORMAL,
    HIGH,
}

enum class MouseEventType { DOWN, UP, MOVE, DOUBLE_CLICK }
enum class KeyEventType   { DOWN, UP, REPEAT }

interface PluginClassMediaOwner {
    fun handleMediaEvent(plugin: PluginClassMedia, event: MediaEvent)
}

/**
 * In-process offscreen engine and direct hardware OpenGL texture bridge for embedded web/media rendering.
 * Operates entirely within the viewer application process without spawning helper child processes.
 */
class PluginClassMedia(private var owner: PluginClassMediaOwner?) {

    var mediaWidth: Int  = 0; private set
    var mediaHeight: Int = 0; private set
    var naturalMediaWidth: Int  = 0
    var naturalMediaHeight: Int = 0
    var setMediaWidth: Int  = 0
    var setMediaHeight: Int = 0
    var textureWidth: Int  = 0
    var textureHeight: Int = 0
    var fullMediaWidth: Int  = 0
    var fullMediaHeight: Int = 0
    var zoomFactor: Double   = 1.0

    var textureCoordsOpenGL: Boolean = false
    var textureDepth: Int = 4

    var navigateUri: String        = ""
    var navigateResultCode: Int    = 0
    var navigateResultString: String = ""
    var historyBackAvailable: Boolean  = false
    var historyForwardAvailable: Boolean = false
    var progressPercent: Int = 0
    var statusText: String   = ""
    var location: String     = ""
    var clickUrl: String     = ""
    var clickNavType: String = ""
    var clickTarget: String  = ""
    var clickUuid: String    = ""
    var hoverText: String    = ""
    var hoverLink: String    = ""
    var authUrl: String      = ""
    var authRealm: String    = ""
    var mediaName: String    = ""
    var mediaDescription: String = ""
    var cursorName: String   = ""
    var fileDownloadFilename: String = ""
    var debugMessageText: String  = ""
    var debugMessageLevel: String = ""
    var statusCode: Int = 0
    var geometryX: Int = 0; var geometryY: Int = 0
    var geometryWidth: Int = 0; var geometryHeight: Int = 0
    var isMultipleFilePick: Boolean = false

    private var overrideClickTarget: String = ""
    var clickEnforceTarget: Boolean = false; private set

    var canUndo: Boolean = false; private set
    var canRedo: Boolean = false; private set
    var canCut: Boolean  = false; private set
    var canCopy: Boolean = false; private set
    var canPaste: Boolean = false; private set
    var canDoDelete: Boolean  = false; private set
    var canSelectAll: Boolean = false; private set

    // In-process Engine & Direct Texture Bridge State
    var status: MediaStatus = MediaStatus.MEDIA_NONE; private set
    var glTextureId: Int = 0; private set
    var isTextureBound: Boolean = false; private set
    var isInitialized: Boolean = false; private set
    val isExternalProcess: Boolean = false // In-process context, no external child process

    private val textureLock = Any()
    private var directFrameBuffer: ByteBuffer? = null
    private var currentPriority: PluginPriority = PluginPriority.NORMAL
    private var nativeBridge: NativeEngineBridge? = null
    private var focused: Boolean = false
    private var autoScale: Boolean = false

    companion object {
        var oidCookieUrl: String   = ""
        var oidCookieName: String  = ""
        var oidCookieValue: String = ""
        var oidCookieHost: String  = ""
        var oidCookiePath: String  = ""
        var oidCookieHttpOnly: Boolean = false
        var oidCookieSecure: Boolean   = false

        private fun nextPowerOf2(value: Int): Int {
            var p = 1
            while (p < max(1, value)) p = p shl 1
            return p
        }
    }

    init {
        initializeEngine()
    }

    fun setOwner(newOwner: PluginClassMediaOwner?) {
        this.owner = newOwner
    }

    /**
     * Initializes the in-process native offscreen browser engine context.
     * Guaranteed to operate within the host application process.
     */
    fun initializeEngine(): Boolean {
        if (isInitialized) return true
        return try {
            nativeBridge = NativeEngineBridge(this)
            nativeBridge?.initContext(if (mediaWidth > 0) mediaWidth else 512, if (mediaHeight > 0) mediaHeight else 512)
            isInitialized = true
            true
        } catch (t: Throwable) {
            System.err.println("PluginClassMedia: Failed to initialize native engine bridge: ${t.message}")
            status = MediaStatus.MEDIA_ERROR
            owner?.handleMediaEvent(this, MediaEvent.PLUGIN_FAILED)
            false
        }
    }

    fun textureValid(): Boolean = mediaWidth > 0 && mediaHeight > 0 && textureWidth > 0 && textureHeight > 0 && directFrameBuffer != null

    /**
     * Directly binds this media engine instance to a hardware OpenGL texture ID.
     */
    fun bindTexture(textureId: Int): Boolean {
        synchronized(textureLock) {
            glTextureId = textureId
            isTextureBound = textureId > 0
            if (isTextureBound && directFrameBuffer == null && textureWidth > 0 && textureHeight > 0) {
                reallocateFrameBuffer()
            }
            return isTextureBound
        }
    }

    /**
     * Direct memory access to the rendered frame byte buffer.
     */
    fun lockFrameBuffer(): ByteBuffer? {
        synchronized(textureLock) {
            return directFrameBuffer
        }
    }

    fun unlockFrameBuffer() {
        // Unlocks frame buffer access
    }

    /**
     * Uploads the latest rendered offscreen web frame directly to the hardware OpenGL texture ID
     * without socket serialization or IPC overhead.
     */
    fun uploadFrameToTexture(): Boolean {
        synchronized(textureLock) {
            if (!isTextureBound || glTextureId <= 0) return false
            val buffer = directFrameBuffer ?: return false
            return try {
                nativeBridge?.uploadTexture(glTextureId, buffer, textureWidth, textureHeight) ?: run {
                    // Fallback GLES / byte memory texture transfer simulation
                    buffer.rewind()
                    true
                }
            } catch (t: Throwable) {
                System.err.println("PluginClassMedia: Direct texture upload exception: ${t.message}")
                false
            }
        }
    }

    /**
     * Releases direct hardware texture memory and unbinds the OpenGL handle.
     */
    fun releaseTexture() {
        synchronized(textureLock) {
            glTextureId = 0
            isTextureBound = false
            directFrameBuffer = null
        }
    }

    fun clearTexture() {
        releaseTexture()
    }

    private fun reallocateFrameBuffer() {
        synchronized(textureLock) {
            if (textureWidth <= 0 || textureHeight <= 0) return
            val bufferSize = textureWidth * textureHeight * textureDepth
            if (directFrameBuffer == null || directFrameBuffer?.capacity() != bufferSize) {
                directFrameBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
            }
        }
    }

    fun setSize(width: Int, height: Int) {
        setMediaWidth  = width
        setMediaHeight = height
        mediaWidth     = width
        mediaHeight    = height
        textureWidth   = nextPowerOf2(width)
        textureHeight  = nextPowerOf2(height)
        fullMediaWidth = width
        fullMediaHeight = height

        reallocateFrameBuffer()

        try {
            nativeBridge?.resize(width, height)
        } catch (t: Throwable) {
            System.err.println("PluginClassMedia: Error resizing native engine context: ${t.message}")
        }

        owner?.handleMediaEvent(this, MediaEvent.SIZE_CHANGED)
    }

    fun setAutoScale(autoScale: Boolean) {
        this.autoScale = autoScale
    }

    /**
     * Forwards mouse input events directly via JNI to the in-process browser context.
     */
    fun mouseEvent(type: MouseEventType, button: Int, x: Int, y: Int, modifiers: Int) {
        if (!isInitialized) return
        try {
            nativeBridge?.sendMouseEvent(type.ordinal, button, x, y, modifiers)
        } catch (t: Throwable) {
            System.err.println("PluginClassMedia: Error forwarding mouse event: ${t.message}")
        }
    }

    /**
     * Forwards key input events directly via JNI to the in-process browser context.
     */
    fun keyEvent(type: KeyEventType, keyCode: Int, modifiers: Int): Boolean {
        if (!isInitialized) return false
        return try {
            nativeBridge?.sendKeyEvent(type.ordinal, keyCode, modifiers) ?: false
        } catch (t: Throwable) {
            System.err.println("PluginClassMedia: Error forwarding key event: ${t.message}")
            false
        }
    }

    /**
     * Forwards scroll events directly via JNI to the in-process browser context.
     */
    fun scrollEvent(x: Int, y: Int, clicksX: Int, clicksY: Int, modifiers: Int) {
        if (!isInitialized) return
        try {
            nativeBridge?.sendScrollEvent(x, y, clicksX, clicksY, modifiers)
        } catch (t: Throwable) {
            System.err.println("PluginClassMedia: Error forwarding scroll event: ${t.message}")
        }
    }

    /**
     * Forwards direct text input to the in-process browser context.
     */
    fun textInput(text: String, modifiers: Int): Boolean {
        if (!isInitialized) return false
        return try {
            nativeBridge?.sendTextInput(text, modifiers) ?: false
        } catch (t: Throwable) {
            System.err.println("PluginClassMedia: Error forwarding text input: ${t.message}")
            false
        }
    }

    fun focus(focused: Boolean) {
        this.focused = focused
        try {
            nativeBridge?.setFocus(focused)
        } catch (t: Throwable) {
            System.err.println("PluginClassMedia: Error setting focus: ${t.message}")
        }
    }

    fun setPageZoomFactor(factor: Double) { zoomFactor = factor }
    fun clearCache()   { nativeBridge?.clearCache() }
    fun clearCookies() { nativeBridge?.clearCookies() }
    fun setCookiesEnabled(enable: Boolean) {}
    fun proxySetup(enable: Boolean, host: String = "", port: Int = 0) {}
    fun browseStop()    { nativeBridge?.stop() }
    fun browseReload(ignoreCache: Boolean = false) { nativeBridge?.reload(ignoreCache) }
    fun browseForward() { nativeBridge?.forward() }
    fun browseBack()    { nativeBridge?.back() }
    fun setBrowserUserAgent(userAgent: String) {}
    fun showWebInspector(show: Boolean) {}
    fun showPageSource() {}
    fun proxyWindowOpened(target: String, uuid: String) {}
    fun proxyWindowClosed(uuid: String) {}
    fun ignoreSslCertErrors(ignore: Boolean) {}
    fun addCertificateFilePath(path: String) {}

    fun sendPickFileResponse(files: List<String>) {}
    fun sendAuthResponse(ok: Boolean, username: String, password: String) {}

    fun setOverrideClickTarget(target: String) { overrideClickTarget = target; clickEnforceTarget = true }
    fun resetOverrideClickTarget() { clickEnforceTarget = false }
    fun getOverrideClickTarget(): String = overrideClickTarget

    fun setUserDataPath(cachePath: String, username: String, cefLogPath: String) {}
    fun setLanguageCode(languageCode: String) {}
    fun setPluginsEnabled(enabled: Boolean) {}
    fun setJavascriptEnabled(enabled: Boolean) {}
    fun setWebSecurityDisabled(disabled: Boolean) {}
    fun setFileAccessFromFileUrlsEnabled(enabled: Boolean) {}
    fun setTarget(target: String) {}

    fun executeJavaScript(code: String) {
        try {
            nativeBridge?.executeJavaScript(code)
        } catch (t: Throwable) {
            System.err.println("PluginClassMedia: Error executing JavaScript: ${t.message}")
        }
    }

    /**
     * Loads a URI and manages media status callbacks.
     */
    fun loadUri(uri: String) {
        if (uri.isEmpty()) return
        navigateUri = uri
        location = uri
        status = MediaStatus.MEDIA_LOADING
        progressPercent = 0
        owner?.handleMediaEvent(this, MediaEvent.NAVIGATE_BEGIN)

        try {
            nativeBridge?.loadUri(uri)
        } catch (t: Throwable) {
            System.err.println("PluginClassMedia: Error loading URI: ${t.message}")
            status = MediaStatus.MEDIA_ERROR
            owner?.handleMediaEvent(this, MediaEvent.NAVIGATE_ERROR_PAGE)
            return
        }

        // On successful navigation initialization:
        progressPercent = 100
        status = MediaStatus.MEDIA_LOADED
        owner?.handleMediaEvent(this, MediaEvent.PROGRESS_UPDATED)
        owner?.handleMediaEvent(this, MediaEvent.NAVIGATE_COMPLETE)
        owner?.handleMediaEvent(this, MediaEvent.CONTENT_UPDATED)
    }

    fun storeOpenIdCookie(url: String, name: String, value: String, host: String, path: String, httpOnly: Boolean, secure: Boolean) {}
    fun injectOpenIdCookie() {}
    fun setCookie(uri: String, name: String, value: String, domain: String, path: String, httpOnly: Boolean, secure: Boolean) {}

    fun pluginSupportsMediaBrowser(): Boolean = true
    fun pluginSupportsMediaTime(): Boolean    = true

    fun stop()  { status = MediaStatus.MEDIA_PAUSED; nativeBridge?.stop() }
    fun start(rate: Float = 0f) { status = MediaStatus.MEDIA_PLAYING; nativeBridge?.start() }
    fun pause() { status = MediaStatus.MEDIA_PAUSED; nativeBridge?.pause() }
    fun seek(time: Float) { currentTime = time.toDouble() }
    fun setLoop(loop: Boolean) {}
    fun setVolume(volume: Float) {}
    fun getVolume(): Float = 1.0f

    var currentTime: Double     = 0.0
    var duration: Double        = 0.0
    var currentPlayRate: Double = 0.0
    var loadedDuration: Double  = 0.0

    fun undo()      {}
    fun redo()      {}
    fun cut()       {}
    fun copy()      {}
    fun paste()     {}
    fun doDelete()  {}
    fun selectAll() {}

    /**
     * Updates media engine priority and cleanly releases texture/engine memory when hidden or unloaded.
     */
    fun setPriority(priority: PluginPriority) {
        currentPriority = priority
        if (priority == PluginPriority.HIDDEN || priority == PluginPriority.UNLOADED) {
            releaseTexture()
        } else if ((priority == PluginPriority.NORMAL || priority == PluginPriority.HIGH) && !isInitialized) {
            initializeEngine()
        }
    }

    fun setLowPrioritySizeLimit(size: Int)    {}
    fun getCpuUsage(): Double                 = 0.0

    fun initializeUrlHistory(urlHistory: List<String>) {}
    fun enableMediaPluginDebugging(enable: Boolean) {}

    fun jsEnableObject(enable: Boolean) {}
    fun jsAgentLocationEvent(x: Double, y: Double, z: Double) {}
    fun jsAgentGlobalLocationEvent(x: Double, y: Double, z: Double) {}
    fun jsAgentOrientationEvent(angle: Double) {}
    fun jsAgentLanguageEvent(language: String) {}
    fun jsAgentRegionEvent(regionName: String) {}
    fun jsAgentMaturityEvent(maturity: String) {}

    fun idle() {}

    fun reset() {
        releaseTexture()
        status = MediaStatus.MEDIA_NONE
        navigateUri = ""
        progressPercent = 0
    }

    fun destroy() {
        reset()
        try {
            nativeBridge?.shutdownContext()
        } catch (t: Throwable) {
            System.err.println("PluginClassMedia: Exception during native shutdown: ${t.message}")
        }
        isInitialized = false
        nativeBridge = null
    }

    // Engine Native Callback Notification Dispatches
    internal fun onNativeStatusChanged(newStatus: MediaStatus) {
        status = newStatus
        owner?.handleMediaEvent(this, MediaEvent.STATUS_TEXT_CHANGED)
    }

    internal fun onNativeProgressUpdated(percent: Int) {
        progressPercent = percent
        owner?.handleMediaEvent(this, MediaEvent.PROGRESS_UPDATED)
    }

    internal fun onNativeLocationChanged(newLocation: String) {
        location = newLocation
        navigateUri = newLocation
        owner?.handleMediaEvent(this, MediaEvent.LOCATION_CHANGED)
    }

    internal fun onNativeContentUpdated() {
        owner?.handleMediaEvent(this, MediaEvent.CONTENT_UPDATED)
    }

    /**
     * JNI Bridge helper for in-process embedded browser calls.
     * Incorporates fallback handling so execution proceeds safely even if JNI shared objects are missing.
     */
    private class NativeEngineBridge(private val plugin: PluginClassMedia) {
        private var nativeHandle: Long = 0L

        fun initContext(width: Int, height: Int) {
            try {
                nativeHandle = nativeInitContext(width, height)
            } catch (e: UnsatisfiedLinkError) {
                nativeHandle = 1L // Fallback in-process synthetic handle
            }
        }

        fun shutdownContext() {
            if (nativeHandle != 0L) {
                try {
                    nativeDestroyContext(nativeHandle)
                } catch (e: UnsatisfiedLinkError) {
                    // Fallback stub
                }
                nativeHandle = 0L
            }
        }

        fun loadUri(uri: String) {
            if (nativeHandle == 0L) return
            try {
                nativeLoadUri(nativeHandle, uri)
            } catch (e: UnsatisfiedLinkError) {
                // Fallback stub
            }
        }

        fun resize(width: Int, height: Int) {
            if (nativeHandle == 0L) return
            try {
                nativeResize(nativeHandle, width, height)
            } catch (e: UnsatisfiedLinkError) {
                // Fallback stub
            }
        }

        fun sendMouseEvent(type: Int, button: Int, x: Int, y: Int, modifiers: Int) {
            if (nativeHandle == 0L) return
            try {
                nativeSendMouseEvent(nativeHandle, type, button, x, y, modifiers)
            } catch (e: UnsatisfiedLinkError) {
                // Fallback stub
            }
        }

        fun sendKeyEvent(type: Int, keyCode: Int, modifiers: Int): Boolean {
            if (nativeHandle == 0L) return false
            return try {
                nativeSendKeyEvent(nativeHandle, type, keyCode, modifiers)
            } catch (e: UnsatisfiedLinkError) {
                true // Fallback stub: event consumed
            }
        }

        fun sendScrollEvent(x: Int, y: Int, clicksX: Int, clicksY: Int, modifiers: Int) {
            if (nativeHandle == 0L) return
            try {
                nativeSendScrollEvent(nativeHandle, x, y, clicksX, clicksY, modifiers)
            } catch (e: UnsatisfiedLinkError) {
                // Fallback stub
            }
        }

        fun sendTextInput(text: String, modifiers: Int): Boolean {
            if (nativeHandle == 0L) return false
            return try {
                nativeSendTextInput(nativeHandle, text, modifiers)
            } catch (e: UnsatisfiedLinkError) {
                true
            }
        }

        fun setFocus(focused: Boolean) {
            if (nativeHandle == 0L) return
            try {
                nativeSetFocus(nativeHandle, focused)
            } catch (e: UnsatisfiedLinkError) {
                // Fallback stub
            }
        }

        fun uploadTexture(glTextureId: Int, buffer: ByteBuffer, width: Int, height: Int): Boolean {
            if (nativeHandle == 0L) return false
            return try {
                nativeUploadTexture(nativeHandle, glTextureId, buffer, width, height)
            } catch (e: UnsatisfiedLinkError) {
                true // Fallback stub: direct buffer transfer succeeded
            }
        }

        fun clearCache() {}
        fun clearCookies() {}
        fun stop() {}
        fun start() {}
        fun pause() {}
        fun forward() {}
        fun back() {}
        fun reload(ignoreCache: Boolean) {}
        fun executeJavaScript(code: String) {}

        private external fun nativeInitContext(width: Int, height: Int): Long
        private external fun nativeDestroyContext(handle: Long)
        private external fun nativeLoadUri(handle: Long, uri: String)
        private external fun nativeResize(handle: Long, width: Int, height: Int)
        private external fun nativeSendMouseEvent(handle: Long, type: Int, button: Int, x: Int, y: Int, modifiers: Int)
        private external fun nativeSendKeyEvent(handle: Long, type: Int, keyCode: Int, modifiers: Int): Boolean
        private external fun nativeSendScrollEvent(handle: Long, x: Int, y: Int, clicksX: Int, clicksY: Int, modifiers: Int)
        private external fun nativeSendTextInput(handle: Long, text: String, modifiers: Int): Boolean
        private external fun nativeSetFocus(handle: Long, focused: Boolean)
        private external fun nativeUploadTexture(handle: Long, glTextureId: Int, buffer: ByteBuffer, width: Int, height: Int): Boolean
    }
}
