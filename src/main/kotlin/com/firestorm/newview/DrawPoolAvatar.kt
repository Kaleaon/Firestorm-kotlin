package com.firestorm.newview

class DrawPoolAvatar(type: UInt) : FacePool(type) {

    companion object {
        var sSkipOpaque: Boolean = false
        var sSkipTransparent: Boolean = false
        var sShadowPass: Int = -1
        var sDiffuseChannel: Int = 0
        var sMinimumAlpha: Float = 0.2f
        var sVertexProgram: GlslShader? = null

        const val VERTEX_DATA_MASK: UInt = (
            VertexBuffer.MAP_VERTEX or
            VertexBuffer.MAP_NORMAL or
            VertexBuffer.MAP_TEXCOORD0 or
            VertexBuffer.MAP_WEIGHT or
            VertexBuffer.MAP_CLOTHWEIGHT
        )

        const val AVATAR_BUFFER_ELEMENTS: Int = 8192

        var avatarOffsetPos: Int = 0
        var avatarOffsetNormal: Int = 16
        var avatarOffsetTex0: Int = 32
        var avatarOffsetTex1: Int = 40
        var avatarVertexBytes: Int = 48

        var gAvatarEmbossBumpMap: Boolean = false

        private var shaderLevel: UInt = 0u
        private var isDeferredRender: Boolean = false
        private var isPostDeferredRender: Boolean = false
        private var isRenderingSkinned: Boolean = false
        private var normalChannel: Int = -1
        private var specularChannel: Int = -1
        private var cubeChannel: Int = -1

        fun getModelView(): FloatArray {
            TODO("GPU: build LLMatrix4 from gGLModelView rows")
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

    override fun isDead(): Boolean {
        return super.isDead()
    }

    override fun getNumPasses(): Int = 3

    override fun beginRenderPass(pass: Int) {
        TODO("GPU: VertexBuffer.unbind(); route to beginImpostor/beginRigid/beginSkinned")
    }

    override fun endRenderPass(pass: Int) {
        TODO("GPU: route to endImpostor/endRigid/endSkinned")
    }

    override fun render(pass: Int) {
        TODO("GPU: renderAvatars(null, pass)")
    }

    override fun getNumDeferredPasses(): Int = 3

    fun beginDeferredPass(pass: Int) {
        TODO("GPU: sSkipTransparent=true; route to beginDeferredImpostor/Rigid/Skinned")
    }

    fun endDeferredPass(pass: Int) {
        TODO("GPU: sSkipTransparent=false; route to endDeferredImpostor/Rigid/Skinned")
    }

    fun renderDeferred(pass: Int) {
        render(pass)
    }

    override fun getNumPostDeferredPasses(): Int = 1

    fun beginPostDeferredPass(pass: Int) {
        TODO("GPU: sSkipOpaque=true; bind gDeferredAvatarAlphaProgram; setMinimumAlpha; enableTexture DIFFUSE_MAP")
    }

    fun endPostDeferredPass(pass: Int) {
        TODO("GPU: sRenderingSkinned=false; sSkipOpaque=false; unbindDeferredShader; sDiffuseChannel=0")
    }

    override fun renderPostDeferred(pass: Int) {
        TODO("GPU: isPostDeferredRender=true; render(0 or 2 based on impostor); isPostDeferredRender=false")
    }

    override fun getNumShadowPasses(): Int = ShadowPass.NUM_SHADOW_PASSES

    override fun beginShadowPass(pass: Int) {
        TODO("GPU: bind shadow shader per pass (opaque/alpha-blend/alpha-mask), set diffuseChannel")
    }

    override fun endShadowPass(pass: Int) {
        TODO("GPU: unbind sVertexProgram; sShadowPass=-1")
    }

    override fun renderShadow(pass: Int) {
        TODO("GPU: validate avatar liveness/visibility; delegate renderSkinned with skip flags")
    }

    fun beginImpostor() {
        TODO("GPU: bind gImpostorProgram; setMinimumAlpha(0.01); enableLightsFullbright")
    }

    fun endImpostor() {
        TODO("GPU: unbind gImpostorProgram; enableLightsDynamic")
    }

    fun beginRigid() {
        TODO("GPU: bind gObjectAlphaMaskNoColorProgram; setMinimumAlpha(sMinimumAlpha)")
    }

    fun endRigid() {
        TODO("GPU: shaderLevel=mShaderLevel; unbind sVertexProgram")
    }

    fun beginSkinned() {
        TODO("GPU: bind gAvatarProgram; sRenderingSkinned=true; setMinimumAlpha")
    }

    fun endSkinned() {
        TODO("GPU: sRenderingSkinned=false; disableTexture BUMP_MAP; unbind; activate texunit 0")
    }

    fun beginDeferredImpostor() {
        TODO("GPU: bind gDeferredImpostorProgram; enable specular/normal/diffuse channels; setMinimumAlpha(0.01)")
    }

    fun endDeferredImpostor() {
        TODO("GPU: disableTexture normal/specular/diffuse; unbindDeferredShader; sDiffuseChannel=0")
    }

    fun beginDeferredRigid() {
        TODO("GPU: bind gDeferredNonIndexedDiffuseAlphaMaskNoColorProgram; enable DIFFUSE_MAP; setMinimumAlpha")
    }

    fun endDeferredRigid() {
        TODO("GPU: disableTexture DIFFUSE_MAP; unbind; activate texunit 0")
    }

    fun beginDeferredSkinned() {
        TODO("GPU: bind gDeferredAvatarProgram; sRenderingSkinned=true; enable DIFFUSE_MAP; activate texunit 0")
    }

    fun endDeferredSkinned() {
        TODO("GPU: sRenderingSkinned=false; unbind; disableTexture DIFFUSE_MAP; activate texunit 0")
    }

    fun renderAvatars(singleAvatar: VoAvatar?, pass: Int = -1) {
        if (pass == -1) {
            for (i in 1 until getNumPasses()) {
                prerender()
                beginRenderPass(i)
                renderAvatars(singleAvatar, i)
                endRenderPass(i)
            }
            return
        }

        TODO("GPU: resolve avatarp from drawFace or singleAvatar; " +
             "pass 0: impostors; pass 1: rigid eyeballs; pass 2+: skinned with cloth wind shader")
    }

    fun getDebugTexture(): Any? {
        TODO("GPU: return first reference face's TE image 0")
    }

    fun getDebugColor(): FloatArray = floatArrayOf(0f, 1f, 0f)
}

abstract class FacePool(type: UInt) : RenderPass(type) {
    val mDrawFace: MutableList<Face> = mutableListOf()
    val mReferences: MutableList<Face> = mutableListOf()

    open fun isDead(): Boolean = mDrawFace.isEmpty()
    override fun getNumDeferredPasses(): Int = 0
}

class Face {
    fun getDrawable(): Drawable? { TODO("GPU: Face.getDrawable") }
    fun getVertexBuffer(): VertexBuffer? { TODO("GPU: Face.getVertexBuffer") }
    fun getTextureEntry(): TextureEntry? { TODO("GPU: Face.getTextureEntry") }
    fun getTexture(): Any? { TODO("GPU: Face.getTexture") }
}

class Drawable {
    fun getVObj(): Any? { TODO("GPU: Drawable.getVObj") }
    fun getRegion(): Region? { TODO("GPU: Drawable.getRegion") }
    fun isNull(): Boolean = false
}

class Region {
    val mRenderMatrix: FloatArray = FloatArray(16)
}

class TextureEntry {
    fun getBumpmap(): UByte { TODO("GPU: TextureEntry.getBumpmap") }
}

class VoAvatar {
    var mDrawable: Drawable? = null
    var mWindVec: FloatArray = FloatArray(4)
    var mRipplePhase: Float = 0f
    var mImpostor: Any? = null

    fun isDead(): Boolean { TODO("GPU: VoAvatar.isDead") }
    fun isUIAvatar(): Boolean { TODO("GPU: VoAvatar.isUIAvatar") }
    fun isControlAvatar(): Boolean { TODO("GPU: VoAvatar.isControlAvatar") }
    fun isSelf(): Boolean { TODO("GPU: VoAvatar.isSelf") }
    fun isBuddy(): Boolean { TODO("GPU: VoAvatar.isBuddy") }
    fun isImpostor(): Boolean { TODO("GPU: VoAvatar.isImpostor") }
    fun isFullyLoaded(): Boolean { TODO("GPU: VoAvatar.isFullyLoaded") }
    fun isTooSlow(): Boolean { TODO("GPU: VoAvatar.isTooSlow") }
    fun needsImpostorUpdate(): Boolean { TODO("GPU: VoAvatar.needsImpostorUpdate") }
    fun getOverallAppearance(): Int { TODO("GPU: VoAvatar.getOverallAppearance") }
    fun getAttachedAvatar(): VoAvatar? { TODO("GPU: VoAvatar.getAttachedAvatar") }
    fun getID(): String { TODO("GPU: VoAvatar.getID") }
    fun getPositionAgent(): FloatArray { TODO("GPU: VoAvatar.getPositionAgent") }
    fun getRotationRegion(): FloatArray { TODO("GPU: VoAvatar.getRotationRegion") }
    fun getScale(): FloatArray { TODO("GPU: VoAvatar.getScale") }
    fun getMutedAVColor(): FloatArray { TODO("GPU: VoAvatar.getMutedAVColor") }
    fun renderImpostor(color: FloatArray, diffuseChannel: Int) { TODO("GPU: VoAvatar.renderImpostor") }
    fun renderRigid() { TODO("GPU: VoAvatar.renderRigid") }
    fun renderSkinned() { TODO("GPU: VoAvatar.renderSkinned") }

    companion object {
        var sNumVisibleAvatars: Int = 0
        const val AOA_NORMAL: Int = 0
        const val AOA_JELLYDOLL: Int = 1
        const val AOA_INVISIBLE: Int = 2
    }
}
