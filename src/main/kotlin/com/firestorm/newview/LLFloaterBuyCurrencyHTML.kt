package com.firestorm.newview

import java.util.UUID

class LLFloaterBuyCurrencyHTML(key: LLSD) : LLFloater(key), LLViewerMediaObserver {

    private var mBrowser: LLMediaCtrl? = null
    private var mSpecificSumRequested: Boolean = false
    private var mMessage: String = ""
    private var mSum: Int = 0

    override fun postBuild(): Boolean {
        mBrowser = getChild<LLMediaCtrl>("browser")
        mBrowser?.addObserver(this)
        return true
    }

    fun navigateToFinalURL() {
        var buyCurrencyUrl: String = getString("buy_currency_url")

        val replace = mutableMapOf<String, String>()
        replace["[LANGUAGE]"] = LLUI.getLanguage()
        replace["[SPECIFIC_AMOUNT]"] = if (mSpecificSumRequested) "y" else "n"
        replace["[SUM]"] = mSum.toString()
        replace["[BAL]"] = gStatusBar.getBalance().toString()
        replace["[MSG]"] = LLURI.escape(mMessage)

        buyCurrencyUrl = LLStringUtil.format(buyCurrencyUrl, replace)

        mBrowser?.navigateTo(buyCurrencyUrl, HTTP_CONTENT_TEXT_HTML)
    }

    override fun handleMediaEvent(self: LLPluginClassMedia, event: EMediaEvent) {
        if (LLPluginClassMediaOwner.MEDIA_EVENT_NAVIGATE_COMPLETE == event) {
            LLStatusBar.sendMoneyBalanceRequest()
        }
    }

    override fun onClose(appQuitting: Boolean) {
        LLStatusBar.sendMoneyBalanceRequest()
        destroy()
    }

    fun setParams(specificSumRequested: Boolean, message: String, sum: Int) {
        mSpecificSumRequested = specificSumRequested
        mMessage = message
        mSum = sum
    }
}
