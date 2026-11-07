package com.firestorm.newview

import com.firestorm.ui.LLFloater
import com.firestorm.ui.LLUICtrl
import com.firestorm.llsd.LLSD

class LLFloaterPreviewTrash(key: LLSD) : LLFloater(key) {

    override fun postBuild(): Boolean {
        getChild<LLUICtrl>("empty_btn").setCommitCallback { onClickEmpty() }
        getChild<LLUICtrl>("cancel_btn").setCommitCallback { onClickCancel() }
        // Always center — purchases are important and should be on-screen regardless of window resize.
        center()
        return true
    }

    protected fun onClickEmpty() {
        gInventory.emptyFolderType("PurgeSelectedItems", LLFolderType.FT_TRASH)
        closeFloater()
    }

    protected fun onClickCancel() {
        closeFloater()
    }

    companion object {
        fun show() {
            LLFloaterReg.showTypedInstance<LLFloaterPreviewTrash>("preview_trash", LLSD(), true)
        }

        fun isVisible(): Boolean {
            return LLFloaterReg.instanceVisible("preview_trash")
        }
    }
}
