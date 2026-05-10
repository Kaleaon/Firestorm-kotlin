package com.firestorm.newview

import kotlin.math.*

const val FLEXIBLE_OBJECT_TIMESLICE: Float = 0.003f
const val FLEXIBLE_OBJECT_MAX_LOD: UInt = 10u
const val FLEXIBLE_OBJECT_MAX_SECTIONS: Int = 7
private const val SEC_PER_FLEXI_FRAME: Double = 1.0 / 60.0

data class LLFlexibleObjectSection(
    var scale: FloatArray = floatArrayOf(1f, 1f),
    var axisRotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f),
    var position: FloatArray = FloatArray(3),
    var velocity: FloatArray = FloatArray(3),
    var direction: FloatArray = FloatArray(3),
    var rotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f),
    var mdPosition: FloatArray = FloatArray(3)
)

class LLVolumeImplFlexible(
    private val mVO: LLViewerObject,
    private var mAttributes: LLFlexibleObjectData?
) : LLVolumeInterface() {

    private val mTimer = LLTimer()
    private var mAnchorPosition: FloatArray = FloatArray(3)
    private var mParentPosition: FloatArray = FloatArray(3)
    private var mParentRotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f)
    private var mLastFrameRotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f)
    private var mLastSegmentRotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f)
    private var mInitialized: Boolean = false
    private var mUpdated: Boolean = false
    private var mInitializedRes: Int = -1
    private var mSimulateRes: Int = 0
    private var mRenderRes: Int = -1
    private var mLastFrameNum: ULong = 0uL
    private var mLastUpdatePeriod: UInt = 0u
    private var mCollisionSpherePosition: FloatArray = FloatArray(3)
    private var mCollisionSphereRadius: Float = 0f
    private val mID: UInt
    private var mInstanceIndex: Int

    private val mSection: Array<LLFlexibleObjectSection> =
        Array((1 shl FLEXIBLE_OBJECT_MAX_SECTIONS) + 1) { LLFlexibleObjectSection() }

    init {
        mID = nextSeed++.toUInt()
        mInstanceIndex = sInstanceList.size
        sInstanceList.add(this)
        mVO.mDrawable?.makeActive()
    }

    fun destroy() {
        val endIdx = sInstanceList.size - 1
        if (endIdx != mInstanceIndex) {
            sInstanceList[mInstanceIndex] = sInstanceList[endIdx]
            sInstanceList[mInstanceIndex].mInstanceIndex = mInstanceIndex
        }
        sInstanceList.removeAt(sInstanceList.size - 1)
    }

    override fun getID(): UInt = mID

    override fun getFramePosition(): FloatArray = mVO.getRenderPosition()

    override fun getFrameRotation(): FloatArray = mVO.getRenderRotation()

    override fun getInterfaceType(): LLVolumeInterfaceType = LLVolumeInterfaceType.INTERFACE_FLEXIBLE

    override fun updateRenderRes() {
        val attrs = mAttributes ?: return
        val drawablep = mVO.mDrawable ?: return
        val newRes = attrs.getSimulateLOD()
        val appAngle = mVO.getScale()[2] / drawablep.mDistanceWRTCamera
        mRenderRes = (12f * appAngle).toInt()
        mRenderRes = mRenderRes.coerceIn(newRes - 1, FLEXIBLE_OBJECT_MAX_SECTIONS)
        val clampedRes = if (mRenderRes < newRes) mRenderRes else newRes
        if (!mInitialized || mSimulateRes != clampedRes) {
            mSimulateRes = clampedRes
            setAttributesOfAllSections()
            mInitialized = true
        }
    }

    override fun doIdleUpdate() {
        val drawablep = mVO.mDrawable ?: return
        drawablep.makeActive()
        if (TODO("GPU: pipeline.hasRenderDebugFeatureMask FEATURE_FLEXIBLE") as Boolean) {
            val visible = drawablep.isVisible()
            if (mRenderRes == -1) {
                updateRenderRes()
                TODO("GPU: markRebuild REBUILD_POSITION")
            } else {
                val pixelArea = mVO.getPixelArea()
                val screenPixelArea = TODO("GPU: LLViewerCamera.screenPixelArea") as Float
                val updatePeriod = (maxOf(
                    (screenPixelArea * 0.01f / (pixelArea * (sUpdateFactor + 1f))).toInt(), 0
                ) + 1).toUInt().coerceIn(1u, 32u)
                val virtualFrameNum = (elapsedSeconds() / SEC_PER_FLEXI_FRAME).toULong()
                if (visible) {
                    if (!drawablep.isState(LLDrawable.IN_REBUILD_Q) && pixelArea > 256f) {
                        val id: UInt = if (mVO.isRootEdit()) mID
                                       else (mVO.getParent() as? LLVOVolume)?.getVolumeInterfaceID() ?: mID
                        val throttlingDelay = (virtualFrameNum + id.toULong()) % updatePeriod.toULong()
                        if ((throttlingDelay == 0uL && mLastFrameNum < virtualFrameNum)
                            || mLastFrameNum + updatePeriod.toULong() < virtualFrameNum
                            || mLastFrameNum > virtualFrameNum
                        ) {
                            mLastFrameNum = virtualFrameNum - throttlingDelay
                            mLastUpdatePeriod = updatePeriod
                            updateRenderRes()
                            mVO.shrinkWrap()
                            TODO("GPU: markRebuild REBUILD_POSITION")
                        }
                    }
                } else {
                    mLastFrameNum = virtualFrameNum
                    mLastUpdatePeriod = updatePeriod
                }
            }
        }
    }

    override fun doUpdateGeometry(drawable: LLDrawable): Boolean {
        TODO("GPU: doUpdateGeometry for flexible volume")
    }

    override fun getPivotPosition(): FloatArray = getAnchorPosition()

    override fun onSetVolume(volumeParams: LLVolumeParams, detail: Int) {}

    override fun onSetScale(scale: FloatArray, damped: Boolean) {
        setAttributesOfAllSections(scale)
    }

    override fun onParameterChanged(paramType: Int, data: LLNetworkData?, inUse: Boolean, localOrigin: Boolean) {
        if (paramType == LLNetworkData.PARAMS_FLEXIBLE) {
            mAttributes = data as? LLFlexibleObjectData
            setAttributesOfAllSections()
        }
    }

    override fun onShift(shiftVector: FloatArray) {
        for (section in 0 until (1 shl FLEXIBLE_OBJECT_MAX_SECTIONS) + 1) {
            mSection[section].position[0] += shiftVector[0]
            mSection[section].position[1] += shiftVector[1]
            mSection[section].position[2] += shiftVector[2]
        }
    }

    override fun isVolumeUnique(): Boolean = true
    override fun isVolumeGlobal(): Boolean = true
    override fun isActive(): Boolean = true

    override fun getWorldMatrix(xform: Any): Any {
        TODO("GPU: getWorldMatrix from xform")
    }

    override fun updateRelativeXform(forceIdentity: Boolean) {
        TODO("GPU: updateRelativeXform")
    }

    fun setParentPositionAndRotationDirectly(p: FloatArray, r: FloatArray) {
        mParentPosition = p
        mParentRotation = r
    }

    fun setUsingCollisionSphere(u: Boolean) {}
    fun setCollisionSphere(position: FloatArray, radius: Float) {
        mCollisionSpherePosition = position
        mCollisionSphereRadius = radius
    }
    fun setRenderingCollisionSphere(r: Boolean) {}

    fun getEndPosition(): FloatArray = mSection[(1 shl mSimulateRes)].position.copyOf()
    fun getEndRotation(): FloatArray = mSection[(1 shl mSimulateRes)].rotation.copyOf()
    fun getNodePosition(nodeIndex: Int): FloatArray = mSection[nodeIndex].position.copyOf()
    fun getAnchorPosition(): FloatArray = mAnchorPosition.copyOf()

    fun doFlexibleUpdate() {
        val volume = mVO.getVolume() ?: return
        val path = volume.getPath()
        if ((mSimulateRes == 0 || !mInitialized) && mVO.mDrawable?.isVisible() == true) {
            val forceUpdate = mSimulateRes == 0
            doIdleUpdate()
            if (!forceUpdate || !(TODO("GPU: hasRenderDebugFeatureMask FEATURE_FLEXIBLE") as Boolean)) return
        }
        if (!mInitialized || mAttributes == null) return
        if (mRenderRes < 0) return

        val numSections = 1 shl mSimulateRes
        var secondsThisFrame = mTimer.getElapsedTimeAndResetF32()
        if (secondsThisFrame > 0.2f) secondsThisFrame = 0.2f

        val basePosition = getFramePosition()
        val baseRotation = getFrameRotation()
        var parentSegmentRotation = baseRotation.copyOf()
        val anchorDirectionRotated = rotateVector(floatArrayOf(0f, 0f, 1f), parentSegmentRotation)
        val anchorScale = mVO.mDrawable!!.getScale()

        val sectionLength = anchorScale[2] / numSections.toFloat()
        val invSectionLength = 1f / sectionLength

        val anchorPosition = floatArrayOf(
            basePosition[0] - anchorScale[2] / 2f * anchorDirectionRotated[0],
            basePosition[1] - anchorScale[2] / 2f * anchorDirectionRotated[1],
            basePosition[2] - anchorScale[2] / 2f * anchorDirectionRotated[2]
        )
        mSection[0].position = anchorPosition.copyOf()
        mSection[0].direction = anchorDirectionRotated.copyOf()
        mSection[0].rotation = baseRotation.copyOf()

        val attrs = mAttributes!!
        var tFactor = attrs.getTension() * 0.1f
        tFactor *= (1 - 0.85f.pow(secondsThisFrame * 30))
        if (tFactor > FLEXIBLE_OBJECT_MAX_INTERNAL_TENSION_FORCE)
            tFactor = FLEXIBLE_OBJECT_MAX_INTERNAL_TENSION_FORCE

        val frictionCoeff = run {
            val base = (attrs.getAirFriction() * 2 + 1)
            val v = 10f.pow(base * secondsThisFrame)
            if (v > 1f) v else 1f
        }
        val momentum = 1.0f / frictionCoeff
        val windFactor = attrs.getWindSensitivity() * 0.1f * sectionLength * secondsThisFrame
        val maxAngle = atan(sectionLength * 2f)
        val forceFactor = sectionLength * secondsThisFrame

        for (i in 1..numSections) {
            val lastPosition = mSection[i].position.copyOf()

            mSection[i].position[2] -= attrs.getGravity() * forceFactor

            if (attrs.getWindSensitivity() > 0.001f) {
                val wind = TODO("GPU: getWindVelocity at position") as FloatArray
                mSection[i].position[0] += wind[0] * windFactor
                mSection[i].position[1] += wind[1] * windFactor
                mSection[i].position[2] += wind[2] * windFactor
            }

            val userForce = attrs.getUserForce()
            mSection[i].position[0] += userForce[0] * forceFactor
            mSection[i].position[1] += userForce[1] * forceFactor
            mSection[i].position[2] += userForce[2] * forceFactor

            val parentSectionPosition = mSection[i - 1].position
            val parentDirection = mSection[i - 1].direction
            val parentSectionVector = if (i == 1) mSection[0].direction else mSection[i - 2].direction

            val currentVector = floatArrayOf(
                mSection[i].position[0] - parentSectionPosition[0],
                mSection[i].position[1] - parentSectionPosition[1],
                mSection[i].position[2] - parentSectionPosition[2]
            )
            val difference = floatArrayOf(
                parentSectionVector[0] * sectionLength - currentVector[0],
                parentSectionVector[1] * sectionLength - currentVector[1],
                parentSectionVector[2] * sectionLength - currentVector[2]
            )
            mSection[i].position[0] += difference[0] * tFactor
            mSection[i].position[1] += difference[1] * tFactor
            mSection[i].position[2] += difference[2] * tFactor

            mSection[i].position[0] += mSection[i].velocity[0] * momentum
            mSection[i].position[1] += mSection[i].velocity[1] * momentum
            mSection[i].position[2] += mSection[i].velocity[2] * momentum

            var dir = floatArrayOf(
                mSection[i].position[0] - parentSectionPosition[0],
                mSection[i].position[1] - parentSectionPosition[1],
                mSection[i].position[2] - parentSectionPosition[2]
            )
            dir = normalizeVec3(dir)
            val deltaRotation = shortestArcQuat(parentDirection, dir)
            val angleAxis = quatToAngleAxis(deltaRotation)
            var angle = angleAxis[0]
            val axis = floatArrayOf(angleAxis[1], angleAxis[2], angleAxis[3])
            if (angle > PI.toFloat()) angle -= 2f * PI.toFloat()
            if (angle < -PI.toFloat()) angle += 2f * PI.toFloat()
            val clampedAngle = angle.coerceIn(-maxAngle, maxAngle)
            val clampedDelta = angleAxisToQuat(clampedAngle, axis)
            val segmentRotation = multiplyQuat(parentSegmentRotation, clampedDelta)
            parentSegmentRotation = segmentRotation.copyOf()

            mSection[i].direction = rotateVector(parentDirection, clampedDelta)
            mSection[i].position = floatArrayOf(
                parentSectionPosition[0] + mSection[i].direction[0] * sectionLength,
                parentSectionPosition[1] + mSection[i].direction[1] * sectionLength,
                parentSectionPosition[2] + mSection[i].direction[2] * sectionLength
            )
            mSection[i].rotation = segmentRotation.copyOf()

            if (i > 1) {
                val halfDelta = angleAxisToQuat(clampedAngle / 2f, axis)
                mSection[i - 1].rotation = multiplyQuat(mSection[i - 1].rotation, halfDelta)
            }

            val velVec = floatArrayOf(
                mSection[i].position[0] - lastPosition[0],
                mSection[i].position[1] - lastPosition[1],
                mSection[i].position[2] - lastPosition[2]
            )
            val velMagSq = velVec[0]*velVec[0] + velVec[1]*velVec[1] + velVec[2]*velVec[2]
            mSection[i].velocity = if (velMagSq > 1f) normalizeVec3(velVec) else velVec
        }

        mSection[0].mdPosition = scaleVec3(
            floatArrayOf(
                mSection[1].position[0] - mSection[0].position[0],
                mSection[1].position[1] - mSection[0].position[1],
                mSection[1].position[2] - mSection[0].position[2]
            ), invSectionLength
        )
        for (i in 1 until numSections) {
            val a = scaleVec3(floatArrayOf(
                mSection[i-1].position[0] - mSection[i].position[0] + mSection[i+1].position[0] - mSection[i].position[0],
                mSection[i-1].position[1] - mSection[i].position[1] + mSection[i+1].position[1] - mSection[i].position[1],
                mSection[i-1].position[2] - mSection[i].position[2] + mSection[i+1].position[2] - mSection[i].position[2]
            ), 0.5f * invSectionLength * invSectionLength)
            val bx = (mSection[i+1].position[0] - mSection[i].position[0] - a[0] * sectionLength * sectionLength) * invSectionLength
            val by = (mSection[i+1].position[1] - mSection[i].position[1] - a[1] * sectionLength * sectionLength) * invSectionLength
            val bz = (mSection[i+1].position[2] - mSection[i].position[2] - a[2] * sectionLength * sectionLength) * invSectionLength
            mSection[i].mdPosition = floatArrayOf(bx, by, bz)
        }
        mSection[numSections].mdPosition = scaleVec3(floatArrayOf(
            mSection[numSections].position[0] - mSection[numSections-1].position[0],
            mSection[numSections].position[1] - mSection[numSections-1].position[1],
            mSection[numSections].position[2] - mSection[numSections-1].position[2]
        ), invSectionLength)

        val numRenderSections = 1 shl mRenderRes
        if (path.getPathLength() != numRenderSections + 1) {
            (mVO as? LLVOVolume)?.mVolumeChanged = true
            volume.resizePath(numRenderSections + 1)
        }

        val newSection = Array((1 shl FLEXIBLE_OBJECT_MAX_SECTIONS) + 1) { LLFlexibleObjectSection() }
        remapSections(mSection, mSimulateRes, newSection, mRenderRes)

        val deltaRot = invertQuat(getFrameRotation())
        val framePos = getFramePosition()
        val deltaPos = rotateVector(floatArrayOf(-framePos[0], -framePos[1], -framePos[2]), deltaRot)

        for (i in 0..numRenderSections) {
            val pos = rotateVector(newSection[i].position, deltaRot).let { p ->
                floatArrayOf(p[0] + deltaPos[0], p[1] + deltaPos[1], p[2] + deltaPos[2])
            }
            val rot = multiplyQuat(multiplyQuat(mSection[i].axisRotation, newSection[i].rotation), deltaRot)
            val np = path.mPath[i]
            val npPos = np.getPosition()
            val dist = sqrtf((npPos[0]-pos[0]).pow(2) + (npPos[1]-pos[1]).pow(2) + (npPos[2]-pos[2]).pow(2))
            if (!mUpdated || dist / mVO.mDrawable!!.mDistanceWRTCamera > 0.001f) {
                np.setPosition(pos)
                mUpdated = false
            }
            np.setRotation(rot)
            np.setScale(newSection[i].scale[0], newSection[i].scale[1])
            np.setTexT(i.toFloat() / numRenderSections)
        }
        mLastSegmentRotation = parentSegmentRotation.copyOf()
    }

    fun preRebuild() {
        if (!mUpdated) doFlexibleRebuild(false)
    }

    fun doFlexibleRebuild(rebuildVolume: Boolean) {
        val volume = mVO.getVolume() ?: return
        if (rebuildVolume) volume.setDirty()
        volume.regen()
        mUpdated = true
    }

    private fun setAttributesOfAllSections(inScale: FloatArray? = null) {
        var bottomScale = floatArrayOf(1f, 1f)
        var topScale = floatArrayOf(1f, 1f)
        var beginRot = 0f
        var endRot = 0f
        mVO.getVolume()?.let { vol ->
            val params = vol.getParams().getPathParams()
            bottomScale = params.getBeginScale()
            topScale = params.getEndScale()
            beginRot = PI.toFloat() * params.getTwistBegin()
            endRot = PI.toFloat() * params.getTwist()
        }
        val drawablep = mVO.mDrawable ?: return
        val scale = inScale ?: drawablep.getScale()

        mSection[0].position = getAnchorPosition()
        mSection[0].direction = rotateVector(floatArrayOf(0f, 0f, 1f), getFrameRotation())
        mSection[0].mdPosition = mSection[0].direction.copyOf()
        mSection[0].scale = floatArrayOf(scale[0] * bottomScale[0], scale[1] * bottomScale[1])
        mSection[0].velocity = FloatArray(3)
        mSection[0].axisRotation = angleAxisToQuat(beginRot, floatArrayOf(0f, 0f, 1f))

        remapSections(mSection, mInitializedRes, mSection, mSimulateRes)
        mInitializedRes = mSimulateRes

        val numSections = 1 shl mSimulateRes
        val tInc = 1f / numSections.toFloat()
        var t = tInc
        for (i in 1..numSections) {
            mSection[i].axisRotation = angleAxisToQuat(lerp(beginRot, endRot, t), floatArrayOf(0f, 0f, 1f))
            mSection[i].scale = floatArrayOf(
                scale[0] * lerp(bottomScale[0], topScale[0], t),
                scale[1] * lerp(bottomScale[1], topScale[1], t)
            )
            t += tInc
        }
    }

    private fun remapSections(
        source: Array<LLFlexibleObjectSection>, sourceSections: Int,
        dest: Array<LLFlexibleObjectSection>, destSections: Int
    ) {
        val numOutputSections = 1 shl destSections
        val scale = mVO.mDrawable!!.getScale()
        val sourceSectionLength = scale[2] / (1 shl sourceSections).toFloat()
        val sectionLength = scale[2] / numOutputSections.toFloat()
        when {
            sourceSections == -1 -> {
                dest[0] = source[0].copy()
                for (section in 0 until numOutputSections) {
                    dest[section + 1] = dest[section].copy(
                        position = floatArrayOf(
                            dest[section].position[0] + dest[section].direction[0] * sectionLength,
                            dest[section].position[1] + dest[section].direction[1] * sectionLength,
                            dest[section].position[2] + dest[section].direction[2] * sectionLength
                        ),
                        velocity = FloatArray(3)
                    )
                }
            }
            sourceSections > destSections -> {
                val numSteps = 1 shl (sourceSections - destSections)
                for (section in 0 until numOutputSections) {
                    dest[section + 1] = source[(section + 1) * numSteps].copy()
                }
                dest[0] = source[0].copy()
            }
            sourceSections < destSections -> {
                val stepShift = destSections - sourceSections
                val numSteps = 1 shl stepShift
                var section = numOutputSections - numSteps
                while (section >= 0) {
                    val lastSrc = source[section shr stepShift]
                    val src = source[(section shr stepShift) + 1]
                    val d = lastSrc.position
                    val c = scaleVec3(lastSrc.mdPosition, sourceSectionLength)
                    val y = floatArrayOf(
                        src.mdPosition[0] * sourceSectionLength - c[0],
                        src.mdPosition[1] * sourceSectionLength - c[1],
                        src.mdPosition[2] * sourceSectionLength - c[2]
                    )
                    val x = floatArrayOf(src.position[0]-d[0]-c[0], src.position[1]-d[1]-c[1], src.position[2]-d[2]-c[2])
                    val a = floatArrayOf(y[0]-2*x[0], y[1]-2*x[1], y[2]-2*x[2])
                    val b = floatArrayOf(x[0]-a[0], x[1]-a[1], x[2]-a[2])
                    val tIncr = 1f / numSteps.toFloat()
                    var tt = tIncr
                    for (step in 1 until numSteps) {
                        val tSq = tt * tt
                        dest[section + step] = LLFlexibleObjectSection(
                            scale = floatArrayOf(
                                lerp(lastSrc.scale[0], src.scale[0], tt),
                                lerp(lastSrc.scale[1], src.scale[1], tt)
                            ),
                            axisRotation = slerpQuat(tt, lastSrc.axisRotation, src.axisRotation),
                            position = floatArrayOf(
                                tSq*(tt*a[0]+b[0]) + tt*c[0] + d[0],
                                tSq*(tt*a[1]+b[1]) + tt*c[1] + d[1],
                                tSq*(tt*a[2]+b[2]) + tt*c[2] + d[2]
                            ),
                            rotation = slerpQuat(tt, lastSrc.rotation, src.rotation),
                            velocity = floatArrayOf(
                                lerp(lastSrc.velocity[0], src.velocity[0], tt),
                                lerp(lastSrc.velocity[1], src.velocity[1], tt),
                                lerp(lastSrc.velocity[2], src.velocity[2], tt)
                            ),
                            direction = floatArrayOf(
                                lerp(lastSrc.direction[0], src.direction[0], tt),
                                lerp(lastSrc.direction[1], src.direction[1], tt),
                                lerp(lastSrc.direction[2], src.direction[2], tt)
                            ),
                            mdPosition = floatArrayOf(
                                lerp(lastSrc.mdPosition[0], src.mdPosition[0], tt),
                                lerp(lastSrc.mdPosition[1], src.mdPosition[1], tt),
                                lerp(lastSrc.mdPosition[2], src.mdPosition[2], tt)
                            )
                        )
                        dest[section + numSteps] = src.copy()
                        tt += tIncr
                    }
                    section -= numSteps
                }
                dest[0] = source[0].copy()
            }
            else -> {
                for (section in 0..numOutputSections) dest[section] = source[section].copy()
            }
        }
    }

    companion object {
        val sInstanceList: MutableList<LLVolumeImplFlexible> = mutableListOf()
        var sUpdateFactor: Float = 1.0f
        private var nextSeed: Int = 0

        fun updateClass() {
            val virtualFrameNum = (elapsedSeconds() / SEC_PER_FLEXI_FRAME).toULong()
            for (instance in sInstanceList) {
                if (instance.mRenderRes == -1
                    || instance.mLastFrameNum + instance.mLastUpdatePeriod.toULong() <= virtualFrameNum
                    || instance.mLastFrameNum > virtualFrameNum
                ) {
                    instance.doIdleUpdate()
                }
            }
        }
    }
}

private const val FLEXIBLE_OBJECT_MAX_INTERNAL_TENSION_FORCE: Float = 0.3f

private fun elapsedSeconds(): Double = TODO("APR: use JVM equivalent for LLTimer::getElapsedSeconds")
private fun normalizeVec3(v: FloatArray): FloatArray {
    val len = sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]).toFloat()
    return if (len > 0f) floatArrayOf(v[0]/len, v[1]/len, v[2]/len) else v.copyOf()
}
private fun scaleVec3(v: FloatArray, s: Float): FloatArray = floatArrayOf(v[0]*s, v[1]*s, v[2]*s)
private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
private fun rotateVector(v: FloatArray, q: FloatArray): FloatArray = TODO("GPU: quaternion rotate vector")
private fun multiplyQuat(a: FloatArray, b: FloatArray): FloatArray = TODO("GPU: quaternion multiply")
private fun invertQuat(q: FloatArray): FloatArray = TODO("GPU: quaternion invert/conjugate")
private fun shortestArcQuat(from: FloatArray, to: FloatArray): FloatArray = TODO("GPU: shortestArcQuat")
private fun quatToAngleAxis(q: FloatArray): FloatArray = TODO("GPU: quaternion to angle-axis [angle, x, y, z]")
private fun angleAxisToQuat(angle: Float, axis: FloatArray): FloatArray = TODO("GPU: angle-axis to quaternion")
private fun slerpQuat(t: Float, a: FloatArray, b: FloatArray): FloatArray = TODO("GPU: quaternion slerp")
private fun sqrtf(v: Float): Float = sqrt(v.toDouble()).toFloat()
