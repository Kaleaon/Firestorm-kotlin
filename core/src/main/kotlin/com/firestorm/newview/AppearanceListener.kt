package com.firestorm.newview

import java.util.UUID

// AppearanceListener handles the "LLAppearance" event-API surface:
// wear/detach items and query outfit lists from scripted or automation callers.
class AppearanceListener {

    init {
        register("wearOutfit",    ::wearOutfit)
        register("wearItems",     ::wearItems)
        register("detachItems",   ::detachItems)
        register("getOutfitsList",::getOutfitsList)
        register("getOutfitItems",::getOutfitItems)
    }

    // Wear an outfit folder identified by either folderId or folderName.
    // When append is true the outfit is added to COF rather than replacing it.
    private fun wearOutfit(data: Map<String, Any?>): Map<String, Any?> {
        val hasFolderId   = data.containsKey("folder_id")
        val hasFolderName = data.containsKey("folder_name")
        if (!hasFolderId && !hasFolderName) {
            return errorResponse("Either [folder_id] or [folder_name] is required")
        }
        val append = (data["append"] as? Boolean) ?: false
        return emptyMap()
    }

    private fun wearItems(data: Map<String, Any?>): Map<String, Any?> {
        val ids = collectUUIDs(data["items_id"])
        val replace = (data["replace"] as? Boolean) ?: false
        return emptyMap()
    }

    private fun detachItems(data: Map<String, Any?>): Map<String, Any?> {
        val ids = collectUUIDs(data["items_id"])
        return emptyMap()
    }

    private fun getOutfitsList(data: Map<String, Any?>): Map<String, Any?> {
        return emptyMap()
    }

    private fun getOutfitItems(data: Map<String, Any?>): Map<String, Any?> {
        val outfitId = data["outfit_id"] as? UUID
            ?: return errorResponse("outfit_id is required")
        return emptyMap()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun collectUUIDs(raw: Any?): List<UUID> {
        @Suppress("UNCHECKED_CAST")
        return when (raw) {
            is List<*> -> raw.filterIsInstance<UUID>()
            is UUID    -> listOf(raw)
            else       -> emptyList()
        }
    }

    private fun errorResponse(message: String): Map<String, Any?> =
        mapOf("error" to message)

    // Stub for the EventAPI registration mechanism.
    private fun register(name: String, handler: (Map<String, Any?>) -> Map<String, Any?>) {
        System.err.println("AppearanceListener: register not yet implemented")
    }
}
