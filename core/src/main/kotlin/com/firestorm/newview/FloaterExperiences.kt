package com.firestorm.newview

import com.firestorm.ui.Floater
import com.firestorm.ui.PanelExperienceLog
import com.firestorm.ui.PanelExperiencePicker
import com.firestorm.ui.PanelExperiences
import com.firestorm.ui.TabContainer
import com.firestorm.viewer.Agent
import com.firestorm.viewer.ExperienceCache
import com.firestorm.viewer.FloaterReg
import com.firestorm.viewer.ViewerRegion
import java.util.UUID
import java.util.function.BiConsumer

class FloaterExperiences(data: LLSD) : Floater(data) {

    typealias NameMap = MutableMap<String, String>
    typealias Callback = BiConsumer<PanelExperiences, LLSD>

    private val prepurchaseIds: MutableList<UUID> = mutableListOf()

    private var capsReceivedConnection: Connection? = null

    override fun postBuild(): Boolean {
        val tabs = getChild<TabContainer>("xp_tabs")
        tabs.addTabPanel(PanelExperiencePicker())
        addTab("Allowed_Experiences_Tab", select = true)
        addTab("Blocked_Experiences_Tab", select = false)
        addTab("Admin_Experiences_Tab", select = false)
        addTab("Contrib_Experiences_Tab", select = false)
        val owned = addTab("Owned_Experiences_Tab", select = false)
        owned.setButtonAction("acquire") { sendPurchaseRequest() }
        owned.enableButton(false)
        tabs.addTabPanel(PanelExperienceLog())
        resizeToTabs()
        return true
    }

    override fun onOpen(key: LLSD) {
        EventPumps.obtain("experience_permission").stopListening("FloaterExperiences")
        EventPumps.obtain("experience_permission").listen("FloaterExperiences") { perm ->
            updatePermissions(perm)
        }
        val region = Agent.region
        if (region != null) {
            if (region.capabilitiesReceived()) {
                refreshContents()
            } else {
                capsReceivedConnection = region.setCapabilitiesReceivedCallback { refreshContents() }
            }
        }
    }

    override fun onClose(appQuitting: Boolean) {
        capsReceivedConnection?.disconnect()
        EventPumps.obtain("experience_permission").stopListening("FloaterExperiences")
        super.onClose(appQuitting)
    }

    private fun addTab(name: String, select: Boolean): PanelExperiences {
        val panel = PanelExperiences.create(name)
        getChild<TabContainer>("xp_tabs").addTabPanel(
            panel,
            label = Trans.getString(name),
            selectTab = select
        )
        return panel
    }

    private fun resizeToTabs() {
        val tabWidthPadding = 16
        val tabs = getChild<TabContainer>("xp_tabs")
        val rect = getRect()
        if (rect.width < tabs.totalTabWidth + tabWidthPadding) {
            reshape(tabs.totalTabWidth + tabWidthPadding, rect.height, false)
        }
    }

    private fun refreshContents() {
        setupRecentTabs()
        val region = Agent.region ?: return
        val tabMap: NameMap = mutableMapOf(
            "experiences"    to "Allowed_Experiences_Tab",
            "blocked"        to "Blocked_Experiences_Tab",
            "experience_ids" to "Owned_Experiences_Tab"
        )
        val handle = getDerivedHandle<FloaterExperiences>()

        retrieveExperienceList(region.getCapability("GetExperiences"), handle, tabMap)
        updateInfo("GetAdminExperiences", "Admin_Experiences_Tab")
        updateInfo("GetCreatorExperiences", "Contrib_Experiences_Tab")
        retrieveExperienceList(
            url = region.getCapability("AgentExperiences"),
            hparent = handle,
            tabMapping = tabMap,
            errorNotify = "ExperienceAcquireFailed",
            cback = Callback { panel, content -> checkPurchaseInfo(panel, content) }
        )
    }

    private fun setupRecentTabs() {
        // SHOW_RECENT_TAB is disabled (compile-time 0 in C++); intentional no-op.
    }

    private fun clearFromRecent(ids: LLSD) {
        // SHOW_RECENT_TAB disabled – intentional no-op.
    }

    private fun updatePermissions(permission: LLSD): Boolean {
        val tabs = getChild<TabContainer>("xp_tabs")
        val experience = if (permission.has("experience")) permission["experience"].asUUID() else null
        val permissionString = experience?.let {
            permission[it.toString()]?.get("permission")?.asString()
        } ?: ""

        fun applyToTab(tabName: String, listKey: String, allowValue: String) {
            val tab = tabs.getPanelByName(tabName) as? PanelExperiences ?: return
            when {
                permission.has(listKey)        -> tab.setExperienceList(permission[listKey])
                experience != null && experience != NULL_UUID -> {
                    if (permissionString != allowValue) tab.removeExperience(experience)
                    else tab.addExperience(experience)
                }
            }
        }

        applyToTab("Allowed_Experiences_Tab", "experiences", "Allow")
        applyToTab("Blocked_Experiences_Tab", "blocked", "Block")
        return false
    }

    private fun checkPurchaseInfo(panel: PanelExperiences, content: LLSD) {
        panel.enableButton(content.has("purchase"))
        findInstance()?.updateInfo("GetAdminExperiences", "Admin_Experiences_Tab")
        findInstance()?.updateInfo("GetCreatorExperiences", "Contrib_Experiences_Tab")
    }

    private fun checkAndOpen(panel: PanelExperiences, content: LLSD) {
        checkPurchaseInfo(panel, content)
        val responseIds = content["experience_ids"]
        if (prepurchaseIds.size + 1 == responseIds.size()) {
            for (entry in responseIds.asIterable()) {
                val experienceId = entry.asUUID()
                if (experienceId !in prepurchaseIds) {
                    val args = LLSD.map("experience_id" to experienceId, "edit_experience" to true)
                    FloaterReg.showInstance("experience_profile", args, true)
                    break
                }
            }
        }
    }

    private fun updateInfo(experienceCap: String, tab: String) {
        val region = Agent.region ?: return
        val tabMap: NameMap = mutableMapOf("experience_ids" to tab)
        val handle = getDerivedHandle<FloaterExperiences>()
        retrieveExperienceList(region.getCapability(experienceCap), handle, tabMap)
    }

    private fun sendPurchaseRequest() {
        val region = Agent.region ?: return
        val tabOwnedName = "Owned_Experiences_Tab"
        val tabMap: NameMap = mutableMapOf("experience_ids" to tabOwnedName)
        val handle = getDerivedHandle<FloaterExperiences>()
        val tabs = getChild<TabContainer>("xp_tabs")
        val tabOwned = tabs.getPanelByName(tabOwnedName) as? PanelExperiences
        prepurchaseIds.clear()
        tabOwned?.getExperienceIdsList(prepurchaseIds)
        requestNewExperience(
            url = region.getCapability("AgentExperiences"),
            hparent = handle,
            tabMapping = tabMap,
            errorNotify = "ExperienceAcquireFailed",
            cback = Callback { panel, content -> checkAndOpen(panel, content) }
        )
    }

    private fun retrieveExperienceList(
        url: String,
        hparent: Handle<FloaterExperiences>,
        tabMapping: NameMap,
        errorNotify: String = "ErrorMessage",
        cback: Callback? = null
    ) {
        System.err.println("FloaterExperiences: retrieveExperienceList not yet implemented")
    }

    private fun requestNewExperience(
        url: String,
        hparent: Handle<FloaterExperiences>,
        tabMapping: NameMap,
        errorNotify: String,
        cback: Callback
    ) {
        System.err.println("FloaterExperiences: requestNewExperience not yet implemented")
    }

    companion object {
        private val NULL_UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")

        fun findInstance(): FloaterExperiences? =
            FloaterReg.findTypedInstance<FloaterExperiences>("experiences")
    }
}
