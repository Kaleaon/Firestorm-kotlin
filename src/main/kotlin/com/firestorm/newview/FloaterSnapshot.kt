package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.FloaterView
import com.firestorm.llui.Panel
import com.firestorm.llui.UICtrl
import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.ComboBox
import com.firestorm.llui.TextBox
import com.firestorm.llui.Button
import com.firestorm.llui.SpinCtrl

private const val AUTO_SNAPSHOT_TIME_DELAY: Float = 1.0f
private const val MAX_POSTCARD_DATASIZE: Int = 1572864
private const val MAX_TEXTURE_SIZE: Int = 2048

var gSnapshotFloaterView: SnapshotFloaterView? = null

abstract class FloaterSnapshotBase(key: Any) : Floater(key) {

    var impl: ImplBase? = null
    protected var originalHeight: Int = 0
    var thumbnailPlaceholder: UICtrl? = null
    var refreshBtn: UICtrl? = null
    var refreshLabel: UICtrl? = null
    var succeessLblPanel: UICtrl? = null
    var failureLblPanel: UICtrl? = null
    var freezeFrameCheck: UICtrl? = null

    fun getOriginalHeight(): Int = originalHeight

    abstract fun saveTexture()

    open fun draw() {
        val previewp = getPreviewView()
        if (previewp != null && (previewp.isSnapshotActive() || previewp.getThumbnailLock())) {
            return
        }
        super.draw()
        if (previewp != null && !isMinimized() && thumbnailPlaceholder?.getVisible() == true) {
            if (previewp.getThumbnailImage() != null) {
                val working = impl?.getStatus() == ImplBase.Status.WORKING
                val thumbnailRect = getThumbnailPlaceholderRect()
                val thumbnailW = previewp.getThumbnailWidth()
                val thumbnailH = previewp.getThumbnailHeight()
                val localOffsetX = (thumbnailRect.width - thumbnailW) / 2
                val localOffsetY = (thumbnailRect.height - thumbnailH) / 2
                val offsetX = thumbnailRect.left + localOffsetX
                val offsetY = thumbnailRect.bottom + localOffsetY
                // GPU: gGL.matrixMode / gl_draw_scaled_image for thumbnail
            }
        }
        impl?.updateLayout(this)
    }

    open fun onClose(appQuitting: Boolean) {
        getParent()?.setMouseOpaque(false)
        val previewp = getPreviewView()
        previewp?.setAllowFullScreenPreview(false)
        previewp?.setVisible(false)
        previewp?.setEnabled(false)
        System.err.println("FloaterSnapshotBase: gSavedSettings.setBOOL(FreezeTime) not yet implemented")
        impl?.avatarPauseHandles?.clear()
        impl?.lastToolset?.let {
            System.err.println("FloaterSnapshotBase: LLToolMgr::getInstance()->setCurrentToolset not yet implemented")
        }
    }

    open fun notify(info: Map<String, Any>): Int {
        if (info.containsKey("set-ready")) {
            impl?.setStatus(ImplBase.Status.READY)
            return 1
        }
        if (info.containsKey("set-working")) {
            impl?.setStatus(ImplBase.Status.WORKING)
            return 1
        }
        if (info.containsKey("set-finished")) {
            val data = info["set-finished"] as? Map<*, *>
            val ok = data?.get("ok") as? Boolean ?: true
            val msg = data?.get("msg") as? String ?: ""
            impl?.setStatus(ImplBase.Status.FINISHED, ok, msg)
            return 1
        }
        if (info.containsKey("snapshot-updating")) {
            impl?.updateControls(this)
            return 1
        }
        if (info.containsKey("snapshot-updated")) {
            impl?.updateControls(this)
            impl?.setNeedRefresh(false)
            if (refreshBtn != null && refreshBtn?.getVisible() == false) {
                refreshBtn?.setVisible(true)
            }
            return 1
        }
        return 0
    }

    fun getPreviewView(): SnapshotLivePreview? =
        impl?.previewHandle?.get() as? SnapshotLivePreview

    fun getImageData(): Any? {
        System.err.println("FloaterSnapshotBase: getImageData not yet implemented")
        return null
    }

    fun getPosTakenGlobal(): Any {
        System.err.println("FloaterSnapshotBase: getPosTakenGlobal not yet implemented")
        return Any()
    }

    fun getThumbnailPlaceholderRect(): Any = thumbnailPlaceholder?.getRect() ?: Any()

    fun setRefreshLabelVisible(value: Boolean) { refreshLabel?.setVisible(value) }
    fun setSuccessLabelPanelVisible(value: Boolean) { succeessLblPanel?.setVisible(value) }
    fun setFailureLabelPanelVisible(value: Boolean) { failureLblPanel?.setVisible(value) }

    fun postSave() {
        impl?.updateControls(this)
        impl?.setStatus(ImplBase.Status.WORKING)
    }

    open fun postPanelSwitch() {
        impl?.updateControls(this)
        impl?.setStatus(ImplBase.Status.READY)
        val panel = impl?.getActivePanel(this)
        if (panel != null) {
            val isCustomResolution = panel.isCustomResolution()
            panel.enableAspectRatioCheckbox(isCustomResolution)
            panel.getWidthSpinner()?.setEnabled(isCustomResolution)
            panel.getHeightSpinner()?.setEnabled(isCustomResolution)
        }
    }

    fun inventorySaveFailed() {
        impl?.updateControls(this)
        impl?.setStatus(ImplBase.Status.FINISHED, false, "inventory")
    }

    abstract class ImplBase(val floater: FloaterSnapshotBase) {

        enum class Status { READY, WORKING, FINISHED }

        val avatarPauseHandles: MutableList<Any> = mutableListOf()
        var lastToolset: Any? = null
        var previewHandle: Any? = null  // Handle<SnapshotLivePreview>
        var aspectRatioCheckOff: Boolean = false
        var needRefresh: Boolean = false
        var advanced: Boolean = false
        var skipReshaping: Boolean = false
        var status: Status = Status.READY

        fun setAdvanced(advanced: Boolean) { this.advanced = advanced }
        fun setSkipReshaping(skip: Boolean) { skipReshaping = skip }

        fun getPreviewView(): SnapshotLivePreview? = previewHandle?.let {
            System.err.println("ImplBase: getPreviewView handle resolution not yet implemented")
            null
        }

        abstract fun getActivePanel(floater: FloaterSnapshotBase, okIfNotFound: Boolean = true): PanelSnapshot?
        open fun getActiveSnapshotType(floater: FloaterSnapshotBase): SnapshotModel.SnapshotType {
            return getActivePanel(floater)?.getSnapshotType() ?: SnapshotModel.SnapshotType.SNAPSHOT_NONE
        }
        abstract fun getImageFormat(floater: FloaterSnapshotBase): SnapshotModel.SnapshotFormat
        abstract fun getSnapshotPanelPrefix(): String
        abstract fun updateControls(floater: FloaterSnapshotBase)
        abstract fun getLayerType(floater: FloaterSnapshotBase): SnapshotModel.SnapshotLayerType
        abstract fun setFinished(finished: Boolean, ok: Boolean = true, msg: String = "")

        open fun updateLayout(floaterp: FloaterSnapshotBase) {
            val previewp = getPreviewView() ?: return
            var panelWidth = (400f * 1.0f).toInt()
            if (panelWidth > 700) panelWidth = 700
            previewp.setFixedThumbnailSize(panelWidth, 420)
            System.err.println("ImplBase: reshape floater based on advanced/collapsed state not yet implemented")
        }

        open fun updateLivePreview() {
            if (floater.isInVisibleChain() && updatePreviewList(true)) {
                updateControls(floater)
            }
        }

        open fun setStatus(status: Status, ok: Boolean = true, msg: String = "") {
            when (status) {
                Status.READY    -> { setWorking(false); setFinished(false) }
                Status.WORKING  -> { setWorking(true);  setFinished(false) }
                Status.FINISHED -> { setWorking(false); setFinished(true, ok, msg) }
            }
            this.status = status
        }

        fun getStatus(): Status = status

        open fun setNeedRefresh(need: Boolean) {
            val autoSnapshot = false
            val effective = if (autoSnapshot) false else need
            floater.setRefreshLabelVisible(effective)
            needRefresh = effective
        }

        open fun checkAutoSnapshot(previewp: SnapshotLivePreview?, updateThumbnail: Boolean = false) {
            if (previewp == null) return
            val autosnap = false
            previewp.updateSnapshot(autosnap, updateThumbnail, if (autosnap) AUTO_SNAPSHOT_TIME_DELAY else 0f)
        }

        fun setWorking(working: Boolean) {
            floater.getChild<UICtrl>("working_lbl")?.setVisible(working)
            floater.getChild<UICtrl>("working_indicator")?.setVisible(working)
            floater.setCtrlsEnabled(!working)
            val activePanel = getActivePanel(floater)
            if (activePanel != null) {
                activePanel.enableControls(!working)
                if (working) {
                    val panelName = activePanel.getName()
                    val prefix = panelName.removePrefix(getSnapshotPanelPrefix())
                    val progressText = floater.getString("${prefix}_progress_str")
                    floater.getChild<UICtrl>("working_lbl")?.setValue(progressText)
                }
            }
        }

        companion object {
            fun updatePreviewList(initialized: Boolean, haveSocials: Boolean = false): Boolean {
                if (!initialized && !haveSocials) return false
                System.err.println("ImplBase: updatePreviewList LLSnapshotLivePreview::onIdle not yet implemented")
                return false
            }

            fun onClickNewSnapshot(data: Any?) {
                val floater = data as? FloaterSnapshotBase ?: return
                val previewp = floater.getPreviewView() ?: return
                floater.impl?.setStatus(Status.READY)
                previewp.forceUpdateSnapshot = true
            }

            fun onClickAutoSnap(ctrl: UICtrl?, data: Any?) {
                val check = ctrl as? CheckBoxCtrl ?: return
                System.err.println("ImplBase: gSavedSettings.setBOOL(AutoSnapshot) not yet implemented")
                val view = data as? FloaterSnapshotBase ?: return
                view.impl?.checkAutoSnapshot(view.getPreviewView())
                view.impl?.updateControls(view)
            }

            fun onClickNoPost(ctrl: UICtrl?, data: Any?) {
                val noPost = (ctrl as? CheckBoxCtrl)?.get() ?: false
                TODO("APR: use JVM equivalent - gSavedSettings.setBOOL(RenderSnapshotNoPost, noPost)")
                val view = data as? FloaterSnapshotBase ?: return
                view.getPreviewView()?.updateSnapshot(true, true)
                view.impl?.updateControls(view)
            }

            fun onClickFilter(ctrl: UICtrl?, data: Any?) {
                val view = data as? FloaterSnapshotBase ?: return
                view.impl?.updateControls(view)
                val previewp = view.getPreviewView() ?: return
                view.impl?.checkAutoSnapshot(previewp)
                val filterbox = view.getChild<ComboBox>("filters_combobox") ?: return
                val filterName = if (filterbox.getCurrentIndex() != 0) filterbox.getSimple() else ""
                previewp.setFilter(filterName)
                previewp.updateSnapshot(true)
            }

            fun onClickDisplaySetting(ctrl: UICtrl?, data: Any?) {
                val view = data as? FloaterSnapshotBase ?: return
                view.getPreviewView()?.updateSnapshot(true, true)
                view.impl?.updateControls(view)
            }

            fun onClickCurrencyCheck(ctrl: UICtrl?, data: Any?) {
                val view = data as? FloaterSnapshotBase ?: return
                view.getPreviewView()?.updateSnapshot(true, true)
                view.impl?.updateControls(view)
            }

            fun onCommitFreezeFrame(ctrl: UICtrl?, data: Any?) {
                val checkBox = ctrl as? CheckBoxCtrl ?: return
                val view = data as? FloaterSnapshotBase ?: return
                val previewp = view.getPreviewView() ?: return
                TODO("APR: use JVM equivalent - gSavedSettings.setBOOL(UseFreezeFrame, checkBox.get())")
                if (checkBox.get()) {
                    previewp.prepareFreezeFrame()
                }
                view.impl?.updateLayout(view)
            }
        }
    }
}

abstract class PanelSnapshot : Panel() {
    abstract fun getSnapshotType(): SnapshotModel.SnapshotType
    abstract fun getImageFormat(): SnapshotModel.SnapshotFormat
    abstract fun getWidthSpinner(): SpinCtrl?
    abstract fun getHeightSpinner(): SpinCtrl?
    abstract fun getImageSizeComboName(): String
    abstract fun getAspectRatioCBName(): String
    abstract fun getImageSizeComboBox(): ComboBox?
    abstract fun getTypedPreviewWidth(): Int
    abstract fun getTypedPreviewHeight(): Int
    abstract fun updateControls(info: Map<String, Any>)
    abstract fun enableControls(enable: Boolean)
    fun enableAspectRatioCheckbox(enable: Boolean) {
        getChild<UICtrl>(getAspectRatioCBName())?.setEnabled(enable)
    }
    fun isCustomResolution(): Boolean {
        val selected = getImageSizeComboBox()?.getSelectedValue() ?: return false
        TODO("APR: use JVM equivalent - parse LLSD notation from selected value")
    }
}

object SnapshotModel {
    enum class SnapshotType { SNAPSHOT_WEB, SNAPSHOT_POSTCARD, SNAPSHOT_TEXTURE, SNAPSHOT_LOCAL, SNAPSHOT_NONE }
    enum class SnapshotFormat { SNAPSHOT_FORMAT_PNG, SNAPSHOT_FORMAT_JPEG, SNAPSHOT_FORMAT_BMP }
    enum class SnapshotLayerType { SNAPSHOT_TYPE_COLOR, SNAPSHOT_TYPE_DEPTH, SNAPSHOT_TYPE_DEPTH24 }
}

class SnapshotLivePreview {
    var forceUpdateSnapshot: Boolean = false
    var keepAspectRatio: Boolean = false

    fun isSnapshotActive(): Boolean = TODO("APR: use JVM equivalent - check snapshot active")
    fun getThumbnailLock(): Boolean = TODO("APR: use JVM equivalent - check thumbnail lock")
    fun getThumbnailImage(): Any? = TODO("APR: use JVM equivalent - get thumbnail image")
    fun getThumbnailWidth(): Int = TODO("APR: use JVM equivalent - get thumbnail width")
    fun getThumbnailHeight(): Int = TODO("APR: use JVM equivalent - get thumbnail height")
    fun getSnapshotUpToDate(): Boolean = TODO("APR: use JVM equivalent - check snapshot up to date")
    fun getDataSize(): Int = TODO("APR: use JVM equivalent - get data size")
    fun getEncodedImageWidth(): Int = TODO("APR: use JVM equivalent - get encoded image width")
    fun getEncodedImageHeight(): Int = TODO("APR: use JVM equivalent - get encoded image height")
    fun getMaxImageSize(): Int = TODO("APR: use JVM equivalent - get max image size")
    fun getSize(w: Int, h: Int) { TODO("APR: use JVM equivalent - get preview size") }
    fun setSize(w: Int, h: Int) { TODO("APR: use JVM equivalent - set preview size") }
    fun setMaxImageSize(size: Int) { TODO("APR: use JVM equivalent - set max image size") }
    fun setSnapshotType(type: SnapshotModel.SnapshotType) { TODO("APR: use JVM equivalent - set snapshot type") }
    fun setSnapshotFormat(format: SnapshotModel.SnapshotFormat) { TODO("APR: use JVM equivalent - set snapshot format") }
    fun setSnapshotBufferType(type: SnapshotModel.SnapshotLayerType) { TODO("APR: use JVM equivalent - set buffer type") }
    fun setSnapshotQuality(quality: Int) { TODO("APR: use JVM equivalent - set snapshot quality") }
    fun setFilter(filterName: String) { TODO("APR: use JVM equivalent - set image filter") }
    fun setVisible(visible: Boolean) { TODO("APR: use JVM equivalent - set visible") }
    fun setEnabled(enabled: Boolean) { TODO("APR: use JVM equivalent - set enabled") }
    fun setAllowFullScreenPreview(allow: Boolean) { TODO("APR: use JVM equivalent - set full screen preview") }
    fun setFixedThumbnailSize(width: Int, height: Int) { TODO("APR: use JVM equivalent - set fixed thumbnail size") }
    fun setThumbnailPlaceholderRect(rect: Any) { TODO("APR: use JVM equivalent - set thumbnail placeholder rect") }
    fun setThumbnailImageSize() { TODO("APR: use JVM equivalent - set thumbnail image size") }
    fun setContainer(floater: FloaterSnapshotBase) { TODO("APR: use JVM equivalent - set container floater") }
    fun updateSnapshot(autosnap: Boolean, updateThumbnail: Boolean = false, delay: Float = 0f) {
        TODO("APR: use JVM equivalent - update snapshot")
    }
    fun prepareFreezeFrame() { TODO("APR: use JVM equivalent - prepare freeze frame") }
    fun saveTexture() { TODO("APR: use JVM equivalent - save texture to inventory") }
    fun saveLocal(successCb: Any, failureCb: Any) { TODO("APR: use JVM equivalent - save snapshot locally") }
    fun drawPreviewRect(offsetX: Int, offsetY: Int) { TODO("GPU: draw preview rect") }
    fun getHandle(): Any = TODO("APR: use JVM equivalent - get handle")

    companion object {
        val sList: MutableSet<SnapshotLivePreview> = mutableSetOf()
        fun onIdle(preview: SnapshotLivePreview): Boolean = TODO("APR: use JVM equivalent - onIdle update")
    }
}

class FloaterSnapshot(key: Any) : FloaterSnapshotBase(key) {

    private var isOpen: Boolean = false

    init {
        impl = Impl(this)
    }

    override fun postBuild(): Boolean {
        refreshBtn = getChild<UICtrl>("new_snapshot_btn")
        childSetAction("new_snapshot_btn") { ImplBase.onClickNewSnapshot(this) }
        refreshLabel    = getChild<UICtrl>("refresh_lbl")
        succeessLblPanel = getChild<UICtrl>("succeeded_panel")
        failureLblPanel  = getChild<UICtrl>("failed_panel")

        childSetCommitCallback("ui_check")      { ctrl, data -> ImplBase.onClickDisplaySetting(ctrl, this) }
        childSetCommitCallback("balance_check") { ctrl, data -> ImplBase.onClickDisplaySetting(ctrl, this) }
        childSetCommitCallback("hud_check")     { ctrl, data -> ImplBase.onClickDisplaySetting(ctrl, this) }
        childSetCommitCallback("currency_check"){ ctrl, data -> ImplBase.onClickCurrencyCheck(ctrl, this) }

        (impl as Impl).setAspectRatioCheckboxValue(
            this,
            TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(KeepAspectForSnapshot)") as Boolean
        )

        childSetCommitCallback("layer_types") { ctrl, data -> Impl.onCommitLayerTypes(ctrl, this) }
        getChild<UICtrl>("layer_types")?.setValue("colors")
        getChildView("layer_types")?.setEnabled(false)

        freezeFrameCheck = getChild<UICtrl>("freeze_frame_check")
        freezeFrameCheck?.setValue(TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(UseFreezeFrame)"))
        freezeFrameCheck?.setCommitCallback { ctrl, data -> ImplBase.onCommitFreezeFrame(ctrl, this) }

        getChild<UICtrl>("auto_snapshot_check")?.setValue(TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(AutoSnapshot)"))
        childSetCommitCallback("auto_snapshot_check") { ctrl, _ -> ImplBase.onClickAutoSnap(ctrl, this) }

        getChild<UICtrl>("no_post_check")?.setValue(TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(RenderSnapshotNoPost)"))
        childSetCommitCallback("no_post_check") { ctrl, _ -> ImplBase.onClickNoPost(ctrl, this) }

        getChild<Button>("retract_btn")?.setCommitCallback { onExtendFloater() }
        getChild<Button>("extend_btn")?.setCommitCallback { onExtendFloater() }
        getChild<Button>("360_label")?.setCommitCallback { on360Snapshot() }

        val filterbox = getChild<ComboBox>("filters_combobox") ?: return false
        val filterList = TODO("APR: use JVM equivalent - LLImageFiltersManager::getFiltersList()") as List<String>
        filterList.forEach { filterbox.add(it) }
        childSetCommitCallback("filters_combobox") { ctrl, _ -> ImplBase.onClickFilter(ctrl, this) }

        TODO("APR: use JVM equivalent - LLWebProfile::setImageUploadResultCallback / LLPostCard::setPostResultCallback")

        thumbnailPlaceholder = getChild<UICtrl>("thumbnail_placeholder")

        val fullScreenRect = TODO("APR: use JVM equivalent - getRootView()->getRect()") as Any
        val previewp = SnapshotLivePreview()
        SnapshotLivePreview.sList.add(previewp)
        TODO("APR: use JVM equivalent - reparent previewp under gSnapshotFloaterView parent, add to gSnapshotFloaterView")

        originalHeight = getRect().height
        impl?.previewHandle = previewp.getHandle()
        previewp.setContainer(this)
        impl?.updateControls(this)
        impl?.setAdvanced(TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(AdvanceSnapshot)") as Boolean)
        impl?.updateLayout(this)
        previewp.setThumbnailPlaceholderRect(getThumbnailPlaceholderRect())

        return true
    }

    override fun onOpen(key: Any) {
        val previewp = getPreviewView()
        if (previewp != null) {
            previewp.setAllowFullScreenPreview(true)
            previewp.updateSnapshot(true)
        }
        focusFirstItem(false)
        gSnapshotFloaterView?.setEnabled(true)
        gSnapshotFloaterView?.setVisible(true)
        TODO("APR: use JVM equivalent - gSnapshotFloaterView->adjustToFitScreen")

        impl?.updateControls(this)
        impl?.setAdvanced(TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(AdvanceSnapshot)") as Boolean)
        impl?.updateLayout(this)

        if (isOpen) return
        isOpen = true

        val lastPanel = TODO("APR: use JVM equivalent - gSavedSettings.getString(FSLastSnapshotPanel)") as? String ?: ""
        val panelName = if (lastPanel.isEmpty()) "panel_snapshot_options" else lastPanel
        TODO("APR: use JVM equivalent - panel_container->selectTabByName(panelName) and onOpen")
        succeessLblPanel?.setVisible(false)
        failureLblPanel?.setVisible(false)
    }

    override fun onClose(appQuitting: Boolean) {
        super.onClose(appQuitting)
        isOpen = false
        val panelName = TODO("APR: use JVM equivalent - panel_container->getCurrentPanel()->getName()") as? String ?: ""
        TODO("APR: use JVM equivalent - gSavedSettings.setString(FSLastSnapshotPanel, panelName)")
    }

    override fun notify(info: Map<String, Any>): Int {
        val res = super.notify(info)
        if (res != 0) return res

        if (info.containsKey("combo-res-change")) {
            val data = info["combo-res-change"] as? Map<*, *>
            val comboName = data?.get("control-name") as? String ?: return 1
            (impl as? Impl)?.updateResolution(getChild<UICtrl>(comboName), this)
            return 1
        }
        if (info.containsKey("custom-res-change")) {
            val res2 = info["custom-res-change"] as? Map<*, *>
            val w = res2?.get("w") as? Int ?: return 1
            val h = res2?.get("h") as? Int ?: return 1
            (impl as? Impl)?.applyCustomResolution(this, w, h)
            return 1
        }
        if (info.containsKey("keep-aspect-change")) {
            val checked = info["keep-aspect-change"] as? Boolean ?: return 1
            (impl as? Impl)?.applyKeepAspectCheck(this, checked)
            return 1
        }
        if (info.containsKey("image-quality-change")) {
            val quality = info["image-quality-change"] as? Int ?: return 1
            (impl as? Impl)?.onImageQualityChange(this, quality)
            return 1
        }
        if (info.containsKey("image-format-change")) {
            (impl as? Impl)?.onImageFormatChange(this)
            return 1
        }
        return 0
    }

    override fun saveTexture() {
        val previewp = getPreviewView() ?: return
        previewp.saveTexture()
    }

    fun saveLocal(successCb: Any, failureCb: Any) {
        val previewp = getPreviewView() ?: return
        previewp.saveLocal(successCb, failureCb)
    }

    fun onExtendFloater() {
        impl?.setAdvanced(TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(AdvanceSnapshot)") as Boolean)
    }

    fun on360Snapshot() {
        TODO("APR: use JVM equivalent - LLFloaterReg::showInstance(360capture)")
        closeFloater()
    }

    fun isWaitingState(): Boolean = impl?.getStatus() == ImplBase.Status.WORKING

    companion object {
        fun getInstance(): FloaterSnapshot? =
            TODO("APR: use JVM equivalent - LLFloaterReg::getTypedInstance<FloaterSnapshot>(snapshot)")
        fun findInstance(): FloaterSnapshot? =
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterSnapshot>(snapshot)")

        fun update() {
            val inst = findInstance()
            if (inst != null) {
                inst.impl?.updateLivePreview()
            } else {
                ImplBase.updatePreviewList(false)
            }
        }

        fun setAgentEmail(email: String) {
            TODO("APR: use JVM equivalent - set agent email for postcard")
        }
    }

    class Impl(floater: FloaterSnapshotBase) : ImplBase(floater) {

        override fun getActivePanel(floater: FloaterSnapshotBase, okIfNotFound: Boolean): PanelSnapshot? {
            TODO("APR: use JVM equivalent - get active panel from panel_container SideTray")
        }

        override fun getImageFormat(floater: FloaterSnapshotBase): SnapshotModel.SnapshotFormat {
            val activePanel = getActivePanel(floater)
            return activePanel?.getImageFormat() ?: SnapshotModel.SnapshotFormat.SNAPSHOT_FORMAT_PNG
        }

        override fun getSnapshotPanelPrefix(): String = "panel_snapshot_"

        override fun getLayerType(floater: FloaterSnapshotBase): SnapshotModel.SnapshotLayerType {
            val value = floater.getChild<UICtrl>("layer_types")?.getValue() as? String ?: return SnapshotModel.SnapshotLayerType.SNAPSHOT_TYPE_COLOR
            return when (value) {
                "colors"  -> SnapshotModel.SnapshotLayerType.SNAPSHOT_TYPE_COLOR
                "depth"   -> SnapshotModel.SnapshotLayerType.SNAPSHOT_TYPE_DEPTH
                "depth24" -> SnapshotModel.SnapshotLayerType.SNAPSHOT_TYPE_DEPTH24
                else      -> SnapshotModel.SnapshotLayerType.SNAPSHOT_TYPE_COLOR
            }
        }

        override fun updateControls(floater: FloaterSnapshotBase) {
            val shotType   = getActiveSnapshotType(floater)
            val shotFormat = TODO("APR: use JVM equivalent - gSavedSettings.getS32(SnapshotFormat)") as? Int ?: 0
            val layerType  = getLayerType(floater)

            floater.getChild<ComboBox>("local_format_combo")?.selectNthItem(shotFormat)
            floater.getChildView("layer_types")?.setEnabled(shotType == SnapshotModel.SnapshotType.SNAPSHOT_LOCAL)

            val activePanel = getActivePanel(floater)
            if (activePanel != null && activePanel.getName() != "panel_snapshot_options") {
                val widthCtrl  = getWidthSpinner(floater)
                val heightCtrl = getHeightSpinner(floater)
                if (activePanel.isCustomResolution()) {
                    val renderUiInSnapshot = TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(RenderUIInSnapshot)") as? Boolean ?: false
                    val renderHudInSnapshot = TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(RenderHUDInSnapshot)") as? Boolean ?: false
                    if (renderUiInSnapshot || renderHudInSnapshot) {
                        val width  = TODO("APR: use JVM equivalent - gViewerWindow->getWindowWidthRaw()") as? Int ?: 0
                        val height = TODO("APR: use JVM equivalent - gViewerWindow->getWindowHeightRaw()") as? Int ?: 0
                        widthCtrl?.setMaxValue(width.toFloat())
                        heightCtrl?.setMaxValue(height.toFloat())
                        if ((widthCtrl?.getValue() as? Int ?: 0) > width)  widthCtrl?.forceSetValue(width)
                        if ((heightCtrl?.getValue() as? Int ?: 0) > height) heightCtrl?.forceSetValue(height)
                    }
                } else {
                    widthCtrl?.setMaxValue(TODO("APR: use JVM equivalent - MAX_SNAPSHOT_IMAGE_SIZE") as Float)
                    heightCtrl?.setMaxValue(TODO("APR: use JVM equivalent - MAX_SNAPSHOT_IMAGE_SIZE") as Float)
                }
            }

            val previewp   = getPreviewView()
            val gotSnap    = previewp?.getSnapshotUpToDate() ?: false

            when (shotType) {
                SnapshotModel.SnapshotType.SNAPSHOT_WEB -> {
                    floater.getChild<UICtrl>("layer_types")?.setValue("colors")
                    setResolution(floater, "profile_size_combo")
                }
                SnapshotModel.SnapshotType.SNAPSHOT_POSTCARD -> {
                    floater.getChild<UICtrl>("layer_types")?.setValue("colors")
                    setResolution(floater, "postcard_size_combo")
                }
                SnapshotModel.SnapshotType.SNAPSHOT_TEXTURE -> {
                    floater.getChild<UICtrl>("layer_types")?.setValue("colors")
                    setResolution(floater, "texture_size_combo")
                }
                SnapshotModel.SnapshotType.SNAPSHOT_LOCAL -> setResolution(floater, "local_size_combo")
                else -> {}
            }

            val keepAspect = TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(KeepAspectForSnapshot)") as? Boolean ?: false
            setAspectRatioCheckboxValue(floater, keepAspect)
            enableAspectRatioCheckbox(floater, !aspectRatioCheckOff)

            previewp?.setSnapshotType(shotType)
            previewp?.setSnapshotFormat(SnapshotModel.SnapshotFormat.values()[shotFormat])
            previewp?.setSnapshotBufferType(layerType)

            val currentPanel = getActivePanel(floater)
            if (currentPanel != null) {
                currentPanel.updateControls(mapOf("have-snapshot" to gotSnap))
            }
        }

        override fun setFinished(finished: Boolean, ok: Boolean, msg: String) {
            floater.setSuccessLabelPanelVisible(finished && ok)
            floater.setFailureLabelPanelVisible(finished && !ok)
            if (finished) {
                val lbl = floater.getChild<UICtrl>(if (ok) "succeeded_lbl" else "failed_lbl")
                val resultText = floater.getString("${msg}_${if (ok) "succeeded_str" else "failed_str"}")
                lbl?.setValue(resultText)
                val panel = getActivePanel(floater)
                if (panel != null) {
                    val isCustom = panel.isCustomResolution()
                    panel.enableAspectRatioCheckbox(isCustom)
                    panel.getWidthSpinner()?.setEnabled(isCustom)
                    panel.getHeightSpinner()?.setEnabled(isCustom)
                }
            }
        }

        fun getWidthSpinner(floater: FloaterSnapshotBase): SpinCtrl? {
            val panel = getActivePanel(floater)
            return panel?.getWidthSpinner() ?: floater.getChild<SpinCtrl>("snapshot_width")
        }

        fun getHeightSpinner(floater: FloaterSnapshotBase): SpinCtrl? {
            val panel = getActivePanel(floater)
            return panel?.getHeightSpinner() ?: floater.getChild<SpinCtrl>("snapshot_height")
        }

        fun enableAspectRatioCheckbox(floater: FloaterSnapshotBase, enable: Boolean) {
            getActivePanel(floater)?.enableAspectRatioCheckbox(enable)
        }

        fun setAspectRatioCheckboxValue(floater: FloaterSnapshotBase, checked: Boolean) {
            val panel = getActivePanel(floater) ?: return
            floater.getChild<UICtrl>(panel.getAspectRatioCBName())?.setValue(checked)
        }

        fun setResolution(floater: FloaterSnapshotBase, comboname: String) {
            val combo = floater.getChild<ComboBox>(comboname) ?: return
            combo.setVisible(true)
            updateResolution(combo, floater, false)
        }

        fun applyKeepAspectCheck(view: FloaterSnapshotBase, checked: Boolean) {
            TODO("APR: use JVM equivalent - gSavedSettings.setBOOL(KeepAspectForSnapshot, checked)")
            if (checked) {
                val activePanel = getActivePanel(view)
                if (activePanel != null) {
                    floater.getChild<ComboBox>(activePanel.getImageSizeComboName())
                        ?.setCurrentByIndex(floater.getChild<ComboBox>(activePanel.getImageSizeComboName())?.getItemCount()?.minus(1) ?: 0)
                }
            }
            val previewp = getPreviewView() ?: return
            previewp.keepAspectRatio = checked
            var w = 0; var h = 0
            previewp.getSize(w, h)
            updateSpinners(view, previewp, w, h, true)
            previewp.setSize(w, h)
            previewp.updateSnapshot(true)
            checkAutoSnapshot(previewp, true)
        }

        fun updateResolution(ctrl: UICtrl?, data: Any?, doUpdate: Boolean = true) {
            val combobox = ctrl as? ComboBox ?: return
            val view = data as? FloaterSnapshot ?: return
            TODO("APR: use JVM equivalent - parse LLSD resolution from combo, set preview size, check aspect")
        }

        fun applyCustomResolution(view: FloaterSnapshotBase, w: Int, h: Int) {
            val previewp = getPreviewView() ?: return
            TODO("APR: use JVM equivalent - set preview size and trigger update")
        }

        fun onImageQualityChange(view: FloaterSnapshotBase, qualityVal: Int) {
            getPreviewView()?.setSnapshotQuality(qualityVal)
        }

        fun onImageFormatChange(view: FloaterSnapshotBase) {
            TODO("APR: use JVM equivalent - gSavedSettings.setS32(SnapshotFormat, getImageFormat(view))")
            getPreviewView()?.updateSnapshot(true)
            updateControls(view)
        }

        fun checkImageSize(previewp: SnapshotLivePreview?, width: Int, height: Int, isWidthChanged: Boolean, maxValue: Int): Boolean {
            if (previewp == null || !previewp.keepAspectRatio) return false
            TODO("APR: use JVM equivalent - gViewerWindow->getWindowWidthRaw/HeightRaw for aspect calc")
        }

        fun setImageSizeSpinnersValues(view: FloaterSnapshotBase, width: Int, height: Int) {
            getWidthSpinner(view)?.forceSetValue(width)
            getHeightSpinner(view)?.forceSetValue(height)
            if (getActiveSnapshotType(view) == SnapshotModel.SnapshotType.SNAPSHOT_TEXTURE) {
                getWidthSpinner(view)?.setIncrement((width shr 1).toFloat())
                getHeightSpinner(view)?.setIncrement((height shr 1).toFloat())
            }
        }

        fun updateSpinners(view: FloaterSnapshotBase, previewp: SnapshotLivePreview?, width: Int, height: Int, isWidthChanged: Boolean) {
            getWidthSpinner(view)?.resetDirty()
            getHeightSpinner(view)?.resetDirty()
            if (checkImageSize(previewp, width, height, isWidthChanged, previewp?.getMaxImageSize() ?: 0)) {
                setImageSizeSpinnersValues(view, width, height)
            }
        }

        private fun checkAspectRatio(view: FloaterSnapshotBase, index: Int) {
            val previewp = getPreviewView() ?: return
            if (getActiveSnapshotType(view) == SnapshotModel.SnapshotType.SNAPSHOT_TEXTURE) {
                previewp.keepAspectRatio = false
                return
            }
            val (keepAspect, enableCb) = when (index) {
                0  -> Pair(true, false)
                -1 -> Pair(TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(KeepAspectForSnapshot)") as Boolean, true)
                else -> Pair(false, false)
            }
            aspectRatioCheckOff = !enableCb
            previewp.keepAspectRatio = keepAspect
        }

        private fun comboSetCustom(floater: FloaterSnapshotBase, comboname: String) {
            val combo = floater.getChild<ComboBox>(comboname) ?: return
            combo.setCurrentByIndex(combo.getItemCount() - 1)
            checkAspectRatio(floater, -1)
        }

        companion object {
            fun onCommitLayerTypes(ctrl: UICtrl?, data: Any?) {
                val combobox = ctrl as? ComboBox ?: return
                val view = data as? FloaterSnapshot ?: return
                val previewp = view.getPreviewView() ?: return
                previewp.setSnapshotBufferType(SnapshotModel.SnapshotLayerType.values()[combobox.getCurrentIndex()])
                view.impl?.let { (it as? ImplBase)?.checkAutoSnapshot(previewp, true) }
                previewp.updateSnapshot(true, true)
            }

            fun onSnapshotUploadFinished(floater: FloaterSnapshotBase, status: Boolean) {
                floater.impl?.setStatus(ImplBase.Status.FINISHED, status, "profile")
            }

            fun onSendingPostcardFinished(floater: FloaterSnapshotBase, status: Boolean) {
                floater.impl?.setStatus(ImplBase.Status.FINISHED, status, "postcard")
            }
        }
    }
}

class SnapshotFloaterView(params: Any) : FloaterView(params) {
    fun handleKey(key: Int, mask: Int, calledFromParent: Boolean): Boolean =
        TODO("APR: use JVM equivalent - handle key event in snapshot floater view")

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean =
        TODO("APR: use JVM equivalent - handle mouse down in snapshot floater view")

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean =
        TODO("APR: use JVM equivalent - handle mouse up in snapshot floater view")

    fun handleHover(x: Int, y: Int, mask: Int): Boolean =
        TODO("APR: use JVM equivalent - handle hover in snapshot floater view")
}
