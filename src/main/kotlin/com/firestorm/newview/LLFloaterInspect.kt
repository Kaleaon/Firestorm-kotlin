package com.firestorm.newview

import java.util.UUID

class LLFloaterInspect private constructor(private val key: LLSD) : LLFloater(key) {

    var mStatsMemoryTotal: ULong = 0UL
    lateinit var mObjectList: LLScrollListCtrl

    private var mDirty: Boolean = false
    private var mObjectSelection: LLSafeHandle<LLObjectSelection>? = null

    private var mOwnerNameCacheConnection: (() -> Unit)? = null
    private var mCreatorNameCacheConnection: (() -> Unit)? = null

    private var mOptionsButton: LLMenuButton? = null
    private var mOptionsMenuHandle: LLHandle<LLView>? = null

    private val mColumnBits: MutableMap<String, UInt> = mutableMapOf(
        "object_name"    to 1u,
        "description"    to 2u,
        "owner_name"     to 4u,
        "creator_name"   to 8u,
        "facecount"      to 16u,
        "vertexcount"    to 32u,
        "trianglecount"  to 64u,
        "tramcount"      to 128u,
        "vramcount"      to 256u,
        "creation_date"  to 512u
    )
    private var mLastResizeDelta: Int = 0
    private var mFSInspectColumnConfigConnection: (() -> Unit)? = null

    private val mTextureList: MutableList<UUID> = mutableListOf()
    private var mTextureMemory: UInt = 0u
    private var mTextureVRAMMemory: UInt = 0u

    init {
        registerCommitCallback("Inspect.OwnerProfile") { onClickOwnerProfile() }
        registerCommitCallback("Inspect.CreatorProfile") { onClickCreatorProfile() }
        registerCommitCallback("Inspect.SelectObject") { onSelectObject() }
    }

    override fun postBuild(): Boolean {
        mObjectList = getChild<LLScrollListCtrl>("object_list")

        val registrar = LLUICtrl.CommitCallbackRegistry.ScopedRegistrar()
        val enableRegistrar = LLUICtrl.EnableCallbackRegistry.ScopedRegistrar()
        registrar.add("Inspect.ToggleColumn") { _, ud -> onColumnVisibilityChecked(ud) }
        enableRegistrar.add("Inspect.EnableColumn") { _, ud -> onEnableColumnVisibilityChecked(ud) }

        mOptionsButton = getChild<LLMenuButton>("options_btn")
        val optionsMenu = LLUICtrlFactory.getInstance()
            .createFromFile<LLToggleableMenu>("menu_fs_inspect_options.xml", gMenuHolder,
                LLViewerMenuHolderGL.child_registry_t.instance())
        if (optionsMenu != null) {
            mOptionsMenuHandle = optionsMenu.getHandle()
            mOptionsButton?.setMenu(optionsMenu, LLMenuButton.MP_BOTTOM_LEFT)
        }

        mFSInspectColumnConfigConnection = gSavedSettings.getControl("FSInspectColumnConfig")
            ?.getSignal()?.connect { onColumnDisplayModeChanged() }
        onColumnDisplayModeChanged()
        refresh()
        return true
    }

    override fun onDestroy() {
        mOwnerNameCacheConnection?.invoke()
        mCreatorNameCacheConnection?.invoke()

        if (!LLFloaterReg.instanceVisible("build")) {
            if (LLToolMgr.getInstance().getBaseTool() == LLToolCompInspect.getInstance()) {
                LLToolMgr.getInstance().clearTransientTool()
            }
            LLToolMgr.getInstance().setCurrentToolset(gBasicToolset)
        } else {
            LLFloaterReg.showInstance("build", LLSD(), true)
        }

        mFSInspectColumnConfigConnection?.invoke()
        mOptionsMenuHandle?.get()?.die()
    }

    override fun onOpen(key: LLSD) {
        val forcesel = LLSelectMgr.getInstance().setForceSelection(true)
        LLToolMgr.getInstance().setTransientTool(LLToolCompInspect.getInstance())
        LLSelectMgr.getInstance().setForceSelection(forcesel)
        mObjectSelection = LLSelectMgr.getInstance().getSelection()
        refresh()
    }

    override fun draw() {
        if (mDirty) {
            refresh()
            mDirty = false
        }
        super.draw()
    }

    override fun refresh() {
        var fcount = 0
        var fcountVisible = 0
        var tcount = 0
        var vcount = 0
        var objcount = 0
        var primcount = 0
        var complexity: UInt = 0u
        mTextureList.clear()
        mTextureMemory = 0u
        mTextureVRAMMemory = 0u

        val maxAttachmentComplexity = maxOf(
            gSavedSettings.getF32("MaxAttachmentComplexity"), 1_000_000.0f
        )

        getChildView("button owner").setEnabled(false)
        getChildView("button creator").setEnabled(false)

        val selectedUuid: UUID
        val selectedIndex = mObjectList.getFirstSelectedIndex()
        selectedUuid = if (selectedIndex > -1) {
            mObjectList.getFirstSelected()?.getUUID() ?: UUID(0, 0)
        } else UUID(0, 0)

        mObjectList.operateOnAll(LLScrollListCtrl.OP_DELETE)

        val selMgr = LLSelectMgr.instance()
        val locale = LLLocale("")
        val resMgr = LLResMgr.instance()

        for (obj in mObjectSelection?.validIterator() ?: emptyList<LLSelectNode>()) {
            if (obj.mCreationDate == 0L) continue

            val vobj = obj.getObject()
            val timestamp = obj.mCreationDate / 1_000_000L
            val use24h = gSavedSettings.getBOOL("Use24HourClock")
            val timeStr = LLStringUtil.format(
                if (use24h) getString("timeStamp") else getString("timeStampAMPM"),
                mapOf("datetime" to timestamp.toInt())
            )

            val idOwner = obj.mPermissions?.getOwner() ?: UUID(0, 0)
            val idCreator = obj.mPermissions?.getCreator() ?: UUID(0, 0)

            val ownerName: String = resolveOwnerName(obj, idOwner)
            val creatorName: String = resolveCreatorName(obj, idCreator)

            val objectName = if (!(vobj?.isRoot() == true || vobj?.isRootEdit() == true))
                "   ${obj.mName}" else obj.mName

            var textureMemory = 0u
            var vramMemory = 0u
            vobj?.let { getObjectTextureMemory(it, textureMemory, vramMemory) }

            val row = LLSD().apply {
                this["id"] = vobj?.getID()
                this["columns"][0]["column"] = "object_name"
                this["columns"][0]["type"] = "text"
                this["columns"][0]["value"] = objectName
                this["columns"][1]["column"] = "owner_name"
                this["columns"][1]["type"] = "text"
                this["columns"][1]["value"] = ownerName
                this["columns"][2]["column"] = "creator_name"
                this["columns"][2]["type"] = "text"
                this["columns"][2]["value"] = creatorName
                this["columns"][3]["column"] = "creation_date"
                this["columns"][3]["type"] = "text"
                this["columns"][3]["value"] = timeStr
                this["columns"][4]["column"] = "description"
                this["columns"][4]["type"] = "text"
                this["columns"][4]["value"] = obj.mDescription
                this["columns"][5]["column"] = "creation_date_sort"
                this["columns"][5]["type"] = "text"
                this["columns"][5]["value"] = timestamp.toString()
                this["columns"][6]["column"] = "facecount"
                this["columns"][6]["type"] = "text"
                this["columns"][6]["value"] = resMgr.getIntegerString(vobj?.getNumFaces() ?: 0)
                this["columns"][7]["column"] = "vertexcount"
                this["columns"][7]["type"] = "text"
                this["columns"][7]["value"] = resMgr.getIntegerString(vobj?.getNumVertices() ?: 0)
                this["columns"][8]["column"] = "trianglecount"
                this["columns"][8]["type"] = "text"
                this["columns"][8]["value"] = resMgr.getIntegerString((vobj?.getNumIndices() ?: 0) / 3)
                this["columns"][9]["column"] = "tramcount"
                this["columns"][9]["type"] = "text"
                this["columns"][9]["value"] = resMgr.getIntegerString((textureMemory / 1024u).toInt())
                this["columns"][10]["column"] = "vramcount"
                this["columns"][10]["type"] = "text"
                this["columns"][10]["value"] = resMgr.getIntegerString((vramMemory / 1024u).toInt())
                this["columns"][11]["column"] = "facecount_sort"
                this["columns"][11]["type"] = "text"
                this["columns"][11]["value"] = vobj?.getNumFaces() ?: 0
                this["columns"][12]["column"] = "vertexcount_sort"
                this["columns"][12]["type"] = "text"
                this["columns"][12]["value"] = vobj?.getNumVertices() ?: 0
                this["columns"][13]["column"] = "trianglecount_sort"
                this["columns"][13]["type"] = "text"
                this["columns"][13]["value"] = (vobj?.getNumIndices() ?: 0) / 3
                this["columns"][14]["column"] = "tramcount_sort"
                this["columns"][14]["type"] = "text"
                this["columns"][14]["value"] = (textureMemory / 1024u).toInt()
                this["columns"][15]["column"] = "vramcount_sort"
                this["columns"][15]["type"] = "text"
                this["columns"][15]["value"] = (vramMemory / 1024u).toInt()
            }

            primcount = selMgr.getSelection().getObjectCount()
            objcount = selMgr.getSelection().getRootObjectCount()
            fcount += vobj?.getNumFaces() ?: 0
            fcountVisible += vobj?.getNumVisibleFaces() ?: 0
            tcount += (vobj?.getNumIndices() ?: 0) / 3
            vcount += vobj?.getNumVertices() ?: 0

            mObjectList.addElement(row, ADD_TOP)
        }

        for (rootObj in mObjectSelection?.validRootIterator() ?: emptyList<LLSelectNode>()) {
            val volume = rootObj.getObject() as? LLVOVolume ?: continue
            val textures = LLVOVolume.TextureCostMap()
            var attachmentTotalCost = 0.0f
            attachmentTotalCost += volume.getRenderCost(textures)
            for (child in volume.getChildren()) {
                val childVolume = child as? LLVOVolume
                if (childVolume != null) {
                    attachmentTotalCost += childVolume.getRenderCost(textures)
                }
            }
            for (texture in textures) {
                attachmentTotalCost += LLVOVolume.getTextureCost(texture)
            }
            complexity += attachmentTotalCost.coerceIn(0.0f, maxAttachmentComplexity).toUInt()
        }

        if (selectedIndex > -1 && mObjectList.getItemIndex(selectedUuid) == selectedIndex) {
            mObjectList.selectNthItem(selectedIndex)
        } else {
            mObjectList.selectNthItem(0)
        }
        onSelectObject()

        val statsArgs = mapOf(
            "NUM_OBJECTS"       to resMgr.getIntegerString(objcount),
            "NUM_PRIMS"         to resMgr.getIntegerString(primcount),
            "NUM_VISIBLE_FACES" to resMgr.getIntegerString(fcountVisible),
            "NUM_FACES"         to resMgr.getIntegerString(fcount),
            "NUM_VERTICES"      to resMgr.getIntegerString(vcount),
            "NUM_TRIANGLES"     to resMgr.getIntegerString(tcount),
            "NUM_TEXTURES"      to resMgr.getIntegerString(mTextureList.size),
            "TEXTURE_MEMORY"    to resMgr.getIntegerString((mTextureMemory / 1024u).toInt()),
            "VRAM_USAGE"        to resMgr.getIntegerString((mTextureVRAMMemory / 1024u).toInt()),
            "COMPLEXITY"        to resMgr.getIntegerString(complexity.toInt())
        )
        getChild<LLTextBase>("linksetstats_text").setText(getString("stats_list", statsArgs))
    }

    fun dirty() {
        setDirty()
    }

    fun getSelectedUUID(): UUID {
        if (mObjectList.getAllSelected().isNotEmpty()) {
            return mObjectList.getFirstSelected()?.getUUID() ?: UUID(0, 0)
        }
        return UUID(0, 0)
    }

    override fun onFocusReceived() {
        LLToolMgr.getInstance().setTransientTool(LLToolCompInspect.getInstance())
        super.onFocusReceived()
    }

    fun onClickCreatorProfile() {
        val node = getSelectedNode() ?: return
        val idCreator = node.mPermissions?.getCreator() ?: return
        if (!RlvActions.canShowName(RlvActions.SNC_DEFAULT, idCreator)
            && (node.mPermissions?.getOwner() == idCreator || RlvUtil.isNearbyAgent(idCreator))
        ) return
        LLAvatarActions.showProfile(idCreator)
    }

    fun onClickOwnerProfile() {
        val node = getSelectedNode() ?: return
        if (node.mPermissions?.isGroupOwned() == true) {
            val idGroup = node.mPermissions?.getGroup() ?: return
            LLGroupActions.show(idGroup)
        } else {
            val ownerId = node.mPermissions?.getOwner() ?: return
            if (!RlvActions.canShowName(RlvActions.SNC_DEFAULT, ownerId)) return
            LLAvatarActions.showProfile(ownerId)
        }
    }

    fun onSelectObject() {
        if (getSelectedUUID() != UUID(0, 0)) {
            if (!RlvActions.isRlvEnabled()) {
                getChildView("button owner").setEnabled(true)
                getChildView("button creator").setEnabled(true)
            } else {
                val node = getSelectedNode()
                val idOwner = node?.mPermissions?.getOwner() ?: UUID(0, 0)
                val idCreator = node?.mPermissions?.getCreator() ?: UUID(0, 0)
                getChildView("button owner").setEnabled(
                    RlvActions.canShowName(RlvActions.SNC_DEFAULT, idOwner)
                        || node?.mPermissions?.isGroupOwned() == true
                )
                getChildView("button creator").setEnabled(
                    (idOwner != idCreator && !RlvUtil.isNearbyAgent(idCreator))
                        || RlvActions.canShowName(RlvActions.SNC_DEFAULT, idCreator)
                )
            }
        }
    }

    fun getSelectedNode(): LLSelectNode? {
        if (mObjectList.getAllSelected().isEmpty()) return null
        val firstSelected = mObjectList.getFirstSelected() ?: return null
        val objId = firstSelected.getUUID()
        return mObjectSelection?.getFirstNode { node -> objId == node.getObject()?.getID() }
    }

    private fun setDirty() { mDirty = true }

    private fun onGetOwnerNameCallback() {
        mOwnerNameCacheConnection?.invoke()
        setDirty()
    }

    private fun onGetCreatorNameCallback() {
        mCreatorNameCacheConnection?.invoke()
        setDirty()
    }

    private fun resolveOwnerName(obj: LLSelectNode, idOwner: UUID): String {
        if (obj.mPermissions?.isGroupOwned() == true) {
            val idGroup = obj.mPermissions?.getGroup() ?: UUID(0, 0)
            val groupName = gCacheName.getGroupName(idGroup)
            return if (groupName != null) {
                "[$groupName] ${getString("Group")}"
            } else {
                mOwnerNameCacheConnection?.invoke()
                mOwnerNameCacheConnection = gCacheName.getGroup(idGroup) { onGetOwnerNameCallback() }
                LLTrans.getString("RetrievingData")
            }
        } else {
            val avName = LLAvatarNameCache.getCached(idOwner)
            return if (avName != null) {
                val canShow = RlvActions.canShowName(RlvActions.SNC_DEFAULT, idOwner)
                    || obj.mPermissions?.isGroupOwned() == true
                if (canShow) avName.getCompleteName() else RlvStrings.getAnonym(avName)
            } else {
                mOwnerNameCacheConnection?.invoke()
                mOwnerNameCacheConnection = LLAvatarNameCache.get(idOwner) { onGetOwnerNameCallback() }
                LLTrans.getString("RetrievingData")
            }
        }
    }

    private fun resolveCreatorName(obj: LLSelectNode, idCreator: UUID): String {
        val avName = LLAvatarNameCache.getCached(idCreator)
        return if (avName != null) {
            val canShow = RlvActions.canShowName(RlvActions.SNC_DEFAULT, idCreator)
                || ((obj.mPermissions?.getOwner() != idCreator) && !RlvUtil.isNearbyAgent(idCreator))
            if (canShow) avName.getCompleteName() else RlvStrings.getAnonym(avName)
        } else {
            mCreatorNameCacheConnection?.invoke()
            mCreatorNameCacheConnection = LLAvatarNameCache.get(idCreator) { onGetCreatorNameCallback() }
            LLTrans.getString("RetrievingData")
        }
    }

    private fun getObjectTextureMemory(
        obj: LLViewerObject,
        objectTextureMemory: UInt,
        objectVramMemory: UInt
    ) {
        var texMem = objectTextureMemory
        var vramMem = objectVramMemory
        val objectTextureList = mutableListOf<UUID>()
        val teCount: UByte = obj.getNumTEs()
        for (j in 0 until teCount.toInt()) {
            val te = obj.getTE(j) ?: continue
            val gltfMat = te.getGLTFRenderMaterial()
            if (gltfMat != null) {
                for (i in 0 until LLGLTFMaterial.GLTF_TEXTURE_INFO_COUNT) {
                    val texId = gltfMat.mTextureId[i]
                    if (texId != UUID(0, 0)) {
                        val img = gTextureList.getImage(texId)
                        if (img != null) {
                            calculateTextureMemory(img, objectTextureList, texMem, vramMem)
                        }
                    }
                }
            } else {
                val img = obj.getTEImage(j)
                if (img != null) {
                    calculateTextureMemory(img, objectTextureList, texMem, vramMem)
                }
                val materialParams = te.getMaterialParams()
                if (materialParams != null) {
                    val normalId = materialParams.getNormalID()
                    if (normalId != UUID(0, 0)) {
                        gTextureList.getImage(normalId)?.let {
                            calculateTextureMemory(it, objectTextureList, texMem, vramMem)
                        }
                    }
                    val specularId = materialParams.getSpecularID()
                    if (specularId != UUID(0, 0)) {
                        gTextureList.getImage(specularId)?.let {
                            calculateTextureMemory(it, objectTextureList, texMem, vramMem)
                        }
                    }
                }
            }
        }
        if (obj.isSculpted() && !obj.isMesh()) {
            val sculptParams = obj.getSculptParams()
            val uuid = sculptParams.getSculptTexture()
            gTextureList.getImage(uuid)?.let {
                calculateTextureMemory(it, objectTextureList, texMem, vramMem)
            }
        }
    }

    private fun calculateTextureMemory(
        texture: LLViewerTexture,
        objectTextureList: MutableList<UUID>,
        objectTextureMemory: UInt,
        objectVramMemory: UInt
    ) {
        var texMem = objectTextureMemory
        var vramMem = objectVramMemory
        val uuid = texture.getID()
        val vramMemBytes = (texture.getFullHeight() * texture.getFullWidth() * 32u / 8u)
        val textureMemBytes = (texture.getFullHeight() * texture.getFullWidth() * texture.getComponents())

        if (!mTextureList.contains(uuid)) {
            mTextureList.add(uuid)
            mTextureMemory += textureMemBytes
            mTextureVRAMMemory += vramMemBytes
        }
        if (!objectTextureList.contains(uuid)) {
            objectTextureList.add(uuid)
            texMem += textureMemBytes
            vramMem += vramMemBytes
        }
    }

    private fun onColumnDisplayModeChanged() {
        val columnConfig: UInt = gSavedSettings.getU32("FSInspectColumnConfig")
        val columnParams = mObjectList.getColumnInitParams()
        val columnPadding = mObjectList.getColumnPadding()

        var defaultWidth = 0
        var newWidth = 0
        val (minWidth, minHeight) = getResizeLimits()

        val currentSortCol = mObjectList.getSortColumnName()
        val currentSortAsc = mObjectList.getSortAscending()

        mObjectList.clearRows()
        mObjectList.clearColumns()
        mObjectList.updateLayout()

        for (p in columnParams) {
            defaultWidth += (p.width.pixelWidth + columnPadding)
            val params = p.copy()
            if (columnConfig and (mColumnBits[p.name.getValue()] ?: 0u) != 0u) {
                newWidth += (p.width.pixelWidth + columnPadding)
            } else {
                params.width.pixelWidth = -1
            }
            mObjectList.addColumn(params)
        }

        val adjustedMinWidth = minWidth - (defaultWidth - newWidth - mLastResizeDelta)
        mLastResizeDelta = defaultWidth - newWidth
        setResizeLimits(adjustedMinWidth, minHeight)

        if (getRect().getWidth() < adjustedMinWidth) {
            reshape(adjustedMinWidth, getRect().getHeight())
        }

        if (currentSortCol.isNotEmpty()) {
            val sortColWidth = mObjectList.getColumn(currentSortCol)?.getWidth() ?: -1
            val creationSortHidden = currentSortCol == "creation_date_sort"
                && mObjectList.getColumn("creation_date")?.getWidth() == -1
            if (creationSortHidden || sortColWidth == -1) {
                mObjectList.clearSortOrder()
            } else {
                mObjectList.sortByColumn(currentSortCol, currentSortAsc)
            }
        }
        mObjectList.setFilterColumn(0)
        mObjectList.dirtyColumns()
        setDirty()
    }

    private fun onColumnVisibilityChecked(userdata: LLSD) {
        val column = userdata.asString()
        val columnConfig: UInt = gSavedSettings.getU32("FSInspectColumnConfig")
        val bit = mColumnBits[column] ?: 0u
        val newValue = if (columnConfig and bit != 0u)
            columnConfig and bit.inv()
        else
            columnConfig or bit
        gSavedSettings.setU32("FSInspectColumnConfig", newValue)
    }

    private fun onEnableColumnVisibilityChecked(userdata: LLSD): Boolean {
        val column = userdata.asString()
        val columnConfig: UInt = gSavedSettings.getU32("FSInspectColumnConfig")
        return (mColumnBits[column] ?: 0u) and columnConfig != 0u
    }
}
