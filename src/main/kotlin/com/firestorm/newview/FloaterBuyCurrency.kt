package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.TextBox
import com.firestorm.llui.LayoutPanel
import com.firestorm.llui.LayoutStack
import com.firestorm.llui.IconCtrl

private const val MINIMUM_BALANCE_AMOUNT: Int = 0

class FloaterBuyCurrencyUI(key: Any) : Floater(key) {

    private var hasTarget: Boolean = false
    private var targetPrice: Int = 0
    private var requiredAmount: Int = 0

    private val manager: CurrencyUIManager = CurrencyUIManager(this)

    fun noTarget() {
        hasTarget = false
        targetPrice = 0
        manager.setAmount(0)
    }

    fun target(name: String, price: Int) {
        hasTarget = true
        targetPrice = price
        if (name.isNotEmpty()) {
            getChild<Any>("target_price_label")?.setValue(name)
        }
        val balance = TODO("APR: use JVM equivalent - gStatusBar->getBalance()") as? Int ?: 0
        val need = maxOf(0, price - balance)
        requiredAmount = need + MINIMUM_BALANCE_AMOUNT
        manager.setAmount(0)
    }

    override fun postBuild(): Boolean {
        manager.prepare()

        getChild<Any>("buy_btn")?.setCommitCallback { onClickBuy() }
        getChild<Any>("cancel_btn")?.setCommitCallback { onClickCancel() }

        center()
        updateUI()
        return true
    }

    override fun draw() {
        if (manager.process()) {
            if (manager.bought()) {
                TODO("APR: use JVM equivalent - LLNotificationsUtil::add(BuyLindenDollarSuccess)")
                closeFloater()
                return
            }
            updateUI()
        }
        getChildView("buy_btn")?.setEnabled(manager.canBuy())
        super.draw()
    }

    fun canClose(): Boolean = manager.canCancel()

    fun updateUI() {
        val hasError = manager.hasError()
        manager.updateUI(!hasError && !manager.buying())

        getChildView("info_buying")?.setVisible(false)
        getChildView("info_need_more")?.setVisible(false)
        getChildView("purchase_warning_repurchase")?.setVisible(false)
        getChildView("purchase_warning_notenough")?.setVisible(false)
        getChildView("contacting")?.setVisible(false)

        if (hasError) {
            TODO("APR: use JVM equivalent - LLNotificationsUtil::add CouldNotBuyCurrency")
            manager.clearError()
            closeFloater()
        } else {
            getChildView("normal_background")?.setVisible(true)

            if (hasTarget) {
                getChildView("info_need_more")?.setVisible(true)
            } else {
                getChildView("info_buying")?.setVisible(true)
            }

            if (manager.buying()) {
                getChildView("contacting")?.setVisible(true)
            } else if (hasTarget) {
                getChild<Any>("target_price")?.setTextArg("[AMT]", targetPrice.toString())
                getChild<Any>("required_amount")?.setTextArg("[AMT]", requiredAmount.toString())
            }

            val balance = TODO("APR: use JVM equivalent - gStatusBar->getBalance()") as? Int ?: 0
            getChildView("balance_label")?.setVisible(true)
            getChildView("balance_amount")?.setVisible(true)
            getChild<Any>("balance_amount")?.setTextArg("[AMT]", balance.toString())

            val buying = manager.getAmount()
            getChildView("buying_label")?.setVisible(true)
            getChildView("buying_amount")?.setVisible(true)
            getChild<Any>("buying_amount")?.setTextArg("[AMT]", buying.toString())

            val total = balance + buying
            getChildView("total_label")?.setVisible(true)
            getChildView("total_amount")?.setVisible(true)
            getChild<Any>("total_amount")?.setTextArg("[AMT]", total.toString())

            if (hasTarget) {
                val currencyLinksVisible = getChildView("currency_links")?.getVisible() ?: false
                getChildView("purchase_warning_repurchase")?.setVisible(!currencyLinksVisible)
            }
        }

        val currencyEstVisible = getChildView("currency_est")?.getVisible() ?: false
        getChildView("getting_data")?.setVisible(!manager.canBuy() && !hasError && !currencyEstVisible)
    }

    fun collapsePanels(collapse: Boolean) {
        val pricePanel = getChild<LayoutPanel>("layout_panel_price") ?: return
        if (pricePanel.isCollapsed() == collapse) return

        val outerStack = getChild<LayoutStack>("outer_stack") ?: return
        val requiredPanel = getChild<LayoutPanel>("layout_panel_required") ?: return
        val msgPanel = getChild<LayoutPanel>("layout_panel_msg") ?: return

        var deltaHeight = pricePanel.getRect().height + requiredPanel.getRect().height + msgPanel.getRect().height
        deltaHeight *= if (collapse) -1 else 1

        val icon = getChild<IconCtrl>("normal_background")
        val rect = icon?.getRect()
        if (rect != null) {
            icon.setRect(rect.setOriginAndSize(rect.left, rect.bottom - deltaHeight, rect.width, rect.height + deltaHeight))
        }

        outerStack.collapsePanel(pricePanel, collapse)
        outerStack.collapsePanel(requiredPanel, collapse)
        outerStack.collapsePanel(msgPanel, collapse)
        outerStack.updateLayout()

        val floaterRect = getRect()
        floaterRect.bottom -= deltaHeight
        setShape(floaterRect, false)
    }

    private fun onClickBuy() {
        manager.buy(getString("buy_currency"))
        updateUI()
        TODO("APR: use JVM equivalent - LLStatusBar::sendMoneyBalanceRequest")
    }

    private fun onClickCancel() {
        closeFloater()
        TODO("APR: use JVM equivalent - LLStatusBar::sendMoneyBalanceRequest")
    }
}

class FetchAvatarPaymentInfo(
    private val hasTarget: Boolean,
    private val name: String = "",
    private val price: Int = 0
) {
    private val avatarId: String = TODO("APR: use JVM equivalent - gAgent.getID()") as String

    init {
        val processor = TODO("APR: use JVM equivalent - LLAvatarPropertiesProcessor::getInstance()") as? Any
        // registers observer and sends properties request
        TODO("APR: use JVM equivalent - processor->addObserver / sendAvatarPropertiesRequest")
    }

    fun processProperties(data: Any?, type: Int) {
        TODO("APR: use JVM equivalent - EAvatarProcessorType dispatch")
    }
}

object FloaterBuyCurrency {

    var propertiesRequest: FetchAvatarPaymentInfo? = null

    fun buyCurrency() {
        propertiesRequest = FetchAvatarPaymentInfo(hasTarget = false)
    }

    fun buyCurrency(name: String, price: Int) {
        propertiesRequest = FetchAvatarPaymentInfo(hasTarget = true, name = name, price = price)
    }

    fun handleBuyCurrency(hasPiof: Boolean, hasTarget: Boolean, name: String, price: Int) {
        propertiesRequest = null
        if (hasPiof) {
            val ui = TODO("APR: use JVM equivalent - LLFloaterReg::showTypedInstance<FloaterBuyCurrencyUI>") as FloaterBuyCurrencyUI
            if (hasTarget) {
                ui.target(name, price)
            } else {
                ui.noTarget()
            }
            ui.updateUI()
            ui.collapsePanels(!hasTarget)
        } else {
            TODO("APR: use JVM equivalent - LLFloaterReg::showInstance(add_payment_method)")
        }
    }

    fun buildFloater(key: Any): Floater = FloaterBuyCurrencyUI(key)

    fun updateCurrencySymbols() {
        val fbc = TODO("APR: use JVM equivalent - LLFloaterReg::findInstance(buy_currency)") as? FloaterBuyCurrencyUI
            ?: return
        fbc.updateCurrencySymbols()
        val labelNames = listOf(
            "info_need_more", "info_buying", "target_price", "balance_amount",
            "required_amount", "currency_label", "total_amount", "purchase_warning_repurchase"
        )
        for (name in labelNames) {
            (fbc.findChild<TextBox>(name))?.updateCurrencySymbols()
        }
    }
}
