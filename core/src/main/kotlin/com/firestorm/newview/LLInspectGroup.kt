package com.firestorm.newview

import java.util.UUID

object LLInspectGroupUtil {
    fun registerFloater() {
        System.err.println("LLInspectGroupUtil: registerFloater not yet implemented")
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
        System.err.println("LLInspectGroup: onClose not yet implemented")
    }

    fun setGroupID(groupId: UUID) {
        System.err.println("LLInspectGroup: setGroupID not yet implemented")
    }

    fun requestUpdate() {
        if (mGroupID == UUID(0L, 0L)) {
            System.err.println("LLInspectGroup: requestUpdate startup check not yet implemented")
        }

        clearGroupFields()

        System.err.println("LLInspectGroup: requestUpdate group data fetch not yet implemented")
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
        System.err.println("LLInspectGroup: processGroupData not yet implemented")
    }

    fun onClickViewProfile() {
        closeFloater(false)
        System.err.println("LLInspectGroup: onClickViewProfile not yet implemented")
    }

    fun onClickJoin() {
        closeFloater(false)
        System.err.println("LLInspectGroup: onClickJoin not yet implemented")
    }

    fun onClickLeave() {
        closeFloater(false)
        System.err.println("LLInspectGroup: onClickLeave not yet implemented")
    }

    private fun extractGroupId(data: Any?): UUID {
        System.err.println("LLInspectGroup: extractGroupId not yet implemented")
        return UUID(0L, 0L)
    }

    private fun setChildValue(childName: String, value: Any) {
        System.err.println("LLInspectGroup: setChildValue not yet implemented")
    }

    private fun setChildVisible(childName: String, visible: Boolean) {
        System.err.println("LLInspectGroup: setChildVisible not yet implemented")
    }

    private fun registerCommitCallback(name: String, callback: () -> Unit) {
        System.err.println("LLInspectGroup: registerCommitCallback not yet implemented")
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
