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
        System.err.println("UpdateLandmarkParent: fire not yet implemented")
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
        System.err.println("PanelLandmarkInfo: postBuild not yet implemented")
        iconPg = "icon_PG"
        iconM = "icon_M"
        iconR = "icon_R"
        return true
    }

    override fun resetLocation() {
        super.resetLocation()
        val loading = "Loading..."
        System.err.println("PanelLandmarkInfo: resetLocation not yet implemented")
    }

    override fun setInfoType(type: InfoType) {
        setInfoTypeWithFolder(type, UUID(0, 0))
    }

    fun setInfoAndCreateLandmark(folderId: UUID) {
        setInfoTypeWithFolder(InfoType.CREATE_LANDMARK, folderId)
    }

    private fun setInfoTypeWithFolder(type: InfoType, folderId: UUID) {
        val isCreateLandmark = type == InfoType.CREATE_LANDMARK

        System.err.println("PanelLandmarkInfo: setInfoTypeWithFolder not yet implemented")

        when (type) {
            InfoType.CREATE_LANDMARK -> {
                currentTitle = "Create Landmark"
                System.err.println("PanelLandmarkInfo: setInfoTypeWithFolder not yet implemented")
                System.err.println("PanelLandmarkInfo: setInfoTypeWithFolder not yet implemented")
                System.err.println("PanelLandmarkInfo: setInfoTypeWithFolder not yet implemented")
                System.err.println("PanelLandmarkInfo: setInfoTypeWithFolder not yet implemented")
            }
            else -> {
                currentTitle = "Landmark"
                System.err.println("PanelLandmarkInfo: setInfoTypeWithFolder not yet implemented")
            }
        }

        populateFoldersList()
        System.err.println("PanelLandmarkInfo: setInfoTypeWithFolder not yet implemented")
        super.setInfoType(type)
    }

    override fun processParcelInfo(parcelData: ParcelData) {
        super.processParcelInfo(parcelData)

        when {
            parcelData.flags and 0x2 != 0 -> {
                System.err.println("PanelLandmarkInfo: processParcelInfo not yet implemented")
            }
            parcelData.flags and 0x1 != 0 -> {
                System.err.println("PanelLandmarkInfo: processParcelInfo not yet implemented")
            }
            else -> {
                System.err.println("PanelLandmarkInfo: processParcelInfo not yet implemented")
            }
        }

        if (parcelData.ownerId != UUID(0, 0)) {
            val ownerSlurl = if (parcelData.flags and 0x4 != 0) {
                "secondlife:///app/group/${parcelData.ownerId}/inspect"
            } else {
                "secondlife:///app/agent/${parcelData.ownerId}/inspect"
            }
            System.err.println("PanelLandmarkInfo: processParcelInfo not yet implemented")
        } else {
            System.err.println("PanelLandmarkInfo: processParcelInfo not yet implemented")
        }

        System.err.println("PanelLandmarkInfo: processParcelInfo not yet implemented")
    }

    fun displayItemInfo(item: InventoryItem?) {
        if (item == null) return

        val creatorId = item.getCreatorUUID()
        if (creatorId != UUID(0, 0)) {
            val slurl = "secondlife:///app/agent/$creatorId/inspect"
            System.err.println("PanelLandmarkInfo: displayItemInfo not yet implemented")
        } else {
            System.err.println("PanelLandmarkInfo: displayItemInfo not yet implemented")
        }

        val perm = item.getPermissions()
        if (perm.isOwned()) {
            val ownerSlurl = if (perm.isGroupOwned()) {
                "secondlife:///app/group/${perm.getGroup()}/inspect"
            } else {
                "secondlife:///app/agent/${perm.getOwner()}/inspect"
            }
            System.err.println("PanelLandmarkInfo: displayItemInfo not yet implemented")
        } else {
            System.err.println("PanelLandmarkInfo: displayItemInfo not yet implemented")
        }

        val timeUtc = item.getCreationDate()
        if (timeUtc == 0L) {
            System.err.println("PanelLandmarkInfo: displayItemInfo not yet implemented")
        } else {
            System.err.println("PanelLandmarkInfo: displayItemInfo not yet implemented")
        }

        System.err.println("PanelLandmarkInfo: displayItemInfo not yet implemented")
    }

    fun toggleLandmarkEditMode(enabled: Boolean) {
        if (enabled && infoType != InfoType.CREATE_LANDMARK) {
            System.err.println("PanelLandmarkInfo: toggleLandmarkEditMode not yet implemented")
        } else {
            System.err.println("PanelLandmarkInfo: toggleLandmarkEditMode not yet implemented")
        }

        System.err.println("PanelLandmarkInfo: toggleLandmarkEditMode not yet implemented")
        System.err.println("PanelLandmarkInfo: toggleLandmarkEditMode not yet implemented")
    }

    fun setCanEdit(enabled: Boolean) {
        System.err.println("PanelLandmarkInfo: setCanEdit not yet implemented")
    }

    fun getLandmarkTitle(): String {
        return ""
    }

    fun getLandmarkNotes(): String {
        return ""
    }

    fun getLandmarkFolder(): UUID {
        return UUID(0, 0)
    }

    fun setLandmarkFolder(id: UUID): Boolean {
        return false
    }

    private fun createLandmark(folderId: UUID) {
        System.err.println("PanelLandmarkInfo: createLandmark not yet implemented")
    }

    private fun populateFoldersList() {
        System.err.println("PanelLandmarkInfo: populateFoldersList not yet implemented")
    }

    companion object {
        fun getFullFolderName(cat: ViewerInventoryCategory): String {
            return ""
        }

        fun collectLandmarkFolders(cats: MutableList<ViewerInventoryCategory>) {
            System.err.println("PanelLandmarkInfo: collectLandmarkFolders not yet implemented")
        }
    }
}
