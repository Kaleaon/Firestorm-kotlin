package com.firestorm.newview

class LLDrawPoolWaterExclusion : LLRenderPass(LLDrawPool.POOL_WATEREXCLUSION) {

    companion object {
        const val VERTEX_DATA_MASK: UInt = LLVertexBuffer.MAP_VERTEX
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun prerender() {}

    override fun render(pass: Int) {
        TODO("GPU: " +
            "gDrawColorProgram.bind() if shaders loaded; " +
            "LLGLDepthTest(GL_TRUE); " +
            "gDrawColorProgram.uniform4f(DIFFUSE_COLOR, 1,1,1,1); " +
            "get water pool, disable cull face, pushWaterPlanes(0), pushWaterPlanes(1); " +
            "gDrawColorProgram.uniform4f(DIFFUSE_COLOR, 0,0,0,1); " +
            "uniform1f(waterSign, 1f); " +
            "pushBatches(PASS_INVISIBLE, false, false); " +
            "gDrawColorProgram.unbind()")
    }

    override fun beginRenderPass(pass: Int) {}

    override fun endRenderPass(pass: Int) {}

    override fun getNumPasses(): Int = 1
}
