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
            System.err.println("FloaterTools: setGridMode not yet implemented")
        }

        fun setEditTool(data: Any?) {
            System.err.println("FloaterTools: setEditTool not yet implemented")
        }
    }

    enum class InfoPanel {
        PANEL_GENERAL, PANEL_OBJECT, PANEL_FEATURES, PANEL_FACE, PANEL_CONTENTS;
        companion object { const val PANEL_COUNT = 5 }
    }

    open fun postBuild(): Boolean {
        System.err.println("FloaterTools: postBuild not yet implemented")
        return false
    }

    open fun onOpen(key: Map<String, Any>) {
        System.err.println("FloaterTools: onOpen not yet implemented")
    }

    open fun canClose(): Boolean = true

    open fun onClose(appQuitting: Boolean) {
        resetToolState()
        System.err.println("FloaterTools: onClose not yet implemented")
    }

    open fun draw() {
        if (mDirty) {
            refresh()
            mDirty = false
        }
        // no-op
    }

    open fun onFocusReceived() {
        System.err.println("FloaterTools: onFocusReceived not yet implemented")
    }

    fun updatePopup(centerX: Int, centerY: Int, mask: Int) {
        System.err.println("FloaterTools: updatePopup not yet implemented")
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
        System.err.println("FloaterTools: resetToolState not yet implemented")
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
        System.err.println("FloaterTools: setTool not yet implemented")
    }

    fun saveLastTool() {
        System.err.println("FloaterTools: saveLastTool not yet implemented")
    }

    fun updateLandImpacts() {
        System.err.println("FloaterTools: updateLandImpacts not yet implemented")
    }

    fun getTextureDropChannel(): Int {
        System.err.println("FloaterTools: getTextureDropChannel not yet implemented")
        return 0
    }

    fun getTextureChannelToEdit(): Int {
        System.err.println("FloaterTools: getTextureChannelToEdit not yet implemented")
        return 0
    }

    fun getPBRDropChannel(): Int {
        System.err.println("FloaterTools: getPBRDropChannel not yet implemented")
        return 0
    }

    fun createDefaultMaterial(oldMat: Any?): Any? {
        System.err.println("FloaterTools: createDefaultMaterial not yet implemented")
        return null
    }

    fun refreshPanelFace() {
        System.err.println("FloaterTools: refreshPanelFace not yet implemented")
    }

    fun onClickBtnCopyKeys() {
        System.err.println("FloaterTools: onClickBtnCopyKeys not yet implemented")
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
        System.err.println("FloaterTools: onClickExpand not yet implemented")
    }

    private fun refresh() {
        System.err.println("FloaterTools: refresh not yet implemented")
    }

    private fun onClickGridOptions() {
        System.err.println("FloaterTools: onClickGridOptions not yet implemented")
    }

    private fun buildTreeGrassCombo() {
        System.err.println("FloaterTools: buildTreeGrassCombo not yet implemented")
    }

    private fun onSelectTreeGrassCombo() {
        System.err.println("FloaterTools: onSelectTreeGrassCombo not yet implemented")
    }

    private fun getWidth(): Int = 0
    private fun reshape(width: Int, height: Int) {
        System.err.println("FloaterTools: reshape not yet implemented")
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
