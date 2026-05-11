package com.firestorm.newview

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
    var image: ViewerFetchedTexture? = null

    companion object {
        var sStandardBumpmapCount: UInt = 0u

        fun clear() {
            TODO("APR: use JVM equivalent — clear gStandardBumpmapList entries (label and image)")
        }

        fun addstandard() {
            TODO("APR: use JVM equivalent — read std_bump.ini; add None/Brightness/Darkness entries; " +
                 "load bump image UUIDs via texture manager; register loaded callbacks; forceToSaveRawImage")
        }

        fun shutdown() { destroyGL() }

        fun destroyGL() { clear() }

        fun restoreGL() { addstandard() }
    }
}

class BumpImageList {
    private val brightnessEntries: MutableMap<String, ViewerTexture?> = mutableMapOf()
    private val darknessEntries: MutableMap<String, ViewerTexture?> = mutableMapOf()

    fun init() {
        StandardBumpmap.restoreGL()
    }

    fun clear() {
        brightnessEntries.clear()
        darknessEntries.clear()
        TODO("GPU: sRenderTarget.release(); StandardBumpmap.clear()")
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
        TODO("APR: use JVM equivalent — guard !textureListInitialized; StandardBumpmap.restoreGL()")
    }

    fun updateImages() {
        TODO("GPU: walk brightness/darkness entries; if image has GL texture: " +
             "if !getBoundRecently: destroyGLTexture; if should destroy: erase from map")
    }

    fun getBrightnessDarknessImage(srcImage: ViewerFetchedTexture?, bumpCode: UByte): ViewerTexture? {
        TODO("GPU: select brightness or darkness map; look up by srcImage ID; " +
             "if missing or resolution mismatch: call onSourceUpdated; return map entry")
    }

    fun addTextureStats(bump: UByte, baseImageId: String, virtualSize: Float) {
        TODO("GPU: mask bump & TEM_BUMP_MASK; get gStandardBumpmapList image; addTextureStats(virtualSize)")
    }

    companion object {
        fun onSourceStandardLoaded(
            success: Boolean,
            srcVi: ViewerFetchedTexture?,
            src: Any?,
            auxSrc: Any?,
            discardLevel: Int,
            final: Boolean,
            userData: Any?
        ) {
            if (!success) return
            TODO("GPU: if sRenderDeferred: generateNormalMapFromAlpha; setExplicitFormat GL_RGBA; createGLTexture")
        }

        fun generateNormalMapFromAlpha(src: Any?, nrmImage: Any?) {
            TODO("GPU: CPU-side normal map from alpha channel heightfield; write RGBA normals into nrmImage")
        }

        private fun onSourceUpdated(src: ViewerTexture?, bumpCode: BumpEffect) {
            TODO("GPU: ensure entry exists; if size changed: " +
                 "setExplicitFormat GL_RGBA; setSize; setUseMipMaps; createGLTexture; " +
                 "setColorAttachment to render target; bind gNormalMapGenProgram; " +
                 "set norm_scale/stepX/stepY/bump_code uniforms; bind src texture; draw TRIANGLE_STRIP fullscreen quad; " +
                 "flush render target; glGenerateMipmap; unbind")
        }
    }
}

class DrawPoolBump private constructor(type: UInt) : RenderPass(type) {

    var isShiny: Boolean = false
    private var mRigged: Boolean = false

    constructor() : this(DrawPool.PoolType.BUMP.value.toUInt())

    companion object {
        var sVertexMask: UInt = VERTEX_MASK_SHINY

        val VERTEX_MASK_SHINY: UInt = VertexBufferFlags.MAP_VERTEX or VertexBufferFlags.MAP_NORMAL or VertexBufferFlags.MAP_COLOR
        val VERTEX_MASK_BUMP: UInt = VertexBufferFlags.MAP_VERTEX or VertexBufferFlags.MAP_TEXCOORD0 or VertexBufferFlags.MAP_TEXCOORD1

        private var shader: GLSLShader? = null
        private var cubeChannel: Int = -1
        private var diffuseChannel: Int = -1
        private var bumpChannel: Int = -1
        private var shiny: Boolean = false

        fun numBumpPasses(): Int = 1

        fun bindCubeMap(shader: GLSLShader?, shaderLevel: Int, diffuseChannelRef: IntArray, cubeChannelRef: IntArray) {
            TODO("GPU: get sky cube map; if exists && !sReflectionProbesEnabled: " +
                 "if shaderLevel>1: set cube matrix 1, enable ENVIRONMENT_MAP cube, enable DIFFUSE_MAP; " +
                 "else: set cube matrix 0, enable ENVIRONMENT_MAP, diffuseChannel=-1; " +
                 "bind cube map; activate texunit 0; set SHINY_ORIGIN uniform")
        }

        fun unbindCubeMap(shader: GLSLShader?, shaderLevel: Int, diffuseChannelRef: IntArray, cubeChannelRef: IntArray) {
            TODO("GPU: if cube map exists && !sReflectionProbesEnabled: " +
                 "if shaderLevel>1: disableTexture ENVIRONMENT_MAP; if diffuseChannel!=0: disableTexture DIFFUSE_MAP; " +
                 "cube_map.disable(); restoreMatrix()")
        }

        fun bindBumpMap(params: DrawInfo, channel: Int = -2): Boolean {
            return bindBumpMap(params.bump, params.texture, channel)
        }

        fun bindBumpMap(face: Face, channel: Int = -2): Boolean {
            val te = face.getTextureEntry() ?: return false
            return bindBumpMap(te.getBumpmap(), face.getTexture(), channel)
        }

        private fun bindBumpMap(bumpCode: UByte, texture: ViewerTexture?, channel: Int): Boolean {
            TODO("GPU: cast to ViewerFetchedTexture; switch bumpCode: " +
                 "NO_BUMP: return false; BRIGHTNESS/DARKNESS: getBrightnessDarknessImage; " +
                 "default: gStandardBumpmapList[bumpCode].image; " +
                 "if bump != null: channel==-2: bindFast to units 1+0; else bind to channel; return true")
        }
    }

    override fun getVertexDataMask(): UInt = sVertexMask
    override fun isDead(): Boolean = false

    override fun prerender() {
        TODO("GPU: mShaderLevel = ViewerShaderMgr.instance().getShaderLevel(SHADER_OBJECT)")
    }

    fun beginFullbrightShiny() {
        TODO("GPU: sVertexMask=SHINY|TEXCOORD0; " +
             "shader=gDeferredFullbrightShinyProgram (or HUD variant); if mRigged: use mRiggedVariant; " +
             "enable EXPOSURE_MAP; bind cube map if available && !sReflectionProbesEnabled; " +
             "build mat from gGLModelView; shader.bind(); set SHINY_ORIGIN uniform; " +
             "if sReflectionProbesEnabled: bindReflectionProbes else setEnvMat; " +
             "if shaderLevel>1: diffuseChannel=0; shiny=true")
    }

    fun renderFullbrightShiny() {
        TODO("GPU: GL_BLEND enabled; " +
             "if shaderLevel>1: pushBatches/pushRiggedBatches with batch textures; " +
             "else: pushBatches/pushRiggedBatches without batch textures")
    }

    fun endFullbrightShiny() {
        TODO("GPU: if cube map && !sReflectionProbesEnabled: cube_map.disable(); " +
             "if shader has reflection probes: unbindReflectionProbes; shader.unbind(); " +
             "diffuseChannel=-1; cubeChannel=0; shiny=false")
    }

    fun beginBump(pass: Int = RenderPass.PassType.PASS_BUMP.value) {
        TODO("GPU: sVertexMask=VERTEX_MASK_BUMP; " +
             "shader=gObjectBumpProgram (rigged variant if mRigged); bind; " +
             "setSceneBlendType BT_MULT_X2")
    }

    fun renderBump(pass: Int = RenderPass.PassType.PASS_BUMP.value) {
        TODO("GPU: depth GL_LEQUAL no-write; GL_BLEND; diffuseColor4f(1,1,1,1); " +
             "polygon offset -1/-1; pushBumpBatches(pass)")
    }

    fun endBump(pass: Int = RenderPass.PassType.PASS_BUMP.value) {
        TODO("GPU: GLSLShader.unbindAll(); setSceneBlendType BT_ALPHA")
    }

    override fun renderGroup(group: SpatialGroup, type: UInt, texture: Boolean) {
        val drawInfo = group.mDrawMap[type.toInt()] ?: return
        for (params in drawInfo) {
            RenderPass.applyModelMatrix(params)
            TODO("GPU: params.vertexBuffer?.setBuffer(); drawRange TRIANGLES")
        }
    }

    override fun getNumDeferredPasses(): Int = 1

    override fun renderDeferred(pass: Int) {
        TODO("GPU: shiny=true; for static+rigged passes: " +
             "gDeferredBumpProgram.bind(rigged); enable diffuse+bump channels; unbind both texunits; " +
             "select PASS_BUMP or PASS_BUMP_RIGGED; iterate render map; " +
             "per DrawInfo: setMinimumAlpha(alphaMaskCutoff); bindBumpMap; " +
             "if rigged: uploadMatrixPalette; pushBumpBatch(texture=true, batchTextures=false); " +
             "disableTexture diffuse+bump; unbind; activate texunit 0; shiny=false")
    }

    override fun getNumPostDeferredPasses(): Int = 1

    override fun renderPostDeferred(pass: Int) {
        val numPasses = if (Pipeline.sRenderingHUDs) 1 else 2
        for (i in 0 until numPasses) {
            mRigged = (i == 1)
            beginFullbrightShiny()
            renderFullbrightShiny()
            endFullbrightShiny()
            beginBump(RenderPass.PassType.PASS_POST_BUMP.value)
            renderBump(RenderPass.PassType.PASS_POST_BUMP.value)
            endBump(RenderPass.PassType.PASS_POST_BUMP.value)
        }
    }

    fun pushBumpBatches(type: Int) {
        TODO("GPU: if mRigged: type+=1 (rigged variant); iterate render map; " +
             "bindBumpMap; if rigged: uploadMatrixPalette (skip if failed); pushBumpBatch(texture=false)")
    }
}
