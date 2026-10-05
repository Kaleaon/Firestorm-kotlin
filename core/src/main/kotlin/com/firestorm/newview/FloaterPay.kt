package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.Button
import com.firestorm.llui.LineEditor
import com.firestorm.llui.TextBox
import com.firestorm.llui.UICtrl
import java.util.UUID

typealias MoneyCallback = (targetId: UUID, region: Any?, amount: Int, isGroup: Boolean, transactionType: Int, description: String) -> Unit

private const val PAY_BUTTON_DEFAULT_0 = 1
private const val PAY_BUTTON_DEFAULT_1 = 5
private const val PAY_BUTTON_DEFAULT_2 = 10
private const val PAY_BUTTON_DEFAULT_3 = 20
private const val PAY_PRICE_HIDE       = -1
private const val PAY_PRICE_DEFAULT    = -2
private const val MAX_PAY_BUTTONS      = 4
private const val FASTPAY_BUTTON_WIDTH = 80
private const val TRANS_PAY_OBJECT     = 5000
private const val TRANS_GIFT           = 5001

private data class GiveMoneyInfo(
    val floater: FloaterPay,
    var amount: Int
)

class FloaterPay(key: Any) : Floater(key) {

    private val callbackData: MutableList<GiveMoneyInfo>   = mutableListOf()
    private var callback: MoneyCallback?                   = null
    private var objectNameText: TextBox?                   = null
    private var targetUUID: UUID                           = UUID.fromString(key.toString())
    private var targetIsGroup: Boolean                     = false
    private var haveName: Boolean                          = false
    private val quickPayButton: Array<Button?>             = arrayOfNulls(MAX_PAY_BUTTONS)
    private val quickPayInfo: Array<GiveMoneyInfo?>        = arrayOfNulls(MAX_PAY_BUTTONS)
    private var objectSelection: Any?                      = null

    companion object {
        private var lastAmount: Int = 0

        fun payViaObject(callback: MoneyCallback, selection: Any?) {
            System.err.println("FloaterPay: payViaObject not yet implemented")
        }

        fun payDirectly(callback: MoneyCallback, targetId: UUID, isGroup: Boolean) {
            System.err.println("FloaterPay: payDirectly not yet implemented")
        }

        private fun payConfirmationCallback(notification: Any, response: Any, info: GiveMoneyInfo): Boolean {
            System.err.println("FloaterPay: payConfirmationCallback not yet implemented")
            return false
        }

        private fun processPayPriceReply(msg: Any) {
            System.err.println("FloaterPay: processPayPriceReply not yet implemented")
        }

        private fun getPrimaryObject(selection: Any?): Any?           = null
        private fun getFirstRootNode(selection: Any?): Any?           = null
        private fun getOwnership(node: Any): Pair<UUID, Boolean>      = Pair(UUID.randomUUID(), false)
        private fun getName(node: Any): String                        = ""
    }

    override fun postBuild(): Boolean {
        var i = 0

        var info = GiveMoneyInfo(this, PAY_BUTTON_DEFAULT_0)
        callbackData.add(info)
        childSetAction("fastpay 1") { onGive(info) }
        getChildView("fastpay 1").setVisible(false)
        quickPayButton[i] = getChild("fastpay 1")
        quickPayInfo[i]   = info
        i++

        info = GiveMoneyInfo(this, PAY_BUTTON_DEFAULT_1)
        callbackData.add(info)
        childSetAction("fastpay 5") { onGive(info) }
        getChildView("fastpay 5").setVisible(false)
        quickPayButton[i] = getChild("fastpay 5")
        quickPayInfo[i]   = info
        i++

        info = GiveMoneyInfo(this, PAY_BUTTON_DEFAULT_2)
        callbackData.add(info)
        childSetAction("fastpay 10") { onGive(info) }
        getChildView("fastpay 10").setVisible(false)
        quickPayButton[i] = getChild("fastpay 10")
        quickPayInfo[i]   = info
        i++

        info = GiveMoneyInfo(this, PAY_BUTTON_DEFAULT_3)
        callbackData.add(info)
        childSetAction("fastpay 20") { onGive(info) }
        getChildView("fastpay 20").setVisible(false)
        quickPayButton[i] = getChild("fastpay 20")
        quickPayInfo[i]   = info

        getChildView("amount text").setVisible(false)
        getChildView("amount").setVisible(false)

        val amountEditor = getChild<LineEditor>("amount")
        amountEditor.setKeystrokeCallback { onKeystroke() }
        amountEditor.setPrevalidate("NON_NEGATIVE_S32")
        if (lastAmount > 0) amountEditor.setValue(lastAmount.toString())

        val payInfo = GiveMoneyInfo(this, 0)
        callbackData.add(payInfo)
        childSetAction("pay btn") { onGive(payInfo) }
        setDefaultBtn("pay btn")
        getChildView("pay btn").setVisible(false)
        getChildView("pay btn").setEnabled(lastAmount > 0)

        childSetAction("cancel btn") { onCancel() }

        return true
    }

    override fun onClose(appQuitting: Boolean) {
        objectSelection = null
    }

    fun setCallback(cb: MoneyCallback) {
        callback = cb
    }

    private fun finishPayUI(targetId: UUID, isGroup: Boolean) {
        val slurl: String = ""
        setTitle(if (isGroup) getString("payee_group") else getString("payee_resident"))
        getChild<TextBox>("payee_name").setText(slurl)

        val amountEditor = getChild<LineEditor>("amount")
        amountEditor.setFocus(true)
        amountEditor.selectAll()

        targetIsGroup = isGroup
    }

    private fun onCancel() {
        closeFloater()
    }

    private fun onKeystroke() {
        val amtStr = getChild<UICtrl>("amount").getValue().toString()
        getChildView("pay btn").setEnabled(amtStr.isNotEmpty() && (amtStr.toIntOrNull() ?: 0) > 0)
    }

    private fun onGive(info: GiveMoneyInfo) {
        System.err.println("FloaterPay: onGive not yet implemented")
    }

    private fun give(amountIn: Int) {
        System.err.println("FloaterPay: give not yet implemented")
    }

    private fun getFirstRootNodeLocal(sel: Any?): Any? {
        System.err.println("FloaterPay: mObjectSelection->getFirstRootNode() not yet implemented")
        return null
    }
}

object FloaterPayUtil {
    fun registerFloater() {
        System.err.println("FloaterPayUtil: register pay_resident and pay_object with FloaterReg using FloaterPay builder not yet implemented")
    }

    fun payViaObject(callback: MoneyCallback, selection: Any?) {
        FloaterPay.payViaObject(callback, selection)
    }

    fun payDirectly(callback: MoneyCallback, targetId: UUID, isGroup: Boolean) {
        FloaterPay.payDirectly(callback, targetId, isGroup)
    }
}
