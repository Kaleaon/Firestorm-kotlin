package com.firestorm.newview

// Deprecated sky draw pool — kept for compatibility with pool registry.
class DrawPoolSky : FacePool(DrawPool.PoolType.SKY.value.toUInt()) {

    companion object {
        const val VERTEX_DATA_MASK: UInt =
            VertexBufferFlags.MAP_VERTEX or VertexBufferFlags.MAP_TEXCOORD0
    }

    private var skyTex: SkyTex? = null
    private var shader: GLSLShader? = null

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun getNumPostDeferredPasses(): Int = getNumPasses()
    override fun beginPostDeferredPass(pass: Int) = beginRenderPass(pass)
    override fun endPostDeferredPass(pass: Int) = endRenderPass(pass)
    override fun renderPostDeferred(pass: Int) = render(pass)

    override fun prerender() {}

    override fun render(pass: Int) {}

    override fun endRenderPass(pass: Int) {}

    fun setSkyTex(st: SkyTex) {
        skyTex = st
    }

    fun renderSkyFace(index: UByte) {}

    fun renderHeavenlyBody(hb: UByte, face: Face) {
        // no-op
    }

    fun renderSunHalo(face: Face) {
        // no-op
    }
}

