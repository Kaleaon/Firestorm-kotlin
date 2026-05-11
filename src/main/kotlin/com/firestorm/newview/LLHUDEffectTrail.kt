package com.firestorm.newview

import java.util.UUID

const val NUM_TRAIL_POINTS: UInt = 40u

class LLHUDEffectSpiral(type: UByte) : LLHUDEffect(type) {

    private var mbInit: Boolean = false
    private var mPartSourcep: LLViewerPartSource? = null

    private var mKillTime: Float = 10f
    private var mVMag: Float = 1f
    private var mVOffset: Float = 0f
    private var mInitialRadius: Float = 1f
    private var mFinalRadius: Float = 1f
    private var mSpinRate: Float = 10f
    private var mFlickerRate: Float = 50f
    private var mScaleBase: Float = 0.1f
    private var mScaleVar: Float = 0f

    private val mTimer: LLFrameTimer = LLFrameTimer()
    private val mFadeInterp: LLInterpLinearFloat = LLInterpLinearFloat().apply {
        setStartTime(0f)
        setEndTime(mKillTime)
        setStartVal(1f)
        setEndVal(1f)
    }

    fun setVMag(vmag: Float) { mVMag = vmag }
    fun setVOffset(offset: Float) { mVOffset = offset }
    fun setInitialRadius(radius: Float) { mInitialRadius = radius }
    fun setFinalRadius(radius: Float) { mFinalRadius = radius }
    fun setScaleBase(scale: Float) { mScaleBase = scale }
    fun setScaleVar(scale: Float) { mScaleVar = scale }
    fun setSpinRate(rate: Float) { mSpinRate = rate }
    fun setFlickerRate(rate: Float) { mFlickerRate = rate }

    override fun markDead() {
        mPartSourcep?.setDead()
        mPartSourcep = null
        super.markDead()
    }

    override fun setTargetObject(objp: LLViewerObject?) {
        if (objp == mTargetObject) return
        mTargetObject = objp
    }

    override fun packData(mesgsys: LLMessageSystem) {
        super.packData(mesgsys)

        val packedData = ByteArray(56)

        TODO("APR: use JVM equivalent - htolememcpy mSourceObject.mID bytes into packedData[0..16]")
        TODO("APR: use JVM equivalent - htolememcpy mTargetObject.mID bytes into packedData[16..32]")
        TODO("APR: use JVM equivalent - htolememcpy mPositionGlobal bytes into packedData[32..56]")
        TODO("APR: use JVM equivalent - mesgsys.addBinaryDataFast _PREHASH_TypeData packedData 56")
    }

    override fun unpackData(mesgsys: LLMessageSystem, blocknum: Int) {
        val effectSize = 56
        val packedData = ByteArray(effectSize)

        super.unpackData(mesgsys, blocknum)

        val size: Int = TODO("APR: use JVM equivalent - mesgsys.getSizeFast _PREHASH_Effect blocknum _PREHASH_TypeData")
        if (size != effectSize) {
            return
        }
        TODO("APR: use JVM equivalent - mesgsys.getBinaryDataFast _PREHASH_Effect _PREHASH_TypeData packedData effectSize blocknum effectSize")
        TODO("APR: use JVM equivalent - htolememcpy objectId from packedData[0..16], targetObjectId from [16..32], mPositionGlobal from [32..56]")

        TODO("APR: use JVM equivalent - resolve source/target objects from gObjectList; call markDead and return if not found")

        triggerLocal()
    }

    fun triggerLocal() {
        mKillTime = mTimer.getElapsedTimeF32() + mDuration

        val showBeam: Boolean = TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(\"ShowSelectionBeam\")")

        val color: LLColor4 = LLColor4(mColor)

        if (mPartSourcep == null) {
            if (mTargetObject != null && mSourceObject != null) {
                if (showBeam) {
                    val psb = LLViewerPartSourceBeam()
                    psb.setColor(color)
                    psb.setSourceObject(mSourceObject)
                    psb.setTargetObject(mTargetObject)
                    TODO("APR: use JVM equivalent - psb.setOwnerUUID(gAgent.getID())")
                    TODO("GPU: LLViewerPartSim.getInstance().addPartSource(psb)")
                    mPartSourcep = psb
                }
            } else {
                if (mSourceObject != null && !mPositionGlobal.isExactlyZero()) {
                    if (showBeam) {
                        val psb = LLViewerPartSourceBeam()
                        psb.setSourceObject(mSourceObject)
                        psb.setTargetObject(null)
                        psb.setColor(color)
                        psb.mLKGTargetPosGlobal = mPositionGlobal
                        TODO("APR: use JVM equivalent - psb.setOwnerUUID(gAgent.getID())")
                        TODO("GPU: LLViewerPartSim.getInstance().addPartSource(psb)")
                        mPartSourcep = psb
                    }
                } else {
                    val pos: LLVector3 = if (mSourceObject != null) {
                        mSourceObject!!.getPositionAgent()
                    } else {
                        TODO("APR: use JVM equivalent - gAgent.getPosAgentFromGlobal(mPositionGlobal)")
                    }
                    val pss = LLViewerPartSourceSpiral(pos)
                    if (mSourceObject != null) {
                        pss.setSourceObject(mSourceObject)
                    }
                    pss.setColor(color)
                    TODO("APR: use JVM equivalent - pss.setOwnerUUID(gAgent.getID())")
                    TODO("GPU: LLViewerPartSim.getInstance().addPartSource(pss)")
                    mPartSourcep = pss
                }
            }
        } else {
            val ps = mPartSourcep!!
            if (ps.getType() == LLViewerPartSource.LL_PART_SOURCE_BEAM) {
                val psb = ps as LLViewerPartSourceBeam
                psb.setSourceObject(mSourceObject)
                psb.setTargetObject(mTargetObject)
                psb.setColor(color)
                if (mTargetObject == null) {
                    psb.mLKGTargetPosGlobal = mPositionGlobal
                }
            } else {
                val pss = ps as LLViewerPartSourceSpiral
                pss.setSourceObject(mSourceObject)
            }
        }

        mbInit = true
    }

    override fun render() {
        val time = mTimer.getElapsedTimeF32()

        val showSelectionBeam: Boolean = TODO("APR: use JVM equivalent - cached gSavedSettings.getBOOL(\"ShowSelectionBeam\")")

        val sourceObjectDead = mSourceObject != null && mSourceObject!!.isDead()
        val targetObjectDead = mTargetObject != null && mTargetObject!!.isDead()

        if (sourceObjectDead || targetObjectDead || mKillTime < time ||
            (mPartSourcep != null && !showSelectionBeam)) {
            markDead()
            return
        }
    }

    override fun renderForTimer() {
        render()
    }
}
