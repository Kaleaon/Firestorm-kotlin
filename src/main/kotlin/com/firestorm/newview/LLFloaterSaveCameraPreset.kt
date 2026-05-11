package com.firestorm.newview

import java.util.UUID

open class LLModalDialog(val key: Any)

class LLFloaterSaveCameraPreset(key: Any) : LLModalDialog(key) {

    private var mSaveRadioGroup: LLRadioGroup? = null
    private var mNameEditor: LLLineEditor? = null
    private var mPresetCombo: LLComboBox? = null
    private var mSaveButton: LLButton? = null

    open fun postBuild(): Boolean {
        mPresetCombo = getChild<LLComboBox>("preset_combo")

        mNameEditor = getChild<LLLineEditor>("preset_txt_editor")
        mNameEditor?.setKeystrokeCallback { onPresetNameEdited() }
        // Save on pressing enter in the name field
        mNameEditor?.setCommitCallback { onBtnSave() }

        mSaveButton = getChild<LLButton>("save")
        mSaveButton?.setCommitCallback { onBtnSave() }

        mSaveRadioGroup = getChild<LLRadioGroup>("radio_save_preset")
        mSaveRadioGroup?.setCommitCallback { onSwitchSaveReplace() }

        getChild<LLButton>("cancel")?.setCommitCallback { onBtnCancel() }

        LLPresetsManager.instance.setPresetListChangeCallback { onPresetsListChange() }

        return true
    }

    open fun onOpen(key: Any) {
        super_onOpen(key)
        var index = 0
        if (key is Map<*, *> && key.containsKey("index")) {
            index = (key["index"] as? Int) ?: 0
        }

        LLPresetsManager.instance.setPresetNamesInComboBox(PRESETS_CAMERA, mPresetCombo, DefaultOptions.BOTTOM)

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
            if (isAgentAvatarValid() && gAgentAvatarp?.getParent() != null) {
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
                LLPresetsManager.instance.loadPresetNamesFromDir(PRESETS_CAMERA, presetNames, DefaultOptions.HIDE)
                if (presetNames.contains(name)) {
                    LLNotificationsUtil.add("PresetAlreadyExists", mapOf("NAME" to name))
                    return
                }
            }

            if (!LLPresetsManager.instance.savePreset(PRESETS_CAMERA, name)) {
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
        LLPresetsManager.instance.setPresetNamesInComboBox(PRESETS_CAMERA, mPresetCombo, DefaultOptions.BOTTOM)
    }

    private fun onPresetNameEdited() {
        if (mSaveRadioGroup?.getSelectedIndex() == 0) {
            val name = mNameEditor?.getValue() ?: ""
            mSaveButton?.setEnabled(name.isNotEmpty())
        }
    }

    private fun super_onOpen(key: Any) { TODO("LLModalDialog: forward to base onOpen") }
    private fun <T> getChild(name: String): T? = TODO("UI: resolve child widget '$name'")
    private fun getString(key: String): String = TODO("UI: getString '$key'")
    private fun closeFloater() { TODO("UI: close this floater") }
}

enum class DefaultOptions { HIDE, BOTTOM }

const val PRESETS_CAMERA = "camera"
const val PRESETS_DEFAULT = "default"

object LLPresetsManager {
    val instance: LLPresetsManager = this
    fun setPresetListChangeCallback(cb: () -> Unit) { TODO("PresetsManager: register preset list change callback") }
    fun setPresetNamesInComboBox(subdir: String, combo: LLComboBox?, option: DefaultOptions) { TODO("PresetsManager: populate combo box from '$subdir'") }
    fun loadPresetNamesFromDir(subdir: String, names: MutableList<String>, option: DefaultOptions) { TODO("PresetsManager: load preset names from '$subdir'") }
    fun savePreset(subdir: String, name: String): Boolean { TODO("PresetsManager: save preset '$name' to '$subdir'") }
}

object LLTrans {
    fun getString(key: String): String { TODO("Trans: translate '$key'") }
}

object LLNotificationsUtil {
    fun add(name: String, args: Map<String, Any> = emptyMap()) { TODO("Notifications: show '$name'") }
}

val gAgent: Any get() = TODO("Agent: global agent")
val gAgentAvatarp: Any? get() = TODO("Agent: global avatar pointer")
val gAgentCamera: Any get() = TODO("Agent: global camera")
val gSavedSettings: Any get() = TODO("Settings: global saved settings")
fun isAgentAvatarValid(): Boolean { TODO("Agent: check avatar validity") }

class LLRadioGroup {
    fun getSelectedIndex(): Int = TODO("UI: get selected radio index")
    fun setSelectedIndex(index: Int) { TODO("UI: set selected radio index") }
    fun setCommitCallback(cb: () -> Unit) { TODO("UI: setCommitCallback on radio group") }
}

class LLLineEditor {
    fun getValue(): String = TODO("UI: get line editor value")
    fun getText(): String = TODO("UI: get line editor text")
    fun setEnabled(enabled: Boolean) { TODO("UI: setEnabled on line editor") }
    fun setKeystrokeCallback(cb: () -> Unit) { TODO("UI: setKeystrokeCallback on line editor") }
    fun setCommitCallback(cb: () -> Unit) { TODO("UI: setCommitCallback on line editor") }
    fun setText(text: String) { TODO("UI: setText on line editor") }
    fun setCursorToEnd() { TODO("UI: setCursorToEnd on line editor") }
    fun setFocus(focus: Boolean) { TODO("UI: setFocus on line editor") }
    fun setCommitOnFocusLost(commit: Boolean) { TODO("UI: setCommitOnFocusLost on line editor") }
}

class LLComboBox {
    fun getSimple(): String = TODO("UI: getSimple on combo box")
    fun setEnabled(enabled: Boolean) { TODO("UI: setEnabled on combo box") }
    fun setTextEntryCallback(cb: () -> Unit) { TODO("UI: setTextEntryCallback on combo box") }
    fun setCommitCallback(cb: () -> Unit) { TODO("UI: setCommitCallback on combo box") }
}

class LLButton {
    fun setCommitCallback(cb: () -> Unit) { TODO("UI: setCommitCallback on button") }
    fun setEnabled(enabled: Boolean) { TODO("UI: setEnabled on button") }
    fun setLabel(label: String) { TODO("UI: setLabel on button") }
}
