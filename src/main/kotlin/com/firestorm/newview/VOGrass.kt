// Converted from llvograss.h / llvograss.cpp (Firestorm / Linden Research)
// LGPL-2.1-only — see project root for full license text.

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

/** Species constants for grass rendering. */
enum class GrassSpecies(val id: UInt) {
    GRASS_MEADOW_GRASS(0u),
    GRASS_ROUGH(1u),
    GRASS_GOLDEN(2u),
    GRASS_DOGBANE(3u),
    GRASS_JUNGLE(4u),
    GRASS_SEAGRASS(5u),
    GRASS_KELP(6u),
}

/** Per-species static data loaded from grass.xml. */
data class GrassSpeciesData(
    val textureId: LLUUID,
    val bladeSizeX: Float,
    val bladeSizeY: Float,
    val name: String,
)

/**
 * Viewer object representing a clump of grass.
 *
 * Mirrors [LLVOGrass] from llvograss.h.
 * All GPU/geometry calls are stubbed with [TODO].
 */
open class VOGrass(
    id: LLUUID,
    localId: UInt,
    pCode: UInt,
) : ViewerObject(id, localId, pCode) {

    // ---- per-instance state ----

    var species: UByte = 0u
    var bladeSizeX: Float = 0f
    var bladeSizeY: Float = 0f

    /** Land-surface patch where this grass clump is centred. */
    var patch: Any? = null           // typed as LLSurfacePatch in C++

    var lastPatchUpdateTime: Long = 0L

    // Wind-driven animation accumulators
    var grassBend: Vector3 = Vector3.ZERO
    var grassVel: Vector3 = Vector3.ZERO
    var wind: Vector3 = Vector3.ZERO
    var bladeWindAngle: Float = 35f
    var bwaOverlap: Float = 2f

    private var lastHeight: Float = 0f
    private var numBlades: Int = MAX_BLADES

    // ---- public API ----

    /** Returns true; grass always runs idle updates. */
    fun isActive(): Boolean = true

    /** Per-frame idle update: checks patch age and requests geometry rebuild if stale. */
    fun idleUpdate(time: Double) {
        TODO("GPU: markRebuild when patch timestamp changes")
    }

    /** Update apparent angle and pixel area from camera distance. */
    fun setPixelAreaAndAngle() {
        TODO("GPU: compute mAppAngle / mPixelArea from camera distance")
    }

    /** Resolve species from attachment state and load the correct texture. */
    fun updateSpecies() {
        val entry = companion.speciesTable[species.toUInt()]
            ?: companion.speciesTable.values.firstOrNull()
            ?: return
        bladeSizeX = entry.bladeSizeX
        bladeSizeY = entry.bladeSizeY
        TODO("GPU: setTEImage from entry.textureId")
    }

    /** Allocate the draw face and populate blade geometry. */
    fun updateGeometry(): Boolean {
        if (numBlades == 0) {
            TODO("GPU: set face size to 0,0 to suppress rendering")
        } else {
            plantBlades()
        }
        return true
    }

    /** Update discrete LOD blade count based on camera-distance tangent. */
    fun updateLOD(): Boolean {
        TODO("GPU: adjust numBlades via doubling/halving against tangent ratio")
    }

    /**
     * Write per-blade quads into the vertex buffer.
     * Each blade is 8 vertices / 12 indices using a gaussian positional spread.
     */
    fun plantBlades() {
        TODO("GPU: fill LLVertexBuffer with grass blade quads from exp_x/exp_y tables")
    }

    /** Ray-intersection test against all grass blade quads. */
    fun lineSegmentIntersect(start: Vector3, end: Vector3): Boolean {
        TODO("GPU: triangle ray-intersect per blade using blade geometry")
    }

    // ---- companion (static) ----

    companion object {
        const val MAX_BLADES = 32
        const val BLADE_BASE = 0.25f
        const val BLADE_HEIGHT = 0.5f
        const val DISTRIBUTION_SD = 0.15f

        /** Loaded from grass.xml at startup. Key = species id. */
        val speciesTable: MutableMap<UInt, GrassSpeciesData> = mutableMapOf()
        var maxGrassSpecies: Int = 0

        // Per-blade randomised offset / rotation / wind-mod arrays (size MAX_BLADES)
        internal val expX = FloatArray(MAX_BLADES)
        internal val expY = FloatArray(MAX_BLADES)
        internal val rotX = FloatArray(MAX_BLADES)
        internal val rotY = FloatArray(MAX_BLADES)
        internal val dzX  = FloatArray(MAX_BLADES)
        internal val dzY  = FloatArray(MAX_BLADES)
        internal val wMod = FloatArray(MAX_BLADES)   // wind-movement factor per blade

        /**
         * One-time class initialisation: parse grass.xml and fill [speciesTable].
         * Also pre-computes the gaussian blade distribution tables.
         */
        fun initClass() {
            TODO("parse grass.xml into speciesTable; fill expX/expY/rotX/rotY/dzX/dzY/wMod")
        }

        /** Release all species data. Call on viewer shutdown. */
        fun cleanupClass() {
            speciesTable.clear()
        }
    }
}
