package com.firestorm.llappearance

import com.firestorm.llcommon.LLUUID
import com.firestorm.llinventory.WearableType
import java.util.UUID

// AvatarBakedTextureIndex is already defined in TexLayerSet.kt
typealias BakedTextureIndex = AvatarBakedTextureIndex

enum class AvatarTextureIndex(val index: Int) {
    TEX_INVALID(-1),
    TEX_HEAD_BODYPAINT(0),
    TEX_UPPER_SHIRT(1),
    TEX_LOWER_PANTS(2),
    TEX_EYES_IRIS(3),
    TEX_HAIR(4),
    TEX_UPPER_BODYPAINT(5),
    TEX_LOWER_BODYPAINT(6),
    TEX_LOWER_SHOES(7),
    TEX_HEAD_BAKED(8),
    TEX_UPPER_BAKED(9),
    TEX_LOWER_BAKED(10),
    TEX_EYES_BAKED(11),
    TEX_LOWER_SOCKS(12),
    TEX_UPPER_JACKET(13),
    TEX_LOWER_JACKET(14),
    TEX_UPPER_GLOVES(15),
    TEX_UPPER_UNDERSHIRT(16),
    TEX_LOWER_UNDERPANTS(17),
    TEX_SKIRT(18),
    TEX_SKIRT_BAKED(19),
    TEX_HAIR_BAKED(20),
    TEX_LOWER_ALPHA(21),
    TEX_UPPER_ALPHA(22),
    TEX_HEAD_ALPHA(23),
    TEX_EYES_ALPHA(24),
    TEX_HAIR_ALPHA(25),
    TEX_HEAD_TATTOO(26),
    TEX_UPPER_TATTOO(27),
    TEX_LOWER_TATTOO(28),
    TEX_HEAD_UNIVERSAL_TATTOO(29),
    TEX_UPPER_UNIVERSAL_TATTOO(30),
    TEX_LOWER_UNIVERSAL_TATTOO(31),
    TEX_SKIRT_TATTOO(32),
    TEX_HAIR_TATTOO(33),
    TEX_EYES_TATTOO(34),
    TEX_LEFT_ARM_TATTOO(35),
    TEX_LEFT_LEG_TATTOO(36),
    TEX_AUX1_TATTOO(37),
    TEX_AUX2_TATTOO(38),
    TEX_AUX3_TATTOO(39),
    TEX_LEFT_ARM_BAKED(40),
    TEX_LEFT_LEG_BAKED(41),
    TEX_AUX1_BAKED(42),
    TEX_AUX2_BAKED(43),
    TEX_AUX3_BAKED(44),
    TEX_NUM_INDICES(45);

    companion object {
        private val byIndex: Map<Int, AvatarTextureIndex> = entries.associateBy { it.index }
        fun fromIndex(i: Int): AvatarTextureIndex = byIndex[i] ?: TEX_INVALID
    }
}

data class BakedTextureInfo(
    val textures: List<AvatarTextureIndex>,
    val wearables: List<WearableType>,
    // UUID used to identify this baked layer set in the wearables hash
    val defaultTexture: LLUUID,
    val superbaked: Boolean = false,
)

object AppearanceDefines {
    const val AVATAR_JOINT_COUNT: Int = 134
    const val BAKED_NUM_ENTRIES: Int = 11  // matches BAKED_NUM_INDICES ordinal count
    const val TEX_NUM_INDICES: Int = 45    // matches AvatarTextureIndex.TEX_NUM_INDICES.index

    private fun uuid(s: String): LLUUID = LLUUID(UUID.fromString(s))

    val BAKED_TEXTURE_DATA: Map<AvatarBakedTextureIndex, BakedTextureInfo> = mapOf(
        AvatarBakedTextureIndex.BAKED_HEAD to BakedTextureInfo(
            textures = listOf(
                AvatarTextureIndex.TEX_HEAD_BODYPAINT,
                AvatarTextureIndex.TEX_HEAD_TATTOO,
                AvatarTextureIndex.TEX_HEAD_ALPHA,
                AvatarTextureIndex.TEX_HEAD_UNIVERSAL_TATTOO,
            ),
            wearables = listOf(
                WearableType.SHAPE,
                WearableType.SKIN,
                WearableType.HAIR,
                WearableType.TATTOO,
                WearableType.ALPHA,
                WearableType.UNIVERSAL,
            ),
            defaultTexture = uuid("a4b9dc38-e13b-4df9-b284-751efb0566ff"),
        ),
        AvatarBakedTextureIndex.BAKED_UPPER to BakedTextureInfo(
            textures = listOf(
                AvatarTextureIndex.TEX_UPPER_SHIRT,
                AvatarTextureIndex.TEX_UPPER_BODYPAINT,
                AvatarTextureIndex.TEX_UPPER_JACKET,
                AvatarTextureIndex.TEX_UPPER_GLOVES,
                AvatarTextureIndex.TEX_UPPER_UNDERSHIRT,
                AvatarTextureIndex.TEX_UPPER_TATTOO,
                AvatarTextureIndex.TEX_UPPER_ALPHA,
                AvatarTextureIndex.TEX_UPPER_UNIVERSAL_TATTOO,
            ),
            wearables = listOf(
                WearableType.SHAPE,
                WearableType.SKIN,
                WearableType.SHIRT,
                WearableType.JACKET,
                WearableType.GLOVES,
                WearableType.UNDERSHIRT,
                WearableType.TATTOO,
                WearableType.ALPHA,
                WearableType.UNIVERSAL,
            ),
            defaultTexture = uuid("5943ff64-d26c-4a90-a8c0-d61f56bd98d4"),
        ),
        AvatarBakedTextureIndex.BAKED_LOWER to BakedTextureInfo(
            textures = listOf(
                AvatarTextureIndex.TEX_LOWER_PANTS,
                AvatarTextureIndex.TEX_LOWER_BODYPAINT,
                AvatarTextureIndex.TEX_LOWER_SHOES,
                AvatarTextureIndex.TEX_LOWER_SOCKS,
                AvatarTextureIndex.TEX_LOWER_JACKET,
                AvatarTextureIndex.TEX_LOWER_UNDERPANTS,
                AvatarTextureIndex.TEX_LOWER_TATTOO,
                AvatarTextureIndex.TEX_LOWER_ALPHA,
                AvatarTextureIndex.TEX_LOWER_UNIVERSAL_TATTOO,
            ),
            wearables = listOf(
                WearableType.SHAPE,
                WearableType.SKIN,
                WearableType.PANTS,
                WearableType.SHOES,
                WearableType.SOCKS,
                WearableType.JACKET,
                WearableType.UNDERPANTS,
                WearableType.TATTOO,
                WearableType.ALPHA,
                WearableType.UNIVERSAL,
            ),
            defaultTexture = uuid("2944ee70-90a7-425d-a5fb-d749c782ed7d"),
        ),
        AvatarBakedTextureIndex.BAKED_EYES to BakedTextureInfo(
            textures = listOf(
                AvatarTextureIndex.TEX_EYES_IRIS,
                AvatarTextureIndex.TEX_EYES_TATTOO,
                AvatarTextureIndex.TEX_EYES_ALPHA,
            ),
            wearables = listOf(
                WearableType.EYES,
                WearableType.UNIVERSAL,
                WearableType.ALPHA,
            ),
            defaultTexture = uuid("27b1bc0f-979f-4b13-95fe-b981c2ba9788"),
        ),
        AvatarBakedTextureIndex.BAKED_SKIRT to BakedTextureInfo(
            textures = listOf(
                AvatarTextureIndex.TEX_SKIRT,
                AvatarTextureIndex.TEX_SKIRT_TATTOO,
            ),
            wearables = listOf(
                WearableType.SKIRT,
                WearableType.UNIVERSAL,
            ),
            defaultTexture = uuid("03e7e8cb-1368-483b-b6f3-74850838ba63"),
        ),
        AvatarBakedTextureIndex.BAKED_HAIR to BakedTextureInfo(
            textures = listOf(
                AvatarTextureIndex.TEX_HAIR,
                AvatarTextureIndex.TEX_HAIR_TATTOO,
                AvatarTextureIndex.TEX_HAIR_ALPHA,
            ),
            wearables = listOf(
                WearableType.HAIR,
                WearableType.UNIVERSAL,
                WearableType.ALPHA,
            ),
            defaultTexture = uuid("a60e85a9-74e8-48d8-8a2d-8129f28d9b61"),
        ),
        AvatarBakedTextureIndex.BAKED_LEFT_ARM to BakedTextureInfo(
            textures = listOf(AvatarTextureIndex.TEX_LEFT_ARM_TATTOO),
            wearables = listOf(WearableType.UNIVERSAL),
            defaultTexture = uuid("9f39febf-22d7-0087-79d1-e9e8c6c9ed19"),
            superbaked = true,
        ),
        AvatarBakedTextureIndex.BAKED_LEFT_LEG to BakedTextureInfo(
            textures = listOf(AvatarTextureIndex.TEX_LEFT_LEG_TATTOO),
            wearables = listOf(WearableType.UNIVERSAL),
            defaultTexture = uuid("054a7a58-8ed5-6386-0add-3b636fb28b78"),
            superbaked = true,
        ),
        AvatarBakedTextureIndex.BAKED_AUX1 to BakedTextureInfo(
            textures = listOf(AvatarTextureIndex.TEX_AUX1_TATTOO),
            wearables = listOf(WearableType.UNIVERSAL),
            defaultTexture = uuid("790c11be-b25c-c17e-b4d2-6a4ad786b752"),
            superbaked = true,
        ),
        AvatarBakedTextureIndex.BAKED_AUX2 to BakedTextureInfo(
            textures = listOf(AvatarTextureIndex.TEX_AUX2_TATTOO),
            wearables = listOf(WearableType.UNIVERSAL),
            defaultTexture = uuid("d78c478f-48c7-5928-5864-8d99fb1f521e"),
            superbaked = true,
        ),
        AvatarBakedTextureIndex.BAKED_AUX3 to BakedTextureInfo(
            textures = listOf(AvatarTextureIndex.TEX_AUX3_TATTOO),
            wearables = listOf(WearableType.UNIVERSAL),
            defaultTexture = uuid("6a95dd53-edd9-aac8-f6d3-27ed99f3c3eb"),
            superbaked = true,
        ),
    )
}
