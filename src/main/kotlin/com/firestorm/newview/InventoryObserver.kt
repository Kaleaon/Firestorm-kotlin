package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ── Base observer ─────────────────────────────────────────────────────────────

abstract class InventoryObserver {
    abstract fun changed(mask: UInt)
}

// ── Fetch observer base ───────────────────────────────────────────────────────

abstract class InventoryFetchObserver(ids: List<LLUUID> = emptyList()) : InventoryObserver() {
    protected val fetchIds:    MutableList<LLUUID> = ids.toMutableList()
    protected val complete:    MutableList<LLUUID> = mutableListOf()
    protected val incomplete:  MutableList<LLUUID> = mutableListOf()

    constructor(id: LLUUID) : this(if (id != LLUUID.NULL) listOf(id) else emptyList())

    fun setFetchID(id: LLUUID)         { fetchIds.clear(); fetchIds.add(id) }
    fun setFetchIDs(ids: List<LLUUID>) { fetchIds.clear(); fetchIds.addAll(ids) }

    fun isFinished(): Boolean = incomplete.isEmpty()

    abstract fun startFetch()
    override abstract fun changed(mask: UInt)
    open fun done() {}
}

// ── Fetch items observer ──────────────────────────────────────────────────────

open class InventoryFetchItemsObserver(ids: List<LLUUID> = emptyList()) : InventoryFetchObserver(ids) {

    constructor(itemId: LLUUID) : this(if (itemId != LLUUID.NULL) listOf(itemId) else emptyList())

    private var fetchPeriodExpiry: Long = 0L

    override fun startFetch() {
        val aisAvailable = false // AIS (Asset Inventory Service) not yet ported to JVM
        resetFetchTimer()

        val requestsByFolder = mutableMapOf<LLUUID, MutableList<LLUUID>>()

        for (id in fetchIds) {
            val item = InventoryModel.getItem(id)
            if (item != null && item.isFinished()) {
                complete.add(id)
                continue
            }
            if (InventoryModel.getCategory(id) != null) continue
            if (id == LLUUID.NULL) continue

            incomplete.add(id)

            if (aisAvailable && item != null) {
                requestsByFolder.getOrPut(item.parentId) { mutableListOf() }.add(id)
            } else if (aisAvailable) {
                System.err.println("InventoryFetchItemsObserver: scheduleItemFetch($id) via AIS not yet ported")
            } else {
                System.err.println("InventoryFetchItemsObserver: FetchInventory2 for $id not yet implemented")
            }
        }

        if (aisAvailable) {
            for ((folderId, items) in requestsByFolder) {
                val cat = InventoryModel.getCategory(folderId)
                when {
                    cat == null -> items.forEach {
                        System.err.println("InventoryFetchItemsObserver: scheduleItemFetch($it) not yet ported")
                    }
                    cat.version == ViewerInventoryCategory.VERSION_UNKNOWN ->
                        cat.fetch()
                    items.size > MAX_INDIVIDUAL_ITEM_REQUESTS ->
                        System.err.println("InventoryFetchItemsObserver: scheduleFolderFetch($folderId) not yet ported")
                    else -> items.forEach {
                        System.err.println("InventoryFetchItemsObserver: scheduleItemFetch($it) not yet ported")
                    }
                }
            }
        }
    }

    override fun changed(mask: UInt) {
        if (incomplete.isEmpty()) return

        val isFetchInProgress = false // BackgroundFetch.isEverythingFetched not yet ported
        if (isFetchInProgress) resetFetchTimer()

        val timedOut = isFetchTimerExpired()

        val iter = incomplete.iterator()
        while (iter.hasNext()) {
            val id   = iter.next()
            val item = InventoryModel.getItem(id)
            if (item != null && item.isFinished()) {
                complete.add(id)
                iter.remove()
            } else if (timedOut) {
                iter.remove()
            }
        }

        if (incomplete.isEmpty()) done()
    }

    private fun resetFetchTimer() {
        fetchPeriodExpiry = System.currentTimeMillis() + FETCH_TIMER_EXPIRY_MS
    }

    private fun isFetchTimerExpired(): Boolean =
        System.currentTimeMillis() > fetchPeriodExpiry

    companion object {
        const val MAX_INDIVIDUAL_ITEM_REQUESTS = 7
        private const val FETCH_TIMER_EXPIRY_MS = 60_000L
    }
}

// ── Fetch descendents observer ────────────────────────────────────────────────

open class InventoryFetchDescendentsObserver(ids: List<LLUUID> = emptyList()) : InventoryFetchObserver(ids) {

    constructor(catId: LLUUID) : this(if (catId != LLUUID.NULL) listOf(catId) else emptyList())

    override fun startFetch() {
        for (id in fetchIds) {
            val cat = InventoryModel.getCategory(id) ?: continue
            if (!isCategoryComplete(cat)) {
                System.err.println("InventoryFetchDescendentsObserver: scheduleFolderFetch($id) not yet ported")
                incomplete.add(id)
            } else {
                complete.add(id)
            }
        }
    }

    override fun changed(mask: UInt) {
        val iter = incomplete.iterator()
        while (iter.hasNext()) {
            val id  = iter.next()
            val cat = InventoryModel.getCategory(id)
            if (cat == null) {
                iter.remove()
                continue
            }
            if (isCategoryComplete(cat)) {
                complete.add(id)
                iter.remove()
            }
        }

        if (incomplete.isEmpty()) {
            done()
        } else {
            val allFetched = false // BackgroundFetch completion state not yet ported
            if (allFetched) done()
        }
    }

    protected fun isCategoryComplete(cat: ViewerInventoryCategory): Boolean {
        if (cat.version == ViewerInventoryCategory.VERSION_UNKNOWN) return false
        if (cat.descendentCount == ViewerInventoryCategory.DESCENDENT_COUNT_UNKNOWN) return false
        val (cats, items) = InventoryModel.getDirectDescendentsOf(cat.uuid)
        val known = cats.size + items.size
        if (known == cat.descendentCount) return true
        if (known >= cat.descendentCount) {
            cat.descendentCount = known
            return true
        }
        return false
    }
}

// ── Fetch combo observer ──────────────────────────────────────────────────────

abstract class InventoryFetchComboObserver(
    folderIds: List<LLUUID>,
    itemIds: List<LLUUID>
) : InventoryObserver() {

    private val fetchDescendents = InventoryFetchDescendentsObserver(folderIds)
    private val fetchItems: InventoryFetchItemsObserver

    init {
        val prunedItems = itemIds.filter { id ->
            val item = InventoryModel.getItem(id)
            item == null || item.parentId !in folderIds
        }
        fetchItems = InventoryFetchItemsObserver(prunedItems)
    }

    override fun changed(mask: UInt) {
        fetchItems.changed(mask)
        fetchDescendents.changed(mask)
        if (fetchItems.isFinished() && fetchDescendents.isFinished()) done()
    }

    fun startFetch() {
        fetchItems.startFetch()
        fetchDescendents.startFetch()
    }

    abstract fun done()
}

// ── Add item by asset observer ────────────────────────────────────────────────

abstract class InventoryAddItemByAssetObserver : InventoryObserver() {
    private val addedItems:    MutableList<LLUUID> = mutableListOf()
    private val watchedAssets: MutableList<LLUUID> = mutableListOf()
    private var isDirty: Boolean = false

    override fun changed(mask: UInt) {
        if (mask and ChangeType.ADD == 0u ||
            mask and ChangeType.CREATE == 0u ||
            mask and ChangeType.UPDATE_CREATE == 0u) return
        if (watchedAssets.isEmpty()) return

        for (id in InventoryModel.getAddedIDs()) {
            val item = InventoryModel.getItem(id) ?: continue
            val assetId = item.assetId
            if (item.uuid != LLUUID.NULL && assetId != LLUUID.NULL && isAssetWatched(assetId)) {
                addedItems.add(item.uuid)
            }
        }

        if (addedItems.size == watchedAssets.size) {
            done()
            addedItems.clear()
            isDirty = true
        }
    }

    fun watchAsset(assetId: LLUUID) {
        if (assetId == LLUUID.NULL) return
        if (isDirty) {
            watchedAssets.clear()
            isDirty = false
        }
        watchedAssets.add(assetId)
        onAssetAdded(assetId)
    }

    fun isAssetWatched(assetId: LLUUID): Boolean = assetId in watchedAssets

    protected open fun onAssetAdded(assetId: LLUUID) {}
    protected abstract fun done()
}

// ── Added observer (fires when any new item is created) ───────────────────────

abstract class InventoryAddedObserver : InventoryObserver() {
    override fun changed(mask: UInt) {
        if (mask and ChangeType.ADD == 0u ||
            mask and ChangeType.CREATE == 0u ||
            mask and ChangeType.UPDATE_CREATE == 0u) return
        if (InventoryModel.getAddedIDs().isNotEmpty()) done()
    }

    protected abstract fun done()
}

// ── Category added observer ───────────────────────────────────────────────────

abstract class InventoryCategoryAddedObserver : InventoryObserver() {
    protected val addedCategories: MutableList<ViewerInventoryCategory> = mutableListOf()

    override fun changed(mask: UInt) {
        if (mask and ChangeType.ADD == 0u) return
        for (id in InventoryModel.getAddedIDs()) {
            val cat = InventoryModel.getCategory(id)
            if (cat != null) addedCategories.add(cat)
        }
        if (addedCategories.isNotEmpty()) {
            done()
            addedCategories.clear()
        }
    }

    protected abstract fun done()
}

// ── Completion observer ───────────────────────────────────────────────────────

abstract class InventoryCompletionObserver : InventoryObserver() {
    protected val complete:   MutableList<LLUUID> = mutableListOf()
    protected val incomplete: MutableList<LLUUID> = mutableListOf()

    override fun changed(mask: UInt) {
        if (incomplete.isEmpty()) return
        val iter = incomplete.iterator()
        while (iter.hasNext()) {
            val id   = iter.next()
            val item = InventoryModel.getItem(id)
            if (item == null) { iter.remove(); continue }
            if (item.isFinished()) { complete.add(id); iter.remove() }
        }
        if (incomplete.isEmpty()) done()
    }

    fun watchItem(id: LLUUID) {
        if (id != LLUUID.NULL) incomplete.add(id)
    }

    protected abstract fun done()
}

// ── Categories observer (monitors a set of categories for changes) ────────────

open class InventoryCategoriesObserver : InventoryObserver() {
    data class CategoryData(
        val catId: LLUUID,
        val callback: () -> Unit,
        var version: Int,
        var descendentsCount: Int,
        var thumbnailId: LLUUID,
        var isFavorite: Boolean,
        var itemNameHash: LLUUID = LLUUID.NULL,
        var isNameHashInitialized: Boolean = false
    )

    private val categoryMap: MutableMap<LLUUID, CategoryData> = mutableMapOf()

    override fun changed(mask: UInt) {
        if (categoryMap.isEmpty()) return
        val toDelete = mutableListOf<LLUUID>()

        for ((catId, data) in categoryMap) {
            val category = InventoryModel.getCategory(catId)
            if (category == null) {
                data.callback()
                toDelete.add(catId)
                continue
            }

            if (category.version == ViewerInventoryCategory.VERSION_UNKNOWN ||
                category.descendentCount == ViewerInventoryCategory.DESCENDENT_COUNT_UNKNOWN) continue

            val (cats, items) = InventoryModel.getDirectDescendentsOf(catId)
            val currentDescendents = cats.size + items.size

            var catChanged = false

            if (category.version != data.version || currentDescendents != data.descendentsCount) {
                data.version         = category.version
                data.descendentsCount = currentDescendents
                catChanged = true
            }

            if (!data.isNameHashInitialized || mask and ChangeType.LABEL != 0u) {
                val newHash = InventoryModel.hashDirectDescendentNames(catId)
                if (data.itemNameHash != newHash) {
                    data.isNameHashInitialized = true
                    data.itemNameHash = newHash
                    catChanged = true
                }
            }

            val thumbnailId = category.thumbnailId
            if (data.thumbnailId != thumbnailId) {
                data.thumbnailId = thumbnailId
                catChanged = true
            }

            if (data.isFavorite != category.isFavorite) {
                data.isFavorite = category.isFavorite
                catChanged = true
            }

            if (catChanged) data.callback()
        }

        toDelete.forEach { removeCategory(it) }
    }

    fun addCategory(catId: LLUUID, cb: () -> Unit, initNameHash: Boolean = false): Boolean {
        var version   = ViewerInventoryCategory.VERSION_UNKNOWN
        var descCount = ViewerInventoryCategory.DESCENDENT_COUNT_UNKNOWN
        var thumbnail = LLUUID.NULL
        var favorite  = false

        val category = InventoryModel.getCategory(catId)
        if (category != null) {
            version   = category.version
            thumbnail = category.thumbnailId
            favorite  = category.isFavorite
            val (cats, items) = InventoryModel.getDirectDescendentsOf(catId)
            descCount = cats.size + items.size
        }

        val nameHash = if (initNameHash) InventoryModel.hashDirectDescendentNames(catId) else LLUUID.NULL

        categoryMap[catId] = CategoryData(
            catId = catId, callback = cb, version = version,
            descendentsCount = descCount, thumbnailId = thumbnail,
            isFavorite = favorite, itemNameHash = nameHash,
            isNameHashInitialized = initNameHash
        )
        return true
    }

    fun removeCategory(catId: LLUUID) { categoryMap.remove(catId) }
}

// ── Scroll-on-rename observer ─────────────────────────────────────────────────

class ScrollOnRenameObserver(
    private val uuid: LLUUID,
    private val onScroll: () -> Unit
) : InventoryObserver() {
    override fun changed(mask: UInt) {
        if (mask and ChangeType.LABEL == 0u) return
        if (uuid in InventoryModel.getChangedIDs()) {
            onScroll()
            InventoryModel.removeObserver(this)
        }
    }
}
