package com.firestorm.newview

import java.util.UUID

// TEX_NUM_INDICES mirrors LLAvatarAppearanceDefines::TEX_NUM_INDICES.
// The value (56) comes from the SL avatar texture layer definition table.
private const val TEX_NUM_INDICES = 56

class Appearance {

    val paramMap: MutableMap<Int, Float> = mutableMapOf()
    val textures: Array<UUID> = Array(TEX_NUM_INDICES) { nilUUID() }

    fun addParam(id: Int, value: Float) {
        paramMap[id] = value
    }

    fun getParam(id: Int, defVal: Float): Float = paramMap.getOrDefault(id, defVal)

    fun addTexture(te: Int, uuid: UUID) {
        if (te < TEX_NUM_INDICES) textures[te] = uuid
    }

    fun getTexture(te: Int): UUID = if (te < TEX_NUM_INDICES) textures[te] else nilUUID()

    fun clear() {
        paramMap.clear()
        for (i in textures.indices) textures[i] = nilUUID()
    }

    companion object {
        // Represents LLUUID::null — all-zero UUID used as a sentinel.
        fun nilUUID(): UUID = UUID(0L, 0L)
    }
}
