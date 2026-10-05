package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llinventory.FolderType
import java.util.ArrayDeque

object InventoryModelBackgroundFetch {

    enum class FetchType {
        DEFAULT,
        FORCED,
        CONTENT_RECURSIVE,
        FOLDER_AND_CONTENT,
        RECURSIVE
    }

    data class FetchQueueInfo(
        val uuid: LLUUID,
        val fetchType: FetchType,
        val isCategory: Boolean = true
    )

    private val fetchFolderQueue: ArrayDeque<FetchQueueInfo> = ArrayDeque()
    private val fetchItemQueue:   ArrayDeque<FetchQueueInfo> = ArrayDeque()
    private val forceFetchSet:    MutableSet<LLUUID>         = mutableSetOf()
    private val expectedFolderIds: MutableList<LLUUID>       = mutableListOf()

    private var backgroundFetchActive: Boolean = false
    var folderFetchActive: Boolean = false
        private set
    private var fetchCount:       Int     = 0
    private var lastFetchCount:   Int     = 0
    private var fetchFolderCount: Int     = 0

    private var recursiveInventoryFetchStarted:  Boolean = false
    private var recursiveLibraryFetchStarted:    Boolean = false
    private var recursiveMarketplaceFetchStarted: Boolean = false
    private var allRecursiveFoldersFetched:      Boolean = false

    private var minTimeBetweenFetchesMs: Long = 300L
    private var maxTimeBetweenFetchesMs: Long = 10_000L
    private var timelyFetchPending:      Boolean = false
    private var numFetchRetries:         Int     = 0
    private var lastFetchTimeMs:         Long    = 0L

    private val fetchCompletionCallbacks: MutableList<() -> Unit> = mutableListOf()

    // ── Public API ────────────────────────────────────────────────────────────

    fun start(catId: LLUUID = LLUUID.NULL, recursive: Boolean = true) {
        val cat = InventoryModel.getCategory(catId)

        if (cat != null || (catId == LLUUID.NULL && !isEverythingFetched())) {
            backgroundFetchActive = true
            folderFetchActive = true
            val fetchType = if (recursive) FetchType.RECURSIVE else FetchType.DEFAULT

            if (catId == LLUUID.NULL) {
                if (!recursiveInventoryFetchStarted) {
                    recursiveInventoryFetchStarted = recursiveInventoryFetchStarted || recursive
                    val rootId = InventoryModel.getRootFolderID()
                    if (recursive && isAISAvailable()) {
                        fetchFolderQueue.addFirst(FetchQueueInfo(rootId, FetchType.FOLDER_AND_CONTENT))
                    } else {
                        fetchFolderQueue.addLast(FetchQueueInfo(rootId, fetchType))
                    }
                    scheduleBackgroundIdle()
                }
                if (!recursiveLibraryFetchStarted) {
                    recursiveLibraryFetchStarted = recursiveLibraryFetchStarted || recursive
                    fetchFolderQueue.addLast(FetchQueueInfo(InventoryModel.getLibraryRootFolderID(), fetchType))
                    scheduleBackgroundIdle()
                }
            } else if (recursive && cat?.preferredType == FolderType.MARKETPLACE_LISTINGS.value) {
                if (fetchFolderQueue.isEmpty() || fetchFolderQueue.peek()?.uuid != catId) {
                    if (recursive && isAISAvailable()) {
                        fetchFolderQueue.addFirst(FetchQueueInfo(catId, FetchType.FOLDER_AND_CONTENT))
                    } else {
                        fetchFolderQueue.addFirst(FetchQueueInfo(catId, fetchType))
                    }
                    scheduleBackgroundIdle()
                    recursiveMarketplaceFetchStarted = true
                }
            } else {
                if (isAISAvailable()) {
                    if (fetchFolderQueue.isEmpty() || fetchFolderQueue.peekLast()?.uuid != catId) {
                        fetchFolderQueue.addLast(FetchQueueInfo(catId, fetchType))
                        scheduleBackgroundIdle()
                    }
                } else if (fetchFolderQueue.isEmpty() || fetchFolderQueue.peek()?.uuid != catId) {
                    fetchFolderQueue.addFirst(FetchQueueInfo(catId, fetchType))
                    scheduleBackgroundIdle()
                }
                if (catId == InventoryModel.getLibraryRootFolderID()) {
                    recursiveLibraryFetchStarted = recursiveLibraryFetchStarted || recursive
                }
                if (catId == InventoryModel.getRootFolderID()) {
                    recursiveInventoryFetchStarted = recursiveInventoryFetchStarted || recursive
                }
            }
        } else {
            val item = InventoryModel.getItem(catId)
            if (item != null && !item.isComplete) scheduleItemFetch(catId)
        }
    }

    fun scheduleFolderFetch(catId: LLUUID, forced: Boolean = false) {
        if (fetchFolderQueue.isEmpty() || fetchFolderQueue.peek()?.uuid != catId) {
            backgroundFetchActive = true
            folderFetchActive = true
            if (forced) {
                if (catId !in forceFetchSet) {
                    forceFetchSet.add(catId)
                    fetchFolderQueue.addFirst(FetchQueueInfo(catId, FetchType.FORCED))
                }
            } else {
                fetchFolderQueue.addFirst(FetchQueueInfo(catId, FetchType.DEFAULT))
            }
            scheduleBackgroundIdle()
        }
    }

    fun scheduleItemFetch(itemId: LLUUID, forced: Boolean = false) {
        if (fetchItemQueue.isEmpty() || fetchItemQueue.peek()?.uuid != itemId) {
            backgroundFetchActive = true
            if (forced) {
                if (itemId !in forceFetchSet) {
                    forceFetchSet.add(itemId)
                    fetchItemQueue.addFirst(FetchQueueInfo(itemId, FetchType.FORCED, isCategory = false))
                }
            } else {
                fetchItemQueue.addFirst(FetchQueueInfo(itemId, FetchType.DEFAULT, isCategory = false))
            }
            scheduleBackgroundIdle()
        }
    }

    fun fetchFolderAndLinks(catId: LLUUID, callback: () -> Unit) {
        val cat = InventoryModel.getCategory(catId)
        cat?.setFetching(ViewerInventoryCategory.FetchType.RECURSIVE)
        incrFetchFolderCount(1)
        expectedFolderIds.add(catId)
        System.err.println("InventoryModelBackgroundFetch: fetchFolderAndLinks not yet implemented")
        backgroundFetchActive = true
        folderFetchActive = true
        scheduleBackgroundIdle()
    }

    fun fetchCOF(callback: () -> Unit) {
        val catId = InventoryModel.findCategoryUUIDForType(FolderType.CURRENT_OUTFIT) ?: return
        val cat   = InventoryModel.getCategory(catId)
        cat?.setFetching(ViewerInventoryCategory.FetchType.RECURSIVE)
        incrFetchFolderCount(1)
        expectedFolderIds.add(catId)
        System.err.println("InventoryModelBackgroundFetch: fetchCOF not yet implemented")
        backgroundFetchActive = true
        folderFetchActive = true
        scheduleBackgroundIdle()
    }

    fun findLostItems() {
        backgroundFetchActive = true
        folderFetchActive = true
        fetchFolderQueue.addLast(FetchQueueInfo(LLUUID.NULL, FetchType.RECURSIVE))
        scheduleBackgroundIdle()
    }

    fun setAllFoldersFetched() {
        if (recursiveInventoryFetchStarted && recursiveLibraryFetchStarted) {
            allRecursiveFoldersFetched = true
        }
        folderFetchActive = false
        if (isBulkFetchProcessingComplete()) backgroundFetchActive = false
        fetchCompletionCallbacks.toList().forEach { it() }
    }

    fun setFetchCompletionCallback(cb: () -> Unit): () -> Unit {
        fetchCompletionCallbacks.add(cb)
        return cb
    }

    fun removeFetchCompletionCallback(cb: () -> Unit) {
        fetchCompletionCallbacks.remove(cb)
    }

    fun addRequestAtFront(id: LLUUID, recursive: Boolean, isCategory: Boolean) {
        val type = if (recursive) FetchType.RECURSIVE else FetchType.DEFAULT
        if (isCategory) fetchFolderQueue.addFirst(FetchQueueInfo(id, type, true))
        else            fetchItemQueue.addFirst(FetchQueueInfo(id, type, false))
    }

    fun addRequestAtBack(id: LLUUID, recursive: Boolean, isCategory: Boolean) {
        val type = if (recursive) FetchType.RECURSIVE else FetchType.DEFAULT
        if (isCategory) fetchFolderQueue.addLast(FetchQueueInfo(id, type, true))
        else            fetchItemQueue.addLast(FetchQueueInfo(id, type, false))
    }

    // ── Status queries ────────────────────────────────────────────────────────

    fun isBulkFetchProcessingComplete(): Boolean =
        fetchFolderQueue.isEmpty() && fetchItemQueue.isEmpty() && fetchCount <= 0

    fun isFolderFetchProcessingComplete(): Boolean =
        fetchFolderQueue.isEmpty() && fetchFolderCount <= 0

    fun isEverythingFetched(): Boolean = allRecursiveFoldersFetched
    fun folderFetchActive(): Boolean   = folderFetchActive

    fun libraryFetchStarted(): Boolean   = recursiveLibraryFetchStarted
    fun libraryFetchCompleted(): Boolean =
        libraryFetchStarted() && fetchQueueContainsNoDescendentsOf(InventoryModel.getLibraryRootFolderID())
    fun libraryFetchInProgress(): Boolean = libraryFetchStarted() && !libraryFetchCompleted()

    fun inventoryFetchStarted(): Boolean   = recursiveInventoryFetchStarted
    fun inventoryFetchCompleted(): Boolean =
        inventoryFetchStarted() && fetchQueueContainsNoDescendentsOf(InventoryModel.getRootFolderID())
    fun inventoryFetchInProgress(): Boolean = inventoryFetchStarted() && !inventoryFetchCompleted()

    // ── Internal callbacks from AIS responses ─────────────────────────────────

    fun onAISContentCallback(
        requestId: LLUUID,
        contentIds: List<LLUUID>,
        responseId: LLUUID,
        fetchType: FetchType
    ) {
        incrFetchFolderCount(-1)

        for (folderId in contentIds) {
            expectedFolderIds.remove(folderId)
            val cat = InventoryModel.getCategory(folderId)
            cat?.setFetching(ViewerInventoryCategory.FetchType.NONE)

            if (responseId == LLUUID.NULL) {
                fetchFolderQueue.addLast(FetchQueueInfo(folderId, FetchType.RECURSIVE))
            } else {
                val (childCats, _) = InventoryModel.getDirectDescendentsOf(folderId)
                childCats.forEach { fetchFolderQueue.addLast(FetchQueueInfo(it.uuid, FetchType.RECURSIVE)) }
            }
        }

        if (fetchFolderQueue.isNotEmpty()) {
            backgroundFetchActive = true
            folderFetchActive = true
            scheduleBackgroundIdle()
        }
    }

    fun onAISFolderCallback(requestId: LLUUID, responseId: LLUUID, fetchType: FetchType) {
        incrFetchFolderCount(-1)

        val found = expectedFolderIds.remove(requestId)
        if (!found && requestId != LLUUID.NULL) {
            // ais shouldn't respond twice — assert in C++; log here
        }

        if (requestId == LLUUID.NULL) return

        var newState = ViewerInventoryCategory.FetchType.NONE
        var requestDescendants = false

        if (responseId == LLUUID.NULL) {
            when (fetchType) {
                FetchType.RECURSIVE -> fetchFolderQueue.addLast(FetchQueueInfo(requestId, FetchType.FOLDER_AND_CONTENT))
                FetchType.FOLDER_AND_CONTENT -> {
                    fetchFolderQueue.addLast(FetchQueueInfo(requestId, FetchType.CONTENT_RECURSIVE))
                    val cat = InventoryModel.getCategory(requestId)
                    if (cat != null && cat.version == ViewerInventoryCategory.VERSION_UNKNOWN) {
                        cat.version = 0
                    }
                    newState = ViewerInventoryCategory.FetchType.FAILED
                }
                else -> {}
            }
        } else {
            when (fetchType) {
                FetchType.RECURSIVE -> {
                    requestDescendants = true
                }
                FetchType.FOLDER_AND_CONTENT -> {
                    fetchFolderQueue.addFirst(FetchQueueInfo(requestId, FetchType.CONTENT_RECURSIVE))
                }
                else -> {}
            }
        }

        if (requestDescendants) {
            val (childCats, _) = InventoryModel.getDirectDescendentsOf(requestId)
            childCats.forEach { fetchFolderQueue.addLast(FetchQueueInfo(it.uuid, FetchType.RECURSIVE)) }
        }

        if (fetchFolderQueue.isNotEmpty()) {
            backgroundFetchActive = true
            folderFetchActive = true
            scheduleBackgroundIdle()
        }

        InventoryModel.getCategory(requestId)?.setFetching(newState)
    }

    // ── Counter helpers ───────────────────────────────────────────────────────

    fun incrFetchCount(delta: Int) {
        fetchCount += delta
        if (fetchCount < 0) fetchCount = 0
    }

    fun incrFetchFolderCount(delta: Int) {
        incrFetchCount(delta)
        fetchFolderCount += delta
        if (fetchFolderCount < 0) fetchFolderCount = 0
    }

    // ── Background fetch dispatcher (called from idle loop) ───────────────────

    fun backgroundFetch() {
        if (!backgroundFetchActive) return
        if (isAISAvailable()) {
            bulkFetchViaAis()
        } else {
            bulkFetch()
        }
    }

    // ── AIS bulk fetch ────────────────────────────────────────────────────────

    private fun bulkFetchViaAis() {
        System.err.println("InventoryModelBackgroundFetch: bulkFetchViaAis not yet implemented")
    }

    // ── Legacy HTTP bulk fetch ────────────────────────────────────────────────

    private fun bulkFetch() {
        System.err.println("InventoryModelBackgroundFetch: bulkFetch not yet implemented")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fetchQueueContainsNoDescendentsOf(catId: LLUUID): Boolean {
        for (info in fetchFolderQueue) {
            if (InventoryModel.isObjectDescendentOf(info.uuid, catId)) return false
        }
        return true
    }

    private fun scheduleBackgroundIdle() {
        System.err.println("InventoryModelBackgroundFetch: scheduleBackgroundIdle not yet implemented")
    }

    private fun isAISAvailable(): Boolean {
        return false
    }
}
