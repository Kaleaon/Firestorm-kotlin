package com.firestorm.newview

class DrawPoolWater : FacePool(DrawPool.PoolType.WATER.value.toUInt()) {

    companion object {
        var skipScreenCopy: Boolean = false
        var needsReflectionUpdate: Boolean = true
        var needsDistortionUpdate: Boolean = true
        var waterFogEnd: Float = 0f

        const val VERTEX_DATA_MASK: UInt =
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_NORMAL or
            VertexBufferFlags.MAP_TEXCOORD0
    }

    private val waterImagep: Array<ViewerTexture?> = arrayOfNulls(2)
    private val waterNormp: Array<ViewerTexture?> = arrayOfNulls(2)
    private var opaqueWaterImagep: ViewerTexture? = null

    // Cached setting to avoid per-frame setting lookup.
    private var renderWaterMipNormal: Boolean = false

    init {
        onRenderWaterMipNormalChanged()
        // Registering the settings change listener uses platform signal machinery.
        // Attach via your settings-change mechanism to call onRenderWaterMipNormalChanged().
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    fun setTransparentTextures(transparentTextureId: String, nextTransparentTextureId: String) {
        TODO("APR: use JVM equivalent — fetch transparent water textures from texture manager; addTextureStats 1024*1024")
    }

    fun setOpaqueTexture(opaqueTextureId: String) {
        TODO("APR: use JVM equivalent — fetch opaque water texture from texture manager; addTextureStats 1024*1024")
    }

    fun setNormalMaps(normalMapId: String, nextNormalMapId: String) {
        TODO("APR: use JVM equivalent — fetch normal map textures from texture manager; addTextureStats 1024*1024")
    }

    override fun prerender() {
        TODO("GPU: query cube map shader level and store in shaderLevel")
    }

    override fun getNumPostDeferredPasses(): Int {
        TODO("GPU: return 1 if camera origin Z < 1024, else 0")
    }

    override fun beginPostDeferredPass(pass: Int) {
        TODO("GPU: setColorMask true/true; if transparent water: copy framebuffer to water distortion render target using gCopyDepthProgram and screen triangle VB")
    }

    override fun renderPostDeferred(pass: Int) {
        TODO("GPU: disable blend; collect environment light/sky/water settings; select underwater or surface water shader; bind deferred shader with water distortion target; bind normal map textures with blend factor; set all water uniforms (fog, wave dirs, specular, refraction scale, exposure, tonemap); disable cull face; pushWaterPlanes(0); unbind deferred shader; restore color mask")
    }

    fun pushWaterPlanes(pass: Int) {
        for (face in drawFace) {
            val water = face.getViewerObject() as? WaterObject
            face.renderIndexed()
            if (water != null && !water.isEdgePatch) {
                needsReflectionUpdate = true
                needsDistortionUpdate = true
            }
        }
    }

    override fun getDebugTexture(): ViewerTexture? {
        TODO("APR: use JVM equivalent — fetch IMG_SMOKE texture")
    }

    fun getDebugColor(): FloatArray = floatArrayOf(0f, 1f, 1f)

    private fun onRenderWaterMipNormalChanged() {
        TODO("APR: use JVM equivalent — read RenderWaterMipNormal boolean setting into renderWaterMipNormal")
    }

    private fun renderOpaqueLegacyWater() {
        TODO("GPU: render opaque water surface using legacy shader path")
    }
}


open class WaterObject : ViewerObject() {
    open val isEdgePatch: Boolean = false
}

open class ViewerObject {
    open fun getRegion(): Any? = null
}

fun Face.getViewerObject(): ViewerObject? {
    TODO("GPU: return the viewer object associated with this face's drawable")
}
