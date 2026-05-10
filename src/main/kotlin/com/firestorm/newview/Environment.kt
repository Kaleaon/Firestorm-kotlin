package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llrender.Shader

enum class EnvSelection(val id: Int) {
    EDIT(0),
    LOCAL(1),
    PUSH(2),
    PARCEL(3),
    REGION(4),
    DEFAULT(5),
    CURRENT(-1),
    NONE(-2);

    companion object { val END = DEFAULT.id + 1 }
}

typealias AltitudeList = FloatArray
typealias EnvChangedCallback = (EnvSelection, Int) -> Unit

data class EnvironmentInfo(
    val parcelId: Int = 0,
    val regionId: LLUUID = LLUUID.NULL,
    val dayLength: Long = 0L,
    val dayOffset: Long = 0L,
    val dayHash: Int = 0,
    val dayCycleName: String = "",
    val altitudes: FloatArray = FloatArray(4),
    val isDefault: Boolean = false,
    val assetId: LLUUID = LLUUID.NULL,
    val isLegacy: Boolean = false,
    val nameList: List<String> = emptyList(),
    val envVersion: Int = 0
)

class DayInstance(val envSelection: EnvSelection) {

    companion object {
        const val NO_ANIMATE_SKY: UInt   = 0x01u
        const val NO_ANIMATE_WATER: UInt = 0x02u
    }

    var sky: Any?    = null
    var water: Any?  = null
    var dayCycle: Any? = null
    var dayLength: Double = 14400.0
    var dayOffset: Double = 0.0
    var skyTrack: Int = 1
    var animateFlags: UInt = 0u

    fun getProgress(): Float = TODO("calculate normalized day position [0,1]")

    fun applyTimeDelta(deltaSeconds: Double): Boolean =
        TODO("advance blenders by deltaSeconds, return true when track boundary crossed")

    fun setSky(pSky: Any?) { sky = pSky }
    fun setWater(pWater: Any?) { water = pWater }
    fun setSkyTrack(trackNo: Int) { skyTrack = trackNo }
    fun animate() { TODO("rebuild blenders for current dayCycle track and offset") }
    fun initialize() { TODO("set up sky/water from dayCycle at current offset") }
    fun clear() { sky = null; water = null; dayCycle = null }
}

class DayTransition(
    val startSky: Any?,
    val startWater: Any?,
    val nextInstance: DayInstance,
    val transitionSeconds: Double
) : DayInstance(nextInstance.envSelection) {

    fun applyTimeDelta(deltaSeconds: Double): Boolean =
        TODO("blend startSky→next.sky and startWater→next.water over transitionSeconds")
}

object Environment {

    companion object {
        const val TRANSITION_INSTANT: Double  = 0.0
        const val TRANSITION_FAST: Double     = 1.0
        const val TRANSITION_DEFAULT: Double  = 5.0
        const val TRANSITION_SLOW: Double     = 10.0
        const val TRANSITION_ALTITUDE: Double = 5.0

        val KNOWN_SKY_SUNRISE      = LLUUID("01e41537-ff51-2f1f-8ef7-17e4df760bfb")
        val KNOWN_SKY_MIDDAY       = LLUUID("c46226b4-0e43-5a56-9708-d27ca1df3292")
        val KNOWN_SKY_LEGACY_MIDDAY = LLUUID("6c83e853-e7f8-cad7-8ee6-5f31c453721c")
        val KNOWN_SKY_SUNSET       = LLUUID("084e26cd-a900-28e8-08d0-64a9de5c15e2")
        val KNOWN_SKY_MIDNIGHT     = LLUUID("8a01b97a-cb20-c1ea-ac63-f7ea84ad0090")

        const val NO_TRACK: Int        = -1
        const val NO_VERSION: Int      = -3
        const val VERSION_CLEANUP: Int = -4

        private const val SUN_DELTA_YAW: Float = Math.PI.toFloat()
    }

    private val environments = arrayOfNulls<DayInstance>(EnvSelection.END)
    private var currentEnvironment: DayInstance? = null
    var selectedEnvironment: EnvSelection = EnvSelection.DEFAULT
        private set

    private var currentTrack: Int = 1
    val trackAltitudes: AltitudeList = floatArrayOf(0f, 1000f, 2000f, 3000f)

    private var cloudScrollDelta: Vector2 = Vector2()
    var isCloudScrollPaused: Boolean = false
        private set

    private var lastCamYaw: Float = 0f
    private val envChangedListeners: MutableList<EnvChangedCallback> = mutableListOf()

    fun getCurrentSky(): Any?   = currentEnvironment?.sky
    fun getCurrentWater(): Any? = currentEnvironment?.water
    fun getCurrentDay(): Any?   = currentEnvironment?.dayCycle

    fun getProgress(): Float        = currentEnvironment?.getProgress() ?: -1f
    fun getRegionProgress(): Float  = environments[EnvSelection.REGION.id]?.getProgress() ?: -1f

    fun canEdit(): Boolean              = TODO("check agent capabilities for edit permission")
    fun isExtendedEnvironmentEnabled(): Boolean = TODO("check region capability 'ExtendedEnvironment'")
    fun isInventoryEnabled(): Boolean   = TODO("check agent inventory capability")
    fun canAgentUpdateParcelEnvironment(): Boolean = TODO("check parcel flags and agent group/owner status")
    fun canAgentUpdateRegionEnvironment(): Boolean = TODO("check region estate manager/owner status")

    fun hasEnvironment(env: EnvSelection): Boolean = environments[env.id] != null

    fun setSelectedEnvironment(env: EnvSelection, transition: Double = TRANSITION_DEFAULT, forced: Boolean = false) {
        TODO("activate env slot, build DayTransition if needed, fire envChangedListeners")
    }

    fun setEnvironment(env: EnvSelection, assetId: LLUUID, transition: Double = TRANSITION_DEFAULT, envVersion: Int = NO_VERSION) {
        TODO("async-load asset then call recordEnvironment")
    }

    fun setEnvironment(env: EnvSelection, dayCycle: Any?, dayLength: Long, dayOffset: Long, envVersion: Int = NO_VERSION) {
        val inst = getOrCreateInstance(env)
        TODO("configure inst with dayCycle, dayLength, dayOffset; call updateEnvironment")
    }

    fun clearEnvironment(env: EnvSelection) {
        environments[env.id] = null
        TODO("rebuild currentEnvironment from remaining priority slots")
    }

    fun updateEnvironment(transition: Double = TRANSITION_DEFAULT, forced: Boolean = false) {
        TODO("resolve highest-priority active env slot, apply transition blender")
    }

    fun update(cam: ViewerCamera) {
        lastCamYaw = cam.yaw + SUN_DELTA_YAW
        if (!isCloudScrollPaused) updateCloudScroll()
        currentEnvironment?.applyTimeDelta(0.0)
        TODO("compute per-frame sky/water blend, update light direction cache")
    }

    fun updateShaderUniforms(shader: Shader) {
        TODO("GPU: push sky and water uniforms to shader")
    }

    fun updateSettingsUniforms() {
        TODO("GPU: snapshot current sky/water into mSkyUniforms/mWaterUniforms arrays")
    }

    fun getLightDirection(): Vector3  = TODO("return sun or moon direction (whichever is above horizon)")
    fun getSunDirection(): Vector3    = TODO("return sun direction in viewer +x right +z up coords")
    fun getMoonDirection(): Vector3   = TODO("return moon direction in viewer coords")

    fun getLightDirectionCFR(): Vector4  = TODO("convert getLightDirection() to Camera-Frame-Right coords")
    fun getSunDirectionCFR(): Vector4    = TODO("convert getSunDirection() to CFR")
    fun getMoonDirectionCFR(): Vector4   = TODO("convert getMoonDirection() to CFR")

    fun getClampedLightNorm(): Vector4  = TODO("OGL coords, Y clamped above -0.1 to avoid sky shader artifacts")
    fun getClampedSunNorm(): Vector4    = TODO("OGL coords sun, Y clamped above -0.1")
    fun getClampedMoonNorm(): Vector4   = TODO("OGL coords moon, Y clamped above -0.1")
    fun getRotatedLightNorm(): Vector4  = TODO("OGL coords rotated by lastCamYaw for water shaders")

    fun getCamHeight(): Float   = TODO("return camera altitude above terrain")
    fun getWaterHeight(): Float = TODO("return current region water level")
    fun getIsSunUp(): Boolean   = TODO("return sunDirection.z > 0")
    fun getIsMoonUp(): Boolean  = TODO("return moonDirection.z > 0")

    fun getCloudScrollDelta(): Vector2 = cloudScrollDelta
    fun pauseCloudScroll()  { isCloudScrollPaused = true }
    fun resumeCloudScroll() { isCloudScrollPaused = false }

    fun calculateSkyTrackForAltitude(altitude: Double): Int {
        for (i in trackAltitudes.size - 1 downTo 1) {
            if (altitude >= trackAltitudes[i]) return i
        }
        return 1
    }

    fun adjustRegionOffset(adjust: Float) {
        TODO("shift region DayInstance offset by adjust seconds (legacy region sync)")
    }

    fun requestRegion(callback: ((Int, EnvironmentInfo) -> Unit)? = null) {
        TODO("HTTP GET region environment, call recordEnvironment on response")
    }

    fun requestParcel(parcelId: Int, callback: ((Int, EnvironmentInfo) -> Unit)? = null) {
        TODO("HTTP GET parcel environment, call recordEnvironment on response")
    }

    fun addEnvironmentChangedListener(cb: EnvChangedCallback) { envChangedListeners += cb }

    fun saveToSettings()    { TODO("serialise local environment overrides to disk") }
    fun loadFromSettings(): Boolean = TODO("deserialise local environment overrides from disk")

    private fun updateCloudScroll() {
        TODO("accumulate cloud scroll delta from sky wind speed settings")
    }

    private fun getOrCreateInstance(env: EnvSelection): DayInstance {
        return environments[env.id] ?: DayInstance(env).also { environments[env.id] = it }
    }
}
