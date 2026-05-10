package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// Singleton manager for all pathfinding operations in the current region.
// Coroutine-based HTTP calls are stubbed with TODO markers because the JVM
// HTTP stack (OkHttp / Ktor / etc.) replaces LLCoreHttpUtil coroutines.
object LLPathfindingManager {

    // ---- capability service name constants ----
    private const val CAP_RETRIEVE_NAVMESH         = "RetrieveNavMeshSrc"
    private const val CAP_NAVMESH_STATUS           = "NavMeshGenerationStatus"
    private const val CAP_GET_OBJECT_LINKSETS      = "RegionObjects"
    private const val CAP_SET_OBJECT_LINKSETS      = "ObjectNavMeshProperties"
    private const val CAP_TERRAIN_LINKSETS         = "TerrainNavMeshProperties"
    private const val CAP_CHARACTERS               = "CharacterProperties"
    private const val CAP_AGENT_STATE              = "AgentState"
    private const val AGENT_STATE_CAN_REBAKE_FIELD = "can_modify_navmesh"

    // ---- public type aliases ----
    typealias RequestId = UInt
    typealias ObjectRequestCallback = (RequestId, ERequestStatus, LLPathfindingObjectList?) -> Unit
    typealias AgentStateCallback = (Boolean) -> Unit
    typealias RebakeNavMeshCallback = (Boolean) -> Unit

    enum class ERequestStatus {
        REQUEST_STARTED,
        REQUEST_COMPLETED,
        REQUEST_NOT_ENABLED,
        REQUEST_ERROR
    }

    // boost::signals2 agent-state listeners → plain list of lambdas
    private val agentStateListeners: MutableList<AgentStateCallback> = mutableListOf()

    // region LLUUID → nav mesh, mirrors C++ NavMeshMap
    private val navMeshMap: MutableMap<LLUUID, LLPathfindingNavMesh> = mutableMapOf()

    // ---- system lifecycle ----

    fun initSystem() {
        TODO("APR: use JVM equivalent of LLPathingLib::initSystem()")
    }

    fun quitSystem() {
        TODO("APR: use JVM equivalent of LLPathingLib::quitSystem()")
    }

    // ---- capability / region queries ----

    fun isPathfindingViewEnabled(): Boolean {
        TODO("APR: use JVM equivalent of LLPathingLib::getInstance() != NULL")
    }

    fun isPathfindingEnabledForCurrentRegion(): Boolean =
        isPathfindingEnabledForRegion(getCurrentRegion())

    fun isPathfindingEnabledForRegion(region: ViewerRegion?): Boolean =
        getRetrieveNavMeshURLForRegion(region).isNotEmpty()

    fun isAllowViewTerrainProperties(): Boolean {
        TODO("APR: use JVM equivalent of gAgent.isGodlike() || region.canManageEstate()")
    }

    // ---- nav mesh ----

    fun registerNavMeshListenerForRegion(
        region: ViewerRegion?,
        callback: LLPathfindingNavMesh.NavMeshCallback
    ): LLPathfindingNavMesh.NavMeshCallback =
        getNavMeshForRegion(region).registerNavMeshListener(callback)

    fun requestGetNavMeshForRegion(region: ViewerRegion?, isGetStatusOnly: Boolean) {
        val navMesh = getNavMeshForRegion(region)
        when {
            region == null -> navMesh.handleNavMeshNotEnabled()
            !region.capabilitiesReceived() -> {
                navMesh.handleNavMeshWaitForRegionLoad()
                region.addCapsReceivedCallback { regionUUID, _ ->
                    handleDeferredGetNavMeshForRegion(regionUUID, isGetStatusOnly)
                }
            }
            !isPathfindingEnabledForRegion(region) -> navMesh.handleNavMeshNotEnabled()
            else -> {
                val statusUrl = getNavMeshStatusURLForRegion(region)
                check(statusUrl.isNotEmpty())
                navMesh.handleNavMeshCheckVersion()
                navMeshStatusRequestCoro(statusUrl, region.handle, isGetStatusOnly)
            }
        }
    }

    // ---- linksets ----

    fun requestGetLinksets(requestId: RequestId, callback: ObjectRequestCallback) {
        val currentRegion = getCurrentRegion()
        when {
            currentRegion == null -> callback(requestId, ERequestStatus.REQUEST_NOT_ENABLED, null)
            !currentRegion.capabilitiesReceived() -> {
                callback(requestId, ERequestStatus.REQUEST_STARTED, null)
                currentRegion.addCapsReceivedCallback { regionUUID, _ ->
                    handleDeferredGetLinksetsForRegion(regionUUID, requestId, callback)
                }
            }
            else -> {
                val objectUrl  = getRetrieveObjectLinksetsURLForCurrentRegion()
                val terrainUrl = getTerrainLinksetsURLForCurrentRegion()
                if (objectUrl.isEmpty() || terrainUrl.isEmpty()) {
                    callback(requestId, ERequestStatus.REQUEST_NOT_ENABLED, null)
                } else {
                    callback(requestId, ERequestStatus.REQUEST_STARTED, null)
                    val doTerrain = isAllowViewTerrainProperties()
                    val responder = LinksetsResponder(requestId, callback, objectRequested = true, terrainRequested = doTerrain)
                    linksetObjectsCoro(objectUrl, responder, putData = null)
                    if (doTerrain) linksetTerrainCoro(terrainUrl, responder, putData = null)
                }
            }
        }
    }

    fun requestSetLinksets(
        requestId: RequestId,
        linksetList: LLPathfindingObjectList?,
        use: LLPathfindingLinkset.ELinksetUse,
        pA: Int, pB: Int, pC: Int, pD: Int,
        callback: ObjectRequestCallback
    ) {
        val objectUrl  = getChangeObjectLinksetsURLForCurrentRegion()
        val terrainUrl = getTerrainLinksetsURLForCurrentRegion()
        if (objectUrl.isEmpty() || terrainUrl.isEmpty()) {
            callback(requestId, ERequestStatus.REQUEST_NOT_ENABLED, null)
            return
        }
        if (linksetList == null || linksetList.isEmpty()) {
            callback(requestId, ERequestStatus.REQUEST_COMPLETED, null)
            return
        }
        val list = linksetList as LLPathfindingLinksetList
        val objectPutData  = list.encodeObjectFields(use, pA, pB, pC, pD)
        val terrainPutData = if (isAllowViewTerrainProperties())
            list.encodeTerrainFields(use, pA, pB, pC, pD) else mutableMapOf()

        if (objectPutData.isEmpty() && terrainPutData.isEmpty()) {
            callback(requestId, ERequestStatus.REQUEST_COMPLETED, null)
            return
        }

        callback(requestId, ERequestStatus.REQUEST_STARTED, null)
        val responder = LinksetsResponder(
            requestId, callback,
            objectRequested  = objectPutData.isNotEmpty(),
            terrainRequested = terrainPutData.isNotEmpty()
        )
        if (objectPutData.isNotEmpty())  linksetObjectsCoro(objectUrl, responder, objectPutData)
        if (terrainPutData.isNotEmpty()) linksetTerrainCoro(terrainUrl, responder, terrainPutData)
    }

    fun requestGetCharacters(requestId: RequestId, callback: ObjectRequestCallback) {
        val currentRegion = getCurrentRegion()
        when {
            currentRegion == null -> callback(requestId, ERequestStatus.REQUEST_NOT_ENABLED, null)
            !currentRegion.capabilitiesReceived() -> {
                callback(requestId, ERequestStatus.REQUEST_STARTED, null)
                currentRegion.addCapsReceivedCallback { regionUUID, _ ->
                    handleDeferredGetCharactersForRegion(regionUUID, requestId, callback)
                }
            }
            else -> {
                val url = getCharactersURLForCurrentRegion()
                if (url.isEmpty()) {
                    callback(requestId, ERequestStatus.REQUEST_NOT_ENABLED, null)
                } else {
                    callback(requestId, ERequestStatus.REQUEST_STARTED, null)
                    charactersCoro(url, requestId, callback)
                }
            }
        }
    }

    // ---- agent state ----

    fun registerAgentStateListener(callback: AgentStateCallback): AgentStateCallback {
        agentStateListeners.add(callback)
        return callback
    }

    fun unregisterAgentStateListener(token: AgentStateCallback) {
        agentStateListeners.remove(token)
    }

    fun requestGetAgentState() {
        val currentRegion = getCurrentRegion()
        when {
            currentRegion == null -> fireAgentStateSignal(false)
            !currentRegion.capabilitiesReceived() ->
                currentRegion.addCapsReceivedCallback { regionUUID, _ ->
                    handleDeferredGetAgentStateForRegion(regionUUID)
                }
            !isPathfindingEnabledForRegion(currentRegion) -> fireAgentStateSignal(false)
            else -> {
                val url = getAgentStateURLForRegion(currentRegion)
                check(url.isNotEmpty())
                navAgentStateRequestCoro(url)
            }
        }
    }

    fun requestRebakeNavMesh(callback: RebakeNavMeshCallback) {
        val currentRegion = getCurrentRegion()
        if (currentRegion == null || !isPathfindingEnabledForRegion(currentRegion)) {
            callback(false)
            return
        }
        val url = getNavMeshStatusURLForCurrentRegion()
        check(url.isNotEmpty())
        navMeshRebakeCoro(url, callback)
    }

    // ---- deferred handlers (fired when region capabilities become available) ----

    private fun handleDeferredGetAgentStateForRegion(regionUUID: LLUUID) {
        val current = getCurrentRegion()
        if (current != null && current.regionId == regionUUID) requestGetAgentState()
    }

    private fun handleDeferredGetNavMeshForRegion(regionUUID: LLUUID, isGetStatusOnly: Boolean) {
        val current = getCurrentRegion()
        if (current != null && current.regionId == regionUUID) requestGetNavMeshForRegion(current, isGetStatusOnly)
    }

    private fun handleDeferredGetLinksetsForRegion(
        regionUUID: LLUUID,
        requestId: RequestId,
        callback: ObjectRequestCallback
    ) {
        val current = getCurrentRegion()
        if (current != null && current.regionId == regionUUID) requestGetLinksets(requestId, callback)
    }

    private fun handleDeferredGetCharactersForRegion(
        regionUUID: LLUUID,
        requestId: RequestId,
        callback: ObjectRequestCallback
    ) {
        val current = getCurrentRegion()
        if (current != null && current.regionId == regionUUID) requestGetCharacters(requestId, callback)
    }

    // ---- coroutine stubs (replace with JVM async HTTP) ----

    private fun navMeshStatusRequestCoro(url: String, regionHandle: ULong, isGetStatusOnly: Boolean) {
        TODO("APR: use JVM equivalent — GET $url, parse LLPathfindingNavMeshStatus, call navMesh handle* methods")
    }

    private fun navAgentStateRequestCoro(url: String) {
        TODO("APR: use JVM equivalent — GET $url, read '$AGENT_STATE_CAN_REBAKE_FIELD', call handleAgentState()")
    }

    private fun navMeshRebakeCoro(url: String, callback: RebakeNavMeshCallback) {
        TODO("APR: use JVM equivalent — POST $url {\"command\":\"rebuild\"}, call callback(success)")
    }

    // putData == null → GET; non-null → PUT
    private fun linksetObjectsCoro(url: String, responder: LinksetsResponder, putData: Map<String, Any>?) {
        TODO("APR: use JVM equivalent — ${if (putData == null) "GET" else "PUT"} $url, " +
                "call responder.handleObjectLinksetsResult / handleObjectLinksetsError")
    }

    private fun linksetTerrainCoro(url: String, responder: LinksetsResponder, putData: Map<String, Any>?) {
        TODO("APR: use JVM equivalent — ${if (putData == null) "GET" else "PUT"} $url, " +
                "call responder.handleTerrainLinksetsResult / handleTerrainLinksetsError")
    }

    private fun charactersCoro(url: String, requestId: RequestId, callback: ObjectRequestCallback) {
        TODO("APR: use JVM equivalent — GET $url, wrap result in LLPathfindingCharacterList, call callback")
    }

    // ---- internal state updates (invoked from sim-push message handlers) ----

    internal fun handleNavMeshStatusUpdate(navMeshStatus: LLPathfindingNavMeshStatus) {
        val navMesh = getNavMeshForRegion(navMeshStatus.regionUUID)
        if (!navMeshStatus.isValid) navMesh.handleNavMeshError()
        else navMesh.handleNavMeshNewVersion(navMeshStatus)
    }

    internal fun handleAgentState(canRebakeRegion: Boolean) {
        fireAgentStateSignal(canRebakeRegion)
    }

    private fun fireAgentStateSignal(value: Boolean) {
        agentStateListeners.forEach { it(value) }
    }

    // ---- nav mesh map ----

    private fun getNavMeshForRegion(regionUUID: LLUUID): LLPathfindingNavMesh =
        navMeshMap.getOrPut(regionUUID) { LLPathfindingNavMesh(regionUUID) }

    private fun getNavMeshForRegion(region: ViewerRegion?): LLPathfindingNavMesh =
        getNavMeshForRegion(region?.regionId ?: LLUUID.NULL)

    // ---- capability URL helpers ----

    private fun getNavMeshStatusURLForCurrentRegion(): String =
        getNavMeshStatusURLForRegion(getCurrentRegion())

    private fun getNavMeshStatusURLForRegion(region: ViewerRegion?): String =
        getCapabilityURLForRegion(region, CAP_NAVMESH_STATUS)

    private fun getRetrieveNavMeshURLForRegion(region: ViewerRegion?): String =
        getCapabilityURLForRegion(region, CAP_RETRIEVE_NAVMESH)

    private fun getRetrieveObjectLinksetsURLForCurrentRegion(): String =
        getCapabilityURLForCurrentRegion(CAP_GET_OBJECT_LINKSETS)

    private fun getChangeObjectLinksetsURLForCurrentRegion(): String =
        getCapabilityURLForCurrentRegion(CAP_SET_OBJECT_LINKSETS)

    private fun getTerrainLinksetsURLForCurrentRegion(): String =
        getCapabilityURLForCurrentRegion(CAP_TERRAIN_LINKSETS)

    private fun getCharactersURLForCurrentRegion(): String =
        getCapabilityURLForCurrentRegion(CAP_CHARACTERS)

    private fun getAgentStateURLForRegion(region: ViewerRegion?): String =
        getCapabilityURLForRegion(region, CAP_AGENT_STATE)

    private fun getCapabilityURLForCurrentRegion(capabilityName: String): String =
        getCapabilityURLForRegion(getCurrentRegion(), capabilityName)

    private fun getCapabilityURLForRegion(region: ViewerRegion?, capabilityName: String): String {
        if (region != null) {
            val url = region.getCapability(capabilityName)
            if (url.isNotEmpty()) return url
        }
        return ""
    }

    private fun getCurrentRegion(): ViewerRegion? {
        TODO("APR: use JVM equivalent of gAgent.getRegion()")
    }

    // ---- LinksetsResponder — coordinates parallel object + terrain HTTP responses ----

    private class LinksetsResponder(
        private val requestId: RequestId,
        private val callback: ObjectRequestCallback,
        objectRequested: Boolean,
        terrainRequested: Boolean
    ) {
        private enum class EMessagingState { NOT_REQUESTED, WAITING, RECEIVED_GOOD, RECEIVED_ERROR }

        private var objectState  = if (objectRequested)  EMessagingState.WAITING else EMessagingState.NOT_REQUESTED
        private var terrainState = if (terrainRequested) EMessagingState.WAITING else EMessagingState.NOT_REQUESTED

        private var objectLinksetList: LLPathfindingObjectList? = null
        private var terrainLinkset: LLPathfindingObject?        = null

        fun handleObjectLinksetsResult(content: Map<String, Any>) {
            objectLinksetList = LLPathfindingLinksetList(content)
            objectState = EMessagingState.RECEIVED_GOOD
            if (terrainState != EMessagingState.WAITING) sendCallback()
        }

        fun handleObjectLinksetsError() {
            objectState = EMessagingState.RECEIVED_ERROR
            if (terrainState != EMessagingState.WAITING) sendCallback()
        }

        fun handleTerrainLinksetsResult(content: Map<String, Any>) {
            terrainLinkset = LLPathfindingLinkset(content)
            terrainState = EMessagingState.RECEIVED_GOOD
            if (objectState != EMessagingState.WAITING) sendCallback()
        }

        fun handleTerrainLinksetsError() {
            terrainState = EMessagingState.RECEIVED_ERROR
            if (objectState != EMessagingState.WAITING) sendCallback()
        }

        private fun sendCallback() {
            check(objectState  != EMessagingState.WAITING)
            check(terrainState != EMessagingState.WAITING)

            val allGood =
                (objectState  == EMessagingState.RECEIVED_GOOD || objectState  == EMessagingState.NOT_REQUESTED) &&
                (terrainState == EMessagingState.RECEIVED_GOOD || terrainState == EMessagingState.NOT_REQUESTED)
            val status = if (allGood) ERequestStatus.REQUEST_COMPLETED else ERequestStatus.REQUEST_ERROR

            if (objectState != EMessagingState.RECEIVED_GOOD) {
                objectLinksetList = LLPathfindingLinksetList()
            }

            if (terrainState == EMessagingState.RECEIVED_GOOD) {
                terrainLinkset?.let { objectLinksetList?.update(it) }
            }

            callback(requestId, status, objectLinksetList)
        }
    }
}
