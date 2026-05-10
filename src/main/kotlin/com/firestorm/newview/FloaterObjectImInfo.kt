package com.firestorm.newview

// Source: llfloaterproperties.h / llfloaterproperties.cpp
// (llfloaterobjectiminfo was not present in the repository; the closest matching
// file is llfloaterproperties, which shows inventory item properties including
// permissions, sale info, creator/owner names, and acquired date.)

import java.util.UUID
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Permission bitmask constants mirroring C++ PERM_* values
object Perm {
    const val MODIFY   = 0x00004000u
    const val COPY     = 0x00008000u
    const val TRANSFER = 0x00002000u
    const val MOVE     = 0x00080000u
    const val EXPORT   = 0x00000040u
}

// Inventory item flags for objects
object InventoryItemFlags {
    const val OBJECT_SLAM_PERM               = 0x00000004u
    const val OBJECT_PERM_OVERWRITE_EVERYONE = 0x00000008u
    const val OBJECT_PERM_OVERWRITE_GROUP    = 0x00000010u
}

data class Permissions(
    val maskBase: UInt       = 0u,
    val maskOwner: UInt      = 0u,
    val maskGroup: UInt      = 0u,
    val maskEveryone: UInt   = 0u,
    val maskNextOwner: UInt  = 0u,
    val owner: UUID          = UUID(0, 0),
    val group: UUID          = UUID(0, 0),
    val isGroupOwned: Boolean = false,
    val isOwned: Boolean      = true
)

data class SaleInfo(
    val isForSale: Boolean = false,
    val saleType: Int      = 0,
    val salePrice: Int     = 0
)

data class InventoryItem(
    val id: UUID                   = UUID.randomUUID(),
    val parentId: UUID             = UUID(0, 0),
    val creatorId: UUID            = UUID(0, 0),
    val name: String               = "",
    val description: String        = "",
    val inventoryType: Int         = 0,
    val assetType: Int             = 0,
    val permissions: Permissions   = Permissions(),
    val saleInfo: SaleInfo         = SaleInfo(),
    val creationDate: Long         = 0L,
    val flags: UInt                = 0u,
    val isLink: Boolean            = false,
    val isFinished: Boolean        = true
)

// Watches the inventory model for changes that affect our displayed item.
class PropertiesObserver(private val floater: FloaterProperties) {
    fun onInventoryChanged(mask: UInt) {
        val LABEL    = 0x00000001u
        val INTERNAL = 0x00000004u
        val REMOVE   = 0x00000008u
        if ((mask and (LABEL or INTERNAL or REMOVE)) != 0u) {
            floater.dirty()
        }
    }
}

open class FloaterProperties(key: Any?) {

    var itemId: UUID   = UUID(0, 0)
    var objectId: UUID = UUID(0, 0)
    private var isDirty: Boolean = true
    private val propertiesObserver: PropertiesObserver = PropertiesObserver(this)

    init {
        if (key is Map<*, *>) {
            (key["item_id"] as? UUID)?.let { itemId = it }
            (key["object_id"] as? UUID)?.let { objectId = it }
        }
        TODO("APR: register propertiesObserver with inventory model")
    }

    fun destroy() {
        TODO("APR: unregister propertiesObserver from inventory model; disconnect avatar-name cache signals")
    }

    open fun postBuild(): Boolean {
        TODO("APR: bind LabelItemName/LabelItemDesc commit, BtnCreator/BtnOwner click, permission checkboxes commit, sale info controls commit; call refresh()")
    }

    open fun onOpen(key: Any?) {
        refresh()
    }

    fun setObjectId(id: UUID) {
        objectId = id
    }

    fun dirty() {
        isDirty = true
    }

    companion object {
        fun dirtyAll() {
            TODO("APR: iterate all open FloaterProperties instances and call dirty() on each")
        }

        fun setAssociatedExperience(handle: Any?, experience: Any?) {
            TODO("APR: look up floater by handle; if alive, set experience text from experience data")
        }
    }

    open fun draw() {
        if (isDirty) {
            isDirty = false
            refresh()
        }
        TODO("APR: delegate to super Floater.draw()")
    }

    fun refresh() {
        val item = findItem()
        if (item != null) {
            refreshFromItem(item)
        } else {
            isDirty = true
            // Temporarily disable all editable controls while the inventory refreshes.
            val enableNames = listOf(
                "LabelItemName", "LabelItemDesc", "LabelCreatorName", "BtnCreator",
                "LabelOwnerName", "BtnOwner", "CheckOwnerModify", "CheckOwnerCopy",
                "CheckOwnerTransfer", "CheckShareWithGroup", "CheckEveryoneCopy",
                "CheckNextOwnerModify", "CheckNextOwnerCopy", "CheckNextOwnerTransfer",
                "CheckOwnerExport", "CheckPurchase", "ComboBoxSaleType", "Edit Cost"
            )
            val hideNames = listOf(
                "BaseMaskDebug", "OwnerMaskDebug", "GroupMaskDebug",
                "EveryoneMaskDebug", "NextMaskDebug"
            )
            TODO("APR: for each name in enableNames call childSetEnabled(name, false); for each name in hideNames call childSetVisible(name, false)")
        }
    }

    private fun refreshFromItem(item: InventoryItem) {
        val perm = item.permissions
        val cannotRestrictPermissions = TODO("APR: LLInventoryType.cannotRestrictPermissions(item.inventoryType)") as Boolean
        val isCallingCard   = item.inventoryType == TODO("APR: LLInventoryType.IT_CALLINGCARD") as Int
        val isSettings      = item.inventoryType == TODO("APR: LLInventoryType.IT_SETTINGS") as Int
        val canAgentManipulate = TODO("APR: gAgent.allowOperation(PERM_OWNER, perm, GP_OBJECT_MANIPULATE)") as Boolean
        val canAgentSell    = (TODO("APR: gAgent.allowOperation(PERM_OWNER, perm, GP_OBJECT_SET_SALE)") as Boolean) && !cannotRestrictPermissions
        val isLink          = item.isLink
        val isComplete      = item.isFinished

        // Object in world: check object-level modify permission.
        val isObjModify: Boolean = if (objectId != UUID(0, 0)) {
            TODO("APR: gObjectList.findObject(objectId)?.permOwnerModify() ?: true") as Boolean
        } else true

        // For LSL scripts, show associated experience name.
        if (item.inventoryType == TODO("APR: IT_LSL") as Int) {
            TODO("APR: show LabelItemExperienceTitle; set LabelItemExperience to loading text; fetch associated experience via ExperienceCache")
        }

        // Name & description
        val isModifiable = (TODO("APR: gAgent.allowOperation(PERM_MODIFY, perm, GP_OBJECT_MANIPULATE)") as Boolean) && isObjModify && isComplete
        TODO("APR: enable LabelItemName (modifiable && !isCallingCard); set value to item.name")
        TODO("APR: enable LabelItemDesc (isModifiable); set value to item.description")
        TODO("APR: show/hide IconLocked based on !isModifiable")

        // Creator name (async avatar name lookup)
        if (item.creatorId != UUID(0, 0)) {
            TODO("APR: disable BtnCreator; set LabelCreatorName to waiting text; request avatar name async; enable BtnCreator and set name in callback")
        } else {
            TODO("APR: disable BtnCreator/LabelCreatorTitle/LabelCreatorName; set LabelCreatorName to 'unknown'")
        }

        // Owner name (async, supports group ownership)
        if (perm.isOwned) {
            TODO("APR: disable BtnOwner; set LabelOwnerName to waiting text; if group-owned lookup group name else lookup avatar name; enable BtnOwner in callback")
        } else {
            TODO("APR: disable BtnOwner/LabelOwnerTitle/LabelOwnerName; set LabelOwnerName to 'public'")
        }

        // Acquired date
        if (item.creationDate == 0L) {
            TODO("APR: set LabelAcquiredDate to 'unknown'")
        } else {
            val dateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochSecond(item.creationDate))
            TODO("APR: set LabelAcquiredDate to dateStr")
        }

        // Owner permissions label
        TODO("APR: set OwnerLabel to 'you can' or 'owner can' based on canAgentManipulate")

        // Owner permission checkboxes (read-only)
        TODO("APR: set CheckOwnerModify/Copy/Transfer values from maskOwner bits; all disabled")
        TODO("APR: set CheckOwnerExport value from maskOwner PERM_EXPORT bit; disabled")

        // Debug permission display
        if (TODO("APR: gSavedSettings.getBOOL('DebugPermissions')") as Boolean) {
            val slamPerm          = (item.flags and InventoryItemFlags.OBJECT_SLAM_PERM) != 0u
            val overwriteGroup    = (item.flags and InventoryItemFlags.OBJECT_PERM_OVERWRITE_GROUP) != 0u
            val overwriteEveryone = (item.flags and InventoryItemFlags.OBJECT_PERM_OVERWRITE_EVERYONE) != 0u
            TODO("APR: format and set BaseMaskDebug, OwnerMaskDebug, GroupMaskDebug, EveryoneMaskDebug, NextMaskDebug text; set all visible")
        } else {
            TODO("APR: hide all *MaskDebug children")
        }

        // Group / everyone sharing
        val isGroupCopy   = (perm.maskGroup and Perm.COPY) != 0u
        val isGroupModify = (perm.maskGroup and Perm.MODIFY) != 0u
        val isGroupMove   = (perm.maskGroup and Perm.MOVE) != 0u
        if (isLink || cannotRestrictPermissions) {
            TODO("APR: disable CheckShareWithGroup, CheckEveryoneCopy")
        } else if (isObjModify && canAgentManipulate) {
            TODO("APR: enable CheckShareWithGroup; enable CheckEveryoneCopy only when owner can copy+transfer")
        } else {
            TODO("APR: disable CheckShareWithGroup, CheckEveryoneCopy")
        }
        TODO("APR: enable CheckOwnerExport only when agent is the item creator")

        val fullGroupShare = isGroupCopy && isGroupModify && isGroupMove
        val noGroupShare   = !isGroupCopy && !isGroupModify && !isGroupMove
        when {
            fullGroupShare -> TODO("APR: set CheckShareWithGroup = true, tentative = false")
            noGroupShare   -> TODO("APR: set CheckShareWithGroup = false, tentative = false")
            else           -> TODO("APR: set CheckShareWithGroup tentative = true, value = true")
        }
        TODO("APR: set CheckEveryoneCopy from maskEveryone PERM_COPY bit")

        // Sale info
        val saleInfo     = item.saleInfo
        val isForSale    = saleInfo.isForSale
        val canSell      = isObjModify && canAgentSell &&
            (TODO("APR: gAgent.allowOperation(PERM_TRANSFER, perm, GP_OBJECT_MANIPULATE)") as Boolean)
        if (canSell) {
            TODO("APR: enable CheckPurchase (isComplete), NextOwnerLabel, CheckNextOwnerModify/Copy/Transfer with base-mask guards; enable ComboBoxSaleType/EditCost when isComplete && isForSale")
        } else {
            TODO("APR: disable CheckPurchase, NextOwnerLabel, CheckNextOwnerModify/Copy/Transfer, ComboBoxSaleType, Edit Cost")
        }
        if (isSettings) {
            TODO("APR: hide next-owner copy/transfer and sale-type combo; these are not relevant for environment settings items")
        }
        TODO("APR: set CheckPurchase value from isForSale; set ComboBoxSaleType and Edit Cost values from saleInfo")

        // Next-owner permissions
        TODO("APR: set CheckNextOwnerModify/Copy/Transfer values from maskNextOwner bits")
    }

    private fun findItem(): InventoryItem? {
        TODO("APR: if objectId is non-null look up in object inventory; else look up in agent inventory by itemId")
    }

    // UI callbacks

    protected fun onClickCreator() {
        TODO("APR: open agent profile for creator UUID")
    }

    protected fun onClickOwner() {
        TODO("APR: if group owned open group info; else open agent profile for owner UUID")
    }

    protected fun onCommitName() {
        TODO("APR: read name field value; update item name via inventory model")
    }

    protected fun onCommitDescription() {
        TODO("APR: read description field value; update item description via inventory model")
    }

    protected fun onCommitPermissions() {
        TODO("APR: collect checkbox values; build new permissions mask; call updateInventoryItem or object message to apply")
    }

    protected fun onCommitSaleInfo() {
        updateSaleInfo()
    }

    protected fun onCommitSaleType() {
        updateSaleInfo()
    }

    private fun updateSaleInfo() {
        TODO("APR: read CheckPurchase, ComboBoxSaleType, Edit Cost; build SaleInfo; send update to inventory or object message")
    }

    // Avatar-name cache async callbacks (FS extension to avoid stale names on first open)
    fun onCreatorNameCallback(avId: UUID, avName: String, perm: Permissions) {
        TODO("APR: enable BtnCreator; set LabelCreatorName to avName.userName; enable BtnCreator click action based on perm")
    }

    fun onOwnerNameCallback(avId: UUID, avName: String) {
        TODO("APR: enable BtnOwner; set LabelOwnerName to avName.userName")
    }

    fun onGroupOwnerNameCallback(name: String) {
        TODO("APR: set LabelOwnerName to group name; keep BtnOwner pointing to group-info action")
    }
}

class MultiProperties : FloaterProperties(null) {
    // Hosts multiple FloaterProperties panels in a tabbed multi-floater,
    // analogous to LLMultiFloater in C++.
    init {
        TODO("APR: initialise multi-floater tab container")
    }
}
