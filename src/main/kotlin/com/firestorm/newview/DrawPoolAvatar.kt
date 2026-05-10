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
            TODO("GPU: build matrix from gGLModelView rows 0,4,8,12")
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
        TODO("GPU: ViewerShaderMgr.instance().getShaderLevel(SHADER_AVATAR)")
    }

    override fun prerender() {
        TODO("GPU: mShaderLevel = ViewerShaderMgr.instance().getShaderLevel(SHADER_AVATAR)")
    }

    override fun getNumPasses(): Int = 3

    override fun beginRenderPass(pass: Int) {
        TODO("GPU: VertexBuffer.unbindAll(); if impostorRender ++pass; " +
             "route pass 0→beginImpostor, 1→beginRigid, 2→beginSkinned; " +
             "if pass==0: diffuseColor4f(1,1,1,1)")
    }

    override fun endRenderPass(pass: Int) {
        TODO("GPU: if impostorRender ++pass; route pass 0→endImpostor, 1→endRigid, 2→endSkinned")
    }

    override fun render(pass: Int) {
        TODO("GPU: if impostorRender: renderAvatars(null, pass+1) return; else renderAvatars(null, pass)")
    }

    override fun getNumDeferredPasses(): Int = 3

    fun beginDeferredPass(pass: Int) {
        TODO("GPU: sSkipTransparent=true; isDeferredRender=true; if impostorRender ++pass; " +
             "route pass 0→beginDeferredImpostor, 1→beginDeferredRigid, 2→beginDeferredSkinned")
    }

    fun endDeferredPass(pass: Int) {
        TODO("GPU: sSkipTransparent=false; isDeferredRender=false; if impostorRender ++pass; " +
             "route pass 0→endDeferredImpostor, 1→endDeferredRigid, 2→endDeferredSkinned")
    }

    override fun renderDeferred(pass: Int) {
        render(pass)
    }

    override fun getNumPostDeferredPasses(): Int = 1

    fun beginPostDeferredPass(pass: Int) {
        TODO("GPU: sSkipOpaque=true; sVertexProgram=gDeferredAvatarAlphaProgram; isRenderingSkinned=true; " +
             "bindDeferredShader; setMinimumAlpha(sMinimumAlpha); sDiffuseChannel=enableTexture(DIFFUSE_MAP)")
    }

    fun endPostDeferredPass(pass: Int) {
        TODO("GPU: isRenderingSkinned=false; sSkipOpaque=false; " +
             "unbindDeferredShader(sVertexProgram); sDiffuseChannel=0; shaderLevel=mShaderLevel")
    }

    override fun renderPostDeferred(pass: Int) {
        TODO("GPU: isPostDeferredRender=true; if impostorRender: render(0) else render(2); isPostDeferredRender=false")
    }

    override fun getNumShadowPasses(): Int = ShadowPass.NUM_SHADOW_PASSES

    override fun beginShadowPass(pass: Int) {
        TODO("GPU: OPAQUE: bind gDeferredAvatarShadowProgram; " +
             "ALPHA_BLEND: bind gDeferredAvatarAlphaShadowProgram, enable DIFFUSE_MAP; " +
             "ALPHA_MASK: bind gDeferredAvatarAlphaMaskShadowProgram, enable DIFFUSE_MAP; " +
             "all passes: if shaderLevel>0: isRenderingSkinned=true; bind; diffuseColor4f(1,1,1,1)")
    }

    override fun endShadowPass(pass: Int) {
        TODO("GPU: if shaderLevel>0: sVertexProgram.unbind(); sVertexProgram=null; isRenderingSkinned=false; sShadowPass=-1")
    }

    override fun renderShadow(pass: Int) {
        TODO("GPU: check drawFace not empty; get avatarp from first face; " +
             "guard: isDead, isUIAvatar, mDrawable.isNull, isTooSlow, impostor, AOA_INVISIBLE → return; " +
             "check friends_only setting; sShadowPass=pass; " +
             "OPAQUE: sSkipTransparent=true, renderSkinned, sSkipTransparent=false; " +
             "ALPHA_BLEND/ALPHA_MASK: sSkipOpaque=true, renderSkinned, sSkipOpaque=false")
    }

    fun beginImpostor() {
        TODO("GPU: if !sReflectionRender: sNumVisibleAvatars=0; " +
             "gImpostorProgram.bind(); setMinimumAlpha(0.01f); enableLightsFullbright(); sDiffuseChannel=0")
    }

    fun endImpostor() {
        TODO("GPU: gImpostorProgram.unbind(); enableLightsDynamic()")
    }

    fun beginRigid() {
        TODO("GPU: if shadersLoaded: sVertexProgram=gObjectAlphaMaskNoColorProgram; " +
             "sVertexProgram.bind(); setMinimumAlpha(sMinimumAlpha)")
    }

    fun endRigid() {
        TODO("GPU: shaderLevel=mShaderLevel; sVertexProgram?.unbind()")
    }

    fun beginSkinned() {
        TODO("GPU: sVertexProgram=gAvatarProgram; isRenderingSkinned=true; " +
             "sVertexProgram.bind(); setMinimumAlpha(sMinimumAlpha)")
    }

    fun endSkinned() {
        TODO("GPU: isRenderingSkinned=false; if shaderLevel>0: disableTexture(BUMP_MAP); " +
             "activate texunit 0; unbind; shaderLevel=mShaderLevel; else if shadersLoaded: unbind; " +
             "activate texunit 0")
    }

    fun beginDeferredImpostor() {
        TODO("GPU: if !sReflectionRender: sNumVisibleAvatars=0; " +
             "sVertexProgram=gDeferredImpostorProgram; " +
             "specularChannel=enableTexture(SPECULAR_MAP); normalChannel=enableTexture(NORMAL_MAP); " +
             "sDiffuseChannel=enableTexture(DIFFUSE_MAP); bind; setMinimumAlpha(0.01f)")
    }

    fun endDeferredImpostor() {
        TODO("GPU: shaderLevel=mShaderLevel; disableTexture(NORMAL_MAP, SPECULAR_MAP, DIFFUSE_MAP); " +
             "unbindDeferredShader(sVertexProgram); sVertexProgram=null; sDiffuseChannel=0")
    }

    fun beginDeferredRigid() {
        TODO("GPU: sVertexProgram=gDeferredNonIndexedDiffuseAlphaMaskNoColorProgram; " +
             "sDiffuseChannel=enableTexture(DIFFUSE_MAP); bind; setMinimumAlpha(sMinimumAlpha)")
    }

    fun endDeferredRigid() {
        TODO("GPU: shaderLevel=mShaderLevel; disableTexture(DIFFUSE_MAP); unbind; activate texunit 0")
    }

    fun beginDeferredSkinned() {
        TODO("GPU: shaderLevel=mShaderLevel; sVertexProgram=gDeferredAvatarProgram; isRenderingSkinned=true; " +
             "bind; setMinimumAlpha(sMinimumAlpha); sDiffuseChannel=enableTexture(DIFFUSE_MAP); activate texunit 0")
    }

    fun endDeferredSkinned() {
        TODO("GPU: isRenderingSkinned=false; unbind; disableTexture(DIFFUSE_MAP); " +
             "shaderLevel=mShaderLevel; activate texunit 0")
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

        TODO("GPU: if drawFace.empty && !singleAvatar: return; resolve avatarp from drawFace[0] or singleAvatar; " +
             "guard null/dead/mDrawable.isNull; " +
             "if DebugRenderHitboxes && pass==2 && !controlAvatar: draw hitbox wireframe with gDebugProgram; " +
             "if !isFullyLoaded && pass==0 && !particles: draw placeholder sphere; return; " +
             "check friends_only; " +
             "pass 0: sNumVisibleAvatars++; if impostor/non-normal: bind impostor textures; renderImpostor; return; " +
             "pass 1: avatarp.renderRigid(); return; " +
             "pass 2+: if RenderAvatarCloth: compute wind/gravity/sinwave uniforms; avatarp.renderSkinned()")
    }

    override fun getDebugTexture(): ViewerTexture? {
        TODO("GPU: return references[0] face drawable vobj TE image 0")
    }

    fun getDebugColor(): FloatArray = floatArrayOf(0f, 1f, 0f)
}
