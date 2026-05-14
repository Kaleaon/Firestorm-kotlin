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
        System.err.println("FloaterProperties: register propertiesObserver with inventory model not yet implemented")
    }

    fun destroy() {
        System.err.println("FloaterProperties: unregister propertiesObserver from inventory model; disconnect avatar-name cache signals not yet implemented")
    }

    open fun postBuild(): Boolean {
        System.err.println("FloaterProperties: bind LabelItemName/LabelItemDesc commit, BtnCreator/BtnOwner click, permission checkboxes commit, sale info controls commit; call refresh() not yet implemented")
        return false
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
            System.err.println("FloaterProperties: iterate all open FloaterProperties instances and call dirty() on each not yet implemented")
        }

        fun setAssociatedExperience(handle: Any?, experience: Any?) {
            System.err.println("FloaterProperties: look up floater by handle; if alive, set experience text from experience data not yet implemented")
        }
    }

    open fun draw() {
        if (isDirty) {
            isDirty = false
            refresh()
        }
        System.err.println("FloaterProperties: delegate to super Floater.draw() not yet implemented")
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
            System.err.println("FloaterProperties: for each name in enableNames call childSetEnabled(name, false); for each name in hideNames call childSetVisible(name, false) not yet implemented")
        }
    }

    private fun refreshFromItem(item: InventoryItem) {
        val perm = item.permissions
        val cannotRestrictPermissions = false // APR: LLInventoryType.cannotRestrictPermissions(item.inventoryType)
        val isCallingCard   = item.inventoryType == 0 // APR: LLInventoryType.IT_CALLINGCARD
        val isSettings      = item.inventoryType == 0 // APR: LLInventoryType.IT_SETTINGS
        val canAgentManipulate = false // APR: gAgent.allowOperation(PERM_OWNER, perm, GP_OBJECT_MANIPULATE)
        val canAgentSell    = false && !cannotRestrictPermissions // APR: gAgent.allowOperation(PERM_OWNER, perm, GP_OBJECT_SET_SALE)
        val isLink          = item.isLink
        val isComplete      = item.isFinished

        // Object in world: check object-level modify permission.
        val isObjModify: Boolean = if (objectId != UUID(0, 0)) {
            true // APR: gObjectList.findObject(objectId)?.permOwnerModify() ?: true
        } else true

        // For LSL scripts, show associated experience name.
        if (item.inventoryType == 0) { // APR: IT_LSL
            System.err.println("FloaterProperties: show LabelItemExperienceTitle; set LabelItemExperience to loading text; fetch associated experience via ExperienceCache not yet implemented")
        }

        // Name & description
        val isModifiable = false && isObjModify && isComplete // APR: gAgent.allowOperation(PERM_MODIFY, perm, GP_OBJECT_MANIPULATE)
        System.err.println("FloaterProperties: enable LabelItemName (modifiable && !isCallingCard); set value to item.name not yet implemented")
        System.err.println("FloaterProperties: enable LabelItemDesc (isModifiable); set value to item.description not yet implemented")
        System.err.println("FloaterProperties: show/hide IconLocked based on !isModifiable not yet implemented")

        // Creator name (async avatar name lookup)
        if (item.creatorId != UUID(0, 0)) {
            System.err.println("FloaterProperties: disable BtnCreator; set LabelCreatorName to waiting text; request avatar name async; enable BtnCreator and set name in callback not yet implemented")
        } else {
            System.err.println("FloaterProperties: disable BtnCreator/LabelCreatorTitle/LabelCreatorName; set LabelCreatorName to 'unknown' not yet implemented")
        }

        // Owner name (async, supports group ownership)
        if (perm.isOwned) {
            System.err.println("FloaterProperties: disable BtnOwner; set LabelOwnerName to waiting text; if group-owned lookup group name else lookup avatar name; enable BtnOwner in callback not yet implemented")
        } else {
            System.err.println("FloaterProperties: disable BtnOwner/LabelOwnerTitle/LabelOwnerName; set LabelOwnerName to 'public' not yet implemented")
        }

        // Acquired date
        if (item.creationDate == 0L) {
            System.err.println("FloaterProperties: set LabelAcquiredDate to 'unknown' not yet implemented")
        } else {
            val dateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochSecond(item.creationDate))
            System.err.println("FloaterProperties: set LabelAcquiredDate to dateStr not yet implemented")
        }

        // Owner permissions label
        System.err.println("FloaterProperties: set OwnerLabel to 'you can' or 'owner can' based on canAgentManipulate not yet implemented")

        // Owner permission checkboxes (read-only)
        System.err.println("FloaterProperties: set CheckOwnerModify/Copy/Transfer values from maskOwner bits; all disabled not yet implemented")
        System.err.println("FloaterProperties: set CheckOwnerExport value from maskOwner PERM_EXPORT bit; disabled not yet implemented")

        // Debug permission display
        if (false) { // APR: gSavedSettings.getBOOL('DebugPermissions')
            val slamPerm          = (item.flags and InventoryItemFlags.OBJECT_SLAM_PERM) != 0u
            val overwriteGroup    = (item.flags and InventoryItemFlags.OBJECT_PERM_OVERWRITE_GROUP) != 0u
            val overwriteEveryone = (item.flags and InventoryItemFlags.OBJECT_PERM_OVERWRITE_EVERYONE) != 0u
            System.err.println("FloaterProperties: format and set BaseMaskDebug, OwnerMaskDebug, GroupMaskDebug, EveryoneMaskDebug, NextMaskDebug text; set all visible not yet implemented")
        } else {
            System.err.println("FloaterProperties: hide all *MaskDebug children not yet implemented")
        }

        // Group / everyone sharing
        val isGroupCopy   = (perm.maskGroup and Perm.COPY) != 0u
        val isGroupModify = (perm.maskGroup and Perm.MODIFY) != 0u
        val isGroupMove   = (perm.maskGroup and Perm.MOVE) != 0u
        if (isLink || cannotRestrictPermissions) {
            System.err.println("FloaterProperties: disable CheckShareWithGroup, CheckEveryoneCopy not yet implemented")
        } else if (isObjModify && canAgentManipulate) {
            System.err.println("FloaterProperties: enable CheckShareWithGroup; enable CheckEveryoneCopy only when owner can copy+transfer not yet implemented")
        } else {
            System.err.println("FloaterProperties: disable CheckShareWithGroup, CheckEveryoneCopy not yet implemented")
        }
        System.err.println("FloaterProperties: enable CheckOwnerExport only when agent is the item creator not yet implemented")

        val fullGroupShare = isGroupCopy && isGroupModify && isGroupMove
        val noGroupShare   = !isGroupCopy && !isGroupModify && !isGroupMove
        when {
            fullGroupShare -> System.err.println("FloaterProperties: set CheckShareWithGroup = true, tentative = false not yet implemented")
            noGroupShare   -> System.err.println("FloaterProperties: set CheckShareWithGroup = false, tentative = false not yet implemented")
            else           -> System.err.println("FloaterProperties: set CheckShareWithGroup tentative = true, value = true not yet implemented")
        }
        System.err.println("FloaterProperties: set CheckEveryoneCopy from maskEveryone PERM_COPY bit not yet implemented")

        // Sale info
        val saleInfo     = item.saleInfo
        val isForSale    = saleInfo.isForSale
        val canSell      = isObjModify && canAgentSell &&
            false // APR: gAgent.allowOperation(PERM_TRANSFER, perm, GP_OBJECT_MANIPULATE)
        if (canSell) {
            System.err.println("FloaterProperties: enable CheckPurchase (isComplete), NextOwnerLabel, CheckNextOwnerModify/Copy/Transfer with base-mask guards; enable ComboBoxSaleType/EditCost when isComplete && isForSale not yet implemented")
        } else {
            System.err.println("FloaterProperties: disable CheckPurchase, NextOwnerLabel, CheckNextOwnerModify/Copy/Transfer, ComboBoxSaleType, Edit Cost not yet implemented")
        }
        if (isSettings) {
            System.err.println("FloaterProperties: hide next-owner copy/transfer and sale-type combo; these are not relevant for environment settings items not yet implemented")
        }
        System.err.println("FloaterProperties: set CheckPurchase value from isForSale; set ComboBoxSaleType and Edit Cost values from saleInfo not yet implemented")

        // Next-owner permissions
        System.err.println("FloaterProperties: set CheckNextOwnerModify/Copy/Transfer values from maskNextOwner bits not yet implemented")
    }

    private fun findItem(): InventoryItem? {
        System.err.println("FloaterProperties: if objectId is non-null look up in object inventory; else look up in agent inventory by itemId not yet implemented")
        return null
    }

    // UI callbacks

    protected fun onClickCreator() {
        System.err.println("FloaterProperties: open agent profile for creator UUID not yet implemented")
    }

    protected fun onClickOwner() {
        System.err.println("FloaterProperties: if group owned open group info; else open agent profile for owner UUID not yet implemented")
    }

    protected fun onCommitName() {
        System.err.println("FloaterProperties: read name field value; update item name via inventory model not yet implemented")
    }

    protected fun onCommitDescription() {
        System.err.println("FloaterProperties: read description field value; update item description via inventory model not yet implemented")
    }

    protected fun onCommitPermissions() {
        System.err.println("FloaterProperties: collect checkbox values; build new permissions mask; call updateInventoryItem or object message to apply not yet implemented")
    }

    protected fun onCommitSaleInfo() {
        updateSaleInfo()
    }

    protected fun onCommitSaleType() {
        updateSaleInfo()
    }

    private fun updateSaleInfo() {
        System.err.println("FloaterProperties: read CheckPurchase, ComboBoxSaleType, Edit Cost; build SaleInfo; send update to inventory or object message not yet implemented")
    }

    // Avatar-name cache async callbacks (FS extension to avoid stale names on first open)
    fun onCreatorNameCallback(avId: UUID, avName: String, perm: Permissions) {
        System.err.println("FloaterProperties: enable BtnCreator; set LabelCreatorName to avName.userName; enable BtnCreator click action based on perm not yet implemented")
    }

    fun onOwnerNameCallback(avId: UUID, avName: String) {
        System.err.println("FloaterProperties: enable BtnOwner; set LabelOwnerName to avName.userName not yet implemented")
    }

    fun onGroupOwnerNameCallback(name: String) {
        System.err.println("FloaterProperties: set LabelOwnerName to group name; keep BtnOwner pointing to group-info action not yet implemented")
    }
}

class MultiProperties : FloaterProperties(null) {
    // Hosts multiple FloaterProperties panels in a tabbed multi-floater,
    // analogous to LLMultiFloater in C++.
    init {
        System.err.println("MultiProperties: initialise multi-floater tab container not yet implemented")
    }
}
