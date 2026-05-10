package com.firestorm.llui

import com.firestorm.llmath.Rect
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val DEFAULT_LABEL_PADDING_RIGHT = 4
private const val FOLDER_CLOSE_TIME_CONSTANT = 0.02f
private const val FOLDER_OPEN_TIME_CONSTANT = 0.03f
private const val FAVORITE_IMAGE_SIZE = 14
private const val FAVORITE_IMAGE_PAD = 3

open class FolderViewItem(
    val root: FolderView,
    val viewModelItem: FolderViewModelItem,
    name: String = "",
    val folderIndentation: Int = 0,
    itemHeightParam: Int = 20,
    val itemTopPad: Int = 0,
    val marketplaceItem: Boolean = false,
    allowDrop: Boolean = true,
    val fontColor: Color4 = Color4(1f, 1f, 1f, 1f),
    val fontHighlightColor: Color4 = Color4(1f, 1f, 1f, 1f),
    val leftPad: Int = 0,
    val iconPad: Int = 0,
    val iconWidth: Int = 0,
    val textPad: Int = 0,
    val textPadRight: Int = 0,
    val arrowSize: Int = 0,
    val maxFolderItemOverlap: Int = 0,
    val singleFolderMode: Boolean = false,
    val doubleClickOverride: Boolean = false,
    val forInventory: Boolean = false,
) {
    companion object {
        val fontsForStyle: MutableMap<Int, Any> = mutableMapOf()
        var topPad: Int = 0
        var folderArrowImg: UIImage? = null
        var selectionImg: UIImage? = null
        var favoriteImg: UIImage? = null
        var favoriteContentImg: UIImage? = null
        var suffixFont: Any? = null

        var fgColor: Color4 = Color4(1f, 1f, 1f, 1f)
        var highlightBgColor: Color4 = Color4(0f, 0f, 1f, 0.5f)
        var flashBgColor: Color4 = Color4(1f, 1f, 0f, 0.5f)
        var focusOutlineColor: Color4 = Color4(1f, 1f, 1f, 1f)
        var mouseOverColor: Color4 = Color4(0.5f, 0.5f, 0.5f, 0.3f)
        var filterBgColor: Color4 = Color4(1f, 1f, 0f, 1f)
        var filterTextColor: Color4 = Color4(0f, 0f, 0f, 1f)
        var suffixColor: Color4 = Color4(0.6f, 0.6f, 1f, 1f)
        var searchStatusColor: Color4 = Color4(1f, 1f, 1f, 1f)
        var favoriteColor: Color4 = Color4(1f, 0.85f, 0f, 1f)
        var protectedColor: Color4 = Color4(1f, 0f, 0f, 1f)
        var colorSetInitialized: Boolean = false

        fun getLabelFontForStyle(style: Int): Any {
            return fontsForStyle.getOrPut(style) {
                TODO("GPU: resolve font for style=$style from font registry")
            }
        }

        fun initClass() {
            TODO("GPU: load default params and initialize static colors and images from UI theme")
        }

        fun cleanupClass() {
            fontsForStyle.clear()
            folderArrowImg = null; selectionImg = null; favoriteImg = null
            favoriteContentImg = null; suffixFont = null
        }
    }

    var label: String = name
    var labelWidth: Int = 0
    var labelWidthDirty: Boolean = true
    var isFavorite: Boolean = false
    var hasFavorites: Boolean = false
    var labelPaddingRight: Int = DEFAULT_LABEL_PADDING_RIGHT
    var parentFolder: FolderViewFolder? = null
    var labelStyle: Int = 0
    var labelSuffix: String = ""
    var suffixNeedsRefresh: Boolean = true
    var icon: UIImage? = null
    var iconOpen: UIImage? = null
    var iconOverlay: UIImage? = null
    var localIndentation: Int = folderIndentation
    var indentation: Int = 0
    var itemHeight: Int = itemHeightParam
    var controlLabelRotation: Float = 0f
    var hasVisibleChildren: Boolean = false
    var isCurSelection: Boolean = false
    var dragAndDropTarget: Boolean = false
    var isMouseOverTitle: Boolean = false
    var allowDrop: Boolean = allowDrop
    var selectPending: Boolean = false
    var isItemCut: Boolean = false
    var cutGeneration: Int = 0
    var isSelected: Boolean = false

    private var labelFont: Any? = null

    var visible: Boolean = true
    var rect: Rect = Rect()

    init {
        viewModelItem.setFolderViewItem(this)
        if (!colorSetInitialized) {
            TODO("GPU: initialize static color set from UI color table")
        }
    }

    fun getLabelFont(): Any {
        if (labelFont == null) labelFont = getLabelFontForStyle(labelStyle)
        return labelFont!!
    }

    open fun postBuild(): Boolean {
        label = viewModelItem.getDisplayName()
        isFavorite = viewModelItem.isFavorite() && !viewModelItem.isItemInTrash()
        viewModelItem.dirtyFilter()
        suffixNeedsRefresh = true
        labelWidthDirty = true
        return true
    }

    fun getRoot(): FolderView = root

    fun isDescendantOf(potentialAncestor: FolderViewFolder): Boolean {
        var current: FolderViewItem = this
        while (current.parentFolder != null) {
            if (current.parentFolder === potentialAncestor) return true
            current = current.parentFolder!!
        }
        return false
    }

    fun getNextOpenNode(includeChildren: Boolean = true): FolderViewItem? {
        val parent = parentFolder ?: return null
        var item = parent.getNextFromChild(this, includeChildren)
        while (item != null && !item.visible) {
            val next = item.parentFolder?.getNextFromChild(item, includeChildren)
            if (next === item) return if (item.visible) item else this
            item = next
        }
        return item
    }

    fun getPreviousOpenNode(includeChildren: Boolean = true): FolderViewItem? {
        val parent = parentFolder ?: return null
        var item = parent.getPreviousFromChild(this, includeChildren)
        while (item != null && !item.visible) {
            val prev = item.parentFolder?.getPreviousFromChild(item, includeChildren)
            if (prev === item) return if (item.visible) item else this
            item = prev
        }
        return item
    }

    fun passedFilter(filterGeneration: Int = -1): Boolean =
        viewModelItem.passedFilter(filterGeneration)

    fun isPotentiallyVisible(filterGeneration: Int = -1): Boolean {
        val gen = if (filterGeneration < 0)
            root.getFolderViewModel().getFilter().getFirstSuccessGeneration()
        else filterGeneration
        var v = viewModelItem.passedFilter(gen)
        if (viewModelItem.getMarkedDirtyGeneration() >= gen) v = v || visible
        return v
    }

    open fun refresh() {
        label = viewModelItem.getDisplayName()
        isFavorite = viewModelItem.isFavorite() && !viewModelItem.isItemInTrash()
        icon = viewModelItem.getIcon()
        iconOpen = viewModelItem.getIconOpen()
        iconOverlay = viewModelItem.getIconOverlay()
        if (root.useLabelSuffix()) {
            labelStyle = viewModelItem.getLabelStyle()
            labelFont = null
            labelSuffix = viewModelItem.getLabelSuffix()
        }
        viewModelItem.dirtyFilter()
        labelWidthDirty = true
        suffixNeedsRefresh = false
    }

    fun refreshSuffix() {
        icon = viewModelItem.getIcon()
        iconOpen = viewModelItem.getIconOpen()
        iconOverlay = viewModelItem.getIconOverlay()
        isFavorite = viewModelItem.isFavorite() && !viewModelItem.isItemInTrash()
        if (root.useLabelSuffix()) {
            labelStyle = viewModelItem.getLabelStyle()
            labelFont = null
            labelSuffix = viewModelItem.getLabelSuffix()
        }
        labelWidthDirty = true
        suffixNeedsRefresh = false
    }

    fun arrangeAndSet(setSelection: Boolean, takeKeyboardFocus: Boolean) {
        parentFolder?.requestArrange()
        if (setSelection) {
            root.setSelection(this, true, takeKeyboardFocus)
            root.scrollToShowSelection()
        }
    }

    fun getSelectionList(): Set<FolderViewItem> = emptySet()

    open fun addToFolder(folder: FolderViewFolder) {
        folder.addItem(this)
        indentation = parentFolder?.let { it.indentation + localIndentation } ?: 0
    }

    open fun arrange(width: IntArray, height: IntArray): Int {
        indentation = if (forInventory) {
            if (parentFolder != null && parentFolder?.parentFolder != null)
                (parentFolder!!.indentation + localIndentation) else 0
        } else {
            parentFolder?.let { it.indentation + localIndentation } ?: 0
        }

        if (labelWidthDirty) {
            if (suffixNeedsRefresh) refreshSuffix()
            TODO("GPU: measure labelWidth using font metrics for label and suffix strings")
        }

        width[0] = max(width[0], labelWidth)
        if (root.getUseEllipses()) width[0] = min(width[0], root.rect.width)
        height[0] = getItemHeight()
        return height[0]
    }

    open fun getItemHeight(): Int = itemHeight

    fun getLabelXPos(): Int = indentation + arrowSize + textPad + iconWidth + iconPad

    fun getIconPad(): Int = iconPad
    fun getTextPad(): Int = textPad

    open fun setSelection(selection: FolderViewItem, openItem: Boolean, takeKeyboardFocus: Boolean = false): Boolean {
        if (selection === this && !isSelected) selectItem()
        else if (isSelected) deselectItem()
        return isSelected
    }

    open fun changeSelection(selection: FolderViewItem, selected: Boolean): Boolean {
        if (selection === this) {
            if (isSelected) deselectItem() else selectItem()
            return true
        }
        return false
    }

    fun deselectItem() { isSelected = false }

    open fun selectItem() {
        if (!isSelected) {
            isSelected = true
            viewModelItem.selectItem()
        }
    }

    open fun isMovable(): Boolean = viewModelItem.isItemMovable()
    open fun isRemovable(): Boolean = viewModelItem.isItemRemovable()

    open fun destroyView() {
        root.removeFromSelectionList(this)
        parentFolder?.extractItem(this)
    }

    fun remove(): Boolean {
        if (!isRemovable()) return false
        return viewModelItem.removeItem()
    }

    fun buildContextMenu(menu: MenuGL, flags: UInt) {
        viewModelItem.buildContextMenu(menu, flags)
    }

    open fun openItem() {
        if (!marketplaceItem || !viewModelItem.isItemWearable()) {
            viewModelItem.openItem()
        }
    }

    fun rename(newName: String) {
        if (newName.isNotEmpty()) viewModelItem.renameItem(newName)
    }

    fun getName(): String = viewModelItem.getName()
    fun getLabel(): String = label

    open fun setOpen(open: Boolean = true) {}
    open fun isOpen(): Boolean = false

    open fun isFolderComplete(): Boolean = true
    open fun areChildrenInited(): Boolean = true
    open fun setChildrenInited(inited: Boolean) {}

    fun isInSelection(): Boolean = isSelected || (parentFolder?.isInSelection() ?: false)

    fun setUnselected() { isSelected = false }
    fun setIsCurSelection(select: Boolean) { isCurSelection = select }
    fun getIsCurSelection(): Boolean = isCurSelection
    fun isFavorite(): Boolean = isFavorite
    fun isSingleFolderMode(): Boolean = singleFolderMode
    fun getFolderViewModel(): FolderViewModelInterface = root.getFolderViewModel()

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (!isSelected) root.setSelection(this, false)
        return true
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val MASK_CTRL = 0x04; val MASK_SHIFT = 0x01
        gFocusMgr.setMouseCapture(null)
        if (!isSelected) {
            when {
                mask and MASK_CTRL != 0 -> root.changeSelection(this, !isSelected)
                mask and MASK_SHIFT != 0 -> parentFolder?.extendSelectionTo(this)
                else -> root.setSelection(this, false)
            }
        } else {
            selectPending = true
        }
        return true
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        isMouseOverTitle = (y > (rect.height - itemHeight))
        if (hasMouseCapture() && isMovable()) {
            TODO("APR: use JVM equivalent for drag threshold check and cursor change")
        }
        root.setHoveredItem(this)
        root.setShowSelectionContext(false)
        return false
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        val MASK_CTRL = 0x04; val MASK_SHIFT = 0x01
        if (pointInView(x, y) && selectPending) {
            when {
                mask and MASK_CTRL != 0 -> root.changeSelection(this, !isSelected)
                mask and MASK_SHIFT != 0 -> parentFolder?.extendSelectionTo(this)
                else -> root.setSelection(this, false)
            }
        }
        selectPending = false
        gFocusMgr.setMouseCapture(null)
        return true
    }

    open fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        openItem()
        return true
    }

    fun onMouseLeave(x: Int, y: Int, mask: Int) {
        isMouseOverTitle = false
        if (this === root.getHoveredItem()) root.clearHoveredItem()
    }

    open fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: IntArray, tooltipMsg: StringBuilder,
    ): Boolean {
        val accepted = viewModelItem.dragOrDrop(mask.toUInt(), drop, cargoType, cargoData, tooltipMsg)
        if (accepted) {
            dragAndDropTarget = true
            accept[0] = ACCEPT_YES_MULTI
        } else {
            accept[0] = ACCEPT_NO
        }
        if (parentFolder != null && !accepted) {
            root.setDraggingOverItem(this)
            val handled = parentFolder!!.handleDragAndDropFromChild(mask, drop, cargoType, cargoData, accept, tooltipMsg)
            root.setDraggingOverItem(null)
            return handled
        }
        return accepted
    }

    fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        TODO("GPU: measure label width; show tooltip if truncated, else clear tooltip")
    }

    open fun draw() {
        val showContext = root.getShowSelectionContext()
        val filled = showContext || root.getParentPanel().hasFocus()
        viewModelItem.update()
        if (!singleFolderMode) drawOpenFolderArrow()
        drawFavoriteIcon()
        drawHighlight(showContext, filled, highlightBgColor, flashBgColor, focusOutlineColor, mouseOverColor)
        TODO("GPU: draw icon, label, suffix, filter highlights, and locked/protected annotations")
    }

    fun drawOpenFolderArrow() {
        if (hasVisibleChildren || !isFolderComplete()) {
            TODO("GPU: gl_draw_scaled_rotated_image for folder arrow at indentation=$indentation rotation=$controlLabelRotation")
        }
    }

    fun drawFavoriteIcon() {
        TODO("GPU: draw favorite star or hollow-star image based on isFavorite/hasFavorites flags")
    }

    open fun isHighlightAllowed(): Boolean = isSelected
    open fun isHighlightActive(): Boolean = isCurSelection
    open fun isFadeItem(): Boolean {
        TODO("APR: use JVM equivalent for clipboard cut-mode check to determine fade state")
    }

    open fun isFlashing(): Boolean = false
    open fun setFlashState(state: Boolean, b: Boolean) {}

    fun drawHighlight(
        showContent: Boolean, hasKeyboardFocus: Boolean,
        selectColor: Color4, flashColor: Color4,
        outlineColor: Color4, hoverColor: Color4,
    ) {
        TODO("GPU: render selection/flash/outline/hover highlight rects via gl_rect_2d")
    }

    fun drawLabel(font: Any, x: Float, y: Float, color: Color4, rightX: FloatArray) {
        TODO("GPU: render label text with ellipsis clipping using font vertex buffer")
    }

    fun hasMouseCapture(): Boolean = TODO("APR: use JVM equivalent for gFocusMgr mouse capture check")
    fun pointInView(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < rect.width && y < rect.height

    companion object {
        const val ACCEPT_YES_MULTI = 2
        const val ACCEPT_NO = 0
    }
}

open class FolderViewFolder(
    root: FolderView,
    viewModelItem: FolderViewModelItem,
    name: String = "",
    folderIndentation: Int = 0,
    itemHeightParam: Int = 20,
    itemTopPad: Int = 0,
    marketplaceItem: Boolean = false,
    allowDrop: Boolean = true,
    fontColor: Color4 = Color4(1f, 1f, 1f, 1f),
    fontHighlightColor: Color4 = Color4(1f, 1f, 1f, 1f),
    leftPad: Int = 0,
    iconPad: Int = 0,
    iconWidth: Int = 0,
    textPad: Int = 0,
    textPadRight: Int = 0,
    arrowSize: Int = 0,
    maxFolderItemOverlap: Int = 0,
    singleFolderMode: Boolean = false,
    doubleClickOverride: Boolean = false,
    forInventory: Boolean = false,
) : FolderViewItem(
    root, viewModelItem, name, folderIndentation, itemHeightParam, itemTopPad,
    marketplaceItem, allowDrop, fontColor, fontHighlightColor, leftPad, iconPad, iconWidth,
    textPad, textPadRight, arrowSize, maxFolderItemOverlap, singleFolderMode, doubleClickOverride,
    forInventory,
) {
    enum class RecurseType { NONE, UP, DOWN, UP_DOWN }

    val items: MutableList<FolderViewItem> = mutableListOf()
    val folders: MutableList<FolderViewFolder> = mutableListOf()

    var isOpenState: Boolean = false
    var expanderHighlighted: Boolean = false
    var curHeight: Float = 0f
    var targetHeight: Float = 0f
    var autoOpenCountdown: Float = 0f
    var lastArrangeGeneration: Int = -1
    var lastCalculatedWidth: Int = 0
    var isFolderCompleteState: Boolean = false
    var areChildrenInitedState: Boolean = false
    private var favoritesDirtyFlags: Int = 0

    override fun isFolderComplete(): Boolean = isFolderCompleteState
    override fun areChildrenInited(): Boolean = areChildrenInitedState
    override fun setChildrenInited(inited: Boolean) { areChildrenInitedState = inited }
    override fun isOpen(): Boolean = isOpenState

    fun hasFavorites(): Boolean = hasFavorites
    fun setHasFavorites(v: Boolean) { hasFavorites = v }

    fun updateHasFavorites(newChildValue: Boolean) {
        TODO("APR: use JVM equivalent for idle-callback-based favorites dirty flag update")
    }

    private fun updateLabelRotation() {
        controlLabelRotation = when {
            autoOpenCountdown != 0f -> autoOpenCountdown * -90f
            isOpenState -> lerpFloat(controlLabelRotation, -90f, FOLDER_OPEN_TIME_CONSTANT)
            else -> lerpFloat(controlLabelRotation, 0f, FOLDER_CLOSE_TIME_CONSTANT)
        }
    }

    override fun addToFolder(folder: FolderViewFolder) {
        folder.addFolder(this)
        indentation = parentFolder?.let { it.indentation + localIndentation } ?: 0
        if (isOpenState && folder.isOpenState) requestArrange()
    }

    override fun arrange(width: IntArray, height: IntArray): Int {
        if (areChildrenInitedState) root.getFolderViewModel().sort(this)

        hasVisibleChildren = false
        if (areChildrenInitedState && viewModelItem.descendantsPassedFilter()) {
            hasVisibleChildren = items.any { it.isPotentiallyVisible() } ||
                folders.any { it.isPotentiallyVisible() }
        }

        if (!isFolderCompleteState && areChildrenInitedState) {
            isFolderCompleteState = root.getFolderViewModel().isFolderComplete(this)
        }

        super.arrange(width, height)

        curHeight = max(height[0].toFloat(), curHeight)
        var runningHeight = height[0].toFloat()
        var targetH = height[0].toFloat()

        if (needsArrange()) {
            lastArrangeGeneration = root.getArrangeGeneration()
            if (isOpenState) {
                for (folder in folders) {
                    folder.visible = folder.isPotentiallyVisible()
                    if (folder.visible) {
                        val cw = intArrayOf(width[0]); val ch = intArrayOf(0)
                        targetH += folder.arrange(cw, ch)
                        runningHeight += ch[0]
                        width[0] = max(width[0], cw[0])
                    }
                }
                for (item in items) {
                    item.visible = item.isPotentiallyVisible()
                    if (item.visible) {
                        val cw = intArrayOf(width[0]); val ch = intArrayOf(0)
                        targetH += item.arrange(cw, ch)
                        runningHeight += ch[0]
                        width[0] = max(width[0], cw[0])
                    }
                }
            }
            targetHeight = targetH
            lastCalculatedWidth = width[0]
        } else {
            width[0] = lastCalculatedWidth
        }

        if (abs(curHeight - targetHeight) > 1f) {
            curHeight = lerpFloat(
                curHeight, targetHeight,
                if (isOpenState) FOLDER_OPEN_TIME_CONSTANT else FOLDER_CLOSE_TIME_CONSTANT
            )
            requestArrange()
        }

        height[0] = curHeight.toInt()
        return height[0]
    }

    fun needsArrange(): Boolean = lastArrangeGeneration < root.getArrangeGeneration()

    override fun setSelection(selection: FolderViewItem, openItem: Boolean, takeKeyboardFocus: Boolean): Boolean {
        var result = super.setSelection(selection, openItem, takeKeyboardFocus)
        if (openItem && selection !== this) setOpen(true)
        for (f in folders) result = result || f.setSelection(selection, openItem, takeKeyboardFocus)
        for (item in items) result = result || item.setSelection(selection, openItem, takeKeyboardFocus)
        return result
    }

    override fun changeSelection(selection: FolderViewItem, selected: Boolean): Boolean {
        var changed = super.changeSelection(selection, selected)
        for (f in folders) changed = changed || f.changeSelection(selection, selected)
        for (item in items) changed = changed || item.changeSelection(selection, selected)
        return changed
    }

    fun extendSelectionTo(selection: FolderViewItem) {
        TODO("APR: use JVM equivalent for range-selection from current selection to target item")
    }

    override fun isRemovable(): Boolean =
        viewModelItem.isItemRemovable() &&
            folders.all { it.isRemovable() } &&
            items.all { it.isRemovable() }

    override fun isMovable(): Boolean =
        viewModelItem.isItemMovable() &&
            folders.all { it.isMovable() } &&
            items.all { it.isMovable() }

    override fun destroyView() {
        for (f in folders.toList()) f.destroyView()
        for (i in items.toList()) i.destroyView()
        gFocusMgr.releaseFocusIfNeeded(TODO("APR: wrap FolderViewFolder as FocusableElement for gFocusMgr"))
        super.destroyView()
    }

    fun destroyRoot() {
        TODO("APR: use JVM equivalent for root folder cleanup")
    }

    open fun extractItem(item: FolderViewItem, deparentModel: Boolean = true) {
        items.remove(item)
        folders.remove(item as? FolderViewFolder)
        item.parentFolder = null
    }

    fun isInSelection(): Boolean = isSelected || (parentFolder?.isInSelection() ?: false)

    fun setAutoOpenCountdown(countdown: Float) { autoOpenCountdown = countdown }

    open fun toggleOpen() { setOpen(!isOpenState) }

    override fun setOpen(open: Boolean) {
        if (isOpenState != open) {
            isOpenState = open
            requestArrange()
        }
    }

    open fun requestArrange() {
        lastArrangeGeneration = root.getArrangeGeneration() - 1
        parentFolder?.requestArrange()
    }

    fun setOpenArrangeRecursively(openItem: Boolean, recurse: RecurseType = RecurseType.NONE) {
        if (recurse == RecurseType.DOWN || recurse == RecurseType.UP_DOWN) {
            for (f in folders) f.setOpenArrangeRecursively(openItem, RecurseType.DOWN)
        }
        setOpen(openItem)
        if (recurse == RecurseType.UP || recurse == RecurseType.UP_DOWN) {
            parentFolder?.setOpenArrangeRecursively(openItem, RecurseType.UP)
        }
    }

    fun applyFunctorToChildren(functor: FolderViewFunctor) {
        for (f in folders) functor.doFolder(f)
        for (item in items) functor.doItem(item)
    }

    fun applyFunctorRecursively(functor: FolderViewFunctor) {
        applyFunctorToChildren(functor)
        for (f in folders) f.applyFunctorRecursively(functor)
    }

    override fun openItem() { setOpen(!isOpenState) }

    fun addItem(item: FolderViewItem) { item.parentFolder = this; items.add(item) }
    fun addFolder(folder: FolderViewFolder) { folder.parentFolder = this; folders.add(folder) }

    fun getNextFromChild(child: FolderViewItem, includeChildren: Boolean = true): FolderViewItem? {
        if (includeChildren && child is FolderViewFolder && child.isOpenState && child.hasVisibleChildren) {
            child.folders.firstOrNull()?.let { return it }
            child.items.firstOrNull()?.let { return it }
        }
        val all: List<FolderViewItem> = folders + items
        val idx = all.indexOf(child)
        if (idx >= 0 && idx + 1 < all.size) return all[idx + 1]
        return parentFolder?.getNextFromChild(this, false)
    }

    fun getPreviousFromChild(child: FolderViewItem, includeChildren: Boolean = true): FolderViewItem? {
        val all: List<FolderViewItem> = folders + items
        val idx = all.indexOf(child)
        if (idx > 0) {
            val prev = all[idx - 1]
            if (includeChildren && prev is FolderViewFolder && prev.isOpenState) {
                return prev.items.lastOrNull() ?: prev.folders.lastOrNull() ?: prev
            }
            return prev
        }
        return this
    }

    fun getCommonAncestor(
        itemA: FolderViewItem, itemB: FolderViewItem, reverse: BooleanArray,
    ): FolderViewFolder {
        TODO("APR: use JVM equivalent for walking ancestry chains to find common ancestor")
    }

    fun gatherChildRangeExclusive(
        start: FolderViewItem, end: FolderViewItem,
        reverse: Boolean, outItems: MutableList<FolderViewItem>,
    ) {
        TODO("APR: use JVM equivalent for collecting items in range between start and end")
    }

    fun handleDragAndDropFromChild(
        mask: Int, drop: Boolean, cargoType: DragAndDropType, cargoData: Any?,
        accept: IntArray, tooltipMsg: StringBuilder,
    ): Boolean = handleDragAndDropToThisFolder(mask, drop, cargoType, cargoData, accept, tooltipMsg)

    fun handleDragAndDropToThisFolder(
        mask: Int, drop: Boolean, cargoType: DragAndDropType, cargoData: Any?,
        accept: IntArray, tooltipMsg: StringBuilder,
    ): Boolean {
        TODO("APR: use JVM equivalent for folder drag-and-drop via view model")
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        isMouseOverTitle = (y > rect.height - itemHeight)
        if (autoOpenCountdown != 0f) {
            TODO("APR: use JVM equivalent for auto-open timer countdown during drag hover")
        }
        return super.handleHover(x, y, mask)
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        updateLabelRotation()
        return super.handleMouseDown(x, y, mask)
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        if (doubleClickOverride) { viewModelItem.openItem(); return true }
        toggleOpen()
        return true
    }

    override fun draw() {
        updateLabelRotation()
        super.draw()
        if (isOpenState) {
            for (f in folders) if (f.visible) f.draw()
            for (item in items) if (item.visible) item.draw()
        }
    }
}

abstract class FolderView : FolderViewFolder(
    root = TODO("FolderView is its own root; subclass must break this circularity"),
    viewModelItem = TODO("FolderView subclass must supply its own FolderViewModelItem"),
) {
    abstract fun removeFromSelectionList(item: FolderViewItem)
    abstract fun setSelection(item: FolderViewItem, openItem: Boolean, takeKeyboardFocus: Boolean = false)
    abstract fun changeSelection(item: FolderViewItem, selected: Boolean)
    abstract fun scrollToShowSelection()
    abstract fun getFolderViewModel(): FolderViewModelInterface
    abstract fun getShowSelectionContext(): Boolean
    abstract fun getParentPanel(): UICtrl
    abstract fun useLabelSuffix(): Boolean
    abstract fun getUseEllipses(): Boolean
    abstract fun getArrangeGeneration(): Int
    abstract fun setDraggingOverItem(item: FolderViewItem?)
    abstract fun getHoveredItem(): FolderViewItem?
    abstract fun clearHoveredItem()
    abstract fun setHoveredItem(item: FolderViewItem)
    abstract fun setShowSelectionContext(show: Boolean)
    abstract fun autoOpenTest(item: FolderViewItem?)
    abstract fun getAllowDrag(): Boolean
    abstract fun getCurSelectedItem(): FolderViewItem?
    abstract fun startDrag(): Boolean
    abstract fun isOverDragThreshold(screenX: Int, screenY: Int): Boolean
    abstract fun setDragStart(screenX: Int, screenY: Int)
    abstract fun getSelectionFadeElapsedTime(): Float
    abstract fun getShowSingleSelection(): Boolean
    abstract fun showItemLinkOverlays(): Boolean
    abstract fun getScrollContainer(): Any?
}

interface FolderViewFunctor {
    fun doItem(item: FolderViewItem)
    fun doFolder(folder: FolderViewFolder)
}

interface FolderViewGroupedItemModel {
    fun groupFilterContextMenu(selectedItems: ArrayDeque<FolderViewItem>, menu: MenuGL)
}
