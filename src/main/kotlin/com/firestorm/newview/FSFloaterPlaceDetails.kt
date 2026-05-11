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
        TODO("APR: use JVM equivalent: remove remote parcel observer for parcelData.parcelId")
    }

    fun setParcelID(parcelId: UUID) {
        if (parcelId != UUID(0L, 0L)) {
            parcelIds.add(parcelId)
            TODO("APR: use JVM equivalent: add observer and send parcel info request for parcelId")
        }
    }

    fun setErrorStatus(status: Int, reason: String) {
        // Log remote parcel request failure; surface to caller if needed
    }

    fun destroy() {
        for (id in parcelIds) {
            TODO("APR: use JVM equivalent: remove observer for id")
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
        TODO("APR: use JVM equivalent: register inventoryObserver with gInventory")
        TODO("APR: use JVM equivalent: connect agentParcelChangedSlot to agent parcel change signal")
    }

    fun destroy() {
        TODO("APR: use JVM equivalent: remove inventoryObserver from gInventory")
        TODO("APR: use JVM equivalent: remove parcelObserver from LLViewerParcelMgr")
        remoteParcelObserver.destroy()
        agentParcelChangedSlot = null
    }

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent: find child panels panel_landmark_info and panel_place_profile")
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
                val inventoryItem = TODO("APR: use JVM equivalent: look up item by expectedLandmarkItemId in gInventory") as? Any ?: return
                TODO("APR: use JVM equivalent: configure panelLandmarkInfo for LANDMARK info type")
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
                    TODO("APR: use JVM equivalent: get agent global position") as Vector3d
                }
                TODO("APR: use JVM equivalent: configure panelLandmarkInfo for CREATE_LANDMARK")
                isInCreateMode = true
                updateVerbs()
            }
            "remote_place" -> {
                displayInfo = PlaceDisplayInfo.REMOTE_PLACE
                TODO("APR: use JVM equivalent: configure panelPlaceInfo for PLACE info type")
                if (key.containsKey("id")) {
                    val parcelId = key["id"] as UUID
                    TODO("APR: use JVM equivalent: set parcel ID on panelPlaceInfo and request remote parcel info")
                    remoteParcelObserver.setParcelID(parcelId)
                } else {
                    globalPos = Vector3d(
                        (key["x"] as Number).toDouble(),
                        (key["y"] as Number).toDouble(),
                        (key["z"] as Number).toDouble()
                    )
                    TODO("APR: use JVM equivalent: display parcel info for globalPos, optionally with region handle from ox/oy")
                }
                updateVerbs()
            }
            "teleport_history" -> {
                displayInfo = PlaceDisplayInfo.TELEPORT_HISTORY_ITEM
                val index = (key["id"] as Number).toInt()
                TODO("APR: use JVM equivalent: retrieve teleport history item at index, set globalPos and title")
                updateVerbs()
            }
            "agent" -> {
                displayInfo = PlaceDisplayInfo.AGENT
                TODO("APR: use JVM equivalent: configure panelPlaceInfo for AGENT info type")
                TODO("APR: use JVM equivalent: add parcelObserver to LLViewerParcelMgr and select parcel at agent position")
            }
        }
    }

    fun updateVerbs() {
        if (displayInfo == PlaceDisplayInfo.NONE) {
            TODO("APR: use JVM equivalent: set visibility/enabled state of teleport_btn, map_btn, edit_btn, save_btn, cancel_btn, close_btn")
            return
        }

        val havePosition = !globalPos.isExactlyZero()
        TODO("APR: use JVM equivalent: set enabled state of teleport_btn and map_btn based on havePosition")

        when (displayInfo) {
            PlaceDisplayInfo.CREATE_LANDMARK, PlaceDisplayInfo.LANDMARK -> {
                TODO("APR: use JVM equivalent: toggle visibility of edit/save/cancel/close/overflow buttons based on isInEditMode/isInCreateMode")
            }
            PlaceDisplayInfo.REMOTE_PLACE, PlaceDisplayInfo.TELEPORT_HISTORY_ITEM -> {
                TODO("APR: use JVM equivalent: show teleport_btn (enabled per RLV) and map_btn; hide edit/save/cancel/close")
            }
            PlaceDisplayInfo.AGENT -> {
                TODO("APR: use JVM equivalent: show teleport_btn (enabled if havePosition and not in agent parcel) and map_btn; hide others")
            }
            else -> {}
        }
    }

    fun showAddedLandmarkInfo(items: Set<UUID>) {
        for (itemId in items) {
            TODO("APR: use JVM equivalent: check highlight_offered_object for itemId")
            val inventoryItem = TODO("APR: use JVM equivalent: gInventory.getItem(itemId)") as? Any ?: continue
            // Only process landmark assets.
            val isLandmark = TODO("APR: use JVM equivalent: check item type == AT_LANDMARK") as Boolean
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
        TODO("APR: use JVM equivalent: set floater title using parcelDetails.name")
    }

    fun changedParcelSelection() {
        TODO("APR: use JVM equivalent: obtain floating parcel selection from LLViewerParcelMgr")
        TODO("APR: use JVM equivalent: determine is_current_parcel and update globalPos accordingly")
        TODO("APR: use JVM equivalent: call panelPlaceInfo.resetLocation() if position changed and timer expired")
        TODO("APR: use JVM equivalent: call panelPlaceInfo.displaySelectedParcelInfo(...)")
        updateVerbs()
    }

    fun updateEstateName(name: String) {
        TODO("APR: use JVM equivalent: delegate to panelPlaceInfo.updateEstateName(name)")
    }

    fun updateEstateOwnerName(name: String) {
        TODO("APR: use JVM equivalent: delegate to panelPlaceInfo.updateEstateOwnerName(name)")
    }

    fun updateCovenantText(text: String) {
        TODO("APR: use JVM equivalent: delegate to panelPlaceInfo.updateCovenantText(text)")
    }

    private fun setItem(newItem: Any) {
        if (displayInfo == PlaceDisplayInfo.LANDMARK) {
            TODO("APR: use JVM equivalent: set floater title with item name")
        }
        item = newItem
        TODO("APR: use JVM equivalent: resolve link if item type is AT_LINK")
        TODO("APR: use JVM equivalent: check landmark editability via inventory permissions")
        TODO("APR: use JVM equivalent: configure panelLandmarkInfo with item info and folder combo")
        TODO("APR: use JVM equivalent: load landmark asset and call onLandmarkLoaded when ready")
    }

    private fun onLandmarkLoaded(landmark: Any) {
        TODO("APR: use JVM equivalent: get region ID and global pos from landmark, call panelLandmarkInfo.displayParcelInfo")
        updateVerbs()
    }

    private fun onTeleportButtonClicked() {
        when (displayInfo) {
            PlaceDisplayInfo.LANDMARK, PlaceDisplayInfo.CREATE_LANDMARK -> {
                if (item == null) return
                TODO("APR: use JVM equivalent: show TeleportFromLandmark notification with item asset UUID and name")
            }
            PlaceDisplayInfo.REMOTE_PLACE, PlaceDisplayInfo.TELEPORT_HISTORY_ITEM, PlaceDisplayInfo.AGENT -> {
                if (!globalPos.isExactlyZero()) {
                    TODO("APR: use JVM equivalent: agent.teleportViaLocation(globalPos) and worldmap.trackLocation(globalPos)")
                }
            }
            else -> {}
        }
    }

    private fun onShowOnMapButtonClicked() {
        TODO("APR: use JVM equivalent: obtain world map instance")
        when (displayInfo) {
            PlaceDisplayInfo.LANDMARK, PlaceDisplayInfo.CREATE_LANDMARK -> {
                TODO("APR: use JVM equivalent: get landmark global pos and track on world map")
            }
            PlaceDisplayInfo.REMOTE_PLACE, PlaceDisplayInfo.TELEPORT_HISTORY_ITEM, PlaceDisplayInfo.AGENT -> {
                if (!globalPos.isExactlyZero()) {
                    TODO("APR: use JVM equivalent: worldmap.trackLocation(globalPos) and show world_map floater")
                }
            }
            else -> {}
        }
    }

    private fun onEditButtonClicked() {
        TODO("APR: use JVM equivalent: panelLandmarkInfo.toggleLandmarkEditMode(true)")
        isInEditMode = true
        isInCreateMode = false
        updateVerbs()
    }

    private fun onCancelButtonClicked() {
        TODO("APR: use JVM equivalent: panelLandmarkInfo.toggleLandmarkEditMode(false)")
        isInEditMode = false
        updateVerbs()
        TODO("APR: use JVM equivalent: panelLandmarkInfo.displayItemInfo(item)")
    }

    private fun onSaveButtonClicked() {
        if (item == null) return
        TODO("APR: use JVM equivalent: read title/notes from panelLandmarkInfo, trim, update inventory item and reparent if folder changed")
        onCancelButtonClicked()
    }

    private fun onCloseButtonClicked() {
        onSaveButtonClicked()
        TODO("APR: use JVM equivalent: closeFloater()")
    }

    private fun onOverflowButtonClicked() {
        TODO("APR: use JVM equivalent: choose place or landmark overflow menu, configure item visibility, and attach to overflow button")
    }

    private fun onOverflowMenuItemClicked(param: String) {
        when (param) {
            "landmark" -> {
                TODO("APR: use JVM equivalent: show fs_placedetails floater with create_landmark key using globalPos")
            }
            "copy" -> {
                TODO("APR: use JVM equivalent: get SLURL from globalPos and call onSLURLBuilt")
            }
            "delete" -> {
                TODO("APR: use JVM equivalent: gInventory.removeItem(item.uuid) and closeFloater()")
            }
            "pick" -> {
                TODO("APR: use JVM equivalent: call panel.createPick(globalPos) on the appropriate panel")
            }
            "add_to_favbar" -> {
                TODO("APR: use JVM equivalent: copy inventory item to favorites folder")
            }
        }
    }

    private fun onOverflowMenuItemEnable(param: String): Boolean {
        if (param == "can_create_pick") {
            TODO("APR: use JVM equivalent: return !LLAgentPicksInfo.isPickLimitReached()")
        }
        return true
    }

    private fun onSLURLBuilt(slurl: String) {
        if (slurl.isEmpty()) {
            TODO("APR: use JVM equivalent: show LandmarkLocationUnknown notification")
            return
        }
        TODO("APR: use JVM equivalent: copy slurl to system clipboard and show CopySLURL notification")
    }

    companion object {
        fun showPlaceDetails(key: Map<String, Any?>) {
            TODO("APR: use JVM equivalent: check FSUseStandalonePlaceDetailsFloater setting; show fs_placedetails floater or places side panel")
        }
    }
}
