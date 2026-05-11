package com.firestorm.newview

import com.firestorm.ui.LLFloater
import com.firestorm.ui.LLSliderCtrl
import com.firestorm.ui.LLTextBox
import com.firestorm.ui.LLComboBox
import com.firestorm.ui.LLCheckBoxCtrl
import com.firestorm.llsd.LLSD

class LLFloaterPreferenceGraphicsAdvanced(key: LLSD) : LLFloater(key) {

    private var mImpostorsChangedSignal: (() -> Unit)? = null
    private var mComplexityChangedSignal: (() -> Unit)? = null
    private var mComplexityModeChangedSignal: (() -> Unit)? = null
    private var mLODFactorChangedSignal: (() -> Unit)? = null
    private var mNumImpostorsChangedSignal: (() -> Unit)? = null

    init {
        mCommitCallbackRegistrar.add("Pref.RenderOptionUpdate") { onRenderOptionEnable() }
        mCommitCallbackRegistrar.add("Pref.UpdateIndirectMaxNonImpostors") { updateMaxNonImpostors() }
        mCommitCallbackRegistrar.add("Pref.UpdateIndirectMaxComplexity") { updateMaxComplexity() }
        mCommitCallbackRegistrar.add("Pref.Cancel") { userdata: LLSD -> onBtnCancel(userdata) }
        mCommitCallbackRegistrar.add("Pref.OK") { userdata: LLSD -> onBtnOK(userdata) }

        mImpostorsChangedSignal = gSavedSettings.getControl("RenderAvatarMaxNonImpostors")
            .connectSignal { newValue -> updateIndirectMaxNonImpostors(newValue) }
    }

    fun postBuild(): Boolean {
        val combo = getChild<LLComboBox>("fsaa")
        TODO("GPU: check gFXAAProgram[0].isComplete() / gSMAAEdgeDetectProgram[0].isComplete() and remove entries accordingly")

        mComplexityChangedSignal = gSavedSettings.getControl("RenderAvatarMaxComplexity")
            .connectCommitSignal { updateComplexityText() }

        mComplexityModeChangedSignal = gSavedSettings.getControl("RenderAvatarComplexityMode")
            .connectSignal { newValue -> updateComplexityMode(newValue) }

        mLODFactorChangedSignal = gSavedSettings.getControl("RenderVolumeLODFactor")
            .connectCommitSignal { updateObjectMeshDetailText() }

        mNumImpostorsChangedSignal = gSavedSettings.getControl("RenderAvatarMaxNonImpostors")
            .connectSignal { newValue -> updateIndirectMaxNonImpostors(newValue) }

        return true
    }

    fun onOpen(key: LLSD) {
        refresh()
    }

    fun onClickCloseBtn(appQuitting: Boolean) {
        val instance = LLFloaterReg.findTypedInstance<LLFloaterPreference>("preferences")
        instance?.cancel(listOf("RenderQualityPerformance"))
        updateMaxComplexity()
    }

    fun onRenderOptionEnable() {
        val instance = LLFloaterReg.findTypedInstance<LLFloaterPreference>("preferences")
        instance?.refresh()
        refreshEnabledGraphics()
    }

    fun onAdvancedAtmosphericsEnable() {
        val instance = LLFloaterReg.findTypedInstance<LLFloaterPreference>("preferences")
        instance?.refresh()
        refreshEnabledGraphics()
    }

    fun refresh() {
        updateSliderText(getChild("ObjectMeshDetail"), getChild("ObjectMeshDetailText"))
        updateSliderText(getChild("FlexibleMeshDetail"), getChild("FlexibleMeshDetailText"))
        updateSliderText(getChild("TreeMeshDetail"), getChild("TreeMeshDetailText"))
        updateSliderText(getChild("AvatarMeshDetail"), getChild("AvatarMeshDetailText"))
        updateSliderText(getChild("AvatarPhysicsDetail"), getChild("AvatarPhysicsDetailText"))
        updateSliderText(getChild("TerrainMeshDetail"), getChild("TerrainMeshDetailText"))
        updateSliderText(getChild("RenderPostProcess"), getChild("PostProcessText"))
        updateSliderText(getChild("SkyMeshDetail"), getChild("SkyMeshDetailText"))

        LLAvatarComplexityControls.setIndirectControls()
        setMaxNonImpostorsText(
            gSavedSettings.getU32("RenderAvatarMaxNonImpostors"),
            getChild("IndirectMaxNonImpostorsText")
        )
        LLAvatarComplexityControls.setText(
            gSavedSettings.getU32("RenderAvatarMaxComplexity"),
            getChild("IndirectMaxComplexityText")
        )
        refreshEnabledState()

        val enableComplexity = gSavedSettings.getS32("RenderAvatarComplexityMode") != LLVOAvatar.AV_RENDER_ONLY_SHOW_FRIENDS
        getChild<LLSliderCtrl>("IndirectMaxComplexity").setEnabled(enableComplexity)
        getChild<LLSliderCtrl>("IndirectMaxNonImpostors").setEnabled(enableComplexity)
    }

    fun refreshEnabledGraphics() {
        refreshEnabledState()
    }

    fun updateMaxComplexity() {
        LLAvatarComplexityControls.updateMax(
            getChild("IndirectMaxComplexity"),
            getChild("IndirectMaxComplexityText")
        )
    }

    fun updateComplexityMode(newValue: LLSD) {
        val enableComplexity = newValue.asInteger() != LLVOAvatar.AV_RENDER_ONLY_SHOW_FRIENDS
        getChild<LLSliderCtrl>("IndirectMaxComplexity").setEnabled(enableComplexity)
        getChild<LLSliderCtrl>("IndirectMaxNonImpostors").setEnabled(enableComplexity)
    }

    fun updateComplexityText() {
        LLAvatarComplexityControls.setText(
            gSavedSettings.getU32("RenderAvatarMaxComplexity"),
            getChild("IndirectMaxComplexityText")
        )
    }

    fun updateObjectMeshDetailText() {
        updateSliderText(getChild("ObjectMeshDetail"), getChild("ObjectMeshDetailText"))
    }

    fun updateSliderText(ctrl: LLSliderCtrl?, textBox: LLTextBox?) {
        if (textBox == null || ctrl == null) return

        val value = ctrl.getValue().asReal().toFloat()
        val min = ctrl.getMinValue()
        val max = ctrl.getMaxValue()
        val range = max - min
        val midPoint = min + range / 3.0f
        val highPoint = min + (2.0f * range / 3.0f)

        when {
            value < midPoint -> textBox.setText(LLTrans.getString("GraphicsQualityLow"))
            value < highPoint -> textBox.setText(LLTrans.getString("GraphicsQualityMid"))
            else -> textBox.setText(LLTrans.getString("GraphicsQualityHigh"))
        }
    }

    fun updateMaxNonImpostors() {
        val ctrl = getChild<LLSliderCtrl>("IndirectMaxNonImpostors")
        var value = ctrl.getValue().asInteger().toUInt()

        if (value == 0u || value >= LLVOAvatar.NON_IMPOSTORS_MAX_SLIDER) {
            value = 0u
        }
        gSavedSettings.setU32("RenderAvatarMaxNonImpostors", value)
        LLVOAvatar.updateImpostorRendering(value)
        setMaxNonImpostorsText(value, getChild("IndirectMaxNonImpostorsText"))
    }

    fun updateIndirectMaxNonImpostors(newValue: LLSD) {
        val value = newValue.asInteger().toUInt()
        if (value != 0u && value != gSavedSettings.getU32("IndirectMaxNonImpostors")) {
            gSavedSettings.setU32("IndirectMaxNonImpostors", value)
        }
        setMaxNonImpostorsText(value, getChild("IndirectMaxNonImpostorsText"))
    }

    fun setMaxNonImpostorsText(value: UInt, textBox: LLTextBox) {
        if (value == 0u) {
            textBox.setText(LLTrans.getString("no_limit"))
        } else {
            textBox.setText(value.toString())
        }
    }

    fun disableUnavailableSettings() {
        val ctrlShadows = getChild<LLComboBox>("ShadowDetail")
        val shadowsText = getChild<LLTextBox>("RenderShadowDetailText")
        val ctrlSsao = getChild<LLCheckBoxCtrl>("UseSSAO")
        val ctrlDof = getChild<LLCheckBoxCtrl>("UseDoF")
        val sky = getChild<LLSliderCtrl>("SkyMeshDetail")
        val skyText = getChild<LLTextBox>("SkyMeshDetailText")
        val casSlider = getChild<LLSliderCtrl>("RenderSharpness")

        if (!LLFeatureManager.getInstance().isFeatureAvailable("WindLightUseAtmosShaders")) {
            sky.setEnabled(false)
            skyText.setEnabled(false)
            ctrlShadows.setEnabled(false)
            ctrlShadows.setValue(0)
            shadowsText.setEnabled(false)
            ctrlSsao.setEnabled(false)
            ctrlSsao.setValue(false)
            ctrlDof.setEnabled(false)
            ctrlDof.setValue(false)
        }

        if (!LLFeatureManager.getInstance().isFeatureAvailable("RenderDeferred")) {
            ctrlShadows.setEnabled(false)
            ctrlShadows.setValue(0)
            shadowsText.setEnabled(false)
            ctrlSsao.setEnabled(false)
            ctrlSsao.setValue(false)
            ctrlDof.setEnabled(false)
            ctrlDof.setValue(false)
        }

        if (!LLFeatureManager.getInstance().isFeatureAvailable("RenderDeferredSSAO")) {
            ctrlSsao.setEnabled(false)
            ctrlSsao.setValue(false)
        }

        if (!LLFeatureManager.getInstance().isFeatureAvailable("RenderShadowDetail")) {
            ctrlShadows.setEnabled(false)
            ctrlShadows.setValue(0)
            shadowsText.setEnabled(false)
        }

        val isNotVintage = gSavedSettings.getBool("RenderDisableVintageMode")
        val tonemapMix = getChild<LLSliderCtrl>("TonemapMix")
        val tonemapSelect = getChild<LLComboBox>("TonemapType")
        val tonemapLabel = getChild<LLTextBox>("TonemapTypeText")
        val exposureSlider = getChild<LLSliderCtrl>("RenderExposure")

        tonemapSelect.setEnabled(isNotVintage)
        tonemapLabel.setEnabled(isNotVintage)
        tonemapMix.setEnabled(isNotVintage)
        exposureSlider.setEnabled(isNotVintage)
        casSlider.setEnabled(isNotVintage)
    }

    fun refreshEnabledState() {
        val sky = getChild<LLSliderCtrl>("SkyMeshDetail")
        val skyText = getChild<LLTextBox>("SkyMeshDetailText")
        sky.setEnabled(true)
        skyText.setEnabled(true)

        var enabled = true

        val ctrlSsao = getChild<LLCheckBoxCtrl>("UseSSAO")
        val ctrlDof = getChild<LLCheckBoxCtrl>("UseDoF")
        val ctrlShadow = getChild<LLComboBox>("ShadowDetail")
        val shadowText = getChild<LLTextBox>("RenderShadowDetailText")

        enabled = enabled && LLFeatureManager.getInstance().isFeatureAvailable("RenderDeferredSSAO")
        ctrlSsao.setEnabled(enabled)
        ctrlDof.setEnabled(enabled)

        enabled = enabled && LLFeatureManager.getInstance().isFeatureAvailable("RenderShadowDetail")
        ctrlShadow.setEnabled(enabled)
        shadowText.setEnabled(enabled)

        if (!LLFeatureManager.getInstance().isFeatureAvailable("RenderVBOEnable")) {
            getChildView("vbo").setEnabled(false)
        }

        if (!LLFeatureManager.getInstance().isFeatureAvailable("RenderCompressTextures")) {
            getChildView("texture compression").setEnabled(false)
        }

        getChildView("antialiasing restart").setVisible(
            !LLFeatureManager.getInstance().isFeatureAvailable("RenderDeferred")
        )

        disableUnavailableSettings()
    }

    protected fun onBtnOK(userdata: LLSD) {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterPreference>("preferences")
        instance?.onBtnOK(userdata)
    }

    protected fun onBtnCancel(userdata: LLSD) {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterPreference>("preferences")
        instance?.onBtnCancel(userdata)
    }
}
