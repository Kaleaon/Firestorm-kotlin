package com.firestorm.newview

import java.util.UUID

// FloaterReg is the sole factory; constructor is private to enforce that.
class FloaterBulkPermission private constructor(seed: Any) : Floater(seed), VOInventoryListener {

    // UI
    private var queueOutputList: ScrollListCtrl? = null

    // Object queue
    private val objectIDs: MutableList<UUID> = mutableListOf()
    private var currentObjectID: UUID? = null
    private var done: Boolean = false

    // Asset-type inclusion filters (loaded from saved settings in postBuild).
    private var bulkChangeIncludeAnimations: Boolean = false
    private var bulkChangeIncludeBodyParts: Boolean = false
    private var bulkChangeIncludeClothing: Boolean = false
    private var bulkChangeIncludeGestures: Boolean = false
    private var bulkChangeIncludeNotecards: Boolean = false
    private var bulkChangeIncludeObjects: Boolean = false
    private var bulkChangeIncludeScripts: Boolean = false
    private var bulkChangeIncludeSounds: Boolean = false
    private var bulkChangeIncludeTextures: Boolean = false
    private var bulkChangeIncludeSettings: Boolean = false
    private var bulkChangeIncludeMaterials: Boolean = false

    // Permission targets.
    private var bulkChangeShareWithGroup: Boolean = false
    private var bulkChangeEveryoneCopy: Boolean = false
    private var bulkChangeNextOwnerModify: Boolean = false
    private var bulkChangeNextOwnerCopy: Boolean = false
    private var bulkChangeNextOwnerTransfer: Boolean = false

    // Unique ID for matching VOInventoryListener callbacks to this floater instance.
    private val id: UUID = UUID.randomUUID()

    init {
        registerCommitCallback("BulkPermission.Ok")         { onOkBtn() }
        registerCommitCallback("BulkPermission.Apply")      { onApplyBtn() }
        registerCommitCallback("BulkPermission.Close")      { onCloseBtn() }
        registerCommitCallback("BulkPermission.CheckAll")   { onCheckAll() }
        registerCommitCallback("BulkPermission.UncheckAll") { onUncheckAll() }
        registerCommitCallback("BulkPermission.CommitCopy") { onCommitCopy() }
    }

    override fun postBuild(): Boolean {
        bulkChangeIncludeAnimations = SavedSettings.getBool("BulkChangeIncludeAnimations")
        bulkChangeIncludeBodyParts  = SavedSettings.getBool("BulkChangeIncludeBodyParts")
        bulkChangeIncludeClothing   = SavedSettings.getBool("BulkChangeIncludeClothing")
        bulkChangeIncludeGestures   = SavedSettings.getBool("BulkChangeIncludeGestures")
        bulkChangeIncludeNotecards  = SavedSettings.getBool("BulkChangeIncludeNotecards")
        bulkChangeIncludeObjects    = SavedSettings.getBool("BulkChangeIncludeObjects")
        bulkChangeIncludeScripts    = SavedSettings.getBool("BulkChangeIncludeScripts")
        bulkChangeIncludeSounds     = SavedSettings.getBool("BulkChangeIncludeSounds")
        bulkChangeIncludeTextures   = SavedSettings.getBool("BulkChangeIncludeTextures")
        bulkChangeIncludeSettings   = SavedSettings.getBool("BulkChangeIncludeSettings")
        bulkChangeIncludeMaterials  = SavedSettings.getBool("BulkChangeIncludeMaterials")

        bulkChangeShareWithGroup    = SavedSettings.getBool("BulkChangeShareWithGroup")
        bulkChangeEveryoneCopy      = SavedSettings.getBool("BulkChangeEveryoneCopy")
        bulkChangeNextOwnerModify   = SavedSettings.getBool("BulkChangeNextOwnerModify")
        bulkChangeNextOwnerCopy     = SavedSettings.getBool("BulkChangeNextOwnerCopy")
        bulkChangeNextOwnerTransfer = SavedSettings.getBool("BulkChangeNextOwnerTransfer")

        // Guard against an invalid permission combination left by MAINT-3339.
        if (!bulkChangeNextOwnerTransfer && !bulkChangeEveryoneCopy) {
            bulkChangeNextOwnerTransfer = true
        }

        queueOutputList = getChild("queue output")
        return true
    }

    override fun onClose(appQuitting: Boolean) {
        removeVOInventoryListener()
        super.onClose(appQuitting)
    }

    private fun onOkBtn() {
        doApply()
        closeFloater()
    }

    private fun onApplyBtn() {
        doApply()
    }

    private fun onCloseBtn() {
        SavedSettings.setBool("BulkChangeIncludeAnimations", bulkChangeIncludeAnimations)
        SavedSettings.setBool("BulkChangeIncludeBodyParts",  bulkChangeIncludeBodyParts)
        SavedSettings.setBool("BulkChangeIncludeClothing",   bulkChangeIncludeClothing)
        SavedSettings.setBool("BulkChangeIncludeGestures",   bulkChangeIncludeGestures)
        SavedSettings.setBool("BulkChangeIncludeNotecards",  bulkChangeIncludeNotecards)
        SavedSettings.setBool("BulkChangeIncludeObjects",    bulkChangeIncludeObjects)
        SavedSettings.setBool("BulkChangeIncludeScripts",    bulkChangeIncludeScripts)
        SavedSettings.setBool("BulkChangeIncludeSounds",     bulkChangeIncludeSounds)
        SavedSettings.setBool("BulkChangeIncludeTextures",   bulkChangeIncludeTextures)
        SavedSettings.setBool("BulkChangeIncludeSettings",   bulkChangeIncludeSettings)
        SavedSettings.setBool("BulkChangeIncludeMaterials",  bulkChangeIncludeMaterials)
        SavedSettings.setBool("BulkChangeShareWithGroup",    bulkChangeShareWithGroup)
        SavedSettings.setBool("BulkChangeEveryoneCopy",      bulkChangeEveryoneCopy)
        SavedSettings.setBool("BulkChangeNextOwnerModify",   bulkChangeNextOwnerModify)
        SavedSettings.setBool("BulkChangeNextOwnerCopy",     bulkChangeNextOwnerCopy)
        SavedSettings.setBool("BulkChangeNextOwnerTransfer", bulkChangeNextOwnerTransfer)
        closeFloater()
    }

    private fun onCommitCopy() {
        // Fair-use rule: if copy is not permitted, transfer must be.
        val copyable = SavedSettings.getBool("BulkChangeNextOwnerCopy")
        if (!copyable) {
            SavedSettings.setBool("BulkChangeNextOwnerTransfer", true)
        }
        getChild<CheckBoxCtrl>("next_owner_transfer").setEnabled(copyable)
    }

    private fun onCheckAll()   { doCheckUncheckAll(true) }
    private fun onUncheckAll() { doCheckUncheckAll(false) }

    private fun doCheckUncheckAll(check: Boolean) {
        SavedSettings.setBool("BulkChangeIncludeAnimations", check)
        SavedSettings.setBool("BulkChangeIncludeBodyParts",  check)
        SavedSettings.setBool("BulkChangeIncludeClothing",   check)
        SavedSettings.setBool("BulkChangeIncludeGestures",   check)
        SavedSettings.setBool("BulkChangeIncludeNotecards",  check)
        SavedSettings.setBool("BulkChangeIncludeObjects",    check)
        SavedSettings.setBool("BulkChangeIncludeScripts",    check)
        SavedSettings.setBool("BulkChangeIncludeSounds",     check)
        SavedSettings.setBool("BulkChangeIncludeTextures",   check)
        SavedSettings.setBool("BulkChangeIncludeSettings",   check)
        SavedSettings.setBool("BulkChangeIncludeMaterials",  check)
    }

    private fun isDone(): Boolean = currentObjectID == null || objectIDs.isEmpty()

    private fun doApply() {
        val list = getChild<ScrollListCtrl>("queue output")
        list.deleteAllItems()

        // Gather all selected objects that the agent is permitted to modify.
        SelectMgr.getInstance().getSelection().applyToNodes { node ->
            if (node.allowOperationOnNode(PermFlags.PERM_MODIFY, GP_OBJECT_MANIPULATE)) {
                objectIDs.add(node.getObject().getID())
            }
            true
        }

        if (objectIDs.isEmpty()) {
            list.setCommentText(getString("nothing_to_modify_text"))
        } else {
            done = false
            start()
        }
    }

    // Kick off processing; returns false if nothing to do.
    private fun start(): Boolean {
        queueOutputList!!.setCommentText(getString("start_text"))
        return nextObject()
    }

    // Advance to the next object in the queue. Returns true while work remains.
    private fun nextObject(): Boolean {
        var successfulStart = false
        do {
            currentObjectID = null
            if (objectIDs.isNotEmpty()) {
                successfulStart = popNext()
            }
        } while (objectIDs.isNotEmpty() && !successfulStart)

        if (isDone() && !done) {
            queueOutputList!!.setCommentText(getString("done_text"))
            done = true
        }
        return successfulStart
    }

    // Dequeue the head object and register for its inventory callback.
    private fun popNext(): Boolean {
        if (currentObjectID != null || objectIDs.isEmpty()) return false
        currentObjectID = objectIDs.removeAt(0)
        val obj = ObjectList.findObject(currentObjectID!!) ?: return false
        registerVOInventoryListener(obj, id)
        requestVOInventory()
        return true
    }

    // Called by the viewer object subsystem when the object's inventory arrives.
    override fun inventoryChanged(
        obj: ViewerObject?,
        inv: MutableList<InventoryObject>?,
        serialNum: Int,
        data: Any?
    ) {
        // Remove the listener first so handleInventory/nextObject do not interact
        // with a stale registered object (fixes SL-6119).
        removeVOInventoryListener()

        if (obj != null && inv != null && obj.getID() == currentObjectID) {
            handleInventory(obj, inv)
        } else {
            nextObject()
        }
    }

    private fun handleInventory(viewerObj: ViewerObject, inv: MutableList<InventoryObject>) {
        for (invObj in inv) {
            val asstype = invObj.getType()
            val include = when (asstype) {
                AssetType.AT_ANIMATION -> bulkChangeIncludeAnimations
                AssetType.AT_BODYPART  -> bulkChangeIncludeBodyParts
                AssetType.AT_CLOTHING  -> bulkChangeIncludeClothing
                AssetType.AT_GESTURE   -> bulkChangeIncludeGestures
                AssetType.AT_NOTECARD  -> bulkChangeIncludeNotecards
                AssetType.AT_OBJECT    -> bulkChangeIncludeObjects
                AssetType.AT_LSL_TEXT  -> bulkChangeIncludeScripts
                AssetType.AT_SOUND     -> bulkChangeIncludeSounds
                AssetType.AT_TEXTURE   -> bulkChangeIncludeTextures
                AssetType.AT_SETTINGS  -> bulkChangeIncludeSettings
                AssetType.AT_MATERIAL  -> bulkChangeIncludeMaterials
                else -> false
            }
            if (!include) continue

            val object_ = ObjectList.findObject(viewerObj.getID()) ?: continue
            val item = invObj as? ViewerInventoryItem ?: continue
            val perm = item.getPermissions().toMutable()

            // AT_SETTINGS always requires copy permission per spec.
            var maskNext = FloaterPerms.getNextOwnerPerms("BulkChange")
            if (asstype == AssetType.AT_SETTINGS) {
                maskNext = maskNext or PermFlags.PERM_COPY
            }
            perm.setMaskNext(maskNext)
            perm.setMaskEveryone(FloaterPerms.getEveryonePerms("BulkChange"))
            perm.setMaskGroup(FloaterPerms.getGroupPerms("BulkChange"))
            item.setPermissions(perm)

            updateInventory(object_, item, TASK_INVENTORY_ITEM_KEY, false)

            val invName = item.getName().take(30)
            val statusText = getString("status_text")
                .replace("[NAME]", invName)
                .replace("[STATUS]", "")
            queueOutputList!!.setCommentText(statusText)
        }

        nextObject()
    }

    // Sends a permission-update message directly to the simulator, bypassing
    // inventory callbacks to avoid iterator invalidation issues.
    private fun updateInventory(obj: ViewerObject, item: ViewerInventoryItem, key: UByte, isNew: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    companion object {
        fun create(seed: Any): FloaterBulkPermission = FloaterBulkPermission(seed)
    }
}
