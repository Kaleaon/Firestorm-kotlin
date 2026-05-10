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
            TODO("APR: use JVM equivalent - notify preference instance of user info update")
        }

        fun refreshEnabledGraphics() {
            TODO("GPU: refresh graphics enabled states on preferences and advanced graphics floaters")
        }

        fun initDoNotDisturbResponse() {
            TODO("APR: use JVM equivalent - init per-account DND response strings if not user-modified")
        }

        fun updateShowFavoritesCheckbox(value: Boolean) {
            TODO("APR: use JVM equivalent - find preference instance and update favorites_on_login_check")
        }

        fun saveAvatarPropertiesCoro(capUrl: String, allowPublish: Boolean) {
            TODO("APR: use JVM equivalent - HTTP PUT to $capUrl with allow_publish=$allowPublish")
        }

        fun loadFromFilename(filename: String, labelMap: MutableMap<String, String>): Boolean {
            TODO("APR: use JVM equivalent - parse XML label file into labelMap")
        }

        fun refreshSkin(data: Any?) {
            TODO("APR: use JVM equivalent - reload skin selection radio from saved setting")
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
        TODO("APR: use JVM equivalent - apply all preference panel changes, send agent user info if changed")
    }

    fun cancel(settingsToSkip: List<String> = emptyList()) {
        TODO("APR: use JVM equivalent - cancel all preference panel changes and restore saved graphics preset")
    }

    open fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent - wire all child widget callbacks and initialize UI state")
    }

    open fun onOpen(key: Map<String, Any?>) {
        TODO("APR: use JVM equivalent - init DND response, maturity combo, popup list, filter, tab selection")
    }

    open fun onClose(appQuitting: Boolean) {
        if (!appQuitting) cancel()
        TODO("APR: use JVM equivalent - save last pref tab index, disable login panel refresh")
    }

    open fun changed() {
        updateDeleteTranscriptsButton()
    }

    fun changed(sessionId: UUID, mask: UInt) {}

    fun processProperties(data: Any?, type: Any) {
        TODO("APR: use JVM equivalent - handle APT_PROPERTIES_LEGACY to read allow_publish flag")
    }

    fun saveAvatarProperties() {
        TODO("APR: use JVM equivalent - launch coro to PUT avatar properties if allowPublish changed")
    }

    fun selectPrivacyPanel() {
        TODO("APR: use JVM equivalent - select the privacy tab in pref core tab container")
    }

    fun selectChatPanel() {
        TODO("APR: use JVM equivalent - select the chat tab in pref core tab container")
    }

    fun getControlNames(names: MutableList<String>) {
        TODO("APR: use JVM equivalent - walk display panel and advanced graphics floater for control names")
    }

    fun updateClickActionViews() {
        TODO("APR: use JVM equivalent - sync click/double-click action UI to saved settings")
    }

    fun updateSearchableItems() {
        collectSearchableItems()
    }

    fun onBtnOK(userdata: Map<String, Any?>) {
        TODO("APR: use JVM equivalent - commit focus, saveSettings, apply, close or hide advanced panel")
    }

    fun onBtnCancel(userdata: Map<String, Any?>) {
        TODO("APR: use JVM equivalent - commit focus, cancel, close or hide advanced panel")
    }

    protected fun onClickBrowserClearCache() {
        TODO("APR: use JVM equivalent - show ConfirmClearBrowserCache notification")
    }

    protected fun onLanguageChange() {
        if (!languageChanged) {
            TODO("APR: use JVM equivalent - notify user that language change requires restart")
            languageChanged = true
        }
    }

    protected fun onTimeFormatChange() {
        TODO("APR: use JVM equivalent - set Use24HourClock from combo value, call onLanguageChange")
    }

    protected fun onNotificationsChange(optionName: String) {
        TODO("APR: use JVM equivalent - update mNotificationOptions, toggle notifications_alert visibility")
    }

    protected fun onNameTagOpacityChange(newValue: Any) {
        TODO("GPU: update background color swatch alpha from newValue")
    }

    protected fun onConsoleOpacityChange(newValue: Any) {
        TODO("GPU: update console_background color swatch alpha from newValue")
    }

    protected fun onPieColorsOverrideChanged() {
        TODO("GPU: enable/disable pie color swatch and slider controls based on OverridePieColors setting")
    }

    protected fun onDoNotDisturbResponseChanged() {
        TODO("APR: use JVM equivalent - compare DND response against localized default, set changed flags")
    }

    protected fun onChangeCustom() = TODO("APR: use JVM equivalent")

    protected fun updateMeterText(ctrl: Any) = TODO("APR: use JVM equivalent")

    protected fun setHardwareDefaults() = setRecommendedSettings()

    fun setRecommendedSettings() {
        resetAutotuneSettings()
        TODO("GPU: gFeatureManager.applyRecommendedSettings(); refreshEnabledGraphics; clear PresetGraphicActive")
    }

    protected fun onRenderOptionEnable() = refreshEnabledGraphics()

    protected fun onAvatarImpostorsEnable() = refreshEnabledGraphics()

    protected fun onLocalLightsEnable() {
        TODO("GPU: enable/disable LocalLightsDetail based on RenderLocalLights setting")
    }

    protected fun onClickActionChange() {
        TODO("APR: use JVM equivalent - update click action keybindings from UI")
    }

    protected fun updateClickActionControls() {
        TODO("APR: use JVM equivalent - update click action keybinding display controls")
    }

    protected fun onAtmosShaderChange() {
        TODO("GPU: handle WindLightUseAtmosShaders setting change")
    }

    protected fun updateUISoundsControls() {
        TODO("APR: use JVM equivalent - sync UI sounds panel controls")
    }

    protected fun populateFontSelectionCombo() {
        TODO("APR: use JVM equivalent - populate font selection combo from skin directories")
    }

    protected fun loadFontPresetsFromDir(dir: String, fontSelectionCombo: Any) {
        TODO("APR: use JVM equivalent - scan dir for font preset XML files and populate combo")
    }

    protected fun onAvatarTagSettingsChanged() {
        TODO("APR: use JVM equivalent - enable/disable avatar tag display sub-options")
    }

    protected fun updateAnimatedScriptDialogs() {
        TODO("APR: use JVM equivalent - enable/disable AnimatedScriptDialogs based on ScriptDialogsPosition")
    }

    protected fun onShowGroupNoticesTopRightChanged() {
        TODO("APR: use JVM equivalent - sync ShowGroupNoticesTopRight radio group from bool setting")
    }

    fun saveSettings() {
        TODO("APR: use JVM equivalent - call saveSettings on all LLPanelPreference children and saveIgnoredNotifications")
    }

    fun saveIgnoredNotifications() {
        TODO("APR: use JVM equivalent - snapshot ignorable notification ignore state")
    }

    fun restoreIgnoredNotifications() {
        TODO("APR: use JVM equivalent - restore snapshot of ignorable notification ignore state")
    }

    fun setCacheLocation(location: String) {
        TODO("APR: use JVM equivalent - update cache_location UI text widget")
    }

    fun setSoundCacheLocation(location: String) {
        TODO("APR: use JVM equivalent - update FSSoundCacheLocation UI text widget")
    }

    fun onClickSetSoundCache() {
        TODO("APR: use JVM equivalent - show dir picker for FSSoundCacheLocation")
    }

    fun changeSoundCachePath(filenames: List<String>, proposedName: String) {
        TODO("APR: use JVM equivalent - update FSSoundCacheLocation setting and UI if changed")
    }

    fun onClickBrowseSoundCache() {
        TODO("APR: use JVM equivalent - open LL_PATH_FS_SOUND_CACHE in file manager")
    }

    fun onClickResetSoundCache() {
        TODO("APR: use JVM equivalent - clear FSSoundCacheLocation setting and show moved notification")
    }

    fun onClickResetVoice() {
        TODO("APR: use JVM equivalent - toggle EnableVoiceChat off then schedule re-enable after 5s")
    }

    fun onClickSetCache() {
        TODO("APR: use JVM equivalent - show dir picker for CacheLocation")
    }

    fun changeCachePath(filenames: List<String>, proposedName: String) {
        TODO("APR: use JVM equivalent - update NewCacheLocation/CacheLocation settings if dir changed")
    }

    fun onClickBrowseCache() {
        TODO("APR: use JVM equivalent - open LL_PATH_CACHE in file manager")
    }

    fun onClickBrowseCrashLogs() {
        TODO("APR: use JVM equivalent - open LL_PATH_LOGS in file manager")
    }

    fun onClickBrowseChatLogDir() {
        TODO("APR: use JVM equivalent - open LL_PATH_CHAT_LOGS in file manager")
    }

    fun onClickResetCache() {
        TODO("APR: use JVM equivalent - reset CacheLocation to default if not already default")
    }

    fun onClickJavascript() {
        TODO("APR: use JVM equivalent - warn if BrowserJavascriptEnabled turned off")
    }

    fun onClickBrowseSettingsDir() {
        TODO("APR: use JVM equivalent - open LL_PATH_USER_SETTINGS in file manager")
    }

    fun onClickSkin(ctrl: Any, userdata: Map<String, Any?>) {
        TODO("APR: use JVM equivalent - set SkinCurrent from userdata")
    }

    fun onSelectSkin() {
        TODO("APR: use JVM equivalent - set SkinCurrent from skin_selection radio group")
    }

    fun onClickPreviewUISound(uiSoundId: Map<String, Any?>) {
        TODO("APR: use JVM equivalent - play preview of UI sound identified by uiSoundId")
    }

    fun setPreprocInclude() {
        TODO("APR: use JVM equivalent - show dir picker for _NACL_PreProcHDDIncludeLocation")
    }

    fun changePreprocIncludePath(filenames: List<String>, proposedName: String) {
        TODO("APR: use JVM equivalent - update _NACL_PreProcHDDIncludeLocation if changed")
    }

    fun setExternalEditor() {
        TODO("APR: use JVM equivalent - show file picker for ExternalEditor executable")
    }

    fun changeExternalEditorPath(filenames: List<String>) {
        TODO("APR: use JVM equivalent - resolve macOS app bundle path if needed, save ExternalEditor setting")
    }

    fun onSelectPopup() {
        TODO("APR: use JVM equivalent - update ignores setting group from selected popup list items")
    }

    fun onUpdatePopupFilter() {
        TODO("APR: use JVM equivalent - apply mPopupFilter text to mPopupList filter string")
    }

    fun resetAllIgnored() {
        TODO("APR: use JVM equivalent - set all ignorable notifications to ignored")
    }

    fun setAllIgnored() {
        TODO("APR: use JVM equivalent - set all ignorable notifications to not-ignored")
    }

    fun onClickLogPath() {
        TODO("APR: use JVM equivalent - show dir picker for InstantMessageLogPath")
    }

    fun changeLogPath(filenames: List<String>, proposedName: String) {
        TODO("APR: use JVM equivalent - update InstantMessageLogPath if changed, store prior path")
    }

    fun moveTranscriptsAndLog(): Boolean {
        TODO("APR: use JVM equivalent - move transcript files to new log path; return success")
    }

    fun onClickResetLogPath() {
        TODO("APR: use JVM equivalent - reset InstantMessageLogPath to default")
    }

    fun setPersonalInfo(visibility: String, imViaEmail: Boolean, email: String) {
        gotPersonalInfo = true
        originalImViaEmail = imViaEmail
        originalHideOnlineStatus = visibility == "hidden"
        directoryVisibility = visibility
        TODO("APR: use JVM equivalent - update UI with personal info visibility and email fields")
    }

    fun refreshEnabledState() {
        TODO("GPU: check feature availability flags and enable/disable individual graphics controls")
    }

    fun disableUnavailableSettings() {
        TODO("GPU: disable shadow, SSAO, tonemap, CAS controls if GPU features unavailable")
    }

    fun onCommitWindowedMode() {
        TODO("GPU: handle windowed/fullscreen mode toggle")
    }

    fun refresh() {
        TODO("GPU: refresh all graphics preference controls from current saved settings")
    }

    fun onChangeQuality(data: Map<String, Any?>) {
        TODO("GPU: apply quality level preset from data, update indirect controls")
    }

    fun onClickClearSettings() {
        TODO("APR: use JVM equivalent - show FirestormClearSettingsPrompt notification, create CLEAR marker file on confirm")
    }

    fun onClickChatOnlineNotices() {
        TODO("APR: use JVM equivalent - toggle OnlineOfflinetoNearbyChatHistory enabled state")
    }

    fun onClickClearSpamList() {
        TODO("APR: use JVM equivalent - purge all NACL anti-spam queues")
    }

    fun onClickInventoryClearCache() {
        TODO("APR: use JVM equivalent - show ConfirmClearInventoryCache notification; create DELETE_INV_GZ marker on confirm")
    }

    fun onClickWebBrowserClearCache() {
        TODO("APR: use JVM equivalent - show ConfirmClearWebBrowserCache; set FSStartupClearBrowserCache on confirm")
    }

    fun updateSliderText(slider: Any, textBox: Any) {
        TODO("GPU: format slider value as text and set on textBox label")
    }

    fun updateMaxNonImpostorsLabel(newValue: Any) {
        TODO("APR: use JVM equivalent - update max non-impostors text label from slider value")
    }

    fun updateMaxComplexityLabel(newValue: Any) {
        TODO("APR: use JVM equivalent - update max complexity text label from slider value")
    }

    fun refreshUI() {
        TODO("GPU: refresh UI slider text labels")
    }

    fun onChangeMaturity() {
        TODO("APR: use JVM equivalent - update maturity icon display from PreferredMaturity setting")
    }

    fun onChangeComplexityMode(newValue: Any) {
        TODO("APR: use JVM equivalent - enable/disable IndirectMaxComplexity slider based on complexity mode")
    }

    fun onChangeModelFolder() = TODO("APR: use JVM equivalent - update model upload folder display")
    fun onChangePBRFolder() = TODO("APR: use JVM equivalent - update PBR upload folder display")
    fun onChangeTextureFolder() = TODO("APR: use JVM equivalent - update texture upload folder display")
    fun onChangeSoundFolder() = TODO("APR: use JVM equivalent - update sound upload folder display")
    fun onChangeAnimationFolder() = TODO("APR: use JVM equivalent - update animation upload folder display")

    fun onClickBlockList() = TODO("APR: use JVM equivalent - show block list floater")
    fun onClickProxySettings() = TODO("APR: use JVM equivalent - show proxy settings floater")
    fun onClickTranslationSettings() = TODO("APR: use JVM equivalent - show translation settings floater")
    fun onClickPermsDefault() = TODO("APR: use JVM equivalent - show default permissions floater")
    fun onClickRememberedUsernames() = TODO("APR: use JVM equivalent - show remembered usernames floater")
    fun onClickAutoReplace() = TODO("APR: use JVM equivalent - show auto-replace settings floater")
    fun onClickSpellChecker() = TODO("APR: use JVM equivalent - show spell checker settings floater")
    fun onClickRenderExceptions() = TODO("APR: use JVM equivalent - show render exceptions floater")
    fun onClickAutoAdjustments() = TODO("APR: use JVM equivalent - show auto adjustments floater")
    fun onClickAdvanced() = TODO("APR: use JVM equivalent - show advanced graphics floater")

    fun applyUIColor(ctrl: Any, param: Map<String, Any?>) {
        TODO("GPU: apply color swatch value to named UI color entry")
    }

    fun getUIColor(ctrl: Any, param: Map<String, Any?>) {
        TODO("GPU: populate color swatch control with current value of named UI color entry")
    }

    fun onLogChatHistorySaved() {
        TODO("APR: use JVM equivalent - update delete transcripts button state after history saved")
    }

    fun buildPopupList() {
        TODO("APR: use JVM equivalent - populate mPopupList from notification templates that have ignore forms")
    }

    fun selectPanel(name: Map<String, Any?>) {
        TODO("APR: use JVM equivalent - select named tab panel in pref core tab container")
    }

    fun saveGraphicsPreset(preset: String) {
        savedGraphicsPreset = preset
    }

    fun onCopySearch() {
        TODO("APR: use JVM equivalent - copy preferences search SLURL to clipboard")
    }

    fun resetAutotuneSettings() {
        TODO("APR: use JVM equivalent - reset AutoTuneFPS and related autotune settings to defaults")
    }

    private fun onDeleteTranscripts() {
        TODO("APR: use JVM equivalent - show delete transcripts confirmation dialog")
    }

    private fun onDeleteTranscriptsResponse(notification: Map<String, Any>, response: Map<String, Any>) {
        TODO("APR: use JVM equivalent - delete transcript files if user confirmed")
    }

    private fun updateDeleteTranscriptsButton() {
        TODO("APR: use JVM equivalent - enable delete_transcripts button only if transcript files exist")
    }

    private fun updateMaxNonImpostors() {
        TODO("APR: use JVM equivalent - compute RenderAvatarMaxNonImpostors from IndirectMaxNonImpostors slider")
    }

    private fun updateIndirectMaxNonImpostors(newValue: Any) {
        TODO("APR: use JVM equivalent - sync IndirectMaxNonImpostors slider from RenderAvatarMaxNonImpostors setting")
    }

    private fun setMaxNonImpostorsText(value: UInt, textBox: Any) {
        TODO("GPU: format value as text ('off' or number) and set on textBox")
    }

    private fun updateMaxComplexity() {
        TODO("GPU: compute RenderAvatarMaxComplexity from IndirectMaxComplexity slider via log scale")
    }

    private fun updateComplexityText() {
        TODO("GPU: update complexity text label from current RenderAvatarMaxComplexity setting")
    }

    private fun onUpdateFilterTerm(force: Boolean = false) {
        TODO("APR: use JVM equivalent - filter preference search items and highlight matches")
    }

    private fun collectSearchableItems() {
        TODO("APR: use JVM equivalent - scan all pref panel children for searchable text items")
    }
}

open class LLPanelPreference : Any() {
    protected val savedValues: MutableMap<Any, Any?> = mutableMapOf()
    private val savedColors: MutableMap<String, LLColor4> = mutableMapOf()
    private var originalMapPickRadiusTransparency: Float = 0f

    open fun postBuild(): Boolean = TODO("APR: use JVM equivalent - wire panel-level callbacks")
    open fun apply() = TODO("APR: use JVM equivalent - apply saved values to control variables")
    open fun cancel(settingsToSkip: List<String> = emptyList()) =
        TODO("APR: use JVM equivalent - restore saved values, skipping any in settingsToSkip")
    open fun setHardwareDefaults() = TODO("APR: use JVM equivalent - reset panel controls to hardware defaults")
    open fun saveSettings() = TODO("APR: use JVM equivalent - snapshot current control values into savedValues")

    fun updateMediaAutoPlayCheckbox(ctrl: Any) =
        TODO("APR: use JVM equivalent - disable AutoPlay checkbox only when both music and media are off")

    fun deletePreset(userData: Map<String, Any?>) = TODO("APR: use JVM equivalent")
    fun savePreset(userData: Map<String, Any?>) = TODO("APR: use JVM equivalent")
    fun loadPreset(userData: Map<String, Any?>) = TODO("APR: use JVM equivalent")

    private fun onEnableGrowlChanged() = TODO("APR: use JVM equivalent")
    private fun onChatWindowChanged() = TODO("APR: use JVM equivalent")
    private fun updateMouselookCombatFeatures() = TODO("APR: use JVM equivalent")
    private fun updateMapPickRadiusTransparency(value: Any) = TODO("APR: use JVM equivalent")
    private fun onCheckContactListColumnMode() = TODO("APR: use JVM equivalent")
}

open class LLPanelPreferenceGraphics : LLPanelPreference() {
    override fun postBuild(): Boolean = TODO("APR: use JVM equivalent")
    fun draw() = TODO("GPU: draw graphics preference panel, update preset text if dirty children")
    override fun cancel(settingsToSkip: List<String>) = TODO("APR: use JVM equivalent")
    override fun saveSettings() = TODO("APR: use JVM equivalent")
    fun resetDirtyChilds() = TODO("APR: use JVM equivalent")
    override fun setHardwareDefaults() = TODO("APR: use JVM equivalent")
    fun setPresetText() = TODO("APR: use JVM equivalent - update preset name label from PresetGraphicActive setting")
    protected fun hasDirtyChilds(): Boolean = TODO("APR: use JVM equivalent")
    private fun onPresetsListChange() = TODO("APR: use JVM equivalent")
}

class LLPanelPreferenceControls : LLPanelPreference() {
    private var pControlsTable: Any? = null
    private var pKeyModeBox: Any? = null
    private var editingControl: String = ""
    private var editingColumn: Int = 0
    private var editingMode: Int = 0

    override fun postBuild(): Boolean = TODO("APR: use JVM equivalent - build keybinding controls table")
    override fun apply() = TODO("APR: use JVM equivalent - commit keybinding changes to conflict handler")
    override fun cancel(settingsToSkip: List<String>) = TODO("APR: use JVM equivalent - discard keybinding edits")
    override fun saveSettings() = TODO("APR: use JVM equivalent")
    fun resetDirtyChilds() = TODO("APR: use JVM equivalent")

    fun onListCommit() = TODO("APR: use JVM equivalent - handle keybinding table row selection")
    fun onModeCommit() = TODO("APR: use JVM equivalent - switch editing mode and repopulate table")
    fun onRestoreDefaultsBtn() = TODO("APR: use JVM equivalent - show confirm dialog for restore defaults")
    fun onRestoreDefaultsResponse(notification: Map<String, Any>, response: Map<String, Any>) =
        TODO("APR: use JVM equivalent - restore defaults on confirm")

    fun canKeyBindHandle(control: String, click: Any, key: Any, mask: Any): Boolean =
        TODO("APR: use JVM equivalent")
    fun setKeyBind(control: String, click: Any, key: Any, mask: Any, set: Boolean) =
        TODO("APR: use JVM equivalent")
    fun updateAndApply() = TODO("APR: use JVM equivalent")

    fun onSetKeyBind(click: Any, key: Any, mask: Any, allModes: Boolean): Boolean =
        TODO("APR: use JVM equivalent")
    fun onDefaultKeyBind(allModes: Boolean) = TODO("APR: use JVM equivalent")
    fun onCancelKeyBind() = TODO("APR: use JVM equivalent")

    private fun regenerateControls() = TODO("APR: use JVM equivalent")
    private fun addControlTableColumns(filename: String): Boolean = TODO("APR: use JVM equivalent")
    private fun addControlTableRows(filename: String): Boolean = TODO("APR: use JVM equivalent")
    private fun addControlTableSeparator() = TODO("APR: use JVM equivalent")
    private fun populateControlTable() = TODO("APR: use JVM equivalent")
    private fun updateTable() = TODO("APR: use JVM equivalent")
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

    fun updateMax(slider: Any, valueLabel: Any, shortVal: Boolean = false) =
        TODO("GPU: read slider, compute RenderAvatarMaxComplexity via log scale, set text label")

    fun setText(value: UInt, textBox: Any, shortVal: Boolean = false) =
        TODO("GPU: format value as 'unlimited' or number and set on textBox")

    fun updateMaxRenderTime(slider: Any, valueLabel: Any, shortVal: Boolean = false) =
        TODO("GPU: read slider, set RenderAvatarMaxART, set text label")

    fun setRenderTimeText(value: Float, textBox: Any, shortVal: Boolean = false) =
        TODO("GPU: format render time value and set on textBox")

    fun setIndirectControls() {
        setIndirectMaxNonImpostors()
        setIndirectMaxArc()
    }

    fun setIndirectMaxNonImpostors() {
        TODO("APR: use JVM equivalent - map RenderAvatarMaxNonImpostors to IndirectMaxNonImpostors slider range")
    }

    fun setIndirectMaxArc() {
        TODO("APR: use JVM equivalent - map RenderAvatarMaxComplexity to IndirectMaxComplexity slider via inverse log scale")
    }
}

class LLPanelPreferenceSkins : LLPanelPreference() {
    private var skin: String = ""
    private var skinTheme: String = ""
    private var skinName: String = ""
    private var skinThemeName: String = ""
    private var skinsInfo: Map<String, Any?> = emptyMap()

    override fun postBuild(): Boolean = TODO("APR: use JVM equivalent - populate skin and theme combos")
    override fun apply() = TODO("APR: use JVM equivalent - apply skin/theme change and show restart notification if needed")
    override fun cancel(settingsToSkip: List<String>) = TODO("APR: use JVM equivalent - revert skin/theme selection")
    fun callbackRestart(notification: Map<String, Any>, response: Map<String, Any>) =
        TODO("APR: use JVM equivalent - close viewer on restart confirmation")

    protected fun onSkinChanged() = TODO("APR: use JVM equivalent")
    protected fun onSkinThemeChanged() = TODO("APR: use JVM equivalent")
    protected fun refreshSkinList() = TODO("APR: use JVM equivalent")
    protected fun refreshSkinThemeList() = TODO("APR: use JVM equivalent")
    protected fun refreshPreviewImage() = TODO("APR: use JVM equivalent")
    protected fun showSkinChangeNotification() = TODO("APR: use JVM equivalent")
}

class LLPanelPreferenceCrashReports : LLPanelPreference() {
    override fun postBuild(): Boolean = TODO("APR: use JVM equivalent")
    override fun apply() = TODO("APR: use JVM equivalent")
    override fun cancel(settingsToSkip: List<String>) = TODO("APR: use JVM equivalent")
    fun refresh() = TODO("APR: use JVM equivalent")
}

class FSPanelPreferenceBackup : LLPanelPreference() {
    override fun postBuild(): Boolean = TODO("APR: use JVM equivalent - wire backup/restore buttons")

    protected fun onClickSetBackupSettingsPath() = TODO("APR: use JVM equivalent - show dir picker")
    protected fun changeBackupSettingsPath(filenames: List<String>, proposedName: String) =
        TODO("APR: use JVM equivalent")
    protected fun onClickSelectAll() = doSelect(true)
    protected fun onClickDeselectAll() = doSelect(false)
    protected fun onClickBackupSettings() =
        TODO("APR: use JVM equivalent - show backup confirmation dialog")
    protected fun onClickRestoreSettings() =
        TODO("APR: use JVM equivalent - show restore confirmation dialog")
    protected fun doSelect(all: Boolean) =
        TODO("APR: use JVM equivalent - select or deselect all items in all backup scroll lists")
    protected fun applySelection(control: Any, all: Boolean) =
        TODO("APR: use JVM equivalent - select or deselect all items in given scroll list")
    protected fun doBackupSettings(notification: Map<String, Any>, response: Map<String, Any>) =
        TODO("APR: use JVM equivalent - execute settings backup on confirmation")
    protected fun doRestoreSettings(notification: Map<String, Any>, response: Map<String, Any>) =
        TODO("APR: use JVM equivalent - execute settings restore on confirmation")
    protected fun onQuitConfirmed(notification: Map<String, Any>, response: Map<String, Any>) =
        TODO("APR: use JVM equivalent - quit viewer after restore completes")
}

class LLPanelPreferenceOpensim : LLPanelPreference() {
    private var gridListChangedCallbackConnection: (() -> Unit)? = null
    private var gridAddedCallbackConnection: (() -> Unit)? = null
    private var currentGrid: String = ""

    override fun postBuild(): Boolean = TODO("APR: use JVM equivalent - populate OpenSim grid list")
    override fun apply() = TODO("APR: use JVM equivalent - apply OpenSim grid settings")
    override fun cancel(settingsToSkip: List<String>) = TODO("APR: use JVM equivalent")

    protected fun onOpen(key: Map<String, Any?>) = TODO("APR: use JVM equivalent - refresh grid list on open")
    protected fun onClickAddGrid() = TODO("APR: use JVM equivalent")
    protected fun addedGrid(success: Boolean) = TODO("APR: use JVM equivalent")
    protected fun onClickClearGrid() = TODO("APR: use JVM equivalent")
    protected fun onClickRefreshGrid() = TODO("APR: use JVM equivalent")
    protected fun onClickRemoveGrid() = TODO("APR: use JVM equivalent")
    protected fun onSelectGrid() = TODO("APR: use JVM equivalent")
    protected fun removeGridCB(notification: Map<String, Any>, response: Map<String, Any>): Boolean =
        TODO("APR: use JVM equivalent")
    protected fun onClickClearDebugSearchURL() = TODO("APR: use JVM equivalent")
    protected fun onClickPickDebugSearchURL() = TODO("APR: use JVM equivalent")
    protected fun refreshGridList(success: Boolean = true) = TODO("APR: use JVM equivalent")
}

class FSPanelPreferenceSounds : LLPanelPreference() {
    private var outputDeviceListChangedConnection: (() -> Unit)? = null

    fun postBuild(): Boolean = TODO("APR: use JVM equivalent - wire output device combo and MOAP checkboxes")

    private fun onOutputDeviceChanged(newValue: Any) =
        TODO("APR: use JVM equivalent - apply selected output device")
    private fun onOutputDeviceSelectionChanged(newValue: Any) =
        TODO("APR: use JVM equivalent - update output device selection")
    private fun onOutputDeviceListChanged(outputDevices: Map<String, Any>) =
        TODO("APR: use JVM equivalent - repopulate output device combo with new device list")
    private fun onMoapInteractionChanged() = updateMoapInteractionSetting()
    private fun updateMoapInteractionSetting() =
        TODO("APR: use JVM equivalent - compute combined MOAP interaction flags from checkboxes")
}

class LLFloaterPreferenceProxy(key: Map<String, Any?>) : LLFloater(key) {
    companion object {
        fun show() = TODO("APR: use JVM equivalent - show proxy settings floater singleton")
    }

    private var socksSettingsDirty: Boolean = false
    private val savedValues: MutableMap<Any, Any?> = mutableMapOf()

    fun cancel() = TODO("APR: use JVM equivalent - restore saved proxy settings")

    protected fun postBuild(): Boolean = TODO("APR: use JVM equivalent - wire proxy control callbacks")
    protected fun onOpen(key: Map<String, Any?>) = TODO("APR: use JVM equivalent")
    protected fun onClose(appQuitting: Boolean) = TODO("APR: use JVM equivalent")
    protected fun saveSettings() = TODO("APR: use JVM equivalent - snapshot proxy control values")
    protected fun onBtnOk() = TODO("APR: use JVM equivalent - apply and close")
    protected fun onBtnCancel() { cancel(); TODO("APR: use JVM equivalent - close floater") }
    protected fun onClickCloseBtn(appQuitting: Boolean = false) = TODO("APR: use JVM equivalent")
    protected fun onChangeSocksSettings() {
        socksSettingsDirty = true
    }
}
