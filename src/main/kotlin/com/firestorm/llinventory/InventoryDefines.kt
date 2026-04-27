package com.firestorm.llinventory

object InventoryDefines {
    const val TASK_INVENTORY_ITEM_KEY: UByte  = 0u
    const val TASK_INVENTORY_ASSET_KEY: UByte = 1u

    const val MAX_INVENTORY_BUFFER_SIZE: Int  = 1024

    // Shared flags (high bits, counting down from bit 30)
    const val II_FLAGS_NONE: UInt                             = 0x00000000u
    const val II_FLAGS_SHARED_SINGLE_REFERENCE: UInt          = 0x40000000u

    // Landmark
    const val II_FLAGS_LANDMARK_VISITED: UInt                 = 0x00000001u

    // Object-specific flags
    const val II_FLAGS_OBJECT_SLAM_PERM: UInt                 = 0x00000100u
    const val II_FLAGS_OBJECT_SLAM_SALE: UInt                 = 0x00001000u
    const val II_FLAGS_OBJECT_PERM_OVERWRITE_BASE: UInt       = 0x00010000u
    const val II_FLAGS_OBJECT_PERM_OVERWRITE_OWNER: UInt      = 0x00020000u
    const val II_FLAGS_OBJECT_PERM_OVERWRITE_GROUP: UInt      = 0x00040000u
    const val II_FLAGS_OBJECT_PERM_OVERWRITE_EVERYONE: UInt   = 0x00080000u
    const val II_FLAGS_OBJECT_PERM_OVERWRITE_NEXT_OWNER: UInt = 0x00100000u
    const val II_FLAGS_OBJECT_HAS_MULTIPLE_ITEMS: UInt        = 0x00200000u

    // Low byte used as sub-type for wearables/settings
    const val II_FLAGS_SUBTYPE_MASK: UInt = 0x000000FFu

    // Combined mask of all flags that must be cleared when asset_id changes
    const val II_FLAGS_PERM_OVERWRITE_MASK: UInt =
        II_FLAGS_OBJECT_SLAM_PERM or
        II_FLAGS_OBJECT_SLAM_SALE or
        II_FLAGS_OBJECT_PERM_OVERWRITE_BASE or
        II_FLAGS_OBJECT_PERM_OVERWRITE_OWNER or
        II_FLAGS_OBJECT_PERM_OVERWRITE_GROUP or
        II_FLAGS_OBJECT_PERM_OVERWRITE_EVERYONE or
        II_FLAGS_OBJECT_PERM_OVERWRITE_NEXT_OWNER
}
