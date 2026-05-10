package com.firestorm.newview

import java.util.UUID

class LLManipTranslate(composite: LLToolComposite?) : LLManip("Move", composite) {

    class ManipulatorHandle(
        val mStartPosition: FloatArray,
        val mEndPosition: FloatArray,
        val mManipID: EManipPart,
        val mHotSpotRadius: Float
    )

    enum class EHandleType {
        HANDLE_CONE,
        HANDLE_BOX,
        HANDLE_SPHERE
    }

    companion object {
        private const val NUM_AXES = 3
        private const val MOUSE_DRAG_SLOP = 2
        private const val SELECTED_ARROW_SCALE = 1.3f
        private const val MANIPULATOR_HOTSPOT_START = 0.2f
        private const val MANIPULATOR_HOTSPOT_END = 1.2f
        private const val SNAP_GUIDE_SCREEN_SIZE = 0.7f
        private const val MIN_PLANE_MANIP_DOT_PRODUCT = 0.25f
        private const val PLANE_TICK_SIZE = 0.4f
        private const val MANIPULATOR_SCALE_HALF_LIFE = 0.07f
        private const val SNAP_ARROW_SCALE = 0.7f

        private val MANIPULATOR_IDS: Array<EManipPart> = arrayOf(
            EManipPart.LL_X_ARROW, EManipPart.LL_Y_ARROW, EManipPart.LL_Z_ARROW,
            EManipPart.LL_X_ARROW, EManipPart.LL_Y_ARROW, EManipPart.LL_Z_ARROW,
            EManipPart.LL_YZ_PLANE, EManipPart.LL_XZ_PLANE, EManipPart.LL_XY_PLANE
        )

        private val ARROW_TO_AXIS: IntArray = intArrayOf(0, 0, 1, 2)

        private var sGridTexName: UInt = 0u

        fun getGridTexName(): UInt {
            if (sGridTexName == 0u) restoreGL()
            return sGridTexName
        }

        fun destroyGL() {
            sGridTexName = 0u
            TODO("GPU: release sGridTex OpenGL texture resource")
        }

        fun restoreGL() {
            TODO("GPU: generate 512x512 mip-mapped grid texture with large/medium/small grain lines, upload via LLImageGL::setManualImage")
        }
    }

    protected var mLastHoverMouseX: Int = -1
    protected var mLastHoverMouseY: Int = -1
    protected var mMouseOutsideSlop: Boolean = false
    protected var mCopyMadeThisDrag: Boolean = false
    protected var mMouseDownX: Int = -1
    protected var mMouseDownY: Int = -1
    protected var mAxisArrowLength: Float = 50f
    protected var mConeSize: Float = 0f
    protected var mArrowLengthMeters: Float = 0f
    protected var mGridSizeMeters: Float = 1f
    protected var mPlaneManipOffsetMeters: Float = 0f
    protected val mManipNormal: FloatArray = FloatArray(3)
    protected val mDragCursorStartGlobal: DoubleArray = DoubleArray(3)
    protected val mDragSelectionStartGlobal: DoubleArray = DoubleArray(3)
    protected val mUpdateTimer: LLTimer = LLTimer()
    protected val mManipulatorVertices: Array<FloatArray> = Array(18) { FloatArray(4) }
    protected var mSnapOffsetMeters: Float = 0f
    protected val mSnapOffsetAxis: FloatArray = FloatArray(3)
    protected val mGridRotation: FloatArray = FloatArray(4)
    protected val mGridOrigin: FloatArray = FloatArray(3)
    protected val mGridScale: FloatArray = FloatArray(3)
    protected var mSubdivisions: Float = 10f
    protected var mInSnapRegime: Boolean = false
    protected val mArrowScales: FloatArray = floatArrayOf(1f, 1f, 1f)
    protected val mPlaneScales: FloatArray = floatArrayOf(1f, 1f, 1f)
    protected val mPlaneManipPositions: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    protected var mWarningNoDragCopy: Boolean = false

    override fun handleSelect() {
        LLSelectMgr.getInstance().saveSelectedObjectTransform(LLSelectMgr.SELECT_ACTION_TYPE_PICK)
        TODO("APR: set gFloaterTools status text to 'move' if available")
        super.handleSelect()
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val isArrowOrPlane = mHighlightedPart == EManipPart.LL_X_ARROW ||
            mHighlightedPart == EManipPart.LL_Y_ARROW ||
            mHighlightedPart == EManipPart.LL_Z_ARROW ||
            mHighlightedPart == EManipPart.LL_YZ_PLANE ||
            mHighlightedPart == EManipPart.LL_XZ_PLANE ||
            mHighlightedPart == EManipPart.LL_XY_PLANE
        if (isArrowOrPlane) {
            return handleMouseDownOnPart(x, y, mask)
        }
        return false
    }

    override fun handleMouseDownOnPart(x: Int, y: Int, mask: Int): Boolean {
        if (!canAffectSelection()) return false
        highlightManipulators(x, y)
        val hitPart = mHighlightedPart
        val validParts = setOf(
            EManipPart.LL_X_ARROW, EManipPart.LL_Y_ARROW, EManipPart.LL_Z_ARROW,
            EManipPart.LL_YZ_PLANE, EManipPart.LL_XZ_PLANE, EManipPart.LL_XY_PLANE
        )
        if (hitPart !in validParts) return true

        mHelpTextTimer.reset()
        sNumTimesHelpTextShown++
        LLSelectMgr.getInstance().getGrid(mGridOrigin, mGridRotation, mGridScale)
        LLSelectMgr.getInstance().enableSilhouette(false)
        LLSelectMgr.getInstance().saveSelectedObjectTransform(LLSelectMgr.SELECT_ACTION_TYPE_MOVE)
        mManipPart = hitPart
        mMouseDownX = x
        mMouseDownY = y
        mMouseOutsideSlop = false

        val selectNode = mObjectSelection?.getFirstMoveableNode(true)
            ?: return true
        val selectedObject = selectNode.getObject()
            ?: run {
                TODO("GPU: set cursor to UI_CURSOR_TOOLTRANSLATE; warn lost object")
                return true
            }

        val axis = FloatArray(3)
        val axisExists = getManipAxis(selectedObject, mManipPart, axis)
        getManipNormal(selectedObject, mManipPart, mManipNormal)

        val selectCenterAgent = getPivotPoint()
        mSubdivisions = getSubdivisionLevel(selectCenterAgent, if (axisExists) axis else floatArrayOf(0f, 0f, 1f), getMinGridScale())

        if (mManipPart.ordinal >= EManipPart.LL_YZ_PLANE.ordinal &&
            mManipPart.ordinal <= EManipPart.LL_XY_PLANE.ordinal
        ) {
            TODO("GPU: if SnapToMouseCursor is set, project object center to screen and warp mouse cursor")
        }

        LLSelectMgr.getInstance().updateSelectionCenter()
        val objectStartGlobal = TODO("GPU: gAgent.getPosGlobalFromAgent(getPivotPoint())")
        getMousePointOnPlaneGlobal(mDragCursorStartGlobal, x, y, objectStartGlobal as DoubleArray, mManipNormal)
        mDragSelectionStartGlobal[0] = objectStartGlobal[0]
        mDragSelectionStartGlobal[1] = objectStartGlobal[1]
        mDragSelectionStartGlobal[2] = objectStartGlobal[2]
        mCopyMadeThisDrag = false
        setMouseCapture(true)
        return true
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (!hasMouseCapture()) {
            TODO("GPU: set cursor to UI_CURSOR_TOOLTRANSLATE")
            highlightManipulators(x, y)
            return true
        }

        TODO("APR: check MASK_COPY and warn if in edit-linked mode; handle auto-orbit when near screen edge")

        if (x == mLastHoverMouseX && y == mLastHoverMouseY) {
            TODO("GPU: set cursor to UI_CURSOR_TOOLTRANSLATE")
            return true
        }
        mLastHoverMouseX = x
        mLastHoverMouseY = y

        if (!mMouseOutsideSlop) {
            if (Math.abs(mMouseDownX - x) < MOUSE_DRAG_SLOP && Math.abs(mMouseDownY - y) < MOUSE_DRAG_SLOP) {
                TODO("GPU: set cursor to UI_CURSOR_TOOLTRANSLATE")
                return true
            } else {
                mMouseOutsideSlop = true
                TODO("APR: if MASK_COPY, call selectDuplicate and set mCopyMadeThisDrag; return early")
            }
        }

        val selectNode = mObjectSelection?.getFirstMoveableNode(true)
            ?: run {
                TODO("GPU: warn and set cursor; return true")
                return true
            }
        val obj = selectNode.getObject()
            ?: run {
                TODO("GPU: warn and set cursor; return true")
                return true
            }

        val axisF = FloatArray(3)
        val axisExists = getManipAxis(obj, mManipPart, axisF)
        val axisD = DoubleArray(3) { axisF[it].toDouble() }

        LLSelectMgr.getInstance().updateSelectionCenter()
        TODO("GPU: project mouse onto manip plane, compute relative_move, snap to grid if SnapEnabled, clamp to MaxDragDistance; apply clamped delta to all selected objects (attachments in local space, roots in global, children via parent offset); update selection center; clear camera focus")

        TODO("GPU: set cursor to UI_CURSOR_TOOLTRANSLATE")
        return true
    }

    override fun highlightManipulators(x: Int, y: Int) {
        mHighlightedPart = EManipPart.LL_NO_PART
        if (mObjectSelection?.getObjectCount() == 0) return
        TODO("GPU: project 9 manipulator arrow/plane vertices through MVP transform; sort by depth; find closest to mouse within hotspot; update mArrowScales/mPlaneScales via smooth interpolation")
    }

    override fun canAffectSelection(): Boolean {
        TODO("APR: return true if selection has any moveable, non-permanently-enforced objects")
    }

    protected fun renderArrow(whichArrow: Int, selectedArrow: Int, boxSize: Float, arrowSize: Float, handleSize: Float, reverseDirection: Boolean) {
        TODO("GPU: render axis arrow with cone/box/sphere handle, scaled by mArrowScales, highlighted if selected")
    }

    protected fun renderTranslationHandles() {
        TODO("GPU: render X/Y/Z arrows and three planar handles with appropriate colors and scale; sort by camera depth")
    }

    protected fun renderText() {
        TODO("GPU: render tick value text for current position along active axis via renderTickValue")
    }

    protected fun renderSnapGuides() {
        TODO("GPU: render snap grid lines and tick marks along active axis using mSnapOffsetAxis, mSubdivisions, mGridScale")
    }

    protected fun renderGrid(x: Float, y: Float, size: Float, r: Float, g: Float, b: Float, a: Float) {
        TODO("GPU: render a planar grid quad using sGridTex texture with given colour and alpha")
    }

    protected fun renderGridVert(xTrans: Float, yTrans: Float, r: Float, g: Float, b: Float, alpha: Float) {
        TODO("GPU: emit a single coloured grid vertex via gGL.color/vertex")
    }

    protected fun highlightIntersection(normal: FloatArray, selectionCenter: FloatArray, gridRotation: FloatArray, innerColor: FloatArray) {
        TODO("GPU: render intersection highlight ring where manipulation plane meets selection bounding box")
    }

    protected fun getMinGridScale(): Float {
        TODO("APR: return minimum of mGridScale X/Y/Z components")
    }
}
