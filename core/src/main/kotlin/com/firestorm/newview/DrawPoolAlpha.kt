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
        // no-op
    }

    override fun getNumPostDeferredPasses(): Int = 1

    override fun renderPostDeferred(pass: Int) {
        // no-op
    }

    fun forwardRender(writeDepth: Boolean = false) {
        // no-op
    }

    fun renderDebugAlpha() {
        if (!sShowDebugAlpha) return
        // no-op
    }

    fun renderAlphaHighlight() {
        // no-op
    }

    fun renderGroupAlpha(group: SpatialGroup, type: UInt, mask: UInt, texture: Boolean = true) {
        // no-op
    }

    fun renderAlpha(mask: UInt, depthOnly: Boolean = false, rigged: Boolean = false) {
        // no-op
    }

    private fun prepareAlphaShader(shader: GLSLShader, deferredEnvironment: Boolean, waterSign: Float) {
        // no-op
    }

    private fun texSetup(draw: DrawInfo, useMaterial: Boolean): Boolean {
        return false
    }

    private fun restoreTexSetup(texSetup: Boolean) {
        if (!texSetup) return
        // no-op
    }

    private fun drawEmissive(draw: DrawInfo) {
        // no-op
    }

    private fun renderEmissives(emissives: MutableList<DrawInfo>) {
        // no-op
    }

    private fun renderRiggedEmissives(emissives: MutableList<DrawInfo>) {
        // no-op
    }

    private fun renderPbrEmissives(emissives: MutableList<DrawInfo>) {
        // no-op
    }

    private fun renderRiggedPbrEmissives(emissives: MutableList<DrawInfo>) {
        // no-op
    }
}
