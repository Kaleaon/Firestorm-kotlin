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

        // Backwards-compatible constant aliases for code that predates the PoolType enum.
        val POOL_SKY: UInt get() = PoolType.SKY.value.toUInt()
        val POOL_WATEREXCLUSION: UInt get() = PoolType.WATEREXCLUSION.value.toUInt()
        val POOL_WL_SKY: UInt get() = PoolType.WL_SKY.value.toUInt()
        val POOL_SIMPLE: UInt get() = PoolType.SIMPLE.value.toUInt()
        val POOL_FULLBRIGHT: UInt get() = PoolType.FULLBRIGHT.value.toUInt()
        val POOL_BUMP: UInt get() = PoolType.BUMP.value.toUInt()
        val POOL_MATERIALS: UInt get() = PoolType.MATERIALS.value.toUInt()
        val POOL_GLTF_PBR: UInt get() = PoolType.GLTF_PBR.value.toUInt()
        val POOL_TERRAIN: UInt get() = PoolType.TERRAIN.value.toUInt()
        val POOL_GRASS: UInt get() = PoolType.GRASS.value.toUInt()
        val POOL_GLTF_PBR_ALPHA_MASK: UInt get() = PoolType.GLTF_PBR_ALPHA_MASK.value.toUInt()
        val POOL_TREE: UInt get() = PoolType.TREE.value.toUInt()
        val POOL_ALPHA_MASK: UInt get() = PoolType.ALPHA_MASK.value.toUInt()
        val POOL_FULLBRIGHT_ALPHA_MASK: UInt get() = PoolType.FULLBRIGHT_ALPHA_MASK.value.toUInt()
        val POOL_AVATAR: UInt get() = PoolType.AVATAR.value.toUInt()
        val POOL_CONTROL_AV: UInt get() = PoolType.CONTROL_AV.value.toUInt()
        val POOL_GLOW: UInt get() = PoolType.GLOW.value.toUInt()
        val POOL_ALPHA_PRE_WATER: UInt get() = PoolType.ALPHA_PRE_WATER.value.toUInt()
        val POOL_VOIDWATER: UInt get() = PoolType.VOIDWATER.value.toUInt()
        val POOL_WATER: UInt get() = PoolType.WATER.value.toUInt()
        val POOL_ALPHA_POST_WATER: UInt get() = PoolType.ALPHA_POST_WATER.value.toUInt()
        val POOL_ALPHA: UInt get() = PoolType.ALPHA.value.toUInt()

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
                PoolType.TREE.value -> DrawPoolTree(tex0 ?: error("tex0 required for POOL_TREE"))
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
        // GPU: activate texture unit 0
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
            // GPU: apply model matrix from DrawInfo
        }

        fun applyModelMatrix(modelMatrix: FloatArray?) {
            // GPU: load modelview matrix and multiply by modelMatrix
        }

        fun uploadMatrixPalette(params: DrawInfo): Boolean {
            return false // GPU: upload matrix palette from DrawInfo avatar/skinInfo
        }

        fun uploadMatrixPalette(avatar: VOAvatar?, skinInfo: MeshSkinInfo?): Boolean {
            return false // GPU: upload matrix palette to shader
        }

        fun uploadMatrixPalette(
            avatar: VOAvatar?,
            skinInfo: MeshSkinInfo?,
            lastAvatar: Array<VOAvatar?>,
            lastMeshId: LongArray,
            skipLastSkin: BooleanArray
        ): Boolean {
            return false // GPU: upload matrix palette with caching
        }

        fun uploadMatrixPalette(
            avatar: VOAvatar?,
            skinInfo: MeshSkinInfo?,
            lastAvatar: Array<VOAvatar?>,
            lastMeshId: LongArray,
            lastAvatarShader: Array<GLSLShader?>,
            skipLastSkin: BooleanArray
        ): Boolean {
            return false // GPU: upload matrix palette with shader caching
        }

        fun pushGLTFBatch(params: DrawInfo) {
            // GPU: bind GLTF material, set buffer, draw range triangles
        }

        fun pushRiggedGLTFBatch(
            params: DrawInfo,
            lastAvatar: Array<VOAvatar?>,
            lastMeshId: LongArray,
            skipLastSkin: BooleanArray
        ) {
            // GPU: upload matrix palette then pushGLTFBatch
        }

        fun pushUntexturedGLTFBatch(params: DrawInfo) {
            // GPU: set cull face, apply model matrix, set buffer, draw range triangles
        }

        fun pushUntexturedRiggedGLTFBatch(
            params: DrawInfo,
            lastAvatar: Array<VOAvatar?>,
            lastMeshId: LongArray,
            skipLastSkin: BooleanArray
        ) {
            // GPU: upload matrix palette then pushUntexturedGLTFBatch
        }
    }

    override fun getDebugTexture(): ViewerTexture? = null
    override fun getTexture(): ViewerTexture? = null
    override fun isDead(): Boolean = false
    override fun resetDrawOrders() {}

    fun pushBatches(type: UInt, texture: Boolean = true, batchTextures: Boolean = false) {
        if (texture) {
            // GPU: iterate pipeline render map for type, call pushBatch for each DrawInfo
        } else {
            pushUntexturedBatches(type)
        }
    }

    fun pushUntexturedBatches(type: UInt) {
        // GPU: iterate pipeline render map for type, call pushUntexturedBatch for each DrawInfo
    }

    fun pushRiggedBatches(type: UInt, texture: Boolean = true, batchTextures: Boolean = false) {
        if (texture) {
            // GPU: iterate pipeline render map with matrix palette upload, call pushBatch
        } else {
            pushUntexturedRiggedBatches(type)
        }
    }

    fun pushUntexturedRiggedBatches(type: UInt) {
        // GPU: iterate pipeline render map with matrix palette upload, call pushUntexturedBatch
    }

    fun pushGLTFBatches(type: UInt) {
        // GPU: iterate pipeline render map, call pushGLTFBatch for each DrawInfo
    }

    fun pushUntexturedGLTFBatches(type: UInt) {
        // GPU: iterate pipeline render map, call pushUntexturedGLTFBatch for each DrawInfo
    }

    fun pushGLTFBatches(type: UInt, textured: Boolean) {
        if (textured) pushGLTFBatches(type) else pushUntexturedGLTFBatches(type)
    }

    fun pushRiggedGLTFBatches(type: UInt) {
        // GPU: iterate pipeline render map with rigged GLTF batch dispatch
    }

    fun pushRiggedGLTFBatches(type: UInt, textured: Boolean) {
        if (textured) pushRiggedGLTFBatches(type) else pushUntexturedRiggedGLTFBatches(type)
    }

    fun pushUntexturedRiggedGLTFBatches(type: UInt) {
        // GPU: iterate pipeline render map with untextured rigged GLTF batch dispatch
    }

    fun pushMaskBatches(type: UInt, texture: Boolean = true, batchTextures: Boolean = false) {
        // GPU: iterate pipeline render map, set minimum alpha, call pushBatch
    }

    fun pushRiggedMaskBatches(type: UInt, texture: Boolean = true, batchTextures: Boolean = false) {
        // GPU: iterate pipeline render map, set minimum alpha, upload matrix palette, call pushBatch
    }

    fun pushBatch(params: DrawInfo, texture: Boolean, batchTextures: Boolean = false) {
        // GPU: apply model matrix, bind textures, set vertex buffer, drawRange TRIANGLES
    }

    fun pushUntexturedBatch(params: DrawInfo) {
        // GPU: apply model matrix, set vertex buffer, drawRange TRIANGLES
    }

    fun pushBumpBatch(params: DrawInfo, texture: Boolean, batchTextures: Boolean = false) {
        // GPU: push bump-mapped batch
    }

    open fun renderGroup(group: SpatialGroup, type: UInt, texture: Boolean = true) {
        // GPU: iterate group draw map for type, call pushBatch
    }

    open fun renderRiggedGroup(group: SpatialGroup, type: UInt, texture: Boolean = true) {
        // GPU: iterate group draw map with matrix palette upload, call pushBatch
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
        r: Float? = null,
        g: Float? = null,
        b: Float? = null,
        a: Float? = null
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
            // GPU: diffuseColor4f($r, $g, $b, $a)
        }

        fun setColor(color: FloatArray) {
            // GPU: diffuseColor4fv(color)
        }

        fun setColorU(color: ByteArray) {
            // GPU: glColor4ubv(color)
        }

        override fun close() {
            sOverrideFaceColor = prevOverride
        }
    }
}


// Types not yet defined elsewhere in the codebase — keep here to avoid unresolved-reference errors.

open class MeshSkinInfo { var hash: ULong = 0u }

open class GLSLShader {
    var mRiggedVariant: GLSLShader? = null

    fun bind(rigged: Boolean = false) { /* GPU: GLSLShader.bind */ }
    fun unbind() { /* GPU: GLSLShader.unbind */ }
    fun uniform1f(name: Any, value: Float) { /* GPU: uniform1f */ }
    fun uniform1i(name: Any, value: Int) { /* GPU: uniform1i */ }
    fun uniform4f(name: Any, x: Float, y: Float, z: Float, w: Float) { /* GPU: uniform4f */ }
    fun uniform4fv(name: Any, count: Int, values: FloatArray) { /* GPU: uniform4fv */ }
    fun setMinimumAlpha(alpha: Float) { /* GPU: setMinimumAlpha */ }
    fun enableTexture(channel: Int): Int { return 0 /* GPU: enableTexture */ }
    fun disableTexture(channel: Int) { /* GPU: disableTexture */ }
    fun bindTexture(channel: Int, texture: Any?) { /* GPU: bindTexture */ }

    companion object {
        var sCurBoundShaderPtr: GLSLShader? = null
    }
}

open class GLTFMaterial {
    var doubleSided: Boolean = false
    var alphaMode: Int = ALPHA_MODE_BLEND
    fun bind(texture: ViewerTexture?) { /* GPU: GLTFMaterial.bind */ }

    companion object {
        const val ALPHA_MODE_BLEND: Int = 0
        const val ALPHA_MODE_MASK: Int = 1
        const val ALPHA_MODE_OPAQUE: Int = 2
    }
}

// Stub pool types that have no full-implementation file yet.
class DrawPoolWLSky : RenderPass(DrawPool.PoolType.WL_SKY.value.toUInt()) { override fun isDead() = false }
class DrawPoolGLTFPBR(type: UInt = DrawPool.PoolType.GLTF_PBR.value.toUInt()) : RenderPass(type) { override fun isDead() = false }
class DrawPoolWaterExclusion : RenderPass(DrawPool.PoolType.WATEREXCLUSION.value.toUInt()) { override fun isDead() = false }
