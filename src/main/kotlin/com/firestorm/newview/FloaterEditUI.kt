package com.firestorm.newview

import kotlin.math.log10

class FloaterTools(private val key: Map<String, Any>) {

    var mBtnFocus: Button? = null
    var mBtnMove: Button? = null
    var mBtnEdit: Button? = null
    var mBtnCreate: Button? = null
    var mBtnLand: Button? = null
    var mTextStatus: TextBox? = null

    var mRadioGroupFocus: RadioGroup? = null
    var mRadioGroupMove: RadioGroup? = null
    var mRadioGroupEdit: RadioGroup? = null

    var mCheckSelectIndividual: CheckBoxCtrl? = null
    var mBtnLink: Button? = null
    var mBtnUnlink: Button? = null
    var mBtnPrevPart: Button? = null
    var mBtnNextPart: Button? = null

    var mCheckSnapToGrid: CheckBoxCtrl? = null
    var mBtnGridOptions: Button? = null
    var mComboGridMode: ComboBox? = null
    var mCheckStretchUniform: CheckBoxCtrl? = null
    var mCheckStretchTexture: CheckBoxCtrl? = null
    var mCheckShowHighlight: CheckBoxCtrl? = null
    var mCheckActualRoot: CheckBoxCtrl? = null
    var mCheckSelectProbes: CheckBoxCtrl? = null

    var mBtnRotateLeft: Button? = null
    var mBtnRotateReset: Button? = null
    var mBtnRotateRight: Button? = null

    var mBtnDelete: Button? = null
    var mBtnDuplicate: Button? = null
    var mBtnDuplicateInPlace: Button? = null

    var mTextSelectionCount: TextBox? = null
    var mTextSelectionEmpty: TextBox? = null
    var mTextSelectionFaces: TextBox? = null
    var mSliderZoom: Slider? = null

    var mTextLinkNumObjCount: TextBox? = null
    var mTextMoreInfoLabel: TextBox? = null
    var mBtnCopyKeys: Button? = null

    var mTreeGrassCombo: ComboBox? = null

    var mCheckSticky: CheckBoxCtrl? = null
    var mCheckCopySelection: CheckBoxCtrl? = null
    var mCheckCopyCenters: CheckBoxCtrl? = null
    var mCheckCopyRotates: CheckBoxCtrl? = null

    var mRadioGroupLand: RadioGroup? = null
    var mSliderDozerSize: Slider? = null
    var mSliderDozerForce: Slider? = null
    var mTextBulldozer: TextBox? = null
    var mTextDozerSize: TextBox? = null
    var mTextDozerStrength: TextBox? = null

    var mBtnApplyToSelection: Button? = null
    val mButtons: MutableList<Button> = mutableListOf()

    var mTab: TabContainer? = null
    var mPanelPermissions: PanelPermissions? = null
    var mPanelObject: PanelObject? = null
    var mPanelVolume: PanelVolume? = null
    var mPanelContents: PanelContents? = null
    var mPanelLandInfo: PanelLandInfo? = null

    var mCostTextBorder: ViewBorder? = null
    var mTabLand: TabContainer? = null

    private val mStatusText: MutableMap<String, String> = mutableMapOf()
    private var mDirty: Boolean = true
    private var mHasSelection: Boolean = true
    private var mOpen: Boolean = false
    private var mCollapsedHeight: Int = 0
    private var mExpandedHeight: Int = 0

    companion object {
        var sShowObjectCost: Boolean = true
        var sPreviousFocusOnAvatar: Boolean = false

        val PANEL_NAMES = arrayOf("General", "Object", "Features", "Texture", "Content")

        val TOOL_NAMES = arrayOf(
            "ToolCube", "ToolPrism", "ToolPyramid", "ToolTetrahedron",
            "ToolCylinder", "ToolHemiCylinder", "ToolCone", "ToolHemiCone",
            "ToolSphere", "ToolHemiSphere", "ToolTorus", "ToolTube",
            "ToolRing", "ToolTree", "ToolGrass"
        )

        fun setGridMode(mode: Int) {
            TODO("GPU: forward grid mode $mode to LLSelectMgr / grid options")
        }

        fun setEditTool(data: Any?) {
            TODO("GPU: activate edit tool from data=$data via LLToolMgr")
        }
    }

    enum class InfoPanel {
        PANEL_GENERAL, PANEL_OBJECT, PANEL_FEATURES, PANEL_FACE, PANEL_CONTENTS;
        companion object { const val PANEL_COUNT = 5 }
    }

    open fun postBuild(): Boolean {
        TODO("GPU: build full tool floater UI — wire all button/checkbox/combo children from XML")
    }

    open fun onOpen(key: Map<String, Any>) {
        TODO("GPU: restore tool state on open")
    }

    open fun canClose(): Boolean = true

    open fun onClose(appQuitting: Boolean) {
        resetToolState()
        TODO("GPU: restore focus to avatar if sPreviousFocusOnAvatar was set")
    }

    open fun draw() {
        if (mDirty) {
            refresh()
            mDirty = false
        }
        TODO("GPU: draw() — delegate to parent LLFloater draw")
    }

    open fun onFocusReceived() {
        TODO("GPU: push build tool to LLToolMgr on focus")
    }

    fun updatePopup(centerX: Int, centerY: Int, mask: Int) {
        TODO("GPU: reposition floater popup, update button highlights from LLToolMgr state, mask=$mask")
    }

    fun updateToolsSizeLimits() {
        mPanelObject?.updateLimits(false)
    }

    fun changePrecision(decimalPrecision: Int) {
        val clamped = decimalPrecision.coerceIn(0, 7)
        mPanelObject?.changePrecision(clamped)
        mPanelVolume?.changePrecision(clamped)
    }

    fun resetToolState() {
        TODO("GPU: clear any transient tool options (grid snap, copy mode, etc.)")
    }

    fun dirty() {
        mDirty = true
    }

    fun showPanel(panel: InfoPanel) {
        mTab?.selectTabByName(PANEL_NAMES[panel.ordinal])
    }

    fun setStatusText(text: String) {
        mTextStatus?.setValue(mStatusText[text] ?: text)
    }

    fun setTool(userData: Map<String, Any>) {
        TODO("GPU: parse tool key from userData and activate via LLToolMgr")
    }

    fun saveLastTool() {
        TODO("GPU: persist current active tool to gSavedSettings")
    }

    fun updateLandImpacts() {
        TODO("GPU: query LLViewerParcelMgr for current parcel land impact and update UI text")
    }

    fun getTextureDropChannel(): Int {
        TODO("GPU: return active texture drop channel from FSPanelFace / LLPanelFace")
    }

    fun getTextureChannelToEdit(): Int {
        TODO("GPU: return texture channel currently selected for editing")
    }

    fun getPBRDropChannel(): Int {
        TODO("GPU: return active PBR texture slot from FSPanelFace")
    }

    fun createDefaultMaterial(oldMat: Any?): Any? {
        TODO("GPU: clone or construct a default LLMaterial based on oldMat")
    }

    fun refreshPanelFace() {
        TODO("GPU: trigger refresh on the active face panel (LLPanelFace or FSPanelFace)")
    }

    fun onClickBtnCopyKeys() {
        TODO("GPU: copy selected object UUIDs to clipboard via LLSelectMgr")
    }

    fun onClickExpand() {
        val expanded = mTab?.isVisible() ?: false
        if (expanded) {
            mTab?.setVisible(false)
            reshape(getWidth(), mCollapsedHeight)
        } else {
            mTab?.setVisible(true)
            reshape(getWidth(), mExpandedHeight)
        }
        TODO("GPU: persist FSToolboxExpanded setting and update button arrow overlay")
    }

    private fun refresh() {
        TODO("GPU: refresh all tool panel states from LLSelectMgr / LLToolMgr")
    }

    private fun onClickGridOptions() {
        TODO("GPU: open LLFloaterBuildOptions")
    }

    private fun buildTreeGrassCombo() {
        TODO("GPU: populate mTreeGrassCombo with available tree/grass variants from LLVOTree/LLVOGrass")
    }

    private fun onSelectTreeGrassCombo() {
        TODO("GPU: apply selected tree/grass pcode to the create tool placer")
    }

    private fun getWidth(): Int = 0
    private fun reshape(width: Int, height: Int) {
        TODO("GPU: reshape floater to ($width × $height)")
    }

    class Button { fun setEnabled(v: Boolean) {} }
    class TextBox {
        fun setValue(v: String) {}
        fun setEnabled(v: Boolean) {}
    }
    class RadioGroup
    class CheckBoxCtrl {
        fun getValue(): Boolean = false
        fun setValue(v: Boolean) {}
    }
    class ComboBox
    class Slider
    class TabContainer {
        fun selectTabByName(name: String) {}
        fun isVisible(): Boolean = false
        fun setVisible(v: Boolean) {}
        fun selectFirstTab() {}
    }
    class ViewBorder
    class PanelPermissions
    class PanelObject {
        fun updateLimits(v: Boolean) {}
        fun changePrecision(p: Int) {}
    }
    class PanelVolume { fun changePrecision(p: Int) {} }
    class PanelContents
    class PanelLandInfo
}
