package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

class LLPathfindingNavMesh(pRegionUUID: LLUUID) {

    enum class ENavMeshRequestStatus {
        kNavMeshRequestUnknown,
        kNavMeshRequestWaiting,
        kNavMeshRequestChecking,
        kNavMeshRequestNeedsUpdate,
        kNavMeshRequestStarted,
        kNavMeshRequestCompleted,
        kNavMeshRequestNotEnabled,
        kNavMeshRequestError
    }

    typealias NavMeshCallback = (ENavMeshRequestStatus, LLPathfindingNavMeshStatus, ByteArray) -> Unit

    private var navMeshStatus: LLPathfindingNavMeshStatus = LLPathfindingNavMeshStatus(pRegionUUID)
    private var navMeshRequestStatus: ENavMeshRequestStatus = ENavMeshRequestStatus.kNavMeshRequestUnknown
    private val navMeshListeners: MutableList<NavMeshCallback> = mutableListOf()
    private var navMeshData: ByteArray = ByteArray(0)

    fun registerNavMeshListener(pNavMeshCallback: NavMeshCallback): NavMeshCallback {
        navMeshListeners.add(pNavMeshCallback)
        return pNavMeshCallback
    }

    fun hasNavMeshVersion(pNavMeshStatus: LLPathfindingNavMeshStatus): Boolean {
        return (navMeshStatus.version == pNavMeshStatus.version) &&
            (navMeshRequestStatus == ENavMeshRequestStatus.kNavMeshRequestStarted ||
                navMeshRequestStatus == ENavMeshRequestStatus.kNavMeshRequestCompleted ||
                (navMeshRequestStatus == ENavMeshRequestStatus.kNavMeshRequestChecking && navMeshData.isNotEmpty()))
    }

    fun handleNavMeshWaitForRegionLoad() {
        setRequestStatus(ENavMeshRequestStatus.kNavMeshRequestWaiting)
    }

    fun handleNavMeshCheckVersion() {
        setRequestStatus(ENavMeshRequestStatus.kNavMeshRequestChecking)
    }

    fun handleRefresh(pNavMeshStatus: LLPathfindingNavMeshStatus) {
        check(navMeshStatus.regionUUID == pNavMeshStatus.regionUUID)
        check(navMeshStatus.version == pNavMeshStatus.version)
        navMeshStatus = pNavMeshStatus
        if (navMeshRequestStatus == ENavMeshRequestStatus.kNavMeshRequestChecking) {
            check(navMeshData.isNotEmpty())
            setRequestStatus(ENavMeshRequestStatus.kNavMeshRequestCompleted)
        } else {
            sendStatus()
        }
    }

    fun handleNavMeshNewVersion(pNavMeshStatus: LLPathfindingNavMeshStatus) {
        check(navMeshStatus.regionUUID == pNavMeshStatus.regionUUID)
        if (navMeshStatus.version == pNavMeshStatus.version) {
            navMeshStatus = pNavMeshStatus
            sendStatus()
        } else {
            navMeshData = ByteArray(0)
            navMeshStatus = pNavMeshStatus
            setRequestStatus(ENavMeshRequestStatus.kNavMeshRequestNeedsUpdate)
        }
    }

    fun handleNavMeshStart(pNavMeshStatus: LLPathfindingNavMeshStatus) {
        check(navMeshStatus.regionUUID == pNavMeshStatus.regionUUID)
        navMeshStatus = pNavMeshStatus
        setRequestStatus(ENavMeshRequestStatus.kNavMeshRequestStarted)
    }

    fun handleNavMeshResult(pContent: Map<String, Any>, pNavMeshVersion: UInt) {
        var navMeshVersion = pNavMeshVersion
        val embeddedVersion = (pContent[NAVMESH_VERSION_FIELD] as? Number)?.toInt()?.coerceAtLeast(0)?.toUInt()
        if (embeddedVersion != null && embeddedVersion != navMeshVersion) {
            navMeshVersion = embeddedVersion
        }

        if (navMeshStatus.version == navMeshVersion) {
            val status: ENavMeshRequestStatus
            val rawData = pContent[NAVMESH_DATA_FIELD] as? ByteArray
            if (rawData != null) {
                val decompressed = TODO("APR: use JVM equivalent — decompress zlib/gzip navmesh binary from rawData") as? ByteArray
                if (decompressed == null) {
                    status = ENavMeshRequestStatus.kNavMeshRequestError
                } else {
                    navMeshData = decompressed
                    status = ENavMeshRequestStatus.kNavMeshRequestCompleted
                }
            } else {
                status = ENavMeshRequestStatus.kNavMeshRequestError
            }
            @Suppress("UNREACHABLE_CODE")
            setRequestStatus(status)
        }
    }

    fun handleNavMeshNotEnabled() {
        navMeshData = ByteArray(0)
        setRequestStatus(ENavMeshRequestStatus.kNavMeshRequestNotEnabled)
    }

    fun handleNavMeshError() {
        navMeshData = ByteArray(0)
        setRequestStatus(ENavMeshRequestStatus.kNavMeshRequestError)
    }

    fun handleNavMeshError(pNavMeshVersion: UInt) {
        if (navMeshStatus.version == pNavMeshVersion) {
            handleNavMeshError()
        }
    }

    private fun setRequestStatus(pNavMeshRequestStatus: ENavMeshRequestStatus) {
        navMeshRequestStatus = pNavMeshRequestStatus
        sendStatus()
    }

    private fun sendStatus() {
        navMeshListeners.forEach { it(navMeshRequestStatus, navMeshStatus, navMeshData) }
    }

    companion object {
        private const val NAVMESH_VERSION_FIELD = "navmesh_version"
        private const val NAVMESH_DATA_FIELD = "navmesh_data"
    }
}
