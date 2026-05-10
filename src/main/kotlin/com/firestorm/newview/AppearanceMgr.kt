// Converted from llappearancemgr.h / llappearancemgr.cpp
// Original: Copyright (C) 2010, Linden Research, Inc. (LGPL 2.1)
package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llinventory.*
import com.firestorm.llappearance.*

// ---------------------------------------------------------------------------
// Observer / callback types
// ---------------------------------------------------------------------------

/** Invoked when the set of worn attachments changes. */
typealias AttachmentsChangedCallback = (itemId: LLUUID) -> Unit

/** Invoked on arbitrary appearance events (COF flush, outfit apply, etc.). */
typealias AppearanceCallback = () -> Unit

// ---------------------------------------------------------------------------
// AppearanceMgr singleton
// Manages the Current Outfit Folder (COF) and drives avatar appearance updates.
// ---------------------------------------------------------------------------

object AppearanceMgr {

    // --- COF identity --------------------------------------------------------

    /** UUID of the Current Outfit Folder in the agent's inventory. */
    var cofId: LLUUID = LLUUID.NULL
        private set

    /** Whether the current region supports server-side baking. */
    var serverBakeRegion: Boolean = false

    /** Version counter incremented whenever the COF changes. */
    var cofVersion: Int = 0
        private set

    fun initCOFID() { TODO("initCOFID: locate COF in inventory tree") }

    fun getCOF(): LLUUID = cofId

    fun getCOFVersion(): Int = cofVersion

    // --- COF state -----------------------------------------------------------

    var isOutfitDirty: Boolean = false

    var isOutfitLocked: Boolean = false
        private set

    var isInUpdateAppearanceFromCOF: Boolean = false
        private set

    fun setOutfitDirty(dirty: Boolean) { isOutfitDirty = dirty }

    fun setOutfitLocked(locked: Boolean) {
        isOutfitLocked = locked
        TODO("setOutfitLocked: notify outfit-lock observers")
    }

    /** Re-examine COF vs. base outfit link to determine dirty state. */
    fun updateIsDirty() { TODO("updateIsDirty") }

    // --- Appearance update ---------------------------------------------------

    /**
     * Drive a full appearance rebuild from current COF contents.
     * Corresponds to LLAppearanceMgr::updateAppearanceFromCOF().
     */
    fun updateAppearanceFromCOF(
        enforceItemRestrictions: Boolean = true,
        enforceOrdering: Boolean = true,
        postUpdateFunc: AppearanceCallback = {}
    ) {
        TODO("updateAppearanceFromCOF")
    }

    fun updateAgentWearables() { TODO("updateAgentWearables: sync from holding pattern") }

    fun requestServerAppearanceUpdate() { TODO("requestServerAppearanceUpdate: POST to bake service") }

    // --- Outfit operations ---------------------------------------------------

    /**
     * Replace the current outfit with the outfit category identified by [newOutfit].
     * Corresponds to LLAppearanceMgr::replaceCurrentOutfit().
     */
    fun replaceCurrentOutfit(newOutfit: LLUUID) { TODO("replaceCurrentOutfit($newOutfit)") }

    /** Wear all items in [categoryId], optionally appending to current outfit. */
    fun wearInventoryCategory(categoryId: LLUUID, copy: Boolean, append: Boolean) {
        TODO("wearInventoryCategory($categoryId, copy=$copy, append=$append)")
    }

    fun wearOutfitByName(name: String) { TODO("wearOutfitByName($name)") }

    fun changeOutfit(proceed: Boolean, category: LLUUID, append: Boolean) {
        if (!proceed) return
        TODO("changeOutfit")
    }

    fun takeOffOutfit(catId: LLUUID)              { TODO("takeOffOutfit($catId)") }
    fun addCategoryToCurrentOutfit(catId: LLUUID) { TODO("addCategoryToCurrentOutfit($catId)") }
    fun renameOutfit(outfitId: LLUUID)            { TODO("renameOutfit($outfitId)") }
    fun removeOutfitPhoto(outfitId: LLUUID)       { TODO("removeOutfitPhoto($outfitId)") }

    // --- Wearing / unwearing individual items --------------------------------

    /**
     * Wear a single inventory item on the avatar.
     * [replace] – if true, replaces any existing item of the same wearable type.
     */
    fun wearItemOnAvatar(itemId: LLUUID, doUpdate: Boolean, replace: Boolean = false) {
        TODO("wearItemOnAvatar($itemId, replace=$replace)")
    }

    fun wearItemsOnAvatar(itemIds: List<LLUUID>, doUpdate: Boolean, replace: Boolean) {
        itemIds.forEach { wearItemOnAvatar(it, doUpdate = false, replace = replace) }
        if (doUpdate) updateAppearanceFromCOF()
    }

    fun removeItemFromAvatar(itemId: LLUUID) { TODO("removeItemFromAvatar($itemId)") }

    fun removeItemsFromAvatar(itemIds: List<LLUUID>) {
        itemIds.forEach { removeItemFromAvatar(it) }
    }

    fun removeAllClothesFromAvatar() {
        TODO("removeAllClothesFromAvatar: removeCOFLinksOfType for all clothing types")
    }

    fun removeAllAttachmentsFromAvatar() { TODO("removeAllAttachmentsFromAvatar") }

    // --- COF link management -------------------------------------------------

    fun addCOFItemLink(itemId: LLUUID, description: String = "") {
        TODO("addCOFItemLink($itemId)")
    }

    fun removeCOFItemLinks(itemId: LLUUID, immediateDelete: Boolean = false) {
        TODO("removeCOFItemLinks($itemId)")
    }

    fun isLinkedInCOF(itemId: LLUUID): Boolean = TODO("isLinkedInCOF($itemId)")

    fun getIsInCOF(objId: LLUUID): Boolean = TODO("getIsInCOF($objId)")

    fun getIsProtectedCOFItem(objId: LLUUID): Boolean = TODO("getIsProtectedCOFItem($objId)")

    // --- Ensemble / outfit links ---------------------------------------------

    /**
     * Add a link to an ensemble (outfit) folder inside the COF.
     * Corresponds to addCategoryToCurrentOutfit / createBaseOutfitLink.
     */
    fun addEnsembleLink(categoryId: LLUUID) { TODO("addEnsembleLink($categoryId)") }

    // --- Base outfit ---------------------------------------------------------

    fun getBaseOutfitName(): String?  = TODO("getBaseOutfitName")
    fun getBaseOutfitUUID(): LLUUID   = TODO("getBaseOutfitUUID")
    fun wearBaseOutfit()              { TODO("wearBaseOutfit") }
    fun updateBaseOutfit(): Boolean   = TODO("updateBaseOutfit")
    fun updatePanelOutfitName(name: String) { TODO("updatePanelOutfitName") }

    // --- Outfit image --------------------------------------------------------

    var outfitImageId: LLUUID = LLUUID.NULL

    // --- Attachment tracking -------------------------------------------------

    var attachmentInvLinkEnabled: Boolean = true

    fun registerAttachment(itemId: LLUUID)   { TODO("registerAttachment($itemId)") }
    fun unregisterAttachment(itemId: LLUUID) { TODO("unregisterAttachment($itemId)") }
    fun setAttachmentInvLinkEnable(enabled: Boolean) { attachmentInvLinkEnabled = enabled }

    // --- Wearable ordering / validation --------------------------------------

    fun validateClothingOrderingInfo(catId: LLUUID = LLUUID.NULL): Boolean =
        TODO("validateClothingOrderingInfo")

    fun updateClothingOrderingInfo(catId: LLUUID = LLUUID.NULL) {
        TODO("updateClothingOrderingInfo")
    }

    fun enforceItemRestrictions() { TODO("enforceItemRestrictions") }

    // --- Queries (static-equivalent helpers) ---------------------------------

    fun getCanMakeFolderIntoOutfit(folderId: LLUUID): Boolean = TODO("getCanMakeFolderIntoOutfit")
    fun getCanRemoveOutfit(outfitCatId: LLUUID): Boolean      = TODO("getCanRemoveOutfit")
    fun getCanReplaceCOF(outfitCatId: LLUUID): Boolean        = TODO("getCanReplaceCOF")
    fun canAddWearables(itemIds: List<LLUUID>): Boolean        = TODO("canAddWearables")

    /**
     * True when the avatar is wearing so few items it is considered nearly naked
     * (used for RLVa and UI warnings).
     */
    fun isNearlyNaked(): Boolean = TODO("isNearlyNaked")

    // --- Observer pattern ----------------------------------------------------

    private val attachmentsChangedListeners: MutableList<AttachmentsChangedCallback> = mutableListOf()
    private val appearanceChangedListeners:  MutableList<AppearanceCallback>          = mutableListOf()

    fun addAttachmentsChangedCallback(cb: AttachmentsChangedCallback) {
        attachmentsChangedListeners += cb
    }

    fun addAppearanceChangedCallback(cb: AppearanceCallback) {
        appearanceChangedListeners += cb
    }

    private fun notifyAttachmentsChanged(itemId: LLUUID) {
        attachmentsChangedListeners.forEach { it(itemId) }
    }

    private fun notifyAppearanceChanged() {
        appearanceChangedListeners.forEach { it() }
    }

    // --- Lifecycle -----------------------------------------------------------

    fun onFirstFullyVisible() { TODO("onFirstFullyVisible: trigger initial bake if needed") }
    fun copyLibraryGestures() { TODO("copyLibraryGestures") }
    fun cleanup()             { TODO("cleanup: release COF resources") }

    // --- Debug helpers -------------------------------------------------------

    fun dumpCOF(): String = TODO("dumpCOF: return LLSD summary string")

    companion object {
        const val EXPECTED_TEXTURE_NAME = "OutfitPreview"
        private const val BAKE_RETRY_MAX_COUNT = 5
        private const val BAKE_RETRY_TIMEOUT   = 2.0f // seconds
    }
}
