package com.firestorm.newview

import java.util.UUID

class FloaterBuyContents(key: Any) : Floater(key), VOInventoryListener {

    private var objectSelection: ObjectSelectionHandle? = null
    private var saleInfo: SaleInfo = SaleInfo()

    override fun postBuild(): Boolean {
        getChildView("cancel_btn").setCommitCallback { onClickCancel() }
        getChildView("buy_btn").setCommitCallback { onClickBuy() }

        getChildView("item_list").setEnabled(false)
        getChildView("buy_btn").setEnabled(false)
        getChildView("wear_check").setEnabled(false)

        setDefaultBtn("cancel_btn")
        center()
        return true
    }

    override fun onDestroy() {
        removeVOInventoryListener()
    }

    override fun inventoryChanged(
        obj: ViewerObject?,
        inv: MutableList<InventoryObject>?,
        serialNum: Int,
        data: Any?
    ) {
        if (obj == null) return

        val itemList = getChildScrollList("item_list") ?: run {
            removeVOInventoryListener()
            return
        }

        itemList.deleteAllItems()

        if (inv == null) return

        val buyBtn = getChildView("buy_btn")
        buyBtn.setEnabled(false)

        var wearableCount = 0

        for (invObj in inv) {
            val assetType = invObj.getType()

            if (assetType == AssetType.AT_CATEGORY) continue

            val invItem = invObj as InventoryItem
            val invType = invItem.getInventoryType()

            if (invType == InventoryType.IT_WEARABLE) {
                wearableCount++
            }

            val ownerId: UUID
            val isGroupOwned: Boolean
            val ownership = invItem.getPermissions().getOwnership() ?: continue
            ownerId = ownership.first
            isGroupOwned = ownership.second

            if (!invItem.getPermissions().allowCopyBy(ownerId, ownerId)) continue
            if (!invItem.getPermissions().allowTransferTo(Agent.id)) continue

            buyBtn.setEnabled(true)

            val itemIsMulti = ((invItem.getFlags() and InventoryItemFlags.II_FLAGS_LANDMARK_VISITED != 0u
                    || invItem.getFlags() and InventoryItemFlags.II_FLAGS_OBJECT_HAS_MULTIPLE_ITEMS != 0u)
                    && invItem.getFlags() and InventoryItemFlags.II_FLAGS_SUBTYPE_MASK == 0u)

            val iconName = InventoryIcon.getIconName(
                invItem.getType(),
                invItem.getInventoryType(),
                invItem.getFlags(),
                itemIsMulti
            )

            val nextOwnerMask = invItem.getPermissions().getMaskNextOwner()
            var text = invObj.getName()
            if (nextOwnerMask and PermFlags.PERM_COPY == 0u) text += getString("no_copy_text")
            if (nextOwnerMask and PermFlags.PERM_MODIFY == 0u) text += getString("no_modify_text")
            if (nextOwnerMask and PermFlags.PERM_TRANSFER == 0u) text += getString("no_transfer_text")

            itemList.addRow(
                icon = iconName,
                text = text,
                font = "SANSSERIF"
            )
        }

        if (wearableCount > 0) {
            getChildView("wear_check").setEnabled(true)
            getChildCtrl("wear_check").setValue(false)
        }
    }

    private fun onClickBuy() {
        if (!getChildView("buy_btn").isEnabled()) {
            closeFloater()
            return
        }

        if (getChildCtrl("wear_check").getValue() == true) {
            InventoryState.sWearNewClothing = true
        }

        val categoryId = Inventory.findCategoryUUIDForType(FolderType.FT_ROOT_INVENTORY)
        SelectMgr.instance.sendBuy(Agent.id, categoryId, saleInfo)
        FirstUse.newInventory()
        closeFloater()
    }

    private fun onClickCancel() {
        closeFloater()
    }

    companion object {
        fun show(saleInfo: SaleInfo) {
            val selection = SelectMgr.instance.getSelection()

            if (selection.getRootObjectCount() != 1) {
                NotificationsUtil.add("BuyContentsOneOnly")
                return
            }

            val floater = FloaterReg.showTypedInstance<FloaterBuyContents>("buy_object_contents") ?: return

            floater.getChildScrollList("item_list")?.deleteAllItems()
            floater.objectSelection = SelectMgr.instance.getSelection()

            val ownerResult = SelectMgr.instance.selectGetOwner() ?: run {
                NotificationsUtil.add("BuyContentsOneOwner")
                return
            }
            val (ownerId, ownerName) = ownerResult

            floater.saleInfo = saleInfo

            val node = selection.getFirstRootNode() ?: return
            val resolvedOwnerName = if (node.permissions?.isGroupOwned() == true) {
                CacheName.getGroupName(ownerId)
            } else {
                ownerName
            }

            floater.getChildCtrl("contains_text").setTextArg("[NAME]", node.name)
            floater.getChildCtrl("buy_text").setTextArg("[AMOUNT]", saleInfo.getSalePrice().toString())
            floater.getChildCtrl("buy_text").setTextArg("[NAME]", resolvedOwnerName)

            val obj = selection.getFirstRootObject()
            floater.registerVOInventoryListener(obj, null)
            floater.requestVOInventory()
        }
    }
}
