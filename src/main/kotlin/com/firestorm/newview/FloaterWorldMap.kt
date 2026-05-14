package com.firestorm.newview

import kotlin.math.*

private const val MAX_VISIBLE_REGIONS = 512
private const val HIDE_BEACON_PAD     = 133
private const val ZOOM_MAX            = 128f

enum class TrackingStatus {
    TRACKING_NOTHING,
    TRACKING_AVATAR,
    TRACKING_LANDMARK,
    TRACKING_LOCATION,
}

enum class PanDirection { UP, DOWN, LEFT, RIGHT }

class WorldMapParcelInfoObserver(private val posGlobal: Vector3d) {

    private var parcelId: String = ""

    fun processParcelInfo(parcelData: Any) {
        System.err.println("WorldMapParcelInfoObserver: forward parcel data to FloaterWorldMap.processParcelInfo if IDs match not yet implemented")
    }

    fun setParcelID(parcelId: String) {
        this.parcelId = parcelId
        System.err.println("WorldMapParcelInfoObserver: add observer and send parcel info request for $parcelId not yet implemented")
    }

    fun setErrorStatus(status: Int, reason: String) {
        System.err.println("WorldMapParcelInfoObserver: log error for failed parcel info request status=$status reason=$reason not yet implemented")
    }
}

class FloaterWorldMap(private val key: Any) {

    companion object {
        val sHomeID: String = "10000000-0000-0000-0000-000000000001"

        var instance: FloaterWorldMap? = null

        fun getInstance(): FloaterWorldMap? = instance

        fun reloadIcons() {
            System.err.println("FloaterWorldMap: LLWorldMap.reloadItems() not yet implemented")
        }
    }

    private var mapView: WorldMapView? = null

    private var inventory: Any?         = null
    private var inventoryObserver: Any? = null
    private var friendObserver: Any?    = null

    private var completingRegionName = ""
    private var completingRegionPos  = Vector3(0f, 0f, 0f)
    private var lastRegionName       = ""
    private var waitingForTracker    = false
    private var isClosing            = false
    private var setToUserPosition    = true
    private var processingSearchUpdate = false

    private var trackedLocation   = Vector3d(0.0, 0.0, 0.0)
    private var trackedStatus     = TrackingStatus.TRACKING_NOTHING
    private var trackedSimName    = ""
    private var trackedAvatarID   = ""
    private var slurl: Any?       = null

    private var requestedGlobalPos = Vector3d(0.0, 0.0, 0.0)
    private var showParcelInfo    = false
    private var parcelInfoObserver: WorldMapParcelInfoObserver? = null

    private val landmarkAssetIdList: MutableList<String> = mutableListOf()
    private val landmarkItemIdList:  MutableList<String> = mutableListOf()

    private val teleportFinishListeners: MutableList<() -> Unit> = mutableListOf()

    init {
        instance = this
    }

    fun postBuild(): Boolean {
        System.err.println("FloaterWorldMap: bind all child UI controls (mapView, buttons, checkboxes, combos, etc.) not yet implemented")
        return false
    }

    fun onOpen(key: Any) {
        System.err.println("FloaterWorldMap: connect teleport-finish, reset pan, reload items, adjust zoom slider bounds not yet implemented")
    }

    fun onClose(appQuitting: Boolean) {
        System.err.println("FloaterWorldMap: clear world map image refs, disconnect teleport-finish signal not yet implemented")
    }

    fun onFocusLost() {
        System.err.println("FloaterWorldMap: handle floater focus loss not yet implemented")
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        System.err.println("FloaterWorldMap: resize floater and reflow map panel not yet implemented")
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("FloaterWorldMap: pass hover event to parent floater not yet implemented")
        return false
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        val mv = mapView ?: return false
        val mapX = 0
        val mapY = 0
        System.err.println("FloaterWorldMap: adjust zoom slider and call mapView.zoomWithPivot not yet implemented")
        return false
    }

    fun draw() {
        System.err.println("FloaterWorldMap: update tracking icons, zoom, checkbox states, and draw floater not yet implemented")
    }

    fun observeInventory(inv: Any) {
        inventory = inv
        System.err.println("FloaterWorldMap: attach LLMapInventoryObserver to inventory not yet implemented")
    }

    fun inventoryChanged() {
        System.err.println("FloaterWorldMap: rebuild landmark combo box from inventory not yet implemented")
    }

    fun observeFriends() {
        System.err.println("FloaterWorldMap: attach LLMapFriendObserver to avatar tracker not yet implemented")
    }

    fun friendsChanged() {
        System.err.println("FloaterWorldMap: rebuild friend combo box from avatar tracker not yet implemented")
    }

    fun trackAvatar(avatarId: String, name: String) {
        showParcelInfo = false
        buildAvatarIdList()
        if (trackedStatus != TrackingStatus.TRACKING_AVATAR || avatarId != trackedAvatarID) {
            trackedStatus    = TrackingStatus.TRACKING_AVATAR
            trackedAvatarID  = avatarId
            System.err.println("FloaterWorldMap: LLTracker.trackAvatar($avatarId, $name) and centerOnTarget not yet implemented")
        }
    }

    fun trackLandmark(landmarkItemId: String) {
        showParcelInfo = false
        buildLandmarkIdLists()
        val idx = landmarkItemIdList.indexOf(landmarkItemId)
        if (idx >= 0) {
            trackedStatus = TrackingStatus.TRACKING_LANDMARK
            System.err.println("FloaterWorldMap: LLTracker.trackLandmark with assetID=${landmarkAssetIdList.getOrNull(idx)} not yet implemented")
        } else {
            System.err.println("FloaterWorldMap: LLTracker.stopTracking(false) not yet implemented")
        }
    }

    fun trackLocation(posGlobal: Vector3d) {
        processingSearchUpdate = false
        System.err.println("FloaterWorldMap: resolve sim info, track location via LLTracker, request parcel info not yet implemented")
    }

    fun trackEvent(eventInfo: ItemInfo) {
        showParcelInfo = false
        trackedStatus  = TrackingStatus.TRACKING_LOCATION
        System.err.println("FloaterWorldMap: LLTracker.trackLocation for event at ${eventInfo.getGlobalPosition()} not yet implemented")
    }

    fun trackGenericItem(item: ItemInfo) {
        showParcelInfo = false
        trackedStatus  = TrackingStatus.TRACKING_LOCATION
        System.err.println("FloaterWorldMap: LLTracker.trackLocation for generic item at ${item.getGlobalPosition()} not yet implemented")
    }

    fun trackUrl(regionName: String, xCoord: Int, yCoord: Int, zCoord: Int) {
        completingRegionName = regionName
        completingRegionPos  = Vector3(xCoord.toFloat(), yCoord.toFloat(), zCoord.toFloat())
        System.err.println("FloaterWorldMap: send named region request, track location once handle resolved not yet implemented")
    }

    fun getDistanceToDestination(posGlobal: Vector3d, zAttenuation: Float = 0.5f): Float {
        System.err.println("FloaterWorldMap: compute distance from agent to posGlobal with Z attenuation not yet implemented")
        return 0f
    }

    fun clearLocationSelection(clearUi: Boolean = false, destReached: Boolean = false) {
        System.err.println("FloaterWorldMap: stop location tracking and optionally clear UI widgets not yet implemented")
    }

    fun clearAvatarSelection(clearUi: Boolean = false) {
        System.err.println("FloaterWorldMap: stop avatar tracking and optionally clear friend combo not yet implemented")
    }

    fun clearLandmarkSelection(clearUi: Boolean = false) {
        System.err.println("FloaterWorldMap: stop landmark tracking and optionally clear landmark combo not yet implemented")
    }

    fun adjustZoomSliderBounds() {
        System.err.println("FloaterWorldMap: set zoom slider min/max based on world bounding box not yet implemented")
    }

    fun updateSims(foundNullSim: Boolean) {
        System.err.println("FloaterWorldMap: update the search results list from world map region data not yet implemented")
    }

    fun teleport() {
        System.err.println("FloaterWorldMap: teleport agent to the currently tracked destination not yet implemented")
    }

    fun onChangeMaturity() {
        System.err.println("FloaterWorldMap: show/hide adult/mature checkboxes based on agent access level not yet implemented")
    }

    fun onClearBtn() {
        System.err.println("FloaterWorldMap: clear all tracking and reset the map to agent position not yet implemented")
    }

    fun avatarTrackFromSlapp(id: String) {
        trackAvatar(id, "")
    }

    fun processParcelInfo(parcelData: Any, posGlobal: Vector3d) {
        System.err.println("FloaterWorldMap: update tracker label/tooltip with parcel name and sim coordinates not yet implemented")
    }

    protected fun onGoHome() {
        System.err.println("FloaterWorldMap: track home position and teleport not yet implemented")
    }

    protected fun onLandmarkComboPrearrange() {
        System.err.println("FloaterWorldMap: rebuild landmark list before showing dropdown not yet implemented")
    }

    protected fun onLandmarkComboCommit() {
        System.err.println("FloaterWorldMap: handle landmark selection; start tracking selected landmark not yet implemented")
    }

    protected fun onAvatarComboPrearrange() {
        System.err.println("FloaterWorldMap: rebuild avatar/friend list before showing dropdown not yet implemented")
    }

    protected fun onAvatarComboCommit() {
        System.err.println("FloaterWorldMap: handle avatar selection; start tracking selected avatar not yet implemented")
    }

    protected fun onComboTextEntry() {
        System.err.println("FloaterWorldMap: update search state on combo text change not yet implemented")
    }

    protected fun onSearchTextEntry() {
        System.err.println("FloaterWorldMap: enable search button and clear stale search results not yet implemented")
    }

    protected fun onClickTeleportBtn() {
        teleport()
    }

    protected fun onShowTargetBtn() {
        System.err.println("FloaterWorldMap: pan map to currently tracked target not yet implemented")
    }

    protected fun onShowAgentBtn() {
        System.err.println("FloaterWorldMap: pan map to agent's current position not yet implemented")
    }

    protected fun onCopySLURL() {
        System.err.println("FloaterWorldMap: copy current tracked SLURL to clipboard not yet implemented")
    }

    protected fun onTrackRegion() {
        System.err.println("FloaterWorldMap: open region tracker floater for currently tracked region not yet implemented")
    }

    protected fun centerOnTarget(animate: Boolean) {
        System.err.println("FloaterWorldMap: pan mapView to center on the tracked target position not yet implemented")
    }

    protected fun updateLocation() {
        System.err.println("FloaterWorldMap: update location editor text from current tracked position not yet implemented")
    }

    protected fun fly() {
        System.err.println("FloaterWorldMap: fly to tracked position instead of teleporting not yet implemented")
    }

    protected fun buildLandmarkIdLists() {
        System.err.println("FloaterWorldMap: populate landmarkAssetIdList and landmarkItemIdList from inventory not yet implemented")
    }

    protected fun flyToLandmark() {
        System.err.println("FloaterWorldMap: fly agent to tracked landmark position not yet implemented")
    }

    protected fun teleportToLandmark() {
        System.err.println("FloaterWorldMap: teleport agent to tracked landmark position not yet implemented")
    }

    protected fun buildAvatarIdList() {
        System.err.println("FloaterWorldMap: populate friend combo from LLAvatarTracker friend list not yet implemented")
    }

    protected fun flyToAvatar() {
        System.err.println("FloaterWorldMap: fly agent to tracked avatar position not yet implemented")
    }

    protected fun teleportToAvatar() {
        System.err.println("FloaterWorldMap: teleport agent to tracked avatar position not yet implemented")
    }

    protected fun updateSearchEnabled() {
        System.err.println("FloaterWorldMap: enable/disable search button based on location editor content not yet implemented")
    }

    protected fun onLocationFocusChanged(ctrl: Any) {
        System.err.println("FloaterWorldMap: handle location editor focus change not yet implemented")
    }

    protected fun onLocationCommit() {
        System.err.println("FloaterWorldMap: parse location editor text and begin tracking resolved position not yet implemented")
    }

    protected fun onCoordinatesCommit() {
        System.err.println("FloaterWorldMap: read X/Y/Z spin controls and update tracked location coordinates not yet implemented")
    }

    protected fun onCommitSearchResult(fromSearch: Boolean) {
        System.err.println("FloaterWorldMap: handle search result selection; start tracking the selected region not yet implemented")
    }

    protected fun onTeleportFinished() {
        System.err.println("FloaterWorldMap: update UI after teleport completes not yet implemented")
    }

    private fun updateTeleportCoordsDisplay(pos: Vector3d) {
        System.err.println("FloaterWorldMap: set X/Y/Z spin control values from global pos adjusted for sim origin not yet implemented")
    }

    private fun enableTeleportCoordsDisplay(enabled: Boolean) {
        System.err.println("FloaterWorldMap: show/hide teleport coordinate spin controls based on RLV and $enabled not yet implemented")
    }

    private fun requestParcelInfo(posGlobal: Vector3d, regionOrigin: Vector3d) {
        if (posGlobal == requestedGlobalPos) return
        requestedGlobalPos = posGlobal
        parcelInfoObserver = WorldMapParcelInfoObserver(posGlobal)
        System.err.println("FloaterWorldMap: send RemoteParcelRequest cap call for $posGlobal not yet implemented")
    }
}

class PanelHideBeacon {

    companion object {
        private var panelInstance: PanelHideBeacon? = null

        fun getInstance(): PanelHideBeacon {
            return panelInstance ?: PanelHideBeacon().also { panelInstance = it }
        }
    }

    private var hideButton: Any? = null

    fun postBuild(): Boolean {
        System.err.println("PanelHideBeacon: bind hideButton child control not yet implemented")
        return false
    }

    fun setVisible(visible: Boolean) {
        if (visible) updatePosition()
    }

    fun draw() {
        updatePosition()
        System.err.println("PanelHideBeacon: draw hide-beacon panel not yet implemented")
    }

    private fun onHideButtonClick() {
        System.err.println("PanelHideBeacon: hide the tracking beacon indicator not yet implemented")
    }

    private fun updatePosition() {
        System.err.println("PanelHideBeacon: reposition panel relative to tracked item on screen not yet implemented")
    }
}
