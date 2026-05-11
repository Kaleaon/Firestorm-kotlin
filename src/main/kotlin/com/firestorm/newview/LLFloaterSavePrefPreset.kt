package com.firestorm.newview

open class LLFloater(val key: Any)

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

        LLPresetsManager.instance.setPresetListChangeCallback { onPresetsListChange() }

        mSaveButton = getChild<LLButton>("save")
        mPresetCombo = getChild<LLComboBox>("preset_combo")

        return true
    }

    open fun onOpen(key: Any) {
        mSubdirectory = key.toString()
        LLPresetsManager.instance.setPresetNamesInComboBox(mSubdirectory, mPresetCombo, DefaultOptions.HIDE)
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
        } else if (!LLPresetsManager.instance.savePreset(mSubdirectory, name)) {
            LLNotificationsUtil.add("PresetNotSaved", mapOf("NAME" to name))
        }

        closeFloater()
    }

    fun onBtnCancel() {
        closeFloater()
    }

    private fun onPresetsListChange() {
        LLPresetsManager.instance.setPresetNamesInComboBox(mSubdirectory, mPresetCombo, DefaultOptions.HIDE)
    }

    private fun onPresetNameEdited() {
        val name = mPresetCombo?.getSimple() ?: ""
        mSaveButton?.setEnabled(name.isNotEmpty())
    }

    private fun <T> getChild(name: String): T? = TODO("UI: resolve child widget '$name'")
    private fun closeFloater() { TODO("UI: close this floater") }
}

const val PRESETS_DEFAULT_UPPER = "DEFAULT"

object LLFloaterReg {
    inline fun <reified T> getTypedInstance(name: String): T? { TODO("FloaterReg: getTypedInstance '$name'") }
    inline fun <reified T> findTypedInstance(name: String): T? { TODO("FloaterReg: findTypedInstance '$name'") }
    fun showInstance(name: String, data: Any? = null) { TODO("FloaterReg: showInstance '$name'") }
}

class LLFloaterPreference(key: Any) : LLFloater(key) {
    fun addDependentFloater(floater: LLFloater) { TODO("FloaterPreference: addDependentFloater") }
}
