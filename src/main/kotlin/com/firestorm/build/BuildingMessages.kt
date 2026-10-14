package com.firestorm.build

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLUUID
import kotlin.math.min

enum class PrimitiveShapeType(val value: Int) {
    BOX(0),
    SPHERE(1),
    CYLINDER(2),
    PRISM(3),
    TORUS(4),
    TUBE(5),
    RING(6),
    SCULPT(7),
    MESH(8);

    companion object {
        fun fromInt(value: Int): PrimitiveShapeType = entries.find { it.value == value } ?: BOX
    }
}

enum class BuildingCommandType {
    REZ_PRIMITIVE,
    UPDATE_TRANSFORM,
    UPDATE_SHAPE_PARAMS,
    APPLY_TEXTURE,
    PICK_TEXTURE
}

data class Vector3Data(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun toLLSDArray(): LLSD = LLSD.array(
        LLSD.real(x.toDouble()),
        LLSD.real(y.toDouble()),
        LLSD.real(z.toDouble())
    )

    companion object {
        fun fromLLSDArray(arr: LLSD?): Vector3Data {
            if (arr == null || arr.size() < 3) return Vector3Data()
            return Vector3Data(
                arr[0].asReal().toFloat(),
                arr[1].asReal().toFloat(),
                arr[2].asReal().toFloat()
            )
        }
    }
}

data class QuaternionData(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    fun toLLSDArray(): LLSD = LLSD.array(
        LLSD.real(x.toDouble()),
        LLSD.real(y.toDouble()),
        LLSD.real(z.toDouble()),
        LLSD.real(w.toDouble())
    )

    companion object {
        fun fromLLSDArray(arr: LLSD?): QuaternionData {
            if (arr == null || arr.size() < 4) return QuaternionData()
            return QuaternionData(
                arr[0].asReal().toFloat(),
                arr[1].asReal().toFloat(),
                arr[2].asReal().toFloat(),
                arr[3].asReal().toFloat()
            )
        }
    }
}

data class Vector2Data(var x: Float = 0f, var y: Float = 0f) {
    fun toLLSDArray(): LLSD = LLSD.array(
        LLSD.real(x.toDouble()),
        LLSD.real(y.toDouble())
    )

    companion object {
        fun fromLLSDArray(arr: LLSD?): Vector2Data {
            if (arr == null || arr.size() < 2) return Vector2Data()
            return Vector2Data(
                arr[0].asReal().toFloat(),
                arr[1].asReal().toFloat()
            )
        }
    }
}

data class ShapeParameters(
    var cut: Vector2Data = Vector2Data(0f, 1f),
    var twist: Vector2Data = Vector2Data(0f, 0f),
    var hollow: Float = 0f,
    var taper: Vector2Data = Vector2Data(0f, 0f),
    var shear: Vector2Data = Vector2Data(0f, 0f),
    var pathScale: Vector2Data = Vector2Data(1f, 1f)
) {
    fun toLLSDMap(): LLSD = LLSD.map(
        "cut" to cut.toLLSDArray(),
        "twist" to twist.toLLSDArray(),
        "hollow" to LLSD.real(hollow.toDouble()),
        "taper" to taper.toLLSDArray(),
        "shear" to shear.toLLSDArray(),
        "path_scale" to pathScale.toLLSDArray()
    )

    companion object {
        fun fromLLSDMap(map: LLSD?): ShapeParameters {
            if (map == null || !map.isDefined()) return ShapeParameters()
            return ShapeParameters(
                cut = Vector2Data.fromLLSDArray(map["cut"]),
                twist = Vector2Data.fromLLSDArray(map["twist"]),
                hollow = map["hollow"].asReal().toFloat(),
                taper = Vector2Data.fromLLSDArray(map["taper"]),
                shear = Vector2Data.fromLLSDArray(map["shear"]),
                pathScale = Vector2Data.fromLLSDArray(map["path_scale"])
            )
        }
    }
}

data class FaceTextureEntry(
    var faceIndex: Int = -1,
    var textureId: String = LLUUID.NULL.toString(),
    var rgbaColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    var scale: Vector2Data = Vector2Data(1f, 1f),
    var offset: Vector2Data = Vector2Data(0f, 0f),
    var rotation: Float = 0f,
    var glow: Float = 0f,
    var fullbright: Boolean = false
) {
    fun toLLSDMap(): LLSD {
        val colorArr = LLSD.array(
            LLSD.real(rgbaColor.getOrElse(0) { 1f }.toDouble()),
            LLSD.real(rgbaColor.getOrElse(1) { 1f }.toDouble()),
            LLSD.real(rgbaColor.getOrElse(2) { 1f }.toDouble()),
            LLSD.real(rgbaColor.getOrElse(3) { 1f }.toDouble())
        )
        return LLSD.map(
            "face_index" to LLSD.integer(faceIndex),
            "texture_id" to LLSD.string(textureId),
            "color" to colorArr,
            "scale" to scale.toLLSDArray(),
            "offset" to offset.toLLSDArray(),
            "rotation" to LLSD.real(rotation.toDouble()),
            "glow" to LLSD.real(glow.toDouble()),
            "fullbright" to LLSD.bool(fullbright)
        )
    }

    companion object {
        fun fromLLSDMap(map: LLSD?): FaceTextureEntry {
            if (map == null || !map.isDefined()) return FaceTextureEntry()
            val colArr = map["color"]
            val color = FloatArray(4) { 1f }
            if (colArr.size() > 0) {
                for (i in 0 until min(4, colArr.size())) {
                    color[i] = colArr[i].asReal().toFloat()
                }
            }

            val faceIdx = if (map.has("face_index")) map["face_index"].asInt() else -1
            val texVal = if (map.has("texture_id")) map["texture_id"].asString() else LLUUID.NULL.toString()

            return FaceTextureEntry(
                faceIndex = faceIdx,
                textureId = texVal,
                rgbaColor = color,
                scale = Vector2Data.fromLLSDArray(map["scale"]),
                offset = Vector2Data.fromLLSDArray(map["offset"]),
                rotation = map["rotation"].asReal().toFloat(),
                glow = map["glow"].asReal().toFloat(),
                fullbright = map["fullbright"].asBoolean()
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceTextureEntry) return false
        if (faceIndex != other.faceIndex) return false
        if (textureId != other.textureId) return false
        if (!rgbaColor.contentEquals(other.rgbaColor)) return false
        if (scale != other.scale) return false
        if (offset != other.offset) return false
        if (rotation != other.rotation) return false
        if (glow != other.glow) return false
        if (fullbright != other.fullbright) return false
        return true
    }

    override fun hashCode(): Int {
        var result = faceIndex
        result = 31 * result + textureId.hashCode()
        result = 31 * result + rgbaColor.contentHashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + offset.hashCode()
        result = 31 * result + rotation.hashCode()
        result = 31 * result + glow.hashCode()
        result = 31 * result + fullbright.hashCode()
        return result
    }
}

data class PrimitiveState(
    var localId: Long = 0,
    var objectId: String = LLUUID.generate().toString(),
    var shape: PrimitiveShapeType = PrimitiveShapeType.BOX,
    var position: Vector3Data = Vector3Data(128f, 128f, 25f),
    var rotation: QuaternionData = QuaternionData(0f, 0f, 0f, 1f),
    var scale: Vector3Data = Vector3Data(0.5f, 0.5f, 0.5f),
    var params: ShapeParameters = ShapeParameters(),
    var textures: MutableMap<Int, FaceTextureEntry> = mutableMapOf()
) {
    fun toLLSDMap(): LLSD {
        val texPairs = textures.map { (key, value) ->
            key.toString() to value.toLLSDMap()
        }.toTypedArray()

        return LLSD.map(
            "local_id" to LLSD.integer(localId.toInt()),
            "object_id" to LLSD.string(objectId),
            "shape" to LLSD.integer(shape.value),
            "position" to position.toLLSDArray(),
            "rotation" to rotation.toLLSDArray(),
            "scale" to scale.toLLSDArray(),
            "params" to params.toLLSDMap(),
            "textures" to LLSD.map(*texPairs)
        )
    }

    companion object {
        fun fromLLSDMap(map: LLSD?): PrimitiveState {
            if (map == null || !map.isDefined()) return PrimitiveState()
            val state = PrimitiveState(
                localId = map["local_id"].asInt().toLong(),
                objectId = if (map.has("object_id")) map["object_id"].asString() else LLUUID.generate().toString(),
                shape = PrimitiveShapeType.fromInt(map["shape"].asInt()),
                position = Vector3Data.fromLLSDArray(map["position"]),
                rotation = QuaternionData.fromLLSDArray(map["rotation"]),
                scale = Vector3Data.fromLLSDArray(map["scale"]),
                params = ShapeParameters.fromLLSDMap(map["params"])
            )

            val texMap = map["textures"]
            if (texMap is LLSD.LLSDMap) {
                state.textures.clear()
                for ((key, value) in texMap.value) {
                    val faceIdx = key.toIntOrNull()
                    if (faceIdx != null) {
                        state.textures[faceIdx] = FaceTextureEntry.fromLLSDMap(value)
                    }
                }
            }

            return state
        }
    }
}

data class BuildingCommand(
    var commandType: BuildingCommandType = BuildingCommandType.REZ_PRIMITIVE,
    var objectId: String = LLUUID.NULL.toString(),
    var payload: LLSD = LLSD.map()
) {
    fun toLLSDMap(): LLSD = LLSD.map(
        "command" to LLSD.string(commandType.name),
        "object_id" to LLSD.string(objectId),
        "payload" to payload
    )

    companion object {
        fun fromLLSDMap(map: LLSD?): BuildingCommand {
            if (map == null || !map.isDefined()) return BuildingCommand()
            val cmdStr = map["command"].asString()
            val type = try {
                BuildingCommandType.valueOf(cmdStr)
            } catch (e: Exception) {
                BuildingCommandType.REZ_PRIMITIVE
            }

            val objId = if (map.has("object_id")) map["object_id"].asString() else LLUUID.NULL.toString()

            return BuildingCommand(
                commandType = type,
                objectId = objId,
                payload = map["payload"]
            )
        }
    }
}
