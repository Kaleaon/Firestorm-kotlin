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
        TODO("APR: forward parcel data to FloaterWorldMap.processParcelInfo if IDs match")
    }

    fun setParcelID(parcelId: String) {
        this.parcelId = parcelId
        TODO("APR: add observer and send parcel info request for $parcelId")
    }

    fun setErrorStatus(status: Int, reason: String) {
        TODO("APR: log error for failed parcel info request status=$status reason=$reason")
    }
}

class FloaterWorldMap(private val key: Any) {

    companion object {
        val sHomeID: String = "10000000-0000-0000-0000-000000000001"

        var instance: FloaterWorldMap? = null

        fun getInstance(): FloaterWorldMap? = instance

        fun reloadIcons() {
            TODO("APR: LLWorldMap.reloadItems()")
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
        TODO("APR: bind all child UI controls (mapView, buttons, checkboxes, combos, etc.)")
    }

    fun onOpen(key: Any) {
        TODO("APR: connect teleport-finish, reset pan, reload items, adjust zoom slider bounds")
    }

    fun onClose(appQuitting: Boolean) {
        TODO("APR: clear world map image refs, disconnect teleport-finish signal")
    }

    fun onFocusLost() {
        TODO("APR: handle floater focus loss")
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        TODO("GPU: resize floater and reflow map panel")
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        TODO("GPU: pass hover event to parent floater")
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        val mv = mapView ?: return false
        val mapX = TODO("GPU: compute x relative to mapView rect") as Int
        val mapY = TODO("GPU: compute y relative to mapView rect") as Int
        TODO("GPU: adjust zoom slider and call mapView.zoomWithPivot")
    }

    fun draw() {
        TODO("GPU: update tracking icons, zoom, checkbox states, and draw floater")
    }

    fun observeInventory(inv: Any) {
        inventory = inv
        TODO("APR: attach LLMapInventoryObserver to inventory")
    }

    fun inventoryChanged() {
        TODO("APR: rebuild landmark combo box from inventory")
    }

    fun observeFriends() {
        TODO("APR: attach LLMapFriendObserver to avatar tracker")
    }

    fun friendsChanged() {
        TODO("APR: rebuild friend combo box from avatar tracker")
    }

    fun trackAvatar(avatarId: String, name: String) {
        showParcelInfo = false
        buildAvatarIdList()
        if (trackedStatus != TrackingStatus.TRACKING_AVATAR || avatarId != trackedAvatarID) {
            trackedStatus    = TrackingStatus.TRACKING_AVATAR
            trackedAvatarID  = avatarId
            TODO("APR: LLTracker.trackAvatar($avatarId, $name) and centerOnTarget")
        }
    }

    fun trackLandmark(landmarkItemId: String) {
        showParcelInfo = false
        buildLandmarkIdLists()
        val idx = landmarkItemIdList.indexOf(landmarkItemId)
        if (idx >= 0) {
            trackedStatus = TrackingStatus.TRACKING_LANDMARK
            TODO("APR: LLTracker.trackLandmark with assetID=${landmarkAssetIdList.getOrNull(idx)}")
        } else {
            TODO("APR: LLTracker.stopTracking(false)")
        }
    }

    fun trackLocation(posGlobal: Vector3d) {
        processingSearchUpdate = false
        TODO("APR: resolve sim info, track location via LLTracker, request parcel info")
    }

    fun trackEvent(eventInfo: ItemInfo) {
        showParcelInfo = false
        trackedStatus  = TrackingStatus.TRACKING_LOCATION
        TODO("APR: LLTracker.trackLocation for event at ${eventInfo.getGlobalPosition()}")
    }

    fun trackGenericItem(item: ItemInfo) {
        showParcelInfo = false
        trackedStatus  = TrackingStatus.TRACKING_LOCATION
        TODO("APR: LLTracker.trackLocation for generic item at ${item.getGlobalPosition()}")
    }

    fun trackUrl(regionName: String, xCoord: Int, yCoord: Int, zCoord: Int) {
        completingRegionName = regionName
        completingRegionPos  = Vector3(xCoord.toFloat(), yCoord.toFloat(), zCoord.toFloat())
        TODO("APR: send named region request, track location once handle resolved")
    }

    fun getDistanceToDestination(posGlobal: Vector3d, zAttenuation: Float = 0.5f): Float {
        TODO("APR: compute distance from agent to posGlobal with Z attenuation")
    }

    fun clearLocationSelection(clearUi: Boolean = false, destReached: Boolean = false) {
        TODO("APR: stop location tracking and optionally clear UI widgets")
    }

    fun clearAvatarSelection(clearUi: Boolean = false) {
        TODO("APR: stop avatar tracking and optionally clear friend combo")
    }

    fun clearLandmarkSelection(clearUi: Boolean = false) {
        TODO("APR: stop landmark tracking and optionally clear landmark combo")
    }

    fun adjustZoomSliderBounds() {
        TODO("APR: set zoom slider min/max based on world bounding box")
    }

    fun updateSims(foundNullSim: Boolean) {
        TODO("APR: update the search results list from world map region data")
    }

    fun teleport() {
        TODO("APR: teleport agent to the currently tracked destination")
    }

    fun onChangeMaturity() {
        TODO("APR: show/hide adult/mature checkboxes based on agent access level")
    }

    fun onClearBtn() {
        TODO("APR: clear all tracking and reset the map to agent position")
    }

    fun avatarTrackFromSlapp(id: String) {
        trackAvatar(id, "")
    }

    fun processParcelInfo(parcelData: Any, posGlobal: Vector3d) {
        TODO("APR: update tracker label/tooltip with parcel name and sim coordinates")
    }

    protected fun onGoHome() {
        TODO("APR: track home position and teleport")
    }

    protected fun onLandmarkComboPrearrange() {
        TODO("APR: rebuild landmark list before showing dropdown")
    }

    protected fun onLandmarkComboCommit() {
        TODO("APR: handle landmark selection; start tracking selected landmark")
    }

    protected fun onAvatarComboPrearrange() {
        TODO("APR: rebuild avatar/friend list before showing dropdown")
    }

    protected fun onAvatarComboCommit() {
        TODO("APR: handle avatar selection; start tracking selected avatar")
    }

    protected fun onComboTextEntry() {
        TODO("APR: update search state on combo text change")
    }

    protected fun onSearchTextEntry() {
        TODO("APR: enable search button and clear stale search results")
    }

    protected fun onClickTeleportBtn() {
        teleport()
    }

    protected fun onShowTargetBtn() {
        TODO("APR: pan map to currently tracked target")
    }

    protected fun onShowAgentBtn() {
        TODO("APR: pan map to agent's current position")
    }

    protected fun onCopySLURL() {
        TODO("APR: copy current tracked SLURL to clipboard")
    }

    protected fun onTrackRegion() {
        TODO("APR: open region tracker floater for currently tracked region")
    }

    protected fun centerOnTarget(animate: Boolean) {
        TODO("APR: pan mapView to center on the tracked target position")
    }

    protected fun updateLocation() {
        TODO("APR: update location editor text from current tracked position")
    }

    protected fun fly() {
        TODO("APR: fly to tracked position instead of teleporting")
    }

    protected fun buildLandmarkIdLists() {
        TODO("APR: populate landmarkAssetIdList and landmarkItemIdList from inventory")
    }

    protected fun flyToLandmark() {
        TODO("APR: fly agent to tracked landmark position")
    }

    protected fun teleportToLandmark() {
        TODO("APR: teleport agent to tracked landmark position")
    }

    protected fun buildAvatarIdList() {
        TODO("APR: populate friend combo from LLAvatarTracker friend list")
    }

    protected fun flyToAvatar() {
        TODO("APR: fly agent to tracked avatar position")
    }

    protected fun teleportToAvatar() {
        TODO("APR: teleport agent to tracked avatar position")
    }

    protected fun updateSearchEnabled() {
        TODO("APR: enable/disable search button based on location editor content")
    }

    protected fun onLocationFocusChanged(ctrl: Any) {
        TODO("APR: handle location editor focus change")
    }

    protected fun onLocationCommit() {
        TODO("APR: parse location editor text and begin tracking resolved position")
    }

    protected fun onCoordinatesCommit() {
        TODO("APR: read X/Y/Z spin controls and update tracked location coordinates")
    }

    protected fun onCommitSearchResult(fromSearch: Boolean) {
        TODO("APR: handle search result selection; start tracking the selected region")
    }

    protected fun onTeleportFinished() {
        TODO("APR: update UI after teleport completes")
    }

    private fun updateTeleportCoordsDisplay(pos: Vector3d) {
        TODO("APR: set X/Y/Z spin control values from global pos adjusted for sim origin")
    }

    private fun enableTeleportCoordsDisplay(enabled: Boolean) {
        TODO("APR: show/hide teleport coordinate spin controls based on RLV and $enabled")
    }

    private fun requestParcelInfo(posGlobal: Vector3d, regionOrigin: Vector3d) {
        if (posGlobal == requestedGlobalPos) return
        requestedGlobalPos = posGlobal
        parcelInfoObserver = WorldMapParcelInfoObserver(posGlobal)
        TODO("APR: send RemoteParcelRequest cap call for $posGlobal")
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
        TODO("APR: bind hideButton child control")
    }

    fun setVisible(visible: Boolean) {
        if (visible) updatePosition()
    }

    fun draw() {
        updatePosition()
        TODO("GPU: draw hide-beacon panel")
    }

    private fun onHideButtonClick() {
        TODO("APR: hide the tracking beacon indicator")
    }

    private fun updatePosition() {
        TODO("GPU: reposition panel relative to tracked item on screen")
    }
}
