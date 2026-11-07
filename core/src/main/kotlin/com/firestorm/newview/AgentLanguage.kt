package com.firestorm.newview

import com.firestorm.agent.Agent
import com.firestorm.control.ViewerControl
import com.firestorm.region.ViewerRegion
import com.firestorm.ui.LLUI

object AgentLanguage {

    fun init() {
        ViewerControl.getControl("Language").addChangeListener { onChange() }
        ViewerControl.getControl("InstallLanguage").addChangeListener { onChange() }
        ViewerControl.getControl("SystemLanguage").addChangeListener { onChange() }
        ViewerControl.getControl("LanguageIsPublic").addChangeListener { onChange() }
        // Apply LanguageIsPublic changes immediately without waiting for next login.
        ViewerControl.getControl("LanguageIsPublic").addChangeListener { update() }
    }

    private fun onChange() {
        // Intentionally left empty: cache purge on language change was removed
        // because it was more disruptive than asking users to clear manually.
    }

    fun update(): Boolean {
        val language = LLUI.instance.getUILanguage(bypassFilter = true)
        val languageIsPublic = ViewerControl.getBoolean("LanguageIsPublic")

        val body = mapOf(
            "language" to language,
            "language_is_public" to languageIsPublic,
        )

        Agent.instance.requestPostCapability("UpdateAgentLanguage", body)
        return true
    }
}
