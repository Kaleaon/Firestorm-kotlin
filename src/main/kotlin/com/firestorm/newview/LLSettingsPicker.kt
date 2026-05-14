package com.firestorm.newview

import java.util.UUID

// ---- UI stub types (would come from the UI framework port) ----

typealias PermissionMask = UInt

abstract class LLView {
    abstract fun getHandle(): Any
    open fun setFocus(focus: Boolean) {}
    open fun setEnabled(enabled: Boolean) {}
    open fun setVisible(visible: Boolean) {}
}

abstract class LLFloater : LLView() {
    var mCommitSignal: ((LLFloater, Map<String, Any>) -> Unit)? = null
    protected open val mViewModel: ViewModel = ViewModel()
    open fun postBuild(): Boolean = true
    open fun onClose(appQuitting: Boolean) {}
    open fun draw() {}
    open fun setValue(value: Any) {}
    open fun getValue(): Any = Unit
    open fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean = false
    open fun handleKeyHere(key: Int, mask: Int): Boolean = false
    open fun onFocusLost() {}
    fun closeFloater() { System.err.println("LLFloater: closeFloater not yet implemented") }
    fun isInVisibleChain(): Boolean { return false }
    fun getHandle(): Any { System.err.println("LLFloater: getHandle not yet implemented"); return Any() }

    class ViewModel {
        fun setDirty() {}
    }
}

abstract class LLInventoryPanel : LLView() {
    abstract fun setFilterTypes(types: UInt)
    abstract fun setFilterPermMask(mask: PermissionMask)
    abstract fun setSelectCallback(cb: (List<LLFolderViewItem>, Boolean) -> Unit)
    abstract fun setShowFolderState(state: Int)
    abstract fun setSuppressOpenItemAction(suppress: Boolean)
    abstract fun getRootFolder(): LLFolderView
    abstract fun setSelection(id: UUID, takeFocus: Boolean)
    abstract fun clearSelection()
    abstract fun getFilterSubString(): String
    abstract fun setFilterSubString(s: String)
    abstract fun getFilter(): LLInventoryFilter
    abstract fun setFilterSettingsTypes(filter: Long)
    abstract fun getItemByID(id: UUID): LLFolderViewItem?
}

abstract class LLFolderView : LLView() {
    abstract fun setAutoSelectOverride(v: Boolean)
    abstract fun applyFunctorRecursively(functor: Any)
    abstract fun scrollToShowSelection()
    abstract fun clearSelection()
}

abstract class LLFolderViewItem : LLView() {
    abstract fun getViewModelItem(): Any?
    abstract fun getIsCurSelection(): Boolean
    abstract fun getLocalRect(): LLRect
    abstract fun localRectToOtherView(rect: LLRect, out: LLRect, view: LLView)
}

class LLRect {
    var mLeft: Int = 0; var mRight: Int = 0; var mBottom: Int = 0; var mTop: Int = 0
    fun pointInRect(x: Int, y: Int) = x in mLeft..mRight && y in mBottom..mTop
}

abstract class LLInventoryFilter {
    abstract fun isNotDefault(): Boolean
    companion object { const val SHOW_NON_EMPTY_FOLDERS = 1 }
}

abstract class LLFilterEditor : LLView() {
    abstract fun setCommitCallback(cb: (Any?, Map<String, Any>) -> Unit)
}

abstract class LLInventoryItem {
    abstract fun getUUID(): UUID
    abstract fun getName(): String
    abstract fun getAssetUUID(): UUID
    abstract fun getPermissions(): LLPermissions
}

abstract class LLPermissions {
    abstract fun allowCopyBy(agentId: UUID): Boolean
    abstract fun allowCopyBy(agentId: UUID, groupId: UUID): Boolean
}

class LLSaveFolderState {
    fun setApply(apply: Boolean) { System.err.println("LLSaveFolderState: setApply not yet implemented") }
}

abstract class LLItemBridge {
    abstract fun getItem(): LLInventoryItem?
    abstract fun isItemCopyable(): Boolean
}

abstract class LLComboBox : LLView() {
    abstract fun clear()
    abstract fun removeall()
    abstract fun add(label: String, value: Any, pos: Int, selectIfUnique: Boolean)
    abstract fun selectFirstItem()
    abstract fun getValue(): Any
    companion object { const val ADD_TOP = 0 }
}

abstract class LLPanel : LLView()

// stub object references filled by caller
object gInventory {
    fun getItem(id: UUID): LLInventoryItem? { System.err.println("gInventory: getItem not yet implemented"); return null }
    fun isObjectDescendentOf(id: UUID, parentId: UUID): Boolean { System.err.println("gInventory: isObjectDescendentOf not yet implemented"); return false }
    fun getLibraryRootFolderID(): UUID { System.err.println("gInventory: getLibraryRootFolderID not yet implemented"); return UUID(0, 0) }
    fun collectDescendentsIf(folderId: UUID, cats: MutableList<*>, items: MutableList<*>, includeTrash: Int, matcher: Any) {
        System.err.println("gInventory: collectDescendentsIf not yet implemented")
    }
}

object gAgent {
    fun getID(): UUID { System.err.println("gAgent: getID not yet implemented"); return UUID(0, 0) }
    fun getGroupID(): UUID { System.err.println("gAgent: getGroupID not yet implemented"); return UUID(0, 0) }
}

// ---- Track mode and type stubs ----

object LLSettingsType {
    enum class TypeE { ST_NONE, ST_SKY, ST_WATER, ST_DAYCYCLE }
    typealias type_e = TypeE
}

abstract class LLSettingsBase
abstract class LLSettingsDay : LLSettingsBase() {
    abstract fun isTrackEmpty(trackNum: Int): Boolean
    companion object {
        const val TRACK_WATER = 0
        const val TRACK_GROUND_LEVEL = 1
        const val TRACK_MAX = 5
    }
}

// ---- LLFloaterSettingsPicker ----

class LLFloaterSettingsPicker(
    owner: LLView,
    initialItemId: UUID,
    params: Map<String, Any> = emptyMap()
) : LLFloater() {

    enum class ETrackMode { TRACK_NONE, TRACK_WATER, TRACK_SKY }

    private val ownerHandle: Any = owner.getHandle()
    private var settingItemID: UUID = initialItemId
    private var settingAssetID: UUID = UUID(0, 0)
    private var trackMode: ETrackMode = ETrackMode.TRACK_NONE
    private var filterEdit: LLFilterEditor? = null
    private var inventoryPanel: LLInventoryPanel? = null
    private var settingsType: LLSettingsType.type_e = LLSettingsType.TypeE.ST_NONE
    private var contextConeOpacity: Float = 0.0f
    private var immediateFilterPermMask: PermissionMask = 0u
    private var active: Boolean = true
    private var noCopySettingsSelected: Boolean = false
    private val savedFolderState: LLSaveFolderState = LLSaveFolderState()

    // boost::signals2 → callback lists
    val closeSignal: MutableList<() -> Unit> = mutableListOf()
    val changeIdSignal: MutableList<(UUID) -> Unit> = mutableListOf()

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        System.err.println("LLFloaterSettingsPicker: postBuild not yet implemented")
        return false
    }

    override fun onClose(appQuitting: Boolean) {
        if (appQuitting) return
        closeSignal.forEach { it() }
        System.err.println("LLFloaterSettingsPicker: onClose not yet implemented")
        settingItemID = UUID(0, 0)
    }

    override fun draw() {
        // no-op
    }

    override fun setValue(value: Any) {
        settingItemID = value as UUID
    }

    override fun getValue(): Any = settingItemID

    fun setActive(active: Boolean) { this.active = active }

    fun getSettingsItemId(): UUID = settingItemID

    fun setSettingsFilter(type: LLSettingsType.type_e) {
        this.settingsType = type
        System.err.println("LLFloaterSettingsPicker: setSettingsFilter not yet implemented")
    }

    fun getSettingsFilter(): LLSettingsType.type_e = settingsType

    fun setTrackMode(mode: ETrackMode) {
        trackMode = mode
        System.err.println("LLFloaterSettingsPicker: setTrackMode not yet implemented")
    }

    fun setTrackWater() { setTrackMode(ETrackMode.TRACK_WATER) }
    fun setTrackSky()   { setTrackMode(ETrackMode.TRACK_SKY) }

    private fun onFilterEdit(searchString: String) {
        System.err.println("LLFloaterSettingsPicker: onFilterEdit not yet implemented")
    }

    private fun onSelectionChange(items: List<LLFolderViewItem>, userAction: Boolean) {
        if (items.isEmpty()) return
        val first = items.first()
        val bridge = first.getViewModelItem() as? LLItemBridge ?: return
        val item = bridge.getItem() ?: return
        noCopySettingsSelected = !bridge.isItemCopyable()
        setSettingsItemId(item.getUUID(), false)
        val assetId = item.getAssetUUID()
        mViewModel.setDirty()
        if (userAction) changeIdSignal.forEach { it(settingItemID) }

        val trackPickerEnabled = trackMode != ETrackMode.TRACK_NONE
        System.err.println("LLFloaterSettingsPicker: onSelectionChange not yet implemented")
    }

    private fun onAssetLoaded(assetId: UUID, settings: LLSettingsBase?) {
        System.err.println("LLFloaterSettingsPicker: onAssetLoaded not yet implemented")
    }

    private fun onButtonCancel() = closeFloater()

    private fun onButtonSelect() = applySelectedItemAndCloseFloater()

    private fun applySelectedItemAndCloseFloater() {
        mCommitSignal?.let { signal ->
            val res = mutableMapOf<String, Any>(
                "ItemId" to settingItemID,
                "Track" to 0
            )
            signal(this, res)
        }
        closeFloater()
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("LLFloaterSettingsPicker: handleDoubleClick not yet implemented")
        return false
    }

    override fun handleKeyHere(key: Int, mask: Int): Boolean {
        System.err.println("LLFloaterSettingsPicker: handleKeyHere not yet implemented")
        return false
    }

    override fun onFocusLost() {
        if (isInVisibleChain()) closeFloater()
    }

    fun setSettingsItemId(settingsId: UUID, setSelection: Boolean = true) {
        if (settingItemID == settingsId || !active) return
        noCopySettingsSelected = false
        mViewModel.setDirty()
        settingItemID = settingsId
        if (settingItemID == UUID(0, 0)) {
            inventoryPanel?.getRootFolder()?.clearSelection()
        } else {
            val itemp = gInventory.getItem(settingsId)
            if (itemp != null && !itemp.getPermissions().allowCopyBy(gAgent.getID())) {
                noCopySettingsSelected = true
            }
        }
        if (setSelection) {
            inventoryPanel?.setSelection(settingsId, false)
        }
    }

    companion object {
        fun findItemId(assetId: UUID, copyableOnly: Boolean, ignoreLibrary: Boolean = false): UUID {
            return findItem(assetId, copyableOnly, ignoreLibrary)?.getUUID() ?: UUID(0, 0)
        }

        fun findItemName(assetId: UUID, copyableOnly: Boolean, ignoreLibrary: Boolean = false): String {
            return findItem(assetId, copyableOnly, ignoreLibrary)?.getName() ?: ""
        }

        fun findItem(assetId: UUID, copyableOnly: Boolean, ignoreLibrary: Boolean): LLInventoryItem? {
            if (assetId == UUID(0, 0)) return null
            System.err.println("LLFloaterSettingsPicker: findItem not yet implemented")
            return null
        }
    }
}
