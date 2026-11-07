package com.firestorm.newview

import java.util.UUID

class FloaterLandmark(private val key: Any?) {

    private var folderCombo: Any? = null
    private var landmarkTitleEditor: Any? = null
    private var notesEditor: Any? = null
    private var landmarksId: UUID? = null
    private var assetId: UUID? = null
    private var parentId: UUID? = null

    private var inventoryObserver: LandmarksInventoryObserver? = null
    private var item: Any? = null

    init {
        inventoryObserver = LandmarksInventoryObserver(this)
    }

    fun getItem(): Any? = item

    fun postBuild(): Boolean {
        folderCombo = findChildByName("folder_combo")
        landmarkTitleEditor = findChildByName("title_editor")
        notesEditor = findChildByName("notes_editor")

        landmarksId = inventoryFindLandmarksCategory()
        return true
    }

    fun onOpen(key: Any?) {
        val destFolder = extractDestFolderFromKey(key)
        item = null
        inventoryAddObserver(inventoryObserver)
        setLandmarkInfo(destFolder)
        populateFoldersList(destFolder)
    }

    private fun setLandmarkInfo(folderId: UUID?) {
        val parcel = parcelMgrGetAgentParcel()
        val name = parcelGetName(parcel)
        val agentPos = agentGetPositionAgent()

        val title: String
        if (name.isEmpty()) {
            val regionX = agentPos[0].toInt()
            val regionY = agentPos[1].toInt()
            val regionZ = agentPos[2].toInt()
            val regionName = parcelMgrGetSelectionRegionName() ?: agentBuildLocationString(agentPos)
            title = "$regionName ($regionX, $regionY, $regionZ)"
        } else {
            title = name
        }

        setEditorText(landmarkTitleEditor, title)
        landmarkActionsCreateLandmarkHere(name, "", folderId ?: inventoryFindLandmarksCategory())
    }

    private fun populateFoldersList(folderId: UUID?) {
        val cats = collectLandmarkFolders()
        comboRemoveAll(folderCombo)

        val landmarkCat = inventoryGetCategory(landmarksId)
        if (landmarkCat != null) {
            val fullName = getCategoryFullName(landmarkCat)
            comboAdd(folderCombo, fullName, getCategoryUUID(landmarkCat))
        }

        val favoritesId = inventoryFindFavoritesCategory()
        val favoritesCat = inventoryGetCategory(favoritesId)
        if (favoritesCat != null) {
            comboAdd(folderCombo, "Favorites", getCategoryUUID(favoritesCat))
        }

        val sortedFolders = cats
            .map { cat -> Pair(getCategoryUUID(cat), getCategoryFullName(cat)) }
            .sortedBy { it.second }

        for ((uuid, name) in sortedFolders) {
            comboAdd(folderCombo, name, uuid)
        }

        if (folderId != null) {
            comboSetCurrentById(folderCombo, folderId)
        }
    }

    fun setItem(items: Set<UUID>) {
        for (itemId in items) {
            val inv = inventoryGetItem(itemId) ?: continue
            if (!isLandmarkAssetType(inv)) continue
            if (item == null) {
                item = inv
                assetId = inventoryItemGetAssetUUID(inv)
                parentId = inventoryItemGetParentUUID(inv)
                setVisibleAndFrontmost(true)
                break
            }
        }
    }

    fun updateItem(items: Set<UUID>, mask: UInt) {
        val current = item ?: return
        val landmarkId = inventoryItemGetUUID(current)

        for (itemId in items) {
            if (landmarkId != itemId) continue

            if (current !== inventoryGetItem(itemId)) {
                closeFloater()
                return
            }

            if (parentId != inventoryItemGetParentUUID(current)) {
                closeFloater()
                return
            }

            if ((mask and INVENTORY_OBSERVER_INTERNAL) != 0u && assetId != inventoryItemGetAssetUUID(current)) {
                closeFloater()
                return
            }

            if ((mask and INVENTORY_OBSERVER_LABEL) != 0u) {
                setEditorText(landmarkTitleEditor, inventoryItemGetName(current))
            }

            if ((mask and INVENTORY_OBSERVER_INTERNAL) != 0u) {
                setEditorText(notesEditor, inventoryItemGetDescription(current))
            }
        }
    }

    private fun onCommitTextChanges() {
        val current = item ?: return

        val currentTitle = getEditorText(landmarkTitleEditor).trim()
        val itemTitle = inventoryItemGetName(current)
        val currentNotes = getEditorText(notesEditor).trim()
        val itemNotes = inventoryItemGetDescription(current)

        if (currentTitle.isNotEmpty() && (itemTitle != currentTitle || itemNotes != currentNotes)) {
            inventoryUpdateItemNameAndDescription(current, currentTitle, currentNotes)
        }
    }

    private fun onCreateFolderClicked() {
        System.err.println("FloaterLandmark: onCreateFolderClicked not yet implemented")
    }

    private fun folderCreatedCallback(folderId: UUID) {
        populateFoldersList(folderId)
    }

    private fun onSaveClicked() {
        val current = item
        if (current == null) {
            closeFloater()
            return
        }

        val currentTitle = getEditorText(landmarkTitleEditor).trim()
        val itemTitle = inventoryItemGetName(current)
        val currentNotes = getEditorText(notesEditor).trim()
        val itemNotes = inventoryItemGetDescription(current)
        val folderId = comboGetCurrentUUID(folderCombo)
        val changeParent = folderId != inventoryItemGetParentUUID(current)

        if (currentTitle.isNotEmpty() && (itemTitle != currentTitle || itemNotes != currentNotes)) {
            inventoryUpdateItemNameAndDescription(current, currentTitle, currentNotes)
            if (changeParent) {
                inventoryMoveItemToFolder(current, folderId)
            }
        } else if (changeParent) {
            inventoryMoveItemToFolder(current, folderId)
        }

        removeObserver()
        inventoryNotifyObservers()
        closeFloater()
    }

    private fun onCancelClicked() {
        removeObserver()
        val current = item
        if (current != null) {
            inventoryRemoveItem(inventoryItemGetUUID(current))
        }
        closeFloater()
    }

    private fun removeObserver() {
        inventoryRemoveObserver(inventoryObserver)
    }

    private fun closeFloater() {
        System.err.println("FloaterLandmark: closeFloater not yet implemented")
    }

    private fun setVisibleAndFrontmost(takeFocus: Boolean) {
        System.err.println("FloaterLandmark: setVisibleAndFrontmost not yet implemented")
    }
}

class LandmarksInventoryObserver(private val floater: FloaterLandmark) {

    fun changed(mask: UInt) {
        if (floater.getItem() != null) {
            checkChanged(mask)
        } else {
            checkCreated(mask)
        }
    }

    private fun checkCreated(mask: UInt) {
        val addedIds = inventoryGetAddedIDs()
        if (addedIds.isEmpty()) return

        if ((mask and INVENTORY_OBSERVER_ADD) == 0u ||
            (mask and INVENTORY_OBSERVER_CREATE) == 0u ||
            (mask and INVENTORY_OBSERVER_UPDATE_CREATE) == 0u) {
            return
        }

        floater.setItem(addedIds)
    }

    private fun checkChanged(mask: UInt) {
        val changedIds = inventoryGetChangedIDs()
        if (changedIds.isEmpty()) return

        val relevantMask = INVENTORY_OBSERVER_LABEL or INVENTORY_OBSERVER_INTERNAL or
                INVENTORY_OBSERVER_REMOVE or INVENTORY_OBSERVER_STRUCTURE or INVENTORY_OBSERVER_REBUILD
        if ((mask and relevantMask) != 0u) {
            floater.updateItem(changedIds, mask)
        }
    }
}

private val INVENTORY_OBSERVER_ADD: UInt = 0x01u
private val INVENTORY_OBSERVER_CREATE: UInt = 0x02u
private val INVENTORY_OBSERVER_UPDATE_CREATE: UInt = 0x04u
private val INVENTORY_OBSERVER_LABEL: UInt = 0x08u
private val INVENTORY_OBSERVER_INTERNAL: UInt = 0x10u
private val INVENTORY_OBSERVER_REMOVE: UInt = 0x20u
private val INVENTORY_OBSERVER_STRUCTURE: UInt = 0x40u
private val INVENTORY_OBSERVER_REBUILD: UInt = 0x80u

private fun findChildByName(name: String): Any? = null
private fun inventoryFindLandmarksCategory(): UUID = UUID(0, 0)
private fun inventoryFindFavoritesCategory(): UUID = UUID(0, 0)
private fun inventoryGetCategory(id: UUID?): Any? = null
private fun inventoryGetItem(id: UUID): Any? = null
private fun inventoryAddObserver(observer: Any?) { System.err.println("FloaterLandmark: inventoryAddObserver not yet implemented") }
private fun inventoryRemoveObserver(observer: Any?) { System.err.println("FloaterLandmark: inventoryRemoveObserver not yet implemented") }
private fun inventoryNotifyObservers() { System.err.println("FloaterLandmark: inventoryNotifyObservers not yet implemented") }
private fun inventoryGetAddedIDs(): Set<UUID> = emptySet()
private fun inventoryGetChangedIDs(): Set<UUID> = emptySet()
private fun inventoryUpdateItemNameAndDescription(item: Any, name: String, description: String) { System.err.println("FloaterLandmark: inventoryUpdateItemNameAndDescription not yet implemented") }
private fun inventoryMoveItemToFolder(item: Any, folderId: UUID?) { System.err.println("FloaterLandmark: inventoryMoveItemToFolder not yet implemented") }
private fun inventoryRemoveItem(itemId: UUID) { System.err.println("FloaterLandmark: inventoryRemoveItem not yet implemented") }
private fun inventoryItemGetUUID(item: Any): UUID = UUID(0, 0)
private fun inventoryItemGetAssetUUID(item: Any): UUID = UUID(0, 0)
private fun inventoryItemGetParentUUID(item: Any): UUID? = null
private fun inventoryItemGetName(item: Any): String = ""
private fun inventoryItemGetDescription(item: Any): String = ""
private fun isLandmarkAssetType(item: Any): Boolean = false
private fun collectLandmarkFolders(): List<Any> = emptyList()
private fun getCategoryUUID(cat: Any): UUID = UUID(0, 0)
private fun getCategoryFullName(cat: Any): String = ""
private fun comboRemoveAll(combo: Any?) { System.err.println("FloaterLandmark: comboRemoveAll not yet implemented") }
private fun comboAdd(combo: Any?, label: String, value: UUID) { System.err.println("FloaterLandmark: comboAdd not yet implemented") }
private fun comboSetCurrentById(combo: Any?, id: UUID) { System.err.println("FloaterLandmark: comboSetCurrentById not yet implemented") }
private fun comboGetCurrentUUID(combo: Any?): UUID? = null
private fun setEditorText(editor: Any?, text: String) { System.err.println("FloaterLandmark: setEditorText not yet implemented") }
private fun getEditorText(editor: Any?): String = ""
private fun parcelMgrGetAgentParcel(): Any? = null
private fun parcelGetName(parcel: Any?): String = ""
private fun parcelMgrGetSelectionRegionName(): String? = null
private fun agentGetPositionAgent(): FloatArray = FloatArray(3)
private fun agentBuildLocationString(pos: FloatArray): String = ""
private fun landmarkActionsCreateLandmarkHere(name: String, notes: String, folderId: UUID?) { System.err.println("FloaterLandmark: landmarkActionsCreateLandmarkHere not yet implemented") }
private fun extractDestFolderFromKey(key: Any?): UUID? = null
