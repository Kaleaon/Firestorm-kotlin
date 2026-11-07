package com.firestorm.newview

import java.util.UUID

// GLTF texture type count matches C++ TEXTURE_TYPE_COUNT = 5
private const val TEXTURE_TYPE_COUNT = 5
private const val INVALID_INDEX = -1

data class Vector4a(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 0f)
data class Vector2(val x: Float = 0f, val y: Float = 0f)

// Stub types representing C++ GLTF domain objects
class GltfAsset {
    var pendingBuffers: Int = 0
    val images: MutableList<GltfImage> = mutableListOf()
    val buffers: MutableList<GltfBuffer> = mutableListOf()
    val materials: MutableList<GltfMaterial> = mutableListOf()
    val textures: MutableList<GltfTexture> = mutableListOf()
    val nodes: MutableList<GltfNode> = mutableListOf()
    val meshes: MutableList<GltfMesh> = mutableListOf()
    val skins: MutableList<GltfSkin> = mutableListOf()
    val samplers: MutableList<GltfSampler> = mutableListOf()
    val renderData: Array<GltfRenderData> = Array(2) { GltfRenderData() }
    var nodesUbo: Int = 0
    var materialsUbo: Int = 0

    fun load(filename: String, validate: Boolean): Boolean = false
    fun save(filename: String): Boolean = false
    fun updateTransforms() { // no-op }
    fun update() { // no-op }
    fun prep(): Boolean = false
    fun serialize(out: MutableMap<String, Any>) { // no-op }
    fun lineSegmentIntersect(
        start: Vector4a, end: Vector4a,
        intersection: Vector4a?, texCoord: Vector2?, normal: Vector4a?, tangent: Vector4a?,
        primitiveHit: IntArray?
    ): Int = 0
}

class GltfImage {
    var texture: Any? = null // ViewerTexture stub
    var bufferView: Int = INVALID_INDEX
    var mimeType: String = ""
    var name: String = ""
    var uri: String = ""
    fun clearData(asset: GltfAsset) { // no-op }
}

class GltfBuffer {
    var data: ByteArray = ByteArray(0)
    var uri: String = ""
}

class GltfMaterial {
    val pbrMetallicRoughness: PbrMetallicRoughness = PbrMetallicRoughness()
    val normalTexture: GltfTextureInfo = GltfTextureInfo()
    val occlusionTexture: GltfTextureInfo = GltfTextureInfo()
    val emissiveTexture: GltfTextureInfo = GltfTextureInfo()
}

class PbrMetallicRoughness {
    val baseColorTexture: GltfTextureInfo = GltfTextureInfo()
    val metallicRoughnessTexture: GltfTextureInfo = GltfTextureInfo()
}

class GltfTextureInfo {
    var index: Int = INVALID_INDEX
}

class GltfTexture {
    var source: Int = INVALID_INDEX
    var sampler: Int = INVALID_INDEX
}

class GltfSampler {
    var wrapS: Int = 0
    var wrapT: Int = 0
    var magFilter: Int = 0
}

class GltfNode {
    var mesh: Int = INVALID_INDEX
    var skin: Int = INVALID_INDEX
    val children: MutableList<Int> = mutableListOf()
    // assetMatrix: represented as a flat FloatArray(16) column-major
    val assetMatrix: FloatArray = FloatArray(16)
    val assetMatrixInv: FloatArray = FloatArray(16)
}

class GltfMesh {
    val primitives: MutableList<GltfPrimitive> = mutableListOf()
}

class GltfPrimitive {
    var vertexBuffer: Any? = null
    var glMode: Int = 0
    var vertexOffset: Int = 0
    var indexOffset: Int = 0
    var indexCount: Int = 0
    var vertexCount: Int = 0
    var octree: Any? = null
    fun getIndexCount(): Int = indexCount
    fun getVertexCount(): Int = vertexCount
}

class GltfSkin {
    var ubo: Int = 0
}

class GltfRenderData {
    // batches indexed by shader variant bitmask
    val batches: MutableMap<Int, MutableList<GltfBatch>> = mutableMapOf()
}

class GltfBatch {
    var vertexBuffer: Any? = null
    val primitives: MutableList<GltfPrimitiveDraw> = mutableListOf()
}

class GltfPrimitiveDraw {
    var nodeIndex: Int = 0
    var primitiveIndex: Int = 0
}

// Stub for ViewerObject that carries a GLTF asset
class ViewerObject(val id: UUID = UUID.randomUUID()) {
    var gltfAsset: GltfAsset? = null
    var isGltfAssetMissing: Boolean = false
    var drawable: Any? = null

    fun isDead(): Boolean = false
    fun getVolume(): Any? = null
    fun markForUpdate() = Unit
    fun setGltfAsset(assetId: UUID) = Unit
    fun getGltfAssetToAgentTransform(): FloatArray = FloatArray(16)
    fun getAgentToGltfAssetTransform(): FloatArray = FloatArray(16)
    fun volumeDirectionToAgent(v: FloatArray): FloatArray = v
    fun ref() = Unit
    fun unref() = Unit
}

object GLTFSceneManager {

    val objects: MutableList<ViewerObject> = mutableListOf()

    var uploadingAsset: GltfAsset? = null
    var gltfUploadPending: Boolean = false
    var uploadingObject: ViewerObject? = null
    var pendingImageUploads: UInt = 0u
    var pendingBinaryUploads: UInt = 0u
    var pendingGltfUploads: UInt = 0u

    var jointUbo: UInt = 0u

    val lastTexture: IntArray = IntArray(TEXTURE_TYPE_COUNT) { -2 }

    fun load() {
        System.err.println("GLTFSceneManager: load not yet implemented")
    }

    fun load(filename: String) {
        System.err.println("GLTFSceneManager: load(filename) not yet implemented")
    }

    fun saveAs() {
        System.err.println("GLTFSceneManager: saveAs not yet implemented")
    }

    fun save(filename: String) {
        System.err.println("GLTFSceneManager: save(filename) not yet implemented")
    }

    fun uploadSelection() {
        System.err.println("GLTFSceneManager: uploadSelection not yet implemented")
    }

    fun update() {
        var i = 0
        while (i < objects.size) {
            val obj = objects[i]
            if (obj.isDead() || obj.gltfAsset == null) {
                objects.removeAt(i)
                continue
            }
            obj.gltfAsset!!.update()
            i++
        }

        val uploading = uploadingAsset ?: return
        if (!gltfUploadPending && pendingImageUploads == 0u && pendingBinaryUploads == 0u) {
            val jsonObj = mutableMapOf<String, Any>()
            uploading.serialize(jsonObj)
            System.err.println("GLTFSceneManager: update upload finalization not yet implemented")
        }
    }

    fun render(opaque: Boolean, rigged: Boolean = false, unlit: Boolean = false) {
        var variant: UByte = 0u
        if (rigged)   variant = (variant or GLTFVariant.RIGGED).toUByte()
        if (!opaque)  variant = (variant or GLTFVariant.ALPHA_BLEND).toUByte()
        if (unlit)    variant = (variant or GLTFVariant.UNLIT).toUByte()
        render(variant)
    }

    fun render(variant: UByte) {
        // no-op
    }

    fun render(asset: GltfAsset, variant: UByte) {
        // no-op
    }

    fun bind(asset: GltfAsset, material: GltfMaterial) {
        // no-op
    }

    fun bindTexture(
        asset: GltfAsset,
        textureType: Int,
        info: GltfTextureInfo,
        fallback: Any?
    ) {
        if (info.index == lastTexture[textureType]) return
        // no-op
    }

    fun renderOpaque() = render(true)

    fun renderAlpha() = render(false)

    fun renderDebug() {
        // no-op
    }

    fun addGltfObject(obj: ViewerObject, gltfId: UUID) {
        if (obj.gltfAsset != null || obj.isGltfAssetMissing) return
        obj.ref()
        System.err.println("GLTFSceneManager: addGltfObject not yet implemented")
    }

    fun onGltfLoadComplete(id: UUID, assetType: Int, obj: ViewerObject?, status: Int) {
        if (status == 0 /* LL_ERR_NOERR */ && obj != null) {
            System.err.println("GLTFSceneManager: onGltfLoadComplete not yet implemented")
        } else {
            obj?.isGltfAssetMissing = true
            obj?.unref()
        }
    }

    fun onGltfBinLoadComplete(id: UUID, assetType: Int, obj: ViewerObject?, status: Int) {
        // Must be posted to main coroutine equivalent.
        if (status == 0 && obj?.gltfAsset != null) {
            val asset = obj.gltfAsset!!
            asset.pendingBuffers--
            if (asset.pendingBuffers == 0) {
                if (asset.prep()) {
                    if (!objects.contains(obj)) objects.add(obj)
                } else {
                    obj.isGltfAssetMissing = true
                    obj.gltfAsset = null
                }
            }
            obj.unref()
        } else {
            obj?.isGltfAssetMissing = true
            obj?.unref()
        }
    }

    fun lineSegmentIntersect(
        start: Vector4a, end: Vector4a,
        pickTransparent: Boolean, pickRigged: Boolean, pickUnselectable: Boolean, pickReflectionProbe: Boolean,
        nodeHit: IntArray?, primitiveHit: IntArray?,
        intersection: Vector4a?, texCoord: Vector2?, normal: Vector4a?, tangent: Vector4a?
    ): Any? /* Drawable? */ {
        return null
    }

    fun lineSegmentIntersect(
        obj: ViewerObject, asset: GltfAsset,
        start: Vector4a, end: Vector4a, face: Int,
        pickTransparent: Boolean, pickRigged: Boolean, pickUnselectable: Boolean,
        nodeHit: IntArray?, primitiveHit: IntArray?,
        intersection: Vector4a?, texCoord: Vector2?, normal: Vector4a?, tangent: Vector4a?
    ): Boolean {
        return false
    }

    object GLTFVariant {
        const val RIGGED: Int = 0x01
        const val ALPHA_BLEND: Int = 0x02
        const val UNLIT: Int = 0x04
        const val MULTI_UV: Int = 0x08
    }
}
