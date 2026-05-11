package com.firestorm.newview

class LLFloaterDeletePrefPreset(key: LLSD) : LLFloater(key) {

    private var mSubdirectory: String = ""

    override fun postBuild(): Boolean {
        val preferences = LLFloaterReg.getTypedInstance<LLFloaterPreference>("preferences")
        preferences?.addDependentFloater(this)

        getChild<LLButton>("delete").setCommitCallback { onBtnDelete() }
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
        val action = LLPresetsManager.getInstance().setPresetNamesInComboBox(
            mSubdirectory, combo, EDefaultOptions.DEFAULT_HIDE
        )
        getChild<LLButton>("delete").setEnabled(action)
    }

    fun onBtnDelete() {
        val combo = getChild<LLComboBox>("preset_combo")
        val name = combo.getSimple()

        if (!LLPresetsManager.getInstance().deletePreset(mSubdirectory, name)) {
            val args = LLSD()
            args["NAME"] = name
            LLNotificationsUtil.add("PresetNotDeleted", args)
        } else if (mSubdirectory == PRESETS_CAMERA) {
            if (gSavedSettings.getString("PresetCameraActive") == name) {
                gSavedSettings.setString("PresetCameraActive", "")
            }
        }

        closeFloater()
    }

    fun onBtnCancel() {
        closeFloater()
    }

    private fun onPresetsListChange() {
        val combo = getChild<LLComboBox>("preset_combo")
        LLPresetsManager.getInstance().setPresetNamesInComboBox(
            mSubdirectory, combo, EDefaultOptions.DEFAULT_HIDE
        )
    }
}
