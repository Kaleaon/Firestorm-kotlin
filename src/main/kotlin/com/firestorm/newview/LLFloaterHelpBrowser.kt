package com.firestorm.newview

class LLFloaterHelpBrowser(key: LLSDMap) : LLFloater(key), LLViewerMediaObserver {

    private var mBrowser: LLMediaCtrl? = null
    private var mCurrentURL: String = ""

    override fun postBuild(): Boolean {
        mBrowser = getChild<LLMediaCtrl>("browser")
        mBrowser?.addObserver(this)
        mBrowser?.setErrorPageURL(gSavedSettings.getString("GenericErrorPageURL"))

        childSetAction("open_browser", ::onClickOpenWebBrowser, this)

        buildURLHistory()
        return true
    }

    override fun onOpen(key: LLSDMap) {
        gSavedSettings.setBoolean("HelpFloaterOpen", true)
        val topic = key.asString()
        mBrowser?.navigateTo(LLViewerHelp.instance().getURL(topic))
    }

    override fun onClose(appQuitting: Boolean) {
        if (!appQuitting) {
            gSavedSettings.setBoolean("HelpFloaterOpen", false)
        }
        // Destroy immediately on close; this floater is recreated on each use.
        destroy()
    }

    override fun handleMediaEvent(self: LLPluginClassMedia, event: EMediaEvent) {
        when (event) {
            EMediaEvent.MEDIA_EVENT_LOCATION_CHANGED ->
                setCurrentURL(self.getLocation())
            EMediaEvent.MEDIA_EVENT_NAVIGATE_BEGIN ->
                getChild<LLUICtrl>("status_text").setValue(getString("loading_text"))
            EMediaEvent.MEDIA_EVENT_NAVIGATE_COMPLETE ->
                getChild<LLUICtrl>("status_text").setValue(getString("done_text"))
            else -> Unit
        }
    }

    fun openMedia(mediaUrl: String) {
        mBrowser?.setHomePageUrl(mediaUrl, HTTP_CONTENT_TEXT_HTML)
        mBrowser?.navigateTo(mediaUrl, HTTP_CONTENT_TEXT_HTML)
        setCurrentURL(mediaUrl)
    }

    private fun buildURLHistory() {
        val browserHistory = LLURLHistory.getURLHistory("browser")
        mBrowser?.getMediaPlugin()?.initializeUrlHistory(browserHistory)
    }

    private fun setCurrentURL(url: String) {
        mCurrentURL = url
        // redirects briefly visit about:blank — skip those so history stays clean
        if (mCurrentURL != "about:blank") {
            LLURLHistory.removeURL("browser", mCurrentURL)
            LLURLHistory.addURL("browser", mCurrentURL)
        }
    }

    companion object {
        @JvmStatic
        private fun onClickClose(userData: Any?) {
            val self = userData as? LLFloaterHelpBrowser ?: return
            self.closeFloater()
        }

        @JvmStatic
        private fun onClickOpenWebBrowser(userData: Any?) {
            val self = userData as? LLFloaterHelpBrowser ?: return
            val url = if (self.mCurrentURL.isEmpty()) self.mBrowser?.getHomePageUrl() ?: "" else self.mCurrentURL
            LLWeb.loadURLExternal(url)
        }
    }
}
