package com.firestorm.newview

import java.util.UUID

class LLFloaterTestInspectors(seed: LLSD) : LLFloater(seed) {

    init {
        mCommitCallbackRegistrar.add("ShowAvatarInspector") { ctrl, avatarId -> showAvatarInspector(ctrl, avatarId) }
        mCommitCallbackRegistrar.add("ShowObjectInspector") { ctrl, objectId -> showObjectInspector(ctrl, objectId) }
    }

    override fun postBuild(): Boolean {
        getChild<LLUICtrl>("intentionally-not-found")?.setEnabled(true)

        getChild<LLUICtrl>("avatar_3d_btn")?.setCommitCallback { onClickAvatar3D() }
        getChild<LLUICtrl>("object_2d_btn")?.setCommitCallback { onClickObject2D() }
        getChild<LLUICtrl>("object_3d_btn")?.setCommitCallback { onClickObject3D() }
        getChild<LLUICtrl>("group_btn")?.setCommitCallback { onClickGroup() }
        getChild<LLUICtrl>("place_btn")?.setCommitCallback { onClickPlace() }
        getChild<LLUICtrl>("event_btn")?.setCommitCallback { onClickEvent() }

        return super.postBuild()
    }

    private fun showAvatarInspector(ctrl: LLUICtrl?, avatarId: LLSD) {
        var id: UUID = UUID(0L, 0L)
        if (LLStartup.getStartupState() >= LLStartup.STATE_STARTED) {
            id = avatarId.asUUID()
        }
        LLFloaterReg.showInstance("inspect_avatar", LLSD().with("avatar_id", id))
    }

    private fun showObjectInspector(ctrl: LLUICtrl?, objectId: LLSD) {
        LLFloaterReg.showInstance("inspect_object", LLSD().with("object_id", objectId))
    }

    private fun onClickAvatar2D() {}

    private fun onClickAvatar3D() {}

    private fun onClickObject2D() {}

    private fun onClickObject3D() {}

    private fun onClickGroup() {}

    private fun onClickPlace() {}

    private fun onClickEvent() {}
}
