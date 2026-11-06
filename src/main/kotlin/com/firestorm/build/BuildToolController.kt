package com.firestorm.build

import com.firestorm.llcommon.LLSDSerialize
import com.firestorm.llcommon.LLUUID
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

class BuildToolController {
    private val activePrimitives = mutableMapOf<String, PrimitiveState>()
    private var selectedObjectId: String? = null
    private var selectedFaceIndex: Int = -1
    private var selectedInventoryTextureUuid: String = LLUUID.NULL.toString()

    fun getSelectedObjectId(): String? = selectedObjectId
    fun getSelectedFaceIndex(): Int = selectedFaceIndex
    fun getSelectedInventoryTextureUuid(): String = selectedInventoryTextureUuid

    fun getSelectedPrimitive(): PrimitiveState? {
        val id = selectedObjectId ?: return null
        return activePrimitives[id]
    }

    fun getPrimitive(objectId: String?): PrimitiveState? {
        if (objectId == null) return null
        return activePrimitives[objectId]
    }

    fun selectPrimitive(objectId: String?) {
        selectedObjectId = objectId
    }

    fun selectFace(faceIndex: Int) {
        selectedFaceIndex = faceIndex
    }

    fun rezPrimitive(
        shape: PrimitiveShapeType,
        position: Vector3Data,
        rotation: QuaternionData,
        scale: Vector3Data
    ): PrimitiveState {
        val state = PrimitiveState(
            localId = Random.nextLong(1000, 999999),
            objectId = LLUUID.generate().toString(),
            shape = shape,
            position = position,
            rotation = rotation,
            scale = scale,
            params = ShapeParameters()
        )

        val defaultFace = FaceTextureEntry(
            faceIndex = -1,
            textureId = LLUUID.NULL.toString(),
            rgbaColor = floatArrayOf(1f, 1f, 1f, 1f),
            scale = Vector2Data(1f, 1f),
            offset = Vector2Data(0f, 0f),
            rotation = 0f,
            glow = 0f,
            fullbright = false
        )
        state.textures[-1] = defaultFace

        activePrimitives[state.objectId] = state
        selectedObjectId = state.objectId
        return state
    }

    fun createRezCommand(
        shape: PrimitiveShapeType,
        position: Vector3Data,
        rotation: QuaternionData,
        scale: Vector3Data
    ): BuildingCommand {
        val state = rezPrimitive(shape, position, rotation, scale)
        return BuildingCommand(
            commandType = BuildingCommandType.REZ_PRIMITIVE,
            objectId = state.objectId,
            payload = state.toLLSDMap()
        )
    }

    fun updateTransform(
        objectId: String,
        position: Vector3Data,
        rotation: QuaternionData,
        scale: Vector3Data
    ): PrimitiveState? {
        val state = getPrimitive(objectId) ?: return null

        state.position = position
        state.rotation = rotation
        state.scale = Vector3Data(
            max(0.01f, scale.x),
            max(0.01f, scale.y),
            max(0.01f, scale.z)
        )

        return state
    }

    fun handleGizmoEvent(
        objectId: String,
        gizmoAxis: String?,
        gizmoMode: String?,
        deltaAmount: Float
    ): PrimitiveState? {
        val state = getPrimitive(objectId) ?: return null

        val pos = state.position.copy()
        var rot = state.rotation.copy()
        val scale = state.scale.copy()

        val mode = (gizmoMode ?: "").lowercase()
        val axis = (gizmoAxis ?: "").lowercase()

        if (mode == "move" || mode == "translate") {
            when (axis) {
                "x" -> pos.x += deltaAmount
                "y" -> pos.y += deltaAmount
                "z" -> pos.z += deltaAmount
            }
        } else if (mode == "scale") {
            when (axis) {
                "x" -> scale.x = max(0.01f, scale.x + deltaAmount)
                "y" -> scale.y = max(0.01f, scale.y + deltaAmount)
                "z" -> scale.z = max(0.01f, scale.z + deltaAmount)
                "all", "uniform" -> {
                    scale.x = max(0.01f, scale.x + deltaAmount)
                    scale.y = max(0.01f, scale.y + deltaAmount)
                    scale.z = max(0.01f, scale.z + deltaAmount)
                }
            }
        } else if (mode == "rotate") {
            val radHalf = (deltaAmount * PI.toFloat() / 180f) / 2f
            val s = sin(radHalf)
            val c = cos(radHalf)

            val deltaRot = when (axis) {
                "x" -> QuaternionData(s, 0f, 0f, c)
                "y" -> QuaternionData(0f, s, 0f, c)
                "z" -> QuaternionData(0f, 0f, s, c)
                else -> QuaternionData(0f, 0f, 0f, 1f)
            }

            rot = multiplyQuaternions(rot, deltaRot)
        }

        return updateTransform(objectId, pos, rot, scale)
    }

    fun updateShapeParameters(objectId: String, paramsObj: ShapeParameters?): PrimitiveState? {
        val state = getPrimitive(objectId) ?: return null
        if (paramsObj != null) {
            paramsObj.hollow = max(0f, min(0.95f, paramsObj.hollow))
            state.params = paramsObj
        }
        return state
    }

    fun pickTextureFromInventory(textureUuid: String?, @Suppress("UNUSED_PARAMETER") textureName: String? = null) {
        val uuid = if (textureUuid.isNullOrEmpty()) LLUUID.NULL.toString() else textureUuid
        selectedInventoryTextureUuid = uuid
    }

    fun applyFaceTexture(objectId: String, faceIndex: Int, entry: FaceTextureEntry?): FaceTextureEntry? {
        val state = getPrimitive(objectId) ?: return null
        if (entry == null) return null

        validateAndSanitizeMaterial(entry)
        entry.faceIndex = faceIndex
        state.textures[faceIndex] = entry

        return entry
    }

    fun calculateTransformUV(
        inputUv: Vector2Data,
        scale: Vector2Data,
        offset: Vector2Data,
        rotationRad: Float
    ): Vector2Data {
        val sU = max(0.001f, scale.x)
        val sV = max(0.001f, scale.y)

        val oU = max(-1f, min(1f, offset.x))
        val oV = max(-1f, min(1f, offset.y))

        val scaledU = (inputUv.x * sU) + oU
        val scaledV = (inputUv.y * sV) + oV

        val cU = scaledU - 0.5f
        val cV = scaledV - 0.5f

        val c = cos(rotationRad)
        val s = sin(rotationRad)

        val rotU = (cU * c) - (cV * s) + 0.5f
        val rotV = (cU * s) + (cV * c) + 0.5f

        return Vector2Data(rotU, rotV)
    }

    fun validateMaterialParams(entry: FaceTextureEntry?): Boolean {
        if (entry == null) return false

        if (entry.glow < 0f || entry.glow > 1f) return false
        if (entry.scale.x <= 0f || entry.scale.y <= 0f) return false
        if (entry.offset.x < -1f || entry.offset.x > 1f) return false
        if (entry.offset.y < -1f || entry.offset.y > 1f) return false

        for (channel in entry.rgbaColor) {
            if (channel < 0f || channel > 1f) return false
        }

        if (entry.textureId.isEmpty()) return false

        return true
    }

    fun validateAndSanitizeMaterial(entry: FaceTextureEntry?) {
        if (entry == null) return

        entry.glow = max(0f, min(1f, entry.glow))
        entry.scale = Vector2Data(max(0.001f, entry.scale.x), max(0.001f, entry.scale.y))
        entry.offset = Vector2Data(max(-1f, min(1f, entry.offset.x)), max(-1f, min(1f, entry.offset.y)))

        if (entry.rgbaColor.size >= 4) {
            for (i in 0 until 4) {
                entry.rgbaColor[i] = max(0f, min(1f, entry.rgbaColor[i]))
            }
        } else {
            entry.rgbaColor = floatArrayOf(1f, 1f, 1f, 1f)
        }

        if (entry.textureId.isEmpty()) {
            entry.textureId = LLUUID.NULL.toString()
        }
    }

    fun serializeCommandToBinary(command: BuildingCommand): ByteArray {
        val llsdMap = command.toLLSDMap()
        return LLSDSerialize.toBinary(llsdMap)
    }

    fun serializeCommandToNotation(command: BuildingCommand): String {
        val llsdMap = command.toLLSDMap()
        return LLSDSerialize.toNotation(llsdMap)
    }

    fun deserializeCommandFromBinary(binaryData: ByteArray): BuildingCommand {
        val llsdMap = LLSDSerialize.fromBinary(binaryData)
        return BuildingCommand.fromLLSDMap(llsdMap)
    }

    fun deserializeCommandFromNotation(notationData: String): BuildingCommand {
        val llsdMap = LLSDSerialize.fromNotation(notationData)
        return BuildingCommand.fromLLSDMap(llsdMap)
    }

    private fun multiplyQuaternions(q1: QuaternionData, q2: QuaternionData): QuaternionData {
        val w1 = q1.w; val x1 = q1.x; val y1 = q1.y; val z1 = q1.z
        val w2 = q2.w; val x2 = q2.x; val y2 = q2.y; val z2 = q2.z

        return QuaternionData(
            x = w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2,
            y = w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2,
            z = w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2,
            w = w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2
        )
    }
}
