package com.firestorm.newview

import java.util.UUID

class LLFloaterHoverHeight(val key: Any) : LLFloater(key) {

    var mModifiers: Int = MASK_NONE
    var mRegionChangedSlot: (() -> Unit)? = null

    fun postBuild(): Boolean {
        val sldrCtrl = getChild<LLSliderCtrl>("HoverHeightSlider")
        sldrCtrl.setMinValue(MIN_HOVER_Z)
        sldrCtrl.setMaxValue(MAX_HOVER_Z)
        sldrCtrl.setSliderMouseUpCallback { onFinalCommit() }
        sldrCtrl.setSliderEditorCommitCallback { onFinalCommit() }
        childSetCommitCallback("HoverHeightSlider") { onSliderMoved(sldrCtrl) }
        childSetCommitCallback("PlusButton") { onPlusButtonClicked() }
        childSetCommitCallback("MinusButton") { onMinusButtonClicked() }
        childSetCommitCallback("ResetButton") { onResetButtonClicked() }

        syncFromPreferenceSetting(this)

        val control = gSavedPerAccountSettings.getControl("AvatarHoverOffsetZ")
        if (control != null) {
            control.getCommitSignal().connect { syncFromPreferenceSetting(this, false) }
        }

        updateEditEnabled()

        if (mRegionChangedSlot == null) {
            mRegionChangedSlot = gAgent.addRegionChangedCallback { onRegionChanged() }
        }
        onRegionChanged()

        return true
    }

    override fun onClose(appQuitting: Boolean) {
        mRegionChangedSlot = null
    }

    fun onFinalCommit() {
        val sldrCtrl = getChild<LLSliderCtrl>("HoverHeightSlider")
        val value = sldrCtrl.getValueF32()
        gSavedPerAccountSettings.setF32("AvatarHoverOffsetZ", value)
    }

    fun handleKey(key: Int, mask: Int, calledFromParent: Boolean): Boolean {
        mModifiers = mask
        return super.handleKey(key, mask, calledFromParent)
    }

    fun handleKeyUp(key: Int, mask: Int, calledFromParent: Boolean): Boolean {
        mModifiers = mask
        return super.handleKeyUp(key, mask, calledFromParent)
    }

    fun onResetButtonClicked() {
        onButtonClicked(0.0f)
    }

    fun onPlusButtonClicked() {
        onButtonClicked(0.01f)
    }

    fun onMinusButtonClicked() {
        onButtonClicked(-0.01f)
    }

    fun onButtonClicked(value: Float) {
        val sldrCtrl = getChild<LLSliderCtrl>("HoverHeightSlider")

        if (value == 0.0f) {
            sldrCtrl.setValue(0.0f)
        } else {
            val delta = when (mModifiers) {
                MASK_ALT -> value * 10.0f
                MASK_CONTROL, MASK_SHIFT -> value * 0.1f
                else -> value
            }
            sldrCtrl.setValue(sldrCtrl.getValueF32() + delta)
        }

        onSliderMoved(sldrCtrl)
        onFinalCommit()
    }

    fun onRegionChanged() {
        val region = gAgent.getRegion()
        if (region != null && region.simulatorFeaturesReceived()) {
            updateEditEnabled()
        } else if (region != null) {
            region.setSimulatorFeaturesReceivedCallback { regionId -> onSimulatorFeaturesReceived(regionId) }
        }
    }

    fun onSimulatorFeaturesReceived(regionId: UUID) {
        val region = gAgent.getRegion()
        if (region != null && region.getRegionID() == regionId) {
            updateEditEnabled()
        }
    }

    fun updateEditEnabled() {
        var enabled = gAgent.getRegion()?.avatarHoverHeightEnabled() == true
        if (!enabled && isAgentAvatarValid() && !gAgentAvatarp.isUsingServerBakes()) {
            enabled = true
        }

        val sldrCtrl = getChild<LLSliderCtrl>("HoverHeightSlider")
        sldrCtrl.setEnabled(enabled)

        getChild<LLButton>("PlusButton").setEnabled(enabled)
        getChild<LLButton>("MinusButton").setEnabled(enabled)
        getChild<LLButton>("ResetButton").setEnabled(enabled)

        if (enabled) {
            syncFromPreferenceSetting(this)
        }
    }

    companion object {
        const val MASK_NONE = 0
        const val MASK_ALT = 0x01
        const val MASK_CONTROL = 0x02
        const val MASK_SHIFT = 0x04

        fun onSliderMoved(ctrl: LLSliderCtrl) {
            if (isAgentAvatarValid()) {
                val value = ctrl.getValueF32()
                val clamped = value.coerceIn(MIN_HOVER_Z, MAX_HOVER_Z)
                val region = gAgent.getRegion()
                if (region != null && region.avatarHoverHeightEnabled()) {
                    if (ctrl.isMouseHeldDown()) {
                        gAgentAvatarp.setHoverOffset(clamped, false)
                    } else {
                        gSavedPerAccountSettings.setF32("AvatarHoverOffsetZ", value)
                    }
                } else if (!gAgentAvatarp.isUsingServerBakes()) {
                    gSavedPerAccountSettings.setF32("AvatarHoverOffsetZ", value)
                }
            }
        }

        fun syncFromPreferenceSetting(self: LLFloaterHoverHeight, updateOffset: Boolean = true) {
            val value = gSavedPerAccountSettings.getF32("AvatarHoverOffsetZ")
            val sldrCtrl = self.getChild<LLSliderCtrl>("HoverHeightSlider")
            sldrCtrl.setValue(value, false)
        }
    }
}
