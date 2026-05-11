package com.firestorm.newview

class LLFloaterWhiteListEntry(key: LLSD) : LLFloater(key) {

    private var mWhiteListEdit: LLLineEditor? = null

    override fun postBuild(): Boolean {
        mWhiteListEdit = getChild<LLLineEditor>("whitelist_entry")

        childSetAction("cancel_btn") { onBtnCancel() }
        childSetAction("ok_btn") { onBtnOK() }

        setDefaultBtn("ok_btn")
        return true
    }

    private fun onBtnOK() {
        val panel = LLFloaterReg.getTypedInstance<LLFloaterMediaSettings>("media_settings")
            ?.getPanelSecurity()
        if (panel != null) {
            val whiteListItem = mWhiteListEdit?.getText() ?: ""
            panel.addWhiteListEntry(whiteListItem)
            panel.updateWhitelistEnableStatus()
        }
        closeFloater()
    }

    private fun onBtnCancel() {
        closeFloater()
    }
}
