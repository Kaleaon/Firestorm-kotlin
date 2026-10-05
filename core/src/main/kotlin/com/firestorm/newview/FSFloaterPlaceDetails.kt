package com.firestorm.newview

import java.util.UUID

enum class PlaceDisplayInfo {
    NONE, LANDMARK, CREATE_LANDMARK, REMOTE_PLACE, TELEPORT_HISTORY_ITEM, AGENT
}

class FSPlaceDetailsInventoryObserver(private val placeDetails: FSFloaterPlaceDetails) {
    fun done(addedIds: Set<UUID>) {
        placeDetails.showAddedLandmarkInfo(addedIds)
    }
}

class FSPlaceDetailsRemoteParcelInfoObserver(private val placeDetails: FSFloaterPlaceDetails) {
    private val parcelIds: MutableSet<UUID> = mutableSetOf()

    fun processParcelInfo(parcelData: LLParcelData) {
        placeDetails.changedGlobalPos(
            Vector3d(parcelData.globalX, parcelData.globalY, parcelData.globalZ)
        )
        placeDetails.processParcelDetails(parcelData)
        parcelIds.remove(parcelData.parcelId)
        System.err.println("FSPlaceDetailsRemoteParcelInfoObserver: processParcelInfo remove observer not yet implemented")
    }

    fun setParcelID(parcelId: UUID) {
        if (parcelId != UUID(0L, 0L)) {
            parcelIds.add(parcelId)
            System.err.println("FSPlaceDetailsRemoteParcelInfoObserver: setParcelID add observer and send parcel info request not yet implemented")
        }
    }

    fun setErrorStatus(status: Int, reason: String) {
        // Log remote parcel request failure; surface to caller if needed
    }

    fun destroy() {
        for (id in parcelIds) {
            System.err.println("FSPlaceDetailsRemoteParcelInfoObserver: destroy remove observer not yet implemented")
        }
        parcelIds.clear()
    }
}

class FSPlaceDetailsPlacesParcelObserver(private val placeDetails: FSFloaterPlaceDetails) {
    fun changed() {
        placeDetails.changedParcelSelection()
    }
}

data class LLParcelData(
    val parcelId: UUID,
    val name: String,
    val globalX: Double,
    val globalY: Double,
    val globalZ: Double
)

data class Vector3d(val x: Double, val y: Double, val z: Double) {
    fun isExactlyZero(): Boolean = x == 0.0 && y == 0.0 && z == 0.0
}

class FSFloaterPlaceDetails(seed: Map<String, Any?>) {

    private companion object {
        const val FS_PLACE_INFO_UPDATE_INTERVAL = 3.0f
    }

    private var panelLandmarkInfo: Any? = null
    private var panelPlaceInfo: Any? = null

    private var parcel: Any? = null
    private var item: Any? = null
    private val inventoryObserver = FSPlaceDetailsInventoryObserver(this)
    private val remoteParcelObserver = FSPlaceDetailsRemoteParcelInfoObserver(this)
    private val parcelObserver = FSPlaceDetailsPlacesParcelObserver(this)
    private var overflowBtn: Any? = null
    private var placeMenu: Any? = null
    private var landmarkMenu: Any? = null
    private var resetInfoTimer: Float = 0f

    private var isInEditMode: Boolean = false
    private var isInCreateMode: Boolean = false
    private var globalPos: Vector3d = Vector3d(0.0, 0.0, 0.0)
    private var displayInfo: PlaceDisplayInfo = PlaceDisplayInfo.NONE
    private var expectedLandmarkItemId: UUID = UUID(0L, 0L)

    private var agentParcelChangedSlot: (() -> Unit)? = { updateVerbs() }

    init {
        System.err.println("FSFloaterPlaceDetails: init register inventoryObserver not yet implemented")
        System.err.println("FSFloaterPlaceDetails: init connect agentParcelChangedSlot not yet implemented")
    }

    fun destroy() {
        System.err.println("FSFloaterPlaceDetails: destroy remove inventoryObserver not yet implemented")
        System.err.println("FSFloaterPlaceDetails: destroy remove parcelObserver not yet implemented")
        remoteParcelObserver.destroy()
        agentParcelChangedSlot = null
    }

    fun postBuild(): Boolean {
        System.err.println("FSFloaterPlaceDetails: postBuild find child panels not yet implemented")
        // Wire button callbacks to the corresponding on* methods.
        updateVerbs()
        return true
    }

    fun onOpen(key: Map<String, Any?>) {
        isInCreateMode = false
        isInEditMode = false
        expectedLandmarkItemId = UUID(0L, 0L)

        val keyType = key["type"] as? String ?: return

        when (keyType) {
            "landmark" -> {
                displayInfo = PlaceDisplayInfo.LANDMARK
                expectedLandmarkItemId = key["id"] as UUID
                System.err.println("FSFloaterPlaceDetails: onOpen landmark gInventory look up not yet implemented")
                val inventoryItem: Any = run {
                    System.err.println("FSFloaterPlaceDetails: onOpen landmark item lookup not yet implemented")
                    return
                }
                System.err.println("FSFloaterPlaceDetails: onOpen configure panelLandmarkInfo for LANDMARK not yet implemented")
                setItem(inventoryItem)
            }
            "create_landmark" -> {
                displayInfo = PlaceDisplayInfo.CREATE_LANDMARK
                globalPos = if (key.containsKey("x")) {
                    Vector3d(
                        (key["x"] as Number).toDouble(),
                        (key["y"] as Number).toDouble(),
                        (key["z"] as Number).toDouble()
                    )
                } else {
                    System.err.println("FSFloaterPlaceDetails: onOpen get agent global position not yet implemented")
                    Vector3d(0.0, 0.0, 0.0)
                }
                System.err.println("FSFloaterPlaceDetails: onOpen configure panelLandmarkInfo for CREATE_LANDMARK not yet implemented")
                isInCreateMode = true
                updateVerbs()
            }
            "remote_place" -> {
                displayInfo = PlaceDisplayInfo.REMOTE_PLACE
                System.err.println("FSFloaterPlaceDetails: onOpen configure panelPlaceInfo for PLACE not yet implemented")
                if (key.containsKey("id")) {
                    val parcelId = key["id"] as UUID
                    System.err.println("FSFloaterPlaceDetails: onOpen set parcel ID and request remote parcel info not yet implemented")
                    remoteParcelObserver.setParcelID(parcelId)
                } else {
                    globalPos = Vector3d(
                        (key["x"] as Number).toDouble(),
                        (key["y"] as Number).toDouble(),
                        (key["z"] as Number).toDouble()
                    )
                    System.err.println("FSFloaterPlaceDetails: onOpen display parcel info for globalPos not yet implemented")
                }
                updateVerbs()
            }
            "teleport_history" -> {
                displayInfo = PlaceDisplayInfo.TELEPORT_HISTORY_ITEM
                val index = (key["id"] as Number).toInt()
                System.err.println("FSFloaterPlaceDetails: onOpen retrieve teleport history item at index not yet implemented")
                updateVerbs()
            }
            "agent" -> {
                displayInfo = PlaceDisplayInfo.AGENT
                System.err.println("FSFloaterPlaceDetails: onOpen configure panelPlaceInfo for AGENT not yet implemented")
                System.err.println("FSFloaterPlaceDetails: onOpen add parcelObserver and select parcel at agent position not yet implemented")
            }
        }
    }

    fun updateVerbs() {
        if (displayInfo == PlaceDisplayInfo.NONE) {
            System.err.println("FSFloaterPlaceDetails: updateVerbs NONE visibility/enabled state not yet implemented")
            return
        }

        val havePosition = !globalPos.isExactlyZero()
        System.err.println("FSFloaterPlaceDetails: updateVerbs set teleport_btn and map_btn enabled state not yet implemented")

        when (displayInfo) {
            PlaceDisplayInfo.CREATE_LANDMARK, PlaceDisplayInfo.LANDMARK -> {
                System.err.println("FSFloaterPlaceDetails: updateVerbs LANDMARK/CREATE_LANDMARK button visibility not yet implemented")
            }
            PlaceDisplayInfo.REMOTE_PLACE, PlaceDisplayInfo.TELEPORT_HISTORY_ITEM -> {
                System.err.println("FSFloaterPlaceDetails: updateVerbs REMOTE_PLACE/TELEPORT_HISTORY_ITEM button visibility not yet implemented")
            }
            PlaceDisplayInfo.AGENT -> {
                System.err.println("FSFloaterPlaceDetails: updateVerbs AGENT button visibility not yet implemented")
            }
            else -> {}
        }
    }

    fun showAddedLandmarkInfo(items: Set<UUID>) {
        for (itemId in items) {
            System.err.println("FSFloaterPlaceDetails: showAddedLandmarkInfo check highlight_offered_object not yet implemented")
            val inventoryItem: Any? = run {
                System.err.println("FSFloaterPlaceDetails: showAddedLandmarkInfo gInventory.getItem not yet implemented")
                null
            }
            inventoryItem ?: continue
            // Only process landmark assets.
            val isLandmark: Boolean = run {
                System.err.println("FSFloaterPlaceDetails: showAddedLandmarkInfo check item type AT_LANDMARK not yet implemented")
                false
            }
            if (isLandmark) {
                if (displayInfo == PlaceDisplayInfo.CREATE_LANDMARK && item == null) {
                    setItem(inventoryItem)
                } else if (displayInfo == PlaceDisplayInfo.LANDMARK && item == null
                    && expectedLandmarkItemId != UUID(0L, 0L)
                    && itemId == expectedLandmarkItemId) {
                    setItem(inventoryItem)
                }
            }
        }
    }

    fun changedGlobalPos(pos: Vector3d) {
        globalPos = pos
        updateVerbs()
    }

    fun processParcelDetails(parcelDetails: LLParcelData) {
        System.err.println("FSFloaterPlaceDetails: processParcelDetails not yet implemented")
    }

    fun changedParcelSelection() {
        System.err.println("FSFloaterPlaceDetails: changedParcelSelection obtain floating parcel selection not yet implemented")
        System.err.println("FSFloaterPlaceDetails: changedParcelSelection determine is_current_parcel not yet implemented")
        System.err.println("FSFloaterPlaceDetails: changedParcelSelection call panelPlaceInfo.resetLocation not yet implemented")
        System.err.println("FSFloaterPlaceDetails: changedParcelSelection call panelPlaceInfo.displaySelectedParcelInfo not yet implemented")
        updateVerbs()
    }

    fun updateEstateName(name: String) {
        System.err.println("FSFloaterPlaceDetails: updateEstateName not yet implemented")
    }

    fun updateEstateOwnerName(name: String) {
        System.err.println("FSFloaterPlaceDetails: updateEstateOwnerName not yet implemented")
    }

    fun updateCovenantText(text: String) {
        System.err.println("FSFloaterPlaceDetails: updateCovenantText not yet implemented")
    }

    private fun setItem(newItem: Any) {
        if (displayInfo == PlaceDisplayInfo.LANDMARK) {
            System.err.println("FSFloaterPlaceDetails: setItem set floater title not yet implemented")
        }
        item = newItem
        System.err.println("FSFloaterPlaceDetails: setItem resolve link for AT_LINK not yet implemented")
        System.err.println("FSFloaterPlaceDetails: setItem check landmark editability not yet implemented")
        System.err.println("FSFloaterPlaceDetails: setItem configure panelLandmarkInfo not yet implemented")
        System.err.println("FSFloaterPlaceDetails: setItem load landmark asset not yet implemented")
    }

    private fun onLandmarkLoaded(landmark: Any) {
        System.err.println("FSFloaterPlaceDetails: onLandmarkLoaded not yet implemented")
        updateVerbs()
    }

    private fun onTeleportButtonClicked() {
        when (displayInfo) {
            PlaceDisplayInfo.LANDMARK, PlaceDisplayInfo.CREATE_LANDMARK -> {
                if (item == null) return
                System.err.println("FSFloaterPlaceDetails: onTeleportButtonClicked TeleportFromLandmark not yet implemented")
            }
            PlaceDisplayInfo.REMOTE_PLACE, PlaceDisplayInfo.TELEPORT_HISTORY_ITEM, PlaceDisplayInfo.AGENT -> {
                if (!globalPos.isExactlyZero()) {
                    System.err.println("FSFloaterPlaceDetails: onTeleportButtonClicked teleportViaLocation not yet implemented")
                }
            }
            else -> {}
        }
    }

    private fun onShowOnMapButtonClicked() {
        System.err.println("FSFloaterPlaceDetails: onShowOnMapButtonClicked obtain world map not yet implemented")
        when (displayInfo) {
            PlaceDisplayInfo.LANDMARK, PlaceDisplayInfo.CREATE_LANDMARK -> {
                System.err.println("FSFloaterPlaceDetails: onShowOnMapButtonClicked LANDMARK track on world map not yet implemented")
            }
            PlaceDisplayInfo.REMOTE_PLACE, PlaceDisplayInfo.TELEPORT_HISTORY_ITEM, PlaceDisplayInfo.AGENT -> {
                if (!globalPos.isExactlyZero()) {
                    System.err.println("FSFloaterPlaceDetails: onShowOnMapButtonClicked trackLocation not yet implemented")
                }
            }
            else -> {}
        }
    }

    private fun onEditButtonClicked() {
        System.err.println("FSFloaterPlaceDetails: onEditButtonClicked panelLandmarkInfo.toggleLandmarkEditMode not yet implemented")
        isInEditMode = true
        isInCreateMode = false
        updateVerbs()
    }

    private fun onCancelButtonClicked() {
        System.err.println("FSFloaterPlaceDetails: onCancelButtonClicked panelLandmarkInfo.toggleLandmarkEditMode false not yet implemented")
        isInEditMode = false
        updateVerbs()
        System.err.println("FSFloaterPlaceDetails: onCancelButtonClicked panelLandmarkInfo.displayItemInfo not yet implemented")
    }

    private fun onSaveButtonClicked() {
        if (item == null) return
        System.err.println("FSFloaterPlaceDetails: onSaveButtonClicked save landmark changes not yet implemented")
        onCancelButtonClicked()
    }

    private fun onCloseButtonClicked() {
        onSaveButtonClicked()
        System.err.println("FSFloaterPlaceDetails: onCloseButtonClicked closeFloater not yet implemented")
    }

    private fun onOverflowButtonClicked() {
        System.err.println("FSFloaterPlaceDetails: onOverflowButtonClicked not yet implemented")
    }

    private fun onOverflowMenuItemClicked(param: String) {
        when (param) {
            "landmark" -> {
                System.err.println("FSFloaterPlaceDetails: onOverflowMenuItemClicked landmark not yet implemented")
            }
            "copy" -> {
                System.err.println("FSFloaterPlaceDetails: onOverflowMenuItemClicked copy not yet implemented")
            }
            "delete" -> {
                System.err.println("FSFloaterPlaceDetails: onOverflowMenuItemClicked delete not yet implemented")
            }
            "pick" -> {
                System.err.println("FSFloaterPlaceDetails: onOverflowMenuItemClicked pick not yet implemented")
            }
            "add_to_favbar" -> {
                System.err.println("FSFloaterPlaceDetails: onOverflowMenuItemClicked add_to_favbar not yet implemented")
            }
        }
    }

    private fun onOverflowMenuItemEnable(param: String): Boolean {
        if (param == "can_create_pick") {
            System.err.println("FSFloaterPlaceDetails: onOverflowMenuItemEnable can_create_pick not yet implemented")
            return false
        }
        return true
    }

    private fun onSLURLBuilt(slurl: String) {
        if (slurl.isEmpty()) {
            System.err.println("FSFloaterPlaceDetails: onSLURLBuilt LandmarkLocationUnknown not yet implemented")
            return
        }
        System.err.println("FSFloaterPlaceDetails: onSLURLBuilt copy to clipboard not yet implemented")
    }

    companion object {
        fun showPlaceDetails(key: Map<String, Any?>) {
            System.err.println("FSFloaterPlaceDetails: showPlaceDetails not yet implemented")
        }
    }
}
