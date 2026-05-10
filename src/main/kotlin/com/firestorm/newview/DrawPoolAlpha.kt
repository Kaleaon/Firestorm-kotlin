package com.firestorm.newview

data class Vector4(val x: Float, val y: Float, val z: Float, val w: Float)

enum class BlendFactor {
    BF_UNDEF,
    BF_SOURCE_ALPHA,
    BF_ONE_MINUS_SOURCE_ALPHA,
    BF_ZERO,
    BF_ONE,
}

class DrawPoolAlpha(type: UInt) : RenderPass(type) {

    companion object {
        var sWaterPlane: Vector4 = Vector4(0f, 0f, 0f, 0f)
        var sShowDebugAlpha: Boolean = false
        var sShowDebugAlphaRigged: Boolean = false

        const val VERTEX_DATA_MASK: UInt = (
            VertexBuffer.MAP_VERTEX or
            VertexBuffer.MAP_NORMAL or
            VertexBuffer.MAP_COLOR or
            VertexBuffer.MAP_TEXCOORD0
        )

        private const val MINIMUM_ALPHA: Float = 0.004f
        private const val MINIMUM_IMPOSTOR_ALPHA: Float = 0.1f
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    private var targetShader: GlslShader? = null
    private var simpleShader: GlslShader? = null
    private var fullbrightShader: GlslShader? = null
    private var emissiveShader: GlslShader? = null
    private var pbrEmissiveShader: GlslShader? = null
    private var pbrShader: GlslShader? = null

    private var colorSFactor: BlendFactor = BlendFactor.BF_UNDEF
    private var colorDFactor: BlendFactor = BlendFactor.BF_UNDEF
    private var alphaSFactor: BlendFactor = BlendFactor.BF_UNDEF
    private var alphaDFactor: BlendFactor = BlendFactor.BF_UNDEF

    private var mRigged: Boolean = false

    override fun getNumPasses(): Int = 1

    override fun prerender() {
        TODO("GPU: mShaderLevel = ViewerShaderMgr.instance().getShaderLevel(SHADER_OBJECT)")
    }

    override fun getNumPostDeferredPasses(): Int = 1

    override fun renderPostDeferred(pass: Int) {
        TODO("GPU: renderPostDeferred full deferred alpha pipeline")
    }

    fun forwardRender(writeDepth: Boolean = false) {
        TODO("GPU: forwardRender alpha pipeline")
    }

    fun renderDebugAlpha() {
        if (!sShowDebugAlpha) return
        TODO("GPU: renderDebugAlpha highlight pass")
    }

    fun renderAlphaHighlight() {
        TODO("GPU: renderAlphaHighlight two-pass rigged/static")
    }

    fun renderGroupAlpha(group: SpatialGroup, type: UInt, mask: UInt, texture: Boolean = true) {
        TODO("GPU: renderGroupAlpha")
    }

    fun renderAlpha(mask: UInt, depthOnly: Boolean = false, rigged: Boolean = false) {
        TODO("GPU: renderAlpha full alpha draw loop with emissive sub-passes")
    }

    private fun prepareAlphaShader(shader: GlslShader, deferredEnvironment: Boolean, waterSign: Float) {
        TODO("GPU: prepareAlphaShader bind + set uniforms (gamma, waterSign, waterPlane, minimumAlpha)")
    }

    private fun texSetup(draw: DrawInfo, useMaterial: Boolean): Boolean {
        TODO("GPU: texSetup bind normal/specular/diffuse maps and texture matrix")
        return false
    }

    private fun restoreTexSetup(texSetup: Boolean) {
        if (!texSetup) return
        TODO("GPU: restoreTexSetup restore texture matrix to identity")
    }

    private fun drawEmissive(draw: DrawInfo) {
        TODO("GPU: drawEmissive uniform EMISSIVE_BRIGHTNESS=1 + drawRange")
    }

    private fun renderEmissives(emissives: MutableList<DrawInfo>) {
        TODO("GPU: renderEmissives bind emissiveShader + draw each")
    }

    private fun renderRiggedEmissives(emissives: MutableList<DrawInfo>) {
        TODO("GPU: renderRiggedEmissives depth no-write + rigged variant + upload matrix palette")
    }

    private fun renderPbrEmissives(emissives: MutableList<DrawInfo>) {
        TODO("GPU: renderPbrEmissives bind pbrEmissiveShader + GLTF material bind")
    }

    private fun renderRiggedPbrEmissives(emissives: MutableList<DrawInfo>) {
        TODO("GPU: renderRiggedPbrEmissives depth no-write + rigged PBR + upload matrix palette")
    }
}

abstract class RenderPass(val type: UInt) {
    var mShaderLevel: Int = 0

    open fun getVertexDataMask(): UInt = 0u
    open fun getNumPasses(): Int = 0
    open fun prerender() {}
    open fun getNumPostDeferredPasses(): Int = 0
    open fun renderPostDeferred(pass: Int) {}
    open fun getNumDeferredPasses(): Int = 0
    open fun renderDeferred(pass: Int) {}
    open fun getNumShadowPasses(): Int = 0
    open fun beginShadowPass(pass: Int) {}
    open fun endShadowPass(pass: Int) {}
    open fun renderShadow(pass: Int) {}
    open fun beginRenderPass(pass: Int) {}
    open fun endRenderPass(pass: Int) {}
    open fun render(pass: Int = 0) {}

    fun pushUntexturedBatches(type: Int) { TODO("GPU: pushUntexturedBatches") }
    fun pushRiggedBatches(type: Int, texture: Boolean = true) { TODO("GPU: pushRiggedBatches") }
    fun pushBatches(type: Int, texture: Boolean = true, batch: Boolean = false) { TODO("GPU: pushBatches") }
    fun pushBumpBatch(params: DrawInfo, texture: Boolean, batchTextures: Boolean = true) { TODO("GPU: pushBumpBatch") }

    companion object {
        fun applyModelMatrix(params: DrawInfo) { TODO("GPU: applyModelMatrix") }
        fun applyModelMatrix(matrix: Any?) { TODO("GPU: applyModelMatrix(matrix)") }

        const val PASS_ALPHA: Int = 0
        const val PASS_ALPHA_RIGGED: Int = 1
        const val PASS_ALPHA_MASK: Int = 2
        const val PASS_ALPHA_MASK_RIGGED: Int = 3
        const val PASS_ALPHA_INVISIBLE: Int = 4
        const val PASS_ALPHA_INVISIBLE_RIGGED: Int = 5
        const val PASS_MATERIAL_ALPHA_MASK: Int = 6
        const val PASS_MATERIAL_ALPHA_MASK_RIGGED: Int = 7
        const val PASS_NORMMAP_MASK: Int = 8
        const val PASS_NORMMAP_MASK_RIGGED: Int = 9
        const val PASS_SPECMAP_MASK: Int = 10
        const val PASS_SPECMAP_MASK_RIGGED: Int = 11
        const val PASS_NORMSPEC_MASK: Int = 12
        const val PASS_NORMSPEC_MASK_RIGGED: Int = 13
        const val PASS_FULLBRIGHT_ALPHA_MASK: Int = 14
        const val PASS_FULLBRIGHT_ALPHA_MASK_RIGGED: Int = 15
        const val PASS_GLTF_PBR_ALPHA_MASK: Int = 16
        const val PASS_GLTF_PBR_ALPHA_MASK_RIGGED: Int = 17
        const val PASS_INVISIBLE: Int = 18
        const val PASS_INVISIBLE_RIGGED: Int = 19
        const val PASS_BUMP: Int = 20
        const val PASS_BUMP_RIGGED: Int = 21
        const val PASS_POST_BUMP: Int = 22
        const val PASS_FULLBRIGHT_SHINY: Int = 23
        const val PASS_FULLBRIGHT_SHINY_RIGGED: Int = 24
        const val PASS_MATERIAL: Int = 25
        const val PASS_MATERIAL_ALPHA_EMISSIVE: Int = 26
        const val PASS_SPECMAP: Int = 27
        const val PASS_SPECMAP_EMISSIVE: Int = 28
        const val PASS_NORMMAP: Int = 29
        const val PASS_NORMMAP_EMISSIVE: Int = 30
        const val PASS_NORMSPEC: Int = 31
        const val PASS_NORMSPEC_EMISSIVE: Int = 32

        fun uploadMatrixPalette(
            avatar: Any?,
            skinInfo: Any?,
            lastAvatar: Any?,
            lastMeshId: ULong,
            skipLastSkin: Boolean
        ): Boolean {
            TODO("GPU: uploadMatrixPalette")
        }

        fun uploadMatrixPalette(
            avatar: Any?,
            skinInfo: Any?,
            lastAvatar: Any?,
            lastMeshId: ULong,
            lastShader: Any?,
            skipLastSkin: Boolean
        ): Boolean {
            TODO("GPU: uploadMatrixPalette(with shader)")
        }
    }
}

class GlslShader {
    var mRiggedVariant: GlslShader? = null
    var mCanBindFast: Boolean = false

    fun bind(rigged: Boolean = false) { TODO("GPU: GlslShader.bind") }
    fun unbind() { TODO("GPU: GlslShader.unbind") }
    fun uniform1f(name: Any, value: Float) { TODO("GPU: uniform1f") }
    fun uniform1i(name: Any, value: Int) { TODO("GPU: uniform1i") }
    fun uniform4f(name: Any, x: Float, y: Float, z: Float, w: Float) { TODO("GPU: uniform4f") }
    fun uniform4fv(name: Any, count: Int, values: FloatArray) { TODO("GPU: uniform4fv") }
    fun setMinimumAlpha(alpha: Float) { TODO("GPU: setMinimumAlpha") }
    fun enableTexture(channel: Int): Int { TODO("GPU: enableTexture") }
    fun disableTexture(channel: Int, target: Any? = null) { TODO("GPU: disableTexture") }
    fun bindTexture(channel: Int, texture: Any?) { TODO("GPU: bindTexture") }
    fun getUniformLocation(name: Any): Int { TODO("GPU: getUniformLocation") }

    companion object {
        var sCurBoundShaderPtr: GlslShader? = null
        fun unbind() { TODO("GPU: GlslShader.unbind static") }
    }
}

class VertexBuffer {
    fun setBuffer() { TODO("GPU: VertexBuffer.setBuffer") }
    fun drawRange(mode: Int, start: Int, end: Int, count: Int, offset: Int) { TODO("GPU: drawRange") }
    fun hasDataType(type: Int): Boolean { TODO("GPU: hasDataType"); return false }
    fun getNumVerts(): Int { TODO("GPU: getNumVerts"); return 0 }
    fun getNumIndices(): Int { TODO("GPU: getNumIndices"); return 0 }

    companion object {
        const val MAP_VERTEX: UInt = 0x0001u
        const val MAP_NORMAL: UInt = 0x0002u
        const val MAP_COLOR: UInt = 0x0004u
        const val MAP_TEXCOORD0: UInt = 0x0008u
        const val MAP_TEXCOORD1: UInt = 0x0010u
        const val MAP_TEXCOORD2: UInt = 0x0020u
        const val MAP_WEIGHT: UInt = 0x0040u
        const val MAP_CLOTHWEIGHT: UInt = 0x0080u
        const val MAP_TANGENT: UInt = 0x0100u
        const val MAP_TEXTURE_INDEX: UInt = 0x0200u
        const val TYPE_EMISSIVE: Int = 10

        fun unbind() { TODO("GPU: VertexBuffer.unbind") }
    }
}

class DrawInfo {
    var mVertexBuffer: VertexBuffer = VertexBuffer()
    var mStart: Int = 0
    var mEnd: Int = 0
    var mCount: Int = 0
    var mOffset: Int = 0
    var mFullbright: Boolean = false
    var mMaterial: Any? = null
    var mGLTFMaterial: GltfMaterial? = null
    var mAvatar: Any? = null
    var mSkinInfo: Any? = null
    var mTextureMatrix: Any? = null
    var mTexture: Any? = null
    var mNormalMap: Any? = null
    var mSpecularMap: Any? = null
    var mTextureList: MutableList<Any?> = mutableListOf()
    var mBlendFuncSrc: Int = 0
    var mBlendFuncDst: Int = 0
    var mSpecColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var mEnvIntensity: Float = 0f
    var mShaderMask: UInt = 0u
    var mAlphaMaskCutoff: Float = 0f
    var mBump: UByte = 0u
    var mGroup: SpatialGroup? = null
    var mNormalMap2: Any? = null
    var mSpecularMap2: Any? = null
}

class GltfMaterial {
    var mDoubleSided: Boolean = false
    var mAlphaMode: Int = ALPHA_MODE_BLEND

    fun bind(texture: Any?) { TODO("GPU: GltfMaterial.bind") }

    companion object {
        const val ALPHA_MODE_BLEND: Int = 0
        const val ALPHA_MODE_MASK: Int = 1
        const val ALPHA_MODE_OPAQUE: Int = 2
    }
}

class SpatialGroup {
    val mDrawMap: MutableMap<Int, MutableList<DrawInfo>> = mutableMapOf()

    fun getSpatialPartition(): SpatialPartition = SpatialPartition()
    fun isDead(): Boolean = false
    fun getExtents(): Array<Any> = emptyArray()
}

class SpatialPartition {
    var mRenderByGroup: Boolean = true
    var mPartitionType: Int = 0

    fun asBridge(): Any? = null
}
