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
        TODO("APR: use JVM equivalent — load RenderTerrainScale/PBRScale/PBRDetail settings; fetch alpha ramp textures; set texture boost level")
    }

    override fun getVertexDataMask(): UInt {
        TODO("GPU: return vertex mask based on shadow render mode and bound shader")
    }

    override fun prerender() {
        TODO("APR: use JVM equivalent — read RenderTerrainPBRDetail setting into pbrDetailMode")
    }

    override fun getNumDeferredPasses(): Int = 1

    override fun beginDeferredPass(pass: Int) {
        TODO("GPU: LLFacePool.beginRenderPass(pass)")
    }

    override fun endDeferredPass(pass: Int) {
        TODO("GPU: LLFacePool.endRenderPass(pass); unbind sShader")
    }

    override fun renderDeferred(pass: Int) {
        if (drawFace.isEmpty()) return
        boostTerrainDetailTextures()
        renderFullShader()
        TODO("APR: use JVM equivalent — check ShowParcelOwners setting; call hilightParcelOwners if true")
    }

    override fun getNumShadowPasses(): Int = 1

    override fun beginShadowPass(pass: Int) {
        TODO("GPU: LLFacePool.beginRenderPass; unbind tex unit 0; bind gDeferredShadowProgram; set SUN_UP_FACTOR uniform")
    }

    override fun endShadowPass(pass: Int) {
        TODO("GPU: LLFacePool.endRenderPass; unbind gDeferredShadowProgram")
    }

    override fun renderShadow(pass: Int) {
        if (drawFace.isEmpty()) return
        drawLoop()
    }

    override fun dirtyTextures(textures: Set<ViewerFetchedTexture>) {
        TODO("GPU: if mTexturep is in textures set, mark all reference drawables as textured in pipeline")
    }

    override fun getTexture(): ViewerTexture = texturep

    override fun getDebugTexture(): ViewerTexture = texturep

    fun getDebugColor(): FloatArray = floatArrayOf(0f, 0f, 1f)

    private fun boostTerrainDetailTextures() {
        TODO("GPU: get region from first draw face, get VLComposition, call boost()")
    }

    private fun drawLoop() {
        for (face in drawFace) {
            RenderPass.applyModelMatrix(null)
            face.renderIndexed()
        }
    }

    private fun renderFullShader() {
        TODO("GPU: select texture or PBR shader based on region composition material type; bind shader; call renderFullShaderTextures or renderFullShaderPBR")
    }

    private fun renderFullShaderTextures() {
        TODO("GPU: bind four detail textures and alpha ramp to shader texture units; set object plane uniforms; call drawLoop; unbind all texture units")
    }

    private fun renderFullShaderPBR(useLocalMaterials: Boolean = false) {
        TODO("GPU: bind PBR base color/normal/metalrough/emissive textures per material slot; upload texture transforms, GLTF color/metallic/roughness/emissive uniforms; call drawLoop; unbind all texture units")
    }

    private fun renderSimple() {
        TODO("GPU: bind base texture; set object plane uniforms at 1/256 scale; call drawLoop; restore texture unit 0")
    }

    private fun renderOwnership() {
        TODO("GPU: bind parcel overlay texture; push texture matrix scaled by 257/256; render all draw faces; pop texture matrix")
    }

    private fun hilightParcelOwners() {
        TODO("GPU: bind gDeferredHighlightProgram; polygon offset -1,-1; renderOwnership; restore previous shader")
    }

    private fun renderFull2TU() {
        TODO("GPU: four-pass 2-TU blend of detail textures 0-3 using alpha ramp; restore blend state and texture units")
    }

    private fun renderFull4TU() {
        TODO("GPU: two-pass 4-TU blend of detail textures 0-3 using alpha ramp; restore blend state and texture units")
    }
}
