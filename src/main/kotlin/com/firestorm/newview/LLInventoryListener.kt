package com.firestorm.newview

import java.util.UUID

enum class EFilterLink {
    INCLUDE_LINKS,
    EXCLUDE_LINKS,
    ONLY_LINKS
}

data class LLInventoryItemInfo(
    val id: UUID,
    val name: String,
    val parentId: UUID,
    val desc: String,
    val invType: String,
    val assetType: String,
    val creationDate: Long,
    val assetId: UUID,
    val isLink: Boolean,
    val linkedId: UUID
)

data class LLInventoryCategoryInfo(
    val id: UUID,
    val name: String,
    val parentId: UUID,
    val type: String
)

data class LLInventoryResponse(
    val items: MutableMap<String, LLInventoryItemInfo> = mutableMapOf(),
    val categories: MutableMap<String, LLInventoryCategoryInfo> = mutableMapOf()
)

class LLFilteredCollector(
    private val name: String,
    private val desc: String,
    private val type: String?,
    private val linkFilter: EFilterLink,
    private val itemLimit: Int
) {
    var itemCount: Int = 0
        private set

    fun exceedsLimit(): Boolean = itemLimit > 0 && itemLimit <= itemCount

    fun checkItem(
        catName: String?,
        catIsLink: Boolean,
        itemName: String?,
        itemDesc: String?,
        itemIsLink: Boolean,
        itemType: String?
    ): Boolean {
        val passed = checkType(catName != null, itemType) &&
                checkNameDesc(catName, itemName, itemDesc) &&
                checkLinks(catName != null, catIsLink, itemIsLink)
        if (passed) itemCount++
        return passed
    }

    private fun checkType(isCat: Boolean, itemType: String?): Boolean {
        if (type == null) return true
        if (isCat && type == "category") return true
        return itemType == type
    }

    private fun checkNameDesc(catName: String?, itemName: String?, itemDesc: String?): Boolean {
        if (catName != null) {
            if (desc.isNotEmpty()) return false
            return name.isEmpty() || catName.contains(name)
        }
        if (itemName != null) {
            val descOk = desc.isEmpty() || (itemDesc?.contains(desc) ?: false)
            val nameOk = name.isEmpty() || itemName.contains(name)
            return descOk && nameOk
        }
        return true
    }

    private fun checkLinks(isCat: Boolean, catIsLink: Boolean, itemIsLink: Boolean): Boolean {
        val isLink = if (isCat) catIsLink else itemIsLink
        if (isLink && linkFilter == EFilterLink.EXCLUDE_LINKS) return false
        if (!isLink && linkFilter == EFilterLink.ONLY_LINKS) return false
        return true
    }
}

class LLInventoryListener {

    fun getItemsInfo(itemIds: List<UUID>): LLInventoryResponse {
        System.err.println("LLInventoryListener: getItemsInfo not yet implemented")
        return LLInventoryResponse()
    }

    fun getFolderTypeNames(): Map<String, String> {
        System.err.println("LLInventoryListener: getFolderTypeNames not yet implemented")
        return emptyMap()
    }

    fun getAssetTypeNames(): Map<String, String> {
        System.err.println("LLInventoryListener: getAssetTypeNames not yet implemented")
        return emptyMap()
    }

    fun getBasicFolderID(ftName: String): UUID {
        System.err.println("LLInventoryListener: getBasicFolderID not yet implemented")
        return UUID(0L, 0L)
    }

    fun getDirectDescendants(folderId: UUID): LLInventoryResponse {
        System.err.println("LLInventoryListener: getDirectDescendants not yet implemented")
        return LLInventoryResponse()
    }

    fun collectDescendantsIf(
        folderId: UUID,
        name: String = "",
        desc: String = "",
        type: String? = null,
        limit: Int = 0,
        filterLinks: EFilterLink = EFilterLink.INCLUDE_LINKS
    ): LLInventoryResponse {
        System.err.println("LLInventoryListener: collectDescendantsIf not yet implemented")
        return LLInventoryResponse()
    }
}
