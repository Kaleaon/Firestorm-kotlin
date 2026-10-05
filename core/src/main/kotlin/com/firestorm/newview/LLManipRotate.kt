package com.firestorm.newview

import java.util.UUID

class LLManipRotate(composite: LLToolComposite?) : LLManip("Rotate", composite) {

    class ManipulatorHandle(
        val mAxisU: FloatArray,
        val mAxisV: FloatArray,
        val mManipID: UInt
    )

    companion object {
        private const val RADIUS_PIXELS = 100f
        private const val SQ_RADIUS = RADIUS_PIXELS * RADIUS_PIXELS
        private const val WIDTH_PIXELS = 8f
        private const val CIRCLE_STEPS = 100
        private const val MAX_MANIP_SELECT_DISTANCE = 100f
        private const val SNAP_ANGLE_INCREMENT = 5.625f
        private const val SNAP_ANGLE_DETENTE = SNAP_ANGLE_INCREMENT
        private const val SNAP_GUIDE_RADIUS_1 = 2.8f
        private const val SNAP_GUIDE_RADIUS_2 = 2.4f
        private const val SNAP_GUIDE_RADIUS_3 = 2.2f
        private const val SNAP_GUIDE_RADIUS_4 = 2.1f
        private const val SNAP_GUIDE_RADIUS_5 = 2.05f
        private const val SNAP_GUIDE_INNER_RADIUS = 2f
        private const val AXIS_ONTO_CAM_TOLERANCE = 0.17364817766693f  // cos(80 * PI/180)
        private const val SELECTED_MANIPULATOR_SCALE = 1.05f
        private const val MANIPULATOR_SCALE_HALF_LIFE = 0.07f

        fun mouseToRay(x: Int, y: Int, rayPt: FloatArray, rayDir: FloatArray) {
            System.err.println("LLManipRotate: mouseToRay not yet implemented")
        }

        fun intersectMouseWithSphere(x: Int, y: Int, sphereCenter: FloatArray, sphereRadius: Float): FloatArray {
            return FloatArray(3)
        }

        fun intersectRayWithSphere(rayPt: FloatArray, rayDir: FloatArray, sphereCenter: FloatArray, sphereRadius: Float): FloatArray {
            return FloatArray(3)
        }
    }

    private val mRotationCenter: DoubleArray = DoubleArray(3)
    private val mCenterScreen: IntArray = IntArray(2)
    private val mRotation: FloatArray = FloatArray(4)
    private val mMouseDown: FloatArray = FloatArray(3)
    private val mMouseCur: FloatArray = FloatArray(3)
    private val mAgentSelfAtAxis: FloatArray = FloatArray(3)
    private var mRadiusMeters: Float = 0f
    private val mCenterToCam: FloatArray = FloatArray(3)
    private val mCenterToCamNorm: FloatArray = FloatArray(3)
    private var mCenterToCamMag: Float = 0f
    private val mCenterToProfilePlane: FloatArray = FloatArray(3)
    private var mCenterToProfilePlaneMag: Float = 0f
    private var mSendUpdateOnMouseUp: Boolean = false
    private var mSmoothRotate: Boolean = false
    private var mCamEdgeOn: Boolean = false
    private val mManipulatorVertices: Array<FloatArray> = Array(6) { FloatArray(4) }
    private val mManipulatorScales: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)

    override fun handleSelect() {
        LLSelectMgr.getInstance().saveSelectedObjectTransform(LLSelectMgr.SELECT_ACTION_TYPE_PICK)
        System.err.println("LLManipRotate: set gFloaterTools status text to 'rotate' not yet implemented")
        super.handleSelect()
    }

    fun render() {
        // no-op
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val firstObject = mObjectSelection?.getFirstMoveableObject(true)
        if (firstObject != null && mHighlightedPart != EManipPart.LL_NO_PART) {
            return handleMouseDownOnPart(x, y, mask)
        }
        return false
    }

    override fun handleMouseDownOnPart(x: Int, y: Int, mask: Int): Boolean {
        if (!canAffectSelection()) return false
        highlightManipulators(x, y)
        val hitPart = mHighlightedPart
        LLSelectMgr.getInstance().saveSelectedObjectTransform(LLSelectMgr.SELECT_ACTION_TYPE_ROTATE)
        val pivotAgent = getPivotPoint()
        mRotationCenter[0] = 0.0
        mRotationCenter[1] = 0.0
        mRotationCenter[2] = 0.0
        mManipPart = hitPart
        System.err.println("LLManipRotate: compute initial mMouseDown from sphere or ring intersection not yet implemented")
        mHelpTextTimer.reset()
        sNumTimesHelpTextShown++
        setMouseCapture(true)
        LLSelectMgr.getInstance().enableSilhouette(false)
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        handleHover(x, y, mask)
        if (hasMouseCapture()) {
            for (selectNode in mObjectSelection ?: emptyList<LLSelectNode>()) {
                val obj = selectNode.getObject() ?: continue
                val rootObj = obj.getRootEdit()
                if (obj.permMove() && !obj.isPermanentEnforced() &&
                    (rootObj == null || !rootObj.isPermanentEnforced()) &&
                    (obj.isRootEdit() || selectNode.mIndividualSelection)
                ) {
                    obj.mUnselectedChildrenPositions.clear()
                }
            }
            mManipPart = EManipPart.LL_NO_PART
            LLSelectMgr.getInstance().sendMultipleUpdate(LLSelectMgr.UPD_ROTATION or LLSelectMgr.UPD_POSITION)
            LLSelectMgr.getInstance().enableSilhouette(true)
            LLSelectMgr.getInstance().updateSelectionCenter()
            LLSelectMgr.getInstance().saveSelectedObjectTransform(LLSelectMgr.SELECT_ACTION_TYPE_PICK)
        }
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture()) {
            if (mObjectSelection?.isEmpty() == true) {
                setMouseCapture(false)
            } else {
                drag(x, y)
            }
        } else {
            highlightManipulators(x, y)
        }
        System.err.println("LLManipRotate: set cursor to UI_CURSOR_TOOLROTATE not yet implemented")
        return true
    }

    override fun highlightManipulators(x: Int, y: Int) {
        // no-op
    }

    override fun canAffectSelection(): Boolean {
        return false
    }

    private fun updateHoverView() {
        System.err.println("LLManipRotate: updateHoverView not yet implemented")
    }

    private fun drag(x: Int, y: Int) {
        if (!updateVisiblity()) return
        val rotation = if (mManipPart == EManipPart.LL_ROT_GENERAL) {
            dragUnconstrained(x, y)
        } else {
            dragConstrained(x, y)
        }
        val damped = mSmoothRotate
        mSmoothRotate = false
        System.err.println("LLManipRotate: apply rotation quaternion to selected objects not yet implemented")
    }

    private fun projectToSphere(x: Float, y: Float, onSphere: BooleanArray): FloatArray {
        val distSquared = x * x + y * y
        onSphere[0] = distSquared <= SQ_RADIUS
        val z = if (onSphere[0]) Math.sqrt((SQ_RADIUS - distSquared).toDouble()).toFloat() else 0f
        return floatArrayOf(x, y, z)
    }

    private fun renderSnapGuides() {
        // no-op
    }

    private fun renderActiveRing(radius: Float, width: Float, centerColor: FloatArray, sideColor: FloatArray) {
        // no-op
    }

    private fun updateVisiblity(): Boolean {
        return false
    }

    private fun findNearestPointOnRing(x: Int, y: Int, center: FloatArray, axis: FloatArray): FloatArray {
        // no-op
        return FloatArray(3)
    }

    private fun dragUnconstrained(x: Int, y: Int): FloatArray {
        // no-op
        return FloatArray(4)
    }

    private fun dragConstrained(x: Int, y: Int): FloatArray {
        // no-op
        return FloatArray(4)
    }

    private fun getConstraintAxis(): FloatArray {
        return if (mManipPart == EManipPart.LL_ROT_ROLL) {
            mCenterToCamNorm.clone()
        } else {
            val axisDir = mManipPart.ordinal - EManipPart.LL_ROT_X.ordinal
            val axis = FloatArray(3)
            if (axisDir in 0 until 3) {
                axis[axisDir] = 1f
            }
            System.err.println("LLManipRotate: getConstraintAxis grid_rotation not yet implemented")
            axis
        }
    }

    private fun getObjectAxisClosestToMouse(axis: FloatArray): Int {
        // no-op
        return 0
    }
}
