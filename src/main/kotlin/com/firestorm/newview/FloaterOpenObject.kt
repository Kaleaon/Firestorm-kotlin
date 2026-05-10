package com.firestorm.newview

import com.firestorm.floater.Floater
import com.firestorm.inventory.InventoryPanel
import com.firestorm.objects.ObjectSelection
import com.firestorm.objects.SafeHandle
import com.firestorm.ui.LLSD
import java.util.UUID

class FloaterOpenObject(key: LLSD) : Floater(key) {

    data class CatAndWear(
        val catId: UUID,
        val wear: Boolean,
        val folderResponded: Boolean,
        val replace: Boolean
    )

    private var panelInventoryObject: PanelObjectInventory? = null
    private var objectSelection: SafeHandle<ObjectSelection> = SafeHandle()
    private var dirty: Boolean = true

    init {
        registerCommitCallback("OpenObject.CopyAction") { _, value -> onClickCopy(value) }
        registerCommitCallback("OpenObject.Cancel")     { _, _     -> onClickCancel() }
    }

    override fun postBuild(): Boolean {
        getChild<UICtrl>("object_name").setTextArg("[DESC]", "Object")
        panelInventoryObject = getChild("object_contents")
        refresh()
        return true
    }

    override fun onOpen(key: LLSD) {
        val objectSelection = SelectMgr.instance.selection
        if (objectSelection.rootObjectCount != 1) {
            NotificationsUtil.add("UnableToViewContentsMoreThanOne")
            closeFloater()
            return
        }
        if (objectSelection.primaryObject == null) {
            closeFloater()
            return
        }
        this.objectSelection = SelectMgr.instance.editSelection
        refresh()
    }

    fun refresh() {
        panelInventoryObject?.refresh()

        val node = objectSelection.firstRootNode
        if (node != null && RlvActions.isRlvEnabled() && !RlvActions.canEdit(node.`object`)) {
            closeFloater()
            return
        }

        val (name, enabled) = if (node != null) node.name to true else "" to false
        getChild<UICtrl>("object_name").setTextArg("[DESC]", name)
        getChildView("copy_flyout").setEnabled(enabled)
    }

    override fun draw() {
        if (dirty) {
            refresh()
            dirty = false
        }
        super.draw()
    }

    fun dirty() {
        dirty = true
    }

    private fun moveToInventory(wear: Boolean, replace: Boolean = false) {
        if (objectSelection.rootObjectCount != 1) {
            NotificationsUtil.add("OnlyCopyContentsOfSingleItem")
            return
        }

        val node = objectSelection.firstRootNode ?: return
        val obj  = node.`object` ?: return
        val objectId = obj.id
        val name = node.name

        val parentCategoryId =
            if (wear) Inventory.findCategoryUUIDForType(FolderType.CLOTHING)
            else      Inventory.rootFolderId

        Inventory.createNewCategory(parentCategoryId, FolderType.NONE, name) { categoryId ->
            callbackCreateInventoryCategory(categoryId, objectId, wear, replace)
        }
    }

    private fun onClickCancel() {
        closeFloater()
    }

    fun onClickCopy(value: LLSD) {
        when (value.asString()) {
            "replace" -> moveToInventory(wear = true, replace = true)
            "add"     -> moveToInventory(wear = true, replace = false)
            else      -> moveToInventory(wear = false)
        }
        closeFloater()
    }

    companion object {
        fun callbackCreateInventoryCategory(
            categoryId: UUID,
            objectId: UUID,
            wear: Boolean,
            replace: Boolean = false
        ) {
            val wearData = CatAndWear(
                catId = categoryId,
                wear = wear,
                folderResponded = true,
                replace = replace
            )

            val success = moveInvCategoryWorldToAgent(
                objectId, categoryId, ignoreWarnings = true
            ) { result, data ->
                callbackMoveInventory(result, data)
            }

            if (!success) {
                NotificationsUtil.add("OpenObjectCannotCopy")
            }
        }

        fun callbackMoveInventory(result: Int, data: CatAndWear?) {
            if (result == 0) {
                val activePanel = InventoryPanel.activeInventoryPanel
                activePanel?.setSelection(data?.catId, takeFocus = false)
            }
        }
    }
}
