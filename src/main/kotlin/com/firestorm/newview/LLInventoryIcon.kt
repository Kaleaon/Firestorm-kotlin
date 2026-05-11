package com.firestorm.newview

import java.util.UUID

object LLInventoryIcon {

    private val iconNames: MutableMap<LLInventoryType.EIconName, String> = mutableMapOf(
        LLInventoryType.EIconName.ICONNAME_TEXTURE to "Inv_Texture",
        LLInventoryType.EIconName.ICONNAME_SOUND to "Inv_Sound",
        LLInventoryType.EIconName.ICONNAME_CALLINGCARD_ONLINE to "Inv_CallingCard",
        LLInventoryType.EIconName.ICONNAME_CALLINGCARD_OFFLINE to "Inv_CallingCardOffline",
        LLInventoryType.EIconName.ICONNAME_LANDMARK to "Inv_Landmark",
        LLInventoryType.EIconName.ICONNAME_LANDMARK_VISITED to "Inv_LandmarkVisited",
        LLInventoryType.EIconName.ICONNAME_SCRIPT to "Inv_Script",
        LLInventoryType.EIconName.ICONNAME_CLOTHING to "Inv_Clothing",
        LLInventoryType.EIconName.ICONNAME_OBJECT to "Inv_Object",
        LLInventoryType.EIconName.ICONNAME_OBJECT_MULTI to "Inv_Object_Multi",
        LLInventoryType.EIconName.ICONNAME_NOTECARD to "Inv_Notecard",
        LLInventoryType.EIconName.ICONNAME_BODYPART to "Inv_Skin",
        LLInventoryType.EIconName.ICONNAME_SNAPSHOT to "Inv_Snapshot",
        LLInventoryType.EIconName.ICONNAME_BODYPART_SHAPE to "Inv_BodyShape",
        LLInventoryType.EIconName.ICONNAME_BODYPART_SKIN to "Inv_Skin",
        LLInventoryType.EIconName.ICONNAME_BODYPART_HAIR to "Inv_Hair",
        LLInventoryType.EIconName.ICONNAME_BODYPART_EYES to "Inv_Eye",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_SHIRT to "Inv_Shirt",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_PANTS to "Inv_Pants",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_SHOES to "Inv_Shoe",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_SOCKS to "Inv_Socks",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_JACKET to "Inv_Jacket",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_GLOVES to "Inv_Gloves",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_UNDERSHIRT to "Inv_Undershirt",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_UNDERPANTS to "Inv_Underpants",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_SKIRT to "Inv_Skirt",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_ALPHA to "Inv_Alpha",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_TATTOO to "Inv_Tattoo",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_UNIVERSAL to "Inv_Universal",
        LLInventoryType.EIconName.ICONNAME_ANIMATION to "Inv_Animation",
        LLInventoryType.EIconName.ICONNAME_GESTURE to "Inv_Gesture",
        LLInventoryType.EIconName.ICONNAME_CLOTHING_PHYSICS to "Inv_Physics",
        LLInventoryType.EIconName.ICONNAME_LINKITEM to "Inv_LinkItem",
        LLInventoryType.EIconName.ICONNAME_LINKFOLDER to "Inv_LinkFolder",
        LLInventoryType.EIconName.ICONNAME_MESH to "Inv_Mesh",
        LLInventoryType.EIconName.ICONNAME_SETTINGS_SKY to "Inv_SettingsSky",
        LLInventoryType.EIconName.ICONNAME_SETTINGS_WATER to "Inv_SettingsWater",
        LLInventoryType.EIconName.ICONNAME_SETTINGS_DAY to "Inv_SettingsDay",
        LLInventoryType.EIconName.ICONNAME_SETTINGS to "Inv_Settings",
        LLInventoryType.EIconName.ICONNAME_MATERIAL to "Inv_Material",
        LLInventoryType.EIconName.ICONNAME_INVALID to "Inv_Invalid",
        LLInventoryType.EIconName.ICONNAME_UNKNOWN to "Inv_Unknown",
        LLInventoryType.EIconName.ICONNAME_NONE to "NONE"
    )

    fun getIconName(
        assetType: LLAssetType.EType,
        inventoryType: LLInventoryType.EType = LLInventoryType.EType.IT_NONE,
        miscFlag: UInt = 0u,
        itemIsMulti: Boolean = false
    ): String {
        val idx = resolveIconName(assetType, inventoryType, miscFlag, itemIsMulti)
        return getIconName(idx)
    }

    fun getIconName(idx: LLInventoryType.EIconName): String {
        return iconNames[idx] ?: "Inv_Unknown"
    }

    fun getIcon(
        assetType: LLAssetType.EType,
        inventoryType: LLInventoryType.EType = LLInventoryType.EType.IT_NONE,
        miscFlag: UInt = 0u,
        itemIsMulti: Boolean = false
    ): LLUIImage? {
        val name = getIconName(assetType, inventoryType, miscFlag, itemIsMulti)
        return TODO("GPU: load UI image named '$name'")
    }

    fun getIcon(idx: LLInventoryType.EIconName): LLUIImage? {
        val name = getIconName(idx)
        return TODO("GPU: load UI image named '$name'")
    }

    private fun resolveIconName(
        assetType: LLAssetType.EType,
        inventoryType: LLInventoryType.EType,
        miscFlag: UInt,
        itemIsMulti: Boolean
    ): LLInventoryType.EIconName {
        if (itemIsMulti) return LLInventoryType.EIconName.ICONNAME_OBJECT_MULTI

        return when (assetType) {
            LLAssetType.EType.AT_TEXTURE ->
                if (inventoryType == LLInventoryType.EType.IT_SNAPSHOT)
                    LLInventoryType.EIconName.ICONNAME_SNAPSHOT
                else
                    LLInventoryType.EIconName.ICONNAME_TEXTURE
            LLAssetType.EType.AT_SOUND -> LLInventoryType.EIconName.ICONNAME_SOUND
            LLAssetType.EType.AT_CALLINGCARD ->
                if (miscFlag != 0u)
                    LLInventoryType.EIconName.ICONNAME_CALLINGCARD_ONLINE
                else
                    LLInventoryType.EIconName.ICONNAME_CALLINGCARD_OFFLINE
            LLAssetType.EType.AT_LANDMARK -> LLInventoryType.EIconName.ICONNAME_LANDMARK
            LLAssetType.EType.AT_SCRIPT,
            LLAssetType.EType.AT_LSL_TEXT,
            LLAssetType.EType.AT_LSL_BYTECODE -> LLInventoryType.EIconName.ICONNAME_SCRIPT
            LLAssetType.EType.AT_CLOTHING,
            LLAssetType.EType.AT_BODYPART -> assignWearableIcon(miscFlag)
            LLAssetType.EType.AT_NOTECARD -> LLInventoryType.EIconName.ICONNAME_NOTECARD
            LLAssetType.EType.AT_ANIMATION -> LLInventoryType.EIconName.ICONNAME_ANIMATION
            LLAssetType.EType.AT_GESTURE -> LLInventoryType.EIconName.ICONNAME_GESTURE
            LLAssetType.EType.AT_LINK -> LLInventoryType.EIconName.ICONNAME_LINKITEM
            LLAssetType.EType.AT_LINK_FOLDER -> LLInventoryType.EIconName.ICONNAME_LINKFOLDER
            LLAssetType.EType.AT_OBJECT -> LLInventoryType.EIconName.ICONNAME_OBJECT
            LLAssetType.EType.AT_MESH -> LLInventoryType.EIconName.ICONNAME_MESH
            LLAssetType.EType.AT_SETTINGS -> assignSettingsIcon(miscFlag)
            LLAssetType.EType.AT_MATERIAL -> LLInventoryType.EIconName.ICONNAME_MATERIAL
            LLAssetType.EType.AT_UNKNOWN -> LLInventoryType.EIconName.ICONNAME_UNKNOWN
            else -> LLInventoryType.EIconName.ICONNAME_OBJECT
        }
    }

    private fun assignWearableIcon(miscFlag: UInt): LLInventoryType.EIconName {
        TODO("APR: use JVM equivalent - map wearable type flags to icon name")
    }

    private fun assignSettingsIcon(miscFlag: UInt): LLInventoryType.EIconName {
        TODO("APR: use JVM equivalent - map settings type flags to icon name")
    }
}

class LLUIImage
