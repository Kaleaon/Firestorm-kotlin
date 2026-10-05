package com.firestorm.newview

class LLDrawPoolWLSky : LLDrawPool(POOL_WL_SKY) {

    companion object {
        val SKY_VERTEX_DATA_MASK: UInt = LLVertexBuffer.MAP_VERTEX or LLVertexBuffer.MAP_TEXCOORD0
        val STAR_VERTEX_DATA_MASK: UInt = LLVertexBuffer.MAP_VERTEX or LLVertexBuffer.MAP_COLOR or LLVertexBuffer.MAP_TEXCOORD0
        val ADV_ATMO_SKY_VERTEX_DATA_MASK: UInt = LLVertexBuffer.MAP_VERTEX or LLVertexBuffer.MAP_TEXCOORD0

        fun cleanupGL() {
            // no-op
        }

        fun restoreGL() {
            // no-op
        }
    }

    override fun isDead(): Boolean = false

    override fun getNumDeferredPasses(): Int = 1

    override fun beginDeferredPass(pass: Int) {
        // no-op
    }

    override fun endDeferredPass(pass: Int) {
        // Clear depth buffer so haze shaders can use unwritten depth as a mask
        // no-op
    }

    override fun renderDeferred(pass: Int) {
        // no-op
    }

    override fun getDebugTexture(): LLViewerTexture? = null

    override fun getVertexDataMask(): UInt = SKY_VERTEX_DATA_MASK

    override fun verify(): Boolean = true

    override fun getShaderLevel(): Int = mShaderLevel

    override fun getTexture(): LLViewerTexture? = null

    override fun isFacePool(): Boolean = false

    override fun resetDrawOrders() {}

    private fun renderDome(camPosLocal: LLVector3, camHeightLocal: Float, shader: LLGLSLShader) {
        // Y-up permutation via 120° rotation around (1,1,1)/√3 so WL sky dome coords align with world
        // no-op
    }

    private fun renderSkyHazeDeferred(camPosLocal: LLVector3, camHeightLocal: Float) {
        // no-op
    }

    private fun renderSkyCloudsDeferred(camPosLocal: LLVector3, camHeightLocal: Float, cloudShader: LLGLSLShader) {
        // no-op
    }

    private fun renderStarsDeferred(camPosLocal: LLVector3) {
        // no-op
    }

    private fun renderHeavenlyBodies() {
        // no-op
    }
}
