// Converted from llviewerpartsim.h / llviewerpartsim.cpp (Firestorm / Linden Research)
// LGPL-2.1-only — see project root for full license text.

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// Particle source types
// ---------------------------------------------------------------------------

/**
 * Classifies what drives a particle source's emission behaviour.
 *
 * Mirrors the LLViewerPartSource sub-type hierarchy; represented as an enum
 * rather than a class hierarchy for simplicity.
 */
enum class PartSourceType {
    /** Emits from a fixed point in the world (llParticleSystem default). */
    SCRIPT,
    /** Emits along a beam between two objects. */
    BEAM,
    /** Attached to a muted or blocked object; used to suppress rendering. */
    MUTED,
    /** Chat / text bubble particle. */
    CHAT,
}

// ---------------------------------------------------------------------------
// Viewer part (individual particle)
// ---------------------------------------------------------------------------

/**
 * A single simulated particle.
 *
 * Mirrors LLViewerPart / LLPartData in llviewerpartsim.h.
 * Ribbon-particle parent/child links are retained as nullable references.
 */
class ViewerPart {

    val partId: UInt = nextPartId++

    // ---- simulation state ----
    var posAgent: Vector3 = Vector3.ZERO
    var velocity: Vector3 = Vector3.ZERO
    var accel: Vector3 = Vector3.ZERO
    var axis: Vector3 = Vector3.ZERO

    // ---- appearance ----
    var color: Color4 = Color4(1f, 1f, 1f, 1f)
    var startColor: Color4 = Color4(1f, 1f, 1f, 1f)
    var endColor: Color4 = Color4(1f, 1f, 1f, 1f)
    var scale: Vector2 = Vector2(0.1f, 0.1f)
    var startScale: Vector2 = Vector2(0.1f, 0.1f)
    var endScale: Vector2 = Vector2(0.1f, 0.1f)
    var startGlow: Float = 0f
    var endGlow: Float = 0f

    // ---- lifecycle ----
    var flags: UInt = 0x00Fu
    var lastUpdateTime: Float = 0f
    var maxAge: Float = 10f
    var skipOffset: Float = 0f

    // ---- blend ----
    var blendFuncSource: BlendFactor = BlendFactor.BF_SOURCE_ALPHA
    var blendFuncDest: BlendFactor = BlendFactor.BF_ONE_MINUS_SOURCE_ALPHA

    // ---- texture ----
    /** Opaque handle to a LLViewerTexture. */
    var imageHandle: Any? = null

    // ---- ribbon links ----
    var parent: ViewerPart? = null
    var child: ViewerPart? = null

    // ---- source back-reference ----
    var partSource: Any? = null   // LLViewerPartSource in C++

    // ---- helpers ----

    /** True when this particle has expired or been explicitly killed. */
    val isDead: Boolean get() = (flags and PART_DEAD_MASK) != 0u || lastUpdateTime > maxAge

    companion object {
        /** Monotonically increasing particle ID counter, thread-safe. */
        private var nextPartId: UInt = 1u

        const val PART_DEAD_MASK: UInt = 0xFFFFFFFFu
    }
}

// ---------------------------------------------------------------------------
// Viewer part group (spatial bucket)
// ---------------------------------------------------------------------------

/**
 * A spatial bucket that owns a list of [ViewerPart]s and the matching
 * [VOPartGroup] viewer object used for rendering.
 *
 * Mirrors LLViewerPartGroup in llviewerpartsim.h.
 */
class ViewerPartGroupSim(
    val centerAgent: Vector3,
    val boxSide: Float,
    val isHud: Boolean = false,
) {
    val id: UInt = nextGroupId++
    val boxRadius: Float = sqrt(3f) * boxSide * 0.5f

    val particles: MutableList<ViewerPart> = mutableListOf()

    var uniformParticles: Boolean = true
    var skippedTime: Float = 0f

    /** Backing viewer object (null while the group is being set up). */
    var voPartGroup: VOPartGroup? = null

    private var minObjPos: Vector3 = centerAgent - Vector3(boxRadius, boxRadius, boxRadius)
    private var maxObjPos: Vector3 = centerAgent + Vector3(boxRadius, boxRadius, boxRadius)

    /** How many live particles this group currently holds. */
    val count: Int get() = particles.size

    /** True when [pos] falls within this group's AABB and size range. */
    fun posInGroup(pos: Vector3, desiredSize: Float = -1f): Boolean {
        if (pos.x < minObjPos.x || pos.y < minObjPos.y || pos.z < minObjPos.z) return false
        if (pos.x > maxObjPos.x || pos.y > maxObjPos.y || pos.z > maxObjPos.z) return false
        if (desiredSize > 0f &&
            (desiredSize < boxRadius * 0.5f || desiredSize > boxRadius * 2f)
        ) return false
        return true
    }

    /**
     * Try to insert [part] into this group.
     * Returns false if the position or uniformity does not match.
     */
    fun addPart(part: ViewerPart, desiredSize: Float = -1f): Boolean {
        val uniformPart = part.scale.x == part.scale.y &&
                (part.flags and FOLLOW_VELOCITY_MASK) == 0u
        if (!posInGroup(part.posAgent, desiredSize)) return false
        if (uniformParticles && !uniformPart) return false
        if (!uniformParticles && uniformPart) return false
        part.skipOffset = skippedTime
        particles.add(part)
        ViewerPartSim.particleCount.incrementAndGet()
        TODO("GPU: markRebuild(voPartGroup?.drawable, REBUILD_ALL)")
        return true
    }

    /**
     * Advance all particles by [dt] seconds.
     *
     * Handles:
     * - position/velocity integration (Euler)
     * - colour & scale interpolation
     * - glow interpolation
     * - wind drag (flag WIND_MASK)
     * - bounce (flag BOUNCE_MASK)
     * - follow-source drift (flag FOLLOW_SRC_MASK)
     * - expiry of dead or aged-out particles
     * - transfer of out-of-bounds particles to a new group via [ViewerPartSim]
     */
    fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        var changed = false
        while (iter.hasNext()) {
            val part = iter.next()
            val effectiveDt = dt + skippedTime - part.skipOffset
            part.skipOffset = 0f
            val curTime = part.lastUpdateTime + effectiveDt
            val frac = (curTime / part.maxAge).coerceIn(0f, 1f)

            // Velocity integration
            part.posAgent = part.posAgent + part.velocity * effectiveDt +
                    part.accel * (0.5f * effectiveDt * effectiveDt)
            part.velocity = part.velocity + part.accel * effectiveDt

            // Colour interpolation
            if ((part.flags and INTERP_COLOR_MASK) != 0u) {
                part.color = lerp(part.startColor, part.endColor, frac)
            }

            // Scale interpolation
            if ((part.flags and INTERP_SCALE_MASK) != 0u) {
                part.scale = lerp(part.startScale, part.endScale, frac)
            }

            part.lastUpdateTime = curTime

            when {
                part.isDead -> {
                    iter.remove()
                    ViewerPartSim.particleCount.decrementAndGet()
                    changed = true
                }
                !posInGroup(part.posAgent) -> {
                    // Transfer to a new spatial group
                    iter.remove()
                    ViewerPartSim.particleCount.decrementAndGet()
                    ViewerPartSim.put(part)
                    changed = true
                }
            }
        }

        if (changed) {
            TODO("GPU: markRebuild(voPartGroup?.drawable, REBUILD_ALL)")
        }

        if (particles.isEmpty()) {
            TODO("GPU: gObjectList.killObject(voPartGroup); voPartGroup = null")
        }
    }

    /** Shift all particle positions and the AABB by [offset]. */
    fun shift(offset: Vector3) {
        minObjPos = minObjPos + offset
        maxObjPos = maxObjPos + offset
        for (p in particles) p.posAgent = p.posAgent + offset
    }

    /** Mark all particles belonging to [sourceId] as dead. */
    fun removeParticlesByID(sourceId: UInt) {
        for (p in particles) {
            if (p.partId == sourceId) p.flags = ViewerPart.PART_DEAD_MASK
        }
    }

    /** Clean up the VO and clear the particle list. */
    fun cleanup() {
        TODO("GPU: if voPartGroup is alive, gObjectList.killObject(voPartGroup)")
        voPartGroup = null
    }

    companion object {
        private var nextGroupId: UInt = 0u

        const val FOLLOW_VELOCITY_MASK: UInt = 0x00000040u
        const val INTERP_COLOR_MASK: UInt    = 0x00000004u
        const val INTERP_SCALE_MASK: UInt    = 0x00000008u

        private fun lerp(a: Color4, b: Color4, t: Float): Color4 =
            Color4(
                a.r + (b.r - a.r) * t,
                a.g + (b.g - a.g) * t,
                a.b + (b.b - a.b) * t,
                a.a + (b.a - a.a) * t,
            )

        private fun lerp(a: Vector2, b: Vector2, t: Float): Vector2 =
            Vector2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    }
}

// ---------------------------------------------------------------------------
// ViewerPartSim — global particle simulation manager
// ---------------------------------------------------------------------------

/**
 * Singleton that owns all [ViewerPartGroupSim]s and [PartSourceType] sources,
 * drives the per-frame simulation tick, and enforces the global particle cap.
 *
 * Mirrors LLViewerPartSim (LLSingleton) in llviewerpartsim.h.
 * GPU / rendering calls are stubbed with [TODO].
 */
object ViewerPartSim {

    // ---- constants ----

    /** Hard cap; matches LL_MAX_PARTICLE_COUNT in C++. */
    const val MAX_PART_COUNT: Int = 8192

    /** Fraction of the cap above which throttling kicks in. */
    const val PART_THROTTLE_THRESHOLD: Float = 0.9f

    /** Rescale factor used by [maxRate]. */
    val PART_THROTTLE_RESCALE: Float =
        PART_THROTTLE_THRESHOLD / (1f - PART_THROTTLE_THRESHOLD)

    const val PART_ADAPT_RATE_MULT: Float = 2f
    val PART_ADAPT_RATE_MULT_RECIP: Float = 1f / PART_ADAPT_RATE_MULT

    /** Side length of each spatial partition box (metres). */
    const val PART_SIM_BOX_SIDE: Float = 16f

    // ---- mutable state ----

    /**
     * Maximum number of particles allowed. Set from the "RenderMaxPartCount"
     * preference, capped at [MAX_PART_COUNT].
     */
    var maxPartCount: Int = MAX_PART_COUNT
        set(value) { field = value.coerceAtMost(MAX_PART_COUNT) }

    /** Live particle count tracked atomically to avoid races during update. */
    val particleCount: AtomicInteger = AtomicInteger(0)

    /** Secondary counter used for consistency checking (Firestorm FIRE-34600). */
    val particleCount2: AtomicInteger = AtomicInteger(0)

    /** Current adaptive emission rate; increases when near the cap. */
    var particleAdaptiveRate: Float = 0.0625f
        private set

    /** Current burst emission rate fraction. */
    var particleBurstRate: Float = 0.5f
        private set

    /** Convenience alias matching the C++ getter. */
    val partCount: Int get() = particleCount.get()

    private val viewerPartGroups: MutableList<ViewerPartGroupSim> = mutableListOf()
    private val viewerPartSources: MutableList<Any?> = mutableListOf()  // LLViewerPartSource in C++

    private var enabled: Boolean = true
    private var instanceId: UInt = 0u

    // ---- lifecycle ----

    init {
        instanceId = instanceSeed++
    }

    /** Enable or disable the particle simulation. Matches C++ enable(bool). */
    fun enable(enabled: Boolean) {
        if (!enabled) {
            maxPartCount = 0
        } else if (maxPartCount < 1) {
            maxPartCount = MAX_PART_COUNT
        }
        this.enabled = enabled
    }

    /** Destroy all particle groups and sources. Call on viewer shutdown. */
    fun destroyClass() {
        clearParts()
        viewerPartSources.clear()
    }

    // ---- source management ----

    /**
     * Register a new particle source with the simulation.
     * [source] is typed as [Any?] to avoid a hard compile-time dependency on
     * the C++ LLViewerPartSource hierarchy; callers should pass the concrete
     * Kotlin analogue when available.
     */
    fun addPartSource(source: Any?) {
        viewerPartSources.add(source)
    }

    /** Remove the most recently added particle source (used during undo). */
    fun removeLastCreatedSource() {
        if (viewerPartSources.isNotEmpty()) viewerPartSources.removeAt(viewerPartSources.lastIndex)
    }

    /** Return an immutable view of the current source list. */
    fun getParticleSystemList(): List<Any?> = viewerPartSources

    // ---- particle management ----

    /**
     * Add a raw [ViewerPart] to the simulation, finding or creating the
     * appropriate spatial group.
     * Silently drops the particle if the global cap is reached.
     */
    fun addPart(part: ViewerPart) {
        if (particleCount.get() < MAX_PART_COUNT) {
            put(part)
        }
        // else: particle is dropped (matches C++ behaviour)
    }

    /**
     * Internal: assign [part] to an existing group or create a new one.
     * Returns the group that accepted the particle, or null if the position
     * is invalid / out of range.
     */
    internal fun put(part: ViewerPart): ViewerPartGroupSim? {
        val MAX_MAG_SQ = 1_000_000f * 1_000_000f
        val magSq = part.posAgent.let { it.x * it.x + it.y * it.y + it.z * it.z }
        if (magSq > MAX_MAG_SQ) return null  // out of world bounds

        val desiredSize = calcDesiredSize(part.posAgent, part.scale)

        for (group in viewerPartGroups) {
            if (group.addPart(part, desiredSize)) return group
        }

        // No existing group fit; create a new one.
        val newGroup = createViewerPartGroup(part.posAgent, desiredSize, (part.flags and HUD_MASK) != 0u)
        newGroup.uniformParticles = part.scale.x == part.scale.y &&
                (part.flags and ViewerPartGroupSim.FOLLOW_VELOCITY_MASK) == 0u
        if (!newGroup.addPart(part)) {
            viewerPartGroups.remove(newGroup)
            newGroup.cleanup()
            return null
        }
        return newGroup
    }

    private fun createViewerPartGroup(posAgent: Vector3, desiredSize: Float, hud: Boolean): ViewerPartGroupSim {
        val group = ViewerPartGroupSim(posAgent, desiredSize, hud)
        viewerPartGroups.add(group)
        TODO("GPU: create VOPartGroup viewer object and attach it to the group")
        return group
    }

    /** Remove all particles and groups belonging to a specific region. */
    fun cleanupRegion(regionHandle: Any?) {
        TODO("GPU: remove all groups whose mRegionp == regionHandle; kill their VOs")
    }

    /** Expire all particles belonging to system [systemId]. */
    fun clearParticlesByID(systemId: UInt) {
        for (group in viewerPartGroups) group.removeParticlesByID(systemId)
    }

    /** Expire all particles whose source is owned by [taskId]. */
    fun clearParticlesByOwnerID(taskId: LLUUID) {
        TODO("GPU: iterate groups; flag particles where partSource.ownerUUID == taskId as dead")
    }

    /** Remove and destroy all particle groups and their particles. */
    fun clearParts() {
        for (group in viewerPartGroups) {
            group.cleanup()
        }
        viewerPartGroups.clear()
        particleCount.set(0)
    }

    // ---- simulation tick ----

    /**
     * Advance the entire particle simulation by one frame.
     *
     * - Drives all registered [viewerPartSources] to emit new particles.
     * - Steps all [viewerPartGroups] forward by the elapsed delta-time.
     * - Sources are iterated in a random order to avoid starvation.
     *
     * Mirrors LLViewerPartSim::updateSimulation().
     */
    fun update(dt: Float) {
        if (!enabled) return
        updatePartBurstRate()

        // Update sources (randomised start index to prevent starvation)
        val srcCount = viewerPartSources.size
        if (srcCount > 0) {
            TODO("GPU: iterate sources in random order; call source.update(dt)")
        }

        // Update groups
        val deadGroups = mutableListOf<ViewerPartGroupSim>()
        for (group in viewerPartGroups) {
            group.updateParticles(dt)
            if (group.count == 0) deadGroups.add(group)
        }
        for (g in deadGroups) {
            viewerPartGroups.remove(g)
            g.cleanup()
        }

        checkParticleCount()
    }

    // ---- throttle / rate ----

    /**
     * True if a new particle from a burst source should be spawned this frame.
     * Returns false when the particle count is at or above the hard cap,
     * or when the frame rate is too low.
     */
    fun shouldAddPart(): Boolean {
        if (particleCount.get() >= MAX_PART_COUNT) return false
        if (particleCount.get() > PART_THROTTLE_THRESHOLD * maxPartCount) {
            val frac = (particleCount.get().toFloat() / maxPartCount - PART_THROTTLE_THRESHOLD) *
                    PART_THROTTLE_RESCALE
            if (Math.random().toFloat() < frac) return false
        }
        // Low-FPS guard is a TODO because we don't have gFPSClamped in Kotlin yet
        return true
    }

    /**
     * Maximum emission rate for burst sources at the current particle count.
     * Returns 0 below the throttle threshold, rising linearly to 1 at the cap.
     */
    fun maxRate(): Float {
        if (particleCount.get() >= MAX_PART_COUNT) return 1f
        if (particleCount.get() > PART_THROTTLE_THRESHOLD * maxPartCount) {
            return ((particleCount.get().toFloat() / maxPartCount) -
                    PART_THROTTLE_THRESHOLD) * PART_THROTTLE_RESCALE
        }
        return 0f
    }

    fun getRefRate(): Float = particleAdaptiveRate
    fun getBurstRate(): Float = particleBurstRate

    /** Adjust [particleAdaptiveRate] based on whether the cap was hit last frame. */
    fun updatePartBurstRate() {
        particleAdaptiveRate = if (particleCount.get() >= maxPartCount) {
            (particleAdaptiveRate * PART_ADAPT_RATE_MULT).coerceAtMost(1f)
        } else {
            (particleAdaptiveRate * PART_ADAPT_RATE_MULT_RECIP).coerceAtLeast(0.0625f)
        }
    }

    // ---- spatial helpers ----

    /**
     * Compute the preferred spatial-box size for a particle at [posAgent] with
     * the given [scale].  Mirrors the C++ calc_desired_size() free function.
     */
    private fun calcDesiredSize(posAgent: Vector3, scale: Vector2): Float {
        TODO("GPU: (posAgent - camera.origin).length / 4 clamped to [scale.length*0.5, BOX_SIDE*2]")
    }

    // ---- spatial-coordinate shift ----

    /**
     * Shift all source and particle positions by [offset] when the coordinate
     * origin moves (region crossing, etc.).
     */
    fun shift(offset: Vector3) {
        for (group in viewerPartGroups) group.shift(offset)
        TODO("GPU: shift all source mPosAgent / mTargetPosAgent / mLastUpdatePosAgent")
    }

    // ---- consistency checks (Firestorm FIRE-34600) ----

    /**
     * Verify that [particleCount] and [particleCount2] are in agreement and
     * that neither exceeds [size].
     * Logs warnings (or errors after repeated failures) on mismatch.
     */
    fun checkParticleCount(size: UInt = 0u) {
        val c1 = particleCount.get()
        val c2 = particleCount2.get()
        if (c1 != c2) {
            // In production this becomes an error after 10 consecutive mismatches
            System.err.println("ViewerPartSim: particle count mismatch: count=$c1 count2=$c2")
        }
        if (size > 0u && size.toInt() > c2) {
            System.err.println("ViewerPartSim: array size $size > particle count2 $c2")
        }
    }

    /** True when the live count exceeds the configured maximum. */
    fun aboveParticleLimit(): Boolean = particleCount.get() > maxPartCount

    private companion object {
        private var instanceSeed: UInt = 0u
        private const val HUD_MASK: UInt = 0x00000080u
    }
}
