package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.UICtrl
import java.util.UUID

class FloaterOpenObject(key: Any) : Floater(key) {

    data class CatAndWear(
        val catId: UUID,
        val wear: Boolean,
        val folderResponded: Boolean,
        val replace: Boolean
    )

    private var panelInventoryObject: Any? = null
    private var objectSelection: Any?      = null
    private var dirty: Boolean             = true

    init {
        registerCommitCallback("OpenObject.CopyAction") { value -> onClickCopy(value) }
        registerCommitCallback("OpenObject.Cancel")     { onClickCancel() }
    }

    override fun postBuild(): Boolean {
        getChild<UICtrl>("object_name").setTextArg("[DESC]", "Object")
        panelInventoryObject = getChild<Any>("object_contents")
        refresh()
        return true
    }

    open fun onOpen(key: Any) {
        val objectSelection = getEditSelection()
        if (getRootObjectCount(objectSelection) != 1) {
            System.err.println("FloaterOpenObject: show UnableToViewContentsMoreThanOne notification not yet implemented")
            closeFloater()
            return
        }
        if (getPrimaryObject(objectSelection) == null) {
            closeFloater()
            return
        }
        this.objectSelection = objectSelection
        refresh()
    }

    open fun draw() {
        if (dirty) {
            refresh()
            dirty = false
        }
        super.draw()
    }

    fun dirty() {
        dirty = true
    }

    private fun refresh() {
        refreshPanelInventory()

        val node = getFirstRootNode(objectSelection)

        if (isRlvEnabled() && node != null && !rlvCanEdit(getObject(node))) {
            closeFloater()
            return
        }

        val name    = node?.let { getName(it) } ?: ""
        val enabled = node != null

        getChild<UICtrl>("object_name").setTextArg("[DESC]", name)
        getChildView("copy_flyout").setEnabled(enabled)
    }

    private fun moveToInventory(wear: Boolean, replace: Boolean = false) {
        if (getRootObjectCount(objectSelection) != 1) {
            System.err.println("FloaterOpenObject: show OnlyCopyContentsOfSingleItem notification not yet implemented")
            return
        }

        val node   = getFirstRootNode(objectSelection) ?: return
        val obj    = getObject(node) ?: return
        val objId  = getObjectId(obj)
        val name   = getName(node)

        val parentCategoryId: UUID = if (wear) {
            UUID(0L, 0L).also { System.err.println("FloaterOpenObject: find clothing category UUID from inventory not yet implemented") }
        } else {
            UUID(0L, 0L).also { System.err.println("FloaterOpenObject: get root folder UUID from inventory not yet implemented") }
        }

        System.err.println("FloaterOpenObject: gInventory.createNewCategory then callbackCreateInventoryCategory not yet implemented")
    }

    private fun onClickCopy(value: Any?) {
        val action = value?.toString() ?: ""
        when (action) {
            "replace" -> moveToInventory(wear = true, replace = true)
            "add"     -> moveToInventory(wear = true, replace = false)
            else      -> moveToInventory(wear = false)
        }
        closeFloater()
    }

    private fun onClickCancel() {
        closeFloater()
    }

    companion object {
        fun callbackCreateInventoryCategory(categoryId: UUID, objectId: UUID, wear: Boolean, replace: Boolean = false) {
            val wearData = CatAndWear(
                catId           = categoryId,
                wear            = wear,
                folderResponded = true,
                replace         = replace
            )
            val success: Boolean = false.also { System.err.println("FloaterOpenObject: move_inv_category_world_to_agent with callbackMoveInventory not yet implemented") }
            if (!success) {
                System.err.println("FloaterOpenObject: show OpenObjectCannotCopy notification not yet implemented")
            }
        }

        fun callbackMoveInventory(result: Int, data: CatAndWear) {
            if (result == 0) {
                System.err.println("FloaterOpenObject: get active inventory panel and select data.catId not yet implemented")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Stubs for C++ subsystems that have no direct JVM equivalent
    // ---------------------------------------------------------------------------

    private fun getEditSelection(): Any?            = null
    private fun getRootObjectCount(sel: Any?): Int  = 0
    private fun getPrimaryObject(sel: Any?): Any?   = null
    private fun getFirstRootNode(sel: Any?): Any?   = null
    private fun getObject(node: Any): Any?          = null
    private fun getObjectId(obj: Any): UUID         = UUID(0L, 0L)
    private fun getName(node: Any): String          = ""
    private fun refreshPanelInventory()             { System.err.println("FloaterOpenObject: refreshPanelInventory not yet implemented") }
    private fun isRlvEnabled(): Boolean             = false
    private fun rlvCanEdit(obj: Any?): Boolean      = false
}
