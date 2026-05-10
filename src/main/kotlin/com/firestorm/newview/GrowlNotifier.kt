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
        TODO("APR: use JVM equivalent for Growl++ native library initialisation (GrowlNotifierWin)")
    }

    override fun showNotification(
        notificationTitle: String,
        notificationMessage: String,
        notificationType: String
    ) {
        TODO("APR: use JVM equivalent for Growl::Notify via growlImpl")
    }

    override fun isUsable(): Boolean {
        TODO("APR: use JVM equivalent for Growl::IsRunning check via growlImpl")
    }

    override fun registerApplication(application: String, notificationTypes: Set<String>) {
        applicationName = application
        TODO("APR: use JVM equivalent for Growl::Register via growlImpl")
    }
}
