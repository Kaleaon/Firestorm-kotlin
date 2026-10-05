package com.firestorm.newview

import kotlin.math.*

abstract class SettingsWater : SettingsBase() {

    companion object {
        val DEFAULT_ASSET_ID = LLUUID("59d1a851-47e7-0e5f-1ed7-6b715154f41a")

        const val SETTING_BLUR_MULTIPLIER      = "blur_multiplier"
        const val SETTING_FOG_COLOR            = "water_fog_color"
        const val SETTING_FOG_DENSITY          = "water_fog_density"
        const val SETTING_FOG_MOD              = "underwater_fog_mod"
        const val SETTING_FRESNEL_OFFSET       = "fresnel_offset"
        const val SETTING_FRESNEL_SCALE        = "fresnel_scale"
        const val SETTING_TRANSPARENT_TEXTURE  = "transparent_texture"
        const val SETTING_NORMAL_MAP           = "normal_map"
        const val SETTING_NORMAL_SCALE         = "normal_scale"
        const val SETTING_SCALE_ABOVE          = "scale_above"
        const val SETTING_SCALE_BELOW          = "scale_below"
        const val SETTING_WAVE1_DIR            = "wave1_direction"
        const val SETTING_WAVE2_DIR            = "wave2_direction"

        private const val SETTING_LEGACY_BLUR_MULTIPLIER = "blurMultiplier"
        private const val SETTING_LEGACY_FOG_COLOR       = "waterFogColor"
        private const val SETTING_LEGACY_FOG_DENSITY     = "waterFogDensity"
        private const val SETTING_LEGACY_FOG_MOD         = "underWaterFogMod"
        private const val SETTING_LEGACY_FRESNEL_OFFSET  = "fresnelOffset"
        private const val SETTING_LEGACY_FRESNEL_SCALE   = "fresnelScale"
        private const val SETTING_LEGACY_NORMAL_MAP      = "normalMap"
        private const val SETTING_LEGACY_NORMAL_SCALE    = "normScale"
        private const val SETTING_LEGACY_SCALE_ABOVE     = "scaleAbove"
        private const val SETTING_LEGACY_SCALE_BELOW     = "scaleBelow"
        private const val SETTING_LEGACY_WAVE1_DIR       = "wave1Dir"
        private const val SETTING_LEGACY_WAVE2_DIR       = "wave2Dir"

        private val DEFAULT_TRANSPARENT_WATER_TEXTURE = LLUUID("2bfd3884-7e27-69b9-ba3a-3e673f680004")
        private val DEFAULT_OPAQUE_WATER_TEXTURE      = LLUUID("43c32285-d658-1793-c123-bf86315de055")
        private val DEFAULT_WATER_NORMAL              = LLUUID("822ded49-9a6c-f61c-cb89-6df54f42cdc4")

        fun getDefaultAssetId(): LLUUID                    = DEFAULT_ASSET_ID
        fun getDefaultWaterNormalAssetId(): LLUUID         = DEFAULT_WATER_NORMAL
        fun getDefaultTransparentTextureAssetId(): LLUUID  = DEFAULT_TRANSPARENT_WATER_TEXTURE
        fun getDefaultOpaqueTextureAssetId(): LLUUID       = DEFAULT_OPAQUE_WATER_TEXTURE

        fun defaults(position: Float = 0f): LLSD {
            val normalScaleOffset = (position * 0.5f) - 0.25f
            val ns = 2f + normalScaleOffset
            return mutableMapOf(
                SETTING_BLUR_MULTIPLIER     to 0.04f,
                SETTING_FOG_COLOR           to listOf(0.0156f, 0.1490f, 0.2509f),
                SETTING_FOG_DENSITY         to 2.0f,
                SETTING_FOG_MOD             to 0.25f,
                SETTING_FRESNEL_OFFSET      to 0.5f,
                SETTING_FRESNEL_SCALE       to 0.3999f,
                SETTING_TRANSPARENT_TEXTURE to DEFAULT_TRANSPARENT_WATER_TEXTURE.value,
                SETTING_NORMAL_MAP          to DEFAULT_WATER_NORMAL.value,
                SETTING_NORMAL_SCALE        to listOf(ns, ns, ns),
                SETTING_SCALE_ABOVE         to 0.0299f,
                SETTING_SCALE_BELOW         to 0.2000f,
                SETTING_WAVE1_DIR           to listOf(1.04999f, -0.42000f),
                SETTING_WAVE2_DIR           to listOf(1.10999f, -1.16000f),
                SettingsBase.SETTING_TYPE   to "water"
            )
        }

        fun translateLegacySettings(legacy: LLSD): LLSD? {
            var converted = false
            val news = defaults()
            fun tryReal(legacyKey: String, newKey: String) {
                if (legacy.containsKey(legacyKey)) { news[newKey] = (legacy[legacyKey] as? Number)?.toFloat(); converted = true }
            }
            fun tryList(legacyKey: String, newKey: String) {
                if (legacy.containsKey(legacyKey)) { news[newKey] = legacy[legacyKey]; converted = true }
            }
            fun tryUUID(legacyKey: String, newKey: String) {
                if (legacy.containsKey(legacyKey)) { news[newKey] = legacy[legacyKey]; converted = true }
            }
            tryReal(SETTING_LEGACY_BLUR_MULTIPLIER, SETTING_BLUR_MULTIPLIER)
            tryList(SETTING_LEGACY_FOG_COLOR,       SETTING_FOG_COLOR)
            tryReal(SETTING_LEGACY_FOG_DENSITY,     SETTING_FOG_DENSITY)
            tryReal(SETTING_LEGACY_FOG_MOD,         SETTING_FOG_MOD)
            tryReal(SETTING_LEGACY_FRESNEL_OFFSET,  SETTING_FRESNEL_OFFSET)
            tryReal(SETTING_LEGACY_FRESNEL_SCALE,   SETTING_FRESNEL_SCALE)
            tryUUID(SETTING_LEGACY_NORMAL_MAP,      SETTING_NORMAL_MAP)
            tryList(SETTING_LEGACY_NORMAL_SCALE,    SETTING_NORMAL_SCALE)
            tryReal(SETTING_LEGACY_SCALE_ABOVE,     SETTING_SCALE_ABOVE)
            tryReal(SETTING_LEGACY_SCALE_BELOW,     SETTING_SCALE_BELOW)
            tryList(SETTING_LEGACY_WAVE1_DIR,       SETTING_WAVE1_DIR)
            tryList(SETTING_LEGACY_WAVE2_DIR,       SETTING_WAVE2_DIR)
            return if (converted) news else null
        }

        fun validationList(): MutableList<Validator> = mutableListOf(
            Validator(SETTING_BLUR_MULTIPLIER,    true, "Real",  { v, f -> Validator.verifyFloatRange(v, f, listOf(-0.5f, 0.5f)) }),
            Validator(SETTING_FOG_COLOR,          true, "Array", { v, f -> Validator.verifyVectorMinMax(v, f, listOf(0f,0f,0f,1f), listOf(1f,1f,1f,1f)) }),
            Validator(SETTING_FOG_DENSITY,        true, "Real",  { v, f -> Validator.verifyFloatRange(v, f, listOf(0.001f, 100f)) }),
            Validator(SETTING_FOG_MOD,            true, "Real",  { v, f -> Validator.verifyFloatRange(v, f, listOf(0f, 20f)) }),
            Validator(SETTING_FRESNEL_OFFSET,     true, "Real",  { v, f -> Validator.verifyFloatRange(v, f, listOf(0f, 1f)) }),
            Validator(SETTING_FRESNEL_SCALE,      true, "Real",  { v, f -> Validator.verifyFloatRange(v, f, listOf(0f, 1f)) }),
            Validator(SETTING_NORMAL_MAP,         true, "UUID"),
            Validator(SETTING_NORMAL_SCALE,       true, "Array", { v, f -> Validator.verifyVectorMinMax(v, f, listOf(0f,0f,0f), listOf(10f,10f,10f)) }),
            Validator(SETTING_SCALE_ABOVE,        true, "Real",  { v, f -> Validator.verifyFloatRange(v, f, listOf(0f, 3f)) }),
            Validator(SETTING_SCALE_BELOW,        true, "Real",  { v, f -> Validator.verifyFloatRange(v, f, listOf(0f, 3f)) }),
            Validator(SETTING_WAVE1_DIR,          true, "Array", { v, f -> Validator.verifyVectorMinMax(v, f, listOf(-20f,-20f), listOf(20f,20f)) }),
            Validator(SETTING_WAVE2_DIR,          true, "Array", { v, f -> Validator.verifyVectorMinMax(v, f, listOf(-20f,-20f), listOf(20f,20f)) })
        )
    }

    var transparentTextureId: LLUUID     = LLUUID.NULL
    var normalMapId: LLUUID              = LLUUID.NULL
    var nextTransparentTextureId: LLUUID = LLUUID.NULL
    var nextNormalMapId: LLUUID          = LLUUID.NULL

    var blurMultiplier: Float   = 0.04f
    var waterFogColor: Color3   = Color3(0.0156f, 0.1490f, 0.2509f)
    var waterFogDensity: Float  = 2f
    var fogMod: Float           = 0.25f
    var fresnelOffset: Float    = 0.5f
    var fresnelScale: Float     = 0.3999f
    var normalScale: Vector3    = Vector3(2f, 2f, 2f)
    var scaleAbove: Float       = 0.0299f
    var scaleBelow: Float       = 0.2f
    var wave1Dir: Vector2       = Vector2(1.04999f, -0.42f)
    var wave2Dir: Vector2       = Vector2(1.10999f, -1.16f)

    abstract fun buildClone(): SettingsWater

    override fun getSettingsType(): String = "water"

    override fun getValidationList(): MutableList<Validator> = validationList()

    override fun replaceSettings(newSettings: LLSD) {
        super.replaceSettings(newSettings)
        nextNormalMapId          = LLUUID.NULL
        nextTransparentTextureId = LLUUID.NULL
    }

    override fun replaceSettings(other: SettingsBase) {
        super.replaceSettings(other)
        val o = other as? SettingsWater ?: return
        blurMultiplier    = o.blurMultiplier
        waterFogColor     = o.waterFogColor.copy()
        waterFogDensity   = o.waterFogDensity
        fogMod            = o.fogMod
        fresnelOffset     = o.fresnelOffset
        fresnelScale      = o.fresnelScale
        normalScale       = o.normalScale.copy()
        scaleAbove        = o.scaleAbove
        scaleBelow        = o.scaleBelow
        wave1Dir          = o.wave1Dir.copy()
        wave2Dir          = o.wave2Dir.copy()
        normalMapId       = o.normalMapId
        transparentTextureId = o.transparentTextureId
        nextNormalMapId          = LLUUID.NULL
        nextTransparentTextureId = LLUUID.NULL
    }

    fun replaceWithWater(other: SettingsWater) {
        replaceWith(other)
        nextNormalMapId          = other.nextNormalMapId
        nextTransparentTextureId = other.nextTransparentTextureId
    }

    override fun loadValuesFromLLSD() {
        super.loadValuesFromLLSD()
        val s = getSettings()
        fun f(k: String, d: Float = 0f) = (s[k] as? Number)?.toFloat() ?: d
        fun listColor3(k: String): Color3 {
            val l = s[k] as? List<*>; return if (l != null && l.size >= 3)
                Color3((l[0] as? Number)?.toFloat() ?: 0f, (l[1] as? Number)?.toFloat() ?: 0f, (l[2] as? Number)?.toFloat() ?: 0f)
            else Color3()
        }
        fun listVec3(k: String): Vector3 {
            val l = s[k] as? List<*>; return if (l != null && l.size >= 3)
                Vector3((l[0] as? Number)?.toFloat() ?: 0f, (l[1] as? Number)?.toFloat() ?: 0f, (l[2] as? Number)?.toFloat() ?: 0f)
            else Vector3()
        }
        fun listVec2(k: String): Vector2 {
            val l = s[k] as? List<*>; return if (l != null && l.size >= 2)
                Vector2((l[0] as? Number)?.toFloat() ?: 0f, (l[1] as? Number)?.toFloat() ?: 0f)
            else Vector2()
        }
        blurMultiplier    = f(SETTING_BLUR_MULTIPLIER, 0.04f)
        waterFogColor     = listColor3(SETTING_FOG_COLOR)
        waterFogDensity   = f(SETTING_FOG_DENSITY, 2f)
        fogMod            = f(SETTING_FOG_MOD, 0.25f)
        fresnelOffset     = f(SETTING_FRESNEL_OFFSET, 0.5f)
        fresnelScale      = f(SETTING_FRESNEL_SCALE, 0.3999f)
        normalScale       = listVec3(SETTING_NORMAL_SCALE)
        scaleAbove        = f(SETTING_SCALE_ABOVE, 0.0299f)
        scaleBelow        = f(SETTING_SCALE_BELOW, 0.2f)
        wave1Dir          = listVec2(SETTING_WAVE1_DIR)
        wave2Dir          = listVec2(SETTING_WAVE2_DIR)
        normalMapId       = (s[SETTING_NORMAL_MAP] as? String)?.let { LLUUID(it) } ?: LLUUID.NULL
        transparentTextureId = (s[SETTING_TRANSPARENT_TEXTURE] as? String)?.let { LLUUID(it) } ?: LLUUID.NULL
    }

    override fun saveValuesToLLSD() {
        super.saveValuesToLLSD()
        val s = getSettings()
        s[SETTING_BLUR_MULTIPLIER]    = blurMultiplier
        s[SETTING_FOG_COLOR]          = listOf(waterFogColor.r, waterFogColor.g, waterFogColor.b)
        s[SETTING_FOG_DENSITY]        = waterFogDensity
        s[SETTING_FOG_MOD]            = fogMod
        s[SETTING_FRESNEL_OFFSET]     = fresnelOffset
        s[SETTING_FRESNEL_SCALE]      = fresnelScale
        s[SETTING_NORMAL_SCALE]       = listOf(normalScale.x, normalScale.y, normalScale.z)
        s[SETTING_SCALE_ABOVE]        = scaleAbove
        s[SETTING_SCALE_BELOW]        = scaleBelow
        s[SETTING_WAVE1_DIR]          = listOf(wave1Dir.x, wave1Dir.y)
        s[SETTING_WAVE2_DIR]          = listOf(wave2Dir.x, wave2Dir.y)
        s[SETTING_NORMAL_MAP]         = normalMapId.value
        s[SETTING_TRANSPARENT_TEXTURE] = transparentTextureId.value
    }

    override fun blend(end: SettingsBase, blendf: Double) {
        val other = end as? SettingsWater ?: return
        val t = blendf.toFloat()
        settingFlags = settingFlags or other.settingFlags
        blurMultiplier  = lerpF(blurMultiplier, other.blurMultiplier, t)
        lerpColor(waterFogColor, other.waterFogColor, t)
        waterFogDensity = lerpF(waterFogDensity, other.waterFogDensity, t)
        fogMod          = lerpF(fogMod, other.fogMod, t)
        fresnelOffset   = lerpF(fresnelOffset, other.fresnelOffset, t)
        fresnelScale    = lerpF(fresnelScale, other.fresnelScale, t)
        lerpVector3(normalScale, other.normalScale, t)
        scaleAbove      = lerpF(scaleAbove, other.scaleAbove, t)
        scaleBelow      = lerpF(scaleBelow, other.scaleBelow, t)
        lerpVector2(wave1Dir, other.wave1Dir, t)
        lerpVector2(wave2Dir, other.wave2Dir, t)
        setDirtyFlag(true)
        setReplaced()
        setLLSDDirty()
        nextNormalMapId          = other.normalMapId
        nextTransparentTextureId = other.transparentTextureId
        setBlendFactor(blendf)
    }

    fun getModifiedWaterFogDensity(underwater: Boolean): Float {
        var fogDensity = waterFogDensity
        val underwaterFogMod = fogMod.coerceIn(0f, 10f)
        if (underwater && underwaterFogMod > 0f) {
            if (fogDensity < 0f && underwaterFogMod != underwaterFogMod.toLong().toFloat()) {
                fogDensity = 1f
            }
            fogDensity = fogDensity.toDouble().pow(underwaterFogMod.toDouble()).toFloat()
        }
        return fogDensity
    }

    fun getBlurMultiplier(): Float               = blurMultiplier
    fun setBlurMultiplier(v: Float)              { blurMultiplier = v;    setDirtyFlag(true); setLLSDDirty() }
    fun getWaterFogColor(): Color3               = waterFogColor
    fun setWaterFogColor(v: Color3)              { waterFogColor = v;     setDirtyFlag(true); setLLSDDirty() }
    fun getWaterFogDensity(): Float              = waterFogDensity
    fun setWaterFogDensity(v: Float)             { waterFogDensity = v;   setDirtyFlag(true); setLLSDDirty() }
    fun getFogMod(): Float                       = fogMod
    fun setFogMod(v: Float)                      { fogMod = v;            setDirtyFlag(true); setLLSDDirty() }
    fun getFresnelOffset(): Float                = fresnelOffset
    fun setFresnelOffset(v: Float)               { fresnelOffset = v;     setDirtyFlag(true); setLLSDDirty() }
    fun getFresnelScale(): Float                 = fresnelScale
    fun setFresnelScale(v: Float)                { fresnelScale = v;      setDirtyFlag(true); setLLSDDirty() }
    fun getTransparentTextureId(): LLUUID        = transparentTextureId
    fun setTransparentTextureId(v: LLUUID)       { transparentTextureId = v; setDirtyFlag(true); setLLSDDirty() }
    fun getNormalMapId(): LLUUID                 = normalMapId
    fun setNormalMapId(v: LLUUID)                { normalMapId = v;       setDirtyFlag(true); setLLSDDirty() }
    fun getNormalScale(): Vector3                = normalScale
    fun setNormalScale(v: Vector3)               { normalScale = v;       setDirtyFlag(true); setLLSDDirty() }
    fun getScaleAbove(): Float                   = scaleAbove
    fun setScaleAbove(v: Float)                  { scaleAbove = v;        setDirtyFlag(true); setLLSDDirty() }
    fun getScaleBelow(): Float                   = scaleBelow
    fun setScaleBelow(v: Float)                  { scaleBelow = v;        setDirtyFlag(true); setLLSDDirty() }
    fun getWave1Dir(): Vector2                   = wave1Dir.copy()
    fun setWave1Dir(v: Vector2)                  { wave1Dir = v.copy();   setDirtyFlag(true); setLLSDDirty() }
    fun getWave2Dir(): Vector2                   = wave2Dir.copy()
    fun setWave2Dir(v: Vector2)                  { wave2Dir = v.copy();   setDirtyFlag(true); setLLSDDirty() }
    fun getNextNormalMapId(): LLUUID             = nextNormalMapId
    fun getNextTransparentTextureId(): LLUUID    = nextTransparentTextureId

    override fun buildDerivedClone(): SettingsBase = buildClone()
}
