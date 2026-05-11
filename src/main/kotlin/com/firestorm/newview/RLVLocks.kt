package com.firestorm.newview

import java.util.UUID

// ============================================================================
// ERlvLockMask
//

enum class ERlvLockMask(val bits: Int) {
    RLV_LOCK_NONE(0x00),
    RLV_LOCK_ADD(0x01),
    RLV_LOCK_REMOVE(0x02),
    RLV_LOCK_ANY(0x03);

    infix fun and(other: ERlvLockMask): Int = this.bits and other.bits
    infix fun or(other: ERlvLockMask): ERlvLockMask = entries.first { it.bits == (this.bits or other.bits) }
    companion object {
        val entries get() = values().toList()
        fun fromBits(bits: Int): ERlvLockMask = entries.firstOrNull { it.bits == bits } ?: RLV_LOCK_NONE
    }
}

enum class ERlvWearMask(val bits: Int) {
    RLV_WEAR_LOCKED(0x00),
    RLV_WEAR_ADD(0x01),
    RLV_WEAR_REPLACE(0x02),
    RLV_WEAR(0x03);
    companion object { val entries get() = values().toList() }
}

// ============================================================================
// RlvAttachPtLookup
//

object RlvAttachPtLookup {

    private val attachPtLookupMap: MutableMap<String, Int> = mutableMapOf()

    fun initLookupTable() {
        TODO("APR: use JVM equivalent — populate attachPtLookupMap from avatar attachment points, including 'root' alias for 'avatar center'")
    }

    fun getAttachPoint(idxAttachPt: Int): LLViewerJointAttachment? =
        TODO("APR: use JVM equivalent — look up attachment point by index on agent avatar")

    fun getAttachPoint(text: String): LLViewerJointAttachment? =
        getAttachPoint(getAttachPointIndex(text))

    fun getAttachPoint(item: LLInventoryItem?): LLViewerJointAttachment? =
        getAttachPoint(getAttachPointIndex(item))

    fun getAttachPointIndex(text: String): Int {
        val lower = text.lowercase()
        return attachPtLookupMap[lower] ?: 0
    }

    fun getAttachPointIndex(pAttachObj: Any?): Int =
        TODO("APR: use JVM equivalent — extract attachment state index from viewer object")

    fun getAttachPointIndex(pAttachPt: LLViewerJointAttachment?): Int =
        TODO("APR: use JVM equivalent — reverse-lookup attachment point index from agent avatar map")

    fun getAttachPointIndex(folder: LLInventoryCategory?): Int {
        folder ?: return 0
        if (RlvSettings.enableLegacyNaming) return getAttachPointIndexLegacy(folder)
        val name = folder.name
        if (name.length < 3 || name[0] != '.') return 0
        val inner = extractFirstParenthesisedText(name) ?: return 0
        return getAttachPointIndex(inner.trim())
    }

    fun getAttachPointIndex(item: LLInventoryItem?, followLinks: Boolean = true): Int {
        if (item == null || item.type != LLAssetType.AT_OBJECT) return 0
        if (item.actualType == LLAssetType.AT_LINK && followLinks) {
            val target = gInventory.getItem(item.linkedUuid)
            val idx = getAttachPointIndex(target, followLinks = false)
            if (idx != 0) return idx
        }
        val strAttachPt = if (item.actualType != LLAssetType.AT_LINK) {
            extractLastParenthesisedText(item.name)?.trim() ?: ""
        } else ""

        return if (item.isModifiable) {
            strAttachPt.ifEmpty { null }?.let { getAttachPointIndex(it) }
                ?: getAttachPointIndex(gInventory.getCategory(item.parentUuid))
        } else {
            getAttachPointIndex(gInventory.getCategory(item.parentUuid))
                .takeIf { it != 0 } ?: strAttachPt.ifEmpty { null }?.let { getAttachPointIndex(it) } ?: 0
        }
    }

    fun hasAttachPointName(item: LLInventoryItem?): Boolean = getAttachPointIndex(item) != 0

    private fun getAttachPointIndexLegacy(folder: LLInventoryCategory): Int {
        val name = folder.name
        if (name.isEmpty()) return 0
        var strAttachPt = extractFirstParenthesisedText(name)
        if (strAttachPt != null) {
            val idxMatch = name.indexOf("($strAttachPt)")
            if (idxMatch != 0 && (idxMatch != 1 || name[0] != '.') &&
                idxMatch + strAttachPt.length + 2 != name.length
            ) {
                strAttachPt = extractLastParenthesisedText(name)
            }
        } else {
            strAttachPt = if (name.isNotEmpty() && name[0] == '.') name.substring(1) else name
        }
        return getAttachPointIndex(strAttachPt ?: "")
    }

    private fun extractFirstParenthesisedText(s: String): String? {
        val start = s.indexOf('(') + 1
        val end = s.indexOf(')')
        return if (start in 1..end) s.substring(start, end) else null
    }

    private fun extractLastParenthesisedText(s: String): String? {
        val end = s.lastIndexOf(')')
        val start = s.lastIndexOf('(', end) + 1
        return if (start in 1..end) s.substring(start, end) else null
    }
}

// Extension to check modifiability
private val LLInventoryItem.isModifiable: Boolean
    get() = TODO("APR: use JVM equivalent — check if agent has modify permission on item")

// ============================================================================
// RlvAttachmentLocks
//

class RlvAttachmentLocks {

    private val attachPtAdd: MutableMap<Int, MutableList<UUID>> = mutableMapOf()
    private val attachPtRem: MutableMap<Int, MutableList<UUID>> = mutableMapOf()
    private val attachObjRem: MutableMap<UUID, MutableList<UUID>> = mutableMapOf()

    var hasLockedHUD: Boolean = false
        private set

    fun addAttachmentLock(idAttachObj: UUID, idRlvObj: UUID) {
        attachObjRem.getOrPut(idAttachObj) { mutableListOf() }.add(idRlvObj)
        updateLockedHUD()
    }

    fun addAttachmentPointLock(idxAttachPt: Int, idRlvObj: UUID, eLock: ERlvLockMask) {
        if (eLock.bits and ERlvLockMask.RLV_LOCK_REMOVE.bits != 0) {
            attachPtRem.getOrPut(idxAttachPt) { mutableListOf() }.add(idRlvObj)
            updateLockedHUD()
        }
        if (eLock.bits and ERlvLockMask.RLV_LOCK_ADD.bits != 0) {
            attachPtAdd.getOrPut(idxAttachPt) { mutableListOf() }.add(idRlvObj)
        }
    }

    fun removeAttachmentLock(idAttachObj: UUID, idRlvObj: UUID) {
        val list = attachObjRem[idAttachObj] ?: return
        list.remove(idRlvObj)
        if (list.isEmpty()) attachObjRem.remove(idAttachObj)
        updateLockedHUD()
    }

    fun removeAttachmentPointLock(idxAttachPt: Int, idRlvObj: UUID, eLock: ERlvLockMask) {
        if (eLock.bits and ERlvLockMask.RLV_LOCK_REMOVE.bits != 0) {
            attachPtRem[idxAttachPt]?.let { list ->
                list.remove(idRlvObj)
                if (list.isEmpty()) attachPtRem.remove(idxAttachPt)
            }
            updateLockedHUD()
        }
        if (eLock.bits and ERlvLockMask.RLV_LOCK_ADD.bits != 0) {
            attachPtAdd[idxAttachPt]?.let { list ->
                list.remove(idRlvObj)
                if (list.isEmpty()) attachPtAdd.remove(idxAttachPt)
            }
        }
    }

    fun hasLockedAttachment(attachPt: LLViewerJointAttachment?): Boolean {
        attachPt ?: return false
        TODO("APR: use JVM equivalent — iterate attached objects on attachment point and check isLockedAttachment")
    }

    fun hasLockedAttachmentPoint(eLock: ERlvLockMask): Boolean =
        (eLock.bits and ERlvLockMask.RLV_LOCK_REMOVE.bits != 0 &&
            (attachPtRem.isNotEmpty() || attachObjRem.isNotEmpty() || RlvFolderLocks.hasLockedAttachment())) ||
            (eLock.bits and ERlvLockMask.RLV_LOCK_ADD.bits != 0 && attachPtAdd.isNotEmpty())

    fun isLockedAttachment(attachObj: Any?): Boolean {
        attachObj ?: return false
        TODO("APR: use JVM equivalent — check attachObjRem map, attachment point RLV_LOCK_REMOVE, and RlvFolderLocks")
    }

    fun isLockedAttachmentExcept(pObj: Any?, idRlvObj: UUID): Boolean {
        if (idRlvObj == UUID(0, 0)) return isLockedAttachment(pObj)
        TODO("APR: use JVM equivalent — check locks on object excluding those owned by idRlvObj")
    }

    fun isLockedAttachmentPoint(idxAttachPt: Int, eLock: ERlvLockMask): Boolean =
        (eLock.bits and ERlvLockMask.RLV_LOCK_REMOVE.bits != 0 && attachPtRem.containsKey(idxAttachPt)) ||
            (eLock.bits and ERlvLockMask.RLV_LOCK_ADD.bits != 0 && attachPtAdd.containsKey(idxAttachPt))

    fun isLockedAttachmentPoint(attachPt: LLViewerJointAttachment?, eLock: ERlvLockMask): Boolean {
        attachPt ?: return false
        return isLockedAttachmentPoint(RlvAttachPtLookup.getAttachPointIndex(attachPt), eLock)
    }

    fun canAttach(): Boolean {
        TODO("APR: use JVM equivalent — return true if any attachment point is not RLV_LOCK_ADD locked")
    }

    fun canAttach(item: LLInventoryItem?, ppAttachPtOut: Array<LLViewerJointAttachment?>? = null): ERlvWearMask {
        val attachPt = RlvAttachPtLookup.getAttachPoint(item)
        ppAttachPtOut?.set(0, attachPt)
        if (!canAttach() || item == null || RlvFolderLocks.isLockedFolder(item.parentUuid, ERlvLockMask.RLV_LOCK_ADD)) {
            return ERlvWearMask.RLV_WEAR_LOCKED
        }
        return if (attachPt == null) ERlvWearMask.RLV_WEAR else canAttach(attachPt)
    }

    fun canAttach(attachPt: LLViewerJointAttachment?): ERlvWearMask {
        attachPt ?: return ERlvWearMask.RLV_WEAR_LOCKED
        if (isLockedAttachmentPoint(attachPt, ERlvLockMask.RLV_LOCK_ADD)) return ERlvWearMask.RLV_WEAR_LOCKED
        val replaceBits = if (canDetach(attachPt, detachAll = true)) ERlvWearMask.RLV_WEAR_REPLACE.bits else 0
        return ERlvWearMask.entries.first { it.bits == (replaceBits or ERlvWearMask.RLV_WEAR_ADD.bits) }
    }

    fun canDetach(item: LLInventoryItem?): Boolean {
        TODO("APR: use JVM equivalent — find worn attachment matching item UUID and check isLockedAttachment")
    }

    fun canDetach(attachPt: LLViewerJointAttachment?, detachAll: Boolean = false): Boolean {
        attachPt ?: return false
        TODO("APR: use JVM equivalent — iterate attached objects; with detachAll=false return true if any unlocked, with detachAll=true return true only if all unlocked")
    }

    fun updateLockedHUD() {
        TODO("APR: use JVM equivalent — scan HUD attachment points for locked attachments and update hasLockedHUD; disable wireframe if locked")
    }

    fun verifyAttachmentLocks(): Boolean {
        val toRemove = attachObjRem.keys.filter { id ->
            TODO("APR: use JVM equivalent — return true if object no longer exists or is not attached")
        }
        toRemove.forEach { attachObjRem.remove(it) }
        return toRemove.isEmpty()
    }

    fun getAttachPtLocks(eLock: ERlvLockMask): Map<Int, List<UUID>> =
        if (eLock == ERlvLockMask.RLV_LOCK_ADD) attachPtAdd else attachPtRem

    fun getAttachObjLocks(): Map<UUID, List<UUID>> = attachObjRem
}

val gRlvAttachmentLocks = RlvAttachmentLocks()

// ============================================================================
// RlvAttachmentLockWatchdog (singleton)
//

object RlvAttachmentLockWatchdog {

    private val pendingDetach: MutableList<UUID> = mutableListOf()
    private val pendingAttach: MutableMap<Int, MutableList<RlvReattachInfo>> = mutableMapOf()
    private val pendingWear: MutableMap<UUID, RlvWearInfo> = mutableMapOf()
    private var timer: Any? = null  // placeholder for LLEventTimer equivalent

    data class RlvReattachInfo(
        val idItem: UUID,
        var assetSaved: Boolean = false,
        val tsDetach: Double = currentTimeSeconds(),
        var tsAttach: Double = 0.0
    )

    data class RlvWearInfo(
        val idItem: UUID,
        val wearAction: ERlvWearMask,
        val tsWear: Double = currentTimeSeconds(),
        val attachPts: MutableMap<Int, MutableList<UUID>> = mutableMapOf()
    ) {
        fun isAddLockedAttachPt(idxAttachPt: Int): Boolean = attachPts.containsKey(idxAttachPt)
    }

    fun onAttach(attachObj: Any, attachPt: LLViewerJointAttachment?) {
        val idxAttachPt = RlvAttachPtLookup.getAttachPointIndex(attachObj)
        if (idxAttachPt == 0) return
        val idAttachItem: UUID = TODO("APR: use JVM equivalent — get attachment item UUID from attachObj")

        val pendingList = pendingAttach[idxAttachPt]
        if (pendingList != null) {
            val reattach = pendingList.firstOrNull { it.idItem == idAttachItem }
            if (reattach != null) {
                RlvBehaviourNotifyHandler.onReattach(attachPt, allowed = true)
                pendingList.remove(reattach)
            } else {
                detach(attachObj)
                RlvBehaviourNotifyHandler.onAttach(attachPt, allowed = false)
            }
            return
        }

        val wearInfo = pendingWear.remove(idAttachItem)
        var attachAllowed = true
        if (wearInfo != null) {
            if (wearInfo.isAddLockedAttachPt(idxAttachPt)) {
                val prevAttachments = wearInfo.attachPts[idxAttachPt] ?: mutableListOf()
                if (!prevAttachments.contains(idAttachItem)) {
                    if (prevAttachments.isEmpty()) {
                        detach(idxAttachPt)
                    } else {
                        TODO("APR: use JVM equivalent — detach current attachments not in prevAttachments, schedule reattach for remaining")
                    }
                    attachAllowed = false
                }
            } else if (wearInfo.wearAction == ERlvWearMask.RLV_WEAR_REPLACE) {
                TODO("APR: use JVM equivalent — handle replace: detach unlocked others, or detach new attachment if a locked one would be displaced")
            }
        }
        RlvBehaviourNotifyHandler.onAttach(attachPt, allowed = attachAllowed)
    }

    fun onDetach(attachObj: Any, attachPt: LLViewerJointAttachment?) {
        val idxAttachPt = RlvAttachPtLookup.getAttachPointIndex(attachPt)
        if (idxAttachPt == 0) return
        val idAttachItem: UUID = TODO("APR: use JVM equivalent — get attachment item UUID from attachObj")

        if (TODO<Boolean>("APR: use JVM equivalent — check FSLSLBridge.canDetach")) return

        val itDetach = pendingDetach.indexOf(idAttachItem)
        if (itDetach >= 0) {
            pendingDetach.removeAt(itDetach)
            RlvBehaviourNotifyHandler.onDetach(attachPt, allowed = true)
            return
        }

        var detachAllowed = true
        if (gRlvAttachmentLocks.isLockedAttachment(attachObj)) {
            val alreadyPending = pendingAttach[idxAttachPt]?.any { it.idItem == idAttachItem } ?: false
            if (!alreadyPending) {
                pendingAttach.getOrPut(idxAttachPt) { mutableListOf() }
                    .add(RlvReattachInfo(idAttachItem))
                startTimer()
            }
            detachAllowed = false
        }
        RlvBehaviourNotifyHandler.onDetach(attachPt, allowed = detachAllowed)
    }

    fun onSavedAssetIntoInventory(idItem: UUID) {
        for ((idxPt, list) in pendingAttach) {
            for (info in list) {
                if (!info.assetSaved && idItem == info.idItem) {
                    TODO("APR: use JVM equivalent — request attachment reattach via LLAttachmentsMgr and update tsAttach")
                }
            }
        }
    }

    fun onTimer(): Boolean {
        val tsCurrent = currentTimeSeconds()

        pendingWear.entries.removeAll { it.value.tsWear + 60 < tsCurrent }

        for ((idxPt, list) in pendingAttach) {
            val iter = list.iterator()
            while (iter.hasNext()) {
                val info = iter.next()
                if (gInventory.getItem(info.idItem) == null) { iter.remove(); continue }
                val doAttach = (!info.assetSaved && info.tsDetach + 15 < tsCurrent) ||
                    (info.assetSaved && info.tsAttach + 30 < tsCurrent)
                if (doAttach) {
                    TODO("APR: use JVM equivalent — request attachment reattach via LLAttachmentsMgr and update tsAttach")
                }
            }
        }

        return pendingAttach.all { it.value.isEmpty() } && pendingDetach.isEmpty() && pendingWear.isEmpty()
    }

    fun onWearAttachment(item: LLInventoryItem, wearAction: ERlvWearMask) =
        onWearAttachment(item.linkedUuid, wearAction)

    fun onWearAttachment(idItem: UUID, wearAction: ERlvWearMask) {
        if (idItem == UUID(0, 0) || !gRlvAttachmentLocks.hasLockedAttachmentPoint(ERlvLockMask.RLV_LOCK_ANY)) return

        val infoWear = RlvWearInfo(idItem, wearAction)
        TODO("APR: use JVM equivalent — populate infoWear.attachPts with current contents of all RLV_LOCK_ADD locked attachment points, then store in pendingWear and start timer")
    }

    private fun detach(attachObj: Any) {
        TODO("APR: use JVM equivalent — send ObjectDetach message for attachObj and add its item ID to pendingDetach")
    }

    private fun detach(idxAttachPt: Int) {
        detach(idxAttachPt, emptyList())
    }

    private fun detach(idxAttachPt: Int, idsAttachObjExcept: List<UUID>) {
        val attachPt = RlvAttachPtLookup.getAttachPoint(idxAttachPt) ?: return
        TODO("APR: use JVM equivalent — send ObjectDetach for all objects on attachPt that are not in idsAttachObjExcept, add their item IDs to pendingDetach")
    }

    private fun startTimer() {
        TODO("APR: use JVM equivalent — schedule periodic onTimer callback (10-second interval) if not already running")
    }

    private fun currentTimeSeconds(): Double = System.currentTimeMillis() / 1000.0
}

// ============================================================================
// RlvWearableLocks
//

class RlvWearableLocks {

    private val wearableTypeAdd: MutableMap<LLWearableType, MutableList<UUID>> = mutableMapOf()
    private val wearableTypeRem: MutableMap<LLWearableType, MutableList<UUID>> = mutableMapOf()

    fun addWearableTypeLock(eType: LLWearableType, idRlvObj: UUID, eLock: ERlvLockMask) {
        if (eLock.bits and ERlvLockMask.RLV_LOCK_REMOVE.bits != 0)
            wearableTypeRem.getOrPut(eType) { mutableListOf() }.add(idRlvObj)
        if (eLock.bits and ERlvLockMask.RLV_LOCK_ADD.bits != 0)
            wearableTypeAdd.getOrPut(eType) { mutableListOf() }.add(idRlvObj)
    }

    fun removeWearableTypeLock(eType: LLWearableType, idRlvObj: UUID, eLock: ERlvLockMask) {
        if (eLock.bits and ERlvLockMask.RLV_LOCK_REMOVE.bits != 0) {
            wearableTypeRem[eType]?.let { it.remove(idRlvObj); if (it.isEmpty()) wearableTypeRem.remove(eType) }
        }
        if (eLock.bits and ERlvLockMask.RLV_LOCK_ADD.bits != 0) {
            wearableTypeAdd[eType]?.let { it.remove(idRlvObj); if (it.isEmpty()) wearableTypeAdd.remove(eType) }
        }
    }

    fun hasLockedWearable(eType: LLWearableType): Boolean {
        TODO("APR: use JVM equivalent — return true if any worn wearable of this type is isLockedWearable")
    }

    fun hasLockedWearableType(eLock: ERlvLockMask): Boolean =
        (eLock.bits and ERlvLockMask.RLV_LOCK_REMOVE.bits != 0 && wearableTypeRem.isNotEmpty()) ||
            (eLock.bits and ERlvLockMask.RLV_LOCK_ADD.bits != 0 && wearableTypeAdd.isNotEmpty())

    fun isLockedWearable(pWearable: Any?): Boolean {
        pWearable ?: return false
        TODO("APR: use JVM equivalent — check wearable type RLV_LOCK_REMOVE lock and RlvFolderLocks")
    }

    fun isLockedWearableExcept(pWearable: Any?, idRlvObj: UUID): Boolean {
        if (idRlvObj == UUID(0, 0)) return isLockedWearable(pWearable)
        TODO("APR: use JVM equivalent — check wearable type lock excluding locks owned by idRlvObj")
    }

    fun canWear(item: LLViewerInventoryItem): ERlvWearMask {
        if (RlvFolderLocks.isLockedFolder(item.parentUuid, ERlvLockMask.RLV_LOCK_ADD)) return ERlvWearMask.RLV_WEAR_LOCKED
        return canWear(item.wearableType)
    }

    fun canWear(eType: LLWearableType): ERlvWearMask {
        if (isLockedWearableType(eType, ERlvLockMask.RLV_LOCK_ADD)) return ERlvWearMask.RLV_WEAR_LOCKED
        return if (!hasLockedWearable(eType)) {
            ERlvWearMask.RLV_WEAR
        } else {
            if (canAddWearable(eType)) ERlvWearMask.RLV_WEAR_ADD else ERlvWearMask.RLV_WEAR_LOCKED
        }
    }

    fun canRemove(item: LLInventoryItem?): Boolean {
        item ?: return false
        TODO("APR: use JVM equivalent — look up worn wearable from item linked UUID and check !isLockedWearable")
    }

    fun canRemove(eType: LLWearableType): Boolean {
        TODO("APR: use JVM equivalent — return true if any worn wearable of this type is not isLockedWearable")
    }

    private fun isLockedWearableType(eType: LLWearableType, eLock: ERlvLockMask): Boolean =
        (eLock.bits and ERlvLockMask.RLV_LOCK_REMOVE.bits != 0 && wearableTypeRem.containsKey(eType)) ||
            (eLock.bits and ERlvLockMask.RLV_LOCK_ADD.bits != 0 && wearableTypeAdd.containsKey(eType))

    private fun isLockedWearableTypeExcept(eType: LLWearableType, eLock: ERlvLockMask, idRlvObj: UUID): Boolean {
        if (idRlvObj == UUID(0, 0)) return isLockedWearableType(eType, eLock)
        if (eLock.bits and ERlvLockMask.RLV_LOCK_REMOVE.bits != 0) {
            if (wearableTypeRem[eType]?.any { it != idRlvObj } == true) return true
        }
        if (eLock.bits and ERlvLockMask.RLV_LOCK_ADD.bits != 0) {
            if (wearableTypeAdd[eType]?.any { it != idRlvObj } == true) return true
        }
        return false
    }

    private fun canAddWearable(eType: LLWearableType): Boolean =
        TODO("APR: use JVM equivalent — check if agent wearables allows adding another wearable of this type")

    fun getWearableTypeLocks(eLock: ERlvLockMask): Map<LLWearableType, List<UUID>> =
        if (eLock == ERlvLockMask.RLV_LOCK_ADD) wearableTypeAdd else wearableTypeRem
}

val gRlvWearableLocks = RlvWearableLocks()

// ============================================================================
// RlvFolderLocks (singleton)
//

object RlvFolderLocks {

    enum class ELockSourceType(val bits: Int) {
        ST_ATTACHMENT(0x01), ST_ATTACHMENTPOINT(0x02), ST_FOLDER(0x04), ST_ROOTFOLDER(0x08),
        ST_SHAREDPATH(0x10), ST_WEARABLETYPE(0x20), ST_NONE(0x00), ST_MASK_ANY(0xFF)
    }

    sealed class LockSource {
        data class ByUuid(val id: UUID) : LockSource()
        data class ByString(val path: String) : LockSource()
        data class ByInt(val index: Int) : LockSource()
        data class ByWearableType(val type: LLWearableType) : LockSource()
    }

    data class FolderLockSource(val type: ELockSourceType, val source: LockSource)

    enum class ELockPermission(val bits: Int) { PERM_ALLOW(0x1), PERM_DENY(0x2), PERM_MASK_ANY(0x3) }
    enum class ELockScope { SCOPE_NODE, SCOPE_SUBTREE }

    data class FolderLockDescr(
        val idRlvObj: UUID,
        val lockType: ERlvLockMask,
        val lockSource: FolderLockSource,
        val lockPermission: ELockPermission,
        val lockScope: ELockScope
    )

    private val folderLocks: MutableList<FolderLockDescr> = mutableListOf()
    private var cntLockAdd: Int = 0
    private var cntLockRem: Int = 0

    private var attachmentChangeSlot: (() -> Unit)? = null

    private var lookupDirty: Boolean = false
    private var rootLockType: Int = ERlvLockMask.RLV_LOCK_NONE.bits
    private val lockedAttachmentRem: MutableList<UUID> = mutableListOf()
    private val lockedFolderMap: MutableMap<UUID, MutableList<FolderLockDescr>> = mutableMapOf()
    private val lockedWearableRem: MutableList<UUID> = mutableListOf()

    init {
        TODO("APR: use JVM equivalent — register onNeedsLookupRefresh with LLOutfitObserver COF-changed and RlvInventory shared-root-changed signals")
    }

    fun addFolderLock(lockSource: FolderLockSource, perm: ELockPermission, scope: ELockScope, idRlvObj: UUID, lockType: ERlvLockMask) {
        require(lockType == ERlvLockMask.RLV_LOCK_ADD || lockType == ERlvLockMask.RLV_LOCK_REMOVE)
        folderLocks.add(FolderLockDescr(idRlvObj, lockType, lockSource, perm, scope))
        if (perm == ELockPermission.PERM_DENY) {
            if (lockType == ERlvLockMask.RLV_LOCK_REMOVE) cntLockRem++
            else if (lockType == ERlvLockMask.RLV_LOCK_ADD) cntLockAdd++
        }
        if (attachmentChangeSlot == null) {
            TODO("APR: use JVM equivalent — register onNeedsLookupRefresh with agent avatar attachment-changed callback")
        }
        lookupDirty = true
    }

    fun removeFolderLock(lockSource: FolderLockSource, perm: ELockPermission, scope: ELockScope, idRlvObj: UUID, lockType: ERlvLockMask) {
        require(lockType == ERlvLockMask.RLV_LOCK_ADD || lockType == ERlvLockMask.RLV_LOCK_REMOVE)
        val descr = FolderLockDescr(idRlvObj, lockType, lockSource, perm, scope)
        val idx = folderLocks.indexOfFirst { it == descr }
        if (idx >= 0) {
            folderLocks.removeAt(idx)
            if (perm == ELockPermission.PERM_DENY) {
                if (lockType == ERlvLockMask.RLV_LOCK_REMOVE) cntLockRem--
                else if (lockType == ERlvLockMask.RLV_LOCK_ADD) cntLockAdd--
            }
            lookupDirty = true
        }
    }

    fun hasLockedAttachment(): Boolean {
        if (lookupDirty) refreshLockedLookups()
        return lockedAttachmentRem.isNotEmpty()
    }

    fun hasLockedFolder(eLock: ERlvLockMask): Boolean =
        (eLock.bits and ERlvLockMask.RLV_LOCK_REMOVE.bits != 0 && cntLockRem > 0) ||
            (eLock.bits and ERlvLockMask.RLV_LOCK_ADD.bits != 0 && cntLockAdd > 0)

    fun hasLockedFolderDescendent(idFolder: UUID, sourceTypeMask: Int, permMask: ELockPermission, lockTypeMask: ERlvLockMask, checkSelf: Boolean): Boolean {
        if (!hasLockedFolder(lockTypeMask)) return false
        if (lookupDirty) refreshLockedLookups()
        if (checkSelf && isLockedFolderEntry(idFolder, sourceTypeMask, permMask, ERlvLockMask.RLV_LOCK_ANY)) return true
        TODO("APR: use JVM equivalent — collect descendant folders and check each with isLockedFolderEntry")
    }

    fun hasLockedWearable(): Boolean {
        if (lookupDirty) refreshLockedLookups()
        return lockedWearableRem.isNotEmpty()
    }

    fun isLockedAttachment(idItem: UUID): Boolean {
        if (lookupDirty) refreshLockedLookups()
        return lockedAttachmentRem.contains(idItem)
    }

    fun isLockedWearable(idItem: UUID): Boolean {
        if (lookupDirty) refreshLockedLookups()
        return lockedWearableRem.contains(idItem)
    }

    fun isLockedFolder(idFolder: UUID, eLockTypeMask: ERlvLockMask, sourceTypeMask: Int = ELockSourceType.ST_MASK_ANY.bits, pLockSourceList: MutableList<FolderLockSource>? = null): Boolean {
        if (!hasLockedFolder(eLockTypeMask)) return false
        val resolvedId = RlvInventory.getFoldedParent(idFolder, checkComposite = true)
        if (resolvedId == UUID(0, 0)) return false
        if (lookupDirty) refreshLockedLookups()

        val idFolderRoot = gInventory.rootFolderId
        val idsRlvObjRem: MutableList<UUID> = mutableListOf()
        val idsRlvObjAdd: MutableList<UUID> = mutableListOf()
        var cur = resolvedId

        while (idFolderRoot != cur) {
            for (descr in (lockedFolderMap[cur] ?: emptyList())) {
                val curLockType = ERlvLockMask.fromBits(descr.lockType.bits and eLockTypeMask.bits)
                val ownerList = if (curLockType == ERlvLockMask.RLV_LOCK_REMOVE) idsRlvObjRem else idsRlvObjAdd
                if (curLockType.bits == 0 ||
                    (descr.lockScope == ELockScope.SCOPE_NODE && resolvedId != cur) ||
                    ownerList.contains(descr.idRlvObj) ||
                    (descr.lockType.bits and sourceTypeMask == 0)
                ) continue

                if (descr.lockPermission == ELockPermission.PERM_DENY) {
                    pLockSourceList?.add(descr.lockSource) ?: return true
                } else if (descr.lockPermission == ELockPermission.PERM_ALLOW) {
                    ownerList.add(descr.idRlvObj)
                }
            }
            val parent = gInventory.getCategory(cur) ?: break
            cur = parent.parentUuid.let { if (it != UUID(0, 0)) it else idFolderRoot }
        }

        if (pLockSourceList != null && pLockSourceList.isNotEmpty()) {
            pLockSourceList.sortBy { it.type.bits }
            return true
        }
        return (rootLockType and eLockTypeMask.bits != 0) &&
            (sourceTypeMask and ELockSourceType.ST_ROOTFOLDER.bits != 0) &&
            idsRlvObjRem.isEmpty() && idsRlvObjAdd.isEmpty()
    }

    fun canMoveFolder(idFolder: UUID, idFolderDest: UUID): Boolean {
        val srcLocks = mutableListOf<FolderLockSource>()
        val dstLocks = mutableListOf<FolderLockSource>()
        return !hasLockedFolderDescendent(idFolder, ELockSourceType.ST_MASK_ANY.bits, ELockPermission.PERM_MASK_ANY, ERlvLockMask.RLV_LOCK_ANY, checkSelf = true) &&
            (isLockedFolder(idFolder, ERlvLockMask.RLV_LOCK_ANY, ELockSourceType.ST_MASK_ANY.bits, srcLocks) ==
                isLockedFolder(idFolderDest, ERlvLockMask.RLV_LOCK_ANY, ELockSourceType.ST_MASK_ANY.bits, dstLocks)) &&
            srcLocks == dstLocks
    }

    fun canRemoveFolder(idFolder: UUID): Boolean =
        !hasLockedFolderDescendent(idFolder, ELockSourceType.ST_MASK_ANY.bits, ELockPermission.PERM_MASK_ANY, ERlvLockMask.RLV_LOCK_ANY, checkSelf = true) &&
            !isLockedFolder(idFolder, ERlvLockMask.RLV_LOCK_ANY, ELockSourceType.ST_MASK_ANY.bits and ELockSourceType.ST_ROOTFOLDER.bits.inv())

    fun canRenameFolder(idFolder: UUID): Boolean =
        !hasLockedFolderDescendent(
            idFolder,
            ELockSourceType.ST_SHAREDPATH.bits or ELockSourceType.ST_ATTACHMENT.bits or
                ELockSourceType.ST_ATTACHMENTPOINT.bits or ELockSourceType.ST_WEARABLETYPE.bits,
            ELockPermission.PERM_MASK_ANY,
            ERlvLockMask.RLV_LOCK_ANY,
            checkSelf = true
        )

    fun canMoveItem(idItem: UUID, idFolderDest: UUID): Boolean {
        val item = gInventory.getItem(idItem)
        val idFolder = item?.parentUuid ?: return false
        val mask = ELockSourceType.ST_MASK_ANY.bits and ELockSourceType.ST_ROOTFOLDER.bits.inv()
        val srcLocks = mutableListOf<FolderLockSource>()
        val dstLocks = mutableListOf<FolderLockSource>()
        return (isLockedFolder(idFolder, ERlvLockMask.RLV_LOCK_ANY, mask, srcLocks) ==
            isLockedFolder(idFolderDest, ERlvLockMask.RLV_LOCK_ANY, mask, dstLocks)) &&
            srcLocks == dstLocks
    }

    fun canRemoveItem(idItem: UUID): Boolean {
        val item = gInventory.getItem(idItem)
        val idFolder = item?.parentUuid ?: return false
        val mask = ELockSourceType.ST_MASK_ANY.bits and ELockSourceType.ST_ROOTFOLDER.bits.inv()
        return !isLockedFolder(idFolder, ERlvLockMask.RLV_LOCK_ANY, mask)
    }

    fun canRenameItem(idItem: UUID): Boolean = true

    private fun isLockedFolderEntry(idFolder: UUID, sourceTypeMask: Int, permMask: ELockPermission, lockTypeMask: ERlvLockMask): Boolean {
        for (descr in (lockedFolderMap[idFolder] ?: return false)) {
            if (descr.lockSource.type.bits and sourceTypeMask != 0 &&
                descr.lockPermission.bits and permMask.bits != 0 &&
                descr.lockType.bits and lockTypeMask.bits != 0
            ) return true
        }
        return false
    }

    private fun onNeedsLookupRefresh() {
        if (folderLocks.isNotEmpty()) lookupDirty = true
    }

    private fun refreshLockedLookups() {
        rootLockType = ERlvLockMask.RLV_LOCK_NONE.bits
        lockedFolderMap.clear()

        for (descr in folderLocks) {
            val locked = getLockedFolders(descr.lockSource)
            val idFolderRoot = gInventory.rootFolderId
            for (folder in locked) {
                if (folder.uuid != idFolderRoot) {
                    lockedFolderMap.getOrPut(folder.uuid) { mutableListOf() }.add(descr)
                } else if (descr.lockScope == ELockScope.SCOPE_SUBTREE) {
                    rootLockType = rootLockType or descr.lockType.bits
                }
            }
        }
        lookupDirty = false

        lockedAttachmentRem.clear()
        lockedWearableRem.clear()
        TODO("APR: use JVM equivalent — iterate COF worn items, check isLockedFolder on their (folded) parent, split into lockedAttachmentRem / lockedWearableRem by asset type, then de-duplicate both lists")
    }

    private fun getLockedFolders(lockSource: FolderLockSource): List<LLViewerInventoryCategory> {
        TODO("APR: use JVM equivalent — resolve lock source (attachment UUID, attachment point index, folder UUID, shared path, wearable type, root) to list of inventory categories")
    }

    fun getFolderLocks(): List<FolderLockDescr> = folderLocks
    fun getAttachmentLookups(): List<UUID> = lockedAttachmentRem
    fun getWearableLookups(): List<UUID> = lockedWearableRem
}

// ============================================================================
// Stubs for referenced types (implemented elsewhere in the Kotlin port)
//

enum class LLWearableType { WT_SHAPE, WT_SKIN, WT_HAIR, WT_EYES, WT_SHIRT, WT_PANTS }

class LLViewerInventoryItem(
    val uuid: UUID,
    val parentUuid: UUID,
    val name: String,
    val type: LLAssetType,
    val actualType: LLAssetType,
    val isLinkType: Boolean,
    val linkedUuid: UUID,
    val wearableType: LLWearableType = LLWearableType.WT_SHIRT
)

object RlvBehaviourNotifyHandler {
    fun onAttach(attachPt: LLViewerJointAttachment?, allowed: Boolean) { TODO("APR: use JVM equivalent") }
    fun onDetach(attachPt: LLViewerJointAttachment?, allowed: Boolean) { TODO("APR: use JVM equivalent") }
    fun onReattach(attachPt: LLViewerJointAttachment?, allowed: Boolean) { TODO("APR: use JVM equivalent") }
}

object RlvActions {
    fun canChangeEnvironment(idObj: UUID = UUID(0, 0)): Boolean = TODO("APR: use JVM equivalent")
}

class ERlvCmdRet
class RlvCommand(
    val behaviour: String,
    val paramType: ERlvParamType,
    val behaviourType: ERlvBehaviour,
    val option: String,
    val param: String,
    val objectId: UUID
) {
    fun hasOption(): Boolean = option.isNotEmpty()
    fun asString(): String = TODO("APR: use JVM equivalent")
}
enum class ERlvParamType { RLV_TYPE_ADD, RLV_TYPE_REMOVE, RLV_TYPE_FORCE, RLV_TYPE_REPLY }
enum class ERlvBehaviour {
    RLV_BHVR_DETACH, RLV_BHVR_ADDATTACH, RLV_BHVR_REMATTACH, RLV_BHVR_ADDOUTFIT, RLV_BHVR_REMOUTFIT,
    RLV_BHVR_RECVCHAT, RLV_BHVR_RECVEMOTE, RLV_BHVR_SENDIM, RLV_BHVR_RECVIM, RLV_BHVR_STARTIM,
    RLV_BHVR_TPLURE, RLV_BHVR_TPREQUEST, RLV_BHVR_ACCEPTTP, RLV_BHVR_ACCEPTTPREQUEST,
    RLV_BHVR_SHOWNAMES, RLV_BHVR_SHOWNAMETAGS
}
object RlvHandler {
    val instance: RlvHandler = this
    val objectMap: Map<UUID, RlvObjectEntry> get() = TODO("APR: use JVM equivalent")
    fun setCommandCallback(cb: (RlvCommand, ERlvCmdRet) -> Unit): (() -> Unit) = TODO("APR: use JVM equivalent")
}
class RlvObjectEntry { val commandList: List<RlvCommand> get() = TODO("APR: use JVM equivalent") }
object RlvStrings {
    fun getVersion(id: UUID?): String = TODO("APR: use JVM equivalent")
}
object RlvUtil { fun sendChatReply(param: String, reply: String): Boolean = TODO("APR: use JVM equivalent") }
