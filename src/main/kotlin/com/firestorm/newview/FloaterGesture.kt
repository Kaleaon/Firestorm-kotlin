package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.ScrollListCtrl
import com.firestorm.llui.ScrollListItem
import com.firestorm.llui.ScrollListIcon
import com.firestorm.llui.ScrollListText
import com.firestorm.llui.FilterEditor
import com.firestorm.llui.CtrlListInterface
import com.firestorm.llui.FontGL
import com.firestorm.llui.LLSD
import com.firestorm.newview.GestureMgr
import com.firestorm.newview.GestureManagerObserver
import com.firestorm.newview.ViewerGesture
import com.firestorm.newview.MultiGesture
import com.firestorm.inventory.InventoryModel
import com.firestorm.inventory.InventoryItem
import com.firestorm.inventory.ViewerInventoryItem
import com.firestorm.inventory.InventoryFetchDescendentsObserver
import com.firestorm.inventory.FolderType
import com.firestorm.inventory.AssetType
import com.firestorm.inventory.InventoryType
import com.firestorm.agent.Agent
import com.firestorm.appearance.AppearanceMgr
import java.util.UUID

private fun areGesturesEnabled(): Boolean =
    SavedPerAccountSettings.getBool("FSGesturesEnabled", default = true)

private fun itemNamePrecedes(a: InventoryItem, b: InventoryItem): Boolean =
    a.name < b.name

private class FloaterGestureObserver(private val floater: FloaterGesture) : GestureManagerObserver {
    override fun changed() { floater.refreshAll() }
}

private class GestureShowCallback : InventoryCallback {
    override fun fire(invItem: UUID) {
        PreviewGesture.show(invItem, UUID.fromString("00000000-0000-0000-0000-000000000000"))
        val item = gInventory.getItem(invItem) ?: return
        val perm = item.permissions
        perm.setMaskNext(FloaterPerms.getNextOwnerPerms("Gestures"))
        perm.setMaskEveryone(FloaterPerms.getEveryonePerms("Gestures"))
        perm.setMaskGroup(FloaterPerms.getGroupPerms("Gestures"))
        item.setPermissions(perm)
        item.updateServer(false)
    }
}

private class GestureCopiedCallback(private val floater: FloaterGesture?) : InventoryCallback {
    override fun fire(invItem: UUID) {
        floater?.let { f ->
            f.addGesture(invItem, null, f.gestureList)
            f.refreshAll()
        }
    }
}

class FloaterGesture(key: LLSD) : Floater(key), InventoryFetchDescendentsObserver {

    private val mObserver: FloaterGestureObserver = FloaterGestureObserver(this)
    private var mSelectedID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var mGestureFolderID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    internal lateinit var gestureList: ScrollListCtrl
    private lateinit var mFilterEditor: FilterEditor
    private var mFilterSubString: String = ""
    private val mItems: MutableMap<UUID, ScrollListItem> = mutableMapOf()

    init {
        GestureMgr.instance.addObserver(mObserver)
        registerCommitCallback("Gesture.Action.ToggleActiveState") { onActivateBtnClick() }
        registerCommitCallback("Gesture.Action.ShowPreview") { onClickEdit() }
        registerCommitCallback("Gesture.Action.CopyPaste") { cmd -> onCopyPasteAction(cmd) }
        registerCommitCallback("Gesture.Action.SaveToCOF") { addToCurrentOutFit() }
        registerCommitCallback("Gesture.Action.Rename") { onRenameSelected() }
        registerCommitCallback("Gesture.Action.RefreshList") { refreshForActiveSort() }
        registerEnableCallback("Gesture.EnableAction") { cmd -> isActionEnabled(cmd) }
    }

    override fun destroy() {
        GestureMgr.instance.removeObserver(mObserver)
        gInventory.removeObserver(this)
    }

    override fun done() {
        if (gInventory.isCategoryComplete(mGestureFolderID)) {
            val categories = mutableListOf<InventoryModel.Category>()
            val items = mutableListOf<InventoryItem>()
            gInventory.getDirectDescendentsOf(mGestureFolderID, categories, items)

            if (categories.isEmpty()) {
                gInventory.removeObserver(this)
                return
            }

            val unloadedFolders = categories
                .filter { !gInventory.isCategoryComplete(it.uuid) }
                .map { it.uuid }

            if (unloadedFolders.isNotEmpty()) {
                setFetchIDs(unloadedFolders)
                startFetch()
            } else {
                gInventory.removeObserver(this)
                buildGestureList()
            }
        }
    }

    override fun postBuild(): Boolean {
        val label = getTitle()
        setTitle(label)

        gestureList = getChild<ScrollListCtrl>("gesture_list")
        gestureList.setCommitCallback { onCommitList() }
        gestureList.setDoubleClickCallback { onClickPlay() }
        gestureList.setFilterColumn(1)

        mFilterEditor = getChild<FilterEditor>("filter_input")
        mFilterEditor.setCommitCallback { s -> onFilterEdit(s) }

        getChild<UICtrl>("edit_btn").setCommitCallback { onClickEdit() }
        getChild<UICtrl>("play_btn").setCommitCallback { onClickPlay() }
        getChild<UICtrl>("stop_btn").setCommitCallback { onClickPlay() }
        getChild<Button>("activate_btn").setClickedCallback { onActivateBtnClick() }
        getChild<UICtrl>("FSShowOnlyActiveGestures").setCommitCallback { refreshForActiveSort() }
        getChild<UICtrl>("FSGesturesEnabled").setCommitCallback { onGesturesEnabledChanged() }
        getChild<UICtrl>("new_gesture_btn").setCommitCallback { onClickNew() }
        getChild<Button>("del_btn").setClickedCallback { onDeleteSelected() }

        getChildView("play_btn").setVisible(true)
        getChildView("stop_btn").setVisible(false)
        setDefaultBtn("play_btn")

        mGestureFolderID = gInventory.findCategoryUUIDForType(FolderType.FT_GESTURE)

        gInventory.addObserver(this)
        setFetchIDs(listOf(mGestureFolderID))
        startFetch()

        buildGestureList()
        gestureList.setFocus(true)
        gestureList.sortByColumn("name", ascending = true)
        gestureList.selectFirstItem()

        onCommitList()
        updateGesturesEnabledState()

        return true
    }

    fun refreshForActiveSort() {
        if (SavedPerAccountSettings.getBool("FSShowOnlyActiveGestures")) {
            mItems.clear()
            gestureList.deleteAllItems()
        }
        refreshAll()
    }

    fun refreshAll() {
        buildGestureList()
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (mSelectedID == nullId) {
            gestureList.selectFirstItem()
        } else {
            if (!gestureList.setCurrentByID(mSelectedID)) {
                gestureList.selectFirstItem()
            }
        }
        onCommitList()
    }

    private fun buildGestureList() {
        val scrollPos = gestureList.getScrollPos()
        val selectedItems = mutableListOf<UUID>()
        getSelectedIds(selectedItems)

        val addedItems = mutableSetOf<UUID>()
        val activeGestures = GestureMgr.instance.getActiveGestures()

        for ((id, gesture) in activeGestures) {
            addGesture(id, gesture, gestureList)
            addedItems.add(id)
        }

        if (gInventory.isCategoryComplete(mGestureFolderID) &&
            !SavedPerAccountSettings.getBool("FSShowOnlyActiveGestures")
        ) {
            val categories = mutableListOf<InventoryModel.Category>()
            val items = mutableListOf<InventoryItem>()
            gInventory.collectDescendentsIf(
                mGestureFolderID, categories, items,
                InventoryModel.EXCLUDE_TRASH, IsTypeGesture()
            )
            for (item in items) {
                if (!activeGestures.containsKey(item.uuid)) {
                    addGesture(item.uuid, null, gestureList)
                    addedItems.add(item.uuid)
                }
            }
        }

        val newItems = mutableMapOf<UUID, ScrollListItem>()
        for ((id, slItem) in mItems) {
            if (id !in addedItems) {
                gestureList.deleteItems(id)
            } else {
                newItems[id] = slItem
            }
        }
        mItems.clear()
        mItems.putAll(newItems)

        selectedItems.forEach { gestureList.selectByID(it) }
        gestureList.setScrollPos(scrollPos)
    }

    fun addGesture(itemId: UUID, gesture: MultiGesture?, list: CtrlListInterface) {
        var itemName = gInventory.getItem(itemId)?.name ?: getString("loading")

        val element = LLSD()
        element["id"] = itemId

        if (gesture != null) {
            var fontStyle = "NORMAL"
            element["columns"][0]["column"] = "active"
            element["columns"][0]["type"] = "icon"
            if (gesture.mPlaying) {
                fontStyle = "BOLD"
                element["columns"][0]["value"] = "Activate_Checkmark"
            }
            itemName = gesture.mName
            if (gInventory.getItem(itemId) != null && gesture.mPlaying) {
                itemName += " " + getString("playing")
            }
            element["columns"][1]["column"] = "name"
            element["columns"][1]["value"] = itemName
            element["columns"][1]["font"]["name"] = "SANSSERIF"
            element["columns"][1]["font"]["style"] = fontStyle

            element["columns"][2]["column"] = "trigger"
            element["columns"][2]["value"] = gesture.mTrigger
            element["columns"][2]["font"]["name"] = "SANSSERIF"
            element["columns"][2]["font"]["style"] = fontStyle

            val (keyString, buffer) = if (gesture.mKey == KEY_NONE) {
                "~~~" to "---"
            } else {
                Keyboard.stringFromKey(gesture.mKey) to
                    Keyboard.stringFromAccelerator(gesture.mMask, gesture.mKey)
            }

            element["columns"][3]["column"] = "key"
            element["columns"][3]["value"] = keyString
            element["columns"][3]["font"]["name"] = "SANSSERIF"
            element["columns"][3]["font"]["style"] = fontStyle

            element["columns"][4]["column"] = "shortcut"
            element["columns"][4]["value"] = buffer
            element["columns"][4]["font"]["name"] = "SANSSERIF"
            element["columns"][4]["font"]["style"] = fontStyle
        } else {
            element["columns"][0]["column"] = "active"
            element["columns"][0]["type"] = "icon"
            element["columns"][0]["value"] = ""
            element["columns"][1]["column"] = "name"
            element["columns"][1]["value"] = itemName
            element["columns"][1]["font"]["name"] = "SANSSERIF"
            element["columns"][1]["font"]["style"] = "NORMAL"
            element["columns"][2]["column"] = "trigger"
            element["columns"][2]["value"] = ""
            element["columns"][2]["font"]["name"] = "SANSSERIF"
            element["columns"][2]["font"]["style"] = "NORMAL"
            element["columns"][3]["column"] = "key"
            element["columns"][3]["value"] = "~~~"
            element["columns"][3]["font"]["name"] = "SANSSERIF"
            element["columns"][3]["font"]["style"] = "NORMAL"
            element["columns"][4]["column"] = "shortcut"
            element["columns"][4]["value"] = "---"
            element["columns"][4]["font"]["name"] = "SANSSERIF"
            element["columns"][4]["font"]["style"] = "NORMAL"
        }

        val slItem: ScrollListItem? = if (!updateItem(itemId, element)) {
            list.addElement(element, ADD_BOTTOM)
        } else null

        if (slItem != null) {
            mItems[itemId] = slItem
            val isActive = GestureMgr.instance.isGestureActive(itemId)
            (slItem.getColumn(COL_ACTIVE) as? ScrollListIcon)?.apply {
                setValue(if (isActive) "Activate_Checkmark" else "")
                if (isActive) setIconSize(10)
            }
            (slItem.getColumn(COL_NAME) as? ScrollListText)?.setFontStyle(
                if (isActive) FontGL.BOLD else FontGL.NORMAL
            )
        }
    }

    private fun getSelectedIds(ids: MutableList<UUID>) {
        gestureList.getAllSelected().forEach { ids.add(it.getUUID()) }
    }

    private fun isActionEnabled(command: LLSD): Boolean {
        if (!areGesturesEnabled()) return false

        return when (command.asString()) {
            "paste" -> {
                if (!Clipboard.instance.hasContents()) return false
                val ids = mutableListOf<UUID>()
                Clipboard.instance.pasteFromClipboard(ids)
                ids.any { gInventory.getItem(it)?.inventoryType == InventoryType.IT_GESTURE }
            }
            "copy_uuid", "edit_gesture" -> gestureList.getAllSelected().size == 1
            "rename_gesture" -> {
                if (gestureList.getAllSelected().size == 1) {
                    val item = gInventory.getItem(gestureList.getCurrentID())
                    item?.permissions?.allowModifyBy(gAgentID) == true
                } else false
            }
            else -> true
        }
    }

    private fun onClickPlay() {
        if (!areGesturesEnabled()) return
        val itemId = gestureList.getCurrentID()
        if (itemId == UUID.fromString("00000000-0000-0000-0000-000000000000")) return

        if (!GestureMgr.instance.isGestureActive(itemId)) {
            GestureMgr.instance.setGestureLoadedCallback(itemId) { playGesture(itemId) }
            val item = gInventory.getItem(itemId) ?: return
            GestureMgr.instance.activateGestureWithAsset(itemId, item.assetUUID, informServer = true, deactivateSimilar = false)
        } else {
            playGesture(itemId)
        }
    }

    private fun onClickNew() {
        if (!areGesturesEnabled()) return
        val cb = GestureShowCallback()
        createInventoryItem(
            gAgent.id, gAgent.sessionID,
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            TransactionID.tnull,
            "New Gesture", "",
            AssetType.AT_GESTURE, InventoryType.IT_GESTURE,
            NO_INV_SUBTYPE,
            PERM_MOVE or FloaterPerms.getNextOwnerPerms("Gestures"),
            cb
        )
    }

    private fun onActivateBtnClick() {
        if (!areGesturesEnabled()) return
        val ids = mutableListOf<UUID>()
        getSelectedIds(ids)
        if (ids.isEmpty()) return

        val gm = GestureMgr.instance
        val firstState = gm.isGestureActive(ids[0])
        val isMixed = ids.drop(1).any { gm.isGestureActive(it) != firstState }

        for (id in ids) {
            if (isMixed) {
                gm.activateGesture(id)
            } else {
                if (firstState) gm.deactivateGesture(id) else gm.activateGesture(id)
            }
        }
    }

    private fun onRenameSelected() {
        val gesture = gInventory.getItem(gestureList.getCurrentID()) ?: return
        val args = LLSD()
        args["NAME"] = gesture.name
        val payload = LLSD()
        payload["gesture_id"] = gestureList.getCurrentID()
        NotificationsUtil.add("RenameGesture", args, payload) { n, r -> onGestureRename(n, r) }
    }

    private fun onCopyPasteAction(command: LLSD) {
        if (!areGesturesEnabled()) return
        when (command.asString()) {
            "copy_gesture" -> {
                val ids = mutableListOf<UUID>()
                getSelectedIds(ids)
                Clipboard.instance.reset()
                ids.filter { gInventory.getItem(it)?.inventoryType == InventoryType.IT_GESTURE }
                   .forEach { Clipboard.instance.addToClipboard(it) }
            }
            "paste" -> {
                val ids = mutableListOf<UUID>()
                Clipboard.instance.pasteFromClipboard(ids)
                if (ids.isEmpty() || !gInventory.isCategoryComplete(mGestureFolderID)) return
                val gestureDir = gInventory.getCategory(mGestureFolderID) ?: return
                val cb = GestureCopiedCallback(this)
                for (id in ids) {
                    val item = gInventory.getItem(id) ?: continue
                    if (item.inventoryType == InventoryType.IT_GESTURE) {
                        val copyName = getString("copy_name", mapOf("COPY_NAME" to item.name))
                        copyInventoryItem(gAgent.id, item.permissions.owner, item.uuid, gestureDir.uuid, copyName, cb)
                    }
                }
                Clipboard.instance.reset()
            }
            "copy_uuid" -> Clipboard.instance.copyToClipboard(gestureList.getCurrentID(), AssetType.AT_GESTURE)
        }
    }

    private fun onClickEdit() {
        if (!areGesturesEnabled()) return
        val itemId = gestureList.getCurrentID()
        val item = gInventory.getItem(itemId) ?: return
        val previewp = PreviewGesture.show(itemId, UUID.fromString("00000000-0000-0000-0000-000000000000"))
        if (previewp.getHost() == null) {
            previewp.setRect(gFloaterView.findNeighboringPosition(this, previewp))
        }
    }

    private fun onCommitList() {
        val itemId = gestureList.getCurrentID()
        mSelectedID = itemId
        val isPlaying = GestureMgr.instance.isGesturePlaying(itemId)
        getChildView("play_btn").setVisible(!isPlaying)
        getChildView("stop_btn").setVisible(isPlaying)
        updateGesturesEnabledState()
    }

    private fun onGesturesEnabledChanged() {
        updateGesturesEnabledState()
    }

    private fun updateGesturesEnabledState() {
        val enabled = areGesturesEnabled()
        gestureList.setEnabled(enabled)
        listOf("edit_btn", "play_btn", "stop_btn", "new_gesture_btn", "del_btn",
               "activate_btn", "filter_input", "gear_btn", "FSShowOnlyActiveGestures")
            .forEach { getChildView(it).setEnabled(enabled) }
    }

    private fun onFilterEdit(searchString: String) {
        val filterUpper = searchString.trimStart().uppercase()
        if (mFilterSubString == filterUpper) return
        mFilterSubString = filterUpper
        gestureList.setFilterString(searchString)
    }

    private fun onDeleteSelected() {
        if (!areGesturesEnabled()) return
        val ids = mutableListOf<UUID>()
        getSelectedIds(ids)
        if (ids.isEmpty()) return

        val trashId = gInventory.findCategoryUUIDForType(FolderType.FT_TRASH)
        val gm = GestureMgr.instance
        for (selectedItem in ids) {
            val invItem = gInventory.getItem(selectedItem) ?: continue
            if (invItem.inventoryType != InventoryType.IT_GESTURE) continue
            if (gm.isGestureActive(selectedItem)) gm.deactivateGesture(selectedItem)

            val update = mutableListOf<InventoryModel.CategoryUpdate>()
            update.add(InventoryModel.CategoryUpdate(invItem.parentUUID, -1))
            update.add(InventoryModel.CategoryUpdate(trashId, 1))
            gInventory.accountForUpdate(update)

            val newItem = ViewerInventoryItem(invItem)
            newItem.setParent(trashId)
            newItem.updateParentOnServer(false)
            gInventory.updateItem(newItem)
        }
        gInventory.notifyObservers()
        buildGestureList()
    }

    private fun addToCurrentOutFit() {
        val ids = mutableListOf<UUID>()
        getSelectedIds(ids)
        val am = AppearanceMgr.instance
        val cb = UpdateAppearanceOnDestroy()
        ids.forEach { am.addCOFItemLink(it, cb) }
    }

    private fun playGesture(itemId: UUID) {
        if (GestureMgr.instance.isGesturePlaying(itemId)) {
            GestureMgr.instance.stopGesture(itemId)
        } else {
            GestureMgr.instance.playGesture(itemId)
        }
    }

    private fun updateItem(itemId: UUID, data: LLSD): Boolean {
        val pItem = mItems[itemId] ?: return false

        if (data["columns"][COL_NAME]["value"].asString().isNotEmpty())
            pItem.getColumn(UI_COL_NAME).setValue(data["columns"][COL_NAME]["value"])

        if (data["columns"][COL_TRIGGER]["value"].asString().isNotEmpty())
            pItem.getColumn(UI_COL_TRIGGER).setValue(data["columns"][COL_TRIGGER]["value"])

        if (data["columns"][COL_SHORTCUT]["value"].asString() != "---")
            pItem.getColumn(UI_COL_SHORTCUT).setValue(data["columns"][COL_SHORTCUT]["value"])

        if (data["columns"][COL_KEY]["value"].asString() != "~~~")
            pItem.getColumn(UI_COL_KEY).setValue(data["columns"][COL_KEY]["value"])

        pItem.getColumn(UI_COL_NAME).setValue(data["columns"][COL_NAME]["value"])

        val isActive = GestureMgr.instance.isGestureActive(itemId)
        val style = if (isActive) FontGL.BOLD else FontGL.NORMAL

        pItem.getColumn(UI_COL_ACTIVE).setValue(if (isActive) "Activate_Checkmark" else "")
        val icon = pItem.getColumn(UI_COL_ACTIVE) as? ScrollListIcon
        if (isActive && icon != null) icon.setIconSize(10)

        (pItem.getColumn(UI_COL_NAME) as? ScrollListText)?.setFontStyle(style)

        return true
    }

    companion object {
        private const val COL_ACTIVE = 0
        private const val COL_NAME = 1
        private const val COL_TRIGGER = 2
        private const val COL_KEY = 3
        private const val COL_SHORTCUT = 4

        private const val UI_COL_ACTIVE = 0
        private const val UI_COL_NAME = 1
        private const val UI_COL_TRIGGER = 2
        private const val UI_COL_KEY = 3
        private const val UI_COL_SHORTCUT = 4

        private const val KEY_NONE = 0
        private const val ADD_BOTTOM = 1

        fun onGestureRename(notification: LLSD, response: LLSD) {
            val option = NotificationsUtil.getSelectedOption(notification, response)
            if (option != 0) return

            var newName = response["new_name"].asString()
            InventoryObject.correctInventoryName(newName)
            if (newName.isNotEmpty()) {
                val itemId = notification["payload"]["gesture_id"].asUUID()
                val gesture = gInventory.getItem(itemId) ?: return
                if (gesture.name != newName) {
                    val updates = LLSD()
                    updates["name"] = newName
                    updateInventoryItem(itemId, updates, null)
                }
            }
        }
    }
}
