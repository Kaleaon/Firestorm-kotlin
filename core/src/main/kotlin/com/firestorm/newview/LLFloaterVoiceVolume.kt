package com.firestorm.newview

import java.util.UUID

class LLFloaterVoiceVolume(sd: LLSD) : LLInspect(LLSD()), LLTransientFloater {

    private var avatarID: UUID = NULL_UUID
    private var avatarName: LLAvatarName = LLAvatarName()
    private var avatarNameCacheConnection: (() -> Unit)? = null

    init {
        LLTransientFloaterMgr.instance.addControlView(LLTransientFloaterMgr.GLOBAL, this)
        initTransientFloater(this)
    }

    override fun postBuild(): Boolean {
        getChild<LLUICtrl>("mute_btn").setCommitCallback { onClickMuteVolume() }
        getChild<LLUICtrl>("volume_slider").setCommitCallback { data -> onVolumeChange(data) }
        return true
    }

    override fun onOpen(data: LLSD) {
        super.onOpen(data)

        avatarID = data["avatar_id"].asUUID()

        repositionInspector(data)

        getChild<LLUICtrl>("avatar_name").setValue("")
        updateVolumeControls()

        avatarNameCacheConnection?.invoke()
        avatarNameCacheConnection = LLAvatarNameCache.get(avatarID) { agentId, avName ->
            onAvatarNameCache(agentId, avName)
        }
    }

    override fun getGroup(): LLTransientFloaterMgr.ETransientGroup =
        LLTransientFloaterMgr.ETransientGroup.GLOBAL

    fun onDestroy() {
        avatarNameCacheConnection?.invoke()
        LLTransientFloaterMgr.instance.removeControlView(this)
    }

    private fun updateVolumeControls() {
        val voiceEnabled = LLVoiceClient.instance.getVoiceEnabled(avatarID)

        val muteBtn = getChild<LLUICtrl>("mute_btn")
        val volumeSlider = getChild<LLUICtrl>("volume_slider")

        if (!voiceEnabled || avatarID == gAgent.id) {
            muteBtn.setVisible(false)
            volumeSlider.setVisible(false)
            return
        }

        muteBtn.setVisible(true)
        volumeSlider.setVisible(true)

        // Only voice-specific mutes are toggled here, not global mutes
        val isMuted = LLAvatarActions.isVoiceMuted(avatarID)
        val isLinden = avatarName.getUserName().endsWith(" Linden")

        muteBtn.setEnabled(!isLinden)
        muteBtn.setValue(isMuted)
        volumeSlider.setEnabled(!isMuted)

        val volume: Float = if (isMuted) {
            // show zero for a muted user so the slider reflects their effective volume
            0f
        } else {
            LLVoiceClient.instance.getUserVolume(avatarID)
        }
        volumeSlider.setValue(volume.toDouble())
    }

    private fun onClickMuteVolume() {
        LLAvatarActions.toggleMuteVoice(avatarID)
        updateVolumeControls()
    }

    private fun onVolumeChange(data: LLSD) {
        val volume = data.asReal().toFloat()
        LLVoiceClient.instance.setUserVolume(avatarID, volume)
    }

    private fun onAvatarNameCache(agentId: UUID, avName: LLAvatarName) {
        avatarNameCacheConnection = null

        if (agentId != avatarID) return

        getChild<LLUICtrl>("avatar_name").setValue(avName.getCompleteName())
        avatarName = avName
    }
}

object LLFloaterVoiceVolumeUtil {
    fun registerFloater() {
        LLFloaterReg.add(
            "floater_voice_volume",
            "floater_voice_volume.xml"
        ) { key -> LLFloaterVoiceVolume(key) }
    }
}
