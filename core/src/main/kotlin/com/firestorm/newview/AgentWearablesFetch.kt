package com.firestorm.newview

import com.firestorm.agent.Agent
import com.firestorm.agent.AgentWearables
import com.firestorm.appearance.AppearanceMgr
import com.firestorm.app.AppViewer
import com.firestorm.avatar.AvatarSelf
import com.firestorm.inventory.FetchDescendentsObserver
import com.firestorm.inventory.FetchItemsObserver
import com.firestorm.inventory.InventoryCallback
import com.firestorm.inventory.InventoryModel
import com.firestorm.inventory.FolderType
import com.firestorm.startup.StartUp
import com.firestorm.wearable.WearableType
import java.util.UUID

private fun orderMyOutfits() {
    if (!AppViewer.instance.isRunning()) return

    val myOutfitsId = InventoryModel.instance.findCategoryUUIDForType(FolderType.FT_MY_OUTFITS)
    if (myOutfitsId == null || myOutfitsId == UUID(0, 0)) return

    val cats = InventoryModel.instance.getDirectDescendentCats(myOutfitsId) ?: return
    if (cats.size < 2) return

    for (outfit in cats) {
        val catId = outfit.getUUID()
        if (catId == UUID(0, 0)) continue
        if (catId == AppearanceMgr.instance.getBaseOutfitUUID()) continue
        AppearanceMgr.instance.updateClothingOrderingInfo(catId)
    }
}

class InitialWearablesFetch(cofId: UUID) : FetchDescendentsObserver(cofId) {

    data class InitialWearableData(
        val type: WearableType.EType,
        val itemId: UUID,
        val assetId: UUID,
    )

    private val agentInitialWearables: MutableList<InitialWearableData> = mutableListOf()

    init {
        AvatarSelf.instance?.let {
            it.startPhase("initial_wearables_fetch")
            it.outputRezTiming("Initial wearables fetch started")
        }
    }

    override fun done() {
        InventoryModel.instance.removeObserver(this)
        // Deferred to the next idle tick to avoid reentrant notifyObservers calls.
        InventoryModel.instance.doOnIdleOneTime { processContents() }
        AvatarSelf.instance?.let {
            it.stopPhase("initial_wearables_fetch")
            it.outputRezTiming("Initial wearables fetch done")
        }
    }

    fun add(data: InitialWearableData) {
        agentInitialWearables.add(data)
    }

    private fun processContents() {
        if (AvatarSelf.instance == null) return

        val completeIds = getComplete()
        if (completeIds.isEmpty()) return

        val wearables = InventoryModel.instance.collectWearableDescendents(
            completeIds.first(),
            excludeTrash = true,
        )

        AppearanceMgr.instance.setAttachmentInvLinkEnable(true)
        if (wearables.isNotEmpty()) {
            AgentWearables.instance.notifyLoadingStarted()
            AppearanceMgr.instance.updateAppearanceFromCOF()
        } else {
            AppearanceMgr.instance.setOutfitDirty(true)
            processWearablesMessage()
        }
    }

    private fun processWearablesMessage() {
        if (agentInitialWearables.isEmpty()) return

        val ids = mutableListOf<UUID>()
        for (wearableData in agentInitialWearables) {
            if (wearableData.assetId != UUID(0, 0)) {
                ids.add(wearableData.itemId)
            }
        }

        // Collect current attachment item IDs so they are linked into the COF as well.
        AvatarSelf.instance?.mAttachmentPoints?.values?.forEach { attachment ->
            attachment?.mAttachedObjects?.forEach { obj ->
                val itemId = obj?.getAttachmentItemID() ?: return@forEach
                if (itemId != UUID(0, 0)) ids.add(itemId)
            }
        }

        val fetcher = FetchAndLinkObserver(ids)
        fetcher.startFetch()
        if (fetcher.isFinished()) {
            fetcher.done()
        } else {
            InventoryModel.instance.addObserver(fetcher)
        }
    }
}

private class FetchAndLinkObserver(ids: MutableList<UUID>) : FetchItemsObserver(ids) {
    override fun done() {
        InventoryModel.instance.removeObserver(this)
        val linkWaiter: InventoryCallback = UpdateAppearanceOnDestroy()
        val itemArray = mutableListOf<Any>()
        for (id in getIDs()) {
            val item = InventoryModel.instance.getItem(id) ?: continue
            itemArray.add(item)
        }
        InventoryModel.instance.linkInventoryArray(AppearanceMgr.instance.getCOF(), itemArray, linkWaiter)
    }
}

class LibraryOutfitsFetch(myOutfitsId: UUID) : FetchDescendentsObserver(myOutfitsId) {

    enum class LibraryOutfitFetchStep {
        LOFS_FOLDER,
        LOFS_OUTFITS,
        LOFS_LIBRARY,
        LOFS_IMPORTED,
        LOFS_CONTENTS,
    }

    var myOutfitsID: UUID = UUID(0, 0)
    private var currFetchStep: LibraryOutfitFetchStep = LibraryOutfitFetchStep.LOFS_FOLDER
    private val libraryClothingFolders: MutableList<UUID> = mutableListOf()
    private val importedClothingFolders: MutableList<UUID> = mutableListOf()
    private var outfitsPopulated: Boolean = false
    private var clothingId: UUID = UUID(0, 0)
    private var libraryClothingId: UUID = UUID(0, 0)
    private var importedClothingId: UUID = UUID(0, 0)
    private val importedClothingName: String = "Imported Library Clothing"

    override fun done() {
        // Deferred to idle to avoid running inside notifyObservers and to spread the heavy work.
        InventoryModel.instance.removeObserver(this)
        InventoryModel.instance.doOnIdleOneTime { doneIdle() }
    }

    fun doneIdle() {
        InventoryModel.instance.addObserver(this)

        when (currFetchStep) {
            LibraryOutfitFetchStep.LOFS_FOLDER -> {
                folderDone()
                currFetchStep = LibraryOutfitFetchStep.LOFS_OUTFITS
            }
            LibraryOutfitFetchStep.LOFS_OUTFITS -> {
                outfitsDone()
                currFetchStep = LibraryOutfitFetchStep.LOFS_LIBRARY
            }
            LibraryOutfitFetchStep.LOFS_LIBRARY -> {
                libraryDone()
                currFetchStep = LibraryOutfitFetchStep.LOFS_IMPORTED
            }
            LibraryOutfitFetchStep.LOFS_IMPORTED -> {
                importedFolderDone()
                currFetchStep = LibraryOutfitFetchStep.LOFS_CONTENTS
            }
            LibraryOutfitFetchStep.LOFS_CONTENTS -> {
                contentsDone()
            }
        }

        if (outfitsPopulated) {
            InventoryModel.instance.removeObserver(this)
        }
    }

    private fun folderDone() {
        val cats = InventoryModel.instance.collectDescendentCats(myOutfitsID, excludeTrash = true)
        if (cats.size > 1) {
            outfitsPopulated = true
            return
        }

        clothingId = InventoryModel.instance.findCategoryUUIDForType(FolderType.FT_CLOTHING) ?: UUID(0, 0)
        libraryClothingId = InventoryModel.instance.findLibraryCategoryUUIDForType(FolderType.FT_CLOTHING) ?: UUID(0, 0)

        val initialOutfitFolders = InventoryModel.instance.collectDescendentCatsNamed(
            libraryClothingId, "Initial Outfits", excludeTrash = true,
        )
        if (initialOutfitFolders.isNotEmpty()) {
            libraryClothingId = initialOutfitFolders[0].getUUID()
        }

        clearComplete()
        setFetchIDs(mutableListOf(clothingId, libraryClothingId))
        startFetch()
        if (isFinished()) done()
    }

    private fun outfitsDone() {
        val cats = InventoryModel.instance.collectDescendentCats(libraryClothingId, excludeTrash = true)
        val folders = mutableListOf<UUID>()
        for (cat in cats) {
            if (cat.getName() != "Ruth") {
                folders.add(cat.getUUID())
                libraryClothingFolders.add(cat.getUUID())
            }
        }

        val imported = InventoryModel.instance.collectDescendentCatsNamed(
            clothingId, importedClothingName, excludeTrash = true,
        )
        if (imported.isNotEmpty()) {
            importedClothingId = imported[0].getUUID()
        }

        clearComplete()
        setFetchIDs(folders)
        startFetch()
        if (isFinished()) done()
    }

    private fun libraryDone() {
        if (importedClothingId != UUID(0, 0)) {
            importedFolderFetch()
            return
        }

        InventoryModel.instance.removeObserver(this)
        val copyWaiter = LibraryOutfitsCopyDone(this)

        InventoryModel.instance.createNewCategory(
            clothingId, FolderType.FT_NONE, importedClothingName,
        ) { newCatId ->
            importedClothingId = newCatId
            for (srcFolderId in libraryClothingFolders) {
                val cat = InventoryModel.instance.getCategory(srcFolderId) ?: continue
                if (!AppearanceMgr.instance.getCanMakeFolderIntoOutfit(srcFolderId)) continue

                val existing = InventoryModel.instance.collectDescendentCatsNamed(
                    importedClothingId, cat.getName(), excludeTrash = true,
                )
                if (existing.isNotEmpty()) continue

                InventoryModel.instance.createNewCategory(
                    importedClothingId, FolderType.FT_NONE, cat.getName(),
                ) { newSubCatId ->
                    AppearanceMgr.instance.shallowCopyCategoryContents(srcFolderId, newSubCatId, copyWaiter)
                }
            }
        }
    }

    fun importedFolderFetch() {
        val folders = mutableListOf(importedClothingId)
        clearComplete()
        setFetchIDs(folders)
        startFetch()
        if (isFinished()) done()
    }

    private fun importedFolderDone() {
        val cats = InventoryModel.instance.collectDescendentCats(importedClothingId, excludeTrash = true)
        val folders = mutableListOf<UUID>()
        for (cat in cats) {
            folders.add(cat.getUUID())
            importedClothingFolders.add(cat.getUUID())
        }
        clearComplete()
        setFetchIDs(folders)
        startFetch()
        if (isFinished()) done()
    }

    private fun contentsDone() {
        val orderOnDestroy = BoostFuncInventoryCallback(onDestroy = ::orderMyOutfits)

        for (folderId in importedClothingFolders) {
            val cat = InventoryModel.instance.getCategory(folderId) ?: continue
            if (cat.getName() == StartUp.getInitialOutfitName()) continue

            InventoryModel.instance.createNewCategory(
                myOutfitsID, FolderType.FT_OUTFIT, cat.getName(),
            ) { _ ->
                val items = InventoryModel.instance.collectDescendentItems(folderId, excludeTrash = true)
                InventoryModel.instance.linkInventoryArray(AppearanceMgr.instance.getCOF(), items, orderOnDestroy)
            }
        }

        outfitsPopulated = true
    }
}

private class LibraryOutfitsCopyDone(
    private var fetcher: LibraryOutfitsFetch?,
) : InventoryCallback {
    private var fireCount: UInt = 0u

    override fun fire(invItemId: UUID) {
        fireCount++
    }

    // Triggers the next autopopulation step once all copies have been dispatched.
    protected fun finalize() {
        val f = fetcher ?: return
        if (!AppViewer.instance.isExiting()) {
            InventoryModel.instance.addObserver(f)
            f.done()
        }
        fetcher = null
    }
}

private class BoostFuncInventoryCallback(private val onDestroy: () -> Unit) : InventoryCallback {
    override fun fire(invItemId: UUID) {}
    protected fun finalize() { onDestroy() }
}

private class UpdateAppearanceOnDestroy : InventoryCallback {
    override fun fire(invItemId: UUID) {}
    protected fun finalize() { AppearanceMgr.instance.updateAppearanceFromCOF() }
}
