package com.firestorm.newview

import kotlin.math.*

// Minimal view-particle stand-ins — the real types live in the particle-sim module.
class ViewerPart {
    var posAgent: Vector3   = Vector3(0f, 0f, 0f)
    var velocity: Vector3   = Vector3(0f, 0f, 0f)
    var axis: Vector3       = Vector3(0f, 0f, 1f)
    var scale: Vector2      = Vector2(0f, 0f)
    var color: ByteArray    = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
    var glow: ByteArray     = byteArrayOf(0, 0, 0, 0)
    var startColor: ByteArray = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
    var startGlow: Float    = 0f
    var flags: UInt         = 0u
    var blendFuncSource: Int = 0
    var blendFuncDest: Int   = 0
    var parent: ViewerPart? = null
    var partSourcep: ViewerPartSource? = null
    var mImagep: Any?       = null
}

class ViewerPartSource {
    var ownerUuid: String      = ""
    var sourceObjectp: Any?    = null
    var posAgent: Vector3      = Vector3(0f, 0f, 0f)
}

class ViewerPartGroup {
    val particles: MutableList<ViewerPart> = mutableListOf()
    fun getCount(): Int = particles.size
    fun getBoxSide(): Float = TODO("APR: use JVM equivalent")
}

// Particle flag bits (mirrors LLPartData).
private const val LL_PART_RIBBON_MASK: UInt           = 0x0040u
private const val LL_PART_EMISSIVE_MASK: UInt         = 0x0010u
private const val LL_PART_FOLLOW_VELOCITY_MASK: UInt  = 0x0004u

private const val MAX_PARTICLE_AREA_SCALE = 0.02f

open class VOPartGroup {

    companion object {
        fun initClass()  {}
        fun restoreGL() { TODO("GPU: restore GL state for particles") }
        fun destroyGL() { TODO("GPU: destroy GL resources for particles") }

        const val VERTEX_DATA_MASK = 0   // GPU: MAP_VERTEX|MAP_NORMAL|MAP_TEXCOORD0|MAP_COLOR|MAP_EMISSIVE|MAP_TEXTURE_INDEX
    }

    var viewerPartGroup: ViewerPartGroup? = null

    fun isActive(): Boolean = false

    fun getBinRadius(): Float = viewerPartGroup?.getBoxSide() ?: 0f

    fun updateSpatialExtents() {
        TODO("GPU: compute AABB from posAgent +/- scale + boxSide*0.5")
    }

    fun getPartitionType(): UInt = TODO("GPU: return PARTITION_PARTICLE")

    fun idleUpdate() {}

    fun setPixelAreaAndAngle() {
        TODO("GPU: compute appAngle from distance and midScale")
    }

    fun updateTextures() {}

    fun createDrawable() {
        TODO("GPU: allocDrawable, setLit(false), setRenderType(PARTICLES)")
    }

    fun updateGeometry(): Boolean {
        val group = viewerPartGroup ?: return true
        val numParts = group.getCount()
        if (numParts == 0) {
            TODO("GPU: clear faces, sCompiles++")
            return true
        }
        TODO("GPU: iterate particles, compute area, set face data, movePartition, sCompiles++")
    }

    fun getGeometryForPart(part: ViewerPart): Array<FloatArray> {
        if (part.flags and LL_PART_RIBBON_MASK != 0u) {
            val pos   = floatArrayOf(part.posAgent.x, part.posAgent.y, part.posAgent.z)
            val axis  = floatArrayOf(part.axis.x, part.axis.y, part.axis.z)
            val scale = part.scale.x * 0.5f
            // Ribbon: four verts spanning current + parent positions.
            TODO("GPU: build ribbon quad from part and parent positions")
        } else {
            // Billboard aligned to camera.
            TODO("GPU: build camera-aligned billboard quad for part")
        }
    }

    fun getGeometry(
        idx: Int,
        vertices: Any, normals: Any, texcoords: Any,
        colors: Any, emissive: Any, indices: Any
    ) {
        val group = viewerPartGroup ?: return
        if (idx >= group.particles.size) return
        val part = group.particles[idx]
        TODO("GPU: fill strider buffers with part geometry, colors, glow, normals")
    }

    fun lineSegmentIntersect(
        start: FloatArray, end: FloatArray,
        face: Int, pickTransparent: Boolean, pickRigged: Boolean, pickUnselectable: Boolean,
        faceHit: IntArray?, intersection: FloatArray?,
        texCoord: FloatArray?, normal: FloatArray?, tangent: FloatArray?
    ): Boolean {
        val group = viewerPartGroup ?: return false
        var closestT = 2f
        var ret = false
        for ((idx, part) in group.particles.withIndex()) {
            val verts = getGeometryForPart(part)
            TODO("GPU: triangle ray-intersect test against particle quad")
        }
        return ret
    }

    fun getPartSize(idx: Int): Float =
        viewerPartGroup?.particles?.getOrNull(idx)?.scale?.x ?: 0f

    fun getBlendFunc(idx: Int): Pair<Int, Int> {
        val part = viewerPartGroup?.particles?.getOrNull(idx) ?: return Pair(0, 0)
        return Pair(part.blendFuncSource, part.blendFuncDest)
    }

    fun getPartOwner(idx: Int): String =
        viewerPartGroup?.particles?.getOrNull(idx)?.partSourcep?.ownerUuid ?: ""

    fun getPartSource(idx: Int): String {
        TODO("APR: return source object ID")
    }

    open fun getCameraPosition(): Vector3 =
        TODO("APR: return agent camera position in agent space")
}

class VOHUDPartGroup : VOPartGroup() {

    override fun getCameraPosition(): Vector3 = Vector3(-1f, 0f, 0f)

    fun createDrawable() {
        TODO("GPU: allocDrawable, setLit(false), setRenderType(HUD_PARTICLES)")
    }

    fun getPartitionType(): UInt = TODO("GPU: return PARTITION_HUD_PARTICLE")
}
