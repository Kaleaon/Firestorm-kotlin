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

private fun savedSettingsGetString(key: String): String = ""
private fun notificationsAdd(notification: String) { System.err.println("FloaterHUD: notificationsAdd not yet implemented") }
private fun findChildByName(name: String): Any? = null
private fun uiGetLanguage(): String = ""
private fun setWebBrowserTakeFocusOnClick(browser: Any, value: Boolean) { System.err.println("FloaterHUD: setWebBrowserTakeFocusOnClick not yet implemented") }
private fun webBrowserNavigateTo(browser: Any, url: String) { System.err.println("FloaterHUD: webBrowserNavigateTo not yet implemented") }
