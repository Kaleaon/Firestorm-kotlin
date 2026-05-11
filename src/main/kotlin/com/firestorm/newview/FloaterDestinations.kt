package com.firestorm.newview

class FloaterDestinations private constructor(key: LLSD) : Floater(key) {

    // Slot held so it can be disconnected on destroy; null means not yet connected.
    private var destinationGuideUrlChangedSlot: (() -> Unit)? = null

    override fun postBuild(): Boolean {
        enableResizeCtrls(true, true, false)

        val destinations = getChild<MediaCtrl>("destination_guide_contents")
        destinations?.setErrorPageURL(savedSettings.getString("GenericErrorPageURL"))

        val destinationGuideUrl: String = resolveDestinationGuideUrl()

        if (destinationGuideUrl.isNotEmpty()) {
            val expanded = LLWeb.expandURLSubstitutions(destinationGuideUrl, LLSD())
            destinations?.navigateTo(expanded, HTTP_CONTENT_TEXT_HTML)
        }

        // Apply OpenID cookie if already available; otherwise the login-completion path
        // will push it once it arrives.
        ViewerMedia.instance.getOpenIDCookie(destinations)
        return true
    }

    override fun onOpen(key: LLSD) {
        // Connect during onOpen rather than in the constructor because
        // LFSimFeatureHandler may not yet be safe to instantiate at construction time.
        if (destinationGuideUrlChangedSlot == null) {
            destinationGuideUrlChangedSlot = LFSimFeatureHandler.instance
                .setDestinationGuideCallback { url -> handleUrlChanged(url) }
        }
        handleUrlChanged(LFSimFeatureHandler.instance.destinationGuideURL())
    }

    private fun handleUrlChanged(url: String) {
        getChild<MediaCtrl>("destination_guide_contents")
            ?.navigateTo(LLWeb.expandURLSubstitutions(url, LLSD()), HTTP_CONTENT_TEXT_HTML)
    }

    private fun resolveDestinationGuideUrl(): String {
        TODO("APR: use JVM equivalent - check OpenSim grid vs. SL grid; return login-response URL or saved setting")
    }

    override fun onDestroy() {
        destinationGuideUrlChangedSlot = null
        super.onDestroy()
    }
}
