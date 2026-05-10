package com.firestorm.newview

import kotlin.math.PI

// ---------------------------------------------------------------------------
// Field name constants
// ---------------------------------------------------------------------------

private const val FIELD_SKY_AMBIENT_LIGHT          = "ambient_light"
private const val FIELD_SKY_BLUE_HORIZON           = "blue_horizon"
private const val FIELD_SKY_BLUE_DENSITY           = "blue_density"
private const val FIELD_SKY_SUN_COLOR              = "sun_color"
private const val FIELD_SKY_CLOUD_COLOR            = "cloud_color"
private const val FIELD_SKY_HAZE_HORIZON           = "haze_horizon"
private const val FIELD_SKY_HAZE_DENSITY           = "haze_density"
private const val FIELD_SKY_CLOUD_COVERAGE         = "cloud_coverage"
private const val FIELD_SKY_CLOUD_MAP              = "cloud_map"
private const val FIELD_WATER_NORMAL_MAP           = "water_normal_map"
private const val FIELD_SKY_CLOUD_SCALE            = "cloud_scale"
private const val FIELD_SKY_SCENE_GAMMA            = "scene_gamma"
private const val FIELD_SKY_SUN_ROTATION           = "sun_rotation"
private const val FIELD_SKY_SUN_AZIMUTH            = "sun_azimuth"
private const val FIELD_SKY_SUN_ELEVATION          = "sun_elevation"
private const val FIELD_SKY_SUN_SCALE              = "sun_scale"
private const val FIELD_SKY_GLOW_FOCUS             = "glow_focus"
private const val FIELD_SKY_GLOW_SIZE              = "glow_size"
private const val FIELD_SKY_STAR_BRIGHTNESS        = "star_brightness"
private const val FIELD_SKY_MOON_ROTATION          = "moon_rotation"
private const val FIELD_SKY_MOON_AZIMUTH           = "moon_azimuth"
private const val FIELD_SKY_MOON_ELEVATION         = "moon_elevation"
private const val FIELD_REFLECTION_PROBE_AMBIANCE  = "probe_ambiance"
private const val BTN_RESET                        = "btn_reset"

private const val SLIDER_SCALE_SUN_AMBIENT               = 3.0f
private const val SLIDER_SCALE_BLUE_HORIZON_DENSITY      = 2.0f
private const val SLIDER_SCALE_GLOW_R                    = 20.0f
private const val SLIDER_SCALE_GLOW_B                    = -5.0f

// Version sentinel: updates with this version are from this floater, not external
private const val FLOATER_ENVIRONMENT_UPDATE = -2

// ---------------------------------------------------------------------------
// Minimal stubs for environment types
// ---------------------------------------------------------------------------

data class LLColor3(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f) {
    operator fun times(scale: Float) = LLColor3(r * scale, g * scale, b * scale)
    operator fun div(scale: Float) = LLColor3(r / scale, g / scale, b / scale)
    val mV get() = floatArrayOf(r, g, b)
}

data class LLQuaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f)

interface LLSettingsSky {
    fun getAmbientColor(): LLColor3
    fun setAmbientColor(c: LLColor3)
    fun getBlueHorizon(): LLColor3
    fun setBlueHorizon(c: LLColor3)
    fun getBlueDensity(): LLColor3
    fun setBlueDensity(c: LLColor3)
    fun getHazeHorizon(): Float
    fun setHazeHorizon(v: Float)
    fun getHazeDensity(): Float
    fun setHazeDensity(v: Float)
    fun getGamma(): Float
    fun setGamma(v: Float)
    fun getCloudColor(): LLColor3
    fun setCloudColor(c: LLColor3)
    fun getCloudShadow(): Float
    fun setCloudShadow(v: Float)
    fun getCloudScale(): Float
    fun setCloudScale(v: Float)
    fun getSunlightColor(): LLColor3
    fun setSunlightColor(c: LLColor3)
    fun getCloudNoiseTextureId(): String
    fun setCloudNoiseTextureId(id: String)
    fun getReflectionProbeAmbiance(autoAdjust: Boolean): Float
    fun setReflectionProbeAmbiance(v: Float)
    fun getGlow(): LLColor3
    fun setGlow(c: LLColor3)
    fun getStarBrightness(): Float
    fun setStarBrightness(v: Float)
    fun getSunRotation(): LLQuaternion
    fun setSunRotation(q: LLQuaternion)
    fun getSunScale(): Float
    fun setSunScale(v: Float)
    fun getMoonRotation(): LLQuaternion
    fun setMoonRotation(q: LLQuaternion)
    fun buildClone(): LLSettingsSky
    fun update()
}

interface LLSettingsWater {
    fun getNormalMapID(): String
    fun setNormalMapID(id: String)
    fun buildClone(): LLSettingsWater
    fun update()
}

// ---------------------------------------------------------------------------
// Minimal LLEnvironment stub
// ---------------------------------------------------------------------------

private object LLEnvironment {
    enum class EnvSelection { ENV_LOCAL, ENV_PARCEL, ENV_REGION, ENV_EDIT, ENV_CURRENT }

    enum class ETransition { TRANSITION_INSTANT }

    typealias ConnectionT = Any

    fun instance(): LLEnvironment = TODO("APR: use JVM equivalent for LLEnvironment singleton")

    fun hasEnvironment(sel: EnvSelection): Boolean = TODO("APR: use JVM equivalent")
    fun getEnvironmentDay(sel: EnvSelection): Any? = TODO("APR: use JVM equivalent")
    fun getEnvironmentFixedSky(sel: EnvSelection, extend: Boolean = false): LLSettingsSky =
        TODO("APR: use JVM equivalent")
    fun getEnvironmentFixedWater(sel: EnvSelection, extend: Boolean = false): LLSettingsWater =
        TODO("APR: use JVM equivalent")
    fun setEnvironment(sel: EnvSelection, sky: LLSettingsSky, version: Int = 0) {}
    fun setEnvironment(sel: EnvSelection, water: LLSettingsWater, version: Int = 0) {}
    fun setSelectedEnvironment(sel: EnvSelection, transition: ETransition = ETransition.TRANSITION_INSTANT) {}
    fun clearEnvironment(sel: EnvSelection) {}
    fun saveBeaconsState() {}
    fun revertBeaconsState() {}
    fun updateEnvironment(transition: ETransition, immediate: Boolean) {}
    fun setEnvironmentChanged(cb: (EnvSelection, Int) -> Unit): ConnectionT = TODO("APR: use JVM equivalent")
}

// ---------------------------------------------------------------------------
// LLVirtualTrackball helper (stubs)
// ---------------------------------------------------------------------------

private object LLVirtualTrackball {
    fun getAzimuthAndElevationDeg(q: LLQuaternion): Pair<Float, Float> =
        TODO("GPU: compute azimuth/elevation from quaternion")

    fun quaternionFromAzimElevDeg(azimuthDeg: Float, elevationDeg: Float): LLQuaternion =
        TODO("GPU: build quaternion from azimuth and elevation angles")
}

// ---------------------------------------------------------------------------
// LLFloaterEnvironmentAdjust
// ---------------------------------------------------------------------------

class LLFloaterEnvironmentAdjust(key: Any) {

    private var liveSky: LLSettingsSky? = null
    private var liveWater: LLSettingsWater? = null
    private var eventConnection: LLEnvironment.ConnectionT? = null

    // Child widget accessors — resolved by the UI framework at runtime
    private fun getColorSwatchValue(field: String): LLColor3 =
        TODO("GPU: getChild<LLColorSwatchCtrl>($field).get()")

    private fun setColorSwatchValue(field: String, c: LLColor3) =
        TODO("GPU: getChild<LLColorSwatchCtrl>($field).set($c)")

    private fun getFloatValue(field: String): Float =
        TODO("GPU: getChild<LLUICtrl>($field).getValue().asReal().toFloat()")

    private fun setFloatValue(field: String, v: Float) =
        TODO("GPU: getChild<LLUICtrl>($field).setValue($v)")

    private fun setFieldCommitCallback(field: String, cb: () -> Unit) =
        TODO("GPU: getChild<LLUICtrl>($field).setCommitCallback { cb() }")

    private fun setTextureFieldCommitCallback(field: String, cb: () -> Unit) =
        TODO("GPU: getChild<LLTextureCtrl>($field).setCommitCallback { cb() }")

    private fun getTextureFieldValue(field: String): String =
        TODO("GPU: getChild<LLTextureCtrl>($field).getValue().asUUID()")

    private fun setTextureFieldValue(field: String, id: String) =
        TODO("GPU: getChild<LLTextureCtrl>($field).setValue($id)")

    private fun getTrackballRotation(field: String): LLQuaternion =
        TODO("GPU: getChild<LLVirtualTrackball>($field).getRotation()")

    private fun setTrackballRotation(field: String, q: LLQuaternion) =
        TODO("GPU: getChild<LLVirtualTrackball>($field).setRotation($q)")

    private fun setAllChildrenEnabled(enabled: Boolean) =
        TODO("GPU: setAllChildrenEnabled($enabled)")

    private fun setEnabled(enabled: Boolean) =
        TODO("GPU: setEnabled($enabled)")

    private fun childSetValue(child: String, value: Any) =
        TODO("GPU: childSetValue($child, $value)")

    private fun getString(key: String): String =
        TODO("GPU: getString($key)")

    private fun closeFloater() =
        TODO("GPU: closeFloater()")

    open fun postBuild(): Boolean {
        setFieldCommitCallback(FIELD_SKY_AMBIENT_LIGHT)    { onAmbientLightChanged() }
        setFieldCommitCallback(FIELD_SKY_BLUE_HORIZON)     { onBlueHorizonChanged() }
        setFieldCommitCallback(FIELD_SKY_BLUE_DENSITY)     { onBlueDensityChanged() }
        setFieldCommitCallback(FIELD_SKY_HAZE_HORIZON)     { onHazeHorizonChanged() }
        setFieldCommitCallback(FIELD_SKY_HAZE_DENSITY)     { onHazeDensityChanged() }
        setFieldCommitCallback(FIELD_SKY_SCENE_GAMMA)      { onSceneGammaChanged() }
        setFieldCommitCallback(FIELD_SKY_CLOUD_COLOR)      { onCloudColorChanged() }
        setFieldCommitCallback(FIELD_SKY_CLOUD_COVERAGE)   { onCloudCoverageChanged() }
        setFieldCommitCallback(FIELD_SKY_CLOUD_SCALE)      { onCloudScaleChanged() }
        setFieldCommitCallback(FIELD_SKY_SUN_COLOR)        { onSunColorChanged() }
        setFieldCommitCallback(FIELD_SKY_GLOW_FOCUS)       { onGlowChanged() }
        setFieldCommitCallback(FIELD_SKY_GLOW_SIZE)        { onGlowChanged() }
        setFieldCommitCallback(FIELD_SKY_STAR_BRIGHTNESS)  { onStarBrightnessChanged() }
        setFieldCommitCallback(FIELD_SKY_SUN_ROTATION)     { onSunRotationChanged() }
        setFieldCommitCallback(FIELD_SKY_SUN_AZIMUTH)      { onSunAzimElevChanged() }
        setFieldCommitCallback(FIELD_SKY_SUN_ELEVATION)    { onSunAzimElevChanged() }
        setFieldCommitCallback(FIELD_SKY_SUN_SCALE)        { onSunScaleChanged() }
        setFieldCommitCallback(FIELD_SKY_MOON_ROTATION)    { onMoonRotationChanged() }
        setFieldCommitCallback(FIELD_SKY_MOON_AZIMUTH)     { onMoonAzimElevChanged() }
        setFieldCommitCallback(FIELD_SKY_MOON_ELEVATION)   { onMoonAzimElevChanged() }
        setFieldCommitCallback(BTN_RESET)                  { onButtonReset() }

        setTextureFieldCommitCallback(FIELD_SKY_CLOUD_MAP)    { onCloudMapChanged() }
        setTextureFieldCommitCallback(FIELD_WATER_NORMAL_MAP) { onWaterMapChanged() }

        setFieldCommitCallback(FIELD_REFLECTION_PROBE_AMBIANCE) { onReflectionProbeAmbianceChanged() }

        refresh()
        return true
    }

    open fun onOpen(key: Any) {
        if (liveSky == null) {
            LLEnvironment.instance().saveBeaconsState()
        }
        captureCurrentEnvironment()

        eventConnection = LLEnvironment.instance().setEnvironmentChanged { env, version ->
            onEnvironmentUpdated(env, version)
        }

        // Resume reflection map manager; setEnvironmentChanged may pause it (SL-20456)
        TODO("GPU: gPipeline.mReflectionMapManager.resume()")
        refresh()
    }

    open fun onClose(appQuitting: Boolean) {
        LLEnvironment.instance().revertBeaconsState()
        TODO("APR: use JVM equivalent for eventConnection.disconnect()")
        liveSky = null
        liveWater = null
    }

    open fun refresh() {
        val sky = liveSky
        val water = liveWater
        if (sky == null || water == null) {
            setAllChildrenEnabled(false)
            return
        }

        setEnabled(true)
        setAllChildrenEnabled(true)

        setColorSwatchValue(FIELD_SKY_AMBIENT_LIGHT, sky.getAmbientColor() / SLIDER_SCALE_SUN_AMBIENT)
        setColorSwatchValue(FIELD_SKY_BLUE_HORIZON,  sky.getBlueHorizon()  / SLIDER_SCALE_BLUE_HORIZON_DENSITY)
        setColorSwatchValue(FIELD_SKY_BLUE_DENSITY,  sky.getBlueDensity()  / SLIDER_SCALE_BLUE_HORIZON_DENSITY)
        setFloatValue(FIELD_SKY_HAZE_HORIZON, sky.getHazeHorizon())
        setFloatValue(FIELD_SKY_HAZE_DENSITY, sky.getHazeDensity())
        setFloatValue(FIELD_SKY_SCENE_GAMMA,  sky.getGamma())
        setColorSwatchValue(FIELD_SKY_CLOUD_COLOR, sky.getCloudColor())
        setFloatValue(FIELD_SKY_CLOUD_COVERAGE, sky.getCloudShadow())
        setFloatValue(FIELD_SKY_CLOUD_SCALE,    sky.getCloudScale())
        setColorSwatchValue(FIELD_SKY_SUN_COLOR, sky.getSunlightColor() / SLIDER_SCALE_SUN_AMBIENT)

        setTextureFieldValue(FIELD_SKY_CLOUD_MAP,    sky.getCloudNoiseTextureId())
        setTextureFieldValue(FIELD_WATER_NORMAL_MAP, water.getNormalMapID())

        val shouldAutoAdjust = TODO<Boolean>("APR: use JVM equivalent for gSavedSettings.getBOOL(\"RenderSkyAutoAdjustLegacy\")")
        setFloatValue(FIELD_REFLECTION_PROBE_AMBIANCE, sky.getReflectionProbeAmbiance(shouldAutoAdjust))

        val glow = sky.getGlow()
        // UI range 0–1.99 corresponds to raw range 40–0.2 for glow.r
        setFloatValue(FIELD_SKY_GLOW_SIZE,       2.0f - (glow.mV[0] / SLIDER_SCALE_GLOW_R))
        setFloatValue(FIELD_SKY_GLOW_FOCUS,      glow.mV[2] / SLIDER_SCALE_GLOW_B)
        setFloatValue(FIELD_SKY_STAR_BRIGHTNESS, sky.getStarBrightness())
        setFloatValue(FIELD_SKY_SUN_SCALE,       sky.getSunScale())

        val (sunAzimuth, sunElevation) = LLVirtualTrackball.getAzimuthAndElevationDeg(sky.getSunRotation())
        setFloatValue(FIELD_SKY_SUN_AZIMUTH,   sunAzimuth)
        setFloatValue(FIELD_SKY_SUN_ELEVATION, sunElevation)
        setTrackballRotation(FIELD_SKY_SUN_ROTATION, sky.getSunRotation())

        val (moonAzimuth, moonElevation) = LLVirtualTrackball.getAzimuthAndElevationDeg(sky.getMoonRotation())
        setFloatValue(FIELD_SKY_MOON_AZIMUTH,   moonAzimuth)
        setFloatValue(FIELD_SKY_MOON_ELEVATION, moonElevation)
        setTrackballRotation(FIELD_SKY_MOON_ROTATION, sky.getMoonRotation())

        updateGammaLabel()
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun captureCurrentEnvironment() {
        val env = LLEnvironment.instance()
        var updateLocal = false

        if (env.hasEnvironment(LLEnvironment.EnvSelection.ENV_LOCAL)) {
            if (env.getEnvironmentDay(LLEnvironment.EnvSelection.ENV_LOCAL) != null) {
                // Full day cycle in local env: freeze into a fixed sky/water clone
                liveSky   = env.getEnvironmentFixedSky(LLEnvironment.EnvSelection.ENV_LOCAL).buildClone()
                liveWater = env.getEnvironmentFixedWater(LLEnvironment.EnvSelection.ENV_LOCAL).buildClone()
                updateLocal = true
            } else {
                liveSky   = env.getEnvironmentFixedSky(LLEnvironment.EnvSelection.ENV_LOCAL)
                liveWater = env.getEnvironmentFixedWater(LLEnvironment.EnvSelection.ENV_LOCAL)
            }
        } else {
            liveSky   = env.getEnvironmentFixedSky(LLEnvironment.EnvSelection.ENV_PARCEL, true).buildClone()
            liveWater = env.getEnvironmentFixedWater(LLEnvironment.EnvSelection.ENV_PARCEL, true).buildClone()
            updateLocal = true
        }

        if (updateLocal) {
            env.setEnvironment(LLEnvironment.EnvSelection.ENV_LOCAL, liveSky!!, FLOATER_ENVIRONMENT_UPDATE)
            env.setEnvironment(LLEnvironment.EnvSelection.ENV_LOCAL, liveWater!!, FLOATER_ENVIRONMENT_UPDATE)
        }
        env.setSelectedEnvironment(LLEnvironment.EnvSelection.ENV_LOCAL, LLEnvironment.ETransition.TRANSITION_INSTANT)
    }

    private fun onButtonReset() {
        TODO("APR: use JVM equivalent for LLNotificationsUtil::add(\"PersonalSettingsConfirmReset\", ...) with confirm callback closing floater and clearing ENV_LOCAL")
    }

    private fun onAmbientLightChanged() {
        liveSky?.setAmbientColor(getColorSwatchValue(FIELD_SKY_AMBIENT_LIGHT) * SLIDER_SCALE_SUN_AMBIENT)
        liveSky?.update()
    }

    private fun onBlueHorizonChanged() {
        liveSky?.setBlueHorizon(getColorSwatchValue(FIELD_SKY_BLUE_HORIZON) * SLIDER_SCALE_BLUE_HORIZON_DENSITY)
        liveSky?.update()
    }

    private fun onBlueDensityChanged() {
        liveSky?.setBlueDensity(getColorSwatchValue(FIELD_SKY_BLUE_DENSITY) * SLIDER_SCALE_BLUE_HORIZON_DENSITY)
        liveSky?.update()
    }

    private fun onHazeHorizonChanged() {
        liveSky?.setHazeHorizon(getFloatValue(FIELD_SKY_HAZE_HORIZON))
        liveSky?.update()
    }

    private fun onHazeDensityChanged() {
        liveSky?.setHazeDensity(getFloatValue(FIELD_SKY_HAZE_DENSITY))
        liveSky?.update()
    }

    private fun onSceneGammaChanged() {
        liveSky?.setGamma(getFloatValue(FIELD_SKY_SCENE_GAMMA))
        liveSky?.update()
    }

    private fun onCloudColorChanged() {
        liveSky?.setCloudColor(getColorSwatchValue(FIELD_SKY_CLOUD_COLOR))
        liveSky?.update()
    }

    private fun onCloudCoverageChanged() {
        liveSky?.setCloudShadow(getFloatValue(FIELD_SKY_CLOUD_COVERAGE))
        liveSky?.update()
    }

    private fun onCloudScaleChanged() {
        liveSky?.setCloudScale(getFloatValue(FIELD_SKY_CLOUD_SCALE))
        liveSky?.update()
    }

    private fun onGlowChanged() {
        val glowR = getFloatValue(FIELD_SKY_GLOW_SIZE)
        val glowB = getFloatValue(FIELD_SKY_GLOW_FOCUS)
        // UI range 0–1.99 → raw range 40–0.2
        val raw = LLColor3(
            r = (2.0f - glowR) * SLIDER_SCALE_GLOW_R,
            g = 0.0f,
            b = glowB * SLIDER_SCALE_GLOW_B
        )
        liveSky?.setGlow(raw)
        liveSky?.update()
    }

    private fun onStarBrightnessChanged() {
        liveSky?.setStarBrightness(getFloatValue(FIELD_SKY_STAR_BRIGHTNESS))
        liveSky?.update()
    }

    private fun onSunRotationChanged() {
        val quat = getTrackballRotation(FIELD_SKY_SUN_ROTATION)
        val (azimuth, elevation) = LLVirtualTrackball.getAzimuthAndElevationDeg(quat)
        setFloatValue(FIELD_SKY_SUN_AZIMUTH,   azimuth)
        setFloatValue(FIELD_SKY_SUN_ELEVATION, elevation)
        liveSky?.setSunRotation(quat)
        liveSky?.update()
    }

    private fun onSunAzimElevChanged() {
        val azimuthDeg   = getFloatValue(FIELD_SKY_SUN_AZIMUTH)
        val elevationDeg = getFloatValue(FIELD_SKY_SUN_ELEVATION)
        val quat = buildQuatFromAzimElev(azimuthDeg, elevationDeg)
        setTrackballRotation(FIELD_SKY_SUN_ROTATION, quat)
        liveSky?.setSunRotation(quat)
        liveSky?.update()
    }

    private fun onSunScaleChanged() {
        liveSky?.setSunScale(getFloatValue(FIELD_SKY_SUN_SCALE))
        liveSky?.update()
    }

    private fun onMoonRotationChanged() {
        val quat = getTrackballRotation(FIELD_SKY_MOON_ROTATION)
        val (azimuth, elevation) = LLVirtualTrackball.getAzimuthAndElevationDeg(quat)
        setFloatValue(FIELD_SKY_MOON_AZIMUTH,   azimuth)
        setFloatValue(FIELD_SKY_MOON_ELEVATION, elevation)
        liveSky?.setMoonRotation(quat)
        liveSky?.update()
    }

    private fun onMoonAzimElevChanged() {
        val azimuthDeg   = getFloatValue(FIELD_SKY_MOON_AZIMUTH)
        val elevationDeg = getFloatValue(FIELD_SKY_MOON_ELEVATION)
        val quat = buildQuatFromAzimElev(azimuthDeg, elevationDeg)
        setTrackballRotation(FIELD_SKY_MOON_ROTATION, quat)
        liveSky?.setMoonRotation(quat)
        liveSky?.update()
    }

    private fun onCloudMapChanged() {
        val sky = liveSky ?: return
        val newId = getTextureFieldValue(FIELD_SKY_CLOUD_MAP)

        LLEnvironment.instance().setSelectedEnvironment(LLEnvironment.EnvSelection.ENV_LOCAL)
        val cloned = sky.buildClone()
        cloned.setCloudNoiseTextureId(newId)
        LLEnvironment.instance().setEnvironment(LLEnvironment.EnvSelection.ENV_LOCAL, cloned)
        LLEnvironment.instance().updateEnvironment(LLEnvironment.ETransition.TRANSITION_INSTANT, true)
        setTextureFieldValue(FIELD_SKY_CLOUD_MAP, newId)
    }

    private fun onWaterMapChanged() {
        liveWater?.setNormalMapID(getTextureFieldValue(FIELD_WATER_NORMAL_MAP))
        liveWater?.update()
    }

    private fun onSunColorChanged() {
        val color = getColorSwatchValue(FIELD_SKY_SUN_COLOR) * SLIDER_SCALE_SUN_AMBIENT
        liveSky?.setSunlightColor(color)
        liveSky?.update()
    }

    private fun onReflectionProbeAmbianceChanged() {
        val sky = liveSky ?: return
        sky.setReflectionProbeAmbiance(getFloatValue(FIELD_REFLECTION_PROBE_AMBIANCE))
        updateGammaLabel()
        sky.update()
    }

    private fun updateGammaLabel() {
        val sky = liveSky ?: return
        val shouldAutoAdjust = TODO<Boolean>("APR: use JVM equivalent for gSavedSettings.getBOOL(\"RenderSkyAutoAdjustLegacy\")")
        val ambiance = sky.getReflectionProbeAmbiance(shouldAutoAdjust)
        if (ambiance != 0f) {
            childSetValue("scene_gamma_label", getString("hdr_string"))
            setFloatValue(FIELD_SKY_SCENE_GAMMA + "_tooltip", 0f) // tooltip set via GPU layer
        } else {
            childSetValue("scene_gamma_label", getString("brightness_string"))
        }
    }

    private fun onEnvironmentUpdated(env: LLEnvironment.EnvSelection, version: Int) {
        if (env == LLEnvironment.EnvSelection.ENV_LOCAL && version != FLOATER_ENVIRONMENT_UPDATE) {
            captureCurrentEnvironment()
            refresh()
        }
    }

    // Build a quaternion from azimuth/elevation in degrees.
    // Mirrors the C++ LLVirtualTrackball approach: -elevation around Y, then (2π - azimuth) around Z.
    private fun buildQuatFromAzimElev(azimuthDeg: Float, elevationDeg: Float): LLQuaternion {
        val DEG_TO_RAD = (PI / 180.0).toFloat()
        val F_TWO_PI   = (2.0 * PI).toFloat()
        val F_APPROX_ZERO = 0.001f

        var az = azimuthDeg * DEG_TO_RAD
        var el = elevationDeg * DEG_TO_RAD
        if (el == 0f) el = F_APPROX_ZERO

        // Quaternion for -elevation around Y axis
        val halfEl   = -el / 2f
        val qElevX   = 0f
        val qElevY   = kotlin.math.sin(halfEl)
        val qElevZ   = 0f
        val qElevW   = kotlin.math.cos(halfEl)

        // Quaternion for (2π - azimuth) around Z axis
        val azAngle  = F_TWO_PI - az
        val halfAz   = azAngle / 2f
        val qAzX     = 0f
        val qAzY     = 0f
        val qAzZ     = kotlin.math.sin(halfAz)
        val qAzW     = kotlin.math.cos(halfAz)

        // Hamilton product: qElev * qAz
        return LLQuaternion(
            x = qElevW * qAzX + qElevX * qAzW + qElevY * qAzZ - qElevZ * qAzY,
            y = qElevW * qAzY - qElevX * qAzZ + qElevY * qAzW + qElevZ * qAzX,
            z = qElevW * qAzZ + qElevX * qAzY - qElevY * qAzX + qElevZ * qAzW,
            w = qElevW * qAzW - qElevX * qAzX - qElevY * qAzY - qElevZ * qAzZ
        )
    }
}
