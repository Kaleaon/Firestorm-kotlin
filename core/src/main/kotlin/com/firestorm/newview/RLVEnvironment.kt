package com.firestorm.newview

import java.util.UUID
import kotlin.math.*

// ============================================================================
// RlvIsOfSettingsType — inventory collector for settings of a specific subtype
//

class RlvIsOfSettingsType(
    private val settingsType: LLSettingsType,
    private val nameMatch: String = ""
) : LLInventoryCollectFunctor() {

    override fun invoke(folder: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null || item.actualType != LLAssetType.AT_SETTINGS) return false
        return (settingsType == LLSettingsType.fromInventoryFlags(item.flags)) &&
            (nameMatch.isEmpty() || item.name.equals(nameMatch, ignoreCase = true))
    }
}

// ============================================================================
// Geometry helpers (ported from file-level math section of rlvenvironment.cpp)
//

private const val F_TWO_PI = (2.0 * Math.PI).toFloat()
private const val F_PI_BY_TWO = (Math.PI / 2.0).toFloat()

/*
 * Azimuth and elevation extraction from a sun/moon direction vector.
 * Full derivation comment retained from original source — see rlvenvironment.cpp header.
 *
 * SL axis mapping: azimuth rotates E→N→W→S; elevation = asin(z).
 * Special cases handle degenerate (x=0) or (y=0) inputs.
 */
fun rlvGetAzimuthFromDirectionVector(vecDir: FloatArray): Float {
    if (vecDir[1] == 0.0f) return 0.0f
    if (vecDir[0] == 0.0f) return F_PI_BY_TWO
    val radAzimuth = atan2(vecDir[1], vecDir[0])
    return if (radAzimuth >= 0.0f) radAzimuth else radAzimuth + F_TWO_PI
}

fun rlvGetElevationFromDirectionVector(vecDir: FloatArray): Float {
    if (vecDir[2] == 0.0f) return 0.0f
    val radElevation = when {
        vecDir[0] == 0.0f && vecDir[1] != 0.0f -> atan2(vecDir[2], vecDir[1])
        vecDir[0] != 0.0f && vecDir[1] == 0.0f -> atan2(vecDir[2], vecDir[0])
        else -> asin(vecDir[2])
    }
    return if (radElevation >= 0.0f) radElevation else radElevation + F_TWO_PI
}

private fun normalizeAngleDomain(angle: Float): Float {
    var a = angle
    while (a < 0) a += F_TWO_PI
    while (a > F_TWO_PI) a -= F_TWO_PI
    return a
}

// ============================================================================
// RlvEnvironment
//

class RlvEnvironment : RlvExtCommandHandler() {

    companion object {
        private const val SLIDER_SCALE_BLUE_HORIZON_DENSITY = 2.0f
        private const val SLIDER_SCALE_DENSITY_MULTIPLIER = 0.001f
        private const val SLIDER_SCALE_GLOW_R = 20.0f
        private const val SLIDER_SCALE_GLOW_B = -5.0f
        private const val SLIDER_SCALE_SUN_AMBIENT = 3.0f

        private const val RLV_GETENV_PREFIX = "getenv_"
        private const val RLV_SETENV_PREFIX = "setenv_"

        private fun getTargetEnvironment(): LLEnvironment.EnvSelection {
            return if (RlvActions.canChangeEnvironment()) {
                LLEnvironment.EnvSelection.ENV_LOCAL
            } else {
                LLEnvironment.EnvSelection.ENV_EDIT
            }
        }

        private fun getTargetSky(forSetCmd: Boolean = false): LLSettingsSky? {
            System.err.println("RlvEnvironment: getTargetSky not yet implemented")
            return null
        }

        private fun rlvGetLibraryEnvironmentsFolder(): UUID {
            System.err.println("RlvEnvironment: rlvGetLibraryEnvironmentsFolder not yet implemented")
            return UUID(0, 0)
        }

        fun onHandleCommand(
            rlvCmd: RlvCommand,
            cmdRet: ERlvCmdRetHolder,
            cmdPrefix: String,
            fnLookup: Map<String, (String) -> ERlvCmdRet>,
            legacyFnLookup: Map<String, (String, UInt) -> ERlvCmdRet>
        ): Boolean {
            val behaviour = rlvCmd.behaviour
            if (behaviour.length <= cmdPrefix.length + 2 || !behaviour.startsWith(cmdPrefix)) return false

            if (rlvCmd.paramType == ERlvParamType.RLV_TYPE_FORCE && !RlvActions.canChangeEnvironment(rlvCmd.objectId)) {
                cmdRet.value = ERlvCmdRet.RLV_RET_FAILED_LOCK
                return true
            }

            val envCommand = behaviour.substring(cmdPrefix.length)
            val handler = fnLookup[envCommand]
            if (handler != null) {
                val arg = if (rlvCmd.paramType == ERlvParamType.RLV_TYPE_FORCE) rlvCmd.option else rlvCmd.param
                cmdRet.value = handler(arg)
                return true
            }

            // Legacy per-component handling (append char suffix denoting RGBA component)
            val idxComponent = rlvGetColorComponentFromCharacter(envCommand.last())
            if (idxComponent <= VALPHA) {
                val baseName = envCommand.dropLast(1)
                val legacyHandler = legacyFnLookup[baseName]
                if (legacyHandler != null) {
                    val arg = if (rlvCmd.paramType == ERlvParamType.RLV_TYPE_FORCE) rlvCmd.option else rlvCmd.param
                    cmdRet.value = legacyHandler(arg, idxComponent)
                    return true
                }
            }

            return false
        }

        private fun rlvGetColorComponentFromCharacter(ch: Char): UInt = when (ch) {
            'r', 'x' -> VRED
            'g', 'y' -> VGREEN
            'b', 'd' -> VBLUE
            'i' -> VALPHA
            else -> UInt.MAX_VALUE
        }

        private const val VRED: UInt = 0u
        private const val VGREEN: UInt = 1u
        private const val VBLUE: UInt = 2u
        private const val VALPHA: UInt = 3u
    }

    private val getFnLookup: MutableMap<String, (String) -> ERlvCmdRet> = mutableMapOf()
    private val setFnLookup: MutableMap<String, (String) -> ERlvCmdRet> = mutableMapOf()
    private val legacyGetFnLookup: MutableMap<String, (String, UInt) -> ERlvCmdRet> = mutableMapOf()
    private val legacySetFnLookup: MutableMap<String, (String, UInt) -> ERlvCmdRet> = mutableMapOf()

    init {
        // ---- Presets ----
        registerSetEnvFn<UUID>("asset") { env, idAsset ->
            if (idAsset == null || idAsset == UUID(0, 0)) return@registerSetEnvFn ERlvCmdRet.RLV_RET_FAILED_OPTION
            System.err.println("RlvEnvironment: apply environment asset UUID not yet implemented")
            ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
        }

        val fnApplyLibraryPreset = { env: LLEnvironment.EnvSelection, strPreset: String, settingsType: LLSettingsType ->
            System.err.println("RlvEnvironment: fnApplyLibraryPreset not yet implemented")
            ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
        }
        registerSetEnvFn<String>("preset") { env, preset -> fnApplyLibraryPreset(env, preset, LLSettingsType.ST_SKY) }
        registerSetEnvFn<String>("daycycle") { env, preset -> fnApplyLibraryPreset(env, preset, LLSettingsType.ST_DAYCYCLE) }

        // ---- Atmosphere & Lighting ----
        registerSkyFn<FloatArray>("ambient",
            { sky -> sky.ambientColor.map { it / SLIDER_SCALE_SUN_AMBIENT }.toFloatArray() },
            { sky, v -> sky.setAmbientColor(v.map { it * SLIDER_SCALE_SUN_AMBIENT }.toFloatArray()) })
        registerLegacySkyFn<FloatArray>("ambient",
            { sky -> sky.ambientColor.map { it / SLIDER_SCALE_SUN_AMBIENT }.toFloatArray() },
            { sky, v -> sky.setAmbientColor(v.map { it * SLIDER_SCALE_SUN_AMBIENT }.toFloatArray()) })

        registerSkyFn<FloatArray>("bluedensity",
            { sky -> sky.blueDensity.map { it / SLIDER_SCALE_BLUE_HORIZON_DENSITY }.toFloatArray() },
            { sky, v -> sky.setBlueDensity(v.map { it * SLIDER_SCALE_BLUE_HORIZON_DENSITY }.toFloatArray()) })
        registerLegacySkyFn<FloatArray>("bluedensity",
            { sky -> sky.blueDensity.map { it / SLIDER_SCALE_BLUE_HORIZON_DENSITY }.toFloatArray() },
            { sky, v -> sky.setBlueDensity(v.map { it * SLIDER_SCALE_BLUE_HORIZON_DENSITY }.toFloatArray()) })

        registerSkyFn<FloatArray>("bluehorizon",
            { sky -> sky.blueHorizon.map { it / SLIDER_SCALE_BLUE_HORIZON_DENSITY }.toFloatArray() },
            { sky, v -> sky.setBlueHorizon(v.map { it * SLIDER_SCALE_BLUE_HORIZON_DENSITY }.toFloatArray()) })
        registerLegacySkyFn<FloatArray>("bluehorizon",
            { sky -> sky.blueHorizon.map { it / SLIDER_SCALE_BLUE_HORIZON_DENSITY }.toFloatArray() },
            { sky, v -> sky.setBlueHorizon(v.map { it * SLIDER_SCALE_BLUE_HORIZON_DENSITY }.toFloatArray()) })

        registerSkyFn<Float>("densitymultiplier",
            { sky -> sky.densityMultiplier / SLIDER_SCALE_DENSITY_MULTIPLIER },
            { sky, v -> sky.setDensityMultiplier(v * SLIDER_SCALE_DENSITY_MULTIPLIER) })

        registerSkyFn<Float>("distancemultiplier",
            { sky -> sky.distanceMultiplier },
            { sky, v -> sky.setDistanceMultiplier(v) })

        registerSkyFn<Float>("dropletradius",
            { sky -> sky.skyDropletRadius },
            { sky, v -> sky.setSkyDropletRadius(v) })

        registerSkyFn<Float>("hazedensity",
            { sky -> sky.hazeDensity },
            { sky, v -> sky.setHazeDensity(v) })

        registerSkyFn<Float>("hazehorizon",
            { sky -> sky.hazeHorizon },
            { sky, v -> sky.setHazeHorizon(v) })

        registerSkyFn<Float>("icelevel",
            { sky -> sky.skyIceLevel },
            { sky, v -> sky.setSkyIceLevel(v) })

        registerSkyFn<Float>("maxaltitude",
            { sky -> sky.maxY },
            { sky, v -> sky.setMaxY(v) })

        registerSkyFn<Float>("moisturelevel",
            { sky -> sky.skyMoistureLevel },
            { sky, v -> sky.setSkyMoistureLevel(v) })

        registerSkyFn<Float>("scenegamma",
            { sky -> sky.gamma },
            { sky, v -> sky.setGamma(v) })

        // ---- Clouds ----
        registerSkyFn<FloatArray>("cloudcolor",
            { sky -> sky.cloudColor },
            { sky, v -> sky.setCloudColor(v) })
        registerLegacySkyFn<FloatArray>("cloudcolor",
            { sky -> sky.cloudColor },
            { sky, v -> sky.setCloudColor(v) })

        registerSkyFn<Float>("cloudcoverage",
            { sky -> sky.cloudShadow },
            { sky, v -> sky.setCloudShadow(v) })

        registerSkyFn<FloatArray>("clouddensity",
            { sky -> sky.cloudPosDensity1 },
            { sky, v -> sky.setCloudPosDensity1(v) })
        registerLegacySkyFn<FloatArray>("cloud",
            { sky -> sky.cloudPosDensity1 },
            { sky, v -> sky.setCloudPosDensity1(v) })

        registerSkyFn<FloatArray>("clouddetail",
            { sky -> sky.cloudPosDensity2 },
            { sky, v -> sky.setCloudPosDensity2(v) })
        registerLegacySkyFn<FloatArray>("clouddetail",
            { sky -> sky.cloudPosDensity2 },
            { sky, v -> sky.setCloudPosDensity2(v) })

        registerSkyFn<Float>("cloudscale",
            { sky -> sky.cloudScale },
            { sky, v -> sky.setCloudScale(v) })

        registerSkyFn<FloatArray>("cloudscroll",
            { sky -> sky.cloudScrollRate },
            { sky, v -> sky.setCloudScrollRate(v) })
        registerLegacySkyFn<FloatArray>("cloudscroll",
            { sky -> sky.cloudScrollRate },
            { sky, v -> sky.setCloudScrollRate(v) })

        registerSkyFn<UUID>("cloudtexture",
            { sky -> sky.cloudNoiseTextureId },
            { sky, v -> sky.setCloudNoiseTextureId(v) })

        registerSkyFn<Float>("cloudvariance",
            { sky -> sky.cloudVariance },
            { sky, v -> sky.setCloudVariance(v) })

        // ---- Sun & Moon ----
        registerSkyFn<Float>("moonbrightness",
            { sky -> sky.moonBrightness },
            { sky, v -> sky.setMoonBrightness(v) })

        registerSkyFn<Float>("moonscale",
            { sky -> sky.moonScale },
            { sky, v -> sky.setMoonScale(v) })

        registerSkyFn<UUID>("moontexture",
            { sky -> sky.moonTextureId },
            { sky, v -> sky.setMoonTextureId(v) })

        registerSkyFn<Float>("sunglowsize",
            { sky -> 2.0f - (sky.glowRed / SLIDER_SCALE_GLOW_R) },
            { sky, v -> sky.setGlow(floatArrayOf((2.0f - v) * SLIDER_SCALE_GLOW_R, 0.0f, sky.glowBlue)) })

        registerSkyFn<Float>("sunglowfocus",
            { sky -> sky.glowBlue / SLIDER_SCALE_GLOW_B },
            { sky, v -> sky.setGlow(floatArrayOf(sky.glowRed, 0.0f, v * SLIDER_SCALE_GLOW_B)) })

        registerSkyFn<FloatArray>("sunlightcolor",
            { sky -> sky.sunlightColor.map { it / SLIDER_SCALE_SUN_AMBIENT }.toFloatArray() },
            { sky, v -> sky.setSunlightColor(v.map { it * SLIDER_SCALE_SUN_AMBIENT }.toFloatArray()) })
        registerLegacySkyFn<FloatArray>("sunmooncolor",
            { sky -> sky.sunlightColor.map { it / SLIDER_SCALE_SUN_AMBIENT }.toFloatArray() },
            { sky, v -> sky.setSunlightColor(v.map { it * SLIDER_SCALE_SUN_AMBIENT }.toFloatArray()) })

        registerSkyFn<Float>("sunscale",
            { sky -> sky.sunScale },
            { sky, v -> sky.setSunScale(v) })

        registerSkyFn<UUID>("suntexture",
            { sky -> sky.sunTextureId },
            { sky, v -> sky.setSunTextureId(v) })

        registerSkyFn<Float>("starbrightness",
            { sky -> sky.starBrightness },
            { sky, v -> sky.setStarBrightness(v) })

        registerSkyFn<Float>("sunazimuth",
            { sky -> rlvGetAzimuthFromDirectionVector(sky.sunDirection) },
            { sky, radAzimuth ->
                val elev = rlvGetElevationFromDirectionVector(sky.sunDirection)
                sky.setSunRotation(convertAzimuthAndAltitudeToQuat(radAzimuth, elev))
            })

        registerSkyFn<Float>("sunelevation",
            { sky -> rlvGetElevationFromDirectionVector(sky.sunDirection) },
            { sky, radElevation ->
                val clamped = radElevation.coerceIn(-F_PI_BY_TWO, F_PI_BY_TWO)
                val azimuth = rlvGetAzimuthFromDirectionVector(sky.sunDirection)
                sky.setSunRotation(convertAzimuthAndAltitudeToQuat(azimuth, clamped))
            })

        registerSkyFn<Float>("moonazimuth",
            { sky -> rlvGetAzimuthFromDirectionVector(sky.moonDirection) },
            { sky, radAzimuth ->
                val elev = rlvGetElevationFromDirectionVector(sky.moonDirection)
                sky.setMoonRotation(convertAzimuthAndAltitudeToQuat(radAzimuth, elev))
            })

        registerSkyFn<Float>("moonelevation",
            { sky -> rlvGetElevationFromDirectionVector(sky.moonDirection) },
            { sky, radElevation ->
                val clamped = radElevation.coerceIn(-F_PI_BY_TWO, F_PI_BY_TWO)
                val azimuth = rlvGetAzimuthFromDirectionVector(sky.moonDirection)
                sky.setMoonRotation(convertAzimuthAndAltitudeToQuat(azimuth, clamped))
            })

        // Legacy WindLight: east angle inverts azimuth direction and normalises to [0,1)
        registerSkyFn<Float>("eastangle",
            { sky ->
                normalizeAngleDomain(-rlvGetAzimuthFromDirectionVector(sky.sunDirection)) / F_TWO_PI
            },
            { sky, radEastAngle ->
                val radAzimuth = -radEastAngle * F_TWO_PI
                val radElevation = rlvGetElevationFromDirectionVector(sky.sunDirection)
                sky.setSunRotation(convertAzimuthAndAltitudeToQuat(radAzimuth, radElevation))
                sky.setMoonRotation(convertAzimuthAndAltitudeToQuat(radAzimuth + Math.PI.toFloat(), -radElevation))
            })

        registerSkyFn<Float>("sunmoonposition",
            { sky -> rlvGetElevationFromDirectionVector(sky.sunDirection) / F_TWO_PI },
            { sky, nValue ->
                val radAzimuth = rlvGetAzimuthFromDirectionVector(sky.sunDirection)
                val radElevation = nValue * F_TWO_PI
                sky.setSunRotation(convertAzimuthAndAltitudeToQuat(radAzimuth, radElevation))
                sky.setMoonRotation(convertAzimuthAndAltitudeToQuat(radAzimuth + Math.PI.toFloat(), -radElevation))
            })

        // daytime: apply a fixed sky at a fraction of the nearest day-cycle
        registerSetEnvFn<Float>("daytime") { env, nValue ->
            when {
                nValue in 0.0f..1.0f -> {
                    System.err.println("RlvEnvironment: daytime set (0..1) not yet implemented")
                    ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
                }
                nValue == -1.0f -> {
                    System.err.println("RlvEnvironment: daytime clear/revert not yet implemented")
                    ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
                }
                else -> ERlvCmdRet.RLV_RET_FAILED_OPTION
            }
        }
        registerGetEnvFn("daytime") { env ->
            System.err.println("RlvEnvironment: daytime get not yet implemented")
            ""
        }
    }

    override fun onReplyCommand(rlvCmd: RlvCommand, cmdRet: ERlvCmdRetHolder): Boolean =
        onHandleCommand(rlvCmd, cmdRet, RLV_GETENV_PREFIX, getFnLookup, legacyGetFnLookup)

    override fun onForceCommand(rlvCmd: RlvCommand, cmdRet: ERlvCmdRetHolder): Boolean =
        onHandleCommand(rlvCmd, cmdRet, RLV_SETENV_PREFIX, setFnLookup, legacySetFnLookup)

    // ---- Registration helpers ----

    private fun registerGetEnvFn(name: String, fn: (LLEnvironment.EnvSelection) -> String) {
        check(name !in getFnLookup) { "Duplicate get-env fn: $name" }
        getFnLookup[name] = { param ->
            val reply = fn(getTargetEnvironment())
            if (RlvUtil.sendChatReply(param, reply)) ERlvCmdRet.RLV_RET_SUCCESS else ERlvCmdRet.RLV_RET_FAILED_PARAM
        }
    }

    private inline fun <reified T> registerSetEnvFn(name: String, crossinline fn: (LLEnvironment.EnvSelection, T) -> ERlvCmdRet) {
        check(name !in setFnLookup) { "Duplicate set-env fn: $name" }
        setFnLookup[name] = { option ->
            val value = parseOption<T>(option) ?: return@set ERlvCmdRet.RLV_RET_FAILED_PARAM
            fn(getTargetEnvironment(), value)
        }
    }

    private inline fun <reified T> registerSkyFn(
        name: String,
        crossinline getFn: (LLSettingsSky) -> T,
        crossinline setFn: (LLSettingsSky, T) -> Unit
    ) {
        check(name !in getFnLookup) { "Duplicate sky get fn: $name" }
        getFnLookup[name] = { param ->
            val sky = getTargetSky() ?: return@set ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
            val reply = formatSkyValue(getFn(sky))
            if (RlvUtil.sendChatReply(param, reply)) ERlvCmdRet.RLV_RET_SUCCESS else ERlvCmdRet.RLV_RET_FAILED_PARAM
        }
        check(name !in setFnLookup) { "Duplicate sky set fn: $name" }
        setFnLookup[name] = { option ->
            val value = parseOption<T>(option) ?: return@set ERlvCmdRet.RLV_RET_FAILED_PARAM
            val sky = getTargetSky(forSetCmd = true) ?: return@set ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
            setFn(sky, value)
            sky.update()
            ERlvCmdRet.RLV_RET_SUCCESS
        }
    }

    private inline fun <reified T> registerLegacySkyFn(
        name: String,
        crossinline getFn: (LLSettingsSky) -> T,
        crossinline setFn: (LLSettingsSky, T) -> Unit
    ) {
        check(name !in legacyGetFnLookup) { "Duplicate legacy sky get fn: $name" }
        legacyGetFnLookup[name] = { param, idxComponent ->
            val sky = getTargetSky() ?: return@set ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
            val reply = formatLegacyComponentValue(getFn(sky), idxComponent) ?: return@set ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
            if (RlvUtil.sendChatReply(param, reply)) ERlvCmdRet.RLV_RET_SUCCESS else ERlvCmdRet.RLV_RET_FAILED_PARAM
        }
        check(name !in legacySetFnLookup) { "Duplicate legacy sky set fn: $name" }
        legacySetFnLookup[name] = { option, idxComponent ->
            val optionFloat = option.toFloatOrNull() ?: return@set ERlvCmdRet.RLV_RET_FAILED_PARAM
            val sky = getTargetSky(forSetCmd = true) ?: return@set ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
            val result = applyLegacyComponentSet(getFn(sky), optionFloat, idxComponent) ?: return@set ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
            setFn(sky, result)
            sky.update()
            ERlvCmdRet.RLV_RET_SUCCESS
        }
    }

    // ---- Generic option parsing / formatting ----

    private inline fun <reified T> parseOption(option: String): T? {
        System.err.println("RlvEnvironment: parseOption not yet implemented")
        return null
    }

    private fun <T> formatSkyValue(value: T): String = when (value) {
        is Float -> value.toString()
        is UUID -> value.toString()
        is FloatArray -> when (value.size) {
            2 -> "${value[0]}/${value[1]}"
            3 -> "${value[0]}/${value[1]}/${value[2]}"
            else -> value.joinToString("/")
        }
        else -> value.toString()
    }

    private fun <T> formatLegacyComponentValue(value: T, idxComponent: UInt): String? {
        return when (value) {
            is FloatArray -> {
                val idx = idxComponent.toInt()
                when {
                    idx < value.size -> value[idx].toString()
                    idxComponent == VALPHA && value.size >= 3 -> maxOf(value[0], value[1], value[2]).toString()
                    else -> null
                }
            }
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> applyLegacyComponentSet(current: T, newComponentValue: Float, idxComponent: UInt): T? {
        if (current !is FloatArray) return null
        val arr = current.copyOf()
        val idx = idxComponent.toInt()
        return when {
            idx < arr.size -> {
                arr[idx] = newComponentValue
                arr as T
            }
            idxComponent == VALPHA && arr.size >= 3 -> {
                // Scale all channels proportionally to hit the target intensity
                val curMax = maxOf(arr[0], arr[1], arr[2])
                if (newComponentValue == 0.0f || curMax == 0.0f) {
                    arr[0] = newComponentValue; arr[1] = newComponentValue; arr[2] = newComponentValue
                } else {
                    val delta = (newComponentValue - curMax) / curMax
                    arr[0] *= (1.0f + delta); arr[1] *= (1.0f + delta); arr[2] *= (1.0f + delta)
                }
                arr as T
            }
            else -> null
        }
    }
}

// ============================================================================
// Stubs for referenced types (implemented elsewhere in the Kotlin port)
//

abstract class RlvExtCommandHandler {
    abstract fun onReplyCommand(rlvCmd: RlvCommand, cmdRet: ERlvCmdRetHolder): Boolean
    abstract fun onForceCommand(rlvCmd: RlvCommand, cmdRet: ERlvCmdRetHolder): Boolean
}

class ERlvCmdRetHolder(var value: ERlvCmdRet = ERlvCmdRet.RLV_RET_SUCCESS)

abstract class LLInventoryCollectFunctor {
    abstract operator fun invoke(folder: LLInventoryCategory?, item: LLInventoryItem?): Boolean
}

class LLInventoryCategory
class LLInventoryItem(
    val actualType: LLAssetType = LLAssetType.AT_SETTINGS,
    val flags: Int = 0,
    val name: String = ""
)

enum class LLAssetType { AT_SETTINGS, AT_CATEGORY, AT_OBJECT, AT_CLOTHING, AT_BODYPART }
enum class LLSettingsType {
    ST_SKY, ST_DAYCYCLE, ST_WATER;
    companion object {
        fun fromInventoryFlags(flags: Int): LLSettingsType {
            System.err.println("LLSettingsType: fromInventoryFlags not yet implemented")
            return ST_SKY
        }
    }
}

object LLEnvironment {
    val instance: LLEnvironment = this
    enum class EnvSelection { ENV_LOCAL, ENV_EDIT, ENV_PUSH, ENV_PARCEL, ENV_REGION }
}

class LLSettingsSky {
    // Atmosphere
    val ambientColor: FloatArray get() {
        System.err.println("LLSettingsSky: ambientColor not yet implemented")
        return floatArrayOf(0f, 0f, 0f)
    }
    fun setAmbientColor(v: FloatArray) {
        System.err.println("LLSettingsSky: setAmbientColor not yet implemented")
    }
    val blueDensity: FloatArray get() {
        System.err.println("LLSettingsSky: blueDensity not yet implemented")
        return floatArrayOf(0f, 0f, 0f)
    }
    fun setBlueDensity(v: FloatArray) {
        System.err.println("LLSettingsSky: setBlueDensity not yet implemented")
    }
    val blueHorizon: FloatArray get() {
        System.err.println("LLSettingsSky: blueHorizon not yet implemented")
        return floatArrayOf(0f, 0f, 0f)
    }
    fun setBlueHorizon(v: FloatArray) {
        System.err.println("LLSettingsSky: setBlueHorizon not yet implemented")
    }
    val densityMultiplier: Float get() {
        System.err.println("LLSettingsSky: densityMultiplier not yet implemented")
        return 0f
    }
    fun setDensityMultiplier(v: Float) {
        System.err.println("LLSettingsSky: setDensityMultiplier not yet implemented")
    }
    val distanceMultiplier: Float get() {
        System.err.println("LLSettingsSky: distanceMultiplier not yet implemented")
        return 0f
    }
    fun setDistanceMultiplier(v: Float) {
        System.err.println("LLSettingsSky: setDistanceMultiplier not yet implemented")
    }
    val skyDropletRadius: Float get() {
        System.err.println("LLSettingsSky: skyDropletRadius not yet implemented")
        return 0f
    }
    fun setSkyDropletRadius(v: Float) {
        System.err.println("LLSettingsSky: setSkyDropletRadius not yet implemented")
    }
    val hazeDensity: Float get() {
        System.err.println("LLSettingsSky: hazeDensity not yet implemented")
        return 0f
    }
    fun setHazeDensity(v: Float) {
        System.err.println("LLSettingsSky: setHazeDensity not yet implemented")
    }
    val hazeHorizon: Float get() {
        System.err.println("LLSettingsSky: hazeHorizon not yet implemented")
        return 0f
    }
    fun setHazeHorizon(v: Float) {
        System.err.println("LLSettingsSky: setHazeHorizon not yet implemented")
    }
    val skyIceLevel: Float get() {
        System.err.println("LLSettingsSky: skyIceLevel not yet implemented")
        return 0f
    }
    fun setSkyIceLevel(v: Float) {
        System.err.println("LLSettingsSky: setSkyIceLevel not yet implemented")
    }
    val maxY: Float get() {
        System.err.println("LLSettingsSky: maxY not yet implemented")
        return 0f
    }
    fun setMaxY(v: Float) {
        System.err.println("LLSettingsSky: setMaxY not yet implemented")
    }
    val skyMoistureLevel: Float get() {
        System.err.println("LLSettingsSky: skyMoistureLevel not yet implemented")
        return 0f
    }
    fun setSkyMoistureLevel(v: Float) {
        System.err.println("LLSettingsSky: setSkyMoistureLevel not yet implemented")
    }
    val gamma: Float get() {
        System.err.println("LLSettingsSky: gamma not yet implemented")
        return 0f
    }
    fun setGamma(v: Float) {
        System.err.println("LLSettingsSky: setGamma not yet implemented")
    }
    // Clouds
    val cloudColor: FloatArray get() {
        System.err.println("LLSettingsSky: cloudColor not yet implemented")
        return floatArrayOf(0f, 0f, 0f)
    }
    fun setCloudColor(v: FloatArray) {
        System.err.println("LLSettingsSky: setCloudColor not yet implemented")
    }
    val cloudShadow: Float get() {
        System.err.println("LLSettingsSky: cloudShadow not yet implemented")
        return 0f
    }
    fun setCloudShadow(v: Float) {
        System.err.println("LLSettingsSky: setCloudShadow not yet implemented")
    }
    val cloudPosDensity1: FloatArray get() {
        System.err.println("LLSettingsSky: cloudPosDensity1 not yet implemented")
        return floatArrayOf(0f, 0f, 0f)
    }
    fun setCloudPosDensity1(v: FloatArray) {
        System.err.println("LLSettingsSky: setCloudPosDensity1 not yet implemented")
    }
    val cloudPosDensity2: FloatArray get() {
        System.err.println("LLSettingsSky: cloudPosDensity2 not yet implemented")
        return floatArrayOf(0f, 0f, 0f)
    }
    fun setCloudPosDensity2(v: FloatArray) {
        System.err.println("LLSettingsSky: setCloudPosDensity2 not yet implemented")
    }
    val cloudScale: Float get() {
        System.err.println("LLSettingsSky: cloudScale not yet implemented")
        return 0f
    }
    fun setCloudScale(v: Float) {
        System.err.println("LLSettingsSky: setCloudScale not yet implemented")
    }
    val cloudScrollRate: FloatArray get() {
        System.err.println("LLSettingsSky: cloudScrollRate not yet implemented")
        return floatArrayOf(0f, 0f)
    }
    fun setCloudScrollRate(v: FloatArray) {
        System.err.println("LLSettingsSky: setCloudScrollRate not yet implemented")
    }
    val cloudNoiseTextureId: UUID get() {
        System.err.println("LLSettingsSky: cloudNoiseTextureId not yet implemented")
        return UUID(0, 0)
    }
    fun setCloudNoiseTextureId(v: UUID) {
        System.err.println("LLSettingsSky: setCloudNoiseTextureId not yet implemented")
    }
    val cloudVariance: Float get() {
        System.err.println("LLSettingsSky: cloudVariance not yet implemented")
        return 0f
    }
    fun setCloudVariance(v: Float) {
        System.err.println("LLSettingsSky: setCloudVariance not yet implemented")
    }
    // Sun & Moon
    val moonBrightness: Float get() {
        System.err.println("LLSettingsSky: moonBrightness not yet implemented")
        return 0f
    }
    fun setMoonBrightness(v: Float) {
        System.err.println("LLSettingsSky: setMoonBrightness not yet implemented")
    }
    val moonScale: Float get() {
        System.err.println("LLSettingsSky: moonScale not yet implemented")
        return 0f
    }
    fun setMoonScale(v: Float) {
        System.err.println("LLSettingsSky: setMoonScale not yet implemented")
    }
    val moonTextureId: UUID get() {
        System.err.println("LLSettingsSky: moonTextureId not yet implemented")
        return UUID(0, 0)
    }
    fun setMoonTextureId(v: UUID) {
        System.err.println("LLSettingsSky: setMoonTextureId not yet implemented")
    }
    val glowRed: Float get() {
        System.err.println("LLSettingsSky: glowRed not yet implemented")
        return 0f
    }
    val glowBlue: Float get() {
        System.err.println("LLSettingsSky: glowBlue not yet implemented")
        return 0f
    }
    fun setGlow(v: FloatArray) {
        System.err.println("LLSettingsSky: setGlow not yet implemented")
    }
    val sunlightColor: FloatArray get() {
        System.err.println("LLSettingsSky: sunlightColor not yet implemented")
        return floatArrayOf(0f, 0f, 0f)
    }
    fun setSunlightColor(v: FloatArray) {
        System.err.println("LLSettingsSky: setSunlightColor not yet implemented")
    }
    val sunScale: Float get() {
        System.err.println("LLSettingsSky: sunScale not yet implemented")
        return 0f
    }
    fun setSunScale(v: Float) {
        System.err.println("LLSettingsSky: setSunScale not yet implemented")
    }
    val sunTextureId: UUID get() {
        System.err.println("LLSettingsSky: sunTextureId not yet implemented")
        return UUID(0, 0)
    }
    fun setSunTextureId(v: UUID) {
        System.err.println("LLSettingsSky: setSunTextureId not yet implemented")
    }
    val starBrightness: Float get() {
        System.err.println("LLSettingsSky: starBrightness not yet implemented")
        return 0f
    }
    fun setStarBrightness(v: Float) {
        System.err.println("LLSettingsSky: setStarBrightness not yet implemented")
    }
    val sunDirection: FloatArray get() {
        System.err.println("LLSettingsSky: sunDirection not yet implemented")
        return floatArrayOf(0f, 0f, 0f)
    }
    fun setSunRotation(quat: FloatArray) {
        System.err.println("LLSettingsSky: setSunRotation not yet implemented")
    }
    val moonDirection: FloatArray get() {
        System.err.println("LLSettingsSky: moonDirection not yet implemented")
        return floatArrayOf(0f, 0f, 0f)
    }
    fun setMoonRotation(quat: FloatArray) {
        System.err.println("LLSettingsSky: setMoonRotation not yet implemented")
    }
    fun update() {
        System.err.println("LLSettingsSky: update not yet implemented")
    }
}

fun convertAzimuthAndAltitudeToQuat(azimuth: Float, altitude: Float): FloatArray {
    System.err.println("convertAzimuthAndAltitudeToQuat: not yet implemented")
    return floatArrayOf(0f, 0f, 0f, 1f)
}
