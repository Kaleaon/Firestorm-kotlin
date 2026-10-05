package com.firestorm.newview

class LLFloaterHUD private constructor(private val key: LLSD) : LLFloater(key) {

    private var mWebBrowser: LLMediaCtrl? = null

    init {
        if (gSavedSettings.getString("TutorialURL").isEmpty()) {
            LLNotificationsUtil.add("TutorialNotFound")
        } else {
            // Opaque background: this floater never takes focus (user must keep walking with arrow keys).
            setBackgroundOpaque(true)
        }
    }

    override fun postBuild(): Boolean {
        mWebBrowser = getChild<LLMediaCtrl>("floater_hud_browser")
        mWebBrowser?.apply {
            // Chrome floater: suppress focus so arrow-key movement remains uninterrupted.
            setTakeFocusOnClick(false)
            val language = LLUI.getLanguage()
            val baseUrl = gSavedSettings.getString("TutorialURL")
            navigateTo("$baseUrl$language/")
        }
        return true
    }
}
