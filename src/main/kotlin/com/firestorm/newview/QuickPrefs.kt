package com.firestorm.newview

import java.util.UUID

const val PHOTOTOOLS_FLOATER = "phototools"
const val PRESET_NAME_REGION_DEFAULT = "__Regiondefault__"
const val PRESET_NAME_DAY_CYCLE = "__Day_Cycle__"
const val PRESET_NAME_NONE = "__None__"

enum class QuickPrefUpdateParam {
    QP_PARAM_SKY,
    QP_PARAM_WATER,
    QP_PARAM_DAYCYCLE
}

enum class ControlType {
    Checkbox,
    Text,
    Spinner,
    Slider,
    Radio,
    Color3,
    Color4
}

data class ControlEntry(
    var panel: Any?,
    var widget: UiControl?,
    var labelTextbox: TextLabel?,
    var label: String,
    var type: ControlType,
    var integer: Boolean,
    var minValue: Float,
    var maxValue: Float,
    var increment: Float,
    var value: Any? = null,
    var signalConnection: (() -> Unit)? = null
)

data class QuickPrefsXmlEntry(
    val controlName: String,
    val label: String,
    val translationId: String?,
    val controlType: UInt,
    val integer: Boolean,
    val minValue: Float,
    val maxValue: Float,
    val increment: Float
)

data class QuickPrefsXml(
    val entries: MutableList<QuickPrefsXmlEntry> = mutableListOf()
)

interface UiControl {
    var controlName: String
    var visible: Boolean
    var enabled: Boolean
    fun getValue(): Any?
    fun setValue(value: Any?)
    fun getControlVariable(): ControlVariable?
}

interface TextLabel : UiControl {
    fun setDoubleClickCallback(cb: (UiControl) -> Unit)
    fun setMouseUpCallback(cb: (UiControl) -> Unit)
}

interface SliderControl : UiControl {
    fun setMinValue(v: Float)
    fun setMaxValue(v: Float)
    fun setIncrement(v: Float)
    fun setPrecision(decimals: Int)
    fun getValueF32(): Float
    fun setSliderMouseUpCallback(cb: () -> Unit)
    fun setSliderEditorCommitCallback(cb: () -> Unit)
    fun setCommitCallback(cb: () -> Unit)
    var isMouseHeldDown: Boolean
}

interface SpinnerControl : UiControl {
    fun setMinValue(v: Float)
    fun setMaxValue(v: Float)
    fun setIncrement(v: Float)
    fun setPrecision(decimals: Int)
    fun getValueF32(): Float
}

interface ComboBox : UiControl {
    fun addItem(label: String, value: Any?, atBottom: Boolean = true, enabled: Boolean = true)
    fun addSeparator()
    fun clearAll()
    fun getCurrentIndex(): Int
    fun getItemCount(): Int
    fun setCurrentByIndex(index: Int)
    fun selectByValue(value: Any?)
    fun getSelectedValue(): Any?
    fun getItemByValue(value: Any?): ComboBoxItem?
    fun sortByName()
    fun addSimpleElement(name: String)
    fun setCommitCallback(cb: () -> Unit)
}

interface ComboBoxItem {
    var isEnabled: Boolean
}

interface CheckBoxControl : UiControl {
    fun setCommitCallback(cb: () -> Unit)
}

interface PanelView {
    val name: String
    fun setName(name: String)
    fun setVisible(v: Boolean)
    fun setBorderVisible(v: Boolean)
    fun setOrigin(x: Int, y: Int)
    fun reshape(w: Int, h: Int)
    fun getWidth(): Int
    fun getHeight(): Int
    fun getParent(): PanelView?
    fun addChild(child: PanelView)
    fun removeChild(child: PanelView)
    fun findChild(name: String): UiControl?
    fun getChild(name: String): UiControl
}

interface LayoutStack {
    fun addPanel(panel: PanelView, animate: Boolean)
    fun removeChild(panel: PanelView)
}

interface LineEditor : UiControl {
    fun setCommitCallback(cb: () -> Unit)
}

interface FloaterBase {
    val name: String
    fun getChild(name: String): UiControl
    fun childSetEnabled(name: String, enabled: Boolean)
    fun setFocus(focus: Boolean)
    fun getRect(): Rect
    fun reshape(w: Int, h: Int)
    fun setCanDock(can: Boolean)
    fun setDocked(docked: Boolean, popup: Boolean)
    fun isDocked(): Boolean
    fun setUseTongue(view: Any?)
    fun isPhototools(): Boolean get() = name == PHOTOTOOLS_FLOATER
}

data class Rect(val x: Int, val y: Int, val width: Int, val height: Int)

interface EnvironmentManager {
    fun getSelectedEnvironment(): Int
    fun getEnvironmentDay(env: Int): EnvironmentDay?
    fun getEnvironmentFixedSky(env: Int): EnvironmentSky?
    fun getEnvironmentFixedWater(env: Int): EnvironmentWater?
    fun setSelectedEnvironment(env: Int)
    fun setManualEnvironment(env: Int, assetId: UUID)
    fun setSharedEnvironment()
    fun setEnvironmentChanged(cb: (Int, Int) -> Unit): (() -> Unit)
}

interface EnvironmentDay  { val assetId: UUID?; val name: String }
interface EnvironmentSky  { val assetId: UUID?; val name: String }
interface EnvironmentWater { val assetId: UUID?; val name: String }

const val ENV_LOCAL  = 0
const val ENV_REGION = 1
const val ENV_PARCEL = 2

interface InventoryModel {
    fun collectSettingsItems(): List<InventoryItem>
}

interface InventoryItem {
    val name: String
    val assetUuid: UUID
    val settingsType: SettingsType
}

enum class SettingsType { SKY, WATER, DAYCYCLE, OTHER }

interface FeatureManager {
    fun isFeatureAvailable(feature: String): Boolean
}

interface RlvHandler {
    fun isEnabled(): Boolean
    fun hasBehaviour(behaviour: String): Boolean
    fun setBehaviourCallback(cb: (String, String) -> Unit): (() -> Unit)
}

interface SavedSettings {
    fun getBool(key: String): Boolean
    fun setBool(key: String, value: Boolean)
    fun getF32(key: String): Float
    fun setF32(key: String, value: Float)
    fun getU32(key: String): UInt
    fun setU32(key: String, value: UInt)
    fun getString(key: String): String
    fun setString(key: String, value: String)
    fun getVector3(key: String): Triple<Float, Float, Float>
    fun setVector3(key: String, value: Triple<Float, Float, Float>)
    fun getControl(key: String): ControlVariable?
    fun applyToAll(func: (String, ControlVariable) -> Unit)
    fun connectCommitSignal(key: String, cb: (Any) -> Unit): (() -> Unit)
}

interface RegionInfo {
    val id: UUID
    fun avatarHoverHeightEnabled(): Boolean
    fun simulatorFeaturesReceived(): Boolean
    fun setSimulatorFeaturesReceivedCallback(cb: (UUID) -> Unit)
}

interface AgentInfo {
    fun getRegion(): RegionInfo?
    fun addRegionChangedCallback(cb: () -> Unit): (() -> Unit)
}

interface AvatarSelf {
    fun setHoverOffset(offset: Triple<Float, Float, Float>, commit: Boolean)
    fun isUsingServerBakes(): Boolean
    fun isValid(): Boolean
}

interface AvatarComplexityControls {
    fun setIndirectMaxNonImpostors()
    fun setIndirectMaxArc()
    fun getText(value: UInt): String
    fun setText(value: UInt, label: TextLabel?)
    fun updateMax(slider: SliderControl?, label: TextLabel?)
}

interface AppViewer {
    fun getSettingsFilename(group: String, name: String): String
}

interface FileSystem {
    fun isFile(path: String): Boolean
    fun remove(path: String)
    fun expandPath(domain: Int, filename: String): String
}

const val LL_PATH_USER_SETTINGS = 0
const val LL_PATH_APP_SETTINGS  = 1
const val MIN_HOVER_Z = -2.0f
const val MAX_HOVER_Z =  2.0f

class FloaterQuickPrefs(
    val floaterBase: FloaterBase,
    val environment: EnvironmentManager,
    val inventory: InventoryModel,
    val featureManager: FeatureManager,
    val rlvHandler: RlvHandler,
    val savedSettings: SavedSettings,
    val savedPerAccountSettings: SavedSettings,
    val agent: AgentInfo,
    val agentAvatar: AvatarSelf,
    val appViewer: AppViewer,
    val fs: FileSystem,
    val complexityControls: AvatarComplexityControls,
    val notificationUtil: NotificationAdder,
    val trans: TranslationProvider
) {
    private val controlsList = mutableMapOf<String, ControlEntry>()
    private val controlsOrder = ArrayDeque<String>()
    private val orderingSlots = mutableListOf<PanelView>()

    private var optionsStack: LayoutStack? = null
    private var selectedControl = ""

    private var wlPresetsCombo: ComboBox? = null
    private var waterPresetsCombo: ComboBox? = null
    private var dayCyclePresetsCombo: ComboBox? = null

    private var ctrlUseSSAO: CheckBoxControl? = null
    private var ctrlUseDoF: CheckBoxControl? = null
    private var ctrlShadowDetail: ComboBox? = null

    private var spinnerVignetteX: SpinnerControl? = null
    private var spinnerVignetteY: SpinnerControl? = null
    private var spinnerVignetteZ: SpinnerControl? = null
    private var sliderVignetteX: SliderControl? = null
    private var sliderVignetteY: SliderControl? = null
    private var sliderVignetteZ: SliderControl? = null

    private var sliderShadowSplitExponentY: SliderControl? = null
    private var spinnerShadowSplitExponentY: SpinnerControl? = null

    private var sliderShadowGaussianX: SliderControl? = null
    private var sliderShadowGaussianY: SliderControl? = null
    private var spinnerShadowGaussianX: SpinnerControl? = null
    private var spinnerShadowGaussianY: SpinnerControl? = null

    private var sliderSSAOEffectX: SliderControl? = null
    private var spinnerSSAOEffectX: SpinnerControl? = null

    private var avatarZOffsetSlider: SliderControl? = null
    private var maxComplexitySlider: SliderControl? = null
    private var maxComplexityLabel: TextLabel? = null

    private var controlLabelEdit: LineEditor? = null
    private var controlNameCombo: ComboBox? = null
    private var controlTypeCombo: ComboBox? = null
    private var controlIntegerCheckbox: CheckBoxControl? = null
    private var controlMinSpinner: SpinnerControl? = null
    private var controlMaxSpinner: SpinnerControl? = null
    private var controlIncrementSpinner: SpinnerControl? = null

    private var rlvBehaviourConnection: (() -> Unit)? = null
    private var envChangedConnection: (() -> Unit)? = null
    private var regionChangedConnection: (() -> Unit)? = null

    fun postBuild(): Boolean {
        if (floaterBase.isPhototools()) {
            ctrlUseSSAO      = floaterBase.getChild("UseSSAO") as? CheckBoxControl
            ctrlUseDoF       = floaterBase.getChild("UseDepthofField") as? CheckBoxControl
            ctrlShadowDetail = floaterBase.getChild("ShadowDetail") as? ComboBox

            spinnerVignetteX = floaterBase.getChild("VignetteSpinnerX") as? SpinnerControl
            spinnerVignetteY = floaterBase.getChild("VignetteSpinnerY") as? SpinnerControl
            spinnerVignetteZ = floaterBase.getChild("VignetteSpinnerZ") as? SpinnerControl
            sliderVignetteX  = floaterBase.getChild("VignetteSliderX")  as? SliderControl
            sliderVignetteY  = floaterBase.getChild("VignetteSliderY")  as? SliderControl
            sliderVignetteZ  = floaterBase.getChild("VignetteSliderZ")  as? SliderControl

            sliderShadowSplitExponentY  = floaterBase.getChild("SB_Shd_Clarity") as? SliderControl
            spinnerShadowSplitExponentY = floaterBase.getChild("S_Shd_Clarity")  as? SpinnerControl

            sliderShadowGaussianX  = floaterBase.getChild("SB_Shd_Soften") as? SliderControl
            sliderShadowGaussianY  = floaterBase.getChild("SB_AO_Soften")  as? SliderControl
            spinnerShadowGaussianX = floaterBase.getChild("S_Shd_Soften")  as? SpinnerControl
            spinnerShadowGaussianY = floaterBase.getChild("S_AO_Soften")   as? SpinnerControl

            sliderSSAOEffectX  = floaterBase.getChild("SB_Effect") as? SliderControl
            spinnerSSAOEffectX = floaterBase.getChild("S_Effect")  as? SpinnerControl

            refreshSettings()
        } else {
            avatarZOffsetSlider = floaterBase.getChild("HoverHeightSlider") as? SliderControl
            avatarZOffsetSlider?.setMinValue(MIN_HOVER_Z)
            avatarZOffsetSlider?.setMaxValue(MAX_HOVER_Z)

            maxComplexitySlider = floaterBase.getChild("IndirectMaxComplexity") as? SliderControl
            maxComplexityLabel  = floaterBase.getChild("IndirectMaxComplexityText") as? TextLabel
        }

        wlPresetsCombo        = floaterBase.getChild("WLPresetsCombo")    as? ComboBox
        waterPresetsCombo     = floaterBase.getChild("WaterPresetsCombo") as? ComboBox
        dayCyclePresetsCombo  = floaterBase.getChild("DCPresetsCombo")    as? ComboBox

        initCallbacks()

        if (rlvHandler.isEnabled()) {
            enableWindlightButtons(
                !rlvHandler.hasBehaviour("setenv") && !rlvHandler.hasBehaviour("setsphere")
            )
        }

        if (floaterBase.isPhototools()) return true

        optionsStack = TODO("get options_stack LayoutStack child from floater")

        loadSavedSettingsFromFile(getSettingsPath(false))

        controlLabelEdit         = floaterBase.getChild("label_edit")                 as? LineEditor
        controlNameCombo         = floaterBase.getChild("control_name_combo")          as? ComboBox
        controlTypeCombo         = floaterBase.getChild("control_type_combo_box")      as? ComboBox
        controlIntegerCheckbox   = floaterBase.getChild("control_integer_checkbox")    as? CheckBoxControl
        controlMinSpinner        = floaterBase.getChild("control_min_edit")            as? SpinnerControl
        controlMaxSpinner        = floaterBase.getChild("control_max_edit")            as? SpinnerControl
        controlIncrementSpinner  = floaterBase.getChild("control_increment_edit")      as? SpinnerControl

        controlLabelEdit?.setCommitCallback { onValuesChanged() }
        controlNameCombo?.setCommitCallback { onValuesChanged() }
        controlTypeCombo?.setCommitCallback { onValuesChanged() }
        controlIntegerCheckbox?.setCommitCallback { onValuesChanged() }
        controlMinSpinner?.setCommitCallback { onValuesChanged() }
        controlMaxSpinner?.setCommitCallback { onValuesChanged() }
        controlIncrementSpinner?.setCommitCallback { onValuesChanged() }

        (floaterBase.getChild("move_up_button") as? UiControl)
            ?.let { TODO("set commit callback for move_up_button") }
        (floaterBase.getChild("move_down_button") as? UiControl)
            ?.let { TODO("set commit callback for move_down_button") }
        (floaterBase.getChild("add_new_button") as? UiControl)
            ?.let { TODO("set commit callback for add_new_button") }

        val visitor = { name: String, control: ControlVariable ->
            if (!control.isHiddenFromSettingsEditor() && !name.startsWith("floater_")) {
                controlNameCombo?.addSimpleElement(name)
            }
        }
        savedSettings.applyToAll(visitor)
        savedPerAccountSettings.applyToAll(visitor)
        controlNameCombo?.sortByName()

        updateAvatarZOffsetEditEnabled()
        onRegionChanged()

        return true
    }

    fun onOpen(key: Any?) {
        loadPresets()
        setSelectedEnvironment()

        complexityControls.setIndirectMaxNonImpostors()

        if (floaterBase.isPhototools()) return

        complexityControls.setIndirectMaxArc()
        complexityControls.setText(savedSettings.getU32("RenderAvatarMaxComplexity"), maxComplexityLabel)

        savedSettings.setBool("QuickPrefsEditMode", false)

        for ((name, entry) in controlsList) {
            val widget = entry.widget ?: continue
            var variable = widget.getControlVariable()
                ?: savedSettings.getControl(name)
                ?: savedPerAccountSettings.getControl(name)
            variable?.let { v ->
                var value = v.getValue()
                if (entry.type == ControlType.Radio && value is Boolean) {
                    value = if (value) 1 else 0
                }
                widget.setValue(value)
            }
        }

        dockToToolbarButton()
    }

    private fun initCallbacks() {
        waterPresetsCombo?.setCommitCallback { onChangeWaterPreset() }
        wlPresetsCombo?.setCommitCallback { onChangeSkyPreset() }
        dayCyclePresetsCombo?.setCommitCallback { onChangeDayCyclePreset() }

        (floaterBase.getChild("WLPrevPreset") as? UiControl)?.let {
            TODO("wire WLPrevPreset commit callback to onClickSkyPrev")
        }
        (floaterBase.getChild("WLNextPreset") as? UiControl)?.let {
            TODO("wire WLNextPreset commit callback to onClickSkyNext")
        }
        (floaterBase.getChild("WWPrevPreset") as? UiControl)?.let {
            TODO("wire WWPrevPreset commit callback to onClickWaterPrev")
        }
        (floaterBase.getChild("WWNextPreset") as? UiControl)?.let {
            TODO("wire WWNextPreset commit callback to onClickWaterNext")
        }
        (floaterBase.getChild("DCPrevPreset") as? UiControl)?.let {
            TODO("wire DCPrevPreset commit callback to onClickDayCyclePrev")
        }
        (floaterBase.getChild("DCNextPreset") as? UiControl)?.let {
            TODO("wire DCNextPreset commit callback to onClickDayCycleNext")
        }
        (floaterBase.getChild("ResetToRegionDefault") as? UiControl)?.let {
            TODO("wire ResetToRegionDefault commit callback to onClickResetToRegionDefault")
        }

        if (floaterBase.isPhototools()) {
            savedSettings.getControl("RenderObjectBump")?.connectSignal { refreshSettings() }
            savedSettings.getControl("RenderDeferred")?.connectSignal { refreshSettings() }
            savedSettings.getControl("RenderShadowDetail")?.connectSignal { refreshSettings() }
            savedSettings.getControl("FSRenderVignette")?.connectSignal { refreshSettings() }
            savedSettings.getControl("RenderShadowSplitExponent")?.connectSignal { refreshSettings() }
            savedSettings.getControl("RenderShadowGaussian")?.connectSignal { refreshSettings() }
            savedSettings.getControl("RenderSSAOEffect")?.connectSignal { refreshSettings() }

            spinnerVignetteX?.setCommitCallback { onChangeVignetteSpinnerX() }
            sliderVignetteX?.setCommitCallback  { onChangeVignetteX() }
            spinnerVignetteY?.setCommitCallback { onChangeVignetteSpinnerY() }
            sliderVignetteY?.setCommitCallback  { onChangeVignetteY() }
            spinnerVignetteZ?.setCommitCallback { onChangeVignetteSpinnerZ() }
            sliderVignetteZ?.setCommitCallback  { onChangeVignetteZ() }

            sliderShadowSplitExponentY?.setCommitCallback  { onChangeRenderShadowSplitExponentSlider() }
            spinnerShadowSplitExponentY?.setCommitCallback { onChangeRenderShadowSplitExponentSpinner() }

            sliderShadowGaussianX?.setCommitCallback  { onChangeRenderShadowGaussianSlider() }
            sliderShadowGaussianY?.setCommitCallback  { onChangeRenderShadowGaussianSlider() }
            spinnerShadowGaussianX?.setCommitCallback { onChangeRenderShadowGaussianSpinner() }
            spinnerShadowGaussianY?.setCommitCallback { onChangeRenderShadowGaussianSpinner() }

            sliderSSAOEffectX?.setCommitCallback  { onChangeRenderSSAOEffectSlider() }
            spinnerSSAOEffectX?.setCommitCallback { onChangeRenderSSAOEffectSpinner() }
        } else {
            (floaterBase.getChild("Restore_Btn") as? UiControl)?.let {
                TODO("wire Restore_Btn commit callback to onClickRestoreDefaults")
            }
            savedSettings.getControl("QuickPrefsEditMode")?.connectSignal { onEditModeChanged() }

            avatarZOffsetSlider?.setSliderMouseUpCallback      { onAvatarZOffsetFinalCommit() }
            avatarZOffsetSlider?.setSliderEditorCommitCallback { onAvatarZOffsetFinalCommit() }
            avatarZOffsetSlider?.setCommitCallback             { onAvatarZOffsetSliderMoved() }

            maxComplexitySlider?.setCommitCallback { updateMaxComplexity() }
            savedSettings.connectCommitSignal("RenderAvatarMaxComplexity") { v ->
                updateMaxComplexityLabel(v)
            }

            syncAvatarZOffsetFromPreferenceSetting()
            savedPerAccountSettings.getControl("AvatarHoverOffsetZ")
                ?.connectSignal { syncAvatarZOffsetFromPreferenceSetting() }

            if (regionChangedConnection == null) {
                regionChangedConnection = agent.addRegionChangedCallback { onRegionChanged() }
            }
        }

        rlvBehaviourConnection = rlvHandler.setBehaviourCallback { behaviour, type ->
            updateRlvRestrictions(behaviour, type)
        }
        savedSettings.connectCommitSignal("IndirectMaxNonImpostors") { v -> updateMaxNonImpostors(v) }

        envChangedConnection = environment.setEnvironmentChanged { _, _ -> setSelectedEnvironment() }
    }

    private fun loadDayCyclePresets(daycycleMap: Map<String, UUID>) {
        dayCyclePresetsCombo?.clearAll()
        dayCyclePresetsCombo?.addItem(trans.getString("QP_WL_Region_Default"), PRESET_NAME_REGION_DEFAULT, enabled = false)
        dayCyclePresetsCombo?.addItem(trans.getString("QP_WL_None"), PRESET_NAME_NONE, enabled = false)
        dayCyclePresetsCombo?.addSeparator()
        for ((name, id) in daycycleMap) {
            if (name.isNotEmpty()) dayCyclePresetsCombo?.addItem(name, id)
        }
    }

    private fun loadSkyPresets(skyMap: Map<String, UUID>) {
        wlPresetsCombo?.clearAll()
        wlPresetsCombo?.addItem(trans.getString("QP_WL_Region_Default"), PRESET_NAME_REGION_DEFAULT, enabled = false)
        wlPresetsCombo?.addItem(trans.getString("QP_WL_Day_Cycle_Based"), PRESET_NAME_DAY_CYCLE, enabled = false)
        wlPresetsCombo?.addSeparator()
        for ((name, id) in skyMap) {
            if (name.isNotEmpty()) wlPresetsCombo?.addItem(name, id)
        }
    }

    private fun loadWaterPresets(waterMap: Map<String, UUID>) {
        waterPresetsCombo?.clearAll()
        waterPresetsCombo?.addItem(trans.getString("QP_WL_Region_Default"), PRESET_NAME_REGION_DEFAULT, enabled = false)
        waterPresetsCombo?.addItem(trans.getString("QP_WL_Day_Cycle_Based"), PRESET_NAME_DAY_CYCLE, enabled = false)
        waterPresetsCombo?.addSeparator()
        for ((name, id) in waterMap) {
            if (name.isNotEmpty()) waterPresetsCombo?.addItem(name, id)
        }
    }

    private fun loadPresets() {
        val items = inventory.collectSettingsItems()
        val skyMap     = mutableMapOf<String, UUID>()
        val waterMap   = mutableMapOf<String, UUID>()
        val daycycleMap = mutableMapOf<String, UUID>()
        for (item in items) {
            when (item.settingsType) {
                SettingsType.SKY      -> skyMap[item.name] = item.assetUuid
                SettingsType.WATER    -> waterMap[item.name] = item.assetUuid
                SettingsType.DAYCYCLE -> daycycleMap[item.name] = item.assetUuid
                else -> Unit
            }
        }
        loadWaterPresets(waterMap)
        loadSkyPresets(skyMap)
        loadDayCyclePresets(daycycleMap)
    }

    private fun setDefaultPresetsEnabled(enabled: Boolean) {
        wlPresetsCombo?.getItemByValue(PRESET_NAME_REGION_DEFAULT)?.let { it.isEnabled = enabled }
        wlPresetsCombo?.getItemByValue(PRESET_NAME_DAY_CYCLE)?.let { it.isEnabled = enabled }
        waterPresetsCombo?.getItemByValue(PRESET_NAME_REGION_DEFAULT)?.let { it.isEnabled = enabled }
        waterPresetsCombo?.getItemByValue(PRESET_NAME_DAY_CYCLE)?.let { it.isEnabled = enabled }
        dayCyclePresetsCombo?.getItemByValue(PRESET_NAME_REGION_DEFAULT)?.let { it.isEnabled = enabled }
        dayCyclePresetsCombo?.getItemByValue(PRESET_NAME_NONE)?.let { it.isEnabled = enabled }
    }

    fun setSelectedEnvironment() {
        setDefaultPresetsEnabled(true)
        wlPresetsCombo?.selectByValue(PRESET_NAME_REGION_DEFAULT)
        waterPresetsCombo?.selectByValue(PRESET_NAME_REGION_DEFAULT)
        dayCyclePresetsCombo?.selectByValue(PRESET_NAME_REGION_DEFAULT)

        if (environment.getSelectedEnvironment() == ENV_LOCAL) {
            val day = environment.getEnvironmentDay(ENV_LOCAL)
            if (day != null && day.assetId != null && day.assetId != UUID(0, 0)) {
                dayCyclePresetsCombo?.selectByValue(day.assetId)
                wlPresetsCombo?.selectByValue(PRESET_NAME_DAY_CYCLE)
                waterPresetsCombo?.selectByValue(PRESET_NAME_DAY_CYCLE)
            } else {
                dayCyclePresetsCombo?.selectByValue(PRESET_NAME_NONE)
            }

            environment.getEnvironmentFixedSky(ENV_LOCAL)?.let { sky ->
                if (sky.assetId != null && sky.assetId != UUID(0, 0)) {
                    wlPresetsCombo?.selectByValue(sky.assetId)
                }
            }

            environment.getEnvironmentFixedWater(ENV_LOCAL)?.let { water ->
                if (water.assetId != null && water.assetId != UUID(0, 0)) {
                    waterPresetsCombo?.selectByValue(water.assetId)
                }
            }
        }

        setDefaultPresetsEnabled(false)
    }

    fun setSelectedSky(presetName: String) {
        wlPresetsCombo?.selectByValue(presetName)
    }

    fun setSelectedWater(presetName: String) {
        waterPresetsCombo?.selectByValue(presetName)
    }

    fun setSelectedDayCycle(presetName: String) {
        dayCyclePresetsCombo?.selectByValue(presetName)
        wlPresetsCombo?.selectByValue(PRESET_NAME_DAY_CYCLE)
        waterPresetsCombo?.selectByValue(PRESET_NAME_DAY_CYCLE)
    }

    private fun isValidPreset(value: Any?): Boolean {
        return when (value) {
            is UUID   -> value != UUID(0, 0)
            is String -> value.isNotEmpty() &&
                         value != PRESET_NAME_REGION_DEFAULT &&
                         value != PRESET_NAME_DAY_CYCLE &&
                         value != PRESET_NAME_NONE
            else      -> false
        }
    }

    private fun stepComboBox(ctrl: ComboBox, forward: Boolean) {
        val increment = if (forward) 1 else -1
        val lastItem = ctrl.getItemCount() - 1
        var currentId = ctrl.getCurrentIndex()
        val startId = currentId
        do {
            currentId += increment
            if (currentId < 0) currentId = lastItem
            else if (currentId > lastItem) currentId = 0
            ctrl.setCurrentByIndex(currentId)
        } while (!isValidPreset(ctrl.getSelectedValue()) && currentId != startId)
    }

    private fun selectSkyPreset(preset: Any?) {
        environment.setSelectedEnvironment(ENV_LOCAL)
        if (preset is UUID) {
            environment.setManualEnvironment(ENV_LOCAL, preset)
        }
    }

    private fun selectWaterPreset(preset: Any?) {
        environment.setSelectedEnvironment(ENV_LOCAL)
        if (preset is UUID) {
            environment.setManualEnvironment(ENV_LOCAL, preset)
        }
    }

    private fun selectDayCyclePreset(preset: Any?) {
        environment.setSelectedEnvironment(ENV_LOCAL)
        if (preset is UUID) {
            environment.setManualEnvironment(ENV_LOCAL, preset)
        }
    }

    private fun onChangeWaterPreset() {
        if (!isValidPreset(waterPresetsCombo?.getSelectedValue())) {
            stepComboBox(waterPresetsCombo!!, true)
        }
        if (isValidPreset(waterPresetsCombo?.getSelectedValue())) {
            selectWaterPreset(waterPresetsCombo?.getSelectedValue())
        } else {
            notificationUtil.add("NoValidEnvSettingFound")
        }
    }

    private fun onChangeSkyPreset() {
        if (!isValidPreset(wlPresetsCombo?.getSelectedValue())) {
            stepComboBox(wlPresetsCombo!!, true)
        }
        if (isValidPreset(wlPresetsCombo?.getSelectedValue())) {
            selectSkyPreset(wlPresetsCombo?.getSelectedValue())
        } else {
            notificationUtil.add("NoValidEnvSettingFound")
        }
    }

    private fun onChangeDayCyclePreset() {
        if (!isValidPreset(dayCyclePresetsCombo?.getSelectedValue())) {
            stepComboBox(dayCyclePresetsCombo!!, true)
        }
        if (isValidPreset(dayCyclePresetsCombo?.getSelectedValue())) {
            selectDayCyclePreset(dayCyclePresetsCombo?.getSelectedValue())
        } else {
            notificationUtil.add("NoValidEnvSettingFound")
        }
    }

    private fun onClickWaterPrev() {
        stepComboBox(waterPresetsCombo!!, false)
        selectWaterPreset(waterPresetsCombo?.getSelectedValue())
    }

    private fun onClickWaterNext() {
        stepComboBox(waterPresetsCombo!!, true)
        selectWaterPreset(waterPresetsCombo?.getSelectedValue())
    }

    private fun onClickSkyPrev() {
        stepComboBox(wlPresetsCombo!!, false)
        selectSkyPreset(wlPresetsCombo?.getSelectedValue())
    }

    private fun onClickSkyNext() {
        stepComboBox(wlPresetsCombo!!, true)
        selectSkyPreset(wlPresetsCombo?.getSelectedValue())
    }

    private fun onClickDayCyclePrev() {
        stepComboBox(dayCyclePresetsCombo!!, false)
        selectDayCyclePreset(dayCyclePresetsCombo?.getSelectedValue())
    }

    private fun onClickDayCycleNext() {
        stepComboBox(dayCyclePresetsCombo!!, true)
        selectDayCyclePreset(dayCyclePresetsCombo?.getSelectedValue())
    }

    private fun onClickResetToRegionDefault() {
        wlPresetsCombo?.selectByValue(PRESET_NAME_REGION_DEFAULT)
        waterPresetsCombo?.selectByValue(PRESET_NAME_REGION_DEFAULT)
        environment.setSharedEnvironment()
    }

    fun refreshSettings() {
        val skyEnabled = featureManager.isFeatureAvailable("RenderDeferredSSAO")
        ctrlUseSSAO?.enabled = skyEnabled
        ctrlUseDoF?.enabled  = skyEnabled

        val shadowEnabled = skyEnabled && featureManager.isFeatureAvailable("RenderShadowDetail")
        ctrlShadowDetail?.enabled = shadowEnabled

        if (!featureManager.isFeatureAvailable("RenderDeferredSSAO")) {
            ctrlUseSSAO?.enabled = false
            ctrlUseSSAO?.setValue(false)
        }
        if (!featureManager.isFeatureAvailable("RenderShadowDetail")) {
            ctrlShadowDetail?.enabled = false
            ctrlShadowDetail?.setValue(0)
        }

        if (floaterBase.isPhototools()) {
            val vignette = savedSettings.getVector3("FSRenderVignette")
            spinnerVignetteX?.setValue(vignette.first)
            spinnerVignetteY?.setValue(vignette.second)
            spinnerVignetteZ?.setValue(vignette.third)
            sliderVignetteX?.setValue(vignette.first)
            sliderVignetteY?.setValue(vignette.second)
            sliderVignetteZ?.setValue(vignette.third)

            val splitExp = savedSettings.getVector3("RenderShadowSplitExponent")
            spinnerShadowSplitExponentY?.setValue(splitExp.second)
            sliderShadowSplitExponentY?.setValue(splitExp.second)

            val gaussian = savedSettings.getVector3("RenderShadowGaussian")
            spinnerShadowGaussianX?.setValue(gaussian.first)
            spinnerShadowGaussianY?.setValue(gaussian.second)
            sliderShadowGaussianX?.setValue(gaussian.first)
            sliderShadowGaussianY?.setValue(gaussian.second)

            val ssao = savedSettings.getVector3("RenderSSAOEffect")
            spinnerSSAOEffectX?.setValue(ssao.first)
            sliderSSAOEffectX?.setValue(ssao.first)
        }
    }

    private fun updateRlvRestrictions(behaviour: String, type: String) {
        if (behaviour == "setenv" || behaviour == "setsphere") {
            enableWindlightButtons(type != "add")
        }
    }

    private fun enableWindlightButtons(enable: Boolean) {
        floaterBase.childSetEnabled("WLPresetsCombo",      enable)
        floaterBase.childSetEnabled("WLPrevPreset",        enable)
        floaterBase.childSetEnabled("WLNextPreset",        enable)
        floaterBase.childSetEnabled("WaterPresetsCombo",   enable)
        floaterBase.childSetEnabled("WWPrevPreset",        enable)
        floaterBase.childSetEnabled("WWNextPreset",        enable)
        floaterBase.childSetEnabled("ResetToRegionDefault",enable)
        floaterBase.childSetEnabled("DCPresetsCombo",      enable)
        floaterBase.childSetEnabled("DCPrevPreset",        enable)
        floaterBase.childSetEnabled("DCNextPreset",        enable)
        floaterBase.childSetEnabled("btn_personal_lighting",enable)

        if (floaterBase.isPhototools()) {
            floaterBase.childSetEnabled("Sunrise",                  enable)
            floaterBase.childSetEnabled("Noon",                     enable)
            floaterBase.childSetEnabled("Sunset",                   enable)
            floaterBase.childSetEnabled("Midnight",                 enable)
            floaterBase.childSetEnabled("Revert to Region Default", enable)
            floaterBase.childSetEnabled("new_sky_preset",           enable)
            floaterBase.childSetEnabled("edit_sky_preset",          enable)
            floaterBase.childSetEnabled("new_water_preset",         enable)
            floaterBase.childSetEnabled("edit_water_preset",        enable)
            floaterBase.childSetEnabled("PauseClouds",              enable)
        }
    }

    private fun getSettingsPath(saveMode: Boolean): String {
        val filename = appViewer.getSettingsFilename("Default", "QuickPreferences")
        val userPath = fs.expandPath(LL_PATH_USER_SETTINGS, filename)
        if (!saveMode && !fs.isFile(userPath)) {
            return fs.expandPath(LL_PATH_APP_SETTINGS, filename)
        }
        return userPath
    }

    private fun loadSavedSettingsFromFile(settingsPath: String) {
        val xml = parseQuickPrefsXml(settingsPath) ?: return
        var saveSettings = false
        for (entry in xml.entries) {
            val label = if (entry.translationId != null) {
                trans.getString(entry.translationId).ifEmpty { entry.label }
            } else {
                entry.label
            }

            if (entry.controlName != "RenderAvatarMaxVisible") {
                addControl(entry.controlName, label, null,
                    ControlType.entries[entry.controlType.toInt()],
                    entry.integer, entry.minValue, entry.maxValue, entry.increment)
                controlsOrder.addLast(entry.controlName)
            } else {
                addControl("IndirectMaxNonImpostors", label, null,
                    ControlType.entries[entry.controlType.toInt()],
                    entry.integer, 1f, 66f, 1f)
                controlsOrder.addLast("IndirectMaxNonImpostors")
                saveSettings = true
            }
        }
        if (saveSettings) onEditModeChanged()
    }

    private fun parseQuickPrefsXml(path: String): QuickPrefsXml? {
        TODO("APR: use JVM XML parser to read quick_preferences.xml at $path")
    }

    fun updateControl(controlName: String, entry: ControlEntry) {
        (entry.panel as? PanelView)?.setName(controlName)

        val typeWidgetNames = mapOf(
            ControlType.Checkbox to "option_checkbox_control",
            ControlType.Text     to "option_text_control",
            ControlType.Spinner  to "option_spinner_control",
            ControlType.Slider   to "option_slider_control",
            ControlType.Radio    to "option_radio_control",
            ControlType.Color3   to "option_color3_control",
            ControlType.Color4   to "option_color4_control"
        )

        val panel = entry.panel as? PanelView ?: return

        for ((type, widgetName) in typeWidgetNames) {
            if (entry.type != type) {
                panel.findChild(widgetName)?.let { w ->
                    w.controlName = "QuickPrefsEditMode"
                    w.visible = false
                    w.enabled = false
                }
            }
        }

        val widget = panel.getChild(typeWidgetNames[entry.type]!!) as UiControl
        entry.widget = widget

        var decimals = 3

        widget.controlName = controlName
        widget.visible = true
        widget.enabled = true

        val alphaWidget = panel.getChild("option_color_alpha_control") as? UiControl
        alphaWidget?.visible = false

        if (entry.increment == 0.0f) {
            entry.increment = when (entry.type) {
                ControlType.Slider  -> (entry.maxValue - entry.minValue) / 100.0f
                ControlType.Spinner -> (entry.maxValue - entry.minValue) / 20.0f
                else -> 0.0f
            }
        }

        if (entry.integer) {
            entry.minValue  = Math.round(entry.minValue).toFloat()
            entry.maxValue  = Math.round(entry.maxValue).toFloat()
            entry.increment = Math.round(entry.increment).toFloat().coerceAtLeast(1f)
            decimals = 0
        }

        when (entry.type) {
            ControlType.Spinner -> {
                (widget as? SpinnerControl)?.let {
                    it.setPrecision(decimals)
                    it.setMinValue(entry.minValue)
                    it.setMaxValue(entry.maxValue)
                    it.setIncrement(entry.increment)
                }
            }
            ControlType.Slider -> {
                (widget as? SliderControl)?.let {
                    it.setPrecision(decimals)
                    it.setMinValue(entry.minValue)
                    it.setMaxValue(entry.maxValue)
                    it.setIncrement(entry.increment)
                }
            }
            ControlType.Color4 -> {
                alphaWidget?.visible = true
            }
            else -> Unit
        }

        var labelTextbox = entry.labelTextbox
        if (labelTextbox == null) {
            labelTextbox = panel.getChild("option_label") as? TextLabel
            labelTextbox?.setDoubleClickCallback { ctrl -> onDoubleClickLabel(ctrl, panel) }
            labelTextbox?.setMouseUpCallback     { ctrl -> onClickLabel(ctrl, panel) }

            val removeButton = panel.getChild("remove_button") as? UiControl
            TODO("wire remove_button commit callback to onRemoveClicked(ctrl, panel)")

            entry.labelTextbox = labelTextbox
        }
        labelTextbox?.setValue("${entry.label}:")

        val variable = savedSettings.getControl(controlName)
            ?: savedPerAccountSettings.getControl(controlName)

        if (variable != null) {
            var value = variable.getValue()
            if (entry.type == ControlType.Radio && value is Boolean) {
                value = if (value) 1 else 0
            }
            widget.setValue(value)
            entry.signalConnection?.invoke()

            if (entry.type == ControlType.Radio) {
                entry.signalConnection = variable.connectSignalReturningDisconnect { v ->
                    val mapped = if (v is Boolean) (if (v) 1 else 0) else v
                    widget.setValue(mapped)
                }
            }
        }
    }

    fun addControl(
        controlName: String,
        controlLabel: String,
        slot: PanelView?,
        type: ControlType = ControlType.Radio,
        integer: Boolean = false,
        minValue: Float = -1000000.0f,
        maxValue: Float = 1000000.0f,
        increment: Float = 0.0f
    ): UiControl? {
        val panel: PanelView = TODO("inflate panel_quickprefs_item.xml layout panel")

        val safeMax = if (maxValue < minValue) minValue else maxValue
        val safeIncrement = if (increment < 0.0f) 0.0f else increment

        val innerPanel: PanelView = panel.getChild("option_ordering_panel") as? PanelView
            ?: return null

        val entry = ControlEntry(
            panel = innerPanel,
            widget = null,
            labelTextbox = null,
            label = controlLabel,
            type = type,
            integer = integer,
            minValue = minValue,
            maxValue = safeMax,
            increment = safeIncrement
        )

        updateControl(controlName, entry)
        controlsList[controlName] = entry

        if (slot != null) {
            slot.addChild(innerPanel)
            innerPanel.setOrigin(0, 0)
            innerPanel.reshape(slot.getWidth(), slot.getHeight())
            TODO("release outer layout panel from memory")
        } else {
            optionsStack?.addPanel(panel, false)
            orderingSlots.add(panel)
            floaterBase.reshape(
                floaterBase.getRect().width,
                floaterBase.getRect().height + panel.getHeight()
            )
            panel.setVisible(true)
        }

        innerPanel.setBorderVisible(false)
        return entry.widget
    }

    fun removeControl(controlName: String, removeSlot: Boolean = true) {
        val entry = controlsList.remove(controlName) ?: return
        val panel = entry.panel as? PanelView ?: return
        val height = panel.getHeight()

        entry.signalConnection?.invoke()

        val slot = panel.getParent()
        slot?.removeChild(panel)
        TODO("free panel from memory")

        if (removeSlot && slot != null) {
            orderingSlots.remove(slot)
            optionsStack?.removeChild(slot)
            floaterBase.reshape(
                floaterBase.getRect().width,
                floaterBase.getRect().height - height
            )
        }
    }

    private fun selectControl(controlName: String) {
        if (selectedControl.isNotEmpty() && hasControl(selectedControl)) {
            (controlsList[selectedControl]?.panel as? PanelView)?.setBorderVisible(false)
        }

        selectedControl = controlName
        savedSettings.setString("QuickPrefsSelectedControl", controlName)

        if (selectedControl.isNotEmpty() && !hasControl(selectedControl)) {
            selectedControl = ""
            return
        }

        if (!savedSettings.getBool("QuickPrefsEditMode")) return

        controlNameCombo?.setCurrentByIndex(0)
        var enableFloatingPoint = false

        if (selectedControl.isNotEmpty()) {
            (controlsList[selectedControl]?.panel as? PanelView)?.setBorderVisible(true)
            val e = controlsList[selectedControl]!!
            controlLabelEdit?.setValue(e.label)
            controlNameCombo?.selectByValue(selectedControl)
            controlTypeCombo?.setValue(e.type.ordinal)
            controlIntegerCheckbox?.setValue(e.integer)
            controlMinSpinner?.setValue(e.minValue)
            controlMaxSpinner?.setValue(e.maxValue)
            controlIncrementSpinner?.setValue(e.increment)

            when (e.type) {
                ControlType.Spinner, ControlType.Slider -> {
                    enableFloatingPoint = true
                    controlIncrementSpinner?.setIncrement(0.1f)
                    val decimals = if (e.integer) 0 else 3
                    if (e.integer) controlIncrementSpinner?.setIncrement(1.0f)
                    controlMinSpinner?.setPrecision(decimals)
                    controlMaxSpinner?.setPrecision(decimals)
                    controlIncrementSpinner?.setPrecision(decimals)
                }
                else -> Unit
            }
        }

        controlMinSpinner?.enabled = enableFloatingPoint
        controlMaxSpinner?.enabled = enableFloatingPoint
        controlIntegerCheckbox?.enabled = enableFloatingPoint
        controlIncrementSpinner?.enabled = enableFloatingPoint
    }

    private fun onClickLabel(ctrl: UiControl, panel: PanelView) {
        if (!savedSettings.getBool("QuickPrefsEditMode")) return
        selectControl(panel.name)
    }

    private fun onDoubleClickLabel(ctrl: UiControl, panel: PanelView) {
        val editMode = !savedSettings.getBool("QuickPrefsEditMode")
        savedSettings.setBool("QuickPrefsEditMode", editMode)
        if (editMode) selectControl(panel.name)
    }

    private fun onEditModeChanged() {
        if (savedSettings.getBool("QuickPrefsEditMode")) return

        selectControl("")

        val entries = controlsOrder.mapNotNull { name ->
            controlsList[name]?.let { e ->
                QuickPrefsXmlEntry(
                    controlName  = name,
                    label        = e.label,
                    translationId = null,
                    controlType  = e.type.ordinal.toUInt(),
                    integer      = e.integer,
                    minValue     = e.minValue,
                    maxValue     = e.maxValue,
                    increment    = e.increment
                )
            }
        }

        val xml = QuickPrefsXml(entries.toMutableList())
        val settingsPath = getSettingsPath(true)
        writeQuickPrefsXml(xml, settingsPath)
    }

    private fun writeQuickPrefsXml(xml: QuickPrefsXml, path: String) {
        TODO("APR: use JVM XML serializer to write quick preferences to $path")
    }

    private fun onValuesChanged() {
        if (!savedSettings.getBool("QuickPrefsEditMode")) return
        if (selectedControl.isEmpty()) return

        val oldName = selectedControl
        val newName = controlNameCombo?.getSelectedValue()?.toString() ?: return

        if (newName.isNotEmpty() && oldName != newName) {
            if (controlsList.containsKey(newName)) {
                notificationUtil.add("QuickPrefsDuplicateControl")
                return
            }

            val oldParams = controlsList[oldName]!!.copy()
            selectControl("")

            val idx = controlsOrder.indexOf(oldName)
            if (idx >= 0) { controlsOrder.removeAt(idx); controlsOrder.add(idx, newName) }

            val slot = (oldParams.panel as? PanelView)?.getParent()
            removeControl(oldName, false)
            addControl(newName, newName, slot)
            selectControl(newName)

            controlsList[selectedControl]?.label = oldParams.label

            val variable = savedSettings.getControl(selectedControl)
                ?: savedPerAccountSettings.getControl(selectedControl)

            if (variable != null && hasControl(selectedControl)) {
                val value = (variable.getValue() as? Number)?.toFloat() ?: 0f
                val minValue = if (value < 0f) value * 2f else 0f
                val maxValue = if (value > 0f) value * 2f else 1f

                val type = guessControlType(variable)
                val increment = calculateIncrement(type, minValue, maxValue)

                controlsList[selectedControl]?.let {
                    it.minValue  = minValue
                    it.maxValue  = maxValue
                    it.increment = increment
                    it.type      = type
                    it.widget?.setValue(variable.getValue())
                }
            }
            updateControl(selectedControl, controlsList[selectedControl]!!)
        } else if (hasControl(selectedControl)) {
            controlsList[selectedControl]?.let { e ->
                e.label      = controlLabelEdit?.getValue()?.toString() ?: e.label
                e.type       = ControlType.entries[controlTypeCombo?.getValue()?.toString()?.toIntOrNull() ?: 0]
                e.integer    = controlIntegerCheckbox?.getValue() as? Boolean ?: false
                e.minValue   = controlMinSpinner?.getValue()?.toString()?.toFloatOrNull() ?: e.minValue
                e.maxValue   = controlMaxSpinner?.getValue()?.toString()?.toFloatOrNull() ?: e.maxValue
                e.increment  = controlIncrementSpinner?.getValue()?.toString()?.toFloatOrNull() ?: e.increment
                updateControl(selectedControl, e)
            }
        }
        selectControl(selectedControl)
    }

    private fun guessControlType(variable: ControlVariable): ControlType {
        return when (variable.getTypeName()) {
            "bool"   -> ControlType.Radio
            "color3" -> ControlType.Color3
            "color4" -> ControlType.Color4
            "u32"    -> ControlType.Slider
            "s32"    -> ControlType.Slider
            "f32"    -> ControlType.Slider
            else     -> ControlType.Text
        }
    }

    private fun calculateIncrement(type: ControlType, min: Float, max: Float): Float {
        val raw = when (type) {
            ControlType.Slider  -> (max - min) / 100.0f
            ControlType.Spinner -> (max - min) / 20.0f
            else -> 0.1f
        }
        return raw.coerceAtLeast(0.1f)
    }

    private fun onAddNewClicked() {
        val count = controlsList.size
        val newName = "NewControl$count"
        addControl(newName, newName)
        controlsOrder.addLast(newName)
        selectControl(newName)
    }

    private fun onRemoveClicked(ctrl: UiControl, panel: PanelView) {
        selectControl("")
        controlsOrder.remove(panel.name)
        removeControl(panel.name)
        floaterBase.setFocus(true)
    }

    private fun onAlphaChanged(ctrl: UiControl, colorSwatch: UiControl) {
        TODO("read alpha value from ctrl, update color swatch control variable")
    }

    private fun swapControls(control1: String, control2: String) {
        val entry1 = controlsList[control1] ?: return
        val entry2 = controlsList[control2] ?: return
        val slot1 = (entry1.panel as? PanelView)?.getParent()
        val slot2 = (entry2.panel as? PanelView)?.getParent()
        slot1?.addChild(entry2.panel as PanelView)
        slot2?.addChild(entry1.panel as PanelView)
    }

    private fun onMoveUpClicked() {
        val idx = controlsOrder.indexOf(selectedControl)
        if (idx <= 0) return
        val prev = controlsOrder[idx - 1]
        controlsOrder[idx - 1] = selectedControl
        controlsOrder[idx] = prev
        swapControls(selectedControl, prev)
    }

    private fun onMoveDownClicked() {
        val idx = controlsOrder.indexOf(selectedControl)
        if (idx < 0 || idx >= controlsOrder.size - 1) return
        val next = controlsOrder[idx + 1]
        controlsOrder[idx + 1] = selectedControl
        controlsOrder[idx] = next
        swapControls(selectedControl, next)
    }

    fun onClose(appQuitting: Boolean) {
        if (floaterBase.isPhototools()) return
        savedSettings.setBool("QuickPrefsEditMode", false)
    }

    private fun onChangeVignetteX() {
        val v = savedSettings.getVector3("FSRenderVignette")
        val updated = Triple(sliderVignetteX?.getValueF32() ?: v.first, v.second, v.third)
        spinnerVignetteX?.setValue(updated.first)
        savedSettings.setVector3("FSRenderVignette", updated)
    }

    private fun onChangeVignetteY() {
        val v = savedSettings.getVector3("FSRenderVignette")
        val updated = Triple(v.first, sliderVignetteY?.getValueF32() ?: v.second, v.third)
        spinnerVignetteY?.setValue(updated.second)
        savedSettings.setVector3("FSRenderVignette", updated)
    }

    private fun onChangeVignetteZ() {
        val v = savedSettings.getVector3("FSRenderVignette")
        val updated = Triple(v.first, v.second, sliderVignetteZ?.getValueF32() ?: v.third)
        spinnerVignetteZ?.setValue(updated.third)
        savedSettings.setVector3("FSRenderVignette", updated)
    }

    private fun onChangeVignetteSpinnerX() {
        val v = savedSettings.getVector3("FSRenderVignette")
        val updated = Triple(spinnerVignetteX?.getValueF32() ?: v.first, v.second, v.third)
        sliderVignetteX?.setValue(updated.first)
        savedSettings.setVector3("FSRenderVignette", updated)
    }

    private fun onChangeVignetteSpinnerY() {
        val v = savedSettings.getVector3("FSRenderVignette")
        val updated = Triple(v.first, spinnerVignetteY?.getValueF32() ?: v.second, v.third)
        sliderVignetteY?.setValue(updated.second)
        savedSettings.setVector3("FSRenderVignette", updated)
    }

    private fun onChangeVignetteSpinnerZ() {
        val v = savedSettings.getVector3("FSRenderVignette")
        val updated = Triple(v.first, v.second, spinnerVignetteZ?.getValueF32() ?: v.third)
        sliderVignetteZ?.setValue(updated.third)
        savedSettings.setVector3("FSRenderVignette", updated)
    }

    private fun onClickResetVignetteX() {
        val default = TODO("get default vector3 for FSRenderVignette") as Triple<Float, Float, Float>
        val v = savedSettings.getVector3("FSRenderVignette")
        val updated = Triple(default.first, v.second, v.third)
        sliderVignetteX?.setValue(updated.first)
        spinnerVignetteX?.setValue(updated.first)
        savedSettings.setVector3("FSRenderVignette", updated)
    }

    private fun onClickResetVignetteY() {
        val default = TODO("get default vector3 for FSRenderVignette") as Triple<Float, Float, Float>
        val v = savedSettings.getVector3("FSRenderVignette")
        val updated = Triple(v.first, default.second, v.third)
        sliderVignetteY?.setValue(updated.second)
        spinnerVignetteY?.setValue(updated.second)
        savedSettings.setVector3("FSRenderVignette", updated)
    }

    private fun onClickResetVignetteZ() {
        val default = TODO("get default vector3 for FSRenderVignette") as Triple<Float, Float, Float>
        val v = savedSettings.getVector3("FSRenderVignette")
        val updated = Triple(v.first, v.second, default.third)
        sliderVignetteZ?.setValue(updated.third)
        spinnerVignetteZ?.setValue(updated.third)
        savedSettings.setVector3("FSRenderVignette", updated)
    }

    private fun onChangeRenderShadowSplitExponentSlider() {
        val v = savedSettings.getVector3("RenderShadowSplitExponent")
        val updated = Triple(v.first, sliderShadowSplitExponentY?.getValueF32() ?: v.second, v.third)
        spinnerShadowSplitExponentY?.setValue(updated.second)
        savedSettings.setVector3("RenderShadowSplitExponent", updated)
    }

    private fun onChangeRenderShadowSplitExponentSpinner() {
        val v = savedSettings.getVector3("RenderShadowSplitExponent")
        val updated = Triple(v.first, spinnerShadowSplitExponentY?.getValueF32() ?: v.second, v.third)
        sliderShadowSplitExponentY?.setValue(updated.second)
        savedSettings.setVector3("RenderShadowSplitExponent", updated)
    }

    private fun onClickResetRenderShadowSplitExponentY() {
        val default = TODO("get default vector3 for RenderShadowSplitExponent") as Triple<Float, Float, Float>
        val v = savedSettings.getVector3("RenderShadowSplitExponent")
        val updated = Triple(v.first, default.second, v.third)
        spinnerShadowSplitExponentY?.setValue(updated.second)
        sliderShadowSplitExponentY?.setValue(updated.second)
        savedSettings.setVector3("RenderShadowSplitExponent", updated)
    }

    private fun onChangeRenderShadowGaussianSlider() {
        val v = savedSettings.getVector3("RenderShadowGaussian")
        val updated = Triple(
            sliderShadowGaussianX?.getValueF32() ?: v.first,
            sliderShadowGaussianY?.getValueF32() ?: v.second,
            v.third
        )
        spinnerShadowGaussianX?.setValue(updated.first)
        spinnerShadowGaussianY?.setValue(updated.second)
        savedSettings.setVector3("RenderShadowGaussian", updated)
    }

    private fun onChangeRenderShadowGaussianSpinner() {
        val v = savedSettings.getVector3("RenderShadowGaussian")
        val updated = Triple(
            spinnerShadowGaussianX?.getValueF32() ?: v.first,
            spinnerShadowGaussianY?.getValueF32() ?: v.second,
            v.third
        )
        sliderShadowGaussianX?.setValue(updated.first)
        sliderShadowGaussianY?.setValue(updated.second)
        savedSettings.setVector3("RenderShadowGaussian", updated)
    }

    private fun onClickResetRenderShadowGaussianX() {
        val default = TODO("get default vector3 for RenderShadowGaussian") as Triple<Float, Float, Float>
        val v = savedSettings.getVector3("RenderShadowGaussian")
        val updated = Triple(default.first, v.second, v.third)
        spinnerShadowGaussianX?.setValue(updated.first)
        sliderShadowGaussianX?.setValue(updated.first)
        savedSettings.setVector3("RenderShadowGaussian", updated)
    }

    private fun onClickResetRenderShadowGaussianY() {
        val default = TODO("get default vector3 for RenderShadowGaussian") as Triple<Float, Float, Float>
        val v = savedSettings.getVector3("RenderShadowGaussian")
        val updated = Triple(v.first, default.second, v.third)
        spinnerShadowGaussianY?.setValue(updated.second)
        sliderShadowGaussianY?.setValue(updated.second)
        savedSettings.setVector3("RenderShadowGaussian", updated)
    }

    private fun onChangeRenderSSAOEffectSlider() {
        val v = savedSettings.getVector3("RenderSSAOEffect")
        val updated = Triple(sliderSSAOEffectX?.getValueF32() ?: v.first, v.second, v.third)
        spinnerSSAOEffectX?.setValue(updated.first)
        savedSettings.setVector3("RenderSSAOEffect", updated)
    }

    private fun onChangeRenderSSAOEffectSpinner() {
        val v = savedSettings.getVector3("RenderSSAOEffect")
        val updated = Triple(spinnerSSAOEffectX?.getValueF32() ?: v.first, v.second, v.third)
        sliderSSAOEffectX?.setValue(updated.first)
        savedSettings.setVector3("RenderSSAOEffect", updated)
    }

    private fun onClickResetRenderSSAOEffectX() {
        val default = TODO("get default vector3 for RenderSSAOEffect") as Triple<Float, Float, Float>
        val v = savedSettings.getVector3("RenderSSAOEffect")
        val updated = Triple(default.first, v.second, v.third)
        spinnerSSAOEffectX?.setValue(updated.first)
        sliderSSAOEffectX?.setValue(updated.first)
        savedSettings.setVector3("RenderSSAOEffect", updated)
    }

    private fun callbackRestoreDefaults(confirmed: Boolean) {
        if (confirmed) {
            selectControl("")
            for (name in controlsOrder.toList()) removeControl(name)
            controlsOrder.clear()
            val filename = appViewer.getSettingsFilename("Default", "QuickPreferences")
            fs.remove(fs.expandPath(LL_PATH_USER_SETTINGS, filename))
            loadSavedSettingsFromFile(fs.expandPath(LL_PATH_APP_SETTINGS, filename))
            savedSettings.setBool("QuickPrefsEditMode", false)
        }
    }

    private fun onClickRestoreDefaults() {
        notificationUtil.add("ConfirmRestoreQuickPrefsDefaults")
    }

    fun dockToToolbarButton() {
        TODO("APR: use JVM equivalent for toolbar docking")
    }

    private fun onAvatarZOffsetSliderMoved() {
        val value = avatarZOffsetSlider?.getValueF32() ?: return
        val clamped = value.coerceIn(MIN_HOVER_Z, MAX_HOVER_Z)
        val region = agent.getRegion()
        if (region?.avatarHoverHeightEnabled() == true) {
            if (avatarZOffsetSlider?.isMouseHeldDown == true) {
                agentAvatar.setHoverOffset(Triple(0f, 0f, clamped), false)
            } else {
                savedPerAccountSettings.setF32("AvatarHoverOffsetZ", value)
            }
        } else if (!agentAvatar.isUsingServerBakes()) {
            savedPerAccountSettings.setF32("AvatarHoverOffsetZ", value)
        }
    }

    private fun onAvatarZOffsetFinalCommit() {
        val value = avatarZOffsetSlider?.getValueF32() ?: return
        savedPerAccountSettings.setF32("AvatarHoverOffsetZ", value)
    }

    private fun updateAvatarZOffsetEditEnabled() {
        val region = agent.getRegion()
        var enabled = region?.avatarHoverHeightEnabled() == true
        if (!enabled && agentAvatar.isValid() && !agentAvatar.isUsingServerBakes()) {
            enabled = true
        }
        avatarZOffsetSlider?.enabled = enabled
        if (enabled) syncAvatarZOffsetFromPreferenceSetting()
    }

    private fun onRegionChanged() {
        val region = agent.getRegion()
        if (region?.simulatorFeaturesReceived() == true) {
            updateAvatarZOffsetEditEnabled()
        } else {
            region?.setSimulatorFeaturesReceivedCallback { regionId ->
                onSimulatorFeaturesReceived(regionId)
            }
        }
    }

    private fun onSimulatorFeaturesReceived(regionId: UUID) {
        val region = agent.getRegion()
        if (region != null && region.id == regionId) {
            updateAvatarZOffsetEditEnabled()
        }
    }

    private fun syncAvatarZOffsetFromPreferenceSetting() {
        val value = savedPerAccountSettings.getF32("AvatarHoverOffsetZ")
        avatarZOffsetSlider?.setValue(value)
    }

    private fun updateMaxNonImpostors(newValue: Any?) {
        var value = (newValue as? Number)?.toInt() ?: return
        if (value == 0 || value >= NON_IMPOSTORS_MAX_SLIDER) value = 0
        savedSettings.setU32("RenderAvatarMaxNonImpostors", value.toUInt())
        TODO("APR: call VOAvatar.updateImpostorRendering(value)")
    }

    private fun updateMaxComplexity() {
        complexityControls.updateMax(maxComplexitySlider, maxComplexityLabel)
    }

    private fun updateMaxComplexityLabel(newValue: Any?) {
        val value = (newValue as? Number)?.toInt()?.toUInt() ?: return
        complexityControls.setText(value, maxComplexityLabel)
    }

    fun hasControl(name: String): Boolean = controlsList.containsKey(name)

    companion object {
        const val NON_IMPOSTORS_MAX_SLIDER = 66
    }
}

interface ControlVariable {
    fun getValue(): Any?
    fun getTypeName(): String
    fun isHiddenFromSettingsEditor(): Boolean
    fun connectSignal(cb: (Any?) -> Unit)
    fun connectSignalReturningDisconnect(cb: (Any?) -> Unit): () -> Unit
}

var UiControl.enabled: Boolean
    get() = TODO("get enabled state")
    set(value) { enabled = value }
