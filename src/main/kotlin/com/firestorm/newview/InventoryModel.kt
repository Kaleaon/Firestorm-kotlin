package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llinventory.FolderType
import com.firestorm.llinventory.InventoryCategory
import com.firestorm.llinventory.InventoryItem

interface InventoryObserver {
    fun changed(mask: UInt)
}

object ChangeType {
    const val NONE: UInt      = 0u
    const val LABEL: UInt     = 1u
    const val ADD: UInt       = 2u
    const val REMOVE: UInt    = 4u
    const val STRUCTURE: UInt = 8u
    const val MOVE: UInt      = 16u
    const val REBUILD: UInt   = 32u
    const val ALL: UInt       = 0xFFFFFFFFu
}

object InventoryModel {

    val items: MutableMap<LLUUID, InventoryItem>             = mutableMapOf()
    val categories: MutableMap<LLUUID, InventoryCategory>   = mutableMapOf()
    val parentChildItemMap: MutableMap<LLUUID, MutableList<LLUUID>> = mutableMapOf()
    val parentChildCatMap:  MutableMap<LLUUID, MutableList<LLUUID>> = mutableMapOf()

    var rootFolderID: LLUUID        = LLUUID.NULL
    var libraryRootFolderID: LLUUID = LLUUID.NULL

    private val observers: MutableList<InventoryObserver> = mutableListOf()

    // ── Root folder accessors ────────────────────────────────────────────────

    fun getRootFolderID(): LLUUID        = rootFolderID
    fun getLibraryRootFolderID(): LLUUID = libraryRootFolderID

    // ── Item CRUD ────────────────────────────────────────────────────────────

    fun getItem(id: LLUUID): InventoryItem? = items[id]

    fun getItemsByName(name: String): List<InventoryItem> =
        items.values.filter { it.name == name }

    fun getItemsInFolder(folderId: LLUUID): List<InventoryItem> =
        parentChildItemMap[folderId]?.mapNotNull { items[it] } ?: emptyList()

    fun addItem(item: InventoryItem) {
        val old = items[item.uuid]
        if (old != null && old.parentId != item.parentId) {
            parentChildItemMap[old.parentId]?.remove(old.uuid)
        }
        items[item.uuid] = item
        parentChildItemMap.getOrPut(item.parentId) { mutableListOf() }.let {
            if (!it.contains(item.uuid)) it.add(item.uuid)
        }
        notifyObservers(ChangeType.ADD)
    }

    fun removeItem(id: LLUUID) {
        val item = items.remove(id) ?: return
        parentChildItemMap[item.parentId]?.remove(id)
        notifyObservers(ChangeType.REMOVE)
    }

    fun moveItem(id: LLUUID, newParentId: LLUUID) {
        val item = items[id] ?: return
        parentChildItemMap[item.parentId]?.remove(id)
        item.parentId = newParentId
        parentChildItemMap.getOrPut(newParentId) { mutableListOf() }.let {
            if (!it.contains(id)) it.add(id)
        }
        notifyObservers(ChangeType.MOVE)
    }

    // ── Category CRUD ────────────────────────────────────────────────────────

    fun getCategory(id: LLUUID): InventoryCategory? = categories[id]

    fun getCategoriesByName(name: String): List<InventoryCategory> =
        categories.values.filter { it.name == name }

    fun getCategoriesInFolder(folderId: LLUUID): List<InventoryCategory> =
        parentChildCatMap[folderId]?.mapNotNull { categories[it] } ?: emptyList()

    fun addCategory(cat: InventoryCategory) {
        val old = categories[cat.uuid]
        if (old != null && old.parentId != cat.parentId) {
            parentChildCatMap[old.parentId]?.remove(old.uuid)
        }
        categories[cat.uuid] = cat
        parentChildCatMap.getOrPut(cat.parentId) { mutableListOf() }.let {
            if (!it.contains(cat.uuid)) it.add(cat.uuid)
        }
        notifyObservers(ChangeType.ADD or ChangeType.STRUCTURE)
    }

    fun removeCategory(id: LLUUID) {
        val cat = categories.remove(id) ?: return
        parentChildCatMap[cat.parentId]?.remove(id)
        notifyObservers(ChangeType.REMOVE or ChangeType.STRUCTURE)
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    fun findCategoryByType(type: FolderType): LLUUID? =
        categories.values.firstOrNull { it.preferredType == type.value }?.uuid

    fun isObjectDescendentOf(id: LLUUID, ancestorId: LLUUID): Boolean {
        var current = id
        val visited = mutableSetOf<LLUUID>()
        while (current != LLUUID.NULL && visited.add(current)) {
            val cat = categories[current] ?: items[current] ?: return false
            val parent = cat.parentId
            if (parent == ancestorId) return true
            current = parent
        }
        return false
    }

    // ── Observers ────────────────────────────────────────────────────────────

    fun addObserver(obs: InventoryObserver) {
        if (!observers.contains(obs)) observers.add(obs)
    }

    fun removeObserver(obs: InventoryObserver) {
        observers.remove(obs)
    }

    fun notifyObservers(mask: UInt = ChangeType.ALL) {
        observers.toList().forEach { it.changed(mask) }
    }
}
