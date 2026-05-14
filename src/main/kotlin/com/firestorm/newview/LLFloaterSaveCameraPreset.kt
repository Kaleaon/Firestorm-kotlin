package com.firestorm.newview

import java.util.UUID

// ============================================================================
// Shared stubs used by save-preset floaters (not defined elsewhere in package)
// ============================================================================

open class LLModalDialog(key: Any) : LLFloater(key)

class LLRadioGroup {
    fun getSelectedIndex(): Int = 0
    fun setSelectedIndex(index: Int) { System.err.println("LLRadioGroup: set selected radio index not yet implemented") }
    fun setCommitCallback(cb: () -> Unit) { System.err.println("LLRadioGroup: setCommitCallback on radio group not yet implemented") }
}

class LLComboBox {
    fun getSimple(): String = ""
    fun setEnabled(enabled: Boolean) { System.err.println("LLComboBox: setEnabled on combo box not yet implemented") }
    fun setTextEntryCallback(cb: () -> Unit) { System.err.println("LLComboBox: setTextEntryCallback on combo box not yet implemented") }
    fun setCommitCallback(cb: () -> Unit) { System.err.println("LLComboBox: setCommitCallback on combo box not yet implemented") }
}

enum class DefaultOptions { HIDE, BOTTOM }

const val PRESETS_CAMERA = "camera"
const val PRESETS_DEFAULT = "default"
const val PRESETS_DEFAULT_UPPER = "DEFAULT"

object LLPresetsManager {
    fun setPresetListChangeCallback(cb: () -> Unit) { System.err.println("LLPresetsManager: register preset list change callback not yet implemented") }
    fun setPresetNamesInComboBox(subdir: String, combo: LLComboBox?, option: DefaultOptions) { System.err.println("LLPresetsManager: populate combo box from '$subdir' not yet implemented") }
    fun loadPresetNamesFromDir(subdir: String, names: MutableList<String>, option: DefaultOptions) { System.err.println("LLPresetsManager: load preset names from '$subdir' not yet implemented") }
    fun savePreset(subdir: String, name: String): Boolean { System.err.println("LLPresetsManager: save preset '$name' to '$subdir' not yet implemented"); return false }
    val instance: LLPresetsManager get() = this
}

object LLNotificationsUtil {
    fun add(name: String, args: Map<String, Any> = emptyMap()) { System.err.println("LLNotificationsUtil: show '$name' not yet implemented") }
}

val gAgentAvatarp: Any? get() = null
fun isAgentAvatarValid(): Boolean { System.err.println("isAgentAvatarValid: check avatar validity not yet implemented"); return false }

// ============================================================================
// LLFloaterSaveCameraPreset
// ============================================================================

class LLFloaterSaveCameraPreset(key: Any) : LLModalDialog(key) {

    private var mSaveRadioGroup: LLRadioGroup? = null
    private var mNameEditor: LLLineEditor? = null
    private var mPresetCombo: LLComboBox? = null
    private var mSaveButton: LLButton? = null

    open fun postBuild(): Boolean {
        mPresetCombo = getChild("preset_combo")

        mNameEditor = getChild("preset_txt_editor")
        mNameEditor?.setKeystrokeCallback { onPresetNameEdited() }
        // Save on pressing enter in the name field
        mNameEditor?.setCommitCallback { onBtnSave() }

        mSaveButton = getChild("save")
        mSaveButton?.setCommitCallback { onBtnSave() }

        mSaveRadioGroup = getChild("radio_save_preset")
        mSaveRadioGroup?.setCommitCallback { onSwitchSaveReplace() }

        getChild<LLButton>("cancel")?.setCommitCallback { onBtnCancel() }

        LLPresetsManager.setPresetListChangeCallback { onPresetsListChange() }

        return true
    }

    open fun onOpen(key: Any) {
        super_onOpen(key)
        var index = 0
        if (key is Map<*, *> && key.containsKey("index")) {
            index = (key["index"] as? Int) ?: 0
        }

        LLPresetsManager.setPresetNamesInComboBox(PRESETS_CAMERA, mPresetCombo, DefaultOptions.BOTTOM)
        mSaveRadioGroup?.setSelectedIndex(index)
        onPresetNameEdited()
        onSwitchSaveReplace()
    }

    fun onBtnSave() {
        val isSavingNew = mSaveRadioGroup?.getSelectedIndex() == 0
        val name = if (isSavingNew) mNameEditor?.getText() ?: "" else mPresetCombo?.getSimple() ?: ""

        if (name.isEmpty()) {
            return
        }

        if (name == LLTrans.getString(PRESETS_DEFAULT) || name == PRESETS_DEFAULT) {
            LLNotificationsUtil.add("DefaultPresetNotSaved")
        } else {
            if (isAgentAvatarValid() && gAgentAvatarp?.let { it::class.java.getMethod("getParent").invoke(it) } != null) {
                gSavedSettings.setQuaternion("AvatarSitRotation", gAgent.getFrameAgent().getQuaternion())
            }
            if (gAgentCamera.isJoystickCameraUsed()) {
                gSavedSettings.setVector3("CameraOffsetRearView", gAgentCamera.getCurrentCameraOffset())
                gSavedSettings.setVector3d("FocusOffsetRearView", gAgentCamera.getCurrentFocusOffset())
                gAgentCamera.resetCameraZoomFraction()
                gAgentCamera.setFocusOnAvatar(true, true, false)
            } else {
                val cameraOffset = gSavedSettings.getVector3("CameraOffsetRearView") * gAgentCamera.getCurrentCameraZoomFraction()
                gSavedSettings.setVector3("CameraOffsetRearView", cameraOffset)
                gAgentCamera.resetCameraZoomFraction()
            }

            if (isSavingNew) {
                val presetNames = mutableListOf<String>()
                LLPresetsManager.loadPresetNamesFromDir(PRESETS_CAMERA, presetNames, DefaultOptions.HIDE)
                if (presetNames.contains(name)) {
                    LLNotificationsUtil.add("PresetAlreadyExists", mapOf("NAME" to name))
                    return
                }
            }

            if (!LLPresetsManager.savePreset(PRESETS_CAMERA, name)) {
                LLNotificationsUtil.add("PresetNotSaved", mapOf("NAME" to name))
            }
        }

        closeFloater()
    }

    fun onBtnCancel() {
        closeFloater()
    }

    fun onSwitchSaveReplace() {
        val isSavingNew = mSaveRadioGroup?.getSelectedIndex() == 0
        val label = if (isSavingNew) getString("btn_label_save") else getString("btn_label_replace")
        mSaveButton?.setLabel(label)
        mNameEditor?.setEnabled(isSavingNew)
        mPresetCombo?.setEnabled(!isSavingNew)
        if (isSavingNew) {
            onPresetNameEdited()
        } else {
            mSaveButton?.setEnabled(true)
        }
    }

    private fun onPresetsListChange() {
        LLPresetsManager.setPresetNamesInComboBox(PRESETS_CAMERA, mPresetCombo, DefaultOptions.BOTTOM)
    }

    private fun onPresetNameEdited() {
        if (mSaveRadioGroup?.getSelectedIndex() == 0) {
            val name = mNameEditor?.getValue() ?: ""
            mSaveButton?.setEnabled(name.isNotEmpty())
        }
    }

    private fun super_onOpen(key: Any) { System.err.println("LLFloaterSaveCameraPreset: delegate to base onOpen not yet implemented") }
    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(name: String): T? = null
    private fun getString(key: String): String = ""
    private fun closeFloater() { System.err.println("LLFloaterSaveCameraPreset: close this floater not yet implemented") }

    // Extension helpers for gAgent / gAgentCamera / gSavedSettings typed calls.
    // The real globals are already typed in Agent.kt, AgentCamera.kt, ViewerControl.kt.
    private fun Any.getFrameAgent(): Any { System.err.println("LLFloaterSaveCameraPreset: getFrameAgent not yet implemented"); return this }
    private fun Any.getQuaternion(): Any { System.err.println("LLFloaterSaveCameraPreset: getQuaternion not yet implemented"); return this }
    private fun Any.isJoystickCameraUsed(): Boolean { System.err.println("LLFloaterSaveCameraPreset: isJoystickCameraUsed not yet implemented"); return false }
    private fun Any.getCurrentCameraOffset(): Any { System.err.println("LLFloaterSaveCameraPreset: getCurrentCameraOffset not yet implemented"); return this }
    private fun Any.getCurrentFocusOffset(): Any { System.err.println("LLFloaterSaveCameraPreset: getCurrentFocusOffset not yet implemented"); return this }
    private fun Any.getCurrentCameraZoomFraction(): Float { System.err.println("LLFloaterSaveCameraPreset: getCurrentCameraZoomFraction not yet implemented"); return 0f }
    private fun Any.resetCameraZoomFraction() { System.err.println("LLFloaterSaveCameraPreset: resetCameraZoomFraction not yet implemented") }
    private fun Any.setFocusOnAvatar(b1: Boolean, b2: Boolean, b3: Boolean) { System.err.println("LLFloaterSaveCameraPreset: setFocusOnAvatar not yet implemented") }
    private fun Any.getVector3(key: String): Any { System.err.println("LLFloaterSaveCameraPreset: getVector3 '$key' not yet implemented"); return this }
    private fun Any.setVector3(key: String, value: Any) { System.err.println("LLFloaterSaveCameraPreset: setVector3 '$key' not yet implemented") }
    private fun Any.setVector3d(key: String, value: Any) { System.err.println("LLFloaterSaveCameraPreset: setVector3d '$key' not yet implemented") }
    private fun Any.setQuaternion(key: String, value: Any) { System.err.println("LLFloaterSaveCameraPreset: setQuaternion '$key' not yet implemented") }
    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private operator fun Any.times(f: Float): Any { System.err.println("LLFloaterSaveCameraPreset: vector scale not yet implemented"); return this }
}
