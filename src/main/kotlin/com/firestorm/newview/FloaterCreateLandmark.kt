package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// LandmarksInventoryObserver  (mirrors LLLandmarksInventoryObserver)
// ---------------------------------------------------------------------------

class LandmarksInventoryObserver(private val floater: FloaterCreateLandmark) {

    fun changed(mask: UInt) {
        if (floater.getItem() != null) {
            checkChanged(mask)
        } else {
            checkCreated(mask)
        }
    }

    private fun checkCreated(mask: UInt) {
        TODO("APR: if inventory ADD|CREATE|UPDATE_CREATE bits set, call floater.setItem(addedIds)")
    }

    private fun checkChanged(mask: UInt) {
        TODO("APR: if inventory LABEL|INTERNAL|REMOVE|STRUCTURE|REBUILD bits set, call floater.updateItem(changedIds, mask)")
    }
}

// ---------------------------------------------------------------------------
// InventoryItem stub — stand-in for LLInventoryItem
// ---------------------------------------------------------------------------

data class InventoryItem(
    val uuid: LLUUID,
    val assetUuid: LLUUID,
    val parentUuid: LLUUID,
    val name: String,
    val description: String,
    val type: Int
) {
    companion object {
        const val AT_LANDMARK: Int = 8
    }
}

// ---------------------------------------------------------------------------
// FloaterCreateLandmark  (mirrors LLFloaterCreateLandmark)
// ---------------------------------------------------------------------------

class FloaterCreateLandmark(key: LLUUID = LLUUID.NULL) {

    private val inventoryObserver = LandmarksInventoryObserver(this)

    private var landmarksId: LLUUID = LLUUID.NULL
    private var assetId: LLUUID = LLUUID.NULL
    private var parentId: LLUUID = LLUUID.NULL

    private var item: InventoryItem? = null

    fun getItem(): InventoryItem? = item

    fun postBuild(): Boolean {
        TODO("APR: bind folder combo, title editor, notes editor; wire new-folder link, OK and Cancel buttons; find landmarks category UUID")
    }

    fun onOpen(key: LLUUID) {
        item = null
        TODO("APR: register inventoryObserver; call setLandmarkInfo(key destFolder); call populateFoldersList(key destFolder)")
    }

    fun setItem(items: Set<LLUUID>) {
        for (itemId in items) {
            val candidate = lookupInventoryItem(itemId) ?: continue
            if (candidate.type != InventoryItem.AT_LANDMARK) continue
            if (item == null) {
                item = candidate
                assetId = candidate.assetUuid
                parentId = candidate.parentUuid
                TODO("APR: make floater visible and bring to front")
                break
            }
        }
    }

    fun updateItem(items: Set<LLUUID>, mask: UInt) {
        val current = item ?: return
        for (itemId in items) {
            if (current.uuid != itemId) continue

            val fresh = lookupInventoryItem(itemId)
            if (fresh == null || fresh != lookupInventoryItem(itemId)) {
                // item removed or replaced
                close()
                return
            }

            if (parentId != current.parentUuid) {
                // user moved the landmark
                close()
                return
            }

            TODO("APR: handle INTERNAL asset-change and LABEL rename from mask")
        }
    }

    private fun setLandmarkInfo(folderId: LLUUID) {
        TODO("APR: get agent parcel name; build title from region/position; call LandmarkActions.createLandmarkHere(name, folderId ?: landmarksId)")
    }

    private fun populateFoldersList(folderId: LLUUID = LLUUID.NULL) {
        TODO("APR: collect landmark-capable folders; sort alphabetically; prepend Landmarks then Favorites; populate folder combo; select folderId if not null")
    }

    private fun onCommitTextChanges() {
        val current = item ?: return
        TODO("APR: trim title/notes editors; if changed call update_inventory_item with new name/description")
    }

    private fun onCreateFolderClicked() {
        TODO("APR: show CreateLandmarkFolder notification; on confirm create sub-folder under landmarksId; call populateFoldersList with new folder UUID")
    }

    private fun onSaveClicked() {
        val current = item ?: run { close(); return }
        TODO("APR: read title/notes from editors; if changed rename item; if folder changed reparent item; removeObserver(); gInventory.updateItem; close()")
    }

    private fun onCancelClicked() {
        removeObserver()
        item?.let { remove_inventory_item(it.uuid) }
        close()
    }

    private fun removeObserver() {
        TODO("APR: gInventory.removeObserver(inventoryObserver) if registered")
    }

    private fun close() {
        TODO("APR: close this floater")
    }

    private fun lookupInventoryItem(itemId: LLUUID): InventoryItem? {
        TODO("APR: gInventory.getItem($itemId)")
    }

    private fun remove_inventory_item(itemId: LLUUID) {
        TODO("APR: remove_inventory_item($itemId, null)")
    }
}
