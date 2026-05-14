package com.firestorm.newview

import kotlin.math.*

typealias LLSD = MutableMap<String, Any?>

fun llsdMapOf(vararg pairs: Pair<String, Any?>): LLSD = mutableMapOf(*pairs)
fun llsdEmptyMap(): LLSD = mutableMapOf()

data class LLUUID(val value: String) {
    fun isNull(): Boolean = value.isEmpty() || value == NULL.value
    fun setNull(): LLUUID = NULL
    companion object {
        val NULL = LLUUID("")
    }
}

data class Vector2(var x: Float = 0f, var y: Float = 0f)
data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f)
data class Vector4(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 0f)
data class Color3(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f)
data class Color4(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f, var a: Float = 0f)
data class Quaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f)

abstract class SettingsBase {

    companion object {
        const val INVALID_TRACKPOS: Float = -1.0f
        const val DEFAULT_SETTINGS_NAME: String = "_default_"

        const val SETTING_ID: String       = "id"
        const val SETTING_NAME: String     = "name"
        const val SETTING_HASH: String     = "hash"
        const val SETTING_TYPE: String     = "type"
        const val SETTING_ASSETID: String  = "asset_id"
        const val SETTING_FLAGS: String    = "flags"

        const val FLAG_NOCOPY:  UInt = 0x01u
        const val FLAG_NOMOD:   UInt = 0x02u
        const val FLAG_NOTRANS: UInt = 0x04u
        const val FLAG_NOSAVE:  UInt = 0x08u

        private const val BREAK_POINT: Float = 0.5f

        fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

        fun lerpVector2(a: Vector2, b: Vector2, mix: Float) {
            a.x = lerpF(a.x, b.x, mix)
            a.y = lerpF(a.y, b.y, mix)
        }

        fun lerpVector3(a: Vector3, b: Vector3, mix: Float) {
            a.x = lerpF(a.x, b.x, mix)
            a.y = lerpF(a.y, b.y, mix)
            a.z = lerpF(a.z, b.z, mix)
        }

        fun lerpColor(a: Color3, b: Color3, mix: Float) {
            a.r = lerpF(a.r, b.r, mix)
            a.g = lerpF(a.g, b.g, mix)
            a.b = lerpF(a.b, b.b, mix)
        }

        fun settingValidation(settings: LLSD, validations: MutableList<Validator>, partial: Boolean = false): LLSD {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            var isValid = true
            val validated = mutableSetOf<String>()
            val flags: UInt = if (partial) Validator.VALIDATION_PARTIAL else 0u

            val builtIn = listOf(
                Validator(SETTING_NAME,    false, "String",  { v, _ -> if (v is String) { (v.substring(0, minOf(v.length, 63))).also { settings[SETTING_NAME] = it }; true } else true }),
                Validator(SETTING_ID,      false, "UUID"),
                Validator(SETTING_HASH,    false, "Integer"),
                Validator(SETTING_TYPE,    false, "String"),
                Validator(SETTING_ASSETID, false, "UUID"),
                Validator(SETTING_FLAGS,   false, "Integer")
            )

            for (v in builtIn) {
                if (!v.verify(settings, flags)) {
                    errors += "Unable to validate '${v.name}'."
                    isValid = false
                }
                validated += v.name
            }

            for (v in validations) {
                if (!v.verify(settings, flags)) {
                    errors += "Settings fails validation for '${v.name}'."
                    isValid = false
                }
                validated += v.name
            }

            val extra = settings.keys - validated
            for (key in extra) {
                warnings += "Stripping setting '$key'"
                settings.remove(key)
            }

            return mutableMapOf("success" to isValid, "errors" to errors, "warnings" to warnings)
        }
    }

    class DefaultParam(val shaderKey: Int = -1, val defaultValue: Any? = null)

    class Validator(
        val name: String,
        val required: Boolean,
        val type: String,
        val verify: ((Any?, UInt) -> Boolean)? = null,
        val default: Any? = null
    ) {
        companion object {
            val VALIDATION_PARTIAL: UInt = 0x01u

            fun verifyColor(value: Any?, flags: UInt): Boolean {
                val list = value as? List<*> ?: return false
                return list.size == 3 || list.size == 4
            }

            fun verifyVector(value: Any?, flags: UInt, length: Int): Boolean {
                val list = value as? List<*> ?: return false
                return list.size == length
            }

            fun verifyVectorMinMax(value: Any?, flags: UInt, minVals: List<Any?>, maxVals: List<Any?>): Boolean {
                val list = value as? MutableList<Any?> ?: return false
                for (i in list.indices) {
                    val min = (minVals.getOrNull(i) as? Number)?.toFloat()
                    val max = (maxVals.getOrNull(i) as? Number)?.toFloat()
                    val v = (list[i] as? Number)?.toFloat() ?: continue
                    if (min != null && v < min) list[i] = min
                    if (max != null && v > max) list[i] = max
                }
                return true
            }

            fun verifyVectorNormalized(value: Any?, flags: UInt, length: Int): Boolean {
                val list = value as? List<*> ?: return false
                if (list.size != length) return false
                val sum = list.sumOf { ((it as? Number)?.toDouble() ?: 0.0).pow(2.0) }
                return abs(sqrt(sum) - 1.0) < 1e-5
            }

            fun verifyFloatRange(value: Any?, flags: UInt, range: List<Float>): Boolean {
                val v = (value as? Number)?.toDouble() ?: return false
                return v >= range[0] && v <= range[1]
            }

            fun verifyIntegerRange(value: Any?, flags: UInt, range: List<Int>): Boolean {
                val v = (value as? Number)?.toInt() ?: return false
                return v >= range[0] && v <= range[1]
            }

            fun verifyStringLength(value: Any?, flags: UInt, length: Int): Boolean = true
        }

        fun verify(data: LLSD, flags: UInt): Boolean {
            if (!data.containsKey(name) || data[name] == null) {
                if ((flags and VALIDATION_PARTIAL) != 0u) return true
                if (default != null) { data[name] = default; return true }
                if (required) return false
                return true
            }
            return verify?.invoke(data[name], flags) ?: true
        }
    }

    private val settings: LLSD = llsdEmptyMap()
    private var llsdDirty: Boolean = true
    private var dirty: Boolean = true
    private var replaced: Boolean = false
    private var blendedFactor: Double = 0.0

    var assetId: LLUUID = LLUUID.NULL
        protected set
    var settingId: LLUUID = LLUUID.NULL
        protected set
    var settingName: String = ""
        protected set
    var settingFlags: UInt = 0u
        protected set

    abstract fun getSettingsType(): String
    abstract fun blend(end: SettingsBase, blendf: Double)
    abstract fun buildDerivedClone(): SettingsBase
    protected abstract fun getValidationList(): MutableList<Validator>
    protected open fun getSkipInterpolateKeys(): Set<String> = setOf(SETTING_FLAGS, SETTING_HASH)
    protected open fun getSlerpKeys(): Set<String> = emptySet()
    protected open fun getParameterMap(): Map<String, DefaultParam> = emptyMap()

    fun hasSetting(param: String): Boolean = settings.containsKey(param)
    fun isDirty(): Boolean = dirty
    fun isVeryDirty(): Boolean = replaced
    fun setDirtyFlag(d: Boolean) { dirty = d; clearAssetId() }
    fun setReplaced() { replaced = true }

    fun getId(): LLUUID = settingId
    fun getName(): String = settingName

    fun setName(v: String) {
        settingName = v
        setDirtyFlag(true)
        setLLSDDirty()
    }

    fun getAssetId(): LLUUID = assetId
    fun getFlags(): UInt = settingFlags

    fun setFlags(value: UInt) {
        settingFlags = value
        setDirtyFlag(true)
        setLLSDDirty()
    }

    fun getFlag(flag: UInt): Boolean = (settingFlags and flag) == flag

    fun setFlag(flag: UInt) {
        settingFlags = settingFlags or flag
        setLLSDDirty()
    }

    fun clearFlag(flag: UInt) {
        settingFlags = settingFlags and flag.inv()
        setLLSDDirty()
    }

    open fun replaceSettings(newSettings: LLSD) {
        blendedFactor = 0.0
        setDirtyFlag(true)
        replaced = true
        settings.clear()
        settings.putAll(newSettings)
        loadValuesFromLLSD()
    }

    open fun replaceSettings(other: SettingsBase) {
        blendedFactor = 0.0
        setDirtyFlag(true)
        replaced = true
        settingFlags = other.getFlags()
        settingName = other.getName()
        settingId = other.getId()
        assetId = other.getAssetId()
        setLLSDDirty()
    }

    fun setSettings(newSettings: LLSD) {
        setDirtyFlag(true)
        settings.clear()
        settings.putAll(newSettings)
        loadValuesFromLLSD()
    }

    open fun getSettings(): LLSD {
        saveValuesIfNeeded()
        return settings
    }

    open fun setLLSDDirty() { llsdDirty = true }

    fun setLLSD(name: String, value: Any?) {
        saveValuesIfNeeded()
        settings[name] = value
        dirty = true
        if (name != SETTING_ASSETID) clearAssetId()
    }

    fun setValue(name: String, value: Any?) = setLLSD(name, value)

    fun getValue(name: String, default: Any? = null): Any? {
        saveValuesIfNeeded()
        return settings.getOrDefault(name, default)
    }

    fun getBlendFactor(): Double = blendedFactor

    fun update() {
        if (!dirty && !replaced) return
        updateSettings()
    }

    open fun validate(): Boolean {
        val validations = getValidationList()
        if (!settings.containsKey(SETTING_TYPE)) settings[SETTING_TYPE] = getSettingsType()
        saveValuesIfNeeded()
        val result = settingValidation(settings, validations)
        loadValuesFromLLSD()
        return result["success"] as? Boolean ?: false
    }

    fun cloneSettings(): LLSD {
        saveValuesIfNeeded()
        val clone = combineSDMaps(getSettings(), llsdEmptyMap())
        if (getFlags() != 0u) clone[SETTING_FLAGS] = getFlags()
        return clone
    }

    fun getHash(): Int {
        saveValuesIfNeeded()
        val filtered = settings.filter { it.key != SETTING_NAME && it.key != SETTING_ID && it.key != SETTING_HASH }
        return filtered.hashCode()
    }

    fun setAssetId(value: LLUUID) {
        assetId = value
        llsdDirty = true
    }

    fun clearAssetId() {
        assetId = LLUUID.NULL
        llsdDirty = true
    }

    open fun updateSettings() {
        dirty = false
        replaced = false
    }

    open fun loadValuesFromLLSD() {
        llsdDirty = false
        assetId = (settings[SETTING_ASSETID] as? String)?.let { LLUUID(it) } ?: LLUUID.NULL
        settingId = (settings[SETTING_ID] as? String)?.let { LLUUID(it) } ?: LLUUID.NULL
        settingName = settings[SETTING_NAME] as? String ?: ""
        settingFlags = (settings[SETTING_FLAGS] as? Number)?.toLong()?.toUInt() ?: 0u
    }

    open fun saveValuesToLLSD() {
        llsdDirty = false
        settings[SETTING_NAME] = settingName
        if (assetId.isNull()) {
            settings.remove(SETTING_ASSETID)
        } else {
            settings[SETTING_ASSETID] = assetId.value
        }
        settings[SETTING_FLAGS] = settingFlags.toLong()
    }

    fun saveValuesIfNeeded() {
        if (llsdDirty) saveValuesToLLSD()
    }

    protected fun setBlendFactor(bf: Double) { blendedFactor = bf }

    open fun replaceWith(other: SettingsBase) {
        replaceSettings(other)
        setBlendFactor(other.getBlendFactor())
    }

    private fun combineSDMaps(first: LLSD, other: LLSD): LLSD {
        val result = llsdEmptyMap()
        for ((k, v) in first) {
            result[k] = when (v) {
                is Map<*, *> -> @Suppress("UNCHECKED_CAST") combineSDMaps(v as LLSD, llsdEmptyMap())
                is List<*> -> v.toMutableList()
                else -> v
            }
        }
        for ((k, v) in other) {
            if (!result.containsKey(k)) {
                result[k] = when (v) {
                    is Map<*, *> -> @Suppress("UNCHECKED_CAST") combineSDMaps(v as LLSD, llsdEmptyMap())
                    is List<*> -> v.toMutableList()
                    else -> v
                }
            }
        }
        return result
    }

    protected fun interpolateSDMap(
        src: LLSD, other: LLSD,
        defaults: Map<String, DefaultParam>,
        mix: Double,
        skip: Set<String>,
        slerps: Set<String>
    ): LLSD {
        val result = llsdEmptyMap()
        for ((k, v) in src) {
            if (k in skip) continue
            val ov = other[k] ?: defaults[k]?.defaultValue
            if (ov == null) { result[k] = v; continue }
            result[k] = interpolateSDValue(k, v, ov, defaults, mix, skip, slerps)
        }
        if (src.containsKey(SETTING_FLAGS)) {
            val f1 = (src[SETTING_FLAGS] as? Number)?.toLong() ?: 0L
            val f2 = (other[SETTING_FLAGS] as? Number)?.toLong() ?: 0L
            result[SETTING_FLAGS] = f1 or f2
        }
        for ((k, v) in other) {
            if (k in skip || src.containsKey(k)) continue
            val dv = defaults[k]?.defaultValue
            if (dv != null) {
                result[k] = interpolateSDValue(k, dv, v, defaults, mix, skip, slerps)
            }
        }
        for ((k, v) in other) {
            if (k !in skip || !src.containsKey(k)) continue
            result[k] = v
        }
        return result
    }

    private fun interpolateSDValue(
        key: String, value: Any?, other: Any?,
        defaults: Map<String, DefaultParam>,
        mix: Double,
        skip: Set<String>,
        slerps: Set<String>
    ): Any? {
        if (value is Number && other is Number) {
            return value.toDouble() + (other.toDouble() - value.toDouble()) * mix
        }
        if (value is LLSD && other is LLSD) {
            return interpolateSDMap(value, other, defaults, mix, skip, slerps)
        }
        if (value is List<*> && other is List<*>) {
            if (key in slerps) {
                // no-op
            }
            val len = maxOf(value.size, other.size)
            val r = mutableListOf<Any?>()
            for (i in 0 until len) {
                r += interpolateSDValue(key, value.getOrNull(i), other.getOrNull(i), defaults, mix, skip, slerps)
            }
            return r
        }
        return if (mix > BREAK_POINT) other else value
    }
}

open class SettingsBlender(
    var target: SettingsBase?,
    var initial: SettingsBase?,
    var final: SettingsBase?
) {
    val onFinishedListeners: MutableList<(SettingsBlender) -> Unit> = mutableListOf()

    init {
        if (initial != null && target != null) target!!.replaceSettings(initial!!.getSettings())
        if (final == null) final = initial
    }

    open fun reset(newInitial: SettingsBase?, newFinal: SettingsBase?, span: Float) {
        initial = newInitial
        final = if (newFinal != null) newFinal else newInitial
        target?.replaceSettings(initial?.getSettings() ?: mutableMapOf())
    }

    open fun update(blendf: Double) {
        val res = setBlendFactor(blendf)
        target?.update()
    }

    open fun setBlendFactor(blendIn: Double): Double {
        var blendf = blendIn.toFloat()
        if (blendf >= 1.0f) triggerComplete()
        blendf = blendf.coerceIn(0f, 1f)
        if (target != null) {
            target!!.replaceSettings(initial ?: return blendf.toDouble())
            target!!.blend(final ?: initial!!, blendf.toDouble())
        }
        return blendf.toDouble()
    }

    open fun applyTimeDelta(timeDelta: Double): Boolean = false

    open fun switchTrack(trackNo: Int, position: Float) {}

    protected fun triggerComplete() {
        if (target != null && final != null) target!!.replaceSettings(final!!)
        target?.update()
        val self = this
        onFinishedListeners.forEach { it(self) }
    }
}

class SettingsBlenderTimeDelta(
    target: SettingsBase?,
    initial: SettingsBase?,
    final: SettingsBase?,
    blendSpan: Float
) : SettingsBlender(target, initial, final) {

    companion object {
        const val MIN_BLEND_DELTA: Double = Float.MIN_VALUE.toDouble()
    }

    var blendSpan: Float = blendSpan
    private var lastUpdate: Double = currentTimeSeconds()
    private var timeSpent: Double = 0.0
    private val timeStart: Double = currentTimeSeconds()
    private var lastBlendF: Double = -1.0

    private fun currentTimeSeconds(): Double = System.currentTimeMillis() / 1000.0

    fun setTimeSpent(t: Double) { timeSpent = t }

    override fun applyTimeDelta(timeDelta: Double): Boolean {
        timeSpent += timeDelta
        if (timeSpent > blendSpan) {
            triggerComplete()
            return false
        }
        val blendf = calculateBlend(timeSpent.toFloat(), blendSpan)
        if (abs(lastBlendF - blendf) < MIN_BLEND_DELTA) return false
        lastBlendF = blendf
        update(blendf)
        return true
    }

    protected fun calculateBlend(spanPos: Float, spanLen: Float): Double =
        if (spanLen == 0f) 0.0 else (spanPos % spanLen / spanLen).toDouble()
}
