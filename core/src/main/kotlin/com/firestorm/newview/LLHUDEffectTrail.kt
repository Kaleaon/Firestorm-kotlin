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

        System.err.println("LLHUDEffectSpiral: packData (htolememcpy source object ID) not yet implemented")
        System.err.println("LLHUDEffectSpiral: packData (htolememcpy target object ID) not yet implemented")
        System.err.println("LLHUDEffectSpiral: packData (htolememcpy position global) not yet implemented")
        System.err.println("LLHUDEffectSpiral: packData (addBinaryDataFast TypeData) not yet implemented")
    }

    override fun unpackData(mesgsys: LLMessageSystem, blocknum: Int) {
        val effectSize = 56
        val packedData = ByteArray(effectSize)

        super.unpackData(mesgsys, blocknum)

        val size: Int = run {
            System.err.println("LLHUDEffectSpiral: unpackData (getSizeFast TypeData) not yet implemented")
            0
        }
        if (size != effectSize) {
            return
        }
        System.err.println("LLHUDEffectSpiral: unpackData (getBinaryDataFast TypeData) not yet implemented")
        System.err.println("LLHUDEffectSpiral: unpackData (htolememcpy objectId/targetObjectId/mPositionGlobal) not yet implemented")

        System.err.println("LLHUDEffectSpiral: unpackData (resolve source/target objects) not yet implemented")

        triggerLocal()
    }

    fun triggerLocal() {
        mKillTime = mTimer.getElapsedTimeF32() + mDuration

        val showBeam: Boolean = run {
            System.err.println("LLHUDEffectSpiral: triggerLocal (gSavedSettings ShowSelectionBeam) not yet implemented")
            false
        }

        val color: LLColor4 = LLColor4(mColor)

        if (mPartSourcep == null) {
            if (mTargetObject != null && mSourceObject != null) {
                if (showBeam) {
                    val psb = LLViewerPartSourceBeam()
                    psb.setColor(color)
                    psb.setSourceObject(mSourceObject)
                    psb.setTargetObject(mTargetObject)
                    System.err.println("LLHUDEffectSpiral: triggerLocal (psb.setOwnerUUID gAgent.getID) not yet implemented")
                    System.err.println("LLHUDEffectSpiral: triggerLocal (LLViewerPartSim addPartSource psb) not yet implemented")
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
                        System.err.println("LLHUDEffectSpiral: triggerLocal (psb.setOwnerUUID gAgent.getID, beam to point) not yet implemented")
                        System.err.println("LLHUDEffectSpiral: triggerLocal (LLViewerPartSim addPartSource psb, beam to point) not yet implemented")
                        mPartSourcep = psb
                    }
                } else {
                    val pos: LLVector3 = if (mSourceObject != null) {
                        mSourceObject!!.getPositionAgent()
                    } else {
                        System.err.println("LLHUDEffectSpiral: triggerLocal (gAgent.getPosAgentFromGlobal) not yet implemented")
                        LLVector3()
                    }
                    val pss = LLViewerPartSourceSpiral(pos)
                    if (mSourceObject != null) {
                        pss.setSourceObject(mSourceObject)
                    }
                    pss.setColor(color)
                    System.err.println("LLHUDEffectSpiral: triggerLocal (pss.setOwnerUUID gAgent.getID) not yet implemented")
                    System.err.println("LLHUDEffectSpiral: triggerLocal (LLViewerPartSim addPartSource pss) not yet implemented")
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

        val showSelectionBeam: Boolean = run {
            System.err.println("LLHUDEffectSpiral: render (gSavedSettings ShowSelectionBeam cached) not yet implemented")
            false
        }

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
