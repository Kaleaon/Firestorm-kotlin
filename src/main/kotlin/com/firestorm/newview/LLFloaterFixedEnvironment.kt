package com.firestorm.newview

import java.util.UUID

private const val FIELD_SETTINGS_NAME = "settings_name"
private const val CONTROL_TAB_AREA = "tab_settings"
private const val BUTTON_NAME_IMPORT = "btn_import"
private const val BUTTON_NAME_COMMIT = "btn_commit"
private const val BUTTON_NAME_CANCEL = "btn_cancel"
private const val BUTTON_NAME_FLYOUT = "btn_flyout"
private const val BUTTON_NAME_LOAD = "btn_load"
private const val ACTION_SAVE = "save_settings"
private const val ACTION_SAVEAS = "save_as_new_settings"
private const val ACTION_COMMIT = "commit_changes"
private const val ACTION_APPLY_LOCAL = "apply_local"
private const val ACTION_APPLY_PARCEL = "apply_parcel"
private const val ACTION_APPLY_REGION = "apply_region"
private const val XML_FLYOUTMENU_FILE = "menu_save_settings.xml"

open class LLFloaterFixedEnvironment(key: LLSDMap) : LLFloaterEditEnvironmentBase(key) {

    protected var mTab: LLTabContainer? = null
    protected var mTxtName: LLLineEditor? = null
    protected var mSettings: LLSettingsBase? = null
    private var mFlyoutControl: LLFlyoutComboBtnCtrl? = null

    open override fun postBuild(): Boolean {
        mTab = getChild<LLTabContainer>(CONTROL_TAB_AREA)
        mTxtName = getChild<LLLineEditor>(FIELD_SETTINGS_NAME)

        mTxtName?.setCommitOnFocusLost(true)
        mTxtName?.setCommitCallback { _, _ -> onNameChanged(mTxtName?.getValue()?.asString() ?: "") }

        getChild<LLButton>(BUTTON_NAME_IMPORT).setClickedCallback { _, _ -> onButtonImport() }
        getChild<LLButton>(BUTTON_NAME_CANCEL).setClickedCallback { _, _ -> onClickCloseBtn() }
        getChild<LLButton>(BUTTON_NAME_LOAD).setClickedCallback { _, _ -> onButtonLoad() }

        mFlyoutControl = LLFlyoutComboBtnCtrl(this, BUTTON_NAME_COMMIT, BUTTON_NAME_FLYOUT, XML_FLYOUTMENU_FILE, false)
        mFlyoutControl?.setAction { ctrl, data -> onButtonApply(ctrl, data) }
        mFlyoutControl?.setMenuItemVisible(ACTION_COMMIT, false)

        return true
    }

    open override fun onOpen(key: LLSDMap) {
        val invid: UUID = if (key.has(KEY_INVENTORY_ID)) key[KEY_INVENTORY_ID].asUUID() else UUID.randomUUID().let { UUID.fromString("00000000-0000-0000-0000-000000000000") }

        loadInventoryItem(invid)
        updateEditEnvironment()
        syncronizeTabs()
        refresh()
        LLEnvironment.instance().setSelectedEnvironment(LLEnvironment.ENV_EDIT, LLEnvironment.TRANSITION_INSTANT)
    }

    open override fun onClose(appQuitting: Boolean) {
        doCloseInventoryFloater(appQuitting)

        if (!appQuitting) {
            LLEnvironment.instance().setSelectedEnvironment(LLEnvironment.ENV_LOCAL)
            LLEnvironment.instance().setCurrentEnvironmentSelection(LLEnvironment.ENV_LOCAL)
            LLEnvironment.instance().clearEnvironment(LLEnvironment.ENV_EDIT)
            mSettings = null
            syncronizeTabs()
        }
    }

    fun setEditSettings(settings: LLSettingsBase) {
        mSettings = settings
        clearDirtyFlag()
        syncronizeTabs()
        refresh()
    }

    open fun getEditSettings(): LLSettingsBase? = mSettings

    open override fun refresh() {
        val settings = mSettings ?: return

        val isInventoryAvail = canUseInventory()
        mFlyoutControl?.setMenuItemEnabled(ACTION_SAVE, isInventoryAvail && mCanMod && mInventoryId != NULL_UUID)
        mFlyoutControl?.setMenuItemEnabled(ACTION_SAVEAS, isInventoryAvail && mCanCopy)
        mFlyoutControl?.setMenuItemEnabled(ACTION_APPLY_PARCEL, canApplyParcel())
        mFlyoutControl?.setMenuItemEnabled(ACTION_APPLY_REGION, canApplyRegion())

        mTxtName?.setValue(settings.getName())
        mTxtName?.setEnabled(mCanMod)

        val count = mTab?.getTabCount() ?: 0
        for (idx in 0 until count) {
            val panel = mTab?.getPanelByIndex(idx) as? LLSettingsEditPanel
            panel?.setCanChangeSettings(mCanMod)
            panel?.refresh()
        }
    }

    override fun setEditSettingsAndUpdate(settings: LLSettingsBase) {
        mSettings = settings
        updateEditEnvironment()
        syncronizeTabs()
        refresh()
        LLEnvironment.instance().updateEnvironment(LLEnvironment.TRANSITION_INSTANT)

        val shouldAutoAdjust = gSavedSettings.getBoolean("RenderSkyAutoAdjustLegacy") == true
        val sky = mSettings as? LLSettingsSky
        if (shouldAutoAdjust && sky != null && sky.canAutoAdjust() && sky.getReflectionProbeAmbiance(true) != 0f) {
            LLNotificationsUtil.add("AutoAdjustHDRSky")
        }
    }

    open fun syncronizeTabs() {
        val count = mTab?.getTabCount() ?: 0
        for (idx in 0 until count) {
            val panel = mTab?.getPanelByIndex(idx) as? LLSettingsEditPanel
            panel?.setSettings(mSettings)
        }
    }

    open fun getSettingsPicker(): LLFloaterSettingsPicker {
        var picker = mInventoryFloater?.get() as? LLFloaterSettingsPicker
        if (picker == null) {
            picker = LLFloaterSettingsPicker(this, NULL_UUID)
            mInventoryFloater = picker.getHandle()
            picker.setCommitCallback { _, data -> onPickerCommitSetting(data["ItemId"].asUUID()) }
        }
        return picker
    }

    protected fun onInventoryCreated(assetId: UUID, inventoryId: UUID, results: LLSDMap) {
        if (inventoryId == NULL_UUID || !results["success"].asBoolean()) {
            LLNotificationsUtil.add("CantCreateInventory")
            return
        }
        onInventoryCreated(assetId, inventoryId)
    }

    protected fun onInventoryCreated(assetId: UUID, inventoryId: UUID) {
        var canTrans = true
        val invItem = mInventoryItem
        if (invItem != null) {
            val perms = invItem.getPermissions()
            val createdItem = gInventory.getItem(mInventoryId)
            if (createdItem != null) {
                canTrans = perms.allowOperationBy(PERM_TRANSFER, gAgent.getID())
                createdItem.setPermissions(perms)
                createdItem.updateServer(false)
            }
        }
        clearDirtyFlag()
        setFocus(true)
        loadInventoryItem(inventoryId, canTrans)
    }

    protected fun onInventoryUpdated(assetId: UUID, inventoryId: UUID, results: LLSDMap) {
        clearDirtyFlag()
        if (inventoryId != mInventoryId) {
            loadInventoryItem(inventoryId)
        }
    }

    open override fun clearDirtyFlag() {
        mIsDirty = false
        val count = mTab?.getTabCount() ?: 0
        for (idx in 0 until count) {
            val panel = mTab?.getPanelByIndex(idx) as? LLSettingsEditPanel
            panel?.clearIsDirty()
        }
    }

    protected fun updatePermissionFlags() {
        System.err.println("LLFloaterFixedEnvironment: updatePermissionFlags not yet implemented")
    }

    protected fun doSelectFromInventory() {
        val picker = getSettingsPicker()
        picker.setSettingsFilter(mSettings?.getSettingsTypeValue() ?: 0)
        picker.openFloater()
        picker.setFocus(true)
    }

    open override fun onClickCloseBtn(appQuitting: Boolean) {
        if (!appQuitting) {
            checkAndConfirmSettingsLoss { closeFloater(); clearDirtyFlag() }
        } else {
            closeFloater()
        }
    }

    private fun onPickerCommitSetting(itemId: UUID) {
        loadInventoryItem(itemId)
    }

    private fun onNameChanged(name: String) {
        mSettings?.setName(name)
        setDirtyFlag()
    }

    private fun onButtonImport() {
        checkAndConfirmSettingsLoss { doImportFromDisk() }
    }

    private fun onButtonApply(ctrl: LLUICtrl, data: LLSDMap) {
        val ctrlAction = ctrl.getName()

        var localDesc = ""
        var settingClone: LLSettingsBase? = null
        var isLocal = false

        when (mSettings?.getSettingsType()) {
            "water" -> {
                val water = mSettings as? LLSettingsWater
                if (water != null) {
                    settingClone = water.buildClone()
                    when {
                        LLLocalBitmapMgr.getInstance().isLocal(water.getNormalMapID()) -> {
                            localDesc = LLTrans.getString("EnvironmentNormalMap"); isLocal = true
                        }
                        LLLocalBitmapMgr.getInstance().isLocal(water.getTransparentTextureID()) -> {
                            localDesc = LLTrans.getString("EnvironmentTransparent"); isLocal = true
                        }
                    }
                }
            }
            "sky" -> {
                val sky = mSettings as? LLSettingsSky
                if (sky != null) {
                    settingClone = sky.buildClone()
                    when {
                        LLLocalBitmapMgr.getInstance().isLocal(sky.getSunTextureId()) -> {
                            localDesc = LLTrans.getString("EnvironmentSun"); isLocal = true
                        }
                        LLLocalBitmapMgr.getInstance().isLocal(sky.getMoonTextureId()) -> {
                            localDesc = LLTrans.getString("EnvironmentMoon"); isLocal = true
                        }
                        LLLocalBitmapMgr.getInstance().isLocal(sky.getCloudNoiseTextureId()) -> {
                            localDesc = LLTrans.getString("EnvironmentCloudNoise"); isLocal = true
                        }
                        LLLocalBitmapMgr.getInstance().isLocal(sky.getBloomTextureId()) -> {
                            localDesc = LLTrans.getString("EnvironmentBloom"); isLocal = true
                        }
                    }
                }
            }
        }

        if (isLocal) {
            val args = LLSDMap()
            args["FIELD"] = localDesc
            LLNotificationsUtil.add("WLLocalTextureFixedBlock", args)
            return
        }

        when (ctrlAction) {
            ACTION_SAVE -> {
                doApplyUpdateInventory(settingClone)
                clearDirtyFlag()
            }
            ACTION_SAVEAS -> {
                val args = LLSDMap()
                args["DESC"] = mSettings?.getName() ?: ""
                LLNotificationsUtil.add("SaveSettingAs", args, LLSDMap()) { _, _ ->
                    onSaveAsCommit(settingClone)
                }
            }
            ACTION_APPLY_LOCAL, ACTION_APPLY_PARCEL, ACTION_APPLY_REGION -> {
                doApplyEnvironment(ctrlAction, settingClone)
            }
        }
    }

    private fun onButtonLoad() {
        checkAndConfirmSettingsLoss { doSelectFromInventory() }
    }
}

class LLFloaterFixedEnvironmentWater(key: LLSDMap) : LLFloaterFixedEnvironment(key) {

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false

        val panel = LLPanelSettingsWaterMainTab()
        panel.buildFromFile("panel_fs_settings_water.xml")
        panel.setWater(mSettings as? LLSettingsWater)
        panel.setOnDirtyFlagChanged { _, value -> onPanelDirtyFlagChanged(value) }
        mTab?.addTabPanel(LLTabContainer.TabPanelParams(panel = panel, selectTab = true))

        return true
    }

    override fun updateEditEnvironment() {
        LLEnvironment.instance().setEnvironment(LLEnvironment.ENV_EDIT, mSettings as? LLSettingsWater)
    }

    override fun onOpen(key: LLSDMap) {
        if (mSettings == null) {
            mSettings = LLEnvironment.instance().getEnvironmentFixedWater(LLEnvironment.ENV_CURRENT)?.buildClone()
            mSettings?.setName("Snapshot water (new)")
        }
        super.onOpen(key)
    }

    override fun doImportFromDisk() {
        System.err.println("LLFloaterFixedEnvironmentWater: doImportFromDisk not yet implemented")
    }

    fun loadWaterSettingFromFile(filenames: MutableList<String>) {
        if (filenames.isEmpty()) return
        val filename = filenames[0]
        val messages = LLSDMap()
        val legacyWater = LLEnvironment.createWaterFromLegacyPreset(filename, messages)
        if (legacyWater == null) {
            LLNotificationsUtil.add("WLImportFail", messages)
            return
        }
        loadInventoryItem(NULL_UUID)
        setDirtyFlag()
        LLEnvironment.instance().setEnvironment(LLEnvironment.ENV_EDIT, legacyWater)
        setEditSettings(legacyWater)
        LLEnvironment.instance().updateEnvironment(LLEnvironment.TRANSITION_INSTANT, true)
    }
}

class LLFloaterFixedEnvironmentSky(key: LLSDMap) : LLFloaterFixedEnvironment(key) {

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false

        var panel: LLPanelSettingsSky = LLPanelSettingsSkyAtmosTab()
        panel.buildFromFile("panel_fs_settings_sky_atmos.xml")
        panel.setSky(mSettings as? LLSettingsSky)
        panel.setOnDirtyFlagChanged { _, value -> onPanelDirtyFlagChanged(value) }
        mTab?.addTabPanel(LLTabContainer.TabPanelParams(panel = panel, selectTab = true))

        panel = LLPanelSettingsSkyCloudTab()
        panel.buildFromFile("panel_fs_settings_sky_clouds.xml")
        panel.setSky(mSettings as? LLSettingsSky)
        panel.setOnDirtyFlagChanged { _, value -> onPanelDirtyFlagChanged(value) }
        mTab?.addTabPanel(LLTabContainer.TabPanelParams(panel = panel, selectTab = false))

        panel = LLPanelSettingsSkySunMoonTab()
        panel.buildFromFile("panel_fs_settings_sky_sunmoon.xml")
        panel.setSky(mSettings as? LLSettingsSky)
        panel.setOnDirtyFlagChanged { _, value -> onPanelDirtyFlagChanged(value) }
        mTab?.addTabPanel(LLTabContainer.TabPanelParams(panel = panel, selectTab = false))

        return true
    }

    override fun updateEditEnvironment() {
        LLEnvironment.instance().setEnvironment(LLEnvironment.ENV_EDIT, mSettings as? LLSettingsSky)
    }

    override fun onOpen(key: LLSDMap) {
        if (mSettings == null) {
            mSettings = LLEnvironment.instance().getEnvironmentFixedSky(LLEnvironment.ENV_CURRENT)?.buildClone()
            mSettings?.setName("Snapshot sky (new)")
            LLEnvironment.instance().saveBeaconsState()
        }
        super.onOpen(key)
    }

    override fun onClose(appQuitting: Boolean) {
        LLEnvironment.instance().revertBeaconsState()
        super.onClose(appQuitting)
    }

    override fun doImportFromDisk() {
        System.err.println("LLFloaterFixedEnvironmentSky: doImportFromDisk not yet implemented")
    }

    fun loadSkySettingFromFile(filenames: MutableList<String>) {
        if (filenames.isEmpty()) return
        val filename = filenames[0]
        val messages = LLSDMap()
        val legacySky = LLEnvironment.createSkyFromLegacyPreset(filename, messages)
        if (legacySky == null) {
            LLNotificationsUtil.add("WLImportFail", messages)
            return
        }
        loadInventoryItem(NULL_UUID)
        setDirtyFlag()
        LLEnvironment.instance().setEnvironment(LLEnvironment.ENV_EDIT, legacySky)
        setEditSettings(legacySky)
        LLEnvironment.instance().updateEnvironment(LLEnvironment.TRANSITION_INSTANT, true)
    }
}
