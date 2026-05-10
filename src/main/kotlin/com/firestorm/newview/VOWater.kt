package com.firestorm.newview

const val N_RES: UInt = 16u
const val WAVE_STEP: UByte = 8u

open class VOWater(
    id: String,
    pCode: UByte,
    region: ViewerRegion?,
) : StaticViewerObject(id, pCode, region) {

    companion object {
        const val VERTEX_DATA_MASK_WATER: UInt =
            (1u shl 0) or   // TYPE_VERTEX
            (1u shl 1) or   // TYPE_NORMAL
            (1u shl 3)      // TYPE_TEXCOORD0

        fun initClass() {}
        fun cleanupClass() {}
    }

    var isEdgePatch: Boolean = false
        private set

    protected var renderType: Int = 0   // LLPipeline.RENDER_TYPE_WATER

    init {
        canSelect = false
        // Aurora-sim compatible: use actual region width rather than hardcoded 256
        TODO("APR: setScale(Vector3(region.getWidth(), region.getWidth(), 0f))")
    }

    open fun markDead() {
        TODO("GPU: LLViewerObject.markDead()")
    }

    open fun isActive(): Boolean = false

    fun setPixelAreaAndAngle() {
        appAngle = 50f
        pixelArea = 500f * 500f
    }

    open fun updateTextures() {}

    open fun idleUpdate(time: Double) {}

    open fun createDrawable(pipeline: Any?): Any? {
        TODO("GPU: pipeline.allocDrawable(this); mDrawable.setLit(false); mDrawable.setRenderType(renderType); attach pool + face")
    }

    open fun updateGeometry(drawable: Any?): Boolean {
        // Water surface is a regular grid of quads.
        // Resolution is scaled by the region size and the RenderTransparentWater setting.
        // Each cell: 4 vertices, 6 indices (2 triangles).
        TODO("GPU: build water quad mesh; fill vertex/normal/texcoord/index buffers; unmapBuffer(); movePartition(); sCompiles++")
    }

    open fun updateSpatialExtents(newMin: FloatArray, newMax: FloatArray) {
        TODO("GPU: compute AABB from positionAgent ± scale*0.5; setPositionGroup(centre)")
    }

    open fun updateTextures() {}

    open fun getPartitionType(): Int =
        if (isEdgePatch) PARTITION_VOIDWATER else PARTITION_WATER

    fun setIsEdgePatch(edgePatch: Boolean) {
        isEdgePatch = edgePatch
    }

    var appAngle: Float = 0f
    var pixelArea: Float = 0f
    var canSelect: Boolean = false

    private companion object {
        const val PARTITION_WATER = 0
        const val PARTITION_VOIDWATER = 1
    }
}

class VOVoidWater(
    id: String,
    pCode: UByte,
    region: ViewerRegion?,
) : VOWater(id, pCode, region) {

    init {
        renderType = RENDER_TYPE_VOIDWATER
    }

    override fun getPartitionType(): Int = PARTITION_VOIDWATER

    private companion object {
        const val RENDER_TYPE_VOIDWATER = 1
        const val PARTITION_VOIDWATER = 1
    }
}

// Spatial partition types — mirrors LLWaterPartition / LLVoidWaterPartition
class WaterPartition(region: ViewerRegion?) {
    var infiniteFarClip: Boolean = true
    var drawableType: Int = 0   // RENDER_TYPE_WATER
    var partitionType: Int = 0  // PARTITION_WATER

    init {
        TODO("GPU: LLSpatialPartition(0, false, regionp)")
    }
}

class VoidWaterPartition(region: ViewerRegion?) : WaterPartition(region) {
    var occlusionEnabled: Boolean = false

    init {
        drawableType = 1    // RENDER_TYPE_VOIDWATER
        partitionType = 1   // PARTITION_VOIDWATER
        occlusionEnabled = false
    }
}
