package com.firestorm.newview

import java.util.UUID

private const val DROP_DOWN_MENU_WIDTH = 250
private const val DROP_DOWN_MENU_TOP_PAD = 13
private const val MAX_LANDMARK_REQUEST_RETRIES = 10

open class FavoritesBarCtrl : UICtrl(), InventoryObserver {

    private var mOverflowMenuHandle: ViewHandle? = null
    private var mContextMenuHandle: ViewHandle? = null

    private var mFavoriteFolderId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var mFont: Font? = null
    private var mFirstDropDownItem: Int = 0
    private var mDropDownItemsCount: Int = 0
    private var mUpdateDropDownItems: Boolean = true
    private var mRestoreOverflowMenu: Boolean = false
    private var mDragToOverflowMenu: Boolean = false
    private var mGetPrevItems: Boolean = true

    private var mSelectedItemId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val mItemsChangedTimer: FrameTimer = FrameTimer()
    private var mImageDragIndication: UIImage? = null

    private var mShowDragMarker: Boolean = false
    private var mLandingTab: UICtrl? = null
    private var mLastTab: UICtrl? = null
    private var mMoreCtrl: UICtrl? = null
    private var mBarLabel: TextBox? = null

    private var mDragItemId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var mStartDrag: Boolean = false
    private val mItems: MutableList<ViewerInventoryItem> = mutableListOf()

    private var mItemsListDirty: Boolean = false
    private var mMouseX: Int = 0
    private var mMouseY: Int = 0

    private var mEndDragConnection: (() -> Unit)? = null

    companion object {
        var sWaitingForCallback: Double = 0.0
    }

    init {
        Inventory.global.addObserver(this)
    }

    fun destroy() {
        Inventory.global.removeObserver(this)
        mOverflowMenuHandle?.get()?.die()
        mContextMenuHandle?.get()?.die()
    }

    open fun postBuild(): Boolean {
        val menu = UICtrlFactory.createMenuFromFile("menu_favorites.xml")
            ?: UICtrlFactory.getDefaultWidget("inventory_menu")
        menu?.setBackgroundColor(UIColorTable.instance.getColor("MenuPopupBgColor"))
        mContextMenuHandle = menu?.getHandle()
        return true
    }

    open fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: AcceptanceHolder, tooltipMsg: StringBuilder
    ): Boolean {
        accept.value = Acceptance.NO

        val source = ToolDragAndDrop.instance.getSource()
        if (source != DragSource.AGENT && source != DragSource.LIBRARY) return false

        when (cargoType) {
            DragAndDropType.LANDMARK -> {
                if (mEndDragConnection == null) {
                    mEndDragConnection = ToolDragAndDrop.instance.setEndDragCallback { onEndDrag() }
                }

                val item = cargoData as? InventoryItem ?: return true

                if (mDragToOverflowMenu) {
                    val overflowMenu = mOverflowMenuHandle?.get()
                    if (overflowMenu != null && !overflowMenu.isDead() && overflowMenu.isVisible()) {
                        overflowMenu.handleHover(x, y, mask)
                    }
                } else {
                    val destButton = findChildByLocalCoords(x, y) as? FavoriteLandmarkButton
                    if (destButton != null) {
                        setLandingTab(destButton)
                    } else {
                        val overflowMenu = mOverflowMenuHandle?.get()
                        if (mMoreCtrl != null && mMoreCtrl!!.isVisible() &&
                            mMoreCtrl!!.getRect().contains(x, y)
                        ) {
                            if (overflowMenu == null || overflowMenu.isDead() || !overflowMenu.isVisible()) {
                                showDropDownMenu()
                            }
                        }
                        if (mLastTab != null && x >= mLastTab!!.getRect().right) {
                            setLandingTab(null)
                        }
                    }
                }

                var existingItem = false
                if (mDragItemId == item.uuid) {
                    existingItem = mItems.any { it.uuid == mDragItemId }
                }

                if (existingItem) {
                    accept.value = Acceptance.YES_SINGLE
                    showDragMarker(true)
                    if (drop) handleExistingFavoriteDragAndDrop(x, y)
                } else {
                    val favoritesId = Inventory.global.findCategoryUUIDForType(FolderType.FAVORITE)
                    if (item.parentUUID == favoritesId) return true

                    accept.value = Acceptance.YES_COPY_MULTI
                    showDragMarker(true)
                    if (drop) {
                        if (mItems.isEmpty()) {
                            setLandingTab(null)
                            mLastTab = null
                        }
                        handleNewFavoriteDragAndDrop(item, favoritesId, x, y)
                    }
                }
            }
            else -> Unit
        }
        return true
    }

    fun handleDragAndDropToMenu(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: AcceptanceHolder, tooltipMsg: StringBuilder
    ): Boolean {
        mDragToOverflowMenu = true
        val handled = handleDragAndDrop(x, y, mask, drop, cargoType, cargoData, accept, tooltipMsg)
        mDragToOverflowMenu = false
        return handled
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (mDragItemId != UUID.fromString("00000000-0000-0000-0000-000000000000") && mStartDrag) {
            val (screenX, screenY) = localPointToScreen(x, y)
            if (ToolDragAndDrop.instance.isOverThreshold(screenX, screenY)) {
                ToolDragAndDrop.instance.beginDrag(DragAndDropType.LANDMARK, mDragItemId, DragSource.LIBRARY)
                mStartDrag = false
                return ToolDragAndDrop.instance.handleHover(x, y, mask)
            }
        }
        return true
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val handled = childrenHandleRightMouseDown(x, y, mask)
        if (!handled && !MenuHolder.instance.hasVisibleMenu()) {
            showNavbarContextMenu(this, x, y)
            return true
        }
        return handled
    }

    override fun changed(mask: UInt) {
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (mFavoriteFolderId == nullId) {
            mFavoriteFolderId = Inventory.global.findCategoryUUIDForType(FolderType.FAVORITE)
            if (mFavoriteFolderId != nullId) {
                Inventory.global.fetchDescendentsOf(mFavoriteFolderId)
            }
        } else {
            val items = mutableListOf<ViewerInventoryItem>()
            Inventory.global.collectDescendentsIf(
                mFavoriteFolderId,
                categories = mutableListOf(),
                items = items,
                excludeTrash = true,
                functor = IsLandmarkType()
            )
            items.forEach { FavoritesOrderStorage.getSLURL(it.assetUUID) }

            if (sWaitingForCallback < Timer.getTotalSeconds()) {
                updateButtons()
                if (!mItemsChangedTimer.isStarted()) mItemsChangedTimer.start()
                else mItemsChangedTimer.reset()
            } else {
                mItemsListDirty = true
            }
        }
    }

    open fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        val deltaWidth = width - getRect().width
        val deltaHeight = height - getRect().height
        val forceUpdate = deltaWidth != 0 || deltaHeight != 0
        super.reshape(width, height, calledFromParent)
        updateButtons(forceUpdate)
    }

    override fun draw() {
        super.draw()

        if (mShowDragMarker) {
            val img = mImageDragIndication
            if (img != null) {
                val w = img.width
                val h = img.height
                if (mLandingTab != null) {
                    val rect = mLandingTab!!.getRect()
                    // no-op
                } else if (mLastTab != null) {
                    val rect = mLastTab!!.getRect()
                    // no-op
                }
            }
            mShowDragMarker = false
        }

        if (mItemsChangedTimer.isStarted()) {
            if (mItemsChangedTimer.getElapsedTimeF32() > 1f) {
                FavoritesOrderStorage.saveFavoritesRecord()
                mItemsChangedTimer.stop()
            }
        }

        if (!mItemsChangedTimer.isStarted() && FavoritesOrderStorage.mUpdateRequired) {
            FavoritesOrderStorage.mUpdateRequired = false
            mItemsChangedTimer.start()
        }

        if (mItemsListDirty && sWaitingForCallback < Timer.getTotalSeconds()) {
            updateButtons()
            if (!mItemsChangedTimer.isStarted()) mItemsChangedTimer.start()
            else mItemsChangedTimer.reset()
        }
    }

    fun showDragMarker(show: Boolean) { mShowDragMarker = show }
    fun setLandingTab(tab: UICtrl?) { mLandingTab = tab }

    protected open fun updateButtons(forceUpdate: Boolean = false) {
        if (App.isExiting()) return

        mItemsListDirty = false
        mItems.clear()
        if (!collectFavoriteItems(mItems)) return

        if (mGetPrevItems && Inventory.global.isCategoryComplete(mFavoriteFolderId)) {
            mItems.forEach { FavoritesOrderStorage.mFavoriteNames[it.uuid] = it.name }
            FavoritesOrderStorage.mPrevFavorites = mItems.toMutableList()
            mGetPrevItems = false
            if (FavoritesOrderStorage.isStorageUpdateNeeded()) {
                if (!mItemsChangedTimer.isStarted()) mItemsChangedTimer.start()
            }
        }

        if (mItems.isEmpty()) {
            mBarLabel?.setVisible(true)
            mLastTab = null
        } else {
            mBarLabel?.setVisible(false)
        }

        val children = getChildList()
        var firstChangedIndex = 0
        var childIt = children.iterator()

        if (!forceUpdate) {
            var idx = 0
            for (child in children) {
                if (idx >= mItems.size) break
                val button = child as? FavoriteLandmarkButton ?: continue
                val item = mItems.getOrNull(idx) ?: break
                if (button.getLandmarkId() != item.uuid || button.getLabelSelected() != item.name) break
                firstChangedIndex++
                idx++
            }
        }

        if (firstChangedIndex <= mItems.size) {
            val toRemove = children.drop(firstChangedIndex).filterIsInstance<FavoriteLandmarkButton>()
            toRemove.forEach { btn ->
                if (mLastTab == btn) mLastTab = null
                removeChild(btn)
            }

            mMoreCtrl?.let { if (it.getParent() == this) removeChild(it) }

            var lastRightEdge = children.lastOrNull { it.isVisible() }?.getRect()?.right ?: 0
            var lastNewButton: Button? = null
            var j = firstChangedIndex

            while (j < mItems.size) {
                val btn = createButton(mItems[j], lastRightEdge) ?: break
                sendChildToBack(btn)
                lastRightEdge = btn.getRect().right
                mLastTab = btn
                lastNewButton = btn
                j++
            }

            if (mLastTab == null && mItems.isNotEmpty()) {
                mLastTab = children.lastOrNull { it is FavoriteLandmarkButton }
            }

            mFirstDropDownItem = j

            if (mFirstDropDownItem < mItems.size) {
                mUpdateDropDownItems = true
                val ctrl = mMoreCtrl ?: return
                val rect = ctrl.getRect()
                val translated = rect.translatedTo(getRect().right - rect.right, 0)
                addChild(ctrl)
                ctrl.setRect(translated)
                ctrl.setVisible(true)
            }

            val overflowMenu = mOverflowMenuHandle?.get() as? ToggleableMenu
            if (overflowMenu != null && overflowMenu.isVisible() &&
                overflowMenu.getItemCount() != mDropDownItemsCount
            ) {
                overflowMenu.setVisible(false)
                if (mUpdateDropDownItems) showDropDownMenu()
            }
        } else {
            mUpdateDropDownItems = false
        }
    }

    protected open fun createButton(item: ViewerInventoryItem, xOffset: Int): Button? {
        val defButtonWidth = buttonParams.width
        val buttonXDelta = buttonParams.leftPad
        val requiredWidth = (mFont?.getWidth(item.name) ?: 0) + 20
        val width = minOf(requiredWidth, defButtonWidth)

        if (xOffset + width + 2 * buttonXDelta + (mMoreCtrl?.getRect()?.width ?: 0) > getRect().right) {
            return null
        }

        val favBtn = FavoriteLandmarkButton()
        addChild(favBtn)

        val newRect = Rect(
            left = xOffset + buttonXDelta,
            bottom = favBtn.getRect().bottom,
            width = width,
            height = favBtn.getRect().height
        )
        favBtn.setLandmarkId(item.uuid)
        favBtn.setRect(newRect)
        favBtn.setFont(mFont)
        favBtn.setLabel(item.name)
        favBtn.setToolTip(item.name)
        favBtn.setCommitCallback { onButtonClick(item.uuid) }
        favBtn.setRightMouseDownCallback { view, x, y, mask -> onButtonRightClick(item.uuid, view, x, y, mask) }
        favBtn.setMouseDownCallback { ctrl, x, y, mask -> onButtonMouseDown(item.uuid, ctrl, x, y, mask) }
        favBtn.setMouseUpCallback { ctrl, x, y, mask -> onButtonMouseUp(item.uuid, ctrl, x, y, mask) }
        return favBtn
    }

    protected open fun collectFavoriteItems(items: MutableList<ViewerInventoryItem>): Boolean {
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (mFavoriteFolderId == nullId) return false

        Inventory.global.collectDescendentsIf(
            mFavoriteFolderId,
            categories = mutableListOf(),
            items = items,
            excludeTrash = true,
            functor = IsLandmarkType()
        )
        items.sortWith(FavoritesSort())

        if (needToSaveItemsOrder(items)) {
            var sortField = 0
            items.forEach { FavoritesOrderStorage.setSortIndex(it, ++sortField) }
            FavoritesOrderStorage.mSaveOnExit = true
        }
        return true
    }

    protected open fun onButtonClick(id: UUID) {
        InventoryBridgeAction.doAction(id, Inventory.global)
    }

    protected open fun onButtonRightClick(id: UUID, button: View?, x: Int, y: Int, mask: Int) {
        mSelectedItemId = id
        val menu = mContextMenuHandle?.get() as? MenuGL ?: return

        val overflowMenu = mOverflowMenuHandle?.get()
        mRestoreOverflowMenu = overflowMenu?.isVisible() == true

        FocusMgr.instance.setMouseCapture(null)
        menu.updateParent(MenuGL.menuContainer)
        MenuGL.showPopup(button, menu, x, y)
    }

    protected open fun onButtonMouseDown(id: UUID, ctrl: UICtrl?, x: Int, y: Int, mask: Int) {
        val menu = mContextMenuHandle?.get() as? MenuGL
        if (menu?.isVisible() == true) menu.setVisible(false)

        mDragItemId = id
        mStartDrag = true
        val (screenX, screenY) = localPointToScreen(x, y)
        ToolDragAndDrop.instance.setDragStart(screenX, screenY)
    }

    protected open fun onButtonMouseUp(id: UUID, ctrl: UICtrl?, x: Int, y: Int, mask: Int) {
        mStartDrag = false
        mDragItemId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    protected open fun onEndDrag() {
        mEndDragConnection = null
        showDragMarker(false)
        mDragItemId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        System.err.println("FavoritesBarCtrl: setCursor(UI_CURSOR_ARROW) not yet implemented")
    }

    protected open fun onMoreTextBoxClicked() {
        val (mx, my) = LLUI.instance.getMousePositionScreen()
        mMouseX = mx
        mMouseY = my
        showDropDownMenu()
    }

    protected open fun showDropDownMenu() {
        if (mOverflowMenuHandle?.isDead() != false) {
            createOverflowMenu()
        }
        val menu = mOverflowMenuHandle?.get() as? ToggleableMenu ?: return
        if (menu.toggleVisibility()) {
            if (mUpdateDropDownItems) updateOverflowMenuItems()
            else menu.buildDrawLabels()
            menu.updateParent(MenuGL.menuContainer)
            mMoreCtrl?.getRect()?.let { menu.setButtonRect(it, this) }
            positionAndShowOverflowMenu()
        }
    }

    protected open fun createOverflowMenu() {
        val menu = FavoriteLandmarkToggleableMenu(
            name = "favorites menu",
            canTearOff = false,
            scrollable = true,
            maxScrollableItems = 10,
            preferredWidth = DROP_DOWN_MENU_WIDTH
        )
        menu.setToolbar(this)
        mOverflowMenuHandle = menu.getHandle()
    }

    protected open fun updateOverflowMenuItems() {
        val menu = mOverflowMenuHandle?.get() as? ToggleableMenu ?: return
        menu.empty()

        for (i in mFirstDropDownItem until mItems.size) {
            val item = mItems[i]
            val menuItem = FavoriteLandmarkMenuItem(
                name = item.name,
                label = item.name,
                onClick = { onButtonClick(item.uuid) }
            )
            menuItem.initFavoritesBarPointer(this)
            menuItem.setRightMouseDownCallback { view, x, y, mask ->
                onButtonRightClick(item.uuid, view, x, y, mask)
            }
            menuItem.setMouseDownCallback { ctrl, x, y, mask ->
                onButtonMouseDown(item.uuid, ctrl, x, y, mask)
            }
            menuItem.setMouseUpCallback { ctrl, x, y, mask ->
                onButtonMouseUp(item.uuid, ctrl, x, y, mask)
            }
            menuItem.setLandmarkId(item.uuid)
            fitLabelWidth(menuItem)
            menu.addChild(menuItem)
        }

        menu.buildDrawLabels()
        mDropDownItemsCount = menu.getItemCount()
        addOpenLandmarksMenuItem(menu)
        mUpdateDropDownItems = false
    }

    protected open fun fitLabelWidth(menuItem: MenuItemCallGL) {
        val maxWidth = minOf(DROP_DOWN_MENU_WIDTH, getRect().width)
        val itemName = menuItem.getName()
        if (menuItem.getNominalWidth() > maxWidth) {
            val totalChars = itemName.length
            var charsFitted = 1
            menuItem.setLabel("")
            val labelSpace = maxWidth - (menuItem.getFont()?.getWidth("...") ?: 0) - menuItem.getNominalWidth()
            while (charsFitted < totalChars &&
                (menuItem.getFont()?.getWidth(itemName, 0, charsFitted) ?: 0) < labelSpace
            ) {
                charsFitted++
            }
            charsFitted--
            menuItem.setLabel(itemName.substring(0, charsFitted) + "...")
        }
    }

    protected open fun addOpenLandmarksMenuItem(menu: ToggleableMenu) {
        val label = Trans.findString("Open landmarks") ?: "Open landmarks"
        val menuItem = MenuItemCallGL(
            name = "open_my_landmarks",
            label = label,
            onClick = {
                FloaterSidePanelContainer.showPanel("places", mapOf("type" to "open_landmark_tab"))
            }
        )
        fitLabelWidth(menuItem)
        menu.addChild(MenuItemSeparatorGL())
        menu.addChild(menuItem)
    }

    protected open fun positionAndShowOverflowMenu() {
        val menu = mOverflowMenuHandle?.get() as? ToggleableMenu ?: return
        val maxWidth = minOf(DROP_DOWN_MENU_WIDTH, getRect().width)
        var menuX = getRect().width - maxWidth
        val menuY = (getParent()?.getRect()?.bottom ?: 0) - DROP_DOWN_MENU_TOP_PAD

        val rightToolbar = ToolBarView.instance?.getChild<ToolBar>("toolbar_right")
        if (rightToolbar != null && rightToolbar.hasButtons()) {
            val toolbarTop = rightToolbar.getChild<View>("button_panel")?.calcScreenRect()?.top ?: 0
            val menuTop = (getParent()?.getRect()?.bottom ?: 0) - DROP_DOWN_MENU_TOP_PAD
            val menuBottom = menuTop - menu.getRect().height
            val (_, menuBottomScreen) = localPointToScreen(0, menuBottom)
            if (menuBottomScreen < toolbarTop) {
                menuX -= rightToolbar.getRect().width
            }
        }

        MenuGL.showPopup(this, menu, menuX, menuY, mMouseX, mMouseY)
    }

    protected open fun enableSelected(userdata: Any): Boolean {
        val param = userdata.toString()
        return when (param) {
            "can_paste" -> isClipboardPasteable()
            "create_pick" -> !AgentPicksInfo.instance.isPickLimitReached()
            "copy_slurl", "show_on_map" -> {
                val item = Inventory.global.getItem(mSelectedItemId) ?: return false
                Landmarks.getAsset(item.assetUUID) != null
            }
            else -> false
        }
    }

    protected open fun doToSelected(userdata: Any) {
        val action = userdata.toString()
        val item = Inventory.global.getItem(mSelectedItemId) ?: return

        when (action) {
            "open" -> onButtonClick(item.uuid)
            "about" -> {
                FSFloaterPlaceDetails.showPlaceDetails(
                    mapOf("type" to "landmark", "id" to mSelectedItemId)
                )
            }
            "copy_slurl" -> {
                val posGlobal = LandmarkActions.getLandmarkGlobalPos(mSelectedItemId)
                if (posGlobal != null && !posGlobal.isZero()) {
                    LandmarkActions.getSLURLfromPosGlobal(posGlobal) { slurl ->
                        copySLURLToClipboard(slurl)
                    }
                } else {
                    NotificationsUtil.add("LandmarkLocationUnknown")
                }
            }
            "show_on_map" -> {
                val posGlobal = LandmarkActions.getLandmarkGlobalPos(mSelectedItemId)
                if (posGlobal != null && !posGlobal.isZero()) {
                    FloaterWorldMap.instance?.trackLocation(posGlobal)
                    FloaterReg.showInstance("world_map", "center")
                } else {
                    NotificationsUtil.add("LandmarkLocationUnknown")
                }
            }
            "create_pick" -> {
                FloaterSidePanelContainer.showPanel(
                    "places",
                    mapOf("type" to "create_pick", "item_id" to item.uuid)
                )
            }
            "cut" -> Unit
            "copy" -> Clipboard.instance.copyToClipboard(mSelectedItemId, AssetType.LANDMARK)
            "paste" -> pasteFromClipboard()
            "delete" -> Inventory.global.removeItem(mSelectedItemId)
            "rename" -> {
                NotificationsUtil.add(
                    "RenameLandmark",
                    args = mapOf("NAME" to item.name),
                    payload = mapOf("id" to mSelectedItemId)
                ) { notification, response ->
                    onRenameCommit(notification, response)
                }
            }
            "move_to_landmarks" -> {
                changeItemParent(
                    mSelectedItemId,
                    Inventory.global.findCategoryUUIDForType(FolderType.LANDMARK)
                )
            }
        }

        val menu = mOverflowMenuHandle?.get() as? ToggleableMenu
        if (mRestoreOverflowMenu && menu?.isVisible() == false) {
            menu.resetScrollPositionOnShow(false)
            showDropDownMenu()
            menu.resetScrollPositionOnShow(true)
        }
    }

    private fun onRenameCommit(notification: Any, response: Any): Boolean {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) {
            val id = NotificationsUtil.getPayloadUUID(notification, "id")
            val item = Inventory.global.getItem(id) ?: return false
            val landmarkName = NotificationsUtil.getResponseString(response, "new_name").trim()
            if (landmarkName.isNotEmpty() && item.name != landmarkName) {
                val newItem = ViewerInventoryItem(item).apply { rename(landmarkName) }
                newItem.updateServer(false)
                Inventory.global.updateItem(newItem)
            }
        }
        return false
    }

    private fun isClipboardPasteable(): Boolean {
        if (!Clipboard.instance.hasContents()) return false
        val objects = Clipboard.instance.pasteFromClipboard()
        for (itemId in objects) {
            if (Inventory.global.getCategory(itemId) != null) return false
            val item = Inventory.global.getItem(itemId)
            if (item != null && item.type != AssetType.LANDMARK) return false
        }
        return true
    }

    private fun pasteFromClipboard() {
        if (!isClipboardPasteable()) return
        val objects = Clipboard.instance.pasteFromClipboard()
        for (objId in objects) {
            val item = Inventory.global.getItem(objId) ?: continue
            System.err.println("FavoritesBarCtrl: copy_inventory_item to mFavoriteFolderId not yet implemented")
        }
    }

    private fun handleExistingFavoriteDragAndDrop(x: Int, y: Int) {
        if (mItems.isEmpty()) return
        val (targetId, insertBefore) = findDragAndDropTarget(x, y) ?: return
        if (targetId == mDragItemId) return
        Inventory.global.updateItemsOrder(mItems, mDragItemId, targetId, insertBefore)
        FavoritesOrderStorage.saveItemsOrder(mItems)

        val menu = mOverflowMenuHandle?.get()
        if (menu != null && !menu.isDead() && menu.isVisible()) {
            updateOverflowMenuItems()
            positionAndShowOverflowMenu()
        }
    }

    private fun handleNewFavoriteDragAndDrop(item: InventoryItem, favoritesId: UUID, x: Int, y: Int) {
        val (targetId, insertBefore) = findDragAndDropTarget(x, y) ?: Pair(null, false)
        if (targetId != null && targetId == mDragItemId) return

        val viewerItem = ViewerInventoryItem(item)
        if (targetId != null) {
            insertItem(mItems, targetId, viewerItem, insertBefore)
        } else {
            mItems.add(viewerItem)
        }

        var sortField = 0
        val callbackItem = mItems.firstOrNull { it.uuid == item.uuid }

        val callbackWait = Timer.getTotalSeconds() + 30.0
        sWaitingForCallback = callbackWait

        for (curr in mItems) {
            if (curr.uuid == item.uuid) {
                sortField++
            } else {
                FavoritesOrderStorage.setSortIndex(curr, ++sortField)
                curr.setComplete(true)
                curr.updateServer(false)
                Inventory.global.updateItem(curr)
            }
        }

        System.err.println("FavoritesBarCtrl: copy_inventory_item / copy_inventory_from_notecard with sortField=$sortField callback not yet implemented")

        updateButtons()
        val overflowMenu = mOverflowMenuHandle?.get()
        if (overflowMenu != null && !overflowMenu.isDead() && overflowMenu.isVisible()) {
            updateOverflowMenuItems()
            positionAndShowOverflowMenu()
        }
    }

    private fun findDragAndDropTarget(x: Int, y: Int): Pair<UUID, Boolean>? {
        if (mItems.isEmpty()) return null

        if (mDragToOverflowMenu) {
            val overflowMenu = mOverflowMenuHandle?.get() ?: return null
            if (overflowMenu.isDead() || !overflowMenu.isVisible()) return null
            val targetItem = overflowMenu.childFromPoint(x, y) as? FavoriteLandmarkMenuItem
            return if (targetItem != null) {
                Pair(targetItem.getLandmarkId(), true)
            } else {
                val bottomItem = overflowMenu.getChildList()
                    .filterIsInstance<FavoriteLandmarkMenuItem>()
                    .firstOrNull() ?: return null
                Pair(bottomItem.getLandmarkId(), false)
            }
        } else {
            val hoveredButton = mLandingTab as? FavoriteLandmarkButton
            return if (hoveredButton != null) {
                Pair(hoveredButton.getLandmarkId(), true)
            } else {
                val lastButton = mLastTab as? FavoriteLandmarkButton ?: return null
                Pair(lastButton.getLandmarkId(), false)
            }
        }
    }

    private fun findChildByLocalCoords(x: Int, y: Int): UICtrl? {
        for (child in getChildList()) {
            if (child.getName() == "favorites_bar_btn") {
                if (x <= child.getRect().right) return child as? UICtrl
            }
        }
        return null
    }

    private fun needToSaveItemsOrder(items: List<ViewerInventoryItem>): Boolean {
        return items.any { FavoritesOrderStorage.getSortIndex(it.uuid) < 0 }
    }

    private fun insertItem(
        items: MutableList<ViewerInventoryItem>,
        destItemId: UUID,
        insertedItem: ViewerInventoryItem,
        insertBefore: Boolean
    ) {
        val idx = items.indexOfFirst { it.uuid == destItemId }
        if (idx < 0) return
        val insertAt = if (insertBefore) idx else idx + 1
        if (insertAt < items.size) items.add(insertAt, insertedItem)
        else items.add(insertedItem)
    }

    private val buttonParams: ButtonParams get() = ButtonParams.loadFromXml("favorites_bar_button.xml")
}

private fun copySLURLToClipboard(slurl: String) {
    if (slurl.isEmpty()) {
        NotificationsUtil.add("LandmarkLocationUnknown")
        return
    }
    Clipboard.instance.copyToClipboard(slurl)
    NotificationsUtil.add("CopySLURL", args = mapOf("SLURL" to slurl))
}

private class FavoritesSort : Comparator<ViewerInventoryItem> {
    override fun compare(a: ViewerInventoryItem, b: ViewerInventoryItem): Int {
        val sortA = FavoritesOrderStorage.getSortIndex(a.uuid)
        val sortB = FavoritesOrderStorage.getSortIndex(b.uuid)
        if (!(sortA < 0 && sortB < 0)) return sortB.compareTo(sortA)
        val createA = a.creationDate
        val createB = b.creationDate
        return if (createA == createB) a.name.compareTo(b.name)
        else createB.compareTo(createA)
    }
}

private class LandmarkInfoGetter {
    private var landmarkId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var name: String = "Loading..."
    private var posX: Int = 0
    private var posY: Int = 0
    private var posZ: Int = 0
    private var loaded: Boolean = false
    private var isLoading: Boolean = false
    private var isFailed: Boolean = false
    private var retries: Int = 0

    fun setLandmarkId(id: UUID) { landmarkId = id }
    fun getLandmarkId(): UUID = landmarkId

    fun getName(): String {
        if (!loaded && !isLoading && !isFailed) requestNameAndPos()
        return name
    }

    fun getPosX(): Int { if (!loaded && !isLoading && !isFailed) requestNameAndPos(); return posX }
    fun getPosY(): Int { if (!loaded && !isLoading && !isFailed) requestNameAndPos(); return posY }
    fun getPosZ(): Int { if (!loaded && !isLoading && !isFailed) requestNameAndPos(); return posZ }
    fun isLoaded(): Boolean = loaded
    fun isFailed(): Boolean = isFailed

    fun tick() {
        retries++
        if (retries <= MAX_LANDMARK_REQUEST_RETRIES) requestNameAndPos()
        else isFailed = true
    }

    private fun requestNameAndPos() {
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (landmarkId == nullId) return
        val gPos = LandmarkActions.getLandmarkGlobalPos(landmarkId) ?: run { isFailed = true; return }
        isLoading = true
        LandmarkActions.getRegionNameAndCoordsFromPosGlobal(gPos) { regionName, x, y, z ->
            name = regionName
            posX = x; posY = y; posZ = z
            loaded = true; isLoading = false; isFailed = false
        }
    }
}

private open class FavoriteLandmarkButton : Button() {
    private val landmarkInfoGetter = LandmarkInfoGetter()

    fun setLandmarkId(id: UUID) { landmarkInfoGetter.setLandmarkId(id) }
    fun getLandmarkId(): UUID = landmarkInfoGetter.getLandmarkId()

    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        val regionName = landmarkInfoGetter.getName()
        if (!landmarkInfoGetter.isFailed()) {
            val extra = if (landmarkInfoGetter.isLoaded())
                "$regionName (${landmarkInfoGetter.getPosX()}, ${landmarkInfoGetter.getPosY()}, ${landmarkInfoGetter.getPosZ()})"
            else regionName
            ToolTipMgr.instance.show(
                message = "${getLabelSelected()}\n$extra",
                maxWidth = 1000,
                stickyRect = calcScreenRect()
            )
        } else {
            ToolTipMgr.instance.show(message = getLabelSelected(), maxWidth = 1000, stickyRect = calcScreenRect())
        }
        return true
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        (getParent() as? FavoritesBarCtrl)?.handleHover(x, y, mask)
        return super.handleHover(x, y, mask)
    }

    override fun onMouseEnter(x: Int, y: Int, mask: Int) {
        if (ToolDragAndDrop.instance.hasMouseCapture()) super.onMouseEnterBase(x, y, mask)
        else super.onMouseEnter(x, y, mask)
    }
}

private open class FavoriteLandmarkMenuItem(
    name: String,
    label: String,
    onClick: () -> Unit
) : MenuItemCallGL(name = name, label = label, onClick = onClick) {

    private val landmarkInfoGetter = LandmarkInfoGetter()
    private var favoritesBar: FavoritesBarCtrl? = null

    fun setLandmarkId(id: UUID) { landmarkInfoGetter.setLandmarkId(id) }
    fun getLandmarkId(): UUID = landmarkInfoGetter.getLandmarkId()
    fun initFavoritesBarPointer(fb: FavoritesBarCtrl) { favoritesBar = fb }

    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        val regionName = landmarkInfoGetter.getName()
        when {
            landmarkInfoGetter.isLoaded() ->
                ToolTipMgr.instance.show(
                    message = "${getLabel()}\n$regionName (${landmarkInfoGetter.getPosX()}, ${landmarkInfoGetter.getPosY()})",
                    stickyRect = calcScreenRect()
                )
            landmarkInfoGetter.isFailed() ->
                ToolTipMgr.instance.show(message = getLabel(), stickyRect = calcScreenRect())
            else ->
                ToolTipMgr.instance.show(message = "${getLabel()}\n$regionName", stickyRect = calcScreenRect())
        }
        return true
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        favoritesBar?.handleHover(x, y, mask)
        return true
    }
}

private open class FavoriteLandmarkToggleableMenu(
    name: String,
    canTearOff: Boolean,
    scrollable: Boolean,
    maxScrollableItems: Int,
    preferredWidth: Int
) : ToggleableMenu(name = name, canTearOff = canTearOff, scrollable = scrollable,
    maxScrollableItems = maxScrollableItems, preferredWidth = preferredWidth) {

    private var mToolbar: FavoritesBarCtrl? = null
    private var mIsHovering: Boolean = false

    fun setToolbar(toolbar: FavoritesBarCtrl) { mToolbar = toolbar }

    override fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: AcceptanceHolder, tooltipMsg: StringBuilder
    ): Boolean {
        mToolbar?.handleDragAndDropToMenu(x, y, mask, drop, cargoType, cargoData, accept, tooltipMsg)
        return true
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        mIsHovering = true
        super.handleHover(x, y, mask)
        mIsHovering = false
        return true
    }

    override fun setVisible(visible: Boolean) {
        if (visible || !mIsHovering) super.setVisible(visible)
    }

    override fun destroy() {
        mIsHovering = false
        super.setVisible(false)
    }
}

object FavoritesOrderStorage {
    private const val SORTING_DATA_FILE_NAME = "landmarks_sorting.xml"
    val NO_INDEX = -1
    var mSaveOnExit: Boolean = false
    var mUpdateRequired: Boolean = false
    var mRecreateFavoriteStorage: Boolean = false

    val mPrevFavorites: MutableList<ViewerInventoryItem> = mutableListOf()
    var mStorageFavorites: Any? = null
    val mFavoriteNames: MutableMap<UUID, String> = mutableMapOf()

    private val mSortIndexes: MutableMap<UUID, Int> = mutableMapOf()
    private val mSLURLs: MutableMap<UUID, String> = mutableMapOf()
    private val mMissingSLURLs: MutableSet<UUID> = mutableSetOf()
    private var mIsDirty: Boolean = false

    init {
        load()
    }

    fun setSortIndex(invItem: ViewerInventoryItem, sortIndex: Int) {
        mSortIndexes[invItem.uuid] = sortIndex
        mIsDirty = true
        getSLURL(invItem.assetUUID)
    }

    fun getSortIndex(invItemId: UUID): Int = mSortIndexes[invItemId] ?: NO_INDEX

    fun removeSortIndex(invItemId: UUID) {
        mSortIndexes.remove(invItemId)
        mIsDirty = true
    }

    fun getSLURL(assetId: UUID) {
        if (mSLURLs.containsKey(assetId)) return
        System.err.println("FavoritesOrderStorage: load landmark and get SLURL for assetId=$assetId not yet implemented")
    }

    fun saveItemsOrder(items: List<ViewerInventoryItem>) {
        var sortField = 0
        for (item in items) {
            setSortIndex(item, ++sortField)
            item.setComplete(true)
            item.updateServer(false)
            Inventory.global.updateItem(item)
            Inventory.global.addChangedMask(InventoryObserver.SORT, item.parentUUID)
        }
        Inventory.global.notifyObservers()
    }

    fun saveOrder() {
        val items = mutableListOf<ViewerInventoryItem>()
        val favId = Inventory.global.findCategoryUUIDForType(FolderType.FAVORITE)
        Inventory.global.collectDescendentsIf(
            favId, categories = mutableListOf(), items = items,
            excludeTrash = true, functor = IsLandmarkType()
        )
        items.sortWith(Comparator { a, b -> getSortIndex(a.uuid).compareTo(getSortIndex(b.uuid)) })
        saveItemsOrder(items)
    }

    fun rearrangeFavoriteLandmarks(sourceItemId: UUID, targetItemId: UUID) {
        val items = mutableListOf<ViewerInventoryItem>()
        val favId = Inventory.global.findCategoryUUIDForType(FolderType.FAVORITE)
        Inventory.global.collectDescendentsIf(
            favId, categories = mutableListOf(), items = items,
            excludeTrash = true, functor = IsLandmarkType()
        )
        items.sortWith(Comparator { a, b -> getSortIndex(a.uuid).compareTo(getSortIndex(b.uuid)) })
        Inventory.global.updateItemsOrder(items, sourceItemId, targetItemId)
        saveItemsOrder(items)
    }

    fun destroyClass() {
        cleanup()
        val filename = getSavedOrderFileName()
        if (filename.isNotEmpty()) {
            System.err.println("FavoritesOrderStorage: delete file $filename not yet implemented")
        }
        if (mSaveOnExit) saveFavoritesRecord(prefChanged = true)
    }

    fun getStoredFavoritesFilename(grid: String = GridManager.instance.getGrid()): String {
        System.err.println("FavoritesOrderStorage: getStoredFavoritesFilename not yet implemented")
        return ""
    }

    fun getSavedOrderFileName(): String {
        System.err.println("FavoritesOrderStorage: getSavedOrderFileName not yet implemented")
        return ""
    }

    fun saveFavoritesRecord(prefChanged: Boolean = false): Boolean {
        var shouldSave = prefChanged || mRecreateFavoriteStorage
        mRecreateFavoriteStorage = false

        if (!Inventory.global.isInventoryUsable()) return false
        val favoriteFolder = Inventory.global.findCategoryUUIDForType(FolderType.FAVORITE)
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (favoriteFolder == nullId) return false

        val items = mutableListOf<ViewerInventoryItem>()
        Inventory.global.collectDescendentsIf(
            favoriteFolder, categories = mutableListOf(), items = items,
            excludeTrash = true, functor = IsLandmarkType()
        )
        items.sortWith(FavoritesSort())

        var nameChanged = false
        for (item in items) {
            if (mFavoriteNames[item.uuid] != item.name) {
                mFavoriteNames[item.uuid] = item.name
                nameChanged = true
            }
        }
        for (missing in mMissingSLURLs) {
            if (mSLURLs.containsKey(missing)) { shouldSave = true; break }
        }

        if (items != mPrevFavorites || nameChanged || shouldSave) {
            System.err.println("FavoritesOrderStorage: serialize and write stored_favorites.xml not yet implemented")
        }
        mPrevFavorites.clear()
        mPrevFavorites.addAll(items)
        return true
    }

    fun showFavoritesOnLoginChanged(show: Boolean) {
        if (show) saveFavoritesRecord(prefChanged = true)
        else removeFavoritesRecordOfUser()
    }

    fun isStorageUpdateNeeded(): Boolean {
        if (!mRecreateFavoriteStorage) {
            System.err.println("FavoritesOrderStorage: iterate mStorageFavorites and compare names not yet implemented")
        }
        return false
    }

    fun removeFavoritesRecordOfUser(user: String = "", grid: String = GridManager.instance.getGrid()) {
        System.err.println("FavoritesOrderStorage: read, strip, and rewrite stored_favorites.xml for user=$user grid=$grid not yet implemented")
    }

    private fun cleanup() {
        if (!mIsDirty) return
        val favId = Inventory.global.findCategoryUUIDForType(FolderType.FAVORITE)
        val items = mutableListOf<ViewerInventoryItem>()
        Inventory.global.collectDescendents(favId, categories = mutableListOf(), items = items, excludeTrash = true)
        val validIds = items.map { it.uuid }.toSet()
        mSortIndexes.keys.retainAll(validIds)
    }

    private fun load() {
        System.err.println("FavoritesOrderStorage: deserialize landmarks_sorting.xml or stored_favorites.xml into mSortIndexes not yet implemented")
    }
}
