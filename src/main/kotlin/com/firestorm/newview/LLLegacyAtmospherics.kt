package com.firestorm.newview

import kotlin.math.*

// ---- Atmosphere constants ----
const val HORIZON_DIST: Float = 1024.0f
const val ATM_EXP_FALLOFF: Float = 0.000126f
const val ATM_SEA_LEVEL_NDENS: Float = 2.55e25f
const val ATM_HEIGHT: Float = 100000f

const val sigma: Float = 0.035f
const val fsigma: Float = (6f + 3f * sigma) / (6f - 7f * sigma)
const val Ndens: Double = 2.55e25
val Ndens2: Double = Ndens * Ndens

// ---- Math/colour stubs (GPU-side types) ----

data class LLColor3(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f) {
    operator fun times(s: Float) = LLColor3(r * s, g * s, b * s)
    operator fun times(o: LLColor3) = LLColor3(r * o.r, g * o.g, b * o.b)
    operator fun plus(o: LLColor3) = LLColor3(r + o.r, g + o.g, b + o.b)
    operator fun minus(o: LLColor3) = LLColor3(r - o.r, g - o.g, b - o.b)
    operator fun unaryMinus() = LLColor3(-r, -g, -b)
    fun brightness() = (r + g + b) / 3f
    fun divide(o: LLColor3) = LLColor3(r / o.r, g / o.g, b / o.b)

    companion object {
        val white = LLColor3(1f, 1f, 1f)
    }
}

data class LLColor4(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f, var a: Float = 0f) {
    constructor(c: LLColor3, a: Float = 0f) : this(c.r, c.g, c.b, a)
    fun setAlpha(v: Float) { a = v }
    operator fun times(s: Float) = LLColor4(r * s, g * s, b * s, a * s)
}

data class LLVector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun length() = sqrt(x * x + y * y + z * z)
    fun normalize(): LLVector3 {
        val l = length()
        return if (l > 0f) LLVector3(x / l, y / l, z / l) else this
    }
    operator fun plus(o: LLVector3) = LLVector3(x + o.x, y + o.y, z + o.z)
    operator fun times(s: Float) = LLVector3(x * s, y * s, z * s)
    operator fun divAssign(s: Float) { x /= s; y /= s; z /= s }
    operator fun timesAssign(s: Float) { x *= s; y *= s; z *= s }
    operator fun get(i: Int) = when (i) { 0 -> x; 1 -> y; else -> z }
    operator fun set(i: Int, v: Float) { when (i) { 0 -> x = v; 1 -> y = v; else -> z = v } }
    infix fun dot(o: LLVector3) = x * o.x + y * o.y + z * o.z
}

data class LLVector4(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 0f) {
    operator fun get(i: Int) = when (i) { 0 -> x; 1 -> y; 2 -> z; else -> w }
}

fun smear(v: Float) = LLColor3(v, v, v)
fun colorIntens(c: LLColor3) = (c.r + c.g + c.b) / 3f
fun colorMax(c: LLColor3) = maxOf(c.r, c.g, c.b)
fun componentDiv(a: LLColor3, b: LLColor3) = a.divide(b)
fun componentMult(a: LLColor3, b: LLColor3) = a * b
fun componentExp(c: LLColor3) = LLColor3(exp(c.r), exp(c.g), exp(c.b))
fun componentMultBy(a: LLColor3, b: LLColor3) { a.r *= b.r; a.g *= b.g; a.b *= b.b }

// ---- Fast logarithm lookup table ----

private class LLFastLn {
    private val mTable = FloatArray(257)

    init {
        mTable[0] = 0f
        for (i in 1..256) {
            mTable[i] = ln(i.toFloat())
        }
    }

    fun ln(x: Float): Float {
        val oo255 = 0.003921568627450980392156862745098f
        val ln255 = 5.5412635451584261462455391880218f
        return when {
            x < oo255 -> kotlin.math.ln(x)
            x < 1f -> {
                val scaled = x * 255f
                val index = scaled.toInt()
                val t = scaled - index
                val low = mTable[index]
                val high = mTable[index + 1]
                low + t * (high - low) - ln255
            }
            x <= 255f -> {
                val index = x.toInt()
                val t = x - index
                val low = mTable[index]
                val high = mTable[index + 1]
                low + t * (high - low)
            }
            else -> kotlin.math.ln(x)
        }
    }

    fun pow(x: Float, y: Float): Float = exp(y * ln(x))
}

private val gFastLn = LLFastLn()

// ---- Colour helpers ----

private fun colorPow(col: LLColor3, e: Float) {
    col.r = gFastLn.pow(col.r, e)
    col.g = gFastLn.pow(col.g, e)
    col.b = gFastLn.pow(col.b, e)
}

private fun colorNorm(col: LLColor3): LLColor3 {
    val m = colorMax(col)
    return if (m > 1f) col * (1f / m) else col
}

private fun colorGammaCorrect(col: LLColor3) {
    val gammaInv = 1f / 1.2f
    if (col.r != 0f) col.r = gFastLn.pow(col.r, gammaInv)
    if (col.g != 0f) col.g = gFastLn.pow(col.g, gammaInv)
    if (col.b != 0f) col.b = gFastLn.pow(col.b, gammaInv)
}

fun colorNormPow(col: LLColor3, e: Float, postmultiply: Boolean): Float {
    val mv = colorMax(col)
    if (mv == 0f) return 0f
    col.r *= 1f / mv; col.g *= 1f / mv; col.b *= 1f / mv
    colorPow(col, e)
    if (postmultiply) { col.r *= mv; col.g *= mv; col.b *= mv }
    return mv
}

// Returns azimuth in radians [0, 2π).
fun azimuth(v: LLVector3): Float {
    return when {
        v.x == 0f -> when {
            v.y > 0f -> (PI * 0.5).toFloat()
            v.y < 0f -> (PI * 1.5).toFloat()
            else -> 0f
        }
        else -> {
            var az = atan(v.y / v.x)
            if (v.x < 0f) az += PI.toFloat()
            else if (v.y < 0f) az += (PI * 2).toFloat()
            az
        }
    }
}

// ---- Rayleigh scattering init ----

fun refrIndCalc(waveLength: LLColor3): LLColor3 {
    val result = LLColor3()
    val comps = floatArrayOf(waveLength.r, waveLength.g, waveLength.b)
    for (i in 0..2) {
        val wl2 = comps[i] * comps[i] * 1e-6f
        var v = 6.43e3f + (2.95e6f / (146f - 1f / wl2)) + (2.55e4f / (41f - 1f / wl2))
        v *= 1e-8f
        v += 1f
        when (i) { 0 -> result.r = v; 1 -> result.g = v; else -> result.b = v }
    }
    return result
}

private fun calcAirScaSeaLevel(): LLColor3 {
    val waveLenVals = LLColor3(675f, 520f, 445f)
    val refrInd = refrIndCalc(waveLenVals)
    val one3 = LLColor3(1f, 1f, 1f)
    val n21 = refrInd * refrInd - one3
    val n4 = n21 * n21
    val wl2 = waveLenVals * waveLenVals * 1e-6f
    val wl4 = wl2 * wl2
    val multConst = LLColor3(
        fsigma * 2f / 3f * 1e24f * (PI * PI).toFloat() * n4.r,
        fsigma * 2f / 3f * 1e24f * (PI * PI).toFloat() * n4.g,
        fsigma * 2f / 3f * 1e24f * (PI * PI).toFloat() * n4.b
    )
    val densDivN = (ATM_SEA_LEVEL_NDENS / Ndens2).toFloat()
    return multConst.divide(wl4) * densDivN
}

// ---- LLHaze ----

class LLHaze() {
    var mG: Float = 0f
    var mSigSca: LLColor3 = LLColor3()
    var mFalloff: Float = 1f
    var mAbsCoef: Float = 0f

    constructor(g: Float, sca: LLColor3, fo: Float = 2f) : this() {
        mG = g
        mSigSca = sca * (0.25f / PI.toFloat())
        mFalloff = fo
        mAbsCoef = colorIntens(mSigSca) / sAirScaIntense
    }

    constructor(g: Float, sca: Float, fo: Float = 2f) : this() {
        mG = g
        mSigSca = LLColor3(sca, sca, sca) * (0.25f / PI.toFloat())
        mFalloff = fo
        mAbsCoef = 0.01f * sca / sAirScaAvg
    }

    fun calcAirSca(h: Float): LLColor3 = sAirScaSeaLevel * calcFalloff(h)

    fun calcAirScaInto(h: Float, result: LLColor3) {
        result.r = sAirScaSeaLevel.r; result.g = sAirScaSeaLevel.g; result.b = sAirScaSeaLevel.b
        val f = calcFalloff(h)
        result.r *= f; result.g *= f; result.b *= f
    }

    fun getG(): Float = mG
    fun setG(g: Float) { mG = g }
    fun getSigSca(): LLColor3 = mSigSca
    fun setSigSca(s: LLColor3) {
        mSigSca = s
        mAbsCoef = 0.01f * colorIntens(mSigSca) / sAirScaIntense
    }
    fun setSigSca(s0: Float, s1: Float, s2: Float) {
        mSigSca = LLColor3(s0, s1, s2) * sAirScaAvg
        mAbsCoef = 0.01f * (s0 + s1 + s2) / 3f
    }
    fun getFalloff(): Float = mFalloff
    fun setFalloff(fo: Float) { mFalloff = fo }
    fun getAbsCoef(): Float = mAbsCoef

    fun calcSigSca(h: Float): LLColor3 = mSigSca * calcFalloff(h * mFalloff)
    fun calcSigScaInto(h: Float, result: LLColor3) {
        result.r = mSigSca.r; result.g = mSigSca.g; result.b = mSigSca.b
        val f = calcFalloff(h * mFalloff)
        result.r *= f; result.g *= f; result.b *= f
    }
    fun calcSigExt(h: Float): LLColor3 = mSigSca * (calcFalloff(h * mFalloff) * (1f + mAbsCoef))

    fun calcPhase(cosTheta: Float): Float {
        val g2 = mG * mG
        val den = 1f + g2 - 2f * mG * cosTheta
        return (1f - g2) * gFastLn.pow(den, -1.5f)
    }

    companion object {
        val sAirScaSeaLevel: LLColor3 = calcAirScaSeaLevel()
        val sAirScaIntense: Float = colorIntens(sAirScaSeaLevel)
        val sAirScaAvg: Float = sAirScaIntense / 3f

        fun calcFalloff(h: Float): Float =
            if (h <= 0f) 1f else exp(-ATM_EXP_FALLOFF * h)
    }
}

// ---- AtmosphericsVars ----

class AtmosphericsVars {
    var hazeColor: LLColor3 = LLColor3()
    var hazeColorBelowCloud: LLColor3 = LLColor3()
    var cloudColorSun: LLColor3 = LLColor3()
    var cloudColorAmbient: LLColor3 = LLColor3()
    var cloudDensity: Float = 0f
    var blue_density: LLColor3 = LLColor3()
    var blue_horizon: LLColor3 = LLColor3()
    var haze_density: Float = 0f
    var haze_horizon: Float = 0f
    var density_multiplier: Float = 0f
    var distance_multiplier: Float = 0f
    var max_y: Float = 0f
    var gamma: Float = 1f
    var sun_norm: LLVector4 = LLVector4(0f, 1f, 0f, 1f)
    var sunlight: LLColor3 = LLColor3()
    var ambient: LLColor3 = LLColor3()
    var glow: LLColor3 = LLColor3()
    var cloud_shadow: Float = 1f
    var dome_radius: Float = 1f
    var dome_offset: Float = 1f
    var light_atten: LLColor3 = LLColor3()
    var light_transmittance: LLColor3 = LLColor3()
    var total_density: LLColor3 = LLColor3()

    override fun equals(other: Any?): Boolean {
        if (other !is AtmosphericsVars) return false
        return hazeColor == other.hazeColor &&
            hazeColorBelowCloud == other.hazeColorBelowCloud &&
            cloudColorSun == other.cloudColorSun &&
            cloudColorAmbient == other.cloudColorAmbient &&
            cloudDensity == other.cloudDensity &&
            density_multiplier == other.density_multiplier &&
            haze_horizon == other.haze_horizon &&
            haze_density == other.haze_density &&
            blue_horizon == other.blue_horizon &&
            blue_density == other.blue_density &&
            dome_offset == other.dome_offset &&
            dome_radius == other.dome_radius &&
            cloud_shadow == other.cloud_shadow &&
            glow == other.glow &&
            ambient == other.ambient &&
            sunlight == other.sunlight &&
            sun_norm == other.sun_norm &&
            gamma == other.gamma &&
            max_y == other.max_y &&
            distance_multiplier == other.distance_multiplier
        // light_atten, light_transmittance, total_density intentionally excluded:
        // they are derived from the values above and are recomputed each sky-map pass.
    }

    override fun hashCode(): Int = hazeColor.hashCode()
}

private fun approxEqualF(a: Float, b: Float, threshold: Float): Boolean {
    val diff = abs(a - b)
    return diff < 1e-7f || diff < maxOf(abs(a), abs(b)) * threshold
}

private fun approxEqual(a: LLColor3, b: LLColor3, threshold: Float) =
    approxEqualF(a.r, b.r, threshold) && approxEqualF(a.g, b.g, threshold) && approxEqualF(a.b, b.b, threshold)

private fun approxEqual(a: LLVector4, b: LLVector4, threshold: Float) =
    approxEqualF(a.x, b.x, threshold) && approxEqualF(a.y, b.y, threshold) &&
    approxEqualF(a.z, b.z, threshold) && approxEqualF(a.w, b.w, threshold)

fun approximatelyEqual(a: AtmosphericsVars, b: AtmosphericsVars, fractionThreshold: Float): Boolean =
    approxEqual(a.hazeColor, b.hazeColor, fractionThreshold) &&
    approxEqual(a.hazeColorBelowCloud, b.hazeColorBelowCloud, fractionThreshold) &&
    approxEqual(a.cloudColorSun, b.cloudColorSun, fractionThreshold) &&
    approxEqual(a.cloudColorAmbient, b.cloudColorAmbient, fractionThreshold) &&
    approxEqualF(a.cloudDensity, b.cloudDensity, fractionThreshold) &&
    approxEqualF(a.density_multiplier, b.density_multiplier, fractionThreshold) &&
    approxEqualF(a.haze_horizon, b.haze_horizon, fractionThreshold) &&
    approxEqualF(a.haze_density, b.haze_density, fractionThreshold) &&
    approxEqual(a.blue_horizon, b.blue_horizon, fractionThreshold) &&
    approxEqual(a.blue_density, b.blue_density, fractionThreshold) &&
    approxEqualF(a.dome_offset, b.dome_offset, fractionThreshold) &&
    approxEqualF(a.dome_radius, b.dome_radius, fractionThreshold) &&
    approxEqualF(a.cloud_shadow, b.cloud_shadow, fractionThreshold) &&
    approxEqual(a.glow, b.glow, fractionThreshold) &&
    approxEqual(a.ambient, b.ambient, fractionThreshold) &&
    approxEqual(a.sunlight, b.sunlight, fractionThreshold) &&
    approxEqual(a.sun_norm, b.sun_norm, fractionThreshold) &&
    approxEqualF(a.gamma, b.gamma, fractionThreshold) &&
    approxEqualF(a.max_y, b.max_y, fractionThreshold) &&
    approxEqualF(a.distance_multiplier, b.distance_multiplier, fractionThreshold)
// light_atten, light_transmittance, total_density excluded for same reason as equals().

// ---- LLAtmospherics ----

// Stub for sky-settings interface; real impl lives in LLSettingsSky.
interface LLSettingsSky {
    fun getBlueDensity(): LLColor3
    fun getBlueHorizon(): LLColor3
    fun getHazeDensity(): Float
    fun getHazeHorizon(): Float
    fun getDensityMultiplier(): Float
    fun getDistanceMultiplier(): Float
    fun getMaxY(): Float
    fun getSunlightColor(): LLColor3
    fun getAmbientColor(): LLColor3
    fun getGlow(): LLColor3
    fun getCloudShadow(): Float
    fun getDomeRadius(): Float
    fun getDomeOffset(): Float
    fun getLightAttenuation(maxY: Float): LLColor3
    fun getLightTransmittance(maxY: Float): LLColor3
    fun getLightTransmittanceFast(totalDensity: LLColor3, densityMultiplier: Float, pLen: Float): LLColor3
    fun getTotalDensity(): LLColor3
    fun getGamma(): Float
    fun gammaCorrect(color: LLColor3, gamma: Float): LLColor3
}

class LLAtmospherics {
    private var mHaze: LLHaze = LLHaze()
    private var mHazeConcentration: Float = 0f
    private var mCloudDensity: Float = 0.2f
    private var mWind: Float = 0f
    private var mInitialized: Boolean = false
    private var mLastLightingDirection: LLVector3 = LLVector3()
    private var mLastTotalAmbient: LLColor3 = LLColor3()
    private var mAmbientScale: Float = 0f
    private var mNightColorShift: LLColor3 = LLColor3()
    private var mInterpVal: Float = 0f
    private var mFogColor: LLColor4 = LLColor4(0.5f, 0.5f, 0.5f, 0f)
    private var mGLFogCol: LLColor4 = LLColor4()
    private var mFogRatio: Float = 1.2f
    private var mWorldScale: Float = 1f

    fun init() {
        val hazeInt = colorIntens(mHaze.calcSigSca(0f))
        mHazeConcentration = hazeInt / (colorIntens(mHaze.calcAirSca(0f)) + hazeInt)
        mInitialized = true
    }

    fun getHaze(): LLHaze = mHaze
    fun getHazeConcentration(): Float = mHazeConcentration
    fun setHaze(h: LLHaze) { mHaze = h }
    fun setFogRatio(fogRatio: Float) { mFogRatio = fogRatio }
    fun getFogRatio(): Float = mFogRatio
    fun getFogColor(): LLColor4 = mFogColor
    fun getGLFogColor(): LLColor4 = mGLFogCol
    fun setCloudDensity(cloudDensity: Float) { mCloudDensity = cloudDensity }
    fun setWind(wind: LLVector3) { mWind = wind.length() }

    fun calcSkyColorInDir(
        psky: LLSettingsSky,
        vars: AtmosphericsVars,
        dir: LLVector3,
        isShiny: Boolean = false,
        lowEnd: Boolean = false
    ): LLColor4 {
        val skySaturation = 0.25f
        val landSaturation = 0.1f

        if (isShiny && dir.z < -0.02f) {
            var desat = LLColor3(mFogColor.r, mFogColor.g, mFogColor.b)
            var brightness = desat.brightness()
            if (brightness < 0.15f) {
                brightness = 0.15f
                desat = smear(0.15f)
            }
            val greyscaleSat = brightness * (1f - landSaturation)
            desat = desat * landSaturation + smear(greyscaleSat)
            val base = if (lowEnd) LLColor4(desat, 0f) else LLColor4(desat * 0.5f, 0f)
            var x = 1f - abs(-0.1f - dir.z)
            x *= x
            return LLColor4(base.r * x * x, base.g * x.pow(2.5f), base.b * x * x * x, 0f)
        }

        // Undo OGL_TO_CFR_ROTATION and negate vertical direction.
        val pn = LLVector3(-dir.y, -dir.z, -dir.x)
        calcSkyColorWLVert(psky, pn, vars)

        if (isShiny) {
            val brightness = vars.hazeColor.brightness()
            val greyscaleSat = brightness * (1f - skySaturation)
            val skyColor = vars.hazeColor * skySaturation + smear(greyscaleSat)
            return LLColor4(skyColor, 0f)
        }

        val skyColor = if (lowEnd) vars.hazeColor * 2f else psky.gammaCorrect(vars.hazeColor * 2f, vars.gamma)
        return LLColor4(skyColor, 0f)
    }

    // Keep in sync with skyV.glsl and cloudsV.glsl.
    protected fun calcSkyColorWLVert(psky: LLSettingsSky, pn: LLVector3, vars: AtmosphericsVars) {
        val blueDensity = vars.blue_density
        val blueHorizon = vars.blue_horizon
        val hazeHorizon = vars.haze_horizon
        val hazeDensity = vars.haze_density
        val densityMultiplier = vars.density_multiplier
        val maxY = vars.max_y
        val sunNorm = vars.sun_norm

        val phi = acos(pn.y)
        val sinA = sin(PI.toFloat() - phi).let { if (abs(it) < 0.01f) 0.01f else it }
        val plen = vars.dome_radius * sin(PI.toFloat() + phi + asin(vars.dome_offset * sinA)) / sinA

        val pnScaled = LLVector3(pn.x * plen, pn.y * plen, pn.z * plen)
        val pnAdjusted = if (pnScaled.y > 0f) {
            val s = maxY / pnScaled.y
            LLVector3(pnScaled.x * s, pnScaled.y * s, pnScaled.z * s)
        } else {
            val s = -32000f / pnScaled.y
            LLVector3(pnScaled.x * s, pnScaled.y * s, pnScaled.z * s)
        }

        val plenFinal = pnAdjusted.length()
        val pnNorm = if (plenFinal > 0f) LLVector3(pnAdjusted.x / plenFinal, pnAdjusted.y / plenFinal, pnAdjusted.z / plenFinal) else pnAdjusted

        var sunlight = LLColor3(vars.sunlight.r, vars.sunlight.g, vars.sunlight.b)
        val ambient = LLColor3(vars.ambient.r, vars.ambient.g, vars.ambient.b)
        val glow = vars.glow
        val cloudShadow = vars.cloud_shadow
        val lightAtten = vars.light_atten
        val lightTransmittance = psky.getLightTransmittanceFast(vars.total_density, vars.density_multiplier, plenFinal)

        val temp1 = LLColor3(vars.total_density.r, vars.total_density.g, vars.total_density.b)
        val blueWeight = componentDiv(blueDensity, temp1)
        val blueFactor = blueHorizon * blueWeight
        val hazeDensityColor = smear(hazeDensity)
        val hazeWeight = componentDiv(hazeDensityColor, temp1)
        val hazeFactor = smear(hazeHorizon) * hazeWeight

        var temp2y = maxOf(1e-7f, maxOf(0f, pnNorm.y) + sunNorm[1])
        temp2y = 1f / temp2y
        componentMultBy(sunlight, componentExp(lightAtten * (-1f) * temp2y))
        componentMultBy(sunlight, lightTransmittance)

        val temp2z = plenFinal * densityMultiplier
        val transparency = componentExp(temp1 * (-1f) * temp2z)

        var temp2x = pnNorm dot LLVector3(sunNorm[0], sunNorm[1], sunNorm[2])
        temp2x = 1f - temp2x
        temp2x = maxOf(temp2x, 0.001f)
        temp2x *= glow.r
        temp2x = temp2x.pow(glow.b)
        temp2x += 0.25f

        vars.hazeColor = blueFactor * (sunlight + ambient) + componentMult(hazeFactor, sunlight * temp2x + ambient)

        val tmpAmbient = ambient + (LLColor3.white - ambient) * (cloudShadow * 0.5f)
        val sunlightDimmed = sunlight * (1f - cloudShadow)

        vars.hazeColorBelowCloud = blueFactor * (sunlightDimmed + tmpAmbient) +
            componentMult(hazeFactor, sunlightDimmed * temp2x + tmpAmbient)

        componentMultBy(vars.hazeColor, LLColor3.white - transparency)
    }

    fun updateFog(distance: Float, tosunIn: LLVector3) {
        TODO("GPU: updateFog — requires pipeline fog state and LLEnvironment access")
    }
}

private operator fun LLColor3.times(f: Float) = LLColor3(r * f, g * f, b * f)
private operator fun LLColor3.minus(o: LLColor3) = LLColor3(r - o.r, g - o.g, b - o.b)
private operator fun Float.times(c: LLColor3) = LLColor3(this * c.r, this * c.g, this * c.b)
