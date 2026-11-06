package com.firestorm.newview

data class LGGBeamShaderColors(
    val startHue: Float = 0.0f,
    val endHue: Float = 360.0f,
    val rotateSpeed: Float = 1.0f
) {
    fun toMap(): Map<String, Float> = mapOf(
        "startHue" to startHue,
        "endHue" to endHue,
        "rotateSpeed" to rotateSpeed
    )

    override fun toString(): String =
        "Start Hue ${startHue.toInt()}\nEnd Hue is ${endHue.toInt()}\nRotate Speed is ${rotateSpeed.toInt()}"

    companion object {
        fun fromMap(data: Map<String, Any?>): LGGBeamShaderColors {
            val startHue   = (data["startHue"]    as? Number)?.toFloat() ?: 0.0f
            val endHue     = (data["endHue"]      as? Number)?.toFloat() ?: 360.0f
            val rotateSpeed = (data["rotateSpeed"] as? Number)?.toFloat() ?: 1.0f
            return LGGBeamShaderColors(startHue, endHue, rotateSpeed)
        }
    }
}
