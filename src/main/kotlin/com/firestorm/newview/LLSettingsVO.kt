package com.firestorm.newview

import java.io.File
import java.util.UUID

// ---- Forward-declared stubs for types defined in other modules ----

abstract class LLSettingsBase {
    abstract fun getSettings(): Map<String, Any>
    abstract fun getSettingsType(): String
    abstract fun getName(): String
    abstract fun setName(name: String)
    abstract fun getSettingsTypeValue(): Int
    abstract fun setAssetId(id: UUID)
    abstract fun getFlags(): UInt
    abstract fun setFlags(flags: UInt)
    abstract fun getFlag(flag: Int): Boolean
    abstract fun cloneSettings(): Map<String, Any>
    abstract fun validate(): Boolean
    abstract fun getHash(): Long
    abstract fun buildDerivedClone(): LLSettingsBase

    companion object {
        fun settingValidation(settings: Map<String, Any>, validations: List<Any>): Map<String, Any> {
            TODO("APR: validate LLSD settings map against validation list; return result map with 'success' key")
        }
        const val FLAG_NOTRANS = 1
        const val DEFAULT_SETTINGS_NAME = "(Default)"
        const val SETTING_TYPE = "type"
        const val SETTING_NAME = "name"
    }
}

abstract class LLSettingsSky : LLSettingsBase() {
    abstract fun getLightDirection(): LLVector3
    abstract fun getSunDirection(): LLVector3
    abstract fun getMoonDirection(): LLVector3
    abstract fun getAmbientColor(): LLColor3
    abstract fun getBlueDensity(): LLColor3
    abstract fun getBlueHorizon(): LLColor3
    abstract fun getHazeDensity(): Float
    abstract fun getHazeHorizon(): Float
    abstract fun getDensityMultiplier(): Float
    abstract fun getDistanceMultiplier(): Float
    abstract fun getCloudPosDensity2(): LLColor3
    abstract fun getCloudScale(): Float
    abstract fun getCloudShadow(): Float
    abstract fun getCloudVariance(): Float
    abstract fun getGlow(): LLColor3
    abstract fun getMaxY(): Float
    abstract fun getMoonBrightness(): Float
    abstract fun getSkyMoistureLevel(): Float
    abstract fun getSkyDropletRadius(): Float
    abstract fun getSkyIceLevel(): Float
    abstract fun getReflectionProbeAmbiance(): Float
    abstract fun getTotalAmbient(): LLColor3
    abstract fun getGamma(): Float
    abstract fun getSunlightColor(): LLColor4
    abstract fun getMoonlightColor(): LLColor4
    abstract fun getCloudColor(): LLColor3
    abstract fun getIsSunUp(): Boolean
    abstract fun getSunMoonGlowFactor(): Float
    abstract fun canAutoAdjust(): Boolean
    abstract fun setTonemapMix(mix: Float)
    abstract fun getSunTextureId(): UUID
    abstract fun getNextSunTextureId(): UUID
    abstract fun getMoonTextureId(): UUID
    abstract fun getNextMoonTextureId(): UUID
    abstract fun getCloudNoiseTextureId(): UUID
    abstract fun getNextCloudNoiseTextureId(): UUID
    abstract fun getBloomTextureId(): UUID
    abstract fun getNextBloomTextureId(): UUID
    abstract fun getSunScale(): Float
    abstract fun getMoonScale(): Float
    abstract fun updateSettings()
    abstract override fun buildDerivedClone(): LLSettingsSky

    typealias ptr_t = LLSettingsSky?
    typealias parammapping_t = MutableMap<String, Any>
    typealias validation_list_t = List<Any>

    companion object {
        fun translateLegacySettings(old: Map<String, Any>): Map<String, Any> {
            TODO("APR: convert legacy Windlight sky LLSD to EEP settings map")
        }
        fun validationList(): validation_list_t = TODO("APR: return sky validation rules list")
        fun defaults(): Map<String, Any> = TODO("APR: return default sky settings map")

        const val SETTING_LEGACY_HAZE = "legacy_haze"
        const val SETTING_AMBIENT = "ambient"
        const val SETTING_BLUE_DENSITY = "blue_density"
        const val SETTING_BLUE_HORIZON = "blue_horizon"
        const val SETTING_DENSITY_MULTIPLIER = "density_multiplier"
        const val SETTING_DISTANCE_MULTIPLIER = "distance_multiplier"
        const val SETTING_HAZE_DENSITY = "haze_density"
        const val SETTING_HAZE_HORIZON = "haze_horizon"
        const val SETTING_CLOUD_COLOR = "cloud_color"
        const val SETTING_CLOUD_POS_DENSITY1 = "cloud_pos_density1"
        const val SETTING_CLOUD_POS_DENSITY2 = "cloud_pos_density2"
        const val SETTING_CLOUD_SCALE = "cloud_scale"
        const val SETTING_CLOUD_SCROLL_RATE = "cloud_scroll_rate"
        const val SETTING_LEGACY_ENABLE_CLOUD_SCROLL = "enable_cloud_scroll"
        const val SETTING_CLOUD_SHADOW = "cloud_shadow"
        const val SETTING_GAMMA = "gamma"
        const val SETTING_GLOW = "glow"
        const val SETTING_LIGHT_NORMAL = "lightnorm"
        const val SETTING_MAX_Y = "max_y"
        const val SETTING_STAR_BRIGHTNESS = "star_brightness"
        const val SETTING_SUNLIGHT_COLOR = "sunlight_color"
        const val SETTING_LEGACY_EAST_ANGLE = "east_angle"
        const val SETTING_LEGACY_SUN_ANGLE = "sun_angle"
        const val SETTING_MOON_BRIGHTNESS = "moon_brightness"
        const val SETTING_SKY_MOISTURE_LEVEL = "moisture_level"
        const val SETTING_SKY_DROPLET_RADIUS = "droplet_radius"
        const val SETTING_SKY_ICE_LEVEL = "ice_level"
        const val SETTING_REFLECTION_PROBE_AMBIANCE = "reflection_probe_ambiance"
        const val SETTING_CLOUD_VARIANCE = "cloud_variance"
        const val DEFAULT_AUTO_ADJUST_PROBE_AMBIANCE = 1.0f
        var sAutoAdjustProbeAmbiance = 1.0f
    }
}

abstract class LLSettingsWater : LLSettingsBase() {
    abstract fun getTransparentTextureID(): UUID
    abstract fun getNextTransparentTextureID(): UUID
    abstract fun getNormalMapID(): UUID
    abstract fun getNextNormalMapID(): UUID
    abstract fun getModifiedWaterFogDensity(underwater: Boolean): Float
    abstract fun getWaterFogColor(): LLColor4
    abstract fun getBlendFactor(): Double
    abstract override fun buildDerivedClone(): LLSettingsWater

    typealias ptr_t = LLSettingsWater?
    typealias parammapping_t = MutableMap<String, Any>
    typealias validation_list_t = List<Any>

    companion object {
        fun translateLegacySettings(old: Map<String, Any>): Map<String, Any> {
            TODO("APR: convert legacy Windlight water LLSD to EEP settings map")
        }
        fun validationList(): validation_list_t = TODO("APR: return water validation rules list")
        fun defaults(): Map<String, Any> = TODO("APR: return default water settings map")
        fun settingValidation(settings: Map<String, Any>, validations: validation_list_t): Map<String, Any> =
            LLSettingsBase.settingValidation(settings, validations)
        fun GetDefaultOpaqueTextureAssetId(): UUID = TODO("APR: return default opaque water texture UUID")

        const val SETTING_BLUR_MULTIPLIER = "blur_multiplier"
        const val SETTING_FOG_COLOR = "water_fog_color"
        const val SETTING_FOG_DENSITY = "water_fog_density"
        const val SETTING_FOG_MOD = "water_fog_mod"
        const val SETTING_FRESNEL_OFFSET = "fresnel_offset"
        const val SETTING_FRESNEL_SCALE = "fresnel_scale"
        const val SETTING_NORMAL_MAP = "normal_map"
        const val SETTING_NORMAL_SCALE = "normal_scale"
        const val SETTING_SCALE_ABOVE = "scale_above"
        const val SETTING_SCALE_BELOW = "scale_below"
        const val SETTING_WAVE1_DIR = "wave1_direction"
        const val SETTING_WAVE2_DIR = "wave2_direction"
        const val SETTING_LEGACY_BLUR_MULTIPLIER = "blurMultiplier"
        const val SETTING_LEGACY_FOG_COLOR = "waterFogColor"
        const val SETTING_LEGACY_FOG_DENSITY = "waterFogDensity"
        const val SETTING_LEGACY_FOG_MOD = "underWaterFogMod"
        const val SETTING_LEGACY_FRESNEL_OFFSET = "fresnelOffset"
        const val SETTING_LEGACY_FRESNEL_SCALE = "fresnelScale"
        const val SETTING_LEGACY_NORMAL_MAP = "normalMap"
        const val SETTING_LEGACY_NORMAL_SCALE = "normScale"
        const val SETTING_LEGACY_SCALE_ABOVE = "scaleAbove"
        const val SETTING_LEGACY_SCALE_BELOW = "scaleBelow"
        const val SETTING_LEGACY_WAVE1_DIR = "wave1Dir"
        const val SETTING_LEGACY_WAVE2_DIR = "wave2Dir"
    }
}

abstract class LLSettingsDay : LLSettingsBase() {
    data class CycleEntry(val keyframe: Float, val settings: LLSettingsBase)
    typealias CycleTrack_t = MutableMap<Float, LLSettingsBase>
    typealias ptr_t = LLSettingsDay?
    typealias validation_list_t = List<Any>

    abstract fun initialize(validate: Boolean = false)
    abstract fun isTrackEmpty(track: Int): Boolean
    abstract fun clearCycleTrack(track: Int)
    abstract fun setSettingsAtKeyframe(settings: LLSettingsBase, keyframe: Double, track: Int)
    abstract fun getCycleTrack(track: Int): CycleTrack_t
    abstract fun getCycleTrackConst(track: Int): CycleTrack_t
    abstract override fun buildDerivedClone(): LLSettingsDay

    companion object {
        const val TRACK_WATER = 0
        const val TRACK_GROUND_LEVEL = 1
        const val TRACK_MAX = 5
        const val SETTING_TRACKS = "tracks"
        const val SETTING_FRAMES = "frames"
        const val SETTING_KEYKFRAME = "time"
        const val SETTING_KEYNAME = "settings"
        fun validationList(): validation_list_t = TODO("APR: return day cycle validation rules list")
        fun defaults(): Map<String, Any> = TODO("APR: return default day cycle settings map")
        fun settingValidation(settings: Map<String, Any>, validations: validation_list_t): Map<String, Any> =
            LLSettingsBase.settingValidation(settings, validations)
        fun GetDefaultAssetId(): UUID = TODO("APR: return default day-cycle asset UUID")
    }
}

typealias LLSettingsSkyPtr_t = LLSettingsSky?
typealias LLSettingsWaterPtr_t = LLSettingsWater?

// ---- Math/color stubs ----
class LLVector3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    val mV = floatArrayOf(x, y, z)
    operator fun get(i: Int) = mV[i]
    companion object { val zero = LLVector3() }
}
class LLVector4(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 0f) {
    val mV = floatArrayOf(x, y, z, w)
    operator fun get(i: Int) = mV[i]
    operator fun plus(o: LLVector4) = LLVector4(x + o.x, y + o.y, z + o.z, w + o.w)
    operator fun timesAssign(s: Float) {}
    fun getValue(): Map<String, Any> = mapOf("x" to x, "y" to y, "z" to z, "w" to w)
}
class LLColor3(val r: Float = 0f, val g: Float = 0f, val b: Float = 0f) {
    val mV = floatArrayOf(r, g, b)
    operator fun times(s: Float) = LLColor3(r * s, g * s, b * s)
    fun getValue(): Map<String, Any> = mapOf("r" to r, "g" to g, "b" to b)
    fun calcHSL(h: FloatArray, s: FloatArray, l: FloatArray) { TODO("APR: compute HSL from RGB") }
}
class LLColor4(val r: Float = 0f, val g: Float = 0f, val b: Float = 0f, val a: Float = 1f) {
    val mV = floatArrayOf(r, g, b, a)
}
fun linearColor3(c: LLColor4): LLColor3 { TODO("GPU: convert sRGB color to linear space") }

// ---- LLSettingsVOBase ----

class LLSettingsVOBase private constructor() {
    companion object {
        fun createNewInventoryItem(
            stype: LLSettingsType.type_e,
            parentId: UUID,
            createdCb: ((UUID) -> Unit)? = null
        ) {
            TODO("APR: create new inventory settings item via region capability; invoke createdCb on success")
        }

        fun createInventoryItem(
            settings: LLSettingsBase,
            parentId: UUID,
            settingsName: String,
            callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)? = null
        ) {
            TODO("APR: serialize settings, upload asset, create inventory item via capability")
        }

        fun createInventoryItem(
            settings: LLSettingsBase,
            nextOwnerPerm: UInt,
            parentId: UUID,
            settingsName: String,
            callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)? = null
        ) {
            TODO("APR: same as above but with explicit nextOwnerPerm")
        }

        fun updateInventoryItem(
            settings: LLSettingsBase,
            invItemId: UUID,
            callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)? = null,
            updateName: Boolean = true
        ) {
            TODO("APR: serialize and upload updated settings asset via UpdateSettingsAgentInventory capability")
        }

        fun updateInventoryItem(
            settings: LLSettingsBase,
            objectId: UUID,
            invItemId: UUID,
            callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)? = null
        ) {
            TODO("APR: upload updated settings asset to a task object's inventory")
        }

        fun getSettingsAsset(
            assetId: UUID,
            callback: (UUID, LLSettingsBase?, Int, Any) -> Unit
        ) {
            TODO("APR: fetch asset from gAssetStorage; deserialize LLSD; call createFromLLSD; invoke callback")
        }

        fun getSettingsInventory(
            inventoryId: UUID,
            callback: ((LLInventoryItem, LLSettingsBase?, Int, Any) -> Unit)? = null
        ) {
            // intentionally empty in original; reserved for future use
        }

        fun exportFile(
            settings: LLSettingsBase,
            filename: String,
            format: Int = 0
        ): Boolean {
            return try {
                TODO("APR: serialize settings map to LLSD notation and write to file")
            } catch (e: Exception) {
                false
            }
        }

        fun importFile(filename: String): LLSettingsBase? {
            return try {
                TODO("APR: read file, deserialize LLSD, call createFromLLSD")
            } catch (e: Exception) {
                null
            }
        }

        fun createFromLLSD(settings: Map<String, Any>): LLSettingsBase? {
            val type = settings[LLSettingsBase.SETTING_TYPE] as? String ?: return null
            return when (type) {
                "water"    -> LLSettingsVOWater.buildWater(settings)
                "sky"      -> LLSettingsVOSky.buildSky(settings)
                "daycycle" -> LLSettingsVODay.buildDay(settings)
                else       -> null
            }
        }

        private fun onInventoryItemCreated(inventoryId: UUID, settings: LLSettingsBase?, callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)?) {
            TODO("APR: update item permissions to PERM_COPY; upload settings asset if provided; invoke callback")
        }

        private fun onAgentAssetUploadComplete(
            itemId: UUID, newAssetId: UUID, newItemId: UUID,
            response: Map<String, Any>, settings: LLSettingsBase,
            callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)?
        ) {
            settings.setAssetId(newAssetId)
            callback?.invoke(newAssetId, itemId, UUID(0, 0), response)
        }

        private fun onTaskAssetUploadComplete(
            itemId: UUID, taskId: UUID, newAssetId: UUID,
            response: Map<String, Any>, settings: LLSettingsBase,
            callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)?
        ) {
            settings.setAssetId(newAssetId)
            callback?.invoke(newAssetId, itemId, taskId, response)
        }
    }
}

// ---- LLSettingsVOSky ----

class LLSettingsVOSky : LLSettingsSky {
    var isAdvanced: Boolean = false
    var mSceneLightStrength: Float = 3.0f

    constructor(data: Map<String, Any>, advanced: Boolean = false) {
        isAdvanced = advanced
    }

    private constructor() {
        isAdvanced = false
    }

    override fun getSettings(): Map<String, Any> = TODO("APR: return internal settings map")
    override fun getSettingsType(): String = "sky"
    override fun getName(): String = TODO("APR: return name from settings map")
    override fun setName(name: String) { TODO("APR: set name in settings map") }
    override fun getSettingsTypeValue(): Int = TODO("APR: return LLSettingsType ordinal for sky")
    override fun setAssetId(id: UUID) { TODO("APR: store asset ID") }
    override fun getFlags(): UInt = TODO("APR: return flags from settings")
    override fun setFlags(flags: UInt) { TODO("APR: store flags") }
    override fun getFlag(flag: Int): Boolean = TODO("APR: test individual flag bit")
    override fun cloneSettings(): Map<String, Any> = TODO("APR: deep-copy settings map")
    override fun validate(): Boolean = TODO("APR: run validation list against current settings")
    override fun getHash(): Long = TODO("APR: compute stable hash of settings content")
    override fun buildDerivedClone(): LLSettingsSky = buildClone()!!

    override fun getLightDirection(): LLVector3 = TODO("APR: compute light direction from sun/moon angle")
    override fun getSunDirection(): LLVector3 = TODO("APR: return sun direction vector")
    override fun getMoonDirection(): LLVector3 = TODO("APR: return moon direction vector")
    override fun getAmbientColor(): LLColor3 = TODO("APR: return ambient color")
    override fun getBlueDensity(): LLColor3 = TODO("APR: return blue density")
    override fun getBlueHorizon(): LLColor3 = TODO("APR: return blue horizon")
    override fun getHazeDensity(): Float = TODO("APR: return haze density scalar")
    override fun getHazeHorizon(): Float = TODO("APR: return haze horizon scalar")
    override fun getDensityMultiplier(): Float = TODO("APR: return density multiplier")
    override fun getDistanceMultiplier(): Float = TODO("APR: return distance multiplier")
    override fun getCloudPosDensity2(): LLColor3 = TODO("APR: return cloud position/density2")
    override fun getCloudScale(): Float = TODO("APR: return cloud scale")
    override fun getCloudShadow(): Float = TODO("APR: return cloud shadow")
    override fun getCloudVariance(): Float = TODO("APR: return cloud variance")
    override fun getGlow(): LLColor3 = TODO("APR: return glow color")
    override fun getMaxY(): Float = TODO("APR: return max Y")
    override fun getMoonBrightness(): Float = TODO("APR: return moon brightness")
    override fun getSkyMoistureLevel(): Float = TODO("APR: return sky moisture level")
    override fun getSkyDropletRadius(): Float = TODO("APR: return sky droplet radius")
    override fun getSkyIceLevel(): Float = TODO("APR: return sky ice level")
    override fun getReflectionProbeAmbiance(): Float = TODO("APR: return reflection probe ambiance")
    override fun getTotalAmbient(): LLColor3 = TODO("APR: return total ambient light")
    override fun getGamma(): Float = TODO("APR: return gamma")
    override fun getSunlightColor(): LLColor4 = TODO("APR: return sunlight color")
    override fun getMoonlightColor(): LLColor4 = TODO("APR: return moonlight color")
    override fun getCloudColor(): LLColor3 = TODO("APR: return cloud color")
    override fun getIsSunUp(): Boolean = TODO("APR: return true if sun is above horizon")
    override fun getSunMoonGlowFactor(): Float = TODO("APR: return sun/moon glow factor")
    override fun canAutoAdjust(): Boolean = TODO("APR: return true for legacy sky presets that support auto-adjust")
    override fun setTonemapMix(mix: Float) { TODO("APR: set tonemap mix value in settings") }
    override fun getSunTextureId(): UUID = TODO("APR: return sun texture UUID")
    override fun getNextSunTextureId(): UUID = TODO("APR: return next sun texture UUID for blending")
    override fun getMoonTextureId(): UUID = TODO("APR: return moon texture UUID")
    override fun getNextMoonTextureId(): UUID = TODO("APR: return next moon texture UUID")
    override fun getCloudNoiseTextureId(): UUID = TODO("APR: return cloud noise texture UUID")
    override fun getNextCloudNoiseTextureId(): UUID = TODO("APR: return next cloud noise texture UUID")
    override fun getBloomTextureId(): UUID = TODO("APR: return bloom texture UUID")
    override fun getNextBloomTextureId(): UUID = TODO("APR: return next bloom texture UUID")
    override fun getSunScale(): Float = TODO("APR: return sun disc scale")
    override fun getMoonScale(): Float = TODO("APR: return moon disc scale")

    open fun updateSettings() {
        TODO("GPU: push sun/moon direction and texture IDs to gSky; compute mSceneLightStrength from sun elevation")
    }

    open fun applyToUniforms(target: Any?) {
        TODO("GPU: push all atmosphere uniforms to LLShaderUniforms via draw_color/draw_real helpers")
    }

    open fun applySpecial(target: Any?, force: Boolean) {
        TODO("GPU: push light direction, cloud scroll, sun/moon colors, irradiance-pass overrides to shader uniforms")
    }

    open fun getParameterMap(): MutableMap<String, Any> {
        TODO("GPU: return static param_map of setting keys to DefaultParam(shaderUniformId, defaultValue)")
    }

    companion object {
        fun buildSky(settings: Map<String, Any>): LLSettingsSky? {
            val validations = LLSettingsSky.validationList()
            val results = LLSettingsBase.settingValidation(settings, validations)
            if (results["success"] != true) return null
            return LLSettingsVOSky(settings, true)
        }

        fun buildFromLegacyPreset(name: String, oldSettings: Map<String, Any>, messages: MutableMap<String, Any>): LLSettingsSky? {
            val newSettings = LLSettingsSky.translateLegacySettings(oldSettings).toMutableMap()
            if (newSettings.isEmpty()) {
                messages["REASONS"] = "SettingTranslateError: $name"
                return null
            }
            newSettings[LLSettingsBase.SETTING_NAME] = name
            val results = LLSettingsBase.settingValidation(newSettings, LLSettingsSky.validationList())
            if (results["success"] != true) {
                messages["REASONS"] = "SettingValidationError: $name"
                return null
            }
            return LLSettingsVOSky(newSettings)
        }

        fun buildFromLegacyPresetFile(name: String, path: String, messages: MutableMap<String, Any>): LLSettingsSky? {
            val legacyData = readLegacyPresetData(name, path, messages) ?: return null
            return buildFromLegacyPreset(uriUnescape(name), legacyData, messages)
        }

        fun buildDefaultSky(): LLSettingsSky {
            val defaultSettings = LLSettingsSky.defaults().toMutableMap()
            defaultSettings[LLSettingsBase.SETTING_NAME] = LLSettingsBase.DEFAULT_SETTINGS_NAME
            return LLSettingsVOSky(defaultSettings)
        }

        fun convertToLegacy(psky: LLSettingsSky, isAdvanced: Boolean): Map<String, Any> {
            TODO("APR: convert EEP sky settings to legacy Windlight LLSD map; include east_angle/sun_angle computation")
        }

        private fun convertAtmosphericsToLegacy(legacy: MutableMap<String, Any>, settings: Map<String, Any>) {
            TODO("APR: copy legacy haze sub-map values into top-level legacy map with ensure_array_4 padding")
        }
    }

    fun buildClone(): LLSettingsSky? {
        val settings = cloneSettings().toMutableMap()
        val flags = getFlags()
        val results = LLSettingsBase.settingValidation(settings, LLSettingsSky.validationList())
        if (results["success"] != true) return null
        return LLSettingsVOSky(settings).also { it.setFlags(flags) }
    }
}

// ---- LLSettingsVOWater ----

class LLSettingsVOWater : LLSettingsWater {
    constructor(data: Map<String, Any>)
    private constructor()

    override fun getSettings(): Map<String, Any> = TODO("APR: return internal settings map")
    override fun getSettingsType(): String = "water"
    override fun getName(): String = TODO("APR: return name from settings map")
    override fun setName(name: String) { TODO("APR: set name in settings map") }
    override fun getSettingsTypeValue(): Int = TODO("APR: return LLSettingsType ordinal for water")
    override fun setAssetId(id: UUID) { TODO("APR: store asset ID") }
    override fun getFlags(): UInt = TODO("APR: return flags")
    override fun setFlags(flags: UInt) { TODO("APR: store flags") }
    override fun getFlag(flag: Int): Boolean = TODO("APR: test individual flag bit")
    override fun cloneSettings(): Map<String, Any> = TODO("APR: deep-copy settings map")
    override fun validate(): Boolean = TODO("APR: run validation list against current settings")
    override fun getHash(): Long = TODO("APR: compute stable hash of settings content")
    override fun buildDerivedClone(): LLSettingsWater = buildClone()!!

    override fun getTransparentTextureID(): UUID = TODO("APR: return transparent water texture UUID")
    override fun getNextTransparentTextureID(): UUID = TODO("APR: return next transparent water texture UUID")
    override fun getNormalMapID(): UUID = TODO("APR: return water normal map UUID")
    override fun getNextNormalMapID(): UUID = TODO("APR: return next water normal map UUID")
    override fun getModifiedWaterFogDensity(underwater: Boolean): Float = TODO("APR: return fog density adjusted for underwater camera")
    override fun getWaterFogColor(): LLColor4 = TODO("APR: return water fog color")
    override fun getBlendFactor(): Double = TODO("APR: return blend factor for water transition")

    open fun updateSettings() {
        TODO("GPU: push transparent/opaque textures and normal maps to LLDrawPoolWater")
    }

    open fun applyToUniforms(target: Any?) { /* water uniforms pushed in applySpecial */ }

    open fun applySpecial(target: Any?, force: Boolean) {
        TODO("GPU: transform water plane to eye space, push waterplane/fog/mirror uniforms to shader")
    }

    open fun getParameterMap(): MutableMap<String, Any> = mutableMapOf()

    companion object {
        private const val WATER_FOG_LIGHT_CLAMP = 0.3f

        fun buildWater(settings: Map<String, Any>): LLSettingsWater? {
            val results = LLSettingsWater.settingValidation(settings, LLSettingsWater.validationList())
            if (results["success"] != true) return null
            return LLSettingsVOWater(settings)
        }

        fun buildFromLegacyPreset(name: String, oldSettings: Map<String, Any>, messages: MutableMap<String, Any>): LLSettingsWater? {
            val newSettings = LLSettingsWater.translateLegacySettings(oldSettings).toMutableMap()
            if (newSettings.isEmpty()) { messages["REASONS"] = "SettingTranslateError: $name"; return null }
            newSettings[LLSettingsBase.SETTING_NAME] = name
            val results = LLSettingsWater.settingValidation(newSettings, LLSettingsWater.validationList())
            if (results["success"] != true) { messages["REASONS"] = "SettingValidationError: $name"; return null }
            return LLSettingsVOWater(newSettings)
        }

        fun buildFromLegacyPresetFile(name: String, path: String, messages: MutableMap<String, Any>): LLSettingsWater? {
            val legacyData = readLegacyPresetData(name, path, messages) ?: return null
            return buildFromLegacyPreset(uriUnescape(name), legacyData, messages)
        }

        fun buildDefaultWater(): LLSettingsWater {
            val defaultSettings = LLSettingsWater.defaults().toMutableMap()
            defaultSettings[LLSettingsBase.SETTING_NAME] = LLSettingsBase.DEFAULT_SETTINGS_NAME
            return LLSettingsVOWater(defaultSettings)
        }

        fun convertToLegacy(pwater: LLSettingsWater): Map<String, Any> {
            val settings = pwater.getSettings()
            return mapOf(
                LLSettingsWater.SETTING_LEGACY_BLUR_MULTIPLIER  to (settings[LLSettingsWater.SETTING_BLUR_MULTIPLIER]  ?: 0),
                LLSettingsWater.SETTING_LEGACY_FOG_COLOR        to ensureArray4(settings[LLSettingsWater.SETTING_FOG_COLOR], 1.0f),
                LLSettingsWater.SETTING_LEGACY_FOG_DENSITY      to (settings[LLSettingsWater.SETTING_FOG_DENSITY]      ?: 0),
                LLSettingsWater.SETTING_LEGACY_FOG_MOD          to (settings[LLSettingsWater.SETTING_FOG_MOD]          ?: 0),
                LLSettingsWater.SETTING_LEGACY_FRESNEL_OFFSET   to (settings[LLSettingsWater.SETTING_FRESNEL_OFFSET]   ?: 0),
                LLSettingsWater.SETTING_LEGACY_FRESNEL_SCALE    to (settings[LLSettingsWater.SETTING_FRESNEL_SCALE]    ?: 0),
                LLSettingsWater.SETTING_LEGACY_NORMAL_MAP       to (settings[LLSettingsWater.SETTING_NORMAL_MAP]       ?: ""),
                LLSettingsWater.SETTING_LEGACY_NORMAL_SCALE     to (settings[LLSettingsWater.SETTING_NORMAL_SCALE]     ?: 0),
                LLSettingsWater.SETTING_LEGACY_SCALE_ABOVE      to (settings[LLSettingsWater.SETTING_SCALE_ABOVE]      ?: 0),
                LLSettingsWater.SETTING_LEGACY_SCALE_BELOW      to (settings[LLSettingsWater.SETTING_SCALE_BELOW]      ?: 0),
                LLSettingsWater.SETTING_LEGACY_WAVE1_DIR        to (settings[LLSettingsWater.SETTING_WAVE1_DIR]        ?: 0),
                LLSettingsWater.SETTING_LEGACY_WAVE2_DIR        to (settings[LLSettingsWater.SETTING_WAVE2_DIR]        ?: 0)
            )
        }
    }

    fun buildClone(): LLSettingsWater? {
        val settings = cloneSettings().toMutableMap()
        val flags = getFlags()
        val results = LLSettingsWater.settingValidation(settings, LLSettingsWater.validationList())
        if (results["success"] != true) return null
        return LLSettingsVOWater(settings).also { it.setFlags(flags) }
    }
}

// ---- LLSettingsVODay ----

class LLSettingsVODay : LLSettingsDay {
    constructor(data: Map<String, Any>)
    private constructor()

    override fun getSettings(): Map<String, Any> = TODO("APR: return internal settings map")
    override fun getSettingsType(): String = "daycycle"
    override fun getName(): String = TODO("APR: return name from settings map")
    override fun setName(name: String) { TODO("APR: set name in settings map") }
    override fun getSettingsTypeValue(): Int = TODO("APR: return LLSettingsType ordinal for day cycle")
    override fun setAssetId(id: UUID) { TODO("APR: store asset ID") }
    override fun getFlags(): UInt = TODO("APR: return flags")
    override fun setFlags(flags: UInt) { TODO("APR: store flags") }
    override fun getFlag(flag: Int): Boolean = TODO("APR: test individual flag bit")
    override fun cloneSettings(): Map<String, Any> = TODO("APR: deep-copy settings map")
    override fun validate(): Boolean = TODO("APR: run validation list against current settings")
    override fun getHash(): Long = TODO("APR: compute stable hash of settings content")
    override fun buildDerivedClone(): LLSettingsDay = buildClone()!!

    override fun initialize(validate: Boolean) { TODO("APR: expand frames/tracks from settings LLSD into CycleTrack structures") }
    override fun isTrackEmpty(track: Int): Boolean = TODO("APR: return true if the given track has no keyframes")
    override fun clearCycleTrack(track: Int) { TODO("APR: remove all keyframes from the given track") }
    override fun setSettingsAtKeyframe(settings: LLSettingsBase, keyframe: Double, track: Int) {
        TODO("APR: insert or replace settings at the given keyframe position in the specified track")
    }
    override fun getCycleTrack(track: Int): CycleTrack_t = TODO("APR: return mutable cycle track map for track index")
    override fun getCycleTrackConst(track: Int): CycleTrack_t = TODO("APR: return read-only view of cycle track map")

    fun getDefaultSky(): LLSettingsSky = LLSettingsVOSky.buildDefaultSky()
    fun getDefaultWater(): LLSettingsWater = LLSettingsVOWater.buildDefaultWater()

    fun buildSky(settings: Map<String, Any>): LLSettingsSky? {
        val skyp = LLSettingsVOSky(settings)
        return if (skyp.validate()) skyp else null
    }

    fun buildWater(settings: Map<String, Any>): LLSettingsWater? {
        val waterp = LLSettingsVOWater(settings)
        return if (waterp.validate()) waterp else null
    }

    fun buildClone(): LLSettingsDay? {
        val settings = cloneSettings().toMutableMap()
        val flags = getFlags()
        val results = LLSettingsDay.settingValidation(settings, LLSettingsDay.validationList())
        if (results["success"] != true) return null
        return LLSettingsVODay(settings).also {
            it.setName(getName())
            if (flags != 0u) it.setFlags(flags)
            it.initialize()
        }
    }

    fun buildDeepCloneAndUncompress(): LLSettingsDay {
        val settings = getSettings().toMutableMap()
        val flags = getFlags()
        val dayClone = LLSettingsVODay(settings)
        for (i in 0 until TRACK_MAX) {
            for ((keyframe, entry) in getCycleTrackConst(i)) {
                dayClone.setSettingsAtKeyframe(entry.buildDerivedClone(), keyframe.toDouble(), i)
            }
        }
        dayClone.setFlags(flags)
        return dayClone
    }

    companion object {
        fun buildDay(settings: Map<String, Any>): LLSettingsDay? {
            val results = LLSettingsDay.settingValidation(settings, LLSettingsDay.validationList())
            if (results["success"] != true) return null
            return LLSettingsVODay(settings).also { it.initialize() }
        }

        fun buildFromLegacyPreset(
            name: String,
            path: String,
            oldSettings: Map<String, Any>,
            messages: MutableMap<String, Any>
        ): LLSettingsDay? {
            TODO("APR: build water/sky sub-presets from sibling filesystem folders; assemble day-cycle LLSD; validate; return LLSettingsVODay")
        }

        fun buildFromLegacyPresetFile(name: String, path: String, messages: MutableMap<String, Any>): LLSettingsDay? {
            val legacyData = readLegacyPresetData(name, path, messages) ?: return null
            return buildFromLegacyPreset(uriUnescape(name), path, legacyData, messages)
        }

        fun buildFromLegacyMessage(regionId: UUID, daycycle: List<Any>, skys: Map<String, Any>, water: Map<String, Any>): LLSettingsDay? {
            TODO("APR: translate legacy sky/water LLSD from region message into EEP day-cycle settings")
        }

        fun buildDefaultDayCycle(): LLSettingsDay {
            val defaultSettings = LLSettingsDay.defaults().toMutableMap()
            defaultSettings[LLSettingsBase.SETTING_NAME] = LLSettingsBase.DEFAULT_SETTINGS_NAME
            return LLSettingsVODay(defaultSettings).also { it.initialize() }
        }

        fun buildFromEnvironmentMessage(settings: Map<String, Any>): LLSettingsDay? {
            val results = LLSettingsDay.settingValidation(settings, LLSettingsDay.validationList())
            if (results["success"] != true) return null
            return LLSettingsVODay(settings).also { it.initialize() }
        }

        fun buildFromOtherSetting(settings: LLSettingsBase, cb: ((LLSettingsDay?) -> Unit)) {
            if (settings.getSettingsType() == "daycycle") {
                cb(settings as? LLSettingsDay)
            } else {
                LLSettingsVOBase.getSettingsAsset(LLSettingsDay.GetDefaultAssetId()) { _, pday, _, _ ->
                    combineIntoDayCycle(pday as? LLSettingsDay, settings, cb)
                }
            }
        }

        private fun combineIntoDayCycle(pday: LLSettingsDay?, settings: LLSettingsBase, cb: (LLSettingsDay?) -> Unit) {
            if (pday == null) { cb(null); return }
            when (settings.getSettingsType()) {
                "sky" -> {
                    pday.setName("sky: " + settings.getName())
                    pday.clearCycleTrack(1)
                    pday.setSettingsAtKeyframe(settings, 0.0, 1)
                }
                "water" -> {
                    pday.setName("water: " + settings.getName())
                    pday.clearCycleTrack(0)
                    pday.setSettingsAtKeyframe(settings, 0.0, 0)
                }
                else -> { cb(null); return }
            }
            cb(pday)
        }

        fun convertToLegacy(pday: LLSettingsVODay): List<Any> {
            TODO("APR: extract water track and first sky track; convert each to legacy LLSD; return legacy daycycle array")
        }
    }
}

// ---- File utilities ----

private fun readLegacyPresetData(name: String, path: String, messages: MutableMap<String, Any>): Map<String, Any>? {
    TODO("APR: locate XML file in path, deserialize LLSD, return as Map or null on failure (populate messages)")
}

private fun uriUnescape(s: String): String {
    TODO("APR: percent-decode URI-encoded string using java.net.URLDecoder")
}

private fun ensureArray4(value: Any?, fill: Float): List<Any> {
    @Suppress("UNCHECKED_CAST")
    val list = (value as? List<Any>)?.toMutableList() ?: mutableListOf()
    while (list.size < 4) list.add(fill)
    return list
}
