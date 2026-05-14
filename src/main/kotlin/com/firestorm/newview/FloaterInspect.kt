package com.firestorm.newview

import com.firestorm.llui.Floater

// Column-visibility bit flags that mirror FSInspectColumnConfig setting bits.
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

class FloaterInspect(key: String) : Floater(key) {

    // PoundLife: accumulated VRAM across all textures in the linkset selection.
    var statsMemoryTotal: Long = 0L

    private var dirty: Boolean = false
    private var objectSelection: Any? = null
    private var ownerNameCacheConnected: Boolean = false
    private var creatorNameCacheConnected: Boolean = false
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

    // Per-refresh texture-deduplication state.
    private val textureList: MutableList<String> = mutableListOf()
    private var textureMemory: UInt = 0u
    private var textureVramMemory: UInt = 0u

    fun postBuild(): Boolean {
        // APR: bind owner/creator profile buttons and object-list selection callback
        registerColumnConfigCallback()
        refresh()
        return true
    }

    fun onOpen(key: String) {
        val prevForcesel: Boolean = false // APR: SelectMgr.getInstance().setForceSelection(true)
        ToolMgr.setTransientTool(ToolCompInspect)
        // APR: SelectMgr.getInstance().setForceSelection(prevForcesel)
        objectSelection = null // APR: SelectMgr.getInstance().getSelection()
        refresh()
    }

    fun onDestroy() {
        if (ownerNameCacheConnected) { /* APR: disconnect ownerNameCacheConnection */ }
        if (creatorNameCacheConnected) { /* APR: disconnect creatorNameCacheConnection */ }

        val buildVisible: Boolean = false // APR: FloaterReg.instanceVisible("build")
        if (!buildVisible) {
            if (ToolMgr.getBaseTool() === ToolCompInspect) {
                ToolMgr.clearTransientTool()
            }
            ToolMgr.setCurrentToolset(gBasicToolset!!)
        } else {
            // APR: FloaterReg.showInstance("build", LLSD(), true)
        }

        disconnectColumnConfigSignal()
        // APR: destroy options menu handle
    }

    fun getSelectedUUID(): String {
        val allSelected: List<Any> = emptyList() // APR: objectList.getAllSelected()
        if (allSelected.isEmpty()) return ""
        val first: Any = Any() // APR: objectList.getFirstSelected()
        return "" // APR: first.getUUID()
    }

    fun dirty() {
        dirty = true
    }

    fun isVisible(): Boolean {
        System.err.println("FloaterInspect: isVisible not yet implemented")
        return false
    }

    override fun draw() {
        if (dirty) {
            refresh()
            dirty = false
        }
        super.draw()
    }

    fun onFocusReceived() {
        ToolMgr.setTransientTool(ToolCompInspect)
        // APR: super.onFocusReceived()
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

        val selectedIndex: Int = 0 // APR: objectList.getFirstSelectedIndex()
        val selectedUuid: String = if (selectedIndex > -1) getSelectedUUID() else ""

        // APR: objectList.deleteAllItems()

        // APR: iterate objectSelection.validIterator():
        //   - skip nodes with mCreationDate == 0
        //   - resolve timestamp from mCreationDate/1000000
        //   - resolve owner name via LLAvatarNameCache (or LLCacheName for group owners)
        //   - resolve creator name via LLAvatarNameCache
        //   - apply RLVa name-anonymization rules
        //   - compute texture/VRAM memory via getObjectTextureMemory()
        //   - accumulate faceCount, faceCountVisible, triangleCount, vertexCount, primCount, objCount
        //   - call objectList.addElement(row, ADD_TOP)

        // Firestorm: accumulate attachment complexity for each root object.
        // APR: iterate valid root objects; for each VOVolume accumulate getRenderCost + children + texture costs, clamped to MaxAttachmentComplexity, add to complexity

        if (selectedIndex > -1 &&
            false // APR: objectList.getItemIndex(selectedUuid) == selectedIndex
        ) {
            // APR: objectList.selectNthItem(selectedIndex)
        } else {
            // APR: objectList.selectNthItem(0)
        }

        onSelectObject()
        // APR: objectList.setScrollPos(savedScrollPos)
        // APR: update linksetstats_text with formatted totals for objects, prims, faces, vertices, triangles, textures, RAM, VRAM, complexity
    }

    fun onClickCreatorProfile() {
        val node = getSelectedNode() ?: return
        val creatorId: String = "" // APR: node.permissions.creator
        // APR: RlvActions.canShowName check; if allowed: AvatarActions.showProfile(creatorId)
    }

    fun onClickOwnerProfile() {
        val node = getSelectedNode() ?: return
        val groupOwned: Boolean = false // APR: node.permissions.isGroupOwned
        if (groupOwned) {
            val groupId: String = "" // APR: node.permissions.group
            // APR: GroupActions.show(groupId)
        } else {
            val ownerId: String = "" // APR: node.permissions.owner
            // APR: RlvActions.canShowName check; if allowed: AvatarActions.showProfile(ownerId)
        }
    }

    fun onSelectObject() {
        val selectedUuid = getSelectedUUID()
        if (selectedUuid.isEmpty()) return
        // APR: enable/disable owner and creator profile buttons subject to RLVa name-visibility restrictions
    }

    private fun getSelectedNode(): Any? {
        val allSelected: List<Any> = emptyList() // APR: objectList.getAllSelected()
        if (allSelected.isEmpty()) return null
        val uuid: String = "" // APR: objectList.getFirstSelected().getUUID()
        return null // APR: objectSelection.getFirstNode { it.object.id == uuid }
    }

    // Accumulates RAM and VRAM usage for all textures on a given object,
    // avoiding double-counting textures that appear on multiple faces.
    private fun getObjectTextureMemory(
        obj: ViewerObjectStub,
        outTexMem: UIntArray,
        outVramMem: UIntArray,
    ) {
        val objectTextureList: MutableList<String> = mutableListOf()
        val teCount: UByte = 0u // APR: obj.getNumTEs()

        for (j in 0u until teCount) {
            val te: Any? = null // APR: obj.getTE(j.toInt())
            te ?: continue

            val gltfMat: Any? = null // APR: te.getGLTFRenderMaterial()
            if (gltfMat != null) {
                // PBR path: iterate GLTF_TEXTURE_INFO_COUNT texture slots.
                // APR: for each non-null texId in gltfMat.mTextureId: fetch LLViewerTexture from gTextureList, call calculateTextureMemory
            } else {
                // Legacy diffuse
                val diffuseImg: Any? = null // APR: obj.getTEImage(j.toInt())
                if (diffuseImg != null)
                    calculateTextureMemory(diffuseImg, objectTextureList, outTexMem, outVramMem)

                // Legacy normal + specular
                // APR: if te.getMaterialParams().notNull(): fetch normal and specular IDs, calculateTextureMemory each if present in gTextureList
            }
        }

        // Sculpt-map texture
        if (false) { // APR: obj.isSculpted() && !obj.isMesh()
            val sculptUuid: String = "" // APR: obj.getSculptParams().getSculptTexture()
            val img: Any? = null // APR: gTextureList.getImage(sculptUuid)
            if (img != null) calculateTextureMemory(img, objectTextureList, outTexMem, outVramMem)
        }
    }

    // VRAM assumes 32 bpp (4 bytes); system-RAM uses actual component count.
    private fun calculateTextureMemory(
        texture: Any,
        objectTextureList: MutableList<String>,
        outTexMem: UIntArray,
        outVramMem: UIntArray,
    ) {
        val uuid: String = "" // APR: texture.getID()
        val fullHeight: Int = 0 // APR: texture.getFullHeight()
        val fullWidth: Int = 0 // APR: texture.getFullWidth()
        val components: Int = 0 // APR: texture.getComponents()

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

    // ---- Configurable-column support (Firestorm FIRE-22292) -----------------

    private fun registerColumnConfigCallback() {
        // APR: connect gSavedSettings FSInspectColumnConfig signal to ::onColumnDisplayModeChanged
    }

    private fun disconnectColumnConfigSignal() {
        // APR: disconnect FSInspectColumnConfig signal connection
    }

    fun onColumnDisplayModeChanged() {
        val config: UInt = 0u // APR: gSavedSettings.getU32("FSInspectColumnConfig")
        // APR: rebuild objectList columns from column_params, hiding columns whose bit is absent in config
        // APR: adjust floater min-width by delta, restore or clear sort order, call dirty()
    }

    fun onColumnVisibilityChecked(columnName: String) {
        val current: UInt = 0u // APR: gSavedSettings.getU32("FSInspectColumnConfig")
        val bit = columnBits[columnName] ?: return
        // APR: gSavedSettings.setU32("FSInspectColumnConfig", current xor bit)
    }

    fun onEnableColumnVisibilityChecked(columnName: String): Boolean {
        val current: UInt = 0u // APR: gSavedSettings.getU32("FSInspectColumnConfig")
        val bit = columnBits[columnName] ?: return false
        return current and bit != 0u
    }

    // ---- Avatar-name cache callbacks ----------------------------------------

    private fun onGetOwnerNameCallback() {
        ownerNameCacheConnected = false
        dirty()
    }

    private fun onGetCreatorNameCallback() {
        creatorNameCacheConnected = false
        dirty()
    }
}
