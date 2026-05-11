package com.firestorm.newview

import java.util.UUID
import kotlin.math.abs
import kotlin.math.sin

const val NUM_POINTS = 5
private const val BEAM_SPACING = 0.075f

data class LLVector3d(val x: Double = 0.0, val y: Double = 0.0, val z: Double = 0.0) {
    operator fun plus(other: LLVector3d) = LLVector3d(x + other.x, y + other.y, z + other.z)
}

data class LLVector3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f)

data class LLColor4U(val r: UByte = 0u, val g: UByte = 0u, val b: UByte = 0u, val a: UByte = 0u)

class LLInterpLinearVec3d {
    private var startTime: Float = 0f
    private var endTime: Float = 0f
    private var startVal: LLVector3d = LLVector3d()
    private var endVal: LLVector3d = LLVector3d()
    private var curVal: LLVector3d = LLVector3d()
    private var curFrac: Float = 0f
    private var active: Boolean = false
    private var done: Boolean = false

    fun setStartTime(t: Float) { startTime = t }
    fun setEndTime(t: Float) { endTime = t }
    fun setStartVal(v: LLVector3d) { startVal = v }
    fun setEndVal(v: LLVector3d) { endVal = v }
    fun getEndTime(): Float = endTime
    fun getStartTime(): Float = startTime
    fun getCurVal(): LLVector3d = curVal
    fun getCurFrac(): Float = curFrac
    fun isActive(): Boolean = active
    fun isDone(): Boolean = done

    fun start() {
        active = true
        done = false
        curVal = startVal
        curFrac = 0f
    }

    fun update(time: Float) {
        if (!active) return
        if (time < startTime) return
        if (time >= endTime) {
            curFrac = 1f
            curVal = endVal
            done = true
            return
        }
        val span = endTime - startTime
        curFrac = if (span > 0f) (time - startTime) / span else 1f
        curVal = LLVector3d(
            startVal.x + (endVal.x - startVal.x) * curFrac,
            startVal.y + (endVal.y - startVal.y) * curFrac,
            startVal.z + (endVal.z - startVal.z) * curFrac
        )
    }
}

class LLInterpLinearF32 {
    private var startTime: Float = 0f
    private var endTime: Float = 0f
    private var startVal: Float = 0f
    private var endVal: Float = 0f
    private var curVal: Float = 0f
    private var active: Boolean = false

    fun setStartTime(t: Float) { startTime = t }
    fun setEndTime(t: Float) { endTime = t }
    fun setStartVal(v: Float) { startVal = v }
    fun setEndVal(v: Float) { endVal = v }
    fun getStartTime(): Float = startTime
    fun getEndTime(): Float = endTime
    fun getCurVal(): Float = curVal

    fun start() {
        active = true
        curVal = startVal
    }

    fun update(time: Float) {
        if (!active) return
        if (time < startTime) { curVal = startVal; return }
        if (time >= endTime) { curVal = endVal; return }
        val span = endTime - startTime
        val frac = if (span > 0f) (time - startTime) / span else 1f
        curVal = startVal + (endVal - startVal) * frac
    }
}

abstract class LLHUDEffect(val type: UByte) {
    var mSourceObject: LLViewerObjectBeam? = null
    var mTargetObject: LLViewerObjectBeam? = null
    var mDuration: Float = 1f
    var mColor: LLColor4U = LLColor4U()
    var mPositionGlobal: LLVector3d = LLVector3d()
    var mDead: Boolean = false

    fun markDead() { mDead = true }
    open fun render() {}
    open fun renderForTimer() {}
    open fun packData(msgSys: Any) {}
    open fun unpackData(msgSys: Any, blockNum: Int) {}
}

class LLViewerObjectBeam {
    val mID: UUID = UUID.randomUUID()
    var mDrawable: LLDrawableBeam? = null

    fun isDead(): Boolean = TODO("check object lifecycle state")
    fun isAvatar(): Boolean = TODO("check object type flags")
    fun getPositionGlobal(): LLVector3d = TODO("return global-space position")
    fun getPCode(): Int = TODO("return primitive code")
}

class LLDrawableBeam {
    fun getGeneration(): Int = TODO("return drawable generation counter")
    fun getPositionAgent(): LLVector3 = TODO("return agent-space position from drawable")
}

class LLHUDEffectBeam(type: UByte) : LLHUDEffect(type) {

    private var killTime: Float = mDuration
    private val timer: Long = System.currentTimeMillis()
    private val interp: Array<LLInterpLinearVec3d> = Array(NUM_POINTS) { LLInterpLinearVec3d() }
    private val interpFade: Array<LLInterpLinearF32> = Array(NUM_POINTS) { LLInterpLinearF32() }
    private val fadeInterp: LLInterpLinearF32 = LLInterpLinearF32()
    private var targetPos: LLVector3d = LLVector3d()

    init {
        killTime = mDuration
        for (i in 0 until NUM_POINTS) {
            interp[i].setStartTime(BEAM_SPACING * i)
            interp[i].setEndTime(BEAM_SPACING * NUM_POINTS + BEAM_SPACING * i)
            interp[i].start()
            interpFade[i].setStartTime(BEAM_SPACING * NUM_POINTS + BEAM_SPACING * i - 0.5f * NUM_POINTS * BEAM_SPACING)
            interpFade[i].setEndTime(BEAM_SPACING * NUM_POINTS + BEAM_SPACING * i)
            interpFade[i].setStartVal(1f)
            interpFade[i].setEndVal(0f)
        }
        val fadeLength = minOf(0.5f, mDuration)
        fadeInterp.setStartTime(killTime - fadeLength)
        fadeInterp.setEndTime(killTime)
        fadeInterp.setStartVal(1f)
        fadeInterp.setEndVal(0f)
    }

    private fun elapsedTimeSec(): Float = (System.currentTimeMillis() - timer) / 1000f

    override fun setSourceObject(objp: LLViewerObjectBeam) {
        if (objp.isDead()) {
            mSourceObject = null
            return
        }
        if (mSourceObject === objp) return
        mSourceObject = objp
        for (i in 0 until NUM_POINTS) {
            val startPos = if (objp.isAvatar()) {
                getAvatarLeftWristGlobal(objp)
            } else {
                objp.getPositionGlobal()
            }
            interp[i].setStartVal(startPos)
            interp[i].start()
        }
    }

    fun setTargetObject(objp: LLViewerObjectBeam) {
        mTargetObject = objp
    }

    fun setTargetPos(posGlobal: LLVector3d) {
        targetPos = posGlobal
        mTargetObject = null
    }

    override fun packData(msgSys: Any) {
        val src = mSourceObject
        if (src == null) { markDead(); return }
        if (!src.isAvatar()) { markDead(); return }
        TODO("APR: use JVM equivalent — serialize source UUID, target flag, and target UUID or LLVector3d into 41-byte binary block")
    }

    override fun unpackData(msgSys: Any, blockNum: Int) {
        TODO("APR: use JVM equivalent — deserialize 41-byte binary block, resolve source/target UUIDs from object list, reset kill time and fade interp")
    }

    override fun render() {
        val src = mSourceObject
        if (src == null || src.isDead()) { markDead(); return }

        val time = elapsedTimeSec()
        if (killTime < time) { markDead(); return }

        fadeInterp.update(time)

        val tgt = mTargetObject
        if (tgt != null && tgt.mDrawable != null) {
            targetPos = if (tgt.mDrawable!!.getGeneration() == -1) {
                tgt.getPositionGlobal()
            } else {
                TODO("GPU: convert drawable agent position to global coords via agent transform")
            }
        }

        for (i in 0 until NUM_POINTS) {
            interp[i].update(time)
            if (!interp[i].isActive()) continue
            interpFade[i].update(time)

            if (interp[i].isDone()) {
                setupParticle(i)
            }

            val frac = interp[i].getCurFrac()
            val scale = (0.025f + abs(0.05f * sin(2.0 * Math.PI * (frac - time)).toFloat())) *
                    interpFade[i].getCurVal()

            val alpha = (fadeInterp.getCurVal() * (mColor.a.toInt() / 255f) *
                    interpFade[i].getCurVal() * 255f).toInt().coerceIn(0, 255)

            TODO("GPU: translate/scale modelview matrix to interp[i].getCurVal() position and render sphere with computed scale and alpha")
        }
    }

    override fun renderForTimer() {
        render()
    }

    private fun setupParticle(i: Int) {
        val src = mSourceObject ?: return
        val startPosGlobal = if (src.getPCode() == LL_PCODE_LEGACY_AVATAR) {
            getAvatarLeftWristGlobal(src)
        } else {
            src.getPositionGlobal()
        }

        val scale = 0.5f
        val dx = (Math.random() * scale - 0.5 * scale).toFloat().toDouble()
        val dy = (Math.random() * scale - 0.5 * scale).toFloat().toDouble()
        val dz = (Math.random() * scale - 0.5 * scale).toFloat().toDouble()
        val jitteredTarget = targetPos + LLVector3d(dx, dy, dz)

        interp[i].setStartTime(interp[i].getEndTime())
        interp[i].setEndTime(interp[i].getStartTime() + BEAM_SPACING * NUM_POINTS)
        interp[i].setStartVal(startPosGlobal)
        interp[i].setEndVal(jitteredTarget)
        interp[i].start()

        interpFade[i].setStartTime(interp[i].getStartTime() + BEAM_SPACING * NUM_POINTS - 0.5f * NUM_POINTS * BEAM_SPACING)
        interpFade[i].setEndTime(interp[i].getStartTime() + BEAM_SPACING * NUM_POINTS - 0.05f)
        interpFade[i].start()
    }

    private fun getAvatarLeftWristGlobal(obj: LLViewerObjectBeam): LLVector3d {
        TODO("APR: use JVM equivalent — query avatar skeleton left wrist joint world position and convert to global coords")
    }

    companion object {
        const val LL_PCODE_LEGACY_AVATAR = 0x2F
    }
}

fun LLHUDEffect.setSourceObject(objp: LLViewerObjectBeam) {
    if (this is LLHUDEffectBeam) setSourceObject(objp)
}
