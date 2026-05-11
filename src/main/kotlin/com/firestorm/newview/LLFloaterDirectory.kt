package com.firestorm.newview

// ---------------------------------------------------------------------------
// Stub UI widget types local to this file
// ---------------------------------------------------------------------------

private open class LLPanelDirBrowser(private val childName: String) {
    fun setFloaterDirectory(dir: LLFloaterDirectory) {}
    fun openProfile() {}
}

private class LLPanelDirWeb : LLPanelDirBrowser("panel_dir_web") {
    fun navigateToSearchPage(category: String, query: String, collection: String) {
        TODO("APR: use JVM equivalent for web panel navigation")
    }
}

private class LLPanelProfileSecondLife {
    fun setAllowEdit(allow: Boolean) {}
    fun getVisible(): Boolean = TODO("GPU: query panel visibility")
    fun setVisible(visible: Boolean) {}
}

private class LLPanelEventInfo {
    fun setVisible(visible: Boolean) {}
}

private class LLPanelGroup {
    fun hideBackBtn() {}
    fun getVisible(): Boolean = TODO("GPU: query panel visibility")
    fun setVisible(visible: Boolean) {}
}

private class LLPanelPlaces {
    fun hideBackBtn() {}
    fun setVisible(visible: Boolean) {}
}

private class LLPanelClassifiedInfo {
    fun setBackgroundVisible(visible: Boolean) {}
    fun setVisible(visible: Boolean) {}
}

private class LLButton {
    fun setVisible(visible: Boolean) {}
    fun setCommitCallback(cb: () -> Unit) {}
}

private class LLTabContainer {
    fun getCurrentPanel(): CurrentPanel? = TODO("GPU: query active tab panel")
    fun selectTabByName(name: String) {}
    fun setCommitCallback(cb: () -> Unit) {}
}

private class CurrentPanel(val name: String)

// ---------------------------------------------------------------------------
// LLFloaterDirectory
// ---------------------------------------------------------------------------

class LLFloaterDirectory(name: String) {

    var mPanelAvatarp: LLPanelProfileSecondLife? = null
    var mPanelEventp: LLPanelEventInfo? = null
    var mPanelGroupp: LLPanelGroup? = null
    var mPanelPlacep: LLPanelPlaces? = null
    var mPanelClassifiedp: LLPanelClassifiedInfo? = null
    var mOpenProfileBtn: LLButton? = null
    var mDirectoryTabs: LLTabContainer? = null

    private var mLastSearchURL: String = ""

    @Suppress("UNCHECKED_CAST")
    private fun <T> findChild(childName: String): T? =
        TODO("GPU: findChild<$childName> in floater hierarchy")

    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(childName: String): T =
        TODO("GPU: getChild<$childName> in floater hierarchy")

    open fun postBuild(): Boolean {
        val panelNames = listOf(
            "panel_dir_classified",
            "panel_dir_events",
            "panel_dir_places",
            "panel_dir_land",
            "panel_dir_people",
            "panel_dir_groups"
        )
        for (panelName in panelNames) {
            findChild<LLPanelDirBrowser>(panelName)?.setFloaterDirectory(this)
        }
        findChild<LLPanelDirWeb>("panel_dir_web")?.setFloaterDirectory(this)

        mPanelAvatarp = findChild("panel_profile_secondlife")
        mPanelAvatarp?.setAllowEdit(false)
        mPanelGroupp = findChild("panel_group_info_sidetray")
        mPanelGroupp?.hideBackBtn()
        mPanelPlacep = findChild("panel_places")
        mPanelPlacep?.hideBackBtn()
        mPanelClassifiedp = findChild("panel_classified_info")
        mPanelClassifiedp?.setBackgroundVisible(false)
        mPanelEventp = findChild("panel_event_info")

        mOpenProfileBtn = getChild("open_profile_btn")
        mOpenProfileBtn?.setCommitCallback {
            val current = mDirectoryTabs?.getCurrentPanel()
            if (current != null) {
                findChild<LLPanelDirBrowser>(current.name)?.openProfile()
            }
        }
        mDirectoryTabs = getChild("Directory Tabs")
        mDirectoryTabs?.setCommitCallback { updateProfileButtonVisibility() }

        mLastSearchURL = resolveSearchUrl()
        return true
    }

    open fun onOpen(key: Map<String, Any?>) {
        var searchUrl = LFSimFeatureHandler.searchURL()
        if (searchUrl.isEmpty()) {
            searchUrl = resolveSearchUrl()
        }

        // Only navigate when the URL has changed or a query is present; avoids
        // re-triggering a page load when the same window is merely brought to front.
        if (!key.containsKey("query") && mLastSearchURL == searchUrl) {
            return
        }
        mLastSearchURL = searchUrl

        val panelDirWeb = findChild<LLPanelDirWeb>("panel_dir_web") ?: return
        if (mDirectoryTabs == null) return

        val category = key["category"] as? String ?: "standard"
        val query = key["query"] as? String ?: ""
        val collection = key["collection"] as? String ?: ""

        mDirectoryTabs?.selectTabByName("panel_dir_web")
        panelDirWeb.navigateToSearchPage(category, query, collection)
    }

    fun hideAllDetailPanels() {
        mPanelAvatarp?.setVisible(false)
        mPanelGroupp?.setVisible(false)
        mPanelPlacep?.setVisible(false)
        mPanelClassifiedp?.setVisible(false)
        mPanelEventp?.setVisible(false)
        mOpenProfileBtn?.setVisible(false)
    }

    fun updateProfileButtonVisibility() {
        val selectedTab = mDirectoryTabs?.getCurrentPanel()?.name ?: return
        when (selectedTab) {
            "panel_dir_people" -> mOpenProfileBtn?.setVisible(mPanelAvatarp?.getVisible() == true)
            "panel_dir_groups" -> mOpenProfileBtn?.setVisible(mPanelGroupp?.getVisible() == true)
            else               -> mOpenProfileBtn?.setVisible(false)
        }
    }

    private fun resolveSearchUrl(): String =
        TODO("APR: use JVM equivalent for grid-aware SearchURL / SearchURLOpenSim lookup")
}

// ---------------------------------------------------------------------------
// Singleton for sim-feature access
// ---------------------------------------------------------------------------

private object LFSimFeatureHandler {
    fun searchURL(): String = TODO("APR: use JVM equivalent for LFSimFeatureHandler::searchURL")
}
