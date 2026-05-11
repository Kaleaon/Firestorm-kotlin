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

    var sRenderDeferred: Boolean = false
    var sImpostorRender: Boolean = false
    var sRenderingHUDs: Boolean = false
    var sUnderWaterRender: Boolean = false
    var sReflectionRender: Boolean = false
    var sReflectionProbesEnabled: Boolean = false
    var RenderDepthOfField: Boolean = false
    var RenderAvatarCloth: Boolean = false
    var sRenderParticles: Boolean = true
    var sImpostorRenderAlphaDepthPass: Boolean = false

    var mTextureMatrixOps: Int = 0

    fun addObject(obj: ViewerObject) {}

    fun removeObject(obj: ViewerObject, killParticles: Boolean) {}

    fun markDirty(drawable: Drawable) {}

    fun updateGeom(maxTime: Float) {}

    fun renderGeom(viewerCamera: ViewerCamera) {}

    fun isWaterClip(): Boolean { TODO("GPU: isWaterClip") }
    fun enableLightsDynamic() { TODO("GPU: enableLightsDynamic") }
    fun enableLightsFullbright() { TODO("GPU: enableLightsFullbright") }
    fun bindDeferredShader(shader: GLSLShader) { TODO("GPU: bindDeferredShader") }
    fun bindDeferredShaderFast(shader: GLSLShader) { TODO("GPU: bindDeferredShaderFast") }
    fun unbindDeferredShader(shader: GLSLShader) { TODO("GPU: unbindDeferredShader") }
    fun bindReflectionProbes(shader: GLSLShader) { TODO("GPU: bindReflectionProbes") }
    fun unbindReflectionProbes(shader: GLSLShader) { TODO("GPU: unbindReflectionProbes") }
    fun setEnvMat(shader: GLSLShader) { TODO("GPU: setEnvMat") }
    fun beginRenderMap(type: Int): Iterator<DrawInfo> { TODO("GPU: beginRenderMap") }
    fun endRenderMap(type: Int): Iterator<DrawInfo> { TODO("GPU: endRenderMap") }
    fun hasRenderType(type: Int): Boolean = renderTypeEnabled(type)
    fun beginAlphaGroups(): Iterator<SpatialGroup> { TODO("GPU: beginAlphaGroups") }
    fun endAlphaGroups(): Iterator<SpatialGroup> { TODO("GPU: endAlphaGroups") }
    fun beginRiggedAlphaGroups(): Iterator<SpatialGroup> { TODO("GPU: beginRiggedAlphaGroups") }
    fun endRiggedAlphaGroups(): Iterator<SpatialGroup> { TODO("GPU: endRiggedAlphaGroups") }
    fun shadersLoaded(): Boolean { TODO("GPU: shadersLoaded") }

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
