package com.firestorm.newview

enum class EPoserReferenceFrame {
    POSER_FRAME_BONE,
    POSER_FRAME_WORLD,
    POSER_FRAME_AVATAR,
    POSER_FRAME_CAMERA,
}

private const val AXIS_ONTO_CAM_TOLERANCE: Float = 0.08716f  // cos(85 deg)
private const val RADIUS_PIXELS: Float = 100f
private const val CIRCLE_STEPS: Int = 100
private const val CIRCLE_STEP_SIZE: Float = (2.0 * Math.PI / CIRCLE_STEPS).toFloat()
private const val SQ_RADIUS: Float = RADIUS_PIXELS * RADIUS_PIXELS
private const val WIDTH_PIXELS: Float = 8f
private const val MAX_MANIP_SELECT_DISTANCE: Float = 100f
private const val SNAP_ANGLE_INCREMENT: Float = 5.625f
private const val SNAP_GUIDE_RADIUS_1: Float = 2.8f
private const val SNAP_GUIDE_RADIUS_2: Float = 2.4f
private const val SNAP_GUIDE_RADIUS_3: Float = 2.2f
private const val SNAP_GUIDE_RADIUS_4: Float = 2.1f
private const val SNAP_GUIDE_RADIUS_5: Float = 2.05f
private const val SNAP_GUIDE_INNER_RADIUS: Float = 2f
private const val SELECTED_MANIPULATOR_SCALE: Float = 1.05f
private const val MANIPULATOR_SCALE_HALF_LIFE: Float = 0.07f
private const val VERTICAL_OFFSET: Int = 100

open class FSManipRotateJoint(composite: Any?) {

    private data class BoneAxes(
        val naturalX: LLVector3 = LLVector3.ZERO,
        val naturalY: LLVector3 = LLVector3.ZERO,
        val naturalZ: LLVector3 = LLVector3.ZERO,
    )

    data class RingRenderParams(
        val part: Int,
        val targetScaleX: Float, val targetScaleY: Float, val targetScaleZ: Float, val targetScaleW: Float,
        val extraRotateAngle: Float,
        val extraRotateAxisX: Float, val extraRotateAxisY: Float, val extraRotateAxisZ: Float,
        val primaryColorR: Float, val primaryColorG: Float, val primaryColorB: Float, val primaryColorA: Float,
        val secondaryColorR: Float, val secondaryColorG: Float, val secondaryColorB: Float, val secondaryColorA: Float,
        val scaleIndex: Int,
    )

    protected var joint: LLJoint? = null
    protected var avatar: Any? = null
    protected var savedJointRot: LLQuaternion = LLQuaternion.DEFAULT
    protected var highlightedJoint: LLJoint? = null
    protected var highlightedPartDistance: Float = 0f
    protected var lastEuler: LLVector3 = LLVector3.ZERO
    protected var initialIntersection: LLVector3 = LLVector3.ZERO
    protected var constraintAxis: LLVector3 = LLVector3.ZERO
    protected var referenceFrame: EPoserReferenceFrame = EPoserReferenceFrame.POSER_FRAME_BONE
    protected var lastAngle: Float = 0f
    protected var lastSetRotation: LLQuaternion = LLQuaternion.DEFAULT

    private var boneAxes: BoneAxes = BoneAxes()
    private var naturalAlignmentQuat: LLQuaternion = LLQuaternion.DEFAULT

    companion object {
        private val referenceUpVectors: MutableMap<String, LLVector3> = mutableMapOf()

        val selectableJoints: List<String> = listOf(
            "mHead", "mNeck", "mPelvis", "mChest", "mTorso",
            "mCollarLeft", "mShoulderLeft", "mElbowLeft", "mWristLeft",
            "mCollarRight", "mShoulderRight", "mElbowRight", "mWristRight",
            "mHipLeft", "mKneeLeft", "mAnkleLeft",
            "mHipRight", "mKneeRight", "mAnkleRight",
        )

        fun getManipPartString(part: Int): String = when (part) {
            LL_NO_PART        -> "None"
            LL_ROT_GENERAL    -> "Rotate General"
            LL_ROT_X          -> "Rotate X"
            LL_ROT_Y          -> "Rotate Y"
            LL_ROT_Z          -> "Rotate Z"
            LL_ROT_ROLL       -> "Rotate Roll"
            else              -> "Unknown"
        }

        const val LL_NO_PART     = 0
        const val LL_ROT_GENERAL = 30
        const val LL_ROT_X       = 31
        const val LL_ROT_Y       = 32
        const val LL_ROT_Z       = 33
        const val LL_ROT_ROLL    = 34
    }

    fun setJoint(joint: LLJoint?) {
        this.joint = joint ?: return
        savedJointRot = getSelectedJointWorldRotation()
        boneAxes = computeBoneAxes()
        naturalAlignmentQuat = computeAlignmentQuat(boneAxes)
    }

    fun setAvatar(avatar: Any?) {
        this.avatar = avatar
        if (avatar == null) this.joint = null
        if (this.avatar != null && this.joint != null) {
            setJoint(getAvatarJointByNumber(avatar, this.joint!!.getJointNum()))
        }
    }

    fun setReferenceFrame(frame: EPoserReferenceFrame) { referenceFrame = frame }

    open fun handleSelect() {
        if (joint != null) savedJointRot = getSelectedJointWorldRotation()
    }

    open fun updateVisibility(): Boolean {
        if (!isAvatarJointSafeToUse()) return false
        TODO("GPU: project joint world position to screen, update mRotationCenter, mRadiusMeters, mCenterToCamNorm etc.")
    }

    open fun render() {
        if (!isAvatarJointSafeToUse()) return
        TODO("GPU: render pulsing/static joint spheres, axes, and manipulator rings using OpenGL")
    }

    fun renderNameXYZ(rot: LLQuaternion) {
        TODO("GPU: render Euler angles, joint name, and manip part as a 2D text overlay using OpenGL")
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (!isAvatarJointSafeToUse()) return false
        highlightManipulators(x, y)
        if (getCurrentHighlightedPart() == LL_NO_PART) return false
        TODO("GPU: compute sphere intersection, save savedJointRot, capture mouse")
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        return if (hasMouseCapture()) {
            releaseMouseCapture()
            setManipPart(LL_NO_PART)
            lastAngle = 0f
            true
        } else if (highlightedJoint != null) {
            selectJointByName(highlightedJoint!!.getName())
            true
        } else false
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture() && joint != null) {
            drag(x, y)
        } else {
            highlightManipulators(x, y)
        }
        return true
    }

    open fun handleMouseDownOnPart(x: Int, y: Int, mask: Int): Boolean {
        if (!isAvatarJointSafeToUse()) return false
        highlightManipulators(x, y)
        savedJointRot = getSelectedJointWorldRotation()
        lastSetRotation = LLQuaternion.DEFAULT
        val hitPart = getCurrentHighlightedPart()
        setManipPart(hitPart)

        if (hitPart == LL_ROT_GENERAL) {
            TODO("GPU: intersect mouse with sphere for unconstrained rotation drag setup")
        } else {
            val axis = setConstraintAxis()
            lastEuler = LLVector3.ZERO
            TODO("GPU: compute constrained drag plane intersection point from axis and mouse position")
        }
        setMouseCapture(true)
        return true
    }

    open fun highlightManipulators(x: Int, y: Int) {
        clearHighlightedPart()
        if (!isAvatarJointSafeToUse()) {
            highlightHoverSpheres(x, y)
            return
        }
        TODO("GPU: compute ring axis projections, test distance thresholds, update mHighlightedPart and cursor")
    }

    open fun drag(x: Int, y: Int) {
        if (!updateVisibility()) return
        val deltaRot = if (getCurrentManipPart() == LL_ROT_GENERAL)
            dragUnconstrained(x, y)
        else
            dragConstrained(x, y)

        var deltaSend = deltaRot * lastSetRotation.conjugate()
        lastSetRotation = deltaRot
        deltaSend = savedJointRot * deltaSend * savedJointRot.conjugate()

        when (referenceFrame) {
            EPoserReferenceFrame.POSER_FRAME_CAMERA,
            EPoserReferenceFrame.POSER_FRAME_AVATAR -> deltaSend = deltaSend.conjugate()
            EPoserReferenceFrame.POSER_FRAME_WORLD  -> deltaSend = deltaSend.copy(x = -deltaSend.x)
            else -> {}
        }

        notifyPosedBoneUpdate(joint?.getName() ?: return, deltaSend)
    }

    protected open fun dragUnconstrained(x: Int, y: Int): LLQuaternion {
        if (!isAvatarJointSafeToUse()) return LLQuaternion.DEFAULT
        TODO("GPU: compute sphere arc rotation from initial to current mouse intersection")
    }

    protected open fun dragConstrained(x: Int, y: Int): LLQuaternion {
        if (!isAvatarJointSafeToUse()) return LLQuaternion.DEFAULT
        val constraintAx = getConstraintAxis()
        if (isCamEdgeOn()) {
            val freeRot = dragUnconstrained(x, y)
            return extractTwist(freeRot, constraintAx)
        }
        TODO("GPU: project mouse onto constraint plane, compute signed angle, return constrained rotation")
    }

    protected fun getConstraintAxis(): LLVector3 = constraintAxis

    fun setConstraintAxis(): LLVector3 {
        val manipPart = getCurrentManipPart()
        val axis: LLVector3
        if (manipPart == LL_ROT_ROLL) {
            axis = getCenterToCamNorm()
        } else {
            val axisDir = manipPart - LL_ROT_X
            axis = when (axisDir) {
                0    -> LLVector3(1f, 0f, 0f)
                1    -> LLVector3(0f, 1f, 0f)
                2    -> LLVector3(0f, 0f, 1f)
                else -> LLVector3(1f, 0f, 0f)
            }
            val j = joint
            if (j != null) {
                TODO("GPU: rotate local axis by joint world rotation (natural or world-aligned)")
            }
        }
        constraintAxis = axis
        return axis
    }

    private fun computeAlignmentQuat(axes: BoneAxes): LLQuaternion {
        TODO("APR: use JVM equivalent for constructing quaternion from three basis vectors")
    }

    private fun computeBoneAxes(): BoneAxes {
        val j = joint ?: return BoneAxes()
        val localEnd = getJointEnd(j)
        var naturalZ = localEnd
        naturalZ = normalizeVector(naturalZ)

        var reference = referenceUpVectors[j.getName()] ?: LLVector3(0f, 0f, 1f)
        if (kotlin.math.abs(dot(naturalZ, reference)) > 0.99f) reference = LLVector3(1f, 0f, 0f)

        val dotVal = dot(naturalZ, reference)
        var naturalY = reference - LLVector3(naturalZ.x * dotVal, naturalZ.y * dotVal, naturalZ.z * dotVal)
        naturalY = normalizeVector(naturalY)
        val naturalX = normalizeVector(cross(naturalY, naturalZ))

        return BoneAxes(naturalX = naturalX, naturalY = naturalY, naturalZ = naturalZ)
    }

    fun highlightHoverSpheres(mouseX: Int, mouseY: Int) {
        val av = avatar ?: return
        if (isAvatarDead(av)) return

        highlightedJoint = null
        var nearestHitDistance = 0f
        var nearestRayDistance = 0f
        var nearestJoint: LLJoint? = null
        val targetRadius = getSettingFloat("FSManipRotateJointTargetSize", 0.03f)

        for (jointName in selectableJoints) {
            val j = getAvatarJoint(av, jointName) ?: continue
            updateJointWorldMatrix(j)
            val jointWorldPos = j.getWorldPosition()
            val (distFromCamera, distFromCenter, hit) = isMouseOverJoint(mouseX, mouseY, jointWorldPos, targetRadius)
            if (!hit) continue
            if (nearestJoint == null || nearestRayDistance > distFromCamera ||
                (nearestRayDistance == distFromCamera && nearestHitDistance > distFromCenter)) {
                nearestJoint = j
                nearestHitDistance = distFromCenter
                nearestRayDistance = distFromCamera
            }
        }
        highlightedJoint = nearestJoint
    }

    private fun isMouseOverJoint(mouseX: Int, mouseY: Int, jointWorldPos: LLVector3, jointRadius: Float): Triple<Float, Float, Boolean> {
        val j = joint ?: return Triple(0f, 0f, false)
        val av = avatar ?: return Triple(0f, 0f, false)
        if (isAvatarDead(av)) return Triple(0f, 0f, false)
        TODO("GPU: ray-sphere intersection test using mouse ray and joint world position")
    }

    private fun getSelectedJointWorldRotation(): LLQuaternion {
        val j = joint ?: return LLQuaternion.DEFAULT
        val av = avatar ?: return LLQuaternion.DEFAULT
        return getManipGimbalRotationFromPoser(j.getName())
    }

    private fun isAvatarJointSafeToUse(): Boolean {
        val j = joint ?: return false
        val av = avatar ?: return false
        if (isAvatarDead(av) || !isAvatarFullyLoaded(av)) {
            setAvatar(null)
            return false
        }
        return true
    }

    private fun extractTwist(rot: LLQuaternion, axis: LLVector3): LLQuaternion {
        val qnorm = rot.normalize()
        val v = LLVector3(qnorm.x, qnorm.y, qnorm.z)
        val w = qnorm.w
        val dotVal = dot(v, axis)
        val proj = LLVector3(axis.x * dotVal, axis.y * dotVal, axis.z * dotVal)
        var twist = LLQuaternion(proj.x, proj.y, proj.z, w)
        if (w < 0f) twist = LLQuaternion(-twist.x, -twist.y, -twist.z, -twist.w)
        return twist.normalize()
    }

    private fun dot(a: LLVector3, b: LLVector3) = a.x * b.x + a.y * b.y + a.z * b.z
    private fun cross(a: LLVector3, b: LLVector3) = LLVector3(
        a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x
    )
    private fun normalizeVector(v: LLVector3): LLVector3 {
        val len = kotlin.math.sqrt((v.x * v.x + v.y * v.y + v.z * v.z).toDouble()).toFloat()
        return if (len > 0f) LLVector3(v.x / len, v.y / len, v.z / len) else v
    }

    private fun isAvatarDead(av: Any): Boolean = TODO("APR: use JVM equivalent")
    private fun isAvatarFullyLoaded(av: Any): Boolean = TODO("APR: use JVM equivalent")
    private fun getAvatarJoint(av: Any, name: String): LLJoint? = TODO("APR: use JVM equivalent")
    private fun getAvatarJointByNumber(av: Any?, num: Int): LLJoint? = TODO("APR: use JVM equivalent")
    private fun updateJointWorldMatrix(j: LLJoint) = TODO("APR: use JVM equivalent")
    private fun getJointEnd(j: LLJoint): LLVector3 = TODO("APR: use JVM equivalent for LLJoint.getEnd()")
    private fun hasMouseCapture(): Boolean = TODO("APR: use JVM equivalent")
    private fun releaseMouseCapture() = TODO("APR: use JVM equivalent")
    private fun setMouseCapture(capture: Boolean) = TODO("APR: use JVM equivalent")
    private fun getCurrentHighlightedPart(): Int = TODO("APR: use JVM equivalent")
    private fun getCurrentManipPart(): Int = TODO("APR: use JVM equivalent")
    private fun setManipPart(part: Int) = TODO("APR: use JVM equivalent")
    private fun clearHighlightedPart() = TODO("APR: use JVM equivalent")
    private fun getCenterToCamNorm(): LLVector3 = TODO("APR: use JVM equivalent for mCenterToCamNorm")
    private fun isCamEdgeOn(): Boolean = TODO("APR: use JVM equivalent for mCamEdgeOn")
    private fun getSettingFloat(key: String, default: Float): Float = TODO("APR: use JVM equivalent for gSavedSettings")
    private fun selectJointByName(name: String) = TODO("APR: use JVM equivalent - delegate to FSFloaterPoser")
    private fun notifyPosedBoneUpdate(name: String, rotation: LLQuaternion) = TODO("APR: use JVM equivalent - delegate to FSFloaterPoser.updatePosedBones")
    private fun getManipGimbalRotationFromPoser(name: String): LLQuaternion = TODO("APR: use JVM equivalent - delegate to FSFloaterPoser.getManipGimbalRotation")
}
