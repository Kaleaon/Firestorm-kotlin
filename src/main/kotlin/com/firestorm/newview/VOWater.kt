package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llinventory.*

const val N_RES: UInt = 16u
const val WAVE_STEP: UByte = 8u

enum class WaterPartitionType {
    WATER, VOID_WATER
}

open class VOWater(id: LLUUID, localId: UInt, pCode: UInt) : ViewerObject(id, localId, pCode) {

    companion object {
        const val RENDER_TYPE_WATER = 0
        const val RENDER_TYPE_VOID_WATER = 1

        fun initClass() {}
        fun cleanupClass() {}
    }

    protected var isEdgePatch: Boolean = false
    protected var renderType: Int = RENDER_TYPE_WATER

    init {
        canSelect = false
    }

    override fun markDead() {
        super.markDead()
    }

    override fun isActive(): Boolean = false

    fun setPixelAreaAndAngle(agent: Any?) {
        appAngle = 50f
        pixelArea = 500f * 500f
    }

    fun updateTextures(): Unit {}

    fun idleUpdate(agent: Any?, time: Double): Unit {}

    fun createDrawable(pipeline: Any?): Any? = TODO("GPU: createDrawable water")

    fun updateGeometry(drawable: Drawable): Boolean = TODO("GPU: updateGeometry water quads")

    fun updateSpatialExtents(newMin: Vector4, newMax: Vector4): Unit = TODO("GPU: updateSpatialExtents")

    open fun getPartitionType(): WaterPartitionType =
        if (isEdgePatch) WaterPartitionType.VOID_WATER else WaterPartitionType.WATER

    fun setIsEdgePatch(edgePatch: Boolean) { isEdgePatch = edgePatch }
    fun getIsEdgePatch(): Boolean = isEdgePatch
}

class VOVoidWater(id: LLUUID, localId: UInt, pCode: UInt) : VOWater(id, localId, pCode) {

    init {
        renderType = RENDER_TYPE_VOID_WATER
    }

    override fun getPartitionType(): WaterPartitionType = WaterPartitionType.VOID_WATER
}
