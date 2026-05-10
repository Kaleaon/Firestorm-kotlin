package com.firestorm.newview

class DrawPoolTree(private val texturep: ViewerTexture) : FacePool(DrawPool.PoolType.TREE.value.toUInt()) {

    companion object {
        var sDiffTex: Int = 0
        private var shader: GLSLShader? = null

        const val VERTEX_DATA_MASK: UInt = (
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_NORMAL or
            VertexBufferFlags.MAP_COLOR or
            VertexBufferFlags.MAP_TEXCOORD0
        )
    }

    init {
        TODO("GPU: texturep.setAddressMode(TAM_WRAP)")
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun getNumDeferredPasses(): Int = 1

    override fun beginDeferredPass(pass: Int) {
        TODO("GPU: shader = gDeferredTreeProgram; shader.bind(); shader.setMinimumAlpha(0.5f)")
    }

    override fun renderDeferred(pass: Int) {
        if (drawFace.isEmpty()) return
        TODO("GPU: if sRenderTextures: bindFast(texturep) else bindFast(sDefaultDiffuseImagep); " +
             "texturep.addTextureStats(1024f * 1024f); " +
             "for each face: get vertexBuffer; if non-null: " +
             "  get region renderMatrix; applyModelMatrix(matrix); " +
             "  buff.setBuffer(); buff.drawRange(TRIANGLES, 0, numVerts-1, numIndices, 0)")
    }

    override fun endDeferredPass(pass: Int) {
        TODO("GPU: shader.unbind()")
    }

    override fun getNumShadowPasses(): Int = 1

    override fun beginShadowPass(pass: Int) {
        TODO("GPU: glPolygonOffset(RenderDeferredTreeShadowOffset, RenderDeferredTreeShadowBias); " +
             "gDeferredTreeShadowProgram.bind(); " +
             "uniform SUN_UP_FACTOR = environment.isSunUp ? 1 : 0; " +
             "setMinimumAlpha(0.5f)")
    }

    override fun renderShadow(pass: Int) {
        renderDeferred(pass)
    }

    override fun endShadowPass(pass: Int) {
        TODO("GPU: glPolygonOffset(RenderDeferredSpotShadowOffset, RenderDeferredSpotShadowBias); " +
             "gDeferredTreeShadowProgram.unbind()")
    }

    override fun verify(): Boolean = true

    override fun getTexture(): ViewerTexture = texturep

    override fun getDebugTexture(): ViewerTexture = texturep

    fun getDebugColor(): FloatArray = floatArrayOf(1f, 0f, 1f)
}
