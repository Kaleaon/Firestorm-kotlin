package com.firestorm.newview

import java.util.UUID

class CurrentlyWornFetchObserver(
    private val ids: MutableList<UUID>,
    private val panel: SidePanelAppearance
) {
    fun startFetch() { System.err.println("CurrentlyWornFetchObserver: startFetch not yet implemented") }
    fun isFinished(): Boolean { return false }
    fun done() {
        panel.inventoryFetched()
    }
}

class SidePanelAppearance : Panel() {

    private var filterEditor: FilterEditor? = null
    private var panelOutfitsInventory: PanelOutfitsInventory? = null
    private var outfitEdit: PanelOutfitEdit? = null
    private var editWearable: PanelEditWearable? = null

    private var openOutfitBtn: Button? = null
    private var editAppearanceBtn: Button? = null
    private var editOutfitBtn: Button? = null
    private var currOutfitPanel: Panel? = null
    private var wearableLoadingIndicator: LoadingIndicator? = null
    private var currentLookName: TextBox? = null
    private var outfitStatus: TextBox? = null

    private var filterSubString: String = ""
    private var opened: Boolean = false
    private var lastAvatarComplexity: UInt = 0u

    init {
        OutfitObserver.instance().addBOFReplacedCallback { refreshCurrentOutfitName("") }
        OutfitObserver.instance().addBOFChangedCallback { refreshCurrentOutfitName("") }
        OutfitObserver.instance().addCOFChangedCallback { refreshCurrentOutfitName("") }

        AgentWearables.instance().addLoadingStartedCallback { setWearablesLoading(true) }
        AgentWearables.instance().addLoadedCallback { setWearablesLoading(false) }
    }

    open fun postBuild(): Boolean {
        openOutfitBtn = getChild<Button>("openoutfit_btn")
        openOutfitBtn?.setClickedCallback { onOpenOutfitButtonClicked() }

        editAppearanceBtn = getChild<Button>("editappearance_btn")
        editAppearanceBtn?.setClickedCallback { onEditAppearanceButtonClicked() }

        childSetAction("edit_outfit_btn") { showOutfitEditPanel() }

        filterEditor = getChild<FilterEditor>("Filter")
        filterEditor?.setCommitCallback { searchString -> onFilterEdit(searchString) }

        panelOutfitsInventory = getChild<Panel>("panel_outfits_inventory") as? PanelOutfitsInventory

        outfitEdit = getChild<Panel>("panel_outfit_edit") as? PanelOutfitEdit
        outfitEdit?.getChild<Button>("back_btn")?.setClickedCallback { showOutfitsInventoryPanel() }

        editWearable = getChild<Panel>("panel_edit_wearable") as? PanelEditWearable
        editWearable?.getChild<Button>("back_btn")?.setClickedCallback { showOutfitEditPanel() }

        currentLookName = getChild<TextBox>("currentlook_name")
        outfitStatus = getChild<TextBox>("currentlook_status")
        currOutfitPanel = getChild<Panel>("panel_currentlook")
        wearableLoadingIndicator = getChild<LoadingIndicator>("wearables_loading_indicator")
        editOutfitBtn = getChild<Button>("edit_outfit_btn")

        setVisibleCallback { newVisibility -> onVisibilityChanged(newVisibility) }
        setWearablesLoading(AgentWearables.instance().isCOFChangeInProgress())

        val gearBtn = getChild<MenuButton>("options_gear_btn")
        val sortBtn = getChild<MenuButton>("sorting_menu_btn")
        val trashBtn = getChild<Button>("trash_btn")
        val sortBtnPanel = getChild<Panel>("options_sort_btn_panel")
        val trashBtnPanel = getChild<Panel>("trash_btn_panel")
        panelOutfitsInventory?.setMenuButtons(gearBtn, sortBtn, trashBtn, sortBtnPanel, trashBtnPanel)

        return true
    }

    open fun onOpen(key: LLSD) {
        if (!key.has("type")) {
            if (!opened) {
                showOutfitsInventoryPanel()
            }
        } else {
            when (key["type"].asString()) {
                "my_outfits" -> showOutfitsInventoryPanel("outfitslist_tab")
                "now_wearing" -> showOutfitsInventoryPanel("cof_tab")
                "edit_outfit" -> showOutfitEditPanel()
                "edit_shape" -> showWearableEditPanel()
            }
        }
        opened = true
    }

    open fun handleKeyHere(key: Key, mask: Mask): Boolean {
        if (FSCommon.isFilterEditorKeyCombo(key, mask)) {
            if (filterEditor?.isVisible() == true) {
                filterEditor?.setFocus(true)
                return true
            } else if (isOutfitEditPanelVisible() && getChildView("filter_panel")?.isVisible() == true) {
                getChild<FilterEditor>("look_item_filter")?.setFocus(true)
                return true
            }
        }
        return super.handleKeyHere(key, mask)
    }

    open fun hasAccelerators(): Boolean = true

    private fun onVisibilityChanged(newVisibility: Boolean) {
        val visibility = LLSD()
        visibility["visible"] = newVisibility
        visibility["reset_accordion"] = false
        updateToVisibility(visibility)
    }

    fun updateToVisibility(newVisibility: LLSD) {
        if (newVisibility["visible"].asBoolean()) {
            val isOutfitEditVisible = outfitEdit?.isVisible() == true
            val isWearableEditVisible = editWearable?.isVisible() == true

            if (isOutfitEditVisible || isWearableEditVisible) {
                val wearablePtr = editWearable?.getWearable() ?: return

                if (!AgentCamera.instance().cameraCustomizeAvatar()) {
                    VOAvatarSelf.onCustomizeStart(
                        WearableType.getInstance().getDisableCameraSwitch(wearablePtr.getType())
                    )
                }

                if (isWearableEditVisible) {
                    if (!AgentWearables.instance().getWearableIndex(wearablePtr, UIntArray(1))) {
                        showOutfitEditPanel()
                    }
                }

                if (isOutfitEditVisible && newVisibility["reset_accordion"].asBoolean()) {
                    outfitEdit?.resetAccordionState()
                }
            }
        } else {
            if (AgentCamera.instance().cameraCustomizeAvatar() &&
                SavedSettings.getBool("AppearanceCameraMovement")
            ) {
                AgentCamera.instance().changeCameraToDefault()
                AgentCamera.instance().resetView()
            }
        }
    }

    private fun onFilterEdit(searchString: String) {
        if (filterSubString != searchString) {
            filterSubString = searchString
            panelOutfitsInventory?.onSearchEdit(filterSubString)
        }
    }

    private fun onOpenOutfitButtonClicked() {
        val outfitLink = AppearanceMgr.getInstance().getBaseOutfitLink() ?: return
        if (!outfitLink.getIsLinkType()) return

        val tabOutfits = panelOutfitsInventory?.findChild<AccordionCtrlTab>("tab_outfits") ?: return
        tabOutfits.changeOpenClose(false)
        val inventoryPanel = tabOutfits.findChild<InventoryPanel>("outfitslist_tab") ?: return
        val root = inventoryPanel.getRootFolder()
        val outfitFolder = inventoryPanel.getItemByID(outfitLink.getLinkedUUID())
        if (outfitFolder != null) {
            outfitFolder.setOpen(!outfitFolder.isOpen())
            root.setSelection(outfitFolder, true)
            root.scrollToShowSelection()
        }
    }

    private fun onEditAppearanceButtonClicked() {
        if (AgentWearables.instance().areWearablesLoaded()) {
            VOAvatarSelf.onCustomizeStart()
        }
    }

    fun showOutfitsInventoryPanel() {
        toggleWearableEditPanel(false)
        toggleOutfitEditPanel(false)
        toggleMyOutfitsPanel(true, "")
    }

    fun showOutfitsInventoryPanel(tabName: String) {
        toggleWearableEditPanel(false)
        toggleOutfitEditPanel(false)
        toggleMyOutfitsPanel(true, tabName)
    }

    fun showOutfitEditPanel() {
        if (outfitEdit?.isVisible() == true) return

        if (editWearable != null && editWearable?.isVisible() == false && outfitEdit != null) {
            outfitEdit?.resetAccordionState()
        }

        if (editWearable?.isVisible() == true && !AgentCamera.instance().cameraCustomizeAvatar()) {
            showOutfitsInventoryPanel()
            return
        }

        toggleMyOutfitsPanel(false, "")
        toggleWearableEditPanel(false, null, disableCameraSwitch = true)
        toggleOutfitEditPanel(true)
    }

    fun showWearableEditPanel(wearable: ViewerWearable? = null, disableCameraSwitch: Boolean = false) {
        toggleMyOutfitsPanel(false, "")
        toggleOutfitEditPanel(false, disableCameraSwitch = true)
        toggleWearableEditPanel(true, wearable, disableCameraSwitch)
    }

    private fun toggleMyOutfitsPanel(visible: Boolean, tabName: String) {
        val inventory = panelOutfitsInventory ?: return
        if (inventory.isVisible() == visible && tabName.isEmpty()) return

        inventory.setVisible(visible)
        filterEditor?.setVisible(visible)
        currOutfitPanel?.setVisible(visible)

        getChildView("options_gear_btn_panel")?.setVisible(false)
        getChildView("options_sort_btn_panel")?.setVisible(visible)
        getChildView("trash_btn_panel")?.setVisible(false)

        if (visible) {
            inventory.onOpen(LLSD())
            if (tabName.isNotEmpty()) {
                inventory.openApearanceTab(tabName)
            }
        }
    }

    fun isCOFPanelVisible(): Boolean {
        return panelOutfitsInventory?.isVisible() == true &&
            panelOutfitsInventory?.isCOFPanelActive() == true
    }

    private fun toggleOutfitEditPanel(visible: Boolean, disableCameraSwitch: Boolean = false) {
        val edit = outfitEdit ?: return
        if (edit.isVisible() == visible) return

        edit.setVisible(visible)
        if (visible) {
            edit.onOpen(LLSD())
            VOAvatarSelf.onCustomizeStart(disableCameraSwitch)
        } else {
            if (!disableCameraSwitch) {
                VOAvatarSelf.onCustomizeEnd(disableCameraSwitch)
                AppearanceMgr.getInstance().updateIsDirty()
            }
        }
    }

    private fun toggleWearableEditPanel(
        visible: Boolean,
        wearable: ViewerWearable? = null,
        disableCameraSwitch: Boolean = false
    ) {
        val editPanel = editWearable ?: return

        if (editPanel.isVisible() == visible &&
            (!visible || editPanel.getWearable() == wearable)
        ) return

        val changeState = !disableCameraSwitch && editPanel.isVisible() != visible

        val resolvedWearable = wearable
            ?: AgentWearables.instance().getViewerWearable(WearableType.WT_SHAPE, 0u)
            ?: return

        editPanel.setVisible(visible)

        if (visible) {
            VOAvatarSelf.onCustomizeStart(!changeState)
            editPanel.setWearable(resolvedWearable, !changeState)
            editPanel.onOpen(LLSD())
        } else {
            editPanel.saveChanges()
            editPanel.setWearable(null)
            AppearanceMgr.getInstance().updateIsDirty()
            if (changeState) {
                VOAvatarSelf.onCustomizeEnd(!changeState)
            }
        }
    }

    fun refreshCurrentOutfitName(name: String = "") {
        val dirty = AppearanceMgr.getInstance().isOutfitDirty()
        val cofStatusStr = getString(if (dirty) "Unsaved Changes" else "Now Wearing")
        outfitStatus?.setText(cofStatusStr)

        if (name.isEmpty()) {
            val outfitName = AppearanceMgr.getInstance().getBaseOutfitName()
            if (outfitName != null) {
                currentLookName?.setText(outfitName)
                return
            }
            val stringName = if (AgentWearables.instance().isCOFChangeInProgress()) "Changing outfits" else "No Outfit"
            currentLookName?.setText(getString(stringName))
            openOutfitBtn?.setEnabled(false)
        } else {
            currentLookName?.setText(name)
            openOutfitBtn?.setEnabled(true)
        }
    }

    fun fetchInventory() {
        val ids = mutableListOf<UUID>()
        for (type in WearableType.WT_SHAPE.ordinal until WearableType.WT_COUNT.ordinal) {
            val wearableType = WearableType.values()[type]
            val count = AgentWearables.instance().getWearableCount(wearableType)
            for (index in 0u until count) {
                val itemId = AgentWearables.instance().getWearableItemID(wearableType, index)
                if (itemId != null) ids.add(itemId)
            }
        }

        System.err.println("SidePanelAppearance: fetchInventory not yet implemented")
    }

    fun inventoryFetched() {}

    fun setWearablesLoading(loading: Boolean) {
        wearableLoadingIndicator?.setVisible(loading)
        editOutfitBtn?.setVisible(!loading)
        if (!loading) {
            refreshCurrentOutfitName()
        }
    }

    fun showDefaultSubpart() {
        if (editWearable?.isVisible() == true) {
            editWearable?.showDefaultSubpart()
        }
    }

    fun updateScrollingPanelList() {
        if (editWearable?.isVisible() == true) {
            editWearable?.updateScrollingPanelList()
        }
    }

    fun isOutfitEditPanelVisible(): Boolean = outfitEdit?.isVisible() == true

    fun isWearableEditPanelVisible(): Boolean = editWearable?.isVisible() == true

    fun getOutfitEditPanel(): PanelOutfitEdit? = outfitEdit

    fun getWearableEditPanel(): PanelEditWearable? = editWearable

    fun getWearable(): PanelEditWearable? = editWearable

    companion object {
        fun editWearable(wearable: ViewerWearable, data: View, disableCameraSwitch: Boolean = false) {
            FloaterSidePanelContainer.showPanel("appearance", LLSD())
            val panel = data as? SidePanelAppearance
            panel?.showWearableEditPanel(wearable, disableCameraSwitch)
        }

        fun updateAvatarComplexity(
            complexity: UInt,
            itemComplexity: MutableMap<UUID, UInt>,
            tempItemComplexity: MutableMap<UUID, UInt>,
            bodyPartsComplexity: UInt
        ) {
            val instance = FloaterSidePanelContainer.getPanel<SidePanelAppearance>("appearance")
            if (instance.lastAvatarComplexity != complexity) {
                instance.panelOutfitsInventory?.updateAvatarComplexity(
                    complexity, itemComplexity, tempItemComplexity, bodyPartsComplexity
                )
                instance.outfitEdit?.updateAvatarComplexity(complexity)
            }
            instance.lastAvatarComplexity = complexity
        }
    }
}
