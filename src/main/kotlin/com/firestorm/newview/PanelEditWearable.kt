package com.firestorm.newview

import java.util.UUID
import kotlin.math.roundToInt

enum class Subpart {
    SHAPE_HEAD, SHAPE_EYES, SHAPE_EARS, SHAPE_NOSE, SHAPE_MOUTH, SHAPE_CHIN,
    SHAPE_TORSO, SHAPE_LEGS, SHAPE_WHOLE, SHAPE_DETAIL,
    SKIN_COLOR, SKIN_FACEDETAIL, SKIN_MAKEUP, SKIN_BODYDETAIL,
    HAIR_COLOR, HAIR_STYLE, HAIR_EYEBROWS, HAIR_FACIAL,
    EYES,
    SHIRT, PANTS, SHOES, SOCKS, JACKET, GLOVES, UNDERSHIRT, UNDERPANTS, SKIRT,
    ALPHA, TATTOO, UNIVERSAL,
    PHYSICS_BREASTS_UPDOWN, PHYSICS_BREASTS_INOUT, PHYSICS_BREASTS_LEFTRIGHT,
    PHYSICS_BELLY_UPDOWN, PHYSICS_BUTT_UPDOWN, PHYSICS_BUTT_LEFTRIGHT, PHYSICS_ADVANCED
}

data class WearableEntry(
    val wearableType: WearableType,
    val title: String,
    val descTitle: String,
    val colorSwatchCtrls: List<TextureIndex>,
    val textureCtrls: List<TextureIndex>,
    val subparts: List<Subpart>
)

data class SubpartEntry(
    val subpart: Subpart,
    val targetJoint: String,
    val editGroup: String,
    val paramList: String,
    val accordionTab: String,
    val targetOffset: Vector3d,
    val cameraOffset: Vector3d,
    val sex: Sex
)

data class PickerControlEntry(
    val textureIndex: TextureIndex,
    val controlName: String,
    val defaultImageId: UUID?,
    val allowNoTexture: Boolean
)

object EditWearableDictionary {
    val wearables: Map<WearableType, WearableEntry> = buildWearables()
    val subparts: Map<Subpart, SubpartEntry> = buildSubparts()
    val colorSwatchCtrls: Map<TextureIndex, PickerControlEntry> = buildColorSwatchCtrls()
    val textureCtrls: Map<TextureIndex, PickerControlEntry> = buildTextureCtrls()

    fun getWearable(type: WearableType): WearableEntry? = wearables[type]
    fun getSubpart(subpart: Subpart): SubpartEntry? = subparts[subpart]
    fun getColorSwatch(index: TextureIndex): PickerControlEntry? = colorSwatchCtrls[index]
    fun getTexturePicker(index: TextureIndex): PickerControlEntry? = textureCtrls[index]

    private fun buildWearables(): Map<WearableType, WearableEntry> = mapOf(
        WearableType.SHAPE to WearableEntry(WearableType.SHAPE, "edit_shape_title", "shape_desc_text", emptyList(), emptyList(),
            listOf(Subpart.SHAPE_WHOLE, Subpart.SHAPE_HEAD, Subpart.SHAPE_EYES, Subpart.SHAPE_EARS, Subpart.SHAPE_NOSE, Subpart.SHAPE_MOUTH, Subpart.SHAPE_CHIN, Subpart.SHAPE_TORSO, Subpart.SHAPE_LEGS)),
        WearableType.SKIN to WearableEntry(WearableType.SKIN, "edit_skin_title", "skin_desc_text", emptyList(),
            listOf(TextureIndex.HEAD_BODYPAINT, TextureIndex.UPPER_BODYPAINT, TextureIndex.LOWER_BODYPAINT),
            listOf(Subpart.SKIN_COLOR, Subpart.SKIN_FACEDETAIL, Subpart.SKIN_MAKEUP, Subpart.SKIN_BODYDETAIL)),
        WearableType.HAIR to WearableEntry(WearableType.HAIR, "edit_hair_title", "hair_desc_text", emptyList(),
            listOf(TextureIndex.HAIR),
            listOf(Subpart.HAIR_COLOR, Subpart.HAIR_STYLE, Subpart.HAIR_EYEBROWS, Subpart.HAIR_FACIAL)),
        WearableType.EYES to WearableEntry(WearableType.EYES, "edit_eyes_title", "eyes_desc_text", emptyList(),
            listOf(TextureIndex.EYES_IRIS), listOf(Subpart.EYES)),
        WearableType.SHIRT to WearableEntry(WearableType.SHIRT, "edit_shirt_title", "shirt_desc_text",
            listOf(TextureIndex.UPPER_SHIRT), listOf(TextureIndex.UPPER_SHIRT), listOf(Subpart.SHIRT)),
        WearableType.PANTS to WearableEntry(WearableType.PANTS, "edit_pants_title", "pants_desc_text",
            listOf(TextureIndex.LOWER_PANTS), listOf(TextureIndex.LOWER_PANTS), listOf(Subpart.PANTS)),
        WearableType.SHOES to WearableEntry(WearableType.SHOES, "edit_shoes_title", "shoes_desc_text",
            listOf(TextureIndex.LOWER_SHOES), listOf(TextureIndex.LOWER_SHOES), listOf(Subpart.SHOES)),
        WearableType.SOCKS to WearableEntry(WearableType.SOCKS, "edit_socks_title", "socks_desc_text",
            listOf(TextureIndex.LOWER_SOCKS), listOf(TextureIndex.LOWER_SOCKS), listOf(Subpart.SOCKS)),
        WearableType.JACKET to WearableEntry(WearableType.JACKET, "edit_jacket_title", "jacket_desc_text",
            listOf(TextureIndex.UPPER_JACKET), listOf(TextureIndex.UPPER_JACKET, TextureIndex.LOWER_JACKET), listOf(Subpart.JACKET)),
        WearableType.GLOVES to WearableEntry(WearableType.GLOVES, "edit_gloves_title", "gloves_desc_text",
            listOf(TextureIndex.UPPER_GLOVES), listOf(TextureIndex.UPPER_GLOVES), listOf(Subpart.GLOVES)),
        WearableType.UNDERSHIRT to WearableEntry(WearableType.UNDERSHIRT, "edit_undershirt_title", "undershirt_desc_text",
            listOf(TextureIndex.UPPER_UNDERSHIRT), listOf(TextureIndex.UPPER_UNDERSHIRT), listOf(Subpart.UNDERSHIRT)),
        WearableType.UNDERPANTS to WearableEntry(WearableType.UNDERPANTS, "edit_underpants_title", "underpants_desc_text",
            listOf(TextureIndex.LOWER_UNDERPANTS), listOf(TextureIndex.LOWER_UNDERPANTS), listOf(Subpart.UNDERPANTS)),
        WearableType.SKIRT to WearableEntry(WearableType.SKIRT, "edit_skirt_title", "skirt_desc_text",
            listOf(TextureIndex.SKIRT), listOf(TextureIndex.SKIRT), listOf(Subpart.SKIRT)),
        WearableType.ALPHA to WearableEntry(WearableType.ALPHA, "edit_alpha_title", "alpha_desc_text", emptyList(),
            listOf(TextureIndex.LOWER_ALPHA, TextureIndex.UPPER_ALPHA, TextureIndex.HEAD_ALPHA, TextureIndex.EYES_ALPHA, TextureIndex.HAIR_ALPHA),
            listOf(Subpart.ALPHA)),
        WearableType.TATTOO to WearableEntry(WearableType.TATTOO, "edit_tattoo_title", "tattoo_desc_text",
            listOf(TextureIndex.HEAD_TATTOO),
            listOf(TextureIndex.LOWER_TATTOO, TextureIndex.UPPER_TATTOO, TextureIndex.HEAD_TATTOO),
            listOf(Subpart.TATTOO)),
        WearableType.UNIVERSAL to WearableEntry(WearableType.UNIVERSAL, "edit_universal_title", "universal_desc_text",
            listOf(TextureIndex.HEAD_UNIVERSAL_TATTOO),
            listOf(TextureIndex.HEAD_UNIVERSAL_TATTOO, TextureIndex.UPPER_UNIVERSAL_TATTOO, TextureIndex.LOWER_UNIVERSAL_TATTOO,
                TextureIndex.SKIRT_TATTOO, TextureIndex.HAIR_TATTOO, TextureIndex.EYES_TATTOO,
                TextureIndex.LEFT_ARM_TATTOO, TextureIndex.LEFT_LEG_TATTOO, TextureIndex.AUX1_TATTOO, TextureIndex.AUX2_TATTOO, TextureIndex.AUX3_TATTOO),
            listOf(Subpart.UNIVERSAL)),
        WearableType.PHYSICS to WearableEntry(WearableType.PHYSICS, "edit_physics_title", "physics_desc_text", emptyList(), emptyList(),
            listOf(Subpart.PHYSICS_BREASTS_UPDOWN, Subpart.PHYSICS_BREASTS_INOUT, Subpart.PHYSICS_BREASTS_LEFTRIGHT,
                Subpart.PHYSICS_BELLY_UPDOWN, Subpart.PHYSICS_BUTT_UPDOWN, Subpart.PHYSICS_BUTT_LEFTRIGHT, Subpart.PHYSICS_ADVANCED))
    )

    private fun buildSubparts(): Map<Subpart, SubpartEntry> = mapOf(
        Subpart.SHAPE_WHOLE to SubpartEntry(Subpart.SHAPE_WHOLE, "mPelvis", "shape_body", "shape_body_param_list", "shape_body_tab", Vector3d(0.0, 0.0, 0.1), Vector3d(-2.5, 0.5, 0.8), Sex.BOTH),
        Subpart.SHAPE_HEAD to SubpartEntry(Subpart.SHAPE_HEAD, "mHead", "shape_head", "shape_head_param_list", "shape_head_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.SHAPE_EYES to SubpartEntry(Subpart.SHAPE_EYES, "mHead", "shape_eyes", "shape_eyes_param_list", "shape_eyes_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.SHAPE_EARS to SubpartEntry(Subpart.SHAPE_EARS, "mHead", "shape_ears", "shape_ears_param_list", "shape_ears_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.SHAPE_NOSE to SubpartEntry(Subpart.SHAPE_NOSE, "mHead", "shape_nose", "shape_nose_param_list", "shape_nose_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.SHAPE_MOUTH to SubpartEntry(Subpart.SHAPE_MOUTH, "mHead", "shape_mouth", "shape_mouth_param_list", "shape_mouth_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.SHAPE_CHIN to SubpartEntry(Subpart.SHAPE_CHIN, "mHead", "shape_chin", "shape_chin_param_list", "shape_chin_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.SHAPE_TORSO to SubpartEntry(Subpart.SHAPE_TORSO, "mTorso", "shape_torso", "shape_torso_param_list", "shape_torso_tab", Vector3d(0.0, 0.0, 0.3), Vector3d(-1.0, 0.15, 0.3), Sex.BOTH),
        Subpart.SHAPE_LEGS to SubpartEntry(Subpart.SHAPE_LEGS, "mPelvis", "shape_legs", "shape_legs_param_list", "shape_legs_tab", Vector3d(0.0, 0.0, -0.5), Vector3d(-1.6, 0.15, -0.5), Sex.BOTH),
        Subpart.SKIN_COLOR to SubpartEntry(Subpart.SKIN_COLOR, "mHead", "skin_color", "skin_color_param_list", "skin_color_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.SKIN_FACEDETAIL to SubpartEntry(Subpart.SKIN_FACEDETAIL, "mHead", "skin_facedetail", "skin_face_param_list", "skin_face_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.SKIN_MAKEUP to SubpartEntry(Subpart.SKIN_MAKEUP, "mHead", "skin_makeup", "skin_makeup_param_list", "skin_makeup_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.SKIN_BODYDETAIL to SubpartEntry(Subpart.SKIN_BODYDETAIL, "mPelvis", "skin_bodydetail", "skin_body_param_list", "skin_body_tab", Vector3d(0.0, 0.0, -0.2), Vector3d(-2.5, 0.5, 0.5), Sex.BOTH),
        Subpart.HAIR_COLOR to SubpartEntry(Subpart.HAIR_COLOR, "mHead", "hair_color", "hair_color_param_list", "hair_color_tab", Vector3d(0.0, 0.0, 0.1), Vector3d(-0.4, 0.05, 0.1), Sex.BOTH),
        Subpart.HAIR_STYLE to SubpartEntry(Subpart.HAIR_STYLE, "mHead", "hair_style", "hair_style_param_list", "hair_style_tab", Vector3d(0.0, 0.0, 0.1), Vector3d(-0.4, 0.05, 0.1), Sex.BOTH),
        Subpart.HAIR_EYEBROWS to SubpartEntry(Subpart.HAIR_EYEBROWS, "mHead", "hair_eyebrows", "hair_eyebrows_param_list", "hair_eyebrows_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.HAIR_FACIAL to SubpartEntry(Subpart.HAIR_FACIAL, "mHead", "hair_facial", "hair_facial_param_list", "hair_facial_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.MALE),
        Subpart.EYES to SubpartEntry(Subpart.EYES, "mHead", "eyes", "eyes_main_param_list", "eyes_main_tab", Vector3d(0.0, 0.0, 0.05), Vector3d(-0.5, 0.05, 0.07), Sex.BOTH),
        Subpart.SHIRT to SubpartEntry(Subpart.SHIRT, "mTorso", "shirt", "shirt_main_param_list", "shirt_main_tab", Vector3d(0.0, 0.0, 0.3), Vector3d(-1.0, 0.15, 0.3), Sex.BOTH),
        Subpart.PANTS to SubpartEntry(Subpart.PANTS, "mPelvis", "pants", "pants_main_param_list", "pants_main_tab", Vector3d(0.0, 0.0, -0.5), Vector3d(-1.6, 0.15, -0.5), Sex.BOTH),
        Subpart.SHOES to SubpartEntry(Subpart.SHOES, "mPelvis", "shoes", "shoes_main_param_list", "shoes_main_tab", Vector3d(0.0, 0.0, -0.5), Vector3d(-1.6, 0.15, -0.5), Sex.BOTH),
        Subpart.SOCKS to SubpartEntry(Subpart.SOCKS, "mPelvis", "socks", "socks_main_param_list", "socks_main_tab", Vector3d(0.0, 0.0, -0.5), Vector3d(-1.6, 0.15, -0.5), Sex.BOTH),
        Subpart.JACKET to SubpartEntry(Subpart.JACKET, "mTorso", "jacket", "jacket_main_param_list", "jacket_main_tab", Vector3d(0.0, 0.0, 0.0), Vector3d(-2.0, 0.1, 0.3), Sex.BOTH),
        Subpart.GLOVES to SubpartEntry(Subpart.GLOVES, "mTorso", "gloves", "gloves_main_param_list", "gloves_main_tab", Vector3d(0.0, 0.0, 0.0), Vector3d(-1.0, 0.15, 0.0), Sex.BOTH),
        Subpart.UNDERSHIRT to SubpartEntry(Subpart.UNDERSHIRT, "mTorso", "undershirt", "undershirt_main_param_list", "undershirt_main_tab", Vector3d(0.0, 0.0, 0.3), Vector3d(-1.0, 0.15, 0.3), Sex.BOTH),
        Subpart.UNDERPANTS to SubpartEntry(Subpart.UNDERPANTS, "mPelvis", "underpants", "underpants_main_param_list", "underpants_main_tab", Vector3d(0.0, 0.0, -0.5), Vector3d(-1.6, 0.15, -0.5), Sex.BOTH),
        Subpart.SKIRT to SubpartEntry(Subpart.SKIRT, "mPelvis", "skirt", "skirt_main_param_list", "skirt_main_tab", Vector3d(0.0, 0.0, -0.5), Vector3d(-1.6, 0.15, -0.5), Sex.BOTH),
        Subpart.ALPHA to SubpartEntry(Subpart.ALPHA, "mPelvis", "alpha", "", "", Vector3d(0.0, 0.0, 0.1), Vector3d(-2.5, 0.5, 0.8), Sex.BOTH),
        Subpart.TATTOO to SubpartEntry(Subpart.TATTOO, "mPelvis", "tattoo", "", "", Vector3d(0.0, 0.0, 0.1), Vector3d(-2.5, 0.5, 0.8), Sex.BOTH),
        Subpart.UNIVERSAL to SubpartEntry(Subpart.UNIVERSAL, "mPelvis", "universal", "", "", Vector3d(0.0, 0.0, 0.1), Vector3d(-2.5, 0.5, 0.8), Sex.BOTH),
        Subpart.PHYSICS_BREASTS_UPDOWN to SubpartEntry(Subpart.PHYSICS_BREASTS_UPDOWN, "mTorso", "physics_breasts_updown", "physics_breasts_updown_param_list", "physics_breasts_updown_tab", Vector3d(0.0, 0.0, 0.3), Vector3d(0.0, 0.0, 0.0), Sex.FEMALE),
        Subpart.PHYSICS_BREASTS_INOUT to SubpartEntry(Subpart.PHYSICS_BREASTS_INOUT, "mTorso", "physics_breasts_inout", "physics_breasts_inout_param_list", "physics_breasts_inout_tab", Vector3d(0.0, 0.0, 0.3), Vector3d(0.0, 0.0, 0.0), Sex.FEMALE),
        Subpart.PHYSICS_BREASTS_LEFTRIGHT to SubpartEntry(Subpart.PHYSICS_BREASTS_LEFTRIGHT, "mTorso", "physics_breasts_leftright", "physics_breasts_leftright_param_list", "physics_breasts_leftright_tab", Vector3d(0.0, 0.0, 0.3), Vector3d(0.0, 0.0, 0.0), Sex.FEMALE),
        Subpart.PHYSICS_BELLY_UPDOWN to SubpartEntry(Subpart.PHYSICS_BELLY_UPDOWN, "mTorso", "physics_belly_updown", "physics_belly_updown_param_list", "physics_belly_tab", Vector3d(0.0, 0.0, 0.3), Vector3d(0.0, 0.0, 0.0), Sex.BOTH),
        Subpart.PHYSICS_BUTT_UPDOWN to SubpartEntry(Subpart.PHYSICS_BUTT_UPDOWN, "mTorso", "physics_butt_updown", "physics_butt_updown_param_list", "physics_butt_tab", Vector3d(0.0, 0.0, 0.3), Vector3d(0.0, 0.0, 0.0), Sex.BOTH),
        Subpart.PHYSICS_BUTT_LEFTRIGHT to SubpartEntry(Subpart.PHYSICS_BUTT_LEFTRIGHT, "mTorso", "physics_butt_leftright", "physics_butt_leftright_param_list", "physics_butt_leftright_tab", Vector3d(0.0, 0.0, 0.0), Vector3d(0.0, 0.0, 0.0), Sex.BOTH),
        Subpart.PHYSICS_ADVANCED to SubpartEntry(Subpart.PHYSICS_ADVANCED, "mTorso", "physics_advanced", "physics_advanced_param_list", "physics_advanced_tab", Vector3d(0.0, 0.0, 0.0), Vector3d(0.0, 0.0, 0.0), Sex.BOTH)
    )

    private fun buildColorSwatchCtrls(): Map<TextureIndex, PickerControlEntry> = mapOf(
        TextureIndex.UPPER_SHIRT to PickerControlEntry(TextureIndex.UPPER_SHIRT, "Color/Tint", null, false),
        TextureIndex.LOWER_PANTS to PickerControlEntry(TextureIndex.LOWER_PANTS, "Color/Tint", null, false),
        TextureIndex.LOWER_SHOES to PickerControlEntry(TextureIndex.LOWER_SHOES, "Color/Tint", null, false),
        TextureIndex.LOWER_SOCKS to PickerControlEntry(TextureIndex.LOWER_SOCKS, "Color/Tint", null, false),
        TextureIndex.UPPER_JACKET to PickerControlEntry(TextureIndex.UPPER_JACKET, "Color/Tint", null, false),
        TextureIndex.SKIRT to PickerControlEntry(TextureIndex.SKIRT, "Color/Tint", null, false),
        TextureIndex.UPPER_GLOVES to PickerControlEntry(TextureIndex.UPPER_GLOVES, "Color/Tint", null, false),
        TextureIndex.UPPER_UNDERSHIRT to PickerControlEntry(TextureIndex.UPPER_UNDERSHIRT, "Color/Tint", null, false),
        TextureIndex.LOWER_UNDERPANTS to PickerControlEntry(TextureIndex.LOWER_UNDERPANTS, "Color/Tint", null, false),
        TextureIndex.HEAD_TATTOO to PickerControlEntry(TextureIndex.HEAD_TATTOO, "Color/Tint", null, false),
        TextureIndex.HEAD_UNIVERSAL_TATTOO to PickerControlEntry(TextureIndex.HEAD_UNIVERSAL_TATTOO, "Color/Tint", null, false)
    )

    private fun buildTextureCtrls(): Map<TextureIndex, PickerControlEntry> = mapOf(
        TextureIndex.HEAD_BODYPAINT to PickerControlEntry(TextureIndex.HEAD_BODYPAINT, "Head", null, true),
        TextureIndex.UPPER_BODYPAINT to PickerControlEntry(TextureIndex.UPPER_BODYPAINT, "Upper Body", null, true),
        TextureIndex.LOWER_BODYPAINT to PickerControlEntry(TextureIndex.LOWER_BODYPAINT, "Lower Body", null, true),
        TextureIndex.HAIR to PickerControlEntry(TextureIndex.HAIR, "Texture", SavedSettings.getUUID("UIImgDefaultHairUUID"), false),
        TextureIndex.EYES_IRIS to PickerControlEntry(TextureIndex.EYES_IRIS, "Iris", SavedSettings.getUUID("UIImgDefaultEyesUUID"), false),
        TextureIndex.UPPER_SHIRT to PickerControlEntry(TextureIndex.UPPER_SHIRT, "Fabric", SavedSettings.getUUID("UIImgDefaultShirtUUID"), false),
        TextureIndex.LOWER_PANTS to PickerControlEntry(TextureIndex.LOWER_PANTS, "Fabric", SavedSettings.getUUID("UIImgDefaultPantsUUID"), false),
        TextureIndex.LOWER_SHOES to PickerControlEntry(TextureIndex.LOWER_SHOES, "Fabric", SavedSettings.getUUID("UIImgDefaultShoesUUID"), false),
        TextureIndex.LOWER_SOCKS to PickerControlEntry(TextureIndex.LOWER_SOCKS, "Fabric", SavedSettings.getUUID("UIImgDefaultSocksUUID"), false),
        TextureIndex.UPPER_JACKET to PickerControlEntry(TextureIndex.UPPER_JACKET, "Upper Fabric", SavedSettings.getUUID("UIImgDefaultJacketUUID"), false),
        TextureIndex.LOWER_JACKET to PickerControlEntry(TextureIndex.LOWER_JACKET, "Lower Fabric", SavedSettings.getUUID("UIImgDefaultJacketUUID"), false),
        TextureIndex.SKIRT to PickerControlEntry(TextureIndex.SKIRT, "Fabric", SavedSettings.getUUID("UIImgDefaultSkirtUUID"), false),
        TextureIndex.UPPER_GLOVES to PickerControlEntry(TextureIndex.UPPER_GLOVES, "Fabric", SavedSettings.getUUID("UIImgDefaultGlovesUUID"), false),
        TextureIndex.UPPER_UNDERSHIRT to PickerControlEntry(TextureIndex.UPPER_UNDERSHIRT, "Fabric", SavedSettings.getUUID("UIImgDefaultUnderwearUUID"), false),
        TextureIndex.LOWER_UNDERPANTS to PickerControlEntry(TextureIndex.LOWER_UNDERPANTS, "Fabric", SavedSettings.getUUID("UIImgDefaultUnderwearUUID"), false),
        TextureIndex.LOWER_ALPHA to PickerControlEntry(TextureIndex.LOWER_ALPHA, "Lower Alpha", SavedSettings.getUUID("UIImgDefaultAlphaUUID"), true),
        TextureIndex.UPPER_ALPHA to PickerControlEntry(TextureIndex.UPPER_ALPHA, "Upper Alpha", SavedSettings.getUUID("UIImgDefaultAlphaUUID"), true),
        TextureIndex.HEAD_ALPHA to PickerControlEntry(TextureIndex.HEAD_ALPHA, "Head Alpha", SavedSettings.getUUID("UIImgDefaultAlphaUUID"), true),
        TextureIndex.EYES_ALPHA to PickerControlEntry(TextureIndex.EYES_ALPHA, "Eye Alpha", SavedSettings.getUUID("UIImgDefaultAlphaUUID"), true),
        TextureIndex.HAIR_ALPHA to PickerControlEntry(TextureIndex.HAIR_ALPHA, "Hair Alpha", SavedSettings.getUUID("UIImgDefaultAlphaUUID"), true),
        TextureIndex.LOWER_TATTOO to PickerControlEntry(TextureIndex.LOWER_TATTOO, "Lower Tattoo", null, true),
        TextureIndex.UPPER_TATTOO to PickerControlEntry(TextureIndex.UPPER_TATTOO, "Upper Tattoo", null, true),
        TextureIndex.HEAD_TATTOO to PickerControlEntry(TextureIndex.HEAD_TATTOO, "Head Tattoo", null, true),
        TextureIndex.LOWER_UNIVERSAL_TATTOO to PickerControlEntry(TextureIndex.LOWER_UNIVERSAL_TATTOO, "Lower Universal Tattoo", null, true),
        TextureIndex.UPPER_UNIVERSAL_TATTOO to PickerControlEntry(TextureIndex.UPPER_UNIVERSAL_TATTOO, "Upper Universal Tattoo", null, true),
        TextureIndex.HEAD_UNIVERSAL_TATTOO to PickerControlEntry(TextureIndex.HEAD_UNIVERSAL_TATTOO, "Head Universal Tattoo", null, true),
        TextureIndex.SKIRT_TATTOO to PickerControlEntry(TextureIndex.SKIRT_TATTOO, "Skirt Tattoo", null, true),
        TextureIndex.HAIR_TATTOO to PickerControlEntry(TextureIndex.HAIR_TATTOO, "Hair Tattoo", null, true),
        TextureIndex.EYES_TATTOO to PickerControlEntry(TextureIndex.EYES_TATTOO, "Eyes Tattoo", null, true),
        TextureIndex.LEFT_ARM_TATTOO to PickerControlEntry(TextureIndex.LEFT_ARM_TATTOO, "Left Arm Tattoo", null, true),
        TextureIndex.LEFT_LEG_TATTOO to PickerControlEntry(TextureIndex.LEFT_LEG_TATTOO, "Left Leg Tattoo", null, true),
        TextureIndex.AUX1_TATTOO to PickerControlEntry(TextureIndex.AUX1_TATTOO, "Aux1 Tattoo", null, true),
        TextureIndex.AUX2_TATTOO to PickerControlEntry(TextureIndex.AUX2_TATTOO, "Aux2 Tattoo", null, true),
        TextureIndex.AUX3_TATTOO to PickerControlEntry(TextureIndex.AUX3_TATTOO, "Aux3 Tattoo", null, true)
    )
}

class PanelEditWearable : Panel(), WearableObserver {

    private var wearablePtr: ViewerWearable? = null
    private var wearableItem: ViewerInventoryItem? = null

    private var btnSaveAs: Button? = null
    private var btnRevert: Button? = null
    private var btnBack: Button? = null
    private var backBtnLabel: String = ""

    private var panelTitle: TextBox? = null
    private var descTitle: TextBox? = null
    private var txtAvatarHeight: TextBox? = null

    private var sexRadio: RadioGroup? = null
    private var maleIcon: IconCtrl? = null
    private var femaleIcon: IconCtrl? = null

    private var meters: String = ""
    private var feet: String = ""
    private var height: String = ""
    private var heightValue: UIString = UIString("[HEIGHT] [METRIC1]")
    private var replacementMetricUrl: UIString = UIString("[URL_METRIC2]")

    private var avatarHeightLabelColor: UIColor = UIColor.green
    private var avatarHeightValueLabelColor: UIColor = UIColor.green

    private var nameEditor: LineEditor? = null

    private var panelShape: Panel? = null
    private var panelSkin: Panel? = null
    private var panelEyes: Panel? = null
    private var panelHair: Panel? = null
    private var panelShirt: Panel? = null
    private var panelPants: Panel? = null
    private var panelShoes: Panel? = null
    private var panelSocks: Panel? = null
    private var panelJacket: Panel? = null
    private var panelGloves: Panel? = null
    private var panelUndershirt: Panel? = null
    private var panelUnderpants: Panel? = null
    private var panelSkirt: Panel? = null
    private var panelAlpha: Panel? = null
    private var panelTattoo: Panel? = null
    private var panelUniversal: Panel? = null
    private var panelPhysics: Panel? = null

    private val accordionTabs: MutableMap<String, AccordionCtrlTab> = mutableMapOf()
    private val paramPanels: MutableMap<String, ScrollingPanelList> = mutableMapOf()
    private val alphaCheckbox2Index: MutableList<Pair<CheckBoxCtrl, TextureIndex>> = mutableListOf()
    private val previousAlphaTexture: MutableMap<TextureIndex, UUID> = mutableMapOf()
    private val lastShownSubpartIndex: MutableMap<WearableType, UByte> = mutableMapOf()

    override fun onDestroyed(wearable: ViewerWearable) {
        wearablePtr = null
    }

    open fun postBuild(): Boolean {
        btnRevert = getChild<Button>("revert_button")
        btnRevert?.setClickedCallback { onRevertButtonClicked(this) }

        btnBack = getChild<Button>("back_btn")
        backBtnLabel = btnBack?.getLabelUnselected() ?: ""
        btnBack?.setLabel("")
        btnBack?.setClickedCallback { onBackButtonClicked(this) }

        childSetAction("import_btn") { onClickedImportBtn() }

        nameEditor = getChild<LineEditor>("description")
        panelTitle = getChild<TextBox>("edit_wearable_title")
        descTitle = getChild<TextBox>("description_text")

        sexRadio = getChild<RadioGroup>("sex_radio")
        sexRadio?.setCommitCallback { onCommitSexChange() }

        maleIcon = getChild<IconCtrl>("male_icon")
        femaleIcon = getChild<IconCtrl>("female_icon")

        btnSaveAs = getChild<Button>("save_as_button")
        btnSaveAs?.setCommitCallback { onSaveAsButtonClicked() }

        panelShape = getChild<Panel>("edit_shape_panel")
        panelSkin = getChild<Panel>("edit_skin_panel")
        panelEyes = getChild<Panel>("edit_eyes_panel")
        panelHair = getChild<Panel>("edit_hair_panel")
        panelShirt = getChild<Panel>("edit_shirt_panel")
        panelPants = getChild<Panel>("edit_pants_panel")
        panelShoes = getChild<Panel>("edit_shoes_panel")
        panelSocks = getChild<Panel>("edit_socks_panel")
        panelJacket = getChild<Panel>("edit_jacket_panel")
        panelGloves = getChild<Panel>("edit_gloves_panel")
        panelUndershirt = getChild<Panel>("edit_undershirt_panel")
        panelUnderpants = getChild<Panel>("edit_underpants_panel")
        panelSkirt = getChild<Panel>("edit_skirt_panel")
        panelAlpha = getChild<Panel>("edit_alpha_panel")
        panelTattoo = getChild<Panel>("edit_tattoo_panel")
        panelUniversal = getChild<Panel>("edit_universal_panel")
        panelPhysics = getChild<Panel>("edit_physics_panel")

        txtAvatarHeight = panelShape?.getChild<TextBox>("avatar_height")
        wearablePtr = null

        configureAlphaCheckbox(TextureIndex.LOWER_ALPHA, "lower alpha texture invisible")
        configureAlphaCheckbox(TextureIndex.UPPER_ALPHA, "upper alpha texture invisible")
        configureAlphaCheckbox(TextureIndex.HEAD_ALPHA, "head alpha texture invisible")
        configureAlphaCheckbox(TextureIndex.EYES_ALPHA, "eye alpha texture invisible")
        configureAlphaCheckbox(TextureIndex.HAIR_ALPHA, "hair alpha texture invisible")

        for (wearableType in WearableType.values()) {
            val wearableEntry = EditWearableDictionary.getWearable(wearableType) ?: continue

            for ((index, subpartEnum) in wearableEntry.subparts.withIndex()) {
                val subpartEntry = EditWearableDictionary.getSubpart(subpartEnum) ?: continue

                val accordionTab = subpartEntry.accordionTab
                if (accordionTab.isEmpty()) continue

                val tab = findChild<AccordionCtrlTab>(accordionTab) ?: continue
                accordionTabs[accordionTab] = tab
                tab.setDropDownStateChangedCallback { param ->
                    onTabExpandedCollapsed(param, index.toUByte())
                }

                val scrollingPanel = subpartEntry.paramList
                if (scrollingPanel.isNotEmpty()) {
                    val panelList = tab.findChild<ScrollingPanelList>(scrollingPanel)
                    if (panelList != null) {
                        paramPanels[scrollingPanel] = panelList
                    }
                }

                val tabContainer = tab.parent?.parent?.parent as? TabContainer
                tabContainer?.setCommitCallback { ctrl -> onTabChanged(ctrl, wearableType) }
            }

            initPickerCtrls(getPanel(wearableType), wearableType)
        }

        meters = panelShape?.getString("meters") ?: ""
        feet = panelShape?.getString("feet") ?: ""
        height = (panelShape?.getString("height") ?: "") + " "
        heightValue = UIString("[HEIGHT] [METRIC1]")
        replacementMetricUrl = UIString("[URL_METRIC2]")

        val heightLabelColorName = panelShape?.getString("height_label_color") ?: ""
        avatarHeightLabelColor = UIColorTable.instance().getColor(heightLabelColorName, UIColor.green)
        val heightValueColorName = panelShape?.getString("height_value_label_color") ?: ""
        avatarHeightValueLabelColor = UIColorTable.instance().getColor(heightValueColorName, UIColor.green)

        SavedSettings.getControl("HeightUnits")?.getSignal()?.connect { newValue ->
            changeHeightUnits(newValue)
        }
        updateMetricLayout(SavedSettings.getBool("HeightUnits"))

        return true
    }

    open fun isDirty(): Boolean {
        val ptr = wearablePtr ?: return false
        return ptr.isDirty() || (wearableItem != null && nameEditor != null &&
            wearableItem!!.getName() != nameEditor!!.getText())
    }

    open fun draw() {
        updateVerbs()
        if (getWearable()?.getType() == WearableType.SHAPE) {
            updateTypeSpecificControls(WearableType.SHAPE)
        }
        super.draw()
    }

    fun onClose() {
        revertChanges()
    }

    override fun setVisible(visible: Boolean) {
        if (!visible) {
            showWearable(wearablePtr, false)
        }
        super.setVisible(visible)
    }

    fun getWearable(): ViewerWearable? = wearablePtr

    fun setWearable(wearable: ViewerWearable?, disableCameraSwitch: Boolean = false) {
        showWearable(wearablePtr, false, disableCameraSwitch)
        wearablePtr?.unregisterObserver(this)
        wearablePtr = wearable
        wearablePtr?.registerObserver(this)
        showWearable(wearablePtr, true, disableCameraSwitch)
    }

    fun saveChanges(forceSaveAs: Boolean = false) {
        val ptr = wearablePtr ?: return
        if (!isDirty()) return

        val index = UIntArray(1)
        if (!AgentWearables.instance().getWearableIndex(ptr, index)) return

        val newName = nameEditor?.getText() ?: ""

        val links = AppearanceMgr.instance().findCOFItemLinks(ptr.getItemID())
        val linkItem = links.firstOrNull()
        val description = if (linkItem?.getIsLinkType() == true) linkItem.getActualDescription() else ""

        if (forceSaveAs) {
            AppearanceMgr.instance().removeCOFItemLinks(ptr.getItemID(), AgentAvatarSelf.instance().mEndCustomizeCallback)
            AgentWearables.instance().saveWearableAs(ptr.getType(), index[0], newName, description, false)
            nameEditor?.setText(wearableItem?.getName() ?: "")
        } else {
            if (linkItem != null) {
                linkInventoryArray(
                    AppearanceMgr.instance().getCOF(),
                    listOf(linkItem),
                    AgentAvatarSelf.instance().mEndCustomizeCallback
                )
                removeInventoryItem(linkItem.getUUID(), AgentAvatarSelf.instance().mEndCustomizeCallback)
            }
            AgentWearables.instance().saveWearable(ptr.getType(), index[0], true, newName)
        }
    }

    fun revertChanges() {
        val ptr = wearablePtr ?: return
        if (!isDirty()) return

        ptr.revertValues()
        nameEditor?.setText(wearableItem?.getName() ?: "")
        updatePanelPickerControls(ptr.getType())
        updateTypeSpecificControls(ptr.getType())
        AgentAvatarSelf.instance().wearableUpdated(ptr.getType(), false)
    }

    fun showDefaultSubpart() {
        val ptr = wearablePtr ?: return
        val lastIndex = lastShownSubpartIndex[ptr.getType()]
        if (lastIndex != null) {
            changeCamera(lastIndex)
        } else {
            changeCamera(0u)
        }
    }

    fun onTabExpandedCollapsed(param: LLSD, index: UByte) {
        val expanded = param.asBoolean()
        val ptr = wearablePtr ?: return
        if (!AgentCamera.instance().cameraCustomizeAvatar()) return

        if (expanded) {
            lastShownSubpartIndex[ptr.getType()] = index
            changeCamera(index)
        }
    }

    fun onTabChanged(ctrl: UICtrl, type: WearableType) {
        val container = ctrl as? TabContainer ?: return
        val ptr = wearablePtr ?: return
        if (!AgentCamera.instance().cameraCustomizeAvatar()) return

        val wearableEntry = EditWearableDictionary.getWearable(type) ?: return
        for ((index, subpartEnum) in wearableEntry.subparts.withIndex()) {
            val subpartEntry = EditWearableDictionary.getSubpart(subpartEnum) ?: continue
            if (container.getCurrentPanel()?.hasChild(subpartEntry.accordionTab) == true) {
                lastShownSubpartIndex[type] = index.toUByte()
                changeCamera(index.toUByte())
                break
            }
        }
    }

    fun changeCamera(subpart: UByte) {
        val ptr = wearablePtr ?: return
        if (WearableType.getInstance().getDisableCameraSwitch(ptr.getType())) return

        val wearableEntry = EditWearableDictionary.getWearable(ptr.getType()) ?: return
        if (subpart >= wearableEntry.subparts.size.toUByte()) return

        val subpartEnum = wearableEntry.subparts[subpart.toInt()]
        val subpartEntry = EditWearableDictionary.getSubpart(subpartEnum) ?: return

        TODO("GPU: MorphView camera update for joint ${subpartEntry.targetJoint}")
    }

    fun updateScrollingPanelList() {
        updateScrollingPanelUI()
    }

    fun onCommitSexChange() {
        if (!isAgentAvatarValid()) return

        val type = wearablePtr?.getType() ?: return
        val index = UIntArray(1)
        if (!AgentWearables.instance().getWearableIndex(wearablePtr!!, index) ||
            !AgentWearables.instance().isWearableModifiable(type, index[0])
        ) return

        val param = AgentAvatarSelf.instance().getVisualParam("male") as? ViewerVisualParam ?: return
        val isNewSexMale = SavedSettings.getUInt("AvatarSex") != 0u

        val wearable = AgentWearables.instance().getViewerWearable(type, index[0])
        wearable?.setVisualParamWeight(param.getID(), isNewSexMale, false)

        param.setWeight(isNewSexMale, false)
        AgentAvatarSelf.instance().updateSexDependentLayerSets(false)
        AgentAvatarSelf.instance().updateVisualParams()
        showWearable(wearablePtr, true, true)
        updateScrollingPanelUI()
    }

    fun onSaveAsButtonClicked() {
        val args = LLSD()
        args["DESC"] = nameEditor?.getText() ?: ""
        NotificationsUtil.add("SaveWearableAs", args, LLSD()) { notification, response ->
            saveAsCallback(notification, response)
        }
    }

    fun saveAsCallback(notification: LLSD, response: LLSD) {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) {
            val wearableName = response["message"].asString().trim()
            if (wearableName.isNotEmpty()) {
                nameEditor?.setText(wearableName)
                saveChanges(true)
            }
        }
    }

    private fun showWearable(wearable: ViewerWearable?, show: Boolean, disableCameraSwitch: Boolean = false) {
        wearable ?: return

        wearableItem = Inventory.getInstance().getItem(wearablePtr!!.getItemID())

        val type = wearable.getType()
        val wearableEntry = EditWearableDictionary.getWearable(type) ?: return
        val targetPanel = getPanel(type) ?: return

        val title = getString(wearableEntry.title)
        val descriptionTitle = getString(wearableEntry.descTitle)

        forEachColorSwatchEntry(targetPanel, type) { panel, entry ->
            panel.getChild<ColorSwatchCtrl>(entry.controlName)?.setEnabled(show)
        }
        forEachTextureEntry(targetPanel, type) { panel, entry ->
            panel.getChild<TextureCtrl>(entry.controlName)?.setEnabled(show)
        }

        targetPanel.setVisible(show)
        toggleTypeSpecificControls(type)
        updateTypeSpecificControls(type)

        if (show) {
            panelTitle?.setText(title)
            panelTitle?.setToolTip(title)
            descTitle?.setText(descriptionTitle)
            nameEditor?.setText(wearableItem?.getName() ?: "")
            updatePanelPickerControls(type)

            for ((index, subpartEnum) in wearableEntry.subparts.withIndex()) {
                val subpartEntry = EditWearableDictionary.getSubpart(subpartEnum) ?: continue
                val scrollingPanel = subpartEntry.paramList
                val accordionTab = subpartEntry.accordionTab

                if (scrollingPanel.isEmpty() || accordionTab.isEmpty()) continue

                val tab = accordionTabs[accordionTab] ?: continue
                val panelList = paramPanels[scrollingPanel] ?: continue

                if (!AgentAvatarSelf.instance().getSex().matches(subpartEntry.sex)) {
                    tab.setVisible(false)
                    continue
                } else {
                    tab.setVisible(true)
                }

                val sortedParams = sortedMapOf<Float, ViewerVisualParam>()
                getSortedParams(sortedParams, subpartEntry.editGroup)

                val joint = AgentAvatarSelf.instance().getJoint(subpartEntry.targetJoint)
                    ?: AgentAvatarSelf.instance().getJoint("mHead")

                buildParamList(panelList, sortedParams, tab, joint)
                updateScrollingPanelUI()
            }

            if (!disableCameraSwitch) {
                showDefaultSubpart()
            }
            updateVerbs()
        }
    }

    private fun updateScrollingPanelUI() {
        val ptr = wearablePtr ?: return
        val type = ptr.getType()
        val panel = getPanel(type) ?: return
        if (ptr.getItemID() == null) return

        val wearableEntry = EditWearableDictionary.getWearable(type) ?: return
        ScrollingPanelParam.sUpdateDelayFrames = 0

        for (subpartEnum in wearableEntry.subparts) {
            val subpartEntry = EditWearableDictionary.getSubpart(subpartEnum) ?: continue
            val scrollingPanel = subpartEntry.paramList
            val panelList = paramPanels[scrollingPanel] ?: continue
            panelList.updatePanels(true)
        }
    }

    private fun getPanel(type: WearableType): Panel? = when (type) {
        WearableType.SHAPE -> panelShape
        WearableType.SKIN -> panelSkin
        WearableType.HAIR -> panelHair
        WearableType.EYES -> panelEyes
        WearableType.SHIRT -> panelShirt
        WearableType.PANTS -> panelPants
        WearableType.SHOES -> panelShoes
        WearableType.SOCKS -> panelSocks
        WearableType.JACKET -> panelJacket
        WearableType.GLOVES -> panelGloves
        WearableType.UNDERSHIRT -> panelUndershirt
        WearableType.UNDERPANTS -> panelUnderpants
        WearableType.SKIRT -> panelSkirt
        WearableType.ALPHA -> panelAlpha
        WearableType.TATTOO -> panelTattoo
        WearableType.UNIVERSAL -> panelUniversal
        WearableType.PHYSICS -> panelPhysics
        else -> null
    }

    private fun getSortedParams(sortedParams: MutableMap<Float, ViewerVisualParam>, editGroup: String) {
        val paramList = mutableListOf<VisualParam>()
        val avatarSex = AgentAvatarSelf.instance().getSex()

        wearablePtr?.getVisualParams(paramList)

        for (param in paramList) {
            val viewerParam = param as? ViewerVisualParam ?: continue
            if (viewerParam.getID() == -1 ||
                !viewerParam.isTweakable() ||
                viewerParam.getEditGroup() != editGroup ||
                !avatarSex.matches(viewerParam.getSex())
            ) continue

            sortedParams[-viewerParam.getDisplayOrder()] = viewerParam
        }
    }

    private fun buildParamList(
        panelList: ScrollingPanelList,
        sortedParams: Map<Float, ViewerVisualParam>,
        tab: AccordionCtrlTab,
        joint: Joint?
    ) {
        val showHints = SavedSettings.getBool("FSAppearanceShowHints")
        panelList.clearPanels()

        for ((_, param) in sortedParams) {
            val wearable = getWearable()
            val isPhysics = wearable?.getType() == WearableType.PHYSICS
            val panelParam: ScrollingPanelParamBase = if (!showHints || isPhysics) {
                ScrollingPanelParamBase(null, param, true, wearable, joint)
            } else {
                ScrollingPanelParam(null, param, true, wearable, joint)
            }
            panelList.addPanel(panelParam)
        }
    }

    private fun updateVerbs() {
        val canCopy = wearableItem?.getPermissions()?.allowCopyBy(Agent.instance().getID()) == true
        val dirty = isDirty()

        btnRevert?.setEnabled(dirty)
        btnSaveAs?.setEnabled(dirty && canCopy)
        childSetEnabled("import_btn", wearableItem?.getPermissions()?.allowModifyBy(Agent.instance().getID()) == true)

        if (isAgentAvatarValid()) {
            SavedSettings.setUInt("AvatarSex", if (AgentAvatarSelf.instance().getSex() == Sex.MALE) 1u else 0u)
        }

        // Update back button label width dynamically when dirty state changes.
        val label = if (dirty) backBtnLabel else ""
        btnBack?.setLabel(label)
    }

    private fun toggleTypeSpecificControls(type: WearableType) {
        val isShape = type == WearableType.SHAPE
        sexRadio?.setVisible(isShape)
        femaleIcon?.setVisible(isShape)
        maleIcon?.setVisible(isShape)
    }

    private fun updateTypeSpecificControls(type: WearableType) {
        val oneMeter = 1.0f
        val oneFoot = 0.3048f * oneMeter

        if (type == WearableType.SHAPE) {
            var newSize = AgentAvatarSelf.instance().mBodySize.z + 0.195f
            if (!SavedSettings.getBool("HeightUnits")) {
                newSize /= oneFoot
            }
            val avatarHeightStr = "%.2f".format(newSize)
            heightValue.setArg("[HEIGHT]", avatarHeightStr)
            updateAvatarHeightLabel()
        }

        if (type == WearableType.ALPHA) {
            updateAlphaCheckboxes()
            initPreviousAlphaTextures()
        }
    }

    private fun updateAvatarHeightLabel() {
        txtAvatarHeight?.setText("")
        txtAvatarHeight?.appendText(height, false, avatarHeightLabelColor)
        txtAvatarHeight?.appendText(heightValue.toString(), false, avatarHeightValueLabelColor)
        txtAvatarHeight?.appendText(" / ", false, avatarHeightLabelColor)
        txtAvatarHeight?.appendText(replacementMetricUrl.toString(), false, avatarHeightLabelColor)
    }

    private fun changeHeightUnits(newValue: LLSD): Boolean {
        updateMetricLayout(newValue.asBoolean())
        updateTypeSpecificControls(WearableType.SHAPE)
        return true
    }

    private fun updateMetricLayout(newValue: Boolean) {
        val currentMetric = if (newValue) meters else feet
        val replacementMetric = if (newValue) feet else meters
        heightValue.setArg("[METRIC1]", currentMetric)
        replacementMetricUrl.setArg("[URL_METRIC2]", "[secondlife:///app/metricsystem $replacementMetric]")
    }

    private fun updatePanelPickerControls(type: WearableType) {
        val panel = getPanel(type) ?: return
        val isModifiable = wearableItem?.getPermissions()?.allowModifyBy(
            Agent.instance().getID(), Agent.instance().getGroupID()
        ) == true

        if (isModifiable) {
            forEachColorSwatchEntry(panel, type) { p, entry ->
                p.getChild<ColorSwatchCtrl>(entry.controlName)?.let { ctrl ->
                    ctrl.set(wearablePtr!!.getClothesColor(entry.textureIndex))
                    ctrl.closeFloaterColorPicker()
                }
            }
            forEachTextureEntry(panel, type) { p, entry ->
                p.getChild<TextureCtrl>(entry.controlName)?.let { ctrl ->
                    val lto = wearablePtr!!.getLocalTextureObject(entry.textureIndex)
                    val newId = if (lto != null && lto.getID() != IMG_DEFAULT_AVATAR) lto.getID() else null
                    if (ctrl.getImageAssetID() != newId) ctrl.closeDependentFloater()
                    ctrl.setImageAssetID(newId)
                }
            }
        } else {
            forEachColorSwatchEntry(panel, type) { p, entry ->
                p.getChild<ColorSwatchCtrl>(entry.controlName)?.setEnabled(false)
            }
            forEachTextureEntry(panel, type) { p, entry ->
                p.getChild<TextureCtrl>(entry.controlName)?.setEnabled(false)
            }
        }
    }

    private fun initPickerCtrls(panel: Panel?, type: WearableType) {
        panel ?: return
        forEachColorSwatchEntry(panel, type) { p, entry ->
            p.getChild<ColorSwatchCtrl>(entry.controlName)?.setOriginal(Color4.white)
        }
        forEachTextureEntry(panel, type) { p, entry ->
            p.getChild<TextureCtrl>(entry.controlName)?.let { ctrl ->
                ctrl.setDefaultImageAssetID(entry.defaultImageId)
                ctrl.setAllowNoTexture(entry.allowNoTexture)
                ctrl.setImmediateFilterPermMask(Perm.NONE)
                ctrl.setDnDFilterPermMask(Perm.NONE)
            }
        }
    }

    private fun forEachColorSwatchEntry(panel: Panel?, type: WearableType, fn: (Panel, PickerControlEntry) -> Unit) {
        panel ?: return
        val entry = EditWearableDictionary.getWearable(type) ?: return
        for (texIndex in entry.colorSwatchCtrls) {
            val ctrl = EditWearableDictionary.getColorSwatch(texIndex) ?: continue
            fn(panel, ctrl)
        }
    }

    private fun forEachTextureEntry(panel: Panel?, type: WearableType, fn: (Panel, PickerControlEntry) -> Unit) {
        panel ?: return
        val entry = EditWearableDictionary.getWearable(type) ?: return
        for (texIndex in entry.textureCtrls) {
            val ctrl = EditWearableDictionary.getTexturePicker(texIndex) ?: continue
            fn(panel, ctrl)
        }
    }

    private fun onColorSwatchCommit(ctrl: UICtrl) {
        val wearable = getWearable() ?: return
        val type = wearable.getType()
        val entry = EditWearableDictionary.getWearable(type)?.colorSwatchCtrls
            ?.mapNotNull { EditWearableDictionary.getColorSwatch(it) }
            ?.firstOrNull { it.controlName == ctrl.getName() } ?: return

        val oldColor = wearable.getClothesColor(entry.textureIndex)
        val newColor = Color4(ctrl.getValue())
        if (oldColor != newColor) {
            wearable.setClothesColor(entry.textureIndex, newColor, true)
            VisualParamHint.requestHintUpdates()
            AgentAvatarSelf.instance().wearableUpdated(wearable.getType(), false)
        }
    }

    private fun onTexturePickerCommit(ctrl: UICtrl) {
        val textureCtrl = ctrl as? TextureCtrl ?: return
        val wearable = getWearable() ?: return
        val type = wearable.getType()

        val entry = EditWearableDictionary.getWearable(type)?.textureCtrls
            ?.mapNotNull { EditWearableDictionary.getTexturePicker(it) }
            ?.firstOrNull { it.controlName == textureCtrl.getName() } ?: return

        val image = ViewerTextureManager.getFetchedTexture(textureCtrl.getImageAssetID())
        val resolvedImage = if (image.getID() == IMG_DEFAULT) {
            ViewerTextureManager.getFetchedTexture(IMG_DEFAULT_AVATAR)
        } else {
            image
        }

        val index = UIntArray(1)
        if (AgentWearables.instance().getWearableIndex(wearable, index)) {
            AgentAvatarSelf.instance().setLocalTexture(entry.textureIndex, resolvedImage, false, index[0])
            VisualParamHint.requestHintUpdates()
            AgentAvatarSelf.instance().wearableUpdated(type, false)
        }
    }

    private fun configureAlphaCheckbox(te: TextureIndex, name: String) {
        val checkbox = panelAlpha?.getChild<CheckBoxCtrl>(name) ?: return
        checkbox.setCommitCallback { onInvisibilityCommit(checkbox, te) }
        alphaCheckbox2Index.add(Pair(checkbox, te))
    }

    private fun onInvisibilityCommit(checkboxCtrl: CheckBoxCtrl, te: TextureIndex) {
        val wearable = getWearable() ?: return
        val index = UIntArray(1)
        if (!AgentWearables.instance().getWearableIndex(wearable, index)) return

        val newInvisState = checkboxCtrl.get()
        if (newInvisState) {
            val lto = wearable.getLocalTextureObject(te)
            if (lto != null) previousAlphaTexture[te] = lto.getID()

            val image = ViewerTextureManager.getFetchedTexture(IMG_INVISIBLE)
            AgentAvatarSelf.instance().setLocalTexture(te, image, false, index[0])
            AgentAvatarSelf.instance().wearableUpdated(wearable.getType(), false)
        } else {
            var prevId = previousAlphaTexture[te]
            if (prevId == null || prevId == IMG_INVISIBLE) {
                prevId = SavedSettings.getUUID("UIImgDefaultAlphaUUID") ?: return
            }
            val image = ViewerTextureManager.getFetchedTexture(prevId) ?: return
            AgentAvatarSelf.instance().setLocalTexture(te, image, false, index[0])
            AgentAvatarSelf.instance().wearableUpdated(wearable.getType(), false)
        }

        updatePanelPickerControls(wearable.getType())
    }

    private fun updateAlphaCheckboxes() {
        for ((ctrl, te) in alphaCheckbox2Index) {
            ctrl.set(!AgentAvatarSelf.instance().isTextureVisible(te, wearablePtr))
        }
    }

    private fun initPreviousAlphaTextures() {
        for (te in listOf(
            TextureIndex.LOWER_ALPHA, TextureIndex.UPPER_ALPHA,
            TextureIndex.HEAD_ALPHA, TextureIndex.EYES_ALPHA, TextureIndex.HAIR_ALPHA
        )) {
            val lto = getWearable()?.getLocalTextureObject(te)
            if (lto != null) previousAlphaTexture[te] = lto.getID()
        }
    }

    private fun onClickedImportBtn() {
        TODO("APR: use JVM equivalent for file picker (LLFilePicker)")
    }

    fun open(info: LLSD) {}

    companion object {
        fun onRevertButtonClicked(panel: PanelEditWearable) {
            panel.revertChanges()
        }

        fun onBackButtonClicked(panel: PanelEditWearable) {
            if (panel.isDirty()) {
                AppearanceMgr.instance().setOutfitDirty(true)
            }
        }
    }
}
