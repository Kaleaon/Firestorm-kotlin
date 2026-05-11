package com.firestorm.newview

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

typealias VPCallback = (part: ViewerPart, dt: Float) -> Unit

// ---------------------------------------------------------------------------
// Minimal math/colour stubs – just enough structure for the particle system.
// ---------------------------------------------------------------------------

data class Vector3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun plus(o: Vector3) = Vector3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vector3) = Vector3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vector3(x * s, y * s, z * s)
    fun magnitude() = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    fun magnitudeSquared() = x * x + y * y + z * z
    fun normalized(): Vector3 {
        val m = magnitude()
        return if (m > 0f) Vector3(x / m, y / m, z / m) else this
    }
    fun isFinite() = x.isFinite() && y.isFinite() && z.isFinite()
    companion object { val ZERO = Vector3() }
}

data class Vector3d(val x: Double = 0.0, val y: Double = 0.0, val z: Double = 0.0) {
    fun magnitudeSquared() = x * x + y * y + z * z
    fun isZero() = x == 0.0 && y == 0.0 && z == 0.0
    operator fun plus(o: Vector3d) = Vector3d(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vector3d) = Vector3d(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vector3d(x * s, y * s, z * s)
    fun toVector3() = Vector3(x.toFloat(), y.toFloat(), z.toFloat())
    companion object { val ZERO = Vector3d() }
}

data class Vector2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(o: Vector2) = Vector2(x + o.x, y + o.y)
    operator fun times(s: Float) = Vector2(x * s, y * s)
    fun magnitude() = sqrt((x * x + y * y).toDouble()).toFloat()
}

data class Color4(val r: Float = 0f, val g: Float = 0f, val b: Float = 0f, val a: Float = 1f)
data class Color4U(val r: UByte = 0u, val g: UByte = 0u, val b: UByte = 0u, val a: UByte = 0u)
data class Color3(val r: Float, val g: Float, val b: Float)
data class Quaternion(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 1f) {
    companion object { val DEFAULT = Quaternion() }
}

// Blend func constants mirroring LLRender enums
const val BF_SOURCE_ALPHA = 0
const val BF_ONE_MINUS_SOURCE_ALPHA = 1

// Part-data flags (mirroring LLPartData)
object PartFlags {
    const val LL_PART_DEAD_MASK: UInt               = 0u
    const val LL_PART_HUD: UInt                     = 0x0200u
    const val LL_PART_FOLLOW_SRC_MASK: UInt         = 0x0001u
    const val LL_PART_FOLLOW_VELOCITY_MASK: UInt    = 0x0002u
    const val LL_PART_TARGET_POS_MASK: UInt         = 0x0004u
    const val LL_PART_TARGET_LINEAR_MASK: UInt      = 0x0008u
    const val LL_PART_WIND_MASK: UInt               = 0x0010u
    const val LL_PART_BOUNCE_MASK: UInt             = 0x0020u
    const val LL_PART_INTERP_COLOR_MASK: UInt       = 0x0040u
    const val LL_PART_INTERP_SCALE_MASK: UInt       = 0x0080u
    const val LL_PART_RIBBON_MASK: UInt             = 0x0100u
}

data class PartData(
    val maxAge: Float = 10f,
    val startColor: Color4 = Color4(),
    val endColor: Color4 = Color4(),
    val startScale: Vector2 = Vector2(1f, 1f),
    val endScale: Vector2 = Vector2(1f, 1f),
    val blendFuncDest: Int = BF_ONE_MINUS_SOURCE_ALPHA,
    val blendFuncSource: Int = BF_SOURCE_ALPHA,
    val startGlow: Float = 0f,
    val endGlow: Float = 0f,
    val flags: UInt = 0u
) {
    companion object {
        val LL_PART_DEAD_MASK: UInt = PartFlags.LL_PART_DEAD_MASK
    }
}

data class PartSysData(
    val maxAge: Float = 0f,
    val startAge: Float = 0f,
    val burstRate: Float = 0.1f,
    val burstPartCount: Int = 1,
    var burstRadius: Float = 0f,
    val burstSpeedMin: Float = 0f,
    val burstSpeedMax: Float = 1f,
    val innerAngle: Float = 0f,
    val outerAngle: Float = 0f,
    val pattern: UInt = 0u,
    val flags: UInt = 0u,
    val angularVelocity: Vector3 = Vector3.ZERO,
    val partAccel: Vector3 = Vector3.ZERO,
    val targetUUID: UUID = UUID(0, 0),
    val partData: PartData = PartData()
) {
    companion object {
        const val LL_PART_SRC_PATTERN_DROP: UInt        = 0x01u
        const val LL_PART_SRC_PATTERN_EXPLODE: UInt     = 0x02u
        const val LL_PART_SRC_PATTERN_ANGLE: UInt       = 0x04u
        const val LL_PART_SRC_PATTERN_ANGLE_CONE: UInt  = 0x08u
        const val LL_PART_USE_NEW_ANGLE: UInt           = 0x01u
    }
}

// ---------------------------------------------------------------------------
// ViewerObject stub – implemented elsewhere in the port
// ---------------------------------------------------------------------------

abstract class ViewerObject {
    abstract val id: UUID
    abstract fun isAvatar(): Boolean
    abstract fun isAttachment(): Boolean
    abstract fun isDead(): Boolean
    abstract fun isHUDAttachment(): Boolean
    abstract fun getRenderPosition(): Vector3
    abstract fun getRenderRotation(): Quaternion
    abstract fun getPositionAgent(): Vector3
    abstract fun getPositionGlobal(): Vector3d
    abstract fun getWorldRotation(): Quaternion
    abstract fun getRotationRegion(): Quaternion
    abstract var mDrawable: Any?
}

// ---------------------------------------------------------------------------
// Base particle source
// ---------------------------------------------------------------------------

open class ViewerPartSource(val type: UInt) {

    companion object {
        const val LL_PART_SOURCE_NULL: UInt   = 0u
        const val LL_PART_SOURCE_SCRIPT: UInt = 1u
        const val LL_PART_SOURCE_SPIRAL: UInt = 2u
        const val LL_PART_SOURCE_BEAM: UInt   = 3u
        const val LL_PART_SOURCE_CHAT: UInt   = 4u

        private val idSeed = AtomicInteger(0)

        fun updatePart(part: ViewerPart, dt: Float) {
            // base no-op; subclasses override via companion static
        }
    }

    var posAgent: Vector3 = Vector3.ZERO
    var targetPosAgent: Vector3 = Vector3.ZERO
    var lastUpdatePosAgent: Vector3 = Vector3.ZERO
    var sourceObjectp: ViewerObject? = null
    val id: UInt = idSeed.incrementAndGet().toUInt()
    var lastPart: ViewerPart? = null

    var isDead: Boolean = false
        protected set
    var isSuspended: Boolean = false

    protected var lastUpdateTime: Float = 0f
    protected var lastPartTime: Float = 0f
    protected var ownerUUID: UUID = UUID(0, 0)
    protected var ownerAvatarp: ViewerObject? = null
    protected var imagep: Any? = null
    protected var partFlags: UInt = 0u
    protected var delay: UInt = 0u

    open fun update(dt: Float) {
        error("ViewerPartSource.update() must be overridden")
    }

    open fun setDead() {
        isDead = true
    }

    fun setSuspended(state: Boolean) { isSuspended = state }
    fun setOwnerUUID(ownerId: UUID) { ownerUUID = ownerId }
    fun getOwnerUUID(): UUID = ownerUUID

    fun getImageUUID(): UUID {
        TODO("GPU: return texture UUID from imagep")
    }

    fun setStart() {
        delay = 0u
    }
}

// ---------------------------------------------------------------------------
// Script-driven particle source
// ---------------------------------------------------------------------------

class ViewerPartSourceScript(sourceObjp: ViewerObject) : ViewerPartSource(LL_PART_SOURCE_SCRIPT) {

    companion object {
        fun unpackPSS(
            sourceObjp: ViewerObject,
            pssp: ViewerPartSourceScript?,
            blockNum: Int
        ): ViewerPartSourceScript? {
            TODO("APR: unpack script particle system from network message block $blockNum")
        }

        fun unpackPSSFromDataPacker(
            sourceObjp: ViewerObject,
            pssp: ViewerPartSourceScript?,
            dp: Any,
            legacy: Boolean
        ): ViewerPartSourceScript? {
            TODO("APR: unpack script particle system from data packer; legacy=$legacy")
        }

        fun createPSS(
            sourceObjp: ViewerObject,
            particleParameters: PartSysData
        ): ViewerPartSourceScript {
            val newPssp = ViewerPartSourceScript(sourceObjp)
            newPssp.partSysData = particleParameters
            if (particleParameters.targetUUID != UUID(0, 0)) {
                TODO("APR: look up target object by UUID and call setTargetObject")
            }
            return newPssp
        }
    }

    init {
        sourceObjectp = sourceObjp
        posAgent = sourceObjp.getPositionAgent()
        imagep = TODO("GPU: fetch default particle image (LLViewerFetchedTexture::sDefaultParticleImagep)")
    }

    var partSysData: PartSysData = PartSysData()
    private var rotation: Quaternion = Quaternion.DEFAULT
    private var targetObjectp: ViewerObject? = null

    override fun setDead() {
        isDead = true
        sourceObjectp = null
        targetObjectp = null
    }

    override fun update(dt: Float) {
        if (isSuspended) return

        if (ownerAvatarp == null && ownerUUID != UUID(0, 0)) {
            ownerAvatarp = TODO("APR: find avatar by ownerUUID")
        }

        TODO("GPU: check owner avatar overall-appearance; return early if not AOA_NORMAL; " +
             "update source/target positions; generate burst particles per partSysData")
    }

    fun getImage(): Any? = imagep

    fun setImage(img: Any?) {
        imagep = img
    }

    fun setTargetObject(objp: ViewerObject?) {
        targetObjectp = objp
    }
}

// ---------------------------------------------------------------------------
// Spiral particle source (customize-avatar effect)
// ---------------------------------------------------------------------------

class ViewerPartSourceSpiral(pos: Vector3) : ViewerPartSource(LL_PART_SOURCE_SPIRAL) {

    companion object {
        fun updatePart(part: ViewerPart, dt: Float) {
            val frac = part.lastUpdateTime / part.maxAge
            val ps = part.partSourcep as? ViewerPartSourceSpiral ?: return
            val srcObj = ps.sourceObjectp
            part.posAgent = if (srcObj != null && !srcObj.isDead() && srcObj.mDrawable != null) {
                srcObj.getRenderPosition()
            } else {
                ps.posAgent
            }
            val x = sin(2.0 * PI * frac + part.parameter).toFloat()
            val y = cos(2.0 * PI * frac + part.parameter).toFloat()
            part.posAgent = part.posAgent.copy(
                x = part.posAgent.x + x,
                y = part.posAgent.y + y,
                z = part.posAgent.z + (-0.5f + frac)
            )
        }
    }

    var color: Color4 = Color4()

    init { posAgent = pos }

    override fun setDead() {
        isDead = true
        sourceObjectp = null
    }

    override fun update(dt: Float) {
        if (imagep == null) {
            imagep = TODO("GPU: fetch default particle image")
        }

        val rate = 0.025f
        lastUpdateTime += dt
        var dtUpdate = minOf(maxOf(1f, 10f * rate), lastUpdateTime - lastPartTime)

        if (dtUpdate > rate) {
            lastPartTime = lastUpdateTime
            if (!ViewerPartSim.shouldAddPart()) return

            val srcObj = sourceObjectp
            if (srcObj != null && !srcObj.isDead() && srcObj.mDrawable != null) {
                posAgent = srcObj.getRenderPosition()
            }

            val part = ViewerPart()
            part.init(this, imagep, ViewerPartSourceSpiral::updatePart)
            part.startColor = color
            part.endColor = color.copy(a = 0f)
            part.posAgent = posAgent
            part.maxAge = 1f
            part.flags = PartFlags.LL_PART_INTERP_COLOR_MASK
            part.lastUpdateTime = 0f
            part.scale = Vector2(0.25f, 0.25f)
            part.parameter = (Math.random() * 2.0 * PI).toFloat()
            part.blendFuncDest = BF_ONE_MINUS_SOURCE_ALPHA
            part.blendFuncSource = BF_SOURCE_ALPHA
            part.startGlow = 0f
            part.endGlow = 0f
            part.glow = Color4U(0u, 0u, 0u, 0u)
            ViewerPartSim.addPart(part)
        }
    }

    fun setSourceObject(objp: ViewerObject?) { sourceObjectp = objp }
    fun setColor(c: Color4) { color = c }
}

// ---------------------------------------------------------------------------
// Tractor-beam particle source
// ---------------------------------------------------------------------------

class ViewerPartSourceBeam : ViewerPartSource(LL_PART_SOURCE_BEAM) {

    companion object {
        fun updatePart(part: ViewerPart, dt: Float) {
            val frac = part.lastUpdateTime / part.maxAge
            val psb = part.partSourcep as? ViewerPartSourceBeam ?: run {
                part.flags = PartFlags.LL_PART_DEAD_MASK
                return
            }
            if (psb.sourceObjectp == null) {
                part.flags = PartFlags.LL_PART_DEAD_MASK
                return
            }
            val srcObj = psb.sourceObjectp
            val sourcePosAgent: Vector3 = when {
                srcObj != null && !srcObj.isDead() && srcObj.mDrawable != null -> {
                    if (srcObj.isAvatar()) {
                        TODO("GPU: get left-wrist world position from avatar")
                    } else {
                        srcObj.getRenderPosition()
                    }
                }
                else -> Vector3.ZERO
            }
            val tgtObj = psb.targetObjectp
            val targetPosAgent: Vector3 = when {
                tgtObj != null && !tgtObj.isDead() && tgtObj.mDrawable != null -> tgtObj.getRenderPosition()
                else -> Vector3.ZERO
            }
            val fromSrc = sourcePosAgent * (1f - frac)
            part.posAgent = if (psb.targetObjectp == null) {
                fromSrc + TODO<Vector3>("APR: convert psb.lkgTargetPosGlobal to agent coords") * frac
            } else {
                fromSrc + targetPosAgent * frac
            }
        }
    }

    var targetObjectp: ViewerObject? = null
    var lkgTargetPosGlobal: Vector3d = Vector3d.ZERO
    var color: Color4 = Color4()

    override fun setDead() {
        isDead = true
        sourceObjectp = null
        targetObjectp = null
    }

    fun setColor(c: Color4) { color = c }

    override fun update(dt: Float) {
        val rate = 0.025f
        lastUpdateTime += dt

        val srcObj = sourceObjectp
        if (srcObj != null && !srcObj.isDead() && srcObj.mDrawable != null) {
            posAgent = if (srcObj.isAvatar()) {
                TODO("GPU: get left-wrist world position from avatar")
            } else {
                srcObj.getRenderPosition()
            }
        }

        val tgtObj = targetObjectp
        when {
            tgtObj != null && !tgtObj.isDead() && tgtObj.mDrawable != null ->
                targetPosAgent = tgtObj.getRenderPosition()
            !lkgTargetPosGlobal.isZero() ->
                targetPosAgent = TODO("APR: convert lkgTargetPosGlobal to agent position")
        }

        var dtUpdate = minOf(maxOf(1f, 10f * rate), lastUpdateTime - lastPartTime)

        if (dtUpdate > rate) {
            lastPartTime = lastUpdateTime
            if (!ViewerPartSim.shouldAddPart()) return

            if (imagep == null) imagep = TODO("GPU: fetch default particle image")

            val part = ViewerPart()
            part.init(this, imagep, ViewerPartSourceBeam::updatePart)
            part.flags = (PartFlags.LL_PART_INTERP_COLOR_MASK or
                PartFlags.LL_PART_INTERP_SCALE_MASK or
                PartFlags.LL_PART_TARGET_POS_MASK or
                PartFlags.LL_PART_FOLLOW_VELOCITY_MASK)
            part.maxAge = 0.5f
            part.startColor = color
            part.endColor = color.copy(a = 0.4f)
            part.color = part.startColor
            part.startScale = Vector2(0.1f, 0.1f)
            part.endScale = Vector2(0.1f, 0.1f)
            part.scale = part.startScale
            part.posAgent = posAgent
            part.velocity = targetPosAgent - posAgent
            part.blendFuncDest = BF_ONE_MINUS_SOURCE_ALPHA
            part.blendFuncSource = BF_SOURCE_ALPHA
            part.startGlow = 0f
            part.endGlow = 0f
            part.glow = Color4U(0u, 0u, 0u, 0u)
            ViewerPartSim.addPart(part)
        }
    }

    fun setSourceObject(objp: ViewerObject?) { sourceObjectp = objp }
    fun setTargetObject(objp: ViewerObject?) { targetObjectp = objp }

    fun setSourcePosGlobal(posGlobal: Vector3d) {
        TODO("APR: convert posGlobal to agent coords and assign to posAgent")
    }

    fun setTargetPosGlobal(posGlobal: Vector3d) {
        lkgTargetPosGlobal = posGlobal
    }
}

// ---------------------------------------------------------------------------
// Chat-bubble spiral particle source
// ---------------------------------------------------------------------------

class ViewerPartSourceChat(pos: Vector3) : ViewerPartSource(LL_PART_SOURCE_CHAT) {

    companion object {
        fun updatePart(part: ViewerPart, dt: Float) {
            val frac = part.lastUpdateTime / part.maxAge
            val ps = part.partSourcep as? ViewerPartSourceChat ?: return
            val srcObj = ps.sourceObjectp
            part.posAgent = if (srcObj != null && !srcObj.isDead() && srcObj.mDrawable != null) {
                srcObj.getRenderPosition()
            } else {
                ps.posAgent
            }
            val x = sin(2.0 * PI * frac + part.parameter).toFloat()
            val y = cos(2.0 * PI * frac + part.parameter).toFloat()
            part.posAgent = part.posAgent.copy(
                x = part.posAgent.x + x,
                y = part.posAgent.y + y,
                z = part.posAgent.z + (-0.5f + frac)
            )
        }
    }

    var color: Color4 = Color4()

    init { posAgent = pos }

    override fun setDead() {
        isDead = true
        sourceObjectp = null
    }

    override fun update(dt: Float) {
        if (imagep == null) imagep = TODO("GPU: fetch default particle image")

        val rate = 0.025f
        lastUpdateTime += dt

        if (lastUpdateTime > 2f) {
            setDead()
            return
        }

        var dtUpdate = minOf(maxOf(1f, 10f * rate), lastUpdateTime - lastPartTime)

        if (dtUpdate > rate) {
            lastPartTime = lastUpdateTime
            if (!ViewerPartSim.shouldAddPart()) return

            val srcObj = sourceObjectp
            if (srcObj != null && !srcObj.isDead() && srcObj.mDrawable != null) {
                posAgent = srcObj.getRenderPosition()
            }

            val part = ViewerPart()
            part.init(this, imagep, ViewerPartSourceChat::updatePart)
            part.startColor = color
            part.endColor = color.copy(a = 0f)
            part.posAgent = posAgent
            part.maxAge = 1f
            part.flags = PartFlags.LL_PART_INTERP_COLOR_MASK
            part.lastUpdateTime = 0f
            part.scale = Vector2(0.25f, 0.25f)
            part.parameter = (Math.random() * 2.0 * PI).toFloat()
            part.blendFuncDest = BF_ONE_MINUS_SOURCE_ALPHA
            part.blendFuncSource = BF_SOURCE_ALPHA
            part.startGlow = 0f
            part.endGlow = 0f
            part.glow = Color4U(0u, 0u, 0u, 0u)
            ViewerPartSim.addPart(part)
        }
    }

    fun setSourceObject(objp: ViewerObject?) { sourceObjectp = objp }
    fun setColor(c: Color4) { color = c }
}
