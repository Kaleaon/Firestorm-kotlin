package com.firestorm.newview

import java.util.UUID

typealias NotificationsMap = MutableMap<String, String>

enum class EGraphicsSettings {
    GS_LOW_GRAPHICS,
    GS_MID_GRAPHICS,
    GS_HIGH_GRAPHICS,
    GS_ULTRA_GRAPHICS
}

class LLFloaterPreference(key: Map<String, Any?>) : LLFloater(key) {

    companion object {
        private var sSkin: String = ""

        fun updateUserInfo(visibility: String, imViaEmail: Boolean, email: String) {
            System.err.println("LLFloaterPreference: updateUserInfo not yet implemented")
        }

        fun refreshEnabledGraphics() {
            System.err.println("LLFloaterPreference: refreshEnabledGraphics not yet implemented")
        }

        fun initDoNotDisturbResponse() {
            System.err.println("LLFloaterPreference: initDoNotDisturbResponse not yet implemented")
        }

        fun updateShowFavoritesCheckbox(value: Boolean) {
            System.err.println("LLFloaterPreference: updateShowFavoritesCheckbox not yet implemented")
        }

        fun saveAvatarPropertiesCoro(capUrl: String, allowPublish: Boolean) {
            System.err.println("LLFloaterPreference: saveAvatarPropertiesCoro not yet implemented")
        }

        fun loadFromFilename(filename: String, labelMap: MutableMap<String, String>): Boolean {
            System.err.println("LLFloaterPreference: loadFromFilename not yet implemented")
            return false
        }

        fun refreshSkin(data: Any?) {
            System.err.println("LLFloaterPreference: refreshSkin not yet implemented")
        }
    }

    private var gotPersonalInfo: Boolean = false
    private var originalImViaEmail: Boolean = false
    private var languageChanged: Boolean = false
    private var avatarDataInitialized: Boolean = false
    private var lastQualityLevel: UInt = 0u
    private var priorInstantMessageLogPath: String = ""
    private var originalHideOnlineStatus: Boolean = false
    private var directoryVisibility: String = ""
    private var allowPublish: Boolean = false
    private var savedGraphicsPreset: String = ""
    private var searchDataDirty: Boolean = true
    private var notificationOptions: NotificationsMap = mutableMapOf()
    private var ignorableNotifs: MutableMap<String, Boolean> = mutableMapOf()
    private var impostorsChangedSignal: (() -> Unit)? = null
    private var complexityChangedSignal: (() -> Unit)? = null

    fun apply() {
        System.err.println("LLFloaterPreference: apply not yet implemented")
    }

    fun cancel(settingsToSkip: List<String> = emptyList()) {
        System.err.println("LLFloaterPreference: cancel not yet implemented")
    }

    open fun postBuild(): Boolean {
        System.err.println("LLFloaterPreference: postBuild not yet implemented")
        return false
    }

    open fun onOpen(key: Map<String, Any?>) {
        System.err.println("LLFloaterPreference: onOpen not yet implemented")
    }

    open fun onClose(appQuitting: Boolean) {
        if (!appQuitting) cancel()
        System.err.println("LLFloaterPreference: onClose not yet implemented")
    }

    open fun changed() {
        updateDeleteTranscriptsButton()
    }

    fun changed(sessionId: UUID, mask: UInt) {}

    fun processProperties(data: Any?, type: Any) {
        System.err.println("LLFloaterPreference: processProperties not yet implemented")
    }

    fun saveAvatarProperties() {
        System.err.println("LLFloaterPreference: saveAvatarProperties not yet implemented")
    }

    fun selectPrivacyPanel() {
        System.err.println("LLFloaterPreference: selectPrivacyPanel not yet implemented")
    }

    fun selectChatPanel() {
        System.err.println("LLFloaterPreference: selectChatPanel not yet implemented")
    }

    fun getControlNames(names: MutableList<String>) {
        System.err.println("LLFloaterPreference: getControlNames not yet implemented")
    }

    fun updateClickActionViews() {
        System.err.println("LLFloaterPreference: updateClickActionViews not yet implemented")
    }

    fun updateSearchableItems() {
        collectSearchableItems()
    }

    fun onBtnOK(userdata: Map<String, Any?>) {
        System.err.println("LLFloaterPreference: onBtnOK not yet implemented")
    }

    fun onBtnCancel(userdata: Map<String, Any?>) {
        System.err.println("LLFloaterPreference: onBtnCancel not yet implemented")
    }

    protected fun onClickBrowserClearCache() {
        System.err.println("LLFloaterPreference: onClickBrowserClearCache not yet implemented")
    }

    protected fun onLanguageChange() {
        if (!languageChanged) {
            System.err.println("LLFloaterPreference: onLanguageChange notification not yet implemented")
            languageChanged = true
        }
    }

    protected fun onTimeFormatChange() {
        System.err.println("LLFloaterPreference: onTimeFormatChange not yet implemented")
    }

    protected fun onNotificationsChange(optionName: String) {
        System.err.println("LLFloaterPreference: onNotificationsChange not yet implemented")
    }

    protected fun onNameTagOpacityChange(newValue: Any) {
        System.err.println("LLFloaterPreference: onNameTagOpacityChange not yet implemented")
    }

    protected fun onConsoleOpacityChange(newValue: Any) {
        System.err.println("LLFloaterPreference: onConsoleOpacityChange not yet implemented")
    }

    protected fun onPieColorsOverrideChanged() {
        System.err.println("LLFloaterPreference: onPieColorsOverrideChanged not yet implemented")
    }

    protected fun onDoNotDisturbResponseChanged() {
        System.err.println("LLFloaterPreference: onDoNotDisturbResponseChanged not yet implemented")
    }

    protected fun onChangeCustom() {
        System.err.println("LLFloaterPreference: onChangeCustom not yet implemented")
    }

    protected fun updateMeterText(ctrl: Any) {
        System.err.println("LLFloaterPreference: updateMeterText not yet implemented")
    }

    protected fun setHardwareDefaults() = setRecommendedSettings()

    fun setRecommendedSettings() {
        resetAutotuneSettings()
        System.err.println("LLFloaterPreference: setRecommendedSettings not yet implemented")
    }

    protected fun onRenderOptionEnable() = refreshEnabledGraphics()

    protected fun onAvatarImpostorsEnable() = refreshEnabledGraphics()

    protected fun onLocalLightsEnable() {
        System.err.println("LLFloaterPreference: onLocalLightsEnable not yet implemented")
    }

    protected fun onClickActionChange() {
        System.err.println("LLFloaterPreference: onClickActionChange not yet implemented")
    }

    protected fun updateClickActionControls() {
        System.err.println("LLFloaterPreference: updateClickActionControls not yet implemented")
    }

    protected fun onAtmosShaderChange() {
        System.err.println("LLFloaterPreference: onAtmosShaderChange not yet implemented")
    }

    protected fun updateUISoundsControls() {
        System.err.println("LLFloaterPreference: updateUISoundsControls not yet implemented")
    }

    protected fun populateFontSelectionCombo() {
        System.err.println("LLFloaterPreference: populateFontSelectionCombo not yet implemented")
    }

    protected fun loadFontPresetsFromDir(dir: String, fontSelectionCombo: Any) {
        System.err.println("LLFloaterPreference: loadFontPresetsFromDir not yet implemented")
    }

    protected fun onAvatarTagSettingsChanged() {
        System.err.println("LLFloaterPreference: onAvatarTagSettingsChanged not yet implemented")
    }

    protected fun updateAnimatedScriptDialogs() {
        System.err.println("LLFloaterPreference: updateAnimatedScriptDialogs not yet implemented")
    }

    protected fun onShowGroupNoticesTopRightChanged() {
        System.err.println("LLFloaterPreference: onShowGroupNoticesTopRightChanged not yet implemented")
    }

    fun saveSettings() {
        System.err.println("LLFloaterPreference: saveSettings not yet implemented")
    }

    fun saveIgnoredNotifications() {
        System.err.println("LLFloaterPreference: saveIgnoredNotifications not yet implemented")
    }

    fun restoreIgnoredNotifications() {
        System.err.println("LLFloaterPreference: restoreIgnoredNotifications not yet implemented")
    }

    fun setCacheLocation(location: String) {
        System.err.println("LLFloaterPreference: setCacheLocation not yet implemented")
    }

    fun setSoundCacheLocation(location: String) {
        System.err.println("LLFloaterPreference: setSoundCacheLocation not yet implemented")
    }

    fun onClickSetSoundCache() {
        System.err.println("LLFloaterPreference: onClickSetSoundCache not yet implemented")
    }

    fun changeSoundCachePath(filenames: List<String>, proposedName: String) {
        System.err.println("LLFloaterPreference: changeSoundCachePath not yet implemented")
    }

    fun onClickBrowseSoundCache() {
        System.err.println("LLFloaterPreference: onClickBrowseSoundCache not yet implemented")
    }

    fun onClickResetSoundCache() {
        System.err.println("LLFloaterPreference: onClickResetSoundCache not yet implemented")
    }

    fun onClickResetVoice() {
        System.err.println("LLFloaterPreference: onClickResetVoice not yet implemented")
    }

    fun onClickSetCache() {
        System.err.println("LLFloaterPreference: onClickSetCache not yet implemented")
    }

    fun changeCachePath(filenames: List<String>, proposedName: String) {
        System.err.println("LLFloaterPreference: changeCachePath not yet implemented")
    }

    fun onClickBrowseCache() {
        System.err.println("LLFloaterPreference: onClickBrowseCache not yet implemented")
    }

    fun onClickBrowseCrashLogs() {
        System.err.println("LLFloaterPreference: onClickBrowseCrashLogs not yet implemented")
    }

    fun onClickBrowseChatLogDir() {
        System.err.println("LLFloaterPreference: onClickBrowseChatLogDir not yet implemented")
    }

    fun onClickResetCache() {
        System.err.println("LLFloaterPreference: onClickResetCache not yet implemented")
    }

    fun onClickJavascript() {
        System.err.println("LLFloaterPreference: onClickJavascript not yet implemented")
    }

    fun onClickBrowseSettingsDir() {
        System.err.println("LLFloaterPreference: onClickBrowseSettingsDir not yet implemented")
    }

    fun onClickSkin(ctrl: Any, userdata: Map<String, Any?>) {
        System.err.println("LLFloaterPreference: onClickSkin not yet implemented")
    }

    fun onSelectSkin() {
        System.err.println("LLFloaterPreference: onSelectSkin not yet implemented")
    }

    fun onClickPreviewUISound(uiSoundId: Map<String, Any?>) {
        System.err.println("LLFloaterPreference: onClickPreviewUISound not yet implemented")
    }

    fun setPreprocInclude() {
        System.err.println("LLFloaterPreference: setPreprocInclude not yet implemented")
    }

    fun changePreprocIncludePath(filenames: List<String>, proposedName: String) {
        System.err.println("LLFloaterPreference: changePreprocIncludePath not yet implemented")
    }

    fun setExternalEditor() {
        System.err.println("LLFloaterPreference: setExternalEditor not yet implemented")
    }

    fun changeExternalEditorPath(filenames: List<String>) {
        System.err.println("LLFloaterPreference: changeExternalEditorPath not yet implemented")
    }

    fun onSelectPopup() {
        System.err.println("LLFloaterPreference: onSelectPopup not yet implemented")
    }

    fun onUpdatePopupFilter() {
        System.err.println("LLFloaterPreference: onUpdatePopupFilter not yet implemented")
    }

    fun resetAllIgnored() {
        System.err.println("LLFloaterPreference: resetAllIgnored not yet implemented")
    }

    fun setAllIgnored() {
        System.err.println("LLFloaterPreference: setAllIgnored not yet implemented")
    }

    fun onClickLogPath() {
        System.err.println("LLFloaterPreference: onClickLogPath not yet implemented")
    }

    fun changeLogPath(filenames: List<String>, proposedName: String) {
        System.err.println("LLFloaterPreference: changeLogPath not yet implemented")
    }

    fun moveTranscriptsAndLog(): Boolean {
        System.err.println("LLFloaterPreference: moveTranscriptsAndLog not yet implemented")
        return false
    }

    fun onClickResetLogPath() {
        System.err.println("LLFloaterPreference: onClickResetLogPath not yet implemented")
    }

    fun setPersonalInfo(visibility: String, imViaEmail: Boolean, email: String) {
        gotPersonalInfo = true
        originalImViaEmail = imViaEmail
        originalHideOnlineStatus = visibility == "hidden"
        directoryVisibility = visibility
        System.err.println("LLFloaterPreference: setPersonalInfo UI update not yet implemented")
    }

    fun refreshEnabledState() {
        System.err.println("LLFloaterPreference: refreshEnabledState not yet implemented")
    }

    fun disableUnavailableSettings() {
        System.err.println("LLFloaterPreference: disableUnavailableSettings not yet implemented")
    }

    fun onCommitWindowedMode() {
        System.err.println("LLFloaterPreference: onCommitWindowedMode not yet implemented")
    }

    fun refresh() {
        System.err.println("LLFloaterPreference: refresh not yet implemented")
    }

    fun onChangeQuality(data: Map<String, Any?>) {
        System.err.println("LLFloaterPreference: onChangeQuality not yet implemented")
    }

    fun onClickClearSettings() {
        System.err.println("LLFloaterPreference: onClickClearSettings not yet implemented")
    }

    fun onClickChatOnlineNotices() {
        System.err.println("LLFloaterPreference: onClickChatOnlineNotices not yet implemented")
    }

    fun onClickClearSpamList() {
        System.err.println("LLFloaterPreference: onClickClearSpamList not yet implemented")
    }

    fun onClickInventoryClearCache() {
        System.err.println("LLFloaterPreference: onClickInventoryClearCache not yet implemented")
    }

    fun onClickWebBrowserClearCache() {
        System.err.println("LLFloaterPreference: onClickWebBrowserClearCache not yet implemented")
    }

    fun updateSliderText(slider: Any, textBox: Any) {
        System.err.println("LLFloaterPreference: updateSliderText not yet implemented")
    }

    fun updateMaxNonImpostorsLabel(newValue: Any) {
        System.err.println("LLFloaterPreference: updateMaxNonImpostorsLabel not yet implemented")
    }

    fun updateMaxComplexityLabel(newValue: Any) {
        System.err.println("LLFloaterPreference: updateMaxComplexityLabel not yet implemented")
    }

    fun refreshUI() {
        System.err.println("LLFloaterPreference: refreshUI not yet implemented")
    }

    fun onChangeMaturity() {
        System.err.println("LLFloaterPreference: onChangeMaturity not yet implemented")
    }

    fun onChangeComplexityMode(newValue: Any) {
        System.err.println("LLFloaterPreference: onChangeComplexityMode not yet implemented")
    }

    fun onChangeModelFolder() {
        System.err.println("LLFloaterPreference: onChangeModelFolder not yet implemented")
    }

    fun onChangePBRFolder() {
        System.err.println("LLFloaterPreference: onChangePBRFolder not yet implemented")
    }

    fun onChangeTextureFolder() {
        System.err.println("LLFloaterPreference: onChangeTextureFolder not yet implemented")
    }

    fun onChangeSoundFolder() {
        System.err.println("LLFloaterPreference: onChangeSoundFolder not yet implemented")
    }

    fun onChangeAnimationFolder() {
        System.err.println("LLFloaterPreference: onChangeAnimationFolder not yet implemented")
    }

    fun onClickBlockList() {
        System.err.println("LLFloaterPreference: onClickBlockList not yet implemented")
    }

    fun onClickProxySettings() {
        System.err.println("LLFloaterPreference: onClickProxySettings not yet implemented")
    }

    fun onClickTranslationSettings() {
        System.err.println("LLFloaterPreference: onClickTranslationSettings not yet implemented")
    }

    fun onClickPermsDefault() {
        System.err.println("LLFloaterPreference: onClickPermsDefault not yet implemented")
    }

    fun onClickRememberedUsernames() {
        System.err.println("LLFloaterPreference: onClickRememberedUsernames not yet implemented")
    }

    fun onClickAutoReplace() {
        System.err.println("LLFloaterPreference: onClickAutoReplace not yet implemented")
    }

    fun onClickSpellChecker() {
        System.err.println("LLFloaterPreference: onClickSpellChecker not yet implemented")
    }

    fun onClickRenderExceptions() {
        System.err.println("LLFloaterPreference: onClickRenderExceptions not yet implemented")
    }

    fun onClickAutoAdjustments() {
        System.err.println("LLFloaterPreference: onClickAutoAdjustments not yet implemented")
    }

    fun onClickAdvanced() {
        System.err.println("LLFloaterPreference: onClickAdvanced not yet implemented")
    }

    fun applyUIColor(ctrl: Any, param: Map<String, Any?>) {
        System.err.println("LLFloaterPreference: applyUIColor not yet implemented")
    }

    fun getUIColor(ctrl: Any, param: Map<String, Any?>) {
        System.err.println("LLFloaterPreference: getUIColor not yet implemented")
    }

    fun onLogChatHistorySaved() {
        System.err.println("LLFloaterPreference: onLogChatHistorySaved not yet implemented")
    }

    fun buildPopupList() {
        System.err.println("LLFloaterPreference: buildPopupList not yet implemented")
    }

    fun selectPanel(name: Map<String, Any?>) {
        System.err.println("LLFloaterPreference: selectPanel not yet implemented")
    }

    fun saveGraphicsPreset(preset: String) {
        savedGraphicsPreset = preset
    }

    fun onCopySearch() {
        System.err.println("LLFloaterPreference: onCopySearch not yet implemented")
    }

    fun resetAutotuneSettings() {
        System.err.println("LLFloaterPreference: resetAutotuneSettings not yet implemented")
    }

    private fun onDeleteTranscripts() {
        System.err.println("LLFloaterPreference: onDeleteTranscripts not yet implemented")
    }

    private fun onDeleteTranscriptsResponse(notification: Map<String, Any>, response: Map<String, Any>) {
        System.err.println("LLFloaterPreference: onDeleteTranscriptsResponse not yet implemented")
    }

    private fun updateDeleteTranscriptsButton() {
        System.err.println("LLFloaterPreference: updateDeleteTranscriptsButton not yet implemented")
    }

    private fun updateMaxNonImpostors() {
        System.err.println("LLFloaterPreference: updateMaxNonImpostors not yet implemented")
    }

    private fun updateIndirectMaxNonImpostors(newValue: Any) {
        System.err.println("LLFloaterPreference: updateIndirectMaxNonImpostors not yet implemented")
    }

    private fun setMaxNonImpostorsText(value: UInt, textBox: Any) {
        System.err.println("LLFloaterPreference: setMaxNonImpostorsText not yet implemented")
    }

    private fun updateMaxComplexity() {
        System.err.println("LLFloaterPreference: updateMaxComplexity not yet implemented")
    }

    private fun updateComplexityText() {
        System.err.println("LLFloaterPreference: updateComplexityText not yet implemented")
    }

    private fun onUpdateFilterTerm(force: Boolean = false) {
        System.err.println("LLFloaterPreference: onUpdateFilterTerm not yet implemented")
    }

    private fun collectSearchableItems() {
        System.err.println("LLFloaterPreference: collectSearchableItems not yet implemented")
    }
}

open class LLPanelPreference : Any() {
    protected val savedValues: MutableMap<Any, Any?> = mutableMapOf()
    private val savedColors: MutableMap<String, LLColor4> = mutableMapOf()
    private var originalMapPickRadiusTransparency: Float = 0f

    open fun postBuild(): Boolean {
        System.err.println("LLPanelPreference: postBuild not yet implemented")
        return false
    }

    open fun apply() {
        System.err.println("LLPanelPreference: apply not yet implemented")
    }

    open fun cancel(settingsToSkip: List<String> = emptyList()) {
        System.err.println("LLPanelPreference: cancel not yet implemented")
    }

    open fun setHardwareDefaults() {
        System.err.println("LLPanelPreference: setHardwareDefaults not yet implemented")
    }

    open fun saveSettings() {
        System.err.println("LLPanelPreference: saveSettings not yet implemented")
    }

    fun updateMediaAutoPlayCheckbox(ctrl: Any) {
        System.err.println("LLPanelPreference: updateMediaAutoPlayCheckbox not yet implemented")
    }

    fun deletePreset(userData: Map<String, Any?>) {
        System.err.println("LLPanelPreference: deletePreset not yet implemented")
    }

    fun savePreset(userData: Map<String, Any?>) {
        System.err.println("LLPanelPreference: savePreset not yet implemented")
    }

    fun loadPreset(userData: Map<String, Any?>) {
        System.err.println("LLPanelPreference: loadPreset not yet implemented")
    }

    private fun onEnableGrowlChanged() {
        System.err.println("LLPanelPreference: onEnableGrowlChanged not yet implemented")
    }

    private fun onChatWindowChanged() {
        System.err.println("LLPanelPreference: onChatWindowChanged not yet implemented")
    }

    private fun updateMouselookCombatFeatures() {
        System.err.println("LLPanelPreference: updateMouselookCombatFeatures not yet implemented")
    }

    private fun updateMapPickRadiusTransparency(value: Any) {
        System.err.println("LLPanelPreference: updateMapPickRadiusTransparency not yet implemented")
    }

    private fun onCheckContactListColumnMode() {
        System.err.println("LLPanelPreference: onCheckContactListColumnMode not yet implemented")
    }
}

open class LLPanelPreferenceGraphics : LLPanelPreference() {
    override fun postBuild(): Boolean {
        System.err.println("LLPanelPreferenceGraphics: postBuild not yet implemented")
        return false
    }

    fun draw() {
        System.err.println("LLPanelPreferenceGraphics: draw not yet implemented")
    }

    override fun cancel(settingsToSkip: List<String>) {
        System.err.println("LLPanelPreferenceGraphics: cancel not yet implemented")
    }

    override fun saveSettings() {
        System.err.println("LLPanelPreferenceGraphics: saveSettings not yet implemented")
    }

    fun resetDirtyChilds() {
        System.err.println("LLPanelPreferenceGraphics: resetDirtyChilds not yet implemented")
    }

    override fun setHardwareDefaults() {
        System.err.println("LLPanelPreferenceGraphics: setHardwareDefaults not yet implemented")
    }

    fun setPresetText() {
        System.err.println("LLPanelPreferenceGraphics: setPresetText not yet implemented")
    }

    protected fun hasDirtyChilds(): Boolean {
        System.err.println("LLPanelPreferenceGraphics: hasDirtyChilds not yet implemented")
        return false
    }

    private fun onPresetsListChange() {
        System.err.println("LLPanelPreferenceGraphics: onPresetsListChange not yet implemented")
    }
}

class LLPanelPreferenceControls : LLPanelPreference() {
    private var pControlsTable: Any? = null
    private var pKeyModeBox: Any? = null
    private var editingControl: String = ""
    private var editingColumn: Int = 0
    private var editingMode: Int = 0

    override fun postBuild(): Boolean {
        System.err.println("LLPanelPreferenceControls: postBuild not yet implemented")
        return false
    }

    override fun apply() {
        System.err.println("LLPanelPreferenceControls: apply not yet implemented")
    }

    override fun cancel(settingsToSkip: List<String>) {
        System.err.println("LLPanelPreferenceControls: cancel not yet implemented")
    }

    override fun saveSettings() {
        System.err.println("LLPanelPreferenceControls: saveSettings not yet implemented")
    }

    fun resetDirtyChilds() {
        System.err.println("LLPanelPreferenceControls: resetDirtyChilds not yet implemented")
    }

    fun onListCommit() {
        System.err.println("LLPanelPreferenceControls: onListCommit not yet implemented")
    }

    fun onModeCommit() {
        System.err.println("LLPanelPreferenceControls: onModeCommit not yet implemented")
    }

    fun onRestoreDefaultsBtn() {
        System.err.println("LLPanelPreferenceControls: onRestoreDefaultsBtn not yet implemented")
    }

    fun onRestoreDefaultsResponse(notification: Map<String, Any>, response: Map<String, Any>) {
        System.err.println("LLPanelPreferenceControls: onRestoreDefaultsResponse not yet implemented")
    }

    fun canKeyBindHandle(control: String, click: Any, key: Any, mask: Any): Boolean {
        System.err.println("LLPanelPreferenceControls: canKeyBindHandle not yet implemented")
        return false
    }

    fun setKeyBind(control: String, click: Any, key: Any, mask: Any, set: Boolean) {
        System.err.println("LLPanelPreferenceControls: setKeyBind not yet implemented")
    }

    fun updateAndApply() {
        System.err.println("LLPanelPreferenceControls: updateAndApply not yet implemented")
    }

    fun onSetKeyBind(click: Any, key: Any, mask: Any, allModes: Boolean): Boolean {
        System.err.println("LLPanelPreferenceControls: onSetKeyBind not yet implemented")
        return false
    }

    fun onDefaultKeyBind(allModes: Boolean) {
        System.err.println("LLPanelPreferenceControls: onDefaultKeyBind not yet implemented")
    }

    fun onCancelKeyBind() {
        System.err.println("LLPanelPreferenceControls: onCancelKeyBind not yet implemented")
    }

    private fun regenerateControls() {
        System.err.println("LLPanelPreferenceControls: regenerateControls not yet implemented")
    }

    private fun addControlTableColumns(filename: String): Boolean {
        System.err.println("LLPanelPreferenceControls: addControlTableColumns not yet implemented")
        return false
    }

    private fun addControlTableRows(filename: String): Boolean {
        System.err.println("LLPanelPreferenceControls: addControlTableRows not yet implemented")
        return false
    }

    private fun addControlTableSeparator() {
        System.err.println("LLPanelPreferenceControls: addControlTableSeparator not yet implemented")
    }

    private fun populateControlTable() {
        System.err.println("LLPanelPreferenceControls: populateControlTable not yet implemented")
    }

    private fun updateTable() {
        System.err.println("LLPanelPreferenceControls: updateTable not yet implemented")
    }
}

object LLAvatarComplexityControls {
    private val INDIRECT_MAX_ARC_OFF: UInt = 101u
    private val MIN_INDIRECT_ARC_LIMIT: UInt = 1u
    private val MIN_ARC_LIMIT: Float = 20000f
    private val MAX_ARC_LIMIT: Float = 350000f
    private val MIN_ARC_LOG: Float = Math.log(MIN_ARC_LIMIT.toDouble()).toFloat()
    private val MAX_ARC_LOG: Float = Math.log(MAX_ARC_LIMIT.toDouble()).toFloat()
    private val ARC_LIMIT_MAP_SCALE: Float =
        (MAX_ARC_LOG - MIN_ARC_LOG) / (INDIRECT_MAX_ARC_OFF.toInt() - 1 - MIN_INDIRECT_ARC_LIMIT.toInt())

    fun updateMax(slider: Any, valueLabel: Any, shortVal: Boolean = false) {
        System.err.println("LLAvatarComplexityControls: updateMax not yet implemented")
    }

    fun setText(value: UInt, textBox: Any, shortVal: Boolean = false) {
        System.err.println("LLAvatarComplexityControls: setText not yet implemented")
    }

    fun updateMaxRenderTime(slider: Any, valueLabel: Any, shortVal: Boolean = false) {
        System.err.println("LLAvatarComplexityControls: updateMaxRenderTime not yet implemented")
    }

    fun setRenderTimeText(value: Float, textBox: Any, shortVal: Boolean = false) {
        System.err.println("LLAvatarComplexityControls: setRenderTimeText not yet implemented")
    }

    fun setIndirectControls() {
        setIndirectMaxNonImpostors()
        setIndirectMaxArc()
    }

    fun setIndirectMaxNonImpostors() {
        System.err.println("LLAvatarComplexityControls: setIndirectMaxNonImpostors not yet implemented")
    }

    fun setIndirectMaxArc() {
        System.err.println("LLAvatarComplexityControls: setIndirectMaxArc not yet implemented")
    }
}

class LLPanelPreferenceSkins : LLPanelPreference() {
    private var skin: String = ""
    private var skinTheme: String = ""
    private var skinName: String = ""
    private var skinThemeName: String = ""
    private var skinsInfo: Map<String, Any?> = emptyMap()

    override fun postBuild(): Boolean {
        System.err.println("LLPanelPreferenceSkins: postBuild not yet implemented")
        return false
    }

    override fun apply() {
        System.err.println("LLPanelPreferenceSkins: apply not yet implemented")
    }

    override fun cancel(settingsToSkip: List<String>) {
        System.err.println("LLPanelPreferenceSkins: cancel not yet implemented")
    }

    fun callbackRestart(notification: Map<String, Any>, response: Map<String, Any>) {
        System.err.println("LLPanelPreferenceSkins: callbackRestart not yet implemented")
    }

    protected fun onSkinChanged() {
        System.err.println("LLPanelPreferenceSkins: onSkinChanged not yet implemented")
    }

    protected fun onSkinThemeChanged() {
        System.err.println("LLPanelPreferenceSkins: onSkinThemeChanged not yet implemented")
    }

    protected fun refreshSkinList() {
        System.err.println("LLPanelPreferenceSkins: refreshSkinList not yet implemented")
    }

    protected fun refreshSkinThemeList() {
        System.err.println("LLPanelPreferenceSkins: refreshSkinThemeList not yet implemented")
    }

    protected fun refreshPreviewImage() {
        System.err.println("LLPanelPreferenceSkins: refreshPreviewImage not yet implemented")
    }

    protected fun showSkinChangeNotification() {
        System.err.println("LLPanelPreferenceSkins: showSkinChangeNotification not yet implemented")
    }
}

class LLPanelPreferenceCrashReports : LLPanelPreference() {
    override fun postBuild(): Boolean {
        System.err.println("LLPanelPreferenceCrashReports: postBuild not yet implemented")
        return false
    }

    override fun apply() {
        System.err.println("LLPanelPreferenceCrashReports: apply not yet implemented")
    }

    override fun cancel(settingsToSkip: List<String>) {
        System.err.println("LLPanelPreferenceCrashReports: cancel not yet implemented")
    }

    fun refresh() {
        System.err.println("LLPanelPreferenceCrashReports: refresh not yet implemented")
    }
}

class FSPanelPreferenceBackup : LLPanelPreference() {
    override fun postBuild(): Boolean {
        System.err.println("FSPanelPreferenceBackup: postBuild not yet implemented")
        return false
    }

    protected fun onClickSetBackupSettingsPath() {
        System.err.println("FSPanelPreferenceBackup: onClickSetBackupSettingsPath not yet implemented")
    }

    protected fun changeBackupSettingsPath(filenames: List<String>, proposedName: String) {
        System.err.println("FSPanelPreferenceBackup: changeBackupSettingsPath not yet implemented")
    }

    protected fun onClickSelectAll() = doSelect(true)
    protected fun onClickDeselectAll() = doSelect(false)

    protected fun onClickBackupSettings() {
        System.err.println("FSPanelPreferenceBackup: onClickBackupSettings not yet implemented")
    }

    protected fun onClickRestoreSettings() {
        System.err.println("FSPanelPreferenceBackup: onClickRestoreSettings not yet implemented")
    }

    protected fun doSelect(all: Boolean) {
        System.err.println("FSPanelPreferenceBackup: doSelect not yet implemented")
    }

    protected fun applySelection(control: Any, all: Boolean) {
        System.err.println("FSPanelPreferenceBackup: applySelection not yet implemented")
    }

    protected fun doBackupSettings(notification: Map<String, Any>, response: Map<String, Any>) {
        System.err.println("FSPanelPreferenceBackup: doBackupSettings not yet implemented")
    }

    protected fun doRestoreSettings(notification: Map<String, Any>, response: Map<String, Any>) {
        System.err.println("FSPanelPreferenceBackup: doRestoreSettings not yet implemented")
    }

    protected fun onQuitConfirmed(notification: Map<String, Any>, response: Map<String, Any>) {
        System.err.println("FSPanelPreferenceBackup: onQuitConfirmed not yet implemented")
    }
}

class LLPanelPreferenceOpensim : LLPanelPreference() {
    private var gridListChangedCallbackConnection: (() -> Unit)? = null
    private var gridAddedCallbackConnection: (() -> Unit)? = null
    private var currentGrid: String = ""

    override fun postBuild(): Boolean {
        System.err.println("LLPanelPreferenceOpensim: postBuild not yet implemented")
        return false
    }

    override fun apply() {
        System.err.println("LLPanelPreferenceOpensim: apply not yet implemented")
    }

    override fun cancel(settingsToSkip: List<String>) {
        System.err.println("LLPanelPreferenceOpensim: cancel not yet implemented")
    }

    protected fun onOpen(key: Map<String, Any?>) {
        System.err.println("LLPanelPreferenceOpensim: onOpen not yet implemented")
    }

    protected fun onClickAddGrid() {
        System.err.println("LLPanelPreferenceOpensim: onClickAddGrid not yet implemented")
    }

    protected fun addedGrid(success: Boolean) {
        System.err.println("LLPanelPreferenceOpensim: addedGrid not yet implemented")
    }

    protected fun onClickClearGrid() {
        System.err.println("LLPanelPreferenceOpensim: onClickClearGrid not yet implemented")
    }

    protected fun onClickRefreshGrid() {
        System.err.println("LLPanelPreferenceOpensim: onClickRefreshGrid not yet implemented")
    }

    protected fun onClickRemoveGrid() {
        System.err.println("LLPanelPreferenceOpensim: onClickRemoveGrid not yet implemented")
    }

    protected fun onSelectGrid() {
        System.err.println("LLPanelPreferenceOpensim: onSelectGrid not yet implemented")
    }

    protected fun removeGridCB(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        System.err.println("LLPanelPreferenceOpensim: removeGridCB not yet implemented")
        return false
    }

    protected fun onClickClearDebugSearchURL() {
        System.err.println("LLPanelPreferenceOpensim: onClickClearDebugSearchURL not yet implemented")
    }

    protected fun onClickPickDebugSearchURL() {
        System.err.println("LLPanelPreferenceOpensim: onClickPickDebugSearchURL not yet implemented")
    }

    protected fun refreshGridList(success: Boolean = true) {
        System.err.println("LLPanelPreferenceOpensim: refreshGridList not yet implemented")
    }
}

class FSPanelPreferenceSounds : LLPanelPreference() {
    private var outputDeviceListChangedConnection: (() -> Unit)? = null

    fun postBuild(): Boolean {
        System.err.println("FSPanelPreferenceSounds: postBuild not yet implemented")
        return false
    }

    private fun onOutputDeviceChanged(newValue: Any) {
        System.err.println("FSPanelPreferenceSounds: onOutputDeviceChanged not yet implemented")
    }

    private fun onOutputDeviceSelectionChanged(newValue: Any) {
        System.err.println("FSPanelPreferenceSounds: onOutputDeviceSelectionChanged not yet implemented")
    }

    private fun onOutputDeviceListChanged(outputDevices: Map<String, Any>) {
        System.err.println("FSPanelPreferenceSounds: onOutputDeviceListChanged not yet implemented")
    }

    private fun onMoapInteractionChanged() = updateMoapInteractionSetting()

    private fun updateMoapInteractionSetting() {
        System.err.println("FSPanelPreferenceSounds: updateMoapInteractionSetting not yet implemented")
    }
}

class LLFloaterPreferenceProxy(key: Map<String, Any?>) : LLFloater(key) {
    companion object {
        fun show() {
            System.err.println("LLFloaterPreferenceProxy: show not yet implemented")
        }
    }

    private var socksSettingsDirty: Boolean = false
    private val savedValues: MutableMap<Any, Any?> = mutableMapOf()

    fun cancel() {
        System.err.println("LLFloaterPreferenceProxy: cancel not yet implemented")
    }

    protected fun postBuild(): Boolean {
        System.err.println("LLFloaterPreferenceProxy: postBuild not yet implemented")
        return false
    }

    protected fun onOpen(key: Map<String, Any?>) {
        System.err.println("LLFloaterPreferenceProxy: onOpen not yet implemented")
    }

    protected fun onClose(appQuitting: Boolean) {
        System.err.println("LLFloaterPreferenceProxy: onClose not yet implemented")
    }

    protected fun saveSettings() {
        System.err.println("LLFloaterPreferenceProxy: saveSettings not yet implemented")
    }

    protected fun onBtnOk() {
        System.err.println("LLFloaterPreferenceProxy: onBtnOk not yet implemented")
    }

    protected fun onBtnCancel() {
        cancel()
        System.err.println("LLFloaterPreferenceProxy: onBtnCancel close not yet implemented")
    }

    protected fun onClickCloseBtn(appQuitting: Boolean = false) {
        System.err.println("LLFloaterPreferenceProxy: onClickCloseBtn not yet implemented")
    }

    protected fun onChangeSocksSettings() {
        socksSettingsDirty = true
    }
}
