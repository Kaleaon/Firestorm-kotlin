package com.firestorm.newview

import java.util.UUID

class LLFloaterGroupInvite protected constructor(groupId: UUID = NULL_UUID) : LLFloater(groupId) {

    private val mImpl: Impl = Impl(groupId)

    init {
        val floaterHeaderSize = getHeaderHeight()
        val invitePanel = LLPanelGroupInvite(groupId)

        mImpl.mInvitePanelp = invitePanel

        val contents = invitePanel.getRect().apply { mTop -= floaterHeaderSize }

        setTitle(invitePanel.getString("GroupInvitation"))
        invitePanel.setCloseCallback(Impl.Companion::closeFloater, this)
        invitePanel.setRect(contents)
        addChild(invitePanel)
    }

    override fun onDestroy() {
        if (mImpl.mGroupID != NULL_UUID) {
            Impl.sInstances.remove(mImpl.mGroupID)
        }
        super.onDestroy()
    }

    private class Impl(val mGroupID: UUID) {
        var mInvitePanelp: LLPanelGroupInvite? = null

        companion object {
            val sInstances: MutableMap<UUID, LLFloaterGroupInvite> = mutableMapOf()

            fun closeFloater(data: Any?) {
                val floater = data as? LLFloaterGroupInvite ?: return
                floater.closeFloater()
            }
        }
    }

    companion object {
        @JvmStatic
        fun showForGroup(groupId: UUID, agentIds: MutableList<UUID>? = null, requestUpdate: Boolean = true) {
            if (groupId == NULL_UUID) {
                return
            }

            var fgi = Impl.sInstances[groupId]

            if (requestUpdate) {
                gAgent.sendAgentDataUpdateRequest()
                LLGroupMgr.getInstance().clearGroupData(groupId)
            }

            if (fgi == null) {
                fgi = LLFloaterGroupInvite(groupId)
                val floaterHeaderSize = LLFloater.getDefaultParams().headerHeight
                val contents = fgi.mImpl.mInvitePanelp!!.getRect().apply { mTop += floaterHeaderSize }
                fgi.setRect(contents)
                fgi.getDragHandle()?.setRect(contents)
                fgi.getDragHandle()?.setTitle(fgi.mImpl.mInvitePanelp!!.getString("GroupInvitation"))
                Impl.sInstances[groupId] = fgi
                fgi.mImpl.mInvitePanelp!!.clear()
            }

            if (agentIds != null) {
                fgi.mImpl.mInvitePanelp!!.addUsers(agentIds)
            }

            fgi.center()
            fgi.openFloater()
            fgi.mImpl.mInvitePanelp!!.update()
        }
    }
}
