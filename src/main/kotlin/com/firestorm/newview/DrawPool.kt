package com.firestorm.newview

abstract class DrawPool(val type: UInt) {

    enum class PoolType(val value: Int) {
        SKY(1),
        WATEREXCLUSION(2),
        WL_SKY(3),
        SIMPLE(4),
        FULLBRIGHT(5),
        BUMP(6),
        MATERIALS(7),
        GLTF_PBR(8),
        TERRAIN(9),
        GRASS(10),
        GLTF_PBR_ALPHA_MASK(11),
        TREE(12),
        ALPHA_MASK(13),
        FULLBRIGHT_ALPHA_MASK(14),
        AVATAR(15),
        CONTROL_AV(16),
        GLOW(17),
        ALPHA_PRE_WATER(18),
        VOIDWATER(19),
        WATER(20),
        ALPHA_POST_WATER(21),
        ALPHA(22),
        NUM_POOL_TYPES(23);

        companion object {
            fun fromValue(v: Int): PoolType? = entries.firstOrNull { it.value == v }
        }
    }

    companion object {
        var numDrawPools: Int = 0
            private set

        fun createPool(type: UInt, tex0: ViewerTexture? = null): DrawPool {
            return when (type.toInt()) {
                PoolType.SIMPLE.value -> DrawPoolSimple()
                PoolType.GRASS.value -> DrawPoolGrass()
                PoolType.ALPHA_MASK.value -> DrawPoolAlphaMask()
                PoolType.FULLBRIGHT_ALPHA_MASK.value -> DrawPoolFullbrightAlphaMask()
                PoolType.FULLBRIGHT.value -> DrawPoolFullbright()
                PoolType.GLOW.value -> DrawPoolGlow()
                PoolType.ALPHA_PRE_WATER.value -> DrawPoolAlpha(PoolType.ALPHA_PRE_WATER.value.toUInt())
                PoolType.ALPHA_POST_WATER.value -> DrawPoolAlpha(PoolType.ALPHA_POST_WATER.value.toUInt())
                PoolType.AVATAR.value, PoolType.CONTROL_AV.value -> DrawPoolAvatar(type)
                PoolType.TERRAIN.value -> DrawPoolTerrain(tex0 ?: error("tex0 required for POOL_TERRAIN"))
                PoolType.SKY.value -> DrawPoolSky()
                PoolType.VOIDWATER.value, PoolType.WATER.value -> DrawPoolWater()
                PoolType.BUMP.value -> DrawPoolBump()
                PoolType.MATERIALS.value -> DrawPoolMaterials()
                PoolType.WL_SKY.value -> DrawPoolWLSky()
                PoolType.GLTF_PBR.value -> DrawPoolGLTFPBR()
                PoolType.GLTF_PBR_ALPHA_MASK.value -> DrawPoolGLTFPBR(PoolType.GLTF_PBR_ALPHA_MASK.value.toUInt())
                PoolType.WATEREXCLUSION.value -> DrawPoolWaterExclusion()
                else -> error("Unknown draw pool type: $type")
            }
        }
    }

    val id: Int
    var shaderLevel: Int = 0
    var skipRender: Boolean = false

    init {
        numDrawPools++
        id = numDrawPools
    }

    abstract fun isDead(): Boolean

    open fun getDebugTexture(): ViewerTexture? = null

    open fun beginRenderPass(pass: Int) {}
    open fun endRenderPass(pass: Int) {
        TODO("GPU: activate texture unit 0")
    }
    open fun getNumPasses(): Int = 1

    open fun beginDeferredPass(pass: Int) {}
    open fun endDeferredPass(pass: Int) {}
    open fun getNumDeferredPasses(): Int = 0
    open fun renderDeferred(pass: Int = 0) {}

    open fun beginPostDeferredPass(pass: Int) {}
    open fun endPostDeferredPass(pass: Int) {}
    open fun getNumPostDeferredPasses(): Int = 0
    open fun renderPostDeferred(pass: Int = 0) {}

    open fun beginShadowPass(pass: Int) {}
    open fun endShadowPass(pass: Int) {}
    open fun getNumShadowPasses(): Int = 0
    open fun renderShadow(pass: Int = 0) {}

    open fun render(pass: Int = 0) {}
    open fun prerender() {}
    open fun getVertexDataMask(): UInt = 0u
    open fun verify(): Boolean = true
    open fun getShaderLevel(): Int = shaderLevel

    abstract fun getTexture(): ViewerTexture?
    open fun isFacePool(): Boolean = false
    abstract fun resetDrawOrders()
    open fun pushFaceGeometry() {}
}


open class RenderPass(type: UInt) : DrawPool(type) {

    enum class PassType(val value: Int) {
        PASS_SIMPLE(DrawPool.PoolType.NUM_POOL_TYPES.value),
        PASS_SIMPLE_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 1),
        PASS_GRASS(DrawPool.PoolType.NUM_POOL_TYPES.value + 2),
        PASS_FULLBRIGHT(DrawPool.PoolType.NUM_POOL_TYPES.value + 3),
        PASS_FULLBRIGHT_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 4),
        PASS_INVISIBLE(DrawPool.PoolType.NUM_POOL_TYPES.value + 5),
        PASS_INVISIBLE_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 6),
        PASS_INVISI_SHINY(DrawPool.PoolType.NUM_POOL_TYPES.value + 7),
        PASS_INVISI_SHINY_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 8),
        PASS_FULLBRIGHT_SHINY(DrawPool.PoolType.NUM_POOL_TYPES.value + 9),
        PASS_FULLBRIGHT_SHINY_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 10),
        PASS_SHINY(DrawPool.PoolType.NUM_POOL_TYPES.value + 11),
        PASS_SHINY_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 12),
        PASS_BUMP(DrawPool.PoolType.NUM_POOL_TYPES.value + 13),
        PASS_BUMP_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 14),
        PASS_POST_BUMP(DrawPool.PoolType.NUM_POOL_TYPES.value + 15),
        PASS_POST_BUMP_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 16),
        PASS_MATERIAL(DrawPool.PoolType.NUM_POOL_TYPES.value + 17),
        PASS_MATERIAL_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 18),
        PASS_MATERIAL_ALPHA(DrawPool.PoolType.NUM_POOL_TYPES.value + 19),
        PASS_MATERIAL_ALPHA_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 20),
        PASS_MATERIAL_ALPHA_MASK(DrawPool.PoolType.NUM_POOL_TYPES.value + 21),
        PASS_MATERIAL_ALPHA_MASK_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 22),
        PASS_MATERIAL_ALPHA_EMISSIVE(DrawPool.PoolType.NUM_POOL_TYPES.value + 23),
        PASS_MATERIAL_ALPHA_EMISSIVE_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 24),
        PASS_SPECMAP(DrawPool.PoolType.NUM_POOL_TYPES.value + 25),
        PASS_SPECMAP_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 26),
        PASS_SPECMAP_BLEND(DrawPool.PoolType.NUM_POOL_TYPES.value + 27),
        PASS_SPECMAP_BLEND_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 28),
        PASS_SPECMAP_MASK(DrawPool.PoolType.NUM_POOL_TYPES.value + 29),
        PASS_SPECMAP_MASK_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 30),
        PASS_SPECMAP_EMISSIVE(DrawPool.PoolType.NUM_POOL_TYPES.value + 31),
        PASS_SPECMAP_EMISSIVE_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 32),
        PASS_NORMMAP(DrawPool.PoolType.NUM_POOL_TYPES.value + 33),
        PASS_NORMMAP_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 34),
        PASS_NORMMAP_BLEND(DrawPool.PoolType.NUM_POOL_TYPES.value + 35),
        PASS_NORMMAP_BLEND_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 36),
        PASS_NORMMAP_MASK(DrawPool.PoolType.NUM_POOL_TYPES.value + 37),
        PASS_NORMMAP_MASK_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 38),
        PASS_NORMMAP_EMISSIVE(DrawPool.PoolType.NUM_POOL_TYPES.value + 39),
        PASS_NORMMAP_EMISSIVE_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 40),
        PASS_NORMSPEC(DrawPool.PoolType.NUM_POOL_TYPES.value + 41),
        PASS_NORMSPEC_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 42),
        PASS_NORMSPEC_BLEND(DrawPool.PoolType.NUM_POOL_TYPES.value + 43),
        PASS_NORMSPEC_BLEND_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 44),
        PASS_NORMSPEC_MASK(DrawPool.PoolType.NUM_POOL_TYPES.value + 45),
        PASS_NORMSPEC_MASK_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 46),
        PASS_NORMSPEC_EMISSIVE(DrawPool.PoolType.NUM_POOL_TYPES.value + 47),
        PASS_NORMSPEC_EMISSIVE_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 48),
        PASS_GLOW(DrawPool.PoolType.NUM_POOL_TYPES.value + 49),
        PASS_GLOW_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 50),
        PASS_GLTF_GLOW(DrawPool.PoolType.NUM_POOL_TYPES.value + 51),
        PASS_GLTF_GLOW_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 52),
        PASS_ALPHA(DrawPool.PoolType.NUM_POOL_TYPES.value + 53),
        PASS_ALPHA_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 54),
        PASS_ALPHA_MASK(DrawPool.PoolType.NUM_POOL_TYPES.value + 55),
        PASS_ALPHA_MASK_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 56),
        PASS_FULLBRIGHT_ALPHA_MASK(DrawPool.PoolType.NUM_POOL_TYPES.value + 57),
        PASS_FULLBRIGHT_ALPHA_MASK_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 58),
        PASS_ALPHA_INVISIBLE(DrawPool.PoolType.NUM_POOL_TYPES.value + 59),
        PASS_ALPHA_INVISIBLE_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 60),
        PASS_GLTF_PBR(DrawPool.PoolType.NUM_POOL_TYPES.value + 61),
        PASS_GLTF_PBR_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 62),
        PASS_GLTF_PBR_ALPHA_MASK(DrawPool.PoolType.NUM_POOL_TYPES.value + 63),
        PASS_GLTF_PBR_ALPHA_MASK_RIGGED(DrawPool.PoolType.NUM_POOL_TYPES.value + 64),
        NUM_RENDER_TYPES(DrawPool.PoolType.NUM_POOL_TYPES.value + 65);
    }

    companion object {
        fun lookupPassName(pass: UInt): String {
            return PassType.entries.firstOrNull { it.value == pass.toInt() }?.name ?: "Unknown pass"
        }

        fun applyModelMatrix(params: DrawInfo) {
            TODO("GPU: apply model matrix from DrawInfo")
        }

        fun applyModelMatrix(modelMatrix: FloatArray?) {
            TODO("GPU: load modelview matrix and multiply by modelMatrix")
        }

        fun uploadMatrixPalette(params: DrawInfo): Boolean {
            TODO("GPU: upload matrix palette from DrawInfo avatar/skinInfo")
        }

        fun uploadMatrixPalette(avatar: VOAvatar?, skinInfo: MeshSkinInfo?): Boolean {
            TODO("GPU: upload matrix palette to shader")
        }

        fun uploadMatrixPalette(
            avatar: VOAvatar?,
            skinInfo: MeshSkinInfo?,
            lastAvatar: Array<VOAvatar?>,
            lastMeshId: LongArray,
            skipLastSkin: BooleanArray
        ): Boolean {
            TODO("GPU: upload matrix palette with caching")
        }

        fun uploadMatrixPalette(
            avatar: VOAvatar?,
            skinInfo: MeshSkinInfo?,
            lastAvatar: Array<VOAvatar?>,
            lastMeshId: LongArray,
            lastAvatarShader: Array<GLSLShader?>,
            skipLastSkin: BooleanArray
        ): Boolean {
            TODO("GPU: upload matrix palette with shader caching")
        }

        fun pushGLTFBatch(params: DrawInfo) {
            TODO("GPU: bind GLTF material, set buffer, draw range triangles")
        }

        fun pushRiggedGLTFBatch(
            params: DrawInfo,
            lastAvatar: Array<VOAvatar?>,
            lastMeshId: LongArray,
            skipLastSkin: BooleanArray
        ) {
            TODO("GPU: upload matrix palette then pushGLTFBatch")
        }

        fun pushUntexturedGLTFBatch(params: DrawInfo) {
            TODO("GPU: set cull face, apply model matrix, set buffer, draw range triangles")
        }

        fun pushUntexturedRiggedGLTFBatch(
            params: DrawInfo,
            lastAvatar: Array<VOAvatar?>,
            lastMeshId: LongArray,
            skipLastSkin: BooleanArray
        ) {
            TODO("GPU: upload matrix palette then pushUntexturedGLTFBatch")
        }
    }

    override fun getDebugTexture(): ViewerTexture? = null
    override fun getTexture(): ViewerTexture? = null
    override fun isDead(): Boolean = false
    override fun resetDrawOrders() {}

    fun pushBatches(type: UInt, texture: Boolean = true, batchTextures: Boolean = false) {
        if (texture) {
            TODO("GPU: iterate pipeline render map for type, call pushBatch for each DrawInfo")
        } else {
            pushUntexturedBatches(type)
        }
    }

    fun pushUntexturedBatches(type: UInt) {
        TODO("GPU: iterate pipeline render map for type, call pushUntexturedBatch for each DrawInfo")
    }

    fun pushRiggedBatches(type: UInt, texture: Boolean = true, batchTextures: Boolean = false) {
        if (texture) {
            TODO("GPU: iterate pipeline render map with matrix palette upload, call pushBatch")
        } else {
            pushUntexturedRiggedBatches(type)
        }
    }

    fun pushUntexturedRiggedBatches(type: UInt) {
        TODO("GPU: iterate pipeline render map with matrix palette upload, call pushUntexturedBatch")
    }

    fun pushGLTFBatches(type: UInt) {
        TODO("GPU: iterate pipeline render map, call pushGLTFBatch for each DrawInfo")
    }

    fun pushUntexturedGLTFBatches(type: UInt) {
        TODO("GPU: iterate pipeline render map, call pushUntexturedGLTFBatch for each DrawInfo")
    }

    fun pushGLTFBatches(type: UInt, textured: Boolean) {
        if (textured) pushGLTFBatches(type) else pushUntexturedGLTFBatches(type)
    }

    fun pushRiggedGLTFBatches(type: UInt) {
        TODO("GPU: iterate pipeline render map with rigged GLTF batch dispatch")
    }

    fun pushRiggedGLTFBatches(type: UInt, textured: Boolean) {
        if (textured) pushRiggedGLTFBatches(type) else pushUntexturedRiggedGLTFBatches(type)
    }

    fun pushUntexturedRiggedGLTFBatches(type: UInt) {
        TODO("GPU: iterate pipeline render map with untextured rigged GLTF batch dispatch")
    }

    fun pushMaskBatches(type: UInt, texture: Boolean = true, batchTextures: Boolean = false) {
        TODO("GPU: iterate pipeline render map, set minimum alpha, call pushBatch")
    }

    fun pushRiggedMaskBatches(type: UInt, texture: Boolean = true, batchTextures: Boolean = false) {
        TODO("GPU: iterate pipeline render map, set minimum alpha, upload matrix palette, call pushBatch")
    }

    fun pushBatch(params: DrawInfo, texture: Boolean, batchTextures: Boolean = false) {
        TODO("GPU: apply model matrix, bind textures, set vertex buffer, drawRange TRIANGLES")
    }

    fun pushUntexturedBatch(params: DrawInfo) {
        TODO("GPU: apply model matrix, set vertex buffer, drawRange TRIANGLES")
    }

    fun pushBumpBatch(params: DrawInfo, texture: Boolean, batchTextures: Boolean = false) {
        TODO("GPU: push bump-mapped batch")
    }

    open fun renderGroup(group: SpatialGroup, type: UInt, texture: Boolean = true) {
        TODO("GPU: iterate group draw map for type, call pushBatch")
    }

    open fun renderRiggedGroup(group: SpatialGroup, type: UInt, texture: Boolean = true) {
        TODO("GPU: iterate group draw map with matrix palette upload, call pushBatch")
    }
}


open class FacePool(type: UInt) : DrawPool(type) {

    companion object {
        const val SHADER_LEVEL_SCATTERING = 2
    }

    val drawFace: MutableList<Face> = mutableListOf()
    val moveFace: MutableList<Face> = mutableListOf()
    val references: MutableList<Face> = mutableListOf()

    override fun isDead(): Boolean = references.isEmpty()

    override fun getTexture(): ViewerTexture? = null

    open fun dirtyTextures(textures: Set<ViewerFetchedTexture>) {}

    open fun enqueue(face: Face) {
        drawFace.add(face)
    }

    open fun addFace(face: Face): Boolean {
        addFaceReference(face)
        return true
    }

    open fun removeFace(face: Face): Boolean {
        removeFaceReference(face)
        val idx = drawFace.indexOf(face)
        if (idx >= 0) {
            drawFace[idx] = drawFace.last()
            drawFace.removeAt(drawFace.lastIndex)
        }
        return true
    }

    override fun verify(): Boolean {
        var ok = true
        for (face in drawFace) {
            if (face.pool !== this) {
                println("Face in wrong pool!")
                ok = false
            } else if (!face.verify()) {
                ok = false
            }
        }
        return ok
    }

    override fun resetDrawOrders() {
        drawFace.clear()
    }

    fun resetAll() {
        drawFace.clear()
        moveFace.clear()
        references.clear()
    }

    fun destroy() {
        if (references.isNotEmpty()) {
            println("${references.size} references left on deletion of draw pool!")
        }
    }

    fun addFaceReference(face: Face) {
        if (face.referenceIndex == -1) {
            face.referenceIndex = references.size
            references.add(face)
        }
    }

    fun removeFaceReference(face: Face) {
        val idx = face.referenceIndex
        if (idx != -1) {
            if (idx != references.size - 1) {
                val back = references.last()
                references[idx] = back
                back.referenceIndex = idx
            }
            references.removeAt(references.lastIndex)
        }
        face.referenceIndex = -1
    }

    override fun pushFaceGeometry() {
        for (face in drawFace) {
            face.renderIndexed()
        }
    }

    fun printDebugInfo() {
        println("Pool $this Type: $type")
    }

    override fun isFacePool(): Boolean = true

    class OverrideFaceColor(
        val pool: DrawPool,
        r: Float? = null, g: Float? = null, b: Float? = null, a: Float? = null
    ) : AutoCloseable {

        private val prevOverride: Boolean = sOverrideFaceColor

        companion object {
            var sOverrideFaceColor: Boolean = false
        }

        init {
            sOverrideFaceColor = true
            if (r != null && g != null && b != null && a != null) {
                setColor(r, g, b, a)
            }
        }

        fun setColor(r: Float, g: Float, b: Float, a: Float) {
            TODO("GPU: diffuseColor4f($r, $g, $b, $a)")
        }

        fun setColor(color: FloatArray) {
            TODO("GPU: diffuseColor4fv(color)")
        }

        fun setColorU(color: ByteArray) {
            TODO("GPU: glColor4ubv(color)")
        }

        override fun close() {
            sOverrideFaceColor = prevOverride
        }
    }
}


// Stub forward-declaration types referenced by DrawPool.createPool and RenderPass methods.
// These are defined in their own files; the stubs here prevent unresolved-reference compile errors
// when DrawPool.kt is compiled in isolation.

class DrawInfo {
    var count: Int = 0
    var start: Int = 0
    var end: Int = 0
    var offset: Int = 0
    var alphaMaskCutoff: Float = 0f
    var avatar: VOAvatar? = null
    var skinInfo: MeshSkinInfo? = null
    var modelMatrix: FloatArray? = null
    var textureMatrix: FloatArray? = null
    var texture: ViewerTexture? = null
    val textureList: MutableList<ViewerTexture?> = mutableListOf()
    var gltfMaterial: GLTFMaterial? = null
    var vertexBuffer: VertexBuffer? = null
}

open class ViewerTexture
open class ViewerFetchedTexture : ViewerTexture()
open class Face {
    var pool: FacePool? = null
    var referenceIndex: Int = -1
    fun renderIndexed() { TODO("GPU: render face indexed geometry") }
    fun verify(): Boolean = true
    fun getPool(): FacePool? = pool
}
open class SpatialGroup
open class VOAvatar
open class MeshSkinInfo { var hash: ULong = 0u }
open class GLSLShader
open class GLTFMaterial { var doubleSided: Boolean = false }
open class VertexBuffer {
    fun setBuffer() { TODO("GPU: bind vertex buffer") }
    fun drawRange(mode: Int, start: Int, end: Int, count: Int, offset: Int) { TODO("GPU: drawRange") }
}

// Stub pool types for classes NOT yet implemented in their own files.
class DrawPoolWLSky : RenderPass(DrawPool.PoolType.WL_SKY.value.toUInt()) { override fun isDead() = false }
class DrawPoolGLTFPBR(type: UInt = DrawPool.PoolType.GLTF_PBR.value.toUInt()) : RenderPass(type) { override fun isDead() = false }
class DrawPoolWaterExclusion : RenderPass(DrawPool.PoolType.WATEREXCLUSION.value.toUInt()) { override fun isDead() = false }
