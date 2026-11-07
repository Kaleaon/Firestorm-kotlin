package com.firestorm.newview

import kotlin.math.*

enum class ParcelIcon {
    VOICE_ICON,
    FLY_ICON,
    PUSH_ICON,
    BUILD_ICON,
    SCRIPTS_ICON,
    DAMAGE_ICON,
    SEE_AVATARS_ICON,
    PATHFINDING_DIRTY_ICON,
    PATHFINDING_DISABLED_ICON;
}

enum class TooltipType { GENERAL, ADULT, MODERATE }

class LocationInputCtrl {

    private var locationContextMenu: Any? = null
    private var addLandmarkBtn: Any?  = null
    private var forSaleBtn: Any?      = null
    private var infoBtn: Any?         = null
    private var maturityButton: Any?  = null
    private val parcelIcons: MutableMap<ParcelIcon, Any> = mutableMapOf()
    private var damageText: Any?      = null

    private var iconHPad         = 0
    private var addLandmarkHPad  = 0

    private var landmarkImageOn:  Any? = null
    private var landmarkImageOff: Any? = null
    private var iconMaturityGeneral:  Any? = null
    private var iconMaturityAdult:    Any? = null
    private var iconMaturityModerate: Any? = null
    private var iconPathfindingDynamic: Any? = null

    private var addLandmarkTooltip  = ""
    private var editLandmarkTooltip = ""
    private var humanReadableLocation = ""
    private var isHumanReadableLocationVisible = false
    private var maturityHelpTopic = ""
    var isNavMeshDirty = false
        private set

    private val tooltips: MutableList<String> = mutableListOf()

    private val coordinatesControlListeners:    MutableList<() -> Unit> = mutableListOf()
    private val parcelPropertiesListeners:      MutableList<() -> Unit> = mutableListOf()
    private val parcelMgrListeners:             MutableList<() -> Unit> = mutableListOf()
    private val locationHistoryListeners:       MutableList<(Any) -> Unit> = mutableListOf()
    private val regionCrossingListeners:        MutableList<() -> Unit> = mutableListOf()
    private val navMeshListeners:               MutableList<(Any) -> Unit> = mutableListOf()

    private var textEntry: Any? = null

    init {
        System.err.println("APR: build sub-controls from XUI params (buttons, icons, text fields)")
    }

    fun setEnabled(enabled: Boolean) {
        System.err.println("APR: propagate enabled state to all child controls including addLandmarkBtn")
    }

    fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("APR: show tooltip for landmark button or list item under cursor")
        return false
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        System.err.println("APR: show dropdown list on KEY_DOWN if list has items")
        return false
    }

    fun onFocusReceived() {
        System.err.println("APR: highlight text entry on focus received")
    }

    fun onFocusLost() {
        System.err.println("APR: restore human-readable location display on focus lost")
    }

    fun draw() {
        System.err.println("GPU: draw location control including all parcel icons and maturity button")
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        System.err.println("GPU: re-lay-out all child controls for new width=$width height=$height")
    }

    fun setFocus(b: Boolean) {
        if (b) focusTextEntry()
    }

    fun hideList() {
        System.err.println("APR: hide dropdown list and restore focus to text entry")
    }

    fun onTextEntry(lineEditor: Any) {
        System.err.println("APR: filter dropdown list or hide it based on typed text")
    }

    fun getTextEntry(): Any? = textEntry

    fun handleLoginComplete() {
        refresh()
    }

    fun refresh() {
        refreshLocation()
        updateAddLandmarkButton()
    }

    fun isNavMeshDirty(): Boolean = isNavMeshDirty

    fun removeAll() {
        System.err.println("APR: clear all location history entries from the dropdown list")
    }

    fun getSimple(): String {
        System.err.println("APR: return the plain text currently in the text entry field")
        return ""
    }

    fun getSelectedValue(): Any? {
        System.err.println("APR: return the LLSD value of the currently selected dropdown item")
        return null
    }

    private fun focusTextEntry() {
        System.err.println("APR: move keyboard focus to the embedded text entry control")
    }

    private fun enableAddLandmarkButton(enabled: Boolean) {
        System.err.println("APR: set add landmark button image to on/off state based on $enabled")
    }

    private fun refreshLocation() {
        System.err.println("APR: rebuild location string from current agent parcel / region")
    }

    private fun refreshParcelIcons() {
        System.err.println("APR: show/hide parcel property icons based on current parcel flags")
    }

    private fun refreshHealth() {
        System.err.println("APR: update damage percentage text field from agent health")
    }

    private fun refreshMaturityButton() {
        System.err.println("APR: set maturity button image based on current region access level")
    }

    private fun positionMaturityButton() {
        System.err.println("GPU: position maturity button to the left of the text entry area")
    }

    private fun addLocationHistoryEntry(title: String, value: Any) {
        System.err.println("APR: prepend a location history entry to the dropdown list")
    }

    private fun rebuildLocationHistory(filter: String = "") {
        System.err.println("APR: clear and repopulate dropdown from LLLocationHistory with optional filter")
    }

    private fun findTeleportItemsByTitle(item: Any, filter: String): Boolean {
        System.err.println("APR: return true if teleport history item title contains filter")
        return false
    }

    private fun setText(text: String) {
        System.err.println("APR: set text entry value without triggering autocomplete")
    }

    private fun updateAddLandmarkButton() {
        System.err.println("APR: set landmark button image based on whether current parcel is bookmarked")
    }

    private fun updateAddLandmarkTooltip() {
        System.err.println("APR: update landmark button tooltip to add vs edit based on bookmark state")
    }

    private fun updateContextMenu() {
        System.err.println("APR: enable/disable context menu items based on current state")
    }

    private fun updateWidgetlayout() {
        System.err.println("GPU: re-position all embedded icons and buttons within the control bounds")
    }

    private fun changeLocationPresentation() {
        System.err.println("APR: switch between human-readable and SLURL display modes")
    }

    private fun onInfoButtonClicked() {
        System.err.println("APR: open place details floater for current parcel")
    }

    private fun onLocationHistoryChanged(event: Any) {
        System.err.println("APR: enable/disable dropdown button based on location history item count")
    }

    private fun onLocationPrearrange(data: Any) {
        System.err.println("APR: rebuild location history dropdown with current text as filter")
    }

    private fun onTextEditorRightClicked(x: Int, y: Int, mask: Int) {
        System.err.println("APR: show location context menu at ($x, $y)")
    }

    private fun onLandmarkLoaded(landmark: Any) {
        System.err.println("APR: check if loaded landmark is in current parcel; update button image")
    }

    private fun onForSaleButtonClicked() {
        System.err.println("APR: open parcel for-sale information floater")
    }

    private fun onAddLandmarkButtonClicked() {
        System.err.println("APR: create landmark at current location or open existing landmark")
    }

    private fun onAgentParcelChange() {
        refreshLocation()
        updateAddLandmarkButton()
        refreshParcelIcons()
    }

    private fun onMaturityButtonClicked() {
        System.err.println("APR: open maturity rating help link for current region")
    }

    private fun onRegionBoundaryCrossed() {
        createNavMeshStatusListenerForCurrentRegion()
        refreshLocation()
    }

    private fun onNavMeshStatusChange(navMeshStatus: Any) {
        System.err.println("APR: update isNavMeshDirty and pathfinding icons from navMeshStatus")
    }

    private fun onLocationContextMenuItemEnabled(userdata: Any): Boolean {
        System.err.println("APR: return whether context menu item identified by userdata is enabled")
        return false
    }

    private fun onLocationContextMenuItemClicked(userdata: Any) {
        System.err.println("APR: dispatch action for context menu item identified by userdata")
    }

    private fun callbackRebakeRegion(notification: Any, response: Any) {
        System.err.println("APR: handle user confirmation and trigger navmesh rebake")
    }

    private fun onParcelIconClick(icon: ParcelIcon) {
        System.err.println("APR: handle click on parcel icon $icon (show relevant floater)")
    }

    private fun createNavMeshStatusListenerForCurrentRegion() {
        System.err.println("APR: connect navmesh status callback for the agent's current region")
    }

    private fun rebakeRegionCallback(notification: Any, response: Any): Boolean {
        System.err.println("APR: confirm and trigger pathfinding navmesh rebake for current region")
        return false
    }
}
