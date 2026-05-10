package com.firestorm.newview

class DrawPoolTree(private val texturep: Any?) : FacePool(DrawPool.POOL_TREE) {

    companion object {
        var sDiffTex: Int = 0
        private var shader: GlslShader? = null
    }

    init {
        TODO("GPU: texturep.setAddressMode(TAM_WRAP)")
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    companion object Constants {
        const val VERTEX_DATA_MASK: UInt = (
            VertexBuffer.MAP_VERTEX or
            VertexBuffer.MAP_NORMAL or
            VertexBuffer.MAP_COLOR or
            VertexBuffer.MAP_TEXCOORD0
        )
    }

    override fun getNumDeferredPasses(): Int = 1

    override fun beginDeferredPass(pass: Int) {
        TODO("GPU: bind gDeferredTreeProgram; setMinimumAlpha(0.5)")
    }

    fun beginDeferredPassImpl(pass: Int) {
        val s = GlslShader()
        shader = s
        s.bind()
        s.setMinimumAlpha(0.5f)
    }

    override fun renderDeferred(pass: Int) {
        if (mDrawFace.isEmpty()) return

        TODO("GPU: bindFast texturep (or default diffuse if !sRenderTextures); addTextureStats(1M); " +
             "for each face: get vertex buffer; applyModelMatrix from region renderMatrix; " +
             "setBuffer; drawRange TRIANGLES 0..(numVerts-1) with numIndices")
    }

    fun renderDeferredImpl(pass: Int) {
        if (mDrawFace.isEmpty()) return

        TODO("GPU: gGL.getTexUnit(sDiffTex).bindFast(texturep); texturep.addTextureStats(1024*1024f)")

        for (face in mDrawFace) {
            val buff = face.getVertexBuffer() ?: continue
            val modelMatrix = face.getDrawable()?.getRegion()?.mRenderMatrix
            RenderPass.applyModelMatrix(modelMatrix)
            TODO("GPU: buff.setBuffer(); buff.drawRange(TRIANGLES, 0, numVerts-1, numIndices, 0)")
        }
    }

    override fun endDeferredPass(pass: Int) {
        TODO("GPU: shader.unbind()")
    }

    override fun getNumShadowPasses(): Int = 1

    override fun beginShadowPass(pass: Int) {
        TODO("GPU: glPolygonOffset(shadowOffset, shadowBias); bind gDeferredTreeShadowProgram; " +
             "uniform SUN_UP_FACTOR; setMinimumAlpha(0.5)")
    }

    fun beginShadowPassImpl(pass: Int) {
        val shadowOffset = 1f
        val shadowBias = 1f
        TODO("GPU: glPolygonOffset($shadowOffset, $shadowBias)")
        val s = GlslShader()
        s.bind()
        s.uniform1i(ShaderMgr.SUN_UP_FACTOR, 1)
        s.setMinimumAlpha(0.5f)
    }

    override fun renderShadow(pass: Int) {
        renderDeferred(pass)
    }

    override fun endShadowPass(pass: Int) {
        TODO("GPU: glPolygonOffset(spotShadowOffset, spotShadowBias); gDeferredTreeShadowProgram.unbind()")
    }

    fun endShadowPassImpl(pass: Int) {
        val spotShadowOffset = 1f
        val spotShadowBias = 1f
        TODO("GPU: glPolygonOffset($spotShadowOffset, $spotShadowBias); unbind shadow program")
    }

    fun verify(): Boolean = true

    fun getTexture(): Any? = texturep

    fun getDebugTexture(): Any? = texturep

    fun getDebugColor(): FloatArray = floatArrayOf(1f, 0f, 1f)
}
