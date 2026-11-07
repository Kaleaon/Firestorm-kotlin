package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.Panel
import com.firestorm.llui.LLSD
import com.firestorm.llui.UICtrl
import com.firestorm.llui.ScrollListCtrl
import com.firestorm.llui.ScrollListItem
import com.firestorm.llui.Button
import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.Slider
import com.firestorm.llui.UIColorTable
import com.firestorm.llui.Color4
import java.util.UUID

class PanelActiveSpeakers(key: LLSD = LLSD()) : Panel(key), SpeakerManagerObserver {

    private lateinit var mSpeakerList: ScrollListCtrl
    private var mSpeakerMgr: SpeakerMgr? = null
    private var mSpeakerListUpdated: Boolean = false

    fun setSpeakerMgr(mgr: SpeakerMgr) {
        mSpeakerMgr = mgr
    }

    override fun postBuild(): Boolean {
        mSpeakerList = getChild<ScrollListCtrl>("speakers_list")
        mSpeakerList.setDoubleClickCallback { onDoubleClickSpeaker() }
        mSpeakerList.setCommitCallback { onChangeSorting() }
        mSpeakerList.setSortChangedCallback { onChangeSorting() }

        getChild<Button>("mute_btn").setClickedCallback { onClickMuteTextVoice() }
        getChild<Button>("mute_voice_btn").setClickedCallback { onClickMuteVoice() }
        getChild<Slider>("speaker_volume").setCommitCallback { onVolumeChange() }
        getChild<Button>("profile_btn").setClickedCallback { onClickProfile() }

        return true
    }

    fun update() {
        val mgr = mSpeakerMgr ?: return
        mgr.update(resortOk = true)

        val speakerList = mutableListOf<Speaker>()
        mgr.getSpeakerList(speakerList, includeText = true)

        val scrollPos = mSpeakerList.getScrollPos()
        val selectedId = mSpeakerList.getCurrentID()
        mSpeakerList.clearRows()

        val speakingColor = UIColorTable.instance.getColor("SpeakingColor")
        val overDrivenColor = UIColorTable.instance.getColor("OverdrivenColor")
        val activeColor = UIColorTable.instance.getColor("ActiveColor")
        val mutedColor = UIColorTable.instance.getColor("MutedColor")

        for (speakerp in speakerList) {
            val row = LLSD()
            row["id"] = speakerp.id

            val nameColor = when (speakerp.status) {
                Speaker.ESpeakerStatus.STATUS_SPEAKING -> speakingColor
                Speaker.ESpeakerStatus.STATUS_HAS_SPOKEN -> activeColor
                Speaker.ESpeakerStatus.STATUS_MUTED -> mutedColor
                else -> activeColor
            }

            row["columns"][0]["column"] = "icon_speaking_status"
            row["columns"][0]["type"] = "icon"
            row["columns"][0]["value"] = when (speakerp.status) {
                Speaker.ESpeakerStatus.STATUS_SPEAKING -> "active_voice_icon"
                Speaker.ESpeakerStatus.STATUS_MUTED -> "mute_icon"
                else -> ""
            }

            row["columns"][1]["column"] = "speaker_name"
            row["columns"][1]["value"] = speakerp.displayName
            row["columns"][1]["color"] = nameColor

            row["columns"][2]["column"] = "speaking_status"
            row["columns"][2]["value"] = speakerp.speechVolume

            mSpeakerList.addElement(row)
        }

        if (selectedId != null) mSpeakerList.setCurrentByID(selectedId)
        mSpeakerList.setScrollPos(scrollPos)
    }

    private fun onDoubleClickSpeaker() {
        val speakerId = mSpeakerList.getCurrentID() ?: return
        AvatarActions.showProfile(speakerId)
    }

    private fun onChangeSorting() {}

    private fun onClickMuteTextVoice() {
        val speakerId = mSpeakerList.getCurrentID() ?: return
        val muteList = MuteList.instance
        if (muteList.isMuted(speakerId)) {
            muteList.remove(speakerId)
        } else {
            muteList.add(MuteData(speakerId, MuteData.MUTE_BY_NAME))
        }
    }

    private fun onClickMuteVoice() {
        val speakerId = mSpeakerList.getCurrentID() ?: return
        if (VoiceClient.instance.getOnMuteList(speakerId)) {
            VoiceClient.instance.removeFromMuteList(speakerId)
        } else {
            VoiceClient.instance.addToMuteList(speakerId)
        }
    }

    private fun onVolumeChange() {
        val speakerId = mSpeakerList.getCurrentID() ?: return
        val volume = getChild<Slider>("speaker_volume").getValue().toFloat()
        VoiceClient.instance.setUserVolume(speakerId, volume)
    }

    private fun onClickProfile() {
        val speakerId = mSpeakerList.getCurrentID() ?: return
        AvatarActions.showProfile(speakerId)
    }
}

class FloaterActiveSpeakers(key: LLSD) : Floater(key) {

    private lateinit var mPanel: PanelActiveSpeakers

    override fun postBuild(): Boolean {
        mPanel = getChild<PanelActiveSpeakers>("active_speakers_panel")
        mPanel.setSpeakerMgr(ActiveSpeakerMgr.instance)
        return true
    }

    override fun draw() {
        mPanel.update()
        super.draw()
    }
}
