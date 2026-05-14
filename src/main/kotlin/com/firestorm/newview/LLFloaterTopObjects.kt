package com.firestorm.newview

import java.util.UUID

enum class LandStatFlags(val value: UInt) {
    STAT_FILTER_BY_PARCEL(0x00000001u),
    STAT_FILTER_BY_OWNER(0x00000002u),
    STAT_FILTER_BY_OBJECT(0x00000004u),
    STAT_FILTER_BY_PARCEL_NAME(0x00000008u),
    STAT_REQUEST_LAST_ENTRY(0x80000000u),
}

enum class LandStatReportType(val value: Int) {
    STAT_REPORT_TOP_SCRIPTS(0),
    STAT_REPORT_TOP_COLLIDERS(1),
}

private const val OBJECT_NOT_AVATAR_NAME = "(???) (???)"

class LLFloaterTopObjects(key: LLSD) : LLFloater(key) {

    private var mMethod: String = ""
    private var mObjectListData: LLSD = LLSD()
    private val mObjectListIDs: MutableList<UUID> = mutableListOf()
    private var mCurrentMode: UInt = LandStatReportType.STAT_REPORT_TOP_SCRIPTS.value.toUInt()
    private var mFlags: UInt = 0u
    private var mFilter: String = ""
    private var mInitialized: Boolean = false
    private var mtotalScore: Float = 0f
    private var mObjectsScrollList: LLScrollListCtrl? = null

    init {
        mCommitCallbackRegistrar.add("TopObjects.ShowBeacon") { onClickShowBeacon() }
        mCommitCallbackRegistrar.add("TopObjects.ReturnSelected") { onReturnSelected() }
        mCommitCallbackRegistrar.add("TopObjects.ReturnAll") { onReturnAll() }
        mCommitCallbackRegistrar.add("TopObjects.Refresh") { onRefresh() }
        mCommitCallbackRegistrar.add("TopObjects.GetByObjectName") { onGetByObjectName() }
        mCommitCallbackRegistrar.add("TopObjects.GetByOwnerName") { onGetByOwnerName() }
        mCommitCallbackRegistrar.add("TopObjects.GetByParcelName") { onGetByParcelName() }
        mCommitCallbackRegistrar.add("TopObjects.CommitObjectsList") { onCommitObjectsList() }
        mCommitCallbackRegistrar.add("TopObjects.TeleportToObject") { onTeleportToObject() }
        mCommitCallbackRegistrar.add("TopObjects.Kick") { onKick() }
        mCommitCallbackRegistrar.add("TopObjects.Profile") { onProfile() }
        mCommitCallbackRegistrar.add("TopObjects.ScriptInfo") { onScriptInfo() }
    }

    override fun postBuild(): Boolean {
        mObjectsScrollList = getChild<LLScrollListCtrl>("objects_list")
        mObjectsScrollList?.setFocus(true)
        mObjectsScrollList?.setDoubleClickCallback { onDoubleClickObjectsList() }
        mObjectsScrollList?.setCommitOnSelectionChange(true)
        mObjectsScrollList?.setCommitCallback { onSelectionChanged() }

        setDefaultBtn("show_beacon_btn")

        mCurrentMode = LandStatReportType.STAT_REPORT_TOP_SCRIPTS.value.toUInt()
        mFlags = 0u
        mFilter = ""

        return true
    }

    companion object {
        fun setMode(mode: UInt) {
            val instance = LLFloaterReg.getTypedInstance<LLFloaterTopObjects>("top_objects") ?: return
            instance.mCurrentMode = mode
        }

        fun handleLandReply(msg: LLMessageSystem, data: Any?) {
            val instance = LLFloaterReg.getTypedInstance<LLFloaterTopObjects>("top_objects")
            if (instance != null && instance.isInVisibleChain()) {
                instance.handleReply(msg, data)
                // Top scripts can initially return empty results even when they exist
                if (instance.mObjectListIDs.isEmpty() && !instance.mInitialized) {
                    instance.onRefresh()
                    instance.mInitialized = true
                }
            } else {
                val regionInfoFloater = LLFloaterReg.getTypedInstance<LLFloaterRegionInfo>("region_info")
                regionInfoFloater?.enableTopButtons()
            }
        }

        private fun callbackReturnAll(notification: LLSD, response: LLSD): Boolean {
            val option = LLNotificationsUtil.getSelectedOption(notification, response)
            val instance = LLFloaterReg.getTypedInstance<LLFloaterTopObjects>("top_objects") ?: return false
            if (option == 0) {
                instance.returnObjects(true)
            }
            return false
        }
    }

    fun handleReply(msg: LLMessageSystem, data: Any?) {
        System.err.println("LLFloaterTopObjects: handleReply not yet implemented")
    }

    fun clearList() {
        val list = childGetListInterface("objects_list")
        list?.operateOnAll(LLCtrlListInterface.OP_DELETE)

        mObjectListData = LLSD()
        mObjectListIDs.clear()
        mtotalScore = 0f

        onSelectionChanged()
    }

    fun updateSelectionInfo() {
        val list = getChild<LLScrollListCtrl>("objects_list") ?: return
        val objectId = list.getCurrentID()
        if (objectId == null || objectId == UUID(0L, 0L)) return

        val avName = LLAvatarNameCache.get(objectId)
        if (avName != null) {
            val isAvatar = avName.getDisplayName() != OBJECT_NOT_AVATAR_NAME
            getChild<LLButton>("profile_btn")?.setEnabled(isAvatar)
            getChild<LLButton>("estate_kick_btn")?.setEnabled(isAvatar && objectId != gAgentID)
        } else {
            getChild<LLButton>("profile_btn")?.setEnabled(false)
            getChild<LLButton>("estate_kick_btn")?.setEnabled(false)
            LLAvatarNameCache.get(objectId) { avatarId, avNameResult -> onAvatarCheck(avatarId, avNameResult) }
        }

        val objectIdString = objectId.toString()
        getChild<LLUICtrl>("id_editor")?.setValue(LLSD(objectIdString))

        val sli = list.getFirstSelected()
        if (sli != null) {
            getChild<LLUICtrl>("object_name_editor")?.setValue(sli.getColumn(1)?.getValue()?.asString())
            getChild<LLUICtrl>("owner_name_editor")?.setValue(sli.getColumn(2)?.getValue()?.asString())
            getChild<LLUICtrl>("parcel_name_editor")?.setValue(sli.getColumn(4)?.getValue()?.asString())
        }
    }

    fun onRefresh() {
        val mode = mCurrentMode
        val flags = mFlags
        val filter = mFilter
        clearList()

        System.err.println("LLFloaterTopObjects: onRefresh not yet implemented")
    }

    fun disableRefreshBtn() {
        getChildView("refresh_btn")?.setEnabled(false)
    }

    private fun initColumns(list: LLCtrlListInterface) {
        System.err.println("LLFloaterTopObjects: initColumns not yet implemented")
    }

    private fun onCommitObjectsList() {
        updateSelectionInfo()
    }

    private fun onSelectionChanged() {
        val enabled = mObjectsScrollList?.getNumSelected() == 1
        childSetEnabled("teleport_to_btn", enabled)
        childSetEnabled("profile_btn", enabled)
        childSetEnabled("script_info_btn", enabled)
        childSetEnabled("estate_kick_btn", enabled)
    }

    private fun onDoubleClickObjectsList() {
        showBeacon()
    }

    private fun onClickShowBeacon() {
        showBeacon()
    }

    private fun returnObjects(all: Boolean) {
        System.err.println("LLFloaterTopObjects: returnObjects not yet implemented")
    }

    private fun onReturnAll() {
        LLNotificationsUtil.add("ReturnAllTopObjects", LLSD(), LLSD()) { notification, response ->
            callbackReturnAll(notification, response)
        }
    }

    private fun onReturnSelected() {
        returnObjects(false)
    }

    private fun onGetByOwnerName() {
        mFlags = LandStatFlags.STAT_FILTER_BY_OWNER.value
        mFilter = getChild<LLUICtrl>("owner_name_editor")?.getValue()?.asString() ?: ""
        onRefresh()
    }

    private fun onGetByObjectName() {
        mFlags = LandStatFlags.STAT_FILTER_BY_OBJECT.value
        mFilter = getChild<LLUICtrl>("object_name_editor")?.getValue()?.asString() ?: ""
        onRefresh()
    }

    private fun onGetByParcelName() {
        mFlags = LandStatFlags.STAT_FILTER_BY_PARCEL_NAME.value
        mFilter = getChild<LLUICtrl>("parcel_name_editor")?.getValue()?.asString() ?: ""
        onRefresh()
    }

    private fun showBeacon() {
        val list = getChild<LLScrollListCtrl>("objects_list") ?: return
        val firstSelected = list.getFirstSelected() ?: return

        val name = firstSelected.getColumn(1)?.getValue()?.asString() ?: return
        val posString = firstSelected.getColumn(3)?.getValue()?.asString() ?: return

        System.err.println("LLFloaterTopObjects: showBeacon not yet implemented")
    }

    private fun onTeleportToObject() {
        val firstSelected = mObjectsScrollList?.getFirstSelected() ?: return
        val posString = firstSelected.getColumn(3)?.getValue()?.asString() ?: return

        System.err.println("LLFloaterTopObjects: onTeleportToObject not yet implemented")
    }

    private fun onKick() {
        val firstSelected = mObjectsScrollList?.getFirstSelected() ?: return
        val objectId = firstSelected.getUUID()
        LLAvatarActions.estateKick(objectId)
    }

    private fun onProfile() {
        val firstSelected = mObjectsScrollList?.getFirstSelected() ?: return
        val objectId = firstSelected.getUUID()
        LLAvatarActions.showProfile(objectId)
    }

    private fun onAvatarCheck(avatarId: UUID, avName: LLAvatarName) {
        val firstSelected = mObjectsScrollList?.getFirstSelected() ?: return
        if (firstSelected.getUUID() == avatarId) {
            val isAvatar = avName.getDisplayName() != OBJECT_NOT_AVATAR_NAME
            getChild<LLButton>("profile_btn")?.setEnabled(isAvatar)
            getChild<LLButton>("estate_kick_btn")?.setEnabled(isAvatar && avatarId != gAgentID)
        }
    }

    private fun onScriptInfo() {
        val firstSelected = mObjectsScrollList?.getFirstSelected() ?: return
        val objectId = firstSelected.getUUID()
        LLAvatarActions.getScriptInfo(objectId)
    }
}
