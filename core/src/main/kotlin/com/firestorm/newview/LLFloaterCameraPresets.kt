package com.firestorm.newview

class LLFloaterCameraPresets(key: LLSD) : LLFloater(key) {

    private lateinit var mPresetList: LLFlatListView

    override fun postBuild(): Boolean {
        mPresetList = getChild<LLFlatListView>("preset_list")
        mPresetList.setCommitCallback { onSelectionChange() }
        mPresetList.setCommitOnSelectionChange(true)
        LLPresetsManager.getInstance().setPresetListChangeCameraCallback { populateList() }
        return true
    }

    override fun onOpen(key: LLSD) {
        populateList()
    }

    private fun populateList() {
        mPresetList.clear()

        val presetsMgr = LLPresetsManager.getInstance()
        val presetNames = presetsMgr.loadPresetNamesFromDir(PRESETS_CAMERA, DEFAULT_BOTTOM)
        val activePreset = gSavedSettings.getString("PresetCameraActive")

        for (name in presetNames) {
            val isDefault = presetsMgr.isDefaultCameraPreset(name)
            val item = LLCameraPresetFlatItem(name, isDefault)
            item.postBuild()
            mPresetList.addItem(item)
            if (name == activePreset) {
                mPresetList.selectItem(item)
            }
        }
    }

    private fun onSelectionChange() {
        val selectedPreset = mPresetList.getSelectedItem() as? LLCameraPresetFlatItem ?: return
        LLFloaterCamera.switchToPreset(selectedPreset.getPresetName())
    }
}

class LLCameraPresetFlatItem(
    private val mPresetName: String,
    private val mIsDefaultPrest: Boolean
) : LLPanel() {

    private lateinit var mDeleteBtn: LLButton
    private lateinit var mResetBtn: LLButton

    init {
        mCommitCallbackRegistrar.add("CameraPresets.Delete") { onDeleteBtnClick() }
        mCommitCallbackRegistrar.add("CameraPresets.Reset") { onResetBtnClick() }
        buildFromFile("panel_camera_preset_item.xml")
    }

    override fun postBuild(): Boolean {
        mDeleteBtn = getChild<LLButton>("delete_btn")
        mDeleteBtn.setVisible(false)

        mResetBtn = getChild<LLButton>("reset_btn")
        mResetBtn.setVisible(false)

        val style = LLStyle.Params()
        val nameText = getChild<LLTextBox>("preset_name")
        val newDesc = LLFontDescriptor(nameText.getFont().getFontDesc())
        newDesc.setStyle(if (mIsDefaultPrest) LLFontGL.ITALIC else LLFontGL.NORMAL)
        val newFont = LLFontGL.getFont(newDesc)
        style.font = newFont
        nameText.setText(mPresetName, style)

        return true
    }

    override fun onMouseEnter(x: Int, y: Int, mask: Int) {
        mDeleteBtn.setVisible(!mIsDefaultPrest)
        mResetBtn.setVisible(mIsDefaultPrest)
        getChildView("hovered_icon").setVisible(true)
        super.onMouseEnter(x, y, mask)
    }

    override fun onMouseLeave(x: Int, y: Int, mask: Int) {
        mDeleteBtn.setVisible(false)
        mResetBtn.setVisible(false)
        getChildView("hovered_icon").setVisible(false)
        super.onMouseLeave(x, y, mask)
    }

    fun setValue(value: LLSD) {
        if (!value.isMap()) return
        if (!value.has("selected")) return
        getChildView("selected_icon").setVisible(value["selected"].asBoolean())
    }

    fun getPresetName(): String = mPresetName

    private fun onDeleteBtnClick() {
        if (!LLPresetsManager.getInstance().deletePreset(PRESETS_CAMERA, mPresetName)) {
            val args = LLSD()
            args["NAME"] = mPresetName
            LLNotificationsUtil.add("PresetNotDeleted", args)
        } else if (gSavedSettings.getString("PresetCameraActive") == mPresetName) {
            gSavedSettings.setString("PresetCameraActive", "")
        }
    }

    private fun onResetBtnClick() {
        LLPresetsManager.getInstance().resetCameraPreset(mPresetName)
    }
}
