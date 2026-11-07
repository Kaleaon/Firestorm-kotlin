package com.firestorm.newview

import java.util.UUID

private class PropertiesObserver(private val floater: FloaterProperties) : InventoryObserver {
    init {
        Inventory.addObserver(this)
    }

    override fun onDestroy() {
        Inventory.removeObserver(this)
    }

    override fun changed(mask: UInt) {
        if (mask and (InventoryObserver.LABEL or InventoryObserver.INTERNAL or InventoryObserver.REMOVE) != 0u) {
            floater.dirty()
        }
    }
}

class FloaterProperties(key: Any) : Floater(key) {

    var itemId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    var objectId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var isDirty = true
    private val propertiesObserver = PropertiesObserver(this)

    private var creatorNameCbConnection: (() -> Unit)? = null
    private var ownerNameCbConnection: (() -> Unit)? = null
    private var groupOwnerNameCbConnection: (() -> Unit)? = null

    init {
        val keyMap = key as? Map<*, *>
        keyMap?.get("item_id")?.let { itemId = it as UUID }
        keyMap?.get("object_id")?.let { objectId = it as UUID }
    }

    override fun onDestroy() {
        propertiesObserver.onDestroy()
        creatorNameCbConnection = null
        ownerNameCbConnection = null
        groupOwnerNameCbConnection = null
    }

    override fun postBuild(): Boolean {
        getChildLineEditor("LabelItemName").setPrevalidate(TextValidate::validateASCIIPrintableNoPipe)
        getChildCtrl("LabelItemName").setCommitCallback { onCommitName() }
        getChildLineEditor("LabelItemDesc").setPrevalidate(TextValidate::validateASCIIPrintableNoPipe)
        getChildCtrl("LabelItemDesc").setCommitCallback { onCommitDescription() }
        getChildCtrl("BtnCreator").setCommitCallback { onClickCreator() }
        getChildCtrl("BtnOwner").setCommitCallback { onClickOwner() }
        getChildCtrl("CheckShareWithGroup").setCommitCallback { onCommitPermissions() }
        getChildCtrl("CheckEveryoneCopy").setCommitCallback { onCommitPermissions() }
        getChildCtrl("CheckNextOwnerModify").setCommitCallback { onCommitPermissions() }
        getChildCtrl("CheckNextOwnerCopy").setCommitCallback { onCommitPermissions() }
        getChildCtrl("CheckNextOwnerTransfer").setCommitCallback { onCommitPermissions() }
        getChildCtrl("CheckOwnerExport").setCommitCallback { onCommitPermissions() }
        getChildCtrl("CheckOwnerExport").setVisible(!GridManager.instance.isInSecondLife())
        getChildCtrl("CheckPurchase").setCommitCallback { onCommitSaleInfo() }
        getChildCtrl("ComboBoxSaleType").setCommitCallback { onCommitSaleType() }
        getChildCtrl("Edit Cost").setCommitCallback { onCommitSaleInfo() }
        refresh()
        return true
    }

    override fun onOpen(key: Any) {
        refresh()
    }

    fun setObjectId(objectId: UUID) {
        this.objectId = objectId
    }

    fun dirty() {
        isDirty = true
    }

    fun refresh() {
        val item = findItem()
        if (item != null) {
            refreshFromItem(item)
        } else {
            isDirty = true
            val enableNames = listOf(
                "LabelItemName", "LabelItemDesc", "LabelCreatorName", "BtnCreator",
                "LabelOwnerName", "BtnOwner", "CheckOwnerModify", "CheckOwnerCopy",
                "CheckOwnerTransfer", "CheckShareWithGroup", "CheckEveryoneCopy",
                "CheckNextOwnerModify", "CheckNextOwnerCopy", "CheckNextOwnerTransfer",
                "CheckOwnerExport", "CheckPurchase", "ComboBoxSaleType", "Edit Cost"
            )
            for (name in enableNames) getChildView(name).setEnabled(false)

            val hideNames = listOf(
                "BaseMaskDebug", "OwnerMaskDebug", "GroupMaskDebug",
                "EveryoneMaskDebug", "NextMaskDebug"
            )
            for (name in hideNames) getChildView(name).setVisible(false)
        }
    }

    override fun draw() {
        if (isDirty) {
            isDirty = false
            refresh()
        }
        super.draw()
    }

    private fun refreshFromItem(item: InventoryItem) {
        val viewerItem = item as ViewerInventoryItem
        val isComplete = viewerItem.isFinished()
        val cannotRestrictPermissions = InventoryType.cannotRestrictPermissions(viewerItem.getInventoryType())
        val isCallingCard = viewerItem.getInventoryType() == InventoryType.IT_CALLINGCARD
        val isSettings = item.getInventoryType() == InventoryType.IT_SETTINGS
        val perm = item.getPermissions()
        val canAgentManipulate = Agent.allowOperation(PermFlags.PERM_OWNER, perm, GroupPowers.GP_OBJECT_MANIPULATE)
        val canAgentSell = Agent.allowOperation(PermFlags.PERM_OWNER, perm, GroupPowers.GP_OBJECT_SET_SALE)
                && !cannotRestrictPermissions
        val isLink = viewerItem.getIsLinkType()

        val obj: ViewerObject? = if (!objectId.isNullUUID()) ObjectList.findObject(objectId) else null
        val isObjModify = obj?.permOwnerModify() ?: true

        if (item.getInventoryType() == InventoryType.IT_LSL) {
            getChildView("LabelItemExperienceTitle").setVisible(true)
            val tb = getChildTextBox("LabelItemExperience")
            tb.setText(getString("loading_experience"))
            tb.setVisible(true)
            ExperienceCache.instance.fetchAssociatedExperience(item.getParentUUID(), item.getUUID()) { experience ->
                setAssociatedExperience(this, experience)
            }
        }

        val isModifiable = Agent.allowOperation(PermFlags.PERM_MODIFY, perm, GroupPowers.GP_OBJECT_MANIPULATE)
                && isObjModify && isComplete

        getChildView("LabelItemNameTitle").setEnabled(true)
        getChildView("LabelItemName").setEnabled(isModifiable && !isCallingCard)
        getChildCtrl("LabelItemName").setValue(item.getName())
        getChildView("LabelItemDescTitle").setEnabled(true)
        getChildView("LabelItemDesc").setEnabled(isModifiable)
        getChildView("IconLocked").setVisible(!isModifiable)
        getChildCtrl("LabelItemDesc").setValue(item.getDescription())

        if (!CacheName.isReady() || !Agent.hasRegion()) return

        if (!item.getCreatorUUID().isNullUUID()) {
            getChildView("LabelCreatorTitle").setEnabled(true)
            getChildView("LabelCreatorName").setEnabled(true)
            getChildView("BtnCreator").setEnabled(false)
            getChildCtrl("LabelCreatorName").setValue(Trans.getString("AvatarNameWaiting"))
            creatorNameCbConnection = AvatarNameCache.get(item.getCreatorUUID()) { avId, avName ->
                onCreatorNameCallback(avId, avName, perm)
            }
        } else {
            getChildView("BtnCreator").setEnabled(false)
            getChildView("LabelCreatorTitle").setEnabled(false)
            getChildView("LabelCreatorName").setEnabled(false)
            getChildCtrl("LabelCreatorName").setValue(getString("unknown"))
        }

        if (perm.isOwned()) {
            getChildView("BtnOwner").setEnabled(false)
            getChildCtrl("LabelOwnerName").setValue(Trans.getString("AvatarNameWaiting"))
            if (perm.isGroupOwned()) {
                groupOwnerNameCbConnection = CacheName.getGroup(perm.getGroup()) { _, name, _ ->
                    onGroupOwnerNameCallback(name)
                }
            } else {
                ownerNameCbConnection = AvatarNameCache.get(perm.getOwner()) { avId, avName ->
                    onOwnerNameCallback(avId, avName)
                }
            }
            getChildView("LabelOwnerTitle").setEnabled(true)
            getChildView("LabelOwnerName").setEnabled(true)
        } else {
            getChildView("BtnOwner").setEnabled(false)
            getChildView("LabelOwnerTitle").setEnabled(false)
            getChildView("LabelOwnerName").setEnabled(false)
            getChildCtrl("LabelOwnerName").setValue(getString("public"))
        }

        val timeUtc = item.getCreationDate()
        if (timeUtc == 0L) {
            getChildCtrl("LabelAcquiredDate").setValue(getString("unknown"))
        } else {
            val timeStr = getString("acquiredDate").replace("[datetime]", timeUtc.toString())
            getChildCtrl("LabelAcquiredDate").setValue(timeStr)
        }

        getChildCtrl("OwnerLabel").setValue(
            if (canAgentManipulate) getString("you_can") else getString("owner_can")
        )

        val baseMask = perm.getMaskBase()
        val ownerMask = perm.getMaskOwner()
        val groupMask = perm.getMaskGroup()
        val everyoneMask = perm.getMaskEveryone()
        val nextOwnerMask = perm.getMaskNextOwner()

        getChildView("OwnerLabel").setEnabled(true)
        getChildView("CheckOwnerModify").setEnabled(false)
        getChildCtrl("CheckOwnerModify").setValue(ownerMask and PermFlags.PERM_MODIFY != 0u)
        getChildView("CheckOwnerCopy").setEnabled(false)
        getChildCtrl("CheckOwnerCopy").setValue(ownerMask and PermFlags.PERM_COPY != 0u)
        getChildView("CheckOwnerTransfer").setEnabled(false)
        getChildCtrl("CheckOwnerTransfer").setValue(ownerMask and PermFlags.PERM_TRANSFER != 0u)
        getChildView("CheckOwnerExport").setEnabled(false)
        getChildCtrl("CheckOwnerExport").setValue(ownerMask and PermFlags.PERM_EXPORT != 0u)

        if (SavedSettings.getBool("DebugPermissions")) {
            var slamPerm = false
            var overwriteGroup = false
            var overwriteEveryone = false

            if (item.getType() == AssetType.AT_OBJECT) {
                val flags = item.getFlags()
                slamPerm = flags and InventoryItemFlags.II_FLAGS_OBJECT_SLAM_PERM != 0u
                overwriteEveryone = flags and InventoryItemFlags.II_FLAGS_OBJECT_PERM_OVERWRITE_EVERYONE != 0u
                overwriteGroup = flags and InventoryItemFlags.II_FLAGS_OBJECT_PERM_OVERWRITE_GROUP != 0u
            }

            val isOpenSim = GridManager.instance.isInOpenSim()

            getChildCtrl("BaseMaskDebug").setValue("B: ${maskToString(baseMask, isOpenSim)}")
            getChildView("BaseMaskDebug").setVisible(true)
            getChildCtrl("OwnerMaskDebug").setValue("O: ${maskToString(ownerMask, isOpenSim)}")
            getChildView("OwnerMaskDebug").setVisible(true)
            getChildCtrl("GroupMaskDebug").setValue("G${if (overwriteGroup) "*" else ""}: ${maskToString(groupMask, isOpenSim)}")
            getChildView("GroupMaskDebug").setVisible(true)
            getChildCtrl("EveryoneMaskDebug").setValue("E${if (overwriteEveryone) "*" else ""}: ${maskToString(everyoneMask, isOpenSim)}")
            getChildView("EveryoneMaskDebug").setVisible(true)
            getChildCtrl("NextMaskDebug").setValue("N${if (slamPerm) "*" else ""}: ${maskToString(nextOwnerMask, isOpenSim)}")
            getChildView("NextMaskDebug").setVisible(true)
        } else {
            for (name in listOf("BaseMaskDebug", "OwnerMaskDebug", "GroupMaskDebug", "EveryoneMaskDebug", "NextMaskDebug")) {
                getChildView(name).setVisible(false)
            }
        }

        if (isLink || cannotRestrictPermissions) {
            getChildView("CheckShareWithGroup").setEnabled(false)
            getChildView("CheckEveryoneCopy").setEnabled(false)
        } else if (isObjModify && canAgentManipulate) {
            getChildView("CheckShareWithGroup").setEnabled(true)
            getChildView("CheckEveryoneCopy").setEnabled(
                ownerMask and PermFlags.PERM_COPY != 0u && ownerMask and PermFlags.PERM_TRANSFER != 0u
            )
        } else {
            getChildView("CheckShareWithGroup").setEnabled(false)
            getChildView("CheckEveryoneCopy").setEnabled(false)
        }

        getChildView("CheckOwnerExport").setEnabled(Agent.id == item.getCreatorUUID())

        val isGroupCopy = groupMask and PermFlags.PERM_COPY != 0u
        val isGroupModify = groupMask and PermFlags.PERM_MODIFY != 0u
        val isGroupMove = groupMask and PermFlags.PERM_MOVE != 0u

        val shareCheckBox = getChildCheckBox("CheckShareWithGroup")
        when {
            isGroupCopy && isGroupModify && isGroupMove -> {
                shareCheckBox?.setValue(true)
                shareCheckBox?.setTentative(false)
            }
            !isGroupCopy && !isGroupModify && !isGroupMove -> {
                shareCheckBox?.setValue(false)
                shareCheckBox?.setTentative(false)
            }
            else -> {
                shareCheckBox?.setTentative(true)
                shareCheckBox?.set(true)
            }
        }

        getChildCtrl("CheckEveryoneCopy").setValue(everyoneMask and PermFlags.PERM_COPY != 0u)

        val saleInfo = item.getSaleInfo()
        val isForSale = saleInfo.isForSale()
        val comboSaleType = getChildComboBox("ComboBoxSaleType")
        val editCost = getChildCtrl("Edit Cost")

        if (isObjModify && canAgentSell
            && Agent.allowOperation(PermFlags.PERM_TRANSFER, perm, GroupPowers.GP_OBJECT_MANIPULATE)
        ) {
            getChildView("CheckPurchase").setEnabled(isComplete)
            getChildView("NextOwnerLabel").setEnabled(true)
            getChildView("CheckNextOwnerModify").setEnabled(baseMask and PermFlags.PERM_MODIFY != 0u && !cannotRestrictPermissions)
            getChildView("CheckNextOwnerCopy").setEnabled(baseMask and PermFlags.PERM_COPY != 0u && !cannotRestrictPermissions && !isSettings)
            getChildView("CheckNextOwnerTransfer").setEnabled(nextOwnerMask and PermFlags.PERM_COPY != 0u && !cannotRestrictPermissions)
            comboSaleType?.setEnabled(isComplete && isForSale)
            editCost?.setEnabled(isComplete && isForSale)
        } else {
            getChildView("CheckPurchase").setEnabled(false)
            getChildView("NextOwnerLabel").setEnabled(false)
            getChildView("CheckNextOwnerModify").setEnabled(false)
            getChildView("CheckNextOwnerCopy").setEnabled(false)
            getChildView("CheckNextOwnerTransfer").setEnabled(false)
            comboSaleType?.setEnabled(false)
            editCost?.setEnabled(false)
        }

        if (isSettings) {
            for (name in listOf("GroupLabel", "CheckShareWithGroup", "AnyoneLabel", "CheckEveryoneCopy",
                                "CheckPurchase", "ComboBoxSaleType", "Edit Cost", "CurrencySymbol")) {
                getChildCtrl(name).setEnabled(false)
                getChildCtrl(name).setVisible(false)
            }
        }

        getChildCtrl("CheckPurchase").setValue(isForSale)
        getChildCtrl("CheckNextOwnerModify").setValue(nextOwnerMask and PermFlags.PERM_MODIFY != 0u)
        getChildCtrl("CheckNextOwnerCopy").setValue(nextOwnerMask and PermFlags.PERM_COPY != 0u)
        getChildCtrl("CheckNextOwnerTransfer").setValue(nextOwnerMask and PermFlags.PERM_TRANSFER != 0u)

        if (isForSale) {
            editCost?.setValue(saleInfo.getSalePrice().toString())
            comboSaleType?.setValue(saleInfo.getSaleType())
        } else {
            editCost?.setValue("0")
            comboSaleType?.setValue(SaleInfo.ForSale.FS_COPY)
        }
    }

    private fun onClickCreator() {
        val item = findItem() ?: return
        if (!item.getCreatorUUID().isNullUUID()) {
            AvatarActions.showProfile(item.getCreatorUUID())
        }
    }

    private fun onClickOwner() {
        val item = findItem() ?: return
        if (item.getPermissions().isGroupOwned()) {
            GroupActions.show(item.getPermissions().getGroup())
        } else {
            AvatarActions.showProfile(item.getPermissions().getOwner())
        }
    }

    private fun onCommitName() {
        val item = findItem() as? ViewerInventoryItem ?: return
        val labelItemName = getChildLineEditor("LabelItemName")
        if (item.getName() != labelItemName.getText()
            && Agent.allowOperation(PermFlags.PERM_MODIFY, item.getPermissions(), GroupPowers.GP_OBJECT_MANIPULATE)
        ) {
            val newItem = ViewerInventoryItem(item)
            newItem.rename(labelItemName.getText())
            if (objectId.isNullUUID()) {
                newItem.updateServer(false)
                Inventory.updateItem(newItem)
                Inventory.notifyObservers()
            } else {
                ObjectList.findObject(objectId)?.updateInventory(newItem, TaskInventoryItemKey, false)
            }
        }
    }

    private fun onCommitDescription() {
        val item = findItem() as? ViewerInventoryItem ?: return
        val labelItemDesc = getChildLineEditor("LabelItemDesc")
        if (item.getDescription() != labelItemDesc.getText()
            && Agent.allowOperation(PermFlags.PERM_MODIFY, item.getPermissions(), GroupPowers.GP_OBJECT_MANIPULATE)
        ) {
            val newItem = ViewerInventoryItem(item)
            newItem.setDescription(labelItemDesc.getText())
            if (objectId.isNullUUID()) {
                newItem.updateServer(false)
                Inventory.updateItem(newItem)
                Inventory.notifyObservers()
            } else {
                ObjectList.findObject(objectId)?.updateInventory(newItem, TaskInventoryItemKey, false)
            }
        }
    }

    private fun onCommitPermissions() {
        val item = findItem() as? ViewerInventoryItem ?: return
        val perm = Permissions(item.getPermissions())

        getChildCheckBox("CheckShareWithGroup")?.let { check ->
            perm.setGroupBits(Agent.id, Agent.getGroupID(), check.get(),
                PermFlags.PERM_MODIFY or PermFlags.PERM_MOVE or PermFlags.PERM_COPY)
        }
        getChildCheckBox("CheckEveryoneCopy")?.let { check ->
            perm.setEveryoneBits(Agent.id, Agent.getGroupID(), check.get(), PermFlags.PERM_COPY)
        }
        getChildCheckBox("CheckNextOwnerModify")?.let { check ->
            perm.setNextOwnerBits(Agent.id, Agent.getGroupID(), check.get(), PermFlags.PERM_MODIFY)
        }
        getChildCheckBox("CheckNextOwnerCopy")?.let { check ->
            perm.setNextOwnerBits(Agent.id, Agent.getGroupID(), check.get(), PermFlags.PERM_COPY)
        }
        getChildCheckBox("CheckNextOwnerTransfer")?.let { check ->
            perm.setNextOwnerBits(Agent.id, Agent.getGroupID(), check.get(), PermFlags.PERM_TRANSFER)
        }
        getChildCheckBox("CheckOwnerExport")?.let { check ->
            perm.setNextOwnerBits(Agent.id, Agent.getGroupID(), check.get(), PermFlags.PERM_EXPORT)
        }

        if (perm != item.getPermissions() && item.isFinished()) {
            val newItem = ViewerInventoryItem(item)
            newItem.setPermissions(perm)
            var flags = newItem.getFlags()
            if (perm.getMaskNextOwner() != item.getPermissions().getMaskNextOwner()
                && item.getType() == AssetType.AT_OBJECT
            ) flags = flags or InventoryItemFlags.II_FLAGS_OBJECT_SLAM_PERM
            if (perm.getMaskEveryone() != item.getPermissions().getMaskEveryone()
                && item.getType() == AssetType.AT_OBJECT
            ) flags = flags or InventoryItemFlags.II_FLAGS_OBJECT_PERM_OVERWRITE_EVERYONE
            if (perm.getMaskGroup() != item.getPermissions().getMaskGroup()
                && item.getType() == AssetType.AT_OBJECT
            ) flags = flags or InventoryItemFlags.II_FLAGS_OBJECT_PERM_OVERWRITE_GROUP
            newItem.setFlags(flags)

            if (objectId.isNullUUID()) {
                newItem.updateServer(false)
                Inventory.updateItem(newItem)
                Inventory.notifyObservers()
            } else {
                ObjectList.findObject(objectId)?.updateInventory(newItem, TaskInventoryItemKey, false)
            }
        } else {
            refresh()
        }
    }

    private fun onCommitSaleInfo() {
        updateSaleInfo()
    }

    private fun onCommitSaleType() {
        updateSaleInfo()
    }

    private fun updateSaleInfo() {
        val item = findItem() as? ViewerInventoryItem ?: return
        val saleInfo = SaleInfo(item.getSaleInfo())

        if (!Agent.allowOperation(PermFlags.PERM_TRANSFER, item.getPermissions(), GroupPowers.GP_OBJECT_SET_SALE)) {
            getChildCtrl("CheckPurchase").setValue(false)
        }

        if (getChildCtrl("CheckPurchase").getValue() == true) {
            var saleType = SaleInfo.ForSale.FS_COPY
            val comboSaleType = getChildComboBox("ComboBoxSaleType")
            if (comboSaleType != null) {
                saleType = SaleInfo.ForSale.fromInt(comboSaleType.getValue().toInt())
            }

            if (saleType == SaleInfo.ForSale.FS_COPY
                && !Agent.allowOperation(PermFlags.PERM_COPY, item.getPermissions(), GroupPowers.GP_OBJECT_SET_SALE)
            ) {
                saleType = SaleInfo.ForSale.FS_ORIGINAL
            }

            val price = getChildCtrl("Edit Cost").getValue().toIntOrNull() ?: -1
            if (price < 0) {
                saleInfo.setSaleType(SaleInfo.ForSale.FS_NOT)
                saleInfo.setSalePrice(0)
            } else {
                saleInfo.setSaleType(saleType)
                saleInfo.setSalePrice(price)
            }
        } else {
            saleInfo.setSaleType(SaleInfo.ForSale.FS_NOT)
        }

        if (saleInfo != item.getSaleInfo() && item.isFinished()) {
            val newItem = ViewerInventoryItem(item)
            if (item.getType() == AssetType.AT_OBJECT) {
                newItem.setFlags(newItem.getFlags() or InventoryItemFlags.II_FLAGS_OBJECT_SLAM_SALE)
            }
            newItem.setSaleInfo(saleInfo)

            if (objectId.isNullUUID()) {
                newItem.updateServer(false)
                Inventory.updateItem(newItem)
                Inventory.notifyObservers()
            } else {
                ObjectList.findObject(objectId)?.updateInventory(newItem, TaskInventoryItemKey, false)
            }
        } else {
            refresh()
        }
    }

    private fun findItem(): InventoryItem? {
        return if (objectId.isNullUUID()) {
            Inventory.getItem(itemId)
        } else {
            ObjectList.findObject(objectId)?.getInventoryObject(itemId) as? InventoryItem
        }
    }

    private fun onCreatorNameCallback(avId: UUID, avName: AvatarName, perm: Permissions) {
        getChildCtrl("LabelCreatorName").setValue(avName.getUserName())
        childSetEnabled("BtnCreator", true)
    }

    private fun onOwnerNameCallback(avId: UUID, avName: AvatarName) {
        getChildCtrl("LabelOwnerName").setValue(avName.getUserName())
        getChildView("BtnOwner").setEnabled(true)
    }

    private fun onGroupOwnerNameCallback(name: String) {
        getChildCtrl("LabelOwnerName").setValue(name)
        getChildView("BtnOwner").setEnabled(true)
    }

    companion object {
        fun dirtyAll() {
            FloaterReg.getFloaterList("properties").forEach { floater ->
                (floater as? FloaterProperties)?.dirty()
            }
        }

        fun setAssociatedExperience(floater: FloaterProperties, experience: Map<String, Any>) {
            val id = experience[ExperienceCache.EXPERIENCE_ID] as? UUID
            val tb = floater.getChildTextBox("LabelItemExperience")
            if (id != null && !id.isNullUUID()) {
                tb.setText(SLURL("experience", id, "profile").getSLURLString())
            } else {
                tb.setText(Trans.getString("ExperienceNameNull"))
            }
        }
    }
}

class MultiProperties : MultiFloater() {
    init {
        System.err.println("MultiProperties: init not yet implemented")
        setTitle(Trans.getString("MultiPropertiesTitle"))
        buildTabContainer()
        center()
    }
}
