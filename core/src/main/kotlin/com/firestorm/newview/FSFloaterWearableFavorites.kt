package com.firestorm.newview

import java.util.UUID

// =============================================================================
// WearableItemsSortOrder — mirrors LLWearableItemsList::ESortOrder
// =============================================================================

enum class WearableItemsSortOrder(val code: Int) {
    BY_NAME(0),
    BY_MOST_RECENT(1),
    BY_TYPE_NAME(2),
    ;

    companion object {
        fun fromCode(code: Int): WearableItemsSortOrder =
            entries.firstOrNull { it.code == code } ?: BY_NAME
    }
}

// =============================================================================
// WFDragAndDropType — mirrors EWFDragAndDropType values used here
// =============================================================================

enum class WFWFDragAndDropType { BODYPART, CLOTHING, OBJECT, OTHER }

// =============================================================================
// FSWearableFavoritesItemsList
// =============================================================================

/**
 * Wearable items list that accepts drag-and-drop of body parts, clothing,
 * and objects, firing registered callbacks when an item is dropped.
 *
 * Mirrors `FSWearableFavoritesItemsList` from `fsfloaterwearablefavorites.h/.cpp`.
 */
open class FSWearableFavoritesItemsList {

    private val dadCallbacks: MutableList<(UUID) -> Unit> = mutableListOf()
    private var sortOrder: WearableItemsSortOrder = WearableItemsSortOrder.BY_NAME
    private var filterSubString: String = ""
    private var noFilteredItemsMsg: String = ""
    private var noItemsCommentText: String = ""
    var doubleClickCallback: (() -> Unit)? = null

    fun setNoFilteredItemsMsg(msg: String) { noFilteredItemsMsg = msg }
    fun setNoItemsCommentText(text: String) { noItemsCommentText = text }

    fun setSortOrder(order: WearableItemsSortOrder) {
        sortOrder = order
        // Platform: mItemsList->setSortOrder(order.code)
    }

    fun getSortOrder(): WearableItemsSortOrder = sortOrder

    fun setFilterSubString(filter: String, forceUpdate: Boolean) {
        // Platform: LLWearableItemsList::setFilterSubString(filter, forceUpdate)
    }

    fun updateList(folderId: UUID) {
        // Platform: LLWearableItemsList::updateList(folderId)
    }

    fun numSelected(): Int {
        // Platform: LLWearableItemsList::numSelected()
        return 0
    }

    fun getSelectedUUID(): UUID {
        // Platform: LLWearableItemsList::getSelectedUUID()
        return UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    fun getSelectedUUIDs(out: MutableList<UUID>) {
        // Platform: LLWearableItemsList::getSelectedUUIDs(out)
    }

    fun setDoubleClickCallback(cb: () -> Unit) { doubleClickCallback = cb }

    fun setDADCallback(cb: (UUID) -> Unit): Any? {
        dadCallbacks.add(cb)
        return cb
    }

    fun rearrange() {
        // Platform: LLWearableItemsList::rearrangeItems()
    }

    open fun handleDragAndDrop(
        x: Int, y: Int, mask: Int,
        drop: Boolean,
        cargoType: WFDragAndDropType,
        cargoData: Any?,
    ): Boolean {
        if (cargoType == WFDragAndDropType.BODYPART ||
            cargoType == WFDragAndDropType.CLOTHING  ||
            cargoType == WFDragAndDropType.OBJECT
        ) {
            if (drop && cargoData != null) {
                val itemId = getUuidFromCargoData(cargoData)
                dadCallbacks.forEach { it(itemId) }
            }
        }
        // Platform: autoScroll(x, y); return ACCEPT_YES_SINGLE when not dropping
        return true
    }

    private fun getUuidFromCargoData(cargoData: Any): UUID {
        // Platform: (cargoData as LLInventoryItem).getUUID()
        return UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
}

// =============================================================================
// FSFloaterWearableFavorites
// =============================================================================

/**
 * Floater that shows the user's "#Wearable Favorites" inventory folder as a
 * filterable, sortable list with drag-and-drop support.
 *
 * Mirrors `FSFloaterWearableFavorites` from `fsfloaterwearablefavorites.h/.cpp`.
 *
 * @param key LLSD construction key from the floater registry.
 */
class FSFloaterWearableFavorites(val key: Any) {

    private var itemsList: FSWearableFavoritesItemsList? = null
    private var removeItemBtn: Any? = null
    private var filterEditor: Any? = null
    private var optionsButton: Any? = null
    private var optionsMenuHandle: Any? = null
    private var categoriesObserver: Any? = null
    private var dadCallbackHandle: ((UUID) -> Unit)? = null
    private var initialized: Boolean = false

    companion object {
        var sFolderID: UUID = NULL_UUID
        private val WEARABLE_FAVORITES_FOLDER = "#Wearable Favorites"
        private val NULL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        fun initCategory(callback: (UUID) -> Unit = {}) {
            if (!InventoryModel.isInventoryUsable()) return

            val existingId = getWearableFavoritesFolderID()
            if (existingId != null) {
                sFolderID = existingId
                callback(sFolderID)
                return
            }

            var fsRootCatId = InventoryModel.findCategoryByName(ROOT_FIRESTORM_FOLDER)
            if (fsRootCatId == NULL_UUID) {
                InventoryModel.createNewCategory(
                    InventoryModel.rootFolderID, FolderType.NONE, ROOT_FIRESTORM_FOLDER
                ) { newCatId ->
                    InventoryModel.createNewCategory(
                        newCatId, FolderType.NONE, WEARABLE_FAVORITES_FOLDER
                    ) { innerCatId ->
                        sFolderID = innerCatId
                        callback(innerCatId)
                    }
                }
            } else {
                InventoryModel.createNewCategory(
                    fsRootCatId, FolderType.NONE, WEARABLE_FAVORITES_FOLDER
                ) { newCatId ->
                    sFolderID = newCatId
                    callback(newCatId)
                }
            }
        }

        fun getFavoritesFolder(): UUID {
            if (sFolderID != NULL_UUID) {
                getWearableFavoritesFolderID()?.let { sFolderID = it }
            }
            return sFolderID
        }

        private fun getWearableFavoritesFolderID(): UUID? {
            val fsRootCatId = InventoryModel.findCategoryByName(ROOT_FIRESTORM_FOLDER)
            if (fsRootCatId == NULL_UUID) return null
            return InventoryModel.findDirectDescendantCategoryByName(
                fsRootCatId, WEARABLE_FAVORITES_FOLDER
            )
        }

        private const val ROOT_FIRESTORM_FOLDER = "#Firestorm"
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    fun postBuild(): Boolean {
        itemsList = getChild("favorites_list")
        itemsList?.setNoFilteredItemsMsg(getString("search_no_items"))
        itemsList?.setDoubleClickCallback { onDoubleClick() }

        removeItemBtn = getChild("remove_btn")
        setChildAction("remove_btn") { handleRemove() }

        setFilterEditorCallback("wearable_filter_input") { search -> onFilterEdit(search) }

        registerMenuCallback("FavWearables.Action") { data -> onOptionsMenuItemClicked(data) }
        registerMenuEnableCallback("FavWearables.CheckAction") { data -> onOptionsMenuItemChecked(data) }

        optionsButton = getChild("options_btn")
        // Platform: load menu_fs_wearable_favorites.xml; attach to optionsButton as MP_BOTTOM_LEFT

        return true
    }

    fun onOpen(info: Any?) {
        if (!initialized) {
            if (sFolderID == NULL_UUID) {
                initCategory { initialize() }
            } else {
                initialize()
            }
        }
    }

    private fun initialize() {
        val category = InventoryModel.getCategory(sFolderID) ?: return
        val cofId = InventoryModel.findCategoryForCurrentOutfit()
        InventoryModel.getCategory(cofId) ?: return

        InventoryModel.addObserver(categoriesObserver)
        InventoryModel.observeCategory(sFolderID) { updateList(sFolderID) }
        InventoryModel.observeCategory(cofId) { updateList(sFolderID) }
        InventoryModel.fetchCategory(sFolderID)

        val savedSortCode = WISavedSettings.getUInt("FSWearableFavoritesSortOrder")
        itemsList?.setSortOrder(WearableItemsSortOrder.fromCode(savedSortCode))
        updateList(sFolderID)

        dadCallbackHandle = { itemId -> onItemDAD(itemId) }
        itemsList?.setDADCallback(dadCallbackHandle!!)

        initialized = true
    }

    fun draw() {
        // Platform: LLFloater::draw()
        val numSelected = itemsList?.numSelected() ?: 0
        setChildEnabled("remove_btn", numSelected > 0)
    }

    fun handleKeyHere(key: Char, mask: Int): Boolean {
        if (FSCommon.isFilterEditorKeyCombo(key, mask)) {
            setChildFocus("wearable_filter_input", true)
            return true
        }
        // Platform: return LLFloater::handleKeyHere(key, mask)
        return false
    }

    fun hasAccelerators(): Boolean = true

    // -------------------------------------------------------------------------
    // List management
    // -------------------------------------------------------------------------

    private fun updateList(folderId: UUID) {
        itemsList?.updateList(folderId)
        if (InventoryModel.isCategoryComplete(folderId)) {
            // Must reset after LLWearableItemsList::updateList which may override the comment.
            itemsList?.setNoItemsCommentText(getString("empty_list"))
        }
    }

    // -------------------------------------------------------------------------
    // Item actions
    // -------------------------------------------------------------------------

    private fun onItemDAD(itemId: UUID) {
        InventoryModel.linkInventoryObject(sFolderID, itemId)
    }

    private fun handleRemove() {
        val selected = mutableListOf<UUID>()
        itemsList?.getSelectedUUIDs(selected)
        for (id in selected) {
            InventoryModel.removeInventoryItem(id)
        }
    }

    private fun onDoubleClick() {
        val selectedId = itemsList?.getSelectedUUID() ?: return
        if (selectedId == NULL_UUID) return

        val ids = listOf(selectedId)
        val item = InventoryModel.getItem(selectedId) ?: return
        val assetType = InventoryModel.getAssetType(item)

        if (InventoryModel.isItemWorn(selectedId)) {
            if (assetType == InventoryAssetType.CLOTHING && RlvActions.canRemove(item)) {
                AppearanceMgr.removeItemsFromAvatar(ids)
            } else if (assetType == InventoryAssetType.OBJECT && RlvActions.canDetach(item)) {
                AppearanceMgr.removeItemsFromAvatar(ids)
            }
        } else {
            if (assetType == InventoryAssetType.BODYPART && RlvActions.canWearReplace(item)) {
                AppearanceMgr.wearMultiple(ids, replace = true)
            } else if (assetType == InventoryAssetType.CLOTHING &&
                AppearanceMgr.canAddWearables(ids) && RlvActions.canWearAdd(item)
            ) {
                AppearanceMgr.wearMultiple(ids, replace = false)
            } else if (assetType == InventoryAssetType.OBJECT &&
                AppearanceMgr.canAddWearables(ids) && RlvActions.canAttachAdd(item)
            ) {
                AppearanceMgr.wearMultiple(ids, replace = false)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Filter
    // -------------------------------------------------------------------------

    private fun onFilterEdit(searchString: String) {
        itemsList?.setFilterSubString(searchString, true)
        itemsList?.setNoItemsCommentText(getString("empty_list"))
        itemsList?.rearrange()
    }

    // -------------------------------------------------------------------------
    // Options menu
    // -------------------------------------------------------------------------

    private fun onOptionsMenuItemClicked(userdata: String) {
        when (userdata) {
            "sort_by_name" -> {
                itemsList?.setSortOrder(WearableItemsSortOrder.BY_NAME)
                WISavedSettings.setUInt("FSWearableFavoritesSortOrder", WearableItemsSortOrder.BY_NAME.code)
            }
            "sort_by_most_recent" -> {
                itemsList?.setSortOrder(WearableItemsSortOrder.BY_MOST_RECENT)
                WISavedSettings.setUInt("FSWearableFavoritesSortOrder", WearableItemsSortOrder.BY_MOST_RECENT.code)
            }
            "sort_by_type_name" -> {
                itemsList?.setSortOrder(WearableItemsSortOrder.BY_TYPE_NAME)
                WISavedSettings.setUInt("FSWearableFavoritesSortOrder", WearableItemsSortOrder.BY_TYPE_NAME.code)
            }
        }
    }

    private fun onOptionsMenuItemChecked(userdata: String): Boolean = when (userdata) {
        "sort_by_name"        -> itemsList?.getSortOrder() == WearableItemsSortOrder.BY_NAME
        "sort_by_most_recent" -> itemsList?.getSortOrder() == WearableItemsSortOrder.BY_MOST_RECENT
        "sort_by_type_name"   -> itemsList?.getSortOrder() == WearableItemsSortOrder.BY_TYPE_NAME
        else                  -> false
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    private fun getString(key: String): String {
        // Platform: getString("$key") from floater XUI strings
        return ""
    }

    private fun <T> getChild(name: String): T? {
        // Platform: getChild<T>("$name")
        return null
    }

    private fun setChildAction(name: String, action: () -> Unit) {
        // Platform: childSetAction("$name", action)
    }

    private fun setChildEnabled(name: String, enabled: Boolean) {
        // Platform: childSetEnabled("$name", $enabled)
    }

    private fun setChildFocus(name: String, focused: Boolean) {
        // Platform: getChild<LLUICtrl>("$name").setFocus($focused)
    }

    private fun setFilterEditorCallback(name: String, cb: (String) -> Unit) {
        // Platform: getChild<LLFilterEditor>("$name").setCommitCallback(cb)
    }

    private fun registerMenuCallback(action: String, cb: (String) -> Unit) {
        // Platform: registrar.add("$action", cb)
    }

    private fun registerMenuEnableCallback(action: String, cb: (String) -> Boolean) {
        // Platform: enable_registrar.add("$action", cb)
    }
}

// =============================================================================
// Stub types local to this file (prefixed "WI" to avoid collision)
// =============================================================================

object WISavedSettings {
    fun getUInt(key: String): Int {
        // Platform: gSavedSettings.getU32("$key")
        return 0
    }
    fun setUInt(key: String, value: Int) {
        // Platform: gSavedSettings.setU32("$key", $value)
    }
}

enum class InventoryAssetType { BODYPART, CLOTHING, OBJECT }
enum class FolderType { NONE }

object InventoryModel {
    val rootFolderID: UUID get() {
        // Platform: gInventory.getRootFolderID()
        return UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
    fun isInventoryUsable(): Boolean {
        // Platform: gInventory.isInventoryUsable()
        return false
    }
    fun findCategoryByName(name: String): UUID {
        // Platform: gInventory.findCategoryByName("$name")
        return UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
    fun findDirectDescendantCategoryByName(parentId: UUID, name: String): UUID? {
        // Platform: iterate gInventory.getDirectDescendentsOf(parentId).cats for name
        return null
    }
    fun findCategoryForCurrentOutfit(): UUID {
        // Platform: gInventory.findCategoryUUIDForType(FT_CURRENT_OUTFIT)
        return UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
    fun getCategory(id: UUID): Any? {
        // Platform: gInventory.getCategory($id)
        return null
    }
    fun getItem(id: UUID): Any? {
        // Platform: gInventory.getItem($id)
        return null
    }
    fun getAssetType(item: Any): InventoryAssetType {
        // Platform: item->getType() mapped to InventoryAssetType
        return InventoryAssetType.OBJECT
    }
    fun isItemWorn(id: UUID): Boolean {
        // Platform: get_is_item_worn($id)
        return false
    }
    fun isCategoryComplete(id: UUID): Boolean {
        // Platform: gInventory.isCategoryComplete($id)
        return false
    }
    fun fetchCategory(id: UUID) {
        // Platform: category->fetch()
    }
    fun addObserver(observer: Any?) {
        // Platform: gInventory.addObserver(observer)
    }
    fun observeCategory(id: UUID, cb: () -> Unit) {
        // Platform: mCategoriesObserver->addCategory($id, cb)
    }
    fun createNewCategory(parentId: UUID, type: FolderType, name: String, cb: (UUID) -> Unit) {
        // Platform: gInventory.createNewCategory(parentId, FT_NONE, "$name", cb)
    }
    fun linkInventoryObject(destFolderId: UUID, itemId: UUID) {
        // Platform: link_inventory_object(destFolderId, itemId, nullptr)
    }
    fun removeInventoryItem(id: UUID) {
        // Platform: remove_inventory_item($id, nullptr)
    }
}

object AppearanceMgr {
    fun removeItemsFromAvatar(ids: List<UUID>) {
        // Platform: LLAppearanceMgr::instance().removeItemsFromAvatar(ids)
    }
    fun canAddWearables(ids: List<UUID>): Boolean {
        // Platform: LLAppearanceMgr::instance().canAddWearables(ids)
        return false
    }
    fun wearMultiple(ids: List<UUID>, replace: Boolean) {
        // Platform: wear_multiple(ids, $replace)
    }
}

object RlvActions {
    fun canRemove(item: Any): Boolean {
        // Platform: !RlvActions::isRlvEnabled() || gRlvWearableLocks.canRemove(item)
        return false
    }
    fun canDetach(item: Any): Boolean {
        // Platform: !RlvActions::isRlvEnabled() || gRlvAttachmentLocks.canDetach(item)
        return false
    }
    fun canWearReplace(item: Any): Boolean {
        // Platform: !RlvActions::isRlvEnabled() || (gRlvWearableLocks.canWear(item) & RLV_WEAR_REPLACE) == RLV_WEAR_REPLACE
        return false
    }
    fun canWearAdd(item: Any): Boolean {
        // Platform: !RlvActions::isRlvEnabled() || (gRlvWearableLocks.canWear(item) & RLV_WEAR_ADD) == RLV_WEAR_ADD
        return false
    }
    fun canAttachAdd(item: Any): Boolean {
        // Platform: !RlvActions::isRlvEnabled() || (gRlvAttachmentLocks.canAttach(item) & RLV_WEAR_ADD) == RLV_WEAR_ADD
        return false
    }
}
