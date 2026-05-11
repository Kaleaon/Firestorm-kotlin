package com.firestorm.newview

import kotlin.math.*

private const val GRASS_MAX_BLADES: Int = 32
private const val GRASS_BLADE_BASE: Float = 0.25f
private const val GRASS_BLADE_HEIGHT: Float = 0.5f
private const val GRASS_DISTRIBUTION_SD: Float = 0.15f

data class GrassSpeciesData(
    val textureId: String,
    val bladeSizeX: Float,
    val bladeSizeY: Float,
    val name: String,
)

open class VOGrass(
    id: String,
    pCode: UByte,
    region: ViewerRegion?,
) : AlphaObject(id, pCode, region) {

    var species: UByte = 0u
    var bladeSizeX: Float = 0f
    var bladeSizeY: Float = 0f
    var patch: SurfacePatch? = null
    var lastPatchUpdateTime: ULong = 0uL
    var grassBend: FloatArray = floatArrayOf(0f, 0f, 0f)
    var grassVel: FloatArray = floatArrayOf(0f, 0f, 0f)
    var wind: FloatArray = floatArrayOf(0f, 0f, 0f)
    var bladeWindAngle: Float = 35f
    var bwaOverlap: Float = 2f

    private var lastHeight: Float = 0f
    private var numBlades: Int = GRASS_MAX_BLADES

    init {
        canSelect = true
        TODO("APR: setNumTEs(1); setTEColor(0, Color4(1,1,1,1))")
    }

    open fun isActive(): Boolean = true

    open fun idleUpdate(time: Double) {
        // Rebuilds geometry when the terrain patch under this grass clump changes.
        TODO("GPU: check mDead, hasRenderType(GRASS); if patch update time changed markRebuild(VOLUME)")
    }

    open fun createDrawable(pipeline: Any?): Any? {
        TODO("GPU: pipeline.allocDrawable(this); mDrawable.setRenderType(RENDER_TYPE_GRASS); return mDrawable")
    }

    open fun updateGeometry(drawable: Any?): Boolean {
        TODO("GPU: dirtySpatialGroup(); if numBlades==0 setSize(0,0) else plantBlades(); return true")
    }

    open fun getGeometry(
        idx: Int,
        verticesp: Any,
        normalsp: Any,
        texcoordsp: Any,
        colorsp: Any,
        emissivep: Any,
        indicesp: Any,
    ) {
        // Generates numBlades grass quads using Gaussian-distributed positions and
        // pre-baked wind-rotation tables (expX/expY/rotX/rotY/dzX/dzY/wMod).
        // Each blade = 8 vertices, 12 indices (4 back-to-back triangles for double-sided rendering).
        TODO("GPU: fill vertex/normal/texcoord/color/index streams from speciesTable and blade tables")
    }

    fun updateFaceSize(idx: Int) {}

    open fun updateTextures() {
        TODO("GPU: getTEImage(0).addTextureStats(mPixelArea)")
    }

    open fun updateLOD(): Boolean {
        // LOD = number of blades, scaled by (scale.x*scale.y / distanceToCamera).
        // Doubles or halves numBlades when distance changes enough; rebuilds geometry.
        TODO("GPU: compute tan_angle, num_blades; markRebuild(ALL) if numBlades changes")
    }

    fun setPixelAreaAndAngle() {
        TODO("APR: compute range from agent camera; appAngle = atan2(maxScale, range)*RAD_TO_DEG; pixelArea = pixels_per_meter^2 * 25")
    }

    fun plantBlades() {
        // Sets up the face's size, position, and extents but does not push any
        // geometry; actual vertex data is filled by getGeometry().
        TODO("GPU: face.setSize(numBlades*8, numBlades*12); face.setState(GLOBAL); mDrawable.movePartition()")
    }

    fun updateDrawable(forceDamped: Boolean) {
        TODO("GPU: if drawable.notNull updateXform(true); markRebuild(ALL); clearChanged(SHIFTED)")
    }

    open fun lineSegmentIntersect(
        start: FloatArray,
        end: FloatArray,
        face: Int = -1,
        pickTransparent: Boolean = false,
        pickRigged: Boolean = false,
        pickUnselectable: Boolean = true,
        faceHit: IntArray? = null,
        intersection: FloatArray? = null,
        texCoord: FloatArray? = null,
        normal: FloatArray? = null,
        tangent: FloatArray? = null,
    ): Boolean {
        // Ray-triangle test for each blade using the same blade geometry as getGeometry().
        // Returns true at the closest transparent (or opaque if pickTransparent) hit.
        TODO("GPU: per-blade LLTriangleRayIntersect on 4 triangles; barycentric tex-coord lookup for alpha test")
    }

    open fun processUpdateMessage(
        blockNum: UInt,
        updateType: Int,
        dp: Any?,
    ): UInt {
        TODO("APR: LLViewerObject.processUpdateMessage; updateSpecies(); zero out any accidental velocity")
    }

    open fun exportFile(position: FloatArray) {
        TODO("APR: write grass state to file")
    }

    open fun getPartitionType(): Int = PARTITION_GRASS

    private fun updateSpecies() {
        TODO("APR: species = getAttachmentState(); resolve unknown species; setTEImage from speciesTable")
    }

    var canSelect: Boolean = false
    var appAngle: Float = 0f
    var pixelArea: Float = 0f

    private companion object {
        const val PARTITION_GRASS = 0
    }

    companion object {
        var maxGrassSpecies: Int = 0
        val speciesTable: MutableMap<UInt, GrassSpeciesData> = mutableMapOf()

        // Per-blade layout tables initialised by initClass() from a Gaussian distribution.
        val expX: FloatArray = FloatArray(GRASS_MAX_BLADES)
        val expY: FloatArray = FloatArray(GRASS_MAX_BLADES)
        val rotX: FloatArray = FloatArray(GRASS_MAX_BLADES)
        val rotY: FloatArray = FloatArray(GRASS_MAX_BLADES)
        val dzX: FloatArray = FloatArray(GRASS_MAX_BLADES)
        val dzY: FloatArray = FloatArray(GRASS_MAX_BLADES)
        val wMod: FloatArray = FloatArray(GRASS_MAX_BLADES)

        fun initClass() {
            TODO("APR: parse grass.xml; fill speciesTable and per-blade layout tables")
            // For each blade i:
            //   u = sqrt(-2 * ln(rand)); v = 2*PI*rand
            //   x = u*sin(v)*SD; y = u*cos(v)*SD; rot = rand(PI)
            //   expX[i]=x; expY[i]=y; rotX[i]=sin(rot); rotY[i]=cos(rot)
            //   dzX[i]=rand(BASE*0.25); dzY[i]=rand(BASE*0.25); wMod[i]=0.5+rand
        }

        fun cleanupClass() {
            speciesTable.clear()
        }
    }
}

// Spatial partition for grass — groups alpha blades into a single VBO per group.
class GrassPartition(region: ViewerRegion?) {
    var drawableType: Int = 0   // RENDER_TYPE_GRASS
    var partitionType: Int = 0  // PARTITION_GRASS
    var lodPeriod: Int = 16
    var depthMask: Boolean = true
    var slopRatio: Float = 0.1f
    var renderPass: Int = 0     // PASS_GRASS

    private val faceList: MutableList<Any> = mutableListOf()

    fun addGeometryCount(group: Any?, vertexCount: UInt, indexCount: UInt) {
        TODO("GPU: iterate drawables in group; accumulate face geometry counts; skip faces that exceed 65536 vertex budget")
    }

    fun getGeometry(group: Any?) {
        TODO("GPU: sort faceList back-to-front; fill shared VBO; build LLDrawInfo batches by texture")
    }
}
