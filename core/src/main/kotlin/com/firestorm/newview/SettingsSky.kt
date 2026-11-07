package com.firestorm.newview

import kotlin.math.*

abstract class SettingsSky : SettingsBase() {

    companion object {
        const val EARTH_RADIUS: Float = 6.370e6f
        const val SUN_RADIUS: Float   = 695.508e6f
        const val SUN_DIST: Float     = 149598.260e6f
        const val MOON_RADIUS: Float  = 1.737e6f
        const val MOON_DIST: Float    = 384.400e6f

        const val DOME_OFFSET: Float = 0.96f
        const val DOME_RADIUS: Float = 15000f

        const val DEFAULT_AUTO_ADJUST_PROBE_AMBIANCE: Float = 1f
        var autoAdjustProbeAmbiance: Float = DEFAULT_AUTO_ADJUST_PROBE_AMBIANCE

        val DEFAULT_ASSET_ID = LLUUID("651510b8-5f4d-8991-1592-e7eeab2a5a06")

        const val SETTING_AMBIENT              = "ambient"
        const val SETTING_BLOOM_TEXTUREID      = "bloom_id"
        const val SETTING_RAINBOW_TEXTUREID    = "rainbow_id"
        const val SETTING_HALO_TEXTUREID       = "halo_id"
        const val SETTING_BLUE_DENSITY         = "blue_density"
        const val SETTING_BLUE_HORIZON         = "blue_horizon"
        const val SETTING_DENSITY_MULTIPLIER   = "density_multiplier"
        const val SETTING_DISTANCE_MULTIPLIER  = "distance_multiplier"
        const val SETTING_HAZE_DENSITY         = "haze_density"
        const val SETTING_HAZE_HORIZON         = "haze_horizon"
        const val SETTING_CLOUD_COLOR          = "cloud_color"
        const val SETTING_CLOUD_POS_DENSITY1   = "cloud_pos_density1"
        const val SETTING_CLOUD_POS_DENSITY2   = "cloud_pos_density2"
        const val SETTING_CLOUD_SCALE          = "cloud_scale"
        const val SETTING_CLOUD_SCROLL_RATE    = "cloud_scroll_rate"
        const val SETTING_CLOUD_SHADOW         = "cloud_shadow"
        const val SETTING_CLOUD_TEXTUREID      = "cloud_id"
        const val SETTING_CLOUD_VARIANCE       = "cloud_variance"
        const val SETTING_DOME_OFFSET          = "dome_offset"
        const val SETTING_DOME_RADIUS          = "dome_radius"
        const val SETTING_GAMMA                = "gamma"
        const val SETTING_GLOW                 = "glow"
        const val SETTING_LIGHT_NORMAL         = "lightnorm"
        const val SETTING_MAX_Y                = "max_y"
        const val SETTING_MOON_ROTATION        = "moon_rotation"
        const val SETTING_MOON_SCALE           = "moon_scale"
        const val SETTING_MOON_TEXTUREID       = "moon_id"
        const val SETTING_MOON_BRIGHTNESS      = "moon_brightness"
        const val SETTING_STAR_BRIGHTNESS      = "star_brightness"
        const val SETTING_SUNLIGHT_COLOR       = "sunlight_color"
        const val SETTING_SUN_ROTATION         = "sun_rotation"
        const val SETTING_SUN_SCALE            = "sun_scale"
        const val SETTING_SUN_TEXTUREID        = "sun_id"
        const val SETTING_PLANET_RADIUS        = "planet_radius"
        const val SETTING_SKY_BOTTOM_RADIUS    = "sky_bottom_radius"
        const val SETTING_SKY_TOP_RADIUS       = "sky_top_radius"
        const val SETTING_SUN_ARC_RADIANS      = "sun_arc_radians"
        const val SETTING_MIE_ANISOTROPY_FACTOR = "anisotropy"
        const val SETTING_RAYLEIGH_CONFIG      = "rayleigh_config"
        const val SETTING_MIE_CONFIG           = "mie_config"
        const val SETTING_ABSORPTION_CONFIG    = "absorption_config"
        const val KEY_DENSITY_PROFILE                         = "density"
        const val SETTING_DENSITY_PROFILE_WIDTH               = "width"
        const val SETTING_DENSITY_PROFILE_EXP_TERM            = "exp_term"
        const val SETTING_DENSITY_PROFILE_EXP_SCALE_FACTOR    = "exp_scale"
        const val SETTING_DENSITY_PROFILE_LINEAR_TERM         = "linear_term"
        const val SETTING_DENSITY_PROFILE_CONSTANT_TERM       = "constant_term"
        const val SETTING_SKY_MOISTURE_LEVEL   = "moisture_level"
        const val SETTING_SKY_DROPLET_RADIUS   = "droplet_radius"
        const val SETTING_SKY_ICE_LEVEL        = "ice_level"
        const val SETTING_REFLECTION_PROBE_AMBIANCE = "reflection_probe_ambiance"
        const val SETTING_LEGACY_HAZE          = "legacy_haze"
        const val SETTING_LEGACY_EAST_ANGLE    = "east_angle"
        const val SETTING_LEGACY_ENABLE_CLOUD_SCROLL = "enable_cloud_scroll"
        const val SETTING_LEGACY_SUN_ANGLE     = "sun_angle"

        private val DEFAULT_SUN_ID   = LLUUID("32bfbcea-24b1-fb9d-1ef9-48a28a63730f")
        private val DEFAULT_MOON_ID  = LLUUID("d07f6eed-b96a-47cd-b51d-400ad4a1c428")
        private val DEFAULT_CLOUD_ID = LLUUID("1dc1368f-e8fe-f02d-a08d-9d9f11c1af6b")
        private val IMG_BLOOM1       = LLUUID("3c59f7fe-9dc8-47f9-8aaf-a9dd1fbc3bef")
        private val IMG_RAINBOW      = LLUUID("11b4c57c-56b3-04ed-1f82-2004363882e4")
        private val IMG_HALO         = LLUUID("12149143-f599-91a7-77ac-b52a3c0f59cd")

        fun getDefaultAssetId(): LLUUID   = DEFAULT_ASSET_ID
        fun getDefaultSunTextureId(): LLUUID   = DEFAULT_SUN_ID
        fun getBlankSunTextureId(): LLUUID     = DEFAULT_SUN_ID
        fun getDefaultMoonTextureId(): LLUUID  = DEFAULT_MOON_ID
        fun getDefaultCloudNoiseTextureId(): LLUUID = DEFAULT_CLOUD_ID
        fun getDefaultBloomTextureId(): LLUUID = IMG_BLOOM1
        fun getDefaultRainbowTextureId(): LLUUID = IMG_RAINBOW
        fun getDefaultHaloTextureId(): LLUUID  = IMG_HALO

        fun defaults(position: Float = 0f): LLSD {
            return mutableMapOf(
                SETTING_AMBIENT            to listOf(0.25f, 0.25f, 0.25f, 1.0f),
                SETTING_BLUE_DENSITY       to listOf(0.2447f, 0.4487f, 0.7599f),
                SETTING_BLUE_HORIZON       to listOf(0.4954f, 0.4954f, 0.6399f),
                SETTING_DENSITY_MULTIPLIER to 0.0001801f,
                SETTING_DISTANCE_MULTIPLIER to 0.8f,
                SETTING_HAZE_DENSITY       to 0.7f,
                SETTING_HAZE_HORIZON       to 0.19f,
                SETTING_CLOUD_COLOR        to listOf(0.41f, 0.41f, 0.41f, 0.0f),
                SETTING_CLOUD_POS_DENSITY1 to listOf(1.0f, 0.5268f, 1.0f),
                SETTING_CLOUD_POS_DENSITY2 to listOf(1.0f, 0.5268f, 0.12f),
                SETTING_CLOUD_SCALE        to 0.42f,
                SETTING_CLOUD_SCROLL_RATE  to listOf(0.2f, 0.011f),
                SETTING_CLOUD_SHADOW       to 0.27f,
                SETTING_CLOUD_TEXTUREID    to DEFAULT_CLOUD_ID.value,
                SETTING_CLOUD_VARIANCE     to 0.0f,
                SETTING_DOME_OFFSET        to DOME_OFFSET,
                SETTING_DOME_RADIUS        to DOME_RADIUS,
                SETTING_GAMMA              to 1.0f,
                SETTING_GLOW               to listOf(5.0f, 0.001f, -0.48f),
                SETTING_MAX_Y              to 1605.0f,
                SETTING_MOON_ROTATION      to listOf(0f, 0f, 0f, 1f),
                SETTING_MOON_SCALE         to 1.0f,
                SETTING_MOON_TEXTUREID     to DEFAULT_MOON_ID.value,
                SETTING_MOON_BRIGHTNESS    to 0.5f,
                SETTING_STAR_BRIGHTNESS    to 0.0f,
                SETTING_SUNLIGHT_COLOR     to listOf(0.7342f, 0.7342f, 0.7342f, 1.0f),
                SETTING_SUN_ROTATION       to listOf(0f, 0f, 0f, 1f),
                SETTING_SUN_SCALE          to 1.0f,
                SETTING_SUN_TEXTUREID      to DEFAULT_SUN_ID.value,
                SETTING_PLANET_RADIUS      to EARTH_RADIUS,
                SETTING_SKY_BOTTOM_RADIUS  to (EARTH_RADIUS + 10000f),
                SETTING_SKY_TOP_RADIUS     to (EARTH_RADIUS + 60000f),
                SETTING_SUN_ARC_RADIANS    to 0.00045f,
                SETTING_SKY_MOISTURE_LEVEL to 0.0f,
                SETTING_SKY_DROPLET_RADIUS to 800.0f,
                SETTING_SKY_ICE_LEVEL      to 0.0f,
                SETTING_REFLECTION_PROBE_AMBIANCE to 0.0f,
                SETTING_TYPE               to "sky"
            )
        }

        fun createDensityProfileLayer(
            width: Float, exponentialTerm: Float, exponentialScaleFactor: Float,
            linearTerm: Float, constantTerm: Float, anisoFactor: Float = 0f
        ): LLSD = mutableMapOf(
            SETTING_DENSITY_PROFILE_WIDTH             to width,
            SETTING_DENSITY_PROFILE_EXP_TERM          to exponentialTerm,
            SETTING_DENSITY_PROFILE_EXP_SCALE_FACTOR  to exponentialScaleFactor,
            SETTING_DENSITY_PROFILE_LINEAR_TERM       to linearTerm,
            SETTING_DENSITY_PROFILE_CONSTANT_TERM     to constantTerm,
            SETTING_MIE_ANISOTROPY_FACTOR             to anisoFactor
        )

        fun createSingleLayerDensityProfile(
            width: Float, exponentialTerm: Float, exponentialScaleFactor: Float,
            linearTerm: Float, constantTerm: Float, anisoFactor: Float = 0f
        ): LLSD = mutableMapOf(
            KEY_DENSITY_PROFILE to listOf(
                createDensityProfileLayer(width, exponentialTerm, exponentialScaleFactor, linearTerm, constantTerm, anisoFactor)
            )
        )

        fun validationList(): MutableList<Validator> = mutableListOf(
            Validator(SETTING_AMBIENT, true, "Array", { v, f -> Validator.verifyVectorMinMax(v, f, listOf(0f, 0f, 0f, "*"), listOf(3f, 3f, 3f, "*")) }),
            Validator(SETTING_CLOUD_COLOR, true, "Array"),
            Validator(SETTING_CLOUD_POS_DENSITY1, true, "Array"),
            Validator(SETTING_CLOUD_POS_DENSITY2, true, "Array"),
            Validator(SETTING_CLOUD_SCALE, true, "Real", { v, f -> Validator.verifyFloatRange(v, f, listOf(0f, 2f)) }),
            Validator(SETTING_CLOUD_SCROLL_RATE, true, "Array"),
            Validator(SETTING_CLOUD_SHADOW, true, "Real", { v, f -> Validator.verifyFloatRange(v, f, listOf(0f, 1f)) }),
            Validator(SETTING_CLOUD_TEXTUREID, false, "UUID"),
            Validator(SETTING_CLOUD_VARIANCE, false, "Real", { v, f -> Validator.verifyFloatRange(v, f, listOf(0f, 1f)) }),
            Validator(SETTING_DOME_OFFSET, false, "Real"),
            Validator(SETTING_DOME_RADIUS, false, "Real"),
            Validator(SETTING_GAMMA, true, "Real"),
            Validator(SETTING_GLOW, true, "Array"),
            Validator(SETTING_MAX_Y, true, "Real"),
            Validator(SETTING_MOON_ROTATION, true, "Array"),
            Validator(SETTING_MOON_SCALE, false, "Real"),
            Validator(SETTING_MOON_TEXTUREID, false, "UUID"),
            Validator(SETTING_MOON_BRIGHTNESS, false, "Real", { v, f -> Validator.verifyFloatRange(v, f, listOf(0f, 1f)) }),
            Validator(SETTING_STAR_BRIGHTNESS, true, "Real"),
            Validator(SETTING_SUNLIGHT_COLOR, true, "Array"),
            Validator(SETTING_SUN_ROTATION, true, "Array"),
            Validator(SETTING_SUN_SCALE, false, "Real"),
            Validator(SETTING_SUN_TEXTUREID, false, "UUID"),
            Validator(SETTING_BLOOM_TEXTUREID, false, "UUID"),
            Validator(SETTING_PLANET_RADIUS, false, "Real"),
            Validator(SETTING_SKY_BOTTOM_RADIUS, false, "Real"),
            Validator(SETTING_SKY_TOP_RADIUS, false, "Real"),
            Validator(SETTING_SUN_ARC_RADIANS, false, "Real"),
            Validator(SETTING_RAYLEIGH_CONFIG, false, "Array"),
            Validator(SETTING_MIE_CONFIG, false, "Array"),
            Validator(SETTING_ABSORPTION_CONFIG, false, "Array"),
            Validator(SETTING_SKY_MOISTURE_LEVEL, false, "Real"),
            Validator(SETTING_SKY_DROPLET_RADIUS, false, "Real"),
            Validator(SETTING_SKY_ICE_LEVEL, false, "Real"),
            Validator(SETTING_REFLECTION_PROBE_AMBIANCE, false, "Real")
        )

        private fun convertAzimuthAltitudeToQuat(azimuth: Float, altitude: Float): Quaternion {
            val sinTheta = sin(azimuth); val cosTheta = cos(azimuth)
            val sinPhi = sin(altitude); val cosPhi = cos(altitude)
            val dx = cosTheta * cosPhi; val dy = sinTheta * cosPhi; val dz = sinPhi
            val axLen = sqrt(dz * dz + dy * dy)
            val axX = 0f; val axY = if (axLen > 1e-6f) -dz / axLen else 0f; val axZ = if (axLen > 1e-6f) dy / axLen else 1f
            val angle = acos(dx.coerceIn(-1f, 1f))
            val s = sin(angle / 2f)
            return Quaternion(axX * s, axY * s, axZ * s, cos(angle / 2f))
        }
    }

    var sunTextureId: LLUUID       = LLUUID.NULL
    var moonTextureId: LLUUID      = LLUUID.NULL
    var cloudTextureId: LLUUID     = LLUUID.NULL
    var bloomTextureId: LLUUID     = LLUUID.NULL
    var rainbowTextureId: LLUUID   = LLUUID.NULL
    var haloTextureId: LLUUID      = LLUUID.NULL
    var nextSunTextureId: LLUUID   = LLUUID.NULL
    var nextMoonTextureId: LLUUID  = LLUUID.NULL
    var nextCloudTextureId: LLUUID = LLUUID.NULL
    var nextBloomTextureId: LLUUID = LLUUID.NULL

    var canAutoAdjust: Boolean = false
    var sunRotation: Quaternion  = Quaternion()
    var moonRotation: Quaternion = Quaternion()
    var sunlightColor: Color3    = Color3()
    var glow: Color3             = Color3()
    var reflectionProbeAmbiance: Float = 0f
    var sunScale: Float          = 1f
    var starBrightness: Float    = 0f
    var moonBrightness: Float    = 0.5f
    var moonScale: Float         = 1f
    var maxY: Float              = 1605f
    var gamma: Float             = 1f
    var cloudVariance: Float     = 0f
    var cloudShadow: Float       = 0.27f
    var cloudScale: Float        = 0.42f
    var tonemapMix: Float        = 1f
    var hdrOffset: Float         = 0f
    var hdrMax: Float            = 8f
    var hdrMin: Float            = 0f
    var scrollRate: Vector2      = Vector2(0.2f, 0.011f)
    var cloudPosDensity1: Color3 = Color3(1f, 0.5268f, 1f)
    var cloudPosDensity2: Color3 = Color3(1f, 0.5268f, 0.12f)
    var cloudColor: Color3       = Color3(0.41f, 0.41f, 0.41f)
    var absorptionConfigs: Any?  = null
    var mieConfigs: Any?         = null
    var rayleighConfigs: Any?    = null
    var sunArcRadians: Float     = 0.00045f
    var skyTopRadius: Float      = EARTH_RADIUS + 60000f
    var skyBottomRadius: Float   = EARTH_RADIUS + 10000f
    var skyMoistureLevel: Float  = 0f
    var skyDropletRadius: Float  = 800f
    var skyIceLevel: Float       = 0f
    var planetRadius: Float      = EARTH_RADIUS

    var hazeHorizon: Float       = 0.19f
    var hazeDensity: Float       = 0.7f
    var distanceMultiplier: Float = 0.8f
    var densityMultiplier: Float  = 0.0001801f
    var blueHorizon: Color3      = Color3(0.4954f, 0.4954f, 0.6399f)
    var blueDensity: Color3      = Color3(0.2447f, 0.4487f, 0.7599f)
    var ambientColor: Color3     = Color3(0.25f, 0.25f, 0.25f)
    var hasLegacyHaze: Boolean   = false

    private var sunDirection: Vector3   = Vector3()
    private var moonDirection: Vector3  = Vector3()
    private var lightDirection: Vector3 = Vector3()
    private var moonAmbient: Color4 = Color4()
    private var moonDiffuse: Color3 = Color3()
    private var sunAmbient: Color4  = Color4()
    private var sunDiffuse: Color3  = Color3()
    private var totalAmbient: Color4 = Color4()
    private var hazeColor: Color4   = Color4()

    abstract fun buildClone(): SettingsSky

    override fun getSettingsType(): String = "sky"

    override fun getValidationList(): MutableList<Validator> = validationList()

    override fun getSlerpKeys(): Set<String> = setOf(SETTING_SUN_ROTATION, SETTING_MOON_ROTATION)

    override fun getSkipInterpolateKeys(): Set<String> =
        super.getSkipInterpolateKeys() + setOf(SETTING_LIGHT_NORMAL)

    override fun replaceSettings(newSettings: LLSD) {
        super.replaceSettings(newSettings)
        nextSunTextureId   = LLUUID.NULL
        nextMoonTextureId  = LLUUID.NULL
        nextCloudTextureId = LLUUID.NULL
        nextBloomTextureId = LLUUID.NULL
    }

    override fun replaceSettings(other: SettingsBase) {
        super.replaceSettings(other)
        val o = other as? SettingsSky ?: return
        canAutoAdjust           = o.canAutoAdjust
        reflectionProbeAmbiance = o.reflectionProbeAmbiance
        sunScale                = o.sunScale
        sunRotation             = o.sunRotation.copy()
        sunlightColor           = o.sunlightColor.copy()
        starBrightness          = o.starBrightness
        moonBrightness          = o.moonBrightness
        moonScale               = o.moonScale
        moonRotation            = o.moonRotation.copy()
        maxY                    = o.maxY
        glow                    = o.glow.copy()
        gamma                   = o.gamma
        cloudVariance           = o.cloudVariance
        cloudShadow             = o.cloudShadow
        scrollRate              = o.scrollRate.copy()
        cloudScale              = o.cloudScale
        cloudPosDensity1        = o.cloudPosDensity1.copy()
        cloudPosDensity2        = o.cloudPosDensity2.copy()
        cloudColor              = o.cloudColor.copy()
        absorptionConfigs       = o.absorptionConfigs
        mieConfigs              = o.mieConfigs
        rayleighConfigs         = o.rayleighConfigs
        sunArcRadians           = o.sunArcRadians
        skyTopRadius            = o.skyTopRadius
        skyBottomRadius         = o.skyBottomRadius
        skyMoistureLevel        = o.skyMoistureLevel
        skyDropletRadius        = o.skyDropletRadius
        skyIceLevel             = o.skyIceLevel
        planetRadius            = o.planetRadius
        hasLegacyHaze           = o.hasLegacyHaze
        distanceMultiplier      = o.distanceMultiplier
        densityMultiplier       = o.densityMultiplier
        hazeHorizon             = o.hazeHorizon
        hazeDensity             = o.hazeDensity
        blueHorizon             = o.blueHorizon.copy()
        blueDensity             = o.blueDensity.copy()
        ambientColor            = o.ambientColor.copy()
        sunTextureId            = o.sunTextureId
        moonTextureId           = o.moonTextureId
        cloudTextureId          = o.cloudTextureId
        bloomTextureId          = o.bloomTextureId
        nextSunTextureId        = LLUUID.NULL
        nextMoonTextureId       = LLUUID.NULL
        nextCloudTextureId      = LLUUID.NULL
        nextBloomTextureId      = LLUUID.NULL
    }

    override fun loadValuesFromLLSD() {
        super.loadValuesFromLLSD()
        val s = getSettings()
        fun floatOf(k: String, d: Float = 0f) = (s[k] as? Number)?.toFloat() ?: d
        fun listColor3(k: String): Color3 {
            val l = s[k] as? List<*>; return if (l != null && l.size >= 3)
                Color3((l[0] as? Number)?.toFloat() ?: 0f, (l[1] as? Number)?.toFloat() ?: 0f, (l[2] as? Number)?.toFloat() ?: 0f)
            else Color3()
        }
        fun listQuat(k: String): Quaternion {
            val l = s[k] as? List<*>; return if (l != null && l.size >= 4)
                Quaternion((l[0] as? Number)?.toFloat() ?: 0f, (l[1] as? Number)?.toFloat() ?: 0f,
                           (l[2] as? Number)?.toFloat() ?: 0f, (l[3] as? Number)?.toFloat() ?: 1f)
            else Quaternion()
        }
        gamma             = floatOf(SETTING_GAMMA, 1f)
        maxY              = floatOf(SETTING_MAX_Y, 1605f)
        starBrightness    = floatOf(SETTING_STAR_BRIGHTNESS)
        moonBrightness    = floatOf(SETTING_MOON_BRIGHTNESS, 0.5f)
        moonScale         = floatOf(SETTING_MOON_SCALE, 1f)
        sunScale          = floatOf(SETTING_SUN_SCALE, 1f)
        cloudScale        = floatOf(SETTING_CLOUD_SCALE, 0.42f)
        cloudShadow       = floatOf(SETTING_CLOUD_SHADOW, 0.27f)
        cloudVariance     = floatOf(SETTING_CLOUD_VARIANCE)
        reflectionProbeAmbiance = floatOf(SETTING_REFLECTION_PROBE_AMBIANCE)
        planetRadius      = floatOf(SETTING_PLANET_RADIUS, EARTH_RADIUS)
        skyBottomRadius   = floatOf(SETTING_SKY_BOTTOM_RADIUS, EARTH_RADIUS + 10000f)
        skyTopRadius      = floatOf(SETTING_SKY_TOP_RADIUS, EARTH_RADIUS + 60000f)
        sunArcRadians     = floatOf(SETTING_SUN_ARC_RADIANS, 0.00045f)
        skyMoistureLevel  = floatOf(SETTING_SKY_MOISTURE_LEVEL)
        skyDropletRadius  = floatOf(SETTING_SKY_DROPLET_RADIUS, 800f)
        skyIceLevel       = floatOf(SETTING_SKY_ICE_LEVEL)
        hazeDensity       = floatOf(SETTING_HAZE_DENSITY, 0.7f)
        hazeHorizon       = floatOf(SETTING_HAZE_HORIZON, 0.19f)
        densityMultiplier = floatOf(SETTING_DENSITY_MULTIPLIER, 0.0001801f)
        distanceMultiplier = floatOf(SETTING_DISTANCE_MULTIPLIER, 0.8f)
        sunRotation       = listQuat(SETTING_SUN_ROTATION)
        moonRotation      = listQuat(SETTING_MOON_ROTATION)
        glow              = listColor3(SETTING_GLOW)
        sunlightColor     = listColor3(SETTING_SUNLIGHT_COLOR)
        ambientColor      = listColor3(SETTING_AMBIENT)
        blueDensity       = listColor3(SETTING_BLUE_DENSITY)
        blueHorizon       = listColor3(SETTING_BLUE_HORIZON)
        cloudColor        = listColor3(SETTING_CLOUD_COLOR)
        cloudPosDensity1  = listColor3(SETTING_CLOUD_POS_DENSITY1)
        cloudPosDensity2  = listColor3(SETTING_CLOUD_POS_DENSITY2)
        sunTextureId      = (s[SETTING_SUN_TEXTUREID] as? String)?.let { LLUUID(it) } ?: LLUUID.NULL
        moonTextureId     = (s[SETTING_MOON_TEXTUREID] as? String)?.let { LLUUID(it) } ?: LLUUID.NULL
        cloudTextureId    = (s[SETTING_CLOUD_TEXTUREID] as? String)?.let { LLUUID(it) } ?: LLUUID.NULL
        bloomTextureId    = (s[SETTING_BLOOM_TEXTUREID] as? String)?.let { LLUUID(it) } ?: LLUUID.NULL
        val scrollList    = s[SETTING_CLOUD_SCROLL_RATE] as? List<*>
        if (scrollList != null && scrollList.size >= 2)
            scrollRate    = Vector2((scrollList[0] as? Number)?.toFloat() ?: 0.2f, (scrollList[1] as? Number)?.toFloat() ?: 0.011f)
        rayleighConfigs   = s[SETTING_RAYLEIGH_CONFIG]
        mieConfigs        = s[SETTING_MIE_CONFIG]
        absorptionConfigs = s[SETTING_ABSORPTION_CONFIG]
        hasLegacyHaze     = s.containsKey(SETTING_LEGACY_HAZE)
        canAutoAdjust     = !hasLegacyHaze && reflectionProbeAmbiance == 0f
    }

    override fun saveValuesToLLSD() {
        super.saveValuesToLLSD()
        val s = getSettings()
        s[SETTING_GAMMA]                = gamma
        s[SETTING_MAX_Y]                = maxY
        s[SETTING_STAR_BRIGHTNESS]      = starBrightness
        s[SETTING_MOON_BRIGHTNESS]      = moonBrightness
        s[SETTING_MOON_SCALE]           = moonScale
        s[SETTING_SUN_SCALE]            = sunScale
        s[SETTING_CLOUD_SCALE]          = cloudScale
        s[SETTING_CLOUD_SHADOW]         = cloudShadow
        s[SETTING_CLOUD_VARIANCE]       = cloudVariance
        s[SETTING_REFLECTION_PROBE_AMBIANCE] = reflectionProbeAmbiance
        s[SETTING_PLANET_RADIUS]        = planetRadius
        s[SETTING_SKY_BOTTOM_RADIUS]    = skyBottomRadius
        s[SETTING_SKY_TOP_RADIUS]       = skyTopRadius
        s[SETTING_SUN_ARC_RADIANS]      = sunArcRadians
        s[SETTING_HAZE_DENSITY]         = hazeDensity
        s[SETTING_HAZE_HORIZON]         = hazeHorizon
        s[SETTING_DENSITY_MULTIPLIER]   = densityMultiplier
        s[SETTING_DISTANCE_MULTIPLIER]  = distanceMultiplier
        s[SETTING_SUN_ROTATION]         = listOf(sunRotation.x, sunRotation.y, sunRotation.z, sunRotation.w)
        s[SETTING_MOON_ROTATION]        = listOf(moonRotation.x, moonRotation.y, moonRotation.z, moonRotation.w)
        s[SETTING_GLOW]                 = listOf(glow.r, glow.g, glow.b)
        s[SETTING_SUNLIGHT_COLOR]       = listOf(sunlightColor.r, sunlightColor.g, sunlightColor.b)
        s[SETTING_AMBIENT]              = listOf(ambientColor.r, ambientColor.g, ambientColor.b)
        s[SETTING_BLUE_DENSITY]         = listOf(blueDensity.r, blueDensity.g, blueDensity.b)
        s[SETTING_BLUE_HORIZON]         = listOf(blueHorizon.r, blueHorizon.g, blueHorizon.b)
        s[SETTING_CLOUD_COLOR]          = listOf(cloudColor.r, cloudColor.g, cloudColor.b)
        s[SETTING_CLOUD_POS_DENSITY1]   = listOf(cloudPosDensity1.r, cloudPosDensity1.g, cloudPosDensity1.b)
        s[SETTING_CLOUD_POS_DENSITY2]   = listOf(cloudPosDensity2.r, cloudPosDensity2.g, cloudPosDensity2.b)
        s[SETTING_CLOUD_SCROLL_RATE]    = listOf(scrollRate.x, scrollRate.y)
        s[SETTING_SUN_TEXTUREID]        = sunTextureId.value
        s[SETTING_MOON_TEXTUREID]       = moonTextureId.value
        s[SETTING_CLOUD_TEXTUREID]      = cloudTextureId.value
        s[SETTING_BLOOM_TEXTUREID]      = bloomTextureId.value
        rayleighConfigs?.let  { s[SETTING_RAYLEIGH_CONFIG]   = it }
        mieConfigs?.let       { s[SETTING_MIE_CONFIG]        = it }
        absorptionConfigs?.let { s[SETTING_ABSORPTION_CONFIG] = it }
    }

    override fun blend(end: SettingsBase, blendf: Double) {
        val other = end as? SettingsSky ?: return
        val t = blendf.toFloat()
        settingFlags = settingFlags or other.settingFlags
        lerpColor(ambientColor, other.ambientColor, t)
        lerpColor(sunlightColor, other.sunlightColor, t)
        lerpColor(blueDensity, other.blueDensity, t)
        lerpColor(blueHorizon, other.blueHorizon, t)
        lerpColor(cloudColor, other.cloudColor, t)
        lerpColor(cloudPosDensity1, other.cloudPosDensity1, t)
        lerpColor(cloudPosDensity2, other.cloudPosDensity2, t)
        lerpColor(glow, other.glow, t)
        gamma            = lerpF(gamma, other.gamma, t)
        maxY             = lerpF(maxY, other.maxY, t)
        starBrightness   = lerpF(starBrightness, other.starBrightness, t)
        moonBrightness   = lerpF(moonBrightness, other.moonBrightness, t)
        cloudScale       = lerpF(cloudScale, other.cloudScale, t)
        cloudShadow      = lerpF(cloudShadow, other.cloudShadow, t)
        cloudVariance    = lerpF(cloudVariance, other.cloudVariance, t)
        hazeDensity      = lerpF(hazeDensity, other.hazeDensity, t)
        hazeHorizon      = lerpF(hazeHorizon, other.hazeHorizon, t)
        densityMultiplier = lerpF(densityMultiplier, other.densityMultiplier, t)
        distanceMultiplier = lerpF(distanceMultiplier, other.distanceMultiplier, t)
        lerpVector2(scrollRate, other.scrollRate, t)
        sunTextureId    = other.sunTextureId
        moonTextureId   = other.moonTextureId
        cloudTextureId  = other.cloudTextureId
        bloomTextureId  = other.bloomTextureId
        nextSunTextureId   = other.sunTextureId
        nextMoonTextureId  = other.moonTextureId
        nextCloudTextureId = other.cloudTextureId
        nextBloomTextureId = other.bloomTextureId
        setDirtyFlag(true)
        setReplaced()
        setLLSDDirty()
        setBlendFactor(blendf)
    }

    override fun updateSettings() {
        super.updateSettings()
        calculateHeavenlyBodyPositions()
        calculateLightSettings()
    }

    private fun calculateHeavenlyBodyPositions() {
        // no-op
    }

    private fun calculateLightSettings() {
        // no-op
    }

    fun getPlanetRadius(): Float           = planetRadius
    fun getSkyBottomRadius(): Float        = skyBottomRadius
    fun getSkyTopRadius(): Float           = skyTopRadius
    fun getSunArcRadians(): Float          = sunArcRadians
    fun getMieAnisotropy(): Float          = getSettings()[SETTING_MIE_ANISOTROPY_FACTOR].let { (it as? Number)?.toFloat() ?: 0.8f }
    fun getSkyMoistureLevel(): Float       = skyMoistureLevel
    fun getSkyDropletRadius(): Float       = skyDropletRadius
    fun getSkyIceLevel(): Float            = skyIceLevel
    fun canAutoAdjust(): Boolean           = canAutoAdjust
    fun getReflectionProbeAmbiance(autoAdjust: Boolean = false): Float =
        if (autoAdjust && canAutoAdjust) autoAdjustProbeAmbiance else reflectionProbeAmbiance

    fun getBloomTextureId(): LLUUID    = bloomTextureId
    fun getRainbowTextureId(): LLUUID  = rainbowTextureId
    fun getHaloTextureId(): LLUUID     = haloTextureId
    fun getNextSunTextureId(): LLUUID  = nextSunTextureId
    fun getNextMoonTextureId(): LLUUID = nextMoonTextureId
    fun getNextCloudNoiseTextureId(): LLUUID = nextCloudTextureId
    fun getNextBloomTextureId(): LLUUID = nextBloomTextureId

    fun getRayleighConfig(): LLSD?   = (rayleighConfigs as? List<*>)?.firstOrNull() as? LLSD
    fun getMieConfig(): LLSD?        = (mieConfigs as? List<*>)?.firstOrNull() as? LLSD
    fun getAbsorptionConfig(): LLSD? = (absorptionConfigs as? List<*>)?.firstOrNull() as? LLSD
    fun getRayleighConfigs(): Any?   = rayleighConfigs
    fun getMieConfigs(): Any?        = mieConfigs
    fun getAbsorptionConfigs(): Any? = absorptionConfigs

    fun setRayleighConfigs(c: Any?) { rayleighConfigs = c; setDirtyFlag(true); setLLSDDirty() }
    fun setMieConfigs(c: Any?)      { mieConfigs = c;      setDirtyFlag(true); setLLSDDirty() }
    fun setAbsorptionConfigs(c: Any?) { absorptionConfigs = c; setDirtyFlag(true); setLLSDDirty() }

    fun setPlanetRadius(r: Float)       { planetRadius = r;       setDirtyFlag(true); setLLSDDirty() }
    fun setSkyBottomRadius(r: Float)    { skyBottomRadius = r;    setDirtyFlag(true); setLLSDDirty() }
    fun setSkyTopRadius(r: Float)       { skyTopRadius = r;       setDirtyFlag(true); setLLSDDirty() }
    fun setSunArcRadians(r: Float)      { sunArcRadians = r;      setDirtyFlag(true); setLLSDDirty() }
    fun setMieAnisotropy(f: Float)      { getSettings()[SETTING_MIE_ANISOTROPY_FACTOR] = f; setDirtyFlag(true) }
    fun setSkyMoistureLevel(v: Float)   { skyMoistureLevel = v;   setDirtyFlag(true); setLLSDDirty() }
    fun setSkyDropletRadius(v: Float)   { skyDropletRadius = v;   setDirtyFlag(true); setLLSDDirty() }
    fun setSkyIceLevel(v: Float)        { skyIceLevel = v;        setDirtyFlag(true); setLLSDDirty() }
    fun setReflectionProbeAmbiance(v: Float) { reflectionProbeAmbiance = v; setDirtyFlag(true); setLLSDDirty() }

    fun getAmbientColor(): Color3           = ambientColor
    fun setAmbientColor(v: Color3)          { ambientColor = v;   setDirtyFlag(true); setLLSDDirty() }
    fun getCloudColor(): Color3             = cloudColor
    fun setCloudColor(v: Color3)            { cloudColor = v;     setDirtyFlag(true); setLLSDDirty() }
    fun getCloudNoiseTextureId(): LLUUID    = cloudTextureId
    fun setCloudNoiseTextureId(id: LLUUID)  { cloudTextureId = id; setDirtyFlag(true); setLLSDDirty() }
    fun getCloudPosDensity1(): Color3       = cloudPosDensity1
    fun setCloudPosDensity1(v: Color3)      { cloudPosDensity1 = v; setDirtyFlag(true); setLLSDDirty() }
    fun getCloudPosDensity2(): Color3       = cloudPosDensity2
    fun setCloudPosDensity2(v: Color3)      { cloudPosDensity2 = v; setDirtyFlag(true); setLLSDDirty() }
    fun getCloudScale(): Float              = cloudScale
    fun setCloudScale(v: Float)             { cloudScale = v;     setDirtyFlag(true); setLLSDDirty() }
    fun getCloudScrollRate(): Vector2       = scrollRate.copy()
    fun setCloudScrollRate(v: Vector2)      { scrollRate = v.copy(); setDirtyFlag(true); setLLSDDirty() }
    fun setCloudScrollRateX(v: Float)       { scrollRate.x = v;   setDirtyFlag(true); setLLSDDirty() }
    fun setCloudScrollRateY(v: Float)       { scrollRate.y = v;   setDirtyFlag(true); setLLSDDirty() }
    fun getCloudShadow(): Float             = cloudShadow
    fun setCloudShadow(v: Float)            { cloudShadow = v;    setDirtyFlag(true); setLLSDDirty() }
    fun getCloudVariance(): Float           = cloudVariance
    fun setCloudVariance(v: Float)          { cloudVariance = v;  setDirtyFlag(true); setLLSDDirty() }
    fun getDomeOffset(): Float              = DOME_OFFSET
    fun getDomeRadius(): Float              = DOME_RADIUS
    fun getGamma(): Float                   = gamma
    fun setGamma(v: Float)                  { gamma = v;          setDirtyFlag(true); setLLSDDirty() }
    fun getHDRMin(autoAdjust: Boolean = false): Float = hdrMin
    fun getHDRMax(autoAdjust: Boolean = false): Float = hdrMax
    fun getHDROffset(autoAdjust: Boolean = false): Float = hdrOffset
    fun getTonemapMix(autoAdjust: Boolean = false): Float = tonemapMix
    fun setTonemapMix(v: Float)             { tonemapMix = v;     setDirtyFlag(true); setLLSDDirty() }
    fun getGlow(): Color3                   = glow
    fun setGlow(v: Color3)                  { glow = v;           setDirtyFlag(true); setLLSDDirty() }
    fun getMaxY(): Float                    = maxY
    fun setMaxY(v: Float)                   { maxY = v;           setDirtyFlag(true); setLLSDDirty() }
    fun getMoonRotation(): Quaternion       = moonRotation.copy()
    fun setMoonRotation(v: Quaternion)      { moonRotation = v;   setDirtyFlag(true); setLLSDDirty() }
    fun getMoonScale(): Float               = moonScale
    fun setMoonScale(v: Float)              { moonScale = v;      setDirtyFlag(true); setLLSDDirty() }
    fun getMoonTextureId(): LLUUID          = moonTextureId
    fun setMoonTextureId(id: LLUUID)        { moonTextureId = id; setDirtyFlag(true); setLLSDDirty() }
    fun getMoonBrightness(): Float          = moonBrightness
    fun setMoonBrightness(v: Float)         { moonBrightness = v; setDirtyFlag(true); setLLSDDirty() }
    fun getStarBrightness(): Float          = starBrightness
    fun setStarBrightness(v: Float)         { starBrightness = v; setDirtyFlag(true); setLLSDDirty() }
    fun getSunlightColor(): Color3          = sunlightColor
    fun setSunlightColor(v: Color3)         { sunlightColor = v;  setDirtyFlag(true); setLLSDDirty() }
    fun getSunRotation(): Quaternion        = sunRotation.copy()
    fun setSunRotation(v: Quaternion)       { sunRotation = v;    setDirtyFlag(true); setLLSDDirty() }
    fun getSunScale(): Float                = sunScale
    fun setSunScale(v: Float)               { sunScale = v;       setDirtyFlag(true); setLLSDDirty() }
    fun getSunTextureId(): LLUUID           = sunTextureId
    fun setSunTextureId(id: LLUUID)         { sunTextureId = id;  setDirtyFlag(true); setLLSDDirty() }

    fun getBlueDensity(): Color3            = blueDensity
    fun setBlueDensity(v: Color3)           { blueDensity = v;    setDirtyFlag(true); setLLSDDirty() }
    fun getBlueHorizon(): Color3            = blueHorizon
    fun setBlueHorizon(v: Color3)           { blueHorizon = v;    setDirtyFlag(true); setLLSDDirty() }
    fun getDensityMultiplier(): Float       = densityMultiplier
    fun setDensityMultiplier(v: Float)      { densityMultiplier = v; setDirtyFlag(true); setLLSDDirty() }
    fun getDistanceMultiplier(): Float      = distanceMultiplier
    fun setDistanceMultiplier(v: Float)     { distanceMultiplier = v; setDirtyFlag(true); setLLSDDirty() }
    fun getHazeDensity(): Float             = hazeDensity
    fun setHazeDensity(v: Float)            { hazeDensity = v;    setDirtyFlag(true); setLLSDDirty() }
    fun getHazeHorizon(): Float             = hazeHorizon
    fun setHazeHorizon(v: Float)            { hazeHorizon = v;    setDirtyFlag(true); setLLSDDirty() }

    fun getIsSunUp(): Boolean  = false
    fun getIsMoonUp(): Boolean = false

    fun getSunMoonGlowFactor(): Float  = 0f
    fun getLightDirection(): Vector3   = Vector3()
    fun getLightDiffuse(): Color3      = Color3()
    fun getSunDirection(): Vector3     = Vector3()
    fun getMoonDirection(): Vector3    = Vector3()
    fun getMoonlightColor(): Color3    = Color3()
    fun getMoonAmbient(): Color4       = Color4()
    fun getMoonDiffuse(): Color3       = Color3()
    fun getSunAmbient(): Color4        = Color4()
    fun getSunDiffuse(): Color3        = Color3()
    fun getTotalAmbient(): Color4      = Color4()
    fun getHazeColor(): Color4         = Color4()
    fun getSunlightColorClamped(): Color3 = Color3()
    fun getAmbientColorClamped(): Color3  = Color3()

    fun getLightAttenuation(distance: Float): Color3  = Color3()
    fun getLightTransmittance(distance: Float): Color3 = Color3()
    fun getTotalDensity(): Color3 = Color3()
    fun gammaCorrect(input: Color3, gamma: Float): Color3 = Color3()

    fun getLightTransmittanceFast(totalDensity: Color3, densityMul: Float, distance: Float): Color3 = Color3()

    override fun buildDerivedClone(): SettingsBase = buildClone()
}
