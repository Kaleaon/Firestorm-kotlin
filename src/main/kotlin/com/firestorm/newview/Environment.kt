package com.firestorm.newview

import kotlin.math.*

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

typealias AltitudeList       = FloatArray
typealias FixedEnvironment   = Pair<SettingsSky?, SettingsWater?>
typealias EnvChangedCallback = (EnvSelection, Int) -> Unit
typealias EnvApplyFn         = (Int, EnvironmentInfo) -> Unit

data class EnvironmentInfo(
    val parcelId: Int                  = 0,
    val regionId: LLUUID               = LLUUID.NULL,
    val dayLength: Long                = 0L,
    val dayOffset: Long                = 0L,
    val dayHash: Int                   = 0,
    val dayCycle: SettingsDayCycle?    = null,
    val dayCycleName: String           = "",
    val altitudes: FloatArray          = FloatArray(4),
    val isDefault: Boolean             = false,
    val assetId: LLUUID                = LLUUID.NULL,
    val isLegacy: Boolean              = false,
    val nameList: List<String>         = emptyList(),
    val envVersion: Int                = 0
)

class DayInstance(val envSelection: EnvSelection) {

    companion object {
        const val NO_ANIMATE_SKY: UInt   = 0x01u
        const val NO_ANIMATE_WATER: UInt = 0x02u
    }

    var sky: SettingsSky?            = null
    var water: SettingsWater?        = null
    var dayCycle: SettingsDayCycle?  = null
    var dayLength: Long              = 14400L
    var dayOffset: Long              = 0L
    var skyTrack: Int                = 1
    var animateFlags: UInt           = 0u
    var initialized: Boolean         = false

    protected var blenderSky: SettingsBlender?   = null
    protected var blenderWater: SettingsBlender? = null
    protected var lastTrackAltitude: Int         = 1

    open fun clone(): DayInstance {
        val c = DayInstance(envSelection)
        c.sky          = sky
        c.water        = water
        c.dayCycle     = dayCycle
        c.dayLength    = dayLength
        c.dayOffset    = dayOffset
        c.skyTrack     = skyTrack
        c.animateFlags = animateFlags
        return c
    }

    open fun applyTimeDelta(deltaSeconds: Double): Boolean {
        var changed = false
        if (animateFlags and NO_ANIMATE_SKY == 0u)   changed = blenderSky?.applyTimeDelta(deltaSeconds) == true || changed
        if (animateFlags and NO_ANIMATE_WATER == 0u) changed = blenderWater?.applyTimeDelta(deltaSeconds) == true || changed
        return changed
    }

    open fun setDay(pDay: SettingsDayCycle, dayLen: Long, dayOff: Long) {
        dayCycle  = pDay
        dayLength = dayLen
        dayOffset = dayOff
        animate()
    }

    fun setSky(pSky: SettingsSky?) { sky = pSky }

    open fun setWater(pWater: SettingsWater?) { water = pWater }

    fun initialize() {
        if (dayCycle != null) animate()
        initialized = dayCycle != null || sky != null || water != null
    }

    fun isInitialized(): Boolean = initialized
    open fun isTransition(): Boolean = false

    fun clear() { sky = null; water = null; dayCycle = null; initialized = false }

    fun setSkyTrack(trackNo: Int) { skyTrack = trackNo; animate() }

    fun getDayCycle(): SettingsDayCycle? = dayCycle
    fun getSky(): SettingsSky?           = sky
    fun getWater(): SettingsWater?       = water
    fun getDayLength(): Long             = dayLength
    fun getDayOffset(): Long             = dayOffset
    fun getSkyTrack(): Int               = skyTrack

    fun setDayOffset(offset: Long) { dayOffset = offset; animate() }

    open fun animate() {
        TODO("build TrackBlenderLoopingTime blenders for sky/water tracks using dayCycle, dayLength, dayOffset")
    }

    fun setBlenders(skyBlend: SettingsBlender?, waterBlend: SettingsBlender?) {
        blenderSky   = skyBlend
        blenderWater = waterBlend
    }

    fun getEnvironmentSelection(): EnvSelection = envSelection

    fun getProgress(): Float {
        val day = dayCycle ?: return -1f
        TODO("compute normalized position within day cycle using current time and dayOffset/dayLength")
    }

    fun setFlags(flag: UInt)  { animateFlags = animateFlags or flag }
    fun clearFlags(flag: UInt) { animateFlags = animateFlags and flag.inv() }
    fun getFlags(): UInt = animateFlags

    protected fun secondsToKeyframe(seconds: Long): Float {
        if (dayLength == 0L) return 1f
        return ((seconds % dayLength).toFloat() / dayLength.toFloat()).coerceIn(0f, 1f)
    }
}

open class DayTransition(
    val startSky: SettingsSky?,
    val startWater: SettingsWater?,
    val nextInstance: DayInstance,
    val transitionTime: Long
) : DayInstance(nextInstance.envSelection) {

    override fun isTransition(): Boolean = true

    override fun applyTimeDelta(deltaSeconds: Double): Boolean {
        TODO("blend startSky→nextInstance.sky and startWater→nextInstance.water over transitionTime seconds")
    }

    override fun animate() {
        TODO("set up transition blenders from startSky/startWater toward nextInstance sky/water")
    }
}

class TrackBlenderLoopingManual(
    target: SettingsBase?,
    val day: SettingsDayCycle,
    private var trackNo: Int
) : SettingsBlender(target, null, null) {

    private var position: Double = 0.0

    fun setPosition(pos: Float): Double {
        position = pos.toDouble()
        TODO("update blender initial/final from track bounding entries at pos, return blend factor")
    }

    override fun switchTrack(trackNo: Int, position: Float) {
        this.trackNo = trackNo
        TODO("rebuild bounding entries for new track at given position")
    }

    fun getTrack(): Int = trackNo

    private fun getBoundingEntries(pos: Double): Pair<Float, Float> {
        TODO("return (lowerBoundFrame, upperBoundFrame) from day track at pos")
    }

    private fun getSpanLength(bounds: Pair<Float, Float>): Double {
        TODO("compute wrapped distance between bounds.first and bounds.second")
    }
}

object Environment {

    companion object {
        const val TRANSITION_INSTANT: Double  = 0.0
        const val TRANSITION_FAST: Double     = 1.0
        const val TRANSITION_DEFAULT: Double  = 5.0
        const val TRANSITION_SLOW: Double     = 10.0
        const val TRANSITION_ALTITUDE: Double = 5.0

        val KNOWN_SKY_SUNRISE       = LLUUID("01e41537-ff51-2f1f-8ef7-17e4df760bfb")
        val KNOWN_SKY_MIDDAY        = LLUUID("c46226b4-0e43-5a56-9708-d27ca1df3292")
        val KNOWN_SKY_LEGACY_MIDDAY = LLUUID("6c83e853-e7f8-cad7-8ee6-5f31c453721c")
        val KNOWN_SKY_SUNSET        = LLUUID("084e26cd-a900-28e8-08d0-64a9de5c15e2")
        val KNOWN_SKY_MIDNIGHT      = LLUUID("8a01b97a-cb20-c1ea-ac63-f7ea84ad0090")

        const val NO_TRACK: Int        = -1
        const val NO_VERSION: Int      = -3
        const val VERSION_CLEANUP: Int = -4

        private const val SUN_DELTA_YAW: Float = PI.toFloat()

        fun updateGLVariablesForSettings(settings: SettingsBase) {
            TODO("GPU: push all setting uniforms to shader uniform block")
        }

        fun logEnvironment(env: EnvSelection, settings: SettingsBase, envVersion: Int = NO_VERSION) {
            TODO("APR: log environment selection event for debugging")
        }

        fun createWaterFromLegacyPreset(filename: String): SettingsWater? {
            TODO("APR: parse legacy XML water preset file, call translateLegacySettings")
        }

        fun createSkyFromLegacyPreset(filename: String): SettingsSky? {
            TODO("APR: parse legacy XML sky preset file, call translateLegacySettings")
        }

        fun createDayCycleFromLegacyPreset(filename: String): SettingsDayCycle? {
            TODO("APR: parse legacy XML day cycle file, build SettingsDayCycle")
        }
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

    private var skyOverrides: LLSD   = llsdEmptyMap()
    private var waterOverrides: LLSD = llsdEmptyMap()
    private val experienceOverrides: MutableMap<String, LLUUID> = mutableMapOf()

    fun getCurrentDay(): SettingsDayCycle?   = currentEnvironment?.getDayCycle()
    fun getCurrentSky(): SettingsSky?        = currentEnvironment?.getSky()
    fun getCurrentWater(): SettingsWater?    = currentEnvironment?.getWater()

    fun getProgress(): Float        = currentEnvironment?.getProgress() ?: -1f
    fun getRegionProgress(): Float  = environments[EnvSelection.REGION.id]?.getProgress() ?: -1f

    fun canEdit(): Boolean                        = TODO("check agent capabilities for edit permission")
    fun isExtendedEnvironmentEnabled(): Boolean   = TODO("check region capability 'ExtendedEnvironment'")
    fun isInventoryEnabled(): Boolean             = TODO("check agent inventory capability")
    fun canAgentUpdateParcelEnvironment(): Boolean = TODO("check parcel flags and agent group/owner status")
    fun canAgentUpdateRegionEnvironment(): Boolean = TODO("check region estate manager/owner status")

    fun hasEnvironment(env: EnvSelection): Boolean = environments[env.id] != null

    fun setSelectedEnvironment(env: EnvSelection, transition: Double = TRANSITION_DEFAULT, forced: Boolean = false) {
        TODO("activate env slot, build DayTransition if needed, fire envChangedListeners")
    }

    fun setEnvironment(env: EnvSelection, pDay: SettingsDayCycle, dayLength: Long, dayOffset: Long, envVersion: Int = NO_VERSION) {
        val inst = getOrCreateInstance(env)
        inst.setDay(pDay, dayLength, dayOffset)
        TODO("store envVersion, call updateEnvironment")
    }

    fun setEnvironment(env: EnvSelection, fixed: FixedEnvironment, envVersion: Int = NO_VERSION) {
        val inst = getOrCreateInstance(env)
        fixed.first?.let  { inst.setSky(it) }
        fixed.second?.let { inst.setWater(it) }
        TODO("store envVersion, call updateEnvironment")
    }

    fun setEnvironment(env: EnvSelection, sky: SettingsSky, envVersion: Int = NO_VERSION) =
        setEnvironment(env, FixedEnvironment(sky, null), envVersion)

    fun setEnvironment(env: EnvSelection, water: SettingsWater, envVersion: Int = NO_VERSION) =
        setEnvironment(env, FixedEnvironment(null, water), envVersion)

    fun setEnvironment(env: EnvSelection, assetId: LLUUID, transition: Double = TRANSITION_DEFAULT, envVersion: Int = NO_VERSION) {
        TODO("APR: async load asset by ID, then call setEnvironment with loaded settings")
    }

    fun clearEnvironment(env: EnvSelection) {
        environments[env.id] = null
        TODO("rebuild currentEnvironment from remaining priority slots")
    }

    fun updateEnvironment(transition: Double = TRANSITION_DEFAULT, forced: Boolean = false) {
        TODO("resolve highest-priority active env slot, apply transition blender")
    }

    fun setCurrentEnvironmentSelection(env: EnvSelection) {
        TODO("update currentEnvironment to point at env slot instance")
    }

    fun getEnvironmentDay(env: EnvSelection): SettingsDayCycle?     = environments[env.id]?.getDayCycle()
    fun getEnvironmentDayLength(env: EnvSelection): Long            = environments[env.id]?.getDayLength() ?: SettingsDayCycle.DEFAULT_DAYLENGTH.toLong()
    fun getEnvironmentDayOffset(env: EnvSelection): Long            = environments[env.id]?.getDayOffset() ?: SettingsDayCycle.DEFAULT_DAYOFFSET.toLong()
    fun getEnvironmentFixed(env: EnvSelection, resolve: Boolean = false): FixedEnvironment =
        FixedEnvironment(environments[env.id]?.getSky(), environments[env.id]?.getWater())
    fun getEnvironmentFixedSky(env: EnvSelection, resolve: Boolean = false): SettingsSky?   = getEnvironmentFixed(env, resolve).first
    fun getEnvironmentFixedWater(env: EnvSelection, resolve: Boolean = false): SettingsWater? = getEnvironmentFixed(env, resolve).second

    fun update(camYaw: Float) {
        lastCamYaw = camYaw + SUN_DELTA_YAW
        if (!isCloudScrollPaused) updateCloudScroll()
        currentEnvironment?.applyTimeDelta(0.0)
        TODO("GPU: compute per-frame sky/water blend, update light direction cache")
    }

    fun updateShaderUniforms() {
        TODO("GPU: push sky and water uniforms to shader")
    }

    fun updateSettingsUniforms() {
        TODO("GPU: snapshot current sky/water into uniform arrays for all shader groups")
    }

    fun getLightDirection(): Vector3    = TODO("return sun or moon direction (whichever is above horizon)")
    fun getSunDirection(): Vector3      = TODO("return sun direction in viewer +x right +z up coords")
    fun getMoonDirection(): Vector3     = TODO("return moon direction in viewer coords")

    fun getLightDirectionCFR(): Vector4 = TODO("convert getLightDirection() to Camera-Frame-Right coords")
    fun getSunDirectionCFR(): Vector4   = TODO("convert getSunDirection() to CFR")
    fun getMoonDirectionCFR(): Vector4  = TODO("convert getMoonDirection() to CFR")
    fun getClampedLightNorm(): Vector4  = TODO("OGL coords, Y clamped above -0.1 to avoid sky shader artifacts")
    fun getClampedSunNorm(): Vector4    = TODO("OGL coords sun, Y clamped above -0.1")
    fun getClampedMoonNorm(): Vector4   = TODO("OGL coords moon, Y clamped above -0.1")
    fun getRotatedLightNorm(): Vector4  = TODO("OGL coords rotated by lastCamYaw for water shaders")

    fun getCamHeight(): Float   = TODO("return camera altitude above terrain")
    fun getWaterHeight(): Float = TODO("return current region water level")
    fun getIsSunUp(): Boolean   = getCurrentSky()?.getIsSunUp() ?: false
    fun getIsMoonUp(): Boolean  = getCurrentSky()?.getIsMoonUp() ?: false
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

    fun createDayCycleFromEnvironment(env: EnvSelection, settings: SettingsBase): SettingsDayCycle? {
        TODO("build a new day cycle from env slot, replacing sky or water track with settings")
    }

    fun requestRegion(callback: EnvApplyFn? = null) {
        TODO("APR: HTTP GET region environment, call recordEnvironment on response")
    }

    fun updateRegion(assetId: LLUUID, displayName: String, trackNum: Int, dayLength: Int, dayOffset: Int,
                     flags: UInt, altitudes: List<Float> = emptyList(), callback: EnvApplyFn? = null) {
        TODO("APR: HTTP PUT region environment settings")
    }

    fun updateRegion(pDay: SettingsDayCycle, dayLength: Int, dayOffset: Int,
                     altitudes: List<Float> = emptyList(), callback: EnvApplyFn? = null) {
        TODO("APR: HTTP PUT region environment from day cycle object")
    }

    fun resetRegion(callback: EnvApplyFn? = null) {
        TODO("APR: HTTP DELETE region environment override")
    }

    fun requestParcel(parcelId: Int, callback: EnvApplyFn? = null) {
        TODO("APR: HTTP GET parcel environment, call recordEnvironment on response")
    }

    fun updateParcel(parcelId: Int, assetId: LLUUID, displayName: String, trackNum: Int,
                     dayLength: Int, dayOffset: Int, flags: UInt,
                     altitudes: List<Float> = emptyList(), callback: EnvApplyFn? = null) {
        TODO("APR: HTTP PUT parcel environment settings")
    }

    fun updateParcel(parcelId: Int, pDay: SettingsDayCycle, trackNum: Int, dayLength: Int, dayOffset: Int,
                     altitudes: List<Float> = emptyList(), callback: EnvApplyFn? = null) {
        TODO("APR: HTTP PUT parcel environment from day cycle object")
    }

    fun resetParcel(parcelId: Int, callback: EnvApplyFn? = null) {
        TODO("APR: HTTP DELETE parcel environment override")
    }

    fun selectAgentEnvironment() {
        TODO("select correct env slot based on agent altitude vs trackAltitudes")
    }

    fun handleEnvironmentPush(message: LLSD) {
        TODO("APR: dispatch PushExpEnvironment action to clear/full/partial handlers")
    }

    fun saveToSettings()    { TODO("APR: serialise local environment overrides to disk") }
    fun loadFromSettings(): Boolean = TODO("APR: deserialise local environment overrides from disk")

    fun getSelectedEnvironmentInstance(): DayInstance? = currentEnvironment
    fun getSharedEnvironmentInstance(): DayInstance? = environments[EnvSelection.REGION.id]

    fun addEnvironmentChangedListener(cb: EnvChangedCallback) { envChangedListeners += cb }

    private fun updateCloudScroll() {
        TODO("accumulate cloud scroll delta from sky scroll rate settings each frame")
    }

    private fun getOrCreateInstance(env: EnvSelection): DayInstance {
        return environments[env.id] ?: DayInstance(env).also { environments[env.id] = it }
    }

    private fun recordEnvironment(parcelId: Int, info: EnvironmentInfo, transition: Double) {
        TODO("store EnvironmentInfo, call setEnvironment with info.dayCycle and transition")
    }

    private fun toCFR(vec: Vector3): Vector4 {
        TODO("GPU: convert viewer-space vec to Camera-Frame-Right (CFR) coord system Vector4")
    }

    private fun toLightNorm(vec: Vector3): Vector4 {
        TODO("GPU: convert light direction to OGL coords, clamp Y above -0.1")
    }
}
