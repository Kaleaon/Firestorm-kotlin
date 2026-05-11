package com.firestorm.newview

open class FilteredWearableListManager(
    private val mWearableList: InventoryItemsList,
    private var mCollector: InventoryCollectFunctor?
) : InventoryObserver() {

    private var mListStale: Boolean = true

    init {
        requireNotNull(mWearableList) { "wearable list must not be null" }
        Inventory.global.addObserver(this)
        Inventory.global.fetchDescendentsOf(Inventory.global.getRootFolderId())
    }

    fun destroy() {
        Inventory.global.removeObserver(this)
    }

    override fun changed(mask: UInt) {
        if (mask == InventoryObserver.CALLING_CARD ||
            mask == InventoryObserver.GESTURE ||
            mask == InventoryObserver.SORT
        ) {
            return
        }

        if (!Inventory.global.isInventoryUsable()) return

        if (mWearableList.isInVisibleChain() || mWearableList.getForceRefresh()) {
            populateList()
        } else {
            mListStale = true
        }
    }

    fun setFilterCollector(collector: InventoryCollectFunctor?) {
        mCollector = collector
        populateList()
    }

    fun populateIfNeeded() {
        if (mListStale) {
            populateList()
        }
    }

    fun holdProgress() {
        mWearableList.setForceRefresh(false)
    }

    private fun populateList() {
        val itemArray = mutableListOf<ViewerInventoryItem>()

        if (mCollector != null) {
            mListStale = false
            Inventory.global.collectDescendentsIf(
                Inventory.global.getRootFolderId(),
                categories = mutableListOf(),
                items = itemArray,
                excludeTrash = true,
                functor = mCollector!!
            )
        }

        if (itemArray.isEmpty() && Inventory.global.isCategoryComplete(Inventory.global.getRootFolderId())) {
            mWearableList.setNoItemsCommentText(Trans.getString("NoneFound"))
        }

        mWearableList.refreshList(itemArray)
    }
}
