package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

class LLPathfindingNavMeshStatus {

    enum class ENavMeshStatus {
        kPending,
        kBuilding,
        kComplete,
        kRepending
    }

    val isValid: Boolean
    val regionUUID: LLUUID
    val version: UInt
    val status: ENavMeshStatus

    constructor() {
        isValid = false
        regionUUID = LLUUID.NULL
        version = 0u
        status = ENavMeshStatus.kComplete
    }

    constructor(pRegionUUID: LLUUID) {
        isValid = false
        regionUUID = pRegionUUID
        version = 0u
        status = ENavMeshStatus.kComplete
    }

    constructor(pRegionUUID: LLUUID, pContent: Map<String, Any>) {
        isValid = true
        regionUUID = pRegionUUID
        val parsed = parseStatus(pContent)
        version = parsed.first
        status = parsed.second
    }

    constructor(pContent: Map<String, Any>) {
        isValid = true
        val uuidStr = pContent[REGION_FIELD] as? String ?: ""
        regionUUID = LLUUID.fromString(uuidStr) ?: LLUUID.NULL
        val parsed = parseStatus(pContent)
        version = parsed.first
        status = parsed.second
    }

    private constructor(
        pIsValid: Boolean,
        pRegionUUID: LLUUID,
        pVersion: UInt,
        pStatus: ENavMeshStatus
    ) {
        isValid = pIsValid
        regionUUID = pRegionUUID
        version = pVersion
        status = pStatus
    }

    fun copy(
        isValid: Boolean = this.isValid,
        regionUUID: LLUUID = this.regionUUID,
        version: UInt = this.version,
        status: ENavMeshStatus = this.status
    ): LLPathfindingNavMeshStatus = LLPathfindingNavMeshStatus(isValid, regionUUID, version, status)

    companion object {
        private const val REGION_FIELD = "region_id"
        private const val STATUS_FIELD = "status"
        private const val VERSION_FIELD = "version"

        private const val STATUS_PENDING = "pending"
        private const val STATUS_BUILDING = "building"
        private const val STATUS_COMPLETE = "complete"
        private const val STATUS_REPENDING = "repending"

        private fun parseStatus(pContent: Map<String, Any>): Pair<UInt, ENavMeshStatus> {
            val version = (pContent[VERSION_FIELD] as? Number)?.toInt()?.coerceAtLeast(0)?.toUInt() ?: 0u
            val statusStr = pContent[STATUS_FIELD] as? String ?: ""
            val status = when (statusStr) {
                STATUS_PENDING -> ENavMeshStatus.kPending
                STATUS_BUILDING -> ENavMeshStatus.kBuilding
                STATUS_COMPLETE -> ENavMeshStatus.kComplete
                STATUS_REPENDING -> ENavMeshStatus.kRepending
                else -> ENavMeshStatus.kComplete
            }
            return Pair(version, status)
        }
    }
}
