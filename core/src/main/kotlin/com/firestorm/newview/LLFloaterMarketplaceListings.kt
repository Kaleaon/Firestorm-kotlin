package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// LLPanelMarketplaceListings
// ---------------------------------------------------------------------------

class LLPanelMarketplaceListings : LLPanel() {

    var mRootFolder: LLFolderView? = null
        private set

    private var mAuditBtn: LLButton? = null
    private var mFilterEditor: LLFilterEditor? = null
    private var mFilterSubString: String = ""
    private var mFilterListingFoldersOnly: Boolean = false
    private var mSortOrder: UInt = LLInventoryFilter.SO_FOLDERS_BY_NAME

    fun postBuild(): Boolean {
        childSetAction("add_btn") { onAddButtonClicked() }
        childSetAction("audit_btn") { onAuditButtonClicked() }

        mFilterEditor = getChild<LLFilterEditor>("filter_editor")
        mFilterEditor?.setCommitCallback { searchString -> onFilterEdit(searchString) }

        mAuditBtn = getChild<LLButton>("audit_btn")
        mAuditBtn?.setEnabled(false)

        return super.postBuild()
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: EDragAndDropType, cargoData: Any?,
        accept: EAcceptanceHolder, tooltipMsg: StringBuilder
    ): Boolean {
        val handledView = childrenHandleDragAndDrop(x, y, mask, drop, cargoType, cargoData, accept, tooltipMsg)
        var handled = handledView != null
        if (handled && handledView?.getName() == "marketplace_drop_zone") {
            val rootFolder = getRootFolder()
            handled = rootFolder?.handleDragAndDropToThisFolder(mask, drop, cargoType, cargoData, accept, tooltipMsg) ?: false
        }
        return handled
    }

    fun draw() {
        if (LLMarketplaceData.instance().checkDirtyCount()) {
            updateAllMarketplaceCount()
        }

        if (mAuditBtn?.getEnabled() == false) {
            val inst = LLInventoryModelBackgroundFetch.getInstance()
            mAuditBtn?.setEnabled(inst.isEverythingFetched() && !inst.folderFetchActive())
        }

        super.draw()
    }

    fun getRootFolder(): LLFolderView? = mRootFolder

    fun allowDropOnRoot(): Boolean {
        val panel = getChild<LLTabContainer>("marketplace_filter_tabs").getCurrentPanel() as? LLInventoryPanel
        return panel?.getAllowDropOnRoot() ?: false
    }

    fun buildAllPanels() {
        val panelAllItems = buildInventoryPanel("All Items", "panel_marketplace_listings_inventory.xml")
        panelAllItems.getFilter().setEmptyLookupMessage("MarketplaceNoMatchingItems")
        panelAllItems.getFilter().markDefault()

        var panel = buildInventoryPanel("Active Items", "panel_marketplace_listings_listed.xml")
        panel.getFilter().setFilterMarketplaceActiveFolders()
        panel.getFilter().setEmptyLookupMessage("MarketplaceNoMatchingItems")
        panel.getFilter().setDefaultEmptyLookupMessage("MarketplaceNoListing")
        panel.getFilter().markDefault()

        panel = buildInventoryPanel("Inactive Items", "panel_marketplace_listings_unlisted.xml")
        panel.getFilter().setFilterMarketplaceInactiveFolders()
        panel.getFilter().setEmptyLookupMessage("MarketplaceNoMatchingItems")
        panel.getFilter().setDefaultEmptyLookupMessage("MarketplaceNoListing")
        panel.getFilter().markDefault()

        panel = buildInventoryPanel("Unassociated Items", "panel_marketplace_listings_unassociated.xml")
        panel.getFilter().setFilterMarketplaceUnassociatedFolders()
        panel.getFilter().setEmptyLookupMessage("MarketplaceNoMatchingItems")
        panel.getFilter().setDefaultEmptyLookupMessage("MarketplaceNoListing")
        panel.getFilter().markDefault()

        val tabsPanel = getChild<LLTabContainer>("marketplace_filter_tabs")
        tabsPanel.setCommitCallback { onTabChange() }
        tabsPanel.selectTabPanel(panelAllItems)
        mRootFolder = panelAllItems.getRootFolder()

        setSortOrder(gSavedSettings.getU32("MarketplaceListingsSortOrder"))
    }

    private fun buildInventoryPanel(childName: String, filename: String): LLInventoryPanel {
        val tabsPanel = getChild<LLTabContainer>("marketplace_filter_tabs")
        LLUICtrlFactory.createFromFile<LLInventoryPanel>(filename, tabsPanel, LLInventoryPanel.childRegistryInstance())
        val panel = getChild<LLInventoryPanel>(childName)
        panel.getFolderViewModel().setSorter(LLInventoryFilter.SO_FOLDERS_BY_NAME)
        panel.setSelectCallback { items, userAction -> onSelectionChange(panel, items, userAction) }
        return panel
    }

    private fun setSortOrder(sortOrder: UInt) {
        mSortOrder = sortOrder
        gSavedSettings.setU32("MarketplaceListingsSortOrder", sortOrder)

        val tabsPanel = getChild<LLTabContainer>("marketplace_filter_tabs")
        (tabsPanel.getPanelByName("All Items") as LLInventoryPanel).setSortOrder(mSortOrder)
        (tabsPanel.getPanelByName("Active Items") as LLInventoryPanel).setSortOrder(mSortOrder)
        (tabsPanel.getPanelByName("Inactive Items") as LLInventoryPanel).setSortOrder(mSortOrder)
        (tabsPanel.getPanelByName("Unassociated Items") as LLInventoryPanel).setSortOrder(mSortOrder)
    }

    private fun onFilterEdit(searchString: String) {
        val panel = getChild<LLTabContainer>("marketplace_filter_tabs").getCurrentPanel() as? LLInventoryPanel
        if (panel != null) {
            mFilterSubString = searchString
            panel.setFilterSubString(mFilterSubString)
        }
    }

    private fun onSelectionChange(panel: LLInventoryPanel, items: ArrayDeque<LLFolderViewItem>, userAction: Boolean) {
        panel.onSelectionChange(items, userAction)
    }

    private fun onTabChange() {
        val panel = getChild<LLTabContainer>("marketplace_filter_tabs").getCurrentPanel() as? LLInventoryPanel
            ?: return

        getChild<LLButton>("add_btn").setEnabled(panel.getAllowDropOnRoot())
        panel.setFilterSubString(mFilterSubString)

        val dropZone = getChild<LLPanel>("marketplace_drop_zone")
        val dropZoneVisible = dropZone.getVisible()
        if (dropZoneVisible != panel.getAllowDropOnRoot()) {
            val tabs = getChild<LLPanel>("tab_container_panel")
            val deltaHeight = if (dropZoneVisible) dropZone.getRect().getHeight() else -dropZone.getRect().getHeight()
            tabs.reshape(tabs.getRect().getWidth(), tabs.getRect().getHeight() + deltaHeight)
            tabs.translate(0, -deltaHeight)
        }
        dropZone.setVisible(panel.getAllowDropOnRoot())
    }

    private fun onAddButtonClicked() {
        val marketplaceListingsId = gInventory.getMarketplaceListingsUUID()
        val preferredType = LLFolderType.lookup("category")
        val handle = getHandle()
        gInventory.createNewCategory(marketplaceListingsId, preferredType, "") { newCatId ->
            val marketplacePanel = handle.get() ?: return@createNewCategory
            val panel = (marketplacePanel.getChild<LLTabContainer>("marketplace_filter_tabs")
                .getCurrentPanel()) as? LLInventoryPanel ?: return@createNewCategory
            gInventory.notifyObservers()
            panel.setSelectionByID(newCatId, true)
            panel.getRootFolder()?.setNeedsAutoRename(true)
        }
    }

    private fun onAuditButtonClicked() {
        LLFloaterReg.showInstance("marketplace_validation", LLSD.emptyMap())
    }

    private fun onViewSortMenuItemClicked(userdata: LLSD) {
        val chosenItem = userdata.asString()
        when (chosenItem) {
            "sort_by_stock_amount" -> setSortOrder(LLInventoryFilter.SO_FOLDERS_BY_WEIGHT)
            "sort_by_name" -> setSortOrder(LLInventoryFilter.SO_FOLDERS_BY_NAME)
            "sort_by_recent" -> setSortOrder(LLInventoryFilter.SO_DATE)
            "show_only_listing_folders" -> {
                mFilterListingFoldersOnly = !mFilterListingFoldersOnly
                val tabsPanel = getChild<LLTabContainer>("marketplace_filter_tabs")
                listOf("All Items", "Active Items", "Inactive Items", "Unassociated Items").forEach { name ->
                    (tabsPanel.getPanelByName(name) as LLInventoryPanel)
                        .getFilter().setFilterMarketplaceListingFolders(mFilterListingFoldersOnly)
                }
            }
        }
    }

    private fun onViewSortMenuItemCheck(userdata: LLSD): Boolean {
        return when (val chosenItem = userdata.asString()) {
            "sort_by_stock_amount" -> (mSortOrder and LLInventoryFilter.SO_FOLDERS_BY_WEIGHT) != 0u
            "sort_by_name" -> (mSortOrder and LLInventoryFilter.SO_FOLDERS_BY_NAME) != 0u
            "sort_by_recent" -> (mSortOrder and LLInventoryFilter.SO_DATE) != 0u
            "show_only_listing_folders" -> mFilterListingFoldersOnly
            else -> false
        }
    }
}

// ---------------------------------------------------------------------------
// LLMarketplaceListingsAddedObserver — internal helper
// ---------------------------------------------------------------------------

private class LLMarketplaceListingsAddedObserver(
    private val mMarketplaceListingsFloater: LLFloaterMarketplaceListings
) : LLInventoryCategoryAddedObserver() {

    override fun done() {
        for (addedCategory in mAddedCategories) {
            if (addedCategory.getPreferredType() == LLFolderType.FT_MARKETPLACE_LISTINGS) {
                val handle = mMarketplaceListingsFloater.getHandle()
                doOnIdleOneTime {
                    val floater = handle.get() as? LLFloaterMarketplaceListings
                    floater?.initializeMarketPlace()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// LLFloaterMarketplaceListings
// ---------------------------------------------------------------------------

class LLFloaterMarketplaceListings(key: LLSD) : LLFloater(key) {

    private var mCategoriesObserver: LLInventoryCategoriesObserver? = null
    private var mCategoryAddedObserver: LLInventoryCategoryAddedObserver? = null

    private var mInventoryStatus: LLTextBox? = null
    private var mInventoryInitializationInProgress: LLView? = null
    private var mInventoryPlaceholder: LLView? = null
    private var mInventoryText: LLTextBox? = null
    private var mInventoryTitle: LLTextBox? = null

    private var mRootFolderId: UUID = UUID_NULL
    private var mRootFolderCreating: Boolean = false
    private var mPanelListings: LLPanelMarketplaceListings? = null
    private var mPanelListingsSet: Boolean = false

    override fun postBuild(): Boolean {
        mInventoryStatus = getChild<LLTextBox>("marketplace_status")
        mInventoryInitializationInProgress = getChild<LLView>("initialization_progress_indicator")
        mInventoryPlaceholder = getChild<LLView>("marketplace_listings_inventory_placeholder_panel")
        mInventoryText = mInventoryPlaceholder?.getChild<LLTextBox>("marketplace_listings_inventory_placeholder_text")
        mInventoryTitle = mInventoryPlaceholder?.getChild<LLTextBox>("marketplace_listings_inventory_placeholder_title")

        mPanelListings = getChild<LLUICtrl>("panel_marketplace_listing") as LLPanelMarketplaceListings

        setFocusReceivedCallback { onFocusReceived() }

        mCategoryAddedObserver = LLMarketplaceListingsAddedObserver(this)
        gInventory.addObserver(mCategoryAddedObserver!!)

        if (!fetchContents()) {
            val marketplaceListingsId = gInventory.getMarketplaceListingsUUID()
            LLInventoryModelBackgroundFetch.instance().start(marketplaceListingsId, true)
        }

        return true
    }

    fun initializeMarketPlace() {
        if (!mRootFolderCreating) {
            LLMarketplaceData.instance().initializeSLM { updateView() }
        }
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: EDragAndDropType, cargoData: Any?,
        accept: EAcceptanceHolder, tooltipMsg: StringBuilder
    ): Boolean {
        if (mPanelListings == null || mRootFolderId == UUID_NULL) return false

        tooltipMsg.clear()
        val handledView = childrenHandleDragAndDrop(x, y, mask, drop, cargoType, cargoData, accept, tooltipMsg)
        var handled = handledView != null

        if ((!handled || !isAccepted(accept.value)) && mPanelListings?.getVisible() == false && mRootFolderId != UUID_NULL) {
            if (!mPanelListingsSet) setPanels()
            val rootFolder = mPanelListings?.getRootFolder()
            handled = rootFolder?.handleDragAndDropToThisFolder(mask, drop, cargoType, cargoData, accept, tooltipMsg) ?: false
        }
        return handled
    }

    fun showNotification(notification: LLNotificationPtr) {
        // forward marketplace notifications to the status bar
        setStatusString(notification.getMessage())
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean = super.handleHover(x, y, mask)

    open fun onMouseLeave(x: Int, y: Int, mask: Int) = super.onMouseLeave(x, y, mask)

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (FSCommon.isFilterEditorKeyCombo(key, mask)) {
            getChild<LLFilterEditor>("filter_editor").setFocus(true)
            return true
        }
        return super.handleKeyHere(key, mask)
    }

    open fun hasAccelerators(): Boolean = true

    protected fun setRootFolder() {
        val mktStatus = LLMarketplaceData.instance().getSLMStatus()
        if (mktStatus != MarketplaceStatusCodes.MARKET_PLACE_MERCHANT &&
            mktStatus != MarketplaceStatusCodes.MARKET_PLACE_MIGRATED_MERCHANT
        ) return

        if (!gInventory.isInventoryUsable()) return

        val preferredType = LLFolderType.FT_MARKETPLACE_LISTINGS
        var marketplaceListingsId = gInventory.findCategoryUUIDForType(preferredType)

        if (marketplaceListingsId == UUID_NULL) {
            if (!mRootFolderCreating) {
                mRootFolderCreating = true
                gInventory.createNewCategory(gInventory.getRootFolderID(), preferredType, "") { newCatId ->
                    val marketplace = LLFloaterReg.findTypedInstance<LLFloaterMarketplaceListings>("marketplace_listings")
                    if (marketplace != null) {
                        if (newCatId != UUID_NULL) {
                            marketplace.updateView()
                        } else {
                            marketplace.mRootFolderCreating = false
                        }
                    }
                }
            }
            return
        }

        val catObserver = mCategoryAddedObserver
        if (catObserver != null && gInventory.containsObserver(catObserver)) {
            gInventory.removeObserver(catObserver)
            mCategoryAddedObserver = null
        }

        mRootFolderCreating = false

        if (marketplaceListingsId == mRootFolderId) return

        mRootFolderId = marketplaceListingsId
    }

    protected fun setPanels() {
        if (mRootFolderId == UUID_NULL) return

        gInventory.consolidateForType(mRootFolderId, LLFolderType.FT_MARKETPLACE_LISTINGS)
        mPanelListings?.buildAllPanels()

        if (mCategoriesObserver == null) {
            mCategoriesObserver = LLInventoryCategoriesObserver()
            gInventory.addObserver(mCategoriesObserver!!)
            mCategoriesObserver!!.addCategory(mRootFolderId) { onChanged() }
        }

        fetchContents()
        mPanelListingsSet = true
    }

    protected fun fetchContents(): Boolean {
        if (mRootFolderId != UUID_NULL &&
            LLMarketplaceData.instance().getSLMDataFetched() != MarketplaceFetchCodes.MARKET_FETCH_LOADING &&
            LLMarketplaceData.instance().getSLMDataFetched() != MarketplaceFetchCodes.MARKET_FETCH_DONE
        ) {
            LLMarketplaceData.instance().setDataFetchedSignal { updateView() }
            LLMarketplaceData.instance().setSLMDataFetched(MarketplaceFetchCodes.MARKET_FETCH_LOADING)
            LLInventoryModelBackgroundFetch.instance().start(mRootFolderId, true)
            LLMarketplaceData.instance().getSLMListings()
            return true
        }
        return false
    }

    protected fun setStatusString(statusString: String) {
        mInventoryStatus?.setText(statusString)
    }

    protected open fun onClose(appQuitting: Boolean) {}

    protected open fun onOpen(key: LLSD) {
        if (LLMarketplaceData.instance().getSLMStatus() <= MarketplaceStatusCodes.MARKET_PLACE_CONNECTION_FAILURE) {
            initializeMarketPlace()
        } else {
            updateView()
        }
    }

    protected fun onFocusReceived() {
        updateView()
    }

    protected fun onChanged() {
        val category = gInventory.getCategory(mRootFolderId)
        if (mRootFolderId != UUID_NULL && category != null) {
            updateView()
        } else {
            mRootFolderId = UUID_NULL
        }
    }

    protected fun isAccepted(accept: EAcceptance): Boolean = accept >= EAcceptance.ACCEPT_YES_COPY_SINGLE

    protected fun updateView() {
        val mktStatus = LLMarketplaceData.instance().getSLMStatus()
        val isMerchant = mktStatus == MarketplaceStatusCodes.MARKET_PLACE_MERCHANT ||
                mktStatus == MarketplaceStatusCodes.MARKET_PLACE_MIGRATED_MERCHANT
        val dataFetched = LLMarketplaceData.instance().getSLMDataFetched()

        if (mRootFolderId == UUID_NULL && isMerchant) setRootFolder()
        if (mRootFolderCreating) return

        if (mktStatus <= MarketplaceStatusCodes.MARKET_PLACE_INITIALIZING ||
            (isMerchant && dataFetched <= MarketplaceFetchCodes.MARKET_FETCH_LOADING)
        ) {
            mInventoryInitializationInProgress?.setVisible(true)
            mPanelListings?.setVisible(false)
            fetchContents()
            return
        }

        mInventoryInitializationInProgress?.setVisible(false)

        if (getFolderCount() > 0) {
            if (!mPanelListingsSet) setPanels()
            mPanelListings?.setVisible(true)
            mInventoryPlaceholder?.setVisible(false)
        } else {
            mPanelListings?.setVisible(false)
            mInventoryPlaceholder?.setVisible(true)

            val subs = LLMarketplaceData.getMarketplaceStringSubstitutions()
            val text: String
            val title: String
            val tooltip: String

            when {
                mktStatus == MarketplaceStatusCodes.MARKET_PLACE_CONNECTION_FAILURE -> {
                    val reason = LLMarketplaceData.instance().getSLMConnectionfailureReason()
                    text = if (reason.isEmpty()) {
                        LLTrans.getString("InventoryMarketplaceConnectionError")
                    } else {
                        val args = LLSD()
                        args["[REASON]"] = reason
                        LLTrans.getString("InventoryMarketplaceConnectionErrorReason", args)
                    }
                    title = LLTrans.getString("InventoryOutboxErrorTitle")
                    tooltip = LLTrans.getString("InventoryOutboxErrorTooltip")
                }
                mRootFolderId != UUID_NULL -> {
                    text = LLTrans.getString("InventoryMarketplaceListingsNoItems", subs)
                    title = LLTrans.getString("InventoryMarketplaceListingsNoItemsTitle")
                    tooltip = LLTrans.getString("InventoryMarketplaceListingsNoItemsTooltip")
                }
                mktStatus <= MarketplaceStatusCodes.MARKET_PLACE_INITIALIZING -> {
                    text = LLTrans.getString("InventoryOutboxInitializing", subs)
                    title = LLTrans.getString("InventoryOutboxInitializingTitle")
                    tooltip = LLTrans.getString("InventoryOutboxInitializingTooltip")
                }
                mktStatus == MarketplaceStatusCodes.MARKET_PLACE_NOT_MERCHANT -> {
                    text = LLTrans.getString("InventoryOutboxNotMerchant", subs)
                    title = LLTrans.getString("InventoryOutboxNotMerchantTitle")
                    tooltip = LLTrans.getString("InventoryOutboxNotMerchantTooltip")
                }
                else -> {
                    text = LLTrans.getString("InventoryMarketplaceError", subs)
                    title = LLTrans.getString("InventoryOutboxErrorTitle")
                    tooltip = LLTrans.getString("InventoryOutboxErrorTooltip")
                }
            }

            mInventoryText?.setValue(text)
            mInventoryTitle?.setValue(title)
            mInventoryPlaceholder?.getParent()?.setToolTip(tooltip)
        }
    }

    private fun getFolderCount(): Int {
        if (mPanelListings != null && mRootFolderId != UUID_NULL) {
            val cats = gInventory.getDirectDescendentsCats(mRootFolderId)
            val items = gInventory.getDirectDescendentsItems(mRootFolderId)
            return cats.size + items.size
        }
        return 0
    }

    override fun onDestroy() {
        val catObs = mCategoriesObserver
        if (catObs != null && gInventory.containsObserver(catObs)) {
            gInventory.removeObserver(catObs)
        }
        val catAddObs = mCategoryAddedObserver
        if (catAddObs != null && gInventory.containsObserver(catAddObs)) {
            gInventory.removeObserver(catAddObs)
        }
        super.onDestroy()
    }
}

// ---------------------------------------------------------------------------
// LLFloaterAssociateListing
// ---------------------------------------------------------------------------

class LLFloaterAssociateListing(key: LLSD) : LLFloater(key) {

    private var mUUID: UUID = UUID_NULL

    override fun postBuild(): Boolean {
        getChild<LLButton>("OK").setCommitCallback { apply(userConfirm = true) }
        getChild<LLButton>("Cancel").setCommitCallback { cancel() }
        getChild<LLLineEditor>("listing_id").setPrevalidate(LLTextValidate::validateNonNegativeS32)
        center()
        return super.postBuild()
    }

    override fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (key == KEY_RETURN && mask == MASK_NONE) {
            apply()
            return true
        }
        if (key == KEY_ESCAPE && mask == MASK_NONE) {
            cancel()
            return true
        }
        return super.handleKeyHere(key, mask)
    }

    private fun apply(userConfirm: Boolean = true) {
        if (mUUID != UUID_NULL) {
            val id = getChild<LLUICtrl>("listing_id").getValue().asInteger()
            if (id > 0) {
                val listingUuid = LLMarketplaceData.instance().getListingFolder(id)
                if (listingUuid != UUID_NULL && userConfirm &&
                    LLMarketplaceData.instance().getActivationState(listingUuid) &&
                    !hasUniqueVersionFolder(mUUID)
                ) {
                    LLNotificationsUtil.add("ConfirmMerchantUnlist", LLSD(), LLSD()) { notification, response ->
                        callbackApply(notification, response)
                    }
                    return
                }
                LLMarketplaceData.instance().associateListing(mUUID, listingUuid, id)
            } else {
                LLNotificationsUtil.add("AlertMerchantListingInvalidID")
            }
        }
        closeFloater()
    }

    private fun cancel() {
        closeFloater()
    }

    private fun callbackApply(notification: LLSD, response: LLSD) {
        val option = LLNotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) apply(userConfirm = false)
    }

    companion object {
        fun show(folderId: UUID): LLFloaterAssociateListing? {
            val floater = LLFloaterReg.showTypedInstance<LLFloaterAssociateListing>("associate_listing")
                ?: return null
            floater.mUUID = folderId
            return floater
        }
    }
}

// Returns true when the given folder has exactly one version sub-folder.
private fun hasUniqueVersionFolder(folderId: UUID): Boolean {
    val categories = gInventory.getDirectDescendentsCats(folderId)
    return categories.size == 1
}

// ---------------------------------------------------------------------------
// LLFloaterMarketplaceValidation
// ---------------------------------------------------------------------------

class LLFloaterMarketplaceValidation(key: LLSD) : LLFloater(key) {

    data class Message(val mErrorLevel: LLError.ELevel, val mMessage: String)

    private var mCurrentListingMessages: MutableList<Message> = mutableListOf()
    private var mCurrentListingErrorLevel: LLError.ELevel = LLError.ELevel.LEVEL_INFO
    private var mMessages: MutableList<Message> = mutableListOf()
    private var mEditor: LLTextEditor? = null

    override fun postBuild(): Boolean {
        childSetAction("OK") { onOK(this) }
        mEditor = getChild<LLTextEditor>("validation_text")
        mEditor?.setEnabled(false)
        mEditor?.setFocus(true)
        mEditor?.setValue(LLSD())
        return true
    }

    override fun draw() {
        super.draw()
    }

    override fun onOpen(key: LLSD) {
        clearMessages()

        var catId = UUID.fromString(key.asString())
        if (catId == UUID_NULL) {
            catId = gInventory.getMarketplaceListingsUUID()
        }

        if (catId != UUID_NULL) {
            LLMarketplaceValidator.getInstance().validateMarketplaceListings(
                catId, null
            ) { message, depth, logLevel -> appendMessage(message, depth, logLevel) }
        }

        handleCurrentListing()

        val editor = mEditor
        if (editor != null) {
            editor.setValue(LLSD())
            if (mMessages.isEmpty()) {
                editor.appendText(LLTrans.getString("Marketplace Validation No Error"), false)
            } else {
                var newLine = false
                for (msg in mMessages) {
                    val style = LLStyle.Params()
                    val fontDesc = LLFontDescriptor(editor.getFont().getFontDesc())
                    fontDesc.setStyle(if (msg.mErrorLevel == LLError.ELevel.LEVEL_ERROR) LLFontGL.BOLD else LLFontGL.NORMAL)
                    style.font = LLFontGL.getFont(fontDesc)
                    editor.appendText(msg.mMessage, newLine, style)
                    newLine = true
                }
            }
        }

        clearMessages()
    }

    fun clearMessages() {
        mMessages.clear()
        mCurrentListingMessages.clear()
        mCurrentListingErrorLevel = LLError.ELevel.LEVEL_INFO
    }

    fun appendMessage(message: String, depth: Int, logLevel: LLError.ELevel) {
        if (depth == 1) handleCurrentListing()
        mCurrentListingMessages.add(Message(logLevel, message))
        if (mCurrentListingErrorLevel < logLevel) mCurrentListingErrorLevel = logLevel
    }

    private fun handleCurrentListing() {
        if (mCurrentListingErrorLevel > LLError.ELevel.LEVEL_INFO) {
            mMessages.addAll(mCurrentListingMessages)
        }
        mCurrentListingMessages.clear()
        mCurrentListingErrorLevel = LLError.ELevel.LEVEL_INFO
    }

    companion object {
        fun onOK(self: LLFloaterMarketplaceValidation) {
            self.clearMessages()
            self.closeFloater()
        }
    }
}

// ---------------------------------------------------------------------------
// LLFloaterItemProperties
// ---------------------------------------------------------------------------

class LLFloaterItemProperties(key: LLSD) : LLFloater(key) {

    override fun postBuild(): Boolean = super.postBuild()

    override fun onOpen(key: LLSD) {
        val panel = findChild<LLPanel>("sidepanel")

        val itemPanel = panel as? LLSidepanelItemInfo
        if (itemPanel != null) {
            itemPanel.setItemID(key["id"].asUUID())
            if (key.has("object")) {
                itemPanel.setObjectID(key["object"].asUUID())
            }
            itemPanel.setParentFloater(this)
        }

        val taskPanel = panel as? LLSidepanelTaskInfo
        if (taskPanel != null) {
            taskPanel.setObjectSelection(LLSelectMgr.getInstance().getSelection())
        }
    }
}

// ---------------------------------------------------------------------------
// LLMultiItemProperties
// ---------------------------------------------------------------------------

class LLMultiItemProperties(key: LLSD) : LLMultiFloater(LLSD()) {

    init {
        val rect = LLRect()
        rect.setLeftTopAndSize(0, gViewerWindow.getWindowHeightScaled(), 350, 350)
        setRect(rect)
        val lastFloater = LLFloaterReg.getLastFloaterInGroup(key.asString())
        if (lastFloater != null) {
            stackWith(lastFloater)
        }
        setTitle(LLTrans.getString("MultiPropertiesTitle"))
        buildTabContainer()
        center()
    }
}
