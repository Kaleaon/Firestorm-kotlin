package com.firestorm.newview

import java.util.UUID

interface InventoryObserver {
    fun changed(mask: UInt)
}

object OutfitObserver : InventoryObserver {

    private const val VERSION_UNKNOWN: Int = -1

    private var cofLastVersion: Int = VERSION_UNKNOWN
    private var baseOutfitId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var baseOutfitLastVersion: Int = VERSION_UNKNOWN
    private var lastBaseOutfitName: String = ""
    private var lastOutfitDirtiness: Boolean = false
    private var itemNameHash: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    private val bofReplacedListeners:    MutableList<() -> Unit> = mutableListOf()
    private val bofChangedListeners:     MutableList<() -> Unit> = mutableListOf()
    private val cofChangedListeners:     MutableList<() -> Unit> = mutableListOf()
    private val cofSavedListeners:       MutableList<() -> Unit> = mutableListOf()
    private val outfitLockChangedListeners: MutableList<() -> Unit> = mutableListOf()

    fun init() {
        InventoryModel.addObserver(this)
    }

    fun cleanup() {
        InventoryModel.removeObserver(this)
    }

    override fun changed(mask: UInt) {
        if (!InventoryModel.isInventoryUsable()) return
        checkCOF()
        checkBaseOutfit()
    }

    fun notifyOutfitLockChanged() {
        outfitLockChangedListeners.forEach { it() }
    }

    fun addBOFReplacedCallback(cb: () -> Unit)      { bofReplacedListeners += cb }
    fun addBOFChangedCallback(cb: () -> Unit)        { bofChangedListeners += cb }
    fun addCOFChangedCallback(cb: () -> Unit)        { cofChangedListeners += cb }
    fun addCOFSavedCallback(cb: () -> Unit)          { cofSavedListeners += cb }
    fun addOutfitLockChangedCallback(cb: () -> Unit) { outfitLockChangedListeners += cb }

    private fun getCategoryVersion(catId: UUID): Int {
        return InventoryModel.getCategory(catId)?.getVersion() ?: VERSION_UNKNOWN
    }

    private fun getCategoryName(catId: UUID): String {
        return InventoryModel.getCategory(catId)?.getName() ?: ""
    }

    private fun checkCOF(): Boolean {
        val cof = AppearanceMgr.getCOF()
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (cof == nullId) return false

        var cofChanged = false

        val nameHash = InventoryModel.hashDirectDescendentNames(cof)
        if (nameHash != itemNameHash) {
            cofChanged = true
            itemNameHash = nameHash
        }

        val cofVersion = getCategoryVersion(cof)
        if (cofVersion != cofLastVersion) {
            cofChanged = true
            cofLastVersion = cofVersion
        }

        if (!cofChanged) return false

        AppearanceMgr.updateIsDirty()
        cofChangedListeners.forEach { it() }
        return true
    }

    private fun checkBaseOutfit() {
        val baseOutfitUUID = AppearanceMgr.getBaseOutfitUUID()
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")

        if (baseOutfitUUID == baseOutfitId) {
            if (baseOutfitUUID == nullId) return
            val ver = getCategoryVersion(baseOutfitUUID)
            val name = getCategoryName(baseOutfitUUID)
            if (ver == baseOutfitLastVersion && name == lastBaseOutfitName) return
        } else {
            baseOutfitId = baseOutfitUUID
            bofReplacedListeners.forEach { it() }
            if (baseOutfitUUID == nullId) return
        }

        baseOutfitLastVersion = getCategoryVersion(baseOutfitId)
        lastBaseOutfitName = getCategoryName(baseOutfitUUID)

        AppearanceMgr.updateIsDirty()
        bofChangedListeners.forEach { it() }

        val nowDirty = AppearanceMgr.isOutfitDirty()
        if (lastOutfitDirtiness != nowDirty) {
            if (!nowDirty) {
                cofSavedListeners.forEach { it() }
            }
            lastOutfitDirtiness = nowDirty
        }
    }
}
