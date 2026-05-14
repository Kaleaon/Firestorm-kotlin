package com.firestorm.newview

class LLDrawPoolWaterExclusion : LLRenderPass(LLDrawPool.POOL_WATEREXCLUSION) {

    companion object {
        const val VERTEX_DATA_MASK: UInt = LLVertexBuffer.MAP_VERTEX
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun prerender() {}

    override fun render(pass: Int) {
        // no-op
    }

    override fun beginRenderPass(pass: Int) {}

    override fun endRenderPass(pass: Int) {}

    override fun getNumPasses(): Int = 1
}
