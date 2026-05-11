package com.firestorm.newview

import java.util.UUID

typealias AttachmentsChangedCallback = (itemId: UUID) -> Unit
typealias AppearanceCallback = () -> Unit

object AppearanceMgr {

    var cofId: UUID = UUID(0, 0)
        private set

    var cofVersion: Int = 0
        private set

    var isOutfitDirty: Boolean = false
    var isInUpdateAppearanceFromCOF: Boolean = false
        private set

    var isOutfitLocked: Boolean = false
        private set

    var outfitImageId: UUID = UUID(0, 0)
    var attachmentInvLinkEnabled: Boolean = true

    private var appearanceServiceUrl: String = ""

    fun initCOFID() { TODO("initCOFID: locate COF in inventory tree") }
    fun getCOF(): UUID = cofId
    fun getCOFVersion(): Int = cofVersion
    fun setOutfitDirty(dirty: Boolean) { isOutfitDirty = dirty }

    fun setOutfitLocked(locked: Boolean) {
        isOutfitLocked = locked
        TODO("setOutfitLocked: start/stop OutfitUnLockTimer, notify observers")
    }

    fun updateIsDirty() { TODO("updateIsDirty: compare COF contents to base outfit link") }

    fun updateAppearanceFromCOF(
        enforceItemRestrictions: Boolean = true,
        enforceOrdering: Boolean = true,
        postUpdateFunc: AppearanceCallback = {}
    ) {
        TODO("updateAppearanceFromCOF")
    }

    fun updateCOF(categoryId: UUID, append: Boolean = false) {
        TODO("updateCOF($categoryId, append=$append)")
    }

    fun updateAgentWearables() { TODO("updateAgentWearables: sync wearable holding pattern to avatar") }
    fun countActiveHoldingPatterns(): Int = TODO("countActiveHoldingPatterns")

    fun requestServerAppearanceUpdate() { TODO("requestServerAppearanceUpdate: POST to appearance bake service") }

    fun setAppearanceServiceURL(url: String) { appearanceServiceUrl = url }
    fun getAppearanceServiceURL(): String = appearanceServiceUrl

    fun replaceCurrentOutfit(newOutfit: UUID) { TODO("replaceCurrentOutfit($newOutfit)") }

    fun wearInventoryCategory(categoryId: UUID, copy: Boolean, append: Boolean) {
        TODO("wearInventoryCategory($categoryId, copy=$copy, append=$append)")
    }

    fun wearInventoryCategoryOnAvatar(categoryId: UUID, append: Boolean) {
        TODO("wearInventoryCategoryOnAvatar($categoryId, append=$append)")
    }

    fun wearCategoryFinal(catId: UUID, copyItems: Boolean, append: Boolean) {
        TODO("wearCategoryFinal($catId, copyItems=$copyItems, append=$append)")
    }

    fun wearOutfitByName(name: String) { TODO("wearOutfitByName($name)") }

    fun wearOutfit(queryMap: Map<String, Any>, append: Boolean = false): Boolean {
        TODO("wearOutfit(append=$append)")
    }

    fun changeOutfit(proceed: Boolean, category: UUID, append: Boolean) {
        if (!proceed) return
        TODO("changeOutfit: wearInventoryCategory or link-based approach")
    }

    fun takeOffOutfit(catId: UUID)              { TODO("takeOffOutfit($catId)") }
    fun addCategoryToCurrentOutfit(catId: UUID) { TODO("addCategoryToCurrentOutfit($catId)") }
    fun renameOutfit(outfitId: UUID)            { TODO("renameOutfit($outfitId)") }
    fun removeOutfitPhoto(outfitId: UUID)       { TODO("removeOutfitPhoto($outfitId)") }

    fun makeNewOutfitLinks(newFolderName: String, showPanel: Boolean = true) {
        TODO("makeNewOutfitLinks($newFolderName, showPanel=$showPanel)")
    }

    fun wearItemOnAvatar(itemId: UUID, doUpdate: Boolean, replace: Boolean = false) {
        TODO("wearItemOnAvatar($itemId, replace=$replace)")
    }

    fun wearItemsOnAvatar(itemIds: List<UUID>, doUpdate: Boolean, replace: Boolean) {
        itemIds.forEach { wearItemOnAvatar(it, doUpdate = false, replace = replace) }
        if (doUpdate) updateAppearanceFromCOF()
    }

    fun removeItemFromAvatar(itemId: UUID, postUpdateFunc: AppearanceCallback = {}, immediateDelete: Boolean = false) {
        TODO("removeItemFromAvatar($itemId)")
    }

    fun removeItemsFromAvatar(itemIds: List<UUID>, postUpdateFunc: AppearanceCallback = {}, immediateDelete: Boolean = false) {
        itemIds.forEach { removeItemFromAvatar(it, postUpdateFunc, immediateDelete) }
    }

    fun removeAllClothesFromAvatar()      { TODO("removeAllClothesFromAvatar: removeCOFLinksOfType for every clothing type") }
    fun removeAllAttachmentsFromAvatar()  { TODO("removeAllAttachmentsFromAvatar") }

    fun shouldRemoveTempAttachment(itemId: UUID): Boolean = TODO("shouldRemoveTempAttachment($itemId)")

    fun addCOFItemLink(itemId: UUID, description: String = "") { TODO("addCOFItemLink($itemId)") }

    fun removeCOFItemLinks(itemId: UUID, immediateDelete: Boolean = false) {
        TODO("removeCOFItemLinks($itemId, immediateDelete=$immediateDelete)")
    }

    fun removeCOFLinksOfType(wearableType: Int) { TODO("removeCOFLinksOfType($wearableType)") }

    fun findCOFItemLinks(itemId: UUID): List<UUID> = TODO("findCOFItemLinks($itemId)")
    fun isLinkedInCOF(itemId: UUID): Boolean       = TODO("isLinkedInCOF($itemId)")
    fun getIsInCOF(objId: UUID): Boolean           = TODO("getIsInCOF($objId)")
    fun getIsProtectedCOFItem(objId: UUID): Boolean = TODO("getIsProtectedCOFItem($objId)")

    fun slamCategoryLinks(srcId: UUID, dstId: UUID, includeFolderLinks: Boolean) {
        TODO("slamCategoryLinks($srcId -> $dstId)")
    }

    fun shallowCopyCategory(srcId: UUID, dstId: UUID) { TODO("shallowCopyCategory($srcId -> $dstId)") }
    fun shallowCopyCategoryContents(srcId: UUID, dstId: UUID) { TODO("shallowCopyCategoryContents($srcId -> $dstId)") }

    fun purgeBaseOutfitLink(categoryId: UUID)                  { TODO("purgeBaseOutfitLink($categoryId)") }
    fun createBaseOutfitLink(categoryId: UUID)                  { TODO("createBaseOutfitLink($categoryId)") }

    fun getBaseOutfitName(): String? = TODO("getBaseOutfitName")
    fun getBaseOutfitUUID(): UUID    = TODO("getBaseOutfitUUID")
    fun wearBaseOutfit()             { TODO("wearBaseOutfit") }
    fun updateBaseOutfit(): Boolean  = TODO("updateBaseOutfit")
    fun updatePanelOutfitName(name: String) { TODO("updatePanelOutfitName") }

    fun registerAttachment(itemId: UUID)   { TODO("registerAttachment($itemId)") }
    fun unregisterAttachment(itemId: UUID) { TODO("unregisterAttachment($itemId)") }
    fun setAttachmentInvLinkEnable(enabled: Boolean) { attachmentInvLinkEnabled = enabled }

    fun validateClothingOrderingInfo(catId: UUID? = null): Boolean = TODO("validateClothingOrderingInfo")
    fun updateClothingOrderingInfo(catId: UUID? = null)            { TODO("updateClothingOrderingInfo") }
    fun enforceCOFItemRestrictions()                               { TODO("enforceCOFItemRestrictions") }

    fun findExcessOrDuplicateItems(catId: UUID, assetType: Int, maxPerType: Int, maxTotal: Int): List<UUID> =
        TODO("findExcessOrDuplicateItems($catId)")

    fun findAllExcessOrDuplicateItems(catId: UUID): List<UUID> = TODO("findAllExcessOrDuplicateItems($catId)")

    fun getCanMakeFolderIntoOutfit(folderId: UUID): Boolean  = TODO("getCanMakeFolderIntoOutfit")
    fun getCanRemoveOutfit(outfitCatId: UUID): Boolean       = TODO("getCanRemoveOutfit")
    fun getCanReplaceCOF(outfitCatId: UUID): Boolean         = TODO("getCanReplaceCOF")
    fun canAddWearables(itemIds: List<UUID>, warnOnTypeMismatch: Boolean = true): Boolean = TODO("canAddWearables")
    fun moveWearable(item: Any, closerToBody: Boolean): Boolean = TODO("moveWearable")

    fun onFirstFullyVisible() { TODO("onFirstFullyVisible: trigger initial appearance bake if needed") }
    fun copyLibraryGestures() { TODO("copyLibraryGestures") }
    fun cleanup()             { TODO("cleanup: release COF resources") }
    fun dumpCOF(): String     = TODO("dumpCOF: return LLSD summary of COF contents")

    fun syncCofVersionAndRefresh() { TODO("syncCofVersionAndRefresh") }
    fun getActiveCopyOperations(): Int = TODO("getActiveCopyOperations")

    private val attachmentsChangedListeners: MutableList<AttachmentsChangedCallback> = mutableListOf()
    private val appearanceChangedListeners:  MutableList<AppearanceCallback>          = mutableListOf()

    fun setAttachmentsChangedCallback(cb: AttachmentsChangedCallback) {
        attachmentsChangedListeners += cb
    }

    fun addAppearanceChangedCallback(cb: AppearanceCallback) {
        appearanceChangedListeners += cb
    }

    private fun notifyAttachmentsChanged(itemId: UUID) {
        attachmentsChangedListeners.forEach { it(itemId) }
    }

    private fun notifyAppearanceChanged() {
        appearanceChangedListeners.forEach { it() }
    }

    companion object {
        const val EXPECTED_TEXTURE_NAME      = "OutfitPreview"
        private const val BAKE_RETRY_MAX_COUNT = 5
        private const val BAKE_RETRY_TIMEOUT   = 2.0f

        fun getCanRemoveFromCOF(outfitCatId: UUID): Boolean = TODO("getCanRemoveFromCOF($outfitCatId)")
        fun getCanAddToCOF(outfitCatId: UUID): Boolean      = TODO("getCanAddToCOF($outfitCatId)")

        fun sortItemsByActualDescription(items: MutableList<Any>) {
            TODO("sortItemsByActualDescription: sort by link description field")
        }

        fun divvyWearablesByType(items: List<Any>, itemsByType: MutableList<MutableList<Any>>) {
            TODO("divvyWearablesByType: partition items list by wearable asset type")
        }
    }
}

fun findDescendentCategoryIDByName(parentId: UUID, name: String): UUID {
    TODO("findDescendentCategoryIDByName: search inventory tree under $parentId for category named '$name'")
}

fun callAfterCOFFetch(cb: AppearanceCallback) {
    TODO("callAfterCOFFetch: fetch COF contents then invoke cb")
}

fun callAfterCategoryFetch(catId: UUID, cb: AppearanceCallback) {
    TODO("callAfterCategoryFetch($catId): fetch category, then invoke cb")
}

fun callAfterCategoryLinksFetch(catId: UUID, cb: AppearanceCallback) {
    TODO("callAfterCategoryLinksFetch($catId): fetch category links, then invoke cb")
}

fun wearMultiple(ids: List<UUID>, replace: Boolean) {
    TODO("wearMultiple: wear all items in ids list, replace=$replace")
}

class UpdateAppearanceOnDestroy(
    private val enforceItemRestrictions: Boolean = true,
    private val enforceOrdering: Boolean = true,
    private val postUpdateFunc: AppearanceCallback = {}
) {
    private var fireCount: UInt = 0u

    fun fire(invItem: UUID) { fireCount++ }

    fun destroy() {
        AppearanceMgr.updateAppearanceFromCOF(enforceItemRestrictions, enforceOrdering, postUpdateFunc)
    }
}

class UpdateAppearanceAndEditWearableOnDestroy(private val itemId: UUID) {
    fun fire(invItem: UUID) {}

    fun destroy() {
        AppearanceMgr.updateAppearanceFromCOF(true, true) {
            TODO("edit_wearable_and_customize_avatar($itemId)")
        }
    }
}

class RequestServerAppearanceUpdateOnDestroy {
    fun fire(invItem: UUID) {}

    fun destroy() {
        AppearanceMgr.requestServerAppearanceUpdate()
    }
}
