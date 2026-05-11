package com.firestorm.newview

import java.util.UUID

object LLFloaterAboutUtil {
    fun registerFloater() {
        LLFloaterReg.add("sl_about", "floater_about.xml") { key -> LLFloaterAbout(key) }
    }

    fun checkUpdatesAndNotify() {
        LLFloaterAbout.setUpdateListener()
    }
}

class LLFloaterAbout(key: LLSD) : LLFloater(key) {

    companion object {
        val sCheckUpdateListenerName: String = "LLFloaterAboutUpdateCheck"

        fun getInfo(): LLSD = LLAppViewer.instance().getViewerInfo()

        fun setUpdateListener() {
            val info = getInfo()
            val version = info["VIEWER_VERSION_STR"].asString()
            val appDir = gDirUtilp.getOSUserAppDir()

            var downloads = false
            var downloadDir = ""
            var done = false
            var next = false
            var skip = false

            val fileVec = gDirUtilp.getFilesInDir(appDir)
            for (entry in fileVec) {
                if (entry.contains("downloads")) {
                    val dirVec = gDirUtilp.getFilesInDir(entry)
                    for (dirEntry in dirVec) {
                        if (dirEntry.contains(version)) {
                            downloads = true
                            downloadDir = dirEntry
                        }
                    }
                }
            }

            if (downloads) {
                for (entry in fileVec) {
                    if (entry.contains(version)) {
                        when {
                            entry.contains(".done") -> done = true
                            entry.contains(".next") -> next = true
                            entry.contains(".skip") -> skip = true
                        }
                    }
                }
            }

            when {
                !downloads -> LLNotificationsUtil.add("UpdateViewerUpToDate")
                !done -> LLNotificationsUtil.add("UpdateDownloadInProgress")
                !next && !skip -> LLNotificationsUtil.add("UpdateDownloadComplete")
                else -> LLNotificationsUtil.add("UpdateDeferred")
            }
        }

        fun startFetchServerReleaseNotes() {
            val region = gAgent.getRegion() ?: return
            val capUrl = region.getCapability("ServerReleaseNotes")
            TODO("APR: use JVM equivalent: launch coroutine for fetchServerReleaseNotesCoro($capUrl)")
        }

        fun fetchServerReleaseNotesCoro(capUrl: String) {
            TODO("APR: use JVM equivalent: HTTP GET $capUrl with redirect disabled, call handleServerReleaseNotes with result")
        }

        fun handleServerReleaseNotes(results: LLSD) {
            val httpHeaders: LLSD = if (results.has(LLCoreHttpUtil.HTTP_RESULTS)) {
                val httpResults = results[LLCoreHttpUtil.HTTP_RESULTS]
                httpResults[LLCoreHttpUtil.HTTP_RESULTS_HEADERS]
            } else {
                results[LLCoreHttpUtil.HTTP_RESULTS_HEADERS]
            }

            var location = httpHeaders[HTTP_IN_HEADER_LOCATION].asString()
            if (location.isEmpty()) {
                location = LLTrans.getString("ErrorFetchingServerReleaseNotesURL")
            }
            LLAppViewer.instance().setServerReleaseNotesURL(location)

            val floaterAbout = LLFloaterReg.findTypedInstance<LLFloaterAbout>("sl_about")
            floaterAbout?.setSupportText(location)
        }

        fun showCheckUpdateNotification(state: Int) {
            TODO("APR: use JVM equivalent: show notification for update check state $state")
        }

        fun callbackCheckUpdate(event: LLSD): Boolean {
            TODO("APR: use JVM equivalent: handle update check event callback")
        }
    }

    override fun postBuild(): Boolean {
        center()

        val supportWidget = getChild<LLViewerTextEditor>("support_editor", true)
        val contribNamesWidget = getChild<LLViewerTextEditor>("contrib_names", true)
        val licensesWidget = getChild<LLViewerTextEditor>("licenses_editor", true)

        getChild<LLUICtrl>("copy_btn").setCommitCallback { onClickCopyToClipboard() }

        val aboutColor = LLUIColorTable.instance().getColor("TextFgReadOnlyColor")

        if (gAgent.getRegion() != null) {
            setSupportText(LLTrans.getString("RetrievingData"))
            startFetchServerReleaseNotes()
        } else {
            setSupportText(LLTrans.getString("NotConnected"))
        }

        supportWidget.blockUndo()
        supportWidget.setEnabled(false)
        supportWidget.startOfDoc()

        val contributorsPath = gDirUtilp.getExpandedFilename(LL_PATH_APP_SETTINGS, "contributors.txt")
        val contributorsFile = java.io.File(contributorsPath)
        if (contributorsFile.exists()) {
            val contributors = contributorsFile.bufferedReader().use { it.readLine() ?: "" }
            contribNamesWidget.setText(contributors)
        }
        contribNamesWidget.setEnabled(false)
        contribNamesWidget.startOfDoc()

        val licensesPath = gDirUtilp.getExpandedFilename(LL_PATH_APP_SETTINGS, "packages-info.txt")
        val licensesFile = java.io.File(licensesPath)
        if (licensesFile.exists()) {
            licensesWidget.clear()
            licensesFile.forEachLine { line ->
                licensesWidget.appendText(line + "\n", false, LLStyle.Params().color(aboutColor))
            }
        }
        licensesWidget.setEnabled(false)
        licensesWidget.startOfDoc()

        return true
    }

    fun onClickCopyToClipboard() {
        val supportWidget = getChild<LLViewerTextEditor>("support_editor", true)
        supportWidget.selectAll()
        supportWidget.copy()
        supportWidget.deselect()
    }

    fun onClickUpdateCheck() {
        setUpdateListener()
    }

    fun setSupportText(serverReleaseNotesUrl: String) {
        val supportWidget = getChild<LLViewerTextEditor>("support_editor", true)
        val aboutColor = LLUIColorTable.instance().getColor("TextFgReadOnlyColor")
        supportWidget.clear()
        supportWidget.appendText(
            LLAppViewer.instance().getViewerInfoString(),
            false,
            LLStyle.Params().color(aboutColor)
        )
    }
}

class LLFloaterAboutListener : LLEventAPI(
    "LLFloaterAbout",
    "LLFloaterAbout listener to retrieve About box info"
) {
    init {
        add(
            "getInfo",
            "Request an LLSD::Map containing information used to populate About box",
            ::getInfo,
            LLSD().with("reply", LLSD())
        )
    }

    private fun getInfo(request: LLSD) {
        val reqid = LLReqID(request)
        val reply = LLFloaterAbout.getInfo()
        reqid.stamp(reply)
        LLEventPumps.instance().obtain(request["reply"]).post(reply)
    }
}
