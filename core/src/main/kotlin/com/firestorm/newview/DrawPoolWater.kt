package com.firestorm.newview

class DrawPoolWater : FacePool(DrawPool.PoolType.WATER.value.toUInt()) {

    companion object {
        var skipScreenCopy: Boolean = false
        var needsReflectionUpdate: Boolean = true
        var needsDistortionUpdate: Boolean = true
        var waterFogEnd: Float = 0f

        const val VERTEX_DATA_MASK: UInt =
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_NORMAL or
            VertexBufferFlags.MAP_TEXCOORD0
    }

    private val waterImagep: Array<ViewerTexture?> = arrayOfNulls(2)
    private val waterNormp: Array<ViewerTexture?> = arrayOfNulls(2)
    private var opaqueWaterImagep: ViewerTexture? = null

    // Cached to avoid per-frame settings lookup — updated via settings signal.
    private var renderWaterMipNormal: Boolean = false

    init {
        onRenderWaterMipNormalChanged()
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    fun setTransparentTextures(transparentTextureId: String, nextTransparentTextureId: String) {
        System.err.println("DrawPoolWater: setTransparentTextures not yet implemented")
    }

    fun setOpaqueTexture(opaqueTextureId: String) {
        System.err.println("DrawPoolWater: setOpaqueTexture not yet implemented")
    }

    fun setNormalMaps(normalMapId: String, nextNormalMapId: String) {
        System.err.println("DrawPoolWater: setNormalMaps not yet implemented")
    }

    override fun prerender() {
        // no-op
    }

    override fun getNumPostDeferredPasses(): Int {
        return 0
    }

    override fun beginPostDeferredPass(pass: Int) {
        // no-op
    }

    override fun renderPostDeferred(pass: Int) {
        // no-op
    }

    fun pushWaterPlanes(pass: Int) {
        // no-op
    }

    override fun getDebugTexture(): ViewerTexture? {
        return null
    }

    fun getDebugColor(): FloatArray = floatArrayOf(0f, 1f, 1f)

    private fun onRenderWaterMipNormalChanged() {
        System.err.println("DrawPoolWater: onRenderWaterMipNormalChanged not yet implemented")
    }

    private fun renderOpaqueLegacyWater() {
        // no-op
    }
}
