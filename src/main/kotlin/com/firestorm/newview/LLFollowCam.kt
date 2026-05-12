package com.firestorm.newview

import java.util.UUID

// Constants governing camera dynamics
private const val FOLLOW_CAM_ZOOM_FACTOR: Float = 0.1f
private const val FOLLOW_CAM_MIN_ZOOM_AMOUNT: Float = 0.1f
private const val DISTANCE_EPSILON: Float = 0.0001f
private const val DEFAULT_MAX_DISTANCE_FROM_SUBJECT: Float = 1000.0f

private const val FOLLOW_CAM_MIN_POSITION_LAG: Float = 0.0f
private const val FOLLOW_CAM_DEFAULT_POSITION_LAG: Float = 0.1f
private const val FOLLOW_CAM_MAX_POSITION_LAG: Float = 3.0f

private const val FOLLOW_CAM_MIN_FOCUS_LAG: Float = 0.0f
private const val FOLLOW_CAM_DEFAULT_FOCUS_LAG: Float = 0.1f
private const val FOLLOW_CAM_MAX_FOCUS_LAG: Float = 3.0f

private const val FOLLOW_CAM_MIN_POSITION_THRESHOLD: Float = 0.0f
private const val FOLLOW_CAM_DEFAULT_POSITION_THRESHOLD: Float = 1.0f
private const val FOLLOW_CAM_MAX_POSITION_THRESHOLD: Float = 4.0f

private const val FOLLOW_CAM_MIN_FOCUS_THRESHOLD: Float = 0.0f
private const val FOLLOW_CAM_DEFAULT_FOCUS_THRESHOLD: Float = 1.0f
private const val FOLLOW_CAM_MAX_FOCUS_THRESHOLD: Float = 4.0f

private const val FOLLOW_CAM_MIN_DISTANCE: Float = 0.5f
private const val FOLLOW_CAM_DEFAULT_DISTANCE: Float = 3.0f

private const val FOLLOW_CAM_MIN_PITCH: Float = -45.0f
private const val FOLLOW_CAM_DEFAULT_PITCH: Float = 0.0f
private const val FOLLOW_CAM_MAX_PITCH: Float = 80.0f

private const val FOLLOW_CAM_MIN_FOCUS_OFFSET: Float = -10.0f
private const val FOLLOW_CAM_MAX_FOCUS_OFFSET: Float = 10.0f

// Default focus offset: (1, 0, 0) in world-space
private val FOLLOW_CAM_DEFAULT_FOCUS_OFFSET = FloatArray(3) { if (it == 0) 1.0f else 0.0f }

private const val FOLLOW_CAM_MIN_BEHINDNESS_LAG: Float = 0.0f
private const val FOLLOW_CAM_DEFAULT_BEHINDNESS_LAG: Float = 0.0f
private const val FOLLOW_CAM_MAX_BEHINDNESS_LAG: Float = 3.0f

private const val FOLLOW_CAM_MIN_BEHINDNESS_ANGLE: Float = 0.0f
private const val FOLLOW_CAM_DEFAULT_BEHINDNESS_ANGLE: Float = 10.0f
private const val FOLLOW_CAM_MAX_BEHINDNESS_ANGLE: Float = 180.0f
private const val FOLLOW_CAM_BEHINDNESS_EPSILON: Float = 1.0f

private fun clamp(value: Float, min: Float, max: Float): Float =
    if (value < min) min else if (value > max) max else value

open class LLFollowCamParams {

    protected var mPositionLag: Float = 0.0f
    protected var mFocusLag: Float = 0.0f
    protected var mFocusThreshold: Float = 0.0f
    protected var mPositionThreshold: Float = 0.0f
    protected var mDistance: Float = 0.0f
    protected var mPitch: Float = 0.0f
    // focusOffset stored as [x, y, z]
    protected var mFocusOffset: FloatArray = FloatArray(3)
    protected var mBehindnessMaxAngle: Float = 0.0f
    protected var mBehindnessLag: Float = 0.0f
    protected var mMaxCameraDistantFromSubject: Float = DEFAULT_MAX_DISTANCE_FROM_SUBJECT

    protected var mPositionLocked: Boolean = false
    protected var mFocusLocked: Boolean = false
    protected var mUsePosition: Boolean = false
    protected var mUseFocus: Boolean = false
    // position/focus stored as [x, y, z] world-space vectors
    protected var mPosition: FloatArray = FloatArray(3)
    protected var mFocus: FloatArray = FloatArray(3)

    init {
        mMaxCameraDistantFromSubject = DEFAULT_MAX_DISTANCE_FROM_SUBJECT
        mPositionLocked = false
        mFocusLocked = false
        mUsePosition = false
        mUseFocus = false

        setPositionLag(FOLLOW_CAM_DEFAULT_POSITION_LAG)
        setFocusLag(FOLLOW_CAM_DEFAULT_FOCUS_LAG)
        setPositionThreshold(FOLLOW_CAM_DEFAULT_POSITION_THRESHOLD)
        setFocusThreshold(FOLLOW_CAM_DEFAULT_FOCUS_THRESHOLD)
        setBehindnessLag(FOLLOW_CAM_DEFAULT_BEHINDNESS_LAG)
        setDistance(FOLLOW_CAM_DEFAULT_DISTANCE)
        setPitch(FOLLOW_CAM_DEFAULT_PITCH)
        setFocusOffset(FOLLOW_CAM_DEFAULT_FOCUS_OFFSET)
        setBehindnessAngle(FOLLOW_CAM_DEFAULT_BEHINDNESS_ANGLE)
        setPositionThreshold(FOLLOW_CAM_DEFAULT_POSITION_THRESHOLD)
        setFocusThreshold(FOLLOW_CAM_DEFAULT_FOCUS_THRESHOLD)
    }

    open fun setPositionLag(p: Float) {
        mPositionLag = clamp(p, FOLLOW_CAM_MIN_POSITION_LAG, FOLLOW_CAM_MAX_POSITION_LAG)
    }

    open fun setFocusLag(f: Float) {
        mFocusLag = clamp(f, FOLLOW_CAM_MIN_FOCUS_LAG, FOLLOW_CAM_MAX_FOCUS_LAG)
    }

    open fun setPositionThreshold(p: Float) {
        mPositionThreshold = clamp(p, FOLLOW_CAM_MIN_POSITION_THRESHOLD, FOLLOW_CAM_MAX_POSITION_THRESHOLD)
    }

    open fun setFocusThreshold(f: Float) {
        mFocusThreshold = clamp(f, FOLLOW_CAM_MIN_FOCUS_THRESHOLD, FOLLOW_CAM_MAX_FOCUS_THRESHOLD)
    }

    open fun setPitch(p: Float) {
        mPitch = clamp(p, FOLLOW_CAM_MIN_PITCH, FOLLOW_CAM_MAX_PITCH)
    }

    open fun setBehindnessLag(b: Float) {
        mBehindnessLag = clamp(b, FOLLOW_CAM_MIN_BEHINDNESS_LAG, FOLLOW_CAM_MAX_BEHINDNESS_LAG)
    }

    open fun setBehindnessAngle(b: Float) {
        mBehindnessMaxAngle = clamp(b, FOLLOW_CAM_MIN_BEHINDNESS_ANGLE, FOLLOW_CAM_MAX_BEHINDNESS_ANGLE)
    }

    open fun setDistance(d: Float) {
        mDistance = clamp(d, FOLLOW_CAM_MIN_DISTANCE, mMaxCameraDistantFromSubject)
    }

    open fun setPositionLocked(l: Boolean) {
        mPositionLocked = l
    }

    open fun setFocusLocked(l: Boolean) {
        mFocusLocked = l
    }

    open fun setFocusOffset(v: FloatArray) {
        mFocusOffset = floatArrayOf(
            clamp(v[0], FOLLOW_CAM_MIN_FOCUS_OFFSET, FOLLOW_CAM_MAX_FOCUS_OFFSET),
            clamp(v[1], FOLLOW_CAM_MIN_FOCUS_OFFSET, FOLLOW_CAM_MAX_FOCUS_OFFSET),
            clamp(v[2], FOLLOW_CAM_MIN_FOCUS_OFFSET, FOLLOW_CAM_MAX_FOCUS_OFFSET)
        )
    }

    open fun setPosition(p: FloatArray) {
        mUsePosition = true
        mPosition = p.copyOf()
    }

    open fun setFocus(f: FloatArray) {
        mUseFocus = true
        mFocus = f.copyOf()
    }

    open fun getPositionLag(): Float = mPositionLag
    open fun getFocusLag(): Float = mFocusLag
    open fun getPositionThreshold(): Float = mPositionThreshold
    open fun getFocusThreshold(): Float = mFocusThreshold
    open fun getDistance(): Float = mDistance
    open fun getPitch(): Float = mPitch
    open fun getFocusOffset(): FloatArray = mFocusOffset
    open fun getBehindnessAngle(): Float = mBehindnessMaxAngle
    open fun getBehindnessLag(): Float = mBehindnessLag
    open fun getPosition(): FloatArray = mPosition
    open fun getFocus(): FloatArray = mFocus
    open fun getPositionLocked(): Boolean = mPositionLocked
    open fun getFocusLocked(): Boolean = mFocusLocked
    open fun getUseFocus(): Boolean = mUseFocus
    open fun getUsePosition(): Boolean = mUsePosition
}

open class LLFollowCam : LLFollowCamParams() {

    protected var mPitchCos: Float = 0.0f
    protected var mPitchSin: Float = 0.0f

    // Global-coordinate simulated position/focus (stored as [x, y, z] doubles)
    protected var mSimulatedPositionGlobal: DoubleArray = DoubleArray(3)
    protected var mSimulatedFocusGlobal: DoubleArray = DoubleArray(3)
    protected var mSimulatedDistance: Float = 0.0f

    protected var mZoomedToMinimumDistance: Boolean = false
    protected var mSubjectPosition: FloatArray = FloatArray(3)
    // Subject rotation represented as a quaternion [x, y, z, w]
    protected var mSubjectRotation: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
    protected var mUpVector: FloatArray = floatArrayOf(0.0f, 0.0f, 1.0f)
    protected var mRelativeFocus: FloatArray = FloatArray(3)
    protected var mRelativePos: FloatArray = FloatArray(3)

    protected var mPitchSineAndCosineNeedToBeUpdated: Boolean = true

    init {
        mUpVector = floatArrayOf(0.0f, 0.0f, 1.0f)
        mSubjectPosition = FloatArray(3)
        mSubjectRotation = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
        mZoomedToMinimumDistance = false
        mPitchCos = 0.0f
        mPitchSin = 0.0f
        mPitchSineAndCosineNeedToBeUpdated = true
        mSimulatedDistance = mDistance
    }

    fun copyParams(params: LLFollowCamParams) {
        setPositionLag(params.getPositionLag())
        setFocusLag(params.getFocusLag())
        setFocusThreshold(params.getFocusThreshold())
        setPositionThreshold(params.getPositionThreshold())
        setPitch(params.getPitch())
        setFocusOffset(params.getFocusOffset())
        setBehindnessAngle(params.getBehindnessAngle())
        setBehindnessLag(params.getBehindnessLag())
        setPositionLocked(params.getPositionLocked())
        setFocusLocked(params.getFocusLocked())
        setDistance(params.getDistance())
        if (params.getUsePosition()) {
            setPosition(params.getPosition())
        }
        if (params.getUseFocus()) {
            setFocus(params.getFocus())
        }
    }

    fun setSubjectPositionAndRotation(p: FloatArray, r: FloatArray) {
        mSubjectPosition = p.copyOf()
        mSubjectRotation = r.copyOf()
    }

    fun update() {
        TODO("GPU: update follow-cam simulation using agent position/rotation transforms")
    }

    fun reset(position: FloatArray, focus: FloatArray, upVector: FloatArray) {
        setPosition(position)
        setFocus(focus)
        mUpVector = upVector.copyOf()
    }

    fun setMaxCameraDistantFromSubject(m: Float) {
        mMaxCameraDistantFromSubject = m
    }

    fun isZoomedToMinimumDistance(): Boolean = mZoomedToMinimumDistance

    fun getUpVector(): FloatArray = mUpVector

    fun zoom(z: Int) {
        var zoomAmount = z * mSimulatedDistance * FOLLOW_CAM_ZOOM_FACTOR
        if (zoomAmount < FOLLOW_CAM_MIN_ZOOM_AMOUNT && zoomAmount > -FOLLOW_CAM_MIN_ZOOM_AMOUNT) {
            zoomAmount = if (zoomAmount < 0.0f) -FOLLOW_CAM_MIN_ZOOM_AMOUNT else FOLLOW_CAM_MIN_ZOOM_AMOUNT
        }
        mSimulatedDistance += zoomAmount
        mZoomedToMinimumDistance = false
        when {
            mSimulatedDistance < FOLLOW_CAM_MIN_DISTANCE -> {
                mSimulatedDistance = FOLLOW_CAM_MIN_DISTANCE
                if (zoomAmount < 0.0f) {
                    mZoomedToMinimumDistance = true
                }
            }
            mSimulatedDistance > mMaxCameraDistantFromSubject -> {
                mSimulatedDistance = mMaxCameraDistantFromSubject
            }
        }
    }

    override fun setPitch(p: Float) {
        super.setPitch(p)
        mPitchSineAndCosineNeedToBeUpdated = true
    }

    override fun setDistance(d: Float) {
        if (d != mDistance) {
            super.setDistance(d)
            mSimulatedDistance = d
            mZoomedToMinimumDistance = false
        }
    }

    override fun setPosition(p: FloatArray) {
        if (!p.contentEquals(mPosition)) {
            super.setPosition(p)
            TODO("GPU: convert mPosition to global coords via agent transform for mSimulatedPositionGlobal")
        }
    }

    override fun setFocus(f: FloatArray) {
        if (!f.contentEquals(mFocus)) {
            super.setFocus(f)
            TODO("GPU: convert focus to global coords via agent transform for mSimulatedFocusGlobal")
        }
    }

    override fun setPositionLocked(locked: Boolean) {
        super.setPositionLocked(locked)
        if (locked) {
            TODO("GPU: propagate simulated position to mRelativePos using subject rotation inverse")
        }
    }

    override fun setFocusLocked(locked: Boolean) {
        super.setFocusLocked(locked)
        if (locked) {
            TODO("GPU: propagate simulated focus to mRelativeFocus using subject rotation inverse")
        }
    }

    fun getSimulatedPosition(): FloatArray {
        TODO("GPU: return mSubjectPosition + (mRelativePos rotated by mSubjectRotation)")
    }

    fun getSimulatedFocus(): FloatArray {
        TODO("GPU: return mSubjectPosition + (mRelativeFocus rotated by mSubjectRotation)")
    }

    protected fun calculatePitchSineAndCosine() {
        val radian = mPitch * (Math.PI / 180.0).toFloat()
        mPitchCos = Math.cos(radian.toDouble()).toFloat()
        mPitchSin = Math.sin(radian.toDouble()).toFloat()
    }

    protected fun updateBehindnessConstraint(focus: FloatArray, camPosition: FloatArray): Boolean {
        TODO("GPU: apply behindness angle constraint using subject rotation and quaternion slerp")
    }
}

object LLFollowCamMgr {

    private val mParamMap: MutableMap<UUID, LLFollowCamParams> = mutableMapOf()
    private val mParamStack: MutableList<LLFollowCamParams> = mutableListOf()

    fun setPositionLag(source: UUID, lag: Float) {
        getParamsForID(source).setPositionLag(lag)
    }

    fun setFocusLag(source: UUID, lag: Float) {
        getParamsForID(source).setFocusLag(lag)
    }

    fun setFocusThreshold(source: UUID, threshold: Float) {
        getParamsForID(source).setFocusThreshold(threshold)
    }

    fun setPositionThreshold(source: UUID, threshold: Float) {
        getParamsForID(source).setPositionThreshold(threshold)
    }

    fun setDistance(source: UUID, distance: Float) {
        getParamsForID(source).setDistance(distance)
    }

    fun setPitch(source: UUID, pitch: Float) {
        getParamsForID(source).setPitch(pitch)
    }

    fun setFocusOffset(source: UUID, offset: FloatArray) {
        getParamsForID(source).setFocusOffset(offset)
    }

    fun setBehindnessAngle(source: UUID, angle: Float) {
        getParamsForID(source).setBehindnessAngle(angle)
    }

    fun setBehindnessLag(source: UUID, lag: Float) {
        getParamsForID(source).setBehindnessLag(lag)
    }

    fun setPosition(source: UUID, position: FloatArray) {
        getParamsForID(source).setPosition(position)
    }

    fun setFocus(source: UUID, focus: FloatArray) {
        getParamsForID(source).setFocus(focus)
    }

    fun setPositionLocked(source: UUID, locked: Boolean) {
        getParamsForID(source).setPositionLocked(locked)
    }

    fun setFocusLocked(source: UUID, locked: Boolean) {
        getParamsForID(source).setFocusLocked(locked)
    }

    fun setCameraActive(source: UUID, active: Boolean) {
        val params = getParamsForID(source)
        mParamStack.remove(params)
        // PermissionsTracker integration omitted — no JVM equivalent yet
        if (active) {
            mParamStack.add(params)
        }
    }

    fun getActiveFollowCamParams(): LLFollowCamParams? =
        if (mParamStack.isEmpty()) null else mParamStack.last()

    fun getParamsForID(source: UUID): LLFollowCamParams =
        mParamMap.getOrPut(source) { LLFollowCamParams() }

    fun removeFollowCamParams(source: UUID) {
        setCameraActive(source, false)
        val params = mParamMap.remove(source)
        // params no longer referenced; GC handles deallocation
    }

    fun isScriptedCameraSource(source: UUID): Boolean =
        mParamMap.containsKey(source)

    fun dump() {
        var paramCount = 0
        println("Scripted camera active stack")
        for (params in mParamStack) {
            println(
                "${paramCount++}" +
                " rot_limit: ${params.getBehindnessAngle()}" +
                " rot_lag: ${params.getBehindnessLag()}" +
                " distance: ${params.getDistance()}" +
                " focus: ${params.getFocus().contentToString()}" +
                " foc_lag: ${params.getFocusLag()}" +
                " foc_lock: ${if (params.getFocusLocked()) "Y" else "N"}" +
                " foc_offset: ${params.getFocusOffset().contentToString()}" +
                " foc_thresh: ${params.getFocusThreshold()}" +
                " pitch: ${params.getPitch()}" +
                " pos: ${params.getPosition().contentToString()}" +
                " pos_lag: ${params.getPositionLag()}" +
                " pos_lock: ${if (params.getPositionLocked()) "Y" else "N"}" +
                " pos_thresh: ${params.getPositionThreshold()}"
            )
        }
    }
}
