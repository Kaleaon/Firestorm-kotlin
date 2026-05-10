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

    private enum class ENavMeshGenerationCategory {
        IGNORE,
        INCLUDE,
        EXCLUDE
    }

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

        private const val LINKSET_CATEGORY_VALUE_INCLUDE = 0
        private const val LINKSET_CATEGORY_VALUE_EXCLUDE = 1
        private const val LINKSET_CATEGORY_VALUE_IGNORE  = 2

        fun getLinksetUseWithToggledPhantom(linksetUse: ELinksetUse): ELinksetUse {
            val phantom = isPhantom(linksetUse)
            val category = getNavMeshGenerationCategory(linksetUse)
            return getLinksetUse(!phantom, category)
        }

        private fun isPhantom(linksetUse: ELinksetUse): Boolean = when (linksetUse) {
            ELinksetUse.WALKABLE,
            ELinksetUse.STATIC_OBSTACLE,
            ELinksetUse.DYNAMIC_OBSTACLE -> false
            ELinksetUse.MATERIAL_VOLUME,
            ELinksetUse.EXCLUSION_VOLUME,
            ELinksetUse.DYNAMIC_PHANTOM  -> true
            ELinksetUse.UNKNOWN          -> false
        }

        private fun getLinksetUse(isPhantom: Boolean, category: ENavMeshGenerationCategory): ELinksetUse {
            return if (isPhantom) {
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
        }

        private fun getNavMeshGenerationCategory(linksetUse: ELinksetUse): ENavMeshGenerationCategory = when (linksetUse) {
            ELinksetUse.WALKABLE,
            ELinksetUse.MATERIAL_VOLUME   -> ENavMeshGenerationCategory.INCLUDE
            ELinksetUse.STATIC_OBSTACLE,
            ELinksetUse.EXCLUSION_VOLUME  -> ENavMeshGenerationCategory.EXCLUDE
            ELinksetUse.DYNAMIC_OBSTACLE,
            ELinksetUse.DYNAMIC_PHANTOM   -> ENavMeshGenerationCategory.IGNORE
            ELinksetUse.UNKNOWN           -> ENavMeshGenerationCategory.IGNORE
        }

        private fun convertCategoryToInt(category: ENavMeshGenerationCategory): Int = when (category) {
            ENavMeshGenerationCategory.IGNORE  -> LINKSET_CATEGORY_VALUE_IGNORE
            ENavMeshGenerationCategory.INCLUDE -> LINKSET_CATEGORY_VALUE_INCLUDE
            ENavMeshGenerationCategory.EXCLUDE -> LINKSET_CATEGORY_VALUE_EXCLUDE
        }

        private fun convertCategoryFromInt(value: Int): ENavMeshGenerationCategory = when (value) {
            LINKSET_CATEGORY_VALUE_IGNORE  -> ENavMeshGenerationCategory.IGNORE
            LINKSET_CATEGORY_VALUE_INCLUDE -> ENavMeshGenerationCategory.INCLUDE
            LINKSET_CATEGORY_VALUE_EXCLUDE -> ENavMeshGenerationCategory.EXCLUDE
            else                           -> ENavMeshGenerationCategory.IGNORE
        }
    }

    // Terrain constructor — no UUID, no object-level fields
    constructor(terrainData: Map<String, Any>) : super() {
        isTerrain = true
        landImpact = 0u
        isModifiable = false
        canBeVolume = false
        isScripted = false
        hasIsScripted = true
        val parsed = parsePathfindingData(terrainData)
        linksetUse = parsed.use
        walkabilityCoefficientA = parsed.a
        walkabilityCoefficientB = parsed.b
        walkabilityCoefficientC = parsed.c
        walkabilityCoefficientD = parsed.d
    }

    // Object constructor
    constructor(uuid: String, linksetData: Map<String, Any>) : super(uuid, linksetData) {
        isTerrain = false
        val linksetFields = parseLinksetData(linksetData)
        landImpact = linksetFields.landImpact
        isModifiable = linksetFields.isModifiable
        canBeVolume = linksetFields.canBeVolume
        isScripted = linksetFields.isScripted
        hasIsScripted = linksetFields.hasIsScripted
        val parsed = parsePathfindingData(linksetData)
        // can_be_volume may also appear in pathfinding block; honour it if present
        if (linksetData.containsKey(LINKSET_CAN_BE_VOLUME)) {
            canBeVolume = (linksetData[LINKSET_CAN_BE_VOLUME] as Boolean)
        }
        linksetUse = parsed.use
        walkabilityCoefficientA = parsed.a
        walkabilityCoefficientB = parsed.b
        walkabilityCoefficientC = parsed.c
        walkabilityCoefficientD = parsed.d
    }

    constructor(other: LLPathfindingLinkset) : super(other) {
        isTerrain = other.isTerrain
        landImpact = other.landImpact
        isModifiable = other.isModifiable
        canBeVolume = other.canBeVolume
        isScripted = other.isScripted
        hasIsScripted = other.hasIsScripted
        linksetUse = other.linksetUse
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
        val itemData = mutableMapOf<String, Any>()

        if (!isTerrain && use != ELinksetUse.UNKNOWN && linksetUse != use &&
            (canBeVolume || (use != ELinksetUse.MATERIAL_VOLUME && use != ELinksetUse.EXCLUSION_VOLUME))
        ) {
            if (isModifiable) {
                itemData[LINKSET_PHANTOM_FIELD] = isPhantom(use)
            }
            itemData[LINKSET_CATEGORY_FIELD] = convertCategoryToInt(getNavMeshGenerationCategory(use))
        }

        if (walkabilityCoefficientA != pA)
            itemData[LINKSET_WALKABILITY_A_FIELD] = pA.coerceIn(MIN_WALKABILITY_VALUE, MAX_WALKABILITY_VALUE)
        if (walkabilityCoefficientB != pB)
            itemData[LINKSET_WALKABILITY_B_FIELD] = pB.coerceIn(MIN_WALKABILITY_VALUE, MAX_WALKABILITY_VALUE)
        if (walkabilityCoefficientC != pC)
            itemData[LINKSET_WALKABILITY_C_FIELD] = pC.coerceIn(MIN_WALKABILITY_VALUE, MAX_WALKABILITY_VALUE)
        if (walkabilityCoefficientD != pD)
            itemData[LINKSET_WALKABILITY_D_FIELD] = pD.coerceIn(MIN_WALKABILITY_VALUE, MAX_WALKABILITY_VALUE)

        return itemData
    }

    // ---- private helpers ----

    private data class LinksetFields(
        val landImpact: UInt,
        val isModifiable: Boolean,
        val canBeVolume: Boolean,
        val isScripted: Boolean,
        val hasIsScripted: Boolean
    )

    private data class PathfindingFields(
        val use: ELinksetUse,
        val a: Int,
        val b: Int,
        val c: Int,
        val d: Int
    )

    private fun parseLinksetData(data: Map<String, Any>): LinksetFields {
        val impact = (requireNotNull(data[LINKSET_LAND_IMPACT_FIELD]) as Number).toInt()
        require(impact >= 0)
        val modifiable = requireNotNull(data[LINKSET_MODIFIABLE_FIELD]) as Boolean
        val hasScript = data.containsKey(LINKSET_IS_SCRIPTED_FIELD)
        val scripted = if (hasScript) requireNotNull(data[LINKSET_IS_SCRIPTED_FIELD]) as Boolean else false
        return LinksetFields(
            landImpact = impact.toUInt(),
            isModifiable = modifiable,
            canBeVolume = true,
            isScripted = scripted,
            hasIsScripted = hasScript
        )
    }

    private fun parsePathfindingData(data: Map<String, Any>): PathfindingFields {
        val phantom = if (data.containsKey(LINKSET_PHANTOM_FIELD))
            data[LINKSET_PHANTOM_FIELD] as Boolean else false
        val categoryInt = (requireNotNull(data[LINKSET_CATEGORY_FIELD]) as Number).toInt()
        val use = getLinksetUse(phantom, convertCategoryFromInt(categoryInt))
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
