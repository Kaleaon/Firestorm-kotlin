package com.firestorm.newview

class LLDrawPoolWLSky : LLDrawPool(POOL_WL_SKY) {

    companion object {
        val SKY_VERTEX_DATA_MASK: UInt = LLVertexBuffer.MAP_VERTEX or LLVertexBuffer.MAP_TEXCOORD0
        val STAR_VERTEX_DATA_MASK: UInt = LLVertexBuffer.MAP_VERTEX or LLVertexBuffer.MAP_COLOR or LLVertexBuffer.MAP_TEXCOORD0
        val ADV_ATMO_SKY_VERTEX_DATA_MASK: UInt = LLVertexBuffer.MAP_VERTEX or LLVertexBuffer.MAP_TEXCOORD0

        fun cleanupGL() {
            TODO("GPU: release GL sky resources")
        }

        fun restoreGL() {
            TODO("GPU: restore GL sky resources")
        }
    }

    override fun isDead(): Boolean = false

    override fun getNumDeferredPasses(): Int = 1

    override fun beginDeferredPass(pass: Int) {
        TODO("GPU: sky_shader = gDeferredWLSkyProgram; cloud_shader = gDeferredWLCloudProgram; sun_shader = gDeferredWLSunProgram; moon_shader = gDeferredWLMoonProgram")
    }

    override fun endDeferredPass(pass: Int) {
        // Clear depth buffer so haze shaders can use unwritten depth as a mask
        TODO("GPU: sky_shader = null; cloud_shader = null; sun_shader = null; moon_shader = null; glClear(GL_DEPTH_BUFFER_BIT)")
    }

    override fun renderDeferred(pass: Int) {
        TODO("GPU: " +
            "if !hasRenderType(RENDER_TYPE_SKY) || mVOSkyp.isNull return; " +
            "mVOSkyp.updateGeometry(drawable); " +
            "camHeightLocal = LLEnvironment.getCamHeight(); " +
            "origin = LLViewerCamera.getOrigin(); " +
            "if canUseWindLightShaders: renderSkyHazeDeferred, renderHeavenlyBodies, " +
            "renderStarsDeferred (not in cube snapshot), renderSkyCloudsDeferred (skip clouds in irradiance pass)")
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
        TODO("GPU: " +
            "gGL.matrixMode(MM_MODELVIEW); pushMatrix; " +
            "translatef to camPosLocal (clamped if reflection render and z > 256); " +
            "rotatef(120, 1/√3, 1/√3, 1/√3); scalef(0.333); translatef(0, -camHeightLocal, 0); " +
            "shader.uniform3f(camPosLocal, 0, camHeightLocal, 0); mVOWLSkyp.drawDome(); popMatrix")
    }

    private fun renderSkyHazeDeferred(camPosLocal: LLVector3, camHeightLocal: Float) {
        TODO("GPU: " +
            "if !mVOSkyp return; " +
            "if canUseWindLightShaders && hasRenderType(SKY): " +
            "bind sky_shader (HDRI or WL); set cube-snapshot uniform; " +
            "bind rainbow/halo textures; set moisture/droplet/ice/glow uniforms; " +
            "renderDome(origin, camHeightLocal, sky_shader); unbind")
    }

    private fun renderSkyCloudsDeferred(camPosLocal: LLVector3, camHeightLocal: Float, cloudShader: LLGLSLShader) {
        TODO("GPU: " +
            "if HDRI sky active return; " +
            "if canUseWindLightShaders && hasRenderType(CLOUDS) && cloudNoiseTex != null: " +
            "bind cloudshader; bind cloud noise textures; set blend/variance/glow uniforms; " +
            "renderDome; unbind cloudshader")
    }

    private fun renderStarsDeferred(camPosLocal: LLVector3) {
        TODO("GPU: " +
            "if !mVOSkyp || HDRI sky active return; " +
            "setSceneBlendType(BT_ADD_WITH_ALPHA); " +
            "compute star_alpha from getStarBrightness()/500; return if < 0.001; " +
            "gDeferredStarProgram.bind; bind bloom textures; " +
            "pushMatrix; translate camPosLocal; rotate by time*0.01 around Z; " +
            "set blend/custom_alpha/WATER_TIME uniforms; mVOWLSkyp.drawStars(); unbind; popMatrix")
    }

    private fun renderHeavenlyBodies() {
        TODO("GPU: " +
            "if !mVOSkyp || HDRI sky active return; " +
            "pushMatrix; translate to camera origin; " +
            "render sun face with sun_shader (bind diffuse textures, set color/blend uniforms, renderIndexed); " +
            "render moon face with moon_shader (bind diffuse textures, set brightness/moonlight/color/direction uniforms, renderIndexed); " +
            "popMatrix")
    }
}
