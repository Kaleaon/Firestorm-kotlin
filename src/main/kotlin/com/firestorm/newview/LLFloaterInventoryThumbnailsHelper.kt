package com.firestorm.newview

import java.util.UUID

class LLFloaterInventoryThumbnailsHelper private constructor(key: Any) : LLFloater("floater_inventory_thumbnails_helper") {

    private var mInventoryThumbnailsList: LLScrollListCtrl? = null
    private var mOutputLog: LLTextEditor? = null
    private var mPasteItemsBtn: LLUICtrl? = null
    private var mPasteTexturesBtn: LLUICtrl? = null
    private var mWriteThumbnailsBtn: LLUICtrl? = null
    private var mLogMissingThumbnailsBtn: LLUICtrl? = null
    private var mClearThumbnailsBtn: LLUICtrl? = null

    private val mItemNamesItems: MutableMap<String, LLViewerInventoryItem> = mutableMapOf()
    private val mTextureNamesIDs: MutableMap<String, UUID> = mutableMapOf()

    private enum class EListColumnNum(val value: Int) {
        NAME(0),
        EXISTING_TEXTURE(1),
        NEW_TEXTURE(2)
    }

    override fun postBuild(): Boolean {
        mInventoryThumbnailsList = getChild<LLScrollListCtrl>("inventory_thumbnails_list")
        mInventoryThumbnailsList!!.setAllowMultipleSelection(true)

        mOutputLog = getChild<LLTextEditor>("output_log")
        mOutputLog!!.setMaxTextLength(0xffff * 0x10)

        mPasteItemsBtn = getChild<LLUICtrl>("paste_items_btn")
        mPasteItemsBtn!!.setCommitCallback { onPasteItems() }
        mPasteItemsBtn!!.setEnabled(true)

        mPasteTexturesBtn = getChild<LLUICtrl>("paste_textures_btn")
        mPasteTexturesBtn!!.setCommitCallback { onPasteTextures() }
        mPasteTexturesBtn!!.setEnabled(true)

        mWriteThumbnailsBtn = getChild<LLUICtrl>("write_thumbnails_btn")
        mWriteThumbnailsBtn!!.setCommitCallback { onWriteThumbnails() }
        mWriteThumbnailsBtn!!.setEnabled(false)

        mLogMissingThumbnailsBtn = getChild<LLUICtrl>("log_missing_thumbnails_btn")
        mLogMissingThumbnailsBtn!!.setCommitCallback { onLogMissingThumbnails() }
        mLogMissingThumbnailsBtn!!.setEnabled(false)

        mClearThumbnailsBtn = getChild<LLUICtrl>("clear_thumbnails_btn")
        mClearThumbnailsBtn!!.setCommitCallback { onClearThumbnails() }
        mClearThumbnailsBtn!!.setEnabled(false)

        return true
    }

    private fun recordInventoryItemEntry(item: LLViewerInventoryItem) {
        val name = item.getName()
        if (!mItemNamesItems.containsKey(name)) {
            mItemNamesItems[name] = item
            writeToLog("ITEM ${mItemNamesItems.size}> $name\n", false)
        }
    }

    private fun onPasteItems() {
        if (!LLClipboard.instance().hasContents()) return

        writeToLog("\n==== Pasting items from inventory ====\n", false)

        val objects = mutableListOf<UUID>()
        LLClipboard.instance().pasteFromClipboard(objects)

        for (entry in objects) {
            val cat = gInventory.getCategory(entry)
            if (cat != null) {
                val catArray = mutableListOf<LLInventoryCategory>()
                val itemArray = mutableListOf<LLViewerInventoryItem>()

                gInventory.collectDescendentsIf(cat.getUUID(), catArray, itemArray,
                    LLInventoryModel.EXCLUDE_TRASH, LLIsType(LLAssetType.AT_OBJECT))
                gInventory.collectDescendentsIf(cat.getUUID(), catArray, itemArray,
                    LLInventoryModel.EXCLUDE_TRASH, LLIsType(LLAssetType.AT_BODYPART))
                gInventory.collectDescendentsIf(cat.getUUID(), catArray, itemArray,
                    LLInventoryModel.EXCLUDE_TRASH, LLIsType(LLAssetType.AT_CLOTHING))

                for (item in itemArray) {
                    recordInventoryItemEntry(item)
                }
            }

            val item = gInventory.getItem(entry)
            if (item != null) {
                val itemType = item.getType()
                if (itemType == LLAssetType.AT_OBJECT || itemType == LLAssetType.AT_BODYPART || itemType == LLAssetType.AT_CLOTHING) {
                    recordInventoryItemEntry(item)
                }
            }
        }

        updateDisplayList()
        updateButtonStates()
    }

    private fun recordTextureItemEntry(item: LLViewerInventoryItem) {
        val name = item.getName()
        if (!mTextureNamesIDs.containsKey(name)) {
            val id = item.getAssetUUID()
            mTextureNamesIDs[name] = id
            writeToLog("TEXTURE ${mTextureNamesIDs.size}> $name\n", false)
        }
    }

    private fun onPasteTextures() {
        if (!LLClipboard.instance().hasContents()) return

        writeToLog("\n==== Pasting textures from inventory ====\n", false)

        val objects = mutableListOf<UUID>()
        LLClipboard.instance().pasteFromClipboard(objects)

        for (entry in objects) {
            val cat = gInventory.getCategory(entry)
            if (cat != null) {
                val catArray = mutableListOf<LLInventoryCategory>()
                val itemArray = mutableListOf<LLViewerInventoryItem>()
                gInventory.collectDescendentsIf(cat.getUUID(), catArray, itemArray,
                    LLInventoryModel.EXCLUDE_TRASH, LLIsType(LLAssetType.AT_TEXTURE))
                for (item in itemArray) {
                    recordTextureItemEntry(item)
                }
            }

            val item = gInventory.getItem(entry)
            if (item != null && item.getType() == LLAssetType.AT_TEXTURE) {
                recordTextureItemEntry(item)
            }
        }

        updateDisplayList()
        updateButtonStates()
    }

    private fun updateDisplayList() {
        mInventoryThumbnailsList!!.deleteAllItems()

        for ((itemName, inventoryItem) in mItemNamesItems) {
            val existingThumbnailId = inventoryItem.getThumbnailUUID()
            val existingTextureName = if (existingThumbnailId != NULL_UUID) existingThumbnailId.toString() else "none"

            val newTextureName = if (mTextureNamesIDs.containsKey(itemName)) itemName else "missing"

            val row = buildListRow(itemName, existingTextureName, newTextureName)
            mInventoryThumbnailsList!!.addElement(row)
        }
    }

    private fun buildListRow(itemName: String, existingTextureName: String, newTextureName: String): Map<String, Any> {
        return mapOf(
            "columns" to listOf(
                mapOf("column" to "item_name", "type" to "text", "value" to itemName, "font" to mapOf("name" to "Monospace")),
                mapOf("column" to "existing_texture", "type" to "text", "value" to existingTextureName, "font" to mapOf("name" to "Monospace")),
                mapOf("column" to "new_texture", "type" to "text", "value" to newTextureName, "font" to mapOf("name" to "Monospace"))
            )
        )
    }

    private fun onWriteThumbnails() {
        LLNotificationsUtil.add("WriteInventoryThumbnailsWarning", emptyMap(), emptyMap()) { notif, resp ->
            val opt = LLNotificationsUtil.getSelectedOption(notif, resp)
            if (opt == 0) {
                for ((itemName, inventoryItem) in mItemNamesItems) {
                    val textureId = mTextureNamesIDs[itemName]
                    if (textureId != null) {
                        val itemId = inventoryItem.getUUID()
                        writeToLog("WRITING THUMB $itemName\nitem ID: $itemId\nthumbnail texture ID: $textureId\n", true)
                        inventoryItem.setThumbnailUUID(textureId)
                        writeInventoryThumbnailID(itemId, textureId)
                    }
                }
                updateDisplayList()
            }
        }
    }

    private fun onLogMissingThumbnails() {
        for ((itemName, inventoryItem) in mItemNamesItems) {
            val thumbnailId = inventoryItem.getThumbnailUUID()
            if (thumbnailId == NULL_UUID) {
                writeToLog("Missing thumbnail: $itemName\n", true)
            }
        }
    }

    private fun onClearThumbnails() {
        LLNotificationsUtil.add("ClearInventoryThumbnailsWarning", emptyMap(), emptyMap()) { notif, resp ->
            val opt = LLNotificationsUtil.getSelectedOption(notif, resp)
            if (opt == 0) {
                for ((_, inventoryItem) in mItemNamesItems) {
                    inventoryItem.setThumbnailUUID(NULL_UUID)
                    val itemId = inventoryItem.getUUID()
                    writeInventoryThumbnailID(itemId, NULL_UUID)
                }
                updateDisplayList()
            }
        }
    }

    private fun updateButtonStates() {
        var foundCount = 0
        for ((itemName, _) in mItemNamesItems) {
            if (mTextureNamesIDs.containsKey(itemName)) foundCount++
        }

        mWriteThumbnailsBtn!!.setEnabled(foundCount > 0)
        val hasItems = mItemNamesItems.isNotEmpty()
        mLogMissingThumbnailsBtn!!.setEnabled(hasItems)
        mClearThumbnailsBtn!!.setEnabled(hasItems)
    }

    private fun writeToLog(logline: String, prependNewline: Boolean) {
        mOutputLog!!.appendText(logline, prependNewline)
        mOutputLog!!.setCursorAndScrollToEnd()
    }

    companion object {
        private val NULL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        private fun writeInventoryThumbnailID(itemId: UUID, thumbnailAssetId: UUID): Boolean {
            System.err.println("LLFloaterInventoryThumbnailsHelper: writeInventoryThumbnailID not yet implemented")
            return false
        }
    }
}
