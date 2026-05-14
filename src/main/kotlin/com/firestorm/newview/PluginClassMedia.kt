package com.firestorm.newview

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
 * JVM-side stub for the CEF/browser plugin channel (LLPluginClassMedia).
 * All wire-protocol and shared-memory operations are replaced with TODO stubs.
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

    companion object {
        var oidCookieUrl: String   = ""
        var oidCookieName: String  = ""
        var oidCookieValue: String = ""
        var oidCookieHost: String  = ""
        var oidCookiePath: String  = ""
        var oidCookieHttpOnly: Boolean = false
        var oidCookieSecure: Boolean   = false
    }

    fun textureValid(): Boolean = mediaWidth > 0 && mediaHeight > 0 && textureWidth > 0 && textureHeight > 0

    fun setSize(width: Int, height: Int) {
        setMediaWidth  = width
        setMediaHeight = height
        System.err.println("PluginClassMedia: setSize not yet implemented")
    }

    fun setAutoScale(autoScale: Boolean) { System.err.println("PluginClassMedia: setAutoScale not yet implemented") }

    fun mouseEvent(type: MouseEventType, button: Int, x: Int, y: Int, modifiers: Int) {
        System.err.println("PluginClassMedia: mouseEvent not yet implemented")
    }

    fun keyEvent(type: KeyEventType, keyCode: Int, modifiers: Int): Boolean {
        System.err.println("PluginClassMedia: keyEvent not yet implemented")
        return false
    }

    fun scrollEvent(x: Int, y: Int, clicksX: Int, clicksY: Int, modifiers: Int) {
        System.err.println("PluginClassMedia: scrollEvent not yet implemented")
    }

    fun textInput(text: String, modifiers: Int): Boolean {
        System.err.println("PluginClassMedia: textInput not yet implemented")
        return false
    }

    fun focus(focused: Boolean) { System.err.println("PluginClassMedia: focus not yet implemented") }
    fun setPageZoomFactor(factor: Double) { zoomFactor = factor; System.err.println("PluginClassMedia: setPageZoomFactor not yet implemented") }
    fun clearCache()   { System.err.println("PluginClassMedia: clearCache not yet implemented") }
    fun clearCookies() { System.err.println("PluginClassMedia: clearCookies not yet implemented") }
    fun setCookiesEnabled(enable: Boolean) { System.err.println("PluginClassMedia: setCookiesEnabled not yet implemented") }
    fun proxySetup(enable: Boolean, host: String = "", port: Int = 0) { System.err.println("PluginClassMedia: proxySetup not yet implemented") }
    fun browseStop()    { System.err.println("PluginClassMedia: browseStop not yet implemented") }
    fun browseReload(ignoreCache: Boolean = false) { System.err.println("PluginClassMedia: browseReload not yet implemented") }
    fun browseForward() { System.err.println("PluginClassMedia: browseForward not yet implemented") }
    fun browseBack()    { System.err.println("PluginClassMedia: browseBack not yet implemented") }
    fun setBrowserUserAgent(userAgent: String) { System.err.println("PluginClassMedia: setBrowserUserAgent not yet implemented") }
    fun showWebInspector(show: Boolean) { System.err.println("PluginClassMedia: showWebInspector not yet implemented") }
    fun showPageSource() { System.err.println("PluginClassMedia: showPageSource not yet implemented") }
    fun proxyWindowOpened(target: String, uuid: String) { System.err.println("PluginClassMedia: proxyWindowOpened not yet implemented") }
    fun proxyWindowClosed(uuid: String) { System.err.println("PluginClassMedia: proxyWindowClosed not yet implemented") }
    fun ignoreSslCertErrors(ignore: Boolean) { System.err.println("PluginClassMedia: ignoreSslCertErrors not yet implemented") }
    fun addCertificateFilePath(path: String) { System.err.println("PluginClassMedia: addCertificateFilePath not yet implemented") }

    fun sendPickFileResponse(files: List<String>) { System.err.println("PluginClassMedia: sendPickFileResponse not yet implemented") }
    fun sendAuthResponse(ok: Boolean, username: String, password: String) { System.err.println("PluginClassMedia: sendAuthResponse not yet implemented") }

    fun setOverrideClickTarget(target: String) { overrideClickTarget = target; clickEnforceTarget = true }
    fun resetOverrideClickTarget() { clickEnforceTarget = false }
    fun getOverrideClickTarget(): String = overrideClickTarget

    fun setUserDataPath(cachePath: String, username: String, cefLogPath: String) { System.err.println("PluginClassMedia: setUserDataPath not yet implemented") }
    fun setLanguageCode(languageCode: String) { System.err.println("PluginClassMedia: setLanguageCode not yet implemented") }
    fun setPluginsEnabled(enabled: Boolean) { System.err.println("PluginClassMedia: setPluginsEnabled not yet implemented") }
    fun setJavascriptEnabled(enabled: Boolean) { System.err.println("PluginClassMedia: setJavascriptEnabled not yet implemented") }
    fun setWebSecurityDisabled(disabled: Boolean) { System.err.println("PluginClassMedia: setWebSecurityDisabled not yet implemented") }
    fun setFileAccessFromFileUrlsEnabled(enabled: Boolean) { System.err.println("PluginClassMedia: setFileAccessFromFileUrlsEnabled not yet implemented") }
    fun setTarget(target: String) { System.err.println("PluginClassMedia: setTarget not yet implemented") }

    fun executeJavaScript(code: String) { System.err.println("PluginClassMedia: executeJavaScript not yet implemented") }
    fun loadUri(uri: String) { System.err.println("PluginClassMedia: loadUri not yet implemented") }

    fun storeOpenIdCookie(url: String, name: String, value: String, host: String, path: String, httpOnly: Boolean, secure: Boolean) {
        System.err.println("PluginClassMedia: storeOpenIdCookie not yet implemented")
    }
    fun injectOpenIdCookie() { System.err.println("PluginClassMedia: injectOpenIdCookie not yet implemented") }
    fun setCookie(uri: String, name: String, value: String, domain: String, path: String, httpOnly: Boolean, secure: Boolean) {
        System.err.println("PluginClassMedia: setCookie not yet implemented")
    }

    fun pluginSupportsMediaBrowser(): Boolean = true
    fun pluginSupportsMediaTime(): Boolean    = false

    fun stop()  { System.err.println("PluginClassMedia: stop not yet implemented") }
    fun start(rate: Float = 0f) { System.err.println("PluginClassMedia: start not yet implemented") }
    fun pause() { System.err.println("PluginClassMedia: pause not yet implemented") }
    fun seek(time: Float) { System.err.println("PluginClassMedia: seek not yet implemented") }
    fun setLoop(loop: Boolean) { System.err.println("PluginClassMedia: setLoop not yet implemented") }
    fun setVolume(volume: Float) { System.err.println("PluginClassMedia: setVolume not yet implemented") }
    fun getVolume(): Float { System.err.println("PluginClassMedia: getVolume not yet implemented"); return 0f }

    var currentTime: Double     = 0.0
    var duration: Double        = 0.0
    var currentPlayRate: Double = 0.0
    var loadedDuration: Double  = 0.0

    fun undo()      { System.err.println("PluginClassMedia: undo not yet implemented") }
    fun redo()      { System.err.println("PluginClassMedia: redo not yet implemented") }
    fun cut()       { System.err.println("PluginClassMedia: cut not yet implemented") }
    fun copy()      { System.err.println("PluginClassMedia: copy not yet implemented") }
    fun paste()     { System.err.println("PluginClassMedia: paste not yet implemented") }
    fun doDelete()  { System.err.println("PluginClassMedia: doDelete not yet implemented") }
    fun selectAll() { System.err.println("PluginClassMedia: selectAll not yet implemented") }

    fun setPriority(priority: PluginPriority) { System.err.println("PluginClassMedia: setPriority not yet implemented") }
    fun setLowPrioritySizeLimit(size: Int)    { System.err.println("PluginClassMedia: setLowPrioritySizeLimit not yet implemented") }
    fun getCpuUsage(): Double                 { System.err.println("PluginClassMedia: getCpuUsage not yet implemented"); return 0.0 }

    fun initializeUrlHistory(urlHistory: List<String>) { System.err.println("PluginClassMedia: initializeUrlHistory not yet implemented") }

    fun enableMediaPluginDebugging(enable: Boolean) { System.err.println("PluginClassMedia: enableMediaPluginDebugging not yet implemented") }

    fun jsEnableObject(enable: Boolean) { System.err.println("PluginClassMedia: jsEnableObject not yet implemented") }
    fun jsAgentLocationEvent(x: Double, y: Double, z: Double) { System.err.println("PluginClassMedia: jsAgentLocationEvent not yet implemented") }
    fun jsAgentGlobalLocationEvent(x: Double, y: Double, z: Double) { System.err.println("PluginClassMedia: jsAgentGlobalLocationEvent not yet implemented") }
    fun jsAgentOrientationEvent(angle: Double) { System.err.println("PluginClassMedia: jsAgentOrientationEvent not yet implemented") }
    fun jsAgentLanguageEvent(language: String) { System.err.println("PluginClassMedia: jsAgentLanguageEvent not yet implemented") }
    fun jsAgentRegionEvent(regionName: String) { System.err.println("PluginClassMedia: jsAgentRegionEvent not yet implemented") }
    fun jsAgentMaturityEvent(maturity: String) { System.err.println("PluginClassMedia: jsAgentMaturityEvent not yet implemented") }

    fun idle() { System.err.println("PluginClassMedia: idle not yet implemented") }
    fun reset() { System.err.println("PluginClassMedia: reset not yet implemented") }
}
