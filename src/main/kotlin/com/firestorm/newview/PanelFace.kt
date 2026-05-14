package com.firestorm.newview

import com.firestorm.ui.Panel
import com.firestorm.ui.Button
import com.firestorm.ui.CheckBoxCtrl
import com.firestorm.ui.ColorSwatchCtrl
import com.firestorm.ui.ComboBox
import com.firestorm.ui.SpinCtrl
import com.firestorm.ui.TextBox
import com.firestorm.ui.TextureCtrl
import com.firestorm.ui.RadioGroup
import com.firestorm.ui.MediaCtrl
import com.firestorm.ui.UICtrl
import com.firestorm.ui.LLSD
import com.firestorm.material.Material
import com.firestorm.material.MaterialPtr
import com.firestorm.material.MaterialMgr
import com.firestorm.material.GLTFMaterial
import com.firestorm.material.TextureEntry
import com.firestorm.object.ViewerObject
import com.firestorm.inventory.InventoryItem
import com.firestorm.agent.Agent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import java.net.HttpURLConnection
import java.net.URL
import java.io.DataOutputStream

typealias Uuid = java.util.UUID
typealias Color4 = FloatArray   // [r, g, b, a]
typealias Color4U = IntArray    // [r, g, b, a] as 0-255

const val MATMEDIA_MATERIAL = 0
const val MATMEDIA_PBR      = 1
const val MATMEDIA_MEDIA    = 2
const val MATTYPE_DIFFUSE   = 0
const val MATTYPE_NORMAL    = 1
const val MATTYPE_SPECULAR  = 2
const val ALPHAMODE_MASK    = 2
const val BUMPY_TEXTURE     = 18
const val SHINY_TEXTURE     = 4
const val PBRTYPE_RENDER_MATERIAL_ID = 0
const val PBRTYPE_BASE_COLOR         = 1
const val PBRTYPE_METALLIC_ROUGHNESS = 2
const val PBRTYPE_EMISSIVE           = 3
const val PBRTYPE_NORMAL_MAP         = 4

class PanelFace : Panel() {

    private var pbrTextureCtrl: TextureCtrl? = null
    private var textureCtrl: TextureCtrl? = null
    private var shinyTextureCtrl: TextureCtrl? = null
    private var bumpyTextureCtrl: TextureCtrl? = null
    private var labelColor: TextBox? = null
    private var colorSwatch: ColorSwatchCtrl? = null
    private var labelShiniColor: TextBox? = null
    private var shinyColorSwatch: ColorSwatchCtrl? = null
    private var labelTexGen: TextBox? = null
    private var comboTexGen: ComboBox? = null
    private var radioMaterialType: RadioGroup? = null
    private var radioPbrType: RadioGroup? = null
    private var checkFullbright: CheckBoxCtrl? = null
    private var checkHideWater: CheckBoxCtrl? = null
    private var labelColorTransp: TextBox? = null
    private var ctrlColorTransp: SpinCtrl? = null
    private var labelGlow: TextBox? = null
    private var ctrlGlow: SpinCtrl? = null
    private var comboMatMedia: ComboBox? = null
    private var titleMedia: MediaCtrl? = null
    private var titleMediaText: TextBox? = null
    private var labelMatPermLoading: TextBox? = null
    private var checkSyncSettings: CheckBoxCtrl? = null
    private var labelBumpiness: TextBox? = null
    private var comboBumpiness: ComboBox? = null
    private var labelShininess: TextBox? = null
    private var comboShininess: ComboBox? = null
    private var labelAlphaMode: TextBox? = null
    private var comboAlphaMode: ComboBox? = null
    private var texScaleU: SpinCtrl? = null
    private var texScaleV: SpinCtrl? = null
    private var texRotate: SpinCtrl? = null
    private var texRepeat: SpinCtrl? = null
    private var texOffsetU: SpinCtrl? = null
    private var texOffsetV: SpinCtrl? = null
    private var planarAlign: CheckBoxCtrl? = null
    private var bumpyScaleU: SpinCtrl? = null
    private var bumpyScaleV: SpinCtrl? = null
    private var bumpyRotate: SpinCtrl? = null
    private var bumpyOffsetU: SpinCtrl? = null
    private var bumpyOffsetV: SpinCtrl? = null
    private var shinyScaleU: SpinCtrl? = null
    private var shinyScaleV: SpinCtrl? = null
    private var shinyRotate: SpinCtrl? = null
    private var shinyOffsetU: SpinCtrl? = null
    private var shinyOffsetV: SpinCtrl? = null
    private var labelGlossiness: TextBox? = null
    private var glossiness: SpinCtrl? = null
    private var labelEnvironment: TextBox? = null
    private var environment: SpinCtrl? = null
    private var labelMaskCutoff: TextBox? = null
    private var maskCutoff: SpinCtrl? = null
    private var addMedia: Button? = null
    private var delMedia: Button? = null
    private var pbrScaleU: SpinCtrl? = null
    private var pbrScaleV: SpinCtrl? = null
    private var pbrRepeat: SpinCtrl? = null
    private var pbrRotate: SpinCtrl? = null
    private var pbrOffsetU: SpinCtrl? = null
    private var pbrOffsetV: SpinCtrl? = null
    private var btnAlign: Button? = null
    private var btnAlignTex: Button? = null
    private var btnEditBbr: Button? = null
    private var btnSaveBbr: Button? = null
    private var btnTexFlipScaleU: Button? = null
    private var btnTexFlipScaleV: Button? = null
    private var btnSelectSameDiff: Button? = null
    private var btnSelectSameSpec: Button? = null
    private var btnSelectSameNorm: Button? = null
    private var btnCopyFaces: Button? = null
    private var btnPasteFaces: Button? = null

    private var isAlpha: Boolean = false
    private var excludeWater: Boolean = false
    private var clipboardParams: LLSD = LLSD()
    private var mediaSettings: LLSD = LLSD()
    private var needMediaTitle: Boolean = false

    companion object {
        val materialOverrideSelection = Selection()

        @JvmStatic fun onMaterialOverrideReceived(objectId: Uuid, side: Int) {
            materialOverrideSelection.onSelectedObjectUpdated(objectId, side)
        }

        @JvmStatic fun valueGlow(obj: ViewerObject?, face: Int): Float {
            TODO("APR: use JVM equivalent - read glow value from object TE")
        }

        @JvmStatic fun deleteMediaConfirm(notification: LLSD, response: LLSD): Boolean {
            TODO("APR: use JVM equivalent - confirmation dialog callback")
        }

        @JvmStatic fun multipleFacesSelectedConfirm(notification: LLSD, response: LLSD): Boolean {
            TODO("APR: use JVM equivalent - multiple-faces confirmation dialog callback")
        }
    }

    override fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent - bind all child controls and set commit callbacks")
    }

    fun refresh() {
        TODO("APR: use JVM equivalent - refresh UI from current selection state")
    }

    fun refreshMedia() {
        TODO("APR: use JVM equivalent - refresh media controls")
    }

    fun unloadMedia() {
        TODO("APR: use JVM equivalent - unload media from controls")
    }

    fun changePrecision(decimalPrecision: Int) {
        TODO("APR: use JVM equivalent - change spinner decimal precision")
    }

    override fun onVisibilityChange(newVisibility: Boolean) {
        TODO("APR: use JVM equivalent - handle panel show/hide")
    }

    override fun draw() {
        TODO("GPU: draw panel face UI")
    }

    fun createDefaultMaterial(currentMaterial: MaterialPtr?): MaterialPtr {
        TODO("APR: use JVM equivalent - create default material preserving alpha mode")
    }

    fun getPBRTextureInfo(): GLTFMaterial.TextureInfo {
        val idx = radioPbrType?.getSelectedIndex() ?: return GLTFMaterial.TextureInfo.COUNT
        return when (idx) {
            PBRTYPE_BASE_COLOR         -> GLTFMaterial.TextureInfo.BASE_COLOR
            PBRTYPE_NORMAL_MAP         -> GLTFMaterial.TextureInfo.NORMAL
            PBRTYPE_METALLIC_ROUGHNESS -> GLTFMaterial.TextureInfo.METALLIC_ROUGHNESS
            PBRTYPE_EMISSIVE           -> GLTFMaterial.TextureInfo.EMISSIVE
            else                       -> GLTFMaterial.TextureInfo.COUNT
        }
    }

    fun getTextureChannelToEdit(): Int {
        TODO("APR: use JVM equivalent - return active texture channel for editing")
    }

    fun getMatTextureChannel(): Int {
        TODO("APR: use JVM equivalent - return material texture channel")
    }

    fun getPBRTextureChannel(): Int {
        TODO("APR: use JVM equivalent - return PBR texture channel")
    }

    fun getTextureDropChannel(): Int {
        TODO("APR: use JVM equivalent - return drop target texture channel")
    }

    fun getPBRDropChannel(): GLTFMaterial.TextureInfo {
        TODO("APR: use JVM equivalent - return drop target PBR channel")
    }

    private fun getState() {
        TODO("APR: use JVM equivalent - read selection state into UI controls")
    }

    private fun sendTexture() {
        TODO("APR: use JVM equivalent - apply and send texture to selected faces")
    }

    private fun sendTextureInfo() {
        TODO("APR: use JVM equivalent - apply and send texture scale/offset/rotation")
    }

    private fun sendColor() {
        TODO("APR: use JVM equivalent - apply and send face color")
    }

    private fun sendAlpha() {
        TODO("APR: use JVM equivalent - apply and send face alpha/transparency")
    }

    private fun sendBump(bumpiness: UInt) {
        TODO("APR: use JVM equivalent - apply and send bump map")
    }

    private fun sendTexGen() {
        TODO("APR: use JVM equivalent - apply and send tex-gen mode")
    }

    private fun sendShiny(shininess: UInt) {
        TODO("APR: use JVM equivalent - apply and send shininess")
    }

    private fun sendFullbright() {
        TODO("APR: use JVM equivalent - apply and send fullbright flag")
    }

    private fun sendGlow() {
        TODO("APR: use JVM equivalent - apply and send glow value")
    }

    private fun alignTextureLayer() {
        TODO("APR: use JVM equivalent - align texture to face dimensions")
    }

    private fun updateCopyTexButton() {
        TODO("APR: use JVM equivalent - update enabled state of copy texture button")
    }

    private fun navigateToTitleMedia(url: String) {
        TODO("APR: use JVM equivalent - navigate media to URL")
    }

    private fun selectedMediaEditable(): Boolean {
        TODO("APR: use JVM equivalent - check if selected media is editable")
    }

    private fun clearMediaSettings() {
        TODO("APR: use JVM equivalent - clear cached media settings")
    }

    private fun updateMediaSettings() {
        TODO("APR: use JVM equivalent - refresh media settings from selection")
    }

    private fun updateMediaTitle() {
        TODO("APR: use JVM equivalent - refresh media title label")
    }

    private fun isMediaTexSelected(): Boolean {
        TODO("APR: use JVM equivalent - check if selected texture is media texture")
    }

    private fun updateUI(forceSetValues: Boolean = false) {
        TODO("APR: use JVM equivalent - rebuild all controls from material/TE state")
    }

    private fun updateVisibility(objectp: ViewerObject? = null) {
        TODO("APR: use JVM equivalent - show/hide control subsets based on UI mode")
    }

    private fun isIdenticalPlanarTexgen(): Boolean {
        TODO("APR: use JVM equivalent - check all selected faces for identical planar texgen")
    }

    private fun updateShinyControls(isSettingTexture: Boolean = false, messWithCombobox: Boolean = false) {
        TODO("APR: use JVM equivalent - update shininess control visibility/enabled")
    }

    private fun updateBumpyControls(isSettingTexture: Boolean = false, messWithCombobox: Boolean = false) {
        TODO("APR: use JVM equivalent - update bump control visibility/enabled")
    }

    private fun updateAlphaControls() {
        TODO("APR: use JVM equivalent - update alpha-mode control visibility/enabled")
    }

    private fun updateUIGLTF(objectp: ViewerObject?, hasPbrMaterial: Boolean, hasFacesWithoutPbr: Boolean, forceSetValues: Boolean) {
        TODO("GPU: update GLTF/PBR control state from selection")
    }

    private fun updateVisibilityGLTF(objectp: ViewerObject? = null) {
        TODO("GPU: update GLTF/PBR control visibility")
    }

    private fun updateSelectedGLTFMaterials(func: (GLTFMaterial) -> Unit) {
        TODO("GPU: iterate selected TEs and apply func to their GLTF material overrides")
    }

    private fun updateSelectedGLTFMaterialsWithScale(func: (GLTFMaterial, Float, Float) -> Unit) {
        TODO("GPU: iterate selected TEs applying func with per-face object scale")
    }

    private fun updateGLTFTextureTransform(edit: (GLTFMaterial.TextureTransform) -> Unit) {
        TODO("GPU: apply texture transform edit to selected GLTF material overrides")
    }

    private fun updateGLTFTextureTransformWithScale(
        textureInfo: GLTFMaterial.TextureInfo,
        edit: (GLTFMaterial.TextureTransform, Float, Float) -> Unit
    ) {
        TODO("GPU: apply texture transform edit with scale to selected GLTF material overrides")
    }

    private fun setMaterialOverridesFromSelection() {
        TODO("APR: use JVM equivalent - read GLTF overrides from selection into UI")
    }

    private fun onTextureSelectionChanged(itemp: InventoryItem?) {
        TODO("APR: use JVM equivalent - validate texture selection against object permissions")
    }

    private fun onPbrSelectionChanged(itemp: InventoryItem?) {
        TODO("APR: use JVM equivalent - validate PBR material selection against object permissions")
    }

    private fun onCommitPbr() { TODO("GPU: commit PBR material change") }
    private fun onCancelPbr() { TODO("GPU: cancel PBR material change") }
    private fun onSelectPbr() { TODO("GPU: handle PBR material selected in picker") }
    private fun onDragPbr(item: InventoryItem?): Boolean { TODO("GPU: validate PBR drag") }
    private fun onDragTexture(item: InventoryItem?): Boolean { TODO("APR: use JVM equivalent - validate texture drag") }
    private fun onCommitTexture() { TODO("GPU: commit texture change") }
    private fun onCancelTexture() { TODO("GPU: cancel texture change") }
    private fun onSelectTexture() { TODO("GPU: handle texture selected in picker") }
    private fun onCommitSpecularTexture(data: LLSD) { TODO("GPU: commit specular texture change") }
    private fun onCancelSpecularTexture(data: LLSD) { TODO("GPU: cancel specular texture change") }
    private fun onSelectSpecularTexture(data: LLSD) { TODO("GPU: handle specular texture selected") }
    private fun onCommitNormalTexture(data: LLSD) { TODO("GPU: commit normal map change") }
    private fun onCancelNormalTexture(data: LLSD) { TODO("GPU: cancel normal map change") }
    private fun onSelectNormalTexture(data: LLSD) { TODO("GPU: handle normal map selected") }
    private fun onCommitColor() { TODO("APR: use JVM equivalent - commit color change") }
    private fun onCommitShinyColor() { TODO("GPU: commit specular light color change") }
    private fun onCommitAlpha() { TODO("APR: use JVM equivalent - commit alpha change") }
    private fun onCancelColor() { TODO("APR: use JVM equivalent - cancel color change") }
    private fun onCancelShinyColor() { TODO("GPU: cancel specular light color change") }
    private fun onSelectColor() { TODO("APR: use JVM equivalent - handle color selected") }
    private fun onSelectShinyColor() { TODO("GPU: handle specular color selected") }
    private fun onCloseTexturePicker(data: LLSD) { TODO("APR: use JVM equivalent - handle texture picker close") }
    private fun onCommitTextureInfo() { TODO("APR: use JVM equivalent - commit texture info (scale/offset/rot)") }
    private fun onCommitTextureScaleX() { TODO("APR: use JVM equivalent - commit texture scale U") }
    private fun onCommitTextureScaleY() { TODO("APR: use JVM equivalent - commit texture scale V") }
    private fun onCommitTextureRot() { TODO("APR: use JVM equivalent - commit texture rotation") }
    private fun onCommitTextureOffsetX() { TODO("APR: use JVM equivalent - commit texture offset U") }
    private fun onCommitTextureOffsetY() { TODO("APR: use JVM equivalent - commit texture offset V") }
    private fun onCommitMaterialBumpyScaleX() { TODO("GPU: commit normal map scale U") }
    private fun onCommitMaterialBumpyScaleY() { TODO("GPU: commit normal map scale V") }
    private fun onCommitMaterialBumpyRot() { TODO("GPU: commit normal map rotation") }
    private fun onCommitMaterialBumpyOffsetX() { TODO("GPU: commit normal map offset U") }
    private fun onCommitMaterialBumpyOffsetY() { TODO("GPU: commit normal map offset V") }
    private fun syncRepeatX(scaleU: Float) { TODO("APR: use JVM equivalent - sync repeat X across all material layers") }
    private fun syncRepeatY(scaleV: Float) { TODO("APR: use JVM equivalent - sync repeat Y across all material layers") }
    private fun syncOffsetX(offsetU: Float) { TODO("APR: use JVM equivalent - sync offset X across all material layers") }
    private fun syncOffsetY(offsetV: Float) { TODO("APR: use JVM equivalent - sync offset Y across all material layers") }
    private fun syncMaterialRot(rot: Float, te: Int = -1) { TODO("APR: use JVM equivalent - sync rotation across all material layers") }
    private fun onCommitMaterialShinyScaleX() { TODO("GPU: commit specular scale U") }
    private fun onCommitMaterialShinyScaleY() { TODO("GPU: commit specular scale V") }
    private fun onCommitMaterialShinyRot() { TODO("GPU: commit specular rotation") }
    private fun onCommitMaterialShinyOffsetX() { TODO("GPU: commit specular offset U") }
    private fun onCommitMaterialShinyOffsetY() { TODO("GPU: commit specular offset V") }
    private fun onCommitMaterialGloss() { TODO("GPU: commit glossiness value") }
    private fun onCommitMaterialEnv() { TODO("GPU: commit environment intensity value") }
    private fun onCommitMaterialMaskCutoff() { TODO("GPU: commit alpha mask cutoff value") }
    private fun onCommitMaterialsMedia() { TODO("APR: use JVM equivalent - commit material/media selection") }
    private fun onCommitMaterialType() { TODO("APR: use JVM equivalent - switch between Blinn-Phong material layers") }
    private fun onCommitPbrType() { TODO("GPU: switch PBR sub-texture channel selection") }
    private fun onClickBtnEditMedia() { TODO("APR: use JVM equivalent - open media settings floater") }
    private fun onClickBtnDeleteMedia() { TODO("APR: use JVM equivalent - delete media from face") }
    private fun onClickBtnAddMedia() { TODO("APR: use JVM equivalent - add media to face") }
    private fun onCommitBump() { TODO("GPU: commit bump map selection") }
    private fun onCommitTexGen() { TODO("APR: use JVM equivalent - commit texgen mode") }
    private fun onCommitShiny() { TODO("GPU: commit shininess selection") }
    private fun onCommitAlphaMode() { TODO("GPU: commit alpha mode selection") }
    private fun onCommitFullbright() { TODO("GPU: commit fullbright flag") }
    private fun onCommitHideWater() { TODO("GPU: commit hide-water flag") }
    private fun onCommitGlow() { TODO("GPU: commit glow value") }
    private fun onCommitPlanarAlign() { TODO("APR: use JVM equivalent - commit planar alignment flag") }
    private fun onCommitRepeatsPerMeter() { TODO("APR: use JVM equivalent - commit repeats-per-meter") }
    private fun onCommitGLTFTextureScaleU() { TODO("GPU: commit GLTF texture scale U") }
    private fun onCommitGLTFTextureScaleV() { TODO("GPU: commit GLTF texture scale V") }
    private fun onCommitGLTFRotation() { TODO("GPU: commit GLTF texture rotation") }
    private fun onCommitGLTFTextureOffsetU() { TODO("GPU: commit GLTF texture offset U") }
    private fun onCommitGLTFTextureOffsetV() { TODO("GPU: commit GLTF texture offset V") }
    private fun onCommitGLTFRepeatsPerMeter() { TODO("GPU: commit GLTF repeats-per-meter") }
    private fun onClickAutoFix() { TODO("APR: use JVM equivalent - auto-fix invalid texture settings") }
    private fun onAlignTexture() { TODO("APR: use JVM equivalent - align texture to face") }
    private fun onClickBtnLoadInvPBR() { TODO("GPU: load PBR material from inventory") }
    private fun onClickBtnEditPBR() { TODO("GPU: open PBR material editor") }
    private fun onClickBtnSavePBR() { TODO("GPU: save PBR material to inventory") }
    private fun onCopyFaces() { TODO("APR: use JVM equivalent - copy face attributes to clipboard") }
    private fun onPasteFaces() { TODO("APR: use JVM equivalent - paste face attributes from clipboard") }
    private fun onClickBtnSelectSameTexture(userData: LLSD) { TODO("APR: use JVM equivalent - select all faces with same texture") }
    private fun onClickMapsSync() { TODO("APR: use JVM equivalent - sync all material map transforms") }
    private fun alignMaterialsProperties() { TODO("APR: use JVM equivalent - align all material map transforms to diffuse") }

    fun onCommitFlip(userData: LLSD) { TODO("APR: use JVM equivalent - flip texture scale sign") }

    fun onCopyColor() { TODO("APR: use JVM equivalent - copy color from all selected faces") }
    fun onPasteColor() { TODO("APR: use JVM equivalent - paste color to selection") }
    fun onPasteColor(objectp: ViewerObject?, te: Int) { TODO("APR: use JVM equivalent - paste color to specific face") }
    fun onCopyTexture() { TODO("APR: use JVM equivalent - copy texture from selection") }
    fun onPasteTexture() { TODO("APR: use JVM equivalent - paste texture to selection") }
    fun onPasteTexture(objectp: ViewerObject?, te: Int) { TODO("APR: use JVM equivalent - paste texture to specific face") }

    private fun validateInventoryItem(te: LLSD, prefix: String): Boolean {
        TODO("APR: use JVM equivalent - validate inventory item for paste operation")
    }

    private fun getCurrentNormalMap(): Uuid { TODO("APR: use JVM equivalent - get current normal map UUID") }
    private fun getCurrentSpecularMap(): Uuid { TODO("APR: use JVM equivalent - get current specular map UUID") }
    private fun getCurrentShininess(): UInt { TODO("APR: use JVM equivalent - get current shininess value") }
    private fun getCurrentBumpiness(): UInt { TODO("APR: use JVM equivalent - get current bumpiness value") }
    private fun getCurrentDiffuseAlphaMode(): UByte { TODO("APR: use JVM equivalent - get current diffuse alpha mode") }
    private fun getCurrentAlphaMaskCutoff(): UByte { TODO("APR: use JVM equivalent - get current alpha mask cutoff") }
    private fun getCurrentEnvIntensity(): UByte { TODO("GPU: get current environment intensity") }
    private fun getCurrentGlossiness(): UByte { TODO("GPU: get current glossiness") }
    private fun getCurrentBumpyRot(): Float { TODO("GPU: get current bump rotation") }
    private fun getCurrentBumpyScaleU(): Float { TODO("GPU: get current bump scale U") }
    private fun getCurrentBumpyScaleV(): Float { TODO("GPU: get current bump scale V") }
    private fun getCurrentBumpyOffsetU(): Float { TODO("GPU: get current bump offset U") }
    private fun getCurrentBumpyOffsetV(): Float { TODO("GPU: get current bump offset V") }
    private fun getCurrentShinyRot(): Float { TODO("GPU: get current specular rotation") }
    private fun getCurrentShinyScaleU(): Float { TODO("GPU: get current specular scale U") }
    private fun getCurrentShinyScaleV(): Float { TODO("GPU: get current specular scale V") }
    private fun getCurrentShinyOffsetU(): Float { TODO("GPU: get current specular offset U") }
    private fun getCurrentShinyOffsetV(): Float { TODO("GPU: get current specular offset V") }
    private fun getCurrentTextureRot(): Float { TODO("APR: use JVM equivalent - get current diffuse texture rotation") }
    private fun getCurrentTextureScaleU(): Float { TODO("APR: use JVM equivalent - get current diffuse scale U") }
    private fun getCurrentTextureScaleV(): Float { TODO("APR: use JVM equivalent - get current diffuse scale V") }
    private fun getCurrentTextureOffsetU(): Float { TODO("APR: use JVM equivalent - get current diffuse offset U") }
    private fun getCurrentTextureOffsetV(): Float { TODO("APR: use JVM equivalent - get current diffuse offset V") }

    class Selection {
        private var changed: Boolean = false
        private var needsSelectionCheck: Boolean = true
        private var selectedObjectCount: Int = 0
        private var selectedTeCount: Int = 0
        private var selectedObjectId: Uuid = Uuid(0, 0)
        private var lastSelectedSide: Int = -1

        fun connect() {
            TODO("APR: use JVM equivalent - connect to selection change signal")
        }

        fun update(): Boolean {
            TODO("APR: use JVM equivalent - check if selection changed since last call")
        }

        fun setDirty() { changed = true }

        fun onSelectionChanged() { needsSelectionCheck = true }

        fun onSelectedObjectUpdated(objectId: Uuid, side: Int) {
            TODO("APR: use JVM equivalent - mark dirty when specific object/side is updated")
        }

        private fun compareSelection(): Boolean {
            TODO("APR: use JVM equivalent - compare current vs. cached selection state")
        }
    }

    object SelectedTEMaterial {
        fun getCurrent(materialOut: (MaterialPtr?) -> Unit, identicalOut: (Boolean) -> Unit) {
            TODO("APR: use JVM equivalent - get material from selected TE")
        }
        fun getMaxSpecularRepeats(repeatsOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) {
            TODO("GPU: get max specular repeats across selection")
        }
        fun getMaxNormalRepeats(repeatsOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) {
            TODO("GPU: get max normal repeats across selection")
        }
        fun getCurrentDiffuseAlphaMode(modeOut: (UByte) -> Unit, identicalOut: (Boolean) -> Unit) {
            TODO("APR: use JVM equivalent - get diffuse alpha mode across selection")
        }
        fun selectionNormalScaleAutofit(panelFace: PanelFace, repeatsPerMeter: Float) {
            TODO("GPU: autofit normal map scale to repeats-per-meter")
        }
        fun selectionSpecularScaleAutofit(panelFace: PanelFace, repeatsPerMeter: Float) {
            TODO("GPU: autofit specular scale to repeats-per-meter")
        }
        fun getNormalID(dataOut: (Uuid) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get normal map UUID") }
        fun getSpecularID(dataOut: (Uuid) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get specular map UUID") }
        fun getSpecularRepeatX(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get specular repeat X") }
        fun getSpecularRepeatY(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get specular repeat Y") }
        fun getSpecularOffsetX(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get specular offset X") }
        fun getSpecularOffsetY(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get specular offset Y") }
        fun getSpecularRotation(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get specular rotation") }
        fun getNormalRepeatX(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get normal repeat X") }
        fun getNormalRepeatY(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get normal repeat Y") }
        fun getNormalOffsetX(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get normal offset X") }
        fun getNormalOffsetY(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get normal offset Y") }
        fun getNormalRotation(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get normal rotation") }
        fun setDiffuseAlphaMode(panelFace: PanelFace, data: UByte, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set diffuse alpha mode on selection") }
        fun setAlphaMaskCutoff(panelFace: PanelFace, data: UByte, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set alpha mask cutoff on selection") }
        fun setNormalOffsetX(panelFace: PanelFace, data: Float, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set normal offset X") }
        fun setNormalOffsetY(panelFace: PanelFace, data: Float, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set normal offset Y") }
        fun setNormalRepeatX(panelFace: PanelFace, data: Float, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set normal repeat X") }
        fun setNormalRepeatY(panelFace: PanelFace, data: Float, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set normal repeat Y") }
        fun setNormalRotation(panelFace: PanelFace, data: Float, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set normal rotation") }
        fun setSpecularOffsetX(panelFace: PanelFace, data: Float, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set specular offset X") }
        fun setSpecularOffsetY(panelFace: PanelFace, data: Float, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set specular offset Y") }
        fun setSpecularRepeatX(panelFace: PanelFace, data: Float, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set specular repeat X") }
        fun setSpecularRepeatY(panelFace: PanelFace, data: Float, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set specular repeat Y") }
        fun setSpecularRotation(panelFace: PanelFace, data: Float, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set specular rotation") }
        fun setEnvironmentIntensity(panelFace: PanelFace, data: UByte, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set environment intensity") }
        fun setSpecularLightExponent(panelFace: PanelFace, data: UByte, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set specular light exponent") }
        fun setNormalID(panelFace: PanelFace, data: Uuid, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set normal map UUID") }
        fun setSpecularID(panelFace: PanelFace, data: Uuid, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set specular map UUID") }
        fun setSpecularLightColor(panelFace: PanelFace, data: Color4U, te: Int = -1, onlyForObjectId: Uuid = Uuid(0,0)) { TODO("GPU: set specular light color") }
    }

    object SelectedTE {
        fun getFace(faceOut: (Any?) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get face from selection") }
        fun getImageFormat(formatOut: (Int) -> Unit, hasAlphaOut: (Boolean) -> Unit, identicalOut: (Boolean) -> Unit, missingOut: (Boolean) -> Unit) { TODO("GPU: get image format from selection") }
        fun getTexId(idOut: (Uuid) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get diffuse texture UUID") }
        fun getPbrMaterialId(idOut: (Uuid) -> Unit, identicalOut: (Boolean) -> Unit, hasPbrOut: (Boolean) -> Unit, hasFacesWithoutPbrOut: (Boolean) -> Unit) { TODO("GPU: get PBR material UUID") }
        fun getObjectScaleS(scaleSOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get object scale S") }
        fun getObjectScaleT(scaleTOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get object scale T") }
        fun getMaxDiffuseRepeats(repeatsOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get max diffuse repeats across selection") }
        fun getBumpmap(dataOut: (UByte) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get bump map value") }
        fun getShiny(dataOut: (UByte) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get shininess value") }
        fun getFullbright(dataOut: (UByte) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get fullbright value") }
        fun getRotation(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get texture rotation") }
        fun getOffsetS(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get texture offset S") }
        fun getOffsetT(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get texture offset T") }
        fun getScaleS(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get texture scale S") }
        fun getScaleT(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get texture scale T") }
        fun getGlow(dataOut: (Float) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("GPU: get glow value") }
        fun getTexGen(dataOut: (Int) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get texgen mode") }
        fun getColor(dataOut: (Color4) -> Unit, identicalOut: (Boolean) -> Unit) { TODO("APR: use JVM equivalent - get face color") }
    }
}
