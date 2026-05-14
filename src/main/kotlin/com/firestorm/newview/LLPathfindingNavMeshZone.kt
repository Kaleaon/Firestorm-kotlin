package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

class LLPathfindingNavMeshZone {

    enum class ENavMeshZoneRequestStatus {
        kNavMeshZoneRequestUnknown,
        kNavMeshZoneRequestWaiting,
        kNavMeshZoneRequestChecking,
        kNavMeshZoneRequestNeedsUpdate,
        kNavMeshZoneRequestStarted,
        kNavMeshZoneRequestCompleted,
        kNavMeshZoneRequestNotEnabled,
        kNavMeshZoneRequestError
    }

    enum class ENavMeshZoneStatus {
        kNavMeshZonePending,
        kNavMeshZoneBuilding,
        kNavMeshZoneSomePending,
        kNavMeshZoneSomeBuilding,
        kNavMeshZonePendingAndBuilding,
        kNavMeshZoneComplete
    }

    typealias NavMeshZoneCallback = (ENavMeshZoneRequestStatus) -> Unit

    private val navMeshLocations: MutableList<NavMeshLocation> = mutableListOf()
    private var navMeshZoneRequestStatus: ENavMeshZoneRequestStatus = ENavMeshZoneRequestStatus.kNavMeshZoneRequestUnknown
    private val navMeshZoneListeners: MutableList<NavMeshZoneCallback> = mutableListOf()

    fun registerNavMeshZoneListener(pNavMeshZoneCallback: NavMeshZoneCallback): NavMeshZoneCallback {
        navMeshZoneListeners.add(pNavMeshZoneCallback)
        return pNavMeshZoneCallback
    }

    fun initialize() {
        navMeshLocations.clear()
        navMeshLocations.add(NavMeshLocation(CENTER_REGION, ::handleNavMeshLocation))

        System.err.println("LLPathfindingNavMeshZone: initialize neighborRegionDir not yet implemented")
        val neighborRegionDir = 0
        if (neighborRegionDir != CENTER_REGION) {
            navMeshLocations.add(NavMeshLocation(neighborRegionDir, ::handleNavMeshLocation))
        }
    }

    fun enable() {
        navMeshLocations.forEach { it.enable() }
    }

    fun disable() {
        navMeshLocations.forEach { it.disable() }
    }

    fun refresh() {
        // no-op
        navMeshLocations.forEach { it.refresh() }
    }

    fun getNavMeshZoneStatus(): ENavMeshZoneStatus {
        var hasPending = false
        var hasBuilding = false
        var hasComplete = false
        var hasRepending = false

        for (loc in navMeshLocations) {
            when (loc.getNavMeshStatus()) {
                LLPathfindingNavMeshStatus.ENavMeshStatus.kPending -> hasPending = true
                LLPathfindingNavMeshStatus.ENavMeshStatus.kBuilding -> hasBuilding = true
                LLPathfindingNavMeshStatus.ENavMeshStatus.kComplete -> hasComplete = true
                LLPathfindingNavMeshStatus.ENavMeshStatus.kRepending -> hasRepending = true
            }
        }

        return when {
            hasRepending || (hasPending && hasBuilding) -> ENavMeshZoneStatus.kNavMeshZonePendingAndBuilding
            hasComplete && hasPending -> ENavMeshZoneStatus.kNavMeshZoneSomePending
            hasComplete && hasBuilding -> ENavMeshZoneStatus.kNavMeshZoneSomeBuilding
            hasComplete -> ENavMeshZoneStatus.kNavMeshZoneComplete
            hasPending -> ENavMeshZoneStatus.kNavMeshZonePending
            hasBuilding -> ENavMeshZoneStatus.kNavMeshZoneBuilding
            else -> ENavMeshZoneStatus.kNavMeshZoneComplete
        }
    }

    private fun handleNavMeshLocation() {
        updateStatus()
    }

    private fun updateStatus() {
        var hasRequestUnknown = false
        var hasRequestWaiting = false
        var hasRequestChecking = false
        var hasRequestNeedsUpdate = false
        var hasRequestStarted = false
        var hasRequestCompleted = false
        var hasRequestNotEnabled = false
        var hasRequestError = false

        for (loc in navMeshLocations) {
            when (loc.getRequestStatus()) {
                LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestUnknown -> hasRequestUnknown = true
                LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestWaiting -> hasRequestWaiting = true
                LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestChecking -> hasRequestChecking = true
                LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestNeedsUpdate -> hasRequestNeedsUpdate = true
                LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestStarted -> hasRequestStarted = true
                LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestCompleted -> hasRequestCompleted = true
                LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestNotEnabled -> hasRequestNotEnabled = true
                LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestError -> hasRequestError = true
            }
        }

        val zoneRequestStatus = when {
            hasRequestWaiting -> ENavMeshZoneRequestStatus.kNavMeshZoneRequestWaiting
            hasRequestNeedsUpdate -> ENavMeshZoneRequestStatus.kNavMeshZoneRequestNeedsUpdate
            hasRequestChecking -> ENavMeshZoneRequestStatus.kNavMeshZoneRequestChecking
            hasRequestStarted -> ENavMeshZoneRequestStatus.kNavMeshZoneRequestStarted
            hasRequestError -> ENavMeshZoneRequestStatus.kNavMeshZoneRequestError
            hasRequestUnknown -> ENavMeshZoneRequestStatus.kNavMeshZoneRequestUnknown
            hasRequestCompleted -> ENavMeshZoneRequestStatus.kNavMeshZoneRequestCompleted
            hasRequestNotEnabled -> ENavMeshZoneRequestStatus.kNavMeshZoneRequestNotEnabled
            else -> ENavMeshZoneRequestStatus.kNavMeshZoneRequestError
        }

        if (navMeshZoneRequestStatus != ENavMeshZoneRequestStatus.kNavMeshZoneRequestCompleted &&
            zoneRequestStatus == ENavMeshZoneRequestStatus.kNavMeshZoneRequestCompleted
        ) {
            // no-op
        }

        navMeshZoneRequestStatus = zoneRequestStatus
        navMeshZoneListeners.forEach { it(navMeshZoneRequestStatus) }
    }

    private inner class NavMeshLocation(
        private val direction: Int,
        private val locationCallback: () -> Unit
    ) {
        private var regionUUID: LLUUID = LLUUID.NULL
        private var hasNavMesh: Boolean = false
        private var navMeshVersion: UInt = 0u
        private var navMeshStatus: LLPathfindingNavMeshStatus.ENavMeshStatus =
            LLPathfindingNavMeshStatus.ENavMeshStatus.kComplete
        private var requestStatus: LLPathfindingNavMesh.ENavMeshRequestStatus =
            LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestUnknown
        private var navMeshListener: LLPathfindingNavMesh.NavMeshCallback? = null

        fun enable() {
            clear()
            val region = getRegion()
            if (region == null) {
                regionUUID = LLUUID.NULL
            } else {
                regionUUID = region.regionId
                val listener: LLPathfindingNavMesh.NavMeshCallback = { status, meshStatus, data ->
                    handleNavMesh(status, meshStatus, data)
                }
                navMeshListener = listener
                System.err.println("LLPathfindingNavMeshZone: enable registerNavMeshListenerForRegion not yet implemented")
            }
        }

        fun refresh() {
            val region = getRegion()
            if (region == null) {
                check(regionUUID.isNull())
                val newStatus = LLPathfindingNavMeshStatus(regionUUID)
                handleNavMesh(
                    LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestNotEnabled,
                    newStatus,
                    ByteArray(0)
                )
            } else {
                check(regionUUID == region.regionId)
                System.err.println("LLPathfindingNavMeshZone: refresh requestGetNavMeshForRegion not yet implemented")
            }
        }

        fun disable() {
            clear()
        }

        fun getRequestStatus(): LLPathfindingNavMesh.ENavMeshRequestStatus = requestStatus

        fun getNavMeshStatus(): LLPathfindingNavMeshStatus.ENavMeshStatus = navMeshStatus

        private fun handleNavMesh(
            pNavMeshRequestStatus: LLPathfindingNavMesh.ENavMeshRequestStatus,
            pNavMeshStatus: LLPathfindingNavMeshStatus,
            pNavMeshData: ByteArray
        ) {
            check(regionUUID == pNavMeshStatus.regionUUID)

            if (pNavMeshRequestStatus == LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestCompleted &&
                (!hasNavMesh || navMeshVersion != pNavMeshStatus.version)
            ) {
                check(pNavMeshData.isNotEmpty())
                hasNavMesh = true
                navMeshVersion = pNavMeshStatus.version
                // no-op
            }

            requestStatus = pNavMeshRequestStatus
            navMeshStatus = pNavMeshStatus.status
            locationCallback()
        }

        private fun clear() {
            hasNavMesh = false
            requestStatus = LLPathfindingNavMesh.ENavMeshRequestStatus.kNavMeshRequestUnknown
            navMeshStatus = LLPathfindingNavMeshStatus.ENavMeshStatus.kComplete
            navMeshListener = null
        }

        private fun getRegion(): ViewerRegion? {
            System.err.println("LLPathfindingNavMeshZone: getRegion not yet implemented")
            return null
        }
    }

    companion object {
        private const val CENTER_REGION = 99
    }
}
