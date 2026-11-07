package com.firestorm.newview

class LLFloaterAddPaymentMethod(key: LLSD) : LLFloater(key) {

    override fun postBuild(): Boolean {
        setCanDrag(false)
        getChild<LLButton>("continue_btn").setCommitCallback { onContinueBtn() }
        getChild<LLButton>("close_btn").setCommitCallback { onCloseBtn() }
        return true
    }

    override fun onOpen(key: LLSD) {
        centerOnScreen()
    }

    private fun onContinueBtn() {
        closeFloater()
        LLNotificationsUtil.add("AddPaymentMethod") { notif, resp ->
            val opt = LLNotificationsUtil.getSelectedOption(notif, resp)
            if (opt == 0) {
                LLWeb.loadURL(getString("continue_url"))
            }
        }
    }

    private fun onCloseBtn() {
        closeFloater()
    }

    private fun centerOnScreen() {
        val windowSize = LLUI.getInstance().getWindowSize()
        centerWithin(LLRect(0, 0, Math.round(windowSize.x), Math.round(windowSize.y)))
    }
}
