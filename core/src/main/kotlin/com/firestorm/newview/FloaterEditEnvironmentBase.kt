package com.firestorm.newview

import java.util.UUID

abstract class FloaterEditEnvironmentBase(key: LLSD) : Floater(key) {

    companion object {
        const val KEY_INVENTORY_ID: String = "inventory_id"

        private const val ACTION_APPLY_LOCAL = "apply_local"
        private const val ACTION_APPLY_PARCEL = "apply_parcel"
        private const val ACTION_APPLY_REGION = "apply_region"
    }

    protected var inventoryId: UUID = NULL_UUID
    protected var inventoryItem: InventoryItem? = null
    protected var inventoryFloater: FloaterHandle? = null
    protected var canCopy: Boolean = false
    protected var canMod: Boolean = false
    protected var canTrans: Boolean = false
    protected var canSave: Boolean = false
    protected var isDirty: Boolean = false

    // Pending asset load guard: only accept callbacks matching this ID.
    protected var expectingAssetId: UUID = NULL_UUID

    abstract fun getEditSettings(): SettingsBase?
    abstract fun setEditSettingsAndUpdate(settings: SettingsBase)
    abstract fun updateEditEnvironment()
    abstract fun getSettingsPicker(): FloaterSettingsPicker?
    abstract fun doImportFromDisk()
    abstract fun clearDirtyFlag()

    override fun isDirty(): Boolean = isDirty

    override fun onFocusReceived() {
        if (isInVisibleChain()) {
            updateEditEnvironment()
            Environment.instance.setSelectedEnvironment(Environment.ENV_EDIT, Environment.TRANSITION_FAST)
            // no-op
        }
    }

    override fun onFocusLost() {}

    fun loadInventoryItem(inventoryId: UUID, canTrans: Boolean = true) {
        if (inventoryId == NULL_UUID) {
            this.inventoryItem = null
            this.inventoryId = NULL_UUID
            canMod = true
            canCopy = true
            this.canTrans = true
            return
        }

        this.inventoryId = inventoryId
        inventoryItem = gInventory.getItem(this.inventoryId)

        if (inventoryItem == null) {
            NotificationsUtil.add("CantFindInvItem")
            closeFloater()
            this.inventoryId = NULL_UUID
            return
        }

        if (inventoryItem!!.getAssetUUID() == NULL_UUID) {
            NotificationsUtil.add("UnableEditItem")
            closeFloater()
            this.inventoryId = NULL_UUID
            inventoryItem = null
            return
        }

        canSave = true
        canCopy = inventoryItem!!.getPermissions().allowCopyBy(Agent.instance.getID())
        canMod = inventoryItem!!.getPermissions().allowModifyBy(Agent.instance.getID())
        this.canTrans = canTrans && inventoryItem!!.getPermissions().allowOperationBy(PERM_TRANSFER, Agent.instance.getID())

        expectingAssetId = inventoryItem!!.getAssetUUID()
        SettingsVOBase.getSettingsAsset(inventoryItem!!.getAssetUUID()) { assetId, settings, status ->
            onAssetLoaded(assetId, settings, status)
        }
    }

    fun checkAndConfirmSettingsLoss(cb: () -> Unit) {
        if (isDirty()) {
            val args = LLSD()
                .with("TYPE", getEditSettings()?.getSettingsType())
                .with("NAME", getEditSettings()?.getName())
            NotificationsUtil.add("SettingsConfirmLoss", args, LLSD()) { notif, resp ->
                val opt = NotificationsUtil.getSelectedOption(notif, resp)
                if (opt == 0) cb()
            }
        } else {
            cb()
        }
    }

    fun onAssetLoaded(assetId: UUID, settings: SettingsBase?, status: Int) {
        if (assetId != expectingAssetId) return
        expectingAssetId = NULL_UUID
        clearDirtyFlag()

        if (settings == null || status != 0) {
            val args = LLSD().with("NAME", inventoryItem?.getName() ?: assetId.toString())
            NotificationsUtil.add("FailedToFindSettings", args)
            closeFloater()
            return
        }

        if (settings.getFlag(SettingsBase.FLAG_NOSAVE)) {
            canSave = false; canCopy = false; canMod = false; canTrans = false
        } else {
            inventoryItem?.let { settings.setName(it.getName()) }

            if (canCopy) settings.clearFlag(SettingsBase.FLAG_NOCOPY) else settings.setFlag(SettingsBase.FLAG_NOCOPY)
            if (canMod) settings.clearFlag(SettingsBase.FLAG_NOMOD) else settings.setFlag(SettingsBase.FLAG_NOMOD)
            if (canTrans) settings.clearFlag(SettingsBase.FLAG_NOTRANS) else settings.setFlag(SettingsBase.FLAG_NOTRANS)
        }

        setEditSettingsAndUpdate(settings)
    }

    fun onButtonImport() {
        checkAndConfirmSettingsLoss { doImportFromDisk() }
    }

    fun onSaveAsCommit(notification: LLSD, response: LLSD, settings: SettingsBase) {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        if (option != 0) return

        var settingsName = response["message"].asString()
        InventoryObject.correctInventoryName(settingsName)
        if (settingsName.isEmpty()) settingsName = "Unnamed"

        if (canMod) {
            doApplyCreateNewInventory(settingsName, settings)
        } else if (inventoryItem != null) {
            val marketplaceListingsId = gInventory.getMarketplaceListingsUUID()
            var parentId = inventoryItem!!.getParentUUID()
            if (marketplaceListingsId == parentId ||
                gInventory.isObjectDescendentOf(inventoryItem!!.getUUID(), gInventory.getLibraryRootFolderID())) {
                parentId = gInventory.findCategoryUUIDForType(FolderType.FT_SETTINGS)
            }
            val cb = FixedSettingCopiedCallback(getHandle())
            System.err.println("FloaterEditEnvironmentBase: onSaveAsCommit not yet implemented")
        }
    }

    override fun onClickCloseBtn(appQuitting: Boolean) {
        if (!appQuitting) {
            checkAndConfirmSettingsLoss { closeFloater(); clearDirtyFlag() }
        } else {
            closeFloater()
        }
    }

    open fun doApplyCreateNewInventory(settingsName: String, settings: SettingsBase) {
        if (inventoryItem != null) {
            val parentId = inventoryItem!!.getParentUUID()
            val nextOwnerPerm = inventoryItem!!.getPermissions().getMaskNextOwner()
            SettingsVOBase.createInventoryItem(settings, nextOwnerPerm, parentId, settingsName) { assetId, inventoryId, _, results ->
                onInventoryCreated(assetId, inventoryId, results)
            }
        } else {
            val parentId = gInventory.findCategoryUUIDForType(FolderType.FT_SETTINGS)
            SettingsVOBase.createInventoryItem(settings, parentId, settingsName) { assetId, inventoryId, _, results ->
                onInventoryCreated(assetId, inventoryId, results)
            }
        }
    }

    open fun doApplyUpdateInventory(settings: SettingsBase) {
        if (inventoryId == NULL_UUID) {
            SettingsVOBase.createInventoryItem(settings, gInventory.findCategoryUUIDForType(FolderType.FT_SETTINGS), "") { assetId, inventoryId, _, results ->
                onInventoryCreated(assetId, inventoryId, results)
            }
        } else {
            SettingsVOBase.updateInventoryItem(settings, inventoryId) { assetId, inventoryId, _, results ->
                onInventoryUpdated(assetId, inventoryId, results)
            }
        }
    }

    open fun doApplyEnvironment(where: String, settings: SettingsBase) {
        var flags: UInt = 0u
        inventoryItem?.let {
            if (!it.getPermissions().allowOperationBy(PERM_MODIFY, Agent.instance.getID()))
                flags = flags or SettingsBase.FLAG_NOMOD
            if (!it.getPermissions().allowOperationBy(PERM_TRANSFER, Agent.instance.getID()))
                flags = flags or SettingsBase.FLAG_NOTRANS
        }
        flags = flags or settings.getFlags()
        settings.setFlag(flags)

        when (where) {
            ACTION_APPLY_LOCAL -> {
                settings.setName("Local")
                Environment.instance.setEnvironment(Environment.ENV_LOCAL, settings)
            }
            ACTION_APPLY_PARCEL -> {
                val parcel = ViewerParcelMgr.instance.getAgentOrSelectedParcel()
                if (parcel == null || parcel.getLocalID() == INVALID_PARCEL_ID) {
                    NotificationsUtil.add("WLParcelApplyFail")
                    return
                }
                if (inventoryItem != null && !isDirty()) {
                    Environment.instance.updateParcel(parcel.getLocalID(), inventoryItem!!.getAssetUUID(), inventoryItem!!.getName(), Environment.NO_TRACK, -1, -1, flags)
                } else {
                    when (settings.getSettingsType()) {
                        "sky" -> Environment.instance.updateParcel(parcel.getLocalID(), settings as SettingsSky, -1, -1)
                        "water" -> Environment.instance.updateParcel(parcel.getLocalID(), settings as SettingsWater, -1, -1)
                        "day" -> Environment.instance.updateParcel(parcel.getLocalID(), settings as SettingsDay, -1, -1)
                    }
                }
            }
            ACTION_APPLY_REGION -> {
                if (inventoryItem != null && !isDirty()) {
                    Environment.instance.updateRegion(inventoryItem!!.getAssetUUID(), inventoryItem!!.getName(), Environment.NO_TRACK, -1, -1, flags)
                } else {
                    when (settings.getSettingsType()) {
                        "sky" -> Environment.instance.updateRegion(settings as SettingsSky, -1, -1)
                        "water" -> Environment.instance.updateRegion(settings as SettingsWater, -1, -1)
                        "day" -> Environment.instance.updateRegion(settings as SettingsDay, -1, -1)
                    }
                }
            }
        }
    }

    fun doCloseInventoryFloater(quitting: Boolean = false) {
        inventoryFloater?.get()?.closeFloater(quitting)
    }

    fun onInventoryCreated(assetId: UUID, inventoryId: UUID, results: LLSD) {
        if (inventoryId == NULL_UUID || !results["success"].asBoolean()) {
            NotificationsUtil.add("CantCreateInventory")
            return
        }
        onInventoryCreated(assetId, inventoryId)
    }

    fun onInventoryCreated(assetId: UUID, inventoryId: UUID) {
        var canTrans = true
        inventoryItem?.let { item ->
            val perms = item.getPermissions()
            val createdItem = gInventory.getItem(this.inventoryId)
            if (createdItem != null) {
                canTrans = perms.allowOperationBy(PERM_TRANSFER, Agent.instance.getID())
                createdItem.setPermissions(perms)
                createdItem.updateServer(false)
            }
        }
        clearDirtyFlag()
        setFocus(true)
        loadInventoryItem(inventoryId, canTrans)
    }

    fun onInventoryUpdated(assetId: UUID, inventoryId: UUID, results: LLSD) {
        clearDirtyFlag()
        if (inventoryId != this.inventoryId) {
            loadInventoryItem(inventoryId)
        }
    }

    protected fun getIsDirty(): Boolean = isDirty
    protected fun setDirtyFlag() { isDirty = true }

    fun onPanelDirtyFlagChanged(value: Boolean) {
        if (value) setDirtyFlag()
    }

    fun canUseInventory(): Boolean = Environment.instance.isInventoryEnabled()
    fun canApplyRegion(): Boolean = Agent.instance.canManageEstate()
    fun canApplyParcel(): Boolean = Environment.instance.canAgentUpdateParcelEnvironment()

    private inner class FixedSettingCopiedCallback(private val handle: FloaterHandle) {
        fun fire(invItemId: UUID) {
            if (!handle.isDead()) {
                val item = gInventory.getItem(invItemId)
                if (item != null) {
                    onInventoryCreated(item.getAssetUUID(), invItemId)
                }
            }
        }
    }
}

abstract class SettingsEditPanel : Panel() {

    private var panelIsDirty: Boolean = false
    private var canEdit: Boolean = false
    private val onDirtyChangedListeners: MutableList<(Panel, Boolean) -> Unit> = mutableListOf()

    abstract fun setSettings(settings: SettingsBase)

    fun getIsDirty(): Boolean = panelIsDirty

    fun setIsDirty() {
        panelIsDirty = true
        onDirtyChangedListeners.forEach { it(this, panelIsDirty) }
    }

    fun clearIsDirty() {
        panelIsDirty = false
        onDirtyChangedListeners.forEach { it(this, panelIsDirty) }
    }

    fun getCanChangeSettings(): Boolean = canEdit
    fun setCanChangeSettings(flag: Boolean) { canEdit = flag }

    fun setOnDirtyFlagChanged(cb: (Panel, Boolean) -> Unit): () -> Unit {
        onDirtyChangedListeners.add(cb)
        return { onDirtyChangedListeners.remove(cb) }
    }
}
