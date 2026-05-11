package com.firestorm.newview

class LLFloaterURLEntry private constructor(parent: LLHandle<LLPanel>) : LLFloater(LLSD()) {

    private var mMediaURLEdit: LLComboBox? = null
    private val mPanelLandMediaHandle: LLHandle<LLPanel> = parent

    init {
        buildFromFile("floater_url_entry.xml")
    }

    override fun postBuild(): Boolean {
        mMediaURLEdit = getChild<LLComboBox>("media_entry")

        childSetAction("cancel_btn") { onBtnCancel(this) }
        childSetAction("clear_btn") { onBtnClear(this) }

        val parcelHistory = LLURLHistory.getURLHistory("parcel")
        getChildView("clear_btn")?.setEnabled(parcelHistory.size() > 0)

        childSetAction("ok_btn") { onBtnOK(this) }

        setDefaultBtn("ok_btn")
        buildURLHistory()

        return true
    }

    private fun buildURLHistory() {
        val urlList = childGetListInterface("media_entry")
        urlList?.operateOnAll(LLCtrlListInterface.OP_DELETE)

        val parcelHistory = LLURLHistory.getURLHistory("parcel")
        for (entry in parcelHistory.arrayIterator()) {
            urlList?.addSimpleElement(entry.asString())
        }
    }

    fun headerFetchComplete(status: Int, mimeType: String) {
        val panelMedia = mPanelLandMediaHandle.get() as? LLPanelLandMedia
        if (panelMedia != null) {
            panelMedia.setMediaType(mimeType)
            panelMedia.setMediaURL(mMediaURLEdit?.getValue()?.asString() ?: "")
        }

        getChildView("loading_label")?.setVisible(false)
        closeFloater()
    }

    fun addURLToCombobox(mediaUrl: String): Boolean {
        if (mMediaURLEdit?.setSimple(mediaUrl) != true && mediaUrl.isNotEmpty()) {
            mMediaURLEdit?.add(mediaUrl)
            mMediaURLEdit?.setSimple(mediaUrl)
            return true
        }
        return false
    }

    override fun onClose(appQuitting: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    companion object {
        private var sInstance: LLFloaterURLEntry? = null

        fun show(panelLandMediaHandle: LLHandle<LLPanel>, mediaUrl: String): LLHandle<LLFloater> {
            if (sInstance == null) {
                sInstance = LLFloaterURLEntry(panelLandMediaHandle)
            }
            sInstance!!.openFloater()
            sInstance!!.addURLToCombobox(mediaUrl)
            return sInstance!!.getHandle()
        }

        private fun onBtnOK(self: LLFloaterURLEntry) {
            var mediaUrl = self.mMediaURLEdit?.getValue()?.asString() ?: ""
            self.mMediaURLEdit?.remove(mediaUrl)
            LLURLHistory.removeURL("parcel", mediaUrl)
            if (self.addURLToCombobox(mediaUrl)) {
                LLURLHistory.addURL("parcel", mediaUrl)
            }

            TODO("APR: use JVM equivalent")
        }

        private fun onBtnCancel(self: LLFloaterURLEntry) {
            self.closeFloater()
        }

        private fun onBtnClear(self: LLFloaterURLEntry) {
            LLNotificationsUtil.add("ConfirmClearMediaUrlList", LLSD(), LLSD()) { notification, response ->
                self.callbackClearUrlList(notification, response)
            }
        }

        private fun getMediaTypeCoro(url: String, parentHandle: LLHandle<LLFloater>) {
            TODO("APR: use JVM equivalent")
        }
    }

    private fun callbackClearUrlList(notification: LLSD, response: LLSD): Boolean {
        val option = LLNotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) {
            val urlList = childGetListInterface("media_entry")
            urlList?.operateOnAll(LLCtrlListInterface.OP_DELETE)

            mMediaURLEdit?.clear()
            LLURLHistory.clear("parcel")
            getChildView("clear_btn")?.setEnabled(false)
        }
        return false
    }
}
