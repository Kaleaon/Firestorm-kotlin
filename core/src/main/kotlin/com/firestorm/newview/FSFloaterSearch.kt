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
        System.err.println("FSPanelSearchPeople: focusDefaultElement not yet implemented")
    }

    fun postBuild(): Boolean {
        System.err.println("FSPanelSearchPeople: postBuild not yet implemented")
        return false
    }

    private fun onBtnFind()     { System.err.println("FSPanelSearchPeople: onBtnFind not yet implemented") }
    private fun onSelectItem()  { System.err.println("FSPanelSearchPeople: onSelectItem not yet implemented") }
    private fun onBtnNext()     { startSearch += RESULT_PAGE_SIZE; System.err.println("FSPanelSearchPeople: onBtnNext not yet implemented") }
    private fun onBtnBack()     { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); System.err.println("FSPanelSearchPeople: onBtnBack not yet implemented") }
    private fun find()          { System.err.println("FSPanelSearchPeople: find not yet implemented") }
    private fun resetSearch()   { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }
    private fun showNextButton(results: Int): Int { System.err.println("FSPanelSearchPeople: showNextButton not yet implemented"); return results }

    companion object {
        /** Handle an incoming avatar-search reply message. */
        fun processSearchReply(msg: Any?) {
            System.err.println("FSPanelSearchPeople: processSearchReply not yet implemented")
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
        System.err.println("FSPanelSearchGroups: focusDefaultElement not yet implemented")
    }

    fun postBuild(): Boolean {
        System.err.println("FSPanelSearchGroups: postBuild not yet implemented")
        return false
    }

    private fun onBtnFind()    { System.err.println("FSPanelSearchGroups: onBtnFind not yet implemented") }
    private fun onSelectItem() { System.err.println("FSPanelSearchGroups: onSelectItem not yet implemented") }
    private fun onBtnNext()    { startSearch += RESULT_PAGE_SIZE; System.err.println("FSPanelSearchGroups: onBtnNext not yet implemented") }
    private fun onBtnBack()    { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); System.err.println("FSPanelSearchGroups: onBtnBack not yet implemented") }
    private fun find()         { System.err.println("FSPanelSearchGroups: find not yet implemented") }
    private fun resetSearch()  { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }

    companion object {
        fun processSearchReply(msg: Any?) { System.err.println("FSPanelSearchGroups: processSearchReply not yet implemented") }
    }
}

/** Place search panel. */
class FSPanelSearchPlaces : FSSearchPanelBase() {

    private var numResultsReturned: Int = 0
    private var startSearch: Int = 0
    private var resultsReceived: Int = 0
    private var queryId: LLUUID = LLUUID.NULL

    override fun focusDefaultElement() {
        System.err.println("FSPanelSearchPlaces: focusDefaultElement not yet implemented")
    }

    fun postBuild(): Boolean {
        System.err.println("FSPanelSearchPlaces: postBuild not yet implemented")
        return false
    }

    private fun onBtnFind()    { System.err.println("FSPanelSearchPlaces: onBtnFind not yet implemented") }
    private fun onSelectItem() { System.err.println("FSPanelSearchPlaces: onSelectItem not yet implemented") }
    private fun onBtnNext()    { startSearch += RESULT_PAGE_SIZE; System.err.println("FSPanelSearchPlaces: onBtnNext not yet implemented") }
    private fun onBtnBack()    { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); System.err.println("FSPanelSearchPlaces: onBtnBack not yet implemented") }
    private fun find()         { System.err.println("FSPanelSearchPlaces: find not yet implemented") }
    private fun resetSearch()  { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }

    companion object {
        fun processSearchReply(msg: Any?) { System.err.println("FSPanelSearchPlaces: processSearchReply not yet implemented") }
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
        System.err.println("FSPanelSearchLand: postBuild not yet implemented")
        return false
    }

    private fun onBtnFind()    { System.err.println("FSPanelSearchLand: onBtnFind not yet implemented") }
    private fun onSelectItem() { System.err.println("FSPanelSearchLand: onSelectItem not yet implemented") }
    private fun onBtnNext()    { startSearch += RESULT_PAGE_SIZE; System.err.println("FSPanelSearchLand: onBtnNext not yet implemented") }
    private fun onBtnBack()    { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); System.err.println("FSPanelSearchLand: onBtnBack not yet implemented") }
    private fun find()         { System.err.println("FSPanelSearchLand: find not yet implemented") }
    private fun resetSearch()  { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }

    companion object {
        fun processSearchReply(msg: Any?) { System.err.println("FSPanelSearchLand: processSearchReply not yet implemented") }
    }
}

/** Classifieds search panel. */
class FSPanelSearchClassifieds : FSSearchPanelBase() {

    private var numResultsReturned: Int = 0
    private var startSearch: Int = 0
    private var resultsReceived: Int = 0
    private var queryId: LLUUID = LLUUID.NULL

    override fun focusDefaultElement() {
        System.err.println("FSPanelSearchClassifieds: focusDefaultElement not yet implemented")
    }

    fun postBuild(): Boolean {
        System.err.println("FSPanelSearchClassifieds: postBuild not yet implemented")
        return false
    }

    private fun onBtnFind()    { System.err.println("FSPanelSearchClassifieds: onBtnFind not yet implemented") }
    private fun onSelectItem() { System.err.println("FSPanelSearchClassifieds: onSelectItem not yet implemented") }
    private fun onBtnNext()    { startSearch += RESULT_PAGE_SIZE; System.err.println("FSPanelSearchClassifieds: onBtnNext not yet implemented") }
    private fun onBtnBack()    { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); System.err.println("FSPanelSearchClassifieds: onBtnBack not yet implemented") }
    private fun find()         { System.err.println("FSPanelSearchClassifieds: find not yet implemented") }
    private fun resetSearch()  { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }

    companion object {
        fun processSearchReply(msg: Any?) { System.err.println("FSPanelSearchClassifieds: processSearchReply not yet implemented") }
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
        System.err.println("FSPanelSearchEvents: focusDefaultElement not yet implemented")
    }

    fun postBuild(): Boolean {
        System.err.println("FSPanelSearchEvents: postBuild not yet implemented")
        return false
    }

    private fun onBtnFind()      { System.err.println("FSPanelSearchEvents: onBtnFind not yet implemented") }
    private fun onSelectItem()   { System.err.println("FSPanelSearchEvents: onSelectItem not yet implemented") }
    private fun onBtnNext()      { startSearch += RESULT_PAGE_SIZE; System.err.println("FSPanelSearchEvents: onBtnNext not yet implemented") }
    private fun onBtnBack()      { startSearch = maxOf(0, startSearch - RESULT_PAGE_SIZE); System.err.println("FSPanelSearchEvents: onBtnBack not yet implemented") }
    private fun onBtnToday()     { setDay(0) }
    private fun onBtnYesterday() { setDay(-1) }
    private fun onBtnTomorrow()  { setDay(1) }
    private fun setDay(d: Int)   { day = d; System.err.println("FSPanelSearchEvents: setDay not yet implemented") }
    private fun onSearchModeChanged() { System.err.println("FSPanelSearchEvents: onSearchModeChanged not yet implemented") }
    private fun find()           { System.err.println("FSPanelSearchEvents: find not yet implemented") }
    private fun resetSearch()    { numResultsReturned = 0; startSearch = 0; resultsReceived = 0 }

    companion object {
        fun processSearchReply(msg: Any?) { System.err.println("FSPanelSearchEvents: processSearchReply not yet implemented") }
    }
}

/** Web-browser search panel (renders the in-world search website). */
class FSPanelSearchWeb : FSSearchPanelBase() {

    private var resetFocusOnLoad: Boolean = false

    fun postBuild(): Boolean {
        System.err.println("FSPanelSearchWeb: postBuild not yet implemented")
        return false
    }

    /** Load the search website for the given [query]. */
    fun loadURL(query: SearchQuery) {
        System.err.println("FSPanelSearchWeb: loadURL not yet implemented")
    }

    override fun focusDefaultElement() {
        resetFocusOnLoad = true
        System.err.println("FSPanelSearchWeb: focusDefaultElement not yet implemented")
    }

    fun draw() {
        System.err.println("FSPanelSearchWeb: draw not yet implemented")
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
        System.err.println("FSFloaterSearch: postBuild not yet implemented")
        return false
    }

    /**
     * Called when the floater is opened.
     *
     * Reads an optional [SearchQuery] from [key] to pre-populate the search
     * field and optionally kick off an immediate search.
     */
    fun onOpen(key: LLSD) {
        System.err.println("FSFloaterSearch: onOpen not yet implemented")
    }

    /** Called when the floater is closed; unregisters all observers. */
    fun onClose(appQuitting: Boolean) {
        results.clear()
        System.err.println("FSFloaterSearch: onClose not yet implemented")
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
        System.err.println("FSFloaterSearch: search not yet implemented")
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
        System.err.println("FSFloaterSearch: onSearchResults not yet implemented")
    }

    // ------------------------------------------------------------------
    // Detail-panel population callbacks (called by observer classes)
    // ------------------------------------------------------------------

    /** Populate the details panel with parcel information. */
    fun displayParcelDetails(parcelData: Any?) {
        System.err.println("FSFloaterSearch: displayParcelDetails not yet implemented")
    }

    /** Populate the details panel with classified-ad information. */
    fun displayClassifiedDetails(classifiedInfo: Any?) {
        System.err.println("FSFloaterSearch: displayClassifiedDetails not yet implemented")
    }

    /** Populate the details panel with avatar profile information. */
    fun displayAvatarDetails(avatarData: Any?) {
        System.err.println("FSFloaterSearch: displayAvatarDetails not yet implemented")
    }

    /** Populate the details panel with group information. */
    fun displayGroupDetails(groupData: Any?) {
        System.err.println("FSFloaterSearch: displayGroupDetails not yet implemented")
    }

    /** Populate the details panel with event information. */
    fun displayEventDetails(eventInfo: Any?): Boolean {
        System.err.println("FSFloaterSearch: displayEventDetails not yet implemented")
        return false
    }

    /** Set the parcel snapshot shown alongside an event. */
    fun displayEventParcelImage(parcelData: Any?) {
        System.err.println("FSFloaterSearch: displayEventParcelImage not yet implemented")
    }

    /**
     * Show or hide the loading-progress indicator.
     *
     * @param started `true` to show the spinner; `false` to hide it.
     */
    fun setLoadingProgress(started: Boolean) {
        System.err.println("FSFloaterSearch: setLoadingProgress not yet implemented")
    }

    /** Called when an avatar display name is updated in the name cache. */
    fun avatarNameUpdatedCallback(id: LLUUID, avName: Any?) {
        System.err.println("FSFloaterSearch: avatarNameUpdatedCallback not yet implemented")
    }

    /** Called when a group name resolves from the group manager. */
    fun groupNameUpdatedCallback(id: LLUUID, name: String, isGroup: Boolean) {
        System.err.println("FSFloaterSearch: groupNameUpdatedCallback not yet implemented")
    }

    /** Invoked when the user selects a search result row. */
    fun onSelectedItem(selectedItem: LLUUID, type: SearchType) {
        selectedId = selectedItem
        hasSelection = true
        System.err.println("FSFloaterSearch: onSelectedItem not yet implemented")
    }

    /** Invoked when the user selects an event row (events use Int IDs). */
    fun onSelectedEvent(selectedEvent: Int) {
        eventId = selectedEvent.toUInt()
        hasSelection = true
        System.err.println("FSFloaterSearch: onSelectedEvent not yet implemented")
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private fun resetVerbs() {
        System.err.println("FSFloaterSearch: resetVerbs not yet implemented")
    }

    private fun flushDetails() {
        System.err.println("FSFloaterSearch: flushDetails not yet implemented")
    }

    private fun onTabChange() {
        System.err.println("FSFloaterSearch: onTabChange not yet implemented")
    }

    // Action button callbacks
    private fun onBtnPeopleProfile()  { System.err.println("FSFloaterSearch: onBtnPeopleProfile not yet implemented") }
    private fun onBtnPeopleIM()       { System.err.println("FSFloaterSearch: onBtnPeopleIM not yet implemented") }
    private fun onBtnPeopleFriend()   { System.err.println("FSFloaterSearch: onBtnPeopleFriend not yet implemented") }
    private fun onBtnGroupProfile()   { System.err.println("FSFloaterSearch: onBtnGroupProfile not yet implemented") }
    private fun onBtnGroupChat()      { System.err.println("FSFloaterSearch: onBtnGroupChat not yet implemented") }
    private fun onBtnGroupJoin()      { System.err.println("FSFloaterSearch: onBtnGroupJoin not yet implemented") }
    private fun onBtnEventReminder()  { System.err.println("FSFloaterSearch: onBtnEventReminder not yet implemented") }
    private fun onBtnTeleport()       { System.err.println("FSFloaterSearch: onBtnTeleport not yet implemented") }
    private fun onBtnMap()            { System.err.println("FSFloaterSearch: onBtnMap not yet implemented") }

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
            System.err.println("FSFloaterSearch: show not yet implemented")
            return instance ?: FSFloaterSearch().also { instance = it }
        }

        /**
         * Retrieve the named sub-panel from an open [FSFloaterSearch] instance.
         *
         * Mirrors the templated [FSFloaterSearch::getSearchPanel<T>] helper.
         *
         * @param panelName The name of the child panel as defined in the XUI XML.
         */
        fun getSearchPanel(panelName: String): FSSearchPanelBase? {
            System.err.println("FSFloaterSearch: getSearchPanel not yet implemented")
            return null
        }
    }
}
