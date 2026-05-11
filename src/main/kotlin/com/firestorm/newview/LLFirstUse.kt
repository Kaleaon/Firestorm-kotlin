package com.firestorm.newview

object LLFirstUse {

    private const val SANDBOX_FIRST_CLEAN_HOUR = 3
    private const val SANDBOX_CLEAN_FREQ = 12

    private var initialized = false

    fun otherAvatarChatFirst(enable: Boolean = true) {
        firstUseNotification(
            controlVar = "FirstOtherChatBeforeUser",
            enable = enable,
            notificationName = "HintChat",
            payload = mapOf(
                "target" to "fs_nearby_chat",
                "direction" to "top_right",
                "distance" to 24
            )
        )
    }

    fun speak(enable: Boolean = true) {
        firstUseNotification(
            controlVar = "FirstSpeak",
            enable = enable,
            notificationName = "HintSpeak",
            payload = mapOf("target" to "speak_btn", "direction" to "top")
        )
    }

    fun sit(enable: Boolean = true) {
        firstUseNotification(
            controlVar = "FirstSit",
            enable = enable,
            notificationName = "HintSit",
            payload = mapOf("target" to "stand_btn", "direction" to "top")
        )
    }

    fun notUsingDestinationGuide(enable: Boolean = true) {
        firstUseNotification(
            controlVar = "FirstNotUseDestinationGuide",
            enable = enable,
            notificationName = "HintDestinationGuide",
            payload = mapOf("target" to "dest_guide_btn", "direction" to "top")
        )
    }

    fun notUsingSidePanel(enable: Boolean = true) {
        // intentionally suppressed in the original; no-op
    }

    fun notMoving(enable: Boolean = true) {
        firstUseNotification(
            controlVar = "FirstNotMoving",
            enable = enable,
            notificationName = "HintMove",
            payload = mapOf("target" to "move_btn", "direction" to "top")
        )
        firstUseNotification(
            controlVar = "FirstNotMoving",
            enable = enable,
            notificationName = "HintMoveClick",
            payload = mapOf(
                "target" to "nav_bar",
                "direction" to "bottom",
                "hint_image" to "click_to_move.png",
                "up_arrow" to ""
            )
        )
    }

    fun viewPopup(enable: Boolean = true) {
        // intentionally suppressed in the original; no-op
    }

    fun newInventory(enable: Boolean = true) {
        // suppressed pending fix for EXP-62 (inventory hint fires prematurely for new users)
    }

    fun receiveLindens(enable: Boolean = true) {
        firstUseNotification(
            controlVar = "FirstReceiveLindens",
            enable = enable,
            notificationName = "HintLindenDollar",
            payload = mapOf("target" to "linden_balance", "direction" to "bottom")
        )
    }

    fun setDisplayName(enable: Boolean = true) {
        firstUseNotification(
            controlVar = "FirstDisplayName",
            enable = enable,
            notificationName = "HintDisplayName",
            payload = mapOf("target" to "set_display_name", "direction" to "left")
        )
    }

    fun useSandbox() {
        firstUseNotification(
            controlVar = "FirstSandbox",
            enable = true,
            notificationName = "FirstSandbox",
            args = mapOf("HOURS" to SANDBOX_CLEAN_FREQ, "TIME" to SANDBOX_FIRST_CLEAN_HOUR)
        )
    }

    protected fun firstUseNotification(
        controlVar: String,
        enable: Boolean,
        notificationName: String,
        args: Map<String, Any?> = emptyMap(),
        payload: Map<String, Any?> = emptyMap()
    ) {
        init()
        if (enable) {
            TODO("APR: check gSavedSettings \"EnableUIHints\" and gWarningSettings[controlVar]; if both true, add notification \"$notificationName\" with args=$args payload=$payload+controlVar")
        } else {
            TODO("APR: cancel notification \"$notificationName\" by name and set gWarningSettings[controlVar]=false")
        }
    }

    private fun init() {
        if (!initialized) {
            TODO("APR: connect processNotification to the Hints notification channel via LLNotifications equivalent")
        }
        initialized = true
    }

    fun processNotification(notify: Map<String, Any?>): Boolean {
        if ((notify["sigtype"] as? String) == "delete") {
            val id = notify["id"]
            TODO("APR: find notification by id; if found, set gWarningSettings[controlVar]=false to suppress future hints")
        }
        return false
    }
}
