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

    fun initCOFID() {
        System.err.println("AppearanceMgr: initCOFID not yet implemented")
    }
    fun getCOF(): UUID = cofId
    fun getCOFVersion(): Int = cofVersion
    fun setOutfitDirty(dirty: Boolean) { isOutfitDirty = dirty }

    fun setOutfitLocked(locked: Boolean) {
        isOutfitLocked = locked
        System.err.println("AppearanceMgr: setOutfitLocked not yet implemented")
    }

    fun updateIsDirty() {
        System.err.println("AppearanceMgr: updateIsDirty not yet implemented")
    }

    fun updateAppearanceFromCOF(
        enforceItemRestrictions: Boolean = true,
        enforceOrdering: Boolean = true,
        postUpdateFunc: AppearanceCallback = {}
    ) {
        System.err.println("AppearanceMgr: updateAppearanceFromCOF not yet implemented")
    }

    fun updateCOF(categoryId: UUID, append: Boolean = false) {
        System.err.println("AppearanceMgr: updateCOF not yet implemented")
    }

    fun updateAgentWearables() {
        System.err.println("AppearanceMgr: updateAgentWearables not yet implemented")
    }
    fun countActiveHoldingPatterns(): Int {
        System.err.println("AppearanceMgr: countActiveHoldingPatterns not yet implemented")
        return 0
    }

    fun requestServerAppearanceUpdate() {
        System.err.println("AppearanceMgr: requestServerAppearanceUpdate not yet implemented")
    }

    fun setAppearanceServiceURL(url: String) { appearanceServiceUrl = url }
    fun getAppearanceServiceURL(): String = appearanceServiceUrl

    fun replaceCurrentOutfit(newOutfit: UUID) {
        System.err.println("AppearanceMgr: replaceCurrentOutfit not yet implemented")
    }

    fun wearInventoryCategory(categoryId: UUID, copy: Boolean, append: Boolean) {
        System.err.println("AppearanceMgr: wearInventoryCategory not yet implemented")
    }

    fun wearInventoryCategoryOnAvatar(categoryId: UUID, append: Boolean) {
        System.err.println("AppearanceMgr: wearInventoryCategoryOnAvatar not yet implemented")
    }

    fun wearCategoryFinal(catId: UUID, copyItems: Boolean, append: Boolean) {
        System.err.println("AppearanceMgr: wearCategoryFinal not yet implemented")
    }

    fun wearOutfitByName(name: String) {
        System.err.println("AppearanceMgr: wearOutfitByName not yet implemented")
    }

    fun wearOutfit(queryMap: Map<String, Any>, append: Boolean = false): Boolean {
        System.err.println("AppearanceMgr: wearOutfit not yet implemented")
        return false
    }

    fun changeOutfit(proceed: Boolean, category: UUID, append: Boolean) {
        if (!proceed) return
        System.err.println("AppearanceMgr: changeOutfit not yet implemented")
    }

    fun takeOffOutfit(catId: UUID) {
        System.err.println("AppearanceMgr: takeOffOutfit not yet implemented")
    }
    fun addCategoryToCurrentOutfit(catId: UUID) {
        System.err.println("AppearanceMgr: addCategoryToCurrentOutfit not yet implemented")
    }
    fun renameOutfit(outfitId: UUID) {
        System.err.println("AppearanceMgr: renameOutfit not yet implemented")
    }
    fun removeOutfitPhoto(outfitId: UUID) {
        System.err.println("AppearanceMgr: removeOutfitPhoto not yet implemented")
    }

    fun makeNewOutfitLinks(newFolderName: String, showPanel: Boolean = true) {
        System.err.println("AppearanceMgr: makeNewOutfitLinks not yet implemented")
    }

    fun wearItemOnAvatar(itemId: UUID, doUpdate: Boolean, replace: Boolean = false) {
        System.err.println("AppearanceMgr: wearItemOnAvatar not yet implemented")
    }

    fun wearItemsOnAvatar(itemIds: List<UUID>, doUpdate: Boolean, replace: Boolean) {
        itemIds.forEach { wearItemOnAvatar(it, doUpdate = false, replace = replace) }
        if (doUpdate) updateAppearanceFromCOF()
    }

    fun removeItemFromAvatar(itemId: UUID, postUpdateFunc: AppearanceCallback = {}, immediateDelete: Boolean = false) {
        System.err.println("AppearanceMgr: removeItemFromAvatar not yet implemented")
    }

    fun removeItemsFromAvatar(itemIds: List<UUID>, postUpdateFunc: AppearanceCallback = {}, immediateDelete: Boolean = false) {
        itemIds.forEach { removeItemFromAvatar(it, postUpdateFunc, immediateDelete) }
    }

    fun removeAllClothesFromAvatar() {
        System.err.println("AppearanceMgr: removeAllClothesFromAvatar not yet implemented")
    }
    fun removeAllAttachmentsFromAvatar() {
        System.err.println("AppearanceMgr: removeAllAttachmentsFromAvatar not yet implemented")
    }

    fun shouldRemoveTempAttachment(itemId: UUID): Boolean {
        System.err.println("AppearanceMgr: shouldRemoveTempAttachment not yet implemented")
        return false
    }

    fun addCOFItemLink(itemId: UUID, description: String = "") {
        System.err.println("AppearanceMgr: addCOFItemLink not yet implemented")
    }

    fun removeCOFItemLinks(itemId: UUID, immediateDelete: Boolean = false) {
        System.err.println("AppearanceMgr: removeCOFItemLinks not yet implemented")
    }

    fun removeCOFLinksOfType(wearableType: Int) {
        System.err.println("AppearanceMgr: removeCOFLinksOfType not yet implemented")
    }

    fun findCOFItemLinks(itemId: UUID): List<UUID> {
        System.err.println("AppearanceMgr: findCOFItemLinks not yet implemented")
        return emptyList()
    }
    fun isLinkedInCOF(itemId: UUID): Boolean {
        System.err.println("AppearanceMgr: isLinkedInCOF not yet implemented")
        return false
    }
    fun getIsInCOF(objId: UUID): Boolean {
        System.err.println("AppearanceMgr: getIsInCOF not yet implemented")
        return false
    }
    fun getIsProtectedCOFItem(objId: UUID): Boolean {
        System.err.println("AppearanceMgr: getIsProtectedCOFItem not yet implemented")
        return false
    }

    fun slamCategoryLinks(srcId: UUID, dstId: UUID, includeFolderLinks: Boolean) {
        System.err.println("AppearanceMgr: slamCategoryLinks not yet implemented")
    }

    fun shallowCopyCategory(srcId: UUID, dstId: UUID) {
        System.err.println("AppearanceMgr: shallowCopyCategory not yet implemented")
    }
    fun shallowCopyCategoryContents(srcId: UUID, dstId: UUID) {
        System.err.println("AppearanceMgr: shallowCopyCategoryContents not yet implemented")
    }

    fun purgeBaseOutfitLink(categoryId: UUID) {
        System.err.println("AppearanceMgr: purgeBaseOutfitLink not yet implemented")
    }
    fun createBaseOutfitLink(categoryId: UUID) {
        System.err.println("AppearanceMgr: createBaseOutfitLink not yet implemented")
    }

    fun getBaseOutfitName(): String? {
        System.err.println("AppearanceMgr: getBaseOutfitName not yet implemented")
        return null
    }
    fun getBaseOutfitUUID(): UUID {
        System.err.println("AppearanceMgr: getBaseOutfitUUID not yet implemented")
        return UUID(0, 0)
    }
    fun wearBaseOutfit() {
        System.err.println("AppearanceMgr: wearBaseOutfit not yet implemented")
    }
    fun updateBaseOutfit(): Boolean {
        System.err.println("AppearanceMgr: updateBaseOutfit not yet implemented")
        return false
    }
    fun updatePanelOutfitName(name: String) {
        System.err.println("AppearanceMgr: updatePanelOutfitName not yet implemented")
    }

    fun registerAttachment(itemId: UUID) {
        System.err.println("AppearanceMgr: registerAttachment not yet implemented")
    }
    fun unregisterAttachment(itemId: UUID) {
        System.err.println("AppearanceMgr: unregisterAttachment not yet implemented")
    }
    fun setAttachmentInvLinkEnable(enabled: Boolean) { attachmentInvLinkEnabled = enabled }

    fun validateClothingOrderingInfo(catId: UUID? = null): Boolean {
        System.err.println("AppearanceMgr: validateClothingOrderingInfo not yet implemented")
        return false
    }
    fun updateClothingOrderingInfo(catId: UUID? = null) {
        System.err.println("AppearanceMgr: updateClothingOrderingInfo not yet implemented")
    }
    fun enforceCOFItemRestrictions() {
        System.err.println("AppearanceMgr: enforceCOFItemRestrictions not yet implemented")
    }

    fun findExcessOrDuplicateItems(catId: UUID, assetType: Int, maxPerType: Int, maxTotal: Int): List<UUID> {
        System.err.println("AppearanceMgr: findExcessOrDuplicateItems not yet implemented")
        return emptyList()
    }

    fun findAllExcessOrDuplicateItems(catId: UUID): List<UUID> {
        System.err.println("AppearanceMgr: findAllExcessOrDuplicateItems not yet implemented")
        return emptyList()
    }

    fun getCanMakeFolderIntoOutfit(folderId: UUID): Boolean {
        System.err.println("AppearanceMgr: getCanMakeFolderIntoOutfit not yet implemented")
        return false
    }
    fun getCanRemoveOutfit(outfitCatId: UUID): Boolean {
        System.err.println("AppearanceMgr: getCanRemoveOutfit not yet implemented")
        return false
    }
    fun getCanReplaceCOF(outfitCatId: UUID): Boolean {
        System.err.println("AppearanceMgr: getCanReplaceCOF not yet implemented")
        return false
    }
    fun canAddWearables(itemIds: List<UUID>, warnOnTypeMismatch: Boolean = true): Boolean {
        System.err.println("AppearanceMgr: canAddWearables not yet implemented")
        return false
    }
    fun moveWearable(item: Any, closerToBody: Boolean): Boolean {
        System.err.println("AppearanceMgr: moveWearable not yet implemented")
        return false
    }

    fun onFirstFullyVisible() {
        System.err.println("AppearanceMgr: onFirstFullyVisible not yet implemented")
    }
    fun copyLibraryGestures() {
        System.err.println("AppearanceMgr: copyLibraryGestures not yet implemented")
    }
    fun cleanup() {
        System.err.println("AppearanceMgr: cleanup not yet implemented")
    }
    fun dumpCOF(): String {
        System.err.println("AppearanceMgr: dumpCOF not yet implemented")
        return ""
    }

    fun syncCofVersionAndRefresh() {
        System.err.println("AppearanceMgr: syncCofVersionAndRefresh not yet implemented")
    }
    fun getActiveCopyOperations(): Int {
        System.err.println("AppearanceMgr: getActiveCopyOperations not yet implemented")
        return 0
    }

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

        fun getCanRemoveFromCOF(outfitCatId: UUID): Boolean {
            System.err.println("AppearanceMgr: getCanRemoveFromCOF not yet implemented")
            return false
        }
        fun getCanAddToCOF(outfitCatId: UUID): Boolean {
            System.err.println("AppearanceMgr: getCanAddToCOF not yet implemented")
            return false
        }

        fun sortItemsByActualDescription(items: MutableList<Any>) {
            System.err.println("AppearanceMgr: sortItemsByActualDescription not yet implemented")
        }

        fun divvyWearablesByType(items: List<Any>, itemsByType: MutableList<MutableList<Any>>) {
            System.err.println("AppearanceMgr: divvyWearablesByType not yet implemented")
        }
    }
}

fun findDescendentCategoryIDByName(parentId: UUID, name: String): UUID {
    System.err.println("AppearanceMgr: findDescendentCategoryIDByName not yet implemented")
    return UUID(0, 0)
}

fun callAfterCOFFetch(cb: AppearanceCallback) {
    System.err.println("AppearanceMgr: callAfterCOFFetch not yet implemented")
}

fun callAfterCategoryFetch(catId: UUID, cb: AppearanceCallback) {
    System.err.println("AppearanceMgr: callAfterCategoryFetch not yet implemented")
}

fun callAfterCategoryLinksFetch(catId: UUID, cb: AppearanceCallback) {
    System.err.println("AppearanceMgr: callAfterCategoryLinksFetch not yet implemented")
}

fun wearMultiple(ids: List<UUID>, replace: Boolean) {
    System.err.println("AppearanceMgr: wearMultiple not yet implemented")
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
            System.err.println("UpdateAppearanceAndEditWearableOnDestroy: edit_wearable_and_customize_avatar not yet implemented")
        }
    }
}

class RequestServerAppearanceUpdateOnDestroy {
    fun fire(invItem: UUID) {}

    fun destroy() {
        AppearanceMgr.requestServerAppearanceUpdate()
    }
}
