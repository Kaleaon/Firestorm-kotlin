package com.firestorm.newview

import java.util.UUID

// ============================================================================
// Shared stubs used by save-preset floaters (not defined elsewhere in package)
// ============================================================================

open class LLModalDialog(key: Any) : LLFloater(key)

class LLRadioGroup {
    fun getSelectedIndex(): Int = TODO("UI: get selected radio index")
    fun setSelectedIndex(index: Int) { TODO("UI: set selected radio index") }
    fun setCommitCallback(cb: () -> Unit) { TODO("UI: setCommitCallback on radio group") }
}

class LLComboBox {
    fun getSimple(): String = TODO("UI: getSimple on combo box")
    fun setEnabled(enabled: Boolean) { TODO("UI: setEnabled on combo box") }
    fun setTextEntryCallback(cb: () -> Unit) { TODO("UI: setTextEntryCallback on combo box") }
    fun setCommitCallback(cb: () -> Unit) { TODO("UI: setCommitCallback on combo box") }
}

enum class DefaultOptions { HIDE, BOTTOM }

const val PRESETS_CAMERA = "camera"
const val PRESETS_DEFAULT = "default"
const val PRESETS_DEFAULT_UPPER = "DEFAULT"

object LLPresetsManager {
    fun setPresetListChangeCallback(cb: () -> Unit) { TODO("PresetsManager: register preset list change callback") }
    fun setPresetNamesInComboBox(subdir: String, combo: LLComboBox?, option: DefaultOptions) { TODO("PresetsManager: populate combo box from '$subdir'") }
    fun loadPresetNamesFromDir(subdir: String, names: MutableList<String>, option: DefaultOptions) { TODO("PresetsManager: load preset names from '$subdir'") }
    fun savePreset(subdir: String, name: String): Boolean { TODO("PresetsManager: save preset '$name' to '$subdir'") }
    val instance: LLPresetsManager get() = this
}

object LLNotificationsUtil {
    fun add(name: String, args: Map<String, Any> = emptyMap()) { TODO("Notifications: show '$name'") }
}

val gAgentAvatarp: Any? get() = TODO("Agent: global avatar pointer")
fun isAgentAvatarValid(): Boolean { TODO("Agent: check avatar validity") }

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

    private fun super_onOpen(key: Any) { TODO("LLModalDialog: delegate to base onOpen") }
    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(name: String): T? = TODO("UI: resolve child widget '$name'")
    private fun getString(key: String): String = TODO("UI: getString '$key'")
    private fun closeFloater() { TODO("UI: close this floater") }

    // Extension helpers for gAgent / gAgentCamera / gSavedSettings typed calls.
    // The real globals are already typed in Agent.kt, AgentCamera.kt, ViewerControl.kt.
    private fun Any.getFrameAgent(): Any = TODO("Agent: getFrameAgent")
    private fun Any.getQuaternion(): Any = TODO("Agent: getQuaternion")
    private fun Any.isJoystickCameraUsed(): Boolean = TODO("AgentCamera: isJoystickCameraUsed")
    private fun Any.getCurrentCameraOffset(): Any = TODO("AgentCamera: getCurrentCameraOffset")
    private fun Any.getCurrentFocusOffset(): Any = TODO("AgentCamera: getCurrentFocusOffset")
    private fun Any.getCurrentCameraZoomFraction(): Float = TODO("AgentCamera: getCurrentCameraZoomFraction")
    private fun Any.resetCameraZoomFraction() { TODO("AgentCamera: resetCameraZoomFraction") }
    private fun Any.setFocusOnAvatar(b1: Boolean, b2: Boolean, b3: Boolean) { TODO("AgentCamera: setFocusOnAvatar") }
    private fun Any.getVector3(key: String): Any = TODO("Settings: getVector3 '$key'")
    private fun Any.setVector3(key: String, value: Any) { TODO("Settings: setVector3 '$key'") }
    private fun Any.setVector3d(key: String, value: Any) { TODO("Settings: setVector3d '$key'") }
    private fun Any.setQuaternion(key: String, value: Any) { TODO("Settings: setQuaternion '$key'") }
    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private operator fun Any.times(f: Float): Any = TODO("Math: vector scale")
}
