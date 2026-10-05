package com.firestorm.llappearance

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLSD
import com.firestorm.llinventory.WearableType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintStream

open class Wearable {
    var assetID: LLUUID = LLUUID.NULL
    var itemID: LLUUID = LLUUID.NULL
    var type: WearableType = WearableType.INVALID
    var name: String = ""
    var description: String = ""
    var version: Int = 0
    var definitionVersion: Int = 0

    val visualParams: MutableMap<Int, Float> = mutableMapOf()
    val textureEntries: MutableMap<Int, LLUUID> = mutableMapOf()

    private var savedParams: Map<Int, Float> = emptyMap()

    fun getVisualParam(id: Int): Float? = visualParams[id]

    fun setVisualParam(id: Int, weight: Float) {
        visualParams[id] = weight
    }

    fun getLocalTextureID(index: Int): LLUUID = textureEntries[index] ?: LLUUID.NULL

    fun setLocalTextureID(index: Int, id: LLUUID) {
        textureEntries[index] = id
    }

    fun exportToSD(): LLSD {
        val params = LLSD.LLSDMap(visualParams.entries.associate { (id, w) ->
            id.toString() to LLSD.LLSDReal(w.toDouble())
        })
        val textures = LLSD.LLSDMap(textureEntries.entries.associate { (idx, uuid) ->
            idx.toString() to LLSD.LLSDUUID(uuid)
        })
        return LLSD.LLSDMap(mapOf(
            "type" to LLSD.LLSDInteger(type.value),
            "parameters" to params,
            "textures" to textures,
        ))
    }

    fun importFromSD(sd: LLSD): Boolean {
        if (sd !is LLSD.LLSDMap) return false
        val map = sd.value

        val typeSD = map["type"] ?: return false
        val typeVal = (typeSD as? LLSD.LLSDInteger)?.value ?: return false
        type = WearableType.entries.firstOrNull { it.value == typeVal } ?: WearableType.INVALID

        (map["parameters"] as? LLSD.LLSDMap)?.value?.forEach { (key, v) ->
            key.toIntOrNull()?.let { id ->
                val w = when (v) {
                    is LLSD.LLSDReal -> v.value.toFloat()
                    is LLSD.LLSDInteger -> v.value.toFloat()
                    else -> null
                }
                if (w != null) visualParams[id] = w
            }
        }

        (map["textures"] as? LLSD.LLSDMap)?.value?.forEach { (key, v) ->
            key.toIntOrNull()?.let { idx ->
                (v as? LLSD.LLSDUUID)?.let { textureEntries[idx] = it.value }
            }
        }

        return true
    }

    fun exportFile(stream: OutputStream) {
        val out = PrintStream(stream)
        out.println("LLWearable version $definitionVersion")
        out.println(name)
        out.println(description)
        out.println("type ${type.value}")
        out.println("parameters ${visualParams.size}")
        visualParams.forEach { (id, w) -> out.println("$id $w") }
        out.println("textures ${textureEntries.size}")
        textureEntries.forEach { (idx, uuid) -> out.println("$idx $uuid") }
        out.flush()
    }

    fun importFile(stream: InputStream): Boolean {
        val reader = BufferedReader(InputStreamReader(stream))
        fun nextNonEmpty(): String? {
            var line: String?
            do { line = reader.readLine() } while (line != null && line.isBlank())
            return line
        }

        val header = nextNonEmpty() ?: return false
        val versionMatch = Regex("LLWearable version (\\d+)").find(header) ?: return false
        definitionVersion = versionMatch.groupValues[1].toInt()

        name = reader.readLine() ?: ""
        description = reader.readLine() ?: ""

        val typeLine = nextNonEmpty() ?: return false
        val typeMatch = Regex("type (-?\\d+)").find(typeLine) ?: return false
        val typeVal = typeMatch.groupValues[1].toInt()
        type = WearableType.entries.firstOrNull { it.value == typeVal } ?: WearableType.INVALID

        val paramHeader = nextNonEmpty() ?: return false
        val numParams = Regex("parameters (\\d+)").find(paramHeader)?.groupValues?.get(1)?.toInt() ?: return false
        repeat(numParams) {
            val line = nextNonEmpty() ?: return false
            val parts = line.trim().split(" ")
            if (parts.size >= 2) {
                parts[0].toIntOrNull()?.let { id ->
                    parts[1].toFloatOrNull()?.let { w -> visualParams[id] = w }
                }
            }
        }

        val texHeader = nextNonEmpty() ?: return false
        val numTex = Regex("textures (\\d+)").find(texHeader)?.groupValues?.get(1)?.toInt() ?: return false
        repeat(numTex) {
            val line = nextNonEmpty() ?: return false
            val parts = line.trim().split(" ")
            if (parts.size >= 2) {
                parts[0].toIntOrNull()?.let { idx ->
                    runCatching { LLUUID(java.util.UUID.fromString(parts[1])) }.getOrNull()
                        ?.let { textureEntries[idx] = it }
                }
            }
        }

        return true
    }

    fun saveValues() {
        savedParams = visualParams.toMap()
    }

    fun revertValues() {
        visualParams.clear()
        visualParams.putAll(savedParams)
    }
}
