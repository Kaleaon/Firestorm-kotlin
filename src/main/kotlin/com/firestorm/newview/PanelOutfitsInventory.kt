package com.firestorm.newview

import com.firestorm.inventory.InventoryCategoriesObserver
import com.firestorm.inventory.InventoryModelBackgroundFetch
import com.firestorm.ui.Button
import com.firestorm.ui.LLSD
import com.firestorm.ui.MenuButton
import com.firestorm.ui.Panel
import com.firestorm.ui.TabContainer
import com.firestorm.ui.ToggleableMenu
import java.util.UUID

private const val OUTFITS_TAB_NAME        = "outfitslist_tab"
private const val OUTFIT_GALLERY_TAB_NAME = "outfit_gallery_tab"
private const val COF_TAB_NAME            = "cof_tab"
private const val SAVE_AS_BTN             = "save_as_btn"
private const val SAVE_BTN                = "save_btn"

class PanelOutfitsInventory : Panel() {

    private var appearanceTabs: TabContainer? = null

    private val categoriesObserver: InventoryCategoriesObserver = InventoryCategoriesObserver()
    private var currentTempAttachmentCount: UInt = 0u
    private val tempAttachmentUpdateTimer = FrameTimer()

    private var activePanel: PanelAppearanceTab? = null
    private var myOutfitsPanel: OutfitsList? = null
    private var outfitGalleryPanel: OutfitGallery? = null
    private var currentOutfitPanel: PanelWearing? = null

    private var listCommands: Panel? = null
    private var wearBtn: Button? = null

    private var initialized = false

    private var gearMenu: MenuButton? = null
    private var sortMenu: MenuButton? = null
    private var trashBtn: Button? = null
    private var sortMenuPanel: Panel? = null
    private var trashMenuPanel: Panel? = null
    private var gearMenuConnection: Connection? = null
    private var sortMenuConnection: Connection? = null
    private var trashMenuConnection: Connection? = null

    init {
        AgentWearables.addLoadedCallback          { onWearablesLoaded() }
        AgentWearables.addLoadingStartedCallback  { onWearablesLoading() }

        val observer = OutfitObserver.instance
        observer.addBofChangedCallback        { updateVerbs() }
        observer.addCofChangedCallback        { updateVerbs() }
        observer.addOutfitLockChangedCallback { updateVerbs() }
    }

    override fun postBuild(): Boolean {
        initTabPanels()
        initListCommandsHandlers()

        val outfitsCat = Inventory.findCategoryUUIDForType(FolderType.MY_OUTFITS)
        if (outfitsCat != NULL_UUID) {
            InventoryModelBackgroundFetch.instance.start(outfitsCat)
        }

        getChild<Button>(SAVE_BTN).setCommitCallback    { saveOutfit(false) }
        getChild<Button>(SAVE_AS_BTN).setCommitCallback { saveOutfit(true) }

        tempAttachmentUpdateTimer.start()

        return true
    }

    override fun onOpen(key: LLSD) {
        if (!initialized) {
            val panelAppearance = getAppearanceSp()
            panelAppearance?.fetchInventory()
            panelAppearance?.refreshCurrentOutfitName()

            Inventory.addObserver(categoriesObserver)
            categoriesObserver.addCategory(AppearanceMgr.instance.getCof()) { onCofChanged() }
            onCofChanged()

            if (!appearanceTabs!!.selectTab(ViewerControl.savedSettings.getInt("LastAppearanceTab"))) {
                appearanceTabs!!.selectFirstTab()
            }

            initialized = true
        }

        onTabChange()
    }

    override fun draw() {
        if (tempAttachmentUpdateTimer.checkExpirationAndReset(1f)) {
            val tempCount = AgentWearables.tempAttachments.size.toUInt()
            if (tempCount != currentTempAttachmentCount) {
                currentTempAttachmentCount = tempCount
                onCofChanged()
            }
        }
        super.draw()
    }

    fun onDestroy() {
        if (appearanceTabs != null && initialized) {
            ViewerControl.savedSettings.setInt("LastAppearanceTab", appearanceTabs!!.currentPanelIndex)
        }
        gearMenuConnection?.disconnect()
        sortMenuConnection?.disconnect()
        trashMenuConnection?.disconnect()

        if (Inventory.containsObserver(categoriesObserver)) {
            Inventory.removeObserver(categoriesObserver)
        }
    }

    fun onSearchEdit(string: String) {
        val panel = activePanel ?: return
        if (!InventoryModelBackgroundFetch.instance.inventoryFetchStarted) {
            InventoryModelBackgroundFetch.instance.start()
        }
        panel.setFilterSubString(string)
    }

    fun onSave() {
        val outfitName = AppearanceMgr.instance.getBaseOutfitName()
            ?: ViewerFolderType.lookupNewCategoryName(FolderType.OUTFIT)

        val args = LLSD.emptyMap()
        args["DESC"] = outfitName

        NotificationsUtil.add("SaveOutfitAs", args, LLSD.emptyMap()) { notification, response ->
            onSaveCommit(notification, response)
        }
    }

    fun onSaveCommit(notification: LLSD, response: LLSD): Boolean {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) {
            val outfitName = response["message"].asString().trim()
            if (outfitName.isNotEmpty()) {
                AppearanceMgr.instance.makeNewOutfitLinks(outfitName)
                getAppearanceSp()?.showOutfitsInventoryPanel()
                appearanceTabs?.selectTabByName(OUTFITS_TAB_NAME)
            }
        }
        return false
    }

    fun saveOutfit(asNew: Boolean) {
        if (!asNew && AppearanceMgr.instance.updateBaseOutfit()) return
        onSave()
    }

    fun isCofPanelActive(): Boolean = activePanel?.name == COF_TAB_NAME

    fun openAppearanceTab(tabName: String) {
        appearanceTabs?.selectTabByName(tabName)
    }

    fun setMenuButtons(
        gearMenu: MenuButton,
        sortMenu: MenuButton,
        trashBtn: Button,
        sortMenuPanel: Panel,
        trashMenuPanel: Panel
    ) {
        this.gearMenu      = gearMenu
        this.sortMenu      = sortMenu
        this.trashBtn      = trashBtn
        this.sortMenuPanel = sortMenuPanel
        this.trashMenuPanel = trashMenuPanel

        gearMenuConnection?.disconnect()
        sortMenuConnection?.disconnect()
        trashMenuConnection?.disconnect()

        gearMenuConnection  = gearMenu.setMouseDownCallback  { onGearMouseDown() }
        sortMenuConnection  = sortMenu.setMouseDownCallback  { onGearMouseDown() }
        trashMenuConnection = trashBtn.setClickedCallback    { onTrashButtonClick() }
    }

    fun updateAvatarComplexity(
        complexity: UInt,
        itemComplexity: MutableMap<UUID, UInt>,
        tempItemComplexity: MutableMap<UUID, UInt>,
        bodyPartsComplexity: UInt
    ) {
        outfitGalleryPanel?.updateAvatarComplexity(complexity)
        myOutfitsPanel?.updateAvatarComplexity(complexity)
        currentOutfitPanel?.updateAvatarComplexity(complexity, itemComplexity, tempItemComplexity, bodyPartsComplexity)
    }

    fun getAppearanceTabs(): TabContainer? = appearanceTabs
    fun getMyOutfitsPanel(): OutfitsList?  = myOutfitsPanel
    fun getCurrentOutfitPanel(): PanelWearing? = currentOutfitPanel

    protected fun updateVerbs() {
        if (listCommands != null) updateListCommands()
    }

    private fun onCofChanged() {
        if (!isAgentAvatarValid()) return
        val cof = AppearanceMgr.instance.getCof()
        val objItems = mutableListOf<InventoryItem>()
        val cats     = mutableListOf<InventoryCategory>()
        val isOfType = IsType(AssetType.EType.OBJECT)
        Inventory.collectDescendentsIf(cof, cats, objItems, InventoryModel.EXCLUDE_TRASH, isOfType)
        val attachments = objItems.size.toUInt() + currentTempAttachmentCount

        val args = mutableMapOf<String, String>()
        args["COUNT"] = attachments.toString()
        args["MAX"]   = AgentAvatarSelf.maxAttachments.toString()
        val title = getString("cof_tab_label", args)
        appearanceTabs?.setPanelTitle(
            appearanceTabs!!.getIndexForPanel(currentOutfitPanel), title
        )
    }

    private fun initTabPanels() {
        currentOutfitPanel = findChild<PanelWearing>(COF_TAB_NAME)?.also {
            it.setSelectionChangeCallback { updateVerbs() }
        }
        myOutfitsPanel = findChild<OutfitsList>(OUTFITS_TAB_NAME)?.also {
            it.setSelectionChangeCallback { updateVerbs() }
        }
        outfitGalleryPanel = findChild<OutfitGallery>(OUTFIT_GALLERY_TAB_NAME)?.also {
            it.setSelectionChangeCallback { updateVerbs() }
        }
        appearanceTabs = getChild<TabContainer>("appearance_tabs").also {
            it.setCommitCallback { onTabChange() }
        }
    }

    private fun onTabChange() {
        val tabs = appearanceTabs ?: return
        activePanel = tabs.currentPanel as? PanelAppearanceTab ?: return

        activePanel!!.checkFilterSubString()
        activePanel!!.onOpen(LLSD())

        gearMenu?.setMenu(activePanel!!.gearMenu, MenuButton.Position.BOTTOM_LEFT)

        if (sortMenu != null && sortMenuPanel != null) {
            val menu: ToggleableMenu? = activePanel!!.sortMenu
            if (menu != null) {
                sortMenu!!.setMenu(menu, MenuButton.Position.BOTTOM_LEFT)
                sortMenuPanel!!.setVisible(true)
            } else {
                sortMenuPanel!!.setVisible(false)
            }
        }

        trashMenuPanel?.setVisible(false)

        updateVerbs()
    }

    private fun initListCommandsHandlers() {
        listCommands = getChild("bottom_panel")
        wearBtn = listCommands?.getChild<Button>("wear_btn")?.also {
            it.setCommitCallback { onWearButtonClick() }
        }
        myOutfitsPanel?.childSetAction("trash_btn")     { onTrashButtonClick() }
        outfitGalleryPanel?.childSetAction("trash_btn") { onTrashButtonClick() }
    }

    private fun updateListCommands() {
        val trashEnabled       = isActionEnabled("delete")
        val wearEnabled        = isActionEnabled("wear")
        val wearVisible        = !isCofPanelActive()
        val makeOutfitEnabled  = isActionEnabled("save_outfit")

        myOutfitsPanel?.childSetEnabled("trash_btn",   trashEnabled)
        outfitGalleryPanel?.childSetEnabled("trash_btn", trashEnabled)
        wearBtn?.setEnabled(wearEnabled)
        wearBtn?.setVisible(wearVisible)
        getChild<Button>(SAVE_BTN).setEnabled(makeOutfitEnabled)
        wearBtn?.setToolTip(
            getString(
                if (!isOutfitsGalleryPanelActive() && myOutfitsPanel?.hasItemSelected() == true)
                    "wear_items_tooltip"
                else
                    "wear_outfit_tooltip"
            )
        )
    }

    private fun onWearButtonClick() {
        when {
            isOutfitsListPanelActive() -> {
                if (myOutfitsPanel?.hasItemSelected() == true) {
                    myOutfitsPanel?.wearSelectedItems()
                } else {
                    myOutfitsPanel?.performAction("replaceoutfit")
                }
            }
            isOutfitsGalleryPanelActive() -> outfitGalleryPanel?.wearSelectedOutfit()
        }
    }

    private fun onTrashButtonClick() {
        when {
            isOutfitsListPanelActive()    -> myOutfitsPanel?.removeSelected()
            isOutfitsGalleryPanelActive() -> outfitGalleryPanel?.removeSelected()
        }
    }

    private fun onGearMouseDown() {
        activePanel?.updateMenuItemsVisibility()
    }

    private fun isActionEnabled(userdata: String): Boolean =
        activePanel?.isActionEnabled(LLSD(userdata)) ?: false

    private fun isOutfitsListPanelActive(): Boolean    = activePanel?.name == OUTFITS_TAB_NAME
    private fun isOutfitsGalleryPanelActive(): Boolean = activePanel?.name == OUTFIT_GALLERY_TAB_NAME

    private fun setWearablesLoading(value: Boolean) { updateVerbs() }
    private fun onWearablesLoaded()  { setWearablesLoading(false) }
    private fun onWearablesLoading() { setWearablesLoading(true) }

    companion object {
        fun findInstance(): PanelOutfitsInventory? =
            FloaterSidePanelContainer.getPanel("appearance", "panel_outfits_inventory") as? PanelOutfitsInventory

        fun getAppearanceSp(): SidepanelAppearance? =
            FloaterSidePanelContainer.getPanel("appearance") as? SidepanelAppearance
    }
}
