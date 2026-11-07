package com.firestorm.newview

import com.firestorm.ui.Panel
import com.firestorm.ui.Button
import com.firestorm.ui.MenuButton
import com.firestorm.ui.FilterEditor
import com.firestorm.ui.TabContainer
import com.firestorm.ui.ToggleableMenu
import com.firestorm.ui.LayoutStack
import com.firestorm.inventory.InventoryItem
import com.firestorm.places.PanelPlaceInfo
import com.firestorm.places.PanelPlaceProfile
import com.firestorm.places.PanelLandmarkInfo
import com.firestorm.places.PanelPlacesTab
import com.firestorm.parcel.ParcelSelection
import com.firestorm.parcel.ParcelObserver
import com.firestorm.parcel.ViewerParcelMgr
import com.firestorm.types.UUID
import com.firestorm.types.LLSD
import com.firestorm.types.Vector3d

typealias FolderPair = Pair<UUID, String>

private const val PLACE_INFO_UPDATE_INTERVAL: Float = 3.0f

private const val AGENT_INFO_TYPE = "agent"
private const val CREATE_LANDMARK_INFO_TYPE = "create_landmark"
private const val CREATE_PICK_TYPE = "create_pick"
private const val LANDMARK_INFO_TYPE = "landmark"
private const val REMOTE_PLACE_INFO_TYPE = "remote_place"
private const val TELEPORT_HISTORY_INFO_TYPE = "teleport_history"
private const val LANDMARK_TAB_INFO_TYPE = "open_landmark_tab"
private const val TELEPORT_HISTORY_TAB_INFO_TYPE = "open_teleport_history_tab"

class PlacesParcelObserver(private val places: PanelPlaces) : ParcelObserver() {
    override fun changed() {
        places.changedParcelSelection()
    }
}

class PlacesInventoryObserver(private val places: PanelPlaces) {
    fun changed(mask: UInt) {
        if (!places.tabsCreated()) places.createTabs()
    }

    fun done(addedIds: Set<UUID>) {
        places.showAddedLandmarkInfo(addedIds)
    }
}

class PlacesRemoteParcelInfoObserver(private val places: PanelPlaces) {
    private val parcelIds: MutableSet<UUID> = mutableSetOf()

    fun processParcelInfo(parcelData: Any) {
        System.err.println("PlacesRemoteParcelInfoObserver: processParcelInfo not yet implemented")
    }

    fun setParcelID(parcelId: UUID) {
        if (parcelId != UUID.NULL) {
            parcelIds.add(parcelId)
            System.err.println("PlacesRemoteParcelInfoObserver: setParcelID not yet implemented")
        }
    }

    fun setErrorStatus(status: Int, reason: String) {
        System.err.println("PlacesRemoteParcelInfoObserver: setErrorStatus not yet implemented")
    }

    fun cleanup() {
        System.err.println("PlacesRemoteParcelInfoObserver: cleanup not yet implemented")
    }
}

class PanelPlaces : Panel() {

    private var filterEditor: FilterEditor? = null
    private var activePanel: PanelPlacesTab? = null
    private var tabContainer: TabContainer? = null
    private var filterContainer: LayoutStack? = null
    private var placeProfile: PanelPlaceProfile? = null
    private var landmarkInfo: PanelLandmarkInfo? = null

    private var placeMenu: ToggleableMenu? = null
    private var landmarkMenu: ToggleableMenu? = null

    private var placeProfileBackBtn: Button? = null
    private var teleportBtn: Button? = null
    private var showOnMapBtn: Button? = null
    private var saveBtn: Button? = null
    private var cancelBtn: Button? = null
    private var closeBtn: Button? = null
    private var overflowBtn: MenuButton? = null
    private var placeInfoBtn: Button? = null

    private var gearMenuButton: MenuButton? = null
    private var sortingMenuButton: MenuButton? = null
    private var addMenuButton: MenuButton? = null
    private var removeSelectedBtn: Button? = null

    private var inventoryObserver: PlacesInventoryObserver? = null
    private var parcelObserver: PlacesParcelObserver? = null
    private var remoteParcelObserver: PlacesRemoteParcelInfoObserver? = null

    private var item: InventoryItem? = null
    private var posGlobal: Vector3d = Vector3d.ZERO
    private var placeInfoType: String = ""
    private var regionId: UUID = UUID.NULL
    private var parcelLocalId: Int = 0
    private var isLandmarkEditModeOn: Boolean = false
    private var tabsCreated: Boolean = false
    private var parcel: ParcelSelection? = null
    private var agentParcelChangedConnection: Any? = null

    init {
        parcelObserver = PlacesParcelObserver(this)
        inventoryObserver = PlacesInventoryObserver(this)
        remoteParcelObserver = PlacesRemoteParcelInfoObserver(this)

        System.err.println("PanelPlaces: init not yet implemented")
    }

    override fun postBuild(): Boolean {
        teleportBtn = getChild<Button>("teleport_btn").also { btn ->
            btn.setClickedCallback { onTeleportButtonClicked() }
        }
        showOnMapBtn = getChild<Button>("map_btn").also { btn ->
            btn.setClickedCallback { onShowOnMapButtonClicked() }
        }
        saveBtn = getChild<Button>("save_btn").also { btn ->
            btn.setClickedCallback { onSaveButtonClicked() }
        }
        cancelBtn = getChild<Button>("cancel_btn").also { btn ->
            btn.setClickedCallback { onCancelButtonClicked() }
        }
        closeBtn = getChild<Button>("close_btn").also { btn ->
            btn.setClickedCallback { onBackButtonClicked() }
        }
        overflowBtn = getChild<MenuButton>("overflow_btn").also { btn ->
            btn.setMouseDownCallback { onOverflowButtonClicked() }
        }
        placeInfoBtn = getChild<Button>("profile_btn").also { btn ->
            btn.setClickedCallback { onProfileButtonClicked() }
        }

        gearMenuButton = getChild<MenuButton>("options_gear_btn").also { btn ->
            btn.setMouseDownCallback { onGearMenuClick() }
        }
        sortingMenuButton = getChild<MenuButton>("sorting_menu_btn").also { btn ->
            btn.setMouseDownCallback { onSortingMenuClick() }
        }
        addMenuButton = getChild<MenuButton>("add_menu_btn").also { btn ->
            btn.setMouseDownCallback { onAddMenuClick() }
        }
        removeSelectedBtn = getChild<Button>("trash_btn").also { btn ->
            btn.setClickedCallback { onRemoveButtonClicked() }
        }

        tabContainer = getChild<TabContainer>("Places Tabs").also { tabs ->
            tabs.setCommitCallback { onTabSelected() }
        }

        filterContainer = getChild("top_menu_panel")

        filterEditor = getChild<FilterEditor>("Filter").also { editor ->
            editor.setCommitOnFocusLost(false)
            editor.setCommitCallback { str -> onFilterEdit(str, false) }
        }

        placeProfile = findChild("panel_place_profile") ?: return false
        landmarkInfo = findChild("panel_landmark_info") ?: return false

        placeProfileBackBtn = placeProfile!!.getChild<Button>("back_btn").also { btn ->
            btn.setClickedCallback { onBackButtonClicked() }
        }
        landmarkInfo!!.getChild<Button>("back_btn").setClickedCallback { onBackButtonClicked() }

        landmarkInfo!!.getChild<Any>("title_editor")
            .setKeystrokeCallback { onEditButtonClicked() }
        landmarkInfo!!.getChild<Any>("notes_editor")
            .setKeystrokeCallback { onEditButtonClicked() }
        landmarkInfo!!.getChild<Any>("folder_combo")
            .setCommitCallback { onEditButtonClicked() }
        landmarkInfo!!.getChild<Button>("edit_btn")
            .setCommitCallback { onEditButtonClicked() }

        System.err.println("PanelPlaces: postBuild (build toggle menus) not yet implemented")

        createTabs()
        updateVerbs()
        return true
    }

    override fun onOpen(key: LLSD) {
        if (placeProfile == null || landmarkInfo == null) return
        if (key.size() == 0) {
            handleParcelManagerState()
            return
        }

        isLandmarkEditModeOn = false
        val keyType = key["type"].asString()

        when (keyType) {
            LANDMARK_TAB_INFO_TYPE -> {
                togglePlaceInfoPanel(false)
                placeInfoType = keyType
                togglePlaceInfoPanel(false)
                onTabSelected()
                updateVerbs()
            }
            CREATE_PICK_TYPE -> {
                val itemId = key["item_id"].asUUID()
                System.err.println("PanelPlaces: onOpen CREATE_PICK_TYPE not yet implemented")
            }
            TELEPORT_HISTORY_TAB_INFO_TYPE -> {
                togglePlaceInfoPanel(false)
                placeInfoType = LANDMARK_TAB_INFO_TYPE
                landmarkInfo!!.setVisible(false)
                System.err.println("PanelPlaces: onOpen TELEPORT_HISTORY_TAB_INFO_TYPE not yet implemented")
                onTabSelected()
                updateVerbs()
            }
            else -> {
                filterEditor?.clear()
                onFilterEdit("", false)
                placeInfoType = keyType
                posGlobal = Vector3d.ZERO
                item = null
                regionId = UUID.NULL
                togglePlaceInfoPanel(true)

                when (placeInfoType) {
                    AGENT_INFO_TYPE -> {
                        placeProfile!!.setInfoType(PanelPlaceInfo.InfoType.AGENT)
                        System.err.println("PanelPlaces: onOpen AGENT_INFO_TYPE not yet implemented")
                    }
                    CREATE_LANDMARK_INFO_TYPE -> {
                        val destFolder = key["dest_folder"].asUUID()
                        landmarkInfo!!.setInfoAndCreateLandmark(destFolder)
                        posGlobal = if (key.has("x") && key.has("y") && key.has("z")) {
                            Vector3d(key["x"].asDouble(), key["y"].asDouble(), key["z"].asDouble())
                        } else {
                            System.err.println("PanelPlaces: onOpen CREATE_LANDMARK_INFO_TYPE (agent global position) not yet implemented")
                            Vector3d.ZERO
                        }
                        landmarkInfo!!.displayParcelInfo(UUID.NULL, posGlobal)
                        saveBtn?.setEnabled(false)
                    }
                    LANDMARK_INFO_TYPE -> {
                        landmarkInfo!!.setInfoType(PanelPlaceInfo.InfoType.LANDMARK)
                        val id = key["id"].asUUID()
                        System.err.println("PanelPlaces: onOpen LANDMARK_INFO_TYPE not yet implemented")
                    }
                    REMOTE_PLACE_INFO_TYPE -> {
                        if (key.has("id")) {
                            val parcelId = key["id"].asUUID()
                            placeProfile!!.setParcelID(parcelId)
                            remoteParcelObserver?.setParcelID(parcelId)
                        } else {
                            posGlobal = Vector3d(key["x"].asDouble(), key["y"].asDouble(), key["z"].asDouble())
                            placeProfile!!.displayParcelInfo(UUID.NULL, posGlobal)
                        }
                        placeProfile!!.setInfoType(PanelPlaceInfo.InfoType.PLACE)
                    }
                    TELEPORT_HISTORY_INFO_TYPE -> {
                        val index = key["id"].asInteger()
                        System.err.println("PanelPlaces: onOpen TELEPORT_HISTORY_INFO_TYPE not yet implemented")
                        placeProfile!!.setInfoType(PanelPlaceInfo.InfoType.TELEPORT_HISTORY)
                        placeProfile!!.displayParcelInfo(UUID.NULL, posGlobal)
                    }
                }
                updateVerbs()
            }
        }

        handleParcelManagerState()
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        System.err.println("PanelPlaces: handleKeyHere not yet implemented")
        return false
    }

    fun changedParcelSelection() {
        System.err.println("PanelPlaces: changedParcelSelection not yet implemented")
    }

    fun createTabs() {
        if (tabsCreated) return
        System.err.println("PanelPlaces: createTabs not yet implemented")
        tabsCreated = true
    }

    fun changedGlobalPos(globalPos: Vector3d) {
        posGlobal = globalPos
        updateVerbs()
    }

    fun showAddedLandmarkInfo(items: Set<UUID>) {
        System.err.println("PanelPlaces: showAddedLandmarkInfo not yet implemented")
    }

    fun setItem(item: InventoryItem?) {
        this.item = item
        System.err.println("PanelPlaces: setItem not yet implemented")
    }

    fun getItem(): InventoryItem? = item
    fun getPlaceInfoType(): String = placeInfoType
    fun tabsCreated(): Boolean = tabsCreated

    override fun notifyParent(info: LLSD): Int {
        System.err.println("PanelPlaces: notifyParent not yet implemented")
        return 0
    }

    fun hideBackBtn() {
        placeProfileBackBtn?.setVisible(false)
    }

    fun resetFilter() {
        filterEditor?.clear()
        onFilterEdit("", false)
    }

    private fun onLandmarkLoaded(landmark: Any) {
        System.err.println("PanelPlaces: onLandmarkLoaded not yet implemented")
    }

    private fun onFilterEdit(searchString: String, forceFilter: Boolean) {
        activePanel?.onSearchEdit(searchString)
        System.err.println("PanelPlaces: onFilterEdit not yet implemented")
    }

    private fun onTabSelected() {
        activePanel = tabContainer?.currentPanel as? PanelPlacesTab
        updateVerbs()
    }

    private fun onTeleportButtonClicked() {
        System.err.println("PanelPlaces: onTeleportButtonClicked not yet implemented")
    }

    private fun onShowOnMapButtonClicked() {
        System.err.println("PanelPlaces: onShowOnMapButtonClicked not yet implemented")
    }

    private fun onEditButtonClicked() {
        System.err.println("PanelPlaces: onEditButtonClicked not yet implemented")
    }

    private fun onSaveButtonClicked() {
        System.err.println("PanelPlaces: onSaveButtonClicked not yet implemented")
    }

    private fun onCancelButtonClicked() {
        isLandmarkEditModeOn = false
        togglePlaceInfoPanel(false)
        updateVerbs()
    }

    private fun onOverflowButtonClicked() {
        System.err.println("PanelPlaces: onOverflowButtonClicked not yet implemented")
    }

    private fun onOverflowMenuItemClicked(param: LLSD) {
        System.err.println("PanelPlaces: onOverflowMenuItemClicked not yet implemented")
    }

    private fun onOverflowMenuItemEnable(param: LLSD): Boolean {
        System.err.println("PanelPlaces: onOverflowMenuItemEnable not yet implemented")
        return false
    }

    private fun onBackButtonClicked() {
        togglePlaceInfoPanel(false)
        updateVerbs()
    }

    private fun onProfileButtonClicked() {
        System.err.println("PanelPlaces: onProfileButtonClicked not yet implemented")
    }

    private fun onGearMenuClick() {
        System.err.println("PanelPlaces: onGearMenuClick not yet implemented")
    }

    private fun onSortingMenuClick() {
        System.err.println("PanelPlaces: onSortingMenuClick not yet implemented")
    }

    private fun onAddMenuClick() {
        System.err.println("PanelPlaces: onAddMenuClick not yet implemented")
    }

    private fun onRemoveButtonClicked() {
        System.err.println("PanelPlaces: onRemoveButtonClicked not yet implemented")
    }

    private fun handleDragAndDropToTrash(
        drop: Boolean,
        cargoType: Int,
        cargoData: Any?,
        accept: IntArray,
    ): Boolean {
        System.err.println("PanelPlaces: handleDragAndDropToTrash not yet implemented")
        return false
    }

    private fun togglePlaceInfoPanel(visible: Boolean) {
        System.err.println("PanelPlaces: togglePlaceInfoPanel not yet implemented")
    }

    override fun onVisibilityChange(newVisibility: Boolean) {
        if (!newVisibility) {
            System.err.println("PanelPlaces: onVisibilityChange (deselect parcel) not yet implemented")
        }
    }

    private fun updateVerbs() {
        System.err.println("PanelPlaces: updateVerbs not yet implemented")
    }

    private fun getCurrentInfoPanel(): PanelPlaceInfo? = when {
        placeProfile?.isVisible() == true -> placeProfile
        landmarkInfo?.isVisible() == true -> landmarkInfo
        else -> null
    }

    private fun handleParcelManagerState() {
        System.err.println("PanelPlaces: handleParcelManagerState not yet implemented")
    }
}
