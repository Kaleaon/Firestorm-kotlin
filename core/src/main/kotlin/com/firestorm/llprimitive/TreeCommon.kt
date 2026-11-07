/**
 * TreeCommon.kt
 * Kotlin conversion of lltree_common.h (indra/llprimitive)
 *
 * Shared tree-type definitions and per-species data used by both the
 * simulator and the viewer.  The C++ header only defined the raw genome
 * struct (LLTree_gene_0); species lookup tables live in llvotree.cpp in
 * the viewer.  This file provides the canonical type catalogue used
 * throughout the Kotlin port.
 */

package com.firestorm.llprimitive

// ---------------------------------------------------------------------------
// TreeType – all known Second Life tree species
// Ordinal values match the EGeneticCode / species-ID used on the wire.
// ---------------------------------------------------------------------------
enum class TreeType(val id: Int) {
    PINE(0),            // Linden Pine
    OAK(1),             // Oak
    TROPICAL_BUSH_1(2),
    PALM_1(3),
    DOGWOOD(4),
    TROPICAL_BUSH_2(5),
    PALM_2(6),
    CYPRESS_1(7),
    CYPRESS_2(8),
    PLUMERIA(9),
    WINTER_PINE_1(10),
    WINTER_ASPEN(11),
    WINTER_PINE_2(12),
    EUCALYPTUS(13),
    FERN(14),
    EELGRASS(15),
    SEA_SWORD(16),
    KELP_1(17),
    BEACH_GRASS_1(18),
    KELP_2(19);

    companion object {
        const val NUM_TREES: Int = 20

        fun fromId(id: Int): TreeType? = entries.firstOrNull { it.id == id }
    }
}

// ---------------------------------------------------------------------------
// TreeGene – raw genome byte-struct  (mirrors LLTree_gene_0)
// Each byte field encodes a parameter in [0, 255].
// ---------------------------------------------------------------------------
data class TreeGene(
    /** Overall scale: divide by 50 to get metres. */
    val scale: UByte           = 0u,
    /** Number of branches at each fork. */
    val branches: UByte        = 0u,
    /** Twist around old branch axis (°): value / 255 * 180. */
    val twist: UByte           = 0u,
    /** Droop away from old axis (°): value / 255 * 180. */
    val droop: UByte           = 0u,
    /** Branch-colour index. */
    val species: UByte         = 0u,
    /** Max recursion depth for the main trunk. */
    val trunkDepth: UByte      = 0u,
    /** Trunk thickness scale: divide by 50 to get metres. */
    val branchThickness: UByte = 0u,
    /** Branch recursion depth to flower / leaf. */
    val maxDepth: UByte        = 0u,
    /** Scale multiplier per recursion step (converts to 0..1f). */
    val scaleStep: UByte       = 0u,
)

// ---------------------------------------------------------------------------
// TreeSpeciesData – viewer-side per-species rendering parameters
// Fields mirror the struct populated in LLVOTree::initClass().
// ---------------------------------------------------------------------------
data class TreeSpeciesData(
    /** Base height of the tree in metres. */
    val scale: Float           = 1f,
    /** Whether this species uses a billboard (flat-card) LOD. */
    val billboard: Boolean     = false,
    /** Noise amplitude applied to branch positions. */
    val noise: Float           = 0f,
    /** Taper factor (how much the trunk narrows toward the top). */
    val taper: Float           = 0f,
    /** Maximum recursion depth of the trunk. */
    val trunkDepth: Int        = 1,
    /** Relative length of each branch segment. */
    val branchLength: Float    = 0.8f,
    /** Relative length of the trunk. */
    val trunkLength: Float     = 1f,
    /** Scale of individual leaf cards. */
    val leafScale: Float       = 1f,
    /** Radius of the leaf-ball cluster. */
    val ballSize: Float        = 1f,
    /** Vertical offset of the leaf-ball from the trunk tip. */
    val ballOffset: Float      = 0f,
    /** Number of branches rendered at low-LOD. */
    val lowLODBranches: Int    = 0,
    /** Maximum branch recursion levels. */
    val numBranchLevels: Int   = 1,
)

// ---------------------------------------------------------------------------
// TreeCommon – singleton holding the species lookup table
// ---------------------------------------------------------------------------
object TreeCommon {

    /**
     * Representative per-species rendering data.
     *
     * The full table lives in LLVOTree::initClass() in the viewer C++.
     * Values here are taken from the most commonly referenced species so
     * the Kotlin runtime has something to work with; the remaining entries
     * should be filled in when LLVOTree is ported.
     */
    val sSpeciesTable: Map<TreeType, TreeSpeciesData> = mapOf(
        TreeType.PINE to TreeSpeciesData(
            scale          = 4f,
            billboard      = false,
            noise          = 0.05f,
            taper          = 0.8f,
            trunkDepth     = 1,
            branchLength   = 0.8f,
            trunkLength    = 1.0f,
            leafScale      = 1.0f,
            ballSize       = 1.0f,
            ballOffset     = 0.0f,
            lowLODBranches = 2,
            numBranchLevels = 2,
        ),
        TreeType.OAK to TreeSpeciesData(
            scale          = 4f,
            billboard      = false,
            noise          = 0.05f,
            taper          = 0.8f,
            trunkDepth     = 1,
            branchLength   = 0.9f,
            trunkLength    = 1.0f,
            leafScale      = 1.5f,
            ballSize       = 1.5f,
            ballOffset     = 0.1f,
            lowLODBranches = 2,
            numBranchLevels = 3,
        ),
        TreeType.PALM_1 to TreeSpeciesData(
            scale          = 5f,
            billboard      = false,
            noise          = 0.05f,
            taper          = 0.8f,
            trunkDepth     = 1,
            branchLength   = 1.5f,
            trunkLength    = 2.5f,
            leafScale      = 3.0f,
            ballSize       = 2.0f,
            ballOffset     = 0.05f,
            lowLODBranches = 0,
            numBranchLevels = 1,
        ),
        TreeType.FERN to TreeSpeciesData(
            scale          = 1f,
            billboard      = true,
            noise          = 0.1f,
            taper          = 0.5f,
            trunkDepth     = 0,
            branchLength   = 0.5f,
            trunkLength    = 0.5f,
            leafScale      = 1.0f,
            ballSize       = 0.5f,
            ballOffset     = 0.0f,
            lowLODBranches = 0,
            numBranchLevels = 1,
        ),
        TreeType.EELGRASS to TreeSpeciesData(
            scale          = 1f,
            billboard      = true,
            noise          = 0.1f,
            taper          = 0.3f,
            trunkDepth     = 0,
            branchLength   = 0.6f,
            trunkLength    = 1.0f,
            leafScale      = 1.0f,
            ballSize       = 0.5f,
            ballOffset     = 0.0f,
            lowLODBranches = 0,
            numBranchLevels = 1,
        ),
    )

    // ------------------------------------------------------------------
    // Convenience helpers
    // ------------------------------------------------------------------

    /** Look up species data, or null if the type is not yet in the table. */
    fun getSpeciesData(type: TreeType): TreeSpeciesData? = sSpeciesTable[type]

    /** Return the number of registered species. */
    fun getNumTrees(): Int = TreeType.NUM_TREES
}
