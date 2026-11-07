package com.firestorm.newview

class DrawPoolMaterials : RenderPass(DrawPool.PoolType.MATERIALS.value.toUInt()) {

    private var shader: GLSLShader? = null

    companion object {
        const val VERTEX_DATA_MASK: UInt = (
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_NORMAL or
            VertexBufferFlags.MAP_TEXCOORD0 or
            VertexBufferFlags.MAP_TEXCOORD1 or
            VertexBufferFlags.MAP_TEXCOORD2 or
            VertexBufferFlags.MAP_COLOR or
            VertexBufferFlags.MAP_TANGENT
        )

        private val SHADER_IDX = intArrayOf(0, 2, 3, 4, 6, 7, 8, 10, 11, 12, 14, 15)

        private val TYPE_LIST = intArrayOf(
            RenderPass.PassType.PASS_MATERIAL.value,
            RenderPass.PassType.PASS_MATERIAL_ALPHA_MASK.value,
            RenderPass.PassType.PASS_MATERIAL_ALPHA_EMISSIVE.value,
            RenderPass.PassType.PASS_SPECMAP.value,
            RenderPass.PassType.PASS_SPECMAP_MASK.value,
            RenderPass.PassType.PASS_SPECMAP_EMISSIVE.value,
            RenderPass.PassType.PASS_NORMMAP.value,
            RenderPass.PassType.PASS_NORMMAP_MASK.value,
            RenderPass.PassType.PASS_NORMMAP_EMISSIVE.value,
            RenderPass.PassType.PASS_NORMSPEC.value,
            RenderPass.PassType.PASS_NORMSPEC_MASK.value,
            RenderPass.PassType.PASS_NORMSPEC_EMISSIVE.value,
        )
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK
    override fun isDead(): Boolean = false

    override fun render(pass: Int) {}

    override fun getNumPasses(): Int = 0

    override fun prerender() {
        // no-op
    }

    override fun getNumDeferredPasses(): Int = 12 * 2

    override fun beginDeferredPass(pass: Int) {
        var p = pass
        val rigged = p >= 12
        if (rigged) p -= 12
        val idx = SHADER_IDX[p]
        // no-op
    }

    override fun endDeferredPass(pass: Int) {
        // no-op
    }

    override fun renderDeferred(pass: Int) {
        var p = pass
        val rigged = p >= 12
        if (rigged) p -= 12

        val type = if (rigged) TYPE_LIST[p] + 1 else TYPE_LIST[p]
        val drawShader = shader ?: return

        // no-op
    }
}
