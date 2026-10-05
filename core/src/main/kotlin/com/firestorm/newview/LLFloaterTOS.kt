package com.firestorm.newview

class LLFloaterTOS(data: LLSD) : LLModalDialog(data["message"].asString()), LLViewerMediaObserver {

    private val message: String = data["message"].asString()
    private var loadingScreenLoaded: Boolean = false
    private var siteAlive: Boolean = false
    private var realNavigateBegun: Boolean = false
    private val replyPumpName: String = data["reply_pump"].asString()

    override fun postBuild(): Boolean {
        childSetAction("Continue") { onContinue() }
        childSetAction("Cancel") { onCancel() }
        childSetCommitCallback("agree_chk") { updateAgree() }

        if (hasChild("tos_text")) {
            val tosText = getChild<LLUICtrl>("tos_text")
            tosText.setEnabled(false)
            tosText.setFocus(true)
            tosText.setValue(LLSD(message))
            return true
        }

        updateAgreeEnabled(false)

        getChild<LLUICtrl>("tos_text").setVisible(false)

        val webBrowser = getChild<LLMediaCtrl>("tos_html")

        val useWebBrowser: Boolean
        if (LLGridManager.instance.isInOpenSim()) {
            // Display the TOS as a browser page only when the message itself is a URL
            useWebBrowser = message.contains("http://")
        } else {
            useWebBrowser = true
        }

        if (webBrowser != null && useWebBrowser) {
            webBrowser.addObserver(this)

            if (LLGridManager.instance.isInOpenSim()) {
                realNavigateBegun = true
                updateAgreeEnabled(true)
                webBrowser.navigateTo(message)
            } else {
                webBrowser.navigateTo(getString("loading_url"))
                val mediaPlugin = webBrowser.getMediaPlugin()
                // All links in tos_html must open externally so users cannot navigate away
                mediaPlugin?.setOverrideClickTarget("_external")
            }
        } else if (LLGridManager.instance.isInOpenSim()) {
            // OpenSim plain-text TOS: embed message in a minimal HTML page
            val showTos = buildOpenSimHtmlTos(message)
            realNavigateBegun = true
            updateAgreeEnabled(true)
            webBrowser?.navigateTo(showTos)
        }

        return true
    }

    private fun buildOpenSimHtmlTos(body: String): String {
        return "data:text/html,%3Chtml%3E%3Chead%3E" +
            "%3Cstyle%3E%0A" +
            "body%20%7B%0A" +
            "background-color%3Argb%2831%2C%2031%2C%2031%29%3B%0A" +
            "margin%3A5px%2020px%205px%2030px%3B%0A" +
            "padding%3A0%3B%0A%7D%0A" +
            "pre%20%7B%0Afont-size%3A12px%3B%0A" +
            "font-family%3A%22Deja%20Vu%20Sans%22%2C%20Helvetica%2C%20Arial%2C%20sans-serif%3B%0A" +
            "color%3A%23fff%3B%0A%7D%0A" +
            "%3C/style%3E" +
            "%3C/head%3E%3Cbody%3E%3Cpre%3E$body%3C/pre%3E%3C/body%3E%3C/html%3E"
    }

    fun setSiteIsAlive(alive: Boolean) {
        siteAlive = alive

        if (!hasChild("tos_html")) return

        if (alive) {
            if (!realNavigateBegun && siteAlive) {
                val webBrowser = getChild<LLMediaCtrl>("tos_html")
                if (webBrowser != null) {
                    realNavigateBegun = true
                    webBrowser.navigateTo(getString("real_url"))
                }
            }
        } else {
            // Page unreachable: allow agreement without waiting for load
            updateAgreeEnabled(true)
            getChild<LLTextBox>("agree_list").setEnabled(true)
        }
    }

    override fun draw() {
        super.draw()
    }

    fun updateAgreeEnabled(enabled: Boolean) {
        getChild<LLCheckBoxCtrl>("agree_chk").setEnabled(enabled)
        getChild<LLTextBox>("agree_list").setEnabled(enabled)
    }

    fun updateAgree() {
        val agree = getChild<LLUICtrl>("agree_chk").getValue().asBoolean()
        getChildView("Continue").setEnabled(agree)
    }

    fun onContinue() {
        if (replyPumpName.isNotEmpty()) {
            LLEventPumps.instance.obtain(replyPumpName).post(LLSD(true))
        }
        closeFloater()
    }

    fun onCancel() {
        LLNotificationsUtil.add("MustAgreeToLogIn", LLSD(), LLSD(), ::loginAlertDone)

        if (replyPumpName.isNotEmpty()) {
            LLEventPumps.instance.obtain(replyPumpName).post(LLSD(false))
        }

        loadingScreenLoaded = false
        siteAlive = false
        realNavigateBegun = false

        closeFloater()
    }

    override fun handleMediaEvent(self: LLPluginClassMedia, event: EMediaEvent) {
        if (event == EMediaEvent.MEDIA_EVENT_NAVIGATE_COMPLETE) {
            if (!loadingScreenLoaded) {
                loadingScreenLoaded = true
                val url = getString("real_url")
                val handle = getHandle()
                testSiteIsAlive(handle, url)
            } else if (realNavigateBegun) {
                updateAgreeEnabled(true)
                getChild<LLTextBox>("agree_list").setEnabled(true)
            }
        }
    }

    private fun testSiteIsAlive(handle: LLHandle<LLFloater>, url: String) {
        Thread {
            val alive = try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "HEAD"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val code = conn.responseCode
                conn.disconnect()
                code in 200..399
            } catch (e: Exception) {
                false
            }
            setSiteIsAlive(alive)
        }.also { it.isDaemon = true; it.start() }
    }
}
