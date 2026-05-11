package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llinventory.AssetTypeId
import com.firestorm.llinventory.InventoryItem
import com.firestorm.llinventory.Permissions
import com.firestorm.llinventory.SaleInfo

class ViewerInventoryItem(
    uuid: LLUUID = LLUUID.NULL,
    parentId: LLUUID = LLUUID.NULL,
    thumbnailId: LLUUID = LLUUID.NULL,
    type: AssetTypeId = -1,
    name: String = "",
    creationDate: Long = 0L,
    isFavorite: Boolean = false,
    permissions: Permissions = Permissions.DEFAULT,
    assetId: LLUUID = LLUUID.NULL,
    inventoryType: Int = -1,
    flags: UInt = 0u,
    saleInfo: SaleInfo = SaleInfo.DEFAULT,
    description: String = ""
) : InventoryItem(
    uuid, parentId, thumbnailId, type, name, creationDate, isFavorite,
    permissions, assetId, inventoryType, flags, saleInfo, description
) {
    var isComplete: Boolean = false
    var isCopy: Boolean = false

    constructor(other: InventoryItem) : this(
        uuid          = other.uuid,
        parentId      = other.parentId,
        thumbnailId   = other.thumbnailId,
        type          = other.type,
        name          = other.name,
        creationDate  = other.creationDate,
        isFavorite    = other.isFavorite,
        permissions   = other.permissions,
        assetId       = other.assetId,
        inventoryType = other.inventoryType,
        flags         = other.flags,
        saleInfo      = other.saleInfo,
        description   = other.description
    )

    fun fetchFromServer() {}

    fun updateServer(isNew: Boolean) {}

    fun copyItem(): ViewerInventoryItem {
        val copy = ViewerInventoryItem(this as InventoryItem)
        copy.isComplete = isComplete
        copy.isCopy     = true
        return copy
    }
}
