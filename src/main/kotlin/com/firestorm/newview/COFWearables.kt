package com.firestorm.newview

import com.firestorm.ui.Panel
import com.firestorm.ui.UICtrl
import com.firestorm.ui.FlatListView
import com.firestorm.ui.TabContainer
import com.firestorm.ui.Rect
import com.firestorm.llsd.LLSD
import com.firestorm.types.LLUUID
import com.firestorm.inventory.InventoryModel
import com.firestorm.inventory.ViewerInventoryItem
import com.firestorm.inventory.AssetType
import com.firestorm.wearable.WearableType
import com.firestorm.wearable.WearableItemsList
import com.firestorm.wearable.AgentWearables
import com.firestorm.appearance.AppearanceMgr
import com.firestorm.appearance.PanelClothingListItem
import com.firestorm.appearance.PanelBodyPartsListItem
import com.firestorm.appearance.PanelDeletableWearableListItem
import com.firestorm.appearance.PanelAttachmentListItem
import com.firestorm.appearance.PanelDummyClothingListItem
import com.firestorm.appearance.PanelInventoryListItemBase
import com.firestorm.context.ListContextMenu
import com.firestorm.agent.Agent

// ────────────────────────────────────────────────────────────────────────────
// COFWearables — shows current-outfit wearables in three flat lists
// ────────────────────────────────────────────────────────────────────────────

class COFWearables : Panel() {

    // ── Callbacks ────────────────────────────────────────────────────────────

    class COFCallbacks {
        var addWearable: (() -> Unit)? = null
        var moveWearableCloser: (() -> Unit)? = null
        var moveWearableFurther: (() -> Unit)? = null
        var editWearable: (() -> Unit)? = null
        var deleteWearable: (() -> Unit)? = null
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    private var attachments: FlatListView? = null
    private var clothing: FlatListView? = null
    private var bodyParts: FlatListView? = null
    private var lastSelectedList: FlatListView? = null

    private var clothingTab: Panel? = null
    private var attachmentsTab: Panel? = null
    private var bodyPartsTab: Panel? = null
    private var lastSelectedTab: Panel? = null

    private val tab2AssetType: MutableMap<Panel, AssetType> = mutableMapOf()

    val cofCallbacks = COFCallbacks()

    private var clothingMenu: ListContextMenu? = null
    private var attachmentMenu: ListContextMenu? = null
    private var bodyPartMenu: ListContextMenu? = null

    private var accordionCtrl: TabContainer? = null

    private var cofVersion: Int = -1

    // ── Lifecycle ────────────────────────────────────────────────────────────

    init {
        clothingMenu = CofClothingContextMenu(this)
        attachmentMenu = CofAttachmentContextMenu(this)
        bodyPartMenu = CofBodyPartContextMenu(this)
    }

    override fun onDestroy() {
        // menus are managed by their owning objects — no explicit delete needed in Kotlin
        super.onDestroy()
    }

    override fun postBuild(): Boolean {
        attachments = getChild("list_attachments")
        clothing = getChild("list_clothing")
        bodyParts = getChild("list_body_parts")

        clothing!!.setRightMouseDownCallback { ctrl, x, y -> onListRightClick(ctrl, x, y, clothingMenu) }
        attachments!!.setRightMouseDownCallback { ctrl, x, y -> onListRightClick(ctrl, x, y, attachmentMenu) }
        bodyParts!!.setRightMouseDownCallback { ctrl, x, y -> onListRightClick(ctrl, x, y, bodyPartMenu) }

        attachments!!.setCommitCallback { onSelectionChange(attachments!!) }
        clothing!!.setCommitCallback { onSelectionChange(clothing!!) }
        bodyParts!!.setCommitCallback { onSelectionChange(bodyParts!!) }

        attachments!!.setCommitOnSelectionChange(true)
        clothing!!.setCommitOnSelectionChange(true)
        bodyParts!!.setCommitOnSelectionChange(true)

        attachments!!.setComparator(WearableItemsList.NAME_COMPARATOR)
        bodyParts!!.setComparator(WearableItemsList.NAME_COMPARATOR)

        clothingTab = getChild("tab_clothing")
        attachmentsTab = getChild("tab_attachments")
        bodyPartsTab = getChild("tab_body_parts")

        tab2AssetType[clothingTab!!] = AssetType.AT_CLOTHING
        tab2AssetType[attachmentsTab!!] = AssetType.AT_OBJECT
        tab2AssetType[bodyPartsTab!!] = AssetType.AT_BODYPART

        accordionCtrl = getChild("wearable_accordion")
        accordionCtrl!!.setCommitCallback { param -> onSelectedTabChanged(param) }

        return super.postBuild()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun getSelectedUUID(): LLUUID = lastSelectedList?.getSelectedUUID() ?: LLUUID.null_

    fun getSelectedUUIDs(selectedIds: MutableList<LLUUID>): Boolean {
        lastSelectedList?.getSelectedUUIDs(selectedIds) ?: return false
        return selectedIds.isNotEmpty()
    }

    fun getSelectedItem(): Panel? = lastSelectedList?.getSelectedItem()

    fun getSelectedItems(selectedItems: MutableList<Panel>) {
        lastSelectedList?.getSelectedItems(selectedItems)
    }

    fun refresh() {
        val cofId = AppearanceMgr.instance.getCOF()
        if (cofId.isNull) return

        val category = InventoryModel.instance.getCategory(cofId) ?: return

        cofVersion = category.version

        val savedScrollPos = mapOf(
            attachments!! to attachments!!.visibleContentRect,
            clothing!!    to clothing!!.visibleContentRect,
            bodyParts!!   to bodyParts!!.visibleContentRect
        )

        val savedSelection = mutableMapOf(
            attachments!! to mutableListOf<LLSD>(),
            clothing!!    to mutableListOf(),
            bodyParts!!   to mutableListOf()
        )
        attachments!!.getSelectedValues(savedSelection[attachments!!]!!)
        clothing!!.getSelectedValues(savedSelection[clothing!!]!!)
        bodyParts!!.getSelectedValues(savedSelection[bodyParts!!]!!)

        clear()

        val cats = mutableListOf<InventoryModel.Category>()
        val cofItems = mutableListOf<ViewerInventoryItem>()
        InventoryModel.instance.collectDescendents(cofId, cats, cofItems, excludeTrash = true)

        populateAttachmentsAndBodypartsLists(cofItems)

        val clothingByType = MutableList(WearableType.WT_COUNT) { mutableListOf<ViewerInventoryItem>() }
        AppearanceMgr.instance.divvyWearablesByType(cofItems, clothingByType)
        populateClothingList(clothingByType)

        // Restore selection without firing commit callbacks
        for ((list, values) in savedSelection) {
            list.setCommitOnSelectionChange(false)
            for (value in values) {
                if (value.asUUID().isNotNull) list.selectItemByValue(value)
            }
            list.setCommitOnSelectionChange(true)
        }

        // Restore scroll positions
        for ((list, scrollPos) in savedScrollPos) {
            list.scrollToShowRect(scrollPos)
        }
    }

    fun clear() {
        attachments?.clear()
        clothing?.clear()
        bodyParts?.clear()
    }

    fun getExpandedAccordionAssetType(): AssetType {
        val ctrl = accordionCtrl ?: return AssetType.AT_NONE
        val currentPanel = ctrl.getCurrentPanel()
        return tab2AssetType[currentPanel] ?: AssetType.AT_NONE
    }

    fun getSelectedAccordionAssetType(): AssetType {
        val ctrl = accordionCtrl ?: return AssetType.AT_NONE
        val currentPanel = ctrl.getCurrentPanel()
        return tab2AssetType[currentPanel] ?: AssetType.AT_NONE
    }

    fun expandDefaultAccordionTab() {
        accordionCtrl?.selectFirstTab()
    }

    fun selectClothing(clothingType: WearableType.EType) {
        val items = mutableListOf<Panel>()
        clothing?.getItems(items)
        for (item in items) {
            val clothingItem = item as? PanelClothingListItem ?: continue
            if (clothingItem.wearableType == clothingType) {
                clothing?.selectItem(clothingItem)
                break
            }
        }
    }

    fun setAttachmentsTitle() {
        val tab = attachmentsTab ?: return
        val freeSlots = Agent.instance.avatar.maxAttachments - (attachments?.size ?: 0)
        val title = "Attachments remain: $freeSlots"
        accordionCtrl?.setPanelTitle(accordionCtrl!!.getIndexForPanel(tab), title)
    }

    // ── Protected helpers ────────────────────────────────────────────────────

    protected fun populateAttachmentsAndBodypartsLists(cofItems: List<ViewerInventoryItem>) {
        for (item in cofItems) {
            when (item.type) {
                AssetType.AT_CLOTHING -> continue
                AssetType.AT_OBJECT, AssetType.AT_GESTURE -> {
                    val panel = buildAttachmentListItem(item) ?: continue
                    attachments!!.addItem(panel, item.uuid, addBottom = true, rearrange = false)
                }
                AssetType.AT_BODYPART -> {
                    val panel = buildBodypartListItem(item) ?: continue
                    bodyParts!!.addItem(panel, item.uuid, addBottom = true, rearrange = false)
                }
                else -> {}
            }
        }

        if (attachments!!.size > 0) {
            attachments!!.sort()
            attachments!!.notify(REARRANGE)
        } else {
            attachments!!.setNoItemsCommentText("No attachments")
        }

        setAttachmentsTitle()

        if (bodyParts!!.size > 0) {
            bodyParts!!.sort()
            bodyParts!!.notify(REARRANGE)
        }
    }

    protected fun populateClothingList(clothingByType: List<MutableList<ViewerInventoryItem>>) {
        for (type in WearableType.WT_SHIRT until WearableType.WT_COUNT) {
            val items = clothingByType[type]
            if (items.isEmpty()) continue

            AppearanceMgr.sortItemsByActualDescription(items)

            // Display in reverse: furthest from body first
            for (i in items.size downTo 1) {
                val item = items[i - 1]
                val panel = buildClothingListItem(item, first = i == items.size, last = i == 1) ?: continue
                clothing!!.addItem(panel, item.uuid, addBottom = true, rearrange = false)
            }
        }

        addClothingTypesDummies(clothingByType)
        clothing!!.notify(REARRANGE)
    }

    protected fun addClothingTypesDummies(clothingByType: List<List<ViewerInventoryItem>>) {
        for (type in WearableType.WT_SHIRT until WearableType.WT_COUNT) {
            if (clothingByType[type].isNotEmpty()) continue
            val wType = WearableType.EType.fromOrdinal(type)
            val panel = PanelDummyClothingListItem.create(wType) ?: continue
            panel.childSetAction("btn_add") { cofCallbacks.addWearable?.invoke() }
            clothing!!.addItem(panel, LLUUID.null_, addBottom = true, rearrange = false)
        }
    }

    protected fun onSelectionChange(selectedList: FlatListView) {
        if (selectedList != lastSelectedList) {
            if (selectedList != attachments) attachments?.resetSelection(true)
            if (selectedList != clothing) clothing?.resetSelection(true)
            if (selectedList != bodyParts) bodyParts?.resetSelection(true)
            lastSelectedList = selectedList
        }
        onCommit()
    }

    protected fun onSelectedTabChanged(param: LLSD) {
        val hadSelection = (clothing?.numSelected() ?: 0) > 0 ||
                (attachments?.numSelected() ?: 0) > 0 ||
                (bodyParts?.numSelected() ?: 0) > 0

        clothing?.resetSelection(true)
        attachments?.resetSelection(true)
        bodyParts?.resetSelection(true)

        val tab = findChild<Panel>(param.asString())
        var tabSelectionChanged = false
        if (tab != null && tab != lastSelectedTab) {
            lastSelectedTab = tab
            tabSelectionChanged = true
        }

        if (hadSelection || tabSelectionChanged) onCommit()
    }

    protected fun buildClothingListItem(item: ViewerInventoryItem, first: Boolean, last: Boolean): PanelClothingListItem? {
        val panel = PanelClothingListItem.create(item) ?: return null
        val linked = item.getLinkedItem() ?: item
        val allowModify = linked.permissions.allowModifyBy(Agent.instance.id)

        panel.setShowLockButton(!allowModify)
        panel.setShowEditButton(allowModify)
        panel.setShowMoveUpButton(!first)
        panel.setShowMoveDownButton(!last)

        panel.childSetAction("btn_delete") { cofCallbacks.deleteWearable?.invoke() }
        panel.childSetAction("btn_move_up") { cofCallbacks.moveWearableFurther?.invoke() }
        panel.childSetAction("btn_move_down") { cofCallbacks.moveWearableCloser?.invoke() }
        panel.childSetAction("btn_edit") { cofCallbacks.editWearable?.invoke() }

        panel.setSeparatorVisible(last)
        return panel
    }

    protected fun buildBodypartListItem(item: ViewerInventoryItem): PanelBodyPartsListItem? {
        val panel = PanelBodyPartsListItem.create(item) ?: return null
        val linked = item.getLinkedItem() ?: item
        val allowModify = linked.permissions.allowModifyBy(Agent.instance.id)

        panel.setShowLockButton(!allowModify)
        panel.setShowEditButton(allowModify)

        panel.childSetAction("btn_delete") { cofCallbacks.deleteWearable?.invoke() }
        panel.childSetAction("btn_edit") { cofCallbacks.editWearable?.invoke() }

        return panel
    }

    protected fun buildAttachmentListItem(item: ViewerInventoryItem): PanelDeletableWearableListItem? {
        val panel = PanelAttachmentListItem.create(item) ?: return null
        panel.childSetAction("btn_delete") { cofCallbacks.deleteWearable?.invoke() }
        return panel
    }

    protected fun onListRightClick(ctrl: UICtrl, x: Int, y: Int, menu: ListContextMenu?) {
        menu ?: return
        val selectedUuids = mutableListOf<LLUUID>()
        if (getSelectedUUIDs(selectedUuids)) {
            val showMenu = selectedUuids.any { !it.isNull }
            if (showMenu) menu.show(ctrl, selectedUuids, x, y)
        }
    }

    // ── Internal context-menu classes ─────────────────────────────────────────

    private abstract inner class CofContextMenu(protected val cofWearables: COFWearables) : ListContextMenu() {

        protected fun getWearableType(itemId: LLUUID): WearableType.EType {
            if (!isDummyItem(itemId)) {
                val item = InventoryModel.instance.getLinkedItem(itemId)
                if (item != null && item.isWearableType) return item.wearableType
            } else {
                val selected = cofWearables.getSelectedItem() as? PanelDummyClothingListItem
                if (selected != null) return selected.wearableType
            }
            return WearableType.WT_NONE
        }

        protected fun createNew(itemId: LLUUID) {
            AgentWearables.createWearable(getWearableType(itemId), true)
        }

        protected fun updateCreateWearableLabel(menu: Any, itemId: LLUUID) {
            val wType = getWearableType(itemId)
            TODO("GPU: update menu item label for wearable type $wType")
        }

        companion object {
            fun isDummyItem(itemId: LLUUID) = itemId.isNull
        }
    }

    private inner class CofAttachmentContextMenu(cofWearables: COFWearables) : CofContextMenu(cofWearables) {
        override fun createMenu(): Any {
            TODO("GPU: build menu_cof_attachment.xml context menu")
        }
    }

    private inner class CofClothingContextMenu(cofWearables: COFWearables) : CofContextMenu(cofWearables) {
        override fun createMenu(): Any {
            TODO("GPU: build menu_cof_clothing.xml context menu")
        }
    }

    private inner class CofBodyPartContextMenu(cofWearables: COFWearables) : CofContextMenu(cofWearables) {
        override fun createMenu(): Any {
            TODO("GPU: build menu_cof_body_part.xml context menu")
        }
    }

    companion object {
        private val REARRANGE = LLSD().with("rearrange", LLSD())
    }
}
