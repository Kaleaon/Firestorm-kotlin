package com.firestorm.newview

import com.firestorm.ui.Panel
import com.firestorm.ui.Button
import com.firestorm.ui.CheckBoxCtrl
import com.firestorm.ui.ColorSwatchCtrl
import com.firestorm.ui.ComboBox
import com.firestorm.ui.SpinCtrl
import com.firestorm.ui.TextBox
import com.firestorm.ui.TextureCtrl
import com.firestorm.ui.UICtrl
import com.firestorm.ui.LLSD
import com.firestorm.object.ViewerObject
import com.firestorm.object.VOVolume
import com.firestorm.agent.Agent
import com.firestorm.agent.AgentCamera
import com.firestorm.pipeline.Pipeline

typealias Uuid = java.util.UUID
typealias Color4 = FloatArray

const val DEFAULT_GRAVITY_MULTIPLIER = 1.0f
const val DEFAULT_DENSITY = 1000.0f

class PanelVolume : Panel() {

    private var comboMaterialItemCount: Int = 0
    private var comboMaterial: ComboBox? = null
    private var lightSavedColor: Color4 = floatArrayOf(1f, 1f, 1f, 1f)
    private var currentObject: ViewerObject? = null
    private var rootObject: ViewerObject? = null
    private var comboPhysicsShapeType: ComboBox? = null
    private var spinPhysicsGravity: SpinCtrl? = null
    private var spinPhysicsFriction: SpinCtrl? = null
    private var spinPhysicsDensity: SpinCtrl? = null
    private var spinPhysicsRestitution: SpinCtrl? = null
    private var btnCopyFeatures: Button? = null
    private var btnPasteFeatures: Button? = null
    private var clipboardParams: LLSD = LLSD()

    override fun postBuild(): Boolean {
        childSetCommitCallback("Animated Mesh Checkbox Ctrl") { ctrl, _ -> onCommitAnimatedMeshCheckbox(ctrl, null) }
        childSetCommitCallback("Flexible1D Checkbox Ctrl") { ctrl, _ -> onCommitIsFlexible(ctrl, null) }
        childSetCommitCallback("FlexNumSections") { ctrl, _ -> onCommitFlexible(ctrl, null) }
        getChild<UICtrl>("FlexNumSections")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("FlexGravity") { ctrl, _ -> onCommitFlexible(ctrl, null) }
        getChild<UICtrl>("FlexGravity")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("FlexFriction") { ctrl, _ -> onCommitFlexible(ctrl, null) }
        getChild<UICtrl>("FlexFriction")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("FlexWind") { ctrl, _ -> onCommitFlexible(ctrl, null) }
        getChild<UICtrl>("FlexWind")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("FlexTension") { ctrl, _ -> onCommitFlexible(ctrl, null) }
        getChild<UICtrl>("FlexTension")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("FlexForceX") { ctrl, _ -> onCommitFlexible(ctrl, null) }
        getChild<UICtrl>("FlexForceX")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("FlexForceY") { ctrl, _ -> onCommitFlexible(ctrl, null) }
        getChild<UICtrl>("FlexForceY")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("FlexForceZ") { ctrl, _ -> onCommitFlexible(ctrl, null) }
        getChild<UICtrl>("FlexForceZ")?.setValidateBeforeCommit(::precommitValidate)

        childSetCommitCallback("Light Checkbox Ctrl") { ctrl, _ -> onCommitIsLight(ctrl, null) }
        getChild<ColorSwatchCtrl>("colorswatch")?.let { swatch ->
            swatch.setOnCancelCallback { onLightCancelColor(it) }
            swatch.setOnSelectCallback { onLightSelectColor(it) }
            childSetCommitCallback("colorswatch") { ctrl, _ -> onCommitLight(ctrl, null) }
        }
        getChild<TextureCtrl>("light texture control")?.let { tex ->
            tex.setOnCancelCallback { onLightCancelTexture(it) }
            tex.setOnSelectCallback { onLightSelectTexture(it) }
            childSetCommitCallback("light texture control") { ctrl, _ -> onCommitLight(ctrl, null) }
        }
        childSetCommitCallback("Light Intensity") { ctrl, _ -> onCommitLight(ctrl, null) }
        getChild<UICtrl>("Light Intensity")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("Light Radius") { ctrl, _ -> onCommitLight(ctrl, null) }
        getChild<UICtrl>("Light Radius")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("Light Falloff") { ctrl, _ -> onCommitLight(ctrl, null) }
        getChild<UICtrl>("Light Falloff")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("Light FOV") { ctrl, _ -> onCommitLight(ctrl, null) }
        getChild<UICtrl>("Light FOV")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("Light Focus") { ctrl, _ -> onCommitLight(ctrl, null) }
        getChild<UICtrl>("Light Focus")?.setValidateBeforeCommit(::precommitValidate)
        childSetCommitCallback("Light Ambiance") { ctrl, _ -> onCommitLight(ctrl, null) }
        getChild<UICtrl>("Light Ambiance")?.setValidateBeforeCommit(::precommitValidate)

        childSetCommitCallback("Reflection Probe") { ctrl, _ -> onCommitIsReflectionProbe(ctrl, null) }
        childSetCommitCallback("Probe Update Type") { ctrl, _ -> onCommitProbe(ctrl, null) }
        childSetCommitCallback("Probe Dynamic") { ctrl, _ -> onCommitProbe(ctrl, null) }
        childSetCommitCallback("Probe Volume Type") { ctrl, _ -> onCommitProbe(ctrl, null) }
        childSetCommitCallback("Probe Ambiance") { ctrl, _ -> onCommitProbe(ctrl, null) }
        childSetCommitCallback("Probe Near Clip") { ctrl, _ -> onCommitProbe(ctrl, null) }

        comboPhysicsShapeType = getChild<ComboBox>("Physics Shape Type Combo Ctrl")
        comboPhysicsShapeType?.setCommitCallback { ctrl, _ -> sendPhysicsShapeType(ctrl, comboPhysicsShapeType) }

        spinPhysicsGravity = getChild<SpinCtrl>("Physics Gravity")
        spinPhysicsGravity?.setCommitCallback { ctrl, _ -> sendPhysicsGravity(ctrl, spinPhysicsGravity) }

        spinPhysicsFriction = getChild<SpinCtrl>("Physics Friction")
        spinPhysicsFriction?.setCommitCallback { ctrl, _ -> sendPhysicsFriction(ctrl, spinPhysicsFriction) }

        spinPhysicsDensity = getChild<SpinCtrl>("Physics Density")
        spinPhysicsDensity?.setCommitCallback { ctrl, _ -> sendPhysicsDensity(ctrl, spinPhysicsDensity) }

        spinPhysicsRestitution = getChild<SpinCtrl>("Physics Restitution")
        spinPhysicsRestitution?.setCommitCallback { ctrl, _ -> sendPhysicsRestitution(ctrl, spinPhysicsRestitution) }

        btnCopyFeatures = getChild<Button>("copy_features_btn")
        btnCopyFeatures?.setCommitCallback { _, _ -> onFSCopyFeatures() }
        btnPasteFeatures = getChild<Button>("paste_features_btn")
        btnPasteFeatures?.setCommitCallback { _, _ -> onFSPasteFeatures() }

        val materialNameMap = mapOf(
            "Stone"   to Trans.getString("Stone"),
            "Metal"   to Trans.getString("Metal"),
            "Glass"   to Trans.getString("Glass"),
            "Wood"    to Trans.getString("Wood"),
            "Flesh"   to Trans.getString("Flesh"),
            "Plastic" to Trans.getString("Plastic"),
            "Rubber"  to Trans.getString("Rubber"),
            "Light"   to Trans.getString("Light")
        )
        MaterialTable.basic.initTableTransNames(materialNameMap)

        comboMaterial = getChild<ComboBox>("material")
        childSetCommitCallback("material") { ctrl, _ -> onCommitMaterial(ctrl, null) }
        comboMaterial?.removeAll()
        for (info in MaterialTable.basic.materialInfoList) {
            if (info.mCode != LL_MCODE_LIGHT) {
                comboMaterial?.add(info.name)
            }
        }
        comboMaterialItemCount = comboMaterial?.getItemCount() ?: 0

        clearCtrls()
        return true
    }

    override fun draw() {
        // no-op
    }

    override fun clearCtrls() {
        super.clearCtrls()
        getChildView("select_single")?.setEnabled(false)
        getChildView("select_single")?.setVisible(true)
        getChildView("edit_object")?.setEnabled(false)
        getChildView("edit_object")?.setVisible(false)
        getChildView("Light Checkbox Ctrl")?.setEnabled(false)
        getChild<ColorSwatchCtrl>("colorswatch")?.let { it.setEnabled(false); it.setValid(false) }
        getChildView("Light Intensity")?.setEnabled(false)
        getChildView("Light Radius")?.setEnabled(false)
        getChildView("Light Falloff")?.setEnabled(false)
        getChildView("Light FOV")?.setEnabled(false)
        getChildView("Light Focus")?.setEnabled(false)
        getChildView("Light Ambiance")?.setEnabled(false)
        getChild<TextureCtrl>("light texture control")?.let { it.setEnabled(false); it.setValid(false) }
        getChildView("Flexible1D Checkbox Ctrl")?.setEnabled(false)
        getChildView("FlexNumSections")?.setEnabled(false)
        getChildView("FlexGravity")?.setEnabled(false)
        getChildView("FlexTension")?.setEnabled(false)
        getChildView("FlexFriction")?.setEnabled(false)
        getChildView("FlexWind")?.setEnabled(false)
        getChildView("FlexForceX")?.setEnabled(false)
        getChildView("FlexForceY")?.setEnabled(false)
        getChildView("FlexForceZ")?.setEnabled(false)
        comboMaterial?.setEnabled(false)
        mSpinPhysicsGravity?.setEnabled(false)
        mSpinPhysicsFriction?.setEnabled(false)
        mSpinPhysicsDensity?.setEnabled(false)
        mSpinPhysicsRestitution?.setEnabled(false)
        comboPhysicsShapeType?.setEnabled(false)
    }

    // private alias to avoid repeated null handling in clearCtrls
    private val mSpinPhysicsGravity get() = spinPhysicsGravity
    private val mSpinPhysicsFriction get() = spinPhysicsFriction
    private val mSpinPhysicsDensity get() = spinPhysicsDensity
    private val mSpinPhysicsRestitution get() = spinPhysicsRestitution

    fun refresh() {
        getState()
        if (currentObject?.isDead() == true) currentObject = null
        if (rootObject?.isDead() == true) rootObject = null

        val region = Agent.instance.getRegion()
        val enableMesh = region?.getSimulatorFeatures()?.has("PhysicsShapeTypes") == true
        getChildView("label physicsshapetype")?.setVisible(enableMesh)
        getChildView("Physics Shape Type Combo Ctrl")?.setVisible(enableMesh)
        getChildView("Physics Gravity")?.setVisible(enableMesh)
        getChildView("Physics Friction")?.setVisible(enableMesh)
        getChildView("Physics Density")?.setVisible(enableMesh)
        getChildView("Physics Restitution")?.setVisible(enableMesh)
    }

    private fun getState() {
        var objectp: ViewerObject? = SelectMgr.instance.getSelection().getFirstRootObject()
        var rootObjectp: ViewerObject? = objectp
        if (objectp == null) {
            objectp = SelectMgr.instance.getSelection().getFirstObject()
            if (objectp != null) {
                rootObjectp = objectp.getRootEdit() ?: objectp
            }
        }

        val volobjp: VOVolume? = if (objectp?.getPCode() == LL_PCODE_VOLUME) objectp as? VOVolume else null
        val rootVolobjp: VOVolume? = if (rootObjectp?.getPCode() == LL_PCODE_VOLUME) rootObjectp as? VOVolume else null

        if (objectp == null) {
            System.err.println("PanelVolume: forfeit keyboard focus not yet implemented")
            clearCtrls()
            return
        }

        val editable = rootObjectp?.permModify() == true && rootObjectp?.isPermanentEnforced() == false
        val singleVolume = SelectMgr.instance.selectionAllPCode(LL_PCODE_VOLUME) &&
            SelectMgr.instance.getSelection().getObjectCount() == 1
        val singleRootVolume = SelectMgr.instance.selectionAllPCode(LL_PCODE_VOLUME) &&
            SelectMgr.instance.getSelection().getRootObjectCount() == 1

        if (singleVolume) {
            getChildView("edit_object")?.setVisible(true)
            getChildView("edit_object")?.setEnabled(true)
            getChildView("select_single")?.setVisible(false)
        } else {
            getChildView("edit_object")?.setVisible(false)
            getChildView("select_single")?.setVisible(true)
            getChildView("select_single")?.setEnabled(true)
        }

        val isLight = volobjp?.getIsLight() == true
        getChild<UICtrl>("Light Checkbox Ctrl")?.setValue(isLight)
        getChildView("Light Checkbox Ctrl")?.setEnabled(editable && singleVolume && volobjp != null)

        if (isLight && editable && singleVolume) {
            getChild<ColorSwatchCtrl>("colorswatch")?.let {
                it.setEnabled(true)
                it.setValid(true)
                it.set(volobjp!!.getLightSRGBBaseColor())
            }
            getChild<TextureCtrl>("light texture control")?.let {
                it.setEnabled(true)
                it.setValid(true)
                it.setImageAssetID(volobjp!!.getLightTextureID())
            }
            getChildView("Light Intensity")?.setEnabled(true)
            getChildView("Light Radius")?.setEnabled(true)
            getChildView("Light Falloff")?.setEnabled(true)
            getChildView("Light FOV")?.setEnabled(true)
            getChildView("Light Focus")?.setEnabled(true)
            getChildView("Light Ambiance")?.setEnabled(true)
            getChild<UICtrl>("Light Intensity")?.setValue(volobjp!!.getLightIntensity())
            getChild<UICtrl>("Light Radius")?.setValue(volobjp.getLightRadius())
            getChild<UICtrl>("Light Falloff")?.setValue(volobjp.getLightFalloff())
            val params = volobjp.getSpotLightParams()
            getChild<UICtrl>("Light FOV")?.setValue(params[0])
            getChild<UICtrl>("Light Focus")?.setValue(params[1])
            getChild<UICtrl>("Light Ambiance")?.setValue(params[2])
            lightSavedColor = volobjp.getLightSRGBBaseColor()
        } else {
            getChild<SpinCtrl>("Light Intensity")?.clear()
            getChild<SpinCtrl>("Light Radius")?.clear()
            getChild<SpinCtrl>("Light Falloff")?.clear()
            getChild<ColorSwatchCtrl>("colorswatch")?.let { it.setEnabled(false); it.setValid(false) }
            getChild<TextureCtrl>("light texture control")?.let {
                it.setEnabled(false)
                it.setValid(false)
                val mask = if (objectp?.isAttachment() == true) PERM_COPY or PERM_TRANSFER else PERM_NONE
                it.setImmediateFilterPermMask(mask)
            }
            getChildView("Light Intensity")?.setEnabled(false)
            getChildView("Light Radius")?.setEnabled(false)
            getChildView("Light Falloff")?.setEnabled(false)
            getChildView("Light FOV")?.setEnabled(false)
            getChildView("Light Focus")?.setEnabled(false)
            getChildView("Light Ambiance")?.setEnabled(false)
        }

        val isProbe = volobjp?.isReflectionProbe() == true
        val isMirror = volobjp?.getReflectionProbeIsMirror() == true
        getChild<UICtrl>("Reflection Probe")?.setValue(isProbe)
        getChildView("Reflection Probe")?.setEnabled(editable && singleVolume && volobjp != null && volobjp.isMesh() == false)

        val probeEnabled = isProbe && editable && singleVolume
        val mirrorsEnabled = Pipeline.renderMirrors
        getChildView("Probe Update Type")?.setVisible(mirrorsEnabled)
        getChildView("Probe Update Label")?.setVisible(mirrorsEnabled)
        getChildView("Probe Dynamic")?.setVisible(!mirrorsEnabled)
        getChildView("Probe Dynamic")?.setEnabled(probeEnabled)
        getChildView("Probe Update Type")?.setEnabled(probeEnabled)
        getChildView("Probe Volume Type")?.setEnabled(probeEnabled && !isMirror)
        getChildView("Probe Ambiance")?.setEnabled(probeEnabled && !isMirror)
        getChildView("Probe Near Clip")?.setEnabled(probeEnabled && !isMirror)
        getChildView("Probe Update Label")?.setEnabled(probeEnabled)

        if (!probeEnabled) {
            getChild<ComboBox>("Probe Volume Type")?.clear()
            getChild<SpinCtrl>("Probe Ambiance")?.clear()
            getChild<SpinCtrl>("Probe Near Clip")?.clear()
            getChild<ComboBox>("Probe Update Type")?.clear()
            getChild<UICtrl>("Probe Dynamic")?.setValue(false)
        } else {
            val volumeType = if (volobjp!!.getReflectionProbeIsBox()) "Box" else "Sphere"
            val updateType = when {
                volobjp.getReflectionProbeIsDynamic() && volobjp.getReflectionProbeIsMirror() -> "Dynamic Mirror"
                volobjp.getReflectionProbeIsMirror()                                           -> "Mirror"
                volobjp.getReflectionProbeIsDynamic()                                          -> "Dynamic"
                else                                                                            -> "Static"
            }
            getChild<ComboBox>("Probe Volume Type")?.setValue(volumeType)
            getChild<SpinCtrl>("Probe Ambiance")?.setValue(volobjp.getReflectionProbeAmbiance())
            getChild<SpinCtrl>("Probe Near Clip")?.setValue(volobjp.getReflectionProbeNearClip())
            getChild<ComboBox>("Probe Update Type")?.setValue(updateType)
            getChild<UICtrl>("Probe Dynamic")?.setValue(volobjp.getReflectionProbeIsDynamic())
        }

        val isAnimatedMesh = singleRootVolume && rootVolobjp?.isAnimatedObject() == true
        getChild<UICtrl>("Animated Mesh Checkbox Ctrl")?.setValue(isAnimatedMesh)
        var enableAnimatedObjectBox = rootVolobjp != null && rootVolobjp == volobjp &&
            singleRootVolume && rootVolobjp.canBeAnimatedObject() && editable
        if (enableAnimatedObjectBox && !isAnimatedMesh &&
            rootVolobjp?.isAttachment() == true && AgentAvatar.instance?.canAttachMoreAnimatedObjects() == false) {
            enableAnimatedObjectBox = false
        }
        getChildView("Animated Mesh Checkbox Ctrl")?.setEnabled(enableAnimatedObjectBox)

        rootVolobjp?.let { root ->
            root.refreshBakeTexture()
            for (child in root.getChildren()) {
                child?.refreshBakeTexture()
            }
            AgentAvatar.instance?.updateMeshVisibility()
        }

        val isFlexible = volobjp?.isFlexible() == true
        getChild<UICtrl>("Flexible1D Checkbox Ctrl")?.setValue(isFlexible)
        val canBeFlexible = volobjp?.canBeFlexible() == true
        getChildView("Flexible1D Checkbox Ctrl")?.setEnabled(
            (isFlexible || canBeFlexible) && editable && singleVolume &&
                volobjp?.isMesh() == false && objectp?.isPermanentEnforced() == false
        )

        if (isFlexible && editable && singleVolume) {
            listOf("FlexNumSections","FlexGravity","FlexTension","FlexFriction",
                   "FlexWind","FlexForceX","FlexForceY","FlexForceZ").forEach {
                getChildView(it)?.setVisible(true)
                getChildView(it)?.setEnabled(true)
            }
            val attrs = objectp!!.getFlexibleObjectData()
            getChild<UICtrl>("FlexNumSections")?.setValue(attrs.getSimulateLOD().toFloat())
            getChild<UICtrl>("FlexGravity")?.setValue(attrs.getGravity())
            getChild<UICtrl>("FlexTension")?.setValue(attrs.getTension())
            getChild<UICtrl>("FlexFriction")?.setValue(attrs.getAirFriction())
            getChild<UICtrl>("FlexWind")?.setValue(attrs.getWindSensitivity())
            val force = attrs.getUserForce()
            getChild<UICtrl>("FlexForceX")?.setValue(force[0])
            getChild<UICtrl>("FlexForceY")?.setValue(force[1])
            getChild<UICtrl>("FlexForceZ")?.setValue(force[2])
        } else {
            listOf("FlexNumSections","FlexGravity","FlexTension","FlexFriction",
                   "FlexWind","FlexForceX","FlexForceY","FlexForceZ").forEach {
                getChild<SpinCtrl>(it)?.clear()
                getChildView(it)?.setEnabled(false)
            }
        }

        var materialCode: UByte = 0u
        val materialSame = SelectMgr.instance.getSelection().getSelectedTEValue { obj, te ->
            obj.getMaterial()
        }.also { materialCode = it.first; }
        val legacyFullbrightDesc = Trans.getString("Fullbright")
        val enableMaterial = editable && singleVolume && materialSame.second

        comboMaterial?.setEnabled(enableMaterial)
        if (materialCode == LL_MCODE_LIGHT) {
            if (comboMaterial?.getItemCount() == comboMaterialItemCount) {
                comboMaterial?.add(legacyFullbrightDesc)
            }
            comboMaterial?.setSimple(legacyFullbrightDesc)
        } else {
            if (comboMaterial?.getItemCount() != comboMaterialItemCount) {
                comboMaterial?.remove(legacyFullbrightDesc)
            }
            comboMaterial?.setSimple(MaterialTable.basic.getName(materialCode))
        }

        spinPhysicsGravity?.set(objectp!!.getPhysicsGravity())
        spinPhysicsGravity?.setEnabled(editable)
        spinPhysicsFriction?.set(objectp.getPhysicsFriction())
        spinPhysicsFriction?.setEnabled(editable)
        spinPhysicsDensity?.set(objectp.getPhysicsDensity())
        spinPhysicsDensity?.setEnabled(editable)
        spinPhysicsRestitution?.set(objectp.getPhysicsRestitution())
        spinPhysicsRestitution?.setEnabled(editable)

        comboPhysicsShapeType?.removeAll()
        comboPhysicsShapeType?.add(getString("None"), LLSD(1))

        val sculptParams = objectp.getSculptParams()
        val isMesh = sculptParams?.let {
            (it.getSculptType() and LL_SCULPT_TYPE_MASK) == LL_SCULPT_TYPE_MESH
        } == true

        if (isMesh) {
            val volumeParams = objectp.getVolume()?.getParams()
            val meshId = volumeParams?.getSculptID()
            if (meshId != null && MeshRepo.instance.hasPhysicsShape(meshId)) {
                comboPhysicsShapeType?.add(getString("Prim"), LLSD(0))
            }
        } else {
            comboPhysicsShapeType?.add(getString("Prim"), LLSD(0))
        }
        comboPhysicsShapeType?.add(getString("Convex Hull"), LLSD(2))
        comboPhysicsShapeType?.setValue(LLSD(objectp.getPhysicsShapeType()))
        comboPhysicsShapeType?.setEnabled(
            editable && !objectp.isPermanentEnforced() &&
                (rootObjectp == null || !rootObjectp.isPermanentEnforced())
        )

        currentObject = objectp
        rootObject = rootObjectp

        btnCopyFeatures?.setEnabled(editable && singleVolume && volobjp != null)
        btnPasteFeatures?.setEnabled(
            editable && singleVolume && volobjp != null &&
                !clipboardParams.emptyMap() &&
                (clipboardParams.has("features") || clipboardParams.has("light"))
        )
    }

    private fun refreshCost() {
        System.err.println("PanelVolume: refreshCost not yet implemented")
    }

    fun sendIsLight() {
        System.err.println("PanelVolume: sendIsLight not yet implemented")
    }

    fun sendIsReflectionProbe() {
        System.err.println("PanelVolume: sendIsReflectionProbe not yet implemented")
    }

    fun doSendIsReflectionProbe(notification: LLSD, response: LLSD) {
        System.err.println("PanelVolume: doSendIsReflectionProbe not yet implemented")
    }

    fun sendIsFlexible() {
        System.err.println("PanelVolume: sendIsFlexible not yet implemented")
    }

    private fun sendPhysicsShapeType(ctrl: UICtrl?, userdata: Any?) {
        System.err.println("PanelVolume: sendPhysicsShapeType not yet implemented")
    }

    private fun sendPhysicsGravity(ctrl: UICtrl?, userdata: Any?) {
        System.err.println("PanelVolume: sendPhysicsGravity not yet implemented")
    }

    private fun sendPhysicsFriction(ctrl: UICtrl?, userdata: Any?) {
        System.err.println("PanelVolume: sendPhysicsFriction not yet implemented")
    }

    private fun sendPhysicsRestitution(ctrl: UICtrl?, userdata: Any?) {
        System.err.println("PanelVolume: sendPhysicsRestitution not yet implemented")
    }

    private fun sendPhysicsDensity(ctrl: UICtrl?, userdata: Any?) {
        System.err.println("PanelVolume: sendPhysicsDensity not yet implemented")
    }

    private fun handleResponseChangeToFlexible(notification: LLSD, response: LLSD) {
        System.err.println("PanelVolume: handleResponseChangeToFlexible not yet implemented")
    }

    fun onLightCancelColor(data: LLSD) {
        System.err.println("PanelVolume: onLightCancelColor not yet implemented")
    }

    fun onLightSelectColor(data: LLSD) {
        System.err.println("PanelVolume: onLightSelectColor not yet implemented")
    }

    fun onLightCancelTexture(data: LLSD) {
        System.err.println("PanelVolume: onLightCancelTexture not yet implemented")
    }

    fun onLightSelectTexture(data: LLSD) {
        System.err.println("PanelVolume: onLightSelectTexture not yet implemented")
    }

    fun onCopyFeatures() {
        System.err.println("PanelVolume: onCopyFeatures not yet implemented")
    }

    fun onPasteFeatures() {
        System.err.println("PanelVolume: onPasteFeatures not yet implemented")
    }

    fun onCopyLight() {
        System.err.println("PanelVolume: onCopyLight not yet implemented")
    }

    fun onPasteLight() {
        System.err.println("PanelVolume: onPasteLight not yet implemented")
    }

    fun onFSCopyFeatures() {
        System.err.println("PanelVolume: onFSCopyFeatures not yet implemented")
    }

    fun onFSPasteFeatures() {
        System.err.println("PanelVolume: onFSPasteFeatures not yet implemented")
    }

    fun onCommitIsFlexible(ctrl: UICtrl?, userdata: Any?) {
        System.err.println("PanelVolume: onCommitIsFlexible not yet implemented")
    }

    fun onCommitAnimatedMeshCheckbox(ctrl: UICtrl?, userdata: Any?) {
        System.err.println("PanelVolume: onCommitAnimatedMeshCheckbox not yet implemented")
    }

    companion object {
        @JvmStatic fun precommitValidate(data: LLSD): Boolean = true

        @JvmStatic fun onCommitIsLight(ctrl: UICtrl?, userdata: Any?) {
            (userdata as? PanelVolume)?.sendIsLight()
        }

        @JvmStatic fun onCommitLight(ctrl: UICtrl?, userdata: Any?) {
            System.err.println("PanelVolume: onCommitLight not yet implemented")
        }

        @JvmStatic fun onCommitIsReflectionProbe(ctrl: UICtrl?, userdata: Any?) {
            (userdata as? PanelVolume)?.sendIsReflectionProbe()
        }

        @JvmStatic fun onCommitProbe(ctrl: UICtrl?, userdata: Any?) {
            System.err.println("PanelVolume: onCommitProbe not yet implemented")
        }

        @JvmStatic fun onCommitFlexible(ctrl: UICtrl?, userdata: Any?) {
            System.err.println("PanelVolume: onCommitFlexible not yet implemented")
        }

        @JvmStatic fun onCommitPhysicsParam(ctrl: UICtrl?, userdata: Any?) {
            System.err.println("PanelVolume: onCommitPhysicsParam not yet implemented")
        }

        @JvmStatic fun onCommitMaterial(ctrl: UICtrl?, userdata: Any?) {
            System.err.println("PanelVolume: onCommitMaterial not yet implemented")
        }

        @JvmStatic fun setLightTextureID(assetId: Uuid, itemId: Uuid, volobjp: VOVolume?) {
            System.err.println("PanelVolume: setLightTextureID not yet implemented")
        }
    }
}
