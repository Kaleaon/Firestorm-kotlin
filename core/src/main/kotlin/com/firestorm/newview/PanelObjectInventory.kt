package com.firestorm.newview

import com.firestorm.inventory.AssetType
import com.firestorm.inventory.FolderType
import com.firestorm.inventory.InventoryAction
import com.firestorm.inventory.InventoryFilter
import com.firestorm.inventory.InventoryItem
import com.firestorm.inventory.InventoryModel
import com.firestorm.inventory.InventoryObject
import com.firestorm.inventory.InventoryType
import com.firestorm.inventory.SettingsType
import com.firestorm.inventory.WearableType
import com.firestorm.ui.FolderView
import com.firestorm.ui.FolderViewFolder
import com.firestorm.ui.FolderViewItem
import com.firestorm.ui.FolderViewModelInventory
import com.firestorm.ui.FolderViewModelItemInventory
import com.firestorm.ui.Panel
import com.firestorm.ui.ScrollContainer
import com.firestorm.ui.UIImage
import com.firestorm.viewer.Agent
import com.firestorm.viewer.SelectMgr
import com.firestorm.viewer.ViewerInventoryItem
import com.firestorm.viewer.ViewerObject
import com.firestorm.viewer.ViewerObjectList
import java.util.UUID

// ─── Bridge hierarchy ──────────────────────────────────────────────────────────

open class TaskInvFVBridge(
    protected val panel: PanelObjectInventory,
    protected val uuid: UUID,
    protected val name: String,
    protected val flags: UInt = 0u
) : FolderViewModelItemInventory(panel.rootViewModel) {

    protected var assetType: AssetType = AssetType.AT_NONE
    protected var inventoryType: InventoryType = InventoryType.IT_NONE

    init {
        findItem()?.let {
            assetType = it.type
            inventoryType = it.inventoryType
        }
    }

    protected fun findInvObject(): InventoryObject? {
        val obj = ViewerObjectList.findObject(panel.taskUUID) ?: return null
        return obj.getInventoryObject(uuid)
    }

    protected fun findItem(): InventoryItem? = findInvObject() as? InventoryItem

    fun showProperties() {
        showTaskItemProfile(uuid, panel.taskUUID)
    }

    fun getPrice(): Int = findItem()?.saleInfo?.salePrice ?: -1

    open fun getName(): String = name

    open fun getDisplayName(): String {
        val item = findItem() ?: return name
        var display = item.name

        if (item.type == AssetType.AT_LSL_TEXT && item.name.startsWith("New Script")) {
            display = display.replace("New Script", Trans.getString("PanelContentsNewScript"))
        }

        val perm = item.permissions
        val copy = Agent.allowOperation(PermType.COPY, perm)
        val mod  = Agent.allowOperation(PermType.MODIFY, perm)
        val xfer = Agent.allowOperation(PermType.TRANSFER, perm)

        if (!copy) display += Trans.getString("no_copy")
        if (!mod)  display += Trans.getString("no_modify")
        if (!xfer) display += Trans.getString("no_transfer")

        mSearchableName = (display + getLabelSuffix()).uppercase()
        mDisplayName = display
        return mDisplayName
    }

    private var mDisplayName: String = ""
    private var mSearchableName: String = ""

    open fun getSearchableName(): String = mSearchableName
    open fun getLabelSuffix(): String = ""

    open fun getCreationDate(): Long = 0L
    open fun setCreationDate(creationDateUtc: Long) {}

    open fun getIcon(): UIImage? {
        val isMulti = (flags and InventoryItemFlags.II_FLAGS_OBJECT_HAS_MULTIPLE_ITEMS) != 0u
        return InventoryIcon.getIcon(assetType, inventoryType, 0u, isMulti)
    }

    open fun canOpenItem(): Boolean = false
    open fun openItem() {}
    open fun closeItem() {}
    open fun selectItem() {}

    open fun isItemRenameable(): Boolean {
        if (Agent.isGodlike()) return true
        val obj = ViewerObjectList.findObject(panel.taskUUID) ?: return false
        val item = obj.getInventoryObject(uuid) as? InventoryItem ?: return false
        return Agent.allowOperation(PermType.MODIFY, item.permissions)
    }

    open fun renameItem(newName: String): Boolean {
        val obj = ViewerObjectList.findObject(panel.taskUUID) ?: return true
        val item = obj.getInventoryObject(uuid) as? ViewerInventoryItem ?: return true
        if (Agent.allowOperation(PermType.MODIFY, item.permissions)) {
            val newItem = ViewerInventoryItem(item)
            newItem.rename(newName)
            obj.updateInventory(newItem, TASK_INVENTORY_ITEM_KEY, false)
        }
        return true
    }

    open fun isItemMovable(): Boolean = true

    open fun isItemRemovable(checkWorn: Boolean = true): Boolean {
        val obj = ViewerObjectList.findObject(panel.taskUUID) ?: return false
        return obj.permModify() || obj.permYouOwner()
    }

    open fun removeItem(): Boolean {
        if (!isItemRemovable() || panel == null) return false
        val obj = ViewerObjectList.findObject(panel.taskUUID) ?: return false
        return if (obj.permModify()) {
            obj.removeInventory(uuid)
            true
        } else {
            NotificationsUtil.add("CantModifyContentInNoModTask")
            false
        }
    }

    open fun removeBatch(batch: MutableList<FolderViewModelItem>) {
        val obj = ViewerObjectList.findObject(panel.taskUUID) ?: return
        if (!obj.permModify()) {
            NotificationsUtil.add("CantModifyContentInNoModTask")
            return
        }
        for (item in batch) {
            val bridge = item as? TaskInvFVBridge ?: continue
            if (bridge.isItemRemovable()) {
                obj.removeInventory(bridge.uuid)
            }
        }
    }

    open fun move(parentListener: FolderViewModelItem) {}

    open fun isItemCopyable(canLink: Boolean = true): Boolean {
        val item = findItem() ?: return false
        return Agent.allowOperation(PermType.COPY, item.permissions)
    }

    open fun copyToClipboard(): Boolean = false
    open fun cutToClipboard(): Boolean = false
    open fun isClipboardPasteable(): Boolean = false
    open fun pasteFromClipboard() {}
    open fun pasteLinkFromClipboard() {}

    open fun startDrag(type: Array<DragAndDropType>, id: Array<UUID>): Boolean {
        val obj = ViewerObjectList.findObject(panel.taskUUID) ?: return false
        val inv = obj.getInventoryObject(uuid) as? InventoryItem ?: return false
        val perm = inv.permissions
        val canCopy = Agent.allowOperation(PermType.COPY, perm)
        if (!canCopy && obj.isAttachment()) return false
        return if ((canCopy && perm.allowTransferTo(Agent.id)) || obj.permYouOwner()) {
            type[0] = ViewerAssetType.lookupDragAndDropType(inv.type)
            id[0] = inv.uuid
            true
        } else false
    }

    open fun dragOrDrop(
        mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        tooltipMsg: StringBuilder
    ): Boolean = false

    open fun performAction(model: InventoryModel, action: String) {
        when (action) {
            "task_open"       -> openItem()
            "task_properties" -> showProperties()
        }
    }

    open fun buildContextMenu(menu: MenuGL, flags: UInt) {
        val item = findItem() ?: run { hideContextEntries(menu, emptyList(), emptyList()); return }
        val items = mutableListOf<String>()
        val disabled = mutableListOf<String>()

        if (canOpenItem()) items.add("Task Open")
        items.add("Task Properties")
        items.add("Task Rename")
        if (!isItemRenameable() || (flags and FIRST_SELECTED_ITEM) == 0u)
            disabled.add("Task Rename")
        items.add("Task Remove")
        if (!isItemRemovable()) disabled.add("Task Remove")

        hideContextEntries(menu, items, disabled)
    }

    open fun isUpToDate(): Boolean = true
    open fun hasChildren(): Boolean = false
    open fun getInventoryType(): InventoryType = InventoryType.IT_NONE
    open fun getWearableType(): WearableType = WearableType.WT_NONE
    open fun getSettingsType(): SettingsType = SettingsType.ST_NONE
    open fun getSortGroup(): InventorySortGroup = InventorySortGroup.SG_ITEM
    open fun getInventoryObject(): InventoryObject? = findInvObject()
    open fun getUUID(): UUID = uuid
    open fun getPermissionMask(): Int = PERM_NONE
    open fun getPreferredType(): FolderType = FolderType.FT_NONE

    companion object {
        fun createObjectBridge(panel: PanelObjectInventory, obj: InventoryObject?): TaskInvFVBridge? {
            val item = obj as? InventoryItem
            val itemFlags = item?.flags ?: 0u
            val type = obj?.type ?: AssetType.AT_CATEGORY
            val id   = obj?.uuid ?: UUID.randomUUID()
            val objName = obj?.name ?: ""

            return when (type) {
                AssetType.AT_TEXTURE     -> TaskTextureBridge(panel, id, objName)
                AssetType.AT_SOUND       -> TaskSoundBridge(panel, id, objName)
                AssetType.AT_LANDMARK    -> TaskLandmarkBridge(panel, id, objName)
                AssetType.AT_CALLINGCARD -> TaskCallingCardBridge(panel, id, objName)
                AssetType.AT_OBJECT      -> TaskObjectBridge(panel, id, objName, itemFlags)
                AssetType.AT_NOTECARD    -> TaskNotecardBridge(panel, id, objName)
                AssetType.AT_ANIMATION   -> TaskAnimationBridge(panel, id, objName)
                AssetType.AT_GESTURE     -> TaskGestureBridge(panel, id, objName)
                AssetType.AT_CLOTHING,
                AssetType.AT_BODYPART    -> TaskWearableBridge(panel, id, objName, itemFlags)
                AssetType.AT_CATEGORY    -> TaskCategoryBridge(panel, id, objName)
                AssetType.AT_LSL_TEXT    -> TaskLSLBridge(panel, id, objName)
                AssetType.AT_SETTINGS    -> TaskSettingsBridge(panel, id, objName, itemFlags)
                AssetType.AT_MATERIAL    -> TaskMaterialBridge(panel, id, objName)
                else -> null
            }
        }
    }
}

// ─── Category bridge ───────────────────────────────────────────────────────────

class TaskCategoryBridge(
    panel: PanelObjectInventory,
    uuid: UUID,
    name: String
) : TaskInvFVBridge(panel, uuid, name) {

    override fun getIcon(): UIImage? = LLUI.getUIImage("Inv_FolderClosed")

    override fun getDisplayName(): String {
        val cat = findInvObject() ?: return mDisplayName
        var displayName = cat.name

        if (cat.parentUUID == null && cat.name == "Contents") {
            val obj = ViewerObjectList.findObject(panel.taskUUID)
            displayName = if (obj != null) {
                val contents = mutableListOf<InventoryObject>()
                obj.getInventoryContents(contents)
                val n = contents.size
                val key = when {
                    n == 0 -> "FSObjectInventoryNoElements"
                    n == 1 -> "FSObjectInventoryOneElement"
                    else   -> "FSObjectInventoryElements"
                }
                val args = mapOf("NUM_ELEMENTS" to n)
                "${Trans.getString("Contents")} (${Trans.getString(key, args)})"
            } else {
                Trans.getString("Contents")
            }
        }

        mDisplayName = displayName
        mSearchableName = displayName.uppercase()
        return mDisplayName
    }

    private var mDisplayName: String = ""
    private var mSearchableName: String = ""

    override fun isItemRenameable(): Boolean = false
    override fun renameItem(newName: String): Boolean = false
    override fun isItemRemovable(checkWorn: Boolean): Boolean = false
    override fun buildContextMenu(menu: MenuGL, flags: UInt) {
        hideContextEntries(menu, emptyList(), emptyList())
    }
    override fun hasChildren(): Boolean = false
    override fun canOpenItem(): Boolean = true
    override fun openItem() {}
    override fun getSortGroup(): InventorySortGroup = InventorySortGroup.SG_NORMAL_FOLDER

    override fun startDrag(type: Array<DragAndDropType>, id: Array<UUID>): Boolean {
        if (uuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) return false
        val obj = ViewerObjectList.findObject(panel.taskUUID) ?: return false
        val cat = obj.getInventoryObject(uuid) ?: return false
        return if (moveInvCategoryWorldToAgent(uuid, null, false)) {
            type[0] = ViewerAssetType.lookupDragAndDropType(cat.type)
            id[0] = uuid
            true
        } else false
    }

    override fun dragOrDrop(
        mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        tooltipMsg: StringBuilder
    ): Boolean {
        val obj = ViewerObjectList.findObject(panel.taskUUID) ?: return false
        return when (cargoType) {
            DragAndDropType.DAD_CATEGORY -> {
                ToolDragAndDrop.instance.dadUpdateInventoryCategory(obj, drop)
            }
            DragAndDropType.DAD_TEXTURE,
            DragAndDropType.DAD_SOUND,
            DragAndDropType.DAD_LANDMARK,
            DragAndDropType.DAD_OBJECT,
            DragAndDropType.DAD_NOTECARD,
            DragAndDropType.DAD_CLOTHING,
            DragAndDropType.DAD_BODYPART,
            DragAndDropType.DAD_ANIMATION,
            DragAndDropType.DAD_GESTURE,
            DragAndDropType.DAD_CALLINGCARD,
            DragAndDropType.DAD_MESH,
            DragAndDropType.DAD_SETTINGS,
            DragAndDropType.DAD_MATERIAL -> {
                val viewerItem = cargoData as? ViewerInventoryItem ?: return false
                val accept = ToolDragAndDrop.isInventoryDropAcceptable(obj, viewerItem)
                if (accept && drop) ToolDragAndDrop.dropInventory(obj, viewerItem,
                    ToolDragAndDrop.instance.source, ToolDragAndDrop.instance.sourceId)
                accept
            }
            DragAndDropType.DAD_SCRIPT -> {
                val viewerItem = cargoData as? ViewerInventoryItem ?: return false
                val accept = ToolDragAndDrop.isInventoryDropAcceptable(obj, viewerItem)
                    && ToolDragAndDrop.instance.source != ToolDragAndDrop.Source.SOURCE_WORLD
                    && ToolDragAndDrop.instance.source != ToolDragAndDrop.Source.SOURCE_NOTECARD
                if (accept && drop) {
                    val active = (mask and MASK_CONTROL) == 0
                    ToolDragAndDrop.dropScript(obj, viewerItem, active,
                        ToolDragAndDrop.instance.source, ToolDragAndDrop.instance.sourceId)
                }
                accept
            }
            else -> false
        }
    }
}

// ─── Typed item bridges ────────────────────────────────────────────────────────

class TaskTextureBridge(panel: PanelObjectInventory, uuid: UUID, name: String) :
    TaskInvFVBridge(panel, uuid, name) {
    override fun canOpenItem(): Boolean = true
    override fun openItem() {
        val obj = ViewerObjectList.findObject(panel.taskUUID)
        if (obj == null || obj.isInventoryPending()) return
        val preview = FloaterReg.showTypedInstance<PreviewTexture>("preview_texture", uuid)
        preview?.let {
            findItem()?.let { item -> it.setAuxItem(item) }
            it.setObjectId(panel.taskUUID)
        }
    }
}

class TaskSoundBridge(panel: PanelObjectInventory, uuid: UUID, name: String) :
    TaskInvFVBridge(panel, uuid, name) {
    override fun canOpenItem(): Boolean = true
    override fun openItem() {
        val obj = ViewerObjectList.findObject(panel.taskUUID)
        if (obj == null || obj.isInventoryPending()) return
        openSoundPreview(this)
    }
    override fun performAction(model: InventoryModel, action: String) {
        if (action == "task_play") {
            findItem()?.let { sendSoundTrigger(it.assetUUID, 1.0f) }
        }
        super.performAction(model, action)
    }
    override fun buildContextMenu(menu: MenuGL, flags: UInt) {
        val item = findItem() ?: run { hideContextEntries(menu, emptyList(), emptyList()); return }
        val items = mutableListOf<String>()
        val disabled = mutableListOf<String>()
        if (canOpenItem() && !isItemCopyable()) disabled.add("Task Open")
        items.add("Task Properties")
        if (isItemRenameable()) items.add("Task Rename")
        if (isItemRemovable()) items.add("Task Remove")
        items.add("Task Play")
        hideContextEntries(menu, items, disabled)
    }
    companion object {
        fun openSoundPreview(bridge: TaskSoundBridge) {
            val preview = FloaterReg.showTypedInstance<PreviewSound>("preview_sound", bridge.uuid)
            preview?.setObjectId(bridge.panel.taskUUID)
        }
    }
}

class TaskLandmarkBridge(panel: PanelObjectInventory, uuid: UUID, name: String) :
    TaskInvFVBridge(panel, uuid, name)

class TaskCallingCardBridge(panel: PanelObjectInventory, uuid: UUID, name: String) :
    TaskInvFVBridge(panel, uuid, name) {
    override fun isItemRenameable(): Boolean = false
    override fun renameItem(newName: String): Boolean = false
}

open class TaskScriptBridge(panel: PanelObjectInventory, uuid: UUID, name: String) :
    TaskInvFVBridge(panel, uuid, name)

class TaskLSLBridge(panel: PanelObjectInventory, uuid: UUID, name: String) :
    TaskScriptBridge(panel, uuid, name) {
    override fun canOpenItem(): Boolean = true
    override fun openItem() {
        val obj = ViewerObjectList.findObject(panel.taskUUID)
        if (obj == null || obj.isInventoryPending()) return
        if (obj.permModify() || Agent.isGodlike()) {
            val key = mapOf("taskid" to panel.taskUUID, "itemid" to uuid)
            val preview = FloaterReg.showTypedInstance<LiveLSLEditor>("preview_scriptedit", key)
            preview?.let {
                val node = SelectMgr.instance.selection.firstRootNode
                if (node?.isValid == true) it.setObjectName(node.name)
                it.setObjectId(panel.taskUUID)
            }
        } else {
            NotificationsUtil.add("CannotOpenScriptObjectNoMod")
        }
    }
    override fun removeItem(): Boolean {
        FloaterReg.hideInstance("preview_scriptedit", uuid)
        return super.removeItem()
    }
}

class TaskObjectBridge(panel: PanelObjectInventory, uuid: UUID, name: String, flags: UInt = 0u) :
    TaskInvFVBridge(panel, uuid, name, flags)

class TaskNotecardBridge(panel: PanelObjectInventory, uuid: UUID, name: String) :
    TaskInvFVBridge(panel, uuid, name) {
    override fun canOpenItem(): Boolean = true
    override fun openItem() {
        val obj = ViewerObjectList.findObject(panel.taskUUID)
        if (obj == null || obj.isInventoryPending()) return
        val item = obj.getInventoryObject(uuid) as? InventoryItem
        val itemCopy = item != null && Agent.allowOperation(PermType.COPY, item.permissions)
        if (itemCopy || obj.permModify() || Agent.isGodlike()) {
            val key = mapOf("taskid" to panel.taskUUID, "itemid" to uuid)
            val preview = FloaterReg.showTypedInstance<PreviewNotecard>("preview_notecard", key)
            preview?.setObjectId(panel.taskUUID)
        }
    }
    override fun removeItem(): Boolean {
        FloaterReg.hideInstance("preview_notecard", uuid)
        return super.removeItem()
    }
}

class TaskGestureBridge(panel: PanelObjectInventory, uuid: UUID, name: String) :
    TaskInvFVBridge(panel, uuid, name) {
    override fun canOpenItem(): Boolean = true
    override fun openItem() {
        val obj = ViewerObjectList.findObject(panel.taskUUID)
        if (obj == null || obj.isInventoryPending()) return
        PreviewGesture.show(uuid, panel.taskUUID)
    }
    override fun removeItem(): Boolean {
        FloaterReg.hideInstance("preview_gesture", uuid)
        return super.removeItem()
    }
}

class TaskAnimationBridge(panel: PanelObjectInventory, uuid: UUID, name: String) :
    TaskInvFVBridge(panel, uuid, name) {
    override fun canOpenItem(): Boolean = true
    override fun openItem() {
        val obj = ViewerObjectList.findObject(panel.taskUUID)
        if (obj == null || obj.isInventoryPending()) return
        val preview = FloaterReg.showTypedInstance<PreviewAnim>("preview_anim", uuid)
        if (preview != null && (obj.permModify() || Agent.isGodlike())) {
            preview.setObjectId(panel.taskUUID)
        }
    }
    override fun removeItem(): Boolean {
        FloaterReg.hideInstance("preview_anim", uuid)
        return super.removeItem()
    }
}

class TaskWearableBridge(panel: PanelObjectInventory, uuid: UUID, name: String, flags: UInt) :
    TaskInvFVBridge(panel, uuid, name, flags) {
    override fun getIcon(): UIImage? =
        InventoryIcon.getIcon(assetType, inventoryType, flags, false)
}

class TaskSettingsBridge(panel: PanelObjectInventory, uuid: UUID, name: String, flags: UInt) :
    TaskInvFVBridge(panel, uuid, name, flags) {
    override fun getIcon(): UIImage? =
        InventoryIcon.getIcon(assetType, inventoryType, flags, false)
    override fun getSettingsType(): SettingsType = SettingsType.ST_NONE
}

class TaskMaterialBridge(panel: PanelObjectInventory, uuid: UUID, name: String) :
    TaskInvFVBridge(panel, uuid, name) {
    override fun canOpenItem(): Boolean = true
    override fun openItem() {
        val obj = ViewerObjectList.findObject(panel.taskUUID)
        if (obj == null || obj.isInventoryPending()) return
        val item = obj.getInventoryObject(uuid) as? InventoryItem
        val itemCopy = item != null && Agent.allowOperation(PermType.COPY, item.permissions)
        if (itemCopy || obj.permModify() || Agent.isGodlike()) {
            val key = mapOf("taskid" to panel.taskUUID, "itemid" to uuid)
            val mat = FloaterReg.getTypedInstance<MaterialEditor>("material_editor", key)
            mat?.let {
                it.setObjectId(panel.taskUUID)
                it.openFloater(key)
                it.setFocus(true)
            }
        }
    }
    override fun removeItem(): Boolean {
        FloaterReg.hideInstance("material_editor", uuid)
        return super.removeItem()
    }
}

// ─── PanelObjectInventory ──────────────────────────────────────────────────────

class PanelObjectInventory(
    val showRootFolder: Boolean = true
) : Panel(), VOInventoryListener {

    val rootViewModel: FolderViewModelInventory = FolderViewModelInventory(name)

    var taskUUID: UUID = NULL_UUID
        private set

    private var attachmentUUID: UUID = NULL_UUID
    private var haveInventory: Boolean = false
    private var isInventoryEmpty: Boolean = true
    private var inventoryNeedsUpdate: Boolean = false

    private var scroller: ScrollContainer? = null
    private var folders: FolderView? = null
    private val itemMap: MutableMap<UUID, FolderViewItem> = mutableMapOf()

    fun hasInventory(): Boolean = haveInventory

    val filter: InventoryFilter get() = rootViewModel.filter

    fun getRootFolder(): FolderView? = folders

    override fun postBuild(): Boolean {
        reset()
        IdleCallbacks.addFunction(::idle)
        return true
    }

    fun doToSelected(userdata: Any?) {
        val action = userdata?.toString() ?: return
        if (action == "rename" || action == "delete") {
            val obj = ViewerObjectList.findObject(taskUUID)
            if (obj != null && !obj.permModify()) {
                NotificationsUtil.add("CantModifyContentInNoModTask")
                return
            }
        }
        InventoryAction.doToSelected(folders, action)
    }

    fun refresh() {
        var hasInventoryLocal = false
        val selection = SelectMgr.instance.selection
        val node = selection.getFirstRootNode(nonRootOk = true)
        if (node != null && node.isValid) {
            val obj = node.getObject()
            if (obj != null && (selection.rootObjectCount == 1 || selection.objectCount == 1)) {
                var makeRequest = !haveInventory
                if (taskUUID != obj.id) {
                    taskUUID = obj.id
                    attachmentUUID = obj.attachmentItemId
                    makeRequest = true
                    clearContents()
                    registerVOInventoryListener(obj, null)
                } else if (attachmentUUID != obj.attachmentItemId) {
                    attachmentUUID = obj.attachmentItemId
                    if (attachmentUUID != NULL_UUID) {
                        SelectMgr.instance.sendSelect()
                    }
                }
                if (node.inventorySerial != obj.inventorySerial || obj.isInventoryDirty()) {
                    makeRequest = true
                }
                if (makeRequest) requestVOInventory()
                hasInventoryLocal = true
            }
        }
        if (!hasInventoryLocal) clearInventoryTask()
        rootViewModel.setTaskId(taskUUID)
    }

    fun clearInventoryTask() {
        taskUUID = NULL_UUID
        attachmentUUID = NULL_UUID
        removeVOInventoryListener()
        clearContents()
    }

    fun removeSelectedItem() {
        folders?.removeSelectedItems()
    }

    fun startRenamingSelectedItem() {
        folders?.startRenamingSelectedItem()
    }

    override fun draw() {
        super.draw()
        if (isInventoryEmpty) {
            val text = when {
                !haveInventory && taskUUID != NULL_UUID -> Trans.getString("LoadingContents")
                haveInventory -> Trans.getString("NoContents")
                else -> ""
            }
            if (text.isNotEmpty()) {
                // no-op
            }
        }
    }

    override fun deleteAllChildren() {
        scroller = null
        folders = null
        super.deleteAllChildren()
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: Array<AcceptType>, tooltipMsg: StringBuilder
    ): Boolean {
        val folderp = folders?.getNextFromChild(null) ?: return false
        val localX = x - (folders?.rect?.left ?: 0)
        val localY = y - (folders?.rect?.bottom ?: 0)
        return if (folders?.pointInView(localX, localY) == true) {
            folders!!.handleDragAndDrop(localX, localY, mask, drop, cargoType, cargoData, accept, tooltipMsg)
        } else {
            folders!!.handleDragAndDrop(5, 1, mask, drop, cargoType, cargoData, accept, tooltipMsg)
        }
    }

    override fun onFocusLost() {
        super.onFocusLost()
    }

    override fun onFocusReceived() {
        super.onFocusReceived()
    }

    override fun inventoryChanged(
        obj: ViewerObject?,
        inventory: MutableList<InventoryObject>?,
        serialNum: Int,
        userData: Any?
    ) {
        obj ?: return
        if (taskUUID == obj.id) {
            inventoryNeedsUpdate = true
        }
        inventory?.forEach { item ->
            FloaterReg.findTypedInstance<FloaterProperties>("properties", item.uuid)?.refresh()
        }
    }

    fun updateInventory() {
        val inventoryHasFocus = haveInventory && folders != null

        val selectedItemIds = mutableListOf<UUID>()
        if (haveInventory && folders != null) {
            folders!!.selectionList.forEach { item ->
                (item.viewModelItem as? FolderViewModelItemInventory)?.getUUID()
                    ?.let { selectedItemIds.add(it) }
            }
        }

        val objectp = ViewerObjectList.findObject(taskUUID)
        if (objectp != null) {
            val inventoryRoot = objectp.inventoryRoot
            val contents = mutableListOf<InventoryObject>()
            objectp.getInventoryContents(contents)

            if (inventoryRoot != null) {
                reset()
                isInventoryEmpty = false
                createFolderViews(inventoryRoot, contents)
                folders?.setEnabled(true)
            } else {
                isInventoryEmpty = true
            }

            haveInventory = !isInventoryEmpty || !objectp.isInventoryDirty()
            if (objectp.isInventoryDirty()) objectp.requestInventory()
        } else {
            isInventoryEmpty = true
            haveInventory = true
        }

        var firstItem = true
        for (itemId in selectedItemIds) {
            val selected = getItemById(itemId) ?: continue
            if (firstItem) {
                folders?.setSelection(selected, true, inventoryHasFocus)
                firstItem = false
            } else {
                folders?.changeSelection(selected, true)
            }
        }

        folders?.requestArrange()
        inventoryNeedsUpdate = false
    }

    private fun createFolderViews(
        inventoryRoot: InventoryObject,
        contents: MutableList<InventoryObject>
    ) {
        val bridge = TaskInvFVBridge.createObjectBridge(this, inventoryRoot) ?: return
        val newFolder = FolderViewFolder(
            name = inventoryRoot.name,
            root = folders,
            listener = bridge
        )
        if (showRootFolder) {
            newFolder.addToFolder(folders)
            newFolder.toggleOpen()
        }
        if (contents.isNotEmpty()) {
            createViewsForCategory(contents, inventoryRoot, if (showRootFolder) newFolder else folders!!)
        }
        if (showRootFolder) newFolder.refresh()
    }

    private fun createViewsForCategory(
        inventory: MutableList<InventoryObject>,
        parent: InventoryObject,
        folder: FolderViewFolder
    ) {
        val childCategories = mutableListOf<Pair<InventoryObject, FolderViewFolder>>()

        for (obj in inventory) {
            if (parent.uuid != obj.parentUUID) continue
            val bridge = TaskInvFVBridge.createObjectBridge(this, obj) ?: continue
            val view: FolderViewItem = if (obj.type == AssetType.AT_CATEGORY) {
                val f = FolderViewFolder(name = obj.name, root = folders, listener = bridge)
                childCategories.add(Pair(obj, f))
                f
            } else {
                FolderViewItem(
                    name = obj.name,
                    root = folders,
                    listener = bridge,
                    creationDate = bridge.getCreationDate()
                )
            }
            view.addToFolder(folder)
            addItemId(obj.uuid, view)
        }

        for ((childObj, childFolder) in childCategories) {
            createViewsForCategory(inventory, childObj, childFolder)
        }
        folder.setChildrenInited(true)
    }

    private fun clearContents() {
        haveInventory = false
        isInventoryEmpty = true
        ToolDragAndDrop.instance?.let {
            if (it.source == ToolDragAndDrop.Source.SOURCE_WORLD) it.endDrag()
        }
        clearItemIds()
        scroller?.let {
            removeChild(it)
            it.die()
            scroller = null
            folders = null
        }
    }

    private fun reset() {
        clearContents()
        rootViewModel.filter.setShowFolderState(InventoryFilter.ShowFolderState.SHOW_ALL_FOLDERS)

        folders = FolderView(
            name = "task inventory",
            title = "task inventory",
            parentPanel = this,
            toolTip = Trans.getString("PanelContentsTooltip"),
            listener = TaskInvFVBridge.createObjectBridge(this, null),
            viewModel = rootViewModel,
            optionsMenu = "menu_inventory.xml"
        )

        val scrollerLocal = ScrollContainer(name = "task inventory scroller", followsAll = true)
        scrollerLocal.addChild(folders!!)
        folders!!.setScrollContainer(scrollerLocal)
        addChild(scrollerLocal)
        scroller = scrollerLocal
    }

    fun getItemById(id: UUID): FolderViewItem? = itemMap[id]
    fun addItemId(id: UUID, item: FolderViewItem) { itemMap[id] = item }
    fun removeItemId(id: UUID) { itemMap.remove(id) }
    fun clearItemIds() { itemMap.clear() }

    fun handleKeyHere(key: Key, mask: Int): Boolean {
        return when (key) {
            Key.KEY_RETURN -> if (mask == MASK_NONE) {
                doToSelected("task_open"); true
            } else false
            Key.KEY_DELETE, Key.KEY_BACKSPACE -> if (isSelectionRemovable() && mask == MASK_NONE) {
                InventoryAction.doToSelected(folders, "delete"); true
            } else false
            else -> false
        }
    }

    private fun isSelectionRemovable(): Boolean {
        val root = folders?.root ?: return false
        val sel = root.selectionList
        if (sel.isEmpty()) return false
        return sel.all { item ->
            val listener = item.viewModelItem as? FolderViewModelItemInventory
            listener != null && listener.isItemRemovable() && !listener.isItemInTrash()
        }
    }

    companion object {
        fun idle(self: PanelObjectInventory) {
            self.folders?.update()
            if (self.inventoryNeedsUpdate) self.updateInventory()
        }
        private val NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
}
