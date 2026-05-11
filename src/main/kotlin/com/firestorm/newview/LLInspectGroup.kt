package com.firestorm.newview

import java.util.UUID

object LLInspectGroupUtil {
    fun registerFloater() {
        TODO("APR: use JVM equivalent of LLFloaterReg::add(\"inspect_group\", \"inspect_group.xml\", builder)")
    }
}

class LLInspectGroup(key: Any?) : LLInspect(key), LLGroupMgrObserver {

    private var mGroupID: UUID = UUID(0L, 0L)

    init {
        registerCommitCallback("InspectGroup.ViewProfile") { onClickViewProfile() }
        registerCommitCallback("InspectGroup.Join")        { onClickJoin() }
        registerCommitCallback("InspectGroup.Leave")       { onClickLeave() }
    }

    override fun onOpen(data: Any?) {
        super.onOpen(data)
        val groupId = extractGroupId(data)
        setGroupID(groupId)
        repositionInspector(data)
        requestUpdate()
    }

    fun onClose(appQuitting: Boolean) {
        TODO("APR: use JVM equivalent of LLGroupMgr::removeObserver(this)")
    }

    fun setGroupID(groupId: UUID) {
        TODO("APR: remove old observer, set mGroupID = groupId, add new observer via LLGroupMgr")
    }

    fun requestUpdate() {
        if (mGroupID == UUID(0L, 0L)) {
            TODO("APR: check startup state; close floater if fully started with null group ID")
        }

        clearGroupFields()

        TODO("APR: use JVM equivalent of LLGroupMgr::getGroupData / sendGroupPropertiesRequest, then gCacheName->getGroup for fast name lookup")
    }

    private fun clearGroupFields() {
        setChildValue("group_name", "")
        setChildValue("group_subtitle", "")
        setChildValue("group_details", "")
        setChildValue("group_cost", "")
        setChildVisible("view_profile_btn", true)
        setChildVisible("leave_btn", false)
        setChildVisible("join_btn", false)
    }

    fun nameUpdatedCallback(id: UUID, name: String, isGroup: Boolean) {
        if (id == mGroupID) {
            setChildValue("group_name", "<nolink>$name</nolink>")
        }
    }

    override fun changed(gc: LLGroupChange) {
        if (gc == LLGroupChange.GC_PROPERTIES) {
            processGroupData()
        }
    }

    fun processGroupData() {
        TODO("APR: use JVM equivalent of LLGroupMgr::getGroupData to fill subtitle, details, icon, cost, join/leave buttons")
    }

    fun onClickViewProfile() {
        closeFloater(false)
        TODO("APR: use JVM equivalent of LLGroupActions::show(mGroupID)")
    }

    fun onClickJoin() {
        closeFloater(false)
        TODO("APR: use JVM equivalent of LLGroupActions::join(mGroupID)")
    }

    fun onClickLeave() {
        closeFloater(false)
        TODO("APR: use JVM equivalent of LLGroupActions::leave(mGroupID)")
    }

    private fun extractGroupId(data: Any?): UUID {
        TODO("APR: use JVM equivalent of LLSD data[\"group_id\"].asUUID()")
    }

    private fun setChildValue(childName: String, value: Any) {
        TODO("APR: use JVM equivalent of getChild<LLUICtrl>(childName)->setValue(value)")
    }

    private fun setChildVisible(childName: String, visible: Boolean) {
        TODO("APR: use JVM equivalent of getChild<LLUICtrl>(childName)->setVisible(visible)")
    }

    private fun registerCommitCallback(name: String, callback: () -> Unit) {
        TODO("APR: use JVM equivalent of mCommitCallbackRegistrar.add(name, callback)")
    }
}

interface LLGroupMgrObserver {
    fun changed(gc: LLGroupChange)
}

enum class LLGroupChange {
    GC_PROPERTIES,
    GC_MEMBER_DATA,
    GC_ROLE_DATA,
    GC_TITLES,
    GC_ALL,
}
