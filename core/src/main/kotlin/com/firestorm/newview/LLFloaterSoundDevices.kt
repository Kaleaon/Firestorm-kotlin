package com.firestorm.newview

class LLFloaterSoundDevices(key: Any) : LLTransientDockableFloater(null, false, key) {

    init {
        LLTransientFloaterMgr.getInstance().addControlView(this)
        setDocked(true)
    }

    fun destroy() {
        LLTransientFloaterMgr.getInstance().removeControlView(this)
    }

    override fun postBuild(): Boolean {
        super.postBuild()

        updateTransparency(TT_ACTIVE)

        val panel = findChild<LLPanelVoiceDeviceSettings>("device_settings_panel")
        if (panel != null) {
            panel.setUseTuningMode(false)
            getChild<LLUICtrl>("voice_input_device").setCommitCallback { _, _ -> panel.apply() }
            getChild<LLUICtrl>("voice_output_device").setCommitCallback { _, _ -> panel.apply() }
            getChild<LLUICtrl>("mic_volume_slider").setCommitCallback { _, _ -> panel.apply() }
        }
        return true
    }

    override fun setDocked(docked: Boolean, popOnUndock: Boolean) {
        super.setDocked(docked, popOnUndock)
    }

    override fun setFocus(focused: Boolean) {
        super.setFocus(focused)
        // Selecting combobox items causes focus loss which would make the floater transparent;
        // force active transparency to keep it fully visible.
        updateTransparency(TT_ACTIVE)
    }
}
