package com.firestorm.newview

import com.firestorm.llappearance.LLWearableType
import com.firestorm.llcommon.LLUUID
import com.firestorm.llinventory.FolderType
import com.firestorm.llinventory.LLFolderType
import com.firestorm.llinventory.LLInventoryType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LLInventorySortTest {

    @Test
    fun testIsObjectDescendentOfNoAllocationAndCycleSafety() {
        val catRoot = ViewerInventoryCategory(uuid = LLUUID.generate(), parentId = LLUUID.NULL, name = "Root", preferredType = FolderType.NONE.value)
        val catFav = ViewerInventoryCategory(uuid = LLUUID.generate(), parentId = catRoot.uuid, name = "Favorites", preferredType = FolderType.FAVORITE.value)
        val catSub = ViewerInventoryCategory(uuid = LLUUID.generate(), parentId = catFav.uuid, name = "SubFolder", preferredType = FolderType.NONE.value)
        val itemInSub = ViewerInventoryItem(uuid = LLUUID.generate(), parentId = catSub.uuid, name = "Landmark1")

        InventoryModel.categoryMap[catRoot.uuid] = catRoot
        InventoryModel.categoryMap[catFav.uuid] = catFav
        InventoryModel.categoryMap[catSub.uuid] = catSub
        InventoryModel.itemMap[itemInSub.uuid] = itemInSub

        // Valid ancestor chain
        assertTrue(InventoryModel.isObjectDescendentOf(itemInSub.uuid, catFav.uuid))
        assertTrue(InventoryModel.isObjectDescendentOf(itemInSub.uuid, catRoot.uuid))
        assertFalse(InventoryModel.isObjectDescendentOf(catFav.uuid, itemInSub.uuid))

        // Create a parent loop A -> B -> A
        val loopA = ViewerInventoryCategory(uuid = LLUUID.generate(), parentId = LLUUID.NULL, name = "LoopA", preferredType = FolderType.NONE.value)
        val loopB = ViewerInventoryCategory(uuid = LLUUID.generate(), parentId = loopA.uuid, name = "LoopB", preferredType = FolderType.NONE.value)
        loopA.parentId = loopB.uuid

        InventoryModel.categoryMap[loopA.uuid] = loopA
        InventoryModel.categoryMap[loopB.uuid] = loopB

        // Cycle detection terminates safely returning false
        assertFalse(InventoryModel.isObjectDescendentOf(loopA.uuid, catFav.uuid))
        assertFalse(InventoryModel.isObjectDescendentOf(loopB.uuid, catFav.uuid))

        // Clean up
        InventoryModel.categoryMap.clear()
        InventoryModel.itemMap.clear()
    }

    @Test
    fun testSortContextLifecycleAndMemoization() {
        val sorter = LLInventorySort()
        assertFalse(sorter.isSortPassActive())

        sorter.prepareSortContext()
        assertTrue(sorter.isSortPassActive())

        sorter.clearSortContext()
        assertFalse(sorter.isSortPassActive())
    }

    @Test
    fun testCompareSortOrderParity() {
        val sorter = LLInventorySort(0) // Default sort by name

        class TestItem(
            private val uuid: UUID,
            private val name: String,
            private val creationDate: Long,
            private val sortGroup: EInventorySortGroup = EInventorySortGroup.SG_ITEM,
            private val invType: LLInventoryType.EType = LLInventoryType.EType.IT_OBJECT
        ) : LLFolderViewModelItemInventory(LLFolderViewModelInventory("test")) {
            override fun getUUID(): UUID = uuid
            override fun getThumbnailUUID(): UUID = UUID(0, 0)
            override fun getCreationDate(): Long = creationDate
            override fun setCreationDate(creationDateUtc: Long) {}
            override fun getPermissionMask(): Int = 0
            override fun getPreferredType(): LLFolderType.EType = LLFolderType.EType.FT_NONE
            override fun showProperties() {}
            override fun isUpToDate(): Boolean = true
            override fun hasChildren(): Boolean = false
            override fun getInventoryType(): LLInventoryType.EType = invType
            override fun performAction(model: LLInventoryModel, action: String) {}
            override fun getWearableType(): LLWearableType.EType = LLWearableType.EType.WT_NONE
            override fun getSettingsType(): LLSettingsType.TypeE = LLSettingsType.TypeE.ST_NONE
            override fun getSortGroup(): EInventorySortGroup = sortGroup
            override fun getInventoryObject(): LLInventoryObject? = null
            override fun startDrag(type: Array<EDragAndDropType>, id: Array<UUID>): Boolean = false
            override fun getDragSource(): LLToolDragAndDrop.ESource = LLToolDragAndDrop.ESource.SRC_INVENTORY

            override fun getName(): String = name
            override fun getDisplayName(): String = name
            override fun getSearchableName(): String = name
            override fun getSearchableDescription(): String = ""
            override fun getSearchableCreatorName(): String = ""
            override fun getSearchableUUIDString(): String = uuid.toString()
            override fun getSearchableAll(): String = name
            override fun getIcon(): LLUIImage? = null
            override fun getIconOpen(): LLUIImage? = null
            override fun getIconOverlay(): LLUIImage? = null
            override fun getLabelStyle(): Int = 0
            override fun getLabelSuffix(): String = ""
            override fun openItem() {}
            override fun closeItem() {}
            override fun selectItem() {}
            override fun navigateToFolder(newWindow: Boolean, changeMode: Boolean) {}
            override fun isItemWearable(): Boolean = false
            override fun isFavorite(): Boolean = false
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
            override fun dragOrDrop(
                mask: UInt,
                drop: Boolean,
                cargoType: Int,
                cargoData: Any?,
                tooltipMsg: StringBuilder
            ): Boolean = false
            override fun filter(filter: LLFolderViewFilter): Boolean = true
        }

        val itemA = TestItem(UUID.randomUUID(), "Alpha", 1000L)
        val itemB = TestItem(UUID.randomUUID(), "Beta", 2000L)

        // Alphabetical comparison
        assertTrue(sorter.compare(itemA, itemB))
        assertFalse(sorter.compare(itemB, itemA))
    }
}
