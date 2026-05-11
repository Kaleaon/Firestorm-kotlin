package com.firestorm.llappearance

open class VisualParam(
    var id: Int = 0,
    var name: String = "",
    var displayName: String = "",
    var minWeight: Float = 0f,
    var maxWeight: Float = 1f,
    var defaultWeight: Float = 0f,
) {
    enum class ParamLocation { LOC_UNKNOWN, LOC_AV_SELF, LOC_AV_OTHER, LOC_WEARABLE }

    var weight: Float = defaultWeight
        protected set

    protected var targetWeight: Float = defaultWeight
    protected var isAnimatingFlag: Boolean = false
    protected var isDummy: Boolean = false

    var paramLocation: ParamLocation = ParamLocation.LOC_UNKNOWN

    open fun setWeight(weight: Float, upload: Boolean) {
        this.weight = weight.coerceIn(minWeight, maxWeight)
        isAnimatingFlag = false
    }

    open fun setAnimationTarget(target: Float, upload: Boolean) {
        targetWeight = target.coerceIn(minWeight, maxWeight)
        isAnimatingFlag = !isDummy
    }

    open fun animate(delta: Float) {
        if (!isAnimatingFlag) return
        weight = if (delta > 0f) {
            (weight + delta).coerceAtMost(targetWeight)
        } else {
            (weight + delta).coerceAtLeast(targetWeight)
        }
        if (weight == targetWeight) isAnimatingFlag = false
    }

    open fun stopAnimating() {
        isAnimatingFlag = false
    }

    fun isAnimating(): Boolean = isAnimatingFlag

    fun getWeight(): Float = if (isAnimatingFlag) targetWeight else weight

    fun getDefaultWeight(): Float = defaultWeight
}
