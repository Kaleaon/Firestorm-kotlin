package com.firestorm.newview

/**
 * Data holder for a beam-color preset: a hue range [startHue, endHue] in degrees
 * and a rotation speed multiplier.
 *
 * C++ heritage: lggBeamsColors (value type, not a singleton)
 */
data class LGGBeamColors(
    val startHue: Float = 0f,
    val endHue: Float = 360f,
    val rotateSpeed: Float = 1f
) {
    fun toLLSD(): MutableMap<String, Any?> = mutableMapOf(
        "startHue" to startHue,
        "endHue" to endHue,
        "rotateSpeed" to rotateSpeed
    )

    override fun toString(): String =
        "Start Hue ${startHue.toInt()}\nEnd Hue is ${endHue.toInt()}\nRotate Speed is ${rotateSpeed.toInt()}"

    companion object {
        fun fromLLSD(inputData: Map<String, Any?>): LGGBeamColors {
            val startHue = (inputData["startHue"] as? Number)?.toFloat() ?: 0f
            val endHue = (inputData["endHue"] as? Number)?.toFloat() ?: 360f
            val rotateSpeed = (inputData["rotateSpeed"] as? Number)?.toFloat() ?: 1f
            return LGGBeamColors(startHue, endHue, rotateSpeed)
        }
    }
}
