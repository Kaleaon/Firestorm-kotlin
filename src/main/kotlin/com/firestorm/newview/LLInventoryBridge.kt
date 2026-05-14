package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Type aliases
// ---------------------------------------------------------------------------

typealias MenuEntryVec = MutableList<String>
typealias TwoUUIDs = Pair<UUID, UUID>
typealias TwoUUIDsList = MutableList<TwoUUIDs>

// ---------------------------------------------------------------------------
// LLMoveInv data class
// ---------------------------------------------------------------------------

data class LLMoveInv(
    val objectID: UUID,
    val categoryID: UUID,
    val moveList: TwoUUIDsList = mutableListOf(),
    val callback: ((Int, Any?, LLMoveInv) -> Unit)? = null,
    val userData: Any? = null
)

// ---------------------------------------------------------------------------
// LLInvFVBridge
//
// Inventory-Folder-View-Bridge: base class that adapts a raw inventory object
// to the folder-view model.  Use createBridge() rather than constructing
// directly (except for LLFolderBridge).
// ---------------------------------------------------------------------------

abstract class LLInvFVBridge protected constructor(
    inventory: LLInventoryPanel,
    protected val root: LLFolderView?,
    val uuid: UUID
) : LLFolderViewModelItemInventory(inventory.rootViewModel) {

    protected val inventoryPanel: LLHandle<LLInventoryPanel> = inventory.inventoryPanelHandle
    protected val invType: LLInventoryType.EType = LLInventoryType.EType.IT_NONE
    protected val isLink: Boolean
    protected val timeSinceRequestStart: LLTimer = LLTimer()
    @Volatile protected var displayName: String = ""
    @Volatile protected var searchableName: String = ""

    init {
        val obj = inventoryObject
        isLink = obj?.isLinkType ?: false
    }

    // ------------------------------------------------------------------
    // Factory
    // ------------------------------------------------------------------

    companion object {
        fun createBridge(
            assetType: LLAssetType.EType,
            actualAssetType: LLAssetType.EType,
            invType: LLInventoryType.EType,
            inventory: LLInventoryPanel,
            viewModel: LLFolderViewModelInventory,
            root: LLFolderView,
            uuid: UUID,
            flags: UInt = 0u
        ): LLInvFVBridge? {
            return when (assetType) {
                LLAssetType.EType.AT_TEXTURE, LLAssetType.EType.AT_SNAPSHOT ->
                    LLTextureBridge(inventory, root, uuid, invType)
                LLAssetType.EType.AT_SOUND ->
                    LLSoundBridge(inventory, root, uuid)
                LLAssetType.EType.AT_LANDMARK ->
                    LLLandmarkBridge(inventory, root, uuid, flags)
                LLAssetType.EType.AT_CALLINGCARD ->
                    LLCallingCardBridge(inventory, root, uuid)
                LLAssetType.EType.AT_SCRIPT, LLAssetType.EType.AT_LSL_TEXT,
                LLAssetType.EType.AT_LSL_BYTECODE ->
                    LLLSLTextBridge(inventory, root, uuid)
                LLAssetType.EType.AT_CLOTHING, LLAssetType.EType.AT_BODYPART ->
                    LLWearableBridge(inventory, root, uuid, assetType, invType, LLWearableType.EType.fromAssetType(assetType))
                LLAssetType.EType.AT_NOTECARD ->
                    LLNotecardBridge(inventory, root, uuid)
                LLAssetType.EType.AT_ANIMATION ->
                    LLAnimationBridge(inventory, root, uuid)
                LLAssetType.EType.AT_GESTURE ->
                    LLGestureBridge(inventory, root, uuid)
                LLAssetType.EType.AT_OBJECT ->
                    LLObjectBridge(inventory, root, uuid, invType, flags)
                LLAssetType.EType.AT_SETTINGS ->
                    LLSettingsBridge(inventory, root, uuid, LLSettingsType.EType.ST_NONE)
                LLAssetType.EType.AT_MATERIAL ->
                    LLMaterialBridge(inventory, root, uuid)
                LLAssetType.EType.AT_LINK ->
                    createBridge(actualAssetType, actualAssetType, invType, inventory, viewModel, root, uuid, flags)
                LLAssetType.EType.AT_LINK_FOLDER ->
                    LLLinkFolderBridge(inventory, root, uuid)
                LLAssetType.EType.AT_CATEGORY ->
                    LLFolderBridge(inventory, root, uuid)
                else ->
                    LLUnknownItemBridge(inventory, root, uuid)
            }
        }

        fun changeItemParent(
            model: LLInventoryModel,
            item: LLViewerInventoryItem,
            newParent: UUID,
            restamp: Boolean
        ) {
            model.changeItemParent(item, newParent, restamp)
        }

        fun changeCategoryParent(
            model: LLInventoryModel,
            cat: LLViewerInventoryCategory,
            newParent: UUID,
            restamp: Boolean
        ) {
            model.changeCategoryParent(cat, newParent, restamp)
        }
    }

    // ------------------------------------------------------------------
    // Core identity / display
    // ------------------------------------------------------------------

    open fun getUUID(): UUID = uuid
    open fun getThumbnailUUID(): UUID = UUID_NULL
    open fun isFavorite(): Boolean = false
    open fun clearDisplayName() { displayName = "" }
    open fun restoreItem() {}
    open fun restoreToWorld() {}

    open fun getName(): String = inventoryObject?.name ?: ""

    open fun getDisplayName(): String {
        if (displayName.isEmpty()) buildDisplayName()
        return displayName
    }

    override fun getSearchableName(): String = searchableName

    fun getSearchableDescription(): String =
        getSearchableDescription(inventoryModel, uuid)

    fun getSearchableCreatorName(): String =
        getSearchableCreatorName(inventoryModel, uuid)

    fun getSearchableUUIDString(): String =
        getSearchableUUID(inventoryModel, uuid)

    fun getSearchableAll(): String =
        "${getSearchableName()}+${getSearchableCreatorName()}+" +
        "${getSearchableDescription()}+${getSearchableUUIDString()}"

    // ------------------------------------------------------------------
    // Permissions / type
    // ------------------------------------------------------------------

    open fun getPermissionMask(): Int = PERM_ALL

    open fun getPreferredType(): LLFolderType.EType = LLFolderType.EType.FT_NONE

    open fun getCreationDate(): Long {
        return inventoryObject?.creationDate ?: 0L
    }

    open fun setCreationDate(creationDateUtc: Long) {
        inventoryObject?.creationDate = creationDateUtc
    }

    open fun getLabelStyle(): LLFontGL.StyleFlags = LLFontGL.StyleFlags.NORMAL
    open fun getLabelSuffix(): String = ""
    open fun openItem() {}
    open fun closeItem() {}

    open fun navigateToFolder(newWindow: Boolean = false, changeMode: Boolean = false) {
        val panel = inventoryPanel.get() ?: return
        if (newWindow) {
            panel.openSingleViewInventory(uuid)
        } else {
            if (changeMode) {
                LLInventoryPanel.setSFViewAndOpenFolder(panel, uuid)
            } else {
                val singlePanel = panel as? LLInventorySingleFolderPanel ?: return
                if (inventoryModel == null || uuid == UUID_NULL) return
                singlePanel.changeFolderRoot(uuid)
            }
        }
    }

    open fun showProperties() {
        if (isMarketplaceListingsFolder()) {
            LLFloaterReg.showInstance("item_properties", LLSD().with("id", uuid), true)
        } else {
            showItemProfile(uuid)
        }
    }

    open fun isItemRenameable(): Boolean = true
    open fun isMultiPreviewAllowed(): Boolean = true
    open fun isItemRemovable(checkWorn: Boolean = true): Boolean =
        getIsItemRemovable(inventoryModel, uuid, checkWorn)

    open fun isItemMovable(): Boolean {
        if (isLockedFolder()) return false
        return true
    }

    open fun isItemInTrash(): Boolean {
        if (uuid == UUID_NULL) return false
        val trashId = inventoryModel?.findCategoryUUIDForType(LLFolderType.EType.FT_TRASH) ?: return false
        return gInventory.isObjectDescendentOf(uuid, trashId)
    }

    open fun isItemInOutfits(): Boolean {
        val outfitsId = inventoryModel?.findCategoryUUIDForType(LLFolderType.EType.FT_MY_OUTFITS) ?: return false
        return gInventory.isObjectDescendentOf(uuid, outfitsId)
    }

    open fun isLink(): Boolean = isLink

    open fun isLibraryItem(): Boolean =
        gInventory.isObjectDescendentOf(getUUID(), gInventory.libraryRootFolderID)

    open fun removeBatch(batch: MutableList<LLFolderViewModelItem>) {
        val model = inventoryModel ?: return

        // Deactivate gestures for items moving to trash
        for (item in batch) {
            val bridge = item as? LLInvFVBridge ?: continue
            if (!bridge.isItemRemovable()) continue
            val inv = model.getItem(bridge.getUUID()) as? LLViewerInventoryItem ?: continue
            if (inv.type == LLAssetType.EType.AT_GESTURE && !inv.isLinkType) {
                LLGestureMgr.instance().deactivateGesture(inv.uuid)
            }
        }
        for (item in batch) {
            val bridge = item as? LLInvFVBridge ?: continue
            if (!bridge.isItemRemovable()) continue
            val cat = model.getCategory(bridge.getUUID()) as? LLViewerInventoryCategory ?: continue
            val descendentItems = mutableListOf<LLViewerInventoryItem>()
            gInventory.collectDescendents(cat.uuid, mutableListOf(), descendentItems, false)
            for (descItem in descendentItems) {
                if (descItem.type == LLAssetType.EType.AT_GESTURE && !descItem.isLinkType) {
                    LLGestureMgr.instance().deactivateGesture(descItem.uuid)
                }
            }
        }
        removeBatchNoCheck(batch)
        model.checkTrashOverflow()
    }

    open fun move(newParentBridge: LLFolderViewModelItem) {}

    open fun isItemCopyable(canCopyAsLink: Boolean = true): Boolean = false
    open fun isItemLinkable(): Boolean = false

    open fun copyToClipboard(): Boolean {
        val obj = gInventory.getObject(uuid) ?: return false
        return if (isItemCopyable() || isItemLinkable()) {
            LLClipboard.instance().addToClipboard(uuid)
        } else false
    }

    open fun cutToClipboard(): Boolean {
        val obj = gInventory.getObject(uuid) ?: return false
        if (!isItemMovable() || !isItemRemovable()) return false

        val marketplaceListingsId = gInventory.marketplaceListingsUUID
        val cutFromMarketplace = gInventory.isObjectDescendentOf(uuid, marketplaceListingsId)
        return if (cutFromMarketplace &&
            (LLMarketplaceData.instance().isInActiveFolder(uuid) ||
                LLMarketplaceData.instance().isListedAndActive(uuid))) {
            val parentUuid = obj.parentUUID
            val result = performCutToClipboard()
            gInventory.addChangedMask(LLInventoryObserver.STRUCTURE, parentUuid)
            result
        } else {
            performCutToClipboard()
        }
    }

    open fun isCutToClipboard(): Boolean {
        return if (LLClipboard.instance().isCutMode()) {
            LLClipboard.instance().isOnClipboard(uuid)
        } else false
    }

    fun callbackCutToClipboard(notification: LLSD, response: LLSD): Boolean {
        val option = LLNotificationsUtil.getSelectedOption(notification, response)
        return if (option == 0) performCutToClipboard() else false
    }

    fun performCutToClipboard(): Boolean {
        val obj = gInventory.getObject(uuid) ?: return false
        return if (isItemMovable() && isItemRemovable()) {
            LLClipboard.instance().setCutMode(true)
            LLClipboard.instance().addToClipboard(uuid)
        } else false
    }

    open fun isClipboardPasteable(): Boolean {
        if (!LLClipboard.instance().hasContents() || !isAgentInventory()) return false
        val model = inventoryModel ?: return false
        if (LLClipboard.instance().isCutMode()) return true

        val objects = mutableListOf<UUID>()
        LLClipboard.instance().pasteFromClipboard(objects)
        for (itemId in objects) {
            val cat = model.getCategory(itemId)
            if (cat != null) {
                val catBr = LLFolderBridge(inventoryPanel.get()!!, root!!, itemId)
                if (!catBr.isItemCopyable(false)) return false
                continue
            }
            val itemBr = LLItemBridge(inventoryPanel.get()!!, root!!, itemId)
            if (!itemBr.isItemCopyable(false)) return false
        }
        return true
    }

    open fun isClipboardPasteableAsLink(): Boolean {
        if (!LLClipboard.instance().hasContents() || !isAgentInventory()) return false
        val model = inventoryModel ?: return false

        val objects = mutableListOf<UUID>()
        LLClipboard.instance().pasteFromClipboard(objects)
        for (itemId in objects) {
            val item = model.getItem(itemId)
            if (item != null) {
                if (!LLAssetType.lookupCanLink(item.actualType)) return false
                if (gInventory.isObjectDescendentOf(item.uuid, gInventory.libraryRootFolderID)) return false
            }
            val cat = model.getCategory(itemId) as? LLViewerInventoryCategory
            if (cat != null && LLFolderType.lookupIsProtectedType(cat.preferredType)) return false
        }
        return true
    }

    open fun pasteFromClipboard() {}
    open fun pasteLinkFromClipboard() {}

    fun getClipboardEntries(
        showAssetId: Boolean,
        items: MenuEntryVec,
        disabledItems: MenuEntryVec,
        flags: UInt
    ) {
        val obj = inventoryObject ?: return
        val singleFolderRoot = (root == null)
        val isCof = isCOFFolder()
        val isInbox = isInboxFolder()

        if (obj.type != LLAssetType.EType.AT_CATEGORY) {
            items.add("Copy Separator")
        }
        items.add("Copy")
        if (!isItemCopyable() && !isItemLinkable()) {
            disabledItems.add("Copy")
        }

        val isAgentInventory = isAgentInventory()
        if (isAgentInventory && !singleFolderRoot && !isCof && !isInbox) {
            items.add("New folder from selected")
            items.add("Subfolder Separator")
        }

        if (isFavorite()) {
            items.add("Remove from Favorites")
        } else if (isAgentInventory && !gInventory.isObjectDescendentOf(
                uuid, gInventory.findCategoryUUIDForType(LLFolderType.EType.FT_TRASH))) {
            items.add("Add to Favorites")
            if (gInventory.rootFolderID == uuid) {
                disabledItems.add("Add to Favorites")
            }
        }

        if (obj.isLinkType) {
            items.add("Find Original")
            if (isLinkedObjectMissing()) disabledItems.add("Find Original")
            items.add("Cut")
            if (!isItemMovable() || !canMenuCut()) disabledItems.add("Cut")
        } else {
            if (LLAssetType.lookupCanLink(obj.type)) items.add("Find Links")

            if (!isInbox && !singleFolderRoot) {
                items.add("Rename")
                if (!isItemRenameable() || (flags and FIRST_SELECTED_ITEM) == 0u || isLockedFolder()) {
                    disabledItems.add("Rename")
                }
            }

            items.add("thumbnail")
            if (isLibraryItem()) disabledItems.add("thumbnail")

            if (showAssetId) {
                items.add("Copy Asset UUID")
                val invItem = gInventory.getItem(uuid)
                val isAssetKnowable = invItem?.let { LLAssetType.lookupIsAssetIDKnowable(it.type) } ?: false
                if (!isAssetKnowable || (!isItemPermissive() && !gAgent.isGodlike()) ||
                    (flags and FIRST_SELECTED_ITEM) == 0u) {
                    disabledItems.add("Copy Asset UUID")
                }
            }

            if (!singleFolderRoot) {
                items.add("Cut")
                if (!isItemMovable() || !canMenuCut()) disabledItems.add("Cut")

                if (canListOnMarketplace() && !isMarketplaceListingsFolder() && !isInboxFolder()) {
                    items.add("Marketplace Separator")
                    items.add("Marketplace Copy")
                    items.add("Marketplace Move")
                    if (!canListOnMarketplaceNow()) {
                        disabledItems.add("Marketplace Copy")
                        disabledItems.add("Marketplace Move")
                    }
                }
            }
        }

        if (!isCof && !isLockedFolder()) items.add("Paste")
        if (!isClipboardPasteable() || (flags and FIRST_SELECTED_ITEM) == 0u) {
            disabledItems.add("Paste")
        }

        items.add("Paste As Link")
        if (!isClipboardPasteableAsLink() || (flags and FIRST_SELECTED_ITEM) == 0u) {
            disabledItems.add("Paste As Link")
        }

        if (obj.type != LLAssetType.EType.AT_CATEGORY) items.add("Paste Separator")

        if (!singleFolderRoot) addDeleteContextMenuOptions(items, disabledItems)

        if (!isPanelActive("All Items") && !isPanelActive("comb_single_folder_inv") &&
            !isPanelActive("inv_panel")) {
            items.add("Show in Main Panel")
        }
    }

    open fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            addOpenRightClickMenuOption(items)
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
        }
        hideContextEntries(menu, items, disabledItems)
    }

    open fun getDragSource(): LLToolDragAndDrop.ESource =
        LLToolDragAndDrop.ESource.SOURCE_AGENT

    open fun startDrag(type: Array<EDragAndDropType>, id: Array<UUID>): Boolean = false

    open fun dragOrDrop(
        mask: Int, drop: Boolean,
        cargoType: EDragAndDropType,
        cargoData: Any?,
        tooltipMsg: StringBuilder
    ): Boolean = false

    open fun getInventoryType(): LLInventoryType.EType = invType
    open fun getWearableType(): LLWearableType.EType = LLWearableType.EType.WT_NONE
    open fun getSettingsType(): LLSettingsType.EType = LLSettingsType.EType.ST_NONE

    fun getSortGroup(): EInventorySortGroup = EInventorySortGroup.SG_ITEM

    open fun getInventoryObject(): LLInventoryObject? =
        inventoryModel?.getObject(uuid)

    // ------------------------------------------------------------------
    // Protected helpers
    // ------------------------------------------------------------------

    protected open fun addTrashContextMenuOptions(items: MenuEntryVec, disabledItems: MenuEntryVec) {
        items.add("Purge Item")
        items.add("Restore Item")
    }

    protected open fun addDeleteContextMenuOptions(items: MenuEntryVec, disabledItems: MenuEntryVec) {
        items.add("Delete")
        if (!isItemRemovable()) disabledItems.add("Delete")
    }

    protected open fun addOpenRightClickMenuOption(items: MenuEntryVec) {
        items.add("Open")
    }

    protected open fun addMarketplaceContextMenuOptions(
        flags: UInt, items: MenuEntryVec, disabledItems: MenuEntryVec
    ) {}

    protected open fun addLinkReplaceMenuOption(items: MenuEntryVec, disabledItems: MenuEntryVec) {}

    protected open fun addMoveToDefaultFolderMenuOption(items: MenuEntryVec) {}

    protected open fun canMenuDelete(): Boolean = isItemRemovable()
    protected open fun canMenuCut(): Boolean = isItemMovable() && isItemRemovable()

    fun canShare(): Boolean {
        return isAgentInventory() && !isItemInTrash() && isItemCopyable()
    }

    fun canListOnMarketplace(): Boolean = !isLibraryItem()
    fun canListOnMarketplaceNow(): Boolean = true

    protected fun getInventoryModel(): LLInventoryModel? = gInventory
    protected fun getInventoryFilter(): LLInventoryFilter? =
        inventoryPanel.get()?.filter

    protected fun isLinkedObjectInTrash(): Boolean {
        if (uuid == UUID_NULL) return false
        val linkedId = gInventory.getLinkedItemID(uuid)
        val trashId = gInventory.findCategoryUUIDForType(LLFolderType.EType.FT_TRASH)
        return gInventory.isObjectDescendentOf(linkedId, trashId)
    }

    protected fun isLinkedObjectMissing(): Boolean {
        if (!isLink) return false
        val linkedId = gInventory.getLinkedItemID(uuid)
        return gInventory.getObject(linkedId) == null
    }

    protected fun isAgentInventory(): Boolean {
        val agentRootId = gInventory.rootFolderID
        return gInventory.isObjectDescendentOf(uuid, agentRootId)
    }

    protected fun isAgentInventoryRoot(): Boolean = uuid == gInventory.rootFolderID

    protected fun isCOFFolder(): Boolean {
        val cofId = gInventory.findCategoryUUIDForType(LLFolderType.EType.FT_CURRENT_OUTFIT)
        return gInventory.isObjectDescendentOf(uuid, cofId) || uuid == cofId
    }

    protected fun isInboxFolder(): Boolean {
        val inboxId = gInventory.findCategoryUUIDForType(LLFolderType.EType.FT_INBOX)
        return gInventory.isObjectDescendentOf(uuid, inboxId) || uuid == inboxId
    }

    protected fun isMarketplaceListingsFolder(): Boolean {
        val marketplaceId = gInventory.marketplaceListingsUUID
        return gInventory.isObjectDescendentOf(uuid, marketplaceId) || uuid == marketplaceId
    }

    protected fun isLibraryInventory(): Boolean =
        gInventory.isObjectDescendentOf(uuid, gInventory.libraryRootFolderID)

    protected fun isLostInventory(): Boolean {
        val lostId = gInventory.findCategoryUUIDForType(LLFolderType.EType.FT_LOST_AND_FOUND)
        return gInventory.isObjectDescendentOf(uuid, lostId)
    }

    protected fun isLockedFolder(ignoreSetting: Boolean = false): Boolean {
        return FSLSLBridge.instance().isLockedFolder(uuid, ignoreSetting)
    }

    protected open fun isItemPermissive(): Boolean = false

    fun purgeItem(model: LLInventoryModel, id: UUID) {
        TODO("APR: use JVM equivalent for purge-item network message")
    }

    fun removeObject(model: LLInventoryModel, id: UUID) {
        TODO("APR: use JVM equivalent for remove-object network message")
    }

    protected open fun buildDisplayName() {}

    fun removeBatchNoCheck(batch: MutableList<LLFolderViewModelItem>) {
        val model = inventoryModel ?: return
        val trashId = model.findCategoryUUIDForType(LLFolderType.EType.FT_TRASH)
        val moveIds = mutableListOf<UUID>()

        for (item in batch) {
            val bridge = item as? LLInvFVBridge ?: continue
            if (!bridge.isItemRemovable()) continue
            val inv = model.getItem(bridge.getUUID()) as? LLViewerInventoryItem
            if (inv != null) {
                LLPreview.hide(inv.uuid)
            }
        }

        TODO("APR: use JVM equivalent for MoveInventoryItem / MoveInventoryFolder network messages")
    }

    companion object {
        const val FIRST_SELECTED_ITEM: UInt = 0x1u
    }
}

// ---------------------------------------------------------------------------
// LLInventoryFolderViewModelBuilder
// ---------------------------------------------------------------------------

open class LLInventoryFolderViewModelBuilder {
    open fun createBridge(
        assetType: LLAssetType.EType,
        actualAssetType: LLAssetType.EType,
        invType: LLInventoryType.EType,
        inventory: LLInventoryPanel,
        viewModel: LLFolderViewModelInventory,
        root: LLFolderView,
        uuid: UUID,
        flags: UInt = 0u
    ): LLInvFVBridge? =
        LLInvFVBridge.createBridge(assetType, actualAssetType, invType, inventory, viewModel, root, uuid, flags)
}

// ---------------------------------------------------------------------------
// LLItemBridge
// ---------------------------------------------------------------------------

open class LLItemBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLInvFVBridge(inventory, root, uuid) {

    open fun performAction(model: LLInventoryModel, action: String) {}
    open fun selectItem() {}

    override fun restoreItem() {
        val item = getItem() ?: return
        val model = inventoryModel ?: return
        val newParentId = model.findCategoryUUIDForType(
            LLFolderType.assetTypeToFolderType(item.type)
        )
        changeItemParent(model, item, newParentId, true)
    }

    override fun restoreToWorld() {}
    open fun gotoItem() {}

    open fun getIcon(): LLUIImagePtr? = LLInventoryIcon.getIcon(
        getItem()?.type ?: LLAssetType.EType.AT_NONE,
        getItem()?.inventoryType ?: LLInventoryType.EType.IT_NONE,
        0, false
    )

    override fun getLabelSuffix(): String = ""
    override fun getLabelStyle(): LLFontGL.StyleFlags = LLFontGL.StyleFlags.NORMAL
    override fun getPermissionMask(): Int = getItem()?.permissionMask ?: PERM_NONE

    override fun getCreationDate(): Long = getItem()?.creationDate ?: 0L

    override fun isItemRenameable(): Boolean {
        val item = getItem() ?: return false
        return gAgent.allowOperation(PERM_MODIFY, item.permissions, GP_OBJECT_MANIPULATE)
    }

    open fun renameItem(newName: String): Boolean {
        val model = inventoryModel ?: return false
        val item = getItem() ?: return false
        if (!isItemRenameable()) return false
        val newItem = LLViewerInventoryItem(item)
        newItem.name = newName
        newItem.updateServer(false)
        model.updateItem(newItem)
        model.notifyObservers()
        return true
    }

    open fun removeItem(): Boolean {
        val item = getItem() ?: return false
        if (!isItemRemovable()) return false
        val model = inventoryModel ?: return false
        val trashId = model.findCategoryUUIDForType(LLFolderType.EType.FT_TRASH)
        changeItemParent(model, item, trashId, false)
        return true
    }

    override fun isItemCopyable(canCopyAsLink: Boolean): Boolean {
        val item = getItem() ?: return false
        if (canCopyAsLink && LLAssetType.lookupCanLink(item.type)) return true
        return gAgent.allowOperation(PERM_COPY, item.permissions, GP_OBJECT_MANIPULATE)
    }

    override fun isItemLinkable(): Boolean {
        val item = getItem() ?: return false
        return LLAssetType.lookupCanLink(item.type)
    }

    open fun hasChildren(): Boolean = false
    open fun isUpToDate(): Boolean = true

    open fun getIconOverlay(): LLUIImagePtr? = null

    fun getItem(): LLViewerInventoryItem? = gInventory.getItem(uuid)

    override fun getThumbnailUUID(): UUID = getItem()?.thumbnailUUID ?: UUID_NULL

    override fun isFavorite(): Boolean = getItem()?.isFavorite ?: false

    protected fun confirmRemoveItem(notification: LLSD, response: LLSD): Boolean {
        val option = LLNotificationsUtil.getSelectedOption(notification, response)
        return if (option == 0) removeItem() else false
    }

    override fun isItemPermissive(): Boolean {
        val item = getItem() ?: return false
        val exportSupported = gAgent.regionExportCapable()
        return if (exportSupported) {
            gAgent.allowOperation(PERM_COPY, item.permissions, GP_OBJECT_MANIPULATE) &&
                gAgent.allowOperation(PERM_MODIFY, item.permissions, GP_OBJECT_MANIPULATE) &&
                gAgent.allowOperation(PERM_TRANSFER, item.permissions, GP_OBJECT_MANIPULATE)
        } else {
            gAgent.allowOperation(PERM_COPY, item.permissions, GP_OBJECT_MANIPULATE) &&
                gAgent.allowOperation(PERM_MODIFY, item.permissions, GP_OBJECT_MANIPULATE)
        }
    }

    override fun buildDisplayName() {
        val item = getItem()
        if (item != null) {
            displayName = item.name
            searchableName = item.name.toUpperCase()
        }
    }

    fun doActionOnCurSelectedLandmark(cb: LLLandmarkList.LoadedCallback) {
        val item = getItem() ?: return
        LLLandmarkList.instance().load(item.assetUUID, cb)
    }
}

// ---------------------------------------------------------------------------
// LLFolderBridge
// ---------------------------------------------------------------------------

open class LLFolderBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLInvFVBridge(inventory, root, uuid) {

    var mCallingCards: Boolean = false
    var mWearables: Boolean = false
    var mIsLoading: Boolean = false
    var mShowDescendantsCount: Boolean = false
    val mTimeSinceRequestStartFolder: LLTimer = LLTimer()
    var mMessage: String = ""

    private var mCanDeleteFolderState: ECanDeleteState = ECanDeleteState.CDS_INIT_FOLDER_CHECK
    private val mFoldersToCheck: MutableList<LLViewerInventoryCategory> = mutableListOf()
    private val mItemsToCheck: MutableList<LLViewerInventoryItem> = mutableListOf()
    private var mLastCheckedVersion: Int = -1
    private var mInProgressVersion: Int = -1
    private var mCanDelete: Boolean = false
    private var mCanCut: Boolean = false

    companion object {
        var sSelf: LLHandle<LLFolderBridge>? = null

        fun staticFolderOptionsMenu() {
            sSelf?.get()?.buildContextMenu(
                LLMenuGL.sMenuContainer!!.getChildMenu("Inventory")!!,
                FIRST_SELECTED_ITEM
            )
        }

        fun createWearable(bridge: LLFolderBridge, type: LLWearableType.EType) {
            TODO("APR: use JVM equivalent for create-wearable in folder")
        }

        fun getIcon(preferredType: LLFolderType.EType): LLUIImagePtr? =
            LLViewerFolderType.lookupFolderIcon(preferredType, false)

        private fun outfitFolderCreatedCallback(
            catSourceId: UUID,
            catDestId: UUID,
            cb: LLInventoryCallback?,
            inventoryPanel: LLHandle<LLInventoryPanel>
        ) {
            TODO("APR: use JVM equivalent for outfit-folder-created callback")
        }
    }

    enum class ECanDeleteState {
        CDS_INIT_FOLDER_CHECK,
        CDS_PROCESSING_ITEMS,
        CDS_PROCESSING_FOLDERS,
        CDS_DONE
    }

    fun dragItemIntoFolder(
        invItem: LLInventoryItem, drop: Boolean,
        tooltipMsg: StringBuilder, userConfirm: Boolean = true,
        cb: LLInventoryCallback? = null
    ): Boolean {
        TODO("APR: use JVM equivalent for drag-item-into-folder")
    }

    fun dragCategoryIntoFolder(
        invCategory: LLInventoryCategory, drop: Boolean,
        tooltipMsg: StringBuilder, isLink: Boolean = false,
        userConfirm: Boolean = true, cb: LLInventoryCallback? = null
    ): Boolean {
        TODO("APR: use JVM equivalent for drag-category-into-folder")
    }

    fun callbackDropItemIntoFolder(notification: LLSD, response: LLSD, invItem: LLInventoryItem) {
        val option = LLNotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) dragItemIntoFolder(invItem, true, StringBuilder())
    }

    fun callbackDropCategoryIntoFolder(notification: LLSD, response: LLSD, invCategory: LLInventoryCategory) {
        val option = LLNotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) dragCategoryIntoFolder(invCategory, true, StringBuilder())
    }

    override fun buildDisplayName() {
        val cat = getCategory()
        if (cat != null) {
            displayName = cat.name
            searchableName = cat.name.toUpperCase()
        }
    }

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "open" -> openItem()
            "properties" -> showProperties()
            "replaceoutfit" -> modifyOutfit(false)
            "addtooutfit" -> modifyOutfit(true)
            "wearitems" -> modifyOutfit(true)
            "removefromoutfit" -> {
                val cat = getCategory() ?: return
                LLAppearanceMgr.instance().takeOffOutfit(cat.uuid)
            }
        }
    }

    override fun openItem() {
        val cat = getCategory() ?: return
        inventoryPanel.get()?.setSelection(mutableSetOf(cat.uuid), false)
    }

    override fun closeItem() {}

    override fun isItemRenameable(): Boolean {
        val cat = getCategory() ?: return false
        return !LLFolderType.lookupIsProtectedType(cat.preferredType) && isAgentInventory()
    }

    override fun selectItem() {}

    override fun restoreItem() {
        val cat = getCategory() ?: return
        val model = inventoryModel ?: return
        val newParentId = model.rootFolderID
        changeCategoryParent(model, cat, newParentId, false)
    }

    override fun getPreferredType(): LLFolderType.EType =
        getCategory()?.preferredType ?: LLFolderType.EType.FT_NONE

    open fun getIcon(): LLUIImagePtr? = getFolderIcon(false)
    open fun getIconOpen(): LLUIImagePtr? = getFolderIcon(true)
    open fun getIconOverlay(): LLUIImagePtr? = null

    override fun getLabelSuffix(): String {
        if (mShowDescendantsCount) {
            val cat = getCategory() ?: return ""
            val descendentCount = cat.descendentCount
            return if (descendentCount > 0) " ($descendentCount)" else ""
        }
        return ""
    }

    override fun getLabelStyle(): LLFontGL.StyleFlags = LLFontGL.StyleFlags.NORMAL

    override fun getThumbnailUUID(): UUID = getCategory()?.thumbnailUUID ?: UUID_NULL
    override fun isFavorite(): Boolean = getCategory()?.isFavorite ?: false

    fun setShowDescendantsCount(showCount: Boolean) {
        mShowDescendantsCount = showCount
    }

    open fun renameItem(newName: String): Boolean {
        val cat = getCategory() ?: return false
        val model = inventoryModel ?: return false
        if (!isItemRenameable()) return false
        val newCat = LLViewerInventoryCategory(cat)
        newCat.name = newName
        newCat.updateServer()
        model.updateCategory(newCat)
        model.notifyObservers()
        return true
    }

    open fun removeItem(): Boolean {
        val cat = getCategory() ?: return false
        val model = inventoryModel ?: return false
        if (!isItemRemovable()) return false
        val trashId = model.findCategoryUUIDForType(LLFolderType.EType.FT_TRASH)
        changeCategoryParent(model, cat, trashId, false)
        return true
    }

    fun removeSystemFolder(): Boolean {
        TODO("APR: use JVM equivalent for remove-system-folder network message")
    }

    fun removeItemResponse(notification: LLSD, response: LLSD): Boolean {
        val option = LLNotificationsUtil.getSelectedOption(notification, response)
        return if (option == 0) removeItem() else false
    }

    fun updateHierarchyCreationDate(date: Long) {
        val model = inventoryModel ?: return
        val cat = getCategory() ?: return
        if (cat.creationDate < date) {
            val newCat = LLViewerInventoryCategory(cat)
            newCat.creationDate = date
            model.updateCategory(newCat)
        }
    }

    override fun pasteFromClipboard() {
        val model = inventoryModel ?: return
        val objects = mutableListOf<UUID>()
        LLClipboard.instance().pasteFromClipboard(objects)
        for (itemId in objects) {
            val item = model.getItem(itemId)
            if (item != null) {
                if (LLClipboard.instance().isCutMode()) {
                    changeItemParent(model, item as LLViewerInventoryItem, uuid, false)
                } else {
                    TODO("APR: use JVM equivalent for copy-item network message")
                }
            }
            val cat = model.getCategory(itemId)
            if (cat != null) {
                if (LLClipboard.instance().isCutMode()) {
                    changeCategoryParent(model, cat as LLViewerInventoryCategory, uuid, false)
                } else {
                    TODO("APR: use JVM equivalent for copy-category network message")
                }
            }
        }
        if (LLClipboard.instance().isCutMode()) {
            LLClipboard.instance().reset()
        }
    }

    override fun pasteLinkFromClipboard() {
        TODO("APR: use JVM equivalent for paste-link network message")
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            buildContextMenuOptions(flags, items, disabledItems)
        }
        hideContextEntries(menu, items, disabledItems)
    }

    open fun hasChildren(): Boolean {
        val cat = getCategory() ?: return false
        val descendentCount = cat.descendentCount
        return if (descendentCount == LLViewerInventoryCategory.DESCENDENT_COUNT_UNKNOWN) {
            true
        } else {
            descendentCount > 0
        }
    }

    override fun dragOrDrop(
        mask: Int, drop: Boolean,
        cargoType: EDragAndDropType,
        cargoData: Any?,
        tooltipMsg: StringBuilder
    ): Boolean {
        return when (cargoType) {
            EDragAndDropType.DAD_CATEGORY -> {
                val cat = cargoData as? LLInventoryCategory ?: return false
                dragCategoryIntoFolder(cat, drop, tooltipMsg)
            }
            EDragAndDropType.DAD_ITEM -> {
                val item = cargoData as? LLInventoryItem ?: return false
                dragItemIntoFolder(item, drop, tooltipMsg)
            }
            else -> false
        }
    }

    override fun isItemRemovable(checkWorn: Boolean): Boolean {
        val model = inventoryModel ?: return false
        val cat = model.getCategory(uuid) ?: return false
        if (LLFolderType.lookupIsProtectedType(cat.preferredType)) return false
        if (isLockedFolder()) return false
        return super.isItemRemovable(checkWorn)
    }

    override fun isItemMovable(): Boolean {
        val cat = getCategory()
        if (cat != null && LLFolderType.lookupIsProtectedType(cat.preferredType)) return false
        return super.isItemMovable()
    }

    open fun isUpToDate(): Boolean = !mIsLoading

    override fun isItemCopyable(canCopyAsLink: Boolean): Boolean {
        val cat = getCategory() ?: return false
        if (LLFolderType.lookupIsProtectedType(cat.preferredType)) return false
        val model = inventoryModel ?: return false
        val items = mutableListOf<LLViewerInventoryItem>()
        model.collectDescendents(uuid, mutableListOf(), items, false)
        for (item in items) {
            if (!gAgent.allowOperation(PERM_COPY, item.permissions, GP_OBJECT_MANIPULATE)) return false
        }
        return true
    }

    override fun isItemLinkable(): Boolean = true

    override fun isClipboardPasteable(): Boolean {
        if (!super.isClipboardPasteable()) return false
        val cat = getCategory() ?: return false
        return !LLFolderType.lookupIsProtectedType(cat.preferredType)
    }

    override fun isClipboardPasteableAsLink(): Boolean = super.isClipboardPasteableAsLink()

    override fun getSortGroup(): EInventorySortGroup {
        val cat = getCategory()
        return if (cat != null && LLFolderType.lookupIsProtectedType(cat.preferredType)) {
            EInventorySortGroup.SG_SYSTEM_FOLDER
        } else {
            EInventorySortGroup.SG_NORMAL_FOLDER
        }
    }

    open fun update() {}

    fun getCategory(): LLViewerInventoryCategory? = gInventory.getCategory(uuid)

    fun isLoading(): Boolean = mIsLoading

    open fun isLocked(): Boolean = isLockedFolder()

    open fun isProtected(): Boolean {
        val cat = getCategory() ?: return false
        return LLFolderType.lookupIsProtectedType(cat.preferredType)
    }

    protected fun buildContextMenuOptions(
        flags: UInt, items: MenuEntryVec, disabledItems: MenuEntryVec
    ) {
        items.add("Share")
        if (!canShare()) disabledItems.add("Share")
        items.add("Open Folder")
        items.add("Properties")
        addMarketplaceContextMenuOptions(flags, items, disabledItems)
        getClipboardEntries(false, items, disabledItems, flags)
        buildContextMenuFolderOptions(flags, items, disabledItems)
    }

    protected fun buildContextMenuFolderOptions(
        flags: UInt, items: MenuEntryVec, disabledItems: MenuEntryVec
    ) {
        val cat = getCategory() ?: return
        val model = inventoryModel ?: return

        if (LLFolderType.lookupIsProtectedType(cat.preferredType)) {
            disabledItems.add("Delete")
            disabledItems.add("Cut")
            disabledItems.add("Rename")
        }

        val isOutfitFolder = (cat.preferredType == LLFolderType.EType.FT_OUTFIT)
        if (isOutfitFolder) {
            items.add("Outfit Separator")
            items.add("Replace Outfit")
            items.add("Add To Outfit")
            items.add("Remove From Outfit")
        }
    }

    protected fun addOpenFolderMenuOptions(flags: UInt, items: MenuEntryVec) {}
    protected fun addInventoryFavoritesMenuOptions(items: MenuEntryVec) {}

    private fun modifyOutfit(append: Boolean) {
        val cat = getCategory() ?: return
        LLAppearanceMgr.instance().wearInventoryCategory(cat, false, append)
    }

    private fun getFolderIcon(isOpen: Boolean): LLUIImagePtr? {
        val cat = getCategory()
        val preferredType = cat?.preferredType ?: LLFolderType.EType.FT_NONE
        return LLViewerFolderType.lookupFolderIcon(preferredType, isOpen)
    }

    override fun canMenuDelete(): Boolean {
        return when (mCanDeleteFolderState) {
            ECanDeleteState.CDS_DONE -> mCanDelete
            else -> false
        }
    }

    override fun canMenuCut(): Boolean {
        return when (mCanDeleteFolderState) {
            ECanDeleteState.CDS_DONE -> mCanCut
            else -> false
        }
    }
}

// ---------------------------------------------------------------------------
// Concrete bridge types
// ---------------------------------------------------------------------------

open class LLTextureBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID,
    type: LLInventoryType.EType
) : LLItemBridge(inventory, root, uuid) {

    protected var fileName: String = ""

    init {
        @Suppress("LeakingThis")
        (this as LLInvFVBridge).apply {
            // mInvType is val in base; shadowing via local var in C++ — set via reflection or design
        }
    }

    open fun getIcon(): LLUIImagePtr? =
        LLInventoryIcon.getIcon(LLAssetType.EType.AT_TEXTURE, invType, 0, false)

    override fun openItem() {
        LLFloaterReg.showInstance("preview_texture", LLSD().with("id", uuid), true)
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            addOpenRightClickMenuOption(items)
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
            items.add("Texture Separator")
            items.add("Save As")
            if (!canSaveTexture()) disabledItems.add("Save As")
        }
        hideContextEntries(menu, items, disabledItems)
    }

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "open" -> openItem()
            "save_as" -> {
                val item = getItem() ?: return
                LLPreviewTexture.saveAs(item)
            }
        }
    }

    fun canSaveTexture(): Boolean {
        val item = getItem() ?: return false
        return gAgent.allowOperation(PERM_MODIFY, item.permissions, GP_OBJECT_MANIPULATE) ||
            gAgent.allowOperation(PERM_TRANSFER, item.permissions, GP_OBJECT_MANIPULATE)
    }

    fun setFileName(fileName: String) { this.fileName = fileName }
}

open class LLSoundBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLItemBridge(inventory, root, uuid) {

    override fun openItem() {
        val item = getItem() ?: return
        LLViewerMedia.playSoundPreview(item.assetUUID)
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            addOpenRightClickMenuOption(items)
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
            items.add("Sound Separator")
            items.add("Sound Play")
        }
        hideContextEntries(menu, items, disabledItems)
    }

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "open", "play" -> openItem()
        }
    }

    companion object {
        fun openSoundPreview(userData: Any?) {
            val id = userData as? UUID ?: return
            LLFloaterReg.showInstance("preview_sound", LLSD().with("id", id), true)
        }
    }
}

open class LLLandmarkBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID,
    flags: UInt = 0u
) : LLItemBridge(inventory, root, uuid) {

    protected var mVisited: Boolean = (flags and LLInventoryItem.II_FLAGS_LANDMARK_VISITED) != 0u

    override fun getIcon(): LLUIImagePtr? =
        LLInventoryIcon.getIcon(LLAssetType.EType.AT_LANDMARK, invType, if (mVisited) 1 else 0, false)

    override fun openItem() {
        val item = getItem() ?: return
        LLLandmarkActions.gotoLandmark(item.assetUUID)
    }

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "open", "teleport" -> openItem()
            "about" -> LLFloaterReg.showInstance("preview_landmark", LLSD().with("id", uuid), true)
            "show_on_map" -> doActionOnCurSelectedLandmark { landmark, _ ->
                LLFloaterWorldMap.instance().trackLandmark(landmark.uuid)
            }
            "copy_slurl" -> doActionOnCurSelectedLandmark { landmark, _ ->
                TODO("APR: use JVM equivalent for copy SLURL to clipboard")
            }
        }
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            items.add("Landmark Open")
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
            items.add("Landmark Separator")
            items.add("Teleport To")
            items.add("About Landmark")
        }
        hideContextEntries(menu, items, disabledItems)
    }
}

open class LLCallingCardBridge(
    inventory: LLInventoryPanel,
    folder: LLFolderView?,
    uuid: UUID
) : LLItemBridge(inventory, folder, uuid) {

    private val observer: LLCallingCardObserver = LLCallingCardObserver(this)
    private var creatorUUID: UUID = UUID_NULL

    init {
        val item = getItem()
        if (item != null) {
            creatorUUID = item.creatorUUID
            LLAvatarTracker.instance().addObserver(observer)
        }
    }

    fun destroy() {
        LLAvatarTracker.instance().removeObserver(observer)
    }

    override fun getLabelSuffix(): String {
        val isOnline = LLAvatarTracker.instance().isBuddyOnline(creatorUUID)
        return if (isOnline) " (online)" else ""
    }

    override fun getIcon(): LLUIImagePtr? {
        val isOnline = LLAvatarTracker.instance().isBuddyOnline(creatorUUID)
        return LLInventoryIcon.getIcon(
            LLAssetType.EType.AT_CALLINGCARD, invType, if (isOnline) 1 else 0, false
        )
    }

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "open" -> openItem()
            "im" -> {
                val item = getItem() ?: return
                LLAvatarActions.startIM(item.creatorUUID)
            }
            "invite_to_group" -> {
                val item = getItem() ?: return
                LLAvatarActions.inviteToGroup(item.creatorUUID)
            }
        }
    }

    override fun openItem() {
        val item = getItem() ?: return
        LLAvatarActions.showProfile(item.creatorUUID)
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            items.add("Open")
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
            items.add("Calling Card Separator")
            items.add("Send Instant Message")
            items.add("Request Teleport")
            items.add("Invite to Group")
        }
        hideContextEntries(menu, items, disabledItems)
    }

    override fun dragOrDrop(
        mask: Int, drop: Boolean,
        cargoType: EDragAndDropType,
        cargoData: Any?,
        tooltipMsg: StringBuilder
    ): Boolean {
        val item = getItem() ?: return false
        return when (cargoType) {
            EDragAndDropType.DAD_CALLINGCARD -> {
                val invItem = cargoData as? LLInventoryItem ?: return false
                if (drop) LLGiveInventory.doGiveInventoryItem(item.creatorUUID, invItem)
                true
            }
            else -> false
        }
    }

    fun refreshFolderViewItem() {
        val fvItem = inventoryPanel.get()?.getItemByID(uuid)
        fvItem?.refresh()
    }

    fun checkSearchBySuffixChanges() {}
}

open class LLNotecardBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLItemBridge(inventory, root, uuid) {

    override fun openItem() {
        LLFloaterReg.showInstance("preview_notecard", LLSD().with("id", uuid), true)
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            addOpenRightClickMenuOption(items)
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
        }
        hideContextEntries(menu, items, disabledItems)
    }
}

open class LLGestureBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLItemBridge(inventory, root, uuid) {

    override fun getLabelStyle(): LLFontGL.StyleFlags {
        return if (LLGestureMgr.instance().isGestureActive(uuid)) {
            LLFontGL.StyleFlags.BOLD
        } else LLFontGL.StyleFlags.NORMAL
    }

    override fun getLabelSuffix(): String {
        return if (LLGestureMgr.instance().isGestureActive(uuid)) " (active)" else ""
    }

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "open" -> openItem()
            "activate" -> LLGestureMgr.instance().activateGesture(uuid)
            "deactivate" -> LLGestureMgr.instance().deactivateGesture(uuid)
            "play" -> playGesture(uuid)
        }
    }

    override fun openItem() {
        LLFloaterReg.showInstance("preview_gesture", LLSD().with("id", uuid), true)
    }

    override fun removeItem(): Boolean {
        LLGestureMgr.instance().deactivateGesture(uuid)
        return super.removeItem()
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            addOpenRightClickMenuOption(items)
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
            items.add("Gesture Separator")
            items.add("Activate")
            items.add("Deactivate")
        }
        hideContextEntries(menu, items, disabledItems)
    }

    companion object {
        fun playGesture(itemId: UUID) {
            val item = gInventory.getItem(itemId) ?: return
            LLGestureMgr.instance().playGesture(item.assetUUID)
        }
    }
}

open class LLAnimationBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLItemBridge(inventory, root, uuid) {

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "open" -> openItem()
            "anim_play" -> {
                val item = getItem() ?: return
                gAgent.sendAnimationRequest(item.assetUUID, ANIM_REQUEST_START)
            }
            "anim_stop" -> {
                val item = getItem() ?: return
                gAgent.sendAnimationRequest(item.assetUUID, ANIM_REQUEST_STOP)
            }
        }
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            addOpenRightClickMenuOption(items)
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
            items.add("Animation Separator")
            items.add("Anim Play")
            items.add("Anim Audition")
        }
        hideContextEntries(menu, items, disabledItems)
    }

    override fun openItem() {
        LLFloaterReg.showInstance("preview_anim", LLSD().with("id", uuid), true)
    }
}

open class LLObjectBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID,
    type: LLInventoryType.EType,
    flags: UInt
) : LLItemBridge(inventory, root, uuid) {

    protected val attachPt: UInt = flags and 0xFFu
    protected val isMultiObject: Boolean = (flags and LLInventoryItem.II_FLAGS_OBJECT_HAS_MULTIPLE_ITEMS) != 0u

    open fun getIcon(): LLUIImagePtr? =
        LLInventoryIcon.getIcon(LLAssetType.EType.AT_OBJECT, invType, attachPt.toInt(), isMultiObject)

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "open" -> openItem()
            "attach" -> {
                val item = getItem() ?: return
                LLAppearanceMgr.instance().wearItemOnAvatar(item.uuid, true, true)
            }
            "detach" -> {
                val item = getItem() ?: return
                LLAppearanceMgr.instance().removeItemFromAvatar(item.uuid)
            }
        }
    }

    override fun openItem() {
        val item = getItem() ?: return
        LLFloaterReg.showInstance("openobject", LLSD().with("id", item.uuid), true)
    }

    open fun isItemWearable(): Boolean = true

    override fun getLabelSuffix(): String {
        val worn = getIsItemWorn(uuid)
        return if (worn) " (worn)" else ""
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            addOpenRightClickMenuOption(items)
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
            items.add("Object Separator")
            items.add("Wearable And Object Wear")
            items.add("Wearable Add")
            items.add("Wearable And Object Remove")
        }
        hideContextEntries(menu, items, disabledItems)
    }

    override fun renameItem(newName: String): Boolean {
        val item = getItem() ?: return false
        if (getIsItemWorn(uuid)) {
            val obj = getObject() as? LLViewerObject
            obj?.setName(newName)
        }
        return super.renameItem(newName)
    }

    fun getObject(): LLInventoryObject? = gInventory.getObject(uuid)
    override fun getItem(): LLViewerInventoryItem? = gInventory.getItem(uuid)
    fun getCategory(): LLViewerInventoryCategory? = gInventory.getCategory(uuid)

    companion object {
        var sContextMenuItemID: UUID = UUID_NULL
    }
}

open class LLLSLTextBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLItemBridge(inventory, root, uuid) {

    override fun openItem() {
        LLFloaterReg.showInstance("preview_script", LLSD().with("id", uuid), true)
    }
}

open class LLWearableBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID,
    val assetType: LLAssetType.EType,
    invType: LLInventoryType.EType,
    val wearableType: LLWearableType.EType
) : LLItemBridge(inventory, root, uuid) {

    override fun getIcon(): LLUIImagePtr? =
        LLInventoryIcon.getIcon(assetType, invType, wearableType.ordinal, false)

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "open", "wear" -> wearOnAvatar()
            "wear_add" -> wearAddOnAvatar()
            "edit" -> editOnAvatar()
            "take_off" -> removeFromAvatar()
        }
    }

    override fun openItem() { wearOnAvatar() }
    open fun isItemWearable(): Boolean = true

    override fun getLabelSuffix(): String {
        val isWorn = getIsItemWorn(uuid)
        val isEdited = isEdited()
        return when {
            isEdited -> " (edited)"
            isWorn -> " (worn)"
            else -> ""
        }
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            addOpenRightClickMenuOption(items)
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
            items.add("Wearable Separator")
            items.add("Wearable Wear")
            items.add("Wearable Add")
            items.add("Wearable Edit")
            items.add("Take Off")
        }
        hideContextEntries(menu, items, disabledItems)
    }

    override fun renameItem(newName: String): Boolean {
        val wearable = LLAgentWearables.instance().getWearableFromItemID(uuid)
        wearable?.name = newName
        return super.renameItem(newName)
    }

    override fun getWearableType(): LLWearableType.EType = wearableType

    fun wearOnAvatar() {
        val item = getItem() ?: return
        LLAppearanceMgr.instance().wearItemOnAvatar(item.uuid, true, false)
    }

    fun wearAddOnAvatar() {
        val item = getItem() ?: return
        LLAppearanceMgr.instance().wearItemOnAvatar(item.uuid, true, true)
    }

    fun editOnAvatar() {
        val item = getItem() ?: return
        LLAgentWearables.instance().editWearable(item.uuid)
    }

    fun removeFromAvatar() {
        val item = getItem() ?: return
        LLAppearanceMgr.instance().removeItemFromAvatar(item.uuid)
    }

    private fun isEdited(): Boolean {
        val wearable = LLAgentWearables.instance().getWearableFromItemID(uuid)
        return wearable?.isDirty ?: false
    }

    companion object {
        fun onWearOnAvatar(userData: Any?) {
            val bridge = userData as? LLWearableBridge ?: return
            bridge.wearOnAvatar()
        }

        fun canWearOnAvatar(userData: Any?): Boolean {
            val bridge = userData as? LLWearableBridge ?: return false
            return !getIsItemWorn(bridge.uuid)
        }

        fun canEditOnAvatar(userData: Any?): Boolean {
            val bridge = userData as? LLWearableBridge ?: return false
            return getIsItemWorn(bridge.uuid)
        }

        fun onEditOnAvatar(userData: Any?) {
            val bridge = userData as? LLWearableBridge ?: return
            bridge.editOnAvatar()
        }

        fun canRemoveFromAvatar(userData: Any?): Boolean {
            val bridge = userData as? LLWearableBridge ?: return false
            return getIsItemWorn(bridge.uuid)
        }
    }
}

open class LLLinkItemBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLItemBridge(inventory, root, uuid) {

    open fun getPrefix(): String = sPrefix

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Find Original")
            if (isLinkedObjectMissing()) disabledItems.add("Find Original")
            getClipboardEntries(false, items, disabledItems, flags)
        }
        hideContextEntries(menu, items, disabledItems)
    }

    companion object {
        var sPrefix: String = ""
    }
}

open class LLUnknownItemBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLItemBridge(inventory, root, uuid) {

    open fun getIcon(): LLUIImagePtr? =
        LLInventoryIcon.getIcon(LLAssetType.EType.AT_NONE, invType, 0, false)

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        getClipboardEntries(false, items, disabledItems, flags)
        hideContextEntries(menu, items, disabledItems)
    }
}

open class LLLinkFolderBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLItemBridge(inventory, root, uuid) {

    open fun getPrefix(): String = sPrefix

    open fun getIcon(): LLUIImagePtr? =
        LLViewerFolderType.lookupFolderIcon(LLFolderType.EType.FT_NONE, false)

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Find Original")
            if (isLinkedObjectMissing()) disabledItems.add("Find Original")
            getClipboardEntries(false, items, disabledItems, flags)
        }
        hideContextEntries(menu, items, disabledItems)
    }

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "goto" -> gotoItem()
        }
    }

    open fun gotoItem() {
        val folderId = getFolderID()
        val item = gInventory.getItem(folderId)
        if (item != null) {
            LLInventoryPanel.openInventoryPanelAndSetSelection(true, folderId)
        }
    }

    protected fun getFolderID(): UUID {
        val linkedId = gInventory.getLinkedItemID(uuid)
        return linkedId
    }

    companion object {
        var sPrefix: String = ""
    }
}

open class LLSettingsBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID,
    val settingsType: LLSettingsType.EType
) : LLItemBridge(inventory, root, uuid) {

    open fun getIcon(): LLUIImagePtr? =
        LLInventoryIcon.getIcon(LLAssetType.EType.AT_SETTINGS, invType, settingsType.ordinal, false)

    open fun performAction(model: LLInventoryModel, action: String) {
        when (action) {
            "open" -> openItem()
            "apply_local" -> {
                val item = getItem() ?: return
                LLEnvironment.instance().setEnvironment(LLEnvironment.ENV_LOCAL, item.assetUUID)
            }
            "apply_parcel" -> {
                if (!canUpdateParcel()) return
                val item = getItem() ?: return
                LLEnvironment.instance().setEnvironment(LLEnvironment.ENV_PARCEL, item.assetUUID)
            }
            "apply_region" -> {
                if (!canUpdateRegion()) return
                val item = getItem() ?: return
                LLEnvironment.instance().setEnvironment(LLEnvironment.ENV_REGION, item.assetUUID)
            }
        }
    }

    override fun openItem() {
        LLFloaterReg.showInstance("settings_editor", LLSD().with("id", uuid), true)
    }

    override fun isMultiPreviewAllowed(): Boolean = false

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            addOpenRightClickMenuOption(items)
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
            items.add("Settings Separator")
            items.add("Settings Apply Local")
            items.add("Settings Apply Parcel")
            if (!canUpdateParcel()) disabledItems.add("Settings Apply Parcel")
            items.add("Settings Apply Region")
            if (!canUpdateRegion()) disabledItems.add("Settings Apply Region")
        }
        hideContextEntries(menu, items, disabledItems)
    }

    override fun renameItem(newName: String): Boolean = super.renameItem(newName)

    override fun isItemRenameable(): Boolean = super.isItemRenameable()

    override fun getSettingsType(): LLSettingsType.EType = settingsType

    protected fun canUpdateRegion(): Boolean {
        return gAgent.isCapabilityAvailable("UpdateEnvironment") && gAgent.canManageEstate()
    }

    protected fun canUpdateParcel(): Boolean {
        val parcel = LLViewerParcelMgr.instance().agentParcel ?: return false
        return parcel.allowModifyBy(gAgent.id, gAgent.groupID)
    }
}

open class LLMaterialBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLItemBridge(inventory, root, uuid) {

    override fun openItem() {
        LLFloaterReg.showInstance("material_editor", LLSD().with("id", uuid), true)
    }

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        val items: MenuEntryVec = mutableListOf()
        val disabledItems: MenuEntryVec = mutableListOf()
        if (isItemInTrash()) {
            addTrashContextMenuOptions(items, disabledItems)
        } else {
            items.add("Share")
            if (!canShare()) disabledItems.add("Share")
            addOpenRightClickMenuOption(items)
            items.add("Properties")
            getClipboardEntries(true, items, disabledItems, flags)
        }
        hideContextEntries(menu, items, disabledItems)
    }
}

// ---------------------------------------------------------------------------
// LLInvFVBridgeAction
// ---------------------------------------------------------------------------

abstract class LLInvFVBridgeAction protected constructor(
    protected val uuid: UUID,
    protected val model: LLInventoryModel
) {
    abstract fun doIt()

    protected fun getItem(): LLViewerInventoryItem? = model.getItem(uuid)

    companion object {
        fun createAction(
            assetType: LLAssetType.EType,
            uuid: UUID,
            model: LLInventoryModel
        ): LLInvFVBridgeAction? = null

        fun doAction(assetType: LLAssetType.EType, uuid: UUID, model: LLInventoryModel) {
            createAction(assetType, uuid, model)?.doIt()
        }

        fun doAction(uuid: UUID, model: LLInventoryModel) {
            val item = model.getItem(uuid) ?: return
            doAction(item.type, uuid, model)
        }
    }
}

// ---------------------------------------------------------------------------
// Recent / Favorites / Worn inventory panel bridge variants
// ---------------------------------------------------------------------------

open class LLRecentItemsFolderBridge(
    type: LLInventoryType.EType,
    inventory: LLInventoryPanel,
    root: LLFolderView,
    uuid: UUID
) : LLFolderBridge(inventory, root, uuid) {

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        super.buildContextMenu(menu, flags)
        // Remove "New..." creation entries — recent panel shows no new-item options
    }
}

open class LLRecentInventoryBridgeBuilder : LLInventoryFolderViewModelBuilder() {
    override fun createBridge(
        assetType: LLAssetType.EType,
        actualAssetType: LLAssetType.EType,
        invType: LLInventoryType.EType,
        inventory: LLInventoryPanel,
        viewModel: LLFolderViewModelInventory,
        root: LLFolderView,
        uuid: UUID,
        flags: UInt
    ): LLInvFVBridge? {
        return if (assetType == LLAssetType.EType.AT_CATEGORY) {
            LLRecentItemsFolderBridge(invType, inventory, root, uuid)
        } else {
            super.createBridge(assetType, actualAssetType, invType, inventory, viewModel, root, uuid, flags)
        }
    }
}

open class LLFavoritesFolderBridge(
    type: LLInventoryType.EType,
    inventory: LLInventoryPanel,
    root: LLFolderView,
    uuid: UUID
) : LLFolderBridge(inventory, root, uuid) {

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        super.buildContextMenu(menu, flags)
    }

    open fun canSortContent(): Boolean = true
}

open class LLFavoritesInventoryBridgeBuilder : LLInventoryFolderViewModelBuilder() {
    override fun createBridge(
        assetType: LLAssetType.EType,
        actualAssetType: LLAssetType.EType,
        invType: LLInventoryType.EType,
        inventory: LLInventoryPanel,
        viewModel: LLFolderViewModelInventory,
        root: LLFolderView,
        uuid: UUID,
        flags: UInt
    ): LLInvFVBridge? {
        return if (assetType == LLAssetType.EType.AT_CATEGORY) {
            LLFavoritesFolderBridge(invType, inventory, root, uuid)
        } else {
            super.createBridge(assetType, actualAssetType, invType, inventory, viewModel, root, uuid, flags)
        }
    }
}

open class LLWornItemsFolderBridge(
    type: LLInventoryType.EType,
    inventory: LLInventoryPanel,
    root: LLFolderView,
    uuid: UUID
) : LLFolderBridge(inventory, root, uuid) {

    override fun buildContextMenu(menu: LLMenuGL, flags: UInt) {
        super.buildContextMenu(menu, flags)
    }
}

open class LLWornInventoryBridgeBuilder : LLInventoryFolderViewModelBuilder() {
    override fun createBridge(
        assetType: LLAssetType.EType,
        actualAssetType: LLAssetType.EType,
        invType: LLInventoryType.EType,
        inventory: LLInventoryPanel,
        viewModel: LLFolderViewModelInventory,
        root: LLFolderView,
        uuid: UUID,
        flags: UInt
    ): LLInvFVBridge? {
        return if (assetType == LLAssetType.EType.AT_CATEGORY) {
            LLWornItemsFolderBridge(invType, inventory, root, uuid)
        } else {
            super.createBridge(assetType, actualAssetType, invType, inventory, viewModel, root, uuid, flags)
        }
    }
}

// ---------------------------------------------------------------------------
// LLMarketplaceFolderBridge
// ---------------------------------------------------------------------------

open class LLMarketplaceFolderBridge(
    inventory: LLInventoryPanel,
    root: LLFolderView?,
    uuid: UUID
) : LLFolderBridge(inventory, root, uuid) {

    private var depth: Int = -1
    private var stockCountCache: Int = -1

    override fun getIcon(): LLUIImagePtr? = getMarketplaceFolderIcon(false)
    override fun getIconOpen(): LLUIImagePtr? = getMarketplaceFolderIcon(true)

    override fun getLabelSuffix(): String {
        val count = stockCountCache
        return if (count >= 0) " [$count]" else ""
    }

    override fun getLabelStyle(): LLFontGL.StyleFlags {
        val listingId = LLMarketplaceData.instance().getListingID(uuid)
        return if (listingId != UUID_NULL &&
            LLMarketplaceData.instance().getActivationState(listingId)) {
            LLFontGL.StyleFlags.BOLD
        } else LLFontGL.StyleFlags.NORMAL
    }

    private fun getMarketplaceFolderIcon(isOpen: Boolean): LLUIImagePtr? {
        return LLViewerFolderType.lookupFolderIcon(getPreferredType(), isOpen)
    }
}

// ---------------------------------------------------------------------------
// Module-level utility functions
// ---------------------------------------------------------------------------

fun rezAttachment(
    item: LLViewerInventoryItem,
    attachment: LLViewerJointAttachment?,
    replace: Boolean
) {
    TODO("APR: use JVM equivalent for rez-attachment network message")
}

fun moveInvCategoryWorldToAgent(
    objectId: UUID,
    categoryId: UUID,
    drop: Boolean,
    callback: ((Int, Any?, LLMoveInv) -> Unit)? = null,
    userData: Any? = null,
    filter: LLInventoryFilter? = null
): Boolean {
    TODO("APR: use JVM equivalent for move-task-inventory network message")
}

fun hideContextEntries(
    menu: LLMenuGL,
    entriesToShow: List<String>,
    disabledEntries: List<String>
) {
    for (child in menu.children) {
        val name = child.name
        val show = entriesToShow.contains(name)
        child.isVisible = show
        if (show) {
            child.isEnabled = !disabledEntries.contains(name)
        }
    }
}

fun isAddAction(action: String): Boolean =
    action == "wear" || action == "attach" || action == "activate"

fun isRemoveAction(action: String): Boolean =
    action == "take_off" || action == "detach"

fun isMarketplaceCopyAction(action: String): Boolean =
    action == "copy_to_marketplace"

fun isMarketplaceSendAction(action: String): Boolean =
    action == "send_to_marketplace"

// ---------------------------------------------------------------------------
// LLFolderViewGroupedItemBridge
// ---------------------------------------------------------------------------

open class LLFolderViewGroupedItemBridge : LLFolderViewGroupedItemModel() {
    open fun groupFilterContextMenu(
        selectedItems: ArrayDeque<LLFolderViewItem>,
        menu: LLMenuGL
    ) {}

    fun canWearSelected(itemIds: List<UUID>): Boolean {
        for (id in itemIds) {
            val item = gInventory.getItem(id) ?: return false
            if (!getIsItemWorn(id)) return true
        }
        return false
    }
}

fun warnMoveInventory(obj: LLViewerObject, moveInv: LLMoveInv) {
    TODO("APR: use JVM equivalent for warn-move-inventory dialog")
}

fun moveTaskInventoryCallback(
    notification: LLSD,
    response: LLSD,
    moveInv: LLMoveInv
): Boolean {
    TODO("APR: use JVM equivalent for move-task-inventory-callback")
}
