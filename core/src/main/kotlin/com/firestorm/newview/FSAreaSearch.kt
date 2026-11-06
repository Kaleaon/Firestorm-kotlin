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
        /* no-op */
        return false
    }

    open fun draw() {
        /* no-op */
    }

    open fun onOpen(key: Any) {
        /* no-op */
    }

    // -----------------------------------------------------------------------
    // Name-cache callbacks
    // -----------------------------------------------------------------------

    fun avatarNameCacheCallback(id: UUID, avName: AvatarName) {
        callbackLoadFullName(id, avName.completeName)
    }

    fun callbackLoadFullName(id: UUID, fullName: String) {
        nameCacheConnections.remove(id)
        val ourRegion: Any? = null // GPU: gAgent.getRegion()
        for (entry in objectDetails.values) {
            if (entry.nameRequested && !entry.listed) {
                val objectp: Any? = null // GPU: gObjectList.findObject(entry.id)
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
        System.err.println("FSAreaSearch: processObjectProperties not yet implemented")
    }

    fun updateObjectCosts(
        objectId: UUID,
        objectCost: Float,
        linkCost: Float,
        physicsCost: Float,
        linkPhysicsCost: Float
    ) {
        if (!isActive) return
        System.err.println("FSAreaSearch: updateObjectCosts not yet implemented")
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
        System.err.println("FSAreaSearch: checkRegion not yet implemented")
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
            System.err.println("FSAreaSearch: refreshList cacheClear timer restart not yet implemented")
        } else {
            objectDetails.values.forEach { it.listed = false }
        }
        System.err.println("FSAreaSearch: refreshList deleteAllItems not yet implemented")
        panelList?.setCounterText()
        System.err.println("FSAreaSearch: refreshList setAgentLastPosition not yet implemented")
        namesRequested.clear()
        needsRefresh = true
        findObjects()
    }

    fun onCommitLine() {
        searchName = ""        // GPU: panelFind.nameLineEditor.getText()
        searchDescription = "" // GPU: panelFind.descriptionLineEditor.getText()
        searchOwner = ""       // GPU: panelFind.ownerLineEditor.getText()
        searchGroup = ""       // GPU: panelFind.groupLineEditor.getText()
        searchCreator = ""     // GPU: panelFind.creatorLineEditor.getText()
        searchLastOwner = ""   // GPU: panelFind.lastOwnerLineEditor.getText()

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
        System.err.println("FSAreaSearch: onButtonClickedSearch tab select not yet implemented")
        refreshList(false)
    }

    fun onCommitCheckboxRegex() {
        regexSearch = false // GPU: panelFind.checkboxRegex.get()
        if (regexSearch) onCommitLine()
    }

    fun setFindOwnerText(value: String) {
        System.err.println("FSAreaSearch: setFindOwnerText not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun findObjects() {
        if (!isActive) return
        val elapsed = false // GPU: lastUpdateTimer.elapsed > MIN_REFRESH_INTERVAL
        val longElapsed = false // GPU: lastUpdateTimer.elapsed > REFRESH_INTERVAL
        if (!(needsRefresh && elapsed || longElapsed)) return

        val ourRegion: Any? = null // GPU: gAgent.getRegion()
        if (ourRegion == null) return

        System.err.println("FSAreaSearch: findObjects timer/queue setup not yet implemented")
        requestQueuePause = true
        needsRefresh = false
        searchableObjects = 0
        checkRegion()

        System.err.println("FSAreaSearch: findObjects object iteration not yet implemented")

        panelList?.updateScrollList()
        updateCounterText()
        System.err.println("FSAreaSearch: findObjects timer restart not yet implemented")
        requestQueuePause = false
    }

    private fun processRequestQueue() {
        if (!isActive || requestQueuePause) return
        System.err.println("FSAreaSearch: processRequestQueue not yet implemented")
    }

    private fun requestObjectProperties(requestList: List<UInt>, select: Boolean, regionp: Any) {
        System.err.println("FSAreaSearch: requestObjectProperties not yet implemented")
    }

    private fun matchObject(details: FSObjectProperties, objectp: Any) {
        if (details.listed) return

        if (filterForSale) { /* GPU: check details.sale_info.isForSale() and price range */ }
        if (filterDistance) { /* GPU: compute distance from panelList.agentLastPosition and compare to filterDistanceMin/filterDistanceMax */ }
        if (filterClickAction) { /* GPU: compare objectp.getClickAction() to filterClickActionType */ }
        if (filterPhysical && false /* GPU: !objectp.flagUsePhysics() */) return
        if (filterTemporary && false /* GPU: !objectp.flagTemporaryOnRez() */) return
        if (filterLocked && (details.ownerMask and 0x00008000u) != 0u) return
        if (filterPhantom && false /* GPU: !objectp.flagPhantom() */) return
        if (filterAttachment && false /* GPU: !objectp.isAttachment() */) return
        if (filterMoaP) { /* GPU: check objectp texture entries for media */ }
        if (filterReflectionProbe) { /* GPU: check objectp.mReflectionProbe.notNull() */ }
        if (filterAgentParcelOnly) { /* GPU: check LLViewerParcelMgr.inAgentParcel(objectp.getPositionGlobal()) */ }
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

        // GPU: apply RLVa_hideNameIfRestricted to ownerName and lastOwnerName

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
        System.err.println("FSAreaSearch: matchObject scroll-list row build not yet implemented")
    }

    private fun getNameFromUUID(id: UUID, group: Boolean, callback: (name: String, pending: Boolean) -> Unit) {
        System.err.println("FSAreaSearch: getNameFromUUID not yet implemented")
        callback("", false)
    }

    private fun updateCounterText() {
        val listed = 0 // GPU: panelList.getResultList().getItemCount()
        val args = mapOf("[LISTED]" to listed.toString(), "[PENDING]" to requested.toString(), "[TOTAL]" to searchableObjects.toString())
        panelList?.setCounterText(args)
    }

    private fun regexTest(text: String): Boolean {
        return try {
            val pattern = Pattern.compile(text)
            pattern.matcher("asdfghjklqwerty1234567890").matches()
            true
        } catch (e: PatternSyntaxException) {
            System.err.println("FSAreaSearch: regexTest RegExFail not yet implemented")
            false
        }
    }

    private fun updateRlvRestrictions(behavior: Any) {
        System.err.println("FSAreaSearch: updateRlvRestrictions not yet implemented")
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

    var agentLastPosition: Any = Object() // GPU: LLVector3d zero

    open fun postBuild(): Boolean {
        System.err.println("FSPanelAreaSearchList: postBuild not yet implemented")
        return false
    }

    fun getResultList(): Any? {
        System.err.println("FSPanelAreaSearchList: getResultList not yet implemented")
        return null
    }

    fun setCounterText() {
        System.err.println("FSPanelAreaSearchList: setCounterText not yet implemented")
    }

    fun setCounterText(args: Map<String, String>) {
        System.err.println("FSPanelAreaSearchList: setCounterText(args) not yet implemented")
    }

    fun updateScrollList() {
        val currentPos: Any = Object() // GPU: gAgent.getPositionGlobal()
        val agentMoved = false // GPU: dist_vec(agentLastPosition, currentPos) > MIN_DISTANCE_MOVED
        if (agentMoved) agentLastPosition = currentPos

        System.err.println("FSPanelAreaSearchList: updateScrollList row update not yet implemented")
    }

    fun updateName(id: UUID, name: String) {
        System.err.println("FSPanelAreaSearchList: updateName not yet implemented")
    }

    fun updateResultListColumns() {
        System.err.println("FSPanelAreaSearchList: updateResultListColumns not yet implemented")
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (key == 'C'.code && mask == MASK_CONTROL) {
            onCopyToClipboard()
            return true
        }
        if (key == 'A'.code && mask == MASK_CONTROL) {
            System.err.println("FSPanelAreaSearchList: selectAll not yet implemented")
            return true
        }
        return false
    }

    private fun onDoubleClick() {
        val item: Any? = null // GPU: mResultList.getFirstSelected()
        if (item == null) return
        val objectId: UUID = UUID(0L, 0L) // GPU: item.getUUID()
        val objectp: Any? = null // GPU: gObjectList.findObject(objectId)
        if (objectp == null) return
        val details = fsAreaSearch.objectDetails[objectId] ?: return
        System.err.println("FSPanelAreaSearchList: onDoubleClick trackLocation not yet implemented")
        if (false /* GPU: fsAreaSearch.panelAdvanced.checkboxClickBuy.get() */)
            buyObject(details, objectp)
        if (false /* GPU: fsAreaSearch.panelAdvanced.checkboxClickTouch.get() */)
            touchObject(objectp)
        if (false /* GPU: fsAreaSearch.panelAdvanced.checkboxClickSit.get() */)
            sitOnObject(details, objectp)
    }

    private fun onClickRefresh() {
        fsAreaSearch.refreshList(true)
    }

    private fun onCommitCheckboxBeacons() {
        fsAreaSearch.beacons = false // GPU: mCheckboxBeacons.get()
    }

    private fun onCopyToClipboard() {
        val selectedItems: List<Any> = emptyList() // GPU: mResultList.getAllSelected()
        if (selectedItems.isEmpty()) return
        val sb = StringBuilder("Distance\tName\tDescription\tPrice\tLand Impact\tPrim Count\tOwner\tGroup\tCreator\tLast Owner\n")
        for (item in selectedItems) {
            val colCount = 0 // GPU: item.getNumColumns()
            for (i in 0 until colCount) {
                val value = "" // GPU: item.getColumn(i)?.getValue()?.asString() ?: ""
                sb.append(value)
                if (i < colCount - 1) sb.append('\t') else sb.append('\n')
            }
        }
        if (sb.endsWith('\n')) sb.deleteCharAt(sb.length - 1)
        System.err.println("FSPanelAreaSearchList: onCopyToClipboard copyToClipboard not yet implemented")
    }

    private fun onContextMenuItemClick(userdata: Any): Boolean {
        System.err.println("FSPanelAreaSearchList: onContextMenuItemClick not yet implemented")
        return false
    }

    private fun onContextMenuItemEnable(userdata: Any): Boolean {
        System.err.println("FSPanelAreaSearchList: onContextMenuItemEnable not yet implemented")
        return false
    }

    private fun onColumnVisibilityChecked(userdata: Any) {
        System.err.println("FSPanelAreaSearchList: onColumnVisibilityChecked not yet implemented")
    }

    private fun buyObject(details: FSObjectProperties, objectp: Any) {
        System.err.println("FSPanelAreaSearchList: buyObject not yet implemented")
    }

    private fun sitOnObject(details: FSObjectProperties, objectp: Any) {
        System.err.println("FSPanelAreaSearchList: sitOnObject not yet implemented")
    }

    companion object {
        private const val MASK_CONTROL = 0x0001

        fun touchObject(objectp: Any) {
            System.err.println("FSPanelAreaSearchList: touchObject not yet implemented")
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
        System.err.println("FSPanelAreaSearchFind: postBuild not yet implemented")
        return false
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        System.err.println("FSPanelAreaSearchFind: handleKeyHere not yet implemented")
        return false
    }

    private fun onButtonClickedClear() {
        System.err.println("FSPanelAreaSearchFind: onButtonClickedClear not yet implemented")
    }
}

// ---------------------------------------------------------------------------
// Filter sub-panel
// ---------------------------------------------------------------------------

open class FSPanelAreaSearchFilter(private val fsAreaSearch: FSAreaSearch) {

    open fun postBuild(): Boolean {
        System.err.println("FSPanelAreaSearchFilter: postBuild not yet implemented")
        return false
    }

    private fun onCommitCheckbox() {
        System.err.println("FSPanelAreaSearchFilter: onCommitCheckbox not yet implemented")
    }

    private fun onCommitSpin() {
        System.err.println("FSPanelAreaSearchFilter: onCommitSpin not yet implemented")
    }

    private fun onCommitCombo() {
        System.err.println("FSPanelAreaSearchFilter: onCommitCombo not yet implemented")
    }

    private fun onButtonClickedSaveAsDefault() {
        System.err.println("FSPanelAreaSearchFilter: onButtonClickedSaveAsDefault not yet implemented")
    }
}

// ---------------------------------------------------------------------------
// Options sub-panel
// ---------------------------------------------------------------------------

open class FSPanelAreaSearchOptions(private val fsAreaSearch: FSAreaSearch) {

    private val columnParms: MutableMap<String, Any> = mutableMapOf()

    private fun onCommitCheckboxDisplayColumn(userdata: Any) {
        System.err.println("FSPanelAreaSearchOptions: onCommitCheckboxDisplayColumn not yet implemented")
    }

    private fun onEnableColumnVisibilityChecked(userdata: Any): Boolean {
        System.err.println("FSPanelAreaSearchOptions: onEnableColumnVisibilityChecked not yet implemented")
        return false
    }
}

// ---------------------------------------------------------------------------
// Advanced sub-panel
// ---------------------------------------------------------------------------

open class FSPanelAreaSearchAdvanced {

    var checkboxClickTouch: Any? = null
    var checkboxClickBuy: Any? = null
    var checkboxClickSit: Any? = null

    open fun postBuild(): Boolean {
        System.err.println("FSPanelAreaSearchAdvanced: postBuild not yet implemented")
        return false
    }
}
