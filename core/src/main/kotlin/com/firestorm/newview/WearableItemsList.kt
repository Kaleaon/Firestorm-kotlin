package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ── Asset / wearable type stubs ──────────────────────────────────────────────

enum class AssetType {
    AT_CLOTHING, AT_BODYPART, AT_OBJECT, AT_GESTURE, AT_UNKNOWN
}

enum class WearableType {
    WT_SHIRT, WT_PANTS, WT_SHOES, WT_SOCKS, WT_JACKET,
    WT_GLOVES, WT_UNDERSHIRT, WT_UNDERPANTS, WT_SKIRT,
    WT_ALPHA, WT_TATTOO, WT_UNIVERSAL, WT_PHYSICS,
    WT_SKIN, WT_HAIR, WT_EYES, WT_SHAPE,
    WT_NONE, WT_COUNT
}

enum class ItemState { IS_DEFAULT, IS_WORN, IS_MISMATCH }

// ── Minimal inventory item stub ───────────────────────────────────────────────

open class ViewerInventoryItem(
    val uuid: LLUUID = LLUUID.NULL,
    val linkedUuid: LLUUID = LLUUID.NULL,
    val type: AssetType = AssetType.AT_UNKNOWN,
    val wearableType: WearableType = WearableType.WT_NONE,
    val name: String = "",
    val description: String = "",
    val creationDate: Long = 0L,
    val isFavorite: Boolean = false
) {
    fun getLinkedUUID(): LLUUID = if (linkedUuid == LLUUID.NULL) uuid else linkedUuid
    fun isWearableType(): Boolean = wearableType != WearableType.WT_NONE
}

// ── Panel hierarchy stubs ─────────────────────────────────────────────────────

abstract class PanelInventoryListItemBase(val inventoryItem: ViewerInventoryItem?) {
    var hovered: Boolean = false
    protected val rightWidgets: MutableList<String> = mutableListOf()
    protected val leftWidgets: MutableList<String> = mutableListOf()
    protected val hiddenWidgets: MutableSet<String> = mutableSetOf()

    open fun getItemName(): String = inventoryItem?.name ?: ""
    open fun getDescription(): String = inventoryItem?.description ?: ""
    fun getType(): AssetType = inventoryItem?.type ?: AssetType.AT_UNKNOWN
    fun getWearableType(): WearableType = inventoryItem?.wearableType ?: WearableType.WT_NONE
    fun getCreationDate(): Long = inventoryItem?.creationDate ?: 0L
    fun getItem(): ViewerInventoryItem? = inventoryItem

    open fun updateItem(name: String, favorite: Boolean, itemState: ItemState = ItemState.IS_DEFAULT) {}

    fun setWidgetsVisible(visible: Boolean) {
        if (!visible) hiddenWidgets.addAll(rightWidgets + leftWidgets)
        else hiddenWidgets.clear()
    }

    fun setShowWidget(widgetName: String, show: Boolean) {
        if (show) hiddenWidgets.remove(widgetName) else hiddenWidgets.add(widgetName)
    }

    fun setShowWidget(button: Any?, show: Boolean) {}

    fun addWidgetToRightSide(name: String) { rightWidgets.add(name) }
    fun addWidgetToLeftSide(name: String) { leftWidgets.add(name) }
    fun setLeftWidgetsWidth(width: Int) {}
    fun setRightWidgetsWidth(width: Int) {}
    fun reshapeWidgets() {}
    fun setSeparatorVisible(visible: Boolean) {}
    fun setNeedsRefresh(needs: Boolean) {}
    open fun postBuild(): Boolean = true
    open fun onMouseEnter(x: Int, y: Int, mask: Int) { hovered = true; setWidgetsVisible(true); reshapeWidgets() }
    open fun onMouseLeave(x: Int, y: Int, mask: Int) { hovered = false; setWidgetsVisible(false); reshapeWidgets() }
}

// ── PanelWearableListItem ─────────────────────────────────────────────────────

open class PanelWearableListItem(item: ViewerInventoryItem?) : PanelInventoryListItemBase(item) {
    override fun onMouseEnter(x: Int, y: Int, mask: Int) { super.onMouseEnter(x, y, mask) }
    override fun onMouseLeave(x: Int, y: Int, mask: Int) { super.onMouseLeave(x, y, mask) }
}

// ── PanelWearableOutfitItem ───────────────────────────────────────────────────

open class PanelWearableOutfitItem(
    item: ViewerInventoryItem?,
    protected val wornIndicationEnabled: Boolean,
    protected val showWidgets: Boolean
) : PanelWearableListItem(item) {

    protected val inventoryItemUUID: LLUUID = item?.uuid ?: LLUUID.NULL

    companion object {
        fun create(item: ViewerInventoryItem?, wornIndicationEnabled: Boolean, showWidgets: Boolean): PanelWearableOutfitItem? {
            if (item == null) return null
            val panel = PanelWearableOutfitItem(item, wornIndicationEnabled, showWidgets)
            panel.postBuild()
            return panel
        }
    }

    override fun postBuild(): Boolean {
        if (showWidgets) {
            addWidgetToRightSide("add_wearable")
            addWidgetToRightSide("remove_wearable")
            setWidgetsVisible(false)
            reshapeWidgets()
        }
        return true
    }

    fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        if (!showWidgets) return false
        val isWorn = AppearanceMgr.isLinkedInCOF(inventoryItemUUID)
        if (isWorn) onRemoveWearable() else onAddWearable()
        return true
    }

    fun onAddWearable() {
        setWidgetsVisible(false)
        reshapeWidgets()
        AppearanceMgr.wearItemOnAvatar(inventoryItemUUID, true, false)
    }

    fun onRemoveWearable() {
        setWidgetsVisible(false)
        reshapeWidgets()
        AppearanceMgr.removeItemFromAvatar(inventoryItemUUID)
    }

    override fun updateItem(name: String, favorite: Boolean, itemState: ItemState) {
        var searchLabel = name
        var state = itemState
        val isWorn = AppearanceMgr.isLinkedInCOF(inventoryItemUUID)

        if (wornIndicationEnabled) {
            if (getType() == AssetType.AT_OBJECT && AppearanceMgr.isItemWorn(inventoryItemUUID)) {
                val attachPoint = AppearanceMgr.getAttachedPointName(inventoryItemUUID)
                searchLabel += if (attachPoint != null) " (worn on $attachPoint)" else " (worn)"
                state = if (isWorn) ItemState.IS_WORN else ItemState.IS_MISMATCH
            } else if (getType() != AssetType.AT_OBJECT && isWorn) {
                searchLabel += " (worn)"
                state = ItemState.IS_WORN
            }
        }

        if (showWidgets) {
            setShowWidget("add_wearable", !isWorn)
            val isBodyPart = inventoryItem?.type == AssetType.AT_BODYPART
            setShowWidget("remove_wearable", isWorn && !isBodyPart)
            if (hovered) { setWidgetsVisible(true); reshapeWidgets() }
        }

        super.updateItem(searchLabel, favorite, state)
    }
}

// ── PanelDeletableWearableListItem ────────────────────────────────────────────

open class PanelDeletableWearableListItem(item: ViewerInventoryItem?) : PanelWearableListItem(item) {

    companion object {
        fun create(item: ViewerInventoryItem?): PanelDeletableWearableListItem? {
            if (item == null) return null
            val panel = PanelDeletableWearableListItem(item)
            panel.postBuild()
            return panel
        }
    }

    override fun postBuild(): Boolean {
        super.postBuild()
        addWidgetToLeftSide("btn_delete")
        setWidgetsVisible(false)
        reshapeWidgets()
        return true
    }

    fun setShowDeleteButton(show: Boolean) = setShowWidget("btn_delete", show)
}

// ── PanelAttachmentListItem ───────────────────────────────────────────────────

class PanelAttachmentListItem(item: ViewerInventoryItem?) : PanelDeletableWearableListItem(item) {

    companion object {
        fun create(item: ViewerInventoryItem?): PanelAttachmentListItem? {
            if (item == null) return null
            val panel = PanelAttachmentListItem(item)
            panel.postBuild()
            return panel
        }
    }

    override fun updateItem(name: String, favorite: Boolean, itemState: ItemState) {
        var title = name
        val inv = inventoryItem
        if (inv != null && AppearanceMgr.isWearingAttachment(inv.getLinkedUUID())) {
            val jointName = AppearanceMgr.getAttachedPointName(inv.getLinkedUUID())
            if (jointName != null) title += " ($jointName)"
        }
        super.updateItem(title, favorite, itemState)
    }
}

// ── PanelClothingListItem ─────────────────────────────────────────────────────

class PanelClothingListItem(item: ViewerInventoryItem?) : PanelDeletableWearableListItem(item) {

    companion object {
        fun create(item: ViewerInventoryItem?): PanelClothingListItem? {
            if (item == null) return null
            val panel = PanelClothingListItem(item)
            panel.postBuild()
            return panel
        }
    }

    override fun postBuild(): Boolean {
        super.postBuild()
        addWidgetToRightSide("btn_move_up")
        addWidgetToRightSide("btn_move_down")
        addWidgetToRightSide("btn_lock")
        addWidgetToRightSide("btn_edit_panel")
        setWidgetsVisible(false)
        reshapeWidgets()
        return true
    }

    fun setShowMoveUpButton(show: Boolean) = setShowWidget("btn_move_up", show)
    fun setShowMoveDownButton(show: Boolean) = setShowWidget("btn_move_down", show)
    fun setShowLockButton(show: Boolean) = setShowWidget("btn_lock", show)
    fun setShowEditButton(show: Boolean) = setShowWidget("btn_edit_panel", show)
}

// ── PanelBodyPartsListItem ────────────────────────────────────────────────────

class PanelBodyPartsListItem(item: ViewerInventoryItem?) : PanelWearableListItem(item) {

    companion object {
        fun create(item: ViewerInventoryItem?): PanelBodyPartsListItem? {
            if (item == null) return null
            val panel = PanelBodyPartsListItem(item)
            panel.postBuild()
            return panel
        }
    }

    override fun postBuild(): Boolean {
        super.postBuild()
        addWidgetToRightSide("btn_lock")
        addWidgetToRightSide("btn_edit_panel")
        setWidgetsVisible(false)
        reshapeWidgets()
        return true
    }

    fun setShowLockButton(show: Boolean) = setShowWidget("btn_lock", show)
    fun setShowEditButton(show: Boolean) = setShowWidget("btn_edit_panel", show)
}

// ── FSPanelCOFWearableOutfitListItem  (Firestorm per-item complexity) ─────────

class FSPanelCOFWearableOutfitListItem(
    item: ViewerInventoryItem?,
    wornIndicationEnabled: Boolean,
    showWidgets: Boolean
) : PanelWearableOutfitItem(item, wornIndicationEnabled, showWidgets) {

    private var itemWeight: UInt = 0u

    companion object {
        fun create(
            item: ViewerInventoryItem?,
            wornIndicationEnabled: Boolean,
            showWidgets: Boolean,
            weight: UInt
        ): FSPanelCOFWearableOutfitListItem? {
            if (item == null) return null
            val panel = FSPanelCOFWearableOutfitListItem(item, wornIndicationEnabled, showWidgets)
            panel.postBuild()
            panel.updateItemWeight(weight)
            return panel
        }
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        addWidgetToRightSide("item_weight")
        return true
    }

    fun updateItemWeight(weight: UInt) {
        itemWeight = weight
    }

    override fun updateItem(name: String, favorite: Boolean, itemState: ItemState) {
        super.updateItem(name, favorite, itemState)
        setShowWidget("item_weight", true)
        reshapeWidgets()
    }

    override fun onMouseLeave(x: Int, y: Int, mask: Int) {
        super.onMouseLeave(x, y, mask)
        setShowWidget("item_weight", true)
        reshapeWidgets()
    }

    fun getWearableType(): WearableType = inventoryItem?.wearableType ?: WearableType.WT_NONE
}

// ── PanelDummyClothingListItem ────────────────────────────────────────────────

class PanelDummyClothingListItem(val wearableType: WearableType) : PanelWearableListItem(null) {

    companion object {
        private val clothingStrings: Map<WearableType, String> = mapOf(
            WearableType.WT_SHIRT       to "shirt_not_worn",
            WearableType.WT_PANTS       to "pants_not_worn",
            WearableType.WT_SHOES       to "shoes_not_worn",
            WearableType.WT_SOCKS       to "socks_not_worn",
            WearableType.WT_JACKET      to "jacket_not_worn",
            WearableType.WT_GLOVES      to "gloves_not_worn",
            WearableType.WT_UNDERSHIRT  to "undershirt_not_worn",
            WearableType.WT_UNDERPANTS  to "underpants_not_worn",
            WearableType.WT_SKIRT       to "skirt_not_worn",
            WearableType.WT_ALPHA       to "alpha_not_worn",
            WearableType.WT_TATTOO      to "tattoo_not_worn",
            WearableType.WT_UNIVERSAL   to "universal_not_worn",
            WearableType.WT_PHYSICS     to "physics_not_worn"
        )

        fun create(wType: WearableType): PanelDummyClothingListItem {
            val panel = PanelDummyClothingListItem(wType)
            panel.postBuild()
            return panel
        }

        fun wearableTypeToString(wType: WearableType): String =
            clothingStrings[wType] ?: "invalid_not_worn"
    }

    override fun postBuild(): Boolean {
        addWidgetToRightSide("btn_add_panel")
        updateItem(wearableTypeToString(wearableType), false)
        setWidgetsVisible(false)
        reshapeWidgets()
        return true
    }
}

// ── Comparators ───────────────────────────────────────────────────────────────

abstract class WearableListItemComparator : Comparator<PanelInventoryListItemBase> {
    final override fun compare(a: PanelInventoryListItemBase, b: PanelInventoryListItemBase): Int =
        if (doCompare(a, b)) -1 else if (doCompare(b, a)) 1 else 0

    protected abstract fun doCompare(a: PanelInventoryListItemBase, b: PanelInventoryListItemBase): Boolean
}

open class WearableItemNameComparator : WearableListItemComparator() {
    override fun doCompare(a: PanelInventoryListItemBase, b: PanelInventoryListItemBase): Boolean =
        a.getItemName().uppercase() < b.getItemName().uppercase()
}

class WearableItemTypeNameComparator : WearableItemNameComparator() {

    enum class TypeListOrder {
        ORDER_RANK_1, ORDER_RANK_2, ORDER_RANK_3, ORDER_RANK_4, ORDER_RANK_UNKNOWN
    }

    private data class WearableTypeOrder(
        val orderPriority: TypeListOrder,
        val sortAssetTypeByName: Boolean,
        val sortWearableTypeByName: Boolean
    )

    private val wearableOrder: MutableMap<AssetType, WearableTypeOrder> = mutableMapOf(
        AssetType.AT_CLOTHING to WearableTypeOrder(TypeListOrder.ORDER_RANK_1, false, false),
        AssetType.AT_OBJECT   to WearableTypeOrder(TypeListOrder.ORDER_RANK_2, true,  true),
        AssetType.AT_BODYPART to WearableTypeOrder(TypeListOrder.ORDER_RANK_3, false, true),
        AssetType.AT_GESTURE  to WearableTypeOrder(TypeListOrder.ORDER_RANK_4, true,  false)
    )

    fun setOrder(itemsOfType: AssetType, orderPriority: TypeListOrder, sortAssetByName: Boolean, sortWearableByName: Boolean) {
        wearableOrder[itemsOfType] = WearableTypeOrder(orderPriority, sortAssetByName, sortWearableByName)
    }

    override fun doCompare(a: PanelInventoryListItemBase, b: PanelInventoryListItemBase): Boolean {
        val typeA = a.getType()
        val typeB = b.getType()
        val orderA = getTypeListOrder(typeA)
        val orderB = getTypeListOrder(typeB)

        if (orderA != orderB) return orderA.ordinal < orderB.ordinal

        if (sortAssetTypeByName(typeA)) return super.doCompare(a, b)

        val wearA = a.getWearableType().ordinal
        val wearB = b.getWearableType().ordinal
        if (wearA != wearB) return wearA < wearB

        return if (sortWearableTypeByName(typeA)) super.doCompare(a, b)
        else a.getDescription() > b.getDescription()
    }

    private fun getTypeListOrder(type: AssetType): TypeListOrder =
        wearableOrder[type]?.orderPriority ?: TypeListOrder.ORDER_RANK_UNKNOWN

    private fun sortAssetTypeByName(type: AssetType): Boolean =
        wearableOrder[type]?.sortAssetTypeByName ?: true

    private fun sortWearableTypeByName(type: AssetType): Boolean =
        wearableOrder[type]?.sortWearableTypeByName ?: true
}

class WearableItemCreationDateComparator : WearableItemNameComparator() {
    override fun doCompare(a: PanelInventoryListItemBase, b: PanelInventoryListItemBase): Boolean {
        val d1 = a.getCreationDate()
        val d2 = b.getCreationDate()
        return if (d1 == d2) super.doCompare(a, b) else d1 > d2
    }
}

// ── WearableItemsList ─────────────────────────────────────────────────────────

class WearableItemsList(
    val isStandalone: Boolean = true,
    val wornIndicationEnabled: Boolean = true,
    val showItemWidgets: Boolean = false,
    val showCreateNew: Boolean = true,
    val showComplexity: Boolean = false
) {
    enum class SortOrder { E_SORT_BY_NAME, E_SORT_BY_MOST_RECENT, E_SORT_BY_TYPE_LAYER, E_SORT_BY_TYPE_NAME }

    var sortOrder: SortOrder = SortOrder.E_SORT_BY_TYPE_LAYER
        private set

    var menuWearableType: WearableType = WearableType.WT_NONE

    private val items: MutableList<PanelInventoryListItemBase> = mutableListOf()
    private val linkedItemsMap: MutableMap<LLUUID, LLUUID> = mutableMapOf()
    private val itemComplexityMap: MutableMap<LLUUID, UInt> = mutableMapOf()
    private var bodyPartsComplexity: UInt = 0u

    private val wearableTypeNameComparator = WearableItemTypeNameComparator()
    private val wearableTypeLayerComparator = WearableItemTypeNameComparator()
    private val wearableNameComparator = WearableItemNameComparator()
    private val wearableCreationDateComparator = WearableItemCreationDateComparator()

    private var attachmentsChangedCallback: ((LLUUID) -> Unit)? = null

    init {
        setSortOrder(SortOrder.E_SORT_BY_TYPE_LAYER, false)
        if (isStandalone) {
            attachmentsChangedCallback = { uuid -> updateChangedItem(uuid) }
        }
    }

    fun createNewItem(item: ViewerInventoryItem?): PanelInventoryListItemBase? {
        if (item == null) return null
        return if (!showComplexity) {
            PanelWearableOutfitItem.create(item, wornIndicationEnabled, showItemWidgets)
        } else {
            val linkedId = item.getLinkedUUID()
            linkedItemsMap[linkedId] = item.uuid
            val weight = if (item.wearableType == WearableType.WT_SKIN) bodyPartsComplexity
                         else itemComplexityMap[linkedId] ?: 0u
            FSPanelCOFWearableOutfitListItem.create(item, wornIndicationEnabled, showItemWidgets, weight)
        }
    }

    fun updateList(categoryId: LLUUID) {
        System.err.println("WearableItemsList: updateList not yet implemented")
    }

    fun updateChangedItems(changedUuids: List<LLUUID>) {
        if (changedUuids.isEmpty()) return
        items.forEach { panel ->
            val inv = (panel as? PanelWearableOutfitItem)?.inventoryItem ?: return@forEach
            if (inv.getLinkedUUID() in changedUuids) panel.setNeedsRefresh(true)
        }
    }

    fun updateChangedItem(uuid: LLUUID) = updateChangedItems(listOf(uuid))

    fun setSortOrder(order: SortOrder, sortNow: Boolean = true) {
        sortOrder = order
        if (sortNow) sort()
    }

    fun updateItemComplexity(itemComplexity: Map<LLUUID, UInt>, bodyPartsComplexity: UInt) {
        if (!showComplexity) return
        itemComplexityMap.clear()
        itemComplexityMap.putAll(itemComplexity)
        this.bodyPartsComplexity = bodyPartsComplexity
        updateComplexity()
    }

    private fun updateComplexity() {
        for ((linkedId, weight) in itemComplexityMap) {
            val itemId = linkedItemsMap[linkedId] ?: continue
            val panel = items.firstOrNull {
                (it as? FSPanelCOFWearableOutfitListItem)?.inventoryItem?.uuid == itemId
            } as? FSPanelCOFWearableOutfitListItem
            panel?.updateItemWeight(weight)
        }
        items.filterIsInstance<FSPanelCOFWearableOutfitListItem>()
            .firstOrNull { it.getWearableType() == WearableType.WT_SKIN }
            ?.updateItemWeight(bodyPartsComplexity)
    }

    private fun sort() {
        val comparator: Comparator<PanelInventoryListItemBase> = when (sortOrder) {
            SortOrder.E_SORT_BY_MOST_RECENT -> wearableCreationDateComparator
            SortOrder.E_SORT_BY_NAME        -> wearableNameComparator
            SortOrder.E_SORT_BY_TYPE_LAYER  -> wearableTypeLayerComparator
            SortOrder.E_SORT_BY_TYPE_NAME   -> {
                wearableTypeNameComparator.setOrder(
                    AssetType.AT_CLOTHING,
                    WearableItemTypeNameComparator.TypeListOrder.ORDER_RANK_1,
                    false, true
                )
                wearableTypeNameComparator
            }
        }
        items.sortWith(comparator)
    }

    fun onRightClick(x: Int, y: Int) {
        val selectedUuids = getSelectedUUIDs()
        if (selectedUuids.isEmpty()) {
            if (menuWearableType != WearableType.WT_NONE && items.isEmpty()) {
                ContextMenu.show(this, menuWearableType, x, y)
            }
        } else {
            ContextMenu.show(this, selectedUuids, x, y)
        }
    }

    private fun getSelectedUUIDs(): List<LLUUID> = emptyList()

    // ── Context menu ──────────────────────────────────────────────────────────

    object ContextMenu {
        private const val MASK_CLOTHING   = 0x01
        private const val MASK_BODYPART   = 0x02
        private const val MASK_ATTACHMENT = 0x04
        private const val MASK_GESTURE    = 0x08
        private const val MASK_UNKNOWN    = 0x10

        private var parent: WearableItemsList? = null

        fun show(spawningView: WearableItemsList, uuids: List<LLUUID>, x: Int, y: Int) {
            parent = spawningView
            System.err.println("WearableItemsList.ContextMenu: show(uuids) not yet implemented")
            parent = null
        }

        fun show(spawningView: WearableItemsList, wType: WearableType, x: Int, y: Int) {
            parent = spawningView
            System.err.println("WearableItemsList.ContextMenu: show(wType) not yet implemented")
            parent = null
        }

        fun createNewWearable(itemId: LLUUID) {
            System.err.println("WearableItemsList.ContextMenu: createNewWearable not yet implemented")
        }

        fun createNewWearableByType(type: WearableType) {
            System.err.println("WearableItemsList.ContextMenu: createNewWearableByType not yet implemented")
        }

        private fun updateMask(mask: Int, at: AssetType): Int = mask or when (at) {
            AssetType.AT_CLOTHING -> MASK_CLOTHING
            AssetType.AT_BODYPART -> MASK_BODYPART
            AssetType.AT_OBJECT   -> MASK_ATTACHMENT
            AssetType.AT_GESTURE  -> MASK_GESTURE
            else                  -> MASK_UNKNOWN
        }
    }
}

// ── FindOutfitItems ───────────────────────────────────────────────────────────

object FindOutfitItems {
    fun matches(item: ViewerInventoryItem?): Boolean {
        if (item == null) return false
        return item.type in setOf(
            AssetType.AT_CLOTHING, AssetType.AT_BODYPART,
            AssetType.AT_OBJECT, AssetType.AT_GESTURE
        )
    }
}

// ── AppearanceMgr stub (used by panel items above) ────────────────────────────

object AppearanceMgr {
    fun isLinkedInCOF(uuid: LLUUID): Boolean { System.err.println("AppearanceMgr: isLinkedInCOF not yet implemented"); return false }
    fun wearItemOnAvatar(uuid: LLUUID, replace: Boolean, immediately: Boolean): Unit { System.err.println("AppearanceMgr: wearItemOnAvatar not yet implemented") }
    fun removeItemFromAvatar(uuid: LLUUID): Unit { System.err.println("AppearanceMgr: removeItemFromAvatar not yet implemented") }
    fun removeItemsFromAvatar(uuids: List<LLUUID>): Unit { System.err.println("AppearanceMgr: removeItemsFromAvatar not yet implemented") }
    fun isItemWorn(uuid: LLUUID): Boolean { System.err.println("AppearanceMgr: isItemWorn not yet implemented"); return false }
    fun isWearingAttachment(uuid: LLUUID): Boolean { System.err.println("AppearanceMgr: isWearingAttachment not yet implemented"); return false }
    fun getAttachedPointName(uuid: LLUUID): String? { System.err.println("AppearanceMgr: getAttachedPointName not yet implemented"); return null }
    fun canAddWearables(uuids: List<LLUUID>): Boolean { System.err.println("AppearanceMgr: canAddWearables not yet implemented"); return false }
    fun setAttachmentsChangedCallback(cb: (LLUUID) -> Unit): Any { System.err.println("AppearanceMgr: setAttachmentsChangedCallback not yet implemented"); return Any() }
}
