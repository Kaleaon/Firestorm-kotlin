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
    fun getBoxSide(): Float = 0f
}

// Particle flag bits (mirrors LLPartData).
private const val LL_PART_RIBBON_MASK: UInt           = 0x0040u
private const val LL_PART_EMISSIVE_MASK: UInt         = 0x0010u
private const val LL_PART_FOLLOW_VELOCITY_MASK: UInt  = 0x0004u

private const val MAX_PARTICLE_AREA_SCALE = 0.02f

open class VOPartGroup {

    companion object {
        fun initClass()  {}
        fun restoreGL() {
            // no-op
        }
        fun destroyGL() {
            // no-op
        }

        const val VERTEX_DATA_MASK = 0   // GPU: MAP_VERTEX|MAP_NORMAL|MAP_TEXCOORD0|MAP_COLOR|MAP_EMISSIVE|MAP_TEXTURE_INDEX
    }

    var viewerPartGroup: ViewerPartGroup? = null

    fun isActive(): Boolean = false

    fun getBinRadius(): Float = viewerPartGroup?.getBoxSide() ?: 0f

    fun updateSpatialExtents() {
        System.err.println("VOPartGroup: updateSpatialExtents not yet implemented")
    }

    open fun getPartitionType(): UInt = 0u

    fun idleUpdate() {}

    fun setPixelAreaAndAngle() {
        System.err.println("VOPartGroup: setPixelAreaAndAngle not yet implemented")
    }

    fun updateTextures() {}

    open fun createDrawable() {
        System.err.println("VOPartGroup: createDrawable not yet implemented")
    }

    fun updateGeometry(): Boolean {
        val group = viewerPartGroup ?: return true
        val numParts = group.getCount()
        if (numParts == 0) {
            System.err.println("VOPartGroup: updateGeometry (clear faces) not yet implemented")
            return true
        }
        System.err.println("VOPartGroup: updateGeometry (fill geometry) not yet implemented")
        return false
    }

    fun getGeometryForPart(part: ViewerPart): Array<FloatArray> {
        if (part.flags and LL_PART_RIBBON_MASK != 0u) {
            val pos   = floatArrayOf(part.posAgent.x, part.posAgent.y, part.posAgent.z)
            val axis  = floatArrayOf(part.axis.x, part.axis.y, part.axis.z)
            val scale = part.scale.x * 0.5f
            // Ribbon: four verts spanning current + parent positions.
            System.err.println("VOPartGroup: getGeometryForPart (ribbon) not yet implemented")
            return emptyArray()
        } else {
            // Billboard aligned to camera.
            System.err.println("VOPartGroup: getGeometryForPart (billboard) not yet implemented")
            return emptyArray()
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
        System.err.println("VOPartGroup: getGeometry not yet implemented")
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
            System.err.println("VOPartGroup: lineSegmentIntersect not yet implemented")
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
        System.err.println("VOPartGroup: getPartSource not yet implemented")
        return ""
    }

    open fun getCameraPosition(): Vector3 {
        System.err.println("VOPartGroup: getCameraPosition not yet implemented")
        return Vector3(0f, 0f, 0f)
    }
}

class VOHUDPartGroup : VOPartGroup() {

    override fun getCameraPosition(): Vector3 = Vector3(-1f, 0f, 0f)

    override fun createDrawable() {
        System.err.println("VOHUDPartGroup: createDrawable not yet implemented")
    }

    override fun getPartitionType(): UInt = 0u
}
