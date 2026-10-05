package com.firestorm.newview

class DrawPoolAvatar(type: UInt) : FacePool(type) {

    companion object {
        var sSkipOpaque: Boolean = false
        var sSkipTransparent: Boolean = false
        var sShadowPass: Int = -1
        var sDiffuseChannel: Int = 0
        var sMinimumAlpha: Float = 0.2f
        var sVertexProgram: GLSLShader? = null

        const val VERTEX_DATA_MASK: UInt = (
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_NORMAL or
            VertexBufferFlags.MAP_TEXCOORD0 or
            VertexBufferFlags.MAP_WEIGHT or
            VertexBufferFlags.MAP_CLOTHWEIGHT
        )

        const val AVATAR_BUFFER_ELEMENTS: Int = 8192

        val avatarOffsetPos: Int = 0
        val avatarOffsetNormal: Int = 16
        val avatarOffsetTex0: Int = 32
        val avatarOffsetTex1: Int = 40
        val avatarVertexBytes: Int = 48

        var gAvatarEmbossBumpMap: Boolean = false

        private var shaderLevel: UInt = 0u
        private var isDeferredRender: Boolean = false
        private var isPostDeferredRender: Boolean = false
        private var isRenderingSkinned: Boolean = false
        private var normalChannel: Int = -1
        private var specularChannel: Int = -1

        fun getModelView(): FloatArray {
            return FloatArray(16)
        }
    }

    enum class ShadowPass {
        AVATAR_OPAQUE,
        AVATAR_ALPHA_BLEND,
        AVATAR_ALPHA_MASK;

        companion object {
            const val NUM_SHADOW_PASSES: Int = 3
        }
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    fun getShaderLevel(): Int {
        return 0
    }

    override fun prerender() {
        // no-op
    }

    override fun getNumPasses(): Int = 3

    override fun beginRenderPass(pass: Int) {
        // no-op
    }

    override fun endRenderPass(pass: Int) {
        // no-op
    }

    override fun render(pass: Int) {
        // no-op
    }

    override fun getNumDeferredPasses(): Int = 3

    fun beginDeferredPass(pass: Int) {
        // no-op
    }

    fun endDeferredPass(pass: Int) {
        // no-op
    }

    override fun renderDeferred(pass: Int) {
        render(pass)
    }

    override fun getNumPostDeferredPasses(): Int = 1

    fun beginPostDeferredPass(pass: Int) {
        // no-op
    }

    fun endPostDeferredPass(pass: Int) {
        // no-op
    }

    override fun renderPostDeferred(pass: Int) {
        // no-op
    }

    override fun getNumShadowPasses(): Int = ShadowPass.NUM_SHADOW_PASSES

    override fun beginShadowPass(pass: Int) {
        // no-op
    }

    override fun endShadowPass(pass: Int) {
        // no-op
    }

    override fun renderShadow(pass: Int) {
        // no-op
    }

    fun beginImpostor() {
        // no-op
    }

    fun endImpostor() {
        // no-op
    }

    fun beginRigid() {
        // no-op
    }

    fun endRigid() {
        // no-op
    }

    fun beginSkinned() {
        // no-op
    }

    fun endSkinned() {
        // no-op
    }

    fun beginDeferredImpostor() {
        // no-op
    }

    fun endDeferredImpostor() {
        // no-op
    }

    fun beginDeferredRigid() {
        // no-op
    }

    fun endDeferredRigid() {
        // no-op
    }

    fun beginDeferredSkinned() {
        // no-op
    }

    fun endDeferredSkinned() {
        // no-op
    }

    fun renderAvatars(singleAvatar: VOAvatar?, pass: Int = -1) {
        if (pass == -1) {
            for (i in 1 until getNumPasses()) {
                prerender()
                beginRenderPass(i)
                renderAvatars(singleAvatar, i)
                endRenderPass(i)
            }
            return
        }

        // no-op
    }

    override fun getDebugTexture(): ViewerTexture? {
        return null
    }

    fun getDebugColor(): FloatArray = floatArrayOf(0f, 1f, 0f)
}
