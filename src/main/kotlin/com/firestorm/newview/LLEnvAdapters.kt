package com.firestorm.newview

import kotlin.math.ln
import kotlin.math.pow

class WLColorControl(
    color: FloatArray,
    private val mName: String,
    private val mSliderName: String = ""
) {
    private val mColor: FloatArray = color.copyOf(4)

    val hasSliderName: Boolean = mSliderName.isNotEmpty()

    val isSunOrAmbientColor: Boolean = mSliderName == "WLSunlight" || mSliderName == "WLAmbient"

    val isBlueHorizonOrDensity: Boolean = mSliderName == "WLBlueHorizon" || mSliderName == "WLBlueDensity"

    fun setColor4(r: Float, g: Float, b: Float, a: Float) {
        mColor[0] = r; mColor[1] = g; mColor[2] = b; mColor[3] = a
    }

    fun setColor3(r: Float, g: Float, b: Float) {
        mColor[0] = r; mColor[1] = g; mColor[2] = b
    }

    fun getColor4(): FloatArray = mColor.copyOf(4)

    fun getColor3(): FloatArray = mColor.copyOf(3)

    fun update(setting: LLSettingsBase) {
        setting.setValue(mName, mColor.copyOf(4))
    }

    fun getSliderName(): String = mSliderName

    fun getRed(): Float = mColor[0]
    fun getGreen(): Float = mColor[1]
    fun getBlue(): Float = mColor[2]
    fun getIntensity(): Float = mColor[3]

    fun setRed(red: Float) { mColor[0] = red }
    fun setGreen(green: Float) { mColor[1] = green }
    fun setBlue(blue: Float) { mColor[2] = blue }
    fun setIntensity(intensity: Float) { mColor[3] = intensity }
}

class WLFloatControl(
    private var x: Float,
    private val mName: String,
    private val mult: Float = 1.0f
) {
    fun getValue(): Float = x

    fun setValue(value: Float) { x = value }

    fun getMult(): Float = mult

    fun update(setting: LLSettingsBase) {
        setting.setValue(mName, x)
    }
}

class WLXFloatControl(
    private var mExp: Float,
    private val mName: String,
    private var mBase: Float
) {
    fun assign(value: Float) {
        mExp = (ln(value.toDouble()) / ln(mBase.toDouble())).toFloat()
    }

    fun toFloat(): Float = mBase.pow(mExp)

    fun update(setting: LLSettingsBase) {
        setting.setValue(mName, mBase.pow(mExp))
    }

    fun getExp(): Float = mExp
    fun setExp(value: Float) { mExp = value }

    fun getBase(): Float = mBase
    fun setBase(value: Float) { mBase = value }
}

class WLVect2Control(
    u: Float,
    v: Float,
    private val mName: String
) {
    private var mU: Float = u
    private var mV: Float = v

    fun assign(u: Float, v: Float) { mU = u; mV = v }

    fun update(setting: LLSettingsBase) {
        setting.setValue(mName, floatArrayOf(mU, mV))
    }

    fun getU(): Float = mU
    fun setU(value: Float) { mU = value }

    fun getV(): Float = mV
    fun setV(value: Float) { mV = value }
}

class WLVect3Control(
    x: Float,
    y: Float,
    z: Float,
    private val mName: String
) {
    private var mX: Float = x
    private var mY: Float = y
    private var mZ: Float = z

    fun assign(x: Float, y: Float, z: Float) { mX = x; mY = y; mZ = z }

    fun update(setting: LLSettingsBase) {
        setting.setValue(mName, floatArrayOf(mX, mY, mZ))
    }

    fun getX(): Float = mX
    fun setX(value: Float) { mX = value }

    fun getY(): Float = mY
    fun setY(value: Float) { mY = value }

    fun getZ(): Float = mZ
    fun setZ(value: Float) { mZ = value }
}

open class LLDensityProfileSettingsAdapter(
    protected val mConfig: String,
    protected val mLayerIndex: Int = 0
) {
    protected val mLayerWidth = WLFloatControl(1.0f, LLSettingsSky.SETTING_DENSITY_PROFILE_WIDTH)
    protected val mExpTerm = WLFloatControl(1.0f, LLSettingsSky.SETTING_DENSITY_PROFILE_EXP_TERM)
    protected val mExpScale = WLFloatControl(1.0f, LLSettingsSky.SETTING_DENSITY_PROFILE_EXP_SCALE_FACTOR)
    protected val mLinTerm = WLFloatControl(1.0f, LLSettingsSky.SETTING_DENSITY_PROFILE_LINEAR_TERM)
    protected val mConstantTerm = WLFloatControl(1.0f, LLSettingsSky.SETTING_DENSITY_PROFILE_CONSTANT_TERM)
}

class LLRayleighDensityProfileSettingsAdapter(layerIndex: Int = 0) :
    LLDensityProfileSettingsAdapter(LLSettingsSky.SETTING_RAYLEIGH_CONFIG, layerIndex)

class LLMieDensityProfileSettingsAdapter(layerIndex: Int = 0) :
    LLDensityProfileSettingsAdapter(LLSettingsSky.SETTING_MIE_CONFIG, layerIndex) {
    protected val mAnisotropy = WLFloatControl(0.8f, LLSettingsSky.SETTING_MIE_ANISOTROPY_FACTOR)
}

class LLAbsorptionDensityProfileSettingsAdapter(layerIndex: Int = 0) :
    LLDensityProfileSettingsAdapter(LLSettingsSky.SETTING_ABSORPTION_CONFIG, layerIndex)

class LLSkySettingsAdapter {
    val mWLGamma = WLFloatControl(1.0f, LLSettingsSky.SETTING_GAMMA)

    val mLightnorm = WLColorControl(floatArrayOf(0f, 0.707f, -0.707f, 1f), LLSettingsSky.SETTING_LIGHT_NORMAL)
    val mSunlight = WLColorControl(floatArrayOf(0.5f, 0.5f, 0.5f, 1f), LLSettingsSky.SETTING_SUNLIGHT_COLOR, "WLSunlight")
    val mGlow = WLColorControl(floatArrayOf(18f, 0f, -0.01f, 1f), LLSettingsSky.SETTING_GLOW)

    val mCloudColor = WLColorControl(floatArrayOf(0.5f, 0.5f, 0.5f, 1f), LLSettingsSky.SETTING_CLOUD_COLOR, "WLCloudColor")
    val mCloudMain = WLColorControl(floatArrayOf(0.5f, 0.5f, 0.125f, 1f), LLSettingsSky.SETTING_CLOUD_POS_DENSITY1)
    val mCloudCoverage = WLFloatControl(0.0f, LLSettingsSky.SETTING_CLOUD_SHADOW)
    val mCloudDetail = WLColorControl(floatArrayOf(0f, 0f, 0f, 1f), LLSettingsSky.SETTING_CLOUD_POS_DENSITY2)
    val mCloudScale = WLFloatControl(0.42f, LLSettingsSky.SETTING_CLOUD_SCALE)
}

class LLWatterSettingsAdapter {
    val mFogColor = WLColorControl(
        floatArrayOf(22f / 255f, 43f / 255f, 54f / 255f, 0f),
        LLSettingsWater.SETTING_FOG_COLOR, "WaterFogColor"
    )
    val mFogDensity = WLXFloatControl(4f, LLSettingsWater.SETTING_FOG_DENSITY, 2f)
    val mUnderWaterFogMod = WLFloatControl(0.25f, LLSettingsWater.SETTING_FOG_MOD)

    val mNormalScale = WLVect3Control(2f, 2f, 2f, LLSettingsWater.SETTING_NORMAL_SCALE)
    val mWave1Dir = WLVect2Control(0.5f, 0.5f, LLSettingsWater.SETTING_WAVE1_DIR)
    val mWave2Dir = WLVect2Control(0.5f, 0.5f, LLSettingsWater.SETTING_WAVE2_DIR)

    val mFresnelScale = WLFloatControl(0.5f, LLSettingsWater.SETTING_FRESNEL_SCALE)
    val mFresnelOffset = WLFloatControl(0.4f, LLSettingsWater.SETTING_FRESNEL_OFFSET)
    val mScaleAbove = WLFloatControl(0.025f, LLSettingsWater.SETTING_SCALE_ABOVE)
    val mScaleBelow = WLFloatControl(0.2f, LLSettingsWater.SETTING_SCALE_BELOW)
    val mBlurMultiplier = WLFloatControl(0.1f, LLSettingsWater.SETTING_BLUR_MULTIPILER)
}
