package com.firestorm.newview

import java.util.UUID

class LLFloaterOpenObject private constructor(key: LLSD) : LLFloater(key) {

    data class LLCatAndWear(
        val catID: UUID,
        val wear: Boolean,
        val folderResponded: Boolean,
        val replace: Boolean
    )

    private var panelInventoryObject: LLPanelObjectInventory? = null
    private var objectSelection: LLSafeHandle<LLObjectSelection>? = null
    private var dirty: Boolean = true

    init {
        mCommitCallbackRegistrar.add("OpenObject.CopyAction") { _, value -> onClickCopy(value) }
        mCommitCallbackRegistrar.add("OpenObject.Cancel") { _, _ -> onClickCancel() }
    }

    override fun postBuild(): Boolean {
        getChild<LLUICtrl>("object_name").setTextArg("[DESC]", "Object")
        panelInventoryObject = getChild("object_contents")
        refresh()
        return true
    }

    override fun onOpen(key: LLSD) {
        val objectSelection = LLSelectMgr.getInstance().getSelection()
        if (objectSelection.getRootObjectCount() != 1) {
            LLNotificationsUtil.add("UnableToViewContentsMoreThanOne")
            closeFloater()
            return
        }
        if (objectSelection.getPrimaryObject() == null) {
            closeFloater()
            return
        }
        this.objectSelection = LLSelectMgr.getInstance().getEditSelection()
        refresh()
    }

    private fun refresh() {
        panelInventoryObject!!.refresh()

        val node = objectSelection?.getFirstRootNode()
        if (node != null) {
            getChild<LLUICtrl>("object_name").setTextArg("[DESC]", node.mName)
            getChildView("copy_flyout").setEnabled(true)
        } else {
            getChild<LLUICtrl>("object_name").setTextArg("[DESC]", "")
            getChildView("copy_flyout").setEnabled(false)
        }
    }

    fun draw() {
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
        if (objectSelection!!.getRootObjectCount() != 1) {
            LLNotificationsUtil.add("OnlyCopyContentsOfSingleItem")
            return
        }

        val node = objectSelection!!.getFirstRootNode() ?: return
        val obj = node.getObject() ?: return

        val objectId = obj.getID()
        val name = node.mName

        val parentCategoryId: UUID = if (wear) {
            gInventory.findCategoryUUIDForType(LLFolderType.FT_CLOTHING)
        } else {
            gInventory.getRootFolderID()
        }

        gInventory.createNewCategory(
            parentCategoryId,
            LLFolderType.FT_NONE,
            name
        ) { categoryId -> callbackCreateInventoryCategory(categoryId, objectId, wear, replace) }
    }

    private fun onClickCancel() {
        closeFloater()
    }

    private fun onClickCopy(value: LLSD) {
        val action = value.asString()
        when (action) {
            "replace" -> moveToInventory(wear = true, replace = true)
            "add" -> moveToInventory(wear = true, replace = false)
            else -> moveToInventory(wear = false)
        }
        closeFloater()
    }

    companion object {
        fun callbackCreateInventoryCategory(categoryId: UUID, objectId: UUID, wear: Boolean, replace: Boolean = false) {
            val wearData = LLCatAndWear(
                catID = categoryId,
                wear = wear,
                folderResponded = true,
                replace = replace
            )

            val success = moveInvCategoryWorldToAgent(
                objectId,
                categoryId,
                ignoreNoMod = true
            ) { result, data -> callbackMoveInventory(result, data) }

            if (!success) {
                LLNotificationsUtil.add("OpenObjectCannotCopy")
            }
        }

        fun callbackMoveInventory(result: Int, data: LLCatAndWear?) {
            if (result == 0) {
                val activePanel = LLInventoryPanel.getActiveInventoryPanel()
                activePanel?.setSelection(data!!.catID, takeFocus = false)
            }
        }
    }
}
