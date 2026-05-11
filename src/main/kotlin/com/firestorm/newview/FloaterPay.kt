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
            val obj = getPrimaryObject(selection) ?: return

            val floater: FloaterPay = TODO("APR: use JVM equivalent - FloaterReg showTypedInstance pay_object with object ID")
            floater.setCallback(callback)
            floater.objectSelection = selection

            val node = getFirstRootNode(selection)
            if (node == null) {
                TODO("APR: use JVM equivalent - show PayObjectFailed notification")
                floater.closeFloater()
                return
            }

            TODO("APR: use JVM equivalent - send RequestPayPrice UDP message to object's region host and register processPayPriceReply handler")

            val (ownerId, isGroup) = getOwnership(node)
            floater.getChild<UICtrl>("object_name_text").setValue(getName(node))
            floater.finishPayUI(ownerId, isGroup)
        }

        fun payDirectly(callback: MoneyCallback, targetId: UUID, isGroup: Boolean) {
            val floater: FloaterPay = TODO("APR: use JVM equivalent - FloaterReg showTypedInstance pay_resident with targetId")
            floater.setCallback(callback)
            floater.objectSelection = null

            floater.getChildView("amount").setVisible(true)
            floater.getChildView("pay btn").setVisible(true)
            floater.getChildView("amount text").setVisible(true)

            val canSendIm: Boolean = TODO("APR: use JVM equivalent - RlvActions::canSendIM(targetId)")
            floater.getChildView("payment_message").setEnabled(canSendIm)

            for (i in 0 until MAX_PAY_BUTTONS) {
                floater.quickPayButton[i]?.setVisible(true)
            }

            floater.finishPayUI(targetId, isGroup)
        }

        private fun payConfirmationCallback(notification: Any, response: Any, info: GiveMoneyInfo): Boolean {
            val option: Int = TODO("APR: use JVM equivalent - LLNotificationsUtil::getSelectedOption")
            if (option == 0) {
                info.floater.give(info.amount)
                info.floater.closeFloater()
            }
            return false
        }

        private fun processPayPriceReply(msg: Any) {
            TODO("APR: use JVM equivalent - handle PayPriceReply UDP message; update quick-pay button labels and visibility, reshape floater for large amounts")
        }

        private fun getPrimaryObject(selection: Any?): Any?           = TODO("APR: use JVM equivalent - selection->getPrimaryObject()")
        private fun getFirstRootNode(selection: Any?): Any?           = TODO("APR: use JVM equivalent - selection->getFirstRootNode()")
        private fun getOwnership(node: Any): Pair<UUID, Boolean>      = TODO("APR: use JVM equivalent - node->mPermissions->getOwnership()")
        private fun getName(node: Any): String                        = TODO("APR: use JVM equivalent - node->mName")
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
        val slurl: String = if (isGroup) {
            TODO("APR: use JVM equivalent - LLSLURL(group, targetId, inspect).getSLURLString()")
        } else {
            TODO("APR: use JVM equivalent - LLSLURL(agent, targetId, inspect).getSLURLString()")
        }
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
        var amount = info.amount
        if (amount == 0) {
            amount = getChild<UICtrl>("amount").getValue().toString().toIntOrNull() ?: 0
        }

        val confirmPayments: Boolean = TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(FSConfirmPayments)")
        val confirmThreshold: Int    = TODO("APR: use JVM equivalent - gSavedSettings.getS32(FSPaymentConfirmationThreshold)")
        val balance: Int             = TODO("APR: use JVM equivalent - gStatusBar.getBalance()")

        if (confirmPayments && amount > confirmThreshold && balance >= amount) {
            val payeeId: UUID
            val isGroup: Boolean

            if (objectSelection != null) {
                val node = getFirstRootNodeLocal(objectSelection)
                if (node == null) {
                    TODO("APR: use JVM equivalent - show PayObjectFailed notification")
                    closeFloater()
                    return
                }
                val ownership: Pair<UUID, Boolean> = TODO("APR: use JVM equivalent - node->mPermissions->getOwnership()")
                payeeId  = ownership.first
                isGroup  = ownership.second
            } else {
                isGroup  = targetIsGroup
                payeeId  = targetUUID
            }

            val agentId: UUID = TODO("APR: use JVM equivalent - gAgent.getID()")
            if (isGroup || payeeId != agentId) {
                val args = mapOf(
                    "TARGET" to TODO<String>("APR: use JVM equivalent - LLSLURL completename for payeeId"),
                    "AMOUNT" to amount
                )
                TODO("APR: use JVM equivalent - show PayConfirmation notification with payConfirmationCallback")
            } else {
                give(amount)
                closeFloater()
            }
        } else {
            give(amount)
            closeFloater()
        }
    }

    private fun give(amountIn: Int) {
        val cb = callback ?: return
        val amount = if (amountIn == 0) {
            getChild<UICtrl>("amount").getValue().toString().toIntOrNull() ?: 0
        } else {
            amountIn
        }
        lastAmount = amount

        if (objectSelection != null) {
            val destObject: Any? = TODO("APR: use JVM equivalent - gObjectList.findObject(targetUUID)")
            val region: Any?     = TODO("APR: use JVM equivalent - destObject->getRegion()")
            if (destObject != null && region != null) {
                val node = getFirstRootNodeLocal(objectSelection)
                val objectName = node?.let { TODO<String>("APR: use JVM equivalent - node->mName") } ?: ""
                val isAvatar: Boolean = TODO("APR: use JVM equivalent - destObject->isAvatar()")
                val txType = if (isAvatar) TRANS_GIFT else TRANS_PAY_OBJECT
                cb(targetUUID, region, amount, false, txType, objectName)
                objectSelection = null
                TODO("APR: use JVM equivalent - send RequestObjectPropertiesFamily UDP message to unmute object owner if needed")
            } else {
                TODO("APR: use JVM equivalent - show PayObjectFailed notification")
            }
        } else {
            val paymentMessage = getChild<LineEditor>("payment_message").getValue().toString()
            cb(targetUUID, TODO("APR: use JVM equivalent - gAgent.getRegion()"), amount, targetIsGroup, TRANS_GIFT, paymentMessage)
            TODO("APR: use JVM equivalent - LLMuteList::autoRemove(targetUUID, AR_MONEY)")
        }
    }

    private fun getFirstRootNodeLocal(sel: Any?): Any? = TODO("APR: use JVM equivalent - mObjectSelection->getFirstRootNode()")
}

object FloaterPayUtil {
    fun registerFloater() {
        TODO("APR: use JVM equivalent - register pay_resident and pay_object with FloaterReg using FloaterPay builder")
    }

    fun payViaObject(callback: MoneyCallback, selection: Any?) {
        FloaterPay.payViaObject(callback, selection)
    }

    fun payDirectly(callback: MoneyCallback, targetId: UUID, isGroup: Boolean) {
        FloaterPay.payDirectly(callback, targetId, isGroup)
    }
}
