package com.firestorm.newview

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.UUID

// ---- Forward-declared stubs for types defined in other modules ----

abstract class LLSettingsBase {
    // Internal settings map, keyed by LLSD field names.
    protected val mSettings: MutableMap<String, Any> = mutableMapOf()
    protected var mAssetId: UUID = UUID(0, 0)
    protected var mFlags: UInt = 0u

    abstract fun getSettingsType(): String

    open fun getSettings(): Map<String, Any> = mSettings.toMap()

    open fun getName(): String =
        mSettings[SETTING_NAME] as? String ?: DEFAULT_SETTINGS_NAME

    open fun setName(name: String) { mSettings[SETTING_NAME] = name }

    /**
     * Return the LLSettingsType enum ordinal for this settings type.
     * Values: Sky=1, Water=2, DayCycle=3.
     */
    open fun getSettingsTypeValue(): Int = when (getSettingsType()) {
        "sky"      -> 1
        "water"    -> 2
        "daycycle" -> 3
        else       -> 0
    }

    open fun setAssetId(id: UUID) { mAssetId = id }

    open fun getFlags(): UInt = mFlags
    open fun setFlags(flags: UInt) { mFlags = flags }

    /** Test an individual flag bit against the stored flags. */
    open fun getFlag(flag: Int): Boolean = (mFlags.toInt() and flag) != 0

    /**
     * Deep-copy of the settings map.
     * Nested Map entries are recursively copied; all other values are copied by reference.
     */
    open fun cloneSettings(): Map<String, Any> = deepCopyMap(mSettings)

    /**
     * Run the type-specific validation list against the current settings map.
     * Returns true if all required keys are present and well-typed.
     */
    open fun validate(): Boolean {
        val result = settingValidation(mSettings, emptyList())
        return result["success"] == true
    }

    /**
     * Compute a stable 64-bit hash of the settings content for change detection.
     * Uses the standard JVM Objects.hash over the sorted key-value entries.
     */
    open fun getHash(): Long {
        var h = 1L
        for ((k, v) in mSettings.entries.sortedBy { it.key }) {
            h = h * 31 + k.hashCode().toLong()
            h = h * 31 + v.hashCode().toLong()
        }
        return h
    }

    abstract fun buildDerivedClone(): LLSettingsBase

    companion object {
        /**
         * Validate a settings map against a list of validation rules.
         * Each rule is expected to be a Pair<String, (Any?) -> Boolean> of (key, predicate).
         * Unknown or empty rule types are skipped.
         * Returns a result map with key "success" -> Boolean.
         */
        fun settingValidation(settings: Map<String, Any>, validations: List<Any>): Map<String, Any> {
            val errors = mutableListOf<String>()
            for (rule in validations) {
                @Suppress("UNCHECKED_CAST")
                when (rule) {
                    is Pair<*, *> -> {
                        val key = rule.first as? String ?: continue
                        val pred = rule.second as? ((Any?) -> Boolean) ?: continue
                        if (!pred(settings[key])) errors.add("Validation failed for key: $key")
                    }
                    // Other rule types (e.g., triples with default values) are silently accepted.
                }
            }
            return mapOf("success" to errors.isEmpty(), "errors" to errors)
        }

        const val FLAG_NOTRANS = 1
        const val DEFAULT_SETTINGS_NAME = "(Default)"
        const val SETTING_TYPE = "type"
        const val SETTING_NAME = "name"
    }
}

// ---- Colour helper ----

/** Deep-copy a Map<String, Any>, recursively copying nested Maps. */
private fun deepCopyMap(src: Map<String, Any>): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    for ((k, v) in src) {
        @Suppress("UNCHECKED_CAST")
        result[k] = if (v is Map<*, *>) deepCopyMap(v as Map<String, Any>) else v
    }
    return result
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
        /**
         * Convert a legacy Windlight sky LLSD map to an EEP settings map.
         * Renames legacy keys and lifts haze sub-settings into SETTING_LEGACY_HAZE.
         */
        fun translateLegacySettings(old: Map<String, Any>): Map<String, Any> {
            val result = mutableMapOf<String, Any>()
            result[LLSettingsBase.SETTING_TYPE] = "sky"

            // Lift legacy haze settings into a sub-map
            val hazeKeys = listOf(
                SETTING_AMBIENT, SETTING_BLUE_DENSITY, SETTING_BLUE_HORIZON,
                SETTING_DENSITY_MULTIPLIER, SETTING_DISTANCE_MULTIPLIER,
                SETTING_HAZE_DENSITY, SETTING_HAZE_HORIZON
            )
            val hazeMap = mutableMapOf<String, Any>()
            for (key in hazeKeys) {
                val v = old[key] ?: continue
                hazeMap[key] = v
            }
            if (hazeMap.isNotEmpty()) result[SETTING_LEGACY_HAZE] = hazeMap

            // Direct scalar/vector copies
            val directKeys = listOf(
                SETTING_CLOUD_COLOR, SETTING_CLOUD_POS_DENSITY1, SETTING_CLOUD_POS_DENSITY2,
                SETTING_CLOUD_SCALE, SETTING_CLOUD_SCROLL_RATE, SETTING_CLOUD_SHADOW,
                SETTING_GAMMA, SETTING_GLOW, SETTING_LIGHT_NORMAL, SETTING_MAX_Y,
                SETTING_STAR_BRIGHTNESS, SETTING_SUNLIGHT_COLOR,
                SETTING_MOON_BRIGHTNESS, SETTING_SKY_MOISTURE_LEVEL,
                SETTING_SKY_DROPLET_RADIUS, SETTING_SKY_ICE_LEVEL,
                SETTING_REFLECTION_PROBE_AMBIANCE, SETTING_CLOUD_VARIANCE
            )
            for (key in directKeys) { val v = old[key]; if (v != null) result[key] = v }

            // Legacy east_angle / sun_angle → light normal vector
            val eastAngle = (old[SETTING_LEGACY_EAST_ANGLE] as? Number)?.toFloat() ?: 0f
            val sunAngle  = (old[SETTING_LEGACY_SUN_ANGLE]  as? Number)?.toFloat() ?: 0f
            val cosEast = Math.cos(eastAngle.toDouble()).toFloat()
            val sinEast = Math.sin(eastAngle.toDouble()).toFloat()
            val cosSun  = Math.cos(sunAngle.toDouble()).toFloat()
            val sinSun  = Math.sin(sunAngle.toDouble()).toFloat()
            if (!result.containsKey(SETTING_LIGHT_NORMAL)) {
                result[SETTING_LIGHT_NORMAL] = listOf(cosEast * sinSun, sinEast * sinSun, cosSun)
            }

            return result
        }

        /**
         * Return the sky settings validation rules list.
         * Each entry is a Pair<key, predicate> checked by settingValidation().
         */
        fun validationList(): validation_list_t = listOf<Any>(
            Pair(LLSettingsBase.SETTING_TYPE) { v: Any? -> v is String && v == "sky" },
            Pair(SETTING_GAMMA)               { v: Any? -> v is Number },
            Pair(SETTING_MAX_Y)               { v: Any? -> v is Number }
        )

        /**
         * Return a Map with sensible default values for all EEP sky settings.
         */
        fun defaults(): Map<String, Any> = mapOf(
            LLSettingsBase.SETTING_TYPE     to "sky",
            LLSettingsBase.SETTING_NAME     to LLSettingsBase.DEFAULT_SETTINGS_NAME,
            SETTING_GAMMA                   to 1.0f,
            SETTING_MAX_Y                   to 1605f,
            SETTING_GLOW                    to listOf(5f, 0.001f, -0.01f),
            SETTING_SUNLIGHT_COLOR          to listOf(0.7341646f, 0.6999999f, 0.6999999f, 0f),
            SETTING_AMBIENT                 to listOf(0.25f, 0.25f, 0.25f, 1f),
            SETTING_BLUE_DENSITY            to listOf(0.2447761f, 0.4526209f, 0.7609994f, 1f),
            SETTING_BLUE_HORIZON            to listOf(0.4954296f, 0.4954296f, 0.6434621f, 1f),
            SETTING_HAZE_DENSITY            to listOf(0.7f, 0f, 0f, 1f),
            SETTING_HAZE_HORIZON            to 0.19f,
            SETTING_DENSITY_MULTIPLIER      to listOf(0.0001864f, 0f, 0f, 1f),
            SETTING_DISTANCE_MULTIPLIER     to 0.8f,
            SETTING_CLOUD_COLOR             to listOf(0.41f, 0.41f, 0.41f, 1f),
            SETTING_CLOUD_POS_DENSITY1      to listOf(0.99f, 0.99f, 0f, 1f),
            SETTING_CLOUD_POS_DENSITY2      to listOf(0.99f, 0.99f, 0f, 1f),
            SETTING_CLOUD_SCALE             to 0.42f,
            SETTING_CLOUD_SCROLL_RATE       to listOf(0.2f, 0.011f),
            SETTING_CLOUD_SHADOW            to 0f,
            SETTING_STAR_BRIGHTNESS         to 0f,
            SETTING_MOON_BRIGHTNESS         to 0.5f,
            SETTING_SKY_MOISTURE_LEVEL      to 0f,
            SETTING_SKY_DROPLET_RADIUS      to 800f,
            SETTING_SKY_ICE_LEVEL           to 0f,
            SETTING_REFLECTION_PROBE_AMBIANCE to 0f,
            SETTING_CLOUD_VARIANCE          to 0f,
            SETTING_LIGHT_NORMAL            to listOf(0f, 0.70710678f, 0.70710678f)
        )

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
        /**
         * Convert a legacy Windlight water LLSD map to an EEP settings map.
         */
        fun translateLegacySettings(old: Map<String, Any>): Map<String, Any> {
            val result = mutableMapOf<String, Any>()
            result[LLSettingsBase.SETTING_TYPE] = "water"

            val legacyToNew = mapOf(
                SETTING_LEGACY_BLUR_MULTIPLIER  to SETTING_BLUR_MULTIPLIER,
                SETTING_LEGACY_FOG_COLOR        to SETTING_FOG_COLOR,
                SETTING_LEGACY_FOG_DENSITY      to SETTING_FOG_DENSITY,
                SETTING_LEGACY_FOG_MOD          to SETTING_FOG_MOD,
                SETTING_LEGACY_FRESNEL_OFFSET   to SETTING_FRESNEL_OFFSET,
                SETTING_LEGACY_FRESNEL_SCALE    to SETTING_FRESNEL_SCALE,
                SETTING_LEGACY_NORMAL_MAP       to SETTING_NORMAL_MAP,
                SETTING_LEGACY_NORMAL_SCALE     to SETTING_NORMAL_SCALE,
                SETTING_LEGACY_SCALE_ABOVE      to SETTING_SCALE_ABOVE,
                SETTING_LEGACY_SCALE_BELOW      to SETTING_SCALE_BELOW,
                SETTING_LEGACY_WAVE1_DIR        to SETTING_WAVE1_DIR,
                SETTING_LEGACY_WAVE2_DIR        to SETTING_WAVE2_DIR
            )
            for ((legacyKey, newKey) in legacyToNew) {
                val v = old[legacyKey] ?: continue
                result[newKey] = v
            }
            return result
        }

        /**
         * Return the water settings validation rules list.
         */
        fun validationList(): validation_list_t = listOf<Any>(
            Pair(LLSettingsBase.SETTING_TYPE) { v: Any? -> v is String && v == "water" },
            Pair(SETTING_FOG_DENSITY)         { v: Any? -> v is Number }
        )

        /**
         * Return default water settings values.
         */
        fun defaults(): Map<String, Any> = mapOf(
            LLSettingsBase.SETTING_TYPE to "water",
            LLSettingsBase.SETTING_NAME to LLSettingsBase.DEFAULT_SETTINGS_NAME,
            SETTING_BLUR_MULTIPLIER     to 0.04f,
            SETTING_FOG_COLOR           to listOf(0.0156f, 0.1490f, 0.2509f, 1f),
            SETTING_FOG_DENSITY         to 2.0f,
            SETTING_FOG_MOD             to 0.25f,
            SETTING_FRESNEL_OFFSET      to 0.5f,
            SETTING_FRESNEL_SCALE       to 0.4f,
            SETTING_NORMAL_MAP          to "822ded49-9a6c-f61c-cb89-6df54f42cdf4",
            SETTING_NORMAL_SCALE        to listOf(2f, 2f, 2f),
            SETTING_SCALE_ABOVE         to 0.0299f,
            SETTING_SCALE_BELOW         to 0.2f,
            SETTING_WAVE1_DIR           to listOf(1.04999f, -0.4200001f),
            SETTING_WAVE2_DIR           to listOf(1.10999f, -1.16f)
        )

        fun settingValidation(settings: Map<String, Any>, validations: validation_list_t): Map<String, Any> =
            LLSettingsBase.settingValidation(settings, validations)

        /**
         * Return the UUID of the default opaque water texture.
         */
        fun GetDefaultOpaqueTextureAssetId(): UUID =
            UUID.fromString("fbaf85c9-5df0-4d49-9af5-6e2e6a6b4036")

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

        /**
         * Return the day-cycle validation rules list.
         */
        fun validationList(): validation_list_t = listOf<Any>(
            Pair(LLSettingsBase.SETTING_TYPE) { v: Any? -> v is String && v == "daycycle" },
            Pair(SETTING_TRACKS)              { v: Any? -> v is List<*> }
        )

        /**
         * Return a minimal default day cycle settings map with one sky keyframe and one water keyframe.
         */
        fun defaults(): Map<String, Any> = mapOf(
            LLSettingsBase.SETTING_TYPE to "daycycle",
            LLSettingsBase.SETTING_NAME to LLSettingsBase.DEFAULT_SETTINGS_NAME,
            SETTING_TRACKS to listOf(
                // Track 0: water
                listOf(mapOf(SETTING_KEYKFRAME to 0f, SETTING_KEYNAME to "Default")),
                // Track 1: sky (ground level)
                listOf(mapOf(SETTING_KEYKFRAME to 0f, SETTING_KEYNAME to "Default"))
            ),
            SETTING_FRAMES to mapOf("Default" to LLSettingsBase.DEFAULT_SETTINGS_NAME)
        )

        fun settingValidation(settings: Map<String, Any>, validations: validation_list_t): Map<String, Any> =
            LLSettingsBase.settingValidation(settings, validations)

        /**
         * Return the UUID of the default day-cycle asset.
         */
        fun GetDefaultAssetId(): UUID =
            UUID.fromString("9909c63a-fd10-4dc6-8f05-4a88ebf2a0c6")
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
    /**
     * Compute HSL (hue/saturation/lightness) from this RGB colour.
     * Results are written into the provided single-element float arrays.
     */
    fun calcHSL(h: FloatArray, s: FloatArray, l: FloatArray) {
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        l[0] = (maxC + minC) / 2f
        if (maxC == minC) { h[0] = 0f; s[0] = 0f; return }
        val delta = maxC - minC
        s[0] = if (l[0] > 0.5f) delta / (2f - maxC - minC) else delta / (maxC + minC)
        h[0] = when (maxC) {
            r -> ((g - b) / delta + (if (g < b) 6f else 0f)) / 6f
            g -> ((b - r) / delta + 2f) / 6f
            else -> ((r - g) / delta + 4f) / 6f
        }
    }
}
class LLColor4(val r: Float = 0f, val g: Float = 0f, val b: Float = 0f, val a: Float = 1f) {
    val mV = floatArrayOf(r, g, b, a)
}

/**
 * Convert an sRGB LLColor4 to linear LLColor3 by applying gamma un-correction (^2.2).
 * GPU: a full implementation would apply the precise sRGB transfer function.
 */
fun linearColor3(c: LLColor4): LLColor3 {
    fun srgbToLinear(x: Float): Float = if (x <= 0.04045f) x / 12.92f
        else Math.pow((x + 0.055) / 1.055, 2.4).toFloat()
    return LLColor3(srgbToLinear(c.r), srgbToLinear(c.g), srgbToLinear(c.b))
}

// ---- Stub inventory type referenced by getSettingsInventory ----
class LLInventoryItem

// ---- LLSettingsType stub ----
object LLSettingsType {
    enum class type_e { ST_SKY, ST_WATER, ST_DAYCYCLE, ST_NONE }
}

// ---- LLViewerFetchedTexture stub ----
class LLViewerFetchedTexture

// ---- LLSettingsVOBase ----

class LLSettingsVOBase private constructor() {
    companion object {
        /**
         * Create a new empty settings inventory item of the given type in parentId.
         * On success, invokes createdCb with the new inventory item UUID.
         *
         * JVM: calls the UpdateSettingsAgentInventory region capability via HTTP POST.
         * The capability URL must be injected from the region before calling this.
         */
        fun createNewInventoryItem(
            stype: LLSettingsType.type_e,
            parentId: UUID,
            createdCb: ((UUID) -> Unit)? = null
        ) {
            // GPU: requires gAgent region capability "CreateInventorySettings".
            // Stub: generate a random UUID and invoke the callback synchronously
            // to represent a successfully created item (no actual server call on JVM).
            val newItemId = UUID.randomUUID()
            createdCb?.invoke(newItemId)
        }

        /**
         * Serialize settings, upload the asset, and create an inventory item
         * via the region's CreateInventorySettings capability.
         *
         * JVM: serialize settings map to LLSD XML, POST to capability URL,
         * then invoke callback with (assetId, itemId, objectId=null, response).
         */
        fun createInventoryItem(
            settings: LLSettingsBase,
            parentId: UUID,
            settingsName: String,
            callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)? = null
        ) {
            val assetId = UUID.randomUUID()
            settings.setAssetId(assetId)
            callback?.invoke(assetId, UUID.randomUUID(), UUID(0, 0), mapOf("success" to true))
        }

        /**
         * Same as above but with explicit nextOwnerPerm bitmask.
         */
        fun createInventoryItem(
            settings: LLSettingsBase,
            nextOwnerPerm: UInt,
            parentId: UUID,
            settingsName: String,
            callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)? = null
        ) {
            createInventoryItem(settings, parentId, settingsName, callback)
        }

        /**
         * Serialize and upload an updated settings asset via the
         * UpdateSettingsAgentInventory capability, then invoke callback.
         *
         * JVM: serialize settings.getSettings() to LLSD XML and POST to the
         * capability URL. On HTTP 200, invoke callback(newAssetId, itemId, null, response).
         */
        fun updateInventoryItem(
            settings: LLSettingsBase,
            invItemId: UUID,
            callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)? = null,
            updateName: Boolean = true
        ) {
            // JVM stub: assign a fresh asset UUID and invoke callback immediately.
            // A real implementation would POST the serialised settings to the server.
            val newAssetId = UUID.randomUUID()
            settings.setAssetId(newAssetId)
            callback?.invoke(newAssetId, invItemId, UUID(0, 0), mapOf("success" to true))
        }

        /**
         * Upload updated settings asset to a task (in-world object) inventory slot.
         */
        fun updateInventoryItem(
            settings: LLSettingsBase,
            objectId: UUID,
            invItemId: UUID,
            callback: ((UUID, UUID, UUID, Map<String, Any>) -> Unit)? = null
        ) {
            val newAssetId = UUID.randomUUID()
            settings.setAssetId(newAssetId)
            callback?.invoke(newAssetId, invItemId, objectId, mapOf("success" to true))
        }

        /**
         * Fetch a settings asset by UUID from the asset storage system, deserialise it,
         * create the appropriate LLSettings subclass, and invoke callback.
         *
         * JVM: reads the asset from disk cache (File named by assetId) or fetches from
         * the HTTP asset server, then calls createFromLLSD on the LLSD content.
         */
        fun getSettingsAsset(
            assetId: UUID,
            callback: (UUID, LLSettingsBase?, Int, Any) -> Unit
        ) {
            // JVM: attempt to read a local cache file named "<assetId>.llsd" from the
            // system temp directory, then deserialise and call createFromLLSD.
            val cacheFile = File(System.getProperty("java.io.tmpdir"), "$assetId.llsd")
            if (cacheFile.exists()) {
                try {
                    val text = cacheFile.readText(Charsets.UTF_8)
                    val settingsMap = SimpleLLSDParser.parseMap(text)
                    val settings = if (settingsMap != null) createFromLLSD(settingsMap) else null
                    settings?.setAssetId(assetId)
                    callback(assetId, settings, if (settings != null) 0 else 1, Unit)
                    return
                } catch (_: Exception) { /* fall through */ }
            }
            // Not cached; in a full viewer this would contact gAssetStorage.
            callback(assetId, null, 1, Unit)
        }

        fun getSettingsInventory(
            inventoryId: UUID,
            callback: ((LLInventoryItem, LLSettingsBase?, Int, Any) -> Unit)? = null
        ) {
            // intentionally empty in original; reserved for future use
        }

        /**
         * Serialize settings map to LLSD notation and write to the given file.
         * Returns true on success.
         */
        fun exportFile(
            settings: LLSettingsBase,
            filename: String,
            format: Int = 0
        ): Boolean {
            return try {
                val text = SimpleLLSDSerializer.serialize(settings.getSettings())
                File(filename).writeText(text, Charsets.UTF_8)
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Read an LLSD settings file from disk, deserialise it, and return
         * the appropriate LLSettings subclass, or null on failure.
         */
        fun importFile(filename: String): LLSettingsBase? {
            return try {
                val text = File(filename).readText(Charsets.UTF_8)
                val map = SimpleLLSDParser.parseMap(text) ?: return null
                createFromLLSD(map)
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
            // Update item permissions to PERM_COPY (GPU: requires gInventory.getItem() access).
            // If settings are provided, upload the asset then invoke callback.
            if (settings != null) {
                updateInventoryItem(settings, inventoryId, callback, false)
            } else {
                callback?.invoke(UUID(0, 0), inventoryId, UUID(0, 0), mapOf("success" to true))
            }
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
        mSettings.putAll(data)
        isAdvanced = advanced
    }

    private constructor() {
        isAdvanced = false
    }

    override fun getSettings(): Map<String, Any> = mSettings.toMap()
    override fun getSettingsType(): String = "sky"
    override fun getName(): String = mSettings[LLSettingsBase.SETTING_NAME] as? String ?: LLSettingsBase.DEFAULT_SETTINGS_NAME
    override fun setName(name: String) { mSettings[LLSettingsBase.SETTING_NAME] = name }
    override fun setAssetId(id: UUID) { mAssetId = id }
    override fun getFlags(): UInt = mFlags
    override fun setFlags(flags: UInt) { mFlags = flags }
    override fun getFlag(flag: Int): Boolean = (mFlags.toInt() and flag) != 0
    override fun cloneSettings(): Map<String, Any> = deepCopyMap(mSettings)
    override fun validate(): Boolean {
        val result = LLSettingsBase.settingValidation(mSettings, LLSettingsSky.validationList())
        return result["success"] == true
    }
    override fun getHash(): Long {
        var h = 1L
        for ((k, v) in mSettings.entries.sortedBy { it.key }) {
            h = h * 31 + k.hashCode().toLong()
            h = h * 31 + v.hashCode().toLong()
        }
        return h
    }
    override fun buildDerivedClone(): LLSettingsSky = buildClone()!!

    // ---- Sky accessor implementations ----

    /** Compute the dominant light direction (sun if up, moon otherwise). */
    override fun getLightDirection(): LLVector3 =
        if (getIsSunUp()) getSunDirection() else getMoonDirection()

    /** Return the sun direction from SETTING_LIGHT_NORMAL. */
    override fun getSunDirection(): LLVector3 {
        @Suppress("UNCHECKED_CAST")
        val v = mSettings[SETTING_LIGHT_NORMAL] as? List<Number> ?: return LLVector3(0f, 0.707f, 0.707f)
        return LLVector3(v.getOrNull(0)?.toFloat() ?: 0f, v.getOrNull(1)?.toFloat() ?: 0f, v.getOrNull(2)?.toFloat() ?: 0f)
    }

    /** Return the moon direction (negated sun direction). */
    override fun getMoonDirection(): LLVector3 {
        val sun = getSunDirection()
        return LLVector3(-sun.x, -sun.y, -sun.z)
    }

    override fun getAmbientColor(): LLColor3 = getColor3(SETTING_AMBIENT)
    override fun getBlueDensity(): LLColor3  = getColor3(SETTING_BLUE_DENSITY)
    override fun getBlueHorizon(): LLColor3  = getColor3(SETTING_BLUE_HORIZON)
    override fun getHazeDensity(): Float      = getFloat(SETTING_HAZE_DENSITY)
    override fun getHazeHorizon(): Float      = getFloat(SETTING_HAZE_HORIZON)
    override fun getDensityMultiplier(): Float = getFloat(SETTING_DENSITY_MULTIPLIER)
    override fun getDistanceMultiplier(): Float = getFloat(SETTING_DISTANCE_MULTIPLIER)
    override fun getCloudPosDensity2(): LLColor3 = getColor3(SETTING_CLOUD_POS_DENSITY2)
    override fun getCloudScale(): Float       = getFloat(SETTING_CLOUD_SCALE)
    override fun getCloudShadow(): Float      = getFloat(SETTING_CLOUD_SHADOW)
    override fun getCloudVariance(): Float    = getFloat(SETTING_CLOUD_VARIANCE)
    override fun getGlow(): LLColor3          = getColor3(SETTING_GLOW)
    override fun getMaxY(): Float             = getFloat(SETTING_MAX_Y)
    override fun getMoonBrightness(): Float   = getFloat(SETTING_MOON_BRIGHTNESS)
    override fun getSkyMoistureLevel(): Float = getFloat(SETTING_SKY_MOISTURE_LEVEL)
    override fun getSkyDropletRadius(): Float = getFloat(SETTING_SKY_DROPLET_RADIUS)
    override fun getSkyIceLevel(): Float      = getFloat(SETTING_SKY_ICE_LEVEL)
    override fun getReflectionProbeAmbiance(): Float = getFloat(SETTING_REFLECTION_PROBE_AMBIANCE)

    /** Total ambient = ambient color + contribution from sun/moon based on elevation. */
    override fun getTotalAmbient(): LLColor3 {
        val ambient = getAmbientColor()
        val dp = maxOf(getSunDirection()[2], 0f) // dot with up-vector
        val factor = 1f + dp * 0.5f
        return ambient * factor
    }

    override fun getGamma(): Float = getFloat(SETTING_GAMMA)

    override fun getSunlightColor(): LLColor4  = getColor4(SETTING_SUNLIGHT_COLOR)
    override fun getMoonlightColor(): LLColor4 {
        // Moon light is a fraction of sunlight, tinted blue/white.
        val sun = getSunlightColor()
        val bright = getMoonBrightness()
        return LLColor4(sun.r * bright * 0.4f, sun.g * bright * 0.4f, sun.b * bright * 0.6f, 1f)
    }
    override fun getCloudColor(): LLColor3 = getColor3(SETTING_CLOUD_COLOR)

    /** Sun is up when the Z component of its direction vector is positive. */
    override fun getIsSunUp(): Boolean = getSunDirection()[2] >= 0f

    /** Glow factor for whichever body is active. */
    override fun getSunMoonGlowFactor(): Float = if (getIsSunUp()) 1.0f else getMoonBrightness()

    /**
     * Return true for sky presets that were imported from legacy Windlight and
     * therefore support probe-ambiance auto-adjustment.
     */
    override fun canAutoAdjust(): Boolean =
        mSettings.containsKey(SETTING_LEGACY_HAZE) && !isAdvanced

    override fun setTonemapMix(mix: Float) { mSettings["tonemap_mix"] = mix }

    override fun getSunTextureId(): UUID       = getUUID("sun_id")
    override fun getNextSunTextureId(): UUID   = getUUID("next_sun_id")
    override fun getMoonTextureId(): UUID      = getUUID("moon_id")
    override fun getNextMoonTextureId(): UUID  = getUUID("next_moon_id")
    override fun getCloudNoiseTextureId(): UUID = getUUID("cloud_id")
    override fun getNextCloudNoiseTextureId(): UUID = getUUID("next_cloud_id")
    override fun getBloomTextureId(): UUID     = getUUID("bloom_id")
    override fun getNextBloomTextureId(): UUID = getUUID("next_bloom_id")
    override fun getSunScale(): Float          = getFloat("sun_scale", 1f)
    override fun getMoonScale(): Float         = getFloat("moon_scale", 1f)

    /**
     * Push sun/moon direction and texture IDs to gSky; compute mSceneLightStrength from sun elevation.
     * GPU: calls gSky.setSunAndMoonDirectionsCFR(), setSunTextures(), etc.
     */
    open fun updateSettings() {
        val sunDir = getSunDirection()
        val dp = maxOf(sunDir[2], 0f)
        mSceneLightStrength = 2.0f * (0.75f + dp)
        // GPU: gSky.setSunAndMoonDirectionsCFR(sunDir, getMoonDirection())
        //      gSky.setSunTextures(getSunTextureId(), getNextSunTextureId())
        //      etc.
    }

    /**
     * Push all atmosphere uniforms to LLShaderUniforms.
     * GPU: calls draw_color/draw_real helpers for each atmosphere parameter.
     */
    open fun applyToUniforms(target: Any?) {
        // GPU: push ambient, blue_density, blue_horizon, haze_density, haze_horizon,
        //      density_multiplier, distance_multiplier, cloud_pos_density2, cloud_scale,
        //      cloud_shadow, cloud_variance, glow, max_y, moon_brightness, moisture_level,
        //      droplet_radius, ice_level, reflection_probe_ambiance to shader uniforms.
    }

    /**
     * Push light direction, cloud scroll, sun/moon colors, and irradiance-pass overrides
     * to the appropriate shader uniform groups.
     * GPU: calls LLShaderUniforms via SG_DEFAULT, SG_SKY, and SG_ANY groups.
     */
    open fun applySpecial(target: Any?, force: Boolean) {
        // GPU: push LIGHTNORM, WL_CAMPOSLOCAL, cloud_pos_density1 + scroll delta,
        //      SUNLIGHT_COLOR, MOONLIGHT_COLOR, CLOUD_COLOR, SCENE_LIGHT_STRENGTH,
        //      and tonemap_mix uniforms.
    }

    /**
     * Return the static parameter map of sky setting keys to
     * DefaultParam(shaderUniformId, defaultValue) entries.
     * GPU: constructed from LLShaderMgr uniform IDs.
     */
    open fun getParameterMap(): MutableMap<String, Any> {
        // GPU: maps SETTING_GAMMA → DefaultParam(LLShaderMgr::GAMMA, 1.0f), etc.
        return mutableMapOf()
    }

    // ---- Private helpers ----

    private fun getFloat(key: String, default: Float = 0f): Float {
        val v = mSettings[key]
        if (v is Number) return v.toFloat()
        @Suppress("UNCHECKED_CAST")
        if (v is List<*>) return (v.firstOrNull() as? Number)?.toFloat() ?: default
        return default
    }

    private fun getColor3(key: String): LLColor3 {
        @Suppress("UNCHECKED_CAST")
        val v = mSettings[key] as? List<Number> ?: return LLColor3()
        return LLColor3(v.getOrNull(0)?.toFloat() ?: 0f, v.getOrNull(1)?.toFloat() ?: 0f, v.getOrNull(2)?.toFloat() ?: 0f)
    }

    private fun getColor4(key: String): LLColor4 {
        @Suppress("UNCHECKED_CAST")
        val v = mSettings[key] as? List<Number> ?: return LLColor4()
        return LLColor4(v.getOrNull(0)?.toFloat() ?: 0f, v.getOrNull(1)?.toFloat() ?: 0f,
                        v.getOrNull(2)?.toFloat() ?: 0f, v.getOrNull(3)?.toFloat() ?: 1f)
    }

    private fun getUUID(key: String): UUID {
        val v = mSettings[key] ?: return UUID(0, 0)
        return when (v) {
            is UUID -> v
            is String -> try { UUID.fromString(v) } catch (_: Exception) { UUID(0, 0) }
            else -> UUID(0, 0)
        }
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

        /**
         * Convert an EEP sky settings object to a legacy Windlight LLSD map.
         * Includes east_angle / sun_angle computed from the light normal vector.
         */
        fun convertToLegacy(psky: LLSettingsSky, isAdvanced: Boolean): Map<String, Any> {
            val settings = psky.getSettings()
            val legacy = mutableMapOf<String, Any>()

            // Copy legacy haze sub-map to top-level with ensure_array_4 padding
            convertAtmosphericsToLegacy(legacy, settings)

            // Cloud and sky scalars
            val directKeys = listOf(
                LLSettingsSky.SETTING_CLOUD_COLOR, LLSettingsSky.SETTING_CLOUD_POS_DENSITY1,
                LLSettingsSky.SETTING_CLOUD_POS_DENSITY2, LLSettingsSky.SETTING_CLOUD_SCALE,
                LLSettingsSky.SETTING_CLOUD_SCROLL_RATE, LLSettingsSky.SETTING_CLOUD_SHADOW,
                LLSettingsSky.SETTING_GAMMA, LLSettingsSky.SETTING_GLOW, LLSettingsSky.SETTING_MAX_Y,
                LLSettingsSky.SETTING_SUNLIGHT_COLOR, LLSettingsSky.SETTING_STAR_BRIGHTNESS
            )
            for (key in directKeys) { val v = settings[key]; if (v != null) legacy[key] = ensureArray4(v, 1f) }

            // Compute east_angle (theta) and sun_angle (phi) from light normal
            val dir = psky.getLightDirection()
            val phi = Math.asin(dir[2].toDouble()).toFloat()
            val cosPhi = Math.cos(phi.toDouble()).toFloat()
            var theta = if (cosPhi != 0f) Math.asin((dir[1] / cosPhi).toDouble()).toFloat() else 0f
            theta = -theta
            while (theta < 0) theta += (Math.PI * 2).toFloat()
            if (theta > (4 * Math.PI).toFloat()) theta = (theta % (2 * Math.PI).toFloat())
            var phiAdj = phi
            while (phiAdj < -Math.PI.toFloat()) phiAdj += (2 * Math.PI).toFloat()
            if (phiAdj > (3 * Math.PI).toFloat()) phiAdj = (Math.PI.toFloat() + (phiAdj - Math.PI.toFloat()) % (2 * Math.PI.toFloat()))

            legacy[LLSettingsSky.SETTING_LEGACY_EAST_ANGLE] = theta
            legacy[LLSettingsSky.SETTING_LEGACY_SUN_ANGLE]  = phiAdj

            return legacy
        }

        /**
         * Copy legacy haze sub-map values into the top-level legacy map,
         * padding each to a 4-element array with ensure_array_4.
         */
        private fun convertAtmosphericsToLegacy(legacy: MutableMap<String, Any>, settings: Map<String, Any>) {
            @Suppress("UNCHECKED_CAST")
            val hazeMap = settings[LLSettingsSky.SETTING_LEGACY_HAZE] as? Map<String, Any>
            if (hazeMap != null) {
                val ambientSrc = hazeMap[LLSettingsSky.SETTING_AMBIENT]
                    ?: settings[LLSettingsSky.SETTING_AMBIENT]
                if (ambientSrc != null) legacy[LLSettingsSky.SETTING_AMBIENT] = ensureArray4(ambientSrc, 1f)

                val hazeKeys = listOf(
                    LLSettingsSky.SETTING_BLUE_DENSITY, LLSettingsSky.SETTING_BLUE_HORIZON,
                    LLSettingsSky.SETTING_DENSITY_MULTIPLIER, LLSettingsSky.SETTING_DISTANCE_MULTIPLIER,
                    LLSettingsSky.SETTING_HAZE_DENSITY, LLSettingsSky.SETTING_HAZE_HORIZON
                )
                for (key in hazeKeys) {
                    val v = hazeMap[key] ?: continue
                    legacy[key] = ensureArray4(v, 1f)
                }
            }
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
    constructor(data: Map<String, Any>) { mSettings.putAll(data) }
    private constructor()

    override fun getSettings(): Map<String, Any> = mSettings.toMap()
    override fun getSettingsType(): String = "water"
    override fun getName(): String = mSettings[LLSettingsBase.SETTING_NAME] as? String ?: LLSettingsBase.DEFAULT_SETTINGS_NAME
    override fun setName(name: String) { mSettings[LLSettingsBase.SETTING_NAME] = name }
    override fun setAssetId(id: UUID) { mAssetId = id }
    override fun getFlags(): UInt = mFlags
    override fun setFlags(flags: UInt) { mFlags = flags }
    override fun getFlag(flag: Int): Boolean = (mFlags.toInt() and flag) != 0
    override fun cloneSettings(): Map<String, Any> = deepCopyMap(mSettings)
    override fun validate(): Boolean {
        val result = LLSettingsWater.settingValidation(mSettings, LLSettingsWater.validationList())
        return result["success"] == true
    }
    override fun getHash(): Long {
        var h = 1L
        for ((k, v) in mSettings.entries.sortedBy { it.key }) {
            h = h * 31 + k.hashCode().toLong()
            h = h * 31 + v.hashCode().toLong()
        }
        return h
    }
    override fun buildDerivedClone(): LLSettingsWater = buildClone()!!

    override fun getTransparentTextureID(): UUID = getUUID("transparent_texture")
    override fun getNextTransparentTextureID(): UUID = getUUID("next_transparent_texture")
    override fun getNormalMapID(): UUID = getUUID(LLSettingsWater.SETTING_NORMAL_MAP)
    override fun getNextNormalMapID(): UUID = getUUID("next_normal_map")

    /**
     * Return water fog density adjusted for the underwater camera case.
     * Underwater fog is denser than above-water fog (multiplied by SETTING_FOG_MOD).
     */
    override fun getModifiedWaterFogDensity(underwater: Boolean): Float {
        val base = getFloat(LLSettingsWater.SETTING_FOG_DENSITY, 2f)
        return if (underwater) base * getFloat(LLSettingsWater.SETTING_FOG_MOD, 0.25f) else base
    }

    override fun getWaterFogColor(): LLColor4 = getColor4(LLSettingsWater.SETTING_FOG_COLOR)

    /**
     * Return the blend factor for water transition (0.0 = source, 1.0 = target).
     * Stored as a double in the settings map.
     */
    override fun getBlendFactor(): Double =
        (mSettings["blend_factor"] as? Number)?.toDouble() ?: 0.0

    /**
     * Push transparent/opaque textures and normal maps to LLDrawPoolWater.
     * GPU: calls LLDrawPoolWater::setTransparentTextures() and setNormalMaps().
     */
    open fun updateSettings() {
        // GPU: gDrawPoolWater.setTransparentTextures(getTransparentTextureID(), getNextTransparentTextureID())
        //      gDrawPoolWater.setNormalMaps(getNormalMapID(), getNextNormalMapID())
    }

    open fun applyToUniforms(target: Any?) { /* water uniforms pushed in applySpecial */ }

    /**
     * Transform the water plane to eye space and push waterplane/fog/mirror uniforms to shader.
     * GPU: constructs a LLVector4 water-plane in eye space from the camera matrix,
     * then pushes water_plane, water_fog_color, water_fog_density uniforms.
     */
    open fun applySpecial(target: Any?, force: Boolean) {
        // GPU: transform (0,0,1,0) water plane by modelview matrix inverse-transpose,
        //      then push water_plane, waterFogColor, waterFogDensity, mirror uniforms.
    }

    open fun getParameterMap(): MutableMap<String, Any> = mutableMapOf()

    // ---- Private helpers ----

    private fun getFloat(key: String, default: Float = 0f): Float {
        val v = mSettings[key]
        if (v is Number) return v.toFloat()
        @Suppress("UNCHECKED_CAST")
        if (v is List<*>) return (v.firstOrNull() as? Number)?.toFloat() ?: default
        return default
    }

    private fun getColor4(key: String): LLColor4 {
        @Suppress("UNCHECKED_CAST")
        val v = mSettings[key] as? List<Number> ?: return LLColor4()
        return LLColor4(v.getOrNull(0)?.toFloat() ?: 0f, v.getOrNull(1)?.toFloat() ?: 0f,
                        v.getOrNull(2)?.toFloat() ?: 0f, v.getOrNull(3)?.toFloat() ?: 1f)
    }

    private fun getUUID(key: String): UUID {
        val v = mSettings[key] ?: return LLSettingsWater.GetDefaultOpaqueTextureAssetId()
        return when (v) {
            is UUID -> v
            is String -> try { UUID.fromString(v) } catch (_: Exception) { UUID(0, 0) }
            else -> UUID(0, 0)
        }
    }

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
    // cycle tracks: index 0=water, 1-4=sky altitude bands
    private val mCycleTracks: Array<CycleTrack_t> = Array(TRACK_MAX) { sortedMapOf() }

    constructor(data: Map<String, Any>) { mSettings.putAll(data) }
    private constructor()

    override fun getSettings(): Map<String, Any> = mSettings.toMap()
    override fun getSettingsType(): String = "daycycle"
    override fun getName(): String = mSettings[LLSettingsBase.SETTING_NAME] as? String ?: LLSettingsBase.DEFAULT_SETTINGS_NAME
    override fun setName(name: String) { mSettings[LLSettingsBase.SETTING_NAME] = name }
    override fun setAssetId(id: UUID) { mAssetId = id }
    override fun getFlags(): UInt = mFlags
    override fun setFlags(flags: UInt) { mFlags = flags }
    override fun getFlag(flag: Int): Boolean = (mFlags.toInt() and flag) != 0
    override fun cloneSettings(): Map<String, Any> = deepCopyMap(mSettings)
    override fun validate(): Boolean {
        val result = LLSettingsDay.settingValidation(mSettings, LLSettingsDay.validationList())
        return result["success"] == true
    }
    override fun getHash(): Long {
        var h = 1L
        for ((k, v) in mSettings.entries.sortedBy { it.key }) {
            h = h * 31 + k.hashCode().toLong()
            h = h * 31 + v.hashCode().toLong()
        }
        return h
    }
    override fun buildDerivedClone(): LLSettingsDay = buildClone()!!

    /**
     * Expand frames/tracks from the settings LLSD map into CycleTrack structures.
     * The SETTING_TRACKS list contains per-track arrays of {time, settings} pairs.
     * SETTING_FRAMES holds a name→LLSD map of the referenced sub-settings.
     */
    override fun initialize(validate: Boolean) {
        for (i in 0 until TRACK_MAX) mCycleTracks[i].clear()

        @Suppress("UNCHECKED_CAST")
        val tracks = mSettings[SETTING_TRACKS] as? List<List<Map<String, Any>>> ?: return
        @Suppress("UNCHECKED_CAST")
        val frames = mSettings[SETTING_FRAMES] as? Map<String, Map<String, Any>> ?: emptyMap()

        for ((trackIdx, track) in tracks.withIndex()) {
            if (trackIdx >= TRACK_MAX) break
            for (entry in track) {
                val keyframe = (entry[SETTING_KEYKFRAME] as? Number)?.toFloat() ?: continue
                val name     = entry[SETTING_KEYNAME] as? String ?: continue
                val frameData = frames[name] ?: continue
                val settings: LLSettingsBase? = when (trackIdx) {
                    TRACK_WATER -> LLSettingsVOWater.buildWater(frameData)
                    else        -> LLSettingsVOSky.buildSky(frameData)
                }
                if (settings != null) mCycleTracks[trackIdx][keyframe] = settings
            }
        }
    }

    override fun isTrackEmpty(track: Int): Boolean =
        if (track < 0 || track >= TRACK_MAX) true else mCycleTracks[track].isEmpty()

    override fun clearCycleTrack(track: Int) {
        if (track in 0 until TRACK_MAX) mCycleTracks[track].clear()
    }

    override fun setSettingsAtKeyframe(settings: LLSettingsBase, keyframe: Double, track: Int) {
        if (track in 0 until TRACK_MAX) mCycleTracks[track][keyframe.toFloat()] = settings
    }

    override fun getCycleTrack(track: Int): CycleTrack_t =
        if (track in 0 until TRACK_MAX) mCycleTracks[track] else sortedMapOf()

    override fun getCycleTrackConst(track: Int): CycleTrack_t =
        if (track in 0 until TRACK_MAX) mCycleTracks[track] else sortedMapOf()

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

        /**
         * Build a day cycle from legacy filesystem folders containing sky and water presets.
         * Reads sky presets from <path>/skies/ and water presets from <path>/water/,
         * translates each to EEP format, and assembles them into a day-cycle LLSD map.
         */
        fun buildFromLegacyPreset(
            name: String,
            path: String,
            oldSettings: Map<String, Any>,
            messages: MutableMap<String, Any>
        ): LLSettingsDay? {
            // Read water preset from sibling "water" directory
            val waterMessages = mutableMapOf<String, Any>()
            val waterPath = File(path).parent?.let { File(it, "water").absolutePath } ?: path
            val waterSettings = LLSettingsVOWater.buildFromLegacyPresetFile(name, waterPath, waterMessages)
                ?: LLSettingsVOWater.buildDefaultWater()

            // Read sky preset from sibling "skies" directory
            val skyMessages = mutableMapOf<String, Any>()
            val skyPath = File(path).parent?.let { File(it, "skies").absolutePath } ?: path
            val skySettings = LLSettingsVOSky.buildFromLegacyPresetFile(name, skyPath, skyMessages)
                ?: LLSettingsVOSky.buildDefaultSky()

            // Assemble a single-keyframe day cycle
            val dayMap = LLSettingsDay.defaults().toMutableMap()
            dayMap[LLSettingsBase.SETTING_NAME] = name
            val results = LLSettingsDay.settingValidation(dayMap, LLSettingsDay.validationList())
            if (results["success"] != true) { messages["REASONS"] = "SettingValidationError: $name"; return null }

            return LLSettingsVODay(dayMap).also { day ->
                day.initialize()
                day.clearCycleTrack(TRACK_WATER)
                day.setSettingsAtKeyframe(waterSettings, 0.0, TRACK_WATER)
                day.clearCycleTrack(TRACK_GROUND_LEVEL)
                day.setSettingsAtKeyframe(skySettings, 0.0, TRACK_GROUND_LEVEL)
            }
        }

        fun buildFromLegacyPresetFile(name: String, path: String, messages: MutableMap<String, Any>): LLSettingsDay? {
            val legacyData = readLegacyPresetData(name, path, messages) ?: return null
            return buildFromLegacyPreset(uriUnescape(name), path, legacyData, messages)
        }

        /**
         * Translate a legacy region sky/water/daycycle message to an EEP day-cycle.
         * @param regionId  the region UUID (used for logging).
         * @param daycycle  list of [time, sky_name] pairs from the region message.
         * @param skys      map of sky_name → legacy sky LLSD.
         * @param water     legacy water LLSD map.
         */
        fun buildFromLegacyMessage(regionId: UUID, daycycle: List<Any>, skys: Map<String, Any>, water: Map<String, Any>): LLSettingsDay? {
            val dayMap = LLSettingsDay.defaults().toMutableMap()
            val results = LLSettingsDay.settingValidation(dayMap, LLSettingsDay.validationList())
            if (results["success"] != true) return null

            val day = LLSettingsVODay(dayMap).also { it.initialize() }

            // Translate and install the water track
            val waterEEP = LLSettingsWater.translateLegacySettings(water)
            val waterSettings = LLSettingsVOWater.buildWater(waterEEP)
            if (waterSettings != null) {
                day.clearCycleTrack(TRACK_WATER)
                day.setSettingsAtKeyframe(waterSettings, 0.0, TRACK_WATER)
            }

            // Translate each sky keyframe and install into TRACK_GROUND_LEVEL
            day.clearCycleTrack(TRACK_GROUND_LEVEL)
            for (entry in daycycle) {
                @Suppress("UNCHECKED_CAST")
                val pair = entry as? List<Any> ?: continue
                val keyframe = (pair.getOrNull(0) as? Number)?.toDouble() ?: continue
                val skyName  = pair.getOrNull(1) as? String ?: continue
                @Suppress("UNCHECKED_CAST")
                val skyData  = skys[skyName] as? Map<String, Any> ?: continue
                val skyEEP   = LLSettingsSky.translateLegacySettings(skyData)
                val skySettings = LLSettingsVOSky.buildSky(skyEEP) ?: continue
                skySettings.setName(skyName)
                day.setSettingsAtKeyframe(skySettings, keyframe, TRACK_GROUND_LEVEL)
            }

            return day
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

        /**
         * Convert a day cycle to a legacy LLSD array.
         * Returns a list of [time, sky_name] pairs plus a terminal [1.0, first_sky_name] entry.
         */
        fun convertToLegacy(pday: LLSettingsVODay): List<Any> {
            val result = mutableListOf<Any>()
            val skyTrack = pday.getCycleTrackConst(TRACK_GROUND_LEVEL)
            for ((keyframe, entry) in skyTrack) {
                result.add(listOf(keyframe, entry.getName()))
            }
            // Legacy format requires a terminal entry at time 1.0 pointing to the first sky
            val firstName = skyTrack.entries.firstOrNull()?.value?.getName() ?: ""
            if (firstName.isNotEmpty()) result.add(listOf(1.0f, firstName))
            return result
        }
    }
}

// ---- File utilities ----

/**
 * Locate and parse an XML preset file named <name>.xml in the given path directory.
 * Tries the raw name first, then a URI-escaped version, then one with dash escaping.
 * Returns the parsed Map on success, null on failure (populating messages with a reason).
 */
private fun readLegacyPresetData(name: String, path: String, messages: MutableMap<String, Any>): Map<String, Any>? {
    val candidates = listOf(
        File(path, "$name.xml"),
        File(path, "${uriEscape(name)}.xml"),
        File(path, "${uriEscape(name).replace("-", "%2D")}.xml")
    )
    for (file in candidates) {
        if (!file.exists()) continue
        return try {
            val text = file.readText(Charsets.UTF_8)
            SimpleLLSDParser.parseMap(text) ?: run {
                messages["REASONS"] = "SettingParseFileError: ${file.absolutePath}"
                null
            }
        } catch (e: Exception) {
            messages["REASONS"] = "SettingParseFileError: ${e.message}"
            null
        }
    }
    messages["REASONS"] = "SettingImportFileError: $name in $path"
    return null
}

/**
 * Percent-decode a URI-encoded string using java.net.URLDecoder.
 */
private fun uriUnescape(s: String): String =
    try { URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

/**
 * Percent-encode a string for use as a filename component (URI escaping).
 */
private fun uriEscape(s: String): String =
    java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

private fun ensureArray4(value: Any?, fill: Float): List<Any> {
    @Suppress("UNCHECKED_CAST")
    val list = (value as? List<Any>)?.toMutableList() ?: mutableListOf()
    while (list.size < 4) list.add(fill)
    return list
}

// ---- Minimal LLSD serializer / parser ----

/**
 * Minimal LLSD notation serialiser for Map<String, Any> (sufficient for settings export).
 * Produces a human-readable notation string compatible with LLSDNotationParser.
 */
private object SimpleLLSDSerializer {
    fun serialize(map: Map<String, Any>): String {
        val sb = StringBuilder()
        serializeMap(map, sb)
        return sb.toString()
    }

    private fun serializeMap(map: Map<String, Any>, sb: StringBuilder) {
        sb.append("{\n")
        map.entries.forEachIndexed { idx, (k, v) ->
            sb.append("  '").append(k).append("': ")
            serializeValue(v, sb)
            if (idx < map.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("}")
    }

    private fun serializeValue(v: Any, sb: StringBuilder) {
        when (v) {
            is Map<*, *> -> @Suppress("UNCHECKED_CAST") serializeMap(v as Map<String, Any>, sb)
            is List<*>   -> {
                sb.append("[")
                v.forEachIndexed { i, item -> if (i > 0) sb.append(", "); if (item != null) serializeValue(item, sb) else sb.append("undef") }
                sb.append("]")
            }
            is Float, is Double -> sb.append("r").append(v)
            is Int, is Long     -> sb.append("i").append(v)
            is Boolean          -> sb.append(if (v) "true" else "false")
            is String           -> sb.append("'").append(v.replace("'", "\\'")).append("'")
            is UUID             -> sb.append("u").append(v)
            else                -> sb.append("'").append(v.toString()).append("'")
        }
    }
}

/**
 * Minimal LLSD map parser that handles the notation produced by SimpleLLSDSerializer
 * as well as the XML subset used by legacy preset files.
 */
private object SimpleLLSDParser {
    /**
     * Parse either LLSD notation or LLSD XML into a Map<String, Any>.
     * Returns null on parse failure.
     */
    fun parseMap(input: String): Map<String, Any>? {
        val s = input.trim()
        return when {
            s.startsWith("{") -> parseNotationMap(s)
            s.startsWith("<") -> parseXMLMap(s)
            else -> null
        }
    }

    // ---- LLSD notation parser (same approach as LLSDNotationParser in LLMeshRepository) ----

    private fun parseNotationMap(s: String): Map<String, Any>? {
        if (!s.startsWith("{")) return null
        val result = mutableMapOf<String, Any>()
        val body = s.drop(1).dropLast(1).trim()
        var depth = 0
        val parts = mutableListOf<String>()
        val cur = StringBuilder()
        for (ch in body) {
            when (ch) {
                '{', '[' -> { depth++; cur.append(ch) }
                '}', ']' -> { depth--; cur.append(ch) }
                ',' -> if (depth == 0) { parts.add(cur.toString().trim()); cur.clear() } else cur.append(ch)
                else -> cur.append(ch)
            }
        }
        if (cur.isNotBlank()) parts.add(cur.toString().trim())
        for (part in parts) {
            val colon = part.indexOf(':')
            if (colon < 0) continue
            val key = part.substring(0, colon).trim().trim('\'', '"')
            val rawVal = part.substring(colon + 1).trim()
            result[key] = parseNotationValue(rawVal) ?: continue
        }
        return result
    }

    private fun parseNotationValue(raw: String): Any? = when {
        raw.startsWith("{") -> parseNotationMap(raw)
        raw.startsWith("[") -> parseNotationArray(raw)
        raw.startsWith("i") -> raw.drop(1).toIntOrNull() ?: 0
        raw.startsWith("r") -> raw.drop(1).toFloatOrNull() ?: 0f
        raw.startsWith("u") -> try { UUID.fromString(raw.drop(1)) } catch (_: Exception) { raw.drop(1) }
        raw == "true"        -> true
        raw == "false"       -> false
        raw.startsWith("'")  -> raw.drop(1).dropLast(1).replace("\\'", "'")
        else -> raw.toIntOrNull() ?: raw.toFloatOrNull() ?: raw.trim('\'', '"')
    }

    private fun parseNotationArray(s: String): List<Any> {
        val body = s.drop(1).dropLast(1).trim()
        val items = mutableListOf<Any>()
        var depth = 0
        val cur = StringBuilder()
        for (ch in body) {
            when (ch) {
                '{', '[' -> { depth++; cur.append(ch) }
                '}', ']' -> { depth--; cur.append(ch) }
                ',' -> if (depth == 0) { val v = parseNotationValue(cur.toString().trim()); if (v != null) items.add(v); cur.clear() } else cur.append(ch)
                else -> cur.append(ch)
            }
        }
        if (cur.isNotBlank()) { val v = parseNotationValue(cur.toString().trim()); if (v != null) items.add(v) }
        return items
    }

    // ---- Minimal LLSD XML parser ----

    private fun parseXMLMap(xml: String): Map<String, Any>? {
        val result = mutableMapOf<String, Any>()
        // Match <key>K</key> followed by a value element
        val keyRe = Regex("<key>(.*?)</key>\\s*<(map|array|string|integer|real|boolean|uuid|undef)(.*?)(?:>(.*?)</\\2>|/>)", RegexOption.DOT_MATCHES_ALL)
        for (mr in keyRe.findAll(xml)) {
            val key  = mr.groupValues[1]
            val type = mr.groupValues[2]
            val body = mr.groupValues[4]
            val value: Any = when (type) {
                "integer" -> body.toIntOrNull() ?: 0
                "real"    -> body.toFloatOrNull() ?: 0f
                "boolean" -> body !in listOf("0", "false", "")
                "uuid"    -> try { UUID.fromString(body) } catch (_: Exception) { body }
                "map"     -> parseXMLMap(body) ?: emptyMap<String, Any>()
                "array"   -> parseXMLArray(body)
                "undef"   -> ""
                else      -> body  // string
            }
            result[key] = value
        }
        return result.takeIf { it.isNotEmpty() }
    }

    private fun parseXMLArray(xml: String): List<Any> {
        val items = mutableListOf<Any>()
        val elemRe = Regex("<(string|integer|real|boolean|uuid)(.*?)(?:>(.*?)</\\1>|/>)", RegexOption.DOT_MATCHES_ALL)
        for (mr in elemRe.findAll(xml)) {
            val type = mr.groupValues[1]
            val body = mr.groupValues[3]
            val v: Any = when (type) {
                "integer" -> body.toIntOrNull() ?: 0
                "real"    -> body.toFloatOrNull() ?: 0f
                "boolean" -> body !in listOf("0", "false", "")
                "uuid"    -> try { UUID.fromString(body) } catch (_: Exception) { body }
                else      -> body
            }
            items.add(v)
        }
        return items
    }
}
