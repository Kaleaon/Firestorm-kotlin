package com.firestorm.newview

import com.firestorm.ui.LLFloater
import com.firestorm.ui.LLSpinCtrl
import com.firestorm.ui.LLUICtrl
import com.firestorm.llsd.LLSD
import com.firestorm.math.LLVector3
import com.firestorm.math.LLVector3d

class LLFloaterPreferenceViewAdvanced(key: LLSD) : LLFloater(key) {

    init {
        mCommitCallbackRegistrar.add("CommitSettings") { onCommitSettings() }
        mCommitCallbackRegistrar.add("CameraPresets.Save") { onSavePreset() }
    }

    open fun draw() {
        updateCameraControl(gAgentCamera.getCameraOffsetInitial())
        updateFocusControl(gAgentCamera.getFocusOffsetInitial())
        super.draw()
    }

    fun onCommitSettings() {
        val vector = LLVector3(
            x = getChild<LLUICtrl>("camera_x").getValue().asReal().toFloat(),
            y = getChild<LLUICtrl>("camera_y").getValue().asReal().toFloat(),
            z = getChild<LLUICtrl>("camera_z").getValue().asReal().toFloat()
        )
        gSavedSettings.setVector3("CameraOffsetRearView", vector)

        val vector3d = LLVector3d(
            x = getChild<LLUICtrl>("focus_x").getValue().asReal().toFloat(),
            y = getChild<LLUICtrl>("focus_y").getValue().asReal().toFloat(),
            z = getChild<LLUICtrl>("focus_z").getValue().asReal().toFloat()
        )
        gSavedSettings.setVector3d("FocusOffsetRearView", vector3d)
    }

    fun updateCameraControl(vector: LLVector3) {
        getChild<LLSpinCtrl>("camera_x").setValue(vector.x)
        getChild<LLSpinCtrl>("camera_y").setValue(vector.y)
        getChild<LLSpinCtrl>("camera_z").setValue(vector.z)
    }

    fun updateFocusControl(vector3d: LLVector3d) {
        getChild<LLSpinCtrl>("focus_x").setValue(vector3d.x)
        getChild<LLSpinCtrl>("focus_y").setValue(vector3d.y)
        getChild<LLSpinCtrl>("focus_z").setValue(vector3d.z)
    }

    fun onSavePreset() {
        LLFloaterReg.hideInstance("delete_pref_preset", PRESETS_CAMERA)
        LLFloaterReg.hideInstance("load_pref_preset", PRESETS_CAMERA)

        val currentPreset = gSavedSettings.getString("PresetCameraActive")
        val isCustomPreset = currentPreset != "" &&
            !LLPresetsManager.getInstance().isDefaultCameraPreset(currentPreset)

        val key = LLSD()
        key["index"] = if (isCustomPreset) 1 else 0
        LLFloaterReg.showInstance("save_camera_preset", key)
    }
}
