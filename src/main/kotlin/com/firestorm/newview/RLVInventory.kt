package com.firestorm.newview

import java.util.UUID

// ============================================================================
// RlvInventory (singleton)
//

object RlvInventory : LLInventoryObserver {

    private const val SHARED_ROOT_NAME = "#RLV"

    private var fetchStarted: Boolean = false
    private var fetchComplete: Boolean = false
    private var idRlvRoot: UUID = UUID(0, 0)

    val onSharedRootIdChanged: MutableList<() -> Unit> = mutableListOf()

    fun addSharedRootIdChangedCallback(cb: () -> Unit) {
        onSharedRootIdChanged.add(cb)
    }

    // ---- LLInventoryObserver ----

    override fun changed(mask: UInt) {
        val changedIds = gInventory.changedIds
        if (changedIds.contains(idRlvRoot)) {
            gInventory.removeObserver(this)
            val prevRoot = idRlvRoot
            idRlvRoot = UUID(0, 0)
            if (prevRoot != getSharedRootId()) {
                onSharedRootIdChanged.forEach { it() }
            }
        }
    }

    // ---- #RLV shared inventory ----

    fun findSharedFolders(criteria: String, folders: MutableList<LLViewerInventoryCategory>): Boolean {
        val rlvRoot = getSharedRoot() ?: return false
        folders.clear()
        TODO("APR: use JVM equivalent — collect descendant categories matching RlvCriteriaCategoryCollector against the shared root")
    }

    fun getPath(idItems: List<UUID>, folders: MutableList<LLViewerInventoryCategory>): Boolean {
        val rlvRoot = getSharedRoot() ?: return false
        folders.clear()
        for (idItem in idItems) {
            val item = gInventory.getItem(idItem) ?: continue
            if (!gInventory.isObjectDescendentOf(item.uuid, rlvRoot.uuid)) continue
            var folder = gInventory.getCategory(item.parentUuid)
            if (folder != null && isFoldedFolder(folder, checkComposite = true)) {
                folder = gInventory.getCategory(folder.parentUuid)
            }
            if (folder != null) folders.add(folder)
        }
        return folders.isNotEmpty()
    }

    fun getSharedRoot(): LLViewerInventoryCategory? {
        val id = getSharedRootId()
        return if (id != UUID(0, 0)) gInventory.getCategory(id) else null
    }

    fun getSharedRootId(): UUID {
        if (idRlvRoot == UUID(0, 0) && gInventory.isInventoryUsable) {
            val rootFolders = gInventory.getDirectDescendentFolders(gInventory.rootFolderId)
            var bestMatch: LLViewerInventoryCategory? = null
            for (folder in rootFolders) {
                if (folder.name == SHARED_ROOT_NAME) {
                    bestMatch = folder
                    if (getDirectDescendentsFolderCount(folder) > 0) break
                }
            }
            bestMatch?.let {
                idRlvRoot = it.uuid
                gInventory.addObserver(this)
            }
        }
        return idRlvRoot
    }

    fun getSharedFolder(idParent: UUID, folderName: String, matchPartial: Boolean = true): LLViewerInventoryCategory? {
        if (folderName.isEmpty()) return null
        val folders = gInventory.getDirectDescendentFolders(idParent)
        var partial: LLViewerInventoryCategory? = null
        for (folder in folders) {
            val name = folder.name
            if (name.equals(folderName, ignoreCase = true)) return folder
            if (matchPartial && partial == null && name.isNotEmpty() &&
                name[0] != RLV_FOLDER_PREFIX_HIDDEN && name.contains(folderName, ignoreCase = true)
            ) {
                partial = folder
            }
        }
        return partial
    }

    fun getSharedFolder(path: String, matchPartial: Boolean = true): LLViewerInventoryCategory? {
        var folder: LLViewerInventoryCategory = getSharedRoot() ?: return null
        for (token in path.split("/").filter { it.isNotEmpty() }) {
            folder = getSharedFolder(folder.uuid, token, matchPartial) ?: return null
        }
        return folder
    }

    fun getSharedPath(pFolder: LLViewerInventoryCategory?): String {
        val rlvRoot = getSharedRoot() ?: return ""
        if (pFolder == null || rlvRoot.uuid == pFolder.uuid) return ""

        val idRLV = rlvRoot.uuid
        val idRoot = gInventory.rootFolderId
        var cur: LLViewerInventoryCategory = pFolder
        val sb = StringBuilder()

        while (true) {
            sb.insert(0, "/${cur.name}")
            val idParent = cur.parentUuid
            if (idParent == idRLV) break
            if (idParent == idRoot) return ""
            cur = gInventory.getCategory(idParent) ?: return ""
        }

        return sb.toString().removePrefix("/")
    }

    fun getSharedPath(idFolder: UUID): String = getSharedPath(gInventory.getCategory(idFolder))

    fun isSharedFolder(idFolder: UUID): Boolean {
        val rlvRoot = getSharedRoot() ?: return false
        return rlvRoot.uuid != idFolder && gInventory.isObjectDescendentOf(idFolder, rlvRoot.uuid)
    }

    fun isGiveToRLVOffer(offerInfo: LLOfferInfo): Boolean {
        if (RlvSettings.forbidGiveToRlv || getSharedRoot() == null) return false
        return if (offerInfo.fromObject) {
            offerInfo.im == IMType.IM_TASK_INVENTORY_OFFERED &&
                offerInfo.type == LLAssetType.AT_CATEGORY &&
                offerInfo.desc.indexOf(RLV_PUTINV_PREFIX) == 1
        } else {
            offerInfo.im == IMType.IM_INVENTORY_OFFERED &&
                offerInfo.type == LLAssetType.AT_CATEGORY &&
                offerInfo.desc.indexOf(RLV_PUTINV_PREFIX) == 0
        }
    }

    // ---- Inventory fetching ----

    fun fetchSharedInventory() {
        val rlvRoot = getSharedRoot()
        if (fetchStarted || rlvRoot == null) return
        TODO("APR: use JVM equivalent — collect all descendant folder UUIDs under shared root and start batch fetch via RlvSharedInventoryFetcher")
    }

    fun fetchWornItems() {
        TODO("APR: use JVM equivalent — collect worn clothing/bodypart/attachment item UUIDs and start batch item fetch")
    }

    private fun fetchSharedLinks() {
        TODO("APR: use JVM equivalent — collect inventory links under shared root and fetch their targets (items and folder targets separately)")
    }

    // ---- General-purpose helpers ----

    fun getDirectDescendentsFolderCount(folder: LLInventoryCategory?): Int {
        folder ?: return 0
        return gInventory.getDirectDescendentFolders(folder.uuid).size
    }

    fun getDirectDescendentsItemCount(folder: LLInventoryCategory?, filterType: LLAssetType): Int {
        folder ?: return 0
        return gInventory.getDirectDescendentItems(folder.uuid).count { it.type == filterType }
    }

    fun getFoldedParent(idFolder: UUID, checkComposite: Boolean): UUID {
        var folder = gInventory.getCategory(idFolder)
        while (folder != null && isFoldedFolder(folder, checkComposite)) {
            folder = gInventory.getCategory(folder.parentUuid)
        }
        return folder?.uuid ?: UUID(0, 0)
    }

    fun isFoldedFolder(folder: LLInventoryCategory?, checkComposite: Boolean): Boolean {
        if (folder == null) return false
        val name = folder.name
        if (!RlvSettings.enableLegacyNaming && (name.isEmpty() || name[0] != RLV_FOLDER_PREFIX_HIDDEN)) return false
        return RlvAttachPtLookup.getAttachPointIndex(folder) != 0 ||
            name == ".($RLV_FOLDER_FLAG_NOSTRIP)"
    }

    private const val RLV_FOLDER_PREFIX_HIDDEN = '.'
    private const val RLV_FOLDER_FLAG_NOSTRIP = "nostrip"
    private const val RLV_PUTINV_PREFIX = "~"
}

// ============================================================================
// RlvRenameOnWearObserver
//

class RlvRenameOnWearObserver(idItem: UUID) : LLInventoryFetchItemsObserver(idItem) {

    override fun done() {
        gInventory.removeObserver(this)
        doOnIdleOneTime { doneIdle() }
    }

    private fun doneIdle() {
        val rlvRoot = RlvInventory.getSharedRoot()
        if (!RlvSettings.enableSharedWear || !RlvSettings.sharedInvAutoRename || rlvRoot == null) {
            return
        }
        TODO("APR: use JVM equivalent — for each completed item, rename or move to attach-point-named folder under #RLV if permitted by item permissions")
    }

    companion object {
        fun onCategoryCreate(idFolder: UUID, idItem: UUID) {
            if (idFolder != UUID(0, 0) && idItem != UUID(0, 0)) {
                TODO("APR: use JVM equivalent — move inventory item to newly created folder via server call")
            }
        }
    }
}

// ============================================================================
// RlvGiveToRLVOffer
//

abstract class RlvGiveToRLVOffer {

    private val destPath: ArrayDeque<String> = ArrayDeque()

    protected fun createDestinationFolder(path: String): Boolean {
        destPath.clear()
        if (!path.startsWith(RLV_PUTINV_PREFIX)) return false

        val parts = path.split(RLV_PUTINV_SEPARATOR).filter { it.isNotEmpty() }
        if (parts.size < 2 || parts.size > RLV_PUTINV_MAXDEPTH) return false

        destPath.addAll(parts)

        if (destPath.first() == RLV_ROOT_FOLDER) {
            destPath.removeFirst()
            val idRlvRoot = RlvInventory.getSharedRootId()
            if (idRlvRoot != UUID(0, 0)) {
                onCategoryCreateCallback(idRlvRoot, this)
            } else {
                TODO("APR: use JVM equivalent — create #RLV root folder then continue via onCategoryCreateCallback")
            }
            return true
        }
        destPath.clear()
        return false
    }

    protected abstract fun onDestinationCreated(idDestFolder: UUID, name: String)

    companion object {
        fun moveAndRename(idFolder: UUID, idDestination: UUID, name: String, cbFinal: InventoryCallback?) {
            TODO("APR: use JVM equivalent — move folder to destination, rename if needed, fire callback on completion")
        }

        private fun onCategoryCreateCallback(idFolder: UUID, instance: RlvGiveToRLVOffer) {
            if (idFolder == UUID(0, 0)) {
                instance.onDestinationCreated(UUID(0, 0), "")
                return
            }
            while (instance.destPath.size > 1) {
                val folderName = instance.destPath.removeFirst()
                val existing = RlvInventory.getSharedFolder(idFolder, folderName, matchPartial = false)
                if (existing != null) {
                    onCategoryCreateCallback(existing.uuid, instance)
                    return
                } else {
                    TODO("APR: use JVM equivalent — create new inventory folder named folderName under idFolder then call onCategoryCreateCallback recursively")
                }
            }
            instance.onDestinationCreated(idFolder, instance.destPath.first())
        }

        private const val RLV_PUTINV_PREFIX = "~"
        private const val RLV_PUTINV_SEPARATOR = "/"
        private const val RLV_PUTINV_MAXDEPTH = 20
        private const val RLV_ROOT_FOLDER = "#RLV"
    }
}

typealias InventoryCallback = (UUID) -> Unit

// ============================================================================
// RlvGiveToRLVTaskOffer
//

class RlvGiveToRLVTaskOffer(private val idTransaction: UUID) : LLInventoryObserver, RlvGiveToRLVOffer() {

    private val folders: MutableList<UUID> = mutableListOf()

    override fun changed(mask: UInt) {
        if (mask and LLInventoryObserver.ADD != 0u &&
            gInventory.transactionId != UUID(0, 0) &&
            idTransaction == gInventory.transactionId
        ) {
            for (idItem in gInventory.addedIds) {
                val cat = gInventory.getCategory(idItem)
                if (cat != null && !folders.contains(cat.uuid)) {
                    folders.add(cat.uuid)
                }
            }
            done()
        }
    }

    private fun done() {
        gInventory.removeObserver(this)
        doOnIdleOneTime { doneIdle() }
    }

    private fun doneIdle() {
        val folder = folders.firstOrNull()?.let { gInventory.getCategory(it) }
        if (folder == null || !createDestinationFolder(folder.name)) {
            // Nothing to do
        }
    }

    override fun onDestinationCreated(idDestFolder: UUID, name: String) {
        if (idDestFolder != UUID(0, 0)) {
            moveAndRename(folders.first(), idDestFolder, name) { idOfferedFolder ->
                onOfferCompleted(idOfferedFolder)
            }
        } else {
            onOfferCompleted(UUID(0, 0))
        }
    }

    private fun onOfferCompleted(idOfferedFolder: UUID) {
        if (idOfferedFolder != UUID(0, 0)) {
            RlvBehaviourNotifyHandler.sendNotification(
                "accepted_in_rlv inv_offer ${RlvInventory.getSharedPath(idOfferedFolder)}"
            )
        }
    }
}

// ============================================================================
// RlvGiveToRLVAgentOffer
//

class RlvGiveToRLVAgentOffer(idFolder: UUID) : LLInventoryFetchDescendentsObserver(idFolder), RlvGiveToRLVOffer() {

    override fun done() {
        gInventory.removeObserver(this)
        doOnIdleOneTime { doneIdle() }
    }

    private fun doneIdle() {
        val folder = completedFolders.firstOrNull()?.let { gInventory.getCategory(it) }
        if (folder == null || !createDestinationFolder(folder.name)) {
            // Nothing to do
        }
    }

    override fun onDestinationCreated(idDestFolder: UUID, name: String) {
        val srcFolder = completedFolders.firstOrNull()
        if (idDestFolder != UUID(0, 0) && srcFolder != null) {
            moveAndRename(srcFolder, idDestFolder, name, null)
        }
    }
}

// ============================================================================
// RlvCriteriaCategoryCollector
//

class RlvCriteriaCategoryCollector(criteria: String) : LLInventoryCollectFunctor() {

    private val criteriaList: List<String>

    init {
        val parts = mutableListOf<String>()
        var last = 0
        while (last < criteria.length) {
            val idx = criteria.indexOf("&&", last).let { if (it == -1) criteria.length else it }
            if (idx != last) parts.add(criteria.substring(last, idx))
            last = idx + 2
        }
        criteriaList = parts
    }

    override fun invoke(folder: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (folder == null || criteriaList.isEmpty()) return false
        val name = folder.name.lowercase()
        if (name.isEmpty() || name[0] == '.' || name[0] == '~') return false
        return criteriaList.all { name.contains(it) }
    }
}

// ============================================================================
// RlvWearableItemCollector
//

class RlvWearableItemCollector(
    rootFolder: LLInventoryCategory,
    private var wearAction: RlvForceWear.EWearAction,
    private val wearFlags: RlvForceWear.EWearFlags
) : LLInventoryCollectFunctor() {

    private val idFolder: UUID = rootFolder.uuid
    private val wearAddPrefix: String = RlvSettings.wearAddPrefix.takeIf { it.length > 1 && it[0] != '.' } ?: ""
    private val wearReplacePrefix: String = RlvSettings.wearReplacePrefix.takeIf { it.length > 1 && it[0] != '.' } ?: ""

    private val folded: ArrayDeque<UUID> = ArrayDeque()
    private val linked: ArrayDeque<UUID> = ArrayDeque()
    private val wearable: ArrayDeque<UUID> = ArrayDeque()
    private val foldingMap: MutableMap<UUID, UUID> = mutableMapOf()
    private val wearActionMap: MutableMap<UUID, RlvForceWear.EWearAction> = mutableMapOf()

    init {
        wearable.addFirst(idFolder)
        wearAction = getWearActionNormal(rootFolder)
        wearActionMap[idFolder] = wearAction
    }

    override fun invoke(folder: LLInventoryCategory?, item: LLInventoryItem?): Boolean =
        if (folder != null) onCollectFolder(folder) else item?.let { onCollectItem(it) } ?: false

    fun getFoldedParent(idFolder: UUID): UUID {
        var cur = foldingMap[idFolder]
        var prev: UUID? = null
        while (cur != null) {
            prev = cur
            cur = foldingMap[cur]
        }
        return prev ?: idFolder
    }

    fun getWearAction(idFolder: UUID): RlvForceWear.EWearAction {
        var cur = idFolder
        while (true) {
            val action = wearActionMap[cur]
            if (action != null) return action
            val folder = gInventory.getCategory(cur) ?: break
            if (gInventory.rootFolderId == folder.parentUuid) break
            cur = folder.parentUuid
        }
        return wearAction
    }

    fun getWearActionNormal(folder: LLInventoryCategory): RlvForceWear.EWearAction {
        if (wearAction == RlvForceWear.EWearAction.ACTION_WEAR_REPLACE && wearAddPrefix.isNotEmpty() &&
            folder.name.startsWith(wearAddPrefix)
        ) {
            return RlvForceWear.EWearAction.ACTION_WEAR_ADD
        }
        if (wearAction == RlvForceWear.EWearAction.ACTION_WEAR_ADD && wearReplacePrefix.isNotEmpty() &&
            folder.name.startsWith(wearReplacePrefix)
        ) {
            return RlvForceWear.EWearAction.ACTION_WEAR_REPLACE
        }
        return if (folder.uuid != idFolder) getWearAction(folder.parentUuid) else wearAction
    }

    fun getWearActionFolded(folder: LLInventoryCategory): RlvForceWear.EWearAction =
        getWearAction(folder.parentUuid)

    fun isLinkedFolder(idFolder: UUID): Boolean = linked.contains(idFolder)

    private fun onCollectFolder(folder: LLInventoryCategory): Boolean {
        val isLinked = isLinkedFolder(folder.uuid)
        if (!isLinked && !wearable.contains(folder.parentUuid)) return false

        val name = folder.name
        if (name.isEmpty()) return false

        val attach = RlvForceWear.isWearAction(wearAction)
        val matchAll = !isLinked && (wearFlags.bits and RlvForceWear.EWearFlags.FLAG_MATCHALL.bits != 0)

        if (!isLinked && RlvInventory.isFoldedFolder(folder, checkComposite = false)) {
            if (!attach || RlvInventory.getDirectDescendentsItemCount(folder, LLAssetType.AT_OBJECT) == 1) {
                folded.addFirst(folder.uuid)
                foldingMap[folder.uuid] = folder.parentUuid
            }
        } else if (name[0] != '.' && (matchAll || isLinked) && !isLinkedFolder(folder.parentUuid)) {
            wearable.addFirst(folder.uuid)
            wearActionMap[folder.uuid] = getWearActionNormal(folder)
            return !isLinked && folder.parentUuid == idFolder
        }
        return false
    }

    private fun onCollectItem(item: LLInventoryItem): Boolean {
        val attach = RlvForceWear.isWearAction(wearAction)
        if (!attach && !RlvForceWear.isStrippable(item)) return false

        val idParent = item.parentUuid
        return when (item.type) {
            LLAssetType.AT_BODYPART -> {
                if (!attach) return false
                wearable.contains(idParent)
            }
            LLAssetType.AT_CLOTHING ->
                wearable.contains(idParent) ||
                    (attach && folded.contains(idParent) && RlvForceWear.isStrippable(item))
            LLAssetType.AT_OBJECT ->
                (wearable.contains(idParent) || folded.contains(idParent)) &&
                    (!attach || RlvAttachPtLookup.hasAttachPointName(item) || RlvSettings.enableSharedWear)
            else -> false
        }
    }
}

// ============================================================================
// RlvIsLinkType
//

class RlvIsLinkType : LLInventoryCollectFunctor() {
    override fun invoke(folder: LLInventoryCategory?, item: LLInventoryItem?): Boolean =
        item != null && item.isLinkType
}

// ============================================================================
// RlvFindAttachmentsOnPoint
//

class RlvFindAttachmentsOnPoint(private val attachPt: LLViewerJointAttachment?) : LLInventoryCollectFunctor() {
    override fun invoke(folder: LLInventoryCategory?, item: LLInventoryItem?): Boolean {
        if (item == null || item.type != LLAssetType.AT_OBJECT) return false
        return attachPt != null && attachPt.getAttachedObject(item.linkedUuid) != null
    }
}

// ============================================================================
// Stubs for referenced types (implemented elsewhere in the Kotlin port)
//

interface LLInventoryObserver {
    fun changed(mask: UInt)
    companion object {
        const val ADD: UInt = 0x01u
    }
}

abstract class LLInventoryFetchItemsObserver(val idItem: UUID) : LLInventoryObserver {
    override fun changed(mask: UInt) {}
    abstract fun done()
}

abstract class LLInventoryFetchDescendentsObserver(val idFolder: UUID) : LLInventoryObserver {
    val completedFolders: MutableList<UUID> = mutableListOf()
    override fun changed(mask: UInt) {}
    abstract fun done()
}

class LLViewerInventoryCategory(val uuid: UUID, val parentUuid: UUID, val name: String)
class LLViewerInventoryItem(
    val uuid: UUID,
    val parentUuid: UUID,
    val name: String,
    val type: LLAssetType,
    val actualType: LLAssetType,
    val isLinkType: Boolean,
    val linkedUuid: UUID
) {
    fun getLinkedItem(): LLViewerInventoryItem? = TODO("APR: use JVM equivalent")
}

class LLViewerJointAttachment {
    fun getAttachedObject(idItem: UUID): Any? = TODO("APR: use JVM equivalent")
}

class LLOfferInfo(
    val fromObject: Boolean,
    val im: IMType,
    val type: LLAssetType,
    val desc: String
)

enum class IMType { IM_TASK_INVENTORY_OFFERED, IM_INVENTORY_OFFERED }

object RlvBehaviourNotifyHandler {
    fun sendNotification(msg: String) { TODO("APR: use JVM equivalent") }
}

object RlvSettings {
    val forbidGiveToRlv: Boolean get() = TODO("APR: use JVM equivalent")
    val enableLegacyNaming: Boolean get() = TODO("APR: use JVM equivalent")
    val enableSharedWear: Boolean get() = TODO("APR: use JVM equivalent")
    val sharedInvAutoRename: Boolean get() = TODO("APR: use JVM equivalent")
    val wearAddPrefix: String get() = TODO("APR: use JVM equivalent")
    val wearReplacePrefix: String get() = TODO("APR: use JVM equivalent")
}

object RlvForceWear {
    enum class EWearAction { ACTION_WEAR_REPLACE, ACTION_WEAR_ADD }
    enum class EWearFlags(val bits: Int) { FLAG_MATCHALL(0x01) }
    fun isWearAction(action: EWearAction): Boolean = action == EWearAction.ACTION_WEAR_ADD || action == EWearAction.ACTION_WEAR_REPLACE
    fun isStrippable(item: LLInventoryItem): Boolean = TODO("APR: use JVM equivalent")
}

object gInventory {
    val rootFolderId: UUID get() = TODO("APR: use JVM equivalent")
    val isInventoryUsable: Boolean get() = TODO("APR: use JVM equivalent")
    val changedIds: List<UUID> get() = TODO("APR: use JVM equivalent")
    val transactionId: UUID get() = TODO("APR: use JVM equivalent")
    val addedIds: List<UUID> get() = TODO("APR: use JVM equivalent")
    fun getCategory(id: UUID): LLViewerInventoryCategory? = TODO("APR: use JVM equivalent")
    fun getItem(id: UUID): LLViewerInventoryItem? = TODO("APR: use JVM equivalent")
    fun getDirectDescendentFolders(idParent: UUID): List<LLViewerInventoryCategory> = TODO("APR: use JVM equivalent")
    fun getDirectDescendentItems(idParent: UUID): List<LLViewerInventoryItem> = TODO("APR: use JVM equivalent")
    fun isObjectDescendentOf(idObj: UUID, idAncestor: UUID): Boolean = TODO("APR: use JVM equivalent")
    fun addObserver(observer: LLInventoryObserver) { TODO("APR: use JVM equivalent") }
    fun removeObserver(observer: LLInventoryObserver) { TODO("APR: use JVM equivalent") }
}

fun doOnIdleOneTime(block: () -> Unit) { TODO("APR: use JVM equivalent — schedule block on main-thread idle loop") }
