package com.firestorm.newview

class FloaterHUD(key: Any?) : Floater(key) {

    private var webBrowser: MediaCtrlStub? = null

    init {
        val tutorialUrl = SavedSettings.getString("TutorialURL")
        if (tutorialUrl.isEmpty()) {
            NotificationsUtil.add("TutorialNotFound")
        } else {
            setBackgroundOpaque(true)
        }
    }

    override fun postBuild(): Boolean {
        webBrowser = getChild("floater_hud_browser")
        webBrowser?.let {
            it.setTakeFocusOnClick(false)
            val language = UI.getLanguage()
            val baseUrl = SavedSettings.getString("TutorialURL")
            it.navigateTo("$baseUrl$language/")
        }
        return true
    }

    private fun setBackgroundOpaque(opaque: Boolean) {
        TODO("GPU: set background opacity for floater")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(name: String): T? {
        TODO("GPU: look up named child widget '$name'")
    }
}

object SavedSettings {
    fun getString(key: String): String = TODO("APR: use JVM equivalent for gSavedSettings.getString")
    fun getBool(key: String): Boolean = TODO("APR: use JVM equivalent for gSavedSettings.getBOOL")
    fun setBool(key: String, value: Boolean): Unit = TODO("APR: use JVM equivalent for gSavedSettings.setBOOL")
    fun getFloat(key: String): Float = TODO("APR: use JVM equivalent for gSavedSettings.getF32")
}

object NotificationsUtil {
    fun add(name: String) {
        TODO("APR: use JVM equivalent for LLNotificationsUtil::add")
    }
}

object UI {
    fun getLanguage(): String = TODO("APR: use JVM equivalent for LLUI::getLanguage")
}

class MediaCtrlStub {
    fun setTakeFocusOnClick(take: Boolean) {}
    fun navigateTo(url: String) {
        TODO("GPU: navigate embedded browser to '$url'")
    }
}
