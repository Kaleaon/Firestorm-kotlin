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
            System.err.println("StandardBumpmap: clear gStandardBumpmapList entries not yet implemented")
        }

        fun addstandard() {
            System.err.println("StandardBumpmap: addstandard not yet implemented")
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
        // no-op: sRenderTarget.release(); StandardBumpmap.clear()
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
        System.err.println("BumpImageList: restoreGL not yet implemented")
    }

    fun updateImages() {
        // no-op: walk brightness/darkness entries; if image has GL texture: destroyGLTexture / erase from map
    }

    fun getBrightnessDarknessImage(srcImage: ViewerFetchedTexture?, bumpCode: UByte): ViewerTexture? {
        // no-op: brightness/darkness image lookup not yet implemented
        return null
    }

    fun addTextureStats(bump: UByte, baseImageId: String, virtualSize: Float) {
        // no-op: addTextureStats not yet implemented
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
            // no-op: generateNormalMapFromAlpha; setExplicitFormat GL_RGBA; createGLTexture
        }

        fun generateNormalMapFromAlpha(src: Any?, nrmImage: Any?) {
            // no-op: generateNormalMapFromAlpha not yet implemented
        }

        private fun onSourceUpdated(src: ViewerTexture?, bumpCode: BumpEffect) {
            // no-op: onSourceUpdated GL render pass not yet implemented
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
            // no-op: bindCubeMap not yet implemented
        }

        fun unbindCubeMap(shader: GLSLShader?, shaderLevel: Int, diffuseChannelRef: IntArray, cubeChannelRef: IntArray) {
            // no-op: unbindCubeMap not yet implemented
        }

        fun bindBumpMap(params: DrawInfo, channel: Int = -2): Boolean {
            return bindBumpMap(params.bump, params.texture, channel)
        }

        fun bindBumpMap(face: Face, channel: Int = -2): Boolean {
            val te = face.getTextureEntry() ?: return false
            return bindBumpMap(te.getBumpmap(), face.getTexture(), channel)
        }

        private fun bindBumpMap(bumpCode: UByte, texture: ViewerTexture?, channel: Int): Boolean {
            // no-op: bindBumpMap not yet implemented
            return false
        }
    }

    override fun getVertexDataMask(): UInt = sVertexMask
    override fun isDead(): Boolean = false

    override fun prerender() {
        // no-op: mShaderLevel update not yet implemented
    }

    fun beginFullbrightShiny() {
        System.err.println("DrawPoolBump: beginFullbrightShiny not yet implemented")
    }

    fun renderFullbrightShiny() {
        System.err.println("DrawPoolBump: renderFullbrightShiny not yet implemented")
    }

    fun endFullbrightShiny() {
        System.err.println("DrawPoolBump: endFullbrightShiny not yet implemented")
    }

    fun beginBump(pass: Int = RenderPass.PassType.PASS_BUMP.value) {
        System.err.println("DrawPoolBump: beginBump not yet implemented")
    }

    fun renderBump(pass: Int = RenderPass.PassType.PASS_BUMP.value) {
        System.err.println("DrawPoolBump: renderBump not yet implemented")
    }

    fun endBump(pass: Int = RenderPass.PassType.PASS_BUMP.value) {
        System.err.println("DrawPoolBump: endBump not yet implemented")
    }

    override fun renderGroup(group: SpatialGroup, type: UInt, texture: Boolean) {
        val drawInfo = group.mDrawMap[type.toInt()] ?: return
        for (params in drawInfo) {
            RenderPass.applyModelMatrix(params)
            // no-op: params.vertexBuffer?.setBuffer(); drawRange TRIANGLES not yet implemented
        }
    }

    override fun getNumDeferredPasses(): Int = 1

    override fun renderDeferred(pass: Int) {
        System.err.println("DrawPoolBump: renderDeferred not yet implemented")
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
        System.err.println("DrawPoolBump: pushBumpBatches not yet implemented")
    }
}
