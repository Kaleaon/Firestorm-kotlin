package com.firestorm.newview

import java.util.UUID

const val DEFAULT_GRID_STATUS_URL = "http://status.secondlifegrid.net/"

class LLFloaterGridStatus(key: LLFloaterWebContent.Params) : LLFloaterWebContent(key) {

    private var mIsFirstUpdate: Boolean = true
    private var mGridStatusTimer: LLFrameTimer = LLFrameTimer()

    fun isFirstUpdate(): Boolean = mIsFirstUpdate
    fun setFirstUpdate(firstUpdate: Boolean) { mIsFirstUpdate = firstUpdate }

    override fun postBuild(): Boolean {
        LLFloaterWebContent.postBuild()
        mWebBrowser?.addObserver(this)
        return true
    }

    override fun onOpen(key: LLSDMap) {
        val p = Params(key)
        p.trustedContent = true
        p.allowAddressEntry = false

        super.onOpen(p)
        getChildView("popexternal")?.setEnabled(true)
        applyPreferredRect()
        mWebBrowser?.navigateTo(LFSimFeatureHandler.instance().gridStatusURL(), HTTP_CONTENT_TEXT_HTML)
    }

    override fun handleReshape(newRect: LLRect, byUser: Boolean) {
        if (byUser && !isMinimized()) {
            gSavedSettings.setRect("GridStatusFloaterRect", newRect)
        }
        super.handleReshape(newRect, byUser)
    }

    fun startGridStatusTimer() {
        checkGridStatusRSS()
        System.err.println("LLFloaterGridStatus: startGridStatusTimer not yet implemented")
    }

    private fun applyPreferredRect() {
        val preferredRect = gSavedSettings.getRect("GridStatusFloaterRect")
        val newRect = getRect().apply {
            setLeftTopAndSize(mLeft, mTop, preferredRect.getWidth(), preferredRect.getHeight())
        }
        setShape(newRect)
    }

    companion object {
        private val sItemsMap: MutableMap<String, String> = mutableMapOf()

        fun getInstance(): LLFloaterGridStatus? {
            return LLFloaterReg.getTypedInstance<LLFloaterGridStatus>("grid_status")
        }

        fun checkGridStatusRSS(): Boolean {
            if (gToolBarView.hasCommand(LLCommandId("gridstatus"))) {
                LLCoros.instance().launch("LLFloaterGridStatus::getGridStatusRSSCoro") {
                    getGridStatusRSSCoro()
                }
            }
            return false
        }

        fun getGridStatusRSSCoro() {
            System.err.println("LLFloaterGridStatus: getGridStatusRSSCoro not yet implemented")

            val url = LFSimFeatureHandler.instance().gridStatusRSS()
            if (url.isEmpty()) return

            // HTTP GET url, parse RSS XML, update sItemsMap, flash toolbar on new entries.
            // The per-grid log path is: gridMgr.isInSecondLife() ->
            //   "grid_status_rss.xml" else scrubbedGridName + "_grid_status_rss.xml"
        }
    }
}
