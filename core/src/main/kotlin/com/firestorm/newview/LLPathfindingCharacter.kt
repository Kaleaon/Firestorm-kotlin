package com.firestorm.newview

class LLPathfindingCharacter : LLPathfindingObject {

    val cpuTime: Float
    val isHorizontal: Boolean
    val length: Float
    val radius: Float

    constructor(uuid: String, characterData: Map<String, Any>) : super(uuid, characterData) {
        cpuTime     = parseFloat(characterData, CHARACTER_CPU_TIME_FIELD)
        isHorizontal = parseBool(characterData, CHARACTER_HORIZONTAL_FIELD)
        length       = parseFloat(characterData, CHARACTER_LENGTH_FIELD)
        radius       = parseFloat(characterData, CHARACTER_RADIUS_FIELD)
    }

    constructor(other: LLPathfindingCharacter) : super(other) {
        cpuTime      = other.cpuTime
        isHorizontal = other.isHorizontal
        length       = other.length
        radius       = other.radius
    }

    private companion object {
        const val CHARACTER_CPU_TIME_FIELD   = "cpu_time"
        const val CHARACTER_HORIZONTAL_FIELD = "horizontal"
        const val CHARACTER_LENGTH_FIELD     = "length"
        const val CHARACTER_RADIUS_FIELD     = "radius"

        fun parseFloat(data: Map<String, Any>, key: String): Float =
            (requireNotNull(data[key]) { "Missing field: $key" } as Number).toFloat()

        fun parseBool(data: Map<String, Any>, key: String): Boolean =
            requireNotNull(data[key]) { "Missing field: $key" } as Boolean
    }
}
