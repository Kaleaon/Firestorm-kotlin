package com.firestorm.newview

class DrawPoolMaterials : RenderPass(DrawPool.POOL_MATERIALS) {

    private var shader: GlslShader? = null

    companion object {
        const val VERTEX_DATA_MASK: UInt = (
            VertexBuffer.MAP_VERTEX or
            VertexBuffer.MAP_NORMAL or
            VertexBuffer.MAP_TEXCOORD0 or
            VertexBuffer.MAP_TEXCOORD1 or
            VertexBuffer.MAP_TEXCOORD2 or
            VertexBuffer.MAP_COLOR or
            VertexBuffer.MAP_TANGENT
        )

        private val SHADER_IDX = intArrayOf(0, 2, 3, 4, 6, 7, 8, 10, 11, 12, 14, 15)

        private val TYPE_LIST = intArrayOf(
            RenderPass.PASS_MATERIAL,
            RenderPass.PASS_MATERIAL_ALPHA_MASK,
            RenderPass.PASS_MATERIAL_ALPHA_EMISSIVE,
            RenderPass.PASS_SPECMAP,
            RenderPass.PASS_SPECMAP_MASK,
            RenderPass.PASS_SPECMAP_EMISSIVE,
            RenderPass.PASS_NORMMAP,
            RenderPass.PASS_NORMMAP_MASK,
            RenderPass.PASS_NORMMAP_EMISSIVE,
            RenderPass.PASS_NORMSPEC,
            RenderPass.PASS_NORMSPEC_MASK,
            RenderPass.PASS_NORMSPEC_EMISSIVE,
        )
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun render(pass: Int) {}

    override fun getNumPasses(): Int = 0

    override fun prerender() {
        TODO("GPU: mShaderLevel = ViewerShaderMgr.instance().getShaderLevel(SHADER_OBJECT)")
    }

    override fun getNumDeferredPasses(): Int = 12 * 2

    override fun beginDeferredPass(pass: Int) {
        var p = pass
        val rigged = p >= 12
        if (rigged) p -= 12

        val idx = SHADER_IDX[p]
        TODO("GPU: shader = gDeferredMaterialProgram[idx]; if rigged use mRiggedVariant; " +
             "Pipeline.bindDeferredShader(shader)")
    }

    override fun endDeferredPass(pass: Int) {
        TODO("GPU: shader.unbind(); RenderPass.endRenderPass(pass)")
    }

    override fun renderDeferred(pass: Int) {
        var p = pass
        val rigged = p >= 12
        if (rigged) p -= 12

        val type = if (rigged) TYPE_LIST[p] + 1 else TYPE_LIST[p]

        TODO("GPU: iterate render map for type; per draw-info: set specular/intensity/minAlpha/brightness/normalMap/" +
             "specMap/diffuse uniforms lazily; uploadMatrixPalette if rigged; applyModelMatrix; " +
             "handle texture matrix; drawRange TRIANGLES")
    }

    fun beginDeferredPassImpl(pass: Int) {
        var p = pass
        val rigged = p >= 12
        if (rigged) p -= 12

        val idx = SHADER_IDX[p]
        val currentShader = resolveShader(idx, rigged)
        Pipeline.bindDeferredShader(currentShader)
        this.shader = currentShader
    }

    fun endDeferredPassImpl(pass: Int) {
        shader?.unbind()
    }

    fun renderDeferredImpl(pass: Int) {
        var p = pass
        val rigged = p >= 12
        if (rigged) p -= 12

        val type = if (rigged) TYPE_LIST[p] + 1 else TYPE_LIST[p]
        val drawShader = shader ?: return

        var lastIntensity = 0f
        var lastFullbright = 0f
        var lastMinimumAlpha = 0f
        var lastSpecular = floatArrayOf(0f, 0f, 0f, 0f)
        var lastNormalMap: Any? = null
        var lastSpecMap: Any? = null
        var lastDiffuse: Any? = null
        var lastAvatar: Any? = null
        var lastMeshId: ULong = 0uL
        var skipLastSkin = false

        val intensityLoc = drawShader.getUniformLocation(ShaderMgr.ENVIRONMENT_INTENSITY)
        val brightnessLoc = drawShader.getUniformLocation(ShaderMgr.EMISSIVE_BRIGHTNESS)
        val minAlphaLoc = drawShader.getUniformLocation(ShaderMgr.MINIMUM_ALPHA)
        val specularLoc = drawShader.getUniformLocation(ShaderMgr.SPECULAR_COLOR)

        TODO("GPU: bind diffuse/specular/normal channels; iterate render map; per draw-info lazy-update " +
             "uniforms; uploadMatrixPalette if rigged; applyModelMatrix; texture matrix; drawRange TRIANGLES; " +
             "restore texture matrix")
    }

    private fun resolveShader(idx: Int, rigged: Boolean): GlslShader {
        TODO("GPU: return gDeferredMaterialProgram[idx].mRiggedVariant if rigged else gDeferredMaterialProgram[idx]")
    }
}

object ShaderMgr {
    const val ENVIRONMENT_INTENSITY: Int = 0
    const val EMISSIVE_BRIGHTNESS: Int = 1
    const val MINIMUM_ALPHA: Int = 2
    const val SPECULAR_COLOR: Int = 3
    const val DIFFUSE_MAP: Int = 4
    const val SPECULAR_MAP: Int = 5
    const val BUMP_MAP: Int = 6
    const val NORMAL_MAP: Int = 7
    const val EXPOSURE_MAP: Int = 8
    const val DISPLAY_GAMMA: Int = 9
    const val WATER_WATERPLANE: Int = 10
    const val SUN_UP_FACTOR: Int = 11
}
