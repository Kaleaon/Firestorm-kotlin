package com.firestorm.newview

/**
 * Floater that displays and edits the scene-graph of a GLTF asset attached to
 * a selected in-world object.  Transform (position / scale / rotation) editing
 * is supported for NODE-type items; SCENE, MESH, and SKIN items are read-only.
 */
class LLFloaterGLTFAssetEditor(key: LLSD) : LLFloater(key) {

    private var mObject: LLViewerObject? = null
    private var mAsset: GLTFAsset? = null

    // Folder-view infrastructure
    private val mUIColor: LLUIColor = LLUIColorTable.instance().getColor("MenuItemEnabledColor", DEFAULT_WHITE)
    val mGLTFViewModel: LLGLTFViewModel = LLGLTFViewModel()
    private var mItemListPanel: LLPanel? = null
    private var mFolderRoot: LLFolderView? = null
    private var mScroller: LLScrollContainer? = null
    private val mNodeToItemMap: MutableMap<Int, LLFolderViewItem> = mutableMapOf()

    // Transform panel state
    private var mLastEulerDegrees: LLVector3 = LLVector3()

    private var mTransformsPanel: LLPanel? = null
    private var mMenuClipboardPos: LLMenuButton? = null
    private var mCtrlPosX: LLSpinCtrl? = null
    private var mCtrlPosY: LLSpinCtrl? = null
    private var mCtrlPosZ: LLSpinCtrl? = null
    private var mMenuClipboardScale: LLMenuButton? = null
    private var mCtrlScaleX: LLSpinCtrl? = null
    private var mCtrlScaleY: LLSpinCtrl? = null
    private var mCtrlScaleZ: LLSpinCtrl? = null
    private var mMenuClipboardRot: LLMenuButton? = null
    private var mCtrlRotX: LLSpinCtrl? = null
    private var mCtrlRotY: LLSpinCtrl? = null
    private var mCtrlRotZ: LLSpinCtrl? = null

    companion object {
        private val DEFAULT_WHITE = LLColor4U(255, 255, 255)

        fun idle(userData: Any?) {
            val floater = userData as? LLFloaterGLTFAssetEditor ?: return
            floater.mFolderRoot?.update()
        }
    }

    init {
        setTitle("GLTF Asset Editor (WIP)")
        mCommitCallbackRegistrar.add("PanelObject.menuDoToSelected") { _, data -> onMenuDoToSelected(data) }
        mEnableCallbackRegistrar.add("PanelObject.menuEnable")       { _, data -> onMenuEnableItem(data) }
    }

    override fun postBuild(): Boolean {
        mMenuClipboardPos = getChild<LLMenuButton>("clipboard_pos_btn")
        mCtrlPosX = getChild<LLSpinCtrl>("Pos X", true)
        mCtrlPosX?.setCommitCallback { _, _ -> onCommitTransform() }
        mCtrlPosY = getChild<LLSpinCtrl>("Pos Y", true)
        mCtrlPosY?.setCommitCallback { _, _ -> onCommitTransform() }
        mCtrlPosZ = getChild<LLSpinCtrl>("Pos Z", true)
        mCtrlPosZ?.setCommitCallback { _, _ -> onCommitTransform() }

        mMenuClipboardScale = getChild<LLMenuButton>("clipboard_size_btn")
        mCtrlScaleX = getChild<LLSpinCtrl>("Scale X", true)
        mCtrlScaleX?.setCommitCallback { _, _ -> onCommitTransform() }
        mCtrlScaleY = getChild<LLSpinCtrl>("Scale Y", true)
        mCtrlScaleY?.setCommitCallback { _, _ -> onCommitTransform() }
        mCtrlScaleZ = getChild<LLSpinCtrl>("Scale Z", true)
        mCtrlScaleZ?.setCommitCallback { _, _ -> onCommitTransform() }

        mMenuClipboardRot = getChild<LLMenuButton>("clipboard_rot_btn")
        mCtrlRotX = getChild<LLSpinCtrl>("Rot X", true)
        mCtrlRotX?.setCommitCallback { _, _ -> onCommitTransform() }
        mCtrlRotY = getChild<LLSpinCtrl>("Rot Y", true)
        mCtrlRotY?.setCommitCallback { _, _ -> onCommitTransform() }
        mCtrlRotZ = getChild<LLSpinCtrl>("Rot Z", true)
        mCtrlRotZ?.setCommitCallback { _, _ -> onCommitTransform() }

        setTransformsEnabled(false)

        mTransformsPanel = getChild<LLPanel>("transform_panel", true)
        mTransformsPanel?.setVisible(false)

        mItemListPanel = getChild<LLPanel>("item_list_panel", true)
        initFolderRoot()

        return true
    }

    fun initFolderRoot() {
        check(mScroller == null && mFolderRoot == null) { "Folder root already initialized" }

        val scrollerViewRect = mItemListPanel?.getRect()?.copy() ?: LLRect()
        scrollerViewRect.translate(-scrollerViewRect.mLeft, -scrollerViewRect.mBottom)

        val scrollerParams = LLUICtrlFactory.getDefaultParams<LLFolderViewScrollContainer>().apply {
            rect(scrollerViewRect)
            name("folder_scroller")
        }
        mScroller = LLUICtrlFactory.create<LLScrollContainer>(scrollerParams)
        mScroller?.setFollowsAll()
        mItemListPanel?.addChild(mScroller)

        val baseItem = LLGLTFFolderItem(mGLTFViewModel)

        val p = LLUICtrlFactory.getDefaultParams<LLFolderView>().apply {
            name     = "Root"
            title    = "Root"
            rect     = LLRect(0, 0, getRect().getWidth(), 0)
            parent_panel = mItemListPanel
            tool_tip = "Root"
            listener = baseItem
            view_model = mGLTFViewModel
            root     = null
            use_ellipses = true
            options_menu = "menu_gltf.xml"
        }
        mFolderRoot = LLUICtrlFactory.create<LLFolderView>(p)
        mFolderRoot?.setCallbackRegistrar(mCommitCallbackRegistrar)
        mFolderRoot?.setEnableRegistrar(mEnableCallbackRegistrar)
        mScroller?.addChild(mFolderRoot)
        mFolderRoot?.setScrollContainer(mScroller)
        mFolderRoot?.setFollowsAll()
        mFolderRoot?.setOpen(true)
        mFolderRoot?.setSelectCallback { items, userAction -> onFolderSelectionChanged(items, userAction) }
        mScroller?.setVisible(true)
    }

    override fun onOpen(key: LLSD) {
        gIdleCallbacks.addFunction(::idle, this)
        loadFromSelection()
    }

    override fun onClose(appQuitting: Boolean) {
        gIdleCallbacks.deleteFunction(::idle, this)
        mAsset  = null
        mObject = null
    }

    private fun clearRoot() {
        while (true) {
            val folder = mFolderRoot?.getFoldersBegin()?.nextOrNull() ?: break
            folder.destroyView()
        }
        mNodeToItemMap.clear()
    }

    fun loadItem(id: Int, name: String, type: LLGLTFFolderItem.EType, parent: LLFolderViewFolder) {
        val listener = LLGLTFFolderItem(id, name, type, mGLTFViewModel)
        val params = LLFolderViewItem.Params().apply {
            this.name(name)
            creation_date(0)
            root(mFolderRoot)
            listener(listener)
            rect(LLRect())
            tool_tip = name
            font_color           = mUIColor
            font_highlight_color = mUIColor
        }
        val view = LLUICtrlFactory.create<LLFolderViewItem>(params)
        view.addToFolder(parent)
        view.setVisible(true)
    }

    fun loadFromNode(nodeId: Int, parent: LLFolderViewFolder) {
        val asset = mAsset ?: return
        if (asset.mNodes.size <= nodeId) return

        val node = asset.mNodes[nodeId]
        val name = node.mName.ifEmpty { getString("node_title") }

        val listener = LLGLTFFolderItem(nodeId, name, LLGLTFFolderItem.TYPE_NODE, mGLTFViewModel)
        val p = LLFolderViewFolder.Params().apply {
            root            = mFolderRoot
            this.listener   = listener
            this.name       = name
            tool_tip        = name
            font_color           = mUIColor
            font_highlight_color = mUIColor
        }
        val view = LLUICtrlFactory.create<LLFolderViewFolder>(p)
        view.addToFolder(parent)
        view.setVisible(true)
        view.setOpen(true)
        mNodeToItemMap[nodeId] = view

        for (childId in node.mChildren) {
            loadFromNode(childId, view)
        }

        if (node.mMesh != GLTF_INVALID_INDEX && asset.mMeshes.size > node.mMesh) {
            val meshName = asset.mMeshes[node.mMesh].mName.ifEmpty { getString("mesh_title") }
            loadItem(node.mMesh, meshName, LLGLTFFolderItem.TYPE_MESH, view)
        }

        if (node.mSkin != GLTF_INVALID_INDEX && asset.mSkins.size > node.mSkin) {
            val skinName = asset.mSkins[node.mSkin].mName.ifEmpty { getString("skin_title") }
            loadItem(node.mSkin, skinName, LLGLTFFolderItem.TYPE_SKIN, view)
        }

        view.setChildrenInited(true)
    }

    fun loadFromSelection() {
        clearRoot()

        if (LLSelectMgr.getInstance().getSelection().getObjectCount() != 1) {
            mAsset  = null
            mObject = null
            return
        }

        val selNode  = LLSelectMgr.getInstance().getSelection().getFirstNode(null)
        val objectp  = selNode?.getObject()
        if (objectp == null || objectp.mGLTFAsset == null) {
            mAsset  = null
            mObject = null
            return
        }

        mAsset  = objectp.mGLTFAsset
        mObject = objectp

        setTitle(if (selNode.mName.isNullOrEmpty()) getString("floater_title") else selNode.mName)

        val asset = mAsset ?: return
        for (i in asset.mScenes.indices) {
            val scene = asset.mScenes[i]
            val sceneName = scene.mName.ifEmpty { getString("scene_title") }

            val listener = LLGLTFFolderItem(i, sceneName, LLGLTFFolderItem.TYPE_SCENE, mGLTFViewModel)
            val p = LLFolderViewFolder.Params().apply {
                name             = sceneName
                root             = mFolderRoot
                this.listener    = listener
                tool_tip         = sceneName
                font_color           = mUIColor
                font_highlight_color = mUIColor
            }
            val view = LLUICtrlFactory.create<LLFolderViewFolder>(p)
            view.addToFolder(mFolderRoot)
            view.setVisible(true)
            view.setOpen(true)

            for (nodeId in scene.mNodes) {
                loadFromNode(nodeId, view)
            }
            view.setChildrenInited(true)
        }

        mGLTFViewModel.requestSortAll()
        mFolderRoot?.setChildrenInited(true)
        mFolderRoot?.arrangeAll()
        mFolderRoot?.update()
    }

    fun dirty() {
        if (mObject == null || mAsset == null || mFolderRoot == null) return

        if (LLSelectMgr.getInstance().getSelection().getObjectCount() > 1) {
            if (getVisible()) closeFloater()
            return
        }

        val selNode = LLSelectMgr.getInstance().getSelection().getFirstNode(null) ?: return
        val objectp = selNode.getObject()

        if (mObject != objectp || objectp?.mGLTFAsset == null) {
            if (getVisible()) closeFloater()
            return
        }

        if (mAsset != objectp.mGLTFAsset) {
            loadFromSelection()
            return
        }

        val itemp = mNodeToItemMap[selNode.mSelectedGLTFNode]
        if (itemp != null) {
            itemp.arrangeAndSet(true, false)
            loadNodeTransforms(selNode.mSelectedGLTFNode)
        }
    }

    protected fun onFolderSelectionChanged(items: ArrayDeque<LLFolderViewItem>, userAction: Boolean) {
        if (items.isEmpty()) {
            setTransformsEnabled(false)
            return
        }

        val item = items.first()
        val vmi  = item.getViewModelItem() as? LLGLTFFolderItem ?: return

        when (vmi.getType()) {
            LLGLTFFolderItem.TYPE_SCENE -> {
                setTransformsEnabled(false)
                LLSelectMgr.getInstance().selectObjectOnly(mObject, SELECT_ALL_TES, -1, -1)
            }
            LLGLTFFolderItem.TYPE_NODE -> {
                setTransformsEnabled(true)
                loadNodeTransforms(vmi.getItemId())
                LLSelectMgr.getInstance().selectObjectOnly(mObject, SELECT_ALL_TES, vmi.getItemId(), 0)
            }
            LLGLTFFolderItem.TYPE_MESH, LLGLTFFolderItem.TYPE_SKIN -> {
                val parentFolder = item.getParentFolder()
                if (parentFolder != null) {
                    val parentVmi = parentFolder.getViewModelItem() as? LLGLTFFolderItem
                    LLSelectMgr.getInstance().selectObjectOnly(mObject, SELECT_ALL_TES, parentVmi?.getItemId() ?: -1, 0)
                }
                setTransformsEnabled(false)
            }
            else -> setTransformsEnabled(false)
        }
    }

    protected fun onCommitTransform() {
        checkNotNull(mFolderRoot)  { "Folder root not initialized" }
        val item = mFolderRoot?.getCurSelectedItem()
        checkNotNull(item)         { "Nothing selected" }
        val vmi = item!!.getViewModelItem() as? LLGLTFFolderItem
        check(vmi != null && vmi.getType() == LLGLTFFolderItem.TYPE_NODE) { "Only nodes implemented" }

        val nodeId = vmi!!.getItemId()
        val asset  = mAsset ?: return
        val node   = asset.mNodes[nodeId]

        node.setTranslation(floatArrayOf(
            mCtrlPosX?.get() ?: 0f,
            mCtrlPosY?.get() ?: 0f,
            mCtrlPosZ?.get() ?: 0f
        ))

        node.setScale(floatArrayOf(
            mCtrlScaleX?.get() ?: 1f,
            mCtrlScaleY?.get() ?: 1f,
            mCtrlScaleZ?.get() ?: 1f
        ))

        var newRotX = roundToNearest(mCtrlRotX?.get() ?: 0f, OBJECT_ROTATION_PRECISION)
        var newRotY = roundToNearest(mCtrlRotY?.get() ?: 0f, OBJECT_ROTATION_PRECISION)
        var newRotZ = roundToNearest(mCtrlRotZ?.get() ?: 0f, OBJECT_ROTATION_PRECISION)

        val newRot  = LLVector3(newRotX, newRotY, newRotZ)
        val delta   = newRot - mLastEulerDegrees

        if (delta.magVec() >= 0.0005f) {
            mLastEulerDegrees = newRot
            newRotX *= DEG_TO_RAD
            newRotY *= DEG_TO_RAD
            newRotZ *= DEG_TO_RAD

            TODO("GPU: compute quaternion from Euler angles and call node.setRotation(q)")
        }

        asset.updateTransforms()
    }

    protected fun onMenuDoToSelected(userdata: LLSD) {
        // All clipboard copy/paste operations are stubs in the original C++ source
        when (userdata.asString()) {
            "psr_paste", "pos_paste", "size_paste", "rot_paste",
            "psr_copy",  "pos_copy",  "size_copy",  "rot_copy" -> {
                TODO("APR: use JVM equivalent – implement clipboard paste/copy for GLTF transforms")
            }
        }
    }

    protected fun onMenuEnableItem(userdata: LLSD): Boolean {
        val folderRoot = mFolderRoot ?: return false
        val item = folderRoot.getCurSelectedItem() ?: return false
        val vmi  = item.getViewModelItem() as? LLGLTFFolderItem ?: return false
        if (vmi.getType() != LLGLTFFolderItem.TYPE_NODE) return false

        return when (userdata.asString()) {
            "pos_paste", "size_paste", "rot_paste", "psr_copy" -> true
            else -> false
        }
    }

    fun setTransformsEnabled(enable: Boolean) {
        mMenuClipboardPos?.setEnabled(enable)
        mCtrlPosX?.setEnabled(enable)
        mCtrlPosY?.setEnabled(enable)
        mCtrlPosZ?.setEnabled(enable)
        mMenuClipboardScale?.setEnabled(enable)
        mCtrlScaleX?.setEnabled(enable)
        mCtrlScaleY?.setEnabled(enable)
        mCtrlScaleZ?.setEnabled(enable)
        mMenuClipboardRot?.setEnabled(enable)
        mCtrlRotX?.setEnabled(enable)
        mCtrlRotY?.setEnabled(enable)
        mCtrlRotZ?.setEnabled(enable)
    }

    fun loadNodeTransforms(nodeId: Int) {
        val asset = mAsset ?: return
        require(nodeId >= 0 && nodeId < asset.mNodes.size) { "Node id out of range: $nodeId" }

        val node = asset.mNodes[nodeId]
        node.makeTRSValid()

        mCtrlPosX?.set(node.mTranslation[0])
        mCtrlPosY?.set(node.mTranslation[1])
        mCtrlPosZ?.set(node.mTranslation[2])

        mCtrlScaleX?.set(node.mScale[0])
        mCtrlScaleY?.set(node.mScale[1])
        mCtrlScaleZ?.set(node.mScale[2])

        TODO("GPU: compute Euler angles from quaternion node.mRotation[0..3], convert to degrees, store in mLastEulerDegrees and set mCtrlRot{X,Y,Z}")
    }

    // Round `value` to the nearest multiple of `precision`
    private fun roundToNearest(value: Float, precision: Float): Float {
        if (precision == 0f) return value
        return Math.round(value / precision) * precision
    }
}
