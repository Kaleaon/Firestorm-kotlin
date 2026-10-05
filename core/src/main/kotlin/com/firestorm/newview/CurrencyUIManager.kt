package com.firestorm.newview

import com.firestorm.ui.Panel
import com.firestorm.ui.LineEditor
import com.firestorm.agent.Agent
import com.firestorm.network.GridManager
import com.firestorm.network.XmlRpcTransaction

private const val CURRENCY_ESTIMATE_FREQUENCY: Double = 2.0

class CurrencyUIManager(panel: Panel) {

    private val impl = Impl(panel)

    fun setAmount(amount: Int, noEstimate: Boolean = false) {
        impl.userCurrencyBuy = amount
        impl.userEnteredCurrencyBuy = false
        impl.updateUI()
        impl.currencyChanged = !noEstimate
    }

    fun getAmount(): Int = impl.userCurrencyBuy

    fun setZeroMessage(message: String) {
        impl.zeroMessage = message
    }

    @Deprecated("Deprecated since viewer 2.0; use setLocalEstimate")
    fun setUsdEstimate(amount: Int) {
        impl.usdCurrencyEstimatedCost = amount
        impl.usdCurrencyEstimated = true
        impl.updateUI()
        impl.currencyChanged = false
    }

    @Deprecated("Deprecated since viewer 2.0; use getLocalEstimate")
    fun getUsdEstimate(): Int =
        if (impl.usdCurrencyEstimated) impl.usdCurrencyEstimatedCost else 0

    fun setLocalEstimate(localEst: String) {
        impl.localCurrencyEstimatedCost = localEst
        impl.localCurrencyEstimated = true
        impl.updateUI()
        impl.currencyChanged = false
    }

    fun getLocalEstimate(): String = impl.getLocalEstimate()

    fun prepare() = impl.prepare()

    fun updateUI(show: Boolean = true) {
        impl.hidden = !show
        impl.updateUI()
    }

    fun process(): Boolean {
        var changed = false
        changed = changed or impl.checkTransaction()
        changed = changed or impl.considerUpdateCurrency()
        return changed
    }

    fun buy(buyMsg: String) {
        if (!canBuy()) return

        val msg = buyMsg
            .replace("[LINDENS]", impl.userCurrencyBuy.toString())
            .replace("[LOCALAMOUNT]", getLocalEstimate())

        ConfirmationManager.confirm(
            impl.siteConfirm,
            msg,
            impl,
            Impl::startCurrencyBuy
        )
    }

    fun inProcess(): Boolean = impl.transactionType != Impl.TransactionType.NONE
    fun canCancel(): Boolean = !buying()
    fun canBuy(): Boolean = !inProcess() && impl.hasEstimate() && impl.userCurrencyBuy > 0
    fun buying(): Boolean = impl.transactionType == Impl.TransactionType.BUY
    fun bought(): Boolean = impl.bought

    fun clearError() = impl.clearError()
    fun hasError(): Boolean = impl.error
    fun errorMessage(): String = impl.errorMessage
    fun errorUri(): String = impl.errorUri

    private class Impl(val panel: Panel) {

        var hidden: Boolean = false
        var error: Boolean = false
        var errorMessage: String = ""
        var errorUri: String = ""
        var zeroMessage: String = ""

        var userCurrencyBuy: Int = 2000
        var userEnteredCurrencyBuy: Boolean = false

        var usdCurrencyEstimated: Boolean = false
        var usdCurrencyEstimatedCost: Int = 0
        var localCurrencyEstimated: Boolean = false
        var localCurrencyEstimatedCost: String = "0"
        var supportsInternationalBilling: Boolean = false
        var siteConfirm: String = ""

        var bought: Boolean = false

        enum class TransactionType { NONE, CURRENCY, BUY }

        var transactionType: TransactionType = TransactionType.NONE
        var transaction: XmlRpcTransaction? = null

        var currencyChanged: Boolean = false
        var currencyKeyTimerElapsed: Double = 0.0

        init {
            clearEstimate()
        }

        fun updateCurrencyInfo() {
            clearEstimate()
            bought = false
            currencyChanged = false

            if (userCurrencyBuy == 0) {
                localCurrencyEstimated = true
                return
            }

            val params = mutableMapOf<String, Any>(
                "agentId" to Agent.instance.id.toString(),
                "secureSessionId" to Agent.instance.secureSessionId.toString(),
                "language" to UI.getLanguage(),
                "currencyBuy" to userCurrencyBuy,
                "viewerChannel" to VersionInfo.channel,
                "viewerMajorVersion" to VersionInfo.major,
                "viewerMinorVersion" to VersionInfo.minor,
                "viewerPatchVersion" to VersionInfo.patch,
                "viewerBuildVersion" to VersionInfo.build.toString()
            )
            startTransaction(TransactionType.CURRENCY, "getCurrencyQuote", params)
        }

        fun finishCurrencyInfo() {
            val result = transaction!!.response()
            if (!result["success"].asBoolean()) {
                setError(result["errorMessage"].asString(), result["errorURI"].asString())
                return
            }

            val currency = result["currency"]

            usdCurrencyEstimated = currency.has("estimatedCost")
            if (usdCurrencyEstimated) {
                usdCurrencyEstimatedCost = currency["estimatedCost"].asInt()
            }

            localCurrencyEstimated = currency.has("estimatedLocalCost")
            if (localCurrencyEstimated) {
                localCurrencyEstimatedCost = currency["estimatedLocalCost"].asString()
                supportsInternationalBilling = true
            }

            val newCurrencyBuy = currency["currencyBuy"].asInt()
            if (newCurrencyBuy != userCurrencyBuy) {
                userCurrencyBuy = newCurrencyBuy
                userEnteredCurrencyBuy = false
            }

            siteConfirm = result["confirm"].asString()
        }

        fun startCurrencyBuy(password: String) {
            val params = mutableMapOf<String, Any>(
                "agentId" to Agent.instance.id.toString(),
                "secureSessionId" to Agent.instance.secureSessionId.toString(),
                "language" to UI.getLanguage(),
                "currencyBuy" to userCurrencyBuy,
                "confirm" to siteConfirm,
                "viewerChannel" to VersionInfo.channel,
                "viewerMajorVersion" to VersionInfo.major,
                "viewerMinorVersion" to VersionInfo.minor,
                "viewerPatchVersion" to VersionInfo.patch,
                "viewerBuildVersion" to VersionInfo.build.toString()
            )

            if (usdCurrencyEstimated) params["estimatedCost"] = usdCurrencyEstimatedCost
            if (localCurrencyEstimated) params["estimatedLocalCost"] = localCurrencyEstimatedCost
            if (password.isNotEmpty()) params["password"] = password

            startTransaction(TransactionType.BUY, "buyCurrency", params)
            clearEstimate()
            currencyChanged = false
        }

        fun finishCurrencyBuy() {
            val result = transaction!!.response()
            if (!result["success"].asBoolean()) {
                setError(result["errorMessage"].asString(), result["errorURI"].asString())
            } else {
                userCurrencyBuy = 0
                userEnteredCurrencyBuy = false
                bought = true
            }
        }

        fun startTransaction(type: TransactionType, method: String, params: Map<String, Any>) {
            val uri = GridManager.instance.helperUri + "currency.php"
            transaction = XmlRpcTransaction(uri, method, params)
            transactionType = type
            clearError()
        }

        fun clearEstimate() {
            usdCurrencyEstimated = false
            usdCurrencyEstimatedCost = 0
            localCurrencyEstimated = false
            localCurrencyEstimatedCost = "0"
        }

        fun hasEstimate(): Boolean = usdCurrencyEstimated || localCurrencyEstimated

        fun getLocalEstimate(): String {
            if (localCurrencyEstimated) return localCurrencyEstimatedCost
            if (usdCurrencyEstimated) {
                val amount = usdCurrencyEstimatedCost / 100.0
                return Trans.getString("LocalEstimateUSD", mapOf("[AMOUNT]" to "${"%.2f".format(amount)}"))
            }
            return ""
        }

        fun checkTransaction(): Boolean {
            val tx = transaction ?: return false
            if (!tx.process()) return false

            if (tx.status() != XmlRpcTransaction.Status.COMPLETE) {
                setError(tx.statusMessage(), tx.statusUri())
            } else {
                when (transactionType) {
                    TransactionType.CURRENCY -> finishCurrencyInfo()
                    TransactionType.BUY -> finishCurrencyBuy()
                    TransactionType.NONE -> Unit
                }
            }

            transaction = null
            transactionType = TransactionType.NONE
            return true
        }

        fun setError(message: String, uri: String) {
            error = true
            errorMessage = message
            errorUri = uri
        }

        fun clearError() {
            error = false
            errorMessage = ""
            errorUri = ""
        }

        fun considerUpdateCurrency(): Boolean {
            if (currencyChanged && transaction == null &&
                currencyKeyTimerElapsed >= CURRENCY_ESTIMATE_FREQUENCY
            ) {
                updateCurrencyInfo()
                return true
            }
            return false
        }

        fun currencyKey(value: Int) {
            userEnteredCurrencyBuy = true
            currencyKeyTimerElapsed = 0.0

            if (userCurrencyBuy == value) return

            userCurrencyBuy = value
            if (hasEstimate()) {
                clearEstimate()
                panel.getChildView("currency_est").isVisible = false
                panel.getChildView("getting_data").isVisible = true
            }
            currencyChanged = true
        }

        fun onCurrencyKey(caller: LineEditor) {
            currencyKey(caller.getText().toIntOrNull() ?: 0)
        }

        fun prepare() {
            val lindenAmount = panel.getChild<LineEditor>("currency_amt")
            lindenAmount?.setPrevalidate(TextValidate::validateNonNegativeInt)
            lindenAmount?.setKeystrokeCallback { caller -> onCurrencyKey(caller) }
        }

        fun updateUI() {
            if (hidden) {
                panel.getChildView("currency_action").isVisible = false
                panel.getChildView("currency_amt").isVisible = false
                panel.getChildView("currency_est").isVisible = false
                return
            }

            panel.getChildView("currency_action").isVisible = true

            val lindenAmount = panel.getChild<LineEditor>("currency_amt")
            if (lindenAmount != null) {
                lindenAmount.isVisible = true
                lindenAmount.setLabel(zeroMessage)

                if (!userEnteredCurrencyBuy) {
                    lindenAmount.setText(if (userCurrencyBuy == 0) "" else userCurrencyBuy.toString())
                    lindenAmount.selectAll()
                }
            }

            val estimated = if (userCurrencyBuy == 0) panel.getString("estimated_zero") else getLocalEstimate()
            panel.getChild<UICtrl>("currency_est").setTextArg("[LOCALAMOUNT]", estimated)
            panel.getChildView("currency_est").isVisible = hasEstimate() || userCurrencyBuy == 0

            panel.getChildView("currency_links").isVisible = supportsInternationalBilling
            panel.getChildView("exchange_rate_note").isVisible = supportsInternationalBilling

            val buyBtnEnabled = panel.getChildView("buy_btn").isEnabled
            val estVisible = panel.getChildView("currency_est").isVisible
            val errorWebVisible = panel.getChildView("error_web").isVisible
            if (buyBtnEnabled || estVisible || errorWebVisible) {
                panel.getChildView("getting_data").isVisible = false
            }
        }
    }
}
