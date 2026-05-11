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
        TODO("APR: use JVM equivalent")
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
        TODO("APR: use JVM equivalent")
    }

    private fun setVisibleAndFrontmost(takeFocus: Boolean) {
        TODO("APR: use JVM equivalent")
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

private fun findChildByName(name: String): Any? = TODO("APR: use JVM equivalent")
private fun inventoryFindLandmarksCategory(): UUID = TODO("APR: use JVM equivalent")
private fun inventoryFindFavoritesCategory(): UUID = TODO("APR: use JVM equivalent")
private fun inventoryGetCategory(id: UUID?): Any? = TODO("APR: use JVM equivalent")
private fun inventoryGetItem(id: UUID): Any? = TODO("APR: use JVM equivalent")
private fun inventoryAddObserver(observer: Any?) { TODO("APR: use JVM equivalent") }
private fun inventoryRemoveObserver(observer: Any?) { TODO("APR: use JVM equivalent") }
private fun inventoryNotifyObservers() { TODO("APR: use JVM equivalent") }
private fun inventoryGetAddedIDs(): Set<UUID> = TODO("APR: use JVM equivalent")
private fun inventoryGetChangedIDs(): Set<UUID> = TODO("APR: use JVM equivalent")
private fun inventoryUpdateItemNameAndDescription(item: Any, name: String, description: String) { TODO("APR: use JVM equivalent") }
private fun inventoryMoveItemToFolder(item: Any, folderId: UUID?) { TODO("APR: use JVM equivalent") }
private fun inventoryRemoveItem(itemId: UUID) { TODO("APR: use JVM equivalent") }
private fun inventoryItemGetUUID(item: Any): UUID = TODO("APR: use JVM equivalent")
private fun inventoryItemGetAssetUUID(item: Any): UUID = TODO("APR: use JVM equivalent")
private fun inventoryItemGetParentUUID(item: Any): UUID? = TODO("APR: use JVM equivalent")
private fun inventoryItemGetName(item: Any): String = TODO("APR: use JVM equivalent")
private fun inventoryItemGetDescription(item: Any): String = TODO("APR: use JVM equivalent")
private fun isLandmarkAssetType(item: Any): Boolean = TODO("APR: use JVM equivalent")
private fun collectLandmarkFolders(): List<Any> = TODO("APR: use JVM equivalent")
private fun getCategoryUUID(cat: Any): UUID = TODO("APR: use JVM equivalent")
private fun getCategoryFullName(cat: Any): String = TODO("APR: use JVM equivalent")
private fun comboRemoveAll(combo: Any?) { TODO("APR: use JVM equivalent") }
private fun comboAdd(combo: Any?, label: String, value: UUID) { TODO("APR: use JVM equivalent") }
private fun comboSetCurrentById(combo: Any?, id: UUID) { TODO("APR: use JVM equivalent") }
private fun comboGetCurrentUUID(combo: Any?): UUID? = TODO("APR: use JVM equivalent")
private fun setEditorText(editor: Any?, text: String) { TODO("APR: use JVM equivalent") }
private fun getEditorText(editor: Any?): String = TODO("APR: use JVM equivalent")
private fun parcelMgrGetAgentParcel(): Any? = TODO("APR: use JVM equivalent")
private fun parcelGetName(parcel: Any?): String = TODO("APR: use JVM equivalent")
private fun parcelMgrGetSelectionRegionName(): String? = TODO("APR: use JVM equivalent")
private fun agentGetPositionAgent(): FloatArray = TODO("APR: use JVM equivalent")
private fun agentBuildLocationString(pos: FloatArray): String = TODO("APR: use JVM equivalent")
private fun landmarkActionsCreateLandmarkHere(name: String, notes: String, folderId: UUID?) { TODO("APR: use JVM equivalent") }
