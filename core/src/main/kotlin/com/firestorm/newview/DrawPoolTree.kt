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
        // no-op
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun getNumDeferredPasses(): Int = 1

    override fun beginDeferredPass(pass: Int) {
        // no-op
    }

    override fun renderDeferred(pass: Int) {
        if (drawFace.isEmpty()) return
        // no-op
    }

    override fun endDeferredPass(pass: Int) {
        // no-op
    }

    override fun getNumShadowPasses(): Int = 1

    override fun beginShadowPass(pass: Int) {
        // no-op
    }

    override fun renderShadow(pass: Int) {
        renderDeferred(pass)
    }

    override fun endShadowPass(pass: Int) {
        // no-op
    }

    override fun verify(): Boolean = true

    override fun getTexture(): ViewerTexture = texturep

    override fun getDebugTexture(): ViewerTexture = texturep

    fun getDebugColor(): FloatArray = floatArrayOf(1f, 0f, 1f)
}
