package com.firestorm.newview

import java.util.UUID

enum class VoiceEffectColumnIndex(val index: Int) {
    NAME_COLUMN(0),
    DATE_COLUMN(1)
}

class LLFloaterVoiceEffect(key: LLSD) : LLFloater(key), LLVoiceEffectObserver {

    private var mSelectedID: UUID = UUID.randomUUID()
    private var mVoiceEffectList: LLScrollListCtrl? = null

    init {
        mCommitCallbackRegistrar.add("VoiceEffect.Record") { onClickRecord() }
        mCommitCallbackRegistrar.add("VoiceEffect.Play") { onClickPlay() }
        mCommitCallbackRegistrar.add("VoiceEffect.Stop") { onClickStop() }
    }

    override fun postBuild(): Boolean {
        setDefaultBtn("record_btn")
        getChild<LLButton>("record_btn").setFocus(true)
        getChild<LLUICtrl>("voice_morphing_link").setTextArg("[URL]", LLTrans.getString("voice_morphing_url"))

        mVoiceEffectList = getChild<LLScrollListCtrl>("voice_effect_list")
        mVoiceEffectList?.setCommitCallback { onClickPlay() }

        val effectInterface = LLVoiceClient.instance().getVoiceEffectInterface()
        if (effectInterface != null) {
            effectInterface.addObserver(this)
            effectInterface.enablePreviewBuffer(true)
        }

        refreshEffectList()
        updateControls()
        return true
    }

    override fun onClose(appQuitting: Boolean) {
        LLVoiceClient.instance().getVoiceEffectInterface()?.enablePreviewBuffer(false)
    }

    private fun refreshEffectList() {
        val list = mVoiceEffectList ?: return

        val effectInterface = LLVoiceClient.instance().getVoiceEffectInterface()
        if (effectInterface == null) {
            list.setEnabled(false)
            return
        }

        val scrollPos = list.getScrollPos()
        val selectedItems = list.getAllSelected().map { it.getUUID() }

        list.deleteAllItems()

        run {
            val element = LLSD()
            element["id"] = UUID.fromString("00000000-0000-0000-0000-000000000000")
            element["columns"][VoiceEffectColumnIndex.NAME_COLUMN.index]["column"] = "name"
            element["columns"][VoiceEffectColumnIndex.NAME_COLUMN.index]["value"] = getString("no_voice_effect")
            element["columns"][VoiceEffectColumnIndex.NAME_COLUMN.index]["font"]["style"] = "BOLD"
            val slItem = list.addElement(element, AddPosition.ADD_BOTTOM)
            if (slItem != null) {
                (slItem.getColumn(0) as? LLScrollListText)?.setFontStyle(LLFontGL.BOLD)
            }
        }

        val templateList = effectInterface.getVoiceEffectTemplateList()
        for ((effectKey, effectId) in templateList) {
            val localizedEffect = "effect_$effectKey"
            var effectName = if (hasString(localizedEffect)) getString(localizedEffect) else effectKey

            val effectProperties = effectInterface.getVoiceEffectProperties(effectId)

            if (effectId == LLVoiceClient.instance().getVoiceEffectDefault()) {
                effectName += " " + getString("active_voice_effect")
            }

            if (effectProperties["is_new"].asBoolean()) {
                effectName += " " + getString("new_voice_effect")
            }

            val expiryDate = effectProperties["expiry_date"].asDate()
            val isTemplateOnly = effectProperties["template_only"].asBoolean()
            val fontStyle = if (!isTemplateOnly) "BOLD" else "NORMAL"

            val element = LLSD()
            element["id"] = effectId

            element["columns"][VoiceEffectColumnIndex.NAME_COLUMN.index]["column"] = "name"
            element["columns"][VoiceEffectColumnIndex.NAME_COLUMN.index]["value"] = effectName
            element["columns"][VoiceEffectColumnIndex.NAME_COLUMN.index]["font"]["style"] = fontStyle

            element["columns"][1]["column"] = "expires"
            if (!isTemplateOnly) {
                element["columns"][VoiceEffectColumnIndex.DATE_COLUMN.index]["value"] = expiryDate
                element["columns"][VoiceEffectColumnIndex.DATE_COLUMN.index]["type"] = "date"
            } else {
                element["columns"][VoiceEffectColumnIndex.DATE_COLUMN.index]["value"] = getString("unsubscribed_voice_effect")
            }

            val slItem = list.addElement(element, AddPosition.ADD_BOTTOM)
            if (slItem != null) {
                val style = if (isTemplateOnly) LLFontGL.NORMAL else LLFontGL.BOLD
                val slt = slItem.getColumn(0) as? LLScrollListText
                slt?.setFontStyle(style)
            }
        }

        for (id in selectedItems) {
            list.selectByID(id)
        }
        list.setScrollPos(scrollPos)
        list.setEnabled(true)
    }

    private fun updateControls() {
        val recording = LLVoiceClient.instance().getVoiceEffectInterface()?.isPreviewRecording() ?: false
        getChild<LLButton>("record_btn").setVisible(!recording)
        getChild<LLButton>("record_stop_btn").setVisible(recording)
    }

    override fun onVoiceEffectChanged(effectListUpdated: Boolean) {
        if (effectListUpdated) {
            refreshEffectList()
        }
        updateControls()
    }

    private fun onClickRecord() {
        LLVoiceClient.instance().getVoiceEffectInterface()?.recordPreviewBuffer()
        updateControls()
    }

    private fun onClickPlay() {
        val list = mVoiceEffectList ?: return
        val effectId = list.getCurrentID()
        LLVoiceClient.instance().getVoiceEffectInterface()?.playPreviewBuffer(effectId)
        updateControls()
    }

    private fun onClickStop() {
        LLVoiceClient.instance().getVoiceEffectInterface()?.stopPreviewBuffer()
        updateControls()
    }
}
