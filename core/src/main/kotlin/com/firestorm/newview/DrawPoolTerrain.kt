package com.firestorm.newview

import kotlin.math.abs

class DrawPoolTerrain(private var texturep: ViewerTexture) : FacePool(DrawPool.PoolType.TERRAIN.value.toUInt()) {

    companion object {
        private const val DETAIL_SCALE = 1f / 16f

        var pbrDetailMode: Int = 0
        var detailScale: Float = DETAIL_SCALE
        var pbrDetailScale: Float = DETAIL_SCALE

        const val TERRAIN_MATERIAL_COUNT = 4

        const val VERTEX_DATA_MASK: UInt =
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_NORMAL or
            VertexBufferFlags.MAP_TANGENT or
            VertexBufferFlags.MAP_TEXCOORD0 or
            VertexBufferFlags.MAP_TEXCOORD1
    }

    var alphaRampImagep: ViewerTexture? = null
    var alphaRampImage2Dp: ViewerTexture? = null
    var alphaNoiseImagep: ViewerTexture? = null

    init {
        System.err.println("DrawPoolTerrain: load RenderTerrainScale/PBRScale/PBRDetail settings; fetch alpha ramp textures; set texture boost level not yet implemented")
    }

    override fun getVertexDataMask(): UInt {
        // no-op
        return VERTEX_DATA_MASK
    }

    override fun prerender() {
        System.err.println("DrawPoolTerrain: read RenderTerrainPBRDetail setting into pbrDetailMode not yet implemented")
    }

    override fun getNumDeferredPasses(): Int = 1

    override fun beginDeferredPass(pass: Int) {
        // no-op
    }

    override fun endDeferredPass(pass: Int) {
        // no-op
    }

    override fun renderDeferred(pass: Int) {
        if (drawFace.isEmpty()) return
        boostTerrainDetailTextures()
        renderFullShader()
        System.err.println("DrawPoolTerrain: check ShowParcelOwners setting; call hilightParcelOwners if true not yet implemented")
    }

    override fun getNumShadowPasses(): Int = 1

    override fun beginShadowPass(pass: Int) {
        // no-op
    }

    override fun endShadowPass(pass: Int) {
        // no-op
    }

    override fun renderShadow(pass: Int) {
        if (drawFace.isEmpty()) return
        drawLoop()
    }

    override fun dirtyTextures(textures: Set<ViewerFetchedTexture>) {
        // no-op
    }

    override fun getTexture(): ViewerTexture = texturep

    override fun getDebugTexture(): ViewerTexture = texturep

    fun getDebugColor(): FloatArray = floatArrayOf(0f, 0f, 1f)

    private fun boostTerrainDetailTextures() {
        // no-op
    }

    private fun drawLoop() {
        for (face in drawFace) {
            RenderPass.applyModelMatrix(null)
            face.renderIndexed()
        }
    }

    private fun renderFullShader() {
        // no-op
    }

    private fun renderFullShaderTextures() {
        // no-op
    }

    private fun renderFullShaderPBR(useLocalMaterials: Boolean = false) {
        // no-op
    }

    private fun renderSimple() {
        // no-op
    }

    private fun renderOwnership() {
        // no-op
    }

    private fun hilightParcelOwners() {
        // no-op
    }

    private fun renderFull2TU() {
        // no-op
    }

    private fun renderFull4TU() {
        // no-op
    }
}
