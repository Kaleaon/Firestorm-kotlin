package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.Button
import com.firestorm.llui.ComboBox
import com.firestorm.llui.TextBox
import com.firestorm.llui.UICtrl
import com.firestorm.llcommon.LLUUID

data class WebContentParams(
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

class FloaterWebContent(private val params: WebContentParams) :
    Floater(params),
    PluginClassMediaOwner
{
    val uuid: String = params.id.ifEmpty { LLUUID.generateNewID().toString() }

    private var webBrowser: MediaCtrl? = null
    private var addressCombo: ComboBox? = null
    private var secureLockIcon: UICtrl? = null
    private var statusBarText: TextBox? = null
    private var pluginFailText: TextBox? = null
    private var statusBarProgress: UICtrl? = null

    private var btnBack: UICtrl? = null
    private var btnForward: UICtrl? = null
    private var btnReload: UICtrl? = null
    private var btnStop: UICtrl? = null

    private var currentUrl: String = ""
    private var displayUrl: String = ""
    private var showPageTitle: Boolean = params.showPageTitle
    private var allowNavigation: Boolean = params.allowBackForwardNavigation
    private val developMode: Boolean = params.devMode

    companion object {
        private val instances: MutableMap<String, FloaterWebContent> = mutableMapOf()

        fun create(p: WebContentParams): FloaterWebContent {
            val resolved = preCreate(p)
            val floater = FloaterWebContent(resolved)
            instances[floater.uuid] = floater
            return floater
        }

        fun closeRequest(uuid: String) {
            instances[uuid]?.closeFloater()
        }

        fun geometryChanged(uuid: String, x: Int, y: Int, width: Int, height: Int) {
            instances[uuid]?.geometryChanged(x, y, width, height)
        }

        private fun preCreate(p: WebContentParams): WebContentParams {
            val id     = p.id.ifEmpty { LLUUID.generateNewID().toString() }
            val target = if (p.target.isEmpty() || p.target == "_blank") id else p.target

            val windowLimit = SavedSettings.getInt("WebContentWindowLimit")
            if (windowLimit != 0 && instances.size >= windowLimit) {
                instances.values.firstOrNull()?.closeFloater()
            }

            return p.copy(id = id, target = target)
        }
    }

    override fun postBuild(): Boolean {
        webBrowser        = getChild("webbrowser")
        addressCombo      = getChild("address")
        statusBarText     = getChild("statusbartext")
        statusBarProgress = getChild("statusbarprogress")
        pluginFailText    = getChild("plugin_fail_text")
        secureLockIcon    = getChild("media_secure_lock_flag")

        btnBack    = getChildView("back")
        btnForward = getChildView("forward")
        btnReload  = getChildView("reload")
        btnStop    = getChildView("stop")

        webBrowser?.addObserver(this)

        webBrowser?.setVisible(false)
        pluginFailText?.setVisible(true)

        btnReload?.setEnabled(true)
        btnReload?.setVisible(false)
        getChildView("popexternal")?.setEnabled(true)

        initializeUrlHistory()
        return true
    }

    private fun initializeUrlHistory() {
        TODO("APR: clear address combo then populate from LLURLHistory.getURLHistory('browser')")
    }

    override fun onOpen(key: Any) {
        val p = WebContentParams(
            url           = SavedSettings.getStringFromLLSD(key, "url"),
            target        = SavedSettings.getStringFromLLSD(key, "target"),
            id            = SavedSettings.getStringFromLLSD(key, "id"),
            trustedContent = SavedSettings.getBoolFromLLSD(key, "trusted_content"),
            showChrome    = SavedSettings.getBoolFromLLSD(key, "show_chrome", default = true),
            allowAddressEntry = SavedSettings.getBoolFromLLSD(key, "allow_address_entry", default = true),
            allowBackForwardNavigation = SavedSettings.getBoolFromLLSD(key, "allow_back_forward_navigation", default = true),
            showPageTitle  = SavedSettings.getBoolFromLLSD(key, "show_page_title", default = true),
            devMode        = SavedSettings.getBoolFromLLSD(key, "dev_mode"),
            preferredMediaWidth  = SavedSettings.getIntFromLLSD(key, "preferred_media_width"),
            preferredMediaHeight = SavedSettings.getIntFromLLSD(key, "preferred_media_height"),
        )
        webBrowser?.setTrustedContent(p.trustedContent)
        openMedia(p)
    }

    override fun onClose(appQuitting: Boolean) {
        TODO("GPU: ViewerMedia.proxyWindowClosed($uuid)")
        instances.remove(uuid)
    }

    override fun draw() {
        val browser = webBrowser
        if (browser != null) {
            btnBack?.setEnabled(browser.canNavigateBack() && allowNavigation)
            btnForward?.setEnabled(browser.canNavigateForward() && allowNavigation)
        }
        super.draw()
    }

    override fun matchesKey(key: Any): Boolean {
        val otherTarget = SavedSettings.getStringFromLLSD(key, "target")
        val otherId     = SavedSettings.getStringFromLLSD(key, "id")
        return if (otherTarget.isNotEmpty() && otherTarget != "_blank") {
            otherTarget == params.target
        } else {
            otherId == uuid
        }
    }

    override fun handleMediaEvent(plugin: PluginClassMedia, event: MediaEvent) {
        when (event) {
            MediaEvent.LOCATION_CHANGED -> {
                val url = plugin.location
                if (url.isNotEmpty()) {
                    statusBarText?.setText(url)
                    setCurrentUrl(url)
                }
            }
            MediaEvent.NAVIGATE_BEGIN -> {
                webBrowser?.setVisible(true)
                pluginFailText?.setVisible(false)
                btnBack?.setEnabled(plugin.historyBackAvailable)
                btnForward?.setEnabled(plugin.historyForwardAvailable)
                btnReload?.setVisible(false)
                btnStop?.setVisible(true)
                statusBarProgress?.setVisible(true)
            }
            MediaEvent.NAVIGATE_COMPLETE -> {
                btnBack?.setEnabled(plugin.historyBackAvailable)
                btnForward?.setEnabled(plugin.historyForwardAvailable)
                btnReload?.setVisible(true)
                btnStop?.setVisible(false)
                statusBarProgress?.setVisible(false)
                statusBarText?.setText("")
            }
            MediaEvent.CLOSE_REQUEST -> {
                closeFloater()
            }
            MediaEvent.STATUS_TEXT_CHANGED -> {
                val text = plugin.statusText
                if (text.isNotEmpty()) statusBarText?.setText(text)
            }
            MediaEvent.PROGRESS_UPDATED -> {
                statusBarProgress?.setValue(plugin.progressPercent)
            }
            MediaEvent.NAME_CHANGED -> {
                btnBack?.setEnabled(plugin.historyBackAvailable)
                btnForward?.setEnabled(plugin.historyForwardAvailable)
                if (showPageTitle) {
                    val pageTitle = plugin.mediaName
                    setTitle(if (pageTitle.isNotEmpty()) pageTitle else currentUrl)
                }
            }
            MediaEvent.LINK_HOVERED -> {
                statusBarText?.setText(plugin.hoverLink)
            }
            else -> Unit
        }
    }

    fun geometryChanged(x: Int, y: Int, width: Int, height: Int) {
        TODO("APR: adjust floater rect to accommodate requested browser geometry, clamped to window bounds")
    }

    fun onClickBack()    { webBrowser?.navigateBack() }
    fun onClickForward() { webBrowser?.navigateForward() }

    fun onClickReload() {
        val plugin = webBrowser?.getMediaPlugin()
        if (plugin != null) {
            TODO("GPU: plugin.browseReload(ignoreCache = true)")
        } else {
            webBrowser?.navigateTo(currentUrl)
        }
    }

    fun onClickStop() {
        TODO("GPU: webBrowser.getMediaPlugin()?.browseStop()")
        btnReload?.setVisible(true)
        btnStop?.setVisible(false)
    }

    fun onEnterAddress() {
        val url = addressCombo?.getValue()?.asString()?.trim() ?: return
        if (url.isNotEmpty()) webBrowser?.navigateTo(url)
    }

    fun onPopExternal() {
        val url = addressCombo?.getValue()?.asString()?.trim() ?: return
        if (url.isNotEmpty()) TODO("APR: Web.loadUrlExternal($url)")
    }

    fun onTestUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isNotEmpty()) webBrowser?.navigateTo(trimmed)
    }

    private fun openMedia(p: WebContentParams) {
        TODO("GPU: ViewerMedia.proxyWindowOpened(p.target, p.id)")
        webBrowser?.setHomePageUrl(p.url)
        webBrowser?.setTarget(p.target)
        webBrowser?.navigateTo(p.url)
        setCurrentUrl(p.url)

        getChildView("status_bar")?.setVisible(p.showChrome)
        getChildView("nav_controls")?.setVisible(p.showChrome)
        getChildView("debug_controls")?.setVisible(developMode)

        val addressEntryEnabled = p.allowAddressEntry && !p.trustedContent
        allowNavigation = p.allowBackForwardNavigation
        getChildView("address")?.setEnabled(addressEntryEnabled)
        getChildView("popexternal")?.setEnabled(addressEntryEnabled)

        if (!p.showChrome) setResizeLimits(100, 100)

        if (p.preferredMediaWidth > 0 && p.preferredMediaHeight > 0) {
            TODO("APR: update layout stack then call geometryChanged to resize floater to preferred dimensions")
        }
    }

    private fun setCurrentUrl(url: String) {
        if (url.isEmpty()) return

        if (currentUrl.isNotEmpty()) {
            addressCombo?.remove(displayUrl)
            addressCombo?.add(currentUrl)
        }

        currentUrl = url.trim()

        TODO("APR: LLURLHistory.removeURL('browser', currentUrl); LLURLHistory.addURL('browser', currentUrl)")

        val secureUrl = currentUrl.startsWith("https://", ignoreCase = true)
        secureLockIcon?.setVisible(secureUrl)
        TODO("APR: set address combo left-text padding to ${if (secureUrl) 22 else 2}")

        displayUrl = currentUrl
        addressCombo?.remove(currentUrl)
        addressCombo?.add(displayUrl)
        addressCombo?.selectByValue(displayUrl)
    }
}
