package com.firestorm.newview

import java.util.UUID

class FSFloaterGroup(seed: LLSD) : LLFloater(seed) {

    private var groupPanel: LLPanelGroup? = null
    private var groupCreatePanel: LLPanelGroupCreate? = null
    private var isCreateGroup: Boolean = false

    override fun postBuild(): Boolean {
        groupPanel = findChild<LLPanelGroup>("panel_group_info_sidetray")
        groupCreatePanel = findChild<LLPanelGroupCreate>("panel_group_creation_sidetray")
        if (groupPanel == null || groupCreatePanel == null) {
            return false
        }
        return true
    }

    override fun onOpen(key: LLSD) {
        // openFloater() sets the key again; force it back to only the group_id component.
        setKey(LLSD().with("group_id", key.get("group_id").asUUID()))
        isCreateGroup = key.has("action") && key.get("action").asString() == "create"

        if (isCreateGroup) {
            groupCreatePanel!!.onOpen(key)
            groupCreatePanel!!.setVisible(true)
            groupPanel!!.setVisible(false)
            setTitle(getString("title_create_group"))
        } else {
            groupPanel!!.onOpen(key)
            groupPanel!!.setVisible(true)
            groupCreatePanel!!.setVisible(false)
            groupPanel!!.getChildView("header_container").setVisible(false)
        }
    }

    fun setGroup(groupId: UUID) {
        groupPanel!!.setGroupID(groupId)
    }

    fun setGroupName(groupName: String) {
        if (isCreateGroup) {
            setTitle(getString("title_create_group"))
        } else {
            if (groupName.isEmpty()) {
                setTitle(getString("title_loading"))
            } else {
                val args = mutableMapOf("[NAME]" to groupName)
                setTitle(getString("title", args))
            }
        }
    }

    fun getGroupPanel(): LLPanelGroup? = groupPanel

    companion object {
        fun openGroupFloater(groupId: UUID): FSFloaterGroup? =
            openGroupFloater(LLSD().with("group_id", groupId))

        fun openGroupFloater(params: LLSD): FSFloaterGroup? {
            if (!params.has("group_id")) return null
            val floater = LLFloaterReg.getTypedInstance<FSFloaterGroup>(
                "fs_group", LLSD().with("group_id", params.get("group_id").asUUID())
            )
            if (floater.getVisible()) {
                floater.onOpen(params)
            } else {
                floater.openFloater(params)
            }
            return floater
        }

        fun closeGroupFloater(groupId: UUID) {
            LLFloaterReg.hideInstance("fs_group", LLSD().with("group_id", groupId))
        }

        fun isFloaterVisible(groupId: UUID): Boolean {
            val inst = LLFloaterReg.findInstance("fs_group", LLSD().with("group_id", groupId))
            return inst?.getVisible() ?: false
        }

        fun getInstance(groupId: UUID): FSFloaterGroup? =
            LLFloaterReg.getTypedInstance("fs_group", LLSD().with("group_id", groupId))

        fun findInstance(groupId: UUID): FSFloaterGroup? =
            LLFloaterReg.findTypedInstance("fs_group", LLSD().with("group_id", groupId))
    }
}
