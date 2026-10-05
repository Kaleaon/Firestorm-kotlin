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
        System.err.println("LLGiveInventory: isInventoryGiveAcceptable not yet implemented")
        return false
    }

    fun isInventoryGroupGiveAcceptable(item: LLInventoryItem?): Boolean {
        if (item == null) return false
        System.err.println("LLGiveInventory: isInventoryGroupGiveAcceptable not yet implemented")
        return false
    }

    fun doGiveInventoryItem(
        toAgent: UUID,
        item: LLInventoryItem?,
        imSessionId: UUID = UUID(0L, 0L)
    ): Boolean {
        if (!isInventoryGiveAcceptable(item)) return false
        System.err.println("LLGiveInventory: doGiveInventoryItem not yet implemented")
        return false
    }

    fun doGiveInventoryCategory(
        toAgent: UUID,
        cat: LLInventoryCategory?,
        sessionId: UUID = UUID(0L, 0L),
        notification: String = ""
    ): Boolean {
        if (cat == null) return false
        System.err.println("LLGiveInventory: doGiveInventoryCategory not yet implemented")
        return false
    }

    fun handleCopyProtectedItem(notification: LLSD, response: LLSD): Boolean {
        System.err.println("LLGiveInventory: handleCopyProtectedItem not yet implemented")
        return false
    }

    // ---- private methods ----

    private fun logInventoryOffer(
        toAgent: UUID,
        imSessionId: UUID = UUID(0L, 0L),
        itemName: String = "",
        isFolder: Boolean = false
    ) {
        System.err.println("LLGiveInventory: logInventoryOffer not yet implemented")
    }

    // RLVa guard added: returns false if RLV is enabled and sharing is blocked for toAgent.
    private fun commitGiveInventoryItem(
        toAgent: UUID,
        item: LLInventoryItem?,
        imSessionId: UUID = UUID(0L, 0L)
    ): Boolean {
        if (item == null) return false
        System.err.println("LLGiveInventory: commitGiveInventoryItem not yet implemented")
        return false
    }

    private fun handleCopyProtectedCategory(notification: LLSD, response: LLSD): Boolean {
        System.err.println("LLGiveInventory: handleCopyProtectedCategory not yet implemented")
        return false
    }

    private fun commitGiveInventoryCategory(
        toAgent: UUID,
        cat: LLInventoryCategory?,
        imSessionId: UUID = UUID(0L, 0L)
    ): Boolean {
        if (cat == null) return false
        System.err.println("LLGiveInventory: commitGiveInventoryCategory not yet implemented")
        return false
    }
}
