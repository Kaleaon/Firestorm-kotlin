// Converted from llvopartgroup.h / llvopartgroup.cpp (Firestorm / Linden Research)
// LGPL-2.1-only — see project root for full license text.

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

/**
 * Blend function factors used when assembling draw calls for a particle face.
 * Mirrors LLRender::eBlendFactor.
 */
enum class BlendFactor {
    BF_SOURCE_ALPHA,
    BF_ONE_MINUS_SOURCE_ALPHA,
    BF_ONE,
    BF_ZERO,
    BF_DEST_ALPHA,
    BF_ONE_MINUS_DEST_ALPHA,
    BF_SOURCE_COLOR,
    BF_ONE_MINUS_SOURCE_COLOR,
}

/**
 * A single particle held inside a [ViewerPartGroup].
 *
 * Mirrors the relevant fields of LLViewerPart.
 */
data class Particle(
    val id: UInt,
    var posAgent: Vector3,
    var velocity: Vector3,
    var accel: Vector3,
    var axis: Vector3,
    var color: Color4,
    /** (scaleX, scaleY) in world-space metres. */
    var scale: Vector2 = Vector2(0.5f, 0.5f),
    var age: Float = 0f,
    var maxAge: Float = 10f,
    var startGlow: Float = 0f,
    var endGlow: Float = 0f,
    var flags: UInt = 0u,
    var blendFuncSrc: BlendFactor = BlendFactor.BF_SOURCE_ALPHA,
    var blendFuncDst: BlendFactor = BlendFactor.BF_ONE_MINUS_SOURCE_ALPHA,
    /** UUID of the agent that owns the source object. */
    val ownerUUID: LLUUID = LLUUID.NULL,
    /** UUID of the source script object. */
    val sourceUUID: LLUUID = LLUUID.NULL,
)

/**
 * A spatial bucket that owns a list of [Particle]s and the corresponding
 * [VOPartGroup] viewer object.
 *
 * Mirrors LLViewerPartGroup.
 */
class ViewerPartGroup(
    val centerAgent: Vector3,
    val boxRadius: Float,
    val isHud: Boolean = false,
) {
    val particles: MutableList<Particle> = mutableListOf()
    val id: UInt = nextGroupId++

    var skippedTime: Float = 0f
    var uniformParticles: Boolean = true

    /** Backing viewer object used for rendering this group. */
    var voPartGroup: VOPartGroup? = null

    /** AABB used for spatial containment tests. */
    var minObjPos: Vector3 = centerAgent - Vector3(boxRadius, boxRadius, boxRadius)
    var maxObjPos: Vector3 = centerAgent + Vector3(boxRadius, boxRadius, boxRadius)

    val count: Int get() = particles.size

    /** Returns true if [pos] lies within this group's AABB (and optionally matches [desiredSize]). */
    fun posInGroup(pos: Vector3, desiredSize: Float = -1f): Boolean {
        if (pos.x < minObjPos.x || pos.y < minObjPos.y || pos.z < minObjPos.z) return false
        if (pos.x > maxObjPos.x || pos.y > maxObjPos.y || pos.z > maxObjPos.z) return false
        if (desiredSize > 0f && (desiredSize < boxRadius * 0.5f || desiredSize > boxRadius * 2f)) return false
        return true
    }

    /**
     * Try to add [particle] to this group.
     * Returns false if the particle's position or uniformity does not match.
     */
    fun addPart(particle: Particle, desiredSize: Float = -1f): Boolean {
        val uniformPart = particle.scale.x == particle.scale.y &&
                (particle.flags and PART_FOLLOW_VELOCITY_MASK) == 0u
        if (!posInGroup(particle.posAgent, desiredSize)) return false
        if (uniformParticles && !uniformPart) return false
        if (!uniformParticles && uniformPart) return false
        particle.age += skippedTime  // mSkipOffset equivalent
        particles.add(particle)
        TODO("GPU: markRebuild voPartGroup drawable REBUILD_ALL")
        return true
    }

    /**
     * Step all particles forward by [dt] seconds, expiring or transferring
     * any that have left the group's bounds.
     */
    fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val part = iter.next()
            part.age += dt
            // Euler integration
            part.posAgent = part.posAgent + part.velocity * dt + part.accel * (0.5f * dt * dt)
            part.velocity = part.velocity + part.accel * dt
            if (part.age > part.maxAge || (part.flags and PART_DEAD_MASK) != 0u) {
                iter.remove()
                TODO("GPU: decrement global particle count")
            }
        }
        if (particles.isEmpty()) {
            TODO("GPU: kill voPartGroup viewer object")
        }
    }

    /** Shift all particle positions and the group AABB by [offset]. */
    fun shift(offset: Vector3) {
        minObjPos = minObjPos + offset
        maxObjPos = maxObjPos + offset
        for (p in particles) p.posAgent = p.posAgent + offset
    }

    /** Flag all particles belonging to [sourceId] as dead. */
    fun removeParticlesByID(sourceId: UInt) {
        for (p in particles) {
            if (p.sourceUUID.hashCode().toUInt() == sourceId) {
                p.flags = p.flags or PART_DEAD_MASK
            }
        }
    }

    companion object {
        private var nextGroupId: UInt = 0u

        const val PART_DEAD_MASK: UInt = 0xFFFFFFFFu
        const val PART_FOLLOW_VELOCITY_MASK: UInt = 0x00000040u
    }
}

/**
 * Viewer object that renders a [ViewerPartGroup]'s particles as camera-facing quads.
 *
 * Mirrors LLVOPartGroup / LLAlphaObject in llvopartgroup.h.
 * GPU geometry and draw-pool calls are stubbed with [TODO].
 */
open class VOPartGroup(
    id: LLUUID,
    localId: UInt,
    pCode: UInt,
) : ViewerObject(id, localId, pCode) {

    /** The particle group this object renders. */
    var viewerPartGroup: ViewerPartGroup? = null

    /** Bounding-box half-extent used for spatial culling. */
    var scale: Vector3 = Vector3.ZERO

    /** Whether this object should run an idle update each frame. Always false for particle groups. */
    open fun isActive(): Boolean = false

    /** No-op: particle area/angle are computed during rendering. */
    fun idleUpdate(time: Double) { /* intentionally empty */ }

    /**
     * Returns the side length of the particle group's spatial box, used as the
     * spatial-partition bin radius.
     */
    fun getBinRadius(): Float = viewerPartGroup?.boxRadius ?: 0f

    /**
     * Update the spatial extents (AABB) of this drawable from the current
     * particle group centre and box size.
     */
    fun updateSpatialExtents(newMin: Vector3, newMax: Vector3) {
        TODO("GPU: compute AABB from posAgent ± (scale + boxSide*0.5); setPositionGroup")
    }

    /**
     * Build draw faces for all visible particles in the group.
     * Faces are sorted front-to-back and culled by projected screen area.
     */
    open fun updateGeometry(): Boolean {
        TODO("GPU: iterate viewerPartGroup.particles; set face sizes, textures, and colours")
    }

    /**
     * Build billboard quad vertices for a single [particle] into [verticesOut].
     * For ribbon particles the quad follows the parent-to-child axis;
     * for normal particles the quad is camera-facing.
     */
    fun getGeometry(particle: Particle, verticesOut: MutableList<Vector3>) {
        TODO("GPU: fill 4 billboard vertices from particle position/scale/velocity/flags")
    }

    /**
     * Fill per-face geometry streams (vertices, normals, tex-coords, colours,
     * emissive, indices) for particle at [idx].
     */
    fun getGeometry(
        idx: Int,
        verticesOut: MutableList<Vector3>,
        normalsOut: MutableList<Vector3>,
        texCoordsOut: MutableList<Vector2>,
        colorsOut: MutableList<Color4>,
        emissiveOut: MutableList<Color4>,
        indicesOut: MutableList<UShort>,
    ) {
        TODO("GPU: delegate to getGeometry(particle, verticesOut) then emit colour/normal streams")
    }

    /** Return the scale of particle [idx] for LOD / pixel-area calculations. */
    fun getPartSize(idx: Int): Float {
        val group = viewerPartGroup ?: return 0f
        return group.particles.getOrNull(idx)?.scale?.x ?: 0f
    }

    /** Fill [src] and [dst] with the blend factors for particle [idx]. */
    fun getBlendFunc(idx: Int, src: Array<BlendFactor>, dst: Array<BlendFactor>) {
        val part = viewerPartGroup?.particles?.getOrNull(idx) ?: return
        src[0] = part.blendFuncSrc
        dst[0] = part.blendFuncDst
    }

    /** Return the owner UUID for particle [idx]. */
    fun getPartOwner(idx: Int): LLUUID =
        viewerPartGroup?.particles?.getOrNull(idx)?.ownerUUID ?: LLUUID.NULL

    /** Return the source-object UUID for particle [idx]. */
    fun getPartSource(idx: Int): LLUUID =
        viewerPartGroup?.particles?.getOrNull(idx)?.sourceUUID ?: LLUUID.NULL

    /** Allocate a draw-pool face for this object in the alpha-particle partition. */
    open fun createDrawable(): Any? {
        TODO("GPU: pipeline.allocDrawable; setRenderType(RENDER_TYPE_PARTICLES)")
    }

    /** Ray vs. all particle quads intersection test. Returns true on hit. */
    fun lineSegmentIntersect(start: Vector3, end: Vector3): Boolean {
        TODO("GPU: LLTriangleRayIntersect per quad built by getGeometry(particle)")
    }

    /** Returns the camera position used to orient billboards. Overridden for HUD. */
    protected open fun getCameraPosition(): Vector3 =
        TODO("GPU: gAgentCamera.getCameraPositionAgent()")

    companion object {
        /** One-time GL resource allocation. */
        fun initClass() { /* no-op in Kotlin */ }

        /** Recreate any GL resources after context loss. */
        fun restoreGL(): Unit = TODO("GPU: restore vertex buffers")

        /** Release GL resources before context destruction. */
        fun destroyGL(): Unit = TODO("GPU: free vertex buffers")
    }
}

/**
 * Variant of [VOPartGroup] that renders into the HUD overlay partition.
 *
 * Mirrors LLVOHUDPartGroup.
 */
class VOHUDPartGroup(
    id: LLUUID,
    localId: UInt,
    pCode: UInt,
) : VOPartGroup(id, localId, pCode) {

    override fun createDrawable(): Any? {
        TODO("GPU: pipeline.allocDrawable; setRenderType(RENDER_TYPE_HUD_PARTICLES)")
    }

    /** HUD camera is always at (-1, 0, 0) in agent space. */
    override fun getCameraPosition(): Vector3 = Vector3(-1f, 0f, 0f)
}
