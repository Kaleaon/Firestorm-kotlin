package com.firestorm.newview

interface Button {
    fun setEnabled(enabled: Boolean)
    fun setImageOverlay(imageName: String)
    fun setValue(value: Any)
    fun setCommitCallback(callback: () -> Unit)
    fun setMouseDownCallback(callback: (Any) -> Unit)
    fun setMouseUpCallback(callback: (Any) -> Unit)
}

interface ViewerMedia {
    fun isParcelAudioPlaying(): Boolean
    fun isAnyMediaPlaying(): Boolean
    fun isParcelMediaPlaying(): Boolean
    fun hasInWorldMedia(): Boolean
    fun hasParcelMedia(): Boolean
    fun hasParcelAudio(): Boolean
}

interface StatusBar {
    fun toggleStream(play: Boolean)
    fun toggleMedia(play: Boolean)
}

interface Agent {
    fun isMicrophoneOn(param: Any): Boolean
    fun isActionAllowed(action: Any): Boolean
}

interface RootView {
    fun findChildView(name: String): Any?
    fun findChildButton(name: String): Button?
}

interface StartupState {
    fun isStarted(): Boolean
}

object UtilityBar {

    private var parcelStreamPlayButton: Button? = null
    private var parcelMediaPlayButton: Button? = null
    private var talkButton: Button? = null
    private var aoInterfaceButton: Button? = null
    private var volumeControlsInterfaceButton: Button? = null
    private var pttButton: Button? = null

    lateinit var viewerMedia: ViewerMedia
    lateinit var statusBar: StatusBar
    lateinit var agent: Agent
    lateinit var startupState: StartupState
    lateinit var savedSettings: (String) -> Boolean
    var tickIntervalSeconds: Float = 0.5f

    fun init(rootView: RootView) {
        if (rootView.findChildView("chat_bar_utility_bar_stack") == null) {
            return
        }

        parcelStreamPlayButton = rootView.findChildButton("utility_parcel_audio_stream_button")
        parcelMediaPlayButton  = rootView.findChildButton("utility_parcel_media_button")
        talkButton             = rootView.findChildButton("utility_talk_button")
        aoInterfaceButton      = rootView.findChildButton("show_ao_interface_button")
        volumeControlsInterfaceButton = rootView.findChildButton("show_volume_controls_button")
        pttButton              = rootView.findChildButton("utility_push_to_talk_lock_button")

        parcelStreamPlayButton?.setCommitCallback { onParcelStreamClicked() }
        parcelMediaPlayButton?.setCommitCallback { onParcelMediaClicked() }

        talkButton?.setMouseDownCallback { statusBar.toString() /* agent.pressMicrophone */ }
        talkButton?.setMouseUpCallback   { statusBar.toString() /* agent.releaseMicrophone */ }
    }

    private fun onParcelStreamClicked() {
        statusBar.toggleStream(!viewerMedia.isParcelAudioPlaying())
    }

    private fun onParcelMediaClicked() {
        val anyPlaying = viewerMedia.isAnyMediaPlaying() || viewerMedia.isParcelMediaPlaying()
        statusBar.toggleMedia(!anyPlaying)
    }

    fun tick(): Boolean {
        if (!startupState.isStarted()) return false

        parcelMediaPlayButton?.let {
            val audioStreamingMedia = savedSettings("AudioStreamingMedia")
            val buttonEnabled = audioStreamingMedia &&
                    (viewerMedia.hasInWorldMedia() || viewerMedia.hasParcelMedia())
            it.setEnabled(buttonEnabled)
            val anyPlaying = viewerMedia.isAnyMediaPlaying() || viewerMedia.isParcelMediaPlaying()
            it.setImageOverlay(if (anyPlaying) "icn_pause.tga" else "icn_play.tga")
        }

        parcelStreamPlayButton?.let {
            val audioStreamingMusic = savedSettings("AudioStreamingMusic")
            val buttonEnabled = audioStreamingMusic && viewerMedia.hasParcelAudio()
            it.setEnabled(buttonEnabled)
            it.setImageOverlay(if (viewerMedia.isParcelAudioPlaying()) "icn_pause.tga" else "icn_play.tga")
        }

        talkButton?.let {
            it.setValue(agent.isMicrophoneOn(Unit))
            it.setEnabled(agent.isActionAllowed("speak"))
        }

        pttButton?.setEnabled(agent.isActionAllowed("speak"))

        return false
    }

    fun setAoInterfaceButtonExpanded(expanded: Boolean) {
        aoInterfaceButton?.setImageOverlay(if (expanded) "arrow_down.tga" else "arrow_up.tga")
    }

    fun setVolumeControlsButtonExpanded(expanded: Boolean) {
        volumeControlsInterfaceButton?.setImageOverlay(if (expanded) "arrow_down.tga" else "arrow_up.tga")
    }
}
