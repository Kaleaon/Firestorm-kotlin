package com.firestorm.newview

open class LLFloater(val key: Map<String, Any?>)

class LLFloaterPostProcess(key: Map<String, Any?>) : LLFloater(key) {

    open fun postBuild(): Boolean {
        childSetCommitCallback("ColorFilterToggle", ::onBoolToggle, "enable_color_filter")
        childSetCommitCallback("ColorFilterBrightness", ::onFloatControlMoved, "brightness")
        childSetCommitCallback("ColorFilterSaturation", ::onFloatControlMoved, "saturation")
        childSetCommitCallback("ColorFilterContrast", ::onFloatControlMoved, "contrast")
        childSetCommitCallback("ColorFilterBaseR", ::onColorControlRMoved, "contrast_base")
        childSetCommitCallback("ColorFilterBaseG", ::onColorControlGMoved, "contrast_base")
        childSetCommitCallback("ColorFilterBaseB", ::onColorControlBMoved, "contrast_base")
        childSetCommitCallback("ColorFilterBaseI", ::onColorControlIMoved, "contrast_base")

        childSetCommitCallback("NightVisionToggle", ::onBoolToggle, "enable_night_vision")
        childSetCommitCallback("NightVisionBrightMult", ::onFloatControlMoved, "brightness_multiplier")
        childSetCommitCallback("NightVisionNoiseSize", ::onFloatControlMoved, "noise_size")
        childSetCommitCallback("NightVisionNoiseStrength", ::onFloatControlMoved, "noise_strength")

        childSetCommitCallback("BloomToggle", ::onBoolToggle, "enable_bloom")
        childSetCommitCallback("BloomExtract", ::onFloatControlMoved, "extract_low")
        childSetCommitCallback("BloomSize", ::onFloatControlMoved, "bloom_width")
        childSetCommitCallback("BloomStrength", ::onFloatControlMoved, "bloom_strength")

        val effectsCombo = getChildComboBox("PPEffectsCombo")
        getChildComboBox("PPLoadEffect").setCommitCallback { onLoadEffect(effectsCombo) }
        effectsCombo.setCommitCallback { ctrl -> onChangeEffectName(ctrl) }

        val nameEditor = getChildLineEditor("PPEffectNameEditor")
        getChildComboBox("PPSaveEffect").setCommitCallback { onSaveEffect(nameEditor) }

        syncMenu()
        return true
    }

    private fun onBoolToggle(ctrl: Any, variableName: String) {
        val value = getCheckBoxValue(ctrl)
        // no-op
    }

    private fun onFloatControlMoved(ctrl: Any, variableName: String) {
        val value = getSliderValue(ctrl)
        // no-op
    }

    private fun onColorControlRMoved(ctrl: Any, variableName: String) {
        val value = getSliderValue(ctrl)
        // no-op
    }

    private fun onColorControlGMoved(ctrl: Any, variableName: String) {
        val value = getSliderValue(ctrl)
        // no-op
    }

    private fun onColorControlBMoved(ctrl: Any, variableName: String) {
        val value = getSliderValue(ctrl)
        // no-op
    }

    private fun onColorControlIMoved(ctrl: Any, variableName: String) {
        val value = getSliderValue(ctrl)
        // no-op
    }

    fun onLoadEffect(comboBox: Any) {
        val effectName = getComboBoxSelectedValue(comboBox)
        System.err.println("LLFloaterPostProcess: onLoadEffect not yet implemented")
    }

    fun onSaveEffect(editBox: Any) {
        val effectName = getLineEditorValue(editBox)
        System.err.println("LLFloaterPostProcess: onSaveEffect not yet implemented")
    }

    fun onChangeEffectName(ctrl: Any) {
        val nameEditor = getChildLineEditor("PPEffectNameEditor")
        setLineEditorValue(nameEditor, getCtrlValue(ctrl))
    }

    fun saveAlertCallback(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        val option = getSelectedOption(notification, response)
        if (option == 0) {
            val effectName = notification["payload"]?.let { (it as Map<*, *>)["effect_name"] as? String } ?: return false
            System.err.println("LLFloaterPostProcess: saveAlertCallback not yet implemented")
        }
        return false
    }

    fun syncMenu() {
        System.err.println("LLFloaterPostProcess: syncMenu not yet implemented")
    }

    private fun childSetCommitCallback(childName: String, callback: (Any, String) -> Unit, userData: String) {
        // no-op
    }

    private fun getChildComboBox(name: String): Any = Object()
    private fun getChildLineEditor(name: String): Any = Object()
    private fun getCheckBoxValue(ctrl: Any): Boolean = false
    private fun getSliderValue(ctrl: Any): Float = 0f
    private fun getComboBoxSelectedValue(combo: Any): String = ""
    private fun getLineEditorValue(editor: Any): String = ""
    private fun setLineEditorValue(editor: Any, value: String) { /* no-op */ }
    private fun getCtrlValue(ctrl: Any): String = ""
    private fun getSelectedOption(notification: Map<String, Any>, response: Map<String, Any>): Int = 0
    private fun Any.setCommitCallback(cb: (Any) -> Unit) { /* no-op */ }
}
