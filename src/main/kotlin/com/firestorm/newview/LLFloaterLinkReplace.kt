package com.firestorm.newview

import java.util.UUID

class LLInventoryLinkReplaceDropTarget(params: Any) : LLLineEditor(params) {

    private var mItemID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val mDADSignal: MutableList<(UUID) -> Unit> = mutableListOf()

    fun setDADCallback(cb: (UUID) -> Unit): () -> Unit {
        mDADSignal.add(cb)
        return { mDADSignal.remove(cb) }
    }

    override fun postBuild(): Boolean {
        setEnabled(false)
        return super.postBuild()
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: Int, cargoData: Any?,
        accept: Array<Int>, tooltipMsg: StringBuilder
    ): Boolean {
        val item = cargoData as? LLInventoryItem

        if (cargoType >= DAD_TEXTURE && cargoType <= DAD_LINK &&
            item != null &&
            item.getActualType() != LLAssetType.AT_LINK_FOLDER &&
            item.getType() != LLAssetType.AT_CATEGORY &&
            (LLAssetType.lookupCanLink(item.getType()) ||
                    (item.getType() == LLAssetType.AT_LINK && gInventory.getObject(item.getLinkedUUID()) == null))) {
            if (drop) {
                setItem(item)
                for (cb in mDADSignal) cb(mItemID)
            } else {
                accept[0] = ACCEPT_YES_SINGLE
            }
        } else {
            accept[0] = ACCEPT_NO
        }
        return true
    }

    fun getItemID(): UUID = mItemID

    fun setItem(item: LLInventoryItem?) {
        if (item != null) {
            mItemID = item.getLinkedUUID()
            setText(item.getName())
        } else {
            mItemID = UUID.fromString("00000000-0000-0000-0000-000000000000")
            setText("")
        }
    }
}


class LLFloaterLinkReplace(key: Any) : LLFloater(key), LLEventTimer(
    gSavedSettings.getFloat("LinkReplaceBatchPauseTime")
) {

    private var mSourceEditor: LLInventoryLinkReplaceDropTarget? = null
    private var mTargetEditor: LLInventoryLinkReplaceDropTarget? = null
    private var mStartBtn: LLButton? = null
    private var mRefreshBtn: LLButton? = null
    private var mStopBtn: LLButton? = null
    private var mStatusText: LLTextBox? = null
    private var mDeleteOnlyToggle: LLCheckBoxCtrl? = null

    private var mSourceUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var mTargetUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var mRemainingItems: UInt = 0u
    private val mBatchSize: UInt = gSavedSettings.getUInt("LinkReplaceBatchSize")
    private var mActiveItems: UInt = 0u
    private val mRemainingInventoryItems: MutableList<LLViewerInventoryItem> = mutableListOf()
    private var mDeleteOnly: Boolean = false
    private var mStopRequested: Boolean = false

    init {
        mEventTimer.stop()
    }

    fun postBuild(): Boolean {
        childSetVisible("delete_text", false)

        mStartBtn = getChild<LLButton>("btn_start")
        mStartBtn!!.setCommitCallback { onStartClicked() }

        mStopBtn = getChild<LLButton>("btn_stop")
        mStopBtn!!.setCommitCallback { onStopClicked() }
        mStopBtn!!.setEnabled(false)
        mStopRequested = false

        mRefreshBtn = getChild<LLButton>("btn_refresh")
        mRefreshBtn!!.setCommitCallback { checkEnableStart() }

        mDeleteOnlyToggle = getChild<LLCheckBoxCtrl>("delete_links_only")
        mDeleteOnlyToggle!!.setCommitCallback { onDeleteOnlyToggle() }

        mSourceEditor = getChild<LLInventoryLinkReplaceDropTarget>("source_uuid_editor")
        mTargetEditor = getChild<LLInventoryLinkReplaceDropTarget>("target_uuid_editor")

        mSourceEditor!!.setDADCallback { id -> onSourceItemDrop(id) }
        mTargetEditor!!.setDADCallback { id -> onTargetItemDrop(id) }

        mStatusText = getChild<LLTextBox>("status_text")

        return true
    }

    open fun onOpen(key: Any) {
        val keyUuid = runCatching { UUID.fromString(key.toString()) }.getOrNull()
        val nullUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (keyUuid != null && keyUuid != nullUuid) {
            val item = gInventory.getItem(keyUuid)
            if (item != null) {
                mSourceEditor!!.setItem(item)
                onSourceItemDrop(item.getLinkedUUID())
            }
        } else {
            checkEnableStart()
        }
    }

    open fun tick(): Boolean {
        val currentBatch = mutableListOf<LLViewerInventoryItem>()
        var i = 0u
        while (i < mBatchSize && mActiveItems <= 250u) {
            if (mRemainingInventoryItems.isEmpty()) {
                mEventTimer.stop()
                break
            }
            currentBatch.add(mRemainingInventoryItems.removeAt(mRemainingInventoryItems.size - 1))
            mActiveItems++
            i++
        }
        processBatch(currentBatch)
        return false
    }

    private fun checkEnableStart() {
        val nullUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (mSourceUUID != nullUuid && mTargetUUID != nullUuid && mSourceUUID == mTargetUUID) {
            mStatusText!!.setText(getString("ItemsIdentical"))
        } else if (mSourceUUID != nullUuid) {
            updateFoundLinks()
        }

        val enable = mRemainingItems > 0u && (
            (mDeleteOnly && mSourceUUID != nullUuid) ||
            (mSourceUUID != nullUuid && mTargetUUID != nullUuid && mSourceUUID != mTargetUUID)
        )
        mStartBtn!!.setEnabled(enable)
    }

    private fun onStartClicked() {
        val nullUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (!mDeleteOnly && (mSourceUUID == nullUuid || mTargetUUID == nullUuid)) return
        if (!mDeleteOnly && mSourceUUID == nullUuid) return
        if (mSourceUUID == mTargetUUID) return

        val sourceItemId = gInventory.getLinkedItemID(mSourceUUID)
        val sourceItem = gInventory.getItem(sourceItemId)
        val targetItemId = gInventory.getLinkedItemID(mTargetUUID)
        val targetItem = gInventory.getItem(targetItemId)

        val shouldPrompt = sourceItem != null && sourceItem.isWearableType() &&
                sourceItem.getWearableType() <= LLWearableType.WT_EYES

        if (shouldPrompt) {
            if (targetItem != null && targetItem.isWearableType() &&
                sourceItem!!.getWearableType() == targetItem.getWearableType()) {
                onStartClickedResponse(emptyMap(), mapOf("option" to 0))
            } else {
                val args = mapOf("TYPE" to LLWearableType.getInstance().getTypeName(sourceItem!!.getWearableType()))
                LLNotifications.instance().add("ConfirmReplaceLink", args) { notif, resp ->
                    onStartClickedResponse(notif, resp)
                }
            }
        } else {
            onStartClickedResponse(emptyMap(), mapOf("option" to 0))
        }
    }

    private fun onStartClickedResponse(notification: Map<String, Any>, response: Map<String, Any>) {
        if (LLNotificationsUtil.getSelectedOption(notification, response) == 0) {
            val catArray = mutableListOf<LLInventoryCategory>()
            mRemainingInventoryItems.clear()
            gInventory.collectDescendentsIf(
                gInventory.getRootFolderID(),
                catArray,
                mRemainingInventoryItems,
                LLInventoryModel.INCLUDE_TRASH,
                LLLinkedItemIDMatches(mSourceUUID)
            )

            if (mRemainingInventoryItems.isNotEmpty()) {
                val targetItem = gInventory.getItem(mTargetUUID)
                if (targetItem != null || mDeleteOnly) {
                    mRemainingItems = mRemainingInventoryItems.size.toUInt()
                    mStatusText!!.setText(getString("ItemsRemaining").replace("%NUM%", mRemainingItems.toString()))
                    mStartBtn!!.setEnabled(false)
                    mRefreshBtn!!.setEnabled(false)
                    mStopBtn!!.setEnabled(true)
                    mStopRequested = false
                    mEventTimer.start()
                    tick()
                } else {
                    mStatusText!!.setText(getString("TargetNotFound"))
                }
            }
        }
    }

    private fun onStopClicked() {
        mStopRequested = true
    }

    private fun onSourceItemDrop(sourceItemId: UUID) {
        mSourceUUID = sourceItemId
        checkEnableStart()
    }

    private fun onTargetItemDrop(targetItemId: UUID) {
        mTargetUUID = targetItemId
        checkEnableStart()
    }

    private fun onDeleteOnlyToggle() {
        val enabled = mDeleteOnlyToggle!!.getValue() as? Boolean ?: false
        mTargetEditor!!.setEnabled(!enabled)
        mTargetEditor!!.setVisible(!enabled)
        childSetVisible("target_label", !enabled)
        childSetVisible("delete_text", enabled)
        mDeleteOnly = enabled
        if (mDeleteOnly) {
            mTargetEditor!!.setItem(null)
            mTargetUUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        }
        checkEnableStart()
    }

    private fun updateFoundLinks() {
        val items = mutableListOf<LLViewerInventoryItem>()
        val catArray = mutableListOf<LLInventoryCategory>()
        gInventory.collectDescendentsIf(
            gInventory.getRootFolderID(),
            catArray,
            items,
            LLInventoryModel.INCLUDE_TRASH,
            LLLinkedItemIDMatches(mSourceUUID)
        )
        mRemainingItems = items.size.toUInt()
        mStatusText!!.setText(getString("ItemsFound").replace("%NUM%", mRemainingItems.toString()))
    }

    private fun decreaseOpenItemCount() {
        mActiveItems--
        mRemainingItems--

        if (mRemainingItems == 0u || mStopRequested) {
            mRemainingItems = 0u
            mRemainingInventoryItems.clear()
            mStatusText!!.setText(getString("ReplaceFinished"))
            mStartBtn!!.setEnabled(true)
            mRefreshBtn!!.setEnabled(true)
            mStopBtn!!.setEnabled(false)
            mEventTimer.stop()
        } else {
            mStatusText!!.setText(getString("ItemsRemaining").replace("%NUM%", mRemainingItems.toString()))
        }
    }

    private fun processBatch(items: List<LLViewerInventoryItem>) {
        val targetItem = gInventory.getItem(mTargetUUID)
        val cofFolderId = gInventory.findCategoryUUIDForType(LLFolderType.FT_CURRENT_OUTFIT)
        val outfitFolderId = gInventory.findCategoryUUIDForType(LLFolderType.FT_MY_OUTFITS)
        val nullUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")

        for (sourceItem in items) {
            if (mStopRequested) {
                decreaseOpenItemCount()
                break
            } else if (sourceItem.getParentUUID() != cofFolderId) {
                val isOutfitFolder = gInventory.isObjectDescendentOf(sourceItem.getParentUUID(), outfitFolderId)
                val needsWearableOrderingUpdate = (isOutfitFolder && sourceItem.getType() == LLAssetType.AT_CLOTHING) ||
                        (!mDeleteOnly && targetItem?.getType() == LLAssetType.AT_CLOTHING)
                val needsDescriptionUpdate = !mDeleteOnly && isOutfitFolder &&
                        targetItem?.getType() != LLAssetType.AT_CLOTHING

                if (!mDeleteOnly) {
                    val floaterHandle = getDerivedHandle<LLFloaterLinkReplace>()
                    TODO("APR: use JVM equivalent — call link_inventory_array and wire linkCreatedCallback")
                } else {
                    val outfitUpdateFolderId = if (needsWearableOrderingUpdate) sourceItem.getParentUUID() else nullUuid
                    val floaterHandle = getDerivedHandle<LLFloaterLinkReplace>()
                    TODO("APR: use JVM equivalent — call remove_inventory_object and wire itemRemovedCallback")
                }
            } else {
                decreaseOpenItemCount()
            }
        }
    }

    companion object {
        fun linkCreatedCallback(
            floaterHandle: LLHandle<LLFloaterLinkReplace>,
            oldItemId: UUID,
            targetItemId: UUID,
            needsWearableOrderingUpdate: Boolean,
            needsDescriptionUpdate: Boolean,
            outfitFolderId: UUID
        ) {
            val nullUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")

            if (needsDescriptionUpdate && outfitFolderId != nullUuid) {
                val items = mutableListOf<LLViewerInventoryItem>()
                val cats = mutableListOf<LLInventoryCategory>()
                gInventory.collectDescendentsIf(
                    outfitFolderId, cats, items,
                    LLInventoryModel.EXCLUDE_TRASH,
                    LLLinkedItemIDMatches(targetItemId)
                )
                for (item in items) {
                    if ((item.getType() == LLAssetType.AT_BODYPART ||
                        item.getType() == LLAssetType.AT_OBJECT ||
                        item.getType() == LLAssetType.AT_GESTURE) &&
                        item.getActualDescription().isNotEmpty()) {
                        val updates = mapOf("desc" to "")
                        TODO("APR: use JVM equivalent — call update_inventory_item with empty description to clear dirty outfit state")
                    }
                }
            }

            val outfitUpdateFolder = if (needsWearableOrderingUpdate && outfitFolderId != nullUuid) outfitFolderId else nullUuid
            TODO("APR: use JVM equivalent — call remove_inventory_object then itemRemovedCallback")
        }

        fun itemRemovedCallback(floaterHandle: LLHandle<LLFloaterLinkReplace>, outfitFolderId: UUID) {
            val nullUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")
            if (outfitFolderId != nullUuid) {
                LLAppearanceMgr.getInstance().updateClothingOrderingInfo(outfitFolderId)
            }
            if (!floaterHandle.isDead()) {
                floaterHandle.get().decreaseOpenItemCount()
            }
        }
    }
}
