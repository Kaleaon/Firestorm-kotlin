package com.firestorm.newview

import java.util.UUID

abstract class LLFloaterPathfindingObjects protected constructor(seed: LLSD) : LLFloater(seed) {

    enum class EMessagingState {
        UNKNOWN,
        GET_REQUEST_SENT,
        GET_ERROR,
        SET_REQUEST_SENT,
        SET_ERROR,
        COMPLETE,
        NOT_ENABLED
    }

    companion object {
        private const val DEFAULT_BEACON_WIDTH = 6
    }

    private var objectsScrollList: LLScrollListCtrl? = null
    private var messagingStatus: LLTextBase? = null
    private var refreshListButton: LLButton? = null
    private var selectAllButton: LLButton? = null
    private var selectNoneButton: LLButton? = null
    private var showBeaconCheckBox: LLCheckBoxCtrl? = null
    private var takeButton: LLButton? = null
    private var takeCopyButton: LLButton? = null
    private var returnButton: LLButton? = null
    private var deleteButton: LLButton? = null
    private var teleportButton: LLButton? = null

    private var defaultBeaconColor: LLColor4 = LLColor4()
    private var defaultBeaconTextColor: LLColor4 = LLColor4()
    private var errorTextColor: LLColor4 = LLColor4()
    private var warningTextColor: LLColor4 = LLColor4()

    private var messagingState: EMessagingState = EMessagingState.UNKNOWN
    private var messagingRequestId: LLPathfindingManager.RequestId = 0u

    private val missingNameObjectsScrollListItems: MutableMap<String, LLScrollListItem> = mutableMapOf()

    protected var objectList: LLPathfindingObjectListPtr? = null
    private var objectsSelection: LLObjectSelectionHandle? = null

    private var hasObjectsToBeSelected: Boolean = false
    private val objectsToBeSelected: MutableList<UUID> = mutableListOf()

    private var selectionUpdateSlot: (() -> Unit)? = null
    private var regionBoundaryCrossingSlot: (() -> Unit)? = null
    private var godLevelChangeSlot: (() -> Unit)? = null

    override fun onOpen(key: LLSD) {
        super.onOpen(key)
        selectNoneObjects()
        objectsScrollList!!.setCommitOnSelectionChange(true)

        if (selectionUpdateSlot == null) {
            selectionUpdateSlot = LLSelectMgr.getInstance().mUpdateSignal.connect { onInWorldSelectionListChanged() }
        }
        if (regionBoundaryCrossingSlot == null) {
            regionBoundaryCrossingSlot = gAgent.addRegionChangedCallback { onRegionBoundaryCrossed() }
        }
        if (godLevelChangeSlot == null) {
            godLevelChangeSlot = gAgent.registerGodLevelChangeListener { level -> onGodLevelChange(level) }
        }

        requestGetObjects()
    }

    override fun onClose(isAppQuitting: Boolean) {
        godLevelChangeSlot = null
        regionBoundaryCrossingSlot = null
        selectionUpdateSlot = null

        objectsScrollList!!.setCommitOnSelectionChange(false)
        selectNoneObjects()

        objectsSelection?.clear()
        objectsSelection = null

        if (isAppQuitting) {
            clearAllObjects()
        }
    }

    open fun draw() {
        super.draw()

        if (isShowBeacons()) {
            val selectedItems = objectsScrollList!!.getAllSelected()
            if (selectedItems.isNotEmpty()) {
                val nameColumnIndex = getNameColumnIndex()
                val beaconColor = getBeaconColor()
                val beaconTextColor = getBeaconTextColor()
                val beaconWidth = getBeaconWidth()

                for (selectedItem in selectedItems) {
                    val viewerObject = gObjectList.findObject(selectedItem.getUUID())
                    if (viewerObject != null) {
                        val objectName = selectedItem.getColumn(nameColumnIndex)!!.getValue().asString()
                        // no-op
                    }
                }
            }
        }
    }

    override fun postBuild(): Boolean {
        defaultBeaconColor = LLUIColorTable.getInstance()!!.getColor("PathfindingDefaultBeaconColor")
        defaultBeaconTextColor = LLUIColorTable.getInstance()!!.getColor("PathfindingDefaultBeaconTextColor")
        errorTextColor = LLUIColorTable.getInstance()!!.getColor("PathfindingErrorColor")
        warningTextColor = LLUIColorTable.getInstance()!!.getColor("PathfindingWarningColor")

        objectsScrollList = findChild("objects_scroll_list")!!
        objectsScrollList!!.setCommitCallback { onScrollListSelectionChanged() }
        objectsScrollList!!.sortByColumnIndex(getNameColumnIndex().toUInt(), ascending = true)

        messagingStatus = findChild("messaging_status")!!

        refreshListButton = findChild("refresh_objects_list")!!
        refreshListButton!!.setCommitCallback { onRefreshObjectsClicked() }

        selectAllButton = findChild("select_all_objects")!!
        selectAllButton!!.setCommitCallback { onSelectAllObjectsClicked() }

        selectNoneButton = findChild("select_none_objects")!!
        selectNoneButton!!.setCommitCallback { onSelectNoneObjectsClicked() }

        showBeaconCheckBox = findChild("show_beacon")!!

        takeButton = findChild("take_objects")!!
        takeButton!!.setCommitCallback { onTakeClicked() }

        takeCopyButton = findChild("take_copy_objects")!!
        takeCopyButton!!.setCommitCallback { onTakeCopyClicked() }

        returnButton = findChild("return_objects")!!
        returnButton!!.setCommitCallback { onReturnClicked() }

        deleteButton = findChild("delete_objects")!!
        deleteButton!!.setCommitCallback { onDeleteClicked() }

        teleportButton = findChild("teleport_me_to_object")!!
        teleportButton!!.setCommitCallback { onTeleportClicked() }

        return super.postBuild()
    }

    open fun requestGetObjects() {
        System.err.println("LLFloaterPathfindingObjects: requestGetObjects not yet implemented")
    }

    fun getNewRequestId(): LLPathfindingManager.RequestId {
        return ++messagingRequestId
    }

    fun handleNewObjectList(
        requestId: LLPathfindingManager.RequestId,
        requestStatus: LLPathfindingManager.ERequestStatus,
        objList: LLPathfindingObjectListPtr?
    ) {
        if (requestId != messagingRequestId) return
        when (requestStatus) {
            LLPathfindingManager.ERequestStatus.STARTED -> setMessagingState(EMessagingState.GET_REQUEST_SENT)
            LLPathfindingManager.ERequestStatus.COMPLETED -> {
                objectList = objList
                rebuildObjectsScrollList()
                setMessagingState(EMessagingState.COMPLETE)
            }
            LLPathfindingManager.ERequestStatus.NOT_ENABLED -> {
                clearAllObjects()
                setMessagingState(EMessagingState.NOT_ENABLED)
            }
            LLPathfindingManager.ERequestStatus.ERROR -> {
                clearAllObjects()
                setMessagingState(EMessagingState.GET_ERROR)
            }
        }
    }

    fun handleUpdateObjectList(
        requestId: LLPathfindingManager.RequestId,
        requestStatus: LLPathfindingManager.ERequestStatus,
        objList: LLPathfindingObjectListPtr?
    ) {
        if (requestId != messagingRequestId) return
        when (requestStatus) {
            LLPathfindingManager.ERequestStatus.STARTED -> setMessagingState(EMessagingState.SET_REQUEST_SENT)
            LLPathfindingManager.ERequestStatus.COMPLETED -> {
                if (objectList == null) {
                    objectList = objList
                } else {
                    objectList!!.update(objList)
                }
                rebuildObjectsScrollList()
                setMessagingState(EMessagingState.COMPLETE)
            }
            LLPathfindingManager.ERequestStatus.NOT_ENABLED -> {
                clearAllObjects()
                setMessagingState(EMessagingState.NOT_ENABLED)
            }
            LLPathfindingManager.ERequestStatus.ERROR -> {
                clearAllObjects()
                setMessagingState(EMessagingState.SET_ERROR)
            }
        }
    }

    fun rebuildObjectsScrollList(updateIfNeeded: Boolean = false) {
        if (!hasObjectsToBeSelected) {
            val selectedItems = objectsScrollList!!.getAllSelected()
            if (selectedItems.isNotEmpty()) {
                for (item in selectedItems) {
                    objectsToBeSelected.add(item.getUUID())
                }
            }
        }

        val origScrollPosition = objectsScrollList!!.getScrollPos()
        objectsScrollList!!.deleteAllItems()
        missingNameObjectsScrollListItems.clear()

        if (objectList != null && !objectList!!.isEmpty()) {
            buildObjectsScrollList(objectList!!)

            if (objectsScrollList!!.selectMultiple(objectsToBeSelected) == 0) {
                if (updateIfNeeded && refreshListButton!!.getEnabled()) {
                    requestGetObjects()
                    return
                }
            }
            if (hasObjectsToBeSelected) {
                objectsScrollList!!.scrollToShowSelected()
            } else {
                objectsScrollList!!.setScrollPos(origScrollPosition)
            }
        }

        objectsToBeSelected.clear()
        hasObjectsToBeSelected = false

        updateControlsOnScrollListChange()
    }

    open fun buildObjectsScrollList(objectListPtr: LLPathfindingObjectListPtr) {
        System.err.println("LLFloaterPathfindingObjects: buildObjectsScrollList not yet implemented")
    }

    fun addObjectToScrollList(objectPtr: LLPathfindingObjectPtr, scrollListItemData: LLSD) {
        val rowParams = LLScrollListItem.Params()
        rowParams.value = objectPtr.getUUID().toString()

        for (cellElement in scrollListItemData.asArray()) {
            val params = LLScrollListCell.Params()
            params.column = cellElement["column"].asString()
            params.value = cellElement["value"].asString()
            rowParams.columns.add(params)
        }

        val scrollListItem = objectsScrollList!!.addRow(rowParams)

        if (objectPtr.hasOwner() && !objectPtr.hasOwnerName()) {
            missingNameObjectsScrollListItems[objectPtr.getUUID().toString()] = scrollListItem
            objectPtr.registerOwnerNameListener { obj -> handleObjectNameResponse(obj) }
        }
    }

    open fun updateControlsOnScrollListChange() {
        updateMessagingStatus()
        updateStateOnListControls()
        selectScrollListItemsInWorld()
        updateStateOnActionControls()
    }

    open fun updateControlsOnInWorldSelectionChange() {
        updateStateOnActionControls()
    }

    open fun getNameColumnIndex(): Int = 0

    open fun getOwnerNameColumnIndex(): Int = 2

    open fun getOwnerName(obj: LLPathfindingObject): String {
        return ""
    }

    open fun getBeaconColor(): LLColor4 = defaultBeaconColor

    open fun getBeaconTextColor(): LLColor4 = defaultBeaconTextColor

    open fun getBeaconWidth(): Int = DEFAULT_BEACON_WIDTH

    fun showFloaterWithSelectionObjects() {
        objectsToBeSelected.clear()

        val selectedObjectsHandle = LLSelectMgr.getInstance().getSelection()
        if (selectedObjectsHandle != null) {
            val selectedObjects = selectedObjectsHandle.get()
            if (!selectedObjects.isEmpty()) {
                for (obj in selectedObjects.validIterable()) {
                    objectsToBeSelected.add(obj.getObject().getID())
                }
            }
        }
        hasObjectsToBeSelected = true

        if (!isShown()) {
            openFloater()
            setVisibleAndFrontmost()
        } else {
            rebuildObjectsScrollList(updateIfNeeded = true)
            if (isMinimized()) {
                setMinimized(false)
            }
            setVisibleAndFrontmost()
        }
        setFocus(true)
    }

    fun isShowBeacons(): Boolean = showBeaconCheckBox!!.get()

    fun clearAllObjects() {
        selectNoneObjects()
        objectsScrollList!!.deleteAllItems()
        missingNameObjectsScrollListItems.clear()
        objectList = null
    }

    fun selectAllObjects() {
        objectsScrollList!!.selectAll()
    }

    fun selectNoneObjects() {
        objectsScrollList!!.deselectAllItems()
    }

    fun teleportToSelectedObject() {
        val selectedItems = objectsScrollList!!.getAllSelected()
        if (selectedItems.size == 1) {
            val selectedItem = selectedItems.first()
            val teleportLocation: LLVector3d
            val viewerObject = gObjectList.findObject(selectedItem.getUUID())
            if (viewerObject == null) {
                val objectPtr = objectList!!.find(selectedItem.getUUID().toString())!!
                teleportLocation = gAgent.getPosGlobalFromAgent(objectPtr.getLocation())
            } else {
                teleportLocation = viewerObject.getPositionGlobal()
            }
            gAgent.teleportViaLocationLookAt(teleportLocation)
        }
    }

    open fun getEmptyObjectList(): LLPathfindingObjectListPtr {
        return LLPathfindingObjectList()
    }

    fun getNumSelectedObjects(): Int = objectsScrollList!!.getNumSelected()

    fun getSelectedObjects(): LLPathfindingObjectListPtr {
        val selectedObjects = getEmptyObjectList()
        val selectedItems = objectsScrollList!!.getAllSelected()
        for (item in selectedItems) {
            val objPtr = findObject(item)
            if (objPtr != null) {
                selectedObjects.update(objPtr)
            }
        }
        return selectedObjects
    }

    fun getFirstSelectedObject(): LLPathfindingObjectPtr? {
        val selectedItems = objectsScrollList!!.getAllSelected()
        return if (selectedItems.isNotEmpty()) findObject(selectedItems.first()) else null
    }

    fun getMessagingState(): EMessagingState = messagingState

    private fun setMessagingState(state: EMessagingState) {
        messagingState = state
        updateControlsOnScrollListChange()
    }

    private fun onRefreshObjectsClicked() {
        requestGetObjects()
    }

    private fun onSelectAllObjectsClicked() {
        selectAllObjects()
    }

    private fun onSelectNoneObjectsClicked() {
        selectNoneObjects()
    }

    private fun onTakeClicked() {
        handleTake()
        requestGetObjects()
    }

    private fun onTakeCopyClicked() {
        handleTakeCopy()
    }

    private fun onReturnClicked() {
        val numItems = getNumSelectedObjects()
        if (numItems == 1) {
            handleReturnItemsResponse(LLSD(), LLSD(0))
        } else if (numItems > 1) {
            LLNotificationsUtil.add(
                "PathfindingReturnMultipleItems",
                LLSD().also { it["NUM_ITEMS"] = numItems }
            ) { notification, response -> handleReturnItemsResponse(notification, response) }
        }
    }

    private fun onDeleteClicked() {
        val numItems = getNumSelectedObjects()
        if (numItems == 1) {
            handleDeleteItemsResponse(LLSD(), LLSD(0))
        } else if (numItems > 1) {
            LLNotificationsUtil.add(
                "PathfindingDeleteMultipleItems",
                LLSD().also { it["NUM_ITEMS"] = numItems }
            ) { notification, response -> handleDeleteItemsResponse(notification, response) }
        }
    }

    private fun onTeleportClicked() {
        teleportToSelectedObject()
    }

    private fun onScrollListSelectionChanged() {
        updateControlsOnScrollListChange()
    }

    private fun onInWorldSelectionListChanged() {
        updateControlsOnInWorldSelectionChange()
    }

    private fun onRegionBoundaryCrossed() {
        requestGetObjects()
    }

    private fun onGodLevelChange(godLevel: UByte) {
        requestGetObjects()
    }

    private fun handleObjectNameResponse(obj: LLPathfindingObject) {
        val uuid = obj.getUUID().toString()
        val scrollListItem = missingNameObjectsScrollListItems[uuid] ?: return
        val scrollListCell = scrollListItem.getColumn(getOwnerNameColumnIndex())
        scrollListCell?.setValue(LLSD(getOwnerName(obj)))
        missingNameObjectsScrollListItems.remove(uuid)
    }

    private fun updateMessagingStatus() {
        val statusText: String
        val styleColor: LLColor4?
        when (messagingState) {
            EMessagingState.UNKNOWN -> { statusText = getString("messaging_initial"); styleColor = errorTextColor }
            EMessagingState.GET_REQUEST_SENT -> { statusText = getString("messaging_get_inprogress"); styleColor = warningTextColor }
            EMessagingState.GET_ERROR -> { statusText = getString("messaging_get_error"); styleColor = errorTextColor }
            EMessagingState.SET_REQUEST_SENT -> { statusText = getString("messaging_set_inprogress"); styleColor = warningTextColor }
            EMessagingState.SET_ERROR -> { statusText = getString("messaging_set_error"); styleColor = errorTextColor }
            EMessagingState.COMPLETE -> {
                if (objectsScrollList!!.isEmpty()) {
                    statusText = getString("messaging_complete_none_found")
                    styleColor = null
                } else {
                    val numItems = objectsScrollList!!.getItemCount()
                    val numSelected = objectsScrollList!!.getNumSelected()
                    statusText = getString("messaging_complete_available")
                        .replace("[NUM_SELECTED]", numSelected.toString())
                        .replace("[NUM_TOTAL]", numItems.toString())
                    styleColor = null
                }
            }
            EMessagingState.NOT_ENABLED -> { statusText = getString("messaging_not_enabled"); styleColor = errorTextColor }
        }
        messagingStatus!!.setText(statusText, styleColor)
    }

    private fun updateStateOnListControls() {
        when (messagingState) {
            EMessagingState.UNKNOWN,
            EMessagingState.GET_REQUEST_SENT,
            EMessagingState.SET_REQUEST_SENT -> {
                refreshListButton!!.setEnabled(false)
                selectAllButton!!.setEnabled(false)
                selectNoneButton!!.setEnabled(false)
            }
            EMessagingState.GET_ERROR,
            EMessagingState.SET_ERROR,
            EMessagingState.NOT_ENABLED -> {
                refreshListButton!!.setEnabled(true)
                selectAllButton!!.setEnabled(false)
                selectNoneButton!!.setEnabled(false)
            }
            EMessagingState.COMPLETE -> {
                val numItems = objectsScrollList!!.getItemCount()
                val numSelected = objectsScrollList!!.getNumSelected()
                refreshListButton!!.setEnabled(true)
                selectAllButton!!.setEnabled(numSelected < numItems)
                selectNoneButton!!.setEnabled(numSelected > 0)
            }
        }
    }

    private fun updateStateOnActionControls() {
        var numSelectedItems = objectsScrollList!!.getNumSelected()
        var isEditEnabled = numSelectedItems > 0

        showBeaconCheckBox!!.setEnabled(isEditEnabled)
        takeButton!!.setEnabled(isEditEnabled && visibleTakeObject())
        takeCopyButton!!.setEnabled(isEditEnabled && enableObjectTakeCopy())
        returnButton!!.setEnabled(isEditEnabled && enableObjectReturn())
        deleteButton!!.setEnabled(isEditEnabled && enableObjectDelete())
        teleportButton!!.setEnabled(numSelectedItems == 1)
    }

    private fun selectScrollListItemsInWorld() {
        objectsSelection?.clear()
        objectsSelection = null
        LLSelectMgr.getInstance().deselectAll()

        val selectedItems = objectsScrollList!!.getAllSelected()
        if (selectedItems.isNotEmpty()) {
            val viewerObjects = mutableListOf<LLViewerObject>()
            for (selectedItem in selectedItems) {
                val viewerObject = gObjectList.findObject(selectedItem.getUUID())
                if (viewerObject != null) {
                    viewerObjects.add(viewerObject)
                }
            }
            if (viewerObjects.isNotEmpty()) {
                objectsSelection = LLSelectMgr.getInstance().selectObjectAndFamily(viewerObjects)
            }
        }
    }

    private fun handleReturnItemsResponse(notification: LLSD, response: LLSD) {
        if (LLNotificationsUtil.getSelectedOption(notification, response) == 0) {
            handleObjectReturn()
            requestGetObjects()
        }
    }

    private fun handleDeleteItemsResponse(notification: LLSD, response: LLSD) {
        if (LLNotificationsUtil.getSelectedOption(notification, response) == 0) {
            handleObjectDelete()
            requestGetObjects()
        }
    }

    private fun findObject(listItem: LLScrollListItem): LLPathfindingObjectPtr? {
        val uuidString = listItem.getUUID().toString()
        return objectList?.find(uuidString)
    }
}
