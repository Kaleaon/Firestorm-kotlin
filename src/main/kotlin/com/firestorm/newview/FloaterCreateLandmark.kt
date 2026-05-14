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
        System.err.println("LandmarksInventoryObserver: checkCreated not yet implemented")
    }

    private fun checkChanged(mask: UInt) {
        System.err.println("LandmarksInventoryObserver: checkChanged not yet implemented")
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
        System.err.println("FloaterCreateLandmark: postBuild not yet implemented")
        return false
    }

    fun onOpen(key: LLUUID) {
        item = null
        System.err.println("FloaterCreateLandmark: onOpen not yet implemented")
    }

    fun setItem(items: Set<LLUUID>) {
        for (itemId in items) {
            val candidate = lookupInventoryItem(itemId) ?: continue
            if (candidate.type != InventoryItem.AT_LANDMARK) continue
            if (item == null) {
                item = candidate
                assetId = candidate.assetUuid
                parentId = candidate.parentUuid
                System.err.println("FloaterCreateLandmark: make floater visible not yet implemented")
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

            System.err.println("FloaterCreateLandmark: updateItem mask handling not yet implemented")
        }
    }

    private fun setLandmarkInfo(folderId: LLUUID) {
        System.err.println("FloaterCreateLandmark: setLandmarkInfo not yet implemented")
    }

    private fun populateFoldersList(folderId: LLUUID = LLUUID.NULL) {
        System.err.println("FloaterCreateLandmark: populateFoldersList not yet implemented")
    }

    private fun onCommitTextChanges() {
        val current = item ?: return
        System.err.println("FloaterCreateLandmark: onCommitTextChanges not yet implemented")
    }

    private fun onCreateFolderClicked() {
        System.err.println("FloaterCreateLandmark: onCreateFolderClicked not yet implemented")
    }

    private fun onSaveClicked() {
        val current = item ?: run { close(); return }
        System.err.println("FloaterCreateLandmark: onSaveClicked not yet implemented")
    }

    private fun onCancelClicked() {
        removeObserver()
        item?.let { remove_inventory_item(it.uuid) }
        close()
    }

    private fun removeObserver() {
        System.err.println("FloaterCreateLandmark: removeObserver not yet implemented")
    }

    private fun close() {
        System.err.println("FloaterCreateLandmark: close not yet implemented")
    }

    private fun lookupInventoryItem(itemId: LLUUID): InventoryItem? {
        System.err.println("FloaterCreateLandmark: lookupInventoryItem not yet implemented")
        return null
    }

    private fun remove_inventory_item(itemId: LLUUID) {
        System.err.println("FloaterCreateLandmark: remove_inventory_item not yet implemented")
    }
}
