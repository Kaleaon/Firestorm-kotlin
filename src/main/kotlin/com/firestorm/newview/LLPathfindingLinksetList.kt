package com.firestorm.newview

class LLPathfindingLinksetList : LLPathfindingObjectList {

    constructor() : super()

    constructor(linksetListData: Map<String, Any>) : super() {
        parseLinksetListData(linksetListData)
    }

    fun encodeObjectFields(
        use: LLPathfindingLinkset.ELinksetUse,
        pA: Int, pB: Int, pC: Int, pD: Int
    ): MutableMap<String, Any> {
        val out = mutableMapOf<String, Any>()
        for ((uuid, obj) in getObjectMap()) {
            val linkset = obj as LLPathfindingLinkset
            if (!linkset.isTerrain) {
                val encoded = linkset.encodeAlteredFields(use, pA, pB, pC, pD)
                if (encoded.isNotEmpty()) out[uuid] = encoded
            }
        }
        return out
    }

    fun encodeTerrainFields(
        use: LLPathfindingLinkset.ELinksetUse,
        pA: Int, pB: Int, pC: Int, pD: Int
    ): MutableMap<String, Any> {
        for ((_, obj) in getObjectMap()) {
            val linkset = obj as LLPathfindingLinkset
            if (linkset.isTerrain) return linkset.encodeAlteredFields(use, pA, pB, pC, pD)
        }
        return mutableMapOf()
    }

    fun isShowUnmodifiablePhantomWarning(use: LLPathfindingLinkset.ELinksetUse): Boolean =
        getObjectMap().values.any { (it as LLPathfindingLinkset).isShowUnmodifiablePhantomWarning(use) }

    fun isShowPhantomToggleWarning(use: LLPathfindingLinkset.ELinksetUse): Boolean =
        getObjectMap().values.any { (it as LLPathfindingLinkset).isShowPhantomToggleWarning(use) }

    fun isShowCannotBeVolumeWarning(use: LLPathfindingLinkset.ELinksetUse): Boolean =
        getObjectMap().values.any { (it as LLPathfindingLinkset).isShowCannotBeVolumeWarning(use) }

    data class PossibleStates(
        val canBeWalkable: Boolean,
        val canBeStaticObstacle: Boolean,
        val canBeDynamicObstacle: Boolean,
        val canBeMaterialVolume: Boolean,
        val canBeExclusionVolume: Boolean,
        val canBeDynamicPhantom: Boolean
    )

    fun determinePossibleStates(): PossibleStates {
        var canBeWalkable        = false
        var canBeStaticObstacle  = false
        var canBeDynamicObstacle = false
        var canBeMaterialVolume  = false
        var canBeExclusionVolume = false
        var canBeDynamicPhantom  = false

        for ((_, obj) in getObjectMap()) {
            if (canBeWalkable && canBeStaticObstacle && canBeDynamicObstacle &&
                canBeMaterialVolume && canBeExclusionVolume && canBeDynamicPhantom
            ) break

            val linkset = obj as LLPathfindingLinkset
            when {
                linkset.isTerrain -> {
                    canBeWalkable = true
                }
                linkset.isModifiable -> {
                    canBeWalkable        = true
                    canBeStaticObstacle  = true
                    canBeDynamicObstacle = true
                    canBeDynamicPhantom  = true
                    if (linkset.canBeVolume) {
                        canBeMaterialVolume  = true
                        canBeExclusionVolume = true
                    }
                }
                linkset.isPhantom() -> {
                    canBeDynamicPhantom = true
                    if (linkset.canBeVolume) {
                        canBeMaterialVolume  = true
                        canBeExclusionVolume = true
                    }
                }
                else -> {
                    canBeWalkable        = true
                    canBeStaticObstacle  = true
                    canBeDynamicObstacle = true
                }
            }
        }

        return PossibleStates(
            canBeWalkable, canBeStaticObstacle, canBeDynamicObstacle,
            canBeMaterialVolume, canBeExclusionVolume, canBeDynamicPhantom
        )
    }

    private fun parseLinksetListData(linksetListData: Map<String, Any>) {
        val map = getObjectMap()
        for ((uuid, value) in linksetListData) {
            @Suppress("UNCHECKED_CAST")
            val linksetData = value as? Map<String, Any> ?: continue
            if (linksetData.isNotEmpty()) {
                map[uuid] = LLPathfindingLinkset(uuid, linksetData)
            }
        }
    }
}
