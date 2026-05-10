package com.firestorm.newview

import java.util.UUID

private const val SIZE_OF_ONE_KB = 1024

class LLFloaterScriptLimits(val seed: Any) {

    private var mTab: LLTabContainer? = null
    private val mInfoPanels: MutableList<LLPanelScriptLimitsInfo> = mutableListOf()

    fun postBuild(): Boolean {
        mTab = getChild<LLTabContainer>("scriptlimits_panels") ?: return false

        val panelMemory = LLPanelScriptLimitsRegionMemory()
        mInfoPanels.add(panelMemory)
        panelMemory.buildFromFile("panel_script_limits_region_memory.xml")
        mTab?.addTabPanel(panelMemory)
        mTab?.selectTab(0)
        return true
    }

    fun refresh() {
        mInfoPanels.forEach { it.refresh() }
    }
}

abstract class LLPanelScriptLimitsInfo : LLPanel() {

    protected var mHost: LLHost? = null

    override fun postBuild(): Boolean {
        refresh()
        return true
    }

    open fun updateChild(childCtrl: LLUICtrl) {}

    protected fun initCtrl(name: String) {
        TODO("APR: use JVM equivalent - initialise named child control")
    }
}

class LLPanelScriptLimitsRegionMemory : LLPanelScriptLimitsInfo() {

    private var mContent: Map<String, Any?> = emptyMap()
    private var mParcelId: UUID = UUID(0, 0)
    private var mGotParcelMemoryUsed: Boolean = false
    private var mGotParcelMemoryMax: Boolean = false
    private var mParcelMemoryMax: Int = 0
    private var mParcelMemoryUsed: Int = 0

    private var mGotParcelURLsUsed: Boolean = false
    private var mGotParcelURLsMax: Boolean = false
    private var mParcelURLsMax: Int = 0
    private var mParcelURLsUsed: Int = 0

    private val mObjectListItems: MutableList<Map<String, Any?>> = mutableListOf()

    private var mAvatarNameCacheConnection: ((UUID, LLAvatarName) -> Unit)? = null
    private var mGroupNameCacheConnection: ((UUID, String) -> Unit)? = null

    fun destroy() {
        if (mParcelId != UUID(0, 0)) {
            LLRemoteParcelInfoProcessor.getInstance().removeObserver(mParcelId, this)
            mParcelId = UUID(0, 0)
        }
    }

    override fun postBuild(): Boolean {
        childSetAction("refresh_list_btn") { onClickRefresh() }
        childSetAction("highlight_btn") { onClickHighlight() }
        childSetAction("return_btn") { onClickReturn() }

        val msgWaiting = LLTrans.getString("ScriptLimitsRequestWaiting")
        getChild<LLUICtrl>("loading_text").setValue(msgWaiting)

        val list = getChild<LLScrollListCtrl>("scripts_list") ?: return false
        list.setCommitCallback { checkButtonsEnabled() }
        checkButtonsEnabled()

        for (column in 0 until list.getNumColumns()) {
            val columnp = list.getColumn(column)
            columnp.mHeader?.setHasResizableElement(true)
        }

        return startRequestChain()
    }

    fun startRequestChain(): Boolean {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterLand>("about_land") ?: run {
            getChild<LLUICtrl>("loading_text").setValue("")
            return false
        }

        val parcel = instance.getCurrentSelectedParcel()
        val region = LLViewerParcelMgr.getInstance().getSelectionRegion()

        if (region != null && parcel != null) {
            val currentRegionId = gAgent.getRegion()?.getRegionID() ?: return false
            val parcelCenter = parcel.getCenterpoint()
            val regionId = region.getRegionID()

            if (regionId != currentRegionId) {
                val msg = LLTrans.getString("ScriptLimitsRequestWrongRegion")
                getChild<LLUICtrl>("loading_text").setValue(msg)
                return false
            }

            val posGlobal = region.getOriginGlobal()

            val url = region.getCapability("RemoteParcelRequest")
            if (url.isNotEmpty()) {
                LLRemoteParcelInfoProcessor.getInstance().requestRegionParcelInfo(
                    url, regionId, parcelCenter, posGlobal, getObserverHandle()
                )
            } else {
                val msgError = LLTrans.getString("ScriptLimitsRequestError")
                getChild<LLUICtrl>("loading_text").setValue(msgError)
            }
        } else {
            val msgNoParcel = LLTrans.getString("ScriptLimitsRequestNoParcelSelected")
            getChild<LLUICtrl>("loading_text").setValue(msgNoParcel)
        }

        return super.postBuild()
    }

    fun getLandScriptResources(): Boolean {
        val region = gAgent.getRegion() ?: return false
        val url = region.getCapability("LandResources")
        if (url.isEmpty()) return false
        TODO("APR: use JVM equivalent - launch coroutine getLandScriptResourcesCoro($url)")
    }

    private fun getLandScriptResourcesCoro(url: String) {
        TODO("APR: use JVM equivalent - POST parcel_id, then spawn summary and details coroutines")
    }

    private fun getLandScriptSummaryCoro(url: String) {
        TODO("APR: use JVM equivalent - GET $url, find panel and call setRegionSummary")
    }

    private fun getLandScriptDetailsCoro(url: String) {
        TODO("APR: use JVM equivalent - GET $url, find panel and call setRegionDetails")
    }

    fun processParcelInfo(parcelData: LLParcelData) {
        if (!getLandScriptResources()) {
            getChild<LLUICtrl>("loading_text").setValue(LLTrans.getString("ScriptLimitsRequestError"))
        } else {
            getChild<LLUICtrl>("loading_text").setValue(LLTrans.getString("ScriptLimitsRequestWaiting"))
        }
    }

    fun setParcelID(parcelId: UUID) {
        if (parcelId != UUID(0, 0)) {
            if (mParcelId != UUID(0, 0)) {
                LLRemoteParcelInfoProcessor.getInstance().removeObserver(mParcelId, this)
                mParcelId = UUID(0, 0)
            }
            mParcelId = parcelId
            LLRemoteParcelInfoProcessor.getInstance().addObserver(parcelId, this)
            LLRemoteParcelInfoProcessor.getInstance().sendParcelInfoRequest(parcelId)
        } else {
            getChild<LLUICtrl>("loading_text").setValue(LLTrans.getString("ScriptLimitsRequestError"))
        }
    }

    fun setErrorStatus(status: Int, reason: String) {
        TODO("APR: log remote parcel request failure, status=$status reason=$reason")
    }

    private fun onAvatarNameCache(id: UUID, avName: LLAvatarName) {
        onNameCache(id, avName.getUserName())
    }

    private fun onNameCache(id: UUID, fullName: String) {
        val list = getChild<LLScrollListCtrl>("scripts_list") ?: return
        val name = LLCacheName.buildUsername(fullName)

        for (element in mObjectListItems) {
            val ownerId = element["owner_id"] as? UUID ?: continue
            if (ownerId == id) {
                val taskId = element["id"] as? UUID ?: continue
                val item = list.getItem(taskId)
                item?.getColumn(3)?.setValue(name)
            }
        }
    }

    fun setRegionDetails(content: Map<String, Any?>) {
        val list = getChild<LLScrollListCtrl>("scripts_list") ?: return

        val parcels = content["parcels"] as? List<*> ?: return
        val numberParcels = parcels.size

        getChild<LLUICtrl>("parcels_listed").setValue(
            LLTrans.getString("ScriptLimitsParcelsOwned", mapOf("[PARCELS]" to "$numberParcels"))
        )

        val namesRequested: MutableList<UUID> = mutableListOf()
        var hasLocations = false
        var hasLocalIds = false

        for (i in 0 until numberParcels) {
            @Suppress("UNCHECKED_CAST")
            val parcelEntry = parcels[i] as? Map<String, Any?> ?: continue
            val parcelName = parcelEntry["name"] as? String ?: ""
            val objects = parcelEntry["objects"] as? List<*> ?: continue
            val numberObjects = objects.size

            var localId = 0
            if (parcelEntry.containsKey("local_id")) {
                hasLocalIds = true
                localId = (parcelEntry["local_id"] as? Number)?.toInt() ?: 0
            }

            for (j in 0 until numberObjects) {
                @Suppress("UNCHECKED_CAST")
                val obj = objects[j] as? Map<String, Any?> ?: continue
                val resources = obj["resources"] as? Map<String, Any?> ?: emptyMap()

                val size = ((resources["memory"] as? Number)?.toInt() ?: 0) / SIZE_OF_ONE_KB
                val urls = (resources["urls"] as? Number)?.toInt() ?: 0
                val nameBuf = obj["name"] as? String ?: ""
                val taskId = obj["id"] as? UUID ?: continue
                val ownerId = obj["owner_id"] as? UUID ?: continue
                val isGroupOwned = obj["is_group_owned"] as? Boolean ?: false

                var locationX = 0f
                var locationY = 0f
                var locationZ = 0f

                val location = obj["location"]
                if (location != null) {
                    val vec = llVector3FromSd(location)
                    hasLocations = true
                    locationX = vec[0]
                    locationY = vec[1]
                    locationZ = vec[2]
                }

                var ownerBuf: String = if (obj.containsKey("owner_name")) {
                    obj["owner_name"] as? String ?: ""
                } else {
                    var nameIsCached: Boolean
                    var resolved = ""
                    if (isGroupOwned) {
                        nameIsCached = gCacheName.getGroupName(ownerId) { resolved = it }
                    } else {
                        val avName = LLAvatarNameCache.get(ownerId)
                        nameIsCached = avName != null
                        resolved = LLCacheName.buildUsername(avName?.getUserName() ?: "")
                    }
                    if (!nameIsCached && !namesRequested.contains(ownerId)) {
                        namesRequested.add(ownerId)
                        if (isGroupOwned) {
                            mGroupNameCacheConnection = { id, name -> onNameCache(id, name) }
                            gCacheName.getGroup(ownerId, mGroupNameCacheConnection!!)
                        } else {
                            mAvatarNameCacheConnection = { id, av -> onAvatarNameCache(id, av) }
                            LLAvatarNameCache.get(ownerId, mAvatarNameCacheConnection!!)
                        }
                    }
                    resolved
                }

                val locationStr = if (hasLocations)
                    "<%0.0f, %0.0f, %0.0f>".format(locationX, locationY, locationZ)
                else ""

                list.addRow(
                    taskId,
                    listOf(size, urls, nameBuf, ownerBuf, parcelName, locationStr)
                )

                mObjectListItems.add(
                    mapOf("owner_id" to ownerId, "id" to taskId, "local_id" to localId)
                )
            }
        }

        if (hasLocations) {
            getChild<LLButton>("highlight_btn")?.setVisible(true)
        }
        if (hasLocalIds) {
            getChild<LLButton>("return_btn")?.setVisible(true)
        }

        @Suppress("UNCHECKED_CAST")
        mContent = content as Map<String, Any?>
    }

    fun setRegionSummary(content: Map<String, Any?>) {
        val summary = content["summary"] as? Map<String, Any?> ?: return
        val used = summary["used"] as? List<*> ?: return
        val available = summary["available"] as? List<*> ?: return

        fun entryType(list: List<*>, idx: Int): String {
            @Suppress("UNCHECKED_CAST")
            return ((list[idx] as? Map<String, Any?>)?.get("type") as? String) ?: ""
        }
        fun entryAmount(list: List<*>, idx: Int): Int {
            @Suppress("UNCHECKED_CAST")
            return ((list[idx] as? Map<String, Any?>)?.get("amount") as? Number)?.toInt() ?: 0
        }

        val memIdx = when {
            entryType(used, 0) == "memory" -> 0
            entryType(used, 1) == "memory" -> 1
            else -> { return }
        }
        mParcelMemoryUsed = entryAmount(used, memIdx) / SIZE_OF_ONE_KB
        mParcelMemoryMax = entryAmount(available, memIdx) / SIZE_OF_ONE_KB
        mGotParcelMemoryUsed = true

        val urlIdx = when {
            entryType(used, 0) == "urls" -> 0
            entryType(used, 1) == "urls" -> 1
            else -> { return }
        }
        mParcelURLsUsed = entryAmount(used, urlIdx)
        mParcelURLsMax = entryAmount(available, urlIdx)
        mGotParcelURLsUsed = true

        if (mParcelMemoryUsed >= 0 && mParcelMemoryMax >= 0) {
            val args = mutableMapOf("[COUNT]" to "$mParcelMemoryUsed")
            val translateMessage: String
            if (mParcelMemoryMax > 0) {
                val parcelMemoryAvailable = mParcelMemoryMax - mParcelMemoryUsed
                args["[MAX]"] = "$mParcelMemoryMax"
                args["[AVAILABLE]"] = "$parcelMemoryAvailable"
                translateMessage = "ScriptLimitsMemoryUsed"
            } else {
                translateMessage = "ScriptLimitsMemoryUsedSimple"
            }
            getChild<LLUICtrl>("memory_used").setValue(LLTrans.getString(translateMessage, args))
        }

        if (mParcelURLsUsed >= 0 && mParcelURLsMax >= 0) {
            val parcelUrlsAvailable = mParcelURLsMax - mParcelURLsUsed
            val args = mapOf(
                "[COUNT]" to "$mParcelURLsUsed",
                "[MAX]" to "$mParcelURLsMax",
                "[AVAILABLE]" to "$parcelUrlsAvailable"
            )
            getChild<LLUICtrl>("urls_used").setValue(LLTrans.getString("ScriptLimitsURLsUsed", args))
        }
    }

    fun clearList() {
        childGetListInterface("scripts_list")?.operateOnAll(LLCtrlListInterface.OP_DELETE)

        mGotParcelMemoryUsed = false
        mGotParcelMemoryMax = false
        mGotParcelURLsUsed = false
        mGotParcelURLsMax = false

        getChild<LLUICtrl>("memory_used").setValue("")
        getChild<LLUICtrl>("urls_used").setValue("")
        getChild<LLUICtrl>("parcels_listed").setValue("")

        mObjectListItems.clear()
        checkButtonsEnabled()
    }

    fun checkButtonsEnabled() {
        val list = getChild<LLScrollListCtrl>("scripts_list")
        val hasSelection = (list?.getNumSelected() ?: 0) > 0
        getChild<LLButton>("highlight_btn")?.setEnabled(hasSelection)
        getChild<LLButton>("return_btn")?.setEnabled(hasSelection)
    }

    fun showBeacon() {
        val list = getChild<LLScrollListCtrl>("scripts_list") ?: return
        val firstSelected = list.getFirstSelected() ?: return

        val name = firstSelected.getColumn(2)?.getValue()?.toString() ?: return
        val posString = firstSelected.getColumn(5)?.getValue()?.toString() ?: return

        val regex = Regex("<([^,]+),([^,]+),([^>]+)>")
        val match = regex.matchEntire(posString.trim()) ?: return
        val x = match.groupValues[1].trim().toFloatOrNull() ?: return
        val y = match.groupValues[2].trim().toFloatOrNull() ?: return
        val z = match.groupValues[3].trim().toFloatOrNull() ?: return

        val posAgent = floatArrayOf(x, y, z)
        val posGlobal = gAgent.getPosGlobalFromAgent(posAgent)
        LLTracker.trackLocation(posGlobal, name, "", LLTracker.LOCATION_ITEM)
    }

    private fun returnObjectsFromParcel(localId: Int) {
        val region = gAgent.getRegion() ?: return
        val list = childGetListInterface("scripts_list") ?: return
        if (list.getItemCount() == 0) return

        TODO("APR: use JVM equivalent - send ParcelReturnObjects UDP message for selected objects matching localId=$localId")
    }

    fun returnObjects() {
        val parcels = mContent["parcels"] as? List<*> ?: return
        for (i in parcels.indices) {
            @Suppress("UNCHECKED_CAST")
            val parcel = parcels[i] as? Map<String, Any?> ?: continue
            val localId = (parcel["local_id"] as? Number)?.toInt() ?: continue
            returnObjectsFromParcel(localId)
        }
        onClickRefresh()
    }

    private fun onClickRefresh() {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterScriptLimits>("script_limits") ?: return
        val tab = instance.getChild<LLTabContainer>("scriptlimits_panels") ?: return
        val panelMemory = tab.getChild<LLPanelScriptLimitsRegionMemory>("script_limits_region_memory_panel") ?: return
        panelMemory.getChild<LLButton>("refresh_list_btn")?.setEnabled(false)
        panelMemory.clearList()
        panelMemory.startRequestChain()
    }

    private fun onClickHighlight() {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterScriptLimits>("script_limits") ?: return
        val tab = instance.getChild<LLTabContainer>("scriptlimits_panels") ?: return
        val panel = tab.getChild<LLPanelScriptLimitsRegionMemory>("script_limits_region_memory_panel") ?: return
        panel.showBeacon()
    }

    private fun onClickReturn() {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterScriptLimits>("script_limits") ?: return
        val tab = instance.getChild<LLTabContainer>("scriptlimits_panels") ?: return
        val panel = tab.getChild<LLPanelScriptLimitsRegionMemory>("script_limits_region_memory_panel") ?: return
        panel.returnObjects()
    }
}
