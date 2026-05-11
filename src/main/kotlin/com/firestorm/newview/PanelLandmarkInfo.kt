package com.firestorm.newview

import java.util.UUID

enum class InfoType { LANDMARK, CREATE_LANDMARK, PLACE }

data class ParcelData(
    val name: String,
    val ownerId: UUID,
    val flags: Int,
    val globalX: Double,
    val globalY: Double,
    val globalZ: Double
)

interface InventoryItem {
    fun getCreatorUUID(): UUID
    fun getPermissions(): Permissions
    fun getCreationDate(): Long
    fun getName(): String
    fun getDescription(): String
}

interface Permissions {
    fun isOwned(): Boolean
    fun isGroupOwned(): Boolean
    fun getGroup(): UUID
    fun getOwner(): UUID
}

interface ViewerInventoryCategory {
    fun getName(): String
    fun getParentUUID(): UUID
    fun getUUID(): UUID
    fun getPreferredType(): Int
}

class UpdateLandmarkParent(
    private val item: Any,
    private val newParentId: UUID
) {
    fun fire(invItemId: UUID) {
        TODO("APR: use JVM equivalent - update item parent in inventory model, notify observers")
    }
}

open class PanelPlaceInfo {
    protected var infoType: InfoType = InfoType.PLACE
    protected var currentTitle: String = ""
    protected var regionTitle: String = ""
    protected var parcelOwner: Any? = null
    protected var parcelName: Any? = null
    protected var maturityRatingIcon: Any? = null
    protected var maturityRatingText: Any? = null
    protected var title: Any? = null

    open fun postBuild(): Boolean = true
    open fun resetLocation() {}
    open fun setInfoType(type: InfoType) { infoType = type }
    open fun processParcelInfo(parcelData: ParcelData) {}
}

class PanelLandmarkInfo : PanelPlaceInfo() {
    private var ownerText: Any? = null
    private var creatorText: Any? = null
    private var createdText: Any? = null
    private var landmarkTitle: Any? = null
    private var landmarkTitleEditor: Any? = null
    private var notesEditor: Any? = null
    private var folderCombo: Any? = null

    private var iconPg: String = ""
    private var iconM: String = ""
    private var iconR: String = ""

    override fun postBuild(): Boolean {
        super.postBuild()
        TODO("GPU: bind ownerText, creatorText, createdText, landmarkTitle, landmarkTitleEditor, notesEditor, folderCombo from UI")
        iconPg = "icon_PG"
        iconM = "icon_M"
        iconR = "icon_R"
        return true
    }

    override fun resetLocation() {
        super.resetLocation()
        val loading = "Loading..."
        TODO("GPU: set ownerText, creatorText, createdText to '$loading'; clear title/notes editors")
    }

    override fun setInfoType(type: InfoType) {
        setInfoTypeWithFolder(type, UUID(0, 0))
    }

    fun setInfoAndCreateLandmark(folderId: UUID) {
        setInfoTypeWithFolder(InfoType.CREATE_LANDMARK, folderId)
    }

    private fun setInfoTypeWithFolder(type: InfoType, folderId: UUID) {
        val isCreateLandmark = type == InfoType.CREATE_LANDMARK

        TODO("GPU: show/hide landmark_info_panel, folder_label, edit_btn, folderCombo based on isCreateLandmark=$isCreateLandmark")

        when (type) {
            InfoType.CREATE_LANDMARK -> {
                currentTitle = "Create Landmark"
                TODO("GPU: show landmarkTitleEditor, hide landmarkTitle; set notesEditor editable")
                TODO("APR: use JVM equivalent - read agent parcel name and position for title default")
                TODO("APR: use JVM equivalent - resolve parcel owner SLURL and set parcelOwner text")
                TODO("APR: use JVM equivalent - if landmark doesn't already exist call createLandmark(folderId)")
            }
            else -> {
                currentTitle = "Landmark"
                TODO("GPU: show landmarkTitle, hide landmarkTitleEditor; set notesEditor read-only")
            }
        }

        populateFoldersList()
        TODO("GPU: set focus to prevent floater losing focus")
        super.setInfoType(type)
    }

    override fun processParcelInfo(parcelData: ParcelData) {
        super.processParcelInfo(parcelData)

        when {
            parcelData.flags and 0x2 != 0 -> {
                TODO("GPU: set maturityRatingIcon to iconR, text to Adult")
            }
            parcelData.flags and 0x1 != 0 -> {
                TODO("GPU: set maturityRatingIcon to iconM, text to Mature")
            }
            else -> {
                TODO("GPU: set maturityRatingIcon to iconPg, text to PG")
            }
        }

        if (parcelData.ownerId != UUID(0, 0)) {
            val ownerSlurl = if (parcelData.flags and 0x4 != 0) {
                "secondlife:///app/group/${parcelData.ownerId}/inspect"
            } else {
                "secondlife:///app/agent/${parcelData.ownerId}/inspect"
            }
            TODO("GPU: set parcelOwner text to $ownerSlurl")
        } else {
            TODO("GPU: set parcelOwner text to 'Public'")
        }

        TODO("APR: use JVM equivalent - notifyParent with update_verbs and global coordinates")
    }

    fun displayItemInfo(item: InventoryItem?) {
        if (item == null) return

        val creatorId = item.getCreatorUUID()
        if (creatorId != UUID(0, 0)) {
            val slurl = "secondlife:///app/agent/$creatorId/inspect"
            TODO("GPU: set creatorText to $slurl")
        } else {
            TODO("GPU: set creatorText to 'Unknown'")
        }

        val perm = item.getPermissions()
        if (perm.isOwned()) {
            val ownerSlurl = if (perm.isGroupOwned()) {
                "secondlife:///app/group/${perm.getGroup()}/inspect"
            } else {
                "secondlife:///app/agent/${perm.getOwner()}/inspect"
            }
            TODO("GPU: set ownerText to $ownerSlurl")
        } else {
            TODO("GPU: set ownerText to 'Public'")
        }

        val timeUtc = item.getCreationDate()
        if (timeUtc == 0L) {
            TODO("GPU: set createdText to 'Unknown'")
        } else {
            TODO("APR: use JVM equivalent - format timeUtc as localized date string and set createdText")
        }

        TODO("GPU: set landmarkTitle and landmarkTitleEditor text to item name; notesEditor to description")
    }

    fun toggleLandmarkEditMode(enabled: Boolean) {
        if (enabled && infoType != InfoType.CREATE_LANDMARK) {
            TODO("GPU: set title text to 'Edit Landmark'")
        } else {
            TODO("GPU: set title text to currentTitle; copy title editor text to title label")
        }

        TODO("GPU: toggle landmarkTitle/Editor visibility and notesEditor read-only; toggle folder controls visibility")
        TODO("GPU: re-set notesEditor text to force color refresh; set focus")
    }

    fun setCanEdit(enabled: Boolean) {
        TODO("GPU: set edit_btn enabled = $enabled")
    }

    fun getLandmarkTitle(): String {
        TODO("GPU: return landmarkTitleEditor text")
    }

    fun getLandmarkNotes(): String {
        TODO("GPU: return notesEditor text")
    }

    fun getLandmarkFolder(): UUID {
        TODO("GPU: return folderCombo selected UUID value")
    }

    fun setLandmarkFolder(id: UUID): Boolean {
        TODO("GPU: select folderCombo item by id, return success")
    }

    private fun createLandmark(folderId: UUID) {
        TODO("APR: use JVM equivalent - read title/notes editors, trim, fall back to parcel/region name, call LandmarkActions.createLandmarkHere")
    }

    private fun populateFoldersList() {
        TODO("APR: use JVM equivalent - collect landmark folders, sort by full name, populate folderCombo with Landmarks folder first")
    }

    companion object {
        fun getFullFolderName(cat: ViewerInventoryCategory): String {
            TODO("APR: use JVM equivalent - walk category parent chain to build full slash-separated path, translating protected-type names via LLTrans")
        }

        fun collectLandmarkFolders(cats: MutableList<ViewerInventoryCategory>) {
            TODO("APR: use JVM equivalent - collect descendant categories of FT_LANDMARK folder, excluding trash")
        }
    }
}
