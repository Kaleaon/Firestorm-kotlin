package com.firestorm.newview

import java.util.UUID

class LLFloaterLoadPrefPreset(key: LLSD) : LLFloater(key) {

    private var mSubdirectory: String = ""

    override fun postBuild(): Boolean {
        val preferences = LLFloaterReg.getTypedInstance<LLFloaterPreference>("preferences")
        preferences?.addDependentFloater(this)

        getChild<LLButton>("ok").setCommitCallback { onBtnOk() }
        getChild<LLButton>("cancel").setCommitCallback { onBtnCancel() }
        LLPresetsManager.instance().setPresetListChangeCallback { onPresetsListChange() }

        return true
    }

    override fun onOpen(key: LLSD) {
        mSubdirectory = key.asString()
        val titleType = "title_$mSubdirectory"
        if (hasString(titleType)) {
            setTitle(getString(titleType))
        } else {
            setTitle(titleType)
        }

        val combo = getChild<LLComboBox>("preset_combo")
        LLPresetsManager.getInstance().setPresetNamesInComboBox(mSubdirectory, combo, EDefaultOptions.DEFAULT_TOP)
        val presetGraphicActive = gSavedSettings.getString("PresetGraphicActive")
        if (presetGraphicActive.isNotEmpty()) {
            combo.setSimple(presetGraphicActive)
        }
    }

    fun onBtnOk() {
        val combo = getChild<LLComboBox>("preset_combo")
        val name = combo.getSimple()
        LLPresetsManager.getInstance().loadPreset(mSubdirectory, name)
        closeFloater()
    }

    fun onBtnCancel() {
        closeFloater()
    }

    private fun onPresetsListChange() {
        val combo = getChild<LLComboBox>("preset_combo")
        LLPresetsManager.getInstance().setPresetNamesInComboBox(mSubdirectory, combo, EDefaultOptions.DEFAULT_TOP)
        val presetGraphicActive = gSavedSettings.getString("PresetGraphicActive")
        if (presetGraphicActive.isNotEmpty()) {
            combo.setSimple(presetGraphicActive)
        }
    }
}
