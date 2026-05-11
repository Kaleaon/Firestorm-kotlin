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
        TODO("GPU: send size message to CEF plugin process")
    }

    fun setAutoScale(autoScale: Boolean) { TODO("GPU: send auto-scale flag to plugin") }

    fun mouseEvent(type: MouseEventType, button: Int, x: Int, y: Int, modifiers: Int) {
        TODO("GPU: forward mouse event to plugin IPC")
    }

    fun keyEvent(type: KeyEventType, keyCode: Int, modifiers: Int): Boolean {
        TODO("GPU: forward key event to plugin IPC")
    }

    fun scrollEvent(x: Int, y: Int, clicksX: Int, clicksY: Int, modifiers: Int) {
        TODO("GPU: forward scroll event to plugin IPC")
    }

    fun textInput(text: String, modifiers: Int): Boolean {
        TODO("GPU: forward unicode text to plugin IPC")
    }

    fun focus(focused: Boolean) { TODO("GPU: relay focus state to CEF plugin") }
    fun setPageZoomFactor(factor: Double) { zoomFactor = factor; TODO("GPU: send zoom to CEF plugin") }
    fun clearCache()   { TODO("GPU: send clear-cache command to plugin") }
    fun clearCookies() { TODO("GPU: send clear-cookies command to plugin") }
    fun setCookiesEnabled(enable: Boolean) { TODO("GPU: send cookies-enabled to plugin") }
    fun proxySetup(enable: Boolean, host: String = "", port: Int = 0) { TODO("GPU: configure plugin proxy") }
    fun browseStop()    { TODO("GPU: send browse_stop to CEF plugin") }
    fun browseReload(ignoreCache: Boolean = false) { TODO("GPU: send browse_reload to CEF plugin") }
    fun browseForward() { TODO("GPU: send browse_forward to CEF plugin") }
    fun browseBack()    { TODO("GPU: send browse_back to CEF plugin") }
    fun setBrowserUserAgent(userAgent: String) { TODO("GPU: send user-agent to CEF plugin") }
    fun showWebInspector(show: Boolean) { TODO("GPU: toggle CEF DevTools window") }
    fun showPageSource() { TODO("GPU: open page source in new browser window") }
    fun proxyWindowOpened(target: String, uuid: String) { TODO("GPU: notify plugin of popup window opened") }
    fun proxyWindowClosed(uuid: String) { TODO("GPU: notify plugin of popup window closed") }
    fun ignoreSslCertErrors(ignore: Boolean) { TODO("GPU: configure SSL cert error handling in CEF") }
    fun addCertificateFilePath(path: String) { TODO("GPU: add CA cert path to CEF plugin") }

    fun sendPickFileResponse(files: List<String>) { TODO("GPU: send file-picker result back to plugin") }
    fun sendAuthResponse(ok: Boolean, username: String, password: String) { TODO("GPU: send HTTP auth credentials to plugin") }

    fun setOverrideClickTarget(target: String) { overrideClickTarget = target; clickEnforceTarget = true }
    fun resetOverrideClickTarget() { clickEnforceTarget = false }
    fun getOverrideClickTarget(): String = overrideClickTarget

    fun setUserDataPath(cachePath: String, username: String, cefLogPath: String) { TODO("GPU: configure plugin user-data paths") }
    fun setLanguageCode(languageCode: String) { TODO("GPU: set plugin locale/language code") }
    fun setPluginsEnabled(enabled: Boolean) { TODO("GPU: enable/disable browser sub-plugins") }
    fun setJavascriptEnabled(enabled: Boolean) { TODO("GPU: enable/disable JS in plugin") }
    fun setWebSecurityDisabled(disabled: Boolean) { TODO("GPU: configure CEF web-security policy") }
    fun setFileAccessFromFileUrlsEnabled(enabled: Boolean) { TODO("GPU: configure file:// access policy") }
    fun setTarget(target: String) { TODO("GPU: set target frame name in CEF plugin") }

    fun executeJavaScript(code: String) { TODO("GPU: inject JS into active CEF page") }
    fun loadUri(uri: String) { TODO("GPU: navigate plugin to URI") }

    fun storeOpenIdCookie(url: String, name: String, value: String, host: String, path: String, httpOnly: Boolean, secure: Boolean) {
        TODO("GPU: store OpenID cookie in CEF cookie store")
    }
    fun injectOpenIdCookie() { TODO("GPU: inject previously stored OpenID cookie into CEF") }
    fun setCookie(uri: String, name: String, value: String, domain: String, path: String, httpOnly: Boolean, secure: Boolean) {
        TODO("GPU: set individual cookie in CEF cookie store")
    }

    fun pluginSupportsMediaBrowser(): Boolean = true
    fun pluginSupportsMediaTime(): Boolean    = false

    fun stop()  { TODO("GPU: stop media time playback in plugin") }
    fun start(rate: Float = 0f) { TODO("GPU: start media time playback in plugin") }
    fun pause() { TODO("GPU: pause media time playback in plugin") }
    fun seek(time: Float) { TODO("GPU: seek media to time position") }
    fun setLoop(loop: Boolean) { TODO("GPU: set media loop flag in plugin") }
    fun setVolume(volume: Float) { TODO("GPU: set media volume in plugin") }
    fun getVolume(): Float { TODO("GPU: query current media volume from plugin") }

    var currentTime: Double     = 0.0
    var duration: Double        = 0.0
    var currentPlayRate: Double = 0.0
    var loadedDuration: Double  = 0.0

    fun undo()      { TODO("GPU: send undo command to CEF plugin") }
    fun redo()      { TODO("GPU: send redo command to CEF plugin") }
    fun cut()       { TODO("GPU: send cut command to CEF plugin") }
    fun copy()      { TODO("GPU: send copy command to CEF plugin") }
    fun paste()     { TODO("GPU: send paste command to CEF plugin") }
    fun doDelete()  { TODO("GPU: send delete command to CEF plugin") }
    fun selectAll() { TODO("GPU: send select-all command to CEF plugin") }

    fun setPriority(priority: PluginPriority) { TODO("GPU: set plugin update priority") }
    fun setLowPrioritySizeLimit(size: Int)    { TODO("GPU: set lower-res size limit for low-priority rendering") }
    fun getCpuUsage(): Double                 { TODO("GPU: query plugin CPU usage") }

    fun initializeUrlHistory(urlHistory: List<String>) { TODO("GPU: send URL history list to CEF plugin") }

    fun enableMediaPluginDebugging(enable: Boolean) { TODO("GPU: toggle CEF plugin debug logging") }

    fun jsEnableObject(enable: Boolean) { TODO("GPU: enable/disable viewer JS API object in page") }
    fun jsAgentLocationEvent(x: Double, y: Double, z: Double) { TODO("GPU: send agent local-position JS event") }
    fun jsAgentGlobalLocationEvent(x: Double, y: Double, z: Double) { TODO("GPU: send agent global-position JS event") }
    fun jsAgentOrientationEvent(angle: Double) { TODO("GPU: send agent heading JS event") }
    fun jsAgentLanguageEvent(language: String) { TODO("GPU: send agent language JS event") }
    fun jsAgentRegionEvent(regionName: String) { TODO("GPU: send agent region-name JS event") }
    fun jsAgentMaturityEvent(maturity: String) { TODO("GPU: send agent maturity-rating JS event") }

    fun idle() { TODO("GPU: pump plugin IPC message queue") }
    fun reset() { TODO("GPU: tear down plugin IPC channel and shared memory") }
}
