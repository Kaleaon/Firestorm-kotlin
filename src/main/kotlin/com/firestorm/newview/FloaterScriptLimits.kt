package com.firestorm.newview

import java.util.UUID

private const val SIZE_OF_ONE_KB = 1024

class FloaterScriptLimits(private val seed: Map<String, Any>) {

    private var mTab: TabContainer? = null
    private val mInfoPanels: MutableList<PanelScriptLimitsInfo> = mutableListOf()

    open fun postBuild(): Boolean {
        mTab = getChild("scriptlimits_panels")
        if (mTab == null) return false

        val panelMemory = PanelScriptLimitsRegionMemory()
        mInfoPanels.add(panelMemory)
        mTab?.addTabPanel(panelMemory)
        mTab?.selectTab(0)
        return true
    }

    open fun refresh() {
        for (panel in mInfoPanels) panel.refresh()
    }

    private fun <T> getChild(name: String): T? = null
}

abstract class PanelScriptLimitsInfo {
    protected var mHost: String = ""

    open fun postBuild(): Boolean {
        refresh()
        return true
    }

    open fun refresh() {}
    open fun updateChild(childCtrl: Any) {}

    protected fun initCtrl(name: String) {}
}

class PanelScriptLimitsRegionMemory : PanelScriptLimitsInfo() {

    private var mParcelId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var mGotParcelMemoryUsed: Boolean = false
    private var mGotParcelMemoryMax: Boolean = false
    private var mParcelMemoryMax: Int = 0
    private var mParcelMemoryUsed: Int = 0
    private var mGotParcelURLsUsed: Boolean = false
    private var mGotParcelURLsMax: Boolean = false
    private var mParcelURLsMax: Int = 0
    private var mParcelURLsUsed: Int = 0
    private var mContent: Map<String, Any> = emptyMap()
    private val mObjectListItems: MutableList<Map<String, Any>> = mutableListOf()

    override fun postBuild(): Boolean {
        setChildAction("refresh_list_btn") { onClickRefresh() }
        setChildAction("highlight_btn") { onClickHighlight() }
        setChildAction("return_btn") { onClickReturn() }

        setChildValue("loading_text", "Waiting for script information…")

        val list = getScriptsList() ?: return false
        list.setCommitCallback { checkButtonsEnabled() }
        checkButtonsEnabled()

        return startRequestChain()
    }

    fun startRequestChain(): Boolean {
        val parcel = getCurrentSelectedParcel()
        val region = getSelectionRegion()

        if (region == null || parcel == null) {
            setChildValue("loading_text", "No parcel selected")
            return super.postBuild()
        }

        val currentRegionId = getAgentRegionId()
        val regionId = region.regionId

        if (regionId != currentRegionId) {
            setChildValue("loading_text", "Wrong region — please select a parcel in your current region")
            return false
        }

        val remoteParcelUrl = region.getCapability("RemoteParcelRequest")
        if (remoteParcelUrl != null) {
            TODO("APR: use JVM equivalent — requestRegionParcelInfo(remoteParcelUrl, regionId, parcelCenter, posGlobal)")
        } else {
            setChildValue("loading_text", "Region does not support RemoteParcelRequest")
        }

        return super.postBuild()
    }

    fun getLandScriptResources(): Boolean {
        val url = getRegionCapability("LandResources") ?: return false
        getLandScriptResourcesCoro(url)
        return true
    }

    private fun getLandScriptResourcesCoro(url: String) {
        TODO("APR: use JVM equivalent — POST parcel_id to url, then spawn summary+details coroutines")
    }

    private fun getLandScriptSummaryCoro(url: String) {
        TODO("APR: use JVM equivalent — GET url, parse result, call setRegionSummary")
    }

    private fun getLandScriptDetailsCoro(url: String) {
        TODO("APR: use JVM equivalent — GET url, parse result, call setRegionDetails")
    }

    fun processParcelInfo(parcelData: Map<String, Any>) {
        if (!getLandScriptResources()) {
            setChildValue("loading_text", "Script limits request error")
        } else {
            setChildValue("loading_text", "Waiting for script resource information…")
        }
    }

    fun setParcelId(parcelId: UUID) {
        if (parcelId != UUID.fromString("00000000-0000-0000-0000-000000000000")) {
            if (mParcelId != UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                TODO("APR: use JVM equivalent — removeObserver(mParcelId, this)")
            }
            mParcelId = parcelId
            TODO("APR: use JVM equivalent — addObserver(parcelId, this) and sendParcelInfoRequest(parcelId)")
        } else {
            setChildValue("loading_text", "Script limits request error")
        }
    }

    fun setErrorStatus(status: Int, reason: String) {
        System.err.println("Remote parcel request failed: HTTP $status — $reason")
    }

    private fun onAvatarNameCache(id: UUID, userName: String) {
        onNameCache(id, userName)
    }

    private fun onNameCache(id: UUID, fullName: String) {
        val list = getScriptsList() ?: return
        val builtName = buildUsername(fullName)
        for (element in mObjectListItems) {
            if (element["owner_id"] == id) {
                val taskId = element["id"] as? UUID ?: continue
                list.getItem(taskId)?.setColumnValue(3, builtName)
            }
        }
    }

    fun setRegionDetails(content: Map<String, Any>) {
        val list = getScriptsList() ?: return

        @Suppress("UNCHECKED_CAST")
        val parcels = content["parcels"] as? List<Map<String, Any>> ?: emptyList()
        val numberParcels = parcels.size

        setChildValue("parcels_listed", "Parcels owned: $numberParcels")

        val namesRequested = mutableListOf<UUID>()
        var hasLocations = false
        var hasLocalIds = false

        for (parcel in parcels) {
            val parcelName = parcel["name"] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            val objects = parcel["objects"] as? List<Map<String, Any>> ?: emptyList()

            var localId = 0
            if (parcel.containsKey("local_id")) {
                hasLocalIds = true
                localId = (parcel["local_id"] as? Number)?.toInt() ?: 0
            }

            for (obj in objects) {
                @Suppress("UNCHECKED_CAST")
                val resources = obj["resources"] as? Map<String, Any> ?: emptyMap()
                val size = ((resources["memory"] as? Number)?.toInt() ?: 0) / SIZE_OF_ONE_KB
                val urls = (resources["urls"] as? Number)?.toInt() ?: 0
                val nameBuf = obj["name"] as? String ?: ""
                val taskId = uuidFromString(obj["id"] as? String)
                val ownerId = uuidFromString(obj["owner_id"] as? String)
                val isGroupOwned = obj["is_group_owned"] as? Boolean ?: false

                var locationX = 0f
                var locationY = 0f
                var locationZ = 0f
                if (obj.containsKey("location")) {
                    hasLocations = true
                    @Suppress("UNCHECKED_CAST")
                    val loc = obj["location"] as? List<Number> ?: emptyList()
                    locationX = loc.getOrNull(0)?.toFloat() ?: 0f
                    locationY = loc.getOrNull(1)?.toFloat() ?: 0f
                    locationZ = loc.getOrNull(2)?.toFloat() ?: 0f
                }

                val ownerBuf: String = when {
                    obj.containsKey("owner_name") -> obj["owner_name"] as? String ?: ""
                    else -> {
                        val cached = lookupOwnerName(ownerId, isGroupOwned)
                        if (cached == null && !namesRequested.contains(ownerId)) {
                            namesRequested.add(ownerId)
                            if (isGroupOwned) {
                                TODO("APR: use JVM equivalent — getGroup(ownerId, ::onNameCache)")
                            } else {
                                TODO("APR: use JVM equivalent — LLAvatarNameCache.get(ownerId, ::onAvatarNameCache)")
                            }
                        }
                        cached ?: ""
                    }
                }

                val locationStr = if (hasLocations)
                    "<%0.0f, %0.0f, %0.0f>".format(locationX, locationY, locationZ)
                else ""

                list.addRow(taskId, size, urls, nameBuf, ownerBuf, parcelName, locationStr)

                mObjectListItems.add(mapOf(
                    "owner_id" to ownerId,
                    "id" to taskId,
                    "local_id" to localId
                ))
            }
        }

        if (hasLocations) setChildVisible("highlight_btn", true)
        if (hasLocalIds) setChildVisible("return_btn", true)
        mContent = content
    }

    fun setRegionSummary(content: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val summary = content["summary"] as? Map<String, Any> ?: return
        @Suppress("UNCHECKED_CAST")
        val used = summary["used"] as? List<Map<String, Any>> ?: return
        @Suppress("UNCHECKED_CAST")
        val available = summary["available"] as? List<Map<String, Any>> ?: return

        val memoryUsedEntry = used.firstOrNull { it["type"] == "memory" }
        val memoryAvailEntry = available.firstOrNull { it["type"] == "memory" }
        if (memoryUsedEntry != null && memoryAvailEntry != null) {
            mParcelMemoryUsed = ((memoryUsedEntry["amount"] as? Number)?.toInt() ?: 0) / SIZE_OF_ONE_KB
            mParcelMemoryMax = ((memoryAvailEntry["amount"] as? Number)?.toInt() ?: 0) / SIZE_OF_ONE_KB
            mGotParcelMemoryUsed = true
            mGotParcelMemoryMax = true
        }

        val urlsUsedEntry = used.firstOrNull { it["type"] == "urls" }
        val urlsAvailEntry = available.firstOrNull { it["type"] == "urls" }
        if (urlsUsedEntry != null && urlsAvailEntry != null) {
            mParcelURLsUsed = (urlsUsedEntry["amount"] as? Number)?.toInt() ?: 0
            mParcelURLsMax = (urlsAvailEntry["amount"] as? Number)?.toInt() ?: 0
            mGotParcelURLsUsed = true
            mGotParcelURLsMax = true
        }

        if (mParcelMemoryUsed >= 0 && mParcelMemoryMax >= 0) {
            val available = mParcelMemoryMax - mParcelMemoryUsed
            val msg = if (mParcelMemoryMax > 0)
                "Memory: $mParcelMemoryUsed KB used of $mParcelMemoryMax KB ($available KB available)"
            else
                "Memory: $mParcelMemoryUsed KB used"
            setChildValue("memory_used", msg)
        }

        if (mParcelURLsUsed >= 0 && mParcelURLsMax >= 0) {
            val available = mParcelURLsMax - mParcelURLsUsed
            setChildValue("urls_used", "URLs: $mParcelURLsUsed used of $mParcelURLsMax ($available available)")
        }
    }

    fun clearList() {
        getScriptsList()?.clearAll()
        mGotParcelMemoryUsed = false
        mGotParcelMemoryMax = false
        mGotParcelURLsUsed = false
        mGotParcelURLsMax = false
        setChildValue("memory_used", "")
        setChildValue("urls_used", "")
        setChildValue("parcels_listed", "")
        mObjectListItems.clear()
        checkButtonsEnabled()
    }

    fun checkButtonsEnabled() {
        val selected = getScriptsList()?.getNumSelected() ?: 0
        setChildEnabled("highlight_btn", selected > 0)
        setChildEnabled("return_btn", selected > 0)
    }

    fun showBeacon() {
        val list = getScriptsList() ?: return
        val firstSelected = list.getFirstSelected() ?: return
        val name = firstSelected.getColumnValue(2)
        val posString = firstSelected.getColumnValue(5)

        val match = Regex("<([^,]+),([^,]+),([^>]+)>").find(posString) ?: return
        val x = match.groupValues[1].trim().toFloatOrNull() ?: return
        val y = match.groupValues[2].trim().toFloatOrNull() ?: return
        val z = match.groupValues[3].trim().toFloatOrNull() ?: return

        TODO("APR: use JVM equivalent — LLTracker.trackLocation(posGlobal, name, tooltip, LOCATION_ITEM) from agent position ($x,$y,$z)")
    }

    fun returnObjectsFromParcel(localId: Int) {
        val list = getScriptsList() ?: return
        if (list.getItemCount() == 0) return

        val selectedItems = mObjectListItems.filter {
            val id = it["id"] as? UUID ?: return@filter false
            list.isSelected(id) && (it["local_id"] as? Int) == localId
        }

        if (selectedItems.isEmpty()) return

        TODO("APR: use JVM equivalent — send ParcelReturnObjects UDP message for localId=$localId with taskIDs=${selectedItems.map { it["id"] }}")
    }

    fun returnObjects() {
        @Suppress("UNCHECKED_CAST")
        val parcels = (mContent["parcels"] as? List<Map<String, Any>>) ?: return
        for (parcel in parcels) {
            val localId = (parcel["local_id"] as? Number)?.toInt() ?: continue
            returnObjectsFromParcel(localId)
        }
        onClickRefresh()
    }

    private fun onClickRefresh() {
        setChildEnabled("refresh_list_btn", false)
        clearList()
        startRequestChain()
    }

    private fun onClickHighlight() = showBeacon()
    private fun onClickReturn() = returnObjects()

    private fun getScriptsList(): ScriptsList? = null
    private fun getCurrentSelectedParcel(): Any? = null
    private fun getSelectionRegion(): Region? = null
    private fun getAgentRegionId(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private fun getRegionCapability(cap: String): String? = null
    private fun lookupOwnerName(id: UUID, isGroup: Boolean): String? = null
    private fun buildUsername(name: String): String = name.replace(" ", ".")
    private fun uuidFromString(s: String?): UUID =
        runCatching { UUID.fromString(s) }.getOrElse { UUID.fromString("00000000-0000-0000-0000-000000000000") }

    private fun setChildValue(id: String, value: Any) {}
    private fun setChildEnabled(id: String, enabled: Boolean) {}
    private fun setChildVisible(id: String, visible: Boolean) {}
    private fun setChildAction(id: String, action: () -> Unit) {}

    private class ScriptsList {
        fun getNumSelected(): Int = 0
        fun getItemCount(): Int = 0
        fun getFirstSelected(): ScriptsListItem? = null
        fun getItem(id: UUID): ScriptsListItem? = null
        fun isSelected(id: UUID): Boolean = false
        fun clearAll() {}
        fun addRow(taskId: UUID, size: Int, urls: Int, name: String, owner: String, parcel: String, location: String) {}
        fun setCommitCallback(cb: () -> Unit) {}
    }

    private class ScriptsListItem {
        fun getColumnValue(col: Int): String = ""
        fun setColumnValue(col: Int, value: String) {}
    }

    private class TabContainer {
        fun addTabPanel(panel: PanelScriptLimitsInfo) {}
        fun selectTab(idx: Int) {}
    }

    private class Region(val regionId: UUID) {
        fun getCapability(cap: String): String? = null
    }
}
