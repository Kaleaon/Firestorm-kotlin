package com.firestorm.newview

// Stub UI-image handle — full implementation lives in the UI layer.
interface LLUIImage

// Stub for folder-view filter used during tree traversal.
interface LLFolderViewFilter {
    fun getCurrentGeneration(): Int
    fun getFirstRequiredGeneration(): Int
    fun checkFolder(item: LLFolderViewModelItem): Boolean
    fun check(item: LLFolderViewModelItem): Boolean
    fun getStringMatchOffset(item: LLFolderViewModelItem): Int
    fun getFilterStringSize(): Int
    fun isTimedOut(): Boolean
}

// Base class representing an item in the folder-view model tree.
// Mirrors LLFolderViewModelItemCommon from the C++ codebase.
abstract class LLFolderViewModelItem {
    var mParent: LLFolderViewModelItem? = null
    protected val mChildren: MutableList<LLFolderViewModelItem> = mutableListOf()

    protected var mLastFilterGeneration: Int = -1
    protected var mLastFolderFilterGeneration: Int = -1
    protected var mMostFilteredDescendantGeneration: Int = -1
    protected var mPassedFilter: Boolean = false
    protected var mPassedFolderFilter: Boolean = false

    fun getLastFilterGeneration(): Int = mLastFilterGeneration
    fun getLastFolderFilterGeneration(): Int = mLastFolderFilterGeneration

    fun passedFilter(generation: Int = mLastFilterGeneration): Boolean = mPassedFilter

    fun descendantsPassedFilter(generation: Int): Boolean =
        mMostFilteredDescendantGeneration >= generation

    fun setPassedFilter(passed: Boolean, generation: Int, matchOffset: Int = 0, stringSize: Int = 0) {
        mPassedFilter = passed
        mLastFilterGeneration = generation
    }

    fun setPassedFolderFilter(passed: Boolean, generation: Int) {
        mPassedFolderFilter = passed
        mLastFolderFilterGeneration = generation
    }

    abstract fun getName(): String
    abstract fun getDisplayName(): String
    abstract fun getSearchableName(): String
    abstract fun getSearchableDescription(): String
    abstract fun getSearchableCreatorName(): String
    abstract fun getSearchableUUIDString(): String
    abstract fun getSearchableAll(): String
    abstract fun getIcon(): LLUIImage?
    abstract fun getIconOpen(): LLUIImage?
    abstract fun getIconOverlay(): LLUIImage?
    abstract fun getLabelStyle(): Int
    abstract fun getLabelSuffix(): String
    abstract fun openItem()
    abstract fun closeItem()
    abstract fun selectItem()
    abstract fun navigateToFolder(newWindow: Boolean = false, changeMode: Boolean = false)
    abstract fun isItemWearable(): Boolean
    abstract fun isItemRenameable(): Boolean
    abstract fun renameItem(newName: String): Boolean
    abstract fun isItemMovable(): Boolean
    abstract fun move(parentListener: LLFolderViewModelItem)
    abstract fun isItemRemovable(checkWorn: Boolean = true): Boolean
    abstract fun removeItem(): Boolean
    abstract fun removeBatch(batch: MutableList<LLFolderViewModelItem>)
    abstract fun isItemCopyable(canCopyAsLink: Boolean = true): Boolean
    abstract fun copyToClipboard(): Boolean
    abstract fun cutToClipboard(): Boolean
    abstract fun isCutToClipboard(): Boolean
    abstract fun isClipboardPasteable(): Boolean
    abstract fun pasteFromClipboard()
    abstract fun pasteLinkFromClipboard()
    abstract fun buildContextMenu(flags: UInt)
    abstract fun potentiallyVisible(): Boolean
    abstract fun hasChildren(): Boolean
    abstract fun dragOrDrop(
        mask: UInt,
        drop: Boolean,
        cargoType: Int,
        cargoData: Any?,
        tooltipMsg: StringBuilder
    ): Boolean
    abstract fun filter(filter: LLFolderViewFilter): Boolean
    abstract fun isFavorite(): Boolean
    abstract fun isItemInTrash(): Boolean
    abstract fun isAgentInventory(): Boolean
    abstract fun isAgentInventoryRoot(): Boolean
}

// Stub interface for the root view-model — provides context for the folder tree.
interface LLFolderViewModelInterface

class LLGLTFFolderItem : LLFolderViewModelItem {

    enum class EType {
        TYPE_ROOT,
        TYPE_SCENE,
        TYPE_NODE,
        TYPE_MESH,
        TYPE_SKIN
    }

    // Using inventory icons as a placeholder — GLTF needs its own icon set.
    private var pIcon: LLUIImage? = null
    private var mName: String = ""
    private var mItemType: EType = EType.TYPE_ROOT

    // mItemId is not necessarily unique across types; e.g. multiple nodes may share a mesh id.
    private var mItemId: Int = -1

    constructor(
        id: Int,
        displayName: String,
        type: EType,
        rootViewModel: LLFolderViewModelInterface
    ) {
        mName = displayName
        mItemType = type
        mItemId = id
        init()
    }

    constructor(rootViewModel: LLFolderViewModelInterface) {
        init()
    }

    private fun init() {
        pIcon = when (mItemType) {
            EType.TYPE_SCENE -> getInventoryIcon("ICONNAME_OBJECT_MULTI")
            EType.TYPE_NODE  -> getInventoryIcon("ICONNAME_OBJECT")
            EType.TYPE_MESH  -> getInventoryIcon("ICONNAME_MESH")
            EType.TYPE_SKIN  -> getInventoryIcon("ICONNAME_BODYPART_SKIN")
            else             -> getInventoryIcon("ICONNAME_OBJECT")
        }
    }

    private fun getInventoryIcon(iconName: String): LLUIImage? {
        return null
    }

    override fun getName(): String = mName
    override fun getDisplayName(): String = mName
    override fun getSearchableName(): String = mName
    override fun getSearchableDescription(): String = ""
    override fun getSearchableCreatorName(): String = ""
    override fun getSearchableUUIDString(): String = ""
    override fun getSearchableAll(): String = ""

    override fun getIcon(): LLUIImage? = pIcon
    override fun getIconOpen(): LLUIImage? = getIcon()
    override fun getIconOverlay(): LLUIImage? = null

    // LLFontGL::NORMAL = 0
    override fun getLabelStyle(): Int = 0
    override fun getLabelSuffix(): String = ""

    override fun openItem() {}
    override fun closeItem() {}
    override fun selectItem() {}
    override fun navigateToFolder(newWindow: Boolean, changeMode: Boolean) {}

    override fun isItemWearable(): Boolean = false
    override fun isItemRenameable(): Boolean = false
    override fun renameItem(newName: String): Boolean = false
    override fun isItemMovable(): Boolean = false
    override fun move(parentListener: LLFolderViewModelItem) {}
    override fun isItemRemovable(checkWorn: Boolean): Boolean = false
    override fun removeItem(): Boolean = false
    override fun removeBatch(batch: MutableList<LLFolderViewModelItem>) {}
    override fun isItemCopyable(canCopyAsLink: Boolean): Boolean = false
    override fun copyToClipboard(): Boolean = false
    override fun cutToClipboard(): Boolean = false
    override fun isCutToClipboard(): Boolean = false
    override fun isClipboardPasteable(): Boolean = false
    override fun pasteFromClipboard() {}
    override fun pasteLinkFromClipboard() {}
    override fun buildContextMenu(flags: UInt) {}
    override fun potentiallyVisible(): Boolean = true
    override fun hasChildren(): Boolean = mChildren.isNotEmpty()

    override fun dragOrDrop(
        mask: UInt,
        drop: Boolean,
        cargoType: Int,
        cargoData: Any?,
        tooltipMsg: StringBuilder
    ): Boolean = false

    override fun isFavorite(): Boolean = false
    override fun isItemInTrash(): Boolean = false
    override fun isAgentInventory(): Boolean = false
    override fun isAgentInventoryRoot(): Boolean = false

    fun getType(): EType = mItemType
    fun getItemId(): Int = mItemId

    fun filterChildItem(item: LLFolderViewModelItem?, filter: LLFolderViewFilter): Boolean {
        val filterGeneration = filter.getCurrentGeneration()
        var continueFiltering = true
        if (item != null) {
            if (item.getLastFilterGeneration() < filterGeneration) {
                continueFiltering = item.filter(filter)
            }
            if (item.passedFilter()) {
                var viewModel: LLGLTFFolderItem? = this
                while (viewModel != null && viewModel.mMostFilteredDescendantGeneration < filterGeneration) {
                    viewModel.mMostFilteredDescendantGeneration = filterGeneration
                    viewModel = viewModel.mParent as? LLGLTFFolderItem
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
            // Previously failed a filter that is a subset of the current one — skip early.
            setPassedFilter(false, filterGeneration)
            setPassedFolderFilter(false, filterGeneration)
            return true
        }

        val passedFilterFolder = filter.checkFolder(this)
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
            if (passedFilter && mChildren.isEmpty()) {
                var viewModel: LLGLTFFolderItem? = this
                while (viewModel != null && viewModel.mMostFilteredDescendantGeneration < filterGeneration) {
                    viewModel.mMostFilteredDescendantGeneration = filterGeneration
                    viewModel = viewModel.mParent as? LLGLTFFolderItem
                }
            }
            setPassedFilter(passedFilter, filterGeneration, filter.getStringMatchOffset(this), filter.getFilterStringSize())
            continueFiltering = !filter.isTimedOut()
        }

        return continueFiltering
    }
}
