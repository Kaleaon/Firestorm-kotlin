package com.firestorm.newview

import java.util.UUID

class LLFloaterMyEnvironment(key: LLSD) :
    LLFloater(key),
    LLInventoryFetchDescendentsObserver {

    private var inventoryList: LLInventoryPanel? = null
    private var filterEdit: LLFilterEditor? = null
    private var typeFilter: Long = 0L
    private var showFolders: LLInventoryFilter.EFolderShow = LLInventoryFilter.EFolderShow.SHOW_NON_EMPTY_FOLDERS
    private var selectedAsset: UUID = UUID.randomUUID()
    private val savedFolderState: LLSaveFolderState = LLSaveFolderState()

    companion object {
        private const val CHECK_DAYS = "chk_days"
        private const val CHECK_SKIES = "chk_skies"
        private const val CHECK_WATER = "chk_water"
        private const val FLT_SEARCH = "flt_search"
        private const val PANEL_SETTINGS = "pnl_settings"
        private const val CHECK_SHOWFOLDERS = "chk_showfolders"
        private const val BUTTON_NEWSETTINGS = "btn_gear"
        private const val BUTTON_GEAR = "btn_newsettings"
        private const val BUTTON_DELETE = "btn_del"

        private const val ACTION_DOCREATE = "MyEnvironments.DoCreate"
        private const val ACTION_DOEDIT = "MyEnvironments.DoEdit"
        private const val ACTION_DOAPPLY = "MyEnvironments.DoApply"
        private const val ACTION_COPYPASTE = "MyEnvironments.CopyPaste"
        private const val ENABLE_ACTION = "MyEnvironments.EnableAction"
        private const val ENABLE_CANAPPLY = "MyEnvironments.CanApply"
        private const val ENABLE_ENVIRONMENT = "MyEnvironments.EnvironmentEnabled"

        private const val PARAMETER_REGION = "region"
        private const val PARAMETER_PARCEL = "parcel"
        private const val PARAMETER_LOCAL = "local"

        private const val PARAMETER_EDIT = "edit"
        private const val PARAMETER_COPY = "copy"
        private const val PARAMETER_PASTE = "paste"
        private const val PARAMETER_COPYUUID = "copy_uuid"

        fun isSettingId(itemId: UUID): Boolean {
            val itemp = gInventory.getItem(itemId)
            return itemp != null && itemp.inventoryType == LLInventoryType.IT_SETTINGS
        }

        fun findItemByAssetId(assetId: UUID, copyableOnly: Boolean, ignoreLibrary: Boolean): UUID {
            val cats = mutableListOf<LLViewerInventoryCategory>()
            val items = mutableListOf<LLViewerInventoryItem>()
            val matcher = LLAssetIDMatches(assetId)

            gInventory.collectDescendentsIf(
                LLUUIDNull,
                cats,
                items,
                LLInventoryModel.INCLUDE_TRASH,
                matcher
            )

            if (items.isNotEmpty()) {
                for (item in items) {
                    val perms = item.permissions
                    if (perms.allowCopyBy(gAgent.id, gAgent.groupId)) {
                        if (!ignoreLibrary || !gInventory.isObjectDescendentOf(item.uuid, gInventory.libraryRootFolderId)) {
                            return item.uuid
                        }
                    }
                }
                if (copyableOnly) {
                    return LLUUIDNull
                } else {
                    if (!ignoreLibrary || !gInventory.isObjectDescendentOf(items[0].uuid, gInventory.libraryRootFolderId)) {
                        return items[0].uuid
                    }
                }
            }
            return LLUUIDNull
        }
    }

    init {
        val stDaycycleBit = 1L shl LLSettingsType.ST_DAYCYCLE.ordinal.toLong().toInt()
        val stSkyBit = 1L shl LLSettingsType.ST_SKY.ordinal.toLong().toInt()
        val stWaterBit = 1L shl LLSettingsType.ST_WATER.ordinal.toLong().toInt()
        typeFilter = stDaycycleBit or stSkyBit or stWaterBit
        selectedAsset = LLUUIDNull

        mCommitCallbackRegistrar.add(ACTION_DOCREATE) { _: LLUICtrl, userdata: LLSD -> onDoCreate(userdata) }
        mCommitCallbackRegistrar.add(ACTION_DOEDIT) { _: LLUICtrl, _: LLSD -> inventoryList?.openSelected() }
        mCommitCallbackRegistrar.add(ACTION_DOAPPLY) { _: LLUICtrl, userdata: LLSD -> onDoApply(userdata.asString()) }
        mCommitCallbackRegistrar.add(ACTION_COPYPASTE) { _: LLUICtrl, userdata: LLSD -> inventoryList?.doToSelected(userdata.asString()) }

        mEnableCallbackRegistrar.add(ENABLE_ACTION) { _: LLUICtrl, userdata: LLSD -> canAction(userdata.asString()) }
        mEnableCallbackRegistrar.add(ENABLE_CANAPPLY) { _: LLUICtrl, userdata: LLSD -> canApply(userdata.asString()) }
        mEnableCallbackRegistrar.add(ENABLE_ENVIRONMENT) { _: LLUICtrl, _: LLSD -> LLEnvironment.instance().isInventoryEnabled() }
    }

    override fun postBuild(): Boolean {
        inventoryList = getChild<LLInventoryPanel>(PANEL_SETTINGS)

        inventoryList?.let { panel ->
            var filterTypes = 0
            filterTypes = filterTypes or (1 shl LLInventoryType.IT_SETTINGS.value)
            panel.setFilterTypes(filterTypes)
            panel.setSelectCallback { _: List<LLFolderViewItem>, _: Boolean -> onSelectionChange() }
            panel.setShowFolderState(showFolders)
            panel.setFilterSettingsTypes(typeFilter)
        }

        childSetCommitCallback(CHECK_DAYS, { _: LLUICtrl, _: Any? -> onFilterCheckChange() }, null)
        childSetCommitCallback(CHECK_SKIES, { _: LLUICtrl, _: Any? -> onFilterCheckChange() }, null)
        childSetCommitCallback(CHECK_WATER, { _: LLUICtrl, _: Any? -> onFilterCheckChange() }, null)
        childSetCommitCallback(CHECK_SHOWFOLDERS, { _: LLUICtrl, _: Any? -> onShowFoldersChange() }, null)

        filterEdit = getChild<LLFilterEditor>(FLT_SEARCH)
        filterEdit?.setCommitCallback { _: LLUICtrl, param: LLSD -> onFilterEdit(param.asString()) }

        childSetCommitCallback(BUTTON_DELETE, { _: LLUICtrl, _: Any? -> onDeleteSelected() }, null)
        savedFolderState.setApply(false)
        return true
    }

    override fun refresh() {
        getChild<LLCheckBoxCtrl>(CHECK_SHOWFOLDERS).setValue(
            showFolders == LLInventoryFilter.EFolderShow.SHOW_ALL_FOLDERS
        )
        val stDaycycleBit = 1L shl LLSettingsType.ST_DAYCYCLE.ordinal.toLong().toInt()
        val stSkyBit = 1L shl LLSettingsType.ST_SKY.ordinal.toLong().toInt()
        val stWaterBit = 1L shl LLSettingsType.ST_WATER.ordinal.toLong().toInt()

        getChild<LLCheckBoxCtrl>(CHECK_DAYS).setValue((typeFilter and stDaycycleBit) != 0L)
        getChild<LLCheckBoxCtrl>(CHECK_SKIES).setValue((typeFilter and stSkyBit) != 0L)
        getChild<LLCheckBoxCtrl>(CHECK_WATER).setValue((typeFilter and stWaterBit) != 0L)

        refreshButtonStates()
    }

    override fun onOpen(key: LLSD) {
        super.onOpen(key)

        if (key.has("asset_id") && inventoryList != null) {
            selectedAsset = key["asset_id"].asUUID()
            if (selectedAsset != LLUUIDNull) {
                val objId = findItemByAssetId(selectedAsset, false, false)
                if (objId != LLUUIDNull) {
                    inventoryList!!.setSelection(objId, false)
                }
            }
        } else {
            selectedAsset = LLUUIDNull
        }

        refresh()
    }

    private fun onShowFoldersChange() {
        val showCheck = getChild<LLCheckBoxCtrl>(CHECK_SHOWFOLDERS).getValue().asBoolean()
        showFolders = if (showCheck) {
            LLInventoryFilter.EFolderShow.SHOW_ALL_FOLDERS
        } else {
            LLInventoryFilter.EFolderShow.SHOW_NON_EMPTY_FOLDERS
        }
        inventoryList?.setShowFolderState(showFolders)
    }

    private fun onFilterCheckChange() {
        typeFilter = 0L
        val stDaycycleBit = 1L shl LLSettingsType.ST_DAYCYCLE.ordinal.toLong().toInt()
        val stSkyBit = 1L shl LLSettingsType.ST_SKY.ordinal.toLong().toInt()
        val stWaterBit = 1L shl LLSettingsType.ST_WATER.ordinal.toLong().toInt()

        if (getChild<LLCheckBoxCtrl>(CHECK_DAYS).getValue().asBoolean()) typeFilter = typeFilter or stDaycycleBit
        if (getChild<LLCheckBoxCtrl>(CHECK_SKIES).getValue().asBoolean()) typeFilter = typeFilter or stSkyBit
        if (getChild<LLCheckBoxCtrl>(CHECK_WATER).getValue().asBoolean()) typeFilter = typeFilter or stWaterBit

        inventoryList?.setFilterSettingsTypes(typeFilter)
    }

    private fun onSelectionChange() {
        refreshButtonStates()
    }

    private fun onFilterEdit(searchString: String) {
        val upperSearch = searchString.toUpperCase()

        if (upperSearch.isEmpty()) {
            if (inventoryList?.getFilterSubString()?.isEmpty() == true) {
                return
            }
            savedFolderState.setApply(true)
            inventoryList?.getRootFolder()?.applyFunctorRecursively(savedFolderState)
            val opener = LLOpenFoldersWithSelection()
            inventoryList?.getRootFolder()?.applyFunctorRecursively(opener)
            inventoryList?.getRootFolder()?.scrollToShowSelection()
        } else if (inventoryList?.getFilterSubString()?.isEmpty() == true) {
            savedFolderState.setApply(false)
            inventoryList?.getRootFolder()?.applyFunctorRecursively(savedFolderState)
        }

        inventoryList?.setFilterSubString(searchString)
    }

    private fun onItemsRemovalConfirmation(notification: LLSD, response: LLSD, itemIds: List<UUID>) {
        val option = LLNotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) {
            val trashId = gInventory.findCategoryUUIDForType(LLFolderType.FT_TRASH)
            for (itemId in itemIds) {
                val invItem = gInventory.getItem(itemId)
                if (invItem != null && invItem.inventoryType == LLInventoryType.IT_SETTINGS) {
                    val update = mutableListOf<LLInventoryModel.LLCategoryUpdate>()
                    update.add(LLInventoryModel.LLCategoryUpdate(invItem.parentUUID, -1))
                    update.add(LLInventoryModel.LLCategoryUpdate(trashId, 1))
                    gInventory.accountForUpdate(update)

                    val newItem = LLViewerInventoryItem(invItem)
                    newItem.setParent(trashId)
                    newItem.updateParentOnServer(false)
                    gInventory.updateItem(newItem)
                }
            }
            gInventory.notifyObservers()
        }
    }

    private fun onDeleteSelected() {
        val selected = mutableListOf<UUID>()
        getSelectedIds(selected)
        if (selected.isEmpty()) return

        val args = LLSD()
        args["QUESTION"] = LLTrans.getString(if (selected.size > 1) "DeleteItems" else "DeleteItem")
        LLNotificationsUtil.add(
            "DeleteItems",
            args,
            LLSD()
        ) { notification: LLSD, response: LLSD ->
            onItemsRemovalConfirmation(notification, response, selected)
        }
    }

    private fun onDoCreate(data: LLSD) {
        menuCreateInventoryItem(inventoryList, null, data)
    }

    private fun onDoApply(context: String) {
        val selected = mutableListOf<UUID>()
        getSelectedIds(selected)
        if (selected.size != 1) return

        val itemId = selected.first()
        val itemp = gInventory.getItem(itemId)

        if (itemp != null && itemp.inventoryType == LLInventoryType.IT_SETTINGS) {
            val assetId = itemp.assetUUID
            val name = itemp.name

            var flags = 0u
            if (!itemp.permissions.allowOperationBy(PERM_MODIFY, gAgent.id)) flags = flags or LLSettingsBase.FLAG_NOMOD
            if (!itemp.permissions.allowOperationBy(PERM_TRANSFER, gAgent.id)) flags = flags or LLSettingsBase.FLAG_NOTRANS

            when (context) {
                PARAMETER_REGION -> {
                    LLEnvironment.instance().updateRegion(assetId, name, LLEnvironment.NO_TRACK, -1, -1, flags)
                    LLEnvironment.instance().setSharedEnvironment()
                }
                PARAMETER_PARCEL -> {
                    val parcel = LLViewerParcelMgr.instance().getAgentOrSelectedParcel()
                    if (parcel == null) return
                    LLEnvironment.instance().updateParcel(parcel.localId, assetId, name, LLEnvironment.NO_TRACK, -1, -1, flags)
                    LLEnvironment.instance().setSharedEnvironment()
                }
                PARAMETER_LOCAL -> {
                    LLEnvironment.instance().setEnvironment(LLEnvironment.ENV_LOCAL, assetId)
                    LLEnvironment.instance().setSelectedEnvironment(LLEnvironment.ENV_LOCAL)
                }
            }
        }
    }

    private fun canAction(context: String): Boolean {
        val selected = mutableListOf<UUID>()
        getSelectedIds(selected)
        if (selected.isEmpty()) return false

        return when (context) {
            PARAMETER_EDIT -> selected.size == 1 && isSettingId(selected.first())
            PARAMETER_COPY -> selected.all { isSettingId(it) }
            PARAMETER_PASTE -> {
                if (!LLClipboard.instance().hasContents()) return false
                if (gInventory.isObjectDescendentOf(selected.first(), gInventory.libraryRootFolderId)) return false
                val ids = mutableListOf<UUID>()
                LLClipboard.instance().pasteFromClipboard(ids)
                ids.all { isSettingId(it) } && selected.size == 1
            }
            PARAMETER_COPYUUID -> selected.size == 1 && isSettingId(selected.first())
            else -> false
        }
    }

    private fun canApply(context: String): Boolean {
        val selected = mutableListOf<UUID>()
        getSelectedIds(selected)
        if (selected.size != 1) return false

        return when (context) {
            PARAMETER_REGION -> isSettingId(selected.first()) && LLEnvironment.instance().canAgentUpdateRegionEnvironment()
            PARAMETER_PARCEL -> isSettingId(selected.first()) && LLEnvironment.instance().canAgentUpdateParcelEnvironment()
            PARAMETER_LOCAL -> isSettingId(selected.first())
            else -> false
        }
    }

    private fun canDelete(id: UUID): Boolean {
        val trashId = gInventory.findCategoryUUIDForType(LLFolderType.FT_TRASH)
        if (id == trashId || gInventory.isObjectDescendentOf(id, trashId)) return false

        val cat = gInventory.getCategory(id)
        if (cat != null) {
            if (!getIsCategoryRemovable(gInventory, id)) return false
        } else if (!getIsItemRemovable(gInventory, id, false)) {
            return false
        }
        return true
    }

    private fun refreshButtonStates() {
        val settingsOk = LLEnvironment.instance().isInventoryEnabled()

        val selected = mutableListOf<UUID>()
        getSelectedIds(selected)

        getChild<LLUICtrl>(BUTTON_GEAR).setEnabled(settingsOk)
        getChild<LLUICtrl>(BUTTON_NEWSETTINGS).setEnabled(true)

        val enableDelete = settingsOk && selected.isNotEmpty() && canDelete(selected.first())
        getChild<LLUICtrl>(BUTTON_DELETE).setEnabled(enableDelete)
    }

    private fun getSelectedIds(ids: MutableList<UUID>) {
        val items = inventoryList?.getSelectedItems() ?: return
        for (itemView in items) {
            val itemp = itemView.getViewModelItem() as? LLFolderViewModelItemInventory ?: continue
            ids.add(itemp.uuid)
        }
    }
}
