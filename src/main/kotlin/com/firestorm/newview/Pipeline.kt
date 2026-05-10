package com.firestorm.newview

object Pipeline {

    var hasShaders: Boolean = false
    var hasWaterReflections: Boolean = false
    var hasShadows: Boolean = false
    var hasSSAO: Boolean = false
    var hasBloom: Boolean = false
    var useFarClip: Boolean = true

    var renderTypeMask: ULong = 0uL

    var lightingDetail: Int = 1
    var objectDetail: Float = 1f
    var terrainDetail: Int = 1

    fun renderTypeEnabled(type: Int): Boolean = (renderTypeMask shr type) and 1uL == 1uL

    fun toggleRenderType(type: Int) {
        renderTypeMask = renderTypeMask xor (1uL shl type)
    }

    fun enableRenderType(type: Int) {
        renderTypeMask = renderTypeMask or (1uL shl type)
    }

    fun disableRenderType(type: Int) {
        renderTypeMask = renderTypeMask and (1uL shl type).inv()
    }

    fun addObject(obj: ViewerObject) {}

    fun removeObject(obj: ViewerObject, killParticles: Boolean) {}

    fun markDirty(drawable: Drawable) {}

    fun updateGeom(maxTime: Float) {}

    fun renderGeom(viewerCamera: ViewerCamera) {}

    companion object {
        const val RENDER_TYPE_SKY = 1
        const val RENDER_TYPE_WATER = 2
        const val RENDER_TYPE_TERRAIN = 4
        const val RENDER_TYPE_SIMPLE = 8
        const val RENDER_TYPE_ALPHA = 16
        const val RENDER_TYPE_AVATAR = 32
        const val RENDER_TYPE_VOLUME = 64
        const val RENDER_TYPE_GLOW = 128
        const val RENDER_TYPE_PARTICLES = 256
        const val RENDER_TYPE_HUD = 512
    }
}
