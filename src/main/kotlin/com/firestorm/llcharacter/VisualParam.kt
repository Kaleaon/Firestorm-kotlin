package com.firestorm.llcharacter

const val MAX_TRANSMITTED_VISUAL_PARAMS = 255

enum class Sex { FEMALE, MALE, BOTH }

enum class VisualParamGroup {
    TWEAKABLE,
    ANIMATABLE,
    TWEAKABLE_NO_TRANSMIT,
    TRANSMIT_NOT_TWEAKABLE;
}

enum class ParamLocation { UNKNOWN, AV_SELF, AV_OTHER, WEARABLE }

fun paramLocationName(loc: ParamLocation): String = when (loc) {
    ParamLocation.UNKNOWN  -> "unknown"
    ParamLocation.AV_SELF  -> "self"
    ParamLocation.AV_OTHER -> "other"
    ParamLocation.WEARABLE -> "wearable"
}

/**
 * LLVisualParamInfo — shared (per-type) descriptor for a visual morph parameter.
 * Populated from XML in the original C++; here we expose it as a plain data class.
 *
 * Translated from llvisualparam.h / llvisualparam.cpp.
 */
data class VisualParamInfo(
    var id: Int = -1,
    var name: String = "",
    var displayName: String = "",
    var minName: String = "Less",
    var maxName: String = "More",
    var group: VisualParamGroup = VisualParamGroup.TWEAKABLE,
    var minWeight: Float = 0f,
    var maxWeight: Float = 1f,
    var defaultWeight: Float = 0f,
    var sex: Sex = Sex.BOTH
) {
    /** Parse attributes from an XML node (stubbed — requires XML tree support). */
    fun parseXml(node: Any): Boolean = TODO("Wire to XML tree parser")
}

/**
 * LLVisualParam — base class for all avatar morphing parameters.
 * Subclasses must implement [apply].
 *
 * Key behaviours translated from llvisualparam.h / llvisualparam.cpp:
 *  - [setWeight] clamps to [minWeight]..[maxWeight] unless animating.
 *  - [setAnimationTarget] marks the param as animating toward [targetWeight].
 *  - [animate] interpolates toward the target by a fractional delta.
 *  - [stopAnimating] commits the target weight and clears the animating flag.
 *  - Chained params via [next] mirror every weight change down the chain.
 */
abstract class VisualParam(val id: Int) {

    var info: VisualParamInfo? = null
        private set

    // Weight state
    var curWeight: Float = 0f
        protected set
    var lastWeight: Float = 0f
    var targetWeight: Float = 0f
        protected set
    var isAnimating: Boolean = false
        protected set
    var isDummy: Boolean = false

    var paramLocation: ParamLocation = ParamLocation.UNKNOWN

    /** Linked next param in a shared chain (owned by caller; cleared on detach). */
    var next: VisualParam? = null
        private set

    // ── Info accessors ────────────────────────────────────────────────────────

    val name: String          get() = info?.name          ?: ""
    val displayName: String   get() = info?.displayName   ?: ""
    val maxDisplayName: String get() = info?.maxName      ?: ""
    val minDisplayName: String get() = info?.minName      ?: ""
    val group: VisualParamGroup get() = info?.group       ?: VisualParamGroup.TWEAKABLE
    val minWeight: Float      get() = info?.minWeight     ?: 0f
    val maxWeight: Float      get() = info?.maxWeight     ?: 1f
    val defaultWeight: Float  get() = info?.defaultWeight ?: 0f
    val sex: Sex              get() = info?.sex           ?: Sex.BOTH

    /** Effective weight: target when animating, current otherwise. */
    fun getWeight(): Float = if (isAnimating) targetWeight else curWeight

    val isTweakable: Boolean
        get() = group == VisualParamGroup.TWEAKABLE ||
                group == VisualParamGroup.TWEAKABLE_NO_TRANSMIT

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Attach [info] to this param and set the initial weight.
     * Returns false if [info].id < 0 (matches C++ setInfo).
     */
    open fun setInfo(info: VisualParamInfo): Boolean {
        if (info.id < 0) return false
        this.info = info
        setWeight(defaultWeight, uploadBake = false)
        return true
    }

    // ── Pure virtual ──────────────────────────────────────────────────────────

    /** Apply this param's current weight to the avatar mesh for [avatarSex]. */
    abstract fun apply(avatarSex: Sex)

    // ── Weight management ─────────────────────────────────────────────────────

    open fun setWeight(weight: Float, uploadBake: Boolean) {
        curWeight = when {
            isAnimating -> weight  // allow overshoot while animating
            info != null -> weight.coerceIn(minWeight, maxWeight)
            else -> weight
        }
        next?.setWeight(weight, uploadBake)
    }

    open fun setAnimationTarget(targetValue: Float, uploadBake: Boolean) {
        if (isDummy) {
            setWeight(targetValue, uploadBake)
            targetWeight = curWeight
            return
        }
        targetWeight = if (info != null && isTweakable)
            targetValue.coerceIn(minWeight, maxWeight)
        else
            targetValue
        isAnimating = true
        next?.setAnimationTarget(targetValue, uploadBake)
    }

    open fun animate(delta: Float, uploadBake: Boolean) {
        if (isAnimating) {
            val newWeight = (targetWeight - curWeight) * delta + curWeight
            setWeight(newWeight, uploadBake)
        }
    }

    open fun stopAnimating(uploadBake: Boolean) {
        if (isAnimating && isTweakable) {
            isAnimating = false
            setWeight(targetWeight, uploadBake)
        }
    }

    open fun setAnimating(animating: Boolean) {
        isAnimating = animating && !isDummy
    }

    // ── Param chain ───────────────────────────────────────────────────────────

    fun setNextParam(n: VisualParam) {
        check(next == null) { "next param already set" }
        next = n
    }

    fun clearNextParam() { next = null }

    // ── Driver-param hooks (no-op in base class) ──────────────────────────────

    open fun linkDrivenParams(mapper: (Int) -> VisualParam?, onlyCrossParams: Boolean): Boolean = true
    open fun resetDrivenParams() { /* no-op */ }

    fun setParamLocation(loc: ParamLocation) {
        if (paramLocation == ParamLocation.UNKNOWN || loc == ParamLocation.UNKNOWN) {
            paramLocation = loc
        }
        // If already set to a different non-UNKNOWN value, silently ignore (matches C++)
    }
}
