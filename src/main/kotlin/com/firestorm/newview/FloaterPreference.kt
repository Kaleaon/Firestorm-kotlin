package com.firestorm.newview

import com.firestorm.floater.Floater
import kotlin.math.ln

private const val VISIBILITY_DEFAULT = "default"
private const val VISIBILITY_HIDDEN  = "hidden"

private const val INDIRECT_MAX_ARC_OFF      = 101u
private const val MIN_INDIRECT_ARC_LIMIT    = 1u
private val MAX_INDIRECT_ARC_LIMIT          = INDIRECT_MAX_ARC_OFF - 1u

private const val MIN_ARC_LIMIT = 20_000f
private const val MAX_ARC_LIMIT = 350_000f
private val MIN_ARC_LOG = ln(MIN_ARC_LIMIT)
private val MAX_ARC_LOG = ln(MAX_ARC_LIMIT)
private val ARC_LIMIT_MAP_SCALE =
    (MAX_ARC_LOG - MIN_ARC_LOG) / (MAX_INDIRECT_ARC_LIMIT - MIN_INDIRECT_ARC_LIMIT).toFloat()

private const val COLUMN_POPUP_SPACER   = 0
private const val COLUMN_POPUP_CHECKBOX = 1
private const val COLUMN_POPUP_LABEL    = 2

enum class GraphicsSettings {
    LOW,
    MID,
    HIGH,
    ULTRA,
}

typealias NotificationsMap = MutableMap<String, String>

open class PanelPreference : Floater(null) {
    val savedValues: MutableMap<Any, Any> = mutableMapOf()
    val savedColors: MutableMap<String, Any> = mutableMapOf()

    open fun apply() = Unit
    open fun cancel(settingsToSkip: List<String> = emptyList()) = Unit
    open fun setHardwareDefaults() = Unit
    open fun saveSettings() = Unit

    fun deletePreset(userData: Any?) {
        System.err.println("FloaterPreference: deletePreset not yet implemented")
    }
    fun savePreset(userData: Any?) {
        System.err.println("FloaterPreference: savePreset not yet implemented")
    }
    fun loadPreset(userData: Any?) {
        System.err.println("FloaterPreference: loadPreset not yet implemented")
    }

    fun updateMediaAutoPlayCheckbox(ctrlName: String) {
        System.err.println("FloaterPreference: updateMediaAutoPlayCheckbox not yet implemented")
    }
}

class PanelPreferenceGraphics : PanelPreference() {
    override fun postBuild(): Boolean {
        System.err.println("FloaterPreference: postBuild (Graphics) not yet implemented")
        return false
    }
    override fun draw() {
        System.err.println("FloaterPreference: draw (Graphics) not yet implemented")
    }
    override fun cancel(settingsToSkip: List<String>) {
        System.err.println("FloaterPreference: cancel (Graphics) not yet implemented")
    }
    override fun saveSettings() {
        System.err.println("FloaterPreference: saveSettings (Graphics) not yet implemented")
    }
    fun resetDirtyChilds() {
        System.err.println("FloaterPreference: resetDirtyChilds (Graphics) not yet implemented")
    }
    override fun setHardwareDefaults() {
        System.err.println("FloaterPreference: setHardwareDefaults (Graphics) not yet implemented")
    }
    fun setPresetText() {
        System.err.println("FloaterPreference: setPresetText not yet implemented")
    }
    fun hasDirtyChilds(): Boolean {
        System.err.println("FloaterPreference: hasDirtyChilds not yet implemented")
        return false
    }
    private fun onPresetsListChange() {
        System.err.println("FloaterPreference: onPresetsListChange not yet implemented")
    }
}

class PanelPreferenceControls : PanelPreference() {
    private var editingControl: String = ""
    private var editingColumn: Int = 0
    private var editingMode: Int = 0

    override fun postBuild(): Boolean {
        System.err.println("FloaterPreference: postBuild (Controls) not yet implemented")
        return false
    }
    override fun apply() {
        System.err.println("FloaterPreference: apply (Controls) not yet implemented")
    }
    override fun cancel(settingsToSkip: List<String>) {
        System.err.println("FloaterPreference: cancel (Controls) not yet implemented")
    }
    override fun saveSettings() {
        System.err.println("FloaterPreference: saveSettings (Controls) not yet implemented")
    }
    fun resetDirtyChilds() {
        System.err.println("FloaterPreference: resetDirtyChilds (Controls) not yet implemented")
    }
    fun onListCommit() {
        System.err.println("FloaterPreference: onListCommit not yet implemented")
    }
    fun onModeCommit() {
        System.err.println("FloaterPreference: onModeCommit not yet implemented")
    }
    fun onRestoreDefaultsBtn() {
        System.err.println("FloaterPreference: onRestoreDefaultsBtn not yet implemented")
    }

    fun canKeyBindHandle(control: String, click: Int, key: Int, mask: Int): Boolean {
        System.err.println("FloaterPreference: canKeyBindHandle not yet implemented")
        return false
    }
    fun setKeyBind(control: String, click: Int, key: Int, mask: Int, set: Boolean) {
        System.err.println("FloaterPreference: setKeyBind not yet implemented")
    }
    fun updateAndApply() {
        System.err.println("FloaterPreference: updateAndApply not yet implemented")
    }
    fun onSetKeyBind(click: Int, key: Int, mask: Int, allModes: Boolean): Boolean {
        System.err.println("FloaterPreference: onSetKeyBind not yet implemented")
        return false
    }
    fun onDefaultKeyBind(allModes: Boolean) {
        System.err.println("FloaterPreference: onDefaultKeyBind not yet implemented")
    }
    fun onCancelKeyBind() {
        System.err.println("FloaterPreference: onCancelKeyBind not yet implemented")
    }

    private fun regenerateControls() {
        System.err.println("FloaterPreference: regenerateControls not yet implemented")
    }
    private fun addControlTableColumns(filename: String): Boolean {
        System.err.println("FloaterPreference: addControlTableColumns not yet implemented")
        return false
    }
    private fun addControlTableRows(filename: String): Boolean {
        System.err.println("FloaterPreference: addControlTableRows not yet implemented")
        return false
    }
    private fun addControlTableSeparator() {
        System.err.println("FloaterPreference: addControlTableSeparator not yet implemented")
    }
    private fun populateControlTable() {
        System.err.println("FloaterPreference: populateControlTable not yet implemented")
    }
    private fun updateTable() {
        System.err.println("FloaterPreference: updateTable not yet implemented")
    }
}

object AvatarComplexityControls {
    fun updateMax(sliderCtrl: Any?, valueLabel: Any?, shortVal: Boolean = false) {
        System.err.println("FloaterPreference: updateMax not yet implemented")
    }
    fun setText(value: UInt, textBox: Any?, shortVal: Boolean = false) {
        System.err.println("FloaterPreference: setText not yet implemented")
    }
    fun updateMaxRenderTime(sliderCtrl: Any?, valueLabel: Any?, shortVal: Boolean = false) {
        System.err.println("FloaterPreference: updateMaxRenderTime not yet implemented")
    }
    fun setRenderTimeText(value: Float, textBox: Any?, shortVal: Boolean = false) {
        System.err.println("FloaterPreference: setRenderTimeText not yet implemented")
    }
    fun setIndirectControls() {
        setIndirectMaxNonImpostors()
        setIndirectMaxArc()
    }
    fun setIndirectMaxNonImpostors() {
        System.err.println("FloaterPreference: setIndirectMaxNonImpostors not yet implemented")
    }
    fun setIndirectMaxArc() {
        System.err.println("FloaterPreference: setIndirectMaxArc not yet implemented")
    }
}

class PanelPreferenceSkins : PanelPreference() {
    private var skin: String = ""
    private var skinTheme: String = ""
    private var skinName: String = ""
    private var skinThemeName: String = ""

    override fun postBuild(): Boolean {
        System.err.println("FloaterPreference: postBuild (Skins) not yet implemented")
        return false
    }
    override fun apply() {
        System.err.println("FloaterPreference: apply (Skins) not yet implemented")
    }
    override fun cancel(settingsToSkip: List<String>) {
        System.err.println("FloaterPreference: cancel (Skins) not yet implemented")
    }
    private fun onSkinChanged() {
        System.err.println("FloaterPreference: onSkinChanged not yet implemented")
    }
    private fun onSkinThemeChanged() {
        System.err.println("FloaterPreference: onSkinThemeChanged not yet implemented")
    }
    private fun refreshSkinList() {
        System.err.println("FloaterPreference: refreshSkinList not yet implemented")
    }
    private fun refreshSkinThemeList() {
        System.err.println("FloaterPreference: refreshSkinThemeList not yet implemented")
    }
    private fun refreshPreviewImage() {
        System.err.println("FloaterPreference: refreshPreviewImage not yet implemented")
    }
    private fun showSkinChangeNotification() {
        System.err.println("FloaterPreference: showSkinChangeNotification not yet implemented")
    }
    fun callbackRestart(notification: Any?, response: Any?) {
        System.err.println("FloaterPreference: callbackRestart not yet implemented")
    }
}

class PanelPreferenceCrashReports : PanelPreference() {
    override fun postBuild(): Boolean {
        System.err.println("FloaterPreference: postBuild (CrashReports) not yet implemented")
        return false
    }
    override fun apply() {
        System.err.println("FloaterPreference: apply (CrashReports) not yet implemented")
    }
    override fun cancel(settingsToSkip: List<String>) {
        System.err.println("FloaterPreference: cancel (CrashReports) not yet implemented")
    }
    fun refresh() {
        System.err.println("FloaterPreference: refresh (CrashReports) not yet implemented")
    }
}

class FSPanelPreferenceBackup : PanelPreference() {
    override fun postBuild(): Boolean {
        System.err.println("FloaterPreference: postBuild (Backup) not yet implemented")
        return false
    }
    private fun onClickSetBackupSettingsPath() {
        System.err.println("FloaterPreference: onClickSetBackupSettingsPath not yet implemented")
    }
    private fun changeBackupSettingsPath(filenames: List<String>, proposedName: String) {
        System.err.println("FloaterPreference: changeBackupSettingsPath not yet implemented")
    }
    private fun onClickSelectAll() = doSelect(true)
    private fun onClickDeselectAll() = doSelect(false)
    private fun onClickBackupSettings() {
        System.err.println("FloaterPreference: onClickBackupSettings not yet implemented")
    }
    private fun onClickRestoreSettings() {
        System.err.println("FloaterPreference: onClickRestoreSettings not yet implemented")
    }
    private fun doSelect(all: Boolean) {
        System.err.println("FloaterPreference: doSelect not yet implemented")
    }
    private fun applySelection(control: Any?, all: Boolean) {
        System.err.println("FloaterPreference: applySelection not yet implemented")
    }
    private fun doBackupSettings(notification: Any?, response: Any?) {
        System.err.println("FloaterPreference: doBackupSettings not yet implemented")
    }
    private fun doRestoreSettings(notification: Any?, response: Any?) {
        System.err.println("FloaterPreference: doRestoreSettings not yet implemented")
    }
    private fun onQuitConfirmed(notification: Any?, response: Any?) {
        System.err.println("FloaterPreference: onQuitConfirmed not yet implemented")
    }
}

class PanelPreferenceOpensim : PanelPreference() {
    override fun postBuild(): Boolean {
        System.err.println("FloaterPreference: postBuild (Opensim) not yet implemented")
        return false
    }
    override fun apply() {
        System.err.println("FloaterPreference: apply (Opensim) not yet implemented")
    }
    override fun cancel(settingsToSkip: List<String>) {
        System.err.println("FloaterPreference: cancel (Opensim) not yet implemented")
    }
    private fun onOpen(key: Any?) {
        System.err.println("FloaterPreference: onOpen (Opensim) not yet implemented")
    }
    private fun onClickAddGrid() {
        System.err.println("FloaterPreference: onClickAddGrid not yet implemented")
    }
    private fun addedGrid(success: Boolean) {
        System.err.println("FloaterPreference: addedGrid not yet implemented")
    }
    private fun onClickClearGrid() {
        System.err.println("FloaterPreference: onClickClearGrid not yet implemented")
    }
    private fun onClickRefreshGrid() {
        System.err.println("FloaterPreference: onClickRefreshGrid not yet implemented")
    }
    private fun onClickRemoveGrid() {
        System.err.println("FloaterPreference: onClickRemoveGrid not yet implemented")
    }
    private fun onSelectGrid() {
        System.err.println("FloaterPreference: onSelectGrid not yet implemented")
    }
    private fun removeGridCb(notification: Any?, response: Any?): Boolean {
        System.err.println("FloaterPreference: removeGridCb not yet implemented")
        return false
    }
    private fun onClickClearDebugSearchURL() {
        System.err.println("FloaterPreference: onClickClearDebugSearchURL not yet implemented")
    }
    private fun onClickPickDebugSearchURL() {
        System.err.println("FloaterPreference: onClickPickDebugSearchURL not yet implemented")
    }
    private fun refreshGridList(success: Boolean = true) {
        System.err.println("FloaterPreference: refreshGridList not yet implemented")
    }
}

class FSPanelPreferenceSounds : PanelPreference() {
    override fun postBuild(): Boolean {
        System.err.println("FloaterPreference: postBuild (Sounds) not yet implemented")
        return false
    }
    override fun apply() {
        System.err.println("FloaterPreference: apply (Sounds) not yet implemented")
    }
    private fun onOutputDeviceChanged(newValue: Any?) {
        System.err.println("FloaterPreference: onOutputDeviceChanged not yet implemented")
    }
    private fun onOutputDeviceSelectionChanged(newValue: Any?) {
        System.err.println("FloaterPreference: onOutputDeviceSelectionChanged not yet implemented")
    }
    private fun onOutputDeviceListChanged(outputDevices: Map<String, String>) {
        System.err.println("FloaterPreference: onOutputDeviceListChanged not yet implemented")
    }
    private fun onMoapInteractionChanged() = updateMoapInteractionSetting()
    private fun updateMoapInteractionSetting() {
        System.err.println("FloaterPreference: updateMoapInteractionSetting not yet implemented")
    }
}

class FloaterPreference(key: Any?) : Floater(key) {

    companion object {
        var skin: String = ""

        fun updateUserInfo(visibility: String, imViaEmail: Boolean, email: String) {
            System.err.println("FloaterPreference: updateUserInfo not yet implemented")
        }

        fun refreshEnabledGraphics() {
            System.err.println("FloaterPreference: refreshEnabledGraphics not yet implemented")
        }

        fun initDoNotDisturbResponse() {
            System.err.println("FloaterPreference: initDoNotDisturbResponse not yet implemented")
        }

        fun updateShowFavoritesCheckbox(value: Boolean) {
            System.err.println("FloaterPreference: updateShowFavoritesCheckbox not yet implemented")
        }

        fun refreshSkin(data: Any?) {
            System.err.println("FloaterPreference: refreshSkin not yet implemented")
        }

        fun saveAvatarPropertiesCoro(capUrl: String, allowPublish: Boolean) {
            System.err.println("FloaterPreference: saveAvatarPropertiesCoro not yet implemented")
        }

        private fun loadFromFilename(filename: String, labelMap: MutableMap<String, String>): Boolean {
            System.err.println("FloaterPreference: loadFromFilename not yet implemented")
            return false
        }
    }

    private var gotPersonalInfo: Boolean = false
    private var originalImViaEmail: Boolean = false
    private var languageChanged: Boolean = false
    private var avatarDataInitialized: Boolean = false
    private var lastQualityLevel: UInt = 0u
    private var priorInstantMessageLogPath: String = ""
    private var originalHideOnlineStatus: Boolean = false
    private var directoryVisibility: String = VISIBILITY_DEFAULT
    private var allowPublish: Boolean = false
    private var savedGraphicsPreset: String = ""
    private var searchDataDirty: Boolean = true
    private val notificationOptions: NotificationsMap = mutableMapOf()
    private val ignorableNotifs: MutableMap<String, Boolean> = mutableMapOf()

    override fun postBuild(): Boolean {
        System.err.println("FloaterPreference: postBuild not yet implemented")
        return false
    }

    override fun onOpen(key: Any?) {
        System.err.println("FloaterPreference: onOpen not yet implemented")
    }

    override fun onClose(appQuitting: Boolean) {
        System.err.println("FloaterPreference: onClose not yet implemented")
    }

    fun apply() {
        System.err.println("FloaterPreference: apply not yet implemented")
    }

    fun cancel(settingsToSkip: List<String> = emptyList()) {
        System.err.println("FloaterPreference: cancel not yet implemented")
    }

    fun changed() {
        System.err.println("FloaterPreference: changed not yet implemented")
    }

    fun changed(sessionId: Any?, mask: UInt) = Unit

    fun processProperties(data: Any?, type: Int) {
        System.err.println("FloaterPreference: processProperties not yet implemented")
    }

    fun saveAvatarProperties() {
        System.err.println("FloaterPreference: saveAvatarProperties not yet implemented")
    }

    fun saveSettings() {
        System.err.println("FloaterPreference: saveSettings not yet implemented")
    }

    fun saveIgnoredNotifications() {
        System.err.println("FloaterPreference: saveIgnoredNotifications not yet implemented")
    }
    fun restoreIgnoredNotifications() {
        System.err.println("FloaterPreference: restoreIgnoredNotifications not yet implemented")
    }

    fun selectPrivacyPanel() {
        System.err.println("FloaterPreference: selectPrivacyPanel not yet implemented")
    }
    fun selectChatPanel() {
        System.err.println("FloaterPreference: selectChatPanel not yet implemented")
    }
    fun selectPanel(name: Any?) {
        System.err.println("FloaterPreference: selectPanel not yet implemented")
    }

    fun getControlNames(names: MutableList<String>) {
        System.err.println("FloaterPreference: getControlNames not yet implemented")
    }

    fun updateClickActionViews() {
        System.err.println("FloaterPreference: updateClickActionViews not yet implemented")
    }
    fun updateSearchableItems() {
        System.err.println("FloaterPreference: updateSearchableItems not yet implemented")
    }

    fun onBtnOK(userData: Any?) {
        apply()
        System.err.println("FloaterPreference: onBtnOK (close floater) not yet implemented")
    }

    fun onBtnCancel(userData: Any?) {
        cancel()
        System.err.println("FloaterPreference: onBtnCancel (close floater) not yet implemented")
    }

    fun setCacheLocation(location: String) {
        System.err.println("FloaterPreference: setCacheLocation not yet implemented")
    }
    fun setSoundCacheLocation(location: String) {
        System.err.println("FloaterPreference: setSoundCacheLocation not yet implemented")
    }

    fun onClickSetCache() {
        System.err.println("FloaterPreference: onClickSetCache not yet implemented")
    }
    fun onClickBrowseCache() {
        System.err.println("FloaterPreference: onClickBrowseCache not yet implemented")
    }
    fun onClickBrowseCrashLogs() {
        System.err.println("FloaterPreference: onClickBrowseCrashLogs not yet implemented")
    }
    fun onClickBrowseSettingsDir() {
        System.err.println("FloaterPreference: onClickBrowseSettingsDir not yet implemented")
    }
    fun onClickResetCache() {
        System.err.println("FloaterPreference: onClickResetCache not yet implemented")
    }
    fun onClickClearCache() {
        System.err.println("FloaterPreference: onClickClearCache not yet implemented")
    }
    fun onClickBrowserClearCache() {
        System.err.println("FloaterPreference: onClickBrowserClearCache not yet implemented")
    }
    fun changeCachePath(filenames: List<String>, proposedName: String) {
        System.err.println("FloaterPreference: changeCachePath not yet implemented")
    }

    fun onClickSetSoundCache() {
        System.err.println("FloaterPreference: onClickSetSoundCache not yet implemented")
    }
    fun onClickBrowseSoundCache() {
        System.err.println("FloaterPreference: onClickBrowseSoundCache not yet implemented")
    }
    fun onClickResetSoundCache() {
        System.err.println("FloaterPreference: onClickResetSoundCache not yet implemented")
    }
    fun changeSoundCachePath(filenames: List<String>, proposedName: String) {
        System.err.println("FloaterPreference: changeSoundCachePath not yet implemented")
    }

    fun onClickResetVoice() {
        System.err.println("FloaterPreference: onClickResetVoice not yet implemented")
    }

    fun onClickLogPath() {
        System.err.println("FloaterPreference: onClickLogPath not yet implemented")
    }
    fun changeLogPath(filenames: List<String>, proposedName: String) {
        System.err.println("FloaterPreference: changeLogPath not yet implemented")
    }
    fun moveTranscriptsAndLog(): Boolean {
        System.err.println("FloaterPreference: moveTranscriptsAndLog not yet implemented")
        return false
    }
    fun onClickResetLogPath() {
        System.err.println("FloaterPreference: onClickResetLogPath not yet implemented")
    }
    fun onClickBrowseChatLogDir() {
        System.err.println("FloaterPreference: onClickBrowseChatLogDir not yet implemented")
    }

    fun onClickJavascript() {
        System.err.println("FloaterPreference: onClickJavascript not yet implemented")
    }
    fun onClickSkin(ctrl: Any?, userData: Any?) {
        System.err.println("FloaterPreference: onClickSkin not yet implemented")
    }
    fun onSelectSkin() {
        System.err.println("FloaterPreference: onSelectSkin not yet implemented")
    }

    fun onClickPreviewUISound(uiSoundId: Any?) {
        System.err.println("FloaterPreference: onClickPreviewUISound not yet implemented")
    }

    fun setPreprocInclude() {
        System.err.println("FloaterPreference: setPreprocInclude not yet implemented")
    }
    fun changePreprocIncludePath(filenames: List<String>, proposedName: String) {
        System.err.println("FloaterPreference: changePreprocIncludePath not yet implemented")
    }

    fun setExternalEditor() {
        System.err.println("FloaterPreference: setExternalEditor not yet implemented")
    }
    fun changeExternalEditorPath(filenames: List<String>) {
        System.err.println("FloaterPreference: changeExternalEditorPath not yet implemented")
    }

    fun onSelectPopup() {
        System.err.println("FloaterPreference: onSelectPopup not yet implemented")
    }
    fun onUpdatePopupFilter() {
        System.err.println("FloaterPreference: onUpdatePopupFilter not yet implemented")
    }
    fun buildPopupList() {
        System.err.println("FloaterPreference: buildPopupList not yet implemented")
    }
    fun resetAllIgnored() {
        System.err.println("FloaterPreference: resetAllIgnored not yet implemented")
    }
    fun setAllIgnored() {
        System.err.println("FloaterPreference: setAllIgnored not yet implemented")
    }

    fun refreshEnabledState() {
        System.err.println("FloaterPreference: refreshEnabledState not yet implemented")
    }
    fun disableUnavailableSettings() {
        System.err.println("FloaterPreference: disableUnavailableSettings not yet implemented")
    }
    fun onCommitWindowedMode() {
        System.err.println("FloaterPreference: onCommitWindowedMode not yet implemented")
    }
    fun refresh() {
        System.err.println("FloaterPreference: refresh not yet implemented")
    }
    fun onChangeQuality(data: Any?) {
        System.err.println("FloaterPreference: onChangeQuality not yet implemented")
    }
    fun onClickClearSettings() {
        System.err.println("FloaterPreference: onClickClearSettings not yet implemented")
    }
    fun onClickChatOnlineNotices() {
        System.err.println("FloaterPreference: onClickChatOnlineNotices not yet implemented")
    }
    fun onClickClearSpamList() {
        System.err.println("FloaterPreference: onClickClearSpamList not yet implemented")
    }
    fun onClickInventoryClearCache() {
        System.err.println("FloaterPreference: onClickInventoryClearCache not yet implemented")
    }
    fun onClickWebBrowserClearCache() {
        System.err.println("FloaterPreference: onClickWebBrowserClearCache not yet implemented")
    }

    fun updateSliderText(sliderCtrl: Any?, textBox: Any?) {
        System.err.println("FloaterPreference: updateSliderText not yet implemented")
    }
    fun updateMaxNonImpostorsLabel(newValue: Any?) {
        System.err.println("FloaterPreference: updateMaxNonImpostorsLabel not yet implemented")
    }
    fun updateMaxComplexityLabel(newValue: Any?) {
        System.err.println("FloaterPreference: updateMaxComplexityLabel not yet implemented")
    }

    fun refreshUI() {
        System.err.println("FloaterPreference: refreshUI not yet implemented")
    }

    fun onChangeMaturity() {
        System.err.println("FloaterPreference: onChangeMaturity not yet implemented")
    }
    fun onChangeComplexityMode(newValue: Any?) {
        System.err.println("FloaterPreference: onChangeComplexityMode not yet implemented")
    }
    fun onChangeModelFolder() {
        System.err.println("FloaterPreference: onChangeModelFolder not yet implemented")
    }
    fun onChangePBRFolder() {
        System.err.println("FloaterPreference: onChangePBRFolder not yet implemented")
    }
    fun onChangeTextureFolder() {
        System.err.println("FloaterPreference: onChangeTextureFolder not yet implemented")
    }
    fun onChangeSoundFolder() {
        System.err.println("FloaterPreference: onChangeSoundFolder not yet implemented")
    }
    fun onChangeAnimationFolder() {
        System.err.println("FloaterPreference: onChangeAnimationFolder not yet implemented")
    }
    fun onClickBlockList() {
        System.err.println("FloaterPreference: onClickBlockList not yet implemented")
    }
    fun onClickProxySettings() {
        System.err.println("FloaterPreference: onClickProxySettings not yet implemented")
    }
    fun onClickTranslationSettings() {
        System.err.println("FloaterPreference: onClickTranslationSettings not yet implemented")
    }
    fun onClickPermsDefault() {
        System.err.println("FloaterPreference: onClickPermsDefault not yet implemented")
    }
    fun onClickRememberedUsernames() {
        System.err.println("FloaterPreference: onClickRememberedUsernames not yet implemented")
    }
    fun onClickAutoReplace() {
        System.err.println("FloaterPreference: onClickAutoReplace not yet implemented")
    }
    fun onClickSpellChecker() {
        System.err.println("FloaterPreference: onClickSpellChecker not yet implemented")
    }
    fun onClickRenderExceptions() {
        System.err.println("FloaterPreference: onClickRenderExceptions not yet implemented")
    }
    fun onClickAutoAdjustments() {
        System.err.println("FloaterPreference: onClickAutoAdjustments not yet implemented")
    }
    fun onClickAdvanced() {
        System.err.println("FloaterPreference: onClickAdvanced not yet implemented")
    }

    fun applyUIColor(ctrl: Any?, param: Any?) {
        System.err.println("FloaterPreference: applyUIColor not yet implemented")
    }
    fun getUIColor(ctrl: Any?, param: Any?) {
        System.err.println("FloaterPreference: getUIColor not yet implemented")
    }

    fun onLogChatHistorySaved() {
        System.err.println("FloaterPreference: onLogChatHistorySaved not yet implemented")
    }

    fun saveGraphicsPreset(preset: String) {
        System.err.println("FloaterPreference: saveGraphicsPreset not yet implemented")
    }

    fun onCopySearch() {
        System.err.println("FloaterPreference: onCopySearch not yet implemented")
    }

    fun setRecommendedSettings() {
        System.err.println("FloaterPreference: setRecommendedSettings not yet implemented")
    }
    fun resetAutotuneSettings() {
        System.err.println("FloaterPreference: resetAutotuneSettings not yet implemented")
    }

    private fun onDeleteTranscripts() {
        System.err.println("FloaterPreference: onDeleteTranscripts not yet implemented")
    }
    private fun onDeleteTranscriptsResponse(notification: Any?, response: Any?) {
        System.err.println("FloaterPreference: onDeleteTranscriptsResponse not yet implemented")
    }

    private fun updateDeleteTranscriptsButton() {
        System.err.println("FloaterPreference: updateDeleteTranscriptsButton not yet implemented")
    }

    private fun updateMaxNonImpostors() {
        System.err.println("FloaterPreference: updateMaxNonImpostors not yet implemented")
    }
    private fun updateIndirectMaxNonImpostors(newValue: Any?) {
        System.err.println("FloaterPreference: updateIndirectMaxNonImpostors not yet implemented")
    }
    private fun setMaxNonImpostorsText(value: UInt, textBox: Any?) {
        System.err.println("FloaterPreference: setMaxNonImpostorsText not yet implemented")
    }
    private fun updateMaxComplexity() {
        System.err.println("FloaterPreference: updateMaxComplexity not yet implemented")
    }
    private fun updateComplexityText() {
        System.err.println("FloaterPreference: updateComplexityText not yet implemented")
    }

    private fun onNameTagOpacityChange(newValue: Any?) {
        System.err.println("FloaterPreference: onNameTagOpacityChange not yet implemented")
    }
    private fun onConsoleOpacityChange(newValue: Any?) {
        System.err.println("FloaterPreference: onConsoleOpacityChange not yet implemented")
    }
    private fun onLanguageChange() {
        System.err.println("FloaterPreference: onLanguageChange not yet implemented")
    }
    private fun onTimeFormatChange() {
        System.err.println("FloaterPreference: onTimeFormatChange not yet implemented")
    }
    private fun onNotificationsChange(optionName: String) {
        System.err.println("FloaterPreference: onNotificationsChange not yet implemented")
    }
    private fun onDoNotDisturbResponseChanged() {
        System.err.println("FloaterPreference: onDoNotDisturbResponseChanged not yet implemented")
    }
    private fun onChangeCustom() {
        System.err.println("FloaterPreference: onChangeCustom not yet implemented")
    }
    private fun updateMeterText(ctrlName: String) {
        System.err.println("FloaterPreference: updateMeterText not yet implemented")
    }
    private fun setHardwareDefaults() {
        System.err.println("FloaterPreference: setHardwareDefaults not yet implemented")
    }
    private fun setRecommended() {
        System.err.println("FloaterPreference: setRecommended not yet implemented")
    }
    private fun onRenderOptionEnable() {
        System.err.println("FloaterPreference: onRenderOptionEnable not yet implemented")
    }
    private fun onAvatarImpostorsEnable() {
        System.err.println("FloaterPreference: onAvatarImpostorsEnable not yet implemented")
    }
    private fun onLocalLightsEnable() {
        System.err.println("FloaterPreference: onLocalLightsEnable not yet implemented")
    }
    private fun onClickActionChange() {
        System.err.println("FloaterPreference: onClickActionChange not yet implemented")
    }
    private fun updateClickActionControls() {
        System.err.println("FloaterPreference: updateClickActionControls not yet implemented")
    }
    private fun onAtmosShaderChange() {
        System.err.println("FloaterPreference: onAtmosShaderChange not yet implemented")
    }
    private fun updateUISoundsControls() {
        System.err.println("FloaterPreference: updateUISoundsControls not yet implemented")
    }
    private fun populateFontSelectionCombo() {
        System.err.println("FloaterPreference: populateFontSelectionCombo not yet implemented")
    }
    private fun loadFontPresetsFromDir(dir: String, comboBox: Any?) {
        System.err.println("FloaterPreference: loadFontPresetsFromDir not yet implemented")
    }
    private fun onAvatarTagSettingsChanged() {
        System.err.println("FloaterPreference: onAvatarTagSettingsChanged not yet implemented")
    }
    private fun updateAnimatedScriptDialogs() {
        System.err.println("FloaterPreference: updateAnimatedScriptDialogs not yet implemented")
    }
    private fun onPieColorsOverrideChanged() {
        System.err.println("FloaterPreference: onPieColorsOverrideChanged not yet implemented")
    }
    private fun onShowGroupNoticesTopRightChanged() {
        System.err.println("FloaterPreference: onShowGroupNoticesTopRightChanged not yet implemented")
    }
    private fun onUpdateFilterTerm(force: Boolean = false) {
        System.err.println("FloaterPreference: onUpdateFilterTerm not yet implemented")
    }
    private fun collectSearchableItems() {
        System.err.println("FloaterPreference: collectSearchableItems not yet implemented")
    }
    private fun setPersonalInfo(visibility: String, imViaEmail: Boolean, email: String) {
        System.err.println("FloaterPreference: setPersonalInfo not yet implemented")
    }
}

class FloaterPreferenceProxy(key: Any?) : Floater(key) {
    private var socksSettingsDirty: Boolean = false
    private val savedValues: MutableMap<Any, Any> = mutableMapOf()

    companion object {
        fun show() {
            System.err.println("FloaterPreference: show (Proxy) not yet implemented")
        }
    }

    override fun postBuild(): Boolean {
        System.err.println("FloaterPreference: postBuild (Proxy) not yet implemented")
        return false
    }
    override fun onOpen(key: Any?) = saveSettings()
    override fun onClose(appQuitting: Boolean) = cancel()

    fun cancel() {
        System.err.println("FloaterPreference: cancel (Proxy) not yet implemented")
    }
    private fun saveSettings() {
        System.err.println("FloaterPreference: saveSettings (Proxy) not yet implemented")
    }
    private fun onBtnOk() {
        System.err.println("FloaterPreference: onBtnOk (Proxy) not yet implemented")
    }
    private fun onBtnCancel() = cancel()
    private fun onClickCloseBtn(appQuitting: Boolean = false) = cancel()
    private fun onChangeSocksSettings() {
        socksSettingsDirty = true
        System.err.println("FloaterPreference: onChangeSocksSettings not yet implemented")
    }
}
