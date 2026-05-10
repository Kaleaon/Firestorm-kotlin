package com.firestorm.newview

class LLPathfindingLinkset : LLPathfindingObject {

    enum class ELinksetUse {
        UNKNOWN,
        WALKABLE,
        STATIC_OBSTACLE,
        DYNAMIC_OBSTACLE,
        MATERIAL_VOLUME,
        EXCLUSION_VOLUME,
        DYNAMIC_PHANTOM
    }

    private enum class ENavMeshGenerationCategory { IGNORE, INCLUDE, EXCLUDE }

    val isTerrain: Boolean
    val landImpact: UInt
    val isModifiable: Boolean
    var canBeVolume: Boolean
        private set
    val isScripted: Boolean
    val hasIsScripted: Boolean
    val linksetUse: ELinksetUse
    val walkabilityCoefficientA: Int
    val walkabilityCoefficientB: Int
    val walkabilityCoefficientC: Int
    val walkabilityCoefficientD: Int

    companion object {
        const val MIN_WALKABILITY_VALUE: Int = 0
        const val MAX_WALKABILITY_VALUE: Int = 100

        private const val LINKSET_LAND_IMPACT_FIELD   = "landimpact"
        private const val LINKSET_MODIFIABLE_FIELD    = "modifiable"
        private const val LINKSET_CATEGORY_FIELD      = "navmesh_category"
        private const val LINKSET_CAN_BE_VOLUME       = "can_be_volume"
        private const val LINKSET_IS_SCRIPTED_FIELD   = "is_scripted"
        private const val LINKSET_PHANTOM_FIELD       = "phantom"
        private const val LINKSET_WALKABILITY_A_FIELD = "A"
        private const val LINKSET_WALKABILITY_B_FIELD = "B"
        private const val LINKSET_WALKABILITY_C_FIELD = "C"
        private const val LINKSET_WALKABILITY_D_FIELD = "D"

        private const val CATEGORY_VALUE_INCLUDE = 0
        private const val CATEGORY_VALUE_EXCLUDE = 1
        private const val CATEGORY_VALUE_IGNORE  = 2

        fun getLinksetUseWithToggledPhantom(linksetUse: ELinksetUse): ELinksetUse =
            getLinksetUse(!isPhantom(linksetUse), getNavMeshGenerationCategory(linksetUse))

        private fun isPhantom(use: ELinksetUse): Boolean = when (use) {
            ELinksetUse.WALKABLE,
            ELinksetUse.STATIC_OBSTACLE,
            ELinksetUse.DYNAMIC_OBSTACLE -> false
            ELinksetUse.MATERIAL_VOLUME,
            ELinksetUse.EXCLUSION_VOLUME,
            ELinksetUse.DYNAMIC_PHANTOM  -> true
            ELinksetUse.UNKNOWN          -> false
        }

        private fun getLinksetUse(phantom: Boolean, category: ENavMeshGenerationCategory): ELinksetUse =
            if (phantom) {
                when (category) {
                    ENavMeshGenerationCategory.IGNORE  -> ELinksetUse.DYNAMIC_PHANTOM
                    ENavMeshGenerationCategory.INCLUDE -> ELinksetUse.MATERIAL_VOLUME
                    ENavMeshGenerationCategory.EXCLUDE -> ELinksetUse.EXCLUSION_VOLUME
                }
            } else {
                when (category) {
                    ENavMeshGenerationCategory.IGNORE  -> ELinksetUse.DYNAMIC_OBSTACLE
                    ENavMeshGenerationCategory.INCLUDE -> ELinksetUse.WALKABLE
                    ENavMeshGenerationCategory.EXCLUDE -> ELinksetUse.STATIC_OBSTACLE
                }
            }

        private fun getNavMeshGenerationCategory(use: ELinksetUse): ENavMeshGenerationCategory = when (use) {
            ELinksetUse.WALKABLE,
            ELinksetUse.MATERIAL_VOLUME   -> ENavMeshGenerationCategory.INCLUDE
            ELinksetUse.STATIC_OBSTACLE,
            ELinksetUse.EXCLUSION_VOLUME  -> ENavMeshGenerationCategory.EXCLUDE
            ELinksetUse.DYNAMIC_OBSTACLE,
            ELinksetUse.DYNAMIC_PHANTOM,
            ELinksetUse.UNKNOWN           -> ENavMeshGenerationCategory.IGNORE
        }

        private fun categoryToInt(category: ENavMeshGenerationCategory): Int = when (category) {
            ENavMeshGenerationCategory.IGNORE  -> CATEGORY_VALUE_IGNORE
            ENavMeshGenerationCategory.INCLUDE -> CATEGORY_VALUE_INCLUDE
            ENavMeshGenerationCategory.EXCLUDE -> CATEGORY_VALUE_EXCLUDE
        }

        private fun categoryFromInt(value: Int): ENavMeshGenerationCategory = when (value) {
            CATEGORY_VALUE_INCLUDE -> ENavMeshGenerationCategory.INCLUDE
            CATEGORY_VALUE_EXCLUDE -> ENavMeshGenerationCategory.EXCLUDE
            else                   -> ENavMeshGenerationCategory.IGNORE
        }
    }

    // Terrain-only constructor: no object-level UUID or object fields
    constructor(terrainData: Map<String, Any>) : super() {
        isTerrain    = true
        landImpact   = 0u
        isModifiable = false
        canBeVolume  = false
        isScripted   = false
        hasIsScripted = true
        val pf = parsePathfindingData(terrainData)
        linksetUse              = pf.use
        walkabilityCoefficientA = pf.a
        walkabilityCoefficientB = pf.b
        walkabilityCoefficientC = pf.c
        walkabilityCoefficientD = pf.d
    }

    // Regular object constructor
    constructor(uuid: String, linksetData: Map<String, Any>) : super(uuid, linksetData) {
        isTerrain = false
        val ls = parseLinksetData(linksetData)
        landImpact    = ls.landImpact
        isModifiable  = ls.isModifiable
        isScripted    = ls.isScripted
        hasIsScripted = ls.hasIsScripted
        val pf = parsePathfindingData(linksetData)
        linksetUse              = pf.use
        walkabilityCoefficientA = pf.a
        walkabilityCoefficientB = pf.b
        walkabilityCoefficientC = pf.c
        walkabilityCoefficientD = pf.d
        // can_be_volume may appear in either block; pathfinding block takes precedence if present
        canBeVolume = if (linksetData.containsKey(LINKSET_CAN_BE_VOLUME))
            linksetData[LINKSET_CAN_BE_VOLUME] as Boolean else ls.canBeVolume
    }

    constructor(other: LLPathfindingLinkset) : super(other) {
        isTerrain               = other.isTerrain
        landImpact              = other.landImpact
        isModifiable            = other.isModifiable
        canBeVolume             = other.canBeVolume
        isScripted              = other.isScripted
        hasIsScripted           = other.hasIsScripted
        linksetUse              = other.linksetUse
        walkabilityCoefficientA = other.walkabilityCoefficientA
        walkabilityCoefficientB = other.walkabilityCoefficientB
        walkabilityCoefficientC = other.walkabilityCoefficientC
        walkabilityCoefficientD = other.walkabilityCoefficientD
    }

    fun isPhantom(): Boolean = isPhantom(linksetUse)

    fun isShowUnmodifiablePhantomWarning(use: ELinksetUse): Boolean =
        !isModifiable && (isPhantom() != isPhantom(use))

    fun isShowPhantomToggleWarning(use: ELinksetUse): Boolean =
        isModifiable && (isPhantom() != isPhantom(use))

    fun isShowCannotBeVolumeWarning(use: ELinksetUse): Boolean =
        !canBeVolume && (use == ELinksetUse.MATERIAL_VOLUME || use == ELinksetUse.EXCLUSION_VOLUME)

    fun encodeAlteredFields(use: ELinksetUse, pA: Int, pB: Int, pC: Int, pD: Int): MutableMap<String, Any> {
        val out = mutableMapOf<String, Any>()

        if (!isTerrain && use != ELinksetUse.UNKNOWN && linksetUse != use &&
            (canBeVolume || (use != ELinksetUse.MATERIAL_VOLUME && use != ELinksetUse.EXCLUSION_VOLUME))
        ) {
            if (isModifiable) out[LINKSET_PHANTOM_FIELD] = isPhantom(use)
            out[LINKSET_CATEGORY_FIELD] = categoryToInt(getNavMeshGenerationCategory(use))
        }

        if (walkabilityCoefficientA != pA)
            out[LINKSET_WALKABILITY_A_FIELD] = pA.coerceIn(MIN_WALKABILITY_VALUE, MAX_WALKABILITY_VALUE)
        if (walkabilityCoefficientB != pB)
            out[LINKSET_WALKABILITY_B_FIELD] = pB.coerceIn(MIN_WALKABILITY_VALUE, MAX_WALKABILITY_VALUE)
        if (walkabilityCoefficientC != pC)
            out[LINKSET_WALKABILITY_C_FIELD] = pC.coerceIn(MIN_WALKABILITY_VALUE, MAX_WALKABILITY_VALUE)
        if (walkabilityCoefficientD != pD)
            out[LINKSET_WALKABILITY_D_FIELD] = pD.coerceIn(MIN_WALKABILITY_VALUE, MAX_WALKABILITY_VALUE)

        return out
    }

    // ---- private parse helpers ----

    private data class LinksetFields(
        val landImpact: UInt,
        val isModifiable: Boolean,
        val canBeVolume: Boolean,
        val isScripted: Boolean,
        val hasIsScripted: Boolean
    )

    private data class PathfindingFields(val use: ELinksetUse, val a: Int, val b: Int, val c: Int, val d: Int)

    private fun parseLinksetData(data: Map<String, Any>): LinksetFields {
        val impact = (requireNotNull(data[LINKSET_LAND_IMPACT_FIELD]) as Number).toInt()
        require(impact >= 0)
        val modifiable   = requireNotNull(data[LINKSET_MODIFIABLE_FIELD]) as Boolean
        val hasScripted  = data.containsKey(LINKSET_IS_SCRIPTED_FIELD)
        val scripted     = if (hasScripted) data[LINKSET_IS_SCRIPTED_FIELD] as Boolean else false
        return LinksetFields(impact.toUInt(), modifiable, canBeVolume = true, scripted, hasScripted)
    }

    private fun parsePathfindingData(data: Map<String, Any>): PathfindingFields {
        val phantom  = if (data.containsKey(LINKSET_PHANTOM_FIELD)) data[LINKSET_PHANTOM_FIELD] as Boolean else false
        val catInt   = (requireNotNull(data[LINKSET_CATEGORY_FIELD]) as Number).toInt()
        val use      = getLinksetUse(phantom, categoryFromInt(catInt))
        val a = (requireNotNull(data[LINKSET_WALKABILITY_A_FIELD]) as Number).toInt()
            .also { require(it in MIN_WALKABILITY_VALUE..MAX_WALKABILITY_VALUE) }
        val b = (requireNotNull(data[LINKSET_WALKABILITY_B_FIELD]) as Number).toInt()
            .also { require(it in MIN_WALKABILITY_VALUE..MAX_WALKABILITY_VALUE) }
        val c = (requireNotNull(data[LINKSET_WALKABILITY_C_FIELD]) as Number).toInt()
            .also { require(it in MIN_WALKABILITY_VALUE..MAX_WALKABILITY_VALUE) }
        val d = (requireNotNull(data[LINKSET_WALKABILITY_D_FIELD]) as Number).toInt()
            .also { require(it in MIN_WALKABILITY_VALUE..MAX_WALKABILITY_VALUE) }
        return PathfindingFields(use, a, b, c, d)
    }
}
