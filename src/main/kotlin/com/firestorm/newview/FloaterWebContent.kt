package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

data class FloaterWebContentParams(
    val url: String = "",
    val target: String = "",
    val windowClass: String = "web_content",
    val id: String = LLUUID.generateNewID().toString(),
    val showChrome: Boolean = true,
    val allowAddressEntry: Boolean = true,
    val allowBackForwardNavigation: Boolean = true,
    val trustedContent: Boolean = false,
    val showPageTitle: Boolean = true,
    val cleanBrowser: Boolean = false,
    val devMode: Boolean = false,
    val preferredMediaWidth: Int = 0,
    val preferredMediaHeight: Int = 0,
)

class FloaterWebContent(private val params: FloaterWebContentParams) : PluginClassMediaOwner {

    val uuid: String = params.id.ifEmpty { LLUUID.generateNewID().toString() }

    val webBrowser: MediaCtrl = MediaCtrl(
        trustedContent = params.trustedContent,
    )

    private var currentUrl: String  = ""
    private var displayUrl: String  = ""
    private var showPageTitle: Boolean = params.showPageTitle
    private var allowNavigation: Boolean = params.allowBackForwardNavigation
    private var developMode: Boolean     = params.devMode

    private val urlHistory: MutableList<String> = mutableListOf()

    private var backEnabled: Boolean    = false
    private var forwardEnabled: Boolean = false
    private var reloadVisible: Boolean  = false
    private var stopVisible: Boolean    = false
    private var progressVisible: Boolean = false
    private var progressPercent: Int    = 0
    private var statusText: String      = ""
    private var addressText: String     = ""
    private var secureLockVisible: Boolean = false
    private var title: String           = ""

    companion object {
        private val instances: MutableMap<String, FloaterWebContent> = mutableMapOf()

        fun create(params: FloaterWebContentParams): FloaterWebContent {
            val p = preCreate(params)
            val floater = FloaterWebContent(p)
            instances[floater.uuid] = floater
            return floater
        }

        fun closeRequest(uuid: String) {
            instances[uuid]?.close()
        }

        fun geometryChanged(uuid: String, x: Int, y: Int, width: Int, height: Int) {
            instances[uuid]?.geometryChanged(x, y, width, height)
        }

        private fun preCreate(params: FloaterWebContentParams): FloaterWebContentParams {
            val id = params.id.ifEmpty { LLUUID.generateNewID().toString() }
            val target = params.target.ifEmpty { id }
            return params.copy(id = id, target = target)
        }
    }

    init {
        webBrowser.addObserver(this)
    }

    fun postBuild() {
        initializeUrlHistory()
        webBrowser.setTrustedContent(params.trustedContent)
    }

    private fun initializeUrlHistory() {
        TODO("APR: load URL history from LLURLHistory('browser') and populate address combo")
    }

    fun onOpen() {
        webBrowser.setTrustedContent(params.trustedContent)
        openMedia(params)
    }

    private fun openMedia(p: FloaterWebContentParams) {
        TODO("GPU: ViewerMedia.proxyWindowOpened(p.target, p.id)")
        webBrowser.setHomePageUrl(p.url)
        webBrowser.setTarget(p.target)
        webBrowser.navigateTo(p.url, cleanBrowser = p.cleanBrowser)
        setCurrentUrl(p.url)

        if (!p.showChrome) {
            TODO("APR: hide status_bar and nav_controls layout panels")
        }
        if (developMode) {
            TODO("APR: show debug_controls layout panel")
        }

        val addressEntryEnabled = p.allowAddressEntry && !p.trustedContent
        allowNavigation = p.allowBackForwardNavigation
        TODO("APR: set address combo and popexternal button enabled=$addressEntryEnabled")

        if (p.preferredMediaWidth > 0 && p.preferredMediaHeight > 0) {
            TODO("APR: update layout stack then call geometryChanged to resize floater to preferred dimensions")
        }
    }

    fun onClose(appQuitting: Boolean) {
        TODO("GPU: ViewerMedia.proxyWindowClosed($uuid)")
        instances.remove(uuid)
    }

    fun close() {
        onClose(false)
    }

    fun draw() {
        backEnabled    = webBrowser.canNavigateBack()    && allowNavigation
        forwardEnabled = webBrowser.canNavigateForward() && allowNavigation
        TODO("APR: push backEnabled/forwardEnabled to back/forward button enabled state; call super.draw()")
    }

    override fun handleMediaEvent(plugin: PluginClassMedia, event: MediaEvent) {
        when (event) {
            MediaEvent.LOCATION_CHANGED -> {
                val url = plugin.location
                if (url.isNotEmpty()) {
                    statusText = url
                    setCurrentUrl(url)
                }
            }
            MediaEvent.NAVIGATE_BEGIN -> {
                TODO("APR: setVisible(webBrowser=true, pluginFailText=false)")
                backEnabled    = plugin.historyBackAvailable
                forwardEnabled = plugin.historyForwardAvailable
                reloadVisible  = false
                stopVisible    = true
                progressVisible = true
            }
            MediaEvent.NAVIGATE_COMPLETE -> {
                backEnabled    = plugin.historyBackAvailable
                forwardEnabled = plugin.historyForwardAvailable
                reloadVisible  = true
                stopVisible    = false
                progressVisible = false
                statusText     = ""
            }
            MediaEvent.CLOSE_REQUEST -> {
                close()
            }
            MediaEvent.STATUS_TEXT_CHANGED -> {
                if (plugin.statusText.isNotEmpty()) statusText = plugin.statusText
            }
            MediaEvent.PROGRESS_UPDATED -> {
                progressPercent = plugin.progressPercent
            }
            MediaEvent.NAME_CHANGED -> {
                backEnabled    = plugin.historyBackAvailable
                forwardEnabled = plugin.historyForwardAvailable
                val pageTitle = plugin.mediaName
                if (showPageTitle) {
                    title = pageTitle.ifEmpty { currentUrl }
                }
            }
            MediaEvent.LINK_HOVERED -> {
                statusText = plugin.hoverLink
            }
            else -> Unit
        }
    }

    private fun setCurrentUrl(url: String) {
        if (url.isEmpty()) return

        if (currentUrl.isNotEmpty()) {
            urlHistory.remove(displayUrl)
            if (!urlHistory.contains(currentUrl)) urlHistory.add(currentUrl)
        }

        currentUrl = url.trim()

        TODO("APR: LLURLHistory.removeURL('browser', currentUrl); LLURLHistory.addURL('browser', currentUrl)")

        val secureUrl = currentUrl.startsWith("https://", ignoreCase = true)
        secureLockVisible = secureUrl
        TODO("APR: set address combo left-text padding to ${if (secureUrl) 22 else 2}")

        displayUrl = currentUrl
        urlHistory.remove(currentUrl)
        urlHistory.add(displayUrl)
        addressText = displayUrl
        TODO("APR: update address combo selection to displayUrl")
    }

    fun matchesKey(url: String, target: String, id: String): Boolean {
        return if (target.isNotEmpty() && target != "_blank") {
            target == params.target
        } else {
            id == uuid
        }
    }

    fun geometryChanged(x: Int, y: Int, width: Int, height: Int) {
        TODO("APR: adjust floater rect to accommodate requested browser geometry, clamped to window bounds")
    }

    fun onClickBack()    { webBrowser.navigateBack() }
    fun onClickForward() { webBrowser.navigateForward() }

    fun onClickReload() {
        val plugin = webBrowser.getMediaPlugin()
        if (plugin != null) {
            TODO("GPU: plugin.browseReload(ignoreCache=true)")
        } else {
            webBrowser.navigateTo(currentUrl)
        }
    }

    fun onClickStop() {
        TODO("GPU: plugin.browseStop()")
        reloadVisible = true
        stopVisible   = false
    }

    fun onEnterAddress(rawUrl: String) {
        val url = rawUrl.trim()
        if (url.isNotEmpty()) webBrowser.navigateTo(url)
    }

    fun onPopExternal(rawUrl: String) {
        val url = rawUrl.trim()
        if (url.isNotEmpty()) TODO("APR: Web.loadUrlExternal($url)")
    }

    fun onTestUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isNotEmpty()) webBrowser.navigateTo(trimmed)
    }
}
