package com.firestorm.newview

import java.util.UUID

class LLFloaterBanDuration(target: LLSD) : LLFloater(target) {

    private var mAvatarIds: MutableList<UUID> = mutableListOf()
    private var mSelectionCallback: ((MutableList<UUID>, Int) -> Unit)? = null

    override fun postBuild(): Boolean {
        childSetAction("ok_btn") { onClickBan() }
        childSetAction("cancel_btn") { onClickCancel() }

        getChild<LLUICtrl>("ban_duration_radio").setCommitCallback { onClickRadio() }
        getChild<LLRadioGroup>("ban_duration_radio").setSelectedIndex(0)
        getChild<LLUICtrl>("ban_hours").setEnabled(false)

        return true
    }

    private fun onClickRadio() {
        getChild<LLUICtrl>("ban_hours").setEnabled(
            getChild<LLRadioGroup>("ban_duration_radio").getSelectedIndex() != 0
        )
    }

    private fun onClickCancel() {
        closeFloater()
    }

    private fun onClickBan() {
        mSelectionCallback?.let { callback ->
            var time = 0
            if (getChild<LLRadioGroup>("ban_duration_radio").getSelectedIndex() != 0) {
                val hoursSpin = getChild<LLSpinCtrl>("ban_hours")
                time = (LLDate.now().secondsSinceEpoch() + (hoursSpin.getValue().asInteger() * 3600)).toInt()
            }
            callback(mAvatarIds, time)
        }
        closeFloater()
    }

    companion object {
        fun show(callback: (MutableList<UUID>, Int) -> Unit, ids: MutableList<UUID>): LLFloaterBanDuration? {
            val floater = LLFloaterReg.showTypedInstance<LLFloaterBanDuration>("ban_duration")
                ?: return null
            floater.mSelectionCallback = callback
            floater.mAvatarIds = ids
            return floater
        }
    }
}
