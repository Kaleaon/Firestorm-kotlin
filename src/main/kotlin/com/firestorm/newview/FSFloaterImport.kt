package com.firestorm.newview

import java.util.UUID

data class FSResourceData(
    val uuid: UUID,
    var temporary: Boolean = false,
    val assetType: LLAssetType,
    var inventoryItem: UUID = UUID(0, 0),
    val wearableType: LLWearableType = LLWearableType.NONE,
    var postAssetUpload: Boolean = false,
    var postAssetUploadId: UUID = UUID(0, 0),
    val floater: FSFloaterImport
)

class FSFloaterImport(filename: LLSD) : LLFloater(filename) {

    private enum class FSImportState { IDLE, INVENTORY_TRANSFER, LINKING }

    private var assetsUploading: UInt = 0u
    private val nextAssets: ArrayDeque<NextAsset> = ArrayDeque()

    private var importState: FSImportState = FSImportState.IDLE

    private var manifest: LLSD = LLSD.emptyMap()
    private val fileFullName: String = filename.asString()
    private var filenameBase: String = ""
    private var filePath: String = ""
    private var creatingActive: Boolean = false
    private val instance: FSFloaterImport = this
    private var linkset: Int = 0
    private var obj: Int = 0
    private var prim: Int = 0
    private var rootPosition: LLVector3 = LLVector3.ZERO
    private var startPosition: LLVector3 = LLVector3.ZERO
    private var linksetPosition: LLVector3 = LLVector3.ZERO
    private var rootRotation: LLQuaternion = LLQuaternion.IDENTITY
    private val primObjectMap: MutableMap<UUID, UUID> = mutableMapOf()
    private var linksetSize: Int = 0
    private var objectSize: Int = 0
    private var objectSelection: LLObjectSelectionHandle? = null

    private val textureQueue: MutableList<UUID> = mutableListOf()
    private var texturesTotal: UInt = 0u
    private val soundQueue: MutableList<UUID> = mutableListOf()
    private var soundsTotal: UInt = 0u
    private val animQueue: MutableList<UUID> = mutableListOf()
    private var animsTotal: UInt = 0u
    private val assetQueue: MutableList<UUID> = mutableListOf()
    private var assetsTotal: UInt = 0u

    val assetItemMap: MutableMap<UUID, UUID> = mutableMapOf()
    private val assetMap: MutableMap<UUID, UUID> = mutableMapOf()

    private var savedSettingShowNewInventory: Boolean = false
    private var objectCreatedCallback: (() -> Unit)? = null

    data class NextAsset(val newUuid: UUID, val assetId: UUID, val assetType: LLAssetType)

    private data class FSInventoryQueue(
        val item: LLViewerInventoryItem,
        val obj: LLViewerObject,
        val primName: String
    )
    private val inventoryQueue: MutableList<FSInventoryQueue> = mutableListOf()
    private var waitTimer: LLFrameTimer = LLFrameTimer()
    private var throttleTime: Float = 0.25f

    init {
        savedSettingShowNewInventory = gSavedSettings.getBOOL("ShowNewInventory")
        // Idle callback registration; drive the state machine from the viewer's idle loop.
        gIdleCallbacks.addFunction(::onIdleStatic, this)
    }

    fun destroy() {
        gIdleCallbacks.deleteFunction(::onIdleStatic, this)
        objectCreatedCallback = null
        gSavedSettings.setBOOL("ShowNewInventory", savedSettingShowNewInventory)
    }

    override fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun onClickCheckBoxUploadAsset() {
        TODO("APR: use JVM equivalent")
    }

    fun onClickCheckBoxTempAsset() {
        TODO("APR: use JVM equivalent")
    }

    fun processPrimCreated(obj: LLViewerObject): Boolean {
        if (!creatingActive) return false

        LLSelectMgr.getInstance().selectObjectAndFamily(obj, true)

        val primUuid = manifest["linkset"][linkset][this.obj].asUUID()
        val prim = manifest["prim"][primUuid.toString()]
        primObjectMap[primUuid] = obj.getID()
        val objectLocalId: UInt = obj.getLocalID()

        val primPosition = LLVector3(prim["position"])
        val position = if (this.obj != 0) (primPosition * rootRotation) + rootPosition else rootPosition
        setPrimPosition(UPD_POSITION.toUByte(), obj, position)

        val textureCount = obj.getNumTEs()
        for (face in 0 until textureCount) {
            val textureEntry = LLTextureEntry()
            if (textureEntry.fromLLSD(prim["texture"][face])) {
                if (assetMap[textureEntry.getID()] != null) {
                    textureEntry.setID(assetMap[textureEntry.getID()]!!)
                }
                obj.setTE(face.toUByte(), textureEntry)
            }
        }
        obj.sendTEUpdate()

        if (prim.has("materials")) {
            var te: UByte = 0u
            for (matSd in prim["materials"].asArray()) {
                val mat = LLMaterial()
                mat.fromLLSD(matSd)
                obj.setTEMaterialParams(te, mat)
                te++
            }
        }

        if (prim.has("sculpt")) {
            val sculptParams = LLSculptParams()
            sculptParams.fromLLSD(prim["sculpt"])
            assetMap[sculptParams.getSculptTexture()]?.let { replacement ->
                sculptParams.setSculptTexture(replacement, sculptParams.getSculptType())
            }
            obj.setParameterEntry(LLNetworkData.PARAMS_SCULPT, sculptParams, true)
        }

        if (prim.has("flexible")) {
            val attributes = LLFlexibleObjectData()
            attributes.fromLLSD(prim["flexible"])
            obj.setParameterEntry(LLNetworkData.PARAMS_FLEXIBLE, attributes, true)
        }

        if (prim.has("light")) {
            val lightParamBlock = LLLightParams()
            lightParamBlock.fromLLSD(prim["light"])
            obj.setParameterEntry(LLNetworkData.PARAMS_LIGHT, lightParamBlock, true)
        }

        if (prim.has("light_texture")) {
            val newLightImageParamBlock = LLLightImageParams()
            newLightImageParamBlock.fromLLSD(prim["light_texture"])
            obj.setParameterEntry(LLNetworkData.PARAMS_LIGHT_IMAGE, newLightImageParamBlock, true)
        }

        if (prim.has("clickaction")) {
            TODO("APR: use JVM equivalent")
        }

        val primName = if (prim.has("name")) {
            prim["name"].asString().also {
                TODO("APR: use JVM equivalent")
            }
        } else ""

        if (prim.has("description")) {
            TODO("APR: use JVM equivalent")
        }

        if (prim.has("group_mask") || prim.has("everyone_mask") || prim.has("next_owner_mask")) {
            TODO("APR: use JVM equivalent")
        }

        if (prim.has("sale_info")) {
            val saleInfo = LLSaleInfo()
            saleInfo.fromLLSD(prim["sale_info"])
            if (saleInfo.isForSale()) {
                TODO("APR: use JVM equivalent")
            }
        }

        if (prim.has("ExtraPhysics")) {
            val simFeatures = LLSD.emptyMap()
            obj.getRegion().getSimulatorFeatures(simFeatures)
            if (simFeatures.has("PhysicsShapeTypes")) {
                val extraPhysics = prim["ExtraPhysics"]
                val physType = extraPhysics["PhysicsShapeType"].asInteger().toUByte()
                val density = extraPhysics["Density"].asReal().toFloat()
                val friction = extraPhysics["Friction"].asReal().toFloat()
                val restitution = extraPhysics["Restitution"].asReal().toFloat()
                val gravity = extraPhysics["GravityMultiplier"].asReal().toFloat()
                obj.setPhysicsShapeType(physType)
                obj.setPhysicsGravity(gravity)
                obj.setPhysicsFriction(friction)
                obj.setPhysicsDensity(density)
                obj.setPhysicsRestitution(restitution)
                obj.updateFlags(true)
            }
        }

        inventoryQueue.clear()
        if (prim.has("content")) {
            for (contentKey in prim["content"].asArray()) {
                val keyStr = contentKey.asString()
                if (!manifest["inventory"].has(keyStr)) continue

                val itemSd = manifest["inventory"][keyStr]
                val assetId = itemSd["asset_id"].asUUID()
                if (assetId == UUID(0, 0)) continue

                if (assetMap[assetId] == null) {
                    searchInventory(assetId, obj, primName)
                    continue
                }

                val newAssetId = assetMap[assetId]!!
                if (assetItemMap[newAssetId] == null) {
                    searchInventory(newAssetId, obj, primName)
                    continue
                }

                val itemId = assetItemMap[newAssetId]!!
                val item = gInventory.getItem(itemId)
                if (item == null) {
                    searchInventory(newAssetId, obj, primName)
                    continue
                }

                inventoryQueue.add(FSInventoryQueue(item, obj, primName))
            }
        }

        throttleTime = if (inventoryQueue.size < 20) 0.25f else 0.5f
        importState = FSImportState.INVENTORY_TRANSFER
        waitTimer.start()
        return true
    }

    fun uploadDone() { assetsUploading-- }
    fun startUpload() { assetsUploading++ }
    fun getUploads(): UInt = assetsUploading

    fun pushNextAsset(newUuid: UUID, assetId: UUID, assetType: LLAssetType) {
        nextAssets.addLast(NextAsset(newUuid, assetId, assetType))
    }

    fun popNextAsset() {
        if (nextAssets.isEmpty()) return
        val asset = nextAssets.removeFirst()
        if (gDisconnected) return

        when (asset.assetType) {
            LLAssetType.AT_TEXTURE -> {
                val iter = textureQueue.indexOf(asset.assetId)
                if (iter >= 0) {
                    if (asset.newUuid != UUID(0, 0)) assetMap[asset.assetId] = asset.newUuid
                    textureQueue.removeAt(iter)
                }
            }
            LLAssetType.AT_SOUND -> {
                val iter = soundQueue.indexOf(asset.assetId)
                if (iter >= 0) {
                    if (asset.newUuid != UUID(0, 0)) assetMap[asset.assetId] = asset.newUuid
                    soundQueue.removeAt(iter)
                }
            }
            LLAssetType.AT_ANIMATION -> {
                val iter = animQueue.indexOf(asset.assetId)
                if (iter >= 0) {
                    if (asset.newUuid != UUID(0, 0)) assetMap[asset.assetId] = asset.newUuid
                    animQueue.removeAt(iter)
                }
            }
            else -> {
                val iter = assetQueue.indexOf(asset.assetId)
                if (iter >= 0) {
                    if (asset.newUuid != UUID(0, 0)) assetMap[asset.assetId] = asset.newUuid
                    assetQueue.removeAt(iter)
                }
            }
        }

        continueUploadOrImport()
    }

    private fun onIdle() {
        if (getUploads() < 1u) popNextAsset()

        when (importState) {
            FSImportState.IDLE -> Unit
            FSImportState.INVENTORY_TRANSFER -> {
                if (inventoryQueue.isEmpty()) {
                    importState = FSImportState.IDLE
                    waitTimer.stop()
                    if ((obj + 1) >= objectSize) {
                        if (objectSize > 1) {
                            LLSelectMgr.getInstance().sendLink()
                            importState = FSImportState.LINKING
                        } else {
                            postLink()
                        }
                    } else {
                        obj++
                        createPrim()
                    }
                    return
                }
                if (waitTimer.getElapsedTimeF32() < throttleTime) return

                val itemQueue = inventoryQueue.last()
                if (itemQueue.item.getType() == LLAssetType.AT_LSL_TEXT) {
                    LLToolDragAndDrop.dropScript(
                        itemQueue.obj, itemQueue.item, true,
                        LLToolDragAndDrop.SOURCE_AGENT, gAgentID
                    )
                } else {
                    LLToolDragAndDrop.dropInventory(
                        itemQueue.obj, itemQueue.item,
                        LLToolDragAndDrop.SOURCE_AGENT, gAgentID
                    )
                }
                inventoryQueue.removeLast()
                waitTimer.start()
            }
            FSImportState.LINKING -> {
                if (LLSelectMgr.getInstance().getSelection().getRootObjectCount() < 2) {
                    importState = FSImportState.IDLE
                    postLink()
                }
            }
        }
    }

    private fun loadFile() {
        TODO("APR: use JVM equivalent")
    }

    private fun populateBackupInfo() {
        TODO("APR: use JVM equivalent")
    }

    private fun onClickBtnImport() {
        primObjectMap.clear()
        linkset = 0
        obj = 0
        prim = 0
        objectSelection = LLSelectMgr.getInstance().getEditSelection()
        LLSelectMgr.getInstance().deselectAll()

        startPosition = gAgent.getPositionAgent()
        val offset = LLVector3(gSavedSettings.getVector3("FSImportBuildOffset"))
        startPosition = startPosition + offset * gAgent.getQuat()

        getChild<LLButton>("import_btn").setEnabled(false)
        getChild<LLCheckBoxCtrl>("do_not_attach").setEnabled(false)
        getChild<LLCheckBoxCtrl>("region_position").setEnabled(false)
        getChild<LLCheckBoxCtrl>("upload_asset").setEnabled(false)
        getChild<LLCheckBoxCtrl>("temp_asset").setEnabled(false)

        val totalAssets = texturesTotal + soundsTotal + animsTotal + assetsTotal
        if (totalAssets != 0u && getChild<LLCheckBoxCtrl>("upload_asset").get()) {
            gSavedSettings.setBOOL("ShowNewInventory", false)

            if (!getChild<LLCheckBoxCtrl>("temp_asset").get()) {
                val expectedUploadCost = texturesTotal.toInt() *
                    LLAgentBenefitsMgr.current().getTextureUploadCost()
                if (!canAffordTransaction(expectedUploadCost)) {
                    TODO("APR: use JVM equivalent")
                    return
                }
            }
            continueUploadOrImport()
        } else {
            importPrims()
        }
    }

    private fun continueUploadOrImport() {
        if (textureQueue.isNotEmpty()) {
            uploadAsset(textureQueue.first())
            return
        }
        if (soundQueue.isNotEmpty()) {
            uploadAsset(soundQueue.first())
            return
        }
        if (animQueue.isNotEmpty()) {
            uploadAsset(animQueue.first())
            return
        }
        if (assetQueue.isNotEmpty()) {
            uploadAsset(assetQueue.first())
            return
        }
        gSavedSettings.setBOOL("ShowNewInventory", savedSettingShowNewInventory)
        importPrims()
    }

    private fun importPrims() {
        objectSize = 0
        val objectsSd = manifest["linkset"][linkset]
        for (ignored in objectsSd.asArray()) objectSize++

        rootPosition = startPosition
        val linksetRootPrimUuid = manifest["linkset"][0][0].asUUID()
        val linksetRootPrim = manifest["prim"][linksetRootPrimUuid.toString()]
        linksetPosition = LLVector3(linksetRootPrim["position"])
        creatingActive = true
        createPrim()
    }

    private fun createPrim() {
        val primUuid = manifest["linkset"][linkset][obj].asUUID()
        val prim = manifest["prim"][primUuid.toString()]

        val scale = LLVector3(prim["scale"])
        var rotation = LLQuaternion.fromLLSD(prim["rotation"])
        if (obj != 0) {
            rotation = rotation * rootRotation
        } else {
            rootRotation = rotation
        }

        val primPosition = LLVector3(prim["position"])
        val position = if (obj != 0) (primPosition * rootRotation) + rootPosition else rootPosition

        objectCreatedCallback = null
        objectCreatedCallback = gObjectList.setNewObjectCallback { viewerObject ->
            processPrimCreated(viewerObject)
        }

        TODO("APR: use JVM equivalent")
    }

    private fun postLink() {
        if (!creatingActive) return

        val rootPrimUuid = manifest["linkset"][linkset][0].asUUID()
        val rootPrim = manifest["prim"][rootPrimUuid.toString()]

        if (rootPrim.has("attachment_point") && !getChild<LLCheckBoxCtrl>("do_not_attach").get()) {
            LLSelectMgr.getInstance().sendAttach(rootPrim["attachment_point"].asInteger().toUByte(), false)
            val rootObject = gObjectList.findObject(primObjectMap[rootPrimUuid]!!)
            setPrimPosition((UPD_POSITION or UPD_LINKED_SETS).toUByte(), rootObject, LLVector3(rootPrim["position"]))
        }

        if (getChild<LLCheckBoxCtrl>("region_position").get()) {
            if (!rootPrim.has("attachment_point")) {
                val rootObject = gObjectList.findObject(primObjectMap[rootPrimUuid]!!)
                setPrimPosition((UPD_POSITION or UPD_LINKED_SETS).toUByte(), rootObject, LLVector3(rootPrim["position"]))
            }
        }

        LLSelectMgr.getInstance().deselectAll()

        if ((linkset + 1) >= linksetSize) {
            creatingActive = false
            objectSelection = null
        } else {
            obj = 0
            linkset++

            objectSize = 0
            val objectsSd = manifest["linkset"][linkset]
            for (ignored in objectsSd.asArray()) objectSize++

            val nextRootUuid = manifest["linkset"][linkset][0].asUUID()
            val nextRootPrim = manifest["prim"][nextRootUuid.toString()]
            val nextRootLocation = LLVector3(nextRootPrim["position"])
            rootPosition = startPosition + (nextRootLocation - linksetPosition)
            createPrim()
        }
    }

    private fun setPrimPosition(
        type: UByte,
        obj: LLViewerObject,
        position: LLVector3,
        rotation: LLQuaternion = LLQuaternion.IDENTITY,
        scale: LLVector3 = LLVector3.ZERO
    ) {
        TODO("APR: use JVM equivalent")
    }

    private fun addAsset(assetId: UUID, assetType: LLAssetType) {
        if (!manifest["asset"].has(assetId.toString())) return

        when (assetType) {
            LLAssetType.AT_TEXTURE -> {
                if (!textureQueue.contains(assetId)) {
                    textureQueue.add(assetId)
                    texturesTotal++
                }
            }
            LLAssetType.AT_SOUND -> {
                if (!soundQueue.contains(assetId)) {
                    soundQueue.add(assetId)
                    soundsTotal++
                }
            }
            LLAssetType.AT_ANIMATION -> {
                if (!animQueue.contains(assetId)) {
                    animQueue.add(assetId)
                    animsTotal++
                }
            }
            else -> {
                if (!assetQueue.contains(assetId)) {
                    assetQueue.add(assetId)
                    assetsTotal++
                }
            }
        }
    }

    private fun processPrim(prim: LLSD) {
        if (prim.has("texture")) {
            for (tex in prim["texture"].asArray()) {
                addAsset(tex["imageid"].asUUID(), LLAssetType.AT_TEXTURE)
            }
        }
        if (prim.has("sculpt")) {
            addAsset(prim["sculpt"]["texture"].asUUID(), LLAssetType.AT_TEXTURE)
        }
        if (!prim.has("content")) return

        for (contentRef in prim["content"].asArray()) {
            val keyStr = contentRef.asString()
            if (!manifest["inventory"].has(keyStr)) continue

            val assetType = LLAssetType.lookup(manifest["inventory"][keyStr]["type"].asString())
            val assetId = manifest["inventory"][keyStr]["asset_id"].asUUID()
            if (!manifest["asset"].has(assetId.toString())) continue

            addAsset(assetId, assetType)
            val buffer = manifest["asset"][assetId.toString()]["data"].asBinary()

            when (assetType) {
                LLAssetType.AT_CLOTHING, LLAssetType.AT_BODYPART -> {
                    val asset = buffer.toString(Charsets.UTF_8)
                    val position = asset.lastIndexOf("textures")
                    val uuidPattern = Regex(
                        "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"
                    )
                    for (match in uuidPattern.findAll(asset.substring(position))) {
                        try {
                            addAsset(UUID.fromString(match.value), LLAssetType.AT_TEXTURE)
                        } catch (_: IllegalArgumentException) { }
                    }
                }
                LLAssetType.AT_GESTURE -> {
                    TODO("APR: use JVM equivalent")
                }
                else -> Unit
            }
        }
    }

    fun uploadAsset(assetId: UUID, inventoryItem: UUID = UUID(0, 0)) {
        TODO("APR: use JVM equivalent")
    }

    private fun searchInventory(assetId: UUID, obj: LLViewerObject, primName: String) {
        val items = gInventory.collectItemsByAssetId(assetId)
        if (items.isNotEmpty()) {
            inventoryQueue.add(FSInventoryQueue(items[0], obj, primName))
        }
    }

    companion object {
        private fun onIdleStatic(userData: Any?) {
            (userData as? FSFloaterImport)?.onIdle()
        }

        fun onAssetUploadComplete(uuid: UUID, userData: Any?, result: Int, extStatus: LLExtStat) {
            val data = userData as? LLResourceData ?: return
            val fsData = data.userData as? FSResourceData ?: return
            val self = fsData.floater
            var assetId = uuid

            if (result >= 0) {
                val folderId = gInventory.findCategoryUUIDForType(data.preferredLocation)
                if (fsData.inventoryItem == UUID(0, 0)) {
                    if (fsData.temporary) {
                        TODO("APR: use JVM equivalent")
                    } else {
                        if (data.assetInfo.type == LLAssetType.AT_SOUND
                            || data.assetInfo.type == LLAssetType.AT_TEXTURE
                            || data.assetInfo.type == LLAssetType.AT_ANIMATION) {
                            TODO("APR: use JVM equivalent")
                        }

                        if (folderId != UUID(0, 0)) {
                            fsData.postAssetUpload = true
                            fsData.postAssetUploadId = assetId
                            val cb = FSCreateItemCallback(fsData)
                            TODO("APR: use JVM equivalent")
                            return
                        }
                    }
                } else {
                    val item = gInventory.getItem(fsData.inventoryItem) as? LLViewerInventoryItem
                    if (item != null) {
                        val newItem = LLViewerInventoryItem(item)
                        newItem.setDescription(data.assetInfo.getDescription())
                        newItem.setTransactionID(data.assetInfo.transactionId)
                        newItem.setAssetUUID(assetId)
                        newItem.updateServer(false)
                        gInventory.updateItem(newItem)
                        gInventory.notifyObservers()
                        self.assetItemMap[assetId] = fsData.inventoryItem
                    } else {
                        assetId = UUID(0, 0)
                    }
                }
            } else {
                TODO("APR: use JVM equivalent")
            }

            self.pushNextAsset(assetId, fsData.uuid, data.assetInfo.type)
        }
    }
}

class FSCreateItemCallback(val data: FSResourceData) : LLInventoryCallback {

    override fun fire(invItem: UUID) {
        val self = data.floater

        if (invItem == UUID(0, 0)) {
            self.pushNextAsset(UUID(0, 0), data.uuid, data.assetType)
            return
        }

        if (data.postAssetUpload) {
            self.assetItemMap[data.postAssetUploadId] = invItem
            self.pushNextAsset(data.postAssetUploadId, data.uuid, data.assetType)
        } else {
            self.uploadAsset(data.uuid, invItem)
        }
    }
}
