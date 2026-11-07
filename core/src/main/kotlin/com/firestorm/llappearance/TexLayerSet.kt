package com.firestorm.llappearance

enum class AvatarBakedTextureIndex {
    BAKED_HEAD,
    BAKED_UPPER,
    BAKED_LOWER,
    BAKED_EYES,
    BAKED_SKIRT,
    BAKED_HAIR,
    BAKED_LEFT_ARM,
    BAKED_LEFT_LEG,
    BAKED_AUX1,
    BAKED_AUX2,
    BAKED_AUX3,
}

class TexLayer(
    var name: String = "",
    var renderPass: TexLayerPass = TexLayerPass.PASS_COLOR,
    var textureIndex: Int = -1,
) {
    enum class TexLayerPass {
        PASS_COLOR,
        PASS_MORPH,
        PASS_HAIR_MORPH,
        PASS_MARK_ALPHA,
        PASS_ALPHA,
        PASS_ALPHA_MASK,
        PASS_MASK,
        PASS_BUMP_MORPH,
    }
}

class TexLayerSet(
    var bodyRegion: AvatarBakedTextureIndex = AvatarBakedTextureIndex.BAKED_HEAD,
) {
    private val layers = mutableListOf<TexLayer>()

    fun addLayer(layer: TexLayer) {
        layers += layer
    }

    fun getLayers(): List<TexLayer> = layers

    fun findLayerByName(name: String): TexLayer? = layers.find { it.name == name }
}
