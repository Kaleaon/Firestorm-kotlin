/**
 * FSFloaterSearch.kt
 * Kotlin conversion of fsfloatersearch.h / fsfloatersearch.cpp
 *
 * Unified search floater for the Firestorm viewer.
 *
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*
import com.firestorm.llui.*

// ---------------------------------------------------------------------------
// Supporting types
// ---------------------------------------------------------------------------

/**
 * The category of content being searched.
 *
 * Mirrors the anonymous `e_search_category` enum (`SC_*`) in
 * `FSFloaterSearch` and maps to the discrete search-panel tabs.
 *
 * LAND is kept as a separate type even though it shares UI similarity with
 * PLACES; the C++ code treats them as distinct panels.
 */
enum class SearchType {
    PEOPLE,
    GROUPS,
    PLACES,
    LAND,
    EVENTS,
    CLASSIFIEDS,
    WEB
}

/**
 * A single item returned by a search operation.
 *
 * @param id          UUID of the returned entity (avatar, group, parcel, etc.).
 * @param name        Human-readable name of the entity.
 * @param description Short description or snippet (may be empty).
 * @param type        The [SearchType] category this result belongs to.
 */
data class SearchResult(
    val id: LLUUID,
    val name: String,
    val description: String,
    val type: SearchType
)

/**
 * Lightweight query descriptor — mirrors `SearchQuery` from the C++ header.
 *
 * @param category Optional category override (used by the web-search panel).
 * @param query    The actual search string entered by the user.
 */
data class SearchQuery(
    val category: String? = null,
    val query: String? = null
)

// ---------------------------------------------------------------------------
// Abstract base for every search sub-panel
// ---------------------------------------------------------------------------

/**
 * Base class for all search sub-panels.
 *
 * Mirrors [FSSearchPanelBase] from the C++ header.  Each concrete sub-panel
 * overrides [focusDefaultElement] to place keyboard focus on its primary
 * search input when the tab is selected.
 */
abstract class FSSearchPanelBase {
    /** Move keyboard focus to the panel's primary input widget. */
    open fun focusDefaultElement() { /* default: do nothing */ }
}

// ---------------------------------------------------------------------------
// Concrete search sub-panels
// ---------------------------------------------------------------------------

/** People (avatar) search panel. */
class FSPanelSearchPeople : FSSearchPanelBase() {

    private var numResultsReturned: Int = 0
    private var startSearch: Int = 0
    private var resultsReceived: Int = 0
    private var queryId: LLUUID = LLUUID.NULL

    override fun focusDefaultElement() {
        TODO("Focus the people-search combo box")
    }

    fun postBuild(): Boolean {
        TODO("Wire find button, result list, next/back buttons to callbacks")
    }

    private fun onBtnFind()     { TODO("Validate query length (>= MIN_SEARCH_STRING_SIZE) then call find()") }
    private fun onSelectItem()  { TODO("Notify parent FSFloaterSearch via onSelectedItem()") }
    private fun onBtnNext()     { startSearch += RESULT_PAGE_SIZE; TODO("Re-run find() with updated startSearch") }
    private fun onBtnBack()     { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); TODO("Re-run find()") }
    private fun find()          { TODO("Send AVATAR_QUERY message and set queryId") }
    private fun resetSearch()   { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }
    private fun showNextButton(results: Int): Int { TODO("Return adjusted result count; hide/show the next-page button") }

    companion object {
        /** Handle an incoming avatar-search reply message. */
        fun processSearchReply(msg: Any?) {
            TODO("Decode LLMessageSystem reply and populate the result list")
        }
    }
}

/** Group search panel. */
class FSPanelSearchGroups : FSSearchPanelBase() {

    private var numResultsReturned: Int = 0
    private var startSearch: Int = 0
    private var resultsReceived: Int = 0
    private var queryId: LLUUID = LLUUID.NULL

    override fun focusDefaultElement() {
        TODO("Focus the groups-search combo box")
    }

    fun postBuild(): Boolean {
        TODO("Wire find button, result list, next/back buttons to callbacks")
    }

    private fun onBtnFind()    { TODO("Validate and call find()") }
    private fun onSelectItem() { TODO("Notify parent FSFloaterSearch") }
    private fun onBtnNext()    { startSearch += RESULT_PAGE_SIZE; TODO("Re-run find()") }
    private fun onBtnBack()    { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); TODO("Re-run find()") }
    private fun find()         { TODO("Send GROUP_QUERY message") }
    private fun resetSearch()  { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }

    companion object {
        fun processSearchReply(msg: Any?) { TODO("Decode group-search reply") }
    }
}

/** Place search panel. */
class FSPanelSearchPlaces : FSSearchPanelBase() {

    private var numResultsReturned: Int = 0
    private var startSearch: Int = 0
    private var resultsReceived: Int = 0
    private var queryId: LLUUID = LLUUID.NULL

    override fun focusDefaultElement() {
        TODO("Focus the places-search combo box")
    }

    fun postBuild(): Boolean {
        TODO("Wire find button, result list, category combo, next/back buttons")
    }

    private fun onBtnFind()    { TODO("Validate and call find()") }
    private fun onSelectItem() { TODO("Notify parent FSFloaterSearch") }
    private fun onBtnNext()    { startSearch += RESULT_PAGE_SIZE; TODO("Re-run find()") }
    private fun onBtnBack()    { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); TODO("Re-run find()") }
    private fun find()         { TODO("Send PLACES_QUERY message") }
    private fun resetSearch()  { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }

    companion object {
        fun processSearchReply(msg: Any?) { TODO("Decode places-search reply") }
    }
}

/** Land-for-sale search panel. */
class FSPanelSearchLand : FSSearchPanelBase() {

    private var numResultsReturned: Int = 0
    private var startSearch: Int = 0
    private var resultsReceived: Int = 0
    private var queryId: LLUUID = LLUUID.NULL

    // Price and area filter inputs (from C++ mPriceEditor, mAreaEditor)
    private var maxPrice: Int = 0
    private var minArea: Int = 0

    fun postBuild(): Boolean {
        TODO("Wire find button, price editor, area editor, result list, next/back buttons")
    }

    private fun onBtnFind()    { TODO("Validate and call find()") }
    private fun onSelectItem() { TODO("Notify parent FSFloaterSearch") }
    private fun onBtnNext()    { startSearch += RESULT_PAGE_SIZE; TODO("Re-run find()") }
    private fun onBtnBack()    { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); TODO("Re-run find()") }
    private fun find()         { TODO("Send LAND_QUERY message with price/area filters") }
    private fun resetSearch()  { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }

    companion object {
        fun processSearchReply(msg: Any?) { TODO("Decode land-search reply") }
    }
}

/** Classifieds search panel. */
class FSPanelSearchClassifieds : FSSearchPanelBase() {

    private var numResultsReturned: Int = 0
    private var startSearch: Int = 0
    private var resultsReceived: Int = 0
    private var queryId: LLUUID = LLUUID.NULL

    override fun focusDefaultElement() {
        TODO("Focus the classifieds-search combo box")
    }

    fun postBuild(): Boolean {
        TODO("Wire find button, category combo, result list, next/back buttons")
    }

    private fun onBtnFind()    { TODO("Validate and call find()") }
    private fun onSelectItem() { TODO("Notify parent FSFloaterSearch") }
    private fun onBtnNext()    { startSearch += RESULT_PAGE_SIZE; TODO("Re-run find()") }
    private fun onBtnBack()    { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); TODO("Re-run find()") }
    private fun find()         { TODO("Send CLASSIFIED_QUERY message") }
    private fun resetSearch()  { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }

    companion object {
        fun processSearchReply(msg: Any?) { TODO("Decode classifieds-search reply") }
    }
}

/** Events search panel. */
class FSPanelSearchEvents : FSSearchPanelBase() {

    private var numResultsReturned: Int = 0
    private var startSearch: Int = 0
    private var resultsReceived: Int = 0
    private var day: Int = 0  // 0 = today, negative = past, positive = future
    private var queryId: LLUUID = LLUUID.NULL

    override fun focusDefaultElement() {
        TODO("Focus the events-search combo box")
    }

    fun postBuild(): Boolean {
        TODO("Wire find button, today/yesterday/tomorrow buttons, mode radio group, result list")
    }

    private fun onBtnFind()      { TODO("Validate and call find()") }
    private fun onSelectItem()   { TODO("Notify parent FSFloaterSearch via onSelectedEvent()") }
    private fun onBtnNext()      { startSearch += RESULT_PAGE_SIZE; TODO("Re-run find()") }
    private fun onBtnBack()      { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); TODO("Re-run find()") }
    private fun onBtnToday()     { setDay(0) }
    private fun onBtnYesterday() { setDay(-1) }
    private fun onBtnTomorrow()  { setDay(1) }
    private fun setDay(d: Int)   { day = d; TODO("Update day-label widget and re-run find()") }
    private fun onSearchModeChanged() { TODO("Switch between All/PG/Mature event filter modes") }
    private fun find()           { TODO("Send EVENT_QUERY message with day and mode filters") }
    private fun resetSearch()    { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }

    companion object {
        fun processSearchReply(msg: Any?) { TODO("Decode events-search reply") }
    }
}

/** Web-browser search panel (renders the in-world search website). */
class FSPanelSearchWeb : FSSearchPanelBase() {

    private var resetFocusOnLoad: Boolean = false

    fun postBuild(): Boolean {
        TODO("Find LLMediaCtrl child and set up category-path LLSD mapping")
    }

    /** Load the search website for the given [query]. */
    fun loadURL(query: SearchQuery) {
        TODO("Construct the search URL from query.category + query.query and load it in mWebBrowser")
    }

    override fun focusDefaultElement() {
        resetFocusOnLoad = true
        TODO("Transfer focus to mWebBrowser once the page finishes loading")
    }

    fun draw() {
        TODO("Handle resetFocusOnLoad flag after page load completes")
    }
}

// ---------------------------------------------------------------------------
// Constants (mirrors static constants in fsfloatersearch.cpp)
// ---------------------------------------------------------------------------

private const val MIN_SEARCH_STRING_SIZE = 2
private const val RESULT_PAGE_SIZE = 100

// ---------------------------------------------------------------------------
// Main floater class
// ---------------------------------------------------------------------------

/**
 * Unified search floater.
 *
 * Hosts multiple tab panels (People, Groups, Places, Land, Events,
 * Classifieds, Web) and a details side-panel that shows information about
 * the currently selected search result.
 *
 * Mirrors [FSFloaterSearch] from `fsfloatersearch.h`.
 */
class FSFloaterSearch {

    // ------------------------------------------------------------------
    // Public state
    // ------------------------------------------------------------------

    /** Accumulated search results from the most recent [search] call. */
    val results: MutableList<SearchResult> = mutableListOf()

    // ------------------------------------------------------------------
    // Private state
    // ------------------------------------------------------------------

    private var selectedId: LLUUID = LLUUID.NULL
    private var eventId: UInt = 0u
    private var hasSelection: Boolean = false

    // Sub-panels (instantiated in postBuild)
    private var panelPeople: FSPanelSearchPeople? = null
    private var panelGroups: FSPanelSearchGroups? = null
    private var panelPlaces: FSPanelSearchPlaces? = null
    private var panelEvents: FSPanelSearchEvents? = null
    private var panelLand: FSPanelSearchLand? = null
    private var panelClassifieds: FSPanelSearchClassifieds? = null
    private var panelWeb: FSPanelSearchWeb? = null

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Called after XML children are built; wires up all sub-panels and observers. */
    fun postBuild(): Boolean {
        TODO("Instantiate sub-panels, find detail widgets, register observers")
    }

    /**
     * Called when the floater is opened.
     *
     * Reads an optional [SearchQuery] from [key] to pre-populate the search
     * field and optionally kick off an immediate search.
     */
    fun onOpen(key: LLSD) {
        TODO("Decode key to SearchQuery, pre-fill search widget, optionally trigger search")
    }

    /** Called when the floater is closed; unregisters all observers. */
    fun onClose(appQuitting: Boolean) {
        results.clear()
        TODO("Disconnect mRemoteParcelObserver, mAvatarPropertiesObserver, mGroupPropertiesRequest, mEventInfoConnection")
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Initiate a search for [query] within the category [type].
     *
     * Clears [results] and dispatches the query to the appropriate sub-panel.
     *
     * @param query The user-entered search string.
     * @param type  Which content category to search.
     */
    fun search(query: String, type: SearchType) {
        if (query.length < MIN_SEARCH_STRING_SIZE) return
        results.clear()
        TODO("Switch on type, call the matching sub-panel's find() equivalent, switch active tab")
    }

    /**
     * Called when a batch of search results arrives from the network.
     *
     * Appends [newResults] to [results] and refreshes the result list widget.
     *
     * @param newResults The list of [SearchResult] items received.
     */
    fun onSearchResults(newResults: List<SearchResult>) {
        results.addAll(newResults)
        TODO("Update the scroll-list widget and show/hide the next-page button")
    }

    // ------------------------------------------------------------------
    // Detail-panel population callbacks (called by observer classes)
    // ------------------------------------------------------------------

    /** Populate the details panel with parcel information. */
    fun displayParcelDetails(parcelData: Any?) {
        TODO("Fill mDetailTitle, mDetailDesc, mDetailLocation, mDetailSnapshot from parcelData")
    }

    /** Populate the details panel with classified-ad information. */
    fun displayClassifiedDetails(classifiedInfo: Any?) {
        TODO("Fill detail widgets from LLAvatarClassifiedInfo")
    }

    /** Populate the details panel with avatar profile information. */
    fun displayAvatarDetails(avatarData: Any?) {
        TODO("Fill detail widgets from LLAvatarData")
    }

    /** Populate the details panel with group information. */
    fun displayGroupDetails(groupData: Any?) {
        TODO("Fill detail widgets from LLGroupMgrGroupData")
    }

    /** Populate the details panel with event information. */
    fun displayEventDetails(eventInfo: Any?): Boolean {
        TODO("Fill detail widgets from LLEventInfo; return true if successful")
    }

    /** Set the parcel snapshot shown alongside an event. */
    fun displayEventParcelImage(parcelData: Any?) {
        TODO("Update mDetailSnapshotParcel from parcel_data.snapshot_id")
    }

    /**
     * Show or hide the loading-progress indicator.
     *
     * @param started `true` to show the spinner; `false` to hide it.
     */
    fun setLoadingProgress(started: Boolean) {
        TODO("Show or hide the LLLoadingIndicator child widget")
    }

    /** Called when an avatar display name is updated in the name cache. */
    fun avatarNameUpdatedCallback(id: LLUUID, avName: Any?) {
        TODO("Refresh the detail-panel title if id == selectedId")
    }

    /** Called when a group name resolves from the group manager. */
    fun groupNameUpdatedCallback(id: LLUUID, name: String, isGroup: Boolean) {
        TODO("Refresh the detail-panel title if id == selectedId")
    }

    /** Invoked when the user selects a search result row. */
    fun onSelectedItem(selectedItem: LLUUID, type: SearchType) {
        selectedId = selectedItem
        hasSelection = true
        TODO("Request details for selectedItem based on type, call setLoadingProgress(true)")
    }

    /** Invoked when the user selects an event row (events use Int IDs). */
    fun onSelectedEvent(selectedEvent: Int) {
        eventId = selectedEvent.toUInt()
        hasSelection = true
        TODO("Request event details via LLEventNotifier")
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private fun resetVerbs() {
        TODO("Disable all action buttons (profile, IM, teleport, map, etc.)")
    }

    private fun flushDetails() {
        TODO("Clear all detail-panel widgets back to empty/placeholder state")
    }

    private fun onTabChange() {
        TODO("Call focusDefaultElement() on the newly-active sub-panel; reset selection")
    }

    // Action button callbacks
    private fun onBtnPeopleProfile()  { TODO("LLAvatarActions::showProfile(selectedId)") }
    private fun onBtnPeopleIM()       { TODO("LLAvatarActions::startIM(selectedId)") }
    private fun onBtnPeopleFriend()   { TODO("LLAvatarActions::requestFriendshipDialog(selectedId)") }
    private fun onBtnGroupProfile()   { TODO("LLGroupActions::show(selectedId)") }
    private fun onBtnGroupChat()      { TODO("LLGroupActions::startIM(selectedId)") }
    private fun onBtnGroupJoin()      { TODO("LLGroupActions::join(selectedId)") }
    private fun onBtnEventReminder()  { TODO("gEventNotifier.add(eventId)") }
    private fun onBtnTeleport()       { TODO("LLAvatarActions::teleportTo(mParcelGlobal)") }
    private fun onBtnMap()            { TODO("LLFloaterWorldMap::show(mParcelGlobal)") }

    // ------------------------------------------------------------------
    // Companion object
    // ------------------------------------------------------------------

    companion object {

        @Volatile
        private var instance: FSFloaterSearch? = null

        /**
         * Show (and if necessary create) the search floater.
         *
         * Mirrors the LLFloaterReg::showInstance call pattern used in C++.
         */
        fun show(): FSFloaterSearch {
            TODO("LLFloaterReg::showInstance(\"search\")")
        }

        /**
         * Retrieve the named sub-panel from an open [FSFloaterSearch] instance.
         *
         * Mirrors the templated [FSFloaterSearch::getSearchPanel<T>] helper.
         *
         * @param panelName The name of the child panel as defined in the XUI XML.
         */
        fun getSearchPanel(panelName: String): FSSearchPanelBase? {
            TODO("Return the matching sub-panel field by name")
        }
    }
}
