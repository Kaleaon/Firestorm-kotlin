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
            return 0f
        }

        @JvmStatic fun deleteMediaConfirm(notification: LLSD, response: LLSD): Boolean {
            return false
        }

        @JvmStatic fun multipleFacesSelectedConfirm(notification: LLSD, response: LLSD): Boolean {
            return false
        }
    }

    override fun postBuild(): Boolean {
        System.err.println("PanelFace: postBuild not yet implemented")
        return false
    }

    fun refresh() {
        System.err.println("PanelFace: refresh not yet implemented")
    }

    fun refreshMedia() {
        System.err.println("PanelFace: refreshMedia not yet implemented")
    }

    fun unloadMedia() {
        System.err.println("PanelFace: unloadMedia not yet implemented")
    }

    fun changePrecision(decimalPrecision: Int) {
        System.err.println("PanelFace: changePrecision not yet implemented")
    }

    override fun onVisibilityChange(newVisibility: Boolean) {
        System.err.println("PanelFace: onVisibilityChange not yet implemented")
    }

    override fun draw() {
    }

    fun createDefaultMaterial(currentMaterial: MaterialPtr?): MaterialPtr {
        System.err.println("PanelFace: createDefaultMaterial not yet implemented")
        return currentMaterial ?: MaterialPtr()
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
        return 0
    }

    fun getMatTextureChannel(): Int {
        return 0
    }

    fun getPBRTextureChannel(): Int {
        return 0
    }

    fun getTextureDropChannel(): Int {
        return 0
    }

    fun getPBRDropChannel(): GLTFMaterial.TextureInfo {
        return GLTFMaterial.TextureInfo.COUNT
    }

    private fun getState() {
        System.err.println("PanelFace: getState not yet implemented")
    }

    private fun sendTexture() {
        System.err.println("PanelFace: sendTexture not yet implemented")
    }

    private fun sendTextureInfo() {
        System.err.println("PanelFace: sendTextureInfo not yet implemented")
    }

    private fun sendColor() {
        System.err.println("PanelFace: sendColor not yet implemented")
    }

    private fun sendAlpha() {
        System.err.println("PanelFace: sendAlpha not yet implemented")
    }

    private fun sendBump(bumpiness: UInt) {
        System.err.println("PanelFace: sendBump not yet implemented")
    }

    private fun sendTexGen() {
        System.err.println("PanelFace: sendTexGen not yet implemented")
    }

    private fun sendShiny(shininess: UInt) {
        System.err.println("PanelFace: sendShiny not yet implemented")
    }

    private fun sendFullbright() {
        System.err.println("PanelFace: sendFullbright not yet implemented")
    }

    private fun sendGlow() {
        System.err.println("PanelFace: sendGlow not yet implemented")
    }

    private fun alignTextureLayer() {
        System.err.println("PanelFace: alignTextureLayer not yet implemented")
    }

    private fun updateCopyTexButton() {
        System.err.println("PanelFace: updateCopyTexButton not yet implemented")
    }

    private fun navigateToTitleMedia(url: String) {
        System.err.println("PanelFace: navigateToTitleMedia not yet implemented")
    }

    private fun selectedMediaEditable(): Boolean {
        return false
    }

    private fun clearMediaSettings() {
        System.err.println("PanelFace: clearMediaSettings not yet implemented")
    }

    private fun updateMediaSettings() {
        System.err.println("PanelFace: updateMediaSettings not yet implemented")
    }

    private fun updateMediaTitle() {
        System.err.println("PanelFace: updateMediaTitle not yet implemented")
    }

    private fun isMediaTexSelected(): Boolean {
        return false
    }

    private fun updateUI(forceSetValues: Boolean = false) {
        System.err.println("PanelFace: updateUI not yet implemented")
    }

    private fun updateVisibility(objectp: ViewerObject? = null) {
        System.err.println("PanelFace: updateVisibility not yet implemented")
    }

    private fun isIdenticalPlanarTexgen(): Boolean {
        return false
    }

    private fun updateShinyControls(isSettingTexture: Boolean = false, messWithCombobox: Boolean = false) {
        System.err.println("PanelFace: updateShinyControls not yet implemented")
    }

    private fun updateBumpyControls(isSettingTexture: Boolean = false, messWithCombobox: Boolean = false) {
        System.err.println("PanelFace: updateBumpyControls not yet implemented")
    }

    private fun updateAlphaControls() {
        System.err.println("PanelFace: updateAlphaControls not yet implemented")
    }

    private fun updateUIGLTF(objectp: ViewerObject?, hasPbrMaterial: Boolean, hasFacesWithoutPbr: Boolean, forceSetValues: Boolean) {
    }

    private fun updateVisibilityGLTF(objectp: ViewerObject? = null) {
    }

    private fun updateSelectedGLTFMaterials(func: (GLTFMaterial) -> Unit) {
    }

    private fun updateSelectedGLTFMaterialsWithScale(func: (GLTFMaterial, Float, Float) -> Unit) {
    }

    private fun updateGLTFTextureTransform(edit: (GLTFMaterial.TextureTransform) -> Unit) {
    }

    private fun updateGLTFTextureTransformWithScale(
        textureInfo: GLTFMaterial.TextureInfo,
        edit: (GLTFMaterial.TextureTransform, Float, Float) -> Unit
    ) {
    }

    private fun setMaterialOverridesFromSelection() {
        System.err.println("PanelFace: setMaterialOverridesFromSelection not yet implemented")
    }

    private fun onTextureSelectionChanged(itemp: InventoryItem?) {
        System.err.println("PanelFace: onTextureSelectionChanged not yet implemented")
    }

    private fun onPbrSelectionChanged(itemp: InventoryItem?) {
        System.err.println("PanelFace: onPbrSelectionChanged not yet implemented")
    }

    private fun onCommitPbr() { }
    private fun onCancelPbr() { }
    private fun onSelectPbr() { }
    private fun onDragPbr(item: InventoryItem?): Boolean { return false }
    private fun onDragTexture(item: InventoryItem?): Boolean { return false }
    private fun onCommitTexture() { }
    private fun onCancelTexture() { }
    private fun onSelectTexture() { }
    private fun onCommitSpecularTexture(data: LLSD) { }
    private fun onCancelSpecularTexture(data: LLSD) { }
    private fun onSelectSpecularTexture(data: LLSD) { }
    private fun onCommitNormalTexture(data: LLSD) { }
    private fun onCancelNormalTexture(data: LLSD) { }
    private fun onSelectNormalTexture(data: LLSD) { }
    private fun onCommitColor() { System.err.println("PanelFace: onCommitColor not yet implemented") }
    private fun onCommitShinyColor() { }
    private fun onCommitAlpha() { System.err.println("PanelFace: onCommitAlpha not yet implemented") }
    private fun onCancelColor() { System.err.println("PanelFace: onCancelColor not yet implemented") }
    private fun onCancelShinyColor() { }
    private fun onSelectColor() { System.err.println("PanelFace: onSelectColor not yet implemented") }
    private fun onSelectShinyColor() { }
    private fun onCloseTexturePicker(data: LLSD) { System.err.println("PanelFace: onCloseTexturePicker not yet implemented") }
    private fun onCommitTextureInfo() { System.err.println("PanelFace: onCommitTextureInfo not yet implemented") }
    private fun onCommitTextureScaleX() { System.err.println("PanelFace: onCommitTextureScaleX not yet implemented") }
    private fun onCommitTextureScaleY() { System.err.println("PanelFace: onCommitTextureScaleY not yet implemented") }
    private fun onCommitTextureRot() { System.err.println("PanelFace: onCommitTextureRot not yet implemented") }
    private fun onCommitTextureOffsetX() { System.err.println("PanelFace: onCommitTextureOffsetX not yet implemented") }
    private fun onCommitTextureOffsetY() { System.err.println("PanelFace: onCommitTextureOffsetY not yet implemented") }
    private fun onCommitMaterialBumpyScaleX() { }
    private fun onCommitMaterialBumpyScaleY() { }
    private fun onCommitMaterialBumpyRot() { }
    private fun onCommitMaterialBumpyOffsetX() { }
    private fun onCommitMaterialBumpyOffsetY() { }
    private fun syncRepeatX(scaleU: Float) { System.err.println("PanelFace: syncRepeatX not yet implemented") }
    private fun syncRepeatY(scaleV: Float) { System.err.println("PanelFace: syncRepeatY not yet implemented") }
    private fun syncOffsetX(offsetU: Float) { System.err.println("PanelFace: syncOffsetX not yet implemented") }
    private fun syncOffsetY(offsetV: Float) { System.err.println("PanelFace: syncOffsetY not yet implemented") }
    private fun syncMaterialRot(rot: Float, te: Int = -1) { System.err.println("PanelFace: syncMaterialRot not yet implemented") }
    private fun onCommitMaterialShinyScaleX() { }
    private fun onCommitMaterialShinyScaleY() { }
    private fun onCommitMaterialShinyRot() { }
    private fun onCommitMaterialShinyOffsetX() { }
    private fun onCommitMaterialShinyOffsetY() { }
    private fun onCommitMaterialGloss() { }
    private fun onCommitMaterialEnv() { }
    private fun onCommitMaterialMaskCutoff() { }
    private fun onCommitMaterialsMedia() { System.err.println("PanelFace: onCommitMaterialsMedia not yet implemented") }
    private fun onCommitMaterialType() { System.err.println("PanelFace: onCommitMaterialType not yet implemented") }
    private fun onCommitPbrType() { }
    private fun onClickBtnEditMedia() { System.err.println("PanelFace: onClickBtnEditMedia not yet implemented") }
    private fun onClickBtnDeleteMedia() { System.err.println("PanelFace: onClickBtnDeleteMedia not yet implemented") }
    private fun onClickBtnAddMedia() { System.err.println("PanelFace: onClickBtnAddMedia not yet implemented") }
    private fun onCommitBump() { }
    private fun onCommitTexGen() { System.err.println("PanelFace: onCommitTexGen not yet implemented") }
    private fun onCommitShiny() { }
    private fun onCommitAlphaMode() { }
    private fun onCommitFullbright() { }
    private fun onCommitHideWater() { }
    private fun onCommitGlow() { }
    private fun onCommitPlanarAlign() { System.err.println("PanelFace: onCommitPlanarAlign not yet implemented") }
    private fun onCommitRepeatsPerMeter() { System.err.println("PanelFace: onCommitRepeatsPerMeter not yet implemented") }
    private fun onCommitGLTFTextureScaleU() { }
    private fun onCommitGLTFTextureScaleV() { }
    private fun onCommitGLTFRotation() { }
    private fun onCommitGLTFTextureOffsetU() { }
    private fun onCommitGLTFTextureOffsetV() { }
    private fun onCommitGLTFRepeatsPerMeter() { }
    private fun onClickAutoFix() { System.err.println("PanelFace: onClickAutoFix not yet implemented") }
    private fun onAlignTexture() { System.err.println("PanelFace: onAlignTexture not yet implemented") }
    private fun onClickBtnLoadInvPBR() { }
    private fun onClickBtnEditPBR() { }
    private fun onClickBtnSavePBR() { }
    private fun onCopyFaces() { System.err.println("PanelFace: onCopyFaces not yet implemented") }
    private fun onPasteFaces() { System.err.println("PanelFace: onPasteFaces not yet implemented") }
    private fun onClickBtnSelectSameTexture(userData: LLSD) { System.err.println("PanelFace: onClickBtnSelectSameTexture not yet implemented") }
    private fun onClickMapsSync() { System.err.println("PanelFace: onClickMapsSync not yet implemented") }
    private fun alignMaterialsProperties() { System.err.println("PanelFace: alignMaterialsProperties not yet implemented") }

    fun onCommitFlip(userData: LLSD) { System.err.println("PanelFace: onCommitFlip not yet implemented") }

    fun onCopyColor() { System.err.println("PanelFace: onCopyColor not yet implemented") }
    fun onPasteColor() { System.err.println("PanelFace: onPasteColor not yet implemented") }
    fun onPasteColor(objectp: ViewerObject?, te: Int) { System.err.println("PanelFace: onPasteColor(object, te) not yet implemented") }
    fun onCopyTexture() { System.err.println("PanelFace: onCopyTexture not yet implemented") }
    fun onPasteTexture() { System.err.println("PanelFace: onPasteTexture not yet implemented") }
    fun onPasteTexture(objectp: ViewerObject?, te: Int) { System.err.println("PanelFace: onPasteTexture(object, te) not yet implemented") }

    private fun validateInventoryItem(te: LLSD, prefix: String): Boolean {
        return false
    }

    private fun getCurrentNormalMap(): Uuid { return Uuid(0, 0) }
    private fun getCurrentSpecularMap(): Uuid { return Uuid(0, 0) }
    private fun getCurrentShininess(): UInt { return 0u }
    private fun getCurrentBumpiness(): UInt { return 0u }
    private fun getCurrentDiffuseAlphaMode(): UByte { return 0u }
    private fun getCurrentAlphaMaskCutoff(): UByte { return 0u }
    private fun getCurrentEnvIntensity(): UByte { return 0u }
    private fun getCurrentGlossiness(): UByte { return 0u }
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

    class Selection {
        private var changed: Boolean = false
        private var needsSelectionCheck: Boolean = true
        private var selectedObjectCount: Int = 0
        private var selectedTeCount: Int = 0
        private var selectedObjectId: Uuid = Uuid(0, 0)
        private var lastSelectedSide: Int = -1

        fun connect() {
            System.err.println("PanelFace: Selection.connect not yet implemented")
        }

        fun update(): Boolean {
            return false
        }

        fun setDirty() { changed = true }

        fun onSelectionChanged() { needsSelectionCheck = true }

        fun onSelectedObjectUpdated(objectId: Uuid, side: Int) {
            System.err.println("PanelFace: Selection.onSelectedObjectUpdated not yet implemented")
        }

        private fun compareSelection(): Boolean {
            return false
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
