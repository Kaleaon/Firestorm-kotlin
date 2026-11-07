package com.firestorm.newview

import com.firestorm.llappearance.Wearable
import com.firestorm.llcommon.LLUUID
import com.firestorm.llinventory.WearableType

object AvatarSelf : VOAvatar(LLUUID.NULL, 0u) {

    override var isSelf: Boolean = true

    val wearables: MutableMap<WearableType, MutableList<Wearable>> = mutableMapOf()

    fun getWearable(type: WearableType, index: Int = 0): Wearable? =
        wearables[type]?.getOrNull(index)

    fun getWearableCount(type: WearableType): Int = wearables[type]?.size ?: 0

    fun addWearable(type: WearableType, wearable: Wearable) {
        wearables.getOrPut(type) { mutableListOf() }.add(wearable)
    }

    fun removeWearable(type: WearableType, index: Int = 0) {
        wearables[type]?.removeAt(index)
    }

    fun isWearing(type: WearableType): Boolean = (wearables[type]?.isNotEmpty()) == true

    fun isWearingItem(itemId: LLUUID): Boolean =
        wearables.values.any { list -> list.any { it.itemId == itemId } }

    fun sendAppearanceMessage() {}

    fun requestLayerSetUpdate(type: WearableType) {}

    fun updateAvatarRenderComplexity() {}
}
