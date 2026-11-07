package com.firestorm.newview

class LLFloaterSavePrefPreset(key: Any) : LLFloater(key) {

    private var mPresetCombo: LLComboBox? = null
    private var mSaveButton: LLButton? = null
    private var mSubdirectory: String = ""

    open fun postBuild(): Boolean {
        val preferences = LLFloaterReg.getTypedInstance<LLFloaterPreference>("preferences")
        preferences?.addDependentFloater(this)

        getChild<LLComboBox>("preset_combo")?.setTextEntryCallback { onPresetNameEdited() }
        // Save on pressing enter in the combo box
        getChild<LLComboBox>("preset_combo")?.setCommitCallback { onBtnSave() }
        getChild<LLButton>("save")?.setCommitCallback { onBtnSave() }
        getChild<LLButton>("cancel")?.setCommitCallback { onBtnCancel() }

        LLPresetsManager.setPresetListChangeCallback { onPresetsListChange() }

        mSaveButton = getChild("save")
        mPresetCombo = getChild("preset_combo")

        return true
    }

    open fun onOpen(key: Any) {
        mSubdirectory = key.toString()
        LLPresetsManager.setPresetNamesInComboBox(mSubdirectory, mPresetCombo, DefaultOptions.HIDE)
        onPresetNameEdited()
    }

    fun onBtnSave() {
        val name = mPresetCombo?.getSimple() ?: ""

        if (name.isEmpty()) {
            return
        }

        val upperName = name.toUpperCase()

        if (name == LLTrans.getString(PRESETS_DEFAULT) || upperName == PRESETS_DEFAULT_UPPER) {
            LLNotificationsUtil.add("DefaultPresetNotSaved")
        } else if (!LLPresetsManager.savePreset(mSubdirectory, name)) {
            LLNotificationsUtil.add("PresetNotSaved", mapOf("NAME" to name))
        }

        closeFloater()
    }

    fun onBtnCancel() {
        closeFloater()
    }

    private fun onPresetsListChange() {
        LLPresetsManager.setPresetNamesInComboBox(mSubdirectory, mPresetCombo, DefaultOptions.HIDE)
    }

    private fun onPresetNameEdited() {
        val name = mPresetCombo?.getSimple() ?: ""
        mSaveButton?.setEnabled(name.isNotEmpty())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(name: String): T? = null
    private fun closeFloater() { System.err.println("LLFloaterSavePrefPreset: closeFloater not yet implemented") }
}

// ============================================================================
// Stubs not defined elsewhere in the package
// ============================================================================

object LLFloaterReg {
    inline fun <reified T> getTypedInstance(name: String): T? = null
    inline fun <reified T> findTypedInstance(name: String): T? = null
    fun showInstance(name: String, data: Any? = null) { System.err.println("LLFloaterReg: showInstance not yet implemented") }
}
