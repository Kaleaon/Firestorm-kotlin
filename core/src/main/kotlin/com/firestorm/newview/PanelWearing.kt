package com.firestorm.newview

import java.util.UUID

class WearingGearMenu(private val panelWearing: PanelWearing) {
    private var menu: ToggleableMenu? = null

    init {
        menu = UICtrlFactory.getInstance().createFromFile<ToggleableMenu>("menu_wearing_gear.xml")
    }

    fun getMenu(): ToggleableMenu? = menu

    fun handleMultiple(functor: (UUID) -> Unit) {
        val selectedIds = mutableListOf<UUID>()
        panelWearing.getSelectedItemsUUIDs(selectedIds)
        selectedIds.forEach { functor(it) }
    }
}

class WearingContextMenu : ListContextMenu() {
    override fun createMenu(): ContextMenu {
        val menu = createFromFile("menu_wearing_tab.xml")
        updateMenuItemsVisibility(menu)
        return menu
    }

    private fun updateMenuItemsVisibility(menu: ContextMenu) {
        var bpSelected = false
        var clothesSelected = false
        var attachmentsSelected = false
        var canFavorite = false
        var canUnfavorite = false

        for (id in uuids) {
            val item = Inventory.getInstance().getItem(id) ?: continue
            val linkedId = item.getLinkedUUID()
            val linkedItem = Inventory.getInstance().getItem(linkedId)

            when (item.getType()) {
                AssetType.AT_CLOTHING -> clothesSelected = true
                AssetType.AT_BODYPART -> bpSelected = true
                AssetType.AT_OBJECT, AssetType.AT_GESTURE -> attachmentsSelected = true
                else -> {}
            }
            canFavorite = canFavorite || linkedItem?.getIsFavorite() == false
            canUnfavorite = canUnfavorite || linkedItem?.getIsFavorite() == true
        }

        val showTouch = !bpSelected && !clothesSelected && attachmentsSelected
        val showEdit = bpSelected || clothesSelected || attachmentsSelected
        val allowDetach = !bpSelected && !clothesSelected && attachmentsSelected
        val allowTakeOff = !bpSelected && clothesSelected && !attachmentsSelected

        menu.setItemVisible("touch_attach", showTouch)
        menu.setItemEnabled("touch_attach", uuids.size == 1)
        menu.setItemVisible("edit_item", showEdit)
        menu.setItemEnabled("edit_item", uuids.size == 1)
        menu.setItemVisible("take_off", allowTakeOff)
        menu.setItemVisible("detach", allowDetach)
        menu.setItemVisible("edit_outfit_separator", showTouch || showEdit || allowTakeOff || allowDetach)
        menu.setItemVisible("show_original", uuids.size == 1)
        menu.setItemVisible("favorites_add", canFavorite)
        menu.setItemVisible("favorites_remove", canUnfavorite)
        menu.setItemVisible("take_off_or_detach", !allowDetach && !allowTakeOff && clothesSelected && attachmentsSelected)
    }
}

class TempAttachmentsContextMenu(private val panelWearing: PanelWearing) : ListContextMenu() {
    override fun createMenu(): ContextMenu {
        val menu = createFromFile("menu_wearing_tab.xml")
        menu.setItemVisible("touch_attach", true)
        menu.setItemEnabled("touch_attach", uuids.size == 1)
        menu.setItemVisible("edit_item", true)
        menu.setItemEnabled("edit_item", uuids.size == 1)
        menu.setItemVisible("take_off", false)
        menu.setItemVisible("detach", true)
        menu.setItemVisible("take_off_or_detach", false)
        menu.setItemVisible("edit_outfit_separator", false)
        menu.setItemVisible("show_original", false)
        menu.setItemVisible("edit_outfit", false)
        return menu
    }
}

class PanelWearing : PanelAppearanceTab() {

    private var cofItemsList: WearableItemsList? = null
    private var tempItemsList: ScrollListCtrl? = null
    private var gearMenu: WearingGearMenu? = null
    private var gearMenuConnection: (() -> Unit)? = null
    private var contextMenu: WearingContextMenu? = null
    private var attachmentsMenu: TempAttachmentsContextMenu? = null

    private var wearablesTab: AccordionCtrlTab? = null
    private var attachmentsTab: AccordionCtrlTab? = null
    private var accordionCtrl: AccordionCtrl? = null

    private var avatarComplexityLabel: TextBox? = null
    private val tempItemComplexityMap: MutableMap<UUID, UInt> = mutableMapOf()
    private val attachmentsMap: MutableMap<UUID, ViewerObject> = mutableMapOf()
    private val objectNames: MutableMap<UUID, String> = mutableMapOf()

    private var attachmentsChangedConnection: (() -> Unit)? = null
    private val updateTimer = FrameTimer()

    private var isInitialized: Boolean = false

    init {
        gearMenu = WearingGearMenu(this)
        contextMenu = WearingContextMenu()
        attachmentsMenu = TempAttachmentsContextMenu(this)
    }

    open fun postBuild(): Boolean {
        accordionCtrl = getChild<AccordionCtrl>("wearables_accordion")
        wearablesTab = getChild<AccordionCtrlTab>("tab_wearables")
        wearablesTab?.setIgnoreResizeNotification(true)
        attachmentsTab = getChild<AccordionCtrlTab>("tab_temp_attachments")
        attachmentsTab?.setDropDownStateChangedCallback { onAccordionTabStateChanged() }

        cofItemsList = getChild<WearableItemsList>("cof_items_list")
        cofItemsList?.setRightMouseDownCallback { ctrl, x, y -> onWearableItemsListRightClick(ctrl, x, y) }
        cofItemsList?.setDoubleClickCallback { onDoubleClick() }

        tempItemsList = getChild<ScrollListCtrl>("temp_attachments_list")
        tempItemsList?.setFgUnselectedColor(Color4.white)
        tempItemsList?.setRightMouseDownCallback { ctrl, x, y -> onTempAttachmentsListRightClick(ctrl, x, y) }
        tempItemsList?.setDoubleClickCallback { onRemoveAttachment() }

        avatarComplexityLabel = getChild<TextBox>("avatar_complexity_label")

        val gearBtn = findChild<MenuButton>("options_gear_btn")
        gearBtn?.setMenu(gearMenu?.getMenu())

        return true
    }

    open fun onOpen(info: LLSD) {
        if (!isInitialized) {
            if (!Inventory.getInstance().isInventoryUsable()) return

            val cof = Inventory.getInstance().findCategoryUUIDForType(FolderType.FT_CURRENT_OUTFIT)
            val category = Inventory.getInstance().getCategory(cof) ?: return

            OutfitObserver.instance().addCOFChangedCallback {
                cofItemsList?.updateList(cof)
            }

            category.fetch()
            cofItemsList?.updateList(cof)
            isInitialized = true
        }
    }

    open fun draw() {
        if (updateTimer.getStarted() && updateTimer.getElapsedTimeF32() > 0.1f) {
            updateTimer.stop()
            updateAttachmentsList()
        }
        super.draw()
    }

    fun onAccordionTabStateChanged() {
        if (attachmentsTab?.isExpanded() == true) {
            startUpdateTimer()
            attachmentsChangedConnection = AppearanceMgr.instance().setAttachmentsChangedCallback {
                startUpdateTimer()
            }
        } else {
            attachmentsChangedConnection = null
        }
    }

    fun startUpdateTimer() {
        if (!updateTimer.getStarted()) {
            updateTimer.start()
        } else {
            updateTimer.reset()
        }
    }

    open fun onFilterSubStringChanged(newString: String, oldString: String) {
        cofItemsList?.setFilterSubString(newString, true)
        accordionCtrl?.arrange()
    }

    open fun isActionEnabled(userdata: LLSD): Boolean {
        val commandName = userdata.asString()

        if (commandName == "save_outfit") {
            val outfitLocked = AppearanceMgr.getInstance().isOutfitLocked()
            val outfitDirty = AppearanceMgr.getInstance().isOutfitDirty()
            return !outfitLocked && outfitDirty
        }

        if (commandName == "take_off") {
            return if (wearablesTab?.isExpanded() == true) {
                hasItemSelected() && canTakeOffSelected()
            } else {
                val item = tempItemsList?.getFirstSelected()
                item?.getUUID() != null
            }
        }

        val selectedUuids = mutableListOf<UUID>()
        getSelectedItemsUUIDs(selectedUuids)

        return when (commandName) {
            "touch_attach" -> selectedUuids.size == 1 && enableAttachmentTouch(selectedUuids.first())
            "edit_item" -> selectedUuids.size == 1 && getIsItemEditable(selectedUuids.first())
            else -> false
        }
    }

    fun updateAttachmentsList() {
        val attachs = AgentWearables.getTempAttachments()
        tempItemsList?.deleteAllItems()
        attachmentsMap.clear()
        if (attachs.isNotEmpty()) {
            if (!populateAttachmentsList()) {
                requestAttachmentDetails()
            }
        } else {
            val noAttachments = getString("no_attachments")
            val row = LLSD()
            row["columns"][0]["column"] = "text"
            row["columns"][0]["value"] = noAttachments
            row["columns"][0]["font"] = "SansSerifBold"
            tempItemsList?.addElement(row)
        }
    }

    fun populateAttachmentsList(update: Boolean = false): Boolean {
        var populated = true
        val list = tempItemsList ?: return populated

        list.deleteAllItems()
        attachmentsMap.clear()

        val attachs = AgentWearables.getTempAttachments()
        val iconName = InventoryIcon.getIconName(AssetType.AT_OBJECT, InventoryType.IT_OBJECT)

        for (attachment in attachs) {
            val row = LLSD()
            row["id"] = attachment.getID()
            row["columns"][0]["column"] = "icon"
            row["columns"][0]["type"] = "icon"
            row["columns"][0]["value"] = iconName
            row["columns"][1]["column"] = "text"

            val id = attachment.getID()
            when {
                objectNames.containsKey(id) && objectNames[id]!!.isNotEmpty() ->
                    row["columns"][1]["value"] = objectNames[id]
                update -> {
                    row["columns"][1]["value"] = id.toString()
                    populated = false
                }
                else -> {
                    row["columns"][1]["value"] = Trans.getString("LoadingData")
                    populated = false
                }
            }

            row["columns"][2]["column"] = "weight"
            row["columns"][2]["value"] = tempItemComplexityMap[id]?.toString() ?: "0"
            row["columns"][2]["halign"] = "right"

            list.addElement(row)
            attachmentsMap[id] = attachment
        }

        return populated
    }

    fun requestAttachmentDetails() {
        val url = Agent.instance().getRegionCapability("AttachmentResources")
        if (url.isNotEmpty()) {
            System.err.println("PanelWearing: requestAttachmentDetails not yet implemented")
        }
    }

    fun setAttachmentDetails(content: LLSD) {
        objectNames.clear()
        val numberAttachments = content["attachments"].size()
        for (i in 0 until numberAttachments) {
            val numberObjects = content["attachments"][i]["objects"].size()
            for (j in 0 until numberObjects) {
                val taskId = content["attachments"][i]["objects"][j]["id"].asUUID()
                val name = content["attachments"][i]["objects"][j]["name"].asString()
                objectNames[taskId] = name
            }
        }
        if (objectNames.isNotEmpty()) {
            populateAttachmentsList(true)
        }
    }

    fun setSelectionChangeCallback(cb: () -> Unit): (() -> Unit)? {
        cofItemsList?.setCommitCallback(cb)
        return cb
    }

    fun hasItemSelected(): Boolean = cofItemsList?.getSelectedItem() != null

    open fun getSelectedItemsUUIDs(selectedUuids: MutableList<UUID>) {
        cofItemsList?.getSelectedUUIDs(selectedUuids)
    }

    open fun copyToClipboard() {
        val values = mutableListOf<LLSD>()
        cofItemsList?.getValues(values)

        val text = values.mapIndexedNotNull { i, uuid ->
            val item = Inventory.getInstance().getItem(uuid.asUUID())
            if (item != null) {
                if (i < values.size - 1) item.getName() + "\n" else item.getName()
            } else null
        }.joinToString("")

        Clipboard.instance().copyToClipboard(text)
    }

    fun onEditAttachment() {
        val item = tempItemsList?.getFirstSelected() ?: return
        SelectMgr.getInstance().deselectAll()
        SelectMgr.getInstance().selectObjectAndFamily(attachmentsMap[item.getUUID()])
        handleObjectEdit()
    }

    fun onRemoveAttachment() {
        val item = tempItemsList?.getFirstSelected() ?: return
        if (item.getUUID() != null) {
            SelectMgr.getInstance().deselectAll()
            SelectMgr.getInstance().selectObjectAndFamily(attachmentsMap[item.getUUID()])
            SelectMgr.getInstance().sendDetach()
        }
    }

    fun getGearMenu(): ToggleableMenu? = gearMenu?.getMenu()

    fun getSortMenu(): ToggleableMenu? = null

    fun getTrashMenuVisible(): Boolean = false

    fun updateMenuItemsVisibility() {}

    fun onRemoveItem() {
        if (wearablesTab?.isExpanded() == true) {
            val selectedUuids = mutableListOf<UUID>()
            getSelectedItemsUUIDs(selectedUuids)
            AppearanceMgr.instance().removeItemsFromAvatar(selectedUuids)
        } else {
            onRemoveAttachment()
        }
    }

    fun updateAvatarComplexity(
        complexity: UInt,
        itemComplexity: MutableMap<UUID, UInt>,
        tempItemComplexity: MutableMap<UUID, UInt>,
        bodyPartsComplexity: UInt
    ) {
        avatarComplexityLabel?.setTextArg("[WEIGHT]", complexity.toString())
        cofItemsList?.updateItemComplexity(itemComplexity, bodyPartsComplexity)
        tempItemComplexityMap.clear()
        tempItemComplexityMap.putAll(tempItemComplexity)
        updateAttachmentsList()
    }

    private fun onWearableItemsListRightClick(ctrl: UICtrl, x: Int, y: Int) {
        val list = ctrl as? WearableItemsList ?: return
        val selectedUuids = mutableListOf<UUID>()
        list.getSelectedUUIDs(selectedUuids)
        contextMenu?.show(ctrl, selectedUuids, x, y)
    }

    private fun onTempAttachmentsListRightClick(ctrl: UICtrl, x: Int, y: Int) {
        val list = ctrl as? ScrollListCtrl ?: return
        list.selectItemAt(x, y)
        val selectedUuids = mutableListOf<UUID>()
        val currentId = list.getCurrentID()
        if (currentId != null) {
            selectedUuids.add(currentId)
            attachmentsMenu?.show(ctrl, selectedUuids, x, y)
        }
    }

    private fun onDoubleClick() {
        val selectedItemId = cofItemsList?.getSelectedUUID() ?: return
        val ids = mutableListOf(selectedItemId)
        val item = Inventory.getInstance().getItem(selectedItemId) ?: return

        val isClothing = item.getType() == AssetType.AT_CLOTHING
        val isObject = item.getType() == AssetType.AT_OBJECT

        if ((isClothing && RlvActions.canRemoveWearable(item)) ||
            (isObject && RlvActions.canDetachAttachment(item))
        ) {
            AppearanceMgr.instance().removeItemsFromAvatar(ids)
        }
    }
}
