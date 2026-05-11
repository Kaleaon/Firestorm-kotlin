package com.firestorm.newview

class FloaterHUD(private val key: Any?) {

    private var webBrowser: Any? = null

    init {
        val tutorialUrl = savedSettingsGetString("TutorialURL")
        if (tutorialUrl.isEmpty()) {
            notificationsAdd("TutorialNotFound")
        }
    }

    fun postBuild(): Boolean {
        val browser = findChildByName("floater_hud_browser")
        if (browser != null) {
            webBrowser = browser
            setWebBrowserTakeFocusOnClick(browser, false)

            val language = uiGetLanguage()
            val baseUrl = savedSettingsGetString("TutorialURL")
            val url = "$baseUrl$language/"
            webBrowserNavigateTo(browser, url)
        }
        return true
    }
}

private fun savedSettingsGetString(key: String): String = TODO("APR: use JVM equivalent")
private fun notificationsAdd(notification: String) { TODO("APR: use JVM equivalent") }
private fun findChildByName(name: String): Any? = TODO("APR: use JVM equivalent")
private fun uiGetLanguage(): String = TODO("APR: use JVM equivalent")
private fun setWebBrowserTakeFocusOnClick(browser: Any, value: Boolean) { TODO("APR: use JVM equivalent") }
private fun webBrowserNavigateTo(browser: Any, url: String) { TODO("APR: use JVM equivalent") }
