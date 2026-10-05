package com.firestorm.llappearance

import com.firestorm.llinventory.WearableType

abstract class AvatarAppearance {
    var isSelf: Boolean = false

    val visualParams: MutableMap<Int, VisualParam> = mutableMapOf()
    val wearables: MutableMap<WearableType, MutableList<Wearable>> = mutableMapOf()

    fun getVisualParam(id: Int): VisualParam? = visualParams[id]

    fun addVisualParam(vp: VisualParam) {
        visualParams[vp.id] = vp
    }

    fun setVisualParamWeight(id: Int, weight: Float, upload: Boolean = false) {
        visualParams[id]?.setWeight(weight, upload)
    }

    fun getWearable(type: WearableType, index: Int = 0): Wearable? =
        wearables[type]?.getOrNull(index)

    fun addWearable(type: WearableType, wearable: Wearable) {
        wearables.getOrPut(type) { mutableListOf() }.add(wearable)
    }

    fun getWearableCount(type: WearableType): Int = wearables[type]?.size ?: 0

    // Returns (totalHeight, hipToFoot, hipToHead) derived from avatar visual param weights.
    // Param 33 drives height; 507/508 drive pelvis-to-foot ratio (approximation).
    fun getActualBodySize(): Triple<Float, Float, Float> {
        val heightParam = visualParams[33]?.weight ?: 0.5f
        val totalHeight = 1.4f + heightParam * 0.6f
        val hipToFoot = totalHeight * 0.48f
        val hipToHead = totalHeight - hipToFoot
        return Triple(totalHeight, hipToFoot, hipToHead)
    }

    abstract fun isValid(): Boolean

    abstract fun updateVisualParams()

    open fun onFirstTEMessageReceived() {}

    companion object {
        const val MAX_WEARABLES_PER_TYPE = 5
    }
}
