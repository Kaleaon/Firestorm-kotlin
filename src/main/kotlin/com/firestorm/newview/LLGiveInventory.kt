package com.firestorm.newview

import java.util.UUID

// MAX_ITEMS: packet-size constraint — (sizeof(uuid)+2) * count < MTUBYTES ≈ 1200
// 18 * count < 1200 => count < 66; cut down for headroom.
private const val MAX_ITEMS: Int = 42

// Stub inventory types required by give logic.
interface LLInventoryItemBase {
    fun getUUID(): UUID
    fun getName(): String
    fun getType(): Int
    fun getPermissions(): LLPermissions
}

interface LLInventoryItem : LLInventoryItemBase {
    fun getCreatorUUID(): UUID
}

interface LLInventoryCategory {
    fun getUUID(): UUID
    fun getName(): String
    fun getType(): Int
}

interface LLPermissions {
    fun allowOperationBy(op: Int, agentId: UUID): Boolean
    fun allowCopyBy(agentId: UUID): Boolean
}

// LLSD is represented as a generic map/Any for JVM compatibility.
typealias LLSD = Map<String, Any?>

object LLGiveInventory {

    fun isInventoryGiveAcceptable(item: LLInventoryItem?): Boolean {
        if (item == null) return false
        TODO("APR: use JVM equivalent — check agent avatar valid, PERM_TRANSFER allowed, item not worn")
    }

    fun isInventoryGroupGiveAcceptable(item: LLInventoryItem?): Boolean {
        if (item == null) return false
        TODO("APR: use JVM equivalent — check agent avatar valid, PERM_TRANSFER and PERM_COPY allowed, not worn as attachment")
    }

    fun doGiveInventoryItem(
        toAgent: UUID,
        item: LLInventoryItem?,
        imSessionId: UUID = UUID(0L, 0L)
    ): Boolean {
        if (!isInventoryGiveAcceptable(item)) return false
        TODO("APR: use JVM equivalent — if copyable call commitGiveInventoryItem; else show CannotCopyWarning notification")
    }

    fun doGiveInventoryCategory(
        toAgent: UUID,
        cat: LLInventoryCategory?,
        sessionId: UUID = UUID(0L, 0L),
        notification: String = ""
    ): Boolean {
        if (cat == null) return false
        TODO("APR: use JVM equivalent — collect giveable descendants, check count limits, show notifications, call commitGiveInventoryCategory")
    }

    fun handleCopyProtectedItem(notification: LLSD, response: LLSD): Boolean {
        TODO("APR: use JVM equivalent — read selected option; on Yes call commitGiveInventoryItem per item UUID, delete non-copyable items")
    }

    // ---- private methods ----

    private fun logInventoryOffer(
        toAgent: UUID,
        imSessionId: UUID = UUID(0L, 0L),
        itemName: String = "",
        isFolder: Boolean = false
    ) {
        TODO("APR: use JVM equivalent — compute IM session ID, log offer message to appropriate IM panel or history file")
    }

    // RLVa guard added: returns false if RLV is enabled and sharing is blocked for toAgent.
    private fun commitGiveInventoryItem(
        toAgent: UUID,
        item: LLInventoryItem?,
        imSessionId: UUID = UUID(0L, 0L)
    ): Boolean {
        if (item == null) return false
        TODO("APR: use JVM equivalent — pack IM_INVENTORY_OFFERED message with item type+UUID bucket, send via reliable UDP, spawn HUD beam effect, log offer, add to recent people")
    }

    private fun handleCopyProtectedCategory(notification: LLSD, response: LLSD): Boolean {
        TODO("APR: use JVM equivalent — read selected option; on Yes call commitGiveInventoryCategory, delete uncopyable items")
    }

    private fun commitGiveInventoryCategory(
        toAgent: UUID,
        cat: LLInventoryCategory?,
        imSessionId: UUID = UUID(0L, 0L)
    ): Boolean {
        if (cat == null) return false
        TODO("APR: use JVM equivalent — collect giveable descendants, build type+UUID bucket for each, pack IM_INVENTORY_OFFERED, send reliable UDP, spawn HUD beam, log offer, add to recent people")
    }
}
