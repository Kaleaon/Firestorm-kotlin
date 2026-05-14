package com.firestorm.newview

open class GrowlNotifier {

    open fun showNotification(
        notificationTitle: String,
        notificationMessage: String,
        notificationType: String
    ) {}

    open fun isUsable(): Boolean = false

    open fun registerApplication(application: String, notificationTypes: Set<String>) {}

    open fun needsThrottle(): Boolean = true
}

class GrowlNotifierWin : GrowlNotifier() {

    private var applicationName: String = ""
    private var growlImpl: Any? = null

    init {
        System.err.println("GrowlNotifierWin: init not yet implemented")
    }

    override fun showNotification(
        notificationTitle: String,
        notificationMessage: String,
        notificationType: String
    ) {
        System.err.println("GrowlNotifierWin: showNotification not yet implemented")
    }

    override fun isUsable(): Boolean {
        return false
    }

    override fun registerApplication(application: String, notificationTypes: Set<String>) {
        applicationName = application
        System.err.println("GrowlNotifierWin: registerApplication not yet implemented")
    }
}
