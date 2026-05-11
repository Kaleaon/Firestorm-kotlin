package com.firestorm.newview

import kotlin.math.pow

class LowPassFilter {
    private var mFiltered: FloatArray = FloatArray(3)
    private var mInitialized: Boolean = false

    fun update(value: FloatArray, secs: Float) {
        val smoothingTime = getSmoothingTime()
        if (!mInitialized) {
            mFiltered = value.copyOf()
            mInitialized = true
            return
        }

        var filterMult = 1.0f
        if (smoothingTime > 1e-6f) {
            // Scale factor: 1 - 1/((1 + 1/smoothingTime)^secs); weights samples proportional to time covered
            filterMult = 1.0f - 1.0f / (1.0f + 1.0f / smoothingTime).pow(secs)
        }
        for (i in mFiltered.indices) {
            mFiltered[i] = value[i] * filterMult + mFiltered[i] * (1.0f - filterMult)
        }
    }

    fun get(): FloatArray = mFiltered

    fun clear() {
        mInitialized = false
    }

    private fun getSmoothingTime(): Float {
        TODO("APR: use JVM equivalent — read FSRegionCrossingSmoothingTime from saved settings")
    }
}

class RegionCrossExtrapolateImpl(private val owner: LLViewerObject) {
    private var mPreviousUpdateTime: Double = 0.0
    private val mFilteredVel = LowPassFilter()
    private val mFilteredAngVel = LowPassFilter()
    private var mMoved: Boolean = false

    fun update() {
        val rawVel = owner.getVelocity()
        if (!mMoved) {
            if (rawVel[0] == 0f && rawVel[1] == 0f && rawVel[2] == 0f) {
                return
            }
            mMoved = true
        }

        val dt: Double
        val now = getElapsedSeconds()
        dt = if (mPreviousUpdateTime != 0.0) {
            now - mPreviousUpdateTime
        } else {
            1.0 / 45.0  // one physics frame on first sample
        }
        mPreviousUpdateTime = now

        val rot = owner.getRotationRegion()
        val inverseRot = conjugate(rot)
        val vel = multiplyVecQuat(owner.getVelocity(), inverseRot)
        val angVel = multiplyVecQuat(owner.getAngularVelocity(), inverseRot)
        mFilteredVel.update(vel, dt.toFloat())
        mFilteredAngVel.update(angVel, dt.toFloat())
    }

    fun getExtrapTimeLimit(): Float {
        val posErrorLimit = getPositionErrorLimit()
        val angleErrorLimit = getAngleErrorLimit()

        val rot = owner.getRotationRegion()
        val inverseRot = conjugate(rot)

        val velDiff = vecLength(vecSub(multiplyVecQuat(owner.getVelocity(), inverseRot), mFilteredVel.get()))
        val angVelDiff = vecLength(vecSub(multiplyVecQuat(owner.getAngularVelocity(), inverseRot), mFilteredAngVel.get()))

        return minOf(divideSafe(posErrorLimit, velDiff), divideSafe(angleErrorLimit, angVelDiff))
    }

    fun hasMoved(): Boolean = mMoved

    private fun divideSafe(num: Float, denom: Float): Float {
        val threshold = 1e-6f
        return if (denom > threshold || denom < -threshold) num / denom else Float.POSITIVE_INFINITY
    }

    private fun getElapsedSeconds(): Double {
        TODO("APR: use JVM equivalent — return wall-clock elapsed seconds (e.g. System.nanoTime() / 1e9)")
    }

    private fun getPositionErrorLimit(): Float {
        TODO("APR: use JVM equivalent — read FSRegionCrossingPositionErrorLimit from saved settings")
    }

    private fun getAngleErrorLimit(): Float {
        TODO("APR: use JVM equivalent — read FSRegionCrossingAngleErrorLimit from saved settings")
    }

    private fun conjugate(q: FloatArray): FloatArray = floatArrayOf(-q[0], -q[1], -q[2], q[3])

    private fun multiplyVecQuat(v: FloatArray, q: FloatArray): FloatArray {
        TODO("GPU: transform vector by quaternion rotation")
    }

    private fun vecSub(a: FloatArray, b: FloatArray): FloatArray =
        floatArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])

    private fun vecLength(v: FloatArray): Float =
        kotlin.math.sqrt((v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble()).toFloat()
}

class RegionCrossExtrapolate {
    private var mImpl: RegionCrossExtrapolateImpl? = null

    fun update(vo: LLViewerObject) {
        mImpl?.update()
    }

    fun changedLink(vo: LLViewerObject) {
        if (ifSatOn(vo)) {
            if (mImpl == null) {
                mImpl = RegionCrossExtrapolateImpl(vo)
            }
        } else {
            mImpl = null
        }
    }

    fun isMovingAndSatOn(vo: LLViewerObject): Boolean {
        return mImpl?.hasMoved() ?: false
    }

    fun getExtrapTimeLimit(): Float {
        return mImpl?.getExtrapTimeLimit() ?: Float.POSITIVE_INFINITY
    }

    protected fun ifSatOn(vo: LLViewerObject): Boolean {
        if (!vo.isRoot()) return false
        return vo.getChildren().any { it.isAvatar() }
    }
}

// Stub — host code must supply real implementations backed by LLViewerObject.
abstract class LLViewerObject {
    abstract fun getVelocity(): FloatArray
    abstract fun getAngularVelocity(): FloatArray
    abstract fun getRotationRegion(): FloatArray
    abstract fun isRoot(): Boolean
    abstract fun isAvatar(): Boolean
    abstract fun getChildren(): List<LLViewerObject>
    abstract fun getId(): Any
}
