package com.firestorm.newview

// Row data for the inspect object list, corresponding to LLSD "row" structure.
data class InspectRow(
    val id: String,
    val objectName: String,
    val ownerName: String,
    val creatorName: String,
    val creationDate: String,
    val creationDateSort: String,
    val description: String,
    val faceCount: Int,
    val vertexCount: Int,
    val triangleCount: Int,
    val textureMem: Int,
    val vramMem: Int,
)

// Column-visibility bit flags matching the FSInspectColumnConfig setting.
object InspectColumnBits {
    const val OBJECT_NAME: UInt = 1u
    const val DESCRIPTION: UInt = 2u
    const val OWNER_NAME: UInt = 4u
    const val CREATOR_NAME: UInt = 8u
    const val FACE_COUNT: UInt = 16u
    const val VERTEX_COUNT: UInt = 32u
    const val TRIANGLE_COUNT: UInt = 64u
    const val TRAM_COUNT: UInt = 128u
    const val VRAM_COUNT: UInt = 256u
    const val CREATION_DATE: UInt = 512u
}

class FloaterInspect(key: Any) : Floater(key) {

    // PoundLife: accumulated stats across the whole linkset selection.
    var statsMemoryTotal: Long = 0L

    private var dirty: Boolean = false
    private var objectSelection: Any? = null
    private var ownerNameCacheConnected: Boolean = false
    private var creatorNameCacheConnected: Boolean = false
    private var columnConfig: UInt = UInt.MAX_VALUE
    private var lastResizeDelta: Int = 0
    private val columnBits: MutableMap<String, UInt> = mutableMapOf(
        "object_name" to InspectColumnBits.OBJECT_NAME,
        "description" to InspectColumnBits.DESCRIPTION,
        "owner_name" to InspectColumnBits.OWNER_NAME,
        "creator_name" to InspectColumnBits.CREATOR_NAME,
        "facecount" to InspectColumnBits.FACE_COUNT,
        "vertexcount" to InspectColumnBits.VERTEX_COUNT,
        "trianglecount" to InspectColumnBits.TRIANGLE_COUNT,
        "tramcount" to InspectColumnBits.TRAM_COUNT,
        "vramcount" to InspectColumnBits.VRAM_COUNT,
        "creation_date" to InspectColumnBits.CREATION_DATE,
    )

    private var textureList: MutableList<String> = mutableListOf()
    private var textureMemory: UInt = 0u
    private var textureVramMemory: UInt = 0u

    override fun postBuild(): Boolean {
        TODO("APR: bind owner/creator profile buttons and object-list selection callback")
        registerColumnConfigCallback()
        refresh()
        return true
    }

    override fun onOpen(key: Any) {
        val prevForcesel: Boolean = TODO("APR: SelectMgr.getInstance().setForceSelection(true)") as Boolean
        TODO("APR: ToolMgr.setTransientTool(ToolComp.inspectInstance())")
        TODO("APR: SelectMgr.getInstance().setForceSelection(prevForcesel)")
        objectSelection = TODO("APR: SelectMgr.getInstance().getSelection()")
        refresh()
    }

    override fun onDestroy() {
        if (ownerNameCacheConnected) TODO("APR: disconnect ownerNameCacheConnection")
        if (creatorNameCacheConnected) TODO("APR: disconnect creatorNameCacheConnection")

        val buildVisible: Boolean = TODO("APR: FloaterReg.instanceVisible(\"build\")") as Boolean
        if (!buildVisible) {
            if (TODO("APR: ToolMgr.getBaseTool() === ToolComp.inspectInstance()") as Boolean) {
                ToolMgr.clearTransientTool()
            }
            ToolMgr.setCurrentToolset(gBasicToolset!!)
        } else {
            TODO("APR: FloaterReg.showInstance(\"build\", LLSD(), true)")
        }

        disconnectColumnConfigSignal()
        TODO("APR: destroy options menu")
    }

    fun getSelectedUUID(): String {
        val firstSelected: Any? = TODO("APR: objectList.getFirstSelected()")
        return if (firstSelected != null) TODO("APR: firstSelected.getUUID()") as String else ""
    }

    fun dirty() {
        dirty = true
    }

    override fun draw() {
        if (dirty) {
            refresh()
            dirty = false
        }
        super.draw()
    }

    override fun onFocusReceived() {
        TODO("APR: ToolMgr.setTransientTool(ToolComp.inspectInstance())")
        super.onFocusReceived()
    }

    fun refresh() {
        var faceCount = 0
        var faceCountVisible = 0
        var triangleCount = 0
        var vertexCount = 0
        var objCount = 0
        var primCount = 0
        var complexity: UInt = 0u
        textureList.clear()
        textureMemory = 0u
        textureVramMemory = 0u

        val selectedIndex: Int = TODO("APR: objectList.getFirstSelectedIndex()") as Int
        val selectedUuid: String = if (selectedIndex > -1) getSelectedUUID() else ""

        TODO("APR: objectList.deleteAllItems()")

        val validIterator: Any = TODO("APR: objectSelection.validIterator()")
        TODO("""
            APR: iterate validIterator, building InspectRow for each node with valid mCreationDate,
            resolving owner/creator names from LLAvatarNameCache (or LLCacheName for groups),
            and computing texture/VRAM memory via getObjectTextureMemory().
            Then call objectList.addElement(row, ADD_TOP).
        """)

        TODO("APR: iterate valid root objects to accumulate complexity via VOVolume.getRenderCost()")

        if (selectedIndex > -1 && TODO("APR: objectList.getItemIndex(selectedUuid) == selectedIndex") as Boolean) {
            TODO("APR: objectList.selectNthItem(selectedIndex)")
        } else {
            TODO("APR: objectList.selectNthItem(0)")
        }

        onSelectObject()
        TODO("APR: objectList.setScrollPos(savedScrollPos)")
        TODO("APR: update linksetstats_text with formatted count/memory/complexity totals")
    }

    fun onClickCreatorProfile() {
        val node = getSelectedNode() ?: return
        val creatorId: String = TODO("APR: node.permissions.creator") as String
        TODO("APR: RlvActions.canShowName check; AvatarActions.showProfile(creatorId)")
    }

    fun onClickOwnerProfile() {
        val node = getSelectedNode() ?: return
        val groupOwned: Boolean = TODO("APR: node.permissions.isGroupOwned") as Boolean
        if (groupOwned) {
            val groupId: String = TODO("APR: node.permissions.group") as String
            TODO("APR: GroupActions.show(groupId)")
        } else {
            val ownerId: String = TODO("APR: node.permissions.owner") as String
            TODO("APR: RlvActions.canShowName check; AvatarActions.showProfile(ownerId)")
        }
    }

    fun onSelectObject() {
        val selectedUuid = getSelectedUUID()
        if (selectedUuid.isEmpty()) return
        TODO("APR: enable/disable owner and creator profile buttons subject to RLVa restrictions")
    }

    private fun getSelectedNode(): Any? {
        val allSelected: List<Any> = TODO("APR: objectList.getAllSelected()") as List<Any>
        if (allSelected.isEmpty()) return null
        val firstSelected: Any = TODO("APR: objectList.getFirstSelected()") as Any
        val uuid: String = TODO("APR: firstSelected.getUUID()") as String
        return TODO("APR: objectSelection.getFirstNode { it.object.id == uuid }")
    }

    private fun getObjectTextureMemory(obj: ViewerObjectStub, outTexMem: UIntArray, outVramMem: UIntArray) {
        val objectTextureList: MutableList<String> = mutableListOf()
        val teCount: UByte = TODO("APR: obj.getNumTEs()") as UByte

        for (j in 0u until teCount) {
            val te: Any? = TODO("APR: obj.getTE(j.toInt())")
            te ?: continue

            val gltfMat: Any? = TODO("APR: te.getGLTFRenderMaterial()")
            if (gltfMat != null) {
                // PBR material: count all referenced GLTF textures.
                TODO("APR: iterate GLTF_TEXTURE_INFO_COUNT, fetch each tex by uuid, call calculateTextureMemory")
            } else {
                // Legacy diffuse
                val diffuseImg: Any? = TODO("APR: obj.getTEImage(j.toInt())")
                if (diffuseImg != null) calculateTextureMemory(diffuseImg, objectTextureList, outTexMem, outVramMem)

                // Legacy normal/specular from material params
                TODO("APR: check te.getMaterialParams for normal and specular IDs, calculateTextureMemory each")
            }
        }

        // Sculpt-map texture
        if (TODO("APR: obj.isSculpted() && !obj.isMesh()") as Boolean) {
            val sculptUuid: String = TODO("APR: obj.getSculptParams().getSculptTexture()") as String
            val img: Any? = TODO("APR: gTextureList.getImage(sculptUuid)")
            if (img != null) calculateTextureMemory(img, objectTextureList, outTexMem, outVramMem)
        }
    }

    private fun calculateTextureMemory(
        texture: Any,
        objectTextureList: MutableList<String>,
        outTexMem: UIntArray,
        outVramMem: UIntArray,
    ) {
        val uuid: String = TODO("APR: texture.getID()") as String
        val fullHeight: Int = TODO("APR: texture.getFullHeight()") as Int
        val fullWidth: Int = TODO("APR: texture.getFullWidth()") as Int
        val components: Int = TODO("APR: texture.getComponents()") as Int

        // VRAM assumes 32 bits per pixel (4 bytes); system-RAM approximation uses actual component count.
        val vramMem: UInt = (fullHeight * fullWidth * 32 / 8).toUInt()
        val texMem: UInt = (fullHeight * fullWidth * components).toUInt()

        if (!textureList.contains(uuid)) {
            textureList.add(uuid)
            textureMemory += texMem
            textureVramMemory += vramMem
        }
        if (!objectTextureList.contains(uuid)) {
            objectTextureList.add(uuid)
            outTexMem[0] += texMem
            outVramMem[0] += vramMem
        }
    }

    // ---- Configurable-column support ----------------------------------------

    private fun registerColumnConfigCallback() {
        TODO("APR: connect gSavedSettings FSInspectColumnConfig signal to onColumnDisplayModeChanged")
    }

    private fun disconnectColumnConfigSignal() {
        TODO("APR: disconnect FSInspectColumnConfig signal connection")
    }

    fun onColumnDisplayModeChanged() {
        val config: UInt = TODO("APR: gSavedSettings.getU32(\"FSInspectColumnConfig\")") as UInt
        TODO("APR: rebuild objectList columns from column_params, hiding any whose bit is absent in config")
        TODO("APR: adjust floater min-width; restore or clear sort order; call setDirty()")
    }

    fun onColumnVisibilityChecked(columnName: String) {
        val current: UInt = TODO("APR: gSavedSettings.getU32(\"FSInspectColumnConfig\")") as UInt
        val bit = columnBits[columnName] ?: return
        val toggled = current xor bit
        TODO("APR: gSavedSettings.setU32(\"FSInspectColumnConfig\", toggled)")
    }

    fun onEnableColumnVisibilityChecked(columnName: String): Boolean {
        val current: UInt = TODO("APR: gSavedSettings.getU32(\"FSInspectColumnConfig\")") as UInt
        val bit = columnBits[columnName] ?: return false
        return current and bit != 0u
    }

    // ---- Name-cache callbacks -----------------------------------------------

    private fun onGetOwnerNameCallback() {
        ownerNameCacheConnected = false
        dirty()
    }

    private fun onGetCreatorNameCallback() {
        creatorNameCacheConnected = false
        dirty()
    }
}
