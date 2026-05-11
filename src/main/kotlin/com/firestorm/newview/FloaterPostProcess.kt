package com.firestorm.newview

class FloaterPostProcess(key: Any?) : Floater(key) {

    override fun postBuild(): Boolean {
        childSetCommitCallback("ColorFilterToggle",     onBoolToggle,        "enable_color_filter")
        childSetCommitCallback("ColorFilterBrightness", onFloatControlMoved, "brightness")
        childSetCommitCallback("ColorFilterSaturation", onFloatControlMoved, "saturation")
        childSetCommitCallback("ColorFilterContrast",   onFloatControlMoved, "contrast")

        childSetCommitCallback("ColorFilterBaseR", onColorControlRMoved, "contrast_base")
        childSetCommitCallback("ColorFilterBaseG", onColorControlGMoved, "contrast_base")
        childSetCommitCallback("ColorFilterBaseB", onColorControlBMoved, "contrast_base")
        childSetCommitCallback("ColorFilterBaseI", onColorControlIMoved, "contrast_base")

        childSetCommitCallback("NightVisionToggle",       onBoolToggle,        "enable_night_vision")
        childSetCommitCallback("NightVisionBrightMult",   onFloatControlMoved, "brightness_multiplier")
        childSetCommitCallback("NightVisionNoiseSize",    onFloatControlMoved, "noise_size")
        childSetCommitCallback("NightVisionNoiseStrength",onFloatControlMoved, "noise_strength")

        childSetCommitCallback("BloomToggle",   onBoolToggle,        "enable_bloom")
        childSetCommitCallback("BloomExtract",  onFloatControlMoved, "extract_low")
        childSetCommitCallback("BloomSize",     onFloatControlMoved, "bloom_width")
        childSetCommitCallback("BloomStrength", onFloatControlMoved, "bloom_strength")

        val effectsCombo = getChild<ComboBox>("PPEffectsCombo")
        getChild<ComboBox>("PPLoadEffect").setCommitCallback { onLoadEffect(effectsCombo) }
        effectsCombo.setCommitCallback { onChangeEffectName(it) }

        val nameEditor = getChild<LineEditor>("PPEffectNameEditor")
        getChild<ComboBox>("PPSaveEffect").setCommitCallback { onSaveEffect(nameEditor) }

        syncMenu()
        return true
    }

    fun onLoadEffect(comboBox: ComboBox) {
        val effectName = comboBox.selectedValue.asString()
        TODO("GPU: gPostProcess->setSelectedEffect(effectName); syncMenu()")
    }

    fun onSaveEffect(editBox: LineEditor) {
        val effectName = editBox.value.asString()
        TODO("GPU: if effect exists, prompt overwrite via saveAlertCallback; else gPostProcess->saveEffect(effectName); syncMenu()")
    }

    fun onChangeEffectName(ctrl: UICtrl) {
        val editBox = getChild<LineEditor>("PPEffectNameEditor")
        editBox.setValue(ctrl.value)
    }

    fun saveAlertCallback(notification: Any?, response: Any?): Boolean {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) {
            TODO("GPU: gPostProcess->saveEffect(notification[payload][effect_name]); syncMenu()")
        }
        return false
    }

    fun syncMenu() {
        val comboBox = getChild<ComboBox>("PPEffectsCombo")
        comboBox.removeAll()
        TODO("GPU: iterate gPostProcess->mAllEffects, add each name to comboBox; select current effect")
    }

    companion object {
        val onBoolToggle: CommitCallback = { ctrl, userData ->
            val varName = userData as String
            val checked = (ctrl as? CheckBoxCtrl)?.value ?: return@CommitCallback
            TODO("GPU: gPostProcess->tweaks[varName] = checked")
        }

        val onFloatControlMoved: CommitCallback = { ctrl, userData ->
            val varName = userData as String
            val value = (ctrl as? SliderCtrl)?.value ?: return@CommitCallback
            TODO("GPU: gPostProcess->tweaks[varName] = value")
        }

        val onColorControlRMoved: CommitCallback = { ctrl, userData ->
            val varName = userData as String
            val value = (ctrl as? SliderCtrl)?.value ?: return@CommitCallback
            TODO("GPU: gPostProcess->tweaks[varName][0] = value")
        }

        val onColorControlGMoved: CommitCallback = { ctrl, userData ->
            val varName = userData as String
            val value = (ctrl as? SliderCtrl)?.value ?: return@CommitCallback
            TODO("GPU: gPostProcess->tweaks[varName][1] = value")
        }

        val onColorControlBMoved: CommitCallback = { ctrl, userData ->
            val varName = userData as String
            val value = (ctrl as? SliderCtrl)?.value ?: return@CommitCallback
            TODO("GPU: gPostProcess->tweaks[varName][2] = value")
        }

        val onColorControlIMoved: CommitCallback = { ctrl, userData ->
            val varName = userData as String
            val value = (ctrl as? SliderCtrl)?.value ?: return@CommitCallback
            TODO("GPU: gPostProcess->tweaks[varName][3] = value")
        }
    }
}
