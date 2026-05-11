package com.firestorm.newview

import java.util.UUID

const val PARTICLE_SCRIPT_NAME = "New Particle Script"

class ParticleEditor(val key: Any) {

    // -----------------------------------------------------------------------
    // Blend / pattern constants — mirrors LLPartSysData / LLPartData values.
    // -----------------------------------------------------------------------

    private val patternMap: MutableMap<String, UByte> = mutableMapOf(
        "drop"             to 0x01u,
        "explode"          to 0x02u,
        "angle"            to 0x04u,
        "angle_cone"       to 0x08u,
        "angle_cone_empty" to 0x10u
    )

    private val scriptPatternMap: MutableMap<String, String> = mutableMapOf(
        "drop"             to "PSYS_SRC_PATTERN_DROP",
        "explode"          to "PSYS_SRC_PATTERN_EXPLODE",
        "angle"            to "PSYS_SRC_PATTERN_ANGLE",
        "angle_cone"       to "PSYS_SRC_PATTERN_ANGLE_CONE",
        "angle_cone_empty" to "PSYS_SRC_PATTERN_ANGLE_CONE_EMPTY"
    )

    private val blendMap: MutableMap<String, UByte> = mutableMapOf(
        "blend_one"                  to 0x00u,
        "blend_zero"                 to 0x01u,
        "blend_dest_color"           to 0x02u,
        "blend_src_color"            to 0x03u,
        "blend_one_minus_dest_color" to 0x04u,
        "blend_one_minus_src_color"  to 0x05u,
        "blend_src_alpha"            to 0x06u,
        "blend_one_minus_src_alpha"  to 0x07u
    )

    private val scriptBlendMap: MutableMap<String, String> = mutableMapOf(
        "blend_one"                  to "PSYS_PART_BF_ONE",
        "blend_zero"                 to "PSYS_PART_BF_ZERO",
        "blend_dest_color"           to "PSYS_PART_BF_DEST_COLOR",
        "blend_src_color"            to "PSYS_PART_BF_SOURCE_COLOR",
        "blend_one_minus_dest_color" to "PSYS_PART_BF_ONE_MINUS_DEST_COLOR",
        "blend_one_minus_src_color"  to "PSYS_PART_BF_ONE_MINUS_SOURCE_COLOR",
        "blend_src_alpha"            to "PSYS_PART_BF_SOURCE_ALPHA",
        "blend_one_minus_src_alpha"  to "PSYS_PART_BF_ONE_MINUS_SOURCE_ALPHA"
    )

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    var objectId: UUID? = null
    var textureId: UUID? = null
    var particleScriptInventoryItemId: UUID? = null

    // defaultParticleTextureId corresponds to pixiesmall.j2c, resolved at init.
    var defaultParticleTextureId: UUID? = null

    // Mirrors LLPartSysData fields used for live preview and script generation.
    var particlePattern: UByte = 0u
    var particleImageId: UUID? = null
    var burstRate: Float = 0.1f
    var burstPartCount: Int = 1
    var burstRadius: Float = 0.0f
    var innerAngle: Float = 0.0f
    var outerAngle: Float = 0.0f
    var burstSpeedMin: Float = 1.0f
    var burstSpeedMax: Float = 1.0f
    var startAlpha: Float = 1.0f
    var endAlpha: Float = 1.0f
    var scaleStartX: Float = 1.0f
    var scaleStartY: Float = 1.0f
    var scaleEndX: Float = 1.0f
    var scaleEndY: Float = 1.0f
    var sourceMaxAge: Float = 0.0f
    var particlesMaxAge: Float = 10.0f
    var startGlow: Float = 0.0f
    var endGlow: Float = 0.0f
    var blendFuncSource: UByte = 0u
    var blendFuncDest: UByte = 0u
    var particleFlags: UInt = 0u
    var targetKey: UUID? = null
    var accelX: Float = 0.0f
    var accelY: Float = 0.0f
    var accelZ: Float = 0.0f
    var omegaX: Float = 0.0f
    var omegaY: Float = 0.0f
    var omegaZ: Float = 0.0f
    var startColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var endColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)

    // -----------------------------------------------------------------------
    // UI control stubs
    // -----------------------------------------------------------------------

    var mainPanel: Panel? = null
    var patternTypeCombo: ComboBox? = null
    var texturePicker: TextureCtrl? = null
    var burstRateSpinner: SpinCtrl? = null
    var burstCountSpinner: SpinCtrl? = null
    var burstRadiusSpinner: SpinCtrl? = null
    var angleBeginSpinner: SpinCtrl? = null
    var angleEndSpinner: SpinCtrl? = null
    var burstSpeedMinSpinner: SpinCtrl? = null
    var burstSpeedMaxSpinner: SpinCtrl? = null
    var startAlphaSpinner: SpinCtrl? = null
    var endAlphaSpinner: SpinCtrl? = null
    var scaleStartXSpinner: SpinCtrl? = null
    var scaleStartYSpinner: SpinCtrl? = null
    var scaleEndXSpinner: SpinCtrl? = null
    var scaleEndYSpinner: SpinCtrl? = null
    var sourceMaxAgeSpinner: SpinCtrl? = null
    var particlesMaxAgeSpinner: SpinCtrl? = null
    var startGlowSpinner: SpinCtrl? = null
    var endGlowSpinner: SpinCtrl? = null
    var blendFuncSrcCombo: ComboBox? = null
    var blendFuncDestCombo: ComboBox? = null
    var bounceCheckBox: CheckBoxCtrl? = null
    var emissiveCheckBox: CheckBoxCtrl? = null
    var followSourceCheckBox: CheckBoxCtrl? = null
    var followVelocityCheckBox: CheckBoxCtrl? = null
    var interpolateColorCheckBox: CheckBoxCtrl? = null
    var interpolateScaleCheckBox: CheckBoxCtrl? = null
    var targetPositionCheckBox: CheckBoxCtrl? = null
    var targetLinearCheckBox: CheckBoxCtrl? = null
    var windCheckBox: CheckBoxCtrl? = null
    var ribbonCheckBox: CheckBoxCtrl? = null
    var targetKeyInput: LineEditor? = null
    var clearTargetButton: Button? = null
    var pickTargetButton: Button? = null
    var accelXSpinner: SpinCtrl? = null
    var accelYSpinner: SpinCtrl? = null
    var accelZSpinner: SpinCtrl? = null
    var omegaXSpinner: SpinCtrl? = null
    var omegaYSpinner: SpinCtrl? = null
    var omegaZSpinner: SpinCtrl? = null
    var startColorSelector: ColorSwatchCtrl? = null
    var endColorSelector: ColorSwatchCtrl? = null
    var copyToLslButton: Button? = null
    var injectScriptButton: Button? = null

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun postBuild(): Boolean {
        TODO("GPU: bind child UI widgets by name from mainPanel and wire commit callbacks")
        blendFuncSrcCombo?.setValue("blend_src_alpha")
        blendFuncDestCombo?.setValue("blend_one_minus_src_alpha")
        onParameterChange()
        return true
    }

    fun destroy() {
        clearParticles()
    }

    // -----------------------------------------------------------------------
    // Particle simulation
    // -----------------------------------------------------------------------

    fun clearParticles() {
        val id = objectId ?: return
        TODO("GPU: LLViewerPartSim::clearParticlesByOwnerID($id)")
    }

    fun updateParticles() {
        val id = objectId ?: return
        clearParticles()
        TODO("GPU: LLViewerPartSourceScript::createPSS + LLViewerPartSim::addPartSource for object $id")
    }

    fun setObject(viewerObjectId: UUID?) {
        if (viewerObjectId != null) {
            objectId = viewerObjectId
            updateParticles()
        }
    }

    // -----------------------------------------------------------------------
    // UI callbacks
    // -----------------------------------------------------------------------

    fun onParameterChange() {
        val selectedPattern = patternTypeCombo?.getSelectedValue() ?: "drop"
        particlePattern = patternMap[selectedPattern] ?: 0u

        val pickedTextureId = texturePicker?.getImageAssetId()
        particleImageId = pickedTextureId
        textureId = if (pickedTextureId == null || pickedTextureId == defaultParticleTextureId) {
            defaultParticleTextureId
        } else {
            pickedTextureId
        }

        // Lower-bound burst rate to avoid internal freeze; script receives the raw spinner value.
        burstRate = maxOf(0.01f, burstRateSpinner?.getValueF32() ?: 0.01f)
        burstPartCount = burstCountSpinner?.getValueInt() ?: 1
        burstRadius = burstRadiusSpinner?.getValueF32() ?: 0f
        innerAngle = angleBeginSpinner?.getValueF32() ?: 0f
        outerAngle = angleEndSpinner?.getValueF32() ?: 0f
        burstSpeedMin = burstSpeedMinSpinner?.getValueF32() ?: 1f
        burstSpeedMax = burstSpeedMaxSpinner?.getValueF32() ?: 1f
        startAlpha = startAlphaSpinner?.getValueF32() ?: 1f
        endAlpha = endAlphaSpinner?.getValueF32() ?: 1f
        scaleStartX = scaleStartXSpinner?.getValueF32() ?: 1f
        scaleStartY = scaleStartYSpinner?.getValueF32() ?: 1f
        scaleEndX = scaleEndXSpinner?.getValueF32() ?: 1f
        scaleEndY = scaleEndYSpinner?.getValueF32() ?: 1f
        sourceMaxAge = sourceMaxAgeSpinner?.getValueF32() ?: 0f
        particlesMaxAge = particlesMaxAgeSpinner?.getValueF32() ?: 10f
        // Glow is set differently from other scale fields per original Linden implementation.
        startGlow = startGlowSpinner?.getValueF32() ?: 0f
        endGlow = endGlowSpinner?.getValueF32() ?: 0f

        blendFuncSource = blendMap[blendFuncSrcCombo?.getSelectedValue() ?: "blend_src_alpha"] ?: 0u
        blendFuncDest = blendMap[blendFuncDestCombo?.getSelectedValue() ?: "blend_one_minus_src_alpha"] ?: 0u

        var flags = 0u
        if (bounceCheckBox?.getValue() == true)           flags = flags or 0x0001u
        if (emissiveCheckBox?.getValue() == true)         flags = flags or 0x0002u
        if (followSourceCheckBox?.getValue() == true)     flags = flags or 0x0004u
        if (followVelocityCheckBox?.getValue() == true)   flags = flags or 0x0008u
        if (interpolateColorCheckBox?.getValue() == true) flags = flags or 0x0010u
        if (interpolateScaleCheckBox?.getValue() == true) flags = flags or 0x0020u
        if (targetPositionCheckBox?.getValue() == true)   flags = flags or 0x0040u
        if (targetLinearCheckBox?.getValue() == true)     flags = flags or 0x0080u
        if (windCheckBox?.getValue() == true)             flags = flags or 0x0100u
        if (ribbonCheckBox?.getValue() == true)           flags = flags or 0x0200u
        particleFlags = flags

        targetKey = runCatching { UUID.fromString(targetKeyInput?.getValue() ?: "") }.getOrNull()

        accelX = accelXSpinner?.getValueF32() ?: 0f
        accelY = accelYSpinner?.getValueF32() ?: 0f
        accelZ = accelZSpinner?.getValueF32() ?: 0f
        omegaX = omegaXSpinner?.getValueF32() ?: 0f
        omegaY = omegaYSpinner?.getValueF32() ?: 0f
        omegaZ = omegaZSpinner?.getValueF32() ?: 0f

        startColor = startColorSelector?.getColor() ?: floatArrayOf(1f, 1f, 1f, 1f)
        endColor = endColorSelector?.getColor() ?: floatArrayOf(1f, 1f, 1f, 1f)

        updateUi()
        updateParticles()
    }

    fun updateUi() {
        val selectedPattern = patternTypeCombo?.getValue() ?: "drop"
        val pattern = patternMap[selectedPattern] ?: 0u
        val dropPattern = pattern == patternMap["drop"]
        val explodePattern = pattern == patternMap["explode"]
        val targetLinear = targetLinearCheckBox?.getValue() ?: false
        val interpolateColor = interpolateColorCheckBox?.getValue() ?: false
        val interpolateScale = interpolateScaleCheckBox?.getValue() ?: false
        val targetEnabled = targetLinear || (targetPositionCheckBox?.getValue() ?: false)

        burstRadiusSpinner?.setEnabled(!targetLinear && !(followSourceCheckBox?.getValue() ?: false) && !dropPattern)
        burstSpeedMinSpinner?.setEnabled(!targetLinear && !dropPattern)
        burstSpeedMaxSpinner?.setEnabled(!targetLinear && !dropPattern)

        // Visually communicate interpolation state by zeroing alpha on the end color swatch.
        val ec = endColorSelector?.getColor()?.copyOf() ?: floatArrayOf(1f, 1f, 1f, 0f)
        ec[3] = if (interpolateColor) 1.0f else 0.0f
        endAlphaSpinner?.setEnabled(interpolateColor)
        endColorSelector?.setColor(ec)
        endColorSelector?.setEnabled(interpolateColor)

        scaleEndXSpinner?.setEnabled(interpolateScale)
        scaleEndYSpinner?.setEnabled(interpolateScale)

        targetPositionCheckBox?.setEnabled(!targetLinear)
        targetKeyInput?.setEnabled(targetEnabled)
        pickTargetButton?.setEnabled(targetEnabled)
        clearTargetButton?.setEnabled(targetEnabled)

        accelXSpinner?.setEnabled(!targetLinear)
        accelYSpinner?.setEnabled(!targetLinear)
        accelZSpinner?.setEnabled(!targetLinear)
        omegaXSpinner?.setEnabled(!targetLinear)
        omegaYSpinner?.setEnabled(!targetLinear)
        omegaZSpinner?.setEnabled(!targetLinear)

        angleBeginSpinner?.setEnabled(!explodePattern && !dropPattern)
        angleEndSpinner?.setEnabled(!explodePattern && !dropPattern)
    }

    fun onClearTargetButtonClicked() {
        targetKeyInput?.clear()
        onParameterChange()
    }

    fun onTargetPickerButtonClicked() {
        pickTargetButton?.setToggleState(true)
        pickTargetButton?.setEnabled(false)
        startPicking()
    }

    private fun startPicking() {
        TODO("GPU: LLToolObjPicker::setExitCallback + LLToolMgr::setTransientTool")
    }

    fun onTargetPicked(pickedObjectId: UUID?) {
        TODO("GPU: LLToolMgr::clearTransientTool")
        pickTargetButton?.setEnabled(true)
        pickTargetButton?.setToggleState(false)
        if (pickedObjectId != null) {
            targetKeyInput?.setValue(pickedObjectId.toString())
            onParameterChange()
        }
    }

    // -----------------------------------------------------------------------
    // Script generation
    // -----------------------------------------------------------------------

    private fun lslVector(x: Float, y: Float, z: Float): String = "<%f,%f,%f>".format(x, y, z)

    private fun lslColor(color: FloatArray): String = lslVector(color[0], color[1], color[2])

    fun createScript(): String {
        var script = """
default
{
    state_entry()
    {
        llParticleSystem(
        [
            PSYS_SRC_PATTERN,[PATTERN],
            PSYS_SRC_BURST_RADIUS,[BURST_RADIUS],
            PSYS_SRC_ANGLE_BEGIN,[ANGLE_BEGIN],
            PSYS_SRC_ANGLE_END,[ANGLE_END],
            PSYS_SRC_TARGET_KEY,[TARGET_KEY],
            PSYS_PART_START_COLOR,[START_COLOR],
            PSYS_PART_END_COLOR,[END_COLOR],
            PSYS_PART_START_ALPHA,[START_ALPHA],
            PSYS_PART_END_ALPHA,[END_ALPHA],
            PSYS_PART_START_GLOW,[START_GLOW],
            PSYS_PART_END_GLOW,[END_GLOW],
            PSYS_PART_BLEND_FUNC_SOURCE,[BLEND_FUNC_SOURCE],
            PSYS_PART_BLEND_FUNC_DEST,[BLEND_FUNC_DEST],
            PSYS_PART_START_SCALE,[START_SCALE],
            PSYS_PART_END_SCALE,[END_SCALE],
            PSYS_SRC_TEXTURE,"[TEXTURE]",
            PSYS_SRC_MAX_AGE,[SOURCE_MAX_AGE],
            PSYS_PART_MAX_AGE,[PART_MAX_AGE],
            PSYS_SRC_BURST_RATE,[BURST_RATE],
            PSYS_SRC_BURST_PART_COUNT,[BURST_COUNT],
            PSYS_SRC_ACCEL,[ACCELERATION],
            PSYS_SRC_OMEGA,[OMEGA],
            PSYS_SRC_BURST_SPEED_MIN,[BURST_SPEED_MIN],
            PSYS_SRC_BURST_SPEED_MAX,[BURST_SPEED_MAX],
            PSYS_PART_FLAGS,
                0[FLAGS]
        ]);
    }
}
""".trimIndent() + "\n"

        val keyString = if (targetKey != null && targetKey != objectId) {
            "(key) \"$targetKey\""
        } else {
            "llGetKey()"
        }

        val textureString = if (textureId != null && textureId != defaultParticleTextureId) {
            textureId.toString()
        } else {
            ""
        }

        val selectedPattern = patternTypeCombo?.getValue() ?: "drop"
        script = script.replace("[PATTERN]", scriptPatternMap[selectedPattern] ?: "PSYS_SRC_PATTERN_DROP")
        script = script.replace("[BURST_RADIUS]", burstRadiusSpinner?.getValue() ?: burstRadius.toString())
        script = script.replace("[ANGLE_BEGIN]", angleBeginSpinner?.getValue() ?: innerAngle.toString())
        script = script.replace("[ANGLE_END]", angleEndSpinner?.getValue() ?: outerAngle.toString())
        script = script.replace("[TARGET_KEY]", keyString)
        script = script.replace("[START_COLOR]", lslColor(startColor))
        script = script.replace("[END_COLOR]", lslColor(endColor))
        script = script.replace("[START_ALPHA]", startAlphaSpinner?.getValue() ?: startAlpha.toString())
        script = script.replace("[END_ALPHA]", endAlphaSpinner?.getValue() ?: endAlpha.toString())
        script = script.replace("[START_GLOW]", startGlowSpinner?.getValue() ?: startGlow.toString())
        script = script.replace("[END_GLOW]", endGlowSpinner?.getValue() ?: endGlow.toString())
        script = script.replace("[START_SCALE]", lslVector(scaleStartX, scaleStartY, 0.0f))
        script = script.replace("[END_SCALE]", lslVector(scaleEndX, scaleEndY, 0.0f))
        script = script.replace("[TEXTURE]", textureString)
        script = script.replace("[SOURCE_MAX_AGE]", sourceMaxAgeSpinner?.getValue() ?: sourceMaxAge.toString())
        script = script.replace("[PART_MAX_AGE]", particlesMaxAgeSpinner?.getValue() ?: particlesMaxAge.toString())
        script = script.replace("[BURST_RATE]", burstRateSpinner?.getValue() ?: burstRate.toString())
        script = script.replace("[BURST_COUNT]", burstCountSpinner?.getValue() ?: burstPartCount.toString())
        script = script.replace("[ACCELERATION]", lslVector(accelX, accelY, accelZ))
        script = script.replace("[OMEGA]", lslVector(omegaX, omegaY, omegaZ))
        script = script.replace("[BURST_SPEED_MIN]", burstSpeedMinSpinner?.getValue() ?: burstSpeedMin.toString())
        script = script.replace("[BURST_SPEED_MAX]", burstSpeedMaxSpinner?.getValue() ?: burstSpeedMax.toString())
        script = script.replace("[BLEND_FUNC_SOURCE]", scriptBlendMap[blendFuncSrcCombo?.getValue() ?: "blend_src_alpha"] ?: "PSYS_PART_BF_SOURCE_ALPHA")
        script = script.replace("[BLEND_FUNC_DEST]", scriptBlendMap[blendFuncDestCombo?.getValue() ?: "blend_one_minus_src_alpha"] ?: "PSYS_PART_BF_ONE_MINUS_SOURCE_ALPHA")

        val delimiter = " |\n                "
        var flagsString = ""
        if (bounceCheckBox?.getValue() == true)           flagsString += delimiter + "PSYS_PART_BOUNCE_MASK"
        if (emissiveCheckBox?.getValue() == true)         flagsString += delimiter + "PSYS_PART_EMISSIVE_MASK"
        if (followSourceCheckBox?.getValue() == true)     flagsString += delimiter + "PSYS_PART_FOLLOW_SRC_MASK"
        if (followVelocityCheckBox?.getValue() == true)   flagsString += delimiter + "PSYS_PART_FOLLOW_VELOCITY_MASK"
        if (interpolateColorCheckBox?.getValue() == true) flagsString += delimiter + "PSYS_PART_INTERP_COLOR_MASK"
        if (interpolateScaleCheckBox?.getValue() == true) flagsString += delimiter + "PSYS_PART_INTERP_SCALE_MASK"
        if (targetLinearCheckBox?.getValue() == true)     flagsString += delimiter + "PSYS_PART_TARGET_LINEAR_MASK"
        if (targetPositionCheckBox?.getValue() == true)   flagsString += delimiter + "PSYS_PART_TARGET_POS_MASK"
        if (windCheckBox?.getValue() == true)             flagsString += delimiter + "PSYS_PART_WIND_MASK"
        if (ribbonCheckBox?.getValue() == true)           flagsString += delimiter + "PSYS_PART_RIBBON_MASK"

        script = script.replace("[FLAGS]", flagsString)
        return script
    }

    fun onCopyButtonClicked() {
        val script = createScript()
        if (script.isNotEmpty()) {
            TODO("GPU: copy script text to clipboard via window/platform API")
        }
    }

    fun onInjectButtonClicked() {
        TODO("APR: locate or create #Firestorm inventory folder, then call createScriptInventoryItem")
    }

    fun createScriptInventoryItem(categoryId: UUID?) {
        var catId = categoryId
        if (catId == null) {
            TODO("APR: find default Scripts folder by type LLFolderType::FT_LSL_TEXT")
        }
        if (catId == null) {
            TODO("APR: notify user - ParticleScriptFindFolderFailed")
            return
        }
        TODO("APR: create_inventory_item with ParticleScriptCreationCallback, then upload script via UpdateScriptAgent cap")
    }

    fun callbackReturned(inventoryItemId: UUID?) {
        setCanClose(true)
        if (inventoryItemId == null) {
            TODO("APR: notify user - ParticleScriptCreationFailed")
            return
        }
        TODO("APR: retrieve inventory item, upload script via UpdateScriptAgent cap, then call scriptInjectReturned on completion")
    }

    fun scriptInjectReturned() {
        setCanClose(true)
        mainPanel?.setEnabled(true)
        TODO("APR: save script to object via LLViewerObject::saveScript if object is still alive")
    }

    private fun setCanClose(canClose: Boolean) {
        TODO("GPU: LLFloater::setCanClose($canClose)")
    }

    // -----------------------------------------------------------------------
    // Minimal UI-stub interfaces
    // -----------------------------------------------------------------------

    interface Panel {
        fun setEnabled(enabled: Boolean)
    }

    interface ComboBox {
        fun getValue(): String?
        fun getSelectedValue(): String?
        fun setValue(value: String)
        fun setCommitCallback(callback: () -> Unit)
    }

    interface TextureCtrl {
        fun getImageAssetId(): UUID?
        fun setCanApplyImmediately(value: Boolean)
        fun setCommitCallback(callback: () -> Unit)
    }

    interface SpinCtrl {
        fun getValueF32(): Float
        fun getValueInt(): Int
        fun getValue(): String?
        fun setEnabled(enabled: Boolean)
        fun setCommitCallback(callback: () -> Unit)
    }

    interface CheckBoxCtrl {
        fun getValue(): Boolean
        fun setValue(value: Boolean)
        fun setEnabled(enabled: Boolean)
        fun setCommitCallback(callback: () -> Unit)
    }

    interface LineEditor {
        fun getValue(): String?
        fun setValue(value: String)
        fun clear()
        fun setEnabled(enabled: Boolean)
        fun setCommitCallback(callback: () -> Unit)
    }

    interface Button {
        fun getValue(): Boolean
        fun setEnabled(enabled: Boolean)
        fun setToggleState(state: Boolean)
        fun setCommitCallback(callback: () -> Unit)
    }

    interface ColorSwatchCtrl {
        fun getColor(): FloatArray
        fun setColor(color: FloatArray)
        fun setEnabled(enabled: Boolean)
        fun setCanApplyImmediately(value: Boolean)
        fun setCommitCallback(callback: () -> Unit)
    }
}

class ParticleScriptCreationCallback(private val editor: ParticleEditor) {
    fun fire(inventoryItem: UUID?) {
        TODO("APR: guard against app quit / disconnect, then call editor.callbackReturned(inventoryItem)")
    }
}
