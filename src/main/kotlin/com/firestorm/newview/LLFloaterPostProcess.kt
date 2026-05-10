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
        TODO("GPU: gPostProcess.tweaks[$variableName] = $value")
    }

    private fun onFloatControlMoved(ctrl: Any, variableName: String) {
        val value = getSliderValue(ctrl)
        TODO("GPU: gPostProcess.tweaks[$variableName] = $value")
    }

    private fun onColorControlRMoved(ctrl: Any, variableName: String) {
        val value = getSliderValue(ctrl)
        TODO("GPU: gPostProcess.tweaks[$variableName][0] = $value")
    }

    private fun onColorControlGMoved(ctrl: Any, variableName: String) {
        val value = getSliderValue(ctrl)
        TODO("GPU: gPostProcess.tweaks[$variableName][1] = $value")
    }

    private fun onColorControlBMoved(ctrl: Any, variableName: String) {
        val value = getSliderValue(ctrl)
        TODO("GPU: gPostProcess.tweaks[$variableName][2] = $value")
    }

    private fun onColorControlIMoved(ctrl: Any, variableName: String) {
        val value = getSliderValue(ctrl)
        TODO("GPU: gPostProcess.tweaks[$variableName][3] = $value")
    }

    fun onLoadEffect(comboBox: Any) {
        val effectName = getComboBoxSelectedValue(comboBox)
        TODO("GPU: gPostProcess.setSelectedEffect($effectName); syncMenu()")
    }

    fun onSaveEffect(editBox: Any) {
        val effectName = getLineEditorValue(editBox)
        TODO("GPU: if gPostProcess.mAllEffects.has(effectName) show alert else gPostProcess.saveEffect(effectName); syncMenu()")
    }

    fun onChangeEffectName(ctrl: Any) {
        val nameEditor = getChildLineEditor("PPEffectNameEditor")
        setLineEditorValue(nameEditor, getCtrlValue(ctrl))
    }

    fun saveAlertCallback(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        val option = getSelectedOption(notification, response)
        if (option == 0) {
            val effectName = notification["payload"]?.let { (it as Map<*, *>)["effect_name"] as? String } ?: return false
            TODO("GPU: gPostProcess.saveEffect($effectName); syncMenu()")
        }
        return false
    }

    fun syncMenu() {
        TODO("GPU: populate PPEffectsCombo from gPostProcess.mAllEffects and sync all slider/toggle values")
    }

    private fun childSetCommitCallback(childName: String, callback: (Any, String) -> Unit, userData: String) =
        TODO("stub: bind child widget commit to callback with userData")

    private fun getChildComboBox(name: String): Any = TODO("stub")
    private fun getChildLineEditor(name: String): Any = TODO("stub")
    private fun getCheckBoxValue(ctrl: Any): Boolean = TODO("stub")
    private fun getSliderValue(ctrl: Any): Float = TODO("stub")
    private fun getComboBoxSelectedValue(combo: Any): String = TODO("stub")
    private fun getLineEditorValue(editor: Any): String = TODO("stub")
    private fun setLineEditorValue(editor: Any, value: String) = TODO("stub")
    private fun getCtrlValue(ctrl: Any): String = TODO("stub")
    private fun getSelectedOption(notification: Map<String, Any>, response: Map<String, Any>): Int = TODO("stub")
    private fun Any.setCommitCallback(cb: (Any) -> Unit) = TODO("stub")
}
