package com.firestorm.newview

class LLFloaterNewFeatureNotification(key: LLSD) : LLFloater(key) {

    override fun postBuild(): Boolean {
        setCanDrag(false)
        getChild<LLButton>("close_btn")?.setCommitCallback { onCloseBtn() }

        if (getKey().isString()) {
            val titleCtrl = "title_txt"
            val descCtrl  = "description_txt"
            val feature   = "_" + getKey().asString()

            if (hasString(titleCtrl + feature)) {
                getChild<LLUICtrl>(titleCtrl)?.setValue(getString(titleCtrl + feature))
                getChild<LLUICtrl>(descCtrl)?.setValue(getString(descCtrl + feature))
            }
        }

        if (getKey().asString() == "gltf") {
            val rect = getRect()
            reshape(rect.width + 90, rect.height + 45)
        }

        return true
    }

    override fun onOpen(key: LLSD) {
        centerOnScreen()
    }

    private fun onCloseBtn() {
        closeFloater()
    }

    private fun centerOnScreen() {
        val windowSize = LLUI.getInstance().getWindowSize()
        centerWithin(LLRect(0, 0, Math.round(windowSize.x), Math.round(windowSize.y)))
        val parent = getParent() as? LLFloaterView
        parent?.bringToFront(this)
    }
}
