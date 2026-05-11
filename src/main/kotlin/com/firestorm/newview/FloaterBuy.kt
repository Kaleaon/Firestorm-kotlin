package com.firestorm.newview

import java.util.UUID

class FloaterBuy(key: Any) : Floater(key), VOInventoryListener {

    private var objectSelection: ObjectSelectionHandle? = null
    private var saleInfo: SaleInfo = SaleInfo()
    private var selectionUpdateSlot: (() -> Unit)? = null

    override fun postBuild(): Boolean {
        getChildView("object_list").setEnabled(false)
        getChildView("item_list").setEnabled(false)

        getChildView("cancel_btn").setCommitCallback { onClickCancel() }
        getChildView("buy_btn").setCommitCallback { onClickBuy() }

        setDefaultBtn("cancel_btn")
        center()
        return true
    }

    override fun onClose(appQuitting: Boolean) {
        selectionUpdateSlot = null
        objectSelection = null
    }

    override fun onDestroy() {
        objectSelection = null
    }

    private fun reset() {
        getChildScrollList("object_list")?.deleteAllItems()
        getChildScrollList("item_list")?.deleteAllItems()
    }

    override fun inventoryChanged(
        obj: ViewerObject?,
        inv: MutableList<InventoryObject>?,
        serialNum: Int,
        data: Any?
    ) {
        if (obj == null) return

        if (inv == null) {
            removeVOInventoryListener()
            return
        }

        val itemList = childGetListInterface("item_list") ?: run {
            removeVOInventoryListener()
            return
        }

        for (invObj in inv) {
            if (invObj.getType() == AssetType.AT_CATEGORY) continue
            if (invObj.getType() == AssetType.AT_NONE) continue

            val invItem = invObj as InventoryItem

            if (!invItem.getPermissions().allowTransferTo(Agent.id)) continue

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
            if (nextOwnerMask and PermFlags.PERM_COPY == 0u) text += Trans.getString("no_copy")
            if (nextOwnerMask and PermFlags.PERM_MODIFY == 0u) text += Trans.getString("no_modify")
            if (nextOwnerMask and PermFlags.PERM_TRANSFER == 0u) text += Trans.getString("no_transfer")

            itemList.addRow(
                icon = iconName,
                text = text,
                font = "SANSSERIF"
            )
        }

        removeVOInventoryListener()
    }

    private fun onSelectionChanged() {
        val editSel = SelectMgr.instance.getEditSelection()
        when {
            editSel.getRootObjectCount() == 0 -> {
                removeVOInventoryListener()
                closeFloater()
            }
            editSel.getRootObjectCount() > 1 -> {
                removeVOInventoryListener()
                showViews(false)
                reset()
                setTitle(getString("multiple_selected"))
            }
        }
    }

    private fun showViews(show: Boolean) {
        getChildCtrl("buy_btn").setEnabled(show)
        getChildCtrl("buy_text").setVisible(show)
        getChildCtrl("buy_name_text").setVisible(show)
    }

    private fun onClickBuy() {
        val categoryId = Inventory.findCategoryUUIDForType(FolderType.FT_OBJECT)
        SelectMgr.instance.sendBuy(Agent.id, categoryId, saleInfo)
        closeFloater()
    }

    private fun onClickCancel() {
        closeFloater()
    }

    companion object {
        fun show(saleInfo: SaleInfo) {
            val selection = SelectMgr.instance.getSelection()

            if (selection.getRootObjectCount() != 1) {
                NotificationsUtil.add("BuyOneObjectOnly")
                return
            }

            val floater = FloaterReg.showTypedInstance<FloaterBuy>("buy_object") ?: return

            floater.reset()
            floater.saleInfo = saleInfo
            floater.objectSelection = SelectMgr.instance.getSelection()

            val node = selection.getFirstRootNode() ?: return

            val titleTemplate = when (saleInfo.getSaleType()) {
                SaleInfo.ForSale.FS_ORIGINAL -> floater.getString("title_buy_text")
                else -> floater.getString("title_buy_copy_text")
            }
            floater.setTitle(titleTemplate.replace("[NAME]", node.name))

            val ownerResult = SelectMgr.instance.selectGetOwner() ?: run {
                NotificationsUtil.add("BuyObjectOneOwner")
                return
            }
            val (_, ownerName) = ownerResult

            val objectList = floater.childGetListInterface("object_list") ?: return

            val iconName = InventoryIcon.getIconName(
                AssetType.AT_OBJECT,
                InventoryType.IT_OBJECT
            )

            val nextOwnerMask = node.permissions?.getMaskNextOwner() ?: 0u
            var text = node.name
            if (nextOwnerMask and PermFlags.PERM_COPY == 0u) text += floater.getString("no_copy_text")
            if (nextOwnerMask and PermFlags.PERM_MODIFY == 0u) text += floater.getString("no_modify_text")
            if (nextOwnerMask and PermFlags.PERM_TRANSFER == 0u) text += floater.getString("no_transfer_text")

            objectList.addRow(icon = iconName, text = text, font = "SANSSERIF")

            floater.getChildCtrl("buy_text").setTextArg("[AMOUNT]", saleInfo.getSalePrice().toString())
            floater.getChildCtrl("buy_name_text").setTextArg("[NAME]", ownerName)

            floater.showViews(true)

            val obj = selection.getFirstRootObject()
            floater.registerVOInventoryListener(obj, null)
            floater.requestVOInventory()

            if (floater.selectionUpdateSlot == null) {
                val slot = { floater.onSelectionChanged() }
                SelectMgr.instance.mUpdateSignal.add(slot)
                floater.selectionUpdateSlot = slot
            }
        }
    }
}
