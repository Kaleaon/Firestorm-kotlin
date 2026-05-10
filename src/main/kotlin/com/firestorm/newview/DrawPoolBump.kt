package com.firestorm.newview

import kotlin.math.sqrt

enum class BumpEffect(val code: Int) {
    NO_BUMP(0),
    BRIGHTNESS(1),
    DARKNESS(2),
    STANDARD_0(3);

    companion object {
        const val COUNT: Int = 4
    }
}

class StandardBumpmap(val label: String = "") {
    var image: Any? = null

    companion object {
        var sStandardBumpmapCount: UInt = 0u

        fun clear() {
            TODO("APR: use JVM equivalent to clear gStandardBumpmapList entries")
        }

        fun addstandard() {
            TODO("APR: use JVM equivalent to read std_bump.ini and load bump textures via texture manager")
        }

        fun shutdown() {
            destroyGL()
        }

        fun destroyGL() {
            clear()
        }

        fun restoreGL() {
            addstandard()
        }
    }
}

class BumpImageList {
    private val brightnessEntries: MutableMap<String, Any?> = mutableMapOf()
    private val darknessEntries: MutableMap<String, Any?> = mutableMapOf()

    fun init() {
        StandardBumpmap.restoreGL()
    }

    fun clear() {
        brightnessEntries.clear()
        darknessEntries.clear()
        TODO("GPU: sRenderTarget.release()")
        StandardBumpmap.clear()
    }

    fun shutdown() {
        clear()
        StandardBumpmap.shutdown()
    }

    fun destroyGL() {
        clear()
        StandardBumpmap.destroyGL()
    }

    fun restoreGL() {
        StandardBumpmap.restoreGL()
    }

    fun updateImages() {
        TODO("GPU: walk brightness/darkness entries, destroy GL textures for stale entries, erase them")
    }

    fun getBrightnessDarknessImage(srcImage: Any?, bumpCode: UByte): Any? {
        TODO("GPU: look up or generate brightness/darkness bump image for srcImage")
    }

    fun addTextureStats(bump: UByte, baseImageId: String, virtualSize: Float) {
        TODO("GPU: addTextureStats to the standard bumpmap image for the given bump code")
    }

    companion object {
        fun onSourceStandardLoaded(
            success: Boolean,
            srcVi: Any?,
            src: Any?,
            auxSrc: Any?,
            discardLevel: Int,
            final: Boolean,
            userData: Any?
        ) {
            if (!success) return
            TODO("GPU: generateNormalMapFromAlpha and upload as GL_RGBA texture")
        }

        fun generateNormalMapFromAlpha(src: Any?, nrmImage: Any?) {
            TODO("GPU: CPU-side normal map generation from alpha channel; write RGBA into nrmImage")
        }

        private fun onSourceUpdated(src: Any?, bumpCode: BumpEffect) {
            TODO("GPU: create/resize GL texture, run gNormalMapGenProgram via render target, generate mipmaps")
        }
    }
}

class DrawPoolBump protected constructor(type: UInt) : RenderPass(type) {

    var isShiny: Boolean = false
    private var mRigged: Boolean = false

    constructor() : this(DrawPool.POOL_BUMP) {
        shiny = false
    }

    companion object {
        var sVertexMask: UInt = VERTEX_MASK_SHINY

        val VERTEX_MASK_SHINY: UInt = VertexBuffer.MAP_VERTEX or VertexBuffer.MAP_NORMAL or VertexBuffer.MAP_COLOR
        val VERTEX_MASK_BUMP: UInt = VertexBuffer.MAP_VERTEX or VertexBuffer.MAP_TEXCOORD0 or VertexBuffer.MAP_TEXCOORD1

        private var shader: GlslShader? = null
        private var cubeChannel: Int = -1
        private var diffuseChannel: Int = -1
        private var bumpChannel: Int = -1
        private var shiny: Boolean = false

        fun numBumpPasses(): Int = 1

        fun bindCubeMap(shader: GlslShader?, shaderLevel: Int, diffuseChannelRef: IntArray, cubeChannelRef: IntArray) {
            TODO("GPU: bindCubeMap from sky cube map if !sReflectionProbesEnabled")
        }

        fun unbindCubeMap(shader: GlslShader?, shaderLevel: Int, diffuseChannelRef: IntArray, cubeChannelRef: IntArray) {
            TODO("GPU: unbindCubeMap disable env map texture")
        }

        fun bindBumpMap(params: DrawInfo, channel: Int = -2): Boolean {
            return bindBumpMap(params.mBump, params.mTexture, channel)
        }

        fun bindBumpMap(face: Face, channel: Int = -2): Boolean {
            val te = face.getTextureEntry() ?: return false
            return bindBumpMap(te.getBumpmap(), face.getTexture(), channel)
        }

        private fun bindBumpMap(bumpCode: UByte, texture: Any?, channel: Int): Boolean {
            TODO("GPU: resolve bump texture from gBumpImageList or gStandardBumpmapList; bind to channel")
        }
    }

    override fun getVertexDataMask(): UInt = sVertexMask

    override fun prerender() {
        TODO("GPU: mShaderLevel = ViewerShaderMgr.instance().getShaderLevel(SHADER_OBJECT)")
    }

    fun beginFullbrightShiny() {
        TODO("GPU: sVertexMask=SHINY|TEXCOORD0; bind gDeferredFullbrightShinyProgram (or HUD variant); " +
             "bind exposure map; bind cube map; set SHINY_ORIGIN uniform; bind reflection probes or envMat")
    }

    fun renderFullbrightShiny() {
        TODO("GPU: GL_BLEND enabled; push PASS_FULLBRIGHT_SHINY batches (rigged or static, indexed or not)")
    }

    fun endFullbrightShiny() {
        TODO("GPU: disable cube map; unbind reflection probes if needed; unbind shader; reset channels")
    }

    fun beginBump(pass: Int = RenderPass.PASS_BUMP) {
        TODO("GPU: sVertexMask=VERTEX_MASK_BUMP; bind gObjectBumpProgram (rigged variant if needed); " +
             "setSceneBlendType BT_MULT_X2")
    }

    fun renderBump(pass: UInt = RenderPass.PASS_BUMP.toUInt()) {
        TODO("GPU: depth test GL_LEQUAL no-write; GL_BLEND; polygon offset -1/-1; pushBumpBatches(pass)")
    }

    fun endBump(pass: UInt = RenderPass.PASS_BUMP.toUInt()) {
        TODO("GPU: GlslShader.unbind; setSceneBlendType BT_ALPHA")
    }

    override fun renderGroup(group: SpatialGroup, type: UInt, texture: Boolean) {
        val drawInfo = group.mDrawMap[type.toInt()] ?: return
        for (params in drawInfo) {
            RenderPass.applyModelMatrix(params)
            TODO("GPU: params.mVertexBuffer.setBuffer(); drawRange TRIANGLES")
        }
    }

    override fun getNumDeferredPasses(): Int = 1

    override fun renderDeferred(pass: Int) {
        TODO("GPU: for static+rigged: bind gDeferredBumpProgram; enable diffuse+bump channels; " +
             "iterate PASS_BUMP/PASS_BUMP_RIGGED; bindBumpMap; uploadMatrixPalette for rigged; pushBumpBatch")
    }

    override fun getNumPostDeferredPasses(): Int = 1

    override fun renderPostDeferred(pass: Int) {
        val numPasses = if (Pipeline.sRenderingHUDs) 1 else 2
        for (i in 0 until numPasses) {
            mRigged = (i == 1)
            beginFullbrightShiny()
            renderFullbrightShiny()
            endFullbrightShiny()
            beginBump()
            renderBump(RenderPass.PASS_POST_BUMP.toUInt())
            endBump()
        }
    }

    fun pushBumpBatches(type: UInt) {
        TODO("GPU: iterate render map for type (nudged +1 for rigged); bindBumpMap; " +
             "uploadMatrixPalette if rigged; pushBumpBatch")
    }
}

object DrawPool {
    const val POOL_BUMP: UInt = 6u
    const val POOL_ALPHA_PRE_WATER: UInt = 7u
    const val POOL_ALPHA_POST_WATER: UInt = 8u
    const val POOL_MATERIALS: UInt = 9u
    const val POOL_TREE: UInt = 10u
    const val POOL_AVATAR: UInt = 11u
}

object Pipeline {
    var sRenderDeferred: Boolean = false
    var sImpostorRender: Boolean = false
    var sRenderingHUDs: Boolean = false
    var sUnderWaterRender: Boolean = false
    var sReflectionRender: Boolean = false
    var sReflectionProbesEnabled: Boolean = false
    var RenderDepthOfField: Boolean = false
    var RenderAvatarCloth: Boolean = false
    var sRenderParticles: Boolean = true

    var mTextureMatrixOps: Int = 0

    fun isWaterClip(): Boolean { TODO("GPU: isWaterClip") }
    fun enableLightsDynamic() { TODO("GPU: enableLightsDynamic") }
    fun enableLightsFullbright() { TODO("GPU: enableLightsFullbright") }
    fun bindDeferredShader(shader: GlslShader) { TODO("GPU: bindDeferredShader") }
    fun bindDeferredShaderFast(shader: GlslShader) { TODO("GPU: bindDeferredShaderFast") }
    fun unbindDeferredShader(shader: GlslShader) { TODO("GPU: unbindDeferredShader") }
    fun bindReflectionProbes(shader: GlslShader) { TODO("GPU: bindReflectionProbes") }
    fun unbindReflectionProbes(shader: GlslShader) { TODO("GPU: unbindReflectionProbes") }
    fun setEnvMat(shader: GlslShader) { TODO("GPU: setEnvMat") }
    fun beginRenderMap(type: Int): Iterator<DrawInfo> { TODO("GPU: beginRenderMap") }
    fun endRenderMap(type: Int): Iterator<DrawInfo> { TODO("GPU: endRenderMap") }
    fun hasRenderType(type: Int): Boolean { TODO("GPU: hasRenderType") }
    fun beginAlphaGroups(): Iterator<SpatialGroup> { TODO("GPU: beginAlphaGroups") }
    fun endAlphaGroups(): Iterator<SpatialGroup> { TODO("GPU: endAlphaGroups") }
    fun beginRiggedAlphaGroups(): Iterator<SpatialGroup> { TODO("GPU: beginRiggedAlphaGroups") }
    fun endRiggedAlphaGroups(): Iterator<SpatialGroup> { TODO("GPU: endRiggedAlphaGroups") }
    fun shadersLoaded(): Boolean { TODO("GPU: shadersLoaded") }
}
