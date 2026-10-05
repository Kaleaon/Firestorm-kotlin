package com.firestorm.newview

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// ViewerPart – a single live particle
// ---------------------------------------------------------------------------

class ViewerPart {

    companion object {
        val sNextPartID = AtomicInteger(1)

        const val LL_PART_INTERP_COLOR_MASK: UInt   = PartFlags.LL_PART_INTERP_COLOR_MASK
        const val LL_PART_INTERP_SCALE_MASK: UInt   = PartFlags.LL_PART_INTERP_SCALE_MASK
        const val LL_PART_DEAD_MASK: UInt            = PartFlags.LL_PART_DEAD_MASK
    }

    init { ViewerPartSim.particleCount2.incrementAndGet() }

    var partID: UInt = 0u
    var lastUpdateTime: Float = 0f
    var skipOffset: Float = 0f

    var vpCallback: VPCallback? = null
    var partSourcep: ViewerPartSource? = null

    var parent: ViewerPart? = null
    var child: ViewerPart? = null

    var imagep: Any? = null
    var posAgent: Vector3 = Vector3.ZERO
    var velocity: Vector3 = Vector3.ZERO
    var accel: Vector3 = Vector3.ZERO
    var axis: Vector3 = Vector3.ZERO
    var color: Color4 = Color4()
    var scale: Vector2 = Vector2()
    var startGlow: Float = 0f
    var endGlow: Float = 0f
    var glow: Color4U = Color4U()

    // PartData fields inlined
    var flags: UInt = 0u
    var maxAge: Float = 10f
    var startColor: Color4 = Color4()
    var endColor: Color4 = Color4()
    var startScale: Vector2 = Vector2(1f, 1f)
    var endScale: Vector2 = Vector2(1f, 1f)
    var blendFuncDest: Int = BF_ONE_MINUS_SOURCE_ALPHA
    var blendFuncSource: Int = BF_SOURCE_ALPHA
    var posOffset: Vector3 = Vector3.ZERO
    var parameter: Float = 0f

    fun init(sourcep: ViewerPartSource, img: Any?, cb: VPCallback?) {
        partID = sNextPartID.getAndIncrement().toUInt()
        flags = 0x00fu
        lastUpdateTime = 0f
        maxAge = 10f
        skipOffset = 0f
        vpCallback = cb
        partSourcep = sourcep
        imagep = img
    }

    fun finalize() {
        val src = partSourcep
        if (src != null && src.lastPart === this) src.lastPart = null
        parent?.let { if (it.child === this) it.child = child }
        child?.let { if (it.parent === this) it.parent = parent }
        partSourcep = null
        ViewerPartSim.particleCount2.decrementAndGet()
    }
}

// ---------------------------------------------------------------------------
// ViewerPartGroup – a spatial bucket of particles
// ---------------------------------------------------------------------------

class ViewerPartGroup(centerAgent: Vector3, boxSide: Float, val hud: Boolean) {

    companion object {
        private val idSeed = AtomicInteger(0)
        private const val SQRT3 = 1.7320508f
    }

    val particles: MutableList<ViewerPart> = mutableListOf()

    val centerAgent: Vector3
    val boxRadius: Float
    val boxSide: Float
    private var minObjPos: Vector3
    private var maxObjPos: Vector3

    var uniformParticles: Boolean = true
    val id: UInt = idSeed.incrementAndGet().toUInt()
    var skippedTime: Float = 0f

    var voPartGroupp: Any? = null      // stub for LLVOPartGroup
    private var regionp: Any? = null   // stub for LLViewerRegion

    init {
        this.centerAgent = centerAgent
        this.boxRadius = SQRT3 * boxSide * 0.5f
        this.boxSide = boxSide

        val extents = Vector3(boxRadius, boxRadius, boxRadius)
        minObjPos = centerAgent - extents
        maxObjPos = centerAgent + extents

        regionp = null

        voPartGroupp = null
        // no-op: configure voPartGroupp position, scale, add to pipeline;
        // compute minObjPos/maxObjPos from spatial group octree node
    }

    fun cleanup() {
        // no-op
    }

    fun posInGroup(pos: Vector3, desiredSize: Float = -1f): Boolean {
        if (pos.x < minObjPos.x || pos.y < minObjPos.y || pos.z < minObjPos.z) return false
        if (pos.x > maxObjPos.x || pos.y > maxObjPos.y || pos.z > maxObjPos.z) return false
        if (desiredSize > 0f &&
            (desiredSize < boxRadius * 0.5f || desiredSize > boxRadius * 2f)) return false
        return true
    }

    fun addPart(part: ViewerPart, desiredSize: Float = -1f): Boolean {
        if (part.flags and PartFlags.LL_PART_HUD != 0u && !hud) return false

        val uniformPart = part.scale.x == part.scale.y &&
            (part.flags and PartFlags.LL_PART_FOLLOW_VELOCITY_MASK == 0u)

        if (!posInGroup(part.posAgent, desiredSize) ||
            (uniformParticles && !uniformPart) ||
            (!uniformParticles && uniformPart)) return false

        // no-op: markRebuild(voPartGroupp drawable, REBUILD_ALL)

        particles.add(part)
        part.skipOffset = skippedTime
        ViewerPartSim.particleCount.incrementAndGet()
        return true
    }

    fun updateParticles(lastdt: Float) {
        val gravity = Vector3(0f, 0f, -9.8f)
        ViewerPartSim.checkParticleCount(particles.size.toUInt())

        var changed = false
        var i = 0
        while (i < particles.size) {
            val part = particles[i]
            val dt = lastdt + skippedTime - part.skipOffset
            part.skipOffset = 0f

            val curTime = part.lastUpdateTime + dt
            val frac = curTime / part.maxAge

            if (part.flags and PartFlags.LL_PART_FOLLOW_SRC_MASK != 0u) {
                part.posAgent = (part.partSourcep?.posAgent ?: Vector3.ZERO) + part.posOffset
            }

            part.vpCallback?.invoke(part, dt)

            if (part.flags and PartFlags.LL_PART_WIND_MASK != 0u) {
                part.velocity = part.velocity * (1f - 0.1f * dt)
                part.velocity = part.velocity + Vector3.ZERO * (0.1f * dt)
            }

            if (part.flags and PartFlags.LL_PART_TARGET_POS_MASK != 0u) {
                val remaining = part.maxAge - part.lastUpdateTime
                val step = (dt / remaining).coerceIn(0f, 0.1f) * 5f
                val deltaPos = (part.partSourcep?.targetPosAgent ?: Vector3.ZERO) - part.posAgent
                val deltaPosScaled = deltaPos * (1f / remaining)
                part.velocity = part.velocity * (1f - step) + deltaPosScaled * step
            }

            if (part.flags and PartFlags.LL_PART_TARGET_LINEAR_MASK != 0u) {
                val src = part.partSourcep
                val deltaPos = (src?.targetPosAgent ?: Vector3.ZERO) - (src?.posAgent ?: Vector3.ZERO)
                part.posAgent = (src?.posAgent ?: Vector3.ZERO) + deltaPos * frac
                part.velocity = deltaPos
            } else {
                part.posAgent = part.posAgent + part.velocity * dt + part.accel * (0.5f * dt * dt)
                part.velocity = part.velocity + part.accel * dt
            }

            if (part.flags and PartFlags.LL_PART_BOUNCE_MASK != 0u) {
                val srcZ = part.partSourcep?.posAgent?.z ?: 0f
                val dz = part.posAgent.z - srcZ
                if (dz < 0f) {
                    part.posAgent = part.posAgent.copy(z = part.posAgent.z - 2f * dz)
                    part.velocity = part.velocity.copy(z = part.velocity.z * -0.75f)
                }
            }

            if (part.flags and PartFlags.LL_PART_FOLLOW_SRC_MASK != 0u) {
                part.posOffset = part.posAgent - (part.partSourcep?.posAgent ?: Vector3.ZERO)
            }

            if (part.flags and PartFlags.LL_PART_INTERP_COLOR_MASK != 0u) {
                val sc = part.startColor
                val ec = part.endColor
                val inv = 1f - frac
                part.color = Color4(
                    r = sc.r * inv + ec.r * frac,
                    g = sc.g * inv + ec.g * frac,
                    b = sc.b * inv + ec.b * frac,
                    a = sc.a * inv + ec.a * frac
                )
            }

            if (part.flags and PartFlags.LL_PART_INTERP_SCALE_MASK != 0u) {
                val ss = part.startScale
                val es = part.endScale
                part.scale = Vector2(
                    x = ss.x * (1f - frac) + es.x * frac,
                    y = ss.y * (1f - frac) + es.y * frac
                )
            }

            val glowAlpha = (part.startGlow + (part.endGlow - part.startGlow) * frac) * 255f
            part.glow = part.glow.copy(a = glowAlpha.toInt().coerceIn(0, 255).toUByte())

            part.lastUpdateTime = curTime

            val isDead = part.lastUpdateTime > part.maxAge ||
                part.flags == PartFlags.LL_PART_DEAD_MASK
            if (isDead) {
                particles.removeAt(i)
                ViewerPartSim.particleCount.decrementAndGet()
                part.finalize()
                changed = true
            } else {
                val desiredSize = calcDesiredSize(part.posAgent, part.scale)
                if (!posInGroup(part.posAgent, desiredSize)) {
                    particles.removeAt(i)
                    ViewerPartSim.put(part)
                    // put() increments sParticleCount on success, so always decrement here
                    ViewerPartSim.particleCount.decrementAndGet()
                    changed = true
                } else {
                    i++
                }
            }
        }

        if (changed) {
            // no-op: markRebuild(voPartGroupp drawable, REBUILD_ALL)
        }

        if (particles.isEmpty()) {
            // no-op: gObjectList.killObject(voPartGroupp); voPartGroupp = null
        }

        ViewerPartSim.checkParticleCount()
    }

    fun shift(offset: Vector3) {
        val nc = centerAgent + offset
        minObjPos = minObjPos + offset
        maxObjPos = maxObjPos + offset
        particles.forEach { it.posAgent = it.posAgent + offset }
    }

    fun removeParticlesByID(sourceId: UInt) {
        particles.forEach { p ->
            if (p.partSourcep?.id == sourceId) p.flags = PartFlags.LL_PART_DEAD_MASK
        }
    }

    fun getCount(): Int = particles.size
    fun getRegion(): Any? = regionp
}

// ---------------------------------------------------------------------------
// ViewerPartSim – singleton particle simulation
// ---------------------------------------------------------------------------

object ViewerPartSim {

    const val MAX_PART_COUNT: Int              = 8192
    const val PART_THROTTLE_THRESHOLD: Float   = 0.9f
    const val PART_ADAPT_RATE_MULT: Float      = 2.0f
    val PART_THROTTLE_RESCALE: Float           = PART_THROTTLE_THRESHOLD / (1f - PART_THROTTLE_THRESHOLD)
    val PART_ADAPT_RATE_MULT_RECIP: Float      = 1f / PART_ADAPT_RATE_MULT

    var maxParticleCount: Int = 0

    // Atomic counters to detect per-frame mismatches (mirrors AVX2 mismatch fix in original)
    val particleCount: AtomicInteger = AtomicInteger(0)
    val particleCount2: AtomicInteger = AtomicInteger(0)

    var particleAdaptiveRate: Float = 0.0625f
    var particleBurstRate: Float = 0.5f

    private val partGroups: MutableList<ViewerPartGroup> = mutableListOf()
    private val partSources: MutableList<ViewerPartSource> = mutableListOf()

    private var idSeed: UInt = 0u
    var id: UInt = ++idSeed

    private var lastSimTime: Long = System.nanoTime()

    // Allow callers to iterate sources read-only
    fun getParticleSystemList(): List<ViewerPartSource> = partSources

    fun enable(enabled: Boolean) {
        if (!enabled && maxParticleCount > 0) {
            maxParticleCount = 0
        } else if (enabled && maxParticleCount < 1) {
            maxParticleCount = MAX_PART_COUNT
        }
    }

    fun destroyClass() {
        partGroups.forEach { it.cleanup() }
        partGroups.clear()
        partSources.clear()
    }

    fun shouldAddPart(): Boolean {
        val count = particleCount.get()
        if (count >= MAX_PART_COUNT) return false
        if (count > PART_THROTTLE_THRESHOLD * maxParticleCount) {
            val frac = (count.toFloat() / maxParticleCount - PART_THROTTLE_THRESHOLD) * PART_THROTTLE_RESCALE
            if (Math.random().toFloat() < frac) return false
        }
        val minFrameRate = 4f
        val currentFps: Float = 60f
        @Suppress("UNREACHABLE_CODE")
        if (currentFps < minFrameRate) return false
        return true
    }

    fun maxRate(): Float {
        val count = particleCount.get()
        return when {
            count >= MAX_PART_COUNT -> 1f
            count > PART_THROTTLE_THRESHOLD * maxParticleCount ->
                ((count.toFloat() / maxParticleCount) - PART_THROTTLE_THRESHOLD) * PART_THROTTLE_RESCALE
            else -> 0f
        }
    }

    fun getRefRate(): Float = particleAdaptiveRate
    fun getBurstRate(): Float = particleBurstRate
    fun aboveParticleLimit(): Boolean = particleCount.get() > maxParticleCount

    fun setMaxPartCount(max: Int) { maxParticleCount = max }
    fun getMaxPartCount(): Int = maxParticleCount

    fun addPart(part: ViewerPart) {
        if (particleCount.get() < MAX_PART_COUNT) {
            put(part)
        } else {
            part.finalize()
        }
    }

    fun put(part: ViewerPart): ViewerPartGroup? {
        val maxMag = 1_000_000f * 1_000_000f
        if (part.posAgent.magnitudeSquared() > maxMag || !part.posAgent.isFinite()) return null

        val desiredSize = calcDesiredSize(part.posAgent, part.scale)

        for (group in partGroups) {
            if (group.addPart(part, desiredSize)) return group
        }

        val group = createViewerPartGroup(part.posAgent, desiredSize, part.flags and PartFlags.LL_PART_HUD != 0u)
        group.uniformParticles = part.scale.x == part.scale.y &&
            (part.flags and PartFlags.LL_PART_FOLLOW_VELOCITY_MASK == 0u)

        if (!group.addPart(part)) {
            partGroups.remove(group)
            group.cleanup()
            part.finalize()
            return null
        }
        return group
    }

    private fun createViewerPartGroup(posAgent: Vector3, desiredSize: Float, hud: Boolean): ViewerPartGroup {
        val group = ViewerPartGroup(posAgent, desiredSize, hud)
        partGroups.add(group)
        return group
    }

    fun shift(offset: Vector3) {
        partSources.forEach { src ->
            src.posAgent = src.posAgent + offset
            src.targetPosAgent = src.targetPosAgent + offset
            src.lastUpdatePosAgent = src.lastUpdatePosAgent + offset
        }
        partGroups.forEach { it.shift(offset) }
    }

    fun updateSimulation() {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastSimTime) / 1_000_000_000.0).toFloat().coerceAtMost(0.1f)
        lastSimTime = nowNs

        val count = partSources.size
        if (count == 0) {
            updatePartBurstRate()
            return
        }

        val start = (Math.random() * count).toInt()
        val dir = if (Math.random() > 0.5) 1 else -1
        val delDir = if (dir == -1) -1 else 0

        var i = start
        var numUpdates = 0
        while (numUpdates < count) {
            i = when {
                i >= partSources.size -> 0
                i < 0 -> partSources.size - 1
                else -> i
            }

            val src = partSources[i]
            if (!src.isDead) {
                val vobj = src.sourceObjectp
                var upd = true

                if (vobj != null && vobj.isAvatar()) {
                    upd = true
                }
                if (upd && vobj != null) {
                    upd = true
                }

                if (upd) src.update(dt)
            }

            if (partSources[i].isDead) {
                partSources.removeAt(i)
                i += delDir
            } else {
                i += dir
            }
            numUpdates++
        }

        var gi = 0
        while (gi < partGroups.size) {
            val group = partGroups[gi]

            val visirate: Int = 1
            val currentFrame: Int = 0
            if ((currentFrame + group.id.toInt()) % visirate == 0) {
                // no-op: markRebuild(vobj drawable, REBUILD_ALL)
                group.updateParticles(dt * visirate)
                group.skippedTime = 0f
                if (group.getCount() == 0) {
                    partGroups.removeAt(gi)
                    gi--
                }
            } else {
                group.skippedTime += dt
            }
            gi++
        }

        val frame: Int = 0
        if (frame % 16 == 0) {
            val cnt = particleCount.get()
            if (cnt > maxParticleCount * 0.875f && particleAdaptiveRate < 2f) {
                particleAdaptiveRate *= PART_ADAPT_RATE_MULT
            } else if (cnt < maxParticleCount * 0.5f && particleAdaptiveRate > 0.03125f) {
                particleAdaptiveRate *= PART_ADAPT_RATE_MULT_RECIP
            }
        }

        updatePartBurstRate()
    }

    fun updatePartBurstRate() {
        val frame: Int = 0
        if (frame and 0xf == 0) {
            val cnt = particleCount.get()
            when {
                cnt >= MAX_PART_COUNT -> particleBurstRate = 0f
                cnt > 0 -> {
                    if (particleBurstRate > 0.0000001f) {
                        val totalParticles = cnt / particleBurstRate
                        val newRate = (0.9f * maxParticleCount / totalParticles).coerceIn(0f, 1f)
                        val deltaThreshold = minOf(0.1f * maxOf(newRate, particleBurstRate), 0.1f)
                        val delta = (newRate - particleBurstRate).coerceIn(-deltaThreshold, deltaThreshold)
                        particleBurstRate = (particleBurstRate + 0.5f * delta).coerceIn(0f, 1f)
                    } else {
                        particleBurstRate += 0.0000001f
                    }
                }
                else -> particleBurstRate += 0.00125f
            }
        }
    }

    fun addPartSource(sourcep: ViewerPartSource) {
        sourcep.setStart()
        partSources.add(sourcep)
    }

    fun removeLastCreatedSource() {
        if (partSources.isNotEmpty()) partSources.removeAt(partSources.lastIndex)
    }

    fun cleanupRegion(regionp: Any) {
        val iter = partGroups.iterator()
        while (iter.hasNext()) {
            val group = iter.next()
            if (group.getRegion() === regionp) {
                group.cleanup()
                iter.remove()
            }
        }
    }

    fun clearParticlesByID(systemId: UInt) {
        partGroups.forEach { it.removeParticlesByID(systemId) }
        partSources.firstOrNull { it.id == systemId }?.setDead()
    }

    fun clearParticlesByOwnerID(taskId: UUID) {
        partSources
            .filter { it.getOwnerUUID() == taskId }
            .forEach { clearParticlesByID(it.id) }
    }

    fun checkParticleCount(size: UInt = 0u) {
        val cnt = particleCount.get()
        val cnt2 = particleCount2.get()
        if (cnt2 != cnt) {
            System.err.println("WARN: sParticleCount=$cnt sParticleCount2=$cnt2")
        }
        if (size > cnt2.toUInt()) {
            System.err.println("WARN: array size=$size > sParticleCount2=$cnt2")
        }
    }
}

// ---------------------------------------------------------------------------
// Free function used by both ViewerPartGroup and ViewerPartSim
// ---------------------------------------------------------------------------

fun calcDesiredSize(posAgent: Vector3, scale: Vector2): Float {
    val cameraDist: Float = 0f
    val desired = cameraDist / 4f
    return desired.coerceIn(scale.magnitude() * 0.5f, 32f)  // 32 = PART_SIM_BOX_SIDE*2
}
