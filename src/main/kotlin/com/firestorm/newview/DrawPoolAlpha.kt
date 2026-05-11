package com.firestorm.newview

class DrawPoolAlpha(type: UInt) : RenderPass(type) {

    companion object {
        var sWaterPlane: FloatArray = FloatArray(4)
        var sShowDebugAlpha: Boolean = false
        var sShowDebugAlphaRigged: Boolean = false

        const val VERTEX_DATA_MASK: UInt = (
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_NORMAL or
            VertexBufferFlags.MAP_COLOR or
            VertexBufferFlags.MAP_TEXCOORD0
        )

        private const val MINIMUM_ALPHA: Float = 0.004f
        private const val MINIMUM_IMPOSTOR_ALPHA: Float = 0.1f
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK
    override fun isDead(): Boolean = false

    private var targetShader: GLSLShader? = null
    private var simpleShader: GLSLShader? = null
    private var fullbrightShader: GLSLShader? = null
    private var emissiveShader: GLSLShader? = null
    private var pbrEmissiveShader: GLSLShader? = null
    private var pbrShader: GLSLShader? = null

    private var colorSFactor: Int = 0
    private var colorDFactor: Int = 0
    private var alphaSFactor: Int = 0
    private var alphaDFactor: Int = 0

    private var mRigged: Boolean = false

    override fun getNumPasses(): Int = 1

    override fun prerender() {
        TODO("GPU: mShaderLevel = ViewerShaderMgr.instance().getShaderLevel(SHADER_OBJECT)")
    }

    override fun getNumPostDeferredPasses(): Int = 1

    override fun renderPostDeferred(pass: Int) {
        TODO("GPU: skip if water-clipped and POOL_ALPHA_PRE_WATER; " +
             "compute waterSign from pool type and underwaterRender; " +
             "prepare emissive/pbrEmissive/fullbright/simple/material/pbr shaders; " +
             "GLSLShader.unbindAll(); forwardRender(rigged=true) unless HUD; forwardRender(); " +
             "if !impostor && RenderDepthOfField && !cubeSnapshot && !HUD && POST_WATER: " +
             "  depth-only pass with fullbrightAlphaMask shader and setColorMask(false,false)")
    }

    fun forwardRender(writeDepth: Boolean = false) {
        TODO("GPU: enableLightsDynamic; GLS pipeline alpha; setColorMask(true,true); " +
             "depth test (write if rigged|skipScreenCopy|impostorAlphaDepth|PRE_WATER); " +
             "blendFunc SRC_ALPHA/ONE_MINUS_SRC_ALPHA for color, ZERO/ONE_MINUS_SRC_ALPHA for alpha; " +
             "if rigged && POST_WATER: render GLTF scene to depth; " +
             "renderAlpha(vertexDataMask|TEXTURE_INDEX|TANGENT|TEXCOORD1|TEXCOORD2, false, rigged); " +
             "setColorMask(true,false); if !rigged && POST_WATER: renderDebugAlpha()")
    }

    fun renderDebugAlpha() {
        if (!sShowDebugAlpha) return
        TODO("GPU: bind gHighlightProgram; diffuseColor red; bind smoke texture; " +
             "renderAlphaHighlight(); pushUntexturedBatches(ALPHA_MASK, ALPHA_INVISIBLE); " +
             "diffuseColor blue for material/normmap/specmap/normspec/fullbright/pbr mask batches; " +
             "diffuseColor green for INVISIBLE; if sShowDebugAlphaRigged: bind rigged variant, " +
             "cyan for rigged material masks, green for rigged invisible; unbind current shader")
    }

    fun renderAlphaHighlight() {
        TODO("GPU: two passes (static + rigged); for each SpatialGroup with renderByGroup && !dead: " +
             "iterate PASS_ALPHA[+pass] drawmap; bind highlight program (rigged variant if needed); " +
             "uploadMatrixPalette for rigged; skip rigged if !sShowDebugAlphaRigged; " +
             "diffuseColor orange for rigged, red for static; applyModelMatrix; drawRange TRIANGLES; " +
             "bind static highlight shader before return")
    }

    fun renderGroupAlpha(group: SpatialGroup, type: UInt, mask: UInt, texture: Boolean = true) {
        TODO("GPU: iterate group draw map for type; applyModelMatrix; setBuffer; drawRange TRIANGLES")
    }

    fun renderAlpha(mask: UInt, depthOnly: Boolean = false, rigged: Boolean = false) {
        TODO("GPU: iterate alpha/riggedAlpha spatial groups; filter by above/below waterHeight; " +
             "skip particle groups if !sRenderParticles; disable cull for particle groups; " +
             "per DrawInfo: select pbr/fullbright/simple/material shader; upload matrix palette; " +
             "texSetup; blendFunc from params; draw TRIANGLES; collect emissive lists; " +
             "after group: if !depthOnly render emissive sub-passes with glow-accumulating blend; " +
             "setSceneBlendType BT_ALPHA; VertexBuffer.unbindAll(); re-enable lights if turned off")
    }

    private fun prepareAlphaShader(shader: GLSLShader, deferredEnvironment: Boolean, waterSign: Float) {
        TODO("GPU: if deferredEnvironment: shader.mCanBindFast=false; shader.bind(); " +
             "uniform DISPLAY_GAMMA = 1/gamma; " +
             "if HUD: waterSign=1, waterPlane=[0,0,-1,0] else set from params; " +
             "setMinimumAlpha (impostor: 0.1, else 0.004); recurse for mRiggedVariant")
    }

    private fun texSetup(draw: DrawInfo, useMaterial: Boolean): Boolean {
        TODO("GPU: if GLTF material: load texture matrix if present; " +
             "else if !HUD && useMaterial && currentShader: bind normalMap + specularMap; " +
             "or bind flat normal + white spec for simple shader; " +
             "bind batched textures or single texture with optional texture matrix; return tex_setup flag")
    }

    private fun restoreTexSetup(texSetup: Boolean) {
        if (!texSetup) return
        TODO("GPU: activate texunit 0; matrixMode MM_TEXTURE; loadIdentity; matrixMode MM_MODELVIEW")
    }

    private fun drawEmissive(draw: DrawInfo) {
        TODO("GPU: uniform EMISSIVE_BRIGHTNESS=1; draw.vertexBuffer.setBuffer(); drawRange TRIANGLES")
    }

    private fun renderEmissives(emissives: MutableList<DrawInfo>) {
        TODO("GPU: bind emissiveShader; uniform EMISSIVE_BRIGHTNESS=1; for each: texSetup; drawEmissive; restoreTexSetup")
    }

    private fun renderRiggedEmissives(emissives: MutableList<DrawInfo>) {
        TODO("GPU: depth(true, no-write); bind emissiveShader.mRiggedVariant; " +
             "uniform EMISSIVE_BRIGHTNESS=1; for each: uploadMatrixPalette; texSetup; drawEmissive; restoreTexSetup")
    }

    private fun renderPbrEmissives(emissives: MutableList<DrawInfo>) {
        TODO("GPU: bind pbrEmissiveShader; for each: disable cull if doubleSided; gltfMaterial.bind; drawRange")
    }

    private fun renderRiggedPbrEmissives(emissives: MutableList<DrawInfo>) {
        TODO("GPU: depth(true, no-write); bind pbrEmissiveShader rigged; " +
             "for each: uploadMatrixPalette; disable cull if doubleSided; gltfMaterial.bind; drawRange")
    }
}
