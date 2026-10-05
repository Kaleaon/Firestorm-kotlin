package com.firestorm.newview

import com.firestorm.ui.FloaterReg
import com.firestorm.ui.StatusBar

object BuyCurrencyHtml {

    fun openCurrencyFloater() {
        if (ViewerControl.savedSettings.getBool("QuickBuyCurrency")) {
            showDialog(specificSumRequested = false, message = "", sum = 0)
        } else {
            FloaterBuyCurrency.buyCurrency()
        }
    }

    fun openCurrencyFloater(message: String, sum: Int) {
        if (ViewerControl.savedSettings.getBool("QuickBuyCurrency")) {
            showDialog(specificSumRequested = true, message = message, sum = sum)
        } else {
            FloaterBuyCurrency.buyCurrency(message, sum)
        }
    }

    fun showDialog(specificSumRequested: Boolean, message: String, sum: Int) {
        val floater = FloaterReg.getInstance("buy_currency_html") as? FloaterBuyCurrencyHtml
        if (floater != null) {
            floater.setParams(specificSumRequested, Tea.wrapCurrency(message), sum)
            floater.navigateToFinalUrl()
            floater.isVisible = true
            floater.setFrontmost(takeFocus = true)
            floater.center()
        } else {
            Logger.warn("Buy Currency (HTML) Floater not found")
        }
    }

    fun closeDialog() {
        val floater = FloaterReg.getInstance("buy_currency_html") as? FloaterBuyCurrencyHtml
        floater?.closeFloater()
        StatusBar.sendMoneyBalanceRequest()
    }
}

class BuyCurrencyHtmlHandler : CommandHandler("buycurrencyhtml", TrustLevel.UNTRUSTED_THROTTLE) {

    override fun handle(params: LLSD, queryMap: LLSD, grid: String, web: MediaCtrl?): Boolean {
        val action = if (params.size() >= 1) params[0].asString() else ""
        val nextAction = if (params.size() >= 2) params[1].asString() else ""
        val resultCode = if (params.size() >= 3) params[2].asInt() else 0

        if (resultCode != 0) {
            Logger.warn("Received nonzero result code: $resultCode")
        }

        if (nextAction == "open_legacy") {
            FloaterBuyCurrency.buyCurrency()
        }

        if (action == "close") {
            BuyCurrencyHtml.closeDialog()
        }

        return true
    }
}
