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
            TODO("APR: use JVM equivalent for LLGLTFMaterialList selection-update callback")
        }

        fun syncRepeatX(self: FSPanelFace, scaleU: Float) {
            TODO("GPU: sync diffuse U scale to other material channels when sync is enabled")
        }

        fun syncRepeatY(self: FSPanelFace, scaleV: Float) {
            TODO("GPU: sync diffuse V scale to other material channels when sync is enabled")
        }

        fun syncOffsetX(self: FSPanelFace, offsetU: Float) {
            TODO("GPU: sync diffuse U offset to other material channels")
        }

        fun syncOffsetY(self: FSPanelFace, offsetV: Float) {
            TODO("GPU: sync diffuse V offset to other material channels")
        }

        fun syncMaterialRot(self: FSPanelFace, rot: Float, te: Int = -1) {
            TODO("GPU: sync material rotation to other channels")
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
            TODO("APR: use JVM equivalent for LLSelectMgr selection-change signal connection")
        }

        fun update(): Boolean {
            TODO("APR: use JVM equivalent: return true when selected objects/sides changed " +
                "and no pending object update")
        }

        fun setDirty() { changed = true }

        fun onSelectionChanged() { needsSelectionCheck = true }

        fun onSelectedObjectUpdated(objectId: UUID, side: Int) {
            TODO("APR: use JVM equivalent for selection object-updated callback")
        }

        private fun compareSelection(): Boolean {
            TODO("APR: use JVM equivalent for comparing current vs stored TE selection")
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    init {
        TODO("GPU: register UI callbacks (BuildTool.Flip, GLTFUVSpinner, SelectSameTexture, " +
            "ShowFindAllButton, HideFindAllButton) and buildFromFile('panel_fs_tools_texture.xml')")
    }

    fun postBuild(): Boolean {
        TODO("GPU: resolve all UI control references from skin XML, wire commit callbacks, " +
            "register GLTF material list selection-update callback, apply decimal precision setting")
    }

    fun refresh() {
        TODO("GPU: read current selection state and push values to all UI controls")
    }

    fun refreshMedia() {
        TODO("GPU: refresh media settings panel controls")
    }

    fun unloadMedia() {
        TODO("GPU: unload any embedded media controller resources")
    }

    fun changePrecision(decimalPrecision: Int) {
        TODO("GPU: set decimal precision on all relevant spin controls")
    }

    open fun onVisibilityChange(newVisibility: Boolean) {
        TODO("GPU: update visibility of UI sub-panels based on current material type selection")
    }

    open fun draw() {
        TODO("GPU: draw panel; delegate to parent LLPanel draw")
    }

    fun createDefaultMaterial(currentMaterial: Any?): Any? {
        TODO("GPU: create a new LLMaterialPtr, copying from currentMaterial if non-null")
    }

    fun getTextureChannelToEdit(): Int {
        TODO("GPU: return tex-index for the currently active material/channel tab")
    }

    fun getTextureDropChannel(): Int {
        TODO("GPU: return drop-target tex-index for the current Blinn-Phong channel")
    }

    fun getPbrDropChannel(): Int {
        TODO("GPU: return GLTF TextureInfo enum for the current PBR channel tab")
    }

    // -------------------------------------------------------------------------
    // Media helpers
    // -------------------------------------------------------------------------

    protected fun navigateToTitleMedia(url: String) {
        TODO("GPU: navigate embedded LLMediaCtrl to $url")
    }

    protected fun selectedMediaEditable(): Boolean {
        TODO("GPU: check if current selection allows media editing")
    }

    protected fun clearMediaSettings() {
        mediaSettings = emptyMap()
        needMediaTitle = true
    }

    protected fun updateMediaSettings() {
        TODO("APR: use JVM equivalent for reading media settings from the selection")
    }

    protected fun updateMediaTitle() {
        TODO("GPU: fetch page title from embedded media and update UI label")
    }

    protected fun isMediaTexSelected(): Boolean {
        TODO("GPU: return true when selected texture channel is a media texture")
    }

    // -------------------------------------------------------------------------
    // State reader
    // -------------------------------------------------------------------------

    protected fun getState() {
        TODO("GPU: read full material state from selection and populate all UI fields")
    }

    // -------------------------------------------------------------------------
    // Send-to-server helpers
    // -------------------------------------------------------------------------

    protected fun sendTexture() { TODO("GPU: apply and send texture to simulator") }
    protected fun sendTextureInfo() { TODO("GPU: apply and send texture transforms to simulator") }
    protected fun sendColor() { TODO("GPU: apply and send diffuse color to simulator") }
    protected fun sendAlpha() { TODO("GPU: apply and send transparency to simulator") }
    protected fun sendBump(bumpiness: UInt) { TODO("GPU: apply and send bump map index $bumpiness") }
    protected fun sendTexGen() { TODO("GPU: apply and send texgen mode to simulator") }
    protected fun sendShiny(shininess: UInt) { TODO("GPU: apply and send shininess index $shininess") }
    protected fun sendFullbright() { TODO("GPU: apply and send fullbright flag to simulator") }
    protected fun sendGlow() { TODO("GPU: apply and send glow value to simulator") }
    protected fun alignTextureLayer() { TODO("GPU: align texture UVs across selection") }
    protected fun updateCopyTexButton() { TODO("GPU: enable/disable copy-texture button based on selection perms") }

    // -------------------------------------------------------------------------
    // UI Callbacks – common
    // -------------------------------------------------------------------------

    protected fun onCopyFaces() { TODO("GPU: copy all face parameters from selection to clipboard") }
    protected fun onPasteFaces() { TODO("GPU: paste clipboard face parameters to selection") }
    protected fun onCommitHideWater() { TODO("GPU: toggle hide-water flag on selection") }
    protected fun onCommitGlow() { sendGlow() }
    protected fun onCommitRepeatsPerMeter() { TODO("GPU: convert repeats-per-meter to UV scale and apply") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Blinn-Phong alpha
    // -------------------------------------------------------------------------

    protected fun onCommitAlpha() { sendAlpha() }
    protected fun onCommitAlphaMode() { TODO("GPU: commit diffuse alpha mode to material") }
    protected fun onCommitMaterialMaskCutoff() { TODO("GPU: commit alpha mask cutoff to material") }
    protected fun onCommitFullbright() { sendFullbright() }

    // -------------------------------------------------------------------------
    // UI Callbacks – Blinn-Phong texture transforms
    // -------------------------------------------------------------------------

    protected fun onCommitTexGen() { sendTexGen() }
    protected fun onCommitPlanarAlign() { TODO("GPU: align planar textures across selection") }
    protected fun onCommitBump() { TODO("GPU: commit bump index; load normal map if BUMPY_TEXTURE selected") }
    protected fun onCommitShiny() { TODO("GPU: commit shininess index; load specular map if SHINY_TEXTURE selected") }
    protected fun onCommitTextureScaleX() { TODO("GPU: commit diffuse U scale; sync if sync-materials enabled") }
    protected fun onCommitTextureScaleY() { TODO("GPU: commit diffuse V scale; sync if sync-materials enabled") }
    protected fun onCommitTextureOffsetX() { TODO("GPU: commit diffuse U offset") }
    protected fun onCommitTextureOffsetY() { TODO("GPU: commit diffuse V offset") }
    protected fun onCommitTextureRot() { TODO("GPU: commit diffuse rotation") }
    protected fun onCommitMaterialBumpyScaleX() { TODO("GPU: commit normal U scale") }
    protected fun onCommitMaterialBumpyScaleY() { TODO("GPU: commit normal V scale") }
    protected fun onCommitMaterialBumpyOffsetX() { TODO("GPU: commit normal U offset") }
    protected fun onCommitMaterialBumpyOffsetY() { TODO("GPU: commit normal V offset") }
    protected fun onCommitMaterialBumpyRot() { TODO("GPU: commit normal rotation") }
    protected fun onCommitMaterialShinyScaleX() { TODO("GPU: commit specular U scale") }
    protected fun onCommitMaterialShinyScaleY() { TODO("GPU: commit specular V scale") }
    protected fun onCommitMaterialShinyOffsetX() { TODO("GPU: commit specular U offset") }
    protected fun onCommitMaterialShinyOffsetY() { TODO("GPU: commit specular V offset") }
    protected fun onCommitMaterialShinyRot() { TODO("GPU: commit specular rotation") }
    protected fun onCommitMaterialGloss() { TODO("GPU: commit glossiness value to material") }
    protected fun onCommitMaterialEnv() { TODO("GPU: commit environment intensity to material") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Diffuse color swatch
    // -------------------------------------------------------------------------

    protected fun onCommitColor() { sendColor() }
    protected fun onCancelColor() { TODO("GPU: revert color swatch to last committed value") }
    protected fun onSelectColor() { TODO("GPU: preview color selection immediately") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Diffuse texture swatch
    // -------------------------------------------------------------------------

    protected fun onSelectTexture() { TODO("GPU: handle texture picker selection") }
    protected fun onCommitTexture() { sendTexture() }
    protected fun onCancelTexture() { TODO("GPU: revert texture picker to last committed texture") }
    protected fun onDragTexture(textureCtrl: Any?, item: Any?): Boolean {
        TODO("GPU: validate drag-and-drop texture permission; return true to allow drop")
    }
    protected fun onCloseTexturePicker() { TODO("GPU: handle texture picker close; restore overlay state") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Normal/Specular texture swatches
    // -------------------------------------------------------------------------

    protected fun onCommitNormalTexture() { TODO("GPU: commit normal map to material") }
    protected fun onCancelNormalTexture() { TODO("GPU: revert normal map picker") }
    protected fun onCommitSpecularTexture() { TODO("GPU: commit specular map to material") }
    protected fun onCancelSpecularTexture() { TODO("GPU: revert specular map picker") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Specular color swatch
    // -------------------------------------------------------------------------

    protected fun onCommitShinyColor() { TODO("GPU: commit specular tint color to material") }
    protected fun onCancelShinyColor() { TODO("GPU: revert specular tint color picker") }
    protected fun onSelectShinyColor() { TODO("GPU: preview specular tint color immediately") }

    // -------------------------------------------------------------------------
    // UI Callbacks – Alignment & sync
    // -------------------------------------------------------------------------

    protected fun onClickAutoFix() { TODO("GPU: auto-fix media alignment") }
    protected fun onAlignTexture() { alignTextureLayer() }
    protected fun onClickMapsSync() { TODO("GPU: toggle sync between Blinn-Phong material maps") }
    protected fun onTextureSelectionChanged(textureCtrl: Any?) {
        TODO("GPU: disable apply controls in texture picker when texture not permitted on selection")
    }

    // -------------------------------------------------------------------------
    // UI Callbacks – Media
    // -------------------------------------------------------------------------

    protected fun onClickBtnEditMedia() { TODO("GPU: open LLFloaterMediaSettings for selection") }
    protected fun onClickBtnDeleteMedia() { TODO("GPU: confirm then delete media from selection") }
    protected fun onClickBtnAddMedia() { TODO("GPU: confirm then add media to selection") }

    protected fun alignMaterialsProperties() {
        TODO("GPU: align normal/specular map UV params to match diffuse map")
    }

    // -------------------------------------------------------------------------
    // UI Callbacks – PBR material
    // -------------------------------------------------------------------------

    protected fun onCommitPbr() { TODO("GPU: commit PBR base material from material picker") }
    protected fun onCancelPbr() { TODO("GPU: revert PBR material picker") }
    protected fun onSelectPbr() { TODO("GPU: handle PBR material picker selection") }
    protected fun onDragPbr(item: Any?): Boolean {
        TODO("GPU: validate drag-and-drop PBR material permission; return true to allow drop")
    }
    protected fun onPbrSelectionChanged(item: Any?) {
        TODO("GPU: update PBR override display when material asset selection changes")
    }
    protected fun onClickBtnSavePBR() { TODO("GPU: save PBR material overrides as new inventory asset") }
    protected fun updatePBROverrideDisplay() {
        TODO("GPU: show/hide PBR override controls based on whether a base material is assigned")
    }

    // PBR per-channel overloads (called by individual texture/color swatches)
    protected fun onCommitPbr(pbrCtrl: Any?) { TODO("GPU: commit single PBR channel change from ctrl=$pbrCtrl") }
    protected fun onCancelPbr(pbrCtrl: Any?) { TODO("GPU: revert single PBR channel from ctrl=$pbrCtrl") }
    protected fun onSelectPbr(pbrCtrl: Any?) { TODO("GPU: select single PBR channel from ctrl=$pbrCtrl") }

    protected fun getGltfMaterial(mat: Any?) {
        TODO("GPU: populate mat with the current GLTF override values from the UI controls")
    }

    // -------------------------------------------------------------------------
    // Confirmation callbacks
    // -------------------------------------------------------------------------

    protected fun deleteMediaConfirm(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        TODO("APR: use JVM equivalent for notification response handling")
    }

    protected fun multipleFacesSelectedConfirm(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
        TODO("APR: use JVM equivalent for notification response handling")
    }

    // -------------------------------------------------------------------------
    // UI state update
    // -------------------------------------------------------------------------

    protected fun updateUI(forceSetValues: Boolean = false) {
        TODO("GPU: read selection material state and push values to all controls; " +
            "forceSetValues=$forceSetValues bypasses spinner focus guard")
    }

    protected fun isIdenticalPlanarTexgen(): Boolean {
        TODO("GPU: query selection for identical planar texgen across all faces")
    }

    // -------------------------------------------------------------------------
    // GLTF update helpers
    // -------------------------------------------------------------------------

    protected fun updateSelectedGltfMaterials(func: (Any) -> Unit) {
        TODO("GPU: iterate selected TEs, apply func to GLTF material override, queue modify")
    }

    protected fun updateSelectedGltfMaterialsWithScale(func: (Any, Float, Float) -> Unit) {
        TODO("GPU: iterate selected TEs with per-face s/t axis scale, apply func, queue modify")
    }

    protected fun updateGltfTextureTransform(textureInfo: Int, edit: (Any) -> Unit) {
        TODO("GPU: apply edit lambda to the TextureTransform for textureInfo on all selected TEs")
    }

    protected fun updateGltfTextureTransformWithScale(textureInfo: Int, edit: (Any, Float, Float) -> Unit) {
        TODO("GPU: apply edit lambda with object scale to TextureTransform on all selected TEs")
    }

    protected fun setMaterialOverridesFromSelection() {
        TODO("GPU: read GLTF override values from selection into the local PBR param cache")
    }

    // -------------------------------------------------------------------------
    // Blinn-Phong sub-panel visibility updates
    // -------------------------------------------------------------------------

    private fun updateShinyControls(isSettingTexture: Boolean = false, messWithCombobox: Boolean = false) {
        TODO("GPU: show/hide specular spinner group depending on shininess combo selection")
    }

    private fun updateBumpyControls(isSettingTexture: Boolean = false, messWithCombobox: Boolean = false) {
        TODO("GPU: show/hide normal map spinner group depending on bumpiness combo selection")
    }

    private fun updateAlphaControls() {
        TODO("GPU: show/hide alpha-mode and mask-cutoff controls depending on diffuse alpha mode")
    }

    private fun updateUIGltf(
        objectp: Any?,
        hasPbrMaterial: Boolean,
        hasFacesWithoutPbr: Boolean,
        forceSetValues: Boolean
    ) {
        TODO("GPU: update PBR channel controls from GLTF material state on objectp")
    }

    private fun updateVisibility(objectp: Any? = null) {
        TODO("GPU: show/hide Blinn-Phong vs PBR vs Media sub-panels based on tab selection")
    }

    // -------------------------------------------------------------------------
    // UI Callbacks – flip, GLTF UV spinners, find-same-texture
    // -------------------------------------------------------------------------

    fun onCommitFlip(userData: Map<String, Any>) {
        TODO("GPU: flip diffuse/normal/specular UV axis indicated by userData")
    }

    fun onCommitGltfUVSpinner(ctrl: Any?, userData: Map<String, Any>) {
        TODO("GPU: commit a GLTF UV transform value from ctrl to the appropriate TextureTransform channel")
    }

    protected fun onClickBtnSelectSameTexture(ctrl: Any?, userData: Map<String, Any>) {
        TODO("GPU: select all faces in scene that share the same texture as the current face selection")
    }

    protected fun onShowFindAllButton(ctrl: Any?, userData: Map<String, Any>) {
        TODO("GPU: make the 'find all faces' button visible")
    }

    protected fun onHideFindAllButton(ctrl: Any?, userData: Map<String, Any>) {
        TODO("GPU: hide the 'find all faces' button")
    }

    // -------------------------------------------------------------------------
    // Clipboard operations (accessible to selection manager)
    // -------------------------------------------------------------------------

    fun onCopyColor() { TODO("GPU: copy color parameters from all selected faces to clipboard") }
    fun onPasteColor() { TODO("GPU: paste clipboard color to selection") }
    fun onPasteColor(objectp: Any?, te: Int) { TODO("GPU: paste clipboard color to specific TE $te of objectp") }
    fun onCopyTexture() { TODO("GPU: copy texture parameters from selection to clipboard") }
    fun onPasteTexture() { TODO("GPU: paste clipboard texture to selection") }
    fun onPasteTexture(objectp: Any?, te: Int) { TODO("GPU: paste clipboard texture to specific TE $te of objectp") }

    // -------------------------------------------------------------------------
    // Tab change callbacks
    // -------------------------------------------------------------------------

    fun onMatTabChange() {
        TODO("GPU: switch BP/PBR/Media mode; hide/show GLTF material on objects; call updateUI")
    }

    fun onMatChannelTabChange() {
        TODO("GPU: switch Blinn-Phong channel (diffuse/normal/specular); call updateUI if not programmatic")
    }

    fun onPBRChannelTabChange() {
        TODO("GPU: switch PBR channel tab; call updateUI if not programmatic")
    }

    // -------------------------------------------------------------------------
    // Convenience accessors for current UI values
    // -------------------------------------------------------------------------

    private fun getCurrentNormalMap(): UUID {
        TODO("GPU: return bumpyTextureCtrl.imageAssetId")
    }

    private fun getCurrentSpecularMap(): UUID {
        TODO("GPU: return shinyTextureCtrl.imageAssetId")
    }

    private fun getCurrentShininess(): UInt {
        TODO("GPU: return comboShininess.currentIndex as UInt")
    }

    private fun getCurrentBumpiness(): UInt {
        TODO("GPU: return comboBumpiness.currentIndex as UInt")
    }

    private fun getCurrentDiffuseAlphaMode(): UByte {
        TODO("GPU: return comboAlphaMode.currentIndex as UByte")
    }

    private fun getCurrentAlphaMaskCutoff(): UByte {
        TODO("GPU: return ctrlMaskCutoff.value as UByte")
    }

    private fun getCurrentEnvIntensity(): UByte {
        TODO("GPU: return ctrlEnvironment.value as UByte")
    }

    private fun getCurrentGlossiness(): UByte {
        TODO("GPU: return ctrlGlossiness.value as UByte")
    }

    private fun getCurrentBumpyRot(): Float { TODO("GPU: return ctrlBumpyRot.value as Float") }
    private fun getCurrentBumpyScaleU(): Float { TODO("GPU: return ctrlBumpyScaleU.value as Float") }
    private fun getCurrentBumpyScaleV(): Float { TODO("GPU: return ctrlBumpyScaleV.value as Float") }
    private fun getCurrentBumpyOffsetU(): Float { TODO("GPU: return ctrlBumpyOffsetU.value as Float") }
    private fun getCurrentBumpyOffsetV(): Float { TODO("GPU: return ctrlBumpyOffsetV.value as Float") }
    private fun getCurrentShinyRot(): Float { TODO("GPU: return ctrlShinyRot.value as Float") }
    private fun getCurrentShinyScaleU(): Float { TODO("GPU: return ctrlShinyScaleU.value as Float") }
    private fun getCurrentShinyScaleV(): Float { TODO("GPU: return ctrlShinyScaleV.value as Float") }
    private fun getCurrentShinyOffsetU(): Float { TODO("GPU: return ctrlShinyOffsetU.value as Float") }
    private fun getCurrentShinyOffsetV(): Float { TODO("GPU: return ctrlShinyOffsetV.value as Float") }
    private fun getCurrentTextureRot(): Float { TODO("GPU: return ctrlTexRot.value as Float") }
    private fun getCurrentTextureScaleU(): Float { TODO("GPU: return ctrlTexScaleU.value as Float") }
    private fun getCurrentTextureScaleV(): Float { TODO("GPU: return ctrlTexScaleV.value as Float") }
    private fun getCurrentTextureOffsetU(): Float { TODO("GPU: return ctrlTexOffsetU.value as Float") }
    private fun getCurrentTextureOffsetV(): Float { TODO("GPU: return ctrlTexOffsetV.value as Float") }

    private fun getCurrentMaterialType(): Int {
        TODO("GPU: return selected tab index from tabsPBRMatMedia")
    }

    private fun getCurrentMatChannel(): Int {
        TODO("GPU: return selected tab index from tabsMatChannel as LLRender.eTexIndex")
    }

    private fun getCurrentPBRChannel(): Int {
        TODO("GPU: return selected tab index from tabsPBRChannel as LLRender.eTexIndex")
    }

    private fun getCurrentPBRType(pbrChannel: Int): Int {
        TODO("GPU: map pbrChannel tab index to LLGLTFMaterial.TextureInfo enum value")
    }

    private fun selectMaterialType(materialType: Int) {
        TODO("GPU: programmatically select material-type tab $materialType and call onMatTabChange")
    }

    private fun selectMatChannel(matChannel: Int) {
        TODO("GPU: programmatically select Blinn-Phong channel tab $matChannel")
    }

    private fun selectPBRChannel(pbrChannel: Int) {
        TODO("GPU: programmatically select PBR channel tab $pbrChannel")
    }

    // -------------------------------------------------------------------------
    // Inner helper: LLSelectedTEMaterial – static accessors for material TE values
    // -------------------------------------------------------------------------

    object LLSelectedTEMaterial {
        fun getCurrent(materialPtr: Any?, identicalMaterial: Boolean) {
            TODO("GPU: get current LLMaterialPtr from selection; set identicalMaterial output")
        }

        fun getMaxSpecularRepeats(repeats: Float, identical: Boolean) {
            TODO("GPU: get max specular repeat scale from selection")
        }

        fun getMaxNormalRepeats(repeats: Float, identical: Boolean) {
            TODO("GPU: get max normal repeat scale from selection")
        }

        fun getCurrentDiffuseAlphaMode(diffuseAlphaMode: UByte, identical: Boolean, diffuseTextureHasAlpha: Boolean) {
            TODO("GPU: get diffuse alpha mode from selection; account for alpha presence")
        }

        fun selectionNormalScaleAutofit(panelFace: FSPanelFace, repeatsPerMeter: Float) {
            TODO("GPU: auto-fit normal map scale based on object dimensions and repeats-per-meter")
        }

        fun selectionSpecularScaleAutofit(panelFace: FSPanelFace, repeatsPerMeter: Float) {
            TODO("GPU: auto-fit specular map scale based on object dimensions and repeats-per-meter")
        }
    }

    // -------------------------------------------------------------------------
    // Inner helper: LLSelectedTE – static accessors for TE state (legacy)
    // -------------------------------------------------------------------------

    object LLSelectedTE {
        fun getFace(faceToReturn: Any?, identicalFace: Boolean) {
            TODO("GPU: get LLFace from first selected TE")
        }

        fun getImageFormat(imageFormatToReturn: Int, identicalFace: Boolean, missingAsset: Boolean) {
            TODO("GPU: get GL image format from selected TE")
        }

        fun getTexId(id: UUID, identical: Boolean) {
            TODO("GPU: get diffuse texture UUID from selected TE")
        }

        fun getObjectScaleS(scaleS: Float, identical: Boolean) {
            TODO("GPU: get object S-axis scale from selected TE")
        }

        fun getObjectScaleT(scaleT: Float, identical: Boolean) {
            TODO("GPU: get object T-axis scale from selected TE")
        }

        fun getMaxDiffuseRepeats(repeats: Float, identical: Boolean) {
            TODO("GPU: get maximum diffuse repeat value across all selected TEs")
        }
    }
}
