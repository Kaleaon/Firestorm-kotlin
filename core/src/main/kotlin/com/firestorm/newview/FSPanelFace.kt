package com.firestorm.newview

import java.util.UUID

// Dirty-flag bit positions for GLTF material tracking (mirrors llmaterialeditor.cpp)
private const val MATERIAL_BASE_COLOR_DIRTY: UInt                    = 0x1u shl 0
private const val MATERIAL_BASE_COLOR_TEX_DIRTY: UInt                = 0x1u shl 1
private const val MATERIAL_NORMAL_TEX_DIRTY: UInt                    = 0x1u shl 2
private const val MATERIAL_METALLIC_ROUGHTNESS_TEX_DIRTY: UInt       = 0x1u shl 3
private const val MATERIAL_METALLIC_ROUGHTNESS_METALNESS_DIRTY: UInt = 0x1u shl 4
private const val MATERIAL_METALLIC_ROUGHTNESS_ROUGHNESS_DIRTY: UInt = 0x1u shl 5
private const val MATERIAL_EMISIVE_COLOR_DIRTY: UInt                 = 0x1u shl 6
private const val MATERIAL_EMISIVE_TEX_DIRTY: UInt                   = 0x1u shl 7
private const val MATERIAL_DOUBLE_SIDED_DIRTY: UInt                  = 0x1u shl 8
private const val MATERIAL_ALPHA_MODE_DIRTY: UInt                    = 0x1u shl 9
private const val MATERIAL_ALPHA_CUTOFF_DIRTY: UInt                  = 0x1u shl 10

class FSPanelFace {

    companion object {
        const val MATMEDIA_MATERIAL = 0
        const val MATMEDIA_PBR = 1
        const val MATMEDIA_MEDIA = 2
        const val BUMPY_TEXTURE = 18
        const val SHINY_TEXTURE = 4

        fun onMaterialOverrideReceived(objectId: UUID, side: Int) {
            System.err.println("FSPanelFace: onMaterialOverrideReceived not yet implemented")
        }

        fun syncRepeatX(self: FSPanelFace, scaleU: Float) {
            System.err.println("FSPanelFace: syncRepeatX not yet implemented")
        }

        fun syncRepeatY(self: FSPanelFace, scaleV: Float) {
            System.err.println("FSPanelFace: syncRepeatY not yet implemented")
        }

        fun syncOffsetX(self: FSPanelFace, offsetU: Float) {
            System.err.println("FSPanelFace: syncOffsetX not yet implemented")
        }

        fun syncOffsetY(self: FSPanelFace, offsetV: Float) {
            System.err.println("FSPanelFace: syncOffsetY not yet implemented")
        }

        fun syncMaterialRot(self: FSPanelFace, rot: Float, te: Int = -1) {
            System.err.println("FSPanelFace: syncMaterialRot not yet implemented")
        }
    }

    // -------------------------------------------------------------------------
    // UI control references (set during postBuild from the skin XML)
    // -------------------------------------------------------------------------

    // Blinn-Phong UV spin controls (public for functor access)
    var ctrlTexScaleU: Any? = null
    var ctrlTexScaleV: Any? = null
    var ctrlBumpyScaleU: Any? = null
    var ctrlBumpyScaleV: Any? = null
    var ctrlShinyScaleU: Any? = null
    var ctrlShinyScaleV: Any? = null
    var ctrlTexOffsetU: Any? = null
    var ctrlTexOffsetV: Any? = null
    var ctrlBumpyOffsetU: Any? = null
    var ctrlBumpyOffsetV: Any? = null
    var ctrlShinyOffsetU: Any? = null
    var ctrlShinyOffsetV: Any? = null
    var ctrlTexRot: Any? = null
    var ctrlBumpyRot: Any? = null
    var ctrlShinyRot: Any? = null

    // Blinn-Phong combo/check controls (public for functor access)
    var comboTexGen: Any? = null
    var checkPlanarAlign: Any? = null

    // Tab container (public for functor access)
    var tabsMatChannel: Any? = null

    // Private controls
    private var tabsPBRMatMedia: Any? = null
    private var tabsPBRChannel: Any? = null
    private var setChannelTab: Boolean = false

    private var btnCopyFaces: Any? = null
    private var btnPasteFaces: Any? = null
    private var ctrlGlow: Any? = null
    private var ctrlRpt: Any? = null

    private var ctrlColorTransp: Any? = null
    private var colorTransPercent: Any? = null
    private var labelAlphaMode: Any? = null
    private var comboAlphaMode: Any? = null
    private var ctrlMaskCutoff: Any? = null
    private var checkFullbright: Any? = null
    private var checkHideWater: Any? = null

    private var labelBumpiness: Any? = null
    private var comboBumpiness: Any? = null
    private var labelShininess: Any? = null
    private var comboShininess: Any? = null
    private var ctrlGlossiness: Any? = null
    private var ctrlEnvironment: Any? = null

    private var colorSwatch: Any? = null
    private var textureCtrl: Any? = null
    private var bumpyTextureCtrl: Any? = null
    private var shinyTextureCtrl: Any? = null
    private var shinyColorSwatch: Any? = null

    private var btnAlignMedia: Any? = null
    private var btnAlignTextures: Any? = null
    private var checkSyncMaterials: Any? = null

    private var btnDeleteMedia: Any? = null
    private var btnAddMedia: Any? = null
    private var titleMedia: Any? = null
    private var titleMediaText: Any? = null

    private var materialCtrlPBR: Any? = null
    private var baseTintPBR: Any? = null
    private var baseTexturePBR: Any? = null
    private var normalTexturePBR: Any? = null
    private var ormTexturePBR: Any? = null
    private var emissiveTexturePBR: Any? = null
    private var emissiveTintPBR: Any? = null
    private var checkDoubleSidedPBR: Any? = null
    private var alphaPBR: Any? = null
    private var labelAlphaModePBR: Any? = null
    private var alphaModePBR: Any? = null
    private var maskCutoffPBR: Any? = null
    private var metallicFactorPBR: Any? = null
    private var roughnessFactorPBR: Any? = null
    private var btnSavePBR: Any? = null

    private data class PBRBaseMaterialParams(
        val map: MutableList<UUID> = MutableList(4) { UUID.randomUUID() },
        var baseColorTint: FloatArray = FloatArray(4),
        var metallic: Float = 0f,
        var roughness: Float = 0f,
        var emissiveTint: FloatArray = FloatArray(4),
        var alphaMode: Int = 0,
        var alphaCutoff: Float = 0f,
        var doubleSided: Boolean = false
    )

    private val pbrBaseMaterialParams = PBRBaseMaterialParams()

    private var unsavedChanges: UInt = 0u
    private var revertedChanges: UInt = 0u

    private var isAlpha: Boolean = false
    private var excludeWater: Boolean = false

    private var clipboardParams: Map<String, Any> = emptyMap()
    private var mediaSettings: Map<String, Any> = emptyMap()
    private var needMediaTitle: Boolean = true

    private var agentInventoryListener: Any? = null
    private var voInventoryListener: Any? = null

    // -------------------------------------------------------------------------
    // Selection tracking inner class
    // -------------------------------------------------------------------------

    private inner class Selection {
        private var changed: Boolean = false
        private var selectConnection: (() -> Unit)? = null
        private var needsSelectionCheck: Boolean = true
        private var selectedObjectCount: Int = 0
        private var selectedTECount: Int = 0
        private var selectedObjectId: UUID? = null
        private var lastSelectedSide: Int = -1

        fun connect() {
            System.err.println("FSPanelFace: Selection.connect not yet implemented")
        }

        fun update(): Boolean {
            return false
        }

        fun setDirty() { changed = true }

        fun onSelectionChanged() { needsSelectionCheck = true }

        fun onSelectedObjectUpdated(objectId: UUID, side: Int) {
            System.err.println("FSPanelFace: Selection.onSelectedObjectUpdated not yet implemented")
        }

        private fun compareSelection(): Boolean {
            return false
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    init {
        System.err.println("FSPanelFace: init not yet implemented")
    }

    fun postBuild(): Boolean {
        return false
    }

    fun refresh() {
        System.err.println("FSPanelFace: refresh not yet implemented")
    }

    fun refreshMedia() {
        System.err.println("FSPanelFace: refreshMedia not yet implemented")
    }

    fun unloadMedia() {
        System.err.println("FSPanelFace: unloadMedia not yet implemented")
    }

    fun changePrecision(decimalPrecision: Int) {
        System.err.println("FSPanelFace: changePrecision not yet implemented")
    }

    open fun onVisibilityChange(newVisibility: Boolean) {
        System.err.println("FSPanelFace: onVisibilityChange not yet implemented")
    }

    open fun draw() {
        System.err.println("FSPanelFace: draw not yet implemented")
    }

    fun createDefaultMaterial(currentMaterial: Any?): Any? {
        return null
    }

    fun getTextureChannelToEdit(): Int {
        return 0
    }

    fun getTextureDropChannel(): Int {
        return 0
    }

    fun getPbrDropChannel(): Int {
        return 0
    }

    // -------------------------------------------------------------------------
    // Media helpers
    // -------------------------------------------------------------------------

    protected fun navigateToTitleMedia(url: String) {
        System.err.println("FSPanelFace: navigateToTitleMedia not yet implemented")
    }

    protected fun selectedMediaEditable(): Boolean {
        return false
    }

    protected fun clearMediaSettings() {
        mediaSettings = emptyMap()
        needMediaTitle = true
    }

    protected fun updateMediaSettings() {
        System.err.println("FSPanelFace: updateMediaSettings not yet implemented")
    }

    protected fun updateMediaTitle() {
        System.err.println("FSPanelFace: updateMediaTitle not yet implemented")
    }

    protected fun isMediaTexSelected(): Boolean {
        return false
    }

    // -------------------------------------------------------------------------
    // State reader
    // -------------------------------------------------------------------------

    protected fun getState() {
        System.err.println("FSPanelFace: getState not yet implemented")
    }

    // -------------------------------------------------------------------------
    // Send-to-server helpers
    // -------------------------------------------------------------------------

    protected fun sendTexture() { System.err.println("FSPanelFace: sendTexture not yet implemented") }
    protected fun sendTextureInfo() { System.err.println("FSPanelFace: sendTextureInfo not yet implemented") }
    protected fun sendColor() { System.err.println("FSPanelFace: sendColor not yet implemented") }
    protected fun sendAlpha() { System.err.println("FSPanelFace: sendAlpha not yet implemented") }
    protected fun sendBump(bumpiness: UInt) { System.err.println("FSPanelFace: sendBump not yet implemented") }
    protected fun sendTexGen() { System.err.println("FSPanelFace: sendTexGen not yet implemented") }
    protected fun sendShiny(shininess: UInt) { System.err.println("FSPanelFace: sendShiny not yet implemented") }
    protected fun sendFullbright() { System.err.println("FSPanelFace: sendFullbright not yet implemented") }
    protected fun sendGlow() { System.err.println("FSPanelFace: sendGlow not yet implemented") }
    protected fun alignTextureLayer() { System.err.println("FSPanelFace: alignTextureLayer not yet implemented") }
    protected fun updateCopyTexButton() { System.err.println("FSPanelFace: updateCopyTexButton not yet implemented") }

    // -------------------------------------------------------------------------
    // UI Callbacks – common
    // -------------------------------------------------------------------------

    protected fun onCopyFaces() { System.err.println("FSPanelFace: onCopyFaces not yet implemented") }
    protected fun onPasteFaces() { System.err.println("FSPanelFace: onPasteFaces not yet implemented") }
    protected fun onCommitHideWater() { System.err.println("FSPanelFace: onCommitHideWater not yet implemented") }
    protected fun onCommitGlow() { sendGlow() }
    protected fun onCommitRepeatsPerMeter() { System.err.println("FSPanelFace: onCommitRepeatsPerMeter not yet implemented") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Blinn-Phong alpha
    // -------------------------------------------------------------------------

    protected fun onCommitAlpha() { sendAlpha() }
    protected fun onCommitAlphaMode() { System.err.println("FSPanelFace: onCommitAlphaMode not yet implemented") }
    protected fun onCommitMaterialMaskCutoff() { System.err.println("FSPanelFace: onCommitMaterialMaskCutoff not yet implemented") }
    protected fun onCommitFullbright() { sendFullbright() }

    // -------------------------------------------------------------------------
    // UI Callbacks – Blinn-Phong texture transforms
    // -------------------------------------------------------------------------

    protected fun onCommitTexGen() { sendTexGen() }
    protected fun onCommitPlanarAlign() { System.err.println("FSPanelFace: onCommitPlanarAlign not yet implemented") }
    protected fun onCommitBump() { System.err.println("FSPanelFace: onCommitBump not yet implemented") }
    protected fun onCommitShiny() { System.err.println("FSPanelFace: onCommitShiny not yet implemented") }
    protected fun onCommitTextureScaleX() { System.err.println("FSPanelFace: onCommitTextureScaleX not yet implemented") }
    protected fun onCommitTextureScaleY() { System.err.println("FSPanelFace: onCommitTextureScaleY not yet implemented") }
    protected fun onCommitTextureOffsetX() { System.err.println("FSPanelFace: onCommitTextureOffsetX not yet implemented") }
    protected fun onCommitTextureOffsetY() { System.err.println("FSPanelFace: onCommitTextureOffsetY not yet implemented") }
    protected fun onCommitTextureRot() { System.err.println("FSPanelFace: onCommitTextureRot not yet implemented") }
    protected fun onCommitMaterialBumpyScaleX() { System.err.println("FSPanelFace: onCommitMaterialBumpyScaleX not yet implemented") }
    protected fun onCommitMaterialBumpyScaleY() { System.err.println("FSPanelFace: onCommitMaterialBumpyScaleY not yet implemented") }
    protected fun onCommitMaterialBumpyOffsetX() { System.err.println("FSPanelFace: onCommitMaterialBumpyOffsetX not yet implemented") }
    protected fun onCommitMaterialBumpyOffsetY() { System.err.println("FSPanelFace: onCommitMaterialBumpyOffsetY not yet implemented") }
    protected fun onCommitMaterialBumpyRot() { System.err.println("FSPanelFace: onCommitMaterialBumpyRot not yet implemented") }
    protected fun onCommitMaterialShinyScaleX() { System.err.println("FSPanelFace: onCommitMaterialShinyScaleX not yet implemented") }
    protected fun onCommitMaterialShinyScaleY() { System.err.println("FSPanelFace: onCommitMaterialShinyScaleY not yet implemented") }
    protected fun onCommitMaterialShinyOffsetX() { System.err.println("FSPanelFace: onCommitMaterialShinyOffsetX not yet implemented") }
    protected fun onCommitMaterialShinyOffsetY() { System.err.println("FSPanelFace: onCommitMaterialShinyOffsetY not yet implemented") }
    protected fun onCommitMaterialShinyRot() { System.err.println("FSPanelFace: onCommitMaterialShinyRot not yet implemented") }
    protected fun onCommitMaterialGloss() { System.err.println("FSPanelFace: onCommitMaterialGloss not yet implemented") }
    protected fun onCommitMaterialEnv() { System.err.println("FSPanelFace: onCommitMaterialEnv not yet implemented") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Diffuse color swatch
    // -------------------------------------------------------------------------

    protected fun onCommitColor() { sendColor() }
    protected fun onCancelColor() { System.err.println("FSPanelFace: onCancelColor not yet implemented") }
    protected fun onSelectColor() { System.err.println("FSPanelFace: onSelectColor not yet implemented") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Diffuse texture swatch
    // -------------------------------------------------------------------------

    protected fun onSelectTexture() { System.err.println("FSPanelFace: onSelectTexture not yet implemented") }
    protected fun onCommitTexture() { sendTexture() }
    protected fun onCancelTexture() { System.err.println("FSPanelFace: onCancelTexture not yet implemented") }
    protected fun onDragTexture(textureCtrl: Any?, item: Any?): Boolean {
        return false
    }
    protected fun onCloseTexturePicker() { System.err.println("FSPanelFace: onCloseTexturePicker not yet implemented") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Normal/Specular texture swatches
    // -------------------------------------------------------------------------

    protected fun onCommitNormalTexture() { System.err.println("FSPanelFace: onCommitNormalTexture not yet implemented") }
    protected fun onCancelNormalTexture() { System.err.println("FSPanelFace: onCancelNormalTexture not yet implemented") }
    protected fun onCommitSpecularTexture() { System.err.println("FSPanelFace: onCommitSpecularTexture not yet implemented") }
    protected fun onCancelSpecularTexture() { System.err.println("FSPanelFace: onCancelSpecularTexture not yet implemented") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Specular color swatch
    // -------------------------------------------------------------------------

    protected fun onCommitShinyColor() { System.err.println("FSPanelFace: onCommitShinyColor not yet implemented") }
    protected fun onCancelShinyColor() { System.err.println("FSPanelFace: onCancelShinyColor not yet implemented") }
    protected fun onSelectShinyColor() { System.err.println("FSPanelFace: onSelectShinyColor not yet implemented") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Alignment & sync
    // -------------------------------------------------------------------------

    protected fun onClickAutoFix() { System.err.println("FSPanelFace: onClickAutoFix not yet implemented") }
    protected fun onAlignTexture() { alignTextureLayer() }
    protected fun onClickMapsSync() { System.err.println("FSPanelFace: onClickMapsSync not yet implemented") }
    protected fun onTextureSelectionChanged(textureCtrl: Any?) {
        System.err.println("FSPanelFace: onTextureSelectionChanged not yet implemented")
    }

    // -------------------------------------------------------------------------
    // UI Callbacks – Media
    // -------------------------------------------------------------------------

    protected fun onClickBtnEditMedia() { System.err.println("FSPanelFace: onClickBtnEditMedia not yet implemented") }
    protected fun onClickBtnDeleteMedia() { System.err.println("FSPanelFace: onClickBtnDeleteMedia not yet implemented") }
    protected fun onClickBtnAddMedia() { System.err.println("FSPanelFace: onClickBtnAddMedia not yet implemented") }

    protected fun alignMaterialsProperties() {
        System.err.println("FSPanelFace: alignMaterialsProperties not yet implemented")
    }

    // -------------------------------------------------------------------------
    // UI Callbacks – PBR material
    // -------------------------------------------------------------------------

    protected fun onCommitPbr() { System.err.println("FSPanelFace: onCommitPbr not yet implemented") }
    protected fun onCancelPbr() { System.err.println("FSPanelFace: onCancelPbr not yet implemented") }
    protected fun onSelectPbr() { System.err.println("FSPanelFace: onSelectPbr not yet implemented") }
    protected fun onDragPbr(item: Any?): Boolean {
        return false
    }
    protected fun onPbrSelectionChanged(item: Any?) {
        System.err.println("FSPanelFace: onPbrSelectionChanged not yet implemented")
    }
    protected fun onClickBtnSavePBR() { System.err.println("FSPanelFace: onClickBtnSavePBR not yet implemented") }
    protected fun updatePBROverrideDisplay() {
        System.err.println("FSPanelFace: updatePBROverrideDisplay not yet implemented")
    }

    // PBR per-channel overloads (called by individual texture/color swatches)
    protected fun onCommitPbr(pbrCtrl: Any?) { System.err.println("FSPanelFace: onCommitPbr(pbrCtrl) not yet implemented") }
    protected fun onCancelPbr(pbrCtrl: Any?) { System.err.println("FSPanelFace: onCancelPbr(pbrCtrl) not yet implemented") }
    protected fun onSelectPbr(pbrCtrl: Any?) { System.err.println("FSPanelFace: onSelectPbr(pbrCtrl) not yet implemented") }

    protected fun getGltfMaterial(mat: Any?) {
        System.err.println("FSPanelFace: getGltfMaterial not yet implemented")
    }

    // -------------------------------------------------------------------------
    // Confirmation callbacks
    // -------------------------------------------------------------------------

    protected fun deleteMediaConfirm(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        return false
    }

    protected fun multipleFacesSelectedConfirm(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        return false
    }

    // -------------------------------------------------------------------------
    // UI state update
    // -------------------------------------------------------------------------

    protected fun updateUI(forceSetValues: Boolean = false) {
        System.err.println("FSPanelFace: updateUI not yet implemented")
    }

    protected fun isIdenticalPlanarTexgen(): Boolean {
        return false
    }

    // -------------------------------------------------------------------------
    // GLTF update helpers
    // -------------------------------------------------------------------------

    protected fun updateSelectedGltfMaterials(func: (Any) -> Unit) {
        System.err.println("FSPanelFace: updateSelectedGltfMaterials not yet implemented")
    }

    protected fun updateSelectedGltfMaterialsWithScale(func: (Any, Float, Float) -> Unit) {
        System.err.println("FSPanelFace: updateSelectedGltfMaterialsWithScale not yet implemented")
    }

    protected fun updateGltfTextureTransform(textureInfo: Int, edit: (Any) -> Unit) {
        System.err.println("FSPanelFace: updateGltfTextureTransform not yet implemented")
    }

    protected fun updateGltfTextureTransformWithScale(textureInfo: Int, edit: (Any, Float, Float) -> Unit) {
        System.err.println("FSPanelFace: updateGltfTextureTransformWithScale not yet implemented")
    }

    protected fun setMaterialOverridesFromSelection() {
        System.err.println("FSPanelFace: setMaterialOverridesFromSelection not yet implemented")
    }

    // -------------------------------------------------------------------------
    // Blinn-Phong sub-panel visibility updates
    // -------------------------------------------------------------------------

    private fun updateShinyControls(isSettingTexture: Boolean = false, messWithCombobox: Boolean = false) {
        System.err.println("FSPanelFace: updateShinyControls not yet implemented")
    }

    private fun updateBumpyControls(isSettingTexture: Boolean = false, messWithCombobox: Boolean = false) {
        System.err.println("FSPanelFace: updateBumpyControls not yet implemented")
    }

    private fun updateAlphaControls() {
        System.err.println("FSPanelFace: updateAlphaControls not yet implemented")
    }

    private fun updateUIGltf(
        objectp: Any?,
        hasPbrMaterial: Boolean,
        hasFacesWithoutPbr: Boolean,
        forceSetValues: Boolean
    ) {
        System.err.println("FSPanelFace: updateUIGltf not yet implemented")
    }

    private fun updateVisibility(objectp: Any? = null) {
        System.err.println("FSPanelFace: updateVisibility not yet implemented")
    }

    // -------------------------------------------------------------------------
    // UI Callbacks – flip, GLTF UV spinners, find-same-texture
    // -------------------------------------------------------------------------

    fun onCommitFlip(userData: Map<String, Any>) {
        System.err.println("FSPanelFace: onCommitFlip not yet implemented")
    }

    fun onCommitGltfUVSpinner(ctrl: Any?, userData: Map<String, Any>) {
        System.err.println("FSPanelFace: onCommitGltfUVSpinner not yet implemented")
    }

    protected fun onClickBtnSelectSameTexture(ctrl: Any?, userData: Map<String, Any>) {
        System.err.println("FSPanelFace: onClickBtnSelectSameTexture not yet implemented")
    }

    protected fun onShowFindAllButton(ctrl: Any?, userData: Map<String, Any>) {
        System.err.println("FSPanelFace: onShowFindAllButton not yet implemented")
    }

    protected fun onHideFindAllButton(ctrl: Any?, userData: Map<String, Any>) {
        System.err.println("FSPanelFace: onHideFindAllButton not yet implemented")
    }

    // -------------------------------------------------------------------------
    // Clipboard operations (accessible to selection manager)
    // -------------------------------------------------------------------------

    fun onCopyColor() { System.err.println("FSPanelFace: onCopyColor not yet implemented") }
    fun onPasteColor() { System.err.println("FSPanelFace: onPasteColor not yet implemented") }
    fun onPasteColor(objectp: Any?, te: Int) { System.err.println("FSPanelFace: onPasteColor(objectp, te) not yet implemented") }
    fun onCopyTexture() { System.err.println("FSPanelFace: onCopyTexture not yet implemented") }
    fun onPasteTexture() { System.err.println("FSPanelFace: onPasteTexture not yet implemented") }
    fun onPasteTexture(objectp: Any?, te: Int) { System.err.println("FSPanelFace: onPasteTexture(objectp, te) not yet implemented") }

    // -------------------------------------------------------------------------
    // Tab change callbacks
    // -------------------------------------------------------------------------

    fun onMatTabChange() {
        System.err.println("FSPanelFace: onMatTabChange not yet implemented")
    }

    fun onMatChannelTabChange() {
        System.err.println("FSPanelFace: onMatChannelTabChange not yet implemented")
    }

    fun onPBRChannelTabChange() {
        System.err.println("FSPanelFace: onPBRChannelTabChange not yet implemented")
    }

    // -------------------------------------------------------------------------
    // Convenience accessors for current UI values
    // -------------------------------------------------------------------------

    private fun getCurrentNormalMap(): UUID {
        return UUID(0L, 0L)
    }

    private fun getCurrentSpecularMap(): UUID {
        return UUID(0L, 0L)
    }

    private fun getCurrentShininess(): UInt {
        return 0u
    }

    private fun getCurrentBumpiness(): UInt {
        return 0u
    }

    private fun getCurrentDiffuseAlphaMode(): UByte {
        return 0u
    }

    private fun getCurrentAlphaMaskCutoff(): UByte {
        return 0u
    }

    private fun getCurrentEnvIntensity(): UByte {
        return 0u
    }

    private fun getCurrentGlossiness(): UByte {
        return 0u
    }

    private fun getCurrentBumpyRot(): Float { return 0f }
    private fun getCurrentBumpyScaleU(): Float { return 0f }
    private fun getCurrentBumpyScaleV(): Float { return 0f }
    private fun getCurrentBumpyOffsetU(): Float { return 0f }
    private fun getCurrentBumpyOffsetV(): Float { return 0f }
    private fun getCurrentShinyRot(): Float { return 0f }
    private fun getCurrentShinyScaleU(): Float { return 0f }
    private fun getCurrentShinyScaleV(): Float { return 0f }
    private fun getCurrentShinyOffsetU(): Float { return 0f }
    private fun getCurrentShinyOffsetV(): Float { return 0f }
    private fun getCurrentTextureRot(): Float { return 0f }
    private fun getCurrentTextureScaleU(): Float { return 0f }
    private fun getCurrentTextureScaleV(): Float { return 0f }
    private fun getCurrentTextureOffsetU(): Float { return 0f }
    private fun getCurrentTextureOffsetV(): Float { return 0f }

    private fun getCurrentMaterialType(): Int {
        return 0
    }

    private fun getCurrentMatChannel(): Int {
        return 0
    }

    private fun getCurrentPBRChannel(): Int {
        return 0
    }

    private fun getCurrentPBRType(pbrChannel: Int): Int {
        return 0
    }

    private fun selectMaterialType(materialType: Int) {
        System.err.println("FSPanelFace: selectMaterialType not yet implemented")
    }

    private fun selectMatChannel(matChannel: Int) {
        System.err.println("FSPanelFace: selectMatChannel not yet implemented")
    }

    private fun selectPBRChannel(pbrChannel: Int) {
        System.err.println("FSPanelFace: selectPBRChannel not yet implemented")
    }

    // -------------------------------------------------------------------------
    // Inner helper: LLSelectedTEMaterial – static accessors for material TE values
    // -------------------------------------------------------------------------

    object LLSelectedTEMaterial {
        fun getCurrent(materialPtr: Any?, identicalMaterial: Boolean) {
            System.err.println("FSPanelFace: LLSelectedTEMaterial.getCurrent not yet implemented")
        }

        fun getMaxSpecularRepeats(repeats: Float, identical: Boolean) {
            System.err.println("FSPanelFace: LLSelectedTEMaterial.getMaxSpecularRepeats not yet implemented")
        }

        fun getMaxNormalRepeats(repeats: Float, identical: Boolean) {
            System.err.println("FSPanelFace: LLSelectedTEMaterial.getMaxNormalRepeats not yet implemented")
        }

        fun getCurrentDiffuseAlphaMode(diffuseAlphaMode: UByte, identical: Boolean, diffuseTextureHasAlpha: Boolean) {
            System.err.println("FSPanelFace: LLSelectedTEMaterial.getCurrentDiffuseAlphaMode not yet implemented")
        }

        fun selectionNormalScaleAutofit(panelFace: FSPanelFace, repeatsPerMeter: Float) {
            System.err.println("FSPanelFace: LLSelectedTEMaterial.selectionNormalScaleAutofit not yet implemented")
        }

        fun selectionSpecularScaleAutofit(panelFace: FSPanelFace, repeatsPerMeter: Float) {
            System.err.println("FSPanelFace: LLSelectedTEMaterial.selectionSpecularScaleAutofit not yet implemented")
        }
    }

    // -------------------------------------------------------------------------
    // Inner helper: LLSelectedTE – static accessors for TE state (legacy)
    // -------------------------------------------------------------------------

    object LLSelectedTE {
        fun getFace(faceToReturn: Any?, identicalFace: Boolean) {
            System.err.println("FSPanelFace: LLSelectedTE.getFace not yet implemented")
        }

        fun getImageFormat(imageFormatToReturn: Int, identicalFace: Boolean, missingAsset: Boolean) {
            System.err.println("FSPanelFace: LLSelectedTE.getImageFormat not yet implemented")
        }

        fun getTexId(id: UUID, identical: Boolean) {
            System.err.println("FSPanelFace: LLSelectedTE.getTexId not yet implemented")
        }

        fun getObjectScaleS(scaleS: Float, identical: Boolean) {
            System.err.println("FSPanelFace: LLSelectedTE.getObjectScaleS not yet implemented")
        }

        fun getObjectScaleT(scaleT: Float, identical: Boolean) {
            System.err.println("FSPanelFace: LLSelectedTE.getObjectScaleT not yet implemented")
        }

        fun getMaxDiffuseRepeats(repeats: Float, identical: Boolean) {
            System.err.println("FSPanelFace: LLSelectedTE.getMaxDiffuseRepeats not yet implemented")
        }
    }
}
