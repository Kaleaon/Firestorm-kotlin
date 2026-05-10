package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.Button
import com.firestorm.llui.LineEditor
import com.firestorm.llui.TextBox
import com.firestorm.llui.UICtrl
import com.firestorm.llmessage.MessageSystem
import com.firestorm.llcommon.LLUUID

// Callback invoked when the L$ transfer is confirmed.
// Parameters: targetId, region, amount, isGroup, transactionType, description
typealias MoneyCallback = (LLUUID, Any?, Int, Boolean, Int, String) -> Unit

// LL LSL constant equivalents for pay button defaults and price signals.
private const val PAY_BUTTON_DEFAULT_0 = 1
private const val PAY_BUTTON_DEFAULT_1 = 5
private const val PAY_BUTTON_DEFAULT_2 = 10
private const val PAY_BUTTON_DEFAULT_3 = 20
private const val PAY_PRICE_HIDE       = -1
private const val PAY_PRICE_DEFAULT    = -2
private const val MAX_PAY_BUTTONS      = 4
private const val FASTPAY_BUTTON_WIDTH = 80

private const val TRANS_PAY_OBJECT = 5000
private const val TRANS_GIFT       = 5001

// Bundles the floater reference with a pre-set amount for a quick-pay button.
private data class GiveMoneyInfo(val floater: FloaterPay, val amount: Int)

class FloaterPay(seed: Any) : Floater(seed) {

    private var callback:         MoneyCallback? = null
    private var objectNameText:   TextBox?        = null
    private val targetUuid:       LLUUID          = TODO("extract UUID from seed LLSD")
    private var targetIsGroup:    Boolean         = false
    private var haveName:         Boolean         = false

    private val callbackData: MutableList<GiveMoneyInfo> = mutableListOf()
    private val quickPayButton: Array<Button?> = arrayOfNulls(MAX_PAY_BUTTONS)
    private val quickPayInfo:   Array<GiveMoneyInfo?> = arrayOfNulls(MAX_PAY_BUTTONS)

    private var objectSelection: Any? = null   // LLObjectSelection handle

    override fun postBuild(): Boolean {
        var i = 0
        val defaults = intArrayOf(
            PAY_BUTTON_DEFAULT_0,
            PAY_BUTTON_DEFAULT_1,
            PAY_BUTTON_DEFAULT_2,
            PAY_BUTTON_DEFAULT_3
        )
        val uiNames = arrayOf("fastpay 1", "fastpay 5", "fastpay 10", "fastpay 20")

        for (idx in defaults.indices) {
            val info = GiveMoneyInfo(this, defaults[idx])
            callbackData.add(info)
            quickPayButton[i] = findChild(uiNames[idx])
            quickPayInfo[i]   = info
            quickPayButton[i]?.setVisible(false)
            quickPayButton[i]?.setOnClick { onGive(info) }
            i++
        }

        findChild<UICtrl>("amount text")?.setVisible(false)
        findChild<UICtrl>("amount")?.setVisible(false)

        findChild<LineEditor>("amount")?.apply {
            setOnKeystroke { onKeystroke() }
            setPrevalidate { s -> s.toIntOrNull()?.let { it >= 0 } ?: false }
            if (lastAmount > 0) setValue(lastAmount.toString())
        }

        val payInfo = GiveMoneyInfo(this, 0)
        callbackData.add(payInfo)
        findChild<UICtrl>("pay btn")?.apply {
            setVisible(false)
            setEnabled(lastAmount > 0)
            setOnClick { onGive(payInfo) }
        }
        setDefaultButton("pay btn")

        findChild<UICtrl>("cancel btn")?.setOnClick { closeFloater() }

        return true
    }

    override fun onClose(appQuitting: Boolean) {
        objectSelection = null
    }

    fun setCallback(cb: MoneyCallback) { callback = cb }

    private fun give(amount: Int) {
        val cb = callback ?: return
        var actualAmount = amount
        if (actualAmount == 0) {
            actualAmount = findChild<UICtrl>("amount")?.getValue()?.toString()?.toIntOrNull() ?: return
        }
        lastAmount = actualAmount

        if (objectSelection != null) {
            TODO("APR: find dest object in gObjectList, determine region, send payment via callback")
        } else {
            val paymentMessage = findChild<LineEditor>("payment_message")?.getValue()?.toString() ?: ""
            cb(
                targetUuid,
                TODO("APR: gAgent.getRegion()"),
                actualAmount,
                targetIsGroup,
                TRANS_GIFT,
                paymentMessage
            )
            TODO("APR: LLMuteList::autoRemove(targetUuid, AR_MONEY)")
        }
    }

    private fun onGive(info: GiveMoneyInfo) {
        var amount = info.amount
        if (amount == 0) {
            amount = findChild<UICtrl>("amount")?.getValue()?.toString()?.toIntOrNull() ?: return
        }

        val confirmPayments = TODO<Boolean>("APR: gSavedSettings.getBOOL(\"FSConfirmPayments\")")
        val threshold       = TODO<Int>("APR: gSavedSettings.getS32(\"FSPaymentConfirmationThreshold\")")
        val balance         = TODO<Int>("APR: gStatusBar.getBalance()")

        if (confirmPayments && amount > threshold && balance >= amount) {
            var payeeId  = LLUUID.NULL
            var isGroup  = false

            if (objectSelection != null) {
                val node = TODO<Any?>("APR: objectSelection.getFirstRootNode()")
                if (node == null) {
                    TODO("APR: LLNotificationsUtil::add(\"PayObjectFailed\"); closeFloater()")
                    return
                }
                TODO("APR: node.mPermissions.getOwnership(payeeId, isGroup)")
            } else {
                isGroup  = targetIsGroup
                payeeId  = targetUuid
            }

            // Skip the confirmation dialog when paying yourself.
            if (!isGroup && payeeId == TODO("APR: gAgent.getID()")) {
                give(amount)
                closeFloater()
                return
            }

            TODO("APR: LLNotificationsUtil::add(\"PayConfirmation\", args, LLSD(), payConfirmationCallback)")
        } else {
            give(amount)
            closeFloater()
        }
    }

    private fun onKeystroke() {
        val text = findChild<UICtrl>("amount")?.getValue()?.toString() ?: ""
        findChild<UICtrl>("pay btn")?.setEnabled(text.isNotEmpty() && (text.toIntOrNull() ?: 0) > 0)
    }

    private fun finishPayUi(targetId: LLUUID, isGroup: Boolean) {
        val slurl: String
        if (isGroup) {
            setTitle(getString("payee_group"))
            slurl = TODO("APR: LLSLURL(\"group\", targetId, \"inspect\").getSLURLString()")
        } else {
            setTitle(getString("payee_resident"))
            slurl = TODO("APR: LLSLURL(\"agent\", targetId, \"inspect\").getSLURLString()")
        }
        findChild<TextBox>("payee_name")?.setText(slurl)

        findChild<LineEditor>("amount")?.apply {
            setFocus(true)
            selectAll()
        }
        targetIsGroup = isGroup
    }

    // Network reply handler – called when the simulator sends pay-price info
    // for an in-world object.
    fun processPayPriceReply(msg: MessageSystem) {
        val target = TODO<LLUUID>("APR: msg.getUUID(\"ObjectData\", \"ObjectID\")")
        if (target != targetUuid) return

        val price = TODO<Int>("APR: msg.getS32(\"ObjectData\", \"DefaultPayPrice\")")
        when (price) {
            PAY_PRICE_HIDE -> {
                findChild<UICtrl>("amount")?.setVisible(false)
                findChild<UICtrl>("pay btn")?.setVisible(false)
                findChild<UICtrl>("amount text")?.setVisible(false)
            }
            PAY_PRICE_DEFAULT -> {
                findChild<UICtrl>("amount")?.setVisible(true)
                findChild<UICtrl>("pay btn")?.setVisible(true)
                findChild<UICtrl>("amount text")?.setVisible(true)
            }
            else -> {
                findChild<UICtrl>("amount")?.setVisible(true)
                findChild<UICtrl>("pay btn")?.apply { setVisible(true); setEnabled(true) }
                findChild<UICtrl>("amount text")?.setVisible(true)
                findChild<UICtrl>("amount")?.setValue(kotlin.math.abs(price).toString())
            }
        }

        var numBlocks = TODO<Int>("APR: msg.getNumberOfBlocks(\"ButtonData\")")
        if (numBlocks > MAX_PAY_BUTTONS) numBlocks = MAX_PAY_BUTTONS

        var maxPayAmount = 0
        for (idx in 0 until numBlocks) {
            val payButton = TODO<Int>("APR: msg.getS32(\"ButtonData\", \"PayButton\", idx)")
            if (payButton > 0) {
                val label = "L\$${payButton}"
                quickPayButton[idx]?.apply {
                    setLabel(label)
                    setVisible(true)
                }
                quickPayInfo[idx] = quickPayInfo[idx]?.copy(amount = payButton)
                if (payButton > maxPayAmount) maxPayAmount = payButton
            } else {
                quickPayButton[idx]?.setVisible(false)
            }
        }
        for (idx in numBlocks until MAX_PAY_BUTTONS) {
            quickPayButton[idx]?.setVisible(false)
        }

        TODO("APR: resize button widths based on maxPayAmount digit count, " +
             "mirror C++ padding_required / button_delta logic; " +
             "call reshape() if max >= 100000")
    }

    // Stubs for UI helper calls resolved at runtime by the platform layer.
    @Suppress("UNCHECKED_CAST")
    private fun <T> findChild(name: String): T? = null
    private fun getString(key: String): String = TODO("APR: XUI string lookup for key=$key")
    private fun setTitle(title: String) { TODO("APR: Floater.setTitle(title)") }
    private fun setDefaultButton(name: String) { TODO("APR: Floater.setDefaultBtn(name)") }
    private fun closeFloater() { TODO("APR: Floater.closeFloater()") }

    companion object {
        // Remembered across floater instances so the amount field is pre-filled.
        var lastAmount: Int = 0
            private set

        fun registerFloater() {
            TODO("APR: FloaterReg.add(\"pay_resident\", \"floater_pay.xml\", ::FloaterPay)")
            TODO("APR: FloaterReg.add(\"pay_object\",   \"floater_pay_object.xml\", ::FloaterPay)")
        }

        fun payViaObject(callback: MoneyCallback, selection: Any?) {
            val obj = TODO<Any?>("APR: selection.getPrimaryObject()")
            if (obj == null) return

            val floater = TODO<FloaterPay?>("APR: FloaterReg.showTypedInstance<FloaterPay>(\"pay_object\", objId)")
            if (floater == null) return

            floater.setCallback(callback)
            floater.objectSelection = selection

            val node = TODO<Any?>("APR: selection.getFirstRootNode()")
            if (node == null) {
                TODO("APR: LLNotificationsUtil::add(\"PayObjectFailed\"); floater.closeFloater()")
                return
            }

            TODO("APR: send RequestPayPrice message to object's region host")
            TODO("APR: msg.setHandlerFuncFast(PayPriceReply, floater.processPayPriceReply)")

            val ownerId = TODO<LLUUID>("APR: node.mPermissions.getOwner()")
            val isGroup = TODO<Boolean>("APR: node.mPermissions.isGroupOwned()")
            floater.findChild<UICtrl>("object_name_text")?.setValue(TODO("APR: node.mName"))
            floater.finishPayUi(ownerId, isGroup)
        }

        fun payDirectly(callback: MoneyCallback, targetId: LLUUID, isGroup: Boolean) {
            val floater = TODO<FloaterPay?>("APR: FloaterReg.showTypedInstance<FloaterPay>(\"pay_resident\", targetId)")
            if (floater == null) return

            floater.setCallback(callback)
            floater.objectSelection = null

            floater.findChild<UICtrl>("amount")?.setVisible(true)
            floater.findChild<UICtrl>("pay btn")?.setVisible(true)
            floater.findChild<UICtrl>("amount text")?.setVisible(true)

            // Honour RLV IM restriction: disable the message field if the agent
            // cannot send IMs to this target.
            val canSendIm = TODO<Boolean>("APR: RlvActions::canSendIM(targetId)")
            floater.findChild<UICtrl>("payment_message")?.setEnabled(canSendIm)

            for (i in 0 until MAX_PAY_BUTTONS) {
                floater.quickPayButton[i]?.setVisible(true)
            }

            floater.finishPayUi(targetId, isGroup)
        }

        private fun payConfirmationCallback(notification: Any, response: Any, info: GiveMoneyInfo): Boolean {
            val option = TODO<Int>("APR: LLNotificationsUtil::getSelectedOption(notification, response)")
            if (option == 0) {
                info.floater.give(info.amount)
                info.floater.closeFloater()
            }
            return false
        }
    }
}

// Top-level utility object mirrors the C++ LLFloaterPayUtil namespace.
object FloaterPayUtil {
    fun registerFloater()                                            = FloaterPay.registerFloater()
    fun payViaObject(cb: MoneyCallback, selection: Any?)             = FloaterPay.payViaObject(cb, selection)
    fun payDirectly(cb: MoneyCallback, id: LLUUID, isGroup: Boolean) = FloaterPay.payDirectly(cb, id, isGroup)
}
