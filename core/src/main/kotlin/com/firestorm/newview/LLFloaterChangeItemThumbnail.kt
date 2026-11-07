package com.firestorm.newview

import java.util.UUID

private class LLThumbnailImagePicker(
    private val inventoryId: UUID,
    private val taskId: UUID = UUID(0L, 0L),
    private val callback: (UUID) -> Unit
) : LLFilePickerThread(LLFilePicker.FFLOAD_IMAGE) {

    override fun notify(filenames: MutableList<String>) {
        if (filenames.isEmpty()) return
        val filePath = filenames[0]
        if (filePath.isEmpty()) return
        LLFloaterSimpleSnapshot.uploadThumbnail(filePath, inventoryId, taskId, callback)
    }
}

private class ImageLoadedData {
    var thumbnailId: UUID = UUID(0L, 0L)
    var taskId: UUID = UUID(0L, 0L)
    val itemIds: MutableSet<UUID> = mutableSetOf()
    var floaterHandle: LLHandle<LLFloater>? = null
    var silent: Boolean = false
    var texturep: LLViewerFetchedTexture? = null
}

private class LLIsOutfitTextureType : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        return item != null && item.getType() == LLAssetType.AT_TEXTURE
    }
}

class LLFloaterChangeItemThumbnail(key: LLSD) :
    LLFloater(key),
    LLInventoryObserver,
    LLVOInventoryListener {

    enum class EToolTipState {
        TOOLTIP_NONE,
        TOOLTIP_UPLOAD_LOCAL,
        TOOLTIP_UPLOAD_SNAPSHOT,
        TOOLTIP_USE_TEXTURE,
        TOOLTIP_COPY_TO_CLIPBOARD,
        TOOLTIP_COPY_FROM_CLIPBOARD,
        TOOLTIP_REMOVE
    }

    private var mObserverInitialized: Boolean = false
    private var mMultipleThumbnails: Boolean = false
    private var mTooltipState: EToolTipState = EToolTipState.TOOLTIP_NONE
    private val mItemList: MutableSet<UUID> = mutableSetOf()
    private var mTaskId: UUID = UUID(0L, 0L)
    private var mExpectingAssetId: UUID = UUID(0L, 0L)

    private var mItemTypeIcon: LLIconCtrl? = null
    private var mItemNameText: LLUICtrl? = null
    private var mThumbnailCtrl: LLThumbnailCtrl? = null
    private var mToolTipTextBox: LLTextBox? = null
    private var mMultipleTextBox: LLTextBox? = null
    private var mCopyToClipboardBtn: LLButton? = null
    private var mPasteFromClipboardBtn: LLButton? = null
    private var mRemoveImageBtn: LLButton? = null

    private var mPickerHandle: LLHandle<LLFloater>? = null
    private var mSnapshotHandle: LLHandle<LLFloater>? = null

    override fun onDestroy() {
        gInventory.removeObserver(this)
        removeVOInventoryListener()
        super.onDestroy()
    }

    override fun postBuild(): Boolean {
        mItemNameText = getChild<LLUICtrl>("item_name")
        mItemTypeIcon = getChild<LLIconCtrl>("item_type_icon")
        mThumbnailCtrl = getChild<LLThumbnailCtrl>("item_thumbnail")
        mToolTipTextBox = getChild<LLTextBox>("tooltip_text")
        mMultipleTextBox = getChild<LLTextBox>("multiple_lbl")

        mToolTipTextBox?.setValue(LLSD())
        mMultipleTextBox?.setVisible(false)

        val uploadLocal = getChild<LLButton>("upload_local")
        uploadLocal.setClickedCallback { onUploadLocal(this) }
        uploadLocal.setMouseEnterCallback { button, param -> onButtonMouseEnter(button, param, EToolTipState.TOOLTIP_UPLOAD_LOCAL) }
        uploadLocal.setMouseLeaveCallback { button, param -> onButtonMouseLeave(button, param, EToolTipState.TOOLTIP_UPLOAD_LOCAL) }

        val uploadSnapshot = getChild<LLButton>("upload_snapshot")
        uploadSnapshot.setClickedCallback { onUploadSnapshot(this) }
        uploadSnapshot.setMouseEnterCallback { button, param -> onButtonMouseEnter(button, param, EToolTipState.TOOLTIP_UPLOAD_SNAPSHOT) }
        uploadSnapshot.setMouseLeaveCallback { button, param -> onButtonMouseLeave(button, param, EToolTipState.TOOLTIP_UPLOAD_SNAPSHOT) }

        val useTexture = getChild<LLButton>("use_texture")
        useTexture.setClickedCallback { onUseTexture(this) }
        useTexture.setMouseEnterCallback { button, param -> onButtonMouseEnter(button, param, EToolTipState.TOOLTIP_USE_TEXTURE) }
        useTexture.setMouseLeaveCallback { button, param -> onButtonMouseLeave(button, param, EToolTipState.TOOLTIP_USE_TEXTURE) }

        mCopyToClipboardBtn = getChild<LLButton>("copy_to_clipboard")
        mCopyToClipboardBtn?.setClickedCallback { onCopyToClipboard(this) }
        mCopyToClipboardBtn?.setMouseEnterCallback { button, param -> onButtonMouseEnter(button, param, EToolTipState.TOOLTIP_COPY_TO_CLIPBOARD) }
        mCopyToClipboardBtn?.setMouseLeaveCallback { button, param -> onButtonMouseLeave(button, param, EToolTipState.TOOLTIP_COPY_TO_CLIPBOARD) }

        mPasteFromClipboardBtn = getChild<LLButton>("paste_from_clipboard")
        mPasteFromClipboardBtn?.setClickedCallback { onPasteFromClipboard(this) }
        mPasteFromClipboardBtn?.setMouseEnterCallback { button, param -> onButtonMouseEnter(button, param, EToolTipState.TOOLTIP_COPY_FROM_CLIPBOARD) }
        mPasteFromClipboardBtn?.setMouseLeaveCallback { button, param -> onButtonMouseLeave(button, param, EToolTipState.TOOLTIP_COPY_FROM_CLIPBOARD) }

        mRemoveImageBtn = getChild<LLButton>("remove_image")
        mRemoveImageBtn?.setClickedCallback { onRemove(this) }
        mRemoveImageBtn?.setMouseEnterCallback { button, param -> onButtonMouseEnter(button, param, EToolTipState.TOOLTIP_REMOVE) }
        mRemoveImageBtn?.setMouseLeaveCallback { button, param -> onButtonMouseLeave(button, param, EToolTipState.TOOLTIP_REMOVE) }

        return super.postBuild()
    }

    override fun onOpen(key: LLSD) {
        if (!key.has("item_id") && !key.isUUID() && !key.isArray()) {
            closeFloater()
            return
        }

        mItemList.clear()
        mMultipleThumbnails = false

        if (key.isArray()) {
            if (key.size() > 50) {
                LLNotificationsUtil.add("ThumbnailSelectionTooLarge")
                closeFloater()
                return
            }

            var imageId: UUID = UUID(0L, 0L)
            for (item in key.arrayIterator()) {
                val obj = gInventory.getObject(item.asUUID())
                if (obj != null) {
                    if (mItemList.isEmpty()) {
                        imageId = obj.getThumbnailUUID()
                    }
                    mItemList.add(item.asUUID())
                    if (imageId != obj.getThumbnailUUID()) {
                        mMultipleThumbnails = true
                    }
                }
            }
        } else if (key.isUUID()) {
            mItemList.add(key.asUUID())
        } else {
            mItemList.add(key["item_id"].asUUID())
            mTaskId = key["task_id"].asUUID()
        }

        if (mItemList.isEmpty()) {
            closeFloater()
            return
        }

        refreshFromInventory()
    }

    override fun onFocusReceived() {
        mPasteFromClipboardBtn?.setEnabled(LLClipboard.instance().hasContents())
    }

    override fun onMouseEnter(x: Int, y: Int, mask: Int) {
        mPasteFromClipboardBtn?.setEnabled(LLClipboard.instance().hasContents())
    }

    override fun handleDragAndDrop(
        x: Int,
        y: Int,
        mask: Int,
        drop: Boolean,
        cargoType: EDragAndDropType,
        cargoData: Any?,
        accept: Array<EAcceptance>,
        tooltipMsg: StringBuilder
    ): Boolean {
        if (cargoType == EDragAndDropType.DAD_TEXTURE) {
            val item = cargoData as? LLInventoryItem
            if (item != null && item.getAssetUUID() != UUID(0L, 0L)) {
                if (drop) {
                    assignAndValidateAsset(item.getAssetUUID())
                }
                accept[0] = EAcceptance.ACCEPT_YES_SINGLE
            } else {
                accept[0] = EAcceptance.ACCEPT_NO
            }
        } else {
            accept[0] = EAcceptance.ACCEPT_NO
        }
        return true
    }

    override fun changed(mask: UInt) {
        if (mTaskId != UUID(0L, 0L) || mItemList.isEmpty()) {
            return
        }

        val changedItemIds = gInventory.getChangedIDs()
        val expectedId = mItemList.first()

        for (id in changedItemIds) {
            if (id == expectedId) {
                val interestingMask = LLInventoryObserver.LABEL or
                        LLInventoryObserver.INTERNAL or
                        LLInventoryObserver.REMOVE
                if ((mask and interestingMask.toUInt()) != 0u) {
                    refreshFromInventory()
                }
            }
        }
    }

    override fun inventoryChanged(
        obj: LLViewerObject,
        inventory: MutableList<LLInventoryObject>?,
        serialNum: Int,
        userData: Any?
    ) {
        refreshFromInventory()
    }

    private fun getInventoryObject(): LLInventoryObject? {
        if (mItemList.isEmpty()) return null

        val itemId = mItemList.first()

        return if (mTaskId == UUID(0L, 0L)) {
            if (!mObserverInitialized) {
                gInventory.addObserver(this)
                mObserverInitialized = true
            }
            gInventory.getObject(itemId)
        } else {
            val viewerObject = gObjectList.findObject(mTaskId)
            if (viewerObject != null) {
                if (!mObserverInitialized) {
                    registerVOInventoryListener(viewerObject, null)
                    mObserverInitialized = false
                }
                viewerObject.getInventoryObject(itemId)
            } else {
                null
            }
        }
    }

    private fun refreshFromInventory() {
        val obj = getInventoryObject()
        if (obj == null) {
            closeFloater()
            return
        }

        val trashId = gInventory.findCategoryUUIDForType(LLFolderType.FT_TRASH)
        val inTrash = gInventory.isObjectDescendentOf(obj.getUUID(), trashId)
        if (inTrash && obj.getUUID() != trashId) {
            closeFloater()
        } else {
            refreshFromObject(obj)
        }
    }

    private fun refreshFromObject(obj: LLInventoryObject) {
        var iconImg: LLUIImagePtr? = null
        var thumbnailId = obj.getThumbnailUUID()

        val item = obj as? LLViewerInventoryItem
        if (item != null) {
            setTitle(getString("title_item_thumbnail"))
            iconImg = LLInventoryIcon.getIcon(item.getType(), item.getInventoryType(), item.getFlags(), false)
            val isNotSelfTexture = item.getActualType() != LLAssetType.AT_TEXTURE || item.getAssetUUID() != thumbnailId
            mRemoveImageBtn?.setEnabled(thumbnailId != UUID(0L, 0L) && isNotSelfTexture)
        } else {
            val cat = obj as? LLViewerInventoryCategory
            if (cat != null) {
                setTitle(getString("title_folder_thumbnail"))
                iconImg = LLUI.getUIImage(LLViewerFolderType.lookupIconName(cat.getPreferredType(), true))

                if (thumbnailId == UUID(0L, 0L) && cat.getPreferredType() == LLFolderType.FT_OUTFIT) {
                    val cats = mutableListOf<LLInventoryCategory>()
                    val items = mutableListOf<LLInventoryItem>()
                    val f = LLIsOutfitTextureType()
                    gInventory.getDirectDescendentsOf(mItemList.first(), cats, items, f)

                    if (items.size == 1) {
                        var outfitItem: LLViewerInventoryItem? = items.first() as? LLViewerInventoryItem
                        if (outfitItem?.getIsLinkType() == true) {
                            outfitItem = outfitItem.getLinkedItem()
                        }
                        if (outfitItem != null) {
                            thumbnailId = outfitItem.getAssetUUID()
                            if (thumbnailId != UUID(0L, 0L)) {
                                assignAndValidateAsset(thumbnailId, true)
                            }
                        }
                    }
                }

                mRemoveImageBtn?.setEnabled(thumbnailId != UUID(0L, 0L))
            }
        }

        if (mItemList.size == 1) {
            mItemTypeIcon?.setImage(iconImg)
            mItemTypeIcon?.setVisible(true)
            mMultipleTextBox?.setVisible(false)
            mItemNameText?.setValue(obj.getName())
            mItemNameText?.setToolTip("")
        } else {
            mItemTypeIcon?.setVisible(false)
            mMultipleTextBox?.setVisible(mMultipleThumbnails)
            mItemNameText?.setValue(getString("multiple_item_names"))

            val itemsToShow = 5
            val sb = StringBuilder()
            var count = 0
            for (id in mItemList) {
                if (count >= itemsToShow) break
                val pobj = gInventory.getObject(id)
                if (pobj != null) {
                    sb.append(pobj.getName())
                    sb.append('\n')
                }
                count++
            }
            if (mItemList.size > itemsToShow) {
                sb.append("...")
            }
            mItemNameText?.setToolTip(sb.toString())
        }

        mThumbnailCtrl?.setValue(thumbnailId)
        mCopyToClipboardBtn?.setEnabled(thumbnailId != UUID(0L, 0L) && !mMultipleThumbnails)
        mPasteFromClipboardBtn?.setEnabled(LLClipboard.instance().hasContents())
    }

    private fun assignAndValidateAsset(assetId: UUID, silent: Boolean = false) {
        val texturep = LLViewerTextureManager.getFetchedTexture(assetId)
        when {
            texturep.isMissingAsset() -> {
                if (!silent) LLNotificationsUtil.add("ThumbnailDimentionsLimit")
            }
            texturep.getFullWidth() == 0 -> {
                mExpectingAssetId = if (silent) UUID(0L, 0L) else assetId

                val data = ImageLoadedData()
                data.taskId = mTaskId
                data.itemIds.addAll(mItemList)
                data.thumbnailId = assetId
                data.floaterHandle = getHandle()
                data.silent = silent
                data.texturep = texturep

                texturep.setLoadedCallback(
                    { success, srcVi, src, auxSrc, discardLevel, final_, userData ->
                        onImageDataLoaded(success, srcVi, src, auxSrc, discardLevel, final_, userData)
                    },
                    MAX_DISCARD_LEVEL,
                    false,
                    false,
                    data,
                    null,
                    false
                )
            }
            else -> {
                if (validateAsset(assetId)) {
                    setThumbnailId(assetId)
                } else if (!silent) {
                    LLNotificationsUtil.add("ThumbnailDimentionsLimit")
                }
            }
        }
    }

    private fun showTexturePicker(thumbnailId: UUID) {
        getWindow()?.setCursor(UI_CURSOR_WAIT)

        val existingPicker = mPickerHandle?.get()
        if (existingPicker != null) {
            existingPicker.openFloater()
        } else {
            val floaterp = LLFloaterTexturePicker(
                this,
                thumbnailId, thumbnailId, thumbnailId,
                false, true,
                LLTrans.getString("TexturePickerOutfitHeader"),
                PERM_NONE, PERM_NONE,
                false, null,
                PICK_TEXTURE
            )
            mPickerHandle = floaterp.getHandle()

            val textureFloaterp = floaterp as? LLFloaterTexturePicker
            textureFloaterp?.setOnFloaterCommitCallback { op, _, _, _, _ ->
                if (op == LLTextureCtrl.ETexturePickOp.TEXTURE_SELECT) {
                    onTexturePickerCommit()
                }
            }
            textureFloaterp?.setLocalTextureEnabled(false)
            textureFloaterp?.setBakeTextureEnabled(false)
            textureFloaterp?.setCanApplyImmediately(false)
            textureFloaterp?.setCanApply(false, true, false)
            textureFloaterp?.setMinDimentionsLimits(LLFloaterSimpleSnapshot.THUMBNAIL_SNAPSHOT_DIM_MIN)

            addDependentFloater(floaterp)
            floaterp.openFloater()
        }

        mPickerHandle?.get()?.setFocus(true)
    }

    private fun onTexturePickerCommit() {
        val floaterp = mPickerHandle?.get() as? LLFloaterTexturePicker ?: return
        val assetId = floaterp.getAssetID()

        if (assetId == UUID(0L, 0L)) {
            setThumbnailId(assetId)
            return
        }

        val obj = getInventoryObject()
        if (obj?.getThumbnailUUID() == assetId) return

        val texturep = LLViewerTextureManager.findFetchedTexture(assetId, TEX_LIST_STANDARD) ?: return
        if (texturep.isMissingAsset()) return

        if (texturep.getFullWidth() != texturep.getFullHeight()) {
            LLNotificationsUtil.add("ThumbnailDimentionsLimit")
            return
        }

        if (texturep.getFullWidth() < LLFloaterSimpleSnapshot.THUMBNAIL_SNAPSHOT_DIM_MIN && texturep.getFullWidth() > 0) {
            LLNotificationsUtil.add("ThumbnailDimentionsLimit")
            return
        }

        if (texturep.getFullWidth() > LLFloaterSimpleSnapshot.THUMBNAIL_SNAPSHOT_DIM_MAX || texturep.getFullWidth() == 0) {
            if (texturep.isFullyLoaded() && texturep.getRawImageLevel() == 0 && texturep.isRawImageValid()) {
                val taskId = mTaskId
                val inventoryIds = mItemList.toMutableSet()
                val handle = getHandle()
                val callback: (UUID) -> Unit = { uploadedAssetId ->
                    onUploadComplete(uploadedAssetId, taskId, inventoryIds, handle)
                }
                LLFloaterSimpleSnapshot.uploadThumbnail(texturep.getRawImage(), mItemList.first(), mTaskId, callback)
            } else {
                val data = ImageLoadedData()
                data.taskId = mTaskId
                data.itemIds.addAll(mItemList)
                data.thumbnailId = assetId
                data.floaterHandle = getHandle()
                data.silent = false
                data.texturep = texturep

                texturep.setBoostLevel(LLGLTexture.BOOST_PREVIEW)
                texturep.setMinDiscardLevel(0)
                texturep.setLoadedCallback(
                    { success, srcVi, src, auxSrc, discardLevel, final_, userData ->
                        onFullImageLoaded(success, srcVi, src, auxSrc, discardLevel, final_, userData)
                    },
                    0, true, false, data, null, false
                )
                texturep.forceToSaveRawImage(0)
            }
            return
        }

        setThumbnailId(assetId)
    }

    private fun setThumbnailId(newThumbnailId: UUID) {
        val obj = getInventoryObject() ?: return

        if (mTaskId != UUID(0L, 0L)) {
            error("Not implemented yet")
        }

        for (id in mItemList) {
            setThumbnailId(newThumbnailId, id, obj)
        }
    }

    private fun onButtonMouseEnter(button: LLUICtrl, param: LLSD, state: EToolTipState) {
        mTooltipState = state
        val tooltipName = "tooltip_" + button.getName()
        val tooltipText = if (hasString(tooltipName)) getString(tooltipName) else ""
        mToolTipTextBox?.setValue(tooltipText)
    }

    private fun onButtonMouseLeave(button: LLUICtrl, param: LLSD, state: EToolTipState) {
        if (mTooltipState == state) {
            mTooltipState = EToolTipState.TOOLTIP_NONE
            mToolTipTextBox?.setValue(LLSD())
        }
    }

    companion object {

        fun validateAsset(assetId: UUID): Boolean {
            if (assetId == UUID(0L, 0L)) return false
            val texturep = LLViewerTextureManager.findFetchedTexture(assetId, TEX_LIST_STANDARD) ?: return false
            if (texturep.isMissingAsset()) return false
            if (texturep.getFullWidth() != texturep.getFullHeight()) return false
            if (texturep.getFullWidth() > LLFloaterSimpleSnapshot.THUMBNAIL_SNAPSHOT_DIM_MAX ||
                texturep.getFullHeight() > LLFloaterSimpleSnapshot.THUMBNAIL_SNAPSHOT_DIM_MAX) return false
            if (texturep.getFullWidth() < LLFloaterSimpleSnapshot.THUMBNAIL_SNAPSHOT_DIM_MIN ||
                texturep.getFullHeight() < LLFloaterSimpleSnapshot.THUMBNAIL_SNAPSHOT_DIM_MIN) return false
            return true
        }

        private fun onUploadLocal(self: LLFloaterChangeItemThumbnail) {
            val taskId = self.mTaskId
            val inventoryIds = self.mItemList.toMutableSet()
            val handle = self.getHandle()

            LLThumbnailImagePicker(
                self.mItemList.first(),
                self.mTaskId
            ) { assetId -> onUploadComplete(assetId, taskId, inventoryIds, handle) }.getFile()

            self.mPickerHandle?.get()?.closeFloater()
            self.mSnapshotHandle?.get()?.closeFloater()
        }

        private fun onUploadSnapshot(self: LLFloaterChangeItemThumbnail) {
            val existingSnapshot = self.mSnapshotHandle?.get()
            if (existingSnapshot != null) {
                existingSnapshot.openFloater()
            } else {
                val key = LLSD()
                key["item_id"] = self.mItemList.first()
                key["task_id"] = self.mTaskId
                val snapshotFloater = LLFloaterReg.showInstance("simple_snapshot", key, true) as? LLFloaterSimpleSnapshot
                if (snapshotFloater != null) {
                    self.addDependentFloater(snapshotFloater)
                    self.mSnapshotHandle = snapshotFloater.getHandle()
                    snapshotFloater.setOwner(self)
                    val taskId = self.mTaskId
                    val inventoryIds = self.mItemList.toMutableSet()
                    val handle = self.getHandle()
                    snapshotFloater.setComplectionCallback { assetId ->
                        onUploadComplete(assetId, taskId, inventoryIds, handle)
                    }
                }
            }

            self.mPickerHandle?.get()?.closeFloater()
        }

        private fun onUseTexture(self: LLFloaterChangeItemThumbnail) {
            val obj = self.getInventoryObject()
            if (obj != null) {
                self.showTexturePicker(obj.getThumbnailUUID())
            }
            self.mSnapshotHandle?.get()?.closeFloater()
        }

        private fun onCopyToClipboard(self: LLFloaterChangeItemThumbnail) {
            val obj = self.getInventoryObject()
            if (obj != null) {
                LLClipboard.instance().reset()
                LLClipboard.instance().addToClipboard(obj.getThumbnailUUID(), LLAssetType.AT_NONE)
                self.mPasteFromClipboardBtn?.setEnabled(true)
            }
        }

        private fun onPasteFromClipboard(self: LLFloaterChangeItemThumbnail) {
            val objects = mutableListOf<UUID>()
            LLClipboard.instance().pasteFromClipboard(objects)
            if (objects.isEmpty()) return

            val potentialUuid = objects[0]
            var assetId: UUID = UUID(0L, 0L)

            if (potentialUuid != UUID(0L, 0L)) {
                val item = gInventory.getItem(potentialUuid)
                if (item != null) {
                    if (item.getType() == LLAssetType.AT_TEXTURE) {
                        val copy = item.getPermissions().allowCopyBy(gAgent.getID())
                        val xfer = item.getPermissions().allowOperationBy(PERM_TRANSFER, gAgent.getID())
                        if (copy && xfer) {
                            assetId = item.getAssetUUID()
                        } else {
                            LLNotificationsUtil.add("ThumbnailInsufficientPermissions")
                            return
                        }
                    }
                } else {
                    assetId = potentialUuid
                }
            }

            val obj = self.getInventoryObject()
            if (obj != null && obj.getThumbnailUUID() == assetId) return

            if (assetId != UUID(0L, 0L)) {
                self.assignAndValidateAsset(assetId)
            }
        }

        private fun onRemove(self: LLFloaterChangeItemThumbnail) {
            val payload = LLSD()
            payload["item_id"] = self.mItemList.first()
            payload["object_id"] = self.mTaskId
            LLNotificationsUtil.add("DeleteThumbnail", LLSD(), payload) { notification, response ->
                onRemovalConfirmation(notification, response, self.getHandle())
            }
        }

        private fun onRemovalConfirmation(
            notification: LLSD,
            response: LLSD,
            handle: LLHandle<LLFloater>?
        ) {
            val option = LLNotificationsUtil.getSelectedOption(notification, response)
            if (option == 0 && handle?.isDead() == false && handle.get()?.isDead() == false) {
                val self = handle.get() as? LLFloaterChangeItemThumbnail
                self?.setThumbnailId(UUID(0L, 0L))
            }
        }

        private fun onImageDataLoaded(
            success: Boolean,
            srcVi: LLViewerFetchedTexture?,
            src: LLImageRaw?,
            auxSrc: LLImageRaw?,
            discardLevel: Int,
            final_: Boolean,
            userData: Any?
        ) {
            val data = userData as? ImageLoadedData ?: return
            if (!final_ && success) return

            if (success) {
                if (validateAsset(data.thumbnailId)) {
                    for (id in data.itemIds) {
                        setThumbnailId(data.thumbnailId, data.taskId, id)
                    }
                } else if (!data.silent) {
                    LLNotificationsUtil.add("ThumbnailDimentionsLimit")
                }
            }

            if (!data.silent && data.floaterHandle?.isDead() == false) {
                val self = data.floaterHandle?.get() as? LLFloaterChangeItemThumbnail
                if (self?.mExpectingAssetId == data.thumbnailId) {
                    self.mExpectingAssetId = UUID(0L, 0L)
                }
            }
        }

        private fun onFullImageLoaded(
            success: Boolean,
            srcVi: LLViewerFetchedTexture?,
            src: LLImageRaw?,
            auxSrc: LLImageRaw?,
            discardLevel: Int,
            final_: Boolean,
            userData: Any?
        ) {
            val data = userData as? ImageLoadedData ?: return
            if (!final_ && success) return

            if (success && srcVi != null) {
                val w = srcVi.getFullWidth()
                val h = srcVi.getFullHeight()
                when {
                    w != h || w < LLFloaterSimpleSnapshot.THUMBNAIL_SNAPSHOT_DIM_MIN -> {
                        if (!data.silent) LLNotificationsUtil.add("ThumbnailDimentionsLimit")
                    }
                    w > LLFloaterSimpleSnapshot.THUMBNAIL_SNAPSHOT_DIM_MAX -> {
                        val taskId = data.taskId
                        val inventoryIds = data.itemIds.toMutableSet()
                        val handle = data.floaterHandle
                        LLFloaterSimpleSnapshot.uploadThumbnail(
                            src,
                            data.itemIds.first(),
                            taskId
                        ) { assetId -> onUploadComplete(assetId, taskId, inventoryIds, handle) }
                    }
                    else -> {
                        for (id in data.itemIds) {
                            setThumbnailId(data.thumbnailId, data.taskId, id)
                        }
                    }
                }
            }
        }

        fun onUploadComplete(
            assetId: UUID,
            taskId: UUID,
            inventoryIds: MutableSet<UUID>,
            handle: LLHandle<LLFloater>?
        ) {
            if (assetId == UUID(0L, 0L)) return

            val iter = inventoryIds.iterator()
            if (!iter.hasNext()) return

            iter.next() // first element was set by upload
            while (iter.hasNext()) {
                setThumbnailId(assetId, taskId, iter.next())
            }

            if (handle?.isDead() == false) {
                val floater = handle.get() as? LLFloaterChangeItemThumbnail
                floater?.mMultipleThumbnails = false
                floater?.mMultipleTextBox?.setVisible(false)
            }
        }

        private fun setThumbnailId(newThumbnailId: UUID, taskId: UUID, invObjId: UUID) {
            if (taskId != UUID(0L, 0L)) return
            val obj = gInventory.getObject(invObjId) ?: return
            setThumbnailId(newThumbnailId, invObjId, obj)
        }

        private fun setThumbnailId(newThumbnailId: UUID, invObjId: UUID, obj: LLInventoryObject) {
            if (obj.getThumbnailUUID() == newThumbnailId) return

            val updates = LLSD()
            if (newThumbnailId != UUID(0L, 0L)) {
                updates["thumbnail"] = LLSD().with("asset_id", newThumbnailId.toString())
            } else {
                updates["thumbnail"] = LLSD()
            }

            if (obj is LLViewerInventoryCategory) {
                update_inventory_category(invObjId, updates, null)
            }
            if (obj is LLViewerInventoryItem) {
                update_inventory_item(invObjId, updates, null)
            }
        }
    }
}
