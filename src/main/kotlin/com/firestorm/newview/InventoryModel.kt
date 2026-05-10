package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llinventory.FolderType
import com.firestorm.llinventory.InventoryCategory
import com.firestorm.llinventory.InventoryItem

object ChangeType {
    const val NONE: UInt         = 0u
    const val LABEL: UInt        = 1u
    const val INTERNAL: UInt     = 2u
    const val ADD: UInt          = 4u
    const val REMOVE: UInt       = 8u
    const val STRUCTURE: UInt    = 16u
    const val CALLING_CARD: UInt = 32u
    const val GESTURE: UInt      = 64u
    const val REBUILD: UInt      = 128u
    const val SORT: UInt         = 256u
    const val CREATE: UInt       = 512u
    const val UPDATE_CREATE: UInt   = 1024u
    const val UPDATE_FAVORITE: UInt = 2048u
    const val ALL: UInt          = 0xFFFFFFFFu
}

enum class HasChildren { NO, YES, MAYBE }

enum class AncestorResult { OK, MISSING, LOOP }

data class CategoryUpdate(
    val categoryId: LLUUID = LLUUID.NULL,
    val descendentDelta: Int = 0,
    val changeVersion: Boolean = true
)

object InventoryModel {

    val itemMap: MutableMap<LLUUID, ViewerInventoryItem>       = mutableMapOf()
    val categoryMap: MutableMap<LLUUID, ViewerInventoryCategory> = mutableMapOf()

    private val parentChildCategoryTree: MutableMap<LLUUID, MutableList<ViewerInventoryCategory>> = mutableMapOf()
    private val parentChildItemTree:     MutableMap<LLUUID, MutableList<ViewerInventoryItem>>     = mutableMapOf()

    private val backlinkMap: MutableMap<LLUUID, MutableSet<LLUUID>> = mutableMapOf()

    var rootFolderId: LLUUID        = LLUUID.NULL
    var libraryRootFolderId: LLUUID = LLUUID.NULL
    var libraryOwnerId: LLUUID      = LLUUID.NULL

    private var isAgentInvUsable: Boolean = false

    private val observers: MutableSet<InventoryObserver> = mutableSetOf()

    private var modifyMask: UInt = ChangeType.NONE
    val changedItemIds: MutableSet<LLUUID> = mutableSetOf()
    val addedItemIds:   MutableSet<LLUUID> = mutableSetOf()

    private var isNotifyingObservers: Boolean = false

    // ── Accessors ────────────────────────────────────────────────────────────

    fun isInventoryUsable(): Boolean = isAgentInvUsable

    fun getRootFolderID(): LLUUID        = rootFolderId
    fun getLibraryRootFolderID(): LLUUID = libraryRootFolderId
    fun getLibraryOwnerID(): LLUUID      = libraryOwnerId
    fun setRootFolderID(id: LLUUID)        { rootFolderId = id }
    fun setLibraryRootFolderID(id: LLUUID) { libraryRootFolderId = id }
    fun setLibraryOwnerID(id: LLUUID)      { libraryOwnerId = id }

    fun getItem(id: LLUUID): ViewerInventoryItem?       = itemMap[id]
    fun getCategory(id: LLUUID): ViewerInventoryCategory? = categoryMap[id]

    fun getItemCount(): Int     = itemMap.size
    fun getCategoryCount(): Int = categoryMap.size

    fun getObject(id: LLUUID): Any? = itemMap[id] ?: categoryMap[id]

    fun getLinkedItemID(objectId: LLUUID): LLUUID {
        val item = itemMap[objectId] ?: return objectId
        return item.assetId.takeIf { it != LLUUID.NULL } ?: objectId
    }

    fun getLinkedItem(objectId: LLUUID): ViewerInventoryItem? = getItem(getLinkedItemID(objectId))

    fun getDirectDescendentsOf(catId: LLUUID): Pair<List<ViewerInventoryCategory>, List<ViewerInventoryItem>> {
        val cats  = parentChildCategoryTree[catId] ?: emptyList()
        val items = parentChildItemTree[catId] ?: emptyList()
        return Pair(cats, items)
    }

    fun getItemsByName(name: String): List<ViewerInventoryItem> =
        itemMap.values.filter { it.name == name }

    fun getCategoriesByName(name: String): List<ViewerInventoryCategory> =
        categoryMap.values.filter { it.name == name }

    fun findCategoryByName(name: String): LLUUID? =
        categoryMap.values.firstOrNull { it.name == name }?.uuid

    fun findCategoryUUIDForType(type: FolderType): LLUUID? =
        findCategoryUUIDForTypeInRoot(type, rootFolderId)

    fun findLibraryCategoryUUIDForType(type: FolderType): LLUUID? =
        findCategoryUUIDForTypeInRoot(type, libraryRootFolderId)

    fun findCategoryUUIDForTypeInRoot(type: FolderType, rootId: LLUUID): LLUUID? =
        categoryMap.values.firstOrNull {
            it.preferredType == type.value && isObjectDescendentOf(it.uuid, rootId)
        }?.uuid

    fun getMarketplaceListingsUUID(): LLUUID =
        findCategoryUUIDForType(FolderType.MARKETPLACE_LISTINGS) ?: LLUUID.NULL

    fun isObjectDescendentOf(objId: LLUUID, catId: LLUUID): Boolean {
        var current = objId
        val visited = mutableSetOf<LLUUID>()
        while (current != LLUUID.NULL && visited.add(current)) {
            val item = itemMap[current]
            val cat  = categoryMap[current]
            val parent = item?.parentId ?: cat?.parentId ?: return false
            if (parent == catId) return true
            current = parent
        }
        return false
    }

    fun getObjectTopmostAncestor(objectId: LLUUID): Pair<AncestorResult, LLUUID> {
        var current = objectId
        val visited = mutableSetOf<LLUUID>()
        while (true) {
            if (!visited.add(current)) return Pair(AncestorResult.LOOP, current)
            val item = itemMap[current]
            val cat  = categoryMap[current]
            val parent = item?.parentId ?: cat?.parentId
                ?: return Pair(AncestorResult.MISSING, current)
            if (parent == LLUUID.NULL) return Pair(AncestorResult.OK, current)
            current = parent
        }
    }

    fun isCategoryComplete(catId: LLUUID): Boolean {
        val cat = categoryMap[catId] ?: return false
        if (cat.version == ViewerInventoryCategory.VERSION_UNKNOWN) return false
        if (cat.descendentCount == ViewerInventoryCategory.DESCENDENT_COUNT_UNKNOWN) return false
        val (cats, items) = getDirectDescendentsOf(catId)
        return (cats.size + items.size) >= cat.descendentCount
    }

    fun categoryHasChildren(catId: LLUUID): HasChildren {
        val cat = categoryMap[catId] ?: return HasChildren.NO
        return when {
            cat.descendentCount == ViewerInventoryCategory.DESCENDENT_COUNT_UNKNOWN -> HasChildren.MAYBE
            cat.descendentCount > 0 -> HasChildren.YES
            else -> {
                val (cats, items) = getDirectDescendentsOf(catId)
                if (cats.isNotEmpty() || items.isNotEmpty()) HasChildren.YES else HasChildren.NO
            }
        }
    }

    fun fetchDescendentsOf(folderId: LLUUID): Boolean {
        val cat = categoryMap[folderId] ?: return false
        return cat.fetch()
    }

    fun collectDescendents(
        id: LLUUID,
        includeTrash: Boolean,
        categories: MutableList<ViewerInventoryCategory> = mutableListOf(),
        items: MutableList<ViewerInventoryItem> = mutableListOf()
    ): Pair<List<ViewerInventoryCategory>, List<ViewerInventoryItem>> {
        val trashId = findCategoryUUIDForType(FolderType.TRASH)
        val queue = ArrayDeque<LLUUID>()
        queue.add(id)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!includeTrash && trashId != null && current == trashId) continue
            val (childCats, childItems) = getDirectDescendentsOf(current)
            categories.addAll(childCats)
            items.addAll(childItems)
            childCats.forEach { queue.add(it.uuid) }
        }
        return Pair(categories, items)
    }

    fun collectLinksTo(itemId: LLUUID): List<ViewerInventoryItem> =
        itemMap.values.filter { it.assetId == itemId }

    fun hashDirectDescendentNames(catId: LLUUID): LLUUID {
        val (_, items) = getDirectDescendentsOf(catId)
        val hash = items.map { it.name }.sorted().hashCode()
        return LLUUID.fromString(hash.toString().padStart(32, '0').take(32))
    }

    // ── Mutators ─────────────────────────────────────────────────────────────

    fun updateItem(item: ViewerInventoryItem, mask: UInt = 0u): UInt {
        val existing = itemMap[item.uuid]
        var changeMask = mask
        if (existing != null && existing.parentId != item.parentId) {
            parentChildItemTree[existing.parentId]?.remove(existing)
            changeMask = changeMask or ChangeType.STRUCTURE
        }
        itemMap[item.uuid] = item
        parentChildItemTree.getOrPut(item.parentId) { mutableListOf() }.let {
            if (!it.contains(item)) it.add(item)
        }
        return changeMask or ChangeType.ADD
    }

    fun updateCategory(cat: ViewerInventoryCategory, mask: UInt = 0u) {
        val existing = categoryMap[cat.uuid]
        if (existing != null && existing.parentId != cat.parentId) {
            parentChildCategoryTree[existing.parentId]?.remove(existing)
        }
        categoryMap[cat.uuid] = cat
        parentChildCategoryTree.getOrPut(cat.parentId) { mutableListOf() }.let {
            if (!it.contains(cat)) it.add(cat)
        }
    }

    fun moveObject(objectId: LLUUID, catId: LLUUID) {
        val item = itemMap[objectId]
        if (item != null) {
            parentChildItemTree[item.parentId]?.remove(item)
            item.parentId = catId
            parentChildItemTree.getOrPut(catId) { mutableListOf() }.add(item)
            return
        }
        val cat = categoryMap[objectId]
        if (cat != null) {
            parentChildCategoryTree[cat.parentId]?.remove(cat)
            cat.parentId = catId
            parentChildCategoryTree.getOrPut(catId) { mutableListOf() }.add(cat)
        }
    }

    fun changeItemParent(item: ViewerInventoryItem, newParentId: LLUUID, restamp: Boolean) {
        moveObject(item.uuid, newParentId)
        item.updateParentOnServer(restamp)
        addChangedMask(ChangeType.STRUCTURE, item.uuid)
    }

    fun changeCategoryParent(cat: ViewerInventoryCategory, newParentId: LLUUID, restamp: Boolean) {
        moveObject(cat.uuid, newParentId)
        cat.updateParentOnServer(restamp)
        addChangedMask(ChangeType.STRUCTURE, cat.uuid)
    }

    open fun deleteObject(id: LLUUID, fixBrokenLinks: Boolean = true, doNotifyObservers: Boolean = true) {
        val item = itemMap.remove(id)
        if (item != null) {
            parentChildItemTree[item.parentId]?.remove(item)
            if (doNotifyObservers) addChangedMask(ChangeType.REMOVE, id)
            return
        }
        val cat = categoryMap.remove(id)
        if (cat != null) {
            parentChildCategoryTree[cat.parentId]?.remove(cat)
            if (doNotifyObservers) addChangedMask(ChangeType.REMOVE or ChangeType.STRUCTURE, id)
        }
    }

    fun removeItem(itemId: LLUUID) {
        val trashId = findCategoryUUIDForType(FolderType.TRASH) ?: return
        moveObject(itemId, trashId)
        addChangedMask(ChangeType.STRUCTURE, itemId)
    }

    fun removeCategory(categoryId: LLUUID) {
        val trashId = findCategoryUUIDForType(FolderType.TRASH) ?: return
        moveObject(categoryId, trashId)
        addChangedMask(ChangeType.STRUCTURE, categoryId)
    }

    fun removeObject(objectId: LLUUID) {
        if (itemMap.containsKey(objectId)) removeItem(objectId) else removeCategory(objectId)
    }

    fun onObjectDeletedFromServer(
        itemId: LLUUID,
        fixBrokenLinks: Boolean = true,
        updateParentVersion: Boolean = true,
        doNotifyObservers: Boolean = true
    ) {
        deleteObject(itemId, fixBrokenLinks, doNotifyObservers)
    }

    fun onDescendentsPurgedFromServer(objectId: LLUUID, fixBrokenLinks: Boolean = true) {
        val (childCats, childItems) = getDirectDescendentsOf(objectId)
        childItems.forEach { deleteObject(it.uuid, fixBrokenLinks, false) }
        childCats.forEach { deleteObject(it.uuid, fixBrokenLinks, false) }
        addChangedMask(ChangeType.REMOVE or ChangeType.STRUCTURE, objectId)
    }

    fun onItemUpdated(itemId: LLUUID, updates: Map<String, Any>, updateParentVersion: Boolean) {
        val item = itemMap[itemId] ?: return
        updates["name"]?.let { item.name = it as String }
        addChangedMask(ChangeType.LABEL, itemId)
    }

    fun onCategoryUpdated(catId: LLUUID, updates: Map<String, Any>) {
        val cat = categoryMap[catId] ?: return
        updates["name"]?.let { cat.name = it as String }
        addChangedMask(ChangeType.LABEL, catId)
    }

    fun accountForUpdate(update: CategoryUpdate) {
        val cat = categoryMap[update.categoryId] ?: return
        cat.descendentCount += update.descendentDelta
        if (update.changeVersion && cat.version != ViewerInventoryCategory.VERSION_UNKNOWN) {
            cat.version++
        }
    }

    fun accountForUpdate(updates: List<CategoryUpdate>) {
        updates.forEach { accountForUpdate(it) }
    }

    fun rearrangeFavoriteLandmarks(sourceItemId: LLUUID, targetItemId: LLUUID) {
        val favId = findCategoryUUIDForType(FolderType.FAVORITES) ?: return
        val items = parentChildItemTree[favId] ?: return
        val srcItem = items.find { it.uuid == sourceItemId } ?: return
        val tgtIdx  = items.indexOfFirst { it.uuid == targetItemId }
        if (tgtIdx < 0) return
        items.remove(srcItem)
        items.add(tgtIdx, srcItem)
    }

    fun createNewCategory(
        parentId: LLUUID,
        preferredType: FolderType,
        name: String,
        callback: InventoryFunc? = null,
        thumbnailId: LLUUID = LLUUID.NULL
    ) {
        TODO("APR: use JVM equivalent - POST to CreateInventoryCategory cap or AIS, then call callback with new UUID")
    }

    // ── Category accounting ───────────────────────────────────────────────────

    fun buildParentChildMap() {
        parentChildCategoryTree.clear()
        parentChildItemTree.clear()
        categoryMap.values.forEach { cat ->
            parentChildCategoryTree.getOrPut(cat.parentId) { mutableListOf() }.add(cat)
        }
        itemMap.values.forEach { item ->
            parentChildItemTree.getOrPut(item.parentId) { mutableListOf() }.add(item)
        }
    }

    // ── Backlinks ─────────────────────────────────────────────────────────────

    fun addBacklinkInfo(linkId: LLUUID, targetId: LLUUID) {
        backlinkMap.getOrPut(targetId) { mutableSetOf() }.add(linkId)
    }

    fun removeBacklinkInfo(linkId: LLUUID, targetId: LLUUID) {
        backlinkMap[targetId]?.remove(linkId)
    }

    fun hasBacklinkInfo(linkId: LLUUID, targetId: LLUUID): Boolean =
        backlinkMap[targetId]?.contains(linkId) == true

    // ── Notifications ─────────────────────────────────────────────────────────

    fun addChangedMask(mask: UInt, referent: LLUUID) {
        modifyMask = modifyMask or mask
        if (referent != LLUUID.NULL) {
            changedItemIds.add(referent)
            if (mask and ChangeType.ADD != 0u) addedItemIds.add(referent)
        }
    }

    fun notifyObservers(transactionId: LLUUID = LLUUID.NULL) {
        if (isNotifyingObservers) return
        if (modifyMask == ChangeType.NONE) return
        isNotifyingObservers = true
        val mask = modifyMask
        modifyMask = ChangeType.NONE
        changedItemIds.clear()
        addedItemIds.clear()
        observers.toList().forEach { it.changed(mask) }
        isNotifyingObservers = false
    }

    fun idleNotifyObservers() {
        if (modifyMask != ChangeType.NONE) notifyObservers()
    }

    fun addObserver(observer: InventoryObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: InventoryObserver) {
        observers.remove(observer)
    }

    fun containsObserver(observer: InventoryObserver): Boolean =
        observers.contains(observer)

    // ── Persistence stubs ─────────────────────────────────────────────────────

    fun loadSkeleton(options: Map<String, Any>, ownerId: LLUUID): Boolean {
        TODO("APR: use JVM equivalent - parse inventory skeleton from login response")
    }

    fun cache(parentFolderId: LLUUID, agentId: LLUUID) {
        TODO("APR: use JVM equivalent - serialize inventory to local cache file")
    }

    fun ensureCategoryForTypeExists(preferredType: FolderType) {
        if (findCategoryUUIDForType(preferredType) == null) {
            createNewCategory(rootFolderId, preferredType, preferredType.name)
        }
    }

    companion object {
        fun getIsFirstTimeInViewer2(): Boolean = false
        fun isSysFoldersReady(): Boolean = true
    }
}

open class ViewerInventoryCategory(
    uuid: LLUUID = LLUUID.NULL,
    parentId: LLUUID = LLUUID.NULL,
    preferredType: Int = FolderType.NONE.value,
    name: String = "",
    ownerId: LLUUID = LLUUID.NULL
) : com.firestorm.newview.ViewerInventoryCategory(uuid, parentId, preferredType, name, ownerId) {
    fun updateParentOnServer(restamp: Boolean) {
        TODO("APR: use JVM equivalent - send MoveInventoryFolder to server")
    }

    fun updateServer(isNew: Boolean) {
        if (FolderType.fromValue(preferredType)?.isProtected == true) return
        TODO("APR: use JVM equivalent - send UpdateInventoryFolder or AIS UpdateCategory")
    }
}
