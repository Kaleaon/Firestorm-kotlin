package com.firestorm.newview

import com.firestorm.inventory.InventoryPanel
import com.firestorm.ui.Button
import com.firestorm.ui.ComboBox
import com.firestorm.ui.FilterEditor
import com.firestorm.ui.LLSD
import com.firestorm.ui.LoadingIndicator
import com.firestorm.ui.MenuButton
import com.firestorm.ui.Panel
import com.firestorm.ui.TextBox
import com.firestorm.ui.ToggleableMenu
import com.firestorm.ui.View
import com.firestorm.wearable.WearableItemsList
import com.firestorm.wearable.WearableType
import java.util.UUID

private const val WEARABLE_MASK   = 1L shl InventoryType.IT_WEARABLE
private const val ATTACHMENT_MASK = (1L shl InventoryType.IT_ATTACHMENT) or (1L shl InventoryType.IT_OBJECT)
private const val ALL_ITEMS_MASK  = WEARABLE_MASK or ATTACHMENT_MASK

private const val REVERT_BTN  = "revert_btn"
private const val SAVE_AS_BTN = "save_as_btn"
private const val SAVE_BTN    = "save_btn"

class ShopUrlDispatcher {
    fun resolveUrl(wearableType: WearableType.EType, sex: ESex): String {
        val prefix = "MarketplaceURL"
        val sexStr = if (sex == ESex.MALE) "Male" else "Female"
        val settingName = when (wearableType) {
            WearableType.EType.ALPHA,
            WearableType.EType.NONE,
            WearableType.EType.INVALID,
            WearableType.EType.COUNT -> prefix
            else -> "${prefix}_${WearableType.instance.getTypeName(wearableType)}$sexStr"
        }
        return ViewerControl.savedSettings.getString(settingName)
    }

    fun resolveUrl(assetType: AssetType.EType, sex: ESex): String {
        val prefix = "MarketplaceURL"
        val sexStr = if (sex == ESex.MALE) "Male" else "Female"
        val settingName = when (assetType) {
            AssetType.EType.CLOTHING,
            AssetType.EType.OBJECT,
            AssetType.EType.BODYPART -> "${prefix}_${AssetType.lookup(assetType)}$sexStr"
            else -> prefix
        }
        return ViewerControl.savedSettings.getString(settingName)
    }
}

data class LookItemType(val displayName: String = "NONE", val inventoryMask: ULong = 0UL)

class FilterItem(val displayName: String = "NONE", val collector: InventoryCollectFunctor? = null)

class CofDragAndDropObserver(private val model: InventoryModel?) : InventoryAddItemByAssetObserver() {
    init {
        model?.addObserver(this)
    }

    override fun done() {
        AppearanceMgr.instance.updateAppearanceFromCof()
    }

    fun detach() {
        if (model != null && model.containsObserver(this)) {
            model.removeObserver(this)
        }
    }
}

class PanelOutfitEdit : Panel() {

    enum class FolderViewItemType { ALL, WEARABLE, ATTACHMENT }
    enum class ListViewItemType {
        ALL, CLOTHING, BODYPART, ATTACHMENT,
        SHAPE, SKIN, HAIR, EYES,
        SHIRT, PANTS, SHOES, SOCKS, JACKET, GLOVES,
        UNDERSHIRT, UNDERPANTS, SKIRT, ALPHA, TATTOO, PHYSICS, UNIVERSAL
    }

    private data class SelectionInfo(val type: WearableType.EType, val count: Int)

    private var currentOutfitName: TextBox? = null
    private var status: TextBox? = null
    private var inventoryItemsPanel: InventoryPanel? = null
    private var searchFilter: FilterEditor? = null
    private val savedFolderState = SaveFolderState().also { it.setApply(false) }
    private var searchString: String = ""
    private var folderViewBtn: Button? = null
    private var listViewBtn: Button? = null
    private var plusBtn: Button? = null
    private var showAddWearablesBtn: Button? = null
    private var filterBtn: Button? = null
    private var addWearablesPanel: Panel? = null
    private var outfitNameStatusPanel: Panel? = null
    private var loadingIndicator: LoadingIndicator? = null
    private var filterPanel: View? = null
    private var noAddWearablesButtonBar: UICtrl? = null
    private var addWearablesButtonBar: UICtrl? = null
    private var avatarComplexityLabel: TextBox? = null
    private var avatarComplexityAddingLabel: TextBox? = null
    private var folderViewFilterCmbBox: ComboBox? = null
    private var listViewFilterCmbBox: ComboBox? = null
    private var wearableListManager: FilteredWearableListManager? = null
    private var wearableItemsList: WearableItemsList? = null
    private var wearablesListViewPanel: Panel? = null
    private var wearableListViewItemsComparator: WearableItemTypeNameComparator? = null
    private var cofDragAndDropObserver: CofDragAndDropObserver? = null
    private val folderViewItemTypes: MutableList<LookItemType> = MutableList(FolderViewItemType.values().size) { LookItemType() }
    private val listViewItemTypes: MutableList<FilterItem> = mutableListOf()
    private var cofWearables: CofWearables? = null
    private var gearMenu: ToggleableMenu? = null
    private var addWearablesGearMenu: ToggleableMenu? = null
    private var initialized = false
    private var wearablesGearMenuBtn: MenuButton? = null
    private var gearMenuBtn: MenuButton? = null

    init {
        val observer = OutfitObserver.instance
        observer.addBofReplacedCallback   { updateCurrentOutfitName() }
        observer.addBofChangedCallback    { updateVerbs() }
        observer.addOutfitLockChangedCallback { updateVerbs() }
        observer.addCofChangedCallback    { onCofChanged() }

        AgentWearables.addLoadingStartedCallback { onOutfitChanging(true) }
        AgentWearables.addLoadedCallback          { onOutfitChanging(false) }
    }

    override fun postBuild(): Boolean {
        folderViewItemTypes[FolderViewItemType.ALL.ordinal]        = LookItemType(getString("Filter.All"),          ALL_ITEMS_MASK.toULong())
        folderViewItemTypes[FolderViewItemType.WEARABLE.ordinal]   = LookItemType(getString("Filter.Clothes/Body"), WEARABLE_MASK.toULong())
        folderViewItemTypes[FolderViewItemType.ATTACHMENT.ordinal] = LookItemType(getString("Filter.Objects"),      ATTACHMENT_MASK.toULong())

        listViewItemTypes.add(FilterItem(getString("Filter.All"),       FindNonLinksByMask(ALL_ITEMS_MASK)))
        listViewItemTypes.add(FilterItem(getString("Filter.Clothing"),  IsTypeActual(AssetType.EType.CLOTHING)))
        listViewItemTypes.add(FilterItem(getString("Filter.Bodyparts"), IsTypeActual(AssetType.EType.BODYPART)))
        listViewItemTypes.add(FilterItem(getString("Filter.Objects"),   FindNonLinksByMask(ATTACHMENT_MASK)))
        listViewItemTypes.add(FilterItem(Trans.getString("shape"),      FindActualWearablesOfType(WearableType.EType.SHAPE)))
        listViewItemTypes.add(FilterItem(Trans.getString("skin"),       FindActualWearablesOfType(WearableType.EType.SKIN)))
        listViewItemTypes.add(FilterItem(Trans.getString("hair"),       FindActualWearablesOfType(WearableType.EType.HAIR)))
        listViewItemTypes.add(FilterItem(Trans.getString("eyes"),       FindActualWearablesOfType(WearableType.EType.EYES)))
        listViewItemTypes.add(FilterItem(Trans.getString("shirt"),      FindActualWearablesOfType(WearableType.EType.SHIRT)))
        listViewItemTypes.add(FilterItem(Trans.getString("pants"),      FindActualWearablesOfType(WearableType.EType.PANTS)))
        listViewItemTypes.add(FilterItem(Trans.getString("shoes"),      FindActualWearablesOfType(WearableType.EType.SHOES)))
        listViewItemTypes.add(FilterItem(Trans.getString("socks"),      FindActualWearablesOfType(WearableType.EType.SOCKS)))
        listViewItemTypes.add(FilterItem(Trans.getString("jacket"),     FindActualWearablesOfType(WearableType.EType.JACKET)))
        listViewItemTypes.add(FilterItem(Trans.getString("gloves"),     FindActualWearablesOfType(WearableType.EType.GLOVES)))
        listViewItemTypes.add(FilterItem(Trans.getString("undershirt"), FindActualWearablesOfType(WearableType.EType.UNDERSHIRT)))
        listViewItemTypes.add(FilterItem(Trans.getString("underpants"), FindActualWearablesOfType(WearableType.EType.UNDERPANTS)))
        listViewItemTypes.add(FilterItem(Trans.getString("skirt"),      FindActualWearablesOfType(WearableType.EType.SKIRT)))
        listViewItemTypes.add(FilterItem(Trans.getString("alpha"),      FindActualWearablesOfType(WearableType.EType.ALPHA)))
        listViewItemTypes.add(FilterItem(Trans.getString("tattoo"),     FindActualWearablesOfType(WearableType.EType.TATTOO)))
        listViewItemTypes.add(FilterItem(Trans.getString("physics"),    FindActualWearablesOfType(WearableType.EType.PHYSICS)))
        listViewItemTypes.add(FilterItem(Trans.getString("universal"),  FindActualWearablesOfType(WearableType.EType.UNIVERSAL)))

        currentOutfitName = getChild("curr_outfit_name")
        status            = getChild("status")
        folderViewBtn     = getChild("folder_view_btn")
        listViewBtn       = getChild("list_view_btn")
        filterPanel       = getChild("filter_panel")
        filterBtn         = getChild<Button>("filter_button").also {
            it.setCommitCallback { showWearablesFilter() }
        }

        setChildCommitCallback("folder_view_btn") { showWearablesFolderView(); saveListSelection() }
        setChildCommitCallback("list_view_btn")   { showWearablesListView();   saveListSelection() }
        setChildCommitCallback("shop_btn_1")      { onShopButtonClicked() }
        setChildCommitCallback("shop_btn_2")      { onShopButtonClicked() }

        setVisibleCallback { visible -> onVisibilityChanged(visible) }

        wearablesGearMenuBtn = getChild("wearables_gear_menu_btn")
        gearMenuBtn          = getChild("gear_menu_btn")

        cofWearables = findChild<CofWearables>("cof_wearables_list")?.also {
            it.setCommitCallback { filterWearablesBySelectedItem() }
            it.cofCallbacks.addWearable      = { onAddWearableClicked() }
            it.cofCallbacks.editWearable     = { onEditWearableClicked() }
            it.cofCallbacks.deleteWearable   = { onRemoveFromOutfitClicked() }
            it.cofCallbacks.moveWearableCloser   = { moveWearable(true) }
            it.cofCallbacks.moveWearableFurther  = { moveWearable(false) }
        }

        addWearablesPanel = getChild("add_wearables_panel")

        inventoryItemsPanel = getChild<InventoryPanel>("folder_view").also {
            it.setFilterTypes(ALL_ITEMS_MASK)
            it.setShowFolderState(InventoryFilter.ShowFolderState.SHOW_NON_EMPTY_FOLDERS)
            it.setSelectCallback { updatePlusButton() }
            it.rootFolder.setReshapeCallback { updatePlusButton() }
        }

        cofDragAndDropObserver = CofDragAndDropObserver(inventoryItemsPanel?.model)

        folderViewFilterCmbBox = getChild<ComboBox>("folder_view_filter_combobox").also {
            it.setCommitCallback { ctrl -> onFolderViewFilterCommitted(ctrl) }
            it.removeAll()
            for (type in folderViewItemTypes) it.add(type.displayName)
            it.setCurrentByIndex(FolderViewItemType.ALL.ordinal)
        }

        listViewFilterCmbBox = getChild<ComboBox>("list_view_filter_combobox").also {
            it.setCommitCallback { ctrl -> onListViewFilterCommitted(ctrl) }
            it.removeAll()
            for (type in listViewItemTypes) it.add(type.displayName)
            it.setCurrentByIndex(ListViewItemType.ALL.ordinal)
        }

        searchFilter = getChild<FilterEditor>("look_item_filter").also {
            it.setCommitCallback { _, value -> onSearchEdit(value.asString()) }
        }

        showAddWearablesBtn = getChild<Button>("show_add_wearables_btn").also {
            it.setClickedCallback { onAddMoreButtonClicked() }
        }

        plusBtn = getChild<Button>("plus_btn").also {
            it.setClickedCallback { onPlusBtnClicked() }
        }

        childSetAction(REVERT_BTN) { AppearanceMgr.instance.wearBaseOutfit() }

        noAddWearablesButtonBar = getChild("no_add_wearables_button_bar")
        addWearablesButtonBar   = getChild("add_wearables_button_bar")

        wearableListViewItemsComparator = WearableItemTypeNameComparator().also {
            it.setOrder(AssetType.EType.CLOTHING, WearableItemTypeNameComparator.ORDER_RANK_1, false, true)
        }

        wearablesListViewPanel = getChild("filtered_wearables_panel")
        wearableItemsList = getChild<WearableItemsList>("list_view").also {
            it.setCommitOnSelectionChange(true)
            it.setCommitCallback { updatePlusButton() }
            it.setDoubleClickCallback { onPlusBtnClicked() }
            it.setComparator(wearableListViewItemsComparator)
        }

        addWearablesGearMenu = AddWearablesGearMenu.create(wearableItemsList!!, inventoryItemsPanel!!)
        wearablesGearMenuBtn?.setMenu(addWearablesGearMenu)

        gearMenu = PanelOutfitEditGearMenu.create()
        gearMenuBtn?.setMenu(gearMenu)

        getChild<Button>(SAVE_BTN).setCommitCallback    { saveOutfit(false) }
        getChild<Button>(SAVE_AS_BTN).setCommitCallback { saveOutfit(true) }

        loadingIndicator     = getChild("edit_outfit_loading_indicator")
        outfitNameStatusPanel = getChild("outfit_name_and_status")

        avatarComplexityLabel       = getChild("avatar_complexity_label")
        avatarComplexityAddingLabel = getChild("avatar_complexity_adding_label")

        onOutfitChanging(AgentWearables.isCofChangeInProgress)

        return true
    }

    override fun onOpen(key: LLSD) {
        if (!initialized) {
            wearableListManager = FilteredWearableListManager(
                wearableItemsList!!, listViewItemTypes[ListViewItemType.ALL.ordinal].collector
            )
            displayCurrentOutfit()
            initialized = true
        }
    }

    fun moveWearable(closerToBody: Boolean) {
        val itemId = cofWearables?.selectedUuid ?: return
        if (itemId == NULL_UUID) return
        val wearable = Inventory.getItem(itemId)
        AppearanceMgr.instance.moveWearable(wearable, closerToBody)
    }

    fun toggleAddWearablesPanel() {
        showAddWearablesPanel(!(addWearablesPanel?.isVisible ?: false))
    }

    fun showAddWearablesPanel(showAddWearables: Boolean) {
        val show = showAddWearables && !RlvHandler.instance.hasBehaviour(RlvBehaviour.SHOWINV)

        addWearablesPanel?.setVisible(show)
        showAddWearablesBtn?.setValue(show)
        updateFiltersVisibility()
        filterBtn?.setVisible(show)

        if (!show) {
            filterBtn?.setValue(false)
            folderViewFilterCmbBox?.setVisible(false)
            listViewFilterCmbBox?.setVisible(false)
            showWearablesFilter()
            wearableItemsList?.setSortOrder(WearableItemsList.SortOrder.BY_TYPE_NAME)
            wearableItemsList?.goToTop()
        } else {
            wearableListManager?.populateIfNeeded()
        }

        noAddWearablesButtonBar?.setVisible(!show)
        addWearablesButtonBar?.setVisible(show)
    }

    fun showWearablesFilter() {
        val filterVisible = filterBtn?.getValue() ?: false
        filterPanel?.setVisible(filterVisible)
        if (!filterVisible) {
            searchFilter?.clear()
            onSearchEdit("")
        } else {
            searchFilter?.setFocus(true)
        }
    }

    fun showWearablesListView() {
        if (switchPanels(inventoryItemsPanel, wearablesListViewPanel)) {
            updateWearablesPanelVerbButtons()
            updateFiltersVisibility()
            wearableListManager?.populateIfNeeded()
        }
        listViewBtn?.setToggleState(true)
    }

    fun showWearablesFolderView() {
        if (switchPanels(wearablesListViewPanel, inventoryItemsPanel)) {
            updateWearablesPanelVerbButtons()
            updateFiltersVisibility()
        }
        folderViewBtn?.setToggleState(true)
    }

    fun updateFiltersVisibility() {
        listViewFilterCmbBox?.setVisible(wearablesListViewPanel?.isVisible ?: false)
        folderViewFilterCmbBox?.setVisible(inventoryItemsPanel?.isVisible ?: false)
    }

    fun onFolderViewFilterCommitted(ctrl: UICtrl) {
        val currFilterType = folderViewFilterCmbBox?.currentIndex ?: return
        if (currFilterType < 0) return
        inventoryItemsPanel?.setFilterTypes(folderViewItemTypes[currFilterType].inventoryMask.toLong())
        savedFolderState.setApply(true)
        inventoryItemsPanel?.rootFolder?.applyFunctorRecursively(savedFolderState)
        val opener = OpenFoldersWithSelection()
        inventoryItemsPanel?.rootFolder?.applyFunctorRecursively(opener)
        inventoryItemsPanel?.rootFolder?.scrollToShowSelection()
        if (!InventoryModelBackgroundFetch.instance.inventoryFetchStarted) {
            InventoryModelBackgroundFetch.instance.start()
        }
    }

    fun onListViewFilterCommitted(ctrl: UICtrl) {
        val currFilterType = listViewFilterCmbBox?.currentIndex ?: return
        if (currFilterType < 0) return
        if (currFilterType >= ListViewItemType.SHAPE.ordinal) {
            wearableItemsList?.setMenuWearableType(WearableType.EType.values()[currFilterType - ListViewItemType.SHAPE.ordinal])
        }
        wearableListManager?.setFilterCollector(listViewItemTypes[currFilterType].collector)
    }

    fun onSearchEdit(string: String) {
        if (searchString != string) {
            searchString = string.uppercase().trimStart()
        }

        if (searchString.isEmpty()) {
            inventoryItemsPanel?.setFilterSubString("")
            wearableItemsList?.setFilterSubString("", true)
            savedFolderState.setApply(true)
            inventoryItemsPanel?.rootFolder?.applyFunctorRecursively(savedFolderState)
            val opener = OpenFoldersWithSelection()
            inventoryItemsPanel?.rootFolder?.applyFunctorRecursively(opener)
            inventoryItemsPanel?.rootFolder?.scrollToShowSelection()
        }

        if (!InventoryModelBackgroundFetch.instance.inventoryFetchStarted) {
            InventoryModelBackgroundFetch.instance.start()
        }

        val currentFilter = inventoryItemsPanel?.filterSubString ?: ""
        if (currentFilter.isEmpty() && searchString.isEmpty()) return

        if (currentFilter.isEmpty()) {
            savedFolderState.setApply(false)
            inventoryItemsPanel?.rootFolder?.applyFunctorRecursively(savedFolderState)
        }

        inventoryItemsPanel?.setFilterSubString(searchString)
        wearableItemsList?.setFilterSubString(searchString, true)
    }

    fun updatePlusButton() {
        val selectedItems = mutableListOf<UUID>()
        getSelectedItemsUuid(selectedItems)
        if (selectedItems.isEmpty()) {
            plusBtn?.setEnabled(false)
            return
        }

        val canAdd = selectedItems.all { canItemBeWorn(it) }
        plusBtn?.setEnabled(canAdd)

        val firstItem = Inventory.getItem(selectedItems.first())
        if (canAdd && firstItem != null && selectedItems.size == 1 && firstItem.type == AssetType.EType.BODYPART) {
            plusBtn?.setToolTip(getString("replace_body_part"))
        } else {
            plusBtn?.setToolTip("")
        }
    }

    fun onPlusBtnClicked() {
        val selectedItems = mutableListOf<UUID>()
        getSelectedItemsUuid(selectedItems)
        val linkWaiter = UpdateAppearanceOnDestroy()
        for (selectedId in selectedItems) {
            AppearanceMgr.instance.wearItemOnAvatar(selectedId, false, true, linkWaiter)
        }
    }

    fun onVisibilityChanged(inVisibleChain: Boolean) {
        showAddWearablesPanel(false)
        wearableItemsList?.resetSelection()
        inventoryItemsPanel?.clearSelection()
        if (inVisibleChain) {
            update()
        } else {
            wearableListManager?.holdProgress()
        }
    }

    fun applyFolderViewFilter(type: FolderViewItemType) {
        folderViewFilterCmbBox?.setCurrentByIndex(type.ordinal)
        folderViewFilterCmbBox?.onCommit()
    }

    fun applyListViewFilter(type: ListViewItemType) {
        listViewFilterCmbBox?.setCurrentByIndex(type.ordinal)
        listViewFilterCmbBox?.onCommit()
    }

    fun filterWearablesBySelectedItem() {
        if (!(addWearablesPanel?.isVisible ?: false)) return

        val ids = mutableListOf<UUID>()
        cofWearables?.getSelectedUuids(ids)

        val nothingSelected    = ids.isEmpty()
        val oneSelected        = ids.size == 1
        val moreThanOneSelected = ids.size > 1
        val isDummyItem        = ids.isNotEmpty() && cofWearables?.selectedItem is PanelDummyClothingListItem

        if (nothingSelected) {
            if (inventoryItemsPanel?.isVisible == true) return
            showWearablesListView()
            var type = cofWearables?.selectedAccordionAssetType ?: AssetType.EType.NONE
            if (type == AssetType.EType.NONE) {
                val selectedItemId = wearableItemsList?.selectedUuid
                val item = Inventory.getLinkedItem(selectedItemId)
                if (item != null) {
                    showFilteredWearablesListView(item.wearableType)
                    return
                }
                type = cofWearables?.expandedAccordionAssetType ?: AssetType.EType.NONE
            }
            when (type) {
                AssetType.EType.OBJECT   -> applyListViewFilter(ListViewItemType.ATTACHMENT)
                AssetType.EType.BODYPART -> applyListViewFilter(ListViewItemType.BODYPART)
                else                     -> applyListViewFilter(ListViewItemType.CLOTHING)
            }
            return
        }

        if (moreThanOneSelected) {
            if (inventoryItemsPanel?.isVisible == true) {
                applyFolderViewFilter(FolderViewItemType.ALL)
                return
            }
            showWearablesListView()
            applyListViewFilter(ListViewItemType.ALL)
            return
        }

        if (oneSelected && isDummyItem) {
            if (inventoryItemsPanel?.isVisible == true) {
                applyFolderViewFilter(FolderViewItemType.WEARABLE)
                return
            }
            onAddWearableClicked()
            return
        }

        val item = Inventory.getItem(ids[0])
        if (item == null && ids[0] != NULL_UUID) {
            if (inventoryItemsPanel?.isVisible == true) {
                applyFolderViewFilter(FolderViewItemType.ALL)
                return
            }
            showWearablesListView()
            applyListViewFilter(ListViewItemType.ALL)
            return
        }

        if (item != null && oneSelected && !isDummyItem) {
            if (item.isWearableType) {
                if (inventoryItemsPanel?.isVisible == true) {
                    applyFolderViewFilter(FolderViewItemType.WEARABLE)
                    return
                }
                showFilteredWearablesListView(item.wearableType)
            } else {
                if (inventoryItemsPanel?.isVisible == true) {
                    applyFolderViewFilter(FolderViewItemType.ATTACHMENT)
                    return
                }
                showWearablesListView()
                applyListViewFilter(ListViewItemType.ATTACHMENT)
            }
        }
    }

    fun onRemoveFromOutfitClicked() {
        val idToRemove = cofWearables?.selectedUuid ?: return
        val type = getWearableTypeByItemUuid(idToRemove)
        AppearanceMgr.instance.removeItemFromAvatar(idToRemove)
        if (cofWearables?.selectedItem == null) {
            cofWearables?.selectClothing(type)
        }
    }

    fun onEditWearableClicked() {
        val selectedItemId = cofWearables?.selectedUuid ?: return
        if (selectedItemId != NULL_UUID) {
            AgentWearables.editWearable(selectedItemId)
        }
    }

    fun onAddWearableClicked() {
        val item = cofWearables?.selectedItem as? PanelDummyClothingListItem ?: return
        showFilteredWearablesListView(item.wearableType)
    }

    fun onReplaceMenuItemClicked(selectedItemId: UUID) {
        val item = Inventory.getLinkedItem(selectedItemId) ?: return
        showFilteredWearablesListView(item.wearableType)
    }

    fun onShopButtonClicked() {
        val urlResolver = ShopUrlDispatcher()
        var url = ""

        if (isAgentAvatarValid()) {
            val selectionInfo = getAddMorePanelSelectionType()
            val type = selectionInfo.type
            url = if (selectionInfo.count > 1) {
                urlResolver.resolveUrl(WearableType.EType.NONE, ESex.FEMALE)
            } else {
                val resolvedType = if (type == WearableType.EType.NONE) getCofWearablesSelectionType() else type
                val sex = AgentAvatarSelf.sex
                if (resolvedType != WearableType.EType.INVALID && resolvedType != WearableType.EType.NONE) {
                    urlResolver.resolveUrl(resolvedType, sex)
                } else {
                    urlResolver.resolveUrl(cofWearables?.expandedAccordionAssetType ?: AssetType.EType.NONE, sex)
                }
            }
        } else {
            url = urlResolver.resolveUrl(WearableType.EType.NONE, ESex.FEMALE)
        }

        Web.loadUrl(url)
    }

    fun displayCurrentOutfit() {
        if (!isVisible) setVisible(true)
        updateCurrentOutfitName()
        update()
    }

    fun updateCurrentOutfitName() {
        val name = AppearanceMgr.instance.getBaseOutfitName()
        currentOutfitName?.setText(name ?: getString("No Outfit"))
    }

    fun update() {
        cofWearables?.refresh()
        updateVerbs()
    }

    fun updateVerbs() {
        val outfitIsDirty  = AppearanceMgr.instance.isOutfitDirty
        val outfitLocked   = AppearanceMgr.instance.isOutfitLocked
        val hasBaseOutfit  = AppearanceMgr.instance.baseOutfitUuid != NULL_UUID

        getChildView(SAVE_BTN).setEnabled(!outfitLocked && outfitIsDirty)
        getChildView(REVERT_BTN).setEnabled(outfitIsDirty && hasBaseOutfit)

        status?.setText(if (outfitIsDirty) getString("unsaved_changes") else getString("now_editing"))

        updateCurrentOutfitName()
        updatePlusButton()
    }

    fun switchPanels(switchFrom: Panel?, switchTo: Panel?): Boolean {
        if (switchFrom != null && switchTo != null && !(switchTo.isVisible)) {
            switchFrom.setVisible(false)
            switchTo.setVisible(true)
            return true
        }
        return false
    }

    fun resetAccordionState() {
        cofWearables?.expandDefaultAccordionTab()
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: AcceptanceRef, tooltipMsg: StringBuilder
    ): Boolean {
        if (cargoData == null) return true

        when (cargoType) {
            DragAndDropType.BODYPART,
            DragAndDropType.CLOTHING,
            DragAndDropType.OBJECT,
            DragAndDropType.LINK -> accept.value = Acceptance.YES_MULTI
            else                 -> accept.value = Acceptance.NO
        }

        if (drop) {
            val item = cargoData as InventoryItem
            if (AssetType.lookupIsAssetIdKnowable(item.type)) {
                cofDragAndDropObserver?.watchAsset(item.assetUuid)
                AppearanceMgr.instance.addCofItemLink(item.linkedUuid)
            } else {
                AppearanceMgr.instance.addCofItemLink(
                    item.linkedUuid,
                    UpdateAppearanceAndEditWearableOnDestroy(item.uuid)
                )
            }
        }

        return true
    }

    fun updateAvatarComplexity(complexity: UInt) {
        val complexityString = complexity.toString()
        avatarComplexityLabel?.setTextArg("[WEIGHT]", complexityString)
        avatarComplexityAddingLabel?.setTextArg("[WEIGHT]", complexityString)
    }

    private fun onAddMoreButtonClicked() {
        toggleAddWearablesPanel()
        filterWearablesBySelectedItem()
    }

    private fun showFilteredWearablesListView(type: WearableType.EType) {
        showAddWearablesPanel(true)
        showWearablesListView()
        applyListViewFilter(ListViewItemType.values()[ListViewItemType.SHAPE.ordinal + type.ordinal])
        wearableItemsList?.setMenuWearableType(type)
    }

    private fun onOutfitChanging(started: Boolean) {
        val panelWidth    = outfitNameStatusPanel?.rect?.width ?: 0
        val indicatorLeft = loadingIndicator?.rect?.left ?: 0
        val delta         = if (started) panelWidth - indicatorLeft else 0
        val rightBorder   = panelWidth - delta
        currentOutfitName?.let { updateStatusWidgetRect(it, rightBorder) }
        status?.let            { updateStatusWidgetRect(it, rightBorder) }
        loadingIndicator?.setVisible(started)
    }

    private fun updateStatusWidgetRect(widget: View, rightBorder: Int) {
        val rect = widget.rect.copy(right = rightBorder)
        widget.setShape(rect)
    }

    private fun getCurrentItemUuid(): UUID? {
        return if (inventoryItemsPanel?.isVisible == true) {
            val currItem = inventoryItemsPanel?.rootFolder?.currentSelectedItem ?: return null
            (currItem.viewModelItem as? FolderViewModelItemInventory)?.uuid
        } else if (wearablesListViewPanel?.isVisible == true) {
            wearableItemsList?.selectedUuid
        } else null
    }

    private fun getSelectedItemsUuid(uuidList: MutableList<UUID>) {
        if (inventoryItemsPanel?.isVisible == true) {
            val itemSet = inventoryItemsPanel?.rootFolder?.selectionList ?: return
            for (it in itemSet) {
                uuidList.add((it.viewModelItem as FolderViewModelItemInventory).uuid)
            }
        } else if (wearablesListViewPanel?.isVisible == true) {
            val itemSet = mutableListOf<LLSD>()
            wearableItemsList?.getSelectedValues(itemSet)
            itemSet.mapTo(uuidList) { it.asUuid() }
        }
    }

    private fun getCofWearablesSelectionType(): WearableType.EType {
        val selectedItems = mutableListOf<Panel>()
        cofWearables?.getSelectedItems(selectedItems)
        if (selectedItems.size != 1) return WearableType.EType.NONE
        val item = selectedItems.first()
        return when (item) {
            is PanelDummyClothingListItem    -> item.wearableType
            is PanelInventoryListItemBase    -> item.wearableType
            else                             -> WearableType.EType.NONE
        }
    }

    private fun getAddMorePanelSelectionType(): SelectionInfo {
        if (addWearablesPanel?.isVisible != true) return SelectionInfo(WearableType.EType.NONE, 0)
        if (inventoryItemsPanel?.isVisible == true) {
            val selected = inventoryItemsPanel?.rootFolder?.selectionList ?: emptySet()
            val count = selected.size
            val type = if (count == 1)
                getWearableTypeByItemUuid((selected.first().viewModelItem as FolderViewModelItemInventory).uuid)
            else WearableType.EType.NONE
            return SelectionInfo(type, count)
        }
        if (wearableItemsList?.isVisible == true) {
            val selected = mutableListOf<UUID>()
            wearableItemsList?.getSelectedUuids(selected)
            val count = selected.size
            val type = if (count == 1) getWearableTypeByItemUuid(selected.first()) else WearableType.EType.NONE
            return SelectionInfo(type, count)
        }
        return SelectionInfo(WearableType.EType.NONE, 0)
    }

    private fun getWearableTypeByItemUuid(itemUuid: UUID): WearableType.EType {
        val item = Inventory.getLinkedItem(itemUuid) ?: return WearableType.EType.NONE
        return item.wearableType
    }

    private fun onCofChanged() {
        update()
    }

    private fun saveListSelection() {
        if (wearablesListViewPanel?.isVisible == true) {
            val selectedIds = inventoryItemsPanel?.rootFolder?.selectionList ?: return
            if (selectedIds.isEmpty()) return
            for (item in selectedIds) {
                wearableItemsList?.selectItemByUuid(
                    (item.viewModelItem as FolderViewModelItemInventory).uuid, true
                )
            }
            wearableItemsList?.scrollToShowFirstSelectedItem()
        } else if (inventoryItemsPanel?.isVisible == true) {
            val selectedIds = mutableListOf<UUID>()
            wearableItemsList?.getSelectedUuids(selectedIds)
            if (selectedIds.isEmpty()) return
            inventoryItemsPanel?.clearSelection()
            val root = inventoryItemsPanel?.rootFolder ?: return
            for (itemId in selectedIds) {
                val item = inventoryItemsPanel?.getItemById(itemId) ?: continue
                item.parentFolder?.setOpenArrangeRecursively(true, FolderViewFolder.RecurseMode.UP)
                root.changeSelection(item, true)
            }
            root.scrollToShowSelection()
        }
    }

    private fun updateWearablesPanelVerbButtons() {
        if (wearablesListViewPanel?.isVisible == true) {
            folderViewBtn?.setToggleState(false)
            folderViewBtn?.setImageOverlay(getString("folder_view_off"), folderViewBtn?.imageOverlayHAlign)
            listViewBtn?.setImageOverlay(getString("list_view_on"), listViewBtn?.imageOverlayHAlign)
        } else if (inventoryItemsPanel?.isVisible == true) {
            listViewBtn?.setToggleState(false)
            listViewBtn?.setImageOverlay(getString("list_view_off"), listViewBtn?.imageOverlayHAlign)
            folderViewBtn?.setImageOverlay(getString("folder_view_on"), folderViewBtn?.imageOverlayHAlign)
        }
    }

    private fun saveOutfit(asNew: Boolean) {
        PanelOutfitsInventory.findInstance()?.saveOutfit(asNew)
    }
}
