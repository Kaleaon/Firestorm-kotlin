package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.UICtrl
import com.firestorm.llui.TextureCtrl
import com.firestorm.llcommon.LLUUID
import kotlin.math.PI

// Floater that lets a user snapshot the current environment and make quick
// incremental sky/water adjustments without entering a full day-cycle editor.
// Mirrors llfloaterenvironmentadjust.h/.cpp (the closest available analog to
// llfloaterenvironmentsettings in this checkout).
class FloaterEnvironmentSettings(seed: Any) : Floater(seed) {

    companion object {
        // Field name constants mirror the C++ anonymous-namespace strings.
        const val FIELD_SKY_AMBIENT_LIGHT     = "ambient_light"
        const val FIELD_SKY_BLUE_HORIZON      = "blue_horizon"
        const val FIELD_SKY_BLUE_DENSITY      = "blue_density"
        const val FIELD_SKY_SUN_COLOR         = "sun_color"
        const val FIELD_SKY_CLOUD_COLOR       = "cloud_color"
        const val FIELD_SKY_HAZE_HORIZON      = "haze_horizon"
        const val FIELD_SKY_HAZE_DENSITY      = "haze_density"
        const val FIELD_SKY_CLOUD_COVERAGE    = "cloud_coverage"
        const val FIELD_SKY_CLOUD_MAP         = "cloud_map"
        const val FIELD_WATER_NORMAL_MAP      = "water_normal_map"
        const val FIELD_SKY_CLOUD_SCALE       = "cloud_scale"
        const val FIELD_SKY_SCENE_GAMMA       = "scene_gamma"
        const val FIELD_SKY_SUN_ROTATION      = "sun_rotation"
        const val FIELD_SKY_SUN_AZIMUTH       = "sun_azimuth"
        const val FIELD_SKY_SUN_ELEVATION     = "sun_elevation"
        const val FIELD_SKY_SUN_SCALE         = "sun_scale"
        const val FIELD_SKY_GLOW_FOCUS        = "glow_focus"
        const val FIELD_SKY_GLOW_SIZE         = "glow_size"
        const val FIELD_SKY_STAR_BRIGHTNESS   = "star_brightness"
        const val FIELD_SKY_MOON_ROTATION     = "moon_rotation"
        const val FIELD_SKY_MOON_AZIMUTH      = "moon_azimuth"
        const val FIELD_SKY_MOON_ELEVATION    = "moon_elevation"
        const val FIELD_REFLECTION_PROBE_AMBIANCE = "probe_ambiance"
        const val BTN_RESET                   = "btn_reset"

        const val SLIDER_SCALE_SUN_AMBIENT          = 3.0f
        const val SLIDER_SCALE_BLUE_HORIZON_DENSITY = 2.0f
        const val SLIDER_SCALE_GLOW_R               = 20.0f
        const val SLIDER_SCALE_GLOW_B               = -5.0f

        // Sentinel version tag that distinguishes updates originated by this
        // floater from those that come from external sources.
        const val FLOATER_ENVIRONMENT_UPDATE = -2
    }

    // Live settings objects that receive incremental edits.
    private var liveSky:   Any? = null
    private var liveWater: Any? = null
    private var eventConnection: Any? = null

    override fun postBuild(): Boolean {
        bindCommit(FIELD_SKY_AMBIENT_LIGHT)    { onAmbientLightChanged() }
        bindCommit(FIELD_SKY_BLUE_HORIZON)     { onBlueHorizonChanged() }
        bindCommit(FIELD_SKY_BLUE_DENSITY)     { onBlueDensityChanged() }
        bindCommit(FIELD_SKY_HAZE_HORIZON)     { onHazeHorizonChanged() }
        bindCommit(FIELD_SKY_HAZE_DENSITY)     { onHazeDensityChanged() }
        bindCommit(FIELD_SKY_SCENE_GAMMA)      { onSceneGammaChanged() }
        bindCommit(FIELD_SKY_CLOUD_COLOR)      { onCloudColorChanged() }
        bindCommit(FIELD_SKY_CLOUD_COVERAGE)   { onCloudCoverageChanged() }
        bindCommit(FIELD_SKY_CLOUD_SCALE)      { onCloudScaleChanged() }
        bindCommit(FIELD_SKY_SUN_COLOR)        { onSunColorChanged() }
        bindCommit(FIELD_SKY_GLOW_FOCUS)       { onGlowChanged() }
        bindCommit(FIELD_SKY_GLOW_SIZE)        { onGlowChanged() }
        bindCommit(FIELD_SKY_STAR_BRIGHTNESS)  { onStarBrightnessChanged() }
        bindCommit(FIELD_SKY_SUN_ROTATION)     { onSunRotationChanged() }
        bindCommit(FIELD_SKY_SUN_AZIMUTH)      { onSunAzimElevChanged() }
        bindCommit(FIELD_SKY_SUN_ELEVATION)    { onSunAzimElevChanged() }
        bindCommit(FIELD_SKY_SUN_SCALE)        { onSunScaleChanged() }
        bindCommit(FIELD_SKY_MOON_ROTATION)    { onMoonRotationChanged() }
        bindCommit(FIELD_SKY_MOON_AZIMUTH)     { onMoonAzimElevChanged() }
        bindCommit(FIELD_SKY_MOON_ELEVATION)   { onMoonAzimElevChanged() }
        bindCommit(BTN_RESET)                  { onButtonReset() }
        bindCommit(FIELD_SKY_CLOUD_MAP)        { onCloudMapChanged() }
        bindCommit(FIELD_WATER_NORMAL_MAP)     { onWaterMapChanged() }
        bindCommit(FIELD_REFLECTION_PROBE_AMBIANCE) { onReflectionProbeAmbianceChanged() }

        refresh()
        return true
    }

    override fun onOpen(key: Any) {
        if (liveSky == null) {
            // APR: LLEnvironment::instance().saveBeaconsState()
        }
        captureCurrentEnvironment()
        eventConnection = null // APR: LLEnvironment::instance().setEnvironmentChanged(::onEnvironmentUpdated)
        // GPU: gPipeline.mReflectionMapManager.resume()
        refresh()
    }

    override fun onClose(appQuitting: Boolean) {
        // APR: LLEnvironment::instance().revertBeaconsState()
        eventConnection = null
        liveSky = null
        liveWater = null
    }

    override fun refresh() {
        if (liveSky == null || liveWater == null) {
            setAllChildrenEnabled(false)
            return
        }
        setAllChildrenEnabled(true)

        // Sky colour fields – raw values scaled to [0,1] UI range.
        setChildValue(FIELD_SKY_AMBIENT_LIGHT,  null) // APR: liveSky.getAmbientColor() / SLIDER_SCALE_SUN_AMBIENT
        setChildValue(FIELD_SKY_BLUE_HORIZON,   null) // APR: liveSky.getBlueHorizon() / SLIDER_SCALE_BLUE_HORIZON_DENSITY
        setChildValue(FIELD_SKY_BLUE_DENSITY,   null) // APR: liveSky.getBlueDensity() / SLIDER_SCALE_BLUE_HORIZON_DENSITY
        setChildValue(FIELD_SKY_HAZE_HORIZON,   null) // APR: liveSky.getHazeHorizon()
        setChildValue(FIELD_SKY_HAZE_DENSITY,   null) // APR: liveSky.getHazeDensity()
        setChildValue(FIELD_SKY_SCENE_GAMMA,    null) // APR: liveSky.getGamma()
        setChildValue(FIELD_SKY_CLOUD_COLOR,    null) // APR: liveSky.getCloudColor()
        setChildValue(FIELD_SKY_CLOUD_COVERAGE, null) // APR: liveSky.getCloudShadow()
        setChildValue(FIELD_SKY_CLOUD_SCALE,    null) // APR: liveSky.getCloudScale()
        setChildValue(FIELD_SKY_SUN_COLOR,      null) // APR: liveSky.getSunlightColor() / SLIDER_SCALE_SUN_AMBIENT
        setChildValue(FIELD_SKY_CLOUD_MAP,      null) // APR: liveSky.getCloudNoiseTextureId()
        setChildValue(FIELD_WATER_NORMAL_MAP,   null) // APR: liveWater.getNormalMapID()
        setChildValue(FIELD_REFLECTION_PROBE_AMBIANCE, null) // APR: liveSky.getReflectionProbeAmbiance(shouldAutoAdjust)

        // Glow: C++ maps [40..0.2] engine range to [0..1.99] UI range.
        val glowR = 0f // APR: liveSky.getGlow().r
        val glowB = 0f // APR: liveSky.getGlow().b
        setChildValue(FIELD_SKY_GLOW_SIZE,  2.0f - (glowR / SLIDER_SCALE_GLOW_R))
        setChildValue(FIELD_SKY_GLOW_FOCUS, glowB / SLIDER_SCALE_GLOW_B)

        setChildValue(FIELD_SKY_STAR_BRIGHTNESS, null) // APR: liveSky.getStarBrightness()
        setChildValue(FIELD_SKY_SUN_SCALE,       null) // APR: liveSky.getSunScale()

        val (sunAzimuth, sunElevation) = getAzimuthAndElevationDeg(Any()) // APR: liveSky.getSunRotation()
        setChildValue(FIELD_SKY_SUN_AZIMUTH,   sunAzimuth)
        setChildValue(FIELD_SKY_SUN_ELEVATION, sunElevation)
        setChildRotation(FIELD_SKY_SUN_ROTATION, null) // APR: liveSky.getSunRotation()

        val (moonAzimuth, moonElevation) = getAzimuthAndElevationDeg(Any()) // APR: liveSky.getMoonRotation()
        setChildValue(FIELD_SKY_MOON_AZIMUTH,   moonAzimuth)
        setChildValue(FIELD_SKY_MOON_ELEVATION, moonElevation)
        setChildRotation(FIELD_SKY_MOON_ROTATION, null) // APR: liveSky.getMoonRotation()

        updateGammaLabel()
    }

    private fun captureCurrentEnvironment() {
        // APR: mirror LLFloaterEnvironmentAdjust::captureCurrentEnvironment –
        // clone fixed sky/water from ENV_LOCAL or ENV_PARCEL and push back to ENV_LOCAL
    }

    private fun onButtonReset() {
        // APR: LLNotificationsUtil::add(PersonalSettingsConfirmReset) ->
        // closeFloater() + LLEnvironment::clearEnvironment(ENV_LOCAL)
    }

    private fun onAmbientLightChanged() {
        val sky = liveSky ?: return
        // APR: sky.setAmbientColor(getChildColor(FIELD_SKY_AMBIENT_LIGHT) * SLIDER_SCALE_SUN_AMBIENT); sky.update()
    }

    private fun onBlueHorizonChanged() {
        val sky = liveSky ?: return
        // APR: sky.setBlueHorizon(getChildColor(FIELD_SKY_BLUE_HORIZON) * SLIDER_SCALE_BLUE_HORIZON_DENSITY); sky.update()
    }

    private fun onBlueDensityChanged() {
        val sky = liveSky ?: return
        // APR: sky.setBlueDensity(getChildColor(FIELD_SKY_BLUE_DENSITY) * SLIDER_SCALE_BLUE_HORIZON_DENSITY); sky.update()
    }

    private fun onHazeHorizonChanged() {
        val sky = liveSky ?: return
        // APR: sky.setHazeHorizon(getChildFloat(FIELD_SKY_HAZE_HORIZON)); sky.update()
    }

    private fun onHazeDensityChanged() {
        val sky = liveSky ?: return
        // APR: sky.setHazeDensity(getChildFloat(FIELD_SKY_HAZE_DENSITY)); sky.update()
    }

    private fun onSceneGammaChanged() {
        val sky = liveSky ?: return
        // APR: sky.setGamma(getChildFloat(FIELD_SKY_SCENE_GAMMA)); sky.update()
    }

    private fun onCloudColorChanged() {
        val sky = liveSky ?: return
        // APR: sky.setCloudColor(getChildColor(FIELD_SKY_CLOUD_COLOR)); sky.update()
    }

    private fun onCloudCoverageChanged() {
        val sky = liveSky ?: return
        // APR: sky.setCloudShadow(getChildFloat(FIELD_SKY_CLOUD_COVERAGE)); sky.update()
    }

    private fun onCloudScaleChanged() {
        val sky = liveSky ?: return
        // APR: sky.setCloudScale(getChildFloat(FIELD_SKY_CLOUD_SCALE)); sky.update()
    }

    private fun onSunColorChanged() {
        val sky = liveSky ?: return
        // APR: sky.setSunlightColor(getChildColor(FIELD_SKY_SUN_COLOR) * SLIDER_SCALE_SUN_AMBIENT); sky.update()
    }

    private fun onGlowChanged() {
        val sky = liveSky ?: return
        // 0–1.99 UI range maps to 40–0.2 engine range.
        val sizeUi  = 0f // APR: getChildFloat(FIELD_SKY_GLOW_SIZE)
        val focusUi = 0f // APR: getChildFloat(FIELD_SKY_GLOW_FOCUS)
        val glowR   = (2.0f - sizeUi) * SLIDER_SCALE_GLOW_R
        val glowB   = focusUi         * SLIDER_SCALE_GLOW_B
        // APR: sky.setGlow(Color3(glowR, 0f, glowB)); sky.update()
    }

    private fun onStarBrightnessChanged() {
        val sky = liveSky ?: return
        // APR: sky.setStarBrightness(getChildFloat(FIELD_SKY_STAR_BRIGHTNESS)); sky.update()
    }

    private fun onSunRotationChanged() {
        val quat = Any() // APR: getChildRotation(FIELD_SKY_SUN_ROTATION)
        val (az, el) = getAzimuthAndElevationDeg(quat)
        setChildValue(FIELD_SKY_SUN_AZIMUTH, az)
        setChildValue(FIELD_SKY_SUN_ELEVATION, el)
        liveSky?.let { /* APR: it.setSunRotation(quat); it.update() */ }
    }

    private fun onSunAzimElevChanged() {
        val azDeg = 0f // APR: getChildFloat(FIELD_SKY_SUN_AZIMUTH)
        val elDeg = 0f // APR: getChildFloat(FIELD_SKY_SUN_ELEVATION)
        val quat  = buildRotationFromAzimElev(azDeg, elDeg)
        setChildRotation(FIELD_SKY_SUN_ROTATION, quat)
        liveSky?.let { /* APR: it.setSunRotation(quat); it.update() */ }
    }

    private fun onSunScaleChanged() {
        val sky = liveSky ?: return
        // APR: sky.setSunScale(getChildFloat(FIELD_SKY_SUN_SCALE)); sky.update()
    }

    private fun onMoonRotationChanged() {
        val quat = Any() // APR: getChildRotation(FIELD_SKY_MOON_ROTATION)
        val (az, el) = getAzimuthAndElevationDeg(quat)
        setChildValue(FIELD_SKY_MOON_AZIMUTH, az)
        setChildValue(FIELD_SKY_MOON_ELEVATION, el)
        liveSky?.let { /* APR: it.setMoonRotation(quat); it.update() */ }
    }

    private fun onMoonAzimElevChanged() {
        val azDeg = 0f // APR: getChildFloat(FIELD_SKY_MOON_AZIMUTH)
        val elDeg = 0f // APR: getChildFloat(FIELD_SKY_MOON_ELEVATION)
        val quat  = buildRotationFromAzimElev(azDeg, elDeg)
        setChildRotation(FIELD_SKY_MOON_ROTATION, quat)
        liveSky?.let { /* APR: it.setMoonRotation(quat); it.update() */ }
    }

    private fun onCloudMapChanged() {
        val sky = liveSky ?: return
        val newId: LLUUID? = null // APR: getChildTextureId(FIELD_SKY_CLOUD_MAP)
        // APR: clone sky, setCloudNoiseTextureId, push to ENV_LOCAL, updateEnvironment(TRANSITION_INSTANT)
    }

    private fun onWaterMapChanged() {
        val water = liveWater ?: return
        val newId: LLUUID? = null // APR: getChildTextureId(FIELD_WATER_NORMAL_MAP)
        // APR: water.setNormalMapID(newId); water.update()
    }

    private fun onReflectionProbeAmbianceChanged() {
        val sky = liveSky ?: return
        val ambiance = 0f // APR: getChildFloat(FIELD_REFLECTION_PROBE_AMBIANCE)
        // APR: sky.setReflectionProbeAmbiance(ambiance); updateGammaLabel(); sky.update()
    }

    private fun updateGammaLabel() {
        val sky = liveSky ?: return
        val ambiance = 0f // APR: sky.getReflectionProbeAmbiance(shouldAutoAdjust)
        if (ambiance != 0f) {
            setChildValue("scene_gamma_label", getString("hdr_string"))
            // APR: getChildCtrl(FIELD_SKY_SCENE_GAMMA).setToolTip(getString("hdr_tooltip"))
        } else {
            setChildValue("scene_gamma_label", getString("brightness_string"))
            // APR: getChildCtrl(FIELD_SKY_SCENE_GAMMA).setToolTip("")
        }
    }

    fun onEnvironmentUpdated(env: EnvSelection, version: Int) {
        if (env == EnvSelection.LOCAL && version != FLOATER_ENVIRONMENT_UPDATE) {
            captureCurrentEnvironment()
            refresh()
        }
    }

    // -------------------------------------------------------------------------
    // Geometry helpers (mirror LLVirtualTrackball statics)
    // -------------------------------------------------------------------------

    private fun getAzimuthAndElevationDeg(quaternion: Any): Pair<Float, Float> {
        // GPU: extract azimuth and elevation in degrees from quaternion via LLVirtualTrackball::getAzimuthAndElevationDeg
        return Pair(0f, 0f)
    }

    private fun buildRotationFromAzimElev(azDeg: Float, elDeg: Float): Any {
        val azRad = Math.toRadians(azDeg.toDouble()).toFloat()
        val elRad = Math.toRadians(elDeg.toDouble()).toFloat()
        val safeEl = if (elRad == 0f) Float.MIN_VALUE else elRad
        // GPU: quat.setAngleAxis(-safeEl,0,1,0) * az_quat.setAngleAxis(2π-azRad,0,0,1)
        return Any()
    }

    // -------------------------------------------------------------------------
    // Stubs for UI wiring – real binding goes through the platform UI layer.
    // -------------------------------------------------------------------------

    private fun bindCommit(fieldName: String, action: () -> Unit) {
        // APR: getChild<UICtrl>(fieldName).setCommitCallback { action() }
    }

    private fun setChildValue(fieldName: String, value: Any?) {
        // APR: getChild<UICtrl>(fieldName).setValue(value)
    }

    private fun setChildRotation(fieldName: String, quat: Any?) {
        // APR: getChild<VirtualTrackball>(fieldName).setRotation(quat)
    }

    private fun setAllChildrenEnabled(enabled: Boolean) {
        // APR: iterate all child views and setEnabled(enabled)
    }

    private fun getString(key: String): String {
        System.err.println("FloaterEnvironmentSettings: getString not yet implemented")
        return ""
    }
}
