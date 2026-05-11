package com.firestorm.llrender

import com.firestorm.llcommon.LLUUID

class Texture(
    var textureId: LLUUID = LLUUID.NULL,
    var width: Int = 0,
    var height: Int = 0,
    var components: Int = 0
) {
    enum class BoostLevel {
        NONE,
        AVATAR,
        AVATAR_BAKED,
        TERRAIN,
        HIGH,
        SCULPTED,
        BUMP,
        SELECTED,
        AVATAR_BAKED_SELF,
        AVATAR_SELF,
        SUPER_HIGH,
        HUD,
        ICON,
        THUMBNAIL,
        UI,
        PREVIEW,
        MAP,
        MAP_VISIBLE,
        LOCAL,
        AVATAR_SCRATCH_TEX,
        DYNAMIC_TEX,
        MEDIA,
        OTHER
    }

    enum class BindTarget {
        TEXTURE_2D,
        TEXTURE_RECT,
        TEXTURE_CUBE_MAP,
        TEXTURE_CUBE_MAP_ARRAY,
        TEXTURE_MULTISAMPLE,
        TEXTURE_3D
    }

    enum class TextureState {
        DELETED,
        ACTIVE,
        NO_DELETE
    }

    var boostLevel: BoostLevel = BoostLevel.NONE
    var textureState: TextureState = TextureState.ACTIVE
    var missingAsset: Boolean = false

    private val discardWidths: MutableMap<Int, Int> = mutableMapOf()
    private val discardHeights: MutableMap<Int, Int> = mutableMapOf()

    fun isValid(): Boolean = textureId != LLUUID.NULL && width > 0 && height > 0
    fun isMissingAsset(): Boolean = missingAsset

    fun bind(stage: Int) { TODO("Bind GL texture to texture unit $stage") }
    fun unbind(stage: Int) { TODO("Unbind GL texture from texture unit $stage") }

    fun getWidth(discard: Int = 0): Int =
        if (discard <= 0) width else discardWidths.getOrDefault(discard, width shr discard)

    fun getHeight(discard: Int = 0): Int =
        if (discard <= 0) height else discardHeights.getOrDefault(discard, height shr discard)

    fun setBoostLevel(level: BoostLevel) {
        boostLevel = level
    }

    fun setDiscardSize(discard: Int, w: Int, h: Int) {
        discardWidths[discard] = w
        discardHeights[discard] = h
    }
}
