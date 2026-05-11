package com.firestorm.newview

import com.firestorm.ui.Panel
import com.firestorm.ui.Button
import com.firestorm.ui.CheckBoxCtrl
import com.firestorm.ui.SpinCtrl
import com.firestorm.ui.FilterEditor
import com.firestorm.ui.FolderView
import com.firestorm.ui.SaveFolderState
import com.firestorm.ui.OpenFoldersWithSelection
import com.firestorm.inventory.PanelObjectInventory
import com.firestorm.object.ViewerObject
import com.firestorm.agent.Agent
import com.firestorm.inventory.InventoryModel
import com.firestorm.inventory.AssetType
import com.firestorm.inventory.InventoryType
import com.firestorm.inventory.ViewerInventoryItem
import com.firestorm.ui.FloaterReg

typealias Uuid = java.util.UUID

class PanelContents : Panel() {

    companion object {
        const val TENTATIVE_SUFFIX = "_tentative"
        const val PERMS_OWNER_INTERACT_KEY   = "perms_owner_interact"
        const val PERMS_OWNER_CONTROL_KEY    = "perms_owner_control"
        const val PERMS_GROUP_INTERACT_KEY   = "perms_group_interact"
        const val PERMS_GROUP_CONTROL_KEY    = "perms_group_control"
        const val PERMS_ANYONE_INTERACT_KEY  = "perms_anyone_interact"
        const val PERMS_ANYONE_CONTROL_KEY   = "perms_anyone_control"

        @JvmStatic fun onClickNewScript(userdata: Any?) {
            val childrenOk = true
            val obj = SelectMgr.instance.getSelection().getFirstRootObject(childrenOk) ?: return

            if (Agent.savedPerAccountSettings.getBool("FSBuildPrefs_UseCustomScript")) {
                val customScriptId = Uuid.fromString(
                    Agent.savedPerAccountSettings.getString("FSBuildPrefs_CustomScriptItem")
                )
                if (customScriptId != Uuid(0, 0)) {
                    val customScript = InventoryModel.global.getItem(customScriptId)
                    if (customScript != null && customScript.getType() == AssetType.AT_LSL_TEXT) {
                        ToolDragAndDrop.dropScript(obj, customScript, true, ToolDragAndDrop.ESource.SOURCE_AGENT, Agent.instance.getID())
                        return
                    }
                }
            }

            val perm = Permissions().apply {
                init(Agent.instance.getID(), Agent.instance.getID(), Uuid(0, 0), Uuid(0, 0))
                initMasks(
                    PERM_ALL, PERM_ALL,
                    FloaterPerms.getEveryonePerms("Scripts"),
                    FloaterPerms.getGroupPerms("Scripts"),
                    PERM_MOVE or FloaterPerms.getNextOwnerPerms("Scripts")
                )
            }
            val desc = ViewerAssetType.generateDescriptionFor(AssetType.AT_LSL_TEXT)
            val newItem = ViewerInventoryItem(
                assetId   = Uuid(0, 0),
                parentId  = Uuid(0, 0),
                perms     = perm,
                assetType = AssetType.AT_LSL_TEXT,
                invType   = InventoryType.IT_LSL,
                name      = "New Script",
                desc      = desc
            )
            obj.saveScript(newItem, active = true, removeInventory = true)
        }

        @JvmStatic fun onClickPermissions(userdata: Any?) {
            val self = userdata as? PanelContents ?: return
            TODO("APR: use JVM equivalent - open bulk permissions floater as dependent of parent")
        }

        @JvmStatic fun onClickResetScripts(userdata: Any?) {
            TODO("APR: use JVM equivalent - send reset scripts action to selected objects")
        }

        @JvmStatic fun onClickRefresh(userdata: Any?) {
            (userdata as? PanelContents)?.refresh()
        }
    }

    var filterEditor: FilterEditor? = null
    val savedFolderState = SaveFolderState()
    var panelInventoryObject: PanelObjectInventory? = null

    private var dirtyFilter: Boolean = false

    override fun postBuild(): Boolean {
        setMouseOpaque(false)

        childSetAction("button new script") { onClickNewScript(this) }
        childSetAction("button permissions") { onClickPermissions(this) }
        childSetAction("btn_reset_scripts") { onClickResetScripts(this) }
        childSetAction("button refresh") { onClickRefresh(this) }

        filterEditor = getChild<FilterEditor>("contents_filter")
        filterEditor?.setCommitCallback { _, _ -> onFilterEdit() }

        panelInventoryObject = getChild<PanelObjectInventory>("contents_inventory")

        savedFolderState.setApply(false)
        return true
    }

    fun refresh() {
        val childrenOk = true
        val obj = SelectMgr.instance.getSelection().getFirstRootObject(childrenOk)
        getState(obj)
        panelInventoryObject?.refresh()
    }

    fun clearContents() {
        panelInventoryObject?.clearInventoryTask()
    }

    private fun getState(objectp: ViewerObject?) {
        if (objectp == null) {
            getChildView("button new script")?.setEnabled(false)
            getChildView("btn_reset_scripts")?.setEnabled(false)
            return
        }

        var groupId = Uuid(0, 0)
        SelectMgr.instance.selectGetGroup { groupId = it }

        var editable = Agent.instance.isGodlike() ||
            (objectp.permModify() && !objectp.isPermanentEnforced() &&
                (objectp.permYouOwner() || (groupId != Uuid(0, 0) && Agent.instance.isInGroup(groupId))))

        val allVolume = SelectMgr.instance.selectionAllPCode(LL_PCODE_VOLUME)

        val objectIsOk = editable && allVolume &&
            (SelectMgr.instance.getSelection().getRootObjectCount() == 1 ||
             SelectMgr.instance.getSelection().getObjectCount() == 1)

        getChildView("button new script")?.setEnabled(objectIsOk)
        getChildView("btn_reset_scripts")?.setEnabled(objectIsOk)
        getChildView("button permissions")?.setEnabled(!objectp.isPermanentEnforced())
        panelInventoryObject?.setEnabled(!objectp.isPermanentEnforced())
    }

    fun onFilterEdit() {
        val filterSubstring = filterEditor?.getText() ?: ""
        if (panelInventoryObject?.hasInventory() != true) {
            dirtyFilter = true
        } else {
            val rootFolder: FolderView? = panelInventoryObject?.getRootFolder()
            if (filterSubstring.isEmpty()) {
                if (panelInventoryObject?.getFilter()?.getFilterSubString().isNullOrEmpty()) {
                    return
                }
                if (dirtyFilter && !savedFolderState.hasOpenFolders()) {
                    rootFolder?.setOpenArrangeRecursively(true, FolderView.ERecurseType.RECURSE_DOWN)
                } else {
                    savedFolderState.setApply(true)
                    rootFolder?.applyFunctorRecursively(savedFolderState)
                }
                dirtyFilter = false
                rootFolder?.let {
                    val opener = OpenFoldersWithSelection()
                    it.applyFunctorRecursively(opener)
                    it.scrollToShowSelection()
                }
            } else if (panelInventoryObject?.getFilter()?.getFilterSubString().isNullOrEmpty()) {
                if (panelInventoryObject?.getFilter()?.isNotDefault() == false) {
                    savedFolderState.setApply(false)
                    rootFolder?.applyFunctorRecursively(savedFolderState)
                    dirtyFilter = false
                }
            }
        }
        panelInventoryObject?.getFilter()?.setFilterSubString(filterSubstring)
    }
}
