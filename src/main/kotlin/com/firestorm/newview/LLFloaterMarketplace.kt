package com.firestorm.newview

import java.util.UUID

class LLFloaterMarketplace(key: LLSD) : LLFloaterWebContent(key) {

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false

        mWebBrowser.setErrorPageURL(gSavedSettings.getString("GenericErrorPageURL"))
        val url = gSavedSettings.getString("MarketplaceURL")
        mWebBrowser.navigateTo(url, HTTP_CONTENT_TEXT_HTML)

        // Cookie may already be present from a prior login; otherwise deferred until login completes.
        LLViewerMedia.getInstance().getOpenIDCookie(mWebBrowser)

        return true
    }

    override fun onOpen(key: LLSD) {
        val params = Params(key)
        if (!params.validateBlock()) {
            closeFloater()
            return
        }
        if (params.url().isEmpty()) {
            openMarketplace()
        } else {
            openMarketplaceURL(params.url)
            setCurrentUrl(params.url)
        }
    }

    override fun onClose(appQuitting: Boolean) {
        // intentionally empty — overrides LLFloaterWebContent close behaviour
    }

    fun openMarketplace() {
        val url = gSavedSettings.getString("MarketplaceURL")
        if (mCurrentURL != url) {
            mWebBrowser.navigateTo(url, HTTP_CONTENT_TEXT_HTML)
        }
    }

    fun openMarketplaceURL(url: String) {
        if (mCurrentURL != url) {
            mWebBrowser.navigateTo(url, HTTP_CONTENT_TEXT_HTML)
        }
    }

    companion object {
        fun isMarketplaceURL(url: String): Boolean {
            fun trimUrl(raw: String): String {
                var s = raw
                s = when {
                    s.startsWith("https://") -> s.removePrefix("https://")
                    s.startsWith("http://") -> s.removePrefix("http://")
                    else -> s
                }
                return s.trimEnd('/')
            }

            val marketplaceUrl = gSavedSettings.getString("MarketplaceURL")
            val marketplaceTrimmed = trimUrl(marketplaceUrl)
            val urlTrimmed = trimUrl(url)
            return marketplaceTrimmed.isNotEmpty() && urlTrimmed.startsWith(marketplaceTrimmed)
        }
    }
}
