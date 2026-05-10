package com.firestorm.newview

// NearbyVoiceMonitor is the JVM-side stand-in for the C++ NearbyVoiceMonitor / LLOutputMonitorCtrl widget.
class NearbyVoiceMonitor(
    val topPad: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    var visible: Boolean = false,
) {
    fun getWidth(): Int = width
    fun setVisible(value: Boolean) { TODO("GPU: toggle voice-monitor widget visibility") }
}

open class FSNearbyChatVoiceControl(
    isDefault: Boolean = false,
    textPadLeft: Int = 0,
    textPadRight: Int = 0,
    backgroundPad: Int = 1,
    bgImage: Any? = null,
    bgImageDisabled: Any? = null,
    bgImageFocused: Any? = null,
    private val voiceMonitorPadding: Int = 0,
    private val nearbyVoiceMonitorParams: NearbyVoiceMonitor = NearbyVoiceMonitor(),
) : FSNearbyChatControl(
    isDefault = isDefault,
    textPadLeft = textPadLeft,
    textPadRight = textPadRight,
    backgroundPad = backgroundPad,
    bgImage = bgImage,
    bgImageDisabled = bgImageDisabled,
    bgImageFocused = bgImageFocused,
) {
    protected val voiceMonitor: NearbyVoiceMonitor
    private val originalTextPadLeft: Int = textPadLeft
    private val originalTextPadRight: Int = textPadRight
    private var voiceMonitorVisible: Boolean = nearbyVoiceMonitorParams.visible

    init {
        TODO("GPU: register widget type fs_nearby_chat_voice_monitor with UI factory")
        // Voice monitor rect is computed from the parent rect dimensions and the monitor's own size.
        voiceMonitor = NearbyVoiceMonitor(
            topPad = nearbyVoiceMonitorParams.topPad,
            width = nearbyVoiceMonitorParams.width,
            height = nearbyVoiceMonitorParams.height,
            visible = nearbyVoiceMonitorParams.visible,
        )
        TODO("GPU: addChild(voiceMonitor) in the widget tree")
    }

    override fun draw() {
        val voiceEnabled: Boolean
        val voiceWorking: Boolean
        TODO("APR: read LLVoiceClient.getInstance().voiceEnabled() and isVoiceWorking() into voiceEnabled/voiceWorking")
        @Suppress("UNREACHABLE_CODE")
        val newVisibility = voiceEnabled && voiceWorking
        if (voiceMonitorVisible != newVisibility) {
            voiceMonitorVisible = newVisibility
            if (voiceMonitorVisible) {
                setTextPadding(
                    originalTextPadLeft,
                    originalTextPadRight + voiceMonitor.getWidth() + voiceMonitorPadding
                )
                voiceMonitor.setVisible(true)
            } else {
                setTextPadding(originalTextPadLeft, originalTextPadRight)
                voiceMonitor.setVisible(false)
            }
        }
        super.draw()
    }
}
