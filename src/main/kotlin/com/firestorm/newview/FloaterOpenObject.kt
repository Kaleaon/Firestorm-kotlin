package com.firestorm.newview

import com.firestorm.floater.Floater
import com.firestorm.ui.LLSD
import java.util.UUID

class FloaterOpenObject(key: LLSD) : Floater(key) {

    data class CatAndWear(
        val catId: UUID,
        val wear: Boolean,
        val folderResponded: Boolean,
        val replace: Boolean
    )

    private var panelInventoryObject: Any? = null
    private var objectSelection: Any? = null
    private var dirty: Boolean = true

    init {
        registerCommitCallback("OpenObject.CopyAction") { _, value -> onClickCopy(value) }
        registerCommitCallback("OpenObject.Cancel")     { _, _     -> onClickCancel() }
    }

    override fun postBuild(): Boolean {
        getChild<Any>("object_name").let {
            TODO("APR: setTextArg [DESC] = 'Object'")
        }
        panelInventoryObject = getChild<Any>("object_contents")
        refresh()
        return true
    }

    override fun onOpen(key: LLSD) {
        val sel = SelectMgr.instance.selection
        if (sel.rootObjectCount != 1) {
            NotificationsUtil.add("UnableToViewContentsMoreThanOne")
            closeFloater()
            return
        }
        if (sel.primaryObject == null) {
            closeFloater()
            return
        }
        objectSelection = SelectMgr.instance.editSelection
        refresh()
    }

    fun refresh() {
        (panelInventoryObject as? PanelObjectInventory)?.refresh()

        val node = (objectSelection as? ObjectSelection)?.firstRootNode
        if (node != null && RlvActions.isRlvEnabled() && !RlvActions.canEdit(node.`object`)) {
            closeFloater()
            return
        }

        val (name, enabled) = if (node != null) node.name to true else "" to false
        getChild<Any>("object_name").let { TODO("APR: setTextArg [DESC] = name") }
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
        val sel = objectSelection as? ObjectSelection
        if (sel == null || sel.rootObjectCount != 1) {
            NotificationsUtil.add("OnlyCopyContentsOfSingleItem")
            return
        }

        val node   = sel.firstRootNode ?: return
        val obj    = node.`object` ?: return
        val objId  = obj.id
        val name   = node.name

        val parentCatId =
            if (wear) Inventory.findCategoryUUIDForType(FolderType.CLOTHING)
            else      Inventory.rootFolderId

        Inventory.createNewCategory(parentCatId, FolderType.NONE, name) { categoryId ->
            callbackCreateInventoryCategory(categoryId, objId, wear, replace)
        }
    }

    private fun onClickCancel() = closeFloater()

    fun onClickCopy(value: LLSD) {
        when (value.asString()) {
            "replace" -> moveToInventory(wear = true,  replace = true)
            "add"     -> moveToInventory(wear = true,  replace = false)
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
                catId          = categoryId,
                wear           = wear,
                folderResponded = true,
                replace        = replace
            )

            val success = moveInvCategoryWorldToAgent(
                objectId, categoryId, ignoreWarnings = true
            ) { result, _ ->
                callbackMoveInventory(result, wearData)
            }

            if (!success) NotificationsUtil.add("OpenObjectCannotCopy")
        }

        fun callbackMoveInventory(result: Int, cat: CatAndWear?) {
            if (result == 0) {
                val activePanel = InventoryPanel.activeInventoryPanel
                activePanel?.setSelection(cat?.catId, takeFocus = false)
            }
        }
    }
}
