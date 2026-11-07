package com.firestorm.newview

import java.util.UUID

open class FloaterEvent(key: LLSD) : Floater(key), ViewerMediaObserver {

    var eventId: UInt = 0u
        private set

    private var browser: MediaCtrl? = null

    override fun postBuild(): Boolean {
        browser = getChild<MediaCtrl>("browser")
        browser?.addObserver(this)
        return true
    }

    fun setEventId(eventId: UInt) {
        this.eventId = eventId
        if (eventId != 0u) {
            val subs = LLSD().apply { put("EVENT_ID", eventId.toInt()) }
            val expandedUrl = Web.expandUrlSubstitutions(SavedSettings.getString("EventURL"), subs)
            browser?.navigateTo(expandedUrl)
        }
    }

    override fun handleMediaEvent(self: PluginClassMedia, event: MediaEvent) {
        when (event) {
            MediaEvent.NAVIGATE_BEGIN ->
                getChild<UiCtrl>("status_text")?.setValue(getString("loading_text"))
            MediaEvent.NAVIGATE_COMPLETE ->
                getChild<UiCtrl>("status_text")?.setValue(getString("done_text"))
            else -> Unit
        }
    }
}
