package com.firestorm.newview

import java.util.UUID

enum class EScaleManipulatorType {
    SCALE_MANIP_CORNER,
    SCALE_MANIP_FACE
}

enum class ESnapRegimes(val bits: Int) {
    SNAP_REGIME_NONE(0),
    SNAP_REGIME_UPPER(0x1),
    SNAP_REGIME_LOWER(0x2)
}

fun getDefaultMaxPrimScale(isFlora: Boolean = false): Float {
    return if (isFlora) 16f else 64f
}

class LLManipScale(composite: LLToolComposite?) : LLManip("Scale", composite) {

    class ManipulatorHandle(
        val mPosition: FloatArray,
        val mManipID: EManipPart,
        val mType: EScaleManipulatorType
    )

    companion object {
        const val NUM_MANIPULATORS: Int = 14

        private const val MAX_MANIP_SELECT_DISTANCE_SQUARED = 11f * 11f
        private const val SNAP_GUIDE_SCREEN_OFFSET = 0.05f
        private const val SNAP_GUIDE_SCREEN_LENGTH = 0.7f
        private const val SELECTED_MANIPULATOR_SCALE = 1.2f
        private const val MANIPULATOR_SCALE_HALF_LIFE = 0.07f

        private val MANIPULATOR_IDS: Array<EManipPart> = arrayOf(
            EManipPart.LL_CORNER_NNN, EManipPart.LL_CORNER_NNP,
            EManipPart.LL_CORNER_NPN, EManipPart.LL_CORNER_NPP,
            EManipPart.LL_CORNER_PNN, EManipPart.LL_CORNER_PNP,
            EManipPart.LL_CORNER_PPN, EManipPart.LL_CORNER_PPP,
            EManipPart.LL_FACE_POSZ, EManipPart.LL_FACE_POSX,
            EManipPart.LL_FACE_POSY, EManipPart.LL_FACE_NEGX,
            EManipPart.LL_FACE_NEGY, EManipPart.LL_FACE_NEGZ
        )

        private var mInvertUniform: Boolean = false
        private var scaleUniform: Boolean = false
        private var scaleStretchTextures: Boolean = false
        private var scaleShowAxes: Boolean = true

        fun setUniform(b: Boolean) {
            scaleUniform = b
        }

        fun getUniform(): Boolean {
            return scaleUniform.xor(mInvertUniform)
        }

        fun setStretchTextures(b: Boolean) {
            scaleStretchTextures = b
        }

        fun getStretchTextures(): Boolean {
            return scaleStretchTextures
        }

        fun setShowAxes(b: Boolean) {
            scaleShowAxes = b
        }

        fun getShowAxes(): Boolean {
            return scaleShowAxes
        }
    }

    private var mScaledBoxHandleSize: Float = 1f
    private val mDragStartPointGlobal: DoubleArray = DoubleArray(3)
    private val mDragStartCenterGlobal: DoubleArray = DoubleArray(3)
    private val mDragPointGlobal: DoubleArray = DoubleArray(3)
    private val mDragFarHitGlobal: DoubleArray = DoubleArray(3)
    private var mLastMouseX: Int = -1
    private var mLastMouseY: Int = -1
    private var mSendUpdateOnMouseUp: Boolean = false
    private var mLastUpdateFlags: UInt = 0u
    private val mProjectedManipulators: MutableList<ManipulatorHandle> = mutableListOf()
    private val mManipulatorVertices: Array<FloatArray> = Array(14) { FloatArray(4) }
    private var mScaleSnapUnit1: Float = 1f
    private var mScaleSnapUnit2: Float = 1f
    private val mScalePlaneNormal1: FloatArray = FloatArray(3)
    private val mScalePlaneNormal2: FloatArray = FloatArray(3)
    private val mSnapGuideDir1: FloatArray = FloatArray(3)
    private val mSnapGuideDir2: FloatArray = FloatArray(3)
    private val mSnapDir1: FloatArray = FloatArray(3)
    private val mSnapDir2: FloatArray = FloatArray(3)
    private var mSnapRegimeOffset: Float = 0f
    private var mTickPixelSpacing1: Float = 0f
    private var mTickPixelSpacing2: Float = 0f
    private var mSnapGuideLength: Float = 0f
    private val mScaleCenter: FloatArray = FloatArray(3)
    private val mScaleDir: FloatArray = FloatArray(3)
    private var mScaleSnappedValue: Float = 0f
    private var mSnapRegime: ESnapRegimes = ESnapRegimes.SNAP_REGIME_NONE
    private val mManipulatorScales: FloatArray = FloatArray(NUM_MANIPULATORS) { 1f }
    private val mBoxHandleSize: FloatArray = FloatArray(NUM_MANIPULATORS) { 1f }
    private var mFirstClickX: Int = 0
    private var mFirstClickY: Int = 0
    private var mIsFirstClick: Boolean = false

    override fun handleSelect() {
        val bbox = LLSelectMgr.getInstance().getBBoxOfSelection()
        updateSnapGuides(bbox)
        LLSelectMgr.getInstance().saveSelectedObjectTransform(LLSelectMgr.SELECT_ACTION_TYPE_PICK)
        super.handleSelect()
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (mHighlightedPart != EManipPart.LL_NO_PART) {
            return handleMouseDownOnPart(x, y, mask)
        }
        return false
    }

    override fun handleMouseDownOnPart(x: Int, y: Int, mask: Int): Boolean {
        if (!canAffectSelection()) return false
        highlightManipulators(x, y)
        val hitPart = mHighlightedPart
        LLSelectMgr.getInstance().enableSilhouette(false)
        mManipPart = hitPart
        val bbox = LLSelectMgr.getInstance().getBBoxOfSelection()
        TODO("GPU: compute box_corner_agent, far_corner_agent from partToUnitVector and bbox localToAgent; set drag globals; save transform; capture mouse")
        mFirstClickX = x
        mFirstClickY = y
        mIsFirstClick = true
        mHelpTextTimer.reset()
        sNumTimesHelpTextShown++
        setMouseCapture(true)
        return true
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        handleHover(x, y, mask)
        if (hasMouseCapture()) {
            val faceMin = EManipPart.LL_FACE_MIN.ordinal
            val faceMax = EManipPart.LL_FACE_MAX.ordinal
            val cornerMin = EManipPart.LL_CORNER_MIN.ordinal
            val cornerMax = EManipPart.LL_CORNER_MAX.ordinal
            val part = mManipPart.ordinal
            if (part in faceMin..faceMax) {
                sendUpdates(sendPosition = true, sendScale = true, corner = false)
            } else if (part in cornerMin..cornerMax) {
                sendUpdates(sendPosition = true, sendScale = true, corner = true)
            }
            LLSelectMgr.getInstance().adjustTexturesByScale(finalizing = true, regScaleTextures = getStretchTextures())
            LLSelectMgr.getInstance().enableSilhouette(true)
            mManipPart = EManipPart.LL_NO_PART
            LLSelectMgr.getInstance().sendMultipleUpdate(mLastUpdateFlags)
            LLSelectMgr.getInstance().saveSelectedObjectTransform(LLSelectMgr.SELECT_ACTION_TYPE_PICK)
        }
        return super.handleMouseUp(x, y, mask)
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture()) {
            if (mObjectSelection?.isEmpty() == true) {
                setMouseCapture(false)
            } else {
                if (mFirstClickX != x || mFirstClickY != y) {
                    mIsFirstClick = false
                }
                if (!mIsFirstClick) {
                    drag(x, y)
                }
            }
        } else {
            mSnapRegime = ESnapRegimes.SNAP_REGIME_NONE
            highlightManipulators(x, y)
        }
        LLSelectMgr.getInstance().adjustTexturesByScale(finalizing = false, regScaleTextures = getStretchTextures())
        TODO("GPU: set cursor to UI_CURSOR_TOOLSCALE via gViewerWindow")
        return true
    }

    override fun highlightManipulators(x: Int, y: Int) {
        mHighlightedPart = EManipPart.LL_NO_PART
        if (!canAffectSelection()) return
        TODO("GPU: project all 14 manipulator corner/face vertices through modelview+projection transform; find closest to mouse within MAX_MANIP_SELECT_DISTANCE_SQUARED; update mManipulatorScales via smooth interpolation")
    }

    override fun canAffectSelection(): Boolean {
        return mObjectSelection?.isEmpty() != true
    }

    fun handleMiddleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        mInvertUniform = true
        return true
    }

    fun handleMiddleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        mInvertUniform = false
        return true
    }

    private fun renderCorners(localBbox: LLBBox) {
        TODO("GPU: iterate all 8 corners of bbox, call conditionalHighlight and renderBoxHandle for each")
    }

    private fun renderFaces(localBbox: LLBBox) {
        TODO("GPU: render 6 face axis handles, sorted by depth from camera; call conditionalHighlight and renderAxisHandle for each face")
    }

    private fun renderBoxHandle(x: Float, y: Float, z: Float) {
        TODO("GPU: draw a scaled box handle at (x,y,z) using gGL quads with depth test disabled")
    }

    private fun renderAxisHandle(part: UInt, start: FloatArray, end: FloatArray) {
        TODO("GPU: draw line and cone/sphere handle from start to end using gGL")
    }

    private fun renderGuidelinesPart(localBbox: LLBBox) {
        TODO("GPU: render axis guideline lines through active scale face/corner")
    }

    private fun renderSnapGuides(localBbox: LLBBox) {
        TODO("GPU: render snap grid tick marks and labels around scale guides based on mSnapRegime, mScaleSnapUnit1/2, mTickPixelSpacing1/2")
    }

    fun render() {
        TODO("GPU: render bounding box corners and faces with scale handles, snap guides, and XYZ extents overlay")
    }

    private fun revert() {
        LLSelectMgr.getInstance().saveSelectedObjectTransform(LLSelectMgr.SELECT_ACTION_TYPE_PICK)
    }

    private fun conditionalHighlight(part: UInt, highlight: FloatArray? = null, normal: FloatArray? = null) {
        for (i in 0 until NUM_MANIPULATORS) {
            if (MANIPULATOR_IDS[i].ordinal.toUInt() == part) {
                mScaledBoxHandleSize = mManipulatorScales[i] * mBoxHandleSize[i]
                break
            }
        }
        TODO("GPU: set gGL color based on whether part is active manip, highlighted, or default normal")
    }

    private fun drag(x: Int, y: Int) {
        val faceMin = EManipPart.LL_FACE_MIN.ordinal
        val faceMax = EManipPart.LL_FACE_MAX.ordinal
        if (mManipPart.ordinal in faceMin..faceMax) {
            dragFace(x, y)
        } else {
            dragCorner(x, y)
        }
    }

    private fun dragFace(x: Int, y: Int) {
        TODO("GPU: project mouse onto scale plane, snap if enabled, compute new face extent, call sendUpdates")
    }

    private fun dragCorner(x: Int, y: Int) {
        TODO("GPU: compute scale delta along diagonal axis from corner drag, apply uniform or non-uniform scaling, call sendUpdates")
    }

    private fun sendUpdates(sendPosition: Boolean, sendScale: Boolean, corner: Boolean = false) {
        if (!sendPosition && !sendScale) return
        mSendUpdateOnMouseUp = corner
        LLSelectMgr.getInstance().sendMultipleUpdate(mLastUpdateFlags)
    }

    private fun faceToUnitVector(part: Int): FloatArray {
        TODO("GPU: map face EManipPart to unit vector (e.g. LL_FACE_POSZ -> (0,0,1))")
    }

    private fun cornerToUnitVector(part: Int): FloatArray {
        TODO("GPU: map corner EManipPart to diagonal unit vector (e.g. LL_CORNER_PPP -> (1,1,1)/sqrt(3))")
    }

    private fun edgeToUnitVector(part: Int): FloatArray {
        TODO("GPU: map edge EManipPart to unit vector along edge midpoint direction")
    }

    private fun partToUnitVector(part: Int): FloatArray {
        val fp = EManipPart.values()[part]
        val faceMin = EManipPart.LL_FACE_MIN.ordinal
        val faceMax = EManipPart.LL_FACE_MAX.ordinal
        val cornerMin = EManipPart.LL_CORNER_MIN.ordinal
        val cornerMax = EManipPart.LL_CORNER_MAX.ordinal
        val edgeMin = EManipPart.LL_EDGE_MIN.ordinal
        val edgeMax = EManipPart.LL_EDGE_MAX.ordinal
        return when {
            part in faceMin..faceMax -> faceToUnitVector(part)
            part in cornerMin..cornerMax -> cornerToUnitVector(part)
            part in edgeMin..edgeMax -> edgeToUnitVector(part)
            else -> FloatArray(3)
        }
    }

    private fun unitVectorToLocalBBoxExtent(v: FloatArray, bbox: LLBBox): FloatArray {
        TODO("GPU: map unit vector components to bbox min/max corners to find the corner extent point in local space")
    }

    private fun partToMaxScale(part: Int, bbox: LLBBox): Float {
        return getDefaultMaxPrimScale()
    }

    private fun partToMinScale(part: Int, bbox: LLBBox): Float {
        return 0.001f
    }

    private fun nearestAxis(v: FloatArray): FloatArray {
        TODO("GPU: return the cardinal axis (+/-X/Y/Z) whose direction most closely matches v")
    }

    private fun stretchFace(dragStartAgent: FloatArray, dragDeltaAgent: FloatArray) {
        TODO("GPU: apply face-stretch to all selected objects: compute new scale and position along face normal axis")
    }

    private fun adjustTextureRepeats() {
        TODO("GPU: rescale texture repeat values on box faces based on ratio of new scale to saved scale")
    }

    private fun updateSnapGuides(bbox: LLBBox) {
        TODO("GPU: compute snap guide normals, directions, snap units and pixel spacing from camera/bbox geometry")
    }
}
