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
            TODO("APR: use JVM equivalent — resolve current sky settings, cloning and promoting to local env when forSetCmd requires a writable sky")
        }

        private fun rlvGetLibraryEnvironmentsFolder(): UUID {
            TODO("APR: use JVM equivalent — search inventory for Library/Environments folder UUID")
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
            TODO("APR: use JVM equivalent — apply environment asset UUID to target env selection")
        }

        val fnApplyLibraryPreset = { env: LLEnvironment.EnvSelection, strPreset: String, settingsType: LLSettingsType ->
            TODO<ERlvCmdRet>("APR: use JVM equivalent — locate preset by name or UUID in library inventory and apply to env")
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
                    TODO("APR: use JVM equivalent — locate nearest day cycle, build fixed sky at nValue position, apply to env")
                }
                nValue == -1.0f -> {
                    TODO("APR: use JVM equivalent — clear environment selection and revert to inherited env")
                }
                else -> ERlvCmdRet.RLV_RET_FAILED_OPTION
            }
        }
        registerGetEnvFn("daytime") { env ->
            TODO("APR: use JVM equivalent — return -1 if env is animating day cycle, else return 2 to indicate a static sky is set")
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
        TODO("APR: use JVM equivalent — parse string option into T (Float, UUID, FloatArray for vec2/vec3)")
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
        fun fromInventoryFlags(flags: Int): LLSettingsType = TODO("APR: use JVM equivalent")
    }
}

object LLEnvironment {
    val instance: LLEnvironment = this
    enum class EnvSelection { ENV_LOCAL, ENV_EDIT, ENV_PUSH, ENV_PARCEL, ENV_REGION }
}

class LLSettingsSky {
    // Atmosphere
    val ambientColor: FloatArray get() = TODO("APR: use JVM equivalent")
    fun setAmbientColor(v: FloatArray) { TODO("APR: use JVM equivalent") }
    val blueDensity: FloatArray get() = TODO("APR: use JVM equivalent")
    fun setBlueDensity(v: FloatArray) { TODO("APR: use JVM equivalent") }
    val blueHorizon: FloatArray get() = TODO("APR: use JVM equivalent")
    fun setBlueHorizon(v: FloatArray) { TODO("APR: use JVM equivalent") }
    val densityMultiplier: Float get() = TODO("APR: use JVM equivalent")
    fun setDensityMultiplier(v: Float) { TODO("APR: use JVM equivalent") }
    val distanceMultiplier: Float get() = TODO("APR: use JVM equivalent")
    fun setDistanceMultiplier(v: Float) { TODO("APR: use JVM equivalent") }
    val skyDropletRadius: Float get() = TODO("APR: use JVM equivalent")
    fun setSkyDropletRadius(v: Float) { TODO("APR: use JVM equivalent") }
    val hazeDensity: Float get() = TODO("APR: use JVM equivalent")
    fun setHazeDensity(v: Float) { TODO("APR: use JVM equivalent") }
    val hazeHorizon: Float get() = TODO("APR: use JVM equivalent")
    fun setHazeHorizon(v: Float) { TODO("APR: use JVM equivalent") }
    val skyIceLevel: Float get() = TODO("APR: use JVM equivalent")
    fun setSkyIceLevel(v: Float) { TODO("APR: use JVM equivalent") }
    val maxY: Float get() = TODO("APR: use JVM equivalent")
    fun setMaxY(v: Float) { TODO("APR: use JVM equivalent") }
    val skyMoistureLevel: Float get() = TODO("APR: use JVM equivalent")
    fun setSkyMoistureLevel(v: Float) { TODO("APR: use JVM equivalent") }
    val gamma: Float get() = TODO("APR: use JVM equivalent")
    fun setGamma(v: Float) { TODO("APR: use JVM equivalent") }
    // Clouds
    val cloudColor: FloatArray get() = TODO("APR: use JVM equivalent")
    fun setCloudColor(v: FloatArray) { TODO("APR: use JVM equivalent") }
    val cloudShadow: Float get() = TODO("APR: use JVM equivalent")
    fun setCloudShadow(v: Float) { TODO("APR: use JVM equivalent") }
    val cloudPosDensity1: FloatArray get() = TODO("APR: use JVM equivalent")
    fun setCloudPosDensity1(v: FloatArray) { TODO("APR: use JVM equivalent") }
    val cloudPosDensity2: FloatArray get() = TODO("APR: use JVM equivalent")
    fun setCloudPosDensity2(v: FloatArray) { TODO("APR: use JVM equivalent") }
    val cloudScale: Float get() = TODO("APR: use JVM equivalent")
    fun setCloudScale(v: Float) { TODO("APR: use JVM equivalent") }
    val cloudScrollRate: FloatArray get() = TODO("APR: use JVM equivalent")
    fun setCloudScrollRate(v: FloatArray) { TODO("APR: use JVM equivalent") }
    val cloudNoiseTextureId: UUID get() = TODO("APR: use JVM equivalent")
    fun setCloudNoiseTextureId(v: UUID) { TODO("APR: use JVM equivalent") }
    val cloudVariance: Float get() = TODO("APR: use JVM equivalent")
    fun setCloudVariance(v: Float) { TODO("APR: use JVM equivalent") }
    // Sun & Moon
    val moonBrightness: Float get() = TODO("APR: use JVM equivalent")
    fun setMoonBrightness(v: Float) { TODO("APR: use JVM equivalent") }
    val moonScale: Float get() = TODO("APR: use JVM equivalent")
    fun setMoonScale(v: Float) { TODO("APR: use JVM equivalent") }
    val moonTextureId: UUID get() = TODO("APR: use JVM equivalent")
    fun setMoonTextureId(v: UUID) { TODO("APR: use JVM equivalent") }
    val glowRed: Float get() = TODO("APR: use JVM equivalent")
    val glowBlue: Float get() = TODO("APR: use JVM equivalent")
    fun setGlow(v: FloatArray) { TODO("APR: use JVM equivalent") }
    val sunlightColor: FloatArray get() = TODO("APR: use JVM equivalent")
    fun setSunlightColor(v: FloatArray) { TODO("APR: use JVM equivalent") }
    val sunScale: Float get() = TODO("APR: use JVM equivalent")
    fun setSunScale(v: Float) { TODO("APR: use JVM equivalent") }
    val sunTextureId: UUID get() = TODO("APR: use JVM equivalent")
    fun setSunTextureId(v: UUID) { TODO("APR: use JVM equivalent") }
    val starBrightness: Float get() = TODO("APR: use JVM equivalent")
    fun setStarBrightness(v: Float) { TODO("APR: use JVM equivalent") }
    val sunDirection: FloatArray get() = TODO("APR: use JVM equivalent")
    fun setSunRotation(quat: FloatArray) { TODO("APR: use JVM equivalent") }
    val moonDirection: FloatArray get() = TODO("APR: use JVM equivalent")
    fun setMoonRotation(quat: FloatArray) { TODO("APR: use JVM equivalent") }
    fun update() { TODO("APR: use JVM equivalent") }
}

fun convertAzimuthAndAltitudeToQuat(azimuth: Float, altitude: Float): FloatArray {
    TODO("APR: use JVM equivalent — replicate llsettingssky.cpp quaternion construction from azimuth/altitude angles")
}
