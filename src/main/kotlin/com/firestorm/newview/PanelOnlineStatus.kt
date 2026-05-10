package com.firestorm.newview

class PanelOnlineStatus private constructor(notification: Notification) : PanelTipToast(notification) {

    init {
        buildFromFile("panel_online_status_toast.xml")

        getChild<UICtrl>("avatar_icon")?.setValue(notification.getPayload()["FROM_ID"])
        getChild<UICtrl>("message")?.setValue(notification.getMessage())

        if (notification.getPayload().has("respond_on_mousedown") &&
            notification.getPayload()["respond_on_mousedown"].asBoolean()
        ) {
            setMouseDownCallback {
                notification.respond(notification.getResponseTemplate())
            }
        }

        val maxLineCount = SavedSettings.getInt("TipToastMessageLineCount")
        snapToMessageHeight(getChild<TextBox>("message"), maxLineCount)
    }

    companion object {
        fun create(notification: Notification): PanelOnlineStatus = PanelOnlineStatus(notification)
    }
}
