package com.firestorm.newview

object LLMarketplaceInventoryNotifications {

    typealias NoCopyCallbackFunction = (payload: Map<String, Any>) -> Unit

    private val noCopyCallbackActions: MutableList<NoCopyCallbackFunction> = mutableListOf()
    private var noCopyNotifyActive: Boolean = false
    private val noCopyPayloads: MutableList<Map<String, Any>> = mutableListOf()

    fun update() {
        if (!noCopyNotifyActive && noCopyPayloads.isNotEmpty()) {
            noCopyNotifyActive = true
            TODO("APR: use JVM equivalent for LLNotificationsUtil::add(\"ConfirmNoCopyToOutbox\") with notifyNoCopyCallback")
        }
    }

    fun addNoCopyNotification(payload: Map<String, Any>, cb: NoCopyCallbackFunction) {
        if (noCopyCallbackActions.isEmpty()) {
            noCopyCallbackActions.add(cb)
        }
        noCopyPayloads.add(payload)
    }

    fun notifyNoCopyCallback(optionSelected: Boolean) {
        if (optionSelected) {
            for (payload in noCopyPayloads) {
                noCopyCallbackActions.forEach { it(payload) }
            }
        }
        noCopyCallbackActions.clear()
        noCopyNotifyActive = false
        noCopyPayloads.clear()
    }
}
