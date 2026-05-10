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
        TODO("APR: use JVM equivalent - update global position and clean up observer")
    }

    fun setParcelID(parcelId: UUID) {
        if (parcelId != UUID.NULL) {
            parcelIds.add(parcelId)
            TODO("APR: use JVM equivalent - register with RemoteParcelInfoProcessor and send request")
        }
    }

    fun setErrorStatus(status: Int, reason: String) {
        TODO("APR: use JVM equivalent - log HTTP error from remote parcel info request")
    }

    fun cleanup() {
        TODO("APR: use JVM equivalent - remove all in-flight observer registrations")
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

        TODO("APR: use JVM equivalent - register inventory observer and agent parcel changed callback")
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

        TODO("APR: use JVM equivalent - build place/landmark toggle menus from XML")

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
                TODO("APR: use JVM equivalent - get landmark and create pick via landmarks panel")
            }
            TELEPORT_HISTORY_TAB_INFO_TYPE -> {
                togglePlaceInfoPanel(false)
                placeInfoType = LANDMARK_TAB_INFO_TYPE
                landmarkInfo!!.setVisible(false)
                TODO("APR: use JVM equivalent - select teleport history tab in container")
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
                        TODO("APR: use JVM equivalent - get region ID from agent")
                    }
                    CREATE_LANDMARK_INFO_TYPE -> {
                        val destFolder = key["dest_folder"].asUUID()
                        landmarkInfo!!.setInfoAndCreateLandmark(destFolder)
                        posGlobal = if (key.has("x") && key.has("y") && key.has("z")) {
                            Vector3d(key["x"].asDouble(), key["y"].asDouble(), key["z"].asDouble())
                        } else {
                            TODO("APR: use JVM equivalent - get agent global position")
                        }
                        landmarkInfo!!.displayParcelInfo(UUID.NULL, posGlobal)
                        saveBtn?.setEnabled(false)
                    }
                    LANDMARK_INFO_TYPE -> {
                        landmarkInfo!!.setInfoType(PanelPlaceInfo.InfoType.LANDMARK)
                        val id = key["id"].asUUID()
                        TODO("APR: use JVM equivalent - load inventory item and check edit permissions")
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
                        TODO("APR: use JVM equivalent - get global pos from teleport history storage at index")
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
        TODO("APR: use JVM equivalent - CTRL-F focuses filter editor")
    }

    fun changedParcelSelection() {
        TODO("APR: use JVM equivalent - update place profile with current parcel")
    }

    fun createTabs() {
        if (tabsCreated) return
        TODO("APR: use JVM equivalent - create My Landmarks and Teleport History tabs")
        tabsCreated = true
    }

    fun changedGlobalPos(globalPos: Vector3d) {
        posGlobal = globalPos
        updateVerbs()
    }

    fun showAddedLandmarkInfo(items: Set<UUID>) {
        TODO("APR: use JVM equivalent - show landmark info panel for newly created landmark")
    }

    fun setItem(item: InventoryItem?) {
        this.item = item
        TODO("APR: use JVM equivalent - load landmark and update place info panel")
    }

    fun getItem(): InventoryItem? = item
    fun getPlaceInfoType(): String = placeInfoType
    fun tabsCreated(): Boolean = tabsCreated

    override fun notifyParent(info: LLSD): Int {
        TODO("APR: use JVM equivalent - handle child panel notifications (e.g. back button)")
    }

    fun hideBackBtn() {
        placeProfileBackBtn?.setVisible(false)
    }

    fun resetFilter() {
        filterEditor?.clear()
        onFilterEdit("", false)
    }

    private fun onLandmarkLoaded(landmark: Any) {
        TODO("APR: use JVM equivalent - display landmark parcel info once asset loaded")
    }

    private fun onFilterEdit(searchString: String, forceFilter: Boolean) {
        activePanel?.onSearchEdit(searchString)
        TODO("APR: use JVM equivalent - propagate filter to active tab panel")
    }

    private fun onTabSelected() {
        activePanel = tabContainer?.currentPanel as? PanelPlacesTab
        updateVerbs()
    }

    private fun onTeleportButtonClicked() {
        TODO("APR: use JVM equivalent - teleport to posGlobal or landmark")
    }

    private fun onShowOnMapButtonClicked() {
        TODO("APR: use JVM equivalent - open world map floater at posGlobal")
    }

    private fun onEditButtonClicked() {
        TODO("APR: use JVM equivalent - enable landmark edit mode and update verbs")
    }

    private fun onSaveButtonClicked() {
        TODO("APR: use JVM equivalent - save landmark info changes")
    }

    private fun onCancelButtonClicked() {
        isLandmarkEditModeOn = false
        togglePlaceInfoPanel(false)
        updateVerbs()
    }

    private fun onOverflowButtonClicked() {
        TODO("APR: use JVM equivalent - show overflow toggleable menu")
    }

    private fun onOverflowMenuItemClicked(param: LLSD) {
        TODO("APR: use JVM equivalent - dispatch overflow menu action (copy SLURL, create pick, etc.)")
    }

    private fun onOverflowMenuItemEnable(param: LLSD): Boolean {
        TODO("APR: use JVM equivalent - return enabled state for overflow menu item")
    }

    private fun onBackButtonClicked() {
        togglePlaceInfoPanel(false)
        updateVerbs()
    }

    private fun onProfileButtonClicked() {
        TODO("APR: use JVM equivalent - show avatar/place profile")
    }

    private fun onGearMenuClick() {
        TODO("APR: use JVM equivalent - show gear toggleable menu")
    }

    private fun onSortingMenuClick() {
        TODO("APR: use JVM equivalent - show sorting toggleable menu")
    }

    private fun onAddMenuClick() {
        TODO("APR: use JVM equivalent - show add landmark/folder menu")
    }

    private fun onRemoveButtonClicked() {
        TODO("APR: use JVM equivalent - remove selected landmark or folder")
    }

    private fun handleDragAndDropToTrash(
        drop: Boolean,
        cargoType: Int,
        cargoData: Any?,
        accept: IntArray,
    ): Boolean {
        TODO("APR: use JVM equivalent - validate and perform DnD delete of landmark/folder")
    }

    private fun togglePlaceInfoPanel(visible: Boolean) {
        TODO("APR: use JVM equivalent - show/hide place profile vs landmark info panel, manage tab visibility")
    }

    override fun onVisibilityChange(newVisibility: Boolean) {
        if (!newVisibility) {
            TODO("APR: use JVM equivalent - deselect parcel when panel hides")
        }
    }

    private fun updateVerbs() {
        TODO("APR: use JVM equivalent - set enabled/visible state of all action buttons based on placeInfoType")
    }

    private fun getCurrentInfoPanel(): PanelPlaceInfo? = when {
        placeProfile?.isVisible() == true -> placeProfile
        landmarkInfo?.isVisible() == true -> landmarkInfo
        else -> null
    }

    private fun handleParcelManagerState() {
        TODO("APR: use JVM equivalent - register/deregister parcel observer based on current info type")
    }
}
