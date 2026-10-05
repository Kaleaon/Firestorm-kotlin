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

open class FloaterFixedEnvironment(key: LLSD) : FloaterEditEnvironmentBase(key) {

    protected var tab: TabContainer? = null
    protected var txtName: LineEditor? = null
    protected var settings: SettingsBase? = null
    private var flyoutControl: FlyoutComboBtnCtrl? = null

    override fun postBuild(): Boolean {
        tab = getChild<TabContainer>(CONTROL_TAB_AREA)
        txtName = getChild<LineEditor>(FIELD_SETTINGS_NAME)

        txtName?.apply {
            commitOnFocusLost = true
            setCommitCallback { onNameChanged(getValue().asString()) }
        }

        getChild<Button>(BUTTON_NAME_IMPORT)?.setClickedCallback { onButtonImport() }
        getChild<Button>(BUTTON_NAME_CANCEL)?.setClickedCallback { onClickCloseBtn() }
        getChild<Button>(BUTTON_NAME_LOAD)?.setClickedCallback { onButtonLoad() }

        flyoutControl = FlyoutComboBtnCtrl(this, BUTTON_NAME_COMMIT, BUTTON_NAME_FLYOUT, XML_FLYOUTMENU_FILE, false)
        flyoutControl?.setAction { ctrl, data -> onButtonApply(ctrl, data) }
        flyoutControl?.setMenuItemVisible(ACTION_COMMIT, false)

        return true
    }

    override fun onOpen(key: LLSD) {
        val invId = if (key.has(KEY_INVENTORY_ID)) key[KEY_INVENTORY_ID].asUUID() else UUID(0, 0)
        loadInventoryItem(invId)
        updateEditEnvironment()
        synchronizeTabs()
        refresh()
        Environment.instance.setSelectedEnvironment(Environment.ENV_EDIT, Environment.TRANSITION_INSTANT)
    }

    override fun onClose(appQuitting: Boolean) {
        doCloseInventoryFloater(appQuitting)
        if (!appQuitting) {
            Environment.instance.setSelectedEnvironment(Environment.ENV_LOCAL)
            Environment.instance.setCurrentEnvironmentSelection(Environment.ENV_LOCAL)
            Environment.instance.clearEnvironment(Environment.ENV_EDIT)
            settings = null
            synchronizeTabs()
        }
    }

    override fun refresh() {
        val s = settings ?: return
        val isInventoryAvail = canUseInventory()

        flyoutControl?.setMenuItemEnabled(ACTION_SAVE, isInventoryAvail && canMod && inventoryId != UUID(0, 0))
        flyoutControl?.setMenuItemEnabled(ACTION_SAVEAS, isInventoryAvail && canCopy)
        flyoutControl?.setMenuItemEnabled(ACTION_APPLY_PARCEL, canApplyParcel())
        flyoutControl?.setMenuItemEnabled(ACTION_APPLY_REGION, canApplyRegion())

        txtName?.setValue(s.name)
        txtName?.isEnabled = canMod

        val tabContainer = tab ?: return
        for (idx in 0 until tabContainer.tabCount) {
            (tabContainer.getPanelByIndex(idx) as? SettingsEditPanel)?.let { panel ->
                panel.setCanChangeSettings(canMod)
                panel.refresh()
            }
        }
    }

    override fun setEditSettingsAndUpdate(settings: SettingsBase) {
        this.settings = settings
        updateEditEnvironment()
        synchronizeTabs()
        refresh()
        Environment.instance.updateEnvironment(Environment.TRANSITION_INSTANT)

        val shouldAutoAdjust = SavedSettings.getBoolean("RenderSkyAutoAdjustLegacy") ?: false
        val sky = settings as? SettingsSky
        if (sky != null && shouldAutoAdjust && sky.canAutoAdjust() && sky.getReflectionProbeAmbiance(true) != 0f) {
            NotificationsUtil.add("AutoAdjustHDRSky")
        }
    }

    override fun getEditSettings(): SettingsBase? = settings

    fun setEditSettings(s: SettingsBase) {
        settings = s
        clearDirtyFlag()
        synchronizeTabs()
        refresh()
    }

    protected open fun synchronizeTabs() {
        val tabContainer = tab ?: return
        for (idx in 0 until tabContainer.tabCount) {
            (tabContainer.getPanelByIndex(idx) as? SettingsEditPanel)?.setSettings(settings)
        }
    }

    override fun getSettingsPicker(): FloaterSettingsPicker {
        var picker = inventoryFloater?.get() as? FloaterSettingsPicker
        if (picker == null) {
            picker = FloaterSettingsPicker(this, UUID(0, 0))
            inventoryFloater = picker.handle
            picker.setCommitCallback { _, data -> onPickerCommitSetting(data["ItemId"].asUUID()) }
        }
        return picker
    }

    private fun onPickerCommitSetting(itemId: UUID) {
        loadInventoryItem(itemId)
    }

    private fun onNameChanged(name: String) {
        settings?.name = name
        setDirtyFlag()
    }

    private fun onButtonImport() {
        checkAndConfirmSettingsLoss { doImportFromDisk() }
    }

    private fun onButtonApply(ctrl: UiCtrl, data: LLSD) {
        val ctrlAction = ctrl.name
        val s = settings ?: return

        var localDesc = ""
        var isLocal = false
        val settingClone: SettingsBase? = when (s.settingsType) {
            "water" -> {
                val water = s as? SettingsWater
                if (water != null) {
                    when {
                        LocalBitmapMgr.instance.isLocal(water.normalMapId) -> {
                            localDesc = Trans.getString("EnvironmentNormalMap"); isLocal = true
                        }
                        LocalBitmapMgr.instance.isLocal(water.transparentTextureId) -> {
                            localDesc = Trans.getString("EnvironmentTransparent"); isLocal = true
                        }
                    }
                    water.buildClone()
                } else null
            }
            "sky" -> {
                val sky = s as? SettingsSky
                if (sky != null) {
                    when {
                        LocalBitmapMgr.instance.isLocal(sky.sunTextureId) -> {
                            localDesc = Trans.getString("EnvironmentSun"); isLocal = true
                        }
                        LocalBitmapMgr.instance.isLocal(sky.moonTextureId) -> {
                            localDesc = Trans.getString("EnvironmentMoon"); isLocal = true
                        }
                        LocalBitmapMgr.instance.isLocal(sky.cloudNoiseTextureId) -> {
                            localDesc = Trans.getString("EnvironmentCloudNoise"); isLocal = true
                        }
                        LocalBitmapMgr.instance.isLocal(sky.bloomTextureId) -> {
                            localDesc = Trans.getString("EnvironmentBloom"); isLocal = true
                        }
                    }
                    sky.buildClone()
                } else null
            }
            else -> null
        }

        if (isLocal) {
            val args = LLSD().apply { put("FIELD", localDesc) }
            NotificationsUtil.add("WLLocalTextureFixedBlock", args)
            return
        }

        when (ctrlAction) {
            ACTION_SAVE -> {
                doApplyUpdateInventory(settingClone)
                clearDirtyFlag()
            }
            ACTION_SAVEAS -> {
                val args = LLSD().apply { put("DESC", s.name) }
                NotificationsUtil.add("SaveSettingAs", args, LLSD()) { notif, resp ->
                    onSaveAsCommit(notif, resp, settingClone)
                }
            }
            ACTION_APPLY_LOCAL, ACTION_APPLY_PARCEL, ACTION_APPLY_REGION ->
                doApplyEnvironment(ctrlAction, settingClone)
        }
    }

    override fun onClickCloseBtn(appQuitting: Boolean) {
        if (!appQuitting) {
            checkAndConfirmSettingsLoss { closeFloater(); clearDirtyFlag() }
        } else {
            closeFloater()
        }
    }

    private fun onButtonLoad() {
        checkAndConfirmSettingsLoss { doSelectFromInventory() }
    }

    private fun onInventoryCreated(assetId: UUID, inventoryId: UUID, results: LLSD) {
        if (inventoryId == UUID(0, 0) || !results["success"].asBoolean()) {
            NotificationsUtil.add("CantCreateInventory")
            return
        }
        onInventoryCreated(assetId, inventoryId)
    }

    private fun onInventoryCreated(assetId: UUID, newInventoryId: UUID) {
        var canTrans = true
        val item = inventoryItem
        if (item != null) {
            val perms = item.permissions
            val createdItem = Inventory.instance.getItem(newInventoryId)
            if (createdItem != null) {
                canTrans = perms.allowOperationBy(PERM_TRANSFER, Agent.instance.id)
                createdItem.permissions = perms
                createdItem.updateServer(false)
            }
        }
        clearDirtyFlag()
        setFocus(true)
        loadInventoryItem(newInventoryId, canTrans)
    }

    private fun onInventoryUpdated(assetId: UUID, updatedInventoryId: UUID, results: LLSD) {
        clearDirtyFlag()
        if (updatedInventoryId != inventoryId) {
            loadInventoryItem(updatedInventoryId)
        }
    }

    override fun clearDirtyFlag() {
        isDirty = false
        val tabContainer = tab ?: return
        for (idx in 0 until tabContainer.tabCount) {
            (tabContainer.getPanelByIndex(idx) as? SettingsEditPanel)?.clearIsDirty()
        }
    }

    private fun doSelectFromInventory() {
        val picker = getSettingsPicker()
        picker.setSettingsFilter(settings?.settingsTypeValue ?: return)
        picker.openFloater()
        picker.setFocus(true)
    }

    protected fun updatePermissionFlags() {
        System.err.println("FloaterWater: updatePermissionFlags not yet implemented")
    }
}

class FloaterFixedEnvironmentWater(key: LLSD) : FloaterFixedEnvironment(key) {

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        val panel = PanelSettingsWaterMainTab()
        panel.buildFromFile("panel_fs_settings_water.xml")
        panel.setWater(settings as? SettingsWater)
        panel.setOnDirtyFlagChanged { _, value -> onPanelDirtyFlagChanged(value) }
        tab?.addTabPanel(TabPanelParams(panel = panel, selectTab = true))
        return true
    }

    override fun onOpen(key: LLSD) {
        if (settings == null) {
            settings = Environment.instance.getEnvironmentFixedWater(Environment.ENV_CURRENT)
                ?.buildClone()
                ?.also { it.name = "Snapshot water (new)" }
        }
        super.onOpen(key)
    }

    override fun updateEditEnvironment() {
        val water = settings as? SettingsWater ?: return
        Environment.instance.setEnvironment(Environment.ENV_EDIT, water)
    }

    override fun doImportFromDisk() {
        FilePickerReplyThread.startPicker(
            callback = { filenames -> loadWaterSettingFromFile(filenames) },
            filter = FilePicker.FFLOAD_XML,
            multi = false
        )
    }

    private fun loadWaterSettingFromFile(filenames: List<String>) {
        if (filenames.isEmpty()) return
        val filename = filenames[0]
        val messages = LLSD()
        val legacyWater = Environment.createWaterFromLegacyPreset(filename, messages) ?: run {
            NotificationsUtil.add("WLImportFail", messages)
            return
        }
        loadInventoryItem(UUID(0, 0))
        setDirtyFlag()
        Environment.instance.setEnvironment(Environment.ENV_EDIT, legacyWater)
        setEditSettings(legacyWater)
        Environment.instance.updateEnvironment(Environment.TRANSITION_INSTANT, true)
    }
}

class FloaterFixedEnvironmentSky(key: LLSD) : FloaterFixedEnvironment(key) {

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false

        var panel: PanelSettingsSky = PanelSettingsSkyAtmosTab()
        panel.buildFromFile("panel_fs_settings_sky_atmos.xml")
        panel.setSky(settings as? SettingsSky)
        panel.setOnDirtyFlagChanged { _, value -> onPanelDirtyFlagChanged(value) }
        tab?.addTabPanel(TabPanelParams(panel = panel, selectTab = true))

        panel = PanelSettingsSkyCloudTab()
        panel.buildFromFile("panel_fs_settings_sky_clouds.xml")
        panel.setSky(settings as? SettingsSky)
        panel.setOnDirtyFlagChanged { _, value -> onPanelDirtyFlagChanged(value) }
        tab?.addTabPanel(TabPanelParams(panel = panel, selectTab = false))

        panel = PanelSettingsSkySunMoonTab()
        panel.buildFromFile("panel_fs_settings_sky_sunmoon.xml")
        panel.setSky(settings as? SettingsSky)
        panel.setOnDirtyFlagChanged { _, value -> onPanelDirtyFlagChanged(value) }
        tab?.addTabPanel(TabPanelParams(panel = panel, selectTab = false))

        return true
    }

    override fun onOpen(key: LLSD) {
        if (settings == null) {
            settings = Environment.instance.getEnvironmentFixedSky(Environment.ENV_CURRENT)
                ?.buildClone()
                ?.also { it.name = "Snapshot sky (new)" }
            Environment.instance.saveBeaconsState()
        }
        super.onOpen(key)
    }

    override fun onClose(appQuitting: Boolean) {
        Environment.instance.revertBeaconsState()
        super.onClose(appQuitting)
    }

    override fun updateEditEnvironment() {
        val sky = settings as? SettingsSky ?: return
        Environment.instance.setEnvironment(Environment.ENV_EDIT, sky)
    }

    override fun doImportFromDisk() {
        FilePickerReplyThread.startPicker(
            callback = { filenames -> loadSkySettingFromFile(filenames) },
            filter = FilePicker.FFLOAD_XML,
            multi = false
        )
    }

    private fun loadSkySettingFromFile(filenames: List<String>) {
        if (filenames.isEmpty()) return
        val filename = filenames[0]
        val messages = LLSD()
        val legacySky = Environment.createSkyFromLegacyPreset(filename, messages) ?: run {
            NotificationsUtil.add("WLImportFail", messages)
            return
        }
        loadInventoryItem(UUID(0, 0))
        setDirtyFlag()
        Environment.instance.setEnvironment(Environment.ENV_EDIT, legacySky)
        setEditSettings(legacySky)
        Environment.instance.updateEnvironment(Environment.TRANSITION_INSTANT, true)
    }
}
