package com.firestorm.newview

private const val NUDGE_TIME = 0.25f
private const val ORBIT_NUDGE_RATE = 0.05f

enum class CameraControlMode {
    PAN,
    FREE_CAMERA,
    PRESETS,
    MODES
}

class PanelCameraZoom {
    private var plusBtn: Any? = null
    private var minusBtn: Any? = null
    private var slider: Any? = null
    private var rollLeft: Any? = null
    private var rollRight: Any? = null

    fun postBuild(): Boolean {
        System.err.println("PanelCameraZoom: postBuild not yet implemented")
        return false
    }

    fun draw() {
        // GPU: mSlider.setValue(gAgentCamera.getCameraZoomFraction()); LLPanel.draw()
    }

    fun onZoomPlusHeldDown() {
        // APR: val - inc; gAgentCamera.unlockView(); setOrbitInKey(getOrbitRate(time))
    }

    fun onZoomMinusHeldDown() {
        // APR: val + inc; gAgentCamera.unlockView(); setOrbitOutKey(getOrbitRate(time))
    }

    fun onRollLeftHeldDown() {
        // APR: gAgentCamera.unlockView(); setRollLeftKey(getOrbitRate(time))
    }

    fun onRollRightHeldDown() {
        // APR: gAgentCamera.unlockView(); setRollRightKey(getOrbitRate(time))
    }

    fun onCameraTrack() {
        // APR: LLFirstUse.viewPopup(false)
    }

    fun onCameraRotate() {
        // APR: LLFirstUse.viewPopup(false)
    }

    private fun getOrbitRate(time: Float): Float {
        return if (time < NUDGE_TIME) {
            ORBIT_NUDGE_RATE + time * (1.0f - ORBIT_NUDGE_RATE) / NUDGE_TIME
        } else {
            1.0f
        }
    }

    fun onSliderValueChanged() {
        // APR: val zoomLevel = slider.getValueF32(); gAgentCamera.setCameraZoomFraction(zoomLevel)
    }

    fun onOpen(key: Any) = Unit
}

class PanelCameraItem {
    var iconOver: Any? = null
    var iconSelected: Any? = null
    var picture: Any? = null
    var pictureSelected: Any? = null
    var text: Any? = null

    fun postBuild(): Boolean {
        System.err.println("PanelCameraItem: postBuild not yet implemented")
        return false
    }

    fun onAnyMouseClick() {
        // APR: fire mCommitSignal(this, LLSD())
    }

    fun setValue(value: Map<String, Any>) {
        val selected = value["selected"] as? Boolean ?: return
        // APR: set visibility of selected_icon, picture, selected_picture based on 'selected'
    }
}

class CameraInfoPanel(
    private val title: String,
    private val cameraRef: Any,
    private val getFocus: () -> FloatArray
) {
    fun draw() {
        // GPU: render camera info (origin, axes, focus, sight) using LLFontGL
    }
}

class FloaterCamera private constructor(val key: Any) {

    var rotate: Any? = null
    var zoom: PanelCameraZoom? = null
    var track: Any? = null

    private var controls: Any? = null
    private var viewerCameraInfo: Any? = null
    private var agentCameraInfo: Any? = null
    private var presetCombo: Any? = null

    private var closed: Boolean = false
    private var prevMode: CameraControlMode = CameraControlMode.PAN
    private var currMode: CameraControlMode = CameraControlMode.PAN
    private val mode2Button: MutableMap<CameraControlMode, Any> = mutableMapOf()

    companion object {
        var freeCamera: Boolean = false
        var appearanceEditing: Boolean = false

        fun inFreeCameraMode(): Boolean {
            val floater = findInstance() ?: findPhototoolsInstance() ?: findSmallInstance()
            return floater != null &&
                floater.currMode == CameraControlMode.FREE_CAMERA &&
                agentCameraMode() != CAMERA_MODE_MOUSELOOK
        }

        fun onClickCameraItem(param: Any) {
            val name = param.toString()
            when (name) {
                "mouselook_view" -> {
                    // APR: gAgentCamera.changeCameraToMouselook()
                }
                "object_view" -> {
                    for (cam in listOfNotNull(findInstance(), findPhototoolsInstance(), findSmallInstance())) {
                        if (cam.currMode == CameraControlMode.FREE_CAMERA) {
                            cam.switchMode(CameraControlMode.PAN)
                        } else {
                            cam.switchMode(CameraControlMode.FREE_CAMERA)
                        }
                        cam.updateItemsSelection()
                    }
                }
                "reset_view" -> {
                    for (cam in listOfNotNull(findInstance(), findPhototoolsInstance(), findSmallInstance())) {
                        cam.switchMode(CameraControlMode.PAN)
                    }
                    // APR: gAgentCamera.changeCameraToDefault(); switchToPreset("rear_view")
                }
                else -> {
                    for (cam in listOfNotNull(findInstance(), findPhototoolsInstance(), findSmallInstance())) {
                        cam.switchMode(CameraControlMode.PAN)
                    }
                    switchToPreset(name)
                }
            }
        }

        fun onLeavingMouseLook() {
            for (cam in listOfNotNull(findInstance(), findPhototoolsInstance(), findSmallInstance())) {
                cam.updateItemsSelection()
                if (inFreeCameraMode()) {
                    activateCameraTool()
                }
            }
        }

        fun resetCameraMode() {
            findInstance()?.switchMode(CameraControlMode.PAN)
            findPhototoolsInstance()?.switchMode(CameraControlMode.PAN)
            findSmallInstance()?.switchMode(CameraControlMode.PAN)
        }

        fun onAvatarEditingAppearance(editing: Boolean) {
            appearanceEditing = editing
            findInstance()?.handleAvatarEditingAppearance(editing)
            findPhototoolsInstance()?.handleAvatarEditingAppearance(editing)
            findSmallInstance()?.handleAvatarEditingAppearance(editing)
        }

        fun onDebugCameraToggled() {
            val instance = findInstance()
            if (instance != null) {
                instance.showDebugInfo(isDebugCameraEnabled())
            }
            if (isDebugCameraEnabled()) {
                // APR: LLFloaterReg.showInstanceOrBringToFront("camera")
            }
        }

        fun switchToPreset(name: String) {
            if (isCameraPresetLocked()) return
            freeCamera = false
            clearCameraTool()
            // APR: map preset name to CAMERA_PRESET_* and call gAgentCamera.switchCameraPreset; load preset via LLPresetsManager
            for (cam in listOfNotNull(findInstance(), findPhototoolsInstance(), findSmallInstance())) {
                cam.updateItemsSelection()
                cam.switchMode(CameraControlMode.PRESETS)
            }
        }

        private fun findInstance(): FloaterCamera? {
            // APR: LLFloaterReg.findTypedInstance("camera")
            return null
        }
        private fun findPhototoolsInstance(): FloaterCamera? {
            // APR: LLFloaterReg.findTypedInstance("phototools_camera")
            return null
        }
        private fun findSmallInstance(): FloaterCamera? {
            // APR: LLFloaterReg.findTypedInstance("fs_camera_small")
            return null
        }

        private fun activateCameraTool() {
            // APR: LLToolMgr.getInstance().setTransientTool(LLToolCamera.getInstance())
        }
        private fun clearCameraTool() {
            // APR: LLToolMgr clear transient camera tool if active
        }
        private fun agentCameraMode(): Int {
            // APR: gAgentCamera.getCameraMode()
            return 0
        }
        private fun isDebugCameraEnabled(): Boolean {
            // APR: LLView.sDebugCamera
            return false
        }
        private fun isCameraPresetLocked(): Boolean {
            // APR: RlvActions.isCameraPresetLocked()
            return false
        }
        private val CAMERA_MODE_MOUSELOOK: Int get() {
            // APR: CAMERA_MODE_MOUSELOOK constant
            return 0
        }
    }

    fun postBuild(): Boolean {
        // APR: bind controls (rotate, zoom, track, presetCombo, mode buttons); call update(); handleAvatarEditingAppearance(appearanceEditing)
        return true
    }

    fun onOpen(key: Any) {
        // APR: LLFirstUse.viewPopup(); zoom.onOpen(key)
        if (!closed) updateState() else toPrevMode()
        closed = false
        if (presetCombo != null) populatePresetCombo()
        showDebugInfo(isDebugCameraEnabled())
    }

    fun onClose(appQuitting: Boolean) {
        if (appQuitting) return
        if (currMode == CameraControlMode.FREE_CAMERA) currMode = CameraControlMode.PAN
        if (currMode == CameraControlMode.PAN) prevMode = CameraControlMode.PAN
        switchMode(CameraControlMode.PAN)
        closed = true
        // APR: gAgent.setMovementLocked(false)
    }

    fun update() {
        val mode = determineMode()
        if (mode != currMode) setMode(mode)
    }

    fun onSavePreset() {
        // APR: LLFloaterReg.hideInstance("delete_pref_preset", PRESETS_CAMERA); show save_camera_preset floater
    }

    fun onCustomPresetSelected() {
        val selectedPreset = getPresetComboSelectedLabel()
        if (getInactiveLabelText() != selectedPreset) {
            switchToPreset(selectedPreset)
        }
    }

    fun populatePresetCombo() {
        // APR: LLPresetsManager.setPresetNamesInComboBox; handle active preset label
    }

    private fun getCurrentTransparency(): Float {
        // APR: min(CameraOpacity, ActiveFloaterTransparency) from gSavedSettings
        return 0f
    }

    private fun determineMode(): CameraControlMode {
        if (appearanceEditing) return CameraControlMode.PAN
        if (isCurrentToolCamera()) return CameraControlMode.FREE_CAMERA
        if (agentCameraMode() == CAMERA_MODE_MOUSELOOK) return CameraControlMode.PRESETS
        return CameraControlMode.PAN
    }

    private fun toPrevMode() {
        switchMode(prevMode)
    }

    private fun switchMode(mode: CameraControlMode) {
        when (mode) {
            CameraControlMode.PRESETS,
            CameraControlMode.PAN -> {
                freeCamera = false
                setMode(mode)
                clearCameraTool()
            }
            CameraControlMode.FREE_CAMERA -> {
                freeCamera = true
                setMode(mode)
                activateCameraTool()
            }
            CameraControlMode.MODES -> setMode(mode)
        }
    }

    private fun setMode(mode: CameraControlMode) {
        if (mode != currMode) {
            prevMode = currMode
            currMode = mode
        }
        updateState()
    }

    private fun updateState() {
        updateItemsSelection()
        if (currMode == CameraControlMode.FREE_CAMERA) return
        for ((modeKey, btn) in mode2Button) {
            setButtonToggleState(btn, modeKey == currMode)
        }
    }

    fun updateItemsSelection() {
        // APR: set selected state on rear_view, group_view, front_view, tpp_view, mouselook_view, object_view PanelCameraItems
    }

    private fun handleAvatarEditingAppearance(editing: Boolean) = Unit

    private fun showDebugInfo(show: Boolean) {
        if (show) {
            // GPU: add LLCameraInfoPanel children for viewer and agent camera if not already added
        }
        // APR: set agentCameraInfo and viewerCameraInfo visibility
    }

    fun switchViews(mode: CameraControlMode) {
        when (mode) {
            CameraControlMode.PRESETS -> {
                // APR: show preset_views_list, hide others; toggle presets_btn on
            }
            CameraControlMode.MODES -> {
                // APR: show camera_modes_list, hide others; toggle avatarview_btn on
            }
            CameraControlMode.PAN -> {
                // APR: show zoom, hide lists; toggle pan_btn on
            }
            else -> {}
        }
    }

    private fun fillFlatlistFromPanel(list: Any, panel: Any) {
        // APR: iterate panel child list in reverse and add each LLPanel item to flat list
    }

    // --- stubs for platform calls ---
    private fun isCurrentToolCamera(): Boolean {
        // APR: LLToolMgr.getInstance().getCurrentTool() == LLToolCamera.getInstance()
        return false
    }
    private fun agentCameraMode(): Int {
        // APR: gAgentCamera.getCameraMode()
        return 0
    }
    private fun isDebugCameraEnabled(): Boolean {
        // APR: LLView.sDebugCamera
        return false
    }
    private fun clearCameraTool() = Companion.clearCameraTool()
    private fun activateCameraTool() = Companion.activateCameraTool()
    private fun setButtonToggleState(btn: Any, on: Boolean) {
        // APR: btn.setToggleState(on)
    }
    private fun getPresetComboSelectedLabel(): String {
        // APR: presetCombo.getSelectedItemLabel()
        return ""
    }
    private fun getInactiveLabelText(): String {
        // APR: getString("inactive_combo_text")
        return ""
    }
    private val CAMERA_MODE_MOUSELOOK: Int get() {
        // APR: CAMERA_MODE_MOUSELOOK constant
        return 0
    }
}
