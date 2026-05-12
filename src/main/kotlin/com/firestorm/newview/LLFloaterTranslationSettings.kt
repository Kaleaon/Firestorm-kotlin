package com.firestorm.newview

class LLFloaterTranslationSettings(key: LLSD) : LLFloater(key) {

    private var mMachineTranslationCB: LLCheckBoxCtrl? = null
    private var mLanguageCombo: LLComboBox? = null
    private var mAzureAPIEndpointEditor: LLComboBox? = null
    private var mAzureAPIKeyEditor: LLLineEditor? = null
    private var mAzureAPIRegionEditor: LLLineEditor? = null
    private var mGoogleAPIKeyEditor: LLLineEditor? = null
    private var mDeepLAPIDomainCombo: LLComboBox? = null
    private var mDeepLAPIKeyEditor: LLLineEditor? = null
    private var mTranslationServiceRadioGroup: LLRadioGroup? = null
    private var mAzureVerifyBtn: LLButton? = null
    private var mGoogleVerifyBtn: LLButton? = null
    private var mDeepLVerifyBtn: LLButton? = null
    private var mOKBtn: LLButton? = null

    private var mAzureKeyVerified: Boolean = false
    private var mGoogleKeyVerified: Boolean = false
    private var mDeepLKeyVerified: Boolean = false

    override fun postBuild(): Boolean {
        mMachineTranslationCB = getChild<LLCheckBoxCtrl>("translate_chat_checkbox")
        mLanguageCombo = getChild<LLComboBox>("translate_language_combo")
        mTranslationServiceRadioGroup = getChild<LLRadioGroup>("translation_service_rg")
        mAzureAPIEndpointEditor = getChild<LLComboBox>("azure_api_endpoint_combo")
        mAzureAPIKeyEditor = getChild<LLLineEditor>("azure_api_key")
        mAzureAPIRegionEditor = getChild<LLLineEditor>("azure_api_region")
        mGoogleAPIKeyEditor = getChild<LLLineEditor>("google_api_key")
        mDeepLAPIDomainCombo = getChild<LLComboBox>("deepl_api_domain_combo")
        mDeepLAPIKeyEditor = getChild<LLLineEditor>("deepl_api_key")
        mAzureVerifyBtn = getChild<LLButton>("verify_azure_api_key_btn")
        mGoogleVerifyBtn = getChild<LLButton>("verify_google_api_key_btn")
        mDeepLVerifyBtn = getChild<LLButton>("verify_deepl_api_key_btn")
        mOKBtn = getChild<LLButton>("ok_btn")

        mMachineTranslationCB?.setCommitCallback { updateControlsEnabledState() }
        mTranslationServiceRadioGroup?.setCommitCallback { updateControlsEnabledState() }
        mOKBtn?.setClickedCallback { onBtnOK() }
        getChild<LLButton>("cancel_btn")?.setClickedCallback { closeFloater(false) }
        mAzureVerifyBtn?.setClickedCallback { onBtnAzureVerify() }
        mGoogleVerifyBtn?.setClickedCallback { onBtnGoogleVerify() }
        mDeepLVerifyBtn?.setClickedCallback { onBtnDeepLVerify() }

        mAzureAPIKeyEditor?.setFocusReceivedCallback { control -> onEditorFocused(control) }
        mAzureAPIKeyEditor?.setKeystrokeCallback { onAzureKeyEdited() }
        mAzureAPIRegionEditor?.setFocusReceivedCallback { control -> onEditorFocused(control) }
        mAzureAPIRegionEditor?.setKeystrokeCallback { onAzureKeyEdited() }

        mAzureAPIEndpointEditor?.setFocusLostCallback { setAzureVerified(false, false, 0) }
        mAzureAPIEndpointEditor?.setCommitCallback { _, _ -> setAzureVerified(false, false, 0) }

        mGoogleAPIKeyEditor?.setFocusReceivedCallback { control -> onEditorFocused(control) }
        mGoogleAPIKeyEditor?.setKeystrokeCallback { onGoogleKeyEdited() }

        mDeepLAPIKeyEditor?.setFocusReceivedCallback { control -> onEditorFocused(control) }
        mDeepLAPIKeyEditor?.setKeystrokeCallback { onDeepLKeyEdited() }

        mDeepLAPIDomainCombo?.setFocusLostCallback { setDeepLVerified(false, false, 0) }
        mDeepLAPIDomainCombo?.setCommitCallback { _, _ -> setDeepLVerified(false, false, 0) }

        center()
        return true
    }

    override fun onOpen(key: LLSD) {
        mMachineTranslationCB?.setValue(gSavedSettings.getBool("TranslateChat"))
        mLanguageCombo?.setSelectedByValue(gSavedSettings.getString("TranslateLanguage"), true)
        mTranslationServiceRadioGroup?.setSelectedByValue(gSavedSettings.getString("TranslationService"), true)

        val azureKey = gSavedSettings.getLLSD("AzureTranslateAPIKey")
        if (azureKey.isMap() && azureKey["id"].asString().isNotEmpty()) {
            mAzureAPIKeyEditor?.setText(azureKey["id"].asString())
            mAzureAPIKeyEditor?.setTentative(false)
            if (azureKey.has("region") && azureKey["region"].asString().isNotEmpty()) {
                mAzureAPIRegionEditor?.setText(azureKey["region"].asString())
                mAzureAPIRegionEditor?.setTentative(false)
            } else {
                mAzureAPIRegionEditor?.setTentative(true)
            }
            mAzureAPIEndpointEditor?.setValue(azureKey["endpoint"])
            verifyKey(LLTranslate.SERVICE_AZURE, azureKey, false)
        } else {
            mAzureAPIKeyEditor?.setTentative(true)
            mAzureAPIRegionEditor?.setTentative(true)
            mAzureKeyVerified = false
        }

        val googleKey = gSavedSettings.getString("GoogleTranslateAPIKey")
        if (googleKey.isNotEmpty()) {
            mGoogleAPIKeyEditor?.setText(googleKey)
            mGoogleAPIKeyEditor?.setTentative(false)
            verifyKey(LLTranslate.SERVICE_GOOGLE, LLSD(googleKey), false)
        } else {
            mGoogleAPIKeyEditor?.setTentative(true)
            mGoogleKeyVerified = false
        }

        val deepLKey = gSavedSettings.getLLSD("DeepLTranslateAPIKey")
        if (deepLKey.isMap() && deepLKey["id"].asString().isNotEmpty()) {
            mDeepLAPIKeyEditor?.setText(deepLKey["id"].asString())
            mDeepLAPIKeyEditor?.setTentative(false)
            mDeepLAPIDomainCombo?.setValue(deepLKey["domain"])
            verifyKey(LLTranslate.SERVICE_DEEPL, deepLKey, false)
        } else {
            mDeepLAPIKeyEditor?.setTentative(true)
            mDeepLKeyVerified = false
        }

        updateControlsEnabledState()
    }

    fun setAzureVerified(ok: Boolean, alert: Boolean, status: Int) {
        if (alert) {
            showAlert(if (ok) "azure_api_key_verified" else "azure_api_key_not_verified", status)
        }
        mAzureKeyVerified = ok
        updateControlsEnabledState()
    }

    fun setGoogleVerified(ok: Boolean, alert: Boolean, status: Int) {
        if (alert) {
            showAlert(if (ok) "google_api_key_verified" else "google_api_key_not_verified", status)
        }
        mGoogleKeyVerified = ok
        updateControlsEnabledState()
    }

    fun setDeepLVerified(ok: Boolean, alert: Boolean, status: Int) {
        if (alert) {
            showAlert(if (ok) "deepl_api_key_verified" else "deepl_api_key_not_verified", status)
        }
        mDeepLKeyVerified = ok
        updateControlsEnabledState()
    }

    override fun onClose(appQuitting: Boolean) {
        val service = gSavedSettings.getString("TranslationService")
        val azureSelected = service == "azure"
        val googleSelected = service == "google"
        val deepLSelected = service == "deepl"

        val serviceVerified =
            (azureSelected && mAzureKeyVerified) ||
            (googleSelected && mGoogleKeyVerified) ||
            (deepLSelected && mDeepLKeyVerified)
        gSavedPerAccountSettings.setBool("TranslatingEnabled", serviceVerified)
    }

    private fun getSelectedService(): String {
        return mTranslationServiceRadioGroup?.getSelectedValue()?.asString() ?: ""
    }

    private fun getEnteredAzureKey(): LLSD {
        val key = LLSD()
        if (mAzureAPIKeyEditor?.getTentative() == false) {
            key["endpoint"] = mAzureAPIEndpointEditor?.getValue()
            key["id"] = mAzureAPIKeyEditor?.getText()
            if (mAzureAPIRegionEditor?.getTentative() == false) {
                key["region"] = mAzureAPIRegionEditor?.getText()
            }
        }
        return key
    }

    private fun getEnteredGoogleKey(): String {
        return if (mGoogleAPIKeyEditor?.getTentative() != false) "" else mGoogleAPIKeyEditor?.getText() ?: ""
    }

    private fun getEnteredDeepLKey(): LLSD {
        val key = LLSD()
        if (mDeepLAPIKeyEditor?.getTentative() == false) {
            key["domain"] = mDeepLAPIDomainCombo?.getValue()
            key["id"] = mDeepLAPIKeyEditor?.getText()
        }
        return key
    }

    private fun showAlert(msgName: String, status: Int) {
        val message = getString(msgName, mapOf("[STATUS]" to status.toString()))
        val args = LLSD()
        args["MESSAGE"] = message
        LLNotificationsUtil.add("GenericAlert", args)
    }

    private fun updateControlsEnabledState() {
        val on = mMachineTranslationCB?.getValue()?.asBoolean() ?: false
        val service = getSelectedService()
        val azureSelected = service == "azure"
        val googleSelected = service == "google"
        val deepLSelected = service == "deepl"

        mTranslationServiceRadioGroup?.setEnabled(on)
        mLanguageCombo?.setEnabled(on)

        getChild<LLTextBox>("azure_api_endoint_label")?.setEnabled(on)
        mAzureAPIEndpointEditor?.setEnabled(on && azureSelected)
        getChild<LLTextBox>("azure_api_key_label")?.setEnabled(on)
        mAzureAPIKeyEditor?.setEnabled(on && azureSelected)
        getChild<LLTextBox>("azure_api_region_label")?.setEnabled(on)
        mAzureAPIRegionEditor?.setEnabled(on && azureSelected)

        mAzureVerifyBtn?.setEnabled(on && azureSelected && !mAzureKeyVerified && getEnteredAzureKey().isMap())

        getChild<LLTextBox>("google_api_key_label")?.setEnabled(on)
        mGoogleAPIKeyEditor?.setEnabled(on && googleSelected)
        mGoogleVerifyBtn?.setEnabled(on && googleSelected && !mGoogleKeyVerified && getEnteredGoogleKey().isNotEmpty())

        getChild<LLTextBox>("deepl_api_domain_label")?.setEnabled(on)
        mDeepLAPIDomainCombo?.setEnabled(on && deepLSelected)
        getChild<LLTextBox>("deepl_api_key_label")?.setEnabled(on)
        mDeepLAPIKeyEditor?.setEnabled(on && deepLSelected)
        mDeepLVerifyBtn?.setEnabled(on && deepLSelected && !mDeepLKeyVerified && getEnteredDeepLKey().isMap())

        val serviceVerified =
            (azureSelected && mAzureKeyVerified) ||
            (googleSelected && mGoogleKeyVerified) ||
            (deepLSelected && mDeepLKeyVerified)
        gSavedPerAccountSettings.setBool("TranslatingEnabled", serviceVerified)

        mOKBtn?.setEnabled(!on || serviceVerified)
    }

    private fun verifyKey(service: Int, key: LLSD, alert: Boolean = true) {
        LLTranslate.verifyKey(service, key) { svc, ok, status ->
            setVerificationStatus(svc, ok, alert, status)
        }
    }

    private fun onEditorFocused(control: LLFocusableElement?) {
        val editor = control as? LLLineEditor ?: return
        if (editor.hasTabStop()) {
            if (editor.getTentative()) {
                editor.setText("")
                editor.setTentative(false)
            }
        }
    }

    private fun onAzureKeyEdited() {
        if (mAzureAPIKeyEditor?.isDirty() == true || mAzureAPIRegionEditor?.isDirty() == true) {
            setAzureVerified(false, false, 0)
        }
    }

    private fun onGoogleKeyEdited() {
        if (mGoogleAPIKeyEditor?.isDirty() == true) {
            setGoogleVerified(false, false, 0)
        }
    }

    private fun onDeepLKeyEdited() {
        if (mDeepLAPIKeyEditor?.isDirty() == true) {
            setDeepLVerified(false, false, 0)
        }
    }

    private fun onBtnAzureVerify() {
        val key = getEnteredAzureKey()
        if (key.isMap()) {
            verifyKey(LLTranslate.SERVICE_AZURE, key)
        }
    }

    private fun onBtnGoogleVerify() {
        val key = getEnteredGoogleKey()
        if (key.isNotEmpty()) {
            verifyKey(LLTranslate.SERVICE_GOOGLE, LLSD(key))
        }
    }

    private fun onBtnDeepLVerify() {
        val key = getEnteredDeepLKey()
        if (key.isMap()) {
            verifyKey(LLTranslate.SERVICE_DEEPL, key)
        }
    }

    private fun onBtnOK() {
        gSavedSettings.setBool("TranslateChat", mMachineTranslationCB?.getValue()?.asBoolean() ?: false)
        gSavedSettings.setString("TranslateLanguage", mLanguageCombo?.getSelectedValue()?.asString() ?: "")
        gSavedSettings.setString("TranslationService", getSelectedService())
        gSavedSettings.setLLSD("AzureTranslateAPIKey", getEnteredAzureKey())
        gSavedSettings.setString("GoogleTranslateAPIKey", getEnteredGoogleKey())
        gSavedSettings.setLLSD("DeepLTranslateAPIKey", getEnteredDeepLKey())

        closeFloater(false)
    }

    companion object {
        fun setVerificationStatus(service: Int, ok: Boolean, alert: Boolean, status: Int) {
            val floater = LLFloaterReg.getTypedInstance<LLFloaterTranslationSettings>("prefs_translation")
            if (floater == null) {
                LLLog.warn("Cannot find translation settings floater")
                return
            }
            when (service) {
                LLTranslate.SERVICE_AZURE -> floater.setAzureVerified(ok, alert, status)
                LLTranslate.SERVICE_GOOGLE -> floater.setGoogleVerified(ok, alert, status)
                LLTranslate.SERVICE_DEEPL -> floater.setDeepLVerified(ok, alert, status)
            }
        }
    }
}
