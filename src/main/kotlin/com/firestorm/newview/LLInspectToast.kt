package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// LLInspectToast
// ---------------------------------------------------------------------------

open class LLInspectToast(notificationId: LLSD) : LLInspect(LLSD()) {

    private var toastDestroyedCallback: ((LLToast) -> Unit)? = null
    private var panel: LLPanel? = null
    private var screenChannel: LLScreenChannel? = null

    init {
        val channel: LLScreenChannelBase? =
            LLChannelManager.getInstance().findChannelByID(NOTIFICATION_CHANNEL_UUID)
        screenChannel = channel as? LLScreenChannel
        if (screenChannel == null) {
            logWarn("Could not get requested screen channel.")
        }
        LLTransientFloaterMgr.getInstance().addControlView(this)
    }

    override fun onOpen(notificationId: LLSD) {
        super.onOpen(notificationId)
        val toast = screenChannel?.getToastByNotificationID(notificationId)
        if (toast == null) {
            logWarn("Could not get requested toast from screen channel.")
            return
        }

        // Store connection as nullable lambda; disconnecting is done by nulling it out
        toastDestroyedCallback = { t -> onToastDestroy(t) }
        toast.setOnToastDestroyedCallback(toastDestroyedCallback!!)

        val p = toast.panel
        if (p == null) {
            logWarn("Could not get toast's panel.")
            return
        }
        p.isVisible = true
        p.isMouseOpaque = false

        val currentPanel = panel
        if (currentPanel != null && currentPanel.parent == this) {
            super.removeChild(currentPanel)
        }
        addChild(p)
        p.setFocus(true)
        panel = p

        val panelRect = p.rect
        reshape(panelRect.width, panelRect.height)
        repositionInspector(notificationId)
    }

    override fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        // Bypass LLInspect's black-tooltip handling (STORM-511)
        return super<LLFloater>.handleToolTip(x, y, mask)
    }

    override fun deleteAllChildren() {
        panel = null
        super.deleteAllChildren()
    }

    override fun removeChild(child: LLView) {
        if (panel == child) {
            panel = null
        }
        super.removeChild(child)
    }

    private fun onToastDestroy(toast: LLToast) {
        closeFloater(false)
    }
}

// ---------------------------------------------------------------------------
// LLNotificationsUI namespace functions
// ---------------------------------------------------------------------------

object LLNotificationsUI {

    val NOTIFICATION_CHANNEL_UUID: UUID = UUID.fromString("notification-channel-uuid-placeholder")

    fun registerFloater() {
        LLFloaterReg.add("inspect_toast", "inspect_toast.xml") { sd ->
            LLInspectToast(sd)
        }
    }
}
