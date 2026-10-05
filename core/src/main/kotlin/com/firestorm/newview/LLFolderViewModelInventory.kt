package com.firestorm.newview

import java.util.UUID

abstract class LLFolderViewModelItemInventory(
    rootViewModel: LLFolderViewModelInventory
) : LLFolderViewModelItemCommon(rootViewModel) {

    protected var mPrevPassedAllFilters: Boolean = false
    protected var mLastAddedChildCreationDate: Long = -1L

    abstract fun getUUID(): UUID
    abstract fun getThumbnailUUID(): UUID
    abstract fun getCreationDate(): Long
    abstract fun setCreationDate(creationDateUtc: Long)
    abstract fun getPermissionMask(): Int
    abstract fun getPreferredType(): LLFolderType.EType
    abstract fun showProperties()
    open fun isItemInTrash(): Boolean = false
    open fun isItemInOutfits(): Boolean = false
    open fun isAgentInventory(): Boolean = false
    open fun isAgentInventoryRoot(): Boolean = false
    abstract fun isUpToDate(): Boolean
    abstract fun hasChildren(): Boolean
    abstract fun getInventoryType(): LLInventoryType.EType
    abstract fun performAction(model: LLInventoryModel, action: String)
    abstract fun getWearableType(): LLWearableType.EType
    abstract fun getSettingsType(): LLSettingsType.TypeE
    abstract fun getSortGroup(): EInventorySortGroup
    abstract fun getInventoryObject(): LLInventoryObject?
    abstract fun startDrag(type: Array<EDragAndDropType>, id: Array<UUID>): Boolean
    abstract fun getDragSource(): LLToolDragAndDrop.ESource

    open fun canSortContent(): Boolean = getUUID() != UUID.fromString("00000000-0000-0000-0000-000000000000")

    override fun addChild(child: LLFolderViewModelItem) {
        val modelChild = child as LLFolderViewModelItemInventory
        mLastAddedChildCreationDate = modelChild.getCreationDate()
        super.addChild(child)
    }

    override fun requestSort() {
        super.requestSort()
        val folderp = mFolderViewItem as? LLFolderViewFolder
        folderp?.requestArrange()

        val sorter = (mRootViewModel as LLFolderViewModelInventory).getSorter()

        if (sorter.isByDate() && mParent != null) {
            if (folderp == null) {
                mParent.requestSort()
            } else if (sorter.isFoldersByDate()) {
                if (mLastAddedChildCreationDate == -1L
                    || mLastAddedChildCreationDate > getCreationDate()
                ) {
                    val modelParent = mParent as LLFolderViewModelItemInventory
                    modelParent.mLastAddedChildCreationDate = mLastAddedChildCreationDate
                    mParent.requestSort()
                }
            }
        }
        mLastAddedChildCreationDate = -1L
    }

    override fun setPassedFilter(
        passed: Boolean,
        filterGeneration: Int,
        stringOffset: Int,
        stringSize: Int
    ) {
        val generationSkip = mMarkedDirtyGeneration >= 0
            && mPrevPassedAllFilters
            && mMarkedDirtyGeneration < mRootViewModel.getFilter().getFirstSuccessGeneration()
        val lastGeneration = mLastFilterGeneration
        super.setPassedFilter(passed, filterGeneration, stringOffset, stringSize)
        val before = mPrevPassedAllFilters
        mPrevPassedAllFilters = passedFilter(filterGeneration)

        if (before != mPrevPassedAllFilters
            || generationSkip
            || (mPrevPassedAllFilters && lastGeneration < mRootViewModel.getFilter().getFirstRequiredGeneration())
        ) {
            mFolderViewItem.getParentFolder()?.requestArrange()
        }
    }

    override fun filterChildItem(item: LLFolderViewModelItem?, filter: LLFolderViewFilter): Boolean {
        val filterGeneration = filter.getCurrentGeneration()
        var continueFiltering = true
        if (item != null) {
            if (item.getLastFilterGeneration() < filterGeneration) {
                continueFiltering = item.filter(filter)
            }
            if (item.passedFilter()) {
                var viewModel: LLFolderViewModelItemInventory? = this
                while (viewModel != null && viewModel.mMostFilteredDescendantGeneration < filterGeneration) {
                    viewModel.mMostFilteredDescendantGeneration = filterGeneration
                    viewModel = viewModel.mParent as? LLFolderViewModelItemInventory
                }
            }
        }
        return continueFiltering
    }

    override fun filter(filter: LLFolderViewFilter): Boolean {
        val filterGeneration = filter.getCurrentGeneration()
        val mustPassGeneration = filter.getFirstRequiredGeneration()

        if (getLastFilterGeneration() >= mustPassGeneration
            && getLastFolderFilterGeneration() >= mustPassGeneration
            && !passedFilter(mustPassGeneration)
        ) {
            setPassedFilter(false, filterGeneration)
            setPassedFolderFilter(false, filterGeneration)
            return true
        }

        val isFolder = getInventoryType() == LLInventoryType.EType.IT_CATEGORY
        val passedFilterFolder = if (isFolder) filter.checkFolder(this) else true
        setPassedFolderFilter(passedFilterFolder, filterGeneration)

        var continueFiltering = true

        if (mChildren.isNotEmpty()
            && (getLastFilterGeneration() < mustPassGeneration
                || descendantsPassedFilter(mustPassGeneration))
        ) {
            for (child in mChildren) {
                continueFiltering = filterChildItem(child, filter)
                if (!continueFiltering) break
            }
        }

        if (continueFiltering) {
            val passedFilter = filter.check(this)
            if (passedFilter && mChildren.isEmpty() && isFolder) {
                var viewModel: LLFolderViewModelItemInventory? = this
                while (viewModel != null && viewModel.mMostFilteredDescendantGeneration < filterGeneration) {
                    viewModel.mMostFilteredDescendantGeneration = filterGeneration
                    viewModel = viewModel.mParent as? LLFolderViewModelItemInventory
                }
            }
            setPassedFilter(passedFilter, filterGeneration, filter.getStringMatchOffset(this), filter.getFilterStringSize())
            continueFiltering = !filter.isTimedOut()
        }
        return continueFiltering
    }
}

class LLInventorySort(order: Int = 0) {

    private var mSortOrder: UInt = 0u
    private var mByDate: Boolean = false
    private var mSystemToTop: Boolean = false
    private var mFoldersByName: Boolean = false
    private var mFoldersByWeight: Boolean = false

    init {
        fromOrder(order)
    }

    fun isByDate(): Boolean = mByDate
    fun isFoldersByName(): Boolean = (!mByDate || mFoldersByName) && !mFoldersByWeight
    fun isFoldersByDate(): Boolean = mByDate && !mFoldersByName && !mFoldersByWeight
    fun getSortOrder(): UInt = mSortOrder

    fun fromOrder(order: Int) {
        mSortOrder = order.toUInt()
        mByDate = (mSortOrder and LLInventoryFilter.SO_DATE) != 0u
        mSystemToTop = (mSortOrder and LLInventoryFilter.SO_SYSTEM_FOLDERS_TO_TOP) != 0u
        mFoldersByName = (mSortOrder and LLInventoryFilter.SO_FOLDERS_BY_NAME) != 0u
        mFoldersByWeight = (mSortOrder and LLInventoryFilter.SO_FOLDERS_BY_WEIGHT) != 0u
    }

    fun compare(a: LLFolderViewModelItemInventory, b: LLFolderViewModelItemInventory): Boolean {
        if (a.getSortGroup() == EInventorySortGroup.SG_ITEM
            && b.getSortGroup() == EInventorySortGroup.SG_ITEM
            && a.getInventoryType() == LLInventoryType.EType.IT_LANDMARK
            && b.getInventoryType() == LLInventoryType.EType.IT_LANDMARK
        ) {
            val favoritesFolderId = gInventory.findCategoryUUIDForType(LLFolderType.EType.FT_FAVORITE)
            if (gInventory.isObjectDescendentOf(a.getUUID(), favoritesFolderId)
                && gInventory.isObjectDescendentOf(b.getUUID(), favoritesFolderId)
            ) {
                val aSort = LLFavoritesOrderStorage.instance().getSortIndex(a.getUUID())
                val bSort = LLFavoritesOrderStorage.instance().getSortIndex(b.getUUID())
                if (!(aSort < 0 && bSort < 0)) {
                    return aSort < bSort
                }
            }
        }

        val byName = ((!mByDate || (mFoldersByName && a.getSortGroup() != EInventorySortGroup.SG_ITEM)) && !mFoldersByWeight)

        if (a.getSortGroup() != b.getSortGroup()) {
            if (mSystemToTop) {
                return a.getSortGroup().ordinal < b.getSortGroup().ordinal
            } else if (mByDate) {
                if (a.getSortGroup() == EInventorySortGroup.SG_TRASH_FOLDER
                    || b.getSortGroup() == EInventorySortGroup.SG_TRASH_FOLDER
                ) {
                    return b.getSortGroup() == EInventorySortGroup.SG_TRASH_FOLDER
                }
            }
        }

        return when {
            byName -> {
                val compare = LLStringUtil.compareDict(a.getDisplayName(), b.getDisplayName())
                if (compare == 0) a.getCreationDate() > b.getCreationDate() else compare < 0
            }
            mFoldersByWeight -> {
                val weightA = computeStockCount(a.getUUID())
                val weightB = computeStockCount(b.getUUID())
                when {
                    weightA == weightB -> LLStringUtil.compareDict(a.getDisplayName(), b.getDisplayName()) < 0
                    weightA == COMPUTE_STOCK_INFINITE -> false
                    weightB == COMPUTE_STOCK_INFINITE -> true
                    else -> weightA < weightB
                }
            }
            else -> {
                val firstCreate = a.getCreationDate()
                val secondCreate = b.getCreationDate()
                if (firstCreate == secondCreate) {
                    LLStringUtil.compareDict(a.getDisplayName(), b.getDisplayName()) < 0
                } else {
                    firstCreate > secondCreate
                }
            }
        }
    }
}

class LLFolderViewModelInventory(name: String) : LLFolderViewModel<LLInventorySort, LLFolderViewModelItemInventory, LLFolderViewModelItemInventory, LLInventoryFilter>(
    LLInventorySort(),
    LLInventoryFilter(LLInventoryFilter.Params().name(name))
) {
    private var mTaskID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    fun setTaskID(id: UUID) {
        mTaskID = id
    }

    fun sort(folder: LLFolderViewFolder) {
        if (!folder.areChildrenInited() || !needsSort(folder.getViewModelItem())) return

        val sortModelp = folder.getViewModelItem() as? LLFolderViewModelItemInventory ?: return
        if (!sortModelp.canSortContent()) return

        var hasFavorites = false

        for (childFolderp in folder.getFolders()) {
            sort(childFolderp)

            val modelp = childFolderp.getViewModelItem() as LLFolderViewModelItemInventory
            hasFavorites = hasFavorites || childFolderp.isFavorite() || childFolderp.hasFavorites()

            if (childFolderp.getFoldersCount() > 0) {
                val folderp = childFolderp.getFolders().first().getViewModelItem() as LLFolderViewModelItemInventory
                val mostRecentFolderTime = folderp.getCreationDate()
                if (mostRecentFolderTime > modelp.getCreationDate()) {
                    modelp.setCreationDate(mostRecentFolderTime)
                }
            }
            if (childFolderp.getItemsCount() > 0) {
                val itemp = childFolderp.getItems().first().getViewModelItem() as LLFolderViewModelItemInventory
                val mostRecentItemTime = itemp.getCreationDate()
                if (mostRecentItemTime > modelp.getCreationDate()) {
                    modelp.setCreationDate(mostRecentItemTime)
                }
            }
        }

        if (!hasFavorites) {
            for (childItemp in folder.getItems()) {
                hasFavorites = hasFavorites || childItemp.isFavorite()
                if (hasFavorites) break
            }
        }
        if (hasFavorites) {
            folder.updateHasFavorites(true)
        }

        super.sort(folder)
    }

    fun contentsReady(): Boolean {
        return !LLInventoryModelBackgroundFetch.instance().folderFetchActive()
    }

    fun isFolderComplete(folder: LLFolderViewFolder): Boolean {
        val modelp = folder.getViewModelItem() as? LLFolderViewModelItemInventory ?: return false
        val catId = modelp.getUUID()
        if (catId == UUID.fromString("00000000-0000-0000-0000-000000000000")) return false
        val cat = gInventory.getCategory(catId) ?: return false
        val descendentsServer = cat.getDescendentCount()
        val descendentsActual = cat.getViewerDescendentCount()
        return descendentsServer == descendentsActual
            || (descendentsActual > 0 && descendentsServer == -1)
    }

    fun startDrag(items: MutableList<LLFolderViewModelItem>): Boolean {
        val types: MutableList<EDragAndDropType> = mutableListOf()
        val cargoIds: MutableList<UUID> = mutableListOf()
        var canDrag = true
        if (items.isNotEmpty()) {
            for (item in items) {
                val typeArr = arrayOf(EDragAndDropType.DAD_NONE)
                val idArr = arrayOf(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                canDrag = canDrag && (item as LLFolderViewModelItemInventory).startDrag(typeArr, idArr)
                types.add(typeArr[0])
                cargoIds.add(idArr[0])
            }
            LLToolDragAndDrop.getInstance().beginMultiDrag(
                types,
                cargoIds,
                (items.first() as LLFolderViewModelItemInventory).getDragSource(),
                mTaskID
            )
        }
        return canDrag
    }
}
