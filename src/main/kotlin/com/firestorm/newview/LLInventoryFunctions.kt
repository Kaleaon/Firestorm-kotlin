package com.firestorm.newview

import java.util.UUID
import java.util.ArrayDeque

const val COMPUTE_STOCK_INFINITE: Int = -1
const val COMPUTE_STOCK_NOT_EVALUATED: Int = -2

const val ROOT_FIRESTORM_FOLDER: String = "#Firestorm"

fun getIsParentToWornItem(id: UUID): Boolean {
    TODO("APR: use JVM equivalent - check whether any COF-linked item has this folder as an ancestor")
}

fun getIsItemWorn(id: UUID): Boolean {
    TODO("APR: use JVM equivalent - check worn/attached/active status by UUID")
}

fun getIsItemWorn(item: LLViewerInventoryItem?): Boolean {
    if (item == null) return false
    return getIsItemWorn(item.uuid)
}

fun getCanItemBeWorn(id: UUID): Boolean {
    TODO("APR: use JVM equivalent - check item type and COF membership")
}

fun getIsItemRemovable(model: LLInventoryModel?, id: UUID, checkWorn: Boolean): Boolean {
    if (model == null) return false
    if (!model.isObjectDescendentOf(id, model.rootFolderID)) return false
    val obj = model.getItem(id)
    if (obj != null && obj.isLinkType) return true
    if (checkWorn && getIsItemWorn(id)) return false
    return true
}

fun getIsItemEditable(invItemId: UUID): Boolean {
    TODO("APR: use JVM equivalent - check wearable modifiable / attachment edit permission")
}

fun handleItemEdit(invItemId: UUID) {
    TODO("APR: use JVM equivalent - open appropriate editor for item type")
}

fun getIsCategoryRemovable(model: LLInventoryModel?, id: UUID): Boolean {
    if (model == null) return false
    if (!model.isObjectDescendentOf(id, model.rootFolderID)) return false
    val category = model.getCategory(id) ?: return false
    if (LLFolderType.lookupIsProtectedType(category.preferredType)) return false
    return true
}

fun getIsCategoryAndChildrenRemovable(model: LLInventoryModel, folderId: UUID, checkWorn: Boolean): Boolean {
    if (!getIsCategoryRemovable(model, folderId)) return false
    TODO("APR: use JVM equivalent - traverse descendants checking remove permissions")
}

fun getIsCategoryRenameable(model: LLInventoryModel?, id: UUID): Boolean {
    if (model == null) return false
    val cat = model.getCategory(id) ?: return false
    return !LLFolderType.lookupIsProtectedType(cat.preferredType)
}

fun showItemProfile(itemUuid: UUID) {
    TODO("APR: use JVM equivalent - open item properties floater")
}

fun showTaskItemProfile(itemUuid: UUID, objectId: UUID) {
    TODO("APR: use JVM equivalent - open task item properties floater")
}

fun showItemOriginal(itemUuid: UUID) {
    TODO("APR: use JVM equivalent - navigate inventory panel to item location")
}

fun resetInventoryFilter() {
    TODO("APR: use JVM equivalent - clear inventory search/filter state")
}

fun deleteFromOutfit(ids: List<UUID>) {
    TODO("APR: use JVM equivalent - remove link-type items from inventory")
}

fun updateMarketplaceCategory(catId: UUID, performConsistencyEnforcement: Boolean = true, skipClearListing: Boolean = false) {
    TODO("APR: use JVM equivalent - refresh marketplace listing status for category hierarchy")
}

fun updateAllMarketplaceCount() {
    TODO("APR: use JVM equivalent - refresh all marketplace stock counts")
}

fun renameCategory(model: LLInventoryModel, catId: UUID, newName: String, cb: ((UUID) -> Unit)? = null) {
    if (!getIsCategoryRenameable(model, catId)) return
    val cat = model.getCategory(catId) ?: return
    if (cat.name == newName) return
    TODO("APR: use JVM equivalent - send rename request to server")
}

fun copyInventoryCategory(
    model: LLInventoryModel,
    cat: LLViewerInventoryCategory,
    parentId: UUID,
    rootCopyId: UUID = UUID.randomUUID(),
    moveNoCopyItems: Boolean = false,
    callback: ((UUID) -> Unit)? = null
) {
    TODO("APR: use JVM equivalent - create category copy via server API")
}

fun copyInventoryCategoryContent(
    newCatUuid: UUID,
    model: LLInventoryModel,
    cat: LLViewerInventoryCategory,
    rootCopyId: UUID,
    moveNoCopyItems: Boolean
) {
    TODO("APR: use JVM equivalent - copy all descendant items and subcategories")
}

fun appendPath(id: UUID, path: StringBuilder) {
    TODO("APR: use JVM equivalent - walk parent chain building path string")
}

fun makePath(obj: LLInventoryObject): String {
    val sb = StringBuilder()
    appendPath(obj.uuid, sb)
    return "$sb/${obj.name}"
}

fun makeInventoryPath(id: UUID): String {
    TODO("APR: use JVM equivalent - look up object and build path")
}

fun makeInfo(obj: LLInventoryObject): String = "'${makePath(obj)}' (${obj.uuid})"

fun makeInventoryInfo(id: UUID): String {
    TODO("APR: use JVM equivalent - look up object and format info string")
}

fun canMoveItemToMarketplace(
    rootFolder: LLInventoryCategory?,
    destFolder: LLInventoryCategory,
    invItem: LLInventoryItem,
    tooltipMsg: StringBuilder,
    bundleSize: Int = 1,
    fromPaste: Boolean = false
): Boolean {
    TODO("APR: use JVM equivalent - validate marketplace item move permissions and counts")
}

fun canMoveFolderToMarketplace(
    rootFolder: LLInventoryCategory?,
    destFolder: LLInventoryCategory,
    invCat: LLInventoryCategory,
    tooltipMsg: StringBuilder,
    bundleSize: Int = 1,
    checkItems: Boolean = true,
    fromPaste: Boolean = false
): Boolean {
    TODO("APR: use JVM equivalent - validate marketplace folder move depth and counts")
}

fun moveItemToMarketplacelistings(invItem: LLInventoryItem, destFolder: UUID, copy: Boolean = false): Boolean {
    TODO("APR: use JVM equivalent - move or copy item into marketplace listings folder")
}

fun moveFolderToMarketplacelistings(invCat: LLInventoryCategory, destFolder: UUID, copy: Boolean = false, moveNoCopyItems: Boolean = false): Boolean {
    TODO("APR: use JVM equivalent - move or copy folder into marketplace listings folder")
}

fun depthNestingInMarketplace(curUuid: UUID): Int {
    TODO("APR: use JVM equivalent - compute depth of UUID within marketplace listings root")
}

fun nestedParentId(curUuid: UUID, depth: Int): UUID {
    TODO("APR: use JVM equivalent - walk parent chain to listing root at given depth")
}

fun computeStockCount(catUuid: UUID, forceCount: Boolean = false): Int {
    TODO("APR: use JVM equivalent - count copyable items in stock folder hierarchy")
}

fun changeItemParent(itemId: UUID, newParentId: UUID) {
    TODO("APR: use JVM equivalent - reparent inventory item via server API")
}

fun moveItemsToNewSubfolder(selectedUuids: List<UUID>, folderName: String) {
    TODO("APR: use JVM equivalent - create subfolder and move selected items into it")
}

fun moveItemsToFolder(newCatUuid: UUID, selectedUuids: List<UUID>) {
    TODO("APR: use JVM equivalent - move each selected item to target folder")
}

fun isOnlyCatsSelected(selectedUuids: List<UUID>): Boolean {
    TODO("APR: use JVM equivalent - check all selected UUIDs are categories")
}

fun isOnlyItemsSelected(selectedUuids: List<UUID>): Boolean {
    TODO("APR: use JVM equivalent - check all selected UUIDs are items")
}

fun getCategoryPath(catId: UUID): String {
    TODO("APR: use JVM equivalent - build slash-delimited path to category")
}

fun canMoveToOutfit(invItem: LLInventoryItem, moveIsIntoCurrentOutfit: Boolean): Boolean {
    TODO("APR: use JVM equivalent - check item type and permissions for outfit move")
}

fun canMoveToLandmarks(invItem: LLInventoryItem): Boolean {
    TODO("APR: use JVM equivalent - check item type is landmark")
}

fun canMoveToMyOutfitsAsOutfit(model: LLInventoryModel, invCat: LLInventoryCategory, wearLimit: UInt): Boolean {
    TODO("APR: use JVM equivalent - check outfit folder validity and wear limit")
}

fun canMoveToMyOutfitsAsSubfolder(model: LLInventoryModel, invCat: LLInventoryCategory, depth: Int = 0): Boolean {
    TODO("APR: use JVM equivalent - check subfolder nesting rules for My Outfits")
}

fun getLocalizedFolderName(catUuid: UUID): String {
    TODO("APR: use JVM equivalent - look up localized name for system folder type")
}

fun newFolderWindow(folderId: UUID) {
    TODO("APR: use JVM equivalent - open a standalone single-folder inventory window")
}

fun ungroupFolderItems(folderId: UUID) {
    TODO("APR: use JVM equivalent - move all direct children of folder to its parent")
}

fun getIsFavorite(obj: LLInventoryObject): Boolean {
    TODO("APR: use JVM equivalent - check per-account favorite flag for object")
}

fun getIsFavorite(objId: UUID): Boolean {
    TODO("APR: use JVM equivalent - look up object and check favorite flag")
}

fun setFavorite(objId: UUID, favorite: Boolean) {
    TODO("APR: use JVM equivalent - persist favorite flag to account settings")
}

fun toggleFavorite(objId: UUID) {
    TODO("APR: use JVM equivalent - invert favorite flag for object")
}

fun toggleFavorites(ids: List<UUID>) {
    ids.forEach { toggleFavorite(it) }
}

fun getSearchableDescription(model: LLInventoryModel, itemId: UUID): String {
    TODO("APR: use JVM equivalent - fetch item description for search indexing")
}

fun getSearchableCreatorName(model: LLInventoryModel, itemId: UUID): String {
    TODO("APR: use JVM equivalent - resolve creator UUID to display name for search")
}

fun getSearchableUUID(model: LLInventoryModel, itemId: UUID): String {
    TODO("APR: use JVM equivalent - return asset UUID string for search indexing")
}

fun canShareItem(itemId: UUID): Boolean {
    TODO("APR: use JVM equivalent - check transfer permission and avatar friendship")
}

enum class EMyOutfitsSubfolderType {
    MY_OUTFITS_NO,
    MY_OUTFITS_SUBFOLDER,
    MY_OUTFITS_OUTFIT,
    MY_OUTFITS_SUBOUTFIT
}

fun myoutfitObjectSubfolderType(
    model: LLInventoryModel,
    objId: UUID,
    myOutfitsId: UUID
): EMyOutfitsSubfolderType {
    TODO("APR: use JVM equivalent - classify object relative to My Outfits folder")
}

class LLMarketplaceValidator private constructor() {

    data class ValidationRequest(
        val categoryId: UUID,
        val cbDone: ((Boolean) -> Unit)?,
        val cbMsg: ((String, Int, Int) -> Unit)?,
        val fixHierarchy: Boolean,
        val depth: Int
    )

    private var validationInProgress: Boolean = false
    private var pendingCallbacks: Int = 0
    private var pendingResult: Boolean = false
    private val validationQueue: ArrayDeque<ValidationRequest> = ArrayDeque()

    companion object {
        val instance: LLMarketplaceValidator by lazy { LLMarketplaceValidator() }
    }

    fun validateMarketplaceListings(
        categoryId: UUID,
        cbDone: ((Boolean) -> Unit)? = null,
        cbMsg: ((String, Int, Int) -> Unit)? = null,
        fixHierarchy: Boolean = true,
        depth: Int = -1
    ) {
        validationQueue.add(ValidationRequest(categoryId, cbDone, cbMsg, fixHierarchy, depth))
        if (!validationInProgress) {
            start()
        }
    }

    private fun start() {
        val request = validationQueue.poll() ?: return
        validationInProgress = true
        TODO("APR: use JVM equivalent - perform async marketplace hierarchy validation")
    }
}

abstract class LLInventoryCollectFunctor {
    abstract fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean
    open fun exceedsLimit(): Boolean = false

    companion object {
        fun itemTransferCommonlyAllowed(item: LLInventoryItem?): Boolean {
            TODO("APR: use JVM equivalent - check item transfer permission flags")
        }
    }
}

class LLAssetIDMatches(private val assetId: UUID) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return item.assetId == assetId
    }
}

class LLLinkedItemIDMatches(private val baseItemId: UUID) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return item.isLinkType && item.linkedUuid == baseItemId
    }
}

class LLIsFolderType(private val type: LLFolderType.EType) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item != null) return false
        return cat?.preferredType == type
    }
}

class LLIsType(private val type: LLAssetType.EType) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return item.type == type
    }
}

class LLIsOneOfTypes(private val types: List<LLAssetType.EType>) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return item.type in types
    }
}

class LLIsNotType(private val type: LLAssetType.EType) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return item.type != type
    }
}

class LLIsOfAssetType(private val type: LLAssetType.EType) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item != null) return item.type == type
        if (cat != null) return LLAssetType.lookupCategoryType(cat.preferredType) == type
        return false
    }
}

class LLAssetIDAndTypeMatches(private val assetId: UUID, private val type: LLAssetType.EType) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return item.assetId == assetId && item.type == type
    }
}

class LLIsValidItemLink : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        if (!item.isLinkType) return false
        return item.linkedUuid != null
    }
}

class LLIsTypeWithPermissions(
    private val type: LLAssetType.EType,
    private val perm: Int,
    private val agentId: UUID,
    private val groupId: UUID
) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null || item.type != type) return false
        TODO("APR: use JVM equivalent - check perm bit against agent/group")
    }
}

class LLFavoritesCollector : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        TODO("APR: use JVM equivalent - check item favorite flag in account settings")
    }
}

class LLBuddyCollector : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        if (item.type != LLAssetType.EType.AT_CALLINGCARD) return false
        TODO("APR: use JVM equivalent - filter non-null, non-self calling cards")
    }
}

class LLUniqueBuddyCollector : LLInventoryCollectFunctor() {
    private val seen: MutableSet<UUID> = mutableSetOf()

    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        if (item.type != LLAssetType.EType.AT_CALLINGCARD) return false
        val id = item.assetId ?: return false
        return seen.add(id)
    }
}

class LLParticularBuddyCollector(private val buddyId: UUID) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        if (item.type != LLAssetType.EType.AT_CALLINGCARD) return false
        return item.assetId == buddyId
    }
}

class LLNameCategoryCollector(private val name: String) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item != null) return false
        return cat?.name?.toLowerCase()?.startsWith(name.toLowerCase()) == true
    }
}

class LLFindCOFValidItems : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return when (item.type) {
            LLAssetType.EType.AT_OBJECT,
            LLAssetType.EType.AT_BODYPART,
            LLAssetType.EType.AT_CLOTHING,
            LLAssetType.EType.AT_GESTURE -> true
            else -> false
        }
    }
}

class LLFindBrokenLinks : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        if (!item.isLinkType) return false
        return item.linkedUuid == null
    }
}

class LLFindByMask(private val filterMask: Long) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return (filterMask and (1L shl item.inventoryType.ordinal)) != 0L
    }
}

class LLFindNonLinksByMask(private var filterMask: Long) : LLInventoryCollectFunctor() {
    fun setFilterMask(mask: Long) { filterMask = mask }

    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null || item.isLinkType) return false
        return (filterMask and (1L shl item.inventoryType.ordinal)) != 0L
    }
}

class LLFindWearables : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return item.type == LLAssetType.EType.AT_CLOTHING || item.type == LLAssetType.EType.AT_BODYPART
    }
}

class LLFindWearablesEx(private val isWorn: Boolean, private val includeBodyParts: Boolean = true) : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        val wearable = item.type == LLAssetType.EType.AT_CLOTHING ||
            (includeBodyParts && item.type == LLAssetType.EType.AT_BODYPART)
        if (!wearable) return false
        TODO("APR: use JVM equivalent - compare worn state to isWorn parameter")
    }
}

open class LLFindWearablesOfType(private var wearableType: LLWearableType.EType) : LLInventoryCollectFunctor() {
    fun setType(type: LLWearableType.EType) { wearableType = type }

    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return item.wearableType == wearableType
    }
}

class LLIsTextureType : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null) return false
        return item.type == LLAssetType.EType.AT_TEXTURE
    }
}

class LLFindActualWearablesOfType(wearableType: LLWearableType.EType) : LLFindWearablesOfType(wearableType) {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item != null && item.isLinkType) return false
        return super.invoke(cat, item)
    }
}

open class LLIsTypeActual(type: LLAssetType.EType) : LLIsType(type) {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item != null && item.isLinkType) return false
        return super.invoke(cat, item)
    }
}

class LLFindNonRemovableObjects : LLInventoryCollectFunctor() {
    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        TODO("APR: use JVM equivalent - check item or category removability")
    }
}

class LLFindLandmarks(private val filterDuplicates: Boolean, private val filterSelf: Boolean) : LLInventoryCollectFunctor() {
    private val assetIds: MutableList<UUID> = mutableListOf()

    override fun invoke(cat: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null || item.type != LLAssetType.EType.AT_LANDMARK) return false
        if (filterDuplicates) {
            val assetId = item.assetId ?: return false
            if (assetId in assetIds) return false
            assetIds.add(assetId)
        }
        if (filterSelf) {
            TODO("APR: use JVM equivalent - exclude landmark at agent's current region")
        }
        return true
    }
}

object LLInventoryState {
    var sWearNewClothing: Boolean = false
    var sWearNewClothingTransactionID: UUID = UUID.randomUUID()
}

object LLInventoryAction {
    private val marketplaceFolders: MutableList<UUID> = mutableListOf()

    fun doToSelected(model: LLInventoryModel, root: LLFolderView, action: String, userConfirm: Boolean = true) {
        TODO("APR: use JVM equivalent - dispatch inventory action to selected items")
    }

    fun removeItemFromDND(root: LLFolderView) {
        TODO("APR: use JVM equivalent - remove items from do-not-disturb notification storage")
    }

    fun fileUploadLocation(destId: UUID, action: String) {
        TODO("APR: use JVM equivalent - persist upload location preference for folder")
    }

    fun isFileUploadLocation(destId: UUID, action: String): Boolean {
        TODO("APR: use JVM equivalent - check if folder matches saved upload location preference")
    }

    fun saveMultipleTextures(filenames: List<String>, selectedItems: Set<LLFolderViewItem>, model: LLInventoryModel) {
        TODO("APR: use JVM equivalent - save texture assets to local files")
    }

    private fun buildMarketplaceFolders(root: LLFolderView) {
        TODO("APR: use JVM equivalent - collect marketplace listing folders needing update")
    }

    private fun updateMarketplaceFolders() {
        TODO("APR: use JVM equivalent - trigger marketplace status refresh for collected folders")
    }
}

// ---------------------------------------------------------------------------
// Stub platform types referenced above; actual definitions live elsewhere
// in the Kotlin project.
// ---------------------------------------------------------------------------

open class LLInventoryObject {
    open var uuid: UUID = UUID.randomUUID()
    open var name: String = ""
    open var parentUuid: UUID = UUID.randomUUID()
    open var isLinkType: Boolean = false
    open var type: LLAssetType.EType = LLAssetType.EType.AT_UNKNOWN
    open var creationDate: Long = 0L
}

open class LLInventoryItem : LLInventoryObject() {
    open var assetId: UUID? = null
    open var linkedUuid: UUID? = null
    open var inventoryType: LLInventoryType.EType = LLInventoryType.EType.IT_NONE
    open var wearableType: LLWearableType.EType = LLWearableType.EType.WT_NONE
    open var flags: UInt = 0u
}

open class LLViewerInventoryItem : LLInventoryItem()

open class LLInventoryCategory : LLInventoryObject() {
    open var preferredType: LLFolderType.EType = LLFolderType.EType.FT_NONE
}

open class LLViewerInventoryCategory : LLInventoryCategory() {
    open var thumbnailUuid: UUID = UUID.randomUUID()
}

abstract class LLInventoryModel {
    abstract val rootFolderID: UUID
    abstract fun isObjectDescendentOf(id: UUID, ancestorId: UUID): Boolean
    abstract fun getItem(id: UUID): LLViewerInventoryItem?
    abstract fun getCategory(id: UUID): LLViewerInventoryCategory?
    abstract fun getObject(id: UUID): LLInventoryObject?
}

class LLFolderView
class LLFolderViewItem

object LLAssetType {
    enum class EType {
        AT_TEXTURE, AT_SOUND, AT_CALLINGCARD, AT_LANDMARK, AT_SCRIPT,
        AT_CLOTHING, AT_OBJECT, AT_NOTECARD, AT_CATEGORY, AT_LSL_TEXT,
        AT_LSL_BYTECODE, AT_TEXTURE_TGA, AT_BODYPART, AT_TRASH, AT_SNAPSHOT_CATEGORY,
        AT_LOST_AND_FOUND, AT_SOUND_WAV, AT_IMAGE_TGA, AT_IMAGE_JPEG, AT_ANIMATION,
        AT_GESTURE, AT_SIMSTATE, AT_LINK, AT_LINK_FOLDER, AT_MESH, AT_SETTINGS,
        AT_MATERIAL, AT_UNKNOWN
    }

    fun lookupIsLinkType(type: EType): Boolean = type == EType.AT_LINK || type == EType.AT_LINK_FOLDER
    fun lookupIsAssetIDKnowable(type: EType): Boolean = type != EType.AT_CATEGORY
    fun lookupCanLink(type: EType): Boolean = !lookupIsLinkType(type)
    fun lookupCategoryType(folderType: LLFolderType.EType): EType = EType.AT_CATEGORY
}

object LLInventoryType {
    enum class EType {
        IT_NONE, IT_TEXTURE, IT_SOUND, IT_CALLINGCARD, IT_LANDMARK, IT_OBJECT,
        IT_NOTECARD, IT_CATEGORY, IT_ROOTCATEGORY, IT_LSL, IT_SNAPSHOT,
        IT_ATTACHMENT, IT_WEARABLE, IT_ANIMATION, IT_GESTURE, IT_MESH,
        IT_SETTINGS, IT_MATERIAL
    }

    enum class EIconName {
        ICONNAME_TEXTURE, ICONNAME_SOUND, ICONNAME_CALLINGCARD_ONLINE,
        ICONNAME_CALLINGCARD_OFFLINE, ICONNAME_LANDMARK, ICONNAME_LANDMARK_VISITED,
        ICONNAME_SCRIPT, ICONNAME_CLOTHING, ICONNAME_OBJECT, ICONNAME_OBJECT_MULTI,
        ICONNAME_NOTECARD, ICONNAME_BODYPART, ICONNAME_SNAPSHOT,
        ICONNAME_BODYPART_SHAPE, ICONNAME_BODYPART_SKIN, ICONNAME_BODYPART_HAIR, ICONNAME_BODYPART_EYES,
        ICONNAME_CLOTHING_SHIRT, ICONNAME_CLOTHING_PANTS, ICONNAME_CLOTHING_SHOES,
        ICONNAME_CLOTHING_SOCKS, ICONNAME_CLOTHING_JACKET, ICONNAME_CLOTHING_GLOVES,
        ICONNAME_CLOTHING_UNDERSHIRT, ICONNAME_CLOTHING_UNDERPANTS, ICONNAME_CLOTHING_SKIRT,
        ICONNAME_CLOTHING_ALPHA, ICONNAME_CLOTHING_TATTOO, ICONNAME_CLOTHING_UNIVERSAL,
        ICONNAME_ANIMATION, ICONNAME_GESTURE, ICONNAME_CLOTHING_PHYSICS,
        ICONNAME_LINKITEM, ICONNAME_LINKFOLDER, ICONNAME_MESH,
        ICONNAME_SETTINGS_SKY, ICONNAME_SETTINGS_WATER, ICONNAME_SETTINGS_DAY, ICONNAME_SETTINGS,
        ICONNAME_MATERIAL, ICONNAME_INVALID, ICONNAME_UNKNOWN, ICONNAME_NONE
    }
}

object LLFolderType {
    enum class EType {
        FT_NONE, FT_TEXTURE, FT_SOUND, FT_CALLINGCARD, FT_LANDMARK, FT_CLOTHING,
        FT_OBJECT, FT_NOTECARD, FT_ROOT_INVENTORY, FT_TRASH, FT_SNAPSHOT_CATEGORY,
        FT_LOST_AND_FOUND, FT_ANIMATION, FT_GESTURE, FT_FAVORITE, FT_OUTFIT,
        FT_MY_OUTFITS, FT_MESH, FT_MARKETPLACE_LISTINGS, FT_MARKETPLACE_STOCK,
        FT_INBOX, FT_OUTBOX, FT_BASIC_ROOT, FT_SETTINGS, FT_MATERIAL, FT_SUITCASE
    }

    fun lookupIsProtectedType(type: EType): Boolean = when (type) {
        EType.FT_TRASH, EType.FT_SNAPSHOT_CATEGORY, EType.FT_LOST_AND_FOUND,
        EType.FT_ROOT_INVENTORY, EType.FT_FAVORITE, EType.FT_MY_OUTFITS,
        EType.FT_INBOX, EType.FT_OUTBOX, EType.FT_BASIC_ROOT -> true
        else -> false
    }

    fun assetTypeToFolderType(type: LLAssetType.EType): EType = EType.FT_NONE
}

object LLWearableType {
    enum class EType { WT_NONE, WT_SHAPE, WT_SKIN, WT_HAIR, WT_EYES,
        WT_SHIRT, WT_PANTS, WT_SHOES, WT_SOCKS, WT_JACKET, WT_GLOVES,
        WT_UNDERSHIRT, WT_UNDERPANTS, WT_SKIRT, WT_ALPHA, WT_TATTOO,
        WT_PHYSICS, WT_UNIVERSAL }
}
