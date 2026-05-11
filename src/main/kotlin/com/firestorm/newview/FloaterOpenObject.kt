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
            TODO("APR: use JVM equivalent - show UnableToViewContentsMoreThanOne notification")
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
            TODO("APR: use JVM equivalent - show OnlyCopyContentsOfSingleItem notification")
            return
        }

        val node   = getFirstRootNode(objectSelection) ?: return
        val obj    = getObject(node) ?: return
        val objId  = getObjectId(obj)
        val name   = getName(node)

        val parentCategoryId: UUID = if (wear) {
            TODO("APR: use JVM equivalent - find clothing category UUID from inventory")
        } else {
            TODO("APR: use JVM equivalent - get root folder UUID from inventory")
        }

        TODO("APR: use JVM equivalent - gInventory.createNewCategory then callbackCreateInventoryCategory")
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
            val success: Boolean = TODO("APR: use JVM equivalent - move_inv_category_world_to_agent with callbackMoveInventory")
            if (!success) {
                TODO("APR: use JVM equivalent - show OpenObjectCannotCopy notification")
            }
        }

        fun callbackMoveInventory(result: Int, data: CatAndWear) {
            if (result == 0) {
                TODO("APR: use JVM equivalent - get active inventory panel and select data.catId")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Stubs for C++ subsystems that have no direct JVM equivalent
    // ---------------------------------------------------------------------------

    private fun getEditSelection(): Any?            = TODO("APR: use JVM equivalent - LLSelectMgr::getEditSelection")
    private fun getRootObjectCount(sel: Any?): Int  = TODO("APR: use JVM equivalent - selection->getRootObjectCount()")
    private fun getPrimaryObject(sel: Any?): Any?   = TODO("APR: use JVM equivalent - selection->getPrimaryObject()")
    private fun getFirstRootNode(sel: Any?): Any?   = TODO("APR: use JVM equivalent - selection->getFirstRootNode()")
    private fun getObject(node: Any): Any?          = TODO("APR: use JVM equivalent - node->getObject()")
    private fun getObjectId(obj: Any): UUID         = TODO("APR: use JVM equivalent - object->getID()")
    private fun getName(node: Any): String          = TODO("APR: use JVM equivalent - node->mName")
    private fun refreshPanelInventory()             = TODO("APR: use JVM equivalent - mPanelInventoryObject->refresh()")
    private fun isRlvEnabled(): Boolean             = TODO("APR: use JVM equivalent - RlvActions::isRlvEnabled()")
    private fun rlvCanEdit(obj: Any?): Boolean      = TODO("APR: use JVM equivalent - RlvActions::canEdit(object)")
}
