package com.firestorm.newview

import java.util.UUID
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val MAX_OBJECTS_PER_PACKET = 255
private const val REFRESH_INTERVAL = 1.0f
private const val MIN_REFRESH_INTERVAL = 0.25f
private const val MIN_DISTANCE_MOVED = 1.0f
private const val REQUEST_TIMEOUT = 30.0f

// ---------------------------------------------------------------------------
// Object-properties request lifecycle enum
// ---------------------------------------------------------------------------

enum class ObjectPropertiesRequest { NEED, SENT, FINISHED, FAILED }

// ---------------------------------------------------------------------------
// Per-object cached data
// ---------------------------------------------------------------------------

data class FSObjectProperties(
    var id: UUID = UUID(0L, 0L),
    var listed: Boolean = false,
    var name: String = "",
    var description: String = "",
    var touchName: String = "",
    var sitName: String = "",
    var creatorId: UUID = UUID(0L, 0L),
    var ownerId: UUID = UUID(0L, 0L),
    var groupId: UUID = UUID(0L, 0L),
    var ownershipId: UUID = UUID(0L, 0L),
    var groupOwned: Boolean = false,
    var creationDate: ULong = 0UL,
    var baseMask: UInt = 0U,
    var ownerMask: UInt = 0U,
    var groupMask: UInt = 0U,
    var everyoneMask: UInt = 0U,
    var nextOwnerMask: UInt = 0U,
    var lastOwnerId: UUID = UUID(0L, 0L),
    var textureIds: MutableList<UUID> = mutableListOf(),
    var nameRequested: Boolean = false,
    var localId: UInt = 0U,
    var regionHandle: ULong = 0UL,
    var request: ObjectPropertiesRequest = ObjectPropertiesRequest.NEED
)

// ---------------------------------------------------------------------------
// Main area-search floater
// LLFloater subclass → open class; virtual methods → open
// ---------------------------------------------------------------------------

open class FSAreaSearch(val key: Any) {

    val objectDetails: MutableMap<UUID, FSObjectProperties> = mutableMapOf()

    var panelAdvanced: FSPanelAreaSearchAdvanced? = null
        protected set
    var panelList: FSPanelAreaSearchList? = null
        protected set

    protected var panelFind: FSPanelAreaSearchFind? = null
    protected var panelFilter: FSPanelAreaSearchFilter? = null
    protected var panelOptions: FSPanelAreaSearchOptions? = null

    // ----- filter/exclusion booleans -----
    var filterForSale: Boolean = false
    var filterLocked: Boolean = false
    var filterPhysical: Boolean = true
    var filterTemporary: Boolean = true
    var filterPhantom: Boolean = false
    var filterAttachment: Boolean = false
    var filterMoaP: Boolean = false
    var filterReflectionProbe: Boolean = false
    var filterForSaleMin: Int = 0
    var filterForSaleMax: Int = 999_999
    var filterClickAction: Boolean = false
    var filterClickActionType: UByte = 0u
    var filterDistance: Boolean = false
    var filterDistanceMin: Int = 0
    var filterDistanceMax: Int = 0
    var filterPermCopy: Boolean = false
    var filterPermModify: Boolean = false
    var filterPermTransfer: Boolean = false
    var filterAgentParcelOnly: Boolean = false

    var excludeAttachment: Boolean = false
    var excludeTemporary: Boolean = false
    var excludeReflectionProbe: Boolean = false
    var excludePhysics: Boolean = false
    var excludeChildPrims: Boolean = true
    var excludeNeighborRegions: Boolean = true

    var beacons: Boolean = false
    var regexSearch: Boolean = false

    // ----- search text -----
    private var searchName: String = ""
    private var searchDescription: String = ""
    private var searchOwner: String = ""
    private var searchGroup: String = ""
    private var searchCreator: String = ""
    private var searchLastOwner: String = ""

    private var regexSearchName: Pattern? = null
    private var regexSearchDescription: Pattern? = null
    private var regexSearchOwner: Pattern? = null
    private var regexSearchGroup: Pattern? = null
    private var regexSearchCreator: Pattern? = null
    private var regexSearchLastOwner: Pattern? = null

    // ----- internal state -----
    private var requested: Int = 0
    private var needsRefresh: Boolean = false
    private var searchableObjects: Int = 0
    var isActive: Boolean = false
        private set
    private var requestQueuePause: Boolean = false
    private var requestNeedsSent: Boolean = false
    private val regionRequests: MutableMap<ULong, Int> = mutableMapOf()

    private val namesRequested: MutableSet<UUID> = mutableSetOf()
    // nullable lambda slot represents boost::signals2::connection
    private val nameCacheConnections: MutableMap<UUID, (() -> Unit)?> = mutableMapOf()
    private var rlvBehaviorCallbackConnection: (() -> Unit)? = null

    // -----------------------------------------------------------------------
    // LLFloater overrides
    // -----------------------------------------------------------------------

    open fun postBuild(): Boolean {
        TODO("GPU: wire tab container child panels and register RLV behaviour callback")
    }

    open fun draw() {
        TODO("GPU: draw beacon overlays over matching objects via gObjectList.addDebugBeacon")
    }

    open fun onOpen(key: Any) {
        TODO("GPU: select the Find tab on first open")
    }

    // -----------------------------------------------------------------------
    // Name-cache callbacks
    // -----------------------------------------------------------------------

    fun avatarNameCacheCallback(id: UUID, avName: AvatarName) {
        callbackLoadFullName(id, avName.completeName)
    }

    fun callbackLoadFullName(id: UUID, fullName: String) {
        nameCacheConnections.remove(id)
        val ourRegion = TODO("GPU: gAgent.getRegion()") as Any?
        for (entry in objectDetails.values) {
            if (entry.nameRequested && !entry.listed) {
                val objectp = TODO("GPU: gObjectList.findObject(entry.id)") as Any?
                if (objectp != null) {
                    matchObject(entry, objectp)
                }
            }
        }
        panelList?.updateName(id, fullName)
    }

    // -----------------------------------------------------------------------
    // Message handler
    // -----------------------------------------------------------------------

    fun processObjectProperties(msg: Any) {
        TODO("APR: use JVM equivalent - unpack ObjectProperties UDP message; populate objectDetails entries and call matchObject for searchable ones")
    }

    fun updateObjectCosts(
        objectId: UUID,
        objectCost: Float,
        linkCost: Float,
        physicsCost: Float,
        linkPhysicsCost: Float
    ) {
        if (!isActive) return
        TODO("GPU: update land_impact cell in result list for objectId with linkCost")
    }

    // -----------------------------------------------------------------------
    // Idle callback (registered with gIdleCallbacks in C++)
    // -----------------------------------------------------------------------

    fun idle() {
        findObjects()
        processRequestQueue()
    }

    // -----------------------------------------------------------------------
    // Region / parcel change
    // -----------------------------------------------------------------------

    fun checkRegion() {
        if (!isActive) return
        TODO("GPU: compare gAgent.getRegion() to lastRegion; on change clear objectDetails/regionRequests, reset panelList, set needsRefresh=true")
    }

    // -----------------------------------------------------------------------
    // Public search API
    // -----------------------------------------------------------------------

    fun refreshList(cacheClear: Boolean) {
        isActive = true
        checkRegion()
        if (cacheClear) {
            requested = 0
            objectDetails.clear()
            regionRequests.clear()
            TODO("GPU: restart lastPropertiesReceivedTimer")
        } else {
            objectDetails.values.forEach { it.listed = false }
        }
        TODO("GPU: panelList.getResultList().deleteAllItems()")
        panelList?.setCounterText()
        TODO("GPU: panelList.setAgentLastPosition(gAgent.getPositionGlobal())")
        namesRequested.clear()
        needsRefresh = true
        findObjects()
    }

    fun onCommitLine() {
        searchName = TODO("GPU: panelFind.nameLineEditor.getText()") as String
        searchDescription = TODO("GPU: panelFind.descriptionLineEditor.getText()") as String
        searchOwner = TODO("GPU: panelFind.ownerLineEditor.getText()") as String
        searchGroup = TODO("GPU: panelFind.groupLineEditor.getText()") as String
        searchCreator = TODO("GPU: panelFind.creatorLineEditor.getText()") as String
        searchLastOwner = TODO("GPU: panelFind.lastOwnerLineEditor.getText()") as String

        if (regexSearch) {
            fun tryCompile(text: String, setter: (Pattern?) -> Unit, clearer: () -> Unit) {
                if (text.isNotEmpty()) {
                    if (regexTest(text)) setter(Pattern.compile(text))
                    else clearer()
                }
            }
            tryCompile(searchName, { regexSearchName = it }) { searchName = "" }
            tryCompile(searchDescription, { regexSearchDescription = it }) { searchDescription = "" }
            tryCompile(searchOwner, { regexSearchOwner = it }) { searchOwner = "" }
            tryCompile(searchGroup, { regexSearchGroup = it }) { searchGroup = "" }
            tryCompile(searchCreator, { regexSearchCreator = it }) { searchCreator = "" }
            tryCompile(searchLastOwner, { regexSearchLastOwner = it }) { searchLastOwner = "" }
        }
    }

    fun clearSearchText() {
        searchName = ""; searchDescription = ""; searchOwner = ""
        searchGroup = ""; searchCreator = ""; searchLastOwner = ""
    }

    fun onButtonClickedSearch() {
        onCommitLine()
        TODO("GPU: mTab.selectFirstTab()")
        refreshList(false)
    }

    fun onCommitCheckboxRegex() {
        regexSearch = TODO("GPU: panelFind.checkboxRegex.get()") as Boolean
        if (regexSearch) onCommitLine()
    }

    fun setFindOwnerText(value: String) {
        TODO("GPU: panelFind.ownerLineEditor.setText(value)")
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun findObjects() {
        if (!isActive) return
        if (!(needsRefresh && TODO("GPU: lastUpdateTimer.elapsed > MIN_REFRESH_INTERVAL") as Boolean ||
                TODO("GPU: lastUpdateTimer.elapsed > REFRESH_INTERVAL") as Boolean)) return

        val ourRegion = TODO("GPU: gAgent.getRegion()") as Any? ?: return

        TODO("GPU: stop lastUpdateTimer; set requestQueuePause=true; set needsRefresh=false; searchableObjects=0")
        requestQueuePause = true
        needsRefresh = false
        searchableObjects = 0
        checkRegion()

        TODO("GPU: iterate gObjectList; for each object call isSearchableObject; add new entries to objectDetails; call matchObject for FINISHED entries; mark FAILED objects")

        panelList?.updateScrollList()
        updateCounterText()
        TODO("GPU: restart lastUpdateTimer; set requestQueuePause=false")
        requestQueuePause = false
    }

    private fun processRequestQueue() {
        if (!isActive || requestQueuePause) return
        TODO("APR: use JVM equivalent - send ObjectSelect+ObjectDeselect UDP packets for NEED entries per region in MAX_OBJECTS_PER_PACKET batches")
    }

    private fun requestObjectProperties(requestList: List<UInt>, select: Boolean, regionp: Any) {
        TODO("APR: use JVM equivalent - pack ObjectSelect or ObjectDeselect message and send in batches of MAX_OBJECTS_PER_PACKET to regionp.host")
    }

    private fun matchObject(details: FSObjectProperties, objectp: Any) {
        if (details.listed) return

        if (filterForSale) TODO("GPU: check details.sale_info.isForSale() and price range")
        if (filterDistance) TODO("GPU: compute distance from panelList.agentLastPosition and compare to filterDistanceMin/filterDistanceMax")
        if (filterClickAction) TODO("GPU: compare objectp.getClickAction() to filterClickActionType")
        if (filterPhysical && TODO("GPU: !objectp.flagUsePhysics()") as Boolean) return
        if (filterTemporary && TODO("GPU: !objectp.flagTemporaryOnRez()") as Boolean) return
        if (filterLocked && (details.ownerMask and 0x00008000u) != 0u) return
        if (filterPhantom && TODO("GPU: !objectp.flagPhantom()") as Boolean) return
        if (filterAttachment && TODO("GPU: !objectp.isAttachment()") as Boolean) return
        if (filterMoaP) TODO("GPU: check objectp texture entries for media")
        if (filterReflectionProbe) TODO("GPU: check objectp.mReflectionProbe.notNull()")
        if (filterAgentParcelOnly) TODO("GPU: check LLViewerParcelMgr.inAgentParcel(objectp.getPositionGlobal())")
        if (filterPermCopy && (details.ownerMask and 0x00008000u) == 0u) return
        if (filterPermModify && (details.ownerMask and 0x00004000u) == 0u) return
        if (filterPermTransfer && (details.ownerMask and 0x00002000u) == 0u) return

        var ownerName = ""
        var creatorName = ""
        var lastOwnerName = ""
        var groupName = ""
        details.nameRequested = false

        getNameFromUUID(details.ownershipId, details.groupOwned) { n, pending ->
            ownerName = n; if (pending) details.nameRequested = true
        }
        getNameFromUUID(details.creatorId, false) { n, pending ->
            creatorName = n; if (pending) details.nameRequested = true
        }
        getNameFromUUID(details.lastOwnerId, false) { n, pending ->
            lastOwnerName = n; if (pending) details.nameRequested = true
        }
        getNameFromUUID(details.groupId, true) { n, pending ->
            groupName = n; if (pending) details.nameRequested = true
        }

        TODO("GPU: apply RLVa_hideNameIfRestricted to ownerName and lastOwnerName")

        if (regexSearch) {
            fun matches(pattern: Pattern?, text: String) = pattern == null || pattern.matcher(text).matches()
            if (searchName.isNotEmpty() && !matches(regexSearchName, details.name)) return
            if (searchDescription.isNotEmpty() && !matches(regexSearchDescription, details.description)) return
            if (searchOwner.isNotEmpty() && !matches(regexSearchOwner, ownerName)) return
            if (searchGroup.isNotEmpty() && !matches(regexSearchGroup, groupName)) return
            if (searchCreator.isNotEmpty() && !matches(regexSearchCreator, creatorName)) return
            if (searchLastOwner.isNotEmpty() && !matches(regexSearchLastOwner, lastOwnerName)) return
        } else {
            fun ci(haystack: String, needle: String) = needle.isEmpty() || haystack.contains(needle, ignoreCase = true)
            if (!ci(details.name, searchName)) return
            if (!ci(details.description, searchDescription)) return
            if (!ci(ownerName, searchOwner)) return
            if (!ci(groupName, searchGroup)) return
            if (!ci(creatorName, searchCreator)) return
            if (!ci(lastOwnerName, searchLastOwner)) return
        }

        details.listed = true
        TODO("GPU: build scroll-list row with distance/name/description/price/landImpact/primCount/owner/group/creator/lastOwner columns and add to panelList.resultList")
    }

    private fun getNameFromUUID(id: UUID, group: Boolean, callback: (name: String, pending: Boolean) -> Unit) {
        TODO("APR: use JVM equivalent - async lookup via LLAvatarNameCache / gCacheName; call callback immediately if cached, otherwise schedule and invoke with pending=true")
    }

    private fun updateCounterText() {
        val listed = TODO("GPU: panelList.getResultList().getItemCount()") as Int
        val args = mapOf("[LISTED]" to listed.toString(), "[PENDING]" to requested.toString(), "[TOTAL]" to searchableObjects.toString())
        panelList?.setCounterText(args)
    }

    private fun regexTest(text: String): Boolean {
        return try {
            val pattern = Pattern.compile(text)
            pattern.matcher("asdfghjklqwerty1234567890").matches()
            true
        } catch (e: PatternSyntaxException) {
            TODO("GPU: show RegExFail notification with e.message")
            @Suppress("UNREACHABLE_CODE")
            false
        }
    }

    private fun updateRlvRestrictions(behavior: Any) {
        TODO("GPU: if behavior == RLV_BHVR_SHOWNAMES call refreshList(false)")
    }

    // -----------------------------------------------------------------------
    // Panel factory stubs (static in C++)
    // -----------------------------------------------------------------------

    protected fun createPanelList(): FSPanelAreaSearchList = FSPanelAreaSearchList(this)
    protected fun createPanelFind(): FSPanelAreaSearchFind = FSPanelAreaSearchFind(this)
    protected fun createPanelFilter(): FSPanelAreaSearchFilter = FSPanelAreaSearchFilter(this)
    protected fun createPanelAdvanced(): FSPanelAreaSearchAdvanced = FSPanelAreaSearchAdvanced()
    protected fun createPanelOptions(): FSPanelAreaSearchOptions = FSPanelAreaSearchOptions(this)
}

// ---------------------------------------------------------------------------
// List sub-panel
// ---------------------------------------------------------------------------

open class FSPanelAreaSearchList(private val fsAreaSearch: FSAreaSearch) {

    private val columnBits: MutableMap<String, UInt> = mutableMapOf(
        "distance"    to 1u,
        "name"        to 2u,
        "description" to 4u,
        "price"       to 8u,
        "land_impact" to 16u,
        "prim_count"  to 32u,
        "owner"       to 64u,
        "group"       to 128u,
        "creator"     to 256u,
        "last_owner"  to 512u
    )

    private var columnConfigConnection: (() -> Unit)? = null

    var agentLastPosition: Any = TODO("GPU: LLVector3d zero")

    open fun postBuild(): Boolean {
        TODO("GPU: bind result_list FSScrollListCtrl, counter LLTextBox, Refresh LLButton, beacons LLCheckBoxCtrl; call updateResultListColumns(); register FSAreaSearchColumnConfig signal")
    }

    fun getResultList(): Any? = TODO("GPU: return FSScrollListCtrl mResultList")

    fun setCounterText() {
        TODO("GPU: mCounterText.setText(getString(ListedPendingTotalBlank))")
    }

    fun setCounterText(args: Map<String, String>) {
        TODO("GPU: mCounterText.setText(getString(ListedPendingTotalFilled, args))")
    }

    fun updateScrollList() {
        val currentPos = TODO("GPU: gAgent.getPositionGlobal()") as Any
        val agentMoved = TODO("GPU: dist_vec(agentLastPosition, currentPos) > MIN_DISTANCE_MOVED") as Boolean
        if (agentMoved) agentLastPosition = currentPos

        TODO("GPU: iterate result list rows; remove rows whose object is gone or no longer searchable; update distance column when agentMoved")
    }

    fun updateName(id: UUID, name: String) {
        TODO("GPU: find rows matching id in mResultList and set name cell value to name")
    }

    fun updateResultListColumns() {
        TODO("GPU: read FSAreaSearchColumnConfig UInt; clear and rebuild columns hiding those whose bit is not set")
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (key == 'C'.code && mask == MASK_CONTROL) {
            onCopyToClipboard()
            return true
        }
        if (key == 'A'.code && mask == MASK_CONTROL) {
            TODO("GPU: mResultList.selectAll()")
            @Suppress("UNREACHABLE_CODE")
            return true
        }
        return false
    }

    private fun onDoubleClick() {
        val item = TODO("GPU: mResultList.getFirstSelected()") as Any? ?: return
        val objectId = TODO("GPU: item.getUUID()") as UUID
        val objectp = TODO("GPU: gObjectList.findObject(objectId)") as Any? ?: return
        val details = fsAreaSearch.objectDetails[objectId] ?: return
        TODO("GPU: LLTracker.trackLocation(objectp.getPositionGlobal(), details.name, ...)")
        if (TODO("GPU: fsAreaSearch.panelAdvanced.checkboxClickBuy.get()") as Boolean)
            buyObject(details, objectp)
        if (TODO("GPU: fsAreaSearch.panelAdvanced.checkboxClickTouch.get()") as Boolean)
            touchObject(objectp)
        if (TODO("GPU: fsAreaSearch.panelAdvanced.checkboxClickSit.get()") as Boolean)
            sitOnObject(details, objectp)
    }

    private fun onClickRefresh() {
        fsAreaSearch.refreshList(true)
    }

    private fun onCommitCheckboxBeacons() {
        fsAreaSearch.beacons = TODO("GPU: mCheckboxBeacons.get()") as Boolean
    }

    private fun onCopyToClipboard() {
        val selectedItems = TODO("GPU: mResultList.getAllSelected()") as List<Any>
        if (selectedItems.isEmpty()) return
        val sb = StringBuilder("Distance\tName\tDescription\tPrice\tLand Impact\tPrim Count\tOwner\tGroup\tCreator\tLast Owner\n")
        for (item in selectedItems) {
            val colCount = TODO("GPU: item.getNumColumns()") as Int
            for (i in 0 until colCount) {
                val value = TODO("GPU: item.getColumn(i)?.getValue()?.asString() ?: \"\"") as String
                sb.append(value)
                if (i < colCount - 1) sb.append('\t') else sb.append('\n')
            }
        }
        if (sb.endsWith('\n')) sb.deleteCharAt(sb.length - 1)
        TODO("GPU: LLClipboard.instance().copyToClipboard(wstring, 0, length)")
    }

    private fun onContextMenuItemClick(userdata: Any): Boolean =
        TODO("GPU: dispatch context-menu actions")

    private fun onContextMenuItemEnable(userdata: Any): Boolean =
        TODO("GPU: return enablement state for context-menu item")

    private fun onColumnVisibilityChecked(userdata: Any) {
        TODO("GPU: XOR column bit in FSAreaSearchColumnConfig setting")
    }

    private fun buyObject(details: FSObjectProperties, objectp: Any) {
        TODO("GPU: initiate buy workflow for objectp")
    }

    private fun sitOnObject(details: FSObjectProperties, objectp: Any) {
        TODO("GPU: send sit request for objectp")
    }

    companion object {
        private const val MASK_CONTROL = 0x0001

        fun touchObject(objectp: Any) {
            TODO("GPU: send touch interaction for objectp")
        }
    }
}

// ---------------------------------------------------------------------------
// Find sub-panel
// ---------------------------------------------------------------------------

open class FSPanelAreaSearchFind(private val fsAreaSearch: FSAreaSearch) {

    var nameLineEditor: Any? = null
    var descriptionLineEditor: Any? = null
    var ownerLineEditor: Any? = null
    var groupLineEditor: Any? = null
    var creatorLineEditor: Any? = null
    var lastOwnerLineEditor: Any? = null
    var checkboxRegex: Any? = null

    open fun postBuild(): Boolean {
        TODO("GPU: bind all LLLineEditor and LLCheckBoxCtrl children; bind search/clear buttons")
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        TODO("GPU: on Enter key trigger fsAreaSearch.onButtonClickedSearch()")
    }

    private fun onButtonClickedClear() {
        TODO("GPU: clear all line editors, then call fsAreaSearch.clearSearchText()")
    }
}

// ---------------------------------------------------------------------------
// Filter sub-panel
// ---------------------------------------------------------------------------

open class FSPanelAreaSearchFilter(private val fsAreaSearch: FSAreaSearch) {

    open fun postBuild(): Boolean {
        TODO("GPU: bind all filter checkboxes, spin controls, combo box, and save-default button")
    }

    private fun onCommitCheckbox() {
        TODO("GPU: read all checkbox values and push to fsAreaSearch filter flags")
    }

    private fun onCommitSpin() {
        TODO("GPU: read min/max spin values and push to fsAreaSearch filter ranges")
    }

    private fun onCommitCombo() {
        TODO("GPU: read combo selection index and set fsAreaSearch.filterClickActionType")
    }

    private fun onButtonClickedSaveAsDefault() {
        TODO("GPU: persist current filter state to gSavedSettings")
    }
}

// ---------------------------------------------------------------------------
// Options sub-panel
// ---------------------------------------------------------------------------

open class FSPanelAreaSearchOptions(private val fsAreaSearch: FSAreaSearch) {

    private val columnParms: MutableMap<String, Any> = mutableMapOf()

    private fun onCommitCheckboxDisplayColumn(userdata: Any) {
        TODO("GPU: toggle column visibility bit in FSAreaSearchColumnConfig setting; call panelList.updateResultListColumns()")
    }

    private fun onEnableColumnVisibilityChecked(userdata: Any): Boolean =
        TODO("GPU: return whether the column named in userdata is currently visible")
}

// ---------------------------------------------------------------------------
// Advanced sub-panel
// ---------------------------------------------------------------------------

open class FSPanelAreaSearchAdvanced {

    var checkboxClickTouch: Any? = null
    var checkboxClickBuy: Any? = null
    var checkboxClickSit: Any? = null

    open fun postBuild(): Boolean {
        TODO("GPU: bind checkboxClickTouch, checkboxClickBuy, checkboxClickSit LLCheckBoxCtrl children")
    }
}
