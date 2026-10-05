package com.firestorm.newview

import java.util.UUID

data class FSAssetResourceData(
    val name: String,
    val description: String,
    val userData: Any?,
    val uuid: UUID
)

class FSFloaterObjectExport(key: LLSD) : LLFloater(key), LLVOInventoryListener {

    companion object {
        const val MAX_TEXTURE_WAIT_TIME: Float = 30f
        const val MAX_INVENTORY_WAIT_TIME: Float = 30f
        const val MAX_ASSET_WAIT_TIME: Float = 60f
    }

    enum class FSExportState { IDLE, INVENTORY_DOWNLOAD, ASSET_DOWNLOAD, TEXTURE_DOWNLOAD }

    private var exportState: FSExportState = FSExportState.IDLE
    private val objects: MutableList<Pair<LLViewerObject, String>> = mutableListOf()
    private var filename: String = ""
    private var objectSelection: LLObjectSelectionHandle? = null
    private var currentObjectId: UUID = UUID(0, 0)

    private var total: Int = 0
    private var included: Int = 0
    private var numTextures: Int = 0
    private var numExportableTextures: Int = 0

    private var objectList: LLScrollListCtrl? = null
    private var texturePanel: LLPanel? = null
    private var objectName: String = ""

    private var manifest: LLSD = LLSD()
    private val requestedTexture: MutableMap<UUID, FSAssetResourceData> = mutableMapOf()
    private val textureChecked: MutableMap<UUID, Boolean> = mutableMapOf()
    private var filePath: String = ""
    private val callbackTextureList: MutableList<Any> = mutableListOf()
    private val waitTimer: LLFrameTimer = LLFrameTimer()
    private var lastRequest: Int = 0
    private var exported: Boolean = false
    private var aborted: Boolean = false
    private var dirty: Boolean = true

    private val textures: MutableList<UUID> = mutableListOf()
    private val textureNames: MutableList<String> = mutableListOf()
    private val inventoryRequests: MutableList<UUID> = mutableListOf()
    private val assetRequests: MutableList<UUID> = mutableListOf()

    override fun postBuild(): Boolean {
        objectList = getChild<LLScrollListCtrl>("selected_objects")
        texturePanel = getChild<LLPanel>("textures_panel")
        childSetAction("export_btn") { onClickExport() }

        LLSelectMgr.getInstance().mUpdateSignal.add { updateSelection() }
        return true
    }

    override fun draw() {
        if (dirty) {
            refresh()
            dirty = false
        }
        super.draw()
    }

    private fun refresh() {
        addSelectedObjects()
        addTexturePreview()
        populateObjectList()
        updateUI()
    }

    private fun markDirty() {
        dirty = true
    }

    override fun onOpen(key: LLSD) {
        val objectSelection = LLSelectMgr.getInstance().getSelection()
        if (objectSelection.getPrimaryObject() == null) {
            closeFloater()
            return
        }
        this.objectSelection = LLSelectMgr.getInstance().getEditSelection()
        refresh()
    }

    fun updateSelection() {
        val objectSelection = LLSelectMgr.getInstance().getSelection()
        val node = objectSelection.getFirstRootNode()

        if (node != null && !node.isValid && node.getObject().getId() == currentObjectId) {
            return
        }

        this.objectSelection = objectSelection
        markDirty()
        refresh()
    }

    private fun exportSelection(): Boolean {
        val sel = objectSelection ?: return false
        val iter = sel.validRootBegin()
        val node = iter ?: return false

        System.err.println("FSFloaterObjectExport: exportSelection filePath not yet implemented")
        filePath = ""

        manifest = LLSD()
        requestedTexture.clear()
        exported = false
        aborted = false
        inventoryRequests.clear()
        assetRequests.clear()
        textureChecked.clear()

        val author: String = ""
        val date: String = ""

        manifest["format_version"] = OXP_FORMAT_VERSION
        manifest["client"] = LLVersionInfo.getInstance().getChannelAndVersion()
        manifest["creation_date"] = date
        manifest["author"] = author
        manifest["grid"] = LLGridManager.getInstance().getGridLabel()

        for (rootNode in sel.validRoots()) {
            manifest["linkset"].append(getLinkSet(rootNode))
        }

        if (exported && !aborted) {
            waitTimer.start()
            lastRequest = inventoryRequests.size
            exportState = FSExportState.INVENTORY_DOWNLOAD
            System.err.println("FSFloaterObjectExport: gIdleCallbacks.addFunction not yet implemented")
        } else {
            return false
        }
        return true
    }

    private fun onIdle() {
        when (exportState) {
            FSExportState.IDLE -> {}
            FSExportState.INVENTORY_DOWNLOAD -> {
                if (false) return

                when {
                    inventoryRequests.isEmpty() -> {
                        lastRequest = assetRequests.size
                        waitTimer.start()
                        exportState = FSExportState.ASSET_DOWNLOAD
                    }
                    lastRequest != inventoryRequests.size -> {
                        waitTimer.start()
                        lastRequest = inventoryRequests.size
                        updateTitleProgress(FSExportState.INVENTORY_DOWNLOAD)
                    }
                    waitTimer.getElapsedTimeF32() > MAX_INVENTORY_WAIT_TIME -> {
                        waitTimer.start()
                        for (objId in inventoryRequests) {
                            val obj = gObjectList.findObject(objId)
                            obj?.dirtyInventory()
                            obj?.requestInventory()
                        }
                    }
                }
            }
            FSExportState.ASSET_DOWNLOAD -> {
                if (false) return

                when {
                    assetRequests.isEmpty() -> {
                        lastRequest = requestedTexture.size
                        waitTimer.start()
                        exportState = FSExportState.TEXTURE_DOWNLOAD
                    }
                    lastRequest != assetRequests.size -> {
                        waitTimer.start()
                        lastRequest = assetRequests.size
                        updateTitleProgress(FSExportState.ASSET_DOWNLOAD)
                    }
                    waitTimer.getElapsedTimeF32() > MAX_ASSET_WAIT_TIME -> {
                        assetRequests.clear()
                    }
                }
            }
            FSExportState.TEXTURE_DOWNLOAD -> {
                if (false) return

                when {
                    requestedTexture.isEmpty() -> {
                        exportState = FSExportState.IDLE
                        System.err.println("FSFloaterObjectExport: gIdleCallbacks.deleteFunction not yet implemented")
                        waitTimer.stop()

                        val zipData = zipLlsd(manifest)
                        System.err.println("FSFloaterObjectExport: writing zipData to file not yet implemented")

                        LLNotificationsUtil.add("ExportFinished", mapOf("FILENAME" to filename))
                        closeFloater()
                    }
                    lastRequest != requestedTexture.size -> {
                        waitTimer.start()
                        lastRequest = requestedTexture.size
                        updateTitleProgress(FSExportState.TEXTURE_DOWNLOAD)
                    }
                    waitTimer.getElapsedTimeF32() > MAX_TEXTURE_WAIT_TIME -> {
                        waitTimer.start()
                        for ((textureId, _) in requestedTexture) {
                            System.err.println("FSFloaterObjectExport: re-request texture not yet implemented")
                        }
                    }
                }
            }
        }
    }

    private fun getLinkSet(node: LLSelectNode): LLSD {
        val linkset = LLSD()
        val obj = node.getObject()
        val objectId = obj.getId()

        linkset.append(objectId)
        addPrim(obj, true)

        for (child in obj.getChildren()) {
            linkset.append(child.getId())
            addPrim(child, false)
        }
        return linkset
    }

    private fun addPrim(obj: LLViewerObject, root: Boolean) {
        val prim = LLSD()
        val objectId = obj.getId()

        val node = LLSelectMgr.getInstance().getSelection().getFirstNode { it.getObject()?.getId() == objectId }
        val defaultPrim = !FSExportPermsCheck.canExportNode(node, false)

        if (root) {
            if (obj.isAttachment()) {
                prim["attachment_point"] = attachmentIdFromState(obj.getAttachmentState())
            }
        } else {
            val parentObj = obj.getParent() as LLViewerObject
            prim["parent"] = parentObj.getId()
        }
        prim["position"] = obj.getPosition().getValue()
        prim["scale"] = obj.getScale().getValue()
        prim["rotation"] = llSdFromQuaternion(obj.getRotation())

        if (defaultPrim) {
            prim["flags"] = llSdFromU32(0u)
            prim["volume"]["path"] = LLPathParams().asLLSD()
            prim["volume"]["profile"] = LLProfileParams().asLLSD()
            prim["material"] = LL_MCODE_WOOD
        } else {
            exported = true
            prim["flags"] = llSdFromU32(obj.getFlags())
            prim["volume"]["path"] = obj.getVolume().getParams().getPathParams().asLLSD()
            prim["volume"]["profile"] = obj.getVolume().getParams().getProfileParams().asLLSD()
            prim["material"] = obj.getMaterial()
            if (obj.getClickAction() != 0) {
                prim["clickaction"] = obj.getClickAction()
            }

            val volobjp: LLVOVolume? = if (obj.getPCode() == LL_PCODE_VOLUME) obj as? LLVOVolume else null
            if (volobjp != null) {
                if (volobjp.isSculpted()) {
                    val sculptParams = obj.getSculptParams()
                    if (sculptParams != null) {
                        if (volobjp.isMesh()) {
                            if (!aborted) aborted = true
                            return
                        } else {
                            prim["sculpt"] = if (exportTexture(sculptParams.getSculptTexture())) {
                                sculptParams.asLLSD()
                            } else {
                                LLSculptParams().asLLSD()
                            }
                        }
                    }
                }

                if (volobjp.isFlexible()) {
                    val flexParams = obj.getFlexibleObjectData()
                    if (flexParams != null) prim["flexible"] = flexParams.asLLSD()
                }

                if (volobjp.getIsLight()) {
                    val lightParams = obj.getLightParams()
                    if (lightParams != null) prim["light"] = lightParams.asLLSD()
                }

                if (volobjp.hasLightTexture()) {
                    val lightImageParams = obj.getLightImageParams()
                    if (lightImageParams != null) prim["light_texture"] = lightImageParams.asLLSD()
                }
            }

            if (obj.isParticleSource()) {
                val partSourceScript = obj.getPartSourceScript()
                prim["particle"] = partSourceScript.mPartSysData.asLLSD()
                if (!exportTexture(partSourceScript.mPartSysData.mPartImageID)) {
                    prim["particle"]["PartImageID"] = UUID(0, 0).toString()
                }
            }

            val textureCount = obj.getNumTEs()
            for (i in 0 until textureCount) {
                val checkTE = obj.getTE(i)
                when {
                    FSCommon.isDefaultTexture(checkTE.getId()) -> {
                        prim["texture"].append(checkTE.asLLSD())
                    }
                    exportTexture(checkTE.getId()) -> {
                        prim["texture"].append(checkTE.asLLSD())
                    }
                    else -> {
                        checkTE.setId(LL_DEFAULT_WOOD_UUID)
                        prim["texture"].append(checkTE.asLLSD())
                    }
                }

                val materialParams = checkTE.getMaterialParams()
                if (materialParams != null) {
                    val params = materialParams.asLLSD()
                    if (exportTexture(params["NormMap"].asUUID()) && exportTexture(params["SpecMap"].asUUID())) {
                        prim["materials"].append(params)
                    }
                }
            }

            if (!obj.getPhysicsShapeUnknown()) {
                prim["ExtraPhysics"]["PhysicsShapeType"] = obj.getPhysicsShapeType()
                prim["ExtraPhysics"]["Density"] = obj.getPhysicsDensity().toDouble()
                prim["ExtraPhysics"]["Friction"] = obj.getPhysicsFriction().toDouble()
                prim["ExtraPhysics"]["Restitution"] = obj.getPhysicsRestitution().toDouble()
                prim["ExtraPhysics"]["GravityMultiplier"] = obj.getPhysicsGravity().toDouble()
            }

            prim["name"] = node!!.mName
            prim["description"] = node.mDescription
            prim["creation_date"] = llSdFromU64(node.mCreationDate)

            val permissions = node.mPermissions!!
            val creatorId = permissions.getCreator()
            if (creatorId != UUID(0, 0)) {
                prim["creator_id"] = creatorId
                LLAvatarNameCache.get(creatorId)?.let { prim["creator_name"] = it.asLLSD() }
            }
            val ownerId = permissions.getOwner()
            if (ownerId != UUID(0, 0)) {
                prim["owner_id"] = ownerId
                LLAvatarNameCache.get(ownerId)?.let { prim["owner_name"] = it.asLLSD() }
            }
            val groupId = permissions.getGroup()
            if (groupId != UUID(0, 0)) {
                prim["group_id"] = groupId
                LLAvatarNameCache.get(groupId)?.let { prim["group_name"] = it.asLLSD() }
            }
            val lastOwnerId = permissions.getLastOwner()
            if (lastOwnerId != UUID(0, 0)) {
                prim["last_owner_id"] = lastOwnerId
                LLAvatarNameCache.get(lastOwnerId)?.let { prim["last_owner_name"] = it.asLLSD() }
            }

            prim["base_mask"] = llSdFromU32(permissions.getMaskBase())
            prim["owner_mask"] = llSdFromU32(permissions.getMaskOwner())
            prim["group_mask"] = llSdFromU32(permissions.getMaskGroup())
            prim["everyone_mask"] = llSdFromU32(permissions.getMaskEveryone())
            prim["next_owner_mask"] = llSdFromU32(permissions.getMaskNextOwner())

            prim["sale_info"] = node.mSaleInfo.asLLSD()
            prim["touch_name"] = node.mTouchName
            prim["sit_name"] = node.mSitName

            if (gSavedSettings.getBool("FSExportContents")) {
                inventoryRequests.add(objectId)
                obj.registerInventoryListener(this, null)
                obj.dirtyInventory()
                obj.requestInventory()
            }
        }

        manifest["prim"][objectId.toString()] = prim
    }

    private fun exportTexture(textureId: UUID): Boolean {
        if (textureId == UUID(0, 0)) return false

        textureChecked[textureId]?.let { return it }

        if (false) {
            textureChecked[textureId] = true
            return true
        }

        val imagep: Any? = null
        var textureExport = false
        var name = ""
        var description = ""

        if (LLGridManager.getInstance().isInSecondLife()) {
            val comment: Map<String, String> = emptyMap()
            if (comment["a"] != null && UUID.fromString(comment["a"]) == gAgentId) {
                textureExport = true
            }
        }

        if (textureExport) {
            FSExportPermsCheck.canExportAsset(textureId) { n, d -> name = n; description = d }
        } else {
            textureExport = FSExportPermsCheck.canExportAsset(textureId) { n, d -> name = n; description = d }
        }

        textureChecked[textureId] = textureExport
        if (!textureExport) return false

        requestedTexture[textureId] = FSAssetResourceData(name = name, description = description, userData = this, uuid = textureId)
        System.err.println("FSFloaterObjectExport: fetch texture not yet implemented")
        return true
    }

    fun onImageLoaded(success: Boolean, srcVi: LLViewerFetchedTexture, final: Boolean) {
        if (final && success) {
            fetchTextureFromCache(srcVi)
        }
    }

    fun fetchTextureFromCache(srcVi: LLViewerFetchedTexture) {
        val textureId = srcVi.getId()
        val textureSize = 0
        System.err.println("FSFloaterObjectExport: fetchTextureFromCache not yet implemented")
    }

    fun removeRequestedTexture(textureId: UUID) {
        requestedTexture.remove(textureId)
    }

    fun saveFormattedImage(imageData: ByteArray, id: UUID) {
        manifest["asset"][id.toString()]["name"] = requestedTexture[id]!!.name
        manifest["asset"][id.toString()]["description"] = requestedTexture[id]!!.description
        manifest["asset"][id.toString()]["type"] = LLAssetType.lookup(LLAssetType.AT_TEXTURE)
        manifest["asset"][id.toString()]["data"] = imageData
        removeRequestedTexture(id)
    }

    override fun inventoryChanged(obj: LLViewerObject, inventory: List<LLInventoryObject>, serialNum: Int, userData: Any?) {
        val idx = inventoryRequests.indexOf(obj.getId())
        if (idx >= 0) inventoryRequests.removeAt(idx)

        val prim = manifest["prim"][obj.getId().toString()]
        for (invObj in inventory) {
            val item = invObj as? LLInventoryItem ?: continue

            var exportable = false
            val perms = item.getPermissions()

            if (LLGridManager.getInstance().isInOpenSim()) {
                exportable = when (LFSimFeatureHandler.instance().exportPolicy()) {
                    EXPORT_ALLOWED     -> (perms.getMaskOwner() and PERM_EXPORT) == PERM_EXPORT
                    EXPORT_UNDEFINED   -> (perms.getMaskBase() and PERM_ITEM_UNRESTRICTED) == PERM_ITEM_UNRESTRICTED
                    else               -> perms.getCreator() == gAgentId
                }
            }

            if (LLGridManager.getInstance().isInSecondLife() && perms.getCreator() == gAgentId) {
                exportable = true
            }

            if (!exportable) continue

            if (item.getType() == LLAssetType.AT_NONE || item.getType() == LLAssetType.AT_OBJECT) continue

            prim["content"].append(item.getUUID())
            manifest["inventory"][item.getUUID().toString()] = llCreateSdFromInventoryItem(item)

            if (item.getAssetUUID() == UUID(0, 0) && item.getType() == LLAssetType.AT_NOTECARD) {
                val assetUuid = UUID.randomUUID()
                manifest["inventory"][item.getUUID().toString()]["asset_id"] = assetUuid
                manifest["asset"][assetUuid.toString()]["name"] = item.getName()
                manifest["asset"][assetUuid.toString()]["description"] = item.getDescription()
                manifest["asset"][assetUuid.toString()]["type"] = LLAssetType.lookup(item.getType())
                System.err.println("FSFloaterObjectExport: blank LLNotecard serialisation not yet implemented")
            } else {
                assetRequests.add(item.getUUID())
                val data = FSAssetResourceData(
                    name = item.getName(),
                    description = item.getDescription(),
                    userData = this,
                    uuid = item.getUUID()
                )
                if (item.getAssetUUID() == UUID(0, 0) ||
                    item.getType() == LLAssetType.AT_NOTECARD ||
                    item.getType() == LLAssetType.AT_LSL_TEXT) {
                    System.err.println("FSFloaterObjectExport: gAssetStorage->getInvItemAsset not yet implemented")
                } else {
                    System.err.println("FSFloaterObjectExport: gAssetStorage->getAssetData not yet implemented")
                }
            }
        }
        obj.removeInventoryListener(this)
    }

    fun onLoadComplete(assetUuid: UUID, type: LLAssetType, data: FSAssetResourceData, status: Int) {
        val itemUuid = data.uuid
        removeRequestedAsset(itemUuid)

        if (status != 0) return

        System.err.println("FSFloaterObjectExport: reading asset file bytes not yet implemented")
        val buffer: ByteArray = ByteArray(0)

        manifest["asset"][assetUuid.toString()]["name"] = data.name
        manifest["asset"][assetUuid.toString()]["description"] = data.description
        manifest["asset"][assetUuid.toString()]["type"] = LLAssetType.lookup(type)
        manifest["asset"][assetUuid.toString()]["data"] = buffer

        if (manifest["inventory"].has(itemUuid.toString())) {
            if (manifest["inventory"][itemUuid.toString()]["asset_id"].asUUID() == UUID(0, 0)) {
                manifest["inventory"][itemUuid.toString()]["asset_id"] = assetUuid
            }
        }

        when (type) {
            LLAssetType.AT_CLOTHING, LLAssetType.AT_BODYPART -> {
                val asset = String(buffer)
                val position = asset.lastIndexOf("textures")
                val uuidPattern = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
                for (match in uuidPattern.findAll(asset.substring(position))) {
                    val uuidStr = match.value
                    if (isValidUUID(uuidStr)) {
                        exportTexture(UUID.fromString(uuidStr))
                    }
                }
            }
            LLAssetType.AT_GESTURE -> {
                System.err.println("FSFloaterObjectExport: LLMultiGesture deserialisation not yet implemented")
            }
            else -> {}
        }
    }

    private fun removeRequestedAsset(assetUuid: UUID) {
        assetRequests.remove(assetUuid)
    }

    private fun addObject(prim: LLViewerObject, name: String) {
        objects.add(Pair(prim, name))
    }

    private fun updateTextureInfo() {
        textures.clear()

        for ((obj, _) in objects) {
            val numFaces = obj.getVolume().getNumVolumeFaces()
            for (faceNum in 0 until numFaces) {
                val te = obj.getTE(faceNum)
                val id = te.getId()
                if (textures.contains(id)) continue
                textures.add(id)

                val imagep: Any? = null
                var exportable = false
                var name = ""
                var description = ""

                if (LLGridManager.getInstance().isInSecondLife()) {
                    val comment: Map<String, String> = emptyMap()
                    if (comment["a"] != null && UUID.fromString(comment["a"]) == gAgentId) {
                        exportable = true
                    }
                }

                if (exportable) {
                    FSExportPermsCheck.canExportAsset(id) { n, d -> name = n; description = d }
                } else {
                    exportable = FSExportPermsCheck.canExportAsset(id) { n, d -> name = n; description = d }
                }

                textureNames.add(if (exportable) name else "")
            }
        }
    }

    private fun updateTitleProgress(state: FSExportState) {
        val titleKey = when (state) {
            FSExportState.INVENTORY_DOWNLOAD -> "title_inventory"
            FSExportState.ASSET_DOWNLOAD     -> "title_assets"
            FSExportState.TEXTURE_DOWNLOAD   -> "title_textures"
            else -> return
        }
        setTitle(getString(titleKey).replace("{OBJECT}", objectName))
    }

    private fun updateUI() {
        childSetTextArg("NameText", "[NAME]", objectName)
        childSetTextArg("exportable_prims", "[COUNT]", included.toString())
        childSetTextArg("exportable_prims", "[TOTAL]", total.toString())
        childSetTextArg("exportable_textures", "[COUNT]", numExportableTextures.toString())
        childSetTextArg("exportable_textures", "[TOTAL]", numTextures.toString())
        setTitle(getString("title_floater").replace("{OBJECT}", objectName))
        childSetEnabled("export_textures_check", numExportableTextures > 0)
        childSetEnabled("export_btn", included > 0)
    }

    private fun onClickExport() {
        System.err.println("FSFloaterObjectExport: onClickExport not yet implemented")
    }

    private fun onExportFileSelected(filenames: List<String>) {
        filename = filenames[0]
        setTitle(getString("title_working").replace("{OBJECT}", objectName))
        if (!exportSelection()) {
            LLNotificationsUtil.add("ExportFailed")
            closeFloater()
        }
    }

    private fun populateObjectList() {
        val list = objectList ?: return
        list.deleteAllItems()
        list.setCommentText(LLTrans.getString("LoadingData"))

        for ((_, name) in objects) {
            list.addElement(mapOf(
                "columns" to listOf(
                    mapOf("column" to "icon", "type" to "icon", "value" to "Inv_Object"),
                    mapOf("column" to "name", "value" to name)
                )
            ))
        }

        for (texName in textureNames) {
            list.addElement(mapOf(
                "columns" to listOf(
                    mapOf("column" to "icon", "type" to "icon", "value" to "Inv_Texture"),
                    mapOf("column" to "name", "value" to texName)
                )
            ))
        }
    }

    private fun addSelectedObjects() {
        total = 0
        included = 0
        numTextures = 0
        numExportableTextures = 0
        objects.clear()
        textures.clear()
        textureNames.clear()

        val sel = objectSelection ?: return
        val node = sel.getFirstRootNode() ?: return
        currentObjectId = node.getObject().getId()
        objectName = node.mName

        for (n in sel) {
            total++
            if (n.getObject().getVolume() == null || !FSExportPermsCheck.canExportNode(n, false)) continue
            included++
            addObject(n.getObject(), n.mName)
        }

        if (objects.isEmpty()) return

        updateTextureInfo()
        numTextures = textures.size
        numExportableTextures = getNumExportableTextures()
    }

    private fun getNumExportableTextures(): Int = textureNames.count { it.isNotEmpty() }

    private fun addTexturePreview() {
        val numText = numExportableTextures
        if (numText == 0) return
        val imgWidth = 100
        val imgHeight = imgWidth + 15
        val panelHeight = (numText / 2 + 1) * imgHeight + 10
        texturePanel?.deleteAllChildren()
        texturePanel?.reshape(230, panelHeight)
        var imgNr = 0
        for (i in textures.indices) {
            if (textureNames[i].isEmpty()) continue
            val left = 8 + (imgNr % 2) * (imgWidth + 13)
            val bottom = panelHeight - (10 + (imgNr / 2 + 1) * imgHeight)
            System.err.println("FSFloaterObjectExport: addTexturePreview not yet implemented")
            imgNr++
        }
    }

    inner class FSExportCacheReadResponder(
        private val id: UUID,
        private var formattedImage: ByteArray?,
        private val parent: FSFloaterObjectExport
    ) {
        private var imageSize: Int = 0
        private var imageLocal: Boolean = false

        fun setData(data: ByteArray, datasize: Int, imagesize: Int, imageformat: Int, imagelocal: Boolean) {
            if (formattedImage != null) {
                formattedImage = formattedImage!! + data
            } else {
                formattedImage = data
            }
            imageSize = imagesize
            imageLocal = imagelocal
        }

        fun completed(success: Boolean) {
            if (success && formattedImage != null && imageSize > 0) {
                parent.saveFormattedImage(formattedImage!!, id)
            } else {
                parent.removeRequestedTexture(id)
            }
        }
    }
}
